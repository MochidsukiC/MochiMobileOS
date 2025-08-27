package jp.moyashi.phoneos.core.apps.launcher;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PGraphics;

/**
 * The built-in Launcher application for MochiMobileOS.
 * This is the primary interface application that manages the home screen
 * and provides access to all other applications in the system.
 * 
 * The LauncherApp is responsible for:
 * - Displaying app shortcuts on the home screen
 * - Providing access to the app library
 * - Managing app organization and shortcuts
 * - Serving as the default entry point for the OS
 * 
 * This application is automatically registered by the Kernel during OS initialization
 * and serves as the default screen when the OS starts.
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class LauncherApp implements IApplication {
    
    /** The application display name */
    private static final String APP_NAME = "Launcher";
    
    /** The application unique identifier */
    private static final String APP_ID = "jp.moyashi.phoneos.core.apps.launcher";
    
    /** The application version */
    private static final String APP_VERSION = "1.0.0";
    
    /** The application description */
    private static final String APP_DESCRIPTION = "System launcher and app manager";
    
    /** Reference to the main home screen instance */
    private HomeScreen homeScreen;
    
    /**
     * Constructs a new LauncherApp instance.
     */
    public LauncherApp() {
        System.out.println("LauncherApp: Launcher application created");
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
            homeScreen = new HomeScreen(kernel);
            System.out.println("LauncherApp: Created home screen instance");
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
        System.out.println("LauncherApp: Initializing launcher with " + 
                          kernel.getAppLoader().getLoadedAppCount() + " available apps");
    }
    
    /**
     * Called when the launcher application is being destroyed.
     * Cleans up any resources used by the launcher.
     */
    @Override
    public void onDestroy() {
        System.out.println("LauncherApp: Launcher application shutting down");
        if (homeScreen != null) {
            homeScreen.cleanup();
            homeScreen = null;
        }
    }
    
    /**
     * Gets the home screen instance if it has been created.
     * 
     * @return The HomeScreen instance, or null if not yet created
     */
    public HomeScreen getHomeScreen() {
        return homeScreen;
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