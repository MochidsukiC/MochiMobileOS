package jp.moyashi.phoneos.api.hardware;

/**
 * スピーカーAPI。
 * プロパティに3段階の音量を持つ。OS側から音声出力の要求があった場合、ストリーム形式で音声をバイパスする。
 */
public interface SpeakerSocket {
    /**
     * 音量レベル定義。
     */
    enum VolumeLevel {
        OFF(0),     // 音量0（ミュート）
        LOW(1),     // 低音量（自分だけ聞こえる）
        MEDIUM(2),  // 中音量（周囲に聞こえる）
        HIGH(3);    // 高音量（遠くまで聞こえる）

        private final int level;

        VolumeLevel(int level) {
            this.level = level;
        }

        public int getLevel() {
            return level;
        }
    }

    /**
     * スピーカーが利用可能かどうかを取得する。
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();

    /**
     * 音量レベルを設定する。
     *
     * @param level 音量レベル
     */
    void setVolumeLevel(VolumeLevel level);

    /**
     * 音量レベルを取得する。
     *
     * @return 現在の音量レベル
     */
    VolumeLevel getVolumeLevel();

    /**
     * 音声を再生する（ストリーム形式）。
     *
     * @param audioData 音声データ
     */
    void playAudio(byte[] audioData);

    /**
     * 音声再生を停止する。
     */
    void stopAudio();
}
