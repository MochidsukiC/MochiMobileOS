package jp.moyashi.phoneos.core.ui.popup;

import processing.core.PApplet;
import processing.core.PGraphics;
import jp.moyashi.phoneos.core.input.GestureEvent;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * グローバルポップアップマネージャー。
 * すべてのアプリケーションから使用可能なポップアップシステムを提供する。
 * ポップアップはすべての描画の最上位に表示される。
 */
public class PopupManager {
    
    /** 現在表示中のポップアップメニュー */
    private PopupMenu currentPopup;
    
    /** ポップアップ待機キュー */
    private ConcurrentLinkedQueue<PopupMenu> popupQueue;
    
    /** 自動クローズタイマー（0で無効） */
    private long autoCloseTime;
    
    /** 自動クローズ遅延時間（ミリ秒） */
    private static final long AUTO_CLOSE_DELAY = 5000; // 5秒
    
    /** アイテムの高さ */
    private static final int ITEM_HEIGHT = 40;
    
    /** メニューのパディング */
    private static final int MENU_PADDING = 10;
    
    /** 最小メニュー幅 */
    private static final int MIN_MENU_WIDTH = 150;

    /** 影のオフセット */
    private static final int SHADOW_OFFSET = 5;

    /** 区切り線の色 */
    private static final int SEPARATOR_COLOR = 0xFF666666;

    /**
     * PopupManagerを作成する。
     */
    public PopupManager() {
        this.popupQueue = new ConcurrentLinkedQueue<>();
        this.autoCloseTime = 0;
        
        System.out.println("PopupManager: Global popup manager initialized");
    }
    
    /**
     * ポップアップメニューを表示する。
     * 既に表示中のポップアップがある場合は、キューに追加される。
     * 
     * @param popup 表示するポップアップメニュー
     */
    public void showPopup(PopupMenu popup) {
        if (popup == null) {
            System.err.println("PopupManager: Cannot show null popup");
            return;
        }
        
        System.out.println("PopupManager: Showing popup with " + popup.getItems().size() + " items");
        
        if (currentPopup == null) {
            // すぐに表示
            currentPopup = popup;
            autoCloseTime = System.currentTimeMillis() + AUTO_CLOSE_DELAY;
            System.out.println("PopupManager: ✅ Popup displayed immediately");
        } else {
            // キューに追加
            popupQueue.offer(popup);
            System.out.println("PopupManager: Popup queued (queue size: " + popupQueue.size() + ")");
        }
    }
    
    /**
     * 現在のポップアップを閉じる。
     * キューに待機中のポップアップがある場合は、次のポップアップを表示する。
     */
    public void closeCurrentPopup() {
        if (currentPopup != null) {
            System.out.println("PopupManager: Closing current popup");
            currentPopup = null;
            autoCloseTime = 0;
            
            // キューから次のポップアップを取得
            PopupMenu nextPopup = popupQueue.poll();
            if (nextPopup != null) {
                System.out.println("PopupManager: Showing next popup from queue");
                showPopup(nextPopup);
            }
        }
    }
    
    /**
     * すべてのポップアップを閉じて、キューもクリアする。
     */
    public void closeAllPopups() {
        System.out.println("PopupManager: Closing all popups and clearing queue");
        currentPopup = null;
        autoCloseTime = 0;
        popupQueue.clear();
    }
    
    /**
     * 現在ポップアップが表示されているかどうかを確認する。
     *
     * @return ポップアップが表示中の場合true
     */
    public boolean isPopupVisible() {
        return currentPopup != null;
    }

    /**
     * ジェスチャーイベントを処理する。
     *
     * @param event ジェスチャーイベント
     * @return ポップアップがイベントを処理した場合true
     */
    public boolean handleGesture(GestureEvent event) {
        if (currentPopup == null) {
            return false;
        }

        // タップイベントの場合、マウスクリックとして処理
        if (event.getType() == jp.moyashi.phoneos.core.input.GestureType.TAP) {
            return handleMouseClick((int)event.getCurrentX(), (int)event.getCurrentY());
        }

        return false;
    }
    
    /**
     * ポップアップを描画する（PGraphics対応）。
     * この メソッドはKernelのdraw()から呼び出される必要がある。
     *
     * @param g 描画用のPGraphicsインスタンス
     */
    public void draw(PGraphics g) {
        // 自動クローズ処理
        if (currentPopup != null && autoCloseTime > 0 && System.currentTimeMillis() > autoCloseTime) {
            System.out.println("PopupManager: Auto-closing popup after timeout");
            closeCurrentPopup();
            return;
        }

        // ポップアップ描画
        if (currentPopup != null) {
            drawPopup(g, currentPopup);
        }
    }

