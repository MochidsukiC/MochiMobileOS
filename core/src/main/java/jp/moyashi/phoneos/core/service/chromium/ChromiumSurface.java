package jp.moyashi.phoneos.core.service.chromium;

import processing.core.PImage;

/**
 * クロミウム描画/操作を抽象化したサーフェスインタフェース。
 * <p>
 * スタンドアロン/Forge などプラットフォーム差異を吸収し、UI 層が
 * 共通の API でブラウザコンテンツを扱えるようにするための契約です。
 */
public interface ChromiumSurface {

    /**
     * サーフェスを識別する ID を返します。
     */
    String getSurfaceId();

    /**
     * 現在のサーフェスの幅を返します。
     */
    int getWidth();

    /**
     * 現在のサーフェスの高さを返します。
     */
    int getHeight();

    /**
     * サーフェスの表示サイズを変更します。
     *
     * @param width  新しい幅
     * @param height 新しい高さ
     */
    void resize(int width, int height);

    /**
     * 指定した URL へナビゲートします。
     */
    void loadUrl(String url);

    /**
     * 現在表示中の URL を取得します。
     */
    String getCurrentUrl();

    /**
     * 現在のタブタイトルを取得します。
     */
    String getTitle();

    /**
     * 現在のページを再読み込みします。
     */
    void reload();

    /**
     * 現在のページの読み込みを停止します。
     */
    void stopLoading();

    /**
     * 戻るナビゲーションが可能かを返します。
     */
    boolean canGoBack();

    /**
     * 戻るナビゲーションを実行します。
     */
    void goBack();

    /**
     * 進むナビゲーションが可能かを返します。
     */
    boolean canGoForward();

    /**
     * 進むナビゲーションを実行します。
     */
    void goForward();

    /**
     * ウィンドウレス描画のフレームレートを設定します。
     */
    void setFrameRate(int fps);

    /**
     * マウスボタンが押されたイベントを通知します。
     */
    void sendMousePressed(int x, int y, int button);

    /**
     * マウスボタンが離されたイベントを通知します。
     */
    void sendMouseReleased(int x, int y, int button);

    /**
     * マウス移動イベントを通知します。
     */
    void sendMouseMoved(int x, int y);

    /**
     * マウスホイールイベントを通知します。
     */
    void sendMouseWheel(int x, int y, float delta);

    /**
     * キー押下イベントを通知します。
     */
    void sendKeyPressed(int keyCode, char keyChar);

    /**
     * キー離上イベントを通知します。
     */
    void sendKeyReleased(int keyCode, char keyChar);

    /**
     * 現在の描画内容を表すフレームを取得します。
     */
    PImage acquireFrame();

    /**
     * サーフェスを破棄し、関連リソースを解放します。
     */
    void dispose();
}
