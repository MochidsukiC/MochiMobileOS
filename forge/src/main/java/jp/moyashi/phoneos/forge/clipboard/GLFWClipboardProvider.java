package jp.moyashi.phoneos.forge.clipboard;

import jp.moyashi.phoneos.core.service.LoggerService;
import jp.moyashi.phoneos.core.service.clipboard.ClipboardProvider;
import net.minecraft.client.Minecraft;
import processing.core.PImage;

/**
 * GLFW（LWJGL）を使用したクリップボードプロバイダー。
 * Forge MOD環境（Minecraft）で使用される。
 */
public class GLFWClipboardProvider implements ClipboardProvider {

    private final LoggerService logger;
    private final Minecraft minecraft;

    public GLFWClipboardProvider(LoggerService logger) {
        this.logger = logger;
        this.minecraft = Minecraft.getInstance();

        if (logger != null) {
            logger.info("GLFWClipboardProvider", "GLFW クリップボードプロバイダーを初期化");
        }
    }

    @Override
    public boolean setText(String text) {
        if (text == null) {
            return false;
        }

        try {
            // MinecraftのkeyboardListenerを使用してクリップボードに設定
            minecraft.keyboardHandler.setClipboard(text);

            if (logger != null) {
                logger.debug("GLFWClipboardProvider", "テキストをコピー: 長さ " + text.length());
            }
            return true;
        } catch (Exception e) {
            if (logger != null) {
                logger.error("GLFWClipboardProvider", "テキストのコピーに失敗: " + e.getMessage());
            }
            return false;
        }
    }

    @Override
    public String getText() {
        try {
            // MinecraftのkeyboardListenerを使用してクリップボードから取得
            String text = minecraft.keyboardHandler.getClipboard();

            if (logger != null && text != null && !text.isEmpty()) {
                logger.debug("GLFWClipboardProvider", "テキストを取得: 長さ " + text.length());
            }

            return text;
        } catch (Exception e) {
            if (logger != null) {
                logger.error("GLFWClipboardProvider", "テキストの取得に失敗: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public boolean hasText() {
        try {
            String text = minecraft.keyboardHandler.getClipboard();
            return text != null && !text.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean setImage(PImage image) {
        // GLFWは画像のクリップボード操作をサポートしていない
        if (logger != null) {
            logger.warn("GLFWClipboardProvider", "画像のコピーはサポートされていません");
        }
        return false;
    }

    @Override
    public PImage getImage() {
        // GLFWは画像のクリップボード操作をサポートしていない
        return null;
    }

    @Override
    public boolean hasImage() {
        // GLFWは画像のクリップボード操作をサポートしていない
        return false;
    }

    @Override
    public void clear() {
        try {
            minecraft.keyboardHandler.setClipboard("");
            if (logger != null) {
                logger.debug("GLFWClipboardProvider", "クリップボードをクリア");
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("GLFWClipboardProvider", "クリップボードのクリアに失敗: " + e.getMessage());
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return minecraft != null && minecraft.keyboardHandler != null;
    }
}
