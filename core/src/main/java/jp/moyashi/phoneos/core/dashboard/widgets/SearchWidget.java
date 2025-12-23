package jp.moyashi.phoneos.core.dashboard.widgets;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.dashboard.DashboardWidgetSize;
import jp.moyashi.phoneos.core.dashboard.DashboardWidgetType;
import jp.moyashi.phoneos.core.dashboard.IDashboardWidget;
import jp.moyashi.phoneos.core.ui.components.TextField;
import jp.moyashi.phoneos.core.ui.theme.ThemeContext;
import processing.core.PGraphics;

/**
 * 検索ウィジェット（システムウィジェット）。
 * インタラクティブ型で、タップすると検索入力モードになる。
 */
public class SearchWidget implements IDashboardWidget {

    private static final int RADIUS_SMALL = 8;
    private static final int TEXT_SIZE_LARGE = 14;

    private Kernel kernel;
    private TextField searchField;
    private boolean isSearching = false;

    /** 検索状態変更リスナー */
    private SearchStateListener searchStateListener;

    @Override
    public String getId() {
        return "system.search";
    }

    @Override
    public String getDisplayName() {
        return "検索";
    }

    @Override
    public String getDescription() {
        return "アプリやウェブを検索します";
    }

    @Override
    public DashboardWidgetSize getSize() {
        return DashboardWidgetSize.FULL_WIDTH;
    }

    @Override
    public DashboardWidgetType getType() {
        return DashboardWidgetType.INTERACTIVE;
    }

    @Override
    public void onAttach(Kernel kernel) {
        this.kernel = kernel;
        // TextFieldは座標が必要なので、後で初期化
    }

    @Override
    public void draw(PGraphics g, float x, float y, float w, float h) {
        if (isSearching && searchField != null) {
            // 検索フィールドの位置を更新
            searchField.setPosition((int) x, (int) y);
            searchField.setSize((int) w, (int) h);
            searchField.draw(g);
        } else {
            var theme = ThemeContext.getTheme();
            int surface = theme != null ? theme.colorSurface() : 0xFFFFFFFF;
            int onSurface = theme != null ? theme.colorOnSurface() : 0xFF111111;
            int border = theme != null ? theme.colorBorder() : 0xFFCCCCCC;

            // Draw card background
            g.fill((surface >> 16) & 0xFF, (surface >> 8) & 0xFF, surface & 0xFF, 200);
            g.stroke(border);
            g.rect(x, y, w, h, RADIUS_SMALL);

            // Draw placeholder text
            g.fill(onSurface);
            g.textAlign(g.LEFT, g.CENTER);
            g.textSize(TEXT_SIZE_LARGE);
            g.text("Search...", x + 20, y + h / 2);
        }
    }

    @Override
    public boolean onTouch(float localX, float localY, int action) {
        if (action == ACTION_TAP) {
            if (!isSearching) {
                // 検索モードに入る
                startSearching();
                return true;
            } else if (searchField != null) {
                // 検索フィールドにタッチイベントを転送
                // Note: ここではローカル座標をそのまま使用
                searchField.onMousePressed((int) localX, (int) localY);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onKeyEvent(int keyCode, char key) {
        if (isSearching && searchField != null) {
            if (keyCode == 27) { // ESC
                stopSearching();
                return true;
            }
            searchField.onKeyPressed(key, keyCode);
            return true;
        }
        return false;
    }

    /**
     * 検索モードを開始する。
     */
    public void startSearching() {
        isSearching = true;
        if (searchField == null) {
            searchField = new TextField(0, 0, 360, 50, "Search...");
        }
        searchField.setVisible(true);
        searchField.setFocused(true);
        if (searchStateListener != null) {
            searchStateListener.onSearchStateChanged(true);
        }
    }

    /**
     * 検索モードを終了する。
     */
    public void stopSearching() {
        isSearching = false;
        if (searchField != null) {
            searchField.setFocused(false);
            searchField.setVisible(false);
        }
        if (searchStateListener != null) {
            searchStateListener.onSearchStateChanged(false);
        }
    }

    /**
     * 検索中かどうかを取得する。
     *
     * @return 検索中の場合true
     */
    public boolean isSearching() {
        return isSearching;
    }

    /**
     * 検索フィールドを取得する。
     *
     * @return 検索フィールド
     */
    public TextField getSearchField() {
        return searchField;
    }

    /**
     * 検索状態変更リスナーを設定する。
     *
     * @param listener リスナー
     */
    public void setSearchStateListener(SearchStateListener listener) {
        this.searchStateListener = listener;
    }

    /**
     * 検索状態変更リスナーインターフェース。
     */
    public interface SearchStateListener {
        void onSearchStateChanged(boolean searching);
    }
}
