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

        // テスト用の仮想Webサーバーを登録
        registerTestServers(kernel);

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
     * テスト用の仮想Webサーバーを登録する。
     *
     * @param kernel Kernelインスタンス
     */
    private static void registerTestServers(Kernel kernel) {
        VirtualRouter router = kernel.getVirtualRouter();
        if (router == null) {
            System.err.println("[ForgeNetworkInitializer] VirtualRouter is null, cannot register test servers");
            return;
        }

        // GENERIC_REQUEST ハンドラーを登録（IPvM HTTPリクエスト処理用）
        router.registerTypeHandler(VirtualPacket.PacketType.GENERIC_REQUEST, packet -> {
            handleHttpRequest(kernel, packet);
        });

        // GENERIC_RESPONSE ハンドラーを登録（レスポンスをForgeVirtualSocketに転送）
        router.registerTypeHandler(VirtualPacket.PacketType.GENERIC_RESPONSE, packet -> {
            if (currentSocket != null) {
                currentSocket.onPacketReceived(packet);
            }
        });

        // テスト用システムサーバーを登録
        router.registerSystemHandler("sys-test", packet -> {
            handleHttpRequest(kernel, packet);
        });

        // Google風テストサーバー
        router.registerSystemHandler("sys-google", packet -> {
            handleHttpRequest(kernel, packet);
        });

        System.out.println("[ForgeNetworkInitializer] Test servers registered:");
        System.out.println("[ForgeNetworkInitializer]   - 3-sys-test (Test Page)");
        System.out.println("[ForgeNetworkInitializer]   - 3-sys-google (Google-style Search)");
    }

    /**
     * HTTPリクエストを処理し、レスポンスを返す。
     */
    private static void handleHttpRequest(Kernel kernel, VirtualPacket request) {
        String path = request.getString("path");
        String originalUrl = request.getString("originalUrl");
        IPvMAddress destination = request.getDestination();

        if (path == null) path = "/";
        if (originalUrl == null) originalUrl = "http://" + destination.toString() + path;

        System.out.println("[ForgeNetworkInitializer] HTTP Request: " + originalUrl);

        String html;
        String systemId = destination.getUUID();

        // システムIDに応じてHTMLを生成
        if ("sys-test".equals(systemId)) {
            html = generateTestPage(path);
        } else if ("sys-google".equals(systemId)) {
            html = generateGoogleStylePage(path);
        } else {
            html = generateDefaultPage(systemId, path);
        }

        // レスポンスパケットを作成
        VirtualPacket response = VirtualPacket.builder()
                .source(destination)
                .destination(request.getSource())
                .type(VirtualPacket.PacketType.GENERIC_RESPONSE)
                .put("html", html)
                .put("status", "200 OK")
                .put("originalUrl", originalUrl)
                .build();

        // レスポンスをクライアントにブロードキャスト
        // サーバー側で処理されるため、全クライアントに送信
        NetworkHandler.sendToAll(response);

        // ローカルのVirtualRouterにも通知（シングルプレイヤー用）
        VirtualRouter router = kernel.getVirtualRouter();
        if (router != null) {
            router.receivePacket(response);
        }
    }

    /**
     * テストページHTMLを生成。
     */
    private static String generateTestPage(String path) {
        return "<!DOCTYPE html>" +
               "<html><head><meta charset='UTF-8'><title>IPvM Test Page</title>" +
               "<style>" +
               "body{font-family:-apple-system,sans-serif;background:#1a1a2e;color:#fff;text-align:center;padding:50px;margin:0;}" +
               "h1{color:#4ade80;font-size:2.5em;}" +
               ".card{background:#16213e;border-radius:16px;padding:30px;margin:20px auto;max-width:400px;}" +
               ".success{color:#4ade80;font-size:3em;}" +
               "code{background:#0f3460;padding:4px 8px;border-radius:4px;color:#60a5fa;}" +
               "</style></head><body>" +
               "<div class='success'>&#10003;</div>" +
               "<h1>IPvM Connection Successful!</h1>" +
               "<div class='card'>" +
               "<p>Path: <code>" + path + "</code></p>" +
               "<p>Server: <code>3-sys-test</code></p>" +
               "<p>Protocol: <code>IPvM over HTTP</code></p>" +
               "</div>" +
               "<p style='color:#888;'>MochiMobileOS Virtual Network</p>" +
               "</body></html>";
    }

    /**
     * Google風検索ページHTMLを生成。
     */
    private static String generateGoogleStylePage(String path) {
        return "<!DOCTYPE html>" +
               "<html><head><meta charset='UTF-8'><title>MochiSearch</title>" +
               "<style>" +
               "body{font-family:-apple-system,sans-serif;background:#1a1a2e;color:#fff;text-align:center;padding:100px 20px;margin:0;}" +
               "h1{font-size:3em;margin-bottom:40px;}" +
               "h1 span:nth-child(1){color:#4285f4;}" +
               "h1 span:nth-child(2){color:#ea4335;}" +
               "h1 span:nth-child(3){color:#fbbc05;}" +
               "h1 span:nth-child(4){color:#4285f4;}" +
               "h1 span:nth-child(5){color:#34a853;}" +
               "h1 span:nth-child(6){color:#ea4335;}" +
               "h1 span:nth-child(7){color:#4285f4;}" +
               "h1 span:nth-child(8){color:#34a853;}" +
               "h1 span:nth-child(9){color:#fbbc05;}" +
               "h1 span:nth-child(10){color:#ea4335;}" +
               "h1 span:nth-child(11){color:#4285f4;}" +
               ".search-box{background:#16213e;border:1px solid #333;border-radius:24px;padding:14px 20px;width:100%;max-width:500px;font-size:16px;color:#fff;outline:none;}" +
               ".search-box:focus{border-color:#4285f4;}" +
               ".info{color:#888;margin-top:40px;font-size:14px;}" +
               "</style></head><body>" +
               "<h1><span>M</span><span>o</span><span>c</span><span>h</span><span>i</span><span>S</span><span>e</span><span>a</span><span>r</span><span>c</span><span>h</span></h1>" +
               "<input type='text' class='search-box' placeholder='Search MochiNet...'>" +
               "<p class='info'>IPvM Address: 3-sys-google | Path: " + path + "</p>" +
               "</body></html>";
    }

    /**
     * デフォルトページHTMLを生成。
     */
    private static String generateDefaultPage(String systemId, String path) {
        return "<!DOCTYPE html>" +
               "<html><head><meta charset='UTF-8'><title>IPvM Server</title>" +
               "<style>" +
               "body{font-family:-apple-system,sans-serif;background:#1a1a2e;color:#fff;text-align:center;padding:50px;margin:0;}" +
               "h1{color:#60a5fa;}" +
               ".card{background:#16213e;border-radius:16px;padding:30px;margin:20px auto;max-width:400px;}" +
               "code{background:#0f3460;padding:4px 8px;border-radius:4px;color:#4ade80;}" +
               "</style></head><body>" +
               "<h1>IPvM Virtual Server</h1>" +
               "<div class='card'>" +
               "<p>System ID: <code>" + systemId + "</code></p>" +
               "<p>Path: <code>" + path + "</code></p>" +
               "</div>" +
               "</body></html>";
    }
}
