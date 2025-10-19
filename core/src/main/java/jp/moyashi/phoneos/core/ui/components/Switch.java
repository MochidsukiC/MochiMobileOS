package jp.moyashi.phoneos.core.ui.components;

import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.function.Consumer;

/**
 * スイッチ（トグル）コンポーネント。
 * ON/OFF状態の切り替え、スムーズなアニメーション対応。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class Switch extends BaseComponent implements Clickable {

    private String label;
    private boolean on;
    private int onColor;
    private int offColor;
    private int knobColor;
    private int labelColor;

    private boolean hovered = false;
    private boolean pressed = false;
    private Consumer<Boolean> onChangeListener;

    // アニメーション
    private float animationProgress = 0.0f;
    private static final float ANIMATION_SPEED = 0.15f;

    // サイズ
    private static final float SWITCH_WIDTH = 50;
    private static final float SWITCH_HEIGHT = 25;

    /**
     * コンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param label ラベルテキスト
     */
    public Switch(float x, float y, String label) {
        super(x, y, 200, 30);
        this.label = label;
        this.on = false;
        this.onColor = 0xFF4A90E2;
        this.offColor = 0xFF999999;
        this.knobColor = 0xFFFFFFFF;
        this.labelColor = 0xFF000000;
    }

    /**
     * ON/OFF状態付きコンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param label ラベルテキスト
     * @param on 初期ON/OFF状態
     */
    public Switch(float x, float y, String label, boolean on) {
        this(x, y, label);
        this.on = on;
        this.animationProgress = on ? 1.0f : 0.0f;
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        g.pushStyle();

        // アニメーション更新
        updateAnimation();

        // スイッチの位置
        float switchX = x + width - SWITCH_WIDTH;
        float switchY = y + (height - SWITCH_HEIGHT) / 2;

        // ラベルテキスト
        if (label != null && !label.isEmpty()) {
            g.fill(enabled ? labelColor : 0xFF999999);
            g.textAlign(PApplet.LEFT, PApplet.CENTER);
            g.textSize(14);
            g.text(label, x, y + height / 2);
        }

        // スイッチ背景
        int currentColor = lerpColor(offColor, onColor, animationProgress);
        if (!enabled) currentColor = 0xFFCCCCCC;

        g.fill(currentColor);
        g.noStroke();
        g.rect(switchX, switchY, SWITCH_WIDTH, SWITCH_HEIGHT, SWITCH_HEIGHT / 2);

        // スイッチつまみ
        float knobSize = SWITCH_HEIGHT - 4;
        float knobX = switchX + 2 + (SWITCH_WIDTH - knobSize - 4) * animationProgress;
        float knobY = switchY + 2;

        g.fill(enabled ? knobColor : 0xFFEEEEEE);
        g.ellipse(knobX + knobSize / 2, knobY + knobSize / 2, knobSize, knobSize);

        g.popStyle();
    }

    private void updateAnimation() {
        float target = on ? 1.0f : 0.0f;
        if (Math.abs(animationProgress - target) > 0.01f) {
            animationProgress += (target - animationProgress) * ANIMATION_SPEED;
        } else {
            animationProgress = target;
        }
    }

    private int lerpColor(int c1, int c2, float amt) {
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
            toggle();
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
        // Switchでは onChange を使用
    }

    /**
     * ON/OFF状態を切り替える。
     */
    public void toggle() {
        setOn(!on);
    }

    /**
     * 変更リスナーを設定する。
     *
     * @param listener ON/OFF状態変更時のコールバック
     */
    public void setOnChangeListener(Consumer<Boolean> listener) {
        this.onChangeListener = listener;
    }

    // Getter/Setter

    public boolean isOn() {
        return on;
    }

    public void setOn(boolean on) {
        if (this.on != on) {
            this.on = on;
            if (onChangeListener != null) {
                onChangeListener.accept(on);
            }
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setOnColor(int color) {
        this.onColor = color;
    }

    public void setOffColor(int color) {
        this.offColor = color;
    }

    public void setKnobColor(int color) {
        this.knobColor = color;
    }

    public void setLabelColor(int color) {
        this.labelColor = color;
    }
}
