package jp.moyashi.phoneos.forge.hardware;

import com.mojang.logging.LogUtils;
import jp.moyashi.phoneos.core.service.hardware.MicrophoneSocket;
import jp.moyashi.phoneos.forge.audio.AudioMixer;
import jp.moyashi.phoneos.forge.audio.JavaMicrophoneRecorder;
import jp.moyashi.phoneos.forge.audio.VoicechatAudioCapture;
import org.slf4j.Logger;

/**
 * Forge環境用のマイクソケット実装。
 * Simple Voice Chat (SVC) MODがインストールされている場合のみ有効になる。
 *
 * 実装方法:
 * - 自分の声: Java標準APIでマイクから直接録音
 * - 他プレイヤーの声: SVCのClientReceiveSoundEventで受信
 * - 両方をAudioMixerでミックスして提供
 */
public class ForgeMicrophoneSocket implements MicrophoneSocket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean enabled;
    private final boolean svcAvailable;

    private JavaMicrophoneRecorder microphoneRecorder;
    private VoicechatAudioCapture audioCapture;
    private AudioMixer audioMixer;

    public ForgeMicrophoneSocket() {
        this.enabled = false;
        this.svcAvailable = SVCDetector.isSVCAvailable();

        if (svcAvailable) {
            // 音声録音コンポーネントを初期化
            this.microphoneRecorder = new JavaMicrophoneRecorder();
            this.audioCapture = VoicechatAudioCapture.getInstance();
            this.audioMixer = new AudioMixer(microphoneRecorder, audioCapture);

            LOGGER.info("[ForgeMicrophoneSocket] Initialized with SVC support and audio recording");
        } else {
            LOGGER.info("[ForgeMicrophoneSocket] Initialized without SVC (microphone unavailable)");
        }
    }

    @Override
    public boolean isAvailable() {
        // SVCが導入されている場合のみ利用可能
        return svcAvailable;
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (!svcAvailable) {
            LOGGER.warn("[ForgeMicrophoneSocket] Cannot enable microphone - SVC not installed");
            return;
        }

        this.enabled = enabled;

        if (enabled) {
            // 録音を開始
            audioMixer.startMixing();
            LOGGER.info("[ForgeMicrophoneSocket] Microphone enabled - started recording");
        } else {
            // 録音を停止
            audioMixer.stopMixing();
            LOGGER.info("[ForgeMicrophoneSocket] Microphone disabled - stopped recording");
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled && svcAvailable;
    }

    @Override
    public byte[] getAudioData() {
        if (!enabled || !svcAvailable) {
            return null;
        }

        // AudioMixerからミックスされた音声データを取得
        // 自分の声（Java標準API）+ 他プレイヤーの声（SVC）
        // バッファ内の全チャンクを取得してデータ欠落を防ぐ
        return audioMixer.getAllMixedAudio();
    }

    /**
     * 現在のサンプリングレートを取得する。
     * @return サンプリングレート（Hz）、利用できない場合は48000
     */
    public float getSampleRate() {
        if (!svcAvailable || audioMixer == null) {
            return 48000.0f;
        }
        return audioMixer.getSampleRate();
    }
}