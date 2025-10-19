package jp.moyashi.phoneos.core.service.clipboard;

import jp.moyashi.phoneos.core.Kernel;
import processing.core.PImage;

/**
 * ClipboardManagerの実装クラス。
 * ClipboardProviderを使用して、環境に応じたクリップボードアクセスを提供する。
 */
public class ClipboardManagerImpl implements ClipboardManager {

    private final Kernel kernel;
    private ClipboardProvider provider;

    /**
     * ClipboardManagerを作成する。
     *
     * @param kernel Kernelインスタンス
     */
    public ClipboardManagerImpl(Kernel kernel) {
        this.kernel = kernel;
        // デフォルトはAWTクリップボードプロバイダー
        this.provider = new AWTClipboardProvider(kernel.getLogger());

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("ClipboardManager", "初期化: " + provider.getClass().getSimpleName() +
                " (利用可能: " + provider.isAvailable() + ")");
        }
    }

    /**
     * クリップボードプロバイダーを設定する。
     * Forge環境などで外部からプロバイダーを差し替えるために使用する。
     *
     * @param provider 新しいクリップボードプロバイダー
     */
    public void setProvider(ClipboardProvider provider) {
        if (provider != null) {
            this.provider = provider;
            if (kernel.getLogger() != null) {
                kernel.getLogger().info("ClipboardManager", "プロバイダーを変更: " + provider.getClass().getSimpleName() +
                    " (利用可能: " + provider.isAvailable() + ")");
            }
        }
    }

    /**
     * 現在のクリップボードプロバイダーを取得する。
     *
     * @return クリップボードプロバイダー
     */
    public ClipboardProvider getProvider() {
        return provider;
    }

    @Override
    public void copyText(String text) {
        copyText("text", text);
    }

    @Override
    public void copyText(String label, String text) {
        if (text == null) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("ClipboardManager", "nullテキストはコピーできません");
            }
            return;
        }

        if (provider.setText(text)) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().info("ClipboardManager", "テキストをクリップボードにコピー: " + label + ", 長さ: " + text.length());
            }
        } else {
            if (kernel.getLogger() != null) {
                kernel.getLogger().warn("ClipboardManager", "テキストのコピーに失敗");
            }
        }
    }

    @Override
    public String pasteText() {
        String text = provider.getText();

        if (text != null && kernel.getLogger() != null) {
            kernel.getLogger().info("ClipboardManager", "クリップボードからテキストをペースト: 長さ " + text.length());
        }

        return text;
    }

    @Override
    public void copyImage(PImage image) {
        copyImage("image", image);
    }

    @Override
    public void copyImage(String label, PImage image) {
        if (image == null) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("ClipboardManager", "null画像はコピーできません");
            }
            return;
        }

        if (provider.setImage(image)) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().info("ClipboardManager", "画像をクリップボードにコピー: " + label + ", サイズ: " + image.width + "x" + image.height);
            }
        } else {
            if (kernel.getLogger() != null) {
                kernel.getLogger().warn("ClipboardManager", "画像のコピーに失敗");
            }
        }
    }

    @Override
    public PImage pasteImage() {
        PImage image = provider.getImage();

        if (image != null && kernel.getLogger() != null) {
            kernel.getLogger().info("ClipboardManager", "クリップボードから画像をペースト: サイズ " + image.width + "x" + image.height);
        }

        return image;
    }

    @Override
    public void copyHtml(String label, String html) {
        if (html == null) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("ClipboardManager", "nullHTMLはコピーできません");
            }
            return;
        }

        // HTMLをテキストとしてコピー
        copyText(label, html);

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("ClipboardManager", "HTMLをテキストとしてクリップボードにコピー: " + label);
        }
    }

    @Override
    public boolean hasText() {
        return provider.hasText();
    }

    @Override
    public boolean hasImage() {
        return provider.hasImage();
    }

    @Override
    public boolean hasHtml() {
        // HTMLはテキストとして扱う
        return hasText();
    }

    @Override
    public void clear() {
        provider.clear();

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("ClipboardManager", "クリップボードをクリア");
        }
    }

    @Override
    public ClipData getPrimaryClip() {
        // クリップボードの内容をClipDataとして取得
        if (hasImage()) {
            PImage image = pasteImage();
            if (image != null) {
                return new ClipData("clipboard_image", image);
            }
        }

        if (hasText()) {
            String text = pasteText();
            if (text != null) {
                return new ClipData("clipboard_text", text);
            }
        }

        return null;
    }

    @Override
    public boolean hasPrimaryClip() {
        return hasText() || hasImage();
    }
}
