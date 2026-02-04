package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * 表示問題をデバッグするためのHomeScreenの簡単なテストバージョン。
 * この最小限の実装により、画面上に何かを確実に表示できる。
 * 
 * @author YourName
 * @version 1.0 (Debug)
 */
public class SimpleHomeScreen implements Screen {
    
    private final Kernel kernel;
    private boolean isInitialized = false;
    private int frameCount = 0;
    
    public SimpleHomeScreen(Kernel kernel) {
        this.kernel = kernel;
    }
    
    @Override
    public void setup(PGraphics g) {
        isInitialized = true;
    }

    /**
     * @deprecated Use {@link #setup(PGraphics)} instead
     */
    @Override
    @Deprecated
    public void setup(processing.core.PApplet p) {
        PGraphics g = p.g;
        setup(g);
    }
    
    @Override
    public void draw(PGraphics g) {
        frameCount++;

        try {
            // Title
            g.fill(255);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(24);
            g.text("MochiMobileOS", g.width/2, 50);

            // Subtitle
            g.textSize(16);
            g.text("Simple Home Screen", g.width/2, 80);

            // Status
            g.textSize(12);
            g.text("Initialized: " + isInitialized, g.width/2, 120);
            g.text("Frame: " + frameCount, g.width/2, 140);

            // Simple rectangle as test
            g.fill(100, 150, 200);
            g.noStroke();
            g.rect(g.width/2 - 50, 200, 100, 60, 10);

            // Button text
            g.fill(255);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(14);
            g.text("Test Button", g.width/2, 230);

            // Instructions
            g.textSize(12);
            g.fill(200);
            g.text("Click anywhere to test", g.width/2, 300);

            // Show time if available
            if (kernel != null && kernel.getSystemClock() != null) {
                g.text("Time: " + kernel.getSystemClock().getFormattedTime(), g.width/2, 400);
            }

            // App count if available
            if (kernel != null && kernel.getAppLoader() != null) {
                int appCount = kernel.getAppLoader().getLoadedApps().size();
                g.text("Apps loaded: " + appCount, g.width/2, 420);
            }

        } catch (Exception e) {
            // Emergency fallback
            g.background(255, 100, 100); // Light red
            g.fill(0);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(16);
            g.text("DRAW ERROR", g.width/2, g.height/2);
            g.text(e.getMessage(), g.width/2, g.height/2 + 20);
        }
    }

    /**
     * @deprecated Use {@link #draw(PGraphics)} instead
     */
    @Override
    @Deprecated
    public void draw(PApplet p) {
        PGraphics g = p.g;
        draw(g);
    }
    
    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        // No-op for simple home screen
    }

    /**
     * @deprecated Use {@link #mousePressed(PGraphics, int, int)} instead
     */
    @Override
    @Deprecated
    public void mousePressed(processing.core.PApplet p, int mouseX, int mouseY) {
        PGraphics g = p.g;
        mousePressed(g, mouseX, mouseY);
    }
    
    @Override
    public void cleanup(PGraphics g) {
        isInitialized = false;
    }

    /**
     * @deprecated Use {@link #cleanup(PGraphics)} instead
     */
    @Override
    @Deprecated
    public void cleanup(processing.core.PApplet p) {
        PGraphics g = p.g;
        cleanup(g);
    }
    
    @Override
    public String getScreenTitle() {
        return "Simple Home Screen (Debug)";
    }

    /**
     * Adds mouseDragged support for PGraphics (empty implementation, can be overridden)
     */
    @Override
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        // Default implementation - subclasses can override
    }

    /**
     * Adds mouseReleased support for PGraphics (empty implementation, can be overridden)
     */
    @Override
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        // Default implementation - subclasses can override
    }

    /**
     * Adds keyPressed support for PGraphics (empty implementation, can be overridden)
     */
    @Override
    public void keyPressed(PGraphics g, char key, int keyCode) {
        // Default implementation - subclasses can override
    }
}