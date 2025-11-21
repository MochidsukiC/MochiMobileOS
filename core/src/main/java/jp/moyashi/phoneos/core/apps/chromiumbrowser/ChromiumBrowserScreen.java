package jp.moyashi.phoneos.core.apps.chromiumbrowser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumSurface;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.components.Button;
import jp.moyashi.phoneos.core.ui.components.TextField;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.Optional;

public class ChromiumBrowserScreen implements Screen {

    private final Kernel kernel;
    private String initialUrl = "https://www.google.com"; // Default URL

    private Button backButton;
    private Button forwardButton;
    private Button reloadButton;
    private Button newTabButton;
    private Button tabListButton;
    private TextField addressBar;
    private boolean isEditingUrl = false;

    public ChromiumBrowserScreen(Kernel kernel) {
        this.kernel = kernel;
    }

    public ChromiumBrowserScreen(Kernel kernel, String url) {
        this.kernel = kernel;
        this.initialUrl = url;
    }

    @Override
    public void setup(PGraphics p) {
        initializeUI(p);
        
        // Create initial tab if no tabs exist
        if (kernel != null && kernel.getChromiumService() != null && kernel.getChromiumService().getSurfaces().isEmpty()) {
            kernel.getChromiumService().createTab(p.width - 20, p.height - 120, initialUrl);
        }
    }

    private void initializeUI(PGraphics p) {
        // Bottom bar buttons
        backButton = new Button(20, 555, 40, 40, "<");
        forwardButton = new Button(80, 555, 40, 40, ">");
        newTabButton = new Button(p.width / 2f - 20, 555, 40, 40, "+");
        tabListButton = new Button(p.width - 70, 555, 40, 40, "□");

        // Top bar button
        reloadButton = new Button(p.width - 60, 5, 40, 40, "R");

        // Address bar
        addressBar = new TextField(10, 5, p.width - 80, 40, "Enter URL");
        addressBar.setVisible(false);

        // Set click listeners
        backButton.setOnClickListener(() -> getActiveBrowserSurface().ifPresent(ChromiumSurface::goBack));
        forwardButton.setOnClickListener(() -> getActiveBrowserSurface().ifPresent(ChromiumSurface::goForward));
        reloadButton.setOnClickListener(() -> getActiveBrowserSurface().ifPresent(ChromiumSurface::reload));
        newTabButton.setOnClickListener(() -> {
            if (kernel != null && kernel.getChromiumService() != null) {
                kernel.getChromiumService().createTab(p.width - 20, p.height - 120, "https://www.google.com");
            }
        });
        tabListButton.setOnClickListener(() -> {
            // TODO: Implement TabListScreen
        });
    }

    @Override
    public void draw(PGraphics g) {
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme == null) return;

        Optional<ChromiumSurface> activeSurfaceOpt = getActiveBrowserSurface();

        // Update button states
        if (activeSurfaceOpt.isPresent()) {
            ChromiumSurface activeSurface = activeSurfaceOpt.get();
            backButton.setEnabled(activeSurface.canGoBack());
            forwardButton.setEnabled(activeSurface.canGoForward());
        } else {
            backButton.setEnabled(false);
            forwardButton.setEnabled(false);
        }

        // Pass modifier keys to address bar if it's focused
        if (isEditingUrl && addressBar.isFocused()) {
            addressBar.setShiftPressed(kernel.isShiftPressed());
            addressBar.setCtrlPressed(kernel.isCtrlPressed());
        }

        // Draw App Background
        int appBg = theme.colorBackground();
        g.background((appBg >> 16) & 0xFF, (appBg >> 8) & 0xFF, appBg & 0xFF);

        // Draw Top Address Bar (holographic)
        drawTopBar(g, theme);

        // Draw Content Area
        drawContentArea(g, theme);

