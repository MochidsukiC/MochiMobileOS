package jp.moyashi.phoneos.forge.hardware;

import jp.moyashi.phoneos.core.service.hardware.BluetoothSocket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Forge環境用のBluetooth通信ソケット実装。
 * 半径10m以内のプレイヤーやエンティティをBluetoothデバイスとして検出する。
 */
public class ForgeBluetoothSocket implements BluetoothSocket {
    private Player player;
    private Level level;
    private Map<String, BluetoothDevice> connectedDevices;
    private static final double BLUETOOTH_RANGE = 10.0; // 10ブロック = 10m

    public ForgeBluetoothSocket(Player player) {
        this.player = player;
        this.level = player != null ? player.level() : null;
        this.connectedDevices = new HashMap<>();
    }

    /**
     * プレイヤーを更新する（ワールド変更時など）。
     */
    public void updatePlayer(Player player) {
        this.player = player;
        this.level = player != null ? player.level() : null;
        // ワールド変更時は接続をクリア
        this.connectedDevices.clear();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public List<BluetoothDevice> scanNearbyDevices() {
        List<BluetoothDevice> devices = new ArrayList<>();

        if (player == null || level == null) {
            return devices;
        }

        // プレイヤーの位置を中心とした範囲内のエンティティを取得
        AABB searchBox = new AABB(
            player.getX() - BLUETOOTH_RANGE,
            player.getY() - BLUETOOTH_RANGE,
            player.getZ() - BLUETOOTH_RANGE,
            player.getX() + BLUETOOTH_RANGE,
            player.getY() + BLUETOOTH_RANGE,
            player.getZ() + BLUETOOTH_RANGE
        );

        // 範囲内のすべてのエンティティを取得
        List<Entity> entities = level.getEntities(player, searchBox);

        for (Entity entity : entities) {
            // 自分自身は除外
            if (entity == player) {
                continue;
            }

            // 距離を計算
            double distance = player.distanceTo(entity);

            if (distance <= BLUETOOTH_RANGE) {
                String name;
                String address;

                if (entity instanceof Player) {
                    // プレイヤーの場合
                    Player otherPlayer = (Player) entity;
                    name = otherPlayer.getName().getString() + "'s Phone";
                    address = "BT-" + otherPlayer.getUUID().toString().substring(0, 8);
                } else {
                    // その他のエンティティの場合
                    name = entity.getType().toString() + " Device";
                    address = "BT-" + entity.getUUID().toString().substring(0, 8);
                }

                devices.add(new BluetoothDevice(name, address, distance));
            }
        }

        return devices;
    }

    @Override
    public List<BluetoothDevice> getConnectedDevices() {
        // 接続デバイスのリストを返す
        // 範囲外に出たデバイスは削除
        List<BluetoothDevice> stillConnected = new ArrayList<>();

        for (BluetoothDevice device : connectedDevices.values()) {
            // デバイスがまだ範囲内にあるか確認
            List<BluetoothDevice> nearby = scanNearbyDevices();
            boolean inRange = false;

            for (BluetoothDevice nearbyDevice : nearby) {
                if (nearbyDevice.address.equals(device.address)) {
                    stillConnected.add(nearbyDevice);
                    inRange = true;
                    break;
                }
            }

            if (!inRange) {
                // 範囲外に出た場合は接続を切断
                connectedDevices.remove(device.address);
            }
        }

        return stillConnected;
    }

    @Override
    public boolean connect(String address) {
        // 範囲内のデバイスを検索
        List<BluetoothDevice> nearby = scanNearbyDevices();

        for (BluetoothDevice device : nearby) {
            if (device.address.equals(address)) {
                // 接続成功
                connectedDevices.put(address, device);
                System.out.println("ForgeBluetoothSocket: Connected to " + device.name);
                return true;
            }
        }

        System.out.println("ForgeBluetoothSocket: Device not found: " + address);
        return false;
    }

    @Override
    public void disconnect(String address) {
        if (connectedDevices.containsKey(address)) {
            BluetoothDevice device = connectedDevices.remove(address);
            System.out.println("ForgeBluetoothSocket: Disconnected from " + device.name);
        }
    }
}