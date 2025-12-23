package jp.moyashi.phoneos.core.dashboard.widgets;

import jp.moyashi.phoneos.core.dashboard.DashboardWidgetSize;
import jp.moyashi.phoneos.core.dashboard.DashboardWidgetType;
import jp.moyashi.phoneos.core.dashboard.IDashboardWidget;
import jp.moyashi.phoneos.core.ui.theme.ThemeContext;
import processing.core.PGraphics;

/**
 * AIアシスタントウィジェット（システムウィジェット）。
 * 情報表示型で、タップするとAIアシスタントアプリを開く。
 */
public class AIAssistantWidget implements IDashboardWidget {

    private static final int RADIUS_SMALL = 8;
    private static final int TEXT_SIZE_LARGE = 14;

    @Override
    public String getId() {
        return "system.ai_assistant";
    }

    @Override
    public String getDisplayName() {
        return "AIアシスタント";
    }

    @Override
    public String getDescription() {
        return "AIアシスタントに質問できます";
    }

    @Override
    public DashboardWidgetSize getSize() {
        return DashboardWidgetSize.FULL_WIDTH;
    }

    @Override
    public DashboardWidgetType getType() {
        return DashboardWidgetType.DISPLAY;
    }

    @Override
    public String getTargetApplicationId() {
        return "jp.moyashi.phoneos.assistant";
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

        // Draw placeholder text
        g.fill(onSurface);
        g.textAlign(g.LEFT, g.CENTER);
        g.textSize(TEXT_SIZE_LARGE);
        g.text("AI Assistant...", x + 20, y + h / 2);
    }

}
