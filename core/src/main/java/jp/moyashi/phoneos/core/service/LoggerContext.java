package jp.moyashi.phoneos.core.service;

/**
 * グローバルなLoggerServiceへのアクセスを提供するコンテキストクラス。
 * UIコンポーネントなど、Kernelへの直接参照を持たないクラスから
 * LoggerServiceを使用できるようにする。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class LoggerContext {

    private static LoggerService globalLogger;

    /**
     * グローバルなLoggerServiceインスタンスを設定する。
     * 通常、Kernelの初期化時に一度だけ呼び出される。
     *
     * @param logger LoggerServiceインスタンス
     */
    public static void setLogger(LoggerService logger) {
        globalLogger = logger;
    }

    /**
     * グローバルなLoggerServiceインスタンスを取得する。
     *
     * @return LoggerServiceインスタンス、設定されていない場合はnull
     */
    public static LoggerService getLogger() {
        return globalLogger;
    }

    /**
     * DEBUGレベルのログを出力する（便利メソッド）。
     *
     * @param tag ログタグ
     * @param message メッセージ
     */
    public static void debug(String tag, String message) {
        if (globalLogger != null) {
            globalLogger.debug(tag, message);
        }
    }

    /**
     * INFOレベルのログを出力する（便利メソッド）。
     *
     * @param tag ログタグ
     * @param message メッセージ
     */
    public static void info(String tag, String message) {
        if (globalLogger != null) {
            globalLogger.info(tag, message);
        }
    }

    /**
     * WARNレベルのログを出力する（便利メソッド）。
     *
     * @param tag ログタグ
     * @param message メッセージ
     */
    public static void warn(String tag, String message) {
        if (globalLogger != null) {
            globalLogger.warn(tag, message);
        }
    }

    /**
     * ERRORレベルのログを出力する（便利メソッド）。
     *
     * @param tag ログタグ
     * @param message メッセージ
     */
    public static void error(String tag, String message) {
        if (globalLogger != null) {
            globalLogger.error(tag, message);
        }
    }
}
