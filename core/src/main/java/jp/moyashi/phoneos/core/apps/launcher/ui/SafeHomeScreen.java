package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.apps.launcher.model.HomePage;
import jp.moyashi.phoneos.core.apps.launcher.model.Shortcut;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * 表示問題をデバッグするためのエラーハンドリング付きHomeScreenの安全版。
 * このバージョンには広範囲なエラーチェックとフォールバック描画が含まれている。
 * 
 * @author YourName  
 * @version 1.0 (セーフ・デバッグ版)
 */
public class SafeHomeScreen implements Screen {
    
    private final Kernel kernel;
    private boolean isInitialized = false;
    private List<HomePage> homePages;
    private int currentPageIndex;
    private boolean isShowingAppLibrary = false;  // Flag for app library page
    private List<IApplication> allApps;  // All available apps for library
    
    // ドラッグ/スワイプ検知
    private boolean isDragging = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int dragCurrentX = 0;
    private int dragCurrentY = 0;
    private static final int SWIPE_THRESHOLD = 50;  // スワイプの最小距離
    private static final int SWIPE_VERTICAL_THRESHOLD = 100;  // 最大垂直移動
    
    // 色設定
    private final int backgroundColor = 0x1E1E1E;
    private final int textColor = 0xFFFFFF;
    private final int accentColor = 0x4A90E2;
    
    // グリッド設定  
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 5;
    private static final int ICON_SIZE = 64;
    private static final int ICON_SPACING = 20;
    
    public SafeHomeScreen(Kernel kernel) {
        this.kernel = kernel;
        this.homePages = new ArrayList<>();
        this.currentPageIndex = 0;
        this.isShowingAppLibrary = false;
        this.allApps = new ArrayList<>();
        System.out.println("✅ SafeHomeScreen: アプリライブラリーサポート付きセーフホームスクリーンを作成");
    }
    
