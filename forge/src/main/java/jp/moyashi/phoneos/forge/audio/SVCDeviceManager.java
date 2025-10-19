package jp.moyashi.phoneos.forge.audio;

import com.mojang.logging.LogUtils;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import org.slf4j.Logger;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import java.util.Arrays;

/**
 * Simple Voice Chat (SVC)のデバイス設定を管理するクラス。
 * SVCのAPIから現在選択されているマイクデバイスや音量設定を取得する。
 */
public class SVCDeviceManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static VoicechatClientApi clientApi;
    private static boolean initialized = false;

    /**
     * SVCDeviceManagerを初期化する。
     *
     * @param api VoicechatClientApiインスタンス
     */
    public static void initialize(VoicechatClientApi api) {
        clientApi = api;
        initialized = true;
        LOGGER.info("[SVCDeviceManager] Initialized with SVC Client API");
    }

    /**
     * 初期化されているかチェックする。
     */
    public static boolean isInitialized() {
        return initialized && clientApi != null;
    }

    /**
     * SVCで選択されているマイクデバイスをJava AudioSystemのMixerとして取得する。
     *
     * @return マイクデバイスのMixer.Info、見つからない場合はnull
     */
    public static Mixer.Info getMicrophoneDevice() {
        if (!isInitialized()) {
            LOGGER.warn("[SVCDeviceManager] Not initialized, cannot get microphone device");
            return null;
        }

        try {
            // SVCからマイクデバイス名を取得
            // 注: 実際のAPIメソッドは環境により異なる可能性があります
            // ここでは仮の実装として、デフォルトマイクを返します
            LOGGER.info("[SVCDeviceManager] Getting microphone device from SVC");

            // 利用可能なマイクデバイスを取得
            Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();
            LOGGER.info("[SVCDeviceManager] Found {} audio mixers", mixerInfos.length);

            // デフォルトマイクを返す（TODO: SVCの設定から取得）
            for (Mixer.Info mixerInfo : mixerInfos) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                // TargetDataLine（マイク入力）をサポートするミキサーを探す
                if (mixer.getTargetLineInfo().length > 0) {
                    LOGGER.info("[SVCDeviceManager] Using microphone device: {}", mixerInfo.getName());
                    return mixerInfo;
                }
            }

        } catch (Exception e) {
            LOGGER.error("[SVCDeviceManager] Failed to get microphone device from SVC", e);
        }

        return null;
    }

    /**
     * SVCのマイク音量設定を取得する。
     *
     * @return マイク音量（0.0〜1.0）、取得できない場合は1.0
     */
    public static float getMicrophoneVolume() {
        if (!isInitialized()) {
            return 1.0f;
        }

        try {
            // TODO: SVCのAPIからマイク音量を取得
            // 現時点ではデフォルト値を返す
            return 1.0f;
        } catch (Exception e) {
            LOGGER.error("[SVCDeviceManager] Failed to get microphone volume", e);
            return 1.0f;
        }
    }

    /**
     * SVCのマイク増幅設定を取得する。
     *
     * @return マイク増幅値、取得できない場合は1.0
     */
    public static float getMicrophoneAmplification() {
        if (!isInitialized()) {
            return 1.0f;
        }

        try {
            // TODO: SVCのAPIからマイク増幅を取得
            return 1.0f;
        } catch (Exception e) {
            LOGGER.error("[SVCDeviceManager] Failed to get microphone amplification", e);
            return 1.0f;
        }
    }

    /**
     * SVCのボイスアクティベーション（VAD）設定を取得する。
     *
     * @return VADが有効な場合true
     */
    public static boolean isVoiceActivationEnabled() {
        if (!isInitialized()) {
            return false;
        }

        try {
            // TODO: SVCのAPIからVAD設定を取得
            return false;
        } catch (Exception e) {
            LOGGER.error("[SVCDeviceManager] Failed to get voice activation setting", e);
            return false;
        }
    }

    /**
     * VoicechatClientApiインスタンスを取得する。
     */
    public static VoicechatClientApi getClientApi() {
        return clientApi;
    }
}
