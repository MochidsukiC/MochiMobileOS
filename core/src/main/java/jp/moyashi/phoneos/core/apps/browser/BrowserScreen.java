package jp.moyashi.phoneos.core.apps.browser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;
import jp.moyashi.phoneos.core.service.webview.WebViewWrapper;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.components.Button;
import jp.moyashi.phoneos.core.ui.components.Label;
import jp.moyashi.phoneos.core.ui.components.TextField;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Webブラウザ画面。
 * UIコンポーネント（アドレスバー、ナビゲーションボタン）とWebViewを統合。
 */
public class BrowserScreen implements Screen {
    private final Kernel kernel;
    private WebViewWrapper webViewWrapper;
    private boolean initialized = false;
    private int loadingFrame = 0;

    // UIコンポーネント
    private TextField addressBar;
    private Button backButton;
    private Button forwardButton;
    private Button refreshButton;
    private Button goButton;
    private Label statusLabel;

    // 履歴管理
    private final List<String> history = new ArrayList<>();
    private int currentHistoryIndex = -1;

    // レイアウト設定
    private static final int NAV_BAR_HEIGHT = 90;
    private static final int BUTTON_SIZE = 35;
    private static final int MARGIN = 5;
    private static final int WEBVIEW_Y = NAV_BAR_HEIGHT + 5;

    // ローディング状態
    private boolean isLoading = false;
    private String loadingUrl = "";

    // httpm プロトコルの正規表現パターン
    private static final Pattern HTTPM_PATTERN = Pattern.compile("^httpm://([0-3]-[0-9a-fA-F-]+)(/.*)?$");

    // バックグラウンド状態
    private boolean isInBackground = false;

    // マウス位置の追跡（マウス移動イベント用）
    private int lastMouseX = -1;
    private int lastMouseY = -1;

    // 修飾キーの状態
    private boolean ctrlPressed = false;
    private boolean shiftPressed = false;

