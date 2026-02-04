package jp.moyashi.phoneos.core.apps.voicememo;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.VFS;
import jp.moyashi.phoneos.core.service.hardware.MicrophoneSocket;
import jp.moyashi.phoneos.core.service.hardware.SpeakerSocket;
import jp.moyashi.phoneos.core.time.Time;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.components.Button;
import jp.moyashi.phoneos.core.ui.components.Checkbox;
import jp.moyashi.phoneos.core.ui.components.Label;
import jp.moyashi.phoneos.core.ui.components.ProgressBar;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.io.ByteArrayOutputStream;
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
    private long lastRecordingUpdateTime = 0; // 最後に録音データを取得した時刻
    private static final long RECORDING_UPDATE_INTERVAL_MS = 20; // 録音データ取得間隔（ms）

    // チャンネル選択状態
    private boolean enableMicChannel = true;      // チャンネル1: マイク入力
    private boolean enableVoicechatChannel = true; // チャンネル2: VC音声
    private boolean enableEnvironmentChannel = false; // チャンネル3: 環境音（未実装のためデフォルトOFF）

    // チャンネル別蓄積バッファ（リングバッファ方式で同期）
    private ByteArrayOutputStream micAccumBuffer = new ByteArrayOutputStream();
    private ByteArrayOutputStream vcAccumBuffer = new ByteArrayOutputStream();
    private ByteArrayOutputStream envAccumBuffer = new ByteArrayOutputStream();

    // 再生状態
    private boolean isPlaying = false;
    private int playingIndex = -1;
    private float currentOutputLevel = 0.0f; // 現在の出力レベル (0.0 - 1.0)
    private byte[] playingAudioData = null;  // 再生中の音声データ
    private int playingChunkIndex = 0;        // 現在の再生チャンクインデックス
    private long playingStartTime = 0;        // 再生開始時刻（フォールバック用）
    private long playingStartFrame = 0;       // 再生開始フレーム（Choreographer連携用）
    private static final int PLAYBACK_CHUNK_SIZE = 1920; // チャンクサイズ（20ms @ 48kHz 16-bit mono）
    private static final int BYTES_PER_FRAME = 1600;     // 60fps @ 48kHz 16-bit mono = 96000 bytes/sec / 60

    // メモ一覧
    private List<VoiceMemo> memos = new ArrayList<>();
    private int scrollOffset = 0;
    private static final int ITEM_HEIGHT = 60;

    // UI状態
    private String statusMessage = "";
    private long statusMessageTime = 0;

    // UIコンポーネント
    private Label headerLabel;
    private Label micStatusLabel;
    private Label speakerStatusLabel;
    private Label volumeLabel;
    private Label inputLevelLabel;
    private Label outputLevelLabel;
    private Label recordingTimeLabel;
    private Label channelTitleLabel;
    private Label statusLabel;
    private Label memoListTitleLabel;

    private Button recordButton;
    private Button playLastButton;
    private Button volumeLowButton;
    private Button volumeMedButton;
    private Button volumeHighButton;
    private Button volumeOffButton;

    private Checkbox micChannelCheckbox;
    private Checkbox voicechatChannelCheckbox;
    private Checkbox environmentChannelCheckbox;

    private ProgressBar inputLevelMeter;
    private ProgressBar outputLevelMeter;

    private List<Button> memoPlayButtons = new ArrayList<>();
    private List<Button> memoDeleteButtons = new ArrayList<>();

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
        log("Setup - Initializing UI components");

        // ヘッダーラベル
        headerLabel = new Label(0, 20, 400, 30, "🎤 Voice Memo");
        headerLabel.setTextSize(20);
        headerLabel.setHorizontalAlign(PApplet.CENTER);
        headerLabel.setVerticalAlign(PApplet.TOP);
        headerLabel.setTextColor(0xFFFFFFFF);

        // ハードウェアステータスラベル
        micStatusLabel = new Label(30, 70, 340, 15, "");
        micStatusLabel.setTextSize(11);
        micStatusLabel.setHorizontalAlign(PApplet.LEFT);
        micStatusLabel.setTextColor(0xFF64FF64);

        speakerStatusLabel = new Label(30, 85, 340, 15, "");
        speakerStatusLabel.setTextSize(11);
        speakerStatusLabel.setHorizontalAlign(PApplet.LEFT);
        speakerStatusLabel.setTextColor(0xFF64FF64);

        volumeLabel = new Label(30, 100, 340, 15, "");
        volumeLabel.setTextSize(11);
        volumeLabel.setHorizontalAlign(PApplet.LEFT);
        volumeLabel.setTextColor(0xFFC8C8C8);

        // 録音/再生コントロールボタン
        recordButton = new Button(50, 150, 120, 40, "⏺ Record");
        recordButton.setBackgroundColor(0xFFFF5050);
        recordButton.setHoverColor(0xFFFF6060);
        recordButton.setPressColor(0xFFFF4040);
        recordButton.setTextColor(0xFFFFFFFF);
        recordButton.setCornerRadius(5);
        recordButton.setOnClickListener(this::handleRecordButton);

        playLastButton = new Button(230, 150, 120, 40, "▶ Play Last");
        playLastButton.setBackgroundColor(0xFF64FF64);
        playLastButton.setHoverColor(0xFF74FF74);
        playLastButton.setPressColor(0xFF54FF54);
        playLastButton.setTextColor(0xFFFFFFFF);
        playLastButton.setCornerRadius(5);
        playLastButton.setOnClickListener(this::handlePlayLastButton);

        // 音量調整ボタン
        volumeLowButton = new Button(50, 200, 80, 30, "🔉 Low");
        volumeLowButton.setBackgroundColor(0xFF9696C8);
        volumeLowButton.setHoverColor(0xFFA6A6D8);
        volumeLowButton.setPressColor(0xFF8686B8);
        volumeLowButton.setTextColor(0xFFFFFFFF);
        volumeLowButton.setCornerRadius(3);
        volumeLowButton.setOnClickListener(() -> {
            speaker.setVolumeLevel(SpeakerSocket.VolumeLevel.LOW);
            showStatus("Volume: Low");
        });

        volumeMedButton = new Button(140, 200, 80, 30, "🔊 Med");
        volumeMedButton.setBackgroundColor(0xFF9696C8);
        volumeMedButton.setHoverColor(0xFFA6A6D8);
        volumeMedButton.setPressColor(0xFF8686B8);
        volumeMedButton.setTextColor(0xFFFFFFFF);
        volumeMedButton.setCornerRadius(3);
        volumeMedButton.setOnClickListener(() -> {
            speaker.setVolumeLevel(SpeakerSocket.VolumeLevel.MEDIUM);
            showStatus("Volume: Medium");
        });

        volumeHighButton = new Button(230, 200, 80, 30, "📢 High");
        volumeHighButton.setBackgroundColor(0xFF9696C8);
        volumeHighButton.setHoverColor(0xFFA6A6D8);
        volumeHighButton.setPressColor(0xFF8686B8);
        volumeHighButton.setTextColor(0xFFFFFFFF);
        volumeHighButton.setCornerRadius(3);
        volumeHighButton.setOnClickListener(() -> {
            speaker.setVolumeLevel(SpeakerSocket.VolumeLevel.HIGH);
            showStatus("Volume: High");
        });

        volumeOffButton = new Button(320, 200, 80, 30, "🔇 Off");
        volumeOffButton.setBackgroundColor(0xFF9696C8);
        volumeOffButton.setHoverColor(0xFFA6A6D8);
        volumeOffButton.setPressColor(0xFF8686B8);
        volumeOffButton.setTextColor(0xFFFFFFFF);
        volumeOffButton.setCornerRadius(3);
        volumeOffButton.setOnClickListener(() -> {
            speaker.setVolumeLevel(SpeakerSocket.VolumeLevel.OFF);
            showStatus("Volume: Off");
        });

        // 録音時間ラベル
        recordingTimeLabel = new Label(0, 240, 400, 20, "");
        recordingTimeLabel.setTextSize(14);
        recordingTimeLabel.setHorizontalAlign(PApplet.CENTER);
        recordingTimeLabel.setTextColor(0xFFFF6464);

        // レベルメーター用ラベル
        inputLevelLabel = new Label(30, 120, 90, 15, "Input Level:");
        inputLevelLabel.setTextSize(11);
        inputLevelLabel.setHorizontalAlign(PApplet.LEFT);
        inputLevelLabel.setTextColor(0xFFC8C8C8);

        outputLevelLabel = new Label(30, 120, 90, 15, "Output Level:");
        outputLevelLabel.setTextSize(11);
        outputLevelLabel.setHorizontalAlign(PApplet.LEFT);
        outputLevelLabel.setTextColor(0xFFC8C8C8);

        // レベルメーター
        inputLevelMeter = new ProgressBar(120, 120, 240, 12);
        inputLevelMeter.setMinValue(0.0f);
        inputLevelMeter.setMaxValue(1.0f);
        inputLevelMeter.setValue(0.0f);
        inputLevelMeter.setFillColor(0xFF00FF00);
        inputLevelMeter.setBackgroundColor(0xFF323C3C);
        inputLevelMeter.setShowPercentage(false);
        inputLevelMeter.setCornerRadius(2);

        outputLevelMeter = new ProgressBar(120, 120, 240, 12);
        outputLevelMeter.setMinValue(0.0f);
        outputLevelMeter.setMaxValue(1.0f);
        outputLevelMeter.setValue(0.0f);
        outputLevelMeter.setFillColor(0xFF0096FF);
        outputLevelMeter.setBackgroundColor(0xFF323C3C);
        outputLevelMeter.setShowPercentage(false);
        outputLevelMeter.setCornerRadius(2);

        // チャンネル選択
        channelTitleLabel = new Label(50, 260, 300, 15, "Recording Channels:");
        channelTitleLabel.setTextSize(12);
        channelTitleLabel.setHorizontalAlign(PApplet.LEFT);
        channelTitleLabel.setTextColor(0xFFC8C8C8);

        micChannelCheckbox = new Checkbox(50, 280, "🎤 Microphone");
        micChannelCheckbox.setChecked(enableMicChannel);
        micChannelCheckbox.setOnChangeListener(checked -> {
            enableMicChannel = checked;
            log("Mic channel: " + enableMicChannel);
        });

        voicechatChannelCheckbox = new Checkbox(200, 280, "👥 Voicechat");
        voicechatChannelCheckbox.setChecked(enableVoicechatChannel);
        voicechatChannelCheckbox.setOnChangeListener(checked -> {
            enableVoicechatChannel = checked;
            log("Voicechat channel: " + enableVoicechatChannel);
        });

        environmentChannelCheckbox = new Checkbox(50, 305, "🔊 Environment (Not implemented)");
        environmentChannelCheckbox.setChecked(enableEnvironmentChannel);
        environmentChannelCheckbox.setOnChangeListener(checked -> {
            enableEnvironmentChannel = checked;
            log("Environment channel: " + enableEnvironmentChannel);
        });

        // メモリストタイトル
        memoListTitleLabel = new Label(30, 340, 340, 20, "");
        memoListTitleLabel.setTextSize(14);
        memoListTitleLabel.setHorizontalAlign(PApplet.LEFT);
        memoListTitleLabel.setTextColor(0xFFFFFFFF);

        // ステータスラベル
        statusLabel = new Label(0, 570, 400, 20, "");
        statusLabel.setTextSize(11);
        statusLabel.setHorizontalAlign(PApplet.CENTER);
        statusLabel.setTextColor(0xFF64C8FF);
    }

    @Override
    public void tick() {
        // 録音中の処理
        // 注: マイクはサンプリングレートに応じた間隔でデータを生成するため、
        // Choreographerの有無に関わらずポーリング間隔チェックを維持
        if (isRecording) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRecordingUpdateTime >= RECORDING_UPDATE_INTERVAL_MS) {
                updateRecording();
                lastRecordingUpdateTime = currentTime;
            }
        }

            // 再生中の処理（ストリーミング）
        // 注: スピーカーの再生は実時間ベースで進むため、常に時間ベースの計算を使用
        // Choreographerの恩恵は安定した呼び出しタイミングのみ
        if (isPlaying) {
            streamAudioPlayback();
        }
    }

    @Override
    public void draw(PGraphics g) {
        // 背景
        g.background(30, 30, 40);

        // ヘッダー
        headerLabel.draw(g);

        // セパレーター
        g.stroke(100, 100, 100);
        g.line(20, 60, 380, 60);

        // ハードウェアステータス
        updateAndDrawHardwareStatus(g);

        // 録音/再生コントロール
        updateAndDrawControls(g);

        // チャンネル選択
        channelTitleLabel.draw(g);
        micChannelCheckbox.draw(g);
        voicechatChannelCheckbox.draw(g);
        environmentChannelCheckbox.draw(g);

        // メモ一覧
        drawMemoList(g);

        // ステータスメッセージ
        if (System.currentTimeMillis() - statusMessageTime < 3000) {
            statusLabel.setText(statusMessage);
            statusLabel.draw(g);
        }

        // 再生中のレベルメーター更新
        if (isPlaying) {
            updatePlaybackLevelMeter();
        }
    }
    
    // 再生位置（バイト単位）
    private int playingByteOffset = 0;
    // これまでにスピーカーに送信したバイト数
    private int bytesSentToSpeaker = 0;

    /**
     * 音声再生をストリーミングで行う。
     * バッファ枯渇を防ぎつつ、スピーカーのバッファ溢れも防ぐようにデータを供給する。
     */
    private void streamAudioPlayback() {
        if (playingAudioData == null) return;

        // 1. 消費されたバイト数を推定 (48kHz 16bit mono = 96 bytes/ms)
        long elapsedMs = System.currentTimeMillis() - playingStartTime;
        int estimatedConsumedBytes = (int) (elapsedMs * 96);
        
        // 2. 現在スピーカーバッファにあると推定される量
        int bufferedBytes = bytesSentToSpeaker - estimatedConsumedBytes;
        
        // 3. バッファ目標量 (約0.5秒分 = 48000 bytes)
        int targetBufferBytes = 48000;
        
        // 4. データが必要なら供給
        if (bufferedBytes < targetBufferBytes) {
            int bytesNeeded = targetBufferBytes - bufferedBytes;
            int remainingData = playingAudioData.length - playingByteOffset;
            
            // 一度に送る最大サイズ制限（スパイク防止）
            int bytesToSend = Math.min(bytesNeeded, 4096 * 4); 
            bytesToSend = Math.min(bytesToSend, remainingData);
            
            if (bytesToSend > 0) {
                byte[] chunk = new byte[bytesToSend];
                System.arraycopy(playingAudioData, playingByteOffset, chunk, 0, bytesToSend);
                speaker.playAudio(chunk);
                
                playingByteOffset += bytesToSend;
                bytesSentToSpeaker += bytesToSend;
                
                // デバッグログ（稀に出力）
                if (playingByteOffset % (96000 * 5) < bytesToSend) {
                    log("Streaming: sent " + bytesToSend + " bytes, offset=" + playingByteOffset + "/" + playingAudioData.length);
                }
            }
        }
        
        // 5. 再生完了判定
        // 全データを送信済みで、かつ推定消費量がデータ長を超えたら終了
        if (playingByteOffset >= playingAudioData.length && estimatedConsumedBytes >= playingAudioData.length) {
            stopPlayback();
        }
    }

    /**
     * フレームベースの音声再生ストリーミング（Choreographer連携版）。
     * Timeクラスを使用して正確なフレーム数から消費バイト数を計算する。
     *
     * @param time Timeインスタンス
     */
    private void streamAudioPlaybackWithTime(Time time) {
        if (playingAudioData == null) return;

        // フレームベースで消費バイト数を計算
        // 60fps @ 48kHz 16-bit mono = 1600 bytes/frame
        long framesSinceStart = time.getTotalFrameCount() - playingStartFrame;
        int estimatedConsumedBytes = (int) (framesSinceStart * BYTES_PER_FRAME);

        // バッファ残量を計算
        int bufferedBytes = bytesSentToSpeaker - estimatedConsumedBytes;

        // ターゲットバッファ量（約0.5秒分 = 48000 bytes）
        int targetBufferBytes = 48000;

        // バッファが不足していれば供給
        if (bufferedBytes < targetBufferBytes) {
            int bytesNeeded = targetBufferBytes - bufferedBytes;
            int remainingData = playingAudioData.length - playingByteOffset;

            // 一度に送る最大サイズ制限（スパイク防止）
            int bytesToSend = Math.min(bytesNeeded, 16384);
            bytesToSend = Math.min(bytesToSend, remainingData);

            if (bytesToSend > 0) {
                byte[] chunk = new byte[bytesToSend];
                System.arraycopy(playingAudioData, playingByteOffset, chunk, 0, bytesToSend);
                speaker.playAudio(chunk);

                playingByteOffset += bytesToSend;
                bytesSentToSpeaker += bytesToSend;

                // デバッグログ（約5秒ごと = 300フレーム）
                if (framesSinceStart % 300 == 0 && framesSinceStart > 0) {
                    log(String.format("Streaming (frame-based): sent %d bytes, offset=%d/%d, frames=%d",
                            bytesToSend, playingByteOffset, playingAudioData.length, framesSinceStart));
                }
            }
        }

        // 再生完了判定
        if (playingByteOffset >= playingAudioData.length &&
            estimatedConsumedBytes >= playingAudioData.length) {
            stopPlayback();
        }
    }

    private void updatePlaybackLevelMeter() {
        if (!isPlaying || playingAudioData == null) {
            return;
        }

        // 経過時間から現在の再生位置を計算
        long elapsedMs = System.currentTimeMillis() - playingStartTime;
        int estimatedBytePosition = (int) (elapsedMs * 96);

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
        }
    }

    private void updateAndDrawHardwareStatus(PGraphics g) {
        // マイク状態
        boolean micAvailable = microphone.isAvailable();
        micStatusLabel.setText("🎤 Microphone: " + (micAvailable ? "Available" : "Not Available"));
        micStatusLabel.setTextColor(micAvailable ? 0xFF64FF64 : 0xFFFF6464);
        micStatusLabel.draw(g);

        // スピーカー状態
        boolean speakerAvailable = speaker.isAvailable();
        speakerStatusLabel.setText("🔊 Speaker: " + (speakerAvailable ? "Available" : "Not Available"));
        speakerStatusLabel.setTextColor(speakerAvailable ? 0xFF64FF64 : 0xFFFF6464);
        speakerStatusLabel.draw(g);

        // 音量レベル
        if (speakerAvailable) {
            volumeLabel.setText("Volume: " + speaker.getVolumeLevel().name());
            volumeLabel.draw(g);
        }

        // 録音中の入力レベルメーター
        if (isRecording) {
            inputLevelLabel.draw(g);
            inputLevelMeter.setValue(currentInputLevel);
            // レベルに応じて色を変更（緑→黄→赤）
            if (currentInputLevel < 0.5f) {
                inputLevelMeter.setFillColor(0xFF00FF00);
            } else if (currentInputLevel < 0.8f) {
                inputLevelMeter.setFillColor(0xFFFFFF00);
            } else {
                inputLevelMeter.setFillColor(0xFFFF0000);
            }
            inputLevelMeter.draw(g);

            // チャンク受信数
            g.fill(180, 180, 180);
            g.textSize(10);
            g.textAlign(PApplet.LEFT, PApplet.TOP);
            g.text("Chunks: " + totalChunksReceived, 30, 138);
        }

        // 再生中の出力レベルメーター
        if (isPlaying) {
            outputLevelLabel.draw(g);
            outputLevelMeter.setValue(currentOutputLevel);
            outputLevelMeter.draw(g);
        }
    }

    private void updateAndDrawControls(PGraphics g) {
        // 録音ボタンの状態を更新
        boolean canRecord = microphone.isAvailable() && !isPlaying;
        recordButton.setText(isRecording ? "⏹ Stop" : "⏺ Record");
        recordButton.setEnabled(canRecord || isRecording);
        if (!canRecord && !isRecording) {
            recordButton.setBackgroundColor(0xFF646464);
        } else {
            recordButton.setBackgroundColor(0xFFFF5050);
        }
        recordButton.draw(g);

        // 再生ボタンの状態を更新
        boolean canPlay = speaker.isAvailable() && !memos.isEmpty() && !isRecording;
        playLastButton.setText(isPlaying ? "⏹ Stop" : "▶ Play Last");
        playLastButton.setEnabled(canPlay || isPlaying);
        if (!canPlay && !isPlaying) {
            playLastButton.setBackgroundColor(0xFF323232);
        } else if (isPlaying) {
            playLastButton.setBackgroundColor(0xFFFF6464);
        } else {
            playLastButton.setBackgroundColor(0xFF64FF64);
        }
        playLastButton.draw(g);

        // 音量調整ボタン
        if (speaker.isAvailable()) {
            volumeLowButton.draw(g);
            volumeMedButton.draw(g);
            volumeHighButton.draw(g);
            volumeOffButton.draw(g);
        }

        // 録音時間表示
        if (isRecording) {
            long duration = (System.currentTimeMillis() - recordingStartTime) / 1000;
            recordingTimeLabel.setText(String.format("⏱ %02d:%02d", duration / 60, duration % 60));
            recordingTimeLabel.draw(g);
        }
    }


    private void drawMemoList(PGraphics g) {
        int listY = 340;
        int listHeight = 230;

        // タイトル更新
        memoListTitleLabel.setText("Saved Memos (" + memos.size() + ")");
        memoListTitleLabel.draw(g);

        // セパレーター
        g.stroke(100, 100, 100);
        g.line(30, listY + 25, 370, listY + 25);

        // メモ一覧
        g.pushMatrix();
        g.translate(0, listY + 30);

        // メモリストボタンを動的生成（必要に応じて）
        while (memoPlayButtons.size() < memos.size()) {
            int index = memoPlayButtons.size();
            Button playBtn = new Button(280, 0, 40, 30, "▶");
            playBtn.setBackgroundColor(0xFF64C864);
            playBtn.setTextColor(0xFFFFFFFF);
            playBtn.setCornerRadius(3);
            final int capturedIndex = index;
            playBtn.setOnClickListener(() -> {
                if (speaker.isAvailable() && !isRecording) {
                    playMemo(capturedIndex);
                }
            });
            memoPlayButtons.add(playBtn);

            Button deleteBtn = new Button(330, 0, 30, 30, "🗑");
            deleteBtn.setBackgroundColor(0xFFC86464);
            deleteBtn.setTextColor(0xFFFFFFFF);
            deleteBtn.setCornerRadius(3);
            deleteBtn.setOnClickListener(() -> deleteMemo(capturedIndex));
            memoDeleteButtons.add(deleteBtn);
        }

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
            g.textAlign(PApplet.LEFT, PApplet.TOP);
            g.text(memo.getName(), 40, itemY + 10);

            // 詳細情報
            g.fill(180, 180, 180);
            g.textSize(10);
            g.text(memo.getDateString() + " | " + memo.getDurationString(), 40, itemY + 30);

            // ボタンの位置を更新して描画
            if (speaker.isAvailable() && i < memoPlayButtons.size()) {
                Button playBtn = memoPlayButtons.get(i);
                playBtn.setPosition(280, itemY + 10);
                playBtn.draw(g);
            }

            if (i < memoDeleteButtons.size()) {
                Button deleteBtn = memoDeleteButtons.get(i);
                deleteBtn.setPosition(330, itemY + 10);
                deleteBtn.draw(g);
            }
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

    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        // コンポーネントのmousePressedイベント
        if (recordButton.onMousePressed(mouseX, mouseY)) return;
        if (playLastButton.onMousePressed(mouseX, mouseY)) return;

        if (speaker.isAvailable()) {
            if (volumeLowButton.onMousePressed(mouseX, mouseY)) return;
            if (volumeMedButton.onMousePressed(mouseX, mouseY)) return;
            if (volumeHighButton.onMousePressed(mouseX, mouseY)) return;
            if (volumeOffButton.onMousePressed(mouseX, mouseY)) return;
        }

        // チャンネル選択チェックボックス
        if (micChannelCheckbox.onMousePressed(mouseX, mouseY)) return;
        if (voicechatChannelCheckbox.onMousePressed(mouseX, mouseY)) return;
        if (environmentChannelCheckbox.onMousePressed(mouseX, mouseY)) return;

        // メモ一覧のボタン（translate考慮）
        int listY = 340 + 30;
        for (int i = 0; i < memos.size(); i++) {
            int itemY = listY + i * ITEM_HEIGHT - scrollOffset;

            if (i < memoPlayButtons.size()) {
                Button playBtn = memoPlayButtons.get(i);
                if (playBtn.onMousePressed(mouseX, mouseY - listY + scrollOffset)) return;
            }

            if (i < memoDeleteButtons.size()) {
                Button deleteBtn = memoDeleteButtons.get(i);
                if (deleteBtn.onMousePressed(mouseX, mouseY - listY + scrollOffset)) return;
            }
        }
    }

    @Override
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        // コンポーネントのmouseReleasedイベント
        recordButton.onMouseReleased(mouseX, mouseY);
        playLastButton.onMouseReleased(mouseX, mouseY);

        if (speaker.isAvailable()) {
            volumeLowButton.onMouseReleased(mouseX, mouseY);
            volumeMedButton.onMouseReleased(mouseX, mouseY);
            volumeHighButton.onMouseReleased(mouseX, mouseY);
            volumeOffButton.onMouseReleased(mouseX, mouseY);
        }

        micChannelCheckbox.onMouseReleased(mouseX, mouseY);
        voicechatChannelCheckbox.onMouseReleased(mouseX, mouseY);
        environmentChannelCheckbox.onMouseReleased(mouseX, mouseY);

        // メモ一覧のボタン
        int listY = 340 + 30;
        for (int i = 0; i < memos.size(); i++) {
            if (i < memoPlayButtons.size()) {
                memoPlayButtons.get(i).onMouseReleased(mouseX, mouseY - listY + scrollOffset);
            }
            if (i < memoDeleteButtons.size()) {
                memoDeleteButtons.get(i).onMouseReleased(mouseX, mouseY - listY + scrollOffset);
            }
        }
    }

    public void mouseMoved(PGraphics g, int mouseX, int mouseY) {
        // コンポーネントのhoverイベント
        recordButton.onMouseMoved(mouseX, mouseY);
        playLastButton.onMouseMoved(mouseX, mouseY);

        if (speaker.isAvailable()) {
            volumeLowButton.onMouseMoved(mouseX, mouseY);
            volumeMedButton.onMouseMoved(mouseX, mouseY);
            volumeHighButton.onMouseMoved(mouseX, mouseY);
            volumeOffButton.onMouseMoved(mouseX, mouseY);
        }

        micChannelCheckbox.onMouseMoved(mouseX, mouseY);
        voicechatChannelCheckbox.onMouseMoved(mouseX, mouseY);
        environmentChannelCheckbox.onMouseMoved(mouseX, mouseY);

        // メモ一覧のボタン
        int listY = 340 + 30;
        for (int i = 0; i < memos.size(); i++) {
            if (i < memoPlayButtons.size()) {
                memoPlayButtons.get(i).onMouseMoved(mouseX, mouseY - listY + scrollOffset);
            }
            if (i < memoDeleteButtons.size()) {
                memoDeleteButtons.get(i).onMouseMoved(mouseX, mouseY - listY + scrollOffset);
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

    private void handleRecordButton() {
        log("Record button clicked - mic.available=" + microphone.isAvailable() + ", isPlaying=" + isPlaying);
        if (microphone.isAvailable() && !isPlaying) {
            if (isRecording) {
                stopRecording();
            } else {
                startRecording();
            }
        }
    }

    private void handlePlayLastButton() {
        if (speaker.isAvailable() && !memos.isEmpty() && !isRecording) {
            if (isPlaying) {
                stopPlayback();
            } else {
                playMemo(memos.size() - 1);
            }
        }
    }

    public void startRecording() {
        if (!microphone.isAvailable()) {
            showStatus("Microphone not available");
            return;
        }

        microphone.setEnabled(true);
        isRecording = true;
        recordingBuffer.clear();

        // チャンネル別蓄積バッファをクリア
        micAccumBuffer.reset();
        vcAccumBuffer.reset();
        envAccumBuffer.reset();

        recordingStartTime = System.currentTimeMillis();
        lastRecordingUpdateTime = recordingStartTime; // 録音更新タイマーを初期化
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
        // 各チャンネルからデータを取得
        byte[] micData = enableMicChannel ? microphone.getMicrophoneAudio() : null;
        byte[] vcData = enableVoicechatChannel ? microphone.getVoicechatAudio() : null;
        byte[] envData = enableEnvironmentChannel ? microphone.getEnvironmentAudio() : null;

        // データがある場合のみバッファに追加（無音挿入は行わない）
        if (enableMicChannel && micData != null && micData.length > 0) {
            micAccumBuffer.write(micData, 0, micData.length);
        }

        if (enableVoicechatChannel && vcData != null && vcData.length > 0) {
            vcAccumBuffer.write(vcData, 0, vcData.length);
        }

        if (enableEnvironmentChannel && envData != null && envData.length > 0) {
            envAccumBuffer.write(envData, 0, envData.length);
        }

        // 有効なチャンネルの蓄積量から最小サイズを計算
        int minSize = Integer.MAX_VALUE;
        int enabledChannelCount = 0;

        if (enableMicChannel && micAccumBuffer.size() > 0) {
            minSize = Math.min(minSize, micAccumBuffer.size());
            enabledChannelCount++;
        }
        if (enableVoicechatChannel && vcAccumBuffer.size() > 0) {
            minSize = Math.min(minSize, vcAccumBuffer.size());
            enabledChannelCount++;
        }
        if (enableEnvironmentChannel && envAccumBuffer.size() > 0) {
            minSize = Math.min(minSize, envAccumBuffer.size());
            enabledChannelCount++;
        }

        // チャンネルがない場合
        if (enabledChannelCount == 0 || minSize == Integer.MAX_VALUE) {
            currentInputLevel *= 0.8f;
            return;
        }

        // 最小チャンクサイズ: 20ms @ 48kHz 16-bit mono = 1920 bytes
        int minChunkSize = 1920;
        if (minSize < minChunkSize) {
            // まだ十分なデータがない
            return;
        }

        // デバッグログ（250回に1回 = 約5秒ごと @ 20ms間隔）
        if (totalChunksReceived % 250 == 0) {
            log(String.format("Buffer sizes - Mic: %d, VC: %d, Env: %d, extracting: %d bytes",
                micAccumBuffer.size(), vcAccumBuffer.size(), envAccumBuffer.size(), minSize));
        }

        // 各バッファから同じ長さのデータを取り出し
        byte[] micChunk = enableMicChannel ? extractFromBuffer(micAccumBuffer, minSize) : null;
        byte[] vcChunk = enableVoicechatChannel ? extractFromBuffer(vcAccumBuffer, minSize) : null;
        byte[] envChunk = enableEnvironmentChannel ? extractFromBuffer(envAccumBuffer, minSize) : null;

        // 同じ長さのチャンネルをミキシング
        byte[] mixedData = mixEqualLengthChannels(micChunk, vcChunk, envChunk);

        if (mixedData != null && mixedData.length > 0) {
            recordingBuffer.add(mixedData);
            totalChunksReceived++;
            currentInputLevel = calculateAudioLevel(mixedData);
        } else {
            currentInputLevel *= 0.8f;
        }
    }

    /**
     * 蓄積バッファから指定されたバイト数を取り出す。
     * 取り出し後、残りのデータはバッファに保持される。
     *
     * @param buffer 蓄積バッファ
     * @param length 取り出すバイト数
     * @return 取り出したデータ、またはnull
     */
    private byte[] extractFromBuffer(ByteArrayOutputStream buffer, int length) {
        if (buffer.size() < length) {
            return null;
        }

        byte[] data = buffer.toByteArray();
        byte[] extracted = new byte[length];
        System.arraycopy(data, 0, extracted, 0, length);

        // 残りのデータをバッファに戻す
        buffer.reset();
        if (data.length > length) {
            buffer.write(data, length, data.length - length);
        }

        return extracted;
    }

    /**
     * 同じ長さの音声チャンネルをミキシングする。
     * リングバッファ方式により、すべてのチャンネルが同じ長さであることが保証される。
     *
     * @param mic マイクチャンネルのデータ
     * @param vc ボイスチャットチャンネルのデータ
     * @param env 環境音チャンネルのデータ
     * @return ミキシングされたデータ
     */
    private byte[] mixEqualLengthChannels(byte[] mic, byte[] vc, byte[] env) {
        // 有効なチャンネルを収集
        List<byte[]> channels = new ArrayList<>();
        if (enableMicChannel && mic != null) channels.add(mic);
        if (enableVoicechatChannel && vc != null) channels.add(vc);
        if (enableEnvironmentChannel && env != null) channels.add(env);

        if (channels.isEmpty()) {
            return null;
        }

        if (channels.size() == 1) {
            return channels.get(0);
        }

        // 全て同じ長さのはず
        int length = channels.get(0).length;
        byte[] mixed = new byte[length];

        for (int i = 0; i < length; i += 2) {
            int mixedSample = 0;

            for (byte[] ch : channels) {
                if (i + 1 < ch.length) {
                    short sample = (short) ((ch[i + 1] << 8) | (ch[i] & 0xFF));
                    mixedSample += sample;
                }
            }

            // クリッピング防止
            mixedSample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixedSample));

            // バイト配列に書き戻し
            mixed[i] = (byte) (mixedSample & 0xFF);
            mixed[i + 1] = (byte) ((mixedSample >> 8) & 0xFF);
        }

        return mixed;
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
        playingByteOffset = 0;
        bytesSentToSpeaker = 0;
        currentOutputLevel = 0.0f;

        // 開始時刻/フレームを記録
        // playingStartTimeは常に設定（updatePlaybackLevelMeter()で使用）
        playingStartTime = System.currentTimeMillis();

        Time time = kernel.getTime();
        if (time != null) {
            playingStartFrame = time.getTotalFrameCount();
            log("Playback started at frame " + playingStartFrame);
        }

        // 初回バッファ充填
        streamAudioPlayback();
        
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
        playingByteOffset = 0;
        bytesSentToSpeaker = 0;
        currentOutputLevel = 0.0f;
        speaker.stopAudio();
        showStatus("Playback stopped");

        log("Playback stopped");
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
