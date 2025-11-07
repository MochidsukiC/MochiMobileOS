package jp.moyashi.phoneos.core.input;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jp.moyashi.phoneos.core.service.LoggerService;

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

    /** ドラッグイベントスロットリング用（1000fps相当 = 1ms間隔） - 超高密度検知 */
    private static final long DRAG_EVENT_INTERVAL_NS = 1_000_000L; // 1ms
    private long lastDragEventTime = 0;

    /** レイテンシ計測しきい値（ログI/O負荷軽減のため高めに設定） */
    private static final long STAGE_INFO_THRESHOLD_NS = 10_000_000L; // 10ms
    private static final long STAGE_WARN_THRESHOLD_NS = 20_000_000L; // 20ms

    /** ロガー */
    private final LoggerService logger;
    
    /**
     * GestureManagerを作成する。
     */
    public GestureManager() {
        this(null);
    }

    public GestureManager(LoggerService logger) {
        this.logger = logger;
        this.listeners = new CopyOnWriteArrayList<>();
        this.isPressed = false;
        this.longPressDetected = false;
        this.isDragging = false;

        debug("Gesture system initialized");
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
            debug("Added gesture listener " + listener.getClass().getSimpleName() + " (priority: " + listener.getPriority() + ")");
            logListenerOrder("Current listener order");
        }
    }
    
    /**
     * ジェスチャーリスナーを削除する。
     * 
     * @param listener 削除するリスナー
     */
    public void removeGestureListener(GestureListener listener) {
        if (listeners.remove(listener)) {
            debug("Removed gesture listener " + listener.getClass().getSimpleName());
        }
    }
    
    /**
     * すべてのジェスチャーリスナーを削除する。
     */
    public void clearGestureListeners() {
        listeners.clear();
        debug("Cleared all gesture listeners");
    }
    
    /**
     * 既存のジェスチャーリスナーを優先度順に再ソートする。
     * 動的優先度変更時に呼び出される。
     */
    public void resortListeners() {
        listeners.sort(Comparator.comparingInt(GestureListener::getPriority).reversed());
        debug("Re-sorted gesture listeners by priority");
        logListenerOrder("Updated listener order");
    }
    
    /**
     * マウスプレスイベントを処理する。
     *
     * @param x X座標
     * @param y Y座標
     * @return イベントが処理された場合true
     */
    public boolean handleMousePressed(int x, int y) {
        long stageStart = System.nanoTime();

        startX = currentX = x;
        startY = currentY = y;
        startTime = System.currentTimeMillis();
        isPressed = true;
        longPressDetected = false;
        isDragging = false;

        logStage("mousePressed", "init", stageStart, System.nanoTime(), x, y);
        return false; // デフォルトでfalseを返す（ジェスチャーはまだ完了していない）
    }
    
    /**
     * マウスドラッグイベントを処理する。
     * 
     * @param x X座標
     * @param y Y座標
     */
    public void handleMouseDragged(int x, int y) {
        if (!isPressed) return;

        long stageStart = System.nanoTime();

        currentX = x;
        currentY = y;

        int dragDistance = (int) Math.sqrt(Math.pow(x - startX, 2) + Math.pow(y - startY, 2));
        // 過剰なログ出力を抑制（パフォーマンス改善）
        // debugVerbose("Mouse dragged to (" + x + ", " + y + "), distance: " + dragDistance);

        if (!isDragging && dragDistance >= DRAG_THRESHOLD) {
            // ドラッグ開始
            isDragging = true;
            longPressDetected = false; // 長押しをキャンセル

            GestureEvent dragStartEvent = new GestureEvent(GestureType.DRAG_START, startX, startY, x, y, startTime, System.currentTimeMillis());
            dispatchGestureEvent(dragStartEvent);
            lastDragEventTime = stageStart; // スロットリング用タイムスタンプ更新
        } else if (isDragging) {
            // ドラッグ中 - イベントスロットリング（60fps相当）
            // 前回のイベントから16ms以上経過した場合のみDRAG_MOVEイベントを発火
            if (stageStart - lastDragEventTime >= DRAG_EVENT_INTERVAL_NS) {
                GestureEvent dragMoveEvent = new GestureEvent(GestureType.DRAG_MOVE, startX, startY, x, y, startTime, System.currentTimeMillis());
                dispatchGestureEvent(dragMoveEvent);
                lastDragEventTime = stageStart;
            }
            // スロットリングによりイベントが間引かれても、座標は常に最新に保つ
        }

        logStage("mouseDragged", "dispatch", stageStart, System.nanoTime(), x, y);
    }
    
    /**
     * マウスリリースイベントを処理する。
     * 
     * @param x X座標
     * @param y Y座標
     */
    public void handleMouseReleased(int x, int y) {
        if (!isPressed) return;

        long stageStart = System.nanoTime();

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
            debug("Long press already detected, ignoring mouseReleased");
        }

        logStage("mouseReleased", "dispatch", stageStart, System.nanoTime(), x, y);

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
                debug("LONG_PRESS detected in update() at (" + startX + ", " + startY + ") after " + duration + "ms");
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
        long dispatchStart = System.nanoTime();
        int handledIndex = -1;

        for (int i = 0; i < listeners.size(); i++) {
            GestureListener listener = listeners.get(i);
            long listenerStart = System.nanoTime();
            try {
                if (listener.isInBounds(event.getCurrentX(), event.getCurrentY())) {
                    boolean handled = listener.onGesture(event);
                    long listenerEnd = System.nanoTime();
                    logStage("gesture", String.format("%s:%s", event.getType(), listener.getClass().getSimpleName()),
                            listenerStart, listenerEnd, event.getCurrentX(), event.getCurrentY());
                    if (handled) {
                        handledIndex = i;
                        break;
                    }
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("GestureManager", "Error in gesture listener " + listener.getClass().getSimpleName(), e);
                }
                e.printStackTrace();
            }
        }

        if (logger != null) {
            String msg = String.format("Dispatch gesture %s handledIndex=%d listeners=%d",
                    event.getType(), handledIndex, listeners.size());
            logger.debug("GestureManager", msg);
        }

        logStage("gesture", "dispatchTotal", dispatchStart, System.nanoTime(),
                event.getCurrentX(), event.getCurrentY());
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

    private void logStage(String event, String stage, long startNs, long endNs, int x, int y) {
        if (logger == null) {
            return;
        }
        long duration = endNs - startNs;
        if (duration <= STAGE_INFO_THRESHOLD_NS) {
            return;
        }
        double ms = duration / 1_000_000.0;
        String msg = String.format("%s %s latency=%.3fms coord=(%d,%d)", event, stage, ms, x, y);
        if (duration >= STAGE_WARN_THRESHOLD_NS) {
            logger.warn("GestureInput", msg);
        } else {
            logger.debug("GestureInput", msg);
        }
    }

    private void debug(String message) {
        if (logger != null) {
            logger.debug("GestureManager", message);
        }
    }

    private void debugVerbose(String message) {
        if (logger != null) {
            logger.debug("GestureManager", message);
        }
    }

    private void logListenerOrder(String header) {
        if (logger == null) {
            return;
        }
        List<String> order = new ArrayList<>(listeners.size());
        for (GestureListener l : listeners) {
            order.add(l.getClass().getSimpleName() + "(priority=" + l.getPriority() + ")");
        }
        logger.debug("GestureManager", header + ": " + String.join(" -> ", order));
    }
}
