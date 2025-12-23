package jp.moyashi.phoneos.core.media;

/**
 * メディアの再生状態を表す列挙型。
 * 各アプリケーションのメディア再生状態を統一的に管理するために使用する。
 *
 * @since 2025-12-21
 * @version 1.0
 */
public enum PlaybackState {

    /**
     * 停止状態。
     * メディアが読み込まれていないか、再生が完全に停止している状態。
     */
    STOPPED,

    /**
     * 再生中。
     * メディアがアクティブに再生されている状態。
     */
    PLAYING,

    /**
     * 一時停止中。
     * 再生が一時的に中断されている状態。再開可能。
     */
    PAUSED,

    /**
     * バッファリング中。
     * ストリーミングメディアがデータを読み込んでいる状態。
     */
    BUFFERING,

    /**
     * エラー状態。
     * 再生中にエラーが発生した状態。
     */
    ERROR;

    /**
     * 再生中またはバッファリング中かどうかを判定する。
     *
     * @return アクティブに再生処理中の場合true
     */
    public boolean isActive() {
        return this == PLAYING || this == BUFFERING;
    }

    /**
     * 再生可能な状態かどうかを判定する。
     *
     * @return 再生開始が可能な場合true
     */
    public boolean canPlay() {
        return this == STOPPED || this == PAUSED;
    }

    /**
     * 一時停止可能な状態かどうかを判定する。
     *
     * @return 一時停止が可能な場合true
     */
    public boolean canPause() {
        return this == PLAYING;
    }
}
