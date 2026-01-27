package jp.moyashi.phoneos.server;

import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;
import jp.moyashi.phoneos.server.network.ServerVirtualRouter;
import jp.moyashi.phoneos.server.network.SystemServerRegistry;
import jp.moyashi.phoneos.server.network.VirtualHttpServer;
import jp.moyashi.phoneos.server.network.builtin.TestSystemServer;

import java.nio.file.Path;

/**
 * MochiMobileOS サーバーサイドエントリーポイント。
 *
 * 外部モジュール（Forge等）がサーバーサイドで使用するAPIを提供する。
 * - システムサーバーの登録
 * - パケットのルーティング
 * - 組み込みサーバーの初期化
 * - サーバーアプリの動的ロード
 */
public class MMOSServer {

    private static boolean initialized = false;

    /** サーバーデータのベースパス */
    private static Path serverDataPath = null;

    /** サーバーアプリローダー */
    private static ServerAppLoader serverAppLoader = null;

    /**
     * サーバーモジュールを初期化する。
     * Forgeモジュールのサーバーサイド初期化時に呼び出す。
     */
    public static synchronized void initialize() {
        if (initialized) {
            log("Already initialized");
            return;
        }

        log("Initializing MMOS Server...");

        // 組み込みシステムサーバーを登録
        registerBuiltinServers();

        // サーバーアプリを読み込み・登録
        loadServerApps();

        initialized = true;
        log("MMOS Server initialized successfully");
    }

    /**
     * サーバーアプリを読み込んで登録する
     */
    private static void loadServerApps() {
        Path dataPath = getServerDataPath();
        if (dataPath == null) {
            log("Server data path not set, skipping server app loading");
            return;
        }

        log("Loading server apps from: " + dataPath);

        serverAppLoader = new ServerAppLoader();
        serverAppLoader.scanAndLoad(dataPath);
        serverAppLoader.registerAll();

        log("Server apps loaded: " + serverAppLoader.getLoadedServerCount());
    }

    /**
     * 組み込みシステムサーバーを登録
     */
    private static void registerBuiltinServers() {
        SystemServerRegistry registry = SystemServerRegistry.getInstance();

        // テストサーバー
        registry.registerServer("test", new TestSystemServer());

        log("Built-in servers registered");
    }

    /**
     * システムサーバーを登録する。
     * 外部ModがカスタムサーバーをIPvMネットワークに追加する際に使用。
     *
     * @param serverId サーバーID（例: "test" → 3-sys-test でアクセス可能）
     * @param server サーバー実装
     */
    public static void registerSystemServer(String serverId, VirtualHttpServer server) {
        ensureInitialized();
        SystemServerRegistry.getInstance().registerServer(serverId, server);
    }

    /**
     * 外部Mod用サーバーを登録する（Type 2）。
     *
     * @param serverId サーバーID（例: "economy" → 2-economy でアクセス可能）
     * @param server サーバー実装
     */
    public static void registerExternalServer(String serverId, VirtualHttpServer server) {
        ensureInitialized();
        SystemServerRegistry.getInstance().registerExternalServer(serverId, server);
    }

    /**
     * クライアントからのHTTPリクエストパケットを処理する。
     * ForgeのNetworkHandlerから呼び出される。
     *
     * @param requestPacket HTTPリクエストを含むVirtualPacket
     * @return HTTPレスポンスを含むVirtualPacket
     */
    public static VirtualPacket handleHttpRequest(VirtualPacket requestPacket) {
        ensureInitialized();
        return ServerVirtualRouter.getInstance().routeRequest(requestPacket);
    }

    /**
     * IPvMアドレスに対応するサーバーが存在するかチェック
     */
    public static boolean hasServer(IPvMAddress address) {
        ensureInitialized();
        return SystemServerRegistry.getInstance().getServer(address).isPresent();
    }

    /**
     * サーバーを登録解除
     */
    public static void unregisterServer(String serverId) {
        ensureInitialized();
        SystemServerRegistry.getInstance().unregisterServer(serverId);
    }

    /**
     * 初期化済みかどうかを確認
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * 初期化を確認し、未初期化なら自動初期化
     */
    private static void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }

    /**
     * シャットダウン処理
     */
    public static synchronized void shutdown() {
        if (!initialized) {
            return;
        }

        log("Shutting down MMOS Server...");

        // サーバーアプリローダーをクローズ
        if (serverAppLoader != null) {
            serverAppLoader.close();
            serverAppLoader = null;
        }

        SystemServerRegistry.getInstance().clear();
        initialized = false;
        log("MMOS Server shutdown complete");
    }

    /**
     * サーバーデータのベースパスを取得する。
     * Forge環境ではゲームディレクトリ/mmos_server_data等が設定される。
     *
     * @return サーバーデータのベースパス
     */
    public static Path getServerDataPath() {
        return serverDataPath;
    }

    /**
     * サーバーデータのベースパスを設定する。
     * MMOSServer.initialize()を呼び出す前に設定する必要がある。
     *
     * @param path サーバーデータのベースパス
     */
    public static void setServerDataPath(Path path) {
        if (initialized) {
            log("Warning: setServerDataPath called after initialization");
        }
        serverDataPath = path;
        log("Server data path set to: " + path);
    }

    private static void log(String message) {
        System.out.println("[MMOSServer] " + message);
    }
}
