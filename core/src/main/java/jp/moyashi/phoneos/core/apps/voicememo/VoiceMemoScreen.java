package jp.moyashi.phoneos.core.apps.voicememo;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.VFS;
import jp.moyashi.phoneos.core.service.hardware.MicrophoneSocket;
import jp.moyashi.phoneos.core.service.hardware.SpeakerSocket;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Base64;

/**
 * ボイスメモ画面。
 * 録音・再生・保存・一覧表示機能を提供する。
 */
public class VoiceMemoScreen implements Screen {
    private Kernel kernel;
    private MicrophoneSocket microphone;
    private SpeakerSocket speaker;
    private VFS vfs;

    // 録音状態
    private boolean isRecording = false;
    private List<byte[]> recordingBuffer = new ArrayList<>();
    private long recordingStartTime = 0;

    // 再生状態
    private boolean isPlaying = false;
    private int playingIndex = -1;

    // メモ一覧
    private List<VoiceMemo> memos = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ITEM_HEIGHT = 60;

    // UI状態
    private String statusMessage = "";
    private long statusMessageTime = 0;

    public VoiceMemoScreen(Kernel kernel) {
        this.kernel = kernel;
        this.microphone = kernel.getMicrophoneSocket();
        this.speaker = kernel.getSpeakerSocket();
        this.vfs = kernel.getVFS();

        loadMemos();
    }

    @Override
    public void setup(PGraphics g) {
        System.out.println("[VoiceMemoScreen] Setup");
    }

    @Override
    public void draw(PGraphics g) {
        // 背景
        g.background(30, 30, 40);

        // ヘッダー
        drawHeader(g);

        // ハードウェアステータス
        drawHardwareStatus(g);

        // 録音/再生コントロール
        drawControls(g);

        // メモ一覧
        drawMemoList(g);

        // ステータスメッセージ
        drawStatusMessage(g);

        // 録音中のアニメーション
        if (isRecording) {
            updateRecording();
        }

        // 再生中の処理
        if (isPlaying) {
            updatePlayback();
        }
    }

    private void drawHeader(PGraphics g) {
        g.fill(255, 255, 255);
        g.textAlign(PApplet.CENTER, PApplet.TOP);
        g.textSize(20);
        g.text("🎤 Voice Memo", 200, 20);

        // セパレーター
        g.stroke(100, 100, 100);
        g.line(20, 60, 380, 60);
    }

    private void drawHardwareStatus(PGraphics g) {
        g.noStroke();
        g.textAlign(PApplet.LEFT, PApplet.TOP);
        g.textSize(11);

        int y = 70;

        // マイク状態
        boolean micAvailable = microphone.isAvailable();
        g.fill(micAvailable ? 100 : 255, micAvailable ? 255 : 100, 100);
        g.text("🎤 Microphone: " + (micAvailable ? "Available" : "Not Available"), 30, y);

        // スピーカー状態
        boolean speakerAvailable = speaker.isAvailable();
        g.fill(speakerAvailable ? 100 : 255, speakerAvailable ? 255 : 100, 100);
        g.text("🔊 Speaker: " + (speakerAvailable ? "Available" : "Not Available"), 30, y + 15);

        // 音量レベル
        if (speakerAvailable) {
            g.fill(200, 200, 200);
            g.text("Volume: " + speaker.getVolumeLevel().name(), 30, y + 30);
        }
    }

