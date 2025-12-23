package jp.moyashi.phoneos.core.media;

/**
 * MediaSessionを外部から制御するためのコントローラー。
 * コントロールセンターやシステムUIはMediaControllerを通じて再生操作を行う。
 *
 * MediaControllerはMediaSessionへの読み取り専用アクセスと、
 * コールバックを通じた再生操作を提供する。
 *
 * @since 2025-12-21
 * @version 1.0
 */
public class MediaController {

    /** 制御対象のMediaSession */
    private final MediaSession session;

    /**
     * MediaControllerを作成する。
     *
     * @param session 制御対象のMediaSession
     */
    public MediaController(MediaSession session) {
        this.session = session;
    }

    /**
     * 再生を開始する。
     */
    public void play() {
        MediaSessionCallback callback = session.getCallback();
        if (callback != null) {
            callback.onPlay();
        }
    }

    /**
     * 再生を一時停止する。
     */
    public void pause() {
        MediaSessionCallback callback = session.getCallback();
        if (callback != null) {
            callback.onPause();
        }
    }

    /**
     * 再生/一時停止をトグルする。
     */
    public void togglePlayPause() {
        if (session.getPlaybackState() == PlaybackState.PLAYING) {
            pause();
        } else {
            play();
        }
    }

    /**
     * 再生を停止する。
     */
    public void stop() {
        MediaSessionCallback callback = session.getCallback();
        if (callback != null) {
            callback.onStop();
        }
    }

    /**
     * 次のトラックへスキップする。
     */
    public void skipToNext() {
        MediaSessionCallback callback = session.getCallback();
        if (callback != null) {
            callback.onSkipToNext();
        }
    }

    /**
     * 前のトラックへスキップする。
     */
    public void skipToPrevious() {
        MediaSessionCallback callback = session.getCallback();
        if (callback != null) {
            callback.onSkipToPrevious();
        }
    }

    /**
     * 指定位置へシークする。
     *
     * @param positionMs シーク先の位置（ミリ秒）
     */
    public void seekTo(long positionMs) {
        MediaSessionCallback callback = session.getCallback();
        if (callback != null) {
            callback.onSeekTo(positionMs);
        }
    }

    /**
     * 高速巻き戻しを行う。
     */
    public void rewind() {
        MediaSessionCallback callback = session.getCallback();
        if (callback != null) {
            callback.onRewind();
        }
    }

    /**
     * 高速早送りを行う。
     */
    public void fastForward() {
        MediaSessionCallback callback = session.getCallback();
        if (callback != null) {
            callback.onFastForward();
        }
    }

    /**
     * 再生速度を変更する。
     *
     * @param speed 再生速度（1.0が通常速度）
     */
    public void setPlaybackSpeed(float speed) {
        MediaSessionCallback callback = session.getCallback();
        if (callback != null) {
            callback.onSetPlaybackSpeed(speed);
        }
    }

    /**
     * リピートモードを変更する。
     *
     * @param repeatMode リピートモード（0=オフ, 1=全曲リピート, 2=1曲リピート）
     */
    public void setRepeatMode(int repeatMode) {
        MediaSessionCallback callback = session.getCallback();
        if (callback != null) {
            callback.onSetRepeatMode(repeatMode);
        }
    }

    /**
     * シャッフルモードを変更する。
     *
     * @param enabled シャッフルを有効にする場合true
     */
    public void setShuffleMode(boolean enabled) {
        MediaSessionCallback callback = session.getCallback();
        if (callback != null) {
            callback.onSetShuffleMode(enabled);
        }
    }

    /**
     * 現在の再生状態を取得する。
     *
     * @return 再生状態
     */
    public PlaybackState getPlaybackState() {
        return session.getPlaybackState();
    }

    /**
     * メタデータを取得する。
     *
     * @return メタデータ
     */
    public MediaMetadata getMetadata() {
        return session.getMetadata();
    }

    /**
     * 現在の再生位置を取得する。
     *
     * @return 現在の再生位置（ミリ秒）
     */
    public long getCurrentPosition() {
        return session.getCurrentPosition();
    }

    /**
     * セッションIDを取得する。
     *
     * @return セッションID
     */
    public String getSessionId() {
        return session.getSessionId();
    }

    /**
     * アプリケーションIDを取得する。
     *
     * @return アプリケーションID
     */
    public String getApplicationId() {
        return session.getApplicationId();
    }

    /**
     * セッションがアクティブかどうかを確認する。
     *
     * @return アクティブな場合true
     */
    public boolean isActive() {
        return session.isActive();
    }

    /**
     * 再生中かどうかを確認する。
     *
     * @return 再生中の場合true
     */
    public boolean isPlaying() {
        return session.getPlaybackState() == PlaybackState.PLAYING;
    }

    @Override
    public String toString() {
        return "MediaController{" +
                "sessionId='" + session.getSessionId() + '\'' +
                ", state=" + session.getPlaybackState() +
                '}';
    }
}
