package jp.moyashi.phoneos.forge.network;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.NetworkAdapter;
import jp.moyashi.phoneos.core.service.network.VirtualAdapter;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;
import jp.moyashi.phoneos.core.service.network.VirtualRouter;

/**
 * Forge環境のネットワーク初期化を行うクラス。
 * Kernel初期化後に呼び出され、ForgeVirtualSocketをNetworkAdapterにバインドする。
 *
 * <p>使用方法（MinecraftKernelWrapper等から呼び出し）:</p>
 * <pre>{@code
 * Kernel kernel = new Kernel(...);
 * kernel.setup();
 *
 * // ネットワークをバインド
 * ForgeNetworkInitializer.initialize(kernel);
 * }</pre>
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class ForgeNetworkInitializer {

    private static ForgeVirtualSocket currentSocket = null;

    /**
     * Forge環境のネットワークを初期化する。
     * KernelのNetworkAdapterにForgeVirtualSocketをバインドする。
     *
     * @param kernel Kernelインスタンス
     */
    public static void initialize(Kernel kernel) {
        if (kernel == null) {
            System.err.println("[ForgeNetworkInitializer] Kernel is null, cannot initialize network");
            return;
        }

        NetworkAdapter networkAdapter = kernel.getNetworkAdapter();
        if (networkAdapter == null) {
            System.err.println("[ForgeNetworkInitializer] NetworkAdapter is null, cannot initialize network");
            return;
        }

        VirtualAdapter virtualAdapter = networkAdapter.getVirtualAdapter();
        if (virtualAdapter == null) {
            System.err.println("[ForgeNetworkInitializer] VirtualAdapter is null, cannot initialize network");
            return;
        }

        // 既存のソケットがあれば閉じる
        if (currentSocket != null) {
            currentSocket.close();
        }

        // ForgeVirtualSocketを作成してバインド
        currentSocket = new ForgeVirtualSocket();
        virtualAdapter.setSocket(currentSocket);

        // VirtualRouter用の外部送信ハンドラーも設定（後方互換性）
        NetworkHandler.setupVirtualRouter(kernel);

        // レスポンスハンドラーを登録
        registerResponseHandlers(kernel);

        System.out.println("[ForgeNetworkInitializer] Network initialized successfully");
        System.out.println("[ForgeNetworkInitializer]   - VirtualSocket: ForgeVirtualSocket");
        System.out.println("[ForgeNetworkInitializer]   - Status: " + virtualAdapter.getStatus().getDisplayName());
        System.out.println("[ForgeNetworkInitializer]   - Carrier: " + virtualAdapter.getCarrierName());
    }

    /**
     * パケットを受信した際に呼び出される。
     * NetworkHandler.handleClientSide()から呼び出される。
     *
     * @param packet 受信パケット
     */
    public static void onPacketReceived(jp.moyashi.phoneos.core.service.network.VirtualPacket packet) {
        if (currentSocket != null) {
            currentSocket.onPacketReceived(packet);
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
        System.out.println("[ForgeNetworkInitializer] Network shutdown");
    }

    /**
     * 現在のForgeVirtualSocketを取得する。
     *
     * @return ForgeVirtualSocket（未初期化の場合null）
     */
    public static ForgeVirtualSocket getCurrentSocket() {
        return currentSocket;
    }

    /**
     * クライアント側のVirtualRouterにレスポンスハンドラーを登録する。
     * サーバーからのGENERIC_RESPONSEをForgeVirtualSocketに転送する。
     *
     * @param kernel Kernelインスタンス
     */
    private static void registerResponseHandlers(Kernel kernel) {
        VirtualRouter router = kernel.getVirtualRouter();
        if (router == null) {
            System.err.println("[ForgeNetworkInitializer] VirtualRouter is null, cannot register response handlers");
            return;
        }

        // GENERIC_RESPONSE ハンドラーを登録（レスポンスをForgeVirtualSocketに転送）
        router.registerTypeHandler(VirtualPacket.PacketType.GENERIC_RESPONSE, packet -> {
            if (currentSocket != null) {
                currentSocket.onPacketReceived(packet);
            }
        });

        System.out.println("[ForgeNetworkInitializer] Response handlers registered");
        System.out.println("[ForgeNetworkInitializer] Note: System servers (3-sys-*) are managed by server module");
    }
}
