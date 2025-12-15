package jp.moyashi.phoneos.api.hardware;

/**
 * SIM情報API。
 * SIM情報を提供する。具体的には使用者の名前とUUIDが入る。
 */
public interface SIMInfo {
    /**
     * SIM情報が利用可能かどうかを取得する。
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();

    /**
     * SIMカードの所持者名を取得する。
     *
     * @return 所持者名
     */
    String getOwnerName();

    /**
     * SIMカードの所持者UUIDを取得する。
     *
     * @return 所持者UUID
     */
    String getOwnerUUID();

    /**
     * SIMカードが挿入されているかどうかを取得する。
     *
     * @return 挿入されている場合true
     */
    boolean isInserted();
}
