package jp.moyashi.phoneos.server.ipc;

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
 * 共有メモリ実装（サーバー側）。
 * メモリマップドファイルを使用してプロセス間通信を行う。
 *
 * @author jp.moyashi
 * @version 1.0
 */
public class SharedMemory implements Closeable {

    private static final Logger LOGGER = LogManager.getLogger(SharedMemory.class);

    /** 共有メモリ名 */
    private final String name;

    /** メモリマップドファイル */
    private final RandomAccessFile file;

    /** ファイルチャンネル */
    private final FileChannel channel;

    /** メモリマップドバッファ */
    private final MappedByteBuffer buffer;

    /** 入力イベント読み取り用バッファ */
    private final ByteBuffer eventBuffer = ByteBuffer.allocate(IPCConstants.INPUT_EVENT_SIZE);

    /**
     * 共有メモリを作成する（サーバー側）。
     *
     * @param name 共有メモリ名
     * @return 共有メモリインスタンス
     */
    public static SharedMemory create(String name) throws IOException {
        return new SharedMemory(name, true);
    }

    /**
     * 既存の共有メモリに接続する（クライアント側）。
     *
     * @param name 共有メモリ名
     * @return 共有メモリインスタンス
     */
    public static SharedMemory connect(String name) throws IOException {
        return new SharedMemory(name, false);
    }

    private SharedMemory(String name, boolean create) throws IOException {
        this.name = name;

        // 一時ディレクトリに共有メモリファイルを作成
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "mmos");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        File shmFile = new File(tempDir, name + ".shm");
        LOGGER.info("[SharedMemory] {} shared memory file: {}",
            create ? "Creating" : "Connecting to", shmFile.getAbsolutePath());

        // ファイルを開く
        this.file = new RandomAccessFile(shmFile, "rw");

        if (create) {
            // ファイルサイズを設定
            file.setLength(IPCConstants.SHARED_MEMORY_SIZE);
        }

        // ファイルチャンネルを取得
        this.channel = file.getChannel();

