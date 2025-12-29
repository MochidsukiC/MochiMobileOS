package jp.moyashi.phoneos.core.apps.chromiumbrowser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PImage;

public class ChromiumBrowserApp implements IApplication {

    private Kernel kernel;

    @Override
    public String getApplicationId() {
        return "jp.moyashi.phoneos.core.apps.chromiumbrowser";
    }

    @Override
    public String getName() {
        return "Browser";
    }

    @Override
    public String getDescription() {
        return "A web browser based on Chromium.";
    }

    @Override
    public PImage getIcon(Kernel kernel) {
        int size = 64;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();
        
        // アンチエイリアス有効化
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // テーマカラー取得
        java.awt.Color backgroundColor = new java.awt.Color(33, 150, 243); // デフォルト: 青 (Blue 500)
        
        if (kernel != null && kernel.getThemeEngine() != null) {
            int primary = kernel.getThemeEngine().colorPrimary();
            // テーマのプライマリカラーを使用
            backgroundColor = new java.awt.Color((primary >> 16) & 0xFF, (primary >> 8) & 0xFF, primary & 0xFF);
        }

        // 背景 (円形)
        g.setColor(backgroundColor);
        g.fillOval(4, 4, size - 8, size - 8);

        // 地球儀のグリッド (薄い白線)
        g.setColor(new java.awt.Color(255, 255, 255, 100));
        g.setStroke(new java.awt.BasicStroke(2));
        
        // 縦線 (経線)
        g.drawOval(size/2 - 12, 4, 24, size - 8);
        g.drawLine(size/2, 4, size/2, size - 4);
        
        // 横線 (緯線)
        g.drawArc(4, 16, size - 8, size - 32, 0, 180); // 上
        g.drawArc(4, size - 16 - (size - 32), size - 8, size - 32, 180, 180); // 下
        g.drawLine(4, size/2, size - 4, size/2); // 赤道

        // コンパスのような中心点
        g.setColor(java.awt.Color.WHITE);
        int centerSize = 8;
        g.fillOval((size - centerSize) / 2, (size - centerSize) / 2, centerSize, centerSize);

        g.dispose();
        
        return new PImage(image);
    }

    @Override
    public void onInitialize(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new ChromiumBrowserScreen(kernel);
    }
}