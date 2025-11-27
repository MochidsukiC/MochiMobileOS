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
    private boolean showBatteryPanel = false;
    private boolean showAboutSystemPanel = false;
    private boolean showSoundVibrationPanel = false;
    private boolean showStoragePanel = false;

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

    // Battery: バッテリーパネル
    private jp.moyashi.phoneos.core.ui.components.Panel batteryPanel;
    private jp.moyashi.phoneos.core.ui.components.ProgressBar progressBatteryLevel;
    private jp.moyashi.phoneos.core.ui.components.Label labelBatteryLevel;
    private jp.moyashi.phoneos.core.ui.components.Label labelBatteryHealth;
    private jp.moyashi.phoneos.core.ui.components.Label labelChargingStatus;
    private jp.moyashi.phoneos.core.ui.components.Switch switchBatterySaver;
    private jp.moyashi.phoneos.core.ui.components.Switch switchAutoBatterySaver;
    private jp.moyashi.phoneos.core.ui.components.Slider sliderBatterySaverThreshold;
    private jp.moyashi.phoneos.core.ui.components.Button btnScreenTimeout15;
    private jp.moyashi.phoneos.core.ui.components.Button btnScreenTimeout30;
    private jp.moyashi.phoneos.core.ui.components.Button btnScreenTimeout60;
    private jp.moyashi.phoneos.core.ui.components.Button btnScreenTimeout120;
    private jp.moyashi.phoneos.core.ui.components.Button btnScreenTimeout300;
    private jp.moyashi.phoneos.core.ui.components.Button btnScreenTimeoutNever;

    // About System: 端末情報パネル
    private jp.moyashi.phoneos.core.ui.components.Panel aboutSystemPanel;
    private jp.moyashi.phoneos.core.ui.components.Label labelOSName;
    private jp.moyashi.phoneos.core.ui.components.Label labelOSVersion;
    private jp.moyashi.phoneos.core.ui.components.Label labelBuildNumber;
    private jp.moyashi.phoneos.core.ui.components.Label labelJavaVersion;
    private jp.moyashi.phoneos.core.ui.components.Label labelJVMVersion;
    private jp.moyashi.phoneos.core.ui.components.Label labelMemoryInfo;
    private jp.moyashi.phoneos.core.ui.components.Button btnOpenSourceLicenses;
    private jp.moyashi.phoneos.core.ui.components.Button btnLegalInfo;

    // Sound & Vibration: 音声・振動パネル
    private jp.moyashi.phoneos.core.ui.components.Panel soundVibrationPanel;
    private jp.moyashi.phoneos.core.ui.components.Slider sliderMasterVolume;
    private jp.moyashi.phoneos.core.ui.components.Switch switchNotificationSound;
    private jp.moyashi.phoneos.core.ui.components.Switch switchTouchSound;
    private jp.moyashi.phoneos.core.ui.components.Switch switchVibration;
    private jp.moyashi.phoneos.core.ui.components.Button btnRingtone;

    // Storage: ストレージパネル
    private jp.moyashi.phoneos.core.ui.components.Panel storagePanel;
    private jp.moyashi.phoneos.core.ui.components.ProgressBar progressStorageUsage;
    private jp.moyashi.phoneos.core.ui.components.Label labelStorageUsage;
    private jp.moyashi.phoneos.core.ui.components.Label labelAppData;
    private jp.moyashi.phoneos.core.ui.components.Label labelCacheSize;
    private jp.moyashi.phoneos.core.ui.components.Button btnClearCache;
    private jp.moyashi.phoneos.core.ui.components.Button btnClearAllData;

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

        // Battery パネル
        if (showBatteryPanel) {
            drawBatteryPanelComponents(g);
        }

        // About System パネル
        if (showAboutSystemPanel) {
            drawAboutSystemPanelComponents(g);
        }

        // Sound & Vibration パネル
        if (showSoundVibrationPanel) {
            drawSoundVibrationPanelComponents(g);
        }

        // Storage パネル
        if (showStoragePanel) {
            drawStoragePanelComponents(g);
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
            } else if (showBatteryPanel) {
                showBatteryPanel = false;
            } else if (showAboutSystemPanel) {
                showAboutSystemPanel = false;
            } else if (showSoundVibrationPanel) {
                showSoundVibrationPanel = false;
            } else if (showStoragePanel) {
                showStoragePanel = false;
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

        // Battery panel: delegate to components
        if (showBatteryPanel) {
            System.out.println("SettingsScreen: Battery panel click at (" + mouseX + ", " + mouseY + ")");
            ensureBatteryComponents();
            if (batteryPanel != null) {
                boolean handled = batteryPanel.onMousePressed(mouseX, mouseY);
                System.out.println("SettingsScreen: Battery panel handled = " + handled);
                if (handled) {
                    return;
                }
            }
            return;
        }

        // About System panel: delegate to components
        if (showAboutSystemPanel) {
            ensureAboutSystemComponents();
            if (aboutSystemPanel != null && aboutSystemPanel.onMousePressed(mouseX, mouseY)) {
                return;
            }
            return;
        }

        // Sound & Vibration panel: delegate to components
        if (showSoundVibrationPanel) {
            ensureSoundVibrationComponents();
            if (soundVibrationPanel != null && soundVibrationPanel.onMousePressed(mouseX, mouseY)) {
                return;
            }
            return;
        }

        // Storage panel: delegate to components
        if (showStoragePanel) {
            ensureStorageComponents();
            if (storagePanel != null && storagePanel.onMousePressed(mouseX, mouseY)) {
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
        System.out.println("SettingsScreen.mouseReleased at (" + mouseX + ", " + mouseY + ")");
        if (showAppearancePanel && appearancePanel != null) {
            System.out.println("  Forwarding to appearancePanel");
            appearancePanel.onMouseReleased(mouseX, mouseY);
        }
        if (showBatteryPanel && batteryPanel != null) {
            System.out.println("  Forwarding to batteryPanel");
            batteryPanel.onMouseReleased(mouseX, mouseY);
        }
        if (showAboutSystemPanel && aboutSystemPanel != null) {
            System.out.println("  Forwarding to aboutSystemPanel");
            aboutSystemPanel.onMouseReleased(mouseX, mouseY);
        }
        if (showSoundVibrationPanel && soundVibrationPanel != null) {
            System.out.println("  Forwarding to soundVibrationPanel");
            soundVibrationPanel.onMouseReleased(mouseX, mouseY);
        }
        if (showStoragePanel && storagePanel != null) {
            System.out.println("  Forwarding to storagePanel");
            storagePanel.onMouseReleased(mouseX, mouseY);
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
        if (showBatteryPanel && batteryPanel != null) {
            batteryPanel.onMouseMoved(mouseX, mouseY);
        }
        if (showAboutSystemPanel && aboutSystemPanel != null) {
            aboutSystemPanel.onMouseMoved(mouseX, mouseY);
        }
        if (showSoundVibrationPanel && soundVibrationPanel != null) {
            soundVibrationPanel.onMouseMoved(mouseX, mouseY);
        }
        if (showStoragePanel && storagePanel != null) {
            storagePanel.onMouseMoved(mouseX, mouseY);
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
                showSoundVibrationPanel = true;
                break;
            case 2:
                System.out.println("SettingsScreen: App settings would open here");
                break;
            case 3:
                showStoragePanel = true;
                break;
            case 4:
                showBatteryPanel = true;
                break;
            case 5:
                showAboutSystemPanel = true;
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

    // =========================================================================
    // Battery Section
    // =========================================================================

    private void ensureBatteryComponents() {
        if (batteryPanel != null) return;

        int px = ITEM_PADDING;
        int py = 80;
        int pw = 400 - 2 * ITEM_PADDING;
        int ph = 480;

        batteryPanel = new jp.moyashi.phoneos.core.ui.components.Panel(px, py, pw, ph);

        int y = py + 60;

        // バッテリーレベル（ProgressBar + Label）
        progressBatteryLevel = new jp.moyashi.phoneos.core.ui.components.ProgressBar(px + 16, y, pw - 32, 30);
        labelBatteryLevel = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y + 35, "100%");

        y += 80;

        // バッテリー寿命
        labelBatteryHealth = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y, "Health: 100%");

        y += 30;

        // 充電状態
        labelChargingStatus = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y, "Status: Not Charging");

        y += 50;

        // バッテリーセーバー
        switchBatterySaver = new jp.moyashi.phoneos.core.ui.components.Switch(px + 16, y, "Battery Saver");
        boolean batterySaverEnabled = kernel.getSettingsManager().getBooleanSetting("power.battery_saver.enabled", false);
        switchBatterySaver.setOn(batterySaverEnabled);
        switchBatterySaver.setOnChangeListener(enabled -> {
            var sm = kernel != null ? kernel.getSettingsManager() : null;
            if (sm != null) {
                sm.setSetting("power.battery_saver.enabled", enabled);
                sm.setSetting("ui.performance.low_power", enabled);
                sm.saveSettings();
            }
        });

        y += 40;

        // 自動バッテリーセーバー
        switchAutoBatterySaver = new jp.moyashi.phoneos.core.ui.components.Switch(px + 16, y, "Auto Battery Saver");
        boolean autoBatterySaverEnabled = kernel.getSettingsManager().getBooleanSetting("power.battery_saver.auto", true);
        switchAutoBatterySaver.setOn(autoBatterySaverEnabled);
        switchAutoBatterySaver.setOnChangeListener(enabled -> {
            var sm = kernel != null ? kernel.getSettingsManager() : null;
            if (sm != null) {
                sm.setSetting("power.battery_saver.auto", enabled);
                sm.saveSettings();
            }
        });

        y += 40;

        // バッテリーセーバー閾値
        sliderBatterySaverThreshold = new jp.moyashi.phoneos.core.ui.components.Slider(px + 16, y, pw - 32, 5, 50, 20);
        sliderBatterySaverThreshold.setLabel("Auto Threshold: 20%");
        sliderBatterySaverThreshold.setOnValueChangeListener(value -> {
            int threshold = value.intValue();
            sliderBatterySaverThreshold.setLabel("Auto Threshold: " + threshold + "%");
            var sm = kernel != null ? kernel.getSettingsManager() : null;
            if (sm != null) {
                sm.setSetting("power.battery_saver.threshold", threshold);
                sm.saveSettings();
            }
        });

        y += 60;

        // スクリーンタイムアウト
        btnScreenTimeout15 = new jp.moyashi.phoneos.core.ui.components.Button(px + 16, y, 56, 24, "15s");
        btnScreenTimeout30 = new jp.moyashi.phoneos.core.ui.components.Button(px + 76, y, 56, 24, "30s");
        btnScreenTimeout60 = new jp.moyashi.phoneos.core.ui.components.Button(px + 136, y, 56, 24, "1m");
        btnScreenTimeout120 = new jp.moyashi.phoneos.core.ui.components.Button(px + 196, y, 56, 24, "2m");
        btnScreenTimeout300 = new jp.moyashi.phoneos.core.ui.components.Button(px + 256, y, 56, 24, "5m");
        btnScreenTimeoutNever = new jp.moyashi.phoneos.core.ui.components.Button(px + 316, y, 56, 24, "Never");

        btnScreenTimeout15.setOnClickListener(() -> updateScreenTimeout(15));
        btnScreenTimeout30.setOnClickListener(() -> updateScreenTimeout(30));
        btnScreenTimeout60.setOnClickListener(() -> updateScreenTimeout(60));
        btnScreenTimeout120.setOnClickListener(() -> updateScreenTimeout(120));
        btnScreenTimeout300.setOnClickListener(() -> updateScreenTimeout(300));
        btnScreenTimeoutNever.setOnClickListener(() -> updateScreenTimeout(-1));

        // コンポーネントをパネルに追加
        batteryPanel.addChild(progressBatteryLevel);
        batteryPanel.addChild(labelBatteryLevel);
        batteryPanel.addChild(labelBatteryHealth);
        batteryPanel.addChild(labelChargingStatus);
        batteryPanel.addChild(switchBatterySaver);
        batteryPanel.addChild(switchAutoBatterySaver);
        batteryPanel.addChild(sliderBatterySaverThreshold);
        batteryPanel.addChild(btnScreenTimeout15);
        batteryPanel.addChild(btnScreenTimeout30);
        batteryPanel.addChild(btnScreenTimeout60);
        batteryPanel.addChild(btnScreenTimeout120);
        batteryPanel.addChild(btnScreenTimeout300);
        batteryPanel.addChild(btnScreenTimeoutNever);

        updateBatteryButtonStyles();
    }

    private void updateScreenTimeout(int seconds) {
        var sm = kernel != null ? kernel.getSettingsManager() : null;
        if (sm != null) {
            sm.setSetting("display.screen_timeout", seconds);
            sm.saveSettings();
            updateBatteryButtonStyles();
        }
    }

    private void updateBatteryButtonStyles() {
        var sm = kernel != null ? kernel.getSettingsManager() : null;
        if (sm == null) return;

        var theme = kernel != null ? kernel.getThemeEngine() : null;
        if (theme == null) return;

        int surface = theme.colorSurface();
        int primary = theme.colorPrimary();
        int border = theme.colorBorder();
        int onSurf = theme.colorOnSurface();

        int timeout = sm.getIntSetting("display.screen_timeout", 30);

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

        styler.accept(btnScreenTimeout15, timeout == 15);
        styler.accept(btnScreenTimeout30, timeout == 30);
        styler.accept(btnScreenTimeout60, timeout == 60);
        styler.accept(btnScreenTimeout120, timeout == 120);
        styler.accept(btnScreenTimeout300, timeout == 300);
        styler.accept(btnScreenTimeoutNever, timeout == -1);

        // Switch の状態を更新
        if (switchBatterySaver != null) {
            switchBatterySaver.setOn(sm.getBooleanSetting("power.battery_saver.enabled", false));
        }
        if (switchAutoBatterySaver != null) {
            switchAutoBatterySaver.setOn(sm.getBooleanSetting("power.battery_saver.auto", true));
        }
        if (sliderBatterySaverThreshold != null) {
            int threshold = sm.getIntSetting("power.battery_saver.threshold", 20);
            sliderBatterySaverThreshold.setValue(threshold);
            sliderBatterySaverThreshold.setLabel("Auto Threshold: " + threshold + "%");
        }
    }

    private void drawBatteryPanelComponents(PGraphics g) {
        int px = ITEM_PADDING;
        int py = 80;

        ensureBatteryComponents();

        // バッテリー情報を更新
        var batteryInfo = kernel != null ? kernel.getBatteryInfo() : null;
        if (batteryInfo != null) {
            int level = batteryInfo.getBatteryLevel();
            int health = batteryInfo.getBatteryHealth();
            boolean charging = batteryInfo.isCharging();

            if (progressBatteryLevel != null) {
                progressBatteryLevel.setValue(level);
            }
            if (labelBatteryLevel != null) {
                labelBatteryLevel.setText(level + "%");
            }
            if (labelBatteryHealth != null) {
                labelBatteryHealth.setText("Health: " + health + "%");
            }
            if (labelChargingStatus != null) {
                String status = charging ? "Charging" : "Not Charging";
                var monitor = kernel.getBatteryMonitor();
                if (monitor != null) {
                    status = monitor.getBatteryStatus();
                }
                labelChargingStatus.setText("Status: " + status);
            }
        }

        // パネル描画
        if (batteryPanel != null) {
            batteryPanel.draw(g);
        }

        // ヘッダー（Appearanceと同じ位置）
        if (kernel != null && kernel.getJapaneseFont() != null) g.textFont(kernel.getJapaneseFont());
        int tcol = (kernel != null && kernel.getThemeEngine() != null) ? kernel.getThemeEngine().colorOnSurface() : textColor;
        g.fill((tcol>>16)&0xFF, (tcol>>8)&0xFF, tcol&0xFF);
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(16);
        g.text("Battery", px + 16, py + 12);

        // セクションヘッダー
        g.textSize(14);
        g.fill(0xFF4A90E2);
        g.text("Battery Level", px + 16, py + 48);
        g.text("Power Management", px + 16, py + 210);
        g.text("Threshold", px + 16, py + 330);
        g.text("Screen Timeout", px + 16, py + 400);
    }

    /**
     * About Systemパネルのコンポーネントを初期化する
     */
    private void ensureAboutSystemComponents() {
        if (aboutSystemPanel != null) return;

        int px = ITEM_PADDING;
        int py = 80;
        int pw = 400 - 2 * ITEM_PADDING;
        int ph = 480;

        // パネルを作成
        aboutSystemPanel = new jp.moyashi.phoneos.core.ui.components.Panel(px, py, pw, ph);

        int y = py + 60;

        // システム情報セクション
        labelOSName = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y, "MochiMobileOS");
        y += 30;

        labelOSVersion = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y, "バージョン: 1.0.0");
        y += 25;

        labelBuildNumber = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y, "ビルド: 2025.11.27");
        y += 60;

        // Java環境セクション
        String javaVersion = System.getProperty("java.version", "不明");
        labelJavaVersion = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y, "Java: " + javaVersion);
        y += 25;

        String jvmName = System.getProperty("java.vm.name", "不明");
        String jvmVersion = System.getProperty("java.vm.version", "不明");
        labelJVMVersion = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y, "JVM: " + jvmName + " " + jvmVersion);
        y += 60;

        // メモリ情報セクション
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / 1024 / 1024; // MB
        long freeMemory = runtime.freeMemory() / 1024 / 1024; // MB
        long usedMemory = totalMemory - freeMemory;
        labelMemoryInfo = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y, "メモリ: " + usedMemory + " MB / " + totalMemory + " MB");
        y += 60;

        // 法的情報ボタン
        btnOpenSourceLicenses = new jp.moyashi.phoneos.core.ui.components.Button(px + 16, y, pw - 32, 40, "オープンソースライセンス");
        btnOpenSourceLicenses.setOnClickListener(() -> {
            System.out.println("SettingsScreen: Open source licenses clicked");
            // 将来的にライセンス画面を表示
        });
        y += 50;

        btnLegalInfo = new jp.moyashi.phoneos.core.ui.components.Button(px + 16, y, pw - 32, 40, "法的情報");
        btnLegalInfo.setOnClickListener(() -> {
            System.out.println("SettingsScreen: Legal info clicked");
            // 将来的に法的情報画面を表示
        });

        // パネルに全てのコンポーネントを追加
        aboutSystemPanel.addChild(labelOSName);
        aboutSystemPanel.addChild(labelOSVersion);
        aboutSystemPanel.addChild(labelBuildNumber);
        aboutSystemPanel.addChild(labelJavaVersion);
        aboutSystemPanel.addChild(labelJVMVersion);
        aboutSystemPanel.addChild(labelMemoryInfo);
        aboutSystemPanel.addChild(btnOpenSourceLicenses);
        aboutSystemPanel.addChild(btnLegalInfo);
    }

    /**
     * About Systemパネルのコンポーネントを描画する
     */
    private void drawAboutSystemPanelComponents(PGraphics g) {
        int px = ITEM_PADDING;
        int py = 80;

        ensureAboutSystemComponents();

        if (aboutSystemPanel != null) {
            aboutSystemPanel.draw(g);
        }

        // ヘッダー（Appearanceと同じ位置）
        if (kernel != null && kernel.getJapaneseFont() != null) g.textFont(kernel.getJapaneseFont());
        int tcol = (kernel != null && kernel.getThemeEngine() != null) ? kernel.getThemeEngine().colorOnSurface() : textColor;
        g.fill((tcol>>16)&0xFF, (tcol>>8)&0xFF, tcol&0xFF);
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(16);
        g.text("About System", px + 16, py + 12);

        // セクションヘッダー
        g.textSize(14);
        g.fill(0xFF4A90E2);
        g.text("System Information", px + 16, py + 48);
        g.text("Java Environment", px + 16, py + 133);
        g.text("Memory", px + 16, py + 218);
        g.text("Legal", px + 16, py + 298);

        // 全てのコンポーネントを描画
        if (labelOSName != null) labelOSName.draw(g);
        if (labelOSVersion != null) labelOSVersion.draw(g);
        if (labelBuildNumber != null) labelBuildNumber.draw(g);
        if (labelJavaVersion != null) labelJavaVersion.draw(g);
        if (labelJVMVersion != null) labelJVMVersion.draw(g);
        if (labelMemoryInfo != null) labelMemoryInfo.draw(g);
        if (btnOpenSourceLicenses != null) btnOpenSourceLicenses.draw(g);
        if (btnLegalInfo != null) btnLegalInfo.draw(g);
    }

    /**
     * Sound & Vibrationパネルのコンポーネントを初期化する
     */
    private void ensureSoundVibrationComponents() {
        if (soundVibrationPanel != null) return;

        int px = ITEM_PADDING;
        int py = 80;
        int pw = 400 - 2 * ITEM_PADDING;
        int ph = 480;

        // パネルを作成
        soundVibrationPanel = new jp.moyashi.phoneos.core.ui.components.Panel(px, py, pw, ph);

        int y = py + 60;

        // マスター音量スライダー
        int currentVolume = kernel.getSettingsManager().getIntSetting("audio.master_volume", 75);
        sliderMasterVolume = new jp.moyashi.phoneos.core.ui.components.Slider(px + 16, y, pw - 32, 0, 100, currentVolume);
        sliderMasterVolume.setLabel("Master Volume: " + currentVolume + "%");
        sliderMasterVolume.setOnValueChangeListener(value -> {
            int volume = value.intValue();
            sliderMasterVolume.setLabel("Master Volume: " + volume + "%");
            var sm = kernel != null ? kernel.getSettingsManager() : null;
            if (sm != null) {
                sm.setSetting("audio.master_volume", volume);
                sm.saveSettings();
            }
        });
        y += 60;

        // 通知音スイッチ
        switchNotificationSound = new jp.moyashi.phoneos.core.ui.components.Switch(px + 16, y, "Notification Sound");
        boolean notificationSound = kernel.getSettingsManager().getBooleanSetting("audio.notification_sound", true);
        switchNotificationSound.setOn(notificationSound);
        switchNotificationSound.setOnChangeListener(enabled -> {
            var sm = kernel != null ? kernel.getSettingsManager() : null;
            if (sm != null) {
                sm.setSetting("audio.notification_sound", enabled);
                sm.saveSettings();
            }
        });
        y += 40;

        // タッチサウンドスイッチ
        switchTouchSound = new jp.moyashi.phoneos.core.ui.components.Switch(px + 16, y, "Touch Sound");
        boolean touchSound = kernel.getSettingsManager().getBooleanSetting("audio.touch_sound", true);
        switchTouchSound.setOn(touchSound);
        switchTouchSound.setOnChangeListener(enabled -> {
            var sm = kernel != null ? kernel.getSettingsManager() : null;
            if (sm != null) {
                sm.setSetting("audio.touch_sound", enabled);
                sm.saveSettings();
            }
        });
        y += 40;

        // バイブレーションスイッチ
        switchVibration = new jp.moyashi.phoneos.core.ui.components.Switch(px + 16, y, "Vibration");
        boolean vibration = kernel.getSettingsManager().getBooleanSetting("audio.vibration", true);
        switchVibration.setOn(vibration);
        switchVibration.setOnChangeListener(enabled -> {
            var sm = kernel != null ? kernel.getSettingsManager() : null;
            if (sm != null) {
                sm.setSetting("audio.vibration", enabled);
                sm.saveSettings();
            }
        });
        y += 60;

        // 着信音選択ボタン
        btnRingtone = new jp.moyashi.phoneos.core.ui.components.Button(px + 16, y, pw - 32, 40, "Ringtone (Coming Soon)");
        btnRingtone.setOnClickListener(() -> {
            System.out.println("SettingsScreen: Ringtone selection clicked (coming soon)");
            // 将来的に着信音選択画面を表示
        });

        // パネルに全てのコンポーネントを追加
        soundVibrationPanel.addChild(sliderMasterVolume);
        soundVibrationPanel.addChild(switchNotificationSound);
        soundVibrationPanel.addChild(switchTouchSound);
        soundVibrationPanel.addChild(switchVibration);
        soundVibrationPanel.addChild(btnRingtone);
    }

    /**
     * Sound & Vibrationパネルのコンポーネントを描画する
     */
    private void drawSoundVibrationPanelComponents(PGraphics g) {
        int px = ITEM_PADDING;
        int py = 80;

        ensureSoundVibrationComponents();

        if (soundVibrationPanel != null) {
            soundVibrationPanel.draw(g);
        }

        // ヘッダー（Appearanceと同じ位置）
        if (kernel != null && kernel.getJapaneseFont() != null) g.textFont(kernel.getJapaneseFont());
        int tcol = (kernel != null && kernel.getThemeEngine() != null) ? kernel.getThemeEngine().colorOnSurface() : textColor;
        g.fill((tcol>>16)&0xFF, (tcol>>8)&0xFF, tcol&0xFF);
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(16);
        g.text("Sound & Vibration", px + 16, py + 12);

        // セクションヘッダー
        g.textSize(14);
        g.fill(0xFF4A90E2);
        g.text("Volume", px + 16, py + 48);
        g.text("Sounds", px + 16, py + 113);
        g.text("Ringtone", px + 16, py + 253);

        // 全てのコンポーネントを描画
        if (sliderMasterVolume != null) sliderMasterVolume.draw(g);
        if (switchNotificationSound != null) switchNotificationSound.draw(g);
        if (switchTouchSound != null) switchTouchSound.draw(g);
        if (switchVibration != null) switchVibration.draw(g);
        if (btnRingtone != null) btnRingtone.draw(g);
    }

    /**
     * Storageパネルのコンポーネントを初期化する
     */
    private void ensureStorageComponents() {
        if (storagePanel != null) return;

        int px = ITEM_PADDING;
        int py = 80;
        int pw = 400 - 2 * ITEM_PADDING;
        int ph = 480;

        // パネルを作成
        storagePanel = new jp.moyashi.phoneos.core.ui.components.Panel(px, py, pw, ph);

        int y = py + 60;

        // ストレージ使用状況（ダミーデータ）
        // TODO: VFSから実際の使用量を取得
        long totalStorage = 1024; // MB
        long usedStorage = 512; // MB
        int usagePercent = (int) ((usedStorage * 100) / totalStorage);

        progressStorageUsage = new jp.moyashi.phoneos.core.ui.components.ProgressBar(px + 16, y, pw - 32, 30);
        progressStorageUsage.setValue(usagePercent);
        y += 35;

        labelStorageUsage = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y, usedStorage + " MB / " + totalStorage + " MB (" + usagePercent + "%)");
        y += 50;

        // アプリデータ使用量（ダミーデータ）
        long appData = 128; // MB
        labelAppData = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y, "App Data: " + appData + " MB");
        y += 30;

        // キャッシュサイズ（ダミーデータ）
        long cacheSize = 64; // MB
        labelCacheSize = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y, "Cache: " + cacheSize + " MB");
        y += 60;

        // キャッシュクリアボタン
        btnClearCache = new jp.moyashi.phoneos.core.ui.components.Button(px + 16, y, pw - 32, 40, "Clear Cache");
        btnClearCache.setOnClickListener(() -> {
            System.out.println("SettingsScreen: Clear cache clicked");
            // TODO: キャッシュクリア処理を実装
            labelCacheSize.setText("Cache: 0 MB");
        });
        y += 50;

        // すべてのデータ削除ボタン
        btnClearAllData = new jp.moyashi.phoneos.core.ui.components.Button(px + 16, y, pw - 32, 40, "Clear All Data");
        btnClearAllData.setOnClickListener(() -> {
            System.out.println("SettingsScreen: Clear all data clicked");
            // TODO: すべてのデータ削除処理を実装（確認ダイアログが必要）
        });

        // パネルに全てのコンポーネントを追加
        storagePanel.addChild(progressStorageUsage);
        storagePanel.addChild(labelStorageUsage);
        storagePanel.addChild(labelAppData);
        storagePanel.addChild(labelCacheSize);
        storagePanel.addChild(btnClearCache);
        storagePanel.addChild(btnClearAllData);
    }

    /**
     * Storageパネルのコンポーネントを描画する
     */
    private void drawStoragePanelComponents(PGraphics g) {
        int px = ITEM_PADDING;
        int py = 80;

        ensureStorageComponents();

        if (storagePanel != null) {
            storagePanel.draw(g);
        }

        // ヘッダー（Appearanceと同じ位置）
        if (kernel != null && kernel.getJapaneseFont() != null) g.textFont(kernel.getJapaneseFont());
        int tcol = (kernel != null && kernel.getThemeEngine() != null) ? kernel.getThemeEngine().colorOnSurface() : textColor;
        g.fill((tcol>>16)&0xFF, (tcol>>8)&0xFF, tcol&0xFF);
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(16);
        g.text("Storage", px + 16, py + 12);

        // セクションヘッダー
        g.textSize(14);
        g.fill(0xFF4A90E2);
        g.text("Internal Storage", px + 16, py + 48);
        g.text("Data Usage", px + 16, py + 153);
        g.text("Storage Management", px + 16, py + 248);

        // 全てのコンポーネントを描画
        if (progressStorageUsage != null) progressStorageUsage.draw(g);
        if (labelStorageUsage != null) labelStorageUsage.draw(g);
        if (labelAppData != null) labelAppData.draw(g);
        if (labelCacheSize != null) labelCacheSize.draw(g);
        if (btnClearCache != null) btnClearCache.draw(g);
        if (btnClearAllData != null) btnClearAllData.draw(g);
    }

    private String toHex(int argb) {
        int rgb = argb & 0x00FFFFFF;
        return String.format("#%06X", rgb);
    }
}









