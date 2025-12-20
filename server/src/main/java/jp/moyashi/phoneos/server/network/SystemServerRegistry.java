package jp.moyashi.phoneos.server.network;

import jp.moyashi.phoneos.core.service.network.IPvMAddress;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * システムサーバーの登録レジストリ。
 * IPvMアドレス（Type 3: System）へのリクエストを処理するサーバーを管理する。
 *
 * このレジストリはサーバーサイドで管理され、
 * クライアントからのリクエストをルーティングする際に使用される。
 */
public class SystemServerRegistry {

    private static final SystemServerRegistry INSTANCE = new SystemServerRegistry();

    /**
     * サーバーID → VirtualHttpServer のマッピング
     */
    private final Map<String, VirtualHttpServer> servers = new ConcurrentHashMap<>();

    private SystemServerRegistry() {
    }

    /**
     * シングルトンインスタンスを取得
     */
    public static SystemServerRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * システムサーバーを登録する。
     * サーバーIDは "sys-" プレフィックスなしで登録される。
     * 例: registerServer("test", server) → 3-sys-test でアクセス可能
     *
     * @param serverId サーバーID（例: "test", "google"）
     * @param server サーバー実装
     */
    public void registerServer(String serverId, VirtualHttpServer server) {
        if (serverId == null || serverId.isEmpty()) {
            throw new IllegalArgumentException("Server ID cannot be null or empty");
        }
        if (server == null) {
            throw new IllegalArgumentException("Server cannot be null");
        }

        String normalizedId = normalizeServerId(serverId);
        servers.put(normalizedId, server);
        log("Registered system server: " + normalizedId + " -> " + server.getDescription());
    }

    /**
     * 外部Mod用サーバーを登録する（Type 2: Server）。
     * 例: registerExternalServer("economy", server) → 2-economy でアクセス可能
     *
     * @param serverId サーバーID
     * @param server サーバー実装
     */
    public void registerExternalServer(String serverId, VirtualHttpServer server) {
        if (serverId == null || serverId.isEmpty()) {
            throw new IllegalArgumentException("Server ID cannot be null or empty");
        }
        if (server == null) {
            throw new IllegalArgumentException("Server cannot be null");
        }

        // 外部サーバーは "ext-" プレフィックスで管理
        String normalizedId = "ext-" + serverId;
        servers.put(normalizedId, server);
        log("Registered external server: " + serverId + " -> " + server.getDescription());
    }

    /**
     * サーバーを登録解除する。
     *
     * @param serverId サーバーID
     */
    public void unregisterServer(String serverId) {
        String normalizedId = normalizeServerId(serverId);
        VirtualHttpServer removed = servers.remove(normalizedId);
        if (removed != null) {
            log("Unregistered server: " + normalizedId);
        }
    }

    /**
     * IPvMアドレスに対応するサーバーを取得する。
     *
     * @param address IPvMアドレス
     * @return サーバー（存在しない場合はOptional.empty()）
     */
    public Optional<VirtualHttpServer> getServer(IPvMAddress address) {
        if (address == null) {
            return Optional.empty();
        }

        String lookupKey;
        if (address.isSystem()) {
            // Type 3: System - "sys-xxx" または直接のID
            lookupKey = normalizeServerId(address.getUUID());
        } else if (address.isServer()) {
            // Type 2: Server - "ext-xxx"
            lookupKey = "ext-" + address.getUUID();
        } else {
            // Player/Device はサーバーではない
            return Optional.empty();
        }

        return Optional.ofNullable(servers.get(lookupKey));
    }

    /**
     * サーバーIDを正規化する。
     * "sys-test" と "test" の両方を同じキーにマッピング。
     */
    private String normalizeServerId(String serverId) {
        if (serverId.startsWith("sys-")) {
            return serverId;
        }
        return "sys-" + serverId;
    }

    /**
     * 登録されているサーバーの一覧を取得（デバッグ用）。
     */
    public Map<String, VirtualHttpServer> getAllServers() {
        return Map.copyOf(servers);
    }

    /**
     * サーバーが登録されているかどうかを確認
     */
    public boolean hasServer(String serverId) {
        return servers.containsKey(normalizeServerId(serverId));
    }

    /**
     * すべてのサーバーを登録解除
     */
    public void clear() {
        servers.clear();
        log("All servers cleared");
    }

    private void log(String message) {
        System.out.println("[SystemServerRegistry] " + message);
    }
}
