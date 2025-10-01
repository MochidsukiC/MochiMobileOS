package jp.moyashi.phoneos.forge.network;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;
import jp.moyashi.phoneos.core.service.network.VirtualRouter;

/**
 * システム通信ハンドラー
 * アプリケーションインストール要求などのシステム向け通信を処理します
 */
public class SystemPacketHandler {

    /**
     * システムアドレス: アプリケーションデータサーバー
     */
    public static final String APP_DATA_SERVER = "0000-0000-0000-0001";

    /**
     * Kernelにシステムハンドラーを登録します
     * @param kernel Kernelインスタンス
     */
    public static void registerHandlers(Kernel kernel) {
        VirtualRouter router = kernel.getVirtualRouter();
        if (router == null) {
            System.err.println("[SystemPacketHandler] VirtualRouter is null, cannot register handlers");
            return;
        }

        // アプリケーションインストール要求ハンドラー
        router.registerTypeHandler(VirtualPacket.PacketType.APP_INSTALL_REQUEST,
                SystemPacketHandler::handleAppInstallRequest);

        // データ転送ハンドラー
        router.registerTypeHandler(VirtualPacket.PacketType.DATA_TRANSFER,
                SystemPacketHandler::handleDataTransfer);

        // メッセージハンドラー
        router.registerTypeHandler(VirtualPacket.PacketType.MESSAGE,
                SystemPacketHandler::handleMessage);

        System.out.println("[SystemPacketHandler] System packet handlers registered");
    }

    /**
     * アプリケーションインストール要求を処理します
     * @param packet 受信パケット
     */
    private static void handleAppInstallRequest(VirtualPacket packet) {
        System.out.println("[SystemPacketHandler] Handling app install request from: " + packet.getSource());

        String appId = packet.getString("app_id");
        String appName = packet.getString("app_name");

        System.out.println("[SystemPacketHandler] App ID: " + appId + ", App Name: " + appName);

        // TODO: 実際のアプリケーションインストール処理を実装
        // 現在は仮の応答を送信

        // 応答パケットを作成
        VirtualPacket response = VirtualPacket.builder()
                .source(packet.getDestination())
                .destination(packet.getSource())
                .type(VirtualPacket.PacketType.APP_INSTALL_RESPONSE)
                .put("status", "success")
                .put("message", "Application install request received")
                .put("app_id", appId)
                .build();

        // TODO: 応答を送信（VirtualRouterを通じて）
        System.out.println("[SystemPacketHandler] App install response created");
    }

    /**
     * データ転送を処理します
     * @param packet 受信パケット
     */
    private static void handleDataTransfer(VirtualPacket packet) {
        System.out.println("[SystemPacketHandler] Handling data transfer from: " + packet.getSource());

        String dataType = packet.getString("data_type");
        String data = packet.getString("data");

        System.out.println("[SystemPacketHandler] Data Type: " + dataType + ", Data: " + data);

        // TODO: データ転送の処理を実装
    }

    /**
     * メッセージを処理します
     * @param packet 受信パケット
     */
    private static void handleMessage(VirtualPacket packet) {
        System.out.println("[SystemPacketHandler] Handling message from: " + packet.getSource());

        String messageText = packet.getString("message");
        String senderName = packet.getString("sender_name");
        String senderAddress = packet.getSource().toString();

        System.out.println("[SystemPacketHandler] Sender: " + senderName + ", Message: " + messageText);

        // メッセージをストレージに保存（Forgeのクライアント側で処理）
        try {
            Kernel kernel = jp.moyashi.phoneos.forge.processing.MinecraftKernelWrapper.getClientKernel();
            if (kernel != null && kernel.getMessageStorage() != null) {
                kernel.getMessageStorage().receiveMessage(senderAddress, senderName, messageText);
                System.out.println("[SystemPacketHandler] Message saved to storage");

                // 通知を追加
                if (kernel.getNotificationManager() != null) {
                    kernel.getNotificationManager().addNotification(
                        senderName,
                        "New Message",
                        messageText,
                        1  // priority: high
                    );
                    System.out.println("[SystemPacketHandler] Notification added for message");
                }
            } else {
                System.err.println("[SystemPacketHandler] Kernel or MessageStorage is null, cannot save message");
            }
        } catch (Exception e) {
            System.err.println("[SystemPacketHandler] Failed to save message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}