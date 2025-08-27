package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.apps.launcher.model.HomePage;
import jp.moyashi.phoneos.core.apps.launcher.model.Shortcut;
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
public class AppLibraryScreen implements Screen {
    
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
    private IApplication longPressedApp;
    private boolean showingContextMenu;
    private static final long LONG_PRESS_DURATION = 500; // 500ms
    
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
        // Draw background
        p.background(backgroundColor);
        
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
        
        // Check if click is on an app item
        IApplication clickedApp = getAppAtPosition(mouseX, mouseY);
        if (clickedApp != null) {
            launchApplication(clickedApp);
        }
    }
    
    /**
     * Cleans up resources when the screen is deactivated.
     */
    @Override
    public void cleanup() {
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
        if (allApps == null || y < LIST_START_Y) {
            return null;
        }
        
        int adjustedY = y + scrollOffset - LIST_START_Y;
        int itemIndex = adjustedY / ITEM_HEIGHT;
        
        if (itemIndex >= 0 && itemIndex < allApps.size()) {
            return allApps.get(itemIndex);
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
}