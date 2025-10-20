package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.*;
import processing.core.PGraphics;
import processing.core.PImage;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * Chromiumブラウザインスタンスのラッパークラス。
 * CefBrowserOsrを管理し、URL読み込み、イベント処理、PGraphics描画を提供する。
 *
 * アーキテクチャ:
 * - CefClient: ブラウザクライアント
 * - CefBrowser: オフスクリーンブラウザインスタンス
 * - ChromiumRenderHandler: レンダリング結果をPImageに変換
 * - loadURL(), loadContent(): ページ読み込み
 * - executeScript(): JavaScript実行
 * - マウス/キーボードイベント処理
 * - drawToPGraphics(): PGraphicsに描画
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class ChromiumBrowser {

    private final Kernel kernel;
    private final CefClient client;
    private final CefBrowser browser;
    private final ChromiumRenderHandler renderHandler;
    private final int width;
    private final int height;

    private String currentUrl = "";
    private boolean isLoading = false;

    // 隠しJFrame（ChromiumのOSRレンダリングをトリガーするために必要）
    private javax.swing.JFrame hiddenFrame;

    /**
     * ChromiumBrowserを構築する。
     *
     * @param kernel Kernelインスタンス
     * @param cefApp CefAppインスタンス
     * @param url 初期URL
     * @param width 幅
     * @param height 高さ
     */
    public ChromiumBrowser(Kernel kernel, CefApp cefApp, String url, int width, int height) {
        this.kernel = kernel;
        this.width = width;
        this.height = height;
        this.currentUrl = url;

        log("Creating ChromiumBrowser: " + url + " (" + width + "x" + height + ")");

        // ChromiumRenderHandlerを作成
        this.renderHandler = new ChromiumRenderHandler(kernel, width, height);

        // CefClientを作成（CefApp.createClient()を使用）
        this.client = cefApp.createClient();

        // ロードハンドラーを追加（ローディング状態管理）
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadStart(CefBrowser browser, CefFrame frame, org.cef.network.CefRequest.TransitionType transitionType) {
                if (frame.isMain()) {
                    isLoading = true;
                    log("Loading started: " + frame.getURL());
                }
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                if (frame.isMain()) {
                    isLoading = false;
                    currentUrl = frame.getURL();
                    log("Loading completed: " + currentUrl + " (status: " + httpStatusCode + ")");
                }
            }

            @Override
            public void onLoadError(CefBrowser browser, CefFrame frame, ErrorCode errorCode, String errorText, String failedUrl) {
                if (frame.isMain()) {
                    isLoading = false;
                    logError("Loading error: " + failedUrl + " - " + errorText);
                }
            }
        });

        // オフスクリーンブラウザを作成（3引数API）
        // jcefmaven 135.0.20の正しいAPI仕様に従う
        // arg1: url - 初期URL
        // arg2: osrEnabled - オフスクリーンレンダリング有効
        // arg3: transparent - 透過なし
        // 注: RenderHandlerはcreateBrowser()では渡せない（4番目の引数はCefRequestContext）
        log("Creating browser with URL: " + url);
        this.browser = client.createBrowser(url, true, false);

        // OSRモードでは、createImmediately()を呼び出す必要がある
        // これにより、ブラウザが即座に作成・初期化される
        if (browser != null) {
            log("Calling createImmediately() for OSR browser");
            try {
                browser.createImmediately();
                log("createImmediately() completed successfully");
            } catch (Exception e) {
                logError("createImmediately() failed: " + e.getMessage());
            }
        }

        // ブラウザインスタンスの状態を確認
        if (browser == null) {
            logError("CRITICAL: createBrowser() returned null!");
            return;
        }

        log("Browser instance created successfully");
        log("Browser class: " + browser.getClass().getName());

        // CefBrowserOsrにonPaintリスナーを登録
        // addOnPaintListener()はConsumer<CefPaintEvent>を受け取る
        // 注: CefBrowserOsrはpackage-privateなので、リフレクションでメソッドを呼び出す
        try {
            // browserがaddOnPaintListener()メソッドを持っているか確認
            java.lang.reflect.Method addListenerMethod = browser.getClass().getMethod("addOnPaintListener", java.util.function.Consumer.class);

            // モジュールアクセス制限を回避するためsetAccessible(true)を設定
            addListenerMethod.setAccessible(true);

            // onPaintイベントリスナーを作成
            // paintEvent（CefPaintEvent）からデータを取得してrenderHandlerに渡す
            java.util.function.Consumer<Object> paintListener = paintEvent -> {
                try {
                    log("🎨 Paint listener called! Event class: " + paintEvent.getClass().getName());

                    // CefPaintEventからデータを取得（リフレクション使用）
                    Class<?> eventClass = paintEvent.getClass();
                    java.nio.ByteBuffer buffer = (java.nio.ByteBuffer) eventClass.getMethod("getRenderedFrame").invoke(paintEvent);
                    int eventWidth = (Integer) eventClass.getMethod("getWidth").invoke(paintEvent);
                    int eventHeight = (Integer) eventClass.getMethod("getHeight").invoke(paintEvent);
                    java.awt.Rectangle[] dirtyRects = (java.awt.Rectangle[]) eventClass.getMethod("getDirtyRects").invoke(paintEvent);
                    boolean popup = (Boolean) eventClass.getMethod("getPopup").invoke(paintEvent);

                    log("🎨 Paint data extracted: " + eventWidth + "x" + eventHeight + ", popup=" + popup);

                    // ChromiumRenderHandlerのonPaint()を呼び出す
                    renderHandler.onPaint(browser, popup, dirtyRects, buffer, eventWidth, eventHeight);

                    log("🎨 renderHandler.onPaint() completed");
                } catch (Exception e) {
                    logError("onPaint listener error: " + e.getMessage());
                    e.printStackTrace();
                }
            };

            // addOnPaintListener()を呼び出す
            addListenerMethod.invoke(browser, paintListener);
            log("✅ Successfully registered onPaint listener via addOnPaintListener()");
        } catch (NoSuchMethodException e) {
            logError("addOnPaintListener() method not found on browser: " + browser.getClass().getName());
        } catch (Exception e) {
            logError("Failed to register onPaint listener: " + e.getMessage());
            e.printStackTrace(); // スタックトレースも出力
        }

        log("ChromiumBrowser instance created");
        log("Browser will load URL: " + url);

        // 重要: createBrowser()はブラウザインスタンスを作成するだけで、URLを自動的に読み込まない
        // 明示的にloadURL()を呼び出す必要がある
        if (url != null && !url.isEmpty()) {
            log("Calling loadURL() explicitly: " + url);
            browser.loadURL(url);
        }

        // CRITICAL: OSRモードでは、GLCanvasを実際にUIツリーに追加しないとonPaintが発火しない
        // CefBrowserOsr.onPaint()はGLContextがnullだと早期リターンする（Line 385-389）
        // GLContextを初期化するには、GLCanvasのreshape()イベントが発火する必要がある
        //
        // 公式サンプル（MainFrame.java:275）ではcontentPanel_.add(getBrowser().getUIComponent())している
        // MochiOSはProcessingベースでSwingを使用していないため、別のアプローチを取る：
        //
        // GraphicsConfigurationエラーを回避するため、SwingUtilities.invokeLater()で遅延実行：
        // 1. 空のJFrameを先に作成・表示（GraphicsConfiguration確定）
        // 2. その後GLCanvasを追加（エラー回避）
        try {
            java.awt.Component uiComponent = browser.getUIComponent();
            if (uiComponent != null) {
                log("UIComponent retrieved: " + uiComponent.getClass().getName());

                // SwingUtilities.invokeLater()で遅延実行（AWTイベントスレッドで実行）
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            log("Creating hidden JFrame on AWT Event Thread...");

                            // 隠しJFrameを作成（軽量、装飾なし）
                            hiddenFrame = new javax.swing.JFrame();
                            hiddenFrame.setUndecorated(true);
                            hiddenFrame.setType(javax.swing.JFrame.Type.UTILITY);
                            hiddenFrame.setDefaultCloseOperation(javax.swing.JFrame.DO_NOTHING_ON_CLOSE);
                            hiddenFrame.setSize(width, height);
                            hiddenFrame.setLocation(-10000, -10000);

                            // 先にJFrameを表示（GraphicsConfiguration確定）
                            hiddenFrame.setVisible(true);

                            log("Hidden JFrame created and visible");

                            // 少し待機してからGLCanvasを追加（GraphicsConfiguration確定後）
                            try {
                                Thread.sleep(100);  // 100ms待機
                            } catch (InterruptedException e) {
                                // ignore
                            }

                            // GLCanvasをJPanelに追加
                            javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.BorderLayout());
                            panel.add(uiComponent, java.awt.BorderLayout.CENTER);
                            hiddenFrame.setContentPane(panel);

                            // 再度validate()を呼び出してUIを更新
                            hiddenFrame.validate();

                            // GLCanvasのサイズを明示的に設定
                            uiComponent.setSize(width, height);

                            log("✅ GLCanvas added to hidden JFrame - GLContext should be initialized");
                        } catch (Exception e) {
                            logError("Failed to setup hidden JFrame (invokeLater): " + e.getMessage());
                            // GraphicsConfigurationエラーが発生しても、レンダリングを試みる
                            // wasResized()を呼び出してブラウザにサイズを通知
                            tryTriggerRendering();
                        }
                    }
                });
            } else {
                logError("getUIComponent() returned null");
            }
        } catch (Exception e) {
            logError("Failed to setup hidden JFrame: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * URLを読み込む。
     *
     * @param url URL
     */
    public void loadURL(String url) {
        if (browser != null) {
            log("Loading URL: " + url);
            currentUrl = url;
            browser.loadURL(url);
        }
    }

    /**
     * HTMLコンテンツを読み込む。
     *
     * @param html HTMLコンテンツ
     */
    public void loadContent(String html) {
        loadContent(html, "about:blank");
    }

    /**
     * HTMLコンテンツを読み込む（ベースURL指定）。
     *
     * @param html HTMLコンテンツ
     * @param baseUrl ベースURL
     */
    public void loadContent(String html, String baseUrl) {
        if (browser != null) {
            log("Loading HTML content (base: " + baseUrl + ")");
            currentUrl = baseUrl;

            // TODO: loadString()メソッドがjcefmaven 122.1.10で利用できない
            // 代替: data URLを使用
            String dataUrl = "data:text/html;charset=utf-8," + html;
            browser.loadURL(dataUrl);
        }
    }

    /**
     * JavaScriptを実行する。
     *
     * @param script JavaScriptコード
     */
    public void executeScript(String script) {
        if (browser != null) {
            browser.executeJavaScript(script, browser.getURL(), 0);
        }
    }

    /**
     * 現在のURLを取得する。
     *
     * @return 現在のURL
     */
    public String getCurrentURL() {
        return currentUrl;
    }

    /**
     * ローディング中かを確認する。
     *
     * @return ローディング中の場合true
     */
    public boolean isLoading() {
        return isLoading;
    }

    /**
     * 戻る。
     */
    public void goBack() {
        if (browser != null && browser.canGoBack()) {
            browser.goBack();
        }
    }

    /**
     * 進む。
     */
    public void goForward() {
        if (browser != null && browser.canGoForward()) {
            browser.goForward();
        }
    }

    /**
     * 戻れるかを確認する。
     *
     * @return 戻れる場合true
     */
    public boolean canGoBack() {
        return browser != null && browser.canGoBack();
    }

    /**
     * 進めるかを確認する。
     *
     * @return 進める場合true
     */
    public boolean canGoForward() {
        return browser != null && browser.canGoForward();
    }

    /**
     * ページを再読み込みする。
     */
    public void reload() {
        if (browser != null) {
            browser.reload();
        }
    }

    /**
     * 読み込みを停止する。
     */
    public void stopLoad() {
        if (browser != null) {
            browser.stopLoad();
        }
    }

    /**
     * マウス押下イベントを送信する。
     *
     * @param x X座標
     * @param y Y座標
     * @param button マウスボタン（1=左、2=中、3=右）
     */
    public void sendMousePressed(int x, int y, int button) {
        // TODO: sendMouseEvent()メソッドがjcefmaven 122.1.10で利用できない
        // JCEFバージョンアップグレード後に実装
        log("Mouse pressed: " + x + "," + y + " button=" + button + " (not implemented)");
    }

    /**
     * マウス離しイベントを送信する。
     *
     * @param x X座標
     * @param y Y座標
     * @param button マウスボタン（1=左、2=中、3=右）
     */
    public void sendMouseReleased(int x, int y, int button) {
        // TODO: sendMouseEvent()メソッドがjcefmaven 122.1.10で利用できない
        log("Mouse released: " + x + "," + y + " button=" + button + " (not implemented)");
    }

    /**
     * マウス移動イベントを送信する。
     *
     * @param x X座標
     * @param y Y座標
     */
    public void sendMouseMoved(int x, int y) {
        // TODO: sendMouseEvent()メソッドがjcefmaven 122.1.10で利用できない
        log("Mouse moved: " + x + "," + y + " (not implemented)");
    }

    /**
     * マウスホイールイベントを送信する。
     *
     * @param x X座標
     * @param y Y座標
     * @param delta スクロール量（正=下、負=上）
     */
    public void sendMouseWheel(int x, int y, float delta) {
        // TODO: sendMouseWheelEvent()メソッドがjcefmaven 122.1.10で利用できない
        log("Mouse wheel: " + x + "," + y + " delta=" + delta + " (not implemented)");
    }

    /**
     * キー押下イベントを送信する。
     *
     * @param keyCode キーコード
     * @param keyChar 文字
     */
    public void sendKeyPressed(int keyCode, char keyChar) {
        // TODO: sendKeyEvent()メソッドがjcefmaven 122.1.10で利用できない
        log("Key pressed: code=" + keyCode + " char=" + keyChar + " (not implemented)");
    }

    /**
     * キー離しイベントを送信する。
     *
     * @param keyCode キーコード
     * @param keyChar 文字
     */
    public void sendKeyReleased(int keyCode, char keyChar) {
        // TODO: sendKeyEvent()メソッドがjcefmaven 122.1.10で利用できない
        log("Key released: code=" + keyCode + " char=" + keyChar + " (not implemented)");
    }

    /**
     * PGraphicsに描画する。
     *
     * @param pg PGraphics
     */
    public void drawToPGraphics(PGraphics pg) {
        if (renderHandler != null) {
            PImage img = renderHandler.getImage();
            if (img != null) {
                pg.image(img, 0, 0);
            }
        }
    }

    /**
     * 描画更新が必要かを確認する。
     *
     * @return 更新が必要な場合true
     */
    public boolean needsUpdate() {
        return renderHandler != null && renderHandler.needsUpdate();
    }

    /**
     * ブラウザを破棄する。
     */
    public void dispose() {
        log("Disposing ChromiumBrowser");

        // 隠しJFrameを破棄
        if (hiddenFrame != null) {
            hiddenFrame.setVisible(false);
            hiddenFrame.dispose();
            hiddenFrame = null;
            log("Hidden JFrame disposed");
        }

        if (browser != null) {
            browser.close(true);
        }
        if (client != null) {
            client.dispose();
        }
    }

    /**
     * レンダリングをトリガーする（GraphicsConfigurationエラー後のフォールバック）。
     * wasResized()を呼び出してブラウザにサイズを通知し、レンダリングを開始させる。
     */
    private void tryTriggerRendering() {
        if (browser == null) {
            return;
        }

        try {
            log("Attempting to trigger rendering via wasResized()...");

            // Reflectionでwas Resized()メソッドを呼び出し
            java.lang.reflect.Method wasResizedMethod = browser.getClass().getMethod("wasResized", int.class, int.class);
            wasResizedMethod.setAccessible(true);
            wasResizedMethod.invoke(browser, width, height);

            log("✅ wasResized() called successfully - rendering may start");
        } catch (NoSuchMethodException e) {
            logError("wasResized(int, int) method not found on browser");
        } catch (Exception e) {
            logError("Failed to call wasResized(): " + e.getMessage());
        }
    }

    /**
     * ログ出力（INFO）。
     */
    private void log(String message) {
        if (kernel.getLogger() != null) {
            kernel.getLogger().debug("ChromiumBrowser", message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        if (kernel.getLogger() != null) {
            kernel.getLogger().error("ChromiumBrowser", message);
        }
    }
}
