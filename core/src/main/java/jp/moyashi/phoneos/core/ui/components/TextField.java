package jp.moyashi.phoneos.core.ui.components;

import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * 単一行テキスト入力フィールドコンポーネント。
 * カーソル、テキスト選択、コピー/ペースト対応。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class TextField extends BaseTextInput {

    /**
     * コンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     */
    public TextField(float x, float y, float width, float height) {
        super(x, y, width, height);
    }

    /**
     * プレースホルダー付きコンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     * @param placeholder プレースホルダーテキスト
     */
    public TextField(float x, float y, float width, float height, String placeholder) {
        super(x, y, width, height, placeholder);
    }

    @Override
    protected void drawText(PGraphics g) {
        float textX = x + 10;
        float textY = y + height / 2;

        if (font != null) {
            g.textFont(font);
        }

        g.textAlign(PApplet.LEFT, PApplet.CENTER);
        g.textSize(14);

        // テキストまたはプレースホルダーを表示
        if (text.isEmpty() && !placeholder.isEmpty() && !focused) {
            g.fill(placeholderColor);
            g.text(placeholder, textX, textY);
        } else {
            // 選択範囲のハイライト
            if (hasSelection()) {
                int start = Math.min(selectionStart, selectionEnd);
                int end = Math.max(selectionStart, selectionEnd);
                start = Math.max(0, Math.min(start, text.length()));
                end = Math.max(0, Math.min(end, text.length()));

                String beforeSelection = text.substring(0, start);
                String selection = text.substring(start, end);

                float beforeWidth = g.textWidth(beforeSelection);
                float selectionWidth = g.textWidth(selection);

                g.fill(100, 150, 255, 100);
                g.noStroke();
                g.rect(textX + beforeWidth, y + height / 2 - 10, selectionWidth, 20);
            }

            // テキスト描画
            g.fill(enabled ? textColor : 0xFF666666);
            g.text(text, textX, textY);

            // カーソル描画（フォーカス時、選択なし）
            if (focused && !hasSelection() && System.currentTimeMillis() % 1000 < 500) {
                String beforeCursor = text.substring(0, Math.min(cursorPosition, text.length()));
                float cursorX = textX + g.textWidth(beforeCursor);

                g.stroke(textColor);
                g.strokeWeight(2);
                g.line(cursorX, y + height / 2 - 10, cursorX, y + height / 2 + 10);
                g.noStroke();
            }
        }
    }

    @Override
    public boolean onKeyPressed(char key, int keyCode) {
        // 親クラスの共通処理を実行
        if (super.onKeyPressed(key, keyCode)) {
            return true;
        }

        // TextFieldに固有のキー処理

        // 左矢印
        if (keyCode == 37) {
            cursorPosition = Math.max(0, cursorPosition - 1);
            anchorPosition = cursorPosition; // アンカー位置を更新
            clearSelection();
            return true;
        }

        // 右矢印
        if (keyCode == 39) {
            cursorPosition = Math.min(text.length(), cursorPosition + 1);
            anchorPosition = cursorPosition; // アンカー位置を更新
            clearSelection();
            return true;
        }

        // Home
        if (keyCode == 36) {
            cursorPosition = 0;
            anchorPosition = cursorPosition; // アンカー位置を更新
            clearSelection();
            return true;
        }

        // End
        if (keyCode == 35) {
            cursorPosition = text.length();
            anchorPosition = cursorPosition; // アンカー位置を更新
            clearSelection();
            return true;
        }

        return false;
    }

    @Override
    protected int getCharPositionFromClick(int mouseX, int mouseY) {
        if (text.isEmpty()) return 0;

        float textX = x + 10;
        float clickOffset = mouseX - textX;

        // 各文字の位置を計算して、最も近い位置を返す
        for (int i = 0; i <= text.length(); i++) {
            String substring = text.substring(0, i);
            float width = getTextWidth(substring, lastGraphics);

            if (clickOffset < width) {
                // 前の文字との中間点で判定
                if (i > 0) {
                    String prevSubstring = text.substring(0, i - 1);
                    float prevWidth = getTextWidth(prevSubstring, lastGraphics);
                    float midPoint = (prevWidth + width) / 2;
                    return (clickOffset < midPoint) ? i - 1 : i;
                }
                return i;
            }
        }

        return text.length();
    }
}
