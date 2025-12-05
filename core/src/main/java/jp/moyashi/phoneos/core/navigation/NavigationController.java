package jp.moyashi.phoneos.core.navigation;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.ScreenManager;
import jp.moyashi.phoneos.core.apps.launcher.LauncherApp;
import jp.moyashi.phoneos.core.ui.lock.LockScreen;
import jp.moyashi.phoneos.core.event.EventBus;
import jp.moyashi.phoneos.core.event.ui.ScreenEvent;
import jp.moyashi.phoneos.core.event.ui.ScreenEvent.TransitionType;

import java.util.Stack;
import java.util.logging.Logger;

/**
 * 画面遷移を管理するコントローラー。
 * ScreenManagerのラッパーとして機能し、画面のプッシュ/ポップ/切り替えを管理する。
 * Kernelから画面遷移の責任を分離。
 *
 * 機能:
 * - 画面のプッシュ/ポップ/切り替え
 * - 画面スタックの管理
 * - ホーム画面への遷移
 * - ロック画面への遷移
 * - 画面履歴の管理
 *
 * @since 2025-12-02
 * @version 1.0
 */
public class NavigationController {

    private static final Logger logger = Logger.getLogger(NavigationController.class.getName());

    /** Kernelインスタンス */
    private final Kernel kernel;

    /** ScreenManagerインスタンス */
    private ScreenManager screenManager;

    /** 画面履歴（戻るボタン用） */
    private final Stack<Screen> navigationHistory = new Stack<>();

    /** ホーム画面のキャッシュ */
    private Screen homeScreen;

    /** 現在の画面 */
    private Screen currentScreen;

    /**
     * NavigationControllerを初期化する。
     *
     * @param kernel Kernelインスタンス
     */
    public NavigationController(Kernel kernel) {
        this.kernel = kernel;
        logger.info("NavigationController initialized");
    }

    /**
     * ScreenManagerを設定する。
     *
     * @param screenManager ScreenManagerインスタンス
     */
    public void setScreenManager(ScreenManager screenManager) {
        this.screenManager = screenManager;
    }

    /**
     * 新しい画面をプッシュする。
     * 現在の画面は履歴に保存される。
     *
     * @param screen プッシュする画面
     * @return プッシュに成功した場合 true
     */
    public boolean pushScreen(Screen screen) {
        if (screen == null) {
            logger.warning("Cannot push null screen");
            return false;
        }

        if (screenManager == null) {
            logger.severe("ScreenManager not set");
            return false;
        }

        // 現在の画面を履歴に追加
        if (currentScreen != null) {
            navigationHistory.push(currentScreen);
        }

        // 画面遷移開始イベントを発行
        ScreenEvent transitionStart = ScreenEvent.transitionStart(
            currentScreen, screen, TransitionType.PUSH);
        EventBus.getInstance().post(transitionStart);

        // キャンセルされた場合は中止
        if (transitionStart.isCancelled()) {
            logger.info("Screen transition cancelled by listener");
            return false;
        }

        // 画面をプッシュ
        screenManager.pushScreen(screen);
        Screen previousScreen = currentScreen;
        currentScreen = screen;

        // 画面遷移完了イベントを発行
        EventBus.getInstance().post(ScreenEvent.transitionEnd(
            previousScreen, screen, TransitionType.PUSH));

        logger.info("Pushed screen: " + screen.getClass().getSimpleName());
        return true;
    }

    /**
     * 現在の画面をポップして前の画面に戻る。
     *
     * @return ポップに成功した場合 true
     */
    public boolean popScreen() {
        if (screenManager == null) {
            logger.severe("ScreenManager not set");
            return false;
        }

        if (navigationHistory.isEmpty()) {
            logger.info("No screen to pop back to");
            return false;
        }

        // 前の画面を取得
        Screen previousScreen = navigationHistory.peek();

        // 画面遷移開始イベントを発行
        ScreenEvent transitionStart = ScreenEvent.transitionStart(
            currentScreen, previousScreen, TransitionType.POP);
        EventBus.getInstance().post(transitionStart);

        // キャンセルされた場合は中止
        if (transitionStart.isCancelled()) {
            logger.info("Screen pop cancelled by listener");
            return false;
        }

        // 現在の画面をポップ
        screenManager.popScreen();

        // 履歴から前の画面を取得
        Screen oldScreen = currentScreen;
        currentScreen = navigationHistory.pop();

        // 画面遷移完了イベントを発行
        EventBus.getInstance().post(ScreenEvent.transitionEnd(
            oldScreen, currentScreen, TransitionType.POP));

        logger.info("Popped back to screen: " +
                   (currentScreen != null ? currentScreen.getClass().getSimpleName() : "null"));
        return true;
    }

