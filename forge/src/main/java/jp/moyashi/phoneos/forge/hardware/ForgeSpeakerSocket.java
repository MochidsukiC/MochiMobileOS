package jp.moyashi.phoneos.forge.hardware;

import com.mojang.logging.LogUtils;
import jp.moyashi.phoneos.core.service.hardware.SpeakerSocket;
import org.slf4j.Logger;

/**
 * Forge環境用のスピーカーソケット実装。
 * Simple Voice Chat (SVC) MODがインストールされている場合のみ有効になる。
 * SVCが導入されていない場合は、isAvailable()がfalseを返す。
 *
 * 音量レベルに応じた動作（将来の実装）:
 * - OFF: 音声再生なし
 * - LOW: 自分だけに聞こえる
 * - MEDIUM: 周囲に聞こえる（中音量）
 * - HIGH: 遠くまで聞こえる（大音量）
 *
 * 将来的な拡張:
 * - SVCサウンドシステムとの連携
 * - 空間オーディオ再生（音量に応じた範囲）
 */
public class ForgeSpeakerSocket implements SpeakerSocket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private VolumeLevel volumeLevel;
    private final boolean svcAvailable;

    public ForgeSpeakerSocket() {
        this.volumeLevel = VolumeLevel.MEDIUM;
        this.svcAvailable = SVCDetector.isSVCAvailable();

        if (svcAvailable) {
            LOGGER.info("[ForgeSpeakerSocket] Initialized with SVC support");
        } else {
            LOGGER.info("[ForgeSpeakerSocket] Initialized without SVC (speaker unavailable)");
        }
    }

    @Override
    public boolean isAvailable() {
        // SVCが導入されている場合のみ利用可能
        return svcAvailable;
    }

    @Override
    public void setVolumeLevel(VolumeLevel level) {
        if (!svcAvailable) {
            LOGGER.warn("[ForgeSpeakerSocket] Cannot set volume - SVC not installed");
            return;
        }

        this.volumeLevel = level;
        LOGGER.info("[ForgeSpeakerSocket] Volume level set to " + level.name());

        // TODO: SVCの音量設定を変更するAPIを呼び出す
    }

    @Override
    public VolumeLevel getVolumeLevel() {
        return volumeLevel;
    }

    @Override
    public void playAudio(byte[] audioData) {
        if (!svcAvailable) {
            LOGGER.warn("[ForgeSpeakerSocket] Cannot play audio - SVC not installed");
            return;
        }

        if (audioData == null || audioData.length == 0) {
            LOGGER.warn("[ForgeSpeakerSocket] Cannot play audio - no data provided");
            return;
        }

        LOGGER.info("[ForgeSpeakerSocket] Playing audio with volume level " + volumeLevel);

        // TODO: SVCシステムを使用して音声を再生
        // - OFF: 何もしない
        // - LOW: 自分だけに再生
        // - MEDIUM/HIGH: 周囲に放送（SVCの範囲ベースサウンド）
        // 現時点では未実装（将来的な拡張ポイント）
    }

    @Override
    public void stopAudio() {
        if (!svcAvailable) {
            return;
        }

        LOGGER.info("[ForgeSpeakerSocket] Stopping audio");

        // TODO: 音声再生を停止
        // 現時点では未実装（将来的な拡張ポイント）
    }
}