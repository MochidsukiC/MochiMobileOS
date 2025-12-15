package jp.moyashi.phoneos.api;

import processing.core.PGraphics;

/**
 * MODアプリケーション用画面インターフェース。
 * 外部MODアプリケーションはこのインターフェースを実装して画面を作成する。
 *
 * Processing (PGraphics) を使用した描画を行う。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public interface ModScreen {

    /**
     * 画面が作成された時に呼ばれる。
     * 初期化処理をここで行う。
     *
     * @param context アプリケーションコンテキスト
     */
    default void onCreate(AppContext context) {}

    /**
     * 画面が表示される時に呼ばれる。
     */
    default void onResume() {}

    /**
     * 画面が非表示になる時に呼ばれる。
     */
    default void onPause() {}

    /**
     * 画面が破棄される時に呼ばれる。
     * リソースの解放をここで行う。
     */
    default void onDestroy() {}

    /**
     * 画面を描画する。
     * 毎フレーム呼び出される。
     *
     * @param g Processing Graphics コンテキスト
     */
    void render(PGraphics g);

    /**
     * タッチ開始イベント。
     *
     * @param x X座標
     * @param y Y座標
     * @return イベントを消費した場合true
     */
    default boolean onTouchStart(int x, int y) { return false; }

    /**
     * タッチ移動イベント。
     *
     * @param x X座標
     * @param y Y座標
     * @return イベントを消費した場合true
     */
    default boolean onTouchMove(int x, int y) { return false; }

    /**
     * タッチ終了イベント。
     *
     * @param x X座標
     * @param y Y座標
     * @return イベントを消費した場合true
     */
    default boolean onTouchEnd(int x, int y) { return false; }

    /**
     * スクロールイベント。
     *
     * @param x X座標
     * @param y Y座標
     * @param delta スクロール量
     * @return イベントを消費した場合true
     */
    default boolean onScroll(int x, int y, float delta) { return false; }

    /**
     * キー入力イベント。
     *
     * @param keyCode キーコード
     * @param keyChar キー文字
     * @return イベントを消費した場合true
     */
    default boolean onKeyTyped(int keyCode, char keyChar) { return false; }

    /**
     * 戻るボタンが押された時の処理。
     *
     * @return イベントを消費した場合true（trueを返すと画面遷移しない）
     */
    default boolean onBackPressed() { return false; }

    /**
     * 画面のタイトルを取得する。
     *
     * @return 画面タイトル
     */
    default String getTitle() { return ""; }
}
