package jp.moyashi.phoneos.core.service.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 仮想ネットワークルーターサービス
 * パケットのルーティングとハンドリングを管理します
 */
public class VirtualRouter {

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

    /**
     * 外部送信ハンドラーインターフェース
     * Forge側でMinecraftパケット送信を実装するために使用
     */
    @FunctionalInterface
    public interface ExternalSendHandler {
        /**
         * パケットを外部（プレイヤーや外部Mod）に送信します
         * @param packet 送信パケット
         */
        void send(VirtualPacket packet);
    }

    // システムアドレスごとのパケットハンドラー
    private final Map<String, PacketHandler> systemHandlers = new HashMap<>();

    // パケットタイプごとのパケットハンドラー
    private final Map<VirtualPacket.PacketType, List<PacketHandler>> typeHandlers = new HashMap<>();

    // 外部送信ハンドラー（Forge側から設定される）
    private ExternalSendHandler externalSendHandler = null;

    /**
     * VirtualRouterを構築します
     */
    public VirtualRouter() {
        // デフォルトのシステムハンドラーを登録
        registerSystemHandler("0000-0000-0000-0001", this::handleAppInstallRequest);
    }

    /**
     * パケットを送信します
     * @param packet 送信するパケット
     */
    public void sendPacket(VirtualPacket packet) {
        if (packet == null) {
            throw new IllegalArgumentException("Packet cannot be null");
        }

        IPvMAddress destination = packet.getDestination();

        // ルーティング処理
        if (destination.isSystem()) {
            // システム宛ての通信：内部で処理
            routeToSystem(packet);
        } else if (destination.isPlayer() || destination.isServer()) {
            // プレイヤー/サーバー宛ての通信：外部ハンドラーに委譲
            if (externalSendHandler != null) {
                externalSendHandler.send(packet);
            } else {
                System.err.println("[VirtualRouter] External send handler not set. Cannot send packet to: " + destination);
            }
        } else {
            System.err.println("[VirtualRouter] Unknown destination type: " + destination);
        }
    }

    /**
     * 外部からパケットを受信します（Forge側から呼ばれる）
     * @param packet 受信パケット
     */
    public void receivePacket(VirtualPacket packet) {
        if (packet == null) {
            return;
        }

        IPvMAddress destination = packet.getDestination();

        // 宛先に応じて処理
        if (destination.isSystem()) {
            routeToSystem(packet);
        } else {
            // その他の宛先は、タイプハンドラーで処理
            notifyTypeHandlers(packet);
        }
    }

    /**
     * システム宛てのパケットをルーティングします
     * @param packet パケット
     */
    private void routeToSystem(VirtualPacket packet) {
        String systemId = packet.getDestination().getUUID();
        PacketHandler handler = systemHandlers.get(systemId);

        if (handler != null) {
            try {
                handler.handle(packet);
            } catch (Exception e) {
                System.err.println("[VirtualRouter] Error handling system packet: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("[VirtualRouter] No handler registered for system ID: " + systemId);
        }

        // タイプハンドラーにも通知
        notifyTypeHandlers(packet);
    }

    /**
     * タイプハンドラーに通知します
     * @param packet パケット
     */
    private void notifyTypeHandlers(VirtualPacket packet) {
        List<PacketHandler> handlers = typeHandlers.get(packet.getType());
        if (handlers != null) {
            for (PacketHandler handler : handlers) {
                try {
                    handler.handle(packet);
                } catch (Exception e) {
                    System.err.println("[VirtualRouter] Error in type handler: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * システムアドレス用のハンドラーを登録します
     * @param systemId システムID
     * @param handler ハンドラー
     */
    public void registerSystemHandler(String systemId, PacketHandler handler) {
        if (systemId == null || handler == null) {
            throw new IllegalArgumentException("System ID and handler cannot be null");
        }
        systemHandlers.put(systemId, handler);
    }

    /**
     * パケットタイプ用のハンドラーを登録します
     * @param type パケットタイプ
     * @param handler ハンドラー
     */
    public void registerTypeHandler(VirtualPacket.PacketType type, PacketHandler handler) {
        if (type == null || handler == null) {
            throw new IllegalArgumentException("Type and handler cannot be null");
        }
        typeHandlers.computeIfAbsent(type, k -> new ArrayList<>()).add(handler);
    }

    /**
     * 外部送信ハンドラーを設定します（Forge側から呼ばれる）
     * @param handler 外部送信ハンドラー
     */
    public void setExternalSendHandler(ExternalSendHandler handler) {
        this.externalSendHandler = handler;
    }

    /**
     * アプリケーションインストール要求を処理します
     * @param packet パケット
     */
    private void handleAppInstallRequest(VirtualPacket packet) {
        System.out.println("[VirtualRouter] Handling app install request from: " + packet.getSource());

        // TODO: 実際のアプリケーションインストール処理を実装
        // 仮の応答を送信
        VirtualPacket response = VirtualPacket.builder()
                .source(packet.getDestination())
                .destination(packet.getSource())
                .type(VirtualPacket.PacketType.APP_INSTALL_RESPONSE)
                .put("status", "success")
                .put("message", "Application install request received")
                .build();

        sendPacket(response);
    }

    /**
     * 特定のシステムアドレスを取得します
     * @param systemId システムID
     * @return IPvMAddress
     */
    public static IPvMAddress getSystemAddress(String systemId) {
        return IPvMAddress.forSystem(systemId);
    }

    /**
     * アプリケーションデータサーバーアドレスを取得します
     * @return アプリケーションデータサーバーのIPvMAddress
     */
    public static IPvMAddress getAppDataServerAddress() {
        return IPvMAddress.forSystem("0000-0000-0000-0001");
    }
}