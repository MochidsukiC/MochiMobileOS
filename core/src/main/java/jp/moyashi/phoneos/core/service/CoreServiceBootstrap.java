package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.NotificationManager;
import jp.moyashi.phoneos.core.service.clipboard.ClipboardManager;
import jp.moyashi.phoneos.core.service.clipboard.SimpleClipboardManager;
import jp.moyashi.phoneos.core.service.ControlCenterManager;
import jp.moyashi.phoneos.core.service.LockManager;
import jp.moyashi.phoneos.core.service.ServiceManager;
import jp.moyashi.phoneos.core.service.MessageStorage;
import jp.moyashi.phoneos.core.service.chromium.ChromiumService;
import jp.moyashi.phoneos.core.service.chromium.ChromiumProvider;
import jp.moyashi.phoneos.core.service.chromium.DefaultChromiumService;
import jp.moyashi.phoneos.core.service.chromium.NoOpChromiumService;
import jp.moyashi.phoneos.core.service.network.VirtualRouter;
import jp.moyashi.phoneos.core.service.hardware.*;
import jp.moyashi.phoneos.core.service.sensor.SensorManager;
import jp.moyashi.phoneos.core.service.sensor.SensorManagerImpl;
import jp.moyashi.phoneos.core.service.BatteryMonitor;
import jp.moyashi.phoneos.core.service.SettingsManager;
import jp.moyashi.phoneos.core.ui.ScreenManager;
import jp.moyashi.phoneos.core.ui.popup.PopupManager;
import jp.moyashi.phoneos.core.ui.theme.ThemeEngine;
import jp.moyashi.phoneos.core.input.InputManager;
import jp.moyashi.phoneos.core.input.GestureManager;
import jp.moyashi.phoneos.core.render.RenderPipeline;
import jp.moyashi.phoneos.core.power.PowerManager;
import jp.moyashi.phoneos.core.lifecycle.SystemLifecycleManager;
import jp.moyashi.phoneos.core.memory.MemoryManager;
import jp.moyashi.phoneos.core.filesystem.FileSystemManager;
import jp.moyashi.phoneos.core.resource.ResourceCache;
import jp.moyashi.phoneos.core.ResourceBundle;
import processing.core.PGraphics;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * コアサービスのブートストラップクラス。
 * すべてのシステムサービスの初期化と依存関係の解決を管理する。
 *
 * 責務:
 * - サービスの登録順序の管理（依存関係を考慮）
 * - サービスのライフサイクル設定
 * - エラーハンドリングとフォールバック
 * - 起動時診断とロギング
 *
 * @since 2025-12-02
 * @version 1.0
 */
public class CoreServiceBootstrap {

    private static final Logger logger = Logger.getLogger(CoreServiceBootstrap.class.getName());

    /** サービスコンテナ */
    private final ServiceContainer container;

    /** Kernelインスタンス */
    private final Kernel kernel;

    /** 初期化完了フラグ */
    private boolean isInitialized = false;

    /**
     * CoreServiceBootstrapを初期化する。
     *
     * @param kernel Kernelインスタンス
     */
    public CoreServiceBootstrap(Kernel kernel) {
        this.kernel = kernel;
        this.container = new ServiceContainer();
    }

    /**
     * すべてのコアサービスを初期化する。
     * 依存関係の順序を考慮して登録する。
     *
     * @param graphics PGraphicsインスタンス
     * @return 初期化に成功した場合true
     */
    public boolean initialize(PGraphics graphics) {
        if (isInitialized) {
            logger.warning("Services already initialized");
            return false;
        }

        logger.info("Starting core service initialization...");

        try {
            // Phase 1: 基盤サービス（依存なし）
            registerFoundationServices();

            // Phase 2: システムサービス（基盤サービスに依存）
            registerSystemServices();

            // Phase 3: UIサービス（システムサービスに依存）
            registerUIServices(graphics);

            // Phase 4: アプリケーションサービス（UIサービスに依存）
            registerApplicationServices();

            // Phase 5: 管理サービス（すべてのサービスに依存）
            registerManagementServices();

            // コンテナをロックして新規登録を防ぐ
            container.lock();

            isInitialized = true;
            logger.info("Core service initialization completed. Total services: " +
                       container.getServiceCount());

            return true;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize core services", e);
            return false;
        }
    }

