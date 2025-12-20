package jp.moyashi.phoneos.core.service.chromium.interceptor;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefResourceHandler;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.handler.CefResourceRequestHandlerAdapter;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;
import org.cef.network.CefURLRequest;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * MochiMobileOS用リクエストハンドラー。
 * IPvM over HTTP アーキテクチャの中核コンポーネント。
 *
 * HTTPリクエストをインターセプトし、以下のルーティングを行う：
 * - IPvMアドレス（http://[0-3]-uuid/path） → VirtualNetworkResourceHandler
 * - アプリアセット（http://app.local/modid/path） → AppAssetResourceHandler
 * - その他 → null（通常のHTTP処理）
 *
 * 詳細: docs/development/10_Proposal_IPvM_over_HTTP.md
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class MochiResourceRequestHandler extends CefResourceRequestHandlerAdapter {

    private final Kernel kernel;

    /**
     * IPvMアドレスパターン: [type]-[identifier]
     * type: 0=Player, 1=Device, 2=Server, 3=System
     *
     * identifier形式:
     * - Player/Device (0,1): 標準UUID形式 (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
     *   例: 0-380df991-f603-344c-a090-369bad2a924a
     * - Server/System (2,3): 文字列ID形式
     *   例: 2-economy-service, 3-appdata-server
     */
    private static final Pattern IPVM_PATTERN = Pattern.compile(
        "^[0-3]-(" +
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" + // 標準UUID
        "|" +
        "[a-zA-Z0-9][a-zA-Z0-9_-]*" + // 文字列ID（Server/System用）
        ")$"
    );

    /**
     * アプリアセット用ホスト名
     */
    private static final String APP_LOCAL_HOST = "app.local";

    /**
     * MochiResourceRequestHandlerを構築する。
     *
     * @param kernel Kernelインスタンス
     */
    public MochiResourceRequestHandler(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * リソースハンドラーを取得する。
     * URLのホスト名を検査し、適切なハンドラーを返す。
     *
     * @param browser CefBrowser
     * @param frame CefFrame
     * @param request CefRequest
     * @return 適切なCefResourceHandler、または通常処理の場合null
     */
    @Override
    public CefResourceHandler getResourceHandler(CefBrowser browser, CefFrame frame, CefRequest request) {
        String url = request.getURL();
        log("getResourceHandler called for URL: " + url);

        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            String scheme = uri.getScheme();

            // HTTPまたはHTTPSリクエストのみ処理
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return null;
            }

            if (host == null || host.isEmpty()) {
                return null;
            }

            // IPvMアドレスの場合
            if (IPVM_PATTERN.matcher(host).matches()) {
                log("IPvM address detected: " + host);
                return new VirtualNetworkResourceHandler(kernel, host, uri.getPath(), url);
            }

            // app.local の場合（アプリアセット）
            if (APP_LOCAL_HOST.equalsIgnoreCase(host)) {
                log("App asset request detected: " + url);
                return new AppAssetResourceHandler(kernel, uri.getPath());
            }

            // その他は通常のHTTP処理
            return null;

        } catch (Exception e) {
            logError("Error parsing URL: " + url + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * ログ出力（INFO）。
     */
    private void log(String message) {
        System.out.println("[MochiResourceRequestHandler] " + message);
        System.out.flush();
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().info("MochiResourceRequestHandler", message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        System.err.println("[MochiResourceRequestHandler] " + message);
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("MochiResourceRequestHandler", message);
        }
    }
}
