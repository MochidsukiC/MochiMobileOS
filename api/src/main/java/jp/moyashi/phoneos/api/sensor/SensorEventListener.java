package jp.moyashi.phoneos.api.sensor;

/**
 * センサーイベントを受け取るリスナーインターフェース。
 */
public interface SensorEventListener {

    /**
     * センサーの値が変化した時に呼ばれる。
     *
     * @param event センサーイベント
     */
    void onSensorChanged(SensorEvent event);

    /**
     * センサーの精度が変化した時に呼ばれる。
     *
     * @param sensor センサー
     * @param accuracy 新しい精度
     */
    void onAccuracyChanged(Sensor sensor, int accuracy);
}
