package jp.moyashi.phoneos.core.filesystem;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.VFS;
import jp.moyashi.phoneos.core.event.EventBus;
import jp.moyashi.phoneos.core.event.EventType;
import jp.moyashi.phoneos.core.event.Event;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.nio.charset.StandardCharsets;

/**
 * ファイルシステム管理を行うマネージャークラス。
 * VFSの上位層として動作し、ファイルアクセス制御、
 * キャッシュ、セキュリティ、監視機能を提供する。
 *
 * 機能:
 * - ファイルアクセスの抽象化
 * - ファイルシステム監視
 * - ファイル操作の非同期実行
 * - ストレージ使用量管理
 * - ファイルシステムイベント通知
 *
 * @since 2025-12-04
 * @version 2.0
 */
public class FileSystemManager {

    private static final Logger logger = Logger.getLogger(FileSystemManager.class.getName());

    /** Kernelインスタンス */
    private final Kernel kernel;

    /** VFSインスタンス */
    private VFS vfs;

    /** ファイルシステム監視サービス */
    private WatchService watchService;

    /** 監視スレッドプール */
    private final ExecutorService watchExecutor;

    /** 非同期操作用スレッドプール */
    private final ExecutorService asyncExecutor;

    /** ファイルキャッシュ */
    private final Map<String, CachedFile> fileCache;

    /** キャッシュサイズ上限（バイト） */
    private long maxCacheSize = 50 * 1024 * 1024; // 50MB

    /** 現在のキャッシュサイズ */
    private final AtomicLong currentCacheSize = new AtomicLong();

    /** 監視中のパス */
    private final Map<Path, WatchKey> watchedPaths = new ConcurrentHashMap<>();

    /** ファイルシステムリスナー */
    private final List<FileSystemListener> listeners = new CopyOnWriteArrayList<>();

    /** ストレージ統計 */
    private final StorageStatistics statistics = new StorageStatistics();

    /** ファイルシステム監視の有効/無効 */
    private volatile boolean watchEnabled = true;

    /** キャッシュの有効/無効 */
    private boolean cacheEnabled = true;

    /**
     * ファイルシステムリスナーインターフェース。
     */
    public interface FileSystemListener {
        /**
         * ファイルが作成された時に呼ばれる。
         */
        void onFileCreated(String path);

        /**
         * ファイルが削除された時に呼ばれる。
         */
        void onFileDeleted(String path);

        /**
         * ファイルが変更された時に呼ばれる。
         */
        void onFileModified(String path);

        /**
         * ディレクトリが作成された時に呼ばれる。
         */
        void onDirectoryCreated(String path);

        /**
         * ディレクトリが削除された時に呼ばれる。
         */
        void onDirectoryDeleted(String path);

        /**
         * ストレージ容量が少なくなった時に呼ばれる。
         */
        void onLowStorage(long availableBytes);
    }

