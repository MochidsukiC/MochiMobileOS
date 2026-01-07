package jp.moyashi.phoneos.forge.audio;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

/**
 * Java標準APIを使用してマイクから音声を録音するクラス。
 * 選択されたマイクデバイス、またはシステムデフォルトを使用する。
 * ByteArrayOutputStreamを使用して連続バッファで安定した録音を実現。
 */
public class JavaMicrophoneRecorder {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1; // モノラル
    // 10msのチャンク時間（細かく読み取って連続バッファに追加）
    private static final int CHUNK_DURATION_MS = 10;
    // TargetDataLineの内部バッファサイズ（50ms分 - アンダーラン防止用）
    private static final int LINE_BUFFER_MS = 50;

    private TargetDataLine microphone;
    private Thread recordingThread;
    private volatile boolean recording = false;
    private AudioFormat currentFormat = null;
    // 実際のサンプリングレートに基づいて計算されるバッファサイズ
    private int dynamicBufferSize = 480; // デフォルト: 48kHz * 10ms

    // 選択されたマイクデバイス（nullの場合はシステムデフォルト）
    private Mixer.Info selectedMixerInfo = null;

    // 連続バッファ（ByteArrayOutputStream）
    private ByteArrayOutputStream audioBuffer = new ByteArrayOutputStream();
    private final Object bufferLock = new Object();

    // デバッグ用: 書き込み・読み込み追跡
    private long totalBytesWritten = 0;
    private long totalBytesRead = 0;

    /**
     * 使用するマイクデバイスを設定する。
     *
     * @param mixerInfo Mixer.Info、nullの場合はシステムデフォルト
     */
    public void setSelectedMixer(Mixer.Info mixerInfo) {
        this.selectedMixerInfo = mixerInfo;
        LOGGER.info("[JavaMicrophoneRecorder] Selected mixer: {}",
            mixerInfo != null ? mixerInfo.getName() : "System Default");
    }

    /**
     * 録音を開始する。
     */
    public void startRecording() {
        if (recording) {
            LOGGER.warn("[JavaMicrophoneRecorder] Already recording");
            return;
        }

        try {
            // 利用可能なミキサーからマイクを探す
            TargetDataLine foundLine = findWorkingMicrophone();

            if (foundLine == null) {
                LOGGER.error("[JavaMicrophoneRecorder] No working microphone found");
                return;
            }

            microphone = foundLine;
            microphone.start();

            // 実際のサンプリングレートに基づいてバッファサイズを計算
            // 20ms @ sampleRate, 16-bit mono = sampleRate * 0.020 samples
            int sampleRate = (int) currentFormat.getSampleRate();
            dynamicBufferSize = (sampleRate * CHUNK_DURATION_MS) / 1000;
            LOGGER.info("[JavaMicrophoneRecorder] Dynamic buffer size: {} samples ({} bytes) for {}Hz",
                dynamicBufferSize, dynamicBufferSize * 2, sampleRate);

            recording = true;

            // バッファとカウンタをクリア
            synchronized (bufferLock) {
                audioBuffer.reset();
                totalBytesWritten = 0;
                totalBytesRead = 0;
            }

            // 録音スレッドを開始
            recordingThread = new Thread(this::recordingLoop, "MochiMobileOS-MicRecorder");
            recordingThread.setDaemon(true);
            // 優先度を上げてGCや他の処理より優先的に実行
            recordingThread.setPriority(Thread.MAX_PRIORITY);
            recordingThread.start();

            LOGGER.info("[JavaMicrophoneRecorder] Started recording with format: " + currentFormat);

        } catch (Exception e) {
            LOGGER.error("[JavaMicrophoneRecorder] Failed to start recording", e);
            recording = false;
        }
    }