    private void drawControls(PGraphics g) {
        int y = 150;

        // 録音ボタン
        boolean canRecord = microphone.isAvailable() && !isPlaying;
        drawButton(g, 50, y, 120, 40, isRecording ? "⏹ Stop" : "⏺ Record",
                   isRecording ? 255 : (canRecord ? 255 : 100), 80, 80);

        // 再生ボタン（最後に録音したメモを再生）
        boolean canPlay = speaker.isAvailable() && !memos.isEmpty() && !isRecording;
        drawButton(g, 230, y, 120, 40, isPlaying ? "⏹ Stop" : "▶ Play Last",
                   isPlaying ? 255 : (canPlay ? 100 : 50), isPlaying ? 255 : (canPlay ? 255 : 100), 100);

        // 音量調整ボタン
        if (speaker.isAvailable()) {
            drawSmallButton(g, 50, y + 50, 80, 30, "🔉 Low", 150, 150, 200);
            drawSmallButton(g, 140, y + 50, 80, 30, "🔊 Med", 150, 150, 200);
            drawSmallButton(g, 230, y + 50, 80, 30, "📢 High", 150, 150, 200);
            drawSmallButton(g, 320, y + 50, 80, 30, "🔇 Off", 150, 150, 200);
        }

        // 録音時間表示
        if (isRecording) {
            long duration = (System.currentTimeMillis() - recordingStartTime) / 1000;
            g.fill(255, 100, 100);
            g.textAlign(PApplet.CENTER, PApplet.TOP);
            g.textSize(14);
            g.text(String.format("⏱ %02d:%02d", duration / 60, duration % 60), 200, y + 90);
        }
    }

    private void drawMemoList(PGraphics g) {
        int listY = 270;
        int listHeight = 300;

        g.fill(255, 255, 255);
        g.textAlign(PApplet.LEFT, PApplet.TOP);
        g.textSize(14);
        g.text("Saved Memos (" + memos.size() + ")", 30, listY);

        // セパレーター
        g.stroke(100, 100, 100);
        g.line(30, listY + 25, 370, listY + 25);

        // メモ一覧
        g.pushMatrix();
        g.translate(0, listY + 30);

        for (int i = 0; i < memos.size(); i++) {
            int itemY = i * ITEM_HEIGHT - scrollOffset;
            if (itemY + ITEM_HEIGHT < 0 || itemY > listHeight) {
                continue; // 画面外はスキップ
            }

            VoiceMemo memo = memos.get(i);
            boolean isSelected = (playingIndex == i && isPlaying);

            // 背景
            g.noStroke();
            g.fill(isSelected ? 80 : 50, isSelected ? 80 : 50, isSelected ? 100 : 60);
            g.rect(30, itemY, 340, ITEM_HEIGHT - 5, 5);

            // タイトル
            g.fill(255, 255, 255);
            g.textSize(13);
            g.text(memo.getName(), 40, itemY + 10);

            // 詳細情報
            g.fill(180, 180, 180);
            g.textSize(10);
            g.text(memo.getDateString() + " | " + memo.getDurationString(), 40, itemY + 30);

            // 再生ボタン
            if (speaker.isAvailable()) {
                drawSmallButton(g, 280, itemY + 10, 40, 30, "▶", 100, 200, 100);
            }

            // 削除ボタン
            drawSmallButton(g, 330, itemY + 10, 30, 30, "🗑", 200, 100, 100);
        }

        g.popMatrix();

        // メモがない場合
        if (memos.isEmpty()) {
            g.fill(150, 150, 150);
            g.textAlign(PApplet.CENTER, PApplet.TOP);
            g.textSize(12);
            g.text("No memos recorded yet", 200, listY + 60);
        }
    }

    private void drawButton(PGraphics g, int x, int y, int w, int h, String label, int r, int gr, int b) {
        g.noStroke();
        g.fill(r, gr, b);
        g.rect(x, y, w, h, 5);

        g.fill(255, 255, 255);
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(13);
        g.text(label, x + w/2, y + h/2);
    }

    private void drawSmallButton(PGraphics g, int x, int y, int w, int h, String label, int r, int gr, int b) {
        g.noStroke();
        g.fill(r, gr, b);
        g.rect(x, y, w, h, 3);

        g.fill(255, 255, 255);
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(10);
        g.text(label, x + w/2, y + h/2);
    }

