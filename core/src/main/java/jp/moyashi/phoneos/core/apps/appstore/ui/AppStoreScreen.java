package jp.moyashi.phoneos.core.apps.appstore.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.service.AppLoader;
import processing.core.PConstants;
import processing.core.PGraphics;

import java.util.List;
import java.util.ArrayList;

/**
 * AppStoreのメイン画面。
 * 利用可能なMODアプリケーションの一覧表示とインストール機能を提供する。
 *
 * @author jp.moyashi
 * @version 1.0
 * @since 1.0
 */
public class AppStoreScreen implements Screen {

    /** OSカーネルへの参照 */
    private final Kernel kernel;

    /** スクロール位置 */
    private float scrollY = 0;

    /** 最大スクロール位置 */
    private float maxScrollY = 0;

    /** タッチ開始位置 */
    private float touchStartY = 0;

    /** スクロール開始位置 */
    private float scrollStartY = 0;

    /** ドラッグ中かどうか */
    private boolean isDragging = false;

    /** UI色定数 */
    private static final int COLOR_BG = 0xFFF5F5F5;
    private static final int COLOR_HEADER = 0xFF007AFF;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_TEXT = 0xFF1C1C1E;
    private static final int COLOR_TEXT_SUB = 0xFF8E8E93;
    private static final int COLOR_BUTTON_INSTALL = 0xFF34C759;
    private static final int COLOR_BUTTON_INSTALLED = 0xFF8E8E93;
    private static final int COLOR_DIVIDER = 0xFFE5E5EA;

    /** レイアウト定数 */
    private static final int HEADER_HEIGHT = 60;
    private static final int ITEM_HEIGHT = 80;
    private static final int MARGIN = 16;
    private static final int BUTTON_WIDTH = 80;
    private static final int BUTTON_HEIGHT = 32;

    /**
     * AppStoreScreenを作成する。
     *
     * @param kernel OSカーネルインスタンス
     */
    public AppStoreScreen(Kernel kernel) {
        this.kernel = kernel;
        System.out.println("AppStoreScreen: Created");
    }

    @Override
    public void setup(PGraphics g) {
        System.out.println("AppStoreScreen: Setup");
    }

    @Override
    public void onForeground() {
        System.out.println("AppStoreScreen: onForeground");
    }

    @Override
    public void onBackground() {
        System.out.println("AppStoreScreen: onBackground");
    }

    @Override
    public void cleanup(PGraphics g) {
        System.out.println("AppStoreScreen: Cleanup");
    }

    @Override
    public void draw(PGraphics g) {
        // 背景
        g.background(COLOR_BG);

        // ヘッダー描画
        drawHeader(g);

        // アプリリスト描画
        drawAppList(g);
    }

    /**
     * ヘッダーを描画する。
     */
    private void drawHeader(PGraphics g) {
        // ヘッダー背景
        g.noStroke();
        g.fill(COLOR_HEADER);
        g.rect(0, 0, g.width, HEADER_HEIGHT);

        // タイトル
        g.fill(COLOR_WHITE);
        g.textAlign(PConstants.CENTER, PConstants.CENTER);
        g.textSize(20);
        g.text("App Store", g.width / 2, HEADER_HEIGHT / 2);
    }

    /**
     * アプリリストを描画する。
     */
    private void drawAppList(PGraphics g) {
        AppLoader appLoader = kernel.getAppLoader();
        List<IApplication> availableApps = appLoader.getAvailableModApps();
        List<IApplication> installedApps = appLoader.getInstalledModApps();

        // 利用可能なアプリがない場合
        if (availableApps.isEmpty() && installedApps.isEmpty()) {
            g.fill(COLOR_TEXT_SUB);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.textSize(16);
            g.text("No MOD apps available", g.width / 2, g.height / 2);
            g.textSize(12);
            g.text("Install MODs that provide apps", g.width / 2, g.height / 2 + 25);
            return;
        }

        // 全アプリのリストを作成（利用可能 + インストール済み）
        List<AppInfo> allApps = new ArrayList<>();

        for (IApplication app : installedApps) {
            allApps.add(new AppInfo(app, true));
        }

        for (IApplication app : availableApps) {
            allApps.add(new AppInfo(app, false));
        }

        // 最大スクロール計算
        int totalHeight = allApps.size() * ITEM_HEIGHT;
        int visibleHeight = g.height - HEADER_HEIGHT;
        maxScrollY = Math.max(0, totalHeight - visibleHeight);

        // クリッピング設定
        g.clip(0, HEADER_HEIGHT, g.width, visibleHeight);

        // 各アプリを描画
        int y = HEADER_HEIGHT - (int) scrollY;
        for (int i = 0; i < allApps.size(); i++) {
            AppInfo appInfo = allApps.get(i);

            // 画面内にある場合のみ描画
            if (y + ITEM_HEIGHT > HEADER_HEIGHT && y < g.height) {
                drawAppItem(g, appInfo, y, i);
            }

            y += ITEM_HEIGHT;
        }

        g.noClip();
    }

