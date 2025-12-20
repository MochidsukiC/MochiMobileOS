package jp.moyashi.phoneos.core.service.chromium.interceptor;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * IPvMアドレス用HTTPスキームハンドラーファクトリ。
 * HTTPスキームに対して登録され、IPvMアドレスパターンにマッチするリクエストのみを処理する。
 *
 * <p>IPvMアドレスパターン: [type]-[identifier]</p>
 * <ul>
 *   <li>0-uuid: Player</li>
 *   <li>1-uuid: Device</li>
 *   <li>2-identifier: Server</li>
 *   <li>3-identifier: System</li>
 * </ul>
 *
 * <p>マッチしないリクエストはnullを返し、通常のHTTP処理に任せる。</p>
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class IPvMSchemeHandlerFactory implements CefSchemeHandlerFactory {

    private final Kernel kernel;

    /**
     * IPvMアドレスパターン: [type]-[identifier]
     * type: 0=Player, 1=Device, 2=Server, 3=System
     *
     * identifier形式:
     * - Player/Device (0,1): 標準UUID形式 (xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx)
     * - Server/System (2,3): 文字列ID形式
     */
    private static final Pattern IPVM_PATTERN = Pattern.compile(
        "^[0-3]-(" +
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" + // 標準UUID
        "|" +
        "[a-zA-Z0-9][a-zA-Z0-9_-]*" + // 文字列ID（Server/System用）
        ")$"
    );

    /**
     * IPvMSchemeHandlerFactoryを構築する。
     *
     * @param kernel Kernelインスタンス
     */
    public IPvMSchemeHandlerFactory(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * リクエストごとにリソースハンドラーを生成する。
     * IPvMアドレスパターンにマッチする場合のみVirtualNetworkResourceHandlerを返す。
     *
     * @param browser CefBrowser
     * @param frame CefFrame
     * @param schemeName スキーム名（"http" または "https"）
     * @param request CefRequest
     * @return VirtualNetworkResourceHandler（IPvMの場合）、または null（通常HTTP）
     */
    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        String url = request.getURL();
        // デバッグ: このメソッドが呼び出されたことをログに記録
        System.out.println("[IPvMSchemeHandlerFactory] create() called for URL: " + url);
        log("create() called for URL: " + url);

        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            String scheme = uri.getScheme();

            // HTTPまたはHTTPSスキームのみ処理
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return null;
            }

            if (host == null || host.isEmpty()) {
                return null;
            }

            // IPvMアドレスパターンにマッチするか確認
            if (IPVM_PATTERN.matcher(host).matches()) {
                log("IPvM address detected: " + host + " - creating VirtualNetworkResourceHandler");
                String path = uri.getPath();
                return new VirtualNetworkResourceHandler(kernel, host, path, url);
            }

            // IPvMパターンにマッチしない場合はnullを返す（通常のHTTP処理に任せる）
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
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().info("IPvMSchemeHandlerFactory", message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("IPvMSchemeHandlerFactory", message);
        }
    }
}
