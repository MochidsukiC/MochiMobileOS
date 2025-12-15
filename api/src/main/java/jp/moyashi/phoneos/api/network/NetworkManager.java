package jp.moyashi.phoneos.api.network;

import java.util.Map;

/**
 * ネットワークマネージャーインターフェース。
 * MochiMobileOSのネットワークシステム（IPvM仮想ネットワーク + 実インターネット）への
 * アクセスを提供する。
 *
 * <p>外部MODアプリがHTTPリクエストを送信したり、仮想サーバーを登録したりできる。</p>
 *
 * <p>使用例（HTTPリクエスト）:</p>
 * <pre>{@code
 * NetworkManager network = context.getNetworkManager();
 * if (network.isAvailable()) {
 *     network.request("http://3-sys-google/search?q=test", "GET",
 *         new NetworkCallback() {
 *             @Override
 *             public void onSuccess(NetworkResponse response) {
 *                 System.out.println(response.getBody());
 *             }
 *
 *             @Override
 *             public void onError(NetworkException e) {
 *                 e.printStackTrace();
 *             }
 *         });
 * }
 * }</pre>
 *
 * <p>使用例（サーバー登録）:</p>
 * <pre>{@code
 * network.registerServer("myapp-api", request -> {
 *     if ("/hello".equals(request.getPath())) {
 *         return ServerResponse.json("{\"msg\": \"Hello!\"}");
 *     }
 *     return ServerResponse.notFound();
 * });
 * }</pre>
 */
public interface NetworkManager {

    // ========================================
    // HTTPリクエスト送信
    // ========================================

    /**
     * 非同期HTTPリクエストを送信します。
     *
     * @param url リクエストURL
     * @param method HTTPメソッド（GET, POST, PUT, DELETE等）
     * @param callback コールバック
     */
    void request(String url, String method, NetworkCallback callback);

    /**
     * 非同期HTTPリクエストを送信します（ヘッダー・ボディ指定）。
     *
     * @param url リクエストURL
     * @param method HTTPメソッド
     * @param headers リクエストヘッダー
     * @param body リクエストボディ
     * @param callback コールバック
     */
    void request(String url, String method, Map<String, String> headers,
                 String body, NetworkCallback callback);

    /**
     * 同期HTTPリクエストを送信します（ブロッキング）。
     *
     * @param url リクエストURL
     * @param method HTTPメソッド
     * @return レスポンス
     * @throws NetworkException ネットワークエラー時
     */
    NetworkResponse requestSync(String url, String method) throws NetworkException;

    /**
     * 同期HTTPリクエストを送信します（ヘッダー・ボディ指定、ブロッキング）。
     *
     * @param url リクエストURL
     * @param method HTTPメソッド
     * @param headers リクエストヘッダー
     * @param body リクエストボディ
     * @return レスポンス
     * @throws NetworkException ネットワークエラー時
     */
    NetworkResponse requestSync(String url, String method, Map<String, String> headers,
                                String body) throws NetworkException;

    // ========================================
    // ネットワーク状態
    // ========================================

    /**
     * ネットワーク接続状態を取得します。
     *
     * @return 接続状態
     */
    NetworkStatus getStatus();

    /**
     * ネットワークが利用可能かどうかを判定します。
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();

    /**
     * 電波強度を取得します。
     *
     * @return 電波強度（0-5）
     */
    int getSignalStrength();

    /**
     * キャリア名を取得します。
     * Minecraftのディメンションに基づいて決定されます。
     *
     * @return キャリア名
     */
    String getCarrierName();

    // ========================================
    // IPvMアドレス関連
    // ========================================

    /**
     * 指定したホスト名がIPvMアドレスかどうかを判定します。
     *
     * @param host ホスト名
     * @return IPvMアドレスの場合true
     */
    boolean isIPvMAddress(String host);

    /**
     * 自分（このデバイス）のIPvMアドレスを取得します。
     *
     * @return 自分のIPvMアドレス
     */
    IPvMAddress getMyAddress();

    // ========================================
    // 仮想サーバー登録（外部MODアプリ用）
    // ========================================

    /**
     * 仮想サーバーを登録します。
     * 登録したサーバーはIPvMネットワーク上で他のアプリからアクセス可能になります。
     * アドレスは "http://2-server-{serverId}/" となります。
     *
     * @param serverId サーバーID（英数字とハイフンのみ）
     * @param server サーバー実装
     * @return 登録成功した場合true
     */
    boolean registerServer(String serverId, VirtualServer server);

    /**
     * 仮想サーバーの登録を解除します。
     *
     * @param serverId サーバーID
     */
    void unregisterServer(String serverId);

    /**
     * サーバーが登録済みかどうかを確認します。
     *
     * @param serverId サーバーID
     * @return 登録済みの場合true
     */
    boolean isServerRegistered(String serverId);
}