    /**
     * アプリアイテムを描画する。
     */
    private void drawAppItem(PGraphics g, AppInfo appInfo, int y, int index) {
        IApplication app = appInfo.app;
        boolean isInstalled = appInfo.isInstalled;

        // 背景
        g.noStroke();
        g.fill(COLOR_WHITE);
        g.rect(0, y, g.width, ITEM_HEIGHT - 1);

        // 区切り線
        g.stroke(COLOR_DIVIDER);
        g.line(MARGIN, y + ITEM_HEIGHT - 1, g.width - MARGIN, y + ITEM_HEIGHT - 1);

        // アイコン（プレースホルダー）
        int iconSize = 50;
        int iconX = MARGIN;
        int iconY = y + (ITEM_HEIGHT - iconSize) / 2;
        g.noStroke();
        g.fill(0xFFE0E0E0);
        g.rect(iconX, iconY, iconSize, iconSize, 10);

        // アイコン文字
        g.fill(COLOR_TEXT_SUB);
        g.textAlign(PConstants.CENTER, PConstants.CENTER);
        g.textSize(20);
        String initial = app.getName().substring(0, 1).toUpperCase();
        g.text(initial, iconX + iconSize / 2, iconY + iconSize / 2);

        // アプリ名
        g.fill(COLOR_TEXT);
        g.textAlign(PConstants.LEFT, PConstants.TOP);
        g.textSize(16);
        g.text(app.getName(), iconX + iconSize + 12, y + 15);

        // バージョンと説明
        g.fill(COLOR_TEXT_SUB);
        g.textSize(12);
        g.text("v" + app.getVersion(), iconX + iconSize + 12, y + 35);

        // 説明（短縮）
        String desc = app.getDescription();
        if (desc.length() > 30) {
            desc = desc.substring(0, 27) + "...";
        }
        g.text(desc, iconX + iconSize + 12, y + 52);

        // インストールボタン
        int btnX = g.width - MARGIN - BUTTON_WIDTH;
        int btnY = y + (ITEM_HEIGHT - BUTTON_HEIGHT) / 2;

        if (isInstalled) {
            // インストール済み
            g.fill(COLOR_BUTTON_INSTALLED);
            g.rect(btnX, btnY, BUTTON_WIDTH, BUTTON_HEIGHT, 16);
            g.fill(COLOR_WHITE);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.textSize(12);
            g.text("Installed", btnX + BUTTON_WIDTH / 2, btnY + BUTTON_HEIGHT / 2);
        } else {
            // インストール可能
            g.fill(COLOR_BUTTON_INSTALL);
            g.rect(btnX, btnY, BUTTON_WIDTH, BUTTON_HEIGHT, 16);
            g.fill(COLOR_WHITE);
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.textSize(12);
            g.text("Install", btnX + BUTTON_WIDTH / 2, btnY + BUTTON_HEIGHT / 2);
        }
    }

    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        touchStartY = mouseY;
        scrollStartY = scrollY;
        isDragging = false;

        // ヘッダー外のタップのみ処理
        if (mouseY > HEADER_HEIGHT) {
            // インストールボタンのタップ判定
            AppLoader appLoader = kernel.getAppLoader();
            List<IApplication> availableApps = appLoader.getAvailableModApps();

            int y = HEADER_HEIGHT - (int) scrollY;
            for (int i = 0; i < availableApps.size(); i++) {
                int btnX = g.width - MARGIN - BUTTON_WIDTH;
                int btnY = y + (ITEM_HEIGHT - BUTTON_HEIGHT) / 2;

                if (mouseX >= btnX && mouseX <= btnX + BUTTON_WIDTH &&
                    mouseY >= btnY && mouseY <= btnY + BUTTON_HEIGHT) {

                    // インストール実行
                    IApplication app = availableApps.get(i);
                    installApp(app);
                    return;
                }

                y += ITEM_HEIGHT;
            }
        }
    }

    /**
     * マウスドラッグ処理。
     */
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        if (mouseY > HEADER_HEIGHT) {
            float delta = touchStartY - mouseY;
            if (Math.abs(delta) > 5) {
                isDragging = true;
            }

            if (isDragging) {
                scrollY = scrollStartY + delta;
                // スクロール範囲制限
                scrollY = Math.max(0, Math.min(scrollY, maxScrollY));
            }
        }
    }

    /**
     * アプリをインストールする。
     */
    private void installApp(IApplication app) {
        System.out.println("AppStoreScreen: Installing app: " + app.getName());

        AppLoader appLoader = kernel.getAppLoader();
        boolean success = appLoader.installModApp(app.getApplicationId(), kernel);

        if (success) {
            System.out.println("AppStoreScreen: Successfully installed: " + app.getName());

            // 通知を表示（NotificationManagerが利用可能な場合）
            if (kernel.getNotificationManager() != null) {
                kernel.getNotificationManager().addNotification(
                    "App Store",
                    "Installed",
                    app.getName() + " has been installed successfully.",
                    1
                );
            }
        } else {
            System.err.println("AppStoreScreen: Failed to install: " + app.getName());
        }
    }

    @Override
    public String getScreenTitle() {
        return "App Store";
    }

    /**
     * アプリ情報を保持する内部クラス。
     */
    private static class AppInfo {
        final IApplication app;
        final boolean isInstalled;

        AppInfo(IApplication app, boolean isInstalled) {
            this.app = app;
            this.isInstalled = isInstalled;
        }
    }
}
