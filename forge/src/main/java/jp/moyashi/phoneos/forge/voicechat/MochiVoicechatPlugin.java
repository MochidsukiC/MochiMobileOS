package jp.moyashi.phoneos.forge.voicechat;

import com.mojang.logging.LogUtils;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.MergeClientSoundEvent;
import jp.moyashi.phoneos.forge.audio.SVCDeviceManager;
import jp.moyashi.phoneos.forge.audio.VoicechatAudioCapture;
import org.slf4j.Logger;

/**
 * MochiMobileOS Voicechat Plugin.
 * Simple Voice ChatのAPIと統合し、音声録音機能を提供する。
 */
@ForgeVoicechatPlugin
public class MochiVoicechatPlugin implements VoicechatPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final String PLUGIN_ID = "mochimobileos_voicechat";

    private static VoicechatApi api;
    private static VoicechatClientApi clientApi;

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        LOGGER.info("[MochiVoicechatPlugin] Initializing plugin...");
        MochiVoicechatPlugin.api = api;

        // クライアント側の場合、VoicechatClientApiとして保存
        if (api instanceof VoicechatClientApi) {
            clientApi = (VoicechatClientApi) api;
            LOGGER.info("[MochiVoicechatPlugin] Client API initialized");

            // SVCDeviceManagerを初期化
            SVCDeviceManager.initialize(clientApi);
        }

        LOGGER.info("[MochiVoicechatPlugin] Plugin initialized");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        LOGGER.info("[MochiVoicechatPlugin] Registering events...");

        // クライアント側の初期化イベント（VoicechatClientApiを取得）
        registration.registerEvent(ClientVoicechatInitializationEvent.class, this::onClientInit);

        // クライアント側のイベント登録
        registration.registerEvent(ClientReceiveSoundEvent.class, this::onClientReceiveSound);

        // 音声ミックスイベント（すべての音声を確実にキャプチャ）
        registration.registerEvent(MergeClientSoundEvent.class, this::onMergeClientSound);

        LOGGER.info("[MochiVoicechatPlugin] Events registered");
    }

    /**
     * クライアント側の初期化イベントハンドラー。
     * VoicechatClientApiを取得して保存する。
     */
    private void onClientInit(ClientVoicechatInitializationEvent event) {
        LOGGER.info("[MochiVoicechatPlugin] Client initialization event received");
        clientApi = event.getVoicechat();
        LOGGER.info("[MochiVoicechatPlugin] Client API initialized from event");

        // SVCDeviceManagerを初期化
        SVCDeviceManager.initialize(clientApi);
        LOGGER.info("[MochiVoicechatPlugin] SVCDeviceManager initialized");
    }

    /**
     * クライアントが音声を受信したときのイベントハンドラー。
     * 他のプレイヤーからの音声データをキャプチャする。
     */
    private void onClientReceiveSound(ClientReceiveSoundEvent event) {
        // whisperingフラグはEntitySoundの場合のみ取得可能
        boolean whispering = false;
        if (event instanceof ClientReceiveSoundEvent.EntitySound) {
            whispering = ((ClientReceiveSoundEvent.EntitySound) event).isWhispering();
        }

        short[] audioData = event.getRawAudio();
        LOGGER.info("[MochiVoicechatPlugin] ClientReceiveSoundEvent - ID: " + event.getId() +
                    ", audioLength: " + (audioData != null ? audioData.length : 0) +
                    ", whispering: " + whispering);

        // VoicechatAudioCaptureに音声データを渡す
        VoicechatAudioCapture.getInstance().onAudioReceived(
            event.getId(),           // 送信者のUUID
            audioData,              // 音声データ（short[]）
            whispering               // ささやきモードかどうか
        );
    }

    /**
     * 音声がミックスされる前のイベントハンドラー。
     * すべての音声（他プレイヤー+環境音など）を確実にキャプチャする。
     */
    private void onMergeClientSound(MergeClientSoundEvent event) {
        // このイベントではmergeAudio(short[])を呼ぶことで音声データを取得できません
        // 代わりに、このイベントが発火したことをログに記録
        LOGGER.info("[MochiVoicechatPlugin] MergeClientSoundEvent fired");
    }

    /**
     * Voicechat APIのインスタンスを取得する。
     */
    public static VoicechatApi getApi() {
        return api;
    }

    /**
     * Voicechat Client APIのインスタンスを取得する。
     * initialize()で設定されたクライアントAPIを返す。
     */
    public static VoicechatClientApi getClientApi() {
        return clientApi;
    }

    /**
     * プラグインがロードされているかチェックする。
     */
    public static boolean isLoaded() {
        return api != null;
    }
}
