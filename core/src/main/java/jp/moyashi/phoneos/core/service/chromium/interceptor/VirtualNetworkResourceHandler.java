package jp.moyashi.phoneos.core.service.chromium.interceptor;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.NetworkAdapter;
import jp.moyashi.phoneos.core.service.network.NetworkException;
import jp.moyashi.phoneos.core.service.network.NetworkStatus;
import jp.moyashi.phoneos.core.service.network.VirtualAdapter;
import jp.moyashi.phoneos.core.service.network.VirtualSocket;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 仮想ネットワークリソースハンドラー。
 * IPvM over HTTP アーキテクチャで使用される。
 *
 * <p>URL形式: http://[IPvMAddress]/path</p>
 * <p>例: http://3-sys-google/index.html</p>
 *
 * <p>NetworkAdapterを通じて仮想ネットワークからHTMLコンテンツを取得する。
 * レスポンスはdata: URLに変換してChromiumにリダイレクトする
 * （MCEF環境でCefResourceHandlerのレスポンスがレンダリングされない問題を回避）。</p>
 *
 * @author MochiOS Team
 * @version 3.0
 */
public class VirtualNetworkResourceHandler extends CefResourceHandlerAdapter {

    private final Kernel kernel;
    private final String ipvmAddressStr;
    private final String path;
    private final String originalUrl;

    private byte[] responseData;
    private String mimeType = "text/html";
    private int statusCode = 200;
    private int readPosition = 0;
    private String redirectUrl = null;

    private static final int TIMEOUT_SECONDS = 10;

    /**
     * VirtualNetworkResourceHandlerを構築する。
     *
     * @param kernel Kernelインスタンス
     * @param ipvmAddressStr IPvMアドレス文字列（例: "3-sys-google"）
     * @param path パス（例: "/index.html"）
     * @param originalUrl 元のURL（例: "http://3-sys-google/index.html"）
     */
    public VirtualNetworkResourceHandler(Kernel kernel, String ipvmAddressStr, String path, String originalUrl) {
        this.kernel = kernel;
        this.ipvmAddressStr = ipvmAddressStr;
        this.path = (path == null || path.isEmpty()) ? "/" : path;
        this.originalUrl = originalUrl;
    }

    /**
     * リクエストを処理する。
     * NetworkAdapterを通じて仮想ネットワークからHTMLを取得する。
     *
     * @param request CefRequest
     * @param callback CefCallback
     * @return 処理成功の場合true
     */
    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        log("Processing virtual network request: " + originalUrl);

        // NetworkAdapterの取得
        NetworkAdapter networkAdapter = kernel.getNetworkAdapter();
        if (networkAdapter == null) {
            logError("NetworkAdapter is not available");
            setErrorResponse(503, "Service Unavailable", "Network adapter not initialized");
            callback.Continue();
            return true;
        }

        VirtualAdapter virtualAdapter = networkAdapter.getVirtualAdapter();
        if (virtualAdapter == null) {
            logError("VirtualAdapter is not available");
            setErrorResponse(503, "Service Unavailable", "Virtual adapter not available");
            callback.Continue();
            return true;
        }

        // 圏外チェック
        NetworkStatus status = virtualAdapter.getStatus();
        if (status.isNoService()) {
            log("Network is in NO_SERVICE state");
            setNoServiceResponse();
            callback.Continue();
            return true;
        }

        // IPvMアドレスをパース
        IPvMAddress destination;
        try {
            destination = IPvMAddress.fromString(ipvmAddressStr);
        } catch (IllegalArgumentException e) {
            logError("Invalid IPvM address: " + ipvmAddressStr);
            setErrorResponse(400, "Bad Request", "Invalid IPvM address: " + ipvmAddressStr);
            callback.Continue();
            return true;
        }

