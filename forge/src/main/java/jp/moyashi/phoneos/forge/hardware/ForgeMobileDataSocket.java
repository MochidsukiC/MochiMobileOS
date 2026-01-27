package jp.moyashi.phoneos.forge.hardware;

import jp.moyashi.phoneos.core.service.hardware.MobileDataSocket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Forge環境用のモバイルデータ通信ソケット実装。
 * 仮想インターネット通信をバイパスする。
 */
public class ForgeMobileDataSocket implements MobileDataSocket {
    private Player player;
    private Level level;
    private boolean connected;

    public ForgeMobileDataSocket(Player player) {
        this.player = player;
        this.level = player != null ? player.level() : null;
        this.connected = true;
    }

    /**
     * プレイヤーを更新する（ワールド変更時など）。
     */
    public void updatePlayer(Player player) {
        this.player = player;
        this.level = player != null ? player.level() : null;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public int getSignalStrength() {
        // 常に最大強度を返す
        return 5;
    }

    @Override
    public String getServiceName() {
        if (level == null) {
            return "No Service";
        }

        // ディメンションに基づいてサービス名を決定
        String dimensionName = level.dimension().location().toString();

        if (dimensionName.contains("overworld")) {
            return "Overworld Network";
        } else if (dimensionName.contains("nether")) {
            return "Nether Network";
        } else if (dimensionName.contains("end")) {
            return "End Network";
        } else {
            return "Unknown Network";
        }
    }

    @Override
    public boolean isConnected() {
        return connected && getSignalStrength() > 0;
    }

    /**
     * 接続状態を設定する。
     */
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}