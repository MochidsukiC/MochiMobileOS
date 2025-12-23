package jp.moyashi.phoneos.core.apps.appstore;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.apps.appstore.ui.AppStoreScreen;
import processing.core.PImage;

/**
 * MochiMobileOS用のAppStoreアプリケーション。
 * 外部MODから登録されたアプリケーションのインストール・管理機能を提供する。
 *
 * 主な機能：
 * - 利用可能なMODアプリ一覧の表示
 * - アプリのインストール/アンインストール
 * - インストール済みアプリの管理
 *
 * @author jp.moyashi
 * @version 1.0
 * @since 1.0
 */
public class AppStoreApp implements IApplication {

    /** アプリケーションメタデータ */
    private static final String APP_ID = "jp.moyashi.phoneos.core.apps.appstore";
    private static final String APP_NAME = "App Store";
    private static final String APP_VERSION = "1.0.0";
    private static final String APP_DESCRIPTION = "Install and manage MOD applications";

    /** 初期化状態 */
    private boolean isInitialized = false;

    /**
     * 新しいAppStoreアプリケーションインスタンスを作成する。
     */
    public AppStoreApp() {
        System.out.println("AppStoreApp: App Store application created");
    }

    /**
     * このアプリケーションの一意識別子を取得する。
     *
     * @return アプリケーションID
     */
    @Override
    public String getApplicationId() {
        return APP_ID;
    }

    /**
     * このアプリケーションの表示名を取得する。
     *
     * @return アプリケーション名
     */
    @Override
    public String getName() {
        return APP_NAME;
    }

    /**
     * このアプリケーションのバージョンを取得する。
     *
     * @return アプリケーションバージョン
     */
    @Override
    public String getVersion() {
        return APP_VERSION;
    }

    /**
     * このアプリケーションの説明を取得する。
     *
     * @return アプリケーション説明
     */
    @Override
    public String getDescription() {
        return APP_DESCRIPTION;
    }

    /**
     * このアプリケーションのエントリースクリーンを取得する。
     *
     * @param kernel OSカーネルインスタンス
     * @return AppStoreのメインスクリーン
     */
    @Override
    public Screen getEntryScreen(Kernel kernel) {
        System.out.println("AppStoreApp: Creating App Store screen");
        return new AppStoreScreen(kernel);
    }

    /**
     * AppStoreアプリケーションを初期化する。
     * アプリケーションが最初に読み込まれるときに呼び出される。
     *
     * @param kernel OSカーネルインスタンス
     */
    @Override
    public void onInitialize(Kernel kernel) {
        if (!isInitialized) {
            isInitialized = true;
            System.out.println("AppStoreApp: App Store application initialized");
        }
    }

    /**
     * AppStoreアプリケーションをクリーンアップする。
     * アプリケーションがアンロードされるときに呼び出される。
     */
    @Override
    public void onDestroy() {
        if (isInitialized) {
            isInitialized = false;
            System.out.println("AppStoreApp: App Store application destroyed");
        }
    }

    /**
     * アプリケーションが初期化されているかどうかを確認する。
     *
     * @return 初期化されている場合true、そうでなければfalse
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}
