package jp.moyashi.phoneos.core.apps.launcher;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen;
import jp.moyashi.phoneos.core.apps.launcher.ui.SimpleHomeScreen;
import jp.moyashi.phoneos.core.apps.launcher.ui.BasicHomeScreen;
import jp.moyashi.phoneos.core.apps.launcher.ui.SafeHomeScreen;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PGraphics;

/**
 * MochiMobileOS用の組み込みランチャーアプリケーション。
 * これはホーム画面を管理し、システム内の他のすべてのアプリケーションへの
 * アクセスを提供する主要なインターフェースアプリケーション。
 * 
 * LauncherAppの責務:
 * - ホーム画面でのアプリショートカット表示
 * - アプリライブラリーへのアクセス提供
 * - アプリの整理とショートカット管理
 * - OSのデフォルトエントリーポイントとしての機能
 * 
 * このアプリケーションはOS初期化時にカーネルによって自動的に登録され、
 * OSの開始時にデフォルト画面として機能する。
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class LauncherApp implements IApplication {
    
    /** アプリケーション表示名 */
    private static final String APP_NAME = "Launcher";
    
    /** アプリケーションの一意識別子 */
    private static final String APP_ID = "jp.moyashi.phoneos.core.apps.launcher";
    
    /** アプリケーションバージョン */
    private static final String APP_VERSION = "1.0.0";
    
    /** アプリケーションの説明 */
    private static final String APP_DESCRIPTION = "System launcher and app manager";
    
    /** メインホームスクリーンインスタンスへの参照 */
    private Screen homeScreen; // HomeScreenとSimpleHomeScreenの両方をサポートするためScreenインターフェースに変更
    
    /**
     * Constructs a new LauncherApp instance.
     */
    public LauncherApp() {
    }
    
    /**
     * Gets the display name of the launcher application.
     * 
     * @return The application name "Launcher"
     */
    @Override
    public String getName() {
        return APP_NAME;
    }
    
    // getIcon()はデフォルト実装（null返却）を使用し、システムが白いアイコンを生成
    
    /**
     * Gets the main entry screen for the launcher application.
     * Returns the home screen that displays app shortcuts and provides
     * navigation to the app library.
     * 
     * @param kernel The OS kernel instance providing system services access
     * @return The HomeScreen instance for this launcher
     */
    @Override
    public Screen getEntryScreen(Kernel kernel) {
        if (homeScreen == null) {
            // Progressive feature testing: Simple -> Basic -> Safe -> Advanced
            String screenMode = "advanced"; // Options: "simple", "basic", "safe", "advanced"
            
            switch (screenMode) {
                case "simple":
                    homeScreen = new SimpleHomeScreen(kernel);
                    break;
                case "basic":
                    homeScreen = new BasicHomeScreen(kernel);
                    break;
                case "safe":
                    homeScreen = new SafeHomeScreen(kernel);
                    break;
                case "advanced":
                default:
                    homeScreen = new HomeScreen(kernel);
                    break;
            }
        }
        return homeScreen;
    }
    
    /**
     * Gets the unique identifier for the launcher application.
     * 
     * @return The launcher application ID
     */
    @Override
    public String getApplicationId() {
        return APP_ID;
    }
    
    /**
     * Gets the version of the launcher application.
     * 
     * @return The application version string
     */
    @Override
    public String getVersion() {
        return APP_VERSION;
    }
    
    /**
     * Gets the description of the launcher application.
     * 
     * @return The application description
     */
    @Override
    public String getDescription() {
        return APP_DESCRIPTION;
    }
    
    /**
     * Called when the launcher application is initialized.
     * Performs any necessary setup for the launcher functionality.
     * 
     * @param kernel The OS kernel instance
     */
    @Override
    public void onInitialize(Kernel kernel) {
    }
    
    /**
     * Called when the launcher application is being destroyed.
     * Cleans up any resources used by the launcher.
     */
    @Override
    public void onDestroy() {
        if (homeScreen != null) {
            homeScreen.cleanup((processing.core.PGraphics) null); // LauncherApp onDestroy context - no PGraphics available
            homeScreen = null;
        }
    }
    
    /**
     * Gets the home screen instance if it has been created.
     * 
     * @return The HomeScreen instance, or null if not yet created
     */
    public HomeScreen getHomeScreen() {
        return (HomeScreen) homeScreen;
    }
    
    /**
     * Checks if this launcher application is the system default.
     * Since this is the built-in launcher, it always returns true.
     * 
     * @return true, as this is the system launcher
     */
    public boolean isSystemLauncher() {
        return true;
    }
}