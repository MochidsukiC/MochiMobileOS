package jp.moyashi.phoneos.forge.audio;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Simple Voice Chatから受信した他プレイヤーの音声をキャプチャするクラス。
 * ClientReceiveSoundEventで受信した音声データをバッファに保存する。
 */
public class VoicechatAudioCapture {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final VoicechatAudioCapture INSTANCE = new VoicechatAudioCapture();

    private final Queue<AudioPacket> audioBuffer = new LinkedList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private boolean capturing = false;

    private VoicechatAudioCapture() {
    }

    public static VoicechatAudioCapture getInstance() {
        return INSTANCE;
    }

    /**
     * 音声キャプチャを開始する。
     */
    public void startCapture() {
        lock.writeLock().lock();
        try {
            capturing = true;
            audioBuffer.clear();
            LOGGER.info("[VoicechatAudioCapture] Started capturing audio - capturing=" + capturing);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 音声キャプチャを停止する。
     */
    public void stopCapture() {
        lock.writeLock().lock();
        try {
            capturing = false;
            LOGGER.info("[VoicechatAudioCapture] Stopped capturing audio");
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
     * 音声データを受信したときに呼ばれる。
     *
     * @param senderId 送信者のUUID
     * @param audioData 音声データ（16-bit PCM）
     * @param whispering ささやきモードかどうか
     */
    public void onAudioReceived(UUID senderId, short[] audioData, boolean whispering) {
        lock.writeLock().lock();
        try {
            LOGGER.info("[VoicechatAudioCapture] Received audio from " + senderId +
                        " - length: " + (audioData != null ? audioData.length : 0) +
                        ", capturing: " + capturing);

            if (!capturing) {
                LOGGER.info("[VoicechatAudioCapture] Not capturing, ignoring audio");
                return;
            }

            // 音声パケットをバッファに追加
            audioBuffer.offer(new AudioPacket(
                System.currentTimeMillis(),
                senderId,
                audioData,
                whispering
            ));

            LOGGER.info("[VoicechatAudioCapture] Added to buffer, size: " + audioBuffer.size());

            // バッファサイズを制限（最大1000パケット = 約20秒）
            // ボイスメモ録音で長時間録音する可能性があるため、大きめに設定
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
            AudioPacket packet = audioBuffer.poll();
            if (packet == null) {
                return null;
            }

            // short[]をbyte[]に変換（16-bit PCM）
            byte[] bytes = new byte[packet.audioData.length * 2];
            for (int i = 0; i < packet.audioData.length; i++) {
                short sample = packet.audioData[i];
                bytes[i * 2] = (byte) (sample & 0xFF);
                bytes[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
            }

            return bytes;

        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * バッファ内の全音声データを取得して連結する。
     * データ欠落を防ぐため、ボイスメモ録音などで使用する。
     *
     * @return 連結された音声データ、バッファが空の場合はnull
     */
    public byte[] pollAllAudioData() {
        lock.writeLock().lock();
        try {
            if (audioBuffer.isEmpty()) {
                return null;
            }

            java.util.ArrayList<short[]> chunks = new java.util.ArrayList<>();
            int totalSamples = 0;

            while (!audioBuffer.isEmpty()) {
                AudioPacket packet = audioBuffer.poll();
                if (packet != null && packet.audioData != null) {
                    chunks.add(packet.audioData);
                    totalSamples += packet.audioData.length;
                }
            }

            if (chunks.isEmpty()) {
                return null;
            }

            // 全チャンクを連結してbyte[]に変換
            byte[] result = new byte[totalSamples * 2];
            int offset = 0;
            for (short[] chunk : chunks) {
                for (short sample : chunk) {
                    result[offset++] = (byte) (sample & 0xFF);
                    result[offset++] = (byte) ((sample >> 8) & 0xFF);
                }
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

    /**
     * 音声パケットを表すクラス。
     */
    private static class AudioPacket {
        final long timestamp;
        final UUID senderId;
        final short[] audioData;
        final boolean whispering;

        AudioPacket(long timestamp, UUID senderId, short[] audioData, boolean whispering) {
            this.timestamp = timestamp;
            this.senderId = senderId;
            this.audioData = audioData;
            this.whispering = whispering;
        }
    }
}
