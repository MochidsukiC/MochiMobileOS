package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.ui.components.TextInputProtocol;

/**
 * Chromium HTMLフィールド用TextInputProtocol実装
 * JavaScript Bridge経由でDOM内のテキストフィールドを操作する
 *
 * 技術的制約: JCEFのexecuteJavaScript()は非同期のみ
 * 解決策: 状態キャッシュ方式（ChromiumSurfaceで選択テキストをキャッシュ）
 */
public class ChromiumTextInput implements TextInputProtocol {

    private final ChromiumSurface surface;

    public ChromiumTextInput(ChromiumSurface surface) {
        this.surface = surface;
    }

    // ========== テキストアクセス ==========

    @Override
    public String getText() {
        // HTMLフィールドの全テキスト取得は複雑なため、選択テキストのみサポート
        return surface.getCachedSelectedText();
    }

    @Override
    public void setText(String text) {
        // 全テキスト置換はサポートしない（複雑すぎる）
    }

    // ========== 選択操作 ==========

    @Override
    public String getSelectedText() {
        return surface.getCachedSelectedText();
    }

    @Override
    public boolean hasSelection() {
        String selected = surface.getCachedSelectedText();
        return selected != null && !selected.isEmpty();
    }

    @Override
    public void selectAll() {
        String script =
            "(function() {" +
            "  var el = document.activeElement;" +
            "  if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {" +
            "    el.select();" +
            "  } else {" +
            "    document.execCommand('selectAll', false, null);" +
            "  }" +
            "})();";
        surface.executeScript(script);
    }

    @Override
    public void clearSelection() {
        String script =
            "(function() {" +
            "  var sel = window.getSelection();" +
            "  if (sel) sel.removeAllRanges();" +
            "})();";
        surface.executeScript(script);
    }

    // ========== 選択範囲 ==========

    @Override
    public int getSelectionStart() {
        // 非同期のため取得不可。-1を返す
        return -1;
    }

    @Override
    public int getSelectionEnd() {
        // 非同期のため取得不可。-1を返す
        return -1;
    }

    @Override
    public void setSelection(int start, int end) {
        String script = String.format(
            "(function() {" +
            "  var el = document.activeElement;" +
            "  if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {" +
            "    el.setSelectionRange(%d, %d);" +
            "  }" +
            "})();", start, end);
        surface.executeScript(script);
    }

    // ========== 編集操作 ==========

    @Override
    public void deleteSelection() {
        String script =
            "(function() {" +
            "  document.execCommand('delete', false, null);" +
            "})();";
        surface.executeScript(script);
    }

    @Override
    public void deleteBackward() {
        // バックスペース操作: 選択があれば削除、なければカーソル前の1文字を削除
        // JavaScriptでKeyboardEventをシミュレートしてバックスペースを実行
        String script =
            "(function() {" +
            "  var el = document.activeElement;" +
            "  if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {" +
            "    var start = el.selectionStart;" +
            "    var end = el.selectionEnd;" +
            "    if (start === end && start > 0) {" +
            "      el.value = el.value.substring(0, start - 1) + el.value.substring(end);" +
            "      el.selectionStart = el.selectionEnd = start - 1;" +
            "    } else if (start !== end) {" +
            "      el.value = el.value.substring(0, start) + el.value.substring(end);" +
            "      el.selectionStart = el.selectionEnd = start;" +
            "    }" +
            "    el.dispatchEvent(new Event('input', { bubbles: true }));" +
            "  } else {" +
            "    document.execCommand('delete', false, null);" +
            "  }" +
            "})();";
        surface.executeScript(script);
    }

    @Override
    public void insertTextAtCursor(String text) {
        replaceSelection(text);
    }

    @Override
    public void replaceSelection(String text) {
        // テキストをエスケープ
        String escapedText = escapeJsString(text);
        String script = String.format(
            "(function() {" +
            "  var el = document.activeElement;" +
            "  if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {" +
            "    var start = el.selectionStart;" +
            "    var end = el.selectionEnd;" +
            "    el.value = el.value.substring(0, start) + '%s' + el.value.substring(end);" +
            "    el.selectionStart = el.selectionEnd = start + %d;" +
            "    el.dispatchEvent(new Event('input', { bubbles: true }));" +
            "  } else {" +
            "    document.execCommand('insertText', false, '%s');" +
            "  }" +
            "})();", escapedText, text.length(), escapedText);
        surface.executeScript(script);
    }

    // ========== カーソル ==========

    @Override
    public int getCursorPosition() {
        // 非同期のため取得不可。-1を返す
        return -1;
    }

    @Override
    public void setCursorPosition(int position) {
        setSelection(position, position);
    }

    // ========== ユーティリティ ==========

    /**
     * JavaScript文字列をエスケープする
     */
    private String escapeJsString(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
