package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefPaintEvent;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandlerAdapter;
import processing.core.PImage;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
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
     *
     * @param browser CEFブラウザ
     * @param popup ポップアップフラグ
     * @param dirtyRects 更新領域（使用しない）
     * @param buffer BGRAピクセルデータ
     * @param width 幅
     * @param height 高さ
     */
    // デバッグ用: onPaint呼び出しカウンター
    private int onPaintCount = 0;

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects,
                        ByteBuffer buffer, int width, int height) {
        onPaintCount++;

        // デバッグ: onPaintが呼ばれていることを確認
        log("onPaint called #" + onPaintCount + ": " + width + "x" + height + ", popup=" + popup + ", buffer=" + (buffer != null ? buffer.remaining() + " bytes" : "null"));

        // デバッグ: バッファの最初の数ピクセルの内容を確認
        if (buffer != null && buffer.remaining() >= 16) {
            buffer.position(0);
            StringBuilder sb = new StringBuilder("First 4 pixels (BGRA): ");
            for (int i = 0; i < 4; i++) {
                int b = buffer.get() & 0xFF;
                int g = buffer.get() & 0xFF;
                int r = buffer.get() & 0xFF;
                int a = buffer.get() & 0xFF;
                sb.append(String.format("[B:%d G:%d R:%d A:%d] ", b, g, r, a));
            }
            buffer.position(0); // リセット
            log(sb.toString());

            // 中央付近のピクセルも確認
            int centerIndex = (height / 2 * width + width / 2) * 4;
            if (buffer.remaining() > centerIndex + 4) {
                buffer.position(centerIndex);
                int b = buffer.get() & 0xFF;
                int g = buffer.get() & 0xFF;
                int r = buffer.get() & 0xFF;
                int a = buffer.get() & 0xFF;
                log(String.format("Center pixel (BGRA): [B:%d G:%d R:%d A:%d]", b, g, r, a));
                buffer.position(0); // リセット
            }
        }

        // フレームスキップ：前回から16ms未満の場合はスキップ（60FPS制限）
        long now = System.nanoTime();
        if (now - lastPaintTimeNs < MIN_PAINT_INTERVAL_NS) {
            log("Frame skipped (too soon)");
            return; // スキップ
        }
        lastPaintTimeNs = now;
        
                boolean isHiDPI = isMac && (width == this.width * 2);
        
                // サイズチェック（HiDPI/Retinaディスプレイ対応）
                // Mac Retinaでは2倍サイズ（800x952）でレンダリングされる可能性がある
                if (width != this.width && !isHiDPI) {
            log("Size difference: expected " + this.width + "x" + this.height +
                ", got " + width + "x" + height + " - using received size");
            // サイズが違っても続行（エラーで返さない）
        }

        synchronized (imageLock) {
            buffer.position(0);
            image.loadPixels();

            if (isHiDPI) {
                // HiDPI: 2x2ピクセルブロックを1ピクセルにダウンサンプリング
                // 800x952 → 400x476（2倍スケールを1/2に縮小）
                for (int y = 0; y < this.height; y++) {
                    for (int x = 0; x < this.width; x++) {
                        // 2x2ブロックの左上ピクセルをサンプリング（Nearest Neighbor）
                        int srcX = x * 2;
                        int srcY = y * 2;
                        int srcIndex = (srcY * width + srcX) * 4; // 4 bytes per pixel (BGRA)

                        // BGRAバイトを読み取り
                        int b = buffer.get(srcIndex) & 0xFF;
                        int g = buffer.get(srcIndex + 1) & 0xFF;
                        int r = buffer.get(srcIndex + 2) & 0xFF;
                        int a = buffer.get(srcIndex + 3) & 0xFF;

                        // ARGBフォーマットに変換
                        int argb = (a << 24) | (r << 16) | (g << 8) | b;

                        // PImageピクセル配列に設定
                        image.pixels[y * this.width + x] = argb;
                    }
                }
            } else {
                // 非HiDPI: 通常の1:1変換
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        // BGRAバイトを読み取り
                        int b = buffer.get() & 0xFF;
                        int g = buffer.get() & 0xFF;
                        int r = buffer.get() & 0xFF;
                        int a = buffer.get() & 0xFF;

                        // ARGBフォーマットに変換
                        int argb = (a << 24) | (r << 16) | (g << 8) | b;

                        // PImageピクセル配列に設定
                        image.pixels[y * width + x] = argb;
                    }
                }
            }

            image.updatePixels();
            needsUpdate.set(true);

            // デバッグ: 変換後のPImageの内容を確認
            if (onPaintCount <= 3) {
                int whiteCount = 0;
                int nonWhiteCount = 0;
                int sampleSize = Math.min(100, image.pixels.length);
                StringBuilder pixelSamples = new StringBuilder("PImage samples: ");
                for (int i = 0; i < sampleSize; i++) {
                    int pixel = image.pixels[i];
                    if (pixel == 0xFFFFFFFF) {
                        whiteCount++;
                    } else {
                        nonWhiteCount++;
                        if (nonWhiteCount <= 3) {
                            pixelSamples.append(String.format("[%d:0x%08X] ", i, pixel));
                        }
                    }
                }
                log("PImage after conversion: white=" + whiteCount + ", nonWhite=" + nonWhiteCount + "/" + sampleSize);
                if (nonWhiteCount > 0) {
                    log(pixelSamples.toString());
                }
            }
        }
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
