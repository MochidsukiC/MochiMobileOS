package jp.moyashi.phoneos.core.ui.components;

import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * ラジオボタンコンポーネント。
 * RadioGroupと組み合わせて排他的選択を実現する。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class RadioButton extends BaseComponent implements Clickable {

    private String label;
    private boolean selected;
    private int circleColor;
    private int selectedColor;
    private int labelColor;
    private float circleSize;

    private boolean hovered = false;
    private boolean pressed = false;
    private Runnable onSelectListener;

    // アニメーション
    private float animationProgress = 0.0f;
    private static final float ANIMATION_SPEED = 0.2f;

    /**
     * コンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param label ラベルテキスト
     */
    public RadioButton(float x, float y, String label) {
        super(x, y, 200, 25);
        this.label = label;
        this.selected = false;
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            this.circleColor = theme.colorBorder();
            this.selectedColor = theme.colorPrimary();
            this.labelColor = theme.colorOnSurface();
        } else {
            this.circleColor = 0xFF666666;
            this.selectedColor = 0xFF4A90E2;
            this.labelColor = 0xFF000000;
        }
        this.circleSize = 18;
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        g.pushStyle();

        // テーマ同期
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            this.labelColor = theme.colorOnSurface();
            this.circleColor = theme.colorBorder();
            this.selectedColor = theme.colorPrimary();
        }

        // アニメーション更新
        updateAnimation();

        // ラジオボタンの円
        int currentColor = selected ? selectedColor : circleColor;
        if (!enabled) currentColor = 0xFF999999;

        g.noFill();
        g.stroke(currentColor);
        g.strokeWeight(2);
        g.ellipse(x + circleSize / 2, y + circleSize / 2, circleSize, circleSize);

        // 選択マーク（内側の円、アニメーション付き）
        if (animationProgress > 0) {
            g.noStroke();
            int alpha = (int)(255 * animationProgress);
            int fillColor = (currentColor & 0x00FFFFFF) | (alpha << 24);
            g.fill(fillColor);
            float innerSize = circleSize * 0.5f * animationProgress;
            g.ellipse(x + circleSize / 2, y + circleSize / 2, innerSize, innerSize);
        }

        // ラベルテキスト
        if (label != null && !label.isEmpty()) {
            g.fill(enabled ? labelColor : 0xFF999999);
            g.textAlign(PApplet.LEFT, PApplet.CENTER);
            g.textSize(14);
            g.text(label, x + circleSize + 8, y + circleSize / 2);
        }

        g.popStyle();
    }

    private void updateAnimation() {
        float target = selected ? 1.0f : 0.0f;
        if (Math.abs(animationProgress - target) > 0.01f) {
            animationProgress += (target - animationProgress) * ANIMATION_SPEED;
        } else {
            animationProgress = target;
        }
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
            select();
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
        this.onSelectListener = listener;
    }

    /**
     * このラジオボタンを選択する。
     */
    public void select() {
        if (!selected) {
            selected = true;
            if (onSelectListener != null) {
                onSelectListener.run();
            }
        }
    }

    // Getter/Setter

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setCircleColor(int color) {
        this.circleColor = color;
    }

    public void setSelectedColor(int color) {
        this.selectedColor = color;
    }

    public void setLabelColor(int color) {
        this.labelColor = color;
    }
}
