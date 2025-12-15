package jp.moyashi.phoneos.api.network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ネットワークレスポンスを表すクラス。
 * HTTPリクエストの結果を保持する。
 */
public class NetworkResponse {

    private final int statusCode;
    private final String body;
    private final String contentType;
    private final Map<String, String> headers;
    private final boolean fromVirtualNetwork;

    /**
     * NetworkResponseを構築します。
     *
     * @param statusCode HTTPステータスコード
     * @param body レスポンスボディ
     * @param contentType Content-Type
     * @param headers レスポンスヘッダー
     * @param fromVirtualNetwork 仮想ネットワークからのレスポンスかどうか
     */
    public NetworkResponse(int statusCode, String body, String contentType,
                          Map<String, String> headers, boolean fromVirtualNetwork) {
        this.statusCode = statusCode;
        this.body = body;
        this.contentType = contentType;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.fromVirtualNetwork = fromVirtualNetwork;
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

    /**
     * 特定のヘッダー値を取得します。
     *
     * @param key ヘッダー名
     * @return ヘッダー値（存在しない場合はnull）
     */
    public String getHeader(String key) {
        return headers.get(key);
    }

    /**
     * リクエストが成功したかどうかを判定します。
     * ステータスコードが200-299の範囲の場合trueを返します。
     *
     * @return 成功した場合true
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * 仮想ネットワーク（IPvM）からのレスポンスかどうかを判定します。
     *
     * @return 仮想ネットワークからのレスポンスの場合true
     */
    public boolean isFromVirtualNetwork() {
        return fromVirtualNetwork;
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
     * NetworkResponseビルダー。
     */
    public static class Builder {
        private int statusCode = 200;
        private String body = "";
        private String contentType = "text/plain";
        private Map<String, String> headers = new HashMap<>();
        private boolean fromVirtualNetwork = false;

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

        public Builder fromVirtualNetwork(boolean fromVirtualNetwork) {
            this.fromVirtualNetwork = fromVirtualNetwork;
            return this;
        }

        public NetworkResponse build() {
            return new NetworkResponse(statusCode, body, contentType, headers, fromVirtualNetwork);
        }
    }

    @Override
    public String toString() {
        return "NetworkResponse{" +
                "statusCode=" + statusCode +
                ", contentType='" + contentType + '\'' +
                ", bodyLength=" + (body != null ? body.length() : 0) +
                ", fromVirtualNetwork=" + fromVirtualNetwork +
                '}';
    }
}
