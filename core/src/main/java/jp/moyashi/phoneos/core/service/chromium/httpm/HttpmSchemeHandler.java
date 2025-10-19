package jp.moyashi.phoneos.core.service.chromium.httpm;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * httpm:カスタムスキームハンドラー。
 * httpm://[IPvMAddress]/path 形式のURLを処理し、
 * VirtualRouterを通じて仮想ネットワークからHTMLコンテンツを取得する。
 *
 * アーキテクチャ:
 * - processRequest(): URLをパースしてVirtualRouterにリクエスト送信
 * - getResponseHeaders(): レスポンスヘッダーを設定
 * - readResponse(): レスポンスデータをストリーム出力
 * - タイムアウト: 10秒
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class HttpmSchemeHandler extends CefResourceHandlerAdapter {

    private final Kernel kernel;
    private final Pattern HTTPM_PATTERN = Pattern.compile("^httpm://([0-3]-[0-9a-fA-F-]+)(/.*)?$");

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
     * httpm:URLをパースし、VirtualRouterを通じて仮想ネットワークからHTMLを取得する。
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
     */
    private void sendVirtualHttpRequest(IPvMAddress destination, String path, String originalUrl, CefCallback callback) {
        if (kernel.getVirtualRouter() == null) {
            logError("VirtualRouter is not available");
            responseData = generateErrorPage("503 Service Unavailable", "Virtual network is not available").getBytes(StandardCharsets.UTF_8);
            statusCode = 503;
            callback.Continue();
            return;
        }

        log("Sending virtual HTTP request to " + destination + " path: " + path);

        // 非同期レスポンスを待つためのラッチ
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> htmlResponse = new AtomicReference<>("");
        AtomicReference<Integer> statusRef = new AtomicReference<>(200);

        // ブラウザ用のシステムアドレスを取得（System ID: 0000-0000-0000-0002）
        IPvMAddress browserSystemAddress = IPvMAddress.forSystem("0000-0000-0000-0002");

        // HTTPリクエストパケットを作成
        VirtualPacket packet = VirtualPacket.builder()
            .source(browserSystemAddress)
            .destination(destination)
            .type(VirtualPacket.PacketType.GENERIC_REQUEST)
            .put("path", path)
            .put("method", "GET")
            .put("originalUrl", originalUrl)
            .build();

        // レスポンスハンドラーを登録（一時的なハンドラー）
        kernel.getVirtualRouter().registerTypeHandler(VirtualPacket.PacketType.GENERIC_RESPONSE, (response) -> {
            // 元のリクエストURLと一致するかチェック
            if (response.getString("originalUrl") != null &&
                response.getString("originalUrl").equals(originalUrl)) {
                String html = response.getString("html");
                String status = response.getString("status");

                htmlResponse.set(html != null ? html : "");
                statusRef.set(parseStatusCode(status));

                latch.countDown();
            }
        });

        // パケットを送信
        kernel.getVirtualRouter().sendPacket(packet);

        // 別スレッドでタイムアウト待機
        new Thread(() -> {
            try {
                boolean received = latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                if (received) {
                    // レスポンスを受信
                    String html = htmlResponse.get();
                    if (html != null && !html.isEmpty()) {
                        responseData = html.getBytes(StandardCharsets.UTF_8);
                        statusCode = statusRef.get();
                        log("Received HTTP response: " + responseData.length + " bytes, status: " + statusCode);
                    } else {
                        // HTMLが空
                        responseData = generateErrorPage("404 Not Found", "Page not found").getBytes(StandardCharsets.UTF_8);
                        statusCode = 404;
                    }
                } else {
                    // タイムアウト
                    logError("HTTP request timeout: " + originalUrl);
                    responseData = generateErrorPage("504 Gateway Timeout", "Request timeout").getBytes(StandardCharsets.UTF_8);
                    statusCode = 504;
                }

                // Chromiumに処理を継続させる
                callback.Continue();

            } catch (InterruptedException e) {
                logError("HTTP request interrupted: " + e.getMessage());
                responseData = generateErrorPage("500 Internal Server Error", "Request interrupted").getBytes(StandardCharsets.UTF_8);
                statusCode = 500;
                callback.Continue();
            }
        }).start();
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
     * ステータス文字列からステータスコードをパースする。
     */
    private int parseStatusCode(String status) {
        if (status == null || status.isEmpty()) {
            return 200;
        }
        try {
            // "200 OK" -> 200
            String[] parts = status.split(" ");
            return Integer.parseInt(parts[0]);
        } catch (Exception e) {
            return 200;
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
