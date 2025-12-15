package jp.moyashi.phoneos.forge.app;

import jp.moyashi.phoneos.api.proxy.IPCChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Forge側IPCチャンネル実装。
 * ファイルベースのIPC通信を使用してServer JVMと通信する。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class ForgeIPCChannel implements IPCChannel {

    private static final Logger LOGGER = LogManager.getLogger(ForgeIPCChannel.class);

    /** リクエストファイル名 */
    private static final String REQUEST_FILE = "api_request.json";

    /** レスポンスファイル名 */
    private static final String RESPONSE_FILE = "api_response.json";

    /** ロックファイル名 */
    private static final String LOCK_FILE = "api.lock";

    /** レスポンス待機タイムアウト（ミリ秒） */
    private static final long RESPONSE_TIMEOUT = 5000;

    /** ポーリング間隔（ミリ秒） */
    private static final long POLL_INTERVAL = 10;

    /** IPCディレクトリ */
    private final Path ipcDir;

    /** リクエストファイルパス */
    private final Path requestFile;

    /** レスポンスファイルパス */
    private final Path responseFile;

    /** ロックファイルパス */
    private final Path lockFile;

    /** リクエストID生成用 */
    private final AtomicLong requestIdCounter = new AtomicLong(0);

    /** 接続フラグ */
    private volatile boolean connected = false;

    /**
     * ForgeIPCChannelを作成する。
     *
     * @param worldId ワールドID
     */
    public ForgeIPCChannel(String worldId) {
        // 一時ディレクトリ内にIPC用ディレクトリを作成
        String sanitized = worldId.replaceAll("[^a-zA-Z0-9_-]", "_");
        this.ipcDir = Paths.get(System.getProperty("java.io.tmpdir"), "mmos", "ipc_" + sanitized);
        this.requestFile = ipcDir.resolve(REQUEST_FILE);
        this.responseFile = ipcDir.resolve(RESPONSE_FILE);
        this.lockFile = ipcDir.resolve(LOCK_FILE);
    }

    /**
     * IPCチャンネルを初期化する。
     *
     * @return 成功した場合true
     */
    public boolean initialize() {
        try {
            // ディレクトリが存在するか確認
            if (!Files.exists(ipcDir)) {
                LOGGER.debug("[ForgeIPCChannel] IPC directory does not exist: {}", ipcDir);
                return false;
            }

            connected = true;
            LOGGER.info("[ForgeIPCChannel] Initialized IPC channel: {}", ipcDir);
            return true;

        } catch (Exception e) {
            LOGGER.error("[ForgeIPCChannel] Failed to initialize IPC channel", e);
            return false;
        }
    }

    @Override
    public String sendAndReceive(String type, String payload) {
        if (!connected) {
            if (!initialize()) {
                return null;
            }
        }

        long requestId = requestIdCounter.incrementAndGet();

        try {
            // リクエストJSONを構築
            String request = String.format(
                "{\"id\":%d,\"type\":\"%s\",\"payload\":%s,\"timestamp\":%d}",
                requestId, type, payload, System.currentTimeMillis()
            );

            // 既存のレスポンスファイルを削除
            Files.deleteIfExists(responseFile);

            // リクエストファイルに書き込み
            Files.writeString(requestFile, request, StandardCharsets.UTF_8);
            LOGGER.debug("[ForgeIPCChannel] Sent request {}: type={}", requestId, type);

            // レスポンスを待機
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < RESPONSE_TIMEOUT) {
                if (Files.exists(responseFile)) {
                    String response = Files.readString(responseFile, StandardCharsets.UTF_8);
                    if (response != null && !response.isEmpty()) {
                        // レスポンスファイルを削除
                        Files.deleteIfExists(responseFile);
                        LOGGER.debug("[ForgeIPCChannel] Received response for request {}", requestId);

                        // レスポンスJSONからpayloadを抽出
                        return extractPayloadFromResponse(response, requestId);
                    }
                }
                Thread.sleep(POLL_INTERVAL);
            }

            LOGGER.warn("[ForgeIPCChannel] Timeout waiting for response to request {}", requestId);
            return null;

        } catch (IOException | InterruptedException e) {
            LOGGER.error("[ForgeIPCChannel] Error in sendAndReceive", e);
            return null;
        }
    }

    /**
     * レスポンスJSONからペイロードを抽出する。
     */
    private String extractPayloadFromResponse(String response, long expectedId) {
        // シンプルなJSON解析（Gsonを使用せずに軽量化）
        // 形式: {"id":123,"payload":{...}}
        try {
            // IDの確認
            int idIndex = response.indexOf("\"id\":");
            if (idIndex < 0) return response;

            int idStart = idIndex + 5;
            int idEnd = response.indexOf(",", idStart);
            if (idEnd < 0) idEnd = response.indexOf("}", idStart);

            long responseId = Long.parseLong(response.substring(idStart, idEnd).trim());
            if (responseId != expectedId) {
                LOGGER.warn("[ForgeIPCChannel] Response ID mismatch: expected {}, got {}", expectedId, responseId);
            }

            // ペイロードの抽出
            int payloadIndex = response.indexOf("\"payload\":");
            if (payloadIndex < 0) return "{}";

            int payloadStart = payloadIndex + 10;
            // ペイロードの終わりを見つける（最後の } まで）
            int braceCount = 0;
            int payloadEnd = payloadStart;
            boolean inString = false;
            boolean escaped = false;

            for (int i = payloadStart; i < response.length(); i++) {
                char c = response.charAt(i);

                if (escaped) {
                    escaped = false;
                    continue;
                }

                if (c == '\\') {
                    escaped = true;
                    continue;
                }

                if (c == '"') {
                    inString = !inString;
                    continue;
                }

                if (!inString) {
                    if (c == '{' || c == '[') {
                        braceCount++;
                    } else if (c == '}' || c == ']') {
                        braceCount--;
                        if (braceCount == 0) {
                            payloadEnd = i + 1;
                            break;
                        }
                    }
                }
            }

            return response.substring(payloadStart, payloadEnd);

        } catch (Exception e) {
            LOGGER.error("[ForgeIPCChannel] Error extracting payload from response", e);
            return response;
        }
    }

    @Override
    public boolean send(String type, String payload) {
        if (!connected) {
            if (!initialize()) {
                return false;
            }
        }

        long requestId = requestIdCounter.incrementAndGet();

        try {
            // リクエストJSONを構築（レスポンス不要フラグ付き）
            String request = String.format(
                "{\"id\":%d,\"type\":\"%s\",\"payload\":%s,\"timestamp\":%d,\"noResponse\":true}",
                requestId, type, payload, System.currentTimeMillis()
            );

            // リクエストファイルに書き込み
            Files.writeString(requestFile, request, StandardCharsets.UTF_8);
            LOGGER.debug("[ForgeIPCChannel] Sent one-way request {}: type={}", requestId, type);

            return true;

        } catch (IOException e) {
            LOGGER.error("[ForgeIPCChannel] Error in send", e);
            return false;
        }
    }

    @Override
    public boolean isConnected() {
        return connected && Files.exists(ipcDir);
    }

    @Override
    public void close() {
        connected = false;
        LOGGER.info("[ForgeIPCChannel] Closed IPC channel");
    }
}
