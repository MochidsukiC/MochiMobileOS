package jp.moyashi.phoneos.core.ui.components;

import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * プログレスバー/レベルメーターコンポーネント。
 * 進捗表示、音声レベル表示などに使用。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class ProgressBar extends BaseComponent {

    private float minValue;
    private float maxValue;
    private float value;
    private boolean vertical;

    private int backgroundColor;
    private int fillColor;
    private int borderColor;
    private float cornerRadius;

    private String label;
    private boolean showPercentage;

    /**
     * コンストラクタ（水平プログレスバー）。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     */
    public ProgressBar(float x, float y, float width, float height) {
        super(x, y, width, height);
        this.minValue = 0;
        this.maxValue = 100;
        this.value = 0;
        this.vertical = false;
        this.backgroundColor = 0xFFE0E0E0;
        this.fillColor = 0xFF4A90E2;
        this.borderColor = 0xFFCCCCCC;
        this.cornerRadius = 3;
        this.label = "";
        this.showPercentage = false;
    }

    /**
     * コンストラクタ（水平/垂直指定）。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     * @param vertical 垂直方向かどうか
     */
    public ProgressBar(float x, float y, float width, float height, boolean vertical) {
        this(x, y, width, height);
        this.vertical = vertical;
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        g.pushStyle();

        if (vertical) {
            drawVertical(g);
        } else {
            drawHorizontal(g);
        }

        g.popStyle();
    }

    private void drawHorizontal(PGraphics g) {
        // 背景
        g.fill(backgroundColor);
        g.noStroke();
        if (cornerRadius > 0) {
            g.rect(x, y, width, height, cornerRadius);
        } else {
            g.rect(x, y, width, height);
        }

        // 進捗
        float fillWidth = ((value - minValue) / (maxValue - minValue)) * width;
        fillWidth = Math.max(0, Math.min(width, fillWidth));

        if (fillWidth > 0) {
            g.fill(fillColor);
            if (cornerRadius > 0) {
                g.rect(x, y, fillWidth, height, cornerRadius);
            } else {
                g.rect(x, y, fillWidth, height);
            }
        }

        // 枠線
        g.noFill();
        g.stroke(borderColor);
        g.strokeWeight(1);
        if (cornerRadius > 0) {
            g.rect(x, y, width, height, cornerRadius);
        } else {
            g.rect(x, y, width, height);
        }

        // ラベルとパーセンテージ
        if (showPercentage || (label != null && !label.isEmpty())) {
            drawLabelHorizontal(g);
        }
    }

    private void drawVertical(PGraphics g) {
        // 背景
        g.fill(backgroundColor);
        g.noStroke();
        if (cornerRadius > 0) {
            g.rect(x, y, width, height, cornerRadius);
        } else {
            g.rect(x, y, width, height);
        }

        // 進捗（下から）
        float fillHeight = ((value - minValue) / (maxValue - minValue)) * height;
        fillHeight = Math.max(0, Math.min(height, fillHeight));

        if (fillHeight > 0) {
            float fillY = y + height - fillHeight;
            g.fill(fillColor);
            if (cornerRadius > 0) {
                g.rect(x, fillY, width, fillHeight, cornerRadius);
            } else {
                g.rect(x, fillY, width, fillHeight);
            }
        }

        // 枠線
        g.noFill();
        g.stroke(borderColor);
        g.strokeWeight(1);
        if (cornerRadius > 0) {
            g.rect(x, y, width, height, cornerRadius);
        } else {
            g.rect(x, y, width, height);
        }
    }

    private void drawLabelHorizontal(PGraphics g) {
        g.fill(0xFF000000);
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(12);
        g.noStroke();

        String text = "";
        if (label != null && !label.isEmpty()) {
            text = label;
        }
        if (showPercentage) {
            float percentage = ((value - minValue) / (maxValue - minValue)) * 100;
            String percentText = String.format("%.0f%%", percentage);
            text += (text.isEmpty() ? "" : " ") + percentText;
        }

        g.text(text, x + width / 2, y + height / 2);
    }

    // Getter/Setter

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = Math.max(minValue, Math.min(maxValue, value));
    }

    public float getMinValue() {
        return minValue;
    }

    public void setMinValue(float minValue) {
        this.minValue = minValue;
        if (value < minValue) value = minValue;
    }

    public float getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(float maxValue) {
        this.maxValue = maxValue;
        if (value > maxValue) value = maxValue;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isShowPercentage() {
        return showPercentage;
    }

    public void setShowPercentage(boolean showPercentage) {
        this.showPercentage = showPercentage;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    public void setFillColor(int color) {
        this.fillColor = color;
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
    }
}
