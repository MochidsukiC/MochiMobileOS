package jp.moyashi.phoneos.core.service.network;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 仮想ネットワークソケットインターフェース。
 * 外部モジュール（Forge/Standalone等）がこのインターフェースを実装し、
 * Kernelに登録することで仮想ネットワーク通信が有効になる。
 *
 * <p>設計原則: 依存性逆転の原則（DIP）</p>
 * <ul>
 *   <li>Core層: このインターフェースを定義</li>
 *   <li>Forge層: ForgeVirtualSocketとして実装（Minecraft SimpleChannel使用）</li>
 *   <li>Standalone層: StandaloneVirtualSocketとして実装（ローカルルーティング）</li>
 *   <li>将来: GTAVirtualSocket, RustVirtualSocket等を追加可能（Core変更不要）</li>
 * </ul>
 *
 * <p>使用例（Forge側）:</p>
 * <pre>{@code
 * // Forge初期化時
 * VirtualSocket socket = new ForgeVirtualSocket(minecraftChannel);
 * kernel.getNetworkAdapter().getVirtualAdapter().setSocket(socket);
 * }</pre>
 *
 * @author MochiOS Team
 * @version 2.0
 */
public interface VirtualSocket {

    /**
     * ソケットが利用可能かを判定する。
     * 接続が確立されているかどうかを返す。
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();

    /**
     * 現在の接続状態を取得する。
     *
     * @return ネットワーク状態
     */
    NetworkStatus getStatus();

    /**
     * パケットを送信する。
     * 非同期で送信し、結果をCompletableFutureで返す。
     *
     * @param packet 送信するパケット
     * @return 送信結果（成功時true）
     */
    CompletableFuture<Boolean> sendPacket(VirtualPacket packet);

    /**
     * パケット受信リスナーを登録する。
     * 外部からパケットを受信した際に呼び出される。
     *
     * @param listener パケット受信リスナー
     */
    void setPacketListener(Consumer<VirtualPacket> listener);

    /**
     * HTTPリクエストを送信し、レスポンスを取得する。
     * IPvMアドレスへのHTTPリクエストを仮想ネットワーク経由で送信する。
     *
     * @param destination 宛先IPvMアドレス
     * @param path リクエストパス（例: "/index.html"）
     * @param method HTTPメソッド（GET, POST等）
     * @return HTTPレスポンス（HTML等）
     * @throws NetworkException ネットワークエラー時
     */
    CompletableFuture<VirtualHttpResponse> httpRequest(IPvMAddress destination, String path, String method)
            throws NetworkException;

    /**
     * HTTPリクエストを送信し、レスポンスを取得する（ボディ付き）。
     * IPvMアドレスへのHTTPリクエストを仮想ネットワーク経由で送信する。
     *
     * @param destination 宛先IPvMアドレス
     * @param path リクエストパス（例: "/api/purchase"）
     * @param method HTTPメソッド（GET, POST等）
     * @param body リクエストボディ（POSTデータ等）
     * @return HTTPレスポンス
     * @throws NetworkException ネットワークエラー時
     */
    CompletableFuture<VirtualHttpResponse> httpRequest(IPvMAddress destination, String path, String method, String body)
            throws NetworkException;

    /**
     * 接続を閉じる。
     * ソケットを破棄し、リソースを解放する。
     */
    void close();

    /**
     * 電波強度を取得する（0-5）。
     * Minecraftの場合、Y座標やディメンションに基づいて計算される。
     *
     * @return 電波強度（0=圏外, 5=最強）
     */
    default int getSignalStrength() {
        return isAvailable() ? 5 : 0;
    }

    /**
     * キャリア名/ネットワーク名を取得する。
     *
     * @return キャリア名（例: "Overworld Network"）
     */
    default String getCarrierName() {
        return "Virtual Network";
    }

    /**
     * 仮想HTTPレスポンスを表すクラス。
     */
    class VirtualHttpResponse {
        private final int statusCode;
        private final String statusText;
        private final String contentType;
        private final String body;

        public VirtualHttpResponse(int statusCode, String statusText, String contentType, String body) {
            this.statusCode = statusCode;
            this.statusText = statusText;
            this.contentType = contentType;
            this.body = body;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public String getStatusText() {
            return statusText;
        }

        public String getContentType() {
            return contentType;
        }

        public String getBody() {
            return body;
        }

        public boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }

        /**
         * 成功レスポンスを生成する。
         */
        public static VirtualHttpResponse ok(String body) {
            return new VirtualHttpResponse(200, "OK", "text/html", body);
        }

        /**
         * 404エラーレスポンスを生成する。
         */
        public static VirtualHttpResponse notFound(String message) {
            return new VirtualHttpResponse(404, "Not Found", "text/html", message);
        }

        /**
         * 500エラーレスポンスを生成する。
         */
        public static VirtualHttpResponse internalError(String message) {
            return new VirtualHttpResponse(500, "Internal Server Error", "text/html", message);
        }

        /**
         * 503エラーレスポンス（圏外）を生成する。
         */
        public static VirtualHttpResponse noService() {
            return new VirtualHttpResponse(503, "Service Unavailable", "text/html",
                    "<!DOCTYPE html><html><head><title>圏外</title></head>" +
                    "<body style='text-align:center;padding:50px;font-family:sans-serif;'>" +
                    "<h1>📵 圏外</h1><p>ネットワークに接続できません</p></body></html>");
        }
    }
}
