package jp.moyashi.phoneos.core.ui.components;

import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ドロップダウンメニューコンポーネント。
 * 選択肢の一覧表示と選択。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class Dropdown extends BaseComponent implements Clickable {

    private List<String> items;
    private int selectedIndex = -1;
    private boolean expanded = false;

    private int backgroundColor;
    private int itemBackgroundColor;
    private int selectedBackgroundColor;
    private int textColor;
    private float itemHeight = 30;

    private boolean hovered = false;
    private boolean pressed = false;
    private Consumer<Integer> onSelectionChangeListener;

    public Dropdown(float x, float y, float width) {
        super(x, y, width, 35);
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

        // メインボタン
        drawMainButton(g);

        // ドロップダウンリスト
        if (expanded) {
            drawDropdownList(g);
        }

        g.popStyle();
    }

    private void drawMainButton(PGraphics g) {
        // テーマ同期
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int border = theme != null ? theme.colorBorder() : 0xFFCCCCCC;
        int onSurface = theme != null ? theme.colorOnSurface() : textColor;
        g.fill(backgroundColor);
        g.stroke(border);
        g.strokeWeight(1);
        g.rect(x, y, width, height, 5);

        // 選択されたテキスト
        String text = (selectedIndex >= 0 && selectedIndex < items.size())
            ? items.get(selectedIndex)
            : "選択してください";

        g.fill(onSurface);
        g.textAlign(PApplet.LEFT, PApplet.CENTER);
        g.textSize(14);
        g.text(text, x + 10, y + height / 2);

        // ドロップダウン矢印
        g.fill(onSurface);
        float arrowX = x + width - 20;
        float arrowY = y + height / 2;
        drawTriangle(g, arrowX, arrowY, 8, expanded ? -1 : 1);
    }

    private void drawDropdownList(PGraphics g) {
        float listY = y + height + 2;
        float listHeight = items.size() * itemHeight;

        // 背景
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int border = theme != null ? theme.colorBorder() : 0xFFCCCCCC;
        int onSurface = theme != null ? theme.colorOnSurface() : textColor;
        g.fill(backgroundColor);
        g.stroke(border);
        g.strokeWeight(1);
        g.rect(x, listY, width, listHeight, 5);

        // アイテム
        for (int i = 0; i < items.size(); i++) {
            float itemY = listY + i * itemHeight;

            g.fill(i == selectedIndex ? selectedBackgroundColor : itemBackgroundColor);
            g.noStroke();
            g.rect(x, itemY, width, itemHeight);

            g.fill(i == selectedIndex ? 0xFFFFFFFF : onSurface);
            g.textAlign(PApplet.LEFT, PApplet.CENTER);
            g.textSize(14);
            g.text(items.get(i), x + 10, itemY + itemHeight / 2);
        }
    }

    private void drawTriangle(PGraphics g, float x, float y, float size, int direction) {
        g.noStroke();
        if (direction > 0) {
            // 下向き
            g.triangle(x - size / 2, y - size / 3, x + size / 2, y - size / 3, x, y + size / 2);
        } else {
            // 上向き
            g.triangle(x - size / 2, y + size / 3, x + size / 2, y + size / 3, x, y - size / 2);
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

        // メインボタンクリック
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            expanded = !expanded;
            return true;
        }

        // ドロップダウンリストクリック
        if (expanded) {
            float listY = y + height + 2;
            int clickedIndex = (int)((mouseY - listY) / itemHeight);
            if (clickedIndex >= 0 && clickedIndex < items.size()) {
                setSelectedIndex(clickedIndex);
                expanded = false;
                return true;
            }
        }

        expanded = false;
        return false;
    }

    @Override
    public boolean onMouseReleased(int mouseX, int mouseY) {
        return false;
    }

    @Override
    public void onMouseMoved(int mouseX, int mouseY) {
        hovered = contains(mouseX, mouseY);
    }

    @Override
    public void setOnClickListener(Runnable listener) {
        // Dropdownでは onSelectionChange を使用
    }

    public void addItem(String item) {
        items.add(item);
    }

    public void clear() {
        items.clear();
        selectedIndex = -1;
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int index) {
        if (index >= 0 && index < items.size() && index != selectedIndex) {
            selectedIndex = index;
            if (onSelectionChangeListener != null) {
                onSelectionChangeListener.accept(index);
            }
        }
    }

    public String getSelectedItem() {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            return items.get(selectedIndex);
        }
        return null;
    }

    public void setOnSelectionChangeListener(Consumer<Integer> listener) {
        this.onSelectionChangeListener = listener;
    }
}
