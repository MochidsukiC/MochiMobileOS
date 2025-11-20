package jp.moyashi.phoneos.core.ui.components;

import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * ダイアログコンポーネント。
 * モーダルダイアログの表示。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class Dialog extends Panel {

    private String title;
    private String message;
    private Button okButton;
    private Button cancelButton;
    private Runnable onOkListener;
    private Runnable onCancelListener;
    private boolean modal = true;

    public Dialog(float x, float y, float width, float height, String title, String message) {
        super(x, y, width, height);
        this.title = title;
        this.message = message;

        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            setBackgroundColor(theme.colorSurface());
            setBorderColor(theme.colorBorder());
            setBorderWidth(2);
            setCornerRadius(theme.radiusMd());
        } else {
            setBackgroundColor(0xFFFFFFFF);
            setBorderColor(0xFF666666);
            setBorderWidth(2);
            setCornerRadius(10);
        }

        // ボタン作成
        float buttonWidth = 80;
        float buttonHeight = 35;
        float buttonY = y + height - buttonHeight - 15;

        okButton = new Button(x + width - buttonWidth - 15, buttonY, buttonWidth, buttonHeight, "OK");
        okButton.setOnClickListener(() -> {
            if (onOkListener != null) onOkListener.run();
            setVisible(false);
        });

        cancelButton = new Button(x + width - buttonWidth * 2 - 25, buttonY, buttonWidth, buttonHeight, "Cancel");
        cancelButton.setOnClickListener(() -> {
            if (onCancelListener != null) onCancelListener.run();
            setVisible(false);
        });

        addChild(okButton);
        addChild(cancelButton);
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();

        // モーダル背景
        if (modal) {
            g.pushStyle();
            int bgColor = theme != null ? theme.colorBackground() : 0x000000;
            g.fill((bgColor>>16)&0xFF, (bgColor>>8)&0xFF, bgColor&0xFF, 180);
            g.noStroke();
            g.rect(0, 0, g.width, g.height);
            g.popStyle();
        }

        // パネル描画
        super.draw(g);

        // タイトルとメッセージ
        g.pushStyle();

        // テーマテキスト色
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFF000000;
        int onSurfaceSec = theme != null ? theme.colorOnSurfaceSecondary() : 0xFF333333;

        // タイトル
        g.fill(onSurface);
        g.textAlign(PApplet.CENTER, PApplet.TOP);
        g.textSize(18);
        g.text(title, x + width / 2, y + 15);

        // 区切り線
        int border = theme != null ? theme.colorBorder() : 0xFFCCCCCC; // TODO: Use constant for fallback
        g.stroke(border);
        g.strokeWeight(1);
        g.line(x + 10, y + 50, x + width - 10, y + 50);

        // メッセージ
        g.fill(onSurfaceSec);
        g.textAlign(PApplet.CENTER, PApplet.TOP);
        g.textSize(14);
        drawWrappedText(g, message, x + 20, y + 65, width - 40);

        g.popStyle();
    }

    private void drawWrappedText(PGraphics g, String text, float x, float y, float maxWidth) {
        String[] words = text.split(" ");
        StringBuilder line = new StringBuilder();
        float lineY = y;

        for (String word : words) {
            String testLine = line + (line.length() > 0 ? " " : "") + word;
            if (g.textWidth(testLine) > maxWidth && line.length() > 0) {
                g.text(line.toString(), x + maxWidth / 2, lineY);
                line = new StringBuilder(word);
                lineY += 20;
            } else {
                line.append((line.length() > 0 ? " " : "")).append(word);
            }
        }
        if (line.length() > 0) {
            g.text(line.toString(), x + maxWidth / 2, lineY);
        }
    }

    public void setOnOkListener(Runnable listener) {
        this.onOkListener = listener;
    }

    public void setOnCancelListener(Runnable listener) {
        this.onCancelListener = listener;
    }

    public void show() {
        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }
}
