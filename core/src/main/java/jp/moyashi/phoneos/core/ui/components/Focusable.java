package jp.moyashi.phoneos.core.ui.components;

/**
 * フォーカス可能なUIコンポーネントのインターフェース。
 * キーボード入力やタブ操作によるフォーカス管理機能を提供する。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public interface Focusable extends UIComponent {

    /**
     * コンポーネントがフォーカスされているかどうかを取得する。
     *
     * @return フォーカス中の場合true
     */
    boolean isFocused();

    /**
     * コンポーネントのフォーカス状態を設定する。
     *
     * @param focused フォーカスする場合true、解除する場合false
     */
    void setFocused(boolean focused);

    /**
     * フォーカスを要求する。
     */
    default void requestFocus() {
        setFocused(true);
    }

    /**
     * フォーカスを解除する。
     */
    default void clearFocus() {
        setFocused(false);
    }

    /**
     * キープレスイベントを処理する。
     *
     * @param key 押されたキー
     * @param keyCode キーコード
     * @return イベントを処理した場合true
     */
    boolean onKeyPressed(char key, int keyCode);

    /**
     * フォーカス取得時に呼ばれるコールバック。
     */
    default void onFocusGained() {
        // オプション機能：デフォルトは何もしない
    }

    /**
     * フォーカス喪失時に呼ばれるコールバック。
     */
    default void onFocusLost() {
        // オプション機能：デフォルトは何もしない
    }
}
