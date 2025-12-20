package jp.moyashi.phoneos.server.network;

/**
 * 仮想HTTPサーバーのインターフェース。
 * SystemServerやExternalServerがこれを実装して、
 * IPvMアドレスへのHTTPリクエストを処理する。
 *
 * 使用例:
 * - 3-sys-test: テストページを提供するシステムサーバー
 * - 3-sys-google: Minecraftワールド内の検索サービス
 * - 2-economy: 外部Modが提供する経済サービス
 */
public interface VirtualHttpServer {

    /**
     * サーバーの識別子を取得する。
     * この値がIPvMアドレスの識別子部分になる。
     * 例: "sys-test", "sys-google", "economy"
     *
     * @return サーバー識別子
     */
    String getServerId();

    /**
     * HTTPリクエストを処理する。
     *
     * @param request リクエスト情報
     * @return レスポンス
     */
    VirtualHttpResponse handleRequest(VirtualHttpRequest request);

    /**
     * サーバーの説明を取得する（デバッグ用）。
     *
     * @return サーバーの説明
     */
    default String getDescription() {
        return "VirtualHttpServer: " + getServerId();
    }
}
