package jp.moyashi.phoneos.core;

import jp.moyashi.phoneos.core.service.*;
import jp.moyashi.phoneos.core.ui.ScreenManager;
import jp.moyashi.phoneos.core.apps.launcher.LauncherApp;
import processing.core.PApplet;

/**
 * The main OS kernel that serves as the heart of the phone OS.
 * This class extends PApplet to utilize Processing's graphics capabilities.
 * It manages all system services and the GUI through the ScreenManager.
 * 
 * @author YourName
 * @version 1.0
 */
public class Kernel extends PApplet {
    
    /** Screen manager for handling UI and screen transitions */
    private ScreenManager screenManager;
    
    /** Virtual file system service */
    private VFS vfs;
    
    /** Settings management service */
    private SettingsManager settingsManager;
    
    /** System clock service */
    private SystemClock systemClock;
    
    /** Application loader service */
    private AppLoader appLoader;
    
    /**
     * Configures Processing settings before setup() is called.
     * Sets the display size and renderer.
     */
    @Override
    public void settings() {
        size(400, 600);  // Smartphone-like aspect ratio
        System.out.println("Kernel: Processing settings configured");
    }
    
    /**
     * Initializes the OS kernel and all its services.
     * This method is called once when the program starts.
     * Creates instances of all core services and the screen manager.
     */
    @Override
    public void setup() {
        System.out.println("Kernel: Initializing OS services...");
        
        // Initialize core services
        vfs = new VFS();
        settingsManager = new SettingsManager();
        systemClock = new SystemClock();
        appLoader = new AppLoader(vfs);
        
        // Scan for and load applications
        appLoader.scanForApps();
        
        // Register built-in launcher application
        LauncherApp launcherApp = new LauncherApp();
        appLoader.registerApplication(launcherApp);
        
        // Initialize screen manager and set initial screen to launcher
        screenManager = new ScreenManager();
        screenManager.pushScreen(launcherApp.getEntryScreen(this));
        
        System.out.println("Kernel: OS initialization complete");
    }
    
    /**
     * Main drawing loop called continuously by Processing.
     * Delegates drawing to the current screen through the screen manager.
     */
    @Override
    public void draw() {
        if (screenManager != null) {
            screenManager.draw(this);
        }
    }
    
    /**
     * Handles mouse press events.
     * Delegates event handling to the current screen through the screen manager.
     */
    @Override
    public void mousePressed() {
        if (screenManager != null) {
            screenManager.mousePressed(mouseX, mouseY);
        }
    }
    
    /**
     * Gets the virtual file system service.
     * @return The VFS instance
     */
    public VFS getVFS() {
        return vfs;
    }
    
    /**
     * Gets the settings manager service.
     * @return The SettingsManager instance
     */
    public SettingsManager getSettingsManager() {
        return settingsManager;
    }
    
    /**
     * Gets the system clock service.
     * @return The SystemClock instance
     */
    public SystemClock getSystemClock() {
        return systemClock;
    }
    
    /**
     * Gets the screen manager.
     * @return The ScreenManager instance
     */
    public ScreenManager getScreenManager() {
        return screenManager;
    }
    
    /**
     * Gets the application loader service.
     * @return The AppLoader instance
     */
    public AppLoader getAppLoader() {
        return appLoader;
    }
}