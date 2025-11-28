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
    private final int width;
    private final int height;
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
    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects,
                        ByteBuffer buffer, int width, int height) {
        // フレームスキップ：前回から16ms未満の場合はスキップ（60FPS制限）
                long now = System.nanoTime();
                if (now - lastPaintTimeNs < MIN_PAINT_INTERVAL_NS) {
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
