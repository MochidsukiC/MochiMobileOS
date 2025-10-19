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
        if (!enabled) {
            g.fill(100, 100, 100, 100);
        } else if (isOn) {
            int alpha = (int) (50 + 100 * animationProgress);
            g.fill(100, 150, 255, alpha);
        } else {
            g.fill(200, 200, 200, 80);
        }
        g.noStroke();
        g.rect(x, y, w, h, 12);
        if (enabled) {
            g.stroke(isOn ? 80 : 150);
            g.strokeWeight(1);
            g.noFill();
            g.rect(x, y, w, h, 12);
        }
    }

    private void drawBackground(PApplet p, float x, float y, float w, float h) {
        if (!enabled) {
            p.fill(100, 100, 100, 100);
        } else if (isOn) {
            int alpha = (int) (50 + 100 * animationProgress);
            p.fill(100, 150, 255, alpha);
        } else {
            p.fill(200, 200, 200, 80);
        }
        p.noStroke();
        p.rect(x, y, w, h, 12);
        p.stroke(enabled ? 150 : 100);
        p.strokeWeight(1);
        p.noFill();
        p.rect(x, y, w, h, 12);
    }

    private void drawIcon(PGraphics g, float x, float y, float w, float h) {
        float iconSize = h * 0.3f;
        float iconX = x + (w - iconSize) / 2;
        float iconY = y + h * 0.1f;

        if (icon != null) {
            g.image(icon, iconX, iconY, iconSize, iconSize);
        } else {
            g.fill(255, 255, 255, enabled ? 200 : 100);
            g.rect(iconX, iconY, iconSize, iconSize, 4);
            g.fill(isOn ? 100 : 150);
            g.textAlign(PApplet.CENTER, PApplet.CENTER);
            g.textSize(iconSize * 0.4f);
            g.text(id.substring(0, 1).toUpperCase(), iconX + iconSize / 2, iconY + iconSize / 2);
        }
    }

    private void drawIcon(PApplet p, float x, float y, float w, float h) {
        float iconSize = h * 0.3f;
        float iconX = x + (w - iconSize) / 2;
        float iconY = y + h * 0.1f;

        if (icon != null) {
            p.tint(enabled ? 255 : 100);
            p.image(icon, iconX, iconY, iconSize, iconSize);
            p.noTint();
        } else {
            p.fill(enabled ? (isOn ? 255 : 150) : 100);
            p.noStroke();
            p.ellipse(iconX + iconSize / 2, iconY + iconSize / 2, iconSize * 0.8f, iconSize * 0.8f);
        }
    }

    private void drawLabel(PGraphics g, float x, float y, float w, float h) {
        float labelY = y + h * 0.5f;
        g.fill(255, 255, 255, enabled ? 255 : 150);
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(12);
        g.text(displayName, x + w / 2, labelY);
    }

    private void drawLabel(PApplet p, float x, float y, float w, float h) {
        float labelY = y + h * 0.5f;
        p.fill(enabled ? 255 : 150);
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(12);
        p.text(displayName, x + w / 2, labelY);
    }

    private void drawToggleSwitch(PGraphics g, float x, float y, float w, float h) {
        float switchWidth = w * 0.5f;
        float switchHeight = h * 0.2f;
        float switchX = x + (w - switchWidth) / 2;
        float switchY = y + h - switchHeight - (h * 0.1f);

        g.fill(isOn ? 100 : 80, isOn ? 200 : 100, isOn ? 255 : 120, enabled ? 200 : 100);
        g.rect(switchX, switchY, switchWidth, switchHeight, switchHeight / 2);

        float knobSize = switchHeight * 0.8f;
        float knobX = switchX + (switchWidth - knobSize - 4) * animationProgress + 2;
        float knobY = switchY + (switchHeight - knobSize) / 2;

        g.fill(255, 255, 255, enabled ? 255 : 150);
        g.ellipse(knobX + knobSize / 2, knobY + knobSize / 2, knobSize, knobSize);
    }

    private void drawToggleSwitch(PApplet p, float x, float y, float w, float h) {
        float switchWidth = w * 0.5f;
        float switchHeight = h * 0.2f;
        float switchX = x + (w - switchWidth) / 2;
        float switchY = y + h - switchHeight - (h * 0.1f);

        if (isOn) {
            int green = (int) (100 + 155 * animationProgress);
            p.fill(100, green, 100);
        } else {
            p.fill(150, 150, 150);
        }
        p.noStroke();
        p.rect(switchX, switchY, switchWidth, switchHeight, switchHeight / 2);

        float knobSize = switchHeight - 4;
        float knobX = switchX + 2 + (switchWidth - knobSize - 4) * animationProgress;
        float knobY = switchY + 2;

        p.fill(enabled ? 255 : 200);
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
