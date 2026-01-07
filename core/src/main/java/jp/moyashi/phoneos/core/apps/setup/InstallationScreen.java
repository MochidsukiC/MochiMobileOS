package jp.moyashi.phoneos.core.apps.setup;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import java.util.ArrayList;
import java.util.List;

public class InstallationScreen implements Screen {
    private final Kernel kernel;
    
    // Logic
    private float targetProgress = 0;
    private float currentProgress = 0;
    private String statusText = "Initializing...";
    private long lastUpdate = 0;
    private boolean isFinished = false;

    // Visuals
    private PImage logo;
    private List<Particle> particles;
    private float time = 0;
    
    // Screen dimensions
    private int screenWidth;
    private int screenHeight;
    
    // Theme Colors (Deep Space Theme)
    private final int BG_TOP = 0xFF0F2027;
    private final int BG_BOTTOM = 0xFF2C5364;
    private final int ACCENT_COLOR = 0xFF00D2FF; // Cyan
    private final int ACCENT_GLOW = 0xFF3A7BD5; // Blue glow

    public InstallationScreen(Kernel kernel) {
        this.kernel = kernel;
        this.screenWidth = kernel.width;
        this.screenHeight = kernel.height;
    }

    @Override
    public void setup(PGraphics g) {
        lastUpdate = System.currentTimeMillis();
        this.screenWidth = g.width;
        this.screenHeight = g.height;
        
        // Load logo
        try {
            logo = kernel.getResourceManager().loadImage("assets/mochios/textures/setup_logo.png");
            if (logo == null) {
                logo = kernel.getResourceManager().loadImage("/assets/mochios/textures/setup_logo.png");
            }
        } catch (Exception e) {
            System.err.println("InstallationScreen: Failed to load logo");
        }

        // Initialize Particles
        particles = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            particles.add(new Particle(screenWidth, screenHeight));
        }
    }

    @Override
    public void tick() {
        if (isFinished) return;

        time += 0.05f;

        // Simulate installation progress
        long now = System.currentTimeMillis();
        if (now - lastUpdate > 50) {
            // Non-linear progress simulation
            float increment = (float)(Math.random() * 0.8 + 0.1);
            if (targetProgress > 80) increment *= 0.2; // Slow down at the end
            
            targetProgress += increment;
            lastUpdate = now;

            if (targetProgress < 20) statusText = "Formatting System Partition...";
            else if (targetProgress < 40) statusText = "Extracting Core Libraries...";
            else if (targetProgress < 60) statusText = "Installing Kernel Modules...";
            else if (targetProgress < 80) statusText = "Configuring User Environment...";
            else if (targetProgress < 95) statusText = "Finalizing Installation...";
            else statusText = "Welcome to MochiMobileOS";

            if (targetProgress >= 100) {
                targetProgress = 100;
                // Add a small delay at 100% before switching
                if (currentProgress > 99.5f) {
                    if (kernel.getScreenManager() != null) {
                        isFinished = true;
                        kernel.getScreenManager().pushScreen(new SetupWizardScreen(kernel));
                    }
                }
            }
        }
        
        // Smooth progress animation
        currentProgress += (targetProgress - currentProgress) * 0.1f;
        
        // Update particles (visuals in tick)
        for (Particle p : particles) {
            p.update();
        }
    }

    @Override
    public void draw(PGraphics g) {
        this.screenWidth = g.width;
        this.screenHeight = g.height;

        // Draw Background Gradient
        drawGradient(g, 0, 0, screenWidth, screenHeight, BG_TOP, BG_BOTTOM);

        // Draw Particles
        g.noStroke();
        for (Particle p : particles) {
            // Update logic is in tick(), but draw here
            p.draw(g);
        }

        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        // Draw Logo (Static and prominent)
        if (logo != null) {
            float logoBaseSize = 120;
            
            // Calculate dimensions maintaining aspect ratio
            float w = logoBaseSize;
            float h = logoBaseSize;
            if (logo.width > 0 && logo.height > 0) {
                float aspect = (float)logo.width / (float)logo.height;
                if (aspect >= 1) {
                    h = w / aspect;
                } else {
                    w = h * aspect;
                }
            }
            
            // Logo Shadow/Glow (Steady aura)
            g.imageMode(PGraphics.CENTER);
            g.tint(ACCENT_GLOW, 80);
            g.image(logo, centerX, centerY - 60, w + 20, h + 20);
            
            // Actual Logo
            g.tint(255);
            g.image(logo, centerX, centerY - 60, w, h);
        }

        // Draw Progress Area
        float barWidth = 200;
        float barHeight = 4;
        float barY = centerY + 80;

        // Progress Bar Background
        g.noStroke();
        g.fill(255, 30);
        g.rectMode(PGraphics.CORNER);
        g.rect(centerX - barWidth/2, barY, barWidth, barHeight, 2);

        // Progress Bar Fill (with Glow)
        float fillWidth = barWidth * (currentProgress / 100f);
        
        // Glow layer
        g.fill(ACCENT_COLOR, 100);
        g.rect(centerX - barWidth/2 - 2, barY - 2, fillWidth + 4, barHeight + 4, 4);
        
        // Main fill
        g.fill(ACCENT_COLOR);
        g.rect(centerX - barWidth/2, barY, fillWidth, barHeight, 2);

        // Text Info
        g.textAlign(PGraphics.CENTER);
        g.fill(255, 200);
        g.textSize(12);
        g.text(statusText, centerX, barY + 30);
        
        g.fill(255, 100);
        g.textSize(10);
        g.text((int)currentProgress + "%", centerX, barY + 45);
    }

    private void drawGradient(PGraphics g, float x, float y, float w, float h, int c1, int c2) {
        g.noFill();
        for (int i = 0; i <= h; i++) {
            float inter = PApplet.map(i, 0, h, 0, 1);
            int c = g.lerpColor(c1, c2, inter);
            g.stroke(c);
            g.line(x, y + i, x + w, y + i);
        }
    }

    // Inner class for visuals
    private class Particle {
        float x, y, size, speedY, alpha;

        Particle(float w, float h) {
            reset(w, h, true);
        }

        void reset(float w, float h, boolean randomY) {
            x = (float)Math.random() * w;
            y = randomY ? (float)Math.random() * h : h + 10;
            size = (float)Math.random() * 3 + 1;
            speedY = (float)Math.random() * 1.5f + 0.5f;
            alpha = (float)Math.random() * 150 + 50;
        }

        void update() {
            y -= speedY;
            if (y < -10) {
                // Reset with current screen dimensions
                reset(screenWidth, screenHeight, false);
            }
        }

        void draw(PGraphics g) {
            g.fill(255, alpha);
            g.ellipse(x, y, size, size);
        }
    }
}