    public BrowserScreen(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void setup(PGraphics pg) {
        if (initialized) {
            return;
        }

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("BrowserScreen", "ブラウザ画面を初期化中...");
        }

        // WebViewManagerが利用可能か確認
        if (kernel.getWebViewManager() == null || !kernel.getWebViewManager().isInitialized()) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("BrowserScreen", "WebViewManagerが初期化されていません");
            }
            return;
        }

        try {
            // WebViewを作成（ナビゲーションバーの下に表示）
            int webViewWidth = pg.width;
            int webViewHeight = pg.height - WEBVIEW_Y;
            webViewWrapper = kernel.getWebViewManager().createWebView(webViewWidth, webViewHeight);

            if (webViewWrapper != null) {
                // UIコンポーネントを作成
                createUIComponents(pg);

                // 仮想HTTPレスポンスハンドラを登録
                if (kernel.getVirtualRouter() != null) {
                    kernel.getVirtualRouter().registerTypeHandler(
                            VirtualPacket.PacketType.GENERIC_RESPONSE,
                            this::onVirtualHttpResponse
                    );
                }

                // デフォルトページを読み込み
                loadURL("about:blank");

                // アドレスバーに初期フォーカスを設定
                addressBar.setFocused(true);

                initialized = true;
                if (kernel.getLogger() != null) {
                    kernel.getLogger().info("BrowserScreen", "ブラウザ画面の初期化が完了しました");
                }
            } else {
                if (kernel.getLogger() != null) {
                    kernel.getLogger().error("BrowserScreen", "WebViewの作成に失敗しました");
                }
            }
        } catch (Exception e) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("BrowserScreen", "初期化に失敗しました", e);
            }
        }
    }

    /**
     * UIコンポーネントを作成する
     */
    private void createUIComponents(PGraphics pg) {
        int y = MARGIN;
        int x = MARGIN;

        // 戻るボタン
        backButton = new Button(x, y, BUTTON_SIZE, BUTTON_SIZE, "<");
        backButton.setOnClickListener(this::goBack);
        x += BUTTON_SIZE + MARGIN;

        // 進むボタン
        forwardButton = new Button(x, y, BUTTON_SIZE, BUTTON_SIZE, ">");
        forwardButton.setOnClickListener(this::goForward);
        x += BUTTON_SIZE + MARGIN;

        // 更新ボタン
        refreshButton = new Button(x, y, BUTTON_SIZE, BUTTON_SIZE, "⟳");
        refreshButton.setOnClickListener(this::refresh);
        x += BUTTON_SIZE + MARGIN;

        // アドレスバー（残りの幅 - 移動ボタンの幅）
        int addressBarWidth = pg.width - x - BUTTON_SIZE - MARGIN * 2;
        addressBar = new TextField(x, y, addressBarWidth, BUTTON_SIZE, "URLを入力...");
        addressBar.setFont(kernel.getJapaneseFont());
        x += addressBarWidth + MARGIN;

        // 移動ボタン
        goButton = new Button(x, y, BUTTON_SIZE, BUTTON_SIZE, "→");
        goButton.setOnClickListener(this::navigateToAddressBarUrl);

        // ステータスラベル
        statusLabel = new Label(MARGIN, y + BUTTON_SIZE + MARGIN, pg.width - MARGIN * 2, 30, "準備完了");
        statusLabel.setFont(kernel.getJapaneseFont());
        statusLabel.setTextSize(12);
        statusLabel.setTextColor(100);
    }

    @Override
    public void draw(PGraphics pg) {
        if (!initialized) {
            drawFallback(pg);
            return;
        }

        // 背景
        pg.background(240);

        // ナビゲーションバーの背景
        pg.fill(255);
        pg.noStroke();
        pg.rect(0, 0, pg.width, NAV_BAR_HEIGHT);

        // UIコンポーネントを描画
        backButton.draw(pg);
        forwardButton.draw(pg);
        refreshButton.draw(pg);
        addressBar.draw(pg);
        goButton.draw(pg);
        statusLabel.draw(pg);

        // 戻る/進むボタンの有効/無効状態を更新
        backButton.setEnabled(currentHistoryIndex > 0);
        forwardButton.setEnabled(currentHistoryIndex < history.size() - 1);


        // WebViewを描画（ナビゲーションバーの下）
        if (webViewWrapper != null && !webViewWrapper.isDisposed()) {
            // スリープ中またはバックグラウンド状態の場合は、WebViewの更新を停止
            if (kernel.isSleeping() || isInBackground) {
                return;
            }

            try {
                // WebViewの準備ができていない場合
                if (!webViewWrapper.isReady()) {
                    // スナップショット取得を進める
                    webViewWrapper.renderToPGraphics(pg);
                    // ローディング画面を描画
                    drawLoadingScreen(pg);
                    loadingFrame++;
                } else {
                    // HTML側の動的な変更（JavaScript、アニメーション等）を反映するため、
                    // 毎フレーム更新を要求する
                    webViewWrapper.requestUpdate();

                    // WebViewをオフセット位置に描画
                    pg.pushMatrix();
                    pg.translate(0, WEBVIEW_Y);
                    webViewWrapper.renderToPGraphics(pg);
                    pg.popMatrix();
                }
            } catch (Exception e) {
                if (kernel.getLogger() != null) {
                    kernel.getLogger().error("BrowserScreen", "描画エラー", e);
                }
                drawFallback(pg);
            }
        }
    }

    /**
     * WebViewが利用できない場合のフォールバック表示
     */
    private void drawFallback(PGraphics pg) {
        pg.background(50);
        pg.fill(255, 0, 0);
        pg.textAlign(processing.core.PApplet.CENTER, processing.core.PApplet.CENTER);
        pg.textSize(16);
        pg.text("ブラウザを初期化できませんでした", pg.width / 2, pg.height / 2 - 20);
        pg.textSize(12);
        pg.text("WebViewManagerが利用できません", pg.width / 2, pg.height / 2 + 10);
    }

    /**
     * ローディング画面を描画
     */
    private void drawLoadingScreen(PGraphics pg) {
        // WebView表示エリアにローディング画面を描画
        pg.pushMatrix();
        pg.translate(0, WEBVIEW_Y);

        // 背景
        pg.fill(30);
        pg.noStroke();
        pg.rect(0, 0, pg.width, pg.height - WEBVIEW_Y);

        // スピナー
        int centerX = pg.width / 2;
        int centerY = (pg.height - WEBVIEW_Y) / 2;
        int spinnerRadius = 30;
        int dotCount = 8;
        float rotation = (loadingFrame * 0.05f) % (processing.core.PApplet.TWO_PI);

        pg.pushMatrix();
        pg.translate(centerX, centerY);
        pg.rotate(rotation);

        for (int i = 0; i < dotCount; i++) {
            float angle = (processing.core.PApplet.TWO_PI / dotCount) * i;
            float x = processing.core.PApplet.cos(angle) * spinnerRadius;
            float y = processing.core.PApplet.sin(angle) * spinnerRadius;

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
        pg.text("読み込み中...", centerX, centerY + 60);

        // URL表示
        if (!loadingUrl.isEmpty()) {
            pg.textSize(12);
            pg.fill(150);
            pg.text(loadingUrl, centerX, centerY + 85);
        }

        pg.popMatrix();
    }

    /**
     * URLを読み込む
     */
    private void loadURL(String url) {
        if (!initialized || webViewWrapper == null || webViewWrapper.isDisposed()) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("BrowserScreen", "WebViewが初期化されていません");
            }
            return;
        }

        // 空のURLは無視
        if (url == null || url.trim().isEmpty()) {
            return;
        }

        url = url.trim();

        // プロトコルが指定されていない場合、http:// を追加
        if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("httpm://") && !url.equals("about:blank")) {
            url = "http://" + url;
        }

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("BrowserScreen", "URLを読み込み中: " + url);
        }

        // ローディング状態を設定
        isLoading = true;
        loadingUrl = url;
        loadingFrame = 0;

        // ステータスを更新
        updateStatus("読み込み中: " + url);

        // プロトコルに応じて処理を分岐
        if (url.startsWith("httpm://")) {
            // 仮想ネットワークから取得
            loadHttpmUrl(url);
        } else if (url.startsWith("http://") || url.startsWith("https://") || url.equals("about:blank")) {
            // 実際のウェブサイトを読み込み
            webViewWrapper.loadURL(url);
            addToHistory(url);
            addressBar.setText(url);
        } else {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("BrowserScreen", "サポートされていないプロトコル: " + url);
            }
            updateStatus("エラー: サポートされていないプロトコル");
            isLoading = false;
        }
    }

    /**
     * httpm:// プロトコルのURLを読み込む
     */
    private void loadHttpmUrl(String url) {
        Matcher matcher = HTTPM_PATTERN.matcher(url);
        if (!matcher.matches()) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("BrowserScreen", "無効なhttpm URLフォーマット: " + url);
            }
            updateStatus("エラー: 無効なhttpm URLフォーマット");
            isLoading = false;
            return;
        }

        String ipvmAddressStr = matcher.group(1);
        String path = matcher.group(2);
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        try {
            IPvMAddress destination = IPvMAddress.fromString(ipvmAddressStr);

            // 仮想HTTPリクエストを送信
            sendVirtualHttpRequest(destination, path, url);

        } catch (Exception e) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("BrowserScreen", "httpm URL解析エラー: " + url, e);
            }
            updateStatus("エラー: httpm URL解析失敗");
            isLoading = false;
        }
    }

    /**
     * 仮想HTTPリクエストを送信
     */
    private void sendVirtualHttpRequest(IPvMAddress destination, String path, String originalUrl) {
        if (kernel.getVirtualRouter() == null) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("BrowserScreen", "VirtualRouterが利用できません");
            }
            updateStatus("エラー: VirtualRouterが利用できません");
            isLoading = false;
            return;
        }

        // 送信元アドレス（自分のアドレス）を取得
        IPvMAddress source = IPvMAddress.forPlayer(kernel.getSIMInfo().getOwnerUUID());

        // HTTPリクエストパケットを構築
        VirtualPacket request = VirtualPacket.builder()
                .source(source)
                .destination(destination)
                .type(VirtualPacket.PacketType.GENERIC_REQUEST)
                .put("method", "GET")
                .put("path", path)
                .put("protocol", "HTTP/1.1")
                .put("originalUrl", originalUrl)
                .build();

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("BrowserScreen", "仮想HTTPリクエストを送信: " + destination + path);
        }

        // パケットを送信
        kernel.getVirtualRouter().sendPacket(request);
    }

    /**
     * 仮想HTTPレスポンスを受信
     */
    private void onVirtualHttpResponse(VirtualPacket packet) {
        // 自分宛てのレスポンスかチェック
        String myUuid = kernel.getSIMInfo().getOwnerUUID();
        if (!packet.getDestination().getUUID().equals(myUuid)) {
            return;
        }

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("BrowserScreen", "仮想HTTPレスポンスを受信: " + packet.getSource());
        }

        // HTMLコンテンツを取得
        String html = packet.getString("html");
        String status = packet.getString("status");
        String originalUrl = packet.getString("originalUrl");

        if (html != null && !html.isEmpty()) {
            // HTMLをWebViewに読み込み
            webViewWrapper.loadContent(html);
            addToHistory(originalUrl != null ? originalUrl : "httpm://" + packet.getSource());
            addressBar.setText(originalUrl != null ? originalUrl : "httpm://" + packet.getSource());
            updateStatus("読み込み完了");
        } else {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("BrowserScreen", "レスポンスにHTMLが含まれていません");
            }
            updateStatus("エラー: コンテンツが見つかりません (ステータス: " + status + ")");

            // エラーページを表示
            String errorHtml = generateErrorPage("404 Not Found", "要求されたページが見つかりません。");
            webViewWrapper.loadContent(errorHtml);
        }

        isLoading = false;
    }

    /**
     * エラーページのHTMLを生成
     */
    private String generateErrorPage(String title, String message) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"ja\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + title + "</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            font-family: sans-serif;\n" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
                "            display: flex;\n" +
                "            justify-content: center;\n" +
                "            align-items: center;\n" +
                "            min-height: 100vh;\n" +
                "            margin: 0;\n" +
                "            color: white;\n" +
                "        }\n" +
                "        .error-container {\n" +
                "            text-align: center;\n" +
                "            padding: 40px;\n" +
                "        }\n" +
                "        h1 {\n" +
                "            font-size: 3em;\n" +
                "            margin-bottom: 20px;\n" +
                "        }\n" +
                "        p {\n" +
                "            font-size: 1.2em;\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"error-container\">\n" +
                "        <h1>" + title + "</h1>\n" +
                "        <p>" + message + "</p>\n" +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
    }

    /**
     * 履歴に追加
     */
    private void addToHistory(String url) {
        // 現在の履歴位置より後ろの履歴を削除
        if (currentHistoryIndex < history.size() - 1) {
            history.subList(currentHistoryIndex + 1, history.size()).clear();
        }

        // 新しいURLを追加
        history.add(url);
        currentHistoryIndex = history.size() - 1;

        if (kernel.getLogger() != null) {
            kernel.getLogger().debug("BrowserScreen", "履歴に追加: " + url + " (インデックス: " + currentHistoryIndex + ")");
        }
    }

    /**
     * ステータスを更新
     */
    private void updateStatus(String status) {
        if (statusLabel != null) {
            statusLabel.setText(status);
        }
    }

    /**
     * 戻るボタンのハンドラ
     */
    private void goBack() {
        if (currentHistoryIndex > 0) {
            currentHistoryIndex--;
            String url = history.get(currentHistoryIndex);
            loadURL(url);
            if (kernel.getLogger() != null) {
                kernel.getLogger().info("BrowserScreen", "履歴を戻る: " + url);
            }
        }
    }

    /**
     * 進むボタンのハンドラ
     */
    private void goForward() {
        if (currentHistoryIndex < history.size() - 1) {
            currentHistoryIndex++;
            String url = history.get(currentHistoryIndex);
            loadURL(url);
            if (kernel.getLogger() != null) {
                kernel.getLogger().info("BrowserScreen", "履歴を進む: " + url);
            }
        }
    }

    /**
     * 更新ボタンのハンドラ
     */
    private void refresh() {
        if (currentHistoryIndex >= 0 && currentHistoryIndex < history.size()) {
            String url = history.get(currentHistoryIndex);
            // 履歴には追加せずに再読み込み
            if (kernel.getLogger() != null) {
                kernel.getLogger().info("BrowserScreen", "ページを更新: " + url);
            }

            // 現在のURLを履歴から削除してから再読み込み（重複を避けるため）
            history.remove(currentHistoryIndex);
            currentHistoryIndex--;
            loadURL(url);
        }
    }

    /**
     * アドレスバーのURLに移動
     */
    private void navigateToAddressBarUrl() {
        String url = addressBar.getText();
        if (url != null && !url.trim().isEmpty()) {
            loadURL(url);
        }
    }

    @Override
    public void mousePressed(PGraphics pg, int mouseX, int mouseY) {
        // マウス位置を記録
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        // UIコンポーネントのイベント処理
        backButton.onMousePressed(mouseX, mouseY);
        forwardButton.onMousePressed(mouseX, mouseY);
        refreshButton.onMousePressed(mouseX, mouseY);

        // アドレスバーのクリック処理とフォーカス管理
        boolean addressBarClicked = addressBar.onMousePressed(mouseX, mouseY);
        if (addressBarClicked) {
            // アドレスバーがクリックされた場合、フォーカスを設定
            addressBar.setFocused(true);
        } else {
            // 他の場所がクリックされた場合、フォーカスを解除
            addressBar.setFocused(false);
        }

        goButton.onMousePressed(mouseX, mouseY);

        // WebViewエリアのクリック（ナビゲーションバーより下）
        if (initialized && webViewWrapper != null && !webViewWrapper.isDisposed() && mouseY >= WEBVIEW_Y) {
            int adjustedY = mouseY - WEBVIEW_Y;
            // マウス移動イベントを送信してホバー効果を更新
            webViewWrapper.simulateMouseMoved(mouseX, adjustedY);
            webViewWrapper.simulateMousePressed(mouseX, adjustedY);
        }
    }

    @Override
    public void mouseReleased(PGraphics pg, int mouseX, int mouseY) {
        // マウス位置を記録
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        // UIコンポーネントのイベント処理
        backButton.onMouseReleased(mouseX, mouseY);
        forwardButton.onMouseReleased(mouseX, mouseY);
        refreshButton.onMouseReleased(mouseX, mouseY);
        addressBar.onMouseReleased(mouseX, mouseY);
        goButton.onMouseReleased(mouseX, mouseY);

        // WebViewエリアのクリック（ナビゲーションバーより下）
        if (initialized && webViewWrapper != null && !webViewWrapper.isDisposed() && mouseY >= WEBVIEW_Y) {
            int adjustedY = mouseY - WEBVIEW_Y;
            // マウス移動イベントを送信してホバー効果を更新
            webViewWrapper.simulateMouseMoved(mouseX, adjustedY);
            webViewWrapper.simulateMouseReleased(mouseX, adjustedY);
            webViewWrapper.simulateMouseClick(mouseX, adjustedY);
        }
    }

    @Override
    public void mouseDragged(PGraphics pg, int mouseX, int mouseY) {
        // マウス位置を記録
        lastMouseX = mouseX;
        lastMouseY = mouseY;

        // UIコンポーネントのイベント処理
        addressBar.onMouseDragged(mouseX, mouseY);

        // WebViewエリアのドラッグ（マウス移動として扱う）
        if (initialized && webViewWrapper != null && !webViewWrapper.isDisposed() && mouseY >= WEBVIEW_Y) {
            int adjustedY = mouseY - WEBVIEW_Y;
            webViewWrapper.simulateMouseMoved(mouseX, adjustedY);
        }
    }

    @Override
    public void mouseWheel(PGraphics pg, int mouseX, int mouseY, float delta) {
        if (kernel.getLogger() != null) {
            kernel.getLogger().debug("BrowserScreen", "mouseWheel called: mouseY=" + mouseY + ", delta=" + delta + ", WEBVIEW_Y=" + WEBVIEW_Y);
        }

        // WebViewエリアのスクロール（ナビゲーションバーより下）
        if (initialized && webViewWrapper != null && !webViewWrapper.isDisposed() && mouseY >= WEBVIEW_Y) {
            int adjustedY = mouseY - WEBVIEW_Y;
            // スクロール量を調整（deltaが正の値：下スクロール、負の値：上スクロール）
            float scrollAmount = delta * 50;

            if (kernel.getLogger() != null) {
                kernel.getLogger().debug("BrowserScreen", "Scrolling WebView: adjustedY=" + adjustedY + ", scrollAmount=" + scrollAmount);
            }

            webViewWrapper.simulateScroll(mouseX, adjustedY, scrollAmount);
        } else {
            if (kernel.getLogger() != null) {
                kernel.getLogger().debug("BrowserScreen", "Scroll ignored: initialized=" + initialized +
                    ", webViewWrapper=" + (webViewWrapper != null) +
                    ", disposed=" + (webViewWrapper != null && webViewWrapper.isDisposed()) +
                    ", mouseY >= WEBVIEW_Y=" + (mouseY >= WEBVIEW_Y));
            }
        }
    }

    @Override
    public void tick() {
        // UIコンポーネントの更新
        if (addressBar != null) {
            addressBar.update();
        }
    }

    @Override
    public void keyPressed(PGraphics g, char key, int keyCode) {
        // アドレスバーがフォーカスされている場合
        if (addressBar != null && addressBar.isFocused()) {
            // Ctrl+V: ペースト（keyCode=86）
            if (ctrlPressed && keyCode == 86) {
                pasteToAddressBar();
                return;
            }

            // Ctrl+C: コピー（keyCode=67）
            if (ctrlPressed && keyCode == 67) {
                copyFromAddressBar();
                return;
            }

            // Ctrl+A: 全選択（keyCode=65）
            if (ctrlPressed && keyCode == 65) {
                // BaseTextInputのCtrl+A処理に委譲
                addressBar.onKeyPressed(key, keyCode);
                return;
            }

            // Enterキーで移動
            if (keyCode == 10) { // Enter
                navigateToAddressBarUrl();
                return;
            }

            // その他のキー入力をアドレスバーに転送
            addressBar.onKeyPressed(key, keyCode);
        } else {
            // アドレスバーがフォーカスされていない場合はWebViewにキー入力を転送
            if (initialized && webViewWrapper != null && !webViewWrapper.isDisposed()) {
                webViewWrapper.simulateKeyPressed(key, keyCode);
            }
        }
    }

    /**
     * クリップボードからアドレスバーにペースト
     */
    private void pasteToAddressBar() {
        if (kernel.getClipboardManager() == null) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("BrowserScreen", "ClipboardManagerが利用できません");
            }
            return;
        }

        String clipboardText = kernel.getClipboardManager().pasteText();
        if (clipboardText != null && !clipboardText.isEmpty()) {
            // 現在のテキストと選択範囲を取得
            String currentText = addressBar.getText();
            String selectedText = addressBar.getSelectedText();

            if (selectedText != null && !selectedText.isEmpty()) {
                // 選択範囲がある場合は置換
                String newText = currentText.replace(selectedText, clipboardText);
                addressBar.setText(newText);
            } else {
                // 選択範囲がない場合は末尾に追加
                addressBar.setText(currentText + clipboardText);
            }

            if (kernel.getLogger() != null) {
                kernel.getLogger().info("BrowserScreen", "アドレスバーにペーストしました: " + clipboardText);
            }
        }
    }

    /**
     * アドレスバーからクリップボードにコピー
     */
    private void copyFromAddressBar() {
        if (kernel.getClipboardManager() == null) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("BrowserScreen", "ClipboardManagerが利用できません");
            }
            return;
        }

        String selectedText = addressBar.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            kernel.getClipboardManager().copyText(selectedText);
            if (kernel.getLogger() != null) {
                kernel.getLogger().info("BrowserScreen", "アドレスバーからコピーしました: " + selectedText);
            }
        } else {
            // 選択範囲がない場合は全体をコピー
            String allText = addressBar.getText();
            if (allText != null && !allText.isEmpty()) {
                kernel.getClipboardManager().copyText(allText);
                if (kernel.getLogger() != null) {
                    kernel.getLogger().info("BrowserScreen", "アドレスバー全体をコピーしました: " + allText);
                }
            }
        }
    }

    @Override
    public void setModifierKeys(boolean shift, boolean ctrl) {
        // 修飾キーの状態を記録
        this.shiftPressed = shift;
        this.ctrlPressed = ctrl;

        // 修飾キーをUIコンポーネントに伝播
        if (addressBar != null) {
            addressBar.setShiftPressed(shift);
            addressBar.setCtrlPressed(ctrl);
        }
    }

    @Override
    public void onBackground() {
        isInBackground = true;
        if (webViewWrapper != null && !webViewWrapper.isDisposed()) {
            webViewWrapper.setInBackground(true);
        }
        if (kernel.getLogger() != null) {
            kernel.getLogger().info("BrowserScreen", "バックグラウンドに移行");
        }
    }

    @Override
    public void onForeground() {
        isInBackground = false;
        if (webViewWrapper != null && !webViewWrapper.isDisposed()) {
            webViewWrapper.setInBackground(false);
            webViewWrapper.requestUpdate();
        }
        if (kernel.getLogger() != null) {
            kernel.getLogger().info("BrowserScreen", "フォアグラウンドに復帰");
        }
    }

    @Override
    public void cleanup(PGraphics pg) {
        if (kernel.getLogger() != null) {
            kernel.getLogger().info("BrowserScreen", "クリーンアップ中...");
        }

        // WebViewを破棄
        if (webViewWrapper != null && !webViewWrapper.isDisposed()) {
            if (kernel.getWebViewManager() != null) {
                kernel.getWebViewManager().destroyWebView(webViewWrapper);
            } else {
                webViewWrapper.dispose();
            }
            webViewWrapper = null;
        }

        initialized = false;
    }

    @Override
    public String getScreenTitle() {
        return "ブラウザ";
    }
}
