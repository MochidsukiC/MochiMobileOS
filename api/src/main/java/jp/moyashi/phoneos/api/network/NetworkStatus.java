package jp.moyashi.phoneos.api.network;

/**
 * ネットワーク接続状態を表すenum。
 */
public enum NetworkStatus {
    /** 圏外（電波なし） */
    NO_SERVICE,

    /** 接続中 */
    CONNECTED,

    /** 接続試行中 */
    CONNECTING,

    /** オフライン（機内モード等） */
    OFFLINE,

    /** エラー */
    ERROR
}
