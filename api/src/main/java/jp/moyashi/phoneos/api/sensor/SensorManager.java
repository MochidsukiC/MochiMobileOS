package jp.moyashi.phoneos.api.sensor;

import java.util.List;

/**
 * センサー管理のインターフェース。
 * Android SensorManagerのMochiOS版実装。
 */
public interface SensorManager {

    /** センサーサンプリングレート：最速 */
    int SENSOR_DELAY_FASTEST = 0;

    /** センサーサンプリングレート：ゲーム向け */
    int SENSOR_DELAY_GAME = 1;

    /** センサーサンプリングレート：UI更新向け */
    int SENSOR_DELAY_UI = 2;

    /** センサーサンプリングレート：通常 */
    int SENSOR_DELAY_NORMAL = 3;

    /**
     * 利用可能なセンサーのリストを取得する。
     *
     * @param type センサータイプ（-1で全センサー）
     * @return センサーリスト
     */
    List<Sensor> getSensorList(int type);

    /**
     * デフォルトセンサーを取得する。
     *
     * @param type センサータイプ
     * @return センサー（存在しない場合はnull）
     */
    Sensor getDefaultSensor(int type);

    /**
     * センサーリスナーを登録する。
     *
     * @param listener リスナー
     * @param sensor センサー
     * @param samplingPeriodUs サンプリング周期（マイクロ秒）またはサンプリングレート定数（SENSOR_DELAY_*）
     * @return 登録成功時true
     */
    boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs);

    /**
     * 特定のセンサーのリスナーを解除する。
     *
     * @param listener リスナー
     * @param sensor センサー
     */
    void unregisterListener(SensorEventListener listener, Sensor sensor);

    /**
     * すべてのセンサーのリスナーを解除する。
     *
     * @param listener リスナー
     */
    void unregisterListener(SensorEventListener listener);
}
