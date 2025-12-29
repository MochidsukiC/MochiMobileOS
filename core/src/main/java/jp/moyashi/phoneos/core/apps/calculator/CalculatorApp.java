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
    
    /**
     * Gets the icon for this application.
     * Generates a calculator icon dynamically based on the current theme.
     * 
     * @param kernel The OS kernel instance
     */
    @Override
    public PImage getIcon(Kernel kernel) {
        int size = 64;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();
        
        // Enable antialiasing
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // Theme colors
        java.awt.Color backgroundColor = new java.awt.Color(255, 149, 0); // Default: Orange
        java.awt.Color foregroundColor = java.awt.Color.WHITE;
        
        if (kernel != null && kernel.getThemeEngine() != null) {
            int primary = kernel.getThemeEngine().colorPrimary();
            int onPrimary = kernel.getThemeEngine().colorOnPrimary();
            backgroundColor = new java.awt.Color((primary >> 16) & 0xFF, (primary >> 8) & 0xFF, primary & 0xFF);
            foregroundColor = new java.awt.Color((onPrimary >> 16) & 0xFF, (onPrimary >> 8) & 0xFF, onPrimary & 0xFF);
        }

        // Background
        g.setColor(backgroundColor);
        g.fillRoundRect(0, 0, size, size, 16, 16);

        // Calculator symbols
        g.setColor(foregroundColor);
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 40));
        
        String text = "+";
        int textWidth = g.getFontMetrics().stringWidth(text);
        int textHeight = g.getFontMetrics().getAscent();
        
        // Center the "+" symbol
        g.drawString(text, (size - textWidth) / 2 + 1, (size + textHeight) / 2 - 12);
        
        // Small decorative symbols
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 14));
        g.drawString("-", 12, size/2 + 5);
        g.drawString("=", size - 20, size/2 + 5);
        g.drawString("x", size/2 - 4, 15);

        g.dispose();
        
        return new PImage(image);
    }
    
    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new CalculatorScreen(kernel);
    }
    
}