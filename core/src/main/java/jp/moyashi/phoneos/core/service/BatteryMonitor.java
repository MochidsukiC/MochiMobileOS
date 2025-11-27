package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.service.hardware.BatteryInfo;

/**
 * バッテリー状態を監視し、自動バッテリーセーバーを制御するサービス。
 * バッテリー残量が指定された閾値を下回った場合、自動的にバッテリーセーバーモードを有効化する。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class BatteryMonitor {

    /** バッテリー情報API */
    private final BatteryInfo batteryInfo;

    /** 設定マネージャー */
    private final SettingsManager settingsManager;

    /** 前回のチェック時のバッテリーレベル */
    private int lastBatteryLevel = -1;

    /**
     * BatteryMonitorインスタンスを構築する。
     *
     * @param batteryInfo バッテリー情報API
     * @param settingsManager 設定マネージャー
     */
    public BatteryMonitor(BatteryInfo batteryInfo, SettingsManager settingsManager) {
        this.batteryInfo = batteryInfo;
        this.settingsManager = settingsManager;
    }

    /**
     * バッテリーレベルをチェックし、必要に応じて自動バッテリーセーバーを起動する。
     * このメソッドは定期的に（例: Kernelのupdate()から）呼び出されることを想定している。
     */
    public void checkBatteryLevel() {
        if (batteryInfo == null) {
            return;
        }

        // 自動バッテリーセーバーが無効の場合は何もしない
        boolean autoEnabled = settingsManager.getBooleanSetting("power.battery_saver.auto", true);
        if (!autoEnabled) {
            return;
        }

        // 現在のバッテリーレベルを取得
        int currentLevel = batteryInfo.getBatteryLevel();

        // 前回と同じレベルの場合はスキップ（不要な処理を避ける）
        if (currentLevel == lastBatteryLevel) {
            return;
        }

        lastBatteryLevel = currentLevel;

        // 閾値を取得
        int threshold = settingsManager.getIntSetting("power.battery_saver.threshold", 20);

        // バッテリーレベルが閾値以下の場合、バッテリーセーバーを有効化
        if (currentLevel <= threshold) {
            boolean batterySaverEnabled = settingsManager.getBooleanSetting("power.battery_saver.enabled", false);
            if (!batterySaverEnabled) {
                settingsManager.setSetting("power.battery_saver.enabled", true);
                settingsManager.setSetting("ui.performance.low_power", true);
                settingsManager.saveSettings();
                System.out.println("BatteryMonitor: バッテリーセーバーを自動的に有効化しました（残量: " + currentLevel + "%）");
            }
        } else {
            // バッテリーレベルが閾値を上回った場合、自動で有効化されたバッテリーセーバーを無効化
            // （ただし、ユーザーが手動で有効化した場合は無効化しない）
            // この実装では、閾値+5%を超えた場合のみ無効化する（ヒステリシスを持たせる）
            if (currentLevel > threshold + 5) {
                boolean batterySaverEnabled = settingsManager.getBooleanSetting("power.battery_saver.enabled", false);
                if (batterySaverEnabled && autoEnabled) {
                    // TODO: ユーザーが手動で有効化したかどうかを判定するフラグが必要
                    // 現在の実装では、自動有効化のみをサポート
                }
            }
        }
    }

    /**
     * バッテリーレベルの状態文字列を取得する。
     *
     * @return バッテリーレベルの状態（"充電中"、"満充電"、"高い"、"中程度"、"低い"、"非常に低い"）
     */
    public String getBatteryStatus() {
        if (batteryInfo == null) {
            return "不明";
        }

        int level = batteryInfo.getBatteryLevel();
        boolean charging = batteryInfo.isCharging();

        if (charging) {
            return "充電中";
        } else if (level >= 95) {
            return "満充電";
        } else if (level >= 50) {
            return "高い";
        } else if (level >= 20) {
            return "中程度";
        } else if (level >= 10) {
            return "低い";
        } else {
            return "非常に低い";
        }
    }
}
