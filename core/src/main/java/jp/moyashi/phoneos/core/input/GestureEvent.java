package jp.moyashi.phoneos.core.input;

/**
 * ジェスチャーイベントを表すクラス。
 * ジェスチャーの種類、位置、時間などの情報を含む。
 */
public class GestureEvent {
    
    /** ジェスチャーの種類 */
    private final GestureType type;
    
    /** ジェスチャーの開始X座標 */
    private final int startX;
    
    /** ジェスチャーの開始Y座標 */
    private final int startY;
    
    /** 現在のX座標 */
    private final int currentX;
    
    /** 現在のY座標 */
    private final int currentY;
    
    /** ジェスチャー開始時刻 */
    private final long startTime;
    
    /** 現在時刻 */
    private final long currentTime;
    
    /** ドラッグ距離（該当する場合） */
    private final int dragDistance;
    
    /** スワイプの速度（該当する場合） */
    private final float swipeVelocity;
    
    /**
     * GestureEventを作成する。
     * 
     * @param type ジェスチャータイプ
     * @param startX 開始X座標
     * @param startY 開始Y座標
     * @param currentX 現在X座標
     * @param currentY 現在Y座標
     * @param startTime 開始時刻
     * @param currentTime 現在時刻
     */
    public GestureEvent(GestureType type, int startX, int startY, int currentX, int currentY, 
                       long startTime, long currentTime) {
        this.type = type;
        this.startX = startX;
        this.startY = startY;
        this.currentX = currentX;
        this.currentY = currentY;
        this.startTime = startTime;
        this.currentTime = currentTime;
        
        // ドラッグ距離を計算
        this.dragDistance = (int) Math.sqrt(Math.pow(currentX - startX, 2) + Math.pow(currentY - startY, 2));
        
        // スワイプ速度を計算
        long timeDiff = currentTime - startTime;
        this.swipeVelocity = timeDiff > 0 ? (float) dragDistance / timeDiff : 0f;
    }
    
    /**
     * 簡単なジェスチャーイベントを作成する。
     * 
     * @param type ジェスチャータイプ
     * @param x X座標
     * @param y Y座標
     */
    public GestureEvent(GestureType type, int x, int y) {
        this(type, x, y, x, y, System.currentTimeMillis(), System.currentTimeMillis());
    }
    
    // Getters
    
    public GestureType getType() { return type; }
    public int getStartX() { return startX; }
    public int getStartY() { return startY; }
    public int getCurrentX() { return currentX; }
    public int getCurrentY() { return currentY; }
    public long getStartTime() { return startTime; }
    public long getCurrentTime() { return currentTime; }
    public int getDragDistance() { return dragDistance; }
    public float getSwipeVelocity() { return swipeVelocity; }
    
    /**
     * ジェスチャーの継続時間を取得する。
     * 
     * @return 継続時間（ミリ秒）
     */
    public long getDuration() {
        return currentTime - startTime;
    }
    
    /**
     * ドラッグのX方向の移動距離を取得する。
     * 
     * @return X方向の移動距離
     */
    public int getDeltaX() {
        return currentX - startX;
    }
    
    /**
     * ドラッグのY方向の移動距離を取得する。
     * 
     * @return Y方向の移動距離
     */
    public int getDeltaY() {
        return currentY - startY;
    }
    
    @Override
    public String toString() {
        return String.format("GestureEvent{type=%s, start=(%d,%d), current=(%d,%d), duration=%dms, distance=%d}", 
                           type, startX, startY, currentX, currentY, getDuration(), dragDistance);
    }
}