package jp.moyashi.phoneos.core.service;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MochiMobileOS ログサービス
 * VFSへの非同期ログ書き込みを提供
 *
 * @author MochiMobileOS
 * @version 2.0
 */
public class LoggerService {

    /** VFS参照 */
    private final VFS vfs;

    /** ログファイルパス */
    private static final String LOG_FILE = "system/logs/latest.log";

    /** アーカイブログファイルパス */
    private static final String ARCHIVE_LOG_FILE = "system/logs/archive.log";

    /** 最大ログサイズ */
    private static final int MAX_LOG_SIZE = 1024 * 1024; // 1MB

    /** メモリ内ログバッファ（直近100件） */
    private final List<String> logBuffer;

    /** メモリ内バッファの最大サイズ */
    private static final int MAX_BUFFER_SIZE = 100;

    /** 日時フォーマッタ（スレッドセーフ）*/
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd HH:mm:ss.SSS");

    /** System.out/errキャプチャ用：元のストリーム */
    private PrintStream originalOut;
    private PrintStream originalErr;

    /** System.out/errキャプチャが有効かどうか */
    private boolean streamCaptureEnabled = false;

    // ===== 非同期I/O関連 =====

    /** 非同期書き込み用キュー */
    private final BlockingQueue<String> writeQueue;

    /** 書き込みスレッド */
    private final Thread writerThread;

    /** シャットダウンフラグ */
    private final AtomicBoolean shutdown;

    /** 書き込みバッファ（VFSへの書き込み頻度を減らすため） */
    private final StringBuilder writeBuffer;

    /** バッファフラッシュ間隔（ミリ秒） */
    private static final long FLUSH_INTERVAL_MS = 1000;

    /** バッファの最大サイズ（この値を超えたら即座にフラッシュ） */
    private static final int WRITE_BUFFER_THRESHOLD = 8192;

    /** 推定ファイルサイズ（毎回読み込まずに追跡） */
    private long estimatedFileSize = 0;

    /** サイズチェック間隔（この回数フラッシュするごとに実際のサイズを確認） */
    private static final int SIZE_CHECK_INTERVAL = 50;

    /** フラッシュカウンター */
    private int flushCount = 0;

    /**       */
    public enum LogLevel {
        DEBUG("[DEBUG]"),
        INFO("[INFO]"),
        WARN("[WARN]"),
        ERROR("[ERROR]");

        private final String prefix;

