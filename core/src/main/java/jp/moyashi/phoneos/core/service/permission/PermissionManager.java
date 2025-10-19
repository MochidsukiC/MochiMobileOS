package jp.moyashi.phoneos.core.service.permission;

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
     * @param appId アプリケーションID
     * @param permission チェックする権限
     * @return 権限が許可されている場合true
     */
    boolean hasPermission(String appId, Permission permission);

    /**
     * アプリケーションが複数の権限を持っているかチェックする。
     *
     * @param appId アプリケーションID
     * @param permissions チェックする権限のリスト
     * @return すべての権限が許可されている場合true
     */
    boolean hasAllPermissions(String appId, List<Permission> permissions);

    /**
     * 権限をリクエストする。
     * 危険な権限の場合、ユーザーに確認ダイアログを表示する。
     *
     * @param appId アプリケーションID
     * @param appName アプリケーション名（ダイアログ表示用）
     * @param permission リクエストする権限
     * @param callback リクエスト結果を受け取るコールバック
     */
    void requestPermission(String appId, String appName, Permission permission, PermissionCallback callback);

    /**
     * 複数の権限を同時にリクエストする。
     *
     * @param appId アプリケーションID
     * @param appName アプリケーション名（ダイアログ表示用）
     * @param permissions リクエストする権限のリスト
     * @param callback リクエスト結果を受け取るコールバック
     */
    void requestPermissions(String appId, String appName, List<Permission> permissions, PermissionCallback callback);

    /**
     * アプリケーションに権限を付与する（内部使用、ユーザー操作の結果として実行される）。
     *
     * @param appId アプリケーションID
     * @param permission 付与する権限
     */
    void grantPermission(String appId, Permission permission);

    /**
     * アプリケーションから権限を剥奪する。
     *
     * @param appId アプリケーションID
     * @param permission 剥奪する権限
     */
    void revokePermission(String appId, Permission permission);

    /**
     * アプリケーションが持つすべての権限を取得する。
     *
     * @param appId アプリケーションID
     * @return 許可されている権限のセット
     */
    Set<Permission> getGrantedPermissions(String appId);

    /**
     * 権限が「今後も尋ねない」設定になっているかチェックする。
     *
     * @param appId アプリケーションID
     * @param permission チェックする権限
     * @return 「今後も尋ねない」設定の場合true
     */
    boolean isDeniedPermanently(String appId, Permission permission);

    /**
     * アプリケーションのすべての権限設定をリセットする。
     *
     * @param appId アプリケーションID
     */
    void resetPermissions(String appId);

    /**
     * システムの権限設定を保存する。
     */
    void savePermissions();

    /**
     * システムの権限設定を読み込む。
     */
    void loadPermissions();
}
