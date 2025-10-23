package jp.moyashi.phoneos.forge.chromium;

import com.cinemamod.mcef.MCEFBrowser;
import com.cinemamod.mcef.MCEFRenderer;
import com.mojang.blaze3d.platform.GlStateManager;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumRenderHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.lwjgl.BufferUtils;
import processing.core.PImage;

import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.glGetTexImage;
import static org.lwjgl.opengl.GL12.GL_BGRA;
import static org.lwjgl.opengl.GL12.GL_UNSIGNED_INT_8_8_8_8_REV;

/**
 * MCEFのOpenGLテクスチャ、もしくはonPaint()で受け取るByteBufferからPImageへ変換するアダプター。
 * 可能な場合はレンダラーから直接フレームバッファを取得し、OpenGLテクスチャからの読み戻しを回避する。
 */
public class MCEFRenderHandlerAdapter extends ChromiumRenderHandler implements MochiMCEFBrowser.FrameListener {

    private static final Logger LOGGER = LogManager.getLogger();

    private final MCEFBrowser mcefBrowser;
    private final MochiMCEFBrowser mochiBrowser;
    private final MCEFRenderer mcefRenderer;
    private final ByteBuffer pixelBuffer;
    private final int imageWidth;
    private final int imageHeight;
    private final int[] latestPixels;
    private final Object pixelLock = new Object();
    private boolean frameAvailable = false;
    private boolean textureUpdated = false;
    private final boolean usingSharedFrame;
    private boolean loggedFirstListenerFrame = false;

    public MCEFRenderHandlerAdapter(Kernel kernel, MCEFBrowser mcefBrowser, int width, int height) {
        super(kernel, width, height);
        this.mcefBrowser = mcefBrowser;
        this.imageWidth = width;
        this.imageHeight = height;
        this.pixelBuffer = BufferUtils.createByteBuffer(width * height * 4);
        this.latestPixels = new int[width * height];

        if (mcefBrowser instanceof MochiMCEFBrowser) {
            this.mochiBrowser = (MochiMCEFBrowser) mcefBrowser;
            this.mochiBrowser.addFrameListener(this);
            this.usingSharedFrame = true;
            try {
                this.mochiBrowser.resize(width, height);
                LOGGER.info("[MCEFRenderHandlerAdapter] Called MochiMCEFBrowser.resize({}, {})", width, height);
            } catch (Exception e) {
                LOGGER.warn("[MCEFRenderHandlerAdapter] Failed to call MochiMCEFBrowser.resize: {}", e.getMessage());
            }
        } else {
            this.mochiBrowser = null;
            this.usingSharedFrame = false;
        }

        this.mcefRenderer = mcefBrowser.getRenderer();

        LOGGER.info("[MCEFRenderHandlerAdapter] Created adapter for MCEFBrowser");
        LOGGER.info("[MCEFRenderHandlerAdapter]   - Texture ID: " + mcefRenderer.getTextureID());
        LOGGER.info("[MCEFRenderHandlerAdapter]   - Size: " + width + "x" + height);
        if (usingSharedFrame) {
            LOGGER.info("[MCEFRenderHandlerAdapter] Direct frame listener registered");
        } else {
            LOGGER.warn("[MCEFRenderHandlerAdapter] Direct frame listener unavailable (falling back to glGetTexImage)");
        }
    }

    /**
     * ChromiumBrowser.drawToPGraphics()から毎フレーム呼び出される。
     * 共有フレームが利用可能であればコピーし、そうでなければOpenGLテクスチャから読み出す。
     */
    public void updateFromTexture() {
        if (applyLatestFrameFromListener()) {
            return;
        }

        if (usingSharedFrame && textureUpdated) {
            // 既に初回フレームを受信しており、新しいフレームがない場合は何もしない
            return;
        }

        int textureID = mcefRenderer.getTextureID();
        if (textureID == 0) {
            if (!textureUpdated) {
                LOGGER.info("[MCEFRenderHandlerAdapter] Texture not yet initialized (textureID=0), waiting...");
            }
            return;
        }

        if (!textureUpdated) {
            LOGGER.info("[MCEFRenderHandlerAdapter] First texture update - textureID: " + textureID);
        }

        try {
            GlStateManager._bindTexture(textureID);
            pixelBuffer.clear();
            glGetTexImage(GL_TEXTURE_2D, 0, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
            pixelBuffer.rewind();

            PImage img = getImage();
            if (img != null) {
                img.loadPixels();
                for (int i = 0; i < img.pixels.length; i++) {
                    int b = pixelBuffer.get() & 0xFF;
                    int g = pixelBuffer.get() & 0xFF;
                    int r = pixelBuffer.get() & 0xFF;
                    int a = pixelBuffer.get() & 0xFF;
                    img.pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
                }
                img.updatePixels();

                if (!textureUpdated) {
                    LOGGER.info("[MCEFRenderHandlerAdapter] First texture update completed successfully");
                }
                textureUpdated = true;
            }
        } catch (Exception e) {
            LOGGER.error("[MCEFRenderHandlerAdapter] Error updating from texture: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean needsUpdate() {
        return true;
    }

    public MCEFBrowser getMCEFBrowser() {
        return mcefBrowser;
    }

    @Override
    public void onFrame(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        if (!usingSharedFrame || popup || buffer == null) {
            return;
        }
        if (width != imageWidth || height != imageHeight) {
            return;
        }

        ByteBuffer duplicate = buffer.duplicate().order(ByteOrder.nativeOrder());
        duplicate.position(0);
        duplicate.limit(Math.min(duplicate.capacity(), imageWidth * imageHeight * 4));

        int copiedPixels = 0;
        int firstPixel = 0;
        synchronized (pixelLock) {
            int maxPixels = Math.min(latestPixels.length, duplicate.remaining() / 4);
            for (int i = 0; i < maxPixels; i++) {
                int b = duplicate.get() & 0xFF;
                int g = duplicate.get() & 0xFF;
                int r = duplicate.get() & 0xFF;
                int a = duplicate.get() & 0xFF;
                latestPixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }
            frameAvailable = true;
            copiedPixels = maxPixels;
            firstPixel = maxPixels > 0 ? latestPixels[0] : 0;
        }

        if (!loggedFirstListenerFrame) {
            loggedFirstListenerFrame = true;
            LOGGER.info("[MCEFRenderHandlerAdapter] onFrame received - popup={} dirtyRects={} copiedPixels={} firstPixel=0x{}", popup, dirtyRects != null ? dirtyRects.length : 0, copiedPixels, Integer.toHexString(firstPixel));
        }
    }

    private boolean applyLatestFrameFromListener() {
        if (!usingSharedFrame) {
            return false;
        }

        synchronized (pixelLock) {
            if (!frameAvailable) {
                return false;
            }
            frameAvailable = false;

            PImage img = getImage();
            if (img == null) {
                return true;
            }

            img.loadPixels();
            System.arraycopy(latestPixels, 0, img.pixels, 0, Math.min(img.pixels.length, latestPixels.length));
            img.updatePixels();
            if (!textureUpdated) {
                LOGGER.info("[MCEFRenderHandlerAdapter] First frame applied via listener firstPixel=0x{}", Integer.toHexString(latestPixels[0]));
            }
        }

        if (!textureUpdated) {
            LOGGER.info("[MCEFRenderHandlerAdapter] Frame copied via direct listener");
        }
        textureUpdated = true;
        return true;
    }
}
