package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefDisplayHandlerAdapter;
import org.cef.handler.CefFocusHandlerAdapter;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefRequestContextHandlerAdapter;
import org.cef.handler.CefWindowHandlerAdapter;
import processing.core.PGraphics;
import processing.core.PImage;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

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
    private final ChromiumProvider provider;
    private final CefClient client;
    private final CefBrowser browser;
    private final ChromiumRenderHandler renderHandler;
    private final int width;
    private final int height;

    private String currentUrl = "";
    private boolean isLoading = false;
    private String currentTitle = "";
    private volatile boolean webPageClicked = false; // Webページがクリックされたか（テキスト入力可能性）

    private final ConcurrentLinkedQueue<InputEvent> inputQueue = new ConcurrentLinkedQueue<>();
    private long lastInputLogNs = System.nanoTime();

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
    public ChromiumBrowser(Kernel kernel, CefApp cefApp, ChromiumProvider provider, String url, int width, int height) {
        this.kernel = kernel;
        this.provider = provider;
        this.width = width;
        this.height = height;
        this.currentUrl = url;

        // CefClientを作成（CefApp.createClient()を使用）
        this.client = cefApp.createClient();
        client.addDisplayHandler(new CefDisplayHandlerAdapter() {
            @Override
            public void onTitleChange(CefBrowser browser, String title) {
                currentTitle = title;
            }

            @Override
            public boolean onConsoleMessage(CefBrowser browser, org.cef.CefSettings.LogSeverity level,
                    String message, String source, int line) {
                // JavaScriptからのテキスト入力フォーカス通知を受信
                if (message != null && message.startsWith("[MochiOS:TextFocus]")) {
                    String focusState = message.substring("[MochiOS:TextFocus]".length());
                    webPageClicked = "true".equals(focusState);
                    log("Text input focus changed: " + webPageClicked);
                    return true; // メッセージを処理したのでtrueを返す
                }
                return false; // 他のコンソールメッセージは通常通り処理
            }
        });

        if (provider == null) {
            logError("CRITICAL: ChromiumProvider is null!");
            throw new RuntimeException("ChromiumProvider is not set");
        }

        // プロバイダー経由でブラウザを作成（環境ごとのAPI差異を吸収）
        this.browser = provider.createBrowser(client, url, true, false);

        // ブラウザインスタンスの状態を確認
        if (browser == null) {
            logError("CRITICAL: createBrowser() returned null!");
            this.renderHandler = null;
            return;
        }

        // ChromiumRenderHandlerを作成
        // MCEFBrowser（Forge）の場合は、プロバイダー経由でアダプターを取得
        // jcefmaven（Standalone）の場合は、通常のChromiumRenderHandlerを使用
        ChromiumRenderHandler customRenderHandler = provider.createRenderHandler(kernel, browser, width, height);
        if (customRenderHandler != null) {
            this.renderHandler = customRenderHandler;
        } else {
            this.renderHandler = new ChromiumRenderHandler(kernel, width, height);
        }
        currentTitle = url == null ? "" : url;

        // ロードハンドラーを追加（ローディング状態管理）
        client.addLoadHandler(new CefLoadHandlerAdapter() {
            @Override
            public void onLoadStart(CefBrowser browser, CefFrame frame, org.cef.network.CefRequest.TransitionType transitionType) {
                if (frame.isMain()) {
                    isLoading = true;
                    currentUrl = frame.getURL();
                    currentTitle = currentUrl;
                    // 新しいページをロードする時、Webページクリック状態をリセット
                    webPageClicked = false;
                }
            }

            @Override
            public void onLoadEnd(CefBrowser browser, CefFrame frame, int httpStatusCode) {
                if (frame.isMain()) {
                    isLoading = false;
                    currentUrl = frame.getURL();
                    if (currentTitle == null || currentTitle.isBlank()) {
                        currentTitle = currentUrl;
                    }

                    // ページロード完了時にテキスト入力フォーカス監視スクリプトを注入
                    injectFocusDetectionScript(browser);
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

        // OSRモードでは、createImmediately()を呼び出す必要がある
        // これにより、ブラウザが即座に作成・初期化される
        if (browser != null) {
            try {
                browser.createImmediately();
            } catch (Exception e) {
                logError("createImmediately() failed: " + e.getMessage());
            }

            // setWindowlessFrameRateはjava-cef master（jcefmaven 135.0.20+）でのみサポート
            // MCEFの古いjava-cefにはこのメソッドが存在しないため、リフレクションで実行時チェック
            if (!provider.getName().equals("MCEF")) {
                try {
                    java.lang.reflect.Method setFrameRateMethod = browser.getClass().getMethod("setWindowlessFrameRate", int.class);
                    setFrameRateMethod.invoke(browser, 60);
                    log("Windowless frame rate set to 60 FPS");
                } catch (NoSuchMethodException e) {
                    log("setWindowlessFrameRate() not available in this JCEF version");
                } catch (Exception e) {
                    logError("Failed to set windowless frame rate: " + e.getMessage());
                }
            } else {
                log("Skipping setWindowlessFrameRate() for MCEF (method not available)");
            }
        }

        // ブラウザインスタンスの状態を確認
        if (browser == null) {
            logError("CRITICAL: createBrowser() returned null!");
            return;
        }


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
                    // CefPaintEventからデータを取得（リフレクション使用）
                    Class<?> eventClass = paintEvent.getClass();
                    java.nio.ByteBuffer buffer = (java.nio.ByteBuffer) eventClass.getMethod("getRenderedFrame").invoke(paintEvent);
                    int eventWidth = (Integer) eventClass.getMethod("getWidth").invoke(paintEvent);
                    int eventHeight = (Integer) eventClass.getMethod("getHeight").invoke(paintEvent);
                    java.awt.Rectangle[] dirtyRects = (java.awt.Rectangle[]) eventClass.getMethod("getDirtyRects").invoke(paintEvent);
                    boolean popup = (Boolean) eventClass.getMethod("getPopup").invoke(paintEvent);

                    // ChromiumRenderHandlerのonPaint()を呼び出す
                    renderHandler.onPaint(browser, popup, dirtyRects, buffer, eventWidth, eventHeight);
                } catch (Exception e) {
                    logError("onPaint listener error: " + e.getMessage());
                    e.printStackTrace();
                }
            };

            // addOnPaintListener()を呼び出す
            addListenerMethod.invoke(browser, paintListener);
        } catch (NoSuchMethodException e) {
            logError("addOnPaintListener() method not found on browser: " + browser.getClass().getName());
        } catch (Exception e) {
            logError("Failed to register onPaint listener: " + e.getMessage());
            e.printStackTrace(); // スタックトレースも出力
        }

        // 重要: createBrowser()はブラウザインスタンスを作成するだけで、URLを自動的に読み込まない
        // 明示的にloadURL()を呼び出す必要がある
        if (url != null && !url.isEmpty()) {
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
        //
        // 注: MCEFはgetUIComponent()をサポートしていない（LWJGLベース）
        // jcefmavenのみがこのメソッドをサポートする（JOGLベース）
        if (provider != null && provider.supportsUIComponent()) {
            try {
                java.awt.Component uiComponent = browser.getUIComponent();
                if (uiComponent != null) {
                // SwingUtilities.invokeLater()で遅延実行（AWTイベントスレッドで実行）
                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // 隠しJFrameを作成（オフスクリーンレンダリング用）
                            hiddenFrame = new javax.swing.JFrame();
                            hiddenFrame.setUndecorated(true);
                            hiddenFrame.setType(javax.swing.JFrame.Type.UTILITY);
                            hiddenFrame.setDefaultCloseOperation(javax.swing.JFrame.DO_NOTHING_ON_CLOSE);
                            hiddenFrame.setSize(width, height);

                            // 画面外に配置（レンダリング専用のため）
                            hiddenFrame.setLocation(-10000, -10000);

                            // フォーカス取得を防ぐ（ProcessingウィンドウでIMEを使用するため）
                            hiddenFrame.setFocusableWindowState(false);
                            hiddenFrame.setAutoRequestFocus(false);

                            // 先にJFrameを表示（GraphicsConfiguration確定）
                            hiddenFrame.setVisible(true);

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

                            // GLCanvasでIMEを無効化（ProcessingウィンドウでIMEを使用するため）
                            uiComponent.enableInputMethods(false);
                            uiComponent.setFocusable(false);

                            // 再度validate()を呼び出してUIを更新
                            hiddenFrame.validate();

                            // GLCanvasのサイズを明示的に設定
                            uiComponent.setSize(width, height);

                            log("Hidden JFrame configured: focusable=false, IME disabled on GLCanvas");

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
     * テキスト入力フォーカス検出スクリプトを注入する。
     * Webページ内のテキスト入力フィールドのfocus/blurイベントを監視し、
     * console.logでJava側に通知する。
     */
    private void injectFocusDetectionScript(CefBrowser browser) {
        String script =
            "(function() {" +
            "  let mochiOS_hasTextFocus = false;" +
            "  function updateFocusState() {" +
            "    const el = document.activeElement;" +
            "    const isTextInput = el && (" +
            "      el.tagName === 'INPUT' && ['text', 'password', 'email', 'search', 'tel', 'url', 'number'].includes(el.type) ||" +
            "      el.tagName === 'TEXTAREA' ||" +
            "      el.isContentEditable" +
            "    );" +
            "    if (isTextInput !== mochiOS_hasTextFocus) {" +
            "      mochiOS_hasTextFocus = isTextInput;" +
            "      console.log('[MochiOS:TextFocus]' + isTextInput);" +
            "    }" +
            "  }" +
            "  document.addEventListener('focusin', updateFocusState, true);" +
            "  document.addEventListener('focusout', updateFocusState, true);" +
            "  updateFocusState();" +
            "})();";

        browser.executeJavaScript(script, browser.getURL(), 0);
        log("Text input focus detection script injected");
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
     * 現在のページタイトルを取得する。
     *
     * @return タイトル、取得できない場合はnull
     */
    public String getTitle() {
        return currentTitle;
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
     * Webページがクリックされたかを取得する。
     * Webページ内をクリックした場合にtrueを返す。
     * テキスト入力フィールドへのフォーカスの可能性を示す。
     * スペースキーのホームボタン判定に使用される。
     *
     * @return Webページがクリックされた場合true
     */
    public boolean hasTextInputFocus() {
        return webPageClicked;
    }

    /**
     * Webページクリック状態をリセットする。
     * 新しいページロード時に呼び出される。
     */
    public void resetWebPageClickState() {
        webPageClicked = false;
    }

    /**
     * ウィンドウレス描画フレームレートを設定する。
     *
     * @param frameRate フレームレート（1-60）
     */
    public void setFrameRate(int frameRate) {
        if (browser != null && !provider.getName().equals("MCEF")) {
            try {
                int fps = Math.max(1, Math.min(60, frameRate));
                java.lang.reflect.Method setFrameRateMethod = browser.getClass().getMethod("setWindowlessFrameRate", int.class);
                setFrameRateMethod.invoke(browser, fps);
            } catch (NoSuchMethodException e) {
                log("setWindowlessFrameRate() not available in this JCEF version");
            } catch (Exception e) {
                logError("Failed to set frame rate: " + e.getMessage());
            }
        }
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
        enqueueInput(InputEvent.mousePress(x, y, button));
    }

    /**
     * マウス離しイベントを送信する。
     *
     * @param x X座標
     * @param y Y座標
     * @param button マウスボタン（1=左、2=中、3=右）
     */
    public void sendMouseReleased(int x, int y, int button) {
        enqueueInput(InputEvent.mouseRelease(x, y, button));
    }

    /**
     * マウス移動イベントを送信する。
     *
     * @param x X座標
     * @param y Y座標
     */
    public void sendMouseMoved(int x, int y) {
        enqueueInput(InputEvent.mouseMove(x, y));
    }

    /**
     * マウスホイールイベントを送信する。
     *
     * @param x X座標
     * @param y Y座標
     * @param delta スクロール量（正=下、負=上）
     */
    public void sendMouseWheel(int x, int y, float delta) {
        enqueueInput(InputEvent.mouseWheel(x, y, delta));
    }

    /**
     * キー押下イベントを送信する。
     *
     * @param keyCode キーコード
     * @param keyChar 文字
     */
    public void sendKeyPressed(int keyCode, char keyChar) {
        enqueueInput(InputEvent.keyPress(keyCode, keyChar));
    }

    /**
     * キー離しイベントを送信する。
     *
     * @param keyCode キーコード
     * @param keyChar 文字
     */
    public void sendKeyReleased(int keyCode, char keyChar) {
        enqueueInput(InputEvent.keyRelease(keyCode, keyChar));
    }

    public void flushInputEvents() {
        if (browser == null) {
            inputQueue.clear();
            return;
        }

        if (provider == null) {
            inputQueue.clear();
            logError("ChromiumProvider is null, cannot dispatch input events");
            return;
        }

        // 時間制限なしで可能な限り処理
        // draw()は60FPSで呼ばれるため、イベント処理を優先
        long startNs = System.nanoTime();

        int processed = 0;
        int mouseMoveProcessed = 0;
        int maxEventsPerFrame = 200; // P2D GPU描画により高速処理可能
        int maxMouseMovePerFrame = 150; // マウス移動も大幅増加（リアルタイム性重視）

        InputEvent event;
        while ((event = inputQueue.poll()) != null) {
            boolean isMouseMove = (event.type == InputEvent.Type.MOUSE_MOVE);

            // マウス移動イベントの制限チェック
            if (isMouseMove && mouseMoveProcessed >= maxMouseMovePerFrame) {
                // マウス移動の処理数上限に達した場合、スキップ（破棄）
                continue;
            }

            long latencyNs = System.nanoTime() - event.enqueueTimeNs;
            event.dispatch(provider, browser, kernel, this);
            long processedNs = System.nanoTime();
            logInputLatency(event, processedNs, inputQueue.size());

            if (isMouseMove) {
                mouseMoveProcessed++;
            }
            processed++;

            if (latencyNs > 50_000_000L && kernel.getLogger() != null) { // >50ms
                kernel.getLogger().debug("ChromiumBrowser", String.format(
                        "InputEvent latency %.3fms type=%s backlog=%d",
                        latencyNs / 1_000_000.0, event.type, inputQueue.size()));
            }

            // イベント数制限
            if (processed >= maxEventsPerFrame) {
                break;
            }
        }

        if (!inputQueue.isEmpty()) {
            long now = System.nanoTime();
            if (now - lastInputLogNs > 500_000_000L) {
                log("Input queue backlog: " + inputQueue.size() + " events");
                lastInputLogNs = now;
            }
        }
    }

    private void enqueueInput(InputEvent event) {
        if (browser == null || event == null) {
            return;
        }

        event.markCaptured();
        event.enqueueTimeNs = System.nanoTime();

        if (provider == null) {
            inputQueue.offer(event);
            return;
        }

        // 全てのイベントをキューに入れて非同期処理
        // （同期処理によるUIブロックを回避）

        // 適度なキューサイズ制限: P2D GPU描画により高速処理可能
        int queueSize = inputQueue.size();

        // マウス移動イベント：キューが200以上なら新規追加を完全拒否（緩和）
        if (event.type == InputEvent.Type.MOUSE_MOVE) {
            if (queueSize >= 200) {
                return; // 追加しない（ログも出さない）
            }
        }

        // 重要なイベント（クリック、キー入力）：キューが500以上なら拒否（緩和）
        if (queueSize >= 500) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("ChromiumBrowser",
                    "Emergency: Rejecting event type=" + event.type + " (queue size: " + queueSize + ")");
            }
            return; // 追加しない
        }

        inputQueue.offer(event);
    }

    private void logInputLatency(InputEvent event, long processedNs, int backlogSize) {
        if (event.captureTimeNs == 0L) {
            return;
        }
        double latencyMs = (processedNs - event.captureTimeNs) / 1_000_000.0;
        // ログI/O負荷軽減: 50ms以上のレイテンシのみ記録
        // （YouTube動画再生時のフレームレート維持）
        if (latencyMs < 50.0) {
            return;
        }
        long processedWallClockMs = System.currentTimeMillis();
        String message = String.format(
                "type=%s latency=%.3fms captured=%tT.%03d processed=%tT.%03d backlog=%d",
                event.type,
                latencyMs,
                event.captureWallClockMs, (int) (event.captureWallClockMs % 1000),
                processedWallClockMs, (int) (processedWallClockMs % 1000),
                backlogSize);
        if (kernel.getLogger() == null) {
            return;
        }
        kernel.getLogger().warn("ChromiumInput", message);
    }

    /**
     * PGraphicsに描画する。
     *
     * @param pg PGraphics
     */
    public void drawToPGraphics(PGraphics pg) {
        PImage img = getUpdatedImage();
        if (img != null) {
            pg.image(img, 0, 0);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void resize(int newWidth, int newHeight) {
        if (browser == null) {
            return;
        }
        try {
            java.lang.reflect.Method resizeMethod = browser.getClass().getMethod("resize", int.class, int.class);
            resizeMethod.setAccessible(true);
            resizeMethod.invoke(browser, newWidth, newHeight);
        } catch (NoSuchMethodException e) {
            try {
                java.lang.reflect.Method wasResizedMethod = browser.getClass().getMethod("wasResized", int.class, int.class);
                wasResizedMethod.setAccessible(true);
                wasResizedMethod.invoke(browser, newWidth, newHeight);
            } catch (Exception ex) {
                logError("Failed to resize browser via wasResized: " + ex.getMessage());
            }
        } catch (Exception e) {
            logError("Failed to resize browser: " + e.getMessage());
        }
    }

    /**
     * レンダリング結果を更新して最新のPImageを取得する。
     *
     * @return 最新のPImage、取得できない場合はnull
     */
    public PImage getUpdatedImage() {
        if (renderHandler == null) {
            return null;
        }

        // MCEFRenderHandlerAdapterの場合はOpenGLテクスチャを読み出す
        if (renderHandler.getClass().getName().contains("MCEFRenderHandlerAdapter")) {
            try {
                java.lang.reflect.Method updateMethod = renderHandler.getClass().getMethod("updateFromTexture");
                updateMethod.invoke(renderHandler);
            } catch (Exception e) {
                // Silent failure (毎フレーム呼ばれるためログ出力しない)
            }
        }

        return renderHandler.getImage();
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
     * レンダリングハンドラーを取得する。
     *
     * @return ChromiumRenderHandler
     */
    public ChromiumRenderHandler getRenderHandler() {
        return renderHandler;
    }

    /**
     * ブラウザを破棄する。
     */
    public void dispose() {
        log("Disposing ChromiumBrowser");

        inputQueue.clear();

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
            try {
                java.lang.reflect.Method resizeMethod = browser.getClass().getMethod("resize", int.class, int.class);
                resizeMethod.setAccessible(true);
                resizeMethod.invoke(browser, width, height);
                log("✅ resize() called successfully - rendering may start");
                return;
            } catch (NoSuchMethodException e) {
                log("resize(int, int) method not found, falling back to wasResized()");
            }

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

    private static final class InputEvent {
        private enum Type {
            MOUSE_PRESS,
            MOUSE_RELEASE,
            MOUSE_MOVE,
            MOUSE_WHEEL,
            KEY_PRESS,
            KEY_RELEASE
        }

        private final Type type;
        private final int x;
        private final int y;
        private final float wheelDelta;
        private final int button;
        private final int keyCode;
        private final char keyChar;
        private long enqueueTimeNs;
        private long captureTimeNs;
        private long captureWallClockMs;

        private InputEvent(Type type, int x, int y, float wheelDelta, int button, int keyCode, char keyChar) {
            this.type = type;
            this.x = x;
            this.y = y;
            this.wheelDelta = wheelDelta;
            this.button = button;
            this.keyCode = keyCode;
            this.keyChar = keyChar;
        }

        static InputEvent mousePress(int x, int y, int button) {
            return new InputEvent(Type.MOUSE_PRESS, x, y, 0f, button, 0, (char) 0);
        }

        static InputEvent mouseRelease(int x, int y, int button) {
            return new InputEvent(Type.MOUSE_RELEASE, x, y, 0f, button, 0, (char) 0);
        }

        static InputEvent mouseMove(int x, int y) {
            return new InputEvent(Type.MOUSE_MOVE, x, y, 0f, 0, 0, (char) 0);
        }

        static InputEvent mouseWheel(int x, int y, float delta) {
            return new InputEvent(Type.MOUSE_WHEEL, x, y, delta, 0, 0, (char) 0);
        }

        static InputEvent keyPress(int keyCode, char keyChar) {
            return new InputEvent(Type.KEY_PRESS, 0, 0, 0f, 0, keyCode, keyChar);
        }

        static InputEvent keyRelease(int keyCode, char keyChar) {
            return new InputEvent(Type.KEY_RELEASE, 0, 0, 0f, 0, keyCode, keyChar);
        }

        void markCaptured() {
            if (captureTimeNs == 0L) {
                captureTimeNs = System.nanoTime();
                captureWallClockMs = System.currentTimeMillis();
            }
        }

        void dispatch(ChromiumProvider provider, CefBrowser browser, Kernel kernel, ChromiumBrowser chromiumBrowser) {
            switch (type) {
                case MOUSE_PRESS:
                    provider.sendMousePressed(browser, x, y, button);
                    break;
                case MOUSE_RELEASE:
                    provider.sendMouseReleased(browser, x, y, button);
                    break;
                case MOUSE_MOVE:
                    provider.sendMouseMoved(browser, x, y);
                    break;
                case MOUSE_WHEEL:
                    provider.sendMouseWheel(browser, x, y, wheelDelta);
                    break;
                case KEY_PRESS:
                    boolean ctrlPressed = kernel != null && kernel.isCtrlPressed();
                    boolean shiftPressed = kernel != null && kernel.isShiftPressed();
                    provider.sendKeyPressed(browser, keyCode, keyChar, ctrlPressed, shiftPressed);
                    break;
                case KEY_RELEASE:
                    boolean ctrlReleased = kernel != null && kernel.isCtrlPressed();
                    boolean shiftReleased = kernel != null && kernel.isShiftPressed();
                    provider.sendKeyReleased(browser, keyCode, keyChar, ctrlReleased, shiftReleased);
                    break;
            }
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
