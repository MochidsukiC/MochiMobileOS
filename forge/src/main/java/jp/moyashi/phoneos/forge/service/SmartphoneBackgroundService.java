package jp.moyashi.phoneos.forge.service;

import jp.moyashi.phoneos.core.Kernel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import processing.core.PApplet;
import processing.core.PGraphics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * スマートフォンのバックグラウンド処理を管理するサービス。
 * プレイヤーのサーバー参加時にスマートフォンを初期化し、
 * PGraphicsバッファからMinecraftテクスチャに変換する新アーキテクチャ。
 *
 * @author jp.moyashi
 * @version 2.0
 * @since 1.0
 */
@Mod.EventBusSubscriber(modid = "mochimobileos", value = Dist.CLIENT)
public class SmartphoneBackgroundService {

    private static final Logger LOGGER = LogManager.getLogger();

    /** プレイヤーごとのスマートフォンカーネル */
    private static final Map<UUID, Kernel> playerKernels = new HashMap<>();

    /** プレイヤーごとのMinecraftテクスチャ */
    private static final Map<UUID, DynamicTexture> playerTextures = new HashMap<>();

    /** バックグラウンド処理用エグゼキューター */
    private static final ScheduledExecutorService backgroundExecutor =
        Executors.newScheduledThreadPool(2);

    /** バックグラウンド処理の実行間隔（ミリ秒） */
    private static final long BACKGROUND_TICK_INTERVAL = 100; // 10fps相当

    /** PAppletインスタンス（リソース作成用） */
    private static PApplet resourceApplet;

