package jp.moyashi.phoneos.core.input;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Kernelレベルでジェスチャーを管理するマネージャークラス。
 * マウス/タッチイベントを解釈してジェスチャーイベントに変換し、
 * 登録されたリスナーに配信する。
 */
public class GestureManager {
    
    /** ジェスチャーリスナーのリスト（優先度順にソート） */
    private final List<GestureListener> listeners;
    
    /** 現在のタッチ状態 */
    private boolean isPressed;
    private int startX, startY;
    private int currentX, currentY;
    private long startTime;
    
    /** ジェスチャー検出の設定値 */
    private static final long LONG_PRESS_THRESHOLD = 500; // 500ms
    private static final int DRAG_THRESHOLD = 10; // 10px
    private static final int SWIPE_THRESHOLD = 50; // 50px
    private static final float SWIPE_VELOCITY_THRESHOLD = 0.5f; // px/ms
    
    /** 長押し検出済みフラグ */
    private boolean longPressDetected;
    
    /** ドラッグ状態フラグ */
    private boolean isDragging;
    
    /**
     * GestureManagerを作成する。
     */
    public GestureManager() {
        this.listeners = new CopyOnWriteArrayList<>();
        this.isPressed = false;
        this.longPressDetected = false;
        this.isDragging = false;
        
        System.out.println("GestureManager: Kernel-level gesture system initialized");
    }
    
    /**
     * ジェスチャーリスナーを登録する。
     * 
     * @param listener 登録するリスナー
     */
    public void addGestureListener(GestureListener listener) {
        if (listener != null) {
            listeners.add(listener);
            // 優先度順にソート
            listeners.sort(Comparator.comparingInt(GestureListener::getPriority).reversed());
            System.out.println("GestureManager: Added gesture listener " + listener.getClass().getSimpleName() + " with priority " + listener.getPriority());
            
            // デバッグ: 現在の優先度順序を出力
            System.out.println("GestureManager: Current listener order:");
            for (int i = 0; i < listeners.size(); i++) {
                GestureListener l = listeners.get(i);
                System.out.println("  " + (i+1) + ". " + l.getClass().getSimpleName() + " (priority: " + l.getPriority() + ")");
            }
        }
    }
    
    /**
     * ジェスチャーリスナーを削除する。
     * 
     * @param listener 削除するリスナー
     */
    public void removeGestureListener(GestureListener listener) {
        if (listeners.remove(listener)) {
            System.out.println("GestureManager: Removed gesture listener");
        }
    }
    
    /**
     * すべてのジェスチャーリスナーを削除する。
     */
    public void clearGestureListeners() {
        listeners.clear();
        System.out.println("GestureManager: Cleared all gesture listeners");
    }
    
    /**
     * 既存のジェスチャーリスナーを優先度順に再ソートする。
     * 動的優先度変更時に呼び出される。
     */
    public void resortListeners() {
        listeners.sort(Comparator.comparingInt(GestureListener::getPriority).reversed());
        System.out.println("GestureManager: Re-sorted listeners by priority");
        
        // デバッグ: 現在の優先度順序を出力
        System.out.println("GestureManager: Updated listener order:");
        for (int i = 0; i < listeners.size(); i++) {
            GestureListener l = listeners.get(i);
            System.out.println("  " + (i+1) + ". " + l.getClass().getSimpleName() + " (priority: " + l.getPriority() + ")");
        }
    }
    
    /**
     * マウスプレスイベントを処理する。
     * 
     * @param x X座標
     * @param y Y座標
     */
    public void handleMousePressed(int x, int y) {
        System.out.println("GestureManager: Mouse pressed at (" + x + ", " + y + ")");
        
        startX = currentX = x;
        startY = currentY = y;
        startTime = System.currentTimeMillis();
        isPressed = true;
        longPressDetected = false;
        isDragging = false;
    }
    
