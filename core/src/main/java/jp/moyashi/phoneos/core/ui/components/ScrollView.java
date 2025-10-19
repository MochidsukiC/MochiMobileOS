package jp.moyashi.phoneos.core.ui.components;

import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * スクロール可能なビューコンポーネント。
 * 子要素をスクロール表示。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class ScrollView extends BaseComponent implements Container, Scrollable {

    private List<UIComponent> children;
    private int scrollOffset = 0;
    private int backgroundColor;
    private boolean showScrollBar = true;

    public ScrollView(float x, float y, float width, float height) {
        super(x, y, width, height);
        this.children = new ArrayList<>();
        this.backgroundColor = 0xFFFFFFFF;
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        g.pushStyle();

        // 背景
        g.fill(backgroundColor);
        g.noStroke();
        g.rect(x, y, width, height);

        // クリッピング + スクロール
        g.pushMatrix();
        // Processingにはクリッピング機能がないため、
        // 描画範囲チェックは子要素側で行う
        g.translate(0, -scrollOffset);

        for (UIComponent child : children) {
            if (child.isVisible()) {
                child.draw(g);
            }
        }

        g.popMatrix();

        // スクロールバー
        if (showScrollBar && isScrollBarVisible()) {
            drawScrollBar(g);
        }

        g.popStyle();
    }

    private void drawScrollBar(PGraphics g) {
        float scrollBarWidth = 8;
        float scrollBarX = x + width - scrollBarWidth - 2;
        float scrollBarHeight = height - 4;
        float contentHeight = getContentHeight();
        float thumbHeight = Math.max(20, scrollBarHeight * (height / contentHeight));
        float thumbY = y + 2 + (scrollBarHeight - thumbHeight) * (scrollOffset / (float)getMaxScrollOffset());

        g.fill(0xFFE0E0E0);
        g.noStroke();
        g.rect(scrollBarX, y + 2, scrollBarWidth, scrollBarHeight, 4);

        g.fill(0xFFAAAAAA);
        g.rect(scrollBarX, thumbY, scrollBarWidth, thumbHeight, 4);
    }

    private float getContentHeight() {
        if (children.isEmpty()) return 0;

        float maxBottom = 0;
        for (UIComponent child : children) {
            float bottom = child.getY() + child.getHeight();
            if (bottom > maxBottom) maxBottom = bottom;
        }
        return maxBottom - y;
    }

    @Override
    public void update() {
        for (UIComponent child : children) {
            child.update();
        }
    }

    @Override
    public int getScrollOffset() {
        return scrollOffset;
    }

    @Override
    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(offset, getMaxScrollOffset()));
    }

    @Override
    public int getMaxScrollOffset() {
        return (int)Math.max(0, getContentHeight() - height);
    }

    @Override
    public void scroll(int delta) {
        setScrollOffset(scrollOffset + delta);
    }

    @Override
    public void addChild(UIComponent child) {
        children.add(child);
    }

    @Override
    public void removeChild(UIComponent child) {
        children.remove(child);
    }

    @Override
    public void removeAllChildren() {
        children.clear();
    }

    @Override
    public List<UIComponent> getChildren() {
        return new ArrayList<>(children);
    }

    @Override
    public void layout() {
        // レイアウトマネージャーで管理
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    public void setShowScrollBar(boolean show) {
        this.showScrollBar = show;
    }
}
