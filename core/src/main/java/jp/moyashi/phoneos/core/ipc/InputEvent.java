package jp.moyashi.phoneos.core.ipc;

import java.nio.ByteBuffer;

/**
 * IPC入力イベントDTO。
 * 共有メモリの入力キューで使用されるイベントデータ構造。
 *
 * バイナリレイアウト（16バイト固定）:
 * Offset 0-3:  type (int) - イベントタイプ（INPUT_*定数）
 * Offset 4-5:  x (short) - X座標（マウス）またはkeyCode上位（キーボード）
 * Offset 6-7:  y (short) - Y座標（マウス）またはkeyCode下位（キーボード）
 * Offset 8-9:  button (short) - マウスボタン（マウス）またはkey char（キーボード）
 * Offset 10:   modifiers (byte) - 修飾キーフラグ
 * Offset 11:   reserved1 (byte)
 * Offset 12-15: deltaOrExtra (float) - ホイールデルタまたは追加データ
 *
 * @author jp.moyashi
 * @version 1.0
 */
public class InputEvent {

    /** イベントタイプ（IPCConstants.INPUT_*） */
    public int type;

    /** X座標（マウスイベント） */
    public int x;

    /** Y座標（マウスイベント） */
    public int y;

    /** マウスボタン（マウスイベント） */
    public int button;

    /** キー文字（キーボードイベント） */
    public char keyChar;

    /** キーコード（キーボードイベント） */
    public int keyCode;

    /** 修飾キーフラグ（IPCConstants.MOD_*の組み合わせ） */
    public int modifiers;

    /** ホイールデルタ（マウスホイールイベント） */
    public float delta;

    /**
     * デフォルトコンストラクタ。
     */
    public InputEvent() {
    }

    /**
     * マウスイベント用コンストラクタ。
     *
     * @param type イベントタイプ
     * @param x X座標
     * @param y Y座標
     * @param button マウスボタン
     * @param modifiers 修飾キーフラグ
     */
    public InputEvent(int type, int x, int y, int button, int modifiers) {
        this.type = type;
        this.x = x;
        this.y = y;
        this.button = button;
        this.modifiers = modifiers;
    }

    /**
     * マウスホイールイベント用コンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param delta ホイールデルタ
     * @param modifiers 修飾キーフラグ
     */
    public static InputEvent mouseWheel(int x, int y, float delta, int modifiers) {
        InputEvent event = new InputEvent();
        event.type = IPCConstants.INPUT_MOUSE_WHEEL;
        event.x = x;
        event.y = y;
        event.delta = delta;
        event.modifiers = modifiers;
        return event;
    }

    /**
     * キーボードイベント用ファクトリメソッド。
     *
     * @param type イベントタイプ（INPUT_KEY_PRESSED or INPUT_KEY_RELEASED）
     * @param keyChar キー文字
     * @param keyCode キーコード
     * @param modifiers 修飾キーフラグ
     */
    public static InputEvent keyboard(int type, char keyChar, int keyCode, int modifiers) {
        InputEvent event = new InputEvent();
        event.type = type;
        event.keyChar = keyChar;
        event.keyCode = keyCode;
        event.modifiers = modifiers;
        return event;
    }

    /**
     * マウスプレスイベントを作成。
     */
    public static InputEvent mousePressed(int x, int y, int modifiers) {
        return new InputEvent(IPCConstants.INPUT_MOUSE_PRESSED, x, y, 1, modifiers);
    }

    /**
     * マウスリリースイベントを作成。
     */
    public static InputEvent mouseReleased(int x, int y, int modifiers) {
        return new InputEvent(IPCConstants.INPUT_MOUSE_RELEASED, x, y, 1, modifiers);
    }

    /**
     * マウスドラッグイベントを作成。
     */
    public static InputEvent mouseDragged(int x, int y, int modifiers) {
        return new InputEvent(IPCConstants.INPUT_MOUSE_DRAGGED, x, y, 1, modifiers);
    }

