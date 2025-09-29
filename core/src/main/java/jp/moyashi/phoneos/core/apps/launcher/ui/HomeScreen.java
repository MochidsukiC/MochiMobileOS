package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.apps.launcher.model.HomePage;
import jp.moyashi.phoneos.core.apps.launcher.model.Shortcut;
import jp.moyashi.phoneos.core.input.GestureListener;
import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
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
public class HomeScreen implements Screen, GestureListener {
    
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
    private float startOffset = 0.0f; // アニメーション開始時のオフセット
    private int animationBasePageIndex = 0; // アニメーション中の基準ページ（固定）
    private static final long ANIMATION_DURATION = 500; // 500ms for smoother animation
    
    /** Grid configuration for app shortcuts */
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 5;
    private static final int ICON_SIZE = 48; // Reduced from 64 to 48
    private static final int ICON_SPACING = 15; // Reduced from 20 to 15
    
    /** App library navigation area */
    private static final int NAV_AREA_HEIGHT = 100;
    
    /**
     * Constructs a new HomeScreen instance.
     * 
     * @param kernel The OS kernel instance providing access to system services
     */
    public HomeScreen(Kernel kernel) {
        this.kernel = kernel;
        this.backgroundColor = 0x1E1E1E; // Dark theme background
        this.textColor = 0xFFFFFF;       // White text
        this.accentColor = 0x4A90E2;     // Blue accent
        this.isInitialized = false;
        this.homePages = new ArrayList<>();
        this.currentPageIndex = 0;
        this.isEditing = false;
        this.longPressTriggered = false;
        this.draggedShortcut = null;
        this.isDragging = false;
        this.isSwipingPages = false;
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
            System.out.println("⚠️ HomeScreen: setup() called again - skipping duplicate initialization");
            return;
        }
        
        isInitialized = true;
        System.out.println("🚀 HomeScreen: Initializing multi-page launcher...");
        
        // 背景画像を読み込み
        loadBackgroundImage();
        
        initializeHomePages();
        
        // Count total shortcuts
        int totalShortcuts = 0;
        for (HomePage page : homePages) {
            totalShortcuts += page.getShortcutCount();
        }
        
        System.out.println("✅ HomeScreen: Initialization complete!");
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
            // ページタイプに応じた背景処理
            HomePage currentPage = getCurrentPage();
            if (currentPage != null && currentPage.isAppLibraryPage()) {
                // AppLibraryページ用の背景
                g.background(42, 42, 42); // ダークグレー
            } else {
                // 通常ページ用の背景
                if (backgroundImage != null) {
                    g.background(30, 30, 30); // ベース背景色
                    g.image(backgroundImage, 0, 0, 400, 600);
                } else {
                    g.background(30, 30, 30);
                }
            }

