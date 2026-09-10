package jp.moyashi.phoneos.forge.mixins;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.apps.chromiumbrowser.ChromiumBrowserScreen;
import jp.moyashi.phoneos.forge.gui.ProcessingScreen;
import jp.moyashi.phoneos.forge.service.SmartphoneBackgroundService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * MinecraftチャットのURLクリックをインターセプトし、
 * MMOS内蔵Chromiumブラウザで開くMixin。
 *
 * Screen.handleComponentClicked(Style)の先頭にフックし、
 * OPEN_URLアクションを検出した場合にMMOSブラウザへリダイレクトする。
 * Kernelが利用できない場合（ワールド外等）は従来のシステムブラウザにフォールバックする。
 */
@Mixin(Screen.class)
public class MMOSChatUrlInterceptMixin {

    private static final Logger LOGGER = LogManager.getLogger("MMOSChatUrlInterceptMixin");

    /**
     * handleComponentClicked(Style)をインターセプトし、URLクリックをMMOSブラウザにリダイレクトする。
     * Note: m_5561_ is the SRG name for handleComponentClicked in Forge 1.20.1
     *
     * @param style クリックされたテキストのスタイル
     * @param cir コールバック情報（戻り値付き）
     */
    @Inject(at = @At("HEAD"), method = "m_5561_", cancellable = true, remap = false)
    private void mmos$onHandleComponentClicked(Style style, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (style == null) {
                return;
            }

            ClickEvent clickEvent = style.getClickEvent();
            if (clickEvent == null || clickEvent.getAction() != ClickEvent.Action.OPEN_URL) {
                return;
            }

            String url = clickEvent.getValue();
            if (url == null || url.isEmpty()) {
                return;
            }

            // Kernelを取得（ワールド内でのみ利用可能）
            Kernel kernel = SmartphoneBackgroundService.getKernel();
            if (kernel == null) {
                // Kernelが利用できない場合は従来のシステムブラウザにフォールバック
                LOGGER.debug("[MMOSChatUrlInterceptMixin] Kernel not available, falling back to system browser for: {}", url);
                return;
            }

            LOGGER.info("[MMOSChatUrlInterceptMixin] Intercepting URL click: {}", url);

            // スリープ状態とロック状態の確認
            // Note: ProcessingScreen.init()がスリープ中ならwake()を呼び、
            //       wake()→showLockScreenAfterWake()→lockManager.lock()でロック状態になる。
            //       そのため、スリープ中の場合もペンディングURL経路を使う必要がある。
            boolean isSleeping = kernel.isSleeping();
            boolean isLocked = kernel.getLockManager() != null && kernel.getLockManager().isLocked();

            if (isLocked || isSleeping) {
                // ロック中 or スリープ中: ペンディングURLを設定し、ロック解除後にブラウザで開く
                // スリープ中はProcessingScreen.init()がwake()→lock()するため、必ずロック画面を経由する
                LOGGER.info("[MMOSChatUrlInterceptMixin] OS is locked/sleeping (locked={}, sleeping={}), setting pending URL: {}", isLocked, isSleeping, url);
                kernel.setPendingUrlAfterUnlock(url);
            } else {
                // アンロック中 かつ 非スリープ: 直接ブラウザ画面を開く
                ChromiumBrowserScreen browserScreen = new ChromiumBrowserScreen(kernel, url);
                kernel.getScreenManager().pushScreen(browserScreen);
            }

            // MinecraftのGUIをProcessingScreen（スマホUI）に切り替え
            Minecraft.getInstance().setScreen(new ProcessingScreen());

            // 元のURL処理をキャンセル
            cir.setReturnValue(true);

        } catch (Exception e) {
            LOGGER.error("[MMOSChatUrlInterceptMixin] Error intercepting URL click, falling back to system browser", e);
            // エラー時は従来動作にフォールバック（returnしてデフォルト処理を継続）
        }
    }
}
