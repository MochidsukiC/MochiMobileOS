package jp.moyashi.phoneos.core.dashboard;

import jp.moyashi.phoneos.core.Kernel;
import processing.core.PGraphics;

import java.util.Map;

/**
 * ダッシュボードウィジェットのインターフェース。
 * iPhoneのウィジェットと同様に、情報表示型とインタラクティブ型の2種類をサポート。
 *
 * <h2>情報表示型（DISPLAY）</h2>
 * <ul>
 *   <li>情報を表示するのみ</li>
 *   <li>タップするとアプリケーションが開く</li>
 *   <li>getTargetApplicationId()でアプリIDを指定</li>
 *   <li>getLaunchArguments()で起動引数を指定可能</li>
 * </ul>
 *
 * <h2>インタラクティブ型（INTERACTIVE）</h2>
 * <ul>
 *   <li>ウィジェット内でインタラクション可能</li>
 *   <li>ボタン、テキストボックス等のUI要素を配置可能</li>
 *   <li>onTouch()でタッチイベントを処理</li>
 *   <li>onKeyEvent()でキー入力を処理</li>
 * </ul>
 */
public interface IDashboardWidget {

    // === 識別 ===

    /**
     * ウィジェットの一意なIDを取得する。
     * 形式: "提供元アプリID.ウィジェット名" 推奨
     * 例: "com.example.myapp.weather", "system.clock"
     *
     * @return ウィジェットID
     */
    String getId();

    /**
     * ウィジェットの表示名を取得する（設定画面用）。
     *
     * @return 表示名
     */
    String getDisplayName();

    /**
     * ウィジェットの説明文を取得する。
     *
     * @return 説明文
     */
    default String getDescription() {
        return "";
    }

    /**
     * ウィジェットを提供するアプリケーションのIDを取得する。
     * システムウィジェットの場合はnullを返す。
     *
     * @return アプリケーションID、システムの場合はnull
     */
    default String getOwnerApplicationId() {
        return null;
    }

    // === サイズ・タイプ ===

    /**
     * ウィジェットのサイズを取得する。
     *
     * @return サイズ（FULL_WIDTH または HALF_WIDTH）
     */
    DashboardWidgetSize getSize();

    /**
     * ウィジェットのタイプを取得する。
     *
     * @return タイプ（DISPLAY または INTERACTIVE）
     */
    DashboardWidgetType getType();

    // === 描画 ===

    /**
     * ウィジェットを描画する。
     *
     * @param g 描画先のPGraphics
     * @param x 描画開始X座標
     * @param y 描画開始Y座標
     * @param w 描画幅
     * @param h 描画高さ
     */
    void draw(PGraphics g, float x, float y, float w, float h);

    // === 情報表示型用 ===

    /**
     * タップ時に開くアプリケーションのIDを取得する。
     * DISPLAY型で使用。nullの場合は何も起きない。
     *
     * @return アプリケーションID、または null
     */
    default String getTargetApplicationId() {
        return null;
    }

    /**
     * アプリケーション起動時の引数を取得する。
     * DISPLAY型で使用。
     *
     * @return 引数のマップ、または null
     */
    default Map<String, Object> getLaunchArguments() {
        return null;
    }

    // === インタラクティブ型用 ===

    /**
     * タッチイベントを処理する。
     * INTERACTIVE型で使用。座標はウィジェット内のローカル座標。
     *
     * @param localX ウィジェット内X座標
     * @param localY ウィジェット内Y座標
     * @param action アクション（0: DOWN, 1: UP, 2: MOVE, 3: TAP）
     * @return イベントを消費した場合true
     */
    default boolean onTouch(float localX, float localY, int action) {
        return false;
    }

    /**
     * キーイベントを処理する。
     * INTERACTIVE型で使用。
     *
     * @param keyCode キーコード
     * @param key キー文字
     * @return イベントを消費した場合true
     */
    default boolean onKeyEvent(int keyCode, char key) {
        return false;
    }

    // === ライフサイクル ===

    /**
     * ウィジェットがスロットに配置された時に呼ばれる。
     * Kernelへの参照を保持し、必要な初期化を行う。
     *
     * @param kernel カーネルインスタンス
     */
    default void onAttach(Kernel kernel) {}

    /**
     * ウィジェットがスロットから外された時に呼ばれる。
     * リソースの解放等を行う。
     */
    default void onDetach() {}

    /**
     * 定期的な更新時に呼ばれる。
     * データの更新等を行う。
     */
    default void onUpdate() {}

    // === 表示制御 ===

    /**
     * ウィジェットが表示可能かどうかを取得する。
     *
     * @return 表示可能な場合true
     */
    default boolean isVisible() {
        return true;
    }

    // === タッチアクション定数 ===

    /** タッチダウンアクション */
    int ACTION_DOWN = 0;
    /** タッチアップアクション */
    int ACTION_UP = 1;
    /** タッチムーブアクション */
    int ACTION_MOVE = 2;
    /** タップアクション（ダウン→アップの短いタッチ） */
    int ACTION_TAP = 3;
}
