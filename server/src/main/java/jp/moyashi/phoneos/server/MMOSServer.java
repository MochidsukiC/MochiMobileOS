package jp.moyashi.phoneos.server;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ipc.IPCConstants;
import jp.moyashi.phoneos.core.ipc.InputEvent;
import jp.moyashi.phoneos.core.ipc.ServerState;
import jp.moyashi.phoneos.core.service.chromium.ChromiumService;
import jp.moyashi.phoneos.core.service.chromium.DefaultChromiumService;
import jp.moyashi.phoneos.core.service.chromium.JCEFChromiumProvider;
import jp.moyashi.phoneos.server.api.APIRequestHandler;
import jp.moyashi.phoneos.server.ipc.SharedMemory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * MMOS外部プロセスサーバー。
 * Forge MODから独立して実行され、共有メモリを通じて通信する。
 *
 * Usage: java -jar mmos-server.jar <worldId>
 *
 * @author jp.moyashi
 * @version 1.0
 */
public class MMOSServer {

    private static final Logger LOGGER = LogManager.getLogger(MMOSServer.class);

    /** 共有メモリ */
    private final SharedMemory shm;

    /** カーネル */
    private final Kernel kernel;

    /** APIリクエストハンドラ */
    private final APIRequestHandler apiHandler;

    /** ワールドID */
    private final String worldId;

    /** 実行フラグ */
    private volatile boolean running = true;

    /** 目標フレームレート */
    private int targetFrameRate = 60;

    /** 最後のフレーム時刻 */
    private long lastFrameTime = 0;

    /**
     * サーバーを初期化する。
     *
     * @param worldId ワールドID
     */
    public MMOSServer(String worldId) throws Exception {
        this.worldId = worldId;
        LOGGER.info("[MMOSServer] Starting MMOS Server for world: {}", worldId);

        // 共有メモリを作成
        String shmName = IPCConstants.getSharedMemoryName(worldId);
        this.shm = SharedMemory.create(shmName);
        LOGGER.info("[MMOSServer] Shared memory created: {}", shmName);

        // カーネルを作成
        this.kernel = new Kernel();
        LOGGER.info("[MMOSServer] Kernel instance created");

        // ChromiumServiceを設定（JCEFChromiumProviderを使用）
        // サーバー環境はHeadlessモードではないため、JCEFを直接使用可能
        LOGGER.info("[MMOSServer] Configuring ChromiumService with JCEFChromiumProvider...");
        try {
            ChromiumService chromiumService = new DefaultChromiumService(new JCEFChromiumProvider());
            kernel.setChromiumService(chromiumService);
            LOGGER.info("[MMOSServer] ChromiumService configured successfully");
        } catch (Exception e) {
            LOGGER.warn("[MMOSServer] Failed to configure ChromiumService: {}", e.getMessage());
            LOGGER.warn("[MMOSServer] Chromium-based features will be disabled");
        }

        // カーネルを初期化（Minecraft用）
        kernel.initializeForMinecraft(IPCConstants.SCREEN_WIDTH, IPCConstants.SCREEN_HEIGHT, worldId);
        LOGGER.info("[MMOSServer] Kernel initialized for Minecraft: {}x{}", IPCConstants.SCREEN_WIDTH, IPCConstants.SCREEN_HEIGHT);

        // APIリクエストハンドラを初期化
        this.apiHandler = new APIRequestHandler(worldId, kernel);
        apiHandler.initialize();
        LOGGER.info("[MMOSServer] API request handler initialized");

        // サーバー準備完了フラグを設定
        shm.setServerReady(true);
        LOGGER.info("[MMOSServer] Server ready, waiting for client connection...");
    }

