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
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.handler.CefResourceHandler;
import org.cef.handler.CefResourceRequestHandler;
import org.cef.handler.CefResourceRequestHandlerAdapter;
import org.cef.handler.CefWindowHandlerAdapter;
import org.cef.misc.BoolRef;
import org.cef.network.CefRequest;
import jp.moyashi.phoneos.core.service.chromium.interceptor.VirtualNetworkResourceHandler;
import processing.core.PGraphics;
import processing.core.PImage;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
    private volatile int width;
    private volatile int height;

    private String currentUrl = "";
    private String displayUrl = null; // IPvM URL用の表示URL（data:URLの代わりに表示）
    private boolean isLoading = false;
    private String currentTitle = "";
    private volatile boolean webPageClicked = false; // Webページがクリックされたか（テキスト入力可能性）
    private volatile String cachedSelectedText = ""; // 選択テキストキャッシュ（TextInputProtocol用）

    private final ConcurrentLinkedQueue<InputEvent> inputQueue = new ConcurrentLinkedQueue<>();
    private long lastInputLogNs = System.nanoTime();
    
    public interface LoadListener {
        void onLoadStart(String url);
        void onLoadEnd(String url, String title, int httpStatusCode);
    }
    
    private final List<LoadListener> loadListeners = new ArrayList<>();

    // 隠しJFrame（ChromiumのOSRレンダリングをトリガーするために必要）
    private javax.swing.JFrame hiddenFrame;

    // GLContext初期化同期用
    private final CountDownLatch glInitLatch = new CountDownLatch(1);
    private volatile boolean glContextReady = false;

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
                // MochiOS関連のコンソールメッセージをログ出力
                if (message != null && message.startsWith("[MochiOS:")) {
                    log("Console message received: " + message);
                }

                // JavaScriptからのテキスト入力フォーカス通知を受信
                if (message != null && message.startsWith("[MochiOS:TextFocus]")) {
                    String focusState = message.substring("[MochiOS:TextFocus]".length());
                    webPageClicked = "true".equals(focusState);
                    log("Text input focus changed: " + webPageClicked);
                    return true; // メッセージを処理したのでtrueを返す
                }
                // JavaScriptからの選択テキスト通知を受信
                if (message != null && message.startsWith("[MochiOS:Selection]")) {
                    cachedSelectedText = message.substring("[MochiOS:Selection]".length());
                    return true; // メッセージを処理したのでtrueを返す
                }
                return false; // 他のコンソールメッセージは通常通り処理
            }
        });

        // Intent URL と IPvM URL を処理するリクエストハンドラーを追加
        client.addRequestHandler(new CefRequestHandlerAdapter() {
            @Override
            public boolean onBeforeBrowse(CefBrowser browser, CefFrame frame, CefRequest request,
                                          boolean user_gesture, boolean is_redirect) {
                String url = request.getURL();
                log("onBeforeBrowse() called for: " + url);

                // Intent URL処理
                if (url != null && url.startsWith("intent://")) {
                    String convertedUrl = convertIntentUrl(url);
                    if (!url.equals(convertedUrl)) {
                        log("Intent URL detected, redirecting to: " + convertedUrl);
                        browser.loadURL(convertedUrl);
                        return true;
                    }
                }

                // IPvM URL処理 - data: URLを使用してHTML表示
                if (url != null && isIPvMUrl(url)) {
                    log("IPvM URL detected in onBeforeBrowse: " + url);
                    // http://3-sys-test/ → httpm://3-sys-test/ に変換（表示用URL）
                    String displayUrl = url.replaceFirst("^https?://", "httpm://");
                    // HTMLを非同期で取得してdata: URLで表示
                    loadIPvMContent(browser, url, displayUrl);
                    return true; // 元のリクエストをキャンセル
                }

                // httpm:// URL処理 - CefResourceHandlerはOSRでレンダリングされないため、ここで処理
                if (url != null && url.startsWith("httpm://")) {
                    log("httpm URL detected in onBeforeBrowse: " + url);
                    // httpm://3-sys-test/ → http://3-sys-test/ に変換（VirtualAdapter用）
                    String httpUrl = url.replaceFirst("^httpm://", "http://");
                    // HTMLを非同期で取得してdata: URLで表示（displayUrlはhttpm://のまま）
                    loadIPvMContent(browser, httpUrl, url);
                    return true; // 元のリクエストをキャンセル
                }

                return false; // 通常のナビゲーションを続行
            }

            @Override
            public CefResourceRequestHandler getResourceRequestHandler(
                    CefBrowser browser, CefFrame frame, CefRequest request,
                    boolean isNavigation, boolean isDownload, String requestInitiator,
                    BoolRef disableDefaultHandling) {
                // IPvMはonBeforeBrowseで処理するため、ここでは何もしない
                return null;
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

        // Forge環境（MCEF）用：コンソールメッセージリスナーを追加
        // MCEFClientはCefClientをラップするため、元のCefDisplayHandlerが動作しない
        // provider経由でMCEFClientにDisplayHandlerを追加する
        provider.addConsoleMessageListener(browser, (message, source, line) -> {
            // MochiOS関連のコンソールメッセージをログ出力
            if (message != null && message.startsWith("[MochiOS:")) {
                log("Console message received (via provider): " + message);
            }

            // JavaScriptからのテキスト入力フォーカス通知を受信
            if (message != null && message.startsWith("[MochiOS:TextFocus]")) {
                String focusState = message.substring("[MochiOS:TextFocus]".length());
                webPageClicked = "true".equals(focusState);
                log("Text input focus changed (via provider): " + webPageClicked);
                return true;
            }
            // JavaScriptからの選択テキスト通知を受信
            if (message != null && message.startsWith("[MochiOS:Selection]")) {
                cachedSelectedText = message.substring("[MochiOS:Selection]".length());
                return true;
            }
            return false;
        });

        // ChromiumRenderHandlerを作成
        // MCEFBrowser（Forge）の場合は、プロバイダー経由でアダプターを取得
        // jcefmaven（Standalone）の場合は、通常のChromiumRenderHandlerを使用
        ChromiumRenderHandler customRenderHandler = provider.createRenderHandler(kernel, browser, width, height);
        if (customRenderHandler != null) {
            this.renderHandler = customRenderHandler;
        } else {
            this.renderHandler = new ChromiumRenderHandler(kernel, width, height);
        }

        // 重要: RenderHandlerをブラウザインスタンスに注入する
        // CefClient.addRenderHandler()が存在しないため、リフレクションを使用して
        // CefBrowserOsrのRenderHandlerフィールドに直接設定する。
        // フィールド名がバージョンによって異なる可能性があるため、型で探索する。
        // デバッグ: ブラウザオブジェクトの全メソッドとフィールドをダンプして
        // サイズ設定やRenderHandler登録の手がかりを探す
        // (解決済みのため削除)

        currentTitle = url == null ? "" : url;

        // ... (中略: loadHandler等の設定) ...
        
        // 重要: 初期サイズを強制適用する
        // CefBrowserOsrは初期サイズが1x1になっており、GLCanvasの初期化を待たずに
        // レンダリングが始まると1x1描画になってしまうため、ここで明示的にサイズを設定する。
        if (browser != null && width > 0 && height > 0) {
            resize(width, height);
            
            // 可視性を強制的に有効化（レンダリング開始のため）
            try {
                java.lang.reflect.Method setVisibilityMethod = getMethodRecursive(browser.getClass(), "setWindowVisibility", boolean.class);
                if (setVisibilityMethod != null) {
                    setVisibilityMethod.setAccessible(true);
                    setVisibilityMethod.invoke(browser, true);
                    log("Called setWindowVisibility(true)");
                }
                
                // 移動/リサイズ開始通知
                java.lang.reflect.Method notifyMethod = getMethodRecursive(browser.getClass(), "notifyMoveOrResizeStarted");
                if (notifyMethod != null) {
                    notifyMethod.setAccessible(true);
                    notifyMethod.invoke(browser);
                    log("Called notifyMoveOrResizeStarted()");
                }
            } catch (Exception e) {
                logError("Failed to set visibility/notify: " + e.getMessage());
            }
        }

        // OSRモードでは、createImmediately()を呼び出す必要がある

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
                    
                    for (LoadListener listener : loadListeners) {
                        listener.onLoadStart(currentUrl);
                    }
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

                    for (LoadListener listener : loadListeners) {
                        listener.onLoadEnd(currentUrl, currentTitle, httpStatusCode);
                    }

                    // CRITICAL: data: URLを使用する場合は強制再描画が不要
                    // loadString()を使用する修正後、forceRepaintAfterLoadは問題を引き起こす可能性がある
                    // （一瞬表示→白画面→再表示のフリッカー現象）
                    // forceRepaintAfterLoad(); // 無効化
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
            log("Successfully registered onPaint listener via reflection");
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
        log("Provider check: provider=" + (provider != null ? provider.getClass().getSimpleName() : "null") +
            ", supportsUIComponent=" + (provider != null ? provider.supportsUIComponent() : "N/A"));
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

                            // GLCanvasのdisplay()を呼び出してOpenGLコンテキストの初期化を強制
                            log("UIComponent class: " + uiComponent.getClass().getName());
                            try {
                                // リフレクションでdisplay()を呼び出し
                                java.lang.reflect.Method displayMethod = uiComponent.getClass().getMethod("display");
                                displayMethod.setAccessible(true);
                                log("Calling display() via reflection...");
                                displayMethod.invoke(uiComponent);
                                log("display() called successfully via reflection");
                            } catch (NoSuchMethodException nsme) {
                                log("display() method not found on " + uiComponent.getClass().getName());
                            } catch (Exception displayEx) {
                                logError("Failed to call display(): " + displayEx.getMessage());
                                displayEx.printStackTrace();
                            }

                            // GLContext初期化完了を通知
                            // GLCanvasがUIツリーに追加された後、少し待機してからreadyにする
                            try {
                                Thread.sleep(200);  // GLContext初期化を待機
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                            }
                            glContextReady = true;
                            glInitLatch.countDown();
                            log("GLContext initialization signaled (latch released)");

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
        } else {
            // supportsUIComponent() が false の場合（CefBrowserOsrNoCanvas 等）
            // hidden JFrame なしでレンダリングをトリガー
            log("Provider does not support UI component, triggering rendering directly...");

            // ブラウザにサイズを通知（onPaint を発火させるために必要）
            tryTriggerRendering();

            // GLContext は不要なので即座に準備完了とマーク
            glContextReady = true;
            glInitLatch.countDown();
            log("NoCanvas mode: GLContext ready (no initialization needed)");
        }
    }

    public void addLoadListener(LoadListener listener) {
        loadListeners.add(listener);
    }

    /**
     * URLを読み込む。
     * GLContext初期化を待機してから読み込みを開始する。
     *
     * @param url URL
     */
    public void loadURL(String url) {
        if (browser != null) {
            // GLContext初期化を待機（最大3秒）
            waitForGLContext(3000);
            log("Loading URL: " + url);
            currentUrl = url;
            // 通常URLの場合はdisplayUrlをクリア（data: URLは例外）
            if (url != null && !url.startsWith("data:")) {
                displayUrl = null;
            }
            browser.loadURL(url);
        }
    }

    /**
     * GLContext初期化を待機する。
     *
     * @param timeoutMs タイムアウト（ミリ秒）
     */
    private void waitForGLContext(long timeoutMs) {
        if (glContextReady) {
            return; // 既に準備完了
        }
        try {
            log("Waiting for GLContext initialization (max " + timeoutMs + "ms)...");
            boolean success = glInitLatch.await(timeoutMs, TimeUnit.MILLISECONDS);
            if (success) {
                log("GLContext initialization completed");
            } else {
                log("Warning: GLContext initialization timed out after " + timeoutMs + "ms");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log("GLContext wait interrupted");
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
     * data: URLを使用してHTMLを直接レンダリングする。
     * カスタムスキーム（mochiapp://等）でOSR再描画がトリガーされない問題を回避。
     *
     * 注意: data: URLを使用する場合、相対パス（CSSや画像）は解決されない。
     * HTML内でmochiapp://の絶対パスを使用するか、
     * <base href="mochiapp://modid/">タグを挿入する必要がある。
     *
     * @param html HTMLコンテンツ
     * @param baseUrl ベースURL（<base>タグ挿入に使用）
     */
    public void loadContent(String html, String baseUrl) {
        if (browser != null) {
            // GLContext初期化を待機（最大3秒）
            waitForGLContext(3000);
            log("Loading HTML content via data URL (base: " + baseUrl + ")");
            currentUrl = baseUrl;

            // HTMLに<base>タグを挿入して相対パスを解決可能にする
            // これによりCSS/JS/画像の相対パスがmochiapp://で正しく解決される
            String htmlWithBase = injectBaseTag(html, baseUrl);

            // data: URLを使用してHTMLを読み込む
            // エンコーディングの問題を避けるため、Base64エンコードを使用
            String base64Html = java.util.Base64.getEncoder().encodeToString(
                htmlWithBase.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            String dataUrl = "data:text/html;charset=utf-8;base64," + base64Html;
            browser.loadURL(dataUrl);
            log("Loaded HTML via data URL (" + html.length() + " chars -> " + base64Html.length() + " base64)");
        }
    }

    /**
     * HTMLに<base>タグを挿入する。
     * 既に<base>タグがある場合は置換する。
     *
     * @param html HTMLコンテンツ
     * @param baseUrl ベースURL
     * @return <base>タグが挿入されたHTML
     */
    private String injectBaseTag(String html, String baseUrl) {
        // ベースURLのディレクトリ部分を抽出
        String baseHref = baseUrl;
        int lastSlash = baseUrl.lastIndexOf('/');
        if (lastSlash > 0 && !baseUrl.endsWith("/")) {
            baseHref = baseUrl.substring(0, lastSlash + 1);
        }

        String baseTag = "<base href=\"" + baseHref + "\">";
        log("Injecting base tag: " + baseTag);

        // 既存の<base>タグを置換
        String htmlLower = html.toLowerCase();
        int existingBaseStart = htmlLower.indexOf("<base");
        if (existingBaseStart >= 0) {
            int existingBaseEnd = html.indexOf(">", existingBaseStart);
            if (existingBaseEnd > existingBaseStart) {
                return html.substring(0, existingBaseStart) + baseTag + html.substring(existingBaseEnd + 1);
            }
        }

        // <head>タグの直後に挿入
        int headEnd = htmlLower.indexOf("<head>");
        if (headEnd >= 0) {
            int insertPos = headEnd + "<head>".length();
            return html.substring(0, insertPos) + "\n" + baseTag + "\n" + html.substring(insertPos);
        }

        // <head>がない場合、<html>の直後に挿入
        int htmlEnd = htmlLower.indexOf("<html");
        if (htmlEnd >= 0) {
            int insertPos = html.indexOf(">", htmlEnd) + 1;
            return html.substring(0, insertPos) + "\n<head>\n" + baseTag + "\n</head>\n" + html.substring(insertPos);
        }

        // 両方ない場合、先頭に挿入
        return "<!DOCTYPE html>\n<html>\n<head>\n" + baseTag + "\n</head>\n<body>\n" + html + "\n</body>\n</html>";
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
     * また、選択テキスト変更も監視してJava側に通知する。
     */
    private void injectFocusDetectionScript(CefBrowser browser) {
        String script =
            "(function() {" +
            "  console.log('[MochiOS:Init] Focus detection script starting');" +
            "  let mochiOS_hasTextFocus = false;" +
            // Shadow DOM内の実際のactiveElementを再帰的に取得
            "  function getDeepActiveElement() {" +
            "    let el = document.activeElement;" +
            "    while (el && el.shadowRoot && el.shadowRoot.activeElement) {" +
            "      el = el.shadowRoot.activeElement;" +
            "    }" +
            "    return el;" +
            "  }" +
            // 要素がテキスト入力可能かチェック（親要素も確認）
            "  function isTextInputElement(el) {" +
            "    if (!el) return false;" +
            // 標準的なテキスト入力要素
            "    if (el.tagName === 'INPUT' && ['text', 'password', 'email', 'search', 'tel', 'url', 'number', ''].includes(el.type || 'text')) return true;" +
            "    if (el.tagName === 'TEXTAREA') return true;" +
            // contenteditable属性（自身または親要素）
            "    if (el.isContentEditable) return true;" +
            "    if (el.getAttribute && el.getAttribute('contenteditable') === 'true') return true;" +
            // role属性でテキスト入力を示すもの（YouTube等で使用）
            "    var role = el.getAttribute && el.getAttribute('role');" +
            "    if (role === 'textbox' || role === 'searchbox' || role === 'combobox') return true;" +
            // 親要素がcontenteditable
            "    var parent = el.parentElement;" +
            "    while (parent) {" +
            "      if (parent.isContentEditable || (parent.getAttribute && parent.getAttribute('contenteditable') === 'true')) return true;" +
            "      parent = parent.parentElement;" +
            "    }" +
            "    return false;" +
            "  }" +
            "  function updateFocusState() {" +
            "    const el = getDeepActiveElement();" +
            "    const isTextInput = isTextInputElement(el);" +
            // デバッグ: フォーカス要素の情報をログ
            "    console.log('[MochiOS:FocusDebug] tag=' + (el ? el.tagName : 'null') + ', type=' + (el ? el.type : 'null') + ', role=' + (el ? el.getAttribute('role') : 'null') + ', contentEditable=' + (el ? el.isContentEditable : 'null') + ', isTextInput=' + isTextInput);" +
            "    if (isTextInput !== mochiOS_hasTextFocus) {" +
            "      mochiOS_hasTextFocus = isTextInput;" +
            "      console.log('[MochiOS:TextFocus]' + isTextInput);" +
            "    }" +
            "  }" +
            "  document.addEventListener('focusin', updateFocusState, true);" +
            "  document.addEventListener('focusout', updateFocusState, true);" +
            // クリックイベントでも検出（YouTube等のカスタム要素対応）
            "  document.addEventListener('click', function(e) {" +
            "    var target = e.target;" +
            "    if (isTextInputElement(target)) {" +
            "      if (!mochiOS_hasTextFocus) {" +
            "        mochiOS_hasTextFocus = true;" +
            "        console.log('[MochiOS:TextFocus]true');" +
            "      }" +
            "    }" +
            "  }, true);" +
            // inputイベントでテキスト入力を検出
            "  document.addEventListener('input', function(e) {" +
            "    if (!mochiOS_hasTextFocus) {" +
            "      mochiOS_hasTextFocus = true;" +
            "      console.log('[MochiOS:TextFocus]true');" +
            "    }" +
            "  }, true);" +
            "  updateFocusState();" +
            // 選択テキスト監視（TextInputProtocol用）
            "  document.addEventListener('selectionchange', function() {" +
            "    var text = '';" +
            "    var el = getDeepActiveElement();" +
            "    if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {" +
            "      text = el.value.substring(el.selectionStart, el.selectionEnd);" +
            "    } else {" +
            "      var sel = window.getSelection();" +
            "      if (sel && sel.rangeCount > 0) {" +
            "        text = sel.toString();" +
            "      }" +
            "    }" +
            "    console.log('[MochiOS:Selection]' + text);" +
            "  });" +
            "})();";

        browser.executeJavaScript(script, browser.getURL(), 0);
        log("Text input focus and selection detection script injected");
    }

    /**
     * 現在のURLを取得する。
     * displayUrlが設定されている場合はそれを返す（IPvM URL用）。
     *
     * @return 現在のURL（表示用）
     */
    public String getCurrentURL() {
        // displayUrlが設定されていて、現在のURLがdata:で始まる場合はdisplayUrlを返す
        if (displayUrl != null && currentUrl != null && currentUrl.startsWith("data:")) {
            return displayUrl;
        }
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
     * キャッシュされた選択テキストを取得する。
     * JavaScript側で選択変更時にconsole.logで通知され、
     * onConsoleMessageで受信してキャッシュが更新される。
     *
     * @return キャッシュされた選択テキスト
     */
    public String getCachedSelectedText() {
        return cachedSelectedText;
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
     * マウスドラッグイベントを送信する。
     *
     * @param x X座標
     * @param y Y座標
     * @param button マウスボタン（1=左、2=中、3=右）
     */
    public void sendMouseDragged(int x, int y, int button) {
        enqueueInput(InputEvent.mouseDrag(x, y, button));
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
     * @param shiftPressed Shiftキーが押されているか
     * @param ctrlPressed Ctrlキーが押されているか
     * @param altPressed Altキーが押されているか
     * @param metaPressed Metaキー（Command/Windowsキー）が押されているか
     */
    public void sendKeyPressed(int keyCode, char keyChar, boolean shiftPressed, boolean ctrlPressed, boolean altPressed, boolean metaPressed) {
        System.out.println("[ChromiumBrowser] sendKeyPressed: keyCode=" + keyCode + ", keyChar=" + (int)keyChar +
                           ", shift=" + shiftPressed + ", ctrl=" + ctrlPressed + ", alt=" + altPressed + ", meta=" + metaPressed);
        enqueueInput(InputEvent.keyPress(keyCode, keyChar));
        System.out.println("[ChromiumBrowser] Enqueued KEY_PRESS event, queue size=" + inputQueue.size());
    }

    /**
     * キー離しイベントを送信する。
     *
     * @param keyCode キーコード
     * @param keyChar 文字
     * @param shiftPressed Shiftキーが押されているか
     * @param ctrlPressed Ctrlキーが押されているか
     * @param altPressed Altキーが押されているか
     * @param metaPressed Metaキー（Command/Windowsキー）が押されているか
     */
    public void sendKeyReleased(int keyCode, char keyChar, boolean shiftPressed, boolean ctrlPressed, boolean altPressed, boolean metaPressed) {
        System.out.println("[ChromiumBrowser] sendKeyReleased: keyCode=" + keyCode + ", keyChar=" + (int)keyChar +
                           ", shift=" + shiftPressed + ", ctrl=" + ctrlPressed + ", alt=" + altPressed + ", meta=" + metaPressed);
        enqueueInput(InputEvent.keyRelease(keyCode, keyChar));
        System.out.println("[ChromiumBrowser] Enqueued KEY_RELEASE event, queue size=" + inputQueue.size());
    }

    public void flushInputEvents() {
        if (inputQueue.size() > 0) {
            System.out.println("[ChromiumBrowser] flushInputEvents: queue size=" + inputQueue.size());
        }

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
            if (event.type == InputEvent.Type.KEY_PRESS || event.type == InputEvent.Type.KEY_RELEASE) {
                System.out.println("[ChromiumBrowser] Processing " + event.type + " event: keyCode=" + event.keyCode);
            }
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
        if (browser == null || renderHandler == null) {
            return;
        }

        if (newWidth <= 0 || newHeight <= 0) {
            logError("Invalid resize dimensions: " + newWidth + "x" + newHeight);
            return;
        }

        log("Resizing browser from " + width + "x" + height + " to " + newWidth + "x" + newHeight);

        // 1. 内部のwidth/heightを更新
        this.width = newWidth;
        this.height = newHeight;

        // 2. RenderHandlerのサイズを更新（getViewRect()が新サイズを返すようになる）
        renderHandler.setSize(newWidth, newHeight);

        // 3. ブラウザの内部サイズ情報を強制更新
        // JCEFのCefBrowserOsrはGLCanvasのイベントに依存してサイズを更新するが、
        // タイミングによってはこれが間に合わないため、リフレクションで直接更新する。
        try {
            // A. browser_rect_ フィールドの更新 (CefBrowserOsr)
            java.lang.reflect.Field rectField = getFieldRecursive(browser.getClass(), "browser_rect_");
            if (rectField != null) {
                rectField.setAccessible(true);
                rectField.set(browser, new java.awt.Rectangle(0, 0, newWidth, newHeight));
                log("Updated browser_rect_ to " + newWidth + "x" + newHeight);
            } else {
                log("Field 'browser_rect_' not found");
            }

            // B. wasResized(int, int) の呼び出し (CefBrowser_N)
            java.lang.reflect.Method wasResizedMethod = getMethodRecursive(browser.getClass(), "wasResized", int.class, int.class);
            if (wasResizedMethod != null) {
                wasResizedMethod.setAccessible(true);
                wasResizedMethod.invoke(browser, newWidth, newHeight);
                log("Called wasResized(" + newWidth + ", " + newHeight + ")");
            } else {
                // 引数なしのwasResized()を試す (Fallback)
                try {
                    java.lang.reflect.Method fallbackMethod = browser.getClass().getMethod("wasResized");
                    fallbackMethod.invoke(browser);
                    log("Called wasResized() [no args]");
                } catch (NoSuchMethodException nsme) {
                    log("Method wasResized() not found");
                }
            }
        } catch (Exception e) {
            logError("Failed to force resize: " + e.getMessage());
        }
    }

    private java.lang.reflect.Method getMethodRecursive(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                java.lang.reflect.Method m = c.getDeclaredMethod(methodName, parameterTypes);
                return m;
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            }
        }
        return null;
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
     * ページロード完了後に強制的に再描画をトリガーする。
     * onPaintが初回のみ呼ばれる問題を解決するため、
     * 複数回にわたってinvalidate、wasResized、display()を呼び出す。
     */
    private void forceRepaintAfterLoad() {
        new Thread(() -> {
            try {
                // DOMのレンダリング完了を待機（JavaScriptの実行も含む）
                Thread.sleep(500);

                log("forceRepaintAfterLoad: Starting OSR invalidation sequence...");

                // 複数回試行（CEFのOSRレンダリングをトリガー）
                for (int attempt = 0; attempt < 10; attempt++) {
                    log("forceRepaintAfterLoad: Attempt " + (attempt + 1) + "/10");

                    if (browser == null) break;

                    // 1. サイズを一時的に変更してOSR再描画をトリガー（+1/-1ピクセル）
                    try {
                        renderHandler.setSize(width + 1, height);
                        java.lang.reflect.Method wasResizedMethod = getMethodRecursive(browser.getClass(), "wasResized", int.class, int.class);
                        if (wasResizedMethod != null) {
                            wasResizedMethod.setAccessible(true);
                            wasResizedMethod.invoke(browser, width + 1, height);
                            log("forceRepaintAfterLoad: Resized to " + (width + 1) + "x" + height);
                        }
                        Thread.sleep(100);

                        // 元のサイズに戻す
                        renderHandler.setSize(width, height);
                        if (wasResizedMethod != null) {
                            wasResizedMethod.invoke(browser, width, height);
                            log("forceRepaintAfterLoad: Resized back to " + width + "x" + height);
                        }
                    } catch (Exception e) {
                        log("forceRepaintAfterLoad: Resize failed: " + e.getMessage());
                    }

                    // 2. setWindowVisibility(true)を呼び出し
                    try {
                        java.lang.reflect.Method setVisibilityMethod = getMethodRecursive(browser.getClass(), "setWindowVisibility", boolean.class);
                        if (setVisibilityMethod != null) {
                            setVisibilityMethod.setAccessible(true);
                            setVisibilityMethod.invoke(browser, true);
                        }
                    } catch (Exception e) {
                        // Silent
                    }

                    // 3. GLCanvasのinvalidate()とrepaint()を呼び出し
                    if (provider != null && provider.supportsUIComponent()) {
                        try {
                            java.awt.Component uiComponent = browser.getUIComponent();
                            if (uiComponent != null) {
                                uiComponent.invalidate();
                                uiComponent.repaint();

                                // display()を呼び出し
                                java.lang.reflect.Method displayMethod = uiComponent.getClass().getMethod("display");
                                displayMethod.setAccessible(true);
                                displayMethod.invoke(uiComponent);
                                log("forceRepaintAfterLoad: Called invalidate/repaint/display on UIComponent");
                            }
                        } catch (Exception e) {
                            // Silent
                        }
                    }

                    // 4. sendMouseMoved()でダミーマウスイベントを送信
                    try {
                        provider.sendMouseMoved(browser, width / 2 + attempt, height / 2);
                    } catch (Exception e) {
                        // Silent
                    }

                    // 待機してから次の試行
                    Thread.sleep(200);
                }

                log("forceRepaintAfterLoad: Completed OSR invalidation sequence");

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "ChromiumBrowser-ForceRepaint").start();
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
            log("Attempting to trigger rendering via setSize/wasResized()...");
            log("Browser class: " + browser.getClass().getName());
            log("Target size: " + width + "x" + height);

            // CefBrowserOsrNoCanvasの場合はsetSize()を直接呼び出す
            // これはForge環境でAWTを使わないブラウザの場合
            try {
                java.lang.reflect.Method setSizeMethod = browser.getClass().getMethod("setSize", int.class, int.class);
                setSizeMethod.setAccessible(true);
                setSizeMethod.invoke(browser, width, height);
                log("✅ setSize() called successfully - rendering should start");
                return;
            } catch (NoSuchMethodException e) {
                log("setSize(int, int) method not found, trying resize()...");
            }

            // resize()メソッドを試す
            try {
                java.lang.reflect.Method resizeMethod = browser.getClass().getMethod("resize", int.class, int.class);
                resizeMethod.setAccessible(true);
                resizeMethod.invoke(browser, width, height);
                log("✅ resize() called successfully - rendering may start");
                return;
            } catch (NoSuchMethodException e) {
                log("resize(int, int) method not found, falling back to wasResized()");
            }

            // wasResized()を試す
            java.lang.reflect.Method wasResizedMethod = browser.getClass().getMethod("wasResized", int.class, int.class);
            wasResizedMethod.setAccessible(true);
            wasResizedMethod.invoke(browser, width, height);

            log("✅ wasResized() called successfully - rendering may start");
        } catch (NoSuchMethodException e) {
            logError("wasResized(int, int) method not found on browser");
        } catch (Exception e) {
            logError("Failed to trigger rendering: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static final class InputEvent {
        private enum Type {
            MOUSE_PRESS,
            MOUSE_RELEASE,
            MOUSE_MOVE,
            MOUSE_DRAG,
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

        static InputEvent mouseDrag(int x, int y, int button) {
            return new InputEvent(Type.MOUSE_DRAG, x, y, 0f, button, 0, (char) 0);
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
                case MOUSE_DRAG:
                    provider.sendMouseDragged(browser, x, y, button);
                    break;
                case MOUSE_WHEEL:
                    provider.sendMouseWheel(browser, x, y, wheelDelta);
                    break;
                case KEY_PRESS:
                    boolean shiftPressed = kernel != null && kernel.isShiftPressed();
                    boolean ctrlPressed = kernel != null && kernel.isCtrlPressed();
                    boolean altPressed = kernel != null && kernel.isAltPressed();
                    boolean metaPressed = kernel != null && kernel.isMetaPressed();
                    provider.sendKeyPressed(browser, keyCode, keyChar, shiftPressed, ctrlPressed, altPressed, metaPressed);
                    break;
                case KEY_RELEASE:
                    System.out.println("[ChromiumBrowser] Processing KEY_RELEASE event: keyCode=" + keyCode);
                    boolean shiftReleased = kernel != null && kernel.isShiftPressed();
                    boolean ctrlReleased = kernel != null && kernel.isCtrlPressed();
                    boolean altReleased = kernel != null && kernel.isAltPressed();
                    boolean metaReleased = kernel != null && kernel.isMetaPressed();
                    provider.sendKeyReleased(browser, keyCode, keyChar, shiftReleased, ctrlReleased, altReleased, metaReleased);
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

    /**
     * Android Intent URLを通常のURLに変換する。
     * Intent URL形式: intent://HOST/PATH#Intent;scheme=SCHEME;S.browser_fallback_url=URL;end;
     * フォールバックURLが存在すればそれを使用し、なければscheme+host+pathから構築する。
     *
     * @param url 変換対象のURL
     * @return 変換後のURL（Intent URLでなければそのまま返す）
     */
    private String convertIntentUrl(String url) {
        if (url == null || !url.startsWith("intent://")) {
            return url;
        }

        try {
            // Extract browser_fallback_url parameter
            String fallbackPrefix = "S.browser_fallback_url=";
            int fallbackStart = url.indexOf(fallbackPrefix);
            if (fallbackStart != -1) {
                int fallbackEnd = url.indexOf(";", fallbackStart);
                if (fallbackEnd != -1) {
                    String encodedFallbackUrl = url.substring(fallbackStart + fallbackPrefix.length(), fallbackEnd);
                    // URL decode
                    String fallbackUrl = java.net.URLDecoder.decode(encodedFallbackUrl, "UTF-8");
                    log("Converted Intent URL to fallback: " + fallbackUrl);
                    return fallbackUrl;
                }
            }

            // If no fallback URL, construct from scheme and host/path
            int intentStart = "intent://".length();
            int intentEnd = url.indexOf("#Intent");
            if (intentEnd == -1) {
                intentEnd = url.length();
            }
            String hostAndPath = url.substring(intentStart, intentEnd);

            // Extract scheme parameter
            String scheme = "https"; // default
            String schemePrefix = "scheme=";
            int schemeStart = url.indexOf(schemePrefix);
            if (schemeStart != -1) {
                int schemeEnd = url.indexOf(";", schemeStart);
                if (schemeEnd != -1) {
                    scheme = url.substring(schemeStart + schemePrefix.length(), schemeEnd);
                }
            }

            String convertedUrl = scheme + "://" + hostAndPath;
            log("Converted Intent URL to: " + convertedUrl);
            return convertedUrl;

        } catch (Exception e) {
            logError("Failed to convert Intent URL: " + e.getMessage());
            return url; // Return original URL if conversion fails
        }
    }

    private java.lang.reflect.Field getFieldRecursive(Class<?> clazz, String fieldName) {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        return null;
    }

    /**
     * レンダリング準備が整っているか（GLContextが初期化されているか）を確認する。
     * JCEFのOSR実装はGLContextがないとonPaintイベントを発火しないため、
     * このメソッドで確認してからURLロードや再描画を行うことが望ましい。
     *
     * @return 準備完了ならtrue
     */
    public boolean isReadyToRender() {
        // まずglContextReadyフラグをチェック（高速パス）
        if (glContextReady) {
            return true;
        }

        if (browser == null) return false;

        // フラグが設定されていない場合、リフレクションでGLContextを直接確認
        try {
            java.lang.reflect.Field canvasField = getFieldRecursive(browser.getClass(), "canvas_");
            if (canvasField != null) {
                canvasField.setAccessible(true);
                Object canvas = canvasField.get(browser);
                if (canvas != null) {
                    java.lang.reflect.Method getContextMethod = canvas.getClass().getMethod("getContext");
                    Object context = getContextMethod.invoke(canvas);
                    boolean isReady = context != null;
                    if (isReady) {
                        // GLContextが準備完了ならフラグも更新
                        glContextReady = true;
                        glInitLatch.countDown();
                    }
                    return isReady;
                }
            }
        } catch (Exception e) {
            logError("Failed to check render readiness: " + e.getMessage());
        }
        return false;
    }

    /**
     * MCEF環境（Forge）かどうかを返す。
     * MCEF環境ではJavaScriptコンソールメッセージが読み取れないため、
     * テキストフォーカス検出が動作しない。
     *
     * @return MCEF環境の場合true
     */
    public boolean isMCEF() {
        return provider != null && "MCEF".equals(provider.getName());
    }

    // ===== IPvM仮想ネットワーク対応 =====

    /**
     * IPvMアドレスパターン: [type]-[identifier]
     * type: 0=Player, 1=Device, 2=Server, 3=System
     */
    private static final java.util.regex.Pattern IPVM_HOST_PATTERN = java.util.regex.Pattern.compile(
        "^[0-3]-(" +
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" +
        "|" +
        "[a-zA-Z0-9][a-zA-Z0-9_-]*" +
        ")$"
    );

    /**
     * URLがIPvMアドレスかどうかを判定する。
     *
     * @param url 判定対象のURL
     * @return IPvMアドレスの場合true
     */
    private boolean isIPvMUrl(String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            String scheme = uri.getScheme();

            // HTTPまたはHTTPSスキームのみ処理
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                return false;
            }

            if (host == null || host.isEmpty()) {
                return false;
            }

            // IPvMアドレスパターンにマッチするか確認
            return IPVM_HOST_PATTERN.matcher(host).matches();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * IPvMリクエスト用のCefResourceRequestHandlerを作成する。
     *
     * @param url リクエストURL
     * @return CefResourceRequestHandler
     */
    private CefResourceRequestHandler createIPvMResourceRequestHandler(String url) {
        // 現在は使用しない（onBeforeBrowseでhandleIPvMNavigationを使用）
        return null;
    }

    /**
     * IPvM URLナビゲーションを処理する。
     * HTMLを取得してdata: URLでロードする。
     *
     * @param browser CefBrowser
     * @param url IPvM URL
     */
    private void handleIPvMNavigation(CefBrowser browser, String url) {
        try {
            java.net.URI uri = new java.net.URI(url);
            String host = uri.getHost();
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            log("handleIPvMNavigation: host=" + host + ", path=" + path);

            // NetworkAdapterからHTMLを取得
            jp.moyashi.phoneos.core.service.network.NetworkAdapter networkAdapter = kernel.getNetworkAdapter();
            if (networkAdapter == null) {
                log("NetworkAdapter is null, loading error page");
                loadErrorPage(browser, "Network adapter not available");
                return;
            }

            jp.moyashi.phoneos.core.service.network.VirtualAdapter virtualAdapter = networkAdapter.getVirtualAdapter();
            if (virtualAdapter == null) {
                log("VirtualAdapter is null, loading error page");
                loadErrorPage(browser, "Virtual adapter not available");
                return;
            }

            // IPvMアドレスをパース
            jp.moyashi.phoneos.core.service.network.IPvMAddress destination =
                    jp.moyashi.phoneos.core.service.network.IPvMAddress.fromString(host);

            final String finalPath = path;
            final String finalUrl = url;

            // 非同期でHTTPリクエストを送信
            virtualAdapter.httpRequest(destination, path, "GET")
                    .orTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .thenAccept(response -> {
                        if (response.isSuccess()) {
                            String html = response.getBody();
                            log("IPvM response received: " + (html != null ? html.length() : 0) + " chars");
                            loadHtmlAsDataUrl(browser, html, finalUrl);
                        } else {
                            log("IPvM request failed: " + response.getStatusCode());
                            loadErrorPage(browser, response.getStatusText());
                        }
                    })
                    .exceptionally(e -> {
                        logError("IPvM request error: " + e.getMessage());
                        loadErrorPage(browser, e.getMessage());
                        return null;
                    });

        } catch (Exception e) {
            logError("handleIPvMNavigation error: " + e.getMessage());
            loadErrorPage(browser, e.getMessage());
        }
    }

    /**
     * HTMLをdata: URLとしてロードする。
     */
    private void loadHtmlAsDataUrl(CefBrowser browser, String html, String originalUrl) {
        if (html == null || html.isEmpty()) {
            html = "<html><body><h1>Empty Response</h1></body></html>";
        }

        try {
            String base64Html = java.util.Base64.getEncoder().encodeToString(
                    html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String dataUrl = "data:text/html;charset=utf-8;base64," + base64Html;
            log("Loading HTML as data URL (length: " + dataUrl.length() + ")");
            browser.loadURL(dataUrl);
        } catch (Exception e) {
            logError("Failed to create data URL: " + e.getMessage());
            loadErrorPage(browser, "Failed to load page");
        }
    }

    /**
     * エラーページをロードする。
     */
    private void loadErrorPage(CefBrowser browser, String message) {
        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Error</title></head>" +
                "<body style=\"background:#1a1a2e;color:#fff;font-family:sans-serif;text-align:center;padding:50px;\">" +
                "<h1 style=\"color:#e74c3c;\">Error</h1><p>" + message + "</p></body></html>";
        loadHtmlAsDataUrl(browser, html, "about:error");
    }

    /**
     * IPvMコンテンツをロードする。
     * VirtualAdapterからHTMLを取得し、CefFrame.loadString()を使用して表示する。
     * これによりURLバーにはhttpm://が表示され、HTMLも正しくレンダリングされる。
     *
     * @param browser CefBrowser
     * @param originalUrl 元のURL（http://3-sys-test/）
     * @param displayUrl 表示用URL（httpm://3-sys-test/）
     */
    private void loadIPvMContent(CefBrowser browser, String originalUrl, String displayUrl) {
        try {
            java.net.URI uri = new java.net.URI(originalUrl);
            String host = uri.getHost();
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            log("loadIPvMContent: host=" + host + ", path=" + path + ", displayUrl=" + displayUrl);

            // NetworkAdapterからHTMLを取得
            jp.moyashi.phoneos.core.service.network.NetworkAdapter networkAdapter = kernel.getNetworkAdapter();
            if (networkAdapter == null) {
                log("NetworkAdapter is null, loading error page");
                loadErrorPageWithUrl(browser, "Network adapter not available", displayUrl);
                return;
            }

            jp.moyashi.phoneos.core.service.network.VirtualAdapter virtualAdapter = networkAdapter.getVirtualAdapter();
            if (virtualAdapter == null) {
                log("VirtualAdapter is null, loading error page");
                loadErrorPageWithUrl(browser, "Virtual adapter not available", displayUrl);
                return;
            }

            // IPvMアドレスをパース
            jp.moyashi.phoneos.core.service.network.IPvMAddress destination =
                    jp.moyashi.phoneos.core.service.network.IPvMAddress.fromString(host);

            final String finalDisplayUrl = displayUrl;

            // 非同期でHTTPリクエストを送信
            virtualAdapter.httpRequest(destination, path, "GET")
                    .orTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .thenAccept(response -> {
                        if (response.isSuccess()) {
                            String html = response.getBody();
                            log("IPvM response received: " + (html != null ? html.length() : 0) + " chars");
                            loadHtmlWithUrl(browser, html, finalDisplayUrl);
                        } else {
                            log("IPvM request failed: " + response.getStatusCode());
                            loadErrorPageWithUrl(browser, response.getStatusText(), finalDisplayUrl);
                        }
                    })
                    .exceptionally(e -> {
                        logError("IPvM request error: " + e.getMessage());
                        loadErrorPageWithUrl(browser, e.getMessage(), finalDisplayUrl);
                        return null;
                    });

        } catch (Exception e) {
            logError("loadIPvMContent error: " + e.getMessage());
            loadErrorPageWithUrl(browser, e.getMessage(), displayUrl);
        }
    }

    /**
     * HTMLをdata: URLでロードし、表示用URLを設定する。
     *
     * @param browser CefBrowser
     * @param html HTMLコンテンツ
     * @param displayUrlParam URLバーに表示するURL
     */
    private void loadHtmlWithUrl(CefBrowser browser, String html, String displayUrlParam) {
        if (html == null || html.isEmpty()) {
            html = "<html><body><h1>Empty Response</h1></body></html>";
        }

        try {
            // 表示用URLを設定（getCurrentURL()で使用）
            this.displayUrl = displayUrlParam;
            log("Set displayUrl: " + displayUrlParam);

            // data: URLでロード
            String base64Html = java.util.Base64.getEncoder().encodeToString(
                    html.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String dataUrl = "data:text/html;charset=utf-8;base64," + base64Html;
            log("Loading HTML via data: URL, displayUrl=" + displayUrlParam);
            browser.loadURL(dataUrl);
        } catch (Exception e) {
            logError("loadHtmlWithUrl error: " + e.getMessage());
            loadHtmlAsDataUrl(browser, html, displayUrlParam);
        }
    }

    /**
     * エラーページをロードし、URLバーに指定URLを表示する。
     *
     * @param browser CefBrowser
     * @param message エラーメッセージ
     * @param displayUrl URLバーに表示するURL
     */
    private void loadErrorPageWithUrl(CefBrowser browser, String message, String displayUrl) {
        String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>Error</title></head>" +
                "<body style=\"background:#1a1a2e;color:#fff;font-family:sans-serif;text-align:center;padding:50px;\">" +
                "<h1 style=\"color:#e74c3c;\">Error</h1><p>" + message + "</p></body></html>";
        loadHtmlWithUrl(browser, html, displayUrl);
    }
}
