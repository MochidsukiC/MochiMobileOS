package jp.moyashi.phoneos.core.ipc;

import jp.moyashi.phoneos.core.Kernel.LayerType;
import processing.core.PFont;
import processing.core.PGraphics;

/**
 * Kernelプロキシインターフェース。
 * ProcessingScreenがKernelまたはRemoteKernelを透過的に使用するための共通インターフェース。
 *
 * @author jp.moyashi
 * @version 1.0
 */
public interface KernelProxy {

    // === publicフィールド相当のGetter ===

    /**
     * フレームカウントを取得する。
     */
    int getFrameCount();

    /**
     * 画面幅を取得する。
     */
    int getWidth();

    /**
     * 画面高さを取得する。
     */
    int getHeight();

    // === グループA: 毎フレーム（描画） ===

    /**
     * 更新処理を実行する。
     */
    void update();

    /**
     * 描画処理を実行する。
     */
    void render();

    /**
     * ピクセル配列を取得する。
     *
     * @return ピクセル配列（ARGB形式）
     */
    int[] getPixels();

    /**
     * グラフィックスバッファを取得する。
     *
     * @return PGraphicsインスタンス、または利用不可の場合null
     */
    PGraphics getGraphics();

    // === グループB: ライフサイクル ===

    /**
     * スリープ状態にする。
     */
    void sleep();

    /**
     * ウェイクアップする。
     */
    void wake();

    /**
     * スリープ状態かどうかを返す。
     */
    boolean isSleeping();

    // === グループC: 入力 ===

    /**
     * マウスプレスイベントを処理する。
     */
    void mousePressed(int x, int y);

    /**
     * マウスリリースイベントを処理する。
     */
    void mouseReleased(int x, int y);

    /**
     * マウスドラッグイベントを処理する。
     */
    void mouseDragged(int x, int y);

    /**
     * マウスホイールイベントを処理する。
     */
    void mouseWheel(int x, int y, float delta);

    /**
     * キープレスイベントを処理する。
     */
    void keyPressed(char key, int keyCode);

    /**
     * キーリリースイベントを処理する。
     */
    void keyReleased(char key, int keyCode);

    // === グループE: ナビゲーション/UI ===

    /**
     * ホーム画面に戻る。
     */
    void requestGoHome();

    /**
     * スクリーンマネージャーを取得する（存在確認用）。
     *
     * @return スクリーンマネージャー、または利用不可の場合null
     */
    Object getScreenManager();
}
