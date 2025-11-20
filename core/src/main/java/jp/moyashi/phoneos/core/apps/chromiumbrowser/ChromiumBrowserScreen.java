package jp.moyashi.phoneos.core.apps.chromiumbrowser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumSurface;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.components.Button;
import jp.moyashi.phoneos.core.ui.components.TextField;
import processing.core.PApplet;
import processing.core.PGraphics;
import java.util.Optional;
import processing.core.PImage;
import java.util.Optional;

public class ChromiumBrowserScreen implements Screen {



    private final Kernel kernel;

    private String initialUrl = "https://www.google.com";



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

    private Optional<ChromiumSurface> getActiveBrowserSurface() {
        if (kernel != null && kernel.getChromiumService() != null) {
            return kernel.getChromiumService().getActiveSurface();
        }
        return Optional.empty();
    }

    @Override
    public String getScreenTitle() {
        return "Chromium Browser";
    }

    @Override
    public void onForeground() {}

    @Override
    public void onBackground() {}
}