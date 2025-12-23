package jp.moyashi.phoneos.core.controls;

import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.render.TextRenderer;
import jp.moyashi.phoneos.core.render.TextRendererContext;
import jp.moyashi.phoneos.core.ui.theme.ThemeContext;
import jp.moyashi.phoneos.core.ui.theme.ThemeEngine;
import jp.moyashi.phoneos.core.util.EmojiUtil;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.function.Consumer;

/**
 * コントロールセンター用の縦型スライダーアイテム。
 * 音量や輝度などの連続値を調整するために使用する。
 * iOSのような、バー全体が上下して値を表現するモダンなデザイン。
 */
public class SliderItem implements IControlCenterItem {

    private final String id;
    private final String displayName;
    private final String iconSymbol; // 絵文字アイコン（例: "☀", "🔊"）
    private float value; // 0.0 to 1.0
    private final Consumer<Float> onValueChanged;
    private final GridAlignment gridAlignment;
    private boolean isDragging = false;

    // インタラクション用レイアウトキャッシュ
    private float lastY, lastH;

    public SliderItem(String id, String displayName, String iconSymbol, float initialValue, Consumer<Float> onValueChanged) {
        this(id, displayName, iconSymbol, initialValue, GridAlignment.LEFT, onValueChanged);
    }

    public SliderItem(String id, String displayName, String iconSymbol, float initialValue, GridAlignment alignment, Consumer<Float> onValueChanged) {
        this.id = id;
        this.displayName = displayName;
        this.iconSymbol = iconSymbol;
        this.value = Math.max(0.0f, Math.min(1.0f, initialValue));
        this.gridAlignment = alignment;
        this.onValueChanged = onValueChanged;
    }

    @Override
    public String getId() { return id; }

    @Override
    public String getDisplayName() { return displayName; }

    @Override
    public String getDescription() { return displayName + ": " + (int)(value * 100) + "%"; }

    @Override
    public int getColumnSpan() { return 1; }

    @Override
    public int getRowSpan() { return 2; }

    @Override
    public GridAlignment getGridAlignment() { return gridAlignment; }

    @Override
    public boolean isDraggable() { return true; }

    public void setValue(float newValue) {
        this.value = Math.max(0.0f, Math.min(1.0f, newValue));
    }

    @Override
    public void draw(PGraphics g, float x, float y, float w, float h) {
        captureLayout(y, h);
        drawModernSlider(g, x, y, w, h);
    }

    @Override
    public void draw(PApplet p, float x, float y, float w, float h) {
        captureLayout(y, h);
        drawModernSlider(p.g, x, y, w, h);
    }

    private void captureLayout(float y, float h) {
        this.lastY = y;
        this.lastH = h;
    }

    private void drawModernSlider(PGraphics g, float x, float y, float w, float h) {
        ThemeEngine theme = ThemeContext.getTheme();
        float radius = theme != null ? theme.radiusLg() : 16; // 大きめの角丸

        g.pushStyle();

        // 1. 背景 (トラック) - 半透明のダークグレー
        // ControlCenterManagerの背景色と調和させる
        g.noStroke();
        g.fill(30, 30, 35, 180); 
        g.rect(x, y, w, h, radius);

        // 2. フィル (値のバー) - 白
        // 下から伸びる
        float fillHeight = h * value;
        float fillY = y + h - fillHeight;

        if (value > 0.01f) {
            g.fill(255, 255, 255, 240); // ほぼ不透明な白
            
            // 上部の角丸処理: 100%に近い時のみ角丸にする
            // Processingのrect(x, y, w, h, tl, tr, br, bl)
            float topRadius = (value > 0.95f) ? radius : 0;
            g.rect(x, fillY, w, fillHeight, topRadius, topRadius, radius, radius);
        }

        // 3. アイコン (下部中央)
        // フィルがアイコンにかかると視認性が悪くなるため、フィルの高さに応じて色を反転させるのがベストだが
        // 簡易的には「常に表示」かつ「背景に応じた色」
        // ここでは「フィルが50%を超えたらアイコンを暗くする」などのロジックを入れる
        
        boolean isIconOnWhite = (value > 0.15f); // アイコン位置（下部15%）までバーが来ているか
        int iconColor = isIconOnWhite ? 0xFF555555 : 0xFFFFFFFF; // 白背景ならグレー、黒背景なら白

        float iconSize = 24;
        float iconX = x + w / 2;
        float iconY = y + h - 20; // 基準位置（下から20px）

        // テキストレンダラーを使用して絵文字を描画
        TextRenderer textRenderer = TextRendererContext.getTextRenderer();
        if (textRenderer != null && EmojiUtil.containsEmoji(iconSymbol)) {
            float textWidth = textRenderer.getTextWidth(g, iconSymbol, iconSize);
            
            // TextRendererは通常(x,y)を左上として描画するため、
            // 中央揃え(X)と下揃え(Y)のために座標を補正する
            float drawX = iconX - textWidth / 2;
            float drawY = iconY - iconSize; // アイコンの高さ分上にずらす（下端基準にするため）
            
            // 文字色設定（TextRendererが色設定を使用する場合）
            g.fill((iconColor >> 16) & 0xFF, (iconColor >> 8) & 0xFF, iconColor & 0xFF);
            
            textRenderer.drawText(g, iconSymbol, drawX, drawY, iconSize);
        } else {
            // フォールバック（標準のtext()はtextAlignに従う）
            g.fill((iconColor >> 16) & 0xFF, (iconColor >> 8) & 0xFF, iconColor & 0xFF);
            g.textAlign(PApplet.CENTER, PApplet.BOTTOM);
            g.textSize(iconSize);
            g.text(iconSymbol, iconX, iconY);
        }

        g.popStyle();
    }

    @Override
    public boolean onGesture(GestureEvent event) {
        if (lastH <= 0) return false;

        switch (event.getType()) {
            case TAP:
            case DRAG_START:
            case DRAG_MOVE:
                updateValueFromGesture(event.getCurrentY());
                isDragging = true;
                return true;
            case DRAG_END:
                isDragging = false;
                return true;
            default:
                return false;
        }
    }

    private void updateValueFromGesture(float touchY) {
        // Y座標は下に行くほど大きい。
        // スライダーは下が0、上が1。
        float relativeY = touchY - lastY;
        float ratio = 1.0f - (relativeY / lastH);
        
        float newValue = Math.max(0.0f, Math.min(1.0f, ratio));
        
        if (Math.abs(this.value - newValue) > 0.001f) {
            this.value = newValue;
            if (onValueChanged != null) {
                onValueChanged.accept(this.value);
            }
        }
    }
}