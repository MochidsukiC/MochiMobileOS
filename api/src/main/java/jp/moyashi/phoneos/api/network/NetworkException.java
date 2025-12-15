package jp.moyashi.phoneos.api.network;

/**
 * ネットワーク例外。
 * ネットワークリクエスト中に発生したエラーを表す。
 */
public class NetworkException extends Exception {

    /**
     * エラーコード。
     */
    public enum ErrorCode {
        /** 圏外（電波なし） */
        NO_SERVICE,
        /** タイムアウト */
        TIMEOUT,
        /** 接続拒否 */
        CONNECTION_REFUSED,
        /** ホストが見つからない */
        HOST_NOT_FOUND,
        /** 無効なURL */
        INVALID_URL,
        /** 不明なエラー */
        UNKNOWN
    }

    private final ErrorCode errorCode;
    private final String url;

    /**
     * NetworkExceptionを構築します。
     *
     * @param errorCode エラーコード
     * @param message エラーメッセージ
     */
    public NetworkException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.url = null;
    }

    /**
     * NetworkExceptionを構築します。
     *
     * @param errorCode エラーコード
     * @param message エラーメッセージ
     * @param url リクエストURL
     */
    public NetworkException(ErrorCode errorCode, String message, String url) {
        super(message);
        this.errorCode = errorCode;
        this.url = url;
    }

    /**
     * NetworkExceptionを構築します。
     *
     * @param errorCode エラーコード
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     */
    public NetworkException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.url = null;
    }

    /**
     * NetworkExceptionを構築します。
     *
     * @param errorCode エラーコード
     * @param message エラーメッセージ
     * @param url リクエストURL
     * @param cause 原因となった例外
     */
    public NetworkException(ErrorCode errorCode, String message, String url, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.url = url;
    }

    /**
     * エラーコードを取得します。
     *
     * @return エラーコード
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * リクエストURLを取得します。
     *
     * @return リクエストURL（不明な場合はnull）
     */
    public String getUrl() {
        return url;
    }

    // ========================================
    // ファクトリメソッド
    // ========================================

    /**
     * 圏外例外を生成します。
     *
     * @return NetworkException
     */
    public static NetworkException noService() {
        return new NetworkException(ErrorCode.NO_SERVICE, "No network service available (out of range)");
    }

    /**
     * タイムアウト例外を生成します。
     *
     * @param url リクエストURL
     * @return NetworkException
     */
    public static NetworkException timeout(String url) {
        return new NetworkException(ErrorCode.TIMEOUT, "Request timed out", url);
    }

    /**
     * 接続拒否例外を生成します。
     *
     * @param url リクエストURL
     * @return NetworkException
     */
    public static NetworkException connectionRefused(String url) {
        return new NetworkException(ErrorCode.CONNECTION_REFUSED, "Connection refused", url);
    }

    /**
     * ホスト未発見例外を生成します。
     *
     * @param host ホスト名
     * @return NetworkException
     */
    public static NetworkException hostNotFound(String host) {
        return new NetworkException(ErrorCode.HOST_NOT_FOUND, "Host not found: " + host);
    }

    /**
     * 無効なURL例外を生成します。
     *
     * @param url 無効なURL
     * @return NetworkException
     */
    public static NetworkException invalidUrl(String url) {
        return new NetworkException(ErrorCode.INVALID_URL, "Invalid URL: " + url, url);
    }

    /**
     * 不明なエラー例外を生成します。
     *
     * @param message エラーメッセージ
     * @return NetworkException
     */
    public static NetworkException unknown(String message) {
        return new NetworkException(ErrorCode.UNKNOWN, message);
    }

    /**
     * 不明なエラー例外を生成します。
     *
     * @param message エラーメッセージ
     * @param cause 原因となった例外
     * @return NetworkException
     */
    public static NetworkException unknown(String message, Throwable cause) {
        return new NetworkException(ErrorCode.UNKNOWN, message, cause);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("NetworkException{");
        sb.append("errorCode=").append(errorCode);
        if (url != null) {
            sb.append(", url='").append(url).append("'");
        }
        sb.append(", message='").append(getMessage()).append("'");
        sb.append("}");
        return sb.toString();
    }
}
