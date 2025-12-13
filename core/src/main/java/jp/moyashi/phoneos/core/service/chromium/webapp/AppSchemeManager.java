package jp.moyashi.phoneos.core.service.chromium.webapp;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebApp用カスタムスキーム管理クラス。
 * app-{modid}:// 形式のスキームを登録・管理し、JAR内リソースへのアクセスを提供する。
 *
 * <p>スキーム形式:</p>
 * <ul>
 *   <li>{@code app-mymod://ui/index.html} → JAR内 {@code /assets/mymod/ui/index.html}</li>
 *   <li>重複時は連番追加: mymod → mymod-2 → mymod-3 ...</li>
 * </ul>
 *
 * <p>使用例:</p>
 * <pre>
 * AppSchemeManager manager = AppSchemeManager.getInstance();
 * String scheme = manager.registerScheme("mymod", classLoader);
 * // scheme = "app-mymod" or "app-mymod-2" if duplicate
 * </pre>
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class AppSchemeManager {

    private static final AppSchemeManager INSTANCE = new AppSchemeManager();

    /**
     * 登録済みスキーム情報
     */
    private final Map<String, SchemeInfo> registeredSchemes = new ConcurrentHashMap<>();

    /**
     * ModIDと登録済みスキーム数のマッピング（重複検出用）
     */
    private final Map<String, Integer> modIdCounts = new ConcurrentHashMap<>();

    /**
     * シングルトンインスタンスを取得する。
     *
     * @return AppSchemeManagerインスタンス
     */
    public static AppSchemeManager getInstance() {
        return INSTANCE;
    }

    private AppSchemeManager() {
        // シングルトン
    }

    /**
     * スキームを登録する。
     *
     * @param modId Mod ID
     * @param classLoader リソース読み込み用ClassLoader
     * @return 登録されたスキーム名（"app-modid" 形式）
     */
    public synchronized String registerScheme(String modId, ClassLoader classLoader) {
        String schemeName = generateSchemeName(modId);

        SchemeInfo info = new SchemeInfo(modId, schemeName, classLoader);
        registeredSchemes.put(schemeName, info);

        System.out.println("[AppSchemeManager] Registered scheme: " + schemeName + " for modId: " + modId);
        return schemeName;
    }

    /**
     * スキーム名を生成する（重複時は連番追加）。
     */
    private String generateSchemeName(String modId) {
        String baseName = "app-" + modId.toLowerCase();

        // 重複チェック
        int count = modIdCounts.getOrDefault(modId.toLowerCase(), 0);

        if (count == 0) {
            // 初回登録
            modIdCounts.put(modId.toLowerCase(), 1);
            return baseName;
        } else {
            // 重複 - 連番追加
            count++;
            modIdCounts.put(modId.toLowerCase(), count);
            return baseName + "-" + count;
        }
    }

    /**
     * スキーム名からリソースを取得する。
     *
     * @param schemeName スキーム名（"app-modid" 形式）
     * @param path リソースパス
     * @return InputStreamまたはnull
     */
    public InputStream getResource(String schemeName, String path) {
        SchemeInfo info = registeredSchemes.get(schemeName);
        if (info == null) {
            System.err.println("[AppSchemeManager] Unknown scheme: " + schemeName);
            return null;
        }

        return info.getResource(path);
    }

    /**
     * スキーム情報を取得する。
     *
     * @param schemeName スキーム名
     * @return SchemeInfoまたはnull
     */
    public SchemeInfo getSchemeInfo(String schemeName) {
        return registeredSchemes.get(schemeName);
    }

    /**
     * スキームが登録済みかチェックする。
     *
     * @param schemeName スキーム名
     * @return 登録済みの場合true
     */
    public boolean isRegistered(String schemeName) {
        return registeredSchemes.containsKey(schemeName);
    }

    /**
     * 全登録スキームをクリアする（テスト用）。
     */
    public synchronized void clear() {
        registeredSchemes.clear();
        modIdCounts.clear();
    }

    /**
     * パスからMIMEタイプを判定する。
     *
     * @param path ファイルパス
     * @return MIMEタイプ
     */
    public static String getMimeType(String path) {
        if (path == null || path.isEmpty()) {
            return "application/octet-stream";
        }

        String lowerPath = path.toLowerCase();

        // HTML
        if (lowerPath.endsWith(".html") || lowerPath.endsWith(".htm")) {
            return "text/html; charset=utf-8";
        }

        // CSS
        if (lowerPath.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }

        // JavaScript
        if (lowerPath.endsWith(".js") || lowerPath.endsWith(".mjs")) {
            return "application/javascript; charset=utf-8";
        }

        // JSON
        if (lowerPath.endsWith(".json")) {
            return "application/json; charset=utf-8";
        }

        // Images
        if (lowerPath.endsWith(".png")) {
            return "image/png";
        }
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerPath.endsWith(".gif")) {
            return "image/gif";
        }
        if (lowerPath.endsWith(".svg")) {
            return "image/svg+xml";
        }
        if (lowerPath.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerPath.endsWith(".ico")) {
            return "image/x-icon";
        }

        // Fonts
        if (lowerPath.endsWith(".woff")) {
            return "font/woff";
        }
        if (lowerPath.endsWith(".woff2")) {
            return "font/woff2";
        }
        if (lowerPath.endsWith(".ttf")) {
            return "font/ttf";
        }
        if (lowerPath.endsWith(".otf")) {
            return "font/otf";
        }
        if (lowerPath.endsWith(".eot")) {
            return "application/vnd.ms-fontobject";
        }

        // Text
        if (lowerPath.endsWith(".txt")) {
            return "text/plain; charset=utf-8";
        }
        if (lowerPath.endsWith(".xml")) {
            return "application/xml; charset=utf-8";
        }

        // Default
        return "application/octet-stream";
    }

    /**
     * スキーム情報を保持するクラス。
     */
    public static class SchemeInfo {
        private final String modId;
        private final String schemeName;
        private final ClassLoader classLoader;

        public SchemeInfo(String modId, String schemeName, ClassLoader classLoader) {
            this.modId = modId;
            this.schemeName = schemeName;
            this.classLoader = classLoader;
        }

        public String getModId() {
            return modId;
        }

        public String getSchemeName() {
            return schemeName;
        }

        public ClassLoader getClassLoader() {
            return classLoader;
        }

        /**
         * リソースを取得する（複数ClassLoaderフォールバック）。
         *
         * @param path リソースパス
         * @return InputStreamまたはnull
         */
        public InputStream getResource(String path) {
            // パスを正規化
            String resourcePath = normalizeResourcePath(path);

            System.out.println("[SchemeInfo:" + modId + "] Loading resource: " + resourcePath);

            // 1. 登録されたClassLoaderから試す
            InputStream stream = tryLoadResource(classLoader, resourcePath);
            if (stream != null) {
                System.out.println("[SchemeInfo:" + modId + "] Found in registered ClassLoader");
                return stream;
            }

            // 2. コンテキストClassLoaderから試す
            ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
            if (contextClassLoader != null && contextClassLoader != classLoader) {
                stream = tryLoadResource(contextClassLoader, resourcePath);
                if (stream != null) {
                    System.out.println("[SchemeInfo:" + modId + "] Found in context ClassLoader");
                    return stream;
                }
            }

            // 3. システムClassLoaderから試す
            stream = tryLoadResource(ClassLoader.getSystemClassLoader(), resourcePath);
            if (stream != null) {
                System.out.println("[SchemeInfo:" + modId + "] Found in system ClassLoader");
                return stream;
            }

            System.err.println("[SchemeInfo:" + modId + "] Resource not found: " + resourcePath);
            return null;
        }

        /**
         * ClassLoaderからリソースを読み込む。
         */
        private InputStream tryLoadResource(ClassLoader loader, String path) {
            if (loader == null) return null;

            // /で始まる場合と始まらない場合の両方を試す
            InputStream stream = loader.getResourceAsStream(path);
            if (stream == null && path.startsWith("/")) {
                stream = loader.getResourceAsStream(path.substring(1));
            }
            if (stream == null && !path.startsWith("/")) {
                stream = loader.getResourceAsStream("/" + path);
            }

            return stream;
        }

        /**
         * リソースパスを正規化する。
         * パスを "assets/{modId}/{path}" 形式に変換。
         */
        private String normalizeResourcePath(String path) {
            // 先頭の/を除去
            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            // assets/modId/ 形式に変換
            return "assets/" + modId + "/" + path;
        }
    }
}
