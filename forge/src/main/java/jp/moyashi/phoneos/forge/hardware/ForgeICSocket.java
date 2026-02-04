package jp.moyashi.phoneos.forge.hardware;

import jp.moyashi.phoneos.core.service.hardware.ICSocket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;

/**
 * Forge環境用のIC通信ソケット実装。
 * プレイヤーがブロックをしゃがみ右クリックしたときの座標やエンティティのUUIDを取得する。
 */
public class ForgeICSocket implements ICSocket {
    private boolean enabled;
    private ICData pendingData;

    public ForgeICSocket() {
        this.enabled = false;
        this.pendingData = new ICData(); // NONE
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public ICData pollICData() {
        // データを取得して返し、クリアする
        ICData data = pendingData;
        pendingData = new ICData(); // NONE
        return data;
    }

    /**
     * ブロックをスキャンしたときに呼び出される（forge側から）。
     */
    public void onBlockScanned(BlockPos pos) {
        if (enabled) {
            pendingData = new ICData(pos.getX(), pos.getY(), pos.getZ());
        }
    }

    /**
     * エンティティをスキャンしたときに呼び出される（forge側から）。
     */
    public void onEntityScanned(Entity entity) {
        if (enabled) {
            pendingData = new ICData(entity.getUUID().toString());
        }
    }

    /**
     * エンティティをスキャンしたときに呼び出される（UUID直接指定版）。
     */
    public void onEntityScanned(UUID uuid) {
        if (enabled) {
            pendingData = new ICData(uuid.toString());
        }
    }
}