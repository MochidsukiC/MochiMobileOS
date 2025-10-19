package jp.moyashi.phoneos.core.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MochiMobileOS内部のログシステム。
 * VFSにログを記録し、デバッグを容易にする。
 *
 * @author MochiMobileOS
 * @version 1.0
 */
public class LoggerService {

    /** VFS参照 */
    private final VFS vfs;

    /** ログファイルパス */
    private static final String LOG_FILE = "system/logs/latest.log";

    /** アーカイブログファイルパス */
    private static final String ARCHIVE_LOG_FILE = "system/logs/archive.log";

    /** 最大ログファイルサイズ（バイト） */
    private static final int MAX_LOG_SIZE = 1024 * 1024; // 1MB

    /** メモリ内ログバッファ（最新100行） */
    private final List<String> logBuffer;

    /** メモリ内ログバッファの最大サイズ */
    private static final int MAX_BUFFER_SIZE = 100;

    /** タイムスタンプフォーマット */
    private final SimpleDateFormat dateFormat;

    /** ログレベル */
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

    /** 現在のログレベル（これ以上のレベルのみ記録） */
    private LogLevel currentLogLevel = LogLevel.DEBUG;

    /**
     * LoggerServiceを作成する。
     *
     * @param vfs VFS参照
     */
    public LoggerService(VFS vfs) {
        this.vfs = vfs;
        this.logBuffer = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss.SSS");

        // ログディレクトリを作成
        initializeLogDirectory();

        // 起動ログを記録
        info("LoggerService", "Logger service initialized");
    }

    /**
     * ログディレクトリを初期化する。
     */
    private void initializeLogDirectory() {
        try {
            // ログディレクトリが存在しない場合は作成
            if (vfs.readFile("system/logs/.keep") == null) {
                vfs.writeFile("system/logs/.keep", "");
            }
        } catch (Exception e) {
            System.err.println("Failed to initialize log directory: " + e.getMessage());
        }
    }

    /**
     * ログレベルを設定する。
     *
     * @param level ログレベル
     */
    public void setLogLevel(LogLevel level) {
        this.currentLogLevel = level;
        info("LoggerService", "Log level set to " + level);
    }

    /**
     * DEBUGレベルのログを記録する。
     *
     * @param tag ログタグ（クラス名など）
     * @param message ログメッセージ
     */
    public void debug(String tag, String message) {
        log(LogLevel.DEBUG, tag, message);
    }

    /**
     * INFOレベルのログを記録する。
     *
     * @param tag ログタグ（クラス名など）
     * @param message ログメッセージ
     */
    public void info(String tag, String message) {
        log(LogLevel.INFO, tag, message);
    }

    /**
     * WARNレベルのログを記録する。
     *
     * @param tag ログタグ（クラス名など）
     * @param message ログメッセージ
     */
    public void warn(String tag, String message) {
        log(LogLevel.WARN, tag, message);
    }

    /**
     * ERRORレベルのログを記録する。
     *
     * @param tag ログタグ（クラス名など）
     * @param message ログメッセージ
     */
    public void error(String tag, String message) {
        log(LogLevel.ERROR, tag, message);
    }

    /**
     * ERRORレベルのログを例外付きで記録する。
     *
     * @param tag ログタグ（クラス名など）
     * @param message ログメッセージ
     * @param throwable 例外
     */
    public void error(String tag, String message, Throwable throwable) {
        log(LogLevel.ERROR, tag, message + ": " + throwable.getMessage());
        // スタックトレースも記録
        for (StackTraceElement element : throwable.getStackTrace()) {
            log(LogLevel.ERROR, tag, "  at " + element.toString());
        }
    }

    /**
     * ログを記録する。
     *
     * @param level ログレベル
     * @param tag ログタグ
     * @param message ログメッセージ
     */
    private void log(LogLevel level, String tag, String message) {
        // ログレベルチェック
        if (level.ordinal() < currentLogLevel.ordinal()) {
            return;
        }

        // タイムスタンプを生成
        String timestamp = dateFormat.format(new Date());

        // ログエントリを作成
        String logEntry = "[" + timestamp + "] " + level.getPrefix() + " [" + tag + "] " + message;

        // メモリバッファに追加
        synchronized (logBuffer) {
            logBuffer.add(logEntry);
            if (logBuffer.size() > MAX_BUFFER_SIZE) {
                logBuffer.remove(0);
            }
        }

        // VFSに書き込み
        writeToFile(logEntry);

        // System.outにも出力（スタンドアロン環境用）
        System.out.println(logEntry);
    }

    /**
     * ログをファイルに書き込む。
     *
     * @param logEntry ログエントリ
     */
    private void writeToFile(String logEntry) {
        try {
            // 既存のログを読み込み
            String existingLog = vfs.readFile(LOG_FILE);
            if (existingLog == null) {
                existingLog = "";
            }

            // 新しいログを追加
            String newLog = existingLog + logEntry + "\n";

            // ファイルサイズチェック
            if (newLog.length() > MAX_LOG_SIZE) {
                // ログをアーカイブ
                archiveLog(existingLog);
                // 新しいログだけを書き込み
                vfs.writeFile(LOG_FILE, logEntry + "\n");
            } else {
                // 既存ログに追記
                vfs.writeFile(LOG_FILE, newLog);
            }
        } catch (Exception e) {
            // VFS書き込み失敗時はSystem.errに出力
            System.err.println("Failed to write log to VFS: " + e.getMessage());
        }
    }

    /**
     * ログをアーカイブする。
     *
     * @param log アーカイブするログ
     */
    private void archiveLog(String log) {
        try {
            // アーカイブファイルに追記
            String existingArchive = vfs.readFile(ARCHIVE_LOG_FILE);
            if (existingArchive == null) {
                existingArchive = "";
            }

            String archiveHeader = "\n\n=== Archived at " + dateFormat.format(new Date()) + " ===\n\n";
            vfs.writeFile(ARCHIVE_LOG_FILE, existingArchive + archiveHeader + log);
        } catch (Exception e) {
            System.err.println("Failed to archive log: " + e.getMessage());
        }
    }

    /**
     * メモリバッファ内のログを取得する。
     *
     * @return ログ行のリスト
     */
    public List<String> getRecentLogs() {
        synchronized (logBuffer) {
            return new ArrayList<>(logBuffer);
        }
    }

    /**
     * ログファイル全体を取得する。
     *
     * @return ログファイルの内容
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
     * アーカイブログを取得する。
     *
     * @return アーカイブログの内容
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
}
