package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.apps.launcher.model.HomePage;
import jp.moyashi.phoneos.core.apps.launcher.model.Shortcut;
import jp.moyashi.phoneos.core.service.LayoutManager;
import jp.moyashi.phoneos.core.input.GestureListener;
import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import jp.moyashi.phoneos.core.ui.popup.PopupMenu;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;

import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HomeScreen implements Screen, GestureListener {

    private static final boolean DEBUG = true;

    private final Kernel kernel;
    private processing.core.PImage backgroundImage;
    private int backgroundColor;
    private int textColor;
    private int accentColor;
    private boolean isInitialized;
    private List<HomePage> homePages;
    private int currentPageIndex;
    private boolean isEditing;
    private long touchStartTime;
    private Shortcut tappedShortcut;
    private IApplication tappedAppLibraryItem;
    private static final long TAP_FEEDBACK_DURATION = 100;
    private static final long LONG_PRESS_DURATION = 500;
    private Shortcut draggedShortcut;
    private boolean isDragging;
    private int dragOffsetX;
    private int dragOffsetY;
    private float swipeStartX;
    private boolean isSwipingPages;
    private static final float SWIPE_THRESHOLD = 50.0f;
    private float pageTransitionOffset = 0.0f;
    private boolean isAnimating = false;
    private float animationProgress = 0.0f;
    private int targetPageIndex = 0;
    private long animationStartTime = 0;
    private float startOffset = 0.0f;
    private int animationBasePageIndex = 0;
    private static final long ANIMATION_DURATION = 500;
    private static final int GRID_COLS = 4;
    private static final int GRID_ROWS = 5;
    private static final int ICON_SIZE = 64;
    private static final int ICON_SPACING = 20;
    private static final int DOCK_HEIGHT = 90; // 常時表示フィールドの高さ
    private static final int DOCK_Y = 600 - DOCK_HEIGHT; // 常時表示フィールドのY座標
    private IApplication appToAdd = null;

    // Page switching during drag
    private static final int EDGE_THRESHOLD = 50; // Distance from edge to trigger page switch
    private static final long EDGE_HOVER_DURATION = 800; // Time to hover at edge before switching (ms)
    private long edgeHoverStartTime = 0;
    private boolean isHoveringAtEdge = false;
    private boolean canSwitchToLeft = false;
    private boolean canSwitchToRight = false;

    // Icon cache to avoid loading icons every frame
    private Map<IApplication, processing.core.PImage> iconCache;
    private boolean iconsLoaded = false;

    // Edit mode temporary page management
    private HomePage temporaryEditPage = null;
    private int tempPageInsertIndex = -1;

    // Global dock shortcuts (shared across all pages)
    private final List<Shortcut> globalDockShortcuts;

    public HomeScreen(Kernel kernel) {
        this.kernel = kernel;
        this.backgroundColor = 0x1E1E1E;
        this.textColor = 0xFFFFFF;
        this.accentColor = 0x4A90E2;
        this.isInitialized = false;
        this.homePages = new ArrayList<>();
        this.currentPageIndex = 0;
        this.isEditing = false;
        this.draggedShortcut = null;
        this.isDragging = false;
        this.iconCache = new ConcurrentHashMap<>();
        this.isSwipingPages = false;
        this.pageTransitionOffset = 0.0f;
        this.isAnimating = false;
        this.targetPageIndex = 0;
        this.globalDockShortcuts = new ArrayList<>();
        if (DEBUG) {
            System.out.println("📱 HomeScreen: Advanced launcher home screen created");
        }
    }

    @Override
    public void setup(processing.core.PApplet p) {
        if (isInitialized) {
            if (DEBUG) System.out.println("⚠️ HomeScreen: setup() called again - skipping duplicate initialization");
            return;
        }
        isInitialized = true;
        if (DEBUG) System.out.println("🚀 HomeScreen: Initializing multi-page launcher...");

        // Ensure all state variables are properly initialized
        pageTransitionOffset = 0.0f;
        isAnimating = false;
        isDragging = false;
        isEditing = false;
        resetEdgeHoverState();

        loadBackgroundImage();
        initializeHomePages();
        preloadIcons(p);
        if (kernel != null && kernel.getGestureManager() != null) {
            kernel.getGestureManager().addGestureListener(this);
            if (DEBUG) System.out.println("HomeScreen: Registered gesture listener with GestureManager");
        } else {
            if (DEBUG) System.out.println("⚠️ HomeScreen: GestureManager is null - gesture handling may not work");
        }
    }

    public void setup(PGraphics g) {
        if (isInitialized) {
            if (DEBUG) System.out.println("⚠️ HomeScreen: setup(PGraphics) called again - skipping duplicate initialization");
            return;
        }
        isInitialized = true;
        if (DEBUG) System.out.println("🚀 HomeScreen: Initializing multi-page launcher (PGraphics)...");

        // Ensure all state variables are properly initialized
        pageTransitionOffset = 0.0f;
        isAnimating = false;
        isDragging = false;
        isEditing = false;
        resetEdgeHoverState();

        loadBackgroundImage();
        initializeHomePages();
        preloadIcons(g);
        if (kernel != null && kernel.getGestureManager() != null) {
            kernel.getGestureManager().addGestureListener(this);
            if (DEBUG) System.out.println("HomeScreen: Registered gesture listener with GestureManager");
        } else {
            if (DEBUG) System.out.println("⚠️ HomeScreen: GestureManager is null - gesture handling may not work");
        }
    }

    @Override
    public void draw(PApplet p) {
        try {
            if (backgroundImage != null) {
                p.image(backgroundImage, 0, 0, 400, 600);
            } else {
                p.background(30, 30, 30);
            }

            // Check for edge timer expiration even when mouse is not moving
            checkEdgeTimerExpiration();

            updatePageAnimation();
            drawStatusBar(p);
            drawPagesWithTransition(p);
            drawPageIndicators(p);
            drawDock(p); // 常時表示フィールドを描画
            if (appToAdd != null) {
                drawAddToHomePopup(p);
            }
        } catch (Exception e) {
            System.err.println("❌ HomeScreen: Draw error - " + e.getMessage());
            e.printStackTrace();
            p.background(255, 0, 0);
            p.fill(255);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text("HomeScreen Error: " + e.getMessage(), p.width / 2, p.height / 2);
        }
    }

    @Override
    public void draw(PGraphics g) {
        try {
            if (backgroundImage != null) {
                g.image(backgroundImage, 0, 0, 400, 600);
            } else {
                g.background(30, 30, 30);
            }

            // Check for edge timer expiration even when mouse is not moving
            checkEdgeTimerExpiration();

            updatePageAnimation();
            drawStatusBar(g);
            drawPagesWithTransition(g);
            drawPageIndicators(g);
            drawDock(g); // 常時表示フィールドを描画
            if (appToAdd != null) {
                drawAddToHomePopup(g);
            }
        } catch (Exception e) {
            System.err.println("❌ HomeScreen: Draw error (PGraphics) - " + e.getMessage());
            e.printStackTrace();
            g.background(255, 0, 0);
            g.fill(255);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(16);
            g.text("HomeScreen Error: " + e.getMessage(), g.width / 2, g.height / 2);
        }
    }

    @Override
    public void mousePressed(processing.core.PApplet p, int mouseX, int mouseY) {
        if (DEBUG) System.out.println("HomeScreen: Touch at (" + mouseX + ", " + mouseY + ")");
        touchStartTime = System.currentTimeMillis();
        tappedShortcut = getShortcutAtPositionWithTransform(mouseX, mouseY);
        swipeStartX = mouseX;
        isSwipingPages = false;
    }

    private boolean isClickingDeleteButton(int mouseX, int mouseY, Shortcut shortcut) {
        if (!isEditing) return false;
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        int iconX = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
        int iconY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20);
        int deleteX = iconX + ICON_SIZE - 8;
        int deleteY = iconY + 8;
        return Math.sqrt(Math.pow(mouseX - deleteX, 2) + Math.pow(mouseY - deleteY, 2)) <= 8;
    }

    private boolean isClickingDeleteButtonWithTransform(int mouseX, int mouseY, Shortcut shortcut) {
        if (!isEditing) return false;
        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        int shortcutPageIndex = -1;
        for (int i = 0; i < homePages.size(); i++) {
            if (homePages.get(i).getShortcuts().contains(shortcut)) {
                shortcutPageIndex = i;
                break;
            }
        }
        if (shortcutPageIndex == -1) return false;
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        int iconX = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
        int iconY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20);
        float screenDeleteX = totalOffset + shortcutPageIndex * 400 + iconX + ICON_SIZE - 8;
        float screenDeleteY = iconY + 8;
        return Math.sqrt(Math.pow(mouseX - screenDeleteX, 2) + Math.pow(mouseY - screenDeleteY, 2)) <= 8;
    }

    public void mouseDragged(processing.core.PApplet p, int mouseX, int mouseY) {
        if (DEBUG) System.out.println("HomeScreen: mouseDragged called - delegating to GestureManager");
    }

    public void mouseReleased(processing.core.PApplet p, int mouseX, int mouseY) {
        if (DEBUG) System.out.println("HomeScreen: mouseReleased called - delegating to GestureManager");
        tappedShortcut = null;
        tappedAppLibraryItem = null;
        resetDragState();
        isSwipingPages = false;

        // Ensure edge hover state is reset
        resetEdgeHoverState();
    }

    private void startDragging(Shortcut shortcut, int mouseX, int mouseY) {
        draggedShortcut = shortcut;
        isDragging = true;
        shortcut.setDragging(true);

        // Set initial drag position to current screen coordinates
        shortcut.setDragPosition(mouseX, mouseY);

        // Calculate drag offset from icon center
        dragOffsetX = ICON_SIZE / 2;
        dragOffsetY = ICON_SIZE / 2;

        if (DEBUG) {
            System.out.println("HomeScreen: Started dragging " + shortcut.getDisplayName());
            System.out.println("  - mouseX: " + mouseX + ", mouseY: " + mouseY);
            System.out.println("  - dragOffsetX: " + dragOffsetX + ", dragOffsetY: " + dragOffsetY);
        }
    }

    private void handleShortcutDrop(int mouseX, int mouseY) {
        if (draggedShortcut == null) return;

        HomePage currentPage = getCurrentPage();
        if (currentPage == null) return;

        // 常時表示フィールドへのドロップを最初にチェック
        if (isInDockArea(mouseY)) {
            handleDockDrop(mouseX, mouseY);
            return;
        }

        // 通常のグリッドへのドロップ処理
        handleGridDrop(mouseX, mouseY);
    }

    private void handleDockDrop(int mouseX, int mouseY) {
        HomePage currentPage = getCurrentPage();
        if (currentPage == null || currentPage.isAppLibraryPage()) return;

        int dropPosition = getDockDropPosition(mouseX);

        if (draggedShortcut.isInDock()) {
            // 常時表示フィールド内での移動
            moveDockShortcut(draggedShortcut, dropPosition);
        } else {
            // 通常のグリッドから常時表示フィールドへの移動
            if (globalDockShortcuts.size() < HomePage.MAX_DOCK_SHORTCUTS) {
                moveShortcutToDock(draggedShortcut);
            } else {
                if (DEBUG) System.out.println("HomeScreen: 常時表示フィールドが満杯です");
            }
        }

        saveCurrentLayout();
    }

    private void handleGridDrop(int mouseX, int mouseY) {
        // Check if dropping on a different page
        int[] coords = transformMouseCoordinates(mouseX, mouseY);
        int targetPageIndex = coords[2];

        if (targetPageIndex >= 0 && targetPageIndex < homePages.size()) {
            HomePage targetPage = homePages.get(targetPageIndex);
            HomePage originalPage = null;

            // Find the original page containing the dragged shortcut
            if (draggedShortcut.isInDock()) {
                originalPage = getCurrentPage();
            } else {
                for (HomePage page : homePages) {
                    if (page.getShortcuts().contains(draggedShortcut)) {
                        originalPage = page;
                        break;
                    }
                }
            }

            int[] gridPos = screenToGridPosition(coords[0], coords[1]);
            if (gridPos != null && !targetPage.isAppLibraryPage()) {
                // Try to place shortcut on target page
                if (targetPage.isPositionEmpty(gridPos[0], gridPos[1])) {
                    // Remove from original location
                    if (originalPage != null) {
                        if (draggedShortcut.isInDock()) {
                            removeDockShortcut(draggedShortcut);
                        } else {
                            originalPage.removeShortcut(draggedShortcut);
                        }
                    }

                    // Add to target page
                    draggedShortcut.setGridPosition(gridPos[0], gridPos[1]);
                    draggedShortcut.setDockPosition(-1); // 常時表示フィールドから削除
                    targetPage.addShortcut(draggedShortcut);

                    if (DEBUG) System.out.println("HomeScreen: ショートカットを移動しました - ページ " + targetPageIndex + " の位置 (" + gridPos[0] + ", " + gridPos[1] + ")");

                    // Check for empty pages after move (during edit mode, empty pages will be cleaned up on exit)
                    if (!isEditing && originalPage != null && originalPage.isEmpty() && !originalPage.isAppLibraryPage() && homePages.size() > 2) {
                        removeEmptyPages();
                    }

                    saveCurrentLayout();
                } else {
                    if (DEBUG) System.out.println("HomeScreen: ショートカット移動失敗 - 位置が占有済み");
                }
            } else if (gridPos != null) {
                // Moving within the same page
                HomePage currentPage = getCurrentPage();
                if (currentPage != null) {
                    if (draggedShortcut.isInDock()) {
                        // 常時表示フィールドから通常のグリッドへの移動
                        if (moveShortcutFromDock(draggedShortcut)) {
                            currentPage.moveShortcut(draggedShortcut, gridPos[0], gridPos[1]);
                            saveCurrentLayout();
                        }
                    } else if (currentPage.moveShortcut(draggedShortcut, gridPos[0], gridPos[1])) {
                        if (DEBUG) System.out.println("HomeScreen: ショートカットを移動しました (" + gridPos[0] + ", " + gridPos[1] + ")");
                        saveCurrentLayout();
                    } else {
                        if (DEBUG) System.out.println("HomeScreen: ショートカット移動失敗 - 位置が占有済みか無効");
                    }
                }
            }
        }
    }

    private void handlePageSwipe(int mouseX) {
        float swipeDistance = mouseX - swipeStartX;
        if (swipeDistance > SWIPE_THRESHOLD) {
            if (currentPageIndex > 0) {
                currentPageIndex--;
                if (DEBUG) System.out.println("HomeScreen: Swiped to page " + currentPageIndex);
            }
        } else if (swipeDistance < -SWIPE_THRESHOLD) {
            if (currentPageIndex < homePages.size() - 1) {
                currentPageIndex++;
                if (DEBUG) System.out.println("HomeScreen: Swiped to page " + currentPageIndex);
            }
        }
    }

    private int[] screenToGridPosition(int screenX, int screenY) {
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
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

    private void resetDragState() {
        if (draggedShortcut != null) {
            draggedShortcut.setDragging(false);
            draggedShortcut = null;
        }
        isDragging = false;

        // Reset edge hover state
        isHoveringAtEdge = false;
        edgeHoverStartTime = 0;
        canSwitchToLeft = false;
        canSwitchToRight = false;
    }

    /**
     * Check if dragged icon is near screen edges and start/update hover timer
     */
    private void handleDragEdgeDetection(int mouseX, int mouseY) {
        if (!isDragging || !isEditing || draggedShortcut == null) {
            resetEdgeHoverState();
            return;
        }

        // Don't start new edge detection during animation, but allow resetting
        if (isAnimating) {
            resetEdgeHoverState();
            return;
        }

        boolean atLeftEdge = mouseX < EDGE_THRESHOLD && currentPageIndex > 0;
        boolean atRightEdge = mouseX > (400 - EDGE_THRESHOLD) && currentPageIndex < homePages.size() - 1;

        // Skip App Library page
        boolean canGoLeft = atLeftEdge && (currentPageIndex > 0) && !homePages.get(currentPageIndex - 1).isAppLibraryPage();
        boolean canGoRight = atRightEdge && (currentPageIndex < homePages.size() - 1) && !homePages.get(currentPageIndex + 1).isAppLibraryPage();

        if (canGoLeft || canGoRight) {
            if (!isHoveringAtEdge) {
                // Start hovering at edge
                isHoveringAtEdge = true;
                edgeHoverStartTime = System.currentTimeMillis();
                canSwitchToLeft = canGoLeft;
                canSwitchToRight = canGoRight;
                if (DEBUG) System.out.println("HomeScreen: Started hovering at " + (canGoLeft ? "left" : "right") + " edge - timer started");
            }
            // Timer expiration check is now handled in checkEdgeTimerExpiration()
        } else {
            resetEdgeHoverState();
        }
    }

    /**
     * Reset edge hover state
     */
    private void resetEdgeHoverState() {
        if (isHoveringAtEdge) {
            isHoveringAtEdge = false;
            edgeHoverStartTime = 0;
            canSwitchToLeft = false;
            canSwitchToRight = false;
        }
    }

    /**
     * Check if edge timer has expired and trigger page switch if needed
     * This runs every frame to ensure immediate response when timer expires
     */
    private void checkEdgeTimerExpiration() {
        if (!isHoveringAtEdge || !isDragging || !isEditing || draggedShortcut == null || isAnimating) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - edgeHoverStartTime >= EDGE_HOVER_DURATION) {
            if (canSwitchToLeft && currentPageIndex > 0) {
                if (DEBUG) System.out.println("HomeScreen: Edge timer expired - switching to left page automatically");
                switchPageDuringDrag(-1);
                resetEdgeHoverState();
            } else if (canSwitchToRight && currentPageIndex < homePages.size() - 1) {
                if (DEBUG) System.out.println("HomeScreen: Edge timer expired - switching to right page automatically");
                switchPageDuringDrag(1);
                resetEdgeHoverState();
            }
        }
    }

    /**
     * Switch page during drag operation
     */
    private void switchPageDuringDrag(int direction) {
        int newPageIndex = currentPageIndex + direction;
        if (newPageIndex >= 0 && newPageIndex < homePages.size() && !homePages.get(newPageIndex).isAppLibraryPage()) {
            if (DEBUG) System.out.println("HomeScreen: Switching page during drag from " + currentPageIndex + " to " + newPageIndex);

            // If we're dragging an icon, we need to adjust both the drag position and offset
            if (draggedShortcut != null) {
                float currentDragX = draggedShortcut.getDragX();
                float currentDragY = draggedShortcut.getDragY();

                if (DEBUG) System.out.println("HomeScreen: Before adjustment - dragX: " + currentDragX + ", dragY: " + currentDragY + ", dragOffsetX: " + dragOffsetX);

                // The icon should maintain its relative position on the new page
                // We need to adjust the drag position by the page offset
                float pageOffset = direction * 400;
                float newDragX = currentDragX + pageOffset;

                // Set new drag position
                draggedShortcut.setDragPosition(newDragX, currentDragY);

                if (DEBUG) System.out.println("HomeScreen: After adjustment - newDragX: " + newDragX + ", pageOffset: " + pageOffset);
            }

            startPageTransition(newPageIndex);

            // Keep the drag state active so user can continue dragging the shortcut
            if (DEBUG) System.out.println("HomeScreen: Page switched while maintaining drag state for shortcut");
        }
    }

    private void removeShortcut(Shortcut shortcut) {
        if (shortcut.isInDock()) {
            removeDockShortcut(shortcut);
        } else {
            HomePage currentPage = getCurrentPage();
            if (currentPage != null) {
                currentPage.removeShortcut(shortcut);

                // Check if page became empty and remove if necessary (but not during edit mode)
                if (!isEditing && currentPage.isEmpty() && !currentPage.isAppLibraryPage() && homePages.size() > 2) {
                    removeEmptyPages();
                }
            }
        }

        if (DEBUG) System.out.println("HomeScreen: ショートカット削除: " + shortcut.getDisplayName());
        saveCurrentLayout();
    }

    private void saveCurrentLayout() {
        if (kernel != null && kernel.getLayoutManager() != null && homePages != null) {
            boolean success = kernel.getLayoutManager().saveLayout(homePages, globalDockShortcuts);
            if (success) {
                if (DEBUG) System.out.println("HomeScreen: レイアウト（グローバルDock含む）保存成功");
            } else {
                System.err.println("HomeScreen: レイアウト保存失敗");
            }
        }
    }

    @Override
    public void cleanup(PApplet p) {
        if (kernel != null && kernel.getGestureManager() != null) {
            kernel.getGestureManager().removeGestureListener(this);
            if (DEBUG) System.out.println("HomeScreen: Unregistered gesture listener");
        }

        // Complete state reset on cleanup
        isInitialized = false;
        pageTransitionOffset = 0.0f;
        isAnimating = false;
        isDragging = false;
        isEditing = false;
        resetDragState();
        resetEdgeHoverState();

        // Clean up temporary edit page if exists
        temporaryEditPage = null;
        tempPageInsertIndex = -1;

        if (DEBUG) System.out.println("HomeScreen: Launcher home screen cleaned up - all state reset");
    }

    private void loadBackgroundImage() {
        try {
            processing.core.PApplet p = kernel.getParentApplet();
            if (p != null) {
                URL wallpaperUrl = getClass().getResource("/data/wallpaper.jpg");
                if (wallpaperUrl != null) {
                    if (DEBUG) System.out.println("HomeScreen: ✅ Background image resource found at: " + wallpaperUrl);
                    backgroundImage = p.loadImage(wallpaperUrl.toExternalForm());
                    if (DEBUG && backgroundImage != null) {
                        System.out.println("HomeScreen: Image dimensions: " + backgroundImage.width + "x" + backgroundImage.height);
                    } else if (backgroundImage == null) {
                        System.err.println("HomeScreen: ❌ loadImage() failed for URL: " + wallpaperUrl);
                    }
                } else {
                    System.err.println("HomeScreen: ❌ Background image resource '/data/wallpaper.jpg' not found in classpath. Using color fallback.");
                }
            }
        } catch (Exception e) {
            System.err.println("HomeScreen: Error loading background image: " + e.getMessage());
            backgroundImage = null;
        }
    }

    @Override
    public String getScreenTitle() {
        return "Home Screen";
    }

    public List<HomePage> getHomePages() {
        return homePages;
    }

    public void navigateToFirstPage() {
        if (DEBUG) System.out.println("HomeScreen: Navigating to first page");
        if (!homePages.isEmpty() && currentPageIndex != 0 && !isAnimating) {
            startPageTransition(0);
        } else if (DEBUG && currentPageIndex == 0) {
            System.out.println("HomeScreen: Already on first page");
        }
    }

    private void initializeHomePages() {
        try {
            homePages.clear();
            boolean layoutLoaded = false;
            if (kernel != null && kernel.getLayoutManager() != null) {
                if (DEBUG) System.out.println("HomeScreen: 保存されたレイアウトを読み込み中...");
                LayoutManager.LayoutLoadResult result = kernel.getLayoutManager().loadLayoutWithDock();
                if (result != null && result.pages != null && !result.pages.isEmpty()) {
                    homePages.addAll(result.pages);

                    // グローバル常時表示フィールドを復元
                    globalDockShortcuts.clear();
                    if (result.globalDockShortcuts != null) {
                        globalDockShortcuts.addAll(result.globalDockShortcuts);
                    }

                    layoutLoaded = true;
                    if (DEBUG) System.out.println("HomeScreen: 保存されたレイアウトを復元しました (" +
                                                homePages.size() + "ページ, " +
                                                globalDockShortcuts.size() + "Dockアイテム)");
                } else {
                    if (DEBUG) System.out.println("HomeScreen: 保存されたレイアウトが見つかりません、デフォルトレイアウトを作成");
                }
            }
            if (!layoutLoaded) {
                createDefaultLayout();
            }
            long appLibraryCount = homePages.stream()
                    .filter(page -> page.getPageType() == HomePage.PageType.APP_LIBRARY)
                    .count();
            if (DEBUG) System.out.println("HomeScreen: 現在のAppLibraryページ数: " + appLibraryCount);
            if (appLibraryCount == 0) {
                createAppLibraryPage();
                if (DEBUG) System.out.println("HomeScreen: AppLibraryページを新規追加しました");
            } else if (appLibraryCount > 1) {
                if (DEBUG) System.out.println("HomeScreen: ⚠️ AppLibraryページが重複しています(" + appLibraryCount + "個) - 修正中...");
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
                if (DEBUG) System.out.println("HomeScreen: ✅ " + toRemove.size() + "個の重複AppLibraryページを削除しました");
            } else {
                if (DEBUG) System.out.println("HomeScreen: AppLibraryページは既に存在します");
            }
            if (DEBUG) System.out.println("HomeScreen: " + homePages.size() + "ページでホーム画面を初期化完了");
        } catch (Exception e) {
            System.err.println("HomeScreen: initializeHomePages でクリティカルエラー: " + e.getMessage());
            e.printStackTrace();
            if (homePages.isEmpty()) {
                homePages.add(new HomePage("Emergency"));
            }
        }
    }

    private void createDefaultLayout() {
        HomePage firstPage = new HomePage("Home");
        homePages.add(firstPage);
        if (kernel != null && kernel.getAppLoader() != null) {
            try {
                List<IApplication> loadedApps = kernel.getAppLoader().getLoadedApps();
                if (loadedApps != null) {
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
            if (DEBUG) System.out.println("HomeScreen: KernelまたはAppLoaderがnull - 空のページを作成");
        }
        if (kernel != null && kernel.getLayoutManager() != null) {
            kernel.getLayoutManager().saveLayout(homePages);
            if (DEBUG) System.out.println("HomeScreen: デフォルトレイアウトを保存しました");
        }
    }

    private void createAppLibraryPage() {
        if (DEBUG) System.out.println("HomeScreen: AppLibraryページを作成中...");
        HomePage appLibraryPage = new HomePage(HomePage.PageType.APP_LIBRARY, "App Library");
        if (kernel != null && kernel.getAppLoader() != null) {
            try {
                List<IApplication> allApps = kernel.getAppLoader().getLoadedApps();
                if (allApps != null) {
                    List<IApplication> availableApps = new ArrayList<>();
                    for (IApplication app : allApps) {
                        if (app != null && !"jp.moyashi.phoneos.core.apps.launcher".equals(app.getApplicationId())) {
                            availableApps.add(app);
                        }
                    }
                    availableApps.sort(Comparator.comparing(IApplication::getName, String.CASE_INSENSITIVE_ORDER));
                    appLibraryPage.setAllApplications(availableApps);
                    if (DEBUG) System.out.println("HomeScreen: AppLibraryページに " + availableApps.size() + " 個のアプリを設定");
                }
            } catch (Exception e) {
                System.err.println("HomeScreen: AppLibraryページ作成エラー: " + e.getMessage());
            }
        }
        homePages.add(appLibraryPage);
        if (DEBUG) {
            System.out.println("HomeScreen: AppLibraryページを追加しました");
            System.out.println("HomeScreen: 総ページ数: " + homePages.size() + ", AppLibraryページインデックス: " + (homePages.size() - 1));
        }
    }

    private void drawStatusBar(PApplet p) {
        try {
            p.fill(textColor, 180);
            p.textAlign(p.LEFT, p.TOP);
            p.textSize(12);
            if (kernel != null && kernel.getSystemClock() != null) {
                try {
                    p.text(kernel.getSystemClock().getFormattedTime(), 15, 15);
                } catch (Exception e) {
                    p.text("--:--", 15, 15);
                }
            } else {
                p.text("No Clock", 15, 15);
            }
            p.textAlign(p.RIGHT, p.TOP);
            p.text("MochiOS", 385, 15);
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
            if (isInitialized) {
                p.fill(76, 175, 80);
            } else {
                p.fill(255, 152, 0);
            }
            p.noStroke();
            p.ellipse(370, 20, 8, 8);
        } catch (Exception e) {
            System.err.println("Error in drawStatusBar: " + e.getMessage());
            p.fill(255);
            p.textAlign(p.LEFT, p.TOP);
            p.textSize(12);
            p.text("Status Error", 15, 15);
        }
    }

    private void drawStatusBar(PGraphics g) {
        try {
            g.fill(textColor, 180);
            g.textAlign(PConstants.LEFT, PConstants.TOP);
            g.textSize(12);
            if (kernel != null && kernel.getSystemClock() != null) {
                try {
                    g.text(kernel.getSystemClock().getFormattedTime(), 15, 15);
                } catch (Exception e) {
                    g.text("--:--", 15, 15);
                }
            } else {
                g.text("No Clock", 15, 15);
            }
            g.textAlign(PConstants.RIGHT, PConstants.TOP);
            g.text("MochiOS", 385, 15);
            if (!homePages.isEmpty() && currentPageIndex < homePages.size()) {
                HomePage currentPage = homePages.get(currentPageIndex);
                String pageName = currentPage.isAppLibraryPage() ? "App Library" :
                        currentPage.getPageName() != null ? currentPage.getPageName() :
                                "Page " + (currentPageIndex + 1);
                g.fill(255, 255, 255, 150);
                g.textAlign(PConstants.CENTER, PConstants.TOP);
                g.textSize(11);
                g.text(pageName, 200, 15);
            }
            if (isInitialized) {
                g.fill(76, 175, 80);
            } else {
                g.fill(255, 152, 0);
            }
            g.noStroke();
            g.ellipse(370, 20, 8, 8);
        } catch (Exception e) {
            System.err.println("Error in drawStatusBar: " + e.getMessage());
            g.fill(255);
            g.textAlign(PConstants.LEFT, PConstants.TOP);
            g.textSize(12);
            g.text("Status Error", 15, 15);
        }
    }

    private void updatePageAnimation() {
        if (!isAnimating) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - animationStartTime;
        if (elapsed >= ANIMATION_DURATION) {
            completePageTransition();
        } else {
            float t = (float) elapsed / ANIMATION_DURATION;
            animationProgress = easeOutCubic(t);
            if (targetPageIndex == animationBasePageIndex) {
                pageTransitionOffset = startOffset * (1.0f - animationProgress);
            } else {
                float targetOffset = (animationBasePageIndex - targetPageIndex) * 400;
                pageTransitionOffset = startOffset + (targetOffset - startOffset) * animationProgress;
                if (DEBUG) System.out.println("🎬 Animation: basePage=" + animationBasePageIndex + " to targetPage=" + targetPageIndex +
                        ", startOffset=" + startOffset + ", targetOffset=" + targetOffset + ", progress=" + animationProgress + ", offset=" + pageTransitionOffset);
            }
        }
    }

    private float easeOutCubic(float t) {
        return 1 - (1 - t) * (1 - t);
    }

    private void completePageTransition() {
        if (DEBUG) System.out.println("🎬 Completing transition: currentPage=" + currentPageIndex + " -> targetPage=" + targetPageIndex);
        currentPageIndex = targetPageIndex;
        pageTransitionOffset = 0.0f;
        isAnimating = false;
        animationProgress = 0.0f;
        startOffset = 0.0f;

        // Reset edge hover state after page transition
        resetEdgeHoverState();

        if (DEBUG) System.out.println("🎬 Page transition completed to page " + currentPageIndex + ", offset reset to 0");
    }

    private void startPageTransition(int newPageIndex) {
        if (newPageIndex == currentPageIndex || isAnimating) {
            if (DEBUG && isAnimating) {
                System.out.println("🎬 Skipping page transition - already animating");
            }
            return;
        }

        // Reset edge hover state when starting any page transition
        resetEdgeHoverState();

        targetPageIndex = newPageIndex;
        isAnimating = true;
        animationStartTime = System.currentTimeMillis();
        animationProgress = 0.0f;
        startOffset = pageTransitionOffset;
        animationBasePageIndex = currentPageIndex;

        if (DEBUG) System.out.println("🎬 Starting page transition from " + currentPageIndex + " to " + targetPageIndex + " with startOffset=" + startOffset + ", basePageIndex=" + animationBasePageIndex);
    }

    private void drawPagesWithTransition(PApplet p) {
        if (homePages.isEmpty()) {
            p.fill(255, 255, 255, 150);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text("No apps installed", 200, 300);
            p.textSize(12);
            p.text("Swipe up to access app library", 200, 320);
            return;
        }
        p.pushMatrix();
        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        p.translate(totalOffset, 0);
        if (DEBUG && isAnimating) {
            System.out.println("🎨 Drawing with basePageIndex=" + basePageForOffset + ", pageTransitionOffset=" + pageTransitionOffset + ", totalOffset=" + totalOffset);
        }
        for (int i = 0; i < homePages.size(); i++) {
            p.pushMatrix();
            p.translate(i * 400, 0);
            HomePage page = homePages.get(i);
            if (page.isAppLibraryPage()) {
                drawAppLibraryPage(p, page);
            } else {
                drawNormalPage(p, page);
            }
            p.popMatrix();
        }
        p.popMatrix();
        if (isDragging && draggedShortcut != null) {
            // Draw dragged shortcut with proper page offset consideration
            drawDraggedShortcutWithOffset(p, draggedShortcut);

            // Draw edge indicators during drag
            if (isEditing && isHoveringAtEdge) {
                drawEdgeIndicators(p);
            }
        }
    }

    private void drawPagesWithTransition(PGraphics g) {
        if (homePages.isEmpty()) {
            g.fill(255, 255, 255, 150);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.textSize(16);
            g.text("No apps installed", 200, 300);
            g.textSize(12);
            g.text("Swipe up to access app library", 200, 320);
            return;
        }
        g.pushMatrix();
        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        g.translate(totalOffset, 0);
        if (DEBUG && isAnimating) {
            System.out.println("🎨 Drawing with basePageIndex=" + basePageForOffset + ", pageTransitionOffset=" + pageTransitionOffset + ", totalOffset=" + totalOffset);
        }
        for (int i = 0; i < homePages.size(); i++) {
            g.pushMatrix();
            g.translate(i * 400, 0);
            HomePage page = homePages.get(i);
            if (page.isAppLibraryPage()) {
                drawAppLibraryPage(g, page);
            } else {
                drawNormalPage(g, page);
            }
            g.popMatrix();
        }
        g.popMatrix();
        if (isDragging && draggedShortcut != null) {
            // Draw dragged shortcut with proper page offset consideration
            drawDraggedShortcutWithOffset(g, draggedShortcut);

            // Draw edge indicators during drag
            if (isEditing && isHoveringAtEdge) {
                drawEdgeIndicators(g);
            }
        }
    }

    private int[] transformMouseCoordinates(int mouseX, int mouseY) {
        float totalOffset = -currentPageIndex * 400 + pageTransitionOffset;
        float transformedX = mouseX - totalOffset;
        int targetPageIndex = (int) (transformedX / 400);
        if (targetPageIndex < 0) targetPageIndex = 0;
        if (targetPageIndex >= homePages.size()) targetPageIndex = homePages.size() - 1;
        float pageX = transformedX - (targetPageIndex * 400);
        return new int[]{(int) pageX, mouseY, targetPageIndex};
    }

    private void drawNormalPage(PApplet p, HomePage page) {
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        p.textAlign(p.CENTER, p.TOP);
        p.textSize(10);

        // Check if this is an empty temporary edit page
        if (isEditing && isTemporaryEditPage(page) && page.isEmpty()) {
            drawEmptyEditPageHint(p, startX, startY);
        }

        for (Shortcut shortcut : page.getShortcuts()) {
            if (shortcut.isDragging()) continue;
            int x = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
            int y = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20);
            if (isEditing) {
                x += (int) (Math.sin(System.currentTimeMillis() * 0.01 + shortcut.getShortcutId().hashCode()) * 2);
                y += (int) (Math.cos(System.currentTimeMillis() * 0.012 + shortcut.getShortcutId().hashCode()) * 1.5);
            }
            drawShortcut(p, shortcut, x, y);
        }
        if (isDragging) {
            drawDropTargets(p, startX, startY);
        }
    }

    private void drawNormalPage(PGraphics g, HomePage page) {
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        g.textAlign(PConstants.CENTER, PConstants.TOP);
        g.textSize(10);

        // Check if this is an empty temporary edit page
        if (isEditing && isTemporaryEditPage(page) && page.isEmpty()) {
            drawEmptyEditPageHint(g, startX, startY);
        }

        for (Shortcut shortcut : page.getShortcuts()) {
            if (shortcut.isDragging()) continue;
            int x = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
            int y = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 20);
            if (isEditing) {
                x += (int) (Math.sin(System.currentTimeMillis() * 0.01 + shortcut.getShortcutId().hashCode()) * 2);
                y += (int) (Math.cos(System.currentTimeMillis() * 0.012 + shortcut.getShortcutId().hashCode()) * 1.5);
            }
            drawShortcut(g, shortcut, x, y);
        }
        if (isDragging) {
            drawDropTargets(g, startX, startY);
        }
    }

    private void drawAppLibraryPage(PApplet p, HomePage appLibraryPage) {
        p.fill(20, 20, 20);
        p.noStroke();
        p.rect(0, 0, 400, 600);
        p.fill(255);
        p.textAlign(p.CENTER, p.TOP);
        p.textSize(18);
        p.text("App Library", 200, 70);
        List<IApplication> apps = appLibraryPage.getAllApplications();
        if (apps.isEmpty()) {
            p.fill(255, 150);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(14);
            p.text("No apps available", 200, 300);
            return;
        }
        int startY = 110;
        int listHeight = 600 - startY - 20;
        int itemHeight = 70;
        int scrollOffset = appLibraryPage.getScrollOffset();
        p.pushMatrix();
        for (int i = 0; i < apps.size(); i++) {
            IApplication app = apps.get(i);
            int itemY = startY + i * itemHeight - scrollOffset;
            if (itemY + itemHeight < startY || itemY > startY + listHeight) {
                continue;
            }
            drawAppLibraryItem(p, app, 20, itemY, 360, itemHeight, i == apps.size() - 1);
        }
        p.popMatrix();
        if (appLibraryPage.needsScrolling(listHeight, itemHeight)) {
            drawScrollIndicator(p, appLibraryPage, startY, listHeight, itemHeight);
        }
    }

    private void drawAppLibraryPage(PGraphics g, HomePage appLibraryPage) {
        g.fill(20, 20, 20);
        g.noStroke();
        g.rect(0, 0, 400, 600);
        g.fill(255);
        g.textAlign(PConstants.CENTER, PConstants.TOP);
        g.textSize(18);
        g.text("App Library", 200, 70);
        List<IApplication> apps = appLibraryPage.getAllApplications();
        if (apps.isEmpty()) {
            g.fill(255, 150);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.textSize(14);
            g.text("No apps available", 200, 300);
            return;
        }
        int startY = 110;
        int listHeight = 600 - startY - 20;
        int itemHeight = 70;
        int scrollOffset = appLibraryPage.getScrollOffset();
        g.pushMatrix();
        for (int i = 0; i < apps.size(); i++) {
            IApplication app = apps.get(i);
            int itemY = startY + i * itemHeight - scrollOffset;
            if (itemY + itemHeight < startY || itemY > startY + listHeight) {
                continue;
            }
            drawAppLibraryItem(g, app, 20, itemY, 360, itemHeight, i == apps.size() - 1);
        }
        g.popMatrix();
        if (appLibraryPage.needsScrolling(listHeight, itemHeight)) {
            drawScrollIndicator(g, appLibraryPage, startY, listHeight, itemHeight);
        }
    }

    private void drawAppLibraryItem(PApplet p, IApplication app, int x, int y, int width, int height, boolean isLast) {
        if (tappedAppLibraryItem == app && System.currentTimeMillis() - touchStartTime < TAP_FEEDBACK_DURATION) {
            p.fill(50, 50, 50, 150);
        } else {
            p.fill(58, 58, 58, 100);
        }
        p.noStroke();
        p.rect(x, y, width, height, 8);
        drawAppIcon(p, app, x + 35, y + 35, 40);
        p.fill(255);
        p.textAlign(p.LEFT, p.CENTER);
        p.textSize(16);
        p.text(app.getName(), x + 75, y + 25);
        if (app.getDescription() != null && !app.getDescription().isEmpty()) {
            p.fill(255, 150);
            p.textSize(12);
            String description = app.getDescription();
            if (description.length() > 40) {
                description = description.substring(0, 37) + "...";
            }
            p.text(description, x + 75, y + 45);
        }
        if (!isLast) {
            p.stroke(80, 80, 80);
            p.strokeWeight(1);
            p.line(x + 10, y + height, x + width - 10, y + height);
        }
    }

    private void drawAppLibraryItem(PGraphics g, IApplication app, int x, int y, int width, int height, boolean isLast) {
        if (tappedAppLibraryItem == app && System.currentTimeMillis() - touchStartTime < TAP_FEEDBACK_DURATION) {
            g.fill(50, 50, 50, 150);
        } else {
            g.fill(58, 58, 58, 100);
        }
        g.noStroke();
        g.rect(x, y, width, height, 8);
        drawAppIcon(g, app, x + 35, y + 35, 40);
        g.fill(255);
        g.textAlign(PConstants.LEFT, PConstants.CENTER);
        g.textSize(16);
        g.text(app.getName(), x + 75, y + 25);
        if (app.getDescription() != null && !app.getDescription().isEmpty()) {
            g.fill(255, 150);
            g.textSize(12);
            String description = app.getDescription();
            if (description.length() > 40) {
                description = description.substring(0, 37) + "...";
            }
            g.text(description, x + 75, y + 45);
        }
        if (!isLast) {
            g.stroke(80, 80, 80);
            g.strokeWeight(1);
            g.line(x + 10, y + height, x + width - 10, y + height);
        }
    }

    private void drawScrollIndicator(PApplet p, HomePage appLibraryPage, int listStartY, int listHeight, int itemHeight) {
        List<IApplication> apps = appLibraryPage.getAllApplications();
        int totalContentHeight = apps.size() * itemHeight;
        int scrollOffset = appLibraryPage.getScrollOffset();
        float scrollbarHeight = Math.max(20, (float) listHeight * listHeight / totalContentHeight);
        float scrollbarY = listStartY + (float) scrollOffset * listHeight / totalContentHeight;
        p.fill(255, 100);
        p.noStroke();
        p.rect(390, scrollbarY, 4, scrollbarHeight, 2);
    }

    private void drawScrollIndicator(PGraphics g, HomePage appLibraryPage, int listStartY, int listHeight, int itemHeight) {
        List<IApplication> apps = appLibraryPage.getAllApplications();
        int totalContentHeight = apps.size() * itemHeight;
        int scrollOffset = appLibraryPage.getScrollOffset();
        float scrollbarHeight = Math.max(20, (float) listHeight * listHeight / totalContentHeight);
        float scrollbarY = listStartY + (float) scrollOffset * listHeight / totalContentHeight;
        g.fill(255, 100);
        g.noStroke();
        g.rect(390, scrollbarY, 4, scrollbarHeight, 2);
    }

    private void drawDraggedShortcut(PApplet p, Shortcut shortcut) {
        if (!shortcut.isDragging()) return;
        int x = (int) shortcut.getDragX();
        int y = (int) shortcut.getDragY();
        p.fill(0, 100);
        p.noStroke();
        p.rect(x + 4, y + 4, ICON_SIZE, ICON_SIZE, 12);
        p.fill(255, 220);
        p.stroke(85);
        p.strokeWeight(2);
        p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
        IApplication app = shortcut.getApplication();
        if (app != null) {
            drawAppIcon(p, app, x + ICON_SIZE / 2, y + ICON_SIZE / 2, ICON_SIZE);
        }
        p.noStroke();
        p.textAlign(p.CENTER, p.TOP);
        p.textSize(11);
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 10) {
            displayName = displayName.substring(0, 9) + "...";
        }
        p.fill(0, 120);
        p.text(displayName, x + ICON_SIZE / 2 + 1, y + ICON_SIZE + 9);
        p.fill(255);
        p.text(displayName, x + ICON_SIZE / 2, y + ICON_SIZE + 8);
    }

    /**
     * Draw dragged shortcut at screen coordinates (no page transformation applied)
     */
    private void drawDraggedShortcutWithOffset(PApplet p, Shortcut shortcut) {
        if (!shortcut.isDragging()) return;

        // Get the direct screen position (no coordinate transformation needed)
        int screenX = (int) (shortcut.getDragX() - dragOffsetX);
        int screenY = (int) (shortcut.getDragY() - dragOffsetY);

        if (DEBUG) {
            System.out.println("HomeScreen: Drawing dragged shortcut at screen position");
            System.out.println("  - dragX: " + shortcut.getDragX() + ", dragY: " + shortcut.getDragY());
            System.out.println("  - dragOffsetX: " + dragOffsetX + ", dragOffsetY: " + dragOffsetY);
            System.out.println("  - screenX: " + screenX + ", screenY: " + screenY);
        }

        // Draw shadow
        p.fill(0, 100);
        p.noStroke();
        p.rect(screenX + 4, screenY + 4, ICON_SIZE, ICON_SIZE, 12);

        // Draw icon background
        p.fill(255, 220);
        p.stroke(85);
        p.strokeWeight(2);
        p.rect(screenX, screenY, ICON_SIZE, ICON_SIZE, 12);

        // Draw icon
        IApplication app = shortcut.getApplication();
        if (app != null) {
            drawAppIcon(p, app, screenX + ICON_SIZE / 2, screenY + ICON_SIZE / 2, ICON_SIZE);
        }

        // Draw text
        p.noStroke();
        p.textAlign(p.CENTER, p.TOP);
        p.textSize(11);
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 10) {
            displayName = displayName.substring(0, 9) + "...";
        }
        p.fill(0, 120);
        p.text(displayName, screenX + ICON_SIZE / 2 + 1, screenY + ICON_SIZE + 9);
        p.fill(255);
        p.text(displayName, screenX + ICON_SIZE / 2, screenY + ICON_SIZE + 8);
    }

    /**
     * Draw edge indicators when hovering at screen edges during drag
     */
    private void drawEdgeIndicators(PApplet p) {
        if (!isHoveringAtEdge) return;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - edgeHoverStartTime;
        float progress = Math.min(1.0f, (float) elapsed / EDGE_HOVER_DURATION);

        // Calculate pulsing opacity
        float baseOpacity = 100 + (50 * progress);
        float pulseOpacity = baseOpacity + (30 * (float) Math.sin(currentTime * 0.01));

        p.noStroke();

        if (canSwitchToLeft && currentPageIndex > 0) {
            // Draw left edge indicator
            p.fill(accentColor, (int) pulseOpacity);
            // Draw triangular indicator pointing left
            p.triangle(EDGE_THRESHOLD - 10, 300 - 20, EDGE_THRESHOLD - 10, 300 + 20, 10, 300);

            // Draw progress bar
            p.fill(255, (int) (150 * progress));
            p.rect(5, 280, 40 * progress, 6, 3);

            // Draw hint text
            if (progress > 0.5f) {
                p.fill(255, (int) (255 * (progress - 0.5f) * 2));
                p.textAlign(p.CENTER, p.CENTER);
                p.textSize(12);
                p.text("前のページ", EDGE_THRESHOLD / 2, 330);
            }
        }

        if (canSwitchToRight && currentPageIndex < homePages.size() - 1) {
            // Draw right edge indicator
            p.fill(accentColor, (int) pulseOpacity);
            // Draw triangular indicator pointing right
            p.triangle(400 - EDGE_THRESHOLD + 10, 300 - 20, 400 - EDGE_THRESHOLD + 10, 300 + 20, 390, 300);

            // Draw progress bar
            p.fill(255, (int) (150 * progress));
            p.rect(400 - 45, 280, 40 * progress, 6, 3);

            // Draw hint text
            if (progress > 0.5f) {
                p.fill(255, (int) (255 * (progress - 0.5f) * 2));
                p.textAlign(p.CENTER, p.CENTER);
                p.textSize(12);
                p.text("次のページ", 400 - EDGE_THRESHOLD / 2, 330);
            }
        }
    }

    private void drawDropTargets(PApplet p, int startX, int startY) {
        HomePage currentPage = getCurrentPage();
        if (currentPage == null) return;
        p.stroke(accentColor, 150);
        p.strokeWeight(2);
        p.noFill();
        for (int gridX = 0; gridX < GRID_COLS; gridX++) {
            for (int gridY = 0; gridY < GRID_ROWS; gridY++) {
                if (currentPage.isPositionEmpty(gridX, gridY)) {
                    int x = startX + gridX * (ICON_SIZE + ICON_SPACING);
                    int y = startY + gridY * (ICON_SIZE + ICON_SPACING + 20);
                    p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
                }
            }
        }
    }

    private void drawShortcut(PApplet p, Shortcut shortcut, int x, int y) {
        IApplication app = shortcut.getApplication();
        if (shortcut.isDragging()) {
            return;
        }
        if (tappedShortcut == shortcut && System.currentTimeMillis() - touchStartTime < TAP_FEEDBACK_DURATION) {
            p.fill(0, 0, 0, 80);
            p.noStroke();
            p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
        } else {
            p.fill(255, 255, 255, 50);
            p.noStroke();
            p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
        }
        drawAppIcon(p, app, x + ICON_SIZE / 2, y + ICON_SIZE / 2, ICON_SIZE);
        if (isEditing) {
            p.fill(0xFF4444);
            p.noStroke();
            p.ellipse(x + ICON_SIZE - 8, y + 8, 16, 16);
            p.fill(textColor);
            p.strokeWeight(2);
            p.stroke(textColor);
            p.line(x + ICON_SIZE - 12, y + 4, x + ICON_SIZE - 4, y + 12);
            p.line(x + ICON_SIZE - 12, y + 12, x + ICON_SIZE - 4, y + 4);
        }
        p.fill(0, 150);
        p.textSize(11);
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 10) {
            displayName = displayName.substring(0, 9) + "...";
        }
        p.textAlign(p.CENTER, p.TOP);
        p.text(displayName, x + ICON_SIZE / 2, y + ICON_SIZE + 8);
        p.fill(255);
        p.text(displayName, x + ICON_SIZE / 2, y + ICON_SIZE + 7);
    }

    private void drawAppIcon(PApplet p, IApplication app, int centerX, int centerY, float iconSize) {
        if (app == null) {
            return;
        }
        processing.core.PImage icon = getCachedIcon(app, p);
        if (icon != null) {
            p.imageMode(p.CENTER);
            float padding = iconSize * 0.125f;
            float drawableSize = iconSize - padding * 2;
            p.image(icon, centerX, centerY, drawableSize, drawableSize);
            p.imageMode(p.CORNER);
        } else {
            p.rectMode(p.CENTER);
            p.fill(accentColor);
            p.noStroke();
            p.rect(centerX, centerY, iconSize * 0.625f, iconSize * 0.625f, 8);
            p.fill(textColor);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(iconSize * 0.4f);
            if (app.getName() != null && !app.getName().isEmpty()) {
                String initial = app.getName().substring(0, 1).toUpperCase();
                p.text(initial, centerX, centerY - 2);
            }
            p.rectMode(p.CORNER);
        }
    }

    private void drawPageIndicators(PApplet p) {
        HomePage currentPage = getCurrentPage();
        if (currentPage != null && currentPage.isAppLibraryPage()) {
            return;
        }

        // AppLibrary以外のページ数をカウント
        int normalPagesCount = 0;
        int currentNormalPageIndex = -1;
        for (int i = 0; i < homePages.size(); i++) {
            if (!homePages.get(i).isAppLibraryPage()) {
                if (i == currentPageIndex) {
                    currentNormalPageIndex = normalPagesCount;
                }
                normalPagesCount++;
            }
        }

        if (normalPagesCount <= 1) {
            return;
        }

        int dotSize = 7;
        int activeDotSize = 9;
        int spacing = 12;

        int totalWidth = (normalPagesCount - 1) * (dotSize + spacing) + activeDotSize;
        int currentX = (400 - totalWidth) / 2;

        int indicatorsY = DOCK_Y - 25; // ドックの上部に少し余白を持たせる

        p.noStroke();

        for (int i = 0; i < normalPagesCount; i++) {
            if (i == currentNormalPageIndex) {
                p.fill(255, 220);
                p.ellipse(currentX + activeDotSize / 2f, indicatorsY, activeDotSize, activeDotSize);
                currentX += activeDotSize + spacing;
            } else {
                p.fill(255, 100);
                p.ellipse(currentX + dotSize / 2f, indicatorsY, dotSize, dotSize);
                currentX += dotSize + spacing;
            }
        }
    }

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

    private Shortcut getShortcutAtPosition(int x, int y) {
        HomePage currentPage = getCurrentPage();
        return getShortcutAtPosition(x, y, currentPage);
    }

    private IApplication getAppAtPosition(int x, int y) {
        Shortcut shortcut = getShortcutAtPosition(x, y);
        return shortcut != null ? shortcut.getApplication() : null;
    }

    private Shortcut getShortcutAtPositionWithTransform(int mouseX, int mouseY) {
        // 最初に常時表示フィールドをチェック
        Shortcut dockShortcut = getDockShortcutAtPosition(mouseX, mouseY);
        if (dockShortcut != null) {
            return dockShortcut;
        }

        int basePageForOffset = isAnimating ? animationBasePageIndex : currentPageIndex;
        float totalOffset = -basePageForOffset * 400 + pageTransitionOffset;
        float transformedX = mouseX - totalOffset;
        int pageIndex = (int) Math.floor(transformedX / 400);
        int localX = (int) (transformedX - pageIndex * 400);
        if (pageIndex >= 0 && pageIndex < homePages.size()) {
            HomePage targetPage = homePages.get(pageIndex);
            return getShortcutAtPosition(localX, mouseY, targetPage);
        }
        return null;
    }

    private void openAppLibrary() {
        if (DEBUG) System.out.println("HomeScreen: Navigating to integrated App Library page");
        if (!homePages.isEmpty()) {
            int appLibraryPageIndex = homePages.size() - 1;
            if (appLibraryPageIndex != currentPageIndex && !isAnimating) {
                startPageTransition(appLibraryPageIndex);
            }
        }
    }

    private void launchApplication(IApplication app) {
        if (DEBUG) System.out.println("HomeScreen: Launching app: " + app.getName());
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

    private void launchApplicationWithAnimation(IApplication app, float iconX, float iconY, float iconSize) {
        if (DEBUG) {
            System.out.println("HomeScreen: Launching app with animation: " + app.getName());
            System.out.println("HomeScreen: Icon position: (" + iconX + ", " + iconY + "), size: " + iconSize);
        }
        if (kernel != null && kernel.getScreenManager() != null) {
            try {
                Screen appScreen = app.getEntryScreen(kernel);
                if (DEBUG) System.out.println("HomeScreen: Got app screen: " + appScreen.getScreenTitle());
                processing.core.PImage appIcon = null;
                processing.core.PApplet pApplet = kernel.getParentApplet();
                if (pApplet != null) {
                    appIcon = getCachedIcon(app, pApplet);
                    if (DEBUG) System.out.println("HomeScreen: Got cached app icon: " + (appIcon != null ? appIcon.width + "x" + appIcon.height : "null"));
                } else {
                    if (DEBUG) System.out.println("HomeScreen: No parent PApplet available for icon caching");
                }
                if (appIcon != null) {
                    if (DEBUG) System.out.println("HomeScreen: Calling pushScreenWithAnimation...");
                    kernel.getScreenManager().pushScreenWithAnimation(appScreen, iconX, iconY, iconSize, appIcon);
                } else {
                    if (DEBUG) System.out.println("HomeScreen: No icon available, using normal launch");
                    kernel.getScreenManager().pushScreen(appScreen);
                }
            } catch (Exception e) {
                System.err.println("HomeScreen: Failed to launch app with animation " + app.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void refreshApps() {
        if (DEBUG) System.out.println("HomeScreen: refreshApps() called - reinitializing pages...");

        // Complete state reset
        pageTransitionOffset = 0.0f;
        isAnimating = false;
        isDragging = false;
        isEditing = false;
        resetDragState();
        resetEdgeHoverState();

        // Clean up temporary edit page if exists
        temporaryEditPage = null;
        tempPageInsertIndex = -1;

        initializeHomePages();
        currentPageIndex = 0;

        if (DEBUG) System.out.println("HomeScreen: Refreshed home screen pages - total pages: " + homePages.size() + " - all state reset");
    }

    public void toggleEditMode() {
        isEditing = !isEditing;
        if (DEBUG) System.out.println("HomeScreen: Edit mode " + (isEditing ? "enabled" : "disabled"));

        if (isEditing) {
            // Enter edit mode - add temporary page
            addTemporaryEditPage();
        } else {
            // Exit edit mode - handle temporary page cleanup
            handleEditModeExit();
            resetDragState();
            if (DEBUG) System.out.println("HomeScreen: Reset drag state on edit mode exit");
        }
    }

    public void addNewPage() {
        HomePage newPage = new HomePage();
        homePages.add(newPage);
        if (DEBUG) System.out.println("HomeScreen: Added new page, total pages: " + homePages.size());
    }

    public HomePage getCurrentPage() {
        if (homePages.isEmpty() || currentPageIndex >= homePages.size()) {
            return null;
        }
        return homePages.get(currentPageIndex);
    }

    @Override
    public boolean onGesture(GestureEvent event) {
        if (kernel.getPopupManager().isPopupVisible()) {
            return kernel.getPopupManager().handleGesture(event);
        }

        if (DEBUG) {
            System.out.println("HomeScreen: Received gesture: " + event.getType() +
                " at (" + event.getCurrentX() + ", " + event.getCurrentY() + ")" +
                " - isAnimating: " + isAnimating +
                " - isDragging: " + isDragging +
                " - isEditing: " + isEditing +
                " - currentPage: " + currentPageIndex);
        }

        boolean result = false;
        switch (event.getType()) {
            case TAP:
                result = handleTap(event.getCurrentX(), event.getCurrentY());
                break;
            case LONG_PRESS:
                result = handleLongPress(event.getCurrentX(), event.getCurrentY());
                break;
            case DRAG_START:
                result = handleDragStart(event);
                break;
            case DRAG_MOVE:
                result = handleDragMove(event);
                break;
            case DRAG_END:
                result = handleDragEnd(event);
                break;
            case SWIPE_LEFT:
                result = handleSwipeLeft();
                break;
            case SWIPE_RIGHT:
                result = handleSwipeRight();
                break;
            case SWIPE_UP:
                result = handleSwipeUp(event);
                break;
            default:
                if (DEBUG) System.out.println("HomeScreen: Unhandled gesture type: " + event.getType());
                result = false;
        }

        if (DEBUG) System.out.println("HomeScreen: Gesture " + event.getType() + " handled: " + result);
        return result;
    }

    @Override
    public boolean isInBounds(int x, int y) {
        return kernel != null &&
                kernel.getScreenManager() != null &&
                kernel.getScreenManager().getCurrentScreen() == this;
    }

    @Override
    public int getPriority() {
        return 50;
    }

    private boolean handleTap(int x, int y) {
        if (DEBUG) System.out.println("HomeScreen: Handling tap at (" + x + ", " + y + ")");
        int[] coords = transformMouseCoordinates(x, y);
        int pageX = coords[0];
        int pageY = coords[1];
        int targetPageIndex = coords[2];
        if (targetPageIndex < 0 || targetPageIndex >= homePages.size()) {
            return false;
        }
        HomePage targetPage = homePages.get(targetPageIndex);
        if (DEBUG) System.out.println("HomeScreen: Transformed tap to page " + targetPageIndex + " at (" + pageX + ", " + pageY + ")");
        if (targetPage.isAppLibraryPage()) {
            return handleAppLibraryTap(pageX, pageY, targetPage);
        }

        if (targetPageIndex != currentPageIndex && !isAnimating) {
            startPageTransition(targetPageIndex);
            return true;
        }
        // 常時表示フィールドでのタップをチェック
        Shortcut dockShortcut = getDockShortcutAtPosition(x, y);
        if (dockShortcut != null) {
            if (isEditing) {
                if (isClickingDockDeleteButton(x, y, dockShortcut)) {
                    removeDockShortcut(dockShortcut);
                    saveCurrentLayout();
                }
            } else {
                // 常時表示フィールドのショートカットを起動
                int dockWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
                int dockStartX = (400 - dockWidth) / 2;
                float iconX = dockStartX + dockShortcut.getDockPosition() * (ICON_SIZE + ICON_SPACING) + ICON_SIZE / 2;
                float iconY = DOCK_Y + (DOCK_HEIGHT - ICON_SIZE) / 2 + ICON_SIZE / 2;
                launchApplicationWithAnimation(dockShortcut.getApplication(), iconX, iconY, ICON_SIZE);
            }
            return true;
        }

        if (targetPageIndex == currentPageIndex) {
            Shortcut tappedShortcut = getShortcutAtPosition(pageX, pageY, targetPage);
            if (tappedShortcut != null) {
                if (isEditing) {
                    if (isClickingDeleteButton(pageX, pageY, tappedShortcut)) {
                        removeShortcut(tappedShortcut);
                    }
                } else {
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
        if (isEditing) {
            if (DEBUG) System.out.println("HomeScreen: Tapped empty space in edit mode - exiting edit mode");
            toggleEditMode();
            return true;
        }
        return false;
    }

    private boolean handleAppLibraryTap(int x, int y, HomePage appLibraryPage) {
        int startY = 110;
        int listHeight = 600 - startY - 20;
        int itemHeight = 70;
        if (y >= startY && y <= startY + listHeight) {
            IApplication tappedApp = appLibraryPage.getApplicationAtPosition(x, y, startY, itemHeight);
            if (tappedApp != null) {
                if (DEBUG) System.out.println("HomeScreen: AppLibraryでアプリをタップ: " + tappedApp.getName());
                float iconX = 20 + 32;
                float iconY = startY + ((y - startY) / itemHeight) * itemHeight + itemHeight / 2;
                launchApplicationWithAnimation(tappedApp, iconX, iconY, 32);
                return true;
            }
        }
        return false;
    }

    private boolean handleLongPress(int x, int y) {
        if (DEBUG) System.out.println("HomeScreen: Handling long press at (" + x + ", " + y + ")");
        int[] coords = transformMouseCoordinates(x, y);
        int pageX = coords[0];
        int pageY = coords[1];
        int targetPageIndex = coords[2];
        if (targetPageIndex < 0 || targetPageIndex >= homePages.size()) {
            return false;
        }
        HomePage targetPage = homePages.get(targetPageIndex);
        if (targetPage.isAppLibraryPage()) {
            return handleAppLibraryLongPress(pageX, pageY, targetPage);
        }
        if (targetPageIndex == currentPageIndex && !isEditing) {
            toggleEditMode();
            return true;
        }
        return false;
    }

    private boolean handleAppLibraryLongPress(int x, int y, HomePage appLibraryPage) {
        int startY = 110;
        int listHeight = 600 - startY - 20;
        int itemHeight = 70;
        if (y >= startY && y <= startY + listHeight) {
            IApplication longPressedApp = appLibraryPage.getApplicationAtPosition(x, y, startY, itemHeight);
            if (longPressedApp != null) {
                if (DEBUG) System.out.println("HomeScreen: AppLibraryで長押し: " + longPressedApp.getName());
                showAddToHomePopup(longPressedApp);
                return true;
            }
        }
        return false;
    }

    private void showAddToHomePopup(IApplication app) {
        if (kernel.getPopupManager() != null) {
            PopupMenu popup = new PopupMenu(app.getName())
                    .addItem("ホーム画面に追加", () -> addShortcutToHomeScreen(app))
                    .addSeparator()
                    .addItem("キャンセル", () -> {});
            kernel.getPopupManager().showPopup(popup);
        } else {
            System.err.println("HomeScreen: ❌ PopupManagerが利用できません");
        }
    }

    public void addShortcutToHomeScreen(IApplication app) {
        if (app == null) return;
        HomePage targetPage = null;
        for (HomePage page : homePages) {
            if (!page.isAppLibraryPage() && !page.isFull()) {
                targetPage = page;
                break;
            }
        }
        if (targetPage == null) {
            int lastHomePageIndex = -1;
            for (int i = 0; i < homePages.size(); i++) {
                if (!homePages.get(i).isAppLibraryPage()) {
                    lastHomePageIndex = i;
                }
            }
            targetPage = new HomePage("Page " + (lastHomePageIndex + 2));
            homePages.add(homePages.size() - 1, targetPage);
        }
        if (targetPage.addShortcut(app)) {
            if (DEBUG) System.out.println("HomeScreen: Added shortcut for " + app.getName() + " to page " + targetPage.getPageName());
            // Cache the icon for the newly added app
            processing.core.PApplet pApplet = kernel.getParentApplet();
            if (pApplet != null) {
                cacheAppIcon(app, pApplet);
            }
            saveCurrentLayout();
        } else {
            System.err.println("HomeScreen: Failed to add shortcut for " + app.getName());
        }
    }

    private boolean handleSwipeLeft() {
        if (DEBUG) System.out.println("HomeScreen: Left swipe detected - next page");

        // Reset any edge hover states
        resetEdgeHoverState();

        if (currentPageIndex < homePages.size() - 1 && !isAnimating) {
            startPageTransition(currentPageIndex + 1);
            return true;
        }
        return false;
    }

    private boolean handleSwipeRight() {
        if (DEBUG) System.out.println("HomeScreen: Right swipe detected - previous page");

        // Reset any edge hover states
        resetEdgeHoverState();

        if (currentPageIndex > 0 && !isAnimating) {
            startPageTransition(currentPageIndex - 1);
            return true;
        }
        return false;
    }

    private boolean handleSwipeUp(GestureEvent event) {
        if (event.getStartY() >= 600 * 0.9f) {
            if (DEBUG) System.out.println("HomeScreen: Bottom swipe up detected - letting Kernel handle control center");
            return false;
        }
        if (DEBUG) System.out.println("HomeScreen: Up swipe detected - opening integrated App Library");
        openAppLibrary();
        return true;
    }

    private boolean handleDragStart(GestureEvent event) {
        HomePage currentPage = getCurrentPage();
        if (currentPage != null && currentPage.isAppLibraryPage()) {
            return handleAppLibraryScrollStart(event);
        }
        if (isEditing) {
            Shortcut clickedShortcut = getShortcutAtPositionWithTransform(event.getStartX(), event.getStartY());
            if (clickedShortcut != null) {
                startDragging(clickedShortcut, event.getStartX(), event.getStartY());
                if (DEBUG) System.out.println("HomeScreen: Started icon drag for " + clickedShortcut.getDisplayName());
                return true;
            }
        }
        return false;
    }

    private boolean handleDragMove(GestureEvent event) {
        HomePage currentPage = getCurrentPage();
        if (currentPage != null && currentPage.isAppLibraryPage()) {
            return handleAppLibraryScroll(event);
        }

        // Handle icon dragging in edit mode
        if (isDragging && draggedShortcut != null) {
            // Update drag position to current mouse position (screen coordinates)
            // This keeps the icon directly under the cursor
            draggedShortcut.setDragPosition(event.getCurrentX(), event.getCurrentY());

            // Check for edge detection during icon drag in edit mode
            handleDragEdgeDetection(event.getCurrentX(), event.getCurrentY());

            if (DEBUG) System.out.println("HomeScreen: Updating icon drag position - screenX: " + event.getCurrentX() + ", screenY: " + event.getCurrentY());
            return true;
        }

        // Handle page dragging when not dragging an icon
        if (isEditing) {
            return false; // In edit mode without icon drag, don't handle page drag
        }

        return handlePageDrag(event);
    }

    private boolean handlePageDrag(GestureEvent event) {
        int deltaX = event.getCurrentX() - event.getStartX();

        // Allow small drags even during animation to prevent gesture blocking
        // but prevent large drags that would interfere with ongoing animations
        if (isAnimating && Math.abs(deltaX) > 50) {
            if (DEBUG) System.out.println("HomeScreen: Large page drag ignored - animation in progress (deltaX: " + deltaX + ")");
            return false;
        }

        if (Math.abs(deltaX) > 10) {
            pageTransitionOffset = Math.max(-400, Math.min(400, deltaX));
            if (currentPageIndex == 0 && pageTransitionOffset > 0) {
                pageTransitionOffset *= 0.3f;
            } else if (currentPageIndex == homePages.size() - 1 && pageTransitionOffset < 0) {
                pageTransitionOffset *= 0.3f;
            }
            if (DEBUG) System.out.println("HomeScreen: Page drag offset: " + pageTransitionOffset + " (deltaX: " + deltaX + ", isAnimating: " + isAnimating + ")");
            return true;
        }
        return false;
    }

    private boolean handleDragEnd(GestureEvent event) {
        HomePage currentPage = getCurrentPage();
        if (currentPage != null && currentPage.isAppLibraryPage()) {
            return handleAppLibraryScrollEnd(event);
        }
        if (isDragging && draggedShortcut != null) {
            if (DEBUG) System.out.println("HomeScreen: Ending icon drag");
            handleShortcutDrop(event.getCurrentX(), event.getCurrentY());
            resetDragState(); // This will also reset edge hover state
            return true;
        }
        if (isEditing) {
            resetEdgeHoverState(); // Ensure edge state is reset even in edit mode
            return false;
        }
        return handlePageDragEnd(event);
    }

    private boolean handlePageDragEnd(GestureEvent event) {
        if (DEBUG) System.out.println("HomeScreen: Page drag end - offset: " + pageTransitionOffset +
            " - currentPage: " + currentPageIndex + " - totalPages: " + homePages.size() + " - isAnimating: " + isAnimating);

        if (Math.abs(pageTransitionOffset) < 50) {
            if (DEBUG) System.out.println("HomeScreen: Small offset, returning to current page");
            startReturnToCurrentPage();
            return true;
        }

        if (pageTransitionOffset > 50 && currentPageIndex > 0) {
            if (DEBUG) System.out.println("HomeScreen: Large positive offset, moving to previous page");
            startPageTransition(currentPageIndex - 1);
            return true;
        } else if (pageTransitionOffset < -50 && currentPageIndex < homePages.size() - 1) {
            if (DEBUG) System.out.println("HomeScreen: Large negative offset, moving to next page");
            startPageTransition(currentPageIndex + 1);
            return true;
        } else {
            if (DEBUG) System.out.println("HomeScreen: At boundary, returning to current page");
            startReturnToCurrentPage();
            return true;
        }
    }

    private void startReturnToCurrentPage() {
        targetPageIndex = currentPageIndex;
        isAnimating = true;
        animationStartTime = System.currentTimeMillis();
        animationProgress = 0.0f;
        startOffset = pageTransitionOffset;
        animationBasePageIndex = currentPageIndex;

        // Reset edge hover state when starting return animation
        resetEdgeHoverState();

        if (DEBUG) System.out.println("🎬 Starting return animation to current page " + currentPageIndex + ", startOffset=" + startOffset + ", basePageIndex=" + animationBasePageIndex);
    }

    private boolean handleAppLibraryScrollStart(GestureEvent event) {
        if (DEBUG) System.out.println("HomeScreen: AppLibrary scroll started");
        return true;
    }

    private boolean handleAppLibraryScroll(GestureEvent event) {
        HomePage currentPage = getCurrentPage();
        if (currentPage == null) return false;
        int deltaY = event.getCurrentY() - event.getStartY();
        int currentScrollOffset = currentPage.getScrollOffset();
        int newScrollOffset = currentScrollOffset - deltaY;
        int startY = 110;
        int listHeight = 600 - startY - 20;
        int itemHeight = 70;
        List<IApplication> apps = currentPage.getAllApplications();
        int maxScrollOffset = Math.max(0, apps.size() * itemHeight - listHeight);
        newScrollOffset = Math.max(0, Math.min(maxScrollOffset, newScrollOffset));
        currentPage.setScrollOffset(newScrollOffset);
        if (DEBUG) System.out.println("HomeScreen: AppLibrary scrolled to offset " + newScrollOffset);
        return true;
    }

    private boolean handleAppLibraryScrollEnd(GestureEvent event) {
        if (DEBUG) System.out.println("HomeScreen: AppLibrary scroll ended");
        return true;
    }

    private void drawAddToHomePopup(PApplet p) {
        if (appToAdd == null) return;

        // Simple popup for adding app to home screen
        // Note: This is a simple implementation. In a full implementation,
        // you might want to use the PopupManager for consistent styling

        int popupWidth = 300;
        int popupHeight = 150;
        int popupX = (p.width - popupWidth) / 2;
        int popupY = (p.height - popupHeight) / 2;

        // Background overlay
        p.fill(0, 0, 0, 128);
        p.noStroke();
        p.rect(0, 0, p.width, p.height);

        // Popup background
        p.fill(42, 42, 42);
        p.stroke(74, 144, 226);
        p.strokeWeight(2);
        p.rect(popupX, popupY, popupWidth, popupHeight, 8);

        // Title
        p.fill(255);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(16);
        p.text("ホーム画面に追加", popupX + popupWidth / 2, popupY + 40);

        // App name
        p.textSize(14);
        p.text(appToAdd.getName(), popupX + popupWidth / 2, popupY + 70);

        // Buttons would go here in a full implementation
        p.textSize(12);
        p.text("タップして追加", popupX + popupWidth / 2, popupY + 100);
    }

    /**
     * Preload all app icons into cache to avoid loading them every frame
     */
    private void preloadIcons(PApplet p) {
        if (iconsLoaded) return;

        if (DEBUG) System.out.println("HomeScreen: Preloading app icons...");

        for (HomePage page : homePages) {
            if (page.isAppLibraryPage()) {
                // Load icons for all applications in app library
                List<IApplication> allApps = page.getAllApplications();
                for (IApplication app : allApps) {
                    if (app != null && !iconCache.containsKey(app)) {
                        processing.core.PImage icon = app.getIcon(p);
                        if (icon != null) {
                            iconCache.put(app, icon);
                        }
                    }
                }
            } else {
                // Load icons for shortcuts on home pages
                List<Shortcut> shortcuts = page.getShortcuts();
                for (Shortcut shortcut : shortcuts) {
                    IApplication app = shortcut.getApplication();
                    if (app != null && !iconCache.containsKey(app)) {
                        processing.core.PImage icon = app.getIcon(p);
                        if (icon != null) {
                            iconCache.put(app, icon);
                        }
                    }
                }
            }
        }

        iconsLoaded = true;
        if (DEBUG) System.out.println("HomeScreen: Preloaded " + iconCache.size() + " app icons");
    }

    private void preloadIcons(PGraphics g) {
        if (iconsLoaded) return;

        if (DEBUG) System.out.println("HomeScreen: Preloading app icons (PGraphics)...");

        for (HomePage page : homePages) {
            if (page.isAppLibraryPage()) {
                // Load icons for all applications in app library
                List<IApplication> allApps = page.getAllApplications();
                for (IApplication app : allApps) {
                    if (app != null && !iconCache.containsKey(app)) {
                        processing.core.PImage icon = app.getIcon(g);
                        if (icon != null) {
                            iconCache.put(app, icon);
                        }
                    }
                }
            } else {
                // Load icons for shortcuts on home pages
                List<Shortcut> shortcuts = page.getShortcuts();
                for (Shortcut shortcut : shortcuts) {
                    IApplication app = shortcut.getApplication();
                    if (app != null && !iconCache.containsKey(app)) {
                        processing.core.PImage icon = app.getIcon(g);
                        if (icon != null) {
                            iconCache.put(app, icon);
                        }
                    }
                }
            }
        }

        iconsLoaded = true;
        if (DEBUG) System.out.println("HomeScreen: Preloaded " + iconCache.size() + " app icons (PGraphics)");
    }

    /**
     * Get cached icon for an application, or load it if not cached
     */
    private processing.core.PImage getCachedIcon(IApplication app, PApplet p) {
        if (app == null) return null;

        processing.core.PImage cached = iconCache.get(app);
        if (cached != null) {
            return cached;
        }

        // If not cached, load and cache it
        processing.core.PImage icon = app.getIcon(p);
        if (icon != null) {
            iconCache.put(app, icon);
        }
        return icon;
    }

    /**
     * Clear icon cache and reload icons (call when apps are added/removed)
     */
    public void refreshIconCache(PApplet p) {
        iconCache.clear();
        iconsLoaded = false;
        preloadIcons(p);
        if (DEBUG) System.out.println("HomeScreen: Icon cache refreshed");
    }

    /**
     * Add a single app icon to cache
     */
    public void cacheAppIcon(IApplication app, PApplet p) {
        if (app != null) {
            processing.core.PImage icon = app.getIcon(p);
            if (icon != null) {
                iconCache.put(app, icon);
                if (DEBUG) System.out.println("HomeScreen: Cached icon for " + app.getName());
            }
        }
    }

    /**
     * Add a temporary page when entering edit mode
     */
    private void addTemporaryEditPage() {
        if (temporaryEditPage != null) {
            if (DEBUG) System.out.println("HomeScreen: Temporary edit page already exists");
            return;
        }

        // Find the last non-AppLibrary page index
        int lastHomePageIndex = -1;
        for (int i = homePages.size() - 1; i >= 0; i--) {
            if (!homePages.get(i).isAppLibraryPage()) {
                lastHomePageIndex = i;
                break;
            }
        }

        // Create temporary page
        int pageNumber = lastHomePageIndex + 2; // +1 for 0-based index, +1 for next page
        temporaryEditPage = new HomePage("Page " + pageNumber);

        // Insert before AppLibrary page (which is always last)
        tempPageInsertIndex = homePages.size() - 1;
        homePages.add(tempPageInsertIndex, temporaryEditPage);

        if (DEBUG) System.out.println("HomeScreen: Added temporary edit page '" + temporaryEditPage.getPageName() + "' at index " + tempPageInsertIndex);
    }

    /**
     * Handle page cleanup when exiting edit mode
     */
    private void handleEditModeExit() {
        if (temporaryEditPage != null) {
            // Check if temporary page is empty
            if (temporaryEditPage.isEmpty()) {
                // Remove empty temporary page
                homePages.remove(temporaryEditPage);
                if (DEBUG) System.out.println("HomeScreen: Removed empty temporary page '" + temporaryEditPage.getPageName() + "'");

                // Adjust current page index if needed
                if (currentPageIndex >= tempPageInsertIndex) {
                    currentPageIndex = Math.max(0, currentPageIndex - 1);
                    if (DEBUG) System.out.println("HomeScreen: Adjusted current page index to " + currentPageIndex);
                }
            } else {
                // Keep non-empty page
                if (DEBUG) System.out.println("HomeScreen: Kept non-empty temporary page '" + temporaryEditPage.getPageName() + "'");
            }

            // Reset temporary page tracking
            temporaryEditPage = null;
            tempPageInsertIndex = -1;
        }

        // Remove all empty normal pages (excluding App Library page)
        removeEmptyPages();

        // Save layout after cleanup
        saveCurrentLayout();
    }

    /**
     * Remove all empty pages from the home screen (except App Library page)
     */
    public void removeEmptyPages() {
        if (DEBUG) System.out.println("HomeScreen: Starting empty page cleanup...");

        List<HomePage> pagesToRemove = new ArrayList<>();
        int removedCount = 0;

        for (int i = homePages.size() - 1; i >= 0; i--) {
            HomePage page = homePages.get(i);

            // Skip App Library page and first page (keep at least one home page)
            if (page.isAppLibraryPage() || (homePages.size() <= 2 && !page.isAppLibraryPage())) {
                continue;
            }

            // Remove empty normal pages
            if (page.isEmpty()) {
                pagesToRemove.add(page);

                // Adjust current page index if we're removing a page before current page
                if (i < currentPageIndex) {
                    currentPageIndex--;
                } else if (i == currentPageIndex) {
                    // If we're removing current page, move to previous page or stay at 0
                    currentPageIndex = Math.max(0, currentPageIndex - 1);
                }

                if (DEBUG) System.out.println("HomeScreen: Marked empty page '" + page.getPageName() + "' for removal (index " + i + ")");
                removedCount++;
            }
        }

        // Remove marked pages
        for (HomePage page : pagesToRemove) {
            homePages.remove(page);
        }

        // Ensure current page index is valid
        if (currentPageIndex >= homePages.size()) {
            currentPageIndex = Math.max(0, homePages.size() - 1);
        }

        if (removedCount > 0) {
            if (DEBUG) System.out.println("HomeScreen: Removed " + removedCount + " empty pages. Current page index adjusted to " + currentPageIndex);
            if (DEBUG) System.out.println("HomeScreen: Remaining pages: " + homePages.size());
        } else {
            if (DEBUG) System.out.println("HomeScreen: No empty pages found to remove");
        }
    }

    /**
     * Check if the given page is the temporary edit page
     */
    private boolean isTemporaryEditPage(HomePage page) {
        return temporaryEditPage != null && temporaryEditPage == page;
    }

    /**
     * Get the temporary edit page if it exists
     */
    public HomePage getTemporaryEditPage() {
        return temporaryEditPage;
    }

    /**
     * Draw hint for empty temporary edit page
     */
    private void drawEmptyEditPageHint(PApplet p, int startX, int startY) {
        // Draw a subtle hint for the empty page
        int centerX = 200;
        int centerY = 300;

        // Draw dotted border around the entire grid area
        p.stroke(255, 255, 255, 100);
        p.strokeWeight(2);
        p.noFill();

        // Draw dashed rectangle
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int gridHeight = GRID_ROWS * (ICON_SIZE + ICON_SPACING + 20) - ICON_SPACING;

        for (int i = 0; i < gridWidth; i += 10) {
            if ((i / 10) % 2 == 0) {
                p.line(startX + i, startY, startX + Math.min(i + 5, gridWidth), startY);
                p.line(startX + i, startY + gridHeight, startX + Math.min(i + 5, gridWidth), startY + gridHeight);
            }
        }
        for (int i = 0; i < gridHeight; i += 10) {
            if ((i / 10) % 2 == 0) {
                p.line(startX, startY + i, startX, startY + Math.min(i + 5, gridHeight));
                p.line(startX + gridWidth, startY + i, startX + gridWidth, startY + Math.min(i + 5, gridHeight));
            }
        }

        // Draw hint text
        p.fill(255, 255, 255, 150);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(16);
        p.text("新しいページ", centerX, centerY - 20);
        p.textSize(12);
        p.text("ここにアプリをドラッグして追加", centerX, centerY + 10);
        p.textSize(10);
        p.text("空のままにすると自動削除されます", centerX, centerY + 30);
    }

    /**
     * 常時表示フィールド（Dock）を描画する
     */
    private void drawDock(PApplet p) {
        HomePage currentPage = getCurrentPage();

        // AppLibraryページでは常時表示フィールドを表示しない
        if (currentPage == null || currentPage.isAppLibraryPage()) {
            return;
        }

        // 背景を描画
        p.fill(0, 0, 0, 120);
        p.noStroke();
        p.rect(0, DOCK_Y, 400, DOCK_HEIGHT, 12, 12, 0, 0);

        // グローバル常時表示フィールドのショートカットを描画
        int dockWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int dockStartX = (400 - dockWidth) / 2;
        int dockIconY = DOCK_Y + (DOCK_HEIGHT - ICON_SIZE) / 2;

        // 編集モード時の空のスロットを描画
        if (isEditing && isDragging) {
            drawDockDropTargets(p, dockStartX, dockIconY);
        }

        // ショートカットを描画
        for (int i = 0; i < globalDockShortcuts.size(); i++) {
            Shortcut shortcut = globalDockShortcuts.get(i);
            if (shortcut.isDragging()) continue; // ドラッグ中のショートカットはスキップ

            int x = dockStartX + i * (ICON_SIZE + ICON_SPACING);
            int y = dockIconY;

            // 編集モード時の揺れエフェクト
            if (isEditing) {
                x += (int) (Math.sin(System.currentTimeMillis() * 0.01 + shortcut.getShortcutId().hashCode()) * 2);
                y += (int) (Math.cos(System.currentTimeMillis() * 0.012 + shortcut.getShortcutId().hashCode()) * 1.5);
            }

            drawDockShortcut(p, shortcut, x, y);
        }
    }

    /**
     * 常時表示フィールドのショートカットを描画する
     */
    private void drawDockShortcut(PApplet p, Shortcut shortcut, int x, int y) {
        IApplication app = shortcut.getApplication();

        // 背景を描画
        p.fill(255, 255, 255, 80);
        p.noStroke();
        p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);

        // アイコンを描画
        drawAppIcon(p, app, x + ICON_SIZE / 2, y + ICON_SIZE / 2, ICON_SIZE);

        // 編集モード時の削除ボタン
        if (isEditing) {
            p.fill(0xFF4444);
            p.noStroke();
            p.ellipse(x + ICON_SIZE - 8, y + 8, 16, 16);
            p.fill(textColor);
            p.strokeWeight(2);
            p.stroke(textColor);
            p.line(x + ICON_SIZE - 12, y + 4, x + ICON_SIZE - 4, y + 12);
            p.line(x + ICON_SIZE - 12, y + 12, x + ICON_SIZE - 4, y + 4);
        }

        // 名前を描画
        p.fill(0, 150);
        p.textSize(11);
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 10) {
            displayName = displayName.substring(0, 9) + "...";
        }
        p.textAlign(p.CENTER, p.TOP);
        p.text(displayName, x + ICON_SIZE / 2, y + ICON_SIZE + 8);
        p.fill(255);
        p.text(displayName, x + ICON_SIZE / 2, y + ICON_SIZE + 7);
    }

    /**
     * 常時表示フィールドのドロップターゲットを描画する（編集モード時）
     */
    private void drawDockDropTargets(PApplet p, int startX, int startY) {
        p.stroke(accentColor, 150);
        p.strokeWeight(2);
        p.noFill();

        // 空のスロットにドロップターゲットを描画
        for (int i = 0; i < HomePage.MAX_DOCK_SHORTCUTS; i++) {
            if (i >= globalDockShortcuts.size() || globalDockShortcuts.get(i) == null) {
                int x = startX + i * (ICON_SIZE + ICON_SPACING);
                int y = startY;
                p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
            }
        }
    }

    /**
     * 常時表示フィールドの指定位置にあるショートカットを取得する
     */
    private Shortcut getDockShortcutAtPosition(int mouseX, int mouseY) {
        HomePage currentPage = getCurrentPage();

        // AppLibraryページまたは常時表示フィールドエリア外の場合はnull
        if (currentPage == null || currentPage.isAppLibraryPage() ||
            mouseY < DOCK_Y || mouseY > DOCK_Y + DOCK_HEIGHT) {
            return null;
        }

        int dockWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int dockStartX = (400 - dockWidth) / 2;

        for (int i = 0; i < globalDockShortcuts.size(); i++) {
            int x = dockStartX + i * (ICON_SIZE + ICON_SPACING);
            int y = DOCK_Y + (DOCK_HEIGHT - ICON_SIZE) / 2;

            if (mouseX >= x && mouseX <= x + ICON_SIZE &&
                mouseY >= y && mouseY <= y + ICON_SIZE) {
                return globalDockShortcuts.get(i);
            }
        }

        return null;
    }

    /**
     * 常時表示フィールドの削除ボタンがクリックされたかを確認する
     */
    private boolean isClickingDockDeleteButton(int mouseX, int mouseY, Shortcut shortcut) {
        if (!isEditing || !shortcut.isInDock()) return false;

        HomePage currentPage = getCurrentPage();
        if (currentPage == null || currentPage.isAppLibraryPage()) return false;

        int dockPosition = shortcut.getDockPosition();
        if (dockPosition < 0 || dockPosition >= globalDockShortcuts.size()) return false;

        int dockWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int dockStartX = (400 - dockWidth) / 2;
        int x = dockStartX + dockPosition * (ICON_SIZE + ICON_SPACING);
        int y = DOCK_Y + (DOCK_HEIGHT - ICON_SIZE) / 2;

        int deleteX = x + ICON_SIZE - 8;
        int deleteY = y + 8;

        return Math.sqrt(Math.pow(mouseX - deleteX, 2) + Math.pow(mouseY - deleteY, 2)) <= 8;
    }

    /**
     * 座標が常時表示フィールドエリア内かどうかを確認する
     */
    private boolean isInDockArea(int mouseY) {
        return mouseY >= DOCK_Y && mouseY <= DOCK_Y + DOCK_HEIGHT;
    }

    /**
     * 常時表示フィールド内のドロップ位置を計算する
     */
    private int getDockDropPosition(int mouseX) {
        int dockWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int dockStartX = (400 - dockWidth) / 2;

        if (mouseX < dockStartX) return 0;

        int position = (mouseX - dockStartX) / (ICON_SIZE + ICON_SPACING);
        return Math.min(position, HomePage.MAX_DOCK_SHORTCUTS - 1);
    }

    // ===========================================
    // グローバル常時表示フィールド管理メソッド
    // ===========================================

    /**
     * グローバル常時表示フィールドにショートカットを追加する
     */
    private boolean addDockShortcut(Shortcut shortcut) {
        if (shortcut == null || globalDockShortcuts.size() >= HomePage.MAX_DOCK_SHORTCUTS) {
            return false;
        }

        // 通常のグリッドから削除（もし存在する場合）
        for (HomePage page : homePages) {
            if (page.getShortcuts().contains(shortcut)) {
                page.removeShortcut(shortcut);
                break;
            }
        }

        // グローバルDockに追加
        globalDockShortcuts.add(shortcut);
        shortcut.setDockPosition(globalDockShortcuts.size() - 1);

        if (DEBUG) System.out.println("HomeScreen: Added shortcut " + shortcut.getDisplayName() +
                " to global dock position " + (globalDockShortcuts.size() - 1));
        return true;
    }

    /**
     * グローバル常時表示フィールドからショートカットを削除する
     */
    private boolean removeDockShortcut(Shortcut shortcut) {
        if (shortcut == null || !globalDockShortcuts.contains(shortcut)) {
            return false;
        }

        globalDockShortcuts.remove(shortcut);

        // 残りのショートカットの位置を再調整
        for (int i = 0; i < globalDockShortcuts.size(); i++) {
            globalDockShortcuts.get(i).setDockPosition(i);
        }

        shortcut.setDockPosition(-1);

        if (DEBUG) System.out.println("HomeScreen: Removed shortcut " + shortcut.getDisplayName() +
                " from global dock");
        return true;
    }

    /**
     * グローバル常時表示フィールド内でショートカットを移動する
     */
    private boolean moveDockShortcut(Shortcut shortcut, int newPosition) {
        if (shortcut == null || !globalDockShortcuts.contains(shortcut) ||
                newPosition < 0 || newPosition >= globalDockShortcuts.size()) {
            return false;
        }

        int oldPosition = globalDockShortcuts.indexOf(shortcut);
        if (oldPosition == newPosition) {
            return true; // 位置変更なし
        }

        // ショートカットを移動
        globalDockShortcuts.remove(oldPosition);
        globalDockShortcuts.add(newPosition, shortcut);

        // 位置を再調整
        for (int i = 0; i < globalDockShortcuts.size(); i++) {
            globalDockShortcuts.get(i).setDockPosition(i);
        }

        if (DEBUG) System.out.println("HomeScreen: Moved global dock shortcut " + shortcut.getDisplayName() +
                " from position " + oldPosition + " to " + newPosition);
        return true;
    }

    /**
     * 通常のグリッドからグローバル常時表示フィールドにショートカットを移動する
     */
    private boolean moveShortcutToDock(Shortcut shortcut) {
        if (shortcut == null || globalDockShortcuts.size() >= HomePage.MAX_DOCK_SHORTCUTS) {
            return false;
        }

        // 通常のグリッドから削除
        for (HomePage page : homePages) {
            if (page.getShortcuts().contains(shortcut)) {
                page.removeShortcut(shortcut);
                break;
            }
        }

        // グローバルDockに追加
        return addDockShortcut(shortcut);
    }

    /**
     * グローバル常時表示フィールドから通常のグリッドにショートカットを移動する
     */
    private boolean moveShortcutFromDock(Shortcut shortcut) {
        if (shortcut == null || !globalDockShortcuts.contains(shortcut)) {
            return false;
        }

        // グローバルDockから削除
        removeDockShortcut(shortcut);

        // 現在のページに追加は呼び出し元で行う
        return true;
    }

    // ===========================================
    // PGraphics overloads for drawing methods
    // ===========================================

    private void drawPageIndicators(PGraphics g) {
        HomePage currentPage = getCurrentPage();
        if (currentPage != null && currentPage.isAppLibraryPage()) {
            return;
        }

        if (homePages.size() <= 1) return;

        g.noStroke();
        int indicatorSize = 8;
        int spacing = 18;
        int totalWidth = homePages.size() * spacing - spacing;
        int startX = (400 - totalWidth) / 2;
        int y = 580;

        for (int i = 0; i < homePages.size(); i++) {
            int x = startX + i * spacing;
            if (i == currentPageIndex) {
                g.fill(accentColor);
                g.ellipse(x, y, indicatorSize, indicatorSize);
            } else {
                g.fill(100);
                g.ellipse(x, y, indicatorSize - 2, indicatorSize - 2);
            }
        }
    }

    private void drawDock(PGraphics g) {
        HomePage currentPage = getCurrentPage();

        // AppLibraryページでは常時表示フィールドを表示しない
        if (currentPage != null && currentPage.isAppLibraryPage()) {
            return;
        }

        // 背景を描画
        g.fill(0, 0, 0, 120);
        g.noStroke();
        g.rect(0, DOCK_Y, 400, DOCK_HEIGHT, 12, 12, 0, 0);

        int dockWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int dockStartX = (400 - dockWidth) / 2;
        int dockIconY = DOCK_Y + (DOCK_HEIGHT - ICON_SIZE) / 2;

        // 編集モード時のドロップターゲット描画
        if (isEditing && isDragging) {
            drawDockDropTargets(g, dockStartX, dockIconY);
        }

        // ショートカットを描画
        for (int i = 0; i < globalDockShortcuts.size(); i++) {
            Shortcut shortcut = globalDockShortcuts.get(i);
            if (shortcut != null && !shortcut.isDragging()) {
                int x = dockStartX + i * (ICON_SIZE + ICON_SPACING);
                int y = dockIconY;
                drawDockShortcut(g, shortcut, x, y);
            }
        }
    }

    private void drawDockShortcut(PGraphics g, Shortcut shortcut, int x, int y) {
        IApplication app = shortcut.getApplication();

        // 背景を描画
        g.fill(50, 100);
        g.noStroke();
        g.rect(x - 5, y - 5, ICON_SIZE + 10, ICON_SIZE + 10, 12);

        drawAppIcon(g, app, x + ICON_SIZE / 2, y + ICON_SIZE / 2, ICON_SIZE);

        // 編集モード時の削除ボタン
        if (isEditing) {
            g.fill(0xFF4444);
            g.noStroke();
            g.ellipse(x + ICON_SIZE - 8, y + 8, 16, 16);
            g.fill(textColor);
            g.strokeWeight(2);
            g.stroke(textColor);
            g.line(x + ICON_SIZE - 12, y + 4, x + ICON_SIZE - 4, y + 12);
            g.line(x + ICON_SIZE - 12, y + 12, x + ICON_SIZE - 4, y + 4);
        }

        // 名前を描画
        g.fill(0, 150);
        g.textSize(11);
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 10) {
            displayName = displayName.substring(0, 9) + "...";
        }
        g.textAlign(PConstants.CENTER, PConstants.TOP);
        g.text(displayName, x + ICON_SIZE / 2, y + ICON_SIZE + 8);
        g.fill(255);
        g.text(displayName, x + ICON_SIZE / 2, y + ICON_SIZE + 7);
    }

    private void drawDockDropTargets(PGraphics g, int startX, int startY) {
        g.stroke(accentColor, 150);
        g.strokeWeight(2);
        g.noFill();

        // 空のスロットにドロップターゲットを描画
        for (int i = 0; i < HomePage.MAX_DOCK_SHORTCUTS; i++) {
            if (i >= globalDockShortcuts.size() || globalDockShortcuts.get(i) == null) {
                int x = startX + i * (ICON_SIZE + ICON_SPACING);
                int y = startY;
                g.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
            }
        }
    }

    private void drawAddToHomePopup(PGraphics g) {
        if (appToAdd == null) return;

        // Simple popup for adding app to home screen
        g.fill(backgroundColor, 230);
        g.noStroke();
        g.rect(100, 250, 200, 100, 10);

        g.fill(textColor);
        g.textAlign(PConstants.CENTER, PConstants.CENTER);
        g.textSize(14);
        g.text("Add to Home Screen?", 200, 280);

        // Yes button
        g.fill(accentColor);
        g.rect(120, 310, 60, 30, 5);
        g.fill(255);
        g.text("Yes", 150, 325);

        // No button
        g.fill(100);
        g.rect(220, 310, 60, 30, 5);
        g.fill(255);
        g.text("No", 250, 325);
    }

    private void drawDraggedShortcutWithOffset(PGraphics g, Shortcut shortcut) {
        if (!shortcut.isDragging()) return;

        // Get the direct screen position (no coordinate transformation needed)
        int screenX = (int)(shortcut.getDragX() - ICON_SIZE / 2);
        int screenY = (int)(shortcut.getDragY() - ICON_SIZE / 2);

        // Semi-transparent background
        g.fill(backgroundColor, 200);
        g.noStroke();
        g.rect(screenX - 5, screenY - 5, ICON_SIZE + 10, ICON_SIZE + 10, 12);

        // Draw the app icon
        IApplication app = shortcut.getApplication();
        if (app != null) {
            drawAppIcon(g, app, screenX + ICON_SIZE / 2, screenY + ICON_SIZE / 2, ICON_SIZE);
        }

        // Draw text
        g.fill(textColor, 180);
        g.textAlign(PConstants.CENTER, PConstants.TOP);
        g.textSize(10);
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 8) {
            displayName = displayName.substring(0, 7) + "...";
        }
        g.text(displayName, screenX + ICON_SIZE / 2, screenY + ICON_SIZE + 5);
    }

    private void drawEdgeIndicators(PGraphics g) {
        if (!isHoveringAtEdge) return;

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - edgeHoverStartTime;
        float progress = Math.min(1.0f, (float) elapsed / EDGE_HOVER_DURATION);

        // Calculate pulsing opacity
        float baseOpacity = 100 + (50 * progress);
        float pulseOpacity = baseOpacity + (30 * (float) Math.sin(currentTime * 0.01));

        g.noStroke();

        if (canSwitchToLeft && currentPageIndex > 0) {
            // Draw left edge indicator
            g.fill(accentColor, (int) pulseOpacity);
            // Draw triangular indicator pointing left
            g.triangle(EDGE_THRESHOLD - 10, 300 - 20, EDGE_THRESHOLD - 10, 300 + 20, 10, 300);

            // Draw progress bar
            g.fill(255, (int) (150 * progress));
        }

        if (canSwitchToRight && currentPageIndex < homePages.size() - 1) {
            // Draw right edge indicator
            g.fill(accentColor, (int) pulseOpacity);
            // Draw triangular indicator pointing right
            g.triangle(400 - EDGE_THRESHOLD + 10, 300 - 20, 400 - EDGE_THRESHOLD + 10, 300 + 20, 390, 300);

            // Draw progress bar
            g.fill(255, (int) (150 * progress));
        }
    }

    private void drawDropTargets(PGraphics g, int startX, int startY) {
        HomePage currentPage = getCurrentPage();
        if (currentPage == null) return;
        g.stroke(accentColor, 150);
        g.strokeWeight(2);
        g.noFill();
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

    private void drawShortcut(PGraphics g, Shortcut shortcut, int x, int y) {
        IApplication app = shortcut.getApplication();
        if (shortcut.isDragging()) {
            return;
        }

        // 背景を描画
        g.fill(50, 100);
        g.noStroke();
        g.rect(x - 5, y - 5, ICON_SIZE + 10, ICON_SIZE + 10, 12);

        // アイコンを描画
        drawAppIcon(g, app, x + ICON_SIZE / 2, y + ICON_SIZE / 2, ICON_SIZE);
        if (isEditing) {
            g.fill(0xFF4444);
            g.noStroke();
            g.ellipse(x + ICON_SIZE - 8, y + 8, 16, 16);
            g.fill(textColor);
            g.strokeWeight(2);
            g.stroke(textColor);
            g.line(x + ICON_SIZE - 12, y + 4, x + ICON_SIZE - 4, y + 12);
            g.line(x + ICON_SIZE - 12, y + 12, x + ICON_SIZE - 4, y + 4);
        }

        // 名前を描画
        g.fill(0, 150);
        g.textSize(11);
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 10) {
            displayName = displayName.substring(0, 9) + "...";
        }
        g.textAlign(PConstants.CENTER, PConstants.TOP);
        g.text(displayName, x + ICON_SIZE / 2, y + ICON_SIZE + 8);
        g.fill(255);
        g.text(displayName, x + ICON_SIZE / 2, y + ICON_SIZE + 7);
    }

    private void drawAppIcon(PGraphics g, IApplication app, int centerX, int centerY, float iconSize) {
        if (app == null) {
            return;
        }
        // Note: PGraphics version cannot access icon cache, so use simplified rendering
        g.rectMode(PConstants.CENTER);
        g.fill(accentColor);
        g.noStroke();
        g.rect(centerX, centerY, iconSize * 0.625f, iconSize * 0.625f, 8);
        g.fill(textColor);
        g.textAlign(PConstants.CENTER, PConstants.CENTER);
        g.textSize(iconSize * 0.4f);
        if (app.getName() != null && !app.getName().isEmpty()) {
            String initial = app.getName().substring(0, 1).toUpperCase();
            g.text(initial, centerX, centerY - 2);
        } else {
            g.text("?", centerX, centerY - 2);
        }
        g.rectMode(PConstants.CORNER);
    }

    private void drawEmptyEditPageHint(PGraphics g, int startX, int startY) {
        // Draw a subtle hint for the empty page
        int centerX = 200;
        int centerY = 300;

        g.fill(100, 80);
        g.noStroke();
        g.textAlign(PConstants.CENTER, PConstants.CENTER);
        g.textSize(14);
        g.text("Drag apps here", centerX, centerY);

        // Draw dotted border
        g.stroke(100, 60);
        g.strokeWeight(2);
        g.noFill();

        // Draw dashed rectangle
        for (int i = 0; i < 360; i += 20) {
            if (i % 40 < 20) {
                float x1 = centerX + 80 * PApplet.cos(PApplet.radians(i));
                float y1 = centerY + 50 * PApplet.sin(PApplet.radians(i));
                float x2 = centerX + 80 * PApplet.cos(PApplet.radians(i + 10));
                float y2 = centerY + 50 * PApplet.sin(PApplet.radians(i + 10));
                g.line(x1, y1, x2, y2);
            }
        }
    }
}