    /**
     * 指定した画面に切り替える。
     * 履歴はクリアされる。
     *
     * @param screen 切り替え先の画面
     * @return 切り替えに成功した場合 true
     */
    public boolean switchToScreen(Screen screen) {
        if (screen == null) {
            logger.warning("Cannot switch to null screen");
            return false;
        }

        if (screenManager == null) {
            logger.severe("ScreenManager not set");
            return false;
        }

        // 履歴をクリア
        navigationHistory.clear();

        // 全画面をクリアして新しい画面を設定
        screenManager.clearAllScreens();
        screenManager.pushScreen(screen);
        currentScreen = screen;

        logger.info("Switched to screen: " + screen.getClass().getSimpleName());
        return true;
    }

    /**
     * ホーム画面に遷移する。
     *
     * @return 遷移に成功した場合 true
     */
    public boolean goToHome() {
        if (homeScreen == null) {
            // ホーム画面を初期化
            LauncherApp launcherApp = new LauncherApp();
            homeScreen = launcherApp.getEntryScreen(kernel);
        }

        logger.info("Navigating to home screen");
        return switchToScreen(homeScreen);
    }

    /**
     * ロック画面に遷移する。
     *
     * @return 遷移に成功した場合 true
     */
    public boolean goToLockScreen() {
        LockScreen lockScreen = new LockScreen(kernel);
        logger.info("Navigating to lock screen");
        return switchToScreen(lockScreen);
    }

    /**
     * 全ての画面をクリアする。
     */
    public void clearAllScreens() {
        if (screenManager != null) {
            screenManager.clearAllScreens();
            navigationHistory.clear();
            currentScreen = null;
            logger.info("Cleared all screens");
        }
    }

    /**
     * 現在の画面を取得する。
     *
     * @return 現在の画面、ない場合は null
     */
    public Screen getCurrentScreen() {
        return currentScreen;
    }

    /**
     * 戻ることができるかを判定する。
     *
     * @return 戻ることができる場合 true
     */
    public boolean canGoBack() {
        return !navigationHistory.isEmpty();
    }

    /**
     * 画面履歴のサイズを取得する。
     *
     * @return 履歴サイズ
     */
    public int getHistorySize() {
        return navigationHistory.size();
    }

    /**
     * 特定の画面タイプが履歴に存在するかを確認する。
     *
     * @param screenClass 検索する画面クラス
     * @return 存在する場合 true
     */
    public boolean isInHistory(Class<? extends Screen> screenClass) {
        for (Screen screen : navigationHistory) {
            if (screenClass.isInstance(screen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 特定の画面まで戻る。
     * 指定した画面タイプが見つかるまでポップを続ける。
     *
     * @param screenClass 戻り先の画面クラス
     * @return 成功した場合 true
     */
    public boolean popToScreen(Class<? extends Screen> screenClass) {
        while (!navigationHistory.isEmpty()) {
            Screen screen = navigationHistory.peek();
            if (screenClass.isInstance(screen)) {
                return popScreen();
            }
            navigationHistory.pop(); // 履歴から削除
        }
        return false;
    }

    /**
     * ホーム画面を設定する。
     *
     * @param homeScreen ホーム画面
     */
    public void setHomeScreen(Screen homeScreen) {
        this.homeScreen = homeScreen;
        logger.info("Home screen set: " +
                   (homeScreen != null ? homeScreen.getClass().getSimpleName() : "null"));
    }

    /**
     * 画面遷移アニメーションを有効/無効にする（将来の拡張用）。
     *
     * @param enabled アニメーションを有効にする場合 true
     */
    public void setTransitionAnimationEnabled(boolean enabled) {
        // TODO: 将来的にアニメーション機能を実装
        logger.fine("Transition animation " + (enabled ? "enabled" : "disabled"));
    }
}