        // メモリにマップ
        this.buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, IPCConstants.SHARED_MEMORY_SIZE);

        if (create) {
            // ヘッダーを初期化
            initializeHeader();
        }

        LOGGER.info("[SharedMemory] Shared memory {} successfully ({} bytes)",
            create ? "created" : "connected", IPCConstants.SHARED_MEMORY_SIZE);
    }

    /**
     * ヘッダーを初期化する（サーバー側）。
     */
    private void initializeHeader() {
        buffer.position(IPCConstants.OFFSET_MAGIC);
        buffer.putInt(IPCConstants.MAGIC);

        buffer.position(IPCConstants.OFFSET_VERSION);
        buffer.putInt(IPCConstants.VERSION);

        buffer.position(IPCConstants.OFFSET_WIDTH);
        buffer.putInt(IPCConstants.SCREEN_WIDTH);

        buffer.position(IPCConstants.OFFSET_HEIGHT);
        buffer.putInt(IPCConstants.SCREEN_HEIGHT);

        buffer.position(IPCConstants.OFFSET_FRAME_COUNT);
        buffer.putInt(0);

        buffer.position(IPCConstants.OFFSET_FRAME_RATE);
        buffer.putInt(60);

        buffer.position(IPCConstants.OFFSET_STATE_FLAGS);
        buffer.putInt(0);

        buffer.position(IPCConstants.OFFSET_INPUT_QUEUE_HEAD);
        buffer.putInt(0);

        buffer.position(IPCConstants.OFFSET_INPUT_QUEUE_TAIL);
        buffer.putInt(0);

        buffer.position(IPCConstants.OFFSET_COMMAND_TYPE);
        buffer.putInt(IPCConstants.CMD_NONE);

        buffer.force();
    }

    /**
     * サーバー準備完了フラグを設定する。
     */
    public void setServerReady(boolean ready) {
        buffer.position(IPCConstants.OFFSET_STATE_FLAGS);
        int flags = buffer.getInt();
        if (ready) {
            flags |= IPCConstants.FLAG_SERVER_READY;
        } else {
            flags &= ~IPCConstants.FLAG_SERVER_READY;
        }
        buffer.position(IPCConstants.OFFSET_STATE_FLAGS);
        buffer.putInt(flags);
    }

    /**
     * クライアント接続済みかチェックする。
     */
    public boolean isClientConnected() {
        buffer.position(IPCConstants.OFFSET_STATE_FLAGS);
        int flags = buffer.getInt();
        return (flags & IPCConstants.FLAG_CLIENT_CONNECTED) != 0;
    }

    /**
     * コマンドタイプを取得する。
     */
    public int getCommandType() {
        buffer.position(IPCConstants.OFFSET_COMMAND_TYPE);
        return buffer.getInt();
    }

    /**
     * コマンド引数1を取得する。
     */
    public int getCommandArg1() {
        buffer.position(IPCConstants.OFFSET_COMMAND_ARG1);
        return buffer.getInt();
    }

    /**
     * コマンド引数2を取得する。
     */
    public int getCommandArg2() {
        buffer.position(IPCConstants.OFFSET_COMMAND_ARG2);
        return buffer.getInt();
    }

    /**
     * コマンド文字列を取得する。
     */
    public String getCommandString() {
        byte[] bytes = new byte[IPCConstants.COMMAND_STRING_SIZE];
        buffer.position(IPCConstants.OFFSET_COMMAND_STRING);
        buffer.get(bytes);

        // null終端を探す
        int length = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == 0) {
                length = i;
                break;
            }
        }

        return new String(bytes, 0, length, StandardCharsets.UTF_8);
    }

    /**
     * コマンドをクリアする。
     */
    public void clearCommand() {
        buffer.position(IPCConstants.OFFSET_COMMAND_TYPE);
        buffer.putInt(IPCConstants.CMD_NONE);
    }

    /**
     * 入力イベントをキューから取得する。
     *
     * @return 入力イベント、またはキューが空の場合null
     */
    public InputEvent pollInputEvent() {
        buffer.position(IPCConstants.OFFSET_INPUT_QUEUE_HEAD);
        int head = buffer.getInt();

        buffer.position(IPCConstants.OFFSET_INPUT_QUEUE_TAIL);
        int tail = buffer.getInt();

        if (head == tail) {
            // キューが空
            return null;
        }

        // イベントを読み取り
        int eventOffset = IPCConstants.OFFSET_INPUT_QUEUE + (head * IPCConstants.INPUT_EVENT_SIZE);
        buffer.position(eventOffset);

        eventBuffer.clear();
        for (int i = 0; i < IPCConstants.INPUT_EVENT_SIZE; i++) {
            eventBuffer.put(buffer.get());
        }
        eventBuffer.flip();

        InputEvent event = InputEvent.readFrom(eventBuffer);

        // ヘッドを進める（循環キュー）
        int newHead = (head + 1) % IPCConstants.INPUT_QUEUE_SIZE;
        buffer.position(IPCConstants.OFFSET_INPUT_QUEUE_HEAD);
        buffer.putInt(newHead);

        return event;
    }

    /**
     * ピクセルデータを書き込む。
     *
     * @param pixels ピクセル配列（ARGB形式）
     */
    public void writePixels(int[] pixels) {
        if (pixels == null || pixels.length != IPCConstants.PIXEL_COUNT) {
            return;
        }

        buffer.position(IPCConstants.OFFSET_PIXELS);
        for (int pixel : pixels) {
            buffer.putInt(pixel);
        }

        // フレームカウントをインクリメント
        buffer.position(IPCConstants.OFFSET_FRAME_COUNT);
        int frameCount = buffer.getInt();
        buffer.position(IPCConstants.OFFSET_FRAME_COUNT);
        buffer.putInt(frameCount + 1);
    }

    /**
     * サーバー状態を書き込む。
     */
    public void writeState(ServerState state) {
        state.writeTo(buffer);
    }

    /**
     * サーバー状態を読み取る。
     */
    public ServerState readState() {
        return ServerState.readFrom(buffer);
    }

    @Override
    public void close() throws IOException {
        LOGGER.info("[SharedMemory] Closing shared memory: {}", name);

        try {
            if (buffer != null) {
                // MappedByteBufferをクリーンアップ
                // Note: Java doesn't guarantee immediate unmap, but we try to help GC
                buffer.force();
            }

            if (channel != null) {
                channel.close();
            }

            if (file != null) {
                file.close();
            }
        } catch (IOException e) {
            LOGGER.error("[SharedMemory] Error closing shared memory", e);
            throw e;
        }
    }

    /**
     * 共有メモリ名を取得する。
     */
    public String getName() {
        return name;
    }
}
