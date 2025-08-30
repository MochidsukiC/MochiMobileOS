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
    private static final int ICON_SIZE = 64;
    private static final int ICON_SPACING = 20;
    
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
     */
    @Override
    public void setup() {
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
     */
    @Override
    public void draw(PApplet p) {
        // Debug: Log first few draw calls
        if (p.frameCount <= 3) {
            System.out.println("🎨 HomeScreen: Drawing frame " + p.frameCount + " - initialized: " + isInitialized);
        }
        
        try {
            // Draw background
            if (backgroundImage != null) {
                // 背景画像を画面サイズに合わせて描画
                p.image(backgroundImage, 0, 0, 400, 600);
            } else {
                // 画像がない場合はカラー背景
                p.background(30, 30, 30);
            }
            
            // Update page transition animation
            updatePageAnimation();
            
            // Draw status bar
            drawStatusBar(p);
            
            // Draw pages with transition animation
            drawPagesWithTransition(p);
            
            // Draw navigation area
            drawNavigationArea(p);
            
            // Draw page indicator dots
            drawPageIndicators(p);
            
        } catch (Exception e) {
            System.err.println("❌ HomeScreen: Draw error - " + e.getMessage());
            e.printStackTrace();
            // Fallback drawing
            p.background(255, 0, 0);
            p.fill(255);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text("HomeScreen Error: " + e.getMessage(), p.width/2, p.height/2);
        }
    }
    
    /**
     * Handles mouse press events on the home screen.
     * Processes app shortcut clicks and navigation gestures.
     * 
     * @param mouseX The x-coordinate of the mouse press
     * @param mouseY The y-coordinate of the mouse press
     */
    @Override
    public void mousePressed(int mouseX, int mouseY) {
        System.out.println("HomeScreen: Touch at (" + mouseX + ", " + mouseY + ")");
        
        touchStartTime = System.currentTimeMillis();
        longPressTriggered = false;
        swipeStartX = mouseX;
        isSwipingPages = false;
        
        // Check if click is in navigation area (swipe to app library)
        if (mouseY > (600 - NAV_AREA_HEIGHT)) {
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
                // Normal mode - launch app or detect long press for edit mode
                launchApplication(clickedShortcut.getApplication());
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
        int iconY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 15);
        
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
        int iconY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 15);
        
        // 画面上での削除ボタン位置を計算（座標変換を考慮）
        float screenDeleteX = totalOffset + shortcutPageIndex * 400 + iconX + ICON_SIZE - 8;
        float screenDeleteY = iconY + 8;
        
        return Math.sqrt((mouseX - screenDeleteX) * (mouseX - screenDeleteX) + (mouseY - screenDeleteY) * (mouseY - screenDeleteY)) <= 8;
    }
    
    /**
     * Handles mouse drag events for shortcut dragging and page swiping.
     * Note: このメソッドは後方互換性のために残されていますが、
     * 実際の処理は GestureManager システムで行われます。
     */
    public void mouseDragged(int mouseX, int mouseY) {
        // GestureManagerシステムが有効な場合は何もしない
        // 実際のドラッグ処理は onGesture -> handleDragMove で実行される
        System.out.println("HomeScreen: mouseDragged called - delegating to GestureManager");
    }
    
    /**
     * Handles mouse release events.
     * Note: このメソッドは後方互換性のために残されていますが、
     * 実際の処理は GestureManager システムで行われます。
     */
    public void mouseReleased(int mouseX, int mouseY) {
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
            int shortcutY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 15);
            
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
            int shortcutY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 15);
            
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
        
        // Calculate target grid position
        int[] gridPos = screenToGridPosition(mouseX, mouseY);
        if (gridPos != null) {
            HomePage currentPage = getCurrentPage();
            if (currentPage != null) {
                // Try to move to new position
                if (currentPage.moveShortcut(draggedShortcut, gridPos[0], gridPos[1])) {
                    System.out.println("HomeScreen: ショートカットを移動しました (" + gridPos[0] + ", " + gridPos[1] + ")");
                    
                    // レイアウトを自動保存
                    saveCurrentLayout();
                } else {
                    System.out.println("HomeScreen: ショートカット移動失敗 - 位置が占有済みか無効");
                }
            }
        }
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
        int gridY = (screenY - startY) / (ICON_SIZE + ICON_SPACING + 15);
        
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
     */
    @Override
    public void cleanup() {
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
            // Processingのコンテキストを使って画像を読み込み
            if (kernel instanceof processing.core.PApplet) {
                processing.core.PApplet p = (processing.core.PApplet) kernel;
                
                // 複数のパスを試す（Processingの仕様に合わせて調整）
                String[] imagePaths = {
                    "data/wallpaper.jpg",           // Processingの標準dataフォルダ
                    "resources/wallpaper.jpg",      // resourcesフォルダ
                    "resources/wallpaper.png", 
                    "wallpaper.jpg",                // ルートディレクトリ
                    "../resources/wallpaper.jpg",   // 相対パス
                    "core/src/resources/settings/personalSettings/backGround/blue.png"  // 既存画像
                };
                
                // 現在の作業ディレクトリを表示（デバッグ用）
                System.out.println("HomeScreen: Current working directory: " + System.getProperty("user.dir"));
                
                for (String path : imagePaths) {
                    System.out.println("HomeScreen: Trying to load background image from: " + path);
                    try {
                        backgroundImage = p.loadImage(path);
                        if (backgroundImage != null) {
                            System.out.println("HomeScreen: ✅ Background image loaded successfully from: " + path);
                            System.out.println("HomeScreen: Image dimensions: " + backgroundImage.width + "x" + backgroundImage.height);
                            break;
                        } else {
                            System.out.println("HomeScreen: ❌ Failed to load from: " + path + " (image is null)");
                        }
                    } catch (Exception pathException) {
                        System.out.println("HomeScreen: ❌ Exception loading from: " + path + " - " + pathException.getMessage());
                    }
                }
                
                if (backgroundImage == null) {
                    System.out.println("HomeScreen: Failed to load background image from all paths, using color fallback");
                }
            }
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
    private void drawStatusBar(PApplet p) {
        try {
            p.fill(textColor, 180); // Semi-transparent text
            p.textAlign(p.LEFT, p.TOP);
            p.textSize(12);
            
            // Current time
            if (kernel != null && kernel.getSystemClock() != null) {
                try {
                    p.text(kernel.getSystemClock().getFormattedTime(), 15, 15);
                } catch (Exception e) {
                    p.text("--:--", 15, 15);
                }
            } else {
                p.text("No Clock", 15, 15);
            }
            
            // System status and current page info
            p.textAlign(p.RIGHT, p.TOP);
            p.text("MochiOS", 385, 15);
            
            // Current page name
            if (!homePages.isEmpty() && currentPageIndex < homePages.size()) {
                HomePage currentPage = homePages.get(currentPageIndex);
                String pageName = currentPage.isAppLibraryPage() ? "App Library" : 
                                 currentPage.getPageName() != null ? currentPage.getPageName() : 
                                 "Page " + (currentPageIndex + 1);
                                 
                p.fill(255, 255, 255, 150);
                p.textAlign(p.CENTER, p.TOP);
                p.textSize(11);
                p.text(pageName, 200, 15);
            }
            
            // Status indicator
            if (isInitialized) {
                p.fill(76, 175, 80); // Green if ready (0x4CAF50 -> RGB)
            } else {
                p.fill(255, 152, 0); // Orange if not (0xFF9800 -> RGB)
            }
            p.noStroke();
            p.ellipse(370, 20, 8, 8);
            
        } catch (Exception e) {
            System.err.println("Error in drawStatusBar: " + e.getMessage());
            // Fallback: just draw a simple status
            p.fill(255);
            p.textAlign(p.LEFT, p.TOP);
            p.textSize(12);
            p.text("Status Error", 15, 15);
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
    private void drawPagesWithTransition(PApplet p) {
        if (homePages.isEmpty()) {
            // No pages, show message
            p.fill(255, 255, 255, 150);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text("No apps installed", 200, 300);
            p.textSize(12);
            p.text("Swipe up to access app library", 200, 320);
            return;
        }
        
        // 座標変換でページ切り替えアニメーションを実現
        p.pushMatrix();
        
        // ページ全体のオフセットを適用
        // アニメーション中は基準ページ（animationBasePageIndex）を使用してジャンプを防ぐ
        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        p.translate(totalOffset, 0);
        
        if (isAnimating) {
            System.out.println("🎨 Drawing with basePageIndex=" + basePageForOffset + ", pageTransitionOffset=" + pageTransitionOffset + ", totalOffset=" + totalOffset);
        }
        
        // 全ページを横に並べて描画
        for (int i = 0; i < homePages.size(); i++) {
            p.pushMatrix();
            p.translate(i * 400, 0); // 各ページを400px間隔で配置
            
            HomePage page = homePages.get(i);
            if (page.isAppLibraryPage()) {
                drawAppLibraryPage(p, page);
            } else {
                drawNormalPage(p, page);
            }
            
            p.popMatrix();
        }
        
        p.popMatrix();
        
        // ドラッグ中のアイコンを最上位レイヤー（変換なし）で描画
        if (isDragging && draggedShortcut != null) {
            drawDraggedShortcut(p, draggedShortcut);
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
    private void drawNormalPage(PApplet p, HomePage page) {
        // 通常のページ描画処理
        int startY = 80; // Below status bar
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2; // Center the grid
        
        p.textAlign(p.CENTER, p.TOP);
        p.textSize(10);
        
        // First draw non-dragged shortcuts
        for (Shortcut shortcut : page.getShortcuts()) {
            if (shortcut.isDragging()) continue; // Draw dragged shortcuts last
            
            int x = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
            int y = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 15); // Extra space for app name
            
            // Apply wiggle animation if in edit mode
            if (isEditing) {
                x += (int) (Math.sin(System.currentTimeMillis() * 0.01 + shortcut.getShortcutId().hashCode()) * 2);
                y += (int) (Math.cos(System.currentTimeMillis() * 0.012 + shortcut.getShortcutId().hashCode()) * 1.5);
            }
            
            // Draw shortcut
            drawShortcut(p, shortcut, x, y);
        }
        
        // ドラッグ中のショートカットは最上位レイヤーで描画するため、ここではスキップ
        // (drawPagesWithTransitionの最後で描画される)
        
        // Draw drop target indicators if dragging
        if (isDragging) {
            drawDropTargets(p, startX, startY);
        }
    }
    
    
    /**
     * AppLibraryページを描画する。
     * 
     * @param p The PApplet instance for drawing
     * @param appLibraryPage AppLibraryページ
     */
    private void drawAppLibraryPage(PApplet p, HomePage appLibraryPage) {
        System.out.println("🎨 HomeScreen: drawAppLibraryPage() called - drawing AppLibrary background and title");
        
        // 画面全体に背景を描画（カーネルの緑色を上書き）
        p.fill(42, 42, 42); // ダークグレーの背景 (0x2A2A2A -> RGB)
        p.noStroke();
        p.rect(0, 0, 400, 600); // 画面全体を覆う
        System.out.println("🎨 Background rect drawn: (0,0,400,600) with color RGB(42,42,42)");
        
        // AppLibraryタイトルを描画
        p.fill(255, 255, 255); // 白色テキスト (0xFFFFFF -> RGB)
        p.textAlign(p.CENTER, p.TOP);
        p.textSize(18);
        System.out.println("🎨 Drawing title: 'App Library' at (200, 70) with size 18, color RGB(255,255,255)");
        p.text("App Library", 200, 70);
        System.out.println("🎨 Title drawing completed");

        // アプリリストを描画
        List<IApplication> apps = appLibraryPage.getAllApplications();
        System.out.println("🎨 AppLibrary apps count: " + apps.size());
        if (apps.isEmpty()) {
            p.fill(255, 255, 255, 150); // textColor with alpha -> RGB
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(14);
            p.text("No apps available", 200, 300);
            System.out.println("🎨 'No apps available' message drawn at (200, 300)");
            return;
        }
        
        int startY = 110; // タイトルの下から開始
        int listHeight = 600 - startY - NAV_AREA_HEIGHT - 20; // 利用可能な高さ
        int itemHeight = 70; // 各アプリアイテムの高さ
        int scrollOffset = appLibraryPage.getScrollOffset();
        System.out.println("🎨 Drawing " + apps.size() + " apps starting at Y=" + startY + ", scrollOffset=" + scrollOffset);
        
        // スクロール可能エリアを設定（クリッピング）
        p.pushMatrix();
        
        // アプリリストを描画
        for (int i = 0; i < apps.size(); i++) {
            IApplication app = apps.get(i);
            int itemY = startY + i * itemHeight - scrollOffset;
            
            // 表示エリア外のアイテムはスキップ
            if (itemY + itemHeight < startY || itemY > startY + listHeight) {
                continue;
            }
            
            drawAppLibraryItem(p, app, 20, itemY, 360, itemHeight);
        }
        
        p.popMatrix();
        
        // スクロールインジケーターを描画
        if (appLibraryPage.needsScrolling(listHeight, itemHeight)) {
            drawScrollIndicator(p, appLibraryPage, startY, listHeight, itemHeight);
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
    private void drawAppLibraryItem(PApplet p, IApplication app, int x, int y, int width, int height) {
        // アイテムの背景
        p.fill(58, 58, 58, 100); // 0x3A3A3A -> RGB with alpha
        p.noStroke();
        p.rect(x, y, width, height, 8);
        
        // アプリアイコン
        p.fill(74, 144, 226); // accentColor (0x4A90E2) -> RGB
        p.rect(x + 10, y + 10, 50, 50, 8);
        
        // アプリ名の最初の文字
        p.fill(255, 255, 255); // textColor -> RGB
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(24);
        String initial = app.getName().substring(0, 1).toUpperCase();
        p.text(initial, x + 35, y + 35);
        
        // アプリ名
        p.fill(255, 255, 255); // textColor -> RGB
        p.textAlign(p.LEFT, p.CENTER);
        p.textSize(16);
        p.text(app.getName(), x + 75, y + 25);
        
        // アプリ説明（あれば）
        if (app.getDescription() != null && !app.getDescription().isEmpty()) {
            p.fill(255, 255, 255, 150); // textColor with alpha -> RGB
            p.textSize(12);
            String description = app.getDescription();
            if (description.length() > 40) {
                description = description.substring(0, 37) + "...";
            }
            p.text(description, x + 75, y + 45);
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
    private void drawScrollIndicator(PApplet p, HomePage appLibraryPage, int listStartY, int listHeight, int itemHeight) {
        List<IApplication> apps = appLibraryPage.getAllApplications();
        int totalHeight = apps.size() * itemHeight;
        int scrollOffset = appLibraryPage.getScrollOffset();
        
        // スクロールバーの位置とサイズを計算
        float scrollbarHeight = Math.max(20, (float) listHeight * listHeight / totalHeight);
        float scrollbarY = listStartY + (float) scrollOffset * listHeight / totalHeight;
        
        // スクロールバーを描画
        p.fill(255, 255, 255, 100); // textColor with alpha -> RGB
        p.noStroke();
        p.rect(385, (int) scrollbarY, 6, (int) scrollbarHeight, 3);
    }
    
    /**
     * ドラッグ中のショートカットを絶対座標で描画する。
     * 座標変換の影響を受けずにマウス位置に正確に追従する。
     * 
     * @param p The PApplet instance for drawing
     * @param shortcut The dragged shortcut
     */
    private void drawDraggedShortcut(PApplet p, Shortcut shortcut) {
        if (!shortcut.isDragging()) return;
        
        int x = (int) shortcut.getDragX();
        int y = (int) shortcut.getDragY();
        
        // ドロップシャドウを描画
        p.fill(0, 0, 0, 100);
        p.noStroke();
        p.rect(x + 4, y + 4, ICON_SIZE, ICON_SIZE, 12);
        
        // アイコンの背景を描画（半透明）
        p.fill(255, 255, 255, 220);
        p.stroke(85, 85, 85);
        p.strokeWeight(2);
        p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
        
        // アプリアイコンを描画
        IApplication app = shortcut.getApplication();
        if (app != null) {
            drawAppIcon(p, app, x + ICON_SIZE/2, y + ICON_SIZE/2);
        }
        
        // アプリ名を描画
        p.fill(textColor);
        p.noStroke();
        p.textAlign(p.CENTER, p.TOP);
        p.textSize(10);
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 8) {
            displayName = displayName.substring(0, 7) + "...";
        }
        p.text(displayName, x + ICON_SIZE/2, y + ICON_SIZE + 5);
    }
    
    /**
     * Draws drop target indicators during drag operation.
     * 
     * @param p The PApplet instance for drawing
     * @param startX Grid start X position
     * @param startY Grid start Y position
     */
    private void drawDropTargets(PApplet p, int startX, int startY) {
        HomePage currentPage = getCurrentPage();
        if (currentPage == null) return;
        
        p.stroke(accentColor, 150);
        p.strokeWeight(2);
        p.noFill();
        
        // Draw indicators for empty positions
        for (int gridX = 0; gridX < GRID_COLS; gridX++) {
            for (int gridY = 0; gridY < GRID_ROWS; gridY++) {
                if (currentPage.isPositionEmpty(gridX, gridY)) {
                    int x = startX + gridX * (ICON_SIZE + ICON_SPACING);
                    int y = startY + gridY * (ICON_SIZE + ICON_SPACING + 15);
                    
                    p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
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
    private void drawShortcut(PApplet p, Shortcut shortcut, int x, int y) {
        IApplication app = shortcut.getApplication();
        
        // ドラッグ中のショートカットは専用メソッドで描画されるため、ここでは描画しない
        if (shortcut.isDragging()) {
            return;
        }
        
        // Draw app icon background
        p.fill(255);
        p.stroke(0x555555);
        p.strokeWeight(1);
        p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
        
        // Draw app icon
        drawAppIcon(p, app, x + ICON_SIZE/2, y + ICON_SIZE/2);
        
        // Draw delete button if in edit mode
        if (isEditing) {
            p.fill(0xFF4444); // Red delete button
            p.noStroke();
            p.ellipse(x + ICON_SIZE - 8, y + 8, 16, 16);
            
            // Draw X
            p.fill(textColor);
            p.strokeWeight(2);
            p.stroke(textColor);
            p.line(x + ICON_SIZE - 12, y + 4, x + ICON_SIZE - 4, y + 12);
            p.line(x + ICON_SIZE - 12, y + 12, x + ICON_SIZE - 4, y + 4);
        }
        
        // Draw app name
        p.fill(textColor);
        p.noStroke();
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 8) {
            displayName = displayName.substring(0, 7) + "...";
        }
        p.text(displayName, x + ICON_SIZE/2, y + ICON_SIZE + 5);
    }
    
    /**
     * Draws an individual app icon (placeholder implementation).
     * 
     * @param p The PApplet instance for drawing
     * @param app The application to draw an icon for
     * @param centerX The center X coordinate for the icon
     * @param centerY The center Y coordinate for the icon
     */
    private void drawAppIcon(PApplet p, IApplication app, int centerX, int centerY) {
        // For now, draw a simple colored square as the app icon
        p.fill(accentColor);
        p.noStroke();
        p.rect(centerX - 20, centerY - 20, 40, 40, 8);
        
        // Draw app initial
        p.fill(textColor);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(20);
        String initial = app.getName().substring(0, 1).toUpperCase();
        p.text(initial, centerX, centerY - 2);
    }
    
    /**
     * Draws the navigation area at the bottom for accessing app library.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawNavigationArea(PApplet p) {
        int navY = 600 - NAV_AREA_HEIGHT;
        
        // Draw navigation background
        p.fill(0x2A2A2A);
        p.noStroke();
        p.rect(0, navY, 400, NAV_AREA_HEIGHT);
        
        // Draw app library access hint
        p.fill(textColor, 150);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(14);
        p.text("App Library", 200, navY + 30);
        
        // Draw edit mode toggle hint if not in edit mode
        if (!isEditing) {
            p.textSize(10);
            p.text("Long press to edit", 200, navY + 50);
        } else {
            p.textSize(10);
            p.text("Tap outside to finish editing", 200, navY + 50);
        }
        
        // Draw swipe indicator for pages if multiple pages exist
        if (homePages.size() > 1) {
            p.stroke(textColor, 100);
            p.strokeWeight(1);
            p.noFill();
            
            // Left arrow for previous page
            if (currentPageIndex > 0) {
                p.line(50, navY + 70, 40, navY + 75);
                p.line(50, navY + 70, 40, navY + 65);
            }
            
            // Right arrow for next page
            if (currentPageIndex < homePages.size() - 1) {
                p.line(350, navY + 70, 360, navY + 75);
                p.line(350, navY + 70, 360, navY + 65);
            }
        }
    }
    
    /**
     * Draws page indicator dots for multiple pages.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawPageIndicators(PApplet p) {
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
        p.fill(0, 0, 0, 100);
        p.noStroke();
        p.rect(startX - 15, dotY - 10, totalWidth + 30, 20, 10);
        
        for (int i = 0; i < homePages.size(); i++) {
            int dotX = startX + i * spacing;
            
            if (i == currentPageIndex) {
                // 現在のページ - 大きく明るく
                p.fill(74, 144, 226); // アクセントカラー (accentColor RGB)
                p.noStroke();
                p.ellipse(dotX, dotY, activeDotSize, activeDotSize);
                
                // 外側のリング
                p.noFill();
                p.stroke(74, 144, 226, 150);
                p.strokeWeight(2);
                p.ellipse(dotX, dotY, activeDotSize + 4, activeDotSize + 4);
            } else {
                // 他のページ - 小さく薄く
                p.fill(255, 255, 255, 120);
                p.noStroke();
                p.ellipse(dotX, dotY, dotSize, dotSize);
            }
        }
        
        // AppLibraryページには特別なアイコン
        for (int i = 0; i < homePages.size(); i++) {
            HomePage page = homePages.get(i);
            if (page.isAppLibraryPage()) {
                int dotX = startX + i * spacing;
                
                // AppLibraryアイコン（グリッド風）
                p.stroke(i == currentPageIndex ? 255 : 200);
                p.strokeWeight(1);
                p.noFill();
                
                // 小さな3x3グリッド
                int gridSize = 6;
                for (int row = 0; row < 3; row++) {
                    for (int col = 0; col < 3; col++) {
                        int x = dotX - gridSize + col * (gridSize / 2);
                        int y = dotY - gridSize + row * (gridSize / 2);
                        p.rect(x, y, 1, 1);
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
            int iconY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 15);
            
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
     * Opens the app library screen.
     */
    private void openAppLibrary() {
        System.out.println("HomeScreen: Opening app library");
        
        if (kernel != null && kernel.getScreenManager() != null) {
            AppLibraryScreen appLibrary = new AppLibraryScreen(kernel);
            appLibrary.setHomeScreen(this); // Pass reference for "Add to Home" functionality
            kernel.getScreenManager().pushScreen(appLibrary);
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
        
        // 編集モード終了時にはドラッグ状態をリセット
        if (!isEditing) {
            resetDragState();
            System.out.println("HomeScreen: Reset drag state on edit mode exit");
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
                return handleSwipeUp();
                
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
        
        // ナビゲーションエリア（下部）のタップでApp Libraryを開く
        if (pageY > (600 - NAV_AREA_HEIGHT)) {
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
                    // 通常モードではアプリ起動
                    launchApplication(tappedShortcut.getApplication());
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
                launchApplication(tappedApp);
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
        // 編集モード中はページスワイプを無効化
        if (isEditing) {
            System.out.println("HomeScreen: Left swipe ignored - edit mode active");
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
        // 編集モード中はページスワイプを無効化
        if (isEditing) {
            System.out.println("HomeScreen: Right swipe ignored - edit mode active");
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
    private boolean handleSwipeUp() {
        System.out.println("HomeScreen: Up swipe detected - opening App Library");
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
            draggedShortcut.setDragPosition(
                event.getCurrentX() - dragOffsetX, 
                event.getCurrentY() - dragOffsetY
            );
            System.out.println("HomeScreen: Updating icon drag position");
            return true; // アイコンドラッグが優先
        }
        
        // 編集モードではページスワイプを無効にする
        if (isEditing) {
            return false;
        }
        
        // 通常モードでのみページ切り替えドラッグを処理
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
            return true;
        }
        
        // 編集モードではページスワイプを処理しない
        if (isEditing) {
            return false;
        }
        
        // 通常モードでのみページドラッグ終了を処理
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
}