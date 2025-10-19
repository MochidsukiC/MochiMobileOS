package jp.moyashi.phoneos.core.ui.components;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;

/**
 * テキストラベルコンポーネント。
 * テキスト表示、アライメント、フォント、カラーのカスタマイズに対応。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class Label extends BaseComponent {

    private String text;
    private int textColor;
    private float textSize;
    private int horizontalAlign;
    private int verticalAlign;
    private PFont font;

    /**
     * コンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     * @param text 表示テキスト
     */
    public Label(float x, float y, float width, float height, String text) {
        super(x, y, width, height);
        this.text = text;
        this.textColor = 0xFF000000; // デフォルト黒
        this.textSize = 14;
        this.horizontalAlign = PApplet.LEFT;
        this.verticalAlign = PApplet.TOP;
        this.font = null;
    }

    /**
     * 簡易コンストラクタ（自動サイズ）。
     *
     * @param x X座標
     * @param y Y座標
     * @param text 表示テキスト
     */
    public Label(float x, float y, String text) {
        this(x, y, 200, 30, text);
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible || text == null || text.isEmpty()) return;

        g.pushStyle();

        // フォント設定
        if (font != null) {
            g.textFont(font);
        }

        // テキスト設定
        g.fill(enabled ? textColor : 0xFF999999);
        g.textAlign(horizontalAlign, verticalAlign);
        g.textSize(textSize);

        // 描画位置の計算
        float drawX = x;
        float drawY = y;

        if (horizontalAlign == PApplet.CENTER) {
            drawX = x + width / 2;
        } else if (horizontalAlign == PApplet.RIGHT) {
            drawX = x + width;
        }

        if (verticalAlign == PApplet.CENTER) {
            drawY = y + height / 2;
        } else if (verticalAlign == PApplet.BOTTOM) {
            drawY = y + height;
        }

        // テキスト描画
        g.text(text, drawX, drawY);

        g.popStyle();
    }

    // Getter/Setter

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public void setTextSize(float size) {
        this.textSize = size;
    }

    public void setHorizontalAlign(int align) {
        this.horizontalAlign = align;
    }

    public void setVerticalAlign(int align) {
        this.verticalAlign = align;
    }

    public void setFont(PFont font) {
        this.font = font;
    }
}
