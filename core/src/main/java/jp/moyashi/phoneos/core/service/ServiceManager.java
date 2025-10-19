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

    // ==================== 初期化とシャットダウン ====================

    /**
     * ServiceManagerを初期化する。
     * 自動起動リストのアプリをバックグラウンドサービスとして起動する。
     */
    public void initialize() {
        System.out.println("ServiceManager: Initializing...");

        // 自動起動リストを読み込む
        Set<String> autostartApps = config.getAutostartApps();
        System.out.println("ServiceManager: Autostart apps: " + autostartApps);

        // 自動起動アプリをバックグラウンドサービスとして初期化
        for (String appId : autostartApps) {
            try {
                initializeBackgroundService(appId);
            } catch (Exception e) {
                System.err.println("ServiceManager: Failed to initialize background service " + appId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("ServiceManager: Initialized with " + autostartApps.size() + " background services");
    }

    /**
     * ServiceManagerをシャットダウンする。
     * 全てのプロセスをクリーンアップする。
     */
    public void shutdown() {
        System.out.println("ServiceManager: Shutting down...");

        // 全てのプロセスをクリーンアップ
        for (ProcessInfo info : processes.values()) {
            try {
                Screen screen = info.getScreen();
                if (screen != null) {
                    screen.cleanup((PGraphics) null); // PGraphicsはnullでOK（cleanup内で使わない）
                }
            } catch (Exception e) {
                System.err.println("ServiceManager: Error during cleanup of " + info.getAppId() + ": " + e.getMessage());
            }
        }

        processes.clear();
        System.out.println("ServiceManager: Shutdown complete");
    }

    // ==================== アプリ起動 ====================

    /**
     * アプリを起動する。
     * 既にインスタンスが存在する場合は再利用し、新規の場合はAppLoaderから取得して作成する。
     *
     * @param appId アプリID
     * @return Screenインスタンス、起動失敗時はnull
     */
    public Screen launchApp(String appId) {
        System.out.println("ServiceManager: Launching app: " + appId);

        // 既存インスタンスがあればそれを返す
        ProcessInfo existingInfo = processes.get(appId);
        if (existingInfo != null) {
            System.out.println("ServiceManager: Reusing existing instance for " + appId);
            existingInfo.incrementLaunchCount();
            existingInfo.setForeground(true);

            // ライフサイクルイベント通知
            try {
                existingInfo.getScreen().onForeground();
            } catch (Exception e) {
                System.err.println("ServiceManager: Error calling onForeground for " + appId + ": " + e.getMessage());
                existingInfo.incrementCrashCount();
            }

            return existingInfo.getScreen();
        }

        // 新規インスタンスを作成
        try {
            AppLoader appLoader = kernel.getAppLoader();
            IApplication app = appLoader.findApplicationById(appId);

            if (app == null) {
                System.err.println("ServiceManager: App not found: " + appId);
                return null;
            }

            Screen screen = app.getEntryScreen(kernel);
            if (screen == null) {
                System.err.println("ServiceManager: Failed to create screen for " + appId);
                return null;
            }

            // ProcessInfoを作成して登録
            ProcessInfo info = new ProcessInfo(appId, screen);
            info.setForeground(true);
            info.incrementLaunchCount();
            processes.put(appId, info);

            System.out.println("ServiceManager: Created new instance for " + appId);
            return screen;

        } catch (Exception e) {
            System.err.println("ServiceManager: Failed to launch app " + appId + ": " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("ServiceManager: Registered background service: " + appId);
        } else {
            System.err.println("ServiceManager: Cannot register background service - app not found: " + appId);
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
            System.out.println("ServiceManager: Unregistered background service: " + appId);
        }
    }

    /**
     * バックグラウンドサービスとして初期化する（自動起動時）。
     *
     * @param appId アプリID
     */
    private void initializeBackgroundService(String appId) {
        System.out.println("ServiceManager: Initializing background service: " + appId);

        try {
            AppLoader appLoader = kernel.getAppLoader();
            IApplication app = appLoader.findApplicationById(appId);

            if (app == null) {
                System.err.println("ServiceManager: Background service app not found: " + appId);
                return;
            }

            Screen screen = app.getEntryScreen(kernel);
            if (screen == null) {
                System.err.println("ServiceManager: Failed to create screen for background service: " + appId);
                return;
            }

            // ProcessInfoを作成して登録
            ProcessInfo info = new ProcessInfo(appId, screen);
            info.setBackgroundService(true);
            info.setForeground(false);
            info.setPriority(ProcessInfo.Priority.BACKGROUND);
            processes.put(appId, info);

            // backgroundInit()を呼び出す
            screen.backgroundInit();

            System.out.println("ServiceManager: Background service initialized: " + appId);

        } catch (Exception e) {
            System.err.println("ServiceManager: Failed to initialize background service " + appId + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== フォアグラウンド/バックグラウンド管理 ====================

    /**
     * アプリのフォアグラウンド状態を設定する。
     *
     * @param appId アプリID
     * @param isForeground フォアグラウンドの場合true
     */
    public void setForeground(String appId, boolean isForeground) {
        ProcessInfo info = processes.get(appId);
        if (info != null) {
            boolean wasForeground = info.isForeground();
            info.setForeground(isForeground);

            // ライフサイクルイベント通知
            try {
                if (isForeground && !wasForeground) {
                    info.getScreen().onForeground();
                } else if (!isForeground && wasForeground) {
                    info.getScreen().onBackground();
                }
            } catch (Exception e) {
                System.err.println("ServiceManager: Error calling lifecycle event for " + appId + ": " + e.getMessage());
                info.incrementCrashCount();
            }

            System.out.println("ServiceManager: Set foreground for " + appId + ": " + isForeground);
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
            System.out.println("ServiceManager: Set priority for " + appId + ": " + priority);
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
     * フォアグラウンド・バックグラウンドアプリのtick処理を実行する。
     * 優先度に応じてtick頻度を調整する。
     */
    public void tick() {
        frameCount++;

        for (ProcessInfo info : processes.values()) {
            // バックグラウンドサービスはticBackground()で処理するのでスキップ
            if (info.isBackgroundService() && !info.isForeground()) {
                continue;
            }

            // 優先度に応じてtick実行
            if (!info.shouldTick(frameCount)) {
                continue;
            }

            try {
                long startTime = System.nanoTime();
                info.getScreen().tick();
                long endTime = System.nanoTime();
                info.addTickTime(endTime - startTime);
            } catch (Exception e) {
                System.err.println("ServiceManager: Error during tick for " + info.getAppId() + ": " + e.getMessage());
                e.printStackTrace();
                info.incrementCrashCount();
            }
        }
    }

    /**
     * バックグラウンドサービスのbackground()処理を実行する。
     * 優先度に応じてtick頻度を調整する。
     */
    public void tickBackground() {
        for (ProcessInfo info : processes.values()) {
            // バックグラウンドサービスのみ
            if (!info.isBackgroundService()) {
                continue;
            }

            // フォアグラウンドの場合はtick()で処理済みなのでスキップ
            if (info.isForeground()) {
                continue;
            }

            // 優先度に応じてtick実行
            if (!info.shouldTick(frameCount)) {
                continue;
            }

            try {
                long startTime = System.nanoTime();
                info.getScreen().background();
                long endTime = System.nanoTime();
                info.addTickTime(endTime - startTime);
            } catch (Exception e) {
                System.err.println("ServiceManager: Error during background for " + info.getAppId() + ": " + e.getMessage());
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