    /**
     * マウスドラッグイベントを処理する。
     * 
     * @param x X座標
     * @param y Y座標
     */
    public void handleMouseDragged(int x, int y) {
        if (!isPressed) return;
        
        currentX = x;
        currentY = y;
        
        int dragDistance = (int) Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));
        
        System.out.println("GestureManager: Mouse dragged to (" + x + ", " + y + "), distance: " + dragDistance);
        
        if (!isDragging && dragDistance >= DRAG_THRESHOLD) {
            // ドラッグ開始
            isDragging = true;
            longPressDetected = false; // 長押しをキャンセル
            
            GestureEvent dragStartEvent = new GestureEvent(GestureType.DRAG_START, startX, startY, x, y, startTime, System.currentTimeMillis());
            dispatchGestureEvent(dragStartEvent);
        } else if (isDragging) {
            // ドラッグ中
            GestureEvent dragMoveEvent = new GestureEvent(GestureType.DRAG_MOVE, startX, startY, x, y, startTime, System.currentTimeMillis());
            dispatchGestureEvent(dragMoveEvent);
        }
    }
    
    /**
     * マウスリリースイベントを処理する。
     * 
     * @param x X座標
     * @param y Y座標
     */
    public void handleMouseReleased(int x, int y) {
        if (!isPressed) return;
        
        System.out.println("GestureManager: Mouse released at (" + x + ", " + y + ")");
        
        currentX = x;
        currentY = y;
        long currentTime = System.currentTimeMillis();
        long duration = currentTime - startTime;
        
        if (isDragging) {
            // ドラッグ終了
            GestureEvent dragEndEvent = new GestureEvent(GestureType.DRAG_END, startX, startY, x, y, startTime, currentTime);
            dispatchGestureEvent(dragEndEvent);
            
            // スワイプ判定
            checkForSwipe(x, y, currentTime);
        } else if (!longPressDetected) {
            if (duration >= LONG_PRESS_THRESHOLD) {
                // 長押し（まだ検出されていない場合）
                longPressDetected = true; // 重要：フラグを設定
                GestureEvent longPressEvent = new GestureEvent(GestureType.LONG_PRESS, startX, startY, x, y, startTime, currentTime);
                dispatchGestureEvent(longPressEvent);
            } else {
                // 短いタップ
                GestureEvent tapEvent = new GestureEvent(GestureType.TAP, x, y);
                dispatchGestureEvent(tapEvent);
            }
        } else {
            // 長押し検出済みの場合、追加のイベントは発生させない
            System.out.println("GestureManager: Long press already detected, ignoring mouseReleased");
        }
        
        // 状態リセット
        isPressed = false;
        isDragging = false;
        longPressDetected = false;
    }
    
    /**
     * 定期的に呼ばれる更新処理。
     * 長押し検出などに使用される。
     */
    public void update() {
        if (isPressed && !longPressDetected && !isDragging) {
            long currentTime = System.currentTimeMillis();
            long duration = currentTime - startTime;
            
            if (duration >= LONG_PRESS_THRESHOLD) {
                // 長押し検出
                longPressDetected = true;
                GestureEvent longPressEvent = new GestureEvent(GestureType.LONG_PRESS, startX, startY, currentX, currentY, startTime, currentTime);
                dispatchGestureEvent(longPressEvent);
                System.out.println("GestureManager: ✅ LONG_PRESS detected in update() at (" + startX + ", " + startY + ") after " + duration + "ms");
            }
        }
    }
    
    /**
     * スワイプジェスチャーをチェックする。
     * 
     * @param endX 終了X座標
     * @param endY 終了Y座標
     * @param endTime 終了時刻
     */
    private void checkForSwipe(int endX, int endY, long endTime) {
        int deltaX = endX - startX;
        int deltaY = endY - startY;
        int distance = (int) Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        long duration = endTime - startTime;
        float velocity = duration > 0 ? (float) distance / duration : 0f;
        
        if (distance >= SWIPE_THRESHOLD && velocity >= SWIPE_VELOCITY_THRESHOLD) {
            GestureType swipeType;
            
            if (Math.abs(deltaX) > Math.abs(deltaY)) {
                // 水平スワイプ
                swipeType = deltaX > 0 ? GestureType.SWIPE_RIGHT : GestureType.SWIPE_LEFT;
            } else {
                // 垂直スワイプ
                swipeType = deltaY > 0 ? GestureType.SWIPE_DOWN : GestureType.SWIPE_UP;
            }
            
            GestureEvent swipeEvent = new GestureEvent(swipeType, startX, startY, endX, endY, startTime, endTime);
            dispatchGestureEvent(swipeEvent);
        }
    }
    
    /**
     * ジェスチャーイベントを登録されたリスナーに配信する。
     * 
     * @param event 配信するイベント
     */
    private void dispatchGestureEvent(GestureEvent event) {
        System.out.println("GestureManager: Dispatching gesture: " + event + " to " + listeners.size() + " listeners");
        
        for (int i = 0; i < listeners.size(); i++) {
            GestureListener listener = listeners.get(i);
            try {
                System.out.println("GestureManager: Checking listener " + (i+1) + ": " + listener.getClass().getSimpleName() + " (priority: " + listener.getPriority() + ")");
                
                if (listener.isInBounds(event.getCurrentX(), event.getCurrentY())) {
                    System.out.println("GestureManager: Listener " + listener.getClass().getSimpleName() + " is in bounds, calling onGesture");
                    if (listener.onGesture(event)) {
                        System.out.println("GestureManager: Gesture handled by " + listener.getClass().getSimpleName() + " - stopping dispatch");
                        return; // イベントが処理された
                    } else {
                        System.out.println("GestureManager: Listener " + listener.getClass().getSimpleName() + " did not handle gesture, continuing");
                    }
                } else {
                    System.out.println("GestureManager: Listener " + listener.getClass().getSimpleName() + " is not in bounds, skipping");
                }
            } catch (Exception e) {
                System.err.println("GestureManager: Error in gesture listener " + listener.getClass().getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("GestureManager: Gesture not handled by any listener");
    }
    
    /**
     * 現在のタッチ状態を取得する（デバッグ用）。
     * 
     * @return タッチ状態の文字列
     */
    public String getDebugState() {
        if (!isPressed) {
            return "GestureManager: Not pressed";
        }
        
        long currentTime = System.currentTimeMillis();
        long duration = currentTime - startTime;
        int distance = (int) Math.sqrt(Math.pow(currentX - startX, 2) + Math.pow(currentY - startY, 2));
        
        return String.format("GestureManager: Pressed at (%d,%d), current (%d,%d), duration=%dms, distance=%dpx, longPress=%s, dragging=%s", 
                           startX, startY, currentX, currentY, duration, distance, longPressDetected, isDragging);
    }
}