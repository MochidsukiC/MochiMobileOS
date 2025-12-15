package jp.moyashi.phoneos.api.hardware;

/**
 * カメラAPI。
 * オンオフのプロパティを持ち、オンの際はカメラの画像を毎フレームバイパスする。
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
     * 現在のカメラフレームをピクセル配列として取得する。
     * ARGB形式の配列を返す。
     *
     * @return カメラ画像のピクセル配列、利用不可の場合null
     */
    int[] getCurrentFramePixels();

    /**
     * カメラフレームの幅を取得する。
     *
     * @return 幅（ピクセル）
     */
    int getFrameWidth();

    /**
     * カメラフレームの高さを取得する。
     *
     * @return 高さ（ピクセル）
     */
    int getFrameHeight();
}
