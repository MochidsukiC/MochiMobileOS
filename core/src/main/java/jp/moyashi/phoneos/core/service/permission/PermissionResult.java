package jp.moyashi.phoneos.core.service.permission;

/**
 * 権限リクエストの結果。
 */
public enum PermissionResult {
    /** 権限が許可された */
    GRANTED,

    /** 権限が拒否された */
    DENIED,

    /** 権限が拒否され、今後も尋ねない設定にされた */
    DENIED_PERMANENTLY
}
