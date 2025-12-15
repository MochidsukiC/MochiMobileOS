package jp.moyashi.phoneos.forge.service;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ipc.KernelProxy;
import jp.moyashi.phoneos.core.service.chromium.ChromiumService;
import jp.moyashi.phoneos.core.service.chromium.DefaultChromiumService;
import jp.moyashi.phoneos.forge.bridge.KernelLocalWrapper;
import jp.moyashi.phoneos.forge.bridge.MMOSBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import processing.core.PGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * スマートフォンのKernelインスタンスを管理するサービス。
 * ワールドロード時にKernelを作成し、ワールドごとに異なるデータを管理する。
 * 描画更新はProcessingScreenのrender()でMinecraftのフレームレートに同期して行われる。
 *
 * @author jp.moyashi
 * @version 4.0
 * @since 1.0
 */
@Mod.EventBusSubscriber(modid = "mochimobileos", value = Dist.CLIENT)
public class SmartphoneBackgroundService {

    private static final Logger LOGGER = LogManager.getLogger();

    /**
     * 外部プロセスモードフラグ。
     * trueの場合、MMOSサーバーを別プロセスで起動し、共有メモリでIPC通信する。
     * HeadlessException回避のために必要（JCEF使用時）。
     * Forge環境ではデフォルトでtrue（java.awt.headless=trueのため）。
     *
     * publicにしてMMOSInstallMixinからアクセス可能にする。
     * 外部プロセスモードではForge側でのJCEFインストールは不要（サーバー側でcoreが管理）。
     */
    public static final boolean EXTERNAL_PROCESS_MODE = true;

    /** 共有カーネルインスタンス（ローカルモード時のみ使用） */
    private static Kernel sharedKernel = null;

    /** MMOSブリッジ（外部プロセスモード時のみ使用） */
    private static MMOSBridge mmosBridge = null;

    /** カーネルプロキシ（ProcessingScreenが使用） */
    private static KernelProxy kernelProxy = null;

    /** 現在のワールドID */
    private static String currentWorldId = null;

