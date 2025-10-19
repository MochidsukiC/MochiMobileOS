package jp.moyashi.phoneos.core.service.sensor;

import jp.moyashi.phoneos.core.Kernel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SensorManagerの実装クラス。
 * センサーの登録、リスナー管理、イベント配信を行う。
 */
public class SensorManagerImpl implements SensorManager {

    private final Kernel kernel;

    /** 利用可能なセンサーリスト */
    private final List<Sensor> availableSensors;

    /** センサータイプ別のデフォルトセンサー */
    private final Map<Integer, Sensor> defaultSensors;

    /** センサーリスナーの登録情報 */
    private final Map<Sensor, List<ListenerRegistration>> sensorListeners;

    /** シミュレーション用のセンサー値 */
    private final Map<Integer, float[]> simulatedValues;

    /** シミュレーション用のセンサー精度 */
    private final Map<Integer, Integer> simulatedAccuracy;

    /** 有効化されているセンサー */
    private final Set<Sensor> enabledSensors;

    /**
     * リスナー登録情報を保持するクラス。
     */
    private static class ListenerRegistration {
        SensorEventListener listener;
        int samplingPeriodUs;
        long lastUpdateTime;

        ListenerRegistration(SensorEventListener listener, int samplingPeriodUs) {
            this.listener = listener;
            this.samplingPeriodUs = samplingPeriodUs;
            this.lastUpdateTime = 0;
        }
    }

    /**
     * SensorManagerを作成する。
     *
     * @param kernel Kernelインスタンス
     */
    public SensorManagerImpl(Kernel kernel) {
        this.kernel = kernel;
        this.availableSensors = new CopyOnWriteArrayList<>();
        this.defaultSensors = new ConcurrentHashMap<>();
        this.sensorListeners = new ConcurrentHashMap<>();
        this.simulatedValues = new ConcurrentHashMap<>();
        this.simulatedAccuracy = new ConcurrentHashMap<>();
        this.enabledSensors = ConcurrentHashMap.newKeySet();

        initializeDefaultSensors();
    }

