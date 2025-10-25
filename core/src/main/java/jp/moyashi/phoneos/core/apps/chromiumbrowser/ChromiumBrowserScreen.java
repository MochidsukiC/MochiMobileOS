package jp.moyashi.phoneos.core.apps.chromiumbrowser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumBrowser;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.List;

/**
 * Chromiumブラウザ画面（Phase 1: 基本実装）。
 * モバイルUI最適化版のブラウザ画面。
 *
 * レイアウト:
 * - アドレスバー（上部 60px）
 * - WebViewエリア
 * - ボトムナビゲーション（下部 64px）
 *
 * Phase 1実装:
 * - 単一タブ
 * - URL読み込み（http/https/httpm）
 * - 戻る/進む/更新
 * - 基本的なマウス/キーボードイベント
 *
 * @author MochiOS Team
 * @version 1.0 (Phase 1)
 */
public class ChromiumBrowserScreen implements Screen {

    // Kernelインスタンス
    private final Kernel kernel;

    // レイアウト定数
    private static final int SCREEN_WIDTH = 400;
    private static final int SCREEN_HEIGHT = 600;
    private static final int ADDRESS_BAR_HEIGHT = 60;
    private static final int BOTTOM_NAV_HEIGHT = 64;
    private static final int WEBVIEW_Y = ADDRESS_BAR_HEIGHT;
    private static final int WEBVIEW_HEIGHT = SCREEN_HEIGHT - ADDRESS_BAR_HEIGHT - BOTTOM_NAV_HEIGHT;
    private static final int NAV_SQUARE_BUTTON_SIZE = 46;
    private static final int NAV_CIRCLE_BUTTON_DIAMETER = 58;
    private static final String DEFAULT_HOMEPAGE = "https://www.google.com";
    private static final long PROFILE_INTERVAL_NS = 500_000_000L; // 0.5s

    // タブ管理
    private final List<BrowserTab> tabs = new ArrayList<>();
    private int activeTabIndex = -1;
    private boolean tabSwitcherVisible = false;
    private boolean menuVisible = false;
    private int nextTabId = 1;

    // UI状態
    private String addressText = DEFAULT_HOMEPAGE;
    private boolean addressBarFocused = false;

    // 初期化フラグ
    private boolean initialized = false;

    // プロファイル用
    private long profileLastLogNs = System.nanoTime();
    private long profileDrawAccumNs = 0L;
    private long profileCopyAccumNs = 0L;
    private long profileFrames = 0L;
    private long profileCopyFrames = 0L;

    // マウス移動スロットリング用
    private long lastMouseMoveTimeMs = 0L;
    private int pendingMouseX = -1;
    private int pendingMouseY = -1;
    private static final long MOUSE_MOVE_THROTTLE_MS = 50; // 20回/秒

