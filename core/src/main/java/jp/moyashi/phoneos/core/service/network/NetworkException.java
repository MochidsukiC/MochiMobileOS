package jp.moyashi.phoneos.core.service.network;

/**
 * ネットワーク関連の例外。
 * 圏外、タイムアウト、接続エラー等を表現する。
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class NetworkException extends Exception {

    /**
     * エラー種別
     */
    public enum ErrorType {
        /**
         * 圏外 - ソケットが設定されていない
         */
        NO_SERVICE,

        /**
         * タイムアウト - 応答がない
         */
        TIMEOUT,

        /**
         * 接続拒否 - 宛先が接続を拒否
         */
        CONNECTION_REFUSED,

        /**
         * 宛先不明 - IPvMアドレスが解決できない
         */
        UNKNOWN_HOST,

        /**
         * プロトコルエラー - パケット形式が不正
         */
        PROTOCOL_ERROR,

        /**
         * 内部エラー - 予期しないエラー
         */
        INTERNAL_ERROR
    }

    private final ErrorType errorType;
    private final NetworkStatus networkStatus;

    /**
     * NetworkExceptionを構築する。
     *
     * @param message エラーメッセージ
     * @param errorType エラー種別
     */
    public NetworkException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
        this.networkStatus = errorType == ErrorType.NO_SERVICE ? NetworkStatus.NO_SERVICE : NetworkStatus.ERROR;
    }

    /**
     * NetworkExceptionを構築する（原因例外付き）。
     *
     * @param message エラーメッセージ
     * @param errorType エラー種別
     * @param cause 原因例外
     */
    public NetworkException(String message, ErrorType errorType, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.networkStatus = errorType == ErrorType.NO_SERVICE ? NetworkStatus.NO_SERVICE : NetworkStatus.ERROR;
    }

    /**
     * エラー種別を取得する。
     *
     * @return エラー種別
     */
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * ネットワーク状態を取得する。
     *
     * @return ネットワーク状態
     */
    public NetworkStatus getNetworkStatus() {
        return networkStatus;
    }

    /**
     * 圏外エラーを生成する。
     *
     * @return 圏外エラー
     */
    public static NetworkException noService() {
        return new NetworkException("No service available - network socket not configured", ErrorType.NO_SERVICE);
    }

    /**
     * タイムアウトエラーを生成する。
     *
     * @param timeoutMs タイムアウト時間（ミリ秒）
     * @return タイムアウトエラー
     */
    public static NetworkException timeout(long timeoutMs) {
        return new NetworkException("Request timed out after " + timeoutMs + "ms", ErrorType.TIMEOUT);
    }

    /**
     * 宛先不明エラーを生成する。
     *
     * @param address 宛先アドレス
     * @return 宛先不明エラー
     */
    public static NetworkException unknownHost(String address) {
        return new NetworkException("Unknown host: " + address, ErrorType.UNKNOWN_HOST);
    }
}
