package jp.moyashi.phoneos.forge.audio;

/**
 * スレッドセーフなバイトリングバッファ。
 * 1人のWriterと1人のReaderを想定（今回は同期化して複数対応）。
 */
public class ByteRingBuffer {
    private final byte[] buffer;
    private final int capacity;
    private int readPos = 0;
    private int writePos = 0;
    private int count = 0; // 現在格納されているデータ量

    public ByteRingBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new byte[capacity];
    }

    /**
     * データを書き込む。
     * バッファが一杯の場合、書き込めるだけ書き込んで、実際に書き込んだバイト数を返す。
     *
     * @param data   ソースデータ
     * @param offset ソースのオフセット
     * @param length 書き込む長さ
     * @return 実際に書き込まれたバイト数
     */
    public synchronized int write(byte[] data, int offset, int length) {
        if (length <= 0) return 0;

        int freeSpace = capacity - count;
        int toWrite = Math.min(length, freeSpace);

        if (toWrite == 0) return 0;

        int firstChunk = Math.min(toWrite, capacity - writePos);
        System.arraycopy(data, offset, buffer, writePos, firstChunk);
        
        int secondChunk = toWrite - firstChunk;
        if (secondChunk > 0) {
            System.arraycopy(data, offset + firstChunk, buffer, 0, secondChunk);
        }

        writePos = (writePos + toWrite) % capacity;
        count += toWrite;

        return toWrite;
    }

    /**
     * データをバッファから読み出す。
     *
     * @param dest   出力先バッファ
     * @param offset 出力先のオフセット
     * @param length 読み出す長さ
     * @return 実際に読み出されたバイト数
     */
    public synchronized int read(byte[] dest, int offset, int length) {
        if (length <= 0) return 0;

        int toRead = Math.min(length, count);
        if (toRead == 0) return 0;

        int firstChunk = Math.min(toRead, capacity - readPos);
        System.arraycopy(buffer, readPos, dest, offset, firstChunk);

        int secondChunk = toRead - firstChunk;
        if (secondChunk > 0) {
            System.arraycopy(buffer, 0, dest, offset + firstChunk, secondChunk);
        }

        readPos = (readPos + toRead) % capacity;
        count -= toRead;

        return toRead;
    }

    /**
     * 指定したバイト数だけ読み込み位置を進める（データを捨てる）。
     */
    public synchronized int skip(int length) {
        if (length <= 0) return 0;
        int toSkip = Math.min(length, count);
        readPos = (readPos + toSkip) % capacity;
        count -= toSkip;
        return toSkip;
    }

    /**
     * バッファをクリアする。
     */
    public synchronized void clear() {
        readPos = 0;
        writePos = 0;
        count = 0;
    }

    /**
     * 読み出し可能なバイト数を取得。
     */
    public synchronized int available() {
        return count;
    }

    /**
     * 書き込み可能なバイト数を取得。
     */
    public synchronized int remainingCapacity() {
        return capacity - count;
    }

    public int getCapacity() {
        return capacity;
    }
}
