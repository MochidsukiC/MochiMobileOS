package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.apps.launcher.model.HomePage;
import jp.moyashi.phoneos.core.apps.launcher.model.Shortcut;
import jp.moyashi.phoneos.core.input.GestureListener;
import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import jp.moyashi.phoneos.core.service.sensor.Sensor;
import jp.moyashi.phoneos.core.service.sensor.SensorEvent;
import jp.moyashi.phoneos.core.service.sensor.SensorEventListener;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * The main home screen of the MochiMobileOS launcher application.
 * This screen serves as the primary interface that users see when the OS starts.
 * It displays app shortcuts in a grid layout and provides access to the app library.
 * 
 * The home screen features:
 * - A grid of app shortcuts for quick access to frequently used applications
 * - Swipe right gesture or button click to access the app library
 * - Status information display
 * - Responsive touch interaction
 * 
 * This is the new implementation supporting multi-page editable home screens.
 * Features drag-and-drop, edit mode with delete buttons, and page management.
 * 
 * @author YourName
 * @version 3.0
 * @since 1.0
 */
public class HomeScreen implements Screen, GestureListener, SensorEventListener {
    
    /** Reference to the OS kernel for accessing system services */
    private final Kernel kernel;
    
    /** Background image for the home screen */
    private processing.core.PImage backgroundImage;
    
    /** Background color for the home screen (fallback) */
    private int backgroundColor;
    
    /** Text color for the home screen */
    private int textColor;
    
    /** Accent color for UI elements */
    private int accentColor;

    /** Sensor values */
    private float currentHumidity = 40.0f;
    private float currentLightLevel = 15.0f;
    
    /** Flag to track if the screen has been initialized */
    private boolean isInitialized;
    
    /** List of home screen pages */
    private List<HomePage> homePages;
    
    /** Current page index */
    private int currentPageIndex;
    
    /** Edit mode state */
    private boolean isEditing;
    
    /** Long press detection */
    private long touchStartTime;
    private boolean longPressTriggered;
    private static final long LONG_PRESS_DURATION = 500; // 500ms
    
    /** Dragging state */
    private Shortcut draggedShortcut;
    private boolean isDragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean isAppLibraryScrolling;
    
    /** Page swiping and animation */
    private float swipeStartX;
    private boolean isSwipingPages;
    private static final float SWIPE_THRESHOLD = 50.0f;
    
    /** Page transition animation */
    private float pageTransitionOffset = 0.0f;
    private boolean isAnimating = false;
    private float animationProgress = 0.0f;
    private int targetPageIndex = 0;
    private long animationStartTime = 0;
    private float startOffset = 0.0f; // アニメーション開始時のオフセチE
    private int animationBasePageIndex = 0; // アニメーション中の基準EージE固定）
        private static final long ANIMATION_DURATION = 500; // 500ms for smoother animation
    
        // --- UI Dimension and Font Size Constants (for theme integration preparation) ---
            private static final int STATUS_BAR_HEIGHT = 40; // Height of the status bar    
        private static final int RADIUS_SMALL = 8; // Small corner radius for cards, etc.
        private static final int RADIUS_MEDIUM = 12; // Medium corner radius for icons
    
        private static final int TEXT_SIZE_TINY = 8; // For app names below icons
        private static final int TEXT_SIZE_SMALL = 11; // For page indicators, hints
        private static final int TEXT_SIZE_MEDIUM = 12; // For status bar time, status
        private static final int TEXT_SIZE_LARGE = 14; // For app library item description
        private static final int TEXT_SIZE_XL = 16; // For app library item app name
        private static final int TEXT_SIZE_XXL = 18; // For app library title
        // --- End UI Dimension and Font Size Constants ---
    
        /** Grid configuration for app shortcuts */    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 5;
    private static final int ICON_SIZE = 48; // Reduced from 64 to 48
    private static final int ICON_SPACING = 15; // Reduced from 20 to 15
    
    /** App library navigation area */
    private static final int NAV_AREA_HEIGHT = 100;
    private static final int APP_LIBRARY_LIST_START_Y = 110;
    private static final int APP_LIBRARY_BOTTOM_PADDING = 20;
    
    /**
     * Constructs a new HomeScreen instance.
     * 
     * @param kernel The OS kernel instance providing access to system services
     */
    public HomeScreen(Kernel kernel) {
        this.kernel = kernel;
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            this.backgroundColor = theme.colorBackground();
            this.textColor = theme.colorOnSurface();
            this.accentColor = theme.colorPrimary();
        } else {
            this.backgroundColor = 0x1E1E1E; // fallback
            this.textColor = 0xFFFFFF;
            this.accentColor = 0x4A90E2;
        }
        this.isInitialized = false;
        this.homePages = new ArrayList<>();
        this.currentPageIndex = 0;
        this.isEditing = false;
        this.longPressTriggered = false;
        this.draggedShortcut = null;
        this.isDragging = false;
        this.isSwipingPages = false;
        this.isAppLibraryScrolling = false;
        this.pageTransitionOffset = 0.0f;
        this.isAnimating = false;
        this.targetPageIndex = 0;
        
