package jp.moyashi.phoneos.api.hardware;

/**
 * モバイルデータ通信ソケットAPI。
 * モバイルデータ通信の通信強度やサービス名を管理し、仮想インターネット通信をバイパスする。
 */
public interface MobileDataSocket {
    /**
     * モバイルデータ通信が利用可能かどうかを取得する。
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();

    /**
     * 通信強度を取得する（0-5の範囲、0は圏外）。
     *
     * @return 通信強度
     */
    int getSignalStrength();

    /**
     * サービス名（キャリア名）を取得する。
     *
     * @return サービス名
     */
    String getServiceName();

    /**
     * データ接続状態を取得する。
     *
     * @return 接続中の場合true
     */
    boolean isConnected();
}
