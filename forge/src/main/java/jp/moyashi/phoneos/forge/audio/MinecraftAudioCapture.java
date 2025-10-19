package jp.moyashi.phoneos.forge.audio;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Minecraftの環境音（ゲーム音）をキャプチャするクラス。
 *
 * 実装方法：
 * Minecraftの音声システム（SoundEngine）にフックして音声データをキャプチャする。
 *
 * TODO: 現在は未実装。Mixinを使用してSoundEngineの出力をキャプチャする必要がある。
 */
public class MinecraftAudioCapture {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final MinecraftAudioCapture INSTANCE = new MinecraftAudioCapture();

    private final Queue<byte[]> audioBuffer = new LinkedList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private boolean capturing = false;

    private MinecraftAudioCapture() {
    }

    public static MinecraftAudioCapture getInstance() {
        return INSTANCE;
    }

    /**
     * 環境音キャプチャを開始する。
     */
    public void startCapture() {
        lock.writeLock().lock();
        try {
            capturing = true;
            audioBuffer.clear();
            LOGGER.info("[MinecraftAudioCapture] Started capturing environment audio");
            LOGGER.warn("[MinecraftAudioCapture] Environment audio capture is not yet implemented");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 環境音キャプチャを停止する。
     */
    public void stopCapture() {
        lock.writeLock().lock();
        try {
            capturing = false;
            LOGGER.info("[MinecraftAudioCapture] Stopped capturing environment audio");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * キャプチャ中かどうかを取得する。
     */
    public boolean isCapturing() {
        lock.readLock().lock();
        try {
            return capturing;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 環境音データを受信したときに呼ばれる。
     * TODO: SoundEngine Mixinから呼び出される予定
     *
     * @param audioData 音声データ（16-bit PCM）
     */
    public void onEnvironmentAudio(byte[] audioData) {
        lock.writeLock().lock();
        try {
            if (!capturing) {
                return;
            }

            // 音声データをバッファに追加
            audioBuffer.offer(audioData);

            // バッファサイズを制限（最大1000パケット = 約20秒）
            while (audioBuffer.size() > 1000) {
                audioBuffer.poll();
            }

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * バッファから音声データを取得する。
     *
     * @return 音声データ、バッファが空の場合はnull
     */
    public byte[] pollAudioData() {
        lock.writeLock().lock();
        try {
            return audioBuffer.poll();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * バッファ内の全音声データを取得して連結する。
     *
     * @return 連結された音声データ、バッファが空の場合はnull
     */
    public byte[] pollAllAudioData() {
        lock.writeLock().lock();
        try {
            if (audioBuffer.isEmpty()) {
                return null;
            }

            java.util.ArrayList<byte[]> chunks = new java.util.ArrayList<>();
            int totalSize = 0;

            while (!audioBuffer.isEmpty()) {
                byte[] chunk = audioBuffer.poll();
                if (chunk != null) {
                    chunks.add(chunk);
                    totalSize += chunk.length;
                }
            }

            if (chunks.isEmpty()) {
                return null;
            }

            // 全チャンクを連結
            byte[] result = new byte[totalSize];
            int offset = 0;
            for (byte[] chunk : chunks) {
                System.arraycopy(chunk, 0, result, offset, chunk.length);
                offset += chunk.length;
            }

            return result;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * バッファ内の音声データ数を取得する。
     */
    public int getBufferSize() {
        lock.readLock().lock();
        try {
            return audioBuffer.size();
        } finally {
            lock.readLock().unlock();
        }
    }
}
