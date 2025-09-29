package jp.moyashi.phoneos.core.apps.calculator;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.apps.calculator.ui.CalculatorScreen;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PGraphics;

/**
 * Simple calculator application for MochiMobileOS.
 * This app provides basic arithmetic operations in an intuitive interface.
 * 
 * @author MochiMobileOS
 * @version 1.0
 */
public class CalculatorApp implements IApplication {
    
    @Override
    public String getApplicationId() {
        return "jp.moyashi.phoneos.calculator";
    }
    
    @Override
    public String getName() {
        return "Calculator";
    }
    
    @Override
    public String getDescription() {
        return "Basic arithmetic calculator with a simple interface";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public PImage getIcon(PApplet p) {
        // Create a simple calculator icon using PGraphics
        PGraphics icon = p.createGraphics(64, 64);
        icon.beginDraw();

        // Background
        icon.background(255, 159, 10); // Orange background

        // Calculator display
        icon.fill(0);
        icon.rect(8, 8, 48, 16, 2);

        // Buttons
        icon.fill(255);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int x = 8 + col * 16;
                int y = 32 + row * 10;
                icon.rect(x, y, 12, 8, 1);
            }
        }

        icon.endDraw();
        return icon;
    }

    @Override
    public PImage getIcon(PGraphics g) {
        // TODO: PGraphics統一アーキテクチャに対応したアイコン作成機能を実装
        // 現在は暫定的にnullを返す
        System.out.println("CalculatorApp: PGraphics icon creation not yet implemented");
        return null;
    }
    
    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new CalculatorScreen(kernel);
    }
    
}