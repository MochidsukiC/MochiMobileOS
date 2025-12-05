package jp.moyashi.phoneos.core.input;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.clipboard.ClipboardManager;
import jp.moyashi.phoneos.core.ui.ScreenManager;
import jp.moyashi.phoneos.core.ui.components.TextInputProtocol;
import jp.moyashi.phoneos.core.ui.Screen;

/**
 * ショートカットキー処理を専門に扱うクラス。
 * Ctrl+C/V/X/A、バックスペースなどのシステム共通ショートカットを処理。
 * iOS/Android方式の統一テキスト入力アーキテクチャに準拠。
 *
 * @since 2025-12-02
 * @version 1.0
 */
public class ShortcutKeyProcessor {

    /** 処理を委譲するKernelインスタンス */
    private final Kernel kernel;

    /**
     * ShortcutKeyProcessorを初期化する。
     *
     * @param kernel Kernelインスタンス
     */
    public ShortcutKeyProcessor(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * キー押下イベントを処理し、ショートカットキーの場合は対応する処理を実行する。
     *
     * @param key 押されたキー文字
     * @param keyCode キーコード
     * @param modifierState 修飾キーの状態
     * @return ショートカットが処理された場合true
     */
    public boolean processKeyPressed(char key, int keyCode, InputManager.ModifierKeyState modifierState) {
        // バックスペース処理（修飾キーなし）
        if (keyCode == 8 && !modifierState.isAnyModifierPressed()) {
            return handleBackspace();
        }

        // Ctrl/Cmdショートカット処理
        if ((modifierState.isCtrlPressed() || modifierState.isMetaPressed()) &&
            !modifierState.isAltPressed()) {
            return handleControlShortcut(key, keyCode, modifierState);
        }

        return false;
    }

    /**
     * バックスペースキーを処理する。
     * フォーカスされたTextInputProtocolに委譲。
     *
     * @return 処理された場合true
     */
    private boolean handleBackspace() {
        ScreenManager screenManager = kernel.getScreenManager();
        if (screenManager == null) {
            return false;
        }

        Screen currentScreen = screenManager.getCurrentScreen();
        if (currentScreen == null) {
            return false;
        }

        TextInputProtocol focusedInput = currentScreen.getFocusedTextInput();
        if (focusedInput != null) {
            focusedInput.deleteBackward();
            return true;
        }

        return false;
    }

    /**
     * Ctrl/Cmdショートカットを処理する。
     * Ctrl+C（コピー）、Ctrl+V（ペースト）、Ctrl+X（カット）、Ctrl+A（全選択）に対応。
     *
     * @param key 押されたキー文字
     * @param keyCode キーコード
     * @param modifierState 修飾キーの状態
     * @return ショートカットが処理された場合true
     */
    private boolean handleControlShortcut(char key, int keyCode, InputManager.ModifierKeyState modifierState) {
        // Ctrl/Cmd以外の修飾キーが押されている場合は処理しない
        if (modifierState.isShiftPressed() && key != 'A' && key != 'a') {
            return false; // Ctrl+Shift+C などは許可しない（Ctrl+Shift+Aは許可）
        }

        ScreenManager screenManager = kernel.getScreenManager();
        if (screenManager == null) {
            return false;
        }

        Screen currentScreen = screenManager.getCurrentScreen();
        if (currentScreen == null) {
            return false;
        }

        TextInputProtocol focusedInput = currentScreen.getFocusedTextInput();
        ClipboardManager clipboardManager = kernel.getClipboardManager();

        // キーコードまたは文字で判定（大文字小文字を考慮）
        char upperKey = Character.toUpperCase(key);

        switch (upperKey) {
            case 'C': // Copy
                if (keyCode == 67 || key == 'c' || key == 'C') {
                    return handleCopy(focusedInput, clipboardManager);
                }
                break;

            case 'V': // Paste
                if (keyCode == 86 || key == 'v' || key == 'V') {
                    return handlePaste(focusedInput, clipboardManager);
                }
                break;

            case 'X': // Cut
                if (keyCode == 88 || key == 'x' || key == 'X') {
                    return handleCut(focusedInput, clipboardManager);
                }
                break;

            case 'A': // Select All
                if (keyCode == 65 || key == 'a' || key == 'A') {
                    return handleSelectAll(focusedInput);
                }
                break;
        }

        return false;
    }

    /**
     * コピー処理を実行する（Ctrl+C）。
     *
     * @param focusedInput フォーカスされた入力フィールド
     * @param clipboardManager クリップボードマネージャー
     * @return 処理された場合true
     */
    private boolean handleCopy(TextInputProtocol focusedInput, ClipboardManager clipboardManager) {
        if (focusedInput == null || clipboardManager == null) {
            return false;
        }

        String selectedText = focusedInput.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            clipboardManager.copyText(selectedText);
            System.out.println("ShortcutKeyProcessor: Copied text to clipboard: " +
                              (selectedText.length() > 20 ? selectedText.substring(0, 20) + "..." : selectedText));
            return true;
        }

        return false;
    }

    /**
     * ペースト処理を実行する（Ctrl+V）。
     *
     * @param focusedInput フォーカスされた入力フィールド
     * @param clipboardManager クリップボードマネージャー
     * @return 処理された場合true
     */
    private boolean handlePaste(TextInputProtocol focusedInput, ClipboardManager clipboardManager) {
        if (focusedInput == null || clipboardManager == null) {
            return false;
        }

        String clipboardText = clipboardManager.pasteText();
        if (clipboardText != null && !clipboardText.isEmpty()) {
            // 選択範囲がある場合は置換、ない場合はカーソル位置に挿入
            if (focusedInput.hasSelection()) {
                focusedInput.replaceSelection(clipboardText);
            } else {
                focusedInput.insertTextAtCursor(clipboardText);
            }
            System.out.println("ShortcutKeyProcessor: Pasted text from clipboard: " +
                              (clipboardText.length() > 20 ? clipboardText.substring(0, 20) + "..." : clipboardText));
            return true;
        }

        return false;
    }

    /**
     * カット処理を実行する（Ctrl+X）。
     *
     * @param focusedInput フォーカスされた入力フィールド
     * @param clipboardManager クリップボードマネージャー
     * @return 処理された場合true
     */
    private boolean handleCut(TextInputProtocol focusedInput, ClipboardManager clipboardManager) {
        if (focusedInput == null || clipboardManager == null) {
            return false;
        }

        String selectedText = focusedInput.getSelectedText();
        if (selectedText != null && !selectedText.isEmpty()) {
            // クリップボードにコピー
            clipboardManager.copyText(selectedText);
            // 選択範囲を削除
            focusedInput.deleteSelection();
            System.out.println("ShortcutKeyProcessor: Cut text to clipboard: " +
                              (selectedText.length() > 20 ? selectedText.substring(0, 20) + "..." : selectedText));
            return true;
        }

        return false;
    }

    /**
     * 全選択処理を実行する（Ctrl+A）。
     *
     * @param focusedInput フォーカスされた入力フィールド
     * @return 処理された場合true
     */
    private boolean handleSelectAll(TextInputProtocol focusedInput) {
        if (focusedInput == null) {
            return false;
        }

        focusedInput.selectAll();
        System.out.println("ShortcutKeyProcessor: Selected all text");
        return true;
    }
}