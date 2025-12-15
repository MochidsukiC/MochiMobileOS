package jp.moyashi.phoneos.core.service.network;

import jp.moyashi.phoneos.core.Kernel;

import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * MochiMobileOS ネットワークアダプター。
 * すべてのネットワーク通信の統一エントリーポイント。
 *
 * <p>アーキテクチャ:</p>
 * <pre>
 * ┌─────────────────────────────────────────────┐
 * │              NetworkAdapter                 │
 * │                                             │
 * │   request(url) → ルーティング判定           │
 * │                                             │
 * │   ┌─────────────────┐  ┌─────────────────┐  │
 * │   │ VirtualAdapter  │  │ RealAdapter     │  │
 * │   │ (IPvM)          │  │ (IPv4/6)        │  │
 * │   └─────────────────┘  └─────────────────┘  │
 * └─────────────────────────────────────────────┘
 * </pre>
 *
 * <p>ルーティング規則:</p>
 * <ul>
 *   <li>IPvMアドレス（http://[0-3]-xxx/...） → VirtualAdapter</li>
 *   <li>通常URL（http://example.com/...） → RealAdapter</li>
 * </ul>
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class NetworkAdapter {

    private final Kernel kernel;
    private final VirtualAdapter virtualAdapter;
    private final RealAdapter realAdapter;

    /**
     * IPvMホスト名パターン: [0-3]-identifier
     * identifier: UUID形式 または 文字列ID
     */
    private static final Pattern IPVM_HOST_PATTERN = Pattern.compile(
            "^[0-3]-(" +
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" +
            "|" +
            "[a-zA-Z0-9][a-zA-Z0-9_-]*" +
            ")$"
    );

    /**
     * NetworkAdapterを構築する。
     *
     * @param kernel Kernelインスタンス
     */
    public NetworkAdapter(Kernel kernel) {
        this.kernel = kernel;
        this.virtualAdapter = new VirtualAdapter(kernel);
        this.realAdapter = new RealAdapter(kernel);

        log("NetworkAdapter initialized");
    }

    /**
     * VirtualAdapter（IPvM用）を取得する。
     *
     * @return VirtualAdapter
     */
    public VirtualAdapter getVirtualAdapter() {
        return virtualAdapter;
    }

    /**
     * RealAdapter（実インターネット用）を取得する。
     *
     * @return RealAdapter
     */
    public RealAdapter getRealAdapter() {
        return realAdapter;
    }

    /**
     * URLに対してHTTPリクエストを送信する。
     * URLを解析し、適切なアダプターにルーティングする。
     *
     * @param url リクエストURL
     * @param method HTTPメソッド（GET, POST等）
     * @return HTTPレスポンスのFuture
     * @throws NetworkException ネットワークエラー時
     */
    public CompletableFuture<NetworkResponse> request(String url, String method) throws NetworkException {
        if (url == null || url.isEmpty()) {
            throw new NetworkException("URL cannot be null or empty",
                    NetworkException.ErrorType.PROTOCOL_ERROR);
        }

        // URLからホスト名を抽出
        String host = extractHost(url);

        if (host != null && isIPvMHost(host)) {
            // IPvMアドレス → VirtualAdapter
            log("Routing to VirtualAdapter: " + url);
            return routeToVirtual(url, host, method);
        } else {
            // 通常URL → RealAdapter
            log("Routing to RealAdapter: " + url);
            return routeToReal(url, method);
        }
    }

    /**
     * IPvMアドレスかどうかを判定する。
     *
     * @param host ホスト名
     * @return IPvMアドレスの場合true
     */
    public boolean isIPvMHost(String host) {
        return host != null && IPVM_HOST_PATTERN.matcher(host).matches();
    }

    /**
     * 仮想ネットワーク（IPvM）が利用可能かを判定する。
     *
     * @return 利用可能な場合true
     */
    public boolean isVirtualNetworkAvailable() {
        return virtualAdapter.isAvailable();
    }

    /**
     * 実インターネットが利用可能かを判定する。
     *
     * @return 利用可能な場合true
     */
    public boolean isRealNetworkAvailable() {
        return realAdapter.isAvailable();
    }

    /**
     * 仮想ネットワークの状態を取得する。
     *
     * @return ネットワーク状態
     */
    public NetworkStatus getVirtualNetworkStatus() {
        return virtualAdapter.getStatus();
    }

    /**
     * 仮想ネットワークの電波強度を取得する。
     *
     * @return 電波強度（0-5）
     */
    public int getSignalStrength() {
        return virtualAdapter.getSignalStrength();
    }

    /**
     * キャリア名を取得する。
     *
     * @return キャリア名
     */
    public String getCarrierName() {
        return virtualAdapter.getCarrierName();
    }

    /**
     * VirtualAdapterへルーティング。
     */
    private CompletableFuture<NetworkResponse> routeToVirtual(String url, String host, String method)
            throws NetworkException {

        // パスを抽出
        String path = extractPath(url);

        return virtualAdapter.httpRequest(host, path, method)
                .thenApply(response -> new NetworkResponse(
                        response.getStatusCode(),
                        response.getContentType(),
                        response.getBody(),
                        NetworkResponse.Source.VIRTUAL
                ));
    }

    /**
     * RealAdapterへルーティング。
     */
    private CompletableFuture<NetworkResponse> routeToReal(String url, String method) {
        return realAdapter.httpRequest(url, method)
                .thenApply(response -> new NetworkResponse(
                        response.getStatusCode(),
                        response.getContentType(),
                        response.getBody(),
                        NetworkResponse.Source.REAL
                ));
    }

    /**
     * URLからホスト名を抽出する。
     */
    private String extractHost(String url) {
        try {
            // http:// または https:// を除去
            String withoutScheme = url;
            if (url.startsWith("http://")) {
                withoutScheme = url.substring(7);
            } else if (url.startsWith("https://")) {
                withoutScheme = url.substring(8);
            }

            // パスの前までを取得
            int slashIndex = withoutScheme.indexOf('/');
            if (slashIndex > 0) {
                return withoutScheme.substring(0, slashIndex);
            } else {
                return withoutScheme;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * URLからパスを抽出する。
     */
    private String extractPath(String url) {
        try {
            // http:// または https:// を除去
            String withoutScheme = url;
            if (url.startsWith("http://")) {
                withoutScheme = url.substring(7);
            } else if (url.startsWith("https://")) {
                withoutScheme = url.substring(8);
            }

            // パスを取得
            int slashIndex = withoutScheme.indexOf('/');
            if (slashIndex > 0) {
                return withoutScheme.substring(slashIndex);
            } else {
                return "/";
            }
        } catch (Exception e) {
            return "/";
        }
    }

    /**
     * アダプターを閉じる。
     */
    public void close() {
        virtualAdapter.close();
        log("NetworkAdapter closed");
    }

    /**
     * ログ出力。
     */
    private void log(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().info("NetworkAdapter", message);
        } else {
            System.out.println("[NetworkAdapter] " + message);
        }
    }

    /**
     * 統一HTTPレスポンスクラス。
     */
    public static class NetworkResponse {
        public enum Source {
            VIRTUAL,  // IPvM経由
            REAL      // 実インターネット経由
        }

        private final int statusCode;
        private final String contentType;
        private final String body;
        private final Source source;

        public NetworkResponse(int statusCode, String contentType, String body, Source source) {
            this.statusCode = statusCode;
            this.contentType = contentType;
            this.body = body;
            this.source = source;
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

        public Source getSource() {
            return source;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }

        public boolean isFromVirtualNetwork() {
            return source == Source.VIRTUAL;
        }

        public boolean isFromRealNetwork() {
            return source == Source.REAL;
        }
    }
}
