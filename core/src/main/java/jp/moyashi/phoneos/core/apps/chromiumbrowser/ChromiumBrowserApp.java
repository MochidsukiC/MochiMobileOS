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
        
        // アンチエイリアスと高品質描画設定
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);

        // 背景: リッチなグラデーション (左上:水色 -> 右下:濃い青)
        java.awt.Color colorTopLeft = new java.awt.Color(41, 182, 246); // Light Blue 400
        java.awt.Color colorBottomRight = new java.awt.Color(2, 119, 189); // Light Blue 800
        
        // テーマが適用されていれば、プライマリカラーをベースにグラデーション生成
        if (kernel != null && kernel.getThemeEngine() != null) {
            int primary = kernel.getThemeEngine().colorPrimary();
            java.awt.Color themeColor = new java.awt.Color((primary >> 16) & 0xFF, (primary >> 8) & 0xFF, primary & 0xFF);
            colorTopLeft = themeColor;
            colorBottomRight = themeColor.darker().darker();
        }

        java.awt.GradientPaint gradient = new java.awt.GradientPaint(
            0, 0, colorTopLeft,
            size, size, colorBottomRight
        );
        g.setPaint(gradient);
        g.fillRoundRect(0, 0, size, size, 16, 16); // 少し角丸にする

        // シンボル: 惑星と軌道
        g.setColor(java.awt.Color.WHITE);
        g.setStroke(new java.awt.BasicStroke(3.0f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));

        int centerX = size / 2;
        int centerY = size / 2;

        // メインの惑星
        int planetSize = 24;
        g.fillOval(centerX - planetSize/2, centerY - planetSize/2, planetSize, planetSize);
        
        // 軌道リング (傾いた楕円)
        java.awt.geom.AffineTransform oldTransform = g.getTransform();
        g.rotate(Math.toRadians(-30), centerX, centerY);
        
        // 外側のリング
        g.setStroke(new java.awt.BasicStroke(3.5f));
        g.drawOval(centerX - 24, centerY - 10, 48, 20);
        
        // リングの手前側を少し消して、惑星の後ろを通っているように見せる工夫（簡易的）
        // 実際には惑星を再描画して隠す
        g.setTransform(oldTransform);
        
        // 惑星を少し小さく再描画して、リングの一部を上書きする（立体感）
        // ただしシンプルにするため、今回は「リングの中にある惑星」としてベタ塗り
        g.fillOval(centerX - planetSize/2, centerY - planetSize/2, planetSize, planetSize);

        // 小さな衛星（装飾）
        g.fillOval(centerX + 16, centerY - 16, 6, 6);

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