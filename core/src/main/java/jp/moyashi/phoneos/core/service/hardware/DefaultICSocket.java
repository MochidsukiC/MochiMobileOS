package jp.moyashi.phoneos.core.service.hardware;

/**
 * IC通信ソケットのデフォルト実装（standalone用）。
 * 常にnullを返す。
 */
public class DefaultICSocket implements ICSocket {
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
    public ICData pollICData() {
        return new ICData(); // type=NONE
    }
}