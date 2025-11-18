package jp.moyashi.phoneos.core.controls;

import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.function.Consumer;

/**
 * ON/OFF状態を持つ基本的なトグルスイッチコントロールセンターアイテム。
 * アイコン、ラベル、現在の状態、状態変更時のアクションを保持し、
 * ユーザーのタップ操作で状態を切り替える機能を提供する。
 * 
 * @author YourName
 * @version 1.1
 * @since 1.0
 */
public class ToggleItem implements IControlCenterItem {
    
    private final String id;
    private final String displayName;
    private final String description;
    private final PImage icon;
    private boolean isOn;
    private final Consumer<Boolean> onStateChanged;
    private boolean enabled;
    private boolean visible;
    private float animationProgress;
    private float targetAnimationProgress;
    private static final float ANIMATION_SPEED = 0.15f;

    public ToggleItem(String id, String displayName, String description, 
                     PImage icon, boolean initialState, Consumer<Boolean> onStateChanged) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
        this.isOn = initialState;
        this.onStateChanged = onStateChanged;
        this.enabled = true;
        this.visible = true;
        this.targetAnimationProgress = initialState ? 1.0f : 0.0f;
        this.animationProgress = this.targetAnimationProgress;
        System.out.println("ToggleItem: Created toggle '" + displayName + "' (ID: " + id + ") with initial state: " + initialState);
    }

    public ToggleItem(String id, String displayName, String description, 
                     boolean initialState, Consumer<Boolean> onStateChanged) {
        this(id, displayName, description, null, initialState, onStateChanged);
    }

    @Override
    public void draw(PGraphics g, float x, float y, float w, float h) {
        updateAnimation();
        drawBackground(g, x, y, w, h);
        drawIcon(g, x, y, w, h);
        drawLabel(g, x, y, w, h);
        drawToggleSwitch(g, x, y, w, h);
    }

    @Override
    public void draw(PApplet p, float x, float y, float w, float h) {
        updateAnimation();
        p.pushMatrix();
        p.pushStyle();
        try {
            drawBackground(p, x, y, w, h);
            drawIcon(p, x, y, w, h);
            drawLabel(p, x, y, w, h);
            drawToggleSwitch(p, x, y, w, h);
        } finally {
            p.popStyle();
            p.popMatrix();
        }
    }

    private void drawBackground(PGraphics g, float x, float y, float w, float h) {
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int surface = theme != null ? theme.colorSurface() : 0xFFFFFFFF;
        int border  = theme != null ? theme.colorBorder()  : 0xFFDDDDDD;
        int acc     = theme != null ? theme.colorPrimary() : 0xFF4A90E2;
        int radius  = theme != null ? theme.radiusMd() : 12;

        // base card: スロット（ControlCenter側）の上に重なるため、少し濃いめ＋半透明で差層を作る
        int sr = (surface>>16)&0xFF, sg = (surface>>8)&0xFF, sb = surface&0xFF;
        // 8%ほど暗く（クランプ）
        sr = Math.max(0, (int)(sr * 0.92f));
        sg = Math.max(0, (int)(sg * 0.92f));
        sb = Math.max(0, (int)(sb * 0.92f));
        g.noStroke();
        g.fill(sr, sg, sb, enabled ? 220 : 180);
        g.rect(x, y, w, h, radius);

        // border
        g.stroke((border>>16)&0xFF, (border>>8)&0xFF, border&0xFF);
        g.strokeWeight(1);
        g.noFill();
        g.rect(x, y, w, h, radius);

        // ON highlight overlay (low alpha accent)
        if (enabled && isOn) {
            int ar = (acc>>16)&0xFF, ag = (acc>>8)&0xFF, ab = acc&0xFF;
            int a = Math.min(120, 28 + (int)(52 * animationProgress));
            try {
                if (theme != null && theme.getTone() == jp.moyashi.phoneos.core.ui.theme.ThemeEngine.Tone.LIGHT &&
                    theme.getFamily() == jp.moyashi.phoneos.core.ui.theme.ThemeEngine.Mode.GREEN) {
                    a = Math.min(96, 20 + (int)(40 * animationProgress)); // Greenライト時は控えめ
                }
            } catch (Throwable ignored) {}
            g.noStroke();
            g.fill(ar, ag, ab, a);
            g.rect(x, y, w, h, radius);
        }
    }

    private void drawBackground(PApplet p, float x, float y, float w, float h) {
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int surface = theme != null ? theme.colorSurface() : 0xFFFFFFFF;
        int border  = theme != null ? theme.colorBorder()  : 0xFFDDDDDD;
        int acc     = theme != null ? theme.colorPrimary() : 0xFF4A90E2;
        int radius  = theme != null ? theme.radiusMd() : 12;

        // base card（少し濃いめ＋半透明）
        int sr = (surface>>16)&0xFF, sg = (surface>>8)&0xFF, sb = surface&0xFF;
        sr = Math.max(0, (int)(sr * 0.92f));
        sg = Math.max(0, (int)(sg * 0.92f));
        sb = Math.max(0, (int)(sb * 0.92f));
        p.noStroke();
        p.fill(sr, sg, sb, enabled ? 220 : 180);
        p.rect(x, y, w, h, radius);

        // border
        p.stroke((border>>16)&0xFF, (border>>8)&0xFF, border&0xFF);
        p.strokeWeight(1);
        p.noFill();
        p.rect(x, y, w, h, radius);

        // ON highlight overlay
        if (enabled && isOn) {
            int ar = (acc>>16)&0xFF, ag = (acc>>8)&0xFF, ab = acc&0xFF;
            int a = Math.min(120, 28 + (int)(52 * animationProgress));
            try {
                if (theme != null && theme.getTone() == jp.moyashi.phoneos.core.ui.theme.ThemeEngine.Tone.LIGHT &&
                    theme.getFamily() == jp.moyashi.phoneos.core.ui.theme.ThemeEngine.Mode.GREEN) {
                    a = Math.min(96, 20 + (int)(40 * animationProgress));
                }
            } catch (Throwable ignored) {}
            p.noStroke();
            p.fill(ar, ag, ab, a);
            p.rect(x, y, w, h, radius);
        }
    }

    private void drawIcon(PGraphics g, float x, float y, float w, float h) {
        float iconSize = h * 0.3f;
        float iconX = x + (w - iconSize) / 2;
        float iconY = y + h * 0.1f;

        if (icon != null) {
            g.image(icon, iconX, iconY, iconSize, iconSize);
        } else {
            var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
            int acc = theme != null ? theme.colorPrimary() : 0xFF4A90E2;
            int onP = theme != null ? theme.colorOnPrimary() : 0xFFFFFFFF;
            g.noStroke();
            g.fill((acc>>16)&0xFF, (acc>>8)&0xFF, acc&0xFF, enabled ? 220 : 140);
            g.rect(iconX, iconY, iconSize, iconSize, 6);
            g.fill((onP>>16)&0xFF, (onP>>8)&0xFF, onP&0xFF);
            g.textAlign(PApplet.CENTER, PApplet.CENTER);
            g.textSize(iconSize * 0.45f);
            g.text(id.substring(0, 1).toUpperCase(), iconX + iconSize / 2, iconY + iconSize / 2);
        }
    }

    private void drawIcon(PApplet p, float x, float y, float w, float h) {
        float iconSize = h * 0.3f;
        float iconX = x + (w - iconSize) / 2;
        float iconY = y + h * 0.1f;

        if (icon != null) {
            p.tint(enabled ? 255 : 120);
            p.image(icon, iconX, iconY, iconSize, iconSize);
            p.noTint();
        } else {
            var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
            int acc = theme != null ? theme.colorPrimary() : 0xFF4A90E2;
            int onP = theme != null ? theme.colorOnPrimary() : 0xFFFFFFFF;
            p.noStroke();
            p.fill((acc>>16)&0xFF, (acc>>8)&0xFF, acc&0xFF, enabled ? 220 : 140);
            p.rect(iconX, iconY, iconSize, iconSize, 6);
            p.fill((onP>>16)&0xFF, (onP>>8)&0xFF, onP&0xFF);
            p.textAlign(PApplet.CENTER, PApplet.CENTER);
            p.textSize(iconSize * 0.45f);
            p.text(id.substring(0, 1).toUpperCase(), iconX + iconSize / 2, iconY + iconSize / 2);
        }
    }

    private void drawLabel(PGraphics g, float x, float y, float w, float h) {
        float labelY = y + h * 0.5f;
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFF111111;
        int textCol = enabled ? onSurface : 0xFF999999;
        g.fill((textCol>>16)&0xFF, (textCol>>8)&0xFF, textCol&0xFF);
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(12);
        g.text(displayName, x + w / 2, labelY);
    }

    private void drawLabel(PApplet p, float x, float y, float w, float h) {
        float labelY = y + h * 0.5f;
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFF111111;
        int textCol = enabled ? onSurface : 0xFF999999;
        p.fill((textCol>>16)&0xFF, (textCol>>8)&0xFF, textCol&0xFF);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(12);
        p.text(displayName, x + w / 2, labelY);
    }

    private void drawToggleSwitch(PGraphics g, float x, float y, float w, float h) {
        float switchWidth = w * 0.5f;
        float switchHeight = h * 0.2f;
        float switchX = x + (w - switchWidth) / 2;
        float switchY = y + h - switchHeight - (h * 0.1f);

        var theme2 = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int acc2 = theme2 != null ? theme2.colorPrimary() : 0xFF4A90E2;
        int off = 0xFFB0B0B0;
        int r = (isOn ? (acc2>>16)&0xFF : (off>>16)&0xFF);
        int gC = (isOn ? (acc2>>8)&0xFF : (off>>8)&0xFF);
        int b = (isOn ? acc2&0xFF : off&0xFF);
        g.fill(r, gC, b, enabled ? 220 : 140);
        g.rect(switchX, switchY, switchWidth, switchHeight, switchHeight / 2);

        float knobSize = switchHeight * 0.8f;
        float knobX = switchX + (switchWidth - knobSize - 4) * animationProgress + 2;
        float knobY = switchY + (switchHeight - knobSize) / 2;

        g.fill(255, 255, 255, enabled ? 255 : 180);
        g.ellipse(knobX + knobSize / 2, knobY + knobSize / 2, knobSize, knobSize);
    }

    private void drawToggleSwitch(PApplet p, float x, float y, float w, float h) {
        float switchWidth = w * 0.5f;
        float switchHeight = h * 0.2f;
        float switchX = x + (w - switchWidth) / 2;
        float switchY = y + h - switchHeight - (h * 0.1f);

        var theme3 = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int acc = theme3 != null ? theme3.colorPrimary() : 0xFF4A90E2;
        int off2 = 0xFFB0B0B0;
        int r2 = (isOn ? (acc>>16)&0xFF : (off2>>16)&0xFF);
        int g2 = (isOn ? (acc>>8)&0xFF : (off2>>8)&0xFF);
        int b2 = (isOn ? acc&0xFF : off2&0xFF);
        p.fill(r2, g2, b2, enabled ? 220 : 140);
        p.noStroke();
        p.rect(switchX, switchY, switchWidth, switchHeight, switchHeight / 2);

        float knobSize = switchHeight - 4;
        float knobX = switchX + 2 + (switchWidth - knobSize - 4) * animationProgress;
        float knobY = switchY + 2;

        p.fill(255, 255, 255, enabled ? 255 : 200);
        p.ellipse(knobX + knobSize / 2, knobY + knobSize / 2, knobSize, knobSize);
    }

    private void updateAnimation() {
        if (Math.abs(animationProgress - targetAnimationProgress) > 0.01f) {
            animationProgress += (targetAnimationProgress - animationProgress) * ANIMATION_SPEED;
        } else {
            animationProgress = targetAnimationProgress;
        }
    }

    @Override
    public boolean onGesture(GestureEvent event) {
        if (!enabled || !visible) {
            return false;
        }
        if (event.getType() == GestureType.TAP) {
            toggle();
            System.out.println("ToggleItem: '" + displayName + "' toggled to " + isOn);
            return true;
        }
        return false;
    }

    public void toggle() {
        if (!enabled) {
            return;
        }
        setOn(!isOn);
    }

    public void setOn(boolean on) {
        if (this.isOn != on) {
            this.isOn = on;
            this.targetAnimationProgress = on ? 1.0f : 0.0f;
            if (onStateChanged != null) {
                try {
                    onStateChanged.accept(on);
                } catch (Exception e) {
                    System.err.println("ToggleItem: Error in state change action: " + e.getMessage());
                }
            }
        }
    }

    public boolean isOn() {
        return isOn;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}
