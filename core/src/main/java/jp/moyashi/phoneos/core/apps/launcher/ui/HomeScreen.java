package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.app.IApplication;
import processing.core.PApplet;
import processing.core.PImage;

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
 * This is the new implementation that replaces the original placeholder HomeScreen.
 * 
 * @author YourName
 * @version 2.0
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
    
    /** List of app shortcuts displayed on the home screen */
    private List<IApplication> homeScreenApps;
    
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
        this.homeScreenApps = new ArrayList<>();
        
        System.out.println("HomeScreen: New launcher home screen created");
    }
    
    /**
     * Initializes the home screen when it becomes active.
     * Sets up the app shortcuts and prepares the UI for display.
     */
    @Override
    public void setup() {
        isInitialized = true;
        loadHomeScreenApps();
        System.out.println("HomeScreen: Launcher home screen initialized with " + 
                          homeScreenApps.size() + " app shortcuts");
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
        
        // Draw app shortcuts grid
        drawAppGrid(p);
        
        // Draw navigation area
        drawNavigationArea(p);
        
        // Draw page indicator dots (if we had multiple pages)
        drawPageIndicator(p);
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
        
        // Check if click is in navigation area (swipe to app library)
        if (mouseY > (600 - NAV_AREA_HEIGHT)) {
            openAppLibrary();
            return;
        }
        
        // Check if click is on an app icon
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
     * Loads the apps that should be displayed as shortcuts on the home screen.
     * For now, this includes all loaded applications.
     */
    private void loadHomeScreenApps() {
        homeScreenApps.clear();
        
        if (kernel != null && kernel.getAppLoader() != null) {
            // Add all loaded apps except the launcher itself
            for (IApplication app : kernel.getAppLoader().getLoadedApps()) {
                if (!app.getApplicationId().equals("jp.moyashi.phoneos.core.apps.launcher")) {
                    homeScreenApps.add(app);
                }
            }
        }
        
        System.out.println("HomeScreen: Loaded " + homeScreenApps.size() + " app shortcuts");
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
     * Draws the grid of app shortcuts.
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawAppGrid(PApplet p) {
        int startY = 80; // Below status bar
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2; // Center the grid
        
        p.textAlign(p.CENTER, p.TOP);
        p.textSize(10);
        
        for (int i = 0; i < homeScreenApps.size() && i < (GRID_COLS * GRID_ROWS); i++) {
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            
            int x = startX + col * (ICON_SIZE + ICON_SPACING);
            int y = startY + row * (ICON_SIZE + ICON_SPACING + 15); // Extra space for app name
            
            IApplication app = homeScreenApps.get(i);
            
            // Draw app icon background
            p.fill(0x333333);
            p.stroke(0x555555);
            p.strokeWeight(1);
            p.rect(x, y, ICON_SIZE, ICON_SIZE, 12);
            
            // Try to draw the app icon (placeholder for now)
            drawAppIcon(p, app, x + ICON_SIZE/2, y + ICON_SIZE/2);
            
            // Draw app name
            p.fill(textColor);
            p.noStroke();
            String displayName = app.getName();
            if (displayName.length() > 8) {
                displayName = displayName.substring(0, 7) + "...";
            }
            p.text(displayName, x + ICON_SIZE/2, y + ICON_SIZE + 5);
        }
        
        // If no apps, show a message
        if (homeScreenApps.isEmpty()) {
            p.fill(textColor, 150);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text("No apps installed", 200, 300);
            p.textSize(12);
            p.text("Swipe up to access app library", 200, 320);
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
        
        // Draw swipe indicator
        p.stroke(textColor, 100);
        p.strokeWeight(2);
        p.noFill();
        p.arc(200, navY + 50, 30, 30, p.PI, p.TWO_PI);
        
        // Arrow up
        p.line(200, navY + 35, 195, navY + 40);
        p.line(200, navY + 35, 205, navY + 40);
    }
    
    /**
     * Draws page indicator dots (currently just one page).
     * 
     * @param p The PApplet instance for drawing
     */
    private void drawPageIndicator(PApplet p) {
        int dotY = 600 - NAV_AREA_HEIGHT - 20;
        
        p.fill(textColor, 100);
        p.noStroke();
        p.ellipse(200, dotY, 8, 8); // Current page dot
    }
    
    /**
     * Gets the application at the specified screen coordinates.
     * 
     * @param x The x coordinate
     * @param y The y coordinate
     * @return The IApplication at that position, or null if none
     */
    private IApplication getAppAtPosition(int x, int y) {
        int startY = 80;
        int gridWidth = GRID_COLS * (ICON_SIZE + ICON_SPACING) - ICON_SPACING;
        int startX = (400 - gridWidth) / 2;
        
        for (int i = 0; i < homeScreenApps.size() && i < (GRID_COLS * GRID_ROWS); i++) {
            int col = i % GRID_COLS;
            int row = i / GRID_COLS;
            
            int iconX = startX + col * (ICON_SIZE + ICON_SPACING);
            int iconY = startY + row * (ICON_SIZE + ICON_SPACING + 15);
            
            if (x >= iconX && x <= iconX + ICON_SIZE && 
                y >= iconY && y <= iconY + ICON_SIZE) {
                return homeScreenApps.get(i);
            }
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
     * Refreshes the home screen app list.
     */
    public void refreshApps() {
        loadHomeScreenApps();
        System.out.println("HomeScreen: Refreshed app shortcuts");
    }
}