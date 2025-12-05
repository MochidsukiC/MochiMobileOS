package jp.moyashi.phoneos.core.event;

/**
 * イベントタイプを定義する列挙型。
 * システム全体で使用されるイベントタイプを一元管理する。
 *
 * @since 2025-12-04
 * @version 1.0
 */
public enum EventType {

    // ========== システムイベント ==========
    /** システム起動 */
    SYSTEM_STARTUP,
    /** システムシャットダウン */
    SYSTEM_SHUTDOWN,
    /** システムスリープ */
    SYSTEM_SLEEP,
    /** システムウェイク */
    SYSTEM_WAKE,
    /** システム一時停止 */
    SYSTEM_PAUSE,
    /** システム再開 */
    SYSTEM_RESUME,

    // ========== アプリケーションイベント ==========
    /** アプリケーション起動 */
    APP_LAUNCH,
    /** アプリケーション終了 */
    APP_CLOSE,
    /** アプリケーション一時停止 */
    APP_PAUSE,
    /** アプリケーション再開 */
    APP_RESUME,
    /** アプリケーションフォーカス取得 */
    APP_FOCUS_GAINED,
    /** アプリケーションフォーカス喪失 */
    APP_FOCUS_LOST,

    // ========== 画面遷移イベント ==========
    /** 画面遷移開始 */
    SCREEN_TRANSITION_START,
    /** 画面遷移完了 */
    SCREEN_TRANSITION_END,
    /** ホーム画面表示 */
    SCREEN_HOME,
    /** ロック画面表示 */
    SCREEN_LOCK,
    /** 設定画面表示 */
    SCREEN_SETTINGS,

    // ========== 入力イベント ==========
    /** キー押下 */
    KEY_PRESSED,
    /** キーリリース */
    KEY_RELEASED,
    /** マウス押下 */
    MOUSE_PRESSED,
    /** マウスリリース */
    MOUSE_RELEASED,
    /** マウス移動 */
    MOUSE_MOVED,
    /** マウスドラッグ */
    MOUSE_DRAGGED,
    /** ホイール回転 */
    MOUSE_WHEEL,
    /** ジェスチャー検出 */
    GESTURE_DETECTED,

    // ========== UIイベント ==========
    /** ポップアップ表示 */
    POPUP_SHOW,
    /** ポップアップ非表示 */
    POPUP_HIDE,
    /** 通知表示 */
    NOTIFICATION_SHOW,
    /** 通知非表示 */
    NOTIFICATION_HIDE,
    /** コントロールセンター表示 */
    CONTROL_CENTER_SHOW,
    /** コントロールセンター非表示 */
    CONTROL_CENTER_HIDE,
    /** ダイアログ表示 */
    DIALOG_SHOW,
    /** ダイアログ非表示 */
    DIALOG_HIDE,

    // ========== 設定変更イベント ==========
    /** 設定変更 */
    SETTINGS_CHANGED,
    /** テーマ変更 */
    THEME_CHANGED,
    /** 言語変更 */
    LANGUAGE_CHANGED,
    /** 音量変更 */
    VOLUME_CHANGED,
    /** 明るさ変更 */
    BRIGHTNESS_CHANGED,

    // ========== ネットワークイベント ==========
    /** ネットワーク接続 */
    NETWORK_CONNECTED,
    /** ネットワーク切断 */
    NETWORK_DISCONNECTED,
    /** WiFi接続 */
    WIFI_CONNECTED,
    /** WiFi切断 */
    WIFI_DISCONNECTED,

    // ========== バッテリーイベント ==========
    /** バッテリー残量変更 */
    BATTERY_LEVEL_CHANGED,
    /** 充電開始 */
    BATTERY_CHARGING_START,
    /** 充電停止 */
    BATTERY_CHARGING_STOP,
    /** バッテリー低下 */
    BATTERY_LOW,

    // ========== ファイルシステムイベント ==========
    /** ファイル作成 */
    FILE_CREATED,
    /** ファイル削除 */
    FILE_DELETED,
    /** ファイル変更 */
    FILE_MODIFIED,
    /** ディレクトリ作成 */
    DIRECTORY_CREATED,
    /** ディレクトリ削除 */
    DIRECTORY_DELETED,

    // ========== カスタムイベント ==========
    /** カスタムイベント（汎用） */
    CUSTOM
}