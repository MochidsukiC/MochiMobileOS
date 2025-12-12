package jp.moyashi.phoneos.forge.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.platform.NativeImage;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.forge.service.SmartphoneBackgroundService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.joml.Matrix4f;

import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MinecraftのGUIシステム内でProcessingベースのMochiMobileOSを表示するスクリーン。
 * 実際のcoreモジュールのProcessing機能を使用してMochiMobileOSを動作させる。
 *
 * @author jp.moyashi
 * @version 1.0
 * @since 1.0
 */
@OnlyIn(Dist.CLIENT)
public class ProcessingScreen extends Screen {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** スマートフォン画面のサイズ（スタンドアロンと同じ） */
    private static final int PHONE_WIDTH = 400;
    private static final int PHONE_HEIGHT = 600;

    /** 共有カーネル参照 */
    private Kernel kernel;

    /** グラフィック描画フラグ */
    private boolean graphicsEnabled = false;

    /** 描画オフセット */
    private int offsetX, offsetY;

    /** 描画スケール係数 */
    private float scale = 1.0f;

    /** スケール後の描画サイズ */
    private int scaledWidth, scaledHeight;

    /** フレームカウンター */
    private int frameCount = 0;

    /** テクスチャ管理 */
    private DynamicTexture dynamicTexture = null;
    private ResourceLocation textureLocation = null;
    private NativeImage nativeImage = null;

    /** マウスイベントキュー（render()内で処理） */
    private final java.util.concurrent.ConcurrentLinkedQueue<MouseEvent> mouseEventQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();

    /** 最後にPRESSEDした座標（RELEASEDで使用） */
    private volatile int lastPressedX = -1;
    private volatile int lastPressedY = -1;

    /** ホームボタン表示用 */
    private static final int HOME_BUTTON_SIZE = 40;
    private static final int HOME_BUTTON_MARGIN = 10;
    private int homeButtonX, homeButtonY;
    private boolean homeButtonVisible = false;
    private boolean homeButtonHovered = false;

    /** マウスイベントタイプ */
    private enum MouseEventType {
        PRESSED, DRAGGED, RELEASED
    }

    /** マウスイベントデータ */
    private static class MouseEvent {
        final int x;
        final int y;
        final MouseEventType type;

        MouseEvent(int x, int y, MouseEventType type) {
            this.x = x;
            this.y = y;
            this.type = type;
        }
    }

    /**
     * ProcessingScreenのコンストラクタ。
     */
    public ProcessingScreen() {
        super(Component.literal("MochiMobileOS"));
        LOGGER.info("[ProcessingScreen] Creating MochiMobileOS display screen");
    }

