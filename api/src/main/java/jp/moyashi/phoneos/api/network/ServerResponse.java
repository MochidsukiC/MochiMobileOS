package jp.moyashi.phoneos.api.network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 仮想サーバーからのレスポンスを表すクラス。
 * VirtualServerが返すレスポンス情報を保持する。
 */
public class ServerResponse {

    private final int statusCode;
    private final String body;
    private final String contentType;
    private final Map<String, String> headers;

    /**
     * ServerResponseを構築します。
     *
     * @param statusCode HTTPステータスコード
     * @param body レスポンスボディ
     * @param contentType Content-Type
     * @param headers レスポンスヘッダー
     */
    public ServerResponse(int statusCode, String body, String contentType, Map<String, String> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.contentType = contentType;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
    }

    /**
     * HTTPステータスコードを取得します。
     *
     * @return ステータスコード
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * レスポンスボディを取得します。
     *
     * @return レスポンスボディ
     */
    public String getBody() {
        return body;
    }

    /**
     * Content-Typeを取得します。
     *
     * @return Content-Type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * レスポンスヘッダーを取得します。
     *
     * @return レスポンスヘッダー（読み取り専用）
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    // ========================================
    // ファクトリメソッド
    // ========================================

    /**
     * 200 OKレスポンスを生成します。
     *
     * @param body レスポンスボディ
     * @return ServerResponse
     */
    public static ServerResponse ok(String body) {
        return new ServerResponse(200, body, "text/plain", null);
    }

    /**
     * 200 OKレスポンスを生成します（Content-Type指定）。
     *
     * @param body レスポンスボディ
     * @param contentType Content-Type
     * @return ServerResponse
     */
    public static ServerResponse ok(String body, String contentType) {
        return new ServerResponse(200, body, contentType, null);
    }

    /**
     * JSONレスポンスを生成します。
     *
     * @param jsonBody JSONボディ
     * @return ServerResponse
     */
    public static ServerResponse json(String jsonBody) {
        return new ServerResponse(200, jsonBody, "application/json", null);
    }

    /**
     * HTMLレスポンスを生成します。
     *
     * @param htmlBody HTMLボディ
     * @return ServerResponse
     */
    public static ServerResponse html(String htmlBody) {
        return new ServerResponse(200, htmlBody, "text/html", null);
    }

    /**
     * 400 Bad Requestレスポンスを生成します。
     *
     * @param message エラーメッセージ
     * @return ServerResponse
     */
    public static ServerResponse badRequest(String message) {
        return new ServerResponse(400, message, "text/plain", null);
    }

    /**
     * 404 Not Foundレスポンスを生成します。
     *
     * @return ServerResponse
     */
    public static ServerResponse notFound() {
        return new ServerResponse(404, "Not Found", "text/plain", null);
    }

    /**
     * 500 Internal Server Errorレスポンスを生成します。
     *
     * @param message エラーメッセージ
     * @return ServerResponse
     */
    public static ServerResponse error(String message) {
        return new ServerResponse(500, message, "text/plain", null);
    }

    /**
     * カスタムレスポンスを生成します。
     *
     * @param statusCode ステータスコード
     * @param body レスポンスボディ
     * @param contentType Content-Type
     * @return ServerResponse
     */
    public static ServerResponse custom(int statusCode, String body, String contentType) {
        return new ServerResponse(statusCode, body, contentType, null);
    }

    // ========================================
    // ビルダー
    // ========================================

    /**
     * ビルダーを作成します。
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * ServerResponseビルダー。
     */
    public static class Builder {
        private int statusCode = 200;
        private String body = "";
        private String contentType = "text/plain";
        private Map<String, String> headers = new HashMap<>();

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder header(String key, String value) {
            this.headers.put(key, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public ServerResponse build() {
            return new ServerResponse(statusCode, body, contentType, headers);
        }
    }

    @Override
    public String toString() {
        return "ServerResponse{" +
                "statusCode=" + statusCode +
                ", contentType='" + contentType + '\'' +
                ", bodyLength=" + (body != null ? body.length() : 0) +
                '}';
    }
}
