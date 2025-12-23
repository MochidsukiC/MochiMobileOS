package jp.moyashi.phoneos.core.dashboard.widgets;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.dashboard.DashboardWidgetSize;
import jp.moyashi.phoneos.core.dashboard.DashboardWidgetType;
import jp.moyashi.phoneos.core.dashboard.IDashboardWidget;
import jp.moyashi.phoneos.core.ui.theme.ThemeContext;
import processing.core.PGraphics;

/**
 * 電子マネーウィジェット（システムウィジェット）。
 * 情報表示型で、タップすると電子マネーアプリを開く。
 */
public class EMoneyWidget implements IDashboardWidget {

    private static final int RADIUS_SMALL = 8;
    private static final int TEXT_SIZE_LARGE = 14;

    private Kernel kernel;

    @Override
    public String getId() {
        return "system.emoney";
    }

    @Override
    public String getDisplayName() {
        return "電子マネー";
    }

    @Override
    public String getDescription() {
        return "電子マネーアプリを開きます";
    }

    @Override
    public DashboardWidgetSize getSize() {
        return DashboardWidgetSize.HALF_WIDTH;
    }

    @Override
    public DashboardWidgetType getType() {
        return DashboardWidgetType.DISPLAY;
    }

    @Override
    public String getTargetApplicationId() {
        return "jp.moyashi.phoneos.emoney";
    }

    @Override
    public void onAttach(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void draw(PGraphics g, float x, float y, float w, float h) {
        var theme = ThemeContext.getTheme();
        int surface = theme != null ? theme.colorSurface() : 0xFFFFFFFF;
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFF111111;
        int border = theme != null ? theme.colorBorder() : 0xFFCCCCCC;

        // Draw card background
        g.fill((surface >> 16) & 0xFF, (surface >> 8) & 0xFF, surface & 0xFF, 200);
        g.stroke(border);
        g.rect(x, y, w, h, RADIUS_SMALL);

        // Draw label
        g.fill(onSurface);
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(TEXT_SIZE_LARGE);
        g.text("E-Money", x + w / 2, y + h / 2);
    }
}
