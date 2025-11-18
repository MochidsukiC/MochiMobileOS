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
    
    /** ジェスチャーリスナーのリスト（優先度順にソート）*/
    private final List<GestureListener> listeners;
    
    /** 現在のタッチ状態*/
    private boolean isPressed;
    private int startX, startY;
    private int currentX, currentY;
    private long startTime;
    
    /** ジェスチャー検出の設定値 */
    private static final long LONG_PRESS_THRESHOLD = 500; // 500ms
    private static final int DRAG_THRESHOLD = 10; // 10px
    private static final int SWIPE_THRESHOLD = 50; // 50px
    private static final float SWIPE_VELOCITY_THRESHOLD = 0.5f; // px/ms
    /** L-2修正: スワイプ角度判定の定数化 */
    private static final double SWIPE_HORIZONTAL_ANGLE_MAX = 30.0; // 水平方向の最大角度（度）
    private static final double SWIPE_VERTICAL_ANGLE_MIN = 60.0; // 垂直方向の最小角度（度）
    
    /** 長押し検出済みフラグ */
    private boolean longPressDetected;
    
    /** ドラッグ状態フラグ */
    private boolean isDragging;
    private int lastDispatchedX;
    private int lastDispatchedY;

    /** ドラッグイベントスロットリング用（120fps相当 = 約8ms間隔） */
    // dispatching at 1000fps was saturating the UI thread, so we cap it closer to display refresh
    private static final long DRAG_EVENT_INTERVAL_NS = 8_000_000L; // 8ms
    private static final int DRAG_EVENT_MIN_DELTA = 2; // px
    private long lastDragEventTime = 0;

    /** レイテンシ計測しきい値。L-1修正: ログI/O負荷軽減のため高めに設定 */
    private static final long STAGE_INFO_THRESHOLD_NS = 20_000_000L; // 20ms
    private static final long STAGE_WARN_THRESHOLD_NS = 50_000_000L; // 50ms

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
        this.lastDispatchedX = 0;
        this.lastDispatchedY = 0;

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
        lastDispatchedX = x;
        lastDispatchedY = y;

        logStage("mousePressed", "init", stageStart, System.nanoTime(), x, y);
        return false; // デフォルトでfalseを返す。ジェスチャーはまだ完了していないため
    }
    
    /**
     * マウスドラッグイベントを処理するため。
     *
     * @param x X座標
     * @param y Y座標
     */
    public void handleMouseDragged(int x, int y) {
        if (!isPressed) return;

        long stageStart = System.nanoTime();

        currentX = x;
        currentY = y;

        int dragDistance = calculateDistance(startX, startY, x, y);

        // H-1修正補完: update()が呼ばれない環境のため、ここでも長押しをチェック
        // ただし、既にドラッグ判定距離を超えている場合は長押しとしない
        if (!longPressDetected && !isDragging && dragDistance < DRAG_THRESHOLD) {
            long currentTime = System.currentTimeMillis();
            long duration = currentTime - startTime;

            if (duration >= LONG_PRESS_THRESHOLD) {
                longPressDetected = true;
                GestureEvent longPressEvent = new GestureEvent(GestureType.LONG_PRESS, startX, startY, currentX, currentY, startTime, currentTime);
                dispatchGestureEvent(longPressEvent);
                debug("LONG_PRESS detected in handleMouseDragged() at (" + startX + ", " + startY + ") after " + duration + "ms");
                return; // 長押し検出後はドラッグ開始をスキップ
            }
        }
        // 過剰なログ出力を抑制し、パフォーマンス改善のため

        if (!isDragging && dragDistance >= DRAG_THRESHOLD) {
            // 長押しで編集モードに入った直後のドラッグも許可する（アイコン移動などに必要）
            // ドラッグ開始
            isDragging = true;

            GestureEvent dragStartEvent = new GestureEvent(GestureType.DRAG_START, startX, startY, x, y, startTime, System.currentTimeMillis());
            dispatchGestureEvent(dragStartEvent);
            lastDragEventTime = stageStart; // スロットリング用タイムスタンプ更新
            lastDispatchedX = x;
            lastDispatchedY = y;
        } else if (isDragging) {
            // ドラッグ中 - イベントスロットリング。1000fps相当
            // 前回のイベントから1ms以上経過した場合のみDRAG_MOVEイベントを発火
            int deltaSinceLast = calculateDistance(lastDispatchedX, lastDispatchedY, x, y);
            if ((stageStart - lastDragEventTime >= DRAG_EVENT_INTERVAL_NS) || deltaSinceLast >= DRAG_EVENT_MIN_DELTA) {
                GestureEvent dragMoveEvent = new GestureEvent(GestureType.DRAG_MOVE, startX, startY, x, y, startTime, System.currentTimeMillis());
                dispatchGestureEvent(dragMoveEvent);
                lastDragEventTime = stageStart;
                lastDispatchedX = x;
                lastDispatchedY = y;
            }
            // スロットリングによりイベントが間引かれても、座標は常に最新に保つ
        }

        // omit detailed stage logging for performance;
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
        } else if (!longPressDetected) {
            // H-1修正: 長押しはupdate()メソッドでのみ検出する
            // リリース時の長押し判定は削除し、TAPのみを処理
            // 短いタッチ
            GestureEvent tapEvent = new GestureEvent(GestureType.TAP, x, y);
            dispatchGestureEvent(tapEvent);
        } else {
            debug("Long press already detected, ignoring mouseReleased");
        }

        if (!longPressDetected) {
            checkForSwipe(x, y, currentTime);
        }

        logStage("mouseReleased", "dispatch", stageStart, System.nanoTime(), x, y);

        // 状態リセット
        isPressed = false;
        isDragging = false;
        longPressDetected = false;
        lastDispatchedX = x;
        lastDispatchedY = y;
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
     * M-4修正: 角度による方向判定を追加し、対角線方向のスワイプは検出しない
     *
     * @param endX 終了座標
     * @param endY 終了座標
     * @param endTime 終了時刻
     */
    private boolean checkForSwipe(int endX, int endY, long endTime) {
        int deltaX = endX - startX;
        int deltaY = endY - startY;
        int distance = calculateDistance(startX, startY, endX, endY);
        long duration = endTime - startTime;
        float velocity = duration > 0 ? (float) distance / duration : 0f;

        if (distance >= SWIPE_THRESHOLD && velocity >= SWIPE_VELOCITY_THRESHOLD) {
            // M-4修正: 角度による判定（±30度の範囲内）
            double angle = Math.atan2(Math.abs(deltaY), Math.abs(deltaX));
            double angleDegrees = Math.toDegrees(angle);

            GestureType swipeType = null;

            // 水平方向（0°±30°）
            if (angleDegrees <= SWIPE_HORIZONTAL_ANGLE_MAX) {
                swipeType = deltaX > 0 ? GestureType.SWIPE_RIGHT : GestureType.SWIPE_LEFT;
            }
            // 垂直方向（90°±30°）
            else if (angleDegrees >= SWIPE_VERTICAL_ANGLE_MIN) {
                swipeType = deltaY > 0 ? GestureType.SWIPE_DOWN : GestureType.SWIPE_UP;
            }
            // 対角線方向（30°〜60°）は検出しない

            if (swipeType != null) {
                GestureEvent swipeEvent = new GestureEvent(swipeType, startX, startY, endX, endY, startTime, endTime);
                dispatchGestureEvent(swipeEvent);
                return true;
            }
        }
        return false;
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
                // M-5修正: 例外が発生しても次のリスナーへイベント配信を継続
                if (logger != null) {
                    logger.error("GestureManager", "Error in gesture listener " + listener.getClass().getSimpleName() +
                                ", continuing to next listener", e);
                } else {
                    e.printStackTrace();
                }
                // 例外が発生したリスナーはスキップし、次のリスナーへ継続
                // break; を削除することで継続
            }
        }

        if (logger != null && event.getType() != GestureType.DRAG_MOVE) {
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
        int distance = calculateDistance(startX, startY, currentX, currentY);

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

    /**
     * L-5修正: 2点間の距離を計算する共通メソッド
     */
    private int calculateDistance(int x1, int y1, int x2, int y2) {
        int dx = x2 - x1;
        int dy = y2 - y1;
        return (int) Math.sqrt(dx * dx + dy * dy);
    }

    private void debug(String message) {
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

    public boolean isPressed() { return isPressed; }
    public boolean isDragging() { return isDragging; }
    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
    public int getCurrentX() { return currentX; }
    public int getCurrentY() { return currentY; }
}
