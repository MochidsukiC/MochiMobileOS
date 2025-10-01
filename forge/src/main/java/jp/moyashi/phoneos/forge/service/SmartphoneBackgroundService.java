package jp.moyashi.phoneos.forge.service;

import jp.moyashi.phoneos.core.Kernel;
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

    /** 共有カーネルインスタンス（常に稼働） */
    private static Kernel sharedKernel = null;

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
                    if (sharedKernel != null) {
                        LOGGER.info("[SmartphoneBackgroundService] Shutting down previous kernel...");
                    }

                    // 新しいワールドID用のKernelを作成
                    sharedKernel = createKernel(currentWorldId);
                    LOGGER.info("[SmartphoneBackgroundService] Kernel created for world: " + currentWorldId);
                }

            } catch (Exception e) {
                LOGGER.error("[SmartphoneBackgroundService] Failed to handle world load", e);
            }
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
            // Kernelインスタンスを作成
            Kernel kernel = new Kernel();

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

            kernel.setSpeakerSocket(new jp.moyashi.phoneos.forge.hardware.ForgeSpeakerSocket());
            LOGGER.info("[SmartphoneBackgroundService] - SpeakerSocket set");

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

            // デバッグ: 各APIの型を確認
            LOGGER.info("[SmartphoneBackgroundService] Updating hardware APIs for player: " + player.getName().getString());
            LOGGER.info("[SmartphoneBackgroundService] - MobileDataSocket type: " + sharedKernel.getMobileDataSocket().getClass().getName());
            LOGGER.info("[SmartphoneBackgroundService] - BluetoothSocket type: " + sharedKernel.getBluetoothSocket().getClass().getName());
            LOGGER.info("[SmartphoneBackgroundService] - LocationSocket type: " + sharedKernel.getLocationSocket().getClass().getName());
            LOGGER.info("[SmartphoneBackgroundService] - SIMInfo type: " + sharedKernel.getSIMInfo().getClass().getName());

            // 各ハードウェアAPIのプレイヤー情報を更新
            if (sharedKernel.getMobileDataSocket() instanceof jp.moyashi.phoneos.forge.hardware.ForgeMobileDataSocket) {
                ((jp.moyashi.phoneos.forge.hardware.ForgeMobileDataSocket) sharedKernel.getMobileDataSocket()).updatePlayer(player);
                LOGGER.info("[SmartphoneBackgroundService] - MobileDataSocket updated");
            }

            if (sharedKernel.getBluetoothSocket() instanceof jp.moyashi.phoneos.forge.hardware.ForgeBluetoothSocket) {
                ((jp.moyashi.phoneos.forge.hardware.ForgeBluetoothSocket) sharedKernel.getBluetoothSocket()).updatePlayer(player);
                LOGGER.info("[SmartphoneBackgroundService] - BluetoothSocket updated");
            }

            if (sharedKernel.getLocationSocket() instanceof jp.moyashi.phoneos.forge.hardware.ForgeLocationSocket) {
                ((jp.moyashi.phoneos.forge.hardware.ForgeLocationSocket) sharedKernel.getLocationSocket()).updatePlayer(player);
                LOGGER.info("[SmartphoneBackgroundService] - LocationSocket updated");
            }

            if (sharedKernel.getSIMInfo() instanceof jp.moyashi.phoneos.forge.hardware.ForgeSIMInfo) {
                ((jp.moyashi.phoneos.forge.hardware.ForgeSIMInfo) sharedKernel.getSIMInfo()).updatePlayer(player);
                LOGGER.info("[SmartphoneBackgroundService] - SIMInfo updated");
            }

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Failed to update hardware APIs", e);
            e.printStackTrace();
        }
    }

    /**
     * サービスのシャットダウン処理。
     */
    public static void shutdown() {
        LOGGER.info("[SmartphoneBackgroundService] Shutting down service");
        try {
            // カーネルのクリーンアップ
            sharedKernel = null;

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Error during shutdown", e);
        }
    }
}