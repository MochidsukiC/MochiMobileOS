package jp.moyashi.phoneos.forge.mixins;

import jp.moyashi.phoneos.forge.installer.MMOSInstaller;
import jp.moyashi.phoneos.forge.installer.MMOSInstallListener;
import jp.moyashi.phoneos.forge.service.SmartphoneBackgroundService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SharedConstantsの静的初期化時にMMOSインストーラーを起動するMixin。
 *
 * MCEFと同様に、Minecraft起動の早い段階でバックグラウンドダウンロードを開始する。
 * これにより、タイトル画面表示前にJCEFのダウンロードが開始される。
 *
 * 注意: 外部プロセスモードでは、JCEFはサーバー側（coreモジュール）で管理されるため、
 * Forge側でのインストールは不要。
 *
 * @author MochiOS Team
 * @version 1.1
 */
@Mixin(net.minecraft.SharedConstants.class)
public class MMOSInstallMixin {

    private static final Logger LOGGER = LogManager.getLogger("MMOSInstallMixin");

    /** インストーラーが既に起動されたかどうか */
    private static boolean mmos$installerStarted = false;

    /**
     * SharedConstantsの静的初期化時にインストーラーを起動する。
     * このクラスはMinecraft起動の非常に早い段階で読み込まれる。
     *
     * 外部プロセスモードでは、JCEFはサーバー側でjcefmavenが自動管理するため、
     * Forge側でのインストールはスキップされる。
     *
     * @param ci コールバック情報
     */
    @Inject(at = @At("TAIL"), method = "<clinit>", remap = false)
    private static void mmos$onStaticInit(CallbackInfo ci) {
        if (mmos$installerStarted) {
            return;
        }
        mmos$installerStarted = true;

        // 外部プロセスモードでは、JCEFはサーバー側（coreモジュール）で管理される
        // Forge側でのインストールは不要
        if (SmartphoneBackgroundService.EXTERNAL_PROCESS_MODE) {
            LOGGER.info("[MMOSInstallMixin] External process mode enabled - skipping Forge-side JCEF installation");
            LOGGER.info("[MMOSInstallMixin] JCEF will be managed by server-side core module via jcefmaven");
            // MMOSInstallListenerを完了状態に設定（待機画面をスキップするため）
            MMOSInstallListener.setInstalled();
            return;
        }

        LOGGER.info("[MMOSInstallMixin] MMOS installer initialization triggered (local mode)");

        // ライブラリパスをセットアップ
        MMOSInstaller.setupLibraryPath();

        // バックグラウンドでインストーラーを起動
        Thread installerThread = new Thread(new MMOSInstaller(), "MMOS-Installer");
        installerThread.setDaemon(true);
        installerThread.start();

        LOGGER.info("[MMOSInstallMixin] MMOS installer thread started");
    }
}