    /**
     * 基盤サービスを登録する（依存関係なし）。
     */
    private void registerFoundationServices() {
        logger.fine("Registering foundation services...");

        // リソース管理
        container.registerSingleton(ResourceBundle.class,
            () -> new ResourceBundle());

        // メモリ管理（シングルトン）
        container.registerSingleton(MemoryManager.class,
            () -> {
                MemoryManager manager = MemoryManager.getInstance();
                manager.startMonitoring(); // 監視を開始
                return manager;
            });

        // ファイルシステム
        container.registerSingleton(FileSystemManager.class,
            () -> new FileSystemManager(kernel));

        // リソースキャッシュ（シングルトン）
        container.registerSingleton(ResourceCache.class,
            () -> ResourceCache.getInstance());

        // VFSサービス（早期初期化が必要）
        container.registerSingleton(VFS.class,
            () -> {
                // Kernelから既存のVFSを取得、または新規作成
                VFS vfs = kernel.getVFS();
                if (vfs == null) {
                    vfs = new VFS();
                    kernel.setVFS(vfs);
                }
                return vfs;
            });

        // ログサービス（VFSに依存）
        container.registerSingleton(LoggerService.class,
            () -> new LoggerService(container.resolve(VFS.class)));

        // 設定管理はVFSを使用
        container.registerSingleton(SettingsManager.class,
            () -> new SettingsManager(container.resolve(VFS.class)));
    }

    /**
     * システムサービスを登録する。
     */
    private void registerSystemServices() {
        logger.fine("Registering system services...");

        // システムクロック
        container.registerSingleton(SystemClock.class,
            () -> new SystemClock());

        // 電源管理
        container.registerSingleton(PowerManager.class,
            () -> new PowerManager(kernel));

        // ライフサイクル管理
        container.registerSingleton(SystemLifecycleManager.class,
            () -> new SystemLifecycleManager(kernel));

        // クリップボード（SimpleClipboardManager実装を使用）
        container.registerSingleton(ClipboardManager.class,
            () -> new SimpleClipboardManager());

        // 通知システム（既存実装を使用）
        container.registerSingleton(NotificationManager.class,
            () -> new NotificationManager());

        // コントロールセンター管理
        container.registerSingleton(ControlCenterManager.class,
            () -> new ControlCenterManager());

        // ロック管理（SettingsManagerに依存）
        container.registerSingleton(LockManager.class,
            () -> new LockManager(container.resolve(SettingsManager.class)));

        // サービスマネージャー（Kernelに依存、特殊ケース）
        // 注: ServiceManagerはKernelに依存するため、循環依存を避けるため
        // Kernelで直接作成される場合があります
        container.registerLazySingleton(ServiceManager.class,
            () -> new ServiceManager(kernel));

        // メッセージストレージ（VFSに依存）
        container.registerSingleton(MessageStorage.class,
            () -> new MessageStorage(container.resolve(VFS.class)));

        // 仮想ネットワークルーター
        container.registerSingleton(VirtualRouter.class,
            () -> new VirtualRouter());

        // ハードウェアバイパスAPI - モバイルデータ
        container.registerSingleton(MobileDataSocket.class,
            () -> new DefaultMobileDataSocket());

        // ハードウェアバイパスAPI - Bluetooth
        container.registerSingleton(BluetoothSocket.class,
            () -> new DefaultBluetoothSocket());

        // ハードウェアバイパスAPI - 位置情報
        container.registerSingleton(LocationSocket.class,
            () -> new DefaultLocationSocket());

        // ハードウェアバイパスAPI - バッテリー情報
        container.registerSingleton(BatteryInfo.class,
            () -> new DefaultBatteryInfo());

        // ハードウェアバイパスAPI - カメラ
        container.registerSingleton(CameraSocket.class,
            () -> new DefaultCameraSocket());

        // ハードウェアバイパスAPI - マイクロフォン
        container.registerSingleton(MicrophoneSocket.class,
            () -> new DefaultMicrophoneSocket());

        // ハードウェアバイパスAPI - スピーカー
        container.registerSingleton(SpeakerSocket.class,
            () -> new DefaultSpeakerSocket());

        // ハードウェアバイパスAPI - IC（近距離通信）
        container.registerSingleton(ICSocket.class,
            () -> new DefaultICSocket());

        // ハードウェアバイパスAPI - SIM情報
        container.registerSingleton(SIMInfo.class,
            () -> new DefaultSIMInfo());

        // センサー管理（Kernelに依存）
        container.registerLazySingleton(SensorManager.class,
            () -> new SensorManagerImpl(kernel));

        // バッテリー監視（BatteryInfoとSettingsManagerに依存）
        container.registerSingleton(BatteryMonitor.class,
            () -> new BatteryMonitor(
                container.resolve(BatteryInfo.class),
                container.resolve(SettingsManager.class)
            ));

        // ChromiumService（ChromiumProviderが必要なため、遅延初期化）
        // 注: ChromiumProviderは環境依存のため、各サブモジュールで設定される
        container.registerLazySingleton(ChromiumService.class,
            () -> {
                ChromiumProvider provider = container.tryResolve(ChromiumProvider.class);
                if (provider != null) {
                    logger.fine("ChromiumProvider found, creating DefaultChromiumService");
                    return new DefaultChromiumService(provider);
                }
                // プロバイダーが登録されていない場合はNoOp実装を返す
                logger.fine("ChromiumProvider not found, using NoOpChromiumService");
                return new NoOpChromiumService();
            });

        // サウンド管理（将来実装）
        // TODO: SoundManagerクラスを実装
    }