    /**
     * デフォルトセンサーを初期化する。
     */
    private void initializeDefaultSensors() {
        // 加速度センサー
        Sensor accelerometer = new Sensor(
                Sensor.TYPE_ACCELEROMETER,
                "MochiOS Accelerometer",
                "MochiOS",
                1,
                19.6f,  // ±2g
                0.01f,
                0.5f,
                10000
        );
        availableSensors.add(accelerometer);
        defaultSensors.put(Sensor.TYPE_ACCELEROMETER, accelerometer);

        // ジャイロスコープ
        Sensor gyroscope = new Sensor(
                Sensor.TYPE_GYROSCOPE,
                "MochiOS Gyroscope",
                "MochiOS",
                1,
                34.9f,  // ±2000 dps
                0.001f,
                6.0f,
                10000
        );
        availableSensors.add(gyroscope);
        defaultSensors.put(Sensor.TYPE_GYROSCOPE, gyroscope);

        // 光センサー
        Sensor light = new Sensor(
                Sensor.TYPE_LIGHT,
                "MochiOS Light Sensor",
                "MochiOS",
                1,
                10000.0f,
                1.0f,
                0.75f,
                0
        );
        availableSensors.add(light);
        defaultSensors.put(Sensor.TYPE_LIGHT, light);

        // 近接センサー
        Sensor proximity = new Sensor(
                Sensor.TYPE_PROXIMITY,
                "MochiOS Proximity Sensor",
                "MochiOS",
                1,
                5.0f,
                5.0f,
                3.0f,
                0
        );
        availableSensors.add(proximity);
        defaultSensors.put(Sensor.TYPE_PROXIMITY, proximity);

        // 温度センサー
        Sensor temperature = new Sensor(
                Sensor.TYPE_AMBIENT_TEMPERATURE,
                "MochiOS Temperature Sensor",
                "MochiOS",
                1,
                80.0f,
                0.1f,
                0.001f,
                0
        );
        availableSensors.add(temperature);
        defaultSensors.put(Sensor.TYPE_AMBIENT_TEMPERATURE, temperature);

        // 気圧センサー
        Sensor pressure = new Sensor(
                Sensor.TYPE_PRESSURE,
                "MochiOS Pressure Sensor",
                "MochiOS",
                1,
                1100.0f,
                0.01f,
                0.001f,
                0
        );
        availableSensors.add(pressure);
        defaultSensors.put(Sensor.TYPE_PRESSURE, pressure);

        // 湿度センサー
        Sensor humidity = new Sensor(
                Sensor.TYPE_RELATIVE_HUMIDITY,
                "MochiOS Humidity Sensor",
                "MochiOS",
                1,
                100.0f,
                1.0f,
                0.001f,
                0
        );
        availableSensors.add(humidity);
        defaultSensors.put(Sensor.TYPE_RELATIVE_HUMIDITY, humidity);

        // GPS
        Sensor gps = new Sensor(
                Sensor.TYPE_GPS,
                "MochiOS GPS",
                "MochiOS",
                1,
                180.0f,
                0.00001f,
                50.0f,
                1000000
        );
        availableSensors.add(gps);
        defaultSensors.put(Sensor.TYPE_GPS, gps);

        // バッテリー
        Sensor battery = new Sensor(
                Sensor.TYPE_BATTERY,
                "MochiOS Battery Monitor",
                "MochiOS",
                1,
                100.0f,
                1.0f,
                0.0f,
                0
        );
        availableSensors.add(battery);
        defaultSensors.put(Sensor.TYPE_BATTERY, battery);

        System.out.println("SensorManager: Initialized " + availableSensors.size() + " default sensors");

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("SensorManager", "デフォルトセンサーを初期化: " + availableSensors.size() + "個");
        }
    }

    @Override
    public List<Sensor> getSensorList(int type) {
        if (type == -1) {
            return new ArrayList<>(availableSensors);
        }

        List<Sensor> result = new ArrayList<>();
        for (Sensor sensor : availableSensors) {
            if (sensor.getType() == type) {
                result.add(sensor);
            }
        }
        return result;
    }

    @Override
    public Sensor getDefaultSensor(int type) {
        return defaultSensors.get(type);
    }

    @Override
    public boolean registerListener(SensorEventListener listener, Sensor sensor, int samplingPeriodUs) {
        if (listener == null || sensor == null) {
            System.err.println("SensorManager: Cannot register null listener or sensor");
            return false;
        }

        if (!availableSensors.contains(sensor)) {
            System.err.println("SensorManager: Sensor not available: " + sensor.getName());
            return false;
        }

        // 定数値（SENSOR_DELAY_*）の場合はマイクロ秒に変換
        int actualSamplingPeriodUs = samplingPeriodUs;
        if (samplingPeriodUs >= 0 && samplingPeriodUs <= 3) {
            switch (samplingPeriodUs) {
                case SENSOR_DELAY_FASTEST:
                    actualSamplingPeriodUs = 0;
                    break;
                case SENSOR_DELAY_GAME:
                    actualSamplingPeriodUs = 20000; // 20ms = 50Hz
                    break;
                case SENSOR_DELAY_UI:
                    actualSamplingPeriodUs = 66667; // ~66ms = 15Hz
                    break;
                case SENSOR_DELAY_NORMAL:
                default:
                    actualSamplingPeriodUs = 200000; // 200ms = 5Hz
                    break;
            }
        }

        sensorListeners.computeIfAbsent(sensor, k -> new CopyOnWriteArrayList<>())
                .add(new ListenerRegistration(listener, actualSamplingPeriodUs));

        enabledSensors.add(sensor);

        System.out.println("SensorManager: Registered listener for " + sensor.getName() +
                         " with sampling period " + actualSamplingPeriodUs + "us");

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("SensorManager",
                    "センサーリスナー登録: " + sensor.getName() + ", " + actualSamplingPeriodUs + "us");
        }

        return true;
    }

    @Override
    public void unregisterListener(SensorEventListener listener, Sensor sensor) {
        if (listener == null || sensor == null) {
            return;
        }

        List<ListenerRegistration> registrations = sensorListeners.get(sensor);
        if (registrations != null) {
            registrations.removeIf(reg -> reg.listener == listener);

            if (registrations.isEmpty()) {
                sensorListeners.remove(sensor);
                enabledSensors.remove(sensor);
            }

            System.out.println("SensorManager: Unregistered listener for " + sensor.getName());

            if (kernel.getLogger() != null) {
                kernel.getLogger().info("SensorManager",
                        "センサーリスナー解除: " + sensor.getName());
            }
        }
    }

    @Override
    public void unregisterListener(SensorEventListener listener) {
        if (listener == null) {
            return;
        }

        for (Sensor sensor : new ArrayList<>(sensorListeners.keySet())) {
            unregisterListener(listener, sensor);
        }
    }

    @Override
    public void setSimulatedSensorValues(int sensorType, float[] values) {
        if (values == null) {
            simulatedValues.remove(sensorType);
            return;
        }

        simulatedValues.put(sensorType, values.clone());

        // リスナーに通知
        Sensor sensor = getDefaultSensor(sensorType);
        if (sensor != null) {
            notifyListeners(sensor, values);
        }
    }

    @Override
    public void setSimulatedSensorAccuracy(int sensorType, int accuracy) {
        simulatedAccuracy.put(sensorType, accuracy);
    }

    @Override
    public boolean enableSensor(Sensor sensor) {
        if (sensor == null || !availableSensors.contains(sensor)) {
            return false;
        }

        enabledSensors.add(sensor);
        System.out.println("SensorManager: Enabled sensor: " + sensor.getName());

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("SensorManager", "センサー有効化: " + sensor.getName());
        }

        return true;
    }

    @Override
    public boolean disableSensor(Sensor sensor) {
        if (sensor == null) {
            return false;
        }

        enabledSensors.remove(sensor);
        System.out.println("SensorManager: Disabled sensor: " + sensor.getName());

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("SensorManager", "センサー無効化: " + sensor.getName());
        }

        return true;
    }

    /**
     * リスナーに通知する（内部メソッド）。
     *
     * @param sensor センサー
     * @param values センサー値
     */
    private void notifyListeners(Sensor sensor, float[] values) {
        List<ListenerRegistration> registrations = sensorListeners.get(sensor);
        if (registrations == null || registrations.isEmpty()) {
            return;
        }

        int accuracy = simulatedAccuracy.getOrDefault(sensor.getType(), Sensor.SENSOR_STATUS_ACCURACY_HIGH);
        long currentTime = System.nanoTime();

        for (ListenerRegistration reg : registrations) {
            // サンプリング周期のチェック
            long elapsedTime = currentTime - reg.lastUpdateTime;
            if (reg.samplingPeriodUs > 0 && elapsedTime < reg.samplingPeriodUs * 1000) {
                continue; // まだサンプリング周期に達していない
            }

            SensorEvent event = new SensorEvent(sensor, accuracy, currentTime, values);
            reg.listener.onSensorChanged(event);
            reg.lastUpdateTime = currentTime;
        }
    }

    /**
     * 更新処理（Kernelから定期的に呼ばれる想定）。
     */
    public void update() {
        // シミュレートされたセンサー値を各リスナーに通知
        for (Map.Entry<Integer, float[]> entry : simulatedValues.entrySet()) {
            Sensor sensor = getDefaultSensor(entry.getKey());
            if (sensor != null && enabledSensors.contains(sensor)) {
                notifyListeners(sensor, entry.getValue());
            }
        }
    }
}
