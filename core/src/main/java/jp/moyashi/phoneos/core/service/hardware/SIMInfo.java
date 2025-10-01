package jp.moyashi.phoneos.core.service.hardware;

/**
 * SIM情報API。
 * SIM情報を提供する。具体的には使用者の名前とUUIDが入る。
 *
 * 環境別動作:
 * - standalone: 名前はDev、UUIDは0000-0000-0000-0000を返す
 * - forge-mod: 所持者の表示名とUUIDを返す
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

    /**
     * SIMカードの所持者名を設定する（forge-mod用）。
     *
     * @param name 所持者名
     */
    void setOwnerName(String name);

    /**
     * SIMカードの所持者UUIDを設定する（forge-mod用）。
     *
     * @param uuid 所持者UUID
     */
    void setOwnerUUID(String uuid);
}