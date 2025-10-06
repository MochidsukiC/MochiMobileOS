package jp.moyashi.phoneos.core.service.hardware;

/**
 * マイクAPI。
 * オンオフのプロパティを持ち、オンの際はストリーム形式で音声をバイパスする。
 *
 * 環境別動作:
 * - standalone: nullを返す
 * - forge-mod: SVCのマイク入力をバイパス
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
     *
     * @return 音声データ、利用不可の場合null
     */
    byte[] getAudioData();

    /**
     * 現在のサンプリングレートを取得する。
     *
     * @return サンプリングレート（Hz）、デフォルトは48000
     */
    default float getSampleRate() {
        return 48000.0f;
    }
}