package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.apps.launcher.model.HomePage;
import jp.moyashi.phoneos.core.apps.launcher.model.Shortcut;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * MochiMobileOS用のレイアウト管理サービス。
 * ホーム画面のレイアウト情報をJSON形式で永続化し、起動時に復元する機能を提供する。
 * 
 * このサービスはVFSとAppLoaderと連携し、アプリケーションショートカットの
 * 配置情報を保存・読み込みする。レイアウトデータは/system/launcher_layout.json
 * に保存される。
 * 
 * @author YourName
 * @version 1.0
 * @since 2.0
 */
public class LayoutManager {
    
    /** レイアウトファイルの保存パス */
    private static final String LAYOUT_FILE_PATH = "/system/launcher_layout.json";
    
    /** VFSサービスへの参照 */
    private final VFS vfs;
    
    /** アプリローダーサービスへの参照 */
    private final AppLoader appLoader;
    
    /** JSON変換用のGsonインスタンス */
    private final Gson gson;
    
    /**
     * 新しいLayoutManagerインスタンスを構築する。
     * 
     * @param vfs ファイルシステムサービス
     * @param appLoader アプリケーションローダーサービス
     */
    public LayoutManager(VFS vfs, AppLoader appLoader) {
        this.vfs = vfs;
        this.appLoader = appLoader;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
        
        System.out.println("LayoutManager: レイアウト管理サービスを初期化完了");
    }
    
    /**
     * ホーム画面のレイアウトを保存する。
     * HomePage と Shortcut の情報をJSON形式に変換し、VFSに保存する。
     *
     * @param pages 保存するホームページのリスト
     * @return 保存に成功した場合true、失敗した場合false
     */
    public boolean saveLayout(List<HomePage> pages) {
        return saveLayout(pages, null);
    }

