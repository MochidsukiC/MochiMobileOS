package jp.moyashi.phoneos.core.power;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.event.EventBus;
import jp.moyashi.phoneos.core.event.system.SystemEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * システムの電源管理を担当するクラス。
 * スリープ、ウェイク、省電力モードの制御を行う。
 * Kernelからパワー管理の責任を分離。
 *
 * 機能:
 * - スリープ/ウェイク状態の管理
 * - パワーイベントのリスナー管理
 * - バッテリー状態の監視（将来の拡張）
 * - 省電力モードの制御
 *
 * @since 2025-12-02
 * @version 1.0
 */
public class PowerManager {

    private static final Logger logger = Logger.getLogger(PowerManager.class.getName());

    /** Kernelインスタンス（互換性のため） */
    private final Kernel kernel;

    /** 現在のパワー状態 */
    private PowerState currentState = PowerState.ACTIVE;

    /** スリープ開始時刻 */
    private long sleepStartTime = 0;

    /** 最後のアクティビティ時刻 */
    private long lastActivityTime = System.currentTimeMillis();

    /** 自動スリープタイムアウト（ミリ秒） */
    private long autoSleepTimeout = 60000; // 1分

    /** 自動スリープが有効か */
    private boolean autoSleepEnabled = false;

    /** パワーイベントリスナー */
    private final List<PowerStateListener> listeners = new CopyOnWriteArrayList<>();

    /** スリープ時のFPS */
    private static final int SLEEP_FPS = 5;

    /** アクティブ時のFPS */
    private static final int ACTIVE_FPS = 60;

    /**
     * パワー状態の定義。
     */
    public enum PowerState {
        /** アクティブ状態 */
        ACTIVE,
        /** スリープ準備中 */
        GOING_TO_SLEEP,
        /** スリープ中 */
        SLEEPING,
        /** ウェイク中 */
        WAKING_UP,
        /** 省電力モード */
        POWER_SAVING,
        /** シャットダウン中 */
        SHUTTING_DOWN
    }

    /**
     * パワー状態変更のリスナーインターフェース。
     */
    public interface PowerStateListener {
        /**
         * パワー状態が変更されたときに呼ばれる。
         *
         * @param oldState 前の状態
         * @param newState 新しい状態
         */
        void onPowerStateChanged(PowerState oldState, PowerState newState);

        /**
         * スリープ前に呼ばれる（キャンセル可能）。
         *
         * @return スリープを許可する場合 true
         */
        default boolean onBeforeSleep() { return true; }

        /**
         * ウェイク後に呼ばれる。
         */
        default void onAfterWake() {}
    }

    /**
     * PowerManagerを初期化する。
     *
     * @param kernel Kernelインスタンス
     */
    public PowerManager(Kernel kernel) {
        this.kernel = kernel;
        logger.info("PowerManager initialized");
    }

    /**
     * システムをスリープ状態にする。
     *
     * @return スリープに成功した場合 true
     */
    public boolean sleep() {
        if (currentState == PowerState.SLEEPING) {
            logger.fine("Already sleeping");
            return true;
        }

        if (!canSleep()) {
            logger.warning("Sleep blocked by listener");
            return false;
        }

        logger.info("Entering sleep mode");

        PowerState oldState = currentState;
        currentState = PowerState.GOING_TO_SLEEP;
        notifyStateChanged(oldState, currentState);

        // EventBusにスリープイベントを発行
        EventBus.getInstance().post(SystemEvent.sleep(this));

        // スリープ処理
        performSleep();

        currentState = PowerState.SLEEPING;
        sleepStartTime = System.currentTimeMillis();
        notifyStateChanged(PowerState.GOING_TO_SLEEP, currentState);

        // FPS削減（省電力）
        if (kernel != null) {
            kernel.frameRate(SLEEP_FPS);
        }

        return true;
    }

    /**
     * システムをウェイク状態にする。
     */
    public void wake() {
        if (currentState != PowerState.SLEEPING) {
            logger.fine("Not sleeping, cannot wake");
            return;
        }

        logger.info("Waking up from sleep");

        PowerState oldState = currentState;
        currentState = PowerState.WAKING_UP;
        notifyStateChanged(oldState, currentState);

        // EventBusにウェイクイベントを発行
        EventBus.getInstance().post(SystemEvent.wake(this));

        // ウェイク処理
        performWake();

        currentState = PowerState.ACTIVE;
        sleepStartTime = 0;
        lastActivityTime = System.currentTimeMillis();
        notifyStateChanged(PowerState.WAKING_UP, currentState);

        // FPS復元
        if (kernel != null) {
            kernel.frameRate(ACTIVE_FPS);
        }

        // リスナーに通知
        for (PowerStateListener listener : listeners) {
            try {
                listener.onAfterWake();
            } catch (Exception e) {
                logger.warning("Error in wake listener: " + e.getMessage());
            }
        }
    }

