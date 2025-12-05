package jp.moyashi.phoneos.core.memory;

import jp.moyashi.phoneos.core.event.EventBus;
import jp.moyashi.phoneos.core.event.Event;
import jp.moyashi.phoneos.core.event.EventType;

import java.lang.management.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Collections;
import javax.management.MBeanServer;
import java.lang.ref.WeakReference;

/**
 * メモリ管理を行うマネージャークラス。
 * アプリケーションのメモリ使用状況の監視、最適化、
 * ガベージコレクション制御を行う。
 *
 * 機能:
 * - メモリ使用量監視
 * - メモリリーク検出
 * - メモリプール管理
 * - GC戦略の最適化
 * - メモリプレッシャー通知
 *
 * @since 2025-12-04
 * @version 2.0
 */
public class MemoryManager {

    private static final Logger logger = Logger.getLogger(MemoryManager.class.getName());

    /** シングルトンインスタンス */
    private static MemoryManager instance;

    /** メモリMXBean */
    private final MemoryMXBean memoryBean;

    /** ランタイム */
    private final Runtime runtime;

    /** メモリプールMXBeans */
    private final List<MemoryPoolMXBean> memoryPools;

    /** GC MXBeans */
    private final List<GarbageCollectorMXBean> gcBeans;

    /** メモリ監視スレッドプール */
    private final ScheduledExecutorService monitorExecutor;

    /** メモリ監視リスナー */
    private final List<MemoryListener> listeners = new CopyOnWriteArrayList<>();

    /** メモリ使用統計 */
    private final MemoryStatistics statistics = new MemoryStatistics();

    /** メモリリークサスペクト（弱参照で保持） */
    private final Map<String, WeakReference<Object>> leakSuspects = new ConcurrentHashMap<>();

    /** メモリしきい値（警告レベル）*/
    private double warningThreshold = 0.75; // 75%

    /** メモリしきい値（クリティカルレベル）*/
    private double criticalThreshold = 0.90; // 90%

    /** 監視間隔（ミリ秒） */
    private long monitorInterval = 5000; // 5秒

    /** 監視タスクFuture */
    private ScheduledFuture<?> monitorTask;

    /** 現在のメモリ状態 */
    private MemoryState currentState = MemoryState.NORMAL;

    /**
     * メモリ状態の列挙型。
     */
    public enum MemoryState {
        /** 正常 */
        NORMAL,
        /** 警告レベル */
        WARNING,
        /** クリティカル */
        CRITICAL,
        /** メモリ不足 */
        OUT_OF_MEMORY
    }

    /**
     * メモリリスナーインターフェース。
     */
    public interface MemoryListener {
        /**
         * メモリ状態が変更された時に呼ばれる。
         * @param oldState 前の状態
         * @param newState 新しい状態
         * @param usage 現在の使用率（0.0-1.0）
         */
        void onMemoryStateChanged(MemoryState oldState, MemoryState newState, double usage);

        /**
         * メモリリークの疑いが検出された時に呼ばれる。
         * @param suspectClass リークの疑いのあるクラス名
         * @param instanceCount インスタンス数
         */
        void onMemoryLeakSuspected(String suspectClass, long instanceCount);
    }

