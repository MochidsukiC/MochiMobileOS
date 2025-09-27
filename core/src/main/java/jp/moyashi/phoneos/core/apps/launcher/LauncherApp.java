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
        System.out.println("LauncherApp: ランチャーアプリケーションを作成");
    }
    
    /**
     * Gets the application name.
     *
     * @return The application name "Launcher"
     */
    @Override
    public String getApplicationName() {
        return APP_NAME;
    }

    /**
     * Gets the application version.
     *
     * @return The application version
     */
    @Override
    public String getApplicationVersion() {
        return APP_VERSION;
    }

    /**
     * Legacy method for compatibility.
     *
     * @return The application name "Launcher"
     */
    public String getName() {
        return getApplicationName();
    }
    
    /**
     * Gets the icon for the launcher application.
     * Creates a simple square icon with launcher-themed graphics.
     * 
     * @param p The PApplet instance for creating the icon
     * @return A PImage representing the launcher icon
     */
    @Override
    public PImage getIcon(PApplet p) {
        // Create a 64x64 icon for the launcher using PGraphics
        PGraphics icon = p.createGraphics(64, 64);
        
        // Begin drawing to the icon
        icon.beginDraw();
        
        // Clear background
        icon.background(0x2E3440); // Dark blue-gray background
        
        // Draw launcher icon - a 3x3 grid representing app icons
        icon.fill(0xFFFFFF); // White
        icon.noStroke();
        
        // Draw 3x3 grid of small squares
        int squareSize = 8;
        int spacing = 4;
        int startX = (64 - (3 * squareSize + 2 * spacing)) / 2;
        int startY = (64 - (3 * squareSize + 2 * spacing)) / 2;
        
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = startX + col * (squareSize + spacing);
                int y = startY + row * (squareSize + spacing);
                icon.rect(x, y, squareSize, squareSize, 2); // Rounded corners
            }
        }
        
        // End drawing
        icon.endDraw();
        
        System.out.println("LauncherApp: Generated launcher icon");
        return icon;
    }

    /**
     * Gets the icon for the launcher application (PGraphics version).
     * Creates a simple square icon with launcher-themed graphics.
     *
     * @param g The PGraphics instance for creating the icon
     * @return A PImage representing the launcher icon
     */
    @Override
    public PImage getIcon(PGraphics g) {
        // PGraphics doesn't have createGraphics method
        // Use the parent PApplet if available
        if (g.parent != null) {
            return getIcon(g.parent);
        }

        System.out.println("LauncherApp: Warning - PGraphics has no parent, cannot create icon");
        return null;
    }
    
    /**
     * Creates the main screen for the launcher application.
     * Returns the home screen that displays app shortcuts and provides
     * navigation to the app library.
     *
     * @return The HomeScreen instance for this launcher
     */
    public Screen createMainScreen() {
        return createMainScreen(null);
    }

    /**
     * Creates the main screen for the launcher application with kernel.
     * Returns the home screen that displays app shortcuts and provides
     * navigation to the app library.
     *
     * @param kernel The kernel instance
     * @return The HomeScreen instance for this launcher
     */
    public Screen createMainScreen(Kernel kernel) {
        if (homeScreen == null) {
            // Progressive feature testing: Simple -> Basic -> Safe -> Advanced
            String screenMode = "advanced"; // Options: "simple", "basic", "safe", "advanced"

            switch (screenMode) {
                case "simple":
                    System.out.println("🔧 LauncherApp: Creating SIMPLE home screen for debugging...");
                    homeScreen = new SimpleHomeScreen(kernel);
                    System.out.println("✅ LauncherApp: Simple home screen created!");
                    break;
                case "basic":
                    System.out.println("🏠 LauncherApp: Creating BASIC functional home screen...");
                    homeScreen = new BasicHomeScreen(kernel);
                    System.out.println("✅ LauncherApp: Basic home screen created!");
                    break;
                case "safe":
                    System.out.println("🛡️ LauncherApp: Creating SAFE home screen with error handling...");
                    homeScreen = new SafeHomeScreen(kernel);
                    System.out.println("✅ LauncherApp: Safe home screen created!");
                    break;
                case "advanced":
                default:
                    System.out.println("🚀 LauncherApp: Creating ADVANCED multi-page home screen...");
                    homeScreen = new HomeScreen(kernel);
                    System.out.println("✅ LauncherApp: Advanced home screen created!");
                    break;
            }
        }
        return homeScreen;
    }

    /**
     * Legacy method for compatibility.
     * Gets the main entry screen for the launcher application.
     *
     * @param kernel The OS kernel instance providing system services access
     * @return The HomeScreen instance for this launcher
     */
    public Screen getEntryScreen(Kernel kernel) {
        return createMainScreen(kernel);
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
     * Legacy method for compatibility.
     * Gets the version of the launcher application.
     *
     * @return The application version string
     */
    public String getVersion() {
        return getApplicationVersion();
    }

    /**
     * Initializes the launcher application.
     * Performs any necessary setup for the launcher functionality.
     */
    @Override
    public void initialize() {
        if (Kernel.getInstance() != null && Kernel.getInstance().getAppLoader() != null) {
            System.out.println("LauncherApp: Initializing launcher with " +
                    Kernel.getInstance().getAppLoader().getLoadedAppCount() + " available apps");
        } else {
            System.out.println("LauncherApp: Initializing launcher (Kernel/AppLoader not ready)");
        }
    }

    /**
     * Called when the launcher application is installed.
     * Performs initial setup tasks.
     */
    @Override
    public void onInstall() {
        System.out.println("LauncherApp: Launcher application installed");
    }

    /**
     * Called when the launcher application is being terminated.
     * Cleans up any resources used by the launcher.
     */
    @Override
    public void terminate() {
        System.out.println("LauncherApp: Launcher application shutting down");
        if (homeScreen != null) {
            homeScreen.cleanup(null); // LauncherApp terminate context - no PApplet available
            homeScreen = null;
        }
    }

    /**
     * Legacy method for compatibility.
     * Called when the launcher application is initialized.
     *
     * @param kernel The OS kernel instance
     */
    public void onInitialize(Kernel kernel) {
        initialize();
    }

    /**
     * Legacy method for compatibility.
     * Called when the launcher application is being destroyed.
     */
    public void onDestroy() {
        terminate();
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