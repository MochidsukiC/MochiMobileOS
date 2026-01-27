package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.service.AppLoader;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PGraphics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * アプリケーションプロセスの管理を担当するサービスマネージャー。
 * Linuxのsystemd風の機能を提供し、アプリのライフサイクル、バックグラウンド処理、
 * 優先度管理、統計情報収集などを行う。
 *
 * 主な機能:
 * - アプリインスタンスのシングルトン管理（多重起動防止）
 * - バックグラウンドタスク管理（tick処理）
 * - 自動起動システム（background処理）
 * - プロセス優先度管理
 * - ライフサイクルイベント通知
 * - 統計情報収集
 *
 * @author MochiMobileOS
 * @version 1.0
 */
public class ServiceManager {

    private static final String TAG = "ServiceManager";

    private final Kernel kernel;
    private final ServiceConfig config;

    // アプリインスタンス管理（appId -> ProcessInfo）
    private final Map<String, ProcessInfo> processes;

    // フレームカウンタ（優先度判定用）
    private long frameCount;

    /**
     * ServiceManagerを作成する。
     *
     * @param kernel Kernelインスタンス
     */
    public ServiceManager(Kernel kernel) {
        this.kernel = kernel;
        this.config = new ServiceConfig(kernel.getVFS());
        this.processes = new ConcurrentHashMap<>();
        this.frameCount = 0;
    }

    // ==================== ロギングヘルパー ====================

