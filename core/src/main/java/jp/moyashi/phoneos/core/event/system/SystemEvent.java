package jp.moyashi.phoneos.core.event.system;

import jp.moyashi.phoneos.core.event.Event;
import jp.moyashi.phoneos.core.event.EventType;

/**
 * システムレベルのイベント基底クラス。
 * システムの起動、終了、スリープなどのライフサイクルイベント。
 *
 * @since 2025-12-04
 * @version 1.0
 */
public class SystemEvent extends Event {

    /** イベントの詳細メッセージ */
    private final String message;

    /** イベントのサブタイプ（詳細分類） */
    private final String subType;

    /**
     * システムイベントを作成する。
     *
     * @param type イベントタイプ
     * @param source イベント発生元
     * @param message 詳細メッセージ
     */
    public SystemEvent(EventType type, Object source, String message) {
        this(type, source, message, null);
    }

    /**
     * システムイベントを作成する。
     *
     * @param type イベントタイプ
     * @param source イベント発生元
     * @param message 詳細メッセージ
     * @param subType サブタイプ
     */
    public SystemEvent(EventType type, Object source, String message, String subType) {
        super(type, source);
        this.message = message;
        this.subType = subType;
    }

    /**
     * 詳細メッセージを取得する。
     *
     * @return 詳細メッセージ
     */
    public String getMessage() {
        return message;
    }

    /**
     * サブタイプを取得する。
     *
     * @return サブタイプ、設定されていない場合null
     */
    public String getSubType() {
        return subType;
    }

    /**
     * システム起動イベントを作成する。
     *
     * @param source イベント発生元
     * @return システム起動イベント
     */
    public static SystemEvent startup(Object source) {
        return new SystemEvent(EventType.SYSTEM_STARTUP, source, "System is starting up");
    }

    /**
     * システムシャットダウンイベントを作成する。
     *
     * @param source イベント発生元
     * @return システムシャットダウンイベント
     */
    public static SystemEvent shutdown(Object source) {
        return new SystemEvent(EventType.SYSTEM_SHUTDOWN, source, "System is shutting down");
    }

    /**
     * システムスリープイベントを作成する。
     *
     * @param source イベント発生元
     * @return システムスリープイベント
     */
    public static SystemEvent sleep(Object source) {
        return new SystemEvent(EventType.SYSTEM_SLEEP, source, "System is going to sleep");
    }

    /**
     * システムウェイクイベントを作成する。
     *
     * @param source イベント発生元
     * @return システムウェイクイベント
     */
    public static SystemEvent wake(Object source) {
        return new SystemEvent(EventType.SYSTEM_WAKE, source, "System is waking up");
    }

    /**
     * システム一時停止イベントを作成する。
     *
     * @param source イベント発生元
     * @return システム一時停止イベント
     */
    public static SystemEvent pause(Object source) {
        return new SystemEvent(EventType.SYSTEM_PAUSE, source, "System is pausing");
    }

    /**
     * システム再開イベントを作成する。
     *
     * @param source イベント発生元
     * @return システム再開イベント
     */
    public static SystemEvent resume(Object source) {
        return new SystemEvent(EventType.SYSTEM_RESUME, source, "System is resuming");
    }

    @Override
    public String toString() {
        return String.format("SystemEvent[type=%s, message=%s, subType=%s]",
                getType(), message, subType);
    }
}