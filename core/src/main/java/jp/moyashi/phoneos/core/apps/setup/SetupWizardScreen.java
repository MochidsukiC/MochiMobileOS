package jp.moyashi.phoneos.core.apps.setup;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.resource.ResourceManager;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen;
import jp.moyashi.phoneos.core.service.SettingsManager;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

public class SetupWizardScreen implements Screen {
    private final Kernel kernel;
    
    // Animation States
    private float introAlpha = 0; // Fade in entire screen
    private float contentY = 50; // Slide up content
    private float buttonScale = 1.0f;
    private boolean isButtonPressed = false;
    
    // Screen dimensions
    private int screenWidth;
    private int screenHeight;
    
    // Theme Colors (Matching InstallationScreen)
    private final int BG_TOP = 0xFF0F2027;
    private final int BG_BOTTOM = 0xFF2C5364;
    private final int ACCENT_COLOR = 0xFF00D2FF;
    private final int BUTTON_HOVER = 0xFF3A7BD5;

    // Visuals
    private List<Particle> particles;
    private float time = 0;

    public SetupWizardScreen(Kernel kernel) {
        this.kernel = kernel;
        this.screenWidth = kernel.width;
        this.screenHeight = kernel.height;
        
        // Initialize Particles
        particles = new ArrayList<>();
        // Fewer particles for cleaner look
        for (int i = 0; i < 30; i++) {
            particles.add(new Particle());
        }
    }

    @Override
    public void setup(PGraphics g) {
        // Initial setup if needed
        this.screenWidth = g.width;
        this.screenHeight = g.height;
    }

    @Override
    public void tick() {
        // Intro Animation
        if (introAlpha < 255) {
            introAlpha += 5;
            if (introAlpha > 255) introAlpha = 255;
        }
        
        // Content Slide Up
        if (contentY > 0) {
            contentY *= 0.9f;
            if (contentY < 0.1f) contentY = 0;
        }

        // Particle Animation
        time += 0.02f;
        for (Particle p : particles) {
            p.update();
        }
        
        // Button Animation (Solid style)
        if (!isButtonPressed) {
            buttonScale += (1.0f - buttonScale) * 0.2f;
        } else {
            buttonScale += (0.95f - buttonScale) * 0.2f;
        }
    }

    @Override
    public void draw(PGraphics g) {
        this.screenWidth = g.width;
        this.screenHeight = g.height;

        // Draw Background
        drawGradient(g, 0, 0, screenWidth, screenHeight, BG_TOP, BG_BOTTOM);
        
        // Draw Particles
        g.noStroke();
        for (Particle p : particles) {
            p.draw(g);
        }

        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f;

        // Apply intro Fade & Slide
        g.pushMatrix();
        g.translate(0, contentY);
        
        // Draw "Glass" Card Background
        float cardW = 300;
        float cardH = 400;
        g.rectMode(PGraphics.CENTER);
        
        // Glass Shadow
        g.fill(0, 50);
        g.noStroke();
        g.rect(centerX + 5, centerY + 5, cardW, cardH, 20);
        
        // Glass Body
        g.fill(255, 20); // Very transparent white
        g.stroke(255, 80); // Subtle white border
        g.strokeWeight(1);
        g.rect(centerX, centerY, cardW, cardH, 20);

        // --- Content inside card ---
        
        // "Hello." Title
        g.fill(255, introAlpha);
        g.textAlign(PGraphics.CENTER, PGraphics.CENTER);
        g.textSize(48); // Big Hero Text
        g.text("Hello.", centerX, centerY - 80);
        
        // Subtitle
        g.fill(200, 200, 255, introAlpha);
        g.textSize(14);
        g.textLeading(20);
        g.text("Welcome to MochiMobileOS.\nYour personal digital space\nis ready to explore.", centerX, centerY - 10);

        // "Start" Button
        drawStartButton(g, centerX, centerY + 100);

        g.popMatrix();
    }
    
