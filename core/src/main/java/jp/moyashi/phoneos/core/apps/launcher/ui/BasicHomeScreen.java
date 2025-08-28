package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.app.IApplication;
import processing.core.PApplet;

import java.util.List;
import java.util.ArrayList;

/**
 * 基本的なランチャー機能を持つ基本的な機能ホーム画面。
 * これはコア機能のみを持つHomeScreenの簡素化されたバージョンです。
 * 
 * @author YourName
 * @version 1.5 (Basic)
 */
public class BasicHomeScreen implements Screen {
    
    private final Kernel kernel;
    private boolean isInitialized = false;
    private List<IApplication> apps;
    
    // UI Configuration
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 5;
    private static final int ICON_SIZE = 64;
    private static final int ICON_SPACING = 20;
    
    // Colors
    private final int backgroundColor = 0x1E1E1E;
    private final int textColor = 0xFFFFFF;
    private final int accentColor = 0x4A90E2;
    
    public BasicHomeScreen(Kernel kernel) {
        System.out.println("🔧 BasicHomeScreen: Constructor called with kernel: " + (kernel != null));
        this.kernel = kernel;
        this.apps = new ArrayList<>();
        System.out.println("✅ BasicHomeScreen: Created basic functional home screen");
    }
    
    @Override
    public void setup() {
        System.out.println("🚀 BasicHomeScreen: Starting setup...");
        isInitialized = true;
        loadApps();
        System.out.println("🚀 BasicHomeScreen: Setup complete with " + apps.size() + " apps");
        System.out.println("   isInitialized: " + isInitialized);
    }
    
    @Override
    public void draw(PApplet p) {
        // Debug: Log first few draw calls
        if (p.frameCount <= 3) {
            System.out.println("🎨 BasicHomeScreen: Drawing frame " + p.frameCount + " - initialized: " + isInitialized);
            System.out.println("   Apps available: " + apps.size());
        }
        
        try {
            // Let Kernel handle background for debugging
            // p.background(backgroundColor); // Commented out to allow Kernel debug display
            
            // Always draw something to verify this method is called
            p.fill(255, 255, 0); // Bright yellow
            p.textAlign(p.LEFT, p.TOP);
            p.textSize(12);
            p.text("BasicHomeScreen Active", 10, 70);
            p.text("Apps: " + apps.size(), 10, 85);
            p.text("Initialized: " + isInitialized, 10, 100);
            
            // Status bar
            drawStatusBar(p);
            
            // App grid
            drawAppGrid(p);
            
            // Navigation area
            drawNavigationArea(p);
            
        } catch (Exception e) {
            System.err.println("❌ BasicHomeScreen draw error: " + e.getMessage());
            e.printStackTrace();
            // Fallback
            p.background(255, 100, 100); // Red background for error
            p.fill(255);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text("BasicHomeScreen Error", p.width/2, p.height/2);
            p.textSize(12);
            p.text(e.getMessage(), p.width/2, p.height/2 + 20);
        }
    }
    
    private void drawStatusBar(PApplet p) {
        // Status bar background
        p.fill(0x2A2A2A);
        p.noStroke();
        p.rect(0, 0, p.width, 40);
        
        // Time
        p.fill(textColor);
        p.textAlign(p.LEFT, p.CENTER);
        p.textSize(12);
        if (kernel != null && kernel.getSystemClock() != null) {
            try {
                p.text(kernel.getSystemClock().getFormattedTime(), 15, 20);
            } catch (Exception e) {
                p.text("--:--", 15, 20);
            }
        } else {
            p.text("No Time", 15, 20);
        }
        
        // Title
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(14);
        p.text("MochiMobileOS", p.width/2, 20);
        
        // Status
        p.textAlign(p.RIGHT, p.CENTER);
        p.textSize(10);
        p.text("Basic Home", p.width - 15, 20);
    }
    
    private void drawAppGrid(PApplet p) {
        if (apps.isEmpty()) {
            // No apps message
            p.fill(textColor, 150);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text("No apps available", p.width/2, p.height/2);
            p.textSize(12);
            p.text("Apps will appear here when loaded", p.width/2, p.height/2 + 20);
            return;
        }
        
        int startY = 60; // Below status bar
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (p.width - gridWidth) / 2;
        
        // Draw apps in grid
        for (int i = 0; i < apps.size() && i < (GRID_COLS * GRID_ROWS); i++) {
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            
            int x = startX + col * (ICON_SIZE + ICON_SPACING);
            int y = startY + row * (ICON_SIZE + ICON_SPACING + 15);
            
            drawAppIcon(p, apps.get(i), x, y);
        }
    }
    
