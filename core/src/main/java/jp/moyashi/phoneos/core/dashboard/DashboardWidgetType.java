package jp.moyashi.phoneos.core.dashboard;

/**
 * ダッシュボードウィジェットのタイプ定義。
 */
public enum DashboardWidgetType {
    /**
     * 情報表示型
     * - 情報を表示するのみ
     * - タップするとアプリケーションが開く
     * - シンプルなウィジェット向け
     * 例: 天気、カレンダー予定、歩数計
     */
    DISPLAY,

    /**
     * インタラクティブ型
     * - ウィジェット内でインタラクション可能
     * - ボタン、テキストボックス等のUI要素を配置可能
     * - ボタンから引数付きでアプリを起動可能
     * 例: 検索バー、メディアコントロール、クイックメモ
     */
    INTERACTIVE;

    /**
     * 名前からタイプを取得する。
     *
     * @param name タイプ名
     * @return 対応するタイプ、見つからない場合はDISPLAY
     */
    public static DashboardWidgetType fromName(String name) {
        if (name == null) {
            return DISPLAY;
        }
        for (DashboardWidgetType type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return DISPLAY;
    }
}
