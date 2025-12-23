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
public class Dropdown extends BaseComponent implements Clickable, Focusable {

    private List<String> items;
    private List<String> filteredItems;
    private List<Integer> filteredIndices;
    private int selectedIndex = -1;
    private boolean expanded = false;

    private float itemHeight = 30;

    private int backgroundColor;
    private int itemBackgroundColor;
    private int selectedBackgroundColor;
    private int textColor;

    private boolean hovered = false;
    private boolean pressed = false;
    private boolean focused = false;
    
    // カスタム設定フラグ
    private boolean isCustomBackgroundColor = false;
    private boolean isCustomItemBackgroundColor = false;
    private boolean isCustomSelectedBackgroundColor = false;
    private boolean isCustomTextColor = false;
    
    // 編集・フィルタリング機能用
    private boolean editable = false;
    private String filterText = "";
    
    private Consumer<Integer> onSelectionChangeListener;

    public Dropdown(float x, float y, float width) {
        super(x, y, width, 35);
        this.items = new ArrayList<>();
        this.filteredItems = new ArrayList<>();
        this.filteredIndices = new ArrayList<>();
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

        // テーマ同期（カスタム設定優先）
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            if (!isCustomBackgroundColor) this.backgroundColor = theme.colorSurface();
            if (!isCustomItemBackgroundColor) this.itemBackgroundColor = theme.colorSurface();
            if (!isCustomSelectedBackgroundColor) this.selectedBackgroundColor = theme.colorPrimary();
            if (!isCustomTextColor) this.textColor = theme.colorOnSurface();
        }

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
        int onSurface = textColor;
        if (focused) {
            border = theme != null ? theme.colorPrimary() : selectedBackgroundColor;
        }

        g.fill(backgroundColor);
        g.stroke(border);
        g.strokeWeight(focused ? 2 : 1);
        g.rect(x, y, width, height, 5);

        // テキスト表示
        String textToDisplay;
        if (editable && focused) {
            textToDisplay = filterText;
        } else {
            textToDisplay = (selectedIndex >= 0 && selectedIndex < items.size())
                ? items.get(selectedIndex)
                : "選択してください";
        }

        g.fill(onSurface);
        g.textAlign(PApplet.LEFT, PApplet.CENTER);
        g.textSize(14);
        g.text(textToDisplay, x + 10, y + height / 2);

        // カーソル描画 (編集モードかつフォーカス時)
        if (editable && focused && System.currentTimeMillis() % 1000 < 500) {
            float textWidth = g.textWidth(textToDisplay);
            float cursorX = x + 10 + textWidth;
            g.stroke(onSurface);
            g.line(cursorX, y + 10, cursorX, y + height - 10);
        }

