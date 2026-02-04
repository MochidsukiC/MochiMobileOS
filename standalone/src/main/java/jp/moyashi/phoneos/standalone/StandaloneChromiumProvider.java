package jp.moyashi.phoneos.standalone;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumAppHandler;
import jp.moyashi.phoneos.core.service.chromium.ChromiumProvider;
import me.friwi.jcefmaven.CefAppBuilder;
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.callback.CefSchemeRegistrar;

import java.awt.Component;
import java.awt.Canvas;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Standalone環境用のChromiumProvider実装。
 * jcefmavenを使用してCefAppを初期化する。
 *
 * このプロバイダーは、Standalone環境（デスクトップアプリ）でChromiumを
 * 使用可能にするため、jcefmavenライブラリを使用してJCEFを自動的に
 * ダウンロード・初期化する。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class StandaloneChromiumProvider implements ChromiumProvider {

    /**
     * CefBrowserが保持するprotectedメソッドをキャッシュする構造体。
     * 反射メソッド探索を一度だけ行い、以降のイベント送出時のオーバーヘッドを削減する。
     */
    private static final class BrowserMethodCache {
        final java.lang.reflect.Method sendMouseEvent;
        final java.lang.reflect.Method sendMouseWheelEvent;
        final java.lang.reflect.Method sendKeyEvent;

        BrowserMethodCache(Class<?> browserClass) throws NoSuchMethodException {
            this.sendMouseEvent = findAndPrepare(browserClass, "sendMouseEvent", java.awt.event.MouseEvent.class);
            this.sendMouseWheelEvent = findAndPrepare(browserClass, "sendMouseWheelEvent", java.awt.event.MouseWheelEvent.class);
            this.sendKeyEvent = findAndPrepare(browserClass, "sendKeyEvent", java.awt.event.KeyEvent.class);
        }
    }

    /**
     * ブラウザクラスごとのメソッドキャッシュ。
     */
    private static final Map<Class<?>, BrowserMethodCache> METHOD_CACHE = new ConcurrentHashMap<>();

    /**
     * イベント送信に使用するデフォルトコンポーネント。
     * 実際のUIコンポーネントが取得できない場合のフォールバック。
     */
    private final Component fallbackComponent = new Canvas();

    private static java.lang.reflect.Method findAndPrepare(Class<?> startClass, String name, Class<?> paramType) throws NoSuchMethodException {
        Class<?> clazz = startClass;
        while (clazz != null) {
            try {
                java.lang.reflect.Method method = clazz.getDeclaredMethod(name, paramType);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                clazz = clazz.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name + "(" + paramType.getSimpleName() + ") not found for " + startClass.getName());
    }

    private static BrowserMethodCache getMethodCache(org.cef.browser.CefBrowser browser) {
        return METHOD_CACHE.computeIfAbsent(browser.getClass(), clazz -> {
            try {
                return new BrowserMethodCache(clazz);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Failed to cache CefBrowser methods for " + clazz.getName(), e);
            }
        });
    }

    @Override
    public CefApp createCefApp(Kernel kernel) {
        try {
            // ChromiumAppHandlerを作成（coreモジュール）
            ChromiumAppHandler coreAppHandler = new ChromiumAppHandler(kernel);

            // jcefmaven用のラッパーAppHandlerを作成（委譲パターン）
            MavenCefAppHandlerAdapter appHandler = new MavenCefAppHandlerAdapter() {
                @Override
                public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
                    // coreのAppHandlerに委譲
                    coreAppHandler.onRegisterCustomSchemes(registrar);
                }

                @Override
                public void onContextInitialized() {
                    // coreのAppHandlerに委譲
                    coreAppHandler.onContextInitialized();
                }
            };

            // CefAppBuilderを使用してJCEFを初期化
            CefAppBuilder builder = new CefAppBuilder();
            builder.setAppHandler(appHandler);

            // CefSettingsを取得して設定
            CefSettings settings = builder.getCefSettings();

            // キャッシュパス（VFS内）
            String cachePath = kernel.getVFS().getFullPath("system/browser_chromium/cache");
            settings.cache_path = cachePath;

            // User-Agent（モバイル最適化）
            settings.user_agent = "Mozilla/5.0 (Linux; Android 12; MochiMobileOS) " +
                                  "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                  "Chrome/122.0.0.0 Mobile Safari/537.36";

            // ロケール
            settings.locale = "ja";

            // ログレベル
            settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_WARNING;

            // オフスクリーンレンダリング有効化
            settings.windowless_rendering_enabled = true;

            // パフォーマンス最適化設定（GPU/CPUを最大限活用）
            // 背景フレームレート制限を解除（YouTube動画再生時のスムーズさ向上）
            // settings.background_color = 0xFFFFFFFF; // 白背景（ColorType型のため直接設定不可）

            // Mac環境特有の問題に対処（コード署名エラー回避）
            String osName = System.getProperty("os.name").toLowerCase();
            boolean isMac = osName.contains("mac");

            if (isMac) {
                // Macでのコード署名エラーを回避（サンドボックス無効化のみ）
                builder.addJcefArgs("--no-sandbox");
                builder.addJcefArgs("--disable-gpu-sandbox");
                // SSL/TLS関連のエラーを回避（大学ネットワーク等の特殊環境対応）
                builder.addJcefArgs("--ignore-certificate-errors");
                // GPU加速は必須なので維持
                builder.addJcefArgs("--enable-gpu");
                builder.addJcefArgs("--enable-accelerated-video-decode");
                builder.addJcefArgs("--enable-accelerated-2d-canvas");
            } else {
                // Windows/その他のプラットフォームでは従来通りの設定（サンドボックス有効）
                builder.addJcefArgs("--enable-gpu");
                builder.addJcefArgs("--enable-accelerated-video-decode");
                builder.addJcefArgs("--enable-accelerated-2d-canvas");
            }

            // 共通設定（全プラットフォーム）
            builder.addJcefArgs("--disable-gpu-vsync"); // VSync無効化でフレームレート向上
            builder.addJcefArgs("--max-fps=60"); // 最大60FPS
            builder.addJcefArgs("--disable-frame-rate-limit"); // フレームレート制限解除

            // Windows固有のGPU最適化設定
            boolean isWindows = osName.contains("windows");
            if (isWindows) {
                // ゼロコピー転送を有効化（GPU→CPU転送オーバーヘッド削減）
                builder.addJcefArgs("--enable-zero-copy");
                // 共有メモリを使用してバッファ転送を最適化
                builder.addJcefArgs("--enable-native-gpu-memory-buffers");
                // ソフトウェアラスタライザを無効化（GPUを強制使用）
                builder.addJcefArgs("--disable-software-rasterizer");
            }

            // JCEFをビルドして初期化
            CefApp cefApp = builder.build();

            return cefApp;

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JCEF with jcefmaven", e);
        }
    }

    @Override
    public boolean isAvailable() {
        // jcefmavenは常に利用可能（自動ダウンロード機能があるため）
        return true;
    }

    @Override
    public String getName() {
        return "jcefmaven (Standalone)";
    }

    @Override
    public void doMessageLoopWork(CefApp cefApp) {
        if (cefApp == null) {
            return;
        }
        try {
            cefApp.doMessageLoopWork(0);
        } catch (Exception e) {
            // Silently ignore CEF message loop errors
        }
    }

    @Override
    public org.cef.browser.CefBrowser createBrowser(org.cef.CefClient client, String url, boolean osrEnabled, boolean transparent) {
        try {
            // jcefmaven 135.0.20の3引数API: createBrowser(url, osrEnabled, transparent)
            org.cef.browser.CefBrowser browser = client.createBrowser(url, osrEnabled, transparent);

            return browser;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create browser with jcefmaven", e);
        }
    }

    private static class SafeRequestContextHandler extends org.cef.handler.CefRequestContextHandlerAdapter {
        // Named class to avoid anonymous class proxy issues in JNI
    }

    @Override
    public org.cef.browser.CefRequestContext createRequestContext(CefApp app, String cachePath) {
        try {
            // NOTE: Currently disabled due to native crashes in JCEF 135 (invalid version -1).
            // When re-enabling, ensure this runs on EDT and handler is properly proxied.
            /*
            final org.cef.browser.CefRequestContext[] result = new org.cef.browser.CefRequestContext[1];
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                org.cef.handler.CefRequestContextHandler handler = new SafeRequestContextHandler();
                result[0] = org.cef.browser.CefRequestContext.createContext(handler);
            });
            return result[0];
            */
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public org.cef.browser.CefBrowser createBrowser(org.cef.CefClient client, String url, boolean osrEnabled, boolean transparent, org.cef.browser.CefRequestContext context) {
        try {
            if (context == null) {
                return createBrowser(client, url, osrEnabled, transparent);
            }

            // context付きのcreateBrowserを呼び出す
            // JCEFの仕様により、コンテキストを使用する場合はEDTでの実行が推奨される
            final org.cef.browser.CefBrowser[] result = new org.cef.browser.CefBrowser[1];
            javax.swing.SwingUtilities.invokeAndWait(() -> {
                result[0] = client.createBrowser(url, osrEnabled, transparent, context);
            });
            
            return result[0];
        } catch (Exception e) {
            return createBrowser(client, url, osrEnabled, transparent);
        }
    }

    @Override
    public void shutdown(CefApp cefApp) {
        if (cefApp == null) {
            return;
        }

        try {
            cefApp.dispose();
        } catch (Exception e) {
            // Silently ignore CefApp disposal errors
        }
    }

    @Override
    public boolean supportsUIComponent() {
        // jcefmavenはJOGLのGLCanvasを使用するため、getUIComponent()が利用可能
        return true;
    }

    // ===============================
    // マウス/キーボードイベント送信
    // ===============================

    @Override
    public void sendMousePressed(org.cef.browser.CefBrowser browser, int x, int y, int button) {
        if (browser == null) {
            return;
        }

        try {
            BrowserMethodCache cache = getMethodCache(browser);

            // Processing: 1=左, 2=中, 3=右
            // AWT MouseEvent: BUTTON1=左, BUTTON2=中, BUTTON3=右
            int awtButton;
            int modifiers;
            switch (button) {
                case 2:
                    awtButton = java.awt.event.MouseEvent.BUTTON2;
                    modifiers = java.awt.event.MouseEvent.BUTTON2_DOWN_MASK;
                    break;
                case 3:
                    awtButton = java.awt.event.MouseEvent.BUTTON3;
                    modifiers = java.awt.event.MouseEvent.BUTTON3_DOWN_MASK;
                    break;
                default:
                    awtButton = java.awt.event.MouseEvent.BUTTON1;
                    modifiers = java.awt.event.MouseEvent.BUTTON1_DOWN_MASK;
                    break;
            }

            // 実際のUIコンポーネントを取得
            Component sourceComponent = browser.getUIComponent();
            if (sourceComponent == null) {
                sourceComponent = fallbackComponent;
            }

            java.awt.event.MouseEvent mouseEvent = new java.awt.event.MouseEvent(
                sourceComponent,                         // source（実際のUIコンポーネント）
                java.awt.event.MouseEvent.MOUSE_PRESSED, // id
                System.currentTimeMillis(),             // when
                modifiers,                               // modifiers
                x, y,                                    // x, y
                1,                                       // clickCount
                false,                                   // popupTrigger
                awtButton                                // button
            );

            cache.sendMouseEvent.invoke(browser, mouseEvent);

            // 注意: browser.setFocus(true) は呼び出さない（理由はsendKeyPressed()と同じ）

        } catch (Exception e) {
            // Silently ignore mouse event errors
        }
    }

    @Override
    public void sendMouseReleased(org.cef.browser.CefBrowser browser, int x, int y, int button) {
        if (browser == null) {
            return;
        }

        try {
            BrowserMethodCache cache = getMethodCache(browser);

            int awtButton;
            int modifiers;
            switch (button) {
                case 2:
                    awtButton = java.awt.event.MouseEvent.BUTTON2;
                    modifiers = java.awt.event.MouseEvent.BUTTON2_DOWN_MASK;
                    break;
                case 3:
                    awtButton = java.awt.event.MouseEvent.BUTTON3;
                    modifiers = java.awt.event.MouseEvent.BUTTON3_DOWN_MASK;
                    break;
                default:
                    awtButton = java.awt.event.MouseEvent.BUTTON1;
                    modifiers = java.awt.event.MouseEvent.BUTTON1_DOWN_MASK;
                    break;
            }

            Component sourceComponent = browser.getUIComponent();
            if (sourceComponent == null) {
                sourceComponent = fallbackComponent;
            }

            java.awt.event.MouseEvent mouseEvent = new java.awt.event.MouseEvent(
                sourceComponent,
                java.awt.event.MouseEvent.MOUSE_RELEASED,
                System.currentTimeMillis(),
                modifiers,
                x, y,
                1,
                false,
                awtButton
            );

            cache.sendMouseEvent.invoke(browser, mouseEvent);

        } catch (Exception e) {
            // Silently ignore mouse event errors
        }
    }

    @Override
    public void sendMouseMoved(org.cef.browser.CefBrowser browser, int x, int y) {
        if (browser == null) {
            return;
        }

        try {
            BrowserMethodCache cache = getMethodCache(browser);

            Component sourceComponent = browser.getUIComponent();
            if (sourceComponent == null) {
                sourceComponent = fallbackComponent;
            }

            java.awt.event.MouseEvent mouseEvent = new java.awt.event.MouseEvent(
                sourceComponent,
                java.awt.event.MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0,  // modifiers (no button pressed)
                x, y,
                0,  // clickCount
                false
            );

            cache.sendMouseEvent.invoke(browser, mouseEvent);

        } catch (Exception e) {
            // マウス移動は頻繁に呼ばれるため、エラーログを抑制
        }
    }

    @Override
    public void sendMouseDragged(org.cef.browser.CefBrowser browser, int x, int y, int button) {
        if (browser == null) {
            return;
        }

        try {
            BrowserMethodCache cache = getMethodCache(browser);

            int modifiers = 0;
            switch (button) {
                case 2:
                    modifiers = java.awt.event.MouseEvent.BUTTON2_DOWN_MASK;
                    break;
                case 3:
                    modifiers = java.awt.event.MouseEvent.BUTTON3_DOWN_MASK;
                    break;
                default:
                    modifiers = java.awt.event.MouseEvent.BUTTON1_DOWN_MASK;
                    break;
            }

            Component sourceComponent = browser.getUIComponent();
            if (sourceComponent == null) {
                sourceComponent = fallbackComponent;
            }

            java.awt.event.MouseEvent mouseEvent = new java.awt.event.MouseEvent(
                sourceComponent,
                java.awt.event.MouseEvent.MOUSE_DRAGGED,
                System.currentTimeMillis(),
                modifiers,
                x, y,
                0,
                false
            );

            cache.sendMouseEvent.invoke(browser, mouseEvent);

        } catch (Exception e) {
            // エラーログを抑制
        }
    }

    @Override
    public void sendMouseWheel(org.cef.browser.CefBrowser browser, int x, int y, float delta) {
        if (browser == null) {
            return;
        }

        try {
            BrowserMethodCache cache = getMethodCache(browser);

            Component sourceComponent = browser.getUIComponent();
            if (sourceComponent == null) {
                sourceComponent = fallbackComponent;
            }

            // MouseWheelEventを使用（deltaの符号反転が必要）
            java.awt.event.MouseWheelEvent wheelEvent = new java.awt.event.MouseWheelEvent(
                sourceComponent,
                java.awt.event.MouseEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                0,  // modifiers
                x, y,
                0,  // clickCount
                false,  // popupTrigger
                java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL,  // scrollType
                3,  // scrollAmount
                (int)delta  // wheelRotation (Processing delta is consistent with AWT)
            );

            cache.sendMouseWheelEvent.invoke(browser, wheelEvent);

        } catch (Exception e) {
            // Silently ignore mouse wheel event errors
        }
    }

    /**
     * ProcessingキーコードをAWTキーコードに変換する。
     * Processingは独自のキーコード定義を持つため、AWT KeyEventに渡す前に変換が必要。
     */
    private int convertProcessingToAwtKeyCode(int processingKeyCode, char keyChar) {
        // 特殊キーの変換
        switch (processingKeyCode) {
            case 8:   return java.awt.event.KeyEvent.VK_BACK_SPACE;  // Backspace
            case 9:   return java.awt.event.KeyEvent.VK_TAB;         // Tab
            case 10:  return java.awt.event.KeyEvent.VK_ENTER;       // Enter
            case 16:  return java.awt.event.KeyEvent.VK_SHIFT;       // Shift
            case 17:  return java.awt.event.KeyEvent.VK_CONTROL;     // Ctrl
            case 18:  return java.awt.event.KeyEvent.VK_ALT;         // Alt
            case 20:  return java.awt.event.KeyEvent.VK_CAPS_LOCK;   // CapsLock
            case 27:  return java.awt.event.KeyEvent.VK_ESCAPE;      // Escape
            case 32:  return java.awt.event.KeyEvent.VK_SPACE;       // Space
            case 33:  return java.awt.event.KeyEvent.VK_PAGE_UP;     // PageUp
            case 34:  return java.awt.event.KeyEvent.VK_PAGE_DOWN;   // PageDown
            case 35:  return java.awt.event.KeyEvent.VK_END;         // End
            case 36:  return java.awt.event.KeyEvent.VK_HOME;        // Home
            case 37:  return java.awt.event.KeyEvent.VK_LEFT;        // Left arrow
            case 38:  return java.awt.event.KeyEvent.VK_UP;          // Up arrow
            case 39:  return java.awt.event.KeyEvent.VK_RIGHT;       // Right arrow
            case 40:  return java.awt.event.KeyEvent.VK_DOWN;        // Down arrow
            case 127: return java.awt.event.KeyEvent.VK_DELETE;      // Delete
        }

        // 文字キー（A-Z, 0-9等）はそのままでOK
        // ProcessingとAWTで同じ値を使用している
        return processingKeyCode;
    }

    @Override
    public void sendKeyPressed(org.cef.browser.CefBrowser browser, int keyCode, char keyChar, boolean shiftPressed, boolean ctrlPressed, boolean altPressed, boolean metaPressed) {
        if (browser == null) {
            return;
        }

        try {
            BrowserMethodCache cache = getMethodCache(browser);

            // 修飾子フラグを構築
            int modifiers = 0;
            if (shiftPressed) {
                modifiers |= java.awt.event.KeyEvent.SHIFT_DOWN_MASK;
            }
            if (ctrlPressed) {
                modifiers |= java.awt.event.KeyEvent.CTRL_DOWN_MASK;
            }
            if (altPressed) {
                modifiers |= java.awt.event.KeyEvent.ALT_DOWN_MASK;
            }
            if (metaPressed) {
                modifiers |= java.awt.event.KeyEvent.META_DOWN_MASK;
            }

            // ProcessingキーコードをAWTキーコードに変換
            int awtKeyCode = convertProcessingToAwtKeyCode(keyCode, keyChar);

            // Ctrl/Alt/Meta押下時で制御文字の場合、keyCharをUNDEFINEDにする
            // ChromiumはmodifiersとkeyCodeの組み合わせでショートカットを判定するため、keyCharは不要
            char adjustedKeyChar = keyChar;
            if ((ctrlPressed || altPressed || metaPressed) && keyChar < 32 && keyChar != 0) {
                // すべての制御文字をUNDEFINEDにする
                adjustedKeyChar = java.awt.event.KeyEvent.CHAR_UNDEFINED;
            }

            // 実際のUIコンポーネントを取得（ブラウザのGLCanvas）
            // これがKeyEventのソースとして使用される
            Component sourceComponent = browser.getUIComponent();
            if (sourceComponent == null) {
                sourceComponent = fallbackComponent;
            }

            // KEY_PRESSED イベント
            java.awt.event.KeyEvent keyEvent = new java.awt.event.KeyEvent(
                sourceComponent,  // 実際のUIコンポーネントを使用
                java.awt.event.KeyEvent.KEY_PRESSED,
                System.currentTimeMillis(),
                modifiers,  // 修飾子フラグを適用
                awtKeyCode,  // 変換後のAWTキーコード
                adjustedKeyChar  // 調整後のkeyChar
            );

            // FocusEventを送信してブラウザにフォーカスを通知
            // setFocus(true)はAWT例外を引き起こすため、FocusEventを直接送信
            try {
                java.awt.event.FocusEvent focusEvent = new java.awt.event.FocusEvent(
                    sourceComponent,
                    java.awt.event.FocusEvent.FOCUS_GAINED,
                    false  // temporary = false
                );
                sourceComponent.dispatchEvent(focusEvent);
            } catch (Exception focusEx) {
                // フォーカスイベント送信に失敗しても続行
            }

            cache.sendKeyEvent.invoke(browser, keyEvent);

            // KEY_TYPED イベントも送信（テキスト入力用）
            // 日本語IME対応: keyCharが有効な文字の場合は送信
            // 制御キーショートカット（Ctrl押下時）は除外するが、IMEの中間文字は許可
            if (keyChar != java.awt.event.KeyEvent.CHAR_UNDEFINED && !ctrlPressed) {
                // 制御文字（0x00-0x1F, 0x7F）は除外するが、IMEの変換中の文字は許可
                boolean isControlChar = (keyChar < 32 && keyChar != 0) || keyChar == 127;
                if (!isControlChar) {
                    java.awt.event.KeyEvent typedEvent = new java.awt.event.KeyEvent(
                        sourceComponent,  // 実際のUIコンポーネントを使用
                        java.awt.event.KeyEvent.KEY_TYPED,
                        System.currentTimeMillis(),
                        modifiers,
                        java.awt.event.KeyEvent.VK_UNDEFINED,  // KEY_TYPEDではVK_UNDEFINED
                        keyChar
                    );
                    cache.sendKeyEvent.invoke(browser, typedEvent);
                }
            }

        } catch (Exception e) {
            // Silently ignore key event errors
        }
    }

    @Override
    public void sendKeyReleased(org.cef.browser.CefBrowser browser, int keyCode, char keyChar, boolean shiftPressed, boolean ctrlPressed, boolean altPressed, boolean metaPressed) {
        if (browser == null) {
            return;
        }

        try {
            BrowserMethodCache cache = getMethodCache(browser);

            // 修飾子フラグを構築
            int modifiers = 0;
            if (shiftPressed) {
                modifiers |= java.awt.event.KeyEvent.SHIFT_DOWN_MASK;
            }
            if (ctrlPressed) {
                modifiers |= java.awt.event.KeyEvent.CTRL_DOWN_MASK;
            }
            if (altPressed) {
                modifiers |= java.awt.event.KeyEvent.ALT_DOWN_MASK;
            }
            if (metaPressed) {
                modifiers |= java.awt.event.KeyEvent.META_DOWN_MASK;
            }

            // ProcessingキーコードをAWTキーコードに変換
            int awtKeyCode = convertProcessingToAwtKeyCode(keyCode, keyChar);

            // Ctrl/Alt/Meta押下時で制御文字の場合、keyCharをUNDEFINEDにする
            char adjustedKeyChar = keyChar;
            if ((ctrlPressed || altPressed || metaPressed) && keyChar < 32 && keyChar != 0) {
                adjustedKeyChar = java.awt.event.KeyEvent.CHAR_UNDEFINED;
            }

            // 実際のUIコンポーネントを取得（ブラウザのGLCanvas）
            Component sourceComponent = browser.getUIComponent();
            if (sourceComponent == null) {
                sourceComponent = fallbackComponent;
            }

            // KEY_RELEASED イベント
            java.awt.event.KeyEvent keyEvent = new java.awt.event.KeyEvent(
                sourceComponent,  // 実際のUIコンポーネントを使用
                java.awt.event.KeyEvent.KEY_RELEASED,
                System.currentTimeMillis(),
                modifiers,  // 修飾子フラグを適用
                awtKeyCode,  // 変換後のAWTキーコード
                adjustedKeyChar  // 調整後のkeyChar
            );

            cache.sendKeyEvent.invoke(browser, keyEvent);

        } catch (Exception e) {
            // Silently ignore key event errors
        }
    }
}
