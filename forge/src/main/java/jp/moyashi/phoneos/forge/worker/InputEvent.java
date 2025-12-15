package jp.moyashi.phoneos.forge.worker;

/**
 * MMOS Worker Thread に転送される入力イベントのデータクラス。
 * Minecraft Main Thread から Worker Thread へ非同期で入力を転送するために使用。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class InputEvent {

    /**
     * 入力イベントの種類。
     */
    public enum Type {
        MOUSE_PRESSED,
        MOUSE_RELEASED,
        MOUSE_DRAGGED,
        MOUSE_MOVED,
        MOUSE_WHEEL,
        KEY_PRESSED,
        KEY_RELEASED
    }

    /** イベントタイプ */
    public final Type type;

    /** マウスX座標（MochiMobileOS座標系） */
    public final int x;

    /** マウスY座標（MochiMobileOS座標系） */
    public final int y;

    /** マウスボタン（0=左, 1=中, 2=右） */
    public final int button;

    /** キー文字 */
    public final char key;

    /** キーコード */
    public final int keyCode;

    /** マウスホイールのスクロール量 */
    public final float delta;

    /**
     * マウスイベント用コンストラクタ。
     *
     * @param type   イベントタイプ
     * @param x      X座標
     * @param y      Y座標
     * @param button マウスボタン
     */
    public InputEvent(Type type, int x, int y, int button) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.button = button;
        this.key = 0;
        this.keyCode = 0;
        this.delta = 0;
    }

    /**
     * マウスホイールイベント用コンストラクタ。
     *
     * @param x     X座標
     * @param y     Y座標
     * @param delta スクロール量
     */
    public InputEvent(int x, int y, float delta) {
        this.type = Type.MOUSE_WHEEL;
        this.x = x;
        this.y = y;
        this.button = 0;
        this.key = 0;
        this.keyCode = 0;
        this.delta = delta;
    }

    /**
     * キーボードイベント用コンストラクタ。
     *
     * @param type    イベントタイプ（KEY_PRESSED または KEY_RELEASED）
     * @param key     キー文字
     * @param keyCode キーコード
     */
    public InputEvent(Type type, char key, int keyCode) {
        this.type = type;
        this.x = 0;
        this.y = 0;
        this.button = 0;
        this.key = key;
        this.keyCode = keyCode;
        this.delta = 0;
    }

    @Override
    public String toString() {
        return "InputEvent{" +
                "type=" + type +
                ", x=" + x +
                ", y=" + y +
                ", button=" + button +
                ", key=" + key +
                ", keyCode=" + keyCode +
                ", delta=" + delta +
                '}';
    }
}
