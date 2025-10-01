package jp.moyashi.phoneos.forge.hardware;

import jp.moyashi.phoneos.core.service.hardware.SIMInfo;
import net.minecraft.world.entity.player.Player;

/**
 * Forge環境用のSIM情報実装。
 * プレイヤーの表示名とUUIDを提供する。
 */
public class ForgeSIMInfo implements SIMInfo {
    private Player player;
    private String ownerName;
    private String ownerUUID;

    public ForgeSIMInfo(Player player) {
        this.player = player;
        updateFromPlayer();
    }

    /**
     * プレイヤー情報を更新する。
     */
    private void updateFromPlayer() {
        if (player != null) {
            this.ownerName = player.getName().getString();
            this.ownerUUID = player.getUUID().toString();
        } else {
            this.ownerName = "Unknown";
            this.ownerUUID = "00000000-0000-0000-0000-000000000000";
        }
    }

    /**
     * プレイヤーを更新する（ワールド変更時など）。
     */
    public void updatePlayer(Player player) {
        this.player = player;
        updateFromPlayer();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getOwnerName() {
        if (player != null) {
            // 毎回最新の情報を返す
            return player.getName().getString();
        }
        return ownerName;
    }

    @Override
    public String getOwnerUUID() {
        if (player != null) {
            // 毎回最新の情報を返す
            return player.getUUID().toString();
        }
        return ownerUUID;
    }

    @Override
    public boolean isInserted() {
        return true;
    }

    @Override
    public void setOwnerName(String name) {
        this.ownerName = name;
    }

    @Override
    public void setOwnerUUID(String uuid) {
        this.ownerUUID = uuid;
    }
}