    private void drawStatusMessage(PGraphics g) {
        if (System.currentTimeMillis() - statusMessageTime < 3000) {
            g.fill(100, 200, 255);
            g.textAlign(PApplet.CENTER, PApplet.BOTTOM);
            g.textSize(11);
            g.text(statusMessage, 200, 590);
        }
    }

    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        // 録音ボタン
        if (isInside(mouseX, mouseY, 50, 150, 120, 40)) {
            if (microphone.isAvailable() && !isPlaying) {
                if (isRecording) {
                    stopRecording();
                } else {
                    startRecording();
                }
            }
        }

        // 再生ボタン（最後のメモ）
        if (isInside(mouseX, mouseY, 230, 150, 120, 40)) {
            if (speaker.isAvailable() && !memos.isEmpty() && !isRecording) {
                if (isPlaying) {
                    stopPlayback();
                } else {
                    playMemo(memos.size() - 1);
                }
            }
        }

        // 音量調整ボタン
        if (speaker.isAvailable()) {
            if (isInside(mouseX, mouseY, 50, 200, 80, 30)) {
                speaker.setVolumeLevel(SpeakerSocket.VolumeLevel.LOW);
                showStatus("Volume: Low");
            } else if (isInside(mouseX, mouseY, 140, 200, 80, 30)) {
                speaker.setVolumeLevel(SpeakerSocket.VolumeLevel.MEDIUM);
                showStatus("Volume: Medium");
            } else if (isInside(mouseX, mouseY, 230, 200, 80, 30)) {
                speaker.setVolumeLevel(SpeakerSocket.VolumeLevel.HIGH);
                showStatus("Volume: High");
            } else if (isInside(mouseX, mouseY, 320, 200, 80, 30)) {
                speaker.setVolumeLevel(SpeakerSocket.VolumeLevel.OFF);
                showStatus("Volume: Off");
            }
        }

