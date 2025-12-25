package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefPaintEvent;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandlerAdapter;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.opengl.PGL;
import processing.opengl.PGraphicsOpenGL;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Chromiumオフスクリーンレンダリングハンドラー。
 * onPaint()コールバックでByteBuffer（BGRA）をPImageに変換する。
 *
 * アーキテクチャ:
 * - onPaint(): Chromiumからのペイントコールバック
 * - ByteBuffer（BGRA）→ BufferedImage（ARGB）→ PImage変換
 * - 描画更新フラグ管理（needsUpdate）
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class ChromiumRenderHandler extends CefRenderHandlerAdapter {

    private final Kernel kernel;
    private volatile int width;
    private volatile int height;
    private PImage image;
    private final AtomicBoolean needsUpdate = new AtomicBoolean(false);
    private final Object imageLock = new Object();
    private final boolean isMac;

    // フレームスキップ用（過剰なフレーム更新を防止）
    private long lastPaintTimeNs = 0L;
    private static final long MIN_PAINT_INTERVAL_NS = 16_000_000L; // 16ms = 60FPS（P2D GPU描画対応）

    // GPU直接テクスチャアップロード用
    private int glTextureId = -1;
    private boolean useGPUUpload = true;
    private boolean gpuUploadInitialized = false;
    private static final int GL_BGRA = 0x80E1;  // OpenGL GL_BGRA定数

    /**
     * ChromiumRenderHandlerを構築する。
     *
     * @param kernel Kernelインスタンス
     * @param width 幅
     * @param height 高さ
     */
    public ChromiumRenderHandler(Kernel kernel, int width, int height) {
        this.kernel = kernel;
        this.width = width;
        this.height = height;

        // OS判定（Mac環境でのみHiDPIリサイズ処理を行う）
        String osName = System.getProperty("os.name").toLowerCase();
        this.isMac = osName.contains("mac");

        // PImageを初期化（ARGB形式）
        this.image = new PImage(width, height, PImage.ARGB);
        this.image.loadPixels();

        // 初期背景色（白）
        for (int i = 0; i < image.pixels.length; i++) {
            image.pixels[i] = 0xFFFFFFFF; // 白
        }
        this.image.updatePixels();
    }

    /**
     * Chromiumからのペイントコールバック。
     * ByteBuffer（BGRA形式）をPImage（ARGB形式）に変換する。
     * 最適化: IntBufferによるバルク読み取りとビットマスク演算で高速変換。
     *
     * @param browser CEFブラウザ
     * @param popup ポップアップフラグ
     * @param dirtyRects 更新領域（使用しない）
     * @param buffer BGRAピクセルデータ
     * @param width 幅
     * @param height 高さ
     */
    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects,
                        ByteBuffer buffer, int width, int height) {
        // フレームスキップ：前回から16ms未満の場合はスキップ（60FPS制限）
        long now = System.nanoTime();
        if (now - lastPaintTimeNs < MIN_PAINT_INTERVAL_NS) {
            return;
        }
        lastPaintTimeNs = now;

        // バッファチェック
        if (buffer == null || buffer.remaining() < width * height * 4) {
            return;
        }

        boolean isHiDPI = isMac && (width == this.width * 2);

        synchronized (imageLock) {
            buffer.position(0);
            image.loadPixels();
            int[] pixels = image.pixels;

            if (isHiDPI) {
                // HiDPI: 2x2ピクセルブロックを1ピクセルにダウンサンプリング
                // バイト単位でアクセス（エンディアン問題を回避）
                for (int y = 0; y < this.height; y++) {
                    for (int x = 0; x < this.width; x++) {
                        int srcX = x * 2;
                        int srcY = y * 2;
                        int srcOffset = (srcY * width + srcX) * 4;
                        int b = buffer.get(srcOffset) & 0xFF;
                        int g = buffer.get(srcOffset + 1) & 0xFF;
                        int r = buffer.get(srcOffset + 2) & 0xFF;
                        int a = buffer.get(srcOffset + 3) & 0xFF;
                        pixels[y * this.width + x] = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                }
            } else {
                // 非HiDPI: インデックスアクセスで高速化（position管理不要）
                int pixelCount = width * height;
                for (int i = 0; i < pixelCount; i++) {
                    int offset = i * 4;
                    int b = buffer.get(offset) & 0xFF;
                    int g = buffer.get(offset + 1) & 0xFF;
                    int r = buffer.get(offset + 2) & 0xFF;
                    int a = buffer.get(offset + 3) & 0xFF;
                    pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }

            image.updatePixels();
            needsUpdate.set(true);
        }
    }

    /**
     * GPU直接テクスチャアップロード。
     * OpenGLテクスチャにBGRAデータを直接アップロードし、GPU内部で色変換を行う。
     * P2D（JOGL）レンダラー使用時に最高のパフォーマンスを発揮。
     *
     * @param g PGraphicsインスタンス（PGraphicsOpenGLが必要）
     * @param buffer BGRAピクセルデータ
     * @param width 幅
     * @param height 高さ
     * @return 成功した場合true
     */
    public boolean uploadToGPUTexture(PGraphics g, ByteBuffer buffer, int width, int height) {
        if (!(g instanceof PGraphicsOpenGL)) {
            useGPUUpload = false;
            return false;
        }

        try {
            PGraphicsOpenGL pg = (PGraphicsOpenGL) g;
            PGL pgl = pg.beginPGL();

            try {
                // テクスチャ作成（初回のみ）
                if (glTextureId == -1) {
                    IntBuffer texID = IntBuffer.allocate(1);
                    pgl.genTextures(1, texID);
                    glTextureId = texID.get(0);
                    gpuUploadInitialized = true;
                }

                pgl.bindTexture(PGL.TEXTURE_2D, glTextureId);

                // GL_BGRA形式で直接アップロード（CPU変換不要！）
                buffer.position(0);
                pgl.texImage2D(
                        PGL.TEXTURE_2D,
                        0,
                        PGL.RGBA8,
                        width, height,
                        0,
                        GL_BGRA,
                        PGL.UNSIGNED_BYTE,
                        buffer
                );

                pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_MIN_FILTER, PGL.LINEAR);
                pgl.texParameteri(PGL.TEXTURE_2D, PGL.TEXTURE_MAG_FILTER, PGL.LINEAR);
                pgl.bindTexture(PGL.TEXTURE_2D, 0);

                return true;
            } finally {
                pg.endPGL();
            }
        } catch (Exception e) {
            useGPUUpload = false;
            return false;
        }
    }

    /**
     * GPU直接アップロードが利用可能かを確認する。
     *
     * @return GPU直接アップロードが利用可能な場合true
     */
    public boolean isGPUUploadAvailable() {
        return useGPUUpload && gpuUploadInitialized;
    }

    /**
     * OpenGLテクスチャIDを取得する。
     *
     * @return テクスチャID（未作成の場合-1）
     */
    public int getGLTextureId() {
        return glTextureId;
    }

    /**
     * デバッグログ出力。
     */
    private void log(String message) {
        if (kernel.getLogger() != null) {
            kernel.getLogger().debug("ChromiumRenderHandler", message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        if (kernel.getLogger() != null) {
            kernel.getLogger().error("ChromiumRenderHandler", message);
        }
    }

    /**
     * レンダリング結果のPImageを取得する。
     *
     * @return PImageインスタンス
     */
    public PImage getImage() {
        synchronized (imageLock) {
            return image;
        }
    }

    /**
     * 描画更新が必要かを確認する。
     *
     * @return 更新が必要な場合true
     */
    public boolean needsUpdate() {
        return needsUpdate.getAndSet(false);
    }

    /**
     * ビューポート矩形を返す（必須オーバーライド）。
     *
     * @param browser CEFブラウザ
     * @return ビューポート矩形
     */
    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        return new Rectangle(0, 0, width, height);
    }

    /**
     * ビューポートサイズを更新する。
     * wasResized()呼び出し前にこのメソッドでサイズを更新する必要がある。
     *
     * @param newWidth 新しい幅
     * @param newHeight 新しい高さ
     */
    public void setSize(int newWidth, int newHeight) {
        if (newWidth <= 0 || newHeight <= 0) {
            log("Invalid size: " + newWidth + "x" + newHeight);
            return;
        }

        synchronized (imageLock) {
            this.width = newWidth;
            this.height = newHeight;

            // PImageを新しいサイズで再作成
            this.image = new PImage(newWidth, newHeight, PImage.ARGB);
            this.image.loadPixels();
            for (int i = 0; i < image.pixels.length; i++) {
                image.pixels[i] = 0xFFFFFFFF; // 白
            }
            this.image.updatePixels();

            log("Viewport resized to: " + newWidth + "x" + newHeight);
        }
    }

    /**
     * onPaintリスナーを設定する（CefRenderHandlerの抽象メソッド）。
     * 現在は使用していないため、空実装。
     */
    @Override
    public void setOnPaintListener(Consumer<CefPaintEvent> listener) {
        // 空実装：現在はリスナー登録機能を使用していない
    }

    /**
     * onPaintリスナーを追加する（CefRenderHandlerの抽象メソッド）。
     * 現在は使用していないため、空実装。
     */
    @Override
    public void addOnPaintListener(Consumer<CefPaintEvent> listener) {
        // 空実装：現在はリスナー登録機能を使用していない
    }

    /**
     * onPaintリスナーを削除する（CefRenderHandlerの抽象メソッド）。
     * 現在は使用していないため、空実装。
     */
    @Override
    public void removeOnPaintListener(Consumer<CefPaintEvent> listener) {
        // 空実装：現在はリスナー登録機能を使用していない
    }
}
