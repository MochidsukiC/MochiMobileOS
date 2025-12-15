package jp.moyashi.phoneos.api.permission;

import java.util.List;
import java.util.Set;

/**
 * アプリケーションの権限を管理するマネージャーインターフェース。
 * Android風のパーミッションシステムを提供する。
 */
public interface PermissionManager {

    /**
     * アプリケーションが特定の権限を持っているかチェックする。
     *
     * @param permission チェックする権限
     * @return 権限が許可されている場合true
     */
    boolean hasPermission(Permission permission);

    /**
     * アプリケーションが複数の権限を持っているかチェックする。
     *
     * @param permissions チェックする権限のリスト
     * @return すべての権限が許可されている場合true
     */
    boolean hasAllPermissions(List<Permission> permissions);

    /**
     * 権限をリクエストする。
     * 危険な権限の場合、ユーザーに確認ダイアログを表示する。
     *
     * @param permission リクエストする権限
     * @param callback リクエスト結果を受け取るコールバック
     */
    void requestPermission(Permission permission, PermissionCallback callback);

    /**
     * 複数の権限を同時にリクエストする。
     *
     * @param permissions リクエストする権限のリスト
     * @param callback リクエスト結果を受け取るコールバック
     */
    void requestPermissions(List<Permission> permissions, PermissionCallback callback);

    /**
     * アプリケーションが持つすべての権限を取得する。
     *
     * @return 許可されている権限のセット
     */
    Set<Permission> getGrantedPermissions();

    /**
     * 権限が「今後も尋ねない」設定になっているかチェックする。
     *
     * @param permission チェックする権限
     * @return 「今後も尋ねない」設定の場合true
     */
    boolean isDeniedPermanently(Permission permission);
}