    /**
     * ChromiumBrowserScreenを構築する。
     *
     * @param kernel Kernelインスタンス
     */
    public ChromiumBrowserScreen(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void setup(PGraphics pg) {
        log("Setting up ChromiumBrowserScreen");

        if (kernel.getChromiumManager() == null) {
            logError("ChromiumManager is not initialized");
            return;
        }

        tabs.clear();
        activeTabIndex = -1;
        tabSwitcherVisible = false;
        menuVisible = false;
        nextTabId = 1;

        try {
            BrowserTab initialTab = createTab(addressText, true);
            if (initialTab != null) {
                addressBarFocused = false;
                initialized = true;
                log("ChromiumBrowser initial tab created successfully");
            } else {
                logError("Failed to create initial Chromium tab");
            }
        } catch (Exception e) {
            logError("Failed to create ChromiumBrowser: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void draw(PGraphics pg) {
        if (!initialized) {
            // 初期化エラー画面
            pg.background(255, 0, 0);
            pg.fill(255);
            pg.textAlign(pg.CENTER, pg.CENTER);
            pg.text("Chromiumブラウザの初期化に失敗しました", SCREEN_WIDTH / 2, SCREEN_HEIGHT / 2);
            return;
        }

        // 背景
        pg.background(240);

        // アドレスバーを描画
        drawAddressBar(pg);

        // WebViewエリアを描画
        long drawStartNs = System.nanoTime();
        updateTabMetadata();

        long copyStartNs = System.nanoTime();
        ChromiumBrowser activeBrowser = getActiveBrowser();
        if (activeBrowser != null) {
            // 入力イベントを処理（CEFポンプと並行して実行）
            activeBrowser.flushInputEvents();

            // IMPORTANT: Processing's pg.image() doesn't automatically transfer to main pixel buffer
            // We need to manually copy pixels from browser image to pg.pixels[]
            PImage browserImage = activeBrowser.getUpdatedImage();
            if (browserImage != null) {
                pg.loadPixels();
                browserImage.loadPixels();

                int webviewY = WEBVIEW_Y;
                int copyWidth = Math.min(browserImage.width, SCREEN_WIDTH);
                int copyHeight = Math.min(browserImage.height, WEBVIEW_HEIGHT);

                for (int y = 0; y < copyHeight; y++) {
                    int srcIndex = y * browserImage.width;
                    int dstIndex = (webviewY + y) * SCREEN_WIDTH;
                    if (dstIndex + copyWidth <= pg.pixels.length && srcIndex + copyWidth <= browserImage.pixels.length) {
                        System.arraycopy(browserImage.pixels, srcIndex, pg.pixels, dstIndex, copyWidth);
                    }
                }

                pg.updatePixels();
                profileCopyFrames++;
            }
        }
        long copyDurationNs = System.nanoTime() - copyStartNs;

        // ボトムナビゲーションを描画
        drawBottomNavigation(pg);

        if (tabSwitcherVisible) {
            drawTabSwitcher(pg);
        }
        if (menuVisible) {
            drawMenuOverlay(pg);
        }

        long drawDurationNs = System.nanoTime() - drawStartNs;

        profileDrawAccumNs += drawDurationNs;
        profileCopyAccumNs += copyDurationNs;
        profileFrames++;

        long now = System.nanoTime();
        if (now - profileLastLogNs >= PROFILE_INTERVAL_NS && kernel.getLogger() != null && profileFrames > 0) {
            double avgDrawMs = (double) profileDrawAccumNs / (double) profileFrames / 1_000_000.0;
            double avgCopyMs = profileCopyFrames == 0 ? 0.0 : (double) profileCopyAccumNs / (double) profileCopyFrames / 1_000_000.0;
            kernel.getLogger().debug(
                    "ChromiumBrowserScreen",
                    String.format("profiling: frames=%d avgDraw=%.3fms avgCopy=%.3fms copyFrames=%d",
                            profileFrames, avgDrawMs, avgCopyMs, profileCopyFrames));
            profileLastLogNs = now;
            profileDrawAccumNs = 0L;
            profileCopyAccumNs = 0L;
            profileFrames = 0L;
            profileCopyFrames = 0L;
        }

        // ペンディング中のマウス位置を定期的に送信
        flushPendingMouseMove();
    }

    /**
     * ペンディング中のマウス移動イベントを送信する。
     * スロットリング期間が経過していれば、最後の位置を送信する。
     */
    private void flushPendingMouseMove() {
        if (pendingMouseX >= 0 && pendingMouseY >= 0) {
            long now = System.currentTimeMillis();
            if (now - lastMouseMoveTimeMs >= MOUSE_MOVE_THROTTLE_MS) {
                ChromiumBrowser active = getActiveBrowser();
                if (active != null) {
                    active.sendMouseMoved(pendingMouseX, pendingMouseY);
                    lastMouseMoveTimeMs = now;
                    pendingMouseX = -1;
                    pendingMouseY = -1;
                }
            }
        }
    }

    /**
     * アドレスバーを描画する。
     */
    private void drawAddressBar(PGraphics pg) {
        // 背景
        pg.fill(255);
        pg.noStroke();
        pg.rect(0, 0, SCREEN_WIDTH, ADDRESS_BAR_HEIGHT);

        // アドレス入力フィールド
        int fieldX = 12;
        int fieldY = 14;
        int fieldW = SCREEN_WIDTH - fieldX * 2;
        int fieldH = ADDRESS_BAR_HEIGHT - fieldY * 2;

        if (addressBarFocused) {
            pg.fill(245, 245, 255);
            pg.stroke(100, 140, 255);
        } else {
            pg.fill(245);
            pg.stroke(210);
        }
        pg.strokeWeight(2);
        pg.rect(fieldX, fieldY, fieldW, fieldH, 10);

        // アドレステキスト
        pg.fill(addressText.isEmpty() ? 150 : 0);
        pg.textAlign(pg.LEFT, pg.CENTER);
        pg.textSize(15);
        String displayText = addressText.isEmpty() ? "検索またはURLを入力" : addressText;
        if (!addressText.isEmpty() && displayText.length() > 56) {
            displayText = displayText.substring(0, 53) + "...";
        }
        pg.text(displayText, fieldX + 12, fieldY + fieldH / 2);
    }

    /**
     * ボトムナビゲーションを描画する。
     */
    private void drawBottomNavigation(PGraphics pg) {
        int navY = SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT;

        // 背景
        pg.fill(250);
        pg.noStroke();
        pg.rect(0, navY, SCREEN_WIDTH, BOTTOM_NAV_HEIGHT);

        NavButtonType[] buttons = NavButtonType.values();
        int centerSpacing = SCREEN_WIDTH / (buttons.length + 1);
        int centerY = navY + BOTTOM_NAV_HEIGHT / 2;
        int squareSize = NAV_SQUARE_BUTTON_SIZE;
        int circleSize = NAV_CIRCLE_BUTTON_DIAMETER;
        ChromiumBrowser activeBrowser = getActiveBrowser();
        boolean canGoBack = activeBrowser != null && activeBrowser.canGoBack();
        boolean canGoForward = activeBrowser != null && activeBrowser.canGoForward();

        for (int i = 0; i < buttons.length; i++) {
            int centerX = centerSpacing * (i + 1);
            NavButtonType type = buttons[i];
            switch (type) {
                case BACK:
                    drawSquareButton(pg, centerX, centerY, squareSize, "◀", canGoBack);
                    break;
                case FORWARD:
                    drawSquareButton(pg, centerX, centerY, squareSize, "▶", canGoForward);
                    break;
                case NEW_TAB:
                    drawCircleButton(pg, centerX, centerY, circleSize, "＋");
                    break;
                case TAB_LIST:
                    drawSquareButton(pg, centerX, centerY, squareSize, "□", tabs.size() > 1);
                    drawTabCountBadge(pg, centerX, centerY, squareSize, tabs.size());
                    break;
                case MENU:
                    drawSquareButton(pg, centerX, centerY, squareSize, "⋯", true);
                    break;
            }
        }
    }

    private void drawSquareButton(PGraphics pg, int centerX, int centerY, int size, String icon, boolean enabled) {
        int half = size / 2;
        if (enabled) {
            pg.fill(100, 150, 255);
        } else {
            pg.fill(200);
        }
        pg.noStroke();
        pg.rect(centerX - half, centerY - half, size, size, 8);

        pg.fill(255);
        pg.textAlign(pg.CENTER, pg.CENTER);
        pg.textSize(22);
        pg.text(icon, centerX, centerY);
    }

    private void drawCircleButton(PGraphics pg, int centerX, int centerY, int diameter, String icon) {
        pg.fill(100, 150, 255);
        pg.noStroke();
        pg.circle(centerX, centerY, diameter);

        pg.fill(255);
        pg.textAlign(pg.CENTER, pg.CENTER);
        pg.textSize(28);
        pg.text(icon, centerX, centerY);
    }

    private void drawTabCountBadge(PGraphics pg, int centerX, int centerY, int buttonSize, int tabCount) {
        if (tabCount <= 0) {
            return;
        }

        int badgeWidth = 28;
        int badgeHeight = 22;
        int badgeX = centerX + buttonSize / 2 - badgeWidth + 6;
        int badgeY = centerY - buttonSize / 2 - badgeHeight / 2 + 12;

        pg.fill(255);
        pg.noStroke();
        pg.rect(badgeX, badgeY, badgeWidth, badgeHeight, badgeHeight / 2f);

        pg.fill(70, 80, 200);
        pg.textAlign(pg.CENTER, pg.CENTER);
        pg.textSize(14);
        pg.text(String.valueOf(Math.min(tabCount, 99)), badgeX + badgeWidth / 2f, badgeY + badgeHeight / 2f);
    }

    private void drawTabSwitcher(PGraphics pg) {
        pg.pushStyle();
        pg.fill(0, 160);
        pg.noStroke();
        pg.rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        int cardX = 30;
        int cardWidth = SCREEN_WIDTH - cardX * 2;
        int cardHeight = 70;
        int spacing = 14;
        int startY = ADDRESS_BAR_HEIGHT + 24;

        pg.textAlign(pg.LEFT, pg.TOP);
        for (int i = 0; i < tabs.size(); i++) {
            int cardY = startY + i * (cardHeight + spacing);
            if (cardY + cardHeight > SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT - 16) {
                break;
            }
            BrowserTab tab = tabs.get(i);
            boolean active = (i == activeTabIndex);
            pg.fill(active ? 235 : 250);
            pg.noStroke();
            pg.rect(cardX, cardY, cardWidth, cardHeight, 12);

            pg.fill(60);
            pg.textSize(14);
            pg.text(abbreviate(tab.title, 28), cardX + 16, cardY + 14);

            pg.fill(100);
            pg.textSize(12);
            pg.text(abbreviate(tab.url, 32), cardX + 16, cardY + 40);
        }
        pg.popStyle();
    }

    private void drawMenuOverlay(PGraphics pg) {
        MenuOption[] options = MenuOption.values();
        int optionHeight = 48;
        int optionSpacing = 12;
        int sheetPadding = 20;
        int sheetHeight = sheetPadding * 2 + options.length * optionHeight + (options.length - 1) * optionSpacing;
        int sheetY = SCREEN_HEIGHT - sheetHeight;

        pg.pushStyle();
        pg.fill(0, 160);
        pg.noStroke();
        pg.rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

        pg.fill(255);
        pg.rect(0, sheetY, SCREEN_WIDTH, sheetHeight, 18, 18, 0, 0);

        pg.fill(200);
        pg.rectMode(pg.CENTER);
        pg.rect(SCREEN_WIDTH / 2f, sheetY + 8, 60, 6, 3);
        pg.rectMode(pg.CORNER);

        int optionY = sheetY + sheetPadding;
        for (MenuOption option : options) {
            boolean enabled = isMenuOptionEnabled(option);
            pg.fill(enabled ? 255 : 232);
            pg.rect(20, optionY, SCREEN_WIDTH - 40, optionHeight, 12);
            pg.fill(enabled ? 60 : 150);
            pg.textAlign(pg.LEFT, pg.CENTER);
            pg.textSize(14);
            pg.text(option.getLabel(), 40, optionY + optionHeight / 2f);
            optionY += optionHeight + optionSpacing;
        }
        pg.popStyle();
    }

    private boolean handleTabSwitcherClick(int mouseX, int mouseY) {
        int cardX = 30;
        int cardWidth = SCREEN_WIDTH - cardX * 2;
        int cardHeight = 70;
        int spacing = 14;
        int startY = ADDRESS_BAR_HEIGHT + 24;

        for (int i = 0; i < tabs.size(); i++) {
            int cardY = startY + i * (cardHeight + spacing);
            if (cardY + cardHeight > SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT - 16) {
                break;
            }
            if (mouseX >= cardX && mouseX <= cardX + cardWidth &&
                mouseY >= cardY && mouseY <= cardY + cardHeight) {
                setActiveTabIndex(i);
                tabSwitcherVisible = false;
                addressBarFocused = false;
                return true;
            }
        }

        tabSwitcherVisible = false;
        addressBarFocused = false;
        return true;
    }

    private boolean handleMenuClick(int mouseX, int mouseY) {
        MenuOption[] options = MenuOption.values();
        int optionHeight = 48;
        int optionSpacing = 12;
        int sheetPadding = 20;
        int sheetHeight = sheetPadding * 2 + options.length * optionHeight + (options.length - 1) * optionSpacing;
        int sheetY = SCREEN_HEIGHT - sheetHeight;

        if (mouseY < sheetY) {
            menuVisible = false;
            addressBarFocused = false;
            return true;
        }

        int optionY = sheetY + sheetPadding;
        for (MenuOption option : options) {
            int optTop = optionY;
            int optBottom = optTop + optionHeight;
            if (mouseY >= optTop && mouseY <= optBottom) {
                if (isMenuOptionEnabled(option)) {
                    performMenuOption(option);
                }
                menuVisible = false;
                addressBarFocused = false;
                return true;
            }
            optionY += optionHeight + optionSpacing;
        }

        menuVisible = false;
        addressBarFocused = false;
        return true;
    }

    private boolean isMenuOptionEnabled(MenuOption option) {
        switch (option) {
            case RELOAD:
                return getActiveBrowser() != null;
            case CLOSE_TAB:
                return !tabs.isEmpty();
            case CLOSE_OTHERS:
                return tabs.size() > 1;
            default:
                return true;
        }
    }

    private void performMenuOption(MenuOption option) {
        switch (option) {
            case RELOAD:
                ChromiumBrowser active = getActiveBrowser();
                if (active != null) {
                    active.reload();
                    log("Reload current tab");
                }
                break;
            case CLOSE_TAB:
                closeTab(activeTabIndex);
                log("Closed current tab");
                break;
            case CLOSE_OTHERS:
                closeOtherTabs(activeTabIndex);
                log("Closed other tabs");
                break;
        }
    }

    private BrowserTab createTab(String url, boolean switchTo) {
        String targetUrl = (url == null) ? "" : url;
        try {
            int tabId = nextTabId++;
            ChromiumBrowser newBrowser = kernel.getChromiumManager().createBrowser(targetUrl, SCREEN_WIDTH, WEBVIEW_HEIGHT);
            if (targetUrl != null && !targetUrl.isBlank()) {
                newBrowser.loadURL(targetUrl);
            }
            String title = formatTitleFromUrl(targetUrl);
            if ("新しいタブ".equals(title)) {
                title = "タブ " + tabId;
            }
            BrowserTab tab = new BrowserTab(tabId, newBrowser, targetUrl, title);
            // P2D GPU描画により高フレームレート対応可能
            newBrowser.setFrameRate(60);
            tabs.add(tab);
            if (switchTo) {
                setActiveTabIndex(tabs.size() - 1);
            }
            return tab;
        } catch (Exception e) {
            logError("Failed to create tab: " + e.getMessage());
            return null;
        }
    }

    private void setActiveTabIndex(int index) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }
        if (activeTabIndex == index) {
            return;
        }

        activeTabIndex = index;
        BrowserTab current = tabs.get(activeTabIndex);
        // 動画再生時の遅延対策：フレームレート変更を無効化（常に10FPS）
        refreshAddressFromActiveTab();
        addressBarFocused = false;
        tabSwitcherVisible = false;
        menuVisible = false;
    }

    private void refreshAddressFromActiveTab() {
        BrowserTab tab = getActiveTab();
        if (tab != null) {
            addressText = tab.url == null ? "" : tab.url;
        } else {
            addressText = "";
        }
    }

    private BrowserTab getActiveTab() {
        if (activeTabIndex < 0 || activeTabIndex >= tabs.size()) {
            return null;
        }
        return tabs.get(activeTabIndex);
    }

    private ChromiumBrowser getActiveBrowser() {
        BrowserTab tab = getActiveTab();
        return tab != null ? tab.getBrowser() : null;
    }

    private void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) {
            return;
        }

