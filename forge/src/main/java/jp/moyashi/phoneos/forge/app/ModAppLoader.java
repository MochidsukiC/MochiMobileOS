package jp.moyashi.phoneos.forge.app;

import jp.moyashi.phoneos.api.IModApplication;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * MODアプリケーションローダー。
 * modsフォルダ内のJARファイルからIModApplicationを実装したクラスを探索・読み込む。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class ModAppLoader {

    private static final Logger LOGGER = LogManager.getLogger(ModAppLoader.class);

    /** MMOS MODアプリケーションマーカーファイル */
    private static final String MMOS_APP_MARKER = "mmos_app.json";

    /** 検出されたアプリケーション */
    private final List<ModAppInfo> discoveredApps = new ArrayList<>();

    /** アプリケーションクラスローダー */
    private URLClassLoader appClassLoader;

    /**
     * MODアプリケーション情報。
     */
    public static class ModAppInfo {
        public final String jarPath;
        public final String className;
        public final IModApplication instance;

        public ModAppInfo(String jarPath, String className, IModApplication instance) {
            this.jarPath = jarPath;
            this.className = className;
            this.instance = instance;
        }
    }

    /**
     * modsフォルダをスキャンしてMMOSアプリを検出する。
     *
     * @return 検出されたアプリの数
     */
    public int scanMods() {
        discoveredApps.clear();

        File modsDir = FMLPaths.MODSDIR.get().toFile();
        if (!modsDir.exists() || !modsDir.isDirectory()) {
            LOGGER.warn("[ModAppLoader] Mods directory not found: {}", modsDir);
            return 0;
        }

        LOGGER.info("[ModAppLoader] Scanning mods directory: {}", modsDir);

        File[] jarFiles = modsDir.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            LOGGER.info("[ModAppLoader] No JAR files found in mods directory");
            return 0;
        }

        List<URL> jarUrls = new ArrayList<>();

        for (File jarFile : jarFiles) {
            try {
                // MMOSアプリマーカーファイルを確認
                if (hasMMOSAppMarker(jarFile)) {
                    LOGGER.info("[ModAppLoader] Found MMOS app JAR: {}", jarFile.getName());
                    jarUrls.add(jarFile.toURI().toURL());
                }
            } catch (Exception e) {
                LOGGER.error("[ModAppLoader] Error scanning JAR: {}", jarFile.getName(), e);
            }
        }

        if (jarUrls.isEmpty()) {
            LOGGER.info("[ModAppLoader] No MMOS app JARs found");
            return 0;
        }

        // クラスローダーを作成
        appClassLoader = new URLClassLoader(
            jarUrls.toArray(new URL[0]),
            getClass().getClassLoader()
        );

        // 各JARからアプリを読み込み
        for (File jarFile : jarFiles) {
            try {
                if (hasMMOSAppMarker(jarFile)) {
                    loadAppsFromJar(jarFile);
                }
            } catch (Exception e) {
                LOGGER.error("[ModAppLoader] Error loading apps from JAR: {}", jarFile.getName(), e);
            }
        }

        LOGGER.info("[ModAppLoader] Discovered {} MMOS apps", discoveredApps.size());
        return discoveredApps.size();
    }

    /**
     * JARファイルにMMOSアプリマーカーが含まれているか確認する。
     */
    private boolean hasMMOSAppMarker(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry marker = jar.getJarEntry(MMOS_APP_MARKER);
            return marker != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * JARファイルからMMOSアプリを読み込む。
     */
    private void loadAppsFromJar(File jarFile) {
        try (JarFile jar = new JarFile(jarFile)) {
            // マーカーファイルを読み込んでアプリクラス名を取得
            JarEntry marker = jar.getJarEntry(MMOS_APP_MARKER);
            if (marker == null) return;

            String markerContent;
            try (InputStream is = jar.getInputStream(marker)) {
                markerContent = new String(is.readAllBytes());
            }

            // JSON解析（シンプルな実装）
            List<String> appClassNames = parseAppClassNames(markerContent);

            for (String className : appClassNames) {
                try {
                    Class<?> clazz = appClassLoader.loadClass(className);
                    if (IModApplication.class.isAssignableFrom(clazz)) {
                        IModApplication app = (IModApplication) clazz.getDeclaredConstructor().newInstance();
                        discoveredApps.add(new ModAppInfo(jarFile.getPath(), className, app));
                        LOGGER.info("[ModAppLoader] Loaded MMOS app: {} ({})", app.getName(), className);
                    } else {
                        LOGGER.warn("[ModAppLoader] Class {} does not implement IModApplication", className);
                    }
                } catch (Exception e) {
                    LOGGER.error("[ModAppLoader] Error loading app class: {}", className, e);
                }
            }

        } catch (Exception e) {
            LOGGER.error("[ModAppLoader] Error reading JAR: {}", jarFile.getName(), e);
        }
    }

    /**
     * マーカーJSONからアプリクラス名を解析する。
     * 形式: {"apps": ["com.example.MyApp", "com.example.OtherApp"]}
     */
    private List<String> parseAppClassNames(String json) {
        List<String> classNames = new ArrayList<>();

        try {
            int appsIndex = json.indexOf("\"apps\"");
            if (appsIndex < 0) return classNames;

            int arrayStart = json.indexOf("[", appsIndex);
            int arrayEnd = json.indexOf("]", arrayStart);
            if (arrayStart < 0 || arrayEnd < 0) return classNames;

            String arrayContent = json.substring(arrayStart + 1, arrayEnd);

            // クラス名を抽出
            int i = 0;
            while (i < arrayContent.length()) {
                int quoteStart = arrayContent.indexOf("\"", i);
                if (quoteStart < 0) break;

                int quoteEnd = arrayContent.indexOf("\"", quoteStart + 1);
                if (quoteEnd < 0) break;

                String className = arrayContent.substring(quoteStart + 1, quoteEnd).trim();
                if (!className.isEmpty()) {
                    classNames.add(className);
                }

                i = quoteEnd + 1;
            }
        } catch (Exception e) {
            LOGGER.error("[ModAppLoader] Error parsing app class names", e);
        }

        return classNames;
    }

    /**
     * 検出されたアプリ一覧を取得する。
     */
    public List<ModAppInfo> getDiscoveredApps() {
        return Collections.unmodifiableList(discoveredApps);
    }

    /**
     * アプリIDで検索する。
     */
    public ModAppInfo findApp(String appId) {
        for (ModAppInfo info : discoveredApps) {
            if (info.instance.getAppId().equals(appId)) {
                return info;
            }
        }
        return null;
    }

    /**
     * リソースをクリーンアップする。
     */
    public void close() {
        if (appClassLoader != null) {
            try {
                appClassLoader.close();
            } catch (Exception e) {
                LOGGER.error("[ModAppLoader] Error closing class loader", e);
            }
            appClassLoader = null;
        }
        discoveredApps.clear();
    }
}
