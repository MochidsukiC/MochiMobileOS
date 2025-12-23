package jp.moyashi.phoneos.core.controls;

/**
 * コントロールセンターのセクション定義。
 * カードはいずれかのセクションに属し、セクション単位で管理される。
 */
public enum ControlCenterSection {
    /** クイック設定: WiFi, Bluetooth, 機内モードなど */
    QUICK_SETTINGS("クイック設定", 0),

    /** メディア: Now Playing, 音量スライダーなど */
    MEDIA("メディア", 1),

    /** ディスプレイ: 輝度, ダークモード, 回転ロックなど */
    DISPLAY("ディスプレイ", 2),

    /** アプリウィジェット: 外部アプリ提供のカード */
    APP_WIDGETS("アプリウィジェット", 3);

    private final String displayName;
    private final int defaultOrder;

    ControlCenterSection(String displayName, int defaultOrder) {
        this.displayName = displayName;
        this.defaultOrder = defaultOrder;
    }

    /**
     * セクションの表示名を取得する。
     * @return 表示名
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * セクションのデフォルト表示順序を取得する。
     * @return デフォルト順序（小さい値が先頭）
     */
    public int getDefaultOrder() {
        return defaultOrder;
    }

    /**
     * 名前からセクションを取得する。
     * @param name セクション名
     * @return セクション、見つからない場合はAPP_WIDGETS
     */
    public static ControlCenterSection fromName(String name) {
        if (name == null) {
            return APP_WIDGETS;
        }
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return APP_WIDGETS;
        }
    }
}