        BrowserTab removed = tabs.remove(index);
        removed.dispose();

        if (tabs.isEmpty()) {
            BrowserTab fallback = createTab(DEFAULT_HOMEPAGE, true);
            if (fallback != null) {
                addressBarFocused = false;
            }
        } else {
            int newIndex = Math.min(index, tabs.size() - 1);
            setActiveTabIndex(newIndex);
        }
        tabSwitcherVisible = false;
        menuVisible = false;
    }

    private void closeOtherTabs(int keepIndex) {
        if (keepIndex < 0 || keepIndex >= tabs.size()) {
            return;
        }
        for (int i = tabs.size() - 1; i >= 0; i--) {
            if (i == keepIndex) {
                continue;
            }
            BrowserTab tab = tabs.remove(i);
            tab.dispose();
        }
        int newIndex = Math.min(keepIndex, tabs.size() - 1);
        setActiveTabIndex(newIndex);
        tabSwitcherVisible = false;
        menuVisible = false;
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        if (maxLength <= 3) {
            return text.substring(0, maxLength);
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String formatTitleFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return "新しいタブ";
        }
        String trimmed = url.replaceFirst("^(https?://)", "");
        if (trimmed.isBlank()) {
            return "新しいタブ";
        }
        if (trimmed.length() > 40) {
            trimmed = trimmed.substring(0, 40);
        }
        return trimmed;
    }

    private void updateTabMetadata() {
        for (BrowserTab tab : tabs) {
            ChromiumBrowser cb = tab.getBrowser();
            if (cb == null) {
                continue;
            }
            String current = cb.getCurrentURL();
            if (current != null && !current.equals(tab.url)) {
                tab.setUrl(current);
                tab.setTitle(formatTitleFromUrl(current));
            }
        }

        if (!addressBarFocused) {
            BrowserTab active = getActiveTab();
            if (active != null) {
                addressText = active.url == null ? "" : active.url;
            }
        }
    }

    @Override
    public void mousePressed(PGraphics pg, int mouseX, int mouseY) {
        if (tabSwitcherVisible) {
            if (handleTabSwitcherClick(mouseX, mouseY)) {
                return;
            }
        }

        if (menuVisible) {
            if (handleMenuClick(mouseX, mouseY)) {
                return;
            }
        }

        // アドレスバーのクリック判定
        int fieldX = 12;
        int fieldY = 14;
        int fieldW = SCREEN_WIDTH - fieldX * 2;
        int fieldH = ADDRESS_BAR_HEIGHT - fieldY * 2;

        if (mouseX >= fieldX && mouseX <= fieldX + fieldW &&
            mouseY >= fieldY && mouseY <= fieldY + fieldH) {
            addressBarFocused = true;
            tabSwitcherVisible = false;
            menuVisible = false;
            log("Address bar focused");
            return;
        }

        // ボトムナビゲーションのクリック判定
        int navY = SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT;
        if (mouseY >= navY) {
            handleBottomNavClick(mouseX, mouseY - navY);
            return;
        }

        // WebViewエリアのクリック
        if (mouseY >= WEBVIEW_Y && mouseY < SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT) {
            addressBarFocused = false;
            ChromiumBrowser active = getActiveBrowser();
            if (active != null) {
                int adjustedY = mouseY - WEBVIEW_Y;
                active.sendMousePressed(mouseX, adjustedY, 1); // 1 = 左ボタン
            }
        }
    }

    @Override
    public void mouseReleased(PGraphics pg, int mouseX, int mouseY) {
        // WebViewエリアのマウス離し
        if (mouseY >= WEBVIEW_Y && mouseY < SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT) {
            ChromiumBrowser active = getActiveBrowser();
            if (active != null) {
                int adjustedY = mouseY - WEBVIEW_Y;
                active.sendMouseReleased(mouseX, adjustedY, 1);
            }
        }
    }

    /**
     * マウス移動イベントを処理する。
     * スロットリング: 50ms間隔（20回/秒）でのみ送信し、イベント蓄積を防ぐ。
     */
    public void mouseMoved(PGraphics pg, int mouseX, int mouseY) {
        if (tabSwitcherVisible || menuVisible) {
            return;
        }
        // WebViewエリアのマウス移動
        if (mouseY >= WEBVIEW_Y && mouseY < SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT) {
            ChromiumBrowser active = getActiveBrowser();
            if (active != null) {
                int adjustedY = mouseY - WEBVIEW_Y;

                // スロットリング: 50ms間隔でのみ送信
                long now = System.currentTimeMillis();
                if (now - lastMouseMoveTimeMs >= MOUSE_MOVE_THROTTLE_MS) {
                    active.sendMouseMoved(mouseX, adjustedY);
                    lastMouseMoveTimeMs = now;
                    pendingMouseX = -1; // 送信完了
                    pendingMouseY = -1;
                } else {
                    // 次回送信用に最新位置を保存
                    pendingMouseX = mouseX;
                    pendingMouseY = adjustedY;
                }
            }
        }
    }

    @Override
    public void mouseWheel(PGraphics pg, int mouseX, int mouseY, float delta) {
        if (tabSwitcherVisible || menuVisible) {
            return;
        }
        // WebViewエリアのマウスホイール
        if (mouseY >= WEBVIEW_Y && mouseY < SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT) {
            ChromiumBrowser active = getActiveBrowser();
            if (active != null) {
                int adjustedY = mouseY - WEBVIEW_Y;
                active.sendMouseWheel(mouseX, adjustedY, delta * 50);
            }
        }
    }

    @Override
    public void keyPressed(PGraphics pg, char key, int keyCode) {
        if (tabSwitcherVisible || menuVisible) {
            return;
        }
        if (addressBarFocused) {
            // アドレスバー入力処理
            if (keyCode == 10) { // Enter
                loadURL(addressText);
                addressBarFocused = false;
            } else if (keyCode == 8) { // Backspace
                if (addressText.length() > 0) {
                    addressText = addressText.substring(0, addressText.length() - 1);
                }
            } else if (key >= 32 && key < 127) {
                addressText += key;
            }
        } else {
            // キーイベントバイパス: すべてのキーイベントをChromiumブラウザに転送
            ChromiumBrowser active = getActiveBrowser();
            if (active != null) {
                boolean isCtrlPressed = kernel.isCtrlPressed();

                // デバッグログ出力（主要なキーのみ）
                if (isCtrlPressed) {
                    // Ctrl+ショートカット
                    switch (keyCode) {
                        case 67:  // C - Copy
                        case 86:  // V - Paste
                        case 88:  // X - Cut
                        case 65:  // A - Select All
                        case 90:  // Z - Undo
                        case 89:  // Y - Redo
                        case 70:  // F - Find
                        case 82:  // R - Reload
                            log("Bypassing control key to Chromium: Ctrl+" + (char)keyCode);
                            break;
                    }
                } else {
                    // 基本的なキー（スペース、バックスペース、エンター）のログ
                    switch (keyCode) {
                        case 32:  // Space
                            log("Bypassing Space to Chromium (keyChar=" + (int)key + ")");
                            break;
                        case 8:   // Backspace
                            log("Bypassing Backspace to Chromium (keyChar=" + (int)key + ")");
                            break;
                        case 10:  // Enter
                            log("Bypassing Enter to Chromium (keyChar=" + (int)key + ")");
                            break;
                    }
                }

                // すべてのキーイベントをブラウザに転送
                active.sendKeyPressed(keyCode, key);
            }
        }
    }

    public void keyReleased(PGraphics pg, char key, int keyCode) {
        if (tabSwitcherVisible || menuVisible) {
            return;
        }
        ChromiumBrowser active = getActiveBrowser();
        if (!addressBarFocused && active != null) {
            active.sendKeyReleased(keyCode, key);
        }
    }

    /**
     * このスクリーンにフォーカスされたテキスト入力コンポーネントがあるかチェック。
     * スペースキー処理の前にKernelから呼び出される。
     *
     * Chromiumブラウザでは、アドレスバーまたはWebView内のテキストフィールドに
     * フォーカスがある可能性があるため、常にtrueを返す。
     * これにより、スペースキーがホームボタンとして処理されず、
     * Chromiumブラウザに正しく転送される。
     *
     * @return 常にtrue（Chromiumブラウザがアクティブな時はテキスト入力を優先）
     */
    @Override
    public boolean hasFocusedComponent() {
        // アドレスバーにフォーカスがある場合
        if (addressBarFocused) {
            return true;
        }

        // Webページ内のテキスト入力フィールドにフォーカスがある場合
        // CefTextInputHandlerによって検出される
        ChromiumBrowser activeBrowser = getActiveBrowser();
        if (activeBrowser != null && activeBrowser.hasTextInputFocus()) {
            return true;
        }

        // どちらでもない場合は、スペースキーでホーム画面に戻る
        return false;
    }

    /**
     * ボトムナビゲーションのクリックを処理する。
     */
    private void handleBottomNavClick(int x, int y) {
        NavButtonType hit = detectNavButton(x, y);
        if (hit == null) {
            return;
        }

        addressBarFocused = false;

        switch (hit) {
            case BACK:
                ChromiumBrowser activeBack = getActiveBrowser();
                if (activeBack != null && activeBack.canGoBack()) {
                    activeBack.goBack();
                    log("Go back");
                }
                break;
            case FORWARD:
                ChromiumBrowser activeForward = getActiveBrowser();
                if (activeForward != null && activeForward.canGoForward()) {
                    activeForward.goForward();
                    log("Go forward");
                }
                break;
            case NEW_TAB:
                createTab(DEFAULT_HOMEPAGE, true);
                addressText = "";
                addressBarFocused = true;
                tabSwitcherVisible = false;
                menuVisible = false;
                log("New tab created");
                break;
            case TAB_LIST:
                if (tabs.size() > 1) {
                    tabSwitcherVisible = !tabSwitcherVisible;
                    if (tabSwitcherVisible) {
                        menuVisible = false;
                        addressBarFocused = false;
                    }
                } else {
                    log("Only one tab open");
                }
                break;
            case MENU:
                menuVisible = !menuVisible;
                if (menuVisible) {
                    tabSwitcherVisible = false;
                    addressBarFocused = false;
                }
                break;
        }
    }

    private NavButtonType detectNavButton(int relativeX, int relativeY) {
        if (relativeY < 0 || relativeY > BOTTOM_NAV_HEIGHT) {
            return null;
        }

        NavButtonType[] buttons = NavButtonType.values();
        int centerSpacing = SCREEN_WIDTH / (buttons.length + 1);
        int centerY = BOTTOM_NAV_HEIGHT / 2;
        int squareHalf = NAV_SQUARE_BUTTON_SIZE / 2;
        int circleRadius = NAV_CIRCLE_BUTTON_DIAMETER / 2;

        for (int i = 0; i < buttons.length; i++) {
            int centerX = centerSpacing * (i + 1);
            NavButtonType type = buttons[i];
            if (type == NavButtonType.NEW_TAB) {
                int dx = relativeX - centerX;
                int dy = relativeY - centerY;
                if (dx * dx + dy * dy <= circleRadius * circleRadius) {
                    return type;
                }
            } else {
                if (relativeX >= centerX - squareHalf && relativeX <= centerX + squareHalf &&
                    relativeY >= centerY - squareHalf && relativeY <= centerY + squareHalf) {
                    return type;
                }
            }
        }
        return null;
    }

    private enum NavButtonType {
        BACK,
        FORWARD,
        NEW_TAB,
        TAB_LIST,
        MENU
    }

    private enum MenuOption {
        RELOAD("再読み込み"),
        CLOSE_TAB("タブを閉じる"),
        CLOSE_OTHERS("他のタブを閉じる");

        private final String label;

        MenuOption(String label) {
            this.label = label;
        }

        String getLabel() {
            return label;
        }
    }

    private static class BrowserTab {
        private final int id;
        private final ChromiumBrowser browser;
        private String url;
        private String title;

        BrowserTab(int id, ChromiumBrowser browser, String url, String title) {
            this.id = id;
            this.browser = browser;
            this.url = url;
            this.title = title;
        }

        ChromiumBrowser getBrowser() {
            return browser;
        }

        void setUrl(String url) {
            this.url = url;
        }

        void setTitle(String title) {
            this.title = title;
        }

        void dispose() {
            if (browser != null) {
                browser.dispose();
            }
        }
    }

    /**
     * URLを読み込む。
     */
    private void loadURL(String url) {
        ChromiumBrowser active = getActiveBrowser();
        if (active == null) {
            return;
        }

        // プロトコルが指定されていない場合、http://を追加
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("httpm://")) {
            url = "http://" + url;
        }

        log("Loading URL: " + url);
        BrowserTab tab = getActiveTab();
        if (tab != null) {
            tab.setUrl(url);
            tab.setTitle(formatTitleFromUrl(url));
        }
        addressText = url;
        addressBarFocused = false;
        tabSwitcherVisible = false;
        menuVisible = false;
        active.loadURL(url);
    }

    @Override
    public void onBackground() {
        log("ChromiumBrowserScreen moved to background");
    }

    @Override
    public void onForeground() {
        log("ChromiumBrowserScreen moved to foreground");
    }

    @Override
    public void cleanup(PGraphics pg) {
        log("ChromiumBrowserScreen cleaned up");
        for (BrowserTab tab : tabs) {
            tab.dispose();
        }
        tabs.clear();
        activeTabIndex = -1;
        tabSwitcherVisible = false;
        menuVisible = false;
        initialized = false;
        nextTabId = 1;
        addressText = DEFAULT_HOMEPAGE;
        addressBarFocused = false;
    }

    /**
     * ログ出力（INFO）。
     */
    private void log(String message) {
        System.out.println("[ChromiumBrowserScreen] " + message);
        if (kernel.getLogger() != null) {
            kernel.getLogger().info("ChromiumBrowserScreen", message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        System.err.println("[ChromiumBrowserScreen] " + message);
        if (kernel.getLogger() != null) {
            kernel.getLogger().error("ChromiumBrowserScreen", message);
        }
    }
}
