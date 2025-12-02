package jp.moyashi.phoneos.core.apps.chromiumbrowser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumSurface;
import jp.moyashi.phoneos.core.service.chromium.ChromiumTextInput;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.components.Button;
import jp.moyashi.phoneos.core.ui.components.TextField;
import jp.moyashi.phoneos.core.ui.components.TextInputProtocol;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.Optional;

public class ChromiumBrowserScreen implements Screen {

    private final Kernel kernel;
    private String initialUrl = "https://www.google.com"; // Default URL

    private Button backButton;
    private Button forwardButton;
    private Button reloadButton;
    private Button newTabButton;
    private Button tabListButton;
    private Button bookmarkButton;
    private Button menuButton;
    private TextField addressBar;

    public ChromiumBrowserScreen(Kernel kernel) {
        this.kernel = kernel;
    }

    public ChromiumBrowserScreen(Kernel kernel, String url) {
        this.kernel = kernel;
        this.initialUrl = url;
    }

    @Override
    public void setup(PGraphics p) {
        initializeUI(p);
        
        // Create initial tab if no tabs exist
        if (kernel != null && kernel.getChromiumService() != null && kernel.getChromiumService().getSurfaces().isEmpty()) {
            kernel.getChromiumService().createTab(p.width - 20, p.height - 120, initialUrl);
        }
    }

    private void initializeUI(PGraphics p) {
        // Bottom bar buttons
        backButton = new Button(20, 555, 40, 40, "<");
        forwardButton = new Button(80, 555, 40, 40, ">");
        newTabButton = new Button(p.width / 2f - 20, 555, 40, 40, "+");
        tabListButton = new Button(p.width - 120, 555, 40, 40, "□");
        menuButton = new Button(p.width - 60, 555, 40, 40, "...");

        // Top bar button
        reloadButton = new Button(p.width - 60, 5, 40, 40, "R");
        bookmarkButton = new Button(p.width - 110, 5, 40, 40, "☆");

        // Address bar
        addressBar = new TextField(10, 5, p.width - 130, 40, "Enter URL");
        addressBar.setVisible(false);

        // Set click listeners
        backButton.setOnClickListener(() -> getActiveBrowserSurface().ifPresent(ChromiumSurface::goBack));
        forwardButton.setOnClickListener(() -> getActiveBrowserSurface().ifPresent(ChromiumSurface::goForward));
        reloadButton.setOnClickListener(() -> getActiveBrowserSurface().ifPresent(ChromiumSurface::reload));
        bookmarkButton.setOnClickListener(() -> {
             getActiveBrowserSurface().ifPresent(s -> {
                 var dm = kernel.getChromiumService().getBrowserDataManager();
                 if (dm.isBookmarked(s.getCurrentUrl())) {
                     dm.removeBookmark(s.getCurrentUrl());
                 } else {
                     dm.addBookmark(s.getTitle(), s.getCurrentUrl());
                 }
             });
        });
        
        newTabButton.setOnClickListener(() -> {
            if (kernel != null && kernel.getChromiumService() != null) {
                kernel.getChromiumService().createTab(p.width - 20, p.height - 120, "https://www.google.com");
            }
        });
        tabListButton.setOnClickListener(() -> {
            if (kernel != null && kernel.getScreenManager() != null) {
                kernel.getScreenManager().pushScreen(new TabListScreen(kernel));
            }
        });
        menuButton.setOnClickListener(() -> {
             if (kernel != null && kernel.getScreenManager() != null) {
                 kernel.getScreenManager().pushScreen(new BrowserMenuScreen(kernel));
             }
        });
    }

    @Override
    public void draw(PGraphics g) {
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme == null) return;

        Optional<ChromiumSurface> activeSurfaceOpt = getActiveBrowserSurface();

        // Update button states
        if (activeSurfaceOpt.isPresent()) {
            ChromiumSurface activeSurface = activeSurfaceOpt.get();
            backButton.setEnabled(activeSurface.canGoBack());
            forwardButton.setEnabled(activeSurface.canGoForward());
        } else {
            backButton.setEnabled(false);
            forwardButton.setEnabled(false);
        }

