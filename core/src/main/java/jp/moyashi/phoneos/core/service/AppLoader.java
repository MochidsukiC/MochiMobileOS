package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.app.IApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * 
     * これはVFSが実際のファイルシステム操作をサポートするようになった時に
     * 拡張されるプレースホルダー実装である。
     */
    public void scanForApps() {
        System.out.println("AppLoader: Scanning /apps/ directory for applications...");
        
        if (hasScannedApps) {
            System.out.println("AppLoader: Apps already scanned, skipping");
            return;
        }
        
        try {
            // TODO: Implement actual file system scanning when VFS is fully implemented
            // For now, this is a placeholder that logs the scanning operation
            
            // Future implementation will include:
            // 1. List<String> jarFiles = vfs.listFiles("/apps/", "*.jar");
            // 2. For each JAR file, create a ClassLoader
            // 3. Search for classes implementing IApplication
            // 4. Instantiate and add to loadedApps list
            
            System.out.println("AppLoader: Scanning complete. Found " + loadedApps.size() + " applications");
            hasScannedApps = true;
            
        } catch (Exception e) {
            System.err.println("AppLoader: Error during application scanning: " + e.getMessage());
            e.printStackTrace();
        }
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