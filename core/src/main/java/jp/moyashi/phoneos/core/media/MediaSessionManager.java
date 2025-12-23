package jp.moyashi.phoneos.core.media;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * システム全体のMediaSessionを管理するサービス。
 * ServiceContainerに登録され、DIコンテナから取得して使用する。
 *
 * MediaSessionManagerは以下の機能を提供する:
 * - MediaSessionの作成と管理
 * - アクティブなセッションの追跡
 * - オーディオフォーカスの管理
 * - セッション変更イベントの通知
 *
 * @since 2025-12-21
 * @version 1.0
 */
public class MediaSessionManager {

    private static final Logger logger = Logger.getLogger(MediaSessionManager.class.getName());

    /** 登録されているセッションのリスト */
    private final List<MediaSession> sessions = new CopyOnWriteArrayList<>();

    /** 現在アクティブなセッション */
    private MediaSession activeSession = null;

    /** オーディオフォーカス管理 */
    private final AudioFocusManager audioFocusManager;

    /** セッション変更リスナー */
    private final List<Consumer<MediaSession>> sessionChangeListeners = new CopyOnWriteArrayList<>();

    /** 再生状態変更リスナー */
    private final List<Consumer<MediaSession>> playbackStateListeners = new CopyOnWriteArrayList<>();

    /** メタデータ変更リスナー */
    private final List<Consumer<MediaSession>> metadataListeners = new CopyOnWriteArrayList<>();

    /**
     * MediaSessionManagerを作成する。
     */
    public MediaSessionManager() {
        this.audioFocusManager = new AudioFocusManager();
        logger.info("MediaSessionManager initialized");
    }

    /**
     * 新しいMediaSessionを作成して登録する。
     *
     * @param applicationId アプリケーションID
     * @return 作成されたMediaSession
     */
    public MediaSession createSession(String applicationId) {
        // 既存のセッションがあれば返す
        for (MediaSession session : sessions) {
            if (session.getApplicationId().equals(applicationId)) {
                logger.fine("MediaSessionManager: Returning existing session for " + applicationId);
                return session;
            }
        }

        // 新しいセッションを作成
        MediaSession session = new MediaSession(applicationId);
        session.setManager(this);
        sessions.add(session);

        logger.info("MediaSessionManager: Created session for " + applicationId);
        return session;
    }

    /**
     * セッションを登録解除する。
     *
     * @param session 登録解除するセッション
     */
    public void unregisterSession(MediaSession session) {
        sessions.remove(session);

        if (activeSession == session) {
            activeSession = null;
            notifySessionChange(null);
        }

        logger.info("MediaSessionManager: Unregistered session " + session.getSessionId());
    }

    /**
     * アクティブなセッションのMediaControllerを取得する。
     *
     * @return MediaController、アクティブなセッションがない場合はnull
     */
    public MediaController getActiveController() {
        if (activeSession != null && activeSession.isActive()) {
            return new MediaController(activeSession);
        }
        return null;
    }

    /**
     * 指定されたアプリケーションのMediaControllerを取得する。
     *
     * @param applicationId アプリケーションID
     * @return MediaController、見つからない場合はnull
     */
    public MediaController getControllerForApp(String applicationId) {
        for (MediaSession session : sessions) {
            if (session.getApplicationId().equals(applicationId)) {
                return new MediaController(session);
            }
        }
        return null;
    }

    /**
     * すべてのアクティブなセッションのコントローラーを取得する。
     *
     * @return MediaControllerのリスト
     */
    public List<MediaController> getAllActiveControllers() {
        List<MediaController> controllers = new ArrayList<>();
        for (MediaSession session : sessions) {
            if (session.isActive()) {
                controllers.add(new MediaController(session));
            }
        }
        return controllers;
    }

    /**
     * 現在アクティブなセッションがあるかを確認する。
     *
     * @return アクティブなセッションがある場合true
     */
    public boolean hasActiveSession() {
        return activeSession != null && activeSession.isActive();
    }

    /**
     * 現在再生中かどうかを確認する。
     *
     * @return 再生中の場合true
     */
    public boolean isPlaying() {
        return activeSession != null &&
                activeSession.getPlaybackState() == PlaybackState.PLAYING;
    }

    /**
     * AudioFocusManagerを取得する。
     *
     * @return AudioFocusManager
     */
    public AudioFocusManager getAudioFocusManager() {
        return audioFocusManager;
    }

