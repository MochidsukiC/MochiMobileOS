package jp.moyashi.phoneos.core.service.hardware;

/**
 * バッテリー情報API。
 * スマートフォンのバッテリー残量とバッテリー寿命を提供する。
 *
 * 環境別動作:
 * - standalone: 100%, 100%を返す
 * - forge-mod: アイテムNBTから適切なデータを返す
 */
public interface BatteryInfo {
    /**
     * バッテリー残量を取得する（0-100の範囲）。
     *
     * @return バッテリー残量（パーセント）
     */
    int getBatteryLevel();

    /**
     * バッテリー寿命を取得する（0-100の範囲）。
     *
     * @return バッテリー寿命（パーセント）
     */
    int getBatteryHealth();

    /**
     * 充電中かどうかを取得する。
     *
     * @return 充電中の場合true
     */
    boolean isCharging();

    /**
     * バッテリー残量を設定する（forge-mod用）。
     *
     * @param level バッテリー残量（0-100）
     */
    void setBatteryLevel(int level);

    /**
     * バッテリー寿命を設定する（forge-mod用）。
     *
     * @param health バッテリー寿命（0-100）
     */
    void setBatteryHealth(int health);

    /**
     * 充電状態を設定する（forge-mod用）。
     *
     * @param charging 充電中の場合true
     */
    void setCharging(boolean charging);
}