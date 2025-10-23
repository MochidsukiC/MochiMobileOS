package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;

/**
 * Chromium統合のプロバイダーインターフェース。
 * 環境ごとに異なるCefApp実装を提供する。
 *
 * このインターフェースにより、coreモジュールはjcefmavenやMCEFなどの
 * 具体的な実装に依存せず、上位モジュール（standaloneやforge）が
 * 適切な実装を注入できるようになる。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public interface ChromiumProvider {

    /**
     * CefAppを作成または取得する。
     *
     * このメソッドは、環境に応じた方法でCefAppを初期化する：
     * - Standalone環境: jcefmavenを使ってCefAppを作成
     * - Forge環境: MCEFから既存のCefAppを取得
     *
     * @param kernel Kernelインスタンス（VFS、Logger等を使用）
     * @return 初期化されたCefAppインスタンス
     * @throws RuntimeException 初期化に失敗した場合
     */
    CefApp createCefApp(Kernel kernel);

    /**
     * CefBrowserを作成する（環境ごとに異なるAPI対応）。
     *
     * jcefmavenとMCEFではCefClient.createBrowser()のメソッドシグネチャが異なるため、
     * 各環境に適したAPI呼び出しをプロバイダーで実装する。
     *
     * - jcefmaven 135.0.20: client.createBrowser(url, osrEnabled, transparent)
     * - MCEF 2.1.6-1.20.1: client.createBrowser(url, osrEnabled, transparent, context) など
     *
     * @param client CefClientインスタンス
     * @param url 初期URL
     * @param osrEnabled オフスクリーンレンダリング有効化
     * @param transparent 透過背景
     * @return 作成されたCefBrowserインスタンス
     * @throws RuntimeException ブラウザ作成に失敗した場合
     */
    CefBrowser createBrowser(CefClient client, String url, boolean osrEnabled, boolean transparent);

    /**
     * このプロバイダーが現在の環境で利用可能かを確認する。
     *
     * 例：
     * - Standalone環境: 常にtrue（jcefmavenが利用可能）
     * - Forge環境: MCEF.isInitialized()の結果
     *
     * @return 利用可能な場合true、それ以外false
     */
    boolean isAvailable();

    /**
     * プロバイダーの名前を取得する。
     * ログ出力やデバッグ時に使用される。
     *
     * @return プロバイダー名（例: "jcefmaven (Standalone)", "MCEF (Forge)"）
     */
    String getName();

    /**
     * CEFメッセージループを実行する。
     * Kernel.update()から毎フレーム呼び出される。
     *
     * デフォルト実装では何もしない（プロバイダーが必要に応じてオーバーライド）。
     *
     * @param cefApp 初期化済みのCefAppインスタンス
     */
    default void doMessageLoopWork(CefApp cefApp) {
        if (cefApp != null) {
            cefApp.doMessageLoopWork(0);
        }
    }

    /**
     * プロバイダーのシャットダウン処理。
     * Kernel.shutdown()から呼び出される。
     *
     * デフォルト実装では何もしない（プロバイダーが必要に応じてオーバーライド）。
     *
     * @param cefApp 初期化済みのCefAppインスタンス
     */
    default void shutdown(CefApp cefApp) {
        // デフォルトでは何もしない
        // Standaloneプロバイダーでは cefApp.dispose() を呼ぶ
        // Forgeプロバイダーでは何もしない（MCEFが管理）
    }

    /**
     * このプロバイダーがUIComponent（GLCanvas）をサポートするかを返す。
     *
     * jcefmavenはJOGLのGLCanvasを使用し、getUIComponent()メソッドが存在する。
     * MCEFはLWJGLのOpenGLを直接使用し、getUIComponent()メソッドは存在しない。
     *
     * @return UIComponentをサポートする場合true、それ以外false
     */
    default boolean supportsUIComponent() {
        return false;  // デフォルトではサポートしない（MCEF互換）
    }

    /**
     * 環境に応じたChromiumRenderHandlerを作成する。
     *
     * jcefmavenでは標準のChromiumRenderHandlerを使用するため、nullを返す（デフォルト実装）。
     * MCEFでは、MCEFRenderHandlerAdapterを作成してOpenGL→PImage変換を行う。
     *
     * @param kernel Kernelインスタンス
     * @param browser 作成されたCefBrowserインスタンス
     * @param width 幅
     * @param height 高さ
     * @return カスタムRenderHandler、またはnull（デフォルトChromiumRenderHandlerを使用）
     */
    default ChromiumRenderHandler createRenderHandler(Kernel kernel, CefBrowser browser, int width, int height) {
        return null;  // デフォルトではnull（ChromiumBrowserが標準のChromiumRenderHandlerを使用）
    }

    // ===============================
    // マウス/キーボードイベント送信
    // ===============================

    /**
     * マウス押下イベントをブラウザに送信する。
     *
     * @param browser CefBrowserインスタンス
     * @param x マウスX座標
     * @param y マウスY座標
     * @param button マウスボタン (1=左, 2=中, 3=右)
     */
    void sendMousePressed(CefBrowser browser, int x, int y, int button);

    /**
     * マウス離しイベントをブラウザに送信する。
     *
     * @param browser CefBrowserインスタンス
     * @param x マウスX座標
     * @param y マウスY座標
     * @param button マウスボタン (1=左, 2=中, 3=右)
     */
    void sendMouseReleased(CefBrowser browser, int x, int y, int button);

    /**
     * マウス移動イベントをブラウザに送信する。
     *
     * @param browser CefBrowserインスタンス
     * @param x マウスX座標
     * @param y マウスY座標
     */
    void sendMouseMoved(CefBrowser browser, int x, int y);

    /**
     * マウスホイールイベントをブラウザに送信する。
     *
     * @param browser CefBrowserインスタンス
     * @param x マウスX座標
     * @param y マウスY座標
     * @param delta スクロール量
     */
    void sendMouseWheel(CefBrowser browser, int x, int y, float delta);

    /**
     * キー押下イベントをブラウザに送信する。
     *
     * @param browser CefBrowserインスタンス
     * @param keyCode キーコード
     * @param keyChar キャラクター
     */
    void sendKeyPressed(CefBrowser browser, int keyCode, char keyChar);

    /**
     * キー離しイベントをブラウザに送信する。
     *
     * @param browser CefBrowserインスタンス
     * @param keyCode キーコード
     * @param keyChar キャラクター
     */
    void sendKeyReleased(CefBrowser browser, int keyCode, char keyChar);
}
