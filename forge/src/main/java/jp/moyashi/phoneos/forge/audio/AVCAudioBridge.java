package jp.moyashi.phoneos.forge.audio;

import com.mojang.logging.LogUtils;
import jp.moyashi.phoneos.forge.hardware.AVCDetector;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AdvancedVC 2.0 とのオーディオブリッジ。
 *
 * 正しい接続方向：
 * - ForgeSpeakerSocket.playAudio() (MEDIUM/HIGH) → AVC AudioInputProvider
 *   → 他プレイヤーにYouTube等の音声を送信
 * - AVC AudioOutputListener → ForgeMicrophoneSocket.getVoicechatAudio() (Ch2)
 *   → 他プレイヤーの声を受信
 *
 * 音声フォーマット:
 * - サンプルレート: 48kHz
 * - ビット深度: 16-bit signed (Little Endian)
 * - AVC入力: モノラル (5760バイト = 60ms)
 * - AVC出力: ステレオ (11520バイト = 60ms)
 */
public class AVCAudioBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PROVIDER_ID = "mochimobileos";

    private static AVCAudioBridge instance;

    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean enabled = new AtomicBoolean(false);

    // スピーカー音声 → AVC入力用バッファ（MEDIUM/HIGHの音声をAVCに送信）
    // リングバッファを使用（約1秒分確保: 5760 * 16 = 92160 bytes）
    private static final int AVC_FRAME_SIZE = 5760; // AVCが期待するフレームサイズ（60ms @ 48kHz 16bit mono）
    private final ByteRingBuffer speakerRingBuffer = new ByteRingBuffer(AVC_FRAME_SIZE * 16);

    // AVC出力 → マイクCh2用バッファ（他プレイヤーの声を保持）
    private final ConcurrentLinkedQueue<byte[]> voicechatAudioBuffer = new ConcurrentLinkedQueue<>();
    private static final int MAX_BUFFER_SIZE = 1500; // Queueのサイズ制限

    private AVCAudioBridge() {
    }

    public static synchronized AVCAudioBridge getInstance() {
        if (instance == null) {
            instance = new AVCAudioBridge();
        }
        return instance;
    }

    /**
     * AVCオーディオブリッジを初期化する。
     * AVCが利用可能な場合のみ、APIに登録を行う。
     * VoiceClientが起動していない場合は初期化を延期する。
     *
     * @return 初期化に成功した場合true、VoiceClient未起動などで延期した場合false
     */
    public boolean initialize() {
        if (initialized.get()) {
            return true;
        }

        if (!AVCDetector.isAVCAvailable()) {
            LOGGER.info("[AVCAudioBridge] AdvancedVC not available - bridge disabled");
            return false;
        }

        try {
            // リフレクションを使用してAVCのAPIにアクセス
            // これにより、AVCがない環境でもコンパイル可能
            if (registerWithAVC()) {
                initialized.set(true);
                LOGGER.info("[AVCAudioBridge] Successfully registered with AdvancedVC");
                return true;
            } else {
                LOGGER.debug("[AVCAudioBridge] VoiceClient not ready yet - will retry later");
                return false;
            }
        } catch (Exception e) {
            LOGGER.error("[AVCAudioBridge] Failed to register with AdvancedVC", e);
            return false;
        }
    }

    /**
     * 遅延初期化を試行する。
     * VoiceClientが利用可能になっている場合にのみ初期化を行う。
     *
     * @return 初期化済みまたは初期化成功した場合true
     */
    public boolean tryInitialize() {
        if (initialized.get()) {
            return true;
        }
        return initialize();
    }

    /**
     * AVCのAPIに登録する。
     * リフレクションを使用してAVCのクラスにアクセスする。
     *
     * @return 登録に成功した場合true、VoiceClientが未起動の場合false
     */
    private boolean registerWithAVC() throws Exception {
        // VoiceClientクラスを取得
        Class<?> voiceClientClass = Class.forName(
            "jp.houlab.mochidsuki.advancedvc2_0.client.VoiceClient"
        );

        // getInstance()メソッドを取得
        java.lang.reflect.Method getInstanceMethod = voiceClientClass.getMethod("getInstance");
        Object voiceClient = getInstanceMethod.invoke(null);

        // VoiceClientがまだ起動していない場合はfalseを返す
        if (voiceClient == null) {
            LOGGER.debug("[AVCAudioBridge] VoiceClient.getInstance() returned null - not ready yet");
            return false;
        }

        // AudioInputProviderインターフェースを取得
        Class<?> audioInputProviderClass = Class.forName(
            "jp.houlab.mochidsuki.advancedvc2_0.client.api.AudioInputProvider"
        );

        // AudioOutputListenerインターフェースを取得
        Class<?> audioOutputListenerClass = Class.forName(
            "jp.houlab.mochidsuki.advancedvc2_0.client.api.AudioOutputListener"
        );

        // AudioInputProviderのプロキシを作成（スピーカー音声 → AVC）
        Object inputProvider = java.lang.reflect.Proxy.newProxyInstance(
            audioInputProviderClass.getClassLoader(),
            new Class<?>[] { audioInputProviderClass },
            (proxy, method, args) -> {
                if ("pollAudioFrame".equals(method.getName())) {
                    return pollSpeakerFrame();
                }
                return null;
            }
        );

        // AudioOutputListenerのプロキシを作成（AVC → マイクCh2）
        Object outputListener = java.lang.reflect.Proxy.newProxyInstance(
            audioOutputListenerClass.getClassLoader(),
            new Class<?>[] { audioOutputListenerClass },
            (proxy, method, args) -> {
                if ("onAudioFrame".equals(method.getName()) && args.length > 0) {
                    onAVCAudioReceived((byte[]) args[0]);
                }
                return null;
            }
        );

        // registerAudioInputProvider(String, AudioInputProvider)を呼び出し
        java.lang.reflect.Method registerInputMethod = voiceClientClass.getMethod(
            "registerAudioInputProvider", String.class, audioInputProviderClass
        );
        registerInputMethod.invoke(voiceClient, PROVIDER_ID, inputProvider);
        LOGGER.info("[AVCAudioBridge] Registered AudioInputProvider with AVC (Speaker → AVC)");

        // registerOutputListener(String, AudioOutputListener)を呼び出し
        java.lang.reflect.Method registerOutputMethod = voiceClientClass.getMethod(
            "registerOutputListener", String.class, audioOutputListenerClass
        );
        registerOutputMethod.invoke(voiceClient, PROVIDER_ID, outputListener);
        LOGGER.info("[AVCAudioBridge] Registered AudioOutputListener with AVC (AVC → Mic Ch2)");

        return true;
    }

    /**
     * AVCから登録を解除する。
     */
    public void shutdown() {
        if (!initialized.get() || !AVCDetector.isAVCAvailable()) {
            return;
        }

        try {
            Class<?> voiceClientClass = Class.forName(
                "jp.houlab.mochidsuki.advancedvc2_0.client.VoiceClient"
            );
            java.lang.reflect.Method getInstanceMethod = voiceClientClass.getMethod("getInstance");
            Object voiceClient = getInstanceMethod.invoke(null);

            // unregisterAudioInputProvider(String)
            java.lang.reflect.Method unregisterInputMethod = voiceClientClass.getMethod(
                "unregisterAudioInputProvider", String.class
            );
            unregisterInputMethod.invoke(voiceClient, PROVIDER_ID);

            // unregisterOutputListener(String)
            java.lang.reflect.Method unregisterOutputMethod = voiceClientClass.getMethod(
                "unregisterOutputListener", String.class
            );
            unregisterOutputMethod.invoke(voiceClient, PROVIDER_ID);

            LOGGER.info("[AVCAudioBridge] Unregistered from AdvancedVC");
        } catch (Exception e) {
            LOGGER.error("[AVCAudioBridge] Failed to unregister from AdvancedVC", e);
        }

        initialized.set(false);
        enabled.set(false);
    }

    /**
     * ブリッジを有効/無効にする。
     *
     * @param enabled 有効にする場合true
     */
    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        LOGGER.info("[AVCAudioBridge] Bridge {}", enabled ? "enabled" : "disabled");
    }

    /**
     * ブリッジが有効かどうかを取得する。
     *
     * @return 有効な場合true
     */
    public boolean isEnabled() {
        return enabled.get() && initialized.get();
    }

    /**
     * スピーカー音声をAVCに送信するためにバッファに追加する。
     * ForgeSpeakerSocket.playAudio()からMEDIUM/HIGH時に呼び出される。
     *
     * @param audioData 音声データ（48kHz, 16bit LE, モノラル）
     */
    public void submitSpeakerAudio(byte[] audioData) {
        if (!isEnabled() || audioData == null || audioData.length == 0) {
            return;
        }

        int written = speakerRingBuffer.write(audioData, 0, audioData.length);
        
        if (written < audioData.length) {
            LOGGER.debug("[AVCAudioBridge] Buffer full, dropped {} bytes", audioData.length - written);
        }
    }

    /**
     * AVCのAudioInputProviderから呼び出される。
     * スピーカー音声のフレームを返す。
     * AVCがマイク音声とのミキシングを自動的に行う。
     *
     * リングバッファから5760バイト（60ms）のフレームを取得する。
     * データ不足時はnullを返し、AVC側で無音処理させる。
     *
     * @return PCMデータ（48kHz, 16bit LE, モノラル, 5760バイト）、データが不足している場合null
     */
    private byte[] pollSpeakerFrame() {
        if (!isEnabled()) {
            return null;
        }

        // 1フレーム分のデータが溜まっているか確認
        if (speakerRingBuffer.available() >= AVC_FRAME_SIZE) {
            byte[] frame = new byte[AVC_FRAME_SIZE];
            speakerRingBuffer.read(frame, 0, AVC_FRAME_SIZE);
            return frame;
        }

        // データ不足
        return null;
    }

    /**
     * AVCのAudioOutputListenerから呼び出される。
     * AVCの最終出力（他プレイヤーの声）をVC音声バッファに保存する。
     *
     * @param pcmData PCMデータ（48kHz, 16bit LE, ステレオ, 11520バイト）
     */
    private void onAVCAudioReceived(byte[] pcmData) {
        if (!isEnabled() || pcmData == null || pcmData.length == 0) {
            return;
        }

        // バッファがいっぱいの場合は古いデータを破棄
        while (voicechatAudioBuffer.size() >= MAX_BUFFER_SIZE) {
            voicechatAudioBuffer.poll();
        }

        // ステレオ→モノラルに変換して保存
        byte[] monoData = stereoToMono(pcmData);
        voicechatAudioBuffer.offer(monoData);
    }

    /**
     * VC音声データを取得する。
     * ForgeMicrophoneSocket.getVoicechatAudio()から呼び出される。
     *
     * @return 音声データ（48kHz, 16bit LE, モノラル）、データがない場合null
     */
    public byte[] pollVoicechatAudio() {
        if (!isEnabled()) {
            return null;
        }

        return voicechatAudioBuffer.poll();
    }

    /**
     * すべてのVC音声データを取得する。
     * ForgeMicrophoneSocket.getVoicechatAudio()から呼び出される。
     *
     * @return 連結された音声データ、データがない場合null
     */
    public byte[] pollAllVoicechatAudio() {
        if (!isEnabled() || voicechatAudioBuffer.isEmpty()) {
            return null;
        }

        // すべてのチャンクを連結
        int totalSize = 0;
        for (byte[] chunk : voicechatAudioBuffer) {
            totalSize += chunk.length;
        }

        if (totalSize == 0) {
            return null;
        }

        byte[] result = new byte[totalSize];
        int offset = 0;
        byte[] chunk;
        while ((chunk = voicechatAudioBuffer.poll()) != null) {
            System.arraycopy(chunk, 0, result, offset, chunk.length);
            offset += chunk.length;
        }

        return result;
    }

    /**
     * ステレオPCMをモノラルPCMに変換する。
     * 左右チャンネルを平均化。
     *
     * @param stereoData ステレオPCMデータ（インターリーブ形式）
     * @return モノラルPCMデータ
     */
    private byte[] stereoToMono(byte[] stereoData) {
        byte[] monoData = new byte[stereoData.length / 2];

        for (int i = 0; i < monoData.length / 2; i++) {
            int stereoOffset = i * 4;
            int monoOffset = i * 2;

            // 左チャンネル
            int left = (stereoData[stereoOffset] & 0xFF) | (stereoData[stereoOffset + 1] << 8);
            // 右チャンネル
            int right = (stereoData[stereoOffset + 2] & 0xFF) | (stereoData[stereoOffset + 3] << 8);

            // 平均化
            int mono = (left + right) / 2;

            monoData[monoOffset] = (byte) (mono & 0xFF);
            monoData[monoOffset + 1] = (byte) ((mono >> 8) & 0xFF);
        }

        return monoData;
    }

    /**
     * ブリッジが初期化されているかどうかを取得する。
     *
     * @return 初期化済みの場合true
     */
    public boolean isInitialized() {
        return initialized.get();
    }
}