    /**
     * 選択されたマイク、またはシステムデフォルトからマイクラインを見つける。
     */
    private TargetDataLine findWorkingMicrophone() {
        // 試すフォーマットのリスト（優先順位順）
        AudioFormat[] formats = {
            new AudioFormat(48000, 16, 1, true, false),
            new AudioFormat(44100, 16, 1, true, false),
            new AudioFormat(16000, 16, 1, true, false),
        };

        // 選択されたミキサーがある場合、それを優先して試す
        if (selectedMixerInfo != null) {
            LOGGER.info("[JavaMicrophoneRecorder] Trying selected microphone: {}", selectedMixerInfo.getName());
            Mixer mixer = AudioSystem.getMixer(selectedMixerInfo);
            for (AudioFormat format : formats) {
                try {
                    DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format);
                    if (mixer.isLineSupported(lineInfo)) {
                        TargetDataLine line = (TargetDataLine) mixer.getLine(lineInfo);
                        // バッファサイズを指定してオープン（500ms分）
                        int bufferSize = (int) (format.getSampleRate() * format.getFrameSize() * LINE_BUFFER_MS / 1000);
                        line.open(format, bufferSize);
                        currentFormat = format;
                        LOGGER.info("[JavaMicrophoneRecorder] Successfully opened selected microphone: {} with format: {}, buffer: {} bytes",
                            selectedMixerInfo.getName(), format, bufferSize);
                        return line;
                    }
                } catch (Exception e) {
                    LOGGER.debug("[JavaMicrophoneRecorder] Selected mixer failed with format {}: {}",
                        format, e.getMessage());
                }
            }
            LOGGER.warn("[JavaMicrophoneRecorder] Selected microphone failed, falling back to system default");
        }

