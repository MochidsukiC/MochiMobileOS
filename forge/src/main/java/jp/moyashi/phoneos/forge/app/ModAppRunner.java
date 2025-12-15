package jp.moyashi.phoneos.forge.app;

import jp.moyashi.phoneos.api.AppContext;
import jp.moyashi.phoneos.api.IModApplication;
import jp.moyashi.phoneos.api.ModScreen;
import jp.moyashi.phoneos.api.proxy.AppContextProxy;
import jp.moyashi.phoneos.api.proxy.IPCChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.core.PGraphics;

import java.util.Stack;

/**
 * MODアプリケーション実行管理。
 * Forge JVMでMODアプリを実行し、描画結果をServer JVMに転送する。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class ModAppRunner implements AppContextProxy.ScreenNavigationCallback {

    private static final Logger LOGGER = LogManager.getLogger(ModAppRunner.class);

    /** アプリケーション */
    private final IModApplication application;

    /** IPCチャンネル */
    private final IPCChannel channel;

    /** AppContext */
    private final AppContextProxy context;

    /** 画面スタック */
    private final Stack<ModScreen> screenStack = new Stack<>();

    /** 現在の画面 */
    private ModScreen currentScreen;

    /** 実行中フラグ */
    private volatile boolean running = false;

    /** 一時停止フラグ */
    private volatile boolean paused = false;

    /**
     * ModAppRunnerを作成する。
     *
     * @param application 実行するアプリケーション
     * @param channel IPCチャンネル
     */
    public ModAppRunner(IModApplication application, IPCChannel channel) {
        this.application = application;
        this.channel = channel;
        this.context = new AppContextProxy(channel, application.getAppId(), application.getName());
        this.context.setScreenNavigationCallback(this);
    }

    /**
     * アプリケーションを開始する。
     */
    public void start() {
        if (running) {
            LOGGER.warn("[ModAppRunner] App already running: {}", application.getName());
            return;
        }

        LOGGER.info("[ModAppRunner] Starting app: {}", application.getName());

        try {
            // アプリを初期化
            application.onInitialize(context);

            // エントリ画面を取得
            currentScreen = application.getEntryScreen(context);
            if (currentScreen != null) {
                currentScreen.onCreate(context);
                currentScreen.onResume();
            }

            running = true;
            LOGGER.info("[ModAppRunner] App started: {}", application.getName());

        } catch (Exception e) {
            LOGGER.error("[ModAppRunner] Error starting app: {}", application.getName(), e);
        }
    }

    /**
     * アプリケーションを停止する。
     */
    public void stop() {
        if (!running) return;

        LOGGER.info("[ModAppRunner] Stopping app: {}", application.getName());

        try {
            // 全画面を破棄
            while (!screenStack.isEmpty()) {
                ModScreen screen = screenStack.pop();
                screen.onPause();
                screen.onDestroy();
            }

            if (currentScreen != null) {
                currentScreen.onPause();
                currentScreen.onDestroy();
                currentScreen = null;
            }

            // アプリを終了
            application.onTerminate();

            running = false;
            LOGGER.info("[ModAppRunner] App stopped: {}", application.getName());

        } catch (Exception e) {
            LOGGER.error("[ModAppRunner] Error stopping app: {}", application.getName(), e);
        }
    }

    /**
     * 一時停止する。
     */
    public void pause() {
        if (!running || paused) return;

        paused = true;
        application.onPause();
        if (currentScreen != null) {
            currentScreen.onPause();
        }
    }

    /**
     * 再開する。
     */
    public void resume() {
        if (!running || !paused) return;

        paused = false;
        application.onResume();
        if (currentScreen != null) {
            currentScreen.onResume();
        }
    }

    /**
     * 画面を描画する。
     *
     * @param g Processingグラフィックスコンテキスト
     */
    public void render(PGraphics g) {
        if (!running || paused || currentScreen == null) return;

        try {
            currentScreen.render(g);
        } catch (Exception e) {
            LOGGER.error("[ModAppRunner] Error rendering screen", e);
        }
    }

    /**
     * タッチ開始イベントを処理する。
     */
    public boolean onTouchStart(int x, int y) {
        if (!running || paused || currentScreen == null) return false;
        return currentScreen.onTouchStart(x, y);
    }

    /**
     * タッチ移動イベントを処理する。
     */
    public boolean onTouchMove(int x, int y) {
        if (!running || paused || currentScreen == null) return false;
        return currentScreen.onTouchMove(x, y);
    }

    /**
     * タッチ終了イベントを処理する。
     */
    public boolean onTouchEnd(int x, int y) {
        if (!running || paused || currentScreen == null) return false;
        return currentScreen.onTouchEnd(x, y);
    }

    /**
     * スクロールイベントを処理する。
     */
    public boolean onScroll(int x, int y, float delta) {
        if (!running || paused || currentScreen == null) return false;
        return currentScreen.onScroll(x, y, delta);
    }

    /**
     * キー入力イベントを処理する。
     */
    public boolean onKeyTyped(int keyCode, char keyChar) {
        if (!running || paused || currentScreen == null) return false;
        return currentScreen.onKeyTyped(keyCode, keyChar);
    }

    /**
     * 戻るボタンを処理する。
     *
     * @return trueの場合、イベントを消費した
     */
    public boolean onBackPressed() {
        if (!running || paused || currentScreen == null) return false;

        // 画面に戻るボタン処理を委譲
        if (currentScreen.onBackPressed()) {
            return true;
        }

        // スタックに画面があれば戻る
        if (!screenStack.isEmpty()) {
            popScreenInternal();
            return true;
        }

        // スタックが空の場合はアプリ終了を示す
        return false;
    }

    // ScreenNavigationCallback実装

    @Override
    public void onPushScreen(ModScreen screen) {
        if (currentScreen != null) {
            currentScreen.onPause();
            screenStack.push(currentScreen);
        }

        currentScreen = screen;
        currentScreen.onCreate(context);
        currentScreen.onResume();

        LOGGER.debug("[ModAppRunner] Pushed screen: {}", screen.getTitle());
    }

    @Override
    public void onPopScreen() {
        popScreenInternal();
    }

    @Override
    public void onReplaceScreen(ModScreen screen) {
        if (currentScreen != null) {
            currentScreen.onPause();
            currentScreen.onDestroy();
        }

        currentScreen = screen;
        currentScreen.onCreate(context);
        currentScreen.onResume();

        LOGGER.debug("[ModAppRunner] Replaced screen: {}", screen.getTitle());
    }

    private void popScreenInternal() {
        if (currentScreen != null) {
            currentScreen.onPause();
            currentScreen.onDestroy();
        }

        if (!screenStack.isEmpty()) {
            currentScreen = screenStack.pop();
            currentScreen.onResume();
            LOGGER.debug("[ModAppRunner] Popped to screen: {}", currentScreen.getTitle());
        } else {
            currentScreen = null;
        }
    }

    // Getter

    public IModApplication getApplication() {
        return application;
    }

    public AppContext getContext() {
        return context;
    }

    public ModScreen getCurrentScreen() {
        return currentScreen;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }
}
