package jp.moyashi.phoneos.core.ui.components;

import java.util.function.Consumer;

/**
 * クリック可能なUIコンポーネントのインターフェース。
 * マウスクリック、ホバー、プレスイベントを処理する機能を提供する。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public interface Clickable extends UIComponent {

    /**
     * マウスがコンポーネント上にあるかどうかを取得する。
     *
     * @return ホバー中の場合true
     */
    boolean isHovered();

    /**
     * コンポーネントがプレス（押下）されているかどうかを取得する。
     *
     * @return プレス中の場合true
     */
    boolean isPressed();

    /**
     * マウスプレスイベントを処理する。
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @return イベントを処理した場合true
     */
    boolean onMousePressed(int mouseX, int mouseY);

    /**
     * マウスリリースイベントを処理する。
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @return イベントを処理した場合true
     */
    boolean onMouseReleased(int mouseX, int mouseY);

    /**
     * マウス移動イベントを処理する（ホバー検出用）。
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     */
    void onMouseMoved(int mouseX, int mouseY);

    /**
     * クリックイベントリスナーを設定する。
     *
     * @param listener クリック時に実行されるコールバック
     */
    void setOnClickListener(Runnable listener);

    /**
     * ロングプレスイベントリスナーを設定する。
     *
     * @param listener ロングプレス時に実行されるコールバック
     */
    default void setOnLongPressListener(Runnable listener) {
        // オプション機能：デフォルトは何もしない
    }
}
