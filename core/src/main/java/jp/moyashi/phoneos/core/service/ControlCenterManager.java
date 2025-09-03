package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.controls.IControlCenterItem;
import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureListener;
import processing.core.PApplet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * コントロールセンターの状態（表示/非表示、アイテム一覧）を一元管理するサービス。
 * 画面下からスライドイン/アウトするアニメーション付きでコントロールセンターを表示し、
 * 登録されたアイテムの描画とイベント処理を担当する。
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class ControlCenterManager implements GestureListener {
    
    /** コントロールセンターアイテムのリスト（スレッドセーフ） */
    private final List<IControlCenterItem> items;
    
    /** 現在の表示状態 */
    private boolean isVisible;
    
    /** アニメーション進行度（0.0 = 非表示, 1.0 = 完全表示） */
    private float animationProgress;
    
    /** アニメーションの目標進行度 */
    private float targetAnimationProgress;
    
    /** アニメーション速度 */
    private static final float ANIMATION_SPEED = 0.12f;
    
    /** コントロールセンターの高さ（画面の何割を占めるか） */
    private static final float CONTROL_CENTER_HEIGHT_RATIO = 1.0f;
    
    /** アイテムの高さ */
    private static final float ITEM_HEIGHT = 60;
    
    /** アイテム間のマージン */
    private static final float ITEM_MARGIN = 8;
    
    /** 画面の幅（描画時に取得） */
    private float screenWidth = 400;
    
    /** 画面の高さ（描画時に取得） */
    private float screenHeight = 600;
    
    /** 背景のアルファ値 */
    private static final int BACKGROUND_ALPHA = 220;
    
    /** スクロールオフセット（縦方向） */
    private float scrollOffset = 0.0f;
    
    /** スクロール可能な最大オフセット */
    private float maxScrollOffset = 0.0f;
    
    /** スクロールの慣性 */
    private float scrollVelocity = 0.0f;
    
    /** 摩擦係数（スクロールの減速） */
    private static final float FRICTION = 0.85f;
    
    /** スクロール感度 */
    private static final float SCROLL_SENSITIVITY = 1.5f;
    
    /** 最小スクロール速度（これより小さければ停止） */
    private static final float MIN_SCROLL_VELOCITY = 0.1f;
    
    /** 前回のドラッグY座標（スクロール計算用） */
    private float lastDragY = 0;
    
    /** ドラッグが開始されているかどうか */
    private boolean isDragScrolling = false;
    
    /** 動的優先度（表示状態に応じて変更される） */
    private int dynamicPriority = 0;
    
    /** ジェスチャーマネージャーへの参照（優先度変更時の再ソート用） */
    private jp.moyashi.phoneos.core.input.GestureManager gestureManager;
    
    /**
     * ControlCenterManagerを作成する。
     */
    public ControlCenterManager() {
        this.items = new CopyOnWriteArrayList<>();
        this.isVisible = false;
        this.animationProgress = 0.0f;
        this.targetAnimationProgress = 0.0f;
        
        System.out.println("ControlCenterManager: Control center service initialized");
    }
    
    /**
     * コントロールセンターを表示する。
     */
    public void show() {
        if (!isVisible) {
            isVisible = true;
            targetAnimationProgress = 1.0f;
            
            // コントロールセンターが表示される時は最高優先度に設定
            setDynamicPriority(15000); // ロック画面(8000)より高い優先度
            
            System.out.println("ControlCenterManager: Showing control center with " + items.size() + " items");
            System.out.println("ControlCenterManager: Set priority to 15000 (highest)");
        }
    }
    
    /**
     * コントロールセンターを非表示にする。
     */
    public void hide() {
        if (isVisible) {
            isVisible = false;
            targetAnimationProgress = 0.0f;
            
            // コントロールセンターが非表示になる時は低い優先度に設定
            setDynamicPriority(0); // 低い優先度に戻す
            
            // スクロール状態をリセット（次回表示時に先頭から表示される）
            scrollOffset = 0.0f;
            scrollVelocity = 0.0f;
            isDragScrolling = false;
            lastDragY = 0;
            
            System.out.println("ControlCenterManager: Hiding control center");
            System.out.println("ControlCenterManager: Set priority to 0 (low)");
        }
    }
    
    /**
     * 表示状態を切り替える。
     */
    public void toggle() {
        if (isVisible) {
            hide();
        } else {
            show();
        }
    }
    
    /**
     * コントロールセンターアイテムを追加する。
     * 
     * @param item 追加するアイテム
     * @return 追加に成功した場合true
     */
    public boolean addItem(IControlCenterItem item) {
        if (item == null) {
            System.err.println("ControlCenterManager: Cannot add null item");
            return false;
        }
        
        // 重複IDチェック
        for (IControlCenterItem existingItem : items) {
            if (existingItem.getId().equals(item.getId())) {
                System.err.println("ControlCenterManager: Item with ID '" + item.getId() + "' already exists");
                return false;
            }
        }
        
        items.add(item);
        System.out.println("ControlCenterManager: Added item '" + item.getDisplayName() + "' (ID: " + item.getId() + ")");
        return true;
    }
    
    /**
     * コントロールセンターアイテムを削除する。
     * 
     * @param itemId 削除するアイテムのID
     * @return 削除に成功した場合true
     */
    public boolean removeItem(String itemId) {
        return items.removeIf(item -> {
            if (item.getId().equals(itemId)) {
                System.out.println("ControlCenterManager: Removed item '" + item.getDisplayName() + "' (ID: " + itemId + ")");
                return true;
            }
            return false;
        });
    }
    
    /**
     * 指定されたIDのアイテムを取得する。
     * 
     * @param itemId アイテムID
     * @return 見つかった場合はアイテム、見つからない場合はnull
     */
    public IControlCenterItem getItem(String itemId) {
        return items.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * すべてのアイテムを削除する。
     */
    public void clearItems() {
        int count = items.size();
        items.clear();
        System.out.println("ControlCenterManager: Cleared " + count + " items");
    }
    
    /**
     * 登録されているアイテムの数を取得する。
     * 
     * @return アイテム数
     */
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * コントロールセンターが表示中かどうかを確認する。
     * 
     * @return 表示中の場合true
     */
    public boolean isVisible() {
        return this.isVisible;
    }
    
    /**
     * コントロールセンターを描画する。
     * 
     * @param p Processing描画コンテキスト
     */
    public void draw(PApplet p) {
        // 画面サイズを更新
        screenWidth = p.width;
        screenHeight = p.height;
        
        // アニメーション進行度を更新
        updateAnimation();
        
        // スクロール慣性を更新
        updateScrollPhysics();
        
        // デバッグ情報出力（最初の数フレームのみ）
        if (isVisible && p.frameCount % 60 == 0) {
            System.out.println("ControlCenter: Drawing - visible=" + isVisible + ", progress=" + animationProgress + ", items=" + items.size());
        }
        
        // 完全に非表示の場合は描画をスキップ
        if (animationProgress <= 0.01f) {
            return;
        }
        
        p.pushMatrix();
        p.pushStyle();
        
        try {
            // 背景オーバーレイ描画
            drawBackgroundOverlay(p);
            
            // コントロールセンターパネル描画
            drawControlPanel(p);
            
        } finally {
            p.popStyle();
            p.popMatrix();
        }
    }
    
    /**
     * 背景オーバーレイを描画する（画面全体を暗くする効果）。
     */
    private void drawBackgroundOverlay(PApplet p) {
        int alpha = (int) (100 * animationProgress);
        p.fill(0, 0, 0, alpha);
        p.noStroke();
        p.rect(0, 0, screenWidth, screenHeight);
    }
    
    /**
     * コントロールセンターパネルを描画する。
     */
    private void drawControlPanel(PApplet p) {
        float panelHeight = screenHeight * CONTROL_CENTER_HEIGHT_RATIO;
        float panelY = screenHeight - panelHeight * animationProgress;
        
        // クリッピングマスクを設定（下のレイヤーに影響しないように）
        // まず、パネルの背景をクリップ領域として設定
        p.pushMatrix();
        p.pushStyle();
        
        try {
            // パネル背景
            int backgroundAlpha = (int) (BACKGROUND_ALPHA * animationProgress);
            p.fill(40, 40, 45, backgroundAlpha);
            p.noStroke();
            p.rect(0, panelY, screenWidth, panelHeight, 20, 20, 0, 0);
            
            // パネル上部の取っ手
            drawHandle(p, panelY);
            
            // ヘッダーテキスト
            drawHeader(p, panelY);
            
            // アイテム描画領域をクリップ（上部ヘッダー分の余白を確保）
            drawItemsWithClipping(p, panelY + 70, panelHeight - 90);
            
        } finally {
            p.popStyle();
            p.popMatrix();
        }
    }
    
    /**
     * パネル上部の取っ手を描画する。
     */
    private void drawHandle(PApplet p, float panelY) {
        float handleWidth = 40;
        float handleHeight = 4;
        float handleX = (screenWidth - handleWidth) / 2;
        float handleY = panelY + 10;
        
        int handleAlpha = (int) (150 * animationProgress);
        p.fill(255, 255, 255, handleAlpha);
        p.noStroke();
        p.rect(handleX, handleY, handleWidth, handleHeight, handleHeight / 2);
    }
    
    /**
     * パネルヘッダーテキストを描画する。
     */
    private void drawHeader(PApplet p, float panelY) {
        int textAlpha = (int) (255 * animationProgress);
        p.fill(255, 255, 255, textAlpha);
        p.textAlign(p.CENTER, p.TOP);
        p.textSize(16);
        p.text("コントロールセンター", screenWidth / 2, panelY + 25);
        
        // 使い方のヒント（小さいテキスト）
        p.fill(200, 200, 200, textAlpha);
        p.textSize(10);
        p.text("上をタップまたは下スワイプで閉じる", screenWidth / 2, panelY + 45);
    }
    
    /**
     * コントロールセンターアイテムをクリッピング付きで描画する。
     */
    private void drawItemsWithClipping(PApplet p, float startY, float availableHeight) {
        if (items.isEmpty()) {
            // アイテムがない場合のメッセージ
            drawEmptyMessage(p, startY, availableHeight);
            return;
        }
        
        // スクロール可能な全体の高さを計算
        float totalContentHeight = 0;
        int visibleItemCount = 0;
        for (IControlCenterItem item : items) {
            if (item.isVisible()) {
                visibleItemCount++;
            }
        }
        
        if (visibleItemCount > 0) {
            totalContentHeight = visibleItemCount * (ITEM_HEIGHT + ITEM_MARGIN) + ITEM_MARGIN;
        }
        
        // 最大スクロールオフセットを更新
        maxScrollOffset = Math.max(0, totalContentHeight - availableHeight);
        
        // スクロールオフセットを制限
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
        
        // クリッピング領域を設定（下のレイヤーに影響しないように）
        // Processing の clip() 機能を使用して描画領域を制限
        p.pushMatrix();
        p.pushStyle();
        
        try {
            // アイテム描画領域をクリップ（範囲外への描画を防止）
            // ProcessingではclipRect()は使えないため、座標チェックで代替
            
            // 描画開始位置をスクロールオフセットに応じて調整
            float currentY = startY + ITEM_MARGIN - scrollOffset;
            int itemIndex = 0;
            
            for (IControlCenterItem item : items) {
                if (!item.isVisible()) {
                    continue;
                }
                
                // 表示領域内かどうかを厳密にチェック（下のレイヤーに影響しないように）
                if (currentY + ITEM_HEIGHT >= startY && currentY < startY + availableHeight) {
                    // アイテム描画
                    float itemX = ITEM_MARGIN;
                    float itemWidth = screenWidth - 2 * ITEM_MARGIN;
                    
                    // アイテムが表示領域内にある場合のみ描画
                    if (currentY >= startY - ITEM_HEIGHT && currentY <= startY + availableHeight + ITEM_HEIGHT) {
                        try {
                            // 下のレイヤーに影響しないよう、描画領域をさらに制限
                            p.pushStyle();
                            
                            // アイテムの描画位置が適切な範囲内にある場合のみ描画
                            if (currentY >= startY && currentY + ITEM_HEIGHT <= startY + availableHeight) {
                                item.draw(p, itemX, currentY, itemWidth, ITEM_HEIGHT);
                            } else if (currentY < startY && currentY + ITEM_HEIGHT > startY) {
                                // 上端で部分的に切れている場合
                                float visibleHeight = currentY + ITEM_HEIGHT - startY;
                                if (visibleHeight > 0) {
                                    item.draw(p, itemX, currentY, itemWidth, ITEM_HEIGHT);
                                }
                            } else if (currentY < startY + availableHeight && currentY + ITEM_HEIGHT > startY + availableHeight) {
                                // 下端で部分的に切れている場合
                                float visibleHeight = startY + availableHeight - currentY;
                                if (visibleHeight > 0) {
                                    item.draw(p, itemX, currentY, itemWidth, ITEM_HEIGHT);
                                }
                            }
                            
                            p.popStyle();
                        } catch (Exception e) {
                            System.err.println("ControlCenterManager: Error drawing item '" + item.getId() + "': " + e.getMessage());
                            
                            // エラー時のフォールバック描画（表示領域内のみ）
                            if (currentY >= startY && currentY + ITEM_HEIGHT <= startY + availableHeight) {
                                drawErrorItem(p, itemX, currentY, itemWidth, ITEM_HEIGHT, item.getDisplayName());
                            }
                        }
                    }
                }
                
                currentY += ITEM_HEIGHT + ITEM_MARGIN;
                itemIndex++;
            }
            
            // スクロールバーを描画（スクロール可能な場合のみ）
            if (maxScrollOffset > 0) {
                drawScrollbar(p, startY, availableHeight);
            }
            
        } finally {
            p.popStyle();
            p.popMatrix();
        }
    }
    
    /**
     * アイテムがない場合のメッセージを描画する。
     */
    private void drawEmptyMessage(PApplet p, float startY, float availableHeight) {
        p.fill(150, 150, 150, (int) (255 * animationProgress));
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(16);
        p.text("コントロールセンターにアイテムがありません", screenWidth / 2, startY + availableHeight / 2);
    }
    
    /**
     * エラー時のフォールバック描画を行う。
     */
    private void drawErrorItem(PApplet p, float x, float y, float w, float h, String itemName) {
        p.fill(100, 50, 50, 150);
        p.noStroke();
        p.rect(x, y, w, h, 8);
        
        p.fill(255, 100, 100);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.textSize(12);
        p.text("Error: " + itemName, x + 10, y + h / 2);
    }
    
    /**
     * スクロールバーを描画する。
     */
    private void drawScrollbar(PApplet p, float startY, float availableHeight) {
        if (maxScrollOffset <= 0) {
            return;
        }
        
        // スクロールバーの位置とサイズを計算
        float scrollbarWidth = 4;
        float scrollbarX = screenWidth - scrollbarWidth - 2;
        
        float scrollbarHeight = (availableHeight / (maxScrollOffset + availableHeight)) * availableHeight;
        scrollbarHeight = Math.max(20, scrollbarHeight); // 最小サイズを設定
        
        float scrollbarY = startY + (scrollOffset / maxScrollOffset) * (availableHeight - scrollbarHeight);
        
        // スクロールバーを描画
        int scrollbarAlpha = (int) (100 * animationProgress);
        p.fill(255, 255, 255, scrollbarAlpha);
        p.noStroke();
        p.rect(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, scrollbarWidth / 2);
    }
    
    /**
     * スクロールの慣性物理を更新する。
     */
    private void updateScrollPhysics() {
        // 慣性によるスクロール
        if (Math.abs(scrollVelocity) > MIN_SCROLL_VELOCITY) {
            scrollOffset += scrollVelocity;
            scrollVelocity *= FRICTION;
        } else {
            scrollVelocity = 0;
        }
        
        // スクロール範囲を制限
        if (scrollOffset < 0) {
            scrollOffset = 0;
            scrollVelocity = 0;
        } else if (scrollOffset > maxScrollOffset) {
            scrollOffset = maxScrollOffset;
            scrollVelocity = 0;
        }
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
    
    
    /**
     * 現在のアニメーション進行度を取得する（デバッグ用）。
     * 
     * @return アニメーション進行度（0.0-1.0）
     */
    public float getAnimationProgress() {
        return animationProgress;
    }
    
    /**
     * アイテム一覧のコピーを取得する。
     * 
     * @return アイテム一覧（読み取り専用）
     */
    public List<IControlCenterItem> getItems() {
        return new ArrayList<>(items);
    }
    
    // GestureListener interface implementation
    
    /**
     * ジェスチャーの優先度を返す。
     * 動的に設定された優先度を返す。
     * 
     * @return 動的優先度
     */
    @Override
    public int getPriority() {
        return dynamicPriority;
    }
    
    /**
     * 動的優先度を設定する。
     * 
     * @param priority 設定する優先度
     */
    public void setDynamicPriority(int priority) {
        int oldPriority = this.dynamicPriority;
        this.dynamicPriority = priority;
        
        // 優先度が変更された場合、ジェスチャーマネージャーにリスナーの再ソートを要求
        if (oldPriority != priority && gestureManager != null) {
            gestureManager.removeGestureListener(this);
            gestureManager.addGestureListener(this);
            System.out.println("ControlCenterManager: Priority changed from " + oldPriority + " to " + priority + ", re-sorted gesture listeners");
        }
    }
    
    /**
     * ジェスチャーマネージャーの参照を設定する。
     * 
     * @param gestureManager ジェスチャーマネージャー
     */
    public void setGestureManager(jp.moyashi.phoneos.core.input.GestureManager gestureManager) {
        this.gestureManager = gestureManager;
    }
    
    /**
     * ジェスチャーイベントを処理する。
     * コントロールセンターが表示中の場合、すべてのジェスチャーを受け取り、
     * 適切なアクション（項目選択、スクロール、非表示）を実行する。
     * 
     * @param event ジェスチャーイベント
     * @return イベントを処理した場合true
     */
    @Override
    public boolean onGesture(GestureEvent event) {
        // コントロールセンターが表示されていない場合は処理しない
        if (!isVisible || animationProgress <= 0.1f) {
            return false;
        }
        
        System.out.println("ControlCenterManager: Processing gesture - " + event.getType() + " at (" + 
                          event.getCurrentX() + ", " + event.getCurrentY() + ")");
        
        // ジェスチャータイプに応じた処理
        switch (event.getType()) {
            case SWIPE_DOWN:
                // 下向きスワイプでコントロールセンターを非表示
                hide();
                return true; // イベントを消費
                
            case TAP:
                // クリックイベント処理（項目選択など）
                handleControlCenterClick(event.getCurrentX(), event.getCurrentY());
                return true; // イベントを消費
                
            case DRAG_MOVE:
                // ドラッグによるスクロール処理
                handleControlCenterScroll(event);
                return true; // イベントを消費
                
            case DRAG_END:
                // ドラッグ終了時にフラグをリセット
                isDragScrolling = false;
                System.out.println("ControlCenterManager: Drag ended, resetting scroll state");
                return true; // イベントを消費
                
            case SWIPE_UP:
            case SWIPE_LEFT:
            case SWIPE_RIGHT:
                // その他のスワイプも消費（下位レイヤーに渡さない）
                return true;
                
            default:
                // その他のジェスチャーも消費
                return true;
        }
    }
    
    /**
     * コントロールセンター内のクリックを処理する。
     * 
     * @param x クリック座標X
     * @param y クリック座標Y
     */
    private void handleControlCenterClick(int x, int y) {
        // 項目領域内のクリック判定とアクション実行
        // パネルの実際の位置を計算（描画ロジックと一致させる）
        float panelHeight = screenHeight * CONTROL_CENTER_HEIGHT_RATIO;
        float panelY = screenHeight - panelHeight * animationProgress;
        float startY = panelY + 70; // ヘッダー分の70pxオフセットを追加
        float itemY = startY + ITEM_MARGIN - scrollOffset;
        
        System.out.println("ControlCenterManager: Click at (" + x + ", " + y + ")");
        System.out.println("ControlCenterManager: startY=" + startY + ", scrollOffset=" + scrollOffset + ", itemY=" + itemY);
        
        for (IControlCenterItem item : items) {
            // 描画と同じように非表示アイテムをスキップ
            if (!item.isVisible()) {
                System.out.println("ControlCenterManager: Skipping invisible item '" + item.getDisplayName() + "'");
                continue;
            }
            
            System.out.println("ControlCenterManager: Checking item '" + item.getDisplayName() + "' at Y range [" + itemY + " - " + (itemY + ITEM_HEIGHT) + "]");
            if (y >= itemY && y <= itemY + ITEM_HEIGHT) {
                System.out.println("ControlCenterManager: Item clicked - " + item.getDisplayName());
                // アイテムのジェスチャー処理を呼び出す
                GestureEvent tapEvent = new GestureEvent(jp.moyashi.phoneos.core.input.GestureType.TAP, x, y, x, y, System.currentTimeMillis(), System.currentTimeMillis());
                item.onGesture(tapEvent);
                return;
            }
            itemY += ITEM_HEIGHT + ITEM_MARGIN;
        }
    }
    
    /**
     * コントロールセンターのスクロールを処理する。
     * 
     * @param event ドラッグイベント
     */
    private void handleControlCenterScroll(GestureEvent event) {
        if (!isDragScrolling) {
            isDragScrolling = true;
            lastDragY = event.getCurrentY();
            scrollVelocity = 0;
        } else {
            float deltaY = event.getCurrentY() - lastDragY;
            scrollOffset -= deltaY * SCROLL_SENSITIVITY;
            
            // スクロール範囲の制限
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScrollOffset) scrollOffset = maxScrollOffset;
            
            scrollVelocity = -deltaY * SCROLL_SENSITIVITY;
            lastDragY = event.getCurrentY();
        }
    }
    
    /**
     * 指定された座標がコントロールセンターの範囲内かどうかを確認する。
     * 
     * @param x X座標
     * @param y Y座標
     * @return コントロールセンターが表示中で範囲内の場合true
     */
    @Override
    public boolean isInBounds(int x, int y) {
        // コントロールセンターが表示中の場合、画面全体をカバー
        boolean inBounds = this.isVisible && animationProgress > 0.1f;
        System.out.println("ControlCenterManager.isInBounds(" + x + ", " + y + ") = " + inBounds + " (visible=" + isVisible + ", animProgress=" + animationProgress + ", priority=" + getPriority() + ")");
        return inBounds;
    }
}