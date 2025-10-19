package jp.moyashi.phoneos.core.service.permission;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.VFS;
import jp.moyashi.phoneos.core.ui.popup.PopupManager;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * パーミッション管理システムの実装クラス。
 * アプリケーションごとの権限情報をVFSに永続化し、ユーザーの承認を管理する。
 */
public class PermissionManagerImpl implements PermissionManager {

    private final Kernel kernel;
    private final VFS vfs;
    private final Gson gson;

    // アプリID -> 許可された権限のセット
    private final Map<String, Set<Permission>> grantedPermissions;

    // アプリID -> 永久に拒否された権限のセット
    private final Map<String, Set<Permission>> deniedPermanently;

    // 権限ファイルのパス
    private static final String PERMISSIONS_FILE = "system/permissions.json";

    /**
     * PermissionManagerを初期化する。
     *
     * @param kernel Kernelインスタンス
     */
    public PermissionManagerImpl(Kernel kernel) {
        this.kernel = kernel;
        this.vfs = kernel.getVFS();
        this.gson = new Gson();
        this.grantedPermissions = new ConcurrentHashMap<>();
        this.deniedPermanently = new ConcurrentHashMap<>();

        // 権限情報を読み込む
        loadPermissions();
    }

    @Override
    public boolean hasPermission(String appId, Permission permission) {
        // 危険でない権限は自動的に許可
        if (!permission.isDangerous()) {
            return true;
        }

        Set<Permission> permissions = grantedPermissions.get(appId);
        return permissions != null && permissions.contains(permission);
    }

