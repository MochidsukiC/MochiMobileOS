package jp.moyashi.phoneos.core.ui.components;

import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * 複数行テキスト入力エリアコンポーネント。
 * スクロール、カーソル、テキスト選択に対応。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class TextArea extends BaseTextInput implements Scrollable {

    // スクロール
    private int scrollOffset = 0;
    private float lineHeight = 20;

    // 自動折り返し
    private boolean wordWrap = true; // デフォルトで有効
    private float wrapWidth = 0; // 折り返し幅（自動計算）

    /**
     * コンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     */
    public TextArea(float x, float y, float width, float height) {
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
    public TextArea(float x, float y, float width, float height, String placeholder) {
        super(x, y, width, height, placeholder);
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        // PGraphics参照を保存（テキスト幅計算用）
        this.lastGraphics = g;

        // 折り返し幅を計算（パディングを考慮）
        wrapWidth = width - 20 - 10; // 左右パディング10px、スクロールバー幅10px

        g.pushStyle();

        // 背景色
        int bgColor = focused ? focusedBackgroundColor : backgroundColor;
        if (!enabled) bgColor = 0xFFF0F0F0;

        // 背景描画
        g.fill(bgColor);
        g.noStroke();
        if (cornerRadius > 0) {
            g.rect(x, y, width, height, cornerRadius);
        } else {
            g.rect(x, y, width, height);
        }

        // 枠線描画
        int currentBorderColor = focused ? focusedBorderColor : borderColor;
        g.noFill();
        g.stroke(currentBorderColor);
        g.strokeWeight(focused ? 2 : 1);
        if (cornerRadius > 0) {
            g.rect(x, y, width, height, cornerRadius);
        } else {
            g.rect(x, y, width, height);
        }

        // テキスト描画（クリッピング）
        g.pushMatrix();
        g.translate(0, -scrollOffset);
        drawText(g);
        g.popMatrix();

        // スクロールバー描画
        if (isScrollBarVisible()) {
            drawScrollBar(g);
        }

        g.popStyle();
    }

    @Override
    protected void drawText(PGraphics g) {
        float textX = x + 10;
        float textY = y + 10;

        if (font != null) {
            g.textFont(font);
        }

        g.textAlign(PApplet.LEFT, PApplet.TOP);
        g.textSize(14);

        // テキストまたはプレースホルダーを表示
        if (text.isEmpty() && !placeholder.isEmpty() && !focused) {
            g.fill(placeholderColor);
            g.text(placeholder, textX, textY);
        } else {
            // 選択範囲のハイライト（複数行対応）
            if (hasSelection()) {
                drawSelectionHighlight(g, textX, textY);
            }

            // 複数行表示（折り返し対応）
            String[] lines = text.split("\n", -1);
            float lineY = textY;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];

                if (wordWrap) {
                    // 折り返しあり：行を折り返して描画
                    java.util.List<String> wrappedLines = wrapLine(line, g);
                    for (String wrappedLine : wrappedLines) {
                        // 表示範囲外はスキップ
                        if (lineY + scrollOffset < y - lineHeight || lineY + scrollOffset > y + height) {
                            lineY += lineHeight;
                            continue;
                        }

                        g.fill(enabled ? textColor : 0xFF666666);
                        g.text(wrappedLine, textX, lineY);

                        lineY += lineHeight;
                    }
                } else {
                    // 折り返しなし：そのまま描画
                    // 表示範囲外はスキップ
                    if (lineY + scrollOffset < y - lineHeight || lineY + scrollOffset > y + height) {
                        lineY += lineHeight;
                        continue;
                    }

                    g.fill(enabled ? textColor : 0xFF666666);
                    g.text(line, textX, lineY);

                    lineY += lineHeight;
                }
            }

            // カーソル描画（フォーカス時、選択なし）
            if (focused && !hasSelection() && System.currentTimeMillis() % 1000 < 500) {
                drawCursor(g, textX, textY);
            }
        }
    }

    private void drawSelectionHighlight(PGraphics g, float textX, float textY) {
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        start = Math.max(0, Math.min(start, text.length()));
        end = Math.max(0, Math.min(end, text.length()));

        g.fill(100, 150, 255, 100);
        g.noStroke();

        if (wordWrap && lastGraphics != null) {
            // 折り返しを考慮した選択範囲ハイライト
            String[] lines = text.split("\n", -1);
            int charPos = 0; // テキスト全体での現在の文字位置
            int displayLine = 0; // 表示行インデックス

            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String line = lines[lineIndex];
                int lineStart = charPos;
                int lineEnd = charPos + line.length();

                // この行が選択範囲と交差するか確認
                if (lineEnd < start) {
                    // この行は選択範囲より前
                    java.util.List<String> wrappedLines = wrapLine(line, g);
                    displayLine += wrappedLines.size();
                    charPos = lineEnd + 1; // +1は改行
                    continue;
                }

                if (lineStart > end) {
                    // この行は選択範囲より後
                    break;
                }

                // この行を折り返して処理
                java.util.List<String> wrappedLines = wrapLine(line, g);
                int wrapCharPos = lineStart;

                for (int wrapIndex = 0; wrapIndex < wrappedLines.size(); wrapIndex++) {
                    String wrappedLine = wrappedLines.get(wrapIndex);
                    int wrapStart = wrapCharPos;
                    int wrapEnd = wrapCharPos + wrappedLine.length();

                    // この折り返し行が選択範囲と交差するか確認
                    if (wrapEnd >= start && wrapStart <= end) {
                        // 選択範囲内の部分を計算
                        int highlightStart = Math.max(0, start - wrapStart);
                        int highlightEnd = Math.min(wrappedLine.length(), end - wrapStart);

                        String beforeHighlight = wrappedLine.substring(0, highlightStart);
                        String highlight = wrappedLine.substring(highlightStart, highlightEnd);

                        float highlightX = textX + g.textWidth(beforeHighlight);
                        float highlightWidth = g.textWidth(highlight);
                        float highlightY = textY + displayLine * lineHeight;

                        // 表示範囲内のみ描画
                        if (highlightY + scrollOffset >= y - lineHeight && highlightY + scrollOffset <= y + height) {
                            g.rect(highlightX, highlightY, highlightWidth, lineHeight - 2);
                        }
                    }

                    wrapCharPos = wrapEnd;
                    displayLine++;
                }

                charPos = lineEnd + 1; // +1は改行
            }

        } else {
            // 折り返しなしの場合（従来のロジック）
            String beforeSelection = text.substring(0, start);
            String selection = text.substring(start, end);

            // 選択範囲の開始行と終了行を計算
            String[] beforeLines = beforeSelection.split("\n", -1);
            int startLine = beforeLines.length - 1;
            String startLineText = beforeLines[startLine];

            String[] selectionLines = selection.split("\n", -1);

            // 複数行の選択範囲をハイライト
            float currentY = textY + startLine * lineHeight;
            for (int i = 0; i < selectionLines.length; i++) {
                // 表示範囲外はスキップ
                if (currentY + scrollOffset < y - lineHeight || currentY + scrollOffset > y + height) {
                    currentY += lineHeight;
                    continue;
                }

                float startX = textX;
                if (i == 0) {
                    // 最初の行：選択開始位置から
                    startX += getTextWidth(startLineText, g);
                }

                float highlightWidth = getTextWidth(selectionLines[i], g);
                g.rect(startX, currentY, highlightWidth, lineHeight - 2);

                currentY += lineHeight;
            }
        }
    }

    private void drawCursor(PGraphics g, float textX, float textY) {
        String beforeCursor = text.substring(0, Math.min(cursorPosition, text.length()));
        String[] lines = beforeCursor.split("\n", -1);
        int cursorLine = lines.length - 1;
        String currentLine = lines[cursorLine];

        float cursorX = textX;
        float cursorY = textY;

        if (wordWrap) {
            // 折り返しを考慮してカーソル位置を計算
            int displayLine = 0;

            // カーソルより前の行を処理
            for (int i = 0; i < cursorLine; i++) {
                java.util.List<String> wrappedLines = wrapLine(lines[i], g);
                displayLine += wrappedLines.size();
            }

            // カーソルのある行を折り返して、カーソルがどの表示行にあるか計算
            java.util.List<String> wrappedCurrentLine = wrapLine(currentLine, g);
            int charCount = 0;
            for (int i = 0; i < wrappedCurrentLine.size(); i++) {
                String wrappedLine = wrappedCurrentLine.get(i);
                if (charCount + wrappedLine.length() >= currentLine.length()) {
                    // カーソルはこの表示行にある
                    String lineBeforeCursor = currentLine.substring(charCount);
                    cursorX = textX + g.textWidth(lineBeforeCursor);
                    cursorY = textY + displayLine * lineHeight;
                    break;
                }
                charCount += wrappedLine.length();
                displayLine++;
            }
        } else {
            // 折り返しなしの場合
            cursorX = textX + g.textWidth(currentLine);
            cursorY = textY + cursorLine * lineHeight;
        }

        g.stroke(textColor);
        g.strokeWeight(2);
        g.line(cursorX, cursorY, cursorX, cursorY + lineHeight - 5);
        g.noStroke();
    }

    private void drawScrollBar(PGraphics g) {
        float scrollBarWidth = 8;
        float scrollBarX = x + width - scrollBarWidth - 2;
        float scrollBarHeight = height - 4;
        float contentHeight = getContentHeight();
        float thumbHeight = Math.max(20, scrollBarHeight * (height / contentHeight));
        float thumbY = y + 2 + (scrollBarHeight - thumbHeight) * (scrollOffset / (float)getMaxScrollOffset());

        // スクロールバー背景
        g.fill(0xFFE0E0E0);
        g.noStroke();
        g.rect(scrollBarX, y + 2, scrollBarWidth, scrollBarHeight, 4);

        // スクロールバーつまみ
        g.fill(0xFFAAAAAA);
        g.rect(scrollBarX, thumbY, scrollBarWidth, thumbHeight, 4);
    }

    private float getContentHeight() {
        if (wordWrap && lastGraphics != null) {
            // 折り返しを考慮した行数を計算
            String[] lines = text.split("\n", -1);
            int wrappedLineCount = 0;
            for (String line : lines) {
                wrappedLineCount += wrapLine(line, lastGraphics).size();
            }
            return wrappedLineCount * lineHeight + 20;
        } else {
            // 折り返しなしの場合
            String[] lines = text.split("\n", -1);
            return lines.length * lineHeight + 20;
        }
    }

    /**
     * 1行のテキストを幅に合わせて折り返す。
     *
     * @param line 折り返す行
     * @param g PGraphics
     * @return 折り返された行のリスト
     */
    private java.util.List<String> wrapLine(String line, PGraphics g) {
        java.util.List<String> wrappedLines = new java.util.ArrayList<>();
        if (line.isEmpty()) {
            wrappedLines.add("");
            return wrappedLines;
        }

        if (!wordWrap || wrapWidth <= 0) {
            wrappedLines.add(line);
            return wrappedLines;
        }

        // フォント設定を適用
        g.pushStyle();
        if (font != null) {
            g.textFont(font);
        }
        g.textSize(14);

        String remaining = line;
        while (!remaining.isEmpty()) {
            // 現在の幅に収まる最大の文字数を探す
            int breakPoint = remaining.length();
            for (int i = 1; i <= remaining.length(); i++) {
                String substring = remaining.substring(0, i);
                float width = g.textWidth(substring);
                if (width > wrapWidth) {
                    breakPoint = i - 1;
                    break;
                }
            }

            // 最低1文字は進める（無限ループ防止）
            if (breakPoint <= 0) {
                breakPoint = 1;
            }

            wrappedLines.add(remaining.substring(0, breakPoint));
            remaining = remaining.substring(breakPoint);
        }

        g.popStyle();
        return wrappedLines;
    }

    public boolean isScrollBarVisible() {
        return getContentHeight() > height;
    }

    @Override
    public boolean onKeyPressed(char key, int keyCode) {
        // 親クラスの共通処理を実行
        if (super.onKeyPressed(key, keyCode)) {
            return true;
        }

        // TextAreaに固有のキー処理

        // Enter
        if (keyCode == 10) {
            if (hasSelection()) {
                deleteSelection();
            }
            text = text.substring(0, cursorPosition) + "\n" + text.substring(cursorPosition);
            cursorPosition++;
            anchorPosition = cursorPosition; // アンカー位置を更新
            return true;
        }

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

        // 上矢印
        if (keyCode == 38) {
            moveCursorUp();
            anchorPosition = cursorPosition; // アンカー位置を更新
            clearSelection();
            return true;
        }

        // 下矢印
        if (keyCode == 40) {
            moveCursorDown();
            anchorPosition = cursorPosition; // アンカー位置を更新
            clearSelection();
            return true;
        }

        return false;
    }

    private void moveCursorUp() {
        String beforeCursor = text.substring(0, cursorPosition);
        int lastNewline = beforeCursor.lastIndexOf('\n');
        if (lastNewline > 0) {
            int prevNewline = beforeCursor.lastIndexOf('\n', lastNewline - 1);
            int columnPos = cursorPosition - lastNewline - 1;
            int targetPos = prevNewline + 1 + Math.min(columnPos, lastNewline - prevNewline - 1);
            cursorPosition = targetPos;
        }
    }

    private void moveCursorDown() {
        int nextNewline = text.indexOf('\n', cursorPosition);
        if (nextNewline >= 0) {
            String beforeCursor = text.substring(0, cursorPosition);
            int lastNewline = beforeCursor.lastIndexOf('\n');
            int columnPos = cursorPosition - lastNewline - 1;

            int nextNextNewline = text.indexOf('\n', nextNewline + 1);
            int lineLength = (nextNextNewline >= 0) ? (nextNextNewline - nextNewline - 1) : (text.length() - nextNewline - 1);
            int targetPos = nextNewline + 1 + Math.min(columnPos, lineLength);
            cursorPosition = targetPos;
        }
    }

    @Override
    protected int getCharPositionFromClick(int mouseX, int mouseY) {
        if (text.isEmpty()) return 0;

        float textX = x + 10;
        float textY = y + 10;

        // スクロールを考慮したY座標
        float adjustedY = mouseY + scrollOffset - textY;
        int clickedDisplayLine = Math.max(0, (int)(adjustedY / lineHeight));

        String[] lines = text.split("\n", -1);

        if (wordWrap && lastGraphics != null) {
            // 折り返しを考慮したクリック位置計算
            int displayLine = 0;
            int positionBeforeLine = 0;

            for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
                String line = lines[lineIndex];
                java.util.List<String> wrappedLines = wrapLine(line, lastGraphics);

                // この行の折り返された各表示行をチェック
                int charOffset = 0;
                for (int wrapIndex = 0; wrapIndex < wrappedLines.size(); wrapIndex++) {
                    if (displayLine == clickedDisplayLine) {
                        // この表示行がクリックされた
                        String wrappedLine = wrappedLines.get(wrapIndex);
                        float clickOffset = mouseX - textX;

                        for (int i = 0; i <= wrappedLine.length(); i++) {
                            String substring = wrappedLine.substring(0, i);
                            float width = getTextWidth(substring, lastGraphics);

                            if (clickOffset < width) {
                                // 前の文字との中間点で判定
                                if (i > 0) {
                                    String prevSubstring = wrappedLine.substring(0, i - 1);
                                    float prevWidth = getTextWidth(prevSubstring, lastGraphics);
                                    float midPoint = (prevWidth + width) / 2;
                                    int offset = (clickOffset < midPoint) ? i - 1 : i;
                                    return positionBeforeLine + charOffset + offset;
                                }
                                return positionBeforeLine + charOffset + i;
                            }
                        }

                        // 行末
                        return positionBeforeLine + charOffset + wrappedLine.length();
                    }

                    charOffset += wrappedLines.get(wrapIndex).length();
                    displayLine++;
                }

                positionBeforeLine += line.length() + 1; // +1は改行
            }

            // 最終行を超えた場合
            return text.length();

        } else {
            // 折り返しなしの場合
            if (clickedDisplayLine >= lines.length) {
                return text.length();
            }

            // クリックした行までの文字数を計算
            int positionBeforeLine = 0;
            for (int i = 0; i < clickedDisplayLine; i++) {
                positionBeforeLine += lines[i].length() + 1; // +1は改行
            }

            // 行内の位置を計算
            String currentLine = lines[clickedDisplayLine];
            float clickOffset = mouseX - textX;

            for (int i = 0; i <= currentLine.length(); i++) {
                String substring = currentLine.substring(0, i);
                float width = getTextWidth(substring, lastGraphics);

                if (clickOffset < width) {
                    // 前の文字との中間点で判定
                    if (i > 0) {
                        String prevSubstring = currentLine.substring(0, i - 1);
                        float prevWidth = getTextWidth(prevSubstring, lastGraphics);
                        float midPoint = (prevWidth + width) / 2;
                        return positionBeforeLine + ((clickOffset < midPoint) ? i - 1 : i);
                    }
                    return positionBeforeLine + i;
                }
            }

            return positionBeforeLine + currentLine.length();
        }
    }

    // Scrollable インターフェースの実装

    @Override
    public int getScrollOffset() {
        return scrollOffset;
    }

    @Override
    public void setScrollOffset(int offset) {
        this.scrollOffset = Math.max(0, Math.min(offset, getMaxScrollOffset()));
    }

    @Override
    public int getMaxScrollOffset() {
        return (int)Math.max(0, getContentHeight() - height);
    }

    @Override
    public void scroll(int delta) {
        setScrollOffset(scrollOffset + delta);
    }

    // 折り返し設定

    /**
     * 自動折り返しを有効/無効にする。
     *
     * @param wordWrap trueで有効、falseで無効
     */
    public void setWordWrap(boolean wordWrap) {
        this.wordWrap = wordWrap;
    }

    /**
     * 自動折り返しが有効かどうかを取得。
     *
     * @return trueで有効、falseで無効
     */
    public boolean isWordWrap() {
        return wordWrap;
    }
}
