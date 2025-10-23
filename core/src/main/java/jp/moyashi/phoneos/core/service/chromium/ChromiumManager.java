package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.CefApp;

/**
 * Chromium統合管理サービス。
 * ChromiumProviderを通じてJCEFの初期化と設定を管理する。
 *
 * アーキテクチャ（依存性注入パターン）:
 * - ChromiumProviderを外部から注入（standaloneやforgeモジュールが提供）
 * - coreモジュールはjcefmavenやMCEFに直接依存しない
 * - 各環境が適切なプロバイダー実装を提供する
 *
 * 使用方法:
 * 1. ChromiumManagerインスタンスを作成
 * 2. setProvider()で環境に応じたプロバイダーを注入
 * 3. initialize()でCefAppを初期化
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class ChromiumManager {

    private final Kernel kernel;
    private CefApp cefApp;
    private ChromiumProvider provider;
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
     * ChromiumProviderを設定する。
     * initialize()の前に呼び出す必要がある。
     *
     * 上位モジュール（standaloneやforge）が環境に応じた実装を注入する。
     *
     * @param provider ChromiumProvider実装
     */
    public void setProvider(ChromiumProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("ChromiumProvider cannot be null");
        }
        this.provider = provider;
        log("ChromiumProvider set: " + provider.getName());
    }

    /**
     * Chromiumを初期化する。
     * このメソッドはKernel.setup()から呼び出される。
     *
     * 事前条件: setProvider()でChromiumProviderが設定されている必要がある。
     */
    public void initialize() {
        if (initialized) {
            log("Chromium already initialized");
            return;
        }

        if (provider == null) {
            logError("ChromiumProvider not set. Call setProvider() before initialize().", null);
            throw new IllegalStateException("ChromiumProvider not set. Upper module must call setProvider() first.");
        }

        if (!provider.isAvailable()) {
            log("Chromium provider " + provider.getName() + " is not available, skipping initialization");
            return;
        }

        log("Initializing Chromium with provider: " + provider.getName());

        try {
            cefApp = provider.createCefApp(kernel);
            initialized = true;
            log("Chromium initialized successfully");
            log("Chromium version: " + cefApp.getVersion());

        } catch (Exception e) {
            logError("Failed to initialize Chromium", e);
            throw new RuntimeException("Chromium initialization failed", e);
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

        if (provider == null) {
            throw new IllegalStateException("ChromiumProvider is not set");
        }
        log("Creating ChromiumBrowser: " + url + " (" + width + "x" + height + ")");
        return new ChromiumBrowser(kernel, cefApp, provider, url, width, height);
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
     * ChromiumProviderインスタンスを取得する。
     *
     * @return ChromiumProviderインスタンス
     */
    public ChromiumProvider getProvider() {
        return provider;
    }

    /**
     * CEFメッセージループを実行する。
     * Kernel.update()から毎フレーム呼び出される必要がある。
     *
     * このメソッドは、CEFのURL読み込み、レンダリング、イベント処理を駆動する。
     * 呼び出さない場合、ブラウザは完全に動作しない。
     */
    public void doMessageLoopWork() {
        if (!initialized || cefApp == null || provider == null) {
            return;
        }

        long startNs = System.nanoTime();
        try {
            // プロバイダー経由でメッセージループを実行
            provider.doMessageLoopWork(cefApp);

            long durationNs = System.nanoTime() - startNs;
            // ログI/O負荷軽減: 10ms以上のみ出力（YouTube動画再生時のフレームレート維持）
            if (durationNs > 10_000_000L && kernel.getLogger() != null) { // >10ms
                double ms = durationNs / 1_000_000.0;
                String message = String.format("MessageLoopWork took %.3fms", ms);
                if (durationNs > 50_000_000L) { // >50ms
                    kernel.getLogger().warn("ChromiumPump", message);
                } else {
                    kernel.getLogger().debug("ChromiumPump", message);
                }
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

        log("Shutting down Chromium...");

        try {
            if (provider != null && cefApp != null) {
                provider.shutdown(cefApp);
            }

            cefApp = null;
            initialized = false;
            log("Chromium shutdown complete");

        } catch (Exception e) {
            logError("Error during Chromium shutdown", e);
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
