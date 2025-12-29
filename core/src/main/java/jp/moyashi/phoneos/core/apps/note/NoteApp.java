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
        
        // アンチエイリアス有効化
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        // テーマカラー取得
        java.awt.Color backgroundColor = new java.awt.Color(255, 235, 59); // デフォルト: 黄色 (Yellow 500)
        java.awt.Color lineColor = new java.awt.Color(0, 0, 0, 50); // 薄い黒線
        
        if (kernel != null && kernel.getThemeEngine() != null) {
            // テーマに合わせて少し色味を変えるなどの調整も可能だが、
            // メモ帳は黄色いイメージが強いので、ベースは黄色系を維持しつつ、
            // ダークモードなら少し暗くするなどの調整を行う
            // ここではシンプルに固定色とするが、将来的にテーマ対応可能
        }

        // 背景 (角丸四角形)
        g.setColor(backgroundColor);
        g.fillRoundRect(0, 0, size, size, 16, 16);
        
        // 上部のテープ風装飾（オプション）
        // g.setColor(new java.awt.Color(255, 255, 255, 100));
        // g.fillRect(16, 0, 32, 12);

        // 罫線を描画
        g.setColor(lineColor);
        g.setStroke(new java.awt.BasicStroke(2));
        int startY = 20;
        int gap = 12;
        for (int i = 0; i < 3; i++) {
            int y = startY + i * gap;
            g.drawLine(12, y, size - 12, y);
        }

        // めくれ効果（右下）
        java.awt.Color foldColor = new java.awt.Color(0, 0, 0, 30);
        g.setColor(foldColor);
        java.awt.Polygon fold = new java.awt.Polygon();
        fold.addPoint(size - 16, size);
        fold.addPoint(size, size - 16);
        fold.addPoint(size, size);
        g.fillPolygon(fold);
        
        // めくれ部分の裏側
        g.setColor(new java.awt.Color(255, 255, 255, 200));
        java.awt.Polygon foldBack = new java.awt.Polygon();
        foldBack.addPoint(size - 16, size);
        foldBack.addPoint(size, size - 16);
        foldBack.addPoint(size - 16, size - 16);
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
