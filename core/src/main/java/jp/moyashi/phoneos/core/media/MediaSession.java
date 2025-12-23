package jp.moyashi.phoneos.core.media;

/**
 * アプリケーションがメディア再生状態を登録するためのセッション。
 * AndroidのMediaSessionに相当する機能を提供する。
 *
 * アプリケーションはMediaSessionを作成し、再生状態やメタデータを更新することで、
 * システム全体でメディア再生状態を共有できる。コントロールセンターや通知センター
 * からの再生操作は、登録されたコールバックを通じて受け取る。
 *
 * 使用例:
 * <pre>
 * MediaSession session = mediaSessionManager.createSession(applicationId);
 * session.setCallback(new MediaSessionCallback() {
 *     public void onPlay() { startPlayback(); }
 *     public void onPause() { pausePlayback(); }
 *     public void onStop() { stopPlayback(); }
 * });
 * session.setMetadata(new MediaMetadata.Builder()
 *     .setTitle("Song Title")
 *     .setArtist("Artist Name")
 *     .build());
 * session.setActive(true);
 * session.setPlaybackState(PlaybackState.PLAYING, 0);
 * </pre>
 *
 * @since 2025-12-21
 * @version 1.0
 */
public class MediaSession {

    /** セッションの一意識別子 */
    private final String sessionId;

    /** このセッションを所有するアプリケーションのID */
    private final String applicationId;

    /** 現在の再生状態 */
    private PlaybackState playbackState = PlaybackState.STOPPED;

    /** メディアのメタデータ */
    private MediaMetadata metadata;

    /** 再生コントロールコールバック */
    private MediaSessionCallback callback;

    /** 現在の再生位置（ミリ秒） */
    private long currentPositionMs = 0;

    /** 最後に再生位置を更新した時刻 */
    private long lastPositionUpdateTime = 0;

    /** セッションがアクティブかどうか */
    private boolean isActive = false;

    /** このセッションを管理するMediaSessionManager */
    private MediaSessionManager manager;

    /**
     * MediaSessionを生成する。
     * このコンストラクタは通常、MediaSessionManager.createSession()から呼び出される。
     *
     * @param applicationId このセッションを所有するアプリケーションのID
     */
    public MediaSession(String applicationId) {
        this.applicationId = applicationId;
        this.sessionId = applicationId + "_" + System.currentTimeMillis();
    }

    /**
     * セッションをアクティブ化または非アクティブ化する。
     * アクティブなセッションのみがコントロールセンターに表示される。
     *
     * @param active アクティブにする場合true
     */
    public void setActive(boolean active) {
        this.isActive = active;
        if (manager != null) {
            if (active) {
                manager.onSessionActivated(this);
            } else {
                manager.onSessionDeactivated(this);
            }
        }
    }

    /**
     * 再生状態を更新する。
     *
     * @param state 新しい再生状態
     * @param positionMs 現在の再生位置（ミリ秒）
     */
    public void setPlaybackState(PlaybackState state, long positionMs) {
        this.playbackState = state;
        this.currentPositionMs = positionMs;
        this.lastPositionUpdateTime = System.currentTimeMillis();
        notifyStateChanged();
    }

    /**
     * 再生状態のみを更新する（位置は変更しない）。
     *
     * @param state 新しい再生状態
     */
    public void setPlaybackState(PlaybackState state) {
        setPlaybackState(state, getCurrentPosition());
    }

    /**
     * メタデータを更新する。
     *
     * @param metadata 新しいメタデータ
     */
    public void setMetadata(MediaMetadata metadata) {
        this.metadata = metadata;
        notifyMetadataChanged();
    }

    /**
     * 再生コントロールコールバックを設定する。
     * コントロールセンター等からの操作はこのコールバックを通じて通知される。
     *
     * @param callback コールバック実装
     */
    public void setCallback(MediaSessionCallback callback) {
        this.callback = callback;
    }

    /**
     * セッションを解放する。
     * セッションが不要になった場合に呼び出す。
     */
    public void release() {
        setActive(false);
        if (manager != null) {
            manager.unregisterSession(this);
        }
    }

    /**
     * セッションIDを取得する。
     *
     * @return セッションの一意識別子
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * アプリケーションIDを取得する。
     *
     * @return このセッションを所有するアプリケーションのID
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * 現在の再生状態を取得する。
     *
     * @return 再生状態
     */
    public PlaybackState getPlaybackState() {
        return playbackState;
    }

    /**
     * メタデータを取得する。
     *
     * @return メタデータ、設定されていない場合はnull
     */
    public MediaMetadata getMetadata() {
        return metadata;
    }

    /**
     * セッションがアクティブかどうかを確認する。
     *
     * @return アクティブな場合true
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * 現在の再生位置を取得する。
     * 再生中の場合は経過時間を考慮して計算される。
     *
     * @return 現在の再生位置（ミリ秒）
     */
    public long getCurrentPosition() {
        if (playbackState == PlaybackState.PLAYING) {
            long elapsed = System.currentTimeMillis() - lastPositionUpdateTime;
            return currentPositionMs + elapsed;
        }
        return currentPositionMs;
    }

    /**
     * コールバックを取得する。
     * 内部使用（MediaControllerから呼び出される）。
     *
     * @return コールバック、設定されていない場合はnull
     */
    MediaSessionCallback getCallback() {
        return callback;
    }

    /**
     * MediaSessionManagerを設定する。
     * 内部使用（MediaSessionManagerから呼び出される）。
     *
     * @param manager このセッションを管理するMediaSessionManager
     */
    void setManager(MediaSessionManager manager) {
        this.manager = manager;
    }

    /**
     * 再生状態変更をマネージャーに通知する。
     */
    private void notifyStateChanged() {
        if (manager != null) {
            manager.onPlaybackStateChanged(this);
        }
    }

    /**
     * メタデータ変更をマネージャーに通知する。
     */
    private void notifyMetadataChanged() {
        if (manager != null) {
            manager.onMetadataChanged(this);
        }
    }

    @Override
    public String toString() {
        return "MediaSession{" +
                "sessionId='" + sessionId + '\'' +
                ", applicationId='" + applicationId + '\'' +
                ", playbackState=" + playbackState +
                ", isActive=" + isActive +
                '}';
    }
}