    /**
     * プライベートコンストラクタ。
     */
    private MemoryManager() {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.runtime = Runtime.getRuntime();
        this.memoryPools = ManagementFactory.getMemoryPoolMXBeans();
        this.gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        this.monitorExecutor = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "MemoryMonitor");
            t.setDaemon(true);
            return t;
        });

        logger.info("MemoryManager initialized");
        logMemoryInfo();
    }

    /**
     * シングルトンインスタンスを取得する。
     * @return MemoryManagerインスタンス
     */
    public static synchronized MemoryManager getInstance() {
        if (instance == null) {
            instance = new MemoryManager();
        }
        return instance;
    }

    /**
     * メモリ監視を開始する。
     */
    public void startMonitoring() {
        if (monitorTask != null && !monitorTask.isDone()) {
            logger.warning("Memory monitoring is already running");
            return;
        }

        monitorTask = monitorExecutor.scheduleAtFixedRate(this::monitorMemory,
                0, monitorInterval, TimeUnit.MILLISECONDS);

        logger.info("Memory monitoring started with interval: " + monitorInterval + "ms");
    }

    /**
     * メモリ監視を停止する。
     */
    public void stopMonitoring() {
        if (monitorTask != null) {
            monitorTask.cancel(false);
            logger.info("Memory monitoring stopped");
        }
    }

    /**
     * メモリを監視する（定期実行）。
     */
    private void monitorMemory() {
        try {
            double usage = getMemoryUsage();
            statistics.update(usage);

            MemoryState newState = determineMemoryState(usage);

            if (newState != currentState) {
                MemoryState oldState = currentState;
                currentState = newState;
                notifyStateChanged(oldState, newState, usage);

                // イベントバスに通知
                MemoryEvent event = new MemoryEvent(newState, usage);
                EventBus.getInstance().post(event);
            }

            // メモリプレッシャーが高い場合は対策を実行
            if (usage > warningThreshold) {
                handleMemoryPressure(usage);
            }

            // メモリリークチェック（1分に1回程度）
            if (statistics.getSampleCount() % 12 == 0) {
                checkForMemoryLeaks();
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error in memory monitoring", e);
        }
    }

    /**
     * メモリ状態を判定する。
     * @param usage メモリ使用率
     * @return メモリ状態
     */
    private MemoryState determineMemoryState(double usage) {
        if (usage >= criticalThreshold) {
            return MemoryState.CRITICAL;
        } else if (usage >= warningThreshold) {
            return MemoryState.WARNING;
        } else {
            return MemoryState.NORMAL;
        }
    }

    /**
     * メモリプレッシャーを処理する。
     * @param usage 現在の使用率
     */
    private void handleMemoryPressure(double usage) {
        logger.warning("Memory pressure detected: " + String.format("%.1f%%", usage * 100));

        // 段階的な対応
        if (usage > criticalThreshold) {
            // クリティカル: 積極的なGC
            performFullGC();
            clearCaches();
        } else if (usage > warningThreshold) {
            // 警告: 軽量なGC
            System.gc();
        }
    }

    /**
     * フルGCを実行する。
     */
    public void performFullGC() {
        logger.info("Performing full garbage collection");
        long before = getUsedMemory();
        System.gc();
        System.runFinalization();
        System.gc();
        long after = getUsedMemory();
        long freed = before - after;

        if (freed > 0) {
            logger.info(String.format("GC freed %s", formatBytes(freed)));
        }
    }

    /**
     * キャッシュをクリアする。
     */
    private void clearCaches() {
        // イベントバスに通知して、各コンポーネントのキャッシュをクリア
        EventBus.getInstance().post(new MemoryClearCacheEvent());
        logger.info("Cache clear requested");
    }

    /**
     * メモリリークをチェックする。
     */
    private void checkForMemoryLeaks() {
        // 簡易的なリーク検出（実際には高度な分析が必要）
        long heapSize = getHeapSize();
        long usedMemory = getUsedMemory();

        // 継続的にメモリが増加している場合、リークの疑い
        if (statistics.isIncreasingTrend() && getMemoryUsage() > 0.5) {
            logger.warning("Potential memory leak detected - continuous memory growth");

            // リスナーに通知
            for (MemoryListener listener : listeners) {
                listener.onMemoryLeakSuspected("Unknown", -1);
            }
        }
    }

    /**
     * 現在のメモリ使用率を取得する。
     * @return メモリ使用率（0.0-1.0）
     */
    public double getMemoryUsage() {
        long total = getTotalMemory();
        long used = getUsedMemory();
        return (double) used / total;
    }

    /**
     * 使用中のメモリ量を取得する。
     * @return 使用中のメモリ（バイト）
     */
    public long getUsedMemory() {
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * 総メモリ量を取得する。
     * @return 総メモリ（バイト）
     */
    public long getTotalMemory() {
        return runtime.totalMemory();
    }

    /**
     * 最大メモリ量を取得する。
     * @return 最大メモリ（バイト）
     */
    public long getMaxMemory() {
        return runtime.maxMemory();
    }

    /**
     * 利用可能なメモリ量を取得する。
     * @return 利用可能メモリ（バイト）
     */
    public long getAvailableMemory() {
        long max = runtime.maxMemory();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        return max - (total - free);
    }

    /**
     * ヒープサイズを取得する。
     * @return ヒープサイズ（バイト）
     */
    public long getHeapSize() {
        return memoryBean.getHeapMemoryUsage().getUsed();
    }

    /**
     * 非ヒープサイズを取得する。
     * @return 非ヒープサイズ（バイト）
     */
    public long getNonHeapSize() {
        return memoryBean.getNonHeapMemoryUsage().getUsed();
    }

    /**
     * GC統計を取得する。
     * @return GC統計情報
     */
    public String getGCStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("Garbage Collection Statistics:\n");

        for (GarbageCollectorMXBean gc : gcBeans) {
            sb.append(String.format("  %s: %d collections, %dms total time%n",
                    gc.getName(),
                    gc.getCollectionCount(),
                    gc.getCollectionTime()));
        }

        return sb.toString();
    }

    /**
     * メモリプール情報を取得する。
     * @return メモリプール情報
     */
    public String getMemoryPoolInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Memory Pool Information:\n");

        for (MemoryPoolMXBean pool : memoryPools) {
            MemoryUsage usage = pool.getUsage();
            sb.append(String.format("  %s (%s): %s / %s (%.1f%%)%n",
                    pool.getName(),
                    pool.getType(),
                    formatBytes(usage.getUsed()),
                    formatBytes(usage.getMax()),
                    (usage.getMax() > 0 ? (double) usage.getUsed() / usage.getMax() * 100 : 0)));
        }

        return sb.toString();
    }

    /**
     * メモリ情報をログ出力する。
     */
    public void logMemoryInfo() {
        logger.info("Memory Status:");
        logger.info(String.format("  Heap: %s / %s (%.1f%%)",
                formatBytes(getHeapSize()),
                formatBytes(getMaxMemory()),
                getMemoryUsage() * 100));
        logger.info(String.format("  Non-Heap: %s",
                formatBytes(getNonHeapSize())));
        logger.info("  " + getGCStatistics().replace("\n", "\n  "));
    }

    /**
     * バイト数をフォーマットする。
     * @param bytes バイト数
     * @return フォーマット済み文字列
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";

        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;

        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }

        return String.format("%.1f %s", size, units[unitIndex]);
    }

    /**
     * リスナーを登録する。
     * @param listener メモリリスナー
     */
    public void addListener(MemoryListener listener) {
        listeners.add(listener);
    }

    /**
     * リスナーを登録解除する。
     * @param listener メモリリスナー
     */
    public void removeListener(MemoryListener listener) {
        listeners.remove(listener);
    }

    /**
     * 状態変更を通知する。
     */
    private void notifyStateChanged(MemoryState oldState, MemoryState newState, double usage) {
        logger.info(String.format("Memory state changed: %s -> %s (%.1f%%)",
                oldState, newState, usage * 100));

        for (MemoryListener listener : listeners) {
            try {
                listener.onMemoryStateChanged(oldState, newState, usage);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error notifying memory listener", e);
            }
        }
    }

    /**
     * しきい値を設定する。
     * @param warning 警告しきい値（0.0-1.0）
     * @param critical クリティカルしきい値（0.0-1.0）
     */
    public void setThresholds(double warning, double critical) {
        if (warning < 0 || warning > 1 || critical < 0 || critical > 1 || warning > critical) {
            throw new IllegalArgumentException("Invalid thresholds");
        }
        this.warningThreshold = warning;
        this.criticalThreshold = critical;
        logger.info(String.format("Memory thresholds set: warning=%.0f%%, critical=%.0f%%",
                warning * 100, critical * 100));
    }

    /**
     * 監視間隔を設定する。
     * @param interval 監視間隔（ミリ秒）
     */
    public void setMonitorInterval(long interval) {
        if (interval < 1000) {
            throw new IllegalArgumentException("Monitor interval must be at least 1000ms");
        }

        this.monitorInterval = interval;

        // 監視中の場合は再開
        if (monitorTask != null && !monitorTask.isDone()) {
            stopMonitoring();
            startMonitoring();
        }
    }

    /**
     * 現在のメモリ状態を取得する。
     * @return メモリ状態
     */
    public MemoryState getCurrentState() {
        return currentState;
    }

    /**
     * メモリ統計を取得する。
     * @return メモリ統計
     */
    public MemoryStatistics getStatistics() {
        return statistics;
    }

    /**
     * シャットダウン処理。
     */
    public void shutdown() {
        stopMonitoring();
        monitorExecutor.shutdown();
        try {
            if (!monitorExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                monitorExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            monitorExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("MemoryManager shutdown complete");
    }

    /**
     * メモリ統計クラス。
     */
    public static class MemoryStatistics {
        private final List<Double> usageHistory = Collections.synchronizedList(new ArrayList<>());
        private final AtomicLong sampleCount = new AtomicLong();
        private double avgUsage = 0;
        private double maxUsage = 0;
        private double minUsage = 1.0;
        private static final int MAX_HISTORY = 100;

        public void update(double usage) {
            usageHistory.add(usage);
            if (usageHistory.size() > MAX_HISTORY) {
                usageHistory.remove(0);
            }

            sampleCount.incrementAndGet();

            // 統計更新
            maxUsage = Math.max(maxUsage, usage);
            minUsage = Math.min(minUsage, usage);

            // 平均計算
            double sum = 0;
            for (double u : usageHistory) {
                sum += u;
            }
            avgUsage = sum / usageHistory.size();
        }

        public boolean isIncreasingTrend() {
            if (usageHistory.size() < 10) return false;

            // 簡単な傾向分析（最後の10サンプル）
            double sumFirst = 0, sumLast = 0;
            int n = Math.min(10, usageHistory.size());

            for (int i = 0; i < n / 2; i++) {
                sumFirst += usageHistory.get(usageHistory.size() - n + i);
                sumLast += usageHistory.get(usageHistory.size() - n / 2 + i);
            }

            return sumLast > sumFirst * 1.1; // 10%以上の増加
        }

        public long getSampleCount() { return sampleCount.get(); }
        public double getAverage() { return avgUsage; }
        public double getMax() { return maxUsage; }
        public double getMin() { return minUsage; }
        public List<Double> getHistory() { return new ArrayList<>(usageHistory); }
    }

    /**
     * メモリイベント。
     */
    public static class MemoryEvent extends Event {
        private final MemoryState state;
        private final double usage;

        public MemoryEvent(MemoryState state, double usage) {
            super(EventType.CUSTOM, MemoryManager.getInstance());
            this.state = state;
            this.usage = usage;
            setData("memoryState", state);
            setData("memoryUsage", usage);
        }

        public MemoryState getMemoryState() { return state; }
        public double getUsage() { return usage; }
    }

    /**
     * キャッシュクリアイベント。
     */
    public static class MemoryClearCacheEvent extends Event {
        public MemoryClearCacheEvent() {
            super(EventType.CUSTOM, MemoryManager.getInstance());
            setData("action", "clearCache");
        }
    }
}