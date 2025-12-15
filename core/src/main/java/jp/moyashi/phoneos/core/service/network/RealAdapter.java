package jp.moyashi.phoneos.core.service.network;

import jp.moyashi.phoneos.core.Kernel;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * 実インターネットアダプター。
 * IPv4/IPv6アドレスへのHTTP通信を管理する。
 * Java標準のHttpClientを使用して実インターネットにアクセスする。
 *
 * <p>使用例:</p>
 * <pre>{@code
 * RealAdapter adapter = kernel.getNetworkAdapter().getRealAdapter();
 *
 * // 実インターネットへHTTPリクエスト
 * CompletableFuture<RealAdapter.HttpResponse> future =
 *     adapter.httpRequest("https://www.google.com/", "GET");
 * }</pre>
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class RealAdapter {

    private final Kernel kernel;
    private final HttpClient httpClient;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);

    /**
     * RealAdapterを構築する。
     *
     * @param kernel Kernelインスタンス
     */
    public RealAdapter(Kernel kernel) {
        this.kernel = kernel;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * 実インターネットが利用可能かを判定する。
     * 現在は常にtrueを返す（JVMレベルでのネットワーク接続を前提）。
     *
     * @return 利用可能な場合true
     */
    public boolean isAvailable() {
        // JVMレベルでのネットワーク接続を前提
        // 必要に応じて実際の接続チェックを実装可能
        return true;
    }

    /**
     * 現在のネットワーク状態を取得する。
     *
     * @return ネットワーク状態
     */
    public NetworkStatus getStatus() {
        return isAvailable() ? NetworkStatus.CONNECTED : NetworkStatus.NO_SERVICE;
    }

    /**
     * HTTPリクエストを送信する（非同期）。
     *
     * @param url リクエストURL
     * @param method HTTPメソッド（GET, POST等）
     * @return HTTPレスポンスのFuture
     */
    public CompletableFuture<RealHttpResponse> httpRequest(String url, String method) {
        return httpRequest(url, method, null, null);
    }

    /**
     * HTTPリクエストを送信する（非同期、ボディ付き）。
     *
     * @param url リクエストURL
     * @param method HTTPメソッド
     * @param contentType Content-Type
     * @param body リクエストボディ
     * @return HTTPレスポンスのFuture
     */
    public CompletableFuture<RealHttpResponse> httpRequest(
            String url, String method, String contentType, String body) {

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DEFAULT_TIMEOUT);

            // メソッドとボディの設定
            if ("GET".equalsIgnoreCase(method)) {
                builder.GET();
            } else if ("POST".equalsIgnoreCase(method)) {
                if (body != null) {
                    builder.POST(HttpRequest.BodyPublishers.ofString(body));
                } else {
                    builder.POST(HttpRequest.BodyPublishers.noBody());
                }
            } else if ("PUT".equalsIgnoreCase(method)) {
                if (body != null) {
                    builder.PUT(HttpRequest.BodyPublishers.ofString(body));
                } else {
                    builder.PUT(HttpRequest.BodyPublishers.noBody());
                }
            } else if ("DELETE".equalsIgnoreCase(method)) {
                builder.DELETE();
            } else {
                builder.method(method, body != null ?
                        HttpRequest.BodyPublishers.ofString(body) :
                        HttpRequest.BodyPublishers.noBody());
            }

            // Content-Typeの設定
            if (contentType != null) {
                builder.header("Content-Type", contentType);
            }

            // User-Agentの設定
            builder.header("User-Agent", "MochiMobileOS/1.0");

            HttpRequest request = builder.build();

            log("Sending HTTP request: " + method + " " + url);

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        String responseContentType = response.headers()
                                .firstValue("Content-Type")
                                .orElse("text/html");
                        return new RealHttpResponse(
                                response.statusCode(),
                                responseContentType,
                                response.body()
                        );
                    })
                    .exceptionally(e -> {
                        logError("HTTP request failed: " + e.getMessage());
                        return RealHttpResponse.error(e.getMessage());
                    });

        } catch (Exception e) {
            logError("Failed to create HTTP request: " + e.getMessage());
            return CompletableFuture.completedFuture(RealHttpResponse.error(e.getMessage()));
        }
    }

    /**
     * 同期HTTPリクエストを送信する。
     *
     * @param url リクエストURL
     * @param method HTTPメソッド
     * @return HTTPレスポンス
     * @throws NetworkException ネットワークエラー時
     */
    public RealHttpResponse httpRequestSync(String url, String method) throws NetworkException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DEFAULT_TIMEOUT)
                    .header("User-Agent", "MochiMobileOS/1.0")
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            String contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("text/html");

            return new RealHttpResponse(response.statusCode(), contentType, response.body());

        } catch (IOException e) {
            throw new NetworkException("Network I/O error: " + e.getMessage(),
                    NetworkException.ErrorType.INTERNAL_ERROR, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new NetworkException("Request interrupted",
                    NetworkException.ErrorType.TIMEOUT, e);
        }
    }

    /**
     * ログ出力。
     */
    private void log(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().debug("RealAdapter", message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("RealAdapter", message);
        } else {
            System.err.println("[RealAdapter] " + message);
        }
    }

    /**
     * 実インターネットHTTPレスポンスを表すクラス。
     */
    public static class RealHttpResponse {
        private final int statusCode;
        private final String contentType;
        private final String body;
        private final boolean success;
        private final String errorMessage;

        public RealHttpResponse(int statusCode, String contentType, String body) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.body = body;
            this.success = statusCode >= 200 && statusCode < 300;
            this.errorMessage = null;
        }

        private RealHttpResponse(String errorMessage) {
            this.statusCode = 0;
            this.contentType = null;
            this.body = null;
            this.success = false;
            this.errorMessage = errorMessage;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getContentType() {
            return contentType;
        }

        public String getBody() {
            return body;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static RealHttpResponse error(String message) {
            return new RealHttpResponse(message);
        }
    }
}
