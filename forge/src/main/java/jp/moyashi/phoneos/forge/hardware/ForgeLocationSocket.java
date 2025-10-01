package jp.moyashi.phoneos.forge.hardware;

import jp.moyashi.phoneos.core.service.hardware.LocationSocket;
import net.minecraft.world.entity.player.Player;

/**
 * Forge環境用の位置情報ソケット実装。
 * プレイヤーの位置情報を提供する。
 */
public class ForgeLocationSocket implements LocationSocket {
    private Player player;
    private boolean enabled;

    public ForgeLocationSocket(Player player) {
        this.player = player;
        this.enabled = true;
    }

    /**
     * プレイヤーを更新する（ワールド変更時など）。
     */
    public void updatePlayer(Player player) {
        this.player = player;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public LocationData getLocation() {
        if (player != null && enabled) {
            // プレイヤーの座標を取得
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();

            // GPS精度（ブロック単位）
            // 地上にいる場合は高精度、地下や高所では低精度
            float accuracy = calculateAccuracy(y);

            return new LocationData(x, y, z, accuracy);
        }

        return new LocationData(0, 0, 0, 0.0f);
    }

    /**
     * Y座標に基づいてGPS精度を計算する。
     * 地上（y=60-80）: 高精度（1-5m）
     * 地下/高所: 低精度（10-50m）
     */
    private float calculateAccuracy(double y) {
        if (y >= 60 && y <= 80) {
            // 地上：高精度
            return 1.0f + (float) (Math.random() * 4.0);
        } else if (y > 80) {
            // 高所：中精度
            float heightFactor = (float) (y - 80) / 100.0f;
            return 5.0f + heightFactor * 20.0f;
        } else {
            // 地下：低精度
            float depthFactor = (60.0f - (float) y) / 60.0f;
            return 10.0f + depthFactor * 40.0f;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}