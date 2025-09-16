package jp.moyashi.phoneos.core.apps.launcher.model;

import jp.moyashi.phoneos.core.app.IApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * ランチャーのホーム画面の単一ページを表す。
 * 各ページはショートカット位置のグリッドを含み、アプリショートカットの
 * 配置、整理、操作を管理する。
 * 
 * ページはグリッドベースのレイアウトシステムを使用し、各位置には
 * 最大1つのショートカットのみを含むことができ、空の位置は
 * 新しいショートカット配置に利用できる。
 * 
 * 特殊なページタイプ（AppLibraryなど）もサポートし、異なる表示方式と
 * インタラクション方式を提供する。
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class HomePage {
    
    /**
     * ページタイプを定義する列挙型
     */
    public enum PageType {
        /** 通常のホームページ（グリッドレイアウト） */
        NORMAL,
        /** AppLibraryページ（リストレイアウト、スクロール可能） */
        APP_LIBRARY
    }
    
    /** ホームページのグリッド寸法 */
    public static final int GRID_COLS = 4;
    public static final int GRID_ROWS = 5;
    public static final int MAX_SHORTCUTS = GRID_COLS * GRID_ROWS;
    
    /** このページ上のショートカット、2D配列として格納 */
    private final Shortcut[][] grid;
    
    /** 簡単な反復処理のためのこのページ上のすべてのショートカットのリスト */
    private final List<Shortcut> shortcuts;
    
    /** このページのカスタム名（オプション） */
    private String pageName;
    
    /** このページの一意識別子 */
    private final String pageId;
    
    /** ページタイプ */
    private final PageType pageType;
    
    /** AppLibraryページ用のスクロールオフセット */
    private int scrollOffset;
    
    /** AppLibraryページ用の全アプリケーションリスト */
    private List<IApplication> allApplications;

    /** 常時表示フィールド（Dock）用のショートカット（最大4個） */
    private final List<Shortcut> dockShortcuts;

    /** 常時表示フィールドの最大アイテム数 */
    public static final int MAX_DOCK_SHORTCUTS = 4;

    /** 一意のページID生成用静的カウンター */
    private static int nextPageId = 1;
    
    /**
     * 新しい空の通常ホームページを作成する。
     */
    public HomePage() {
        this(PageType.NORMAL, null);
    }
    
    /**
     * 指定されたページタイプで新しいホームページを作成する。
     * 
     * @param pageType ページタイプ
     * @param pageName ページ名
     */
    public HomePage(PageType pageType, String pageName) {
        this.pageType = pageType;
        this.pageName = pageName;
        this.pageId = (pageType == PageType.APP_LIBRARY) ? "app_library" : "page_" + (nextPageId++);
        this.scrollOffset = 0;
        this.allApplications = new ArrayList<>();
        this.dockShortcuts = new ArrayList<>();
        
        if (pageType == PageType.NORMAL) {
            this.grid = new Shortcut[GRID_COLS][GRID_ROWS];
            this.shortcuts = new ArrayList<>();
        } else {
            // AppLibraryページではグリッドは使用しない
            this.grid = null;
            this.shortcuts = new ArrayList<>();
        }
        
        System.out.println("HomePage: Created new home page with ID " + pageId);
    }
    
    /**
     * カスタム名で新しい通常ホームページを作成する。
     * 
     * @param pageName このページの名前
     */
    public HomePage(String pageName) {
        this(PageType.NORMAL, pageName);
    }
    
    /**
     * このページの一意識別子を取得する。
     * 
     * @return ページID
     */
    public String getPageId() {
        return pageId;
    }
    
    /**
     * このページのカスタム名を取得する。
     * 
     * @return ページ名、またはカスタム名が設定されていない場合null
     */
    public String getPageName() {
        return pageName;
    }
    
    /**
     * このページのカスタム名を設定する。
     * 
     * @param pageName 新しいページ名、またはクリアする場合null
     */
    public void setPageName(String pageName) {
        this.pageName = pageName;
    }
    
    /**
     * このページの表示名を取得する。
     * カスタム名が設定されている場合はそれを返し、そうでなければデフォルト名を返す。
     * 
     * @return このページに表示する名前
     */
    public String getDisplayName() {
        return pageName != null ? pageName : "Page " + pageId.substring(5);
    }
    
    /**
     * このページ上のすべてのショートカットのリストを取得する。
     * 
     * @return ショートカットリストの変更不可能ビュー
     */
    public List<Shortcut> getShortcuts() {
        return new ArrayList<>(shortcuts);
    }
    
    /**
     * 現在このページ上のショートカットの数を取得する。
     * 
     * @return ショートカットの数
     */
    public int getShortcutCount() {
        return shortcuts.size();
    }
    
    /**
     * このページが満员（空の位置がない）かどうかを確認する。
     * 
     * @return ページが最大容量の場合true、そうでなければfalse
     */
    public boolean isFull() {
        return shortcuts.size() >= MAX_SHORTCUTS;
    }
    
    /**
     * このページが空（ショートカットなし）かどうかを確認する。
     * 
     * @return ページにショートカットがない場合true、そうでなければfalse
     */
    public boolean isEmpty() {
        return shortcuts.isEmpty();
    }
    
    /**
     * 指定されたグリッド位置のショートカットを取得する。
     * 
     * @param gridX グリッド列（0ベース）
     * @param gridY グリッド行（0ベース）
     * @return その位置のショートカット、または空または無効な位置の場合null
     */
    public Shortcut getShortcutAt(int gridX, int gridY) {
        if (!isValidPosition(gridX, gridY)) {
            return null;
        }
        return grid[gridX][gridY];
    }
    
    /**
     * 指定されたグリッド位置が有効かどうかを確認する。
     * 
     * @param gridX グリッド列
     * @param gridY グリッド行
     * @return 位置が範囲内の場合true
     */
    public boolean isValidPosition(int gridX, int gridY) {
        return gridX >= 0 && gridX < GRID_COLS && gridY >= 0 && gridY < GRID_ROWS;
    }
    
    /**
     * 指定されたグリッド位置が空かどうかを確認する。
     * 
     * @param gridX グリッド列
     * @param gridY グリッド行
     * @return 位置が有効で空の場合true
     */
    public boolean isPositionEmpty(int gridX, int gridY) {
        return isValidPosition(gridX, gridY) && grid[gridX][gridY] == null;
    }
    
    /**
     * このページで次に利用可能な空の位置を検索する。
     * 左から右へ、上から下へ検索する。
     * 
     * @return 次の空の位置の配列[x, y]、またはページが満员の場合null
     */
    public int[] findNextEmptyPosition() {
        for (int y = 0; y < GRID_ROWS; y++) {
            for (int x = 0; x < GRID_COLS; x++) {
                if (grid[x][y] == null) {
                    return new int[]{x, y};
                }
            }
        }
        return null; // Page is full
    }
    
    /**
     * 指定された位置のこのページにショートカットを追加する。
     * 
     * @param shortcut 追加するショートカット
     * @param gridX ターゲットグリッド列
     * @param gridY ターゲットグリッド行
     * @return ショートカットが正常に追加された場合true、位置が占有されているか無効な場合false
     */
    public boolean addShortcut(Shortcut shortcut, int gridX, int gridY) {
        if (shortcut == null || !isPositionEmpty(gridX, gridY)) {
            return false;
        }
        
        // Remove from old position if it was already on this page
        removeShortcut(shortcut);
        
        // Add to new position
        grid[gridX][gridY] = shortcut;
        shortcut.setGridPosition(gridX, gridY);
        shortcuts.add(shortcut);
        
        System.out.println("HomePage: Added shortcut " + shortcut.getDisplayName() + 
                          " to position (" + gridX + ", " + gridY + ") on page " + pageId);
        return true;
    }
    
    /**
     * 次に利用可能な位置のこのページにショートカットを追加する。
     * 
     * @param shortcut 追加するショートカット
     * @return ショートカットが正常に追加された場合true、ページが満员の場合false
     */
    public boolean addShortcut(Shortcut shortcut) {
        int[] position = findNextEmptyPosition();
        if (position == null) {
            return false; // Page is full
        }
        return addShortcut(shortcut, position[0], position[1]);
    }
    
    /**
     * 指定されたアプリケーション用の新しいショートカットを作成し追加する。
     * 
     * @param application ショートカットを作成するアプリケーション
     * @return ショートカットが正常に作成・追加された場合true
     */
    public boolean addShortcut(IApplication application) {
        if (application == null) {
            return false;
        }
        Shortcut shortcut = new Shortcut(application);
        return addShortcut(shortcut);
    }
    
    /**
     * 指定された位置で指定されたアプリケーション用の新しいショートカットを作成し追加する。
     * 
     * @param application ショートカットを作成するアプリケーション
     * @param gridX ターゲットグリッド列
     * @param gridY ターゲットグリッド行
     * @return ショートカットが正常に作成・追加された場合true
     */
    public boolean addShortcut(IApplication application, int gridX, int gridY) {
        if (application == null) {
            return false;
        }
        Shortcut shortcut = new Shortcut(application, gridX, gridY);
        return addShortcut(shortcut, gridX, gridY);
    }
    
    /**
     * このページからショートカットを削除する。
     * 
     * @param shortcut 削除するショートカット
     * @return ショートカットが削除された場合true、このページになかった場合false
     */
    public boolean removeShortcut(Shortcut shortcut) {
        if (shortcut == null || !shortcuts.contains(shortcut)) {
            return false;
        }
        
        // Clear from grid
        int gridX = shortcut.getGridX();
        int gridY = shortcut.getGridY();
        if (isValidPosition(gridX, gridY) && grid[gridX][gridY] == shortcut) {
            grid[gridX][gridY] = null;
        }
        
        // Remove from list
        shortcuts.remove(shortcut);
        
        System.out.println("HomePage: Removed shortcut " + shortcut.getDisplayName() + 
                          " from page " + pageId);
        return true;
    }
    
    /**
     * 指定された位置のショートカットを削除する。
     * 
     * @param gridX グリッド列
     * @param gridY グリッド行
     * @return 削除されたショートカット、または位置が空または無効の場合null
     */
    public Shortcut removeShortcutAt(int gridX, int gridY) {
        Shortcut shortcut = getShortcutAt(gridX, gridY);
        if (shortcut != null) {
            removeShortcut(shortcut);
        }
        return shortcut;
    }
    
    /**
     * このページ上でショートカットをある位置から別の位置に移動する。
     * 
     * @param shortcut 移動するショートカット
     * @param newGridX ターゲットグリッド列
     * @param newGridY ターゲットグリッド行
     * @return ショートカットが正常に移動された場合true
     */
    public boolean moveShortcut(Shortcut shortcut, int newGridX, int newGridY) {
        if (shortcut == null || !shortcuts.contains(shortcut) || !isValidPosition(newGridX, newGridY)) {
            return false;
        }
        
        // Check if target position is empty or contains the same shortcut
        Shortcut existingShortcut = grid[newGridX][newGridY];
        if (existingShortcut != null && existingShortcut != shortcut) {
            return false; // Position occupied by different shortcut
        }
        
        // Clear old position
        int oldX = shortcut.getGridX();
        int oldY = shortcut.getGridY();
        if (isValidPosition(oldX, oldY)) {
            grid[oldX][oldY] = null;
        }
        
        // Set new position
        grid[newGridX][newGridY] = shortcut;
        shortcut.setGridPosition(newGridX, newGridY);
        
        System.out.println("HomePage: Moved shortcut " + shortcut.getDisplayName() + 
                          " from (" + oldX + ", " + oldY + ") to (" + newGridX + ", " + newGridY + ")");
        return true;
    }
    
    /**
     * このページ上の2つのショートカットの位置を交換する。
     * 
     * @param shortcut1 最初のショートカット
     * @param shortcut2 2番目のショートカット
     * @return ショートカットが正常に交換された場合true
     */
    public boolean swapShortcuts(Shortcut shortcut1, Shortcut shortcut2) {
        if (shortcut1 == null || shortcut2 == null || 
            !shortcuts.contains(shortcut1) || !shortcuts.contains(shortcut2)) {
            return false;
        }
        
        int x1 = shortcut1.getGridX();
        int y1 = shortcut1.getGridY();
        int x2 = shortcut2.getGridX();
        int y2 = shortcut2.getGridY();
        
        // Swap positions in grid
        grid[x1][y1] = shortcut2;
        grid[x2][y2] = shortcut1;
        
        // Update shortcut positions
        shortcut1.setGridPosition(x2, y2);
        shortcut2.setGridPosition(x1, y1);
        
        System.out.println("HomePage: Swapped shortcuts " + shortcut1.getDisplayName() + 
                          " and " + shortcut2.getDisplayName());
        return true;
    }
    
    /**
     * 指定されたアプリケーションを参照するショートカットを検索する。
     * 
     * @param application 検索するアプリケーション
     * @return アプリケーションを参照する最初のショートカット、または見つからない場合null
     */
    public Shortcut findShortcutForApplication(IApplication application) {
        if (application == null) {
            return null;
        }
        
        return shortcuts.stream()
                .filter(shortcut -> shortcut.referencesApplication(application))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * このページに指定されたアプリケーション用のショートカットが含まれているかどうかを確認する。
     * 
     * @param application 確認するアプリケーション
     * @return このページにこのアプリケーション用のショートカットが存在する場合true
     */
    public boolean hasShortcutForApplication(IApplication application) {
        return findShortcutForApplication(application) != null;
    }
    
    /**
     * すべてのショートカットを空のスペースを埋めるように移動してページを圧縮する。
     * ショートカットは相対的な順序を維持しながら、最も早い利用可能な位置に移動される。
     */
    public void compact() {
        List<Shortcut> allShortcuts = new ArrayList<>(shortcuts);
        
        // Clear the page
        clear();
        
        // Re-add shortcuts to fill from the top-left
        for (Shortcut shortcut : allShortcuts) {
            addShortcut(shortcut);
        }
        
        System.out.println("HomePage: Compacted page " + pageId + ", now has " + shortcuts.size() + " shortcuts");
    }
    
    /**
     * このページからすべてのショートカットをクリアする。
     */
    public void clear() {
        // Clear grid
        for (int x = 0; x < GRID_COLS; x++) {
            for (int y = 0; y < GRID_ROWS; y++) {
                grid[x][y] = null;
            }
        }
        
        // Clear shortcuts list
        shortcuts.clear();
        
        System.out.println("HomePage: Cleared all shortcuts from page " + pageId);
    }
    
    /**
     * このホームページの文字列表現を返す。
     * 
     * @return このページを説明する文字列
     */
    @Override
    public String toString() {
        return "HomePage{" +
                "id=" + pageId +
                ", name=" + pageName +
                ", type=" + pageType +
                ", shortcuts=" + shortcuts.size() + "/" + (pageType == PageType.NORMAL ? MAX_SHORTCUTS : "∞") +
                '}';
    }
    
    // ===========================================
    // AppLibrary専用メソッド
    // ===========================================
    
    /**
     * ページタイプを取得する。
     * 
     * @return ページタイプ
     */
    public PageType getPageType() {
        return pageType;
    }
    
    /**
     * AppLibraryページかどうかを判定する。
     * 
     * @return AppLibraryページの場合true
     */
    public boolean isAppLibraryPage() {
        return pageType == PageType.APP_LIBRARY;
    }
    
    /**
     * スクロールオフセットを取得する（AppLibraryページ専用）。
     * 
     * @return スクロールオフセット
     */
    public int getScrollOffset() {
        return scrollOffset;
    }
    
    /**
     * スクロールオフセットを設定する（AppLibraryページ専用）。
     * 
     * @param scrollOffset 新しいスクロールオフセット
     */
    public void setScrollOffset(int scrollOffset) {
        if (pageType == PageType.APP_LIBRARY) {
            this.scrollOffset = Math.max(0, scrollOffset);
        }
    }
    
    /**
     * 全アプリケーションリストを設定する（AppLibraryページ専用）。
     * 
     * @param applications アプリケーションのリスト
     */
    public void setAllApplications(List<IApplication> applications) {
        if (pageType == PageType.APP_LIBRARY) {
            this.allApplications.clear();
            if (applications != null) {
                this.allApplications.addAll(applications);
            }
        }
    }
    
    /**
     * 全アプリケーションリストを取得する（AppLibraryページ専用）。
     * 
     * @return アプリケーションのリスト
     */
    public List<IApplication> getAllApplications() {
        return new ArrayList<>(allApplications);
    }
    
    /**
     * AppLibraryページでスクロールが必要かどうかを判定する。
     * 
     * @param visibleHeight 表示可能な高さ
     * @param itemHeight アイテム1つあたりの高さ
     * @return スクロールが必要な場合true
     */
    public boolean needsScrolling(int visibleHeight, int itemHeight) {
        if (pageType != PageType.APP_LIBRARY) {
            return false;
        }
        
        int totalHeight = allApplications.size() * itemHeight;
        return totalHeight > visibleHeight;
    }
    
    /**
     * 指定した座標のアプリケーションを取得する（AppLibraryページ専用）。
     * 
     * @param x X座標
     * @param y Y座標
     * @param listStartY リスト開始Y座標
     * @param itemHeight アイテム高さ
     * @return 該当するアプリケーション、またはnull
     */
    public IApplication getApplicationAtPosition(int x, int y, int listStartY, int itemHeight) {
        if (pageType != PageType.APP_LIBRARY || y < listStartY) {
            return null;
        }
        
        int adjustedY = y + scrollOffset - listStartY;
        int itemIndex = adjustedY / itemHeight;
        
        if (itemIndex >= 0 && itemIndex < allApplications.size()) {
            return allApplications.get(itemIndex);
        }

        return null;
    }

    // ===========================================
    // 常時表示フィールド（Dock）専用メソッド
    // ===========================================

    /**
     * 常時表示フィールドのショートカットリストを取得する。
     *
     * @return 常時表示フィールドのショートカットリスト
     */
    public List<Shortcut> getDockShortcuts() {
        return new ArrayList<>(dockShortcuts);
    }

    /**
     * 常時表示フィールドにショートカットを追加する。
     *
     * @param shortcut 追加するショートカット
     * @return 追加に成功した場合true、満杯の場合false
     */
    public boolean addDockShortcut(Shortcut shortcut) {
        if (shortcut == null || dockShortcuts.size() >= MAX_DOCK_SHORTCUTS) {
            return false;
        }

        // 通常のグリッドから削除（もし存在する場合）
        removeShortcut(shortcut);

        // Dockに追加
        dockShortcuts.add(shortcut);
        shortcut.setDockPosition(dockShortcuts.size() - 1);

        System.out.println("HomePage: Added shortcut " + shortcut.getDisplayName() +
                          " to dock position " + (dockShortcuts.size() - 1));
        return true;
    }

    /**
     * 常時表示フィールドにアプリケーションのショートカットを追加する。
     *
     * @param application 追加するアプリケーション
     * @return 追加に成功した場合true、満杯の場合false
     */
    public boolean addDockShortcut(IApplication application) {
        if (application == null) {
            return false;
        }
        Shortcut shortcut = new Shortcut(application);
        return addDockShortcut(shortcut);
    }

    /**
     * 常時表示フィールドからショートカットを削除する。
     *
     * @param shortcut 削除するショートカット
     * @return 削除に成功した場合true
     */
    public boolean removeDockShortcut(Shortcut shortcut) {
        if (shortcut == null || !dockShortcuts.contains(shortcut)) {
            return false;
        }

        dockShortcuts.remove(shortcut);

        // 残りのショートカットの位置を再調整
        for (int i = 0; i < dockShortcuts.size(); i++) {
            dockShortcuts.get(i).setDockPosition(i);
        }

        System.out.println("HomePage: Removed shortcut " + shortcut.getDisplayName() +
                          " from dock");
        return true;
    }

    /**
     * 常時表示フィールド内でショートカットを移動する。
     *
     * @param shortcut 移動するショートカット
     * @param newPosition 新しい位置（0-3）
     * @return 移動に成功した場合true
     */
    public boolean moveDockShortcut(Shortcut shortcut, int newPosition) {
        if (shortcut == null || !dockShortcuts.contains(shortcut) ||
            newPosition < 0 || newPosition >= dockShortcuts.size()) {
            return false;
        }

        int oldPosition = dockShortcuts.indexOf(shortcut);
        if (oldPosition == newPosition) {
            return true; // 位置変更なし
        }

        // ショートカットを移動
        dockShortcuts.remove(oldPosition);
        dockShortcuts.add(newPosition, shortcut);

        // 位置を再調整
        for (int i = 0; i < dockShortcuts.size(); i++) {
            dockShortcuts.get(i).setDockPosition(i);
        }

        System.out.println("HomePage: Moved dock shortcut " + shortcut.getDisplayName() +
                          " from position " + oldPosition + " to " + newPosition);
        return true;
    }

    /**
     * 常時表示フィールドが満杯かどうかを確認する。
     *
     * @return 満杯の場合true
     */
    public boolean isDockFull() {
        return dockShortcuts.size() >= MAX_DOCK_SHORTCUTS;
    }

    /**
     * 常時表示フィールドが空かどうかを確認する。
     *
     * @return 空の場合true
     */
    public boolean isDockEmpty() {
        return dockShortcuts.isEmpty();
    }

    /**
     * 指定した位置の常時表示フィールドのショートカットを取得する。
     *
     * @param position 位置（0-3）
     * @return ショートカット、存在しない場合null
     */
    public Shortcut getDockShortcutAt(int position) {
        if (position < 0 || position >= dockShortcuts.size()) {
            return null;
        }
        return dockShortcuts.get(position);
    }

    /**
     * 通常のグリッドから常時表示フィールドにショートカットを移動する。
     *
     * @param shortcut 移動するショートカット
     * @return 移動に成功した場合true
     */
    public boolean moveShortcutToDock(Shortcut shortcut) {
        if (shortcut == null || !shortcuts.contains(shortcut) || isDockFull()) {
            return false;
        }

        // 通常のグリッドから削除
        removeShortcut(shortcut);

        // Dockに追加
        return addDockShortcut(shortcut);
    }

    /**
     * 常時表示フィールドから通常のグリッドにショートカットを移動する。
     *
     * @param shortcut 移動するショートカット
     * @return 移動に成功した場合true
     */
    public boolean moveShortcutFromDock(Shortcut shortcut) {
        if (shortcut == null || !dockShortcuts.contains(shortcut) || isFull()) {
            return false;
        }

        // Dockから削除
        removeDockShortcut(shortcut);

        // 通常のグリッドに追加
        return addShortcut(shortcut);
    }
}