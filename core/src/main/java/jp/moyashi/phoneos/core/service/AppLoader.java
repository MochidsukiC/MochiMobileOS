package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.service.LoggerContext;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.Enumeration;

/**
 * MochiMobileOS環境内でのアプリケーションの発見、読み込み、管理を担当する
 * アプリケーションローダーサービス。
 * 
 * このサービスは仮想ファイルシステムの/apps/ディレクトリ内のアプリケーション
 * パッケージ（JARファイル）をスキャンし、動的にロードして、ユーザーが起動
 * 可能な利用可能アプリケーションのレジストリを維持する。
 * 
 * AppLoaderはVFSと連携してプラグインスタイルのアーキテクチャを提供し、
 * アプリケーションを実行時にインストール・削除できるようにする。
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class AppLoader {
    
    /** アプリケーションファイルにアクセスするための仮想ファイルシステムインスタンス */
    private final VFS vfs;
    
    /** 正常に読み込まれたアプリケーションのリスト（スレッドセーフ） */
    private final List<IApplication> loadedApps;

    /** アプリケーションがスキャンされたかどうかを示すフラグ（可視性保証のためvolatile化） */
    private volatile boolean hasScannedApps;

    /** 利用可能なMODアプリケーション候補のリスト（まだインストールされていない、スレッドセーフ） */
    private final List<IApplication> availableModApps;

    /** インストール済みMODアプリケーションのリスト（スレッドセーフ） */
    private final List<IApplication> installedModApps;

    /** baseAppId -> resolvedAppIdのマッピング（永続化対応、スレッドセーフ） */
    private final Map<String, String> appIdRegistry;

    /** 永続化されたインストール済みMODアプリのIDセット */
    private final java.util.Set<String> persistedInstalledModAppIds;

    /** 永続化ファイルパス */
    private static final String APP_ID_REGISTRY_PATH = "system/app_id_registry.json";

    /** インストール済みMODアプリの永続化ファイルパス */
    private static final String INSTALLED_MOD_APPS_PATH = "system/installed_mod_apps.json";

    /**
     * 新しいAppLoaderサービスインスタンスを構築する。
     * 
     * @param vfs アプリケーションファイルにアクセスするための仮想ファイルシステムサービス
     */
    public AppLoader(VFS vfs) {
        this.vfs = vfs;
        // スレッドセーフなコレクションを使用（UIスレッドとバックグラウンドスレッド間の競合防止）
        this.loadedApps = new CopyOnWriteArrayList<>();
        this.hasScannedApps = false;
        this.availableModApps = new CopyOnWriteArrayList<>();
        this.installedModApps = new CopyOnWriteArrayList<>();
        this.appIdRegistry = new ConcurrentHashMap<>();
        this.persistedInstalledModAppIds = java.util.concurrent.ConcurrentHashMap.newKeySet();

        // 永続化データを読み込み
        loadAppIdRegistry();
        loadInstalledModApps();
    }
    
    /**
     * VFSの/apps/ディレクトリでアプリケーションパッケージをスキャンし、読み込みを試みる。
     * このメソッドはIApplicationインターフェースを実装したクラスを含む
     * .jarファイルを検索し、動的にアプリケーションレジストリに読み込む。
     * 
     * スキャン処理には以下が含まれる：
     * 1. /apps/ディレクトリ内のファイルをVFSに問い合わせ
     * 2. .jarファイルでフィルタリング
     * 3. リフレクションを使用してアプリケーションクラスを読み込み
     * 4. IApplicationを実装したアプリケーションをインスタンス化
     * 5. 有効なアプリケーションを読み込まれたアプリリストに追加
     */
    public synchronized void scanForApps() {
        if (hasScannedApps) {
            return;
        }

        try {
            LoggerContext.info("AppLoader", "Scanning apps directory...");
            // appsディレクトリが存在しない場合は作成
            if (!vfs.directoryExists("apps")) {
                vfs.createDirectory("apps");
            }

            // JARファイルを検索
            List<String> jarFiles = vfs.listFilesByExtension("apps", ".jar");
            if (!jarFiles.isEmpty()) {
                LoggerContext.info("AppLoader", "Found " + jarFiles.size() + " app jar(s): " + jarFiles);
            }

            if (jarFiles.isEmpty()) {
                scanForClassFiles();
            } else {
                // 各JARファイルを処理
                for (String jarFileName : jarFiles) {
                    try {
                        loadApplicationFromJar(jarFileName);
                    } catch (Exception e) {
                        LoggerContext.error("AppLoader", "Failed to load app jar: " + jarFileName + " - " + e);
                        // Skip failed JAR files
                    }
                }
            }

            hasScannedApps = true;

        } catch (Exception e) {
            LoggerContext.error("AppLoader", "App scan failed: " + e);
            // Scanning failed
        }
    }
    
    /**
     * appsディレクトリ内のサブディレクトリをスキャンしてクラスファイルベースのアプリを検索する。
     */
    private void scanForClassFiles() {
        try {
            List<String> subDirs = vfs.listDirectories("apps");

            for (String subDir : subDirs) {
                try {
                    String appPath = "apps/" + subDir;
                    List<String> classFiles = vfs.listFilesByExtension(appPath, ".class");

                    if (!classFiles.isEmpty()) {
                        loadApplicationFromDirectory(subDir, appPath);
                    }
                } catch (Exception e) {
                    // Skip failed directories
                }
            }
        } catch (Exception e) {
            LoggerContext.error("AppLoader", "App scan failed: " + e);
            // Scanning failed
        }
    }
    
    /**
     * JARファイルからアプリケーションを読み込む。
     * 
     * @param jarFileName JARファイル名
     */
    private void loadApplicationFromJar(String jarFileName) {
        try {
            String jarPath = vfs.getFullPath("apps/" + jarFileName);
            File jarFile = new File(jarPath);

            if (!jarFile.exists()) {
                LoggerContext.warn("AppLoader", "Jar not found: " + jarPath);
                return;
            }

            // JARファイル内のクラスをスキャン
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // .classファイルのみを処理
                    if (entryName.endsWith(".class")) {
                        String className = entryName.replace('/', '.').replace(".class", "");

                        try {
                            // クラスローダーを作成してクラスをロード
                            // 親クラスローダーを指定して、IApplicationなどのコアクラスを参照可能にする
                            URL jarUrl = jarFile.toURI().toURL();
                            URLClassLoader classLoader = new URLClassLoader(
                                new URL[]{jarUrl},
                                getClass().getClassLoader()
                            );
                            Class<?> clazz = classLoader.loadClass(className);

                            // IApplicationインターフェースを実装しているかチェック
                            if (IApplication.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                                try {
                                    IApplication app = (IApplication) clazz.getDeclaredConstructor().newInstance();
                                    if (registerApplication(app)) {
                                        LoggerContext.info("AppLoader", "Registered app from jar: " + app.getApplicationId());
                                    }
                                } catch (Exception e) {
                                    LoggerContext.error("AppLoader", "Failed to instantiate app class: " + className + " - " + e);
                                    // Failed to instantiate app class
                                }
                            }
                        } catch (Exception e) {
                            LoggerContext.warn("AppLoader", "Failed to load class from jar: " + className + " - " + e);
                            // Skip classes that can't be loaded (e.g., dependencies missing)
                        }
                    }
                }
            }
        } catch (Exception e) {
            LoggerContext.error("AppLoader", "Error loading JAR file: " + jarFileName + " - " + e);
            // Error loading JAR file
        }
    }
    
    /**
     * ディレクトリからクラスファイルベースのアプリケーションを読み込む。
     * 
     * @param dirName ディレクトリ名
     * @param dirPath ディレクトリのVFSパス
     */
    private void loadApplicationFromDirectory(String dirName, String dirPath) {
        try {
            // ディレクトリ内のJavaクラスファイルをスキャンする
            // ここでは簡単な実装として、既知のアプリケーション構造をチェック

            // アプリ名からクラス名を推測（例：calculator -> CalculatorApp）
            String expectedClassName = capitalizeFirst(dirName) + "App";

            // 既存のロードされたアプリから探す（開発中のアプリ用）
            // この実装は動的クラスローディングよりも安全

        } catch (Exception e) {
            // Error loading app from directory
        }
    }
    
    /**
     * 文字列の最初の文字を大文字にする。
     * 
     * @param str 入力文字列
     * @return 最初の文字が大文字の文字列
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    /**
     * 正常に読み込まれたすべてのアプリケーションの不変リストを返す。
     * アプリケーションはスキャン処理中に読み込まれた順序で返される。
     * 
     * @return 読み込まれたIApplicationインスタンスの不変リスト
     */
    public List<IApplication> getLoadedApps() {
        return Collections.unmodifiableList(loadedApps);
    }
    
    /**
     * 現在読み込まれているアプリケーションの数を取得する。
     * 
     * @return 読み込まれたアプリケーションの数
     */
    public int getLoadedAppCount() {
        return loadedApps.size();
    }
    
    /**
     * 一意のアプリケーションIDでアプリケーションを検索する。
     * 解決済みappIdを使用して検索する。
     *
     * @param applicationId 検索するアプリケーションの一意識別子
     * @return 一致するIDを持つIApplicationインスタンス、または見つからない場合null
     */
    public IApplication findApplicationById(String applicationId) {
        for (IApplication app : loadedApps) {
            String baseId = app.getApplicationId();
            String resolvedId = appIdRegistry.get(baseId);
            // 解決済みIDまたは元のIDで検索
            if ((resolvedId != null && resolvedId.equals(applicationId)) ||
                baseId.equals(applicationId)) {
                return app;
            }
        }
        return null;
    }
    
    /**
     * 表示名でアプリケーションを検索する。
     * 表示名は一意であることが保証されていないため、このメソッドは
     * 最初に見つかった一致するアプリケーションを返す。
     * 
     * @param name 検索するアプリケーションの表示名
     * @return 一致する名前を持つ最初のIApplicationインスタンス、または見つからない場合null
     */
    public IApplication findApplicationByName(String name) {
        return loadedApps.stream()
                .filter(app -> app.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * ローダーにアプリケーションを手動で登録する。
     * このメソッドはシステムランチャーなど、JARファイルから
     * 読み込みを行う必要のない組み込みアプリケーションに有用である。
     *
     * @param application 登録するアプリケーションインスタンス
     * @return アプリケーションが正常に登録された場合true、すでに登録済みの場合false
     */
    public synchronized boolean registerApplication(IApplication application) {
        if (application == null) {
            return false;
        }

        // appIdを解決（重複時はナンバリング）
        String resolvedId = resolveAppId(application);

        // 既に登録済みかチェック（解決済みIDで検索）
        IApplication existingApp = findApplicationById(resolvedId);
        if (existingApp != null) {
            loadedApps.remove(existingApp);

            // MODアプリリストからも削除（もしあれば）
            if (installedModApps.contains(existingApp)) {
                installedModApps.remove(existingApp);
                installedModApps.add(application);
            }
        }

        loadedApps.add(application);
        return true;
    }
    
    /**
     * ローダーからアプリケーションの登録を解除する。
     * 
     * @param applicationId 登録解除するアプリケーションの一意識別子
     * @return アプリケーションが正常に登録解除された場合true、見つからない場合false
     */
    public synchronized boolean unregisterApplication(String applicationId) {
        IApplication app = findApplicationById(applicationId);
        if (app != null) {
            loadedApps.remove(app);
            return true;
        }
        return false;
    }
    
    /**
     * /apps/ディレクトリを再スキャンしてアプリケーションリストを更新する。
     * このメソッドは現在読み込まれているアプリケーションをクリアし、新たなスキャンを実行する。
     */
    public void refreshApps() {
        // Don't clear built-in apps, only those loaded from files
        // In a full implementation, we would differentiate between
        // file-loaded and manually-registered apps

        hasScannedApps = false;
        scanForApps();
    }
    
    /**
     * アプリケーションスキャンが実行されたかどうかを確認する。
     * 
     * @return アプリケーションがスキャン済みの場合true、そうでなければfalse
     */
    public boolean hasScannedForApps() {
        return hasScannedApps;
    }
    
    /**
     * このAppLoaderが使用するVFSインスタンスを取得する。
     *
     * @return VFSインスタンス
     */
    public VFS getVFS() {
        return vfs;
    }

    // ========== MODアプリケーション管理機能 ==========

    /**
     * 利用可能なMODアプリケーション候補を登録します。
     *
     * このメソッドは主にForge環境で、PhoneAppRegistryEventを通じて
     * 他のMODから登録されたアプリケーションを受け取る際に使用されます。
     *
     * 登録されたアプリケーションは「利用可能」状態となり、
     * AppStoreでユーザーがインストールを選択できるようになります。
     *
     * @param application 登録するアプリケーション。nullは許可されません。
     * @throws IllegalArgumentException applicationがnullの場合
     * @return 正常に登録された場合true、既に登録済みの場合false
     */
    public synchronized boolean registerAvailableModApp(IApplication application) {
        if (application == null) {
            throw new IllegalArgumentException("Application cannot be null");
        }

        // 重複チェック（利用可能リスト内）
        for (IApplication existingApp : availableModApps) {
            if (existingApp.getApplicationId().equals(application.getApplicationId())) {
                return false;
            }
        }

        // インストール済みリストでもチェック
        for (IApplication installedApp : installedModApps) {
            if (installedApp.getApplicationId().equals(application.getApplicationId())) {
                return false;
            }
        }

        // プリインストールアプリ（loadedApps）との重複チェック
        for (IApplication loadedApp : loadedApps) {
            if (loadedApp.getApplicationId().equals(application.getApplicationId())) {
                return false;
            }
        }

        availableModApps.add(application);
        return true;
    }

    /**
     * すべての利用可能なMODアプリケーション候補のリストを取得します。
     *
     * @return 利用可能なMODアプリケーションのリスト（読み取り専用）
     */
    public List<IApplication> getAvailableModApps() {
        return Collections.unmodifiableList(availableModApps);
    }

    /**
     * すべてのインストール済みMODアプリケーションのリストを取得します。
     *
     * @return インストール済みMODアプリケーションのリスト（読み取り専用）
     */
    public List<IApplication> getInstalledModApps() {
        return Collections.unmodifiableList(installedModApps);
    }

    /**
     * 指定されたアプリケーションIDの利用可能なMODアプリケーションを取得します。
     *
     * @param applicationId 検索するアプリケーションID
     * @return 見つかったアプリケーション、存在しない場合はnull
     */
    public IApplication getAvailableModApp(String applicationId) {
        if (applicationId == null) {
            return null;
        }

        for (IApplication app : availableModApps) {
            if (applicationId.equals(app.getApplicationId())) {
                return app;
            }
        }
        return null;
    }

    /**
     * MODアプリケーションをインストールします。
     *
     * 利用可能なアプリケーション候補からインストール済みリストに移動し、
     * アプリケーションのonInstall()メソッドを呼び出します。
     *
     * @param applicationId インストールするアプリケーションのID
     * @param kernel OSカーネルインスタンス（onInstall()メソッド用）
     * @return インストールが成功した場合true
     * @throws IllegalArgumentException applicationIdがnullまたは空の場合
     */
    public synchronized boolean installModApp(String applicationId, Object kernel) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Application ID cannot be null or empty");
        }

        // 利用可能なアプリケーション候補から検索
        IApplication appToInstall = getAvailableModApp(applicationId);
        if (appToInstall == null) {
            return false;
        }

        // 既にインストール済みかチェック
        for (IApplication installedApp : installedModApps) {
            if (installedApp.getApplicationId().equals(applicationId)) {
                return false;
            }
        }

        try {
            // アプリケーションのonInstall()メソッドを呼び出し
            if (kernel instanceof jp.moyashi.phoneos.core.Kernel) {
                appToInstall.onInitialize((jp.moyashi.phoneos.core.Kernel) kernel);
            }

            // appIdを解決してレジストリに登録（セッション再利用のため）
            resolveAppId(appToInstall);

            // 利用可能リストから削除してインストール済みリストに追加
            availableModApps.remove(appToInstall);
            installedModApps.add(appToInstall);

            // 通常のアプリケーションリストにも追加（ランチャーで表示されるように）
            loadedApps.add(appToInstall);

            // 永続化データを更新
            persistedInstalledModAppIds.add(applicationId);
            saveInstalledModApps();

            LoggerContext.info("AppLoader", "MOD app installed and persisted: " + applicationId);

            return true;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * MODアプリケーションがインストール済みかどうかを確認します。
     *
     * @param applicationId 確認するアプリケーションのID
     * @return インストール済みの場合true
     */
    public boolean isModAppInstalled(String applicationId) {
        if (applicationId == null) {
            return false;
        }

        for (IApplication app : installedModApps) {
            if (applicationId.equals(app.getApplicationId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 利用可能なMODアプリケーション候補の数を取得します。
     *
     * @return 利用可能なMODアプリケーション数
     */
    public int getAvailableModAppsCount() {
        return availableModApps.size();
    }

    /**
     * インストール済みMODアプリケーションの数を取得します。
     *
     * @return インストール済みMODアプリケーション数
     */
    public int getInstalledModAppsCount() {
        return installedModApps.size();
    }

    /**
     * Forge環境のModAppRegistryから利用可能なアプリケーションを一括で同期します。
     *
     * このメソッドは、Forgeモジュールが利用可能な場合にのみ動作します。
     * スタンドアロン環境では何も実行されません。
     */
    public void syncWithModRegistry() {
        try {
            // リフレクションを使ってForgeモジュールの存在を確認
            Class<?> modRegistryClass = Class.forName("jp.moyashi.phoneos.forge.event.ModAppRegistry");
            Object registryInstance = modRegistryClass.getMethod("getInstance").invoke(null);

            @SuppressWarnings("unchecked")
            List<IApplication> forgeApps = (List<IApplication>) modRegistryClass
                .getMethod("getAvailableApps").invoke(registryInstance);

            for (IApplication app : forgeApps) {
                registerAvailableModApp(app);
            }

        } catch (ClassNotFoundException e) {
            // Forgeモジュールが存在しない（スタンドアロン環境）
        } catch (Exception e) {
            // Error syncing with ModAppRegistry
        }
    }

    // ==================== appId解決・永続化機構 ====================

    /**
     * アプリケーションの解決済みappIdを取得する。
     * 重複がある場合はナンバリングで一意化し、永続化する。
     *
     * @param application アプリケーションインスタンス
     * @return 解決済みappId
     */
    public String resolveAppId(IApplication application) {
        if (application == null) {
            return null;
        }

        String baseId = application.getApplicationId();
        LoggerContext.info("AppLoader", "resolveAppId called for: " + baseId);

        // 既にレジストリにあればそれを返す
        if (appIdRegistry.containsKey(baseId)) {
            String cached = appIdRegistry.get(baseId);
            LoggerContext.info("AppLoader", "Found in registry: " + baseId + " -> " + cached);
            return cached;
        }

        // 新規登録: 重複をチェックしてナンバリング
        String resolvedId = baseId;
        int counter = 2;

        while (isAppIdInUse(resolvedId)) {
            resolvedId = baseId + "_" + counter;
            counter++;
        }

        // マッピングを保存
        appIdRegistry.put(baseId, resolvedId);
        saveAppIdRegistry();

        LoggerContext.info("AppLoader", "New appId registered: " + baseId + " -> " + resolvedId);

        return resolvedId;
    }

    /**
     * 指定されたappIdが既に使用中かどうかを確認する。
     *
     * @param appId 確認するappId
     * @return 使用中の場合true
     */
    private boolean isAppIdInUse(String appId) {
        return appIdRegistry.containsValue(appId);
    }

    /**
     * アプリケーションの解決済みappIdを取得する。
     * 解決されていない場合は解決してから返す。
     *
     * @param application アプリケーションインスタンス
     * @return 解決済みappId
     */
    public String getResolvedAppId(IApplication application) {
        if (application == null) {
            return null;
        }
        return resolveAppId(application);
    }

    /**
     * VFSからappIdレジストリを読み込む。
     */
    private void loadAppIdRegistry() {
        try {
            if (!vfs.fileExists(APP_ID_REGISTRY_PATH)) {
                return;
            }

            String json = vfs.readFile(APP_ID_REGISTRY_PATH);
            if (json == null || json.trim().isEmpty()) {
                return;
            }

            // 簡易JSONパース（{ "key": "value", ... } 形式）
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) {
                json = json.substring(1, json.length() - 1).trim();
                if (!json.isEmpty()) {
                    String[] entries = json.split(",");
                    for (String entry : entries) {
                        String[] keyValue = entry.split(":");
                        if (keyValue.length == 2) {
                            String key = keyValue[0].trim().replace("\"", "");
                            String value = keyValue[1].trim().replace("\"", "");
                            appIdRegistry.put(key, value);
                        }
                    }
                }
            }

        } catch (Exception e) {
            // Error loading app ID registry
        }
    }

    /**
     * appIdレジストリをVFSに保存する。
     */
    private void saveAppIdRegistry() {
        try {
            // systemディレクトリが存在しない場合は作成
            if (!vfs.directoryExists("system")) {
                vfs.createDirectory("system");
            }

            // 簡易JSON生成
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            boolean first = true;
            for (Map.Entry<String, String> entry : appIdRegistry.entrySet()) {
                if (!first) {
                    json.append(",\n");
                }
                json.append("  \"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
                first = false;
            }
            json.append("\n}");

            vfs.writeFile(APP_ID_REGISTRY_PATH, json.toString());

        } catch (Exception e) {
            // Error saving app ID registry
        }
    }

    // ==================== インストール済みMODアプリの永続化機構 ====================

    /**
     * VFSからインストール済みMODアプリのリストを読み込む。
     */
    private void loadInstalledModApps() {
        try {
            if (!vfs.fileExists(INSTALLED_MOD_APPS_PATH)) {
                LoggerContext.info("AppLoader", "No installed_mod_apps.json found, starting fresh");
                return;
            }

            String json = vfs.readFile(INSTALLED_MOD_APPS_PATH);
            if (json == null || json.trim().isEmpty()) {
                return;
            }

            // 簡易JSONパース（[ "id1", "id2", ... ] 形式）
            json = json.trim();
            if (json.startsWith("[") && json.endsWith("]")) {
                json = json.substring(1, json.length() - 1).trim();
                if (!json.isEmpty()) {
                    String[] ids = json.split(",");
                    for (String id : ids) {
                        String cleanId = id.trim().replace("\"", "");
                        if (!cleanId.isEmpty()) {
                            persistedInstalledModAppIds.add(cleanId);
                        }
                    }
                }
            }

            LoggerContext.info("AppLoader", "Loaded " + persistedInstalledModAppIds.size() + " persisted installed MOD app IDs");

        } catch (Exception e) {
            LoggerContext.error("AppLoader", "Error loading installed MOD apps: " + e.getMessage());
        }
    }

    /**
     * インストール済みMODアプリのリストをVFSに保存する。
     */
    private void saveInstalledModApps() {
        try {
            // systemディレクトリが存在しない場合は作成
            if (!vfs.directoryExists("system")) {
                vfs.createDirectory("system");
            }

            // 簡易JSON生成（配列形式）
            StringBuilder json = new StringBuilder();
            json.append("[\n");
            boolean first = true;
            for (String appId : persistedInstalledModAppIds) {
                if (!first) {
                    json.append(",\n");
                }
                json.append("  \"").append(appId).append("\"");
                first = false;
            }
            json.append("\n]");

            vfs.writeFile(INSTALLED_MOD_APPS_PATH, json.toString());

            LoggerContext.info("AppLoader", "Saved " + persistedInstalledModAppIds.size() + " installed MOD app IDs");

        } catch (Exception e) {
            LoggerContext.error("AppLoader", "Error saving installed MOD apps: " + e.getMessage());
        }
    }

    /**
     * 永続化されたインストール状態に基づいて、利用可能なMODアプリから
     * 以前インストールされていたアプリを復元する。
     *
     * このメソッドはsyncWithModRegistry()の後に呼び出されるべきである。
     *
     * @param kernel OSカーネルインスタンス（onInstall()メソッド用）
     * @return 復元されたアプリの数
     */
    public synchronized int restoreInstalledModApps(Object kernel) {
        int restoredCount = 0;

        LoggerContext.info("AppLoader", "Restoring installed MOD apps from persisted data...");
        LoggerContext.info("AppLoader", "Persisted IDs: " + persistedInstalledModAppIds);
        LoggerContext.info("AppLoader", "Available MOD apps: " + availableModApps.size());

        for (String appId : persistedInstalledModAppIds) {
            // 既にインストール済みならスキップ
            if (isModAppInstalled(appId)) {
                LoggerContext.info("AppLoader", "Already installed, skipping: " + appId);
                continue;
            }

            // 利用可能なアプリから探してインストール
            IApplication app = getAvailableModApp(appId);
            if (app != null) {
                boolean success = installModAppWithoutPersist(appId, kernel);
                if (success) {
                    restoredCount++;
                    LoggerContext.info("AppLoader", "Restored MOD app: " + appId);
                } else {
                    LoggerContext.warn("AppLoader", "Failed to restore MOD app: " + appId);
                }
            } else {
                LoggerContext.warn("AppLoader", "Persisted MOD app not found in available list: " + appId);
            }
        }

        LoggerContext.info("AppLoader", "Restored " + restoredCount + " MOD apps from persisted data");
        return restoredCount;
    }

    /**
     * MODアプリケーションをインストールする（永続化を行わない内部メソッド）。
     * 復元処理で使用される。
     *
     * @param applicationId インストールするアプリケーションのID
     * @param kernel OSカーネルインスタンス
     * @return インストールが成功した場合true
     */
    private synchronized boolean installModAppWithoutPersist(String applicationId, Object kernel) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            return false;
        }

        // 利用可能なアプリケーション候補から検索
        IApplication appToInstall = getAvailableModApp(applicationId);
        if (appToInstall == null) {
            return false;
        }

        // 既にインストール済みかチェック
        for (IApplication installedApp : installedModApps) {
            if (installedApp.getApplicationId().equals(applicationId)) {
                return false;
            }
        }

        try {
            // アプリケーションのonInstall()メソッドを呼び出し
            if (kernel instanceof jp.moyashi.phoneos.core.Kernel) {
                appToInstall.onInitialize((jp.moyashi.phoneos.core.Kernel) kernel);
            }

            // appIdを解決してレジストリに登録
            resolveAppId(appToInstall);

            // 利用可能リストから削除してインストール済みリストに追加
            availableModApps.remove(appToInstall);
            installedModApps.add(appToInstall);

            // 通常のアプリケーションリストにも追加
            loadedApps.add(appToInstall);

            return true;

        } catch (Exception e) {
            LoggerContext.error("AppLoader", "Error installing MOD app: " + e.getMessage());
            return false;
        }
    }

    /**
     * MODアプリケーションをアンインストールする。
     *
     * @param applicationId アンインストールするアプリケーションのID
     * @return アンインストールが成功した場合true
     */
    public synchronized boolean uninstallModApp(String applicationId) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            return false;
        }

        // インストール済みリストから探す
        IApplication appToUninstall = null;
        for (IApplication app : installedModApps) {
            if (app.getApplicationId().equals(applicationId)) {
                appToUninstall = app;
                break;
            }
        }

        if (appToUninstall == null) {
            return false;
        }

        // インストール済みリストから削除
        installedModApps.remove(appToUninstall);

        // 通常のアプリケーションリストからも削除
        loadedApps.remove(appToUninstall);

        // 利用可能リストに戻す
        availableModApps.add(appToUninstall);

        // 永続化データを更新
        persistedInstalledModAppIds.remove(applicationId);
        saveInstalledModApps();

        LoggerContext.info("AppLoader", "MOD app uninstalled and persisted: " + applicationId);

        return true;
    }

    /**
     * 永続化されているインストール済みMODアプリのIDセットを取得する。
     *
     * @return 永続化されたアプリIDのセット（読み取り専用）
     */
    public java.util.Set<String> getPersistedInstalledModAppIds() {
        return Collections.unmodifiableSet(persistedInstalledModAppIds);
    }
}