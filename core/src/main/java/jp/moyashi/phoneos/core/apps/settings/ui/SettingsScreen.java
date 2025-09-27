package jp.moyashi.phoneos.core.apps.settings.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.apps.settings.SettingsApp;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PConstants;

/**
 * Main settings screen displaying system configuration options.
 * Provides access to various system settings and displays system information.
 * This serves as a test screen for the launcher functionality.
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class SettingsScreen implements Screen {
    
    /** Reference to the OS kernel */
    private final Kernel kernel;
    
    /** Reference to the settings app */
    private final SettingsApp settingsApp;
    
    /** UI Colors */
    private final int backgroundColor = 0x1A1A1A;
    private final int textColor = 0xFFFFFF;
    private final int accentColor = 0x4A90E2;
    private final int itemColor = 0x2A2A2A;
    
    /** Screen state */
    private boolean isInitialized = false;
    
    /** Settings items configuration */
    private static final String[] SETTING_ITEMS = {
        "Display Settings",
        "Sound & Vibration", 
        "Apps & Notifications",
        "Storage",
        "Battery",
        "About System"
    };
    
    private static final String[] SETTING_DESCRIPTIONS = {
        "Brightness, wallpaper, theme",
        "Volume, ringtones, alerts",
        "App permissions, notifications",
        "Storage usage and management",
        "Battery usage and optimization",
        "System version and information"
    };
    
    private static final int ITEM_HEIGHT = 70;
    private static final int ITEM_PADDING = 15;
    
    /**
     * Creates a new SettingsScreen instance.
     * 
     * @param kernel The OS kernel instance
     * @param settingsApp The settings application instance
     */
    public SettingsScreen(Kernel kernel, SettingsApp settingsApp) {
        this.kernel = kernel;
        this.settingsApp = settingsApp;
        
        System.out.println("SettingsScreen: Settings screen created");
    }
    
    /**
     * Initializes the settings screen.
     */
    @Override
    public void setup(processing.core.PApplet p) {
        isInitialized = true;
        System.out.println("SettingsScreen: Settings screen initialized");
    }

    /**
     * Initializes the settings screen (PGraphics version).
     */
    public void setup(PGraphics g) {
        isInitialized = true;
        System.out.println("SettingsScreen: Settings screen initialized (PGraphics)");
    }
    
    /**
     * Draws the settings screen interface.
     *
     * @param p The PApplet instance for drawing operations
     */
    @Override
    public void draw(PApplet p) {
        // Draw background
        p.background(backgroundColor);

        // Draw header
        drawHeader(p);

        // Draw settings items
        drawSettingsItems(p);

        // Draw system info
        drawSystemInfo(p);
    }

    /**
     * Draws the settings screen interface (PGraphics version).
     *
     * @param g The PGraphics instance for drawing operations
     */
    public void draw(PGraphics g) {
        // Draw background
        g.background(backgroundColor);

        // Draw header
        drawHeader(g);

        // Draw settings items
        drawSettingsItems(g);

        // Draw system info
        drawSystemInfo(g);
    }
    
    /**
     * Handles mouse press events.
     * 
     * @param mouseX The x-coordinate of the mouse press
     * @param mouseY The y-coordinate of the mouse press
     */
    @Override
    public void mousePressed(processing.core.PApplet p, int mouseX, int mouseY) {
        System.out.println("SettingsScreen: Touch at (" + mouseX + ", " + mouseY + ")");
        
        // Check if clicking back button
        if (mouseY < 60 && mouseX < 100) {
            goBack();
            return;
        }
        
        // Check if clicking on a settings item
        int clickedItem = getSettingsItemAtPosition(mouseY);
        if (clickedItem >= 0) {
            handleSettingsItemClick(clickedItem);
        }
    }
    
    /**
     * Cleans up resources when screen is deactivated.
     */
    @Override
    public void cleanup(processing.core.PApplet p) {
        isInitialized = false;
        System.out.println("SettingsScreen: Settings screen cleaned up");
    }
    
    /**
     * Gets the title of this screen.
     * 
     * @return The screen title
     */
    @Override
    public String getScreenTitle() {
        return "Settings";
    }
    
    /**
     * Draws the header section.
     *
     * @param p The PApplet instance for drawing
     */
    private void drawHeader(PApplet p) {
        // Header background
        p.fill(0x2A2A2A);
        p.noStroke();
        p.rect(0, 0, 400, 60);

        // Back arrow
        p.stroke(textColor);
        p.strokeWeight(2);
        p.line(20, 30, 30, 20);
        p.line(20, 30, 30, 40);

        // Title
        p.fill(textColor);
        p.noStroke();
        p.textAlign(p.LEFT, p.CENTER);
        p.textSize(20);
        p.text("Settings", 50, 30);

        // Settings icon
        p.fill(accentColor);
        p.textAlign(p.RIGHT, p.CENTER);
        p.textSize(16);
        p.text("⚙", 380, 30);

        // Separator line
        p.stroke(0x333333);
        p.strokeWeight(1);
        p.line(0, 59, 400, 59);
    }

    /**
     * Draws the header section (PGraphics version).
     *
     * @param g The PGraphics instance for drawing
     */
    private void drawHeader(PGraphics g) {
        // Header background
        g.fill(0x2A2A2A);
        g.noStroke();
        g.rect(0, 0, 400, 60);

        // Back arrow
        g.stroke(textColor);
        g.strokeWeight(2);
        g.line(20, 30, 30, 20);
        g.line(20, 30, 30, 40);

        // Title
        g.fill(textColor);
        g.noStroke();
        g.textAlign(PConstants.LEFT, PConstants.CENTER);
        g.textSize(20);
        g.text("Settings", 50, 30);

        // Settings icon
        g.fill(accentColor);
        g.textAlign(PConstants.RIGHT, PConstants.CENTER);
        g.textSize(16);
        g.text("⚙", 380, 30);

        // Separator line
        g.stroke(0x333333);
        g.strokeWeight(1);
        g.line(0, 59, 400, 59);
    }
    
    /**
     * Draws the list of settings items.
     *
     * @param p The PApplet instance for drawing
     */
    private void drawSettingsItems(PApplet p) {
        int startY = 80;

        for (int i = 0; i < SETTING_ITEMS.length; i++) {
            int itemY = startY + i * ITEM_HEIGHT;

            // Item background
            p.fill(itemColor);
            p.noStroke();
            p.rect(ITEM_PADDING, itemY, 400 - 2 * ITEM_PADDING, ITEM_HEIGHT - 5);

            // Item icon (placeholder)
            p.fill(accentColor);
            p.ellipse(ITEM_PADDING + 25, itemY + ITEM_HEIGHT/2, 30, 30);

            // Item icon text
            p.fill(textColor);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            String iconText = getIconForSetting(i);
            p.text(iconText, ITEM_PADDING + 25, itemY + ITEM_HEIGHT/2 - 2);

            // Item title
            p.fill(textColor);
            p.textAlign(p.LEFT, p.TOP);
            p.textSize(16);
            p.text(SETTING_ITEMS[i], ITEM_PADDING + 55, itemY + 15);

            // Item description
            p.fill(textColor, 150);
            p.textSize(12);
            p.text(SETTING_DESCRIPTIONS[i], ITEM_PADDING + 55, itemY + 35);

            // Arrow indicator
            p.fill(textColor, 100);
            p.textAlign(p.RIGHT, p.CENTER);
            p.textSize(20);
            p.text("›", 400 - ITEM_PADDING - 10, itemY + ITEM_HEIGHT/2);
        }
    }

    /**
     * Draws the list of settings items (PGraphics version).
     *
     * @param g The PGraphics instance for drawing
     */
    private void drawSettingsItems(PGraphics g) {
        int startY = 80;

        for (int i = 0; i < SETTING_ITEMS.length; i++) {
            int itemY = startY + i * ITEM_HEIGHT;

            // Item background
            g.fill(itemColor);
            g.noStroke();
            g.rect(ITEM_PADDING, itemY, 400 - 2 * ITEM_PADDING, ITEM_HEIGHT - 5);

            // Item icon (placeholder)
            g.fill(accentColor);
            g.ellipse(ITEM_PADDING + 25, itemY + ITEM_HEIGHT/2, 30, 30);

            // Item icon text
            g.fill(textColor);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.textSize(16);
            String iconText = getIconForSetting(i);
            g.text(iconText, ITEM_PADDING + 25, itemY + ITEM_HEIGHT/2 - 2);

            // Item title
            g.fill(textColor);
            g.textAlign(PConstants.LEFT, PConstants.TOP);
            g.textSize(16);
            g.text(SETTING_ITEMS[i], ITEM_PADDING + 55, itemY + 15);

            // Item description
            g.fill(textColor, 150);
            g.textSize(12);
            g.text(SETTING_DESCRIPTIONS[i], ITEM_PADDING + 55, itemY + 35);

            // Arrow indicator
            g.fill(textColor, 100);
            g.textAlign(PConstants.RIGHT, PConstants.CENTER);
            g.textSize(20);
            g.text("›", 400 - ITEM_PADDING - 10, itemY + ITEM_HEIGHT/2);
        }
    }
    
    /**
     * Draws system information at the bottom.
     *
     * @param p The PApplet instance for drawing
     */
    private void drawSystemInfo(PApplet p) {
        int infoY = 500;

        // System info background
        p.fill(itemColor);
        p.noStroke();
        p.rect(ITEM_PADDING, infoY, 400 - 2 * ITEM_PADDING, 80);

        // System info text
        p.fill(textColor);
        p.textAlign(p.LEFT, p.TOP);
        p.textSize(14);
        p.text("MochiMobileOS", ITEM_PADDING + 15, infoY + 15);

        p.fill(textColor, 150);
        p.textSize(12);
        p.text("Version 1.0.0 (Build 1)", ITEM_PADDING + 15, infoY + 35);
        p.text("Processing 4.4.4", ITEM_PADDING + 15, infoY + 50);

        // Runtime info
        if (kernel != null) {
            long uptime = kernel.getSystemClock() != null ?
                System.currentTimeMillis() - kernel.getSystemClock().getStartTime() : 0;
            int uptimeSeconds = (int) (uptime / 1000);
            p.text("Uptime: " + uptimeSeconds + "s", ITEM_PADDING + 200, infoY + 35);

            if (kernel.getAppLoader() != null) {
                int appCount = kernel.getAppLoader().getLoadedApps().size();
                p.text("Loaded Apps: " + appCount, ITEM_PADDING + 200, infoY + 50);
            }
        }
    }

    /**
     * Draws system information at the bottom (PGraphics version).
     *
     * @param g The PGraphics instance for drawing
     */
    private void drawSystemInfo(PGraphics g) {
        int infoY = 500;

        // System info background
        g.fill(itemColor);
        g.noStroke();
        g.rect(ITEM_PADDING, infoY, 400 - 2 * ITEM_PADDING, 80);

        // System info text
        g.fill(textColor);
        g.textAlign(PConstants.LEFT, PConstants.TOP);
        g.textSize(14);
        g.text("MochiMobileOS", ITEM_PADDING + 15, infoY + 15);

        g.fill(textColor, 150);
        g.textSize(12);
        g.text("Version 1.0.0 (Build 1)", ITEM_PADDING + 15, infoY + 35);
        g.text("Processing 4.4.4", ITEM_PADDING + 15, infoY + 50);

        // Runtime info
        if (kernel != null) {
            long uptime = kernel.getSystemClock() != null ?
                System.currentTimeMillis() - kernel.getSystemClock().getStartTime() : 0;
            int uptimeSeconds = (int) (uptime / 1000);
            g.text("Uptime: " + uptimeSeconds + "s", ITEM_PADDING + 200, infoY + 35);

            if (kernel.getAppLoader() != null) {
                int appCount = kernel.getAppLoader().getLoadedApps().size();
                g.text("Loaded Apps: " + appCount, ITEM_PADDING + 200, infoY + 50);
            }
        }
    }
    
    /**
     * Gets the icon character for a settings item.
     * 
     * @param index The settings item index
     * @return The icon character
     */
    private String getIconForSetting(int index) {
        switch (index) {
            case 0: return "⚡"; // Display
            case 1: return "♪";  // Sound
            case 2: return "📱"; // Apps
            case 3: return "💾"; // Storage
            case 4: return "🔋"; // Battery
            case 5: return "ℹ";  // About
            default: return "•";
        }
    }
    
    /**
     * Gets the settings item at the specified Y position.
     * 
     * @param mouseY The Y coordinate
     * @return The item index, or -1 if none
     */
    private int getSettingsItemAtPosition(int mouseY) {
        int startY = 80;
        
        if (mouseY < startY) return -1;
        
        int itemIndex = (mouseY - startY) / ITEM_HEIGHT;
        
        if (itemIndex >= 0 && itemIndex < SETTING_ITEMS.length) {
            return itemIndex;
        }
        
        return -1;
    }
    
    /**
     * Handles clicks on settings items.
     * 
     * @param itemIndex The clicked item index
     */
    private void handleSettingsItemClick(int itemIndex) {
        String itemName = SETTING_ITEMS[itemIndex];
        System.out.println("SettingsScreen: Clicked on " + itemName);
        
        // For now, just show a message - in a real app, we'd navigate to sub-screens
        switch (itemIndex) {
            case 0:
                System.out.println("SettingsScreen: Display settings would open here");
                break;
            case 1:
                System.out.println("SettingsScreen: Sound settings would open here");
                break;
            case 2:
                System.out.println("SettingsScreen: App settings would open here");
                break;
            case 3:
                System.out.println("SettingsScreen: Storage settings would open here");
                break;
            case 4:
                System.out.println("SettingsScreen: Battery settings would open here");
                break;
            case 5:
                System.out.println("SettingsScreen: About system would open here");
                break;
        }
    }
    
    /**
     * Goes back to the previous screen.
     */
    private void goBack() {
        System.out.println("SettingsScreen: Going back");
        
        if (kernel != null && kernel.getScreenManager() != null) {
            kernel.getScreenManager().popScreen();
        }
    }
}