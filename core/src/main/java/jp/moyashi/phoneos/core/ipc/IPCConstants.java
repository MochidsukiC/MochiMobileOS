package jp.moyashi.phoneos.core.ipc;

/**
 * IPC共有メモリのレイアウト定義。
 * server/forgeモジュール間で共有される定数。
 *
 * 共有メモリレイアウト:
 * Offset 0-3:    magic (0x4D4D4F53 = "MMOS")
 * Offset 4-7:    version
 * Offset 8-11:   width
 * Offset 12-15:  height
 * Offset 16-19:  frameCount (フレームカウンター)
 * Offset 20-23:  frameRate (現在のフレームレート)
 * Offset 24-27:  state flags (sleeping, debugMode, hasTextInputFocus等)
 * Offset 28-31:  topClosableLayer (LayerType ordinal)
 * Offset 32-35:  inputQueueHead
 * Offset 36-39:  inputQueueTail
 * Offset 40-43:  commandType (pending command)
 * Offset 44-47:  commandArg1
 * Offset 48-51:  commandArg2
 * Offset 52-115: commandString (64 bytes, UTF-8)
 * Offset 116-127: reserved
 * Offset 128-2175: inputQueue (128 events x 16 bytes)
 * Offset 2176+:  pixels[width*height] (400x600x4 = 960,000 bytes)
 *
 * Total minimum size: 2176 + 960000 = 962,176 bytes (~940KB)
 *
 * @author jp.moyashi
 * @version 1.0
 */
public final class IPCConstants {

    private IPCConstants() {
        // ユーティリティクラス
    }

    // === マジックナンバーとバージョン ===
    /** 共有メモリ識別子 "MMOS" */
    public static final int MAGIC = 0x4D4D4F53;
    /** プロトコルバージョン */
    public static final int VERSION = 1;

    // === 画面サイズ ===
    public static final int SCREEN_WIDTH = 400;
    public static final int SCREEN_HEIGHT = 600;
    public static final int PIXEL_COUNT = SCREEN_WIDTH * SCREEN_HEIGHT;
    public static final int PIXEL_BYTES = PIXEL_COUNT * 4; // ARGB = 4 bytes

    // === 共有メモリオフセット ===
    public static final int OFFSET_MAGIC = 0;
    public static final int OFFSET_VERSION = 4;
    public static final int OFFSET_WIDTH = 8;
    public static final int OFFSET_HEIGHT = 12;
    public static final int OFFSET_FRAME_COUNT = 16;
    public static final int OFFSET_FRAME_RATE = 20;
    public static final int OFFSET_STATE_FLAGS = 24;
    public static final int OFFSET_TOP_CLOSABLE_LAYER = 28;
    public static final int OFFSET_INPUT_QUEUE_HEAD = 32;
    public static final int OFFSET_INPUT_QUEUE_TAIL = 36;
    public static final int OFFSET_COMMAND_TYPE = 40;
    public static final int OFFSET_COMMAND_ARG1 = 44;
    public static final int OFFSET_COMMAND_ARG2 = 48;
    public static final int OFFSET_COMMAND_STRING = 52;
    public static final int COMMAND_STRING_SIZE = 64;
    public static final int OFFSET_RESERVED = 116;
    public static final int OFFSET_INPUT_QUEUE = 128;
    public static final int INPUT_QUEUE_SIZE = 128; // 最大128イベント
    public static final int INPUT_EVENT_SIZE = 16;  // 1イベント16バイト
    public static final int OFFSET_PIXELS = OFFSET_INPUT_QUEUE + (INPUT_QUEUE_SIZE * INPUT_EVENT_SIZE);

    // === 共有メモリ総サイズ ===
    public static final int SHARED_MEMORY_SIZE = OFFSET_PIXELS + PIXEL_BYTES;

    // === 状態フラグビット ===
    public static final int FLAG_SLEEPING = 0x01;
    public static final int FLAG_DEBUG_MODE = 0x02;
    public static final int FLAG_HAS_TEXT_INPUT_FOCUS = 0x04;
    public static final int FLAG_SERVER_READY = 0x08;
    public static final int FLAG_CLIENT_CONNECTED = 0x10;

    // === コマンドタイプ ===
    public static final int CMD_NONE = 0;
    public static final int CMD_INIT = 1;
    public static final int CMD_SHUTDOWN = 2;
    public static final int CMD_SLEEP = 3;
    public static final int CMD_WAKE = 4;
    public static final int CMD_GO_HOME = 5;
    public static final int CMD_HOME_BUTTON = 6;
    public static final int CMD_ADD_LAYER = 7;
    public static final int CMD_REMOVE_LAYER = 8;
    public static final int CMD_FRAMERATE = 9;
    public static final int CMD_RESIZE = 10;

    // === 入力イベントタイプ ===
    public static final int INPUT_MOUSE_PRESSED = 1;
    public static final int INPUT_MOUSE_RELEASED = 2;
    public static final int INPUT_MOUSE_DRAGGED = 3;
    public static final int INPUT_MOUSE_MOVED = 4;
    public static final int INPUT_MOUSE_WHEEL = 5;
    public static final int INPUT_KEY_PRESSED = 6;
    public static final int INPUT_KEY_RELEASED = 7;
    public static final int INPUT_GESTURE = 8;

    // === 修飾キーフラグ ===
    public static final int MOD_SHIFT = 0x01;
    public static final int MOD_CTRL = 0x02;
    public static final int MOD_ALT = 0x04;
    public static final int MOD_META = 0x08;

    // === 共有メモリ名 ===
    /** Windows用共有メモリファイル名プレフィックス */
    public static final String SHM_NAME_PREFIX = "mmos_shm_";

    /**
     * ワールドID用の共有メモリ名を生成する。
     *
     * @param worldId ワールドID
     * @return 共有メモリ名
     */
    public static String getSharedMemoryName(String worldId) {
        // ワールドIDをサニタイズ（ファイル名に使えない文字を除去）
        String sanitized = worldId.replaceAll("[^a-zA-Z0-9_-]", "_");
        return SHM_NAME_PREFIX + sanitized;
    }
}
