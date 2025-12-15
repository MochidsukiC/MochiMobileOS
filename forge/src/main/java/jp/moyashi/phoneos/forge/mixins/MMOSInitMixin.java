package jp.moyashi.phoneos.forge.mixins;

import jp.moyashi.phoneos.forge.gui.MMOSInstallerScreen;
import jp.moyashi.phoneos.forge.installer.MMOSInstallListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft.setScreen()をインターセプトして、
 * MMOSインストール中は待機画面を表示するMixin。
 *
 * @author MochiOS Team
 * @version 1.0
 */
@Mixin(Minecraft.class)
public class MMOSInitMixin {

    private static final Logger LOGGER = LogManager.getLogger("MMOSInitMixin");

    /** インストーラー画面の再帰防止フラグ */
    private static boolean mmos$redirectingScreen = false;

    /** 初期化完了フラグ */
    private static boolean mmos$initialized = false;

    /**
     * setScreen()をインターセプトして、インストール中は待機画面を表示する。
     * Note: m_91152_ is the SRG name for setScreen in Forge 1.20.1
     *
     * @param screen 表示しようとしている画面
     * @param ci コールバック情報
     */
    @Inject(at = @At("HEAD"), method = "m_91152_", cancellable = true, remap = false)
    private void mmos$onSetScreen(Screen screen, CallbackInfo ci) {
        // 再帰防止
        if (mmos$redirectingScreen) {
            return;
        }

        // インストーラー画面への遷移は許可
        if (screen instanceof MMOSInstallerScreen) {
            return;
        }

        // タイトル画面または最初の画面遷移時にチェック
        if (screen instanceof TitleScreen || (screen != null && !mmos$initialized)) {
            MMOSInstallListener listener = MMOSInstallListener.INSTANCE;

            // インストールが完了していない場合、待機画面を表示
            if (!listener.isDone() && !listener.isFailed()) {
                LOGGER.info("[MMOSInitMixin] Installation in progress, showing installer screen");

                mmos$redirectingScreen = true;
                try {
                    Minecraft.getInstance().setScreen(new MMOSInstallerScreen(screen));
                } finally {
                    mmos$redirectingScreen = false;
                }

                ci.cancel();
                return;
            }

            // 初期化完了をマーク
            if (listener.isDone() && !mmos$initialized) {
                LOGGER.info("[MMOSInitMixin] MMOS installation complete, proceeding to game");
                mmos$initialized = true;
            }

            // 失敗した場合はログ出力して続行
            if (listener.isFailed()) {
                LOGGER.error("[MMOSInitMixin] MMOS installation failed: " + listener.getErrorMessage());
                LOGGER.error("[MMOSInitMixin] Some MMOS features may not work correctly");
                mmos$initialized = true;
            }
        }
    }
}
