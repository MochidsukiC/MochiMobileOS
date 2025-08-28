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
    
    /** App list item configuration */
    private static final int ITEM_HEIGHT = 80;
    private static final int ITEM_PADDING = 10;
    private static final int ICON_SIZE = 48;
    private static final int LIST_START_Y = 60;
    
    /**
     * Constructs a new AppLibraryScreen instance.
     * 
     * @param kernel The OS kernel instance providing access to system services
     */
    public AppLibraryScreen(Kernel kernel) {
        this.kernel = kernel;
        this.backgroundColor = 0x0F0F0F; // Darker than home screen
        this.textColor = 0xFFFFFF;       // White text
        this.accentColor = 0x4A90E2;     // Blue accent
        this.isInitialized = false;
        this.scrollOffset = 0;
        this.homeScreen = null;
        this.showingContextMenu = false;
        this.isPressed = false;
        
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
     * Initializes the app library screen when it becomes active.
     * Loads the complete list of available applications.
     */
    @Override
    public void setup() {
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
     * Draws the app library interface.
     * Renders the header, scrollable app list, and navigation elements.
     * 
     * @param p The PApplet instance for drawing operations
     */
    @Override
    public void draw(PApplet p) {
        // Check for long press in draw loop (more reliable than event system)
        checkLongPress();
        
        // Draw background
        p.background(backgroundColor);
        
        // TEST: Force show context menu if key is pressed (for testing)
        if (p.keyPressed && p.key == 't') {
            if (allApps != null && !allApps.isEmpty()) {
                longPressedApp = allApps.get(0);
                showingContextMenu = true;
                System.out.println("AppLibraryScreen: TEST - Force showing context menu for " + longPressedApp.getName());
            }
        }
        
        // Draw header
        drawHeader(p);
        
        // Draw app list
        drawAppList(p);
        
        // Draw scroll indicator if needed
        if (needsScrolling()) {
            drawScrollIndicator(p);
        }
        
        // Draw back navigation hint
        drawNavigationHint(p);
        
        // Context menu is now handled by global PopupManager
        // No need to draw locally anymore
    }
    
    /**
     * Handles mouse press events on the app library screen.
     * Processes app selection and navigation interactions.
     * 
     * @param mouseX The x-coordinate of the mouse press
     * @param mouseY The y-coordinate of the mouse press
     */
    @Override
    public void mousePressed(int mouseX, int mouseY) {
        System.out.println("AppLibraryScreen: Touch at (" + mouseX + ", " + mouseY + ")");
        
        // Check if click is in header area (back navigation)
        if (mouseY < LIST_START_Y) {
            goBack();
            return;
        }
        
        // Check if clicking on context menu
        if (showingContextMenu && longPressedApp != null) {
            if (isClickingAddToHome(mouseX, mouseY)) {
                System.out.println("AppLibraryScreen: ユーザーがホーム追加を選択");
                addAppToHome(longPressedApp);
                showingContextMenu = false;
                longPressedApp = null;
                return;
            } else if (isClickingOutsideMenu(mouseX, mouseY)) {
                System.out.println("AppLibraryScreen: メニュー外をクリック、メニューを閉じます");
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
        if (clickedApp != null) {
            longPressedApp = clickedApp;
            System.out.println("AppLibraryScreen: 🎯 App selected for long press: " + clickedApp.getName());
            
            // TEST: Show popup immediately for testing using new PopupAPI
            System.out.println("AppLibraryScreen: TEST - Showing popup immediately using new PopupAPI");
            showContextMenuForApp(clickedApp);
        } else {
            longPressedApp = null;
            isPressed = false;
            System.out.println("AppLibraryScreen: ❌ No app at touch position");
            // Hide context menu if clicking on empty area
            if (showingContextMenu) {
                showingContextMenu = false;
                System.out.println("AppLibraryScreen: Hiding context menu (clicked empty area)");
            }
        }
    }
    
    /**
     * Handles mouse drag events.
     * We need to handle this to prevent drag from interrupting long-press detection.
     */
    public void mouseDragged(int mouseX, int mouseY) {
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
     * Handles mouse release events.
     */
    public void mouseReleased(int mouseX, int mouseY) {
        System.out.println("AppLibraryScreen: mouseReleased called at (" + mouseX + ", " + mouseY + ")");
        System.out.println("AppLibraryScreen: isPressed = " + isPressed + ", showingContextMenu = " + showingContextMenu);
        
        if (!isPressed) {
            System.out.println("AppLibraryScreen: Not in pressed state, ignoring release");
            return;
        }
        
        isPressed = false;
        
        long currentTime = System.currentTimeMillis();
        long pressDuration = currentTime - touchStartTime;
        
        System.out.println("AppLibraryScreen: Touch start time: " + touchStartTime);
        System.out.println("AppLibraryScreen: Current time: " + currentTime);
        System.out.println("AppLibraryScreen: Press duration: " + pressDuration + "ms (threshold: " + LONG_PRESS_DURATION + "ms)");
        System.out.println("AppLibraryScreen: Long pressed app: " + (longPressedApp != null ? longPressedApp.getName() : "null"));
        
        // Only handle short press here, long press is handled in checkLongPress() during draw loop
        if (pressDuration < LONG_PRESS_DURATION && longPressedApp != null && !showingContextMenu) {
            // Short press - launch app
            System.out.println("AppLibraryScreen: Short press detected, launching app: " + longPressedApp.getName());
            launchApplication(longPressedApp);
        } else if (showingContextMenu) {
            System.out.println("AppLibraryScreen: Context menu was already shown via long press detection");
        }
        
        // Reset long press tracking if not showing context menu
        if (!showingContextMenu) {
            longPressedApp = null;
        }
    }
    
    /**
     * Checks for long press during draw loop.
     * This is more reliable than relying on mouseReleased timing.
     */
    private void checkLongPress() {
        // デバッグ：5フレームに1回状態を出力
        if (isPressed && System.currentTimeMillis() % 100 < 20) {
            System.out.println("AppLibraryScreen: checkLongPress() - isPressed=" + isPressed + 
                              ", longPressedApp=" + (longPressedApp != null ? longPressedApp.getName() : "null") + 
                              ", showingContextMenu=" + showingContextMenu);
        }
        
        if (isPressed && longPressedApp != null && !showingContextMenu) {
            long currentTime = System.currentTimeMillis();
            long pressDuration = currentTime - touchStartTime;
            
            // デバッグ：進行状況を表示
            if (pressDuration % 100 < 20) {
                System.out.println("AppLibraryScreen: Long press progress: " + pressDuration + "ms / " + LONG_PRESS_DURATION + "ms");
            }
            
            if (pressDuration >= LONG_PRESS_DURATION) {
                // Long press detected!
                showingContextMenu = true;
                System.out.println("AppLibraryScreen: ✅✅✅ LONG PRESS DETECTED in draw loop for " + longPressedApp.getName() + " after " + pressDuration + "ms ✅✅✅");
                System.out.println("AppLibraryScreen: Setting showingContextMenu = " + showingContextMenu);
            }
        }
    }
    
    /**
     * Cleans up resources when the screen is deactivated.
     */
    @Override
    public void cleanup() {
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
            allApps = kernel.getAppLoader().getLoadedApps();
            System.out.println("AppLibraryScreen: Loaded " + allApps.size() + " applications");
        }
    }
    
    /**
     * Draws the header section with title and navigation.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawHeader(PApplet p) {
        // Header background
        p.fill(0x1A1A1A);
        p.noStroke();
        p.rect(0, 0, 400, LIST_START_Y);
        
        // Back arrow
        p.stroke(textColor);
        p.strokeWeight(2);
        p.line(20, 30, 30, 20);
        p.line(20, 30, 30, 40);
        
        // Title
        p.fill(textColor);
        p.noStroke();
        p.textAlign(p.LEFT, p.CENTER);
        p.textSize(18);
        p.text("App Library", 50, 30);
        
        // App count
        p.textAlign(p.RIGHT, p.CENTER);
        p.textSize(12);
        p.fill(textColor, 150);
        if (allApps != null) {
            p.text(allApps.size() + " apps", 380, 30);
        }
        
        // Separator line
        p.stroke(0x333333);
        p.strokeWeight(1);
        p.line(0, LIST_START_Y - 1, 400, LIST_START_Y - 1);
    }
    
    /**
     * Draws the scrollable list of applications.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawAppList(PApplet p) {
        if (allApps == null || allApps.isEmpty()) {
            // No apps message
            p.fill(textColor, 150);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text("No applications installed", 200, 300);
            return;
        }
        
        // Calculate visible area
        int visibleHeight = 600 - LIST_START_Y - 40; // 40px for navigation hint
        int visibleItems = visibleHeight / ITEM_HEIGHT;
        
        // Draw visible app items
        for (int i = 0; i < allApps.size(); i++) {
            int itemY = LIST_START_Y + (i * ITEM_HEIGHT) - scrollOffset;
            
            // Skip items outside visible area
            if (itemY + ITEM_HEIGHT < LIST_START_Y || itemY > 600) {
                continue;
            }
            
            IApplication app = allApps.get(i);
            drawAppItem(p, app, itemY, i);
        }
    }
    
    /**
     * Draws an individual application item in the list.
     * 
     * @param p The PApplet instance for drawing
     * @param app The application to draw
     * @param y The y-coordinate for the item
     * @param index The index of the item in the list
     */
    private void drawAppItem(PApplet p, IApplication app, int y, int index) {
        // Item background (alternate colors for better visibility)
        p.fill(index % 2 == 0 ? 0x1E1E1E : 0x2A2A2A);
        p.noStroke();
        p.rect(0, y, 400, ITEM_HEIGHT);
        
        // App icon placeholder
        p.fill(accentColor);
        p.rect(ITEM_PADDING, y + (ITEM_HEIGHT - ICON_SIZE) / 2, ICON_SIZE, ICON_SIZE, 8);
        
        // App icon letter
        p.fill(textColor);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(24);
        String initial = app.getName().substring(0, 1).toUpperCase();
        p.text(initial, ITEM_PADDING + ICON_SIZE / 2, y + ITEM_HEIGHT / 2 - 2);
        
        // App name
        p.fill(textColor);
        p.textAlign(p.LEFT, p.CENTER);
        p.textSize(16);
        p.text(app.getName(), ITEM_PADDING + ICON_SIZE + 15, y + ITEM_HEIGHT / 2 - 8);
        
        // App description
        p.fill(textColor, 150);
        p.textSize(12);
        String description = app.getDescription();
        if (description.length() > 40) {
            description = description.substring(0, 37) + "...";
        }
        p.text(description, ITEM_PADDING + ICON_SIZE + 15, y + ITEM_HEIGHT / 2 + 8);
        
        // Version info
        p.fill(textColor, 100);
        p.textAlign(p.RIGHT, p.CENTER);
        p.textSize(10);
        p.text("v" + app.getVersion(), 390, y + ITEM_HEIGHT / 2);
        
        // Separator line
        p.stroke(0x333333);
        p.strokeWeight(1);
        p.line(ITEM_PADDING, y + ITEM_HEIGHT - 1, 390, y + ITEM_HEIGHT - 1);
    }
    
    /**
     * Draws the scroll indicator if the list is scrollable.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawScrollIndicator(PApplet p) {
        if (allApps == null) return;
        
        int totalHeight = allApps.size() * ITEM_HEIGHT;
        int visibleHeight = 600 - LIST_START_Y - 40;
        
        if (totalHeight <= visibleHeight) return;
        
        // Scroll bar background
        p.fill(0x333333);
        p.noStroke();
        p.rect(395, LIST_START_Y, 5, visibleHeight);
        
        // Scroll thumb
        float scrollRatio = (float) scrollOffset / (totalHeight - visibleHeight);
        float thumbHeight = (float) visibleHeight * visibleHeight / totalHeight;
        float thumbY = LIST_START_Y + scrollRatio * (visibleHeight - thumbHeight);
        
        p.fill(accentColor);
        p.rect(395, thumbY, 5, thumbHeight);
    }
    
    /**
     * Draws the navigation hint at the bottom.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawNavigationHint(PApplet p) {
        p.fill(textColor, 100);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(12);
        p.text("Tap an app to launch • Tap header to go back", 200, 580);
    }
    
    /**
     * Gets the application at the specified screen coordinates.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The IApplication at that position, or null if none
     */
    private IApplication getAppAtPosition(int x, int y) {
        System.out.println("AppLibraryScreen: getAppAtPosition(" + x + ", " + y + ")");
        System.out.println("AppLibraryScreen: allApps = " + (allApps != null ? allApps.size() + " apps" : "null"));
        System.out.println("AppLibraryScreen: LIST_START_Y = " + LIST_START_Y);
        System.out.println("AppLibraryScreen: scrollOffset = " + scrollOffset);
        
        if (allApps == null || y < LIST_START_Y) {
            System.out.println("AppLibraryScreen: ❌ Position check failed: allApps=" + (allApps != null) + ", y=" + y + " < LIST_START_Y=" + LIST_START_Y);
            return null;
        }
        
        int adjustedY = y + scrollOffset - LIST_START_Y;
        int itemIndex = adjustedY / ITEM_HEIGHT;
        
        System.out.println("AppLibraryScreen: adjustedY = " + adjustedY + ", itemIndex = " + itemIndex + ", ITEM_HEIGHT = " + ITEM_HEIGHT);
        
        if (itemIndex >= 0 && itemIndex < allApps.size()) {
            IApplication app = allApps.get(itemIndex);
            System.out.println("AppLibraryScreen: ✅ Found app at index " + itemIndex + ": " + app.getName());
            return app;
        }
        
        System.out.println("AppLibraryScreen: ❌ Item index out of bounds: " + itemIndex + " (0 to " + (allApps.size()-1) + ")");
        return null;
    }
    
    /**
     * Checks if the app list needs scrolling.
     * 
     * @return true if scrolling is needed, false otherwise
     */
    private boolean needsScrolling() {
        if (allApps == null) return false;
        
        int totalHeight = allApps.size() * ITEM_HEIGHT;
        int visibleHeight = 600 - LIST_START_Y - 40;
        
        return totalHeight > visibleHeight;
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
        
        if (kernel != null && kernel.getScreenManager() != null) {
            try {
                Screen appScreen = app.getEntryScreen(kernel);
                kernel.getScreenManager().pushScreen(appScreen);
            } catch (Exception e) {
                System.err.println("AppLibraryScreen: Failed to launch app " + app.getName() + ": " + e.getMessage());
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
     * アプリをホーム画面に追加する。
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
                    // 全てのページが満員の場合、新しいページを作成
                    HomePage newPage = new HomePage();
                    Shortcut newShortcut = new Shortcut(app);
                    if (newPage.addShortcut(newShortcut)) {
                        homePages.add(newPage);
                        System.out.println("AppLibraryScreen: " + app.getName() + "を新しいページに追加しました");
                        added = true;
                    }
                }
                
                if (added) {
                    // レイアウトを保存
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
     * コンテキストメニューを描画する。
     * 
     * @param p 描画用のPAppletインスタンス
     */
    private void drawContextMenu(PApplet p) {
        System.out.println("AppLibraryScreen: drawContextMenu() called, longPressedApp=" + (longPressedApp != null ? longPressedApp.getName() : "null"));
        
        if (longPressedApp == null) {
            System.out.println("AppLibraryScreen: ❌ Cannot draw context menu - longPressedApp is null");
            return;
        }
        
        System.out.println("AppLibraryScreen: 🎨 Drawing context menu overlay and box...");
        
        // 半透明の背景オーバーレイ
        p.fill(0, 0, 0, 150);
        p.noStroke();
        p.rect(0, 0, p.width, p.height);
        
        // コンテキストメニューのボックス
        int menuWidth = 200;
        int menuHeight = 80;
        int menuX = (p.width - menuWidth) / 2;
        int menuY = (p.height - menuHeight) / 2;
        
        p.fill(backgroundColor + 0x202020); // 少し明るい背景
        p.stroke(accentColor);
        p.strokeWeight(2);
        p.rect(menuX, menuY, menuWidth, menuHeight, 8);
        
        // メニューアイテム: "ホーム画面に追加"
        p.fill(textColor);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(16);
        p.text("ホーム画面に追加", menuX + menuWidth/2, menuY + menuHeight/2);
        
        // 選択された App 名
        p.fill(accentColor);
        p.textSize(12);
        p.text(longPressedApp.getName(), menuX + menuWidth/2, menuY + 20);
    }
    
    /**
     * "ホーム画面に追加"ボタンがクリックされたかどうかを確認する。
     * 
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @return クリックされた場合true
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
     * メニュー外をクリックしたかどうかを確認する。
     * 
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @return メニュー外をクリックした場合true
     */
    private boolean isClickingOutsideMenu(int mouseX, int mouseY) {
        return !isClickingAddToHome(mouseX, mouseY);
    }
    
    /**
     * アプリのコンテキストメニューを新しいポップアップAPIで表示する。
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
        return 100; // 高優先度（ポップアップより低い）
    }
    
    /**
     * タップジェスチャーを処理する。
     * 
     * @param x X座標
     * @param y Y座標
     * @return 処理した場合true
     */
    private boolean handleTap(int x, int y) {
        System.out.println("AppLibraryScreen: Handling tap at (" + x + ", " + y + ")");
        
        // ヘッダー領域のタップ（戻る）
        if (y < LIST_START_Y) {
            goBack();
            return true;
        }
        
        // アプリアイテムのタップ（起動）
        IApplication tappedApp = getAppAtPosition(x, y);
        if (tappedApp != null) {
            System.out.println("AppLibraryScreen: Launching app: " + tappedApp.getName());
            launchApplication(tappedApp);
            return true;
        }
        
        return false;
    }
    
    /**
     * 長押しジェスチャーを処理する。
     * 
     * @param x X座標
     * @param y Y座標
     * @return 処理した場合true
     */
    private boolean handleLongPress(int x, int y) {
        System.out.println("AppLibraryScreen: Handling long press at (" + x + ", " + y + ")");
        
        // ヘッダー領域では長押し無効
        if (y < LIST_START_Y) {
            return false;
        }
        
        // アプリアイテムの長押し（コンテキストメニュー）
        IApplication longPressedApp = getAppAtPosition(x, y);
        if (longPressedApp != null) {
            System.out.println("AppLibraryScreen: ✅ Long press detected for " + longPressedApp.getName() + " - showing popup via GestureManager");
            showContextMenuForApp(longPressedApp);
            return true;
        }
        
        return false;
    }
    
    /**
     * 左スワイプジェスチャーを処理する。
     * 
     * @return 処理した場合true
     */
    private boolean handleSwipeLeft() {
        System.out.println("AppLibraryScreen: Left swipe detected");
        // 必要に応じて実装（ページング等）
        return false;
    }
    
    /**
     * 右スワイプジェスチャーを処理する。
     * 
     * @return 処理した場合true
     */
    private boolean handleSwipeRight() {
        System.out.println("AppLibraryScreen: Right swipe detected - going back");
        goBack();
        return true;
    }
    
    /**
     * ホーム画面のページ一覧を取得するためのゲッターメソッド。
     * これはHomeScreenクラスに追加する必要があります。
     */
    // ホーム画面からページリストを取得するため、HomeScreenクラスにもgetterメソッドを追加する必要があります
}