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
        
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);

        // テーマカラー取得
        java.awt.Color themeColor = new java.awt.Color(33, 150, 243); // Blue
        if (kernel != null && kernel.getThemeEngine() != null) {
            int primary = kernel.getThemeEngine().colorPrimary();
            themeColor = new java.awt.Color((primary >> 16) & 0xFF, (primary >> 8) & 0xFF, primary & 0xFF);
        }

        // 背景: 深みのあるダークブルーからテーマカラーへのグラデーション
        // これにより、どんなテーマカラーでも「ブラウザらしさ（青）」と「テーマ色」が調和する
        java.awt.Color deepSpace = new java.awt.Color(20, 30, 48);
        java.awt.GradientPaint bgGradient = new java.awt.GradientPaint(
            0, 0, deepSpace,
            size, size, themeColor
        );
        g.setPaint(bgGradient);
        g.fillRoundRect(0, 0, size, size, 16, 16);

        int centerX = size / 2;
        int centerY = size / 2;

        // 地球儀/グリッド (薄く)
        g.setColor(new java.awt.Color(255, 255, 255, 40));
        g.setStroke(new java.awt.BasicStroke(1.5f));
        g.drawOval(8, 8, size - 16, size - 16);
        g.drawOval(centerX - 10, 8, 20, size - 16);
        g.drawLine(8, centerY, size - 8, centerY);

        // コンパスの針 (赤と白で視認性アップ)
        java.awt.geom.Path2D needleRed = new java.awt.geom.Path2D.Double();
        needleRed.moveTo(centerX, centerY - 20); // Top
        needleRed.lineTo(centerX + 6, centerY);  // Right
        needleRed.lineTo(centerX - 6, centerY);  // Left
        needleRed.closePath();

        java.awt.geom.Path2D needleWhite = new java.awt.geom.Path2D.Double();
        needleWhite.moveTo(centerX, centerY + 20); // Bottom
        needleWhite.lineTo(centerX + 6, centerY);  // Right
        needleWhite.lineTo(centerX - 6, centerY);  // Left
        needleWhite.closePath();

        // 針の影
        g.translate(2, 2);
        g.setColor(new java.awt.Color(0, 0, 0, 80));
        g.fill(needleRed);
        g.fill(needleWhite);
        g.translate(-2, -2);

        // 針本体を描画 (45度傾ける)
        java.awt.geom.AffineTransform old = g.getTransform();
        g.rotate(Math.toRadians(45), centerX, centerY);
        
        g.setColor(new java.awt.Color(244, 67, 54)); // Red 500 (アクセント)
        g.fill(needleRed);
        
        g.setColor(new java.awt.Color(240, 240, 240)); // White
        g.fill(needleWhite);

        // 中心ピン
        g.setColor(new java.awt.Color(220, 220, 220));
        g.fillOval(centerX - 3, centerY - 3, 6, 6);
        g.setColor(deepSpace);
        g.fillOval(centerX - 1, centerY - 1, 2, 2);

        g.setTransform(old);

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