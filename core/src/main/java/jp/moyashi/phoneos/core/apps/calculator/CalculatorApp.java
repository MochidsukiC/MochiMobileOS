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
    
    // getIcon()はデフォルト実装（null返却）を使用し、システムが白いアイコンを生成
    
    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new CalculatorScreen(kernel);
    }
    
}