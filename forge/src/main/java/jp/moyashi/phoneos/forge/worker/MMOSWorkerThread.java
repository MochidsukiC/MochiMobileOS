package jp.moyashi.phoneos.forge.worker;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumProvider;
import jp.moyashi.phoneos.core.service.chromium.ChromiumService;
import jp.moyashi.phoneos.core.service.chromium.DefaultChromiumService;
import jp.moyashi.phoneos.forge.chromium.ForgeChromiumProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * MMOS を別スレッドで実行するワーカースレッド。
 * jcefmaven を使用して JCEF を独立した OpenGL コンテキストで動作させ、
 * CefRequestHandler による IPvM インターセプトを可能にする。
 *
 * <p>アーキテクチャ:</p>
 * <pre>
 * Minecraft Main Thread:
 *   - ProcessingScreen.render() → getLatestPixels() でピクセル取得
 *   - 入力イベント → queueInput() でキューに追加
 *
 * MMOS Worker Thread:
 *   - 60fps ループで Kernel.update() / render()
 *   - 入力イベントをキューから取り出して処理
 *   - ピクセルデータを AtomicReference で公開
 *   - CEF メッセージループを駆動
 * </pre>
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class MMOSWorkerThread {

    private static final Logger LOGGER = LogManager.getLogger();

    /** ターゲットフレームレート（60fps） */
    private static final long TARGET_FRAME_TIME_NS = 16_666_666L; // 1秒 / 60

    /** Kernel インスタンス */
    private Kernel kernel;

    /** JCEF プロバイダー（Forge環境専用、jcefmavenをバイパス） */
    private ForgeChromiumProvider jcefProvider;

    /** ワーカースレッド */
    private Thread workerThread;

    /** 実行フラグ */
    private volatile boolean running = false;

    /** 初期化完了フラグ */
    private volatile boolean initialized = false;

    /** 初期化完了待機用ラッチ */
    private final CountDownLatch initLatch = new CountDownLatch(1);

    /** 最新ピクセルデータ（スレッド間共有） */
    private final AtomicReference<int[]> latestPixels = new AtomicReference<>();

    /** 入力イベントキュー */
    private final ConcurrentLinkedQueue<InputEvent> inputQueue = new ConcurrentLinkedQueue<>();

    /** ワールドID（データ分離用） */
    private final String worldId;

    /** 画面サイズ */
    private final int screenWidth;
    private final int screenHeight;

    /** エラーメッセージ（初期化失敗時） */
    private volatile String errorMessage = null;

    /**
     * MMOSWorkerThread を構築する。
     *
     * @param worldId      ワールドID（データ分離用）
     * @param screenWidth  画面幅
     * @param screenHeight 画面高さ
     */
    public MMOSWorkerThread(String worldId, int screenWidth, int screenHeight) {
        this.worldId = worldId;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        LOGGER.info("[MMOSWorkerThread] Created for world: " + worldId + " (" + screenWidth + "x" + screenHeight + ")");
    }

    /**
     * ワーカースレッドを開始する。
     * 初期化が完了するまでブロックする（タイムアウト: 30秒）。
     *
     * @return 初期化成功時 true
     */
    public boolean start() {
        if (running) {
            LOGGER.warn("[MMOSWorkerThread] Already running");
            return true;
        }

        running = true;
        workerThread = new Thread(this::runLoop, "MMOS-Worker-" + worldId);
        workerThread.setDaemon(true);
        workerThread.start();

        // 初期化完了を待機
        try {
            boolean success = initLatch.await(30, TimeUnit.SECONDS);
            if (!success) {
                LOGGER.error("[MMOSWorkerThread] Initialization timeout");
                shutdown();
                return false;
            }
            return initialized;
        } catch (InterruptedException e) {
            LOGGER.error("[MMOSWorkerThread] Initialization interrupted");
            shutdown();
            return false;
        }
    }

    /**
     * ワーカースレッドを停止する。
     */
    public void shutdown() {
        LOGGER.info("[MMOSWorkerThread] Shutting down...");
        running = false;

        if (workerThread != null) {
            try {
                workerThread.join(5000);
                if (workerThread.isAlive()) {
                    LOGGER.warn("[MMOSWorkerThread] Thread did not terminate, interrupting...");
                    workerThread.interrupt();
                }
            } catch (InterruptedException e) {
                LOGGER.error("[MMOSWorkerThread] Shutdown interrupted");
            }
        }

        // リソース解放
        // kernel.shutdown() が ChromiumService 経由で ForgeChromiumProvider.shutdown() を呼び出す
        if (kernel != null) {
            try {
                kernel.shutdown();
            } catch (Exception e) {
                LOGGER.error("[MMOSWorkerThread] Kernel shutdown error: " + e.getMessage());
            }
            kernel = null;
        }

        // jcefProvider は kernel.shutdown() 内でシャットダウンされるため、
        // ここでは参照をクリアするだけ
        jcefProvider = null;

        LOGGER.info("[MMOSWorkerThread] Shutdown complete");
    }

    /**
     * ワーカースレッドのメインループ。
     */
    private void runLoop() {
        LOGGER.info("[MMOSWorkerThread] Worker thread started");

        try {
            // 初期化
            if (!initialize()) {
                LOGGER.error("[MMOSWorkerThread] Initialization failed");
                return;
            }

            initialized = true;
            initLatch.countDown();
            LOGGER.info("[MMOSWorkerThread] Initialization complete, entering main loop");

            // メインループ
            while (running) {
                long frameStart = System.nanoTime();

                try {
                    // 入力イベント処理
                    processInputEvents();

                    // Kernel 更新
                    // kernel.update() が ChromiumService.update() を呼び出し、
                    // そこで CEF メッセージループが駆動される
                    kernel.update();
                    kernel.render();

                    // ピクセルデータを公開
                    publishPixels();

                } catch (Exception e) {
                    LOGGER.error("[MMOSWorkerThread] Frame error: " + e.getMessage(), e);
                }

                // フレームレート制御
                long elapsed = System.nanoTime() - frameStart;
                long sleepNs = TARGET_FRAME_TIME_NS - elapsed;
                if (sleepNs > 0) {
                    LockSupport.parkNanos(sleepNs);
                }
            }

        } catch (Exception e) {
            LOGGER.error("[MMOSWorkerThread] Fatal error: " + e.getMessage(), e);
            errorMessage = e.getMessage();
        } finally {
            initLatch.countDown(); // 初期化失敗時も解放
            LOGGER.info("[MMOSWorkerThread] Worker thread exiting");
        }
    }

    /**
     * Kernel と JCEF を初期化する。
     *
     * @return 成功時 true
     */
    private boolean initialize() {
        try {
            LOGGER.info("[MMOSWorkerThread] Initializing Kernel...");

            // Kernel 作成
            kernel = new Kernel();

            // ForgeChromiumProvider 作成（jcefmavenのbuild()をバイパス）
            // MMOSInstallerでインストールしたネイティブを直接使用する
            jcefProvider = new ForgeChromiumProvider();

            // ChromiumService を注入
            ChromiumService chromiumService = new DefaultChromiumService(jcefProvider);
            kernel.setChromiumService(chromiumService);

            // Minecraft 環境用初期化
            kernel.initializeForMinecraft(screenWidth, screenHeight, worldId);

            // ネットワーク初期化（仮想ルーター設定）
            jp.moyashi.phoneos.forge.network.NetworkHandler.setupVirtualRouter(kernel);
            jp.moyashi.phoneos.forge.network.SystemPacketHandler.registerHandlers(kernel);

            // ハードウェアAPI初期化
            initializeHardwareAPIs();

            LOGGER.info("[MMOSWorkerThread] Kernel initialized successfully");
            return true;

        } catch (Exception e) {
            LOGGER.error("[MMOSWorkerThread] Initialization error: " + e.getMessage(), e);
            errorMessage = e.getMessage();
            return false;
        }
    }

    /**
     * ハードウェアAPIを初期化する。
     * プレイヤー情報は後で updatePlayer() で設定される。
     */
    private void initializeHardwareAPIs() {
        try {
            // プレイヤーなしで初期化（後で更新）
            kernel.setMobileDataSocket(new jp.moyashi.phoneos.forge.hardware.ForgeMobileDataSocket(null));
            kernel.setBluetoothSocket(new jp.moyashi.phoneos.forge.hardware.ForgeBluetoothSocket(null));
            kernel.setLocationSocket(new jp.moyashi.phoneos.forge.hardware.ForgeLocationSocket(null));
            kernel.setBatteryInfo(new jp.moyashi.phoneos.forge.hardware.ForgeBatteryInfo(null));
            kernel.setCameraSocket(new jp.moyashi.phoneos.forge.hardware.ForgeCameraSocket());
            kernel.setMicrophoneSocket(new jp.moyashi.phoneos.forge.hardware.ForgeMicrophoneSocket());
            kernel.setSpeakerSocket(new jp.moyashi.phoneos.core.service.hardware.DefaultSpeakerSocket());
            kernel.setICSocket(new jp.moyashi.phoneos.forge.hardware.ForgeICSocket());
            kernel.setSIMInfo(new jp.moyashi.phoneos.forge.hardware.ForgeSIMInfo(null));

            LOGGER.info("[MMOSWorkerThread] Hardware APIs initialized");
        } catch (Exception e) {
            LOGGER.error("[MMOSWorkerThread] Hardware API initialization error: " + e.getMessage());
        }
    }

    /**
     * 入力イベントをキューから取り出して処理する。
     */
    private void processInputEvents() {
        InputEvent event;
        while ((event = inputQueue.poll()) != null) {
            try {
                switch (event.type) {
                    case MOUSE_PRESSED:
                        kernel.mousePressed(event.x, event.y);
                        break;
                    case MOUSE_RELEASED:
                        kernel.mouseReleased(event.x, event.y);
                        break;
                    case MOUSE_DRAGGED:
                        kernel.mouseDragged(event.x, event.y);
                        break;
                    case MOUSE_MOVED:
                        kernel.mouseMoved(event.x, event.y);
                        break;
                    case MOUSE_WHEEL:
                        kernel.mouseWheel(event.x, event.y, event.delta);
                        break;
                    case KEY_PRESSED:
                        kernel.keyPressed(event.key, event.keyCode);
                        break;
                    case KEY_RELEASED:
                        kernel.keyReleased(event.key, event.keyCode);
                        break;
                }
            } catch (Exception e) {
                LOGGER.error("[MMOSWorkerThread] Input event error: " + e.getMessage());
            }
        }
    }

    /**
     * ピクセルデータを AtomicReference に公開する。
     */
    private void publishPixels() {
        try {
            int[] pixels = kernel.getPixels();
            if (pixels != null && pixels.length > 0) {
                // コピーを作成してスレッドセーフを確保
                latestPixels.set(pixels.clone());
            }
        } catch (Exception e) {
            LOGGER.error("[MMOSWorkerThread] Pixel publish error: " + e.getMessage());
        }
    }

    // =========================================================================
    // 公開 API（Minecraft Main Thread から呼び出し）
    // =========================================================================

    /**
     * 最新のピクセルデータを取得する。
     * 取得後、内部参照はクリアされる（ダブルバッファリング）。
     *
     * @return ピクセル配列、または null（データなし）
     */
    public int[] getLatestPixels() {
        return latestPixels.getAndSet(null);
    }

    /**
     * 最新のピクセルデータを取得する（クリアしない）。
     *
     * @return ピクセル配列、または null（データなし）
     */
    public int[] peekLatestPixels() {
        return latestPixels.get();
    }

    /**
     * 入力イベントをキューに追加する。
     *
     * @param event 入力イベント
     */
    public void queueInput(InputEvent event) {
        if (running && event != null) {
            inputQueue.offer(event);
        }
    }

    /**
     * ハードウェアAPIのプレイヤー情報を更新する。
     *
     * @param player Minecraft プレイヤー
     */
    public void updatePlayer(net.minecraft.world.entity.player.Player player) {
        if (kernel == null || player == null) {
            return;
        }

        try {
            if (kernel.getMobileDataSocket() instanceof jp.moyashi.phoneos.forge.hardware.ForgeMobileDataSocket) {
                ((jp.moyashi.phoneos.forge.hardware.ForgeMobileDataSocket) kernel.getMobileDataSocket()).updatePlayer(player);
            }
            if (kernel.getBluetoothSocket() instanceof jp.moyashi.phoneos.forge.hardware.ForgeBluetoothSocket) {
                ((jp.moyashi.phoneos.forge.hardware.ForgeBluetoothSocket) kernel.getBluetoothSocket()).updatePlayer(player);
            }
            if (kernel.getLocationSocket() instanceof jp.moyashi.phoneos.forge.hardware.ForgeLocationSocket) {
                ((jp.moyashi.phoneos.forge.hardware.ForgeLocationSocket) kernel.getLocationSocket()).updatePlayer(player);
            }
            if (kernel.getSIMInfo() instanceof jp.moyashi.phoneos.forge.hardware.ForgeSIMInfo) {
                ((jp.moyashi.phoneos.forge.hardware.ForgeSIMInfo) kernel.getSIMInfo()).updatePlayer(player);
            }
        } catch (Exception e) {
            LOGGER.error("[MMOSWorkerThread] Player update error: " + e.getMessage());
        }
    }

    /**
     * Kernel がスリープ中かどうかを確認する。
     *
     * @return スリープ中の場合 true
     */
    public boolean isKernelSleeping() {
        return kernel != null && kernel.isSleeping();
    }

    /**
     * Kernel をウェイクする。
     */
    public void wakeKernel() {
        if (kernel != null && kernel.isSleeping()) {
            kernel.wake();
        }
    }

    /**
     * Kernel をスリープさせる。
     */
    public void sleepKernel() {
        if (kernel != null && !kernel.isSleeping()) {
            kernel.sleep();
        }
    }

    /**
     * ホーム画面に戻るリクエストを送信する。
     */
    public void requestGoHome() {
        if (kernel != null) {
            kernel.requestGoHome();
        }
    }

    /**
     * 初期化完了しているかどうかを確認する。
     *
     * @return 初期化完了の場合 true
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * 実行中かどうかを確認する。
     *
     * @return 実行中の場合 true
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * エラーメッセージを取得する。
     *
     * @return エラーメッセージ、または null
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Kernel インスタンスを取得する（デバッグ用）。
     *
     * @return Kernel インスタンス
     */
    public Kernel getKernel() {
        return kernel;
    }
}