        LogLevel(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    /**          E      E        E E*/
    // Reduce default verbosity for performance
    private LogLevel currentLogLevel = LogLevel.WARN;

    public boolean isDebugEnabled() {
        return currentLogLevel == LogLevel.DEBUG;
    }

    /**
     * LoggerServiceを作成する。
     *
     * @param vfs VFS参照
     */
    public LoggerService(VFS vfs) {
        this.vfs = vfs;
        this.logBuffer = new ArrayList<>();

        // 非同期I/O初期化
        this.writeQueue = new LinkedBlockingQueue<>();
        this.shutdown = new AtomicBoolean(false);
        this.writeBuffer = new StringBuilder();

        // ログディレクトリ初期化
        initializeLogDirectory();

        // 書き込みスレッドを開始
        this.writerThread = new Thread(this::writerLoop, "LoggerService-Writer");
        this.writerThread.setDaemon(true);
        this.writerThread.start();

        // 初期化完了ログ
        info("LoggerService", "Logger service initialized (async I/O enabled)");
    }

    /**
     *    E        E     E
     */
    private void initializeLogDirectory() {
        try {
            //    E            E    E  E
            if (vfs.readFile("system/logs/.keep") == null) {
                vfs.writeFile("system/logs/.keep", "");
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize log directory: " + e.getMessage());
        }
    }

    /**
     *            E
     *
     * @param level
     */
    public void setLogLevel(LogLevel level) {
        this.currentLogLevel = level;
        info("LoggerService", "Log level set to " + level);
    }

    /**
     * DEBUG            E
     *
     * @param tag      E        E E
     * @param message     E    
     */
    public void debug(String tag, String message) {
        log(LogLevel.DEBUG, tag, message);
    }

    /**
     * INFO            E
     *
     * @param tag      E        E E
     * @param message     E    
     */
    public void info(String tag, String message) {
        log(LogLevel.INFO, tag, message);
    }

    /**
     * WARN            E
     *
     * @param tag      E        E E
     * @param message     E    
     */
    public void warn(String tag, String message) {
        log(LogLevel.WARN, tag, message);
    }

    /**
     * ERROR            E
     *
     * @param tag      E        E E
     * @param message     E    
     */
    public void error(String tag, String message) {
        log(LogLevel.ERROR, tag, message);
    }

    /**
     * ERROR                 E
     *
     * @param tag      E        E E
     * @param message     E
     * @param throwable   E
     */
    public void error(String tag, String message, Throwable throwable) {
        log(LogLevel.ERROR, tag, message + ": " + throwable.getMessage());
        //    E
        for (StackTraceElement element : throwable.getStackTrace()) {
            log(LogLevel.ERROR, tag, "  at " + element.toString());
        }
    }

    /**
     * ログレベルに関係なく直接ログを書き込む（System.outキャプチャ用）。
     * コンソールへの出力は行わない（元のストリームで既に出力済みのため）。
     *
     * 注意: 現在無効化中（パフォーマンス調査のため）
     *
     * @param tag タグ
     * @param message メッセージ
     */
    public void logDirect(String tag, String message) {
        // 一時的に無効化（フリーズ調査のため）
        // TODO: 問題解決後に再有効化
    }

    /**
     * ログを出力する。
     *
     * @param level ログレベル
     * @param tag タグ
     * @param message メッセージ
     */
    private void log(LogLevel level, String tag, String message) {
        // ログレベル判定（パフォーマンス最適化）
        if (level.ordinal() < currentLogLevel.ordinal()) {
            return;
        }

        // タイムスタンプ生成（スレッドセーフ）
        String timestamp = LocalDateTime.now().format(DATE_FORMATTER);

        // ログエントリ作成
        String logEntry = "[" + timestamp + "] " + level.getPrefix() + " [" + tag + "] " + message;

        // バッファに追加
        synchronized (logBuffer) {
            logBuffer.add(logEntry);
            if (logBuffer.size() > MAX_BUFFER_SIZE) {
                logBuffer.remove(0);
            }
        }

        // VFSに書き込み
        writeToFile(logEntry);

        // コンソールにも出力（WARNとERRORのみ）
        if (level.ordinal() >= LogLevel.WARN.ordinal()) {
            System.out.println(logEntry);
        }
    }

    /**
     * ログをファイルに書き込む（非同期）。
     * キューにエンキューして即座にリターンする。
     *
     * @param logEntry ログエントリ
     */
    private void writeToFile(String logEntry) {
        if (shutdown.get()) {
            return;
        }
        // ノンブロッキングでキューに追加
        writeQueue.offer(logEntry);
    }

    /**
     * 非同期書き込みループ。
     * バックグラウンドスレッドで実行される。
     */
    private void writerLoop() {
        long lastFlushTime = System.currentTimeMillis();

        while (!shutdown.get() || !writeQueue.isEmpty()) {
            try {
                // タイムアウト付きでキューからログエントリを取得
                String entry = writeQueue.poll(100, TimeUnit.MILLISECONDS);

                if (entry != null) {
                    writeBuffer.append(entry).append("\n");
                }

                long now = System.currentTimeMillis();
                boolean shouldFlush =
                    writeBuffer.length() >= WRITE_BUFFER_THRESHOLD ||
                    (now - lastFlushTime >= FLUSH_INTERVAL_MS && writeBuffer.length() > 0) ||
                    (shutdown.get() && writeBuffer.length() > 0);

                if (shouldFlush) {
                    flushBufferToVFS();
                    lastFlushTime = now;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 最終フラッシュ
        if (writeBuffer.length() > 0) {
            flushBufferToVFS();
        }
    }

    /**
     * バッファの内容をVFSにフラッシュする（追記モード）。
     */
    private void flushBufferToVFS() {
        if (writeBuffer.length() == 0) {
            return;
        }

        try {
            int bufferLen = writeBuffer.length();
            flushCount++;

            // 定期的に実際のファイルサイズを確認（50回に1回）
            if (flushCount % SIZE_CHECK_INTERVAL == 0) {
                String existingLog = vfs.readFile(LOG_FILE);
                estimatedFileSize = existingLog != null ? existingLog.length() : 0;
            }

            // 推定サイズでアーカイブ判定
            if (estimatedFileSize + bufferLen > MAX_LOG_SIZE) {
                // アーカイブ処理（この時だけ全文読み込み）
                String existingLog = vfs.readFile(LOG_FILE);
                if (existingLog != null && !existingLog.isEmpty()) {
                    archiveLog(existingLog);
                }
                // ファイルをクリアして新規書き込み
                vfs.writeFile(LOG_FILE, writeBuffer.toString());
                estimatedFileSize = bufferLen;
            } else {
                // 追記モードで書き込み（高速・ファイル読み込みなし）
                vfs.appendFile(LOG_FILE, writeBuffer.toString());
                estimatedFileSize += bufferLen;
            }

            // バッファをクリア
            writeBuffer.setLength(0);
        } catch (Exception e) {
            System.err.println("Failed to flush log to VFS: " + e.getMessage());
        }
    }

    /**
     *            E
     *
     * @param log          
     */
    private void archiveLog(String log) {
        try {
            //             E
            String existingArchive = vfs.readFile(ARCHIVE_LOG_FILE);
            if (existingArchive == null) {
                existingArchive = "";
            }

            String archiveHeader = "\n\n=== Archived at " + LocalDateTime.now().format(DATE_FORMATTER) + " ===\n\n";
            vfs.writeFile(ARCHIVE_LOG_FILE, existingArchive + archiveHeader + log);
        } catch (Exception e) {
            System.err.println("Failed to archive log: " + e.getMessage());
        }
    }

    /**
     *         E E        E
     *
     * @return     E   E
     */
    public List<String> getRecentLogs() {
        synchronized (logBuffer) {
            return new ArrayList<>(logBuffer);
        }
    }

    /**
     *               E
     *
     * @return         E  
     */
    public String getFullLog() {
        try {
            String log = vfs.readFile(LOG_FILE);
            return log != null ? log : "";
        } catch (Exception e) {
            return "Failed to read log file: " + e.getMessage();
        }
    }

    /**
     *              E
     *
     * @return          E  
     */
    public String getArchivedLog() {
        try {
            String log = vfs.readFile(ARCHIVE_LOG_FILE);
            return log != null ? log : "";
        } catch (Exception e) {
            return "Failed to read archived log: " + e.getMessage();
        }
    }

    /**
     * ログをクリアする。
     */
    public void clearLog() {
        try {
            vfs.writeFile(LOG_FILE, "");
            synchronized (logBuffer) {
                logBuffer.clear();
            }
            info("LoggerService", "Log cleared");
        } catch (Exception e) {
            System.err.println("Failed to clear log: " + e.getMessage());
        }
    }

    /**
     * LoggerServiceをシャットダウンする。
     * 書き込みスレッドを停止し、残りのログをフラッシュする。
     */
    public void shutdown() {
        if (shutdown.getAndSet(true)) {
            return; // 既にシャットダウン済み
        }

        info("LoggerService", "Shutting down...");

        try {
            // 書き込みスレッドの終了を待機（最大3秒）
            writerThread.join(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // スレッドがまだ生きていれば中断
        if (writerThread.isAlive()) {
            writerThread.interrupt();
        }
    }

    /**
     * 即座にバッファをフラッシュする（テスト用）。
     */
    public void flush() {
        // キューが空になるまで待機
        while (!writeQueue.isEmpty()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /**
     * System.out/System.errのキャプチャを有効化する。
     * MMOSパッケージからの出力のみをキャプチャしてログに転送する。
     * 元のコンソール出力は維持される。
     */
    public void enableSystemStreamCapture() {
        if (streamCaptureEnabled) {
            return; // 既に有効化済み
        }

        // 元のストリームを保存
        originalOut = System.out;
        originalErr = System.err;

        // キャプチャ用ストリームに置換
        System.setOut(new CapturingPrintStream(originalOut, this, false));
        System.setErr(new CapturingPrintStream(originalErr, this, true));

        streamCaptureEnabled = true;
        info("LoggerService", "System.out/err capture enabled");
    }

    /**
     * System.out/System.errのキャプチャを無効化し、元のストリームに復元する。
     */
    public void disableSystemStreamCapture() {
        if (!streamCaptureEnabled) {
            return; // 既に無効化済み
        }

        // 元のストリームに復元
        if (originalOut != null) {
            System.setOut(originalOut);
        }
        if (originalErr != null) {
            System.setErr(originalErr);
        }

        originalOut = null;
        originalErr = null;
        streamCaptureEnabled = false;
        info("LoggerService", "System.out/err capture disabled");
    }

    /**
     * System.out/errキャプチャが有効かどうかを返す。
     *
     * @return 有効な場合true
     */
    public boolean isStreamCaptureEnabled() {
        return streamCaptureEnabled;
    }
}


