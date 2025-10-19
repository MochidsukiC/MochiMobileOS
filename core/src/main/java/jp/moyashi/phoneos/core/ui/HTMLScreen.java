package jp.moyashi.phoneos.core.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.webview.WebViewWrapper;
import processing.core.PGraphics;

/**
 * HTML/CSS/JavaScriptで定義されたUIを表示する画面の基底クラス。
 * JavaFX WebViewを使用してHTMLをレンダリングし、PGraphicsに変換する。
 *
 * 使用例:
 * <pre>
 * public class MyHTMLApp extends HTMLScreen {
 *     public MyHTMLApp(Kernel kernel) {
 *         super(kernel);
 *     }
 *
 *     @Override
 *     protected String getHTMLContent() {
 *         return kernel.getVFS().readString("apps/myapp/index.html");
 *     }
 *
 *     @Override
 *     public String getScreenTitle() {
 *         return "My HTML App";
 *     }
 * }
 * </pre>
 *
 * HTML内でのMochiOS API使用例:
 * <pre>
 * &lt;script&gt;
 *   // 通知表示
 *   MochiOS.showNotification("タイトル", "メッセージ");
 *
 *   // VFSからデータ読み込み
 *   var data = MochiOS.vfs.readString("apps/myapp/data.json");
 *
 *   // ハードウェアAPI
 *   var battery = MochiOS.hardware.getBatteryLevel();
 * &lt;/script&gt;
 * </pre>
 */
public abstract class HTMLScreen implements Screen {
    protected WebViewWrapper webViewWrapper;
    protected Kernel kernel;
    protected boolean initialized = false;
    protected int loadingFrame = 0; // ローディングアニメーション用フレームカウンタ

