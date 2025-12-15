package jp.moyashi.phoneos.api.permission;

/**
 * 権限リクエストの結果。
 */
public enum PermissionResult {
    /** 権限が許可された */
    GRANTED,
    /** 権限が拒否された */
    DENIED,
    /** 権限が永久に拒否された（「今後も尋ねない」を選択） */
    DENIED_PERMANENTLY
}
