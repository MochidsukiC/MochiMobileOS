package jp.moyashi.phoneos.server.network.builtin;

import jp.moyashi.phoneos.server.network.VirtualHttpRequest;
import jp.moyashi.phoneos.server.network.VirtualHttpResponse;
import jp.moyashi.phoneos.server.network.VirtualHttpServer;

/**
 * テスト用システムサーバー。
 * IPvMアドレス: 3-sys-test
 *
 * 開発・デバッグ用のテストページを提供する。
 */
public class TestSystemServer implements VirtualHttpServer {

    public static final String SERVER_ID = "sys-test";

    @Override
    public String getServerId() {
        return SERVER_ID;
    }

    @Override
    public VirtualHttpResponse handleRequest(VirtualHttpRequest request) {
        String path = request.getPath();
        System.out.println("[TestSystemServer] Handling request: " + request.getMethod() + " " + path);

        // ルーティング
        if ("/".equals(path) || "/index.html".equals(path)) {
            return handleIndex(request);
        } else if ("/api/echo".equals(path)) {
            return handleEcho(request);
        } else if ("/api/info".equals(path)) {
            return handleInfo(request);
        } else {
            return VirtualHttpResponse.notFound();
        }
    }

    /**
     * インデックスページ
     */
    private VirtualHttpResponse handleIndex(VirtualHttpRequest request) {
        String html = "<!DOCTYPE html>\n" +
                "<html lang=\"ja\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>MochiOS Test Server</title>\n" +
                "    <style>\n" +
                "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
                "        body {\n" +
                "            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n" +
                "            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);\n" +
                "            color: #fff;\n" +
                "            min-height: 100vh;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .container {\n" +
                "            max-width: 600px;\n" +
                "            margin: 0 auto;\n" +
                "        }\n" +
                "        h1 {\n" +
                "            font-size: 2em;\n" +
                "            margin-bottom: 20px;\n" +
                "            text-align: center;\n" +
                "            background: linear-gradient(90deg, #00d2ff, #3a7bd5);\n" +
                "            -webkit-background-clip: text;\n" +
                "            -webkit-text-fill-color: transparent;\n" +
                "        }\n" +
                "        .card {\n" +
                "            background: rgba(255, 255, 255, 0.1);\n" +
                "            border-radius: 12px;\n" +
                "            padding: 20px;\n" +
                "            margin-bottom: 15px;\n" +
                "        }\n" +
                "        .card h2 {\n" +
                "            font-size: 1.2em;\n" +
                "            margin-bottom: 10px;\n" +
                "            color: #00d2ff;\n" +
                "        }\n" +
                "        .card p {\n" +
                "            color: #aaa;\n" +
                "            line-height: 1.6;\n" +
                "        }\n" +
                "        .success {\n" +
                "            background: rgba(0, 200, 83, 0.2);\n" +
                "            border: 1px solid #00c853;\n" +
                "        }\n" +
                "        .success h2 {\n" +
                "            color: #00c853;\n" +
                "        }\n" +
                "        .info-grid {\n" +
                "            display: grid;\n" +
                "            grid-template-columns: 1fr 1fr;\n" +
                "            gap: 10px;\n" +
                "        }\n" +
                "        .info-item {\n" +
                "            background: rgba(0, 0, 0, 0.2);\n" +
                "            padding: 10px;\n" +
                "            border-radius: 8px;\n" +
                "        }\n" +
                "        .info-item .label {\n" +
                "            font-size: 0.8em;\n" +
                "            color: #888;\n" +
                "        }\n" +
                "        .info-item .value {\n" +
                "            font-size: 1.1em;\n" +
                "            color: #fff;\n" +
                "            word-break: break-all;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"container\">\n" +
                "        <h1>MochiOS Test Server</h1>\n" +
                "        \n" +
                "        <div class=\"card success\">\n" +
                "            <h2>✓ 接続成功</h2>\n" +
                "            <p>IPvM over HTTP アーキテクチャが正常に動作しています。</p>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"card\">\n" +
                "            <h2>リクエスト情報</h2>\n" +
                "            <div class=\"info-grid\">\n" +
                "                <div class=\"info-item\">\n" +
                "                    <div class=\"label\">送信元</div>\n" +
                "                    <div class=\"value\">" + request.getSource() + "</div>\n" +
                "                </div>\n" +
                "                <div class=\"info-item\">\n" +
                "                    <div class=\"label\">送信先</div>\n" +
                "                    <div class=\"value\">" + request.getDestination() + "</div>\n" +
                "                </div>\n" +
                "                <div class=\"info-item\">\n" +
                "                    <div class=\"label\">メソッド</div>\n" +
                "                    <div class=\"value\">" + request.getMethod() + "</div>\n" +
                "                </div>\n" +
                "                <div class=\"info-item\">\n" +
                "                    <div class=\"label\">パス</div>\n" +
                "                    <div class=\"value\">" + request.getPath() + "</div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        \n" +
                "        <div class=\"card\">\n" +
                "            <h2>サーバー情報</h2>\n" +
                "            <p>IPvM Address: 3-" + SERVER_ID + "</p>\n" +
                "            <p>Server Time: " + java.time.LocalDateTime.now() + "</p>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";

        return VirtualHttpResponse.html(html);
    }

    /**
     * Echoエンドポイント
     */
    private VirtualHttpResponse handleEcho(VirtualHttpRequest request) {
        String json = "{\n" +
                "  \"echo\": true,\n" +
                "  \"method\": \"" + request.getMethod() + "\",\n" +
                "  \"path\": \"" + request.getPath() + "\",\n" +
                "  \"body\": \"" + escapeJson(request.getBody()) + "\",\n" +
                "  \"timestamp\": " + System.currentTimeMillis() + "\n" +
                "}";
        return VirtualHttpResponse.json(json);
    }

    /**
     * 情報エンドポイント
     */
    private VirtualHttpResponse handleInfo(VirtualHttpRequest request) {
        String json = "{\n" +
                "  \"server\": \"" + SERVER_ID + "\",\n" +
                "  \"version\": \"1.0.0\",\n" +
                "  \"ipvmAddress\": \"3-" + SERVER_ID + "\",\n" +
                "  \"endpoints\": [\"/\", \"/api/echo\", \"/api/info\"]\n" +
                "}";
        return VirtualHttpResponse.json(json);
    }

    /**
     * JSON用エスケープ
     */
    private String escapeJson(String text) {
        if (text == null) return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public String getDescription() {
        return "MochiOS Test System Server (3-" + SERVER_ID + ")";
    }
}
