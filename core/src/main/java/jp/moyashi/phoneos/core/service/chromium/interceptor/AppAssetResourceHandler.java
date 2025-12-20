package jp.moyashi.phoneos.core.service.chromium.interceptor;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.webapp.AppSchemeManager;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * アプリアセットリソースハンドラー。
 * IPvM over HTTP アーキテクチャで使用される。
 *
 * URL形式: http://app.local/modid/path
 * 例: http://app.local/sample-app/ui/index.html
 * → JAR内: /assets/sample-app/ui/index.html
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class AppAssetResourceHandler extends CefResourceHandlerAdapter {

    private final Kernel kernel;
    private final String path;

    private byte[] responseData;
    private String mimeType = "application/octet-stream";
    private int statusCode = 200;
    private int readPosition = 0;

    /**
     * AppAssetResourceHandlerを構築する。
     *
     * @param kernel Kernelインスタンス
     * @param path パス（例: "/sample-app/ui/index.html"）
     */
    public AppAssetResourceHandler(Kernel kernel, String path) {
        this.kernel = kernel;
        this.path = path;
    }

    /**
     * リクエストを処理する。
     * JAR内のリソースを読み込む。
     *
     * @param request CefRequest
     * @param callback CefCallback
     * @return 処理成功の場合true
     */
    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        String url = request.getURL();
        log("Processing app asset request: " + url);

        // パスを解析: /modid/subpath
        String cleanPath = path;
        if (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        // クエリパラメータを除去
        int queryIndex = cleanPath.indexOf('?');
        if (queryIndex >= 0) {
            cleanPath = cleanPath.substring(0, queryIndex);
        }

        // フラグメントを除去
        int fragmentIndex = cleanPath.indexOf('#');
        if (fragmentIndex >= 0) {
            cleanPath = cleanPath.substring(0, fragmentIndex);
        }

        // modidとsubpathを分離
        int firstSlash = cleanPath.indexOf('/');
        String modId;
        String subPath;

        if (firstSlash > 0) {
            modId = cleanPath.substring(0, firstSlash);
            subPath = cleanPath.substring(firstSlash + 1);
        } else {
            // パスがmodidのみの場合
            modId = cleanPath;
            subPath = "index.html";
        }

        if (subPath.isEmpty()) {
            subPath = "index.html";
        }

        log("ModId: " + modId + ", SubPath: " + subPath);

        // リソースを読み込み
        InputStream inputStream = loadResource(modId, subPath);

        if (inputStream == null) {
            logError("Resource not found: app.local/" + modId + "/" + subPath);
            responseData = generateErrorPage("404 Not Found", "Resource not found: " + modId + "/" + subPath)
                    .getBytes(StandardCharsets.UTF_8);
            mimeType = "text/html; charset=utf-8";
            statusCode = 404;
            callback.Continue();
            return true;
        }

        try {
            // InputStreamをbyte[]に変換
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(chunk)) != -1) {
                buffer.write(chunk, 0, bytesRead);
            }
            inputStream.close();

            responseData = buffer.toByteArray();
            mimeType = getMimeType(subPath);
            statusCode = 200;

            log("Resource loaded: " + responseData.length + " bytes, MIME: " + mimeType);

        } catch (Exception e) {
            logError("Error reading resource: " + e.getMessage());
            responseData = generateErrorPage("500 Internal Server Error", "Error reading resource: " + e.getMessage())
                    .getBytes(StandardCharsets.UTF_8);
            mimeType = "text/html; charset=utf-8";
            statusCode = 500;
        }

        callback.Continue();
        return true;
    }

    /**
     * レスポンスヘッダーを設定する。
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
     * リソースを読み込む（複数ClassLoaderフォールバック）。
     *
     * @param modId Mod ID
     * @param subPath サブパス
     * @return InputStreamまたはnull
     */
    private InputStream loadResource(String modId, String subPath) {
        // パスを正規化: assets/{modId}/{subPath}
        String resourcePath = "assets/" + modId + "/" + subPath;
        log("Trying to load resource: " + resourcePath);

        // 1. このクラスのClassLoaderから試す
        InputStream stream = getClass().getResourceAsStream("/" + resourcePath);
        if (stream != null) {
            log("Found in class ClassLoader: /" + resourcePath);
            return stream;
        }

        // 2. コンテキストClassLoaderから試す
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        if (contextClassLoader != null) {
            stream = contextClassLoader.getResourceAsStream(resourcePath);
            if (stream != null) {
                log("Found in context ClassLoader: " + resourcePath);
                return stream;
            }
        }

        // 3. システムClassLoaderから試す
        stream = ClassLoader.getSystemResourceAsStream(resourcePath);
        if (stream != null) {
            log("Found in system ClassLoader: " + resourcePath);
            return stream;
        }

        // 4. AppSchemeManagerに登録されているClassLoaderからも試す
        AppSchemeManager manager = AppSchemeManager.getInstance();
        AppSchemeManager.SchemeInfo info = manager.getSchemeInfo("app-" + modId);
        if (info != null) {
            stream = info.getResource(subPath);
            if (stream != null) {
                log("Found via AppSchemeManager: " + subPath);
                return stream;
            }
        }

        // 見つからない
        logError("Resource not found in any ClassLoader: " + resourcePath);
        return null;
    }

    /**
     * ファイル拡張子からMIMEタイプを取得する。
     */
    private String getMimeType(String path) {
        // AppSchemeManagerに委譲
        return AppSchemeManager.getMimeType(path);
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
            default: return "Unknown";
        }
    }

    /**
     * エラーページHTMLを生成する。
     */
    private String generateErrorPage(String title, String message) {
        return "<!DOCTYPE html>\n" +
               "<html><head>\n" +
               "<meta charset=\"UTF-8\">\n" +
               "<title>" + title + "</title>\n" +
               "<style>\n" +
               "body { font-family: sans-serif; text-align: center; padding: 50px; background: #1a1a1a; color: #fff; }\n" +
               "h1 { color: #e74c3c; }\n" +
               ".code { background: #2a2a2a; padding: 10px; border-radius: 5px; display: inline-block; }\n" +
               "</style>\n" +
               "</head><body>\n" +
               "<h1>" + title + "</h1>\n" +
               "<p>" + message + "</p>\n" +
               "<p class=\"code\">Path: " + path + "</p>\n" +
               "</body></html>";
    }

    /**
     * ログ出力（INFO）。
     */
    private void log(String message) {
        System.out.println("[AppAssetResourceHandler] " + message);
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().debug("AppAssetResourceHandler", message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        System.err.println("[AppAssetResourceHandler] " + message);
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("AppAssetResourceHandler", message);
        }
    }
}
