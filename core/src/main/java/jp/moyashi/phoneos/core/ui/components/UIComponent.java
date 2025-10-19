package jp.moyashi.phoneos.core.ui.components;

import processing.core.PGraphics;

/**
 * すべてのUIコンポーネントの基底インターフェース。
 * MochiMobileOSのUIシステムにおける再利用可能なコンポーネントの基本機能を定義する。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public interface UIComponent {

    /**
     * コンポーネントを描画する。
     *
     * @param g PGraphics描画コンテキスト
     */
    void draw(PGraphics g);

    /**
     * コンポーネントの状態を更新する。
     * アニメーション、タイマー、状態遷移などの処理を行う。
     */
    void update();

    /**
     * コンポーネントのX座標を取得する。
     *
     * @return X座標
     */
    float getX();

    /**
     * コンポーネントのY座標を取得する。
     *
     * @return Y座標
     */
    float getY();

    /**
     * コンポーネントの幅を取得する。
     *
     * @return 幅
     */
    float getWidth();

    /**
     * コンポーネントの高さを取得する。
     *
     * @return 高さ
     */
    float getHeight();

    /**
     * コンポーネントの位置を設定する。
     *
     * @param x X座標
     * @param y Y座標
     */
    void setPosition(float x, float y);

    /**
     * コンポーネントのサイズを設定する。
     *
     * @param width 幅
     * @param height 高さ
     */
    void setSize(float width, float height);

    /**
     * コンポーネントが表示されるかどうかを取得する。
     *
     * @return 表示される場合true、非表示の場合false
     */
    boolean isVisible();

    /**
     * コンポーネントの表示/非表示を設定する。
     *
     * @param visible 表示する場合true、非表示にする場合false
     */
    void setVisible(boolean visible);

    /**
     * コンポーネントが有効かどうかを取得する。
     * 無効なコンポーネントはグレーアウト表示され、操作できない。
     *
     * @return 有効な場合true、無効な場合false
     */
    boolean isEnabled();

    /**
     * コンポーネントの有効/無効を設定する。
     *
     * @param enabled 有効にする場合true、無効にする場合false
     */
    void setEnabled(boolean enabled);

    /**
     * 指定された座標がコンポーネント内にあるかどうかを判定する。
     *
     * @param x X座標
     * @param y Y座標
     * @return コンポーネント内の場合true、外の場合false
     */
    default boolean contains(float x, float y) {
        return x >= getX() && x <= getX() + getWidth() &&
               y >= getY() && y <= getY() + getHeight();
    }
}
