package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.apps.launcher.model.HomePage;
import jp.moyashi.phoneos.core.input.GestureListener;
import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import processing.core.PApplet;
import processing.core.PImage;
import jp.moyashi.phoneos.core.ui.popup.PopupMenu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AppLibraryScreen implements Screen, GestureListener {

    private static final boolean DEBUG = false;

    private final Kernel kernel;
    private int backgroundColor;
    private int textColor;
    private int accentColor;
    private boolean isInitialized;
    private List<IApplication> allApps;
    private int scrollOffset;
    private HomeScreen homeScreen;
    private long touchStartTime;
    private IApplication tappedApp;
    private static final int ITEM_HEIGHT = 70;
    private static final int LIST_START_Y = 60;

    public AppLibraryScreen(Kernel kernel) {
        this.kernel = kernel;
        this.backgroundColor = 0x141414; // Darker background
        this.textColor = 0xFFFFFF;
        this.accentColor = 0x4A90E2;
        this.isInitialized = false;
        this.scrollOffset = 0;
    }

    public void setHomeScreen(HomeScreen homeScreen) {
        this.homeScreen = homeScreen;
    }

    @Override
    public void setup(PApplet p) {
        isInitialized = true;
        loadAllApps();
        if (kernel != null && kernel.getGestureManager() != null) {
            kernel.getGestureManager().addGestureListener(this);
        }
    }

    @Override
    public void draw(PApplet p) {
        p.background(backgroundColor);
        drawHeader(p);
        drawAppList(p);
        if (needsScrolling()) {
            drawScrollIndicator(p);
        }
        drawNavigationHint(p);
    }

    @Override
    public void mousePressed(PApplet p, int mouseX, int mouseY) {
        touchStartTime = System.currentTimeMillis();
        tappedApp = getAppAtPosition(mouseX, mouseY);
    }

    @Override
    public void mouseReleased(PApplet p, int mouseX, int mouseY) {
        tappedApp = null;
    }

    @Override
    public void cleanup(PApplet p) {
        if (kernel != null && kernel.getGestureManager() != null) {
            kernel.getGestureManager().removeGestureListener(this);
        }
        isInitialized = false;
        allApps = null;
    }

    @Override
    public String getScreenTitle() {
        return "App Library";
    }

    private void loadAllApps() {
        if (kernel != null && kernel.getAppLoader() != null) {
            allApps = new ArrayList<>(kernel.getAppLoader().getLoadedApps());
            allApps.removeIf(app -> app.getApplicationId().equals("jp.moyashi.phoneos.core.apps.launcher"));
            allApps.sort(Comparator.comparing(IApplication::getName, String.CASE_INSENSITIVE_ORDER));
        }
    }

    private void drawHeader(PApplet p) {
        p.fill(0x1A1A1A);
        p.noStroke();
        p.rect(0, 0, 400, LIST_START_Y);
        p.stroke(textColor);
        p.strokeWeight(2);
        p.line(20, 30, 30, 20);
        p.line(20, 30, 30, 40);
        p.fill(textColor);
        p.noStroke();
        p.textAlign(p.LEFT, p.CENTER);
        p.textSize(18);
        p.text("App Library", 50, 30);
        p.textAlign(p.RIGHT, p.CENTER);
        p.textSize(12);
        p.fill(textColor, 150);
        if (allApps != null) {
            p.text(allApps.size() + " apps", 380, 30);
        }
        p.stroke(0x333333);
        p.strokeWeight(1);
        p.line(0, LIST_START_Y - 1, 400, LIST_START_Y - 1);
    }

    private void drawAppList(PApplet p) {
        if (allApps == null || allApps.isEmpty()) {
            p.fill(textColor, 150);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text("No applications installed", 200, 300);
            return;
        }
        int visibleHeight = 600 - LIST_START_Y - 40;
        for (int i = 0; i < allApps.size(); i++) {
            int itemY = LIST_START_Y + (i * ITEM_HEIGHT) - scrollOffset;
            if (itemY + ITEM_HEIGHT < LIST_START_Y || itemY > 600) {
                continue;
            }
            drawAppItem(p, allApps.get(i), itemY, i == allApps.size() - 1);
        }
    }

    private void drawAppItem(PApplet p, IApplication app, int y, boolean isLast) {
        if (tappedApp == app && System.currentTimeMillis() - touchStartTime < 100) {
            p.fill(50, 50, 50, 150);
        } else {
            p.fill(58, 58, 58, 100);
        }
        p.noStroke();
        p.rect(20, y, 360, ITEM_HEIGHT, 8);
        drawAppIcon(p, app, 20 + 35, y + 35, 40);
        p.fill(255);
        p.textAlign(p.LEFT, p.CENTER);
        p.textSize(16);
        p.text(app.getName(), 20 + 75, y + 25);
        if (app.getDescription() != null && !app.getDescription().isEmpty()) {
            p.fill(255, 150);
            p.textSize(12);
            String description = app.getDescription();
            if (description.length() > 40) {
                description = description.substring(0, 37) + "...";
            }
            p.text(description, 20 + 75, y + 45);
        }
        if (!isLast) {
            p.stroke(80, 80, 80);
            p.strokeWeight(1);
            p.line(20 + 10, y + ITEM_HEIGHT, 20 + 360 - 10, y + ITEM_HEIGHT);
        }
    }

    private void drawAppIcon(PApplet p, IApplication app, int centerX, int centerY, float iconSize) {
        if (app == null) return;
        PImage icon = app.getIcon(p);
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
                p.text(app.getName().substring(0, 1).toUpperCase(), centerX, centerY - 2);
            }
            p.rectMode(p.CORNER);
        }
    }

    private void drawScrollIndicator(PApplet p) {
        if (allApps == null) return;
        int totalHeight = allApps.size() * ITEM_HEIGHT;
        int visibleHeight = 600 - LIST_START_Y - 40;
        if (totalHeight <= visibleHeight) return;
        float scrollRatio = (float) scrollOffset / (totalHeight - visibleHeight);
        float thumbHeight = (float) visibleHeight * visibleHeight / totalHeight;
        float thumbY = LIST_START_Y + scrollRatio * (visibleHeight - thumbHeight);
        p.fill(255, 100);
        p.noStroke();
        p.rect(390, thumbY, 4, thumbHeight, 2);
    }

    private void drawNavigationHint(PApplet p) {
        p.fill(textColor, 100);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(12);
        p.text("Tap an app to launch • Tap header to go back", 200, 580);
    }

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

    private boolean needsScrolling() {
        if (allApps == null) return false;
        int totalHeight = allApps.size() * ITEM_HEIGHT;
        int visibleHeight = 600 - LIST_START_Y - 40;
        return totalHeight > visibleHeight;
    }

    private void goBack() {
        if (kernel != null && kernel.getScreenManager() != null) {
            kernel.getScreenManager().popScreen();
        }
    }

    private void launchApplication(IApplication app) {
        if (kernel != null && kernel.getScreenManager() != null) {
            try {
                kernel.getScreenManager().pushScreen(app.getEntryScreen(kernel));
            } catch (Exception e) {
                System.err.println("AppLibraryScreen: Failed to launch app " + app.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void addAppToHome(IApplication app) {
        if (homeScreen != null) {
            homeScreen.addShortcutToHomeScreen(app);
        }
    }

    @Override
    public boolean onGesture(GestureEvent event) {
        switch (event.getType()) {
            case TAP:
                return handleTap(event.getCurrentX(), event.getCurrentY());
            case LONG_PRESS:
                return handleLongPress(event.getCurrentX(), event.getCurrentY());
            case DRAG_MOVE:
                return handleDrag(event);
            default:
                return false;
        }
    }

    @Override
    public boolean isInBounds(int x, int y) {
        return kernel != null && kernel.getScreenManager() != null && kernel.getScreenManager().getCurrentScreen() == this;
    }

    @Override
    public int getPriority() {
        return 100;
    }

    private boolean handleTap(int x, int y) {
        if (y < LIST_START_Y) {
            goBack();
            return true;
        }
        IApplication app = getAppAtPosition(x, y);
        if (app != null) {
            launchApplication(app);
            return true;
        }
        return false;
    }

    private boolean handleLongPress(int x, int y) {
        IApplication app = getAppAtPosition(x, y);
        if (app != null) {
            showContextMenuForApp(app);
            return true;
        }
        return false;
    }

    private boolean handleDrag(GestureEvent event) {
        int deltaY = event.getCurrentY() - event.getStartY();
        scrollOffset -= deltaY;
        int maxScroll = Math.max(0, allApps.size() * ITEM_HEIGHT - (600 - LIST_START_Y - 40));
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        return true;
    }

    private void showContextMenuForApp(IApplication app) {
        if (kernel.getPopupManager() != null) {
            PopupMenu popup = new PopupMenu(app.getName())
                .addItem("ホーム画面に追加", () -> addAppToHome(app))
                .addSeparator()
                .addItem("キャンセル", () -> {});
            kernel.getPopupManager().showPopup(popup);
        }
    }
}
