package jp.moyashi.phoneos.api.hardware;

/**
 * マイクAPI。
 * オンオフのプロパティを持ち、オンの際はストリーム形式で音声をバイパスする。
 *
 * 3つの独立したチャンネルを提供：
 * - チャンネル1: 自分のマイク入力
 * - チャンネル2: 他プレイヤーのVC音声
 * - チャンネル3: Minecraft環境音
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
     * チャンネル1: 自分のマイク入力音声データを取得する。
     *
     * @return 音声データ、利用不可の場合null
     */
    byte[] getMicrophoneAudio();

    /**
     * チャンネル2: 他プレイヤーのVC音声データを取得する。
     *
     * @return 音声データ、利用不可の場合null
     */
    byte[] getVoicechatAudio();

    /**
     * チャンネル3: Minecraft環境音データを取得する。
     *
     * @return 音声データ、利用不可の場合null
     */
    byte[] getEnvironmentAudio();

    /**
     * 現在のサンプリングレートを取得する。
     *
     * @return サンプリングレート（Hz）、デフォルトは48000
     */
    float getSampleRate();
}
