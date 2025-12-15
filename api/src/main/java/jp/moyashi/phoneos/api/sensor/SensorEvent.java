package jp.moyashi.phoneos.api.sensor;

/**
 * センサーイベントを表すクラス。
 * センサーから送信されるデータを保持する。
 */
public class SensorEvent {

    /** センサー情報 */
    public final Sensor sensor;

    /** センサー精度 */
    public int accuracy;

    /** タイムスタンプ（ナノ秒） */
    public long timestamp;

    /** センサー値（最大3軸分のデータ） */
    public final float[] values;

    /**
     * SensorEventを作成する。
     *
     * @param sensor センサー情報
     * @param accuracy 精度
     * @param timestamp タイムスタンプ（ナノ秒）
     * @param values センサー値
     */
    public SensorEvent(Sensor sensor, int accuracy, long timestamp, float[] values) {
        this.sensor = sensor;
        this.accuracy = accuracy;
        this.timestamp = timestamp;
        this.values = values != null ? values.clone() : new float[3];
    }

    /**
     * SensorEventを作成する（デフォルトコンストラクタ）。
     *
     * @param sensor センサー情報
     */
    public SensorEvent(Sensor sensor) {
        this(sensor, Sensor.SENSOR_STATUS_UNRELIABLE, System.nanoTime(), new float[3]);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SensorEvent{");
        sb.append("sensor=").append(sensor.getName());
        sb.append(", accuracy=").append(getAccuracyString());
        sb.append(", timestamp=").append(timestamp);
        sb.append(", values=[");

        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(String.format("%.2f", values[i]));
        }

        sb.append("]}");
        return sb.toString();
    }

    /**
     * 精度を文字列で取得する。
     *
     * @return 精度文字列
     */
    private String getAccuracyString() {
        switch (accuracy) {
            case Sensor.SENSOR_STATUS_UNRELIABLE: return "UNRELIABLE";
            case Sensor.SENSOR_STATUS_ACCURACY_LOW: return "LOW";
            case Sensor.SENSOR_STATUS_ACCURACY_MEDIUM: return "MEDIUM";
            case Sensor.SENSOR_STATUS_ACCURACY_HIGH: return "HIGH";
            default: return "UNKNOWN";
        }
    }
}
