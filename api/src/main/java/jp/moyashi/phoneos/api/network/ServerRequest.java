package jp.moyashi.phoneos.api.network;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 仮想サーバーへのリクエストを表すクラス。
 * VirtualServerが受け取るリクエスト情報を保持する。
 */
public class ServerRequest {

    private final String method;
    private final String path;
    private final Map<String, String> queryParams;
    private final Map<String, String> headers;
    private final String body;
    private final IPvMAddress sourceAddress;

    /**
     * ServerRequestを構築します。
     *
     * @param method HTTPメソッド
     * @param path リクエストパス
     * @param queryParams クエリパラメータ
     * @param headers リクエストヘッダー
     * @param body リクエストボディ
     * @param sourceAddress リクエスト元IPvMアドレス
     */
    public ServerRequest(String method, String path, Map<String, String> queryParams,
                        Map<String, String> headers, String body, IPvMAddress sourceAddress) {
        this.method = method != null ? method : "GET";
        this.path = path != null ? path : "/";
        this.queryParams = queryParams != null ? new HashMap<>(queryParams) : new HashMap<>();
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.body = body;
        this.sourceAddress = sourceAddress;
    }

    /**
     * HTTPメソッドを取得します。
     *
     * @return HTTPメソッド（GET, POST, PUT, DELETE等）
     */
    public String getMethod() {
        return method;
    }

    /**
     * リクエストパスを取得します。
     *
     * @return リクエストパス（例: "/api/users"）
     */
    public String getPath() {
        return path;
    }

    /**
     * クエリパラメータを取得します。
     *
     * @return クエリパラメータ（読み取り専用）
     */
    public Map<String, String> getQueryParams() {
        return Collections.unmodifiableMap(queryParams);
    }

    /**
     * 特定のクエリパラメータを取得します。
     *
     * @param key パラメータ名
     * @return パラメータ値（存在しない場合はnull）
     */
    public String getQueryParam(String key) {
        return queryParams.get(key);
    }

    /**
     * リクエストヘッダーを取得します。
     *
     * @return リクエストヘッダー（読み取り専用）
     */
    public Map<String, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * 特定のヘッダーを取得します。
     *
     * @param key ヘッダー名
     * @return ヘッダー値（存在しない場合はnull）
     */
    public String getHeader(String key) {
        return headers.get(key);
    }

    /**
     * リクエストボディを取得します。
     *
     * @return リクエストボディ（POST/PUT時、存在しない場合はnull）
     */
    public String getBody() {
        return body;
    }

    /**
     * リクエスト元のIPvMアドレスを取得します。
     *
     * @return リクエスト元IPvMアドレス
     */
    public IPvMAddress getSourceAddress() {
        return sourceAddress;
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
     * ServerRequestビルダー。
     */
    public static class Builder {
        private String method = "GET";
        private String path = "/";
        private Map<String, String> queryParams = new HashMap<>();
        private Map<String, String> headers = new HashMap<>();
        private String body;
        private IPvMAddress sourceAddress;

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder queryParam(String key, String value) {
            this.queryParams.put(key, value);
            return this;
        }

        public Builder queryParams(Map<String, String> queryParams) {
            this.queryParams.putAll(queryParams);
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

        public Builder body(String body) {
            this.body = body;
            return this;
        }

        public Builder sourceAddress(IPvMAddress sourceAddress) {
            this.sourceAddress = sourceAddress;
            return this;
        }

        public ServerRequest build() {
            return new ServerRequest(method, path, queryParams, headers, body, sourceAddress);
        }
    }

    @Override
    public String toString() {
        return "ServerRequest{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", queryParams=" + queryParams +
                ", bodyLength=" + (body != null ? body.length() : 0) +
                ", sourceAddress=" + sourceAddress +
                '}';
    }
}
