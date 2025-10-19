package jp.moyashi.phoneos.core.service.webview;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import jp.moyashi.phoneos.core.Kernel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JavaFX WebViewを管理するサービス。
 * オフスクリーンレンダリング、HTMLウィジェット作成、JavaScriptブリッジを提供する。
 *
 * 機能:
 * - フルスクリーンHTMLアプリのレンダリング
 * - 部分的なHTMLウィジェット（埋め込み）のレンダリング
 * - JavaScript↔Java通信ブリッジ（MochiOS API）
 *
 * 外部アプリ開発者はこのサービスを通じて、HTML/CSS/JavaScriptでアプリを作成できる。
 */
public class WebViewManager {
    private final Kernel kernel;
    private final int defaultWidth;
    private final int defaultHeight;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final List<WebViewWrapper> activeWrappers = new ArrayList<>();

    private JSBridge jsBridge;

    /**
     * ログ出力ヘルパーメソッド
     */
    private void log(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().info("WebViewManager", message);
        }
    }

    /**
     * エラーログ出力ヘルパーメソッド
     */
    private void logError(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("WebViewManager", message);
        }
    }

    /**
     * エラーログ出力ヘルパーメソッド（例外付き）
     */
    private void logError(String message, Throwable throwable) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("WebViewManager", message, throwable);
        }
    }

    /**
     * WebViewManagerを作成する。
     *
     * @param kernel Kernelインスタンス（JavaScriptブリッジで使用）
     * @param width デフォルト幅
     * @param height デフォルト高さ
     */
    public WebViewManager(Kernel kernel, int width, int height) {
        this.kernel = kernel;
        this.defaultWidth = width;
        this.defaultHeight = height;
    }

    /**
     * JavaFX Platformを初期化する。
     * 注意: この処理は1度だけ呼び出される必要がある。
     */
    public void initialize() {
        if (initialized.getAndSet(true)) {
            log("Already initialized");
            return;
        }

        log("Initializing JavaFX Platform...");
        CountDownLatch latch = new CountDownLatch(1);

        // JavaFX Platformを起動
        Platform.startup(() -> {
            log("JavaFX Platform started");
            latch.countDown();
        });

        try {
            latch.await(); // 初期化完了を待つ
        } catch (InterruptedException e) {
            logError("Initialization interrupted");
            e.printStackTrace();
            return;
        }

        // JavaScriptブリッジを初期化
        jsBridge = new JSBridge(kernel);

        log("Initialization complete");
        log("  - Default size: " + defaultWidth + "x" + defaultHeight);
        log("  - JavaScript Bridge: Ready");
    }

    /**
     * 新しいフルスクリーンWebViewインスタンスを作成する。
     * アプリ全体をHTMLで定義する場合に使用。
     *
     * @return WebViewWrapper インスタンス
     */
    public WebViewWrapper createWebView() {
        return createWebView(defaultWidth, defaultHeight);
    }

    /**
     * 新しいWebViewインスタンスを作成する（サイズ指定版）。
     *
     * @param width 幅
     * @param height 高さ
     * @return WebViewWrapper インスタンス
     */
    public WebViewWrapper createWebView(int width, int height) {
        if (!initialized.get()) {
            throw new IllegalStateException("WebViewManager not initialized. Call initialize() first.");
        }

        WebViewWrapper[] wrapper = new WebViewWrapper[1];
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                WebView webView = new WebView();
                webView.setPrefSize(width, height);
                webView.setMaxSize(width, height);
                webView.setMinSize(width, height);

                // オフスクリーンレンダリング用のSceneを作成
                StackPane root = new StackPane(webView);
                Scene scene = new Scene(root, width, height);

                // 非表示のStageを作成（WebViewレンダリングに必須）
                Stage stage = new Stage(StageStyle.UTILITY);
                stage.setScene(scene);
                stage.setWidth(width);
                stage.setHeight(height);
                stage.setX(-10000); // 画面外に移動（レンダリングは実行される）
                stage.setY(-10000);
                stage.setOpacity(0.01); // ほぼ透明だが、レンダリングはスキップされない
                stage.show(); // Stageを表示（画面外なので見えない）

                wrapper[0] = new WebViewWrapper(webView, scene, stage, width, height, jsBridge, kernel);
                activeWrappers.add(wrapper[0]);

                log("WebView created (" + width + "x" + height + ") with offscreen Stage");
            } catch (Exception e) {
                logError("Failed to create WebView");
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return wrapper[0];
    }

    /**
     * 新しいHTMLウィジェットを作成する。
     * Java画面の一部にHTMLを埋め込む場合に使用（例: ウェブブラウザのレンダリングエリア）。
     *
     * @param width 幅
     * @param height 高さ
     * @return HTMLWidget インスタンス
     */
    public HTMLWidget createWidget(int width, int height) {
        if (!initialized.get()) {
            throw new IllegalStateException("WebViewManager not initialized. Call initialize() first.");
        }

        WebViewWrapper wrapper = createWebView(width, height);
        return new HTMLWidget(wrapper, width, height);
    }

    /**
     * WebViewWrapperを破棄する。
     * メモリリークを防ぐため、不要になったWebViewは必ず破棄すること。
     *
     * @param wrapper 破棄するWebViewWrapper
     */
    public void destroyWebView(WebViewWrapper wrapper) {
        if (wrapper != null) {
            activeWrappers.remove(wrapper);
            wrapper.dispose();
            log("WebView destroyed");
        }
    }

    /**
     * HTMLウィジェットを破棄する。
     *
     * @param widget 破棄するHTMLWidget
     */
    public void destroyWidget(HTMLWidget widget) {
        if (widget != null) {
            destroyWebView(widget.getWrapper());
        }
    }

    /**
     * JavaScriptブリッジを取得する。
     * 外部アプリ開発者がカスタムJSハンドラを追加する場合に使用。
     *
     * @return JSBridge インスタンス
     */
    public JSBridge getJSBridge() {
        return jsBridge;
    }

    /**
     * 初期化済みかどうかを確認する。
     *
     * @return 初期化済みの場合true
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * システムシャットダウン時の処理。
     * すべてのWebViewを破棄し、JavaFX Platformをシャットダウンする。
     */
    public void shutdown() {
        if (initialized.get()) {
            log("Shutting down...");

            // すべてのWebViewを破棄
            for (WebViewWrapper wrapper : new ArrayList<>(activeWrappers)) {
                destroyWebView(wrapper);
            }

            // JavaFX Platformをシャットダウン
            Platform.exit();

            log("Shutdown complete");
            log("  - Destroyed " + activeWrappers.size() + " WebView instances");
        }
    }
}
