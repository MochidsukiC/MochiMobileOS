package jp.moyashi.phoneos.core.service.webview;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import java.awt.image.BufferedImage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * WebViewのラッパークラス。
 * スナップショット取得、HTML読み込み、DOM操作などの便利なメソッドを提供する。
 *
 * パフォーマンス最適化:
 * - BufferedImage → PImage の一括ピクセル転送（超高速）
 * - DOM変更検知によるスナップショットキャッシング
 * - pg.image()による一括描画（60FPS維持）
 */
public class WebViewWrapper {
    private final WebView webView;
    private final WebEngine engine;
    private final Scene scene;
    private final javafx.stage.Stage stage; // Stageへの参照（バックグラウンド制御用）
    private final int width;
    private final int height;
    private final JSBridge jsBridge;
    private final jp.moyashi.phoneos.core.Kernel kernel; // LoggerService取得用
    private final AtomicBoolean needsUpdate = new AtomicBoolean(true);
    private final AtomicBoolean disposed = new AtomicBoolean(false);
    private final AtomicBoolean snapshotInProgress = new AtomicBoolean(false);
    private volatile boolean firstSnapshotDone = false;
    private volatile boolean loadSucceeded = false; // HTML読み込み完了フラグ
    private volatile int readyFrameCount = 0;
    private static final int READY_DELAY_FRAMES = 60; // HTMLレンダリング完了まで60フレーム（約1秒）待つ

    /** バックグラウンド状態フラグ（OS側で強制制御） */
    private volatile boolean isInBackground = false;

    // キャッシュ（BufferedImageとPImageの両方をキャッシュ）
    private BufferedImage cachedBufferedImage;
    private PImage cachedPImage;

