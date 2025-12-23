package jp.moyashi.phoneos.core.dashboard;

/**
 * ダッシュボードウィジェットのサイズ定義。
 * iPhoneのウィジェットと同様に、フル幅とハーフ幅の2種類をサポート。
 */
public enum DashboardWidgetSize {
    /**
     * フル幅ウィジェット（360px幅）
     * Slot 1（検索）、Slot 4（下部）で使用
     */
    FULL_WIDTH(360),

    /**
     * ハーフ幅ウィジェット（175px幅）
     * Slot 2（左）、Slot 3（右）で使用
     */
    HALF_WIDTH(175);

    private final int width;

    DashboardWidgetSize(int width) {
        this.width = width;
    }

    /**
     * ウィジェットの幅を取得する。
     *
     * @return 幅（ピクセル）
     */
    public int getWidth() {
        return width;
    }

    /**
     * 名前からサイズを取得する。
     *
     * @param name サイズ名
     * @return 対応するサイズ、見つからない場合はFULL_WIDTH
     */
    public static DashboardWidgetSize fromName(String name) {
        if (name == null) {
            return FULL_WIDTH;
        }
        for (DashboardWidgetSize size : values()) {
            if (size.name().equalsIgnoreCase(name)) {
                return size;
            }
        }
        return FULL_WIDTH;
    }
}
