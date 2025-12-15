package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandlerAdapter;

/**
 * Chromium AppHandler。
 *
 * 注意: IPvM over HTTP アーキテクチャ移行により、カスタムスキーム登録は廃止されました。
 * リクエストのインターセプトは CefRequestHandler.getResourceRequestHandler() で行います。
 * 詳細: docs/development/10_Proposal_IPvM_over_HTTP.md
 *
 * @author MochiOS Team
 * @version 4.0
 */
public class ChromiumAppHandler extends CefAppHandlerAdapter {

    private final Kernel kernel;

    /**
     * コンテキスト初期化済みフラグ。
     */
    private static volatile boolean contextInitialized = false;

    /**
     * ChromiumAppHandlerを構築する。
     *
     * @param kernel Kernelインスタンス
     */
    public ChromiumAppHandler(Kernel kernel) {
        super(new String[0]); // CefAppHandlerAdapterはString[]引数を要求する
        this.kernel = kernel;
    }

    /**
     * カスタムスキームを登録する。
     * IPvM over HTTP移行により、カスタムスキーム登録は不要になりました。
     *
     * @param registrar CefSchemeRegistrar
     */
    @Override
    public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
        log("onRegisterCustomSchemes called (no custom schemes to register - using IPvM over HTTP)");
        // カスタムスキーム登録は廃止
        // リクエストのインターセプトはCefRequestHandler.getResourceRequestHandler()で行う
    }

    /**
     * コンテキスト初期化完了時に呼び出される。
     */
    @Override
    public void onContextInitialized() {
        log("Context initialized");
        contextInitialized = true;
        // SchemeHandlerFactory登録は廃止
        // リクエストのインターセプトはCefRequestHandler.getResourceRequestHandler()で行う
    }

    /**
     * コンテキストが初期化済みかどうかを返す。
     *
     * @return 初期化済みの場合true
     */
    public static boolean isContextInitialized() {
        return contextInitialized;
    }

    /**
     * ログ出力（INFO）。
     */
    private void log(String message) {
        System.out.println("[ChromiumAppHandler] " + message);
        if (kernel.getLogger() != null) {
            kernel.getLogger().info("ChromiumAppHandler", message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        System.err.println("[ChromiumAppHandler] " + message);
        if (kernel.getLogger() != null) {
            kernel.getLogger().error("ChromiumAppHandler", message);
        }
    }
}
