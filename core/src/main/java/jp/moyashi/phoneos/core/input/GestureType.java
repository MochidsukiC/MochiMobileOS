package jp.moyashi.phoneos.core.input;

/**
 * サポートされるジェスチャータイプを定義する列挙型。
 */
public enum GestureType {
    
    /** 短いタップ（通常のクリック） */
    TAP,
    
    /** 長押し */
    LONG_PRESS,
    
    /** ドラッグ開始 */
    DRAG_START,
    
    /** ドラッグ中 */
    DRAG_MOVE,
    
    /** ドラッグ終了 */
    DRAG_END,
    
    /** 左スワイプ */
    SWIPE_LEFT,
    
    /** 右スワイプ */
    SWIPE_RIGHT,
    
    /** 上スワイプ */
    SWIPE_UP,
    
    /** 下スワイプ */
    SWIPE_DOWN
}