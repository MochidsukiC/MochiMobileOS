package jp.moyashi.phoneos.core.service.chromium.httpm;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.NetworkAdapter;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * httpm:カスタムスキームハンドラー。
 * httpm://[IPvMAddress]/path 形式のURLを処理し、
 * VirtualAdapterを通じて仮想ネットワークからHTMLコンテンツを取得する。
 *
 * アーキテクチャ:
 * - processRequest(): URLをパースしてVirtualAdapterにHTTPリクエスト送信
 * - getResponseHeaders(): レスポンスヘッダーを設定
 * - readResponse(): レスポンスデータをストリーム出力
 * - タイムアウト: 10秒
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class HttpmSchemeHandler extends CefResourceHandlerAdapter {

    private final Kernel kernel;
    // IPvMアドレスパターン: [type]-[identifier]
    // type: 0=Player, 1=Device, 2=Server, 3=System
    // identifier: UUID形式 or 文字列ID
    private static final Pattern HTTPM_PATTERN = Pattern.compile(
        "^httpm://([0-3]-(?:" +
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}" + // 標準UUID
        "|" +
        "[a-zA-Z0-9][a-zA-Z0-9_-]*" + // 文字列ID（Server/System用）
        "))(/.*)?$"
    );

    private byte[] responseData;
    private String mimeType = "text/html";
    private int statusCode = 200;
    private int readPosition = 0;

    private static final int TIMEOUT_SECONDS = 10;

    /**
     * HttpmSchemeHandlerを構築する。
     *
     * @param kernel Kernelインスタンス
     */
    public HttpmSchemeHandler(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * リクエストを処理する。
     * httpm:URLをパースし、VirtualAdapterを通じて仮想ネットワークからHTMLを取得する。
     *
     * @param request CefRequest
     * @param callback CefCallback
     * @return 処理成功の場合true
     */
    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        String url = request.getURL();
        log("Processing request: " + url);

        // httpm:URLをパース
        Matcher matcher = HTTPM_PATTERN.matcher(url);
        if (!matcher.matches()) {
            logError("Invalid httpm URL format: " + url);
            responseData = generateErrorPage("400 Bad Request", "Invalid httpm URL format").getBytes(StandardCharsets.UTF_8);
            mimeType = "text/html";
            statusCode = 400;
            callback.Continue();
            return true;
        }

        String ipvmAddressStr = matcher.group(1);
        String path = matcher.group(2);
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        try {
            IPvMAddress destination = IPvMAddress.fromString(ipvmAddressStr);
            log("Parsed httpm URL: destination=" + destination + ", path=" + path);

            // 仮想HTTPリクエストを送信（非同期）
            sendVirtualHttpRequest(destination, path, url, callback);

            // 非同期処理中なので、ここではfalseを返さない（Continueを待つ）
            return true;

        } catch (Exception e) {
            logError("Error parsing httpm URL: " + url + " - " + e.getMessage());
            responseData = generateErrorPage("500 Internal Server Error", "Error processing httpm request: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
            mimeType = "text/html";
            statusCode = 500;
            callback.Continue();
            return true;
        }
    }

    /**
     * 仮想HTTPリクエストを送信する（非同期）。
     * VirtualAdapterを使用して仮想ネットワークにHTTPリクエストを送信する。
     */
    private void sendVirtualHttpRequest(IPvMAddress destination, String path, String originalUrl, CefCallback callback) {
        // NetworkAdapterを取得
        NetworkAdapter networkAdapter = kernel.getNetworkAdapter();
        if (networkAdapter == null) {
            logError("NetworkAdapter is not available");
            responseData = generateErrorPage("503 Service Unavailable", "Network adapter not initialized").getBytes(StandardCharsets.UTF_8);
            statusCode = 503;
            callback.Continue();
            return;
        }

        VirtualAdapter virtualAdapter = networkAdapter.getVirtualAdapter();
        if (virtualAdapter == null) {
            logError("VirtualAdapter is not available");
            responseData = generateErrorPage("503 Service Unavailable", "Virtual adapter not available").getBytes(StandardCharsets.UTF_8);
            statusCode = 503;
            callback.Continue();
            return;
        }

        log("Sending virtual HTTP request to " + destination + " path: " + path);

        try {
            // 非同期でHTTPリクエストを送信
            CompletableFuture<VirtualSocket.VirtualHttpResponse> future =
                    virtualAdapter.httpRequest(destination, path, "GET");

            // タイムアウト付きで待機
            future.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .thenAccept(response -> {
                        if (response.isSuccess()) {
                            // 成功: HTMLを設定
                            String html = response.getBody();
                            if (html != null && !html.isEmpty()) {
                                responseData = html.getBytes(StandardCharsets.UTF_8);
                                statusCode = response.getStatusCode();
                                log("Received HTTP response: " + responseData.length + " bytes, status: " + statusCode);
                            } else {
                                // HTMLが空
                                responseData = generateErrorPage("404 Not Found", "Page not found").getBytes(StandardCharsets.UTF_8);
                                statusCode = 404;
                            }
                        } else {
                            // エラー
                            responseData = generateErrorPage(
                                response.getStatusCode() + " " + response.getStatusText(),
                                response.getBody()
                            ).getBytes(StandardCharsets.UTF_8);
                            statusCode = response.getStatusCode();
                        }
                        callback.Continue();
                    })
                    .exceptionally(e -> {
                        logError("HTTP request failed: " + e.getMessage());
                        if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                            responseData = generateErrorPage("504 Gateway Timeout", "Request timeout: " + originalUrl).getBytes(StandardCharsets.UTF_8);
                            statusCode = 504;
                        } else {
                            responseData = generateErrorPage("500 Internal Server Error", e.getMessage()).getBytes(StandardCharsets.UTF_8);
                            statusCode = 500;
                        }
                        callback.Continue();
                        return null;
                    });

        } catch (Exception e) {
            logError("Error sending HTTP request: " + e.getMessage());
            responseData = generateErrorPage("500 Internal Server Error", e.getMessage()).getBytes(StandardCharsets.UTF_8);
            statusCode = 500;
            callback.Continue();
        }
    }

    /**
     * レスポンスヘッダーを設定する。
     *
     * @param response CefResponse
     * @param responseLength レスポンス長（出力）
     * @param redirectUrl リダイレクトURL（出力）
     */
    @Override
    public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
        response.setStatus(statusCode);
        response.setMimeType(mimeType);
        response.setStatusText(getStatusText(statusCode));

        if (responseData != null) {
            responseLength.set(responseData.length);
        } else {
            responseLength.set(0);
        }
    }

    /**
     * レスポンスデータを読み取る。
     *
     * @param dataOut 出力バッファ
     * @param bytesToRead 読み取るバイト数
     * @param bytesRead 実際に読み取ったバイト数（出力）
     * @param callback CefCallback
     * @return まだデータがある場合true
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
               "</title><style>body{font-family:sans-serif;text-align:center;padding:50px;}h1{color:#e74c3c;}</style></head>" +
               "<body><h1>" + title + "</h1><p>" + message + "</p></body></html>";
    }

    /**
     * ログ出力（INFO）。
     */
    private void log(String message) {
        System.out.println("[HttpmSchemeHandler] " + message);
        if (kernel.getLogger() != null) {
            kernel.getLogger().debug("HttpmSchemeHandler", message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        System.err.println("[HttpmSchemeHandler] " + message);
        if (kernel.getLogger() != null) {
            kernel.getLogger().error("HttpmSchemeHandler", message);
        }
    }
}
