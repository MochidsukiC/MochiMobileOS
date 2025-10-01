package jp.moyashi.phoneos.core.service.hardware;

/**
 * 位置情報ソケットのデフォルト実装（standalone用）。
 * 常に0,0,0を返す。
 */
public class DefaultLocationSocket implements LocationSocket {
    private boolean enabled = true;

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public LocationData getLocation() {
        return new LocationData(0, 0, 0, 0.0f);
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}