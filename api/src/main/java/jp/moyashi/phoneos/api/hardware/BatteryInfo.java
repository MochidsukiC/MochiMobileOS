package jp.moyashi.phoneos.api.hardware;

/**
 * バッテリー情報API。
 * スマートフォンのバッテリー残量とバッテリー寿命を提供する。
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
}