    /**
     * メインループを実行する。
     */
    public void run() {
        LOGGER.info("[MMOSServer] Entering main loop");
        long frameTime = 1000 / targetFrameRate;

        while (running) {
            long startTime = System.currentTimeMillis();

            try {
                // 1. コマンドを処理
                processCommands();

                // 2. 入力イベントを処理
                processInputEvents();

                // 3. APIリクエストを処理
                apiHandler.processRequests();

                // 4. カーネルを更新・描画（スリープ中でない場合）
                if (!kernel.isSleeping()) {
                    kernel.update();
                    kernel.render();
                }

                // 5. ピクセルデータを共有メモリに書き込み
                int[] pixels = kernel.getPixels();
                if (pixels != null && pixels.length > 0) {
                    shm.writePixels(pixels);
                }

                // 6. サーバー状態を更新
                updateServerState();

                // 7. フレームレート制御
                long elapsed = System.currentTimeMillis() - startTime;
                long sleepTime = frameTime - elapsed;
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime);
                }

            } catch (InterruptedException e) {
                LOGGER.info("[MMOSServer] Interrupted, shutting down...");
                running = false;
            } catch (Exception e) {
                LOGGER.error("[MMOSServer] Error in main loop", e);
            }
        }

        shutdown();
    }

    /**
     * コマンドを処理する。
     */
    private void processCommands() {
        int cmdType = shm.getCommandType();
        if (cmdType == IPCConstants.CMD_NONE) {
            return;
        }

        LOGGER.info("[MMOSServer] Processing command: {}", cmdType);

        switch (cmdType) {
            case IPCConstants.CMD_SHUTDOWN:
                LOGGER.info("[MMOSServer] Received SHUTDOWN command");
                running = false;
                break;

            case IPCConstants.CMD_SLEEP:
                LOGGER.info("[MMOSServer] Received SLEEP command");
                kernel.sleep();
                break;

            case IPCConstants.CMD_WAKE:
                LOGGER.info("[MMOSServer] Received WAKE command");
                kernel.wake();
                break;

            case IPCConstants.CMD_GO_HOME:
                LOGGER.info("[MMOSServer] Received GO_HOME command");
                kernel.requestGoHome();
                break;

            case IPCConstants.CMD_HOME_BUTTON:
                LOGGER.info("[MMOSServer] Received HOME_BUTTON command");
                kernel.handleHomeButton();
                break;

            case IPCConstants.CMD_FRAMERATE:
                int fps = shm.getCommandArg1();
                LOGGER.info("[MMOSServer] Received FRAMERATE command: {}", fps);
                targetFrameRate = fps > 0 ? fps : 60;
                kernel.frameRate(fps);
                break;

            case IPCConstants.CMD_ADD_LAYER:
                int layerOrdinal = shm.getCommandArg1();
                LOGGER.info("[MMOSServer] Received ADD_LAYER command: {}", layerOrdinal);
                // TODO: LayerType enumからordinalで取得
                break;

            case IPCConstants.CMD_REMOVE_LAYER:
                int removeLayerOrdinal = shm.getCommandArg1();
                LOGGER.info("[MMOSServer] Received REMOVE_LAYER command: {}", removeLayerOrdinal);
                // TODO: LayerType enumからordinalで取得
                break;

            default:
                LOGGER.warn("[MMOSServer] Unknown command type: {}", cmdType);
        }

        // コマンドをクリア
        shm.clearCommand();
    }

    /**
     * 入力イベントを処理する。
     */
    private void processInputEvents() {
        InputEvent event;
        int processed = 0;

        while ((event = shm.pollInputEvent()) != null && processed < 100) {
            processed++;

            try {
                switch (event.type) {
                    case IPCConstants.INPUT_MOUSE_PRESSED:
                        kernel.mousePressed(event.x, event.y);
                        break;

                    case IPCConstants.INPUT_MOUSE_RELEASED:
                        kernel.mouseReleased(event.x, event.y);
                        break;

                    case IPCConstants.INPUT_MOUSE_DRAGGED:
                        kernel.mouseDragged(event.x, event.y);
                        break;

                    case IPCConstants.INPUT_MOUSE_MOVED:
                        kernel.mouseMoved(event.x, event.y);
                        break;

                    case IPCConstants.INPUT_MOUSE_WHEEL:
                        kernel.mouseWheel(event.x, event.y, event.delta);
                        break;

                    case IPCConstants.INPUT_KEY_PRESSED:
                        kernel.keyPressed(event.keyChar, event.keyCode);
                        break;

                    case IPCConstants.INPUT_KEY_RELEASED:
                        kernel.keyReleased(event.keyChar, event.keyCode);
                        break;

                    default:
                        LOGGER.warn("[MMOSServer] Unknown input event type: {}", event.type);
                }
            } catch (Exception e) {
                LOGGER.error("[MMOSServer] Error processing input event: {}", event, e);
            }
        }
    }

    /**
     * サーバー状態を共有メモリに書き込む。
     */
    private void updateServerState() {
        ServerState state = new ServerState();
        state.frameCount = kernel.frameCount;
        state.frameRate = kernel.getFrameRate();
        state.sleeping = kernel.isSleeping();
        state.debugMode = kernel.isDebugMode();
        state.hasTextInputFocus = kernel.hasTextInputFocus();
        state.serverReady = true;
        state.clientConnected = shm.isClientConnected();

        // トップクローズ可能レイヤー
        var topLayer = kernel.getTopMostClosableLayer();
        state.topClosableLayer = topLayer != null ? topLayer.ordinal() : -1;

        shm.writeState(state);
    }

    /**
     * シャットダウン処理。
     */
    private void shutdown() {
        LOGGER.info("[MMOSServer] Shutting down...");

        try {
            // APIハンドラをシャットダウン
            if (apiHandler != null) {
                apiHandler.shutdown();
            }

            // カーネルをシャットダウン
            if (kernel != null) {
                kernel.shutdown();
            }

            // 共有メモリを閉じる
            if (shm != null) {
                shm.close();
            }

            LOGGER.info("[MMOSServer] Shutdown complete");
        } catch (Exception e) {
            LOGGER.error("[MMOSServer] Error during shutdown", e);
        }
    }

    /**
     * メインエントリポイント。
     *
     * @param args コマンドライン引数（args[0] = worldId）
     */
    public static void main(String[] args) {
        String worldId = args.length > 0 ? args[0] : "default";

        LOGGER.info("==============================================");
        LOGGER.info("  MochiMobileOS Server");
        LOGGER.info("  World ID: {}", worldId);
        LOGGER.info("==============================================");

        try {
            MMOSServer server = new MMOSServer(worldId);

            // シャットダウンフックを追加
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("[MMOSServer] Shutdown hook triggered");
                server.running = false;
            }));

            // メインループを開始
            server.run();

        } catch (Exception e) {
            LOGGER.error("[MMOSServer] Failed to start server", e);
            System.exit(1);
        }
    }
}
