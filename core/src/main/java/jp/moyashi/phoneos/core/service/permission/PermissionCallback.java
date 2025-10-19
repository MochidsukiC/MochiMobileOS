package jp.moyashi.phoneos.core.service.permission;

/**
 * 権限リクエストのコールバックインターフェース。
 */
@FunctionalInterface
public interface PermissionCallback {
    /**
     * 権限リクエストの結果が返された時に呼び出される。
     *
     * @param permission リクエストした権限
     * @param result 結果（GRANTED, DENIED, DENIED_PERMANENTLY）
     */
    void onResult(Permission permission, PermissionResult result);
}
