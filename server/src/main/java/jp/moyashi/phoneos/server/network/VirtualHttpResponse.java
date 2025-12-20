package jp.moyashi.phoneos.server.network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 仮想HTTPレスポンスを表すクラス。
 * VirtualHttpServerからのレスポンスをクライアントに返す際に使用される。
 */
public class VirtualHttpResponse {

    private final int statusCode;
    private final String statusText;
    private final Map<String, String> headers;
    private final String body;
    private final String mimeType;

    private VirtualHttpResponse(Builder builder) {
        this.statusCode = builder.statusCode;
        this.statusText = builder.statusText;
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.body = builder.body;
        this.mimeType = builder.mimeType;
    }

    /**
     * ステータスコードを取得
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * ステータステキストを取得
     */
    public String getStatusText() {
        return statusText;
    }

    /**
     * ヘッダーを取得
     */
    public Map<String, String> getHeaders() {
        return headers;
    }

    /**
     * 特定のヘッダーを取得
     */
    public String getHeader(String name) {
        return headers.get(name);
    }

    /**
     * レスポンスボディを取得
     */
    public String getBody() {
        return body;
    }

    /**
     * MIMEタイプを取得
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * 成功レスポンスかどうかを判定
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * ビルダーを作成
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 200 OKレスポンスを作成
     */
    public static VirtualHttpResponse ok(String body) {
        return builder()
                .statusCode(200)
                .statusText("OK")
                .body(body)
                .build();
    }

    /**
     * HTMLレスポンスを作成
     */
    public static VirtualHttpResponse html(String html) {
        return builder()
                .statusCode(200)
                .statusText("OK")
                .mimeType("text/html")
                .body(html)
                .build();
    }

    /**
     * JSONレスポンスを作成
     */
    public static VirtualHttpResponse json(String json) {
        return builder()
                .statusCode(200)
                .statusText("OK")
                .mimeType("application/json")
                .body(json)
                .build();
    }

    /**
     * 404 Not Foundレスポンスを作成
     */
    public static VirtualHttpResponse notFound() {
        return builder()
                .statusCode(404)
                .statusText("Not Found")
                .mimeType("text/html")
                .body("<!DOCTYPE html><html><head><title>404 Not Found</title></head>" +
                      "<body><h1>404 Not Found</h1></body></html>")
                .build();
    }

    /**
     * 500 Internal Server Errorレスポンスを作成
     */
    public static VirtualHttpResponse error(String message) {
        return builder()
                .statusCode(500)
                .statusText("Internal Server Error")
                .mimeType("text/html")
                .body("<!DOCTYPE html><html><head><title>500 Error</title></head>" +
                      "<body><h1>500 Internal Server Error</h1><p>" + message + "</p></body></html>")
                .build();
    }

    public static class Builder {
        private int statusCode = 200;
        private String statusText = "OK";
        private Map<String, String> headers = new HashMap<>();
        private String body = "";
        private String mimeType = "text/html";

        public Builder statusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder statusText(String statusText) {
            this.statusText = statusText;
            return this;
        }

        public Builder header(String name, String value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder mimeType(String mimeType) {
            this.mimeType = mimeType;
            return this;
        }

        public VirtualHttpResponse build() {
            return new VirtualHttpResponse(this);
        }
    }

    @Override
    public String toString() {
        return "VirtualHttpResponse{" +
                "statusCode=" + statusCode +
                ", statusText='" + statusText + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", bodyLength=" + (body != null ? body.length() : 0) +
                '}';
    }
}