    /**
     * ログ出力ヘルパーメソッド（デバッグレベル）
     */
    private void log(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().debug("WebViewWrapper", message);
        }
    }

    /**
     * エラーログ出力ヘルパーメソッド
     */
    private void logError(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("WebViewWrapper", message);
        }
    }

    /**
     * エラーログ出力ヘルパーメソッド（例外付き）
     */
    private void logError(String message, Throwable throwable) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("WebViewWrapper", message, throwable);
        }
    }

    public WebViewWrapper(WebView webView, Scene scene, javafx.stage.Stage stage, int width, int height, JSBridge jsBridge, jp.moyashi.phoneos.core.Kernel kernel) {
        this.webView = webView;
        this.engine = webView.getEngine();
        this.scene = scene;
        this.stage = stage;
        this.width = width;
        this.height = height;
        this.jsBridge = jsBridge;
        this.kernel = kernel;

        // モバイル用User-Agentを設定してスマートフォンOSであることを伝える
        String mobileUserAgent = "Mozilla/5.0 (Linux; Android 12; MochiMobileOS) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36";
        engine.setUserAgent(mobileUserAgent);
        log("User-Agent set to: " + mobileUserAgent);

        // パフォーマンス最適化：JavaScriptとWebGL有効化、コンテキストメニュー無効化
        engine.setJavaScriptEnabled(true);
        webView.setContextMenuEnabled(false); // 右クリックメニューを無効化してパフォーマンス向上

        // PImageキャッシュは最初のスナップショット取得時に作成される（nullのまま）
        // これにより、HTML読み込み前に黒い画面が表示されるのを防ぐ

        // DOM変更検知（SUCCEEDEDの時のみ更新をマークし、読み込み完了フラグを設定）
        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                loadSucceeded = true;
                readyFrameCount = 0; // フレームカウントをリセット
                needsUpdate.set(true);
                // 古いキャッシュは保持したまま、新しいスナップショット取得を要求
                // （新しいスナップショットが準備できるまでローディング画面が表示される）
                log("Load state changed to SUCCEEDED - marking for update");

                // モバイルViewportを設定（レスポンシブデザイン対応）
                injectMobileViewport();
            }
        });

        // JavaScriptブリッジをWebEngineに注入
        if (jsBridge != null) {
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    jsBridge.injectIntoWebView(this);
                }
            });
        }

        // コンソールログをキャプチャ（デバッグ用）
        engine.setOnAlert(event -> {
            log("[WebView Console] " + event.getData());
        });
    }

    /**
     * HTMLコンテンツを読み込む。
     *
     * @param htmlContent HTML文字列
     */
    public void loadContent(String htmlContent) {
        if (disposed.get()) {
            logError("Cannot load content - wrapper is disposed");
            return;
        }

        Platform.runLater(() -> {
            engine.loadContent(htmlContent);
            needsUpdate.set(true);
        });
    }

    /**
     * URLを読み込む。
     *
     * @param url URL文字列
     */
    public void loadURL(String url) {
        if (disposed.get()) {
            logError("Cannot load URL - wrapper is disposed");
            return;
        }

        Platform.runLater(() -> {
            engine.load(url);
            needsUpdate.set(true);
        });
    }

    /**
     * JavaScriptを実行する。
     *
     * @param script JavaScriptコード
     * @return 実行結果
     */
    public Object executeScript(String script) {
        if (disposed.get()) {
            logError("Cannot execute script - wrapper is disposed");
            return null;
        }

        Object[] result = new Object[1];
        CountDownLatch latch = new CountDownLatch(1);

        Platform.runLater(() -> {
            try {
                result[0] = engine.executeScript(script);
                needsUpdate.set(true); // DOMが変更された可能性がある
            } catch (Exception e) {
                logError("Script execution error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return result[0];
    }

    /**
     * WebViewのスナップショットを取得してPGraphicsに描画する（最適化版）。
     * キャッシングにより、DOM変更時のみスナップショットを取得する。
     *
     * パフォーマンス:
     * - DOM変更なし: ~0.1ms（キャッシュ使用）
     * - DOM変更あり: ~5-10ms（スナップショット取得 + 変換）
     *
     * @param pg PGraphicsインスタンス
     */
    public void renderToPGraphics(PGraphics pg) {
        if (disposed.get()) {
            return;
        }

        // バックグラウンド状態の場合は、スナップショット取得を完全にスキップ（OS側の強制制御）
        // キャッシュも描画しないことで、GPU使用率を最小化
        if (isInBackground) {
            return;
        }

        // HTML読み込み完了後のフレームカウントをインクリメント
        if (loadSucceeded && readyFrameCount < READY_DELAY_FRAMES) {
            readyFrameCount++;
        }

        // キャッシュが有効で、更新不要な場合は既存のPImageを使用（準備完了時のみ描画）
        if (!needsUpdate.get() && cachedPImage != null) {
            if (isReady()) {
                pg.image(cachedPImage, 0, 0);
            }
            return;
        }

        // 既にスナップショット取得中の場合は、既存のキャッシュを使用（非ブロッキング、準備完了時のみ描画）
        if (snapshotInProgress.get()) {
            if (cachedPImage != null && isReady()) {
                pg.image(cachedPImage, 0, 0);
            }
            return;
        }

        // スナップショット取得を開始（非同期）
        if (!snapshotInProgress.compareAndSet(false, true)) {
            // 別のスレッドが既に取得中（準備完了時のみ描画）
            if (cachedPImage != null && isReady()) {
                pg.image(cachedPImage, 0, 0);
            }
            return;
        }

        long startTime = System.nanoTime();
        log("Starting async snapshot capture...");

        // 完全非同期でスナップショット取得
        Platform.runLater(() -> {
            try {
                long snapshotStartTime = System.nanoTime();

                // WebViewのスナップショットを取得
                WritableImage snapshot = webView.snapshot(null, null);

                // BufferedImageに変換
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);

                long snapshotEndTime = System.nanoTime();
                double snapshotMs = (snapshotEndTime - snapshotStartTime) / 1_000_000.0;
                log("Async snapshot captured in " + snapshotMs + " ms");

                // PImageに変換（JavaFXスレッド上で実行）
                if (bufferedImage != null) {
                    long convertStartTime = System.nanoTime();
                    cachedBufferedImage = bufferedImage;
                    // cachedPImageがnullの場合は新しく作成
                    if (cachedPImage == null) {
                        cachedPImage = new PImage(width, height, PApplet.ARGB);
                    }
                    convertBufferedImageToPImage(cachedBufferedImage, cachedPImage);
                    firstSnapshotDone = true;
                    needsUpdate.set(false);
                    long convertEndTime = System.nanoTime();
                    double convertMs = (convertEndTime - convertStartTime) / 1_000_000.0;
                    log("Image conversion completed in " + convertMs + " ms");
                }
            } catch (Exception e) {
                logError("Async snapshot failed: " + e.getMessage());
                e.printStackTrace();
            } finally {
                snapshotInProgress.set(false);
            }
        });

        // キャッシュがあり、準備完了の場合のみ描画（前フレームの画像）
        if (cachedPImage != null && isReady()) {
            pg.image(cachedPImage, 0, 0);
        }

        long endTime = System.nanoTime();
        double totalMs = (endTime - startTime) / 1_000_000.0;
        log("Render time (non-blocking): " + totalMs + " ms");
    }

    /**
     * WebViewのスナップショットを取得してPGraphicsの指定位置に描画する（オフセット版）。
     * HTMLウィジェット用。
     *
     * @param pg PGraphicsインスタンス
     * @param x X座標オフセット
     * @param y Y座標オフセット
     */
    public void renderToPGraphics(PGraphics pg, int x, int y) {
        if (disposed.get()) {
            return;
        }

        // キャッシュが有効で、更新不要な場合は既存のPImageを使用
        if (!needsUpdate.get() && cachedPImage != null) {
            pg.image(cachedPImage, x, y);
            return;
        }

        // スナップショット取得
        CountDownLatch latch = new CountDownLatch(1);
        BufferedImage[] imageHolder = new BufferedImage[1];

        Platform.runLater(() -> {
            try {
                WritableImage snapshot = webView.snapshot(null, null);
                imageHolder[0] = SwingFXUtils.fromFXImage(snapshot, null);
            } catch (Exception e) {
                logError("Snapshot failed");
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        });

        try {
            latch.await(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (imageHolder[0] != null) {
            cachedBufferedImage = imageHolder[0];
            convertBufferedImageToPImage(cachedBufferedImage, cachedPImage);
            needsUpdate.set(false);
        }

        // PImageを指定位置に描画
        if (cachedPImage != null) {
            pg.image(cachedPImage, x, y);
        }
    }

    /**
     * BufferedImageをPImageに変換する（最適化版）。
     * ピクセル配列を一括コピーするため、超高速（240,000ピクセルでも~1ms）。
     *
     * @param bufferedImg BufferedImage
     * @param pImg PImage（出力先）
     */
    private void convertBufferedImageToPImage(BufferedImage bufferedImg, PImage pImg) {
        if (bufferedImg == null || pImg == null) return;

        int w = Math.min(bufferedImg.getWidth(), pImg.width);
        int h = Math.min(bufferedImg.getHeight(), pImg.height);

        // BufferedImageのピクセル配列を一括取得（超高速）
        bufferedImg.getRGB(0, 0, w, h, pImg.pixels, 0, w);

        // PImageのピクセル更新フラグを立てる
        pImg.updatePixels();
    }

    /**
     * マウスクリックイベントをWebViewにシミュレートする。
     *
     * @param x X座標
     * @param y Y座標
     */
    public void simulateMouseClick(int x, int y) {
        if (disposed.get()) {
            return;
        }

        Platform.runLater(() -> {
            // JavaScriptでクリックイベントを発火
            String script = String.format(
                "var element = document.elementFromPoint(%d, %d);" +
                "if (element) {" +
                "  var event = new MouseEvent('click', {" +
                "    view: window," +
                "    bubbles: true," +
                "    cancelable: true," +
                "    clientX: %d," +
                "    clientY: %d" +
                "  });" +
                "  element.dispatchEvent(event);" +
                "  console.log('Clicked element:', element.tagName, 'at (%d, %d)');" +
                "}",
                x, y, x, y, x, y
            );
            engine.executeScript(script);
            needsUpdate.set(true);
        });
    }

    /**
     * マウスプレスイベントをWebViewにシミュレートする。
     *
     * @param x X座標
     * @param y Y座標
     */
    public void simulateMousePressed(int x, int y) {
        if (disposed.get()) {
            return;
        }

        Platform.runLater(() -> {
            String script = String.format(
                "var element = document.elementFromPoint(%d, %d);" +
                "if (element) {" +
                "  var event = new MouseEvent('mousedown', {" +
                "    view: window," +
                "    bubbles: true," +
                "    cancelable: true," +
                "    clientX: %d," +
                "    clientY: %d" +
                "  });" +
                "  element.dispatchEvent(event);" +
                "}",
                x, y, x, y
            );
            engine.executeScript(script);
            needsUpdate.set(true);
        });
    }

    /**
     * マウスリリースイベントをWebViewにシミュレートする。
     *
     * @param x X座標
     * @param y Y座標
     */
    public void simulateMouseReleased(int x, int y) {
        if (disposed.get()) {
            return;
        }

        Platform.runLater(() -> {
            String script = String.format(
                "var element = document.elementFromPoint(%d, %d);" +
                "if (element) {" +
                "  var event = new MouseEvent('mouseup', {" +
                "    view: window," +
                "    bubbles: true," +
                "    cancelable: true," +
                "    clientX: %d," +
                "    clientY: %d" +
                "  });" +
                "  element.dispatchEvent(event);" +
                "}",
                x, y, x, y
            );
            engine.executeScript(script);
            needsUpdate.set(true);
        });
    }

    /**
     * マウス移動イベントをWebViewにシミュレートする。
     * ホバー効果やカーソル変更を有効にする。
     *
     * @param x X座標
     * @param y Y座標
     */
    public void simulateMouseMoved(int x, int y) {
        if (disposed.get()) {
            return;
        }

        Platform.runLater(() -> {
            String script = String.format(
                "var element = document.elementFromPoint(%d, %d);" +
                "if (element) {" +
                "  var event = new MouseEvent('mousemove', {" +
                "    view: window," +
                "    bubbles: true," +
                "    cancelable: true," +
                "    clientX: %d," +
                "    clientY: %d" +
                "  });" +
                "  element.dispatchEvent(event);" +
                "}",
                x, y, x, y
            );
            engine.executeScript(script);
            needsUpdate.set(true);
        });
    }

    /**
     * スクロールイベントをWebViewにシミュレートする。
     *
     * @param x X座標
     * @param y Y座標
     * @param deltaY スクロール量（正の値：下スクロール、負の値：上スクロール）
     */
    public void simulateScroll(int x, int y, float deltaY) {
        if (disposed.get()) {
            log("simulateScroll: disposed, ignoring");
            return;
        }

        log("simulateScroll called: x=" + x + ", y=" + y + ", deltaY=" + deltaY);

        Platform.runLater(() -> {
            try {
                // JavaScriptコードを動的に構築（%fのロケール問題を回避）
                String script =
                    "(function() {" +
                    "  var scrollAmount = " + deltaY + ";" +
                    "  console.log('[MochiOS] simulateScroll: scrollAmount=' + scrollAmount);" +
                    "  console.log('[MochiOS] window.scrollY before:', window.scrollY);" +
                    "  window.scrollBy(0, scrollAmount);" +
                    "  console.log('[MochiOS] window.scrollY after:', window.scrollY);" +
                    "  " +
                    "  var element = document.elementFromPoint(" + x + ", " + y + ");" +
                    "  console.log('[MochiOS] Element at point:', element ? element.tagName : 'null');" +
                    "  if (element) {" +
                    "    var scrollableParent = element;" +
                    "    while (scrollableParent && scrollableParent !== document.body) {" +
                    "      var overflowY = window.getComputedStyle(scrollableParent).overflowY;" +
                    "      if (overflowY === 'auto' || overflowY === 'scroll') {" +
                    "        console.log('[MochiOS] Found scrollable element:', scrollableParent.tagName);" +
                    "        scrollableParent.scrollTop += scrollAmount;" +
                    "        break;" +
                    "      }" +
                    "      scrollableParent = scrollableParent.parentElement;" +
                    "    }" +
                    "  }" +
                    "  " +
                    "  var wheelEvent = new WheelEvent('wheel', {" +
                    "    deltaY: scrollAmount," +
                    "    deltaMode: 0," +
                    "    bubbles: true," +
                    "    cancelable: true," +
                    "    clientX: " + x + "," +
                    "    clientY: " + y +
                    "  });" +
                    "  if (element) {" +
                    "    element.dispatchEvent(wheelEvent);" +
                    "  }" +
                    "})();";

                engine.executeScript(script);
                needsUpdate.set(true);
                log("simulateScroll: script executed successfully");
            } catch (Exception e) {
                logError("Error simulating scroll: " + e.getMessage(), e);
            }
        });
    }

    /**
     * キー入力イベントをWebViewにシミュレートする。
     * フォーカスされているテキストフィールドに文字を入力する。
     *
     * @param key 入力された文字
     * @param keyCode キーコード
     */
    public void simulateKeyPressed(char key, int keyCode) {
        if (disposed.get()) {
            return;
        }

        Platform.runLater(() -> {
            try {
                // 特殊キーの処理
                String keyName = null;
                if (keyCode == 8) { // Backspace
                    keyName = "Backspace";
                } else if (keyCode == 10 || keyCode == 13) { // Enter
                    keyName = "Enter";
                } else if (keyCode == 9) { // Tab
                    keyName = "Tab";
                } else if (keyCode == 27) { // Escape
                    keyName = "Escape";
                } else if (keyCode == 37) { // Left arrow
                    keyName = "ArrowLeft";
                } else if (keyCode == 38) { // Up arrow
                    keyName = "ArrowUp";
                } else if (keyCode == 39) { // Right arrow
                    keyName = "ArrowRight";
                } else if (keyCode == 40) { // Down arrow
                    keyName = "ArrowDown";
                }

                if (keyName != null) {
                    // 特殊キーの場合はkeydownイベントを送信
                    String script = String.format(
                        "var activeElement = document.activeElement;" +
                        "if (activeElement) {" +
                        "  var event = new KeyboardEvent('keydown', {" +
                        "    key: '%s'," +
                        "    code: '%s'," +
                        "    keyCode: %d," +
                        "    bubbles: true," +
                        "    cancelable: true" +
                        "  });" +
                        "  activeElement.dispatchEvent(event);" +
                        "}",
                        keyName, keyName, keyCode
                    );
                    engine.executeScript(script);
                } else if (key >= 32 && key != 127 && key != 65535) {
                    // 通常文字の場合はinputイベントを送信
                    String charStr = String.valueOf(key).replace("\\", "\\\\").replace("'", "\\'");
                    String script = String.format(
                        "var activeElement = document.activeElement;" +
                        "if (activeElement && (activeElement.tagName === 'INPUT' || activeElement.tagName === 'TEXTAREA' || activeElement.isContentEditable)) {" +
                        "  var start = activeElement.selectionStart || 0;" +
                        "  var end = activeElement.selectionEnd || 0;" +
                        "  var value = activeElement.value || '';" +
                        "  activeElement.value = value.substring(0, start) + '%s' + value.substring(end);" +
                        "  activeElement.selectionStart = activeElement.selectionEnd = start + 1;" +
                        "  var event = new Event('input', { bubbles: true, cancelable: true });" +
                        "  activeElement.dispatchEvent(event);" +
                        "}",
                        charStr
                    );
                    engine.executeScript(script);
                }

                needsUpdate.set(true);
            } catch (Exception e) {
                // キー入力エラーは無視（ログのみ）
                log("Error simulating key press: " + e.getMessage());
            }
        });
    }

    /**
     * 強制的に再描画を要求する。
     * バックグラウンド状態の場合は、OS側で強制的に拒否してGPU使用率を削減する。
     */
    public void requestUpdate() {
        // バックグラウンド状態の場合は更新を拒否（OS側の強制制御）
        if (isInBackground) {
            return;
        }
        needsUpdate.set(true);
    }

    /**
     * バックグラウンド状態を設定する（OS側で制御）。
     * Chromeの「Tab Discarding」と同じ仕組み：Stage自体を隠してレンダリングパイプラインを完全停止。
     * DOMとキャッシュは保持されるため、フォアグラウンド復帰時に即座に再開できる。
     *
     * @param inBackground バックグラウンド状態の場合true
     */
    public void setInBackground(boolean inBackground) {
        this.isInBackground = inBackground;
        log("Background state changed - isInBackground=" + inBackground);
        log("  - disposed=" + disposed.get() + ", stage=" + (stage != null ? "not null" : "null"));

        // Chromeの「Tab Discarding」を実装：Stage自体を隠す
        if (!disposed.get() && stage != null) {
            log("Entering Platform.runLater for Stage visibility control");
            Platform.runLater(() -> {
                log("Inside Platform.runLater - inBackground=" + inBackground);
                try {
                    if (inBackground) {
                        // バックグラウンドに移行：WebViewを非表示にしてレンダリングを停止
                        // 注意：stage.hide()は使用しない（JavaFXイベントループが停止してしまうため）
                        webView.setVisible(false);
                        log("WebView hidden - rendering stopped (Chrome Tab Discarding)");

                        // JavaScript側にもvisibilityイベントを送信
                        engine.executeScript(
                            "(function() {" +
                            "  // requestAnimationFrameを無効化（バックグラウンド専用）" +
                            "  if (!window.__mochiOS_originalRAF) {" +
                            "    window.__mochiOS_originalRAF = window.requestAnimationFrame;" +
                            "    window.__mochiOS_rafIds = [];" +
                            "  }" +
                            "  // すべてのrequestAnimationFrameをキャンセル" +
                            "  window.__mochiOS_rafIds.forEach(function(id) {" +
                            "    window.__mochiOS_originalRAF.call(window, function() {" +
                            "      cancelAnimationFrame(id);" +
                            "    });" +
                            "  });" +
                            "  window.__mochiOS_rafIds = [];" +
                            "  // 新しいrequestAnimationFrameを無効化" +
                            "  window.requestAnimationFrame = function(callback) {" +
                            "    console.log('[MochiOS] requestAnimationFrame blocked (background)');" +
                            "    var id = window.__mochiOS_originalRAF.call(window, function() {});" +
                            "    window.__mochiOS_rafIds.push(id);" +
                            "    return id;" +
                            "  };" +
                            "  // setIntervalを無効化（バックグラウンド専用）" +
                            "  if (!window.__mochiOS_originalSetInterval) {" +
                            "    window.__mochiOS_originalSetInterval = window.setInterval;" +
                            "    window.__mochiOS_intervalIds = [];" +
                            "  }" +
                            "  // すべてのsetIntervalをクリア" +
                            "  window.__mochiOS_intervalIds.forEach(function(id) {" +
                            "    window.__mochiOS_originalSetInterval.call(window, function() {" +
                            "      clearInterval(id);" +
                            "    }, 0);" +
                            "  });" +
                            "  window.__mochiOS_intervalIds = [];" +
                            "  // 新しいsetIntervalを無効化" +
                            "  window.setInterval = function(callback, delay) {" +
                            "    console.log('[MochiOS] setInterval blocked (background)');" +
                            "    return -1;" +
                            "  };" +
                            "  console.log('[MochiOS] App moved to background - all animations and timers stopped');" +
                            "})();"
                        );
                    } else {
                        // フォアグラウンドに復帰：WebViewを再表示してレンダリングを再開
                        webView.setVisible(true);
                        log("WebView shown - rendering resumed (Chrome Tab Restore)");

                        // requestAnimationFrameとタイマーを復元
                        engine.executeScript(
                            "(function() {" +
                            "  // requestAnimationFrameを復元" +
                            "  if (window.__mochiOS_originalRAF) {" +
                            "    window.requestAnimationFrame = window.__mochiOS_originalRAF;" +
                            "    window.__mochiOS_originalRAF = null;" +
                            "    window.__mochiOS_rafIds = [];" +
                            "  }" +
                            "  // setIntervalを復元" +
                            "  if (window.__mochiOS_originalSetInterval) {" +
                            "    window.setInterval = window.__mochiOS_originalSetInterval;" +
                            "    window.__mochiOS_originalSetInterval = null;" +
                            "    window.__mochiOS_intervalIds = [];" +
                            "  }" +
                            "  // visibilityイベントを発火してアプリケーションに再開を通知" +
                            "  Object.defineProperty(document, 'hidden', { value: false, writable: true });" +
                            "  Object.defineProperty(document, 'visibilityState', { value: 'visible', writable: true });" +
                            "  document.dispatchEvent(new Event('visibilitychange'));" +
                            "  console.log('[MochiOS] App moved to foreground - animations and timers restored');" +
                            "})();"
                        );

                        // ★重要★ Stage.show()の直後、次のイベントループでスナップショットを強制更新
                        // JavaFXのレンダリングパイプラインが完全に再開するまで少し時間がかかるため、
                        // 次のイベントループでスナップショット取得を要求することで、確実に最新の描画を取得する
                        Platform.runLater(() -> {
                            try {
                                needsUpdate.set(true);
                                log("Snapshot update requested after stage.show() (next event loop)");

                                // 即座にスナップショットを取得して、キャッシュを更新
                                // これにより、次のrenderToPGraphics()呼び出しで最新の画像が使用される
                                WritableImage snapshot = webView.snapshot(null, null);
                                if (snapshot != null) {
                                    BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);
                                    if (bufferedImage != null) {
                                        cachedBufferedImage = bufferedImage;
                                        if (cachedPImage == null) {
                                            cachedPImage = new PImage(width, height, PApplet.ARGB);
                                        }
                                        convertBufferedImageToPImage(cachedBufferedImage, cachedPImage);
                                        needsUpdate.set(false);
                                        log("Snapshot successfully refreshed after foreground resume");
                                    }
                                }
                            } catch (Exception e) {
                                logError("Failed to refresh snapshot after stage.show(): " + e.getMessage());
                                e.printStackTrace();
                            }
                        });
                    }
                } catch (Exception e) {
                    logError("Failed to control WebView visibility/JavaScript: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * バックグラウンド状態かどうかを取得する。
     *
     * @return バックグラウンド状態の場合true
     */
    public boolean isInBackground() {
        return isInBackground;
    }

    /**
     * WebViewWrapperを破棄する。
     * メモリリークを防ぐため、不要になったら必ず呼び出すこと。
     */
    public void dispose() {
        if (disposed.getAndSet(true)) {
            return; // 既に破棄済み
        }

        Platform.runLater(() -> {
            try {
                engine.load(null);
                log("Disposed");
            } catch (Exception e) {
                logError("Disposal error: " + e.getMessage());
            }
        });

        // キャッシュをクリア
        cachedBufferedImage = null;
        cachedPImage = null;
    }

    public WebEngine getEngine() {
        return engine;
    }

    public WebView getWebView() {
        return webView;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isDisposed() {
        return disposed.get();
    }

    /**
     * WebViewの準備ができているか確認する。
     * HTML読み込みが完了（SUCCEEDED状態）してから、HTMLレンダリングが安定し、
     * かつスナップショットが取得できている場合にtrueを返す。
     *
     * @return 準備完了の場合true
     */
    public boolean isReady() {
        return loadSucceeded && readyFrameCount >= READY_DELAY_FRAMES && cachedPImage != null;
    }

    /**
     * モバイル用Viewportメタタグを注入する。
     * スマートフォンOSとして適切にウェブサイトを表示するため、
     * レスポンシブデザインに対応したViewport設定を行う。
     */
    private void injectMobileViewport() {
        if (disposed.get()) {
            return;
        }

        Platform.runLater(() -> {
            try {
                String script =
                    "(function() {" +
                    "  // 既存のviewportメタタグを確認" +
                    "  var viewport = document.querySelector('meta[name=\"viewport\"]');" +
                    "  if (!viewport) {" +
                    "    // viewportメタタグが存在しない場合は追加" +
                    "    viewport = document.createElement('meta');" +
                    "    viewport.name = 'viewport';" +
                    "    viewport.content = 'width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes';" +
                    "    var head = document.head || document.getElementsByTagName('head')[0];" +
                    "    if (head) {" +
                    "      head.appendChild(viewport);" +
                    "      console.log('[MochiOS] Mobile viewport meta tag injected');" +
                    "    }" +
                    "  } else {" +
                    "    console.log('[MochiOS] Viewport meta tag already exists:', viewport.content);" +
                    "  }" +
                    "  " +
                    "  // モバイル最適化のためのCSSを追加" +
                    "  var style = document.createElement('style');" +
                    "  style.textContent = '" +
                    "    /* MochiOS Mobile Optimization */' +" +
                    "    'body { -webkit-text-size-adjust: 100%; }' +" +
                    "    '* { -webkit-tap-highlight-color: rgba(0,0,0,0.1); }' +" +
                    "    'input, textarea, select { font-size: 16px !important; }' +" + // iOSのズーム防止
                    "  ';" +
                    "  document.head.appendChild(style);" +
                    "  console.log('[MochiOS] Mobile optimization CSS injected');" +
                    "})();";

                engine.executeScript(script);
                needsUpdate.set(true);
                log("Mobile viewport and optimization CSS injected");
            } catch (Exception e) {
                logError("Failed to inject mobile viewport: " + e.getMessage(), e);
            }
        });
    }
}
