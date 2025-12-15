package jp.moyashi.phoneos.api.proxy;

/**
 * プロセス間通信チャンネルインターフェース。
 * Forge JVMとServer JVM間の通信を抽象化する。
 *
 * このインターフェースを実装することで、将来のFabric/NeoForge対応が可能。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public interface IPCChannel {

    /**
     * IPCメッセージを送信し、レスポンスを待つ。
     *
     * @param type メッセージタイプ
     * @param payload ペイロード（JSON文字列）
     * @return レスポンス（JSON文字列）、エラー時はnull
     */
    String sendAndReceive(String type, String payload);

    /**
     * IPCメッセージを送信する（レスポンス不要）。
     *
     * @param type メッセージタイプ
     * @param payload ペイロード（JSON文字列）
     * @return 送信成功した場合true
     */
    boolean send(String type, String payload);

    /**
     * チャンネルが接続されているかどうか。
     *
     * @return 接続中の場合true
     */
    boolean isConnected();

    /**
     * チャンネルを閉じる。
     */
    void close();

    // ========================================
    // メッセージタイプ定数
    // ========================================

    /** ストレージ: ファイル読み込み */
    String MSG_STORAGE_READ = "storage.read";

    /** ストレージ: ファイル書き込み */
    String MSG_STORAGE_WRITE = "storage.write";

    /** ストレージ: バイナリ読み込み */
    String MSG_STORAGE_READ_BYTES = "storage.readBytes";

    /** ストレージ: バイナリ書き込み */
    String MSG_STORAGE_WRITE_BYTES = "storage.writeBytes";

    /** ストレージ: 存在確認 */
    String MSG_STORAGE_EXISTS = "storage.exists";

    /** ストレージ: 削除 */
    String MSG_STORAGE_DELETE = "storage.delete";

    /** ストレージ: 一覧 */
    String MSG_STORAGE_LIST = "storage.list";

    /** ストレージ: ディレクトリ作成 */
    String MSG_STORAGE_MKDIR = "storage.mkdir";

    /** ストレージ: ファイル確認 */
    String MSG_STORAGE_IS_FILE = "storage.isFile";

    /** ストレージ: ディレクトリ確認 */
    String MSG_STORAGE_IS_DIR = "storage.isDir";

    /** 設定: 取得 */
    String MSG_SETTINGS_GET = "settings.get";

    /** 設定: 保存 */
    String MSG_SETTINGS_SET = "settings.set";

    /** 設定: 存在確認 */
    String MSG_SETTINGS_CONTAINS = "settings.contains";

    /** 設定: 削除 */
    String MSG_SETTINGS_REMOVE = "settings.remove";

    /** 設定: キー一覧 */
    String MSG_SETTINGS_KEYS = "settings.keys";

    /** 設定: 永続化 */
    String MSG_SETTINGS_SAVE = "settings.save";

    /** 通知: 送信 */
    String MSG_NOTIFICATION_SEND = "notification.send";

    /** システム情報: 取得 */
    String MSG_SYSTEM_INFO = "system.info";

    /** ログ: 出力 */
    String MSG_LOG = "log";

    /** 画面: プッシュ */
    String MSG_SCREEN_PUSH = "screen.push";

    /** 画面: ポップ */
    String MSG_SCREEN_POP = "screen.pop";

    /** 画面: 置換 */
    String MSG_SCREEN_REPLACE = "screen.replace";

    /** アプリ: 登録 */
    String MSG_APP_REGISTER = "app.register";

    /** アプリ: 登録解除 */
    String MSG_APP_UNREGISTER = "app.unregister";

    /** ピクセル: 描画データ送信 */
    String MSG_PIXELS_UPDATE = "pixels.update";

    // ========================================
    // ハードウェアAPI
    // ========================================

    /** バッテリー情報取得 */
    String MSG_HARDWARE_BATTERY = "hardware.battery";

    /** 位置情報取得 */
    String MSG_HARDWARE_LOCATION = "hardware.location";

    /** 位置情報有効/無効設定 */
    String MSG_HARDWARE_LOCATION_SET = "hardware.location.set";

    /** Bluetooth情報取得 */
    String MSG_HARDWARE_BLUETOOTH = "hardware.bluetooth";

    /** Bluetooth接続/切断 */
    String MSG_HARDWARE_BLUETOOTH_CONNECT = "hardware.bluetooth.connect";

    /** モバイルデータ情報取得 */
    String MSG_HARDWARE_MOBILE_DATA = "hardware.mobileData";

    /** SIM情報取得 */
    String MSG_HARDWARE_SIM = "hardware.sim";

    /** カメラ情報取得 */
    String MSG_HARDWARE_CAMERA = "hardware.camera";

    /** カメラ有効/無効設定 */
    String MSG_HARDWARE_CAMERA_SET = "hardware.camera.set";

    /** マイク情報取得 */
    String MSG_HARDWARE_MICROPHONE = "hardware.microphone";

    /** マイク有効/無効設定 */
    String MSG_HARDWARE_MICROPHONE_SET = "hardware.microphone.set";

    /** スピーカー情報取得 */
    String MSG_HARDWARE_SPEAKER = "hardware.speaker";

    /** スピーカー設定 */
    String MSG_HARDWARE_SPEAKER_SET = "hardware.speaker.set";

    /** IC通信情報取得 */
    String MSG_HARDWARE_IC = "hardware.ic";

    /** IC通信有効/無効設定 */
    String MSG_HARDWARE_IC_SET = "hardware.ic.set";

    /** IC通信データポーリング */
    String MSG_HARDWARE_IC_POLL = "hardware.ic.poll";

    // ========================================
    // センサーAPI
    // ========================================

    /** センサー一覧取得 */
    String MSG_SENSOR_LIST = "sensor.list";

    /** センサーデフォルト取得 */
    String MSG_SENSOR_DEFAULT = "sensor.default";

    /** センサーリスナー登録 */
    String MSG_SENSOR_REGISTER = "sensor.register";

    /** センサーリスナー解除 */
    String MSG_SENSOR_UNREGISTER = "sensor.unregister";

    // ========================================
    // パーミッションAPI
    // ========================================

    /** パーミッションチェック */
    String MSG_PERMISSION_CHECK = "permission.check";

    /** パーミッションリクエスト */
    String MSG_PERMISSION_REQUEST = "permission.request";

    /** 許可済みパーミッション取得 */
    String MSG_PERMISSION_GRANTED = "permission.granted";

    // ========================================
    // インテント/ActivityAPI
    // ========================================

    /** アクティビティ開始 */
    String MSG_ACTIVITY_START = "activity.start";

    /** アクティビティ検索 */
    String MSG_ACTIVITY_FIND = "activity.find";

    // ========================================
    // ネットワークAPI
    // ========================================

    /** ネットワーク: HTTPリクエスト */
    String MSG_NETWORK_REQUEST = "network.request";

    /** ネットワーク: 状態取得 */
    String MSG_NETWORK_STATUS = "network.status";

    /** ネットワーク: 自分のIPvMアドレス取得 */
    String MSG_NETWORK_MY_ADDRESS = "network.myAddress";

    /** ネットワーク: サーバー登録 */
    String MSG_NETWORK_SERVER_REGISTER = "network.server.register";

    /** ネットワーク: サーバー登録解除 */
    String MSG_NETWORK_SERVER_UNREGISTER = "network.server.unregister";

    /** ネットワーク: サーバーリクエスト（core→MOD） */
    String MSG_NETWORK_SERVER_REQUEST = "network.server.request";

    /** ネットワーク: サーバーレスポンス（MOD→core） */
    String MSG_NETWORK_SERVER_RESPONSE = "network.server.response";

    /** ネットワーク: サーバー登録確認 */
    String MSG_NETWORK_SERVER_IS_REGISTERED = "network.server.isRegistered";

    // ========================================
    // クリップボードAPI
    // ========================================

    /** クリップボードテキストコピー */
    String MSG_CLIPBOARD_COPY_TEXT = "clipboard.copyText";

    /** クリップボードテキストペースト */
    String MSG_CLIPBOARD_PASTE_TEXT = "clipboard.pasteText";

    /** クリップボード画像コピー */
    String MSG_CLIPBOARD_COPY_IMAGE = "clipboard.copyImage";

    /** クリップボード画像ペースト */
    String MSG_CLIPBOARD_PASTE_IMAGE = "clipboard.pasteImage";

    /** クリップボードHTMLコピー */
    String MSG_CLIPBOARD_COPY_HTML = "clipboard.copyHtml";

    /** クリップボードクリア */
    String MSG_CLIPBOARD_CLEAR = "clipboard.clear";

    /** クリップボード状態取得 */
    String MSG_CLIPBOARD_STATUS = "clipboard.status";
}