    @Override
    public void setup(PGraphics g) {
        try {
            isInitialized = true;
            System.out.println("🚀 SafeHomeScreen: セーフ初期化を開始...");

            // ホームページの安全な初期化
            initializeHomePagesWithErrorHandling();

            System.out.println("✅ SafeHomeScreen: セーフ初期化完了!");
            System.out.println("    • Pages: " + homePages.size());
            System.out.println("    • Current page shortcuts: " +
                (homePages.isEmpty() ? 0 : homePages.get(0).getShortcutCount()));

        } catch (Exception e) {
            System.err.println("❌ SafeHomeScreen setup error: " + e.getMessage());
            e.printStackTrace();
            // Create empty fallback page
            homePages.clear();
            homePages.add(new HomePage("Emergency Page"));
        }
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
    
    private void initializeHomePagesWithErrorHandling() {
        try {
            homePages.clear();
            
            // Create first page
            HomePage firstPage = new HomePage("Home");
            homePages.add(firstPage);
            
            // Safely load applications
            if (kernel != null && kernel.getAppLoader() != null) {
                List<IApplication> apps = kernel.getAppLoader().getLoadedApps();
                System.out.println("SafeHomeScreen: Found " + apps.size() + " applications");
                
                // Store all apps for app library page
                allApps.clear();
                allApps.addAll(apps);
                
                for (IApplication app : apps) {
                    try {
                        // Skip launcher app itself
                        if ("jp.moyashi.phoneos.core.apps.launcher".equals(app.getApplicationId())) {
                            continue;
                        }
                        
                        // Try to add shortcut to first page
                        boolean added = firstPage.addShortcut(app);
                        if (added) {
                            System.out.println("SafeHomeScreen: Added shortcut for " + app.getName());
                        } else {
                            System.out.println("SafeHomeScreen: Could not add shortcut for " + app.getName() + " (page may be full)");
                        }
                        
                    } catch (Exception appError) {
                        System.err.println("SafeHomeScreen: Error adding app " + app.getName() + ": " + appError.getMessage());
                    }
                }
            } else {
                System.out.println("SafeHomeScreen: No apps available (kernel or appLoader is null)");
            }
            
        } catch (Exception e) {
            System.err.println("❌ SafeHomeScreen: Critical error in initializeHomePagesWithErrorHandling: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public void draw(PGraphics g) {
        try {
            // Always draw background first
            g.background(backgroundColor);

            // Draw title
            g.fill(textColor);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(24);
            g.text("Safe Home Screen", g.width/2, 50);

            // Draw status
            g.textSize(14);
            g.text("Initialized: " + isInitialized, g.width/2, 80);
            g.text("Pages: " + homePages.size() + " + App Library", g.width/2, 100);

            if (isShowingAppLibrary) {
                // Draw App Library page
                g.text("📚 App Library (" + allApps.size() + " apps)", g.width/2, 120);
                drawAppLibraryPage(g);
            } else if (!homePages.isEmpty()) {
                HomePage currentPage = homePages.get(currentPageIndex);
                g.text("📄 Page " + (currentPageIndex + 1) + " (" + currentPage.getShortcutCount() + " shortcuts)", g.width/2, 120);

                // Draw shortcuts
                drawShortcuts(g, currentPage);
            } else {
                g.fill(255, 100, 100);
                g.text("No pages available", g.width/2, 150);
            }

            // Draw navigation instructions
            g.fill(textColor, 150);
            g.textSize(12);
            if (isDragging) {
                int deltaX = dragCurrentX - dragStartX;
                g.text("Dragging: " + deltaX + "px", g.width/2, g.height - 70);
            }

            if (isShowingAppLibrary) {
                g.text("← Drag left to return to home pages", g.width/2, g.height - 50);
            } else {
                g.text("Drag left/right to navigate • Drag right to App Library →", g.width/2, g.height - 50);
            }

            // Draw page indicators
            drawPageIndicators(g);

        } catch (Exception e) {
            System.err.println("❌ SafeHomeScreen draw error: " + e.getMessage());
            e.printStackTrace();

            // Emergency fallback drawing
            g.background(100, 50, 50); // Red background to indicate error
            g.fill(255);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(16);
            g.text("DRAW ERROR", g.width/2, g.height/2);
            g.textSize(12);
            g.text("Error: " + e.getMessage(), g.width/2, g.height/2 + 20);
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
    
    private void drawShortcuts(PGraphics g, HomePage page) {
        try {
            List<Shortcut> shortcuts = page.getShortcuts();

            int startY = 150;
            int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
            int startX = (g.width - gridWidth) / 2;

            for (Shortcut shortcut : shortcuts) {
                try {
                    int x = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
                    int y = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 15);

                    // Draw icon background
                    g.fill(0x333333);
                    g.stroke(0x555555);
                    g.strokeWeight(1);
                    g.rect(x, y, ICON_SIZE, ICON_SIZE, 8);

                    // Draw app icon placeholder
                    g.fill(accentColor);
                    g.noStroke();
                    g.rect(x + 8, y + 8, ICON_SIZE - 16, ICON_SIZE - 16, 4);

                    // Draw app initial
                    g.fill(textColor);
                    g.textAlign(g.CENTER, g.CENTER);
                    g.textSize(18);
                    String initial = shortcut.getDisplayName().substring(0, 1).toUpperCase();
                    g.text(initial, x + ICON_SIZE/2, y + ICON_SIZE/2);

                    // Draw app name
                    g.textSize(10);
                    g.textAlign(g.CENTER, g.TOP);
                    String name = shortcut.getDisplayName();
                    if (name.length() > 8) {
                        name = name.substring(0, 7) + "...";
                    }
                    g.text(name, x + ICON_SIZE/2, y + ICON_SIZE + 3);

                } catch (Exception shortcutError) {
                    System.err.println("Error drawing shortcut " + shortcut.getDisplayName() + ": " + shortcutError.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Error in drawShortcuts: " + e.getMessage());
        }
    }
    
    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        System.out.println("🖱️ SafeHomeScreen: Mouse pressed at (" + mouseX + ", " + mouseY + ")");

        try {
            // Start drag detection
            isDragging = false;
            dragStartX = mouseX;
            dragStartY = mouseY;
            dragCurrentX = mouseX;
            dragCurrentY = mouseY;

        } catch (Exception e) {
            System.err.println("Error in mousePressed: " + e.getMessage());
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
    
    /**
     * Handles mouse drag events for page swiping (PGraphics version)
     */
    @Override
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        try {
            dragCurrentX = mouseX;
            dragCurrentY = mouseY;

            int deltaX = dragCurrentX - dragStartX;
            int deltaY = Math.abs(dragCurrentY - dragStartY);

            // Check if this is a valid horizontal swipe
            if (Math.abs(deltaX) > 10 && deltaY < SWIPE_VERTICAL_THRESHOLD) {
                isDragging = true;
                // Visual feedback could be added here (e.g., page preview)
            }

        } catch (Exception e) {
            System.err.println("Error in mouseDragged: " + e.getMessage());
        }
    }

    /**
     * @deprecated Use {@link #mouseDragged(PGraphics, int, int)} instead
     */
    @Deprecated
    public void mouseDragged(processing.core.PApplet p, int mouseX, int mouseY) {
        PGraphics g = p.g;
        mouseDragged(g, mouseX, mouseY);
    }
    
    /**
     * Handles mouse release events for completing swipe gestures or app clicks (PGraphics version)
     */
    @Override
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        System.out.println("🖱️ SafeHomeScreen: Mouse released at (" + mouseX + ", " + mouseY + ")");

        try {
            if (isDragging) {
                // Handle swipe gesture
                handleSwipeGesture();
            } else {
                // Handle app click (no significant drag occurred)
                handleAppClick(mouseX, mouseY);
            }

            // Reset drag state
            isDragging = false;
            dragStartX = 0;
            dragStartY = 0;
            dragCurrentX = 0;
            dragCurrentY = 0;

        } catch (Exception e) {
            System.err.println("Error in mouseReleased: " + e.getMessage());
        }
    }

    /**
     * @deprecated Use {@link #mouseReleased(PGraphics, int, int)} instead
     */
    @Deprecated
    public void mouseReleased(processing.core.PApplet p, int mouseX, int mouseY) {
        PGraphics g = p.g;
        mouseReleased(g, mouseX, mouseY);
    }
    
    private boolean isClickOnShortcut(int mouseX, int mouseY, Shortcut shortcut) {
        int startY = 150;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        
        int x = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
        int y = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 15);
        
        return mouseX >= x && mouseX <= x + ICON_SIZE && 
               mouseY >= y && mouseY <= y + ICON_SIZE;
    }
    
    private void launchApplication(IApplication app) {
        try {
            if (kernel != null && kernel.getScreenManager() != null) {
                Screen appScreen = app.getEntryScreen(kernel);
                kernel.getScreenManager().pushScreen(appScreen);
            }
        } catch (Exception e) {
            System.err.println("Error launching application: " + e.getMessage());
        }
    }

    private void launchApplicationWithAnimation(IApplication app, float iconX, float iconY, float iconSize) {
        System.out.println("SafeHomeScreen: Launching app with animation: " + app.getName());
        System.out.println("SafeHomeScreen: Icon position: (" + iconX + ", " + iconY + "), size: " + iconSize);

        if (kernel != null && kernel.getScreenManager() != null) {
            try {
                Screen appScreen = app.getEntryScreen(kernel);
                if (appScreen == null) {
                    System.err.println("SafeHomeScreen: getEntryScreen returned null for " + app.getName());
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
                System.err.println("SafeHomeScreen: Failed to launch app with animation " + app.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    @Override
    public void cleanup(PGraphics g) {
        isInitialized = false;
        System.out.println("🧹 SafeHomeScreen: Cleanup completed");
    }

    /**
     * @deprecated Use {@link #cleanup(PGraphics)} instead
     */
    @Deprecated
    @Override
    public void cleanup(PApplet p) {
        PGraphics g = p.g;
        cleanup(g);
    }
    
    @Override
    public String getScreenTitle() {
        return "Safe Home Screen (Debug)";
    }
    
    /**
     * Draws the app library page showing all available applications
     */
    private void drawAppLibraryPage(PGraphics g) {
        try {
            int startY = 150;
            int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
            int startX = (g.width - gridWidth) / 2;

            // Filter out launcher app for display
            List<IApplication> displayApps = new ArrayList<>();
            for (IApplication app : allApps) {
                if (!"jp.moyashi.phoneos.core.apps.launcher".equals(app.getApplicationId())) {
                    displayApps.add(app);
                }
            }

            for (int i = 0; i < displayApps.size() && i < (GRID_COLS * GRID_ROWS); i++) {
                int col = i % GRID_COLS;
                int row = i / GRID_COLS;

                int x = startX + col * (ICON_SIZE + ICON_SPACING);
                int y = startY + row * (ICON_SIZE + ICON_SPACING + 15);

                drawAppLibraryIcon(g, displayApps.get(i), x, y);
            }

        } catch (Exception e) {
            System.err.println("Error in drawAppLibraryPage: " + e.getMessage());
        }
    }
    
    /**
     * Draws an app icon in the app library (with different styling)
     */
    private void drawAppLibraryIcon(PGraphics g, IApplication app, int x, int y) {
        try {
            // Library icons have purple theme
            g.fill(80, 80, 120); // Purple-gray background
            g.stroke(150, 150, 200); // Light purple border
            g.strokeWeight(2);
            g.rect(x, y, ICON_SIZE, ICON_SIZE, 12);

            // App icon placeholder (purple)
            g.fill(120, 80, 180); // Purple
            g.noStroke();
            g.rect(x + 12, y + 12, ICON_SIZE - 24, ICON_SIZE - 24, 8);

            // App initial (white text)
            g.fill(255, 255, 255);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(18);
            String initial = app.getName().substring(0, 1).toUpperCase();
            g.text(initial, x + ICON_SIZE/2, y + ICON_SIZE/2 - 2);

            // App name (white text)
            g.fill(255, 255, 255);
            g.textSize(10);
            g.textAlign(g.CENTER, g.TOP);
            String name = app.getName();
            if (name.length() > 8) {
                name = name.substring(0, 7) + "...";
            }
            g.text(name, x + ICON_SIZE/2, y + ICON_SIZE + 3);

        } catch (Exception e) {
            System.err.println("Error drawing app library icon for " + app.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Draws page indicators at the bottom
     */
    private void drawPageIndicators(PGraphics g) {
        try {
            int totalPages = homePages.size() + 1; // +1 for app library
            int dotSize = 8;
            int dotSpacing = 15;
            int totalWidth = totalPages * dotSpacing - dotSpacing + dotSize;
            int startX = (g.width - totalWidth) / 2;
            int y = g.height - 30;

            for (int i = 0; i < totalPages; i++) {
                int x = startX + i * dotSpacing;

                if (i == totalPages - 1) {
                    // App library indicator
                    if (isShowingAppLibrary) {
                        g.fill(120, 80, 180); // Purple (active)
                    } else {
                        g.fill(80, 80, 120); // Dim purple
                    }
                } else {
                    // Regular page indicator
                    if (!isShowingAppLibrary && i == currentPageIndex) {
                        g.fill(accentColor); // Blue (active)
                    } else {
                        g.fill(100, 100, 100); // Gray
                    }
                }

                g.noStroke();
                g.ellipse(x, y, dotSize, dotSize);
            }

        } catch (Exception e) {
            System.err.println("Error drawing page indicators: " + e.getMessage());
        }
    }
    
    /**
     * Handles clicks on app library apps
     */
    private void handleAppLibraryClick(int mouseX, int mouseY) {
        try {
            int startY = 150;
            int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
            int startX = (400 - gridWidth) / 2;
            
            // Filter out launcher app
            List<IApplication> displayApps = new ArrayList<>();
            for (IApplication app : allApps) {
                if (!"jp.moyashi.phoneos.core.apps.launcher".equals(app.getApplicationId())) {
                    displayApps.add(app);
                }
            }
            
            for (int i = 0; i < displayApps.size() && i < (GRID_COLS * GRID_ROWS); i++) {
                int col = i % GRID_COLS;
                int row = i / GRID_COLS;
                
                int x = startX + col * (ICON_SIZE + ICON_SPACING);
                int y = startY + row * (ICON_SIZE + ICON_SPACING + 15);
                
                if (mouseX >= x && mouseX <= x + ICON_SIZE && 
                    mouseY >= y && mouseY <= y + ICON_SIZE) {
                    System.out.println("🚀 Launching from App Library: " + displayApps.get(i).getName());
                    launchApplication(displayApps.get(i));
                    return;
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in handleAppLibraryClick: " + e.getMessage());
        }
    }
    
    /**
     * Handles swipe gestures for page navigation
     */
    private void handleSwipeGesture() {
        try {
            int deltaX = dragCurrentX - dragStartX;
            int deltaY = Math.abs(dragCurrentY - dragStartY);
            
            // Check if swipe is significant enough and mostly horizontal
            if (Math.abs(deltaX) >= SWIPE_THRESHOLD && deltaY < SWIPE_VERTICAL_THRESHOLD) {
                System.out.println("🌊 Swipe detected: deltaX=" + deltaX + ", deltaY=" + deltaY);
                
                if (deltaX > 0) {
                    // Swiped right - go to previous page or exit app library
                    if (isShowingAppLibrary) {
                        isShowingAppLibrary = false;
                        System.out.println("📄 Swiped right: Returning to home pages");
                    } else if (currentPageIndex > 0) {
                        currentPageIndex--;
                        System.out.println("📄 Swiped right: Switched to page " + (currentPageIndex + 1));
                    }
                } else {
                    // Swiped left - go to next page or app library
                    if (!isShowingAppLibrary) {
                        if (currentPageIndex < homePages.size() - 1) {
                            currentPageIndex++;
                            System.out.println("📄 Swiped left: Switched to page " + (currentPageIndex + 1));
                        } else {
                            // AppLibraryScreenに遷移
                            openAppLibrary();
                            System.out.println("📚 Swiped left: Navigating to App Library Screen");
                        }
                    }
                }
            } else {
                System.out.println("🌊 Swipe not significant enough: deltaX=" + deltaX + ", deltaY=" + deltaY);
            }
            
        } catch (Exception e) {
            System.err.println("Error in handleSwipeGesture: " + e.getMessage());
        }
    }
    
    /**
     * Handles app clicks when no swipe gesture was detected
     */
    private void handleAppClick(int mouseX, int mouseY) {
        try {
            if (isShowingAppLibrary) {
                // Click on app library apps
                handleAppLibraryClick(mouseX, mouseY);
            } else if (!homePages.isEmpty()) {
                // Click on home page shortcuts
                HomePage currentPage = homePages.get(currentPageIndex);
                int startY = 150;
                int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
                int startX = (400 - gridWidth) / 2;

                for (Shortcut shortcut : currentPage.getShortcuts()) {
                    int x = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
                    int y = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 15);

                    if (mouseX >= x && mouseX <= x + ICON_SIZE && 
                        mouseY >= y && mouseY <= y + ICON_SIZE) {
                        
                        System.out.println("🚀 Clicking to launch: " + shortcut.getDisplayName());
                        float iconCenterX = x + ICON_SIZE / 2f;
                        float iconCenterY = y + ICON_SIZE / 2f;
                        launchApplicationWithAnimation(shortcut.getApplication(), iconCenterX, iconCenterY, ICON_SIZE);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in handleAppClick: " + e.getMessage());
        }
    }
    
    /**
     * AppLibraryScreenを開く。
     */
    private void openAppLibrary() {
        try {
            System.out.println("SafeHomeScreen: Opening AppLibraryScreen");
            
            if (kernel != null && kernel.getScreenManager() != null) {
                // AppLibraryScreenを作成
                AppLibraryScreen appLibraryScreen = new AppLibraryScreen(kernel);
                
                // HomeScreenの参照を設定（ショートカット追加のため）
                // SafeHomeScreenをHomeScreenとして使用できるように、適切な方法を検討する必要があります
                // 今回は直接設定はしませんが、必要に応じて後で実装
                
                // AppLibraryScreenに遷移
                kernel.getScreenManager().pushScreen(appLibraryScreen);
                System.out.println("SafeHomeScreen: ✅ Successfully pushed AppLibraryScreen");
            } else {
                System.err.println("SafeHomeScreen: Cannot open AppLibrary - ScreenManager not available");
            }
        } catch (Exception e) {
            System.err.println("SafeHomeScreen: Error opening AppLibrary: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Adds keyPressed support for PGraphics (empty implementation, can be overridden)
     */
    @Override
    public void keyPressed(PGraphics g, char key, int keyCode) {
        // Default implementation - subclasses can override
    }
}