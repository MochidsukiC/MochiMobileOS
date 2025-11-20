package jp.moyashi.phoneos.core.ui.components;

import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * リストビューコンポーネント。
 * 項目のリスト表示、選択、スクロール対応。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class ListView extends BaseComponent implements Scrollable {

    public static class ListItem {
        public String text;
        public Object data;

        public ListItem(String text) {
            this.text = text;
            this.data = null;
        }

        public ListItem(String text, Object data) {
            this.text = text;
            this.data = data;
        }
    }

    private List<ListItem> items;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private float itemHeight = 40;

    private int backgroundColor;
    private int itemBackgroundColor;
    private int selectedBackgroundColor;
    private int textColor;

    private Consumer<Integer> onItemClickListener;

    public ListView(float x, float y, float width, float height) {
        super(x, y, width, height);
        this.items = new ArrayList<>();
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            this.backgroundColor = theme.colorSurface();
            this.itemBackgroundColor = theme.colorSurface();
            this.selectedBackgroundColor = theme.colorPrimary();
            this.textColor = theme.colorOnSurface();
        } else {
            this.backgroundColor = 0xFFFFFFFF;
            this.itemBackgroundColor = 0xFFF5F5F5;
            this.selectedBackgroundColor = 0xFF4A90E2;
            this.textColor = 0xFF000000;
        }
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        g.pushStyle();

        // テーマ更新
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            this.backgroundColor = theme.colorSurface();
            this.itemBackgroundColor = theme.colorSurface();
            this.selectedBackgroundColor = theme.colorPrimary();
            this.textColor = theme.colorOnSurface();
        }

        // 背景
        int alpha = 200;
        g.fill((backgroundColor>>16)&0xFF, (backgroundColor>>8)&0xFF, backgroundColor&0xFF, alpha);
        g.noStroke();
        g.rect(x, y, width, height);

        // アイテム描画
        g.pushMatrix();
        g.translate(0, -scrollOffset);

        for (int i = 0; i < items.size(); i++) {
            float itemY = y + i * itemHeight;

            // 表示範囲外はスキップ
            if (itemY + scrollOffset + itemHeight < y || itemY + scrollOffset > y + height) {
                continue;
            }

            drawItem(g, i, itemY);
        }

        g.popMatrix();

        g.popStyle();
    }

    private void drawItem(PGraphics g, int index, float itemY) {
        ListItem item = items.get(index);
        boolean isSelected = (index == selectedIndex);

        // 背景
        int currentBgColor = isSelected ? selectedBackgroundColor : itemBackgroundColor;
        int alpha = isSelected ? 255 : 200; // Selected items are solid, others semi-transparent
        g.fill((currentBgColor>>16)&0xFF, (currentBgColor>>8)&0xFF, currentBgColor&0xFF, alpha);
        g.noStroke();
        g.rect(x, itemY, width, itemHeight);

        // テキスト
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int currentTextColor = isSelected ? (theme != null ? theme.colorOnPrimary() : 0xFFFFFFFF) : textColor;
        g.fill(currentTextColor);
        g.textAlign(PApplet.LEFT, PApplet.CENTER);
        g.textSize(14);
        g.text(item.text, x + 10, itemY + itemHeight / 2);

        // 区切り線
        var themeLocal = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int border = themeLocal != null ? themeLocal.colorBorder() : 0xFFE0E0E0;
        g.stroke(border);
        g.strokeWeight(1);
        g.line(x, itemY + itemHeight, x + width, itemY + itemHeight);
    }

    public boolean onMousePressed(int mouseX, int mouseY) {
        if (!enabled || !visible) return false;

        if (!contains(mouseX, mouseY)) return false;

        // クリックされたアイテムを特定
        int clickedIndex = getItemIndexAt(mouseX, mouseY);
        if (clickedIndex >= 0 && clickedIndex < items.size()) {
            selectedIndex = clickedIndex;
            if (onItemClickListener != null) {
                onItemClickListener.accept(clickedIndex);
            }
            return true;
        }
        return false;
    }

    private int getItemIndexAt(int mouseX, int mouseY) {
        float relativeY = mouseY - y + scrollOffset;
        return (int)(relativeY / itemHeight);
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
        return (int)Math.max(0, items.size() * itemHeight - height);
    }

    @Override
    public void scroll(int delta) {
        setScrollOffset(scrollOffset + delta);
    }

    public void addItem(String text) {
        items.add(new ListItem(text));
    }

    public void addItem(String text, Object data) {
        items.add(new ListItem(text, data));
    }

    public void clear() {
        items.clear();
        selectedIndex = -1;
        scrollOffset = 0;
    }

    public void setOnItemClickListener(Consumer<Integer> listener) {
        this.onItemClickListener = listener;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public ListItem getSelectedItem() {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            return items.get(selectedIndex);
        }
        return null;
    }

    public List<ListItem> getItems() {
        return new ArrayList<>(items);
    }
}
