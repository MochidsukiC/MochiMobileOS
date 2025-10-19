package jp.moyashi.phoneos.core.ui.components;

import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.function.Consumer;

/**
 * スライダーコンポーネント。
 * 数値入力、範囲指定、水平・垂直方向対応。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class Slider extends BaseComponent implements Clickable {

    private float minValue;
    private float maxValue;
    private float value;
    private boolean vertical;

    private int trackColor;
    private int fillColor;
    private int knobColor;
    private int labelColor;

    private boolean hovered = false;
    private boolean pressed = false;
    private boolean dragging = false;
    private Consumer<Float> onValueChangeListener;

    private String label;

    /**
     * コンストラクタ（水平スライダー）。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param minValue 最小値
     * @param maxValue 最大値
     * @param initialValue 初期値
     */
    public Slider(float x, float y, float width, float minValue, float maxValue, float initialValue) {
        super(x, y, width, 30);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.value = Math.max(minValue, Math.min(maxValue, initialValue));
        this.vertical = false;
        this.trackColor = 0xFFCCCCCC;
        this.fillColor = 0xFF4A90E2;
        this.knobColor = 0xFFFFFFFF;
        this.labelColor = 0xFF000000;
        this.label = "";
    }

    /**
     * コンストラクタ（垂直スライダー）。
     *
     * @param x X座標
     * @param y Y座標
     * @param height 高さ
     * @param minValue 最小値
     * @param maxValue 最大値
     * @param initialValue 初期値
     * @param vertical 垂直方向かどうか
     */
    public Slider(float x, float y, float height, float minValue, float maxValue, float initialValue, boolean vertical) {
        super(x, y, vertical ? 30 : 200, vertical ? height : 30);
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.value = Math.max(minValue, Math.min(maxValue, initialValue));
        this.vertical = vertical;
        this.trackColor = 0xFFCCCCCC;
        this.fillColor = 0xFF4A90E2;
        this.knobColor = 0xFFFFFFFF;
        this.labelColor = 0xFF000000;
        this.label = "";
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
        float trackY = y + height / 2;
        float trackHeight = 6;
        float knobSize = 20;

        // トラック背景
        g.fill(enabled ? trackColor : 0xFFE0E0E0);
        g.noStroke();
        g.rect(x, trackY - trackHeight / 2, width, trackHeight, trackHeight / 2);

        // トラック塗りつぶし
        float fillWidth = ((value - minValue) / (maxValue - minValue)) * width;
        g.fill(enabled ? fillColor : 0xFFAAAAAA);
        g.rect(x, trackY - trackHeight / 2, fillWidth, trackHeight, trackHeight / 2);

        // つまみ
        float knobX = x + fillWidth;
        g.fill(enabled ? knobColor : 0xFFCCCCCC);
        if (dragging || hovered) {
            g.stroke(fillColor);
            g.strokeWeight(2);
        } else {
            g.stroke(0xFF999999);
            g.strokeWeight(1);
        }
        g.ellipse(knobX, trackY, knobSize, knobSize);

        // ラベル
        if (label != null && !label.isEmpty()) {
            g.fill(enabled ? labelColor : 0xFF999999);
            g.textAlign(PApplet.CENTER, PApplet.TOP);
            g.textSize(12);
            g.noStroke();
            g.text(label + ": " + String.format("%.1f", value), x + width / 2, y - 18);
        }
    }

    private void drawVertical(PGraphics g) {
        float trackX = x + width / 2;
        float trackWidth = 6;
        float knobSize = 20;

        // トラック背景
        g.fill(enabled ? trackColor : 0xFFE0E0E0);
        g.noStroke();
        g.rect(trackX - trackWidth / 2, y, trackWidth, height, trackWidth / 2);

        // トラック塗りつぶし（下から）
        float fillHeight = ((value - minValue) / (maxValue - minValue)) * height;
        float fillY = y + height - fillHeight;
        g.fill(enabled ? fillColor : 0xFFAAAAAA);
        g.rect(trackX - trackWidth / 2, fillY, trackWidth, fillHeight, trackWidth / 2);

        // つまみ
        float knobY = fillY;
        g.fill(enabled ? knobColor : 0xFFCCCCCC);
        if (dragging || hovered) {
            g.stroke(fillColor);
            g.strokeWeight(2);
        } else {
            g.stroke(0xFF999999);
            g.strokeWeight(1);
        }
        g.ellipse(trackX, knobY, knobSize, knobSize);

        // ラベル
        if (label != null && !label.isEmpty()) {
            g.fill(enabled ? labelColor : 0xFF999999);
            g.textAlign(PApplet.LEFT, PApplet.TOP);
            g.textSize(12);
            g.noStroke();
            g.text(label + ": " + String.format("%.1f", value), x + width + 5, y);
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
            dragging = true;
            updateValue(mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseReleased(int mouseX, int mouseY) {
        if (!enabled || !visible) {
            pressed = false;
            dragging = false;
            return false;
        }

        pressed = false;
        dragging = false;
        return true;
    }

    @Override
    public void onMouseMoved(int mouseX, int mouseY) {
        if (!enabled || !visible) {
            hovered = false;
            return;
        }

        hovered = contains(mouseX, mouseY);
    }

    /**
     * マウスドラッグイベントを処理する。
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     */
    public void onMouseDragged(int mouseX, int mouseY) {
        if (dragging && enabled) {
            updateValue(mouseX, mouseY);
        }
    }

    private void updateValue(int mouseX, int mouseY) {
        float newValue;

        if (vertical) {
            float ratio = 1.0f - (mouseY - y) / height;
            ratio = Math.max(0, Math.min(1, ratio));
            newValue = minValue + ratio * (maxValue - minValue);
        } else {
            float ratio = (mouseX - x) / width;
            ratio = Math.max(0, Math.min(1, ratio));
            newValue = minValue + ratio * (maxValue - minValue);
        }

        if (newValue != value) {
            value = newValue;
            if (onValueChangeListener != null) {
                onValueChangeListener.accept(value);
            }
        }
    }

    @Override
    public void setOnClickListener(Runnable listener) {
        // Sliderでは onValueChange を使用
    }

    /**
     * 値変更リスナーを設定する。
     *
     * @param listener 値変更時のコールバック
     */
    public void setOnValueChangeListener(Consumer<Float> listener) {
        this.onValueChangeListener = listener;
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

    public void setTrackColor(int color) {
        this.trackColor = color;
    }

    public void setFillColor(int color) {
        this.fillColor = color;
    }

    public void setKnobColor(int color) {
        this.knobColor = color;
    }

    public void setLabelColor(int color) {
        this.labelColor = color;
    }
}
