package jp.moyashi.phoneos.core.dashboard.widgets;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.dashboard.DashboardWidgetSize;
import jp.moyashi.phoneos.core.dashboard.DashboardWidgetType;
import jp.moyashi.phoneos.core.dashboard.IDashboardWidget;
import jp.moyashi.phoneos.core.ui.theme.ThemeContext;
import processing.core.PGraphics;

/**
 * 時計/天気ウィジェット（システムウィジェット）。
 * ダッシュボードの最上部に固定表示される。
 */
public class ClockWidget implements IDashboardWidget {

    private static final int RADIUS_SMALL = 8;
    private static final int TEXT_SIZE_XL = 16;

    private Kernel kernel;
    private float currentHumidity = 40.0f;
    private float currentLightLevel = 15.0f;

    @Override
    public String getId() {
        return "system.clock";
    }

    @Override
    public String getDisplayName() {
        return "時計/天気";
    }

    @Override
    public String getDescription() {
        return "現在時刻と天気を表示します";
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

        // Get time
        String time = "--:--";
        if (kernel != null && kernel.getSystemClock() != null) {
            try {
                time = kernel.getSystemClock().getFormattedTime();
            } catch (Exception e) {
                // ignore
            }
        }

        // Determine weather
        String weather = determineWeather();

        // Draw time
        g.fill(onSurface);
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(TEXT_SIZE_XL);
        g.text(time, x + 20, y + 20);

        // Draw weather
        g.textAlign(g.RIGHT, g.TOP);
        g.text(weather, x + w - 20, y + 20);
    }

    private String determineWeather() {
        if (currentHumidity > 80) {
            if (currentLightLevel < 10) {
                return "Thundering";
            } else {
                return "Rainy";
            }
        }
        return "Clear";
    }

    /**
     * 湿度を設定する（センサーからの更新用）。
     *
     * @param humidity 湿度（0-100）
     */
    public void setHumidity(float humidity) {
        this.currentHumidity = humidity;
    }

    /**
     * 光レベルを設定する（センサーからの更新用）。
     *
     * @param lightLevel 光レベル
     */
    public void setLightLevel(float lightLevel) {
        this.currentLightLevel = lightLevel;
    }
}
