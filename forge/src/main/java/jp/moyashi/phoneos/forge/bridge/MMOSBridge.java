package jp.moyashi.phoneos.forge.bridge;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MMOS外部プロセスブリッジ。
 * MMOSサーバープロセスの起動・管理と、RemoteKernelの提供を行う。
 *
 * @author jp.moyashi
 * @version 1.0
 */
public class MMOSBridge {

    private static final Logger LOGGER = LogManager.getLogger(MMOSBridge.class);

    /** サーバー接続待機のタイムアウト（ミリ秒） */
    private static final int CONNECTION_TIMEOUT_MS = 30000;

    /** 接続リトライ間隔（ミリ秒） */
    private static final int RETRY_INTERVAL_MS = 100;

    /** プロセスランチャー */
    private ProcessLauncher launcher;

    /** リモートカーネル */
    private RemoteKernel remoteKernel;

    /** ワールドID */
    private String worldId;

    /** 初期化済みフラグ */
    private volatile boolean initialized = false;

    /**
     * MMOSブリッジを初期化する。
     *
     * @param worldId ワールドID
     * @return 初期化成功した場合true
     */
    public boolean initialize(String worldId) {
        if (initialized && this.worldId != null && this.worldId.equals(worldId)) {
            LOGGER.info("[MMOSBridge] Already initialized for world: {}", worldId);
            return true;
        }

        // 既存の接続をクリーンアップ
        shutdown();

        this.worldId = worldId;
        LOGGER.info("[MMOSBridge] Initializing for world: {}", worldId);

        // プロセスランチャーを作成
        launcher = new ProcessLauncher(worldId);

        // サーバーJARを探す
        if (!launcher.findServerJar()) {
            LOGGER.error("[MMOSBridge] Server JAR not found, cannot initialize");
            return false;
        }

        // サーバープロセスを起動
        if (!launcher.launch()) {
            LOGGER.error("[MMOSBridge] Failed to launch server process");
            return false;
        }

        // RemoteKernelを作成
        remoteKernel = new RemoteKernel(worldId);

        // サーバー接続を待機
        if (!waitForServerConnection()) {
            LOGGER.error("[MMOSBridge] Failed to connect to server");
            shutdown();
            return false;
        }

        initialized = true;
        LOGGER.info("[MMOSBridge] Initialization complete for world: {}", worldId);
        return true;
    }

    /**
     * サーバー接続を待機する。
     *
     * @return 接続成功した場合true
     */
    private boolean waitForServerConnection() {
        LOGGER.info("[MMOSBridge] Waiting for server connection...");

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < CONNECTION_TIMEOUT_MS) {
            // プロセスが終了していないか確認
            if (!launcher.isRunning()) {
                LOGGER.error("[MMOSBridge] Server process terminated unexpectedly");
                return false;
            }

            // 接続を試みる
            if (remoteKernel.connect()) {
                // サーバー準備完了を待機
                if (remoteKernel.isServerReady()) {
                    LOGGER.info("[MMOSBridge] Connected to server (took {} ms)",
                        System.currentTimeMillis() - startTime);
                    return true;
                }
            }

            // 少し待機
            try {
                Thread.sleep(RETRY_INTERVAL_MS);
            } catch (InterruptedException e) {
                LOGGER.warn("[MMOSBridge] Connection wait interrupted");
                return false;
            }
        }

        LOGGER.error("[MMOSBridge] Connection timeout ({} ms)", CONNECTION_TIMEOUT_MS);
        return false;
    }

    /**
     * リモートカーネルを取得する。
     *
     * @return リモートカーネル、または初期化されていない場合null
     */
    public RemoteKernel getRemoteKernel() {
        return remoteKernel;
    }

    /**
     * 初期化済みかどうかを返す。
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * サーバーが実行中かどうかを返す。
     */
    public boolean isServerRunning() {
        return launcher != null && launcher.isRunning();
    }

    /**
     * シャットダウンする。
     */
    public void shutdown() {
        LOGGER.info("[MMOSBridge] Shutting down...");

        initialized = false;

        // RemoteKernelをシャットダウン
        if (remoteKernel != null) {
            try {
                remoteKernel.shutdown();
            } catch (Exception e) {
                LOGGER.error("[MMOSBridge] Error shutting down RemoteKernel", e);
            }
            remoteKernel = null;
        }

        // サーバープロセスを停止
        if (launcher != null) {
            launcher.stop();
            launcher = null;
        }

        worldId = null;
        LOGGER.info("[MMOSBridge] Shutdown complete");
    }

    /**
     * サーバーを再起動する。
     *
     * @return 再起動成功した場合true
     */
    public boolean restart() {
        LOGGER.info("[MMOSBridge] Restarting server...");

        String currentWorldId = this.worldId;
        shutdown();

        if (currentWorldId != null) {
            return initialize(currentWorldId);
        }

        return false;
    }

    /**
     * ワールドIDを取得する。
     */
    public String getWorldId() {
        return worldId;
    }
}
