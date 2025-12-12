package jp.moyashi.phoneos.core.service;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

/**
 * システムクリップボード操作サービス（iOS UIPasteboard相当）
 * Headless環境（Forge等）では内部メモリベースのクリップボードを使用
 */
public class ClipboardService {

    private final Clipboard systemClipboard;
    private final boolean isHeadless;

    // Headless環境用の内部クリップボード
    private String internalClipboardText = null;

    public ClipboardService() {
        // Headless環境かどうかを判定
        this.isHeadless = GraphicsEnvironment.isHeadless();

        if (!isHeadless) {
            // システムクリップボードを使用
            Clipboard tempClipboard = null;
            try {
                tempClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            } catch (Exception e) {
                System.err.println("[ClipboardService] Failed to get system clipboard, using internal clipboard: " + e.getMessage());
            }
            this.systemClipboard = tempClipboard;
        } else {
            // Headless環境ではシステムクリップボードは使用しない
            this.systemClipboard = null;
            System.out.println("[ClipboardService] Running in headless mode, using internal clipboard");
        }
    }

    /**
     * テキストをクリップボードにコピー
     */
    public void copy(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        if (systemClipboard != null) {
            try {
                StringSelection selection = new StringSelection(text);
                systemClipboard.setContents(selection, null);
            } catch (Exception e) {
                // フォールバック: 内部クリップボードを使用
                internalClipboardText = text;
                System.err.println("[ClipboardService] System clipboard copy failed, using internal: " + e.getMessage());
            }
        } else {
            // 内部クリップボードを使用
            internalClipboardText = text;
        }
    }

    /**
     * クリップボードからテキストを取得
     */
    public String paste() {
        if (systemClipboard != null) {
            try {
                Transferable contents = systemClipboard.getContents(null);
                if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    return (String) contents.getTransferData(DataFlavor.stringFlavor);
                }
            } catch (Exception e) {
                System.err.println("[ClipboardService] System clipboard paste failed, using internal: " + e.getMessage());
            }
        }

        // フォールバック: 内部クリップボードから取得
        return internalClipboardText;
    }

    /**
     * クリップボードにテキストがあるかどうか
     */
    public boolean hasText() {
        if (systemClipboard != null) {
            try {
                Transferable contents = systemClipboard.getContents(null);
                if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    return true;
                }
            } catch (Exception e) {
                // フォールバック
            }
        }

        // 内部クリップボードを確認
        return internalClipboardText != null && !internalClipboardText.isEmpty();
    }

    /**
     * クリップボードをクリア
     */
    public void clear() {
        internalClipboardText = null;

        if (systemClipboard != null) {
            try {
                systemClipboard.setContents(new StringSelection(""), null);
            } catch (Exception e) {
                // 無視
            }
        }
    }

    /**
     * Headless環境かどうかを返す
     */
    public boolean isHeadless() {
        return isHeadless;
    }
}
