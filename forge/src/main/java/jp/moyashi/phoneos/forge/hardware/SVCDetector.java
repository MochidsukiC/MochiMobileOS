package jp.moyashi.phoneos.forge.hardware;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Simple Voice Chat (SVC) MODの検出ユーティリティ。
 * SVCがインストールされているかを検知し、マイク/スピーカーAPIの有効化を判断する。
 */
public class SVCDetector {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SVC_MOD_ID = "voicechat";
    private static Boolean svcAvailable = null;

    /**
     * Simple Voice Chat MODがインストールされているかをチェックする。
     * 結果はキャッシュされ、2回目以降は即座に返される。
     *
     * @return SVCが利用可能な場合true
     */
    public static boolean isSVCAvailable() {
        if (svcAvailable == null) {
            svcAvailable = ModList.get().isLoaded(SVC_MOD_ID);
            if (svcAvailable) {
                LOGGER.info("[SVCDetector] Simple Voice Chat MOD detected - Audio features enabled");
            } else {
                LOGGER.info("[SVCDetector] Simple Voice Chat MOD not found - Audio features disabled");
            }
        }
        return svcAvailable;
    }

    /**
     * キャッシュをクリアする（テスト用）。
     */
    public static void clearCache() {
        svcAvailable = null;
    }
}
