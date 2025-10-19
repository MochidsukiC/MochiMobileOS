package jp.moyashi.phoneos.core.service.clipboard;

import jp.moyashi.phoneos.core.service.LoggerService;
import processing.core.PImage;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * AWT Toolkitを使用したクリップボードプロバイダー。
 * Standalone環境で使用される。
 */
public class AWTClipboardProvider implements ClipboardProvider {

    private final LoggerService logger;
    private final boolean available;

    public AWTClipboardProvider(LoggerService logger) {
        this.logger = logger;

        // システムクリップボードが利用可能かチェック
        boolean temp = true;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard();
        } catch (HeadlessException | AWTError | SecurityException e) {
            temp = false;
            if (logger != null) {
                logger.warn("AWTClipboardProvider", "AWTクリップボードが利用できません: " + e.getMessage());
            }
        }
        this.available = temp;
    }

    private Clipboard getSystemClipboard() {
        if (!available) {
            return null;
        }
        try {
            return Toolkit.getDefaultToolkit().getSystemClipboard();
        } catch (HeadlessException | AWTError | SecurityException e) {
            if (logger != null) {
                logger.error("AWTClipboardProvider", "システムクリップボードの取得に失敗: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public boolean setText(String text) {
        if (text == null) {
            return false;
        }

        Clipboard clipboard = getSystemClipboard();
        if (clipboard == null) {
            return false;
        }

        try {
            StringSelection selection = new StringSelection(text);
            clipboard.setContents(selection, null);
            if (logger != null) {
                logger.debug("AWTClipboardProvider", "テキストをコピー: 長さ " + text.length());
            }
            return true;
        } catch (IllegalStateException e) {
            if (logger != null) {
                logger.error("AWTClipboardProvider", "テキストのコピーに失敗: " + e.getMessage());
            }
            return false;
        }
    }

    @Override
    public String getText() {
        Clipboard clipboard = getSystemClipboard();
        if (clipboard == null) {
            return null;
        }

        try {
            if (!clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                return null;
            }
            return (String) clipboard.getData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
            if (logger != null) {
                logger.error("AWTClipboardProvider", "テキストの取得に失敗: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public boolean hasText() {
        Clipboard clipboard = getSystemClipboard();
        if (clipboard == null) {
            return false;
        }

        try {
            return clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public boolean setImage(PImage image) {
        if (image == null) {
            return false;
        }

        Clipboard clipboard = getSystemClipboard();
        if (clipboard == null) {
            return false;
        }

        try {
            BufferedImage bufferedImage = pImageToBufferedImage(image);
            ImageSelection selection = new ImageSelection(bufferedImage);
            clipboard.setContents(selection, null);
            if (logger != null) {
                logger.debug("AWTClipboardProvider", "画像をコピー: " + image.width + "x" + image.height);
            }
            return true;
        } catch (IllegalStateException e) {
            if (logger != null) {
                logger.error("AWTClipboardProvider", "画像のコピーに失敗: " + e.getMessage());
            }
            return false;
        }
    }

    @Override
    public PImage getImage() {
        Clipboard clipboard = getSystemClipboard();
        if (clipboard == null) {
            return null;
        }

        try {
            if (!clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                return null;
            }

            Image image = (Image) clipboard.getData(DataFlavor.imageFlavor);

            BufferedImage bufferedImage;
            if (image instanceof BufferedImage) {
                bufferedImage = (BufferedImage) image;
            } else {
                bufferedImage = new BufferedImage(
                    image.getWidth(null),
                    image.getHeight(null),
                    BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g = bufferedImage.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
            }

            return bufferedImageToPImage(bufferedImage);
        } catch (UnsupportedFlavorException | IOException | IllegalStateException e) {
            if (logger != null) {
                logger.error("AWTClipboardProvider", "画像の取得に失敗: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public boolean hasImage() {
        Clipboard clipboard = getSystemClipboard();
        if (clipboard == null) {
            return false;
        }

        try {
            return clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor);
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Override
    public void clear() {
        Clipboard clipboard = getSystemClipboard();
        if (clipboard == null) {
            return;
        }

        try {
            clipboard.setContents(new StringSelection(""), null);
        } catch (IllegalStateException e) {
            if (logger != null) {
                logger.error("AWTClipboardProvider", "クリップボードのクリアに失敗: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    private BufferedImage pImageToBufferedImage(PImage pImage) {
        BufferedImage bufferedImage = new BufferedImage(
            pImage.width,
            pImage.height,
            BufferedImage.TYPE_INT_ARGB
        );
        pImage.loadPixels();
        bufferedImage.setRGB(0, 0, pImage.width, pImage.height, pImage.pixels, 0, pImage.width);
        return bufferedImage;
    }

    private PImage bufferedImageToPImage(BufferedImage bufferedImage) {
        PImage pImage = new PImage(bufferedImage.getWidth(), bufferedImage.getHeight(), PImage.ARGB);
        bufferedImage.getRGB(0, 0, pImage.width, pImage.height, pImage.pixels, 0, pImage.width);
        pImage.updatePixels();
        return pImage;
    }

    private static class ImageSelection implements Transferable {
        private final Image image;

        public ImageSelection(Image image) {
            this.image = image;
        }

        @Override
        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { DataFlavor.imageFlavor };
        }

        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return DataFlavor.imageFlavor.equals(flavor);
        }

        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (!DataFlavor.imageFlavor.equals(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return image;
        }
    }
}
