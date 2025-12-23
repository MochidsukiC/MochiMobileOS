package jp.moyashi.phoneos.core.ui.components;

import jp.moyashi.phoneos.core.render.TextRenderer;
import jp.moyashi.phoneos.core.render.TextRendererContext;
import jp.moyashi.phoneos.core.service.LoggerService;
import jp.moyashi.phoneos.core.util.EmojiUtil;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;

/**
 * テキスト入力コンポーネントの基底クラス。
 * TextField、TextAreaなどの共通機能を提供する。
 *
 * <p>このクラスは以下の機能を提供します：</p>
 * <ul>
 *   <li>テキスト編集（入力、削除、カーソル移動）</li>
 *   <li>テキスト選択（マウスドラッグ、シフトクリック）</li>
 *   <li>クリップボード連携（Ctrl+C/V/A）</li>
 *   <li>フォーカス管理</li>
 *   <li>スタイル設定（背景色、テキスト色、枠線など）</li>
 * </ul>
 *
 * <p>外部アプリケーション開発者は、このクラスを継承して独自のテキスト入力コンポーネントを作成できます。</p>
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public abstract class BaseTextInput extends BaseComponent implements Focusable, Clickable, TextInputProtocol {

    // テキストデータ
    protected String text;
    protected String placeholder;
    protected int cursorPosition;

    // テキスト選択
    protected int selectionStart = -1;
    protected int selectionEnd = -1;
    protected int anchorPosition = -1; // シフトクリック範囲選択のアンカー位置

    // スタイル
    protected int backgroundColor;
    protected int focusedBackgroundColor;
    protected int textColor;
    protected int placeholderColor;
    protected int borderColor;
    protected int focusedBorderColor;
    protected float cornerRadius;
    protected PFont font;

    // 状態
    protected boolean focused = false;
    protected boolean hovered = false;
    protected boolean pressed = false;
    protected boolean isDragging = false;

    protected boolean shiftPressed = false;
    protected boolean ctrlPressed = false;

    // ロガー
    protected LoggerService logger;

    // テキスト幅計算用のPGraphics参照（draw時に更新）
    protected PGraphics lastGraphics;
    
    // カスタム設定フラグ
    protected boolean isCustomBackgroundColor = false;
    protected boolean isCustomFocusedBackgroundColor = false;
    protected boolean isCustomTextColor = false;
    protected boolean isCustomPlaceholderColor = false;
    protected boolean isCustomBorderColor = false;
    protected boolean isCustomFocusedBorderColor = false;
    protected boolean isCustomCornerRadius = false;

    /**
     * コンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     */
    public BaseTextInput(float x, float y, float width, float height) {
        super(x, y, width, height);
        this.text = "";
        this.placeholder = "";
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            this.backgroundColor = theme.colorSurface();
            this.focusedBackgroundColor = theme.colorSurface();
            this.textColor = theme.colorOnSurface();
            this.placeholderColor = 0xFF999999;
            this.borderColor = theme.colorBorder();
            this.focusedBorderColor = theme.colorPrimary();
            this.cornerRadius = theme.radiusSm();
        } else {
            this.backgroundColor = 0xFFFFFFFF;
            this.focusedBackgroundColor = 0xFFFFFFC8;
            this.textColor = 0xFF000000;
            this.placeholderColor = 0xFF999999;
            this.borderColor = 0xFFCCCCCC;
            this.focusedBorderColor = 0xFF4A90E2;
            this.cornerRadius = 5;
        }
        this.cursorPosition = 0;
        this.font = null;
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
    public BaseTextInput(float x, float y, float width, float height, String placeholder) {
        this(x, y, width, height);
        this.placeholder = placeholder;
    }

    // ===== 抽象メソッド（サブクラスで実装） =====

    /**
     * マウス座標から文字位置を計算。
     * サブクラスで実装（単一行/複数行で異なる）。
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標（単一行の場合は未使用）
     * @return 文字位置（0〜text.length()）
     */
    protected abstract int getCharPositionFromClick(int mouseX, int mouseY);

    /**
     * テキストを描画。
     * サブクラスで実装（単一行/複数行で異なる）。
     *
     * @param g PGraphics
     */
    protected abstract void drawText(PGraphics g);

    // ===== 共通実装 =====

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        // PGraphics参照を保存（テキスト幅計算用）
        this.lastGraphics = g;

        g.pushStyle();

        // 動的切替に追従（カスタム設定優先）
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        if (theme != null) {
            if (!isCustomBorderColor) this.borderColor = theme.colorBorder();
            if (!isCustomFocusedBorderColor) this.focusedBorderColor = theme.colorPrimary();
            if (!isCustomBackgroundColor) this.backgroundColor = theme.colorSurface();
            if (!isCustomFocusedBackgroundColor) this.focusedBackgroundColor = theme.colorSurface();
            if (!isCustomTextColor) this.textColor = theme.colorOnSurface();
            if (!isCustomCornerRadius) this.cornerRadius = theme.radiusSm();
        }

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
        }
        else {
            g.rect(x, y, width, height);
        }

        // テキスト描画（サブクラスで実装）
        drawText(g);

        g.popStyle();
    }

    @Override
    public boolean isFocused() {
        return focused;
    }

    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
        if (focused) {
            onFocusGained();
        } else {
            onFocusLost();
            // フォーカスを失ったらアンカー位置もクリア
            anchorPosition = -1;
        }
    }

    @Override
    public boolean onKeyPressed(char key, int keyCode) {
        if (!focused || !enabled) return false;

        // Ctrl+C/V/X/A はOS側（Kernel）で統一管理されるため、ここでは処理しない
        // TextInputProtocolインターフェース経由でOS側が操作する

        // バックスペース
        if (keyCode == 8) {
            if (hasSelection()) {
                deleteSelection();
            } else if (cursorPosition > 0) {
                text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                cursorPosition--;
            }
            anchorPosition = cursorPosition; // アンカー位置を更新
            return true;
        }

        // Delete
        if (keyCode == 127) {
            if (hasSelection()) {
                deleteSelection();
            } else if (cursorPosition < text.length()) {
                text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
            }
            anchorPosition = cursorPosition; // アンカー位置を更新
            return true;
        }

        // 通常文字入力（全角対応）
        // 制御文字（0-31, 127）を除外、それ以外の文字を許可
        if (key >= 32 && key != 127 && key != 65535) {
            if (hasSelection()) {
                deleteSelection();
            }
            text = text.substring(0, cursorPosition) + key + text.substring(cursorPosition);
            cursorPosition++;
            anchorPosition = cursorPosition; // アンカー位置を更新
            return true;
        }

        return false;
    }

    @Override
    public boolean onMousePressed(int mouseX, int mouseY) {
        if (!enabled || !visible) return false;

        if (contains(mouseX, mouseY)) {
            pressed = true;
            focused = true;

            // マウス位置から文字位置を計算
            int charPos = getCharPositionFromClick(mouseX, mouseY);

            // シフトクリック：アンカー位置から新しい位置まで選択
            if (shiftPressed && anchorPosition >= 0) {
                selectionStart = Math.min(anchorPosition, charPos);
                selectionEnd = Math.max(anchorPosition, charPos);
                cursorPosition = charPos;
                isDragging = false; // シフトクリックではドラッグしない
            } else {
                // 通常クリック：アンカー位置を設定し、ドラッグ選択を開始
                cursorPosition = charPos;
                anchorPosition = charPos;
                selectionStart = charPos;
                selectionEnd = charPos;
                isDragging = true;
            }

            onFocusGained();
            return true;
        } else {
            // 外側をクリックした場合、フォーカスを外す
            if (focused) {
                focused = false;
                onFocusLost();
            }
        }

        return false;
    }

    @Override
    public boolean onMouseReleased(int mouseX, int mouseY) {
        if (!enabled || !visible) return false;

        pressed = false;
        isDragging = false;

        // 選択範囲が同じ位置なら選択解除
        if (selectionStart == selectionEnd) {
            clearSelection();
        }

        return contains(mouseX, mouseY);
    }

    @Override
    public void onMouseMoved(int mouseX, int mouseY) {
        if (!enabled || !visible) return;

        hovered = contains(mouseX, mouseY);
    }

    @Override
    public void setOnClickListener(Runnable listener) {
        // テキスト入力ではクリックリスナーは使用しない
    }

    /**
     * マウスドラッグ処理（テキスト選択）。
     */
    public void onMouseDragged(int mouseX, int mouseY) {
        if (!isDragging || !enabled || !visible) return;

        if (contains(mouseX, mouseY)) {
            int charPos = getCharPositionFromClick(mouseX, mouseY);
            selectionEnd = charPos;
            cursorPosition = charPos;
        }
    }

    @Override
    public boolean isHovered() {
        return hovered;
    }

    @Override
    public boolean isPressed() {
        return pressed;
    }

    // ===== ヘルパーメソッド =====

    /**
     * テキスト幅を取得（フォント考慮）。
     * 描画時と同じフォント設定で幅を計算するため、PGraphicsが必要。
     * draw()の外で呼ばれる場合は概算値を返す。
     * 絵文字を含む場合はTextRendererを使用して正確な幅を計算する。
     */
    protected float getTextWidth(String str, PGraphics g) {
        if (g != null) {
            // 絵文字が含まれる場合はTextRendererを使用
            TextRenderer textRenderer = TextRendererContext.getTextRenderer();
            if (textRenderer != null && EmojiUtil.containsEmoji(str)) {
                return textRenderer.getTextWidth(g, str, 14);
            }

            g.pushStyle();
            if (font != null) {
                g.textFont(font);
            }
            g.textSize(14);
            float width = g.textWidth(str);
            g.popStyle();
            return width;
        }
        // PGraphicsがない場合は概算
        return str.length() * 8;
    }

    // ===== TextInputProtocol実装 =====

    @Override
    public boolean hasSelection() {
        return selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd;
    }

    @Override
    public void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
        // anchorPositionはクリアしない（シフトクリック範囲選択のため）
    }

    @Override
    public void deleteSelection() {
        if (!hasSelection()) return;

        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        start = Math.max(0, Math.min(start, text.length()));
        end = Math.max(0, Math.min(end, text.length()));

        text = text.substring(0, start) + text.substring(end);
        cursorPosition = start;
        anchorPosition = cursorPosition; // アンカー位置を更新
        clearSelection();
    }

    @Override
    public void deleteBackward() {
        // バックスペース操作: 選択があれば削除、なければカーソル前の1文字を削除
        if (hasSelection()) {
            deleteSelection();
        } else if (cursorPosition > 0) {
            text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
            cursorPosition--;
            anchorPosition = cursorPosition;
        }
    }

    @Override
    public void selectAll() {
        selectionStart = 0;
        selectionEnd = text.length();
        cursorPosition = text.length();
        anchorPosition = cursorPosition;
    }

    @Override
    public int getSelectionStart() {
        return selectionStart;
    }

    @Override
    public int getSelectionEnd() {
        return selectionEnd;
    }

    @Override
    public void setSelection(int start, int end) {
        selectionStart = Math.max(0, Math.min(start, text.length()));
        selectionEnd = Math.max(0, Math.min(end, text.length()));
        cursorPosition = selectionEnd;
        anchorPosition = selectionStart;
    }

    @Override
    public void insertTextAtCursor(String insertText) {
        insertText(insertText);
    }

    @Override
    public void replaceSelection(String replacement) {
        if (hasSelection()) {
            deleteSelection();
        }
        insertText(replacement);
    }

    @Override
    public int getCursorPosition() {
        return cursorPosition;
    }

    @Override
    public void setCursorPosition(int position) {
        cursorPosition = Math.max(0, Math.min(position, text.length()));
        anchorPosition = cursorPosition;
        clearSelection();
    }

    // ===== Getter/Setter =====

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(String text) {
        this.text = text;
        cursorPosition = Math.min(cursorPosition, text.length());
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
        this.isCustomBackgroundColor = true;
    }
    
    public void setFocusedBackgroundColor(int color) {
        this.focusedBackgroundColor = color;
        this.isCustomFocusedBackgroundColor = true;
    }

    public void setTextColor(int color) {
        this.textColor = color;
        this.isCustomTextColor = true;
    }
    
    public void setPlaceholderColor(int color) {
        this.placeholderColor = color;
        this.isCustomPlaceholderColor = true;
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
        this.isCustomBorderColor = true;
    }
    
    public void setFocusedBorderColor(int color) {
        this.focusedBorderColor = color;
        this.isCustomFocusedBorderColor = true;
    }
    
    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
        this.isCustomCornerRadius = true;
    }

    public void setFont(PFont font) {
        this.font = font;
    }

    public void setLogger(LoggerService logger) {
        this.logger = logger;
    }

    /**
     * 選択されたテキストを取得。
     */
    @Override
    public String getSelectedText() {
        if (!hasSelection()) return "";

        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        start = Math.max(0, Math.min(start, text.length()));
        end = Math.max(0, Math.min(end, text.length()));

        return text.substring(start, end);
    }

    /**
     * カーソル位置にテキストを挿入。
     * 選択範囲がある場合は削除してから挿入する。
     *
     * @param insertText 挿入するテキスト
     */
    public void insertText(String insertText) {
        if (insertText == null || insertText.isEmpty()) return;

        // 選択範囲がある場合は削除
        if (hasSelection()) {
            deleteSelection();
        }

        // カーソル位置にテキストを挿入
        text = text.substring(0, cursorPosition) + insertText + text.substring(cursorPosition);
        cursorPosition += insertText.length();
        anchorPosition = cursorPosition; // アンカー位置を更新
    }

    /**
     * シフトキーの押下状態を設定。
     * マウスイベント前に呼び出す。
     */
    public void setShiftPressed(boolean shiftPressed) {
        this.shiftPressed = shiftPressed;
    }

    /**
     * Ctrlキーの押下状態を設定。
     * キーイベント処理で使用。
     */
    public void setCtrlPressed(boolean ctrlPressed) {
        this.ctrlPressed = ctrlPressed;
    }
}
