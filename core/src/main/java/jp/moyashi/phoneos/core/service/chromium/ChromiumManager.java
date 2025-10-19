package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.VFS;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefSettings;

/**
 * Chromium統合管理サービス。
 * JCEFのシングルトン初期化と設定を管理する。
 *
 * アーキテクチャ:
 * - CefApp.getInstance()でJCEFを初期化
 * - httpm:カスタムスキームをサポート
 * - キャッシュとCookieをVFSに保存
 * - Kernelサービスとして動作
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class ChromiumManager {

    private final Kernel kernel;
    private CefApp cefApp;
    private ChromiumAppHandler appHandler;
    private boolean initialized = false;

    /**
     * ChromiumManagerを構築する。
     *
     * @param kernel Kernelインスタンス
     */
    public ChromiumManager(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * JCEFを初期化する。
     * このメソッドはKernel.setup()から呼び出される。
     */
    public void initialize() {
        if (initialized) {
            log("Chromium already initialized");
            return;
        }

        log("Initializing JCEF (Java Chromium Embedded Framework)...");

        try {
            // ChromiumAppHandlerを作成
            appHandler = new ChromiumAppHandler(kernel);

            // CefAppBuilderを使用してJCEFを初期化（jcefmavenによる自動ダウンロード対応）
            CefAppBuilder builder = new CefAppBuilder();
            builder.setAppHandler(appHandler);

            // CefSettingsを取得して設定
            CefSettings settings = builder.getCefSettings();

            // キャッシュパス（VFS内）
            String cachePath = kernel.getVFS().getFullPath("system/browser_chromium/cache");
            settings.cache_path = cachePath;
            log("Cache path: " + cachePath);

            // User-Agent（モバイル最適化）
            settings.user_agent = "Mozilla/5.0 (Linux; Android 12; MochiMobileOS) " +
                                  "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                  "Chrome/122.0.0.0 Mobile Safari/537.36";

            // ロケール
            settings.locale = "ja";

            // ログレベル
            settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_WARNING;

            // オフスクリーンレンダリング有効化
            settings.windowless_rendering_enabled = true;

            // JCEFをビルドして初期化
            cefApp = builder.build();

            initialized = true;
            log("JCEF initialized successfully");
            log("Chromium version: " + cefApp.getVersion());

        } catch (Exception e) {
            logError("Failed to initialize JCEF", e);
            throw new RuntimeException("JCEF initialization failed", e);
        }
    }

    /**
     * ChromiumBrowserインスタンスを作成する。
     *
     * @param url 初期URL
     * @param width 幅
     * @param height 高さ
     * @return ChromiumBrowserインスタンス
     */
    public ChromiumBrowser createBrowser(String url, int width, int height) {
        if (!initialized) {
            throw new IllegalStateException("ChromiumManager is not initialized");
        }

        log("Creating ChromiumBrowser: " + url + " (" + width + "x" + height + ")");
        return new ChromiumBrowser(kernel, cefApp, url, width, height);
    }

    /**
     * JCEFが初期化されているかを確認する。
     *
     * @return 初期化済みの場合true
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * CefAppインスタンスを取得する。
     *
     * @return CefAppインスタンス
     */
    public CefApp getCefApp() {
        return cefApp;
    }

    /**
     * CEFメッセージループを実行する。
     * Kernel.update()から毎フレーム呼び出される必要がある。
     *
     * このメソッドは、CEFのURL読み込み、レンダリング、イベント処理を駆動する。
     * 呼び出さない場合、ブラウザは完全に動作しない。
     */
    public void doMessageLoopWork() {
        if (!initialized || cefApp == null) {
            return;
        }

        try {
            // CEFメッセージループを実行（delay=0で即座に実行）
            // これがURL読み込み、onPaint()コールバック、イベント処理を駆動する
            cefApp.doMessageLoopWork(0);

            // デバッグログ（フレーム数を100で割った余りが0のときだけ出力）
            if (System.currentTimeMillis() % 1000 < 16) {
                log("doMessageLoopWork() called successfully");
            }
        } catch (Exception e) {
            // エラーログは出すが、例外は飲み込んで処理を継続
            // メッセージループの失敗でKernel全体が停止するのを防ぐ
            logError("Error in CEF message loop", e);
        }
    }

    /**
     * シャットダウン処理。
     * Kernel.shutdown()から呼び出される。
     */
    public void shutdown() {
        if (!initialized) {
            return;
        }

        log("Shutting down JCEF...");

        try {
            if (cefApp != null) {
                cefApp.dispose();
                cefApp = null;
            }

            initialized = false;
            log("JCEF shutdown complete");

        } catch (Exception e) {
            logError("Error during JCEF shutdown", e);
        }
    }

    /**
     * ログ出力（INFO）。
     */
    private void log(String message) {
        System.out.println("[ChromiumManager] " + message);
        if (kernel.getLogger() != null) {
            kernel.getLogger().info("ChromiumManager", message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message, Throwable e) {
        System.err.println("[ChromiumManager] " + message + ": " + e.getMessage());
        if (kernel.getLogger() != null) {
            kernel.getLogger().error("ChromiumManager", message, e);
        }
        e.printStackTrace();
    }
}
