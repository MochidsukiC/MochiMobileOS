package jp.moyashi.phoneos.api.proxy;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import jp.moyashi.phoneos.api.*;
import jp.moyashi.phoneos.api.clipboard.ClipData;
import jp.moyashi.phoneos.api.clipboard.ClipboardManager;
import jp.moyashi.phoneos.api.hardware.*;
import jp.moyashi.phoneos.api.intent.ActivityInfo;
import jp.moyashi.phoneos.api.intent.ActivityManager;
import jp.moyashi.phoneos.api.intent.Intent;
import jp.moyashi.phoneos.api.network.*;
import jp.moyashi.phoneos.api.permission.Permission;
import jp.moyashi.phoneos.api.permission.PermissionCallback;
import jp.moyashi.phoneos.api.permission.PermissionManager;
import jp.moyashi.phoneos.api.permission.PermissionResult;
import jp.moyashi.phoneos.api.sensor.Sensor;
import jp.moyashi.phoneos.api.sensor.SensorEvent;
import jp.moyashi.phoneos.api.sensor.SensorEventListener;
import jp.moyashi.phoneos.api.sensor.SensorManager;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IPC経由のAppContext実装。
 * Forge JVMで動作し、API呼び出しをServer JVMに転送する。
 *
 * 開発者はこのクラスのIPCの存在を意識する必要がない。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class AppContextProxy implements AppContext {

    private static final Gson GSON = new Gson();

    private final IPCChannel channel;
    private final String appId;
    private final String appName;
    private final AppStorageProxy storage;
    private final AppSettingsProxy settings;
    private SystemInfoProxy systemInfo;

    // ハードウェアAPI
    private BatteryInfoProxy batteryInfo;
    private LocationSocketProxy locationSocket;
    private BluetoothSocketProxy bluetoothSocket;
    private MobileDataSocketProxy mobileDataSocket;
    private SIMInfoProxy simInfo;
    private CameraSocketProxy cameraSocket;
    private MicrophoneSocketProxy microphoneSocket;
    private SpeakerSocketProxy speakerSocket;
    private ICSocketProxy icSocket;

    // その他API
    private SensorManagerProxy sensorManager;
    private PermissionManagerProxy permissionManager;
    private ActivityManagerProxy activityManager;
    private ClipboardManagerProxy clipboardManager;
    private NetworkManagerProxy networkManager;

    /**
     * AppContextProxyを作成する。
     *
     * @param channel IPCチャンネル
     * @param appId アプリID
     * @param appName アプリ名
     */
    public AppContextProxy(IPCChannel channel, String appId, String appName) {
        this.channel = channel;
        this.appId = appId;
        this.appName = appName;
        this.storage = new AppStorageProxy(channel, appId);
        this.settings = new AppSettingsProxy(channel, appId);
    }

    @Override
    public AppStorage getStorage() {
        return storage;
    }

    @Override
    public AppSettings getSettings() {
        return settings;
    }

    @Override
    public void sendNotification(String title, String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("appId", appId);
        payload.addProperty("title", title);
        payload.addProperty("message", message);
        payload.addProperty("priority", "NORMAL");
        channel.send(IPCChannel.MSG_NOTIFICATION_SEND, GSON.toJson(payload));
    }

    @Override
    public void sendNotification(Notification notification) {
        JsonObject payload = new JsonObject();
        payload.addProperty("appId", appId);
        payload.addProperty("title", notification.getTitle());
        payload.addProperty("message", notification.getMessage());
        payload.addProperty("priority", notification.getPriority().name());
        if (notification.getIconPath() != null) {
            payload.addProperty("iconPath", notification.getIconPath());
        }
        channel.send(IPCChannel.MSG_NOTIFICATION_SEND, GSON.toJson(payload));
    }

    @Override
    public void pushScreen(ModScreen screen) {
        // 画面遷移はForge側で管理（Serverにはピクセルのみ送信）
        // 実際の実装はModAppRunnerで行う
    }

    @Override
    public void popScreen() {
        // 画面遷移はForge側で管理
    }

    @Override
    public void replaceScreen(ModScreen screen) {
        // 画面遷移はForge側で管理
    }

    @Override
    public String getAppId() {
        return appId;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public SystemInfo getSystemInfo() {
        if (systemInfo == null) {
            systemInfo = new SystemInfoProxy(channel);
        }
        return systemInfo;
    }

    @Override
    public void logDebug(String message) {
        sendLog("DEBUG", message);
    }

    @Override
    public void logInfo(String message) {
        sendLog("INFO", message);
    }

    @Override
    public void logWarn(String message) {
        sendLog("WARN", message);
    }

    @Override
    public void logError(String message) {
        sendLog("ERROR", message);
    }

    @Override
    public void logError(String message, Throwable throwable) {
        sendLog("ERROR", message + ": " + throwable.getMessage());
    }

    private void sendLog(String level, String message) {
        JsonObject payload = new JsonObject();
        payload.addProperty("appId", appId);
        payload.addProperty("level", level);
        payload.addProperty("message", message);
        channel.send(IPCChannel.MSG_LOG, GSON.toJson(payload));
    }

    @Override
    public String getResourcePath(String resourcePath) {
        // MODリソースパスを構築
        return "mmos_mod/" + appId + "/" + resourcePath;
    }

    // ========================================
    // ハードウェアAPI
    // ========================================

    @Override
    public BatteryInfo getBatteryInfo() {
        if (batteryInfo == null) {
            batteryInfo = new BatteryInfoProxy(channel);
        }
        return batteryInfo;
    }

    @Override
    public LocationSocket getLocationSocket() {
        if (locationSocket == null) {
            locationSocket = new LocationSocketProxy(channel, appId);
        }
        return locationSocket;
    }

    @Override
    public BluetoothSocket getBluetoothSocket() {
        if (bluetoothSocket == null) {
            bluetoothSocket = new BluetoothSocketProxy(channel, appId);
        }
        return bluetoothSocket;
    }

    @Override
    public MobileDataSocket getMobileDataSocket() {
        if (mobileDataSocket == null) {
            mobileDataSocket = new MobileDataSocketProxy(channel, appId);
        }
        return mobileDataSocket;
    }

    @Override
    public SIMInfo getSIMInfo() {
        if (simInfo == null) {
            simInfo = new SIMInfoProxy(channel);
        }
        return simInfo;
    }

    @Override
    public CameraSocket getCameraSocket() {
        if (cameraSocket == null) {
            cameraSocket = new CameraSocketProxy(channel, appId);
        }
        return cameraSocket;
    }

    @Override
    public MicrophoneSocket getMicrophoneSocket() {
        if (microphoneSocket == null) {
            microphoneSocket = new MicrophoneSocketProxy(channel, appId);
        }
        return microphoneSocket;
    }

    @Override
    public SpeakerSocket getSpeakerSocket() {
        if (speakerSocket == null) {
            speakerSocket = new SpeakerSocketProxy(channel, appId);
        }
        return speakerSocket;
    }

    @Override
    public ICSocket getICSocket() {
        if (icSocket == null) {
            icSocket = new ICSocketProxy(channel, appId);
        }
        return icSocket;
    }

    // ========================================
    // センサー/パーミッション/インテント/クリップボードAPI
    // ========================================

    @Override
    public SensorManager getSensorManager() {
        if (sensorManager == null) {
            sensorManager = new SensorManagerProxy(channel, appId);
        }
        return sensorManager;
    }

    @Override
    public PermissionManager getPermissionManager() {
        if (permissionManager == null) {
            permissionManager = new PermissionManagerProxy(channel, appId);
        }
        return permissionManager;
    }

    @Override
    public ActivityManager getActivityManager() {
        if (activityManager == null) {
            activityManager = new ActivityManagerProxy(channel, appId);
        }
        return activityManager;
    }

    @Override
    public boolean startActivity(Intent intent) {
        return getActivityManager().startActivity(intent);
    }

    @Override
    public ClipboardManager getClipboardManager() {
        if (clipboardManager == null) {
            clipboardManager = new ClipboardManagerProxy(channel, appId);
        }
        return clipboardManager;
    }

    @Override
    public NetworkManager getNetworkManager() {
        if (networkManager == null) {
            networkManager = new NetworkManagerProxy(channel, appId);
        }
        return networkManager;
    }

    /**
     * 画面遷移コールバックを設定する。
     * 内部使用のみ。
     */
    public void setScreenNavigationCallback(ScreenNavigationCallback callback) {
        this.screenCallback = callback;
    }

    private ScreenNavigationCallback screenCallback;

    /**
     * 画面遷移コールバック。
     */
    public interface ScreenNavigationCallback {
        void onPushScreen(ModScreen screen);
        void onPopScreen();
        void onReplaceScreen(ModScreen screen);
    }

    // ========================================
    // 内部クラス: AppStorageProxy
    // ========================================

    private static class AppStorageProxy implements AppStorage {
        private final IPCChannel channel;
        private final String appId;

        AppStorageProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        @Override
        public String readFile(String path) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("path", path);
            String response = channel.sendAndReceive(IPCChannel.MSG_STORAGE_READ, GSON.toJson(payload));
            if (response == null) return null;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            if (result.has("content")) {
                return result.get("content").isJsonNull() ? null : result.get("content").getAsString();
            }
            return null;
        }

        @Override
        public boolean writeFile(String path, String content) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("path", path);
            payload.addProperty("content", content);
            String response = channel.sendAndReceive(IPCChannel.MSG_STORAGE_WRITE, GSON.toJson(payload));
            if (response == null) return false;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            return result.has("success") && result.get("success").getAsBoolean();
        }

        @Override
        public byte[] readBytes(String path) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("path", path);
            String response = channel.sendAndReceive(IPCChannel.MSG_STORAGE_READ_BYTES, GSON.toJson(payload));
            if (response == null) return null;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            if (result.has("data")) {
                String base64 = result.get("data").getAsString();
                return Base64.getDecoder().decode(base64);
            }
            return null;
        }

        @Override
        public boolean writeBytes(String path, byte[] data) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("path", path);
            payload.addProperty("data", Base64.getEncoder().encodeToString(data));
            String response = channel.sendAndReceive(IPCChannel.MSG_STORAGE_WRITE_BYTES, GSON.toJson(payload));
            if (response == null) return false;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            return result.has("success") && result.get("success").getAsBoolean();
        }

        @Override
        public boolean exists(String path) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("path", path);
            String response = channel.sendAndReceive(IPCChannel.MSG_STORAGE_EXISTS, GSON.toJson(payload));
            if (response == null) return false;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            return result.has("exists") && result.get("exists").getAsBoolean();
        }

        @Override
        public boolean delete(String path) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("path", path);
            String response = channel.sendAndReceive(IPCChannel.MSG_STORAGE_DELETE, GSON.toJson(payload));
            if (response == null) return false;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            return result.has("success") && result.get("success").getAsBoolean();
        }

        @Override
        public List<String> list(String directory) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("path", directory);
            String response = channel.sendAndReceive(IPCChannel.MSG_STORAGE_LIST, GSON.toJson(payload));
            if (response == null) return Collections.emptyList();
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            if (result.has("files")) {
                Type listType = new TypeToken<List<String>>(){}.getType();
                return GSON.fromJson(result.get("files"), listType);
            }
            return Collections.emptyList();
        }

        @Override
        public boolean mkdir(String path) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("path", path);
            String response = channel.sendAndReceive(IPCChannel.MSG_STORAGE_MKDIR, GSON.toJson(payload));
            if (response == null) return false;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            return result.has("success") && result.get("success").getAsBoolean();
        }

        @Override
        public boolean isFile(String path) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("path", path);
            String response = channel.sendAndReceive(IPCChannel.MSG_STORAGE_IS_FILE, GSON.toJson(payload));
            if (response == null) return false;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            return result.has("isFile") && result.get("isFile").getAsBoolean();
        }

        @Override
        public boolean isDirectory(String path) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("path", path);
            String response = channel.sendAndReceive(IPCChannel.MSG_STORAGE_IS_DIR, GSON.toJson(payload));
            if (response == null) return false;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            return result.has("isDir") && result.get("isDir").getAsBoolean();
        }
    }

    // ========================================
    // 内部クラス: AppSettingsProxy
    // ========================================

    private static class AppSettingsProxy implements AppSettings {
        private final IPCChannel channel;
        private final String appId;

        AppSettingsProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        @Override
        public String getString(String key, String defaultValue) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("key", key);
            payload.addProperty("type", "string");
            String response = channel.sendAndReceive(IPCChannel.MSG_SETTINGS_GET, GSON.toJson(payload));
            if (response == null) return defaultValue;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            if (result.has("value") && !result.get("value").isJsonNull()) {
                return result.get("value").getAsString();
            }
            return defaultValue;
        }

        @Override
        public void setString(String key, String value) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("key", key);
            payload.addProperty("value", value);
            payload.addProperty("type", "string");
            channel.send(IPCChannel.MSG_SETTINGS_SET, GSON.toJson(payload));
        }

        @Override
        public int getInt(String key, int defaultValue) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("key", key);
            payload.addProperty("type", "int");
            String response = channel.sendAndReceive(IPCChannel.MSG_SETTINGS_GET, GSON.toJson(payload));
            if (response == null) return defaultValue;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            if (result.has("value") && !result.get("value").isJsonNull()) {
                return result.get("value").getAsInt();
            }
            return defaultValue;
        }

        @Override
        public void setInt(String key, int value) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("key", key);
            payload.addProperty("value", value);
            payload.addProperty("type", "int");
            channel.send(IPCChannel.MSG_SETTINGS_SET, GSON.toJson(payload));
        }

        @Override
        public float getFloat(String key, float defaultValue) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("key", key);
            payload.addProperty("type", "float");
            String response = channel.sendAndReceive(IPCChannel.MSG_SETTINGS_GET, GSON.toJson(payload));
            if (response == null) return defaultValue;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            if (result.has("value") && !result.get("value").isJsonNull()) {
                return result.get("value").getAsFloat();
            }
            return defaultValue;
        }

        @Override
        public void setFloat(String key, float value) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("key", key);
            payload.addProperty("value", value);
            payload.addProperty("type", "float");
            channel.send(IPCChannel.MSG_SETTINGS_SET, GSON.toJson(payload));
        }

        @Override
        public boolean getBoolean(String key, boolean defaultValue) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("key", key);
            payload.addProperty("type", "boolean");
            String response = channel.sendAndReceive(IPCChannel.MSG_SETTINGS_GET, GSON.toJson(payload));
            if (response == null) return defaultValue;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            if (result.has("value") && !result.get("value").isJsonNull()) {
                return result.get("value").getAsBoolean();
            }
            return defaultValue;
        }

        @Override
        public void setBoolean(String key, boolean value) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("key", key);
            payload.addProperty("value", value);
            payload.addProperty("type", "boolean");
            channel.send(IPCChannel.MSG_SETTINGS_SET, GSON.toJson(payload));
        }

        @Override
        public boolean contains(String key) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("key", key);
            String response = channel.sendAndReceive(IPCChannel.MSG_SETTINGS_CONTAINS, GSON.toJson(payload));
            if (response == null) return false;
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            return result.has("contains") && result.get("contains").getAsBoolean();
        }

        @Override
        public void remove(String key) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("key", key);
            channel.send(IPCChannel.MSG_SETTINGS_REMOVE, GSON.toJson(payload));
        }

        @Override
        public Set<String> keys() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_SETTINGS_KEYS, GSON.toJson(payload));
            if (response == null) return Collections.emptySet();
            JsonObject result = GSON.fromJson(response, JsonObject.class);
            if (result.has("keys")) {
                Type setType = new TypeToken<Set<String>>(){}.getType();
                return GSON.fromJson(result.get("keys"), setType);
            }
            return Collections.emptySet();
        }

        @Override
        public void save() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            channel.send(IPCChannel.MSG_SETTINGS_SAVE, GSON.toJson(payload));
        }
    }

    // ========================================
    // 内部クラス: SystemInfoProxy
    // ========================================

    private static class SystemInfoProxy implements SystemInfo {
        private final IPCChannel channel;
        private JsonObject cachedInfo;
        private long cacheTime;
        private static final long CACHE_DURATION = 1000; // 1秒キャッシュ

        SystemInfoProxy(IPCChannel channel) {
            this.channel = channel;
        }

        private JsonObject getInfo() {
            long now = System.currentTimeMillis();
            if (cachedInfo == null || now - cacheTime > CACHE_DURATION) {
                String response = channel.sendAndReceive(IPCChannel.MSG_SYSTEM_INFO, "{}");
                if (response != null) {
                    cachedInfo = GSON.fromJson(response, JsonObject.class);
                    cacheTime = now;
                }
            }
            return cachedInfo;
        }

        @Override
        public String getOsVersion() {
            JsonObject info = getInfo();
            return info != null && info.has("osVersion") ? info.get("osVersion").getAsString() : "Unknown";
        }

        @Override
        public int getScreenWidth() {
            JsonObject info = getInfo();
            return info != null && info.has("screenWidth") ? info.get("screenWidth").getAsInt() : 320;
        }

        @Override
        public int getScreenHeight() {
            JsonObject info = getInfo();
            return info != null && info.has("screenHeight") ? info.get("screenHeight").getAsInt() : 568;
        }

        @Override
        public int getBatteryLevel() {
            JsonObject info = getInfo();
            return info != null && info.has("batteryLevel") ? info.get("batteryLevel").getAsInt() : 100;
        }

        @Override
        public boolean isCharging() {
            JsonObject info = getInfo();
            return info != null && info.has("isCharging") && info.get("isCharging").getAsBoolean();
        }

        @Override
        public boolean isWifiConnected() {
            JsonObject info = getInfo();
            return info != null && info.has("isWifiConnected") && info.get("isWifiConnected").getAsBoolean();
        }

        @Override
        public long getCurrentTimeMillis() {
            return System.currentTimeMillis();
        }

        @Override
        public String getWorldId() {
            JsonObject info = getInfo();
            return info != null && info.has("worldId") ? info.get("worldId").getAsString() : "unknown";
        }
    }

    // ========================================
    // 内部クラス: BatteryInfoProxy
    // ========================================

    private static class BatteryInfoProxy implements BatteryInfo {
        private final IPCChannel channel;
        private JsonObject cachedInfo;
        private long cacheTime;
        private static final long CACHE_DURATION = 500;

        BatteryInfoProxy(IPCChannel channel) {
            this.channel = channel;
        }

        private JsonObject getInfo() {
            long now = System.currentTimeMillis();
            if (cachedInfo == null || now - cacheTime > CACHE_DURATION) {
                String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_BATTERY, "{}");
                if (response != null) {
                    cachedInfo = GSON.fromJson(response, JsonObject.class);
                    cacheTime = now;
                }
            }
            return cachedInfo;
        }

        @Override
        public int getBatteryLevel() {
            JsonObject info = getInfo();
            return info != null && info.has("level") ? info.get("level").getAsInt() : 100;
        }

        @Override
        public int getBatteryHealth() {
            JsonObject info = getInfo();
            return info != null && info.has("health") ? info.get("health").getAsInt() : 100;
        }

        @Override
        public boolean isCharging() {
            JsonObject info = getInfo();
            return info != null && info.has("isCharging") && info.get("isCharging").getAsBoolean();
        }
    }

    // ========================================
    // 内部クラス: LocationSocketProxy
    // ========================================

    private static class LocationSocketProxy implements LocationSocket {
        private final IPCChannel channel;
        private final String appId;

        LocationSocketProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        private JsonObject getInfo() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_LOCATION, GSON.toJson(payload));
            return response != null ? GSON.fromJson(response, JsonObject.class) : null;
        }

        @Override
        public boolean isAvailable() {
            JsonObject info = getInfo();
            return info != null && info.has("available") && info.get("available").getAsBoolean();
        }

        @Override
        public LocationData getLocation() {
            JsonObject info = getInfo();
            if (info != null) {
                double x = info.has("x") ? info.get("x").getAsDouble() : 0.0;
                double y = info.has("y") ? info.get("y").getAsDouble() : 0.0;
                double z = info.has("z") ? info.get("z").getAsDouble() : 0.0;
                float accuracy = info.has("accuracy") ? info.get("accuracy").getAsFloat() : 0.0f;
                return new LocationData(x, y, z, accuracy);
            }
            return new LocationData(0, 0, 0, 0);
        }

        @Override
        public void setEnabled(boolean enabled) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("enabled", enabled);
            channel.send(IPCChannel.MSG_HARDWARE_LOCATION_SET, GSON.toJson(payload));
        }

        @Override
        public boolean isEnabled() {
            JsonObject info = getInfo();
            return info != null && info.has("enabled") && info.get("enabled").getAsBoolean();
        }
    }

    // ========================================
    // 内部クラス: BluetoothSocketProxy
    // ========================================

    private static class BluetoothSocketProxy implements BluetoothSocket {
        private final IPCChannel channel;
        private final String appId;

        BluetoothSocketProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        private JsonObject getInfo() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_BLUETOOTH, GSON.toJson(payload));
            return response != null ? GSON.fromJson(response, JsonObject.class) : null;
        }

        @Override
        public boolean isAvailable() {
            JsonObject info = getInfo();
            return info != null && info.has("available") && info.get("available").getAsBoolean();
        }

        @Override
        public List<BluetoothDevice> scanNearbyDevices() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("scan", true);
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_BLUETOOTH, GSON.toJson(payload));
            List<BluetoothDevice> devices = new ArrayList<>();
            if (response != null) {
                JsonObject info = GSON.fromJson(response, JsonObject.class);
                if (info.has("nearbyDevices")) {
                    JsonArray arr = info.getAsJsonArray("nearbyDevices");
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject dev = arr.get(i).getAsJsonObject();
                        String name = dev.has("name") ? dev.get("name").getAsString() : "";
                        String addr = dev.has("address") ? dev.get("address").getAsString() : "";
                        double distance = dev.has("distance") ? dev.get("distance").getAsDouble() : 0.0;
                        devices.add(new BluetoothDevice(name, addr, distance));
                    }
                }
            }
            return devices;
        }

        @Override
        public List<BluetoothDevice> getConnectedDevices() {
            JsonObject info = getInfo();
            List<BluetoothDevice> devices = new ArrayList<>();
            if (info != null && info.has("connectedDevices")) {
                JsonArray arr = info.getAsJsonArray("connectedDevices");
                for (int i = 0; i < arr.size(); i++) {
                    JsonObject dev = arr.get(i).getAsJsonObject();
                    String name = dev.has("name") ? dev.get("name").getAsString() : "";
                    String addr = dev.has("address") ? dev.get("address").getAsString() : "";
                    double distance = dev.has("distance") ? dev.get("distance").getAsDouble() : 0.0;
                    devices.add(new BluetoothDevice(name, addr, distance));
                }
            }
            return devices;
        }

        @Override
        public boolean connect(String address) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("address", address);
            payload.addProperty("connect", true);
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_BLUETOOTH_CONNECT, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                return result.has("success") && result.get("success").getAsBoolean();
            }
            return false;
        }

        @Override
        public void disconnect(String address) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("address", address);
            payload.addProperty("connect", false);
            channel.send(IPCChannel.MSG_HARDWARE_BLUETOOTH_CONNECT, GSON.toJson(payload));
        }
    }

    // ========================================
    // 内部クラス: MobileDataSocketProxy
    // ========================================

    private static class MobileDataSocketProxy implements MobileDataSocket {
        private final IPCChannel channel;
        private final String appId;

        MobileDataSocketProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        private JsonObject getInfo() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_MOBILE_DATA, GSON.toJson(payload));
            return response != null ? GSON.fromJson(response, JsonObject.class) : null;
        }

        @Override
        public boolean isAvailable() {
            JsonObject info = getInfo();
            return info != null && info.has("available") && info.get("available").getAsBoolean();
        }

        @Override
        public int getSignalStrength() {
            JsonObject info = getInfo();
            return info != null && info.has("signalStrength") ? info.get("signalStrength").getAsInt() : 0;
        }

        @Override
        public String getServiceName() {
            JsonObject info = getInfo();
            return info != null && info.has("serviceName") ? info.get("serviceName").getAsString() : "";
        }

        @Override
        public boolean isConnected() {
            JsonObject info = getInfo();
            return info != null && info.has("connected") && info.get("connected").getAsBoolean();
        }
    }

    // ========================================
    // 内部クラス: SIMInfoProxy
    // ========================================

    private static class SIMInfoProxy implements SIMInfo {
        private final IPCChannel channel;

        SIMInfoProxy(IPCChannel channel) {
            this.channel = channel;
        }

        private JsonObject getInfo() {
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_SIM, "{}");
            return response != null ? GSON.fromJson(response, JsonObject.class) : null;
        }

        @Override
        public boolean isAvailable() {
            JsonObject info = getInfo();
            return info != null && info.has("available") && info.get("available").getAsBoolean();
        }

        @Override
        public String getOwnerName() {
            JsonObject info = getInfo();
            return info != null && info.has("ownerName") ? info.get("ownerName").getAsString() : "";
        }

        @Override
        public String getOwnerUUID() {
            JsonObject info = getInfo();
            return info != null && info.has("ownerUUID") ? info.get("ownerUUID").getAsString() : "";
        }

        @Override
        public boolean isInserted() {
            JsonObject info = getInfo();
            return info != null && info.has("inserted") && info.get("inserted").getAsBoolean();
        }
    }

    // ========================================
    // 内部クラス: CameraSocketProxy
    // ========================================

    private static class CameraSocketProxy implements CameraSocket {
        private final IPCChannel channel;
        private final String appId;

        CameraSocketProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        private JsonObject getInfo() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_CAMERA, GSON.toJson(payload));
            return response != null ? GSON.fromJson(response, JsonObject.class) : null;
        }

        @Override
        public boolean isAvailable() {
            JsonObject info = getInfo();
            return info != null && info.has("available") && info.get("available").getAsBoolean();
        }

        @Override
        public void setEnabled(boolean enabled) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("enabled", enabled);
            channel.send(IPCChannel.MSG_HARDWARE_CAMERA_SET, GSON.toJson(payload));
        }

        @Override
        public boolean isEnabled() {
            JsonObject info = getInfo();
            return info != null && info.has("enabled") && info.get("enabled").getAsBoolean();
        }

        @Override
        public int[] getCurrentFramePixels() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("getFrame", true);
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_CAMERA, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("pixels") && !result.get("pixels").isJsonNull()) {
                    byte[] bytes = Base64.getDecoder().decode(result.get("pixels").getAsString());
                    int[] pixels = new int[bytes.length / 4];
                    for (int i = 0; i < pixels.length; i++) {
                        pixels[i] = ((bytes[i * 4] & 0xFF) << 24) |
                                ((bytes[i * 4 + 1] & 0xFF) << 16) |
                                ((bytes[i * 4 + 2] & 0xFF) << 8) |
                                (bytes[i * 4 + 3] & 0xFF);
                    }
                    return pixels;
                }
            }
            return null;
        }

        @Override
        public int getFrameWidth() {
            JsonObject info = getInfo();
            return info != null && info.has("frameWidth") ? info.get("frameWidth").getAsInt() : 640;
        }

        @Override
        public int getFrameHeight() {
            JsonObject info = getInfo();
            return info != null && info.has("frameHeight") ? info.get("frameHeight").getAsInt() : 480;
        }
    }

    // ========================================
    // 内部クラス: MicrophoneSocketProxy
    // ========================================

    private static class MicrophoneSocketProxy implements MicrophoneSocket {
        private final IPCChannel channel;
        private final String appId;

        MicrophoneSocketProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        private JsonObject getInfo() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_MICROPHONE, GSON.toJson(payload));
            return response != null ? GSON.fromJson(response, JsonObject.class) : null;
        }

        @Override
        public boolean isAvailable() {
            JsonObject info = getInfo();
            return info != null && info.has("available") && info.get("available").getAsBoolean();
        }

        @Override
        public void setEnabled(boolean enabled) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("enabled", enabled);
            channel.send(IPCChannel.MSG_HARDWARE_MICROPHONE_SET, GSON.toJson(payload));
        }

        @Override
        public boolean isEnabled() {
            JsonObject info = getInfo();
            return info != null && info.has("enabled") && info.get("enabled").getAsBoolean();
        }

        @Override
        public byte[] getMicrophoneAudio() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("channel", "microphone");
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_MICROPHONE, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("audio") && !result.get("audio").isJsonNull()) {
                    return Base64.getDecoder().decode(result.get("audio").getAsString());
                }
            }
            return null;
        }

        @Override
        public byte[] getVoicechatAudio() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("channel", "voicechat");
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_MICROPHONE, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("audio") && !result.get("audio").isJsonNull()) {
                    return Base64.getDecoder().decode(result.get("audio").getAsString());
                }
            }
            return null;
        }

        @Override
        public byte[] getEnvironmentAudio() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("channel", "environment");
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_MICROPHONE, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("audio") && !result.get("audio").isJsonNull()) {
                    return Base64.getDecoder().decode(result.get("audio").getAsString());
                }
            }
            return null;
        }

        @Override
        public float getSampleRate() {
            JsonObject info = getInfo();
            return info != null && info.has("sampleRate") ? info.get("sampleRate").getAsFloat() : 48000f;
        }
    }

    // ========================================
    // 内部クラス: SpeakerSocketProxy
    // ========================================

    private static class SpeakerSocketProxy implements SpeakerSocket {
        private final IPCChannel channel;
        private final String appId;

        SpeakerSocketProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        private JsonObject getInfo() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_SPEAKER, GSON.toJson(payload));
            return response != null ? GSON.fromJson(response, JsonObject.class) : null;
        }

        @Override
        public boolean isAvailable() {
            JsonObject info = getInfo();
            return info != null && info.has("available") && info.get("available").getAsBoolean();
        }

        @Override
        public void setVolumeLevel(VolumeLevel level) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("volumeLevel", level.name());
            channel.send(IPCChannel.MSG_HARDWARE_SPEAKER_SET, GSON.toJson(payload));
        }

        @Override
        public VolumeLevel getVolumeLevel() {
            JsonObject info = getInfo();
            if (info != null && info.has("volumeLevel")) {
                try {
                    return VolumeLevel.valueOf(info.get("volumeLevel").getAsString());
                } catch (IllegalArgumentException e) {
                    return VolumeLevel.MEDIUM;
                }
            }
            return VolumeLevel.MEDIUM;
        }

        @Override
        public void playAudio(byte[] audioData) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("audio", Base64.getEncoder().encodeToString(audioData));
            channel.send(IPCChannel.MSG_HARDWARE_SPEAKER_SET, GSON.toJson(payload));
        }

        @Override
        public void stopAudio() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("stop", true);
            channel.send(IPCChannel.MSG_HARDWARE_SPEAKER_SET, GSON.toJson(payload));
        }
    }

    // ========================================
    // 内部クラス: ICSocketProxy
    // ========================================

    private static class ICSocketProxy implements ICSocket {
        private final IPCChannel channel;
        private final String appId;

        ICSocketProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        private JsonObject getInfo() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_IC, GSON.toJson(payload));
            return response != null ? GSON.fromJson(response, JsonObject.class) : null;
        }

        @Override
        public boolean isAvailable() {
            JsonObject info = getInfo();
            return info != null && info.has("available") && info.get("available").getAsBoolean();
        }

        @Override
        public void setEnabled(boolean enabled) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("enabled", enabled);
            channel.send(IPCChannel.MSG_HARDWARE_IC_SET, GSON.toJson(payload));
        }

        @Override
        public boolean isEnabled() {
            JsonObject info = getInfo();
            return info != null && info.has("enabled") && info.get("enabled").getAsBoolean();
        }

        @Override
        public ICData pollICData() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_HARDWARE_IC_POLL, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("type")) {
                    String typeStr = result.get("type").getAsString();
                    ICDataType dataType;
                    try {
                        dataType = ICDataType.valueOf(typeStr);
                    } catch (IllegalArgumentException e) {
                        dataType = ICDataType.NONE;
                    }

                    switch (dataType) {
                        case BLOCK:
                            int x = result.has("blockX") ? result.get("blockX").getAsInt() : 0;
                            int y = result.has("blockY") ? result.get("blockY").getAsInt() : 0;
                            int z = result.has("blockZ") ? result.get("blockZ").getAsInt() : 0;
                            return new ICData(x, y, z);
                        case ENTITY:
                            String uuid = result.has("entityUUID") ? result.get("entityUUID").getAsString() : "";
                            return new ICData(uuid);
                        default:
                            return new ICData();
                    }
                }
            }
            return new ICData();
        }
    }

    // ========================================
    // 内部クラス: SensorManagerProxy
    // ========================================

    private static class SensorManagerProxy implements SensorManager {
        private final IPCChannel channel;
        private final String appId;
        private final Map<SensorEventListener, Integer> registeredListeners = new ConcurrentHashMap<>();

        SensorManagerProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        @Override
        public List<Sensor> getSensorList(int type) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("type", type);
            String response = channel.sendAndReceive(IPCChannel.MSG_SENSOR_LIST, GSON.toJson(payload));
            List<Sensor> sensors = new ArrayList<>();
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("sensors")) {
                    JsonArray arr = result.getAsJsonArray("sensors");
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject s = arr.get(i).getAsJsonObject();
                        sensors.add(jsonToSensor(s));
                    }
                }
            }
            return sensors;
        }

        @Override
        public Sensor getDefaultSensor(int type) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("type", type);
            String response = channel.sendAndReceive(IPCChannel.MSG_SENSOR_DEFAULT, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("sensor")) {
                    return jsonToSensor(result.getAsJsonObject("sensor"));
                }
            }
            return null;
        }

        private Sensor jsonToSensor(JsonObject json) {
            int type = json.has("type") ? json.get("type").getAsInt() : 0;
            String name = json.has("name") ? json.get("name").getAsString() : "";
            String vendor = json.has("vendor") ? json.get("vendor").getAsString() : "";
            int version = json.has("version") ? json.get("version").getAsInt() : 1;
            float maxRange = json.has("maxRange") ? json.get("maxRange").getAsFloat() : 0;
            float resolution = json.has("resolution") ? json.get("resolution").getAsFloat() : 0;
            float power = json.has("power") ? json.get("power").getAsFloat() : 0;
            int minDelay = json.has("minDelay") ? json.get("minDelay").getAsInt() : 0;
            return new Sensor(type, name, vendor, version, maxRange, resolution, power, minDelay);
        }

        @Override
        public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("sensorType", sensor.getType());
            payload.addProperty("samplingPeriod", samplingPeriodUs);
            String response = channel.sendAndReceive(IPCChannel.MSG_SENSOR_REGISTER, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("success") && result.get("success").getAsBoolean()) {
                    registeredListeners.put(listener, sensor.getType());
                    return true;
                }
            }
            return false;
        }

        @Override
        public void unregisterListener(SensorEventListener listener) {
            Integer sensorType = registeredListeners.remove(listener);
            if (sensorType != null) {
                JsonObject payload = new JsonObject();
                payload.addProperty("appId", appId);
                payload.addProperty("sensorType", sensorType);
                channel.send(IPCChannel.MSG_SENSOR_UNREGISTER, GSON.toJson(payload));
            }
        }

        @Override
        public void unregisterListener(SensorEventListener listener, Sensor sensor) {
            registeredListeners.remove(listener);
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("sensorType", sensor.getType());
            channel.send(IPCChannel.MSG_SENSOR_UNREGISTER, GSON.toJson(payload));
        }
    }

    // ========================================
    // 内部クラス: PermissionManagerProxy
    // ========================================

    private static class PermissionManagerProxy implements PermissionManager {
        private final IPCChannel channel;
        private final String appId;

        PermissionManagerProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        @Override
        public boolean hasPermission(Permission permission) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("permission", permission.name());
            String response = channel.sendAndReceive(IPCChannel.MSG_PERMISSION_CHECK, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                return result.has("granted") && result.get("granted").getAsBoolean();
            }
            return false;
        }

        @Override
        public boolean hasAllPermissions(List<Permission> permissions) {
            for (Permission p : permissions) {
                if (!hasPermission(p)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void requestPermission(Permission permission, PermissionCallback callback) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("permission", permission.name());
            String response = channel.sendAndReceive(IPCChannel.MSG_PERMISSION_REQUEST, GSON.toJson(payload));
            if (response != null && callback != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                PermissionResult permResult = PermissionResult.DENIED;
                if (result.has("result")) {
                    try {
                        permResult = PermissionResult.valueOf(result.get("result").getAsString());
                    } catch (IllegalArgumentException ignored) {}
                } else if (result.has("granted") && result.get("granted").getAsBoolean()) {
                    permResult = PermissionResult.GRANTED;
                }
                callback.onResult(permission, permResult);
            }
        }

        @Override
        public void requestPermissions(List<Permission> permissions, PermissionCallback callback) {
            for (Permission p : permissions) {
                requestPermission(p, callback);
            }
        }

        @Override
        public Set<Permission> getGrantedPermissions() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_PERMISSION_GRANTED, GSON.toJson(payload));
            Set<Permission> granted = new HashSet<>();
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("permissions")) {
                    JsonArray arr = result.getAsJsonArray("permissions");
                    for (int i = 0; i < arr.size(); i++) {
                        try {
                            granted.add(Permission.valueOf(arr.get(i).getAsString()));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            }
            return granted;
        }

        @Override
        public boolean isDeniedPermanently(Permission permission) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("permission", permission.name());
            String response = channel.sendAndReceive(IPCChannel.MSG_PERMISSION_CHECK, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                return result.has("deniedPermanently") && result.get("deniedPermanently").getAsBoolean();
            }
            return false;
        }
    }

    // ========================================
    // 内部クラス: ActivityManagerProxy
    // ========================================

    private static class ActivityManagerProxy implements ActivityManager {
        private final IPCChannel channel;
        private final String appId;

        ActivityManagerProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        @Override
        public boolean startActivity(Intent intent) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("action", intent.getAction() != null ? intent.getAction() : "");
            if (intent.getData() != null) {
                payload.addProperty("data", intent.getData());
            }
            if (intent.getType() != null) {
                payload.addProperty("type", intent.getType());
            }
            if (intent.getComponent() != null) {
                payload.addProperty("component", intent.getComponent());
            }
            JsonObject extras = new JsonObject();
            for (String key : intent.getExtras().keySet()) {
                Object val = intent.getExtras().get(key);
                if (val instanceof String) {
                    extras.addProperty(key, (String) val);
                } else if (val instanceof Number) {
                    extras.addProperty(key, (Number) val);
                } else if (val instanceof Boolean) {
                    extras.addProperty(key, (Boolean) val);
                }
            }
            payload.add("extras", extras);

            String response = channel.sendAndReceive(IPCChannel.MSG_ACTIVITY_START, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                return result.has("success") && result.get("success").getAsBoolean();
            }
            return false;
        }

        @Override
        public boolean startActivityForResult(Intent intent, ActivityResultCallback callback) {
            // 簡略実装: 結果コールバックはIPC経由では複雑なため、startActivityと同等
            return startActivity(intent);
        }

        @Override
        public boolean startActivityWithChooser(Intent intent) {
            // chooserフラグを追加
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("action", intent.getAction() != null ? intent.getAction() : "");
            payload.addProperty("chooser", true);
            if (intent.getData() != null) {
                payload.addProperty("data", intent.getData());
            }
            if (intent.getType() != null) {
                payload.addProperty("type", intent.getType());
            }
            String response = channel.sendAndReceive(IPCChannel.MSG_ACTIVITY_START, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                return result.has("success") && result.get("success").getAsBoolean();
            }
            return false;
        }

        @Override
        public List<ActivityInfo> findMatchingActivities(Intent intent) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("action", intent.getAction() != null ? intent.getAction() : "");
            if (intent.getData() != null) {
                payload.addProperty("data", intent.getData());
            }
            if (intent.getType() != null) {
                payload.addProperty("type", intent.getType());
            }

            String response = channel.sendAndReceive(IPCChannel.MSG_ACTIVITY_FIND, GSON.toJson(payload));
            List<ActivityInfo> activities = new ArrayList<>();
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("activities")) {
                    JsonArray arr = result.getAsJsonArray("activities");
                    for (int i = 0; i < arr.size(); i++) {
                        JsonObject a = arr.get(i).getAsJsonObject();
                        activities.add(jsonToActivityInfo(a));
                    }
                }
            }
            return activities;
        }

        @Override
        public ActivityInfo resolveIntent(Intent intent) {
            List<ActivityInfo> matching = findMatchingActivities(intent);
            return matching.isEmpty() ? null : matching.get(0);
        }

        private ActivityInfo jsonToActivityInfo(JsonObject json) {
            String appId = json.has("appId") ? json.get("appId").getAsString() : "";
            String appName = json.has("appName") ? json.get("appName").getAsString() : "";
            return new ActivityInfo(appId, appName);
        }
    }

    // ========================================
    // 内部クラス: ClipboardManagerProxy
    // ========================================

    private static class ClipboardManagerProxy implements ClipboardManager {
        private final IPCChannel channel;
        private final String appId;

        ClipboardManagerProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        @Override
        public void copyText(String text) {
            copyText("text", text);
        }

        @Override
        public void copyText(String label, String text) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("label", label);
            payload.addProperty("text", text);
            channel.send(IPCChannel.MSG_CLIPBOARD_COPY_TEXT, GSON.toJson(payload));
        }

        @Override
        public String pasteText() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_CLIPBOARD_PASTE_TEXT, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("text") && !result.get("text").isJsonNull()) {
                    return result.get("text").getAsString();
                }
            }
            return null;
        }

        @Override
        public void copyImage(int[] pixels, int width, int height) {
            copyImage("image", pixels, width, height);
        }

        @Override
        public void copyImage(String label, int[] pixels, int width, int height) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("label", label);
            payload.addProperty("width", width);
            payload.addProperty("height", height);
            // ピクセルデータはBase64エンコード
            byte[] bytes = new byte[pixels.length * 4];
            for (int i = 0; i < pixels.length; i++) {
                bytes[i * 4] = (byte) ((pixels[i] >> 24) & 0xFF);
                bytes[i * 4 + 1] = (byte) ((pixels[i] >> 16) & 0xFF);
                bytes[i * 4 + 2] = (byte) ((pixels[i] >> 8) & 0xFF);
                bytes[i * 4 + 3] = (byte) (pixels[i] & 0xFF);
            }
            payload.addProperty("pixels", Base64.getEncoder().encodeToString(bytes));
            channel.send(IPCChannel.MSG_CLIPBOARD_COPY_IMAGE, GSON.toJson(payload));
        }

        @Override
        public int[] pasteImagePixels() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_CLIPBOARD_PASTE_IMAGE, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("pixels") && !result.get("pixels").isJsonNull()) {
                    byte[] bytes = Base64.getDecoder().decode(result.get("pixels").getAsString());
                    int[] pixels = new int[bytes.length / 4];
                    for (int i = 0; i < pixels.length; i++) {
                        pixels[i] = ((bytes[i * 4] & 0xFF) << 24) |
                                ((bytes[i * 4 + 1] & 0xFF) << 16) |
                                ((bytes[i * 4 + 2] & 0xFF) << 8) |
                                (bytes[i * 4 + 3] & 0xFF);
                    }
                    return pixels;
                }
            }
            return null;
        }

        @Override
        public int getImageWidth() {
            ClipData clip = getPrimaryClip();
            return clip != null ? clip.getImageWidth() : 0;
        }

        @Override
        public int getImageHeight() {
            ClipData clip = getPrimaryClip();
            return clip != null ? clip.getImageHeight() : 0;
        }

        @Override
        public void copyHtml(String label, String html) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("label", label);
            payload.addProperty("html", html);
            channel.send(IPCChannel.MSG_CLIPBOARD_COPY_HTML, GSON.toJson(payload));
        }

        @Override
        public boolean hasText() {
            ClipData clip = getPrimaryClip();
            return clip != null && clip.getType() == ClipData.Type.TEXT;
        }

        @Override
        public boolean hasImage() {
            ClipData clip = getPrimaryClip();
            return clip != null && clip.getType() == ClipData.Type.IMAGE;
        }

        @Override
        public boolean hasHtml() {
            ClipData clip = getPrimaryClip();
            return clip != null && clip.getType() == ClipData.Type.HTML;
        }

        @Override
        public void clear() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            channel.send(IPCChannel.MSG_CLIPBOARD_CLEAR, GSON.toJson(payload));
        }

        @Override
        public ClipData getPrimaryClip() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_CLIPBOARD_STATUS, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("hasClip") && result.get("hasClip").getAsBoolean()) {
                    String type = result.has("type") ? result.get("type").getAsString() : "TEXT";
                    String label = result.has("label") ? result.get("label").getAsString() : "";

                    switch (type) {
                        case "TEXT":
                            String text = result.has("text") ? result.get("text").getAsString() : "";
                            return new ClipData(label, text);
                        case "HTML":
                            String html = result.has("html") ? result.get("html").getAsString() : "";
                            return new ClipData(label, html, "text/html");
                        case "IMAGE":
                            int width = result.has("width") ? result.get("width").getAsInt() : 0;
                            int height = result.has("height") ? result.get("height").getAsInt() : 0;
                            return new ClipData(label, null, width, height);
                        default:
                            return null;
                    }
                }
            }
            return null;
        }

        @Override
        public boolean hasPrimaryClip() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_CLIPBOARD_STATUS, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                return result.has("hasClip") && result.get("hasClip").getAsBoolean();
            }
            return false;
        }
    }

    // ========================================
    // 内部クラス: NetworkManagerProxy
    // ========================================

    private static class NetworkManagerProxy implements NetworkManager {
        private final IPCChannel channel;
        private final String appId;
        private final Map<String, VirtualServer> registeredServers = new ConcurrentHashMap<>();

        NetworkManagerProxy(IPCChannel channel, String appId) {
            this.channel = channel;
            this.appId = appId;
        }

        @Override
        public void request(String url, String method, NetworkCallback callback) {
            request(url, method, null, null, callback);
        }

        @Override
        public void request(String url, String method, Map<String, String> headers,
                           String body, NetworkCallback callback) {
            // 非同期で実行
            new Thread(() -> {
                try {
                    NetworkResponse response = requestSync(url, method, headers, body);
                    if (callback != null) {
                        callback.onSuccess(response);
                    }
                } catch (NetworkException e) {
                    if (callback != null) {
                        callback.onError(e);
                    }
                }
            }).start();
        }

        @Override
        public NetworkResponse requestSync(String url, String method) throws NetworkException {
            return requestSync(url, method, null, null);
        }

        @Override
        public NetworkResponse requestSync(String url, String method, Map<String, String> headers,
                                          String body) throws NetworkException {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("url", url);
            payload.addProperty("method", method);
            if (body != null) {
                payload.addProperty("body", body);
            }
            if (headers != null && !headers.isEmpty()) {
                JsonObject headersJson = new JsonObject();
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    headersJson.addProperty(entry.getKey(), entry.getValue());
                }
                payload.add("headers", headersJson);
            }

            String response = channel.sendAndReceive(IPCChannel.MSG_NETWORK_REQUEST, GSON.toJson(payload));
            if (response == null) {
                throw NetworkException.unknown("No response from server");
            }

            JsonObject result = GSON.fromJson(response, JsonObject.class);

            // エラーチェック
            if (result.has("error") && result.get("error").getAsBoolean()) {
                String errorCode = result.has("errorCode") ? result.get("errorCode").getAsString() : "UNKNOWN";
                String errorMessage = result.has("errorMessage") ? result.get("errorMessage").getAsString() : "Unknown error";

                switch (errorCode) {
                    case "NO_SERVICE":
                        throw NetworkException.noService();
                    case "TIMEOUT":
                        throw NetworkException.timeout(url);
                    case "CONNECTION_REFUSED":
                        throw NetworkException.connectionRefused(url);
                    case "HOST_NOT_FOUND":
                        throw NetworkException.hostNotFound(url);
                    case "INVALID_URL":
                        throw NetworkException.invalidUrl(url);
                    default:
                        throw NetworkException.unknown(errorMessage);
                }
            }

            // レスポンス構築
            int statusCode = result.has("statusCode") ? result.get("statusCode").getAsInt() : 200;
            String responseBody = result.has("body") ? result.get("body").getAsString() : "";
            String contentType = result.has("contentType") ? result.get("contentType").getAsString() : "text/plain";
            boolean fromVirtual = result.has("fromVirtualNetwork") && result.get("fromVirtualNetwork").getAsBoolean();

            Map<String, String> responseHeaders = new HashMap<>();
            if (result.has("headers") && result.get("headers").isJsonObject()) {
                JsonObject headersJson = result.getAsJsonObject("headers");
                for (String key : headersJson.keySet()) {
                    responseHeaders.put(key, headersJson.get(key).getAsString());
                }
            }

            return new NetworkResponse(statusCode, responseBody, contentType, responseHeaders, fromVirtual);
        }

        @Override
        public NetworkStatus getStatus() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_NETWORK_STATUS, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("status")) {
                    try {
                        return NetworkStatus.valueOf(result.get("status").getAsString());
                    } catch (IllegalArgumentException e) {
                        return NetworkStatus.ERROR;
                    }
                }
            }
            return NetworkStatus.ERROR;
        }

        @Override
        public boolean isAvailable() {
            NetworkStatus status = getStatus();
            return status == NetworkStatus.CONNECTED;
        }

        @Override
        public int getSignalStrength() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_NETWORK_STATUS, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("signalStrength")) {
                    return result.get("signalStrength").getAsInt();
                }
            }
            return 0;
        }

        @Override
        public String getCarrierName() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_NETWORK_STATUS, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("carrierName")) {
                    return result.get("carrierName").getAsString();
                }
            }
            return "Unknown";
        }

        @Override
        public boolean isIPvMAddress(String host) {
            // IPvMアドレスパターン: [0-3]-xxxx
            if (host == null || host.isEmpty()) {
                return false;
            }
            return host.matches("^[0-3]-.*");
        }

        @Override
        public IPvMAddress getMyAddress() {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            String response = channel.sendAndReceive(IPCChannel.MSG_NETWORK_MY_ADDRESS, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("address")) {
                    try {
                        return IPvMAddress.fromString(result.get("address").getAsString());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                }
            }
            return null;
        }

        @Override
        public boolean registerServer(String serverId, VirtualServer server) {
            if (serverId == null || serverId.isEmpty() || server == null) {
                return false;
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("serverId", serverId);
            payload.addProperty("description", server.getDescription());

            String response = channel.sendAndReceive(IPCChannel.MSG_NETWORK_SERVER_REGISTER, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                if (result.has("success") && result.get("success").getAsBoolean()) {
                    registeredServers.put(serverId, server);
                    return true;
                }
            }
            return false;
        }

        @Override
        public void unregisterServer(String serverId) {
            if (serverId == null || serverId.isEmpty()) {
                return;
            }

            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("serverId", serverId);

            channel.send(IPCChannel.MSG_NETWORK_SERVER_UNREGISTER, GSON.toJson(payload));
            registeredServers.remove(serverId);
        }

        @Override
        public boolean isServerRegistered(String serverId) {
            if (serverId == null || serverId.isEmpty()) {
                return false;
            }

            // ローカルにあるか確認
            if (registeredServers.containsKey(serverId)) {
                return true;
            }

            // サーバーに確認
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            payload.addProperty("serverId", serverId);

            String response = channel.sendAndReceive(IPCChannel.MSG_NETWORK_SERVER_IS_REGISTERED, GSON.toJson(payload));
            if (response != null) {
                JsonObject result = GSON.fromJson(response, JsonObject.class);
                return result.has("registered") && result.get("registered").getAsBoolean();
            }
            return false;
        }

        /**
         * サーバーリクエストを処理する（core→MODのIPCで呼ばれる）。
         * 内部使用のみ。
         *
         * @param serverId サーバーID
         * @param requestJson リクエストJSON
         * @return レスポンスJSON
         */
        public String handleServerRequest(String serverId, String requestJson) {
            VirtualServer server = registeredServers.get(serverId);
            if (server == null) {
                JsonObject error = new JsonObject();
                error.addProperty("statusCode", 404);
                error.addProperty("body", "Server not found: " + serverId);
                error.addProperty("contentType", "text/plain");
                return GSON.toJson(error);
            }

            if (!server.isAvailable()) {
                JsonObject error = new JsonObject();
                error.addProperty("statusCode", 503);
                error.addProperty("body", "Service unavailable");
                error.addProperty("contentType", "text/plain");
                return GSON.toJson(error);
            }

            try {
                JsonObject reqJson = GSON.fromJson(requestJson, JsonObject.class);

                // ServerRequestを構築
                String method = reqJson.has("method") ? reqJson.get("method").getAsString() : "GET";
                String path = reqJson.has("path") ? reqJson.get("path").getAsString() : "/";
                String body = reqJson.has("body") && !reqJson.get("body").isJsonNull()
                        ? reqJson.get("body").getAsString() : null;

                Map<String, String> queryParams = new HashMap<>();
                if (reqJson.has("queryParams") && reqJson.get("queryParams").isJsonObject()) {
                    JsonObject qp = reqJson.getAsJsonObject("queryParams");
                    for (String key : qp.keySet()) {
                        queryParams.put(key, qp.get(key).getAsString());
                    }
                }

                Map<String, String> headers = new HashMap<>();
                if (reqJson.has("headers") && reqJson.get("headers").isJsonObject()) {
                    JsonObject h = reqJson.getAsJsonObject("headers");
                    for (String key : h.keySet()) {
                        headers.put(key, h.get(key).getAsString());
                    }
                }

                IPvMAddress sourceAddress = null;
                if (reqJson.has("sourceAddress") && !reqJson.get("sourceAddress").isJsonNull()) {
                    try {
                        sourceAddress = IPvMAddress.fromString(reqJson.get("sourceAddress").getAsString());
                    } catch (IllegalArgumentException ignored) {}
                }

                ServerRequest request = new ServerRequest(method, path, queryParams, headers, body, sourceAddress);

                // サーバーで処理
                ServerResponse serverResponse = server.handleRequest(request);

                // レスポンスJSON構築
                JsonObject respJson = new JsonObject();
                respJson.addProperty("statusCode", serverResponse.getStatusCode());
                respJson.addProperty("body", serverResponse.getBody());
                respJson.addProperty("contentType", serverResponse.getContentType());

                JsonObject respHeaders = new JsonObject();
                for (Map.Entry<String, String> entry : serverResponse.getHeaders().entrySet()) {
                    respHeaders.addProperty(entry.getKey(), entry.getValue());
                }
                respJson.add("headers", respHeaders);

                return GSON.toJson(respJson);

            } catch (Exception e) {
                JsonObject error = new JsonObject();
                error.addProperty("statusCode", 500);
                error.addProperty("body", "Internal server error: " + e.getMessage());
                error.addProperty("contentType", "text/plain");
                return GSON.toJson(error);
            }
        }
    }
}
