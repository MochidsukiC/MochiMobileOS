package jp.moyashi.phoneos.forge.app;

import jp.moyashi.phoneos.api.IModApplication;
import jp.moyashi.phoneos.api.proxy.IPCChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.core.PGraphics;

import java.util.*;

/**
 * MODアプリケーションマネージャー。
 * MODアプリの読み込み、登録、実行を一元管理する。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class ModAppManager {

    private static final Logger LOGGER = LogManager.getLogger(ModAppManager.class);

    /** シングルトンインスタンス */
    private static ModAppManager instance;

    /** MODアプリローダー */
    private final ModAppLoader loader;

    /** ワールドID */
    private String worldId;

    /** IPCチャンネル */
    private ForgeIPCChannel ipcChannel;

    /** 実行中のアプリランナー */
    private final Map<String, ModAppRunner> runningApps = new HashMap<>();

    /** 現在アクティブなアプリID */
    private String activeAppId;

    /** 初期化済みフラグ */
    private boolean initialized = false;

    private ModAppManager() {
        this.loader = new ModAppLoader();
    }

    /**
     * シングルトンインスタンスを取得する。
     */
    public static synchronized ModAppManager getInstance() {
        if (instance == null) {
            instance = new ModAppManager();
        }
        return instance;
    }

    /**
     * マネージャーを初期化する。
     *
     * @param worldId ワールドID
     */
    public void initialize(String worldId) {
        if (initialized && this.worldId != null && this.worldId.equals(worldId)) {
            return;
        }

        LOGGER.info("[ModAppManager] Initializing for world: {}", worldId);

        // 既存のリソースをクリーンアップ
        shutdown();

        this.worldId = worldId;

        // IPCチャンネルを作成
        this.ipcChannel = new ForgeIPCChannel(worldId);

        // MODをスキャン
        int appCount = loader.scanMods();
        LOGGER.info("[ModAppManager] Found {} MMOS apps", appCount);

        // Serverにアプリを登録
        for (ModAppLoader.ModAppInfo info : loader.getDiscoveredApps()) {
            registerAppWithServer(info.instance);
        }

        initialized = true;
    }

    /**
     * ServerにMODアプリを登録する。
     */
    private void registerAppWithServer(IModApplication app) {
        if (ipcChannel == null || !ipcChannel.isConnected()) {
            if (!ipcChannel.initialize()) {
                LOGGER.warn("[ModAppManager] IPC channel not connected, cannot register app: {}", app.getName());
                return;
            }
        }

        String payload = String.format(
            "{\"appId\":\"%s\",\"name\":\"%s\",\"description\":\"%s\",\"version\":\"%s\",\"author\":\"%s\",\"iconPath\":%s}",
            escapeJson(app.getAppId()),
            escapeJson(app.getName()),
            escapeJson(app.getDescription()),
            escapeJson(app.getVersion()),
            escapeJson(app.getAuthor()),
            app.getIconPath() != null ? "\"" + escapeJson(app.getIconPath()) + "\"" : "null"
        );

        ipcChannel.send(IPCChannel.MSG_APP_REGISTER, payload);
        LOGGER.info("[ModAppManager] Registered app with server: {}", app.getName());
    }

    /**
     * アプリを起動する。
     *
     * @param appId アプリID
     * @return 起動成功した場合true
     */
    public boolean launchApp(String appId) {
        // 既に実行中の場合はフォアグラウンドに
        if (runningApps.containsKey(appId)) {
            ModAppRunner runner = runningApps.get(appId);
            if (runner.isPaused()) {
                runner.resume();
            }
            activeAppId = appId;
            return true;
        }

        // アプリを検索
        ModAppLoader.ModAppInfo info = loader.findApp(appId);
        if (info == null) {
            LOGGER.warn("[ModAppManager] App not found: {}", appId);
            return false;
        }

        // ランナーを作成して開始
        ModAppRunner runner = new ModAppRunner(info.instance, ipcChannel);
        runner.start();

        runningApps.put(appId, runner);
        activeAppId = appId;

        LOGGER.info("[ModAppManager] Launched app: {}", info.instance.getName());
        return true;
    }

    /**
     * アプリを終了する。
     *
     * @param appId アプリID
     */
    public void stopApp(String appId) {
        ModAppRunner runner = runningApps.remove(appId);
        if (runner != null) {
            runner.stop();
            LOGGER.info("[ModAppManager] Stopped app: {}", runner.getApplication().getName());
        }

        if (appId.equals(activeAppId)) {
            activeAppId = null;
        }
    }

    /**
     * アクティブなアプリの画面を描画する。
     *
     * @param g Processingグラフィックスコンテキスト
     */
    public void renderActiveApp(PGraphics g) {
        if (activeAppId == null) return;

        ModAppRunner runner = runningApps.get(activeAppId);
        if (runner != null && runner.isRunning()) {
            runner.render(g);
        }
    }

    /**
     * タッチ開始イベントをアクティブアプリに転送する。
     */
    public boolean onTouchStart(int x, int y) {
        if (activeAppId == null) return false;
        ModAppRunner runner = runningApps.get(activeAppId);
        return runner != null && runner.onTouchStart(x, y);
    }

    /**
     * タッチ移動イベントをアクティブアプリに転送する。
     */
    public boolean onTouchMove(int x, int y) {
        if (activeAppId == null) return false;
        ModAppRunner runner = runningApps.get(activeAppId);
        return runner != null && runner.onTouchMove(x, y);
    }

    /**
     * タッチ終了イベントをアクティブアプリに転送する。
     */
    public boolean onTouchEnd(int x, int y) {
        if (activeAppId == null) return false;
        ModAppRunner runner = runningApps.get(activeAppId);
        return runner != null && runner.onTouchEnd(x, y);
    }

    /**
     * スクロールイベントをアクティブアプリに転送する。
     */
    public boolean onScroll(int x, int y, float delta) {
        if (activeAppId == null) return false;
        ModAppRunner runner = runningApps.get(activeAppId);
        return runner != null && runner.onScroll(x, y, delta);
    }

    /**
     * キー入力イベントをアクティブアプリに転送する。
     */
    public boolean onKeyTyped(int keyCode, char keyChar) {
        if (activeAppId == null) return false;
        ModAppRunner runner = runningApps.get(activeAppId);
        return runner != null && runner.onKeyTyped(keyCode, keyChar);
    }

    /**
     * 戻るボタンをアクティブアプリに転送する。
     *
     * @return trueの場合、アプリがイベントを消費した
     */
    public boolean onBackPressed() {
        if (activeAppId == null) return false;
        ModAppRunner runner = runningApps.get(activeAppId);
        if (runner != null) {
            if (!runner.onBackPressed()) {
                // アプリがイベントを消費しなかった場合、アプリを終了
                stopApp(activeAppId);
                return true;
            }
            return true;
        }
        return false;
    }

    /**
     * 検出されたMODアプリ一覧を取得する。
     */
    public List<IModApplication> getDiscoveredApps() {
        List<IModApplication> apps = new ArrayList<>();
        for (ModAppLoader.ModAppInfo info : loader.getDiscoveredApps()) {
            apps.add(info.instance);
        }
        return apps;
    }

    /**
     * アクティブなアプリIDを取得する。
     */
    public String getActiveAppId() {
        return activeAppId;
    }

    /**
     * アプリが実行中かどうか。
     */
    public boolean isAppRunning(String appId) {
        ModAppRunner runner = runningApps.get(appId);
        return runner != null && runner.isRunning();
    }

    /**
     * マネージャーをシャットダウンする。
     */
    public void shutdown() {
        LOGGER.info("[ModAppManager] Shutting down");

        // 全アプリを停止
        for (ModAppRunner runner : runningApps.values()) {
            runner.stop();
        }
        runningApps.clear();
        activeAppId = null;

        // IPCチャンネルを閉じる
        if (ipcChannel != null) {
            ipcChannel.close();
            ipcChannel = null;
        }

        // ローダーをクリーンアップ
        loader.close();

        initialized = false;
        worldId = null;
    }

    /**
     * JSON文字列をエスケープする。
     */
    private String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
