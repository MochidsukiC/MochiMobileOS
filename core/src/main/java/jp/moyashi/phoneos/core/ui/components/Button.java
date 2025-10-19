package jp.moyashi.phoneos.core.ui.components;

import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PApplet;

/**
 * 基本的なボタンコンポーネント。
 * テキスト、アイコン、カラーカスタマイズ、ホバー・プレスアニメーションに対応。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class Button extends BaseComponent implements Clickable {

    private String text;
    private PImage icon;
    private int backgroundColor;
    private int textColor;
    private int borderColor;
    private int hoverColor;
    private int pressColor;
    private float cornerRadius;

    private boolean hovered = false;
    private boolean pressed = false;
    private Runnable onClickListener;

    // アニメーション
    private float animationProgress = 0.0f;
    private static final float ANIMATION_SPEED = 0.2f;

    /**
     * コンストラクタ（テキストのみ）。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     * @param text ボタンテキスト
     */
    public Button(float x, float y, float width, float height, String text) {
        super(x, y, width, height);
        this.text = text;
        this.icon = null;
        this.backgroundColor = 0xFF4A90E2; // デフォルト青
        this.textColor = 0xFFFFFFFF; // 白
        this.borderColor = 0xFF3A7BC8;
        this.hoverColor = 0xFF5AA0F2;
        this.pressColor = 0xFF3A80D2;
        this.cornerRadius = 5;
    }

    /**
     * コンストラクタ（テキスト + アイコン）。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     * @param text ボタンテキスト
     * @param icon アイコン画像
     */
    public Button(float x, float y, float width, float height, String text, PImage icon) {
        this(x, y, width, height, text);
        this.icon = icon;
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        g.pushStyle();

        // アニメーション更新
        updateAnimation();

        // 背景色の決定
        int currentColor = backgroundColor;
        if (pressed) {
            currentColor = pressColor;
        } else if (hovered && enabled) {
            currentColor = lerpColor(g, backgroundColor, hoverColor, animationProgress);
        }

        // 無効状態の場合はグレーアウト
        if (!enabled) {
            currentColor = 0xFF999999;
        }

        // 背景描画
        g.fill(currentColor);
        g.noStroke();
        if (cornerRadius > 0) {
            g.rect(x, y, width, height, cornerRadius);
        } else {
            g.rect(x, y, width, height);
        }

        // 枠線描画
        if (enabled) {
            g.noFill();
            g.stroke(borderColor);
            g.strokeWeight(1);
            if (cornerRadius > 0) {
                g.rect(x, y, width, height, cornerRadius);
            } else {
                g.rect(x, y, width, height);
            }
        }

        // アイコンとテキストの描画
        drawContent(g);

        g.popStyle();
    }

    private void drawContent(PGraphics g) {
        float contentX = x + width / 2;
        float contentY = y + height / 2;

        if (icon != null && text != null && !text.isEmpty()) {
            // アイコン + テキスト
            float iconSize = height * 0.5f;
            float spacing = 5;
            float totalWidth = iconSize + spacing + g.textWidth(text);
            float startX = contentX - totalWidth / 2;

            g.image(icon, startX, contentY - iconSize / 2, iconSize, iconSize);
            g.fill(enabled ? textColor : 0xFFCCCCCC);
            g.textAlign(PApplet.LEFT, PApplet.CENTER);
            g.textSize(14);
            g.text(text, startX + iconSize + spacing, contentY);
        } else if (icon != null) {
            // アイコンのみ
            float iconSize = Math.min(width, height) * 0.6f;
            g.image(icon, contentX - iconSize / 2, contentY - iconSize / 2, iconSize, iconSize);
        } else if (text != null && !text.isEmpty()) {
            // テキストのみ
            g.fill(enabled ? textColor : 0xFFCCCCCC);
            g.textAlign(PApplet.CENTER, PApplet.CENTER);
            g.textSize(14);
            g.text(text, contentX, contentY);
        }
    }

    private void updateAnimation() {
        float target = hovered ? 1.0f : 0.0f;
        if (Math.abs(animationProgress - target) > 0.01f) {
            animationProgress += (target - animationProgress) * ANIMATION_SPEED;
        } else {
            animationProgress = target;
        }
    }

    private int lerpColor(PGraphics g, int c1, int c2, float amt) {
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int r = (int)(r1 + (r2 - r1) * amt);
        int gr = (int)(g1 + (g2 - g1) * amt);
        int b = (int)(b1 + (b2 - b1) * amt);

        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    @Override
    public boolean isHovered() {
        return hovered;
    }

    @Override
    public boolean isPressed() {
        return pressed;
    }

    @Override
    public boolean onMousePressed(int mouseX, int mouseY) {
        if (!enabled || !visible) return false;

        if (contains(mouseX, mouseY)) {
            pressed = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseReleased(int mouseX, int mouseY) {
        if (!enabled || !visible) {
            pressed = false;
            return false;
        }

        if (pressed && contains(mouseX, mouseY)) {
            pressed = false;
            if (onClickListener != null) {
                onClickListener.run();
            }
            return true;
        }
        pressed = false;
        return false;
    }

    @Override
    public void onMouseMoved(int mouseX, int mouseY) {
        if (!enabled || !visible) {
            hovered = false;
            return;
        }

        hovered = contains(mouseX, mouseY);
    }

    @Override
    public void setOnClickListener(Runnable listener) {
        this.onClickListener = listener;
    }

    // Getter/Setter

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public PImage getIcon() {
        return icon;
    }

    public void setIcon(PImage icon) {
        this.icon = icon;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
    }

    public void setHoverColor(int color) {
        this.hoverColor = color;
    }

    public void setPressColor(int color) {
        this.pressColor = color;
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
    }
}
