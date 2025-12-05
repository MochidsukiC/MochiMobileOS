package jp.moyashi.phoneos.core.render;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.ScreenManager;
import jp.moyashi.phoneos.core.ui.theme.ThemeEngine;
import processing.core.PGraphics;

/**
 * 描画パイプラインを管理するクラス。
 * PGraphicsバッファへの描画、背景処理、スクリーン描画の委譲を担当。
 * Kernelクラスから描画責任を分離し、単一責任原則に準拠。
 *
 * @since 2025-12-02
 * @version 1.0
 */
public class RenderPipeline {

    /** 処理を委譲するKernelインスタンス */
    private final Kernel kernel;

    /** フレームカウント */
    private int frameCount = 0;

    /** ピクセルキャッシュ（パフォーマンス最適化用） */
    private int[] pixelsCache;
    private boolean pixelsCacheDirty = true;

    /** 画面サイズ */
    private int width;
    private int height;

    /** 前回のフレーム時間（FPS計測用） */
    private long lastFrameTime = System.nanoTime();

    /** FPS計測用 */
    private float currentFps = 60.0f;
    private int fpsFrameCount = 0;
    private long fpsStartTime = System.nanoTime();

    /**
     * RenderPipelineを初期化する。
     *
     * @param kernel Kernelインスタンス
     * @param width 画面幅
     * @param height 画面高さ
     */
    public RenderPipeline(Kernel kernel, int width, int height) {
        this.kernel = kernel;
        this.width = width;
        this.height = height;
        this.pixelsCache = new int[width * height];
    }

    /**
     * 描画処理を実行する。
     * 背景描画、スクリーン描画、スリープ処理を管理。
     *
     * @param graphics PGraphicsインスタンス
     * @param screenManager スクリーンマネージャー
     * @param themeEngine テーマエンジン
     * @param isSleeping スリープ状態
     */
    public void render(PGraphics graphics, ScreenManager screenManager,
                      ThemeEngine themeEngine, boolean isSleeping) {
        // PGraphicsの描画開始を宣言（重要！）
        graphics.beginDraw();

        try {
            // スリープ中は描画をスキップして電力を節約
            if (isSleeping) {
                renderSleepMode(graphics);
            } else {
                // 通常の描画処理
                // 背景描画
                renderBackground(graphics, themeEngine);

                // スクリーンマネージャーによる画面描画
                if (screenManager != null) {
                    screenManager.draw(graphics);
                }

                // ピクセルキャッシュを更新
                updatePixelsCache(graphics);

                // フレームカウントとFPSを更新
                updateFrameInfo();
            }
        } catch (Exception e) {
            System.err.println("RenderPipeline: 描画エラー: " + e.getMessage());
            e.printStackTrace();
            // エラー時でも描画を継続（クラッシュを防ぐ）
            renderErrorState(graphics);
        } finally {
            // PGraphicsの描画終了を宣言（必須！）
            graphics.endDraw();
        }
    }

    /**
     * 背景を描画する。
     * テーマエンジンから背景色を取得して適用。
     *
     * @param g PGraphicsインスタンス
     * @param themeEngine テーマエンジン
     */
    private void renderBackground(PGraphics g, ThemeEngine themeEngine) {
        int bgColor;

        // テーマエンジンから背景色を取得
        if (themeEngine != null) {
            bgColor = themeEngine.colorBackground();
        } else {
            // テーマエンジンがない場合はデフォルト色
            bgColor = 0xFF1A1A1A; // ダークグレー
        }

        // 背景色を適用
        g.background((bgColor >> 16) & 0xFF, // R
                    (bgColor >> 8) & 0xFF,  // G
                    bgColor & 0xFF);        // B
    }

    /**
     * スリープモード時の描画処理。
     * 黒画面を表示して電力を節約。
     *
     * @param g PGraphicsインスタンス
     */
    private void renderSleepMode(PGraphics g) {
        // 黒画面を表示
        g.background(0);

        // スリープ中であることを示す小さなインジケーター（オプション）
        if (shouldShowSleepIndicator()) {
            g.pushStyle();
            g.fill(50);
            g.noStroke();
            g.ellipse(width / 2, height / 2, 10, 10);
            g.popStyle();
        }
    }

