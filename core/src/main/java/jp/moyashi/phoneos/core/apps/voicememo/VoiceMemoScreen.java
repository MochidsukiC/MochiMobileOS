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
    private float currentInputLevel = 0.0f; // 現在の入力レベル (0.0 - 1.0)
    private int totalChunksReceived = 0;    // 受信したチャンク数
    private float recordingSampleRate = 48000.0f; // 録音時のサンプリングレート

    // 再生状態
    private boolean isPlaying = false;
    private int playingIndex = -1;
    private float currentOutputLevel = 0.0f; // 現在の出力レベル (0.0 - 1.0)
    private byte[] playingAudioData = null;  // 再生中の音声データ
    private int playingChunkIndex = 0;        // 現在の再生チャンクインデックス
    private long playingStartTime = 0;        // 再生開始時刻
    private static final int PLAYBACK_CHUNK_SIZE = 2048; // チャンクサイズ（約21ms @ 48kHz）

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

    /**
     * ロガーヘルパーメソッド。
     */
    private void log(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().debug("VoiceMemoScreen", message);
        }
        System.out.println("[VoiceMemoScreen] " + message);
    }

    private void logError(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("VoiceMemoScreen", message);
        }
        System.err.println("[VoiceMemoScreen] " + message);
    }

    private void logError(String message, Throwable throwable) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("VoiceMemoScreen", message, throwable);
        }
        System.err.println("[VoiceMemoScreen] " + message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void setup(PGraphics g) {
        log("Setup");
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

        // 録音中の入力レベルメーター
        if (isRecording) {
            g.fill(200, 200, 200);
            g.text("Input Level:", 30, y + 50);

            // レベルメーターバー
            g.noStroke();
            g.fill(50, 50, 60);
            g.rect(120, y + 50, 240, 12, 2);

            // 現在のレベル（緑→黄色→赤）
            if (currentInputLevel > 0) {
                float barWidth = currentInputLevel * 240;
                int r = (int) (currentInputLevel * 255);
                int gr = (int) ((1.0f - currentInputLevel) * 255);
                g.fill(r, gr, 0);
                g.rect(120, y + 50, barWidth, 12, 2);
            }

            // チャンク受信数
            g.fill(180, 180, 180);
            g.textSize(10);
            g.text("Chunks: " + totalChunksReceived, 30, y + 68);
        }

        // 再生中の出力レベルメーター
        if (isPlaying) {
            g.fill(200, 200, 200);
            g.textSize(11);
            g.text("Output Level:", 30, y + 50);

            // レベルメーターバー
            g.noStroke();
            g.fill(50, 50, 60);
            g.rect(120, y + 50, 240, 12, 2);

            // 現在のレベル（青→シアン）
            if (currentOutputLevel > 0) {
                float barWidth = currentOutputLevel * 240;
                g.fill(0, 150, 255);
                g.rect(120, y + 50, barWidth, 12, 2);
            }
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
        log("mousePressed: (" + mouseX + ", " + mouseY + ")");

        // 録音ボタン
        if (isInside(mouseX, mouseY, 50, 150, 120, 40)) {
            log("Record button clicked - mic.available=" + microphone.isAvailable() + ", isPlaying=" + isPlaying);
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
        currentInputLevel = 0.0f;
        totalChunksReceived = 0;
        recordingSampleRate = microphone.getSampleRate();
        showStatus("Recording started...");
        log("Recording started - sample rate: " + recordingSampleRate + " Hz");
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

        log("Recording stopped");
    }

    private void updateRecording() {
        // マイクからデータを取得（バッファ内の全チャンクを連結して取得）
        byte[] audioData = microphone.getAudioData();
        if (audioData != null && audioData.length > 0) {
            recordingBuffer.add(audioData);
            totalChunksReceived++;

            // 音声レベルを計算（16-bit PCM）
            currentInputLevel = calculateAudioLevel(audioData);
        } else {
            // データがない場合はレベルを徐々に下げる
            currentInputLevel *= 0.8f;
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

        // Base64エンコードしてメタデータと共にJSON形式で保存
        String encodedData = Base64.getEncoder().encodeToString(fullData);
        long duration = (System.currentTimeMillis() - recordingStartTime) / 1000;

        // JSON形式のメタデータ
        String jsonData = String.format(
            "{\"version\":\"1.0\",\"sampleRate\":%.1f,\"duration\":%d,\"audioData\":\"%s\"}",
            recordingSampleRate,
            duration,
            encodedData
        );

        vfs.writeFile("voice_memos/" + filename, jsonData);

        // メモリストに追加
        VoiceMemo memo = new VoiceMemo(filename, new Date(), duration, recordingSampleRate);
        memos.add(memo);

        log("Saved memo: " + filename + " (" + totalSize + " bytes, " + recordingSampleRate + " Hz)");
    }

    private void loadMemos() {
        memos.clear();

        // VFSからメモファイルを読み込み
        // 注: VFSはファイル一覧を取得するメソッドがないため、ここでは空リストとして開始
        // メモは保存時にリストに追加される

        log("Loaded " + memos.size() + " memos");
    }

    public void playMemo(int index) {
        log("playMemo() called - index: " + index + ", memos.size: " + memos.size() + ", speaker.isAvailable: " + speaker.isAvailable());

        if (index < 0 || index >= memos.size() || !speaker.isAvailable()) {
            log("playMemo() conditions not met - returning");
            return;
        }

        VoiceMemo memo = memos.get(index);
        String fileData = vfs.readFile("voice_memos/" + memo.getFilename());

        if (fileData == null || fileData.isEmpty()) {
            showStatus("Failed to load memo");
            return;
        }

        // JSONデータをパースして音声データとサンプリングレートを取得
        byte[] audioData;
        float sampleRate = memo.getSampleRate();

        try {
            // JSON形式かどうかをチェック（バージョン1.0のフォーマット）
            if (fileData.startsWith("{")) {
                // JSON形式: メタデータを含む
                int audioDataStart = fileData.indexOf("\"audioData\":\"") + 13;
                int audioDataEnd = fileData.lastIndexOf("\"");
                String encodedData = fileData.substring(audioDataStart, audioDataEnd);

                // サンプリングレートを取得
                int sampleRateStart = fileData.indexOf("\"sampleRate\":") + 13;
                int sampleRateEnd = fileData.indexOf(",", sampleRateStart);
                sampleRate = Float.parseFloat(fileData.substring(sampleRateStart, sampleRateEnd));

                // Base64デコード
                audioData = Base64.getDecoder().decode(encodedData);
            } else {
                // 旧形式: Base64のみ
                audioData = Base64.getDecoder().decode(fileData);
                sampleRate = 48000.0f; // デフォルト
            }
        } catch (Exception e) {
            showStatus("Failed to decode memo");
            logError("Failed to decode: " + e.getMessage(), e);
            return;
        }

        log("Original sample rate: " + sampleRate + " Hz");

        // 48kHz以外の場合はリサンプリング
        if (Math.abs(sampleRate - 48000.0f) > 0.1f) {
            log("Resampling from " + sampleRate + " Hz to 48000 Hz...");
            audioData = resampleAudio(audioData, sampleRate, 48000.0f);
            log("Resampled to " + audioData.length + " bytes");
        }

        isPlaying = true;
        playingIndex = index;
        playingAudioData = audioData;
        playingChunkIndex = 0;
        playingStartTime = System.currentTimeMillis();
        currentOutputLevel = 0.0f;
        speaker.playAudio(audioData);
        showStatus("Playing: " + memo.getName());

        log("Playing memo: " + memo.getFilename() + " (" + audioData.length + " bytes)");
    }

    public void stopPlayback() {
        if (!isPlaying) {
            return;
        }

        isPlaying = false;
        playingIndex = -1;
        playingAudioData = null;
        playingChunkIndex = 0;
        currentOutputLevel = 0.0f;
        speaker.stopAudio();
        showStatus("Playback stopped");

        log("Playback stopped");
    }

    private void updatePlayback() {
        if (!isPlaying || playingAudioData == null) {
            return;
        }

        // 経過時間から現在の再生位置を計算
        // 48kHz, 16-bit, mono = 96000 bytes/sec
        long elapsedMs = System.currentTimeMillis() - playingStartTime;
        int estimatedBytePosition = (int) (elapsedMs * 96); // 96 bytes/ms

        // 現在位置のチャンクの音量レベルを計算
        if (estimatedBytePosition < playingAudioData.length) {
            int chunkStart = estimatedBytePosition;
            int chunkEnd = Math.min(chunkStart + PLAYBACK_CHUNK_SIZE, playingAudioData.length);
            int chunkSize = chunkEnd - chunkStart;

            if (chunkSize > 0) {
                byte[] chunk = new byte[chunkSize];
                System.arraycopy(playingAudioData, chunkStart, chunk, 0, chunkSize);
                currentOutputLevel = calculateAudioLevel(chunk);
            }
        } else {
            // 再生完了
            stopPlayback();
        }
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
        log("Deleted memo: " + memo.getFilename());
    }

    private void showStatus(String message) {
        statusMessage = message;
        statusMessageTime = System.currentTimeMillis();
    }

    /**
     * 音声データを指定したサンプリングレートにリサンプリングする。
     * 線形補間を使用した単純なリサンプリング。
     *
     * @param audioData 元の音声データ（16-bit PCM）
     * @param sourceSampleRate 元のサンプリングレート
     * @param targetSampleRate 目標のサンプリングレート
     * @return リサンプリングされた音声データ
     */
    private byte[] resampleAudio(byte[] audioData, float sourceSampleRate, float targetSampleRate) {
        // サンプル数を計算（16-bit = 2 bytes per sample）
        int sourceSampleCount = audioData.length / 2;
        int targetSampleCount = (int) (sourceSampleCount * targetSampleRate / sourceSampleRate);

        // リサンプリング比率
        float ratio = sourceSampleRate / targetSampleRate;

        // 出力バッファ
        byte[] output = new byte[targetSampleCount * 2];

        // 線形補間でリサンプリング
        for (int i = 0; i < targetSampleCount; i++) {
            // 元の音声データでの位置（浮動小数点）
            float sourcePos = i * ratio;
            int sourceIndex = (int) sourcePos;
            float fraction = sourcePos - sourceIndex;

            // 元の音声データから2つのサンプルを取得
            short sample1 = 0, sample2 = 0;

            if (sourceIndex * 2 + 1 < audioData.length) {
                sample1 = (short) ((audioData[sourceIndex * 2 + 1] << 8) | (audioData[sourceIndex * 2] & 0xFF));
            }

            if ((sourceIndex + 1) * 2 + 1 < audioData.length) {
                sample2 = (short) ((audioData[(sourceIndex + 1) * 2 + 1] << 8) | (audioData[(sourceIndex + 1) * 2] & 0xFF));
            } else {
                sample2 = sample1; // 最後のサンプルの場合
            }

            // 線形補間
            short interpolated = (short) (sample1 + fraction * (sample2 - sample1));

            // 出力バッファに書き込み
            output[i * 2] = (byte) (interpolated & 0xFF);
            output[i * 2 + 1] = (byte) ((interpolated >> 8) & 0xFF);
        }

        return output;
    }

    /**
     * 音声データから音量レベルを計算する（RMS）。
     * @param audioData 16-bit PCMの音声データ
     * @return 0.0〜1.0の範囲のレベル
     */
    private float calculateAudioLevel(byte[] audioData) {
        if (audioData == null || audioData.length < 2) {
            return 0.0f;
        }

        // RMS (Root Mean Square) を計算
        long sum = 0;
        int sampleCount = audioData.length / 2; // 16-bit = 2 bytes per sample

        for (int i = 0; i < audioData.length - 1; i += 2) {
            // 16-bit PCMサンプルを読み取る（リトルエンディアン）
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            sum += sample * sample;
        }

        double rms = Math.sqrt((double) sum / sampleCount);

        // 正規化（16-bit PCMの最大値は32767）
        float normalized = (float) (rms / 32767.0);

        // デシベルに変換して視覚的に見やすくする
        // -60dB〜0dBの範囲を0.0〜1.0にマッピング
        if (normalized > 0.001f) {
            float db = (float) (20.0 * Math.log10(normalized));
            float level = Math.max(0.0f, (db + 60.0f) / 60.0f);
            return Math.min(1.0f, level);
        }

        return 0.0f;
    }

    /**
     * ボイスメモのデータモデル。
     */
    private static class VoiceMemo {
        private String filename;
        private Date date;
        private long durationSeconds;
        private float sampleRate;

        public VoiceMemo(String filename, Date date, long durationSeconds, float sampleRate) {
            this.filename = filename;
            this.date = date;
            this.durationSeconds = durationSeconds;
            this.sampleRate = sampleRate;
        }

        public String getFilename() {
            return filename;
        }

        public float getSampleRate() {
            return sampleRate;
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
