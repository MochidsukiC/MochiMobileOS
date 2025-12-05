package jp.moyashi.phoneos.core.resource;

import processing.core.PImage;
import processing.core.PFont;
import processing.core.PGraphics;
import jp.moyashi.phoneos.core.memory.MemoryManager;
import jp.moyashi.phoneos.core.event.EventBus;
import jp.moyashi.phoneos.core.event.EventListener;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * リソースキャッシュシステム。
 * 画像、フォント、その他のリソースを効率的にキャッシュし、
 * メモリ使用量を最適化する。
 *
 * 特徴:
 * - LRU（Least Recently Used）キャッシュ
 * - ソフト/ウィーク参照によるメモリ管理
 * - 非同期リソース読み込み
 * - 自動キャッシュサイズ調整
 * - メモリプレッシャー対応
 *
 * @since 2025-12-04
 * @version 1.0
 */
public class ResourceCache {

    private static final Logger logger = Logger.getLogger(ResourceCache.class.getName());

    /** シングルトンインスタンス */
    private static ResourceCache instance;

    /** 画像キャッシュ（ソフト参照） */
    private final Map<String, CacheEntry<PImage>> imageCache;

    /** フォントキャッシュ（強参照 - フォントは少数で重要） */
    private final Map<String, PFont> fontCache;

    /** 汎用オブジェクトキャッシュ（ウィーク参照） */
    private final Map<String, CacheEntry<Object>> objectCache;

    /** プリロードキャッシュ（強参照 - 明示的にプリロードされたリソース） */
    private final Map<String, Object> preloadCache;

    /** キャッシュ統計 */
    private final CacheStatistics statistics = new CacheStatistics();

    /** 非同期ローダー用スレッドプール */
    private final ExecutorService loaderExecutor;

    /** キャッシュの最大サイズ（バイト） */
    private long maxCacheSize = 100 * 1024 * 1024; // 100MB

    /** 現在のキャッシュサイズ（推定） */
    private final AtomicLong currentCacheSize = new AtomicLong();

    /** アクセス順序を記録（LRU用） */
    private final LinkedHashMap<String, Long> accessOrder;

    /** クリーンアップ用スケジューラー */
    private final ScheduledExecutorService cleanupScheduler;

    /** クリーンアップタスク */
    private ScheduledFuture<?> cleanupTask;

    /** メモリマネージャー */
    private MemoryManager memoryManager;

    /** キャッシュポリシー */
    private CachePolicy policy = CachePolicy.LRU;

    /**
     * キャッシュポリシー列挙型。
     */
    public enum CachePolicy {
        /** Least Recently Used */
        LRU,
        /** Least Frequently Used */
        LFU,
        /** First In First Out */
        FIFO,
        /** 適応型（メモリ状況に応じて変更） */
        ADAPTIVE
    }

    /**
     * キャッシュエントリ。
     */
    private static class CacheEntry<T> {
        private final SoftReference<T> softRef;
        private final WeakReference<T> weakRef;
        private final long size;
        private final long timestamp;
        private final AtomicInteger accessCount;
        private final boolean useSoftReference;

        public CacheEntry(T object, long size, boolean useSoftReference) {
            this.size = size;
            this.timestamp = System.currentTimeMillis();
            this.accessCount = new AtomicInteger(0);
            this.useSoftReference = useSoftReference;

            if (useSoftReference) {
                this.softRef = new SoftReference<>(object);
                this.weakRef = null;
            } else {
                this.softRef = null;
                this.weakRef = new WeakReference<>(object);
            }
        }

        public T get() {
            accessCount.incrementAndGet();
            return useSoftReference ?
                (softRef != null ? softRef.get() : null) :
                (weakRef != null ? weakRef.get() : null);
        }

        public long getSize() { return size; }
        public int getAccessCount() { return accessCount.get(); }
        public long getAge() { return System.currentTimeMillis() - timestamp; }
    }

