package jp.moyashi.phoneos.core.service.chromium.texture;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * テクスチャリソースハンドラー。
 * 2つのURLパターンに対応:
 * 1. mochitexture://namespace/path  (カスタムスキーム - 直接登録)
 * 2. http://3-texture/namespace/path (HTTP経由 - IPvMSchemeHandlerFactory から委譲)
 *
 * 例: mochitexture://minecraft/stone, http://3-texture/minecraft/stone
 *
 * クライアント側のResourceManagerからテクスチャPNGを返す。
 * サーバースレッドで実行されるPiggleShopApiServerの代わりに、
 * Chromiumがクライアント側で直接テクスチャを取得する。
 */
public class TextureSchemeHandler extends CefResourceHandlerAdapter {

    private final Kernel kernel;

    private static final Pattern URL_PATTERN = Pattern.compile(
        "^mochitexture://([a-z0-9_.-]+)/([a-z0-9_/.-]+)$"
    );

    /** HTTP経由のテクスチャURLパターン: http://3-texture/namespace/path */
    private static final Pattern HTTP_TEXTURE_PATTERN = Pattern.compile(
        "^https?://3-texture/([a-z0-9_.-]+)/([a-z0-9_/.-]+)$"
    );

    /** テクスチャキャッシュ（namespace:path → PNGバイト配列） */
    private static final Map<String, byte[]> TEXTURE_CACHE = new ConcurrentHashMap<>();

    /** フォールバック用紫黒テクスチャ（16x16 PNG） */
    private static final byte[] FALLBACK_TEXTURE = createFallbackTexture();

    private byte[] responseData;
    private String mimeType = "image/png";
    private int statusCode = 200;
    private int readPosition = 0;

    public TextureSchemeHandler(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        String url = request.getURL();
        System.err.println("[TextureSchemeHandler] processRequest: " + url);
        log("processRequest: " + url);

        // 両方のURLパターンを試行
        Matcher matcher = URL_PATTERN.matcher(url);
        if (!matcher.matches()) {
            matcher = HTTP_TEXTURE_PATTERN.matcher(url);
        }
        if (!matcher.matches()) {
            System.err.println("[TextureSchemeHandler] Invalid texture URL: " + url);
            logError("Invalid texture URL: " + url);
            responseData = FALLBACK_TEXTURE;
            callback.Continue();
            return true;
        }

        String namespace = matcher.group(1);
        String path = matcher.group(2);
        String cacheKey = namespace + ":" + path;

        // キャッシュチェック
        byte[] cached = TEXTURE_CACHE.get(cacheKey);
        if (cached != null) {
            responseData = cached;
            callback.Continue();
            return true;
        }

        // TextureProviderからテクスチャを取得
        TextureProvider provider = kernel.getTextureProvider();
        if (provider == null) {
            logError("TextureProvider not available for " + cacheKey);
            responseData = FALLBACK_TEXTURE;
            callback.Continue();
            return true;
        }

        try {
            byte[] textureData = provider.getItemTexture(namespace, path);
            if (textureData != null && textureData.length > 0) {
                log("Loaded texture " + cacheKey + " (" + textureData.length + " bytes)");
                TEXTURE_CACHE.put(cacheKey, textureData);
                responseData = textureData;
            } else {
                logError("Texture not found: " + cacheKey + ", using fallback");
                responseData = FALLBACK_TEXTURE;
            }
        } catch (Exception e) {
            logError("Failed to get texture for " + cacheKey + ": " + e.getMessage());
            responseData = FALLBACK_TEXTURE;
        }

        callback.Continue();
        return true;
    }

    @Override
    public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
        response.setStatus(statusCode);
        response.setMimeType(mimeType);
        response.setStatusText("OK");

        if (responseData != null) {
            responseLength.set(responseData.length);
        } else {
            responseLength.set(0);
        }
    }

    @Override
    public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
        if (responseData == null || readPosition >= responseData.length) {
            bytesRead.set(0);
            return false;
        }

        int remainingBytes = responseData.length - readPosition;
        int bytesToCopy = Math.min(bytesToRead, remainingBytes);

        System.arraycopy(responseData, readPosition, dataOut, 0, bytesToCopy);
        readPosition += bytesToCopy;
        bytesRead.set(bytesToCopy);

        return true;
    }

    /**
     * 16x16の紫黒チェッカーパターンPNGを生成する（Missing Texture）。
     */
    private static byte[] createFallbackTexture() {
        // 最小限の16x16 PNGを手動構築
        int size = 16;
        int[] pixels = new int[size * size];
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // 8x8チェッカーパターン（紫と黒）
                boolean isBlack = ((x / 8) + (y / 8)) % 2 == 0;
                pixels[y * size + x] = isBlack ? 0xFF000000 : 0xFFF800F8;
            }
        }
        return encodePNG(pixels, size, size);
    }

    /**
     * ARGB ピクセル配列からPNGバイト配列を生成する。
     * 外部ライブラリを使わず、最小限のPNGエンコーダ。
     */
    private static byte[] encodePNG(int[] pixels, int width, int height) {
        try {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(createBufferedImage(pixels, width, height), "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            // 最悪の場合、1x1透明PNGを返す
            return new byte[]{
                (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte)0xC4,
                (byte)0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
                0x54, 0x78, (byte)0x9C, 0x62, 0x00, 0x00, 0x00, 0x02,
                0x00, 0x01, (byte)0xE5, 0x27, (byte)0xDE, (byte)0xFC, 0x00, 0x00,
                0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte)0xAE, 0x42,
                0x60, (byte)0x82
            };
        }
    }

    private static java.awt.image.BufferedImage createBufferedImage(int[] pixels, int width, int height) {
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
            width, height, java.awt.image.BufferedImage.TYPE_INT_ARGB
        );
        img.setRGB(0, 0, width, height, pixels, 0, width);
        return img;
    }

    private void log(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().info("TextureSchemeHandler", message);
        }
    }

    private void logError(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("TextureSchemeHandler", message);
        }
    }
}
