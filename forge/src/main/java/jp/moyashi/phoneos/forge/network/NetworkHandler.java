package jp.moyashi.phoneos.forge.network;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;
import jp.moyashi.phoneos.core.service.network.VirtualRouter;
import jp.moyashi.phoneos.forge.processing.MinecraftKernelWrapper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;

/**
 * 仮想ネットワークパケット通信を管理するハンドラー
 */
public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("mochimobileos", "virtual_network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    /**
     * パケットを登録します
     */
    public static void register() {
        INSTANCE.messageBuilder(VirtualNetworkPacket.class, packetId++)
                .encoder(VirtualNetworkPacket::encode)
                .decoder(VirtualNetworkPacket::decode)
                .consumerMainThread(VirtualNetworkPacket::handle)
                .add();

        System.out.println("[NetworkHandler] Virtual network packets registered");
    }

    /**
     * プレイヤーにパケットを送信します
     * @param packet 送信パケット
     * @param playerUUID プレイヤーのUUID
     */
    public static void sendToPlayer(VirtualPacket packet, UUID playerUUID) {
        VirtualNetworkPacket networkPacket = new VirtualNetworkPacket(packet);

        // サーバー側でプレイヤーを検索して送信
        // TODO: サーバーインスタンスからプレイヤーを取得
        System.out.println("[NetworkHandler] Sending packet to player: " + playerUUID);
        // INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), networkPacket);
    }

    /**
     * すべてのプレイヤーにパケットを送信します
     * @param packet 送信パケット
     */
    public static void sendToAll(VirtualPacket packet) {
        VirtualNetworkPacket networkPacket = new VirtualNetworkPacket(packet);
        INSTANCE.send(PacketDistributor.ALL.noArg(), networkPacket);
        System.out.println("[NetworkHandler] Broadcasting packet to all players");
    }

    /**
     * サーバーにパケットを送信します
     * @param packet 送信パケット
     */
    public static void sendToServer(VirtualPacket packet) {
        VirtualNetworkPacket networkPacket = new VirtualNetworkPacket(packet);
        INSTANCE.sendToServer(networkPacket);
        System.out.println("[NetworkHandler] Sending packet to server");
    }

    /**
     * パケット受信時の処理
     * @param packet 受信パケット
     * @param ctx ネットワークコンテキスト
     */
    public static void handleReceivedPacket(VirtualPacket packet, NetworkEvent.Context ctx) {
        System.out.println("[NetworkHandler] handleReceivedPacket called");
        System.out.println("[NetworkHandler] Received packet: " + packet.getType() +
                " from " + packet.getSource() + " to " + packet.getDestination());

        // サーバー側とクライアント側で処理を分ける
        ServerPlayer sender = ctx.getSender();
        System.out.println("[NetworkHandler] Sender: " + (sender != null ? sender.getUUID() : "null (client side)"));

        if (sender != null) {
            // サーバー側での処理
            System.out.println("[NetworkHandler] Routing to server-side handler");
            handleServerSide(packet, sender);
        } else {
            // クライアント側での処理
            System.out.println("[NetworkHandler] Routing to client-side handler");
            handleClientSide(packet);
        }
    }

    /**
     * サーバー側でのパケット処理
     * @param packet 受信パケット
     * @param sender 送信プレイヤー
     */
    private static void handleServerSide(VirtualPacket packet, ServerPlayer sender) {
        System.out.println("[NetworkHandler] Server-side handling for packet from: " + sender.getUUID());

        IPvMAddress destination = packet.getDestination();

        // 宛先に応じて処理を分岐
        if (destination.isPlayer()) {
            // プレイヤー宛て：全クライアントにブロードキャスト
            // （シングルプレイヤー環境では全員に送信）
            sendToAll(packet);
        } else if (destination.isSystem()) {
            // システム宛て：VirtualRouterで処理してから全クライアントにブロードキャスト
            Kernel kernel = MinecraftKernelWrapper.getKernelForPlayer(sender);
            if (kernel != null) {
                VirtualRouter router = kernel.getVirtualRouter();
                if (router != null) {
                    router.receivePacket(packet);
                }
            }
            // システム応答も全クライアントに送信
            sendToAll(packet);
        } else if (destination.isServer()) {
            // 外部Mod宛て：VirtualNetworkRegistryで処理してから全クライアントにブロードキャスト
            VirtualNetworkRegistry.handlePacket(packet);
            sendToAll(packet);
        }
    }

    /**
     * クライアント側でのパケット処理
     * @param packet 受信パケット
     */
    private static void handleClientSide(VirtualPacket packet) {
        System.out.println("[NetworkHandler] Client-side handling for packet");

        // クライアント側のKernelを取得
        Kernel kernel = MinecraftKernelWrapper.getClientKernel();
        if (kernel != null) {
            VirtualRouter router = kernel.getVirtualRouter();
            if (router != null) {
                router.receivePacket(packet);
            }
        }
    }

    /**
     * VirtualRouterの外部送信ハンドラーを設定します
     * @param kernel Kernelインスタンス
     */
    public static void setupVirtualRouter(Kernel kernel) {
        VirtualRouter router = kernel.getVirtualRouter();
        if (router != null) {
            router.setExternalSendHandler(packet -> {
                IPvMAddress destination = packet.getDestination();

                if (destination.isPlayer()) {
                    // プレイヤー宛て：サーバーに送信してサーバーが配送
                    sendToServer(packet);
                } else if (destination.isServer()) {
                    // 外部Mod宛て：サーバーに送信
                    sendToServer(packet);
                } else {
                    System.err.println("[NetworkHandler] Unknown destination type: " + destination);
                }
            });

            System.out.println("[NetworkHandler] VirtualRouter external send handler configured");
        }
    }
}