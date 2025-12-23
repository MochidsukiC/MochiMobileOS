package jp.moyashi.phoneos.core.media;

/**
 * 外部からの再生コントロール要求を受け取るコールバックインターフェース。
 * アプリケーションはこのインターフェースを実装してMediaSessionに登録することで、
 * コントロールセンターや他のシステムUIからの再生操作を受け取ることができる。
 *
 * @since 2025-12-21
 * @version 1.0
 */
public interface MediaSessionCallback {

    /**
     * 再生開始の要求を受け取る。
     * 一時停止状態または停止状態から再生を開始する。
     */
    void onPlay();

    /**
     * 一時停止の要求を受け取る。
     * 再生を一時的に中断し、後で再開可能な状態にする。
     */
    void onPause();

    /**
     * 再生停止の要求を受け取る。
     * 再生を完全に停止し、リソースを解放する。
     */
    void onStop();

    /**
     * 次のトラックへスキップする要求を受け取る。
     * プレイリストの次の曲に移動する。
     * デフォルトでは何もしない（単一トラック再生の場合）。
     */
    default void onSkipToNext() {
        // デフォルト実装：何もしない
    }

    /**
     * 前のトラックへスキップする要求を受け取る。
     * プレイリストの前の曲に移動する。
     * デフォルトでは何もしない（単一トラック再生の場合）。
     */
    default void onSkipToPrevious() {
        // デフォルト実装：何もしない
    }

    /**
     * シーク（位置移動）の要求を受け取る。
     * 指定された位置へ再生位置を移動する。
     *
     * @param positionMs 移動先の再生位置（ミリ秒）
     */
    default void onSeekTo(long positionMs) {
        // デフォルト実装：何もしない
    }

    /**
     * 高速巻き戻しの要求を受け取る。
     * 現在位置から数秒前に戻る。
     * デフォルトでは10秒巻き戻しとしてonSeekToを呼び出す。
     */
    default void onRewind() {
        // デフォルト実装：何もしない
    }

    /**
     * 高速早送りの要求を受け取る。
     * 現在位置から数秒先に進む。
     * デフォルトでは10秒早送りとしてonSeekToを呼び出す。
     */
    default void onFastForward() {
        // デフォルト実装：何もしない
    }

    /**
     * 再生速度変更の要求を受け取る。
     *
     * @param speed 再生速度（1.0が通常速度）
     */
    default void onSetPlaybackSpeed(float speed) {
        // デフォルト実装：何もしない
    }

    /**
     * リピートモード変更の要求を受け取る。
     *
     * @param repeatMode リピートモード（0=オフ, 1=全曲リピート, 2=1曲リピート）
     */
    default void onSetRepeatMode(int repeatMode) {
        // デフォルト実装：何もしない
    }

    /**
     * シャッフルモード変更の要求を受け取る。
     *
     * @param enabled シャッフルを有効にする場合true
     */
    default void onSetShuffleMode(boolean enabled) {
        // デフォルト実装：何もしない
    }
}
