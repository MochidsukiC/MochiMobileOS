package jp.moyashi.phoneos.core.lifecycle;

import jp.moyashi.phoneos.core.Kernel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

/**
 * システムのライフサイクル管理を担当するクラス。
 * アプリケーションの起動、一時停止、再開、終了を管理する。
 *
 * 機能:
 * - システム起動/終了の管理
 * - アプリケーション一時停止/再開の処理
 * - ライフサイクルイベントの通知
 * - リソースの適切な解放
 * - 状態の保存と復元
 *
 * @since 2025-12-02
 * @version 1.0
 */
public class SystemLifecycleManager {

    private static final Logger logger = Logger.getLogger(SystemLifecycleManager.class.getName());

    /** Kernelインスタンス */
    private final Kernel kernel;

    /** 現在のライフサイクル状態 */
    private LifecycleState currentState = LifecycleState.NOT_STARTED;

    /** ライフサイクルイベントリスナー */
    private final List<LifecycleListener> listeners = new CopyOnWriteArrayList<>();

    /** システム開始時刻 */
    private long startTime = 0;

    /** 最後の一時停止時刻 */
    private long lastPauseTime = 0;

    /** 総実行時間（一時停止を除く） */
    private long totalActiveTime = 0;

    /**
     * ライフサイクル状態の定義。
     */
    public enum LifecycleState {
        /** 未開始 */
        NOT_STARTED,
        /** 起動中 */
        STARTING,
        /** 実行中 */
        RUNNING,
        /** 一時停止中 */
        PAUSED,
        /** 再開中 */
        RESUMING,
        /** 停止中 */
        STOPPING,
        /** 停止済み */
        STOPPED,
        /** エラー状態 */
        ERROR
    }

    /**
     * ライフサイクルイベントリスナーインターフェース。
     */
    public interface LifecycleListener {
        /**
         * システム起動前に呼ばれる。
         * @return 起動を続行する場合 true
         */
        default boolean onBeforeStart() { return true; }

        /**
         * システム起動後に呼ばれる。
         */
        default void onStart() {}

        /**
         * システム一時停止前に呼ばれる。
         */
        default void onPause() {}

        /**
         * システム再開時に呼ばれる。
         */
        default void onResume() {}

        /**
         * システム停止前に呼ばれる。
         */
        default void onStop() {}

        /**
         * エラー発生時に呼ばれる。
         * @param error 発生したエラー
         */
        default void onError(Throwable error) {}

        /**
         * 状態変更時に呼ばれる。
         * @param oldState 前の状態
         * @param newState 新しい状態
         */
        default void onStateChanged(LifecycleState oldState, LifecycleState newState) {}
    }

    /**
     * SystemLifecycleManagerを初期化する。
     *
     * @param kernel Kernelインスタンス
     */
    public SystemLifecycleManager(Kernel kernel) {
        this.kernel = kernel;
        logger.info("SystemLifecycleManager initialized");
    }

    /**
     * システムを起動する。
     *
     * @return 起動に成功した場合 true
     */
    public boolean start() {
        if (currentState != LifecycleState.NOT_STARTED &&
            currentState != LifecycleState.STOPPED) {
            logger.warning("Cannot start: current state is " + currentState);
            return false;
        }

        logger.info("Starting system...");

        // 起動前の確認
        for (LifecycleListener listener : listeners) {
            try {
                if (!listener.onBeforeStart()) {
                    logger.warning("Start cancelled by listener");
                    return false;
                }
            } catch (Exception e) {
                handleError("Error in onBeforeStart listener", e);
                return false;
            }
        }

        changeState(LifecycleState.STARTING);

        try {
            // システム起動処理
            performStart();

            startTime = System.currentTimeMillis();
            changeState(LifecycleState.RUNNING);

            // 起動完了通知
            for (LifecycleListener listener : listeners) {
                try {
                    listener.onStart();
                } catch (Exception e) {
                    handleError("Error in onStart listener", e);
                }
            }

            logger.info("System started successfully");
            return true;

        } catch (Exception e) {
            handleError("Failed to start system", e);
            changeState(LifecycleState.ERROR);
            return false;
        }
    }

    /**
     * システムを一時停止する。
     */
    public void pause() {
        if (currentState != LifecycleState.RUNNING) {
            logger.warning("Cannot pause: current state is " + currentState);
            return;
        }

        logger.info("Pausing system...");
        changeState(LifecycleState.PAUSED);

        // 実行時間を記録
        if (lastPauseTime == 0) {
            totalActiveTime += System.currentTimeMillis() - startTime;
        } else {
            totalActiveTime += System.currentTimeMillis() - lastPauseTime;
        }
        lastPauseTime = System.currentTimeMillis();

        // 一時停止処理
        performPause();

        // リスナーに通知
        for (LifecycleListener listener : listeners) {
            try {
                listener.onPause();
            } catch (Exception e) {
                handleError("Error in onPause listener", e);
            }
        }

        logger.info("System paused");
    }

    /**
     * システムを再開する。
     */
    public void resume() {
        if (currentState != LifecycleState.PAUSED) {
            logger.warning("Cannot resume: current state is " + currentState);
            return;
        }

        logger.info("Resuming system...");
        changeState(LifecycleState.RESUMING);

        // 再開処理
        performResume();

        lastPauseTime = System.currentTimeMillis();
        changeState(LifecycleState.RUNNING);

        // リスナーに通知
        for (LifecycleListener listener : listeners) {
            try {
                listener.onResume();
            } catch (Exception e) {
                handleError("Error in onResume listener", e);
            }
        }

        logger.info("System resumed");
    }

