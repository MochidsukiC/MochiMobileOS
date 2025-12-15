package jp.moyashi.phoneos.api.network;

/**
 * ネットワークリクエストの非同期コールバックインターフェース。
 */
public interface NetworkCallback {

    /**
     * リクエスト成功時に呼ばれます。
     *
     * @param response レスポンス
     */
    void onSuccess(NetworkResponse response);

    /**
     * リクエスト失敗時に呼ばれます。
     *
     * @param exception 例外
     */
    void onError(NetworkException exception);
}
