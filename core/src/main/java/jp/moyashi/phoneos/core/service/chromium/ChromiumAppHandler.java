package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.httpm.HttpmSchemeHandlerFactory;
import org.cef.CefApp;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandlerAdapter;

/**
 * Chromium AppHandler。
 * カスタムスキーム（httpm:）の登録と、スキームハンドラーファクトリの登録を行う。
 *
 * アーキテクチャ:
 * - onRegisterCustomSchemes(): httpm:スキームを標準スキームとして登録
 * - onContextInitialized(): HttpmSchemeHandlerFactoryを登録
 *
 * jcefmaven依存を削除し、org.cef.*のみに依存。
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class ChromiumAppHandler extends CefAppHandlerAdapter {

    private final Kernel kernel;

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
     * JCEFが初期化される前に呼び出される。
     *
     * @param registrar CefSchemeRegistrar
     */
    @Override
    public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
        log("Registering custom scheme: httpm://");

        // httpm:スキームを標準スキームとして登録
        // is_standard=true: 標準的なHTTPスキームと同じ動作
        // is_local=false: ローカルリソースではない
        // is_display_isolated=false: 表示分離なし
        boolean registered = registrar.addCustomScheme(
            "httpm",    // スキーム名
            true,       // is_standard
            false,      // is_local
            false,      // is_display_isolated
            false,      // is_secure
            false,      // is_cors_enabled
            false,      // is_csp_bypassing
            false       // is_fetch_enabled
        );

        if (registered) {
            log("httpm:// scheme registered successfully");
        } else {
            logError("Failed to register httpm:// scheme");
        }
    }

    /**
     * コンテキスト初期化完了時に呼び出される。
     * スキームハンドラーファクトリを登録する。
     */
    @Override
    public void onContextInitialized() {
        log("Context initialized, registering scheme handler factory");

        // httpm:スキームハンドラーファクトリを登録
        CefApp cefApp = CefApp.getInstance();
        cefApp.registerSchemeHandlerFactory(
            "httpm",                                    // スキーム名
            "",                                         // ドメイン（空=全ドメイン対応）
            new HttpmSchemeHandlerFactory(kernel)       // ハンドラーファクトリ
        );

        log("HttpmSchemeHandlerFactory registered for httpm://");
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
