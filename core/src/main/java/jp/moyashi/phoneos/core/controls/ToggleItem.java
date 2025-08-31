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
 * @version 1.0
 * @since 1.0
 */
public class ToggleItem implements IControlCenterItem {
    
    /** アイテムの一意識別子 */
    private final String id;
    
    /** 表示名 */
    private final String displayName;
    
    /** 説明 */
    private final String description;
    
    /** アイコン画像（オプション） */
    private final PImage icon;
    
    /** 現在の状態（ON/OFF） */
    private boolean isOn;
    
    /** 状態が変更された時に呼び出されるアクション */
    private final Consumer<Boolean> onStateChanged;
    
    /** 有効/無効フラグ */
    private boolean enabled;
    
    /** 表示/非表示フラグ */
    private boolean visible;
    
    /** アニメーション用の状態遷移進行度（0.0-1.0） */
    private float animationProgress;
    
    /** アニメーションの目標進行度 */
    private float targetAnimationProgress;
    
    /** アニメーション速度 */
    private static final float ANIMATION_SPEED = 0.15f;
    
    /**
     * 新しいToggleItemを作成する。
     * 
     * @param id アイテムの一意識別子
     * @param displayName 表示名
     * @param description 説明
     * @param icon アイコン画像（nullの場合はデフォルトアイコンを使用）
     * @param initialState 初期状態（true = ON, false = OFF）
     * @param onStateChanged 状態変更時のアクション
     */
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
        
        // アニメーション状態を初期化
        this.targetAnimationProgress = initialState ? 1.0f : 0.0f;
        this.animationProgress = this.targetAnimationProgress;
        
        System.out.println("ToggleItem: Created toggle '" + displayName + "' (ID: " + id + ") with initial state: " + initialState);
    }
    
    /**
     * アイコンなしでToggleItemを作成する便利なコンストラクタ。
     */
    public ToggleItem(String id, String displayName, String description, 
                     boolean initialState, Consumer<Boolean> onStateChanged) {
        this(id, displayName, description, null, initialState, onStateChanged);
    }
    
    @Override
    public void draw(PApplet p, float x, float y, float w, float h) {
        // アニメーション進行度を更新
        updateAnimation();
        
        p.pushMatrix();
        p.pushStyle();
        
        try {
            // 背景描画
            drawBackground(p, x, y, w, h);
            
            // アイコン描画
            drawIcon(p, x, y, w, h);
            
            // ラベル描画
            drawLabel(p, x, y, w, h);
            
            // トグルスイッチ描画
            drawToggleSwitch(p, x, y, w, h);
            
        } finally {
            p.popStyle();
            p.popMatrix();
        }
    }
    
    /**
     * 背景を描画する。
     */
    private void drawBackground(PApplet p, float x, float y, float w, float h) {
        // 状態に応じて背景色を設定
        if (!enabled) {
            p.fill(100, 100, 100, 100); // グレーアウト
        } else if (isOn) {
            // ONの場合は明るい背景色
            int alpha = (int) (50 + 100 * animationProgress);
            p.fill(100, 150, 255, alpha);
        } else {
            // OFFの場合はデフォルトの背景色
            p.fill(200, 200, 200, 80);
        }
        
        p.noStroke();
        p.rect(x, y, w, h, 12); // 角丸四角形
        
        // 境界線
        if (enabled) {
            p.stroke(150, 150, 150);
        } else {
            p.stroke(100, 100, 100);
        }
        p.strokeWeight(1);
        p.noFill();
        p.rect(x, y, w, h, 12);
    }
    
    /**
     * アイコンを描画する。
     */
    private void drawIcon(PApplet p, float x, float y, float w, float h) {
        float iconSize = Math.min(w, h) * 0.2f;
        float iconX = x + 15;
        float iconY = y + (h - iconSize) / 2;
        
        if (icon != null) {
            // カスタムアイコンを描画
            if (enabled) {
                p.tint(255, 255, 255, 255);
            } else {
                p.tint(100, 100, 100, 150);
            }
            p.image(icon, iconX, iconY, iconSize, iconSize);
            p.noTint();
        } else {
            // デフォルトアイコンを描画（シンプルな円）
            if (enabled) {
                p.fill(isOn ? 255 : 150, isOn ? 255 : 150, isOn ? 255 : 150);
            } else {
                p.fill(100, 100, 100);
            }
            p.noStroke();
            p.ellipse(iconX + iconSize/2, iconY + iconSize/2, iconSize * 0.8f, iconSize * 0.8f);
        }
    }
    
    /**
     * ラベルを描画する。
     */
    private void drawLabel(PApplet p, float x, float y, float w, float h) {
        // 表示名
        if (enabled) {
            p.fill(255, 255, 255);
        } else {
            p.fill(150, 150, 150);
        }
        p.textAlign(PApplet.LEFT, PApplet.TOP);
        p.textSize(14);
        p.text(displayName, x + 50, y + 8);
        
        // 説明（小さいフォント）
        if (enabled) {
            p.fill(200, 200, 200);
        } else {
            p.fill(120, 120, 120);
        }
        p.textSize(10);
        p.text(description, x + 50, y + 25);
    }
    
    /**
     * トグルスイッチを描画する。
     */
    private void drawToggleSwitch(PApplet p, float x, float y, float w, float h) {
        float switchWidth = 50;
        float switchHeight = 24;
        float switchX = x + w - switchWidth - 15;
        float switchY = y + (h - switchHeight) / 2;
        
        // スイッチ背景
        if (isOn) {
            int green = (int) (100 + 155 * animationProgress);
            p.fill(100, green, 100);
        } else {
            p.fill(150, 150, 150);
        }
        p.noStroke();
        p.rect(switchX, switchY, switchWidth, switchHeight, switchHeight/2);
        
        // スイッチつまみ
        float knobSize = switchHeight - 4;
        float knobX = switchX + 2 + (switchWidth - knobSize - 4) * animationProgress;
        float knobY = switchY + 2;
        
        if (enabled) {
            p.fill(255, 255, 255);
        } else {
            p.fill(200, 200, 200);
        }
        p.ellipse(knobX + knobSize/2, knobY + knobSize/2, knobSize, knobSize);
        
        // つまみの影
        p.fill(0, 0, 0, 50);
        p.ellipse(knobX + knobSize/2 + 1, knobY + knobSize/2 + 1, knobSize * 0.8f, knobSize * 0.8f);
    }
    
    /**
     * アニメーション進行度を更新する。
     */
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
            // トグル状態を切り替え
            toggle();
            System.out.println("ToggleItem: '" + displayName + "' toggled to " + isOn);
            return true;
        }
        
        return false;
    }
    
    /**
     * トグル状態を切り替える。
     */
    public void toggle() {
        if (!enabled) {
            return;
        }
        
        setOn(!isOn);
    }
    
    /**
     * トグルの状態を設定する。
     * 
     * @param on 新しい状態
     */
    public void setOn(boolean on) {
        if (this.isOn != on) {
            this.isOn = on;
            this.targetAnimationProgress = on ? 1.0f : 0.0f;
            
            // 状態変更アクションを実行
            if (onStateChanged != null) {
                try {
                    onStateChanged.accept(on);
                } catch (Exception e) {
                    System.err.println("ToggleItem: Error in state change action: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 現在のトグル状態を取得する。
     * 
     * @return ONの場合true、OFFの場合false
     */
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
    
    /**
     * アイテムの有効/無効を設定する。
     * 
     * @param enabled 有効にする場合true
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * アイテムの表示/非表示を設定する。
     * 
     * @param visible 表示する場合true
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
}