package jp.moyashi.phoneos.forge;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.forge.event.ModAppRegistry;
import jp.moyashi.phoneos.forge.event.PhoneAppRegistryEvent;
import jp.moyashi.phoneos.forge.registry.ModItems;
import jp.moyashi.phoneos.server.MMOSServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * MochiMobileOSのMinecraft Forge統合MODメインクラス。
 *
 * このMODは、Minecraft環境でMochiMobileOSの機能を提供し、
 * 他のMODが独自のアプリケーションを登録できる基盤を構築します。
 *
 * 主な機能：
 * - InterModComms (IMC) を通じた他MODからのアプリケーション登録受付
 * - ModAppRegistryを通じたアプリケーション管理
 *
 * == 外部MODからのアプリ登録方法 ==
 *
 * 1. FMLCommonSetupEventで InterModComms.sendTo() を使用:
 * <pre>{@code
 * @SubscribeEvent
 * public void onCommonSetup(FMLCommonSetupEvent event) {
 *     event.enqueueWork(() -> {
 *         InterModComms.sendTo("mochimobileos", "register_app", () -> new MyApplication());
 *     });
 * }
 * }</pre>
 *
 * 2. IApplicationインターフェースを実装したクラスを作成
 *
 * 注意：このMODは、現在はスタンドアロン版MochiMobileOSとは独立して動作します。
 *
 * @author jp.moyashi
 * @version 1.1
 * @since 1.0
 */
@Mod("mochimobileos")
public class MochiMobileOSMod {

    /** MOD ID */
    public static final String MOD_ID = "mochimobileos";