    /**
     * ポップアップを描画する（PApplet互換性メソッド）。
     * この メソッドはKernelのdraw()から呼び出される必要がある。
     *
     * @param p 描画用のPAppletインスタンス
     */
    public void draw(PApplet p) {
        // 自動クローズ処理
        if (currentPopup != null && autoCloseTime > 0 && System.currentTimeMillis() > autoCloseTime) {
            System.out.println("PopupManager: Auto-closing popup after timeout");
            closeCurrentPopup();
            return;
        }

        // ポップアップ描画
        if (currentPopup != null) {
            drawPopup(p, currentPopup);
        }
    }
    
    /**
     * マウスクリックイベントを処理する。
     * この メソッドはKernelのmousePressed()から呼び出される必要がある。
     * 
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @return ポップアップがクリックを処理した場合true
     */
    public boolean handleMouseClick(int mouseX, int mouseY) {
        if (currentPopup == null) {
            return false;
        }
        
        System.out.println("PopupManager: Processing mouse click at (" + mouseX + ", " + mouseY + ")");
        
        // メニューの実際の位置とサイズを計算
        int[] bounds = calculateMenuBounds(currentPopup, 400, 600); // 仮のスクリーンサイズ
        int actualX = bounds[0];
        int actualY = bounds[1];
        int actualWidth = bounds[2];
        int actualHeight = bounds[3];
        
        // メニュー内クリックかチェック
        if (currentPopup.isInside(mouseX, mouseY, actualX, actualY, actualWidth, actualHeight)) {
            // アイテムクリック処理
            PopupItem clickedItem = currentPopup.getClickedItem(mouseX, mouseY, actualX, actualY, actualWidth, ITEM_HEIGHT);
            if (clickedItem != null && clickedItem.isEnabled()) {
                System.out.println("PopupManager: Clicked item: " + clickedItem.getText());
                clickedItem.executeAction();
                closeCurrentPopup();
                return true;
            }
        } else {
            // メニュー外クリック - ポップアップを閉じる
            System.out.println("PopupManager: Click outside popup, closing");
            closeCurrentPopup();
            return true; // クリックは処理済み（下のレイヤーに渡さない）
        }
        
        return false;
    }
    
    /**
     * ポップアップを描画する（PGraphics版）。
     *
     * @param g 描画用のPGraphicsインスタンス
     * @param popup 描画するポップアップ
     */
    private void drawPopup(PGraphics g, PopupMenu popup) {
        // メニューの位置とサイズを計算
        int[] bounds = calculateMenuBounds(popup, g.width, g.height);
        int menuX = bounds[0];
        int menuY = bounds[1];
        int menuWidth = bounds[2];
        int menuHeight = bounds[3];
        int cornerRadius = popup.getCornerRadius();

        // 半透明背景オーバーレイ
        g.fill(0, 0, 0, 120);
        g.noStroke();
        g.rect(0, 0, g.width, g.height);

        // ドロップシャドウ描画
        g.fill(popup.getShadowColor());
        g.noStroke();
        g.rect(menuX + SHADOW_OFFSET, menuY + SHADOW_OFFSET, menuWidth, menuHeight, cornerRadius);

        // メニュー背景
        g.fill(popup.getBackgroundColor());
        g.noStroke();
        g.rect(menuX, menuY, menuWidth, menuHeight, cornerRadius);

        // タイトル描画
        int currentY = menuY + MENU_PADDING;
        if (popup.getTitle() != null) {
            g.fill(popup.getTextColor());
            g.textAlign(PApplet.CENTER, PApplet.CENTER);
            g.textSize(16);
            g.text(popup.getTitle(), menuX + menuWidth / 2, currentY + ITEM_HEIGHT / 2);
            currentY += ITEM_HEIGHT;

            // タイトル区切り線
            g.stroke(SEPARATOR_COLOR);
            g.strokeWeight(1);
            g.line(menuX + MENU_PADDING, currentY, menuX + menuWidth - MENU_PADDING, currentY);
            currentY += 5;
        }

        // アイテム描画
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(14);

        for (PopupItem item : popup.getItems()) {
            if (item.getText().equals("---")) {
                // セパレーター
                g.stroke(SEPARATOR_COLOR);
                g.strokeWeight(1);
                g.line(menuX + MENU_PADDING, currentY + ITEM_HEIGHT / 2,
                      menuX + menuWidth - MENU_PADDING, currentY + ITEM_HEIGHT / 2);
            } else {
                // 通常アイテム
                g.fill(item.isEnabled() ? popup.getTextColor() : popup.getDisabledColor());
                g.noStroke();
                g.text(item.getText(), menuX + menuWidth / 2, currentY + ITEM_HEIGHT / 2);
            }
            currentY += ITEM_HEIGHT;
        }
    }