    /**
     * プレイヤーがサーバーに参加した際の処理（クライアントサイド）。
     * スマートフォンカーネルを初期化し、バックグラウンド処理を開始する。
     */
    @SubscribeEvent
    public static void onPlayerLogin(ClientPlayerNetworkEvent.LoggingIn event) {
        LocalPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        UUID playerId = player.getUUID();
        LOGGER.info("[SmartphoneBackgroundService] Player joined: {}, initializing smartphone", player.getName().getString());

        try {
            // リソース用PAppletを初期化（初回のみ）
            if (resourceApplet == null) {
                initializeResourceApplet();
            }

            // プレイヤー用のスマートフォンカーネルを作成
            Kernel kernel = createBackgroundKernel(playerId);
            playerKernels.put(playerId, kernel);

            // Minecraftテクスチャを作成
            DynamicTexture texture = createSmartphoneTexture(kernel);
            playerTextures.put(playerId, texture);

            // バックグラウンド処理を開始
            startBackgroundProcessing(playerId, kernel);

            LOGGER.info("[SmartphoneBackgroundService] Smartphone initialized for player: {}", player.getName().getString());

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Failed to initialize smartphone for player: {}",
                player.getName().getString(), e);
        }
    }

    /**
     * プレイヤーがサーバーから退出した際の処理（クライアントサイド）。
     * スマートフォンカーネルをクリーンアップする。
     */
    @SubscribeEvent
    public static void onPlayerLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        LocalPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        UUID playerId = player.getUUID();
        LOGGER.info("[SmartphoneBackgroundService] Player left: {}, cleaning up smartphone", player.getName().getString());

        try {
            Kernel kernel = playerKernels.remove(playerId);
            DynamicTexture texture = playerTextures.remove(playerId);

            if (kernel != null) {
                cleanupKernel(kernel);
                LOGGER.info("[SmartphoneBackgroundService] Smartphone cleaned up for player: {}", player.getName().getString());
            }

            if (texture != null) {
                texture.close();
                LOGGER.info("[SmartphoneBackgroundService] Texture cleaned up for player: {}", player.getName().getString());
            }
        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Failed to cleanup smartphone for player: {}",
                player.getName().getString(), e);
        }
    }

    /**
     * リソース用のPAppletを初期化する。
     */
    private static void initializeResourceApplet() {
        LOGGER.info("[SmartphoneBackgroundService] Initializing resource PApplet");
        resourceApplet = new PApplet() {
            @Override
            public void settings() {
                size(1, 1); // 最小サイズ
            }

            @Override
            public void setup() {
                // 何もしない
            }

            @Override
            public void draw() {
                // 何もしない
            }
        };

        // ヘッドレスモードで初期化（直接setupとdrawを呼び出し）
        resourceApplet.settings();
        resourceApplet.setup();
        LOGGER.info("[SmartphoneBackgroundService] Resource PApplet initialized");
    }

    /**
     * カーネルを作成する（新しいPGraphicsベースアーキテクチャ）。
     */
    private static Kernel createBackgroundKernel(UUID playerId) {
        LOGGER.info("[SmartphoneBackgroundService] Creating kernel for player: {}", playerId);

        try {
            // Kernelインスタンスを作成（新しいAPI）
            Kernel kernel = new Kernel();

            // Kernelを初期化（サイズ指定版）
            kernel.initialize(resourceApplet, 400, 600);

            LOGGER.info("[SmartphoneBackgroundService] Kernel created successfully for player: {}", playerId);
            return kernel;

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Failed to create kernel for player: {}", playerId, e);
            throw new RuntimeException("Failed to create smartphone kernel", e);
        }
    }

    /**
     * PGraphicsからMinecraftテクスチャを作成する。
     */
    private static DynamicTexture createSmartphoneTexture(Kernel kernel) {
        try {
            PGraphics graphics = kernel.getGraphics();
            if (graphics == null) {
                throw new RuntimeException("Kernel graphics buffer is null");
            }

            int[] size = kernel.getScreenSize();
            int width = size[0];
            int height = size[1];

            // NativeImageを作成
            NativeImage nativeImage = new NativeImage(width, height, false);

            // DynamicTextureを作成
            DynamicTexture texture = new DynamicTexture(nativeImage);

            LOGGER.info("[SmartphoneBackgroundService] Created texture {}x{} for smartphone", width, height);
            return texture;

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Failed to create smartphone texture", e);
            throw new RuntimeException("Failed to create smartphone texture", e);
        }
    }

    /**
     * PGraphicsのピクセルデータをMinecraftテクスチャに転送する。
     */
    public static void updateTextureFromKernel(UUID playerId) {
        Kernel kernel = playerKernels.get(playerId);
        DynamicTexture texture = playerTextures.get(playerId);

        if (kernel == null || texture == null) {
            return;
        }

        try {
            // カーネルの描画を実行
            kernel.draw();

            PGraphics graphics = kernel.getGraphics();
            if (graphics == null) {
                return;
            }

            // ピクセルデータを取得
            graphics.loadPixels();
            int[] pixels = graphics.pixels;

            if (pixels == null || pixels.length == 0) {
                return;
            }

            // NativeImageにピクセルデータをコピー
            NativeImage nativeImage = texture.getPixels();
            if (nativeImage != null) {
                int width = nativeImage.getWidth();
                int height = nativeImage.getHeight();

                for (int y = 0; y < height && y < graphics.height; y++) {
                    for (int x = 0; x < width && x < graphics.width; x++) {
                        int pixelIndex = y * graphics.width + x;
                        if (pixelIndex < pixels.length) {
                            int pixel = pixels[pixelIndex];

                            // ProcessingのARGB形式からMinecraftのABGR形式に変換
                            int a = (pixel >> 24) & 0xFF;
                            int r = (pixel >> 16) & 0xFF;
                            int g = (pixel >> 8) & 0xFF;
                            int b = pixel & 0xFF;

                            // ABGRフォーマットでセット
                            int abgr = (a << 24) | (b << 16) | (g << 8) | r;
                            nativeImage.setPixelRGBA(x, y, abgr);
                        }
                    }
                }

                // テクスチャを更新
                texture.upload();
            }

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Failed to update texture for player: {}", playerId, e);
        }
    }

    /**
     * プレイヤーのテクスチャを取得する。
     */
    public static DynamicTexture getPlayerTexture(UUID playerId) {
        return playerTextures.get(playerId);
    }

    /**
     * プレイヤーのカーネルを取得する。
     */
    public static Kernel getPlayerKernel(UUID playerId) {
        return playerKernels.get(playerId);
    }

    /**
     * バックグラウンド処理を開始する。
     */
    private static void startBackgroundProcessing(UUID playerId, Kernel kernel) {
        LOGGER.info("[SmartphoneBackgroundService] Starting background processing for player: {}", playerId);

        // 定期的にテクスチャを更新
        backgroundExecutor.scheduleAtFixedRate(() -> {
            try {
                updateTextureFromKernel(playerId);
            } catch (Exception e) {
                LOGGER.error("[SmartphoneBackgroundService] Background processing error for player: {}", playerId, e);
            }
        }, 0, BACKGROUND_TICK_INTERVAL, TimeUnit.MILLISECONDS);
    }

    /**
     * カーネルをクリーンアップする。
     */
    private static void cleanupKernel(Kernel kernel) {
        try {
            // 必要に応じてクリーンアップ処理を追加
            LOGGER.info("[SmartphoneBackgroundService] Kernel cleanup completed");
        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Error during kernel cleanup", e);
        }
    }

    /**
     * マウスイベントをカーネルに転送する。
     */
    public static void forwardMouseEvent(UUID playerId, String eventType, int mouseX, int mouseY) {
        Kernel kernel = playerKernels.get(playerId);
        if (kernel == null) {
            return;
        }

        try {
            switch (eventType) {
                case "pressed":
                    kernel.mousePressed(mouseX, mouseY);
                    break;
                case "dragged":
                    kernel.mouseDragged(mouseX, mouseY);
                    break;
                case "released":
                    kernel.mouseReleased(mouseX, mouseY);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Error forwarding mouse event for player: {}", playerId, e);
        }
    }

    /**
     * キーイベントをカーネルに転送する。
     */
    public static void forwardKeyEvent(UUID playerId, String eventType, char key, int keyCode, int mouseX, int mouseY) {
        Kernel kernel = playerKernels.get(playerId);
        if (kernel == null) {
            return;
        }

        try {
            switch (eventType) {
                case "pressed":
                    kernel.keyPressed(key, keyCode, mouseX, mouseY);
                    break;
                case "released":
                    kernel.keyReleased(key, keyCode);
                    break;
            }
        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Error forwarding key event for player: {}", playerId, e);
        }
    }

    /**
     * サービスのシャットダウン処理。
     */
    public static void shutdown() {
        LOGGER.info("[SmartphoneBackgroundService] Shutting down service");
        try {
            // 全てのテクスチャをクリーンアップ
            for (DynamicTexture texture : playerTextures.values()) {
                if (texture != null) {
                    texture.close();
                }
            }
            playerTextures.clear();

            // 全てのカーネルをクリーンアップ
            for (Kernel kernel : playerKernels.values()) {
                if (kernel != null) {
                    cleanupKernel(kernel);
                }
            }
            playerKernels.clear();

            // エグゼキューターを停止
            backgroundExecutor.shutdown();
            if (!backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                backgroundExecutor.shutdownNow();
            }

        } catch (Exception e) {
            LOGGER.error("[SmartphoneBackgroundService] Error during shutdown", e);
        }
    }
}