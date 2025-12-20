package jp.moyashi.phoneos.core.service.network;

/**
 * ネットワーク接続状態を表すenum。
 * SIMカード/ネットワークアダプターの状態を管理する。
 *
 * @author MochiOS Team
 * @version 2.0
 */
public enum NetworkStatus {
    /**
     * 圏外 - ソケットが設定されていない、または接続不可
     * SIMカードが挿入されていない状態に相当
     */
    NO_SERVICE("圏外", false),

    /**
     * 接続中 - ネットワークに接続済み
     */
    CONNECTED("接続中", true),

    /**
     * 接続試行中 - ネットワークへの接続を試みている
     */
    CONNECTING("接続中...", false),

    /**
     * オフライン - 機内モード等で意図的に切断
     */
    OFFLINE("オフライン", false),

    /**
     * エラー - 接続エラーが発生
     */
    ERROR("エラー", false);

    private final String displayName;
    private final boolean canSendData;

    NetworkStatus(String displayName, boolean canSendData) {
        this.displayName = displayName;
        this.canSendData = canSendData;
    }

    /**
     * 表示名を取得する。
     *
     * @return 表示名（日本語）
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * データ送信が可能かを判定する。
     *
     * @return データ送信可能な場合true
     */
    public boolean canSendData() {
        return canSendData;
    }

    /**
     * 圏外状態かを判定する。
     *
     * @return 圏外の場合true
     */
    public boolean isNoService() {
        return this == NO_SERVICE;
    }

    /**
     * 接続済みかを判定する。
     *
     * @return 接続済みの場合true
     */
    public boolean isConnected() {
        return this == CONNECTED;
    }
}
