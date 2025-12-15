package jp.moyashi.phoneos.api;

/**
 * MODアプリケーションインターフェース。
 * 外部MODはこのインターフェースを実装してアプリを作成する。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public interface IModApplication {

    /**
     * アプリケーションIDを取得する。
     * 一意である必要がある（例: "com.example.myapp"）。
     *
     * @return アプリケーションID
     */
    String getAppId();

    /**
     * アプリケーション名を取得する。
     * ホーム画面やアプリ一覧に表示される。
     *
     * @return アプリケーション名
     */
    String getName();

    /**
     * アプリケーションの説明を取得する。
     *
     * @return 説明文
     */
    default String getDescription() { return ""; }

    /**
     * アプリケーションのバージョンを取得する。
     *
     * @return バージョン文字列
     */
    default String getVersion() { return "1.0.0"; }

    /**
     * アプリケーション作者を取得する。
     *
     * @return 作者名
     */
    default String getAuthor() { return "Unknown"; }

    /**
     * アプリアイコンのリソースパスを取得する。
     * MODのリソースフォルダからの相対パス。
     *
     * @return アイコンパス（例: "textures/icon.png"）、nullの場合はデフォルトアイコン
     */
    default String getIconPath() { return null; }

    /**
     * アプリケーションが初期化された時に呼ばれる。
     * アプリ起動前の初期化処理をここで行う。
     *
     * @param context アプリケーションコンテキスト
     */
    default void onInitialize(AppContext context) {}

    /**
     * エントリ画面を取得する。
     * アプリ起動時に最初に表示される画面。
     *
     * @param context アプリケーションコンテキスト
     * @return エントリ画面
     */
    ModScreen getEntryScreen(AppContext context);

    /**
     * アプリケーションが終了する時に呼ばれる。
     * リソースの解放をここで行う。
     */
    default void onTerminate() {}

    /**
     * バックグラウンドに移行する時に呼ばれる。
     */
    default void onPause() {}

    /**
     * フォアグラウンドに復帰する時に呼ばれる。
     */
    default void onResume() {}
}
