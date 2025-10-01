package jp.moyashi.phoneos.core.service.hardware;

/**
 * マイクソケットのデフォルト実装（standalone用）。
 * 常にnullを返す。
 */
public class DefaultMicrophoneSocket implements MicrophoneSocket {
    private boolean enabled = false;

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public byte[] getAudioData() {
        return null;
    }
}