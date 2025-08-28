package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.apps.launcher.model.HomePage;
import jp.moyashi.phoneos.core.apps.launcher.model.Shortcut;
import processing.core.PApplet;

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
    public void setup() {
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
    public void draw(PApplet p) {
        try {
            // Always draw background first
            p.background(backgroundColor);
            
            // Debug info
            if (p.frameCount <= 5) {
                System.out.println("🎨 SafeHomeScreen: Drawing frame " + p.frameCount + 
                    " - initialized: " + isInitialized + " - pages: " + homePages.size());
            }
            
            // Draw title
            p.fill(textColor);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(24);
            p.text("Safe Home Screen", p.width/2, 50);
            
            // Draw status
            p.textSize(14);
            p.text("Initialized: " + isInitialized, p.width/2, 80);
            p.text("Pages: " + homePages.size() + " + App Library", p.width/2, 100);
            
            if (isShowingAppLibrary) {
                // Draw App Library page
                p.text("📚 App Library (" + allApps.size() + " apps)", p.width/2, 120);
                drawAppLibraryPage(p);
            } else if (!homePages.isEmpty()) {
                HomePage currentPage = homePages.get(currentPageIndex);
                p.text("📄 Page " + (currentPageIndex + 1) + " (" + currentPage.getShortcutCount() + " shortcuts)", p.width/2, 120);
                
                // Draw shortcuts
                drawShortcuts(p, currentPage);
            } else {
                p.fill(255, 100, 100);
                p.text("No pages available", p.width/2, 150);
            }
            
            // Draw navigation instructions
            p.fill(textColor, 150);
            p.textSize(12);
            if (isDragging) {
                int deltaX = dragCurrentX - dragStartX;
                p.text("Dragging: " + deltaX + "px", p.width/2, p.height - 70);
            }
            
            if (isShowingAppLibrary) {
                p.text("← Drag left to return to home pages", p.width/2, p.height - 50);
            } else {
                p.text("Drag left/right to navigate • Drag right to App Library →", p.width/2, p.height - 50);
            }
            
            // Draw page indicators
            drawPageIndicators(p);
            
        } catch (Exception e) {
            System.err.println("❌ SafeHomeScreen draw error: " + e.getMessage());
            e.printStackTrace();
            
            // Emergency fallback drawing
            p.background(100, 50, 50); // Red background to indicate error
            p.fill(255);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text("DRAW ERROR", p.width/2, p.height/2);
            p.textSize(12);
            p.text("Error: " + e.getMessage(), p.width/2, p.height/2 + 20);
        }
    }
    
    private void drawShortcuts(PApplet p, HomePage page) {
        try {
            List<Shortcut> shortcuts = page.getShortcuts();
            
            int startY = 150;
            int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
            int startX = (p.width - gridWidth) / 2;
            
            for (Shortcut shortcut : shortcuts) {
                try {
                    int x = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
                    int y = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 15);
                    
                    // Draw icon background
                    p.fill(0x333333);
                    p.stroke(0x555555);
                    p.strokeWeight(1);
                    p.rect(x, y, ICON_SIZE, ICON_SIZE, 8);
                    
                    // Draw app icon placeholder
                    p.fill(accentColor);
                    p.noStroke();
                    p.rect(x + 8, y + 8, ICON_SIZE - 16, ICON_SIZE - 16, 4);
                    
                    // Draw app initial
                    p.fill(textColor);
                    p.textAlign(p.CENTER, p.CENTER);
                    p.textSize(18);
                    String initial = shortcut.getDisplayName().substring(0, 1).toUpperCase();
                    p.text(initial, x + ICON_SIZE/2, y + ICON_SIZE/2);
                    
                    // Draw app name
                    p.textSize(10);
                    p.textAlign(p.CENTER, p.TOP);
                    String name = shortcut.getDisplayName();
                    if (name.length() > 8) {
                        name = name.substring(0, 7) + "...";
                    }
                    p.text(name, x + ICON_SIZE/2, y + ICON_SIZE + 3);
                    
                } catch (Exception shortcutError) {
                    System.err.println("Error drawing shortcut " + shortcut.getDisplayName() + ": " + shortcutError.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error in drawShortcuts: " + e.getMessage());
        }
    }
    
    @Override
    public void mousePressed(int mouseX, int mouseY) {
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
     * Handles mouse drag events for page swiping
     */
    public void mouseDragged(int mouseX, int mouseY) {
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
     * Handles mouse release events for completing swipe gestures or app clicks
     */
    public void mouseReleased(int mouseX, int mouseY) {
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
    
    @Override
    public void cleanup() {
        isInitialized = false;
        System.out.println("🧹 SafeHomeScreen: Cleanup completed");
    }
    
    @Override
    public String getScreenTitle() {
        return "Safe Home Screen (Debug)";
    }
    
    /**
     * Draws the app library page showing all available applications
     */
    private void drawAppLibraryPage(PApplet p) {
        try {
            int startY = 150;
            int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
            int startX = (p.width - gridWidth) / 2;
            
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
                
                drawAppLibraryIcon(p, displayApps.get(i), x, y);
            }
            
        } catch (Exception e) {
            System.err.println("Error in drawAppLibraryPage: " + e.getMessage());
        }
    }
    
    /**
     * Draws an app icon in the app library (with different styling)
     */
    private void drawAppLibraryIcon(PApplet p, IApplication app, int x, int y) {
        try {
            // Library icons have purple theme
            p.fill(80, 80, 120); // Purple-gray background
            p.stroke(150, 150, 200); // Light purple border
            p.strokeWeight(2);
            p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
            
            // App icon placeholder (purple)
            p.fill(120, 80, 180); // Purple
            p.noStroke();
            p.rect(x + 12, y + 12, ICON_SIZE - 24, ICON_SIZE - 24, 8);
            
            // App initial (white text)
            p.fill(255, 255, 255);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(18);
            String initial = app.getName().substring(0, 1).toUpperCase();
            p.text(initial, x + ICON_SIZE/2, y + ICON_SIZE/2 - 2);
            
            // App name (white text)
            p.fill(255, 255, 255);
            p.textSize(10);
            p.textAlign(p.CENTER, p.TOP);
            String name = app.getName();
            if (name.length() > 8) {
                name = name.substring(0, 7) + "...";
            }
            p.text(name, x + ICON_SIZE/2, y + ICON_SIZE + 3);
            
        } catch (Exception e) {
            System.err.println("Error drawing app library icon for " + app.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Draws page indicators at the bottom
     */
    private void drawPageIndicators(PApplet p) {
        try {
            int totalPages = homePages.size() + 1; // +1 for app library
            int dotSize = 8;
            int dotSpacing = 15;
            int totalWidth = totalPages * dotSpacing - dotSpacing + dotSize;
            int startX = (p.width - totalWidth) / 2;
            int y = p.height - 30;
            
            for (int i = 0; i < totalPages; i++) {
                int x = startX + i * dotSpacing;
                
                if (i == totalPages - 1) {
                    // App library indicator
                    if (isShowingAppLibrary) {
                        p.fill(120, 80, 180); // Purple (active)
                    } else {
                        p.fill(80, 80, 120); // Dim purple
                    }
                } else {
                    // Regular page indicator
                    if (!isShowingAppLibrary && i == currentPageIndex) {
                        p.fill(accentColor); // Blue (active)
                    } else {
                        p.fill(100, 100, 100); // Gray
                    }
                }
                
                p.noStroke();
                p.ellipse(x, y, dotSize, dotSize);
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
                for (Shortcut shortcut : currentPage.getShortcuts()) {
                    if (isClickOnShortcut(mouseX, mouseY, shortcut)) {
                        System.out.println("🚀 Clicking to launch: " + shortcut.getDisplayName());
                        launchApplication(shortcut.getApplication());
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
}