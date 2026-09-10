package jp.moyashi.phoneos.forge.service;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumService;
import jp.moyashi.phoneos.core.service.chromium.DefaultChromiumService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.TickEvent;
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

    /** 共有カーネルインスタンス（常に稼働） */
    private static Kernel sharedKernel = null;

    /** 現在のワールドID */
    private static String currentWorldId = null;

    /** ホットリスタート保留中フラグ */
    private static volatile boolean pendingRestart = false;

    /** リスタート後にProcessingScreenを再表示するフラグ */
    private static volatile boolean pendingReopenScreen = false;

    /**
     * ワールドロード時の処理。
     * Kernelが既に存在する場合はディメンション移動と判断し、何もしない。
     * Kernelがnullの場合（初回接続 or 切断後の再接続）のみ新規作成する。
     */
    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        // クライアント側のみ処理
        if (event.getLevel().isClientSide()) {
            try {
                // Kernelが既に存在する → ディメンション移動、何もしない
                if (sharedKernel != null) {
                    LOGGER.info("[SmartphoneBackgroundService] World loaded with active kernel, keeping alive (dimension change)");
                    return;
                }

                // Kernelがnull → 初回接続 or 切断後の再接続、新規作成
                Minecraft mc = Minecraft.getInstance();
                String worldName = "unknown";

                if (mc.getSingleplayerServer() != null) {
                    worldName = mc.getSingleplayerServer().getWorldData().getLevelName();
                } else if (mc.getCurrentServer() != null) {
                    worldName = mc.getCurrentServer().name;
                }

                worldName = sanitizeWorldId(worldName);
                currentWorldId = worldName;
                sharedKernel = createKernel(currentWorldId);
                LOGGER.info("[SmartphoneBackgroundService] Kernel created for world: " + currentWorldId);

            } catch (Exception e) {
                LOGGER.error("[SmartphoneBackgroundService] Failed to handle world load", e);
            }
        }
    }

    /**
     * ワールドアンロード時の処理。
     * ディメンション移動でも発火するため、ここではシャットダウンしない。
     * 実際のシャットダウンは onClientTick の mc.level==null 検出で行う。
     */
    @SubscribeEvent
    public static void onWorldUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            LOGGER.info("[SmartphoneBackgroundService] World unloading (no action, tick-based state check handles shutdown)");
        }
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

            // NetworkAdapter用のForgeVirtualSocketを初期化（IPvM over HTTP用）
            LOGGER.info("[SmartphoneBackgroundService] Initializing ForgeNetworkInitializer...");
            jp.moyashi.phoneos.forge.network.ForgeNetworkInitializer.initialize(kernel);
            LOGGER.info("[SmartphoneBackgroundService] ForgeNetworkInitializer initialized");

            // ハードウェアバイパスAPIの初期化（Forge実装に置き換え）
            LOGGER.info("[SmartphoneBackgroundService] Initializing hardware bypass APIs...");
            initializeHardwareAPIs(kernel);
            LOGGER.info("[SmartphoneBackgroundService] Hardware bypass APIs initialized");

            // テクスチャプロバイダーの初期化（mochitexture://スキーム用）
            LOGGER.info("[SmartphoneBackgroundService] Initializing TextureProvider...");
            kernel.setTextureProvider(new ForgeTextureProvider());
            LOGGER.info("[SmartphoneBackgroundService] ForgeTextureProvider initialized");

            // クリップボードプロバイダーの初期化（GLFW実装に置き換え）
            LOGGER.info("[SmartphoneBackgroundService] Initializing clipboard provider...");
            initializeClipboardProvider(kernel);
            LOGGER.info("[SmartphoneBackgroundService] Clipboard provider initialized");

            // MODアプリケーションの同期とプリインストール
            LOGGER.info("[SmartphoneBackgroundService] Syncing MOD applications...");
            syncAndPreinstallModApps(kernel);
            LOGGER.info("[SmartphoneBackgroundService] MOD applications sync complete");

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

            var forgeMicrophoneSocket = new jp.moyashi.phoneos.forge.hardware.ForgeMicrophoneSocket();
            kernel.setMicrophoneSocket(forgeMicrophoneSocket);
            LOGGER.info("[SmartphoneBackgroundService] - MicrophoneSocket set");

            // SpeakerSocket: ForgeSpeakerSocketを使用
            var forgeSpeakerSocket = new jp.moyashi.phoneos.forge.hardware.ForgeSpeakerSocket();
            kernel.setSpeakerSocket(forgeSpeakerSocket);
            LOGGER.info("[SmartphoneBackgroundService] - ForgeSpeakerSocket set");

            // AudioDeviceSocket: オーディオデバイス選択機能
            var audioDeviceSocket = new jp.moyashi.phoneos.forge.hardware.ForgeAudioDeviceSocket();
            kernel.setAudioDeviceSocket(audioDeviceSocket);
            LOGGER.info("[SmartphoneBackgroundService] - ForgeAudioDeviceSocket set");

            // デバイス選択変更時のリスナーを設定
            audioDeviceSocket.setOnMicrophoneChangeListener(() -> {
                var mixerInfo = audioDeviceSocket.getSelectedMicrophoneMixerInfo();
                forgeMicrophoneSocket.setSelectedMixer(mixerInfo);
                LOGGER.info("[SmartphoneBackgroundService] Microphone device changed");
            });
            audioDeviceSocket.setOnSpeakerChangeListener(() -> {
                var mixerInfo = audioDeviceSocket.getSelectedSpeakerMixerInfo();
                forgeSpeakerSocket.setSelectedMixer(mixerInfo);
                LOGGER.info("[SmartphoneBackgroundService] Speaker device changed");
            });

            // 初期デバイス設定を適用
            forgeMicrophoneSocket.setSelectedMixer(audioDeviceSocket.getSelectedMicrophoneMixerInfo());
            forgeSpeakerSocket.setSelectedMixer(audioDeviceSocket.getSelectedSpeakerMixerInfo());

            kernel.setICSocket(new jp.moyashi.phoneos.forge.hardware.ForgeICSocket());
            LOGGER.info("[SmartphoneBackgroundService] - ICSocket set");

            kernel.setSIMInfo(new jp.moyashi.phoneos.forge.hardware.ForgeSIMInfo(player));
            LOGGER.info("[SmartphoneBackgroundService] - SIMInfo set");

            // ChatSocket: Minecraftチャットに通知を送信
            jp.moyashi.phoneos.core.service.NotificationManager notificationManager = kernel.getNotificationManager();
            if (notificationManager != null) {
                notificationManager.setChatSocket(new jp.moyashi.phoneos.forge.hardware.ForgeChatSocket());
                LOGGER.info("[SmartphoneBackgroundService] - ForgeChatSocket set to NotificationManager");
            } else {
                LOGGER.warn("[SmartphoneBackgroundService] - NotificationManager is null, cannot set ForgeChatSocket");
            }

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
     * 共有カーネルを取得する。
     * GUIから呼び出されて、描画とインタラクションに使用される。
     *
     * @return 共有Kernelインスタンス
     */
    public static Kernel getKernel() {
        return sharedKernel;
    }

    /**
     * ハードウェアAPIのプレイヤー情報を更新する。
     * ProcessingScreenから毎フレーム呼び出される。
     */
    public static void updateHardwareAPIs() {
        if (sharedKernel == null) {
            LOGGER.warn("[SmartphoneBackgroundService] updateHardwareAPIs: sharedKernel is null");
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
     * MODアプリケーションの同期とプリインストールを行う。
     *
     * 1. ModAppRegistryからAppLoaderに利用可能なアプリを同期
     * 2. 永続化されたインストール状態からMODアプリを復元
     * 3. MMOSConfigのプリインストールリストに含まれるアプリを自動インストール
     * 4. AppStoreアプリを組み込みアプリとして登録
     *
     * @param kernel Kernelインスタンス
     */
    private static void syncAndPreinstallModApps(jp.moyashi.phoneos.core.Kernel kernel) {
        try {
            // 1. ModAppRegistryからAppLoaderに同期
            kernel.getAppLoader().syncWithModRegistry();
            int availableCount = kernel.getAppLoader().getAvailableModAppsCount();
            LOGGER.info("[SmartphoneBackgroundService] Synced " + availableCount + " MOD apps from registry");

            // 2. 永続化されたインストール状態からMODアプリを復元
            LOGGER.info("[SmartphoneBackgroundService] Restoring previously installed MOD apps...");
            int restoredCount = kernel.getAppLoader().restoreInstalledModApps(kernel);
            LOGGER.info("[SmartphoneBackgroundService] Restored " + restoredCount + " MOD apps from persisted data");

            // 3. プリインストールリストのアプリを自動インストール（まだインストールされていないもののみ）
            java.util.List<String> preinstalledIds = jp.moyashi.phoneos.forge.MMOSConfig.getPreinstalledAppIds();
            LOGGER.info("[SmartphoneBackgroundService] Preinstall list: " + preinstalledIds);

            for (String appId : preinstalledIds) {
                // 既にインストール済み（復元済み含む）ならスキップ
                if (kernel.getAppLoader().isModAppInstalled(appId)) {
                    LOGGER.info("[SmartphoneBackgroundService] Already installed (skipping preinstall): " + appId);
                    continue;
                }

                if (kernel.getAppLoader().getAvailableModApp(appId) != null) {
                    boolean success = kernel.getAppLoader().installModApp(appId, kernel);
                    if (success) {
                        LOGGER.info("[SmartphoneBackgroundService] Preinstalled: " + appId);
                    } else {
                        LOGGER.warn("[SmartphoneBackgroundService] Failed to preinstall: " + appId);
                    }
                } else {
                    LOGGER.warn("[SmartphoneBackgroundService] Preinstall app not found: " + appId);
                }
            }

            // 4. AppStoreアプリを組み込みアプリとして登録
            LOGGER.info("[SmartphoneBackgroundService] Registering AppStore app...");
            kernel.getAppLoader().registerApplication(
                new jp.moyashi.phoneos.core.apps.appstore.AppStoreApp()
            );
            LOGGER.info("[SmartphoneBackgroundService] AppStore app registered");

            int installedCount = kernel.getAppLoader().getInstalledModAppsCount();
            LOGGER.info("[SmartphoneBackgroundService] MOD app sync complete - " +
                       availableCount + " available, " + installedCount + " installed");

            // 5. 現在の画面がHomeScreenの場合、アプリリストをリフレッシュ
            // ログイン済み状態でOS起動した場合、HomeScreenはすでに表示されているため
            // 新しくインストールされたアプリを反映する必要がある
            if (kernel.getScreenManager() != null) {
                jp.moyashi.phoneos.core.ui.Screen currentScreen = kernel.getScreenManager().getCurrentScreen();
                if (currentScreen instanceof jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen) {
                    LOGGER.info("[SmartphoneBackgroundService] Refreshing HomeScreen apps after preinstall...");
                    ((jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen) currentScreen).refreshApps();
                    LOGGER.info("[SmartphoneBackgroundService] HomeScreen apps refreshed");
                }
            }

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Error syncing MOD applications", e);
            e.printStackTrace();
        }
    }

    /**
     * ワールド名をVFS用のIDにサニタイズする。
     * 英数字、ハイフン、アンダースコアのみ許可し、その他の文字はアンダースコアに置換。
     * 空の場合は"unknown"を返す。
     *
     * @param worldName 元のワールド名
     * @return サニタイズされたワールドID
     */
    private static String sanitizeWorldId(String worldName) {
        if (worldName == null || worldName.isEmpty()) {
            return "unknown";
        }

        // スペースをアンダースコアに置換
        String sanitized = worldName.replace(" ", "_");

        // 英数字、ハイフン、アンダースコア以外をアンダースコアに置換
        sanitized = sanitized.replaceAll("[^a-zA-Z0-9_-]", "_");

        // 連続するアンダースコアを1つに圧縮
        sanitized = sanitized.replaceAll("_+", "_");

        // 先頭と末尾のアンダースコアを除去
        sanitized = sanitized.replaceAll("^_+|_+$", "");

        // 空になった場合はフォールバック
        if (sanitized.isEmpty()) {
            return "world_" + System.currentTimeMillis();
        }

        return sanitized;
    }

    /**
     * Kernelをシャットダウンする。
     * ChromiumService（サーフェス破棄、Pumpスレッド停止）を含む
     * 全リソースの正しいクリーンアップを行う。
     */
    private static void shutdownKernel() {
        if (sharedKernel == null) {
            return;
        }

        try {
            LOGGER.info("[SmartphoneBackgroundService] Shutting down kernel (full shutdown)...");

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

            // Kernel.shutdown() を呼んで全リソースを解放
            // ChromiumService → ChromiumManager → ForgeChromiumProvider.shutdown()
            // （ForgeChromiumProviderはCefApp.dispose()を呼ばず、CefAppはJVMライフタイムで存続）
            sharedKernel.shutdown();

            LOGGER.info("[SmartphoneBackgroundService] Kernel shutdown complete");

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Error during kernel shutdown", e);
        }
    }

    /**
     * Kernelを安全にシャットダウンする（ワールド変更時用）。
     * shutdown(true) を使用し、非同期スレッドを起動しない。
     * これにより、新Kernelが作成された後に旧KernelのスレッドがEventBusを無効化する
     * レースコンディションを防止する。
     */
    private static void shutdownKernelSafe() {
        if (sharedKernel == null) {
            return;
        }

        try {
            LOGGER.info("[SmartphoneBackgroundService] Shutting down kernel (safe/hot-restart style)...");

            // マイクを停止
            if (sharedKernel.getMicrophoneSocket() != null && sharedKernel.getMicrophoneSocket().isEnabled()) {
                sharedKernel.getMicrophoneSocket().setEnabled(false);
            }

            // スピーカーを停止
            if (sharedKernel.getSpeakerSocket() != null) {
                sharedKernel.getSpeakerSocket().stopAudio();
            }

            // shutdown(true) で非同期スレッドを起動しない
            sharedKernel.shutdown(true);

            LOGGER.info("[SmartphoneBackgroundService] Kernel safe shutdown complete");

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Error during safe kernel shutdown", e);
        }
    }

    /**
     * Kernelをホットリスタートする。
     * 同じワールドIDで再作成し、VFSデータを引き継ぐ。
     */
    private static void restartKernel() {
        if (sharedKernel == null || currentWorldId == null) return;

        String worldId = currentWorldId;
        LOGGER.info("[SmartphoneBackgroundService] Hot-restarting kernel for world: " + worldId);

        try {
            // マイクを停止
            if (sharedKernel.getMicrophoneSocket() != null && sharedKernel.getMicrophoneSocket().isEnabled()) {
                sharedKernel.getMicrophoneSocket().setEnabled(false);
                LOGGER.info("[SmartphoneBackgroundService] Microphone stopped for restart");
            }

            // スピーカーを停止
            if (sharedKernel.getSpeakerSocket() != null) {
                sharedKernel.getSpeakerSocket().stopAudio();
                LOGGER.info("[SmartphoneBackgroundService] Speaker stopped for restart");
            }

            // ホットリスタート用シャットダウン（画面描画・非同期終了をスキップ）
            sharedKernel.shutdown(true);

            // 同じワールドIDで新しいKernelを作成
            sharedKernel = createKernel(worldId);
            LOGGER.info("[SmartphoneBackgroundService] Kernel hot-restart complete for world: " + worldId);

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Error during kernel hot-restart", e);
        }
    }

    /**
     * クライアントティックイベント。
     * GUIが開いていない時でもKernelのバックグラウンド処理を実行する。
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        // END phaseでのみ処理（1 tickに1回だけ実行）
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // ワールド切断検出: mc.levelがnullならメインメニューに戻っている
        // ディメンション移動中はmc.levelがold→newと遷移しnullにならないため誤検出しない
        if (sharedKernel != null && Minecraft.getInstance().level == null) {
            LOGGER.info("[SmartphoneBackgroundService] No active level detected, shutting down kernel (world disconnect)");
            shutdownKernelSafe();
            sharedKernel = null;
            currentWorldId = null;
        }

        // ホットリスタートリクエスト検出
        if (sharedKernel != null && sharedKernel.isRestartRequested()) {
            pendingRestart = true;
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof jp.moyashi.phoneos.forge.gui.ProcessingScreen) {
                mc.setScreen(null);
                pendingReopenScreen = true;
            }
        }

        // ホットリスタート実行（ProcessingScreen閉じた後のtickで実行）
        if (pendingRestart && sharedKernel != null) {
            pendingRestart = false;
            restartKernel();
            if (pendingReopenScreen) {
                pendingReopenScreen = false;
                Minecraft.getInstance().execute(() -> {
                    Minecraft.getInstance().setScreen(new jp.moyashi.phoneos.forge.gui.ProcessingScreen());
                });
            }
        }

        // Kernelが存在し、GUIが開いていない場合のみバックグラウンド更新
        if (sharedKernel != null) {
            Minecraft mc = Minecraft.getInstance();

            // ProcessingScreen（スマートフォンGUI）が開いていない時のみバックグラウンド更新
            // ProcessingScreenが開いている時はProcessingScreen内でkernel.update()が呼ばれる
            boolean isPhoneScreenOpen = mc.screen != null &&
                mc.screen.getClass().getName().contains("ProcessingScreen");

            if (!isPhoneScreenOpen) {
                try {
                    // ハードウェアAPIのプレイヤー情報を更新
                    // （ディメンション移動でmc.playerが新インスタンスに変わるため）
                    updateHardwareAPIs();

                    // バックグラウンドでKernelを更新（描画なし）
                    sharedKernel.update();
                } catch (Exception e) {
                    // エラーログは頻繁に出力しないように制限
                    if (sharedKernel.frameCount % 600 == 0) {
                        LOGGER.error("[SmartphoneBackgroundService] Background tick error: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * サービスのシャットダウン処理。
     */
    public static void shutdown() {
        LOGGER.info("[SmartphoneBackgroundService] Shutting down service");
        try {
            shutdownKernel();
            sharedKernel = null;
            currentWorldId = null;

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Error during shutdown", e);
        }
    }
}
