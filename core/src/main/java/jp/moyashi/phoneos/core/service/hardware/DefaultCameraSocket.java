package jp.moyashi.phoneos.core.service.hardware;

import processing.core.PImage;

/**
 * カメラソケットのデフォルト実装（standalone用）。
 * 常にnullを返す。
 */
public class DefaultCameraSocket implements CameraSocket {
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
    public PImage getCurrentFrame() {
        return null;
    }
}