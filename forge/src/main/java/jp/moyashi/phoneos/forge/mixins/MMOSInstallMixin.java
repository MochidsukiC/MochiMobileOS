package jp.moyashi.phoneos.forge.mixins;

import jp.moyashi.phoneos.forge.installer.MMOSInstaller;
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
 * @author MochiOS Team
 * @version 1.2
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
     * @param ci コールバック情報
     */
    @Inject(at = @At("TAIL"), method = "<clinit>", remap = false)
    private static void mmos$onStaticInit(CallbackInfo ci) {
        if (mmos$installerStarted) {
            return;
        }
        mmos$installerStarted = true;

        LOGGER.info("[MMOSInstallMixin] MMOS installer initialization triggered");

        // ライブラリパスをセットアップ
        MMOSInstaller.setupLibraryPath();

        // バックグラウンドでインストーラーを起動
        Thread installerThread = new Thread(new MMOSInstaller(), "MMOS-Installer");
        installerThread.setDaemon(true);
        installerThread.start();

        LOGGER.info("[MMOSInstallMixin] MMOS installer thread started");
    }
}
