package jp.moyashi.phoneos.core.apps.settings;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.apps.settings.ui.SettingsScreen;
import processing.core.PGraphics;
import processing.core.PImage;

/**
 * Settings application for MochiMobileOS.
 * Provides access to system settings and configuration options.
 * This serves as a test application for the launcher and home screen functionality.
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class SettingsApp implements IApplication {
    
    /** Application metadata */
    private static final String APP_ID = "jp.moyashi.phoneos.core.apps.settings";
    private static final String APP_NAME = "Settings";
    private static final String APP_VERSION = "1.0.0";
    private static final String APP_DESCRIPTION = "System settings and configuration";
    
    /** Initialization state */
    private boolean isInitialized = false;
    
    /**
     * Creates a new Settings application instance.
     */
    public SettingsApp() {
        System.out.println("SettingsApp: Settings application created");
    }
    
    /**
     * Gets the unique identifier for this application.
     * 
     * @return The application ID
     */
    @Override
    public String getApplicationId() {
        return APP_ID;
    }
    
    /**
     * Gets the display name of this application.
     * 
     * @return The application name
     */
    @Override
    public String getName() {
        return APP_NAME;
    }
    
    /**
     * Gets the version of this application.
     * 
     * @return The application version
     */
    @Override
    public String getVersion() {
        return APP_VERSION;
    }
    
    /**
     * Gets the description of this application.
     * 
     * @return The application description
     */
    @Override
    public String getDescription() {
        return APP_DESCRIPTION;
    }
    
    /**
     * Gets the icon for this application.
     * Creates a simple gear-like icon to represent settings.
     * 
     * @param p The PApplet instance for drawing operations
     * @return The application icon
     */
    @Override
    public PImage getIcon(processing.core.PApplet p) {
        // Create graphics buffer for icon
        PGraphics icon = p.createGraphics(64, 64);
        
        icon.beginDraw();
        icon.background(0x666666); // Gray background
        icon.noStroke();
        
        // Draw gear shape
        icon.fill(0xFFFFFF); // White gear
        
        // Outer gear circle
        icon.ellipse(32, 32, 40, 40);
        
        // Inner hole
        icon.fill(0x666666);
        icon.ellipse(32, 32, 16, 16);
        
        // Gear teeth (simplified)
        icon.fill(0xFFFFFF);
        icon.rect(30, 8, 4, 12);   // Top
        icon.rect(30, 44, 4, 12);  // Bottom
        icon.rect(8, 30, 12, 4);   // Left
        icon.rect(44, 30, 12, 4);  // Right
        
        // Diagonal teeth
        icon.rect(18, 14, 8, 3);   // Top-left
        icon.rect(38, 14, 8, 3);   // Top-right
        icon.rect(18, 47, 8, 3);   // Bottom-left
        icon.rect(38, 47, 8, 3);   // Bottom-right
        
        icon.endDraw();
        
        return icon;
    }
    
    /**
     * Gets the entry screen for this application.
     * 
     * @param kernel The OS kernel instance
     * @return The main screen of the settings app
     */
    @Override
    public Screen getEntryScreen(Kernel kernel) {
        System.out.println("SettingsApp: Creating settings screen");
        return new SettingsScreen(kernel, this);
    }
    
    /**
     * Initializes the settings application.
     * Called when the application is first loaded.
     */
    @Override
    public void onInitialize(Kernel kernel) {
        if (!isInitialized) {
            isInitialized = true;
            System.out.println("SettingsApp: Settings application initialized");
        }
    }
    
    /**
     * Cleans up the settings application.
     * Called when the application is being unloaded.
     */
    @Override
    public void onDestroy() {
        if (isInitialized) {
            isInitialized = false;
            System.out.println("SettingsApp: Settings application destroyed");
        }
    }
    
    /**
     * Checks if the application is initialized.
     * 
     * @return true if initialized, false otherwise
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}