    /**
     * 画面の初期化処理。
     * バックグラウンドサービスから共有Kernelを取得し、グラフィック描画を有効化する。
     */
    @Override
    protected void init() {
        super.init();

        try {
            LOGGER.info("[ProcessingScreen] Initializing MochiMobileOS display...");

            // スケール計算：画面サイズに対してスマートフォン全体が収まるように調整
            // マージンを20ピクセル確保
            int availableWidth = this.width - 40;
            int availableHeight = this.height - 40;

            float scaleX = (float) availableWidth / PHONE_WIDTH;
            float scaleY = (float) availableHeight / PHONE_HEIGHT;

            // 小さい方のスケールを採用（アスペクト比維持）
            // 拡大・縮小の両方に対応
            this.scale = Math.min(scaleX, scaleY);

            // スケール後のサイズを計算
            this.scaledWidth = (int) (PHONE_WIDTH * this.scale);
            this.scaledHeight = (int) (PHONE_HEIGHT * this.scale);

            // 画面中央に配置
            this.offsetX = (this.width - this.scaledWidth) / 2;
            this.offsetY = (this.height - this.scaledHeight) / 2;

            LOGGER.info("[ProcessingScreen] Screen size: " + this.width + "x" + this.height +
                ", Phone size: " + PHONE_WIDTH + "x" + PHONE_HEIGHT +
                ", Scale: " + this.scale +
                ", Scaled size: " + this.scaledWidth + "x" + this.scaledHeight +
                ", Offset: (" + offsetX + "," + offsetY + ")");

            // MOD起動時に作成済みの共有Kernelを取得
            this.kernel = SmartphoneBackgroundService.getKernel();

            if (this.kernel != null) {
                LOGGER.info("[ProcessingScreen] Found shared kernel - frameCount: " + this.kernel.frameCount);

                // カーネルがスリープ中の場合はウェイクする
                if (this.kernel.isSleeping()) {
                    LOGGER.info("[ProcessingScreen] Kernel is sleeping, waking up...");
                    this.kernel.wake();
                }

                // ハードウェアAPIのプレイヤー情報を初回更新
                SmartphoneBackgroundService.updateHardwareAPIs();

                // グラフィック描画を有効化
                enableGraphics();
                this.graphicsEnabled = true;

                // テクスチャを初期化
                initializeTexture();
            } else {
                LOGGER.warn("[ProcessingScreen] No kernel found - it should have been created at MOD startup");
                this.graphicsEnabled = false;
            }

        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Failed to initialize display: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * カーネルのグラフィック描画を有効化する。
     * バックグラウンドで動作していたカーネルに対してグラフィック処理を開始させる。
     */
    private void enableGraphics() {
        try {
            LOGGER.info("[ProcessingScreen] Enabling graphics for kernel...");

            if (kernel != null) {
                // グラフィック関連の設定を復元
                kernel.width = 300;
                kernel.height = 450;

                // PGraphicsバッファが適切に初期化されているかチェック
                try {
                    // TODO: PGraphics統一アーキテクチャに移行後、グラフィックバッファチェックを再実装
                    // if (kernel.getGraphicsBuffer() != null) { // 古いAPI
                    //     LOGGER.debug("[ProcessingScreen] PGraphics buffer is available: " +
                    //                 kernel.getGraphicsBuffer().width + "x" + kernel.getGraphicsBuffer().height);
                    // }
                    LOGGER.debug("[ProcessingScreen] PGraphics buffer check temporarily disabled");
                } catch (Exception e) {
                    LOGGER.debug("[ProcessingScreen] PGraphics buffer check skipped: " + e.getMessage());
                }

                LOGGER.info("[ProcessingScreen] Graphics enabled successfully");
            }

        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Failed to enable graphics: " + e.getMessage(), e);
        }
    }

    /**
     * キューに溜まったマウスイベントを全て処理する（render()から呼び出される）。
     * 全ての処理がRender threadで行われるため、スレッドセーフ。
     */
    private void processPendingMouseEvents() {
        if (kernel == null) {
            mouseEventQueue.clear();
            return;
        }

        MouseEvent event;
        while ((event = mouseEventQueue.poll()) != null) {
            try {
                switch (event.type) {
                    case PRESSED:
                        LOGGER.info("[ProcessingScreen] Processing PRESSED at (" + event.x + ", " + event.y +
                                   ") isSleeping=" + kernel.isSleeping() +
                                   ", screenManager=" + (kernel.getScreenManager() != null));
                        kernel.mousePressed(event.x, event.y);
                        break;
                    case DRAGGED:
                        kernel.mouseDragged(event.x, event.y);
                        break;
                    case RELEASED:
                        LOGGER.info("[ProcessingScreen] Processing RELEASED at (" + event.x + ", " + event.y + ")");
                        kernel.mouseReleased(event.x, event.y);
                        break;
                }
            } catch (Exception e) {
                LOGGER.error("[ProcessingScreen] Error processing mouse event: " + e.getMessage(), e);
            }
        }
    }

    /**
     * 画面の描画処理。
     * バックグラウンドで動作するカーネルに対してグラフィック描画を実行し、結果をMinecraftのGUIに描画する。
     */
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 背景を暗くする
        this.renderBackground(guiGraphics);

        // 注: 以前のLWJGL/GLFWによる直接マウスイベント処理は削除されました。
        // マウスイベントはminecraftのイベントハンドラ（mouseClicked, mouseReleased）で処理されます。

        if (!graphicsEnabled || kernel == null) {
            // カーネル接続待ちまたはエラー状態の表示
            drawConnectionMessage(guiGraphics);
            return;
        }

        try {
            // フレームカウンターを更新
            frameCount++;

            // 保留中のマウスイベントを処理（Render threadで実行）
            processPendingMouseEvents();

            // ハードウェアAPIのプレイヤー情報を更新
            SmartphoneBackgroundService.updateHardwareAPIs();

            // MochiMobileOSのグラフィック描画を実行（オンデマンド）
            renderKernelGraphics();

            // MochiMobileOSのピクセルデータをMinecraftのGUIに転送
            renderKernelToMinecraft(guiGraphics);

            // Chromiumブラウザ画面が表示されている場合、ホームボタンを描画
            updateHomeButtonVisibility();
            if (homeButtonVisible) {
                renderHomeButton(guiGraphics, mouseX, mouseY);
            }

            // デバッグのため、スマートフォンフレームを一時的に削除
            // drawSmartphoneFrame(guiGraphics);

        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Render error: " + e.getMessage(), e);
            drawErrorMessage(guiGraphics, e.getMessage());
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    /**
     * カーネルとの接続状態メッセージを描画。
     */
    private void drawConnectionMessage(GuiGraphics guiGraphics) {
        String message;
        if (kernel == null) {
            message = "Connecting to MochiMobileOS background service...";
        } else if (!graphicsEnabled) {
            message = "Enabling graphics for MochiMobileOS...";
        } else {
            message = "MochiMobileOS Ready";
        }

        int textWidth = this.font.width(message);
        guiGraphics.drawString(this.font, message,
            (this.width - textWidth) / 2, this.height / 2, 0xFFFFFFFF);
    }

    /**
     * エラーメッセージを描画。
     */
    private void drawErrorMessage(GuiGraphics guiGraphics, String error) {
        String message = "MochiMobileOS Error: " + error;
        int textWidth = this.font.width(message);
        guiGraphics.drawString(this.font, message,
            (this.width - textWidth) / 2, this.height / 2, 0xFFFF5555);
    }


    /**
     * カーネルのグラフィック描画を実行（60fps - モーション管理のため）。
     * Kernelは60fpsで動作する必要があるため、フレームレート制限なし。
     */
    private void renderKernelGraphics() {
        try {
            if (kernel != null && graphicsEnabled) {
                // Kernelのモーションは60fpsで設計されているため、制限なしで実行
                kernel.update();
                kernel.render();

                // デバッグ用: 60フレームごとにログ出力
                if (kernel.frameCount % 60 == 0) {
                    LOGGER.info("[ProcessingScreen] Kernel rendered at full speed, frame: " + kernel.frameCount);
                }
            } else {
                LOGGER.debug("[ProcessingScreen] Skipping graphics - kernel: " + (kernel != null) + ", graphics: " + graphicsEnabled);
            }
        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Graphics rendering error: " + e.getMessage(), e);
        }
    }

    /**
     * MochiMobileOSカーネルの描画結果をMinecraftのGUIに転送。
     */
    private void renderKernelToMinecraft(GuiGraphics guiGraphics) {
        // GuiGraphicsのマトリックス状態を保存
        guiGraphics.pose().pushPose();

        try {
            // デバッグのため、スマートフォンの黒い背景を削除（コメントアウト）
            // guiGraphics.fill(offsetX, offsetY, offsetX + PHONE_WIDTH, offsetY + PHONE_HEIGHT, 0xFF000000);

            if (kernel != null && graphicsEnabled) {
                // 既存の動作中Kernelから描画結果を取得・表示
                renderKernelTexture(guiGraphics);
            } else {
                renderFallbackRectangle(guiGraphics);
            }

            // デバッグのため、スマートフォンの境界線を一時的に削除
            // guiGraphics.fill(offsetX - 2, offsetY - 2, offsetX + PHONE_WIDTH + 2, offsetY, 0xFFFFFFFF); // 上
            // guiGraphics.fill(offsetX - 2, offsetY + PHONE_HEIGHT, offsetX + PHONE_WIDTH + 2, offsetY + PHONE_HEIGHT + 2, 0xFFFFFFFF); // 下
            // guiGraphics.fill(offsetX - 2, offsetY, offsetX, offsetY + PHONE_HEIGHT, 0xFFFFFFFF); // 左
            // guiGraphics.fill(offsetX + PHONE_WIDTH, offsetY, offsetX + PHONE_WIDTH + 2, offsetY + PHONE_HEIGHT, 0xFFFFFFFF); // 右
        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Failed to render kernel: " + e.getMessage(), e);
            renderFallbackRectangle(guiGraphics);
        } finally {
            // GuiGraphicsのマトリックス状態を復元
            guiGraphics.pose().popPose();
        }
    }

    /**
     * カーネルからピクセルデータを安全に取得する。
     */
    private int[] getKernelPixels() {
        try {
            if (kernel != null) {
                LOGGER.info("[ProcessingScreen] Getting pixels from kernel - frameCount: " + kernel.frameCount);

                // loadPixels()を使わず、バックグラウンドサービスで直接設定されたピクセル配列を使用
                LOGGER.info("[ProcessingScreen] Using pixels directly from background service (skipping loadPixels)");

                // PGraphicsバッファからピクセル配列を取得
                int[] pixels = kernel.getPixels();
                if (pixels != null && pixels.length > 0) {
                    LOGGER.info("[ProcessingScreen] Retrieved " + pixels.length + " pixels, first few pixels: " +
                        Integer.toHexString(pixels[0]) + " " +
                        Integer.toHexString(pixels[Math.min(1, pixels.length - 1)]) + " " +
                        Integer.toHexString(pixels[Math.min(100, pixels.length - 1)]) + " " +
                        Integer.toHexString(pixels[Math.min(1000, pixels.length - 1)]));
                    return pixels.clone(); // コピーを返してスレッドセーフティを確保
                } else {
                    LOGGER.info("[ProcessingScreen] Pixel array is null or empty after loadPixels(), creating default");
                    // ピクセル配列が初期化されていない場合は、デフォルトの色で作成
                    return createDefaultPixelArray();
                }
            } else {
                LOGGER.info("[ProcessingScreen] Player kernel is null");
            }
        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Failed to get pixels: " + e.getMessage(), e);
        }
        return createDefaultPixelArray(); // エラー時はデフォルト配列を返す
    }

    /**
     * デフォルトのピクセル配列を作成する（MochiMobileOSの基本画面）。
     */
    private int[] createDefaultPixelArray() {
        int[] pixels = new int[PHONE_WIDTH * PHONE_HEIGHT];

        // グラデーション背景を生成（青から黒へ）
        for (int y = 0; y < PHONE_HEIGHT; y++) {
            for (int x = 0; x < PHONE_WIDTH; x++) {
                int index = y * PHONE_WIDTH + x;

                // 縦方向のグラデーション
                float ratio = (float) y / PHONE_HEIGHT;
                int blue = (int) (255 * (1.0f - ratio * 0.8f));
                int green = (int) (100 * (1.0f - ratio));

                pixels[index] = 0xFF000000 | (green << 8) | blue;
            }
        }

        LOGGER.info("[ProcessingScreen] Created default pixel array with gradient background");
        return pixels;
    }

    /**
     * ピクセル配列をMinecraftのGUIに描画。
     */
    private void renderPixelsToMinecraft(GuiGraphics guiGraphics, int[] pixels) {
        // GuiGraphicsのマトリックス状態を保存
        guiGraphics.pose().pushPose();
        // デバッグ：ピクセル配列の状態を確認
        LOGGER.info("[ProcessingScreen] Rendering pixels to Minecraft - array length: " + pixels.length +
            ", first pixel: " + Integer.toHexString(pixels[0]) +
            ", middle pixel: " + Integer.toHexString(pixels[pixels.length / 2]));

        LOGGER.info("[ProcessingScreen] Screen size: " + this.width + "x" + this.height +
            ", Offset: (" + offsetX + "," + offsetY + "), Phone size: " + PHONE_WIDTH + "x" + PHONE_HEIGHT);

        // ピクセルデータから色を分析
        int headerColor = 0xFF2E5090; // 予想されるヘッダー色
        int bodyColor = 0xFF64C864;   // 予想される背景色

        // ヘッダー領域（最初の50行）とボディ領域（50行目以降）を確認
        boolean hasHeader = false;
        boolean hasBody = false;

        // ヘッダー領域をチェック（最初の数ピクセル）
        for (int i = 0; i < Math.min(50, pixels.length); i++) {
            int color = pixels[i] & 0x00FFFFFF; // アルファチャンネルを除去
            if (color == (headerColor & 0x00FFFFFF)) hasHeader = true;
        }

        // ボディ領域をチェック（50行目以降のピクセル）
        int bodyStartIndex = 50 * PHONE_WIDTH; // 50行目の開始位置
        for (int i = bodyStartIndex; i < Math.min(bodyStartIndex + 100, pixels.length); i++) {
            int color = pixels[i] & 0x00FFFFFF; // アルファチャンネルを除去
            if (color == (bodyColor & 0x00FFFFFF)) hasBody = true;
        }

        LOGGER.info("[ProcessingScreen] Color analysis - hasHeader: " + hasHeader + ", hasBody: " + hasBody);

        // 実際のMochiMobileOSピクセルデータを効率的に描画
        if (hasHeader && hasBody) {
            LOGGER.info("[ProcessingScreen] Drawing MochiMobileOS with efficient block rendering");

            // 10x10ブロックで効率的に描画
            int blockSize = 10;
            for (int by = 0; by < PHONE_HEIGHT; by += blockSize) {
                for (int bx = 0; bx < PHONE_WIDTH; bx += blockSize) {
                    // ブロック内の代表ピクセルを取得
                    int pixelIndex = by * PHONE_WIDTH + bx;
                    if (pixelIndex < pixels.length) {
                        int color = 0xFF000000 | (pixels[pixelIndex] & 0x00FFFFFF);

                        // ブロックサイズの矩形を描画
                        int endX = Math.min(bx + blockSize, PHONE_WIDTH);
                        int endY = Math.min(by + blockSize, PHONE_HEIGHT);
                        guiGraphics.fill(offsetX + bx, offsetY + by, offsetX + endX, offsetY + endY, color);
                    }
                }
            }

            LOGGER.info("[ProcessingScreen] MochiMobileOS rendered with " + blockSize + "x" + blockSize + " blocks");
        } else {
            // フォールバック：単純な色分け表示
            LOGGER.info("[ProcessingScreen] Using fallback color display");

            // スマートフォンエリア内に描画（黒い背景の上に表示されるように）
            LOGGER.info("[ProcessingScreen] Drawing test rectangles inside phone area at offset (" + offsetX + "," + offsetY + ")");

            // スマートフォン画面内に大きく見やすい矩形を描画
            guiGraphics.fill(offsetX + 20, offsetY + 20, offsetX + 120, offsetY + 120, 0xFFFFFF00); // 黄色
            guiGraphics.fill(offsetX + 140, offsetY + 20, offsetX + 240, offsetY + 120, 0xFFFF00FF); // マゼンタ
            guiGraphics.fill(offsetX + 20, offsetY + 140, offsetX + 120, offsetY + 240, 0xFF00FFFF); // シアン
            guiGraphics.fill(offsetX + 140, offsetY + 140, offsetX + 240, offsetY + 240, 0xFFFF8000); // オレンジ

            // 画面左上のテスト（これは境界線の外に描画されるため見える）
            guiGraphics.fill(0, 0, 100, 100, 0xFFFF0000); // 赤
            guiGraphics.fill(100, 0, 200, 100, 0xFF00FF00); // 緑
            guiGraphics.fill(200, 0, 300, 100, 0xFF0000FF); // 青

            // 中央のテスト
            int centerX = this.width / 2;
            int centerY = this.height / 2;
            LOGGER.info("[ProcessingScreen] Drawing center test at (" + centerX + "," + centerY + ")");
            guiGraphics.fill(centerX - 25, centerY - 25, centerX + 25, centerY + 25, 0xFFFFFFFF); // 白
        }

        // GuiGraphicsのマトリックス状態を復元
        guiGraphics.pose().popPose();
    }

    /**
     * フォールバック矩形を描画。
     */
    private void renderFallbackRectangle(GuiGraphics guiGraphics) {
        try {
            LOGGER.info("[ProcessingScreen] Rendering fallback rectangle at " + offsetX + "," + offsetY + " size " + PHONE_WIDTH + "x" + PHONE_HEIGHT);

            // 動的な色変化でProcessingが動作していることを示す
            int time = frameCount / 10;
            int r = (int) (128 + 127 * Math.sin(time * 0.1));
            int g = (int) (128 + 127 * Math.sin(time * 0.1 + 2));
            int b = (int) (128 + 127 * Math.sin(time * 0.1 + 4));
            int color = 0xFF000000 | (r << 16) | (g << 8) | b;

            // 背景を描画
            guiGraphics.fill(offsetX, offsetY, offsetX + PHONE_WIDTH, offsetY + PHONE_HEIGHT, color);
            LOGGER.debug("[ProcessingScreen] Background filled with color: " + Integer.toHexString(color));

            // 境界線を描画（デバッグ用）
            guiGraphics.fill(offsetX, offsetY, offsetX + PHONE_WIDTH, offsetY + 2, 0xFFFF0000); // 上
            guiGraphics.fill(offsetX, offsetY + PHONE_HEIGHT - 2, offsetX + PHONE_WIDTH, offsetY + PHONE_HEIGHT, 0xFFFF0000); // 下
            guiGraphics.fill(offsetX, offsetY, offsetX + 2, offsetY + PHONE_HEIGHT, 0xFFFF0000); // 左
            guiGraphics.fill(offsetX + PHONE_WIDTH - 2, offsetY, offsetX + PHONE_WIDTH, offsetY + PHONE_HEIGHT, 0xFFFF0000); // 右

            // MochiMobileOSテキストを表示
            String text = (kernel != null && graphicsEnabled) ?
                "MochiMobileOS Running (Background)" : "MochiMobileOS Loading...";
            int textWidth = this.font.width(text);
            guiGraphics.drawString(this.font, text,
                offsetX + (PHONE_WIDTH - textWidth) / 2,
                offsetY + PHONE_HEIGHT / 2 - 10, 0xFFFFFFFF);

            String subText = "Frame: " + frameCount + " Graphics: " + graphicsEnabled;
            int subTextWidth = this.font.width(subText);
            guiGraphics.drawString(this.font, subText,
                offsetX + (PHONE_WIDTH - subTextWidth) / 2,
                offsetY + PHONE_HEIGHT / 2 + 10, 0xFFCCCCCC);

            LOGGER.debug("[ProcessingScreen] Fallback rendering completed");
        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Failed to render fallback: " + e.getMessage(), e);
        }
    }

    /**
     * スマートフォンのフレームを描画。
     */
    private void drawSmartphoneFrame(GuiGraphics guiGraphics) {
        // 外枠
        guiGraphics.fill(offsetX - 10, offsetY - 10,
            offsetX + PHONE_WIDTH + 10, offsetY + PHONE_HEIGHT + 10, 0xFF2A2A2A);

        // 内枠
        guiGraphics.fill(offsetX - 5, offsetY - 5,
            offsetX + PHONE_WIDTH + 5, offsetY + PHONE_HEIGHT + 5, 0xFF000000);
    }

    /**
     * マウスクリック処理。
     * 座標を記録し、ポーリングスレッドで処理する（ゼロ遅延）。
     */
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        LOGGER.info("[ProcessingScreen] mouseClicked: (" + mouseX + ", " + mouseY + "), graphics=" + graphicsEnabled + ", kernel=" + (kernel != null));

        if (!graphicsEnabled || kernel == null) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        // ホームボタンがクリックされた場合、ホームに戻る
        if (isMouseOverHomeButton((int) mouseX, (int) mouseY)) {
            handleHomeButtonClick();
            return true;
        }

        // スマートフォン画面内のクリックかチェック（スケール後のサイズを使用）
        LOGGER.info("[ProcessingScreen] Checking bounds: offset=(" + offsetX + "," + offsetY + "), scaled size=" + scaledWidth + "x" + scaledHeight);
        if (mouseX >= offsetX && mouseX <= offsetX + scaledWidth &&
            mouseY >= offsetY && mouseY <= offsetY + scaledHeight) {

            try {
                // Minecraft座標をMochiMobileOS座標に変換（スケールを考慮）
                int mobileX = (int) ((mouseX - offsetX) / scale);
                int mobileY = (int) ((mouseY - offsetY) / scale);

                LOGGER.info("[ProcessingScreen] Inside bounds, queuing mousePressed(" + mobileX + ", " + mobileY + ")");

                // キューに追加（render()で処理）
                mouseEventQueue.offer(new MouseEvent(mobileX, mobileY, MouseEventType.PRESSED));

                return true;

            } catch (Exception e) {
                LOGGER.error("[ProcessingScreen] Mouse click error: " + e.getMessage(), e);
            }
        } else {
            LOGGER.info("[ProcessingScreen] Outside bounds, ignoring click");
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * マウスリリース処理。
     * 座標を記録し、ポーリングスレッドで処理する（ゼロ遅延）。
     */
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!graphicsEnabled || kernel == null) {
            return super.mouseReleased(mouseX, mouseY, button);
        }

        // スマートフォン画面内のリリースかチェック（スケール後のサイズを使用）
        if (mouseX >= offsetX && mouseX <= offsetX + scaledWidth &&
            mouseY >= offsetY && mouseY <= offsetY + scaledHeight) {

            try {
                // Minecraft座標をMochiMobileOS座標に変換（スケールを考慮）
                int mobileX = (int) ((mouseX - offsetX) / scale);
                int mobileY = (int) ((mouseY - offsetY) / scale);

                // キューに追加（render()で処理）
                mouseEventQueue.offer(new MouseEvent(mobileX, mobileY, MouseEventType.RELEASED));

                return true;

            } catch (Exception e) {
                System.err.println("[ProcessingScreen] Mouse release error: " + e.getMessage());
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    /**
     * マウスドラッグ処理（スワイプ操作）。
     * 座標を記録し、ポーリングスレッドで処理する（ゼロ遅延、キューなし）。
     */
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!graphicsEnabled || kernel == null) {
            return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        }

        // スマートフォン画面内のドラッグかチェック（スケール後のサイズを使用）
        if (mouseX >= offsetX && mouseX <= offsetX + scaledWidth &&
            mouseY >= offsetY && mouseY <= offsetY + scaledHeight) {

            try {
                // Minecraft座標をMochiMobileOS座標に変換（スケールを考慮）
                int mobileX = (int) ((mouseX - offsetX) / scale);
                int mobileY = (int) ((mouseY - offsetY) / scale);

                // キューに追加（render()で処理）
                mouseEventQueue.offer(new MouseEvent(mobileX, mobileY, MouseEventType.DRAGGED));

                return true;

            } catch (Exception e) {
                System.err.println("[ProcessingScreen] Mouse drag error: " + e.getMessage());
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    /**
     * マウスホイール処理（スクロール操作）。
     */
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!graphicsEnabled || kernel == null) {
            return super.mouseScrolled(mouseX, mouseY, delta);
        }

        // スマートフォン画面内のスクロールかチェック（スケール後のサイズを使用）
        if (mouseX >= offsetX && mouseX <= offsetX + scaledWidth &&
            mouseY >= offsetY && mouseY <= offsetY + scaledHeight) {

            try {
                // Minecraft座標をMochiMobileOS座標に変換（スケールを考慮）
                int mobileX = (int) ((mouseX - offsetX) / scale);
                int mobileY = (int) ((mouseY - offsetY) / scale);

                // MochiMobileOSのマウスホイールイベントを送信
                kernel.mouseWheel(mobileX, mobileY, (float) delta);

                return true;

            } catch (Exception e) {
                System.err.println("[ProcessingScreen] Mouse scroll error: " + e.getMessage());
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    /**
     * キーボード入力処理（特殊キー用）。
     */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // ESCキーでスリープしてから画面を閉じる
        if (keyCode == 256) { // ESC key
            LOGGER.info("[ProcessingScreen] ESC pressed, entering sleep mode and closing screen");
            // カーネルをスリープ状態にする
            if (kernel != null) {
                kernel.sleep();
            }
            this.onClose();
            return true;
        }

        // 特殊キー（矢印、Backspace、Delete、Space、Shift、Ctrl等）のみここで処理
        // 通常の文字入力はcharTyped()で処理される
        // Minecraftキーコード: 259=Backspace, 261=Delete, 257=Enter, 262-265=矢印, 268=Home, 269=End, 32=Space
        // 修飾キー: 340=Shift Left, 344=Shift Right, 341=Ctrl Left, 345=Ctrl Right
        boolean isSpecialKey = (keyCode == 259 || keyCode == 261 || keyCode == 257 ||
                               (keyCode >= 262 && keyCode <= 265) || keyCode == 268 || keyCode == 269 || keyCode == 32 ||
                               keyCode == 340 || keyCode == 344 || keyCode == 341 || keyCode == 345);

        // Ctrlが押されている場合、通常文字キーもkeyPressed()に転送（Ctrl+C/V/A等のショートカット用）
        boolean isCtrlPressed = (modifiers & 2) != 0; // GLFW_MOD_CONTROL = 2
        boolean shouldForwardKey = isSpecialKey || isCtrlPressed;

        if (graphicsEnabled && kernel != null && shouldForwardKey) {
            try {
                // Minecraftキーコードを対応するProcessingキーコードに変換
                int processingKeyCode = isSpecialKey ? convertMinecraftKeyCode(keyCode) : keyCode;
                char key = (char) processingKeyCode;
                kernel.keyPressed(key, processingKeyCode);
                LOGGER.info("[ProcessingScreen] Key pressed: " + keyCode + " -> " + processingKeyCode + " (Ctrl: " + isCtrlPressed + ")");
                // 特殊キーとCtrlショートカットはイベントを消費してcharTyped()での二重処理を防ぐ
                return true;
            } catch (Exception e) {
                LOGGER.error("[ProcessingScreen] Key press error: " + e.getMessage(), e);
            }
        } else {
            LOGGER.info("[ProcessingScreen] Skipping normal character in keyPressed() - will be handled by charTyped()");
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /**
     * MinecraftキーコードをProcessingキーコードに変換。
     */
    private int convertMinecraftKeyCode(int minecraftKeyCode) {
        switch (minecraftKeyCode) {
            case 259: return 8;   // Backspace
            case 261: return 127; // Delete
            case 257: return 10;  // Enter
            case 262: return 39;  // Right Arrow
            case 263: return 37;  // Left Arrow
            case 264: return 40;  // Down Arrow
            case 265: return 38;  // Up Arrow
            case 268: return 36;  // Home
            case 269: return 35;  // End
            case 32:  return 32;  // Space
            case 340: return 16;  // Shift Left
            case 344: return 16;  // Shift Right
            case 341: return 17;  // Ctrl Left
            case 345: return 17;  // Ctrl Right
            default:  return minecraftKeyCode;
        }
    }

    /**
     * 文字入力処理（Unicode対応）。
     * IMEを通じて確定した文字がここに渡される。
     */
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        LOGGER.info("[ProcessingScreen] charTyped - char: '" + codePoint + "' (Unicode: " + (int)codePoint + ")");

        // 制御文字とスペース（Enter、Backspace、Delete、Space等）は除外
        // これらはkeyPressed()で既に処理されている
        if (codePoint <= 32 || codePoint == 127) {
            LOGGER.info("[ProcessingScreen] Skipping control character/space in charTyped()");
            return super.charTyped(codePoint, modifiers);
        }

        // 矢印キーとその他の特殊キーのキーコードを除外
        // 35=End, 36=Home, 37=Left, 38=Up, 39=Right, 40=Down
        if ((codePoint >= 35 && codePoint <= 40)) {
            LOGGER.info("[ProcessingScreen] Skipping special key code in charTyped(): " + (int)codePoint);
            return super.charTyped(codePoint, modifiers);
        }

        if (graphicsEnabled && kernel != null) {
            try {
                // Unicode文字をMochiMobileOSに転送
                kernel.keyPressed(codePoint, 0);
                return true;
            } catch (Exception e) {
                LOGGER.error("[ProcessingScreen] Char typed error: " + e.getMessage(), e);
            }
        }

        return super.charTyped(codePoint, modifiers);
    }

    /**
     * キーリリース処理。
     * 修飾キー（Shift、Ctrl等）のリリースイベントをKernelに転送する。
     */
    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        LOGGER.info("[ProcessingScreen] keyReleased - keyCode: " + keyCode);

        if (graphicsEnabled && kernel != null) {
            try {
                // Minecraftキーコードを対応するProcessingキーコードに変換
                int processingKeyCode = convertMinecraftKeyCode(keyCode);
                char key = (char) processingKeyCode;
                kernel.keyReleased(key, processingKeyCode);
                LOGGER.info("[ProcessingScreen] Key released: " + keyCode + " -> " + processingKeyCode);
            } catch (Exception e) {
                LOGGER.error("[ProcessingScreen] Key release error: " + e.getMessage(), e);
            }
        }

        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    /**
     * 画面終了処理。
     * グラフィック描画を無効化し、バックグラウンド処理のみに戻す。
     */
    @Override
    public void onClose() {
        try {
            LOGGER.info("[ProcessingScreen] Closing MochiMobileOS display...");

            // グラフィック描画を無効化（バックグラウンド処理は継続）
            graphicsEnabled = false;
            kernel = null;

            LOGGER.info("[ProcessingScreen] MochiMobileOS display closed, background processing continues");

        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Error during close: " + e.getMessage(), e);
        }

        super.onClose();
    }

    /**
     * 画面がゲームを一時停止するかどうか。
     */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /**
     * 子要素のリストを返す（空のリストを返すことで、全てのマウスイベントが親Screenに届く）。
     */
    @Override
    public java.util.List<? extends net.minecraft.client.gui.components.events.GuiEventListener> children() {
        return java.util.Collections.emptyList();
    }

    /**
     * 次の要素へのフォーカス移動を無効化。
     */
    @Override
    public void setFocused(net.minecraft.client.gui.components.events.GuiEventListener listener) {
        // フォーカス管理を無効化（何もしない）
    }

    /**
     * 初期フォーカスを設定しない。
     */
    @Override
    public net.minecraft.client.gui.components.events.GuiEventListener getFocused() {
        return null;
    }

    /**
     * Kernelを通常の描画モードで実行する。
     * バックグラウンドサービスではなく、GUI表示時に直接実行。
     */
    private void executeKernelDrawing() {
        try {
            if (kernel != null) {
                LOGGER.info("[ProcessingScreen] Executing kernel draw() - frameCount: " + kernel.frameCount);

                // TODO: PGraphics統一アーキテクチャに移行後、描画サイクルを再実装
                // kernel.draw(); // 古いAPI - 削除済み
                LOGGER.info("[ProcessingScreen] Kernel drawing temporarily disabled");

                LOGGER.info("[ProcessingScreen] Kernel draw() completed successfully");
            }
        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Failed to execute kernel drawing: " + e.getMessage(), e);
        }
    }

    /**
     * Kernelの描画結果をテクスチャとしてMinecraft GUIに表示する。
     */
    private void renderKernelTexture(GuiGraphics guiGraphics) {
        try {
            if (kernel != null) {
                // PGraphicsバッファから最新の描画結果を取得
                int[] pixels = kernel.getPixels();

                if (pixels != null && pixels.length > 0) {
                    // テクスチャとして効率的に描画
                    renderTextureFromPixels(guiGraphics, pixels);
                } else {
                    LOGGER.warn("[ProcessingScreen] No pixels available from kernel");
                    renderFallbackRectangle(guiGraphics);
                }
            }
        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Failed to render kernel texture: " + e.getMessage(), e);
            renderFallbackRectangle(guiGraphics);
        }
    }

    /**
     * テクスチャを初期化する。
     */
    private void initializeTexture() {
        try {
            LOGGER.info("[ProcessingScreen] Initializing texture for " + PHONE_WIDTH + "x" + PHONE_HEIGHT);

            // NativeImageを作成（RGBA形式）
            nativeImage = new NativeImage(NativeImage.Format.RGBA, PHONE_WIDTH, PHONE_HEIGHT, false);

            // DynamicTextureを作成
            dynamicTexture = new DynamicTexture(nativeImage);

            // ResourceLocationを登録
            textureLocation = Minecraft.getInstance().getTextureManager()
                .register("mochimobileos_screen", dynamicTexture);

            LOGGER.info("[ProcessingScreen] Texture initialized successfully: " + textureLocation);
        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Failed to initialize texture: " + e.getMessage(), e);
        }
    }

    /**
     * ピクセル配列からテクスチャを効率的に描画する（最適化版）。
     * パフォーマンス改善: ループ内でのARGB→ABGR変換を一括で実行し、
     * setPixelRGBA()の呼び出しオーバーヘッドを削減。
     */
    private void renderTextureFromPixels(GuiGraphics guiGraphics, int[] pixels) {
        if (nativeImage == null || dynamicTexture == null || textureLocation == null) {
            LOGGER.warn("[ProcessingScreen] Texture not initialized, falling back");
            renderFallbackRectangle(guiGraphics);
            return;
        }

        try {
            // 最適化: ARGB→ABGR変換を一括で行い、NativeImageに直接書き込み
            // setPixelRGBA()の繰り返し呼び出しを避ける
            int totalPixels = PHONE_WIDTH * PHONE_HEIGHT;
            for (int i = 0; i < Math.min(totalPixels, pixels.length); i++) {
                int processingColor = pixels[i];

                // ProcessingのARGB形式からMinecraftのABGR形式に変換（ビットシフト最適化）
                int a = processingColor & 0xFF000000;
                int r = (processingColor >> 16) & 0xFF;
                int g = (processingColor >> 8) & 0xFF;
                int b = processingColor & 0xFF;

                // ABGR形式に変換
                int abgrColor = a | (b << 16) | (g << 8) | r;

                // 座標を計算（y * width + x の逆算）
                int x = i % PHONE_WIDTH;
                int y = i / PHONE_WIDTH;

                nativeImage.setPixelRGBA(x, y, abgrColor);
            }

            // テクスチャを更新
            dynamicTexture.upload();

            // PoseStackを使ってスケーリング変換を適用
            guiGraphics.pose().pushPose();

            // 描画位置に移動
            guiGraphics.pose().translate(offsetX, offsetY, 0);

            // スケーリングを適用
            guiGraphics.pose().scale(scale, scale, 1.0f);

            // 元のサイズで描画（スケール変換が適用される）
            guiGraphics.blit(textureLocation, 0, 0, 0, 0, PHONE_WIDTH, PHONE_HEIGHT, PHONE_WIDTH, PHONE_HEIGHT);

            guiGraphics.pose().popPose();

        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Failed to render texture: " + e.getMessage(), e);
            renderFallbackRectangle(guiGraphics);
        }
    }

    /**
     * 画面を閉じる際のクリーンアップ。
     */
    @Override
    public void removed() {
        super.removed();

        cleanupTexture();
    }

    /**
     * テクスチャリソースをクリーンアップする。
     */
    private void cleanupTexture() {
        try {
            if (textureLocation != null) {
                Minecraft.getInstance().getTextureManager().release(textureLocation);
                textureLocation = null;
            }
            if (dynamicTexture != null) {
                dynamicTexture.close();
                dynamicTexture = null;
            }
            if (nativeImage != null) {
                nativeImage.close();
                nativeImage = null;
            }
            LOGGER.info("[ProcessingScreen] Texture cleaned up successfully");
        } catch (Exception e) {
            LOGGER.error("[ProcessingScreen] Failed to cleanup texture: " + e.getMessage(), e);
        }
    }

    /**
     * Chromiumブラウザ画面が表示されているかを確認し、ホームボタンの表示状態を更新する。
     */
    private void updateHomeButtonVisibility() {
        homeButtonVisible = false;
        if (kernel != null && kernel.getScreenManager() != null) {
            jp.moyashi.phoneos.core.ui.Screen currentScreen = kernel.getScreenManager().getCurrentScreen();
            if (currentScreen != null) {
                String screenClassName = currentScreen.getClass().getSimpleName();
                // ChromiumBrowserScreenが表示されている場合のみホームボタンを表示
                homeButtonVisible = screenClassName.contains("ChromiumBrowser");
            }
        }

        // ホームボタンの位置を更新（スマートフォンUIの右下外側）
        if (homeButtonVisible) {
            homeButtonX = offsetX + scaledWidth + HOME_BUTTON_MARGIN;
            homeButtonY = offsetY + scaledHeight - HOME_BUTTON_SIZE - HOME_BUTTON_MARGIN;
        }
    }

    /**
     * ホームボタンを描画する。
     */
    private void renderHomeButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // ホバー状態を更新
        homeButtonHovered = isMouseOverHomeButton(mouseX, mouseY);

        // ボタン背景
        int bgColor = homeButtonHovered ? 0xDD444444 : 0xAA333333;
        guiGraphics.fill(homeButtonX, homeButtonY, homeButtonX + HOME_BUTTON_SIZE, homeButtonY + HOME_BUTTON_SIZE, bgColor);

        // ボタン枠
        int borderColor = homeButtonHovered ? 0xFFFFFFFF : 0xFFAAAAAA;
        guiGraphics.fill(homeButtonX, homeButtonY, homeButtonX + HOME_BUTTON_SIZE, homeButtonY + 1, borderColor); // 上
        guiGraphics.fill(homeButtonX, homeButtonY + HOME_BUTTON_SIZE - 1, homeButtonX + HOME_BUTTON_SIZE, homeButtonY + HOME_BUTTON_SIZE, borderColor); // 下
        guiGraphics.fill(homeButtonX, homeButtonY, homeButtonX + 1, homeButtonY + HOME_BUTTON_SIZE, borderColor); // 左
        guiGraphics.fill(homeButtonX + HOME_BUTTON_SIZE - 1, homeButtonY, homeButtonX + HOME_BUTTON_SIZE, homeButtonY + HOME_BUTTON_SIZE, borderColor); // 右

        // ホームアイコン（家の形を描く）
        int iconColor = homeButtonHovered ? 0xFFFFFFFF : 0xFFCCCCCC;
        int cx = homeButtonX + HOME_BUTTON_SIZE / 2;
        int cy = homeButtonY + HOME_BUTTON_SIZE / 2;

        // 家の屋根（三角形の代わりに台形で表現）
        guiGraphics.fill(cx - 12, cy - 2, cx + 12, cy - 1, iconColor);
        guiGraphics.fill(cx - 10, cy - 4, cx + 10, cy - 3, iconColor);
        guiGraphics.fill(cx - 8, cy - 6, cx + 8, cy - 5, iconColor);
        guiGraphics.fill(cx - 6, cy - 8, cx + 6, cy - 7, iconColor);
        guiGraphics.fill(cx - 4, cy - 10, cx + 4, cy - 9, iconColor);

        // 家の本体
        guiGraphics.fill(cx - 10, cy, cx + 10, cy + 10, iconColor);

        // ドア（中央に開口部）
        int doorColor = bgColor;
        guiGraphics.fill(cx - 3, cy + 3, cx + 3, cy + 10, doorColor);
    }

    /**
     * マウスがホームボタンの上にあるかを判定する。
     */
    private boolean isMouseOverHomeButton(int mouseX, int mouseY) {
        return homeButtonVisible &&
               mouseX >= homeButtonX && mouseX < homeButtonX + HOME_BUTTON_SIZE &&
               mouseY >= homeButtonY && mouseY < homeButtonY + HOME_BUTTON_SIZE;
    }

    /**
     * ホームボタンがクリックされた時の処理。
     */
    private void handleHomeButtonClick() {
        if (kernel != null) {
            LOGGER.info("[ProcessingScreen] Home button clicked - going to home screen");
            // Kernel.handleHomeButton()を呼び出してホーム画面に戻る
            kernel.handleHomeButton();
        }
    }
}