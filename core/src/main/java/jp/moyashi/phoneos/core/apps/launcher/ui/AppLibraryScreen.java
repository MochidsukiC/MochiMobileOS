package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.apps.launcher.model.HomePage;
import jp.moyashi.phoneos.core.apps.launcher.model.Shortcut;
import jp.moyashi.phoneos.core.ui.popup.PopupMenu;
import jp.moyashi.phoneos.core.ui.popup.PopupItem;
import jp.moyashi.phoneos.core.input.GestureListener;
import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.List;

/**
 * The App Library screen displays all installed applications in a comprehensive list view.
 * This screen provides users with access to all available applications in the system,
 * including those that may not have shortcuts on the home screen.
 * 
 * Features include:
 * - Scrollable list of all installed applications
 * - App icons and names display
 * - Touch-to-launch functionality
 * - Return to home screen navigation
 * - Real-time app list updates
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class AppLibraryScreen implements Screen, GestureListener {
    
    /** Reference to the OS kernel for accessing system services */
    private final Kernel kernel;
    
    /** Background color for the app library screen */
    private int backgroundColor;
    
    /** Text color for the app library screen */
    private int textColor;
    
    /** Accent color for UI elements */
    private int accentColor;
    
    /** Flag to track if the screen has been initialized */
    private boolean isInitialized;
    
    /** List of all available applications */
    private List<IApplication> allApps;
    
    /** Scroll offset for app list */
    private int scrollOffset;
    
    /** Reference to home screen for adding shortcuts */
    private jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen homeScreen;
    
    /** Long press detection for context menu */
    private long touchStartTime;
    private int touchStartX, touchStartY;
    private IApplication longPressedApp;
    private boolean showingContextMenu;
    private boolean isPressed;
    private static final long LONG_PRESS_DURATION = 200; // 200ms for testing (was 500ms)
    private static final int DRAG_TOLERANCE = 15; // Pixel tolerance for drag during long press
    
    // Search/hover state
    private boolean searchFocused = false;
    private String searchQuery = "";
    private java.util.List<IApplication> visibleApps = null;
    private int hoveredIndex = -1;
    private int pressedIndex = -1;
    
    /** App list item configuration */
    private static final int ITEM_HEIGHT = 88;
    private static final int ITEM_PADDING = 16;
    private static final int ICON_SIZE = 48;
    // 検索ボックスを追加したためヘッダー領域を拡大
    private static final int LIST_START_Y = 112;
    private static final int SEARCH_X = 12;
    private static final int SEARCH_Y = 70; // ヘッダー冁E
    private static final int SEARCH_W = 400 - 24;
    private static final int SEARCH_H = 26;
    
    /**
     * Constructs a new AppLibraryScreen instance.
     * 
     * @param kernel The OS kernel instance providing access to system services
     */
    public AppLibraryScreen(Kernel kernel) {
        this.kernel = kernel;
        var themeInit = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (themeInit != null) {
            this.backgroundColor = themeInit.colorBackground();
            this.textColor = themeInit.colorOnSurface();
            this.accentColor = themeInit.colorPrimary();
        } else {
            this.backgroundColor = 0x0F0F0F; // Darker than home screen
            this.textColor = 0xFFFFFF;       // White text
            this.accentColor = 0x4A90E2;     // Blue accent
        }
        this.isInitialized = false;
        this.scrollOffset = 0;
        this.homeScreen = null;
        this.showingContextMenu = false;
        this.isPressed = false;
        this.searchFocused = false;
        this.searchQuery = "";
        this.hoveredIndex = -1;
        this.pressedIndex = -1;
        
        System.out.println("AppLibraryScreen: App library screen created");
    }
    
    /**
     * Sets the reference to the home screen for shortcut management.
     * 
     * @param homeScreen The HomeScreen instance
     */
    public void setHomeScreen(jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen homeScreen) {
        this.homeScreen = homeScreen;
    }
    
    /**
     * Initializes the app library screen when it becomes active (PGraphics version).
     * Loads the complete list of available applications.
     */
    @Override
    public void setup(PGraphics g) {
        isInitialized = true;
        loadAllApps();

        // ジェスチャーリスナーを登録
        if (kernel != null && kernel.getGestureManager() != null) {
            kernel.getGestureManager().addGestureListener(this);
            System.out.println("AppLibraryScreen: Registered gesture listener");
        }

        System.out.println("AppLibraryScreen: App library initialized with " +
                          (allApps != null ? allApps.size() : 0) + " applications");
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
    
    /**
     * Draws the app library interface (PGraphics version).
     * Renders the header, scrollable app list, and navigation elements.
     *
     * @param g The PGraphics instance for drawing operations
     */
    @Override
    public void draw(PGraphics g) {
        // Check for long press in draw loop (more reliable than event system)
        checkLongPress();

        // Theme sync
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            backgroundColor = theme.colorBackground();
            textColor = theme.colorOnSurface();
            accentColor = theme.colorPrimary();
        }

        // Draw background
        int bg = backgroundColor; g.background((bg>>16)&0xFF, (bg>>8)&0xFF, bg&0xFF);

        // Draw header + search box
        drawHeader(g);
        drawSearchBox(g);

        // Draw app list
        drawAppList(g);

        // Draw scroll indicator if needed
        if (needsScrolling()) {
            drawScrollIndicator(g);
        }

        // Draw back navigation hint
        drawNavigationHint(g);

        // Context menu is now handled by global PopupManager
        // No need to draw locally anymore
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
    
    /**
     * Handles mouse press events on the app library screen (PGraphics version).
     * Processes app selection and navigation interactions.
     *
     * @param g The PGraphics instance
     * @param mouseX The x-coordinate of the mouse press
     * @param mouseY The y-coordinate of the mouse press
     */
    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        // Search box focus
        if (mouseY >= SEARCH_Y && mouseY <= SEARCH_Y + SEARCH_H && mouseX >= SEARCH_X && mouseX <= SEARCH_X + SEARCH_W) {
            searchFocused = true;
            return;
        }

        // Check if click is in header area (back navigation)
        if (mouseY < LIST_START_Y) {
            goBack();
            return;
        }

        // Check if clicking on context menu
        if (showingContextMenu && longPressedApp != null) {
            if (isClickingAddToHome(mouseX, mouseY)) {
                addAppToHome(longPressedApp);
                showingContextMenu = false;
                longPressedApp = null;
                return;
            } else if (isClickingOutsideMenu(mouseX, mouseY)) {
                showingContextMenu = false;
                longPressedApp = null;
                return;
            }
        }

        // Start long press detection
        touchStartTime = System.currentTimeMillis();
        touchStartX = mouseX;
        touchStartY = mouseY;
        isPressed = true;
        System.out.println("AppLibraryScreen: 👆 Touch start at (" + mouseX + ", " + mouseY + ") time: " + touchStartTime);

        // Check if click is on an app item
        IApplication clickedApp = getAppAtPosition(mouseX, mouseY);
        // pressed index
        if (mouseY >= LIST_START_Y) {
            int idx = (mouseY + scrollOffset - LIST_START_Y) / ITEM_HEIGHT;
            pressedIndex = idx;
        } else {
            pressedIndex = -1;
        }
        if (clickedApp != null) {
            longPressedApp = clickedApp;

            // TEST: Show popup immediately for testing using new PopupAPI
            showContextMenuForApp(clickedApp);
        } else {
            longPressedApp = null;
            isPressed = false;
            // Hide context menu if clicking on empty area
            if (showingContextMenu) {
                showingContextMenu = false;
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
    
    /**
     * Handles mouse drag events (PGraphics version).
     * We need to handle this to prevent drag from interrupting long-press detection.
     */
    @Override
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        // Don't interrupt long press detection for small drags
        if (isPressed && longPressedApp != null) {
            // Calculate drag distance from original touch point
            int dragDistance = (int) Math.sqrt(Math.pow(mouseX - touchStartX, 2) + Math.pow(mouseY - touchStartY, 2));

            System.out.println("AppLibraryScreen: mouseDragged at (" + mouseX + ", " + mouseY + ") - distance: " + dragDistance + "px");

            if (dragDistance > DRAG_TOLERANCE) {
                // Too much movement, cancel long press
                System.out.println("AppLibraryScreen: Drag distance exceeded tolerance (" + dragDistance + " > " + DRAG_TOLERANCE + "), canceling long press");
                isPressed = false;
                longPressedApp = null;
            } else {
                System.out.println("AppLibraryScreen: Drag within tolerance, continuing long press detection");
            }
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
     * Handles mouse release events (PGraphics version).
     */
    @Override
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        if (!isPressed) {
            return;
        }

        isPressed = false;

        // clear press visual
        pressedIndex = -1;

        long currentTime = System.currentTimeMillis();
        long pressDuration = currentTime - touchStartTime;

        // Only handle short press here, long press is handled in checkLongPress() during draw loop
        if (pressDuration < LONG_PRESS_DURATION && longPressedApp != null && !showingContextMenu) {
            // Short press - launch app
            System.out.println("AppLibraryScreen: Short press detected, launching app: " + longPressedApp.getName());
            
            // アイコン位置を計箁E
            int itemIndex = getAppIndex(longPressedApp);
            if (itemIndex >= 0) {
                int itemY = LIST_START_Y + (itemIndex * ITEM_HEIGHT) - scrollOffset;
                float iconCenterX = ITEM_PADDING + ICON_SIZE / 2;
                float iconCenterY = itemY + ITEM_HEIGHT / 2;
                
                System.out.println("AppLibraryScreen: Using animation launch for " + longPressedApp.getName());
                launchApplicationWithAnimation(longPressedApp, iconCenterX, iconCenterY);
            } else {
                // フォールバック
                System.out.println("AppLibraryScreen: Using fallback launch for " + longPressedApp.getName());
                launchApplication(longPressedApp);
            }
        } else if (showingContextMenu) {
            System.out.println("AppLibraryScreen: Context menu was already shown via long press detection");
        }

        // Reset long press tracking if not showing context menu
        if (!showingContextMenu) {
            longPressedApp = null;
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
    
    /**
     * Checks for long press during draw loop.
     * This is more reliable than relying on mouseReleased timing.
     */
    private void checkLongPress() {
        // デバッグ用にフレームに1回状態を出力
        if (isPressed && System.currentTimeMillis() % 100 < 20) {
            System.out.println("AppLibraryScreen: checkLongPress() - isPressed=" + isPressed + 
                              ", longPressedApp=" + (longPressedApp != null ? longPressedApp.getName() : "null") + 
                              ", showingContextMenu=" + showingContextMenu);
        }
        
        if (isPressed && longPressedApp != null && !showingContextMenu) {
            long currentTime = System.currentTimeMillis();
            long pressDuration = currentTime - touchStartTime;
            
            // デバッグ用に進行状況を表示
            if (pressDuration % 100 < 20) {
                System.out.println("AppLibraryScreen: Long press progress: " + pressDuration + "ms / " + LONG_PRESS_DURATION + "ms");
            }
            
            if (pressDuration >= LONG_PRESS_DURATION) {
                // Long press detected!
                showingContextMenu = true;
                System.out.println("AppLibraryScreen: ✅🔥 LONG PRESS DETECTED in draw loop for " + longPressedApp.getName() + " after " + pressDuration + "ms ✅🔥");
                System.out.println("AppLibraryScreen: Setting showingContextMenu = " + showingContextMenu);
            }
        }
    }
    
    /**
     * Cleans up resources when the screen is deactivated (PGraphics version).
     */
    @Override
    public void cleanup(PGraphics g) {
        // ジェスチャーリスナーを削除
        if (kernel != null && kernel.getGestureManager() != null) {
            kernel.getGestureManager().removeGestureListener(this);
            System.out.println("AppLibraryScreen: Unregistered gesture listener");
        }

        isInitialized = false;
        allApps = null;
        System.out.println("AppLibraryScreen: App library screen cleaned up");
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
    
    /**
     * Gets the title of this screen.
     * 
     * @return The screen title
     */
    @Override
    public String getScreenTitle() {
        return "App Library";
    }
    
    /**
     * Loads all available applications from the app loader.
     */
    private void loadAllApps() {
        if (kernel != null && kernel.getAppLoader() != null) {
            // アプリローダーから最新のアプリリストを取得
            allApps = kernel.getAppLoader().getLoadedApps();
            System.out.println("AppLibraryScreen: Loaded " + allApps.size() + " applications");
            
            // デバッグ用: ロードされたアプリの詳細を表示
            for (int i = 0; i < allApps.size(); i++) {
                IApplication app = allApps.get(i);
                System.out.println("  " + (i+1) + ". " + app.getName() + " (" + app.getApplicationId() + ") - " + app.getDescription());
            }
            
            // もしアプリぁEつもなぁE��合、�Eスキャンを実衁E
            if (allApps.isEmpty()) {
                System.out.println("AppLibraryScreen: No apps found, triggering rescan...");
                kernel.getAppLoader().refreshApps();
                allApps = kernel.getAppLoader().getLoadedApps();
                System.out.println("AppLibraryScreen: After rescan: " + allApps.size() + " applications");
            }
        }
    }
    
    /**
     * Draws the header section with title and navigation.
     *
     * @param g The PGraphics instance for drawing
     */
    private void drawHeader(PGraphics g) {
        // Header background
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int surface = theme != null ? theme.colorSurface() : 0xFF1A1A1A;
        int border = theme != null ? theme.colorBorder() : 0xFF333333;
        // subtle elevation
        jp.moyashi.phoneos.core.ui.effects.Elevation.drawRectShadow(g, 0, 0, 400, LIST_START_Y, 8, 2);
        g.fill((surface>>16)&0xFF, (surface>>8)&0xFF, surface&0xFF);
        g.noStroke();
        g.rect(0, 0, 400, LIST_START_Y);

        // Back arrow
        g.stroke(textColor);
        g.strokeWeight(2);
        g.line(20, 30, 30, 20);
        g.line(20, 30, 30, 40);

        // Title
        g.fill(textColor);
        g.noStroke();
        g.textAlign(g.LEFT, g.CENTER);
        g.textSize(18);
        g.text("App Library", 50, 30);

        // App count
        g.textAlign(g.RIGHT, g.CENTER);
        g.textSize(12);
        { int c=textColor; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, 150); }
        if (allApps != null) {
            g.text(allApps.size() + " apps", 380, 30);
        }

        // Separator line
        g.stroke((border>>16)&0xFF, (border>>8)&0xFF, border&0xFF);
        g.strokeWeight(1);
        g.line(0, LIST_START_Y - 1, 400, LIST_START_Y - 1);
    }

    private void drawSearchBox(PGraphics g) {
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int border = theme != null ? theme.colorBorder() : 0xFF3A3A3A;
        int surface = theme != null ? theme.colorSurface() : 0xFF202020;
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFFFFFFFF;

        g.fill((surface>>16)&0xFF, (surface>>8)&0xFF, surface&0xFF);
        g.stroke((border>>16)&0xFF, (border>>8)&0xFF, border&0xFF);
        g.strokeWeight(searchFocused ? 2 : 1);
        g.rect(SEARCH_X, SEARCH_Y, SEARCH_W, SEARCH_H, 8);

        g.fill((onSurface>>16)&0xFF, (onSurface>>8)&0xFF, onSurface&0xFF, searchQuery.isEmpty() ? 120 : 255);
        g.textAlign(g.LEFT, g.CENTER);
        g.textSize(12);
        String display = searchQuery.isEmpty() ? "Search apps" : searchQuery;
        g.text(display, SEARCH_X + 10, SEARCH_Y + SEARCH_H/2f);

        if (searchFocused && (System.currentTimeMillis() / 500) % 2 == 0) {
            float tw = g.textWidth(searchQuery);
            float cx = SEARCH_X + 10 + tw + 1;
            g.stroke((onSurface>>16)&0xFF, (onSurface>>8)&0xFF, onSurface&0xFF);
            g.strokeWeight(1);
            g.line(cx, SEARCH_Y + 6, cx, SEARCH_Y + SEARCH_H - 6);
            g.noStroke();
        }
    }
    
    /**
     * Draws the scrollable list of applications.
     *
     * @param g The PGraphics instance for drawing
     */
    private void drawAppList(PGraphics g) {
        if (allApps == null || allApps.isEmpty()) {
            // No apps message
            { int c=textColor; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, 150); }
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(16);
            g.text("No applications installed", 200, 300);
            return;
        }

        ensureVisibleApps();

        // Calculate visible area
        int visibleHeight = 600 - LIST_START_Y - 40; // 40px for navigation hint

        // Draw visible app items
        for (int i = 0; i < visibleApps.size(); i++) {
            int itemY = LIST_START_Y + (i * ITEM_HEIGHT) - scrollOffset;

            // Skip items outside visible area
            if (itemY + ITEM_HEIGHT < LIST_START_Y || itemY > 600) {
                continue;
            }

            IApplication app = visibleApps.get(i);
            drawAppItem(g, app, itemY, i);
        }
    }
    
    /**
     * Draws an individual application item in the list.
     *
     * @param g The PGraphics instance for drawing
     * @param app The application to draw
     * @param y The y-coordinate for the item
     * @param index The index of the item in the list
     */
    private void drawAppItem(PGraphics g, IApplication app, int y, int index) {
        // Item background (theme surface、奁E��行に薁E��オーバ�Eレイ)
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int surface = theme != null ? theme.colorSurface() : 0xFF1E1E1E;
        g.fill((surface>>16)&0xFF, (surface>>8)&0xFF, surface&0xFF);
        g.noStroke();
        g.rect(0, y, 400, ITEM_HEIGHT);
        if ((index % 2) == 1) {
            g.fill(255,255,255,12);
            g.rect(0, y, 400, ITEM_HEIGHT);
        }

        // Hover/Pressed overlay
        if (theme != null) {
            if (index == pressedIndex) {
                int pr = theme.colorPressed();
                g.fill((pr>>16)&0xFF, (pr>>8)&0xFF, pr&0xFF, 60);
                g.rect(0, y, 400, ITEM_HEIGHT);
            } else if (index == hoveredIndex) {
                int hv = theme.colorHover();
                g.fill((hv>>16)&0xFF, (hv>>8)&0xFF, hv&0xFF, 40);
                g.rect(0, y, 400, ITEM_HEIGHT);
            }
        }

        // App icon placeholder
        g.fill(accentColor);
        g.rect(ITEM_PADDING, y + (ITEM_HEIGHT - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE, 8);

        // App icon letter
        g.fill(textColor);
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(24);
        String initial = app.getName().substring(0, 1).toUpperCase();
        g.text(initial, ITEM_PADDING + ICON_SIZE / 2, y + ITEM_HEIGHT / 2 - 2);

        // App name
        g.fill(textColor);
        g.textAlign(g.LEFT, g.CENTER);
        g.textSize(16);
        g.text(app.getName(), ITEM_PADDING + ICON_SIZE + 15, y + ITEM_HEIGHT / 2 - 8);

        // App description
        g.fill(textColor, 150);
        g.textSize(12);
        String description = app.getDescription();
        if (description.length() > 40) {
            description = description.substring(0, 37) + "...";
        }
        g.text(description, ITEM_PADDING + ICON_SIZE + 15, y + ITEM_HEIGHT / 2 + 8);

        // Version info
        g.fill(textColor, 100);
        g.textAlign(g.RIGHT, g.CENTER);
        g.textSize(10);
        g.text("v" + app.getVersion(), (400 - ITEM_PADDING), y + ITEM_HEIGHT / 2);

        // Separator line
        int border = theme != null ? theme.colorBorder() : 0xFF333333;
        g.stroke((border>>16)&0xFF, (border>>8)&0xFF, border&0xFF);
        g.strokeWeight(1);
        g.line(ITEM_PADDING, y + ITEM_HEIGHT - 1, (400 - ITEM_PADDING), y + ITEM_HEIGHT - 1);
    }
    
    /**
     * Draws the scroll indicator if the list is scrollable.
     *
     * @param g The PGraphics instance for drawing
     */
    private void drawScrollIndicator(PGraphics g) {
        if (allApps == null) return;

        ensureVisibleApps();
        int totalHeight = visibleApps.size() * ITEM_HEIGHT;
        int visibleHeight = 600 - LIST_START_Y - 40;

        if (totalHeight <= visibleHeight) return;

        // Scroll bar background
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int border = theme != null ? theme.colorBorder() : 0xFF333333;
        g.fill((border>>16)&0xFF, (border>>8)&0xFF, border&0xFF);
        g.noStroke();
        g.rect(395, LIST_START_Y, 5, visibleHeight);

        // Scroll thumb
        float scrollRatio = (float) scrollOffset / (totalHeight - visibleHeight);
        float thumbHeight = (float) visibleHeight * visibleHeight / totalHeight;
        float thumbY = LIST_START_Y + scrollRatio * (visibleHeight - thumbHeight);

        g.fill(accentColor);
        g.rect(395, thumbY, 5, thumbHeight);
    }
    
    /**
     * Draws the navigation hint at the bottom.
     *
     * @param g The PGraphics instance for drawing
     */
    private void drawNavigationHint(PGraphics g) {
        { int c=textColor; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, 100); }
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(12);
        g.text("Tap an app to launch • Tap header to go back", 200, 580);
    }
    
    /**
     * Gets the application at the specified screen coordinates.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The IApplication at that position, or null if none
     */
    private IApplication getAppAtPosition(int x, int y) {
        if (allApps == null || y < LIST_START_Y) {
            return null;
        }
        ensureVisibleApps();
        int adjustedY = y + scrollOffset - LIST_START_Y;
        int itemIndex = adjustedY / ITEM_HEIGHT;
        if (itemIndex >= 0 && visibleApps != null && itemIndex < visibleApps.size()) {
            return visibleApps.get(itemIndex);
        }
        return null;
    }
    
    /**
     * Checks if the app list needs scrolling.
     * 
     * @return true if scrolling is needed, false otherwise
     */
    private boolean needsScrolling() {
        if (allApps == null) return false;
        ensureVisibleApps();
        int totalHeight = visibleApps.size() * ITEM_HEIGHT;
        int visibleHeight = 600 - LIST_START_Y - 40;
        return totalHeight > visibleHeight;
    }

    private void ensureVisibleApps() {
        if (allApps == null) { visibleApps = java.util.Collections.emptyList(); return; }
        if (searchQuery == null || searchQuery.isEmpty()) { visibleApps = allApps; return; }
        String q = searchQuery.toLowerCase();
        java.util.ArrayList<IApplication> list = new java.util.ArrayList<>();
        for (IApplication app : allApps) {
            String name = app.getName() != null ? app.getName().toLowerCase() : "";
            if (name.contains(q)) list.add(app);
        }
        visibleApps = list;
    }

    @Override
    public void mouseMoved(PGraphics g, int mouseX, int mouseY) {
        if (mouseY >= LIST_START_Y) {
            ensureVisibleApps();
            int idx = (mouseY + scrollOffset - LIST_START_Y) / ITEM_HEIGHT;
            if (idx >= 0 && visibleApps != null && idx < visibleApps.size()) {
                hoveredIndex = idx;
            } else {
                hoveredIndex = -1;
            }
        } else {
            hoveredIndex = -1;
        }
    }

    @Override
    public void mouseWheel(PGraphics g, int mouseX, int mouseY, float delta) {
        ensureVisibleApps();
        int totalHeight = visibleApps != null ? visibleApps.size() * ITEM_HEIGHT : 0;
        int visibleHeight = 600 - LIST_START_Y - 40;
        int maxOffset = Math.max(0, totalHeight - visibleHeight);
        scrollOffset += (int)(delta * 20);
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxOffset) scrollOffset = maxOffset;
    }

    @Override
    public void keyPressed(PGraphics g, char key, int keyCode) {
        if (!searchFocused) return;
        if (keyCode == 8) { // Backspace
            if (!searchQuery.isEmpty()) searchQuery = searchQuery.substring(0, searchQuery.length()-1);
            return;
        }
        if (keyCode == 27) { // ESC
            searchFocused = false;
            return;
        }
        if (key >= 32 && key != 127 && key != 65535) {
            searchQuery += key;
        }
    }
    
    /**
     * Goes back to the previous screen (home screen).
     */
    private void goBack() {
        System.out.println("AppLibraryScreen: Going back to home screen");
        
        if (kernel != null && kernel.getScreenManager() != null) {
            kernel.getScreenManager().popScreen();
        }
    }
    
    /**
     * Launches the specified application.
     * 
     * @param app The application to launch
     */
    private void launchApplication(IApplication app) {
        System.out.println("AppLibraryScreen: Launching app: " + app.getName());

        if (kernel != null && kernel.getScreenManager() != null && kernel.getServiceManager() != null) {
            try {
                // ServiceManager経由でアプリを起動（既存インスタンスを再利用または新規作成）
                Screen appScreen = kernel.getServiceManager().launchApp(app.getApplicationId());
                if (appScreen != null) {
                    kernel.getScreenManager().pushScreen(appScreen);
                } else {
                    System.err.println("AppLibraryScreen: ServiceManager returned null screen for " + app.getName());
                }
            } catch (Exception e) {
                System.err.println("AppLibraryScreen: Failed to launch app " + app.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Launches the specified application with animation from icon position.
     * 
     * @param app The application to launch
     * @param iconX Icon center X position
     * @param iconY Icon center Y position
     */
    private void launchApplicationWithAnimation(IApplication app, float iconX, float iconY) {
        System.out.println("AppLibraryScreen: Launching app with animation: " + app.getName());
        System.out.println("AppLibraryScreen: Icon position: (" + iconX + ", " + iconY + "), size: " + ICON_SIZE);

        if (kernel != null && kernel.getScreenManager() != null && kernel.getServiceManager() != null) {
            try {
                // ServiceManager経由でアプリを起動（既存インスタンスを再利用または新規作成）
                Screen appScreen = kernel.getServiceManager().launchApp(app.getApplicationId());
                if (appScreen == null) {
                    System.err.println("AppLibraryScreen: ServiceManager returned null screen for " + app.getName());
                    return;
                }
                System.out.println("AppLibraryScreen: Got app screen: " + appScreen.getScreenTitle());
                
                // Get app icon for animation
                processing.core.PImage appIcon = app.getIcon();

                // If icon is null, create a white default icon
                if (appIcon == null && kernel != null) {
                    // Create a new PImage directly instead of using get() which fails with P2D renderer
                    appIcon = new processing.core.PImage(64, 64, processing.core.PConstants.ARGB);
                    appIcon.loadPixels();
                    for (int i = 0; i < appIcon.pixels.length; i++) {
                        appIcon.pixels[i] = 0xFFFFFFFF; // White color
                    }
                    appIcon.updatePixels();
                }

                System.out.println("AppLibraryScreen: Got app icon: " + (appIcon != null ? appIcon.width + "x" + appIcon.height : "null"));
                
                // Launch with animation
                if (appIcon != null) {
                    System.out.println("AppLibraryScreen: Calling pushScreenWithAnimation...");
                    kernel.getScreenManager().pushScreenWithAnimation(appScreen, iconX, iconY, ICON_SIZE, appIcon);
                } else {
                    System.out.println("AppLibraryScreen: No icon available, using normal launch");
                    // Fallback to normal launch
                    kernel.getScreenManager().pushScreen(appScreen);
                }
            } catch (Exception e) {
                System.err.println("AppLibraryScreen: Failed to launch app with animation " + app.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Refreshes the application list.
     */
    public void refreshApps() {
        loadAllApps();
        scrollOffset = 0; // Reset scroll position
        System.out.println("AppLibraryScreen: Refreshed application list");
    }
    
    /**
     * アプリをホーム画面に追加する
     *
     * @param app 追加するアプリケーション
     */
    private void addAppToHome(IApplication app) {
        if (homeScreen != null && kernel != null) {
            try {
                // ホーム画面の最初のページを取得
                List<HomePage> homePages = homeScreen.getHomePages();
                if (homePages.isEmpty()) {
                    // ページが存在しない場合は作成
                    homePages.add(new HomePage("Home"));
                }
                
                // 空いているページを探してショートカットを追加
                boolean added = false;
                for (HomePage page : homePages) {
                    if (!page.isFull()) {
                        Shortcut newShortcut = new Shortcut(app);
                        if (page.addShortcut(newShortcut)) {
                            added = true;
                            System.out.println("AppLibraryScreen: " + app.getName() + "をホーム画面に追加しました");
                            break;
                        }
                    }
                }
                
                if (!added) {
                    // 全てのペ�Eジが満員の場合、新しいペ�Eジを作�E
                    HomePage newPage = new HomePage();
                    Shortcut newShortcut = new Shortcut(app);
                    if (newPage.addShortcut(newShortcut)) {
                        homePages.add(newPage);
                        System.out.println("AppLibraryScreen: " + app.getName() + "を新しいペ�Eジに追加しました");
                        added = true;
                    }
                }
                
                if (added) {
                    // レイアウトを保孁E
                    if (kernel.getLayoutManager() != null) {
                        kernel.getLayoutManager().saveLayout(homePages);
                        System.out.println("AppLibraryScreen: レイアウトを保存しました");
                    }
                } else {
                    System.err.println("AppLibraryScreen: " + app.getName() + "の追加に失敗しました");
                }
                
            } catch (Exception e) {
                System.err.println("AppLibraryScreen: ホーム追加エラー: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("AppLibraryScreen: ホーム画面参照またはカーネルがnull");
        }
    }
    
    /**
     * コンテキストメニューを描画する
     *
     * @param g 描画用のPGraphicsインスタンス
     */
    private void drawContextMenu(PGraphics g) {
        System.out.println("AppLibraryScreen: drawContextMenu() called, longPressedApp=" + (longPressedApp != null ? longPressedApp.getName() : "null"));

        if (longPressedApp == null) {
            System.out.println("AppLibraryScreen: ❁ECannot draw context menu - longPressedApp is null");
            return;
        }

        System.out.println("AppLibraryScreen: 🎨 Drawing context menu overlay and box...");

        // 半透�Eの背景オーバ�Eレイ
        g.fill(0, 0, 0, 150);
        g.noStroke();
        g.rect(0, 0, g.width, g.height);

        // コンチE��ストメニューのボックス
        int menuWidth = 200;
        int menuHeight = 80;
        int menuX = (g.width - menuWidth) / 2;
        int menuY = (g.height - menuHeight) / 2;

        g.fill(backgroundColor + 0x202020); // 少し明るぁE��景
        g.stroke(accentColor);
        g.strokeWeight(2);
        g.rect(menuX, menuY, menuWidth, menuHeight, 8);

        // メニューアイチE��: "ホ�Eム画面に追加"
        g.fill(textColor);
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(16);
        g.text("ホ�Eム画面に追加", menuX + menuWidth/2, menuY + menuHeight/2);

        // 選択された App 吁E
        g.fill(accentColor);
        g.textSize(12);
        g.text(longPressedApp.getName(), menuX + menuWidth/2, menuY + 20);
    }
    
    /**
     * "ホ�Eム画面に追加"ボタンがクリチE��されたかどぁE��を確認する、E
     * 
     * @param mouseX マウスX座樁E
     * @param mouseY マウスY座樁E
     * @return クリチE��された場吁Erue
     */
    private boolean isClickingAddToHome(int mouseX, int mouseY) {
        int menuWidth = 200;
        int menuHeight = 80;
        int menuX = (400 - menuWidth) / 2; // hardcoded screen width
        int menuY = (600 - menuHeight) / 2; // hardcoded screen height
        
        return mouseX >= menuX && mouseX <= menuX + menuWidth &&
               mouseY >= menuY && mouseY <= menuY + menuHeight;
    }
    
    /**
     * メニュー外をクリックしたかどうかを確認する
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @return メニュー外をクリックした場合はtrue
     */
    private boolean isClickingOutsideMenu(int mouseX, int mouseY) {
        return !isClickingAddToHome(mouseX, mouseY);
    }
    
    /**
     * アプリのコンテキストメニューを新しいポップアップAPIで表示する
     *
     * @param app 対象のアプリケーション
     */
    private void showContextMenuForApp(IApplication app) {
        if (kernel == null || kernel.getPopupManager() == null) {
            System.err.println("AppLibraryScreen: PopupManager not available");
            return;
        }
        
        System.out.println("AppLibraryScreen: Creating popup menu for " + app.getName());
        
        // ポップアップメニューを作成
        PopupMenu popup = new PopupMenu(app.getName())
            .addItem("ホーム画面に追加", () -> {
                System.out.println("AppLibraryScreen: Adding " + app.getName() + " to home screen via PopupAPI");
                addAppToHome(app);
            })
            .addSeparator()
            .addItem("キャンセル", () -> {
                System.out.println("AppLibraryScreen: Popup cancelled");
            });
        
        // ポップアップを表示
        kernel.getPopupManager().showPopup(popup);
        System.out.println("AppLibraryScreen: ✅ Popup shown via PopupManager");
    }
    
    // ===========================================
    // GestureListener Implementation
    // ===========================================
    
    @Override
    public boolean onGesture(GestureEvent event) {
        System.out.println("AppLibraryScreen: Received gesture: " + event);
        
        switch (event.getType()) {
            case TAP:
                return handleTap(event.getCurrentX(), event.getCurrentY());
                
            case LONG_PRESS:
                return handleLongPress(event.getCurrentX(), event.getCurrentY());
                
            case SWIPE_LEFT:
                return handleSwipeLeft();
                
            case SWIPE_RIGHT:
                return handleSwipeRight();
                
            default:
                return false; // 処理しないジェスチャー
        }
    }
    
    @Override
    public boolean isInBounds(int x, int y) {
        // AppLibraryScreenが現在のスクリーンの場合のみ処理
        return kernel != null && 
               kernel.getScreenManager() != null && 
               kernel.getScreenManager().getCurrentScreen() == this;
    }
    
    @Override
    public int getPriority() {
        return 100; // 高優先度�E��EチE�EアチE�Eより低い�E�E
    }
    
    /**
     * タチE�Eジェスチャーを�E琁E��る、E
     * 
     * @param x X座樁E
     * @param y Y座樁E
     * @return 処琁E��た場吁Erue
     */
    private boolean handleTap(int x, int y) {
        System.out.println("AppLibraryScreen: Handling tap at (" + x + ", " + y + ")");
        
        // ヘッダー領域のタチE�E�E�戻る！E
        if (y < LIST_START_Y) {
            goBack();
            return true;
        }
        
        // アプリアイチE��のタチE�E�E�起動！E
        IApplication tappedApp = getAppAtPosition(x, y);
        if (tappedApp != null) {
            System.out.println("AppLibraryScreen: Launching app with animation: " + tappedApp.getName());
            
            // アイコン位置を計箁E
            int itemIndex = getAppIndex(tappedApp);
            System.out.println("AppLibraryScreen: getAppIndex returned " + itemIndex + " for " + tappedApp.getName());
            if (itemIndex >= 0) {
                int itemY = LIST_START_Y + (itemIndex * ITEM_HEIGHT) - scrollOffset;
                float iconCenterX = ITEM_PADDING + ICON_SIZE / 2;
                float iconCenterY = itemY + ITEM_HEIGHT / 2;
                
                System.out.println("AppLibraryScreen: Using animation launch for " + tappedApp.getName());
                launchApplicationWithAnimation(tappedApp, iconCenterX, iconCenterY);
            } else {
                // フォールバック
                System.out.println("AppLibraryScreen: Using fallback launch for " + tappedApp.getName());
                launchApplication(tappedApp);
            }
            return true;
        }
        
        return false;
    }
    
    /**
     * 長押しジェスチャーを�E琁E��る、E
     * 
     * @param x X座樁E
     * @param y Y座樁E
     * @return 処琁E��た場吁Erue
     */
    private boolean handleLongPress(int x, int y) {
        System.out.println("AppLibraryScreen: Handling long press at (" + x + ", " + y + ")");
        
        // ヘッダー領域では長押し無効
        if (y < LIST_START_Y) {
            return false;
        }
        
        // アプリアイチE��の長押し（コンチE��ストメニュー�E�E
        IApplication longPressedApp = getAppAtPosition(x, y);
        if (longPressedApp != null) {
            System.out.println("AppLibraryScreen: ✁ELong press detected for " + longPressedApp.getName() + " - showing popup via GestureManager");
            showContextMenuForApp(longPressedApp);
            return true;
        }
        
        return false;
    }
    
    /**
     * 左スワイプジェスチャーを�E琁E��る、E
     * 
     * @return 処琁E��た場吁Erue
     */
    private boolean handleSwipeLeft() {
        System.out.println("AppLibraryScreen: Left swipe detected");
        // 忁E��に応じて実裁E���Eージング等！E
        return false;
    }
    
    /**
     * 右スワイプジェスチャーを�E琁E��る、E
     * 
     * @return 処琁E��た場吁Erue
     */
    private boolean handleSwipeRight() {
        System.out.println("AppLibraryScreen: Right swipe detected - going back");
        goBack();
        return true;
    }
    
    /**
     * アプリのリスト�EインチE��クスを取得する、E
     * 
     * @param app 検索対象のアプリケーション
     * @return アプリのインチE��クス、見つからなぁE��合�E-1
     */
    private int getAppIndex(IApplication app) {
        if (allApps == null || app == null) {
            return -1;
        }
        
        for (int i = 0; i < allApps.size(); i++) {
            if (allApps.get(i) == app) {
                return i;
            }
        }
        return -1;
    }

    

    /**
     * ホ�Eム画面のペ�Eジ一覧を取得するため�EゲチE��ーメソチE��、E
     * これはHomeScreenクラスに追加する忁E��があります、E
     */
    // ホ�Eム画面からペ�Eジリストを取得するため、HomeScreenクラスにもgetterメソチE��を追加する忁E��がありまぁE
}

