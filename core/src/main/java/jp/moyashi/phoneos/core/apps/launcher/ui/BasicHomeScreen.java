package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.app.IApplication;
import processing.core.PApplet;
import processing.core.PGraphics;

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
    public void setup(PGraphics g) {
        System.out.println("🚀 BasicHomeScreen: Starting setup...");
        isInitialized = true;
        loadApps();
        System.out.println("🚀 BasicHomeScreen: Setup complete with " + apps.size() + " apps");
        System.out.println("   isInitialized: " + isInitialized);
    }

    /**
     * @deprecated Use {@link #setup(PGraphics)} instead
     */
    @Deprecated
    @Override
    public void setup(processing.core.PApplet p) {
        PGraphics g = p.g;
        setup(g);
    }
    
    @Override
    public void draw(PGraphics g) {
        try {
            // Always draw something to verify this method is called
            g.fill(255, 255, 0); // Bright yellow
            g.textAlign(g.LEFT, g.TOP);
            g.textSize(12);
            g.text("BasicHomeScreen Active", 10, 70);
            g.text("Apps: " + apps.size(), 10, 85);
            g.text("Initialized: " + isInitialized, 10, 100);

            // Status bar
            drawStatusBar(g);

            // App grid
            drawAppGrid(g);

            // Navigation area
            drawNavigationArea(g);

        } catch (Exception e) {
            System.err.println("❌ BasicHomeScreen draw error: " + e.getMessage());
            e.printStackTrace();
            // Fallback
            g.background(255, 100, 100); // Red background for error
            g.fill(255);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(16);
            g.text("BasicHomeScreen Error", g.width/2, g.height/2);
            g.textSize(12);
            g.text(e.getMessage(), g.width/2, g.height/2 + 20);
        }
    }

    /**
     * @deprecated Use {@link #draw(PGraphics)} instead
     */
    @Deprecated
    @Override
    public void draw(PApplet p) {
        PGraphics g = p.g;
        draw(g);
    }
    
    private void drawStatusBar(PGraphics g) {
        // Status bar background
        g.fill(0x2A2A2A);
        g.noStroke();
        g.rect(0, 0, g.width, 40);

        // Time
        g.fill(textColor);
        g.textAlign(g.LEFT, g.CENTER);
        g.textSize(12);
        if (kernel != null && kernel.getSystemClock() != null) {
            try {
                g.text(kernel.getSystemClock().getFormattedTime(), 15, 20);
            } catch (Exception e) {
                g.text("--:--", 15, 20);
            }
        } else {
            g.text("No Time", 15, 20);
        }

        // Title
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(14);
        g.text("MochiMobileOS", g.width/2, 20);

        // Status
        g.textAlign(g.RIGHT, g.CENTER);
        g.textSize(10);
        g.text("Basic Home", g.width - 15, 20);
    }
    
    private void drawAppGrid(PGraphics g) {
        if (apps.isEmpty()) {
            // No apps message
            g.fill(textColor, 150);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(16);
            g.text("No apps available", g.width/2, g.height/2);
            g.textSize(12);
            g.text("Apps will appear here when loaded", g.width/2, g.height/2 + 20);
            return;
        }

        int startY = 60; // Below status bar
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (g.width - gridWidth) / 2;

        // Draw apps in grid
        for (int i = 0; i < apps.size() && i < (GRID_COLS * GRID_ROWS); i++) {
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;

            int x = startX + col * (ICON_SIZE + ICON_SPACING);
            int y = startY + row * (ICON_SIZE + ICON_SPACING + 15);

            drawAppIcon(g, apps.get(i), x, y);
        }
    }
    
    private void drawAppIcon(PGraphics g, IApplication app, int x, int y) {
        try {
            // Icon background (make more visible with bright colors for debugging)
            g.fill(100, 100, 100); // Gray background
            g.stroke(255, 255, 255); // White border
            g.strokeWeight(2);
            g.rect(x, y, ICON_SIZE, ICON_SIZE, 12);

            // App icon placeholder (bright blue for visibility)
            g.fill(0, 150, 255); // Bright blue
            g.noStroke();
            g.rect(x + 12, y + 12, ICON_SIZE - 24, ICON_SIZE - 24, 8);

            // App initial (white text)
            g.fill(255, 255, 255); // White
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(18);
            String initial = app.getName().substring(0, 1).toUpperCase();
            g.text(initial, x + ICON_SIZE/2, y + ICON_SIZE/2 - 2);

            // App name (white text)
            g.fill(255, 255, 255); // White
            g.textSize(10);
            g.textAlign(g.CENTER, g.TOP);
            String name = app.getName();
            if (name.length() > 8) {
                name = name.substring(0, 7) + "...";
            }
            g.text(name, x + ICON_SIZE/2, y + ICON_SIZE + 3);

            // Debug: Draw a bright rectangle around the entire icon area
            g.stroke(255, 255, 0); // Yellow border
            g.strokeWeight(1);
            g.noFill();
            g.rect(x - 2, y - 2, ICON_SIZE + 4, ICON_SIZE + 20);

        } catch (Exception e) {
            System.err.println("❌ Error drawing icon for " + app.getName() + ": " + e.getMessage());
            // Emergency fallback - draw bright red square
            g.fill(255, 0, 0);
            g.noStroke();
            g.rect(x, y, ICON_SIZE, ICON_SIZE);
        }
    }
    
    private void drawNavigationArea(PGraphics g) {
        int navY = g.height - 80;

        // Navigation background
        g.fill(0x2A2A2A);
        g.noStroke();
        g.rect(0, navY, g.width, 80);

        // App Library hint
        g.fill(textColor, 150);
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(12);
        g.text("Tap here for App Library", g.width/2, navY + 25);

        // Instructions
        g.textSize(10);
        g.text("Click app icons to launch • Basic launcher mode", g.width/2, navY + 50);
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
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        System.out.println("🖱️ BasicHomeScreen: Click at (" + mouseX + ", " + mouseY + ")");

        // Check navigation area (App Library)
        if (mouseY > getHeight() - 80) {
            System.out.println("📚 Opening App Library...");
            openAppLibrary();
            return;
        }

        // Check app icons
        int startY = 60;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (g.width - gridWidth) / 2;

        for (int i = 0; i < apps.size() && i < (GRID_COLS * GRID_ROWS); i++) {
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;

            int x = startX + col * (ICON_SIZE + ICON_SPACING);
            int y = startY + row * (ICON_SIZE + ICON_SPACING + 15);

            if (mouseX >= x && mouseX <= x + ICON_SIZE &&
                    mouseY >= y && mouseY <= y + ICON_SIZE) {

                IApplication clickedApp = apps.get(i);
                System.out.println("🚀 Launching: " + clickedApp.getName());

                float iconCenterX = x + ICON_SIZE / 2f;
                float iconCenterY = y + ICON_SIZE / 2f;

                launchApplicationWithAnimation(clickedApp, iconCenterX, iconCenterY, ICON_SIZE);
                return; // Found and handled click
            }
        }
    }

    /**
     * @deprecated Use {@link #mousePressed(PGraphics, int, int)} instead
     */
    @Deprecated
    @Override
    public void mousePressed(processing.core.PApplet p, int mouseX, int mouseY) {
        PGraphics g = p.g;
        mousePressed(g, mouseX, mouseY);
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

    private void launchApplicationWithAnimation(IApplication app, float iconX, float iconY, float iconSize) {
        System.out.println("BasicHomeScreen: Launching app with animation: " + app.getName());
        System.out.println("BasicHomeScreen: Icon position: (" + iconX + ", " + iconY + "), size: " + iconSize);

        if (kernel != null && kernel.getScreenManager() != null) {
            try {
                Screen appScreen = app.getEntryScreen(kernel);
                if (appScreen == null) {
                    System.err.println("BasicHomeScreen: getEntryScreen returned null for " + app.getName());
                    return;
                }
                
                processing.core.PImage appIcon = app.getIcon();

                if (appIcon == null && kernel != null) {
                    processing.core.PGraphics graphics = kernel.getGraphics();
                    if (graphics != null) {
                        appIcon = graphics.get(0, 0, 1, 1);
                        appIcon.resize(64, 64);
                        appIcon.loadPixels();
                        for (int i = 0; i < appIcon.pixels.length; i++) {
                            appIcon.pixels[i] = 0xFFFFFFFF; // White color
                        }
                        appIcon.updatePixels();
                    }
                }
                
                if (appIcon != null) {
                    kernel.getScreenManager().pushScreenWithAnimation(appScreen, iconX, iconY, iconSize, appIcon);
                } else {
                    // Fallback to normal launch
                    kernel.getScreenManager().pushScreen(appScreen);
                }
            } catch (Exception e) {
                System.err.println("BasicHomeScreen: Failed to launch app with animation " + app.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private int getHeight() {
        return 600; // Assume standard height
    }
    
    @Override
    public void cleanup(PGraphics g) {
        isInitialized = false;
        System.out.println("🧹 BasicHomeScreen: Cleanup completed");
    }

    /**
     * @deprecated Use {@link #cleanup(PGraphics)} instead
     */
    @Deprecated
    @Override
    public void cleanup(processing.core.PApplet p) {
        PGraphics g = p.g;
        cleanup(g);
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

    /**
     * Adds mouseDragged support for PGraphics (empty implementation, can be overridden)
     */
    @Override
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        // Default implementation - subclasses can override
    }

    /**
     * Adds mouseReleased support for PGraphics (empty implementation, can be overridden)
     */
    @Override
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        // Default implementation - subclasses can override
    }

    /**
     * Adds keyPressed support for PGraphics (empty implementation, can be overridden)
     */
    @Override
    public void keyPressed(PGraphics g, char key, int keyCode) {
        // Default implementation - subclasses can override
    }
}