    /**
     * ワールドロード時の処理。
     * ワールドIDを取得してKernelを再作成する。
     */
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        // クライアント側のみ処理
        if (event.getLevel().isClientSide()) {
            try {
                ClientLevel level = (ClientLevel) event.getLevel();

                // ワールド名を取得（シングルプレイヤー/マルチプレイヤー対応）
                Minecraft mc = Minecraft.getInstance();
                String worldName = "unknown";

                if (mc.getSingleplayerServer() != null) {
                    // シングルプレイヤー：ワールド名を使用
                    worldName = mc.getSingleplayerServer().getWorldData().getLevelName();
                } else if (mc.getCurrentServer() != null) {
                    // マルチプレイヤー：サーバー名を使用
                    worldName = mc.getCurrentServer().name.replace(" ", "_");
                }

                LOGGER.info("[SmartphoneBackgroundService] World loaded: " + worldName);

                // ワールドIDが変わった場合、または初回ロード時
                if (!worldName.equals(currentWorldId)) {
                    currentWorldId = worldName;

                    // 既存のKernelをシャットダウン
                    shutdownAll();

                    if (EXTERNAL_PROCESS_MODE) {
                        // 外部プロセスモード：MMOSBridgeを使用
                        LOGGER.info("[SmartphoneBackgroundService] Using EXTERNAL PROCESS MODE for world: " + currentWorldId);
                        mmosBridge = new MMOSBridge();
                        if (mmosBridge.initialize(currentWorldId)) {
                            kernelProxy = mmosBridge.getRemoteKernel();
                            LOGGER.info("[SmartphoneBackgroundService] MMOSBridge initialized for world: " + currentWorldId);
                        } else {
                            LOGGER.error("[SmartphoneBackgroundService] Failed to initialize MMOSBridge, falling back to local mode");
                            mmosBridge = null;
                            // フォールバック：ローカルモード
                            sharedKernel = createKernel(currentWorldId);
                            kernelProxy = new KernelLocalWrapper(sharedKernel);
                        }
                    } else {
                        // ローカルモード：直接Kernelを作成
                        LOGGER.info("[SmartphoneBackgroundService] Using LOCAL MODE for world: " + currentWorldId);
                        sharedKernel = createKernel(currentWorldId);
                        kernelProxy = new KernelLocalWrapper(sharedKernel);
                        LOGGER.info("[SmartphoneBackgroundService] Kernel created for world: " + currentWorldId);
                    }
                }

            } catch (Exception e) {
                LOGGER.error("[SmartphoneBackgroundService] Failed to handle world load", e);
            }
        }
    }

    /**
     * ワールドアンロード時の処理。
     * Kernelをクリーンアップする。
     */
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        // クライアント側のみ処理
        if (event.getLevel().isClientSide()) {
            try {
                LOGGER.info("[SmartphoneBackgroundService] World unloading: " + currentWorldId);

                // すべてをシャットダウン
                shutdownAll();
                currentWorldId = null;
                LOGGER.info("[SmartphoneBackgroundService] Shutdown complete");

            } catch (Exception e) {
                LOGGER.error("[SmartphoneBackgroundService] Failed to handle world unload", e);
            }
        }
    }

    /**
     * すべてのリソースをシャットダウンする。
     */
    private static void shutdownAll() {
        // MMOSBridgeをシャットダウン（外部プロセスモード）
        if (mmosBridge != null) {
            LOGGER.info("[SmartphoneBackgroundService] Shutting down MMOSBridge...");
            mmosBridge.shutdown();
            mmosBridge = null;
        }

        // ローカルKernelをシャットダウン
        if (sharedKernel != null) {
            LOGGER.info("[SmartphoneBackgroundService] Shutting down local kernel...");
            shutdownKernel();
            sharedKernel = null;
        }

        kernelProxy = null;
    }

    /**
     * 共有カーネルを作成する（PGraphics統一アーキテクチャ）。
     *
     * @param worldId ワールドID（データ分離用）
     */
    private static Kernel createKernel(String worldId) {
        LOGGER.info("[SmartphoneBackgroundService] Creating shared kernel for world: " + worldId);

        try {
            // Kernelインスタンスを作成（初期化はまだ）
            Kernel kernel = new Kernel();

            // **重要**: ChromiumServiceを初期化前にカーネルへ注入する
            // ForgeChromiumProviderはjcefmavenのbuild()をバイパスし、
            // MMOSInstallerでインストールしたネイティブライブラリを直接使用する
            LOGGER.info("[SmartphoneBackgroundService] Configuring ChromiumService with ForgeChromiumProvider before kernel initialization...");
            ChromiumService chromiumService = new DefaultChromiumService(new jp.moyashi.phoneos.forge.chromium.ForgeChromiumProvider());
            kernel.setChromiumService(chromiumService);

            // Minecraft環境用の初期化メソッドを使用（サイズは400x600: スタンドアロンと同じ）
            // ワールドIDを渡してデータを分離
            kernel.initializeForMinecraft(400, 600, worldId);

            // 仮想ネットワークルーターの初期化
            LOGGER.info("[SmartphoneBackgroundService] Initializing virtual network router...");
            jp.moyashi.phoneos.forge.network.NetworkHandler.setupVirtualRouter(kernel);
            jp.moyashi.phoneos.forge.network.SystemPacketHandler.registerHandlers(kernel);
            LOGGER.info("[SmartphoneBackgroundService] Virtual network router initialized");

            // ハードウェアバイパスAPIの初期化（Forge実装に置き換え）
            LOGGER.info("[SmartphoneBackgroundService] Initializing hardware bypass APIs...");
            initializeHardwareAPIs(kernel);
            LOGGER.info("[SmartphoneBackgroundService] Hardware bypass APIs initialized");

            // クリップボードプロバイダーの初期化（GLFW実装に置き換え）
            LOGGER.info("[SmartphoneBackgroundService] Initializing clipboard provider...");
            initializeClipboardProvider(kernel);
            LOGGER.info("[SmartphoneBackgroundService] Clipboard provider initialized");

            LOGGER.info("[SmartphoneBackgroundService] Shared kernel created successfully for world: " + worldId);
            return kernel;

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Failed to create shared kernel", e);
            throw new RuntimeException("Failed to create smartphone kernel", e);
        }
    }

    /**
     * ハードウェアバイパスAPIを初期化する。
     * デフォルト実装をForge実装に置き換える。
     *
     * @param kernel Kernelインスタンス
     */
    private static void initializeHardwareAPIs(Kernel kernel) {
        try {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.world.entity.player.Player player = mc.player;

            LOGGER.info("[SmartphoneBackgroundService] Initializing hardware APIs - Player: " + (player != null ? player.getName().getString() : "null"));

            // プレイヤーがいない場合でもForge実装を作成（後で更新される）
            if (player == null) {
                LOGGER.warn("[SmartphoneBackgroundService] Player not available yet, creating Forge implementations without player");
            }

            // 各ハードウェアAPIをForge実装に置き換え（playerがnullでも作成）
            kernel.setMobileDataSocket(new jp.moyashi.phoneos.forge.hardware.ForgeMobileDataSocket(player));
            LOGGER.info("[SmartphoneBackgroundService] - MobileDataSocket set");

            kernel.setBluetoothSocket(new jp.moyashi.phoneos.forge.hardware.ForgeBluetoothSocket(player));
            LOGGER.info("[SmartphoneBackgroundService] - BluetoothSocket set");

            kernel.setLocationSocket(new jp.moyashi.phoneos.forge.hardware.ForgeLocationSocket(player));
            LOGGER.info("[SmartphoneBackgroundService] - LocationSocket set");

            kernel.setBatteryInfo(new jp.moyashi.phoneos.forge.hardware.ForgeBatteryInfo(null));
            LOGGER.info("[SmartphoneBackgroundService] - BatteryInfo set");

            kernel.setCameraSocket(new jp.moyashi.phoneos.forge.hardware.ForgeCameraSocket());
            LOGGER.info("[SmartphoneBackgroundService] - CameraSocket set");

            kernel.setMicrophoneSocket(new jp.moyashi.phoneos.forge.hardware.ForgeMicrophoneSocket());
            LOGGER.info("[SmartphoneBackgroundService] - MicrophoneSocket set");

            // SpeakerSocket: SVCが利用可能な場合のみForgeSpeakerSocketを使用
            if (jp.moyashi.phoneos.forge.hardware.SVCDetector.isSVCAvailable()) {
                kernel.setSpeakerSocket(new jp.moyashi.phoneos.forge.hardware.ForgeSpeakerSocket());
                LOGGER.info("[SmartphoneBackgroundService] - ForgeSpeakerSocket set (SVC available)");
            } else {
                kernel.setSpeakerSocket(new jp.moyashi.phoneos.core.service.hardware.DefaultSpeakerSocket());
                LOGGER.info("[SmartphoneBackgroundService] - DefaultSpeakerSocket set (SVC not available)");
            }

            kernel.setICSocket(new jp.moyashi.phoneos.forge.hardware.ForgeICSocket());
            LOGGER.info("[SmartphoneBackgroundService] - ICSocket set");

            kernel.setSIMInfo(new jp.moyashi.phoneos.forge.hardware.ForgeSIMInfo(player));
            LOGGER.info("[SmartphoneBackgroundService] - SIMInfo set");

            LOGGER.info("[SmartphoneBackgroundService] All hardware APIs initialized with Forge implementations");

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Failed to initialize hardware APIs", e);
            e.printStackTrace();
        }
    }

    /**
     * クリップボードプロバイダーを初期化する。
     * デフォルトのAWTクリップボードをGLFWクリップボードに置き換える。
     *
     * @param kernel Kernelインスタンス
     */
    private static void initializeClipboardProvider(Kernel kernel) {
        try {
            // ClipboardManagerを取得
            jp.moyashi.phoneos.core.service.clipboard.ClipboardManager clipboardManager = kernel.getClipboardManager();

            if (clipboardManager instanceof jp.moyashi.phoneos.core.service.clipboard.ClipboardManagerImpl) {
                // GLFWクリップボードプロバイダーを作成して設定
                jp.moyashi.phoneos.core.service.clipboard.ClipboardProvider glfwProvider =
                    new jp.moyashi.phoneos.forge.clipboard.GLFWClipboardProvider(kernel.getLogger());

                ((jp.moyashi.phoneos.core.service.clipboard.ClipboardManagerImpl) clipboardManager).setProvider(glfwProvider);

                LOGGER.info("[SmartphoneBackgroundService] GLFWClipboardProvider set (available: " + glfwProvider.isAvailable() + ")");
            } else {
                LOGGER.warn("[SmartphoneBackgroundService] ClipboardManager is not ClipboardManagerImpl, cannot set GLFW provider");
            }

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Failed to initialize clipboard provider", e);
            e.printStackTrace();
        }
    }

    /**
     * カーネルプロキシを取得する。
     * GUIから呼び出されて、描画とインタラクションに使用される。
     * ローカルモードではKernelLocalWrapper、外部プロセスモードではRemoteKernelが返される。
     *
     * @return KernelProxyインスタンス
     */
    public static KernelProxy getKernelProxy() {
        return kernelProxy;
    }

    /**
     * 実際のKernelインスタンスを取得する（ローカルモードのみ）。
     * ハードウェアAPI更新など、Kernel固有の操作に使用。
     *
     * @return Kernelインスタンス（外部プロセスモードではnull）
     */
    public static Kernel getKernel() {
        return sharedKernel;
    }

    /**
     * ハードウェアAPIのプレイヤー情報を更新する。
     * ProcessingScreenから毎フレーム呼び出される。
     * 注: 外部プロセスモードではハードウェアAPIはサーバー側で管理されるため、この処理はスキップされる。
     */
    public static void updateHardwareAPIs() {
        // 外部プロセスモードではローカルのハードウェアAPIは使用しない
        if (sharedKernel == null) {
            // 外部プロセスモードまたは未初期化の場合はスキップ（警告は出さない）
            return;
        }

        try {
            Minecraft mc = Minecraft.getInstance();
            net.minecraft.world.entity.player.Player player = mc.player;

            if (player == null) {
                LOGGER.warn("[SmartphoneBackgroundService] updateHardwareAPIs: player is null");
                return;
            }

            // 各ハードウェアAPIのプレイヤー情報を更新
            if (sharedKernel.getMobileDataSocket() instanceof jp.moyashi.phoneos.forge.hardware.ForgeMobileDataSocket) {
                ((jp.moyashi.phoneos.forge.hardware.ForgeMobileDataSocket) sharedKernel.getMobileDataSocket()).updatePlayer(player);
            }

            if (sharedKernel.getBluetoothSocket() instanceof jp.moyashi.phoneos.forge.hardware.ForgeBluetoothSocket) {
                ((jp.moyashi.phoneos.forge.hardware.ForgeBluetoothSocket) sharedKernel.getBluetoothSocket()).updatePlayer(player);
            }

            if (sharedKernel.getLocationSocket() instanceof jp.moyashi.phoneos.forge.hardware.ForgeLocationSocket) {
                ((jp.moyashi.phoneos.forge.hardware.ForgeLocationSocket) sharedKernel.getLocationSocket()).updatePlayer(player);
            }

            if (sharedKernel.getSIMInfo() instanceof jp.moyashi.phoneos.forge.hardware.ForgeSIMInfo) {
                ((jp.moyashi.phoneos.forge.hardware.ForgeSIMInfo) sharedKernel.getSIMInfo()).updatePlayer(player);
            }

            // 天候情報をセンサーマネージャーに設定
            jp.moyashi.phoneos.core.service.sensor.SensorManager sensorManager = sharedKernel.getSensorManager();
            if (sensorManager != null) {
                ClientLevel level = mc.level;
                if (level != null) {
                    boolean isRaining = level.isRaining();
                    boolean isThundering = level.isThundering();

                    // 湿度センサーを更新
                    float humidity = isRaining ? 100.0f : 40.0f;
                    sensorManager.setSimulatedSensorValues(jp.moyashi.phoneos.core.service.sensor.Sensor.TYPE_RELATIVE_HUMIDITY, new float[]{humidity});

                    // 光センサーを更新（雷の場合は暗くする）
                    float lightLevel = isThundering ? 5.0f : 15.0f;
                    sensorManager.setSimulatedSensorValues(jp.moyashi.phoneos.core.service.sensor.Sensor.TYPE_LIGHT, new float[]{lightLevel});
                }
            }

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Failed to update hardware APIs", e);
            e.printStackTrace();
        }
    }

    /**
     * Kernelをシャットダウンする。
     */
    private static void shutdownKernel() {
        if (sharedKernel == null) {
            return;
        }

        try {
            LOGGER.info("[SmartphoneBackgroundService] Shutting down kernel...");

            // マイクを停止
            if (sharedKernel.getMicrophoneSocket() != null && sharedKernel.getMicrophoneSocket().isEnabled()) {
                sharedKernel.getMicrophoneSocket().setEnabled(false);
                LOGGER.info("[SmartphoneBackgroundService] Microphone stopped");
            }

            // スピーカーを停止
            if (sharedKernel.getSpeakerSocket() != null) {
                sharedKernel.getSpeakerSocket().stopAudio();
                LOGGER.info("[SmartphoneBackgroundService] Speaker stopped");
            }

            // その他のリソースクリーンアップ
            // TODO: 必要に応じて他のハードウェアAPIのクリーンアップを追加

            LOGGER.info("[SmartphoneBackgroundService] Kernel shutdown complete");

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Error during kernel shutdown", e);
        }
    }

    /**
     * サービスのシャットダウン処理。
     */
    public static void shutdown() {
        LOGGER.info("[SmartphoneBackgroundService] Shutting down service");
        try {
            shutdownAll();
            currentWorldId = null;

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Error during shutdown", e);
        }
    }

    /**
     * 外部プロセスモードかどうかを返す。
     */
    public static boolean isExternalProcessMode() {
        return EXTERNAL_PROCESS_MODE;
    }

    /**
     * MMOSBridgeを取得する（外部プロセスモードのみ）。
     */
    public static MMOSBridge getMMOSBridge() {
        return mmosBridge;
    }
}
