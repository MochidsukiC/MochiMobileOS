package jp.moyashi.phoneos.core.apps.chromiumbrowser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumBrowser;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PGraphics;

/**
 * Chromiumブラウザ画面（Phase 1: 基本実装）。
 * モバイルUI最適化版のブラウザ画面。
 *
 * レイアウト:
 * - アドレスバー（上部 70px）
 * - WebViewエリア（中央 470px）
 * - ボトムナビゲーション（下部 60px）
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
    private static final int ADDRESS_BAR_HEIGHT = 70;
    private static final int BOTTOM_NAV_HEIGHT = 60;
    private static final int WEBVIEW_Y = ADDRESS_BAR_HEIGHT;
    private static final int WEBVIEW_HEIGHT = SCREEN_HEIGHT - ADDRESS_BAR_HEIGHT - BOTTOM_NAV_HEIGHT;

    // Chromiumブラウザ
    private ChromiumBrowser browser;

    // UI状態
    private String addressText = "https://www.google.com";
    private boolean addressBarFocused = false;
    private int cursorPosition = 0;

    // 初期化フラグ
    private boolean initialized = false;

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

        // Chromiumブラウザを作成
        try {
            browser = kernel.getChromiumManager().createBrowser(addressText, SCREEN_WIDTH, WEBVIEW_HEIGHT);
            initialized = true;
            log("ChromiumBrowser created successfully");
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
        if (browser != null) {
            pg.pushMatrix();
            pg.translate(0, WEBVIEW_Y);
            browser.drawToPGraphics(pg);
            pg.popMatrix();
        }

        // ボトムナビゲーションを描画
        drawBottomNavigation(pg);
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
        int fieldX = 10;
        int fieldY = 15;
        int fieldW = SCREEN_WIDTH - 70;
        int fieldH = 40;

        // フィールド背景
        if (addressBarFocused) {
            pg.fill(245, 245, 255);
            pg.stroke(100, 100, 255);
        } else {
            pg.fill(245);
            pg.stroke(200);
        }
        pg.strokeWeight(2);
        pg.rect(fieldX, fieldY, fieldW, fieldH, 5);

        // アドレステキスト
        pg.fill(0);
        pg.textAlign(pg.LEFT, pg.CENTER);
        pg.textSize(14);
        String displayText = addressText;
        if (displayText.length() > 40) {
            displayText = displayText.substring(0, 37) + "...";
        }
        pg.text(displayText, fieldX + 10, fieldY + fieldH / 2);

        // 更新ボタン
        int btnX = SCREEN_WIDTH - 55;
        int btnY = 15;
        int btnSize = 40;

        pg.fill(100, 150, 255);
        pg.noStroke();
        pg.rect(btnX, btnY, btnSize, btnSize, 5);

        pg.fill(255);
        pg.textAlign(pg.CENTER, pg.CENTER);
        pg.textSize(20);
        pg.text("↻", btnX + btnSize / 2, btnY + btnSize / 2);
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

        // ボタンサイズ
        int btnSize = 50;
        int spacing = (SCREEN_WIDTH - btnSize * 5) / 6;

        // 戻るボタン
        drawNavButton(pg, spacing, navY + 5, btnSize, "◀", browser != null && browser.canGoBack());

        // 進むボタン
        drawNavButton(pg, spacing * 2 + btnSize, navY + 5, btnSize, "▶", browser != null && browser.canGoForward());

        // ブックマークボタン
        drawNavButton(pg, spacing * 3 + btnSize * 2, navY + 5, btnSize, "☆", true);

        // メニューボタン
        drawNavButton(pg, spacing * 4 + btnSize * 3, navY + 5, btnSize, "≡", true);

        // タブボタン
        drawNavButton(pg, spacing * 5 + btnSize * 4, navY + 5, btnSize, "□", true);
    }

    /**
     * ナビゲーションボタンを描画する。
     */
    private void drawNavButton(PGraphics pg, int x, int y, int size, String icon, boolean enabled) {
        // ボタン背景
        if (enabled) {
            pg.fill(100, 150, 255);
        } else {
            pg.fill(200);
        }
        pg.noStroke();
        pg.rect(x, y, size, size, 5);

        // アイコン
        pg.fill(255);
        pg.textAlign(pg.CENTER, pg.CENTER);
        pg.textSize(24);
        pg.text(icon, x + size / 2, y + size / 2);
    }

    @Override
    public void mousePressed(PGraphics pg, int mouseX, int mouseY) {
        // アドレスバーのクリック判定
        int fieldX = 10;
        int fieldY = 15;
        int fieldW = SCREEN_WIDTH - 70;
        int fieldH = 40;

        if (mouseX >= fieldX && mouseX <= fieldX + fieldW &&
            mouseY >= fieldY && mouseY <= fieldY + fieldH) {
            addressBarFocused = true;
            log("Address bar focused");
            return;
        }

        // 更新ボタンのクリック判定
        int btnX = SCREEN_WIDTH - 55;
        int btnY = 15;
        int btnSize = 40;

        if (mouseX >= btnX && mouseX <= btnX + btnSize &&
            mouseY >= btnY && mouseY <= btnY + btnSize) {
            if (browser != null) {
                browser.reload();
                log("Reload button clicked");
            }
            return;
        }

        // ボトムナビゲーションのクリック判定
        int navY = SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT;
        if (mouseY >= navY) {
            handleBottomNavClick(mouseX - 0, mouseY - navY);
            return;
        }

        // WebViewエリアのクリック
        if (mouseY >= WEBVIEW_Y && mouseY < SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT) {
            addressBarFocused = false;
            if (browser != null) {
                int adjustedY = mouseY - WEBVIEW_Y;
                browser.sendMousePressed(mouseX, adjustedY, 1); // 1 = 左ボタン
            }
        }
    }

    @Override
    public void mouseReleased(PGraphics pg, int mouseX, int mouseY) {
        // WebViewエリアのマウス離し
        if (mouseY >= WEBVIEW_Y && mouseY < SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT) {
            if (browser != null) {
                int adjustedY = mouseY - WEBVIEW_Y;
                browser.sendMouseReleased(mouseX, adjustedY, 1);
            }
        }
    }

    /**
     * マウス移動イベントを処理する。
     */
    public void mouseMoved(PGraphics pg, int mouseX, int mouseY) {
        // WebViewエリアのマウス移動
        if (mouseY >= WEBVIEW_Y && mouseY < SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT) {
            if (browser != null) {
                int adjustedY = mouseY - WEBVIEW_Y;
                browser.sendMouseMoved(mouseX, adjustedY);
            }
        }
    }

    @Override
    public void mouseWheel(PGraphics pg, int mouseX, int mouseY, float delta) {
        // WebViewエリアのマウスホイール
        if (mouseY >= WEBVIEW_Y && mouseY < SCREEN_HEIGHT - BOTTOM_NAV_HEIGHT) {
            if (browser != null) {
                int adjustedY = mouseY - WEBVIEW_Y;
                browser.sendMouseWheel(mouseX, adjustedY, delta * 50);
            }
        }
    }

    @Override
    public void keyPressed(PGraphics pg, char key, int keyCode) {
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
            // WebViewにキーイベントを送信
            if (browser != null) {
                browser.sendKeyPressed(keyCode, key);
            }
        }
    }

    public void keyReleased(PGraphics pg, char key, int keyCode) {
        if (!addressBarFocused && browser != null) {
            browser.sendKeyReleased(keyCode, key);
        }
    }

    /**
     * ボトムナビゲーションのクリックを処理する。
     */
    private void handleBottomNavClick(int x, int y) {
        int btnSize = 50;
        int spacing = (SCREEN_WIDTH - btnSize * 5) / 6;

        // 戻るボタン
        if (x >= spacing && x <= spacing + btnSize) {
            if (browser != null && browser.canGoBack()) {
                browser.goBack();
                log("Go back");
            }
            return;
        }

        // 進むボタン
        if (x >= spacing * 2 + btnSize && x <= spacing * 2 + btnSize * 2) {
            if (browser != null && browser.canGoForward()) {
                browser.goForward();
                log("Go forward");
            }
            return;
        }

        // ブックマークボタン（未実装）
        if (x >= spacing * 3 + btnSize * 2 && x <= spacing * 3 + btnSize * 3) {
            log("Bookmark button clicked (not implemented)");
            return;
        }

        // メニューボタン（未実装）
        if (x >= spacing * 4 + btnSize * 3 && x <= spacing * 4 + btnSize * 4) {
            log("Menu button clicked (not implemented)");
            return;
        }

        // タブボタン（未実装）
        if (x >= spacing * 5 + btnSize * 4 && x <= spacing * 5 + btnSize * 5) {
            log("Tab button clicked (not implemented)");
            return;
        }
    }

    /**
     * URLを読み込む。
     */
    private void loadURL(String url) {
        if (browser == null) {
            return;
        }

        // プロトコルが指定されていない場合、http://を追加
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("httpm://")) {
            url = "http://" + url;
        }

        log("Loading URL: " + url);
        browser.loadURL(url);
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
        if (browser != null) {
            browser.dispose();
            browser = null;
        }
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