        System.out.println("📱 HomeScreen: Advanced launcher home screen created");
        System.out.println("    • Multi-page support ready");
        System.out.println("    • Drag & drop system initialized");
        System.out.println("    • Edit mode with animations enabled");
    }
    
    /**
     * Initializes the home screen when it becomes active.
     * Sets up the app shortcuts and prepares the UI for display.
     *
     * @deprecated Use setup(PGraphics g) instead for unified graphics architecture
     */
    @Override
    @Deprecated
    public void setup(processing.core.PApplet p) {
        PGraphics g = p.g;
        setup(g);
    }

    /**
     * Initializes the home screen when it becomes active (PGraphics version).
     * Sets up the app shortcuts and prepares the UI for display.
     *
     * @param g The PGraphics instance for drawing operations
     */
    public void setup(PGraphics g) {
        if (isInitialized) {
            System.out.println("⚠EEHomeScreen: setup() called again - skipping duplicate initialization");
            return;
        }

        try {
            isInitialized = true;
            System.out.println("🚀 HomeScreen: Initializing multi-page launcher...");

            // 背景画像を読み込み
            try {
                loadBackgroundImage();
            } catch (Exception e) {
                System.err.println("❁EHomeScreen: Failed to load background image: " + e.getMessage());
                e.printStackTrace();
            }

            initializeHomePages();

            // Count total shortcuts
            int totalShortcuts = 0;
            for (HomePage page : homePages) {
                totalShortcuts += page.getShortcutCount();
            }

            System.out.println("✁EHomeScreen: Initialization complete!");
            System.out.println("    • Pages created: " + homePages.size());
            System.out.println("    • Total shortcuts: " + totalShortcuts);
            System.out.println("    • Grid size: " + GRID_COLS + "x" + GRID_ROWS + " per page");
            System.out.println("    • Ready for user interaction!");
            System.out.println();
            System.out.println("🎮 HOW TO USE:");
            System.out.println("    • Tap icons to launch apps");
            System.out.println("    • Long press for edit mode");
            System.out.println("    • Drag icons to rearrange");
            System.out.println("    • Swipe left/right for pages");
            System.out.println("    • Swipe up for App Library");

            // Register gesture listener
            if (kernel != null && kernel.getGestureManager() != null) {
                kernel.getGestureManager().addGestureListener(this);
                System.out.println("HomeScreen: Registered gesture listener");
            }
            // Register sensor listener
            if (kernel != null && kernel.getSensorManager() != null) {
                jp.moyashi.phoneos.core.service.sensor.SensorManager sm = kernel.getSensorManager();
                Sensor humiditySensor = sm.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
                if (humiditySensor != null) {
                    sm.registerListener(this, humiditySensor, jp.moyashi.phoneos.core.service.sensor.SensorManager.SENSOR_DELAY_NORMAL);
                }
                Sensor lightSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT);
                if (lightSensor != null) {
                    sm.registerListener(this, lightSensor, jp.moyashi.phoneos.core.service.sensor.SensorManager.SENSOR_DELAY_NORMAL);
                }
            }
        } catch (Exception e) {
            System.err.println("❁EHomeScreen: Critical error during setup: " + e.getMessage());
            e.printStackTrace();
            // 緊急時E少なくとめEつの空ペEジを確俁E
            if (homePages.isEmpty()) {
                homePages.add(new HomePage("Emergency"));
            }
        }
    }
    
    /**
     * Draws the home screen interface.
     * Renders the background, app shortcuts grid, navigation hints, and system information.
     *
     * @param p The PApplet instance for drawing operations
     * @deprecated Use draw(PGraphics g) instead for unified graphics architecture
     */
    @Override
    @Deprecated
    public void draw(PApplet p) {
        PGraphics g = p.g;
        draw(g);
    }

    /**
     * Draws the home screen using PGraphics (unified architecture).
     *
     * @param g The PGraphics instance to draw to
     */
    public void draw(PGraphics g) {
        try {
            // 毎フレームチEEマ更新EE動Eり替え対応）
            var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
            if (theme != null) {
                this.backgroundColor = theme.colorBackground();
                this.textColor = theme.colorOnSurface();
                this.accentColor = theme.colorPrimary();
            }
            // DEBUGログは無効化（パフォーマンス向上EためEE
            // ペEジタイプに応じた背景処理
            HomePage currentPage = getCurrentPage();
            int bg = backgroundColor;
            int br = (bg>>16)&0xFF, gr = (bg>>8)&0xFF, bb = bg&0xFF;
            if (backgroundImage != null && (currentPage == null || !currentPage.isAppLibraryPage())) {
                g.background(br, gr, bb);
                g.image(backgroundImage, 0, 0, 400, 600);
            } else {
                g.background(br, gr, bb);
            }

            // Update page transition animation
            updatePageAnimation();

            // Live follow from gesture manager
            // TODO: syncLivePageDragFromGesture() - method not found, commented out for now
            // syncLivePageDragFromGesture();

            // Check edge auto-slide timer continuously during drag
            updateEdgeAutoSlideTimer();

            // Draw status bar
            drawStatusBar(g);

            // Draw pages with transition animation
            drawPagesWithTransition(g);

            // Draw navigation area
            drawNavigationArea(g);

            // Draw page indicator dots
            drawPageIndicators(g);

            // Draw new dashboard cards
            drawClockAndWeatherCard(g);
            drawSearchCard(g);

        } catch (Exception e) {
            System.err.println("❁EHomeScreen: Draw error (PGraphics) - " + e.getMessage());
            e.printStackTrace();
            // Fallback drawing
            g.background(255, 0, 0);
            g.fill(255);
            g.textAlign(g.CENTER, g.CENTER);

            // 日本語フォントを設定
            if (kernel != null && kernel.getJapaneseFont() != null) {
                g.textFont(kernel.getJapaneseFont());
            }

            g.textSize(TEXT_SIZE_XL);
            g.text("HomeScreen Error: " + e.getMessage(), g.width/2, g.height/2);
        }
    }

    /**
     * Handles mouse press events on the home screen.
     * Processes app shortcut clicks and navigation gestures.
     *
     * @param mouseX The x-coordinate of the mouse press
     * @param mouseY The y-coordinate of the mouse press
     * @deprecated Use mousePressed(PGraphics g, int, int) instead for unified graphics architecture
     */
    @Override
    @Deprecated
    public void mousePressed(processing.core.PApplet p, int mouseX, int mouseY) {
        PGraphics g = p.g;
        mousePressed(g, mouseX, mouseY);
    }

    /**
     * Handles mouse press events on the home screen (PGraphics version).
     * Processes app shortcut clicks and navigation gestures.
     *
     * @param g The PGraphics instance
     * @param mouseX The x-coordinate of the mouse press
     * @param mouseY The y-coordinate of the mouse press
     */
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        System.out.println("HomeScreen: Touch at (" + mouseX + ", " + mouseY + ")");

        touchStartTime = System.currentTimeMillis();
        longPressTriggered = false;
        swipeStartX = mouseX;
        isSwipingPages = false;
        isAppLibraryScrolling = false;

        // Check if click is in navigation area (app library), but not in control center area
        // Control center area starts at 90% of screen height (540px), nav area ends at 500px
        if (mouseY > (600 - NAV_AREA_HEIGHT) && mouseY < 540) {
            openAppLibrary();
            return;
        }

        // Check if click is on a shortcut (座標変換を老EEE)
        Shortcut clickedShortcut = getShortcutAtPositionWithTransform(mouseX, mouseY);
        if (clickedShortcut != null) {
            if (isEditing) {
                // In edit mode, check if clicking delete button
                if (isClickingDeleteButtonWithTransform(mouseX, mouseY, clickedShortcut)) {
                    removeShortcut(clickedShortcut);
                    return;
                }
                // Start potential dragging
                startDragging(clickedShortcut, mouseX, mouseY);
            } else {
                // Normal mode - launch app with animation
                int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
                int startX = (400 - gridWidth) / 2;
                int startY = 80;
                float iconX = startX + clickedShortcut.getGridX() * (ICON_SIZE + ICON_SPACING) + ICON_SIZE / 2;
                float iconY = startY + clickedShortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20) + ICON_SIZE / 2;
                launchApplicationWithAnimation(clickedShortcut.getApplication(), iconX, iconY, ICON_SIZE);
            }
        } else {
            // Empty area - could be page swipe or long press for edit mode
            if (isEditing) {
                // 編雁EEEード中に空のスペEスをクリチEEEした場合E編雁EEEード終了EE予紁E
                // 実際の処理EEGestureManagerのTAPイベントで実行される
                System.out.println("HomeScreen: Empty space clicked in edit mode - will exit on TAP");
            } else {
                // Start monitoring for long press to enter edit mode
            }
        }
    }
    
    /**
     * Checks if the mouse click is on a shortcut's delete button.
     * 
     * @param mouseX The mouse x coordinate
     * @param mouseY The mouse y coordinate
     * @param shortcut The shortcut to check
     * @return true if clicking the delete button
     */
    private boolean isClickingDeleteButton(int mouseX, int mouseY, Shortcut shortcut) {
        if (!isEditing) return false;
        
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        
        int iconX = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
        int iconY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20);
        
        int deleteX = iconX + ICON_SIZE - 8;
        int deleteY = iconY + 8;
        
        return Math.sqrt((mouseX - deleteX) * (mouseX - deleteX) + (mouseY - deleteY) * (mouseY - deleteY)) <= 8;
    }
    
    /**
     * 座標変換を老EEEした削除ボタンクリチEEE判定、
     * 
     * @param mouseX マウスX座標（絶対座標）
     * @param mouseY マウスY座標（絶対座標）
     * @param shortcut 対象のショートカチEEE
     * @return 削除ボタンをクリチEEEした場合rue
     */
    private boolean isClickingDeleteButtonWithTransform(int mouseX, int mouseY, Shortcut shortcut) {
        if (!isEditing) return false;
        
        // 現在の座標変換オフセチEEEを計箁E
        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        
        // ショートカチEEEがどのペEジにあるかを特定
        int shortcutPageIndex = -1;
        for (int i = 0; i < homePages.size(); i++) {
            HomePage page = homePages.get(i);
            if (page.getShortcuts().contains(shortcut)) {
                shortcutPageIndex = i;
                break;
            }
        }
        
        if (shortcutPageIndex == -1) return false;
        
        // ペEジ冁EEEのショートカチEEE座標を計箁E
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        
        int iconX = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
        int iconY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20);
        
        // 画面上での削除ボタン位置を計算（座標変換を老EEEEEEE
        float screenDeleteX = totalOffset + shortcutPageIndex * 400 + iconX + ICON_SIZE - 8;
        float screenDeleteY = iconY + 8;
        
        return Math.sqrt((mouseX - screenDeleteX) * (mouseX - screenDeleteX) + (mouseY - screenDeleteY) * (mouseY - screenDeleteY)) <= 8;
    }
    
    /**
     * Handles mouse drag events for shortcut dragging and page swiping.
     * Note: このメソッドは後方互換性のために残されていますが、
     * 実際の処理は GestureManager システムで行われます。
     *
     * @deprecated Use mouseDragged(PGraphics g, int, int) instead for unified graphics architecture
     */
    @Deprecated
    public void mouseDragged(processing.core.PApplet p, int mouseX, int mouseY) {
        PGraphics g = p.g;
        mouseDragged(g, mouseX, mouseY);
    }

    /**
     * Handles mouse drag events for shortcut dragging and page swiping (PGraphics version).
     * Note: このメソッドは後方互換性のために残されていますが、
     * 実際の処理は GestureManager システムで行われます。
     *
     * @param g The PGraphics instance
     * @param mouseX The x-coordinate of the mouse drag
     * @param mouseY The y-coordinate of the mouse drag
     */
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        // GestureManagerシスチEEEが有効な場合E何もしなぁE
        // 実際のドラチEEE処理EE onGesture -> handleDragMove で実行される
        // パフォーマンス改喁E 頻繁に呼ばれるのでログ出力を抑制
        // System.out.println("HomeScreen: mouseDragged called - delegating to GestureManager");
    }

    /**
     * Handles mouse release events.
     * Note: このメソッドは後方互換性のために残されていますが、
     * 実際の処理は GestureManager システムで行われます。
     *
     * @deprecated Use mouseReleased(PGraphics g, int, int) instead for unified graphics architecture
     */
    @Deprecated
    public void mouseReleased(processing.core.PApplet p, int mouseX, int mouseY) {
        PGraphics g = p.g;
        mouseReleased(g, mouseX, mouseY);
    }

    /**
     * Handles mouse release events (PGraphics version).
     * Note: このメソッドは後方互換性のために残されていますが、
     * 実際の処理は GestureManager システムで行われます。
     *
     * @param g The PGraphics instance
     * @param mouseX The x-coordinate of the mouse release
     * @param mouseY The y-coordinate of the mouse release
     */
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        // GestureManagerシスチEEEが有効な場合E基本皁EEE何もしなぁE
        // 実際の処理EE onGesture -> handleDragEnd, handleLongPress で実行される
        System.out.println("HomeScreen: mouseReleased called - delegating to GestureManager");

        // 念のため状態をリセチEEEEEE安E措置EEEE
        resetDragState();
        isSwipingPages = false;
    }
    
    /**
     * Starts dragging a shortcut.
     * 
     * @param shortcut The shortcut to drag
     * @param mouseX Current mouse X
     * @param mouseY Current mouse Y
     */
    private void startDragging(Shortcut shortcut, int mouseX, int mouseY) {
        draggedShortcut = shortcut;
        isDragging = true;
        shortcut.setDragging(true);
        
        // 座標変換を老EEEしてショートカチEEEの画面上位置を計箁E
        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        
        // ショートカチEEEがどのペEジにあるかを特定
        int shortcutPageIndex = -1;
        for (int i = 0; i < homePages.size(); i++) {
            HomePage page = homePages.get(i);
            if (page.getShortcuts().contains(shortcut)) {
                shortcutPageIndex = i;
                break;
            }
        }
        
        if (shortcutPageIndex != -1) {
            // ペEジ冁EEEのショートカチEEE座標を計箁E
            int startY = 80;
            int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
            int startX = (400 - gridWidth) / 2;
            
            int localShortcutX = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
            int shortcutY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20);
            
            // 画面上でのショートカチEEE位置を計算（座標変換を老EEEEEEE
            int screenShortcutX = (int) (totalOffset + shortcutPageIndex * 400 + localShortcutX);
            
            dragOffsetX = mouseX - screenShortcutX;
            dragOffsetY = mouseY - shortcutY;
        } else {
            // フォールバック: 従来の計箁E
            int startY = 80;
            int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
            int startX = (400 - gridWidth) / 2;
            
            int shortcutX = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
            int shortcutY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20);
            
            dragOffsetX = mouseX - shortcutX;
            dragOffsetY = mouseY - shortcutY;
        }
        
        System.out.println("HomeScreen: Started dragging " + shortcut.getDisplayName());
    }
    
    /**
     * Handles dropping a dragged shortcut.
     * 
     * @param mouseX Drop position X
     * @param mouseY Drop position Y
     */
    private void handleShortcutDrop(int mouseX, int mouseY) {
        if (draggedShortcut == null) return;

        System.out.println("HomeScreen: [DROP] Handling shortcut drop at (" + mouseX + ", " + mouseY + ") on page " + currentPageIndex);

        // アニメーション中の場合EドロチEEEを遅延実衁E
        if (isAnimating) {
            System.out.println("HomeScreen: [DROP] Animation in progress, scheduling drop for later");
            scheduleDelayedDrop(mouseX, mouseY);
            return;
        }

        // 即座にドロチEEEを実衁E
        executeDrop(mouseX, mouseY, draggedShortcut);
    }

    /**
     * Handles moving a shortcut to the next page when dragged to the right edge.
     */
    private void handleShortcutMoveToNextPage() {
        if (draggedShortcut == null) return;

        HomePage currentPage = getCurrentPage();
        if (currentPage != null) {
            // 現在のペEジからショートカチEEEを削除
            currentPage.removeShortcut(draggedShortcut);

            // 次のペEジを取得またE作E
            int nextPageIndex = currentPageIndex + 1;
            if (nextPageIndex >= homePages.size()) {
                // 新しいペEジを作E
                addNewPage();
            }

            // 次のペEジに移勁E
            HomePage nextPage = homePages.get(nextPageIndex);
            if (nextPage != null && !nextPage.isAppLibraryPage()) {
                // 次のペEジの最初E空きスロチEEEに追加
                int[] emptySlot = findFirstEmptySlot(nextPage);
                if (emptySlot != null) {
                    draggedShortcut.setGridPosition(emptySlot[0], emptySlot[1]);
                    nextPage.addShortcut(draggedShortcut);

                    // 次のペEジに自動的にスライチE
                    startPageTransition(nextPageIndex);

                    System.out.println("HomeScreen: ショートカチEEEを次のペEジに移動しました");

                    // レイアウトを自動保孁E
                    saveCurrentLayout();
                } else {
                    // 次のペEジがフルの場合、さらに新しいペEジを作E
                    addNewPage();
                    HomePage newPage = homePages.get(homePages.size() - 1);
                    draggedShortcut.setGridPosition(0, 0);
                    newPage.addShortcut(draggedShortcut);
                    startPageTransition(homePages.size() - 1);

                    System.out.println("HomeScreen: 新しいペEジを作EしてショートカチEEEを移動しました");
                    saveCurrentLayout();
                }
            }
        }

        // Reset drag state
        resetDragState();
    }

    /**
     * Finds the first empty slot in a page.
     * @param page The page to search
     * @return [x, y] coordinates of the first empty slot, or null if page is full
     */
    private int[] findFirstEmptySlot(HomePage page) {
        for (int y = 0; y < GRID_ROWS; y++) {
            for (int x = 0; x < GRID_COLS; x++) {
                if (page.getShortcutAt(x, y) == null) {
                    return new int[]{x, y};
                }
            }
        }
        return null; // Page is full
    }

    /**
     * Handles page swipe gesture.
     * 
     * @param mouseX Final mouse X position
     */
    private void handlePageSwipe(int mouseX) {
        float swipeDistance = mouseX - swipeStartX;
        
        if (swipeDistance > SWIPE_THRESHOLD) {
            // Swipe right - go to previous page
            if (currentPageIndex > 0) {
                currentPageIndex--;
                System.out.println("HomeScreen: Swiped to page " + currentPageIndex);
            }
        } else if (swipeDistance < -SWIPE_THRESHOLD) {
            // Swipe left - go to next page
            if (currentPageIndex < homePages.size() - 1) {
                currentPageIndex++;
                System.out.println("HomeScreen: Swiped to page " + currentPageIndex);
            }
        }
    }
    
    /**
     * Converts screen coordinates to grid position.
     * 
     * @param screenX Screen X coordinate
     * @param screenY Screen Y coordinate
     * @return Grid position [x, y] or null if invalid
     */
    private int[] screenToGridPosition(int screenX, int screenY) {
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        
        // Check if within grid bounds
        if (screenX < startX || screenY < startY) {
            return null;
        }
        
        int gridX = (screenX - startX) / (ICON_SIZE + ICON_SPACING);
        int gridY = (screenY - startY) / (ICON_SIZE + ICON_SPACING + 20);
        
        if (gridX >= 0 && gridX < GRID_COLS && gridY >= 0 && gridY < GRID_ROWS) {
            return new int[]{gridX, gridY};
        }
        
        return null;
    }
    
    /**
     * Resets drag-related state.
     */
    private void resetDragState() {
        if (draggedShortcut != null) {
            draggedShortcut.setDragging(false);
            draggedShortcut = null;
        }
        isDragging = false;
    }
    
    /**
     * Removes a shortcut from the current page.
     * 
     * @param shortcut The shortcut to remove
     */
    private void removeShortcut(Shortcut shortcut) {
        HomePage currentPage = getCurrentPage();
        if (currentPage != null) {
            currentPage.removeShortcut(shortcut);
            System.out.println("HomeScreen: ショートカチEEE削除: " + shortcut.getDisplayName());
            
            // レイアウトを自動保孁E
            saveCurrentLayout();
        }
    }
    
    /**
     * 現在のレイアウトをLayoutManagerに保存する、
     */
    private void saveCurrentLayout() {
        if (kernel != null && kernel.getLayoutManager() != null && homePages != null) {
            boolean success = kernel.getLayoutManager().saveLayout(homePages);
            if (success) {
                System.out.println("HomeScreen: レイアウト保存成功");
            } else {
                System.err.println("HomeScreen: レイアウト保存失敗");
            }
        }
    }
    
    /**
     * Cleans up resources when the screen is deactivated.
     *
     * @deprecated Use cleanup(PGraphics g) instead for unified graphics architecture
     */
    @Override
    @Deprecated
    public void cleanup(processing.core.PApplet p) {
        PGraphics g = p.g;
        cleanup(g);
    }

    /**
     * Cleans up the home screen resources (PGraphics version).
     *
     * @param g The PGraphics instance
     */
    public void cleanup(PGraphics g) {
        // Unregister gesture listener
        if (kernel != null && kernel.getGestureManager() != null) {
            kernel.getGestureManager().removeGestureListener(this);
            System.out.println("HomeScreen: Unregistered gesture listener");
        }
        // Unregister sensor listener
        if (kernel != null && kernel.getSensorManager() != null) {
            kernel.getSensorManager().unregisterListener(this);
        }

        isInitialized = false;
        resetDragState();
        isEditing = false;
        System.out.println("HomeScreen: Launcher home screen cleaned up");
    }
    
    /**
     * 背景画像を読み込む、
     */
    private void loadBackgroundImage() {
        try {
            // TODO: PGraphics統一アーキチEEEチャに対応した画像読み込み機Eを実裁E
            // 現在はbackgroundImageをnullのままにして、色背景を使用
            System.out.println("HomeScreen: Background image loading disabled in PGraphics architecture - using color background");
            backgroundImage = null;
        } catch (Exception e) {
            System.err.println("HomeScreen: Error loading background image: " + e.getMessage());
            backgroundImage = null;
        }
    }
    
    /**
     * Gets the title of this screen.
     * 
     * @return The screen title
     */
    @Override
    public String getScreenTitle() {
        return "Home Screen";
    }
    
    /**
     * ホEムペEジのリストを取得する、
     * AppLibraryScreenからアクセスするために使用される、
     * 
     * @return ホEムペEジのリスチE
     */
    public List<HomePage> getHomePages() {
        return homePages;
    }
    
    /**
     * 最初EペEジEEEメインホEムペEジEEEに移動する、
     * スペEスキーによるホEムナビゲーション用、
     */
    public void navigateToFirstPage() {
        System.out.println("HomeScreen: Navigating to first page");
        
        if (!homePages.isEmpty() && currentPageIndex != 0 && !isAnimating) {
            startPageTransition(0);
        } else if (currentPageIndex == 0) {
            System.out.println("HomeScreen: Already on first page");
        }
    }
    
    /**
     * ホEムペEジをE期化し、保存されたレイアウトを読み込むかアプリをE置する、
     * まず保存されたレイアウトE読み込みを試行し、存在しなぁEEE合EチEEEォルトレイアウトを作Eする、
     */
    private void initializeHomePages() {
        try {
            homePages.clear();

            // 保存されたレイアウトを読み込む試行（一時的に無効化）
            boolean layoutLoaded = false;
            // TEMPORARY FIX: Skip saved layout loading to avoid potential issues
            /*
            if (kernel != null && kernel.getLayoutManager() != null) {
                System.out.println("HomeScreen: 保存されたレイアウトを読み込み中...");
                List<HomePage> savedLayout = kernel.getLayoutManager().loadLayout();

                if (savedLayout != null && !savedLayout.isEmpty()) {
                    homePages.addAll(savedLayout);
                    layoutLoaded = true;
                    System.out.println("HomeScreen: 保存されたレイアウトを復允EEEました (" + homePages.size() + "ペEジ)");
                } else {
                    System.out.println("HomeScreen: 保存されたレイアウトが見つかりません、デフォルトレイアウトを作E");
                }
            }
            */
            System.out.println("HomeScreen: チEEEォルトレイアウトを作E中...");


            // 保存されたレイアウトがなぁEEE合EチEEEォルトレイアウトを作E
            if (!layoutLoaded) {
                createDefaultLayout();
            }
            
            // AppLibraryペEジの重褁EEE防ぐ（厳寁EEEチェチEEEEEEE
            long appLibraryCount = homePages.stream()
                .filter(page -> page.getPageType() == HomePage.PageType.APP_LIBRARY)
                .count();
                
            System.out.println("HomeScreen: 現在のAppLibraryペEジ数: " + appLibraryCount);
            
            if (appLibraryCount == 0) {
                createAppLibraryPage();
                System.out.println("HomeScreen: AppLibraryペEジを新規追加しました");
            } else if (appLibraryCount > 1) {
                // 重褁EEEある場合E修正
                System.out.println("HomeScreen: ⚠EEEEAppLibraryペEジが重褁EEEてぁEEEぁE" + appLibraryCount + "倁E - 修正中...");
                // 最初EもE以外を削除
                List<HomePage> toRemove = new ArrayList<>();
                boolean foundFirst = false;
                for (HomePage page : homePages) {
                    if (page.getPageType() == HomePage.PageType.APP_LIBRARY) {
                        if (foundFirst) {
                            toRemove.add(page);
                        } else {
                            foundFirst = true;
                        }
                    }
                }
                homePages.removeAll(toRemove);
                System.out.println("HomeScreen: ✁E" + toRemove.size() + "個E重複AppLibraryペEジを削除しました");
            } else {
                System.out.println("HomeScreen: AppLibraryページは既に存在します");
            }

            System.out.println("HomeScreen: " + homePages.size() + "ページでホーム画面を初期化完了");
            
        } catch (Exception e) {
            System.err.println("HomeScreen: initializeHomePages でクリチEカルエラー: " + e.getMessage());
            e.printStackTrace();
            // 緊急時E少なくとめEつの空ペEジを確俁E
            if (homePages.isEmpty()) {
                homePages.add(new HomePage("Emergency"));
            }
        }
    }
    
    /**
     * チEEEォルトEレイアウトを作Eし、利用可能なアプリをE置する、
     */
    private void createDefaultLayout() {
        // 最初EペEジを作E
        HomePage firstPage = new HomePage("Home");
        homePages.add(firstPage);
        
        if (kernel != null && kernel.getAppLoader() != null) {
            try {
                List<IApplication> loadedApps = kernel.getAppLoader().getLoadedApps();
                if (loadedApps != null) {
                    // ランチャー以外Eロード済みアプリを追加
                    List<IApplication> availableApps = new ArrayList<>();
                    for (IApplication app : loadedApps) {
                        if (app != null && !"jp.moyashi.phoneos.core.apps.launcher".equals(app.getApplicationId())) {
                            availableApps.add(app);
                        }
                    }
                    
                    HomePage currentPage = firstPage;
                    for (IApplication app : availableApps) {
                        try {
                            if (currentPage.isFull()) {
                                // 現在のペEジが満員の場合E新しいペEジを作E
                                currentPage = new HomePage();
                                homePages.add(currentPage);
                            }
                            currentPage.addShortcut(app);
                        } catch (Exception e) {
                            System.err.println("HomeScreen: ペEジへのアプリ追加エラー: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("HomeScreen: AppLoaderアクセスエラー: " + e.getMessage());
            }
        } else {
            System.out.println("HomeScreen: KernelまたEAppLoaderがnull - 空のペEジを作E");
        }
        
        // チEEEォルトレイアウトを保孁E
        if (kernel != null && kernel.getLayoutManager() != null) {
            kernel.getLayoutManager().saveLayout(homePages);
            System.out.println("HomeScreen: チEEEォルトレイアウトを保存しました");
        }
    }
    
    /**
     * AppLibraryペEジを作Eし、アプリケーションを設定する、
     */
    private void createAppLibraryPage() {
        System.out.println("HomeScreen: AppLibraryペEジを作E中...");
        
        // AppLibraryペEジを作E
        HomePage appLibraryPage = new HomePage(HomePage.PageType.APP_LIBRARY, "App Library");
        
        // 全アプリケーションを取得してAppLibraryペEジに設定
        if (kernel != null && kernel.getAppLoader() != null) {
            try {
                List<IApplication> allApps = kernel.getAppLoader().getLoadedApps();
                if (allApps != null) {
                    // ランチャー以外Eアプリを取征E
                    List<IApplication> availableApps = new ArrayList<>();
                    for (IApplication app : allApps) {
                        if (app != null && !"jp.moyashi.phoneos.core.apps.launcher".equals(app.getApplicationId())) {
                            availableApps.add(app);
                        }
                    }
                    appLibraryPage.setAllApplications(availableApps);
                    System.out.println("HomeScreen: AppLibraryページに " + availableApps.size() + " 個のアプリを設定");
                }
            } catch (Exception e) {
                System.err.println("HomeScreen: AppLibraryペEジ作Eエラー: " + e.getMessage());
            }
        }
        
        // ペEジリストに追加
        homePages.add(appLibraryPage);
        System.out.println("HomeScreen: AppLibraryペEジを追加しました");
        System.out.println("HomeScreen: 総Eージ数: " + homePages.size() + ", AppLibraryペEジインチEEEクス: " + (homePages.size() - 1));
    }
    
    /**
     * Draws the status bar at the top of the screen.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawStatusBar(PGraphics g) {
        try {
            // チEEEマ色
            var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
            int onSurface = theme != null ? theme.colorOnSurface() : textColor;
            int success = theme != null ? theme.colorSuccess() : 0xFF4CAF50;
            int warning = theme != null ? theme.colorWarning() : 0xFFFF9800;

            // Draw status bar background with semi-transparency
            int surfaceColor = theme != null ? theme.colorSurface() : 0xFF2A2A2A; // Fallback
            int alpha = 180; // Semi-transparent alpha for holographic effect
            g.fill((surfaceColor>>16)&0xFF, (surfaceColor>>8)&0xFF, surfaceColor&0xFF, alpha);
            g.noStroke();
            g.rect(0, 0, 400, 40); // Assuming status bar height is 40px

            { int c=onSurface; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, 180); } // Semi-transparent text
            g.textAlign(g.LEFT, g.TOP);
            g.textSize(TEXT_SIZE_MEDIUM);
            
            // Current time
            if (kernel != null && kernel.getSystemClock() != null) {
                try {
                    g.text(kernel.getSystemClock().getFormattedTime(), 15, 15);
                } catch (Exception e) {
                    g.text("--:--", 15, 15);
                }
            } else {
                g.text("No Clock", 15, 15);
            }
            
            // System status and current page info
            g.textAlign(g.RIGHT, g.TOP);
            g.text("MochiOS", 385, 15);
            
            // Current page name
            if (!homePages.isEmpty() && currentPageIndex < homePages.size()) {
                HomePage currentPage = homePages.get(currentPageIndex);
                String pageName = currentPage.isAppLibraryPage() ? "App Library" : 
                                 currentPage.getPageName() != null ? currentPage.getPageName() : 
                                 "Page " + (currentPageIndex + 1);
                                 
                { int c=onSurface; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, 150); }
                g.textAlign(g.CENTER, g.TOP);
                g.textSize(TEXT_SIZE_SMALL);
                g.text(pageName, 200, 15);
            }

            // Status indicator
            if (isInitialized) {
                g.fill((success>>16)&0xFF, (success>>8)&0xFF, success&0xFF);
            } else {
                g.fill((warning>>16)&0xFF, (warning>>8)&0xFF, warning&0xFF);
            }
            g.noStroke();
            g.ellipse(370, 20, 8, 8);
            
        } catch (Exception e) {
            System.err.println("Error in drawStatusBar: " + e.getMessage());
            // Fallback: just draw a simple status
            g.fill(255);
            g.textAlign(g.LEFT, g.TOP);
            g.textSize(TEXT_SIZE_MEDIUM);
            g.text("Status Error", 15, 15);
        }
    }
    
    /**
     * アニメーションの進行度を更新する、
     */
    private void updatePageAnimation() {
        if (!isAnimating) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - animationStartTime;
        long effectiveDuration = ANIMATION_DURATION;
        if (kernel != null && kernel.getSettingsManager() != null) {
            effectiveDuration = jp.moyashi.phoneos.core.ui.effects.Motion.durationAdjusted((int)ANIMATION_DURATION, kernel.getSettingsManager());
        }

        if (elapsed >= effectiveDuration) {
            // アニメーション完了
            completePageTransition();
        } else {
            // アニメーション進行中 - イージング関数を適用
            float t = (float) elapsed / (float)Math.max(1L, effectiveDuration);
            animationProgress = jp.moyashi.phoneos.core.ui.effects.Motion.easeOutCubic(t);
            
            // ペEジオフセチEEEを計箁E
            if (targetPageIndex == animationBasePageIndex) {
                // 允EEEペEジに戻るアニメーション - ドラチEEE位置から0に戻めE
                pageTransitionOffset = startOffset * (1.0f - animationProgress);
            } else {
                // ペEジ刁EEE替えアニメーション - 開始位置から目標位置への補間
                float targetOffset = (animationBasePageIndex - targetPageIndex) * 400;
                pageTransitionOffset = startOffset + (targetOffset - startOffset) * animationProgress;
                System.out.println("🎬 Animation: basePage=" + animationBasePageIndex + " to targetPage=" + targetPageIndex + 
                                 ", startOffset=" + startOffset + ", targetOffset=" + targetOffset + ", progress=" + animationProgress + ", offset=" + pageTransitionOffset);
            }
        }
    }
    
    /**
     * イージング関数EEEEase-out quad - より穏やか）
     * 
     * @param t 進行度 (0.0 EEEE1.0)
     * @return イージング適用後E値
     */
    private float easeOutCubic(float t) {
        // ease-out quadratic - より自然で穏やかな動き
        return 1 - (1 - t) * (1 - t);
    }
    
    /**
     * ペEジ刁EEE替えアニメーションを完了EEる、
     */
    private void completePageTransition() {
        // アニメーション完了EEにペEジインチEEEクスを更新し、座標系をリセチEEE
        System.out.println("🎬 Completing transition: currentPage=" + currentPageIndex + " -> targetPage=" + targetPageIndex);
        
        // ペEジインチEEEクスを目標に更新
        currentPageIndex = targetPageIndex;
        
        // 座標系をリセチEEE
        pageTransitionOffset = 0.0f;
        isAnimating = false;
        animationProgress = 0.0f;
        startOffset = 0.0f;
        
        System.out.println("🎬 Page transition completed to page " + currentPageIndex + ", offset reset to 0");

        // アニメーション完了EEに遁EEEされたドロチEEEを実衁E
        executePendingDrop();
    }
    
    /**
     * ペEジ刁EEE替えアニメーションを開始する、
     * 
     * @param newPageIndex 目標EージインチEEEクス
     */
    private void startPageTransition(int newPageIndex) {
        if (newPageIndex == currentPageIndex || isAnimating) {
            return; // 同じペEジまたEアニメーション中は無要E
        }
        
        targetPageIndex = newPageIndex;
        isAnimating = true;
        animationStartTime = System.currentTimeMillis();
        animationProgress = 0.0f;
        startOffset = pageTransitionOffset; // 現在のオフセチEEEを保孁E
        animationBasePageIndex = currentPageIndex; // 座標計算E基準Eージを固定
        
        System.out.println("🎬 Starting page transition from " + currentPageIndex + " to " + targetPageIndex + " with startOffset=" + startOffset + ", basePageIndex=" + animationBasePageIndex);
    }
    
    /**
     * Draws pages with smooth transition animation using matrix transformation.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawPagesWithTransition(PGraphics g) {
        if (homePages.isEmpty()) {
            // No pages, show message
            g.fill(255, 255, 255, 150);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(TEXT_SIZE_XL);
            g.text("No apps installed", 200, 300);
            g.textSize(TEXT_SIZE_MEDIUM);
            g.text("Swipe up to access app library", 200, 320);
            return;
        }
        
        // 座標変換でペEジ刁EEE替えアニメーションを実現
        g.pushMatrix();

        // ペEジ全体EオフセチEEEを適用
        // アニメーション中は基準EージEEEEnimationBasePageIndexEEEを使用してジャンプを防ぁE
        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        g.translate(totalOffset, 0);
        
        if (isAnimating) {
            System.out.println("🎨 Drawing with basePageIndex=" + basePageForOffset + ", pageTransitionOffset=" + pageTransitionOffset + ", totalOffset=" + totalOffset);
        }
        
        // 全ペEジを横に並べて描画
        for (int i = 0; i < homePages.size(); i++) {
            g.pushMatrix();
            g.translate(i * 400, 0); // 合EEージめE00px間隔で配置
            
            HomePage page = homePages.get(i);
            if (page.isAppLibraryPage()) {
                drawAppLibraryPage(g, page);
            } else {
                drawNormalPage(g, page);
            }
            
            g.popMatrix();
        }
        
        g.popMatrix();

        // ドラチEEE中のアイコンを最上位レイヤーEEE変換なし）で描画
        if (isDragging && draggedShortcut != null) {
            drawDraggedShortcut(g, draggedShortcut);
        }
    }
    
    /**
     * マウス座標をペEジ座標系に変換する、
     * 
     * @param mouseX マウスX座樁E
     * @param mouseY マウスY座樁E
     * @return [変換後X座樁E 変換後Y座樁E ペEジインチEEEクス]
     */
    private int[] transformMouseCoordinates(int mouseX, int mouseY) {
        // 現在の変換行Eを老EEEしてマウス座標を変換
        float totalOffset = -currentPageIndex * 400 + pageTransitionOffset;
        float transformedX = mouseX - totalOffset;
        
        // どのペEジ上EクリチEEEかを判定
        int targetPageIndex = (int) (transformedX / 400);
        if (targetPageIndex < 0) targetPageIndex = 0;
        if (targetPageIndex >= homePages.size()) targetPageIndex = homePages.size() - 1;
        
        // ペEジ冁EEE標に変換
        float pageX = transformedX - (targetPageIndex * 400);
        
        return new int[]{(int) pageX, mouseY, targetPageIndex};
    }
    
    /**
     * Draws a normal home page with shortcuts.
     * 
     * @param p The PApplet instance for drawing
     * @param page The page to draw
     */
    private void drawNormalPage(PGraphics g, HomePage page) {
        // 通常のペEジ描画処理
        int startY = 80; // Below status bar
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2; // Center the grid
        
        g.textAlign(g.CENTER, g.TOP);
        g.textSize(TEXT_SIZE_TINY);
        
        // First draw non-dragged shortcuts
        for (Shortcut shortcut : page.getShortcuts()) {
            if (shortcut.isDragging()) continue; // Draw dragged shortcuts last
            
            int x = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
            int y = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20); // Extra space for app name
            
            // Apply wiggle animation if in edit mode
            if (isEditing) {
                x += (int) (Math.sin(System.currentTimeMillis() * 0.01 + shortcut.getShortcutId().hashCode()) * 2);
                y += (int) (Math.cos(System.currentTimeMillis() * 0.012 + shortcut.getShortcutId().hashCode()) * 1.5);
            }
            
            // Draw shortcut
            drawShortcut(g, shortcut, x, y);
        }
        
        // ドラチEEE中のショートカチEEEは最上位レイヤーで描画するため、ここではスキチEEE
        // (drawPagesWithTransitionの最後で描画されめE
        
        // Draw drop target indicators if dragging
        if (isDragging) {
            drawDropTargets(g, startX, startY);
        }
    }
    
    
    /**
     * AppLibraryペEジを描画する、
     * 
     * @param p The PApplet instance for drawing
     * @param appLibraryPage AppLibraryペEジ
     */
    private void drawAppLibraryPage(PGraphics g, HomePage appLibraryPage) {
        System.out.println("🎨 HomeScreen: drawAppLibraryPage() called - drawing AppLibrary content");

        // AppLibraryタイトルを描画Eテーマ色EE
        var themeAL = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int onSurfaceAL = themeAL != null ? themeAL.colorOnSurface() : 0xFF111111;
        { int c=onSurfaceAL; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF); }
        g.textAlign(g.CENTER, g.TOP);
        g.textSize(TEXT_SIZE_XXL);
        System.out.println("🎨 Drawing title: 'App Library' at (200, 70) with size 18, color RGB(255,255,255)");
        g.text("App Library", 200, 70);
        System.out.println("🎨 Title drawing completed");

        // アプリリストを描画
        List<IApplication> apps = appLibraryPage.getAllApplications();
        System.out.println("🎨 AppLibrary apps count: " + apps.size());
        if (apps.isEmpty()) {
            { int c=onSurfaceAL; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, 150); }
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(TEXT_SIZE_LARGE);
            g.text("No apps available", 200, 300);
            System.out.println("🎨 'No apps available' message drawn at (200, 300)");
            return;
        }
        
        int startY = 110; // タイトルの下から開始
        int listHeight = 600 - startY - NAV_AREA_HEIGHT - 20; // 利用可能な高さ
        int itemHeight = 70; // 合EEプリアイチEEEの高さ
        int scrollOffset = appLibraryPage.getScrollOffset();
        System.out.println("🎨 Drawing " + apps.size() + " apps starting at Y=" + startY + ", scrollOffset=" + scrollOffset);
        
        // スクロール可能エリアを設定（クリチEEEングEEEE
        g.pushMatrix();
        
        // アプリリストを描画
        for (int i = 0; i < apps.size(); i++) {
            IApplication app = apps.get(i);
            int itemY = startY + i * itemHeight - scrollOffset;
            
            // 表示エリア外EアイチEEEはスキチEEE
            if (itemY + itemHeight < startY || itemY > startY + listHeight) {
                continue;
            }
            
            drawAppLibraryItem(g, app, 20, itemY, 360, itemHeight);
        }
        
        g.popMatrix();
        
        // スクロールインジケーターを描画
        if (appLibraryPage.needsScrolling(listHeight, itemHeight)) {
            drawScrollIndicator(g, appLibraryPage, startY, listHeight, itemHeight);
        }
    }
    
    /**
     * AppLibraryのアプリアイチEEEを描画する、
     * 
     * @param p The PApplet instance for drawing
     * @param app 描画するアプリケーション
     * @param x アイチEEEのX座樁E
     * @param y アイチEEEのY座樁E
     * @param width アイチEEEの幁E
     * @param height アイチEEEの高さ
     */
    private void drawAppLibraryItem(PGraphics g, IApplication app, int x, int y, int width, int height) {
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int surface = theme != null ? theme.colorSurface() : 0xFFFFFFFF;
        int border = theme != null ? theme.colorBorder() : 0xFFDDDDDD;
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFF111111;
        int onSurfaceSec = theme != null ? theme.colorOnSurfaceSecondary() : 0xFF666666;

        // カード背景EEE薁EEEE
        jp.moyashi.phoneos.core.ui.effects.Elevation.drawRectShadow(g, x, y, width, height, 8, 1);
        g.fill((surface>>16)&0xFF, (surface>>8)&0xFF, surface&0xFF, 200); // Semi-transparent surface
        g.stroke((border>>16)&0xFF, (border>>8)&0xFF, border&0xFF);
        g.strokeWeight(1);
        g.rect(x, y, width, height, RADIUS_SMALL);

        // アプリアイコンのプレースホルダはアクセント色
        int acc = theme != null ? theme.colorPrimary() : 0xFF4A90E2;
        g.noStroke();
        g.fill((acc>>16)&0xFF, (acc>>8)&0xFF, acc&0xFF);
        g.rect(x + 10, y + 10, 50, 50, RADIUS_SMALL);

        // アプリ名E最初E斁EE
        { int c=onSurface; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF); }
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(24);
        String initial = app.getName().substring(0, 1).toUpperCase();
        g.text(initial, x + 35, y + 35);

        // アプリ合
        { int c=onSurface; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF); }
        g.textAlign(g.LEFT, g.CENTER);
        g.textSize(TEXT_SIZE_XL);
        g.text(app.getName(), x + 75, y + 25);

        // アプリ説明（あれEEE
        if (app.getDescription() != null && !app.getDescription().isEmpty()) {
            { int c=onSurfaceSec; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, 200); }
            g.textSize(TEXT_SIZE_MEDIUM);
            String description = app.getDescription();
            if (description.length() > 40) {
                description = description.substring(0, 37) + "...";
            }
            g.text(description, x + 75, y + 45);
        }
        
        // 長押し時の「Eーム画面に追加」Eタンを描画
        // EEE実裁EEE後で追加EEEE
    }
    
    /**
     * スクロールインジケーターを描画する、
     * 
     * @param p The PApplet instance for drawing
     * @param appLibraryPage AppLibraryペEジ
     * @param listStartY リスト開始Y座樁E
     * @param listHeight リストE高さ
     * @param itemHeight アイチEEE高さ
     */
    private void drawScrollIndicator(PGraphics g, HomePage appLibraryPage, int listStartY, int listHeight, int itemHeight) {
        List<IApplication> apps = appLibraryPage.getAllApplications();
        int totalHeight = apps.size() * itemHeight;
        int scrollOffset = appLibraryPage.getScrollOffset();
        
        // スクロールバEの位置とサイズを計箁E
        float scrollbarHeight = Math.max(20, (float) listHeight * listHeight / totalHeight);
        float scrollbarY = listStartY + (float) scrollOffset * listHeight / totalHeight;
        
        // スクロールバEを描画
        g.fill(255, 255, 255, 100); // textColor with alpha -> RGB
        g.noStroke();
        g.rect(385, (int) scrollbarY, 6, (int) scrollbarHeight, 3);
    }
    
    /**
     * ドラチEEE中のショートカチEEEを絶対座標で描画する、
     * 座標変換の影響を受けずにマウス位置に正確に追従する、
     * 
     * @param p The PApplet instance for drawing
     * @param shortcut The dragged shortcut
     */
    private void drawDraggedShortcut(PGraphics g, Shortcut shortcut) {
        if (!shortcut.isDragging()) return;
        
        int x = (int) shortcut.getDragX();
        int y = (int) shortcut.getDragY();
        
        // ドロチEEEシャドウを描画
        g.fill(0, 0, 0, 100);
        g.noStroke();
        g.rect(x + 4, y + 4, ICON_SIZE, ICON_SIZE, RADIUS_MEDIUM);

        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int surface = theme != null ? theme.colorSurface() : 0xFFFFFFFF; // Fallback to white
        int border = theme != null ? theme.colorBorder() : 0xFF888888; // Fallback to gray

        // アイコンの背景を描画EEE半透EEEEE
        g.fill((surface>>16)&0xFF, (surface>>8)&0xFF, surface&0xFF, 220); // Semi-transparent surface
        g.stroke((border>>16)&0xFF, (border>>8)&0xFF, border&0xFF);
        g.strokeWeight(2);
        g.rect(x, y, ICON_SIZE, ICON_SIZE, RADIUS_MEDIUM);

        // アプリアイコンを描画
        IApplication app = shortcut.getApplication();
        if (app != null) {
            drawAppIcon(g, app, x + ICON_SIZE/2, y + ICON_SIZE/2);
        }

        // アプリ名を描画EEEドラチEEE中も同じスタイルEEEE
        g.noStroke();
        g.textAlign(g.CENTER, g.TOP); // 中央配置、上詰めE
        g.textSize(8); // メインのアイコンと同じフォントサイズ
        
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 10) {
            displayName = displayName.substring(0, 9) + "...";
        }
        
        // チEEEストE影を追加EEEドラチEEE中も可読性向上）
        g.fill(0, 0, 0, 120); // 少し濁EEE影
        g.text(displayName, x + ICON_SIZE/2 + 1, y + ICON_SIZE + 9);

        // メインチEEEストを描画
        g.fill(255, 255, 255); // 白色チEEEスチE
        g.text(displayName, x + ICON_SIZE/2, y + ICON_SIZE + 8);
    }
    
    /**
     * Draws drop target indicators during drag operation.
     * 
     * @param p The PApplet instance for drawing
     * @param startX Grid start X position
     * @param startY Grid start Y position
     */
    private void drawDropTargets(PGraphics g, int startX, int startY) {
        HomePage currentPage = getCurrentPage();
        if (currentPage == null) return;
        
        g.stroke(accentColor, 150);
        g.strokeWeight(2);
        g.noFill();
        
        // Draw indicators for empty positions
        for (int gridX = 0; gridX < GRID_COLS; gridX++) {
            for (int gridY = 0; gridY < GRID_ROWS; gridY++) {
                if (currentPage.isPositionEmpty(gridX, gridY)) {
                    int x = startX + gridX * (ICON_SIZE + ICON_SPACING);
                    int y = startY + gridY * (ICON_SIZE + ICON_SPACING + 20);
                    
                    g.rect(x, y, ICON_SIZE, ICON_SIZE, RADIUS_MEDIUM);
                }
            }
        }
    }
    
    /**
     * Draws an individual shortcut.
     * 
     * @param p The PApplet instance for drawing
     * @param shortcut The shortcut to draw
     * @param x The x coordinate for the shortcut
     * @param y The y coordinate for the shortcut
     */
    private void drawShortcut(PGraphics g, Shortcut shortcut, int x, int y) {
        IApplication app = shortcut.getApplication();
        
        // ドラチEEE中のショートカチEEEは専用メソチEEEで描画されるため、ここでは描画しなぁE
        if (shortcut.isDragging()) {
            return;
        }
        
        // Draw app icon tile (theme-aware)
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int surface = theme != null ? theme.colorSurface() : 0xFFFFFFFF;
        int border = theme != null ? theme.colorBorder() : 0xFFCCCCCC;
        // subtle shadow to separate from light background
        jp.moyashi.phoneos.core.ui.effects.Elevation.drawRectShadow(g, x, y, ICON_SIZE, ICON_SIZE, 12, 2);
        g.fill((surface>>16)&0xFF, (surface>>8)&0xFF, surface&0xFF, 200); // Semi-transparent surface
        g.stroke((border>>16)&0xFF, (border>>8)&0xFF, border&0xFF);
        g.strokeWeight(1);
        g.rect(x, y, ICON_SIZE, ICON_SIZE, RADIUS_MEDIUM);
        
        // Draw app icon
        drawAppIcon(g, app, x + ICON_SIZE/2, y + ICON_SIZE/2);
        
        // Draw delete button if in edit mode
        if (isEditing) {
            g.fill(theme != null ? theme.colorError() : 0xFFDD4444); // Red delete button, theme-aware
            g.noStroke();
            g.ellipse(x + ICON_SIZE - 8, y + 8, 16, 16);
            
            // Draw X
            g.fill(textColor);
            g.strokeWeight(2);
            g.stroke(textColor);
            g.line(x + ICON_SIZE - 12, y + 4, x + ICON_SIZE - 4, y + 12);
            g.line(x + ICON_SIZE - 12, y + 12, x + ICON_SIZE - 4, y + 4);
        }
        
        // Draw app name below the icon
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFF111111;
        { int c=onSurface; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF); }
        g.noStroke();
        g.textAlign(g.CENTER, g.TOP); // 中央配置、上詰めE       g.textSize(8); // 適刁EEEフォントサイズ
        
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 10) {
            displayName = displayName.substring(0, 9) + "...";
        }
        
        // subtle shadow for readability on light backgrounds
        g.fill(0, 0, 0, 60);
        g.text(displayName, x + ICON_SIZE/2 + 1, y + ICON_SIZE + 9);
        { int c=onSurface; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF); }
        g.text(displayName, x + ICON_SIZE/2, y + ICON_SIZE + 8);
    }
    
    /**
     * Draws an individual app icon.
     * If the application provides an icon image, it is drawn.
     * Otherwise, a placeholder is drawn.
     *
     * @param p The PApplet instance for drawing
     * @param app The application to draw an icon for
     * @param centerX The center X coordinate for the icon
     * @param centerY The center Y coordinate for the icon
     */
    private void drawAppIcon(PGraphics g, IApplication app, int centerX, int centerY) {
        if (app == null) {
            return;
        }

        processing.core.PImage icon = app.getIcon();

        if (icon != null) {
            // SECURITY FIX: Force crop/resize any icon to 64x64 to prevent oversized icons from covering the screen
            // Don't trust app-provided icon sizes - always enforce our size constraints
            final int MAX_ICON_SIZE = 64;
            processing.core.PImage safeIcon = icon;

            // If icon is larger than our max size, crop it from center
            if (icon.width > MAX_ICON_SIZE || icon.height > MAX_ICON_SIZE) {
                System.out.println("[HomeScreen] Cropping oversized icon for " + app.getName() +
                    " from " + icon.width + "x" + icon.height + " to " + MAX_ICON_SIZE + "x" + MAX_ICON_SIZE);

                // Calculate center crop coordinates
                int cropX = Math.max(0, (icon.width - MAX_ICON_SIZE) / 2);
                int cropY = Math.max(0, (icon.height - MAX_ICON_SIZE) / 2);
                int cropWidth = Math.min(MAX_ICON_SIZE, icon.width);
                int cropHeight = Math.min(MAX_ICON_SIZE, icon.height);

                // Create cropped icon
                safeIcon = icon.get(cropX, cropY, cropWidth, cropHeight);
            }

            g.imageMode(PGraphics.CENTER);
            // Draw the icon, ensuring it fits within the icon size with some padding
            float padding = 8;
            float drawableSize = ICON_SIZE - padding * 2;
            g.image(safeIcon, centerX, centerY, drawableSize, drawableSize);
            g.imageMode(PGraphics.CORNER); // Reset image mode to default
        } else {
            // Fallback to placeholder if icon is null
            g.rectMode(PGraphics.CENTER);
            // use accent color tile
            g.fill(accentColor);
            g.noStroke();
            g.rect(centerX, centerY, 40, 40, RADIUS_SMALL);

            // Draw app initial
            var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
            int onSurface = theme != null ? theme.colorOnSurface() : 0xFF111111;
            { int c=onSurface; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF); }
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(TEXT_SIZE_MEDIUM);
            if (app.getName() != null && !app.getName().isEmpty()) {
                String initial = app.getName().substring(0, 1).toUpperCase();
                g.text(initial, centerX, centerY - 2);
            }
            g.rectMode(PGraphics.CORNER); // Reset rect mode to default
        }
    }
    
    /**
     * Draws the navigation area at the bottom for accessing app library.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawNavigationArea(PGraphics g) {
        int navY = 600 - NAV_AREA_HEIGHT;

        // Draw navigation background
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int surface = theme != null ? theme.colorSurface() : 0xFF2A2A2A;
        int onSurface = theme != null ? theme.colorOnSurface() : textColor;
        int alpha = 180; // Semi-transparent alpha for holographic effect
        g.fill((surface>>16)&0xFF, (surface>>8)&0xFF, surface&0xFF, alpha);
        g.noStroke();
        g.rect(0, navY, 400, NAV_AREA_HEIGHT);

        // Top border
        int border = theme != null ? theme.colorBorder() : 0xFF444444;
        g.stroke((border>>16)&0xFF, (border>>8)&0xFF, border&0xFF);
        g.strokeWeight(1);
        g.line(0, navY, 400, navY);

        // Draw app library access hint
        g.fill((onSurface>>16)&0xFF, (onSurface>>8)&0xFF, onSurface&0xFF, 150);
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(TEXT_SIZE_LARGE);
        g.text("App Library", 200, navY + 30);
        
        // Draw edit mode toggle hint if not in edit mode
        if (!isEditing) {
            g.textSize(TEXT_SIZE_TINY);
            g.text("Long press to edit", 200, navY + 50);
        } else {
            g.textSize(TEXT_SIZE_TINY);
            g.text("Tap outside to finish editing", 200, navY + 50);
        }
        
        // Draw swipe indicator for pages if multiple pages exist
        if (homePages.size() > 1) {
            { int c=onSurface; g.stroke((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, 100); }
            g.strokeWeight(1);
            g.noFill();
            
            // Left arrow for previous page
            if (currentPageIndex > 0) {
                g.line(50, navY + 70, 40, navY + 75);
                g.line(50, navY + 70, 40, navY + 65);
            }
            
            // Right arrow for next page
            if (currentPageIndex < homePages.size() - 1) {
                g.line(350, navY + 70, 360, navY + 75);
                g.line(350, navY + 70, 360, navY + 65);
            }
        }
    }
    
    /**
     * Draws page indicator dots for multiple pages.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawPageIndicators(PGraphics g) {
        if (homePages.size() <= 1) {
            return; // Don't show indicators for single page
        }

        int dotY = 600 - NAV_AREA_HEIGHT - 25; // 少し上に移勁E
        int dotSize = 10;
        int activeDotSize = 14;
        int spacing = 20;
        int totalWidth = homePages.size() * spacing - (spacing - dotSize);
        int startX = (400 - totalWidth) / 2;

        // 背景の半透Eエリア
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFFFFFFFF;
        int border = theme != null ? theme.colorBorder() : 0xFF444444;
        int primary = theme != null ? theme.colorPrimary() : accentColor;
        g.fill((border>>16)&0xFF, (border>>8)&0xFF, border&0xFF, 60);
        g.noStroke();
        g.rect(startX - 15, dotY - 10, totalWidth + 30, 20, 10);

        for (int i = 0; i < homePages.size(); i++) {
            int dotX = startX + i * spacing;

            if (i == currentPageIndex) {
                // 現在のペEジ - 大きく明るぁE                g.fill((primary>>16)&0xFF, (primary>>8)&0xFF, primary&0xFF);
                g.noStroke();
                g.ellipse(dotX, dotY, activeDotSize, activeDotSize);

                // 外Eのリング
                g.noFill();
                g.stroke((primary>>16)&0xFF, (primary>>8)&0xFF, primary&0xFF, 150);
                g.strokeWeight(2);
                g.ellipse(dotX, dotY, activeDotSize + 4, activeDotSize + 4);
            } else {
                // 他EペEジ - 小さく薄ぁE                g.fill((onSurface>>16)&0xFF, (onSurface>>8)&0xFF, onSurface&0xFF, 120);
                g.noStroke();
                g.ellipse(dotX, dotY, dotSize, dotSize);
            }
        }
        
        // AppLibraryペEジには特別なアイコン
        for (int i = 0; i < homePages.size(); i++) {
            HomePage page = homePages.get(i);
            if (page.isAppLibraryPage()) {
                int dotX = startX + i * spacing;
                
                // AppLibraryアイコンEEEグリチEEE風EEEE
                g.stroke(i == currentPageIndex ? 255 : 200);
                g.strokeWeight(1);
                g.noFill();
                
                // 小さな3x3グリチEEE
                int gridSize = 6;
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int x = dotX - gridSize + col * (gridSize / 2);
                        int y = dotY - gridSize + row * (gridSize / 2);
                        g.rect(x, y, 1, 1);
                    }
                }
                break;
            }
        }
    }

    /**
     * Draws the clock and weather card.
     * @param g The PGraphics instance for drawing
     */
    private void drawClockAndWeatherCard(PGraphics g) {
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int surface = theme != null ? theme.colorSurface() : 0xFFFFFFFF;
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFF111111;

        int cardX = 20;
        int cardY = 80;
        int cardWidth = 360;
        int cardHeight = 80;

        // Draw card background
        g.fill((surface>>16)&0xFF, (surface>>8)&0xFF, surface&0xFF, 200);
        g.stroke(theme.colorBorder());
        g.rect(cardX, cardY, cardWidth, cardHeight, RADIUS_SMALL);

        // Get time
        String time = "--:--";
        if (kernel != null && kernel.getSystemClock() != null) {
            try {
                time = kernel.getSystemClock().getFormattedTime();
            } catch (Exception e) {
                // ignore
            }
        }

        // Determine weather
        String weather;
        if (currentHumidity > 80) {
            if (currentLightLevel < 10) {
                weather = "Thundering";
            } else {
                weather = "Rainy";
            }
        } else {
            weather = "Clear";
        }

        // Draw time
        g.fill(onSurface);
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(TEXT_SIZE_XL);
        g.text(time, cardX + 20, cardY + 20);

        // Draw weather
        g.textAlign(g.RIGHT, g.TOP);
        g.text(weather, cardX + cardWidth - 20, cardY + 20);
    }

    /**
     * Draws the search card.
     * @param g The PGraphics instance for drawing
     */
    private void drawSearchCard(PGraphics g) {
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int surface = theme != null ? theme.colorSurface() : 0xFFFFFFFF;
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFF111111;

        int cardX = 20;
        int cardY = 170; // Below Clock & Weather card
        int cardWidth = 360;
        int cardHeight = 50;

        // Draw card background
        g.fill((surface>>16)&0xFF, (surface>>8)&0xFF, surface&0xFF, 200);
        g.stroke(theme.colorBorder());
        g.rect(cardX, cardY, cardWidth, cardHeight, RADIUS_SMALL);

        // Draw placeholder text
        g.fill(onSurface);
        g.textAlign(g.LEFT, g.CENTER);
        g.textSize(TEXT_SIZE_LARGE);
        g.text("Search...", cardX + 20, cardY + cardHeight / 2);
    }
    
    /**
     * Gets the shortcut at the specified coordinates on a specific page.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @param page The page to search
     * @return The Shortcut at that position, or null if none
     */
    private Shortcut getShortcutAtPosition(int x, int y, HomePage page) {
        if (page == null || page.isAppLibraryPage()) {
            return null;
        }
        
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        
        for (Shortcut shortcut : page.getShortcuts()) {
            int iconX = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
            int iconY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20);
            
            if (x >= iconX && x <= iconX + ICON_SIZE && 
                y >= iconY && y <= iconY + ICON_SIZE) {
                return shortcut;
            }
        }
        
        return null;
    }
    
    /**
     * Gets the shortcut at the specified screen coordinates (legacy method).
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The Shortcut at that position, or null if none
     */
    private Shortcut getShortcutAtPosition(int x, int y) {
        HomePage currentPage = getCurrentPage();
        return getShortcutAtPosition(x, y, currentPage);
    }
    
    /**
     * Gets the application at the specified screen coordinates.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The IApplication at that position, or null if none
     */
    private IApplication getAppAtPosition(int x, int y) {
        Shortcut shortcut = getShortcutAtPosition(x, y);
        return shortcut != null ? shortcut.getApplication() : null;
    }
    
    /**
     * マウス座標を座標変換後E座標に変換し、E刁EEEペEジでショートカチEEEを検索する、
     * 
     * @param mouseX マウスX座標（絶対座標）
     * @param mouseY マウスY座標（絶対座標）
     * @return 該当位置のショートカチEEE、またE null
     */
    private Shortcut getShortcutAtPositionWithTransform(int mouseX, int mouseY) {
        // 現在の座標変換オフセチEEEを計箁E
        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        
        // マウス座標を変換後E座標系に調整
        float transformedX = mouseX - totalOffset;
        
        // どのペEジ篁EEEにぁEEEかを判定
        int pageIndex = (int) Math.floor(transformedX / 400);
        
        // ペEジ篁EEE冁EEEのローカル座標を計箁E
        int localX = (int) (transformedX - pageIndex * 400);
        
        // ペEジインチEEEクスが有効篁EEE冁EEEチェチEEE
        if (pageIndex >= 0 && pageIndex < homePages.size()) {
            HomePage targetPage = homePages.get(pageIndex);
            return getShortcutAtPosition(localX, mouseY, targetPage);
        }
        
        return null;
    }
    
    /**
     * Opens the app library by switching to the App Library page within the home screen.
     */
    private void openAppLibrary() {
        System.out.println("HomeScreen: Navigating to integrated App Library page");
        
        // AppLibraryペEジEEE最後EペEジEEEに刁EEE替ぁE
        if (!homePages.isEmpty()) {
            int appLibraryPageIndex = homePages.size() - 1;
            if (appLibraryPageIndex != currentPageIndex && !isAnimating) {
                startPageTransition(appLibraryPageIndex);
            }
        }
    }
    
    /**
     * Launches the specified application.
     * 
     * @param app The application to launch
     */
    private void launchApplication(IApplication app) {
        System.out.println("HomeScreen: Launching app: " + app.getName());

        if (kernel != null && kernel.getScreenManager() != null && kernel.getServiceManager() != null) {
            try {
                // ServiceManager経由でアプリを起動（既存インスタンスをE利用またE新規作EEEEE
                Screen appScreen = kernel.getServiceManager().launchApp(app.getApplicationId());
                if (appScreen != null) {
                    kernel.getScreenManager().pushScreen(appScreen);
                } else {
                    System.err.println("HomeScreen: ServiceManager returned null screen for " + app.getName());
                }
            } catch (Exception e) {
                System.err.println("HomeScreen: Failed to launch app " + app.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * アニメーション付きでアプリケーションを起動すめE
     */
    private void launchApplicationWithAnimation(IApplication app, float iconX, float iconY, float iconSize) {
        System.out.println("HomeScreen: Launching app with animation: " + app.getName());
        System.out.println("HomeScreen: Icon position: (" + iconX + ", " + iconY + "), size: " + iconSize);

        if (kernel != null && kernel.getScreenManager() != null && kernel.getServiceManager() != null) {
            try {
                // ServiceManager経由でアプリを起動（既存インスタンスをE利用またE新規作EEEEE
                Screen appScreen = kernel.getServiceManager().launchApp(app.getApplicationId());
                if (appScreen == null) {
                    System.err.println("HomeScreen: ServiceManager returned null screen for " + app.getName());
                    return;
                }
                System.out.println("HomeScreen: Got app screen: " + appScreen.getScreenTitle());
                
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

                System.out.println("HomeScreen: Got app icon: " + (appIcon != null ? appIcon.width + "x" + appIcon.height : "null"));
                
                // Launch with animation
                if (appIcon != null) {
                    System.out.println("HomeScreen: Calling pushScreenWithAnimation...");
                    kernel.getScreenManager().pushScreenWithAnimation(appScreen, iconX, iconY, iconSize, appIcon);
                } else {
                    System.out.println("HomeScreen: No icon available, using normal launch");
                    // Fallback to normal launch
                    kernel.getScreenManager().pushScreen(appScreen);
                }
            } catch (Exception e) {
                System.err.println("HomeScreen: Failed to launch app with animation " + app.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Refreshes the home screen pages.
     */
    public void refreshApps() {
        System.out.println("HomeScreen: refreshApps() called - reinitializing pages...");
        initializeHomePages();
        currentPageIndex = 0;
        isEditing = false;
        System.out.println("HomeScreen: Refreshed home screen pages - total pages: " + homePages.size());
    }
    
    /**
     * Toggles edit mode.
     */
    public void toggleEditMode() {
        isEditing = !isEditing;
        System.out.println("HomeScreen: Edit mode " + (isEditing ? "enabled" : "disabled"));

        if (isEditing) {
            // 編雁EEEード開始時に空のペEジを最後に追加
            addEmptyPageIfNeeded();
        } else {
            // 編雁EEEード終了EEにはドラチEEE状態をリセチEEE
            resetDragState();
            System.out.println("HomeScreen: Reset drag state on edit mode exit");

            // 編雁EEEード終了EEに空のペEジを削除
            removeEmptyPagesAtEnd();
        }
    }
    
    /**
     * Adds a new page to the home screen.
     */
    public void addNewPage() {
        HomePage newPage = new HomePage();
        homePages.add(newPage);
        System.out.println("HomeScreen: Added new page, total pages: " + homePages.size());
    }

    /**
     * Adds an empty page at the end if needed (for edit mode).
     */
    private void addEmptyPageIfNeeded() {
        System.out.println("HomeScreen: addEmptyPageIfNeeded() called");
        System.out.println("HomeScreen: Total pages: " + homePages.size());

        // 最後EペEジが空でなぁEEE合、またE最後がAppLibraryペEジの場合E空ペEジを追加
        if (homePages.isEmpty()) {
            System.out.println("HomeScreen: No pages exist, adding first page");
            addNewPage();
            return;
        }

        // AppLibraryペEジの前に空ペEジを挿入するロジチEEEに変更
        int insertIndex = homePages.size();
        HomePage lastPage = homePages.get(homePages.size() - 1);

        System.out.println("HomeScreen: Last page type: " + (lastPage.isAppLibraryPage() ? "APP_LIBRARY" : "NORMAL"));
        System.out.println("HomeScreen: Last page shortcuts count: " + lastPage.getShortcuts().size());

        // AppLibraryペEジがある場合E、その前に挿入
        if (lastPage.isAppLibraryPage()) {
            insertIndex = homePages.size() - 1; // AppLibraryペEジの前に挿入

            // AppLibraryペEジの前EペEジが空でなぁEEE合Eみ空ペEジを追加
            if (insertIndex > 0) {
                HomePage secondToLastPage = homePages.get(insertIndex - 1);
                if (!secondToLastPage.getShortcuts().isEmpty()) {
                    HomePage newPage = new HomePage();
                    homePages.add(insertIndex, newPage);
                    System.out.println("HomeScreen: Added empty page before AppLibrary at index " + insertIndex);
                } else {
                    System.out.println("HomeScreen: Page before AppLibrary is already empty, no need to add");
                }
            } else {
                // AppLibraryペEジが最初EペEジの場合（通常はなぁEEEE
                HomePage newPage = new HomePage();
                homePages.add(0, newPage);
                System.out.println("HomeScreen: Added empty page before AppLibrary at index 0");
            }
        } else {
            // 最後EペEジが通常ペEジで空でなぁEEE合、空ペEジを追加
            if (!lastPage.getShortcuts().isEmpty()) {
                addNewPage();
                System.out.println("HomeScreen: Added empty page at end");
            } else {
                System.out.println("HomeScreen: Last page is already empty, no need to add");
            }
        }
    }

    /**
     * Removes empty pages at the end (after edit mode ends).
     */
    private void removeEmptyPagesAtEnd() {
        System.out.println("HomeScreen: removeEmptyPagesAtEnd() called");
        System.out.println("HomeScreen: Total pages before cleanup: " + homePages.size());

        // AppLibraryペEジを除ぁEEE通常ペEジの中で、後ろから空のペEジを削除
        boolean removedAny = false;
        for (int i = homePages.size() - 1; i >= 0; i--) {
            HomePage page = homePages.get(i);

            // AppLibraryペEジはスキチEEE
            if (page.isAppLibraryPage()) {
                System.out.println("HomeScreen: Skipping AppLibrary page at index " + i);
                continue;
            }

            // 空のペEジを削除
            if (page.getShortcuts().isEmpty()) {
                homePages.remove(i);
                removedAny = true;
                System.out.println("HomeScreen: Removed empty page at index " + i);

                // 現在のペEジが削除された場合E調整
                if (currentPageIndex >= homePages.size()) {
                    currentPageIndex = Math.max(0, homePages.size() - 1);
                    System.out.println("HomeScreen: Adjusted current page index to " + currentPageIndex);
                }

                // 現在のペEジインチEEEクスが削除されたEージ以降E場合E調整
                if (currentPageIndex > i) {
                    currentPageIndex--;
                    System.out.println("HomeScreen: Decremented current page index to " + currentPageIndex);
                }
            } else {
                // 空でなぁEEEージが見つかったら、以降E削除は停止
                // EEEただしAppLibraryペEジは除外）
                System.out.println("HomeScreen: Found non-empty page at index " + i + ", stopping cleanup");
                break;
            }
        }

        if (!removedAny) {
            System.out.println("HomeScreen: No empty pages to remove");
        }

        System.out.println("HomeScreen: Total pages after cleanup: " + homePages.size());
    }
    
    /**
     * Gets the current page.
     * 
     * @return The current HomePage, or null if none
     */
    public HomePage getCurrentPage() {
        if (homePages.isEmpty() || currentPageIndex >= homePages.size()) {
            return null;
        }
        return homePages.get(currentPageIndex);
    }
    
    // ===========================================
    // GestureListener Implementation
    // ===========================================
    
    @Override
    public boolean onGesture(GestureEvent event) {
        // パフォーマンス改喁E DRAG_MOVEイベントE非常に頻繁なのでログを抑制
        if (event.getType() != GestureType.DRAG_MOVE) {
            System.out.println("HomeScreen: Received gesture: " + event);
        }
        
        switch (event.getType()) {
            case TAP:
                return handleTap(event.getCurrentX(), event.getCurrentY());
                
            case LONG_PRESS:
                return handleLongPress(event.getCurrentX(), event.getCurrentY());
                
            case DRAG_START:
                return handleDragStart(event);
                
            case DRAG_MOVE:
                return handleDragMove(event);
                
            case DRAG_END:
                return handleDragEnd(event);
                
            case SWIPE_LEFT:
                return handleSwipeLeft();
                
            case SWIPE_RIGHT:
                return handleSwipeRight();
                
            case SWIPE_UP:
                return handleSwipeUp(event);
                
            default:
                return false; // 処理EEなぁEEEェスチャー
        }
    }
    
    @Override
    public boolean isInBounds(int x, int y) {
        // HomeScreenが現在のスクリーンの場合Eみ処理
        return kernel != null && 
               kernel.getScreenManager() != null && 
               kernel.getScreenManager().getCurrentScreen() == this;
    }
    
    @Override
    public int getPriority() {
        return 50; // 中優先度
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
            currentHumidity = event.values[0];
        } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
            currentLightLevel = event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used for now
    }
    
    /**
     * タチEEEジェスチャーをE理EEる、
     * 
     * @param x X座樁E
     * @param y Y座樁E
     * @return 処理EEた場合rue
     */
    private boolean handleTap(int x, int y) {
        System.out.println("HomeScreen: Handling tap at (" + x + ", " + y + ")");
        
        // マウス座標を変換
        int[] coords = transformMouseCoordinates(x, y);
        int pageX = coords[0];
        int pageY = coords[1];
        int targetPageIndex = coords[2];
        
        // 対象ペEジが篁EEE外E場合E無要E
        if (targetPageIndex < 0 || targetPageIndex >= homePages.size()) {
            return false;
        }
        
        HomePage targetPage = homePages.get(targetPageIndex);
        System.out.println("HomeScreen: Transformed tap to page " + targetPageIndex + " at (" + pageX + ", " + pageY + ")");
        
        // Check if tap is on the search card
        if (x > 20 && x < 20 + 360 && y > 170 && y < 170 + 50) {
            return handleSearchCardTap();
        }

        // AppLibraryペEジの場合E特別処理
        if (targetPage.isAppLibraryPage()) {
            return handleAppLibraryTap(pageX, pageY, targetPage);
        }
        
        // ナビゲーションエリアEEE下部EEEEEタチEEEでApp Libraryを開く（コントロールセンター領域を除く）
        if (pageY > (600 - NAV_AREA_HEIGHT) && pageY < 540) {
            openAppLibrary();
            return true;
        }
        
        // 対象ペEジが現在のペEジでなぁEEE合EペEジ刁EEE替ぁE
        if (targetPageIndex != currentPageIndex && !isAnimating) {
            startPageTransition(targetPageIndex);
            return true;
        }
        
        // ショートカチEEEのタチEEE処理EE現在のペEジのみEEEE
        if (targetPageIndex == currentPageIndex) {
            Shortcut tappedShortcut = getShortcutAtPosition(pageX, pageY, targetPage);
            if (tappedShortcut != null) {
                if (isEditing) {
                    // 編雁EEEードでは削除ボタンかアイコンかをチェチEEE
                    if (isClickingDeleteButton(pageX, pageY, tappedShortcut)) {
                        removeShortcut(tappedShortcut);
                    }
                } else {
                    // 通常モードではアプリ起動（アニメーション付きEEEE
                    // ショートカチEEEの画面上での位置を計箁E
                    HomePage currentPage = homePages.get(targetPageIndex);
                    int startX = (400 - (GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING)) / 2;
                    int startY = 80;
                    float iconX = startX + tappedShortcut.getGridX() * (ICON_SIZE + ICON_SPACING) + ICON_SIZE / 2;
                    float iconY = startY + tappedShortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20) + ICON_SIZE / 2;
                    launchApplicationWithAnimation(tappedShortcut.getApplication(), iconX, iconY, ICON_SIZE);
                }
                return true;
            }
        }
        
        // 編雁EEEード中に空のスペEスをタチEEEした場合E編雁EEEードを終了
        if (isEditing) {
            System.out.println("HomeScreen: Tapped empty space in edit mode - exiting edit mode");
            toggleEditMode();
            return true;
        }
        
        return false;
    }

    private boolean handleSearchCardTap() {
        System.out.println("HomeScreen: Search card tapped");
        if (kernel != null && kernel.getAppLoader() != null) {
            IApplication browserApp = kernel.getAppLoader().findApplicationById("jp.moyashi.phoneos.core.apps.chromiumbrowser");
            if (browserApp != null) {
                kernel.getScreenManager().pushScreen(browserApp.getEntryScreen(kernel));
                return true;
            }
        }
        return false;
    }
    
    /**
     * AppLibraryペEジでのタチEEEをE理EEる、
     * 
     * @param x X座樁E
     * @param y Y座樁E
     * @param appLibraryPage AppLibraryペEジ
     * @return 処理EEた場合rue
     */
    private boolean handleAppLibraryTap(int x, int y, HomePage appLibraryPage) {
        // ナビゲーションエリアEEE下部EEEEEタチEEEは無要E
        if (y > (600 - NAV_AREA_HEIGHT)) {
            return false;
        }
        
        // アプリリストE篁EEE冁EEEチェチEEE
        int startY = 110;
        int listHeight = 600 - startY - NAV_AREA_HEIGHT - 20;
        int itemHeight = 70;
        
        if (y >= startY && y <= startY + listHeight) {
            // タチEEEされたアプリケーションを取征E
            IApplication tappedApp = appLibraryPage.getApplicationAtPosition(x, y, startY, itemHeight);
            if (tappedApp != null) {
                System.out.println("HomeScreen: AppLibraryでアプリをタチEEE: " + tappedApp.getName());
                // アイコン位置を計算）ppLibraryアイチEEE用EEEE
                float iconX = 20 + 32; // ITEM_PADDING + ICON_SIZE/2
                float iconY = startY + ((y - startY) / itemHeight) * itemHeight + itemHeight / 2;
                launchApplicationWithAnimation(tappedApp, iconX, iconY, 32); // AppLibraryのアイコンサイズは32
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 長押しジェスチャーをE理EEる、
     * 
     * @param x X座樁E
     * @param y Y座樁E
     * @return 処理EEた場合rue
     */
    private boolean handleLongPress(int x, int y) {
        System.out.println("HomeScreen: Handling long press at (" + x + ", " + y + ")");
        
        // マウス座標を変換
        int[] coords = transformMouseCoordinates(x, y);
        int pageX = coords[0];
        int pageY = coords[1];
        int targetPageIndex = coords[2];
        
        // 対象ペEジが篁EEE外E場合E無要E
        if (targetPageIndex < 0 || targetPageIndex >= homePages.size()) {
            return false;
        }
        
        HomePage targetPage = homePages.get(targetPageIndex);
        
        // AppLibraryペEジの場合E特別処理
        if (targetPage.isAppLibraryPage()) {
            return handleAppLibraryLongPress(pageX, pageY, targetPage);
        }
        
        // 現在のペEジでの長押しEみ編雁EEEードEり替ぁE
        if (targetPageIndex == currentPageIndex && !isEditing) {
            toggleEditMode();
            return true;
        }
        
        return false;
    }
    
    /**
     * AppLibraryペEジでの長押しを処理EEる、
     * 
     * @param x X座樁E
     * @param y Y座樁E
     * @param appLibraryPage AppLibraryペEジ
     * @return 処理EEた場合rue
     */
    private boolean handleAppLibraryLongPress(int x, int y, HomePage appLibraryPage) {
        // ナビゲーションエリアEEE下部EEEEE長押しE無要E
        if (y > (600 - NAV_AREA_HEIGHT)) {
            return false;
        }
        
        // アプリリストE篁EEE冁EEEチェチEEE
        int startY = 110;
        int listHeight = 600 - startY - NAV_AREA_HEIGHT - 20;
        int itemHeight = 70;
        
        if (y >= startY && y <= startY + listHeight) {
            // 長押しされたアプリケーションを取征E
            IApplication longPressedApp = appLibraryPage.getApplicationAtPosition(x, y, startY, itemHeight);
            if (longPressedApp != null) {
                System.out.println("HomeScreen: AppLibraryで長押ぁE " + longPressedApp.getName());
                showAddToHomePopup(longPressedApp, x, y);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 「Eーム画面に追加」EチEEEアチEEEを表示する、
     * 
     * @param app 対象のアプリケーション
     * @param x ポップアチEEE表示位置X
     * @param y ポップアチEEE表示位置Y
     */
    private void showAddToHomePopup(IApplication app, int x, int y) {
        if (kernel != null && kernel.getPopupManager() != null) {
            System.out.println("HomeScreen: ✁EポップアチEEEマネージャーが利用可能");
            
            // 簡単なコンチEEEストメニューポップアチEEEを作E
            String message = "「" + app.getName() + "」をホーム画面に追加しますか？";
            
            // ポップアチEEEマネージャーに実裁EEEれたポップアチEEEシスチEEEを使用
            // EEE実際の実裁EEEPopupManagerの仕様に依存）
            System.out.println("HomeScreen: 🎯 「ホーム画面に追加」ポップアップ表示予定");
            System.out.println("    • アプリ合 " + app.getName());
            System.out.println("    • 位置: (" + x + ", " + y + ")");
            System.out.println("    • メチEEEージ: " + message);
            
            // PopupManagerの実裁EEE応じてここでポップアチEEEを表示
            // 現在はログ出力EみEEE実際のポップアチEEE実裁EEE別途忁EEEEEEE
        } else {
            System.err.println("HomeScreen: ❁EPopupManagerが利用できません");
        }
    }
    
    /**
     * 左スワイプジェスチャーをE理EEる、
     * 
     * @return 処理EEた場合rue
     */
    private boolean handleSwipeLeft() {
        // 編雁EEEード中でもEージスワイプを有効化（ドラチEEE中は無効EEEE
        if (isEditing && isDragging) {
            System.out.println("HomeScreen: Left swipe ignored - dragging shortcut in edit mode");
            return false;
        }
        
        System.out.println("HomeScreen: Left swipe detected - next page");
        if (currentPageIndex < homePages.size() - 1) {
            startPageTransition(currentPageIndex + 1);
            return true;
        }
        return false;
    }
    
    /**
     * 右スワイプジェスチャーをE理EEる、
     * 
     * @return 処理EEた場合rue
     */
    private boolean handleSwipeRight() {
        // 編雁EEEード中でもEージスワイプを有効化（ドラチEEE中は無効EEEE
        if (isEditing && isDragging) {
            System.out.println("HomeScreen: Right swipe ignored - dragging shortcut in edit mode");
            return false;
        }
        
        System.out.println("HomeScreen: Right swipe detected - previous page");
        if (currentPageIndex > 0) {
            startPageTransition(currentPageIndex - 1);
            return true;
        }
        return false;
    }
    
    /**
     * 上スワイプジェスチャーをE理EEる、
     * 
     * @return 処理EEた場合rue
     */
    private boolean handleSwipeUp(GestureEvent event) {
        // 画面下部EEE高さの90%以上）からEスワイプアチEEEはKernelのコントロールセンター用に予紁E
        if (event.getStartY() >= 600 * 0.9f) {
            System.out.println("HomeScreen: Bottom swipe up detected - letting Kernel handle control center");
            return false; // Kernelに処理EE委譲
        }
        
        // 画面の中央部からのスワイプアチEEEでApp Libraryを開ぁE
        System.out.println("HomeScreen: Up swipe detected - opening integrated App Library");
        openAppLibrary();
        return true;
    }
    
    /**
     * ドラチEEE開始ジェスチャーをE理EEる、
     * 
     * @param event ジェスチャーイベント
     * @return 処理EEた場合rue
     */
    private boolean handleDragStart(GestureEvent event) {
        HomePage currentPage = getCurrentPage();
        if (currentPage != null && currentPage.isAppLibraryPage()) {
            if (shouldHandleAppLibraryScroll(event)) {
                isAppLibraryScrolling = true;
                return handleAppLibraryScrollStart(event);
            } else {
                isAppLibraryScrolling = false;
            }
        } else {
            isAppLibraryScrolling = false;
        }
        
        // 編雁EEEードでのアイコンドラチEEEを優先的に処理
        if (isEditing) {
            Shortcut clickedShortcut = getShortcutAtPositionWithTransform(event.getStartX(), event.getStartY());
            if (clickedShortcut != null) {
                // アイコンドラチEEEを開始
                startDragging(clickedShortcut, event.getStartX(), event.getStartY());
                System.out.println("HomeScreen: Started icon drag for " + clickedShortcut.getDisplayName());
                return true; // アイコンドラチEEEが優先される
            }
        }
        
        return false; // ペEジスワイプ用のドラチEEE処理EE handleDragMove で実裁E
    }
    
    /**
     * ドラチEEE移動ジェスチャーをE理EEる、
     * 
     * @param event ジェスチャーイベント
     * @return 処理EEた場合rue
     */
    private boolean handleDragMove(GestureEvent event) {
        HomePage currentPage = getCurrentPage();
        if (currentPage != null && currentPage.isAppLibraryPage()) {
            if (isAppLibraryScrolling || shouldHandleAppLibraryScroll(event)) {
                isAppLibraryScrolling = true;
                return handleAppLibraryScroll(event);
            }
        } else {
            isAppLibraryScrolling = false;
        }
        
        // アイコンドラチEEEが進行中の場合E、それを優允E
        if (isDragging && draggedShortcut != null) {
            int dragX = event.getCurrentX() - dragOffsetX;
            int dragY = event.getCurrentY() - dragOffsetY;

            // ドラチEEE座標E墁EEEチェチEEEと調整
            dragX = constrainDragPosition(dragX, dragY)[0];
            dragY = constrainDragPosition(dragX, dragY)[1];

            draggedShortcut.setDragPosition(dragX, dragY);
            // パフォーマンス改喁E 過剰なコンソール出力を抑制
            // System.out.println("HomeScreen: Updating icon drag position to (" + dragX + ", " + dragY + ")");

            // 画面端での自動Eージスライドを実裁E
            handleEdgeAutoSlide(event.getCurrentX(), event.getCurrentY());

            return true; // アイコンドラチEEEが優允E
        }
        
        // 編雁EEEードでもEージスワイプを有効化（ショートカチEEEドラチEEE中は無効EEEE
        if (isEditing && isDragging) {
            return false; // ショートカチEEEドラチEEE中はペEジドラチEEEを無効
        }

        // 通常モードおよE編雁EEEード（ショートカチEEEドラチEEE中以外）でペEジ刁EEE替えドラチEEEをE理
        return handlePageDrag(event);
    }
    
    /**
     * ペEジドラチEEEをE理EEる（リアルタイムペEジ移動）、
     * 
     * @param event ジェスチャーイベント
     * @return 処理EEた場合rue
     */
    private boolean handlePageDrag(GestureEvent event) {
        if (isAnimating) {
            return false; // アニメーション中はドラチEEEを無要E
        }
        
        int deltaX = event.getCurrentX() - event.getStartX();
        
        // 水平ドラチEEEのみをEージ移動として扱ぁE
        if (Math.abs(deltaX) > 10) { // 10px以上EドラチEEEで反忁E
            // ペEジオフセチEEEを計算（画面幁EEE篁EEE冁EEE制限）
            pageTransitionOffset = Math.max(-400, Math.min(400, deltaX));
            
            // 端ペEジでの制陁E
            if (currentPageIndex == 0 && pageTransitionOffset > 0) {
                pageTransitionOffset *= 0.3f; // バウンス効极E
            } else if (currentPageIndex == homePages.size() - 1 && pageTransitionOffset < 0) {
                pageTransitionOffset *= 0.3f; // バウンス効极E
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * ドラチEEE終了EEェスチャーをE理EEる、
     * 
     * @param event ジェスチャーイベント
     * @return 処理EEた場合rue
     */
    private boolean handleDragEnd(GestureEvent event) {
        HomePage currentPage = getCurrentPage();
        if (currentPage != null && currentPage.isAppLibraryPage()) {
            if (isAppLibraryScrolling) {
                isAppLibraryScrolling = false;
                return handleAppLibraryScrollEnd(event);
            }
        } else {
            isAppLibraryScrolling = false;
        }
        
        // アイコンドラチEEEの終了EE理
        if (isDragging && draggedShortcut != null) {
            System.out.println("HomeScreen: Ending icon drag");
            handleShortcutDrop(event.getCurrentX(), event.getCurrentY());

            // 画面端スライド状態をリセチEEE
            resetEdgeSlideState();

            return true;
        }
        
        // 編雁EEEードでもEージスワイプを有効化（ショートカチEEEドラチEEE中は無効EEEE
        if (isEditing && isDragging) {
            return false; // ショートカチEEEドラチEEE中はペEジドラチEEE終了EE無効
        }

        // 通常モードおよE編雁EEEード（ショートカチEEEドラチEEE中以外）でペEジドラチEEE終了EE処理
        return handlePageDragEnd(event);
    }
    
    /**
     * ペEジドラチEEE終了EE処理EEる、
     * 
     * @param event ジェスチャーイベント
     * @return 処理EEた場合rue
     */
    private boolean handlePageDragEnd(GestureEvent event) {
        if (Math.abs(pageTransitionOffset) < 50) {
            // ドラチEEE距離が短ぁEEE合E允EEEペEジに戻めE
            startReturnToCurrentPage();
            return true;
        }
        
        // ドラチEEE距離が十刁EEE場合EペEジ刁EEE替ぁE
        if (pageTransitionOffset > 50 && currentPageIndex > 0) {
            // 右にドラチEEE - 前EペEジ
            startPageTransition(currentPageIndex - 1);
            return true;
        } else if (pageTransitionOffset < -50 && currentPageIndex < homePages.size() - 1) {
            // 左にドラチEEE - 次のペEジ
            startPageTransition(currentPageIndex + 1);
            return true;
        } else {
            // 端ペEジまたE条件を満たさなぁEEE合E允EEE戻めE
            startReturnToCurrentPage();
            return true;
        }
    }
    
    /**
     * 現在のペEジに戻るアニメーションを開始する、
     */
    private void startReturnToCurrentPage() {
        targetPageIndex = currentPageIndex;
        isAnimating = true;
        animationStartTime = System.currentTimeMillis();
        animationProgress = 0.0f;
        startOffset = pageTransitionOffset; // 現在のオフセチEEEを保孁E
        animationBasePageIndex = currentPageIndex; // 座標計算E基準Eージを固定
        System.out.println("🎬 Starting return animation to current page " + currentPageIndex + ", startOffset=" + startOffset + ", basePageIndex=" + animationBasePageIndex);
    }
    
    /**
     * AppLibraryペEジでのスクロール開始を処理EEる、
     * 
     * @param event ジェスチャーイベント
     * @return 処理EEた場合rue
     */
    private boolean handleAppLibraryScrollStart(GestureEvent event) {
        System.out.println("HomeScreen: AppLibrary scroll started");
        return true; // スクロール開始を受け入れる
    }
    
    /**
     * AppLibraryペEジでのスクロールをE理EEる、
     * 
     * @param event ジェスチャーイベント
     * @return 処理EEた場合rue
     */
    private boolean handleAppLibraryScroll(GestureEvent event) {
        HomePage currentPage = getCurrentPage();
        if (currentPage == null || !currentPage.isAppLibraryPage()) return false;
        
        // 垂直ドラチEEEのみをスクロールとして扱ぁE
        int deltaY = event.getCurrentY() - event.getStartY();
        
        // 現在のスクロールオフセチEEEを調整
        int currentScrollOffset = currentPage.getScrollOffset();
        int newScrollOffset = currentScrollOffset - deltaY; // 下方向ドラチEEEで上方向スクロール
        
        // スクロール篁EEEを制陁E
        int startY = APP_LIBRARY_LIST_START_Y;
        int listHeight = SCREEN_HEIGHT - startY - NAV_AREA_HEIGHT - APP_LIBRARY_BOTTOM_PADDING;
        int itemHeight = 70;
        List<IApplication> apps = currentPage.getAllApplications();
        int maxScrollOffset = Math.max(0, apps.size() * itemHeight - listHeight);
        
        newScrollOffset = Math.max(0, Math.min(maxScrollOffset, newScrollOffset));
        currentPage.setScrollOffset(newScrollOffset);
        
        System.out.println("HomeScreen: AppLibrary scrolled to offset " + newScrollOffset);
        return true;
    }
    
    /**
     * AppLibraryペEジでのスクロール終了EE処理EEる、
     *
     * @param event ジェスチャーイベント
     * @return 処理EEた場合rue
     */
    private boolean handleAppLibraryScrollEnd(GestureEvent event) {
        System.out.println("HomeScreen: AppLibrary scroll ended");
        isAppLibraryScrolling = false;
        return true;
    }

    private boolean shouldHandleAppLibraryScroll(GestureEvent event) {
        if (homePages == null || homePages.isEmpty()) {
            return false;
        }

        int[] coords = transformMouseCoordinates(event.getStartX(), event.getStartY());
        int pageIndex = Math.max(0, Math.min(homePages.size() - 1, coords[2]));
        HomePage targetPage = homePages.get(pageIndex);
        if (targetPage == null || !targetPage.isAppLibraryPage()) {
            return false;
        }

        return isWithinAppLibraryScrollZone(coords[1]);
    }

    private boolean isWithinAppLibraryScrollZone(int pageY) {
        int listStart = APP_LIBRARY_LIST_START_Y;
        int listEnd = SCREEN_HEIGHT - NAV_AREA_HEIGHT - APP_LIBRARY_BOTTOM_PADDING;
        return pageY >= listStart && pageY <= listEnd;
    }

    // Edge auto-slide functionality variables
    private long edgeSlideTimer = 0;
    private boolean isEdgeSliding = false;
    private static final int EDGE_SLIDE_ZONE = 30; // ピクセル数での端検Eゾーン
    private static final long EDGE_SLIDE_DELAY = 500; // ミリ秒での自動スライド遅延
    private static final int SCREEN_WIDTH = 400; // 画面幁E(HomeScreenの標準幁E
    private static final int SCREEN_HEIGHT = 600;

    /**
     * ドラチEEE中のショートカチEEEが画面端にある場合、動的にペEジスライドを実行する、
     *
     * @param currentX 現在のマウス/タチEEEのX座樁E
     * @param currentY 現在のマウス/タチEEEのY座樁E
     */
    private void handleEdgeAutoSlide(int currentX, int currentY) {
        if (isAnimating) {
            return; // すでにアニメーション中の場合E何もしなぁE
        }

        // 最後EドラチEEE座標を記録EEE継続的なチェチEEE用EEEE
        lastDragX = currentX;
        lastDragY = currentY;

        long currentTime = System.currentTimeMillis();
        boolean inLeftEdge = currentX < EDGE_SLIDE_ZONE;
        boolean inRightEdge = currentX > (SCREEN_WIDTH - EDGE_SLIDE_ZONE);

        // 画面端に入ったかチェチEEE
        if (inLeftEdge || inRightEdge) {
            if (!isEdgeSliding) {
                // 初回の端検E
                isEdgeSliding = true;
                edgeSlideTimer = currentTime;
                System.out.println("HomeScreen: [Move] Edge slide zone entered at X=" + currentX +
                                 (inLeftEdge ? " (LEFT)" : " (RIGHT)") + " - Timer started");
            } else {
                // 既に端にぁEEE場合E経過時間を表示
                long elapsed = currentTime - edgeSlideTimer;
                System.out.println("HomeScreen: [Move] Still in edge zone at X=" + currentX +
                                 " - Elapsed: " + elapsed + "ms / " + EDGE_SLIDE_DELAY + "ms");

                if (elapsed >= EDGE_SLIDE_DELAY) {
                    // 十EEな時間が経過したので自動スライドを実衁E
                    if (inLeftEdge && currentPageIndex > 0) {
                        // 左端なので前EペEジに移勁E
                        System.out.println("HomeScreen: [Move] Auto-sliding to previous page (LEFT edge)");
                        slideToPage(currentPageIndex - 1, true);
                        resetEdgeSlideState();
                    } else if (inRightEdge && currentPageIndex < homePages.size() - 1) {
                        // 右端なので次のペEジに移勁E
                        System.out.println("HomeScreen: [Move] Auto-sliding to next page (RIGHT edge)");
                        slideToPage(currentPageIndex + 1, true);
                        resetEdgeSlideState();
                    } else {
                        // 端ペEジの場合E何もしなぁE
                        System.out.println("HomeScreen: [Move] Already at edge page, no auto-slide");
                        resetEdgeSlideState();
                    }
                }
            }
        } else {
            // 画面端を離れたのでリセチEEE
            if (isEdgeSliding) {
                System.out.println("HomeScreen: [Move] Left edge slide zone at X=" + currentX + " - Timer reset");
                resetEdgeSlideState();
            }
        }
    }

    /**
     * 画面端スライドE状態をリセチEEEする、
     */
    private void resetEdgeSlideState() {
        isEdgeSliding = false;
        edgeSlideTimer = 0;
    }

    // 最後に記録したマウス/タチEEE座標（継続的なエチEEEチェチEEE用EEEE
    private int lastDragX = 0;
    private int lastDragY = 0;

    // 遁EEEドロチEEE処理EEの変数
    private boolean hasPendingDrop = false;
    private int pendingDropX = 0;
    private int pendingDropY = 0;
    private Shortcut pendingDropShortcut = null;

    /**
     * 描画ループ中にエチEEE自動スライドEタイマEを継続的にチェチEEEする、
     * ドラチEEE中で画面端に滞在してぁEEE場合、時間経過で自動スライドを実行する、
     */
    private void updateEdgeAutoSlideTimer() {
        // ドラチEEE中かつエチEEEスライド中の場合EみチェチEEE
        if (!isDragging || !isEdgeSliding || draggedShortcut == null || isAnimating) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        boolean inLeftEdge = lastDragX < EDGE_SLIDE_ZONE;
        boolean inRightEdge = lastDragX > (SCREEN_WIDTH - EDGE_SLIDE_ZONE);

        // 画面端に滞在してぁEEE場合Eみ継続チェチEEE
        if ((inLeftEdge || inRightEdge) && currentTime - edgeSlideTimer >= EDGE_SLIDE_DELAY) {
            System.out.println("HomeScreen: [Timer] Edge auto-slide triggered at X=" + lastDragX +
                             " after " + (currentTime - edgeSlideTimer) + "ms");

            if (inLeftEdge && currentPageIndex > 0) {
                // 左端なので前EペEジに移勁E
                System.out.println("HomeScreen: [Timer] Auto-sliding to previous page (LEFT edge)");
                slideToPage(currentPageIndex - 1, true);
                resetEdgeSlideState();
            } else if (inRightEdge && currentPageIndex < homePages.size() - 1) {
                // 右端なので次のペEジに移勁E
                System.out.println("HomeScreen: [Timer] Auto-sliding to next page (RIGHT edge)");
                slideToPage(currentPageIndex + 1, true);
                resetEdgeSlideState();
            } else {
                // 端ペEジの場合E何もしなぁE
                System.out.println("HomeScreen: [Timer] Already at edge page, no auto-slide");
                resetEdgeSlideState();
            }
        }
    }

    /**
     * 持EEEしたEEージにスライドする、
     * ドラチEEE継続中でも呼び出せるようにする、
     *
     * @param pageIndex 移動EのペEジインチEEEクス
     * @param maintainDrag ドラチEEE状態を維持するかどぁEEE
     */
    private void slideToPage(int pageIndex, boolean maintainDrag) {
        if (pageIndex < 0 || pageIndex >= homePages.size() || pageIndex == currentPageIndex) {
            return;
        }

        // ドラチEEE中のショートカチEEEの報EEを保孁E
        Shortcut savedDraggedShortcut = null;
        int savedDragOffsetX = 0, savedDragOffsetY = 0;
        boolean wasDragging = isDragging && draggedShortcut != null;

        if (wasDragging && maintainDrag) {
            savedDraggedShortcut = draggedShortcut;
            savedDragOffsetX = dragOffsetX;
            savedDragOffsetY = dragOffsetY;
            System.out.println("HomeScreen: Saving drag state for shortcut: " + savedDraggedShortcut.getDisplayName());
        }

        // ペEジ刁EEE替えを実衁E
        currentPageIndex = pageIndex;
        targetPageIndex = pageIndex;
        isAnimating = true;
        animationStartTime = System.currentTimeMillis();
        animationProgress = 0.0f;
        pageTransitionOffset = 0.0f;
        startOffset = 0.0f;
        animationBasePageIndex = pageIndex;

        System.out.println("HomeScreen: Sliding to page " + pageIndex + " (maintainDrag=" + maintainDrag + ")");

        // ドラチEEE状態を復允E
        if (wasDragging && maintainDrag && savedDraggedShortcut != null) {
            draggedShortcut = savedDraggedShortcut;
            dragOffsetX = savedDragOffsetX;
            dragOffsetY = savedDragOffsetY;
            isDragging = true;

            // ペEジ刁EEE替え後にドラチEEE位置を画面冁EEE安Eな場所に調整
            adjustDragPositionAfterSlide();

            System.out.println("HomeScreen: Restored drag state for shortcut: " + draggedShortcut.getDisplayName());
        }
    }

    /**
     * ペEジスライド後EドラチEEE位置を画面冁EEE安Eな場所に調整する、
     * 画面端でスライドが発生した場合、ショートカチEEEを画面冁EEE適刁EEE位置に配置する、
     */
    private void adjustDragPositionAfterSlide() {
        if (draggedShortcut == null) {
            return;
        }

        // 現在のドラチEEE位置を取征E
        float currentDragX = draggedShortcut.getDragX();
        float currentDragY = draggedShortcut.getDragY();

        // 画面墁EEE
        final int MARGIN = 10; // 画面端からの安EマEジン
        final int MIN_X = MARGIN;
        final int MAX_X = SCREEN_WIDTH - ICON_SIZE - MARGIN;
        final int MIN_Y = 80; // スチEEEタスバE丁E
        final int MAX_Y = 600 - NAV_AREA_HEIGHT - ICON_SIZE - MARGIN; // ナビゲーションエリア丁E

        float adjustedX = currentDragX;
        float adjustedY = currentDragY;

        // 左端からのスライドE場合、右側の安Eな位置に移勁E
        if (currentDragX < EDGE_SLIDE_ZONE) {
            adjustedX = EDGE_SLIDE_ZONE + 20; // 端検Eゾーンから少し冁EEE
            System.out.println("HomeScreen: Adjusting drag X from " + currentDragX + " to " + adjustedX + " (left edge slide)");
        }
        // 右端からのスライドE場合、左側の安Eな位置に移勁E
        else if (currentDragX > (SCREEN_WIDTH - EDGE_SLIDE_ZONE)) {
            adjustedX = SCREEN_WIDTH - EDGE_SLIDE_ZONE - 20; // 端検Eゾーンから少し冁EEE
            System.out.println("HomeScreen: Adjusting drag X from " + currentDragX + " to " + adjustedX + " (right edge slide)");
        }

        // Y座標E墁EEEチェチEEE
        if (adjustedY < MIN_Y) {
            adjustedY = MIN_Y;
            System.out.println("HomeScreen: Adjusting drag Y from " + currentDragY + " to " + adjustedY + " (top boundary)");
        } else if (adjustedY > MAX_Y) {
            adjustedY = MAX_Y;
            System.out.println("HomeScreen: Adjusting drag Y from " + currentDragY + " to " + adjustedY + " (bottom boundary)");
        }

        // X座標E最終墁EEEチェチEEEEE念のためEEEE
        if (adjustedX < MIN_X) {
            adjustedX = MIN_X;
            System.out.println("HomeScreen: Final X adjustment from " + currentDragX + " to " + adjustedX + " (left boundary)");
        } else if (adjustedX > MAX_X) {
            adjustedX = MAX_X;
            System.out.println("HomeScreen: Final X adjustment from " + currentDragX + " to " + adjustedX + " (right boundary)");
        }

        // 調整された座標を設定
        draggedShortcut.setDragPosition((int)adjustedX, (int)adjustedY);

        // lastDragX/Yも更新EEE継続的なエチEEEチェチEEE用EEEE
        lastDragX = (int)adjustedX;
        lastDragY = (int)adjustedY;

        System.out.println("HomeScreen: Drag position adjusted to (" + (int)adjustedX + ", " + (int)adjustedY + ")");
    }

    /**
     * ドラチEEE座標を画面墁EEE冁EEE制限する、
     *
     * @param dragX 允EEEドラチEEEX座樁E
     * @param dragY 允EEEドラチEEEY座樁E
     * @return 制限後E座樁E[adjustedX, adjustedY]
     */
    private int[] constrainDragPosition(int dragX, int dragY) {
        // 画面墁EEEEE通常のドラチEEE用EEEE
        final int MIN_X = -10; // 少し画面外まで許可EEEエチEEE検EのためEEEE
        final int MAX_X = SCREEN_WIDTH + 10; // 少し画面外まで許可EEEエチEEE検EのためEEEE
        final int MIN_Y = 80; // スチEEEタスバE丁E
        final int MAX_Y = 600 - NAV_AREA_HEIGHT - 10; // ナビゲーションエリア丁E

        int adjustedX = Math.max(MIN_X, Math.min(MAX_X, dragX));
        int adjustedY = Math.max(MIN_Y, Math.min(MAX_Y, dragY));

        return new int[]{adjustedX, adjustedY};
    }

    /**
     * 全ペEジからショートカチEEEを削除するEEEEEージ間移動時の重褁EEE止EEE、
     *
     * @param shortcut 削除するショートカチEEE
     */
    private void removeShortcutFromAllPages(Shortcut shortcut) {
        if (shortcut == null) return;

        System.out.println("HomeScreen: [REMOVE] Removing shortcut '" + shortcut.getDisplayName() + "' from all pages");

        for (int i = 0; i < homePages.size(); i++) {
            HomePage page = homePages.get(i);
            boolean removed = page.removeShortcut(shortcut);
            if (removed) {
                System.out.println("HomeScreen: [REMOVE] Removed from page " + i);
            }
        }
    }

    /**
     * アニメーション中にドロチEEEが発生した場合、アニメーション完了EEに実行するよぁEEEケジュールする、
     *
     * @param mouseX ドロチEEEのX座樁E
     * @param mouseY ドロチEEEのY座樁E
     */
    private void scheduleDelayedDrop(int mouseX, int mouseY) {
        hasPendingDrop = true;
        pendingDropX = mouseX;
        pendingDropY = mouseY;
        pendingDropShortcut = draggedShortcut;

        System.out.println("HomeScreen: [DROP] Scheduled delayed drop for shortcut '" +
                          (pendingDropShortcut != null ? pendingDropShortcut.getDisplayName() : "null") +
                          "' at (" + mouseX + ", " + mouseY + ")");

        // ドラチEEE状態をぁEEEたんクリアEEEただし、E延ドロチEEEのためにショートカチEEE報EEは保持EEEE
        isDragging = false;
    }

    /**
     * アニメーション完了EEに遁EEEされたドロチEEEを実行する、
     */
    private void executePendingDrop() {
        if (hasPendingDrop && pendingDropShortcut != null) {
            System.out.println("HomeScreen: [DROP] Executing pending drop for shortcut '" +
                              pendingDropShortcut.getDisplayName() + "' at (" + pendingDropX + ", " + pendingDropY + ")");

            // 遁EEEドロチEEEの実衁E
            executeDrop(pendingDropX, pendingDropY, pendingDropShortcut);

            // 遁EEEドロチEEE状態をリセチEEE
            hasPendingDrop = false;
            pendingDropX = 0;
            pendingDropY = 0;
            pendingDropShortcut = null;
        }
    }

    /**
     * 実際のドロチEEE処理EE実行する（即座実行と遁EEE実行で共通）、
     *
     * @param mouseX ドロチEEEのX座樁E
     * @param mouseY ドロチEEEのY座樁E
     * @param shortcut ドロチEEEするショートカチEEE
     */
    private void executeDrop(int mouseX, int mouseY, Shortcut shortcut) {
        if (shortcut == null) return;

        System.out.println("HomeScreen: [EXECUTE] Executing drop for shortcut '" + shortcut.getDisplayName() +
                          "' at (" + mouseX + ", " + mouseY + ") on page " + currentPageIndex);

        // Calculate target grid position
        int[] gridPos = screenToGridPosition(mouseX, mouseY);
        if (gridPos != null) {
            HomePage targetPage = getCurrentPage();
            if (targetPage != null) {
                System.out.println("HomeScreen: [EXECUTE] Target page: " + currentPageIndex + ", Grid position: (" + gridPos[0] + ", " + gridPos[1] + ")");

                // 安Eな配置処理EEEEに配置を試行し、功した場合Eみ他EペEジから削除
                boolean placed = safelyPlaceShortcut(shortcut, targetPage, gridPos[0], gridPos[1]);

                if (placed) {
                    System.out.println("HomeScreen: [EXECUTE] ショートカチEEE '" + shortcut.getDisplayName() +
                                     "' をEージ " + currentPageIndex + " の (" + gridPos[0] + ", " + gridPos[1] + ") に配置しました");
                    saveCurrentLayout();
                } else {
                    System.out.println("HomeScreen: [EXECUTE] ショートカット配置失敗 - フォールバック処理実行");

                    // 配置失敗時は最初E空きスロチEEEに配置
                    int[] emptySlot = findFirstEmptySlot(targetPage);
                    if (emptySlot != null && safelyPlaceShortcut(shortcut, targetPage, emptySlot[0], emptySlot[1])) {
                        System.out.println("HomeScreen: [EXECUTE] フォールバック: 空きスロチEEE (" + emptySlot[0] + ", " + emptySlot[1] + ") に配置しました");
                        saveCurrentLayout();
                    } else {
                        System.out.println("HomeScreen: [EXECUTE] エラー: 配置可能な空きスロットがありません - ショートカットを元の場所に戻します");
                        // 最悪の場合E允EEE場所に戻す（削除を防ぐ）
                        restoreShortcutToSafePage(shortcut);
                    }
                }
            }
        }

        // Reset drag state
        resetDragState();
    }

    /**
     * ショートカチEEEを安Eに配置する、
     * 他EペEジから削除する前に、まず目標Eージに配置できることを確認する、
     *
     * @param shortcut 配置するショートカチEEE
     * @param targetPage 目標Eージ
     * @param gridX 目標グリチEEEX座樁E
     * @param gridY 目標グリチEEEY座樁E
     * @return 配置に成功した場合rue
     */
    private boolean safelyPlaceShortcut(Shortcut shortcut, HomePage targetPage, int gridX, int gridY) {
        if (shortcut == null || targetPage == null) {
            return false;
        }

        System.out.println("HomeScreen: [SAFE_PLACE] Attempting to place shortcut '" + shortcut.getDisplayName() +
                          "' at (" + gridX + ", " + gridY + ") on page " + currentPageIndex);

        // ショートカチEEEが既にターゲチEEEペEジにある場合E通常のmoveShortcutを使用
        if (targetPage.getShortcuts().contains(shortcut)) {
            System.out.println("HomeScreen: [SAFE_PLACE] Shortcut already on target page, using moveShortcut");
            return targetPage.moveShortcut(shortcut, gridX, gridY);
        }

        // ショートカチEEEが他EペEジにある場合
        // 1. まず、目標位置が空ぁEEEぁEEEかチェチEEE
        if (!targetPage.isPositionEmpty(gridX, gridY)) {
            System.out.println("HomeScreen: [SAFE_PLACE] Target position is occupied");
            return false;
        }

        // 2. 他EペEジから削除
        HomePage sourcePageFound = null;
        for (HomePage page : homePages) {
            if (page.getShortcuts().contains(shortcut)) {
                sourcePageFound = page;
                break;
            }
        }

        if (sourcePageFound != null) {
            System.out.println("HomeScreen: [SAFE_PLACE] Removing shortcut from source page");
            boolean removed = sourcePageFound.removeShortcut(shortcut);
            if (!removed) {
                System.out.println("HomeScreen: [SAFE_PLACE] Failed to remove from source page");
                return false;
            }
        }

        // 3. ターゲチEEEペEジに追加
        boolean added = targetPage.addShortcut(shortcut, gridX, gridY);
        if (!added) {
            System.out.println("HomeScreen: [SAFE_PLACE] Failed to add to target page - restoring to source page");
            // 追加に失敗した場合E允EEEペEジに戻ぁE
            if (sourcePageFound != null) {
                int[] emptySlot = findFirstEmptySlot(sourcePageFound);
                if (emptySlot != null) {
                    sourcePageFound.addShortcut(shortcut, emptySlot[0], emptySlot[1]);
                    System.out.println("HomeScreen: [SAFE_PLACE] Restored shortcut to source page at (" + emptySlot[0] + ", " + emptySlot[1] + ")");
                }
            }
            return false;
        }

        System.out.println("HomeScreen: [SAFE_PLACE] Successfully placed shortcut at (" + gridX + ", " + gridY + ")");
        return true;
    }

    /**
     * ショートカチEEEを安Eな場所に復允EEEる（E置失敗時のフォールバックEEE、
     *
     * @param shortcut 復允EEEるショートカチEEE
     */
    private void restoreShortcutToSafePage(Shortcut shortcut) {
        if (shortcut == null) return;

        System.out.println("HomeScreen: [RESTORE] Restoring shortcut '" + shortcut.getDisplayName() + "' to safe page");

        // 最初EペEジで空きスロチEEEを探ぁE
        for (HomePage page : homePages) {
            if (page.isAppLibraryPage()) continue; // AppLibraryペEジはスキチEEE

            int[] emptySlot = findFirstEmptySlot(page);
            if (emptySlot != null) {
                boolean added = page.addShortcut(shortcut, emptySlot[0], emptySlot[1]);
                if (added) {
                    System.out.println("HomeScreen: [RESTORE] Restored shortcut to page at (" + emptySlot[0] + ", " + emptySlot[1] + ")");
                    return;
                }
            }
        }

        System.out.println("HomeScreen: [RESTORE] Warning: Could not find safe page for shortcut");
    }
}
