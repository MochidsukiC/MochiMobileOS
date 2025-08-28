package jp.moyashi.phoneos.core.apps.launcher.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PApplet;

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
        System.out.println("✅ SimpleHomeScreen: Created simple home screen for debugging");
    }
    
    @Override
    public void setup() {
        isInitialized = true;
        System.out.println("🚀 SimpleHomeScreen: Setup complete!");
    }
    
    @Override
    public void draw(PApplet p) {
        frameCount++;
        
        // Log first few frames
        if (frameCount <= 5) {
            System.out.println("🎨 SimpleHomeScreen: Drawing frame " + frameCount);
        }
        
        try {
            // Let Kernel handle background - don't override it
            // p.background(30, 30, 50); // Commented out to allow Kernel debug display
            
            // Title
            p.fill(255);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(24);
            p.text("MochiMobileOS", p.width/2, 50);
            
            // Subtitle
            p.textSize(16);
            p.text("Simple Home Screen", p.width/2, 80);
            
            // Status
            p.textSize(12);
            p.text("Initialized: " + isInitialized, p.width/2, 120);
            p.text("Frame: " + frameCount, p.width/2, 140);
            
            // Simple rectangle as test
            p.fill(100, 150, 200);
            p.noStroke();
            p.rect(p.width/2 - 50, 200, 100, 60, 10);
            
            // Button text
            p.fill(255);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(14);
            p.text("Test Button", p.width/2, 230);
            
            // Instructions
            p.textSize(12);
            p.fill(200);
            p.text("Click anywhere to test", p.width/2, 300);
            
            // Show time if available
            if (kernel != null && kernel.getSystemClock() != null) {
                p.text("Time: " + kernel.getSystemClock().getFormattedTime(), p.width/2, 400);
            }
            
            // App count if available
            if (kernel != null && kernel.getAppLoader() != null) {
                int appCount = kernel.getAppLoader().getLoadedApps().size();
                p.text("Apps loaded: " + appCount, p.width/2, 420);
            }
            
        } catch (Exception e) {
            System.err.println("❌ SimpleHomeScreen: Error in draw() - " + e.getMessage());
            e.printStackTrace();
            
            // Emergency fallback
            p.background(255, 100, 100); // Light red
            p.fill(0);
            p.textAlign(p.CENTER, p.CENTER);
            p.textSize(16);
            p.text("DRAW ERROR", p.width/2, p.height/2);
            p.text(e.getMessage(), p.width/2, p.height/2 + 20);
        }
    }
    
    @Override
    public void mousePressed(int mouseX, int mouseY) {
        System.out.println("🖱️ SimpleHomeScreen: Mouse clicked at (" + mouseX + ", " + mouseY + ")");
        
        // Test: change background color on click
        // This would require storing state, but for now just log
        System.out.println("   Frame: " + frameCount + ", Initialized: " + isInitialized);
    }
    
    @Override
    public void cleanup() {
        isInitialized = false;
        System.out.println("🧹 SimpleHomeScreen: Cleanup completed");
    }
    
    @Override
    public String getScreenTitle() {
        return "Simple Home Screen (Debug)";
    }
}