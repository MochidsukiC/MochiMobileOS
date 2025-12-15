package jp.moyashi.phoneos.api.hardware;

import java.util.List;

/**
 * Bluetooth通信ソケットAPI。
 * 周囲のBluetoothデバイスや接続済みのデバイスを管理し、Bluetooth通信をバイパスする。
 */
public interface BluetoothSocket {
    /**
     * Bluetoothデバイス情報クラス。
     */
    class BluetoothDevice {
        public final String name;
        public final String address;
        public final double distance;

        public BluetoothDevice(String name, String address, double distance) {
            this.name = name;
            this.address = address;
            this.distance = distance;
        }
    }

    /**
     * Bluetoothが利用可能かどうかを取得する。
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();

    /**
     * 周囲のBluetoothデバイスを検索する。
     *
     * @return 検出されたデバイスのリスト
     */
    List<BluetoothDevice> scanNearbyDevices();

    /**
     * 接続済みのBluetoothデバイスを取得する。
     *
     * @return 接続済みデバイスのリスト
     */
    List<BluetoothDevice> getConnectedDevices();

    /**
     * 指定されたデバイスに接続する。
     *
     * @param address デバイスアドレス
     * @return 接続成功の場合true
     */
    boolean connect(String address);

    /**
     * 指定されたデバイスから切断する。
     *
     * @param address デバイスアドレス
     */
    void disconnect(String address);
}
