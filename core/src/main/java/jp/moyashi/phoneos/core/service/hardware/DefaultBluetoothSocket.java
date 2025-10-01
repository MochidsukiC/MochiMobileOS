package jp.moyashi.phoneos.core.service.hardware;

import java.util.ArrayList;
import java.util.List;

/**
 * Bluetooth通信ソケットのデフォルト実装（standalone用）。
 * 常にNOTFOUNDを返す。
 */
public class DefaultBluetoothSocket implements BluetoothSocket {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<BluetoothDevice> scanNearbyDevices() {
        return new ArrayList<>(); // 空のリスト
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        return new ArrayList<>(); // 空のリスト
    }

    @Override
    public boolean connect(String address) {
        return false;
    }

    @Override
    public void disconnect(String address) {
        // 何もしない
    }
}