        // 非同期でHTTPリクエストを送信
        sendHttpRequestAsync(virtualAdapter, destination, callback);
        return true;
    }

    /**
     * 非同期でHTTPリクエストを送信する。
     */
    private void sendHttpRequestAsync(VirtualAdapter virtualAdapter, IPvMAddress destination, CefCallback callback) {
        try {
            CompletableFuture<VirtualSocket.VirtualHttpResponse> future =
                    virtualAdapter.httpRequest(destination, path, "GET");

            // タイムアウト付きで待機
            future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .thenAccept(response -> {
                        if (response.isSuccess()) {
                            // 成功: レスポンスをdata: URLに変換してリダイレクト
                            String html = response.getBody();
                            setDataUrlRedirect(html);
                            log("Received HTTP response: " + (html != null ? html.length() : 0) + " chars");
                        } else {
                            // エラー: エラーページを表示
                            setErrorResponse(response.getStatusCode(), response.getStatusText(), response.getBody());
                        }
                        callback.Continue();
                    })
                    .exceptionally(e -> {
                        logError("HTTP request failed: " + e.getMessage());
                        if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                            setErrorResponse(504, "Gateway Timeout", "Request timeout: " + originalUrl);
                        } else {
                            setErrorResponse(500, "Internal Server Error", e.getMessage());
                        }
                        callback.Continue();
                        return null;
                    });

        } catch (NetworkException e) {
            logError("Network error: " + e.getMessage());
            if (e.getErrorType() == NetworkException.ErrorType.NO_SERVICE) {
                setNoServiceResponse();
            } else {
                setErrorResponse(503, "Service Unavailable", e.getMessage());
            }
            callback.Continue();
        }
    }

    /**
     * data: URLリダイレクトを設定する。
     */
    private void setDataUrlRedirect(String html) {
        if (html == null || html.isEmpty()) {
            html = generateErrorPage("404 Not Found", "Page not found");
            statusCode = 404;
        } else {
            statusCode = 302; // Redirect
        }

        String base64Html = java.util.Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
        redirectUrl = "data:text/html;charset=utf-8;base64," + base64Html;
        mimeType = "text/html";
        responseData = new byte[0];

        log("Set data: URL redirect (base64 length: " + base64Html.length() + ")");
    }

    /**
     * エラーレスポンスを設定する。
     */
    private void setErrorResponse(int code, String statusText, String message) {
        statusCode = 302; // Redirect to error page
        String html = generateErrorPage(code + " " + statusText, message);
        String base64Html = java.util.Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
        redirectUrl = "data:text/html;charset=utf-8;base64," + base64Html;
        mimeType = "text/html";
        responseData = new byte[0];
    }

    /**
     * 圏外レスポンスを設定する。
     */
    private void setNoServiceResponse() {
        statusCode = 302; // Redirect to no service page
        String html = generateNoServicePage();
        String base64Html = java.util.Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
        redirectUrl = "data:text/html;charset=utf-8;base64," + base64Html;
        mimeType = "text/html";
        responseData = new byte[0];
    }

    /**
     * レスポンスヘッダーを設定する。
     */
    @Override
    public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrlRef) {
        log("getResponseHeaders called - status: " + statusCode + ", redirectUrl: " + (redirectUrl != null ? "set" : "null"));
        response.setStatus(statusCode);
        response.setMimeType(mimeType);
        response.setStatusText(getStatusText(statusCode));

        if (redirectUrl != null) {
            redirectUrlRef.set(redirectUrl);
            responseLength.set(0);
            log("Setting redirect URL");
        } else if (responseData != null) {
            responseLength.set(responseData.length);
        } else {
            responseLength.set(0);
        }
    }

    /**
     * レスポンスデータを読み取る。
     */
    @Override
    public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
        if (responseData == null || readPosition >= responseData.length) {
            bytesRead.set(0);
            return false;
        }

        int remainingBytes = responseData.length - readPosition;
        int bytesToCopy = Math.min(bytesToRead, remainingBytes);

        System.arraycopy(responseData, readPosition, dataOut, 0, bytesToCopy);
        readPosition += bytesToCopy;
        bytesRead.set(bytesToCopy);

        return readPosition < responseData.length;
    }

    /**
     * ステータスコードからステータステキストを取得する。
     */
    private String getStatusText(int statusCode) {
        switch (statusCode) {
            case 200: return "OK";
            case 302: return "Found";
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 500: return "Internal Server Error";
            case 503: return "Service Unavailable";
            case 504: return "Gateway Timeout";
            default: return "Unknown";
        }
    }

    /**
     * エラーページHTMLを生成する。
     */
    private String generateErrorPage(String title, String message) {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + title +
               "</title><style>" +
               "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;" +
               "text-align:center;padding:50px;background:#1a1a2e;color:#fff;margin:0;}" +
               "h1{color:#e74c3c;font-size:2em;margin-bottom:20px;}" +
               "p{color:#aaa;font-size:1.1em;}" +
               ".url{font-size:0.9em;color:#666;word-break:break-all;}" +
               "</style></head>" +
               "<body><h1>" + title + "</h1><p>" + message + "</p>" +
               "<p class='url'>URL: " + originalUrl + "</p></body></html>";
    }

    /**
     * 圏外ページHTMLを生成する。
     */
    private String generateNoServicePage() {
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>圏外</title>" +
               "<style>" +
               "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;" +
               "text-align:center;padding:80px 20px;background:#1a1a2e;color:#fff;margin:0;}" +
               "h1{font-size:4em;margin-bottom:20px;}" +
               "h2{color:#e74c3c;font-size:1.5em;margin-bottom:30px;}" +
               "p{color:#aaa;font-size:1em;line-height:1.6;}" +
               ".hint{margin-top:30px;padding:20px;background:rgba(255,255,255,0.05);border-radius:10px;}" +
               "</style></head>" +
               "<body>" +
               "<h1>📵</h1>" +
               "<h2>圏外</h2>" +
               "<p>仮想ネットワークに接続できません</p>" +
               "<div class='hint'>" +
               "<p><strong>ヒント:</strong></p>" +
               "<p>• サーバーに接続していることを確認してください</p>" +
               "<p>• 地下深くにいる場合は地上に移動してください</p>" +
               "</div>" +
               "</body></html>";
    }

    /**
     * ログ出力（INFO）。
     */
    private void log(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().debug("VirtualNetworkResourceHandler", message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("VirtualNetworkResourceHandler", message);
        }
    }
}
