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
     * HTMLコンテンツを直接読み込みます。
     * CefFrame.loadString()を使用してHTMLを直接レンダリングします。
     * カスタムスキーム（mochiapp://等）でOSR再描画がトリガーされない問題を回避するために使用。
     *
     * @param html HTMLコンテンツ
     * @param baseUrl ベースURL（相対パス解決に使用）
     */
    void loadContent(String html, String baseUrl);

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
     * マウスドラッグイベントを通知します。
     */
    void sendMouseDragged(int x, int y, int button);

    /**
     * マウスホイールイベントを通知します。
     */
    void sendMouseWheel(int x, int y, float delta);

    /**
     * キー押下イベントを通知します。
     *
     * @param keyCode キーコード
     * @param keyChar キャラクター
     * @param shiftPressed Shiftキーが押されているか
     * @param ctrlPressed Ctrlキーが押されているか
     * @param altPressed Altキーが押されているか
     * @param metaPressed Metaキー（Command/Windowsキー）が押されているか
     */
    void sendKeyPressed(int keyCode, char keyChar, boolean shiftPressed, boolean ctrlPressed, boolean altPressed, boolean metaPressed);

    /**
     * キー離上イベントを通知します。
     *
     * @param keyCode キーコード
     * @param keyChar キャラクター
     * @param shiftPressed Shiftキーが押されているか
     * @param ctrlPressed Ctrlキーが押されているか
     * @param altPressed Altキーが押されているか
     * @param metaPressed Metaキー（Command/Windowsキー）が押されているか
     */
    void sendKeyReleased(int keyCode, char keyChar, boolean shiftPressed, boolean ctrlPressed, boolean altPressed, boolean metaPressed);

    /**
     * 現在の描画内容を表すフレームを取得します。
     */
    PImage acquireFrame();

    /**
     * サーフェスを破棄し、関連リソースを解放します。
     */
    void dispose();

    /**
     * レンダリング準備が整っているか（GLContextが初期化されているか）を確認する。
     * JCEFのOSR実装はGLContextがないとonPaintイベントを発火しないため、
     * このメソッドで確認してからURLロードや再描画を行うことが望ましい。
     *
     * @return 準備完了ならtrue
     */
    boolean isReadyToRender();

    // ========== TextInputProtocol用メソッド ==========

    /**
     * Webページ内のテキスト入力フィールドにフォーカスがあるかを返します。
     * JavaScript側のフォーカス検出スクリプトにより更新されます。
     *
     * @return テキスト入力フィールドにフォーカスがある場合true
     */
    boolean hasTextInputFocus();

    /**
     * キャッシュされた選択テキストを取得します。
     * JavaScript側で選択変更時にコンソール通知され、Java側でキャッシュされます。
     *
     * @return キャッシュされた選択テキスト（選択がない場合は空文字列）
     */
    String getCachedSelectedText();

    /**
     * JavaScriptを実行します。
     *
     * @param script 実行するJavaScriptコード
     */
    void executeScript(String script);

    /**
     * MCEF環境（Forge）かどうかを返します。
     * MCEF環境ではJavaScriptコンソールメッセージが読み取れないため、
     * テキストフォーカス検出が動作しません。
     *
     * @return MCEF環境の場合true
     */
    boolean isMCEF();

    // ========== メディア再生状態関連 ==========

    /**
     * メディア検出スクリプトを注入します。
     * ページ内のvideo/audio要素の再生状態を監視し、Java側に通知します。
     */
    default void injectMediaDetectionScript() {}

    /**
     * メディアが再生中かどうかを返します。
     *
     * @return 再生中の場合true
     */
    default boolean isMediaPlaying() { return false; }

    /**
     * 再生中のメディアのタイトルを返します。
     *
     * @return タイトル
     */
    default String getMediaTitle() { return ""; }

    /**
     * 再生中のメディアのアーティストを返します。
     *
     * @return アーティスト
     */
    default String getMediaArtist() { return ""; }

    /**
     * メディアの長さ（秒）を返します。
     *
     * @return 長さ（秒）
     */
    default double getMediaDuration() { return 0; }

    /**
     * メディアの現在再生位置（秒）を返します。
     *
     * @return 現在位置（秒）
     */
    default double getMediaCurrentTime() { return 0; }

    /**
     * メディア検出状態をリセットします。
     * ページ遷移時に呼び出します。
     */
    default void resetMediaDetection() {}
}