    /**
     * UIサービスを登録する。
     */
    private void registerUIServices(PGraphics graphics) {
        logger.fine("Registering UI services...");

        // テーマエンジン
        container.registerSingleton(ThemeEngine.class,
            () -> {
                SettingsManager settings = container.resolve(SettingsManager.class);
                return new ThemeEngine(settings);
            });

        // アニメーション管理（将来実装）
        // TODO: AnimationManagerクラスを実装

        // ポップアップ管理（既存のコンストラクタを使用）
        container.registerSingleton(PopupManager.class,
            () -> new PopupManager());

        // スクリーン管理（既存のコンストラクタを使用）
        container.registerSingleton(ScreenManager.class,
            () -> new ScreenManager());

        // レンダリングパイプライン（パラメータを指定）
        container.registerSingleton(RenderPipeline.class,
            () -> new RenderPipeline(kernel, kernel.width, kernel.height));
    }

    /**
     * アプリケーションサービスを登録する。
     */
    private void registerApplicationServices() {
        logger.fine("Registering application services...");

        // アプリケーションローダー（VFSに依存）
        container.registerSingleton(AppLoader.class,
            () -> new AppLoader(container.resolve(VFS.class)));

        // レイアウト管理（VFSとAppLoaderに依存）
        container.registerSingleton(LayoutManager.class,
            () -> new LayoutManager(
                container.resolve(VFS.class),
                container.resolve(AppLoader.class)
            ));

        // インテント管理（将来実装）
        // TODO: IntentManagerクラスを実装

        // ジェスチャー管理（既存のコンストラクタを使用）
        container.registerSingleton(GestureManager.class,
            () -> {
                // LoggerServiceを使用するコンストラクタ
                LoggerService logger = container.tryResolve(LoggerService.class);
                return new GestureManager(logger != null ? logger : kernel.getLogger());
            });

        // 入力管理（最後に登録：多くのサービスに依存）
        container.registerSingleton(InputManager.class,
            () -> new InputManager(kernel));
    }

    /**
     * 管理サービスを登録する（診断・監視用）。
     */
    private void registerManagementServices() {
        logger.fine("Registering management services...");

        // サービス診断用（将来の拡張）
        // container.registerLazySingleton(ServiceDiagnostics.class,
        //     () -> new ServiceDiagnostics(container));
    }

    /**
     * サービスを取得する。
     *
     * @param <T> サービスの型
     * @param serviceClass サービスクラス
     * @return サービスインスタンス
     */
    public <T> T getService(Class<T> serviceClass) {
        if (!isInitialized) {
            throw new IllegalStateException("Services not initialized. Call initialize() first.");
        }
        return container.resolve(serviceClass);
    }

    /**
     * サービスを安全に取得する（存在しない場合はnull）。
     *
     * @param <T> サービスの型
     * @param serviceClass サービスクラス
     * @return サービスインスタンス、または null
     */
    public <T> T tryGetService(Class<T> serviceClass) {
        if (!isInitialized) {
            return null;
        }
        return container.tryResolve(serviceClass);
    }

    /**
     * 特定のサービスが登録されているかを確認する。
     *
     * @param serviceClass サービスクラス
     * @return 登録されている場合 true
     */
    public boolean hasService(Class<?> serviceClass) {
        return container.isRegistered(serviceClass);
    }

    /**
     * ServiceContainerを取得する（高度な使用向け）。
     *
     * @return ServiceContainerインスタンス
     */
    protected ServiceContainer getContainer() {
        return container;
    }

    /**
     * すべてのサービスをシャットダウンする。
     */
    public void shutdown() {
        logger.info("Shutting down core services...");

        // ライフサイクル管理サービスがある場合は通知
        SystemLifecycleManager lifecycle = tryGetService(SystemLifecycleManager.class);
        if (lifecycle != null) {
            lifecycle.shutdown();
        }

        // その他のクリーンアップ処理
        // 各サービスのdispose()メソッドを呼び出すなど

        isInitialized = false;
        logger.info("Core services shutdown completed");
    }

    /**
     * 初期化状態を取得する。
     *
     * @return 初期化済みの場合 true
     */
    public boolean isInitialized() {
        return isInitialized;
    }

    /**
     * サービスの統計情報を取得する。
     *
     * @return 登録済みサービス数
     */
    public int getServiceCount() {
        return container.getServiceCount();
    }
}