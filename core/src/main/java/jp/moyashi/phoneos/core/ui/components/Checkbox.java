package jp.moyashi.phoneos.core.ui.components;

import jp.moyashi.phoneos.core.render.TextRenderer;
import jp.moyashi.phoneos.core.render.TextRendererContext;
import jp.moyashi.phoneos.core.util.EmojiUtil;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.function.Consumer;

/**
 * チェックボックスコンポーネント。
 * チェック状態の切り替え、アニメーション対応。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class Checkbox extends BaseComponent implements Clickable {

    private String label;
    private boolean checked;
    private int boxColor;
    private int checkedColor;
    private int labelColor;
    private float boxSize;

    private boolean hovered = false;
    private boolean pressed = false;
    private Consumer<Boolean> onChangeListener;

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
    public Checkbox(float x, float y, String label) {
        super(x, y, 200, 25);
        this.label = label;
        this.checked = false;
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            this.boxColor = theme.colorBorder();
            this.checkedColor = theme.colorPrimary();
            this.labelColor = theme.colorOnSurface();
        } else {
            this.boxColor = 0xFF666666;
            this.checkedColor = 0xFF4A90E2;
            this.labelColor = 0xFF000000;
        }
        this.boxSize = 18;
    }

    /**
     * チェック状態付きコンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param label ラベルテキスト
     * @param checked 初期チェック状態
     */
    public Checkbox(float x, float y, String label, boolean checked) {
        this(x, y, label);
        this.checked = checked;
        this.animationProgress = checked ? 1.0f : 0.0f;
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        g.pushStyle();

        // テーマ同期
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            this.labelColor = theme.colorOnSurface();
            this.boxColor = theme.colorBorder();
            this.checkedColor = theme.colorPrimary();
        }

        // アニメーション更新
        updateAnimation();

        // チェックボックスの枠
        int currentBoxColor = checked ? checkedColor : boxColor;
        if (!enabled) currentBoxColor = 0xFF999999;

        g.noFill();
        g.stroke(currentBoxColor);
        g.strokeWeight(2);
        g.rect(x, y, boxSize, boxSize, 3);

        // チェックマーク（アニメーション付き）
        if (animationProgress > 0) {
            g.noStroke();
            int alpha = (int)(255 * animationProgress);
            int fillColor = (currentBoxColor & 0x00FFFFFF) | (alpha << 24);
            g.fill(fillColor);
            g.rect(x + 4, y + 4, boxSize - 8, boxSize - 8, 2);
        }

        // ラベルテキスト（絵文字対応）
        if (label != null && !label.isEmpty()) {
            g.fill(enabled ? labelColor : 0xFF999999);
            g.textAlign(PApplet.LEFT, PApplet.CENTER);
            float labelSize = 14;
            g.textSize(labelSize);

            TextRenderer textRenderer = TextRendererContext.getTextRenderer();
            if (textRenderer != null && EmojiUtil.containsEmoji(label)) {
                textRenderer.drawText(g, label, x + boxSize + 8, y + boxSize / 2, labelSize);
            } else {
                g.text(label, x + boxSize + 8, y + boxSize / 2);
            }
        }

        g.popStyle();
    }

    private void updateAnimation() {
        float target = checked ? 1.0f : 0.0f;
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
        // Checkboxでは onChange を使用
    }

    /**
     * チェック状態を切り替える。
     */
    public void toggle() {
        setChecked(!checked);
    }

    /**
     * 変更リスナーを設定する。
     *
     * @param listener チェック状態変更時のコールバック
     */
    public void setOnChangeListener(Consumer<Boolean> listener) {
        this.onChangeListener = listener;
    }

    // Getter/Setter

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            if (onChangeListener != null) {
                onChangeListener.accept(checked);
            }
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setBoxColor(int color) {
        this.boxColor = color;
    }

    public void setCheckedColor(int color) {
        this.checkedColor = color;
    }

    public void setLabelColor(int color) {
        this.labelColor = color;
    }
}
