package jp.moyashi.phoneos.standalone.network;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.network.NetworkAdapter;
import jp.moyashi.phoneos.core.service.network.VirtualAdapter;

/**
 * Standalone環境のネットワーク初期化を行うクラス。
 * Kernel初期化後に呼び出され、StandaloneVirtualSocketをNetworkAdapterにバインドする。
 *
 * <p>使用方法（Standalone.java等から呼び出し）:</p>
 * <pre>{@code
 * Kernel kernel = new Kernel(...);
 * kernel.setup();
 *
 * // ネットワークをバインド
 * StandaloneNetworkInitializer.initialize(kernel);
 * }</pre>
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class StandaloneNetworkInitializer {

    private static StandaloneVirtualSocket currentSocket = null;

    /**
     * Standalone環境のネットワークを初期化する。
     * KernelのNetworkAdapterにStandaloneVirtualSocketをバインドする。
     *
     * @param kernel Kernelインスタンス
     */
    public static void initialize(Kernel kernel) {
        if (kernel == null) {
            System.err.println("[StandaloneNetworkInitializer] Kernel is null, cannot initialize network");
            return;
        }

        NetworkAdapter networkAdapter = kernel.getNetworkAdapter();
        if (networkAdapter == null) {
            System.err.println("[StandaloneNetworkInitializer] NetworkAdapter is null, cannot initialize network");
            return;
        }

        VirtualAdapter virtualAdapter = networkAdapter.getVirtualAdapter();
        if (virtualAdapter == null) {
            System.err.println("[StandaloneNetworkInitializer] VirtualAdapter is null, cannot initialize network");
            return;
        }

        // 既存のソケットがあれば閉じる
        if (currentSocket != null) {
            currentSocket.close();
        }

        // StandaloneVirtualSocketを作成してバインド
        currentSocket = new StandaloneVirtualSocket(kernel);
        virtualAdapter.setSocket(currentSocket);

        // VirtualRouterにパケット受信リスナーを設定
        setupVirtualRouterListener(kernel);

        System.out.println("[StandaloneNetworkInitializer] Network initialized successfully");
        System.out.println("[StandaloneNetworkInitializer]   - VirtualSocket: StandaloneVirtualSocket");
        System.out.println("[StandaloneNetworkInitializer]   - Status: " + virtualAdapter.getStatus().getDisplayName());
        System.out.println("[StandaloneNetworkInitializer]   - Carrier: " + virtualAdapter.getCarrierName());
    }

    /**
     * VirtualRouterにパケット受信リスナーを設定する。
     * VirtualRouterからの応答をStandaloneVirtualSocketに転送する。
     */
    private static void setupVirtualRouterListener(Kernel kernel) {
        if (kernel.getVirtualRouter() != null && currentSocket != null) {
            // GENERIC_RESPONSEタイプのパケットをソケットに転送
            kernel.getVirtualRouter().registerTypeHandler(
                    jp.moyashi.phoneos.core.service.network.VirtualPacket.PacketType.GENERIC_RESPONSE,
                    packet -> currentSocket.onPacketReceived(packet)
            );
        }
    }

    /**
     * ネットワークをシャットダウンする。
     */
    public static void shutdown() {
        if (currentSocket != null) {
            currentSocket.close();
            currentSocket = null;
        }
        System.out.println("[StandaloneNetworkInitializer] Network shutdown");
    }

    /**
     * 現在のStandaloneVirtualSocketを取得する。
     *
     * @return StandaloneVirtualSocket（未初期化の場合null）
     */
    public static StandaloneVirtualSocket getCurrentSocket() {
        return currentSocket;
    }
}
