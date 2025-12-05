package jp.moyashi.phoneos.core.service.clipboard;

import processing.core.PImage;

/**
 * ClipboardManagerのシンプルな実装。
 * OS内部でテキストや画像をコピー・ペーストする機能を提供する。
 *
 * @since 2025-12-04
 * @version 1.0
 */
public class SimpleClipboardManager implements ClipboardManager {

    /** 現在のクリップボードデータ */
    private ClipData currentClip;

    /**
     * SimpleClipboardManagerを初期化する。
     */
    public SimpleClipboardManager() {
        this.currentClip = null;
    }

    @Override
    public void copyText(String text) {
        copyText("text", text);
    }

    @Override
    public void copyText(String label, String text) {
        if (text != null) {
            currentClip = new ClipData(label, text);
        }
    }

    @Override
    public String pasteText() {
        if (currentClip != null &&
            (currentClip.getType() == ClipData.Type.TEXT ||
             currentClip.getType() == ClipData.Type.HTML)) {
            return currentClip.getText();
        }
        return null;
    }

    @Override
    public void copyImage(PImage image) {
        copyImage("image", image);
    }

    @Override
    public void copyImage(String label, PImage image) {
        if (image != null) {
            currentClip = new ClipData(label, image);
        }
    }

    @Override
    public PImage pasteImage() {
        if (currentClip != null && currentClip.getType() == ClipData.Type.IMAGE) {
            return currentClip.getImage();
        }
        return null;
    }

    @Override
    public void copyHtml(String label, String html) {
        if (html != null) {
            currentClip = new ClipData(label, html, "text/html");
        }
    }

    @Override
    public boolean hasText() {
        return currentClip != null &&
               (currentClip.getType() == ClipData.Type.TEXT ||
                currentClip.getType() == ClipData.Type.HTML);
    }

    @Override
    public boolean hasImage() {
        return currentClip != null && currentClip.getType() == ClipData.Type.IMAGE;
    }

    @Override
    public boolean hasHtml() {
        return currentClip != null && currentClip.getType() == ClipData.Type.HTML;
    }

    @Override
    public void clear() {
        currentClip = null;
    }

    @Override
    public ClipData getPrimaryClip() {
        return currentClip;
    }

    @Override
    public boolean hasPrimaryClip() {
        return currentClip != null && !currentClip.isEmpty();
    }
}