    @Override
    public boolean hasAllPermissions(String appId, List<Permission> permissions) {
        for (Permission permission : permissions) {
            if (!hasPermission(appId, permission)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void requestPermission(String appId, String appName, Permission permission, PermissionCallback callback) {
        requestPermissions(appId, appName, Collections.singletonList(permission), callback);
    }

    @Override
    public void requestPermissions(String appId, String appName, List<Permission> permissions, PermissionCallback callback) {
        // 既に許可されているか、危険でない権限のみの場合は即座に許可
        boolean allGrantedOrSafe = true;
        for (Permission permission : permissions) {
            if (permission.isDangerous() && !hasPermission(appId, permission)) {
                allGrantedOrSafe = false;
                break;
            }
        }

        if (allGrantedOrSafe) {
            // すべて許可済みまたは安全な権限
            for (Permission permission : permissions) {
                if (callback != null) {
                    callback.onResult(permission, PermissionResult.GRANTED);
                }
            }
            return;
        }

        // 危険な権限で未許可のものをリストアップ
        List<Permission> dangerousPermissions = new ArrayList<>();
        for (Permission permission : permissions) {
            if (permission.isDangerous() && !hasPermission(appId, permission)) {
                // 永久拒否されているかチェック
                if (isDeniedPermanently(appId, permission)) {
                    if (callback != null) {
                        callback.onResult(permission, PermissionResult.DENIED_PERMANENTLY);
                    }
                } else {
                    dangerousPermissions.add(permission);
                }
            }
        }

        // 確認が必要な権限がない場合は終了
        if (dangerousPermissions.isEmpty()) {
            return;
        }

        // 権限確認ダイアログを表示
        showPermissionDialog(appId, appName, dangerousPermissions, callback);
    }

    /**
     * 権限確認ダイアログを表示する。
     *
     * @param appId アプリケーションID
     * @param appName アプリケーション名
     * @param permissions 確認が必要な権限のリスト
     * @param callback コールバック
     */
    private void showPermissionDialog(String appId, String appName, List<Permission> permissions, PermissionCallback callback) {
        PopupManager popupManager = kernel.getPopupManager();
        if (popupManager == null) {
            // PopupManagerが利用できない場合は拒否
            for (Permission permission : permissions) {
                if (callback != null) {
                    callback.onResult(permission, PermissionResult.DENIED);
                }
            }
            return;
        }

        // メッセージを構築
        StringBuilder message = new StringBuilder();
        message.append(appName).append(" が以下の権限を必要としています:\n\n");
        for (Permission permission : permissions) {
            message.append("• ").append(permission.getDisplayName())
                    .append("\n  (").append(permission.getDescription()).append(")\n");
        }

        // TODO: PopupManager.showConfirmation()が実装されていないため、一時的に自動許可
        // 将来的には確認ダイアログを表示する
        System.out.println("PermissionManager: Auto-granting permissions (dialog not yet implemented)");
        for (Permission permission : permissions) {
            grantPermission(appId, permission);
            if (callback != null) {
                callback.onResult(permission, PermissionResult.GRANTED);
            }
        }
        savePermissions();
    }

    @Override
    public void grantPermission(String appId, Permission permission) {
        grantedPermissions.computeIfAbsent(appId, k -> new HashSet<>()).add(permission);

        // 永久拒否リストから削除（許可された場合）
        Set<Permission> denied = deniedPermanently.get(appId);
        if (denied != null) {
            denied.remove(permission);
        }

        System.out.println("PermissionManager: Granted " + permission.getPermissionString() + " to " + appId);
    }

    @Override
    public void revokePermission(String appId, Permission permission) {
        Set<Permission> permissions = grantedPermissions.get(appId);
        if (permissions != null) {
            permissions.remove(permission);
            System.out.println("PermissionManager: Revoked " + permission.getPermissionString() + " from " + appId);
        }
    }

    @Override
    public Set<Permission> getGrantedPermissions(String appId) {
        Set<Permission> permissions = grantedPermissions.get(appId);
        return permissions != null ? new HashSet<>(permissions) : new HashSet<>();
    }

    @Override
    public boolean isDeniedPermanently(String appId, Permission permission) {
        Set<Permission> denied = deniedPermanently.get(appId);
        return denied != null && denied.contains(permission);
    }

    @Override
    public void resetPermissions(String appId) {
        grantedPermissions.remove(appId);
        deniedPermanently.remove(appId);
        savePermissions();
        System.out.println("PermissionManager: Reset all permissions for " + appId);
    }

    @Override
    public void savePermissions() {
        try {
            // 権限情報をJSONに変換して保存
            PermissionData data = new PermissionData();
            data.grantedPermissions = convertPermissionsToStrings(grantedPermissions);
            data.deniedPermanently = convertPermissionsToStrings(deniedPermanently);

            String json = gson.toJson(data);
            vfs.writeFile(PERMISSIONS_FILE, json);

            System.out.println("PermissionManager: Saved permissions to VFS");
        } catch (Exception e) {
            System.err.println("PermissionManager: Failed to save permissions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void loadPermissions() {
        try {
            if (!vfs.fileExists(PERMISSIONS_FILE)) {
                System.out.println("PermissionManager: No permissions file found, starting with empty permissions");
                return;
            }

            String json = vfs.readFile(PERMISSIONS_FILE);
            PermissionData data = gson.fromJson(json, PermissionData.class);

            if (data != null) {
                grantedPermissions.clear();
                grantedPermissions.putAll(convertStringsToPermissions(data.grantedPermissions));

                deniedPermanently.clear();
                deniedPermanently.putAll(convertStringsToPermissions(data.deniedPermanently));

                System.out.println("PermissionManager: Loaded permissions from VFS");
            }
        } catch (Exception e) {
            System.err.println("PermissionManager: Failed to load permissions: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Permission列挙型のマップを文字列のマップに変換する（JSON保存用）。
     */
    private Map<String, Set<String>> convertPermissionsToStrings(Map<String, Set<Permission>> permissionMap) {
        Map<String, Set<String>> result = new HashMap<>();
        for (Map.Entry<String, Set<Permission>> entry : permissionMap.entrySet()) {
            Set<String> permissionStrings = new HashSet<>();
            for (Permission permission : entry.getValue()) {
                permissionStrings.add(permission.getPermissionString());
            }
            result.put(entry.getKey(), permissionStrings);
        }
        return result;
    }

    /**
     * 文字列のマップをPermission列挙型のマップに変換する（JSON読み込み用）。
     */
    private Map<String, Set<Permission>> convertStringsToPermissions(Map<String, Set<String>> stringMap) {
        Map<String, Set<Permission>> result = new HashMap<>();
        if (stringMap == null) {
            return result;
        }

        for (Map.Entry<String, Set<String>> entry : stringMap.entrySet()) {
            Set<Permission> permissions = new HashSet<>();
            for (String permissionString : entry.getValue()) {
                Permission permission = Permission.fromString(permissionString);
                if (permission != null) {
                    permissions.add(permission);
                }
            }
            result.put(entry.getKey(), permissions);
        }
        return result;
    }

    /**
     * 権限データのJSON保存用クラス。
     */
    private static class PermissionData {
        Map<String, Set<String>> grantedPermissions;
        Map<String, Set<String>> deniedPermanently;
    }
}
