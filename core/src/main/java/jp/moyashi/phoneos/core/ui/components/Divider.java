package jp.moyashi.phoneos.core.ui.components;

import processing.core.PGraphics;

/**
 * 区切り線コンポーネント。
 * 水平/垂直方向の線を描画。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class Divider extends BaseComponent {

    private int color;
    private float thickness;
    private boolean vertical;

    /**
     * コンストラクタ（水平線）。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     */
    public Divider(float x, float y, float width) {
        super(x, y, width, 1);
        this.color = 0xFFCCCCCC;
        this.thickness = 1;
        this.vertical = false;
    }

    /**
     * コンストラクタ（水平/垂直指定）。
     *
     * @param x X座標
     * @param y Y座標
     * @param length 長さ
     * @param vertical 垂直方向かどうか
     */
    public Divider(float x, float y, float length, boolean vertical) {
        super(x, y, vertical ? 1 : length, vertical ? length : 1);
        this.color = 0xFFCCCCCC;
        this.thickness = 1;
        this.vertical = vertical;
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        g.pushStyle();

        g.stroke(color);
        g.strokeWeight(thickness);

        if (vertical) {
            g.line(x, y, x, y + height);
        } else {
            g.line(x, y, x + width, y);
        }

        g.popStyle();
    }

    // Getter/Setter

    public void setColor(int color) {
        this.color = color;
    }

    public void setThickness(float thickness) {
        this.thickness = thickness;
        if (vertical) {
            this.width = thickness;
        } else {
            this.height = thickness;
        }
    }
}
