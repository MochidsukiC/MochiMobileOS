package jp.moyashi.phoneos.forge;

import jp.moyashi.phoneos.forge.event.ModAppRegistry;
import jp.moyashi.phoneos.forge.event.PhoneAppRegistryEvent;
import jp.moyashi.phoneos.forge.registry.ModItems;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * MochiMobileOSのMinecraft Forge統合MODメインクラス。
 *
 * このMODは、Minecraft環境でMochiMobileOSの機能を提供し、
 * 他のMODが独自のアプリケーションを登録できる基盤を構築します。
 *
 * 主な機能：
 * - MOD初期化時のPhoneAppRegistryEventの発行
 * - 他のMODからのアプリケーション登録受付
 * - ModAppRegistryを通じたアプリケーション管理
 *
 * 注意：このMODは、現在はスタンドアロン版MochiMobileOSとは独立して動作します。
 * 将来的には、統合されたクロスプラットフォーム動作が実装される予定です。
 *
 * @author jp.moyashi
 * @version 1.0
 * @since 1.0
 */
@Mod("mochimobileos")
public class MochiMobileOSMod {

    /** MODの初期化状態 */
    private static boolean initialized = false;

    /** MochiMobileOSKernelのインスタンス（将来的にはForge環境用のKernelAdapterになる可能性） */
    private static Object kernelInstance = null;

    /**
     * MODのコンストラクタ。
     * Forgeによって自動的に呼び出されます。
     */
    public MochiMobileOSMod() {
        System.out.println("[MochiMobileOSMod] ==================== CONSTRUCTOR START ====================");
        System.out.println("[MochiMobileOSMod] Initializing MochiMobileOS Forge Integration");

        try {
            // アイテム登録
            System.out.println("[MochiMobileOSMod] Registering items...");
            ModItems.register(FMLJavaModLoadingContext.get().getModEventBus());
            System.out.println("[MochiMobileOSMod] Items registered successfully");

            // イベントバスに登録
            System.out.println("[MochiMobileOSMod] Adding event listener...");
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
            System.out.println("[MochiMobileOSMod] Event listener added successfully");

        } catch (Exception e) {
            System.err.println("[MochiMobileOSMod] ERROR in constructor: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("[MochiMobileOSMod] ==================== CONSTRUCTOR END ====================");
    }

    /**
     * 共通セットアップイベントハンドラー。
     *
     * FMLCommonSetupEvent発生時に呼び出され、MODの初期化処理を行います。
     * PhoneAppRegistryEventを発行して、他のMODにアプリケーション登録の機会を提供します。
     *
     * @param event FMLCommonSetupEvent
     */
    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {
        System.out.println("[MochiMobileOSMod] Starting common setup...");

        // 初期化フェーズ1: ModAppRegistryの準備
        ModAppRegistry registry = ModAppRegistry.getInstance();
        registry.markAsInitialized();

        // 初期化フェーズ2: Kernelの準備チェック
        // 注意: 現在はスタンドアロン版とは独立しているため、
        // Forge環境専用の軽量版Kernelか、アプリ管理のみのサービスを想定
        boolean kernelReady = checkKernelReadiness();

        if (kernelReady) {
            System.out.println("[MochiMobileOSMod] Kernel (or equivalent) is ready");

            // 初期化フェーズ3: アイテム初期化
            ModItems.initialize();

            // 初期化フェーズ4: PhoneAppRegistryEventの発行
            firePhoneAppRegistryEvent();

            // 初期化フェーズ5: 登録されたアプリケーションの処理
            processRegisteredApps();

            initialized = true;
            System.out.println("[MochiMobileOSMod] MochiMobileOS Forge integration initialized successfully");
            System.out.println("[MochiMobileOSMod] " + registry.getAvailableAppsCount() + " apps available for registration");
        } else {
            System.out.println("[MochiMobileOSMod] Warning: Kernel not ready, deferring app registration");
        }
    }

    /**
     * Kernelまたは同等機能の準備状態をチェックします。
     *
     * 現在の実装では、Forge環境用の軽量版サービス（AppManagerService等）
     * の存在をチェックするか、単純にtrueを返します。
     *
     * 将来的には、より詳細なKernel準備状態の確認が実装されます。
     *
     * @return Kernelが準備できている場合true
     */
    private boolean checkKernelReadiness() {
        // TODO: 将来的にはForge環境用のKernelAdapterまたはAppManagerServiceの存在をチェック
        // 現在は、MOD環境では常に「準備完了」として扱う
        System.out.println("[MochiMobileOSMod] Checking kernel readiness for Forge environment");
        return true;
    }

    /**
     * PhoneAppRegistryEventを発行し、他のMODにアプリケーション登録の機会を提供します。
     *
     * このイベントを受信した他のMODは、event.registerApp(application)を呼び出して
     * 自身のアプリケーションをMochiMobileOSに登録できます。
     */
    private void firePhoneAppRegistryEvent() {
        System.out.println("[MochiMobileOSMod] Firing PhoneAppRegistryEvent for mod app registration");

        PhoneAppRegistryEvent event = new PhoneAppRegistryEvent();

        // MODイベントバスに投稿（他のMODからの登録を受け付ける）
        FMLJavaModLoadingContext.get().getModEventBus().post(event);

        System.out.println("[MochiMobileOSMod] PhoneAppRegistryEvent fired, " +
                          event.getRegisteredAppsCount() + " apps registered during event");
    }

    /**
     * 登録されたアプリケーションを処理し、利用可能なアプリケーションリストに追加します。
     *
     * ModAppRegistryに保持されているアプリケーションを取得し、
     * 将来的にはAppLoaderに転送またはForge環境用のアプリ管理サービスに登録します。
     */
    private void processRegisteredApps() {
        ModAppRegistry registry = ModAppRegistry.getInstance();
        int appCount = registry.getAvailableAppsCount();

        System.out.println("[MochiMobileOSMod] Processing " + appCount + " registered apps");

        if (appCount > 0) {
            // TODO: 将来的には、ここでスタンドアロン版のAppLoaderとの連携、
            // またはForge環境専用のアプリケーション管理システムとの統合を行う

            System.out.println("[MochiMobileOSMod] Apps available for installation:");
            registry.getAvailableApps().forEach(app -> {
                System.out.println("  - " + app.getName() +
                                 " (" + app.getApplicationId() + ") v" + app.getVersion());
            });
        } else {
            System.out.println("[MochiMobileOSMod] No apps were registered during initialization");
        }
    }

    /**
     * MODが初期化済みかどうかを確認します。
     *
     * @return 初期化済みの場合true
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 現在のKernelインスタンス（または同等機能）を取得します。
     *
     * @return Kernelインスタンス、利用できない場合はnull
     */
    public static Object getKernelInstance() {
        return kernelInstance;
    }

    /**
     * ModAppRegistryのインスタンスを取得します。
     *
     * 他のMODや外部システムがアプリケーションリストにアクセスする際に使用されます。
     *
     * @return ModAppRegistryのインスタンス
     */
    public static ModAppRegistry getAppRegistry() {
        return ModAppRegistry.getInstance();
    }
}