        // Pass modifier keys to address bar if it's focused
        if (addressBar.isFocused()) {
            addressBar.setShiftPressed(kernel.isShiftPressed());
            addressBar.setCtrlPressed(kernel.isCtrlPressed());
        }

        // Draw App Background
        int appBg = theme.colorBackground();
        g.background((appBg >> 16) & 0xFF, (appBg >> 8) & 0xFF, appBg & 0xFF);

        // Draw Top Address Bar (holographic)
        drawTopBar(g, theme);

        // Draw Content Area
        drawContentArea(g, theme);

        // Draw Bottom Navigation Bar (holographic)
        drawBottomBar(g, theme);
    }

    private void drawTopBar(PGraphics g, jp.moyashi.phoneos.core.ui.theme.ThemeEngine theme) {
        int surfaceColor = theme.colorSurface();
        int onSurfaceColor = theme.colorOnSurface();
        g.fill((surfaceColor >> 16) & 0xFF, (surfaceColor >> 8) & 0xFF, surfaceColor & 0xFF, 180);
        g.noStroke();
        g.rect(0, 0, g.width, 50);

        if (addressBar.isFocused()) {
            addressBar.draw(g);
        } else {
            g.fill(onSurfaceColor);
            g.textAlign(g.LEFT, g.CENTER);
            g.textSize(14);
            String title = getActiveBrowserSurface().map(ChromiumSurface::getTitle).orElse("No Active Tab");
            g.text("🔒 " + title, 20, 25);
        }

        if (reloadButton != null) reloadButton.draw(g);
        if (bookmarkButton != null) {
             // Update bookmark state
             if (getActiveBrowserSurface().isPresent()) {
                 String url = getActiveBrowserSurface().get().getCurrentUrl();
                 boolean isBookmarked = kernel.getChromiumService().getBrowserDataManager().isBookmarked(url);
                 bookmarkButton.setText(isBookmarked ? "★" : "☆");
             }
             bookmarkButton.draw(g);
        }
        if (newTabButton != null) newTabButton.draw(g);
        if (tabListButton != null) tabListButton.draw(g);
    }

    private void drawContentArea(PGraphics g, jp.moyashi.phoneos.core.ui.theme.ThemeEngine theme) {
        Optional<ChromiumSurface> activeSurfaceOpt = getActiveBrowserSurface();
        if (activeSurfaceOpt.isPresent()) {
            PImage frame = activeSurfaceOpt.get().acquireFrame();
            if (frame != null) {
                g.image(frame, 10, 60);
            }
        } else {
            g.fill(theme.colorSurface());
            g.rect(10, 60, g.width - 20, g.height - 120, 8);
            g.fill(theme.colorOnSurface());
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(20);
            g.text("No Active Tab", g.width / 2, g.height / 2);
        }
    }

    private void drawBottomBar(PGraphics g, jp.moyashi.phoneos.core.ui.theme.ThemeEngine theme) {
        int surfaceColor = theme.colorSurface();
        g.fill((surfaceColor >> 16) & 0xFF, (surfaceColor >> 8) & 0xFF, surfaceColor & 0xFF, 180);
        g.noStroke();
        g.rect(0, g.height - 50, g.width, 50);

        if (backButton != null) backButton.draw(g);
        if (forwardButton != null) forwardButton.draw(g);
        if (newTabButton != null) newTabButton.draw(g);
        if (tabListButton != null) tabListButton.draw(g);
        if (menuButton != null) menuButton.draw(g);
    }

    @Override
    public void cleanup(PGraphics p) {
        // All surfaces are destroyed by ChromiumService on shutdown
    }

    @Override
    public String getScreenTitle() {
        return "Chromium Browser";
    }

    @Override
    public void onForeground() {}

    @Override
    public void onBackground() {}

    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        if (backButton.onMousePressed(mouseX, mouseY)) return;
        if (forwardButton.onMousePressed(mouseX, mouseY)) return;
        if (reloadButton.onMousePressed(mouseX, mouseY)) return;
        if (bookmarkButton.onMousePressed(mouseX, mouseY)) return;
        if (newTabButton.onMousePressed(mouseX, mouseY)) return;
        if (tabListButton.onMousePressed(mouseX, mouseY)) return;
        if (menuButton.onMousePressed(mouseX, mouseY)) return;

        // アドレスバー領域をクリックした場合、フォーカスを設定
        if (!addressBar.isFocused() && mouseX > 10 && mouseX < g.width - 120 && mouseY > 5 && mouseY < 45) {
            addressBar.setVisible(true);
            addressBar.setFocused(true);
            getActiveBrowserSurface().ifPresent(s -> addressBar.setText(s.getCurrentUrl()));
            return;
        }

        // アドレスバーがフォーカスされている場合、クリックイベントを転送
        if (addressBar.isFocused() && addressBar.onMousePressed(mouseX, mouseY)) return;

        // アドレスバーの外側をクリックした場合、フォーカスを解除
        if (addressBar.isFocused() && !addressBar.contains(mouseX, mouseY)) {
            System.out.println("[ChromiumBrowserScreen] Clicked outside addressBar, clearing focus");
            addressBar.setVisible(false);
            addressBar.setFocused(false);
        }

        // Forward mouse press to Chromium if in content area
        // Button: 1=左, 2=中, 3=右 (Processing convention)
        if (isInContentArea(g, mouseX, mouseY)) {
            System.out.println("[ChromiumBrowserScreen] Clicked in content area, forwarding to Chromium");
            getActiveBrowserSurface().ifPresent(s -> s.sendMousePressed(mouseX - 10, mouseY - 60, 1));
        }
    }

    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        if (backButton.onMouseReleased(mouseX, mouseY)) return;
        if (forwardButton.onMouseReleased(mouseX, mouseY)) return;
        if (reloadButton.onMouseReleased(mouseX, mouseY)) return;
        if (bookmarkButton.onMouseReleased(mouseX, mouseY)) return;
        if (newTabButton.onMouseReleased(mouseX, mouseY)) return;
        if (tabListButton.onMouseReleased(mouseX, mouseY)) return;
        if (menuButton.onMouseReleased(mouseX, mouseY)) return;
        if (addressBar.isFocused()) addressBar.onMouseReleased(mouseX, mouseY);

        // Forward mouse release to Chromium if in content area
        // Button: 1=左, 2=中, 3=右 (Processing convention)
        if (isInContentArea(g, mouseX, mouseY)) {
            getActiveBrowserSurface().ifPresent(s -> s.sendMouseReleased(mouseX - 10, mouseY - 60, 1));
        }
    }
    
    public void mouseMoved(PGraphics g, int mouseX, int mouseY) {
        backButton.onMouseMoved(mouseX, mouseY);
        forwardButton.onMouseMoved(mouseX, mouseY);
        reloadButton.onMouseMoved(mouseX, mouseY);
        bookmarkButton.onMouseMoved(mouseX, mouseY);
        newTabButton.onMouseMoved(mouseX, mouseY);
        tabListButton.onMouseMoved(mouseX, mouseY);
        menuButton.onMouseMoved(mouseX, mouseY);
        if (addressBar.isFocused()) addressBar.onMouseMoved(mouseX, mouseY);

        // Forward mouse move to Chromium if in content area
        if (isInContentArea(g, mouseX, mouseY)) {
            getActiveBrowserSurface().ifPresent(s -> s.sendMouseMoved(mouseX - 10, mouseY - 60));
        }
    }

    @Override
    public void mouseWheel(PGraphics g, int x, int y, float delta) {
        // Forward wheel events to browser if in content area
        // Processing delta: positive is down (scroll down content), negative is up
        // Amplify delta for better scrolling (ChromiumProvider casts to int)
        if (isInContentArea(g, x, y)) {
            getActiveBrowserSurface().ifPresent(s -> s.sendMouseWheel(x - 10, y - 60, delta * 10));
        }
    }

    @Override
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        // UI components don't have onMouseDragged, so just forward to Chromium
        if (addressBar.isFocused()) {
            // If address bar is focused, let it handle text selection dragging
            // Address bar uses onMousePressed and onMouseMoved for selection
        }

        // Forward mouse drag to Chromium if in content area
        // Button: 1=左, 2=中, 3=右 (Processing convention)
        if (isInContentArea(g, mouseX, mouseY)) {
            getActiveBrowserSurface().ifPresent(s -> s.sendMouseDragged(mouseX - 10, mouseY - 60, 1));
        }
    }

    public void keyPressed(PGraphics g, char key, int keyCode) {
        System.out.println("[ChromiumBrowserScreen] keyPressed: key=" + (int)key + ", keyCode=" + keyCode +
                           ", addressBarFocused=" + addressBar.isFocused());

        // アドレスバーがフォーカスされている場合、アドレスバーにイベントを転送
        if (addressBar.isFocused()) {
            if (keyCode == PApplet.ENTER || keyCode == PApplet.RETURN) {
                // ENTERキーでURL読み込み
                String url = addressBar.getText();
                // Convert Intent URL to fallback URL if needed
                url = convertIntentUrl(url);
                final String finalUrl = url;
                getActiveBrowserSurface().ifPresent(s -> s.loadUrl(finalUrl));
                addressBar.setFocused(false);
                addressBar.setVisible(false);
            } else {
                System.out.println("[ChromiumBrowserScreen] Forwarding to addressBar");
                addressBar.onKeyPressed(key, keyCode);
            }
        } else {
            // アドレスバーがフォーカスされていない場合、Chromiumに送信
            boolean shiftPressed = kernel != null && kernel.isShiftPressed();
            boolean ctrlPressed = kernel != null && kernel.isCtrlPressed();
            boolean altPressed = kernel != null && kernel.isAltPressed();
            boolean metaPressed = kernel != null && kernel.isMetaPressed();
            System.out.println("[ChromiumBrowserScreen] Sending to Chromium: shift=" + shiftPressed +
                               ", ctrl=" + ctrlPressed + ", alt=" + altPressed + ", meta=" + metaPressed);
            Optional<ChromiumSurface> surface = getActiveBrowserSurface();
            if (surface.isPresent()) {
                System.out.println("[ChromiumBrowserScreen] Active surface found, calling sendKeyPressed");
                surface.get().sendKeyPressed(keyCode, key, shiftPressed, ctrlPressed, altPressed, metaPressed);
            } else {
                System.out.println("[ChromiumBrowserScreen] No active surface!");
            }
        }
    }

    public void keyReleased(PGraphics g, char key, int keyCode) {
        System.out.println("[ChromiumBrowserScreen] keyReleased: key=" + (int)key + ", keyCode=" + keyCode + ", addressBarFocused=" + addressBar.isFocused());

        // アドレスバーがフォーカスされている場合はChromiumに送信しない
        // BaseTextInputにはonKeyReleasedがないため、フォーカス時は何もしない
        if (addressBar.isFocused()) {
            System.out.println("[ChromiumBrowserScreen] Address bar focused, ignoring keyReleased");
            return;
        }

        // アドレスバーがフォーカスされていない場合、Chromiumに送信
        boolean shiftPressed = kernel != null && kernel.isShiftPressed();
        boolean ctrlPressed = kernel != null && kernel.isCtrlPressed();
        boolean altPressed = kernel != null && kernel.isAltPressed();
        boolean metaPressed = kernel != null && kernel.isMetaPressed();
        System.out.println("[ChromiumBrowserScreen] Sending keyReleased to Chromium: shift=" + shiftPressed + ", ctrl=" + ctrlPressed + ", alt=" + altPressed + ", meta=" + metaPressed);
        getActiveBrowserSurface().ifPresent(s -> s.sendKeyReleased(keyCode, key, shiftPressed, ctrlPressed, altPressed, metaPressed));
    }

    private Optional<ChromiumSurface> getActiveBrowserSurface() {
        if (kernel != null && kernel.getChromiumService() != null) {
            return kernel.getChromiumService().getActiveSurface();
        }
        return Optional.empty();
    }

    /**
     * コンテンツエリア（Chromiumブラウザ表示領域）内かどうかをチェック
     * コンテンツエリア: (10, 60) から (width - 10, height - 60) まで
     */
    private boolean isInContentArea(PGraphics g, int x, int y) {
        return x >= 10 && x < g.width - 10 && y >= 60 && y < g.height - 60;
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
                    System.out.println("ChromiumBrowserScreen: Converted Intent URL to fallback: " + fallbackUrl);
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
            System.out.println("ChromiumBrowserScreen: Converted Intent URL to: " + convertedUrl);
            return convertedUrl;

        } catch (Exception e) {
            System.err.println("ChromiumBrowserScreen: Failed to convert Intent URL: " + e.getMessage());
            e.printStackTrace();
            return url; // Return original URL if conversion fails
        }
    }

    /**
     * OSのフォーカス管理システムとの統合。
     * アドレスバーがフォーカスされている場合、キーボードイベントはアドレスバーに送られる。
     * フォーカスされていない場合、キーボードイベントはChromiumブラウザに送られる。
     *
     * @return アドレスバーがフォーカスされている場合true
     */
    @Override
    public boolean hasFocusedComponent() {
        return addressBar != null && addressBar.isFocused();
    }

    /**
     * 修飾キー状態をアドレスバーに伝える。
     * これにより、アドレスバーでShift+矢印キーによる選択やCtrl+A等が正しく動作する。
     *
     * @param shift Shiftキーが押されているか
     * @param ctrl Ctrlキーが押されているか
     */
    @Override
    public void setModifierKeys(boolean shift, boolean ctrl) {
        if (addressBar != null) {
            // BaseTextInputは内部でshiftPressed/ctrlPressedフィールドを持つ
            // ただし、public setterがないため、現在の実装では直接設定できない
            // 将来的にBaseTextInputにpublic setterを追加するか、
            // または各キーイベントで修飾キー状態が自動的に反映される
        }
    }

    /**
     * 現在フォーカスされているTextInputProtocolを返す。
     * iOS UITextInputの統一インターフェースに相当。
     * OS側でCtrl+C/V/X/Aを統一的に処理するために使用される。
     *
     * 優先順位:
     * 1. アドレスバーがフォーカスされている場合: addressBar (TextField/BaseTextInput)
     * 2. Chromiumコンテンツ内のテキスト入力にフォーカスがある場合: ChromiumTextInput
     * 3. それ以外: null
     *
     * @return フォーカスされているTextInputProtocol、なければnull
     */
    @Override
    public TextInputProtocol getFocusedTextInput() {
        // 1. アドレスバーがフォーカスされている場合
        if (addressBar != null && addressBar.isFocused()) {
            if (kernel != null && kernel.getLogger() != null) {
                kernel.getLogger().debug("ChromiumBrowserScreen", "getFocusedTextInput: addressBar focused");
            }
            return addressBar;
        }

        // 2. Chromiumコンテンツがアクティブな場合
        // iOS/Android方式: フォーカス検出に頼らず、常にChromiumTextInputを返す
        // YouTube等のカスタム要素でもJavaScript経由で操作可能
        Optional<ChromiumSurface> activeSurface = getActiveBrowserSurface();
        if (activeSurface.isPresent()) {
            ChromiumSurface surface = activeSurface.get();
            if (kernel != null && kernel.getLogger() != null) {
                kernel.getLogger().debug("ChromiumBrowserScreen", "getFocusedTextInput: returning ChromiumTextInput (always-on mode)");
            }
            return new ChromiumTextInput(surface);
        }

        // 3. アクティブなサーフェスなし
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().debug("ChromiumBrowserScreen", "getFocusedTextInput: no active surface");
        }
        return null;
    }
}