    /**
     * ポップアップを描画する（PApplet互換性メソッド）。
     *
     * @param p 描画用のPAppletインスタンス
     * @param popup 描画するポップアップ
     */
    private void drawPopup(PApplet p, PopupMenu popup) {
        // メニューの位置とサイズを計算
        int[] bounds = calculateMenuBounds(popup, p.width, p.height);
        int menuX = bounds[0];
        int menuY = bounds[1];
        int menuWidth = bounds[2];
        int menuHeight = bounds[3];
        int cornerRadius = popup.getCornerRadius();
        
        // 半透明背景オーバーレイ
        p.fill(0, 0, 0, 120);
        p.noStroke();
        p.rect(0, 0, p.width, p.height);

        // ドロップシャドウ描画
        p.fill(popup.getShadowColor());
        p.noStroke();
        p.rect(menuX + SHADOW_OFFSET, menuY + SHADOW_OFFSET, menuWidth, menuHeight, cornerRadius);
        
        // メニュー背景
        p.fill(popup.getBackgroundColor());
        p.noStroke();
        p.rect(menuX, menuY, menuWidth, menuHeight, cornerRadius);
        
        // タイトル描画
        int currentY = menuY + MENU_PADDING;
        if (popup.getTitle() != null) {
            p.fill(popup.getTextColor());
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text(popup.getTitle(), menuX + menuWidth / 2, currentY + ITEM_HEIGHT / 2);
            currentY += ITEM_HEIGHT;
            
            // タイトル区切り線
            p.stroke(SEPARATOR_COLOR);
            p.strokeWeight(1);
            p.line(menuX + MENU_PADDING, currentY, menuX + menuWidth - MENU_PADDING, currentY);
            currentY += 5;
        }
        
        // アイテム描画
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(14);
        
        for (PopupItem item : popup.getItems()) {
            if (item.getText().equals("---")) {
                // セパレーター
                p.stroke(SEPARATOR_COLOR);
                p.strokeWeight(1);
                p.line(menuX + MENU_PADDING, currentY + ITEM_HEIGHT / 2, 
                      menuX + menuWidth - MENU_PADDING, currentY + ITEM_HEIGHT / 2);
            } else {
                // 通常アイテム
                p.fill(item.isEnabled() ? popup.getTextColor() : popup.getDisabledColor());
                p.noStroke();
                p.text(item.getText(), menuX + menuWidth / 2, currentY + ITEM_HEIGHT / 2);
            }
            currentY += ITEM_HEIGHT;
        }
    }
    
    /**
     * メニューの位置とサイズを計算する。
     * 
     * @param popup ポップアップメニュー
     * @param screenWidth スクリーン幅
     * @param screenHeight スクリーン高さ
     * @return [x, y, width, height]
     */
    private int[] calculateMenuBounds(PopupMenu popup, int screenWidth, int screenHeight) {
        // 幅計算
        int menuWidth = popup.getWidth();
        if (menuWidth <= 0) {
            menuWidth = MIN_MENU_WIDTH;
            // テキスト幅に基づく自動計算は簡略化
        }
        
        // 高さ計算
        int menuHeight = popup.getHeight();
        if (menuHeight <= 0) {
            int itemCount = popup.getItems().size();
            if (popup.getTitle() != null) itemCount++; // タイトル分
            menuHeight = (itemCount * ITEM_HEIGHT) + (MENU_PADDING * 2);
            if (popup.getTitle() != null) menuHeight += 5; // セパレーター分
        }
        
        // 位置計算
        int menuX = popup.getX();
        int menuY = popup.getY();
        
        if (menuX < 0) {
            menuX = (screenWidth - menuWidth) / 2; // 中央
        }
        if (menuY < 0) {
            menuY = (screenHeight - menuHeight) / 2; // 中央
        }
        
        // 画面外に出ないよう調整
        if (menuX + menuWidth > screenWidth) {
            menuX = screenWidth - menuWidth - 10;
        }
        if (menuY + menuHeight > screenHeight) {
            menuY = screenHeight - menuHeight - 10;
        }
        if (menuX < 0) menuX = 10;
        if (menuY < 0) menuY = 10;
        
        return new int[]{menuX, menuY, menuWidth, menuHeight};
    }
}
