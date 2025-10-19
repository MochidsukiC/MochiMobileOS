package jp.moyashi.phoneos.core.ui.components;

import java.util.List;

/**
 * 子要素を持つことができるUIコンポーネントのインターフェース。
 * レイアウト管理、子要素の追加/削除などの機能を提供する。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public interface Container extends UIComponent {

    /**
     * 子要素を追加する。
     *
     * @param child 追加する子要素
     */
    void addChild(UIComponent child);

    /**
     * 子要素を削除する。
     *
     * @param child 削除する子要素
     */
    void removeChild(UIComponent child);

    /**
     * すべての子要素を削除する。
     */
    void removeAllChildren();

    /**
     * 子要素のリストを取得する。
     *
     * @return 子要素のリスト（読み取り専用）
     */
    List<UIComponent> getChildren();

    /**
     * 子要素の数を取得する。
     *
     * @return 子要素の数
     */
    default int getChildCount() {
        return getChildren().size();
    }

    /**
     * 指定されたインデックスの子要素を取得する。
     *
     * @param index インデックス
     * @return 子要素
     */
    default UIComponent getChild(int index) {
        return getChildren().get(index);
    }

    /**
     * レイアウトを更新する。
     * 子要素の位置とサイズを再計算する。
     */
    void layout();
}
