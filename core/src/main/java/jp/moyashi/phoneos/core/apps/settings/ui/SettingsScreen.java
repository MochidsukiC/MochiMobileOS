package jp.moyashi.phoneos.core.apps.settings.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.apps.settings.SettingsApp;
import processing.core.PApplet;
import processing.core.PGraphics;

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
    
    /** UI Colors (ThemeEngine      E   */
    private int backgroundColor;
    private int textColor;
    private int accentColor;
    private int itemColor;
    
    /** Screen state */
    private boolean isInitialized = false;
    private boolean showAppearancePanel = false;

    // Appearance:     E     E   E 
    private jp.moyashi.phoneos.core.ui.components.Panel appearancePanel;
    // Tone
    private jp.moyashi.phoneos.core.ui.components.Button btnToneLight;
    private jp.moyashi.phoneos.core.ui.components.Button btnToneDark;
    private jp.moyashi.phoneos.core.ui.components.Button btnToneAuto;
    // Family
    private jp.moyashi.phoneos.core.ui.components.Button btnFamWhite;
    private jp.moyashi.phoneos.core.ui.components.Button btnFamOrange;
    private jp.moyashi.phoneos.core.ui.components.Button btnFamYellow;
    
    private jp.moyashi.phoneos.core.ui.components.Button btnFamPink;
    private jp.moyashi.phoneos.core.ui.components.Button btnFamGreen;
    private jp.moyashi.phoneos.core.ui.components.Button btnFamAqua;
    private jp.moyashi.phoneos.core.ui.components.Button btnFamBlack;
    // Corners
    private jp.moyashi.phoneos.core.ui.components.Button btnCornerCompact;
    private jp.moyashi.phoneos.core.ui.components.Button btnCornerStandard;
    private jp.moyashi.phoneos.core.ui.components.Button btnCornerRounded;
    // Text size
    private jp.moyashi.phoneos.core.ui.components.Button btnTextMinus;
    private jp.moyashi.phoneos.core.ui.components.Button btnTextPlus;
    // Toggles
    private jp.moyashi.phoneos.core.ui.components.Button btnReduceMotion;
    private jp.moyashi.phoneos.core.ui.components.Button btnLowPower;
    
    /** Settings items configuration */
    private static final String[] SETTING_ITEMS = {
        "Appearance",
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

    //

    //
    private final int accentPreviewGreen = 0xFF27AE60;
    private final int accentPreviewPurple = 0xFF8E44AD;
    private final int accentPreviewOrange = 0xFFF39C12;

    //
    
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

        //  E E           E
        if (kernel != null && kernel.getThemeEngine() != null) {
            var theme = kernel.getThemeEngine();
            backgroundColor = theme.colorBackground();
            textColor = theme.colorOnSurface();
            accentColor = theme.colorPrimary();
            itemColor = theme.colorSurface();
        } else {
            backgroundColor = 0xFF1A1A1A;
            textColor = 0xFFFFFFFF;
            accentColor = 0xFF4A90E2;
            itemColor = 0xFF2A2A2A;
        }
    }
    
    /**
     * Initializes the settings screen.
     * @deprecated Use {@link #setup(PGraphics)} instead for unified architecture.
     */
    @Override
    @Deprecated
    public void setup(processing.core.PApplet p) {
        PGraphics g = p.g;
        setup(g);
    }

    /**
     * Initializes the settings screen (PGraphics unified architecture).
     *
     * @param g The PGraphics instance
     */
    public void setup(PGraphics g) {
        isInitialized = true;
        System.out.println("SettingsScreen: Settings screen initialized");
    }
    
    /**
     * Draws the settings screen interface.
     * @deprecated Use {@link #draw(PGraphics)} instead for unified architecture.
     *
     * @param p The PApplet instance for drawing operations
     */
    @Override
    @Deprecated
    public void draw(PApplet p) {
        PGraphics g = p.g;
        draw(g);
    }

    /**
     * Draws the settings screen using PGraphics (unified architecture).
     *
     * @param g The PGraphics instance to draw to
     */
    public void draw(PGraphics g) {
        //  E E               
        if (kernel != null && kernel.getThemeEngine() != null) {
            var theme = kernel.getThemeEngine();
            backgroundColor = theme.colorBackground();
            textColor = theme.colorOnSurface();
            accentColor = theme.colorPrimary();
            itemColor = theme.colorSurface();
        }

        //      
        g.background(backgroundColor);

        //        
        drawHeader(g);

        //    E     
        drawSettingsItems(g);

        //    E   E     
        drawSystemInfo(g);

        // Appearance      
        if (showAppearancePanel) {
            drawAppearancePanelComponents(g);
        }
    }

    /**
     * Handles mouse press events.
     * @deprecated Use {@link #mousePressed(PGraphics, int, int)} instead for unified architecture.
     *
     * @param p The PApplet instance
     * @param mouseX The x-coordinate of the mouse press
     * @param mouseY The y-coordinate of the mouse press
     */
    @Override
    @Deprecated
    public void mousePressed(processing.core.PApplet p, int mouseX, int mouseY) {
        PGraphics g = p.g;
        mousePressed(g, mouseX, mouseY);
    }

    /**
     * Handles mouse press events (PGraphics unified architecture).
     *
     * @param g The PGraphics instance
     * @param mouseX The x-coordinate of the mouse press
     * @param mouseY The y-coordinate of the mouse press
     */
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        System.out.println("SettingsScreen: Touch at (" + mouseX + ", " + mouseY + ")");

        //    E      E    E
        if (mouseX >= 10 && mouseX <= 50 && mouseY >= 10 && mouseY <= 50) {
            if (showAppearancePanel) {
                showAppearancePanel = false;
            } else {
                goBack();
            }
            return;
        }

        // Appearance panel: delegate to components
        if (showAppearancePanel) {
            ensureAppearanceComponents();
            if (appearancePanel != null && appearancePanel.onMousePressed(mouseX, mouseY)) {
                return;
            }
            return;
        }

        //    E      E    E
        int clickedItem = getSettingsItemAtPosition(mouseY);
        if (clickedItem >= 0) {
            handleSettingsItemClick(clickedItem);
        }
    }
    
    /**
     * Cleans up resources when screen is deactivated.
     * @deprecated Use {@link #cleanup(PGraphics)} instead for unified architecture.
     */
    @Override
    @Deprecated
    public void cleanup(processing.core.PApplet p) {
        PGraphics g = p.g;
        cleanup(g);
    }

    /**
     * Cleans up resources when screen is deactivated (PGraphics unified architecture).
     *
     * @param g The PGraphics instance
     */
    public void cleanup(PGraphics g) {
        isInitialized = false;
        System.out.println("SettingsScreen: Settings screen cleaned up");
    }

    /**
     * Handles key press events.
     * @deprecated Use {@link #keyPressed(PGraphics, char, int)} instead for unified architecture.
     *
     * @param p The PApplet instance
     * @param key The pressed key
     * @param keyCode The key code
     */
    @Deprecated
    public void keyPressed(PApplet p, char key, int keyCode) {
        PGraphics g = p.g;
        keyPressed(g, key, keyCode);
    }

    /**
     * Handles key press events (PGraphics unified architecture).
     *
     * @param g The PGraphics instance
     * @param key The pressed key
     * @param keyCode The key code
     */
    public void keyPressed(PGraphics g, char key, int keyCode) {
        //
        //
    }

    /**
     * Handles mouse drag events.
     * @deprecated Use {@link #mouseDragged(PGraphics, int, int)} instead for unified architecture.
     *
     * @param p The PApplet instance
     * @param mouseX The x-coordinate of the mouse drag
     * @param mouseY The y-coordinate of the mouse drag
     */
    @Deprecated
    public void mouseDragged(PApplet p, int mouseX, int mouseY) {
        PGraphics g = p.g;
        mouseDragged(g, mouseX, mouseY);
    }

    /**
     * Handles mouse drag events (PGraphics unified architecture).
     *
     * @param g The PGraphics instance
     * @param mouseX The x-coordinate of the mouse drag
     * @param mouseY The y-coordinate of the mouse drag
     */
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        //
        //
    }

    /**
     * Handles mouse release events.
     * @deprecated Use {@link #mouseReleased(PGraphics, int, int)} instead for unified architecture.
     *
     * @param p The PApplet instance
     * @param mouseX The x-coordinate of the mouse release
     * @param mouseY The y-coordinate of the mouse release
     */
    @Deprecated
    public void mouseReleased(PApplet p, int mouseX, int mouseY) {
        PGraphics g = p.g;
        mouseReleased(g, mouseX, mouseY);
    }

    /**
     * Handles mouse release events (PGraphics unified architecture).
     *
     * @param g The PGraphics instance
     * @param mouseX The x-coordinate of the mouse release
     * @param mouseY The y-coordinate of the mouse release
     */
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        if (showAppearancePanel && appearancePanel != null) {
            appearancePanel.onMouseReleased(mouseX, mouseY);
        }
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
     * Handles mouse move events (PGraphics unified architecture).
     */
    public void mouseMoved(PGraphics g, int mouseX, int mouseY) {
        if (showAppearancePanel && appearancePanel != null) {
            appearancePanel.onMouseMoved(mouseX, mouseY);
        }
    }
    
    
    
    
    /**
     * Gets the icon character for a settings item.
     * 
     * @param index The settings item index
     * @return The icon character
     */
    private String getIconForSetting(int index) {
        // ASCII-only placeholders to avoid encoding issues
        switch (index) {
            case 0: return "*";   // Appearance
            case 1: return "~";   // Sound
            case 2: return "@";   // Apps
            case 3: return "#";   // Storage
            case 4: return "B";   // Battery
            case 5: return "i";   // About
            default: return ".";
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

        //  E       E E    E
        switch (itemIndex) {
            case 0:
                showAppearancePanel = true;
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

    /**
     * Draws the header section (PGraphics unified architecture).
     *
     * @param g The PGraphics instance for drawing
     */
    private void drawHeader(PGraphics g) {
        //
        g.fill(itemColor);
        g.noStroke();
        g.rect(0, 0, 400, 60);

        //
        g.stroke(textColor);
        g.strokeWeight(2);
        g.line(20, 30, 30, 20);
        g.line(20, 30, 30, 40);

        //
        g.fill(textColor);
        g.noStroke();
        g.textAlign(g.LEFT, g.CENTER);
        g.textSize(20);

        //
        if (kernel != null && kernel.getJapaneseFont() != null) {
            g.textFont(kernel.getJapaneseFont());
        }

        g.text("Settings", 50, 30);

        //          
        g.fill(accentColor);
        g.textAlign(g.RIGHT, g.CENTER);
        g.textSize(16);
        g.text("Settings", 380, 30);

        //       E  E   E
        g.strokeWeight(1);
        g.line(0, 59, 400, 59);
    }

    /**
     * Draws the list of settings items (PGraphics unified architecture).
     *
     * @param g The PGraphics instance for drawing
     */
    private void drawSettingsItems(PGraphics g) {
        int startY = 80;

        for (int i = 0; i < SETTING_ITEMS.length; i++) {
            int itemY = startY + i * ITEM_HEIGHT;

            //
            g.fill(itemColor);
            g.noStroke();
            g.rect(ITEM_PADDING, itemY, 400 - 2 * ITEM_PADDING, ITEM_HEIGHT - 5);

            //
            g.fill(accentColor);
            g.ellipse(ITEM_PADDING + 25, itemY + ITEM_HEIGHT/2, 30, 30);

            //
            if (kernel != null && kernel.getJapaneseFont() != null) {
                g.textFont(kernel.getJapaneseFont());
            }

            //
            g.fill(textColor);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(16);
            String iconText = getIconForSetting(i);
            g.text(iconText, ITEM_PADDING + 25, itemY + ITEM_HEIGHT/2 - 2);

            //
            g.fill(textColor);
            g.textAlign(g.LEFT, g.TOP);
            g.textSize(16);
            g.text(SETTING_ITEMS[i], ITEM_PADDING + 55, itemY + 15);

            //
            { int c=textColor; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, 150); }
            g.textSize(12);
            g.text(SETTING_DESCRIPTIONS[i], ITEM_PADDING + 55, itemY + 35);

            //
            g.fill(textColor, 100);
            g.textAlign(g.RIGHT, g.CENTER);
            g.textSize(20);
            g.text("  E ", 400 - ITEM_PADDING - 10, itemY + ITEM_HEIGHT/2);
        }
    }

    /**
     * Draws system information at the bottom (PGraphics unified architecture).
     *
     * @param g The PGraphics instance for drawing
     */
    private void drawSystemInfo(PGraphics g) {
        int infoY = 500;

        //
        g.fill(itemColor);
        g.noStroke();
        g.rect(ITEM_PADDING, infoY, 400 - 2 * ITEM_PADDING, 80);

        //
        if (kernel != null && kernel.getJapaneseFont() != null) {
            g.textFont(kernel.getJapaneseFont());
        }

        //
        g.fill(textColor);
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(14);
        g.text("MochiMobileOS", ITEM_PADDING + 15, infoY + 15);

        { int c=textColor; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF, 150); }
        g.textSize(12);
        g.text("Version 1.0.0 (Build 1)", ITEM_PADDING + 15, infoY + 35);
        g.text("Processing 4.4.4", ITEM_PADDING + 15, infoY + 50);

        //
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

    //`n    // old drawAppearancePanel removed

    //  :     E    E   Appearance  
    private void drawAppearancePanelComponents(PGraphics g) {
        int px = ITEM_PADDING;
        int py = 80;
        int pw = 400 - 2 * ITEM_PADDING;
        int ph = 480;

        ensureAppearanceComponents();
        updateAppearanceButtonStyles();

        if (appearancePanel != null) {
            appearancePanel.draw(g);
        }

        if (kernel != null && kernel.getJapaneseFont() != null) g.textFont(kernel.getJapaneseFont());
        int tcol = (kernel != null && kernel.getThemeEngine() != null) ? kernel.getThemeEngine().colorOnSurface() : textColor;
        g.fill((tcol>>16)&0xFF, (tcol>>8)&0xFF, tcol&0xFF);
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(16);
        g.text("Appearance", px + 16, py + 12);

        // ボタン配置に同期したラベル配置
        int baseY = py + 48;              // Theme Mode 行
        int fam1Y = baseY + 32;           // Family 行1
        int fam2Y = fam1Y + 32;           // Family 行2
        int fam3Y = fam2Y + 32;           // Family 行3
        int yc    = fam3Y + 42;           // Corners 行
        int yt    = yc + 42;              // Text Size 行
        int yr    = yt + 42;              // Reduce Motion 行
        int yl    = yr + 42;              // Low Power 行

        g.textSize(14);
        g.text("Theme Mode", px + 16, baseY);
        g.text("Color Family", px + 16, fam1Y);
        g.text("Corners", px + 16, yc);
        g.text("Text Size", px + 16, yt);
        g.text("Reduce Motion", px + 16, yr);
        g.text("Low Power Mode", px + 16, yl);
    }

    private void ensureAppearanceComponents() {
        if (appearancePanel != null) return;

        int px = ITEM_PADDING;
        int py = 80;
        int pw = 400 - 2 * ITEM_PADDING;
        int ph = 480;

        appearancePanel = new jp.moyashi.phoneos.core.ui.components.Panel(px, py, pw, ph);

        int baseY = py + 48;
        // Theme Mode row
        btnToneLight = new jp.moyashi.phoneos.core.ui.components.Button(px + 140, baseY - 6, 52, 24, "Light");
        btnToneDark  = new jp.moyashi.phoneos.core.ui.components.Button(px + 196, baseY - 6, 52, 24, "Dark");
        btnToneAuto  = new jp.moyashi.phoneos.core.ui.components.Button(px + 252, baseY - 6, 52, 24, "Auto");

        // Family rows (3 rows)
        int fam1Y = baseY + 32;
        int fam2Y = fam1Y + 32;
        int fam3Y = fam2Y + 32;
        btnFamWhite  = new jp.moyashi.phoneos.core.ui.components.Button(px + 140, fam1Y - 6, 52, 24, "White");
        btnFamOrange = new jp.moyashi.phoneos.core.ui.components.Button(px + 196, fam1Y - 6, 64, 24, "Orange");
        btnFamYellow = new jp.moyashi.phoneos.core.ui.components.Button(px + 264, fam1Y - 6, 60, 24, "Yellow");

        btnFamPink   = new jp.moyashi.phoneos.core.ui.components.Button(px + 140, fam2Y - 6, 52, 24, "Pink");
        btnFamGreen  = new jp.moyashi.phoneos.core.ui.components.Button(px + 196, fam2Y - 6, 60, 24, "Green");
        btnFamAqua   = new jp.moyashi.phoneos.core.ui.components.Button(px + 260, fam2Y - 6, 56, 24, "Aqua");

        btnFamBlack  = new jp.moyashi.phoneos.core.ui.components.Button(px + 140, fam3Y - 6, 56, 24, "Black");

        // Corners row: after family block + extra spacing
        int yc = fam3Y + 42;
        btnCornerCompact  = new jp.moyashi.phoneos.core.ui.components.Button(px + 140, yc - 6, 72, 24, "Compact");
        btnCornerStandard = new jp.moyashi.phoneos.core.ui.components.Button(px + 216, yc - 6, 78, 24, "Standard");
        btnCornerRounded  = new jp.moyashi.phoneos.core.ui.components.Button(px + 298, yc - 6, 76, 24, "Rounded");

        // Text size: after corners
        int yt = yc + 42;
        btnTextMinus = new jp.moyashi.phoneos.core.ui.components.Button(px + 140, yt - 6, 24, 24, "-");
        btnTextPlus  = new jp.moyashi.phoneos.core.ui.components.Button(px + 196, yt - 6, 24, 24, "+");

        // Reduce Motion: after text size
        int yr = yt + 42;
        btnReduceMotion = new jp.moyashi.phoneos.core.ui.components.Button(px + 140, yr - 6, 68, 24, "Off");
        // Low Power: after reduce motion
        int yl = yr + 42;
        btnLowPower = new jp.moyashi.phoneos.core.ui.components.Button(px + 140, yl - 6, 68, 24, "Off");

        var sm = kernel != null ? kernel.getSettingsManager() : null;
        if (sm != null) {
            btnToneLight.setOnClickListener(() -> { sm.setSetting("ui.theme.tone", "light"); sm.saveSettings(); });
            btnToneDark.setOnClickListener(() ->  { sm.setSetting("ui.theme.tone", "dark");  sm.saveSettings(); });
            btnToneAuto.setOnClickListener(() ->  { sm.setSetting("ui.theme.tone", "auto");  sm.saveSettings(); });

            btnFamWhite.setOnClickListener(() ->  { sm.setSetting("ui.theme.family", "white");  sm.saveSettings(); });
            btnFamOrange.setOnClickListener(() -> { sm.setSetting("ui.theme.family", "orange"); sm.saveSettings(); });
            btnFamYellow.setOnClickListener(() -> { sm.setSetting("ui.theme.family", "yellow"); sm.saveSettings(); });
            btnFamPink.setOnClickListener(() ->   { sm.setSetting("ui.theme.family", "pink");   sm.saveSettings(); });
            btnFamGreen.setOnClickListener(() ->  { sm.setSetting("ui.theme.family", "green");  sm.saveSettings(); });
            btnFamAqua.setOnClickListener(() ->   { sm.setSetting("ui.theme.family", "aqua");   sm.saveSettings(); });
            btnFamBlack.setOnClickListener(() ->  { sm.setSetting("ui.theme.family", "black");  sm.saveSettings(); });

            btnCornerCompact.setOnClickListener(() ->  { sm.setSetting("ui.shape.corner_scale", "compact");  sm.saveSettings(); });
            btnCornerStandard.setOnClickListener(() -> { sm.setSetting("ui.shape.corner_scale", "standard"); sm.saveSettings(); });
            btnCornerRounded.setOnClickListener(() ->  { sm.setSetting("ui.shape.corner_scale", "rounded");  sm.saveSettings(); });

            btnTextMinus.setOnClickListener(() -> {
                int cur = sm.getIntSetting("ui.typography.base_size", 14);
                sm.setSetting("ui.typography.base_size", Math.max(12, cur - 1));
                sm.saveSettings();
            });
            btnTextPlus.setOnClickListener(() -> {
                int cur = sm.getIntSetting("ui.typography.base_size", 14);
                sm.setSetting("ui.typography.base_size", Math.min(20, cur + 1));
                sm.saveSettings();
            });

            btnReduceMotion.setOnClickListener(() -> {
                boolean cur = sm.getBooleanSetting("ui.motion.reduce", false);
                sm.setSetting("ui.motion.reduce", !cur);
                sm.saveSettings();
            });
            btnLowPower.setOnClickListener(() -> {
                boolean cur = sm.getBooleanSetting("ui.performance.low_power", false);
                // TODO:        E     E    E
                sm.setSetting("ui.performance.low_power", !cur);
                sm.saveSettings();
            });
        }

        appearancePanel.addChild(btnToneLight);
        appearancePanel.addChild(btnToneDark);
        appearancePanel.addChild(btnToneAuto);
        appearancePanel.addChild(btnFamWhite);
        appearancePanel.addChild(btnFamOrange);
        appearancePanel.addChild(btnFamYellow);
        appearancePanel.addChild(btnFamPink);
        appearancePanel.addChild(btnFamGreen);
        appearancePanel.addChild(btnFamAqua);
        appearancePanel.addChild(btnFamBlack);
        appearancePanel.addChild(btnCornerCompact);
        appearancePanel.addChild(btnCornerStandard);
        appearancePanel.addChild(btnCornerRounded);
        appearancePanel.addChild(btnTextMinus);
        appearancePanel.addChild(btnTextPlus);
        appearancePanel.addChild(btnReduceMotion);
        appearancePanel.addChild(btnLowPower);
    }

    private void updateAppearanceButtonStyles() {
        var sm = kernel != null ? kernel.getSettingsManager() : null;
        var theme = kernel != null ? kernel.getThemeEngine() : null;
        if (sm == null || theme == null) return;

        String tone = sm.getStringSetting("ui.theme.tone", null);
        String legacy = sm.getStringSetting("ui.theme.mode", "light");
        if (tone == null || tone.isEmpty()) tone = ("dark".equals(legacy) ? "dark" : ("auto".equals(legacy) ? "auto" : "light"));
        String family = sm.getStringSetting("ui.theme.family", null);
        if (family == null || family.isEmpty()) family = legacy;

        int surface = theme.colorSurface();
        int border  = theme.colorBorder();
        int primary = theme.colorPrimary();
        int onSurf  = theme.colorOnSurface();
        int onPrim  = theme.colorOnPrimary();

        java.util.function.BiConsumer<jp.moyashi.phoneos.core.ui.components.Button, Boolean> styler = (btn, selected) -> {
            if (btn == null) return;
            if (selected) {
                btn.setBackgroundColor(primary);
                btn.setBorderColor(primary);
                btn.setTextColor(onSurf);
            } else {
                btn.setBackgroundColor(surface);
                btn.setBorderColor(border);
                btn.setTextColor(onSurf);
            }
        };

        styler.accept(btnToneLight, "light".equals(tone));
        styler.accept(btnToneDark,  "dark".equals(tone));
        styler.accept(btnToneAuto,  "auto".equals(tone));

        styler.accept(btnFamWhite,  "white".equals(family));
        styler.accept(btnFamOrange, "orange".equals(family));
        styler.accept(btnFamYellow, "yellow".equals(family));
        styler.accept(btnFamPink,   "pink".equals(family));
        styler.accept(btnFamGreen,  "green".equals(family));
        styler.accept(btnFamAqua,   "aqua".equals(family));
        styler.accept(btnFamBlack,  "black".equals(family));

        String corner = sm.getStringSetting("ui.shape.corner_scale", "standard");
        styler.accept(btnCornerCompact,  "compact".equals(corner));
        styler.accept(btnCornerStandard, "standard".equals(corner));
        styler.accept(btnCornerRounded,  "rounded".equals(corner));

        styler.accept(btnTextMinus, false);
        styler.accept(btnTextPlus,  false);

        boolean reduce = sm.getBooleanSetting("ui.motion.reduce", false);
        boolean lowp   = sm.getBooleanSetting("ui.performance.low_power", false);
        if (btnReduceMotion != null) btnReduceMotion.setText(reduce ? "On" : "Off");
        if (btnLowPower != null)     btnLowPower.setText(lowp ? "On" : "Off");
        styler.accept(btnReduceMotion, reduce);
        styler.accept(btnLowPower,     lowp);
    }

    private void handleAppearancePanelClick(int mouseX, int mouseY) {
        int px = ITEM_PADDING;
        int py = 80;
        int pw = 400 - 2 * ITEM_PADDING;
        int ph = 480;
        if (mouseX < px || mouseX > px + pw || mouseY < py || mouseY > py + ph) return;

        var sm = kernel != null ? kernel.getSettingsManager() : null;
        if (sm == null) return;

        // compute rows to avoid   
        int y = py + 48;
        int y2 = y + 32;
        int y3 = y2 + 32;
        // Tone row
        if (hit(mouseX, mouseY, px + 140, y - 6, 52, 24)) { sm.setSetting("ui.theme.tone", "light"); sm.saveSettings(); return; }
        if (hit(mouseX, mouseY, px + 196, y - 6, 52, 24)) { sm.setSetting("ui.theme.tone", "dark"); sm.saveSettings(); return; }
        if (hit(mouseX, mouseY, px + 252, y - 6, 52, 24)) { sm.setSetting("ui.theme.tone", "auto"); sm.saveSettings(); return; }
        // Family rows
        if (hit(mouseX, mouseY, px + 140, y2 - 6, 52, 24)) { sm.setSetting("ui.theme.family", "white"); sm.saveSettings(); return; }
        if (hit(mouseX, mouseY, px + 196, y2 - 6, 64, 24)) { sm.setSetting("ui.theme.family", "orange"); sm.saveSettings(); return; }
        if (hit(mouseX, mouseY, px + 264, y2 - 6, 60, 24)) { sm.setSetting("ui.theme.family", "yellow"); sm.saveSettings(); return; }
        if (hit(mouseX, mouseY, px + 140, y3 - 6, 52, 24)) { sm.setSetting("ui.theme.family", "pink"); sm.saveSettings(); return; }
        if (hit(mouseX, mouseY, px + 196, y3 - 6, 60, 24)) { sm.setSetting("ui.theme.family", "green"); sm.saveSettings(); return; }
        if (hit(mouseX, mouseY, px + 260, y3 - 6, 56, 24)) { sm.setSetting("ui.theme.family", "aqua"); sm.saveSettings(); return; }
        int y4 = py + 48 + 32 + 32;
        if (hit(mouseX, mouseY, px + 140, y4 - 6, 56, 24)) { sm.setSetting("ui.theme.family", "black"); sm.saveSettings(); return; }

        // accent controls removed

        //
        if (hit(mouseX, mouseY, px + 140, py + 126, 72, 24)) { sm.setSetting("ui.shape.corner_scale", "compact"); sm.saveSettings(); return; }
        if (hit(mouseX, mouseY, px + 216, py + 126, 78, 24)) { sm.setSetting("ui.shape.corner_scale", "standard"); sm.saveSettings(); return; }
        if (hit(mouseX, mouseY, px + 298, py + 126, 76, 24)) { sm.setSetting("ui.shape.corner_scale", "rounded"); sm.saveSettings(); return; }

        //
        if (hit(mouseX, mouseY, px + 140, py + 168, 24, 24)) {
            int cur = sm.getIntSetting("ui.typography.base_size", 14);
            sm.setSetting("ui.typography.base_size", Math.max(12, cur - 1));
            sm.saveSettings();
            return;
        }
        if (hit(mouseX, mouseY, px + 196, py + 168, 24, 24)) {
            int cur = sm.getIntSetting("ui.typography.base_size", 14);
            sm.setSetting("ui.typography.base_size", Math.min(20, cur + 1));
            sm.saveSettings();
            return;
        }

        //
        if (hit(mouseX, mouseY, px + 140, py + 210, 68, 24)) {
            boolean cur = sm.getBooleanSetting("ui.motion.reduce", false);
            sm.setSetting("ui.motion.reduce", !cur);
            sm.saveSettings();
            return;
        }

        // Low Power    E     E
        if (hit(mouseX, mouseY, px + 140, py + 252, 68, 24)) {
            boolean cur = sm.getBooleanSetting("ui.performance.low_power", false);
            sm.setSetting("ui.performance.low_power", !cur);
            sm.saveSettings();
            return;
        }

        //        E      E    E
        int presetStartX = px + 16;
        int presetStartY = py + 294;
        int sw = 24, sh = 24; int gap = 8; int perRow = 6;

        //        E      E    E         
    }

    private boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
    
    private String toHex(int argb) {
        int rgb = argb & 0x00FFFFFF;
        return String.format("#%06X", rgb);
    }
}