        // メモ一覧のクリック判定
        int listY = 270 + 30;
        for (int i = 0; i < memos.size(); i++) {
            int itemY = listY + i * ITEM_HEIGHT - scrollOffset;

            // 再生ボタン
            if (isInside(mouseX, mouseY, 280, itemY + 10, 40, 30)) {
                if (speaker.isAvailable() && !isRecording) {
                    playMemo(i);
                }
            }

            // 削除ボタン
            if (isInside(mouseX, mouseY, 330, itemY + 10, 30, 30)) {
                deleteMemo(i);
            }
        }
    }

    @Override
    public void keyPressed(PGraphics g, char key, int keyCode) {
        if (key == 'q' || key == 'Q') {
            kernel.getScreenManager().popScreen();
        }
    }

    @Override
    public String getScreenTitle() {
        return "Voice Memo";
    }

    private boolean isInside(int x, int y, int rx, int ry, int rw, int rh) {
        return x >= rx && x <= rx + rw && y >= ry && y <= ry + rh;
    }

    public void startRecording() {
        if (!microphone.isAvailable()) {
            showStatus("Microphone not available");
            return;
        }

        microphone.setEnabled(true);
        isRecording = true;
        recordingBuffer.clear();
        recordingStartTime = System.currentTimeMillis();
        showStatus("Recording started...");
        System.out.println("[VoiceMemoScreen] Recording started");
    }

    public void stopRecording() {
        if (!isRecording) {
            return;
        }

        isRecording = false;
        microphone.setEnabled(false);

        // 録音データを保存
        if (!recordingBuffer.isEmpty()) {
            saveMemo();
            showStatus("Recording saved!");
        } else {
            showStatus("No data recorded");
        }

        System.out.println("[VoiceMemoScreen] Recording stopped");
    }

    private void updateRecording() {
        // マイクからデータを取得
        byte[] audioData = microphone.getAudioData();
        if (audioData != null && audioData.length > 0) {
            recordingBuffer.add(audioData);
        }
    }

    private void saveMemo() {
        // メモ名を生成
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        String timestamp = sdf.format(new Date());
        String filename = "voice_memo_" + timestamp + ".dat";

        // 録音データを結合
        int totalSize = recordingBuffer.stream().mapToInt(arr -> arr.length).sum();
        byte[] fullData = new byte[totalSize];
        int offset = 0;
        for (byte[] chunk : recordingBuffer) {
            System.arraycopy(chunk, 0, fullData, offset, chunk.length);
            offset += chunk.length;
        }

        // Base64エンコードしてVFSに保存
        String encodedData = Base64.getEncoder().encodeToString(fullData);
        vfs.writeFile("voice_memos/" + filename, encodedData);

        // メモリストに追加
        long duration = (System.currentTimeMillis() - recordingStartTime) / 1000;
        VoiceMemo memo = new VoiceMemo(filename, new Date(), duration);
        memos.add(memo);

        System.out.println("[VoiceMemoScreen] Saved memo: " + filename + " (" + totalSize + " bytes)");
    }

    private void loadMemos() {
        memos.clear();

        // VFSからメモファイルを読み込み
        // 注: VFSはファイル一覧を取得するメソッドがないため、ここでは空リストとして開始
        // メモは保存時にリストに追加される

        System.out.println("[VoiceMemoScreen] Loaded " + memos.size() + " memos");
    }

    public void playMemo(int index) {
        if (index < 0 || index >= memos.size() || !speaker.isAvailable()) {
            return;
        }

        VoiceMemo memo = memos.get(index);
        String encodedData = vfs.readFile("voice_memos/" + memo.getFilename());

        if (encodedData == null || encodedData.isEmpty()) {
            showStatus("Failed to load memo");
            return;
        }

        // Base64デコード
        byte[] audioData;
        try {
            audioData = Base64.getDecoder().decode(encodedData);
        } catch (IllegalArgumentException e) {
            showStatus("Failed to decode memo");
            System.err.println("[VoiceMemoScreen] Failed to decode: " + e.getMessage());
            return;
        }

        isPlaying = true;
        playingIndex = index;
        speaker.playAudio(audioData);
        showStatus("Playing: " + memo.getName());

        System.out.println("[VoiceMemoScreen] Playing memo: " + memo.getFilename());
    }

    public void stopPlayback() {
        if (!isPlaying) {
            return;
        }

        isPlaying = false;
        playingIndex = -1;
        speaker.stopAudio();
        showStatus("Playback stopped");

        System.out.println("[VoiceMemoScreen] Playback stopped");
    }

    private void updatePlayback() {
        // 再生が完了したかチェック（TODO: 実際の実装では再生時間を追跡）
        // 現時点では3秒後に自動停止
        // 実際のSVC連携時は、再生完了イベントをハンドリングする
    }

    private void deleteMemo(int index) {
        if (index < 0 || index >= memos.size()) {
            return;
        }

        VoiceMemo memo = memos.get(index);

        // VFSから削除
        vfs.deleteFile("voice_memos/" + memo.getFilename());

        // リストから削除
        memos.remove(index);

        showStatus("Memo deleted");
        System.out.println("[VoiceMemoScreen] Deleted memo: " + memo.getFilename());
    }

    private void showStatus(String message) {
        statusMessage = message;
        statusMessageTime = System.currentTimeMillis();
    }

    /**
     * ボイスメモのデータモデル。
     */
    private static class VoiceMemo {
        private String filename;
        private Date date;
        private long durationSeconds;

        public VoiceMemo(String filename, Date date, long durationSeconds) {
            this.filename = filename;
            this.date = date;
            this.durationSeconds = durationSeconds;
        }

        public String getFilename() {
            return filename;
        }

        public String getName() {
            // ファイル名から表示名を生成
            return filename.replace("voice_memo_", "").replace(".raw", "").replace("_", " ");
        }

        public Date getDate() {
            return date;
        }

        public String getDateString() {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd HH:mm");
            return sdf.format(date);
        }

        public String getDurationString() {
            if (durationSeconds > 0) {
                return String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60);
            }
            return "Unknown";
        }
    }
}
