package jp.moyashi.phoneos.core.ui.components;

/**
 * UIコンポーネントの基本実装を提供する抽象クラス。
 * 共通の状態管理機能を実装する。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public abstract class BaseComponent implements UIComponent {

    protected float x;
    protected float y;
    protected float width;
    protected float height;
    protected boolean visible = true;
    protected boolean enabled = true;

    /**
     * コンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     */
    public BaseComponent(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public float getX() {
        return x;
    }

    @Override
    public float getY() {
        return y;
    }

    @Override
    public float getWidth() {
        return width;
    }

    @Override
    public float getHeight() {
        return height;
    }

    @Override
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public void update() {
        // デフォルトは何もしない
        // 必要に応じてサブクラスでオーバーライド
    }
}