    private void drawAppIcon(PApplet p, IApplication app, int x, int y) {
        // Debug: Log icon drawing
        if (p.frameCount <= 5) {
            System.out.println("🎨 Drawing icon for " + app.getName() + " at (" + x + ", " + y + ")");
        }
        
        try {
            // Icon background (make more visible with bright colors for debugging)
            p.fill(100, 100, 100); // Gray background
            p.stroke(255, 255, 255); // White border
            p.strokeWeight(2);
            p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
            
            // App icon placeholder (bright blue for visibility)
            p.fill(0, 150, 255); // Bright blue
            p.noStroke();
            p.rect(x + 12, y + 12, ICON_SIZE - 24, ICON_SIZE - 24, 8);
            
            // App initial (white text)
            p.fill(255, 255, 255); // White
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(18);
            String initial = app.getName().substring(0, 1).toUpperCase();
            p.text(initial, x + ICON_SIZE/2, y + ICON_SIZE/2 - 2);
            
            // App name (white text)
            p.fill(255, 255, 255); // White
            p.textSize(10);
            p.textAlign(p.CENTER, p.TOP);
            String name = app.getName();
            if (name.length() > 8) {
                name = name.substring(0, 7) + "...";
            }
            p.text(name, x + ICON_SIZE/2, y + ICON_SIZE + 3);
            
            // Debug: Draw a bright rectangle around the entire icon area
            p.stroke(255, 255, 0); // Yellow border
            p.strokeWeight(1);
            p.noFill();
            p.rect(x - 2, y - 2, ICON_SIZE + 4, ICON_SIZE + 20);
            
        } catch (Exception e) {
            System.err.println("❌ Error drawing icon for " + app.getName() + ": " + e.getMessage());
            // Emergency fallback - draw bright red square
            p.fill(255, 0, 0);
            p.noStroke();
            p.rect(x, y, ICON_SIZE, ICON_SIZE);
        }
    }
    
    private void drawNavigationArea(PApplet p) {
        int navY = p.height - 80;
        
        // Navigation background
        p.fill(0x2A2A2A);
        p.noStroke();
        p.rect(0, navY, p.width, 80);
        
        // App Library hint
        p.fill(textColor, 150);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(12);
        p.text("Tap here for App Library", p.width/2, navY + 25);
        
        // Instructions
        p.textSize(10);
        p.text("Click app icons to launch • Basic launcher mode", p.width/2, navY + 50);
    }
    
    private void loadApps() {
        apps.clear();
        
        System.out.println("🔍 BasicHomeScreen: Loading apps...");
        System.out.println("   Kernel: " + (kernel != null));
        System.out.println("   AppLoader: " + (kernel != null && kernel.getAppLoader() != null));
        
        if (kernel != null && kernel.getAppLoader() != null) {
            try {
                List<IApplication> loadedApps = kernel.getAppLoader().getLoadedApps();
                System.out.println("   Total loaded apps: " + (loadedApps != null ? loadedApps.size() : "null"));
                
                if (loadedApps != null) {
                    for (IApplication app : loadedApps) {
                        System.out.println("   Checking app: " + (app != null ? app.getName() + " (" + app.getApplicationId() + ")" : "null"));
                        if (app != null && !"jp.moyashi.phoneos.core.apps.launcher".equals(app.getApplicationId())) {
                            apps.add(app);
                            System.out.println("   ✅ Added: " + app.getName());
                        } else {
                            System.out.println("   ⏭️ Skipped: " + (app != null ? "launcher app" : "null app"));
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Error loading apps: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("❌ Kernel or AppLoader is null!");
        }
        
        System.out.println("✅ BasicHomeScreen: Loaded " + apps.size() + " applications");
        for (IApplication app : apps) {
            System.out.println("   • " + app.getName() + " (" + app.getApplicationId() + ")");
        }
    }
    
    @Override
    public void mousePressed(int mouseX, int mouseY) {
        System.out.println("🖱️ BasicHomeScreen: Click at (" + mouseX + ", " + mouseY + ")");
        
        // Check navigation area (App Library)
        if (mouseY > getHeight() - 80) {
            System.out.println("📚 Opening App Library...");
            openAppLibrary();
            return;
        }
        
        // Check app icons
        IApplication clickedApp = getAppAtPosition(mouseX, mouseY);
        if (clickedApp != null) {
            System.out.println("🚀 Launching: " + clickedApp.getName());
            launchApplication(clickedApp);
        }
    }
    
    private IApplication getAppAtPosition(int mouseX, int mouseY) {
        if (apps.isEmpty()) return null;
        
        int startY = 60;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2; // Assume 400px width
        
        for (int i = 0; i < apps.size() && i < (GRID_COLS * GRID_ROWS); i++) {
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            
            int x = startX + col * (ICON_SIZE + ICON_SPACING);
            int y = startY + row * (ICON_SIZE + ICON_SPACING + 15);
            
            if (mouseX >= x && mouseX <= x + ICON_SIZE && 
                mouseY >= y && mouseY <= y + ICON_SIZE) {
                return apps.get(i);
            }
        }
        
        return null;
    }
    
    private void openAppLibrary() {
        if (kernel != null && kernel.getScreenManager() != null) {
            try {
                AppLibraryScreen appLibrary = new AppLibraryScreen(kernel);
                kernel.getScreenManager().pushScreen(appLibrary);
            } catch (Exception e) {
                System.err.println("Error opening App Library: " + e.getMessage());
            }
        }
    }
    
    private void launchApplication(IApplication app) {
        if (kernel != null && kernel.getScreenManager() != null) {
            try {
                Screen appScreen = app.getEntryScreen(kernel);
                kernel.getScreenManager().pushScreen(appScreen);
            } catch (Exception e) {
                System.err.println("Error launching app: " + e.getMessage());
            }
        }
    }
    
    private int getHeight() {
        return 600; // Assume standard height
    }
    
    @Override
    public void cleanup() {
        isInitialized = false;
        System.out.println("🧹 BasicHomeScreen: Cleanup completed");
    }
    
    @Override
    public String getScreenTitle() {
        return "Basic Home Screen";
    }
    
    /**
     * Refreshes the app list.
     */
    public void refreshApps() {
        loadApps();
    }
}