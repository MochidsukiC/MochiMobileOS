package jp.moyashi.phoneos.server.network;

import jp.moyashi.phoneos.core.service.network.IPvMAddress;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 仮想HTTPリクエストを表すクラス。
 * Chromiumからのリクエストがサーバー側で処理される際に使用される。
 */
public class VirtualHttpRequest {

    private final IPvMAddress source;
    private final IPvMAddress destination;
    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final String body;

    private VirtualHttpRequest(Builder builder) {
        this.source = builder.source;
        this.destination = builder.destination;
        this.method = builder.method;
        this.path = builder.path;
        this.headers = Collections.unmodifiableMap(new HashMap<>(builder.headers));
        this.body = builder.body;
    }

    /**
     * 送信元アドレスを取得
     */
    public IPvMAddress getSource() {
        return source;
    }

    /**
     * 送信先アドレスを取得
     */
    public IPvMAddress getDestination() {
        return destination;
    }

    /**
     * HTTPメソッドを取得
     */
    public String getMethod() {
        return method;
    }

    /**
     * リクエストパスを取得
     */
    public String getPath() {
        return path;
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
     * リクエストボディを取得
     */
    public String getBody() {
        return body;
    }

    /**
     * ビルダーを作成
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private IPvMAddress source;
        private IPvMAddress destination;
        private String method = "GET";
        private String path = "/";
        private Map<String, String> headers = new HashMap<>();
        private String body = "";

        public Builder source(IPvMAddress source) {
            this.source = source;
            return this;
        }

        public Builder destination(IPvMAddress destination) {
            this.destination = destination;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
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

        public VirtualHttpRequest build() {
            if (source == null) {
                throw new IllegalArgumentException("Source address is required");
            }
            if (destination == null) {
                throw new IllegalArgumentException("Destination address is required");
            }
            return new VirtualHttpRequest(this);
        }
    }

    @Override
    public String toString() {
        return "VirtualHttpRequest{" +
                "method='" + method + '\'' +
                ", path='" + path + '\'' +
                ", source=" + source +
                ", destination=" + destination +
                '}';
    }
}
