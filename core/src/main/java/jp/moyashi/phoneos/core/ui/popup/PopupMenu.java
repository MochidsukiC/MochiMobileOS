package jp.moyashi.phoneos.core.ui.popup;

import java.util.ArrayList;
import java.util.List;

/**
 * ポップアップメニューを表すクラス。
 * 複数のPopupItemを持ち、位置とスタイリング情報を管理する。
 */
public class PopupMenu {
    
    /** ポップアップのタイトル（オプション） */
    private String title;
    
    /** ポップアップメニューのアイテムリスト */
    private List<PopupItem> items;
    
    /** 表示位置のX座標（-1の場合は中央） */
    private int x;
    
    /** 表示位置のY座標（-1の場合は中央） */
    private int y;
    
    /** メニューの幅（-1の場合は自動計算） */
    private int width;
    
    /** メニューの高さ（-1の場合は自動計算） */
    private int height;
    
    /** 背景色 */
    private int backgroundColor;
    
    /** テキスト色 */
    private int textColor;
    
    /** 境界線色 */
    private int borderColor;
    
    /** 無効化されたアイテムの色 */
    private int disabledColor;
    
    /**
     * デフォルトのPopupMenuを作成する。
     */
    public PopupMenu() {
        this.items = new ArrayList<>();
        this.x = -1; // 中央
        this.y = -1; // 中央
        this.width = -1; // 自動
        this.height = -1; // 自動
        this.backgroundColor = 0xFF2A2A2A; // ダークグレー
        this.textColor = 0xFFFFFFFF; // 白
        this.borderColor = 0xFF4A90E2; // 青
        this.disabledColor = 0xFF888888; // グレー
    }
    
    /**
     * タイトル付きのPopupMenuを作成する。
     * 
     * @param title ポップアップのタイトル
     */
    public PopupMenu(String title) {
        this();
        this.title = title;
    }
    
    /**
     * アイテムを追加する。
     * 
     * @param item 追加するアイテム
     * @return このPopupMenuインスタンス（メソッドチェーン用）
     */
    public PopupMenu addItem(PopupItem item) {
        if (item != null) {
            items.add(item);
        }
        return this;
    }
    
    /**
     * 便利メソッド：テキストとアクションでアイテムを追加する。
     * 
     * @param text 表示テキスト
     * @param action クリック時のアクション
     * @return このPopupMenuインスタンス（メソッドチェーン用）
     */
    public PopupMenu addItem(String text, Runnable action) {
        return addItem(new PopupItem(text, action));
    }
    
    /**
     * セパレーター（区切り線）を追加する。
     * 
     * @return このPopupMenuインスタンス（メソッドチェーン用）
     */
    public PopupMenu addSeparator() {
        return addItem(new PopupItem("---", null, false));
    }
    
    /**
     * すべてのアイテムを削除する。
     */
    public void clearItems() {
        items.clear();
    }
    
    /**
     * 表示位置を設定する。
     * 
     * @param x X座標（-1で中央）
     * @param y Y座標（-1で中央）
     * @return このPopupMenuインスタンス（メソッドチェーン用）
     */
    public PopupMenu setPosition(int x, int y) {
        this.x = x;
        this.y = y;
        return this;
    }
    
    /**
     * サイズを設定する。
     * 
     * @param width 幅（-1で自動）
     * @param height 高さ（-1で自動）
     * @return このPopupMenuインスタンス（メソッドチェーン用）
     */
    public PopupMenu setSize(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }
    
    /**
     * 色設定をする。
     * 
     * @param backgroundColor 背景色
     * @param textColor テキスト色
     * @param borderColor 境界線色
     * @return このPopupMenuインスタンス（メソッドチェーン用）
     */
    public PopupMenu setColors(int backgroundColor, int textColor, int borderColor) {
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.borderColor = borderColor;
        return this;
    }
    
    // Getters
    
    public String getTitle() { return title; }
    public List<PopupItem> getItems() { return new ArrayList<>(items); }
    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getBackgroundColor() { return backgroundColor; }
    public int getTextColor() { return textColor; }
    public int getBorderColor() { return borderColor; }
    public int getDisabledColor() { return disabledColor; }
    
    /**
     * 指定した座標がメニュー内かどうかをチェックする。
     * 
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @param actualX 実際の描画X座標
     * @param actualY 実際の描画Y座標
     * @param actualWidth 実際の描画幅
     * @param actualHeight 実際の描画高さ
     * @return メニュー内の場合true
     */
    public boolean isInside(int mouseX, int mouseY, int actualX, int actualY, int actualWidth, int actualHeight) {
        return mouseX >= actualX && mouseX <= actualX + actualWidth && 
               mouseY >= actualY && mouseY <= actualY + actualHeight;
    }
    
    /**
     * 指定した座標でクリックされたアイテムを取得する。
     * 
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @param actualX 実際の描画X座標
     * @param actualY 実際の描画Y座標
     * @param actualWidth 実際の描画幅
     * @param itemHeight アイテム1つあたりの高さ
     * @return クリックされたアイテム、またはnull
     */
    public PopupItem getClickedItem(int mouseX, int mouseY, int actualX, int actualY, int actualWidth, int itemHeight) {
        if (!isInside(mouseX, mouseY, actualX, actualY, actualWidth, items.size() * itemHeight)) {
            return null;
        }
        
        int relativeY = mouseY - actualY;
        if (title != null) {
            relativeY -= itemHeight; // タイトル分の高さを引く
        }
        
        int itemIndex = relativeY / itemHeight;
        if (itemIndex >= 0 && itemIndex < items.size()) {
            PopupItem item = items.get(itemIndex);
            // セパレーターは選択不可
            if (item.getText().equals("---")) {
                return null;
            }
            return item;
        }
        
        return null;
    }
}