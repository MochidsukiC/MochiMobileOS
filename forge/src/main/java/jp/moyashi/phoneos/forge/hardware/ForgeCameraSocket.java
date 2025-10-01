package jp.moyashi.phoneos.forge.hardware;

import jp.moyashi.phoneos.core.service.hardware.CameraSocket;
import processing.core.PImage;

/**
 * Forge環境用のカメラソケット実装。
 * Minecraftの画面映像をバイパスする。
 *
 * TODO: 将来的にMinecraftのフレームバッファをキャプチャする機能を実装
 */
public class ForgeCameraSocket implements CameraSocket {
    private boolean enabled;

    public ForgeCameraSocket() {
        this.enabled = false;
    }

    @Override
    public boolean isAvailable() {
        // TODO: 将来的にフレームバッファキャプチャが実装されたらtrueに
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        System.out.println("ForgeCameraSocket: Camera " + (enabled ? "enabled" : "disabled"));
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public PImage getCurrentFrame() {
        if (!enabled) {
            return null;
        }

        // TODO: MinecraftのフレームバッファをキャプチャしてPImageとして返す
        // 現時点では未実装
        return null;
    }
}