    /**
     * システムを停止する。
     */
    public void stop() {
        if (currentState == LifecycleState.STOPPED ||
            currentState == LifecycleState.NOT_STARTED) {
            logger.warning("System already stopped or not started");
            return;
        }

        logger.info("Stopping system...");
        changeState(LifecycleState.STOPPING);

        // 実行時間の最終計算
        if (currentState == LifecycleState.RUNNING) {
            totalActiveTime += System.currentTimeMillis() -
                             (lastPauseTime > 0 ? lastPauseTime : startTime);
        }

        // 停止処理
        performStop();

        // リスナーに通知
        for (LifecycleListener listener : listeners) {
            try {
                listener.onStop();
            } catch (Exception e) {
                handleError("Error in onStop listener", e);
            }
        }

        changeState(LifecycleState.STOPPED);
        logger.info("System stopped. Total active time: " +
                   (totalActiveTime / 1000) + " seconds");
    }

    /**
     * 強制シャットダウンを実行する。
     * 通常のstop()と異なり、リスナーの確認をスキップする。
     */
    public void shutdown() {
        logger.warning("Performing forced shutdown");

        changeState(LifecycleState.STOPPING);

        try {
            // 最小限のクリーンアップのみ実行
            performEmergencyShutdown();
        } catch (Exception e) {
            logger.severe("Error during emergency shutdown: " + e.getMessage());
        }

        changeState(LifecycleState.STOPPED);
    }

    /**
     * システム起動処理を実行する。
     */
    private void performStart() {
        logger.fine("Performing system startup operations");
        // サービスの初期化、リソースの確保など
    }

    /**
     * システム一時停止処理を実行する。
     */
    private void performPause() {
        logger.fine("Performing pause operations");
        // アクティブなタスクの中断、状態の保存など
    }

    /**
     * システム再開処理を実行する。
     */
    private void performResume() {
        logger.fine("Performing resume operations");
        // タスクの再開、状態の復元など
    }

    /**
     * システム停止処理を実行する。
     */
    private void performStop() {
        logger.fine("Performing stop operations");
        // リソースの解放、設定の保存など
    }

    /**
     * 緊急シャットダウン処理を実行する。
     */
    private void performEmergencyShutdown() {
        logger.fine("Performing emergency shutdown");
        // 最小限のクリーンアップのみ
    }

    /**
     * 状態を変更して通知する。
     */
    private void changeState(LifecycleState newState) {
        LifecycleState oldState = currentState;
        currentState = newState;

        for (LifecycleListener listener : listeners) {
            try {
                listener.onStateChanged(oldState, newState);
            } catch (Exception e) {
                logger.warning("Error notifying state change listener: " + e.getMessage());
            }
        }
    }

    /**
     * エラーを処理する。
     */
    private void handleError(String message, Throwable error) {
        logger.severe(message + ": " + error.getMessage());

        for (LifecycleListener listener : listeners) {
            try {
                listener.onError(error);
            } catch (Exception e) {
                logger.warning("Error in onError listener: " + e.getMessage());
            }
        }
    }

    /**
     * ライフサイクルリスナーを追加する。
     *
     * @param listener リスナー
     */
    public void addLifecycleListener(LifecycleListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            logger.fine("Lifecycle listener added");
        }
    }

    /**
     * ライフサイクルリスナーを削除する。
     *
     * @param listener リスナー
     */
    public void removeLifecycleListener(LifecycleListener listener) {
        listeners.remove(listener);
    }

    /**
     * 現在のライフサイクル状態を取得する。
     *
     * @return 現在の状態
     */
    public LifecycleState getCurrentState() {
        return currentState;
    }

    /**
     * システムが実行中かを判定する。
     *
     * @return 実行中の場合 true
     */
    public boolean isRunning() {
        return currentState == LifecycleState.RUNNING;
    }

    /**
     * システムが一時停止中かを判定する。
     *
     * @return 一時停止中の場合 true
     */
    public boolean isPaused() {
        return currentState == LifecycleState.PAUSED;
    }

    /**
     * システムが停止済みかを判定する。
     *
     * @return 停止済みの場合 true
     */
    public boolean isStopped() {
        return currentState == LifecycleState.STOPPED ||
               currentState == LifecycleState.NOT_STARTED;
    }

    /**
     * システムがエラー状態かを判定する。
     *
     * @return エラー状態の場合 true
     */
    public boolean isError() {
        return currentState == LifecycleState.ERROR;
    }

    /**
     * 総実行時間を取得する（一時停止時間を除く）。
     *
     * @return 実行時間（ミリ秒）
     */
    public long getTotalActiveTime() {
        if (currentState == LifecycleState.RUNNING) {
            return totalActiveTime + (System.currentTimeMillis() -
                   (lastPauseTime > 0 ? lastPauseTime : startTime));
        }
        return totalActiveTime;
    }

    /**
     * システム起動からの経過時間を取得する。
     *
     * @return 経過時間（ミリ秒）
     */
    public long getUptime() {
        if (startTime > 0) {
            return System.currentTimeMillis() - startTime;
        }
        return 0;
    }
}