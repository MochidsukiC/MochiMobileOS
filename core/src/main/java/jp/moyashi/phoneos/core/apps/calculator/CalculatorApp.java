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
        
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // テーマカラー
        java.awt.Color themeColor = new java.awt.Color(255, 149, 0); // Default: Orange
        
        if (kernel != null && kernel.getThemeEngine() != null) {
            int primary = kernel.getThemeEngine().colorPrimary();
            themeColor = new java.awt.Color((primary >> 16) & 0xFF, (primary >> 8) & 0xFF, primary & 0xFF);
        }

        // 背景: 鮮やかなグラデーション (左上:明るい -> 右下:テーマ色)
        java.awt.GradientPaint bgGradient = new java.awt.GradientPaint(
            0, 0, themeColor.brighter(),
            size, size, themeColor
        );
        g.setPaint(bgGradient);
        g.fillRoundRect(0, 0, size, size, 16, 16);

        // かすかなグリッド模様 (電卓のキー配列をイメージ)
        g.setColor(new java.awt.Color(255, 255, 255, 30));
        g.fillRect(32, 0, 1, size);
        g.fillRect(0, 32, size, 1);

        // メイン記号 (+): ドロップシャドウ付き
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 42));
        String text = "+";
        int textWidth = g.getFontMetrics().stringWidth(text);
        int textHeight = g.getFontMetrics().getAscent();
        int x = (size - textWidth) / 2 + 1;
        int y = (size + textHeight) / 2 - 12;

        // 影
        g.setColor(new java.awt.Color(0, 0, 0, 60));
        g.drawString(text, x + 2, y + 2);
        
        // 本体
        g.setColor(java.awt.Color.WHITE);
        g.drawString(text, x, y);
        
        // 装飾記号: 右下に "=" を配置 (アクセントカラーの補色などで強調してもいいが、今回は白で統一し透明度で調整)
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 18));
        g.setColor(new java.awt.Color(255, 255, 255, 180));
        g.drawString("=", size - 18, size - 10);
        g.drawString("-", 10, size - 10);
        g.drawString("×", 10, 20);

        g.dispose();
        
        return new PImage(image);
    }
    
    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new CalculatorScreen(kernel);
    }
    
}