    /**
     * マウスムーブイベントを作成。
     */
    public static InputEvent mouseMoved(int x, int y, int modifiers) {
        return new InputEvent(IPCConstants.INPUT_MOUSE_MOVED, x, y, 0, modifiers);
    }

    /**
     * ByteBufferにシリアライズする。
     *
     * @param buffer 書き込み先バッファ
     */
    public void writeTo(ByteBuffer buffer) {
        buffer.putInt(type);

        if (type == IPCConstants.INPUT_KEY_PRESSED || type == IPCConstants.INPUT_KEY_RELEASED) {
            // キーボードイベント: x/yをkeyCodeに使用、buttonをkeyCharに使用
            buffer.putShort((short) ((keyCode >> 16) & 0xFFFF));
            buffer.putShort((short) (keyCode & 0xFFFF));
            buffer.putShort((short) keyChar);
        } else {
            // マウスイベント
            buffer.putShort((short) x);
            buffer.putShort((short) y);
            buffer.putShort((short) button);
        }

        buffer.put((byte) modifiers);
        buffer.put((byte) 0); // reserved

        if (type == IPCConstants.INPUT_MOUSE_WHEEL) {
            buffer.putFloat(delta);
        } else {
            buffer.putFloat(0.0f);
        }
    }

    /**
     * ByteBufferからデシリアライズする。
     *
     * @param buffer 読み込み元バッファ
     * @return デシリアライズされたイベント
     */
    public static InputEvent readFrom(ByteBuffer buffer) {
        InputEvent event = new InputEvent();
        event.type = buffer.getInt();

        short val1 = buffer.getShort();
        short val2 = buffer.getShort();
        short val3 = buffer.getShort();

        if (event.type == IPCConstants.INPUT_KEY_PRESSED || event.type == IPCConstants.INPUT_KEY_RELEASED) {
            // キーボードイベント
            event.keyCode = ((val1 & 0xFFFF) << 16) | (val2 & 0xFFFF);
            event.keyChar = (char) val3;
        } else {
            // マウスイベント
            event.x = val1;
            event.y = val2;
            event.button = val3;
        }

        event.modifiers = buffer.get() & 0xFF;
        buffer.get(); // reserved

        event.delta = buffer.getFloat();

        return event;
    }

    /**
     * 修飾キーフラグを設定する。
     *
     * @param shift Shiftキー
     * @param ctrl Ctrlキー
     * @param alt Altキー
     * @param meta Metaキー
     */
    public void setModifiers(boolean shift, boolean ctrl, boolean alt, boolean meta) {
        modifiers = 0;
        if (shift) modifiers |= IPCConstants.MOD_SHIFT;
        if (ctrl) modifiers |= IPCConstants.MOD_CTRL;
        if (alt) modifiers |= IPCConstants.MOD_ALT;
        if (meta) modifiers |= IPCConstants.MOD_META;
    }

    /**
     * Shiftキーが押されているか。
     */
    public boolean isShiftPressed() {
        return (modifiers & IPCConstants.MOD_SHIFT) != 0;
    }

    /**
     * Ctrlキーが押されているか。
     */
    public boolean isCtrlPressed() {
        return (modifiers & IPCConstants.MOD_CTRL) != 0;
    }

    /**
     * Altキーが押されているか。
     */
    public boolean isAltPressed() {
        return (modifiers & IPCConstants.MOD_ALT) != 0;
    }

    /**
     * Metaキーが押されているか。
     */
    public boolean isMetaPressed() {
        return (modifiers & IPCConstants.MOD_META) != 0;
    }

    @Override
    public String toString() {
        return "InputEvent{type=" + type + ", x=" + x + ", y=" + y +
               ", button=" + button + ", keyChar=" + keyChar +
               ", keyCode=" + keyCode + ", modifiers=" + modifiers +
               ", delta=" + delta + "}";
    }
}
