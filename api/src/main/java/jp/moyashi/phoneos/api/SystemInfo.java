package jp.moyashi.phoneos.api;

/**
 * システム情報API（読み取り専用）。
 * アプリケーションがシステムの状態を参照するためのインターフェース。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public interface SystemInfo {

    /**
     * OSバージョンを取得する。
     *
     * @return OSバージョン文字列
     */
    String getOsVersion();

    /**
     * 画面幅を取得する。
     *
     * @return 画面幅（ピクセル）
     */
    int getScreenWidth();

    /**
     * 画面高さを取得する。
     *
     * @return 画面高さ（ピクセル）
     */
    int getScreenHeight();

    /**
     * 現在のバッテリー残量を取得する。
     *
     * @return バッテリー残量（0-100）
     */
    int getBatteryLevel();

    /**
     * 充電中かどうかを取得する。
     *
     * @return 充電中の場合true
     */
    boolean isCharging();

    /**
     * WiFi接続中かどうかを取得する。
     *
     * @return WiFi接続中の場合true
     */
    boolean isWifiConnected();

    /**
     * 現在の時刻をミリ秒で取得する。
     *
     * @return エポックからのミリ秒
     */
    long getCurrentTimeMillis();

    /**
     * ワールドIDを取得する。
     *
     * @return ワールドID
     */
    String getWorldId();
}
