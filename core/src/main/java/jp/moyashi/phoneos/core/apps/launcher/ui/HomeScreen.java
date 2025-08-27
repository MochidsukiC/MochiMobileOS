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
public class HomeScreen implements Screen {
    
    /** Reference to the OS kernel for accessing system services */
    private final Kernel kernel;
    
    /** Background color for the home screen */
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
    
    /** Page swiping */
    private float swipeStartX;
    private boolean isSwipingPages;
    private static final float SWIPE_THRESHOLD = 50.0f;
    
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
        isInitialized = true;
        System.out.println("🚀 HomeScreen: Initializing multi-page launcher...");
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
    }
    
    /**
     * Draws the home screen interface.
     * Renders the background, app shortcuts grid, navigation hints, and system information.
     * 
     * @param p The PApplet instance for drawing operations
     */
    @Override
    public void draw(PApplet p) {
        // Draw background
        p.background(backgroundColor);
        
        // Draw status bar
        drawStatusBar(p);
        
        // Draw current page shortcuts
        drawCurrentPage(p);
        
        // Draw navigation area
        drawNavigationArea(p);
        
        // Draw page indicator dots
        drawPageIndicators(p);
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
        
        // Check if click is on a shortcut
        Shortcut clickedShortcut = getShortcutAtPosition(mouseX, mouseY);
        if (clickedShortcut != null) {
            if (isEditing) {
                // In edit mode, check if clicking delete button
                if (isClickingDeleteButton(mouseX, mouseY, clickedShortcut)) {
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
            if (!isEditing) {
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
     * Handles mouse drag events for shortcut dragging and page swiping.
     */
    public void mouseDragged(int mouseX, int mouseY) {
        if (isDragging && draggedShortcut != null) {
            // Update drag position
            draggedShortcut.setDragPosition(mouseX - dragOffsetX, mouseY - dragOffsetY);
        } else if (!isEditing && Math.abs(mouseX - swipeStartX) > SWIPE_THRESHOLD) {
            // Page swiping
            isSwipingPages = true;
        }
    }
    
    /**
     * Handles mouse release events.
     */
    public void mouseReleased(int mouseX, int mouseY) {
        long pressDuration = System.currentTimeMillis() - touchStartTime;
        
        if (isDragging && draggedShortcut != null) {
            // Handle drop
            handleShortcutDrop(mouseX, mouseY);
        } else if (isSwipingPages) {
            // Handle page swipe
            handlePageSwipe(mouseX);
        } else if (pressDuration >= LONG_PRESS_DURATION && !longPressTriggered) {
            // Long press detected - toggle edit mode
            if (!isEditing) {
                toggleEditMode();
            }
        }
        
        // Reset states
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
        
        // Calculate offset from shortcut position to mouse
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        
        int shortcutX = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
        int shortcutY = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 15);
        
        dragOffsetX = mouseX - shortcutX;
        dragOffsetY = mouseY - shortcutY;
        
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
                    System.out.println("HomeScreen: Moved shortcut to (" + gridPos[0] + ", " + gridPos[1] + ")");
                } else {
                    System.out.println("HomeScreen: Cannot move shortcut - position occupied or invalid");
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
            System.out.println("HomeScreen: Removed shortcut for " + shortcut.getDisplayName());
        }
    }
    
    /**
     * Cleans up resources when the screen is deactivated.
     */
    @Override
    public void cleanup() {
        isInitialized = false;
        resetDragState();
        isEditing = false;
        System.out.println("HomeScreen: Launcher home screen cleaned up");
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
     * Initializes home pages and distributes apps across them.
     */
    private void initializeHomePages() {
        homePages.clear();
        
        // Create first page
        HomePage firstPage = new HomePage("Home");
        homePages.add(firstPage);
        
        if (kernel != null && kernel.getAppLoader() != null) {
            // Add all loaded apps except the launcher itself
            List<IApplication> availableApps = new ArrayList<>();
            for (IApplication app : kernel.getAppLoader().getLoadedApps()) {
                if (!app.getApplicationId().equals("jp.moyashi.phoneos.core.apps.launcher")) {
                    availableApps.add(app);
                }
            }
            
            // Distribute apps across pages
            HomePage currentPage = firstPage;
            for (IApplication app : availableApps) {
                if (currentPage.isFull()) {
                    // Create new page if current is full
                    currentPage = new HomePage();
                    homePages.add(currentPage);
                }
                currentPage.addShortcut(app);
            }
        }
        
        System.out.println("HomeScreen: Initialized " + homePages.size() + " pages with shortcuts");
    }
    
    /**
     * Draws the status bar at the top of the screen.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawStatusBar(PApplet p) {
        p.fill(textColor, 180); // Semi-transparent text
        p.textAlign(p.LEFT, p.TOP);
        p.textSize(12);
        
        // Current time
        if (kernel != null && kernel.getSystemClock() != null) {
            p.text(kernel.getSystemClock().getFormattedTime(), 15, 15);
        }
        
        // System status
        p.textAlign(p.RIGHT, p.TOP);
        p.text("MochiOS", 385, 15);
        
        // Status indicator
        p.fill(isInitialized ? 0x4CAF50 : 0xFF9800); // Green if ready, orange if not
        p.noStroke();
        p.ellipse(370, 20, 8, 8);
    }
    
    /**
     * Draws the current page of shortcuts.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawCurrentPage(PApplet p) {
        if (homePages.isEmpty() || currentPageIndex >= homePages.size()) {
            // No pages or invalid index, show message
            p.fill(textColor, 150);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text("No apps installed", 200, 300);
            p.textSize(12);
            p.text("Swipe up to access app library", 200, 320);
            return;
        }
        
        HomePage currentPage = homePages.get(currentPageIndex);
        
        int startY = 80; // Below status bar
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2; // Center the grid
        
        p.textAlign(p.CENTER, p.TOP);
        p.textSize(10);
        
        // First draw non-dragged shortcuts
        for (Shortcut shortcut : currentPage.getShortcuts()) {
            if (shortcut.isDragging()) continue; // Draw dragged shortcuts last
            
            int x = startX + shortcut.getGridX() * (ICON_SIZE + ICON_SPACING);
            int y = startY + shortcut.getGridY() * (ICON_SIZE + ICON_SPACING + 15); // Extra space for app name
            
            // Apply wiggle animation if in edit mode and not dragging
            if (isEditing && !shortcut.isDragging()) {
                x += (int) (Math.sin(System.currentTimeMillis() * 0.01 + shortcut.getShortcutId().hashCode()) * 2);
                y += (int) (Math.cos(System.currentTimeMillis() * 0.012 + shortcut.getShortcutId().hashCode()) * 1.5);
            }
            
            // Draw shortcut
            drawShortcut(p, shortcut, x, y);
        }
        
        // Then draw dragged shortcuts (on top)
        for (Shortcut shortcut : currentPage.getShortcuts()) {
            if (!shortcut.isDragging()) continue;
            
            // Dragged shortcuts use their drag position
            drawShortcut(p, shortcut, 0, 0); // Position handled inside drawShortcut
        }
        
        // Draw drop target indicators if dragging
        if (isDragging) {
            drawDropTargets(p, startX, startY);
        }
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
        
        // If this shortcut is being dragged, use its drag position
        if (shortcut.isDragging()) {
            x = (int) shortcut.getDragX();
            y = (int) shortcut.getDragY();
        }
        
        // Draw app icon background
        if (shortcut.isDragging()) {
            p.fill(0x555555, 200); // Semi-transparent when dragging
        } else {
            p.fill(0x333333);
        }
        p.stroke(0x555555);
        p.strokeWeight(1);
        p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
        
        // Draw app icon
        drawAppIcon(p, app, x + ICON_SIZE/2, y + ICON_SIZE/2);
        
        // Draw delete button if in edit mode and not dragging
        if (isEditing && !shortcut.isDragging()) {
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
        
        // Draw app name (only if not dragging or show faded)
        if (shortcut.isDragging()) {
            p.fill(textColor, 150);
        } else {
            p.fill(textColor);
        }
        p.noStroke();
        String displayName = shortcut.getDisplayName();
        if (displayName.length() > 8) {
            displayName = displayName.substring(0, 7) + "...";
        }
        p.text(displayName, x + ICON_SIZE/2, y + ICON_SIZE + 5);
        
        // Draw drop shadow if dragging
        if (shortcut.isDragging()) {
            p.fill(0x000000, 100);
            p.noStroke();
            p.rect(x + 2, y + 2, ICON_SIZE, ICON_SIZE, 12);
        }
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
        
        int dotY = 600 - NAV_AREA_HEIGHT - 20;
        int totalWidth = homePages.size() * 16 - 4; // 12px dot + 4px spacing
        int startX = (400 - totalWidth) / 2;
        
        for (int i = 0; i < homePages.size(); i++) {
            if (i == currentPageIndex) {
                p.fill(textColor, 200); // Bright for current page
            } else {
                p.fill(textColor, 100); // Dim for other pages
            }
            p.noStroke();
            p.ellipse(startX + i * 16, dotY, 8, 8);
        }
    }
    
    /**
     * Gets the shortcut at the specified screen coordinates.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The Shortcut at that position, or null if none
     */
    private Shortcut getShortcutAtPosition(int x, int y) {
        if (homePages.isEmpty() || currentPageIndex >= homePages.size()) {
            return null;
        }
        
        HomePage currentPage = homePages.get(currentPageIndex);
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        
        for (Shortcut shortcut : currentPage.getShortcuts()) {
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
        initializeHomePages();
        currentPageIndex = 0;
        isEditing = false;
        System.out.println("HomeScreen: Refreshed home screen pages");
    }
    
    /**
     * Toggles edit mode.
     */
    public void toggleEditMode() {
        isEditing = !isEditing;
        System.out.println("HomeScreen: Edit mode " + (isEditing ? "enabled" : "disabled"));
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
}