    private void drawStartButton(PGraphics g, float x, float y) {
        float btnW = 160;
        float btnH = 44;
        
        g.pushMatrix();
        g.translate(x, y);
        g.scale(buttonScale);
        
        // Button Glow
        g.noStroke();
        for (int i = 0; i < 5; i++) {
            g.fill(ACCENT_COLOR, 20 - i * 4);
            g.rect(0, 0, btnW + i * 4, btnH + i * 4, 25);
        }
        
        // Button Body (Gradient)
        // Since we can't easily do gradient fill in rect without shaders or loops, 
        // we use a solid color with a nice highlight
        g.fill(ACCENT_COLOR);
        g.rect(0, 0, btnW, btnH, 22);
        
        // Button Highlight (Top shine)
        g.fill(255, 50);
        g.rect(0, -btnH/4, btnW - 10, btnH/2, 20);
        
        // Button Text
        g.fill(0xFF0F2027); // Dark text on bright button
        g.textSize(16);
        // Assuming font is loaded, otherwise use default
        g.text("Get Started", 0, -2); // -2 for visual center
        
        g.popMatrix();
    }

    @Override
    public void mousePressed(PApplet p, int x, int y) {
        // Check collision with Start Button
        // Use screenWidth/screenHeight which are updated in draw()
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 2f + 100 + contentY; // Apply slide offset
        float btnW = 160 / 2; // Half width for hit test
        float btnH = 44 / 2;
        
        if (x > centerX - btnW && x < centerX + btnW &&
            y > centerY - btnH && y < centerY + btnH) {
            isButtonPressed = true;
        }
    }
    
    @Override
    public void mouseReleased(PApplet p, int x, int y) {
        if (isButtonPressed) {
            isButtonPressed = false;
            // Check if still inside to trigger action
            float centerX = screenWidth / 2f;
            float centerY = screenHeight / 2f + 100 + contentY;
            float btnW = 160 / 2;
            float btnH = 44 / 2;
            
            if (x > centerX - btnW && x < centerX + btnW &&
                y > centerY - btnH && y < centerY + btnH) {
                finishSetup();
            }
        }
    }

    private void finishSetup() {
        // Mark setup as completed
        SettingsManager settings = kernel.getService(SettingsManager.class);
        if (settings != null) {
            settings.setSetting("system.setup_completed", true);
            settings.saveSettings();
        }

        // Navigate to Home Screen
        if (kernel.getScreenManager() != null) {
            kernel.getScreenManager().clearAllScreens();
            kernel.getScreenManager().pushScreen(new HomeScreen(kernel));
        }
    }

    // Helper for gradients
    private void drawGradient(PGraphics g, float x, float y, float w, float h, int c1, int c2) {
        g.noFill();
        for (int i = 0; i <= h; i++) {
            float inter = PApplet.map(i, 0, h, 0, 1);
            int c = g.lerpColor(c1, c2, inter);
            g.stroke(c);
            g.line(x, y + i, x + w, y + i);
        }
    }
    
    // Simple Particle class (Reused logic but independent instance)
    private class Particle {
        float x, y, size, speedY, alpha;

        Particle() {
            reset(true);
        }

        void reset(boolean randomY) {
            // Use instance fields screenWidth/screenHeight
            float w = screenWidth;
            float h = screenHeight;
            x = (float)Math.random() * w;
            y = randomY ? (float)Math.random() * h : h + 10;
            size = (float)Math.random() * 2 + 1; // Smaller particles
            speedY = (float)Math.random() * 0.5f + 0.2f; // Slower
            alpha = (float)Math.random() * 100 + 20;
        }

        void update() {
            y -= speedY;
            if (y < -10) {
                reset(false);
            }
        }

        void draw(PGraphics g) {
            g.fill(255, alpha);
            g.ellipse(x, y, size, size);
        }
    }
}
