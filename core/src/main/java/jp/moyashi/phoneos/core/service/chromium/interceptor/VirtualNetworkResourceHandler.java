package jp.moyashi.phoneos.core.service.chromium.interceptor;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.NetworkAdapter;
import jp.moyashi.phoneos.core.service.network.NetworkException;
import jp.moyashi.phoneos.core.service.network.NetworkStatus;
import jp.moyashi.phoneos.core.service.network.VirtualAdapter;
import jp.moyashi.phoneos.core.service.network.VirtualSocket;
import jp.moyashi.phoneos.core.service.chromium.texture.TextureProvider;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
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
    private static final byte[] FALLBACK_TEXTURE = new byte[]{
        (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
        0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte)0xC4,
        (byte)0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
        0x54, 0x78, (byte)0x9C, 0x62, 0x00, 0x00, 0x00, 0x02,
        0x00, 0x01, (byte)0xE5, 0x27, (byte)0xDE, (byte)0xFC, 0x00, 0x00,
        0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte)0xAE, 0x42,
        0x60, (byte)0x82
    };

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
        log("processRequest() ENTER - url: " + originalUrl);
        log("Processing virtual network request: " + originalUrl);

        // 3-texture は常にクライアント側でローカル解決（サーバーへは送らない）
        if ("3-texture".equals(ipvmAddressStr)) {
            return handleTextureRequest(callback);
        }

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

        // CefRequestからメソッドとボディを取得
        String method = request.getMethod();
        if (method == null || method.isEmpty()) {
            method = "GET";
        }

        String body = null;
        if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
            org.cef.network.CefPostData postData = request.getPostData();
            if (postData != null) {
                java.util.Vector<org.cef.network.CefPostDataElement> elements = new java.util.Vector<>();
                postData.getElements(elements);
                StringBuilder bodyBuilder = new StringBuilder();
                for (org.cef.network.CefPostDataElement element : elements) {
                    int size = element.getBytesCount();
                    if (size > 0) {
                        byte[] bytes = new byte[size];
                        element.getBytes(size, bytes);
                        bodyBuilder.append(new String(bytes, StandardCharsets.UTF_8));
                    }
                }
                body = bodyBuilder.toString();
            }
        }

        log("Request method: " + method + ", body length: " + (body != null ? body.length() : 0));

        // 同期的にHTTPリクエストを送信（JCEFではprocessRequest内で完了する必要がある）
        sendHttpRequestSync(virtualAdapter, destination, method, body, callback);
        return true;
    }

    /**
     * 同期的にHTTPリクエストを送信する。
     * JCEFではprocessRequest内で同期的にレスポンスを準備する必要がある。
     */
    private void sendHttpRequestSync(VirtualAdapter virtualAdapter, IPvMAddress destination,
                                      String method, String body, CefCallback callback) {
        try {
            log("sendHttpRequestSync: starting sync request to " + destination + " method=" + method);

            CompletableFuture<VirtualSocket.VirtualHttpResponse> future =
                    virtualAdapter.httpRequest(destination, path, method, body);

            // 同期的に待機（JCEFではprocessRequest内でレスポンスを準備する必要がある）
            VirtualSocket.VirtualHttpResponse response = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            log("sendHttpRequestSync: response received, isSuccess=" + response.isSuccess());

            if (response.isSuccess()) {
                // 成功: レスポンスボディとContent-Typeを返却
                String responseBody = response.getBody();
                String contentType = response.getContentType();
                setSuccessResponse(responseBody, contentType);
                log("Received HTTP response: " + (responseBody != null ? responseBody.length() : 0) + " chars, contentType=" + contentType);
            } else {
                // エラー: エラーページを表示
                setErrorResponse(response.getStatusCode(), response.getStatusText(), response.getBody());
            }

        } catch (java.util.concurrent.TimeoutException e) {
            logError("HTTP request timeout: " + e.getMessage());
            setErrorResponse(504, "Gateway Timeout", "Request timeout: " + originalUrl);
        } catch (NetworkException e) {
            logError("Network error: " + e.getMessage());
            if (e.getErrorType() == NetworkException.ErrorType.NO_SERVICE) {
                setNoServiceResponse();
            } else {
                setErrorResponse(503, "Service Unavailable", e.getMessage());
            }
        } catch (Exception e) {
            logError("HTTP request failed: " + e.getMessage());
            setErrorResponse(500, "Internal Server Error", e.getMessage());
        }

        log("sendHttpRequestSync: calling callback.Continue()");
        callback.Continue();
        log("sendHttpRequestSync: callback.Continue() returned");
    }

    private boolean handleTextureRequest(CefCallback callback) {
        String texturePath = (path != null) ? path : "";
        if (texturePath.startsWith("/")) {
            texturePath = texturePath.substring(1);
        }

        int slash = texturePath.indexOf('/');
        if (slash <= 0 || slash >= texturePath.length() - 1) {
            logError("Invalid texture path: " + texturePath);
            setBinaryResponse(FALLBACK_TEXTURE, "image/png", 200);
            callback.Continue();
            return true;
        }

        String namespace = texturePath.substring(0, slash);
        String itemPath = texturePath.substring(slash + 1);

        TextureProvider provider = kernel.getTextureProvider();
        if (provider == null) {
            logError("TextureProvider not available for " + namespace + ":" + itemPath);
            setBinaryResponse(FALLBACK_TEXTURE, "image/png", 200);
            callback.Continue();
            return true;
        }

        try {
            byte[] textureData = provider.getItemTexture(namespace, itemPath);
            if (textureData != null && textureData.length > 0) {
                setBinaryResponse(textureData, "image/png", 200);
                log("Texture loaded: " + namespace + ":" + itemPath + " (" + textureData.length + " bytes)");
            } else {
                logError("Texture not found: " + namespace + ":" + itemPath);
                setBinaryResponse(FALLBACK_TEXTURE, "image/png", 200);
            }
        } catch (Exception e) {
            logError("Texture load failed: " + e.getMessage());
            setBinaryResponse(FALLBACK_TEXTURE, "image/png", 200);
        }

        callback.Continue();
        return true;
    }

    /**
     * 成功レスポンスを設定する。
     *
     * @param body レスポンスボディ
     * @param contentType Content-Type（nullの場合はtext/htmlを使用）
     */
    private void setSuccessResponse(String body, String contentType) {
        if (body == null || body.isEmpty()) {
            body = generateErrorPage("404 Not Found", "Page not found");
            statusCode = 404;
            mimeType = "text/html";
        } else {
            statusCode = 200;
            mimeType = (contentType != null && !contentType.isEmpty()) ? contentType : "text/html";
        }

        responseData = body.getBytes(StandardCharsets.UTF_8);
        redirectUrl = null;

        log("Set success response (length: " + responseData.length + " bytes, mimeType: " + mimeType + ")");
    }

    private void setBinaryResponse(byte[] data, String contentType, int code) {
        responseData = (data != null) ? data : new byte[0];
        statusCode = code;
        mimeType = (contentType != null && !contentType.isEmpty()) ? contentType : "application/octet-stream";
        redirectUrl = null;
    }

    /**
     * エラーレスポンスを設定する（直接HTML返却）。
     */
    private void setErrorResponse(int code, String statusText, String message) {
        statusCode = code;
        String html = generateErrorPage(code + " " + statusText, message);
        responseData = html.getBytes(StandardCharsets.UTF_8);
        mimeType = "text/html";
        redirectUrl = null;
        log("Set error response: " + code + " " + statusText);
    }

    /**
     * 圏外レスポンスを設定する（直接HTML返却）。
     */
    private void setNoServiceResponse() {
        statusCode = 503;
        String html = generateNoServicePage();
        responseData = html.getBytes(StandardCharsets.UTF_8);
        mimeType = "text/html";
        redirectUrl = null;
        log("Set no service response");
    }

    /**
     * レスポンスヘッダーを設定する。
     */
    @Override
    public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrlRef) {
        int dataLength = (responseData != null) ? responseData.length : 0;

        log("getResponseHeaders called - status: " + statusCode + ", mimeType: " + mimeType +
            ", responseData: " + dataLength + " bytes");

        response.setStatus(statusCode);
        response.setMimeType(mimeType);
        response.setStatusText(getStatusText(statusCode));

        // CORSヘッダーを個別に設定（setHeaderMapではなくsetHeaderByNameを使用）
        response.setHeaderByName("Access-Control-Allow-Origin", "*", true);
        response.setHeaderByName("Access-Control-Allow-Methods", "GET, POST, OPTIONS", true);
        response.setHeaderByName("Access-Control-Allow-Headers", "*", true);
        // Content-LengthはresponseLength.set()で設定されるため、ここでは設定しない
        
        log("Headers set via setHeaderByName: CORS");

        if (redirectUrl != null) {
            redirectUrlRef.set(redirectUrl);
            responseLength.set(0);
            log("Setting redirect URL");
        } else {
            responseLength.set(dataLength);
            log("Set responseLength to " + dataLength);
        }
    }

    /**
     * レスポンスデータを読み取る。
     */
    @Override
    public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
        log("readResponse called - bytesToRead: " + bytesToRead + ", readPosition: " + readPosition +
            ", responseData: " + (responseData != null ? responseData.length : "null") + " bytes");

        if (responseData == null || readPosition >= responseData.length) {
            log("readResponse: no more data to read");
            bytesRead.set(0);
            return false;
        }

        int remainingBytes = responseData.length - readPosition;
        int bytesToCopy = Math.min(bytesToRead, remainingBytes);

        System.arraycopy(responseData, readPosition, dataOut, 0, bytesToCopy);
        readPosition += bytesToCopy;
        bytesRead.set(bytesToCopy);

        log("readResponse: copied " + bytesToCopy + " bytes, remaining: " + (responseData.length - readPosition));

        // JCEF仕様準拠: データが読み込まれた場合(bytesRead > 0)は必ずtrueを返す。
        // falseを返すと、データが破棄されたりリクエストが失敗扱いになる場合がある。
        // 次回の呼び出しで (readPosition >= responseData.length) となり false が返される。
        return true;
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
        String safeTitle = escapeHtml(title);
        String safeMessage = escapeHtml(message);
        String safeUrl = escapeHtml(originalUrl);
        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><title>" + safeTitle +
               "</title><style>" +
               "body{font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;" +
               "text-align:center;padding:50px;background:#1a1a2e;color:#fff;margin:0;}" +
               "h1{color:#e74c3c;font-size:2em;margin-bottom:20px;}" +
               "p{color:#aaa;font-size:1.1em;}" +
               ".url{font-size:0.9em;color:#666;word-break:break-all;}" +
               "</style></head>" +
               "<body><h1>" + safeTitle + "</h1><p>" + safeMessage + "</p>" +
               "<p class='url'>URL: " + safeUrl + "</p></body></html>";
    }

    /**
     * HTMLエスケープを行う（XSS対策）。
     */
    private String escapeHtml(String input) {
        if (input == null) return "";
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
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
