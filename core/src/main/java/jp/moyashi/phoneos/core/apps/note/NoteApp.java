package jp.moyashi.phoneos.core.apps.note;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;

/**
 * メモアプリケーション。
 * クリップボード機能のテストに使用。
 */
public class NoteApp implements IApplication {

    @Override
    public String getName() {
        return "メモ";
    }

    @Override
    public String getDescription() {
        return "テキストメモとクリップボードのテスト";
    }

    @Override
    public processing.core.PImage getIcon(Kernel kernel) {
        int size = 64;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();
        
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // ベース色 (紙の色): 淡いクリームイエロー
        java.awt.Color paperColor = new java.awt.Color(255, 245, 157); // Light Yellow 200
        java.awt.Color tapeColor = new java.awt.Color(255, 235, 59); // Default tape (Yellow 500)
        
        if (kernel != null && kernel.getThemeEngine() != null) {
            int primary = kernel.getThemeEngine().colorPrimary();
            // テープの色をテーマカラーにする
            tapeColor = new java.awt.Color((primary >> 16) & 0xFF, (primary >> 8) & 0xFF, primary & 0xFF);
            
            // もしテーマがダークモードなら、紙の色も少し暗くする
            if (kernel.getThemeEngine().getMode() == jp.moyashi.phoneos.core.ui.theme.ThemeEngine.Mode.DARK) {
                 paperColor = new java.awt.Color(210, 200, 130);
            }
        }

        // 背景 (紙)
        g.setColor(paperColor);
        g.fillRoundRect(0, 0, size, size, 4, 4); // 角は少しだけ丸める

        // 罫線
        g.setColor(new java.awt.Color(0, 0, 0, 40));
        g.setStroke(new java.awt.BasicStroke(2));
        int startY = 28;
        int gap = 10;
        for (int i = 0; i < 4; i++) {
            int y = startY + i * gap;
            g.drawLine(10, y, size - 10, y);
        }

        // 上部のテープ装飾 (テーマカラーを使用)
        // 半透明にして下の紙が透けている感じを出す
        g.setColor(new java.awt.Color(tapeColor.getRed(), tapeColor.getGreen(), tapeColor.getBlue(), 200));
        g.fillRect(16, -5, 32, 20); // 上にはみ出させて貼っている感を出す
        
        // めくれ効果（右下）
        java.awt.Color foldShadow = new java.awt.Color(0, 0, 0, 40);
        g.setColor(foldShadow);
        java.awt.Polygon fold = new java.awt.Polygon();
        fold.addPoint(size - 18, size);
        fold.addPoint(size, size - 18);
        fold.addPoint(size, size);
        g.fillPolygon(fold);
        
        // めくれ部分の裏側 (紙の色より少し明るく)
        g.setColor(paperColor.brighter());
        java.awt.Polygon foldBack = new java.awt.Polygon();
        foldBack.addPoint(size - 18, size);
        foldBack.addPoint(size, size - 18);
        foldBack.addPoint(size - 18, size - 18);
        g.fillPolygon(foldBack);

        g.dispose();
        
        return new processing.core.PImage(image);
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        if (kernel.getLogger() != null) {
            kernel.getLogger().info("NoteApp", "メモアプリを起動");
        }
        return new NoteScreen(kernel);
    }
}
