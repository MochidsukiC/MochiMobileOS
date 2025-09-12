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
    
    /**
     * 新しいAppLoaderサービスインスタンスを構築する。
     * 
     * @param vfs アプリケーションファイルにアクセスするための仮想ファイルシステムサービス
     */
    public AppLoader(VFS vfs) {
        this.vfs = vfs;
        this.loadedApps = new ArrayList<>();
        this.hasScannedApps = false;
        
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
}