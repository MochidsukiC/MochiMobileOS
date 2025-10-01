package jp.moyashi.phoneos.core.service.hardware;

import processing.core.PImage;

/**
 * カメラAPI。
 * オンオフのプロパティを持ち、オンの際はカメラの画像を毎フレームバイパスする。
 *
 * 環境別動作:
 * - standalone: nullを返す
 * - forge-mod: Minecraftの画面映像をそのままバイパス
 */
public interface CameraSocket {
    /**
     * カメラが利用可能かどうかを取得する。
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();

    /**
     * カメラを有効化する。
     *
     * @param enabled 有効にする場合true
     */
    void setEnabled(boolean enabled);

    /**
     * カメラが有効かどうかを取得する。
     *
     * @return 有効な場合true
     */
    boolean isEnabled();

    /**
     * 現在のカメラフレームを取得する。
     *
     * @return カメラ画像、利用不可の場合null
     */
    PImage getCurrentFrame();
}