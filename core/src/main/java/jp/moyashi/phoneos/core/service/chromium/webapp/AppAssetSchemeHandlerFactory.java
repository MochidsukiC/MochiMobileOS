package jp.moyashi.phoneos.core.service.chromium.webapp;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

/**
 * app-{modid}:// カスタムスキームハンドラーファクトリ。
 * CefAppHandler.onContextInitialized()で登録され、
 * app-スキームリクエストごとにAppAssetSchemeHandlerインスタンスを生成する。
 *
 * <p>アーキテクチャ:</p>
 * <ul>
 *   <li>{@link #create}: app-スキームリクエストごとにAppAssetSchemeHandlerを生成</li>
 *   <li>Kernelインスタンスを各ハンドラーに渡す</li>
 *   <li>スキーム名はリクエストURLから動的に抽出</li>
 * </ul>
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class AppAssetSchemeHandlerFactory implements CefSchemeHandlerFactory {

    private final Kernel kernel;

    /**
     * AppAssetSchemeHandlerFactoryを構築する。
     *
     * @param kernel Kernelインスタンス
     */
    public AppAssetSchemeHandlerFactory(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * リクエストごとにAppAssetSchemeHandlerを生成する。
     *
     * @param browser CefBrowser
     * @param frame CefFrame
     * @param schemeName スキーム名（"mochiapp" または "app-modid" 形式）
     * @param request CefRequest
     * @return AppAssetSchemeHandlerインスタンス、またはnull
     */
    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        System.out.println("[AppAssetSchemeHandlerFactory] create() called - scheme: " + schemeName + ", url: " + (request != null ? request.getURL() : "null"));

        // mochiapp:// スキームを処理
        if ("mochiapp".equals(schemeName)) {
            System.out.println("[AppAssetSchemeHandlerFactory] Creating handler for mochiapp:// scheme");
            return new AppAssetSchemeHandler(kernel, schemeName);
        }

        // 後方互換性: app-で始まるスキームも処理
        if (schemeName != null && schemeName.startsWith("app-")) {
            System.out.println("[AppAssetSchemeHandlerFactory] Creating handler for legacy scheme: " + schemeName);
            return new AppAssetSchemeHandler(kernel, schemeName);
        }

        // その他のスキームはnullを返す（処理しない）
        System.out.println("[AppAssetSchemeHandlerFactory] Unknown scheme, returning null: " + schemeName);
        return null;
    }
}
