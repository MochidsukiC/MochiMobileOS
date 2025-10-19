package jp.moyashi.phoneos.forge.hardware;

import com.mojang.logging.LogUtils;
import jp.moyashi.phoneos.core.service.hardware.MicrophoneSocket;
import jp.moyashi.phoneos.forge.audio.AudioMixer;
import jp.moyashi.phoneos.forge.audio.JavaMicrophoneRecorder;
import jp.moyashi.phoneos.forge.audio.MinecraftAudioCapture;
import jp.moyashi.phoneos.forge.audio.VoicechatAudioCapture;
import org.slf4j.Logger;

/**
 * Forge環境用のマイクソケット実装。
 * Simple Voice Chat (SVC) MODがインストールされている場合のみ有効になる。
 *
 * 3つの独立したチャンネルを提供：
 * - チャンネル1: 自分のマイク入力（Java標準API）
 * - チャンネル2: 他プレイヤーのVC音声（SVC）
 * - チャンネル3: Minecraft環境音（未実装）
 *
 * ミキシングはOS側で行うため、各チャンネルは独立して取得可能。
 */
public class ForgeMicrophoneSocket implements MicrophoneSocket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean enabled;
    private final boolean svcAvailable;

    private JavaMicrophoneRecorder microphoneRecorder;
    private VoicechatAudioCapture voicechatCapture;
    private MinecraftAudioCapture environmentCapture;
    private AudioMixer audioMixer;

    public ForgeMicrophoneSocket() {
        this.enabled = false;
        this.svcAvailable = SVCDetector.isSVCAvailable();

        if (svcAvailable) {
            // 音声録音コンポーネントを初期化
            this.microphoneRecorder = new JavaMicrophoneRecorder();
            this.voicechatCapture = VoicechatAudioCapture.getInstance();
            this.environmentCapture = MinecraftAudioCapture.getInstance();
            this.audioMixer = new AudioMixer(microphoneRecorder, voicechatCapture);

            LOGGER.info("[ForgeMicrophoneSocket] Initialized with 3-channel audio support");
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
            // 全チャンネルの録音を開始
            audioMixer.startMixing();
            environmentCapture.startCapture();
            LOGGER.info("[ForgeMicrophoneSocket] All channels enabled - started 3-channel recording");
        } else {
            // 全チャンネルの録音を停止
            audioMixer.stopMixing();
            environmentCapture.stopCapture();
            LOGGER.info("[ForgeMicrophoneSocket] All channels disabled - stopped recording");
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

    @Override
    public byte[] getMicrophoneAudio() {
        if (!enabled || !svcAvailable) {
            return null;
        }

        // チャンネル1: 自分のマイク入力
        return microphoneRecorder.pollAllAudioData();
    }

    @Override
    public byte[] getVoicechatAudio() {
        if (!enabled || !svcAvailable) {
            return null;
        }

        // チャンネル2: VC音声（他プレイヤーの声 + 自分の声）
        // 他プレイヤーの声がキャプチャできない場合は、自分のマイク音声を返す
        byte[] vcData = voicechatCapture.pollAllAudioData();
        if (vcData != null && vcData.length > 0) {
            return vcData;
        }

        // VCデータが取得できない場合は、自分のマイク音声を返す
        // これにより、VCチャンネルだけを有効にした場合でも録音可能になる
        return microphoneRecorder.pollAllAudioData();
    }

    @Override
    public byte[] getEnvironmentAudio() {
        if (!enabled || !svcAvailable) {
            return null;
        }

        // チャンネル3: Minecraft環境音（未実装）
        return environmentCapture.pollAllAudioData();
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