package jp.moyashi.phoneos.forge.network;

import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.NetworkException;
import jp.moyashi.phoneos.core.service.network.NetworkStatus;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;
import jp.moyashi.phoneos.core.service.network.VirtualSocket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Forge環境用の仮想ネットワークソケット実装。
 * Minecraft ForgeのSimpleChannelを使用してパケット通信を行う。
 *
 * <p>電波強度はY座標とディメンションに基づいて計算される:</p>
 * <ul>
 *   <li>Y=60-100: 強い (4-5)</li>
 *   <li>Y=100+: 中程度 (3-4)</li>
 *   <li>Y=30-60: 弱い (2-3)</li>
 *   <li>Y=30未満: 非常に弱い (0-1)</li>
 * </ul>
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class ForgeVirtualSocket implements VirtualSocket {

    private Consumer<VirtualPacket> packetListener;
    private boolean connected = false;

    // HTTPリクエストのレスポンス待ち用マップ
    private final Map<String, CompletableFuture<VirtualHttpResponse>> pendingRequests = new ConcurrentHashMap<>();

    // タイムアウト設定（秒）
    private static final int REQUEST_TIMEOUT_SECONDS = 10;

    /**
     * ForgeVirtualSocketを構築する。
     */
    public ForgeVirtualSocket() {
        this.connected = true;
        log("ForgeVirtualSocket initialized");
    }

    @Override
    public boolean isAvailable() {
        // Minecraftクライアントが接続中かつワールドが存在するか確認
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) {
            return false;
        }

        // 電波強度が0より大きければ利用可能
        return connected && getSignalStrength() > 0;
    }

    @Override
    public NetworkStatus getStatus() {
        if (!connected) {
            return NetworkStatus.OFFLINE;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) {
            return NetworkStatus.NO_SERVICE;
        }

        int signal = getSignalStrength();
        if (signal == 0) {
            return NetworkStatus.NO_SERVICE;
        }

        return NetworkStatus.CONNECTED;
    }

    @Override
    public CompletableFuture<Boolean> sendPacket(VirtualPacket packet) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isAvailable()) {
                    log("Cannot send packet - network not available");
                    return false;
                }

                // NetworkHandler経由でパケットを送信
                NetworkHandler.sendToServer(packet);
                return true;
            } catch (Exception e) {
                logError("Failed to send packet: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public void setPacketListener(Consumer<VirtualPacket> listener) {
        this.packetListener = listener;
    }

    /**
     * パケットを受信した際に呼び出される。
     * NetworkHandler.handleClientSide()から呼び出される。
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

        // ソースアドレスを取得（プレイヤーUUID）
        IPvMAddress sourceAddress = getPlayerAddress();

        // HTTPリクエストパケットを作成
        VirtualPacket packet = VirtualPacket.builder()
                .source(sourceAddress)
                .destination(destination)
                .type(VirtualPacket.PacketType.GENERIC_REQUEST)
                .put("path", path)
                .put("method", method)
                .put("originalUrl", originalUrl)
                .build();

        // パケットを送信
        NetworkHandler.sendToServer(packet);

        return future;
    }

    @Override
    public void close() {
        connected = false;
        pendingRequests.clear();
        log("ForgeVirtualSocket closed");
    }

    @Override
    public int getSignalStrength() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return 0;
        }

        LocalPlayer player = mc.player;
        double y = player.getY();

        // Y座標に基づいて電波強度を計算
        // 地上付近（Y=60-100）が最も強い
        if (y >= 60 && y <= 100) {
            return 5; // 最強
        } else if (y > 100 && y <= 200) {
            return 4; // 強い（高所）
        } else if (y >= 30 && y < 60) {
            return 3; // 中程度（地下浅部）
        } else if (y >= 0 && y < 30) {
            return 2; // 弱い（地下深部）
        } else if (y < 0) {
            return 1; // 非常に弱い（深層）
        } else {
            return 4; // 高度が非常に高い場合
        }
    }

    @Override
    public String getCarrierName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) {
            return "No Service";
        }

        Level level = mc.level;
        String dimensionName = level.dimension().location().getPath();

        // ディメンションに基づいてキャリア名を決定
        switch (dimensionName) {
            case "overworld":
                return "Overworld Network";
            case "the_nether":
                return "Nether Network";
            case "the_end":
                return "End Network";
            default:
                return dimensionName + " Network";
        }
    }

    /**
     * 現在のプレイヤーのIPvMアドレスを取得する。
     */
    private IPvMAddress getPlayerAddress() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            return IPvMAddress.forPlayer(mc.player.getUUID().toString());
        }
        // フォールバック
        return IPvMAddress.forSystem("browser");
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
        System.out.println("[ForgeVirtualSocket] " + message);
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        System.err.println("[ForgeVirtualSocket] " + message);
    }
}
