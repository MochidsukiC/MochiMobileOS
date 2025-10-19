package jp.moyashi.phoneos.forge.audio;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 複数の音声ソースをミックスするクラス。
 * 自分のマイク入力と他プレイヤーからの音声を1つのストリームに合成する。
 */
public class AudioMixer {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final JavaMicrophoneRecorder microphoneRecorder;
    private final VoicechatAudioCapture audioCapture;

    public AudioMixer(JavaMicrophoneRecorder microphoneRecorder, VoicechatAudioCapture audioCapture) {
        this.microphoneRecorder = microphoneRecorder;
        this.audioCapture = audioCapture;
    }

    /**
     * 音声ミキシングを開始する。
     */
    public void startMixing() {
        microphoneRecorder.startRecording();
        audioCapture.startCapture();
        LOGGER.info("[AudioMixer] Started mixing audio");
    }

    /**
     * 音声ミキシングを停止する。
     */
    public void stopMixing() {
        microphoneRecorder.stopRecording();
        audioCapture.stopCapture();
        LOGGER.info("[AudioMixer] Stopped mixing audio");
    }

    /**
     * ミックスされた音声データを取得する。
     * 自分のマイク入力と他プレイヤーの音声を合成して返す。
     *
     * @return ミックスされた音声データ、データがない場合はnull
     */
    public byte[] getMixedAudio() {
        byte[] micData = microphoneRecorder.pollAudioData();
        byte[] vcData = audioCapture.pollAudioData();

        // どちらかのデータがない場合
        if (micData == null && vcData == null) {
            return null;
        }

        if (micData == null) {
            return vcData;
        }

        if (vcData == null) {
            return micData;
        }

        // 両方のデータがある場合、ミックスする
        return mixAudio(micData, vcData);
    }

    /**
     * 2つの音声データをミックスする。
     *
     * @param audio1 1つ目の音声データ
     * @param audio2 2つ目の音声データ
     * @return ミックスされた音声データ
     */
    private byte[] mixAudio(byte[] audio1, byte[] audio2) {
        // 短い方の長さに合わせる
        int length = Math.min(audio1.length, audio2.length);
        byte[] mixed = new byte[length];

        for (int i = 0; i < length; i += 2) {
            // 16-bit PCMとして読み取り
            short sample1 = (short) ((audio1[i + 1] << 8) | (audio1[i] & 0xFF));
            short sample2 = (short) ((audio2[i + 1] << 8) | (audio2[i] & 0xFF));

            // サンプルを加算（平均ではなく加算でミックス）
            int mixedSample = sample1 + sample2;

            // クリッピング防止（-32768 〜 32767の範囲に収める）
            mixedSample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixedSample));

            // バイト配列に書き戻し
            mixed[i] = (byte) (mixedSample & 0xFF);
            mixed[i + 1] = (byte) ((mixedSample >> 8) & 0xFF);
        }

        return mixed;
    }

    /**
     * ミキシング中かどうかを取得する。
     */
    public boolean isMixing() {
        return microphoneRecorder.isRecording() || audioCapture.isCapturing();
    }

    /**
     * バッファ内の音声データ数を取得する（デバッグ用）。
     */
    public int getTotalBufferSize() {
        return microphoneRecorder.getBufferSize() + audioCapture.getBufferSize();
    }

    /**
     * バッファ内の全てのミックスされた音声データを取得して連結する。
     * ボイスメモ録音など、データの欠落を防ぎたい場合に使用する。
     *
     * @return 連結されたミックス音声データ、データがない場合はnull
     */
    public byte[] getAllMixedAudio() {
        // 各バッファから全データを取得
        byte[] micData = microphoneRecorder.pollAllAudioData();
        byte[] vcData = audioCapture.pollAllAudioData();

        // どちらかのデータがない場合
        if (micData == null && vcData == null) {
            return null;
        }

        if (micData == null) {
            return vcData;
        }

        if (vcData == null) {
            return micData;
        }

        // 両方のデータがある場合、ミックスする
        // 長い方の長さに合わせる
        int maxLength = Math.max(micData.length, vcData.length);
        byte[] mixed = new byte[maxLength];

        for (int i = 0; i < maxLength; i += 2) {
            short sample1 = 0;
            short sample2 = 0;

            // micDataから読み取り
            if (i + 1 < micData.length) {
                sample1 = (short) ((micData[i + 1] << 8) | (micData[i] & 0xFF));
            }

            // vcDataから読み取り
            if (i + 1 < vcData.length) {
                sample2 = (short) ((vcData[i + 1] << 8) | (vcData[i] & 0xFF));
            }

            // サンプルを加算（平均ではなく加算でミックス）
            int mixedSample = sample1 + sample2;

            // クリッピング防止（-32768 〜 32767の範囲に収める）
            mixedSample = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, mixedSample));

            // バイト配列に書き戻し
            mixed[i] = (byte) (mixedSample & 0xFF);
            mixed[i + 1] = (byte) ((mixedSample >> 8) & 0xFF);
        }

        return mixed;
    }

    /**
     * 現在のサンプリングレートを取得する。
     * マイクのサンプリングレートを返す。
     * @return サンプリングレート（Hz）、録音していない場合は48000（デフォルト）
     */
    public float getSampleRate() {
        float rate = microphoneRecorder.getSampleRate();
        return rate > 0 ? rate : 48000.0f; // デフォルトは48kHz（SVC標準）
    }
}
