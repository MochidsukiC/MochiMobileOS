package jp.moyashi.phoneos.core.apps.settings;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.apps.settings.ui.SettingsScreen;
import jp.moyashi.phoneos.core.controls.ToggleItem;
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
     * コントロールセンター用のテストトグルも登録する。
     */
    @Override
    public void onInitialize(Kernel kernel) {
        if (!isInitialized) {
            isInitialized = true;
            System.out.println("SettingsApp: Settings application initialized");
            
            // コントロールセンター用テストトグルを作成・登録
            setupControlCenterItems(kernel);
            
            // 通知センター用テスト通知を作成・登録
            setupNotificationCenterTests(kernel);
        }
    }
    
    /**
     * コントロールセンター用のテストアイテムをセットアップする。
     * 
     * @param kernel OSカーネルインスタンス
     */
    private void setupControlCenterItems(Kernel kernel) {
        try {
            // ナイトビジョントグル
            ToggleItem nightVisionToggle = new ToggleItem(
                "settings.nightvision",
                "\u30ca\u30a4\u30c8\u30d3\u30b8\u30e7\u30f3",
                "\u753b\u9762\u3092\u6697\u3044\u30c6\u30fc\u30de\u306b\u5207\u308a\u66ff\u3048\u307e\u3059",
                false, // 初期状態: OFF
                (isOn) -> {
                    System.out.println("SettingsApp: Night vision " + (isOn ? "enabled" : "disabled"));
                    // TODO: 実際のナイトモード切り替え処理を実装
                }
            );
            
            // Wi-Fi切り替えトグル（シミュレーション）
            ToggleItem wifiToggle = new ToggleItem(
                "settings.wifi",
                "Wi-Fi",
                "\u30ef\u30a4\u30e4\u30ec\u30b9\u63a5\u7d9a\u3092\u7ba1\u7406\u3057\u307e\u3059",
                true, // 初期状態: ON
                (isOn) -> {
                    System.out.println("SettingsApp: Wi-Fi " + (isOn ? "connected" : "disconnected"));
                    // TODO: 実際のWi-Fi切り替え処理を実装
                }
            );
            
            // Bluetooth切り替えトグル（シミュレーション）
            ToggleItem bluetoothToggle = new ToggleItem(
                "settings.bluetooth",
                "Bluetooth",
                "\u30c7\u30d0\u30a4\u30b9\u9593\u306e\u8fd1\u8ddd\u96e2\u901a\u4fe1\u3092\u7ba1\u7406\u3057\u307e\u3059",
                false, // 初期状態: OFF
                (isOn) -> {
                    System.out.println("SettingsApp: Bluetooth " + (isOn ? "enabled" : "disabled"));
                    // TODO: 実際のBluetooth切り替え処理を実装
                }
            );
            
            // 機内モードトグル（シミュレーション）
            ToggleItem airplaneModeToggle = new ToggleItem(
                "settings.airplane",
                "\u6a5f\u5185\u30e2\u30fc\u30c9",
                "\u3059\u3079\u3066\u306e\u7121\u7dda\u901a\u4fe1\u3092\u30aa\u30d5\u306b\u3057\u307e\u3059",
                false, // 初期状態: OFF
                (isOn) -> {
                    System.out.println("SettingsApp: Airplane mode " + (isOn ? "enabled" : "disabled"));
                    // TODO: 実際の機内モード切り替え処理を実装
                    
                    // 機内モードONの場合、他の通信機能も自動的にOFFにする
                    if (isOn) {
                        wifiToggle.setOn(false);
                        bluetoothToggle.setOn(false);
                    }
                }
            );
            
            // コントロールセンターにトグルを追加
            if (kernel.getControlCenterManager() != null) {
                kernel.getControlCenterManager().addItem(nightVisionToggle);
                kernel.getControlCenterManager().addItem(wifiToggle);
                kernel.getControlCenterManager().addItem(bluetoothToggle);
                kernel.getControlCenterManager().addItem(airplaneModeToggle);
                
                System.out.println("SettingsApp: Added 4 test toggles to control center");
            } else {
                System.err.println("SettingsApp: ControlCenterManager is not available");
            }
            
        } catch (Exception e) {
            System.err.println("SettingsApp: Error setting up control center items: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 通知センター用のテスト通知をセットアップする。
     * 
     * @param kernel OSカーネルインスタンス
     */
    private void setupNotificationCenterTests(Kernel kernel) {
        try {
            if (kernel.getNotificationManager() != null) {
                // テスト通知を追加（NotificationManagerのコンストラクタで既に追加されているが、追加で設定アプリからも追加）
                kernel.getNotificationManager().addNotification(
                    "\u8a2d\u5b9a\u30a2\u30d7\u30ea", 
                    "\u901a\u77e5\u30c6\u30b9\u30c8", 
                    "\u8a2d\u5b9a\u30a2\u30d7\u30ea\u304b\u3089\u9001\u4fe1\u3055\u308c\u305f\u30c6\u30b9\u30c8\u901a\u77e5\u3067\u3059\u3002\u901a\u77e5\u30bb\u30f3\u30bf\u30fc\u304c\u6b63\u5e38\u306b\u52d5\u4f5c\u3057\u3066\u3044\u307e\u3059\u3002", 
                    1
                );
                
                System.out.println("SettingsApp: Added test notification to notification center");
            } else {
                System.err.println("SettingsApp: NotificationManager is not available");
            }
            
        } catch (Exception e) {
            System.err.println("SettingsApp: Error setting up notification center tests: " + e.getMessage());
            e.printStackTrace();
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