    /**
     * 省電力モードを切り替える。
     *
     * @param enabled 有効にする場合 true
     */
    public void setPowerSavingMode(boolean enabled) {
        if (enabled && currentState == PowerState.ACTIVE) {
            PowerState oldState = currentState;
            currentState = PowerState.POWER_SAVING;
            notifyStateChanged(oldState, currentState);

            // FPS削減
            if (kernel != null) {
                kernel.frameRate(30); // 省電力時は30FPS
            }
            logger.info("Power saving mode enabled");

        } else if (!enabled && currentState == PowerState.POWER_SAVING) {
            PowerState oldState = currentState;
            currentState = PowerState.ACTIVE;
            notifyStateChanged(oldState, currentState);

            // FPS復元
            if (kernel != null) {
                kernel.frameRate(ACTIVE_FPS);
            }
            logger.info("Power saving mode disabled");
        }
    }

    /**
     * アクティビティを記録する（自動スリープのリセット）。
     */
    public void recordActivity() {
        lastActivityTime = System.currentTimeMillis();
    }

    /**
     * 自動スリープをチェックして実行する。
     * 定期的に呼び出す必要がある。
     */
    public void checkAutoSleep() {
        if (!autoSleepEnabled || currentState != PowerState.ACTIVE) {
            return;
        }

        long inactiveTime = System.currentTimeMillis() - lastActivityTime;
        if (inactiveTime > autoSleepTimeout) {
            logger.info("Auto-sleep triggered after " + (inactiveTime / 1000) + " seconds");
            sleep();
        }
    }

    /**
     * 自動スリープを設定する。
     *
     * @param enabled 有効にする場合 true
     * @param timeoutMs タイムアウト時間（ミリ秒）
     */
    public void setAutoSleep(boolean enabled, long timeoutMs) {
        this.autoSleepEnabled = enabled;
        this.autoSleepTimeout = timeoutMs;
        logger.info("Auto-sleep " + (enabled ? "enabled" : "disabled") +
                   " with timeout: " + timeoutMs + "ms");
    }

    /**
     * スリープ可能かを確認する。
     *
     * @return スリープ可能な場合 true
     */
    private boolean canSleep() {
        // すべてのリスナーに確認
        for (PowerStateListener listener : listeners) {
            try {
                if (!listener.onBeforeSleep()) {
                    return false;
                }
            } catch (Exception e) {
                logger.warning("Error in sleep listener: " + e.getMessage());
            }
        }
        return true;
    }

    /**
     * スリープ処理を実行する。
     */
    private void performSleep() {
        // スクリーンオフ、サービス一時停止などの処理
        logger.fine("Performing sleep operations");
    }

    /**
     * ウェイク処理を実行する。
     */
    private void performWake() {
        // スクリーンオン、サービス再開などの処理
        logger.fine("Performing wake operations");
    }

    /**
     * システムをシャットダウンする。
     */
    public void shutdown() {
        logger.info("Initiating system shutdown");

        PowerState oldState = currentState;
        currentState = PowerState.SHUTTING_DOWN;
        notifyStateChanged(oldState, currentState);

        // シャットダウン処理
        // リソースの解放、設定の保存など

        logger.info("System shutdown completed");
    }

    /**
     * 現在のパワー状態を取得する。
     *
     * @return 現在のパワー状態
     */
    public PowerState getCurrentState() {
        return currentState;
    }

    /**
     * スリープ中かを判定する。
     *
     * @return スリープ中の場合 true
     */
    public boolean isSleeping() {
        return currentState == PowerState.SLEEPING;
    }

    /**
     * アクティブ状態かを判定する。
     *
     * @return アクティブの場合 true
     */
    public boolean isActive() {
        return currentState == PowerState.ACTIVE;
    }

    /**
     * 省電力モード中かを判定する。
     *
     * @return 省電力モードの場合 true
     */
    public boolean isPowerSaving() {
        return currentState == PowerState.POWER_SAVING;
    }

    /**
     * スリープ時間を取得する。
     *
     * @return スリープ時間（ミリ秒）、スリープ中でない場合は0
     */
    public long getSleepDuration() {
        if (currentState == PowerState.SLEEPING && sleepStartTime > 0) {
            return System.currentTimeMillis() - sleepStartTime;
        }
        return 0;
    }

    /**
     * パワー状態リスナーを追加する。
     *
     * @param listener リスナー
     */
    public void addPowerStateListener(PowerStateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            logger.fine("Power state listener added");
        }
    }

    /**
     * パワー状態リスナーを削除する。
     *
     * @param listener リスナー
     */
    public void removePowerStateListener(PowerStateListener listener) {
        listeners.remove(listener);
    }

    /**
     * 状態変更を通知する。
     */
    private void notifyStateChanged(PowerState oldState, PowerState newState) {
        for (PowerStateListener listener : listeners) {
            try {
                listener.onPowerStateChanged(oldState, newState);
            } catch (Exception e) {
                logger.warning("Error notifying power state listener: " + e.getMessage());
            }
        }
    }

    /**
     * バッテリー情報を取得する（将来の拡張用）。
     *
     * @return バッテリーレベル（0.0-1.0）
     */
    public float getBatteryLevel() {
        // 実装は将来追加
        return 1.0f; // 常にフル充電として返す
    }

    /**
     * 充電中かを判定する（将来の拡張用）。
     *
     * @return 充電中の場合 true
     */
    public boolean isCharging() {
        // 実装は将来追加
        return true; // 常に充電中として返す
    }
}