package jp.moyashi.phoneos.forge;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * MochiMobileOSのForge設定ファイル。
 *
 * この設定ファイルは、MOD環境でのMochiMobileOSの動作をカスタマイズするために使用されます。
 * 設定はconfig/mochimobileos-common.tomlに保存されます。
 *
 * 主な設定項目：
 * - preinstalledApps: ワールド読み込み時に自動インストールするアプリのIDリスト
 *
 * @author jp.moyashi
 * @version 1.0
 * @since 1.0
 */
public class MMOSConfig {

    /** Forge設定仕様 */
    public static final ForgeConfigSpec SPEC;

    /** プリインストールアプリのリスト */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PREINSTALLED_APPS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("MochiMobileOS Configuration");
        builder.comment("This file controls various aspects of MochiMobileOS behavior in the Minecraft environment.");
        builder.push("apps");

        PREINSTALLED_APPS = builder
            .comment("List of application IDs to automatically install when a world is loaded.")
            .comment("Apps in this list will be installed without requiring user action in AppStore.")
            .comment("Example: [\"jp.mochidsuki.phoneos.moymoy\", \"com.example.myapp\"]")
            .defineList("preinstalled",
                List.of(), // デフォルトは空リスト
                obj -> obj instanceof String && !((String) obj).isEmpty());

        builder.pop();
        SPEC = builder.build();
    }

    /**
     * 指定されたアプリIDがプリインストールリストに含まれているかを確認します。
     *
     * @param appId 確認するアプリケーションID
     * @return プリインストールリストに含まれている場合true
     */
    public static boolean isPreinstalled(String appId) {
        if (appId == null || appId.isEmpty()) {
            return false;
        }
        List<? extends String> preinstalled = PREINSTALLED_APPS.get();
        return preinstalled.contains(appId);
    }

    /**
     * プリインストールアプリのリストを取得します。
     *
     * @return プリインストールアプリIDのリスト
     */
    public static List<String> getPreinstalledAppIds() {
        @SuppressWarnings("unchecked")
        List<String> list = (List<String>) PREINSTALLED_APPS.get();
        return list;
    }
}
