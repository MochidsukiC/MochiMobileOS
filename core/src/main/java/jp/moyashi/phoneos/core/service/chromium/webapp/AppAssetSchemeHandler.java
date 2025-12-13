package jp.moyashi.phoneos.core.service.chromium.webapp;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.callback.CefCallback;
import org.cef.handler.CefResourceHandlerAdapter;
import org.cef.misc.IntRef;
import org.cef.misc.StringRef;
import org.cef.network.CefRequest;
import org.cef.network.CefResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * mochiapp:// カスタムスキームハンドラー。
 * JAR内のリソースファイル（HTML/CSS/JS/画像等）をChromiumに提供する。
 *
 * <p>URL形式:</p>
 * <ul>
 *   <li>{@code mochiapp://modid/ui/index.html} → JAR内 {@code /assets/modid/ui/index.html}</li>
 *   <li>{@code mochiapp://modid/styles/main.css} → JAR内 {@code /assets/modid/styles/main.css}</li>
 * </ul>
 *
 * <p>後方互換性:</p>
 * <ul>
 *   <li>{@code app-mymod://path} 形式もサポート（事前登録済みの場合）</li>
 * </ul>
 *
 * <p>アーキテクチャ:</p>
 * <ul>
 *   <li>{@link #processRequest}: URLをパースしてJAR内リソースを読み込む</li>
 *   <li>{@link #getResponseHeaders}: MIMEタイプとステータスを設定</li>
 *   <li>{@link #readResponse}: データをストリーム出力</li>
 * </ul>
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class AppAssetSchemeHandler extends CefResourceHandlerAdapter {

    private final Kernel kernel;
    private final String schemeName;

    // URL解析パターン: mochiapp://modid/path または app-{modid}://path
    private static final Pattern MOCHIOS_APP_PATTERN = Pattern.compile("^mochiapp://([a-z0-9_-]+)/(.*)$");
    private static final Pattern LEGACY_APP_PATTERN = Pattern.compile("^(app-[a-z0-9_-]+)://(.*)$");

    private byte[] responseData;
    private String mimeType = "application/octet-stream";
    private int statusCode = 200;
    private int readPosition = 0;

    /**
     * AppAssetSchemeHandlerを構築する。
     *
     * @param kernel Kernelインスタンス
     * @param schemeName スキーム名（"app-modid" 形式）
     */
    public AppAssetSchemeHandler(Kernel kernel, String schemeName) {
        this.kernel = kernel;
        this.schemeName = schemeName;
    }

    @Override
    public boolean processRequest(CefRequest request, CefCallback callback) {
        String url = request.getURL();
        log("Processing request: " + url);

        String modId;
        String path;

        // URLをパース（mochios-app://modid/path 形式を優先）
        Matcher mochiosMatcher = MOCHIOS_APP_PATTERN.matcher(url);
        if (mochiosMatcher.matches()) {
            modId = mochiosMatcher.group(1);
            path = mochiosMatcher.group(2);
        } else {
            // レガシー形式: app-modid://path
            Matcher legacyMatcher = LEGACY_APP_PATTERN.matcher(url);
            if (legacyMatcher.matches()) {
                String scheme = legacyMatcher.group(1);
                // app-modid -> modid を抽出
                modId = scheme.substring(4); // "app-" を除去
                path = legacyMatcher.group(2);
            } else {
                logError("Invalid URL format: " + url);
                responseData = generateErrorPage("400 Bad Request", "Invalid URL format").getBytes(StandardCharsets.UTF_8);
                mimeType = "text/html; charset=utf-8";
                statusCode = 400;
                callback.Continue();
                return true;
            }
        }

        // クエリパラメータを除去
        int queryIndex = path.indexOf('?');
        if (queryIndex >= 0) {
            path = path.substring(0, queryIndex);
        }

        // フラグメントを除去
        int fragmentIndex = path.indexOf('#');
        if (fragmentIndex >= 0) {
            path = path.substring(0, fragmentIndex);
        }

        // 空パスの場合はindex.htmlを試す
        if (path.isEmpty() || path.equals("/")) {
            path = "index.html";
        }

        log("ModId: " + modId + ", Path: " + path);

        // リソースを直接読み込み（複数ClassLoaderフォールバック）
        InputStream inputStream = loadResource(modId, path);

        if (inputStream == null) {
            logError("Resource not found: mochiapp://" + modId + "/" + path);
            responseData = generateErrorPage("404 Not Found", "Resource not found: " + modId + "/" + path).getBytes(StandardCharsets.UTF_8);
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
            mimeType = AppSchemeManager.getMimeType(path);
            statusCode = 200;

            log("Resource loaded: " + responseData.length + " bytes, MIME: " + mimeType);

        } catch (Exception e) {
            logError("Error reading resource: " + e.getMessage());
            responseData = generateErrorPage("500 Internal Server Error", "Error reading resource: " + e.getMessage()).getBytes(StandardCharsets.UTF_8);
            mimeType = "text/html; charset=utf-8";
            statusCode = 500;
        }

        callback.Continue();
        return true;
    }

    @Override
    public void getResponseHeaders(CefResponse response, IntRef responseLength, StringRef redirectUrl) {
        response.setStatus(statusCode);
        response.setMimeType(mimeType);
        response.setStatusText(getStatusText(statusCode));

        // CORSヘッダーを設定（同一オリジンポリシー回避）
        // 注: CefResponseにはsetHeader/setCustomHeaderがないため、
        // 必要に応じてCefResponseHeaderHandlerを使用するか、
        // onBeforeResourceLoadでリクエストを変更する

        if (responseData != null) {
            responseLength.set(responseData.length);
        } else {
            responseLength.set(0);
        }
    }

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
               "<p class=\"code\">Scheme: " + schemeName + "</p>\n" +
               "</body></html>";
    }

    /**
     * ログ出力（INFO）。
     */
    private void log(String message) {
        System.out.println("[AppAssetSchemeHandler:" + schemeName + "] " + message);
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().debug("AppAssetSchemeHandler:" + schemeName, message);
        }
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message) {
        System.err.println("[AppAssetSchemeHandler:" + schemeName + "] " + message);
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("AppAssetSchemeHandler:" + schemeName, message);
        }
    }

    /**
     * リソースを読み込む（複数ClassLoaderフォールバック）。
     *
     * @param modId Mod ID
     * @param path リソースパス
     * @return InputStreamまたはnull
     */
    private InputStream loadResource(String modId, String path) {
        // パスを正規化: assets/{modId}/{path}
        String resourcePath = "assets/" + modId + "/" + path;
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

        // AppSchemeManagerに登録されているClassLoaderからも試す
        AppSchemeManager manager = AppSchemeManager.getInstance();
        AppSchemeManager.SchemeInfo info = manager.getSchemeInfo("app-" + modId);
        if (info != null) {
            stream = info.getResource(path);
            if (stream != null) {
                log("Found via AppSchemeManager: " + path);
                return stream;
            }
        }

        // 見つからない
        logError("Resource not found in any ClassLoader: " + resourcePath);
        return null;
    }
}
