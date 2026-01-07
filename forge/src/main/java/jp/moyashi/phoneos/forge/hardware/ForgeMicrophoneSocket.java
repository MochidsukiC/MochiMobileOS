package jp.moyashi.phoneos.forge.hardware;

import com.mojang.logging.LogUtils;
import jp.moyashi.phoneos.core.service.hardware.MicrophoneSocket;
import jp.moyashi.phoneos.forge.audio.AVCAudioBridge;
import jp.moyashi.phoneos.forge.audio.JavaMicrophoneRecorder;
import jp.moyashi.phoneos.forge.audio.MinecraftAudioCapture;
import org.slf4j.Logger;

/**
 * Forge環境用のマイクソケット実装。
 * AdvancedVC 2.0 (AVC) MODがインストールされている場合のみ有効になる。
 *
 * 3つの独立したチャンネルを提供：
 * - チャンネル1: 自分のマイク入力（Java標準API）
 * - チャンネル2: 他プレイヤーのVC音声（AVC経由）
 * - チャンネル3: Minecraft環境音（未実装）
 *
 * ミキシングはOS側で行うため、各チャンネルは独立して取得可能。
 */
public class ForgeMicrophoneSocket implements MicrophoneSocket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean enabled;
    private final boolean avcAvailable;

    private JavaMicrophoneRecorder microphoneRecorder;
    private MinecraftAudioCapture environmentCapture;
    private AVCAudioBridge avcBridge;

    public ForgeMicrophoneSocket() {
        this.enabled = false;
        this.avcAvailable = AVCDetector.isAVCAvailable();

        // 音声録音コンポーネントを初期化（AVCの有無に関わらず）
        this.microphoneRecorder = new JavaMicrophoneRecorder();
        this.environmentCapture = MinecraftAudioCapture.getInstance();

        if (avcAvailable) {
            this.avcBridge = AVCAudioBridge.getInstance();
            LOGGER.info("[ForgeMicrophoneSocket] Initialized with AVC support");
        } else {
            LOGGER.info("[ForgeMicrophoneSocket] Initialized without AVC (AVC features disabled)");
        }
    }

    @Override
    public boolean isAvailable() {
        // マイクは常に利用可能（AVC連携はオプション）
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        if (enabled) {
            // マイク録音を開始
            microphoneRecorder.startRecording();
            environmentCapture.startCapture();
            LOGGER.info("[ForgeMicrophoneSocket] Microphone enabled - started recording");

            // AVCブリッジを初期化して有効化（遅延初期化）
            if (avcBridge != null) {
                if (avcBridge.tryInitialize()) {
                    avcBridge.setEnabled(true);
                    LOGGER.info("[ForgeMicrophoneSocket] AdvancedVC audio bridge initialized and enabled");
                } else {
                    LOGGER.info("[ForgeMicrophoneSocket] AdvancedVC VoiceClient not ready - bridge will retry later");
                }
            }
        } else {
            // マイク録音を停止
            microphoneRecorder.stopRecording();
            environmentCapture.stopCapture();
            LOGGER.info("[ForgeMicrophoneSocket] Microphone disabled - stopped recording");

            // AVCブリッジを無効化
            if (avcBridge != null) {
                avcBridge.setEnabled(false);
                LOGGER.info("[ForgeMicrophoneSocket] AdvancedVC audio bridge disabled");
            }
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public byte[] getAudioData() {
        if (!enabled) {
            return null;
        }

        // マイク入力のみを返す（ミキシングはOS側で行う）
        return microphoneRecorder.pollAllAudioData();
    }

    @Override
    public byte[] getMicrophoneAudio() {
        if (!enabled) {
            return null;
        }

        // チャンネル1: 自分のマイク入力
        return microphoneRecorder.pollAllAudioData();
    }

    @Override
    public byte[] getVoicechatAudio() {
        if (!enabled) {
            return null;
        }

        // チャンネル2: VC音声（他プレイヤーの声）
        // AVCブリッジから他プレイヤーの音声を取得
        if (avcBridge != null) {
            // 遅延初期化を試行
            if (!avcBridge.isInitialized()) {
                if (avcBridge.tryInitialize()) {
                    avcBridge.setEnabled(true);
                    LOGGER.info("[ForgeMicrophoneSocket] AdvancedVC audio bridge initialized (deferred) in getVoicechatAudio");
                }
            }
            if (avcBridge.isEnabled()) {
                byte[] avcAudio = avcBridge.pollAllVoicechatAudio();
                if (avcAudio != null && avcAudio.length > 0) {
                    return avcAudio;
                }
            }
        }

        // AVCからデータが取得できない場合はnullを返す
        return null;
    }

    @Override
    public byte[] getEnvironmentAudio() {
        if (!enabled) {
            return null;
        }

        // チャンネル3: Minecraft環境音
        return environmentCapture.pollAllAudioData();
    }

    /**
     * 現在のサンプリングレートを取得する。
     * @return サンプリングレート（Hz）、利用できない場合は48000
     */
    public float getSampleRate() {
        if (microphoneRecorder == null) {
            return 48000.0f;
        }
        return microphoneRecorder.getSampleRate();
    }

    /**
     * 使用するマイクデバイスを設定する。
     * 録音中の場合は自動的に再起動して新しいデバイスを使用する。
     *
     * @param mixerInfo Mixer.Info、nullの場合はシステムデフォルト
     */
    public void setSelectedMixer(javax.sound.sampled.Mixer.Info mixerInfo) {
        if (microphoneRecorder != null) {
            boolean wasRecording = microphoneRecorder.isRecording();

            // 録音中の場合は一度停止
            if (wasRecording) {
                microphoneRecorder.stopRecording();
                LOGGER.info("[ForgeMicrophoneSocket] Stopped recording to switch device");
            }

            // 新しいデバイスを設定
            microphoneRecorder.setSelectedMixer(mixerInfo);
            LOGGER.info("[ForgeMicrophoneSocket] Selected mixer: {}",
                mixerInfo != null ? mixerInfo.getName() : "System Default");

            // 録音中だった場合は再開
            if (wasRecording) {
                microphoneRecorder.startRecording();
                LOGGER.info("[ForgeMicrophoneSocket] Restarted recording with new device");
            }
        }
    }
}