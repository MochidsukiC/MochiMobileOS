package jp.moyashi.phoneos.core.apps.appstore.ui;

import com.google.gson.Gson;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.apps.appstore.model.AppStorePackage;
import jp.moyashi.phoneos.core.apps.appstore.model.AppStoreRepository;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.service.AppLoader;
import jp.moyashi.phoneos.core.service.network.NetworkAdapter;
import processing.core.PConstants;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import jp.moyashi.phoneos.core.service.LoggerContext;

/**
 * AppStoreのメイン画面。
 * 利用可能な外部リポジトリのアプリケーション一覧表示とインストール機能を提供する。
 *
 * @author jp.moyashi
 * @version 1.1
 * @since 1.0
 */
public class AppStoreScreen implements Screen {

    private final Kernel kernel;
    private final Gson gson = new Gson();

    /** リポジトリURL */
    private static final String REPO_URL = "https://mochidsukic.github.io/MochiMobileOS-AppStore/repository.json";
    private static final String REPO_BASE_URL = "https://mochidsukic.github.io/MochiMobileOS-AppStore/";

    /** 状態管理 */
    private AppStoreRepository repository;
    private boolean isLoading = false;
    private String errorMessage = null;
    private String statusMessage = null;

    /** スクロール管理 */
    private float scrollY = 0;
    private float maxScrollY = 0;
    private float touchStartY = 0;
    private float scrollStartY = 0;
    private boolean isDragging = false;

    /** UI定数 */
    private static final int COLOR_BG = 0xFFF5F5F5;
    private static final int COLOR_HEADER = 0xFF007AFF;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF1C1C1E;
    private static final int COLOR_TEXT_SUB = 0xFF8E8E93;
    private static final int COLOR_BUTTON_INSTALL = 0xFF34C759;
    private static final int COLOR_BUTTON_INSTALLED = 0xFF8E8E93;
    private static final int COLOR_BUTTON_DOWNLOADING = 0xFF007AFF;
    private static final int COLOR_BUTTON_UPDATE = 0xFFFF9500;
    private static final int COLOR_DIVIDER = 0xFFE5E5EA;

    private static final int HEADER_HEIGHT = 60;
    private static final int ITEM_HEIGHT = 100;
    private static final int MARGIN = 16;
    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 32;

    /** ダウンロード中のアプリID（スレッドセーフ） */
    private final List<String> downloadingAppIds = new CopyOnWriteArrayList<>();

    /** インストール済みアプリのバージョン追跡マップ（appId -> version） */
    private final Map<String, String> installedVersions = new ConcurrentHashMap<>();
    private static final String INSTALLED_VERSIONS_PATH = "system/appstore_installed_versions.json";