    /**
     * プライベートコンストラクタ。
     */
    private ResourceCache() {
        this.imageCache = new ConcurrentHashMap<>();
        this.fontCache = new ConcurrentHashMap<>();
        this.objectCache = new ConcurrentHashMap<>();
        this.preloadCache = new ConcurrentHashMap<>();
        this.accessOrder = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return false; // 手動で管理
            }
        };

        this.loaderExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "ResourceLoader");
            t.setDaemon(true);
            return t;
        });

        this.cleanupScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "CacheCleanup");
            t.setDaemon(true);
            return t;
        });

        // メモリマネージャーとの連携
        this.memoryManager = MemoryManager.getInstance();
        registerMemoryListener();

        // 定期クリーンアップを開始
        startCleanupTask();

        logger.info("ResourceCache initialized");
    }

    /**
     * シングルトンインスタンスを取得する。
     */
    public static synchronized ResourceCache getInstance() {
        if (instance == null) {
            instance = new ResourceCache();
        }
        return instance;
    }

    /**
     * メモリリスナーを登録する。
     */
    private void registerMemoryListener() {
        // メモリキャッシュクリアイベントをリッスン
        EventBus.getInstance().register(MemoryManager.MemoryClearCacheEvent.class,
            new EventListener<MemoryManager.MemoryClearCacheEvent>() {
                @Override
                public void onEvent(MemoryManager.MemoryClearCacheEvent event) {
                    clearWeakCaches();
                }
            });

        // メモリ状態変更をリッスン
        if (memoryManager != null) {
            memoryManager.addListener(new MemoryManager.MemoryListener() {
                @Override
                public void onMemoryStateChanged(MemoryManager.MemoryState oldState,
                                                MemoryManager.MemoryState newState,
                                                double usage) {
                    if (newState == MemoryManager.MemoryState.CRITICAL) {
                        // クリティカル時は積極的にキャッシュをクリア
                        clearNonEssentialCaches();
                    } else if (newState == MemoryManager.MemoryState.WARNING) {
                        // 警告時は古いキャッシュのみクリア
                        cleanupOldEntries();
                    }
                }

                @Override
                public void onMemoryLeakSuspected(String suspectClass, long instanceCount) {
                    // メモリリークの疑いがある場合、該当クラスのキャッシュをクリア
                    if (suspectClass.contains("PImage")) {
                        clearImageCache();
                    }
                }
            });
        }
    }

    /**
     * 定期クリーンアップタスクを開始する。
     */
    private void startCleanupTask() {
        cleanupTask = cleanupScheduler.scheduleWithFixedDelay(
            this::performCleanup,
            30, 30, TimeUnit.SECONDS // 30秒ごと
        );
    }

    /**
     * クリーンアップを実行する。
     */
    private void performCleanup() {
        try {
            // 無効な参照を削除
            removeInvalidEntries();

            // キャッシュサイズチェック
            if (currentCacheSize.get() > maxCacheSize) {
                evictEntries();
            }

            // 統計を更新
            statistics.cleanup();

        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during cache cleanup", e);
        }
    }

    /**
     * 画像をキャッシュから取得する。
     */
    public PImage getImage(String key) {
        // アクセス順序を記録
        synchronized (accessOrder) {
            accessOrder.put(key, System.currentTimeMillis());
        }

        CacheEntry<PImage> entry = imageCache.get(key);
        if (entry != null) {
            PImage image = entry.get();
            if (image != null) {
                statistics.hit();
                return image;
            } else {
                // 参照が無効になっている
                imageCache.remove(key);
                currentCacheSize.addAndGet(-entry.getSize());
            }
        }

        statistics.miss();
        return null;
    }

    /**
     * 画像をキャッシュに追加する。
     */
    public void putImage(String key, PImage image) {
        if (image == null) return;

        long size = estimateImageSize(image);

        // キャッシュサイズ制限チェック
        ensureCapacity(size);

        // ソフト参照でキャッシュ
        CacheEntry<PImage> entry = new CacheEntry<>(image, size, true);
        imageCache.put(key, entry);
        currentCacheSize.addAndGet(size);

        synchronized (accessOrder) {
            accessOrder.put(key, System.currentTimeMillis());
        }

        statistics.put(size);
    }

    /**
     * 画像を非同期で読み込む。
     */
    public CompletableFuture<PImage> loadImageAsync(String key, ImageLoader loader) {
        // キャッシュチェック
        PImage cached = getImage(key);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        // 非同期読み込み
        return CompletableFuture.supplyAsync(() -> {
            try {
                PImage image = loader.load();
                if (image != null) {
                    putImage(key, image);
                }
                return image;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to load image: " + key, e);
                return null;
            }
        }, loaderExecutor);
    }

    /**
     * フォントをキャッシュから取得する。
     */
    public PFont getFont(String key) {
        PFont font = fontCache.get(key);
        if (font != null) {
            statistics.hit();
        } else {
            statistics.miss();
        }
        return font;
    }

    /**
     * フォントをキャッシュに追加する。
     */
    public void putFont(String key, PFont font) {
        if (font == null) return;

        fontCache.put(key, font);
        statistics.put(1024); // フォントサイズの推定値
    }

    /**
     * 汎用オブジェクトをキャッシュに追加する。
     */
    public void putObject(String key, Object object, long size) {
        if (object == null) return;

        ensureCapacity(size);

        // ウィーク参照でキャッシュ
        CacheEntry<Object> entry = new CacheEntry<>(object, size, false);
        objectCache.put(key, entry);
        currentCacheSize.addAndGet(size);

        synchronized (accessOrder) {
            accessOrder.put(key, System.currentTimeMillis());
        }

        statistics.put(size);
    }

    /**
     * 汎用オブジェクトを取得する。
     */
    public Object getObject(String key) {
        CacheEntry<Object> entry = objectCache.get(key);
        if (entry != null) {
            Object obj = entry.get();
            if (obj != null) {
                statistics.hit();
                synchronized (accessOrder) {
                    accessOrder.put(key, System.currentTimeMillis());
                }
                return obj;
            } else {
                objectCache.remove(key);
                currentCacheSize.addAndGet(-entry.getSize());
            }
        }

        statistics.miss();
        return null;
    }

    /**
     * リソースをプリロードする。
     */
    public void preload(String key, Object resource) {
        if (resource == null) return;
        preloadCache.put(key, resource);
    }

    /**
     * プリロードされたリソースを取得する。
     */
    public Object getPreloaded(String key) {
        return preloadCache.get(key);
    }

    /**
     * 画像サイズを推定する。
     */
    private long estimateImageSize(PImage image) {
        if (image == null) return 0;
        // 幅 × 高さ × 4バイト（ARGB）
        return (long) image.width * image.height * 4;
    }

    /**
     * キャパシティを確保する。
     */
    private void ensureCapacity(long requiredSize) {
        while (currentCacheSize.get() + requiredSize > maxCacheSize) {
            if (!evictLRUEntry()) {
                break;
            }
        }
    }

    /**
     * LRUエントリを削除する。
     */
    private boolean evictLRUEntry() {
        String oldestKey = null;

        synchronized (accessOrder) {
            if (!accessOrder.isEmpty()) {
                oldestKey = accessOrder.keySet().iterator().next();
                accessOrder.remove(oldestKey);
            }
        }

        if (oldestKey != null) {
            return removeEntry(oldestKey);
        }

        return false;
    }

    /**
     * エントリを削除する。
     */
    private boolean removeEntry(String key) {
        boolean removed = false;

        // 各キャッシュから削除
        CacheEntry<PImage> imageEntry = imageCache.remove(key);
        if (imageEntry != null) {
            currentCacheSize.addAndGet(-imageEntry.getSize());
            removed = true;
        }

        CacheEntry<Object> objEntry = objectCache.remove(key);
        if (objEntry != null) {
            currentCacheSize.addAndGet(-objEntry.getSize());
            removed = true;
        }

        fontCache.remove(key);
        preloadCache.remove(key);

        if (removed) {
            statistics.evict();
        }

        return removed;
    }

    /**
     * 無効なエントリを削除する。
     */
    private void removeInvalidEntries() {
        // 画像キャッシュ
        imageCache.entrySet().removeIf(entry -> {
            CacheEntry<PImage> cache = entry.getValue();
            if (cache.get() == null) {
                currentCacheSize.addAndGet(-cache.getSize());
                statistics.evict();
                return true;
            }
            return false;
        });

        // オブジェクトキャッシュ
        objectCache.entrySet().removeIf(entry -> {
            CacheEntry<Object> cache = entry.getValue();
            if (cache.get() == null) {
                currentCacheSize.addAndGet(-cache.getSize());
                statistics.evict();
                return true;
            }
            return false;
        });
    }

    /**
     * エントリを削除する（LRU/LFU/FIFOポリシー）。
     */
    private void evictEntries() {
        int evicted = 0;
        long targetSize = maxCacheSize * 3 / 4; // 75%まで削減

        while (currentCacheSize.get() > targetSize && evicted < 10) {
            if (!evictLRUEntry()) {
                break;
            }
            evicted++;
        }

        if (evicted > 0) {
            logger.info("Evicted " + evicted + " cache entries");
        }
    }

    /**
     * 古いエントリをクリーンアップする。
     */
    private void cleanupOldEntries() {
        long maxAge = 300000; // 5分

        imageCache.entrySet().removeIf(entry -> {
            CacheEntry<PImage> cache = entry.getValue();
            if (cache.getAge() > maxAge && cache.getAccessCount() < 2) {
                currentCacheSize.addAndGet(-cache.getSize());
                statistics.evict();
                return true;
            }
            return false;
        });
    }

    /**
     * 弱参照キャッシュをクリアする。
     */
    private void clearWeakCaches() {
        objectCache.clear();
        logger.info("Weak reference caches cleared");
    }

    /**
     * 非必須キャッシュをクリアする。
     */
    private void clearNonEssentialCaches() {
        imageCache.clear();
        objectCache.clear();
        currentCacheSize.set(0);
        logger.info("Non-essential caches cleared due to memory pressure");
    }

    /**
     * 画像キャッシュをクリアする。
     */
    public void clearImageCache() {
        imageCache.clear();
        logger.info("Image cache cleared");
    }

    /**
     * すべてのキャッシュをクリアする。
     */
    public void clearAll() {
        imageCache.clear();
        fontCache.clear();
        objectCache.clear();
        preloadCache.clear();
        accessOrder.clear();
        currentCacheSize.set(0);
        statistics.reset();
        logger.info("All caches cleared");
    }

    /**
     * キャッシュ統計を取得する。
     */
    public CacheStatistics getStatistics() {
        return statistics;
    }

    /**
     * キャッシュポリシーを設定する。
     */
    public void setCachePolicy(CachePolicy policy) {
        this.policy = policy;
        logger.info("Cache policy set to: " + policy);
    }

    /**
     * 最大キャッシュサイズを設定する。
     */
    public void setMaxCacheSize(long size) {
        this.maxCacheSize = size;
        logger.info("Max cache size set to: " + size);
    }

    /**
     * シャットダウン処理。
     */
    public void shutdown() {
        if (cleanupTask != null) {
            cleanupTask.cancel(false);
        }

        cleanupScheduler.shutdown();
        loaderExecutor.shutdown();

        try {
            if (!cleanupScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupScheduler.shutdownNow();
            }
            if (!loaderExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                loaderExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupScheduler.shutdownNow();
            loaderExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        clearAll();
        logger.info("ResourceCache shutdown complete");
    }

    /**
     * 画像ローダーインターフェース。
     */
    @FunctionalInterface
    public interface ImageLoader {
        PImage load() throws Exception;
    }

    /**
     * キャッシュ統計。
     */
    public static class CacheStatistics {
        private final AtomicLong hits = new AtomicLong();
        private final AtomicLong misses = new AtomicLong();
        private final AtomicLong puts = new AtomicLong();
        private final AtomicLong evictions = new AtomicLong();
        private final AtomicLong totalSize = new AtomicLong();

        void hit() { hits.incrementAndGet(); }
        void miss() { misses.incrementAndGet(); }
        void put(long size) { puts.incrementAndGet(); totalSize.addAndGet(size); }
        void evict() { evictions.incrementAndGet(); }
        void cleanup() { /* 定期的な統計クリーンアップ */ }
        void reset() {
            hits.set(0); misses.set(0); puts.set(0);
            evictions.set(0); totalSize.set(0);
        }

        public long getHits() { return hits.get(); }
        public long getMisses() { return misses.get(); }
        public long getPuts() { return puts.get(); }
        public long getEvictions() { return evictions.get(); }
        public long getTotalSize() { return totalSize.get(); }
        public double getHitRate() {
            long h = hits.get();
            long total = h + misses.get();
            return total > 0 ? (double) h / total : 0;
        }

        @Override
        public String toString() {
            return String.format("CacheStats[hits=%d, misses=%d, rate=%.1f%%, puts=%d, evictions=%d]",
                    getHits(), getMisses(), getHitRate() * 100, getPuts(), getEvictions());
        }
    }
}