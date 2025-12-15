package jp.moyashi.phoneos.core.ipc;

import java.nio.ByteBuffer;

/**
 * サーバー状態DTO。
 * 共有メモリのヘッダー領域から読み取る状態データ。
 *
 * @author jp.moyashi
 * @version 1.0
 */
public class ServerState {

    /** フレームカウンター */
    public int frameCount;

    /** 現在のフレームレート */
    public int frameRate;

    /** スリープ状態 */
    public boolean sleeping;

    /** デバッグモード */
    public boolean debugMode;

    /** テキスト入力フォーカス */
    public boolean hasTextInputFocus;

    /** サーバー準備完了 */
    public boolean serverReady;

    /** クライアント接続済み */
    public boolean clientConnected;

    /** 最上位クローズ可能レイヤー（ordinal値） */
    public int topClosableLayer;

    /**
     * ByteBufferから状態を読み取る。
     *
     * @param buffer 共有メモリバッファ（先頭から）
     * @return 読み取った状態
     */
    public static ServerState readFrom(ByteBuffer buffer) {
        ServerState state = new ServerState();

        // ヘッダー位置に移動
        buffer.position(IPCConstants.OFFSET_FRAME_COUNT);
        state.frameCount = buffer.getInt();

        buffer.position(IPCConstants.OFFSET_FRAME_RATE);
        state.frameRate = buffer.getInt();

        buffer.position(IPCConstants.OFFSET_STATE_FLAGS);
        int flags = buffer.getInt();
        state.sleeping = (flags & IPCConstants.FLAG_SLEEPING) != 0;
        state.debugMode = (flags & IPCConstants.FLAG_DEBUG_MODE) != 0;
        state.hasTextInputFocus = (flags & IPCConstants.FLAG_HAS_TEXT_INPUT_FOCUS) != 0;
        state.serverReady = (flags & IPCConstants.FLAG_SERVER_READY) != 0;
        state.clientConnected = (flags & IPCConstants.FLAG_CLIENT_CONNECTED) != 0;

        buffer.position(IPCConstants.OFFSET_TOP_CLOSABLE_LAYER);
        state.topClosableLayer = buffer.getInt();

        return state;
    }

    /**
     * 状態をByteBufferに書き込む（サーバー側用）。
     *
     * @param buffer 共有メモリバッファ
     */
    public void writeTo(ByteBuffer buffer) {
        buffer.position(IPCConstants.OFFSET_FRAME_COUNT);
        buffer.putInt(frameCount);

        buffer.position(IPCConstants.OFFSET_FRAME_RATE);
        buffer.putInt(frameRate);

        int flags = 0;
        if (sleeping) flags |= IPCConstants.FLAG_SLEEPING;
        if (debugMode) flags |= IPCConstants.FLAG_DEBUG_MODE;
        if (hasTextInputFocus) flags |= IPCConstants.FLAG_HAS_TEXT_INPUT_FOCUS;
        if (serverReady) flags |= IPCConstants.FLAG_SERVER_READY;
        if (clientConnected) flags |= IPCConstants.FLAG_CLIENT_CONNECTED;

        buffer.position(IPCConstants.OFFSET_STATE_FLAGS);
        buffer.putInt(flags);

        buffer.position(IPCConstants.OFFSET_TOP_CLOSABLE_LAYER);
        buffer.putInt(topClosableLayer);
    }

    @Override
    public String toString() {
        return "ServerState{frameCount=" + frameCount +
               ", frameRate=" + frameRate +
               ", sleeping=" + sleeping +
               ", debugMode=" + debugMode +
               ", hasTextInputFocus=" + hasTextInputFocus +
               ", serverReady=" + serverReady +
               ", topClosableLayer=" + topClosableLayer + "}";
    }
}
