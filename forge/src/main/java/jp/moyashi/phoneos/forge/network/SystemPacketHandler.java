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
    }

    /**
     * アプリケーションインストール要求を処理します
     * @param packet 受信パケット
     */
    private static void handleAppInstallRequest(VirtualPacket packet) {
        String appId = packet.getString("app_id");
        String appName = packet.getString("app_name");

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
    }

    /**
     * データ転送を処理します
     * @param packet 受信パケット
     */
    private static void handleDataTransfer(VirtualPacket packet) {
        String dataType = packet.getString("data_type");
        String data = packet.getString("data");

        // TODO: データ転送の処理を実装
    }

    /**
     * メッセージを処理します
     * @param packet 受信パケット
     */
    private static void handleMessage(VirtualPacket packet) {
        String messageText = packet.getString("message");
        String senderName = packet.getString("sender_name");
        String senderAddress = packet.getSource().toString();

        // メッセージをストレージに保存（Forgeのクライアント側で処理）
        try {
            Kernel kernel = jp.moyashi.phoneos.forge.processing.MinecraftKernelWrapper.getClientKernel();
            if (kernel != null && kernel.getMessageStorage() != null) {
                kernel.getMessageStorage().receiveMessage(senderAddress, senderName, messageText);

                // 通知を追加
                if (kernel.getNotificationManager() != null) {
                    kernel.getNotificationManager().addNotification(
                        senderName,
                        "New Message",
                        messageText,
                        1  // priority: high
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}