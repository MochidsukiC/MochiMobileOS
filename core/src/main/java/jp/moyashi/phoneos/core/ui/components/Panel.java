package jp.moyashi.phoneos.core.ui.components;

import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * コンテナパネルコンポーネント。
 * 子要素をグループ化し、背景・枠線を提供。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class Panel extends BaseComponent implements Container {

    private List<UIComponent> children;
    private int backgroundColor;
    private int borderColor;
    private float cornerRadius;
    private float borderWidth;
    private float padding;
    
    // カスタム設定フラグ
    private boolean isCustomBackgroundColor = false;
    private boolean isCustomBorderColor = false;
    private boolean isCustomCornerRadius = false;

    /**
     * コンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     */
    public Panel(float x, float y, float width, float height) {
        super(x, y, width, height);
        this.children = new ArrayList<>();
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            this.backgroundColor = theme.colorSurface();
            this.borderColor = theme.colorBorder();
            this.cornerRadius = theme.radiusMd();
        } else {
            this.backgroundColor = 0xFFFFFFFF;
            this.borderColor = 0xFFCCCCCC;
            this.cornerRadius = 5;
        }
        this.borderWidth = 1;
        this.padding = 10;
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        g.pushStyle();

        // 動的切替に追従（カスタム設定優先）
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            if (!isCustomBackgroundColor) this.backgroundColor = theme.colorSurface();
            if (!isCustomBorderColor) this.borderColor = theme.colorBorder();
            if (!isCustomCornerRadius) this.cornerRadius = theme.radiusMd();
        }

        // 背景
        int alpha = 200; // Semi-transparent alpha for holographic effect
        g.fill((backgroundColor>>16)&0xFF, (backgroundColor>>8)&0xFF, backgroundColor&0xFF, alpha);
        g.noStroke();
        if (cornerRadius > 0) {
            g.rect(x, y, width, height, cornerRadius);
        } else {
            g.rect(x, y, width, height);
        }

        // 枠線
        if (borderWidth > 0) {
            g.noFill();
            g.stroke(borderColor);
            g.strokeWeight(borderWidth);
            if (cornerRadius > 0) {
                g.rect(x, y, width, height, cornerRadius);
            } else {
                g.rect(x, y, width, height);
            }
        }

        g.popStyle();

        // 子要素描画
        for (UIComponent child : children) {
            if (child.isVisible()) {
                child.draw(g);
            }
        }
    }

    @Override
    public void update() {
        for (UIComponent child : children) {
            child.update();
        }
    }

    @Override
    public void addChild(UIComponent child) {
        children.add(child);
    }

    @Override
    public void removeChild(UIComponent child) {
        children.remove(child);
    }

    @Override
    public void removeAllChildren() {
        children.clear();
    }

    @Override
    public List<UIComponent> getChildren() {
        return new ArrayList<>(children);
    }

    @Override
    public void layout() {
        // デフォルトは何もしない
        // レイアウトマネージャーを使用するか、
        // サブクラスでオーバーライドする
    }

    /**
     * マウスプレスイベントを子要素に伝播する。
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @return イベントを処理した場合true
     */
    public boolean onMousePressed(int mouseX, int mouseY) {
        if (!enabled || !visible) return false;

        // 子要素から順に処理（後ろから = 描画順の逆）
        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child instanceof Clickable && child.isEnabled() && child.isVisible()) {
                if (((Clickable) child).onMousePressed(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * マウスリリースイベントを子要素に伝播する。
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @return イベントを処理した場合true
     */
    public boolean onMouseReleased(int mouseX, int mouseY) {
        if (!enabled || !visible) return false;

        for (int i = children.size() - 1; i >= 0; i--) {
            UIComponent child = children.get(i);
            if (child instanceof Clickable && child.isEnabled() && child.isVisible()) {
                if (((Clickable) child).onMouseReleased(mouseX, mouseY)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * マウス移動イベントを子要素に伝播する。
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     */
    public void onMouseMoved(int mouseX, int mouseY) {
        for (UIComponent child : children) {
            if (child instanceof Clickable && child.isEnabled() && child.isVisible()) {
                ((Clickable) child).onMouseMoved(mouseX, mouseY);
            }
        }
    }

    // Getter/Setter

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        this.isCustomBackgroundColor = true;
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
        this.isCustomBorderColor = true;
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
        this.isCustomCornerRadius = true;
    }

    public void setBorderWidth(float width) {
        this.borderWidth = width;
    }

    public float getPadding() {
        return padding;
    }

    public void setPadding(float padding) {
        this.padding = padding;
    }
}