    /** 遅延タスク実行用スケジューラ */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "AppStoreScreen-Scheduler");
        t.setDaemon(true);
        return t;
    });

    public AppStoreScreen(Kernel kernel) {
        this.kernel = kernel;
        loadInstalledVersions();
    }

    @Override
    public void setup(PGraphics g) {
        fetchRepository();
    }

    @Override
    public void cleanup(PGraphics g) {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    /**
     * リポジトリ情報を取得する。
     */
    private void fetchRepository() {
        if (isLoading) return;
        
        isLoading = true;
        errorMessage = null;
        statusMessage = "Fetching repository...";

        try {
            kernel.getNetworkAdapter().request(REPO_URL, "GET")
                .thenAccept(response -> {
                    if (response.isSuccess()) {
                        repository = gson.fromJson(response.getBody(), AppStoreRepository.class);
                        
                        // バリデーション: 不正なIDを持つアプリを除外
                        if (repository.apps != null) {
                            repository.apps.removeIf(app -> {
                                if (app.id == null || !isValidAppId(app.id)) {
                                    System.err.println("AppStoreScreen: Invalid app ID ignored: " + app.id);
                                    return true;
                                }
                                return false;
                            });
                        }

                        // Modアプリを追加
                        List<jp.moyashi.phoneos.core.app.IApplication> modApps = new ArrayList<>();
                        modApps.addAll(kernel.getAppLoader().getAvailableModApps());
                        modApps.addAll(kernel.getAppLoader().getInstalledModApps());
                        
                        if (!modApps.isEmpty()) {
                            if (repository.apps == null) {
                                repository.apps = new ArrayList<>();
                            }
                            
                            // 既に追加済みのIDを管理して重複を防ぐ（念のため）
                            List<String> addedIds = new ArrayList<>();
                            
                            for (jp.moyashi.phoneos.core.app.IApplication modApp : modApps) {
                                String appId = modApp.getApplicationId();
                                if (addedIds.contains(appId)) continue;
                                
                                AppStorePackage pkg = new AppStorePackage();
                                pkg.id = appId;
                                pkg.name = modApp.getName();
                                pkg.version = modApp.getVersion();
                                pkg.author = "Mod System";
                                pkg.description = modApp.getDescription();
                                pkg.download_url = "local:mod:" + appId;
                                pkg.file_size = 0; // ローカルなのでサイズ不明/0
                                
                                // リストの先頭に追加
                                repository.apps.add(0, pkg);
                                addedIds.add(appId);
                            }
                        }

                        statusMessage = null;
                    } else {
                        errorMessage = "Failed to fetch repository: " + response.getStatusCode();
                    }
                    isLoading = false;
                })
                .exceptionally(e -> {
                    errorMessage = "Network error: " + e.getMessage();
                    isLoading = false;
                    return null;
                });
        } catch (Exception e) {
            errorMessage = "Internal error: " + e.getMessage();
            isLoading = false;
        }
    }

    /**
     * アプリIDの妥当性をチェックする（パストラバーサル対策）。
     * 英数字、ドット、ハイフン、アンダースコアのみ許可。
     */
    private boolean isValidAppId(String appId) {
        return appId != null && appId.matches("^[a-zA-Z0-9._-]+$") && !appId.contains("..");
    }

    @Override
    public void draw(PGraphics g) {
        g.background(COLOR_BG);
        drawHeader(g);

        if (isLoading && repository == null) {
            drawLoading(g);
        } else if (errorMessage != null && repository == null) {
            drawError(g);
        } else if (repository != null) {
            drawAppList(g);
        }

        // ステータスメッセージがあれば表示
        if (statusMessage != null) {
            drawStatusBar(g, statusMessage, COLOR_BUTTON_DOWNLOADING);
        }
    }

    private void drawHeader(PGraphics g) {
        g.noStroke();
        g.fill(COLOR_HEADER);
        g.rect(0, 0, g.width, HEADER_HEIGHT);

        g.fill(COLOR_WHITE);
        g.textAlign(PConstants.CENTER, PConstants.CENTER);
        g.textSize(20);
        g.text("MochiOS App Store", g.width / 2, HEADER_HEIGHT / 2);
        
        // リロードボタン
        g.textSize(12);
        g.textAlign(PConstants.RIGHT, PConstants.CENTER);
        g.text("Reload", g.width - MARGIN, HEADER_HEIGHT / 2);
    }

    private void drawLoading(PGraphics g) {
        g.fill(COLOR_TEXT_SUB);
        g.textAlign(PConstants.CENTER, PConstants.CENTER);
        g.textSize(16);
        g.text("Loading repository...", g.width / 2, g.height / 2);
    }

    private void drawError(PGraphics g) {
        g.fill(0xFFFF3B30);
        g.textAlign(PConstants.CENTER, PConstants.CENTER);
        g.textSize(16);
        g.text("Error", g.width / 2, g.height / 2 - 20);
        g.fill(COLOR_TEXT_SUB);
        g.textSize(12);
        g.text(errorMessage, g.width / 2, g.height / 2 + 10);
        
        // 再試行ボタン
        g.fill(COLOR_HEADER);
        g.rect(g.width / 2 - 50, g.height / 2 + 40, 100, 30, 8);
        g.fill(COLOR_WHITE);
        g.text("Retry", g.width / 2, g.height / 2 + 55);
    }

    private void drawAppList(PGraphics g) {
        List<AppStorePackage> apps = repository.apps;
        if (apps == null || apps.isEmpty()) {
            g.fill(COLOR_TEXT_SUB);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.text("No apps available", g.width / 2, g.height / 2);
            return;
        }

        int totalHeight = apps.size() * ITEM_HEIGHT;
        maxScrollY = Math.max(0, totalHeight - (g.height - HEADER_HEIGHT));

        g.clip(0, HEADER_HEIGHT, g.width, g.height - HEADER_HEIGHT);
        
        int y = HEADER_HEIGHT - (int) scrollY;
        for (int i = 0; i < apps.size(); i++) {
            if (y + ITEM_HEIGHT > HEADER_HEIGHT && y < g.height) {
                drawAppItem(g, apps.get(i), y);
            }
            y += ITEM_HEIGHT;
        }
        
        g.noClip();
    }

    private void drawAppItem(PGraphics g, AppStorePackage pkg, int y) {
        // 背景
        g.noStroke();
        g.fill(COLOR_WHITE);
        g.rect(0, y, g.width, ITEM_HEIGHT - 1);
        g.stroke(COLOR_DIVIDER);
        g.line(MARGIN, y + ITEM_HEIGHT - 1, g.width - MARGIN, y + ITEM_HEIGHT - 1);

        // アイコン
        int iconSize = 64;
        int iconX = MARGIN;
        int iconY = y + (ITEM_HEIGHT - iconSize) / 2;
        g.noStroke();
        g.fill(0xFFE0E0E0);
        g.rect(iconX, iconY, iconSize, iconSize, 12);
        
        g.fill(COLOR_TEXT_SUB);
        g.textAlign(PConstants.CENTER, PConstants.CENTER);
        g.textSize(24);
        // NPE防止: pkg.nameがnullまたは空の場合のフォールバック
        String displayName = (pkg.name != null && !pkg.name.isEmpty()) ? pkg.name : "Unknown";
        g.text(displayName.substring(0, 1).toUpperCase(), iconX + iconSize/2, iconY + iconSize/2);

        // テキスト情報
        g.textAlign(PConstants.LEFT, PConstants.TOP);
        g.fill(COLOR_TEXT);
        g.textSize(16);
        g.text(displayName, iconX + iconSize + 12, y + 15);
        
        g.fill(COLOR_TEXT_SUB);
        g.textSize(12);
        g.text("v" + pkg.version + " • " + pkg.author, iconX + iconSize + 12, y + 38);
        
        String desc = pkg.description;
        if (desc != null && desc.length() > 40) desc = desc.substring(0, 37) + "...";
        g.text(desc, iconX + iconSize + 12, y + 55);
        
        g.textSize(10);
        g.text(String.format("%.1f KB", pkg.file_size / 1024.0), iconX + iconSize + 12, y + 75);

        // インストールボタン
        int btnX = g.width - MARGIN - BUTTON_WIDTH;
        int btnY = y + (ITEM_HEIGHT - BUTTON_HEIGHT) / 2;

        // インストール済み判定の強化
        jp.moyashi.phoneos.core.app.IApplication installedApp = kernel.getAppLoader().findApplicationById(pkg.id);
        if (installedApp == null) {
            // IDが完全一致しない場合でも、小文字一致や前方一致でチェック
            String targetId = pkg.id.toLowerCase();
            List<jp.moyashi.phoneos.core.app.IApplication> loadedApps = kernel.getAppLoader().getLoadedApps();

            for (jp.moyashi.phoneos.core.app.IApplication app : loadedApps) {
                String appId = app.getApplicationId().toLowerCase();

                if (appId.equals(targetId) || appId.startsWith(targetId + ".") || appId.contains(targetId)) {
                    installedApp = app;
                    break;
                }
            }
        }

        boolean isInstalled = installedApp != null;
        boolean needsUpdate = false;

        // バージョン比較（Modアプリはアップデート対象外、追跡マップを使用）
        if (isInstalled && pkg.version != null
                && !(pkg.download_url != null && pkg.download_url.startsWith("local:mod:"))) {
            String trackedVersion = installedVersions.get(pkg.id);
            if (trackedVersion == null || !trackedVersion.equals(pkg.version)) {
                needsUpdate = true;
            }
        }

        boolean isDownloading = downloadingAppIds.contains(pkg.id);

        if (isDownloading) {
            g.fill(COLOR_BUTTON_DOWNLOADING);
            g.rect(btnX, btnY, BUTTON_WIDTH, BUTTON_HEIGHT, 16);
            g.fill(COLOR_WHITE);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.text("Wait...", btnX + BUTTON_WIDTH / 2, btnY + BUTTON_HEIGHT / 2);
        } else if (needsUpdate) {
            g.fill(COLOR_BUTTON_UPDATE);
            g.rect(btnX, btnY, BUTTON_WIDTH, BUTTON_HEIGHT, 16);
            g.fill(COLOR_WHITE);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.text("UPDATE", btnX + BUTTON_WIDTH / 2, btnY + BUTTON_HEIGHT / 2);
        } else if (isInstalled) {
            g.fill(COLOR_BUTTON_INSTALLED);
            g.rect(btnX, btnY, BUTTON_WIDTH, BUTTON_HEIGHT, 16);
            g.fill(COLOR_WHITE);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.text("Installed", btnX + BUTTON_WIDTH / 2, btnY + BUTTON_HEIGHT / 2);
        } else {
            g.fill(COLOR_BUTTON_INSTALL);
            g.rect(btnX, btnY, BUTTON_WIDTH, BUTTON_HEIGHT, 16);
            g.fill(COLOR_WHITE);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.text("GET", btnX + BUTTON_WIDTH / 2, btnY + BUTTON_HEIGHT / 2);
        }
    }

    private void drawStatusBar(PGraphics g, String message, int color) {
        g.noStroke();
        g.fill(color);
        g.rect(0, g.height - 30, g.width, 30);
        g.fill(255);
        g.textAlign(PConstants.CENTER, PConstants.CENTER);
        g.textSize(12);
        g.text(message, g.width / 2, g.height - 15);
    }

    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        touchStartY = mouseY;
        scrollStartY = scrollY;
        isDragging = false;

        if (mouseY < HEADER_HEIGHT) {
            if (mouseX > g.width - 80) {
                fetchRepository();
            }
            return;
        }

        if (errorMessage != null && repository == null) {
            if (mouseY > g.height / 2 + 40 && mouseY < g.height / 2 + 70 &&
                mouseX > g.width / 2 - 50 && mouseX < g.width / 2 + 50) {
                fetchRepository();
            }
            return;
        }

        if (repository != null) {
            List<AppStorePackage> apps = repository.apps;
            int y = HEADER_HEIGHT - (int) scrollY;
            for (AppStorePackage pkg : apps) {
                int btnX = g.width - MARGIN - BUTTON_WIDTH;
                int btnY = y + (ITEM_HEIGHT - BUTTON_HEIGHT) / 2;

                if (mouseX >= btnX && mouseX <= btnX + BUTTON_WIDTH &&
                    mouseY >= btnY && mouseY <= btnY + BUTTON_HEIGHT) {

                    jp.moyashi.phoneos.core.app.IApplication installedApp = kernel.getAppLoader().findApplicationById(pkg.id);
                    boolean isInstalled = installedApp != null;
                    boolean needsUpdate = false;

                    if (isInstalled && pkg.version != null
                            && !(pkg.download_url != null && pkg.download_url.startsWith("local:mod:"))) {
                        String trackedVersion = installedVersions.get(pkg.id);
                        if (trackedVersion == null || !trackedVersion.equals(pkg.version)) {
                            needsUpdate = true;
                        }
                    }

                    if ((!isInstalled || needsUpdate) && !downloadingAppIds.contains(pkg.id)) {
                        downloadAndInstallApp(pkg);
                    }
                    return;
                }
                y += ITEM_HEIGHT;
            }
        }
    }

    @Override
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        float delta = touchStartY - mouseY;
        if (Math.abs(delta) > 5) isDragging = true;
        
        if (isDragging) {
            scrollY = scrollStartY + delta;
            scrollY = Math.max(0, Math.min(scrollY, maxScrollY));
        }
    }

    /**
     * アプリをダウンロードしてインストールする。
     */
    private void downloadAndInstallApp(AppStorePackage pkg) {
        // IDバリデーション
        if (pkg.id == null || !isValidAppId(pkg.id)) {
            errorMessage = "Invalid App ID detected.";
            return;
        }

        // Modアプリのインストール処理
        if (pkg.download_url != null && pkg.download_url.startsWith("local:mod:")) {
            String appId = pkg.download_url.substring("local:mod:".length());
            statusMessage = "Installing Mod: " + pkg.name + "...";
            
            try {
                boolean success = kernel.getAppLoader().installModApp(appId, kernel);
                if (success) {
                    statusMessage = pkg.name + " installed!";
                    refreshHomeScreens();
                    // UI更新のために3秒後にメッセージを消す（非ブロッキング）
                    scheduler.schedule(() -> {
                        if (statusMessage != null && statusMessage.contains("installed")) {
                            statusMessage = null;
                        }
                    }, 3, TimeUnit.SECONDS);
                    
                    if (kernel.getNotificationManager() != null) {
                        kernel.getNotificationManager().addNotification(
                            "App Store",
                            "Mod Installed",
                            pkg.name + " has been enabled successfully.",
                            1
                        );
                    }
                } else {
                    errorMessage = "Failed to install Mod app.";
                }
            } catch (Exception e) {
                errorMessage = "Error installing Mod: " + e.getMessage();
            }
            return;
        }

        downloadingAppIds.add(pkg.id);
        statusMessage = "Downloading " + pkg.name + "...";
        
        String url = pkg.download_url;
        if (!url.startsWith("http")) {
            url = REPO_BASE_URL + url;
        }

        try {
            kernel.getNetworkAdapter().requestBytes(url)
                .thenAccept(response -> {
                    if (response.isSuccess()) {
                        byte[] data = response.getBody();
                        installJar(pkg, data);
                    } else {
                        errorMessage = "Download failed: " + response.getStatusCode();
                        downloadingAppIds.remove(pkg.id);
                        statusMessage = null;
                    }
                })
                .exceptionally(e -> {
                    errorMessage = "Download error: " + e.getMessage();
                    downloadingAppIds.remove(pkg.id);
                    statusMessage = null;
                    return null;
                });
        } catch (Exception e) {
            errorMessage = "Download error: " + e.getMessage();
            downloadingAppIds.remove(pkg.id);
            statusMessage = null;
        }
    }

    /**
     * JARデータをVFSに保存し、インストールを完了する。
     */
    private void installJar(AppStorePackage pkg, byte[] data) {
        try {
            statusMessage = "Installing " + pkg.name + "...";

            // アップデート時: 起動中のアプリを終了する
            boolean wasRunning = kernel.getServiceManager() != null
                    && kernel.getServiceManager().isAppRunning(pkg.id);
            if (wasRunning) {
                statusMessage = "Restarting " + pkg.name + "...";
                // ScreenManagerからアプリの画面を除去
                kernel.getScreenManager().removeScreensByAppId(pkg.id);
                // ServiceManagerからプロセスを終了（cleanup + onDestroy）
                kernel.getServiceManager().terminateApp(pkg.id);
            }

            // VFSに保存
            String fileName = pkg.id + ".jar";
            String vfsPath = "apps/" + fileName;
            kernel.getVFS().writeBinaryFile(vfsPath, data);

            // AppLoaderをリフレッシュ（新しいバージョンのIApplicationを読み込む）
            kernel.getAppLoader().refreshApps();

            // アップデート時: アプリを再起動する
            if (wasRunning) {
                jp.moyashi.phoneos.core.ui.Screen relaunchedScreen =
                        kernel.getServiceManager().launchApp(pkg.id);
                if (relaunchedScreen != null) {
                    kernel.getScreenManager().pushScreen(relaunchedScreen);
                }
            }

            // インストール済みバージョンを記録して永続化
            installedVersions.put(pkg.id, pkg.version);
            saveInstalledVersions();

            statusMessage = pkg.name + (wasRunning ? " updated!" : " installed!");
            downloadingAppIds.remove(pkg.id);
            refreshHomeScreens();

            // 3秒後にステータスメッセージを消す（非ブロッキング）
            scheduler.schedule(() -> {
                if (statusMessage != null && (statusMessage.contains("installed") || statusMessage.contains("updated"))) {
                    statusMessage = null;
                }
            }, 3, TimeUnit.SECONDS);

            // 通知を表示
            if (kernel.getNotificationManager() != null) {
                kernel.getNotificationManager().addNotification(
                    "App Store",
                    wasRunning ? "Updated" : "Installed",
                    pkg.name + (wasRunning ? " has been updated and restarted." : " has been installed successfully."),
                    1
                );
            }

        } catch (Exception e) {
            errorMessage = "Installation failed: " + e.getMessage();
            downloadingAppIds.remove(pkg.id);
            statusMessage = null;
        }
    }

    /**
     * インストール後にホーム画面のアプリ一覧を更新する。
     */
    private void refreshHomeScreens() {
        if (kernel == null || kernel.getScreenManager() == null) {
            return;
        }
        try {
            for (Screen screen : kernel.getScreenManager().getAllScreens()) {
                if (screen instanceof jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen) {
                    ((jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen) screen).refreshApps();
                }
            }
        } catch (Exception e) {
            // Ignore refresh errors to avoid blocking installation UX
        }
    }

    /**
     * VFSからインストール済みバージョン情報を読み込む。
     */
    private void loadInstalledVersions() {
        try {
            if (!kernel.getVFS().fileExists(INSTALLED_VERSIONS_PATH)) {
                return;
            }

            String json = kernel.getVFS().readFile(INSTALLED_VERSIONS_PATH);
            if (json == null || json.trim().isEmpty()) {
                return;
            }

            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1).trim();
                if (!json.isEmpty()) {
                    String[] entries = json.split(",");
                    for (String entry : entries) {
                        String[] keyValue = entry.split(":");
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim().replace("\"", "");
                            String value = keyValue[1].trim().replace("\"", "");
                            installedVersions.put(key, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LoggerContext.error("AppStoreScreen", "Error loading installed versions: " + e.getMessage());
        }
    }

    /**
     * インストール済みバージョン情報をVFSに保存する。
     */
    private void saveInstalledVersions() {
        try {
            if (!kernel.getVFS().directoryExists("system")) {
                kernel.getVFS().createDirectory("system");
            }

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            boolean first = true;
            for (Map.Entry<String, String> entry : installedVersions.entrySet()) {
                if (!first) {
                    json.append(",\n");
                }
                json.append("  \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
                first = false;
            }
            json.append("\n}");

            kernel.getVFS().writeFile(INSTALLED_VERSIONS_PATH, json.toString());
        } catch (Exception e) {
            LoggerContext.error("AppStoreScreen", "Error saving installed versions: " + e.getMessage());
        }
    }

    @Override
    public String getScreenTitle() {
        return "App Store";
    }
}
