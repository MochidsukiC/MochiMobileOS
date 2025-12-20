package jp.moyashi.phoneos.server.network;

import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;

import java.util.Map;
import java.util.Optional;

/**
 * サーバーサイドの仮想ネットワークルーター。
 * クライアントからのVirtualPacketを受信し、適切なVirtualHttpServerにルーティングする。
 *
 * フロー:
 * 1. ForgePacketHandler がクライアントからパケットを受信
 * 2. ServerVirtualRouter.routeRequest() を呼び出し
 * 3. 登録されたVirtualHttpServerを検索
 * 4. レスポンスをクライアントに返送
 */
public class ServerVirtualRouter {

    private static final ServerVirtualRouter INSTANCE = new ServerVirtualRouter();

    private final SystemServerRegistry registry;

    private ServerVirtualRouter() {
        this.registry = SystemServerRegistry.getInstance();
    }

    /**
     * シングルトンインスタンスを取得
     */
    public static ServerVirtualRouter getInstance() {
        return INSTANCE;
    }

    /**
     * HTTPリクエストパケットをルーティングする。
     *
     * @param packet HTTPリクエストを含むVirtualPacket
     * @return HTTPレスポンスを含むVirtualPacket
     */
    public VirtualPacket routeRequest(VirtualPacket packet) {
        if (packet == null) {
            return createErrorPacket(null, null, 400, "Bad Request", "Packet is null");
        }

        IPvMAddress destination = packet.getDestination();
        IPvMAddress source = packet.getSource();

        log("Routing request: " + source + " -> " + destination);

        // VirtualHttpServerを検索
        Optional<VirtualHttpServer> serverOpt = registry.getServer(destination);

        if (serverOpt.isEmpty()) {
            log("No server found for: " + destination);
            return createErrorPacket(destination, source, 404, "Not Found",
                    "Server not found: " + destination);
        }

        VirtualHttpServer server = serverOpt.get();

        try {
            // リクエストを構築
            VirtualHttpRequest request = buildRequest(packet);

            // サーバーでリクエストを処理
            VirtualHttpResponse response = server.handleRequest(request);

            // レスポンスパケットを構築
            return buildResponsePacket(destination, source, response);

        } catch (Exception e) {
            log("Error handling request: " + e.getMessage());
            e.printStackTrace();
            return createErrorPacket(destination, source, 500, "Internal Server Error", e.getMessage());
        }
    }

    /**
     * VirtualPacketからVirtualHttpRequestを構築
     */
    private VirtualHttpRequest buildRequest(VirtualPacket packet) {
        Map<String, Object> data = packet.getData();

        String method = getStringOrDefault(data, "method", "GET");
        String path = getStringOrDefault(data, "path", "/");
        String body = getStringOrDefault(data, "body", "");

        VirtualHttpRequest.Builder builder = VirtualHttpRequest.builder()
                .source(packet.getSource())
                .destination(packet.getDestination())
                .method(method)
                .path(path)
                .body(body);

        // ヘッダーがあれば追加
        Object headersObj = data.get("headers");
        if (headersObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, String> headers = (Map<String, String>) headersObj;
            builder.headers(headers);
        }

        return builder.build();
    }

    /**
     * VirtualHttpResponseからVirtualPacketを構築
     */
    private VirtualPacket buildResponsePacket(IPvMAddress source, IPvMAddress destination, VirtualHttpResponse response) {
        return VirtualPacket.builder()
                .source(source)
                .destination(destination)
                .type(VirtualPacket.PacketType.GENERIC_RESPONSE)
                .put("statusCode", response.getStatusCode())
                .put("statusText", response.getStatusText())
                .put("mimeType", response.getMimeType())
                .put("body", response.getBody())
                .put("headers", response.getHeaders())
                .build();
    }

    /**
     * エラーレスポンスパケットを作成
     */
    private VirtualPacket createErrorPacket(IPvMAddress source, IPvMAddress destination,
                                            int statusCode, String statusText, String message) {
        String errorHtml = generateErrorHtml(statusCode, statusText, message);

        VirtualPacket.Builder builder = VirtualPacket.builder()
                .type(VirtualPacket.PacketType.GENERIC_RESPONSE)
                .put("statusCode", statusCode)
                .put("statusText", statusText)
                .put("mimeType", "text/html")
                .put("body", errorHtml);

        // source/destinationがnullの場合はダミーアドレスを使用
        if (source != null) {
            builder.source(source);
        } else {
            builder.source(IPvMAddress.forSystem("error"));
        }

        if (destination != null) {
            builder.destination(destination);
        } else {
            builder.destination(IPvMAddress.forSystem("error"));
        }

        return builder.build();
    }

    /**
     * エラーHTMLを生成
     */
    private String generateErrorHtml(int statusCode, String statusText, String message) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">" +
                "<title>" + statusCode + " " + statusText + "</title>" +
                "<style>" +
                "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;" +
                "text-align:center;padding:50px;background:#1a1a2e;color:#fff;margin:0;}" +
                "h1{color:#e74c3c;font-size:2em;margin-bottom:20px;}" +
                "p{color:#aaa;font-size:1.1em;}" +
                "</style></head>" +
                "<body><h1>" + statusCode + " " + statusText + "</h1>" +
                "<p>" + escapeHtml(message) + "</p></body></html>";
    }

    /**
     * HTML特殊文字をエスケープ
     */
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * Mapから文字列を取得（デフォルト値付き）
     */
    private String getStringOrDefault(Map<String, Object> data, String key, String defaultValue) {
        Object value = data.get(key);
        if (value instanceof String) {
            return (String) value;
        }
        return defaultValue;
    }

    private void log(String message) {
        System.out.println("[ServerVirtualRouter] " + message);
    }
}