    /**
     * エラー状態時の描画処理。
     * エラーが発生してもアプリケーションがクラッシュしないようにする。
     *
     * @param g PGraphicsインスタンス
     */
    private void renderErrorState(PGraphics g) {
        // エラー時は安全な背景色を表示
        g.background(64, 0, 0); // 暗い赤

        // エラーメッセージを表示（デバッグ用）
        if (kernel.isDebugMode()) {
            g.pushStyle();
            g.fill(255);
            g.textAlign(PGraphics.CENTER, PGraphics.CENTER);
            g.text("Render Error Occurred", width / 2, height / 2);
            g.popStyle();
        }
    }

    /**
     * ピクセルキャッシュを更新する。
     * パフォーマンス最適化のため、変更があった場合のみ更新。
     *
     * @param g PGraphicsインスタンス
     */
    private void updatePixelsCache(PGraphics g) {
        if (pixelsCacheDirty) {
            g.loadPixels();
            if (g.pixels != null && g.pixels.length == pixelsCache.length) {
                System.arraycopy(g.pixels, 0, pixelsCache, 0, g.pixels.length);
                pixelsCacheDirty = false;
            }
        }
    }

    /**
     * フレーム情報を更新する。
     * フレームカウントとFPSを計算。
     */
    private void updateFrameInfo() {
        frameCount++;
        fpsFrameCount++;

        // 1秒ごとにFPSを計算
        long currentTime = System.nanoTime();
        long elapsedTime = currentTime - fpsStartTime;

        if (elapsedTime >= 1_000_000_000L) { // 1秒経過
            currentFps = (float) fpsFrameCount * 1_000_000_000L / elapsedTime;
            fpsFrameCount = 0;
            fpsStartTime = currentTime;

            // デバッグモードの場合はFPSを出力
            if (kernel.isDebugMode()) {
                System.out.println("RenderPipeline: FPS = " + String.format("%.1f", currentFps));
            }
        }

        lastFrameTime = currentTime;
    }

    /**
     * スリープインジケーターを表示すべきかを判定する。
     *
     * @return 表示すべき場合true
     */
    private boolean shouldShowSleepIndicator() {
        // 設定から取得（将来的な実装）
        // 現在は常に非表示
        return false;
    }

    /**
     * 画面サイズを更新する。
     * ウィンドウリサイズ時に呼び出される。
     *
     * @param newWidth 新しい幅
     * @param newHeight 新しい高さ
     */
    public void updateSize(int newWidth, int newHeight) {
        if (width != newWidth || height != newHeight) {
            this.width = newWidth;
            this.height = newHeight;
            this.pixelsCache = new int[newWidth * newHeight];
            this.pixelsCacheDirty = true;
            System.out.println("RenderPipeline: Screen size updated to " + newWidth + "x" + newHeight);
        }
    }

    /**
     * ピクセルキャッシュを無効化する。
     * 次回の描画時に更新される。
     */
    public void invalidatePixelsCache() {
        this.pixelsCacheDirty = true;
    }

    /**
     * 現在のフレームカウントを取得する。
     *
     * @return フレームカウント
     */
    public int getFrameCount() {
        return frameCount;
    }

    /**
     * 現在のFPSを取得する。
     *
     * @return FPS値
     */
    public float getCurrentFps() {
        return currentFps;
    }

    /**
     * ピクセルキャッシュを取得する。
     * パフォーマンス最適化のため、直接配列を返す（コピーしない）。
     *
     * @return ピクセル配列
     */
    public int[] getPixelsCache() {
        return pixelsCache;
    }

    /**
     * 画面幅を取得する。
     *
     * @return 画面幅
     */
    public int getWidth() {
        return width;
    }

    /**
     * 画面高さを取得する。
     *
     * @return 画面高さ
     */
    public int getHeight() {
        return height;
    }
}