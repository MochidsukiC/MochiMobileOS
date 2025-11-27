package jp.moyashi.phoneos.core.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * スマートフォンOS用の設定管理サービス。
 * - メモリ上の設定データ
 * - VFSへのJSON永続化
 * - 設定変更リスナーへの通知
 */
public class SettingsManager {

    /** 設定のキー値ペアを格納する内部ストレージ */
    private final Map<String, Object> settings;

    /** JSON変換用Gson */
    private final Gson gson;

    /** 永続化用VFS */
    private final VFS vfs;

    /** レジストリの保存パス */
    private static final String REGISTRY_PATH = "system/settings/registry.json";

    /** 変更リスナー */
    public interface SettingsListener {
        void onSettingChanged(String key, Object newValue);
    }

    private final Set<SettingsListener> listeners = new HashSet<>();

    /**
     * 新しいSettingsManagerインスタンスを構築する。
     *
     * @param vfs VFSへの参照。永続化に使用する。
     */
    public SettingsManager(VFS vfs) {
        this.vfs = vfs;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.settings = new HashMap<>();

        // デフォルト設定を投入
        loadDefaultSettings();

        // 既存レジストリの読み込み。存在すればマージする。
        loadFromStorage();

        System.out.println("SettingsManager: 設定サービスを初期化完了");
    }

    /** デフォルト設定を登録。存在しないキーのみ採用する。 */
    private void loadDefaultSettings() {
        // 一般設定
        settings.putIfAbsent("display_brightness", 75);
        settings.putIfAbsent("sound_enabled", true);

        // UIレジストリ（テーマ・外観）
        settings.putIfAbsent("ui.theme.mode", "light"); // legacy
        settings.putIfAbsent("ui.theme.tone", "light");  // new: light | dark | auto
        settings.putIfAbsent("ui.theme.family", "white"); // new: white | orange | yellow | pink | green | black

        settings.putIfAbsent("ui.theme.seed_color", "#4A90E2");
        settings.putIfAbsent("ui.theme.accent_color", null);
        settings.putIfAbsent("ui.theme.use_dynamic_palette", true);
        settings.putIfAbsent("ui.theme.contrast", "normal"); // normal | high
        settings.putIfAbsent("ui.typography.base_size", 14);
        settings.putIfAbsent("ui.motion.reduce", false);
        settings.putIfAbsent("ui.performance.low_power", false);
        settings.putIfAbsent("ui.shape.corner_scale", "standard"); // compact | standard | rounded
        settings.putIfAbsent("ui.effects.elevation_scale", 1.0);

        // Battery（バッテリー設定）
        settings.putIfAbsent("power.battery_saver.enabled", false);
        settings.putIfAbsent("power.battery_saver.auto", true);
        settings.putIfAbsent("power.battery_saver.threshold", 20);
        settings.putIfAbsent("display.screen_timeout", 30); // 秒単位

        // Sound & Vibration（音声・振動設定）
        settings.putIfAbsent("audio.master_volume", 75); // 0-100
        settings.putIfAbsent("audio.notification_sound", true);
        settings.putIfAbsent("audio.touch_sound", true);
        settings.putIfAbsent("audio.vibration", true);
        settings.putIfAbsent("audio.ringtone", "default"); // 着信音ID

        System.out.println("SettingsManager: Default settings loaded");
    }

    /** ストレージからJSONを読み出して設定に反映 */
    private void loadFromStorage() {
        if (vfs == null) return;
        String json = vfs.readFile(REGISTRY_PATH);
        if (json == null || json.isEmpty()) {
            // ない場合は初期を書き込む
            saveSettings();
            return;
        }
        try {
            Type type = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> loaded = gson.fromJson(json, type);
            if (loaded != null) {
                settings.putAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("SettingsManager: JSON読み込みエラー: " + e.getMessage());
        }
    }

    /** 設定値の取得 */
    public Object getSetting(String key) {
        Object value = settings.get(key);
        //System.out.println("SettingsManager: Getting setting " + key + " = " + value);
        return value;
    }

    /** 設定値の更新＋リスナーへの通知。永続化は呼び出し側で適宜！ */
    public void setSetting(String key, Object value) {
        settings.put(key, value);
        for (SettingsListener l : listeners) {
            try { l.onSettingChanged(key, value); } catch (Exception ignored) {}
        }
        System.out.println("SettingsManager: Setting " + key + " = " + value);
    }

    /** 型安全な取得ユーティリティ */
    public String getStringSetting(String key, String defaultValue) {
        Object value = settings.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    public int getIntSetting(String key, int defaultValue) {
        Object value = settings.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return value != null ? Integer.parseInt(value.toString()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public boolean getBooleanSetting(String key, boolean defaultValue) {
        Object value = settings.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value != null) {
            return Boolean.parseBoolean(value.toString());
        }
        return defaultValue;
    }

    public double getDoubleSetting(String key, double defaultValue) {
        Object value = settings.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return value != null ? Double.parseDouble(value.toString()) : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /** JSONとして永続化 */
    public boolean saveSettings() {
        if (vfs == null) return false;
        try {
            String json = gson.toJson(settings);
            return vfs.writeFile(REGISTRY_PATH, json);
        } catch (Exception e) {
            System.err.println("SettingsManager: 保存エラー: " + e.getMessage());
            return false;
        }
    }

    /** リスナー登録/解除 */
    public void addListener(SettingsListener listener) { if (listener != null) listeners.add(listener); }
    public void removeListener(SettingsListener listener) { if (listener != null) listeners.remove(listener); }
}
