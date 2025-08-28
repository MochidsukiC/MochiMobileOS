package jp.moyashi.phoneos.core;

import jp.moyashi.phoneos.core.service.*;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.ScreenManager;
import jp.moyashi.phoneos.core.apps.launcher.LauncherApp;
import jp.moyashi.phoneos.core.apps.settings.SettingsApp;
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
        System.out.println("📱 Kernel: Processing window configured (400x600)");
    }
    
    /**
     * Initializes the OS kernel and all its services.
     * This method is called once when the program starts.
     * Creates instances of all core services and the screen manager.
     */
    @Override
    public void setup() {
        // CRITICAL FIX: Set frame rate immediately
        frameRate(60);
        
        System.out.println("=== MochiMobileOS Kernel Initialization ===");
        System.out.println("Kernel: Initializing OS services...");
        System.out.println("Kernel: Frame rate set to 60 FPS");
        
        // Initialize core services
        System.out.println("  -> Creating VFS (Virtual File System)...");
        vfs = new VFS();
        
        System.out.println("  -> Creating Settings Manager...");
        settingsManager = new SettingsManager();
        
        System.out.println("  -> Creating System Clock...");
        systemClock = new SystemClock();
        
        System.out.println("  -> Creating Application Loader...");
        appLoader = new AppLoader(vfs);
        
        // Scan for and load applications
        System.out.println("  -> Scanning for external applications...");
        appLoader.scanForApps();
        
        // Register built-in applications
        System.out.println("  -> Registering LauncherApp...");
        LauncherApp launcherApp = new LauncherApp();
        appLoader.registerApplication(launcherApp);
        launcherApp.onInitialize(this);
        
        System.out.println("  -> Registering SettingsApp...");
        SettingsApp settingsApp = new SettingsApp();
        appLoader.registerApplication(settingsApp);
        settingsApp.onInitialize(this);
        
        System.out.println("Kernel: Registered " + appLoader.getLoadedApps().size() + " applications");
        
        // Initialize screen manager and set initial screen to launcher
        System.out.println("  -> Creating Screen Manager...");
        screenManager = new ScreenManager();
        System.out.println("✅ ScreenManager created: " + (screenManager != null));
        
        System.out.println("▶️ Starting LauncherApp as initial screen...");
        Screen launcherScreen = launcherApp.getEntryScreen(this);
        System.out.println("✅ LauncherApp screen obtained: " + (launcherScreen != null));
        if (launcherScreen != null) {
            System.out.println("   Screen title: " + launcherScreen.getScreenTitle());
        }
        
        screenManager.pushScreen(launcherScreen);
        System.out.println("✅ Screen pushed to ScreenManager");
        
        System.out.println("✅ Kernel: OS initialization complete!");
        System.out.println("    • LauncherApp is now running");
        System.out.println("    • " + appLoader.getLoadedApps().size() + " applications available");
        System.out.println("    • System ready for user interaction");
        System.out.println("===========================================");
    }
    
    /**
     * Main drawing loop called continuously by Processing.
     * Delegates drawing to the current screen through the screen manager.
     */
    @Override
    public void draw() {
        // FORCE bright background to ensure something is visible
        background(100, 200, 100); // Bright green to ensure visibility
        
        // Debug logging with more detail
        if (frameCount <= 10 || frameCount % 60 == 0) {
            System.out.println("🎨 Kernel Frame " + frameCount + ": ScreenManager=" + (screenManager != null));
            if (screenManager != null) {
                System.out.println("   ScreenManager has current screen: " + (screenManager.getCurrentScreen() != null));
                if (screenManager.getCurrentScreen() != null) {
                    System.out.println("   Current screen: " + screenManager.getCurrentScreen().getScreenTitle());
                }
            }
        }
        
        // ALWAYS draw debug info to verify Kernel draw() is being called
        fill(255, 255, 255);
        textAlign(LEFT, TOP);
        textSize(14);
        text("Kernel Frame: " + frameCount, 10, 10);
        text("ScreenManager: " + (screenManager != null), 10, 30);
        
        if (screenManager != null) {
            text("Has Screen: " + (screenManager.getCurrentScreen() != null), 10, 50);
            try {
                screenManager.draw(this);
            } catch (Exception e) {
                System.err.println("❌ ScreenManager draw error: " + e.getMessage());
                e.printStackTrace();
                // BIG error display
                fill(255, 0, 0);
                rect(50, height/2 - 50, width - 100, 100);
                fill(255, 255, 255);
                textAlign(CENTER, CENTER);
                textSize(18);
                text("SCREEN ERROR!", width/2, height/2 - 20);
                textSize(12);
                text("Error: " + e.getMessage(), width/2, height/2);
                text("Check console for details", width/2, height/2 + 20);
            }
        } else {
            // BIG loading indicator
            fill(255, 255, 0);
            rect(50, height/2 - 30, width - 100, 60);
            fill(0);
            textAlign(CENTER, CENTER);
            textSize(18);
            text("NO SCREEN MANAGER!", width/2, height/2);
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
     * Handles mouse drag events.
     * Delegates event handling to the current screen through the screen manager.
     */
    @Override
    public void mouseDragged() {
        if (screenManager != null) {
            screenManager.mouseDragged(mouseX, mouseY);
        }
    }
    
    /**
     * Handles mouse release events.
     * Delegates event handling to the current screen through the screen manager.
     */
    @Override
    public void mouseReleased() {
        if (screenManager != null) {
            screenManager.mouseReleased(mouseX, mouseY);
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