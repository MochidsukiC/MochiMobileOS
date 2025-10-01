package jp.moyashi.phoneos.forge.network;

import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 外部Modが仮想ネットワークに登録するためのレジストリ
 */
public class VirtualNetworkRegistry {

    /**
     * パケットハンドラーインターフェース
     */
    @FunctionalInterface
    public interface PacketHandler {
        /**
         * パケットを処理します
         * @param packet 受信パケット
         */
        void handle(VirtualPacket packet);
    }

    // サーバーID -> パケットハンドラー のマッピング
    private static final Map<String, PacketHandler> serverHandlers = new HashMap<>();

    // サーバーID -> IPvMAddress のマッピング
    private static final Map<String, IPvMAddress> serverAddresses = new HashMap<>();

    // 次に割り当てるサーバーID（自動インクリメント）
    private static int nextServerId = 1;

    /**
     * 外部Modをサーバーとして登録します
     * @param modId ModのID
     * @param handler パケットハンドラー
     * @return 割り当てられたIPvMAddress
     */
    public static synchronized IPvMAddress registerServer(String modId, PacketHandler handler) {
        if (modId == null || modId.isEmpty()) {
            throw new IllegalArgumentException("Mod ID cannot be null or empty");
        }
        if (handler == null) {
            throw new IllegalArgumentException("Handler cannot be null");
        }

        // 既に登録されている場合はそのアドレスを返す
        if (serverAddresses.containsKey(modId)) {
            System.out.println("[VirtualNetworkRegistry] Mod '" + modId + "' is already registered");
            return serverAddresses.get(modId);
        }

        // 新しいサーバーIDを生成（16進数形式: 0000-0000-0000-XXXX）
        String serverId = String.format("0000-0000-0000-%04X", nextServerId++);
        IPvMAddress address = IPvMAddress.forServer(serverId);

        // 登録
        serverHandlers.put(serverId, handler);
        serverAddresses.put(modId, address);

        System.out.println("[VirtualNetworkRegistry] Registered server: " + modId + " -> " + address);
        return address;
    }

    /**
     * 外部Modの登録を解除します
     * @param modId ModのID
     */
    public static synchronized void unregisterServer(String modId) {
        IPvMAddress address = serverAddresses.remove(modId);
        if (address != null) {
            serverHandlers.remove(address.getUUID());
            System.out.println("[VirtualNetworkRegistry] Unregistered server: " + modId);
        }
    }

    /**
     * サーバー宛てのパケットを処理します
     * @param packet 受信パケット
     */
    public static void handlePacket(VirtualPacket packet) {
        IPvMAddress destination = packet.getDestination();
        if (!destination.isServer()) {
            System.err.println("[VirtualNetworkRegistry] Packet destination is not a server: " + destination);
            return;
        }

        String serverId = destination.getUUID();
        PacketHandler handler = serverHandlers.get(serverId);

        if (handler != null) {
            try {
                handler.handle(packet);
                System.out.println("[VirtualNetworkRegistry] Packet handled by server: " + serverId);
            } catch (Exception e) {
                System.err.println("[VirtualNetworkRegistry] Error handling packet: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("[VirtualNetworkRegistry] No handler registered for server: " + serverId);
        }
    }

    /**
     * ModIDからIPvMAddressを取得します
     * @param modId ModのID
     * @return IPvMAddress、登録されていない場合はnull
     */
    public static IPvMAddress getServerAddress(String modId) {
        return serverAddresses.get(modId);
    }

    /**
     * 登録されているすべてのサーバーのModIDを取得します
     * @return ModIDの配列
     */
    public static String[] getRegisteredModIds() {
        return serverAddresses.keySet().toArray(new String[0]);
    }

    /**
     * レジストリをクリアします（デバッグ用）
     */
    public static synchronized void clear() {
        serverHandlers.clear();
        serverAddresses.clear();
        nextServerId = 1;
        System.out.println("[VirtualNetworkRegistry] Registry cleared");
    }

    /**
     * 登録されているサーバーの数を取得します
     * @return サーバー数
     */
    public static int getServerCount() {
        return serverAddresses.size();
    }
}