    /** IMCメソッド名: アプリケーション登録 */
    public static final String IMC_METHOD_REGISTER_APP = "register_app";

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
            // 設定ファイルを登録
            System.out.println("[MochiMobileOSMod] Registering config...");
            ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, MMOSConfig.SPEC);
            System.out.println("[MochiMobileOSMod] Config registered successfully");

            // アイテム登録
            System.out.println("[MochiMobileOSMod] Registering items...");
            ModItems.register(FMLJavaModLoadingContext.get().getModEventBus());
            System.out.println("[MochiMobileOSMod] Items registered successfully");

            // イベントバスに登録
            System.out.println("[MochiMobileOSMod] Adding event listeners...");
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
            System.out.println("[MochiMobileOSMod] Event listeners added successfully");

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

        // 初期化フェーズ1: 仮想ネットワークの初期化
        event.enqueueWork(() -> {
            System.out.println("[MochiMobileOSMod] Registering virtual network packets...");
            jp.moyashi.phoneos.forge.network.NetworkHandler.register();
            System.out.println("[MochiMobileOSMod] Virtual network packets registered");

            // サーバーデータパスを設定（サーバーアプリ用）
            Path serverDataPath = FMLPaths.GAMEDIR.get().resolve("mmos_server_data");
            System.out.println("[MochiMobileOSMod] Setting server data path: " + serverDataPath);
            MMOSServer.setServerDataPath(serverDataPath);

            // サーバーモジュールの初期化（IPvMシステムサーバー登録 + サーバーアプリロード）
            System.out.println("[MochiMobileOSMod] Initializing MMOS Server module...");
            MMOSServer.initialize();
            System.out.println("[MochiMobileOSMod] MMOS Server module initialized");
        });

        // 初期化フェーズ2: ModAppRegistryの準備
        ModAppRegistry registry = ModAppRegistry.getInstance();
        registry.markAsInitialized();

        // 初期化フェーズ3: Kernelの準備チェック
        // 注意: 現在はスタンドアロン版とは独立しているため、
        // Forge環境専用の軽量版Kernelか、アプリ管理のみのサービスを想定
        boolean kernelReady = checkKernelReadiness();

        if (kernelReady) {
            System.out.println("[MochiMobileOSMod] Kernel (or equivalent) is ready");

            // 初期化フェーズ4: アイテム初期化
            ModItems.initialize();

            // 初期化フェーズ5: レガシーPhoneAppRegistryEventの発行（後方互換性のため維持）
            // 注意: このイベントは正しく動作しません（旧方式）
            // 新しい方式はInterModComms (IMC) を使用します
            firePhoneAppRegistryEvent();

            initialized = true;
            System.out.println("[MochiMobileOSMod] MochiMobileOS Forge integration initialized");
            System.out.println("[MochiMobileOSMod] Waiting for IMC messages from other mods...");
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
     * InterModComms (IMC) メッセージを処理します。
     *
     * 他のMODから送信されたアプリケーション登録リクエストを受け取り、
     * ModAppRegistryに登録します。
     *
     * @param event InterModProcessEvent
     */
    @SubscribeEvent
    public void processIMC(InterModProcessEvent event) {
        System.out.println("[MochiMobileOSMod] Processing InterModComms messages...");

        ModAppRegistry registry = ModAppRegistry.getInstance();
        final int[] registeredCount = {0};

        // "register_app" メッセージを処理
        InterModComms.getMessages(MOD_ID, IMC_METHOD_REGISTER_APP::equals)
            .forEach(msg -> {
                try {
                    Object payload = msg.messageSupplier().get();

                    if (payload instanceof IApplication) {
                        IApplication app = (IApplication) payload;
                        registry.addAvailableApp(app);
                        registeredCount[0]++;
                        System.out.println("[MochiMobileOSMod] IMC: Registered app from '" + msg.senderModId() +
                            "': " + app.getName() + " (" + app.getApplicationId() + ")");
                    } else if (payload instanceof Supplier) {
                        // Supplier<IApplication> として処理
                        @SuppressWarnings("unchecked")
                        Supplier<IApplication> supplier = (Supplier<IApplication>) payload;
                        IApplication app = supplier.get();
                        if (app != null) {
                            registry.addAvailableApp(app);
                            registeredCount[0]++;
                            System.out.println("[MochiMobileOSMod] IMC: Registered app from '" + msg.senderModId() +
                                "': " + app.getName() + " (" + app.getApplicationId() + ")");
                        }
                    } else {
                        System.err.println("[MochiMobileOSMod] IMC: Invalid payload type from '" + msg.senderModId() +
                            "': expected IApplication or Supplier<IApplication>, got " +
                            (payload != null ? payload.getClass().getName() : "null"));
                    }
                } catch (Exception e) {
                    System.err.println("[MochiMobileOSMod] IMC: Error processing message from '" + msg.senderModId() +
                        "': " + e.getMessage());
                    e.printStackTrace();
                }
            });

        System.out.println("[MochiMobileOSMod] IMC processing complete: " + registeredCount[0] + " apps registered");

        // 登録されたアプリケーションを処理
        if (registeredCount[0] > 0) {
            processRegisteredApps();
        }
    }

    /**
     * PhoneAppRegistryEventを発行し、他のMODにアプリケーション登録の機会を提供します。
     *
     * @deprecated この方式は正しく動作しません。代わりに InterModComms を使用してください。
     * <pre>{@code
     * InterModComms.sendTo("mochimobileos", "register_app", () -> new MyApplication());
     * }</pre>
     */
    @Deprecated
    private void firePhoneAppRegistryEvent() {
        System.out.println("[MochiMobileOSMod] [DEPRECATED] Firing PhoneAppRegistryEvent (use IMC instead)");

        PhoneAppRegistryEvent event = new PhoneAppRegistryEvent();

        // MODイベントバスに投稿（他のMODからの登録を受け付ける - 実際には動作しない）
        FMLJavaModLoadingContext.get().getModEventBus().post(event);

        if (event.getRegisteredAppsCount() > 0) {
            System.out.println("[MochiMobileOSMod] PhoneAppRegistryEvent: " +
                              event.getRegisteredAppsCount() + " apps registered");
        }
    }

    /**
     * 登録されたアプリケーションを処理し、利用可能なアプリケーションリストに追加します。
     *
     * ModAppRegistryに保持されているアプリケーションを取得し、ログ出力します。
     */
    private void processRegisteredApps() {
        ModAppRegistry registry = ModAppRegistry.getInstance();
        int appCount = registry.getAvailableAppsCount();

        System.out.println("[MochiMobileOSMod] Total registered apps: " + appCount);

        if (appCount > 0) {
            System.out.println("[MochiMobileOSMod] Apps available for installation:");
            registry.getAvailableApps().forEach(app -> {
                System.out.println("  - " + app.getName() +
                                 " (" + app.getApplicationId() + ") v" + app.getVersion());
            });
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