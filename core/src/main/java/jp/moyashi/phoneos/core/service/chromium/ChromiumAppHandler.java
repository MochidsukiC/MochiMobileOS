package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.httpm.HttpmSchemeHandlerFactory;
import jp.moyashi.phoneos.core.service.chromium.webapp.AppAssetSchemeHandlerFactory;
import jp.moyashi.phoneos.core.service.chromium.webapp.AppSchemeManager;
import org.cef.CefApp;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandlerAdapter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chromium AppHandler。
 * カスタムスキーム（httpm:, app-*:）の登録と、スキームハンドラーファクトリの登録を行う。
 *
 * アーキテクチャ:
 * - onRegisterCustomSchemes(): httpm:, app-*:スキームを標準スキームとして登録
 * - onContextInitialized(): HttpmSchemeHandlerFactory, AppAssetSchemeHandlerFactoryを登録
 * - registerAppScheme(): 動的にapp-{modid}スキームを登録（WebApp用）
 *
 * jcefmaven依存を削除し、org.cef.*のみに依存。
 *
 * @author MochiOS Team
 * @version 3.0
 */
public class ChromiumAppHandler extends CefAppHandlerAdapter {

    private final Kernel kernel;

    /**
     * 事前登録が必要なapp-*スキームのセット。
     * onRegisterCustomSchemes()の前にregisterAppScheme()で追加される。
     */
    private static final Set<String> pendingAppSchemes = ConcurrentHashMap.newKeySet();

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
     * JCEFが初期化される前に呼び出される。
     *
     * @param registrar CefSchemeRegistrar
     */
    @Override
    public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
        log("Registering custom schemes...");

        // httpm:スキームを標準スキームとして登録
        // is_standard=true: 標準的なHTTPスキームと同じ動作
        // is_local=false: ローカルリソースではない
        // is_display_isolated=false: 表示分離なし
        boolean httpmRegistered = registrar.addCustomScheme(
            "httpm",    // スキーム名
            true,       // is_standard
            false,      // is_local
            false,      // is_display_isolated
            false,      // is_secure
            false,      // is_cors_enabled
            false,      // is_csp_bypassing
            false       // is_fetch_enabled
        );

        if (httpmRegistered) {
            log("httpm:// scheme registered successfully");
        } else {
            logError("Failed to register httpm:// scheme");
        }

        // WebApp用 mochiapp:// スキームを登録（汎用スキーム）
        // URL形式: mochiapp://modid/path/to/resource
        // 注意: スキーム名にハイフンを含めるとJCEFで問題が発生する可能性があるため、
        //       シンプルな名前を使用
        boolean mochiosAppRegistered = registrar.addCustomScheme(
            "mochiapp",  // スキーム名
            true,           // is_standard
            true,           // is_local (ローカルリソース)
            false,          // is_display_isolated
            true,           // is_secure (セキュアとして扱う)
            true,           // is_cors_enabled (CORS有効化)
            false,          // is_csp_bypassing
            true            // is_fetch_enabled (fetch API対応)
        );

        if (mochiosAppRegistered) {
            log("mochiapp:// scheme registered successfully");
        } else {
            logError("Failed to register mochiapp:// scheme");
        }

        // 後方互換性: 事前登録済みapp-*スキームも登録
        for (String scheme : pendingAppSchemes) {
            boolean appRegistered = registrar.addCustomScheme(
                scheme,     // スキーム名
                true,       // is_standard
                true,       // is_local (ローカルリソース)
                false,      // is_display_isolated
                true,       // is_secure (セキュアとして扱う)
                true,       // is_cors_enabled (CORS有効化)
                false,      // is_csp_bypassing
                true        // is_fetch_enabled (fetch API対応)
            );

            if (appRegistered) {
                log(scheme + ":// scheme registered successfully");
            } else {
                logError("Failed to register " + scheme + ":// scheme");
            }
        }
    }

    /**
     * WebApp用スキームを事前登録する（CefApp初期化前に呼び出す）。
     * 注意: この機能は現在のJCEF/MCEFの制約により、アプリ起動前に
     * すべてのスキームを登録しておく必要がある場合に使用する。
     *
     * @param schemeName スキーム名（"app-modid" 形式）
     */
    public static void preRegisterAppScheme(String schemeName) {
        if (!contextInitialized) {
            pendingAppSchemes.add(schemeName);
            System.out.println("[ChromiumAppHandler] Pre-registered scheme: " + schemeName);
        } else {
            System.err.println("[ChromiumAppHandler] Cannot pre-register scheme after context initialized: " + schemeName);
        }
    }

    /**
     * コンテキスト初期化完了時に呼び出される。
     * スキームハンドラーファクトリを登録する。
     */
    @Override
    public void onContextInitialized() {
        log("Context initialized, registering scheme handler factories");
        contextInitialized = true;

        CefApp cefApp = CefApp.getInstance();

        // httpm:スキームハンドラーファクトリを登録
        cefApp.registerSchemeHandlerFactory(
            "httpm",                                    // スキーム名
            "",                                         // ドメイン（空=全ドメイン対応）
            new HttpmSchemeHandlerFactory(kernel)       // ハンドラーファクトリ
        );
        log("HttpmSchemeHandlerFactory registered for httpm://");

        // WebApp用 mochiapp:// スキームハンドラーファクトリを登録
        AppAssetSchemeHandlerFactory appFactory = new AppAssetSchemeHandlerFactory(kernel);
        cefApp.registerSchemeHandlerFactory(
            "mochiapp",                             // スキーム名
            "",                                     // ドメイン（空=全ドメイン対応）
            appFactory                              // ハンドラーファクトリ
        );
        log("AppAssetSchemeHandlerFactory registered for mochiapp://");

        // 後方互換性: 事前登録済みapp-*スキームのファクトリも登録
        for (String scheme : pendingAppSchemes) {
            cefApp.registerSchemeHandlerFactory(
                scheme,                                 // スキーム名
                "",                                     // ドメイン（空=全ドメイン対応）
                appFactory                              // ハンドラーファクトリ
            );
            log("AppAssetSchemeHandlerFactory registered for " + scheme + "://");
        }

        log("All scheme handler factories registered");
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
