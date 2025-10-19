package jp.moyashi.phoneos.core.service.hardware;

/**
 * マイクAPI。
 * オンオフのプロパティを持ち、オンの際はストリーム形式で音声をバイパスする。
 *
 * 3つの独立したチャンネルを提供：
 * - チャンネル1: 自分のマイク入力
 * - チャンネル2: 他プレイヤーのVC音声
 * - チャンネル3: Minecraft環境音
 *
 * ミキシングはOS側で行う。
 *
 * 環境別動作:
 * - standalone: nullを返す
 * - forge-mod: SVCのマイク入力と環境音をバイパス
 */
public interface MicrophoneSocket {
    /**
     * マイクが利用可能かどうかを取得する。
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();

    /**
     * マイクを有効化する。
     *
     * @param enabled 有効にする場合true
     */
    void setEnabled(boolean enabled);

    /**
     * マイクが有効かどうかを取得する。
     *
     * @return 有効な場合true
     */
    boolean isEnabled();

    /**
     * 音声データを取得する（ストリーム形式）。
     * 全チャンネルをミックスした音声を返す（後方互換性のため）。
     *
     * @return 音声データ、利用不可の場合null
     * @deprecated チャンネル別メソッドを使用してください
     */
    @Deprecated
    byte[] getAudioData();

    /**
     * チャンネル1: 自分のマイク入力音声データを取得する。
     *
     * @return 音声データ、利用不可の場合null
     */
    default byte[] getMicrophoneAudio() {
        return null;
    }

    /**
     * チャンネル2: 他プレイヤーのVC音声データを取得する。
     *
     * @return 音声データ、利用不可の場合null
     */
    default byte[] getVoicechatAudio() {
        return null;
    }

    /**
     * チャンネル3: Minecraft環境音データを取得する。
     *
     * @return 音声データ、利用不可の場合null
     */
    default byte[] getEnvironmentAudio() {
        return null;
    }

    /**
     * 現在のサンプリングレートを取得する。
     *
     * @return サンプリングレート（Hz）、デフォルトは48000
     */
    default float getSampleRate() {
        return 48000.0f;
    }
}