package jp.moyashi.phoneos.api.network;

/**
 * 仮想サーバーインターフェース。
 * 外部MODアプリがIPvMネットワーク上にサーバーを公開するために実装する。
 *
 * <p>使用例:</p>
 * <pre>{@code
 * NetworkManager network = context.getNetworkManager();
 * network.registerServer("myapp-api", new VirtualServer() {
 *     @Override
 *     public ServerResponse handleRequest(ServerRequest request) {
 *         if ("/api/hello".equals(request.getPath())) {
 *             return ServerResponse.json("{\"message\": \"Hello!\"}");
 *         }
 *         return ServerResponse.notFound();
 *     }
 * });
 * }</pre>
 */
public interface VirtualServer {

    /**
     * HTTPリクエストを処理します。
     *
     * @param request リクエスト情報
     * @return レスポンス
     */
    ServerResponse handleRequest(ServerRequest request);

    /**
     * サーバーが利用可能かどうかを返します。
     * 利用不可の場合、リクエストは503 Service Unavailableとして処理されます。
     *
     * @return 利用可能な場合true
     */
    default boolean isAvailable() {
        return true;
    }

    /**
     * サーバーの説明を取得します（デバッグ・ログ用）。
     *
     * @return サーバーの説明
     */
    default String getDescription() {
        return "Virtual Server";
    }
}
