package jp.moyashi.phoneos.core.service.network;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IPvM (IP virtual Mobile) アドレスを表すクラス
 * 形式: [種類]-UUID
 *
 * 種類:
 * - 0: プレイヤー (Player_UUID)
 * - 1: デバイス/エンティティ (Entity_UUID) ※現在未使用
 * - 2: サーバー (外部Mod) (登録式識別ID)
 * - 3: システム (本Mod) (登録式識別ID)
 */
public class IPvMAddress {

    public enum AddressType {
        PLAYER(0),
        DEVICE(1),
        SERVER(2),
        SYSTEM(3);

        private final int code;

        AddressType(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static AddressType fromCode(int code) {
            for (AddressType type : values()) {
                if (type.code == code) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid address type code: " + code);
        }
    }

    private static final Pattern ADDRESS_PATTERN = Pattern.compile("^([0-3])-((?:[0-9a-fA-F]{4}-?){1,8})$");

    private final AddressType type;
    private final String uuid;

    /**
     * IPvMAddressを構築します
     * @param type アドレス種類
     * @param uuid UUID文字列（ハイフン区切り）
     */
    public IPvMAddress(AddressType type, String uuid) {
        if (type == null) {
            throw new IllegalArgumentException("Address type cannot be null");
        }
        if (uuid == null || uuid.isEmpty()) {
            throw new IllegalArgumentException("UUID cannot be null or empty");
        }
        this.type = type;
        this.uuid = uuid;
    }

    /**
     * 文字列からIPvMAddressを生成します
     * @param address "[種類]-UUID" 形式の文字列
     * @return IPvMAddressインスタンス
     */
    public static IPvMAddress fromString(String address) {
        if (address == null || address.isEmpty()) {
            throw new IllegalArgumentException("Address string cannot be null or empty");
        }

        Matcher matcher = ADDRESS_PATTERN.matcher(address);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid IPvM address format: " + address);
        }

        int typeCode = Integer.parseInt(matcher.group(1));
        String uuid = matcher.group(2);

        return new IPvMAddress(AddressType.fromCode(typeCode), uuid);
    }

    /**
     * プレイヤーアドレスを生成します
     * @param playerUUID プレイヤーのUUID
     * @return プレイヤーのIPvMAddress
     */
    public static IPvMAddress forPlayer(String playerUUID) {
        return new IPvMAddress(AddressType.PLAYER, playerUUID);
    }

    /**
     * サーバー（外部Mod）アドレスを生成します
     * @param serverId サーバーID
     * @return サーバーのIPvMAddress
     */
    public static IPvMAddress forServer(String serverId) {
        return new IPvMAddress(AddressType.SERVER, serverId);
    }

    /**
     * システムアドレスを生成します
     * @param systemId システムID
     * @return システムのIPvMAddress
     */
    public static IPvMAddress forSystem(String systemId) {
        return new IPvMAddress(AddressType.SYSTEM, systemId);
    }

    /**
     * デバイスアドレスを生成します
     * @param deviceUUID デバイスのUUID
     * @return デバイスのIPvMAddress
     */
    public static IPvMAddress forDevice(String deviceUUID) {
        return new IPvMAddress(AddressType.DEVICE, deviceUUID);
    }

    /**
     * アドレスの種類を取得します
     * @return AddressType
     */
    public AddressType getType() {
        return type;
    }

    /**
     * UUIDを取得します
     * @return UUID文字列
     */
    public String getUUID() {
        return uuid;
    }

    /**
     * プレイヤーアドレスかどうかを判定します
     * @return プレイヤーアドレスの場合true
     */
    public boolean isPlayer() {
        return type == AddressType.PLAYER;
    }

    /**
     * サーバー（外部Mod）アドレスかどうかを判定します
     * @return サーバーアドレスの場合true
     */
    public boolean isServer() {
        return type == AddressType.SERVER;
    }

    /**
     * システムアドレスかどうかを判定します
     * @return システムアドレスの場合true
     */
    public boolean isSystem() {
        return type == AddressType.SYSTEM;
    }

    /**
     * デバイスアドレスかどうかを判定します
     * @return デバイスアドレスの場合true
     */
    public boolean isDevice() {
        return type == AddressType.DEVICE;
    }

    @Override
    public String toString() {
        return type.getCode() + "-" + uuid;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        IPvMAddress that = (IPvMAddress) obj;
        return type == that.type && Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, uuid);
    }
}