package jp.moyashi.phoneos.core.input;

/**
 * ジェスチャーイベントを受信するためのリスナーインターフェース。
 * アプリケーションはこのインターフェースを実装してジェスチャーハンドリングを行う。
 */
public interface GestureListener {
    
    /**
     * ジェスチャーイベントが発生した時に呼び出される。
     * 
     * @param event ジェスチャーイベント
     * @return イベントを処理した場合true、処理しなかった場合false
     */
    boolean onGesture(GestureEvent event);
    
    /**
     * このリスナーが処理可能な領域内かどうかを確認する。
     * 
     * @param x X座標
     * @param y Y座標
     * @return 処理可能な場合true
     */
    default boolean isInBounds(int x, int y) {
        return true; // デフォルトは全画面対応
    }
    
    /**
     * このリスナーの優先度を取得する。
     * 値が高いほど優先される。
     * 
     * @return 優先度（デフォルト: 0）
     */
    default int getPriority() {
        return 0;
    }
}