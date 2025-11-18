package jp.moyashi.phoneos.core.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * MochiMobileOS E        E   E
 * VFS                   E
 *
 * @author MochiMobileOS
 * @version 1.0
 */
public class LoggerService {

    /** VFS  E */
    private final VFS vfs;

    /**          */
    private static final String LOG_FILE = "system/logs/latest.log";

    /**               */
    private static final String ARCHIVE_LOG_FILE = "system/logs/archive.log";

    /**             E     E*/
    private static final int MAX_LOG_SIZE = 1024 * 1024; // 1MB

    /**     E        E   100  E*/
    private final List<String> logBuffer;

    /**     E              */
    private static final int MAX_BUFFER_SIZE = 100;

    /**              E*/
    private final SimpleDateFormat dateFormat;

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
    // Reduce default verbosity for performance\n    private LogLevel currentLogLevel = LogLevel.WARN;\n\n    public boolean isDebugEnabled() {\n        return currentLogLevel == LogLevel.DEBUG;\n    }

    /**
     * LoggerService   E   E
     *
     * @param vfs VFS  E
     */
    public LoggerService(VFS vfs) {
        this.vfs = vfs;
        this.logBuffer = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("MM/dd HH:mm:ss.SSS");

        //    E         E
        initializeLogDirectory();

        //        
        info("LoggerService", "Logger service initialized");
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
        //this.currentLogLevel = level;
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
     *         E
     *
     * @param level      
     * @param tag     
     * @param message     E    
     */
    private void log(LogLevel level, String tag, String message) {
        //         E
        /*
        if (level.ordinal() < currentLogLevel.ordinal()) {
            return;
        }

         */

        //           E
        String timestamp = dateFormat.format(new Date());

        //          E
        String logEntry = "[" + timestamp + "] " + level.getPrefix() + " [" + tag + "] " + message;

        //           
        synchronized (logBuffer) {
            logBuffer.add(logEntry);
            if (logBuffer.size() > MAX_BUFFER_SIZE) {
                logBuffer.remove(0);
            }
        }

        // VFS     
        writeToFile(logEntry);

        // System.out   E           E   E E
        System.out.println(logEntry);
    }

    /**
     *              E
     *
     * @param logEntry       
     */
    private void writeToFile(String logEntry) {
        try {
            //    E       
            String existingLog = vfs.readFile(LOG_FILE);
            if (existingLog == null) {
                existingLog = "";
            }

            //         
            String newLog = existingLog + logEntry + "\n";

            //           E  
            if (newLog.length() > MAX_LOG_SIZE) {
                //         E
                archiveLog(existingLog);
                //             
                vfs.writeFile(LOG_FILE, logEntry + "\n");
            } else {
                //        E
                vfs.writeFile(LOG_FILE, newLog);
            }
        } catch (Exception e) {
            // VFS        System.err   E
            System.err.println("Failed to write log to VFS: " + e.getMessage());
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

            String archiveHeader = "\n\n=== Archived at " + dateFormat.format(new Date()) + " ===\n\n";
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
     *          E
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


