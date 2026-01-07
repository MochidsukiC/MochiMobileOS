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

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

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
    private static final int COLOR_DIVIDER = 0xFFE5E5EA;

    private static final int HEADER_HEIGHT = 60;
    private static final int ITEM_HEIGHT = 100;
    private static final int MARGIN = 16;
    private static final int BUTTON_WIDTH = 90;
    private static final int BUTTON_HEIGHT = 32;

    /** ダウンロード中のアプリID */
    private final List<String> downloadingAppIds = new ArrayList<>();

    public AppStoreScreen(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void setup(PGraphics g) {
        fetchRepository();
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
        g.text(pkg.name.substring(0, 1).toUpperCase(), iconX + iconSize/2, iconY + iconSize/2);

        // テキスト情報
        g.textAlign(PConstants.LEFT, PConstants.TOP);
        g.fill(COLOR_TEXT);
        g.textSize(16);
        g.text(pkg.name, iconX + iconSize + 12, y + 15);
        
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
        boolean isInstalled = kernel.getAppLoader().findApplicationById(pkg.id) != null;
        if (!isInstalled) {
            // IDが完全一致しない場合でも、小文字一致や前方一致でチェック
            String targetId = pkg.id.toLowerCase();
            List<jp.moyashi.phoneos.core.app.IApplication> loadedApps = kernel.getAppLoader().getLoadedApps();
            
            // デバッグログ: 比較対象の全IDを出力（最初の1回だけ、または特定のタイミングで）
            // System.out.println("AppStoreScreen: Checking installation for " + pkg.id);
            
            for (jp.moyashi.phoneos.core.app.IApplication app : loadedApps) {
                String appId = app.getApplicationId().toLowerCase();
                // System.out.println("  - Loaded App ID: " + appId + " (" + app.getName() + ")");
                
                if (appId.equals(targetId) || appId.startsWith(targetId + ".") || appId.contains(targetId)) {
                    isInstalled = true;
                    // System.out.println("    -> MATCH FOUND!");
                    break;
                }
            }
        }
        
        boolean isDownloading = downloadingAppIds.contains(pkg.id);

        if (isDownloading) {
            g.fill(COLOR_BUTTON_DOWNLOADING);
            g.rect(btnX, btnY, BUTTON_WIDTH, BUTTON_HEIGHT, 16);
            g.fill(COLOR_WHITE);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.text("Wait...", btnX + BUTTON_WIDTH / 2, btnY + BUTTON_HEIGHT / 2);
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
                    
                    boolean isInstalled = kernel.getAppLoader().findApplicationById(pkg.id) != null;
                    if (!isInstalled && !downloadingAppIds.contains(pkg.id)) {
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
        // Modアプリのインストール処理
        if (pkg.download_url != null && pkg.download_url.startsWith("local:mod:")) {
            String appId = pkg.download_url.substring("local:mod:".length());
            statusMessage = "Installing Mod: " + pkg.name + "...";
            
            try {
                boolean success = kernel.getAppLoader().installModApp(appId, kernel);
                if (success) {
                    statusMessage = pkg.name + " installed!";
                    // UI更新のために少し待ってからメッセージを消す
                    CompletableFuture.runAsync(() -> {
                        try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                        if (statusMessage != null && statusMessage.contains("installed")) {
                            statusMessage = null;
                        }
                    });
                    
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
            
            // VFSに保存
            String fileName = pkg.id + ".jar";
            String vfsPath = "apps/" + fileName;
            
            // AppLoaderのrefreshを実行するために一度書き込む
            kernel.getVFS().writeBinaryFile(vfsPath, data);
            
            // AppLoaderをリフレッシュ
            kernel.getAppLoader().refreshApps();
            
            statusMessage = pkg.name + " installed!";
            downloadingAppIds.remove(pkg.id);
            
            // 3秒後にステータスメッセージを消す
            CompletableFuture.runAsync(() -> {
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
                if (statusMessage != null && statusMessage.contains("installed")) {
                    statusMessage = null;
                }
            });

            // 通知を表示
            if (kernel.getNotificationManager() != null) {
                kernel.getNotificationManager().addNotification(
                    "App Store",
                    "Installed",
                    pkg.name + " has been installed successfully.",
                    1
                );
            }
            
        } catch (Exception e) {
            errorMessage = "Installation failed: " + e.getMessage();
            downloadingAppIds.remove(pkg.id);
            statusMessage = null;
        }
    }

    @Override
    public String getScreenTitle() {
        return "App Store";
    }
}