package jp.moyashi.phoneos.core.service.chromium.httpm;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

/**
 * httpm:カスタムスキームハンドラーファクトリ。
 * CefAppHandler.onContextInitialized()で登録され、
 * httpm:リクエストごとにHttpmSchemeHandlerインスタンスを生成する。
 *
 * アーキテクチャ:
 * - create(): httpm:リクエストごとにHttpmSchemeHandlerを生成
 * - Kernelインスタンスを各ハンドラーに渡す
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class HttpmSchemeHandlerFactory implements CefSchemeHandlerFactory {

    private final Kernel kernel;

    /**
     * HttpmSchemeHandlerFactoryを構築する。
     *
     * @param kernel Kernelインスタンス
     */
    public HttpmSchemeHandlerFactory(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * リクエストごとにHttpmSchemeHandlerを生成する。
     *
     * @param browser CefBrowser
     * @param frame CefFrame
     * @param schemeName スキーム名（"httpm"）
     * @param request CefRequest
     * @return HttpmSchemeHandlerインスタンス
     */
    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        // httpm:スキームの場合のみハンドラーを返す
        if ("httpm".equals(schemeName)) {
            return new HttpmSchemeHandler(kernel);
        }

        // その他のスキームはnullを返す（処理しない）
        return null;
    }
}