    /**
     * ホーム画面のレイアウトと常時表示フィールドを保存する。
     * HomePage と Shortcut、グローバルDockの情報をJSON形式に変換し、VFSに保存する。
     *
     * @param pages 保存するホームページのリスト
     * @param globalDockShortcuts グローバル常時表示フィールドのショートカットリスト
     * @return 保存に成功した場合true、失敗した場合false
     */
    public boolean saveLayout(List<HomePage> pages, List<Shortcut> globalDockShortcuts) {
        try {
            LayoutData layoutData = new LayoutData();
            layoutData.pages = new ArrayList<>();

            for (int i = 0; i < pages.size(); i++) {
                HomePage page = pages.get(i);
                PageLayoutData pageData = new PageLayoutData();
                pageData.pageIndex = i;
                pageData.pageName = page.getPageName();
                pageData.pageType = page.getPageType();
                pageData.shortcuts = new ArrayList<>();

                // 各ショートカットの情報を保存
                for (Shortcut shortcut : page.getShortcuts()) {
                    ShortcutLayoutData shortcutData = new ShortcutLayoutData();
                    shortcutData.applicationId = shortcut.getApplication().getApplicationId();
                    shortcutData.gridX = shortcut.getGridX();
                    shortcutData.gridY = shortcut.getGridY();
                    shortcutData.customName = shortcut.getCustomName();

                    pageData.shortcuts.add(shortcutData);
                }

                layoutData.pages.add(pageData);
            }

            // グローバル常時表示フィールドの情報を保存
            layoutData.dockShortcuts = new ArrayList<>();
            if (globalDockShortcuts != null) {
                for (Shortcut shortcut : globalDockShortcuts) {
                    DockShortcutLayoutData dockData = new DockShortcutLayoutData();
                    dockData.applicationId = shortcut.getApplication().getApplicationId();
                    dockData.dockPosition = shortcut.getDockPosition();
                    dockData.customName = shortcut.getCustomName();

                    layoutData.dockShortcuts.add(dockData);
                }
            }

            // JSONに変換して保存
            String jsonData = gson.toJson(layoutData);
            boolean success = vfs.writeFile(LAYOUT_FILE_PATH, jsonData);

            if (success) {
                int totalShortcuts = layoutData.pages.stream().mapToInt(p -> p.shortcuts.size()).sum();
                System.out.println("LayoutManager: レイアウトを保存しました (" +
                                 pages.size() + "ページ, " +
                                 totalShortcuts + "ショートカット, " +
                                 (globalDockShortcuts != null ? globalDockShortcuts.size() : 0) + "Dockアイテム)");
            }

            return success;

        } catch (Exception e) {
            System.err.println("LayoutManager: レイアウト保存エラー: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 保存されたホーム画面のレイアウトを読み込む。
     * VFSからJSONデータを読み込み、AppLoaderを使用してアプリケーションを復元し、
     * HomePage と Shortcut のリストを再構築する。
     *
     * @return 復元されたホームページのリスト、読み込みに失敗した場合やファイルが存在しない場合はnull
     */
    public List<HomePage> loadLayout() {
        LayoutLoadResult result = loadLayoutWithDock();
        return result != null ? result.pages : null;
    }

    /**
     * 保存されたホーム画面のレイアウトと常時表示フィールドを読み込む。
     * VFSからJSONデータを読み込み、AppLoaderを使用してアプリケーションを復元し、
     * HomePage と Shortcut、グローバルDockのリストを再構築する。
     *
     * @return 復元されたレイアウト情報、読み込みに失敗した場合やファイルが存在しない場合はnull
     */
    public LayoutLoadResult loadLayoutWithDock() {
        try {
            if (!vfs.fileExists(LAYOUT_FILE_PATH)) {
                System.out.println("LayoutManager: レイアウトファイルが存在しません、デフォルトレイアウトを使用");
                return null;
            }

            String jsonData = vfs.readFile(LAYOUT_FILE_PATH);
            if (jsonData == null || jsonData.trim().isEmpty()) {
                System.out.println("LayoutManager: レイアウトファイルが空です");
                return null;
            }

            // まず新しい形式で読み込みを試行
            try {
                LayoutData layoutData = gson.fromJson(jsonData, LayoutData.class);
                if (layoutData != null && layoutData.pages != null) {
                    return loadFromNewFormat(layoutData);
                }
            } catch (Exception e) {
                System.out.println("LayoutManager: 新しい形式での読み込みに失敗、旧形式を試行します");
            }

            // 旧形式での読み込みを試行
            Type listType = new TypeToken<List<PageLayoutData>>(){}.getType();
            List<PageLayoutData> oldLayoutData = gson.fromJson(jsonData, listType);

            if (oldLayoutData == null || oldLayoutData.isEmpty()) {
                System.out.println("LayoutManager: 無効なレイアウトデータ");
                return null;
            }

            return loadFromOldFormat(oldLayoutData);

        } catch (Exception e) {
            System.err.println("LayoutManager: レイアウト読み込みエラー: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private LayoutLoadResult loadFromNewFormat(LayoutData layoutData) {
        List<HomePage> pages = new ArrayList<>();
        List<Shortcut> globalDockShortcuts = new ArrayList<>();

        // 各ページを復元
        for (PageLayoutData pageData : layoutData.pages) {
            HomePage.PageType pageType = (pageData.pageType != null) ?
                pageData.pageType : HomePage.PageType.NORMAL;
            HomePage page = new HomePage(pageType, pageData.pageName);

            // 各ショートカットを復元
            for (ShortcutLayoutData shortcutData : pageData.shortcuts) {
                IApplication app = appLoader.findApplicationById(shortcutData.applicationId);

                if (app != null) {
                    Shortcut shortcut = new Shortcut(app, shortcutData.gridX, shortcutData.gridY);
                    if (shortcutData.customName != null) {
                        shortcut.setCustomName(shortcutData.customName);
                    }

                    page.addShortcut(shortcut, shortcutData.gridX, shortcutData.gridY);
                } else {
                    System.out.println("LayoutManager: アプリケーションが見つかりません: " +
                                     shortcutData.applicationId);
                }
            }

            // AppLibraryページの場合は全アプリケーションを設定
            if (page.isAppLibraryPage()) {
                setupAppLibraryPage(page);
            }

            pages.add(page);
        }

        // グローバル常時表示フィールドを復元
        if (layoutData.dockShortcuts != null) {
            for (DockShortcutLayoutData dockData : layoutData.dockShortcuts) {
                IApplication app = appLoader.findApplicationById(dockData.applicationId);

                if (app != null) {
                    Shortcut shortcut = new Shortcut(app);
                    shortcut.setDockPosition(dockData.dockPosition);
                    if (dockData.customName != null) {
                        shortcut.setCustomName(dockData.customName);
                    }

                    globalDockShortcuts.add(shortcut);
                } else {
                    System.out.println("LayoutManager: Dockアプリケーションが見つかりません: " +
                                     dockData.applicationId);
                }
            }
        }

        int totalShortcuts = pages.stream().mapToInt(p -> p.getShortcutCount()).sum();
        System.out.println("LayoutManager: レイアウトを復元しました (" +
                         pages.size() + "ページ, " +
                         totalShortcuts + "ショートカット, " +
                         globalDockShortcuts.size() + "Dockアイテム)");

        return new LayoutLoadResult(pages, globalDockShortcuts);
    }

    private LayoutLoadResult loadFromOldFormat(List<PageLayoutData> layoutData) {
        List<HomePage> pages = new ArrayList<>();

        // 各ページを復元
        for (PageLayoutData pageData : layoutData) {
            HomePage.PageType pageType = (pageData.pageType != null) ?
                pageData.pageType : HomePage.PageType.NORMAL;
            HomePage page = new HomePage(pageType, pageData.pageName);

            // 各ショートカットを復元
            for (ShortcutLayoutData shortcutData : pageData.shortcuts) {
                IApplication app = appLoader.findApplicationById(shortcutData.applicationId);

                if (app != null) {
                    Shortcut shortcut = new Shortcut(app, shortcutData.gridX, shortcutData.gridY);
                    if (shortcutData.customName != null) {
                        shortcut.setCustomName(shortcutData.customName);
                    }

                    page.addShortcut(shortcut, shortcutData.gridX, shortcutData.gridY);
                } else {
                    System.out.println("LayoutManager: アプリケーションが見つかりません: " +
                                     shortcutData.applicationId);
                }
            }

            // AppLibraryページの場合は全アプリケーションを設定
            if (page.isAppLibraryPage()) {
                setupAppLibraryPage(page);
            }

            pages.add(page);
        }

        System.out.println("LayoutManager: 旧形式レイアウトを復元しました (" +
                         pages.size() + "ページ, " +
                         pages.stream().mapToInt(p -> p.getShortcutCount()).sum() + "ショートカット)");

        return new LayoutLoadResult(pages, new ArrayList<>());
    }

    private void setupAppLibraryPage(HomePage page) {
        List<IApplication> allApps = appLoader.getLoadedApps();
        if (allApps != null) {
            List<IApplication> availableApps = new ArrayList<>();
            for (IApplication app : allApps) {
                if (app != null && !"jp.moyashi.phoneos.core.apps.launcher".equals(app.getApplicationId())) {
                    availableApps.add(app);
                }
            }
            page.setAllApplications(availableApps);
            System.out.println("LayoutManager: AppLibraryページに " + availableApps.size() + " 個のアプリを設定");
        }
    }
    
    /**
     * 保存されたレイアウトファイルが存在するかどうかを確認する。
     * 
     * @return レイアウトファイルが存在する場合true、存在しない場合false
     */
    public boolean hasLayoutFile() {
        return vfs.fileExists(LAYOUT_FILE_PATH);
    }
    
    /**
     * 保存されたレイアウトファイルを削除する。
     * 
     * @return 削除に成功した場合true、失敗した場合false
     */
    public boolean deleteLayoutFile() {
        boolean success = vfs.deleteFile(LAYOUT_FILE_PATH);
        if (success) {
            System.out.println("LayoutManager: レイアウトファイルを削除しました");
        }
        return success;
    }
    
    /**
     * 全体のレイアウト情報を格納するデータクラス。
     * JSON変換用の内部クラス。
     */
    private static class LayoutData {
        /** ページのリスト */
        public List<PageLayoutData> pages;

        /** グローバル常時表示フィールドのショートカット */
        public List<DockShortcutLayoutData> dockShortcuts;
    }

    /**
     * ページレイアウト情報を格納するデータクラス。
     * JSON変換用の内部クラス。
     */
    private static class PageLayoutData {
        /** ページのインデックス */
        public int pageIndex;

        /** ページのカスタム名 */
        public String pageName;

        /** ページのタイプ */
        public HomePage.PageType pageType;

        /** ページ内のショートカット一覧 */
        public List<ShortcutLayoutData> shortcuts;
    }

    /**
     * ショートカットレイアウト情報を格納するデータクラス。
     * JSON変換用の内部クラス。
     */
    private static class ShortcutLayoutData {
        /** 参照するアプリケーションの一意ID */
        public String applicationId;

        /** グリッドX座標 */
        public int gridX;

        /** グリッドY座標 */
        public int gridY;

        /** カスタム表示名 */
        public String customName;
    }

    /**
     * 常時表示フィールドのショートカットレイアウト情報を格納するデータクラス。
     * JSON変換用の内部クラス。
     */
    private static class DockShortcutLayoutData {
        /** 参照するアプリケーションの一意ID */
        public String applicationId;

        /** 常時表示フィールド内の位置 */
        public int dockPosition;

        /** カスタム表示名 */
        public String customName;
    }

    /**
     * レイアウト読み込み結果を格納するクラス
     */
    public static class LayoutLoadResult {
        public final List<HomePage> pages;
        public final List<Shortcut> globalDockShortcuts;

        public LayoutLoadResult(List<HomePage> pages, List<Shortcut> globalDockShortcuts) {
            this.pages = pages;
            this.globalDockShortcuts = globalDockShortcuts;
        }
    }
}