        // Draw Bottom Navigation Bar (holographic)
        drawBottomBar(g, theme);
    }

    private void drawTopBar(PGraphics g, jp.moyashi.phoneos.core.ui.theme.ThemeEngine theme) {
        int surfaceColor = theme.colorSurface();
        int onSurfaceColor = theme.colorOnSurface();
        g.fill((surfaceColor >> 16) & 0xFF, (surfaceColor >> 8) & 0xFF, surfaceColor & 0xFF, 180);
        g.noStroke();
        g.rect(0, 0, g.width, 50);

        if (isEditingUrl) {
            addressBar.draw(g);
        } else {
            g.fill(onSurfaceColor);
            g.textAlign(g.LEFT, g.CENTER);
            g.textSize(14);
            String title = getActiveBrowserSurface().map(ChromiumSurface::getTitle).orElse("No Active Tab");
            g.text("🔒 " + title, 20, 25);
        }

        if (reloadButton != null) reloadButton.draw(g);
        if (newTabButton != null) newTabButton.draw(g);
        if (tabListButton != null) tabListButton.draw(g);
    }

    private void drawContentArea(PGraphics g, jp.moyashi.phoneos.core.ui.theme.ThemeEngine theme) {
        Optional<ChromiumSurface> activeSurfaceOpt = getActiveBrowserSurface();
        if (activeSurfaceOpt.isPresent()) {
            PImage frame = activeSurfaceOpt.get().acquireFrame();
            if (frame != null) {
                g.image(frame, 10, 60);
            }
        } else {
            g.fill(theme.colorSurface());
            g.rect(10, 60, g.width - 20, g.height - 120, 8);
            g.fill(theme.colorOnSurface());
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(20);
            g.text("No Active Tab", g.width / 2, g.height / 2);
        }
    }

    private void drawBottomBar(PGraphics g, jp.moyashi.phoneos.core.ui.theme.ThemeEngine theme) {
        int surfaceColor = theme.colorSurface();
        g.fill((surfaceColor >> 16) & 0xFF, (surfaceColor >> 8) & 0xFF, surfaceColor & 0xFF, 180);
        g.noStroke();
        g.rect(0, g.height - 50, g.width, 50);

        if (backButton != null) backButton.draw(g);
        if (forwardButton != null) forwardButton.draw(g);
        if (newTabButton != null) newTabButton.draw(g);
        if (tabListButton != null) tabListButton.draw(g);
    }

    @Override
    public void cleanup(PGraphics p) {
        // All surfaces are destroyed by ChromiumService on shutdown
    }

    @Override
    public String getScreenTitle() {
        return "Chromium Browser";
    }

    @Override
    public void onForeground() {}

    @Override
    public void onBackground() {}

    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        if (backButton.onMousePressed(mouseX, mouseY)) return;
        if (forwardButton.onMousePressed(mouseX, mouseY)) return;
        if (reloadButton.onMousePressed(mouseX, mouseY)) return;
        if (newTabButton.onMousePressed(mouseX, mouseY)) return;
        if (tabListButton.onMousePressed(mouseX, mouseY)) return;
        
        if (!isEditingUrl && mouseX > 10 && mouseX < g.width - 70 && mouseY > 5 && mouseY < 45) {
            isEditingUrl = true;
            addressBar.setVisible(true);
            addressBar.setFocused(true);
            getActiveBrowserSurface().ifPresent(s -> addressBar.setText(s.getCurrentUrl()));
            return;
        }

        if (isEditingUrl && addressBar.onMousePressed(mouseX, mouseY)) return;

        if (isEditingUrl && !addressBar.contains(mouseX, mouseY)) {
            isEditingUrl = false;
            addressBar.setVisible(false);
            addressBar.setFocused(false);
        }

        getActiveBrowserSurface().ifPresent(s -> s.sendMousePressed(mouseX - 10, mouseY - 60, 0));
    }

    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        if (backButton.onMouseReleased(mouseX, mouseY)) return;
        if (forwardButton.onMouseReleased(mouseX, mouseY)) return;
        if (reloadButton.onMouseReleased(mouseX, mouseY)) return;
        if (newTabButton.onMouseReleased(mouseX, mouseY)) return;
        if (tabListButton.onMouseReleased(mouseX, mouseY)) return;
        if (isEditingUrl) addressBar.onMouseReleased(mouseX, mouseY);
        
        getActiveBrowserSurface().ifPresent(s -> s.sendMouseReleased(mouseX - 10, mouseY - 60, 0));
    }

    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        if (isEditingUrl) addressBar.onMouseDragged(mouseX, mouseY);
        getActiveBrowserSurface().ifPresent(s -> s.sendMouseMoved(mouseX - 10, mouseY - 60));
    }
    
    public void mouseMoved(PGraphics g, int mouseX, int mouseY) {
        backButton.onMouseMoved(mouseX, mouseY);
        forwardButton.onMouseMoved(mouseX, mouseY);
        reloadButton.onMouseMoved(mouseX, mouseY);
        newTabButton.onMouseMoved(mouseX, mouseY);
        tabListButton.onMouseMoved(mouseX, mouseY);
        if (isEditingUrl) addressBar.onMouseMoved(mouseX, mouseY);
    }
    
    public void keyPressed(PGraphics g, char key, int keyCode) {
        if (isEditingUrl) {
            if (keyCode == PApplet.ENTER || keyCode == PApplet.RETURN) {
                getActiveBrowserSurface().ifPresent(s -> s.loadUrl(addressBar.getText()));
                isEditingUrl = false;
                addressBar.setFocused(false);
                addressBar.setVisible(false);
            } else {
                addressBar.onKeyPressed(key, keyCode);
            }
        } else {
            getActiveBrowserSurface().ifPresent(s -> s.sendKeyPressed(keyCode, key));
        }
    }
    
    public void keyReleased(PGraphics g, char key, int keyCode) {
        // BaseTextInput does not have onKeyReleased, so directly send to browser surface
        getActiveBrowserSurface().ifPresent(s -> s.sendKeyReleased(keyCode, key));
    }

    private Optional<ChromiumSurface> getActiveBrowserSurface() {
        if (kernel != null && kernel.getChromiumService() != null) {
            return kernel.getChromiumService().getActiveSurface();
        }
        return Optional.empty();
    }
}