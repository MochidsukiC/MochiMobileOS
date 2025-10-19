package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.service.VFS;

import java.util.HashSet;
import java.util.Set;

/**
 * サービスマネージャーの設定を管理するクラス。
 * 自動起動リストの保存/読み込みを行う。
 *
 * @author MochiMobileOS
 * @version 1.0
 */
public class ServiceConfig {

    private static final String CONFIG_PATH = "system/services/autostart.json";
    private static final String CONFIG_VERSION = "1.0";

    private final VFS vfs;
    private Set<String> autostartApps;

    /**
     * ServiceConfigを作成する。
     *
     * @param vfs VFSインスタンス
     */
    public ServiceConfig(VFS vfs) {
        this.vfs = vfs;
        this.autostartApps = new HashSet<>();
        load();
    }

    /**
     * 自動起動リストを取得する。
     *
     * @return 自動起動アプリIDのセット
     */
    public Set<String> getAutostartApps() {
        return new HashSet<>(autostartApps);
    }

    /**
     * アプリを自動起動リストに追加する。
     *
     * @param appId アプリID
     */
    public void addAutostartApp(String appId) {
        autostartApps.add(appId);
        save();
    }

    /**
     * アプリを自動起動リストから削除する。
     *
     * @param appId アプリID
     */
    public void removeAutostartApp(String appId) {
        autostartApps.remove(appId);
        save();
    }

    /**
     * アプリが自動起動リストに含まれているか確認する。
     *
     * @param appId アプリID
     * @return 含まれている場合true
     */
    public boolean isAutostartApp(String appId) {
        return autostartApps.contains(appId);
    }

    /**
     * 設定をVFSから読み込む。
     */
    private void load() {
        try {
            String json = vfs.readFile(CONFIG_PATH);
            if (json != null && !json.isEmpty()) {
                parseJson(json);
                System.out.println("ServiceConfig: Loaded autostart config: " + autostartApps.size() + " apps");
            } else {
                // デフォルトリストを作成
                createDefaultConfig();
            }
        } catch (Exception e) {
            System.err.println("ServiceConfig: Failed to load config: " + e.getMessage());
            createDefaultConfig();
        }
    }

    /**
     * 設定をVFSに保存する。
     */
    private void save() {
        try {
            String json = toJson();
            vfs.writeFile(CONFIG_PATH, json);
            System.out.println("ServiceConfig: Saved autostart config: " + autostartApps.size() + " apps");
        } catch (Exception e) {
            System.err.println("ServiceConfig: Failed to save config: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * デフォルト設定を作成する。
     * 現在はデフォルトで自動起動するアプリはない。
     */
    private void createDefaultConfig() {
        autostartApps = new HashSet<>();
        // デフォルトでは自動起動アプリなし
        // 将来的にはシステムサービス（通知監視など）を追加可能
        System.out.println("ServiceConfig: Created default config (no autostart apps)");
        save();
    }

    /**
     * JSON文字列をパースする（簡易実装）。
     *
     * @param json JSON文字列
     */
    private void parseJson(String json) {
        autostartApps = new HashSet<>();

        // 簡易JSONパーサー: "autostartApps":["app1","app2"]
        int arrayStart = json.indexOf("[");
        int arrayEnd = json.indexOf("]");

        if (arrayStart != -1 && arrayEnd != -1 && arrayStart < arrayEnd) {
            String arrayContent = json.substring(arrayStart + 1, arrayEnd);

            if (!arrayContent.trim().isEmpty()) {
                String[] apps = arrayContent.split(",");
                for (String app : apps) {
                    // "app_id" -> app_id
                    String appId = app.trim().replace("\"", "");
                    if (!appId.isEmpty()) {
                        autostartApps.add(appId);
                    }
                }
            }
        }
    }

    /**
     * JSON文字列に変換する（簡易実装）。
     *
     * @return JSON文字列
     */
    private String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": \"").append(CONFIG_VERSION).append("\",\n");
        sb.append("  \"autostartApps\": [\n");

        int i = 0;
        for (String appId : autostartApps) {
            sb.append("    \"").append(appId).append("\"");
            if (i < autostartApps.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
            i++;
        }

        sb.append("  ]\n");
        sb.append("}\n");

        return sb.toString();
    }

    /**
     * 設定をリロードする。
     */
    public void reload() {
        load();
    }
}
