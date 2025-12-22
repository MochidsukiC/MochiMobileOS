package jp.moyashi.phoneos.core.controls;

import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import jp.moyashi.phoneos.core.ui.theme.ThemeContext;
import jp.moyashi.phoneos.core.ui.theme.ThemeEngine;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.function.Consumer;

/**
 * ON/OFF状態を持つモダンなトグルボタン。
 * iOSのコントロールセンターのように、ON状態で背景色が変化し、アイコンのみ（またはアイコン＋ラベル）を表示する。
 * 4カラム・スクエアグリッドに最適化されたデザイン。
 * 
 * @author YourName
 * @version 2.0 (Modern Square Design)
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
    
    // アニメーション用
    private float animationProgress; // 0.0 (OFF) -> 1.0 (ON)
    private float targetAnimationProgress;
    private static final float ANIMATION_SPEED = 0.2f; // 少し早めに

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
    }

    public ToggleItem(String id, String displayName, String description, 
                     boolean initialState, Consumer<Boolean> onStateChanged) {
        this(id, displayName, description, null, initialState, onStateChanged);
    }

    @Override
    public void draw(PGraphics g, float x, float y, float w, float h) {
        updateAnimation();
        drawModernToggle(g, x, y, w, h);
    }

    @Override
    public void draw(PApplet p, float x, float y, float w, float h) {
        draw(p.g, x, y, w, h);
    }

    /**
     * モダンなトグルボタンを描画する。
     */
    private void drawModernToggle(PGraphics g, float x, float y, float w, float h) {
        ThemeEngine theme = ThemeContext.getTheme();
        
        // カラーパレット
        int colorOff = 0xFF4A4A4A; // 暗めのグレー (OFF時背景)
        int colorOn = theme != null ? theme.colorPrimary() : 0xFF007AFF; // アクセントカラー (ON時背景)
        int colorIconOff = 0xFFFFFFFF; // 白 (OFF時アイコン)
        int colorIconOn = 0xFFFFFFFF;  // 白 (ON時アイコン)
        
        if (theme != null && theme.getMode() == ThemeEngine.Mode.LIGHT) {
            colorOff = 0xFFE0E0E0; // ライトモード時は明るめのグレー
            colorIconOff = 0xFF333333; // ライトモードOFF時は濃いグレーアイコン
        }

        // アニメーションに基づいた色補間
        // 背景色
        int rOff = (colorOff >> 16) & 0xFF;
        int gOff = (colorOff >> 8) & 0xFF;
        int bOff = colorOff & 0xFF;
        
        int rOn = (colorOn >> 16) & 0xFF;
        int gOn = (colorOn >> 8) & 0xFF;
        int bOn = colorOn & 0xFF;
        
        int rCurrent = (int) lerp(rOff, rOn, animationProgress);
        int gCurrent = (int) lerp(gOff, gOn, animationProgress);
        int bCurrent = (int) lerp(bOff, bOn, animationProgress);
        
        // アイコン色
        int rIconOff = (colorIconOff >> 16) & 0xFF;
        int gIconOff = (colorIconOff >> 8) & 0xFF;
        int bIconOff = colorIconOff & 0xFF;
        
        int rIconOn = (colorIconOn >> 16) & 0xFF;
        int gIconOn = (colorIconOn >> 8) & 0xFF;
        int bIconOn = colorIconOn & 0xFF;
        
        int rIcon = (int) lerp(rIconOff, rIconOn, animationProgress);
        int gIcon = (int) lerp(gIconOff, gIconOn, animationProgress);
        int bIcon = (int) lerp(bIconOff, bIconOn, animationProgress);

        g.pushStyle();
        
        // 1. 背景 (円形または角丸四角)
        // 4カラムグリッドなので、正方形に近い。完全な円にするのが最も美しい。
        float radius = Math.min(w, h) / 2.0f;
        // しかし、ControlCenterManager側ですでに角丸矩形の背景を描画している可能性がある。
        // ControlCenterManager.java を確認すると、
        // g.rect(itemX, itemY, itemW, itemH, 10); と背景を描画している。
        // ToggleItemはそれに重ねて描画するので、背景色はControlCenterManagerの背景を上書きする形になる。
        // アイテム自体が「ボタン」として見えるように、ここで独自の背景を描く。
        
        g.noStroke();
        g.fill(rCurrent, gCurrent, bCurrent);
        // セルいっぱいではなく、少しマージンを取って円形にするのがモダン。
        // ただしタップ領域はセル全体なので、見た目だけ調整。
        // ControlCenterManagerの背景描画と競合しないよう、ControlCenterManager側で
        // 「アイテムが背景を描画する場合は、マネージャー側の背景をスキップする」仕組みがないため、
        // 重ね書きになる。
        
        // ここでは「角丸四角」で塗りつぶす（マネージャーと一致させる）。
        // ただし、マネージャーのカード背景は「枠」のような役割。
        // ToggleItemは「中身」として振る舞うべきか、それとも「ボタンそのもの」か？
        // iOS風にするなら、ToggleItemが「背景色を持つボタン」そのものになるべき。
        
        g.rect(x, y, w, h, 12); // 半径はThemeに合わせて調整すべきだが一旦固定

        // 2. アイコン
        float iconSize = Math.min(w, h) * 0.4f;
        float iconX = x + (w - iconSize) / 2;
        float iconY = y + (h - iconSize) / 2;
        
        // ラベルがある場合は少し上にずらす
        boolean showLabel = true;
        if (showLabel) {
            iconY -= h * 0.1f;
        }

        if (icon != null) {
            g.tint(rIcon, gIcon, bIcon);
            g.image(icon, iconX, iconY, iconSize, iconSize);
            g.noTint();
        } else {
            // アイコン画像がない場合のフォールバック（文字アイコン）
            g.fill(rIcon, gIcon, bIcon);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.textSize(iconSize * 0.8f);
            
            // IDの頭文字などを表示するが、もっと適切な記号があればそれを使う
            // 簡易的にIDの頭文字を表示
            String symbol = displayName.substring(0, 1);
            // 特定のIDには絵文字を割り当ててみる
            if (id.contains("wifi")) symbol = "📶";
            else if (id.contains("blue")) symbol = "Bluetooth"; // フォント次第だが
            else if (id.contains("data")) symbol = "📡";
            else if (id.contains("air")) symbol = "✈";
            else if (id.contains("silent")) symbol = "🔔";
            else if (id.contains("low")) symbol = "🔋";
            else if (id.contains("rot")) symbol = "🔄";
            else if (id.contains("loc")) symbol = "📍";
            else if (id.contains("dark")) symbol = "🌙";
            
            // 絵文字が使えないフォント環境を考慮し、フォールバックはdisplayNameの頭文字
            // ここではシンプルに描画
            g.text(symbol, iconX + iconSize/2, iconY + iconSize/2);
        }

        // 3. ラベル (極小サイズ)
        if (showLabel) {
            g.fill(rIcon, gIcon, bIcon); // アイコンと同じ色
            g.textAlign(PConstants.CENTER, PConstants.TOP);
            g.textSize(10); // 小さく
            // はみ出さないようにクリップするか、短くする
            g.text(displayName, x + w/2, iconY + iconSize + 2);
        }

        g.popStyle();
    }

    private float lerp(float start, float stop, float amt) {
        return start + (stop - start) * amt;
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
            // System.out.println("ToggleItem: '" + displayName + "' toggled to " + isOn);
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