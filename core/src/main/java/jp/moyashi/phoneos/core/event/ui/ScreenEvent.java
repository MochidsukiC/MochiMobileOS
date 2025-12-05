package jp.moyashi.phoneos.core.event.ui;

import jp.moyashi.phoneos.core.event.Event;
import jp.moyashi.phoneos.core.event.EventType;
import jp.moyashi.phoneos.core.ui.Screen;

/**
 * 画面遷移イベント。
 * 画面の表示、非表示、遷移を通知する。
 *
 * @since 2025-12-04
 * @version 1.0
 */
public class ScreenEvent extends Event {

    /** 遷移元の画面 */
    private final Screen fromScreen;

    /** 遷移先の画面 */
    private final Screen toScreen;

    /** 画面クラス（Screenインスタンスが利用できない場合） */
    private final Class<? extends Screen> screenClass;

    /** 画面名 */
    private final String screenName;

    /** 遷移タイプ */
    private final TransitionType transitionType;

    /** アニメーション時間（ミリ秒） */
    private final long animationDuration;

    /**
     * 画面遷移タイプ。
     */
    public enum TransitionType {
        /** プッシュ（スタックに追加） */
        PUSH,
        /** ポップ（スタックから削除） */
        POP,
        /** 置換（現在の画面を置き換え） */
        REPLACE,
        /** リセット（スタックをクリアして新規） */
        RESET,
        /** なし（単純な表示/非表示） */
        NONE
    }

    /**
     * 画面遷移イベントを作成する。
     *
     * @param type イベントタイプ
     * @param fromScreen 遷移元画面
     * @param toScreen 遷移先画面
     * @param transitionType 遷移タイプ
     */
    public ScreenEvent(EventType type, Screen fromScreen, Screen toScreen,
                       TransitionType transitionType) {
        this(type, fromScreen, toScreen, transitionType, 300);
    }

    /**
     * 画面遷移イベントを作成する。
     *
     * @param type イベントタイプ
     * @param fromScreen 遷移元画面
     * @param toScreen 遷移先画面
     * @param transitionType 遷移タイプ
     * @param animationDuration アニメーション時間
     */
    public ScreenEvent(EventType type, Screen fromScreen, Screen toScreen,
                       TransitionType transitionType, long animationDuration) {
        super(type, toScreen != null ? toScreen : fromScreen);
        this.fromScreen = fromScreen;
        this.toScreen = toScreen;
        this.transitionType = transitionType;
        this.animationDuration = animationDuration;

        if (toScreen != null) {
            this.screenClass = toScreen.getClass();
            this.screenName = toScreen.getClass().getSimpleName();
        } else if (fromScreen != null) {
            this.screenClass = fromScreen.getClass();
            this.screenName = fromScreen.getClass().getSimpleName();
        } else {
            this.screenClass = null;
            this.screenName = "Unknown";
        }
    }

    /**
     * 画面クラスでイベントを作成する。
     *
     * @param type イベントタイプ
     * @param source イベント発生元
     * @param screenClass 画面クラス
     * @param transitionType 遷移タイプ
     */
    public ScreenEvent(EventType type, Object source,
                       Class<? extends Screen> screenClass,
                       TransitionType transitionType) {
        super(type, source);
        this.fromScreen = null;
        this.toScreen = null;
        this.screenClass = screenClass;
        this.screenName = screenClass != null ? screenClass.getSimpleName() : "Unknown";
        this.transitionType = transitionType;
        this.animationDuration = 300;
    }

    /**
     * 遷移元画面を取得する。
     *
     * @return 遷移元画面、存在しない場合null
     */
    public Screen getFromScreen() {
        return fromScreen;
    }

    /**
     * 遷移先画面を取得する。
     *
     * @return 遷移先画面、存在しない場合null
     */
    public Screen getToScreen() {
        return toScreen;
    }

    /**
     * 画面クラスを取得する。
     *
     * @return 画面クラス
     */
    public Class<? extends Screen> getScreenClass() {
        return screenClass;
    }

    /**
     * 画面名を取得する。
     *
     * @return 画面名
     */
    public String getScreenName() {
        return screenName;
    }

    /**
     * 遷移タイプを取得する。
     *
     * @return 遷移タイプ
     */
    public TransitionType getTransitionType() {
        return transitionType;
    }

    /**
     * アニメーション時間を取得する。
     *
     * @return アニメーション時間（ミリ秒）
     */
    public long getAnimationDuration() {
        return animationDuration;
    }

    /**
     * 画面遷移開始イベントを作成する。
     *
     * @param from 遷移元画面
     * @param to 遷移先画面
     * @param type 遷移タイプ
     * @return 画面遷移開始イベント
     */
    public static ScreenEvent transitionStart(Screen from, Screen to,
                                              TransitionType type) {
        return new ScreenEvent(EventType.SCREEN_TRANSITION_START, from, to, type);
    }

    /**
     * 画面遷移完了イベントを作成する。
     *
     * @param from 遷移元画面
     * @param to 遷移先画面
     * @param type 遷移タイプ
     * @return 画面遷移完了イベント
     */
    public static ScreenEvent transitionEnd(Screen from, Screen to,
                                            TransitionType type) {
        return new ScreenEvent(EventType.SCREEN_TRANSITION_END, from, to, type);
    }

    /**
     * ホーム画面表示イベントを作成する。
     *
     * @param homeScreen ホーム画面
     * @return ホーム画面表示イベント
     */
    public static ScreenEvent home(Screen homeScreen) {
        return new ScreenEvent(EventType.SCREEN_HOME, null, homeScreen,
                TransitionType.RESET);
    }

    /**
     * ロック画面表示イベントを作成する。
     *
     * @param lockScreen ロック画面
     * @return ロック画面表示イベント
     */
    public static ScreenEvent lock(Screen lockScreen) {
        return new ScreenEvent(EventType.SCREEN_LOCK, null, lockScreen,
                TransitionType.PUSH);
    }

    /**
     * 設定画面表示イベントを作成する。
     *
     * @param settingsScreen 設定画面
     * @return 設定画面表示イベント
     */
    public static ScreenEvent settings(Screen settingsScreen) {
        return new ScreenEvent(EventType.SCREEN_SETTINGS, null, settingsScreen,
                TransitionType.PUSH);
    }

    @Override
    public boolean isCancellable() {
        // 画面遷移開始イベントはキャンセル可能
        return getType() == EventType.SCREEN_TRANSITION_START;
    }

    @Override
    public String toString() {
        return String.format("ScreenEvent[type=%s, from=%s, to=%s, transition=%s]",
                getType(),
                fromScreen != null ? fromScreen.getClass().getSimpleName() : "null",
                toScreen != null ? toScreen.getClass().getSimpleName() : "null",
                transitionType);
    }
}