    private void logInfo(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().info(TAG, message);
        }
    }

    private void logDebug(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().debug(TAG, message);
        }
    }

    private void logWarn(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().warn(TAG, message);
        }
    }

    private void logError(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error(TAG, message);
        }
    }

    // ==================== 初期化とシャットダウン ====================

    /**
     * ServiceManagerを初期化する。
     * 1. hasBackgroundService()がtrueを返すアプリを自動検出して初期化
     * 2. 自動起動リストのアプリをバックグラウンドサービスとして起動
     */
    public void initialize() {
        logInfo("Initializing...");

        // Phase 1: AppLoaderに登録されている全アプリをスキャンし、
        // hasBackgroundService()がtrueを返すアプリを自動的に初期化
        AppLoader appLoader = kernel.getAppLoader();
        if (appLoader != null) {
            for (IApplication app : appLoader.getLoadedApps()) {
                if (app.hasBackgroundService()) {
                    String appId = app.getApplicationId();
                    // まだ初期化されていない場合のみ初期化
                    if (!processes.containsKey(appId)) {
                        logInfo("Auto-detected background service app: " + appId);
                        try {
                            initializeBackgroundService(appId);
                            // autostartリストにも追加（次回起動時のため）
                            config.addAutostartApp(appId);
                        } catch (Exception e) {
                            logError("Failed to initialize auto-detected background service " + appId + ": " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        // Phase 2: 自動起動リストからも初期化（Phase 1で漏れたアプリ用）
        Set<String> autostartApps = config.getAutostartApps();
        logInfo("Autostart apps from config: " + autostartApps);

        for (String appId : autostartApps) {
            // 既に初期化済みの場合はスキップ
            if (processes.containsKey(appId)) {
                logDebug("Skipping already initialized: " + appId);
                continue;
            }
            try {
                initializeBackgroundService(appId);
            } catch (Exception e) {
                logError("Failed to initialize background service " + appId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        logInfo("Initialized with " + processes.size() + " background services");
    }

    /**
     * ServiceManagerをシャットダウンする。
     * 全てのプロセスをクリーンアップする。
     */
    public void shutdown() {
        logInfo("Shutting down...");

        // 全てのプロセスをクリーンアップ
        for (ProcessInfo info : processes.values()) {
            try {
                // フォアグラウンドスクリーンのクリーンアップ
                Screen foregroundScreen = info.getForegroundScreen();
                if (foregroundScreen != null) {
                    foregroundScreen.cleanup((PGraphics) null);
                }
                // バックグラウンドサービススクリーンのクリーンアップ
                Screen backgroundServiceScreen = info.getBackgroundServiceScreen();
                if (backgroundServiceScreen != null) {
                    backgroundServiceScreen.cleanup((PGraphics) null);
                }
            } catch (Exception e) {
                logError("Error during cleanup of " + info.getAppId() + ": " + e.getMessage());
            }
        }

        processes.clear();
        logInfo("Shutdown complete");
    }

    // ==================== アプリ起動 ====================

    /**
     * アプリを起動する。
     * IApplicationインスタンスはシングルトンとして管理され、ProcessInfoに保持される。
     * フォアグラウンド用スクリーンはIApplication.getEntryScreen()から取得する。
     * バックグラウンドサービス用スクリーンとは完全に分離される。
     *
     * @param appId アプリID
     * @return Screenインスタンス、起動失敗時はnull
     */
    public Screen launchApp(String appId) {
        logInfo("Launching app: " + appId);
        logDebug("Current processes keys: " + processes.keySet());

        // 既存のプロセスがあるか確認
        ProcessInfo existingInfo = processes.get(appId);
        if (existingInfo != null) {
            logInfo("ProcessInfo exists for " + appId + ", getting entry screen from IApplication");
            existingInfo.incrementLaunchCount();
            existingInfo.setForeground(true);

            // IApplicationインスタンスからエントリースクリーンを取得
            IApplication app = existingInfo.getApplication();
            if (app == null) {
                logError("IApplication is null for existing ProcessInfo: " + appId);
                return null;
            }

            try {
                // フォアグラウンド用のエントリースクリーンを取得
                Screen entryScreen = app.getEntryScreen(kernel);
                if (entryScreen == null) {
                    logError("Failed to get entry screen for " + appId);
                    existingInfo.incrementCrashCount();
                    return null;
                }

                entryScreen.setApplicationId(appId);
                existingInfo.setForegroundScreen(entryScreen);

                // onForegroundを呼び出す
                try {
                    entryScreen.onForeground();
                } catch (Exception e) {
                    logError("Error calling onForeground for " + appId + ": " + e.getMessage());
                }

                logInfo("Entry screen obtained for app: " + appId);
                return entryScreen;

            } catch (Exception e) {
                logError("Error getting entry screen for " + appId + ": " + e.getMessage());
                existingInfo.incrementCrashCount();
                return null;
            }
        }

        // 新規インスタンスを作成
        logInfo("Creating new instance for: " + appId);

        try {
            AppLoader appLoader = kernel.getAppLoader();
            IApplication app = appLoader.findApplicationById(appId);

            if (app == null) {
                logError("App not found: " + appId);
                return null;
            }

            // 重要: getEntryScreen()を呼ぶ前にProcessInfoをマップに登録
            // これにより、アプリがgetEntryScreen()内でregisterBackgroundService()を呼べる
            ProcessInfo newInfo = new ProcessInfo(appId, app);
            newInfo.setForeground(true);
            newInfo.incrementLaunchCount();
            processes.put(appId, newInfo);
            logDebug("ProcessInfo registered for " + appId + " with IApplication before getEntryScreen()");

            // getEntryScreen()を呼び出し（アプリはここでregisterBackgroundServiceを呼べる）
            Screen screen = app.getEntryScreen(kernel);
            if (screen == null) {
                logError("Failed to create screen for " + appId);
                processes.remove(appId); // 失敗したので削除
                return null;
            }

            // アプリケーションIDを設定
            screen.setApplicationId(appId);

            // ProcessInfoにフォアグラウンドスクリーンを設定
            newInfo.setForegroundScreen(screen);

            logInfo("Created new instance for " + appId + ", total processes: " + processes.size());
            return screen;

        } catch (Exception e) {
            logError("Failed to launch app " + appId + ": " + e.getMessage());
            e.printStackTrace();
            processes.remove(appId); // 失敗したので削除
            return null;
        }
    }

    // ==================== バックグラウンドサービス管理 ====================

    /**
     * アプリをバックグラウンドサービスとして登録する。
     *
     * @param appId アプリID
     */
    public void registerBackgroundService(String appId) {
        ProcessInfo info = processes.get(appId);
        if (info != null) {
            info.setBackgroundService(true);
            config.addAutostartApp(appId);
            logInfo("Registered background service: " + appId);
        } else {
            logError("Cannot register background service - app not found: " + appId + " (processes: " + processes.keySet() + ")");
        }
    }

    /**
     * アプリをバックグラウンドサービスから解除する。
     *
     * @param appId アプリID
     */
    public void unregisterBackgroundService(String appId) {
        ProcessInfo info = processes.get(appId);
        if (info != null) {
            info.setBackgroundService(false);
            config.removeAutostartApp(appId);
            logInfo("Unregistered background service: " + appId);
        }
    }

    /**
     * バックグラウンドサービスとして初期化する（自動起動時）。
     * アプリがgetBackgroundService()を実装している場合はそれを使用し、
     * 実装していない場合はgetEntryScreen()にフォールバックする（後方互換性）。
     *
     * @param appId アプリID
     */
    private void initializeBackgroundService(String appId) {
        logInfo("Initializing background service: " + appId);

        try {
            AppLoader appLoader = kernel.getAppLoader();
            IApplication app = appLoader.findApplicationById(appId);

            if (app == null) {
                logError("Background service app not found: " + appId);
                return;
            }

            Screen backgroundScreen;

            // 新しいAPIを使用：getBackgroundService()が実装されていればそれを使用
            if (app.hasBackgroundService()) {
                logDebug("App " + appId + " has background service, calling getBackgroundService()");
                backgroundScreen = app.getBackgroundService(kernel);
                if (backgroundScreen != null) {
                    logInfo("Using dedicated background service screen for: " + appId);
                } else {
                    // hasBackgroundService()がtrueなのにgetBackgroundService()がnullの場合は警告
                    logWarn("hasBackgroundService() returned true but getBackgroundService() returned null for: " + appId);
                    // フォールバック
                    backgroundScreen = app.getEntryScreen(kernel);
                }
            } else {
                // 後方互換性：getBackgroundService()が未実装の場合はgetEntryScreen()を使用
                backgroundScreen = app.getEntryScreen(kernel);
                logInfo("Using entry screen as background service (legacy mode) for: " + appId);
            }

            if (backgroundScreen == null) {
                logError("Failed to create screen for background service: " + appId);
                return;
            }

            // アプリケーションIDを設定
            backgroundScreen.setApplicationId(appId);

            // ProcessInfoを作成して登録（IApplicationインスタンスを保持）
            ProcessInfo info = new ProcessInfo(appId, app);
            info.setBackgroundServiceScreen(backgroundScreen);
            info.setBackgroundService(true);
            info.setForeground(false);
            info.setPriority(ProcessInfo.Priority.BACKGROUND);
            processes.put(appId, info);

            // backgroundInit()を呼び出す
            backgroundScreen.backgroundInit();

            logInfo("Background service initialized: " + appId);

        } catch (Exception e) {
            logError("Failed to initialize background service " + appId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== フォアグラウンド/バックグラウンド管理 ====================

    /**
     * アプリのフォアグラウンド状態を設定する。
     * フォアグラウンドスクリーンに対してライフサイクルイベントを通知する。
     *
     * @param appId アプリID
     * @param isForeground フォアグラウンドの場合true
     */
    public void setForeground(String appId, boolean isForeground) {
        ProcessInfo info = processes.get(appId);
        if (info != null) {
            boolean wasForeground = info.isForeground();
            info.setForeground(isForeground);

            // フォアグラウンドスクリーンに対してライフサイクルイベント通知
            Screen foregroundScreen = info.getForegroundScreen();
            if (foregroundScreen != null) {
                try {
                    if (isForeground && !wasForeground) {
                        foregroundScreen.onForeground();
                    } else if (!isForeground && wasForeground) {
                        foregroundScreen.onBackground();
                    }
                } catch (Exception e) {
                    logError("Error calling lifecycle event for " + appId + ": " + e.getMessage());
                    info.incrementCrashCount();
                }
            }

            logDebug("Set foreground for " + appId + ": " + isForeground);
        }
    }

    // ==================== 優先度管理 ====================

    /**
     * アプリの優先度を設定する。
     *
     * @param appId アプリID
     * @param priority 優先度
     */
    public void setPriority(String appId, ProcessInfo.Priority priority) {
        ProcessInfo info = processes.get(appId);
        if (info != null) {
            info.setPriority(priority);
            logDebug("Set priority for " + appId + ": " + priority);
        }
    }

    // ==================== 統計情報 ====================

    /**
     * プロセス情報を取得する。
     *
     * @param appId アプリID
     * @return ProcessInfo、存在しない場合はnull
     */
    public ProcessInfo getProcessInfo(String appId) {
        return processes.get(appId);
    }

    /**
     * 全てのプロセス情報を取得する。
     *
     * @return ProcessInfoのコレクション
     */
    public Collection<ProcessInfo> getAllProcessInfo() {
        return new ArrayList<>(processes.values());
    }

    // ==================== tick処理 ====================

    /**
     * フォアグラウンドアプリのtick処理を実行する。
     * フォアグラウンドスクリーン（foregroundScreen）に対してtick()を呼び出す。
     * 優先度に応じてtick頻度を調整する。
     */
    public void tick() {
        frameCount++;

        for (ProcessInfo info : processes.values()) {
            // フォアグラウンドスクリーンがない場合はスキップ
            Screen foregroundScreen = info.getForegroundScreen();
            if (foregroundScreen == null) {
                continue;
            }

            // フォアグラウンドでない場合はスキップ（バックグラウンドに移行したアプリ）
            if (!info.isForeground()) {
                continue;
            }

            // 優先度に応じてtick実行
            if (!info.shouldTick(frameCount)) {
                continue;
            }

            try {
                long startTime = System.nanoTime();
                foregroundScreen.tick();
                long endTime = System.nanoTime();
                info.addTickTime(endTime - startTime);
            } catch (Exception e) {
                logError("Error during tick for " + info.getAppId() + ": " + e.getMessage());
                e.printStackTrace();
                info.incrementCrashCount();
            }
        }
    }

    /**
     * バックグラウンドサービスのbackground()処理を実行する。
     * バックグラウンドサービススクリーン（backgroundServiceScreen）に対してbackground()を呼び出す。
     * フォアグラウンド/バックグラウンド状態に関係なく、常にバックグラウンドサービスを実行する。
     * 優先度に応じてtick頻度を調整する。
     */
    public void tickBackground() {
        for (ProcessInfo info : processes.values()) {
            // バックグラウンドサービスでない場合はスキップ
            if (!info.isBackgroundService()) {
                // デバッグ: なぜスキップされたか
                if (frameCount % 600 == 0) { // 10秒に1回だけログ出力
                    logDebug("tickBackground skip (not background service): " + info.getAppId());
                }
                continue;
            }

            // バックグラウンドサービススクリーンがない場合はスキップ
            Screen backgroundServiceScreen = info.getBackgroundServiceScreen();
            if (backgroundServiceScreen == null) {
                // デバッグ: なぜスキップされたか
                if (frameCount % 600 == 0) { // 10秒に1回だけログ出力
                    logDebug("tickBackground skip (backgroundServiceScreen is null): " + info.getAppId());
                }
                continue;
            }

            // 優先度に応じてtick実行
            if (!info.shouldTick(frameCount)) {
                continue;
            }

            try {
                long startTime = System.nanoTime();
                backgroundServiceScreen.background();
                long endTime = System.nanoTime();
                info.addTickTime(endTime - startTime);
            } catch (Exception e) {
                logError("Error during background for " + info.getAppId() + ": " + e.getMessage());
                e.printStackTrace();
                info.incrementCrashCount();
            }
        }
    }

    // ==================== ユーティリティ ====================

    /**
     * 現在のフレームカウントを取得する。
     *
     * @return フレームカウント
     */
    public long getFrameCount() {
        return frameCount;
    }

    /**
     * アプリが起動中かどうかを確認する。
     *
     * @param appId アプリID
     * @return 起動中の場合true
     */
    public boolean isAppRunning(String appId) {
        return processes.containsKey(appId);
    }

    /**
     * ServiceConfigを取得する。
     *
     * @return ServiceConfig
     */
    public ServiceConfig getConfig() {
        return config;
    }
}
