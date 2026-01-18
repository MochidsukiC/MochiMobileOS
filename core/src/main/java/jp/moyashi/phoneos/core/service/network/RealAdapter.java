package jp.moyashi.phoneos.core.service.network;

import jp.moyashi.phoneos.core.Kernel;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
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
     * プライベート/ローカルIPアドレスへのアクセスを禁止するかどうか。
     * SSRF（Server-Side Request Forgery）攻撃を防止するために有効化推奨。
     */
    private static final boolean BLOCK_PRIVATE_IPS = true;

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
     * URLがプライベート/ローカルIPアドレスかどうかをチェックする。
     * SSRF攻撃防止のため、プライベートネットワーク・ループバック・リンクローカルアドレスをブロック。
     *
     * @param url チェック対象のURL
     * @return プライベートIPアドレスの場合true
     */
    private boolean isPrivateOrLocalAddress(String url) {
        if (!BLOCK_PRIVATE_IPS) {
            return false;
        }

        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                return true; // ホストが取得できない場合は安全のためブロック
            }

            // localhostの文字列チェック
            if (host.equalsIgnoreCase("localhost")) {
                return true;
            }

            // IPアドレスを解決してチェック
            InetAddress address = InetAddress.getByName(host);

            // ループバックアドレス (127.0.0.0/8, ::1)
            if (address.isLoopbackAddress()) {
                return true;
            }

            // プライベートアドレス (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, fc00::/7)
            if (address.isSiteLocalAddress()) {
                return true;
            }

            // リンクローカルアドレス (169.254.0.0/16, fe80::/10)
            if (address.isLinkLocalAddress()) {
                return true;
            }

            // マルチキャストアドレス
            if (address.isMulticastAddress()) {
                return true;
            }

            // ワイルドカードアドレス (0.0.0.0, ::)
            if (address.isAnyLocalAddress()) {
                return true;
            }

            // IPv4マップされたIPv6アドレスの場合、埋め込みIPv4をチェック
            byte[] addressBytes = address.getAddress();
            if (addressBytes.length == 16) {
                // ::ffff:x.x.x.x 形式のIPv4マップアドレス
                boolean isV4Mapped = true;
                for (int i = 0; i < 10; i++) {
                    if (addressBytes[i] != 0) {
                        isV4Mapped = false;
                        break;
                    }
                }
                if (isV4Mapped && addressBytes[10] == (byte) 0xff && addressBytes[11] == (byte) 0xff) {
                    // 埋め込みIPv4を抽出してチェック
                    int b1 = addressBytes[12] & 0xff;
                    int b2 = addressBytes[13] & 0xff;
                    // プライベートIPv4レンジをチェック
                    if (b1 == 10 ||                                    // 10.0.0.0/8
                        (b1 == 172 && (b2 >= 16 && b2 <= 31)) ||       // 172.16.0.0/12
                        (b1 == 192 && b2 == 168) ||                    // 192.168.0.0/16
                        b1 == 127 ||                                    // 127.0.0.0/8
                        (b1 == 169 && b2 == 254)) {                    // 169.254.0.0/16
                        return true;
                    }
                }
            }

            return false;

        } catch (UnknownHostException e) {
            logError("Failed to resolve host for SSRF check: " + e.getMessage());
            return true; // 解決できない場合は安全のためブロック
        } catch (Exception e) {
            logError("SSRF check failed: " + e.getMessage());
            return true; // エラー時は安全のためブロック
        }
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

        // SSRF対策: プライベート/ローカルIPアドレスへのアクセスをブロック
        if (isPrivateOrLocalAddress(url)) {
            logError("SSRF blocked: Access to private/local address denied: " + url);
            return CompletableFuture.completedFuture(
                    RealHttpResponse.error("Access to private/local network addresses is not allowed"));
        }

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
     * HTTPリクエストを送信する（非同期、バイト配列として取得）。
     *
     * @param url リクエストURL
     * @return HTTPレスポンスのFuture（ボディはbyte[]）
     */
    public CompletableFuture<RealByteHttpResponse> httpRequestBytes(String url) {
        // SSRF対策: プライベート/ローカルIPアドレスへのアクセスをブロック
        if (isPrivateOrLocalAddress(url)) {
            logError("SSRF blocked: Access to private/local address denied: " + url);
            return CompletableFuture.completedFuture(
                    RealByteHttpResponse.error("Access to private/local network addresses is not allowed"));
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(DEFAULT_TIMEOUT)
                    .header("User-Agent", "MochiMobileOS/1.0")
                    .GET()
                    .build();

            log("Sending HTTP request (Bytes): GET " + url);

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray())
                    .thenApply(response -> {
                        String responseContentType = response.headers()
                                .firstValue("Content-Type")
                                .orElse("application/octet-stream");
                        return new RealByteHttpResponse(
                                response.statusCode(),
                                responseContentType,
                                response.body()
                        );
                    })
                    .exceptionally(e -> {
                        logError("HTTP request (Bytes) failed: " + e.getMessage());
                        return RealByteHttpResponse.error(e.getMessage());
                    });

        } catch (Exception e) {
            logError("Failed to create HTTP request (Bytes): " + e.getMessage());
            return CompletableFuture.completedFuture(RealByteHttpResponse.error(e.getMessage()));
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
        // SSRF対策: プライベート/ローカルIPアドレスへのアクセスをブロック
        if (isPrivateOrLocalAddress(url)) {
            logError("SSRF blocked: Access to private/local address denied: " + url);
            throw new NetworkException("Access to private/local network addresses is not allowed",
                    NetworkException.ErrorType.INTERNAL_ERROR, null);
        }

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

    /**
     * 実インターネットHTTPレスポンス（バイト配列版）を表すクラス。
     */
    public static class RealByteHttpResponse {
        private final int statusCode;
        private final String contentType;
        private final byte[] body;
        private final boolean success;
        private final String errorMessage;

        public RealByteHttpResponse(int statusCode, String contentType, byte[] body) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.body = body;
            this.success = statusCode >= 200 && statusCode < 300;
            this.errorMessage = null;
        }

        private RealByteHttpResponse(String errorMessage) {
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

        public byte[] getBody() {
            return body;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public static RealByteHttpResponse error(String message) {
            return new RealByteHttpResponse(message);
        }
    }
}
