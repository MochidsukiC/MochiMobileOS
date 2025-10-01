package jp.moyashi.phoneos.core.service.hardware;

/**
 * SIM情報のデフォルト実装（standalone用）。
 * 開発用の固定値を返す。
 */
public class DefaultSIMInfo implements SIMInfo {
    private String ownerName = "Dev";
    private String ownerUUID = "00000000-0000-0000-0000-000000000000";

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public String getOwnerUUID() {
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