            // Update page transition animation
            updatePageAnimation();

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

        } catch (Exception e) {
            System.err.println("❌ HomeScreen: Draw error (PGraphics) - " + e.getMessage());
            e.printStackTrace();
            // Fallback drawing
            g.background(255, 0, 0);
            g.fill(255);
            g.textAlign(g.CENTER, g.CENTER);

            // 日本語フォントを設定
            if (kernel != null && kernel.getJapaneseFont() != null) {
                g.textFont(kernel.getJapaneseFont());
            }

            g.textSize(16);
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

        // Check if click is in navigation area (app library), but not in control center area
        // Control center area starts at 90% of screen height (540px), nav area ends at 500px
        if (mouseY > (600 - NAV_AREA_HEIGHT) && mouseY < 540) {
            openAppLibrary();
            return;
        }

        // Check if click is on a shortcut (座標変換を考慮)
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
                // 編集モード中に空のスペースをクリックした場合は編集モード終了を予約
                // 実際の処理はGestureManagerのTAPイベントで実行される
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
     * 座標変換を考慮した削除ボタンクリック判定。
     * 
     * @param mouseX マウスX座標（絶対座標）
     * @param mouseY マウスY座標（絶対座標）
     * @param shortcut 対象のショートカット
     * @return 削除ボタンをクリックした場合true
     */
    private boolean isClickingDeleteButtonWithTransform(int mouseX, int mouseY, Shortcut shortcut) {
        if (!isEditing) return false;
        
        // 現在の座標変換オフセットを計算
        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        
        // ショートカットがどのページにあるかを特定
        int shortcutPageIndex = -1;
        for (int i = 0; i < homePages.size(); i++) {
            HomePage page = homePages.get(i);
            if (page.getShortcuts().contains(shortcut)) {
                shortcutPageIndex = i;
                break;
            }
        }
        
        if (shortcutPageIndex == -1) return false;
        
        // ページ内でのショートカット座標を計算
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        
        int iconX = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
        int iconY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20);
        
        // 画面上での削除ボタン位置を計算（座標変換を考慮）
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
        // GestureManagerシステムが有効な場合は何もしない
        // 実際のドラッグ処理は onGesture -> handleDragMove で実行される
        System.out.println("HomeScreen: mouseDragged called - delegating to GestureManager");
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
        // GestureManagerシステムが有効な場合は基本的に何もしない
        // 実際の処理は onGesture -> handleDragEnd, handleLongPress で実行される
        System.out.println("HomeScreen: mouseReleased called - delegating to GestureManager");

        // 念のため状態をリセット（安全措置）
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
        
        // 座標変換を考慮してショートカットの画面上位置を計算
        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        
        // ショートカットがどのページにあるかを特定
        int shortcutPageIndex = -1;
        for (int i = 0; i < homePages.size(); i++) {
            HomePage page = homePages.get(i);
            if (page.getShortcuts().contains(shortcut)) {
                shortcutPageIndex = i;
                break;
            }
        }
        
        if (shortcutPageIndex != -1) {
            // ページ内でのショートカット座標を計算
            int startY = 80;
            int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
            int startX = (400 - gridWidth) / 2;
            
            int localShortcutX = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
            int shortcutY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20);
            
            // 画面上でのショートカット位置を計算（座標変換を考慮）
            int screenShortcutX = (int) (totalOffset + shortcutPageIndex * 400 + localShortcutX);
            
            dragOffsetX = mouseX - screenShortcutX;
            dragOffsetY = mouseY - shortcutY;
        } else {
            // フォールバック: 従来の計算
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

        // アニメーション中の場合はドロップを遅延実行
        if (isAnimating) {
            System.out.println("HomeScreen: [DROP] Animation in progress, scheduling drop for later");
            scheduleDelayedDrop(mouseX, mouseY);
            return;
        }

        // 即座にドロップを実行
        executeDrop(mouseX, mouseY, draggedShortcut);
    }

    /**
     * Handles moving a shortcut to the next page when dragged to the right edge.
     */
    private void handleShortcutMoveToNextPage() {
        if (draggedShortcut == null) return;

        HomePage currentPage = getCurrentPage();
        if (currentPage != null) {
            // 現在のページからショートカットを削除
            currentPage.removeShortcut(draggedShortcut);

            // 次のページを取得または作成
            int nextPageIndex = currentPageIndex + 1;
            if (nextPageIndex >= homePages.size()) {
                // 新しいページを作成
                addNewPage();
            }

            // 次のページに移動
            HomePage nextPage = homePages.get(nextPageIndex);
            if (nextPage != null && !nextPage.isAppLibraryPage()) {
                // 次のページの最初の空きスロットに追加
                int[] emptySlot = findFirstEmptySlot(nextPage);
                if (emptySlot != null) {
                    draggedShortcut.setGridPosition(emptySlot[0], emptySlot[1]);
                    nextPage.addShortcut(draggedShortcut);

                    // 次のページに自動的にスライド
                    startPageTransition(nextPageIndex);

                    System.out.println("HomeScreen: ショートカットを次のページに移動しました");

                    // レイアウトを自動保存
                    saveCurrentLayout();
                } else {
                    // 次のページがフルの場合、さらに新しいページを作成
                    addNewPage();
                    HomePage newPage = homePages.get(homePages.size() - 1);
                    draggedShortcut.setGridPosition(0, 0);
                    newPage.addShortcut(draggedShortcut);
                    startPageTransition(homePages.size() - 1);

                    System.out.println("HomeScreen: 新しいページを作成してショートカットを移動しました");
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
            System.out.println("HomeScreen: ショートカット削除: " + shortcut.getDisplayName());
            
            // レイアウトを自動保存
            saveCurrentLayout();
        }
    }
    
    /**
     * 現在のレイアウトをLayoutManagerに保存する。
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

        isInitialized = false;
        resetDragState();
        isEditing = false;
        System.out.println("HomeScreen: Launcher home screen cleaned up");
    }
    
    /**
     * 背景画像を読み込む。
     */
    private void loadBackgroundImage() {
        try {
            // TODO: PGraphics統一アーキテクチャに対応した画像読み込み機能を実装
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
     * ホームページのリストを取得する。
     * AppLibraryScreenからアクセスするために使用される。
     * 
     * @return ホームページのリスト
     */
    public List<HomePage> getHomePages() {
        return homePages;
    }
    
    /**
     * 最初のページ（メインホームページ）に移動する。
     * スペースキーによるホームナビゲーション用。
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
     * ホームページを初期化し、保存されたレイアウトを読み込むかアプリを配置する。
     * まず保存されたレイアウトの読み込みを試行し、存在しない場合はデフォルトレイアウトを作成する。
     */
    private void initializeHomePages() {
        try {
            homePages.clear();
            
            // 保存されたレイアウトを読み込む試行
            boolean layoutLoaded = false;
            if (kernel != null && kernel.getLayoutManager() != null) {
                System.out.println("HomeScreen: 保存されたレイアウトを読み込み中...");
                List<HomePage> savedLayout = kernel.getLayoutManager().loadLayout();
                
                if (savedLayout != null && !savedLayout.isEmpty()) {
                    homePages.addAll(savedLayout);
                    layoutLoaded = true;
                    System.out.println("HomeScreen: 保存されたレイアウトを復元しました (" + homePages.size() + "ページ)");
                } else {
                    System.out.println("HomeScreen: 保存されたレイアウトが見つかりません、デフォルトレイアウトを作成");
                }
            }

            
            // 保存されたレイアウトがない場合はデフォルトレイアウトを作成
            if (!layoutLoaded) {
                createDefaultLayout();
            }
            
            // AppLibraryページの重複を防ぐ（厳密なチェック）
            long appLibraryCount = homePages.stream()
                .filter(page -> page.getPageType() == HomePage.PageType.APP_LIBRARY)
                .count();
                
            System.out.println("HomeScreen: 現在のAppLibraryページ数: " + appLibraryCount);
            
            if (appLibraryCount == 0) {
                createAppLibraryPage();
                System.out.println("HomeScreen: AppLibraryページを新規追加しました");
            } else if (appLibraryCount > 1) {
                // 重複がある場合は修正
                System.out.println("HomeScreen: ⚠️ AppLibraryページが重複しています(" + appLibraryCount + "個) - 修正中...");
                // 最初のもの以外を削除
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
                System.out.println("HomeScreen: ✅ " + toRemove.size() + "個の重複AppLibraryページを削除しました");
            } else {
                System.out.println("HomeScreen: AppLibraryページは既に存在します");
            }
            
            System.out.println("HomeScreen: " + homePages.size() + "ページでホーム画面を初期化完了");
            
        } catch (Exception e) {
            System.err.println("HomeScreen: initializeHomePages でクリティカルエラー: " + e.getMessage());
            e.printStackTrace();
            // 緊急時は少なくとも1つの空ページを確保
            if (homePages.isEmpty()) {
                homePages.add(new HomePage("Emergency"));
            }
        }
    }
    
    /**
     * デフォルトのレイアウトを作成し、利用可能なアプリを配置する。
     */
    private void createDefaultLayout() {
        // 最初のページを作成
        HomePage firstPage = new HomePage("Home");
        homePages.add(firstPage);
        
        if (kernel != null && kernel.getAppLoader() != null) {
            try {
                List<IApplication> loadedApps = kernel.getAppLoader().getLoadedApps();
                if (loadedApps != null) {
                    // ランチャー以外のロード済みアプリを追加
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
                                // 現在のページが満員の場合は新しいページを作成
                                currentPage = new HomePage();
                                homePages.add(currentPage);
                            }
                            currentPage.addShortcut(app);
                        } catch (Exception e) {
                            System.err.println("HomeScreen: ページへのアプリ追加エラー: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("HomeScreen: AppLoaderアクセスエラー: " + e.getMessage());
            }
        } else {
            System.out.println("HomeScreen: KernelまたはAppLoaderがnull - 空のページを作成");
        }
        
        // デフォルトレイアウトを保存
        if (kernel != null && kernel.getLayoutManager() != null) {
            kernel.getLayoutManager().saveLayout(homePages);
            System.out.println("HomeScreen: デフォルトレイアウトを保存しました");
        }
    }
    
    /**
     * AppLibraryページを作成し、全アプリケーションを設定する。
     */
    private void createAppLibraryPage() {
        System.out.println("HomeScreen: AppLibraryページを作成中...");
        
        // AppLibraryページを作成
        HomePage appLibraryPage = new HomePage(HomePage.PageType.APP_LIBRARY, "App Library");
        
        // 全アプリケーションを取得してAppLibraryページに設定
        if (kernel != null && kernel.getAppLoader() != null) {
            try {
                List<IApplication> allApps = kernel.getAppLoader().getLoadedApps();
                if (allApps != null) {
                    // ランチャー以外のアプリを取得
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
                System.err.println("HomeScreen: AppLibraryページ作成エラー: " + e.getMessage());
            }
        }
        
        // ページリストに追加
        homePages.add(appLibraryPage);
        System.out.println("HomeScreen: AppLibraryページを追加しました");
        System.out.println("HomeScreen: 総ページ数: " + homePages.size() + ", AppLibraryページインデックス: " + (homePages.size() - 1));
    }
    
    /**
     * Draws the status bar at the top of the screen.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawStatusBar(PGraphics g) {
        try {
            g.fill(textColor, 180); // Semi-transparent text
            g.textAlign(g.LEFT, g.TOP);
            g.textSize(12);
            
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
                                 
                g.fill(255, 255, 255, 150);
                g.textAlign(g.CENTER, g.TOP);
                g.textSize(11);
                g.text(pageName, 200, 15);
            }
            
            // Status indicator
            if (isInitialized) {
                g.fill(76, 175, 80); // Green if ready (0x4CAF50 -> RGB)
            } else {
                g.fill(255, 152, 0); // Orange if not (0xFF9800 -> RGB)
            }
            g.noStroke();
            g.ellipse(370, 20, 8, 8);
            
        } catch (Exception e) {
            System.err.println("Error in drawStatusBar: " + e.getMessage());
            // Fallback: just draw a simple status
            g.fill(255);
            g.textAlign(g.LEFT, g.TOP);
            g.textSize(12);
            g.text("Status Error", 15, 15);
        }
    }
    
    /**
     * アニメーションの進行度を更新する。
     */
    private void updatePageAnimation() {
        if (!isAnimating) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - animationStartTime;
        
        if (elapsed >= ANIMATION_DURATION) {
            // アニメーション完了
            completePageTransition();
        } else {
            // アニメーション進行中 - イージング関数を適用
            float t = (float) elapsed / ANIMATION_DURATION;
            animationProgress = easeOutCubic(t);
            
            // ページオフセットを計算
            if (targetPageIndex == animationBasePageIndex) {
                // 元のページに戻るアニメーション - ドラッグ位置から0に戻る
                pageTransitionOffset = startOffset * (1.0f - animationProgress);
            } else {
                // ページ切り替えアニメーション - 開始位置から目標位置への補間
                float targetOffset = (animationBasePageIndex - targetPageIndex) * 400;
                pageTransitionOffset = startOffset + (targetOffset - startOffset) * animationProgress;
                System.out.println("🎬 Animation: basePage=" + animationBasePageIndex + " to targetPage=" + targetPageIndex + 
                                 ", startOffset=" + startOffset + ", targetOffset=" + targetOffset + ", progress=" + animationProgress + ", offset=" + pageTransitionOffset);
            }
        }
    }
    
    /**
     * イージング関数（ease-out quad - より穏やか）
     * 
     * @param t 進行度 (0.0 ～ 1.0)
     * @return イージング適用後の値
     */
    private float easeOutCubic(float t) {
        // ease-out quadratic - より自然で穏やかな動き
        return 1 - (1 - t) * (1 - t);
    }
    
    /**
     * ページ切り替えアニメーションを完了する。
     */
    private void completePageTransition() {
        // アニメーション完了時にページインデックスを更新し、座標系をリセット
        System.out.println("🎬 Completing transition: currentPage=" + currentPageIndex + " -> targetPage=" + targetPageIndex);
        
        // ページインデックスを目標に更新
        currentPageIndex = targetPageIndex;
        
        // 座標系をリセット
        pageTransitionOffset = 0.0f;
        isAnimating = false;
        animationProgress = 0.0f;
        startOffset = 0.0f;
        
        System.out.println("🎬 Page transition completed to page " + currentPageIndex + ", offset reset to 0");

        // アニメーション完了後に遅延されたドロップを実行
        executePendingDrop();
    }
    
    /**
     * ページ切り替えアニメーションを開始する。
     * 
     * @param newPageIndex 目標ページインデックス
     */
    private void startPageTransition(int newPageIndex) {
        if (newPageIndex == currentPageIndex || isAnimating) {
            return; // 同じページまたはアニメーション中は無視
        }
        
        targetPageIndex = newPageIndex;
        isAnimating = true;
        animationStartTime = System.currentTimeMillis();
        animationProgress = 0.0f;
        startOffset = pageTransitionOffset; // 現在のオフセットを保存
        animationBasePageIndex = currentPageIndex; // 座標計算の基準ページを固定
        
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
            g.textSize(16);
            g.text("No apps installed", 200, 300);
            g.textSize(12);
            g.text("Swipe up to access app library", 200, 320);
            return;
        }
        
        // 座標変換でページ切り替えアニメーションを実現
        g.pushMatrix();

        // ページ全体のオフセットを適用
        // アニメーション中は基準ページ（animationBasePageIndex）を使用してジャンプを防ぐ
        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        g.translate(totalOffset, 0);
        
        if (isAnimating) {
            System.out.println("🎨 Drawing with basePageIndex=" + basePageForOffset + ", pageTransitionOffset=" + pageTransitionOffset + ", totalOffset=" + totalOffset);
        }
        
        // 全ページを横に並べて描画
        for (int i = 0; i < homePages.size(); i++) {
            g.pushMatrix();
            g.translate(i * 400, 0); // 各ページを400px間隔で配置
            
            HomePage page = homePages.get(i);
            if (page.isAppLibraryPage()) {
                drawAppLibraryPage(g, page);
            } else {
                drawNormalPage(g, page);
            }
            
            g.popMatrix();
        }
        
        g.popMatrix();

        // ドラッグ中のアイコンを最上位レイヤー（変換なし）で描画
        if (isDragging && draggedShortcut != null) {
            drawDraggedShortcut(g, draggedShortcut);
        }
    }
    
    /**
     * マウス座標をページ座標系に変換する。
     * 
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @return [変換後X座標, 変換後Y座標, ページインデックス]
     */
    private int[] transformMouseCoordinates(int mouseX, int mouseY) {
        // 現在の変換行列を考慮してマウス座標を変換
        float totalOffset = -currentPageIndex * 400 + pageTransitionOffset;
        float transformedX = mouseX - totalOffset;
        
        // どのページ上のクリックかを判定
        int targetPageIndex = (int) (transformedX / 400);
        if (targetPageIndex < 0) targetPageIndex = 0;
        if (targetPageIndex >= homePages.size()) targetPageIndex = homePages.size() - 1;
        
        // ページ内座標に変換
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
        // 通常のページ描画処理
        int startY = 80; // Below status bar
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2; // Center the grid
        
        g.textAlign(g.CENTER, g.TOP);
        g.textSize(10);
        
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
        
        // ドラッグ中のショートカットは最上位レイヤーで描画するため、ここではスキップ
        // (drawPagesWithTransitionの最後で描画される)
        
        // Draw drop target indicators if dragging
        if (isDragging) {
            drawDropTargets(g, startX, startY);
        }
    }
    
    
    /**
     * AppLibraryページを描画する。
     * 
     * @param p The PApplet instance for drawing
     * @param appLibraryPage AppLibraryページ
     */
    private void drawAppLibraryPage(PGraphics g, HomePage appLibraryPage) {
        System.out.println("🎨 HomeScreen: drawAppLibraryPage() called - drawing AppLibrary content");
        
        // AppLibraryタイトルを描画
        g.fill(255, 255, 255); // 白色テキスト (0xFFFFFF -> RGB)
        g.textAlign(g.CENTER, g.TOP);
        g.textSize(18);
        System.out.println("🎨 Drawing title: 'App Library' at (200, 70) with size 18, color RGB(255,255,255)");
        g.text("App Library", 200, 70);
        System.out.println("🎨 Title drawing completed");

        // アプリリストを描画
        List<IApplication> apps = appLibraryPage.getAllApplications();
        System.out.println("🎨 AppLibrary apps count: " + apps.size());
        if (apps.isEmpty()) {
            g.fill(255, 255, 255, 150); // textColor with alpha -> RGB
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(14);
            g.text("No apps available", 200, 300);
            System.out.println("🎨 'No apps available' message drawn at (200, 300)");
            return;
        }
        
        int startY = 110; // タイトルの下から開始
        int listHeight = 600 - startY - NAV_AREA_HEIGHT - 20; // 利用可能な高さ
        int itemHeight = 70; // 各アプリアイテムの高さ
        int scrollOffset = appLibraryPage.getScrollOffset();
        System.out.println("🎨 Drawing " + apps.size() + " apps starting at Y=" + startY + ", scrollOffset=" + scrollOffset);
        
        // スクロール可能エリアを設定（クリッピング）
        g.pushMatrix();
        
        // アプリリストを描画
        for (int i = 0; i < apps.size(); i++) {
            IApplication app = apps.get(i);
            int itemY = startY + i * itemHeight - scrollOffset;
            
            // 表示エリア外のアイテムはスキップ
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
     * AppLibraryのアプリアイテムを描画する。
     * 
     * @param p The PApplet instance for drawing
     * @param app 描画するアプリケーション
     * @param x アイテムのX座標
     * @param y アイテムのY座標
     * @param width アイテムの幅
     * @param height アイテムの高さ
     */
    private void drawAppLibraryItem(PGraphics g, IApplication app, int x, int y, int width, int height) {
        // アイテムの背景
        g.fill(58, 58, 58, 100); // 0x3A3A3A -> RGB with alpha
        g.noStroke();
        g.rect(x, y, width, height, 8);

        // アプリアイコン
        g.fill(74, 144, 226); // accentColor (0x4A90E2) -> RGB
        g.rect(x + 10, y + 10, 50, 50, 8);

        // アプリ名の最初の文字
        g.fill(255, 255, 255); // textColor -> RGB
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(24);
        String initial = app.getName().substring(0, 1).toUpperCase();
        g.text(initial, x + 35, y + 35);

        // アプリ名
        g.fill(255, 255, 255); // textColor -> RGB
        g.textAlign(g.LEFT, g.CENTER);
        g.textSize(16);
        g.text(app.getName(), x + 75, y + 25);

        // アプリ説明（あれば）
        if (app.getDescription() != null && !app.getDescription().isEmpty()) {
            g.fill(255, 255, 255, 150); // textColor with alpha -> RGB
            g.textSize(12);
            String description = app.getDescription();
            if (description.length() > 40) {
                description = description.substring(0, 37) + "...";
            }
            g.text(description, x + 75, y + 45);
        }
        
        // 長押し時の「ホーム画面に追加」ボタンを描画
        // （実装は後で追加）
    }
    
    /**
     * スクロールインジケーターを描画する。
     * 
     * @param p The PApplet instance for drawing
     * @param appLibraryPage AppLibraryページ
     * @param listStartY リスト開始Y座標
     * @param listHeight リストの高さ
     * @param itemHeight アイテム高さ
     */
    private void drawScrollIndicator(PGraphics g, HomePage appLibraryPage, int listStartY, int listHeight, int itemHeight) {
        List<IApplication> apps = appLibraryPage.getAllApplications();
        int totalHeight = apps.size() * itemHeight;
        int scrollOffset = appLibraryPage.getScrollOffset();
        
        // スクロールバーの位置とサイズを計算
        float scrollbarHeight = Math.max(20, (float) listHeight * listHeight / totalHeight);
        float scrollbarY = listStartY + (float) scrollOffset * listHeight / totalHeight;
        
        // スクロールバーを描画
        g.fill(255, 255, 255, 100); // textColor with alpha -> RGB
        g.noStroke();
        g.rect(385, (int) scrollbarY, 6, (int) scrollbarHeight, 3);
    }
    
    /**
     * ドラッグ中のショートカットを絶対座標で描画する。
     * 座標変換の影響を受けずにマウス位置に正確に追従する。
     * 
     * @param p The PApplet instance for drawing
     * @param shortcut The dragged shortcut
     */
    private void drawDraggedShortcut(PGraphics g, Shortcut shortcut) {
        if (!shortcut.isDragging()) return;
        
        int x = (int) shortcut.getDragX();
        int y = (int) shortcut.getDragY();
        
        // ドロップシャドウを描画
        g.fill(0, 0, 0, 100);
        g.noStroke();
        g.rect(x + 4, y + 4, ICON_SIZE, ICON_SIZE, 12);

        // アイコンの背景を描画（半透明）
        g.fill(255, 255, 255, 220);
        g.stroke(85, 85, 85);
        g.strokeWeight(2);
        g.rect(x, y, ICON_SIZE, ICON_SIZE, 12);

        // アプリアイコンを描画
        IApplication app = shortcut.getApplication();
        if (app != null) {
            drawAppIcon(g, app, x + ICON_SIZE/2, y + ICON_SIZE/2);
        }

        // アプリ名を描画（ドラッグ中も同じスタイル）
        g.noStroke();
        g.textAlign(g.CENTER, g.TOP); // 中央配置、上詰め
        g.textSize(11); // メインのアイコンと同じフォントサイズ
        
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 10) {
            displayName = displayName.substring(0, 9) + "...";
        }
        
        // テキストの影を追加（ドラッグ中も可読性向上）
        g.fill(0, 0, 0, 120); // 少し濃い影
        g.text(displayName, x + ICON_SIZE/2 + 1, y + ICON_SIZE + 9);

        // メインテキストを描画
        g.fill(255, 255, 255); // 白色テキスト
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
                    
                    g.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
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
        
        // ドラッグ中のショートカットは専用メソッドで描画されるため、ここでは描画しない
        if (shortcut.isDragging()) {
            return;
        }
        
        // Draw app icon background
        g.fill(255);
        g.stroke(0x555555);
        g.strokeWeight(1);
        g.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
        
        // Draw app icon
        drawAppIcon(g, app, x + ICON_SIZE/2, y + ICON_SIZE/2);
        
        // Draw delete button if in edit mode
        if (isEditing) {
            g.fill(0xFF4444); // Red delete button
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
        g.fill(255, 255, 255); // 白色テキストで視認性向上
        g.noStroke();
        g.textAlign(g.CENTER, g.TOP); // 中央配置、上詰め
        g.textSize(11); // 適切なフォントサイズ
        
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 10) {
            displayName = displayName.substring(0, 9) + "...";
        }
        
        // テキストの影を追加して可読性向上
        g.fill(0, 0, 0, 100); // 半透明の黒い影
        g.text(displayName, x + ICON_SIZE/2 + 1, y + ICON_SIZE + 9);
        
        // メインテキストを描画
        g.fill(255, 255, 255); // 白色テキスト
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

        processing.core.PImage icon = app.getIcon(g);

        if (icon != null) {
            g.imageMode(PGraphics.CENTER);
            // Draw the icon, ensuring it fits within the icon size with some padding
            float padding = 8;
            float drawableSize = ICON_SIZE - padding * 2;
            g.image(icon, centerX, centerY, drawableSize, drawableSize);
            g.imageMode(PGraphics.CORNER); // Reset image mode to default
        } else {
            // Fallback to placeholder if icon is null
            g.rectMode(PGraphics.CENTER);
            g.fill(accentColor);
            g.noStroke();
            g.rect(centerX, centerY, 40, 40, 8);
            
            // Draw app initial
            g.fill(textColor);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(20);
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
        g.fill(0x2A2A2A);
        g.noStroke();
        g.rect(0, navY, 400, NAV_AREA_HEIGHT);
        
        // Draw app library access hint
        g.fill(textColor, 150);
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(14);
        g.text("App Library", 200, navY + 30);
        
        // Draw edit mode toggle hint if not in edit mode
        if (!isEditing) {
            g.textSize(10);
            g.text("Long press to edit", 200, navY + 50);
        } else {
            g.textSize(10);
            g.text("Tap outside to finish editing", 200, navY + 50);
        }
        
        // Draw swipe indicator for pages if multiple pages exist
        if (homePages.size() > 1) {
            g.stroke(textColor, 100);
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
        
        int dotY = 600 - NAV_AREA_HEIGHT - 25; // 少し上に移動
        int dotSize = 10;
        int activeDotSize = 14;
        int spacing = 20;
        int totalWidth = homePages.size() * spacing - (spacing - dotSize);
        int startX = (400 - totalWidth) / 2;
        
        // 背景の半透明エリア
        g.fill(0, 0, 0, 100);
        g.noStroke();
        g.rect(startX - 15, dotY - 10, totalWidth + 30, 20, 10);
        
        for (int i = 0; i < homePages.size(); i++) {
            int dotX = startX + i * spacing;
            
            if (i == currentPageIndex) {
                // 現在のページ - 大きく明るく
                g.fill(74, 144, 226); // アクセントカラー (accentColor RGB)
                g.noStroke();
                g.ellipse(dotX, dotY, activeDotSize, activeDotSize);
                
                // 外側のリング
                g.noFill();
                g.stroke(74, 144, 226, 150);
                g.strokeWeight(2);
                g.ellipse(dotX, dotY, activeDotSize + 4, activeDotSize + 4);
            } else {
                // 他のページ - 小さく薄く
                g.fill(255, 255, 255, 120);
                g.noStroke();
                g.ellipse(dotX, dotY, dotSize, dotSize);
            }
        }
        
        // AppLibraryページには特別なアイコン
        for (int i = 0; i < homePages.size(); i++) {
            HomePage page = homePages.get(i);
            if (page.isAppLibraryPage()) {
                int dotX = startX + i * spacing;
                
                // AppLibraryアイコン（グリッド風）
                g.stroke(i == currentPageIndex ? 255 : 200);
                g.strokeWeight(1);
                g.noFill();
                
                // 小さな3x3グリッド
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
     * マウス座標を座標変換後の座標に変換し、適切なページでショートカットを検索する。
     * 
     * @param mouseX マウスX座標（絶対座標）
     * @param mouseY マウスY座標（絶対座標）
     * @return 該当位置のショートカット、または null
     */
    private Shortcut getShortcutAtPositionWithTransform(int mouseX, int mouseY) {
        // 現在の座標変換オフセットを計算
        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        
        // マウス座標を変換後の座標系に調整
        float transformedX = mouseX - totalOffset;
        
        // どのページ範囲にいるかを判定
        int pageIndex = (int) Math.floor(transformedX / 400);
        
        // ページ範囲内でのローカル座標を計算
        int localX = (int) (transformedX - pageIndex * 400);
        
        // ページインデックスが有効範囲内かチェック
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
        
        // AppLibraryページ（最後のページ）に切り替え
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
        
        if (kernel != null && kernel.getScreenManager() != null) {
            try {
                Screen appScreen = app.getEntryScreen(kernel);
                kernel.getScreenManager().pushScreen(appScreen);
            } catch (Exception e) {
                System.err.println("HomeScreen: Failed to launch app " + app.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * アニメーション付きでアプリケーションを起動する
     */
    private void launchApplicationWithAnimation(IApplication app, float iconX, float iconY, float iconSize) {
        System.out.println("HomeScreen: Launching app with animation: " + app.getName());
        System.out.println("HomeScreen: Icon position: (" + iconX + ", " + iconY + "), size: " + iconSize);
        
        if (kernel != null && kernel.getScreenManager() != null) {
            try {
                Screen appScreen = app.getEntryScreen(kernel);
                System.out.println("HomeScreen: Got app screen: " + appScreen.getScreenTitle());
                
                // Get app icon for animation
                processing.core.PImage appIcon = null;
                if (kernel != null) {
                    // Use PGraphics-based icon retrieval
                    processing.core.PGraphics graphics = kernel.getGraphics();
                    if (graphics != null) {
                        appIcon = app.getIcon(graphics);
                        System.out.println("HomeScreen: Got app icon: " + (appIcon != null ? appIcon.width + "x" + appIcon.height : "null"));
                    } else {
                        System.out.println("HomeScreen: Kernel graphics is null");
                    }
                } else {
                    System.out.println("HomeScreen: Kernel is null");
                }
                
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
            // 編集モード開始時に空のページを最後に追加
            addEmptyPageIfNeeded();
        } else {
            // 編集モード終了時にはドラッグ状態をリセット
            resetDragState();
            System.out.println("HomeScreen: Reset drag state on edit mode exit");

            // 編集モード終了時に空のページを削除
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

        // 最後のページが空でない場合、または最後がAppLibraryページの場合は空ページを追加
        if (homePages.isEmpty()) {
            System.out.println("HomeScreen: No pages exist, adding first page");
            addNewPage();
            return;
        }

        // AppLibraryページの前に空ページを挿入するロジックに変更
        int insertIndex = homePages.size();
        HomePage lastPage = homePages.get(homePages.size() - 1);

        System.out.println("HomeScreen: Last page type: " + (lastPage.isAppLibraryPage() ? "APP_LIBRARY" : "NORMAL"));
        System.out.println("HomeScreen: Last page shortcuts count: " + lastPage.getShortcuts().size());

        // AppLibraryページがある場合は、その前に挿入
        if (lastPage.isAppLibraryPage()) {
            insertIndex = homePages.size() - 1; // AppLibraryページの前に挿入

            // AppLibraryページの前のページが空でない場合のみ空ページを追加
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
                // AppLibraryページが最初のページの場合（通常はない）
                HomePage newPage = new HomePage();
                homePages.add(0, newPage);
                System.out.println("HomeScreen: Added empty page before AppLibrary at index 0");
            }
        } else {
            // 最後のページが通常ページで空でない場合、空ページを追加
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

        // AppLibraryページを除いた通常ページの中で、後ろから空のページを削除
        boolean removedAny = false;
        for (int i = homePages.size() - 1; i >= 0; i--) {
            HomePage page = homePages.get(i);

            // AppLibraryページはスキップ
            if (page.isAppLibraryPage()) {
                System.out.println("HomeScreen: Skipping AppLibrary page at index " + i);
                continue;
            }

            // 空のページを削除
            if (page.getShortcuts().isEmpty()) {
                homePages.remove(i);
                removedAny = true;
                System.out.println("HomeScreen: Removed empty page at index " + i);

                // 現在のページが削除された場合は調整
                if (currentPageIndex >= homePages.size()) {
                    currentPageIndex = Math.max(0, homePages.size() - 1);
                    System.out.println("HomeScreen: Adjusted current page index to " + currentPageIndex);
                }

                // 現在のページインデックスが削除されたページ以降の場合は調整
                if (currentPageIndex > i) {
                    currentPageIndex--;
                    System.out.println("HomeScreen: Decremented current page index to " + currentPageIndex);
                }
            } else {
                // 空でないページが見つかったら、以降の削除は停止
                // （ただしAppLibraryページは除外）
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
        System.out.println("HomeScreen: Received gesture: " + event);
        
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
                return false; // 処理しないジェスチャー
        }
    }
    
    @Override
    public boolean isInBounds(int x, int y) {
        // HomeScreenが現在のスクリーンの場合のみ処理
        return kernel != null && 
               kernel.getScreenManager() != null && 
               kernel.getScreenManager().getCurrentScreen() == this;
    }
    
    @Override
    public int getPriority() {
        return 50; // 中優先度
    }
    
    /**
     * タップジェスチャーを処理する。
     * 
     * @param x X座標
     * @param y Y座標
     * @return 処理した場合true
     */
    private boolean handleTap(int x, int y) {
        System.out.println("HomeScreen: Handling tap at (" + x + ", " + y + ")");
        
        // マウス座標を変換
        int[] coords = transformMouseCoordinates(x, y);
        int pageX = coords[0];
        int pageY = coords[1];
        int targetPageIndex = coords[2];
        
        // 対象ページが範囲外の場合は無視
        if (targetPageIndex < 0 || targetPageIndex >= homePages.size()) {
            return false;
        }
        
        HomePage targetPage = homePages.get(targetPageIndex);
        System.out.println("HomeScreen: Transformed tap to page " + targetPageIndex + " at (" + pageX + ", " + pageY + ")");
        
        // AppLibraryページの場合の特別処理
        if (targetPage.isAppLibraryPage()) {
            return handleAppLibraryTap(pageX, pageY, targetPage);
        }
        
        // ナビゲーションエリア（下部）のタップでApp Libraryを開く（コントロールセンター領域を除く）
        if (pageY > (600 - NAV_AREA_HEIGHT) && pageY < 540) {
            openAppLibrary();
            return true;
        }
        
        // 対象ページが現在のページでない場合はページ切り替え
        if (targetPageIndex != currentPageIndex && !isAnimating) {
            startPageTransition(targetPageIndex);
            return true;
        }
        
        // ショートカットのタップ処理（現在のページのみ）
        if (targetPageIndex == currentPageIndex) {
            Shortcut tappedShortcut = getShortcutAtPosition(pageX, pageY, targetPage);
            if (tappedShortcut != null) {
                if (isEditing) {
                    // 編集モードでは削除ボタンかアイコンかをチェック
                    if (isClickingDeleteButton(pageX, pageY, tappedShortcut)) {
                        removeShortcut(tappedShortcut);
                    }
                } else {
                    // 通常モードではアプリ起動（アニメーション付き）
                    // ショートカットの画面上での位置を計算
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
        
        // 編集モード中に空のスペースをタップした場合は編集モードを終了
        if (isEditing) {
            System.out.println("HomeScreen: Tapped empty space in edit mode - exiting edit mode");
            toggleEditMode();
            return true;
        }
        
        return false;
    }
    
    /**
     * AppLibraryページでのタップを処理する。
     * 
     * @param x X座標
     * @param y Y座標
     * @param appLibraryPage AppLibraryページ
     * @return 処理した場合true
     */
    private boolean handleAppLibraryTap(int x, int y, HomePage appLibraryPage) {
        // ナビゲーションエリア（下部）のタップは無視
        if (y > (600 - NAV_AREA_HEIGHT)) {
            return false;
        }
        
        // アプリリストの範囲内かチェック
        int startY = 110;
        int listHeight = 600 - startY - NAV_AREA_HEIGHT - 20;
        int itemHeight = 70;
        
        if (y >= startY && y <= startY + listHeight) {
            // タップされたアプリケーションを取得
            IApplication tappedApp = appLibraryPage.getApplicationAtPosition(x, y, startY, itemHeight);
            if (tappedApp != null) {
                System.out.println("HomeScreen: AppLibraryでアプリをタップ: " + tappedApp.getName());
                // アイコン位置を計算（AppLibraryアイテム用）
                float iconX = 20 + 32; // ITEM_PADDING + ICON_SIZE/2
                float iconY = startY + ((y - startY) / itemHeight) * itemHeight + itemHeight / 2;
                launchApplicationWithAnimation(tappedApp, iconX, iconY, 32); // AppLibraryのアイコンサイズは32
                return true;
            }
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
        System.out.println("HomeScreen: Handling long press at (" + x + ", " + y + ")");
        
        // マウス座標を変換
        int[] coords = transformMouseCoordinates(x, y);
        int pageX = coords[0];
        int pageY = coords[1];
        int targetPageIndex = coords[2];
        
        // 対象ページが範囲外の場合は無視
        if (targetPageIndex < 0 || targetPageIndex >= homePages.size()) {
            return false;
        }
        
        HomePage targetPage = homePages.get(targetPageIndex);
        
        // AppLibraryページの場合の特別処理
        if (targetPage.isAppLibraryPage()) {
            return handleAppLibraryLongPress(pageX, pageY, targetPage);
        }
        
        // 現在のページでの長押しのみ編集モード切り替え
        if (targetPageIndex == currentPageIndex && !isEditing) {
            toggleEditMode();
            return true;
        }
        
        return false;
    }
    
    /**
     * AppLibraryページでの長押しを処理する。
     * 
     * @param x X座標
     * @param y Y座標
     * @param appLibraryPage AppLibraryページ
     * @return 処理した場合true
     */
    private boolean handleAppLibraryLongPress(int x, int y, HomePage appLibraryPage) {
        // ナビゲーションエリア（下部）の長押しは無視
        if (y > (600 - NAV_AREA_HEIGHT)) {
            return false;
        }
        
        // アプリリストの範囲内かチェック
        int startY = 110;
        int listHeight = 600 - startY - NAV_AREA_HEIGHT - 20;
        int itemHeight = 70;
        
        if (y >= startY && y <= startY + listHeight) {
            // 長押しされたアプリケーションを取得
            IApplication longPressedApp = appLibraryPage.getApplicationAtPosition(x, y, startY, itemHeight);
            if (longPressedApp != null) {
                System.out.println("HomeScreen: AppLibraryで長押し: " + longPressedApp.getName());
                showAddToHomePopup(longPressedApp, x, y);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 「ホーム画面に追加」ポップアップを表示する。
     * 
     * @param app 対象のアプリケーション
     * @param x ポップアップ表示位置X
     * @param y ポップアップ表示位置Y
     */
    private void showAddToHomePopup(IApplication app, int x, int y) {
        if (kernel != null && kernel.getPopupManager() != null) {
            System.out.println("HomeScreen: ✅ ポップアップマネージャーが利用可能");
            
            // 簡単なコンテキストメニューポップアップを作成
            String message = "「" + app.getName() + "」をホーム画面に追加しますか？";
            
            // ポップアップマネージャーに実装されたポップアップシステムを使用
            // （実際の実装はPopupManagerの仕様に依存）
            System.out.println("HomeScreen: 🎯 「ホーム画面に追加」ポップアップ表示予定");
            System.out.println("    • アプリ名: " + app.getName());
            System.out.println("    • 位置: (" + x + ", " + y + ")");
            System.out.println("    • メッセージ: " + message);
            
            // PopupManagerの実装に応じてここでポップアップを表示
            // 現在はログ出力のみ（実際のポップアップ実装は別途必要）
        } else {
            System.err.println("HomeScreen: ❌ PopupManagerが利用できません");
        }
    }
    
    /**
     * 左スワイプジェスチャーを処理する。
     * 
     * @return 処理した場合true
     */
    private boolean handleSwipeLeft() {
        // 編集モード中でもページスワイプを有効化（ドラッグ中は無効）
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
     * 右スワイプジェスチャーを処理する。
     * 
     * @return 処理した場合true
     */
    private boolean handleSwipeRight() {
        // 編集モード中でもページスワイプを有効化（ドラッグ中は無効）
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
     * 上スワイプジェスチャーを処理する。
     * 
     * @return 処理した場合true
     */
    private boolean handleSwipeUp(GestureEvent event) {
        // 画面下部（高さの90%以上）からのスワイプアップはKernelのコントロールセンター用に予約
        if (event.getStartY() >= 600 * 0.9f) {
            System.out.println("HomeScreen: Bottom swipe up detected - letting Kernel handle control center");
            return false; // Kernelに処理を委譲
        }
        
        // 画面の中央部からのスワイプアップでApp Libraryを開く
        System.out.println("HomeScreen: Up swipe detected - opening integrated App Library");
        openAppLibrary();
        return true;
    }
    
    /**
     * ドラッグ開始ジェスチャーを処理する。
     * 
     * @param event ジェスチャーイベント
     * @return 処理した場合true
     */
    private boolean handleDragStart(GestureEvent event) {
        HomePage currentPage = getCurrentPage();
        if (currentPage != null && currentPage.isAppLibraryPage()) {
            // AppLibraryページでのドラッグはスクロール開始
            return handleAppLibraryScrollStart(event);
        }
        
        // 編集モードでのアイコンドラッグを優先的に処理
        if (isEditing) {
            Shortcut clickedShortcut = getShortcutAtPositionWithTransform(event.getStartX(), event.getStartY());
            if (clickedShortcut != null) {
                // アイコンドラッグを開始
                startDragging(clickedShortcut, event.getStartX(), event.getStartY());
                System.out.println("HomeScreen: Started icon drag for " + clickedShortcut.getDisplayName());
                return true; // アイコンドラッグが優先される
            }
        }
        
        return false; // ページスワイプ用のドラッグ処理は handleDragMove で実装
    }
    
    /**
     * ドラッグ移動ジェスチャーを処理する。
     * 
     * @param event ジェスチャーイベント
     * @return 処理した場合true
     */
    private boolean handleDragMove(GestureEvent event) {
        HomePage currentPage = getCurrentPage();
        if (currentPage != null && currentPage.isAppLibraryPage()) {
            // AppLibraryページでのドラッグはスクロール
            return handleAppLibraryScroll(event);
        }
        
        // アイコンドラッグが進行中の場合は、それを優先
        if (isDragging && draggedShortcut != null) {
            int dragX = event.getCurrentX() - dragOffsetX;
            int dragY = event.getCurrentY() - dragOffsetY;

            // ドラッグ座標の境界チェックと調整
            dragX = constrainDragPosition(dragX, dragY)[0];
            dragY = constrainDragPosition(dragX, dragY)[1];

            draggedShortcut.setDragPosition(dragX, dragY);
            System.out.println("HomeScreen: Updating icon drag position to (" + dragX + ", " + dragY + ")");

            // 画面端での自動ページスライドを実装
            handleEdgeAutoSlide(event.getCurrentX(), event.getCurrentY());

            return true; // アイコンドラッグが優先
        }
        
        // 編集モードでもページスワイプを有効化（ショートカットドラッグ中は無効）
        if (isEditing && isDragging) {
            return false; // ショートカットドラッグ中はページドラッグを無効
        }

        // 通常モードおよび編集モード（ショートカットドラッグ中以外）でページ切り替えドラッグを処理
        return handlePageDrag(event);
    }
    
    /**
     * ページドラッグを処理する（リアルタイムページ移動）。
     * 
     * @param event ジェスチャーイベント
     * @return 処理した場合true
     */
    private boolean handlePageDrag(GestureEvent event) {
        if (isAnimating) {
            return false; // アニメーション中はドラッグを無視
        }
        
        int deltaX = event.getCurrentX() - event.getStartX();
        
        // 水平ドラッグのみをページ移動として扱う
        if (Math.abs(deltaX) > 10) { // 10px以上のドラッグで反応
            // ページオフセットを計算（画面幅の範囲内で制限）
            pageTransitionOffset = Math.max(-400, Math.min(400, deltaX));
            
            // 端ページでの制限
            if (currentPageIndex == 0 && pageTransitionOffset > 0) {
                pageTransitionOffset *= 0.3f; // バウンス効果
            } else if (currentPageIndex == homePages.size() - 1 && pageTransitionOffset < 0) {
                pageTransitionOffset *= 0.3f; // バウンス効果
            }
            
            return true;
        }
        
        return false;
    }
    
    /**
     * ドラッグ終了ジェスチャーを処理する。
     * 
     * @param event ジェスチャーイベント
     * @return 処理した場合true
     */
    private boolean handleDragEnd(GestureEvent event) {
        HomePage currentPage = getCurrentPage();
        if (currentPage != null && currentPage.isAppLibraryPage()) {
            // AppLibraryページでのドラッグ終了はスクロール終了
            return handleAppLibraryScrollEnd(event);
        }
        
        // アイコンドラッグの終了処理
        if (isDragging && draggedShortcut != null) {
            System.out.println("HomeScreen: Ending icon drag");
            handleShortcutDrop(event.getCurrentX(), event.getCurrentY());

            // 画面端スライド状態をリセット
            resetEdgeSlideState();

            return true;
        }
        
        // 編集モードでもページスワイプを有効化（ショートカットドラッグ中は無効）
        if (isEditing && isDragging) {
            return false; // ショートカットドラッグ中はページドラッグ終了を無効
        }

        // 通常モードおよび編集モード（ショートカットドラッグ中以外）でページドラッグ終了を処理
        return handlePageDragEnd(event);
    }
    
    /**
     * ページドラッグ終了を処理する。
     * 
     * @param event ジェスチャーイベント
     * @return 処理した場合true
     */
    private boolean handlePageDragEnd(GestureEvent event) {
        if (Math.abs(pageTransitionOffset) < 50) {
            // ドラッグ距離が短い場合は元のページに戻る
            startReturnToCurrentPage();
            return true;
        }
        
        // ドラッグ距離が十分な場合はページ切り替え
        if (pageTransitionOffset > 50 && currentPageIndex > 0) {
            // 右にドラッグ - 前のページ
            startPageTransition(currentPageIndex - 1);
            return true;
        } else if (pageTransitionOffset < -50 && currentPageIndex < homePages.size() - 1) {
            // 左にドラッグ - 次のページ
            startPageTransition(currentPageIndex + 1);
            return true;
        } else {
            // 端ページまたは条件を満たさない場合は元に戻る
            startReturnToCurrentPage();
            return true;
        }
    }
    
    /**
     * 現在のページに戻るアニメーションを開始する。
     */
    private void startReturnToCurrentPage() {
        targetPageIndex = currentPageIndex;
        isAnimating = true;
        animationStartTime = System.currentTimeMillis();
        animationProgress = 0.0f;
        startOffset = pageTransitionOffset; // 現在のオフセットを保存
        animationBasePageIndex = currentPageIndex; // 座標計算の基準ページを固定
        System.out.println("🎬 Starting return animation to current page " + currentPageIndex + ", startOffset=" + startOffset + ", basePageIndex=" + animationBasePageIndex);
    }
    
    /**
     * AppLibraryページでのスクロール開始を処理する。
     * 
     * @param event ジェスチャーイベント
     * @return 処理した場合true
     */
    private boolean handleAppLibraryScrollStart(GestureEvent event) {
        System.out.println("HomeScreen: AppLibrary scroll started");
        return true; // スクロール開始を受け入れる
    }
    
    /**
     * AppLibraryページでのスクロールを処理する。
     * 
     * @param event ジェスチャーイベント
     * @return 処理した場合true
     */
    private boolean handleAppLibraryScroll(GestureEvent event) {
        HomePage currentPage = getCurrentPage();
        if (currentPage == null) return false;
        
        // 垂直ドラッグのみをスクロールとして扱う
        int deltaY = event.getCurrentY() - event.getStartY();
        
        // 現在のスクロールオフセットを調整
        int currentScrollOffset = currentPage.getScrollOffset();
        int newScrollOffset = currentScrollOffset - deltaY; // 下方向ドラッグで上方向スクロール
        
        // スクロール範囲を制限
        int startY = 110;
        int listHeight = 600 - startY - NAV_AREA_HEIGHT - 20;
        int itemHeight = 70;
        List<IApplication> apps = currentPage.getAllApplications();
        int maxScrollOffset = Math.max(0, apps.size() * itemHeight - listHeight);
        
        newScrollOffset = Math.max(0, Math.min(maxScrollOffset, newScrollOffset));
        currentPage.setScrollOffset(newScrollOffset);
        
        System.out.println("HomeScreen: AppLibrary scrolled to offset " + newScrollOffset);
        return true;
    }
    
    /**
     * AppLibraryページでのスクロール終了を処理する。
     *
     * @param event ジェスチャーイベント
     * @return 処理した場合true
     */
    private boolean handleAppLibraryScrollEnd(GestureEvent event) {
        System.out.println("HomeScreen: AppLibrary scroll ended");
        return true;
    }

    // Edge auto-slide functionality variables
    private long edgeSlideTimer = 0;
    private boolean isEdgeSliding = false;
    private static final int EDGE_SLIDE_ZONE = 30; // ピクセル数での端検出ゾーン
    private static final long EDGE_SLIDE_DELAY = 500; // ミリ秒での自動スライド遅延
    private static final int SCREEN_WIDTH = 400; // 画面幅 (HomeScreenの標準幅)

    /**
     * ドラッグ中のショートカットが画面端にある場合、自動的にページスライドを実行する。
     *
     * @param currentX 現在のマウス/タッチのX座標
     * @param currentY 現在のマウス/タッチのY座標
     */
    private void handleEdgeAutoSlide(int currentX, int currentY) {
        if (isAnimating) {
            return; // すでにアニメーション中の場合は何もしない
        }

        // 最後のドラッグ座標を記録（継続的なチェック用）
        lastDragX = currentX;
        lastDragY = currentY;

        long currentTime = System.currentTimeMillis();
        boolean inLeftEdge = currentX < EDGE_SLIDE_ZONE;
        boolean inRightEdge = currentX > (SCREEN_WIDTH - EDGE_SLIDE_ZONE);

        // 画面端に入ったかチェック
        if (inLeftEdge || inRightEdge) {
            if (!isEdgeSliding) {
                // 初回の端検出
                isEdgeSliding = true;
                edgeSlideTimer = currentTime;
                System.out.println("HomeScreen: [Move] Edge slide zone entered at X=" + currentX +
                                 (inLeftEdge ? " (LEFT)" : " (RIGHT)") + " - Timer started");
            } else {
                // 既に端にいる場合は経過時間を表示
                long elapsed = currentTime - edgeSlideTimer;
                System.out.println("HomeScreen: [Move] Still in edge zone at X=" + currentX +
                                 " - Elapsed: " + elapsed + "ms / " + EDGE_SLIDE_DELAY + "ms");

                if (elapsed >= EDGE_SLIDE_DELAY) {
                    // 十分な時間が経過したので自動スライドを実行
                    if (inLeftEdge && currentPageIndex > 0) {
                        // 左端なので前のページに移動
                        System.out.println("HomeScreen: [Move] Auto-sliding to previous page (LEFT edge)");
                        slideToPage(currentPageIndex - 1, true);
                        resetEdgeSlideState();
                    } else if (inRightEdge && currentPageIndex < homePages.size() - 1) {
                        // 右端なので次のページに移動
                        System.out.println("HomeScreen: [Move] Auto-sliding to next page (RIGHT edge)");
                        slideToPage(currentPageIndex + 1, true);
                        resetEdgeSlideState();
                    } else {
                        // 端ページの場合は何もしない
                        System.out.println("HomeScreen: [Move] Already at edge page, no auto-slide");
                        resetEdgeSlideState();
                    }
                }
            }
        } else {
            // 画面端を離れたのでリセット
            if (isEdgeSliding) {
                System.out.println("HomeScreen: [Move] Left edge slide zone at X=" + currentX + " - Timer reset");
                resetEdgeSlideState();
            }
        }
    }

    /**
     * 画面端スライドの状態をリセットする。
     */
    private void resetEdgeSlideState() {
        isEdgeSliding = false;
        edgeSlideTimer = 0;
    }

    // 最後に記録したマウス/タッチ座標（継続的なエッジチェック用）
    private int lastDragX = 0;
    private int lastDragY = 0;

    // 遅延ドロップ処理用の変数
    private boolean hasPendingDrop = false;
    private int pendingDropX = 0;
    private int pendingDropY = 0;
    private Shortcut pendingDropShortcut = null;

    /**
     * 描画ループ中にエッジ自動スライドのタイマーを継続的にチェックする。
     * ドラッグ中で画面端に滞在している場合、時間経過で自動スライドを実行する。
     */
    private void updateEdgeAutoSlideTimer() {
        // ドラッグ中かつエッジスライド中の場合のみチェック
        if (!isDragging || !isEdgeSliding || draggedShortcut == null || isAnimating) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        boolean inLeftEdge = lastDragX < EDGE_SLIDE_ZONE;
        boolean inRightEdge = lastDragX > (SCREEN_WIDTH - EDGE_SLIDE_ZONE);

        // 画面端に滞在している場合のみ継続チェック
        if ((inLeftEdge || inRightEdge) && currentTime - edgeSlideTimer >= EDGE_SLIDE_DELAY) {
            System.out.println("HomeScreen: [Timer] Edge auto-slide triggered at X=" + lastDragX +
                             " after " + (currentTime - edgeSlideTimer) + "ms");

            if (inLeftEdge && currentPageIndex > 0) {
                // 左端なので前のページに移動
                System.out.println("HomeScreen: [Timer] Auto-sliding to previous page (LEFT edge)");
                slideToPage(currentPageIndex - 1, true);
                resetEdgeSlideState();
            } else if (inRightEdge && currentPageIndex < homePages.size() - 1) {
                // 右端なので次のページに移動
                System.out.println("HomeScreen: [Timer] Auto-sliding to next page (RIGHT edge)");
                slideToPage(currentPageIndex + 1, true);
                resetEdgeSlideState();
            } else {
                // 端ページの場合は何もしない
                System.out.println("HomeScreen: [Timer] Already at edge page, no auto-slide");
                resetEdgeSlideState();
            }
        }
    }

    /**
     * 指定したページにスライドする。
     * ドラッグ継続中でも呼び出せるようにする。
     *
     * @param pageIndex 移動先のページインデックス
     * @param maintainDrag ドラッグ状態を維持するかどうか
     */
    private void slideToPage(int pageIndex, boolean maintainDrag) {
        if (pageIndex < 0 || pageIndex >= homePages.size() || pageIndex == currentPageIndex) {
            return;
        }

        // ドラッグ中のショートカットの情報を保存
        Shortcut savedDraggedShortcut = null;
        int savedDragOffsetX = 0, savedDragOffsetY = 0;
        boolean wasDragging = isDragging && draggedShortcut != null;

        if (wasDragging && maintainDrag) {
            savedDraggedShortcut = draggedShortcut;
            savedDragOffsetX = dragOffsetX;
            savedDragOffsetY = dragOffsetY;
            System.out.println("HomeScreen: Saving drag state for shortcut: " + savedDraggedShortcut.getDisplayName());
        }

        // ページ切り替えを実行
        currentPageIndex = pageIndex;
        targetPageIndex = pageIndex;
        isAnimating = true;
        animationStartTime = System.currentTimeMillis();
        animationProgress = 0.0f;
        pageTransitionOffset = 0.0f;
        startOffset = 0.0f;
        animationBasePageIndex = pageIndex;

        System.out.println("HomeScreen: Sliding to page " + pageIndex + " (maintainDrag=" + maintainDrag + ")");

        // ドラッグ状態を復元
        if (wasDragging && maintainDrag && savedDraggedShortcut != null) {
            draggedShortcut = savedDraggedShortcut;
            dragOffsetX = savedDragOffsetX;
            dragOffsetY = savedDragOffsetY;
            isDragging = true;

            // ページ切り替え後にドラッグ位置を画面内の安全な場所に調整
            adjustDragPositionAfterSlide();

            System.out.println("HomeScreen: Restored drag state for shortcut: " + draggedShortcut.getDisplayName());
        }
    }

    /**
     * ページスライド後のドラッグ位置を画面内の安全な場所に調整する。
     * 画面端でスライドが発生した場合、ショートカットを画面内の適切な位置に配置する。
     */
    private void adjustDragPositionAfterSlide() {
        if (draggedShortcut == null) {
            return;
        }

        // 現在のドラッグ位置を取得
        float currentDragX = draggedShortcut.getDragX();
        float currentDragY = draggedShortcut.getDragY();

        // 画面境界
        final int MARGIN = 10; // 画面端からの安全マージン
        final int MIN_X = MARGIN;
        final int MAX_X = SCREEN_WIDTH - ICON_SIZE - MARGIN;
        final int MIN_Y = 80; // ステータスバー下
        final int MAX_Y = 600 - NAV_AREA_HEIGHT - ICON_SIZE - MARGIN; // ナビゲーションエリア上

        float adjustedX = currentDragX;
        float adjustedY = currentDragY;

        // 左端からのスライドの場合、右側の安全な位置に移動
        if (currentDragX < EDGE_SLIDE_ZONE) {
            adjustedX = EDGE_SLIDE_ZONE + 20; // 端検出ゾーンから少し内側
            System.out.println("HomeScreen: Adjusting drag X from " + currentDragX + " to " + adjustedX + " (left edge slide)");
        }
        // 右端からのスライドの場合、左側の安全な位置に移動
        else if (currentDragX > (SCREEN_WIDTH - EDGE_SLIDE_ZONE)) {
            adjustedX = SCREEN_WIDTH - EDGE_SLIDE_ZONE - 20; // 端検出ゾーンから少し内側
            System.out.println("HomeScreen: Adjusting drag X from " + currentDragX + " to " + adjustedX + " (right edge slide)");
        }

        // Y座標の境界チェック
        if (adjustedY < MIN_Y) {
            adjustedY = MIN_Y;
            System.out.println("HomeScreen: Adjusting drag Y from " + currentDragY + " to " + adjustedY + " (top boundary)");
        } else if (adjustedY > MAX_Y) {
            adjustedY = MAX_Y;
            System.out.println("HomeScreen: Adjusting drag Y from " + currentDragY + " to " + adjustedY + " (bottom boundary)");
        }

        // X座標の最終境界チェック（念のため）
        if (adjustedX < MIN_X) {
            adjustedX = MIN_X;
            System.out.println("HomeScreen: Final X adjustment from " + currentDragX + " to " + adjustedX + " (left boundary)");
        } else if (adjustedX > MAX_X) {
            adjustedX = MAX_X;
            System.out.println("HomeScreen: Final X adjustment from " + currentDragX + " to " + adjustedX + " (right boundary)");
        }

        // 調整された座標を設定
        draggedShortcut.setDragPosition((int)adjustedX, (int)adjustedY);

        // lastDragX/Yも更新（継続的なエッジチェック用）
        lastDragX = (int)adjustedX;
        lastDragY = (int)adjustedY;

        System.out.println("HomeScreen: Drag position adjusted to (" + (int)adjustedX + ", " + (int)adjustedY + ")");
    }

    /**
     * ドラッグ座標を画面境界内に制限する。
     *
     * @param dragX 元のドラッグX座標
     * @param dragY 元のドラッグY座標
     * @return 制限後の座標 [adjustedX, adjustedY]
     */
    private int[] constrainDragPosition(int dragX, int dragY) {
        // 画面境界（通常のドラッグ用）
        final int MIN_X = -10; // 少し画面外まで許可（エッジ検出のため）
        final int MAX_X = SCREEN_WIDTH + 10; // 少し画面外まで許可（エッジ検出のため）
        final int MIN_Y = 80; // ステータスバー下
        final int MAX_Y = 600 - NAV_AREA_HEIGHT - 10; // ナビゲーションエリア上

        int adjustedX = Math.max(MIN_X, Math.min(MAX_X, dragX));
        int adjustedY = Math.max(MIN_Y, Math.min(MAX_Y, dragY));

        return new int[]{adjustedX, adjustedY};
    }

    /**
     * 全ページからショートカットを削除する（ページ間移動時の重複防止）。
     *
     * @param shortcut 削除するショートカット
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
     * アニメーション中にドロップが発生した場合、アニメーション完了後に実行するようスケジュールする。
     *
     * @param mouseX ドロップのX座標
     * @param mouseY ドロップのY座標
     */
    private void scheduleDelayedDrop(int mouseX, int mouseY) {
        hasPendingDrop = true;
        pendingDropX = mouseX;
        pendingDropY = mouseY;
        pendingDropShortcut = draggedShortcut;

        System.out.println("HomeScreen: [DROP] Scheduled delayed drop for shortcut '" +
                          (pendingDropShortcut != null ? pendingDropShortcut.getDisplayName() : "null") +
                          "' at (" + mouseX + ", " + mouseY + ")");

        // ドラッグ状態をいったんクリア（ただし、遅延ドロップのためにショートカット情報は保持）
        isDragging = false;
    }

    /**
     * アニメーション完了後に遅延されたドロップを実行する。
     */
    private void executePendingDrop() {
        if (hasPendingDrop && pendingDropShortcut != null) {
            System.out.println("HomeScreen: [DROP] Executing pending drop for shortcut '" +
                              pendingDropShortcut.getDisplayName() + "' at (" + pendingDropX + ", " + pendingDropY + ")");

            // 遅延ドロップの実行
            executeDrop(pendingDropX, pendingDropY, pendingDropShortcut);

            // 遅延ドロップ状態をリセット
            hasPendingDrop = false;
            pendingDropX = 0;
            pendingDropY = 0;
            pendingDropShortcut = null;
        }
    }

    /**
     * 実際のドロップ処理を実行する（即座実行と遅延実行で共通）。
     *
     * @param mouseX ドロップのX座標
     * @param mouseY ドロップのY座標
     * @param shortcut ドロップするショートカット
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

                // 安全な配置処理：先に配置を試行し、成功した場合のみ他のページから削除
                boolean placed = safelyPlaceShortcut(shortcut, targetPage, gridPos[0], gridPos[1]);

                if (placed) {
                    System.out.println("HomeScreen: [EXECUTE] ショートカット '" + shortcut.getDisplayName() +
                                     "' をページ " + currentPageIndex + " の (" + gridPos[0] + ", " + gridPos[1] + ") に配置しました");
                    saveCurrentLayout();
                } else {
                    System.out.println("HomeScreen: [EXECUTE] ショートカット配置失敗 - フォールバック処理を実行");

                    // 配置失敗時は最初の空きスロットに配置
                    int[] emptySlot = findFirstEmptySlot(targetPage);
                    if (emptySlot != null && safelyPlaceShortcut(shortcut, targetPage, emptySlot[0], emptySlot[1])) {
                        System.out.println("HomeScreen: [EXECUTE] フォールバック: 空きスロット (" + emptySlot[0] + ", " + emptySlot[1] + ") に配置しました");
                        saveCurrentLayout();
                    } else {
                        System.out.println("HomeScreen: [EXECUTE] エラー: 配置可能な空きスロットがありません - ショートカットを元の場所に戻します");
                        // 最悪の場合は元の場所に戻す（削除を防ぐ）
                        restoreShortcutToSafePage(shortcut);
                    }
                }
            }
        }

        // Reset drag state
        resetDragState();
    }

    /**
     * ショートカットを安全に配置する。
     * 他のページから削除する前に、まず目標ページに配置できることを確認する。
     *
     * @param shortcut 配置するショートカット
     * @param targetPage 目標ページ
     * @param gridX 目標グリッドX座標
     * @param gridY 目標グリッドY座標
     * @return 配置に成功した場合true
     */
    private boolean safelyPlaceShortcut(Shortcut shortcut, HomePage targetPage, int gridX, int gridY) {
        if (shortcut == null || targetPage == null) {
            return false;
        }

        System.out.println("HomeScreen: [SAFE_PLACE] Attempting to place shortcut '" + shortcut.getDisplayName() +
                          "' at (" + gridX + ", " + gridY + ") on page " + currentPageIndex);

        // ショートカットが既にターゲットページにある場合は通常のmoveShortcutを使用
        if (targetPage.getShortcuts().contains(shortcut)) {
            System.out.println("HomeScreen: [SAFE_PLACE] Shortcut already on target page, using moveShortcut");
            return targetPage.moveShortcut(shortcut, gridX, gridY);
        }

        // ショートカットが他のページにある場合
        // 1. まず、目標位置が空いているかチェック
        if (!targetPage.isPositionEmpty(gridX, gridY)) {
            System.out.println("HomeScreen: [SAFE_PLACE] Target position is occupied");
            return false;
        }

        // 2. 他のページから削除
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

        // 3. ターゲットページに追加
        boolean added = targetPage.addShortcut(shortcut, gridX, gridY);
        if (!added) {
            System.out.println("HomeScreen: [SAFE_PLACE] Failed to add to target page - restoring to source page");
            // 追加に失敗した場合は元のページに戻す
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
     * ショートカットを安全な場所に復元する（配置失敗時のフォールバック）。
     *
     * @param shortcut 復元するショートカット
     */
    private void restoreShortcutToSafePage(Shortcut shortcut) {
        if (shortcut == null) return;

        System.out.println("HomeScreen: [RESTORE] Restoring shortcut '" + shortcut.getDisplayName() + "' to safe page");

        // 最初のページで空きスロットを探す
        for (HomePage page : homePages) {
            if (page.isAppLibraryPage()) continue; // AppLibraryページはスキップ

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