    /**
     * セッション変更リスナーを追加する。
     * アクティブなセッションが変更されたときに通知される。
     *
     * @param listener リスナー
     */
    public void addSessionChangeListener(Consumer<MediaSession> listener) {
        sessionChangeListeners.add(listener);
    }

    /**
     * セッション変更リスナーを削除する。
     *
     * @param listener 削除するリスナー
     */
    public void removeSessionChangeListener(Consumer<MediaSession> listener) {
        sessionChangeListeners.remove(listener);
    }

    /**
     * 再生状態変更リスナーを追加する。
     * いずれかのセッションの再生状態が変更されたときに通知される。
     *
     * @param listener リスナー
     */
    public void addPlaybackStateListener(Consumer<MediaSession> listener) {
        playbackStateListeners.add(listener);
    }

    /**
     * 再生状態変更リスナーを削除する。
     *
     * @param listener 削除するリスナー
     */
    public void removePlaybackStateListener(Consumer<MediaSession> listener) {
        playbackStateListeners.remove(listener);
    }

    /**
     * メタデータ変更リスナーを追加する。
     * いずれかのセッションのメタデータが変更されたときに通知される。
     *
     * @param listener リスナー
     */
    public void addMetadataListener(Consumer<MediaSession> listener) {
        metadataListeners.add(listener);
    }

    /**
     * メタデータ変更リスナーを削除する。
     *
     * @param listener 削除するリスナー
     */
    public void removeMetadataListener(Consumer<MediaSession> listener) {
        metadataListeners.remove(listener);
    }

    /**
     * 登録されているセッション数を取得する。
     *
     * @return セッション数
     */
    public int getSessionCount() {
        return sessions.size();
    }

    /**
     * アクティブなセッション数を取得する。
     *
     * @return アクティブなセッション数
     */
    public int getActiveSessionCount() {
        int count = 0;
        for (MediaSession session : sessions) {
            if (session.isActive()) {
                count++;
            }
        }
        return count;
    }

    // 内部コールバック（MediaSessionから呼び出される）

    /**
     * セッションがアクティブ化されたときに呼び出される。
     *
     * @param session アクティブ化されたセッション
     */
    void onSessionActivated(MediaSession session) {
        activeSession = session;
        notifySessionChange(session);
        logger.fine("MediaSessionManager: Session activated - " + session.getSessionId());
    }

    /**
     * セッションが非アクティブ化されたときに呼び出される。
     *
     * @param session 非アクティブ化されたセッション
     */
    void onSessionDeactivated(MediaSession session) {
        if (activeSession == session) {
            activeSession = null;
            notifySessionChange(null);
        }
        logger.fine("MediaSessionManager: Session deactivated - " + session.getSessionId());
    }

    /**
     * 再生状態が変更されたときに呼び出される。
     *
     * @param session 状態が変更されたセッション
     */
    void onPlaybackStateChanged(MediaSession session) {
        for (Consumer<MediaSession> listener : playbackStateListeners) {
            try {
                listener.accept(session);
            } catch (Exception e) {
                logger.warning("MediaSessionManager: Error in playback state listener: " +
                        e.getMessage());
            }
        }
    }

    /**
     * メタデータが変更されたときに呼び出される。
     *
     * @param session メタデータが変更されたセッション
     */
    void onMetadataChanged(MediaSession session) {
        for (Consumer<MediaSession> listener : metadataListeners) {
            try {
                listener.accept(session);
            } catch (Exception e) {
                logger.warning("MediaSessionManager: Error in metadata listener: " +
                        e.getMessage());
            }
        }
    }

    /**
     * セッション変更をリスナーに通知する。
     *
     * @param session 新しいアクティブセッション（null可）
     */
    private void notifySessionChange(MediaSession session) {
        for (Consumer<MediaSession> listener : sessionChangeListeners) {
            try {
                listener.accept(session);
            } catch (Exception e) {
                logger.warning("MediaSessionManager: Error in session change listener: " +
                        e.getMessage());
            }
        }
    }

    /**
     * すべてのセッションを解放する。
     * システムシャットダウン時に呼び出す。
     */
    public void releaseAll() {
        for (MediaSession session : new ArrayList<>(sessions)) {
            session.release();
        }
        sessions.clear();
        activeSession = null;
        audioFocusManager.clearFocusStack();
        logger.info("MediaSessionManager: All sessions released");
    }
}
