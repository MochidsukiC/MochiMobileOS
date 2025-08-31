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
    private static final float FRICTION = 0.9f;
    
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
            System.out.println("ControlCenterManager: Showing control center with " + items.size() + " items");
        }
    }
    
    /**
     * コントロールセンターを非表示にする。
     */
    public void hide() {
        if (isVisible) {
            isVisible = false;
            targetAnimationProgress = 0.0f;
            
            // スクロール状態をリセット（次回表示時に先頭から表示される）
            scrollOffset = 0.0f;
            scrollVelocity = 0.0f;
            isDragScrolling = false;
            lastDragY = 0;
            
            System.out.println("ControlCenterManager: Hiding control center");
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
     * ジェスチャーイベントを処理する。
     * 
     * @param event ジェスチャーイベント
     * @return イベントを処理した場合true
     */
    public boolean onGesture(GestureEvent event) {
        // 非表示の場合は処理しない
        if (animationProgress <= 0.1f) {
            return false;
        }
        
        float panelHeight = screenHeight * CONTROL_CENTER_HEIGHT_RATIO;
        float panelY = screenHeight - panelHeight * animationProgress;
        
        // コントロールセンターパネル外のタップで閉じる
        // （現在は全画面表示のため、上部のヘッダー領域外でのタップで判定）
        float headerHeight = 70; // ヘッダー部分の高さ
        if (event.getType() == jp.moyashi.phoneos.core.input.GestureType.TAP && 
            event.getCurrentY() < panelY + headerHeight) {
            System.out.println("ControlCenterManager: Tapped outside panel content area, hiding control center");
            hide();
            return true;
        }
        
        // アイテム領域内の座標を計算（ヘッダー分の余白を確保）
        float startY = panelY + 70;
        float availableHeight = panelHeight - 90;
        
        // アイテム領域内でのスクロール処理
        if (event.getCurrentY() >= startY && event.getCurrentY() <= startY + availableHeight) {
            // ドラッグ開始
            if (event.getType() == jp.moyashi.phoneos.core.input.GestureType.DRAG_START) {
                isDragScrolling = true;
                lastDragY = event.getCurrentY();
                scrollVelocity = 0; // 慣性を停止
                System.out.println("ControlCenterManager: Drag scroll started at Y=" + lastDragY);
                return true;
            }
            
            // ドラッグ中 - リアルタイムスクロール
            if (event.getType() == jp.moyashi.phoneos.core.input.GestureType.DRAG_MOVE && isDragScrolling) {
                float currentY = event.getCurrentY();
                float deltaY = currentY - lastDragY;
                
                // ドラッグの動きに応じてスクロールオフセットを更新
                // 下向きドラッグ（deltaY > 0）で上スクロール（コンテンツが下に移動）
                // 上向きドラッグ（deltaY < 0）で下スクロール（コンテンツが上に移動）
                scrollOffset -= deltaY * SCROLL_SENSITIVITY;
                
                // スクロール範囲を制限
                scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
                
                lastDragY = currentY;
                System.out.println("ControlCenterManager: Drag scrolling - deltaY=" + deltaY + ", scrollOffset=" + scrollOffset);
                return true;
            }
            
            // ドラッグ終了 - 慣性を設定
            if (event.getType() == jp.moyashi.phoneos.core.input.GestureType.DRAG_END && isDragScrolling) {
                isDragScrolling = false;
                
                // ドラッグの最終的な動きから慣性速度を計算
                float deltaY = event.getCurrentY() - event.getStartY();
                if (Math.abs(deltaY) > 20) { // 十分な移動距離がある場合
                    scrollVelocity = -deltaY * SCROLL_SENSITIVITY * 0.3f; // 慣性方向を設定
                }
                
                System.out.println("ControlCenterManager: Drag scroll ended, velocity=" + scrollVelocity);
                return true;
            }
            
            // 上向きスワイプで下にスクロール（コンテンツが上に移動）
            if (event.getType() == jp.moyashi.phoneos.core.input.GestureType.SWIPE_UP) {
                scrollVelocity = SCROLL_SENSITIVITY * 10; // 下向きスクロール
                System.out.println("ControlCenterManager: Swipe up - scroll down");
                return true;
            }
            
            // 下向きスワイプで上にスクロール（コンテンツが下に移動）
            // ただし、スクロール位置が0より大きい場合のみ（コンテンツが既にスクロールされている場合）
            if (event.getType() == jp.moyashi.phoneos.core.input.GestureType.SWIPE_DOWN) {
                if (scrollOffset > 0) {
                    scrollVelocity = -SCROLL_SENSITIVITY * 10; // 上向きスクロール
                    System.out.println("ControlCenterManager: Swipe down - scroll up");
                    return true;
                } else {
                    // スクロール位置が0の場合は、コントロールセンターを閉じる
                    System.out.println("ControlCenterManager: Swipe down at top, hiding control center");
                    hide();
                    return true;
                }
            }
        }
        
        // アイテム領域外での下向きスワイプでも閉じる
        if (event.getType() == jp.moyashi.phoneos.core.input.GestureType.SWIPE_DOWN) {
            System.out.println("ControlCenterManager: Swipe down detected, hiding control center");
            hide();
            return true;
        }
        
        // 左右スワイプをブロック（下のレイヤーに貫通させない）
        if (event.getType() == jp.moyashi.phoneos.core.input.GestureType.SWIPE_LEFT || 
            event.getType() == jp.moyashi.phoneos.core.input.GestureType.SWIPE_RIGHT) {
            System.out.println("ControlCenterManager: Left/Right swipe blocked (control center is visible)");
            return true; // イベントを消費して下のレイヤーに渡さない
        }
        
        // アイテムとのインタラクション処理
        float currentY = startY + ITEM_MARGIN - scrollOffset;
        
        for (IControlCenterItem item : items) {
            if (!item.isVisible() || !item.isEnabled()) {
                continue;
            }
            
            // 表示領域内のアイテムのみ処理
            if (currentY + ITEM_HEIGHT >= startY && currentY <= startY + availableHeight) {
                float itemX = ITEM_MARGIN;
                float itemWidth = screenWidth - 2 * ITEM_MARGIN;
                
                // ドラッグスクロール中でない場合のみアイテムの相互作用を処理
                if (!isDragScrolling && item.isInBounds(event.getCurrentX(), event.getCurrentY(), itemX, currentY, itemWidth, ITEM_HEIGHT)) {
                    try {
                        if (item.onGesture(event)) {
                            System.out.println("ControlCenterManager: Item '" + item.getId() + "' handled gesture");
                            return true;
                        }
                    } catch (Exception e) {
                        System.err.println("ControlCenterManager: Error in item gesture handling: " + e.getMessage());
                    }
                }
            }
            
            currentY += ITEM_HEIGHT + ITEM_MARGIN;
        }
        
        // コントロールセンターが表示されているときは、処理されなかったジェスチャーも
        // すべてブロックして下のレイヤーに貫通させない
        System.out.println("ControlCenterManager: Gesture blocked (control center is visible): " + event.getType());
        return true;
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
        this.dynamicPriority = priority;
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
        return this.isVisible && animationProgress > 0.1f;
    }
}