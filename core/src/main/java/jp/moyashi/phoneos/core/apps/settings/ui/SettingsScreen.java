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

    // Notifications: 通知パネル
    private boolean showNotificationsPanel = false;
    private jp.moyashi.phoneos.core.ui.components.Panel notificationsPanel;
    private jp.moyashi.phoneos.core.ui.components.Switch switchSilentMode;
    private jp.moyashi.phoneos.core.ui.components.Switch switchChatNotification;
    private jp.moyashi.phoneos.core.ui.components.Button btnNotificationSound;
    private jp.moyashi.phoneos.core.ui.components.Label labelCurrentNotificationSound;
    // 通知音選択リスト用
    private java.util.List<String> availableSounds = new java.util.ArrayList<>();
    private int selectedSoundIndex = -1;

    // Control Center: コントロールセンターパネル
    private boolean showControlCenterPanel = false;
    private jp.moyashi.phoneos.core.ui.components.Panel controlCenterSettingsPanel;
    private java.util.List<jp.moyashi.phoneos.core.controls.CardPlacement> ccPlacements = new java.util.ArrayList<>();
    private String selectedCardId = null;
    private float ccPreviewScale = 0.5f;

    // Dashboard: ダッシュボードウィジェット設定パネル
    private boolean showDashboardPanel = false;
    private jp.moyashi.phoneos.core.dashboard.DashboardSlot selectedDashboardSlot = null;
    private java.util.List<jp.moyashi.phoneos.core.dashboard.IDashboardWidget> availableWidgets = new java.util.ArrayList<>();

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
        "Control Center",
        "Dashboard",
        "About System"
    };

    private static final String[] SETTING_DESCRIPTIONS = {
        "Brightness, wallpaper, theme",
        "Volume, ringtones, alerts",
        "App permissions, notifications",
        "Storage usage and management",
        "Battery usage and optimization",
        "Customize control center cards",
        "Customize home screen widgets",
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

        // Notifications パネル
        if (showNotificationsPanel) {
            drawNotificationsPanelComponents(g);
        }

        // Control Center パネル
        if (showControlCenterPanel) {
            drawControlCenterPanelComponents(g);
        }

        // Dashboard パネル
        if (showDashboardPanel) {
            drawDashboardPanelComponents(g);
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
            } else if (showNotificationsPanel) {
                showNotificationsPanel = false;
            } else if (showControlCenterPanel) {
                showControlCenterPanel = false;
                selectedCardId = null;
            } else if (showDashboardPanel) {
                showDashboardPanel = false;
                selectedDashboardSlot = null;
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

        // Notifications panel: delegate to components
        if (showNotificationsPanel) {
            ensureNotificationsComponents();
            if (notificationsPanel != null && notificationsPanel.onMousePressed(mouseX, mouseY)) {
                return;
            }
            return;
        }

        // Control Center panel: handle card selection
        if (showControlCenterPanel) {
            handleControlCenterClick(mouseX, mouseY);
            return;
        }

        // Dashboard panel: handle slot/widget selection
        if (showDashboardPanel) {
            handleDashboardClick(mouseX, mouseY);
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
        if (showNotificationsPanel && notificationsPanel != null) {
            System.out.println("  Forwarding to notificationsPanel");
            notificationsPanel.onMouseReleased(mouseX, mouseY);
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
        if (showNotificationsPanel && notificationsPanel != null) {
            notificationsPanel.onMouseMoved(mouseX, mouseY);
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
                showNotificationsPanel = true;
                break;
            case 3:
                showStoragePanel = true;
                break;
            case 4:
                showBatteryPanel = true;
                break;
            case 5:
                showControlCenterPanel = true;
                loadControlCenterPlacements();
                break;
            case 6:
                showDashboardPanel = true;
                loadDashboardWidgets();
                break;
            case 7:
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

    // =========================================================================
    // Notifications Section
    // =========================================================================

    /**
     * Notificationsパネルのコンポーネントを初期化する
     */
    private void ensureNotificationsComponents() {
        if (notificationsPanel != null) return;

        int px = ITEM_PADDING;
        int py = 80;
        int pw = 400 - 2 * ITEM_PADDING;
        int ph = 480;

        // パネルを作成
        notificationsPanel = new jp.moyashi.phoneos.core.ui.components.Panel(px, py, pw, ph);

        int y = py + 60;

        // 消音モードスイッチ
        switchSilentMode = new jp.moyashi.phoneos.core.ui.components.Switch(px + 16, y, "サイレントモード");
        boolean silentMode = kernel.getSettingsManager().getBooleanSetting("audio.silent_mode", false);
        switchSilentMode.setOn(silentMode);
        switchSilentMode.setOnChangeListener(enabled -> {
            var sm = kernel != null ? kernel.getSettingsManager() : null;
            if (sm != null) {
                sm.setSetting("audio.silent_mode", enabled);
                sm.saveSettings();
                System.out.println("SettingsScreen: Silent mode " + (enabled ? "enabled" : "disabled"));

                // コントロールセンターのトグルも同期
                syncControlCenterToggle("silent_mode", enabled);
            }
        });
        y += 50;

        // チャット通知スイッチ（Forge環境でMinecraftチャットに通知を送信）
        switchChatNotification = new jp.moyashi.phoneos.core.ui.components.Switch(px + 16, y, "チャット通知");
        boolean chatNotification = kernel.getSettingsManager().getBooleanSetting("notification.chat_enabled", true);
        switchChatNotification.setOn(chatNotification);
        switchChatNotification.setOnChangeListener(enabled -> {
            var sm = kernel != null ? kernel.getSettingsManager() : null;
            if (sm != null) {
                sm.setSetting("notification.chat_enabled", enabled);
                sm.saveSettings();
                System.out.println("SettingsScreen: Chat notification " + (enabled ? "enabled" : "disabled"));
            }
        });
        y += 70;

        // 現在の通知音を表示
        String currentSoundPath = kernel.getSettingsManager().getStringSetting("notification.sound_path", null);
        String displayName = (currentSoundPath == null || currentSoundPath.isEmpty()) ? "デフォルト" : getFileName(currentSoundPath);
        labelCurrentNotificationSound = new jp.moyashi.phoneos.core.ui.components.Label(px + 16, y, "通知音: " + displayName);
        y += 30;

        // 通知音選択ボタン
        btnNotificationSound = new jp.moyashi.phoneos.core.ui.components.Button(px + 16, y, pw - 32, 40, "通知音を選択...");
        btnNotificationSound.setOnClickListener(() -> {
            System.out.println("SettingsScreen: Notification sound selection clicked");
            showNotificationSoundSelector();
        });
        y += 60;

        // VFS内のsoundsディレクトリから利用可能な音声ファイルを取得
        loadAvailableSounds();

        // パネルに全てのコンポーネントを追加
        notificationsPanel.addChild(switchSilentMode);
        notificationsPanel.addChild(switchChatNotification);
        notificationsPanel.addChild(labelCurrentNotificationSound);
        notificationsPanel.addChild(btnNotificationSound);
    }

    /**
     * VFSからシステムのsoundsディレクトリの音声ファイル一覧を取得する
     */
    private void loadAvailableSounds() {
        availableSounds.clear();
        availableSounds.add("デフォルト"); // 最初にデフォルトを追加

        if (kernel == null || kernel.getVFS() == null) return;

        var vfs = kernel.getVFS();

        // system/soundsディレクトリが存在するか確認
        if (!vfs.directoryExists("system/sounds")) {
            vfs.createDirectory("system/sounds");
            System.out.println("SettingsScreen: Created system/sounds directory");
        }

        // 音声ファイルを検索
        java.util.List<String> wavFiles = vfs.listFilesByExtension("system/sounds", ".wav");
        java.util.List<String> mp3Files = vfs.listFilesByExtension("system/sounds", ".mp3");

        for (String file : wavFiles) {
            availableSounds.add("system/sounds/" + file);
        }
        for (String file : mp3Files) {
            availableSounds.add("system/sounds/" + file);
        }

        System.out.println("SettingsScreen: Found " + (availableSounds.size() - 1) + " notification sounds");
    }

    /**
     * コントロールセンターのトグルと設定を同期する
     *
     * @param toggleId トグルID
     * @param enabled 有効/無効
     */
    private void syncControlCenterToggle(String toggleId, boolean enabled) {
        if (kernel == null || kernel.getControlCenterManager() == null) return;

        var item = kernel.getControlCenterManager().getItem(toggleId);
        if (item instanceof jp.moyashi.phoneos.core.controls.ToggleItem) {
            var toggle = (jp.moyashi.phoneos.core.controls.ToggleItem) item;
            // コールバックを発火させずに状態のみ更新
            if (toggle.isOn() != enabled) {
                toggle.setOnSilent(enabled);
            }
        }
    }

    /**
     * ファイルパスからファイル名を取得する
     */
    private String getFileName(String path) {
        if (path == null || path.isEmpty()) return "";
        int lastSlash = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
        return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
    }

    /**
     * 通知音選択ダイアログを表示する（簡易版：コンソール出力＋次の音を選択）
     */
    private void showNotificationSoundSelector() {
        if (availableSounds.isEmpty()) {
            loadAvailableSounds();
        }

        if (availableSounds.size() <= 1) {
            System.out.println("SettingsScreen: No custom notification sounds available");
            System.out.println("SettingsScreen: Place .wav or .mp3 files in system/sounds directory");
            return;
        }

        // 現在の設定を取得
        String currentPath = kernel.getSettingsManager().getStringSetting("notification.sound_path", null);

        // 現在の選択インデックスを特定
        selectedSoundIndex = 0;
        for (int i = 0; i < availableSounds.size(); i++) {
            String sound = availableSounds.get(i);
            if ("デフォルト".equals(sound)) {
                if (currentPath == null || currentPath.isEmpty()) {
                    selectedSoundIndex = i;
                    break;
                }
            } else if (sound.equals(currentPath)) {
                selectedSoundIndex = i;
                break;
            }
        }

        // 次の音声に切り替え
        selectedSoundIndex = (selectedSoundIndex + 1) % availableSounds.size();
        String selected = availableSounds.get(selectedSoundIndex);

        // 設定を更新
        var sm = kernel.getSettingsManager();
        if ("デフォルト".equals(selected)) {
            sm.setSetting("notification.sound_path", "");
        } else {
            sm.setSetting("notification.sound_path", selected);
        }
        sm.saveSettings();

        // ラベルを更新
        String displayName = "デフォルト".equals(selected) ? "デフォルト" : getFileName(selected);
        if (labelCurrentNotificationSound != null) {
            labelCurrentNotificationSound.setText("通知音: " + displayName);
        }

        System.out.println("SettingsScreen: Notification sound set to: " + displayName);
    }

    /**
     * Notificationsパネルのコンポーネントを描画する
     */
    private void drawNotificationsPanelComponents(PGraphics g) {
        int px = ITEM_PADDING;
        int py = 80;

        ensureNotificationsComponents();

        // 設定状態を更新
        var sm = kernel != null ? kernel.getSettingsManager() : null;
        if (sm != null) {
            if (switchSilentMode != null) {
                switchSilentMode.setOn(sm.getBooleanSetting("audio.silent_mode", false));
            }
            if (switchChatNotification != null) {
                switchChatNotification.setOn(sm.getBooleanSetting("notification.chat_enabled", true));
            }
            if (labelCurrentNotificationSound != null) {
                String currentPath = sm.getStringSetting("notification.sound_path", null);
                String displayName = (currentPath == null || currentPath.isEmpty()) ? "デフォルト" : getFileName(currentPath);
                labelCurrentNotificationSound.setText("通知音: " + displayName);
            }
        }

        if (notificationsPanel != null) {
            notificationsPanel.draw(g);
        }

        // ヘッダー
        if (kernel != null && kernel.getJapaneseFont() != null) g.textFont(kernel.getJapaneseFont());
        int tcol = (kernel != null && kernel.getThemeEngine() != null) ? kernel.getThemeEngine().colorOnSurface() : textColor;
        g.fill((tcol>>16)&0xFF, (tcol>>8)&0xFF, tcol&0xFF);
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(16);
        g.text("Notifications", px + 16, py + 12);

        // セクションヘッダー
        g.textSize(14);
        g.fill(0xFF4A90E2);
        g.text("通知モード", px + 16, py + 48);
        g.text("通知音", px + 16, py + 168);

        // 説明テキスト
        g.textSize(12);
        // colorOnSurface()を薄くして使用
        int baseColor = (kernel != null && kernel.getThemeEngine() != null)
            ? kernel.getThemeEngine().colorOnSurface()
            : 0xFFFFFFFF;
        g.fill((baseColor >> 16) & 0xFF, (baseColor >> 8) & 0xFF, baseColor & 0xFF, 150);
        g.text("サイレントモード: 通知音とチャット通知をオフにします", px + 16, py + 108);
        g.text("system/sounds にwav/mp3ファイルを配置して選択できます", px + 16, py + 268);
    }

    // ==================== Control Center Panel ====================

    /**
     * CardRegistryから配置情報を読み込む。
     */
    private void loadControlCenterPlacements() {
        ccPlacements.clear();
        if (kernel != null && kernel.getControlCenterCardRegistry() != null) {
            var registry = kernel.getControlCenterCardRegistry();
            ccPlacements.addAll(registry.getAllPlacements());
            System.out.println("SettingsScreen: Loaded " + ccPlacements.size() + " card placements");
        }
    }

    /**
     * Control Centerパネルの描画。
     */
    private void drawControlCenterPanelComponents(PGraphics g) {
        // 背景
        g.fill(backgroundColor);
        g.noStroke();
        g.rect(0, 0, g.width, g.height);

        // ヘッダー
        drawPanelHeader(g, "Control Center");

        int py = 70;  // ヘッダー下からスタート
        int px = 16;
        int pw = g.width - 32;

        // グリッドプレビュー領域
        g.fill(textColor);
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(14);
        g.text("プレビュー（カードをタップして選択）", px, py);
        py += 24;

        // プレビュー背景
        int previewHeight = 200;
        g.fill(itemColor);
        g.stroke(accentColor);
        g.strokeWeight(1);
        g.rect(px, py, pw, previewHeight, 8);

        // プレビュー描画
        drawControlCenterPreview(g, px + 4, py + 4, pw - 8, previewHeight - 8);
        py += previewHeight + 16;

        // カードリスト（セクション別）
        g.fill(textColor);
        g.noStroke();
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(14);
        g.text("カード一覧", px, py);
        py += 24;

        // セクションごとにカードをリスト表示
        for (jp.moyashi.phoneos.core.controls.ControlCenterSection section :
                jp.moyashi.phoneos.core.controls.ControlCenterSection.values()) {

            // セクションヘッダー
            g.fill(accentColor);
            g.textSize(12);
            g.text(section.getDisplayName(), px, py);
            py += 18;

            // セクション内のカード
            for (var placement : ccPlacements) {
                if (placement.getSection() != section) continue;

                String cardId = placement.getCardId();
                String cardName = getCardDisplayName(cardId);
                boolean isVisible = placement.isVisible();
                boolean isSelected = cardId.equals(selectedCardId);

                // カード行の背景
                if (isSelected) {
                    g.fill(accentColor, 50);
                } else {
                    g.fill(itemColor);
                }
                g.noStroke();
                g.rect(px, py, pw, 36, 4);

                // チェックボックス（表示/非表示）
                int cbX = px + 8;
                int cbY = py + 8;
                int cbSize = 20;
                g.stroke(textColor);
                g.strokeWeight(1);
                g.noFill();
                g.rect(cbX, cbY, cbSize, cbSize, 4);
                if (isVisible) {
                    g.fill(accentColor);
                    g.noStroke();
                    g.rect(cbX + 4, cbY + 4, cbSize - 8, cbSize - 8, 2);
                }

                // カード名
                g.fill(textColor);
                g.textSize(13);
                g.textAlign(g.LEFT, g.CENTER);
                g.text(cardName, cbX + cbSize + 12, py + 18);

                // 選択時のオプション
                if (isSelected) {
                    // 上移動ボタン
                    int btnY = py + 4;
                    int btnSize = 28;
                    int upX = pw - 70;
                    int downX = pw - 35;

                    g.fill(itemColor);
                    g.stroke(textColor);
                    g.rect(upX, btnY, btnSize, btnSize, 4);
                    g.rect(downX, btnY, btnSize, btnSize, 4);

                    g.fill(textColor);
                    g.textAlign(g.CENTER, g.CENTER);
                    g.textSize(16);
                    g.text("↑", upX + btnSize / 2, btnY + btnSize / 2);
                    g.text("↓", downX + btnSize / 2, btnY + btnSize / 2);
                }

                py += 40;
            }

            py += 8; // セクション間スペース
        }

        // 保存ボタン
        py += 8;
        int btnW = 120;
        int btnH = 36;
        int btnX = (g.width - btnW) / 2;
        g.fill(accentColor);
        g.noStroke();
        g.rect(btnX, py, btnW, btnH, 8);
        g.fill(0xFFFFFFFF);
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(14);
        g.text("保存", btnX + btnW / 2, py + btnH / 2);
    }

    /**
     * Control Centerのグリッドプレビューを描画。
     */
    private void drawControlCenterPreview(PGraphics g, float x, float y, float w, float h) {
        // ミニチュア版のコントロールセンターを描画
        float scale = 0.4f;
        int cols = 4;
        float cellSize = w / cols;
        float cellHeight = cellSize;
        int row = 0, col = 0;

        // グリッド占有状況
        boolean[][] occupied = new boolean[10][cols];

        var registry = kernel != null ? kernel.getControlCenterCardRegistry() : null;
        if (registry == null) return;

        // 配置順序でソート
        var sortedPlacements = new java.util.ArrayList<>(ccPlacements);
        sortedPlacements.sort((a, b) -> {
            int secCompare = Integer.compare(a.getSection().getDefaultOrder(), b.getSection().getDefaultOrder());
            if (secCompare != 0) return secCompare;
            return Integer.compare(a.getOrder(), b.getOrder());
        });

        for (var placement : sortedPlacements) {
            if (!placement.isVisible()) continue;

            var card = registry.getCard(placement.getCardId());
            if (card == null) continue;

            int colSpan = Math.min(card.getColumnSpan(), cols);
            int rowSpan = card.getRowSpan();

            // 配置位置を探す
            int placeCol = -1, placeRow = -1;
            outer:
            for (int r = 0; r < occupied.length; r++) {
                for (int c = 0; c <= cols - colSpan; c++) {
                    boolean canPlace = true;
                    for (int dr = 0; dr < rowSpan && canPlace; dr++) {
                        for (int dc = 0; dc < colSpan && canPlace; dc++) {
                            if (r + dr >= occupied.length || occupied[r + dr][c + dc]) {
                                canPlace = false;
                            }
                        }
                    }
                    if (canPlace) {
                        placeCol = c;
                        placeRow = r;
                        break outer;
                    }
                }
            }

            if (placeCol < 0) continue;

            // グリッドを占有
            for (int dr = 0; dr < rowSpan; dr++) {
                for (int dc = 0; dc < colSpan; dc++) {
                    if (placeRow + dr < occupied.length) {
                        occupied[placeRow + dr][placeCol + dc] = true;
                    }
                }
            }

            // カード描画
            float cardX = x + placeCol * cellSize + 2;
            float cardY = y + placeRow * cellHeight + 2;
            float cardW = cellSize * colSpan - 4;
            float cardH = cellHeight * rowSpan - 4;

            boolean isSelected = placement.getCardId().equals(selectedCardId);

            // 背景
            if (isSelected) {
                g.fill(accentColor);
            } else {
                g.fill(0xFF3A3A3C);
            }
            g.noStroke();
            g.rect(cardX, cardY, cardW, cardH, 4);

            // カード名（短縮表示）
            g.fill(0xFFFFFFFF);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(Math.max(8, cardW / 6));
            String shortName = getCardShortName(placement.getCardId());
            g.text(shortName, cardX + cardW / 2, cardY + cardH / 2);
        }
    }

    /**
     * Control Centerパネルでのクリック処理。
     */
    private void handleControlCenterClick(int mouseX, int mouseY) {
        int py = 70 + 24;  // ヘッダー + タイトル
        int px = 16;
        int pw = 400 - 32;
        int previewHeight = 200;

        // プレビュー領域のクリック判定
        if (mouseY >= py && mouseY < py + previewHeight) {
            handlePreviewClick(mouseX - px, mouseY - py, pw, previewHeight);
            return;
        }

        py += previewHeight + 16 + 24;  // カードリストのタイトル分

        // セクションごとのカードリストのクリック判定
        for (jp.moyashi.phoneos.core.controls.ControlCenterSection section :
                jp.moyashi.phoneos.core.controls.ControlCenterSection.values()) {

            py += 18;  // セクションヘッダー

            for (var placement : ccPlacements) {
                if (placement.getSection() != section) continue;

                String cardId = placement.getCardId();
                boolean isSelected = cardId.equals(selectedCardId);

                if (mouseY >= py && mouseY < py + 36) {
                    // チェックボックス領域
                    int cbX = px + 8;
                    int cbSize = 20;
                    if (mouseX >= cbX && mouseX < cbX + cbSize + 12 &&
                            mouseY >= py + 8 && mouseY < py + 28) {
                        // 表示/非表示トグル
                        toggleCardVisibility(cardId);
                        return;
                    }

                    // 上下ボタン（選択中のみ）
                    if (isSelected) {
                        int upX = pw - 70 + px;
                        int downX = pw - 35 + px;
                        int btnY = py + 4;
                        int btnSize = 28;

                        if (mouseX >= upX && mouseX < upX + btnSize &&
                                mouseY >= btnY && mouseY < btnY + btnSize) {
                            moveCardUp(cardId);
                            return;
                        }
                        if (mouseX >= downX && mouseX < downX + btnSize &&
                                mouseY >= btnY && mouseY < btnY + btnSize) {
                            moveCardDown(cardId);
                            return;
                        }
                    }

                    // カード選択
                    selectedCardId = cardId;
                    System.out.println("SettingsScreen: Selected card: " + cardId);
                    return;
                }

                py += 40;
            }

            py += 8;
        }

        // 保存ボタン
        py += 8;
        int btnW = 120;
        int btnH = 36;
        int btnX = (400 - btnW) / 2;
        if (mouseX >= btnX && mouseX < btnX + btnW && mouseY >= py && mouseY < py + btnH) {
            saveControlCenterPlacements();
        }
    }

    /**
     * プレビュー領域でのクリック処理。
     */
    private void handlePreviewClick(int localX, int localY, int previewW, int previewH) {
        float cellSize = (float) previewW / 4;
        float cellHeight = cellSize;
        int cols = 4;

        boolean[][] occupied = new boolean[10][cols];

        var registry = kernel != null ? kernel.getControlCenterCardRegistry() : null;
        if (registry == null) return;

        var sortedPlacements = new java.util.ArrayList<>(ccPlacements);
        sortedPlacements.sort((a, b) -> {
            int secCompare = Integer.compare(a.getSection().getDefaultOrder(), b.getSection().getDefaultOrder());
            if (secCompare != 0) return secCompare;
            return Integer.compare(a.getOrder(), b.getOrder());
        });

        for (var placement : sortedPlacements) {
            if (!placement.isVisible()) continue;

            var card = registry.getCard(placement.getCardId());
            if (card == null) continue;

            int colSpan = Math.min(card.getColumnSpan(), cols);
            int rowSpan = card.getRowSpan();

            int placeCol = -1, placeRow = -1;
            outer:
            for (int r = 0; r < occupied.length; r++) {
                for (int c = 0; c <= cols - colSpan; c++) {
                    boolean canPlace = true;
                    for (int dr = 0; dr < rowSpan && canPlace; dr++) {
                        for (int dc = 0; dc < colSpan && canPlace; dc++) {
                            if (r + dr >= occupied.length || occupied[r + dr][c + dc]) {
                                canPlace = false;
                            }
                        }
                    }
                    if (canPlace) {
                        placeCol = c;
                        placeRow = r;
                        break outer;
                    }
                }
            }

            if (placeCol < 0) continue;

            for (int dr = 0; dr < rowSpan; dr++) {
                for (int dc = 0; dc < colSpan; dc++) {
                    if (placeRow + dr < occupied.length) {
                        occupied[placeRow + dr][placeCol + dc] = true;
                    }
                }
            }

            float cardX = placeCol * cellSize + 2;
            float cardY = placeRow * cellHeight + 2;
            float cardW = cellSize * colSpan - 4;
            float cardH = cellHeight * rowSpan - 4;

            if (localX >= cardX && localX < cardX + cardW &&
                    localY >= cardY && localY < cardY + cardH) {
                selectedCardId = placement.getCardId();
                System.out.println("SettingsScreen: Selected card from preview: " + selectedCardId);
                return;
            }
        }
    }

    /**
     * カードの表示/非表示をトグル。
     */
    private void toggleCardVisibility(String cardId) {
        for (var placement : ccPlacements) {
            if (placement.getCardId().equals(cardId)) {
                placement.setVisible(!placement.isVisible());
                System.out.println("SettingsScreen: Toggled visibility for " + cardId + " to " + placement.isVisible());
                break;
            }
        }
    }

    /**
     * カードを上に移動。
     */
    private void moveCardUp(String cardId) {
        if (kernel == null || kernel.getControlCenterCardRegistry() == null) return;
        kernel.getControlCenterCardRegistry().moveCardUp(cardId);
        loadControlCenterPlacements();
        System.out.println("SettingsScreen: Moved card up: " + cardId);
    }

    /**
     * カードを下に移動。
     */
    private void moveCardDown(String cardId) {
        if (kernel == null || kernel.getControlCenterCardRegistry() == null) return;
        kernel.getControlCenterCardRegistry().moveCardDown(cardId);
        loadControlCenterPlacements();
        System.out.println("SettingsScreen: Moved card down: " + cardId);
    }

    /**
     * 配置情報を保存。
     */
    private void saveControlCenterPlacements() {
        if (kernel == null || kernel.getControlCenterCardRegistry() == null) return;

        var registry = kernel.getControlCenterCardRegistry();
        for (var placement : ccPlacements) {
            registry.setCardVisible(placement.getCardId(), placement.isVisible());
        }
        registry.savePlacements();
        System.out.println("SettingsScreen: Saved control center placements");
    }

    /**
     * カードIDから表示名を取得。
     */
    private String getCardDisplayName(String cardId) {
        if (kernel == null || kernel.getControlCenterCardRegistry() == null) return cardId;
        var card = kernel.getControlCenterCardRegistry().getCard(cardId);
        return card != null ? card.getDisplayName() : cardId;
    }

    /**
     * カードIDから短縮名を取得（プレビュー用）。
     */
    private String getCardShortName(String cardId) {
        String name = getCardDisplayName(cardId);
        if (name.length() > 6) {
            return name.substring(0, 5) + "…";
        }
        return name;
    }

    /**
     * パネルヘッダーを描画（共通）。
     */
    private void drawPanelHeader(PGraphics g, String title) {
        g.fill(itemColor);
        g.noStroke();
        g.rect(0, 0, g.width, 60);

        // 戻るボタン
        g.stroke(textColor);
        g.strokeWeight(2);
        g.line(20, 30, 30, 20);
        g.line(20, 30, 30, 40);

        // タイトル
        g.fill(textColor);
        g.noStroke();
        g.textAlign(g.LEFT, g.CENTER);
        g.textSize(18);
        if (kernel != null && kernel.getJapaneseFont() != null) {
            g.textFont(kernel.getJapaneseFont());
        }
        g.text(title, 50, 30);

        // 下線
        g.stroke(textColor);
        g.strokeWeight(1);
        g.line(0, 59, g.width, 59);
    }

    private String toHex(int argb) {
        int rgb = argb & 0x00FFFFFF;
        return String.format("#%06X", rgb);
    }

    // ==================== Dashboard Panel Methods ====================

    /**
     * ダッシュボードウィジェット一覧を読み込む。
     */
    private void loadDashboardWidgets() {
        availableWidgets.clear();
        selectedDashboardSlot = null;

        if (kernel == null || kernel.getDashboardWidgetRegistry() == null) return;

        var registry = kernel.getDashboardWidgetRegistry();
        availableWidgets.addAll(registry.getAllWidgets());
        System.out.println("SettingsScreen: Loaded " + availableWidgets.size() + " dashboard widgets");
    }

    /**
     * Dashboardパネルの描画。
     */
    private void drawDashboardPanelComponents(PGraphics g) {
        // 背景
        g.fill(backgroundColor);
        g.noStroke();
        g.rect(0, 0, g.width, g.height);

        // ヘッダー
        drawPanelHeader(g, "Dashboard");

        int py = 70;  // ヘッダー下からスタート
        int px = 16;
        int pw = g.width - 32;

        // 説明
        g.fill(textColor);
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(12);
        g.text("ホーム画面のダッシュボードウィジェットを設定します。", px, py);
        g.text("スロットをタップしてウィジェットを選択してください。", px, py + 16);
        py += 40;

        // プレビュー領域
        g.fill(textColor);
        g.textSize(14);
        g.text("プレビュー", px, py);
        py += 24;

        // プレビュー背景
        int previewHeight = 220;
        g.fill(itemColor);
        g.stroke(accentColor);
        g.strokeWeight(1);
        g.rect(px, py, pw, previewHeight, 8);

        // プレビュー描画
        drawDashboardPreview(g, px + 4, py + 4, pw - 8, previewHeight - 8);
        py += previewHeight + 16;

        // 選択中のスロットがある場合、ウィジェット選択リストを表示
        if (selectedDashboardSlot != null) {
            g.fill(textColor);
            g.textSize(14);
            g.text(selectedDashboardSlot.name() + " スロットのウィジェットを選択", px, py);
            py += 24;

            // ウィジェットリスト
            var registry = kernel != null ? kernel.getDashboardWidgetRegistry() : null;
            if (registry != null) {
                // 選択中スロットに適合するウィジェットのみ表示
                var compatibleWidgets = new java.util.ArrayList<jp.moyashi.phoneos.core.dashboard.IDashboardWidget>();
                for (var widget : availableWidgets) {
                    if (widget.getSize() == selectedDashboardSlot.getRequiredSize()) {
                        compatibleWidgets.add(widget);
                    }
                }

                // 現在割り当てられているウィジェット
                var currentWidget = registry.getWidgetForSlot(selectedDashboardSlot);
                String currentId = currentWidget != null ? currentWidget.getId() : null;

                for (var widget : compatibleWidgets) {
                    boolean isSelected = widget.getId().equals(currentId);

                    // ウィジェット行の背景
                    if (isSelected) {
                        g.fill(accentColor, 80);
                    } else {
                        g.fill(itemColor);
                    }
                    g.noStroke();
                    g.rect(px, py, pw, 44, 4);

                    // ウィジェット名と説明
                    g.fill(textColor);
                    g.textSize(14);
                    g.textAlign(g.LEFT, g.TOP);
                    g.text(widget.getDisplayName(), px + 12, py + 6);
                    g.textSize(11);
                    g.fill(textColor, 180);
                    g.text(widget.getDescription(), px + 12, py + 24);

                    // 選択マーク
                    if (isSelected) {
                        g.fill(accentColor);
                        g.textSize(16);
                        g.textAlign(g.RIGHT, g.CENTER);
                        g.text("✓", px + pw - 12, py + 22);
                    }

                    py += 48;
                }
            }
        } else {
            g.fill(textColor, 150);
            g.textSize(12);
            g.textAlign(g.CENTER, g.TOP);
            g.text("上のプレビューでスロットをタップしてください", g.width / 2, py);
        }
    }

    /**
     * ダッシュボードのプレビューを描画。
     */
    private void drawDashboardPreview(PGraphics g, float x, float y, float w, float h) {
        var registry = kernel != null ? kernel.getDashboardWidgetRegistry() : null;
        if (registry == null) return;

        float scale = w / 360f;  // 360px を基準にスケール

        // 各スロットを描画
        for (var slot : jp.moyashi.phoneos.core.dashboard.DashboardSlot.values()) {
            float slotX = x + (slot.getX() - 20) * scale;  // 左マージン分を調整
            float slotY = y + (slot.getY() - 80) * scale;  // 上マージン分を調整
            float slotW = slot.getWidth() * scale;
            float slotH = slot.getHeight() * scale;

            // スロット背景
            boolean isSelected = slot == selectedDashboardSlot;
            if (isSelected) {
                g.fill(accentColor, 100);
                g.stroke(accentColor);
                g.strokeWeight(2);
            } else {
                g.fill(0xFFE0E0E0);
                g.stroke(0xFF999999);
                g.strokeWeight(1);
            }
            g.rect(slotX, slotY, slotW, slotH, 4);

            // ウィジェット名を表示
            var widget = registry.getWidgetForSlot(slot);
            String label = widget != null ? widget.getDisplayName() : "(空き)";
            if (!slot.isConfigurable()) {
                label = widget != null ? widget.getDisplayName() : slot.name();
            }

            g.fill(isSelected ? accentColor : textColor);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(Math.max(8, 10 * scale));
            g.noStroke();

            // 長いラベルは切り詰め
            if (label.length() > 8) {
                label = label.substring(0, 7) + "…";
            }
            g.text(label, slotX + slotW / 2, slotY + slotH / 2);

            // 変更不可スロットにはロックアイコン
            if (!slot.isConfigurable()) {
                g.textSize(8 * scale);
                g.text("🔒", slotX + slotW - 10 * scale, slotY + 10 * scale);
            }
        }
    }

    /**
     * Dashboardパネルのクリック処理。
     */
    private void handleDashboardClick(int mouseX, int mouseY) {
        int px = 16;
        int pw = 400 - 32;
        int previewY = 70 + 40 + 24;  // ヘッダー + 説明 + ラベル
        int previewHeight = 220;

        // プレビュー領域内のクリック
        if (mouseY >= previewY && mouseY < previewY + previewHeight) {
            handleDashboardPreviewClick(mouseX - px - 4, mouseY - previewY - 4, pw - 8, previewHeight - 8);
            return;
        }

        // ウィジェットリストのクリック
        if (selectedDashboardSlot != null) {
            int listY = previewY + previewHeight + 16 + 24;  // プレビュー後 + スペース + ラベル
            int itemHeight = 48;

            var registry = kernel != null ? kernel.getDashboardWidgetRegistry() : null;
            if (registry != null) {
                // 選択中スロットに適合するウィジェットのリスト
                var compatibleWidgets = new java.util.ArrayList<jp.moyashi.phoneos.core.dashboard.IDashboardWidget>();
                for (var widget : availableWidgets) {
                    if (widget.getSize() == selectedDashboardSlot.getRequiredSize()) {
                        compatibleWidgets.add(widget);
                    }
                }

                int widgetIndex = (mouseY - listY) / itemHeight;
                if (widgetIndex >= 0 && widgetIndex < compatibleWidgets.size()) {
                    var selectedWidget = compatibleWidgets.get(widgetIndex);
                    registry.assignWidgetToSlot(selectedDashboardSlot, selectedWidget.getId());
                    registry.saveAssignments();
                    System.out.println("SettingsScreen: Assigned widget " + selectedWidget.getId() + " to slot " + selectedDashboardSlot.name());
                }
            }
        }
    }

    /**
     * プレビュー領域でのクリック処理（Dashboard）。
     */
    private void handleDashboardPreviewClick(int localX, int localY, int previewW, int previewH) {
        float scale = (float) previewW / 360f;

        for (var slot : jp.moyashi.phoneos.core.dashboard.DashboardSlot.values()) {
            // 変更不可スロットはスキップ
            if (!slot.isConfigurable()) continue;

            float slotX = (slot.getX() - 20) * scale;
            float slotY = (slot.getY() - 80) * scale;
            float slotW = slot.getWidth() * scale;
            float slotH = slot.getHeight() * scale;

            if (localX >= slotX && localX < slotX + slotW &&
                    localY >= slotY && localY < slotY + slotH) {
                selectedDashboardSlot = slot;
                System.out.println("SettingsScreen: Selected dashboard slot: " + slot.name());
                return;
            }
        }
    }
}