        // システムデフォルトマイクを試す
        LOGGER.info("[JavaMicrophoneRecorder] Trying system default microphone...");
        for (AudioFormat format : formats) {
            try {
                DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format);
                if (AudioSystem.isLineSupported(lineInfo)) {
                    TargetDataLine line = AudioSystem.getTargetDataLine(format);
                    // バッファサイズを指定してオープン（500ms分）
                    int bufferSize = (int) (format.getSampleRate() * format.getFrameSize() * LINE_BUFFER_MS / 1000);
                    line.open(format, bufferSize);
                    currentFormat = format;
                    LOGGER.info("[JavaMicrophoneRecorder] Successfully opened system default microphone with format: {}, buffer: {} bytes", format, bufferSize);
                    return line;
                }
            } catch (LineUnavailableException e) {
                LOGGER.debug("[JavaMicrophoneRecorder] System default failed with format " + format + ": " + e.getMessage());
            } catch (Exception e) {
                LOGGER.debug("[JavaMicrophoneRecorder] Error with system default: " + e.getMessage());
            }
        }

        // Primary Sound Capture Driverを探す
        LOGGER.info("[JavaMicrophoneRecorder] System default failed, trying Primary Sound Capture Driver...");
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixerInfos) {
            String name = mixerInfo.getName();
            if (name.contains("Primary Sound Capture Driver") || name.contains("既定のオーディオ")) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                for (AudioFormat format : formats) {
                    try {
                        DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format);
                        if (mixer.isLineSupported(lineInfo)) {
                            TargetDataLine line = (TargetDataLine) mixer.getLine(lineInfo);
                            int bufferSize = (int) (format.getSampleRate() * format.getFrameSize() * LINE_BUFFER_MS / 1000);
                            line.open(format, bufferSize);
                            currentFormat = format;
                            LOGGER.info("[JavaMicrophoneRecorder] Successfully opened: {} with format: {}, buffer: {} bytes", name, format, bufferSize);
                            return line;
                        }
                    } catch (Exception e) {
                        LOGGER.debug("[JavaMicrophoneRecorder] " + name + " failed: " + e.getMessage());
                    }
                }
            }
        }

        // フォールバック: 全ミキサーを試す
        LOGGER.info("[JavaMicrophoneRecorder] Trying all available mixers...");
        for (Mixer.Info mixerInfo : mixerInfos) {
            Mixer mixer = AudioSystem.getMixer(mixerInfo);
            Line.Info[] targetLineInfos = mixer.getTargetLineInfo();
            if (targetLineInfos.length == 0) {
                continue;
            }

            for (AudioFormat format : formats) {
                DataLine.Info lineInfo = new DataLine.Info(TargetDataLine.class, format);
                try {
                    if (mixer.isLineSupported(lineInfo)) {
                        TargetDataLine line = (TargetDataLine) mixer.getLine(lineInfo);
                        int bufferSize = (int) (format.getSampleRate() * format.getFrameSize() * LINE_BUFFER_MS / 1000);
                        line.open(format, bufferSize);
                        currentFormat = format;
                        LOGGER.info("[JavaMicrophoneRecorder] Successfully opened: {} with format: {}, buffer: {} bytes",
                            mixerInfo.getName(), format, bufferSize);
                        return line;
                    }
                } catch (Exception e) {
                    LOGGER.debug("[JavaMicrophoneRecorder] " + mixerInfo.getName() + " failed: " + e.getMessage());
                }
            }
        }

        LOGGER.error("[JavaMicrophoneRecorder] No working microphone found");
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
     * 連続的にマイクからデータを読み取り、ByteArrayOutputStreamに蓄積。
     */
    private void recordingLoop() {
        byte[] buffer = new byte[dynamicBufferSize * 2]; // 16-bit = 2 bytes per sample
        long lastLogTime = System.currentTimeMillis();
        int totalBytesInSecond = 0;
        int readCountInSecond = 0;

        while (recording) {
            try {
                int bytesRead = microphone.read(buffer, 0, buffer.length);

                // デバッグ: 1秒ごとに統計を出力
                totalBytesInSecond += bytesRead;
                readCountInSecond++;
                long now = System.currentTimeMillis();
                if (now - lastLogTime >= 1000) {
                    LOGGER.info("[JavaMicrophoneRecorder] Stats: {} bytes in {} reads over {}ms (expected ~96000 bytes)",
                        totalBytesInSecond, readCountInSecond, now - lastLogTime);
                    totalBytesInSecond = 0;
                    readCountInSecond = 0;
                    lastLogTime = now;
                }

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

                    // 連続バッファに追加
                    synchronized (bufferLock) {
                        audioBuffer.write(audioData, 0, audioData.length);
                        totalBytesWritten += audioData.length;

                        // バッファサイズを制限（最大30秒分 @ 48kHz = 約2.88MB）
                        // 超過した場合は古いデータを破棄
                        int maxSize = 48000 * 2 * 30; // 30秒分
                        if (audioBuffer.size() > maxSize) {
                            byte[] current = audioBuffer.toByteArray();
                            audioBuffer.reset();
                            // 後半のデータのみ保持
                            int keepSize = maxSize / 2;
                            audioBuffer.write(current, current.length - keepSize, keepSize);
                        }
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
     * 互換性のために残していますが、pollAllAudioData()の使用を推奨。
     *
     * @return 音声データ、バッファが空の場合はnull
     */
    public byte[] pollAudioData() {
        return pollAllAudioData();
    }

    /**
     * バッファ内の全音声データを取得してバッファをクリアする。
     * 連続バッファなのでデータ欠落なし。
     *
     * @return 蓄積された音声データ、バッファが空の場合はnull
     */
    public byte[] pollAllAudioData() {
        synchronized (bufferLock) {
            if (audioBuffer.size() == 0) {
                LOGGER.debug("[JavaMicrophoneRecorder] pollAllAudioData: buffer empty");
                return null;
            }

            byte[] result = audioBuffer.toByteArray();
            audioBuffer.reset();
            totalBytesRead += result.length;

            // 書き込みと読み込みの差分をチェック
            long diff = totalBytesWritten - totalBytesRead;
            LOGGER.info("[JavaMicrophoneRecorder] poll: {} bytes (written: {}, read: {}, diff: {})",
                result.length, totalBytesWritten, totalBytesRead, diff);
            return result;
        }
    }

    /**
     * 録音中かどうかを取得する。
     */
    public boolean isRecording() {
        return recording;
    }

    /**
     * バッファ内の音声データのバイト数を取得する。
     */
    public int getBufferSize() {
        synchronized (bufferLock) {
            return audioBuffer.size();
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
