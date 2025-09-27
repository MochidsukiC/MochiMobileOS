package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.app.IApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    
    /** 正常に読み込まれたアプリケーションのリスト */
    private final List<IApplication> loadedApps;

    /** アプリケーションがスキャンされたかどうかを示すフラグ */
    private boolean hasScannedApps;

    /** 利用可能なMODアプリケーション候補のリスト（まだインストールされていない） */
    private final List<IApplication> availableModApps;

    /** インストール済みMODアプリケーションのリスト */
    private final List<IApplication> installedModApps;
    
    /**
     * 新しいAppLoaderサービスインスタンスを構築する。
     * 
     * @param vfs アプリケーションファイルにアクセスするための仮想ファイルシステムサービス
     */
    public AppLoader(VFS vfs) {
        this.vfs = vfs;
        this.loadedApps = new ArrayList<>();
        this.hasScannedApps = false;
        this.availableModApps = new ArrayList<>();
        this.installedModApps = new ArrayList<>();

        System.out.println("AppLoader: Application loader service initialized");
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
    public void scanForApps() {
        System.out.println("AppLoader: Scanning /apps/ directory for applications...");
        
        if (hasScannedApps) {
            System.out.println("AppLoader: Apps already scanned, skipping");
            return;
        }
        
        try {
            // appsディレクトリが存在しない場合は作成
            if (!vfs.directoryExists("apps")) {
                System.out.println("AppLoader: /apps/ directory not found, creating it");
                vfs.createDirectory("apps");
            }
            
            // JARファイルを検索
            List<String> jarFiles = vfs.listFilesByExtension("apps", ".jar");
            System.out.println("AppLoader: Found " + jarFiles.size() + " JAR files in /apps/");
            
            if (jarFiles.isEmpty()) {
                System.out.println("AppLoader: No JAR files found, scanning for class files in subdirectories");
                scanForClassFiles();
            } else {
                // 各JARファイルを処理
                for (String jarFileName : jarFiles) {
                    try {
                        loadApplicationFromJar(jarFileName);
                    } catch (Exception e) {
                        System.err.println("AppLoader: Failed to load JAR file " + jarFileName + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
            
            System.out.println("AppLoader: Scanning complete. Found " + loadedApps.size() + " applications");
            hasScannedApps = true;
            
        } catch (Exception e) {
            System.err.println("AppLoader: Error during application scanning: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * appsディレクトリ内のサブディレクトリをスキャンしてクラスファイルベースのアプリを検索する。
     */
    private void scanForClassFiles() {
        try {
            List<String> subDirs = vfs.listDirectories("apps");
            System.out.println("AppLoader: Found " + subDirs.size() + " subdirectories in /apps/");
            
            for (String subDir : subDirs) {
                try {
                    String appPath = "apps/" + subDir;
                    List<String> classFiles = vfs.listFilesByExtension(appPath, ".class");
                    
                    if (!classFiles.isEmpty()) {
                        System.out.println("AppLoader: Found " + classFiles.size() + " class files in " + appPath);
                        loadApplicationFromDirectory(subDir, appPath);
                    }
                } catch (Exception e) {
                    System.err.println("AppLoader: Failed to scan directory " + subDir + ": " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("AppLoader: Error scanning for class files: " + e.getMessage());
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
                System.err.println("AppLoader: JAR file not found: " + jarPath);
                return;
            }
            
            System.out.println("AppLoader: Loading JAR: " + jarPath);
            
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
                            URL jarUrl = jarFile.toURI().toURL();
                            URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl});
                            Class<?> clazz = classLoader.loadClass(className);
                            
                            // IApplicationインターフェースを実装しているかチェック
                            if (IApplication.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                                try {
                                    IApplication app = (IApplication) clazz.getDeclaredConstructor().newInstance();
                                    if (registerApplication(app)) {
                                        System.out.println("AppLoader: Successfully loaded app from JAR: " + app.getName());
                                    }
                                } catch (Exception e) {
                                    System.err.println("AppLoader: Failed to instantiate app class " + className + ": " + e.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            // Skip classes that can't be loaded (e.g., dependencies missing)
                            System.out.println("AppLoader: Skipping class " + className + " (load error: " + e.getMessage() + ")");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("AppLoader: Error loading JAR file " + jarFileName + ": " + e.getMessage());
        }
    }
    
    /**
     * ディレクトリからクラスファイルベースのアプリケーションを読み込む。
     * 
     * @param dirName ディレクトリ名
     * @param dirPath ディレクトリのVFSパス
     */
    private void loadApplicationFromDirectory(String dirName, String dirPath) {
        System.out.println("AppLoader: Attempting to load app from directory: " + dirPath);
        
        try {
            // ディレクトリ内のJavaクラスファイルをスキャンする
            // ここでは簡単な実装として、既知のアプリケーション構造をチェック
            
            // アプリ名からクラス名を推測（例：calculator -> CalculatorApp）
            String expectedClassName = capitalizeFirst(dirName) + "App";
            System.out.println("AppLoader: Looking for app class: " + expectedClassName);
            
            // 既存のロードされたアプリから探す（開発中のアプリ用）
            // この実装は動的クラスローディングよりも安全
            
        } catch (Exception e) {
            System.err.println("AppLoader: Error loading app from directory " + dirName + ": " + e.getMessage());
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
     * 
     * @param applicationId 検索するアプリケーションの一意識別子
     * @return 一致するIDを持つIApplicationインスタンス、または見つからない場合null
     */
    public IApplication findApplicationById(String applicationId) {
        return loadedApps.stream()
                .filter(app -> app.getApplicationId().equals(applicationId))
                .findFirst()
                .orElse(null);
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
    public boolean registerApplication(IApplication application) {
        if (application == null) {
            System.err.println("AppLoader: Cannot register null application");
            return false;
        }
        
        // Check if already registered
        if (findApplicationById(application.getApplicationId()) != null) {
            System.out.println("AppLoader: Application " + application.getName() + " already registered");
            return false;
        }
        
        loadedApps.add(application);
        System.out.println("AppLoader: Registered application: " + application.getName() + 
                          " (ID: " + application.getApplicationId() + ")");
        return true;
    }
    
    /**
     * ローダーからアプリケーションの登録を解除する。
     * 
     * @param applicationId 登録解除するアプリケーションの一意識別子
     * @return アプリケーションが正常に登録解除された場合true、見つからない場合false
     */
    public boolean unregisterApplication(String applicationId) {
        IApplication app = findApplicationById(applicationId);
        if (app != null) {
            loadedApps.remove(app);
            System.out.println("AppLoader: Unregistered application: " + app.getName());
            return true;
        }
        return false;
    }
    
    /**
     * /apps/ディレクトリを再スキャンしてアプリケーションリストを更新する。
     * このメソッドは現在読み込まれているアプリケーションをクリアし、新たなスキャンを実行する。
     */
    public void refreshApps() {
        System.out.println("AppLoader: Refreshing application list...");
        
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
    public boolean registerAvailableModApp(IApplication application) {
        if (application == null) {
            throw new IllegalArgumentException("Application cannot be null");
        }

        // 重複チェック（利用可能リスト内）
        for (IApplication existingApp : availableModApps) {
            if (existingApp.getApplicationId().equals(application.getApplicationId())) {
                System.out.println("AppLoader: MOD app " + application.getApplicationId() +
                                 " already registered as available, skipping");
                return false;
            }
        }

        // インストール済みリストでもチェック
        for (IApplication installedApp : installedModApps) {
            if (installedApp.getApplicationId().equals(application.getApplicationId())) {
                System.out.println("AppLoader: MOD app " + application.getApplicationId() +
                                 " already installed, skipping registration as available");
                return false;
            }
        }

        availableModApps.add(application);
        System.out.println("AppLoader: Registered available MOD app: " +
                          application.getApplicationName() + " (" + application.getApplicationId() + ")");
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
    public boolean installModApp(String applicationId, Object kernel) {
        if (applicationId == null || applicationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Application ID cannot be null or empty");
        }

        // 利用可能なアプリケーション候補から検索
        IApplication appToInstall = getAvailableModApp(applicationId);
        if (appToInstall == null) {
            System.err.println("AppLoader: Cannot install MOD app " + applicationId +
                             " - not found in available apps");
            return false;
        }

        // 既にインストール済みかチェック
        for (IApplication installedApp : installedModApps) {
            if (installedApp.getApplicationId().equals(applicationId)) {
                System.out.println("AppLoader: MOD app " + applicationId + " already installed");
                return false;
            }
        }

        try {
            // アプリケーションをインストール
            System.out.println("AppLoader: Installing MOD app: " + appToInstall.getApplicationName());

            // アプリケーションのonInstall()メソッドを呼び出し
            if (kernel instanceof jp.moyashi.phoneos.core.Kernel) {
                appToInstall.onInstall((jp.moyashi.phoneos.core.Kernel) kernel);
            }

            // 利用可能リストから削除してインストール済みリストに追加
            availableModApps.remove(appToInstall);
            installedModApps.add(appToInstall);

            // 通常のアプリケーションリストにも追加（ランチャーで表示されるように）
            loadedApps.add(appToInstall);

            System.out.println("AppLoader: Successfully installed MOD app: " +
                             appToInstall.getApplicationName());
            return true;

        } catch (Exception e) {
            System.err.println("AppLoader: Failed to install MOD app " + applicationId +
                             ": " + e.getMessage());
            e.printStackTrace();
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

            System.out.println("AppLoader: Syncing with Forge ModAppRegistry - found " +
                             forgeApps.size() + " apps");

            for (IApplication app : forgeApps) {
                registerAvailableModApp(app);
            }

            System.out.println("AppLoader: Sync complete - " + availableModApps.size() +
                             " MOD apps now available");

        } catch (ClassNotFoundException e) {
            // Forgeモジュールが存在しない（スタンドアロン環境）
            System.out.println("AppLoader: Forge module not found - running in standalone mode");
        } catch (Exception e) {
            System.err.println("AppLoader: Error syncing with ModAppRegistry: " + e.getMessage());
        }
    }
}