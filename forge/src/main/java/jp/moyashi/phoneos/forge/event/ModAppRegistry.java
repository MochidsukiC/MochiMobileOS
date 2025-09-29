package jp.moyashi.phoneos.forge.event;

import jp.moyashi.phoneos.core.app.IApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Forge環境でのMODアプリケーション登録を管理するレジストリクラス。
 *
 * このクラスは、PhoneAppRegistryEventで登録されたアプリケーションを
 * 一時的に保持し、MochiMobileOSModがAppLoaderに転送するまで管理します。
 *
 * スレッドセーフな実装となっており、複数のMODが同時にアプリケーションを
 * 登録しても安全に動作します。
 *
 * @author jp.moyashi
 * @version 1.0
 * @since 1.0
 */
public class ModAppRegistry {

    /** シングルトンインスタンス */
    private static final ModAppRegistry INSTANCE = new ModAppRegistry();

    /** 利用可能なMODアプリケーションのリスト（スレッドセーフ） */
    private final List<IApplication> availableApps = new CopyOnWriteArrayList<>();

    /** インスタンス化の状態 */
    private boolean initialized = false;

    /**
     * プライベートコンストラクタ（シングルトンパターン）。
     */
    private ModAppRegistry() {
        // シングルトンのため外部からのインスタンス化を防ぐ
    }

    /**
     * ModAppRegistryのシングルトンインスタンスを取得します。
     *
     * @return ModAppRegistryのインスタンス
     */
    public static ModAppRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * 利用可能なアプリケーション候補を追加します。
     *
     * 追加されたアプリケーションは、AppStoreで「インストール可能」として
     * 表示されるようになります。
     *
     * @param application 追加するアプリケーション。nullは許可されません。
     * @throws IllegalArgumentException applicationがnullの場合
     */
    public void addAvailableApp(IApplication application) {
        if (application == null) {
            throw new IllegalArgumentException("Application cannot be null");
        }

        // 重複チェック
        for (IApplication existingApp : availableApps) {
            if (existingApp.getApplicationId().equals(application.getApplicationId())) {
                System.out.println("[ModAppRegistry] Warning: App with ID " +
                                 application.getApplicationId() + " already registered, skipping");
                return;
            }
        }

        availableApps.add(application);
        System.out.println("[ModAppRegistry] Added available app: " +
                          application.getName() + " (Total: " + availableApps.size() + ")");
    }

    /**
     * すべての利用可能なアプリケーション候補のリストを取得します。
     *
     * 返されるリストは読み取り専用です。
     *
     * @return 利用可能なアプリケーションのリスト（読み取り専用）
     */
    public List<IApplication> getAvailableApps() {
        return new ArrayList<>(availableApps);
    }

    /**
     * 指定されたアプリケーションIDの利用可能なアプリケーションを取得します。
     *
     * @param applicationId 検索するアプリケーションID
     * @return 見つかったアプリケーション、存在しない場合はnull
     */
    public IApplication getAvailableApp(String applicationId) {
        if (applicationId == null) {
            return null;
        }

        for (IApplication app : availableApps) {
            if (applicationId.equals(app.getApplicationId())) {
                return app;
            }
        }
        return null;
    }

    /**
     * 利用可能なアプリケーション候補の数を取得します。
     *
     * @return 利用可能なアプリケーション数
     */
    public int getAvailableAppsCount() {
        return availableApps.size();
    }

    /**
     * すべての利用可能なアプリケーション候補をクリアします。
     *
     * 主にテストやリセット目的で使用されます。
     */
    public void clearAvailableApps() {
        int count = availableApps.size();
        availableApps.clear();
        System.out.println("[ModAppRegistry] Cleared " + count + " available apps");
    }

    /**
     * レジストリが初期化済みかどうかを確認します。
     *
     * @return 初期化済みの場合true
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * レジストリを初期化済みとしてマークします。
     *
     * MochiMobileOSModから呼び出されます。
     */
    public void markAsInitialized() {
        this.initialized = true;
        System.out.println("[ModAppRegistry] Marked as initialized with " +
                          availableApps.size() + " available apps");
    }
}