    /**
     * HTMLScreenを作成する。
     *
     * @param kernel Kernelインスタンス
     */
    public HTMLScreen(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void setup(PGraphics pg) {
        if (kernel.getLogger() != null) {
            kernel.getLogger().debug("HTMLScreen", "setup() called for " + getScreenTitle());
        }

        if (initialized) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().debug("HTMLScreen", "Already initialized");
            }
            return;
        }

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("HTMLScreen", "Setting up for " + getScreenTitle());
        }

        // WebViewManagerが利用可能か確認
        if (kernel.getWebViewManager() == null) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("HTMLScreen", "WebViewManager not available - make sure kernel.getWebViewManager().initialize() is called");
            }
            return;
        }

        if (!kernel.getWebViewManager().isInitialized()) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("HTMLScreen", "WebViewManager not initialized");
            }
            return;
        }

        try {
            // WebViewを作成
            webViewWrapper = kernel.getWebViewManager().createWebView();

            if (webViewWrapper != null) {
                // HTMLコンテンツを読み込む
                String htmlContent = getHTMLContent();

                if (htmlContent != null && !htmlContent.isEmpty()) {
                    if (kernel.getLogger() != null) {
                        kernel.getLogger().debug("HTMLScreen", "Loading HTML content (" + htmlContent.length() + " characters)");
                    }
                    webViewWrapper.loadContent(htmlContent);
                } else {
                    String url = getHTMLURL();
                    if (url != null && !url.isEmpty()) {
                        if (kernel.getLogger() != null) {
                            kernel.getLogger().debug("HTMLScreen", "Loading URL: " + url);
                        }
                        webViewWrapper.loadURL(url);
                    } else {
                        if (kernel.getLogger() != null) {
                            kernel.getLogger().error("HTMLScreen", "No HTML content or URL provided");
                        }
                    }
                }

                initialized = true;
                if (kernel.getLogger() != null) {
                    kernel.getLogger().info("HTMLScreen", "Setup complete for " + getScreenTitle());
                }
            } else {
                if (kernel.getLogger() != null) {
                    kernel.getLogger().error("HTMLScreen", "Failed to create WebView");
                }
            }
        } catch (Exception e) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("HTMLScreen", "Setup failed", e);
            }
        }
    }

    @Override
    public void draw(PGraphics pg) {
        if (!initialized || webViewWrapper == null || webViewWrapper.isDisposed()) {
            // フォールバック表示
            drawFallback(pg);
            return;
        }

        // スリープ中またはバックグラウンド状態の場合は、WebViewの更新を停止してCPU/GPU使用率を削減
        if (kernel.isSleeping() || isInBackground) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().debug("HTMLScreen", "Skipping draw - isSleeping=" + kernel.isSleeping() +
                    ", isInBackground=" + isInBackground + ", screen=" + getScreenTitle());
            }
            return;
        }

        try {
            // WebViewの準備ができていない場合
            if (!webViewWrapper.isReady()) {
                // スナップショット取得を進めるため、renderを呼び出す
                // （結果は表示しないが、非同期処理を進める必要がある）
                webViewWrapper.renderToPGraphics(pg);

                // ローディング画面で上書き（WebViewの白画面を隠す）
                drawLoadingScreen(pg);
                loadingFrame++; // アニメーション用フレームカウンタを進める
            } else {
                // WebViewの準備ができたら、通常通り描画
                webViewWrapper.renderToPGraphics(pg);
            }
        } catch (Exception e) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("HTMLScreen", "Render error", e);
            }
            drawFallback(pg);
        }
    }

    /**
     * WebViewが利用できない場合のフォールバック表示。
     *
     * @param pg PGraphicsインスタンス
     */
    protected void drawFallback(PGraphics pg) {
        pg.background(50);
        pg.fill(255, 0, 0);
        pg.textAlign(processing.core.PApplet.CENTER, processing.core.PApplet.CENTER);
        pg.textSize(16);
        pg.text("WebView not initialized", pg.width / 2, pg.height / 2 - 20);
        pg.textSize(12);
        pg.text("Make sure WebViewManager is initialized", pg.width / 2, pg.height / 2 + 10);
    }

    /**
     * HTML読み込み中のローディング画面を表示する。
     * スピナーアニメーション付き。
     *
     * @param pg PGraphicsインスタンス
     */
    protected void drawLoadingScreen(PGraphics pg) {
        // 背景
        pg.background(30);

        // スピナーの設定
        int centerX = pg.width / 2;
        int centerY = pg.height / 2;
        int spinnerRadius = 30;
        int dotCount = 8;
        float rotation = (loadingFrame * 0.05f) % (processing.core.PApplet.TWO_PI);

        // スピナーの描画（8つの点が回転）
        pg.pushMatrix();
        pg.translate(centerX, centerY);
        pg.rotate(rotation);

        for (int i = 0; i < dotCount; i++) {
            float angle = (processing.core.PApplet.TWO_PI / dotCount) * i;
            float x = processing.core.PApplet.cos(angle) * spinnerRadius;
            float y = processing.core.PApplet.sin(angle) * spinnerRadius;

            // 透明度を変化させて回転感を出す
            float alpha = processing.core.PApplet.map(i, 0, dotCount - 1, 100, 255);
            pg.fill(100, 150, 255, alpha);
            pg.noStroke();
            pg.ellipse(x, y, 8, 8);
        }

        pg.popMatrix();

        // ローディングテキスト
        pg.fill(200);
        pg.textAlign(processing.core.PApplet.CENTER, processing.core.PApplet.CENTER);
        pg.textSize(14);
        pg.text("Loading...", centerX, centerY + 60);

        // アプリ名を表示（サブクラスでオーバーライド可能）
        pg.textSize(12);
        pg.fill(150);
        pg.text(getScreenTitle(), centerX, centerY + 85);
    }

    @Override
    public void mousePressed(PGraphics pg, int mouseX, int mouseY) {
        if (initialized && webViewWrapper != null && !webViewWrapper.isDisposed()) {
            webViewWrapper.simulateMousePressed(mouseX, mouseY);
        }
    }

    @Override
    public void mouseReleased(PGraphics pg, int mouseX, int mouseY) {
        if (initialized && webViewWrapper != null && !webViewWrapper.isDisposed()) {
            webViewWrapper.simulateMouseReleased(mouseX, mouseY);
            // HTML要素のonclickハンドラを動作させるため、clickイベントも発火
            webViewWrapper.simulateMouseClick(mouseX, mouseY);
        }
    }

    @Override
    public void mouseDragged(PGraphics pg, int mouseX, int mouseY) {
        // HTMLではマウスドラッグはmousemoveイベントとして扱われる
        // 必要に応じてサブクラスでオーバーライド
    }

    /** バックグラウンド状態かどうか */
    protected boolean isInBackground = false;

    @Override
    public void onBackground() {
        // バックグラウンドに移行する際は、WebViewの更新を停止するが破棄はしない
        // これにより、フォアグラウンドに戻った際に速やかに再開できる
        isInBackground = true;

        // OS側のAPIを呼び出してWebViewのフレーム更新を強制的に停止
        if (webViewWrapper != null && !webViewWrapper.isDisposed()) {
            webViewWrapper.setInBackground(true);
        }

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("HTMLScreen", getScreenTitle() + " moved to background (WebView kept alive)");
        }
    }

    @Override
    public void onForeground() {
        // フォアグラウンドに戻った際は、WebViewの更新を再開
        isInBackground = false;

        // OS側のAPIを呼び出してWebViewのフレーム更新を再開
        if (webViewWrapper != null && !webViewWrapper.isDisposed()) {
            webViewWrapper.setInBackground(false);
            // フォアグラウンドに戻った際は即座に更新を要求
            webViewWrapper.requestUpdate();
        }

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("HTMLScreen", getScreenTitle() + " moved to foreground (WebView resumed)");
        }
    }

    @Override
    public void cleanup(PGraphics pg) {
        // cleanup() は本当にアプリを終了する際にのみ呼び出される
        // ServiceManager によってプロセスが管理されている場合、このメソッドは呼ばれない
        // WebViewを完全に破棄してリソースを解放
        if (kernel.getLogger() != null) {
            kernel.getLogger().info("HTMLScreen", getScreenTitle() + " cleanup called - disposing WebView");
        }
        dispose();
    }

    /**
     * JavaScriptを実行する。
     *
     * @param script JavaScriptコード
     * @return 実行結果
     */
    protected Object executeScript(String script) {
        if (initialized && webViewWrapper != null && !webViewWrapper.isDisposed()) {
            return webViewWrapper.executeScript(script);
        }
        return null;
    }

    /**
     * 強制的に再描画を要求する。
     * JavaScriptでDOMを変更した後に呼び出すことで、即座に画面に反映できる。
     */
    protected void requestUpdate() {
        if (initialized && webViewWrapper != null) {
            webViewWrapper.requestUpdate();
        }
    }

    /**
     * 表示するHTMLコンテンツを取得する。
     * サブクラスでオーバーライドして実装する。
     * getHTMLURL()と併用する場合は、こちらが優先される。
     *
     * @return HTML文字列、URLを使用する場合はnullまたは空文字列
     */
    protected abstract String getHTMLContent();

    /**
     * 表示するHTMLのURLを取得する。
     * getHTMLContent()がnullまたは空文字列の場合のみ使用される。
     * デフォルト実装ではnullを返す。
     *
     * @return URL文字列、HTMLコンテンツを直接指定する場合はnull
     */
    protected String getHTMLURL() {
        return null;
    }

    /**
     * 画面が破棄される際の処理。
     * WebViewを適切にクリーンアップする。
     */
    public void dispose() {
        if (webViewWrapper != null && !webViewWrapper.isDisposed()) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().info("HTMLScreen", "Disposing WebView for " + getScreenTitle());
            }

            // WebViewManagerに破棄を依頼
            if (kernel.getWebViewManager() != null) {
                kernel.getWebViewManager().destroyWebView(webViewWrapper);
            } else {
                // フォールバック: 直接破棄
                webViewWrapper.dispose();
            }

            webViewWrapper = null;
            initialized = false;
        }
    }

    /**
     * 内部のWebViewWrapperを取得する。
     * 高度な操作が必要な場合に使用。
     *
     * @return WebViewWrapper インスタンス、初期化されていない場合はnull
     */
    protected WebViewWrapper getWebViewWrapper() {
        return webViewWrapper;
    }

    /**
     * 初期化済みかどうかを確認する。
     *
     * @return 初期化済みの場合true
     */
    public boolean isInitialized() {
        return initialized;
    }
}
