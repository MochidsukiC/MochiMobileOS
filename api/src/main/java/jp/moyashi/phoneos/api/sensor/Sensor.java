package jp.moyashi.phoneos.api.sensor;

/**
 * センサーを表すクラス。
 * Android SensorのMochiOS版実装。
 */
public class Sensor {

    /** センサータイプ：加速度センサー（m/s^2） */
    public static final int TYPE_ACCELEROMETER = 1;

    /** センサータイプ：ジャイロスコープ（rad/s） */
    public static final int TYPE_GYROSCOPE = 4;

    /** センサータイプ：光センサー（lx） */
    public static final int TYPE_LIGHT = 5;

    /** センサータイプ：近接センサー（cm） */
    public static final int TYPE_PROXIMITY = 8;

    /** センサータイプ：温度センサー（℃） */
    public static final int TYPE_AMBIENT_TEMPERATURE = 13;

    /** センサータイプ：気圧センサー（hPa） */
    public static final int TYPE_PRESSURE = 6;

    /** センサータイプ：湿度センサー（%） */
    public static final int TYPE_RELATIVE_HUMIDITY = 12;

    /** センサータイプ：磁気センサー（μT） */
    public static final int TYPE_MAGNETIC_FIELD = 2;

    /** センサータイプ：GPS位置情報（緯度・経度） */
    public static final int TYPE_GPS = 100;

    /** センサータイプ：バッテリー状態（%） */
    public static final int TYPE_BATTERY = 101;

    /** センサータイプ：ネットワーク状態 */
    public static final int TYPE_NETWORK = 102;

    /** センサー精度：信頼性なし */
    public static final int SENSOR_STATUS_UNRELIABLE = 0;

    /** センサー精度：低精度 */
    public static final int SENSOR_STATUS_ACCURACY_LOW = 1;

    /** センサー精度：中精度 */
    public static final int SENSOR_STATUS_ACCURACY_MEDIUM = 2;

    /** センサー精度：高精度 */
    public static final int SENSOR_STATUS_ACCURACY_HIGH = 3;

    private final int type;
    private final String name;
    private final String vendor;
    private final int version;
    private final float maxRange;
    private final float resolution;
    private final float power;
    private final int minDelay;

    /**
     * センサーを作成する。
     *
     * @param type センサータイプ
     * @param name センサー名
     * @param vendor ベンダー名
     * @param version バージョン
     * @param maxRange 最大測定範囲
     * @param resolution 分解能
     * @param power 消費電力（mA）
     * @param minDelay 最小遅延時間（マイクロ秒）
     */
    public Sensor(int type, String name, String vendor, int version,
                  float maxRange, float resolution, float power, int minDelay) {
        this.type = type;
        this.name = name;
        this.vendor = vendor;
        this.version = version;
        this.maxRange = maxRange;
        this.resolution = resolution;
        this.power = power;
        this.minDelay = minDelay;
    }

    /**
     * センサータイプを取得する。
     *
     * @return センサータイプ
     */
    public int getType() {
        return type;
    }

    /**
     * センサー名を取得する。
     *
     * @return センサー名
     */
    public String getName() {
        return name;
    }

    /**
     * ベンダー名を取得する。
     *
     * @return ベンダー名
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * バージョンを取得する。
     *
     * @return バージョン
     */
    public int getVersion() {
        return version;
    }

    /**
     * 最大測定範囲を取得する。
     *
     * @return 最大測定範囲
     */
    public float getMaximumRange() {
        return maxRange;
    }

    /**
     * 分解能を取得する。
     *
     * @return 分解能
     */
    public float getResolution() {
        return resolution;
    }

    /**
     * 消費電力を取得する。
     *
     * @return 消費電力（mA）
     */
    public float getPower() {
        return power;
    }

    /**
     * 最小遅延時間を取得する。
     *
     * @return 最小遅延時間（マイクロ秒）
     */
    public int getMinDelay() {
        return minDelay;
    }

    @Override
    public String toString() {
        return "Sensor{" +
                "type=" + getTypeString() +
                ", name='" + name + '\'' +
                ", vendor='" + vendor + '\'' +
                ", version=" + version +
                ", maxRange=" + maxRange +
                ", resolution=" + resolution +
                ", power=" + power +
                ", minDelay=" + minDelay +
                '}';
    }

    /**
     * センサータイプを文字列で取得する。
     *
     * @return センサータイプ文字列
     */
    private String getTypeString() {
        switch (type) {
            case TYPE_ACCELEROMETER: return "ACCELEROMETER";
            case TYPE_GYROSCOPE: return "GYROSCOPE";
            case TYPE_LIGHT: return "LIGHT";
            case TYPE_PROXIMITY: return "PROXIMITY";
            case TYPE_AMBIENT_TEMPERATURE: return "AMBIENT_TEMPERATURE";
            case TYPE_PRESSURE: return "PRESSURE";
            case TYPE_RELATIVE_HUMIDITY: return "RELATIVE_HUMIDITY";
            case TYPE_MAGNETIC_FIELD: return "MAGNETIC_FIELD";
            case TYPE_GPS: return "GPS";
            case TYPE_BATTERY: return "BATTERY";
            case TYPE_NETWORK: return "NETWORK";
            default: return "UNKNOWN";
        }
    }
}
