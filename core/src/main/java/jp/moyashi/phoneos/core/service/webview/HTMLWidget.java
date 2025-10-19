package jp.moyashi.phoneos.core.service.webview;

import processing.core.PGraphics;

/**
 * HTMLウィジェット - Java画面の一部にHTMLを埋め込むためのウィジェット。
 *
 * 使用例:
 * <pre>
 * public class BrowserScreen extends Screen {
 *     private HTMLWidget webWidget;
 *
 *     public void setup(PGraphics pg) {
 *         webWidget = kernel.getWebViewManager().createWidget(300, 400);
 *         webWidget.loadURL("https://example.com");
 *     }
 *
 *     public void draw(PGraphics pg) {
 *         // Javaで背景を描画
 *         pg.background(255);
 *
 *         // HTMLウィジェットを指定位置に描画
 *         webWidget.render(pg, 50, 100);
 *     }
 *
 *     public void mousePressed(int mouseX, int mouseY) {
 *         // マウスイベントをウィジェットに転送
 *         webWidget.handleMousePressed(mouseX, mouseY, 50, 100);
 *     }
 * }
 * </pre>
 */
public class HTMLWidget {
    private final WebViewWrapper wrapper;
    private final int width;
    private final int height;

    /**
     * HTMLウィジェットを作成する。
     *
     * @param wrapper WebViewWrapper インスタンス
     * @param width 幅
     * @param height 高さ
     */
    public HTMLWidget(WebViewWrapper wrapper, int width, int height) {
        this.wrapper = wrapper;
        this.width = width;
        this.height = height;
    }

    /**
     * HTMLコンテンツを読み込む。
     *
     * @param htmlContent HTML文字列
     */
    public void loadContent(String htmlContent) {
        wrapper.loadContent(htmlContent);
    }

    /**
     * URLを読み込む。
     *
     * @param url URL文字列
     */
    public void loadURL(String url) {
        wrapper.loadURL(url);
    }

    /**
     * JavaScriptを実行する。
     *
     * @param script JavaScriptコード
     * @return 実行結果
     */
    public Object executeScript(String script) {
        return wrapper.executeScript(script);
    }

    /**
     * ウィジェットをPGraphicsに描画する（フルスクリーン版）。
     * (0, 0)の位置に描画される。
     *
     * @param pg PGraphicsインスタンス
     */
    public void render(PGraphics pg) {
        wrapper.renderToPGraphics(pg, 0, 0);
    }

    /**
     * ウィジェットをPGraphicsの指定位置に描画する。
     *
     * @param pg PGraphicsインスタンス
     * @param x X座標
     * @param y Y座標
     */
    public void render(PGraphics pg, int x, int y) {
        wrapper.renderToPGraphics(pg, x, y);
    }

    /**
     * マウスクリックイベントを処理する。
     * ウィジェットの座標系に変換してWebViewにシミュレートする。
     *
     * @param mouseX グローバルマウスX座標
     * @param mouseY グローバルマウスY座標
     * @param widgetX ウィジェットのX座標
     * @param widgetY ウィジェットのY座標
     * @return ウィジェット内でのクリックの場合true
     */
    public boolean handleMouseClick(int mouseX, int mouseY, int widgetX, int widgetY) {
        // マウスがウィジェット内にあるか確認
        if (mouseX >= widgetX && mouseX < widgetX + width &&
            mouseY >= widgetY && mouseY < widgetY + height) {

            // ウィジェット座標系に変換
            int localX = mouseX - widgetX;
            int localY = mouseY - widgetY;

            wrapper.simulateMouseClick(localX, localY);
            return true;
        }
        return false;
    }

    /**
     * マウスプレスイベントを処理する。
     *
     * @param mouseX グローバルマウスX座標
     * @param mouseY グローバルマウスY座標
     * @param widgetX ウィジェットのX座標
     * @param widgetY ウィジェットのY座標
     * @return ウィジェット内でのプレスの場合true
     */
    public boolean handleMousePressed(int mouseX, int mouseY, int widgetX, int widgetY) {
        if (mouseX >= widgetX && mouseX < widgetX + width &&
            mouseY >= widgetY && mouseY < widgetY + height) {

            int localX = mouseX - widgetX;
            int localY = mouseY - widgetY;

            wrapper.simulateMousePressed(localX, localY);
            return true;
        }
        return false;
    }

    /**
     * マウスリリースイベントを処理する。
     *
     * @param mouseX グローバルマウスX座標
     * @param mouseY グローバルマウスY座標
     * @param widgetX ウィジェットのX座標
     * @param widgetY ウィジェットのY座標
     * @return ウィジェット内でのリリースの場合true
     */
    public boolean handleMouseReleased(int mouseX, int mouseY, int widgetX, int widgetY) {
        if (mouseX >= widgetX && mouseX < widgetX + width &&
            mouseY >= widgetY && mouseY < widgetY + height) {

            int localX = mouseX - widgetX;
            int localY = mouseY - widgetY;

            wrapper.simulateMouseReleased(localX, localY);
            return true;
        }
        return false;
    }

    /**
     * マウスがウィジェット内にあるか確認する。
     *
     * @param mouseX グローバルマウスX座標
     * @param mouseY グローバルマウスY座標
     * @param widgetX ウィジェットのX座標
     * @param widgetY ウィジェットのY座標
     * @return ウィジェット内にある場合true
     */
    public boolean isMouseInside(int mouseX, int mouseY, int widgetX, int widgetY) {
        return mouseX >= widgetX && mouseX < widgetX + width &&
               mouseY >= widgetY && mouseY < widgetY + height;
    }

    /**
     * 強制的に再描画を要求する。
     */
    public void requestUpdate() {
        wrapper.requestUpdate();
    }

    /**
     * ウィジェットを破棄する。
     * メモリリークを防ぐため、不要になったら必ず呼び出すこと。
     */
    public void dispose() {
        wrapper.dispose();
    }

    /**
     * 内部のWebViewWrapperを取得する。
     * 高度な操作が必要な場合に使用。
     *
     * @return WebViewWrapper インスタンス
     */
    public WebViewWrapper getWrapper() {
        return wrapper;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public boolean isDisposed() {
        return wrapper.isDisposed();
    }
}
