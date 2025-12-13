package jp.moyashi.phoneos.core.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumSurface;
import jp.moyashi.phoneos.core.service.chromium.ChromiumTextInput;
import jp.moyashi.phoneos.core.ui.components.TextInputProtocol;
import jp.moyashi.phoneos.core.ui.theme.ThemeContext;
import jp.moyashi.phoneos.core.ui.theme.ThemeEngine;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.Optional;

/**
 * HTML/CSS/JSを使用したUIを持つアプリケーション画面の基底クラス。
 * 外部アプリケーション開発者向けの高機能画面クラス。
 *
 * <p>使用例:</p>
 * <pre>
 * // アプリケーションクラスから使用
 * public class MyApp implements IApplication {
 *     public Screen getEntryScreen(Kernel kernel) {
 *         return WebScreen.create(this, "ui/index.html");
 *     }
 * }
 * </pre>
 *
 * <p>機能:</p>
 * <ul>
 *   <li>スマートファクトリ: 最低限の記述でHTML UIをロード</li>
 *   <li>Chromium統合: JCEF/MCEFによるHTML/CSS/JSレンダリング</li>
 *   <li>入力イベント転送: マウス・キーボードイベントの自動ブリッジ</li>
 *   <li>テキスト入力統合: OS統一クリップボード管理対応</li>
 * </ul>
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class WebScreen implements Screen {

    private final Kernel kernel;
    private final String modId;
    private final String resourcePath;
    private final ClassLoader classLoader;

    private int contentWidth;
    private int contentHeight;
    private boolean initialized = false;

    /**
     * WebScreenを構築する（内部用）。
     *
     * @param kernel Kernelインスタンス
     * @param modId Mod ID（スキーム名に使用）
     * @param resourcePath リソースパス（assets/modId/以下の相対パス）
     * @param classLoader リソース読み込み用ClassLoader
     */
    private WebScreen(Kernel kernel, String modId, String resourcePath, ClassLoader classLoader) {
        this.kernel = kernel;
        this.modId = modId;
        this.resourcePath = resourcePath;
        this.classLoader = classLoader;
    }

    /**
     * WebScreenを作成する（呼び出し元からModIDとClassLoaderを自動取得）。
     *
     * <p>使用例:</p>
     * <pre>
     * // IApplicationのgetEntryScreenから呼び出し
     * return WebScreen.create(this, "ui/index.html");
     * </pre>
     *
     * @param caller 呼び出し元オブジェクト（通常はIApplication実装クラス）
     * @param resourcePath リソースパス（assets/modId/以下の相対パス）
     * @return WebScreenインスタンス
     */
    public static WebScreen create(Object caller, String resourcePath) {
        // 呼び出し元のクラス名からModIDを推測
        String className = caller.getClass().getName();
        String modId = extractModId(className);
        ClassLoader classLoader = caller.getClass().getClassLoader();

        // Kernelは後でsetup時に取得する必要がある
        return new WebScreen(null, modId, resourcePath, classLoader);
    }

    /**
     * WebScreenを作成する（ModIDを明示的に指定）。
     *
     * <p>使用例:</p>
     * <pre>
     * return WebScreen.create(kernel, "mymod", "ui/index.html");
     * </pre>
     *
     * @param kernel Kernelインスタンス
     * @param modId Mod ID（スキーム名に使用）
     * @param resourcePath リソースパス（assets/modId/以下の相対パス）
     * @return WebScreenインスタンス
     */
    public static WebScreen create(Kernel kernel, String modId, String resourcePath) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = WebScreen.class.getClassLoader();
        }
        return new WebScreen(kernel, modId, resourcePath, classLoader);
    }

    /**
     * WebScreenを作成する（Kernel、ModID、ClassLoaderを明示的に指定）。
     *
     * @param kernel Kernelインスタンス
     * @param modId Mod ID（スキーム名に使用）
     * @param resourcePath リソースパス（assets/modId/以下の相対パス）
     * @param classLoader リソース読み込み用ClassLoader
     * @return WebScreenインスタンス
     */
    public static WebScreen create(Kernel kernel, String modId, String resourcePath, ClassLoader classLoader) {
        return new WebScreen(kernel, modId, resourcePath, classLoader);
    }

    /**
     * クラス名からModIDを推測する。
     * 例: "com.example.mymod.MyApp" -> "mymod"
     */
    private static String extractModId(String className) {
        String[] parts = className.split("\\.");
        if (parts.length >= 3) {
            // com.example.mymod.MyApp -> mymod
            return parts[parts.length - 2].toLowerCase();
        } else if (parts.length >= 2) {
            return parts[parts.length - 2].toLowerCase();
        }
        return "webapp";
    }

    /**
     * Kernelを設定する（create(Object, String)で作成した場合に必要）。
     *
     * @param kernel Kernelインスタンス
     * @return this（メソッドチェーン用）
     */
    public WebScreen withKernel(Kernel kernel) {
        return new WebScreen(kernel, this.modId, this.resourcePath, this.classLoader);
    }

    @Override
    public void setup(PGraphics p) {
        log("setup() called - PGraphics size: " + p.width + "x" + p.height);

        // コンテンツエリアのサイズ（画面全体を使用）
        contentWidth = p.width;
        contentHeight = p.height;

        // ChromiumServiceが利用可能か確認
        if (kernel == null) {
            logError("Kernel is null - WebScreen requires a Kernel instance");
            return;
        }

        if (kernel.getChromiumService() == null) {
            logError("ChromiumService is not available");
            return;
        }

        // URLを構築
        String url = buildUrl();
        log("Target URL: " + url);

        // デバッグ用：通常のHTTPS URLでテスト
        // String testUrl = "https://www.google.com";
        // log("DEBUG: Using test URL: " + testUrl);

        // タブを作成
        ChromiumSurface surface = kernel.getChromiumService().createTab(contentWidth, contentHeight, url);
        initialized = true;

        // カスタムスキームURLは即座にリソースを読み込むため、
        // ブラウザのGLCanvasが初期化される前にレンダリングが開始される
        // 遅延してからURLを再読み込みすることで正しいサイズでレンダリングさせる
        if (surface != null) {
            final String targetUrl = url;
            new Thread(() -> {
                try {
                    // GLCanvasの初期化を待つ（Hidden JFrame設定完了後）
                    Thread.sleep(500);
                    log("Reloading URL after GLCanvas initialization: " + targetUrl);
                    surface.reload();
                } catch (InterruptedException e) {
                    logError("Reload interrupted: " + e.getMessage());
                }
            }, "WebScreen-Reload").start();
        }
    }

    /**
     * リソースパスからURLを構築する。
     */
    private String buildUrl() {
        // mochiapp://modid/path 形式
        return "mochiapp://" + modId + "/" + resourcePath;
    }

    /**
     * アクティブなサーフェスを取得する（ChromiumBrowserScreenと同じ方法）。
     */
    private Optional<ChromiumSurface> getActiveSurface() {
        if (kernel != null && kernel.getChromiumService() != null) {
            return kernel.getChromiumService().getActiveSurface();
        }
        return Optional.empty();
    }

    @Override
    public void draw(PGraphics g) {
        ThemeEngine theme = ThemeContext.getTheme();
        if (theme == null) return;

        // 背景を描画
        int appBg = theme.colorBackground();
        g.background((appBg >> 16) & 0xFF, (appBg >> 8) & 0xFF, appBg & 0xFF);

        // Chromiumサーフェスを描画（ChromiumBrowserScreenと同じ方法）
        Optional<ChromiumSurface> activeSurfaceOpt = getActiveSurface();
        if (activeSurfaceOpt.isPresent()) {
            PImage frame = activeSurfaceOpt.get().acquireFrame();
            if (frame != null) {
                g.image(frame, 0, 0);
            }
        } else {
            // サーフェスがない場合のフォールバック表示
            g.fill(theme.colorSurface());
            g.rect(10, 10, g.width - 20, g.height - 20, 8);
            g.fill(theme.colorOnSurface());
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(16);
            g.text("Loading...", g.width / 2f, g.height / 2f);
        }
    }

    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        if (!initialized) return;
        getActiveSurface().ifPresent(s -> s.sendMousePressed(mouseX, mouseY, 1));
    }

    @Override
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        if (!initialized) return;
        getActiveSurface().ifPresent(s -> s.sendMouseReleased(mouseX, mouseY, 1));
    }

    @Override
    public void mouseMoved(PGraphics g, int mouseX, int mouseY) {
        if (!initialized) return;
        getActiveSurface().ifPresent(s -> s.sendMouseMoved(mouseX, mouseY));
    }

    @Override
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        if (!initialized) return;
        getActiveSurface().ifPresent(s -> s.sendMouseDragged(mouseX, mouseY, 1));
    }

    @Override
    public void mouseWheel(PGraphics g, int x, int y, float delta) {
        if (!initialized) return;
        getActiveSurface().ifPresent(s -> s.sendMouseWheel(x, y, delta * 10));
    }

    @Override
    public void keyPressed(PGraphics g, char key, int keyCode) {
        if (!initialized || kernel == null) return;
        boolean shiftPressed = kernel.isShiftPressed();
        boolean ctrlPressed = kernel.isCtrlPressed();
        boolean altPressed = kernel.isAltPressed();
        boolean metaPressed = kernel.isMetaPressed();
        getActiveSurface().ifPresent(s -> s.sendKeyPressed(keyCode, key, shiftPressed, ctrlPressed, altPressed, metaPressed));
    }

    @Override
    public void keyReleased(PGraphics g, char key, int keyCode) {
        if (!initialized || kernel == null) return;
        boolean shiftPressed = kernel.isShiftPressed();
        boolean ctrlPressed = kernel.isCtrlPressed();
        boolean altPressed = kernel.isAltPressed();
        boolean metaPressed = kernel.isMetaPressed();
        getActiveSurface().ifPresent(s -> s.sendKeyReleased(keyCode, key, shiftPressed, ctrlPressed, altPressed, metaPressed));
    }

    @Override
    public void cleanup(PGraphics p) {
        log("cleanup() called");

        getActiveSurface().ifPresent(surface -> {
            if (kernel != null && kernel.getChromiumService() != null) {
                kernel.getChromiumService().closeTab(surface.getSurfaceId());
            }
        });

        initialized = false;
    }

    @Override
    public String getScreenTitle() {
        if (!initialized) {
            return "WebApp";
        }
        return getActiveSurface()
                .map(ChromiumSurface::getTitle)
                .filter(title -> title != null && !title.isEmpty())
                .orElse("WebApp");
    }

    @Override
    public boolean hasFocusedComponent() {
        // WebScreenでは常にtrueを返す（スペースキーをHTML側に転送）
        return true;
    }

    @Override
    public TextInputProtocol getFocusedTextInput() {
        if (!initialized) {
            return null;
        }
        return getActiveSurface()
                .map(ChromiumTextInput::new)
                .orElse(null);
    }

    @Override
    public void setModifierKeys(boolean shift, boolean ctrl) {
        // 修飾キー状態はkeyPressed/keyReleasedで直接取得するため、ここでは何もしない
    }

    @Override
    public void onForeground() {
        log("onForeground()");
    }

    @Override
    public void onBackground() {
        log("onBackground()");
    }

    // ========== 追加ユーティリティメソッド ==========

    /**
     * 現在のURLを取得する。
     *
     * @return 現在のURL、サーフェスがない場合はnull
     */
    public String getCurrentUrl() {
        return getActiveSurface().map(ChromiumSurface::getCurrentUrl).orElse(null);
    }

    /**
     * 指定したURLにナビゲートする。
     *
     * @param url ナビゲート先URL
     */
    public void loadUrl(String url) {
        getActiveSurface().ifPresent(s -> s.loadUrl(url));
    }

    /**
     * ページを再読み込みする。
     */
    public void reload() {
        getActiveSurface().ifPresent(ChromiumSurface::reload);
    }

    /**
     * JavaScriptを実行する。
     *
     * @param script 実行するJavaScriptコード
     */
    public void executeScript(String script) {
        getActiveSurface().ifPresent(s -> s.executeScript(script));
    }

    /**
     * Mod IDを取得する。
     *
     * @return Mod ID
     */
    public String getModId() {
        return modId;
    }

    /**
     * リソースパスを取得する。
     *
     * @return リソースパス
     */
    public String getResourcePath() {
        return resourcePath;
    }

    /**
     * ClassLoaderを取得する。
     *
     * @return ClassLoader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    // ========== ログ出力 ==========

    private void log(String message) {
        System.out.println("[WebScreen:" + modId + "] " + message);
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().info("WebScreen:" + modId, message);
        }
    }

    private void logError(String message) {
        System.err.println("[WebScreen:" + modId + "] " + message);
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("WebScreen:" + modId, message);
        }
    }
}
