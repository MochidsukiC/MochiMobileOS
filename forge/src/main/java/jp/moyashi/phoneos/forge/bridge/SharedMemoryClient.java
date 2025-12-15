package jp.moyashi.phoneos.forge.bridge;

import jp.moyashi.phoneos.core.ipc.IPCConstants;
import jp.moyashi.phoneos.core.ipc.InputEvent;
import jp.moyashi.phoneos.core.ipc.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;

/**
 * 共有メモリクライアント（Forge側）。
 * メモリマップドファイルを使用してMMOSサーバーと通信する。
 *
 * @author jp.moyashi
 * @version 1.0
 */
public class SharedMemoryClient implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger(SharedMemoryClient.class);

    /** 共有メモリ名 */
    private final String name;

    /** メモリマップドファイル */
    private RandomAccessFile file;

    /** ファイルチャンネル */
    private FileChannel channel;

    /** メモリマップドバッファ */
    private MappedByteBuffer buffer;

    /** 入力イベント書き込み用バッファ */
    private final ByteBuffer eventBuffer = ByteBuffer.allocate(IPCConstants.INPUT_EVENT_SIZE);

    /** ピクセルキャッシュ */
    private int[] pixelCache;

    /** 最後に読み取ったフレームID */
    private int lastFrameId = -1;

    /** 接続済みフラグ */
    private volatile boolean connected = false;

    /**
     * 共有メモリクライアントを作成する。
     *
     * @param worldId ワールドID
     */
    public SharedMemoryClient(String worldId) {
        this.name = IPCConstants.getSharedMemoryName(worldId);
        this.pixelCache = new int[IPCConstants.PIXEL_COUNT];
    }

    /**
     * サーバーに接続する。
     *
     * @return 接続成功した場合true
     */
    public boolean connect() {
        if (connected) {
            return true;
        }

        try {
            // 一時ディレクトリの共有メモリファイルを探す
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "mmos");
            File shmFile = new File(tempDir, name + ".shm");

            if (!shmFile.exists()) {
                LOGGER.debug("[SharedMemoryClient] Shared memory file not found: {}", shmFile.getAbsolutePath());
                return false;
            }

            LOGGER.info("[SharedMemoryClient] Connecting to shared memory: {}", shmFile.getAbsolutePath());

            // ファイルを開く
            this.file = new RandomAccessFile(shmFile, "rw");
            this.channel = file.getChannel();
            this.buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, IPCConstants.SHARED_MEMORY_SIZE);

            // マジックナンバーを確認
            buffer.position(IPCConstants.OFFSET_MAGIC);
            int magic = buffer.getInt();
            if (magic != IPCConstants.MAGIC) {
                LOGGER.error("[SharedMemoryClient] Invalid magic number: 0x{}", Integer.toHexString(magic));
                close();
                return false;
            }

            // クライアント接続フラグを設定
            setClientConnected(true);

            connected = true;
            LOGGER.info("[SharedMemoryClient] Connected to shared memory successfully");
            return true;

        } catch (IOException e) {
            LOGGER.error("[SharedMemoryClient] Failed to connect to shared memory", e);
            return false;
        }
    }

    /**
     * 接続済みかどうかを返す。
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * サーバーが準備完了しているかを確認する。
     */
    public boolean isServerReady() {
        if (!connected) return false;

        buffer.position(IPCConstants.OFFSET_STATE_FLAGS);
        int flags = buffer.getInt();
        return (flags & IPCConstants.FLAG_SERVER_READY) != 0;
    }

    /**
     * クライアント接続フラグを設定する。
     */
    private void setClientConnected(boolean connected) {
        buffer.position(IPCConstants.OFFSET_STATE_FLAGS);
        int flags = buffer.getInt();
        if (connected) {
            flags |= IPCConstants.FLAG_CLIENT_CONNECTED;
        } else {
            flags &= ~IPCConstants.FLAG_CLIENT_CONNECTED;
        }
        buffer.position(IPCConstants.OFFSET_STATE_FLAGS);
        buffer.putInt(flags);
    }

    /**
     * サーバー状態を読み取る。
     */
    public ServerState readState() {
        if (!connected) return null;
        return ServerState.readFrom(buffer);
    }

    /**
     * ピクセルデータを読み取る。
     * フレームIDが変わっていない場合はキャッシュを返す。
     *
     * @return ピクセル配列（ARGB形式）
     */
    public int[] readPixels() {
        if (!connected) return pixelCache;

        // フレームカウントをチェック
        buffer.position(IPCConstants.OFFSET_FRAME_COUNT);
        int currentFrameId = buffer.getInt();

        if (currentFrameId == lastFrameId) {
            // フレームが更新されていない - キャッシュを返す
            return pixelCache;
        }

        // 新しいフレーム - ピクセルを読み取る
        lastFrameId = currentFrameId;

        buffer.position(IPCConstants.OFFSET_PIXELS);
        for (int i = 0; i < IPCConstants.PIXEL_COUNT; i++) {
            pixelCache[i] = buffer.getInt();
        }

        return pixelCache;
    }

    /**
     * コマンドを送信する。
     *
     * @param cmdType コマンドタイプ（IPCConstants.CMD_*）
     */
    public void sendCommand(int cmdType) {
        sendCommand(cmdType, 0, 0, null);
    }

    /**
     * コマンドを送信する（引数付き）。
     *
     * @param cmdType コマンドタイプ
     * @param arg1 引数1
     */
    public void sendCommand(int cmdType, int arg1) {
        sendCommand(cmdType, arg1, 0, null);
    }

    /**
     * コマンドを送信する（文字列引数付き）。
     *
     * @param cmdType コマンドタイプ
     * @param arg1 引数1
     * @param arg2 引数2
     * @param str 文字列引数
     */
    public void sendCommand(int cmdType, int arg1, int arg2, String str) {
        if (!connected) return;

        buffer.position(IPCConstants.OFFSET_COMMAND_ARG1);
        buffer.putInt(arg1);

        buffer.position(IPCConstants.OFFSET_COMMAND_ARG2);
        buffer.putInt(arg2);

        if (str != null) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            byte[] padded = new byte[IPCConstants.COMMAND_STRING_SIZE];
            System.arraycopy(bytes, 0, padded, 0, Math.min(bytes.length, IPCConstants.COMMAND_STRING_SIZE - 1));
            buffer.position(IPCConstants.OFFSET_COMMAND_STRING);
            buffer.put(padded);
        }

        // コマンドタイプを最後に設定（アトミック性のため）
        buffer.position(IPCConstants.OFFSET_COMMAND_TYPE);
        buffer.putInt(cmdType);
    }

    /**
     * 入力イベントをキューに追加する。
     *
     * @param event 入力イベント
     */
    public void writeInputEvent(InputEvent event) {
        if (!connected) return;

        buffer.position(IPCConstants.OFFSET_INPUT_QUEUE_HEAD);
        int head = buffer.getInt();

        buffer.position(IPCConstants.OFFSET_INPUT_QUEUE_TAIL);
        int tail = buffer.getInt();

        // 次のテール位置
        int nextTail = (tail + 1) % IPCConstants.INPUT_QUEUE_SIZE;

        if (nextTail == head) {
            // キューがフル - 古いイベントを破棄
            LOGGER.warn("[SharedMemoryClient] Input queue full, dropping event");
            return;
        }

        // イベントを書き込み
        eventBuffer.clear();
        event.writeTo(eventBuffer);
        eventBuffer.flip();

        int eventOffset = IPCConstants.OFFSET_INPUT_QUEUE + (tail * IPCConstants.INPUT_EVENT_SIZE);
        buffer.position(eventOffset);
        while (eventBuffer.hasRemaining()) {
            buffer.put(eventBuffer.get());
        }

        // テールを更新
        buffer.position(IPCConstants.OFFSET_INPUT_QUEUE_TAIL);
        buffer.putInt(nextTail);
    }

    @Override
    public void close() {
        if (!connected) return;

        LOGGER.info("[SharedMemoryClient] Closing shared memory client: {}", name);

        try {
            // クライアント接続フラグをクリア
            if (buffer != null) {
                setClientConnected(false);
                buffer.force();
            }

            if (channel != null) {
                channel.close();
            }

            if (file != null) {
                file.close();
            }

            connected = false;

        } catch (IOException e) {
            LOGGER.error("[SharedMemoryClient] Error closing shared memory client", e);
        }
    }

    /**
     * 共有メモリ名を取得する。
     */
    public String getName() {
        return name;
    }
}
