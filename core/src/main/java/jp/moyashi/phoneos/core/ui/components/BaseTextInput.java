package jp.moyashi.phoneos.core.ui.components;

import jp.moyashi.phoneos.core.service.LoggerService;
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
public abstract class BaseTextInput extends BaseComponent implements Focusable, Clickable {

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

    // 修飾キー状態
    protected boolean shiftPressed = false;
    protected boolean ctrlPressed = false;

    // ロガー
    protected LoggerService logger;

    // テキスト幅計算用のPGraphics参照（draw時に更新）
    protected PGraphics lastGraphics;

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
        this.backgroundColor = 0xFFFFFFFF;
        this.focusedBackgroundColor = 0xFFFFFFC8;
        this.textColor = 0xFF000000;
        this.placeholderColor = 0xFF999999;
        this.borderColor = 0xFFCCCCCC;
        this.focusedBorderColor = 0xFF4A90E2;
        this.cornerRadius = 5;
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

        // Ctrl+C: コピー（keyCodeで判定: C=67）
        if (ctrlPressed && keyCode == 67) {
            // コピー処理は外部で実装（ClipboardManager経由）
            return true;
        }

        // Ctrl+V: ペースト（keyCodeで判定: V=86）
        if (ctrlPressed && keyCode == 86) {
            // ペースト処理は外部で実装（ClipboardManager経由）
            return true;
        }

        // Ctrl+A: 全選択（keyCodeで判定: A=65）
        if (ctrlPressed && keyCode == 65) {
            selectionStart = 0;
            selectionEnd = text.length();
            cursorPosition = text.length();
            anchorPosition = cursorPosition; // アンカー位置を更新
            return true;
        }

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
     */
    protected float getTextWidth(String str, PGraphics g) {
        if (g != null) {
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

    protected boolean hasSelection() {
        return selectionStart >= 0 && selectionEnd >= 0 && selectionStart != selectionEnd;
    }

    protected void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
        // anchorPositionはクリアしない（シフトクリック範囲選択のため）
    }

    protected void deleteSelection() {
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

    // ===== Getter/Setter =====

    public String getText() {
        return text;
    }

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
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
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
