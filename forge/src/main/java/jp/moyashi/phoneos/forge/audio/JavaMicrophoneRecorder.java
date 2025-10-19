package jp.moyashi.phoneos.forge.audio;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Java標準APIを使用してマイクから音声を録音するクラス。
 * Simple Voice Chatの設定を流用し、同じマイクデバイスを使用する。
 */
public class JavaMicrophoneRecorder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SAMPLE_RATE = 48000; // SVCと同じ
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1; // モノラル
    private static final int BUFFER_SIZE = 1024; // 約21ms

    private TargetDataLine microphone;
    private Thread recordingThread;
    private volatile boolean recording = false;
    private AudioFormat currentFormat = null;

    private final Queue<byte[]> audioBuffer = new LinkedList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * 録音を開始する。
     */
    public void startRecording() {
        if (recording) {
            LOGGER.warn("[JavaMicrophoneRecorder] Already recording");
            return;
        }

        try {
            // SVCの設定からマイクデバイスを取得
            // 注: SVCDeviceManagerは現在正しく動作していないため、nullを渡してシステムデフォルトを使用
            Mixer.Info mixerInfo = null; // SVCDeviceManager.getMicrophoneDevice();
            Mixer mixer = null; // mixerInfo != null ? AudioSystem.getMixer(mixerInfo) : null;

            // サポートされているフォーマットを検出（システムデフォルトマイクを使用）
            AudioFormat format = findSupportedFormat(mixer);

            if (format == null) {
                LOGGER.error("[JavaMicrophoneRecorder] No supported audio format found");
                return;
            }

            LOGGER.info("[JavaMicrophoneRecorder] Using audio format: " + format);

            // 現在のフォーマットを保存
            currentFormat = format;

            // マイクを開く
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

            if (mixer != null) {
                microphone = (TargetDataLine) mixer.getLine(info);
            } else {
                LOGGER.warn("[JavaMicrophoneRecorder] No SVC device found, using default");
                microphone = AudioSystem.getTargetDataLine(format);
            }

            // バッファサイズをデフォルトにして、shared modeで開く
            // 大きすぎるバッファはマイクを占有してDiscordなどと競合する
            // スレッド優先度をMAXにすることでデータ欠落を防ぐ
            microphone.open(format);
            microphone.start();

            recording = true;

            // 録音スレッドを開始
            recordingThread = new Thread(this::recordingLoop, "MochiMobileOS-MicRecorder");
            recordingThread.setDaemon(true);
            // 優先度を上げてGCや他の処理より優先的に実行
            recordingThread.setPriority(Thread.MAX_PRIORITY);
            recordingThread.start();

            LOGGER.info("[JavaMicrophoneRecorder] Started recording");

        } catch (LineUnavailableException e) {
            LOGGER.error("[JavaMicrophoneRecorder] Failed to start recording", e);
            recording = false;
        }
    }

    /**
     * システムがサポートしているオーディオフォーマットを検出する。
     */
    private AudioFormat findSupportedFormat(Mixer mixer) {
        // 試すフォーマットのリスト（優先順位順）
        AudioFormat[] formats = {
            // 48kHz, 16-bit, mono (SVCと同じ)
            new AudioFormat(48000, 16, 1, true, false),
            // 44.1kHz, 16-bit, mono
            new AudioFormat(44100, 16, 1, true, false),
            // 16kHz, 16-bit, mono
            new AudioFormat(16000, 16, 1, true, false),
            // 8kHz, 16-bit, mono
            new AudioFormat(8000, 16, 1, true, false),
            // 48kHz, 16-bit, mono, big-endian
            new AudioFormat(48000, 16, 1, true, true),
            // 44.1kHz, 16-bit, mono, big-endian
            new AudioFormat(44100, 16, 1, true, true),
        };

        // まず指定されたMixerで試す（実際にLineを取得してみる）
        if (mixer != null) {
            for (AudioFormat format : formats) {
                DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                try {
                    // isLineSupportedではなく、実際にLineを取得してみる
                    TargetDataLine testLine = (TargetDataLine) mixer.getLine(info);
                    testLine.close(); // すぐに閉じる
                    LOGGER.info("[JavaMicrophoneRecorder] Found supported format on SVC mixer: " + format);
                    return format;
                } catch (LineUnavailableException e) {
                    LOGGER.debug("[JavaMicrophoneRecorder] Format not available on SVC mixer: " + format + " - " + e.getMessage());
                } catch (Exception e) {
                    LOGGER.debug("[JavaMicrophoneRecorder] Format not supported on SVC mixer: " + format + " - " + e.getMessage());
                }
            }
            LOGGER.warn("[JavaMicrophoneRecorder] SVC mixer does not support any input format, trying system default");
        }

        // Mixerでサポートされていない場合、システムデフォルトを試す
        for (AudioFormat format : formats) {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            try {
                if (AudioSystem.isLineSupported(info)) {
                    LOGGER.info("[JavaMicrophoneRecorder] Found supported format on system default: " + format);
                    return format;
                }
            } catch (Exception e) {
                LOGGER.debug("[JavaMicrophoneRecorder] Format not supported on system default: " + format);
            }
        }

        LOGGER.error("[JavaMicrophoneRecorder] No supported audio format found on any device");
        return null;
    }

    /**
     * 録音を停止する。
     */
    public void stopRecording() {
        if (!recording) {
            return;
        }

        recording = false;

        // 録音スレッドの終了を待つ
        if (recordingThread != null) {
            try {
                recordingThread.join(1000);
            } catch (InterruptedException e) {
                LOGGER.warn("[JavaMicrophoneRecorder] Interrupted while waiting for recording thread", e);
            }
        }

        // マイクを閉じる
        if (microphone != null) {
            microphone.stop();
            microphone.close();
            microphone = null;
        }

        LOGGER.info("[JavaMicrophoneRecorder] Stopped recording");
    }

    /**
     * 録音ループ（別スレッドで実行）
     */
    private void recordingLoop() {
        byte[] buffer = new byte[BUFFER_SIZE * 2]; // 16-bit = 2 bytes per sample

        while (recording) {
            try {
                int bytesRead = microphone.read(buffer, 0, buffer.length);

                if (bytesRead > 0) {
                    // SVCの音量設定を適用
                    float volume = SVCDeviceManager.getMicrophoneVolume();
                    float amplification = SVCDeviceManager.getMicrophoneAmplification();
                    float totalGain = volume * amplification;

                    byte[] audioData = new byte[bytesRead];
                    for (int i = 0; i < bytesRead; i += 2) {
                        // 16-bit PCMとして読み取り
                        short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));

                        // 音量を適用
                        sample = (short) (sample * totalGain);

                        // クリッピング防止
                        sample = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));

                        // バイト配列に書き戻し
                        audioData[i] = (byte) (sample & 0xFF);
                        audioData[i + 1] = (byte) ((sample >> 8) & 0xFF);
                    }

                    // バッファに追加
                    lock.writeLock().lock();
                    try {
                        audioBuffer.offer(audioData);

                        // バッファサイズを制限（最大1000パケット = 約20秒）
                        // ボイスメモ録音で長時間録音する可能性があるため、大きめに設定
                        while (audioBuffer.size() > 1000) {
                            audioBuffer.poll();
                        }
                    } finally {
                        lock.writeLock().unlock();
                    }
                }

            } catch (Exception e) {
                if (recording) {
                    LOGGER.error("[JavaMicrophoneRecorder] Error during recording", e);
                }
            }
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
     * 録音中かどうかを取得する。
     */
    public boolean isRecording() {
        return recording;
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
     * 現在のサンプリングレートを取得する。
     * @return サンプリングレート（Hz）、録音していない場合は0
     */
    public float getSampleRate() {
        return currentFormat != null ? currentFormat.getSampleRate() : 0;
    }
}
