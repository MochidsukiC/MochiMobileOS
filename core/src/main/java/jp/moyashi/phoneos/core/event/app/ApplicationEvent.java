package jp.moyashi.phoneos.core.event.app;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.event.Event;
import jp.moyashi.phoneos.core.event.EventType;

/**
 * アプリケーションレベルのイベント。
 * アプリケーションの起動、終了、フォーカス変更などを通知する。
 *
 * @since 2025-12-04
 * @version 1.0
 */
public class ApplicationEvent extends Event {

    /** 対象アプリケーション */
    private final IApplication application;

    /** アプリケーションID */
    private final String appId;

    /** アプリケーション名 */
    private final String appName;

    /** 追加情報 */
    private final String details;

    /**
     * アプリケーションイベントを作成する。
     *
     * @param type イベントタイプ
     * @param application アプリケーション
     */
    public ApplicationEvent(EventType type, IApplication application) {
        this(type, application, null);
    }

    /**
     * アプリケーションイベントを作成する。
     *
     * @param type イベントタイプ
     * @param application アプリケーション
     * @param details 追加情報
     */
    public ApplicationEvent(EventType type, IApplication application, String details) {
        super(type, application);
        this.application = application;
        this.appId = application != null ? application.getClass().getName() : null;
        this.appName = application != null ? application.getName() : null;
        this.details = details;
    }

    /**
     * アプリケーションIDのみでイベントを作成する。
     *
     * @param type イベントタイプ
     * @param source イベント発生元
     * @param appId アプリケーションID
     * @param appName アプリケーション名
     */
    public ApplicationEvent(EventType type, Object source, String appId, String appName) {
        super(type, source);
        this.application = null;
        this.appId = appId;
        this.appName = appName;
        this.details = null;
    }

    /**
     * アプリケーションを取得する。
     *
     * @return アプリケーション、存在しない場合null
     */
    public IApplication getApplication() {
        return application;
    }

    /**
     * アプリケーションIDを取得する。
     *
     * @return アプリケーションID
     */
    public String getAppId() {
        return appId;
    }

    /**
     * アプリケーション名を取得する。
     *
     * @return アプリケーション名
     */
    public String getAppName() {
        return appName;
    }

    /**
     * 追加情報を取得する。
     *
     * @return 追加情報、存在しない場合null
     */
    public String getDetails() {
        return details;
    }

    /**
     * アプリケーション起動イベントを作成する。
     *
     * @param application 起動するアプリケーション
     * @return アプリケーション起動イベント
     */
    public static ApplicationEvent launch(IApplication application) {
        return new ApplicationEvent(EventType.APP_LAUNCH, application,
                "Application " + application.getName() + " is launching");
    }

    /**
     * アプリケーション終了イベントを作成する。
     *
     * @param application 終了するアプリケーション
     * @return アプリケーション終了イベント
     */
    public static ApplicationEvent close(IApplication application) {
        return new ApplicationEvent(EventType.APP_CLOSE, application,
                "Application " + application.getName() + " is closing");
    }

    /**
     * アプリケーション一時停止イベントを作成する。
     *
     * @param application 一時停止するアプリケーション
     * @return アプリケーション一時停止イベント
     */
    public static ApplicationEvent pause(IApplication application) {
        return new ApplicationEvent(EventType.APP_PAUSE, application,
                "Application " + application.getName() + " is pausing");
    }

    /**
     * アプリケーション再開イベントを作成する。
     *
     * @param application 再開するアプリケーション
     * @return アプリケーション再開イベント
     */
    public static ApplicationEvent resume(IApplication application) {
        return new ApplicationEvent(EventType.APP_RESUME, application,
                "Application " + application.getName() + " is resuming");
    }

    /**
     * アプリケーションフォーカス取得イベントを作成する。
     *
     * @param application フォーカスを取得するアプリケーション
     * @return フォーカス取得イベント
     */
    public static ApplicationEvent focusGained(IApplication application) {
        return new ApplicationEvent(EventType.APP_FOCUS_GAINED, application,
                "Application " + application.getName() + " gained focus");
    }

    /**
     * アプリケーションフォーカス喪失イベントを作成する。
     *
     * @param application フォーカスを失うアプリケーション
     * @return フォーカス喪失イベント
     */
    public static ApplicationEvent focusLost(IApplication application) {
        return new ApplicationEvent(EventType.APP_FOCUS_LOST, application,
                "Application " + application.getName() + " lost focus");
    }

    @Override
    public String toString() {
        return String.format("ApplicationEvent[type=%s, appId=%s, appName=%s, details=%s]",
                getType(), appId, appName, details);
    }
}