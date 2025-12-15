package jp.moyashi.phoneos.server.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jp.moyashi.phoneos.api.proxy.IPCChannel;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.VFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MODアプリAPIリクエストハンドラ。
 * Forge JVMからのAPI呼び出しを処理する。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class APIRequestHandler {

    private static final Logger LOGGER = LogManager.getLogger(APIRequestHandler.class);

    /** リクエストファイル名 */
    private static final String REQUEST_FILE = "api_request.json";

    /** レスポンスファイル名 */
    private static final String RESPONSE_FILE = "api_response.json";

    /** ポーリング間隔（ミリ秒） */
    private static final long POLL_INTERVAL = 5;

    /** IPCディレクトリ */
    private final Path ipcDir;

    /** リクエストファイルパス */
    private final Path requestFile;

    /** レスポンスファイルパス */
    private final Path responseFile;

    /** カーネル */
    private final Kernel kernel;

    /** VFS */
    private VFS vfs;

    /** 最後に処理したリクエストID */
    private long lastProcessedRequestId = 0;

    /** 登録されたMODアプリ情報 */
    private final Map<String, ModAppInfo> registeredApps = new ConcurrentHashMap<>();

    /** アプリ設定キャッシュ */
    private final Map<String, Map<String, Object>> appSettings = new ConcurrentHashMap<>();

    private static final Gson GSON = new Gson();

    /**
     * MODアプリ情報。
     */
    public static class ModAppInfo {
        public final String appId;
        public final String name;
        public final String description;
        public final String version;
        public final String author;
        public final String iconPath;

        public ModAppInfo(String appId, String name, String description, String version, String author, String iconPath) {
            this.appId = appId;
            this.name = name;
            this.description = description;
            this.version = version;
            this.author = author;
            this.iconPath = iconPath;
        }
    }

    /**
     * APIRequestHandlerを作成する。
     *
     * @param worldId ワールドID
     * @param kernel カーネル
     */
    public APIRequestHandler(String worldId, Kernel kernel) {
        String sanitized = worldId.replaceAll("[^a-zA-Z0-9_-]", "_");
        this.ipcDir = Paths.get(System.getProperty("java.io.tmpdir"), "mmos", "ipc_" + sanitized);
        this.requestFile = ipcDir.resolve(REQUEST_FILE);
        this.responseFile = ipcDir.resolve(RESPONSE_FILE);
        this.kernel = kernel;

        // VFSを取得
        try {
            // Kernelから直接VFSを取得できないため、VFSインスタンスを別途管理
            this.vfs = kernel.getVFS();
        } catch (Exception e) {
            LOGGER.warn("[APIRequestHandler] Could not get VFS from kernel", e);
        }
    }

    /**
     * 初期化する。
     */
    public void initialize() throws IOException {
        // IPCディレクトリを作成
        Files.createDirectories(ipcDir);

        // 既存のファイルを削除
        Files.deleteIfExists(requestFile);
        Files.deleteIfExists(responseFile);

        LOGGER.info("[APIRequestHandler] Initialized IPC directory: {}", ipcDir);
    }

    /**
     * 保留中のリクエストを処理する。
     * メインループから定期的に呼び出す。
     */
    public void processRequests() {
        try {
            if (!Files.exists(requestFile)) {
                return;
            }

            // リクエストを読み込み
            String requestJson = Files.readString(requestFile, StandardCharsets.UTF_8);
            if (requestJson == null || requestJson.isEmpty()) {
                return;
            }

            // リクエストを削除（処理中を示す）
            Files.deleteIfExists(requestFile);

            // JSONをパース
            JsonObject request = JsonParser.parseString(requestJson).getAsJsonObject();

            long requestId = request.get("id").getAsLong();
            if (requestId <= lastProcessedRequestId) {
                // 既に処理済み
                return;
            }

            String type = request.get("type").getAsString();
            JsonObject payload = request.has("payload") ? request.get("payload").getAsJsonObject() : new JsonObject();
            boolean noResponse = request.has("noResponse") && request.get("noResponse").getAsBoolean();

            LOGGER.debug("[APIRequestHandler] Processing request {}: type={}", requestId, type);

            // リクエストを処理
            JsonObject responsePayload = handleRequest(type, payload);

            // レスポンスを書き込み（noResponseでない場合）
            if (!noResponse) {
                JsonObject response = new JsonObject();
                response.addProperty("id", requestId);
                response.add("payload", responsePayload);

                Files.writeString(responseFile, GSON.toJson(response), StandardCharsets.UTF_8);
            }

            lastProcessedRequestId = requestId;

        } catch (Exception e) {
            LOGGER.error("[APIRequestHandler] Error processing request", e);
        }
    }

    /**
     * リクエストを処理する。
     */
    private JsonObject handleRequest(String type, JsonObject payload) {
        JsonObject response = new JsonObject();

        try {
            switch (type) {
                // ストレージ操作
                case IPCChannel.MSG_STORAGE_READ:
                    return handleStorageRead(payload);
                case IPCChannel.MSG_STORAGE_WRITE:
                    return handleStorageWrite(payload);
                case IPCChannel.MSG_STORAGE_READ_BYTES:
                    return handleStorageReadBytes(payload);
                case IPCChannel.MSG_STORAGE_WRITE_BYTES:
                    return handleStorageWriteBytes(payload);
                case IPCChannel.MSG_STORAGE_EXISTS:
                    return handleStorageExists(payload);
                case IPCChannel.MSG_STORAGE_DELETE:
                    return handleStorageDelete(payload);
                case IPCChannel.MSG_STORAGE_LIST:
                    return handleStorageList(payload);
                case IPCChannel.MSG_STORAGE_MKDIR:
                    return handleStorageMkdir(payload);
                case IPCChannel.MSG_STORAGE_IS_FILE:
                    return handleStorageIsFile(payload);
                case IPCChannel.MSG_STORAGE_IS_DIR:
                    return handleStorageIsDir(payload);

                // 設定操作
                case IPCChannel.MSG_SETTINGS_GET:
                    return handleSettingsGet(payload);
                case IPCChannel.MSG_SETTINGS_SET:
                    return handleSettingsSet(payload);
                case IPCChannel.MSG_SETTINGS_CONTAINS:
                    return handleSettingsContains(payload);
                case IPCChannel.MSG_SETTINGS_REMOVE:
                    return handleSettingsRemove(payload);
                case IPCChannel.MSG_SETTINGS_KEYS:
                    return handleSettingsKeys(payload);
                case IPCChannel.MSG_SETTINGS_SAVE:
                    return handleSettingsSave(payload);

                // 通知
                case IPCChannel.MSG_NOTIFICATION_SEND:
                    return handleNotificationSend(payload);

                // システム情報
                case IPCChannel.MSG_SYSTEM_INFO:
                    return handleSystemInfo(payload);

                // ログ
                case IPCChannel.MSG_LOG:
                    return handleLog(payload);

                // アプリ登録
                case IPCChannel.MSG_APP_REGISTER:
                    return handleAppRegister(payload);
                case IPCChannel.MSG_APP_UNREGISTER:
                    return handleAppUnregister(payload);

                default:
                    LOGGER.warn("[APIRequestHandler] Unknown request type: {}", type);
                    response.addProperty("error", "Unknown request type: " + type);
            }
        } catch (Exception e) {
            LOGGER.error("[APIRequestHandler] Error handling request: {}", type, e);
            response.addProperty("error", e.getMessage());
        }

        return response;
    }

    // === ストレージ操作 ===

    private String getAppStoragePath(String appId, String path) {
        // サンドボックス化: apps/<appId>/data/<path>
        return "apps/" + appId + "/data/" + path;
    }

    private JsonObject handleStorageRead(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String path = payload.get("path").getAsString();

        String fullPath = getAppStoragePath(appId, path);
        String content = vfs != null ? vfs.readFile(fullPath) : null;
        response.addProperty("content", content);
        return response;
    }

    private JsonObject handleStorageWrite(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String path = payload.get("path").getAsString();
        String content = payload.get("content").getAsString();

        String fullPath = getAppStoragePath(appId, path);
        boolean success = vfs != null && vfs.writeFile(fullPath, content);
        response.addProperty("success", success);
        return response;
    }

    private JsonObject handleStorageReadBytes(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String path = payload.get("path").getAsString();

        String fullPath = getAppStoragePath(appId, path);
        // バイナリ読み込み - VFSにはreadBytesがないのでreadFileをバイトに変換
        String content = vfs != null ? vfs.readFile(fullPath) : null;
        if (content != null) {
            response.addProperty("data", Base64.getEncoder().encodeToString(content.getBytes()));
        } else {
            response.add("data", null);
        }
        return response;
    }

    private JsonObject handleStorageWriteBytes(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String path = payload.get("path").getAsString();
        String dataBase64 = payload.get("data").getAsString();

        String fullPath = getAppStoragePath(appId, path);
        byte[] data = Base64.getDecoder().decode(dataBase64);
        // バイナリ書き込み - VFSにはwriteBytesがないのでwriteFileを使用
        boolean success = vfs != null && vfs.writeFile(fullPath, new String(data));
        response.addProperty("success", success);
        return response;
    }

    private JsonObject handleStorageExists(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String path = payload.get("path").getAsString();

        String fullPath = getAppStoragePath(appId, path);
        boolean exists = vfs != null && (vfs.fileExists(fullPath) || vfs.directoryExists(fullPath));
        response.addProperty("exists", exists);
        return response;
    }

    private JsonObject handleStorageDelete(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String path = payload.get("path").getAsString();

        String fullPath = getAppStoragePath(appId, path);
        boolean success = vfs != null && vfs.deleteFile(fullPath);
        response.addProperty("success", success);
        return response;
    }

    private JsonObject handleStorageList(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String path = payload.get("path").getAsString();

        String fullPath = getAppStoragePath(appId, path);
        List<String> files = vfs != null ? vfs.listFiles(fullPath) : Collections.emptyList();
        response.add("files", GSON.toJsonTree(files));
        return response;
    }

    private JsonObject handleStorageMkdir(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String path = payload.get("path").getAsString();

        String fullPath = getAppStoragePath(appId, path);
        boolean success = vfs != null && vfs.createDirectory(fullPath);
        response.addProperty("success", success);
        return response;
    }

    private JsonObject handleStorageIsFile(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String path = payload.get("path").getAsString();

        String fullPath = getAppStoragePath(appId, path);
        boolean isFile = vfs != null && vfs.fileExists(fullPath);
        response.addProperty("isFile", isFile);
        return response;
    }

    private JsonObject handleStorageIsDir(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String path = payload.get("path").getAsString();

        String fullPath = getAppStoragePath(appId, path);
        boolean isDir = vfs != null && vfs.directoryExists(fullPath);
        response.addProperty("isDir", isDir);
        return response;
    }

    // === 設定操作 ===

    private Map<String, Object> getAppSettingsMap(String appId) {
        return appSettings.computeIfAbsent(appId, k -> {
            // VFSから設定を読み込み
            Map<String, Object> settings = new HashMap<>();
            if (vfs != null) {
                String settingsPath = "apps/" + appId + "/settings.json";
                String content = vfs.readFile(settingsPath);
                if (content != null && !content.isEmpty()) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> loaded = GSON.fromJson(content, Map.class);
                        settings.putAll(loaded);
                    } catch (Exception e) {
                        LOGGER.error("[APIRequestHandler] Error loading settings for {}", appId, e);
                    }
                }
            }
            return settings;
        });
    }

    private JsonObject handleSettingsGet(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String key = payload.get("key").getAsString();

        Map<String, Object> settings = getAppSettingsMap(appId);
        Object value = settings.get(key);
        if (value != null) {
            response.add("value", GSON.toJsonTree(value));
        } else {
            response.add("value", null);
        }
        return response;
    }

    private JsonObject handleSettingsSet(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String key = payload.get("key").getAsString();
        Object value = GSON.fromJson(payload.get("value"), Object.class);

        Map<String, Object> settings = getAppSettingsMap(appId);
        settings.put(key, value);
        response.addProperty("success", true);
        return response;
    }

    private JsonObject handleSettingsContains(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String key = payload.get("key").getAsString();

        Map<String, Object> settings = getAppSettingsMap(appId);
        response.addProperty("contains", settings.containsKey(key));
        return response;
    }

    private JsonObject handleSettingsRemove(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String key = payload.get("key").getAsString();

        Map<String, Object> settings = getAppSettingsMap(appId);
        settings.remove(key);
        response.addProperty("success", true);
        return response;
    }

    private JsonObject handleSettingsKeys(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();

        Map<String, Object> settings = getAppSettingsMap(appId);
        response.add("keys", GSON.toJsonTree(settings.keySet()));
        return response;
    }

    private JsonObject handleSettingsSave(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();

        Map<String, Object> settings = getAppSettingsMap(appId);
        if (vfs != null) {
            String settingsPath = "apps/" + appId + "/settings.json";
            boolean success = vfs.writeFile(settingsPath, GSON.toJson(settings));
            response.addProperty("success", success);
        } else {
            response.addProperty("success", false);
        }
        return response;
    }

    // === 通知 ===

    private JsonObject handleNotificationSend(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();
        String title = payload.get("title").getAsString();
        String message = payload.get("message").getAsString();

        // カーネルの通知サービスに送信
        if (kernel != null) {
            kernel.sendNotification(title, message);
        }
        response.addProperty("success", true);
        return response;
    }

    // === システム情報 ===

    private JsonObject handleSystemInfo(JsonObject payload) {
        JsonObject response = new JsonObject();
        response.addProperty("osVersion", "1.0.0");
        response.addProperty("screenWidth", kernel != null ? kernel.getWidth() : 400);
        response.addProperty("screenHeight", kernel != null ? kernel.getHeight() : 600);
        response.addProperty("batteryLevel", 100);
        response.addProperty("isCharging", false);
        response.addProperty("isWifiConnected", true);
        response.addProperty("worldId", kernel != null ? kernel.getWorldId() : "unknown");
        return response;
    }

    // === ログ ===

    private JsonObject handleLog(JsonObject payload) {
        String appId = payload.get("appId").getAsString();
        String level = payload.get("level").getAsString();
        String message = payload.get("message").getAsString();

        String logMessage = "[MOD:" + appId + "] " + message;

        switch (level.toUpperCase()) {
            case "DEBUG":
                LOGGER.debug(logMessage);
                break;
            case "INFO":
                LOGGER.info(logMessage);
                break;
            case "WARN":
                LOGGER.warn(logMessage);
                break;
            case "ERROR":
                LOGGER.error(logMessage);
                break;
            default:
                LOGGER.info(logMessage);
        }

        return new JsonObject();
    }

    // === アプリ登録 ===

    private JsonObject handleAppRegister(JsonObject payload) {
        JsonObject response = new JsonObject();

        String appId = payload.get("appId").getAsString();
        String name = payload.get("name").getAsString();
        String description = payload.has("description") ? payload.get("description").getAsString() : "";
        String version = payload.has("version") ? payload.get("version").getAsString() : "1.0.0";
        String author = payload.has("author") ? payload.get("author").getAsString() : "Unknown";
        String iconPath = payload.has("iconPath") && !payload.get("iconPath").isJsonNull()
            ? payload.get("iconPath").getAsString() : null;

        ModAppInfo info = new ModAppInfo(appId, name, description, version, author, iconPath);
        registeredApps.put(appId, info);

        LOGGER.info("[APIRequestHandler] Registered MOD app: {} ({})", name, appId);
        response.addProperty("success", true);
        return response;
    }

    private JsonObject handleAppUnregister(JsonObject payload) {
        JsonObject response = new JsonObject();
        String appId = payload.get("appId").getAsString();

        registeredApps.remove(appId);
        LOGGER.info("[APIRequestHandler] Unregistered MOD app: {}", appId);
        response.addProperty("success", true);
        return response;
    }

    // === ゲッター ===

    /**
     * 登録されたMODアプリ一覧を取得する。
     */
    public Collection<ModAppInfo> getRegisteredApps() {
        return Collections.unmodifiableCollection(registeredApps.values());
    }

    /**
     * シャットダウンする。
     */
    public void shutdown() {
        // 設定を保存
        for (String appId : appSettings.keySet()) {
            JsonObject payload = new JsonObject();
            payload.addProperty("appId", appId);
            handleSettingsSave(payload);
        }

        LOGGER.info("[APIRequestHandler] Shutdown complete");
    }
}
