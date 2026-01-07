package jp.moyashi.phoneos.forge.hardware;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * AdvancedVC 2.0 MODの検出ユーティリティ。
 * AVCがインストールされているかを検知し、オーディオブリッジの有効化を判断する。
 */
public class AVCDetector {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String AVC_MOD_ID = "advancedvc2_0";
    private static Boolean avcAvailable = null;

    /**
     * AdvancedVC 2.0 MODがインストールされているかをチェックする。
     * 結果はキャッシュされ、2回目以降は即座に返される。
     *
     * @return AVCが利用可能な場合true
     */
    public static boolean isAVCAvailable() {
        if (avcAvailable == null) {
            avcAvailable = ModList.get().isLoaded(AVC_MOD_ID);
            if (avcAvailable) {
                LOGGER.info("[AVCDetector] AdvancedVC 2.0 MOD detected - Audio bridge enabled");
            } else {
                LOGGER.info("[AVCDetector] AdvancedVC 2.0 MOD not found - Audio bridge disabled");
            }
        }
        return avcAvailable;
    }

    /**
     * キャッシュをクリアする（テスト用）。
     */
    public static void clearCache() {
        avcAvailable = null;
    }
}
