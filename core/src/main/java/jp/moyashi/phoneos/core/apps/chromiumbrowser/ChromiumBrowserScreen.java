package jp.moyashi.phoneos.core.apps.chromiumbrowser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumSurface;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

public class ChromiumBrowserScreen implements Screen {

    private final Kernel kernel;
    private String currentUrl = "https://www.google.com";
    private final String surfaceId = "browser_" + System.currentTimeMillis();
    private ChromiumSurface browserSurface;

    public ChromiumBrowserScreen(Kernel kernel) {
        this.kernel = kernel;
    }

    public ChromiumBrowserScreen(Kernel kernel, String url) {
        this.kernel = kernel;
        this.currentUrl = url;
    }

    @Override
    public void setup(PGraphics p) {
        // Initialization logic for the browser screen
        if (kernel != null && kernel.getChromiumService() != null) {
            browserSurface = kernel.getChromiumService().createSurface(
                surfaceId,
                p.width - 20, // content area width
                p.height - 120, // content area height
                currentUrl
            );
        }
    }

    @Override
    public void draw(PGraphics g) {
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme == null) return;

        // Draw App Background
        int appBg = theme.colorBackground();
        g.background((appBg >> 16) & 0xFF, (appBg >> 8) & 0xFF, appBg & 0xFF);

        // Draw Top Address Bar (holographic)
        drawTopBar(g, theme);

        // Draw Content Area (placeholder)
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

        g.fill(onSurfaceColor);
        g.textAlign(g.LEFT, g.CENTER);
        g.textSize(14);
        g.text("🔒 " + currentUrl, 20, 25);
    }

    private void drawContentArea(PGraphics g, jp.moyashi.phoneos.core.ui.theme.ThemeEngine theme) {
        if (browserSurface != null) {
            PImage frame = browserSurface.acquireFrame();
            if (frame != null) {
                g.image(frame, 10, 60);
            }
        } else {
            // Placeholder for the actual web content
            g.fill(theme.colorSurface());
            g.rect(10, 60, g.width - 20, g.height - 120, 8);
            g.fill(theme.colorOnSurface());
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(20);
            g.text("Web Content Area", g.width / 2, g.height / 2);
        }
    }

    private void drawBottomBar(PGraphics g, jp.moyashi.phoneos.core.ui.theme.ThemeEngine theme) {
        int surfaceColor = theme.colorSurface();
        int onSurfaceColor = theme.colorOnSurface();
        g.fill((surfaceColor >> 16) & 0xFF, (surfaceColor >> 8) & 0xFF, surfaceColor & 0xFF, 180);
        g.noStroke();
        g.rect(0, g.height - 50, g.width, 50);

        // Placeholder for navigation buttons
        g.fill(onSurfaceColor);
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(24);
        g.text("◀", g.width * 0.15f, g.height - 25);
        g.text("▶", g.width * 0.35f, g.height - 25);
        g.text("➕", g.width * 0.55f, g.height - 25);
        g.text("□", g.width * 0.75f, g.height - 25);
        g.text("…", g.width * 0.95f, g.height - 25);
    }

    @Override
    public void cleanup(PGraphics p) {
        // Cleanup logic for the browser screen
        if (kernel != null && kernel.getChromiumService() != null) {
            kernel.getChromiumService().destroySurface(surfaceId);
        }
    }

    @Override
    public String getScreenTitle() {
        return "Chromium Browser";
    }

    @Override
    public void onForeground() {}

    @Override
    public void onBackground() {}

    // Input handling
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        if (browserSurface != null) {
            browserSurface.sendMousePressed(mouseX - 10, mouseY - 60, 0); // Assuming left-click (button 0)
        }
    }

    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        if (browserSurface != null) {
            browserSurface.sendMouseReleased(mouseX - 10, mouseY - 60, 0); // Assuming left-click (button 0)
        }
    }

    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        if (browserSurface != null) {
            browserSurface.sendMouseMoved(mouseX - 10, mouseY - 60); // CEF uses moved for drag
        }
    }

    public void mouseWheel(PGraphics g, int x, int y, float delta) {
        if (browserSurface != null) {
            browserSurface.sendMouseWheel(x - 10, y - 60, delta);
        }
    }

    public void keyPressed(PGraphics g, char key, int keyCode) {
        if (browserSurface != null) {
            browserSurface.sendKeyPressed(keyCode, key);
        }
    }

    public void keyReleased(PGraphics g, char key, int keyCode) {
        if (browserSurface != null) {
            browserSurface.sendKeyReleased(keyCode, key);
        }
    }
}