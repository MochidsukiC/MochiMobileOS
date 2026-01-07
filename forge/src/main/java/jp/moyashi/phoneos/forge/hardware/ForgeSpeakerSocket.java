package jp.moyashi.phoneos.forge.hardware;

import com.mojang.logging.LogUtils;
import jp.moyashi.phoneos.core.service.hardware.SpeakerSocket;
import jp.moyashi.phoneos.forge.audio.AVCAudioBridge;
import jp.moyashi.phoneos.forge.audio.ByteRingBuffer;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Forge環境用のスピーカーソケット実装。
 * AdvancedVC 2.0 (AVC) MODがインストールされている場合のみ有効になる。
 * Java標準AudioAPIで自分用の音声を再生し、AVCで他プレイヤーに配信する。
 *
 * 設計変更:
 * - PlayAudioはノンブロッキングでリングバッファに書き込むのみ。
 * - バックグラウンドスレッドがSourceDataLineへの書き込みを担当。
 * - AVCへの送信もバッファリング経由で行う。
 */
public class ForgeSpeakerSocket implements SpeakerSocket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SAMPLE_RATE = 48000;
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1;

    private volatile VolumeLevel volumeLevel = VolumeLevel.MEDIUM;
    private final boolean avcAvailable;
    
    // 自分用再生バッファ（約1秒分）
    private final ByteRingBuffer playbackBuffer = new ByteRingBuffer(48000 * 2);
    
    // 再生ワーカー制御
    private final Thread workerThread;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private volatile long lastAudioSubmitTime = 0; // 最終音声送信時刻
    
    private AVCAudioBridge avcBridge;
    private SourceDataLine speakerLine;
    private Mixer.Info selectedMixerInfo = null;

    public ForgeSpeakerSocket() {
        this.avcAvailable = AVCDetector.isAVCAvailable();

        if (avcAvailable) {
            this.avcBridge = AVCAudioBridge.getInstance();
            LOGGER.info("[ForgeSpeakerSocket] Initialized with AVC support");
        } else {
            LOGGER.info("[ForgeSpeakerSocket] Initialized without AVC");
        }

        // 再生ワーカースレッドの開始
        this.workerThread = new Thread(this::playbackLoop, "MochiOS-SpeakerWorker");
        this.workerThread.setDaemon(true);
        this.workerThread.start();
    }

    public void setSelectedMixer(Mixer.Info mixerInfo) {
        this.selectedMixerInfo = mixerInfo;
        // ミキサー変更時はラインを再構築する必要があるため、一度閉じる
        closeSpeakerLine();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void setVolumeLevel(VolumeLevel level) {
        this.volumeLevel = level;
    }

    @Override
    public VolumeLevel getVolumeLevel() {
        return volumeLevel;
    }

    @Override
    public void playAudio(byte[] audioData) {
        if (audioData == null || audioData.length == 0) return;

        // 1. 自分用再生バッファに追加
        if (volumeLevel != VolumeLevel.OFF) {
            playbackBuffer.write(audioData, 0, audioData.length);
        }

        // 2. AVC用バッファに追加 (MEDIUM/HIGHのみ)
        if (avcAvailable && (volumeLevel == VolumeLevel.MEDIUM || volumeLevel == VolumeLevel.HIGH)) {
            lastAudioSubmitTime = System.currentTimeMillis();
            
            // AVCブリッジの初期化チェック
            if (!avcBridge.isInitialized()) {
                avcBridge.tryInitialize();
            }
            if (avcBridge.isInitialized() && !avcBridge.isEnabled()) {
                avcBridge.setEnabled(true);
            }
            
            avcBridge.submitSpeakerAudio(audioData);
        }
    }

    @Override
    public void stopAudio() {
        // バッファをクリアして再生を即時停止
        playbackBuffer.clear();
        if (speakerLine != null) {
            speakerLine.flush();
        }
        
        // AVC送信も即時停止
        if (avcAvailable && avcBridge != null && avcBridge.isEnabled()) {
            avcBridge.setEnabled(false);
            LOGGER.info("[ForgeSpeakerSocket] AVC bridge disabled via stopAudio()");
        }
    }
    
    /**
     * バックグラウンドで音声再生を行うループ
     */
    private void playbackLoop() {
        byte[] buffer = new byte[1024]; // 読み出し用テンポラリバッファ
        
        while (running.get()) {
            try {
                // AVC送信の自動停止監視（500ms以上データ供給がなければ無効化）
                if (avcAvailable && avcBridge != null && avcBridge.isEnabled()) {
                    long idleTime = System.currentTimeMillis() - lastAudioSubmitTime;
                    if (idleTime > 500) {
                        avcBridge.setEnabled(false);
                        LOGGER.info("[ForgeSpeakerSocket] AVC bridge disabled due to inactivity (>500ms)");
                    }
                }

                // バッファにデータがない場合は少し待機
                if (playbackBuffer.available() == 0) {
                    Thread.sleep(5);
                    continue;
                }

                // スピーカーラインの準備
                if (speakerLine == null || !speakerLine.isOpen()) {
                    if (!openSpeakerLine()) {
                        // 開けない場合はデータを捨てて待機
                        playbackBuffer.clear();
                        Thread.sleep(1000);
                        continue;
                    }
                }

                // バッファから読み出して再生
                int read = playbackBuffer.read(buffer, 0, buffer.length);
                if (read > 0) {
                    // 音量がOFFになった場合のガード（バッファには残っている可能性がある）
                    if (volumeLevel != VolumeLevel.OFF) {
                        speakerLine.write(buffer, 0, read);
                    }
                }
                
            } catch (Exception e) {
                LOGGER.error("[ForgeSpeakerSocket] Error in playback loop", e);
                try {
                    Thread.sleep(1000); // エラー時は少し待機
                } catch (InterruptedException ignored) {}
            }
        }
        
        closeSpeakerLine();
    }

    private boolean openSpeakerLine() {
        try {
            AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE_BITS, CHANNELS, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);

            if (selectedMixerInfo != null) {
                try {
                    Mixer mixer = AudioSystem.getMixer(selectedMixerInfo);
                    if (mixer.isLineSupported(info)) {
                        speakerLine = (SourceDataLine) mixer.getLine(info);
                        speakerLine.open(format);
                        speakerLine.start();
                        return true;
                    }
                } catch (Exception e) {
                    LOGGER.warn("[ForgeSpeakerSocket] Selected mixer failed, fallback to default", e);
                }
            }

            if (!AudioSystem.isLineSupported(info)) {
                return false;
            }

            speakerLine = (SourceDataLine) AudioSystem.getLine(info);
            speakerLine.open(format);
            speakerLine.start();
            return true;

        } catch (LineUnavailableException e) {
            LOGGER.error("[ForgeSpeakerSocket] Failed to open speaker line", e);
            return false;
        }
    }

    private void closeSpeakerLine() {
        if (speakerLine != null) {
            speakerLine.stop();
            speakerLine.close();
            speakerLine = null;
        }
    }
    
    // アプリケーション終了時などに呼び出す（必要であれば）
    public void shutdown() {
        running.set(false);
        try {
            workerThread.join(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
