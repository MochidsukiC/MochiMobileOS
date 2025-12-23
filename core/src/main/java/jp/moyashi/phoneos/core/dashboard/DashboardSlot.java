package jp.moyashi.phoneos.core.dashboard;

/**
 * ダッシュボードのスロット定義。
 * 各スロットは固定位置・サイズを持ち、ウィジェットを配置できる。
 *
 * レイアウト:
 * ┌──────────────────────────────────────┐
 * │  CLOCK: 時計/天気 (変更不可)          │ Y: 80, H: 80
 * ├──────────────────────────────────────┤
 * │  SEARCH: 検索                        │ Y: 170, H: 50
 * ├─────────────────────┬────────────────┤
 * │  LEFT: 左           │  RIGHT: 右     │ Y: 230, H: 150
 * ├──────────────────────────────────────┤
 * │  BOTTOM: 下部                        │ Y: 390, H: 50
 * └──────────────────────────────────────┘
 */
public enum DashboardSlot {
    /** 時計/天気スロット（変更不可） */
    CLOCK(0, DashboardWidgetSize.FULL_WIDTH, 20, 80, 80, false),

    /** 検索スロット */
    SEARCH(1, DashboardWidgetSize.FULL_WIDTH, 20, 170, 50, true),

    /** 左ハーフスロット */
    LEFT(2, DashboardWidgetSize.HALF_WIDTH, 20, 230, 150, true),

    /** 右ハーフスロット */
    RIGHT(3, DashboardWidgetSize.HALF_WIDTH, 205, 230, 150, true),

    /** 下部スロット */
    BOTTOM(4, DashboardWidgetSize.FULL_WIDTH, 20, 390, 50, true);

    private final int index;
    private final DashboardWidgetSize requiredSize;
    private final int x;
    private final int y;
    private final int height;
    private final boolean configurable;

    DashboardSlot(int index, DashboardWidgetSize requiredSize, int x, int y, int height, boolean configurable) {
        this.index = index;
        this.requiredSize = requiredSize;
        this.x = x;
        this.y = y;
        this.height = height;
        this.configurable = configurable;
    }

    /**
     * スロットのインデックスを取得する。
     *
     * @return インデックス（0-4）
     */
    public int getIndex() {
        return index;
    }

    /**
     * このスロットに必要なウィジェットサイズを取得する。
     *
     * @return 必要なサイズ
     */
    public DashboardWidgetSize getRequiredSize() {
        return requiredSize;
    }

    /**
     * スロットのX座標を取得する。
     *
     * @return X座標（ピクセル）
     */
    public int getX() {
        return x;
    }

    /**
     * スロットのY座標を取得する。
     *
     * @return Y座標（ピクセル）
     */
    public int getY() {
        return y;
    }

    /**
     * スロットの幅を取得する。
     *
     * @return 幅（ピクセル）
     */
    public int getWidth() {
        return requiredSize.getWidth();
    }

    /**
     * スロットの高さを取得する。
     *
     * @return 高さ（ピクセル）
     */
    public int getHeight() {
        return height;
    }

    /**
     * このスロットが設定可能かどうかを取得する。
     *
     * @return 設定可能な場合true
     */
    public boolean isConfigurable() {
        return configurable;
    }

    /**
     * 指定座標がこのスロット内にあるかどうかを判定する。
     *
     * @param px X座標
     * @param py Y座標
     * @return スロット内の場合true
     */
    public boolean contains(float px, float py) {
        return px >= x && px < x + getWidth() && py >= y && py < y + height;
    }

    /**
     * インデックスからスロットを取得する。
     *
     * @param index インデックス
     * @return 対応するスロット、見つからない場合はnull
     */
    public static DashboardSlot fromIndex(int index) {
        for (DashboardSlot slot : values()) {
            if (slot.index == index) {
                return slot;
            }
        }
        return null;
    }

    /**
     * 名前からスロットを取得する。
     *
     * @param name スロット名
     * @return 対応するスロット、見つからない場合はnull
     */
    public static DashboardSlot fromName(String name) {
        if (name == null) {
            return null;
        }
        for (DashboardSlot slot : values()) {
            if (slot.name().equalsIgnoreCase(name)) {
                return slot;
            }
        }
        return null;
    }

    /**
     * 指定座標を含むスロットを取得する。
     *
     * @param px X座標
     * @param py Y座標
     * @return 座標を含むスロット、見つからない場合はnull
     */
    public static DashboardSlot getSlotAt(float px, float py) {
        for (DashboardSlot slot : values()) {
            if (slot.contains(px, py)) {
                return slot;
            }
        }
        return null;
    }
}
