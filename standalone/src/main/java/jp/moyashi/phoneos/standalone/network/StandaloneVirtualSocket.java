package jp.moyashi.phoneos.standalone.network;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.NetworkException;
import jp.moyashi.phoneos.core.service.network.NetworkStatus;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;
import jp.moyashi.phoneos.core.service.network.VirtualRouter;
import jp.moyashi.phoneos.core.service.network.VirtualSocket;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Standalone環境用の仮想ネットワークソケット実装。
 * シングルプレイヤー環境として、ローカルでパケットルーティングを行う。
 *
 * <p>Standalone環境では:</p>
 * <ul>
 *   <li>すべてのパケットがローカルで処理される</li>
 *   <li>電波強度は常に最大（5）</li>
 *   <li>VirtualRouterに直接パケットを渡す</li>
 * </ul>
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class StandaloneVirtualSocket implements VirtualSocket {

    private final Kernel kernel;
    private Consumer<VirtualPacket> packetListener;
    private boolean connected = false;

    // HTTPリクエストのレスポンス待ち用マップ
    private final Map<String, CompletableFuture<VirtualHttpResponse>> pendingRequests = new ConcurrentHashMap<>();

    // タイムアウト設定（秒）
    private static final int REQUEST_TIMEOUT_SECONDS = 10;

    /**
     * StandaloneVirtualSocketを構築する。
     *
     * @param kernel Kernelインスタンス
     */
    public StandaloneVirtualSocket(Kernel kernel) {
        this.kernel = kernel;
        this.connected = true;
        log("StandaloneVirtualSocket initialized");
    }

    @Override
    public boolean isAvailable() {
        return connected;
    }

    @Override
    public NetworkStatus getStatus() {
        return connected ? NetworkStatus.CONNECTED : NetworkStatus.OFFLINE;
    }

    @Override
    public CompletableFuture<Boolean> sendPacket(VirtualPacket packet) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isAvailable()) {
                    log("Cannot send packet - network not available");
                    return false;
                }

                // Standalone環境ではローカルで処理
                routeLocally(packet);
                return true;
            } catch (Exception e) {
                logError("Failed to send packet: " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * パケットをローカルでルーティングする。
     */
    private void routeLocally(VirtualPacket packet) {
        VirtualRouter router = kernel.getVirtualRouter();
        if (router != null) {
            // VirtualRouterでパケットを処理
            router.receivePacket(packet);
        } else {
            logError("VirtualRouter is null, cannot route packet");
        }
    }

    @Override
    public void setPacketListener(Consumer<VirtualPacket> listener) {
        this.packetListener = listener;
    }

    /**
     * パケットを受信した際に呼び出される。
     * VirtualRouterからの応答を処理する。
     *
     * @param packet 受信パケット
     */
    public void onPacketReceived(VirtualPacket packet) {
        // HTTPレスポンスの場合、待機中のリクエストに通知
        if (packet.getType() == VirtualPacket.PacketType.GENERIC_RESPONSE) {
            String originalUrl = packet.getString("originalUrl");
            if (originalUrl != null) {
                CompletableFuture<VirtualHttpResponse> future = pendingRequests.remove(originalUrl);
                if (future != null) {
                    String html = packet.getString("html");
                    String status = packet.getString("status");
                    int statusCode = parseStatusCode(status);

                    VirtualHttpResponse response;
                    if (html != null && !html.isEmpty()) {
                        response = new VirtualHttpResponse(statusCode, getStatusText(statusCode), "text/html", html);
                    } else {
                        response = VirtualHttpResponse.notFound("Page not found");
                    }
                    future.complete(response);
                }
            }
        }

        // リスナーに通知
        if (packetListener != null) {
            packetListener.accept(packet);
        }
    }

    @Override
    public CompletableFuture<VirtualHttpResponse> httpRequest(IPvMAddress destination, String path, String method)
            throws NetworkException {

        if (!isAvailable()) {
            throw NetworkException.noService();
        }

        String originalUrl = "http://" + destination.toString() + path;
        log("HTTP request: " + method + " " + originalUrl);

        // レスポンス待ち用のFutureを作成
        CompletableFuture<VirtualHttpResponse> future = new CompletableFuture<>();
        pendingRequests.put(originalUrl, future);

        // タイムアウト設定
        future.orTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .exceptionally(e -> {
                    pendingRequests.remove(originalUrl);
                    return VirtualHttpResponse.internalError("Request timeout: " + originalUrl);
                });

        // ソースアドレス（Standalone用のシステムアドレス）
        IPvMAddress sourceAddress = IPvMAddress.forSystem("standalone-browser");

        // HTTPリクエストパケットを作成
        VirtualPacket packet = VirtualPacket.builder()
                .source(sourceAddress)
                .destination(destination)
                .type(VirtualPacket.PacketType.GENERIC_REQUEST)
                .put("path", path)
                .put("method", method)
                .put("originalUrl", originalUrl)
                .build();

        // ローカルでルーティング
        routeLocally(packet);

        return future;
    }

    @Override
    public void close() {
        connected = false;
        pendingRequests.clear();
        log("StandaloneVirtualSocket closed");
    }

    @Override
    public int getSignalStrength() {
        // Standalone環境では常に最大
        return connected ? 5 : 0;
    }

    @Override
    public String getCarrierName() {
        return "Standalone Network";
    }

    /**
     * ステータス文字列からステータスコードをパースする。
     */
    private int parseStatusCode(String status) {
        if (status == null || status.isEmpty()) {
            return 200;
        }
        try {
            String[] parts = status.split(" ");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            return 200;
        }
    }

    /**
     * ステータスコードからステータステキストを取得する。
     */
    private String getStatusText(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            default: return "Unknown";
        }
    }

    /**
     * ログ出力。
     */
    private void log(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().debug("StandaloneVirtualSocket", message);
        } else {
            System.out.println("[StandaloneVirtualSocket] " + message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("StandaloneVirtualSocket", message);
        } else {
            System.err.println("[StandaloneVirtualSocket] " + message);
        }
    }
}
