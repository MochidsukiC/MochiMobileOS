package jp.moyashi.phoneos.api;

import jp.moyashi.phoneos.api.clipboard.ClipboardManager;
import jp.moyashi.phoneos.api.hardware.*;
import jp.moyashi.phoneos.api.intent.ActivityManager;
import jp.moyashi.phoneos.api.intent.Intent;
import jp.moyashi.phoneos.api.network.NetworkManager;
import jp.moyashi.phoneos.api.permission.PermissionManager;
import jp.moyashi.phoneos.api.sensor.SensorManager;

/**
 * アプリケーションコンテキストAPI。
 * 外部MODアプリケーション開発者向けの公開SDK。
 *
 * このインターフェースは実OSのSDK（iOS SDK, Android SDK等）に相当し、
 * Kernelの詳細を隠蔽しながら必要な機能を提供する。
 *
 * @author MochiOS Team
 * @version 2.0
 */
public interface AppContext {

    // ========================================
    // ストレージ・設定
    // ========================================

    /**
     * アプリ専用ストレージを取得する。
     * サンドボックス化されており、他のアプリの領域にはアクセスできない。
     *
     * @return アプリ専用ストレージ
     */
    AppStorage getStorage();

    /**
     * アプリ固有設定を取得する。
     *
     * @return アプリ固有設定
     */
    AppSettings getSettings();

    // ========================================
    // 通知
    // ========================================

    /**
     * シンプルな通知を送信する。
     *
     * @param title 通知タイトル
     * @param message 通知メッセージ
     */
    void sendNotification(String title, String message);

    /**
     * 詳細な通知を送信する。
     *
     * @param notification 通知オブジェクト
     */
    void sendNotification(Notification notification);

    // ========================================
    // 画面遷移
    // ========================================

    /**
     * 画面をスタックにプッシュする。
     *
     * @param screen プッシュする画面
     */
    void pushScreen(ModScreen screen);

    /**
     * 現在の画面をポップする。
     */
    void popScreen();

    /**
     * 現在の画面を置き換える。
     *
     * @param screen 新しい画面
     */
    void replaceScreen(ModScreen screen);

    // ========================================
    // アプリ情報
    // ========================================

    /**
     * アプリIDを取得する。
     *
     * @return アプリID
     */
    String getAppId();

    /**
     * アプリ名を取得する。
     *
     * @return アプリ名
     */
    String getAppName();

    // ========================================
    // システム情報
    // ========================================

    /**
     * システム情報を取得する（読み取り専用）。
     *
     * @return システム情報
     */
    SystemInfo getSystemInfo();

    // ========================================
    // ハードウェアAPI
    // ========================================

    /**
     * バッテリー情報を取得する。
     *
     * @return バッテリー情報
     */
    BatteryInfo getBatteryInfo();

    /**
     * 位置情報ソケットを取得する。
     *
     * @return 位置情報ソケット
     */
    LocationSocket getLocationSocket();

    /**
     * Bluetooth通信ソケットを取得する。
     *
     * @return Bluetoothソケット
     */
    BluetoothSocket getBluetoothSocket();

    /**
     * モバイルデータ通信ソケットを取得する。
     *
     * @return モバイルデータソケット
     */
    MobileDataSocket getMobileDataSocket();

    /**
     * SIM情報を取得する。
     *
     * @return SIM情報
     */
    SIMInfo getSIMInfo();

    /**
     * カメラソケットを取得する。
     *
     * @return カメラソケット
     */
    CameraSocket getCameraSocket();

    /**
     * マイクソケットを取得する。
     *
     * @return マイクソケット
     */
    MicrophoneSocket getMicrophoneSocket();

    /**
     * スピーカーソケットを取得する。
     *
     * @return スピーカーソケット
     */
    SpeakerSocket getSpeakerSocket();

    /**
     * IC通信ソケットを取得する。
     *
     * @return ICソケット
     */
    ICSocket getICSocket();

    // ========================================
    // センサーAPI
    // ========================================

    /**
     * センサーマネージャーを取得する。
     *
     * @return センサーマネージャー
     */
    SensorManager getSensorManager();

    // ========================================
    // パーミッションAPI
    // ========================================

    /**
     * パーミッションマネージャーを取得する。
     *
     * @return パーミッションマネージャー
     */
    PermissionManager getPermissionManager();

    // ========================================
    // インテント/ActivityAPI
    // ========================================

    /**
     * アクティビティマネージャーを取得する。
     *
     * @return アクティビティマネージャー
     */
    ActivityManager getActivityManager();

    /**
     * インテントを送信してアクティビティを開始する。
     *
     * @param intent 開始するインテント
     * @return 成功した場合true
     */
    boolean startActivity(Intent intent);

    // ========================================
    // クリップボードAPI
    // ========================================

    /**
     * クリップボードマネージャーを取得する。
     *
     * @return クリップボードマネージャー
     */
    ClipboardManager getClipboardManager();

    // ========================================
    // ネットワークAPI
    // ========================================

    /**
     * ネットワークマネージャーを取得する。
     * HTTPリクエストの送信、仮想サーバーの登録、ネットワーク状態の取得が可能。
     *
     * @return ネットワークマネージャー
     */
    NetworkManager getNetworkManager();

    // ========================================
    // ログ
    // ========================================

    /**
     * デバッグログを出力する。
     *
     * @param message ログメッセージ
     */
    void logDebug(String message);

    /**
     * 情報ログを出力する。
     *
     * @param message ログメッセージ
     */
    void logInfo(String message);

    /**
     * 警告ログを出力する。
     *
     * @param message ログメッセージ
     */
    void logWarn(String message);

    /**
     * エラーログを出力する。
     *
     * @param message ログメッセージ
     */
    void logError(String message);

    /**
     * エラーログを出力する。
     *
     * @param message ログメッセージ
     * @param throwable 例外
     */
    void logError(String message, Throwable throwable);

    // ========================================
    // リソース
    // ========================================

    /**
     * アプリリソースからテクスチャパスを取得する。
     * MODのリソースフォルダ内のテクスチャを参照する。
     *
     * @param resourcePath リソースパス（例: "textures/icon.png"）
     * @return 完全修飾リソースパス
     */
    String getResourcePath(String resourcePath);
}
