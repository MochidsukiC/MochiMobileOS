package jp.moyashi.phoneos.core.service;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;

/**
 * システムクリップボード操作サービス（iOS UIPasteboard相当）
 */
public class ClipboardService {

    private final Clipboard clipboard;

    public ClipboardService() {
        this.clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    }

    /**
     * テキストをクリップボードにコピー
     */
    public void copy(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        StringSelection selection = new StringSelection(text);
        clipboard.setContents(selection, null);
    }

    /**
     * クリップボードからテキストを取得
     */
    public String paste() {
        try {
            Transferable contents = clipboard.getContents(null);
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (Exception e) {
            System.err.println("[ClipboardService] paste error: " + e.getMessage());
        }
        return null;
    }

    /**
     * クリップボードにテキストがあるかどうか
     */
    public boolean hasText() {
        try {
            Transferable contents = clipboard.getContents(null);
            return contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * クリップボードをクリア
     */
    public void clear() {
        clipboard.setContents(new StringSelection(""), null);
    }
}
