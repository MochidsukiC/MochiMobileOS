package jp.moyashi.phoneos.forge.hardware;

import com.mojang.logging.LogUtils;
import jp.moyashi.phoneos.core.service.hardware.MicrophoneSocket;
import org.slf4j.Logger;

/**
 * Forge環境用のマイクソケット実装。
 * Simple Voice Chat (SVC) MODがインストールされている場合のみ有効になる。
 * SVCが導入されていない場合は、isAvailable()がfalseを返す。
 *
 * 将来的な拡張:
 * - SVCのマイク入力をバイトストリームとして取得
 * - リアルタイム音声処理
 */
public class ForgeMicrophoneSocket implements MicrophoneSocket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private boolean enabled;
    private final boolean svcAvailable;

    public ForgeMicrophoneSocket() {
        this.enabled = false;
        this.svcAvailable = SVCDetector.isSVCAvailable();

        if (svcAvailable) {
            LOGGER.info("[ForgeMicrophoneSocket] Initialized with SVC support");
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
        LOGGER.info("[ForgeMicrophoneSocket] Microphone " + (enabled ? "enabled" : "disabled"));

        // TODO: SVCのマイクを有効/無効にするAPIを呼び出す
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

        // TODO: SVCのマイク入力をバイトストリームとして返す
        // 現時点では未実装（将来的な拡張ポイント）
        return null;
    }
}