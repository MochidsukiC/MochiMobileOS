package jp.moyashi.phoneos.forge.event;

import jp.moyashi.phoneos.core.app.IApplication;
import net.minecraftforge.eventbus.api.Event;

/**
 * ForgeのMODが自身のアプリケーションをMochiMobileOSに登録するためのカスタムイベント。
 *
 * このイベントは、MochiMobileOSがForge環境で初期化された後に発行され、
 * 他のMODが自身のアプリケーションを登録する機会を提供します。
 *
 * 使用方法：
 * 1. イベントをリスンする（@SubscribeEventアノテーション）
 * 2. 自身のIApplicationインスタンスを作成
 * 3. event.registerApp(application)を呼び出してアプリケーションを登録
 *
 * @author jp.moyashi
 * @version 1.0
 * @since 1.0
 */
public class PhoneAppRegistryEvent extends Event {

    /** 登録されたアプリケーションの数 */
    private int registeredAppsCount = 0;

    /**
     * デフォルトコンストラクタ。
     * イベントのバス（FML.MOD_EVENT_BUS）に投稿されることを想定しています。
     */
    public PhoneAppRegistryEvent() {
        super();
    }

    /**
     * アプリケーションをMochiMobileOSに登録します。
     *
     * 登録されたアプリケーションは、AppStoreで「利用可能なアプリ候補」として表示され、
     * ユーザーがインストールを選択できるようになります。
     *
     * @param application 登録するアプリケーション。nullは許可されません。
     * @throws IllegalArgumentException applicationがnullの場合
     */
    public void registerApp(IApplication application) {
        if (application == null) {
            throw new IllegalArgumentException("Application cannot be null");
        }

        // AppLoaderに直接登録するのではなく、一時的に保持する
        // 実際の登録は、MochiMobileOSModが行う
        ModAppRegistry.getInstance().addAvailableApp(application);
        registeredAppsCount++;

        System.out.println("[PhoneAppRegistryEvent] Registered app: " +
                          application.getApplicationName() + " (" +
                          application.getApplicationId() + ")");
    }

    /**
     * このイベントで登録されたアプリケーションの総数を取得します。
     *
     * @return 登録されたアプリケーションの数
     */
    public int getRegisteredAppsCount() {
        return registeredAppsCount;
    }

    /**
     * イベントがキャンセル可能かどうかを返します。
     * この イベントはキャンセルできません。
     *
     * @return 常にfalse
     */
    @Override
    public boolean isCancelable() {
        return false;
    }

    /**
     * このイベントにデフォルト結果があるかどうかを返します。
     *
     * @return 常にfalse（デフォルト結果なし）
     */
    @Override
    public boolean hasResult() {
        return false;
    }
}