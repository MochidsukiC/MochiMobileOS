package jp.moyashi.phoneos.forge.gui;

import jp.moyashi.phoneos.forge.installer.MMOSInstallListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MMOSインストーラーの待機画面。
 * JCEFのダウンロード・インストール中に表示される。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class MMOSInstallerScreen extends Screen {

    private static final Logger LOGGER = LogManager.getLogger("MMOSInstallerScreen");

    /** 背後の画面（インストール完了後に遷移） */
    private final Screen parentScreen;

    /** デバッグログ出力間隔用カウンター */
    private int logCounter = 0;

    /** マーキーアニメーション用カウンター */
    private float marqueePosition = 0;

    /** マーキーの幅（プログレスバーの何割か） */
    private static final float MARQUEE_WIDTH_RATIO = 0.3f;

    /** マーキーの速度 */
    private static final float MARQUEE_SPEED = 0.02f;

    /** プログレスバーの幅 */
    private static final int PROGRESS_BAR_WIDTH = 200;

    /** プログレスバーの高さ */
    private static final int PROGRESS_BAR_HEIGHT = 14;

    /** プログレスバーの背景色 */
    private static final int PROGRESS_BG_COLOR = 0xFF333333;

    /** プログレスバーの枠色 */
    private static final int PROGRESS_BORDER_COLOR = 0xFF666666;

    /** プログレスバーの塗りつぶし色 */
    private static final int PROGRESS_FILL_COLOR = 0xFF00AA00;

    /** エラー時のプログレスバー色 */
    private static final int PROGRESS_ERROR_COLOR = 0xFFAA0000;

    /**
     * インストーラー画面を構築する。
     *
     * @param parentScreen 背後の画面
     */
    public MMOSInstallerScreen(Screen parentScreen) {
        super(Component.literal("MMOS is installing required libraries..."));
        this.parentScreen = parentScreen;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void tick() {
        super.tick();

        // インストール完了または失敗したら背後の画面に遷移
        MMOSInstallListener listener = MMOSInstallListener.INSTANCE;
        if (listener.isDone() || listener.isFailed()) {
            onClose();
        }
    }

    @Override
    public void onClose() {
        // 背後の画面に遷移
        Minecraft.getInstance().setScreen(parentScreen);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 背景を描画
        renderBackground(graphics);

        MMOSInstallListener listener = MMOSInstallListener.INSTANCE;

        // デバッグログ（60フレームごと = 約1秒）
        logCounter++;
        if (logCounter >= 60) {
            logCounter = 0;
            LOGGER.info("[MMOSInstallerScreen] UI読み取り - progress={}, task={}, done={}, failed={}",
                listener.getProgress(), listener.getTask(), listener.isDone(), listener.isFailed());
        }

        // 画面中央座標
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // タイトルを描画
        String title = "MMOS is installing required libraries...";
        graphics.drawCenteredString(this.font, title, centerX, centerY - 40, 0xFFFFFF);

        // プログレスバーを描画
        int barX = centerX - PROGRESS_BAR_WIDTH / 2;
        int barY = centerY - PROGRESS_BAR_HEIGHT / 2;

        // 背景
        graphics.fill(barX, barY, barX + PROGRESS_BAR_WIDTH, barY + PROGRESS_BAR_HEIGHT, PROGRESS_BG_COLOR);

        // 枠
        graphics.fill(barX, barY, barX + PROGRESS_BAR_WIDTH, barY + 1, PROGRESS_BORDER_COLOR); // 上
        graphics.fill(barX, barY + PROGRESS_BAR_HEIGHT - 1, barX + PROGRESS_BAR_WIDTH, barY + PROGRESS_BAR_HEIGHT, PROGRESS_BORDER_COLOR); // 下
        graphics.fill(barX, barY, barX + 1, barY + PROGRESS_BAR_HEIGHT, PROGRESS_BORDER_COLOR); // 左
        graphics.fill(barX + PROGRESS_BAR_WIDTH - 1, barY, barX + PROGRESS_BAR_WIDTH, barY + PROGRESS_BAR_HEIGHT, PROGRESS_BORDER_COLOR); // 右

        // 進捗
        float progress = listener.getProgress();
        String task = listener.getTask();
        int fillColor = listener.isFailed() ? PROGRESS_ERROR_COLOR : PROGRESS_FILL_COLOR;

        // 展開フェーズ（進捗が報告されないフェーズ）かどうかを判定
        boolean isIndeterminatePhase = task != null && (
            task.contains("Extracting") ||
            task.contains("Initializing") ||
            task.contains("Installing")
        ) && progress < 0.01f;

        if (isIndeterminatePhase) {
            // マーキーアニメーション（不確定プログレスバー）
            marqueePosition += MARQUEE_SPEED;
            if (marqueePosition > 1.0f + MARQUEE_WIDTH_RATIO) {
                marqueePosition = -MARQUEE_WIDTH_RATIO;
            }

            int barInnerWidth = PROGRESS_BAR_WIDTH - 4;
            int marqueeWidth = (int) (barInnerWidth * MARQUEE_WIDTH_RATIO);
            int marqueeStart = (int) (barInnerWidth * marqueePosition);
            int marqueeEnd = marqueeStart + marqueeWidth;

            // クリッピング（バー内に収める）
            int drawStart = Math.max(0, marqueeStart);
            int drawEnd = Math.min(barInnerWidth, marqueeEnd);

            if (drawEnd > drawStart) {
                graphics.fill(barX + 2 + drawStart, barY + 2,
                    barX + 2 + drawEnd, barY + PROGRESS_BAR_HEIGHT - 2, fillColor);
            }
        } else {
            // 通常のプログレスバー
            int fillWidth = (int) ((PROGRESS_BAR_WIDTH - 4) * progress);
            graphics.fill(barX + 2, barY + 2, barX + 2 + fillWidth, barY + PROGRESS_BAR_HEIGHT - 2, fillColor);
        }

        // タスク名を描画
        if (task != null && !task.isEmpty()) {
            graphics.drawCenteredString(this.font, task, centerX, centerY + 20, 0xCCCCCC);
        }

        // パーセンテージを描画（不確定フェーズでは「Please wait...」を表示）
        String statusText = isIndeterminatePhase ? "Please wait..." : (listener.getProgressPercent() + "%");
        graphics.drawCenteredString(this.font, statusText, centerX, centerY + 35, 0xAAAAAA);

        // エラーメッセージを描画
        if (listener.isFailed()) {
            String errorMessage = listener.getErrorMessage();
            if (errorMessage != null) {
                graphics.drawCenteredString(this.font, "Error: " + errorMessage, centerX, centerY + 55, 0xFF5555);
            }
            graphics.drawCenteredString(this.font, "Press ESC to continue (some features may not work)", centerX, centerY + 70, 0xFFAAAA);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // エラー時のみESCで閉じられるようにする
        return MMOSInstallListener.INSTANCE.isFailed();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
