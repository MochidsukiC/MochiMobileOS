package jp.moyashi.phoneos.core.apps.settings;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.apps.settings.ui.SettingsScreen;
import processing.core.PGraphics;
import processing.core.PImage;

/**
 * MochiMobileOS用の設定アプリケーション。
 * システム設定と構成オプションへのアクセスを提供する。
 * これはランチャーとホーム画面機能のテストアプリケーションとして機能する。
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class SettingsApp implements IApplication {
    
    /** アプリケーションメタデータ */
    private static final String APP_ID = "jp.moyashi.phoneos.core.apps.settings";
    private static final String APP_NAME = "Settings";
    private static final String APP_VERSION = "1.0.0";
    private static final String APP_DESCRIPTION = "System settings and configuration";
    
    /** 初期化状態 */
    private boolean isInitialized = false;
    
    /**
     * 新しい設定アプリケーションインスタンスを作成する。
     */
    public SettingsApp() {
        System.out.println("SettingsApp: Settings application created");
    }
    
    /**
     * このアプリケーションの一意識別子を取得する。
     * 
     * @return アプリケーションID
     */
    @Override
    public String getApplicationId() {
        return APP_ID;
    }
    
    /**
     * このアプリケーションの表示名を取得する。
     * 
     * @return アプリケーション名
     */
    @Override
    public String getName() {
        return APP_NAME;
    }
    
    /**
     * このアプリケーションのバージョンを取得する。
     * 
     * @return アプリケーションバージョン
     */
    @Override
    public String getVersion() {
        return APP_VERSION;
    }
    
    /**
     * このアプリケーションの説明を取得する。
     * 
     * @return アプリケーション説明
     */
    @Override
    public String getDescription() {
        return APP_DESCRIPTION;
    }
    
    /**
     * このアプリケーションのアイコンを取得する。
     * 設定を表すシンプルな歯車状のアイコンを作成する。
     * 
     * @param p 描画操作用のPAppletインスタンス
     * @return アプリケーションアイコン
     */
    @Override
    public PImage getIcon(processing.core.PApplet p) {
        // Create graphics buffer for icon
        PGraphics icon = p.createGraphics(64, 64);
        
        icon.beginDraw();
        icon.background(0x666666); // Gray background
        icon.noStroke();
        
        // Draw gear shape
        icon.fill(0xFFFFFF); // White gear
        
        // Outer gear circle
        icon.ellipse(32, 32, 40, 40);
        
        // Inner hole
        icon.fill(0x666666);
        icon.ellipse(32, 32, 16, 16);
        
        // Gear teeth (simplified)
        icon.fill(0xFFFFFF);
        icon.rect(30, 8, 4, 12);   // Top
        icon.rect(30, 44, 4, 12);  // Bottom
        icon.rect(8, 30, 12, 4);   // Left
        icon.rect(44, 30, 12, 4);  // Right
        
        // Diagonal teeth
        icon.rect(18, 14, 8, 3);   // Top-left
        icon.rect(38, 14, 8, 3);   // Top-right
        icon.rect(18, 47, 8, 3);   // Bottom-left
        icon.rect(38, 47, 8, 3);   // Bottom-right
        
        icon.endDraw();
        
        return icon;
    }
    
    /**
     * このアプリケーションのエントリースクリーンを取得する。
     * 
     * @param kernel OSカーネルインスタンス
     * @return 設定アプリのメインスクリーン
     */
    @Override
    public Screen getEntryScreen(Kernel kernel) {
        System.out.println("SettingsApp: Creating settings screen");
        return new SettingsScreen(kernel, this);
    }
    
    /**
     * 設定アプリケーションを初期化する。
     * アプリケーションが最初に読み込まれるときに呼び出される。
     */
    @Override
    public void onInitialize(Kernel kernel) {
        if (!isInitialized) {
            isInitialized = true;
            System.out.println("SettingsApp: Settings application initialized");
        }
    }
    
    /**
     * 設定アプリケーションをクリーンアップする。
     * アプリケーションがアンロードされるときに呼び出される。
     */
    @Override
    public void onDestroy() {
        if (isInitialized) {
            isInitialized = false;
            System.out.println("SettingsApp: Settings application destroyed");
        }
    }
    
    /**
     * アプリケーションが初期化されているかどうかを確認する。
     * 
     * @return 初期化されている場合true、そうでなければfalse
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}