    /**
     * FileSystemManagerを初期化する。
     *
     * @param kernel Kernelインスタンス
     */
    public FileSystemManager(Kernel kernel) {
        this.kernel = kernel;
        this.fileCache = new ConcurrentHashMap<>();
        this.watchExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "FileSystemWatcher");
            t.setDaemon(true);
            return t;
        });
        this.asyncExecutor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "FileSystemAsync");
            t.setDaemon(true);
            return t;
        });

        // VFSを取得
        if (kernel != null) {
            this.vfs = kernel.getVFS();
        }

        // ファイルシステム監視を初期化
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            startWatching();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to initialize file system watch service", e);
        }

        logger.info("FileSystemManager initialized");
    }

    /**
     * ファイルを読み込む（同期）。
     *
     * @param path ファイルパス
     * @return ファイル内容
     * @throws IOException 読み込みエラー
     */
    public String readFile(String path) throws IOException {
        // キャッシュチェック
        if (cacheEnabled) {
            CachedFile cached = fileCache.get(path);
            if (cached != null && cached.isValid()) {
                statistics.cacheHit();
                return cached.getContent();
            }
        }

        // VFSまたは通常のファイルシステムから読み込み
        String content;
        if (vfs != null && vfs.fileExists(path)) {
            content = vfs.readFile(path);
        } else {
            content = Files.readString(Paths.get(path), StandardCharsets.UTF_8);
        }

        // キャッシュに追加
        if (cacheEnabled) {
            addToCache(path, content);
        }

        statistics.fileRead(path);
        return content;
    }

    /**
     * ファイルを非同期で読み込む。
     *
     * @param path ファイルパス
     * @return CompletableFuture
     */
    public CompletableFuture<String> readFileAsync(String path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return readFile(path);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, asyncExecutor);
    }

    /**
     * ファイルに書き込む（同期）。
     *
     * @param path ファイルパス
     * @param content 書き込む内容
     * @throws IOException 書き込みエラー
     */
    public void writeFile(String path, String content) throws IOException {
        writeFile(path, content, false);
    }

    /**
     * ファイルに書き込む（同期）。
     *
     * @param path ファイルパス
     * @param content 書き込む内容
     * @param append 追記モード
     * @throws IOException 書き込みエラー
     */
    public void writeFile(String path, String content, boolean append) throws IOException {
        if (vfs != null && (vfs.fileExists(path) || isVirtualPath(path))) {
            vfs.writeFile(path, content);
        } else {
            Path filePath = Paths.get(path);
            if (append) {
                Files.writeString(filePath, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else {
                Files.writeString(filePath, content, StandardCharsets.UTF_8);
            }
        }

        // キャッシュを無効化
        invalidateCache(path);

        // イベント通知
        notifyFileModified(path);
        statistics.fileWrite(path);
    }

    /**
     * ファイルを非同期で書き込む。
     *
     * @param path ファイルパス
     * @param content 書き込む内容
     * @return CompletableFuture
     */
    public CompletableFuture<Void> writeFileAsync(String path, String content) {
        return CompletableFuture.runAsync(() -> {
            try {
                writeFile(path, content);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        }, asyncExecutor);
    }

    /**
     * ファイルを削除する。
     *
     * @param path ファイルパス
     * @return 削除成功時true
     */
    public boolean deleteFile(String path) {
        try {
            if (vfs != null && vfs.fileExists(path)) {
                vfs.deleteFile(path);
            } else {
                Files.deleteIfExists(Paths.get(path));
            }

            invalidateCache(path);
            notifyFileDeleted(path);
            statistics.fileDelete(path);
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to delete file: " + path, e);
            return false;
        }
    }

    /**
     * ディレクトリを作成する。
     *
     * @param path ディレクトリパス
     * @return 作成成功時true
     */
    public boolean createDirectory(String path) {
        try {
            if (vfs != null && isVirtualPath(path)) {
                vfs.createDirectory(path);
            } else {
                Files.createDirectories(Paths.get(path));
            }

            notifyDirectoryCreated(path);
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to create directory: " + path, e);
            return false;
        }
    }

    /**
     * ファイルまたはディレクトリの存在を確認する。
     *
     * @param path パス
     * @return 存在する場合true
     */
    public boolean exists(String path) {
        if (vfs != null && (vfs.fileExists(path) || vfs.directoryExists(path))) {
            return true;
        }
        return Files.exists(Paths.get(path));
    }

    /**
     * ディレクトリかどうかを判定する。
     *
     * @param path パス
     * @return ディレクトリの場合true
     */
    public boolean isDirectory(String path) {
        if (vfs != null && vfs.directoryExists(path)) {
            return true;
        }
        return Files.isDirectory(Paths.get(path));
    }

    /**
     * ファイルサイズを取得する。
     *
     * @param path ファイルパス
     * @return ファイルサイズ（バイト）
     */
    public long getFileSize(String path) {
        try {
            if (vfs != null && vfs.fileExists(path)) {
                // VFSファイルサイズ取得（実装が必要）
                String content = vfs.readFile(path);
                return content != null ? content.getBytes(StandardCharsets.UTF_8).length : 0;
            }
            return Files.size(Paths.get(path));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to get file size: " + path, e);
            return -1;
        }
    }

    /**
     * ディレクトリ内のファイルをリストする。
     *
     * @param path ディレクトリパス
     * @return ファイルリスト
     */
    public List<String> listFiles(String path) {
        List<String> files = new ArrayList<>();

        try {
            if (vfs != null && vfs.directoryExists(path)) {
                // VFSのリスト取得
                files.addAll(vfs.listFiles(path));
            } else {
                Path dir = Paths.get(path);
                if (Files.isDirectory(dir)) {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                        for (Path entry : stream) {
                            files.add(entry.getFileName().toString());
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to list files: " + path, e);
        }

        return files;
    }

    /**
     * ファイルをコピーする。
     *
     * @param source コピー元
     * @param destination コピー先
     * @return コピー成功時true
     */
    public boolean copyFile(String source, String destination) {
        try {
            // VFSと通常ファイルシステム間のコピーも考慮
            String content = readFile(source);
            writeFile(destination, content);
            return true;
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to copy file: " + source + " to " + destination, e);
            return false;
        }
    }

    /**
     * ファイルを移動する。
     *
     * @param source 移動元
     * @param destination 移動先
     * @return 移動成功時true
     */
    public boolean moveFile(String source, String destination) {
        try {
            if (copyFile(source, destination)) {
                return deleteFile(source);
            }
            return false;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to move file: " + source + " to " + destination, e);
            return false;
        }
    }

    /**
     * ディレクトリの監視を開始する。
     *
     * @param path 監視するディレクトリパス
     */
    public void watchDirectory(String path) {
        if (!watchEnabled) return;

        try {
            Path dir = Paths.get(path);
            if (Files.isDirectory(dir)) {
                WatchKey key = dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);
                watchedPaths.put(dir, key);
                logger.info("Started watching directory: " + path);
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to watch directory: " + path, e);
        }
    }

    /**
     * ディレクトリの監視を停止する。
     *
     * @param path 監視を停止するディレクトリパス
     */
    public void unwatchDirectory(String path) {
        Path dir = Paths.get(path);
        WatchKey key = watchedPaths.remove(dir);
        if (key != null) {
            key.cancel();
            logger.info("Stopped watching directory: " + path);
        }
    }

    /**
     * ファイルシステムの監視を開始する。
     */
    private void startWatching() {
        if (watchService == null) return;

        watchExecutor.execute(() -> {
            while (watchEnabled) {
                try {
                    WatchKey key = watchService.take();
                    Path dir = (Path) key.watchable();

                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent.Kind<?> kind = event.kind();

                        if (kind == StandardWatchEventKinds.OVERFLOW) {
                            continue;
                        }

                        Path filename = (Path) event.context();
                        Path child = dir.resolve(filename);

                        // イベント処理
                        handleWatchEvent(kind, child.toString());
                    }

                    key.reset();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error in file system watcher", e);
                }
            }
        });
    }

    /**
     * ファイルシステムイベントを処理する。
     */
    private void handleWatchEvent(WatchEvent.Kind<?> kind, String path) {
        if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
            if (Files.isDirectory(Paths.get(path))) {
                notifyDirectoryCreated(path);
            } else {
                notifyFileCreated(path);
            }
        } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
            // 削除の場合、ファイルかディレクトリか判別できないので両方通知
            notifyFileDeleted(path);
        } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
            notifyFileModified(path);
        }
    }

    /**
     * 仮想パスかどうかを判定する。
     */
    private boolean isVirtualPath(String path) {
        // VFSのルートパス以下はすべて仮想パスとみなす
        return path.startsWith("/") && !path.startsWith("/Users") && !path.startsWith("/home");
    }

    /**
     * キャッシュに追加する。
     */
    private void addToCache(String path, String content) {
        long size = content.getBytes(StandardCharsets.UTF_8).length;

        // キャッシュサイズ制限チェック
        while (currentCacheSize.get() + size > maxCacheSize && !fileCache.isEmpty()) {
            // 古いエントリを削除（LRU的な処理が必要）
            Iterator<Map.Entry<String, CachedFile>> it = fileCache.entrySet().iterator();
            if (it.hasNext()) {
                Map.Entry<String, CachedFile> entry = it.next();
                currentCacheSize.addAndGet(-entry.getValue().getSize());
                it.remove();
            }
        }

        CachedFile cached = new CachedFile(path, content);
        fileCache.put(path, cached);
        currentCacheSize.addAndGet(size);
    }

    /**
     * キャッシュを無効化する。
     */
    private void invalidateCache(String path) {
        CachedFile removed = fileCache.remove(path);
        if (removed != null) {
            currentCacheSize.addAndGet(-removed.getSize());
        }
    }

    /**
     * すべてのキャッシュをクリアする。
     */
    public void clearCache() {
        fileCache.clear();
        currentCacheSize.set(0);
        logger.info("File cache cleared");
    }

    /**
     * ストレージ情報を取得する。
     */
    public StorageInfo getStorageInfo() {
        try {
            FileStore store = Files.getFileStore(Paths.get("/"));
            long total = store.getTotalSpace();
            long available = store.getUsableSpace();
            long used = total - available;

            return new StorageInfo(total, used, available);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Failed to get storage info", e);
            return new StorageInfo(0, 0, 0);
        }
    }

    /**
     * リスナーを追加する。
     */
    public void addListener(FileSystemListener listener) {
        listeners.add(listener);
    }

    /**
     * リスナーを削除する。
     */
    public void removeListener(FileSystemListener listener) {
        listeners.remove(listener);
    }

    // イベント通知メソッド群
    private void notifyFileCreated(String path) {
        EventBus.getInstance().post(new FileSystemEvent(EventType.FILE_CREATED, path));
        for (FileSystemListener listener : listeners) {
            listener.onFileCreated(path);
        }
    }

    private void notifyFileDeleted(String path) {
        EventBus.getInstance().post(new FileSystemEvent(EventType.FILE_DELETED, path));
        for (FileSystemListener listener : listeners) {
            listener.onFileDeleted(path);
        }
    }

    private void notifyFileModified(String path) {
        EventBus.getInstance().post(new FileSystemEvent(EventType.FILE_MODIFIED, path));
        for (FileSystemListener listener : listeners) {
            listener.onFileModified(path);
        }
    }

    private void notifyDirectoryCreated(String path) {
        EventBus.getInstance().post(new FileSystemEvent(EventType.DIRECTORY_CREATED, path));
        for (FileSystemListener listener : listeners) {
            listener.onDirectoryCreated(path);
        }
    }

    private void notifyDirectoryDeleted(String path) {
        EventBus.getInstance().post(new FileSystemEvent(EventType.DIRECTORY_DELETED, path));
        for (FileSystemListener listener : listeners) {
            listener.onDirectoryDeleted(path);
        }
    }

    /**
     * シャットダウン処理。
     */
    public void shutdown() {
        watchEnabled = false;

        // 監視を停止
        watchedPaths.values().forEach(WatchKey::cancel);
        watchedPaths.clear();

        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error closing watch service", e);
            }
        }

        // スレッドプールを停止
        watchExecutor.shutdown();
        asyncExecutor.shutdown();

        try {
            if (!watchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                watchExecutor.shutdownNow();
            }
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            watchExecutor.shutdownNow();
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        logger.info("FileSystemManager shutdown complete");
    }

    /**
     * キャッシュされたファイル。
     */
    private static class CachedFile {
        private final String path;
        private final String content;
        private final long size;
        private final long timestamp;
        private final long ttl = 60000; // 1分

        public CachedFile(String path, String content) {
            this.path = path;
            this.content = content;
            this.size = content.getBytes(StandardCharsets.UTF_8).length;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isValid() {
            return System.currentTimeMillis() - timestamp < ttl;
        }

        public String getContent() { return content; }
        public long getSize() { return size; }
    }

    /**
     * ストレージ情報。
     */
    public static class StorageInfo {
        private final long totalSpace;
        private final long usedSpace;
        private final long availableSpace;

        public StorageInfo(long total, long used, long available) {
            this.totalSpace = total;
            this.usedSpace = used;
            this.availableSpace = available;
        }

        public long getTotalSpace() { return totalSpace; }
        public long getUsedSpace() { return usedSpace; }
        public long getAvailableSpace() { return availableSpace; }
        public double getUsagePercentage() {
            return totalSpace > 0 ? (double) usedSpace / totalSpace : 0;
        }
    }

    /**
     * ストレージ統計。
     */
    private static class StorageStatistics {
        private final AtomicLong totalReads = new AtomicLong();
        private final AtomicLong totalWrites = new AtomicLong();
        private final AtomicLong totalDeletes = new AtomicLong();
        private final AtomicLong cacheHits = new AtomicLong();
        private final AtomicLong cacheMisses = new AtomicLong();

        public void fileRead(String path) { totalReads.incrementAndGet(); cacheMisses.incrementAndGet(); }
        public void fileWrite(String path) { totalWrites.incrementAndGet(); }
        public void fileDelete(String path) { totalDeletes.incrementAndGet(); }
        public void cacheHit() { cacheHits.incrementAndGet(); }

        public long getTotalReads() { return totalReads.get(); }
        public long getTotalWrites() { return totalWrites.get(); }
        public long getTotalDeletes() { return totalDeletes.get(); }
        public double getCacheHitRate() {
            long hits = cacheHits.get();
            long total = hits + cacheMisses.get();
            return total > 0 ? (double) hits / total : 0;
        }
    }

    /**
     * ファイルシステムイベント。
     */
    public static class FileSystemEvent extends Event {
        private final String path;

        public FileSystemEvent(EventType type, String path) {
            super(type, path);
            this.path = path;
        }

        public String getPath() { return path; }
    }
}