        // ドロップダウン矢印
        g.fill(onSurface);
        float arrowX = x + width - 20;
        float arrowY = y + height / 2;
        drawTriangle(g, arrowX, arrowY, 8, expanded ? -1 : 1);
    }

    private void drawDropdownList(PGraphics g) {
        List<String> displayItems = editable ? filteredItems : items;
        
        if (displayItems.isEmpty()) {
            // 空の場合の表示などは省略するか、"No items"などを表示してもよい
            return;
        }

        float listY = y + height + 2;
        float listHeight = displayItems.size() * itemHeight;

        // リストが長すぎる場合は制限することも検討すべきだが、簡易実装のためそのまま
        
        // 背景
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int border = theme != null ? theme.colorBorder() : 0xFFCCCCCC;
        int onSurface = theme != null ? theme.colorOnSurface() : textColor;
        g.fill(backgroundColor);
        g.stroke(border);
        g.strokeWeight(1);
        g.rect(x, listY, width, listHeight, 5);

        // アイテム
        for (int i = 0; i < displayItems.size(); i++) {
            float itemY = listY + i * itemHeight;
            
            // 選択状態の判定
            boolean isSelected;
            if (editable) {
                // フィルタリング時は、元のインデックスで比較
                int originalIndex = filteredIndices.get(i);
                isSelected = (originalIndex == selectedIndex);
            } else {
                isSelected = (i == selectedIndex);
            }

            g.fill(isSelected ? selectedBackgroundColor : itemBackgroundColor);
            g.noStroke();
            g.rect(x, itemY, width, itemHeight);

            g.fill(isSelected ? 0xFFFFFFFF : onSurface);
            g.textAlign(PApplet.LEFT, PApplet.CENTER);
            g.textSize(14);
            g.text(displayItems.get(i), x + 10, itemY + itemHeight / 2);
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
    public boolean isFocused() {
        return focused;
    }
    
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
        if (!focused) {
            expanded = false;
            // フォーカスが外れたら、テキストを選択中のものに戻すなどの処理が必要かも
            if (editable && selectedIndex >= 0) {
                filterText = items.get(selectedIndex);
            }
        } else {
            if (editable) {
                // フォーカス取得時、フィルタリングテキストを初期化（選択中のアイテム名にするか、空にするか）
                // ここでは空にして全件表示＆入力しやすくする
                filterText = "";
                updateFilteredItems();
                expanded = true;
            }
        }
    }

    @Override
    public boolean onMousePressed(int mouseX, int mouseY) {
        if (!enabled || !visible) return false;

        // メインボタンクリック
        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            requestFocus();
            expanded = !expanded;
            if (expanded && editable) {
                filterText = ""; // 展開時にフィルターリセット
                updateFilteredItems();
            }
            return true;
        }

        // ドロップダウンリストクリック
        if (expanded) {
            List<String> displayItems = editable ? filteredItems : items;
            float listY = y + height + 2;
            int clickedIndex = (int)((mouseY - listY) / itemHeight);
            
            if (clickedIndex >= 0 && clickedIndex < displayItems.size()) {
                if (editable) {
                    // フィルタリングされている場合は元のインデックスを取得
                    setSelectedIndex(filteredIndices.get(clickedIndex));
                    filterText = items.get(selectedIndex); // 選択したテキストを反映
                } else {
                    setSelectedIndex(clickedIndex);
                }
                expanded = false;
                return true;
            }
        }

        // 外側クリック処理は親UIシステムまたはInputManagerで行われることが多いが、
        // ここでは自分自身でフォーカス喪失判定を行う（BaseComponentの仕様による）
        if (focused) {
            clearFocus();
            expanded = false;
        }
        
        return false;
    }

    @Override
    public boolean onKeyPressed(char key, int keyCode) {
        if (!focused || !editable) return false;

        // Enter: 確定して閉じる
        if (keyCode == 10) { // Enter
            expanded = false;
            if (!filteredItems.isEmpty()) {
                // フィルタリング結果の一番上を選択（Enter時）
                setSelectedIndex(filteredIndices.get(0));
                filterText = items.get(selectedIndex);
            }
            return true;
        }
        
        // Backspace: 削除
        if (keyCode == 8) { // Backspace
            if (filterText.length() > 0) {
                filterText = filterText.substring(0, filterText.length() - 1);
                updateFilteredItems();
                expanded = true;
            }
            return true;
        }

        // 文字入力
        if (key >= 32 && key != 127 && key != 65535) {
            filterText += key;
            updateFilteredItems();
            expanded = true;
            return true;
        }

        return false;
    }

    private void updateFilteredItems() {
        filteredItems.clear();
        filteredIndices.clear();
        
        String query = filterText.toLowerCase();
        
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            if (item.toLowerCase().contains(query)) {
                filteredItems.add(item);
                filteredIndices.add(i);
            }
        }
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
    
    /**
     * ドロップダウンを編集可能（フィルタリング可能）にするかを設定する。
     * @param editable trueなら編集可能
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        if (editable) {
            updateFilteredItems();
        }
    }
    
    public boolean isEditable() {
        return editable;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        this.isCustomBackgroundColor = true;
    }

    public void setItemBackgroundColor(int color) {
        this.itemBackgroundColor = color;
        this.isCustomItemBackgroundColor = true;
    }

    public void setSelectedBackgroundColor(int color) {
        this.selectedBackgroundColor = color;
        this.isCustomSelectedBackgroundColor = true;
    }

    public void setTextColor(int color) {
        this.textColor = color;
        this.isCustomTextColor = true;
    }

    public void addItem(String item) {
        items.add(item);
        if (editable) {
            updateFilteredItems();
        }
    }

    public void clear() {
        items.clear();
        if (editable) {
            filteredItems.clear();
            filteredIndices.clear();
        }
        selectedIndex = -1;
        filterText = "";
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
