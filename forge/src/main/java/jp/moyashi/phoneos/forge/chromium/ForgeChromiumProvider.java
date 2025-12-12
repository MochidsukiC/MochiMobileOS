package jp.moyashi.phoneos.forge.chromium;

import com.mojang.logging.LogUtils;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumProvider;
import org.cef.CefApp;
import org.slf4j.Logger;

/**
 * Forge環境用のChromiumProvider実装。
 * MCEFから既存のCefAppを取得して使用する。
 *
 * このプロバイダーは、Forge MOD環境でChromiumを使用可能にするため、
 * MCEF (Minecraft Chromium Embedded Framework) MODが提供する既存の
 * CefAppインスタンスを取得して利用する。
 *
 * MCEFはMinecraftの起動時に独自にChromiumを初期化しているため、
 * このプロバイダーはそれを再利用するだけで、新規初期化は行わない。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class ForgeChromiumProvider implements ChromiumProvider {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String TAG = "[ForgeChromiumProvider]";
    private static final String MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12; MochiMobileOS) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/122.0.0.0 Mobile Safari/537.36";

    @Override
    public CefApp createCefApp(Kernel kernel) {
        if (!isAvailable()) {
            throw new RuntimeException("MCEF is not initialized");
        }

        try {
            System.out.println(TAG + " Getting CefApp from MCEF...");

            // MCEFから既存のCefAppを取得
            CefApp cefApp = com.cinemamod.mcef.MCEF.getApp().getHandle();

            try {
                com.cinemamod.mcef.MCEF.getSettings().setUserAgent(MOBILE_USER_AGENT);
                System.out.println(TAG + " Custom mobile user agent applied: " + MOBILE_USER_AGENT);
            } catch (Exception e) {
                System.err.println(TAG + " Failed to set user agent: " + e.getMessage());
            }

            System.out.println(TAG + " CefApp obtained from MCEF successfully");
            System.out.println(TAG + " Chromium version: " + cefApp.getVersion());

            return cefApp;

        } catch (Exception e) {
            System.err.println(TAG + " Failed to get CefApp from MCEF: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to get CefApp from MCEF", e);
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // MCEFが初期化されているかチェック
            return com.cinemamod.mcef.MCEF.isInitialized();
        } catch (Exception e) {
            System.err.println(TAG + " Error checking MCEF availability: " + e.getMessage());
            return false;
        }
    }

    @Override
    public String getName() {
        return "MCEF (Forge)";
    }

    @Override
    public void doMessageLoopWork(CefApp cefApp) {
        if (cefApp == null) {
            return;
        }

        try {
            // CEFメッセージループを実行
            // MCEFが管理しているCefAppでも、アプリ側でメッセージループを駆動する必要がある
            cefApp.doMessageLoopWork(0);

        } catch (Exception e) {
            // エラーは静かに無視（MCEF側でも処理している可能性があるため）
            // デバッグが必要な場合のみログ出力
            // System.err.println(TAG + " Error in CEF message loop: " + e.getMessage());
        }
    }

    /** MCEFClientのマッピング（ブラウザごとにMCEFClientを保持） */
    private final java.util.Map<org.cef.browser.CefBrowser, com.cinemamod.mcef.MCEFClient> mcefClientMap = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public org.cef.browser.CefBrowser createBrowser(org.cef.CefClient client, String url, boolean osrEnabled, boolean transparent) {
        try {
            System.out.println(TAG + " Creating browser with MCEF API...");
            System.out.println(TAG + " - URL: " + url);
            System.out.println(TAG + " - OSR: " + osrEnabled + ", Transparent: " + transparent);

            // MCEFBrowserを使用してブラウザを作成
            // MCEFBrowserは内部でMCEFRendererを持ち、onPaint()を自動的に処理する
            // 参考: com.cinemamod.mcef.MCEFBrowser.java:87-93

            // MCEFClientを作成（CefClientをラップ）
            // MCEFClientはCefClientを内部で保持し、LoadHandlerなどの委譲を行う
            com.cinemamod.mcef.MCEFClient mcefClient = new com.cinemamod.mcef.MCEFClient(client);

            // MCEFBrowserを作成（CefBrowserOsrを継承しているので互換性がある）
            MochiMCEFBrowser browser = new MochiMCEFBrowser(
                mcefClient,
                url,
                transparent
            );

            // MCEFClientをマップに保存（後でaddConsoleMessageListenerで使用）
            mcefClientMap.put(browser, mcefClient);

            System.out.println(TAG + " Browser created successfully (MCEFBrowser)");
            System.out.println(TAG + " MCEFRenderer initialized with textureID: " + browser.getRenderer().getTextureID());
            return browser;

        } catch (Exception e) {
            System.err.println(TAG + " Failed to create browser: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to create browser with MCEF", e);
        }
    }

    @Override
    public void shutdown(CefApp cefApp) {
        // MCEFが管理しているCefAppは、MCEFがシャットダウン時に自動的にdisposeする
        // このプロバイダーでは何もしない
        System.out.println(TAG + " Shutdown called (MCEF will handle CefApp disposal)");
    }

    @Override
    public jp.moyashi.phoneos.core.service.chromium.ChromiumRenderHandler createRenderHandler(
            jp.moyashi.phoneos.core.Kernel kernel,
            org.cef.browser.CefBrowser browser,
            int width,
            int height) {

        // MCEFBrowserの場合は、MCEFRenderHandlerAdapterを返す
        if (browser instanceof com.cinemamod.mcef.MCEFBrowser) {
            System.out.println(TAG + " Creating MCEFRenderHandlerAdapter for MCEFBrowser");
            com.cinemamod.mcef.MCEFBrowser mcefBrowser = (com.cinemamod.mcef.MCEFBrowser) browser;
            return new jp.moyashi.phoneos.forge.chromium.MCEFRenderHandlerAdapter(kernel, mcefBrowser, width, height);
        }

        // MCEFBrowserでない場合は、nullを返す（デフォルトのChromiumRenderHandlerを使用）
        System.out.println(TAG + " Browser is not MCEFBrowser, using default ChromiumRenderHandler");
        return null;
    }

    // ===============================
    // マウス/キーボードイベント送信
    // ===============================
    // 注: MCEFのCefBrowserはMouseEvent/KeyEventクラスを公開していないため、
    //     現時点ではイベント送信は未実装。将来的にMCEFのAPIが拡張された場合に実装予定。

    private static com.cinemamod.mcef.MCEFBrowser getMcefBrowser(org.cef.browser.CefBrowser browser) {
        if (browser instanceof com.cinemamod.mcef.MCEFBrowser) {
            return (com.cinemamod.mcef.MCEFBrowser) browser;
        }
        System.err.println(TAG + " Browser does not extend MCEFBrowser. Input event ignored.");
        return null;
    }

    private static int toMcefButton(int button) {
        switch (button) {
            case 1: return 0; // 左クリック
            case 2: return 2; // 中クリック
            case 3: return 1; // 右クリック
            default: return 0;
        }
    }

    @Override
    public void sendMousePressed(org.cef.browser.CefBrowser browser, int x, int y, int button) {
        try {
            com.cinemamod.mcef.MCEFBrowser mcefBrowser = getMcefBrowser(browser);
            if (mcefBrowser == null) return;
            mcefBrowser.sendMousePress(x, y, toMcefButton(button));
            mcefBrowser.setFocus(true);
        } catch (Exception e) {
            System.err.println(TAG + " Failed to send mouse pressed: " + e.getMessage());
        }
    }

    @Override
    public void sendMouseReleased(org.cef.browser.CefBrowser browser, int x, int y, int button) {
        try {
            com.cinemamod.mcef.MCEFBrowser mcefBrowser = getMcefBrowser(browser);
            if (mcefBrowser == null) return;
            mcefBrowser.sendMouseRelease(x, y, toMcefButton(button));
            mcefBrowser.setFocus(true);
        } catch (Exception e) {
            System.err.println(TAG + " Failed to send mouse released: " + e.getMessage());
        }
    }

    @Override
    public void sendMouseMoved(org.cef.browser.CefBrowser browser, int x, int y) {
        try {
            com.cinemamod.mcef.MCEFBrowser mcefBrowser = getMcefBrowser(browser);
            if (mcefBrowser == null) return;
            mcefBrowser.sendMouseMove(x, y);
        } catch (Exception e) {
            // マウス移動は頻繁に呼び出されるため、スタックトレースは抑止
        }
    }

    @Override
    public void sendMouseDragged(org.cef.browser.CefBrowser browser, int x, int y, int button) {
        try {
            com.cinemamod.mcef.MCEFBrowser mcefBrowser = getMcefBrowser(browser);
            if (mcefBrowser == null) return;
            // MCEF does not have explicit drag support, so we reuse move
            mcefBrowser.sendMouseMove(x, y);
        } catch (Exception e) {
            // Suppress stack trace
        }
    }

    @Override
    public void sendMouseWheel(org.cef.browser.CefBrowser browser, int x, int y, float delta) {
        try {
            com.cinemamod.mcef.MCEFBrowser mcefBrowser = getMcefBrowser(browser);
            if (mcefBrowser == null) return;
            double amount = delta / 50.0;
            mcefBrowser.sendMouseWheel(x, y, amount, 0);
        } catch (Exception e) {
            System.err.println(TAG + " Failed to send mouse wheel: " + e.getMessage());
        }
    }

    @Override
    public void sendKeyPressed(org.cef.browser.CefBrowser browser, int keyCode, char keyChar, boolean shiftPressed, boolean ctrlPressed, boolean altPressed, boolean metaPressed) {
        try {
            com.cinemamod.mcef.MCEFBrowser mcefBrowser = getMcefBrowser(browser);
            if (mcefBrowser == null) return;

            // 修飾子フラグを構築（MCEF APIに合わせる）
            int modifiers = 0;
            if (shiftPressed) {
                modifiers |= 1;  // SHIFT_DOWN_MASK equivalent
            }
            if (ctrlPressed) {
                modifiers |= 2;  // CTRL_DOWN_MASK equivalent
            }
            if (altPressed) {
                modifiers |= 4;  // ALT_DOWN_MASK equivalent
            }
            if (metaPressed) {
                modifiers |= 8;  // META_DOWN_MASK equivalent
            }

            mcefBrowser.sendKeyPress(keyCode, 0L, modifiers);
            if (keyChar != 0 && keyChar >= 32 && !ctrlPressed) {
                mcefBrowser.sendKeyTyped(keyChar, modifiers);
            }
            mcefBrowser.setFocus(true);
        } catch (Exception e) {
            System.err.println(TAG + " Failed to send key pressed: " + e.getMessage());
        }
    }

    @Override
    public void sendKeyReleased(org.cef.browser.CefBrowser browser, int keyCode, char keyChar, boolean shiftPressed, boolean ctrlPressed, boolean altPressed, boolean metaPressed) {
        try {
            com.cinemamod.mcef.MCEFBrowser mcefBrowser = getMcefBrowser(browser);
            if (mcefBrowser == null) return;

            // 修飾子フラグを構築
            int modifiers = 0;
            if (shiftPressed) {
                modifiers |= 1;  // SHIFT_DOWN_MASK equivalent
            }
            if (ctrlPressed) {
                modifiers |= 2;  // CTRL_DOWN_MASK equivalent
            }
            if (altPressed) {
                modifiers |= 4;  // ALT_DOWN_MASK equivalent
            }
            if (metaPressed) {
                modifiers |= 8;  // META_DOWN_MASK equivalent
            }

            mcefBrowser.sendKeyRelease(keyCode, 0L, modifiers);
        } catch (Exception e) {
            System.err.println(TAG + " Failed to send key released: " + e.getMessage());
        }
    }

    @Override
    public void addConsoleMessageListener(org.cef.browser.CefBrowser browser, ConsoleMessageListener listener) {
        LOGGER.warn("{} addConsoleMessageListener called", TAG);
        LOGGER.warn("{} browser: {}", TAG, browser);
        LOGGER.warn("{} mcefClientMap size: {}", TAG, mcefClientMap.size());
        LOGGER.warn("{} mcefClientMap keys: {}", TAG, mcefClientMap.keySet());

        com.cinemamod.mcef.MCEFClient mcefClient = mcefClientMap.get(browser);
        if (mcefClient == null) {
            LOGGER.error("{} MCEFClient not found for browser, cannot add console listener", TAG);
            LOGGER.error("{} Browser class: {}", TAG, browser.getClass().getName());
            return;
        }

        // MCEFClientにDisplayHandlerを追加
        mcefClient.addDisplayHandler(new org.cef.handler.CefDisplayHandler() {
            @Override
            public void onAddressChange(org.cef.browser.CefBrowser b, org.cef.browser.CefFrame frame, String url) {
            }

            @Override
            public void onTitleChange(org.cef.browser.CefBrowser b, String title) {
            }

            public void onFullscreenModeChange(org.cef.browser.CefBrowser b, boolean fullscreen) {
            }

            @Override
            public boolean onTooltip(org.cef.browser.CefBrowser b, String text) {
                return false;
            }

            @Override
            public void onStatusMessage(org.cef.browser.CefBrowser b, String value) {
            }

            @Override
            public boolean onConsoleMessage(org.cef.browser.CefBrowser b, org.cef.CefSettings.LogSeverity level,
                    String message, String source, int line) {
                LOGGER.warn("{} [DEBUG] onConsoleMessage received: {}", TAG, message);
                boolean result = listener.onConsoleMessage(message, source, line);
                LOGGER.warn("{} [DEBUG] listener returned: {}", TAG, result);
                return result;
            }

            @Override
            public boolean onCursorChange(org.cef.browser.CefBrowser b, int cursorType) {
                return false;
            }
        });

        LOGGER.warn("{} Console message listener added to MCEFClient", TAG);
    }
}
