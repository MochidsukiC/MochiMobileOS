package jp.moyashi.phoneos.core.service.hardware;

/**
 * バッテリー情報のデフォルト実装（standalone用）。
 * 常に100%を返す。
 */
public class DefaultBatteryInfo implements BatteryInfo {
    private int batteryLevel = 100;
    private int batteryHealth = 100;
    private boolean charging = false;

    @Override
    public int getBatteryLevel() {
        return batteryLevel;
    }

    @Override
    public int getBatteryHealth() {
        return batteryHealth;
    }

    @Override
    public boolean isCharging() {
        return charging;
    }

    @Override
    public void setBatteryLevel(int level) {
        this.batteryLevel = Math.max(0, Math.min(100, level));
    }

    @Override
    public void setBatteryHealth(int health) {
        this.batteryHealth = Math.max(0, Math.min(100, health));
    }

    @Override
    public void setCharging(boolean charging) {
        this.charging = charging;
    }
}