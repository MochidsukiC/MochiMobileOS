package jp.moyashi.phoneos.core.ui.components;

/**
 * スクロール可能なUIコンポーネントのインターフェース。
 * コンテンツのスクロール、スクロールバー表示などの機能を提供する。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public interface Scrollable extends UIComponent {

    /**
     * 現在のスクロールオフセットを取得する。
     *
     * @return スクロールオフセット（ピクセル）
     */
    int getScrollOffset();

    /**
     * スクロールオフセットを設定する。
     *
     * @param offset スクロールオフセット（ピクセル）
     */
    void setScrollOffset(int offset);

    /**
     * スクロール可能な最大オフセットを取得する。
     *
     * @return 最大スクロールオフセット
     */
    int getMaxScrollOffset();

    /**
     * スクロールする。
     *
     * @param delta スクロール量（正の値で下方向、負の値で上方向）
     */
    void scroll(int delta);

    /**
     * スクロールバーを表示するかどうかを取得する。
     *
     * @return スクロールバーを表示する場合true
     */
    default boolean isScrollBarVisible() {
        return getMaxScrollOffset() > 0;
    }

    /**
     * マウスドラッグによるスクロールイベントを処理する。
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @param pmouseX 前フレームのマウスX座標
     * @param pmouseY 前フレームのマウスY座標
     */
    default void onMouseDragged(int mouseX, int mouseY, int pmouseX, int pmouseY) {
        int deltaY = mouseY - pmouseY;
        scroll(-deltaY);
    }
}
