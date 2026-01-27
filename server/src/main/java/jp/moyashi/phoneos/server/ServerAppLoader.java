package jp.moyashi.phoneos.server;

import jp.moyashi.phoneos.server.network.SystemServerRegistry;
import jp.moyashi.phoneos.server.network.VirtualHttpServer;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * サーバーサイドアプリケーション（VirtualHttpServer実装）のローダー。
 * 専用ディレクトリからJARファイルをスキャンし、VirtualHttpServerを動的にロードする。
 *
 * AppLoaderがVFS/appsからIApplicationを読み込むのと同様に、
 * ServerAppLoaderはserver_apps/ディレクトリからVirtualHttpServer実装を読み込む。
 *
 * サーバーアプリJARはServiceLoader（SPI）を使用して実装を提供する必要がある：
 * META-INF/services/jp.moyashi.phoneos.server.network.VirtualHttpServer
 */
public class ServerAppLoader {

    private static final String SERVER_APPS_DIR = "server_apps";

    private final List<VirtualHttpServer> loadedServers = new ArrayList<>();
    private final List<URLClassLoader> classLoaders = new ArrayList<>();

    /**
     * サーバーアプリをスキャンして読み込む
     *
     * @param basePath ベースディレクトリ（VFSルートまたはファイルシステムパス）
     */
    public void scanAndLoad(Path basePath) {
        // 1. server_apps/ ディレクトリをスキャン（サーバー専用アプリ）
        Path serverAppsPath = basePath.resolve(SERVER_APPS_DIR);
        scanDirectory(serverAppsPath, true);

        // 2. apps/ ディレクトリもスキャン（クライアント+サーバー統合アプリ）
        // これにより、IApplicationとVirtualHttpServerの両方を含むJARから
        // サーバー機能も検出できる
        Path appsPath = basePath.resolve("apps");
        scanDirectory(appsPath, false);

        log("Scan complete. Loaded " + loadedServers.size() + " server app(s)");
    }

    /**
     * 指定ディレクトリをスキャンしてJARを読み込む
     *
     * @param directory スキャン対象ディレクトリ
     * @param createIfMissing ディレクトリが存在しない場合に作成するか
     */
    private void scanDirectory(Path directory, boolean createIfMissing) {
        try {
            if (!Files.exists(directory)) {
                if (createIfMissing) {
                    Files.createDirectories(directory);
                    log("Created directory: " + directory);
                }
                return;
            }

            // JARファイルをスキャン
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.jar")) {
                for (Path jarPath : stream) {
                    loadServerApp(jarPath);
                }
            }

        } catch (IOException e) {
            log("Failed to scan directory " + directory + ": " + e.getMessage());
        }
    }

    /**
     * JARファイルからVirtualHttpServer実装を読み込む
     *
     * @param jarPath JARファイルのパス
     */
    private void loadServerApp(Path jarPath) {
        log("Loading server app from: " + jarPath.getFileName());

        try {
            // URLClassLoaderを作成（クローズせずに保持）
            URLClassLoader classLoader = new URLClassLoader(
                    new URL[]{jarPath.toUri().toURL()},
                    getClass().getClassLoader());

            // ServiceLoaderでVirtualHttpServer実装を検出
            ServiceLoader<VirtualHttpServer> loader =
                    ServiceLoader.load(VirtualHttpServer.class, classLoader);

            int count = 0;
            for (VirtualHttpServer server : loader) {
                loadedServers.add(server);
                count++;
                log("  Found: " + server.getServerId() + " (" + server.getClass().getName() + ")");
            }

            if (count > 0) {
                // クラスローダーを保持（サーバーインスタンスが参照するため）
                classLoaders.add(classLoader);
            } else {
                // サーバーが見つからない場合はクラスローダーを閉じる
                classLoader.close();
                log("  No VirtualHttpServer implementation found in: " + jarPath.getFileName());
            }

        } catch (Exception e) {
            log("Failed to load server app: " + jarPath.getFileName() + " - " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 読み込んだサーバーをSystemServerRegistryに登録
     */
    public void registerAll() {
        SystemServerRegistry registry = SystemServerRegistry.getInstance();

        for (VirtualHttpServer server : loadedServers) {
            try {
                registry.registerServer(server.getServerId(), server);
                log("Registered server: " + server.getServerId());
            } catch (Exception e) {
                log("Failed to register server: " + server.getServerId() + " - " + e.getMessage());
            }
        }
    }

    /**
     * 読み込んだサーバーの数を取得
     *
     * @return サーバー数
     */
    public int getLoadedServerCount() {
        return loadedServers.size();
    }

    /**
     * 読み込んだサーバーのリストを取得
     *
     * @return サーバーリスト（読み取り専用）
     */
    public List<VirtualHttpServer> getLoadedServers() {
        return List.copyOf(loadedServers);
    }

    /**
     * リソースを解放
     */
    public void close() {
        for (URLClassLoader classLoader : classLoaders) {
            try {
                classLoader.close();
            } catch (IOException e) {
                // ignore
            }
        }
        classLoaders.clear();
        loadedServers.clear();
    }

    private void log(String message) {
        System.out.println("[ServerAppLoader] " + message);
    }
}
