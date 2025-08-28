package jp.moyashi.phoneos.core;

import jp.moyashi.phoneos.core.service.*;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.ScreenManager;
import jp.moyashi.phoneos.core.ui.popup.PopupManager;
import jp.moyashi.phoneos.core.input.GestureManager;
import jp.moyashi.phoneos.core.apps.launcher.LauncherApp;
import jp.moyashi.phoneos.core.apps.settings.SettingsApp;
import processing.core.PApplet;

/**
 * スマートフォンOSの中核となるメインカーネル。
 * ProcessingのグラフィックスAPIを利用するためPAppletを継承している。
 * すべてのシステムサービスとScreenManagerを通じたGUIを管理する。
 * 
 * @author YourName
 * @version 1.0
 */
public class Kernel extends PApplet {
    
    /** UIと画面遷移を管理するスクリーンマネージャー */
    private ScreenManager screenManager;
    
    /** 仮想ファイルシステムサービス */
    private VFS vfs;
    
    /** 設定管理サービス */
    private SettingsManager settingsManager;
    
    /** システムクロックサービス */
    private SystemClock systemClock;
    
    /** アプリケーション読み込みサービス */
    private AppLoader appLoader;
    
    /** レイアウト管理サービス */
    private LayoutManager layoutManager;
    
    /** グローバルポップアップマネージャー */
    private PopupManager popupManager;
    
    /** Kernelレベルジェスチャーマネージャー */
    private GestureManager gestureManager;
    
    /**
     * setup()が呼ばれる前にProcessingの設定を行う。
     * ディスプレイサイズとレンダラーを設定する。
     */
    @Override
    public void settings() {
        size(400, 600);  // スマートフォンに似たアスペクト比
        System.out.println("📱 Kernel: Processing window configured (400x600)");
    }
    
    /**
     * OSカーネルとすべてのサービスを初期化する。
     * このメソッドはプログラム開始時に一度だけ呼ばれる。
     * すべてのコアサービスとスクリーンマネージャーのインスタンスを作成する。
     */
    @Override
    public void setup() {
        // 重要な修正: フレームレートを即座に設定
        frameRate(60);
        
        System.out.println("=== MochiMobileOS カーネル初期化 ===");
        System.out.println("Kernel: OSサービスを初期化中...");
        System.out.println("Kernel: フレームレートを60FPSに設定");
        
        // コアサービスの初期化
        System.out.println("  -> VFS（仮想ファイルシステム）作成中...");
        vfs = new VFS();
        
        System.out.println("  -> 設定マネージャー作成中...");
        settingsManager = new SettingsManager();
        
        System.out.println("  -> システムクロック作成中...");
        systemClock = new SystemClock();
        
        System.out.println("  -> アプリケーションローダー作成中...");
        appLoader = new AppLoader(vfs);
        
        // アプリケーションをスキャンして読み込む
        System.out.println("  -> 外部アプリケーションをスキャン中...");
        appLoader.scanForApps();
        
        System.out.println("  -> レイアウト管理サービス作成中...");
        layoutManager = new LayoutManager(vfs, appLoader);
        
        System.out.println("  -> グローバルポップアップマネージャー作成中...");
        popupManager = new PopupManager();
        
        System.out.println("  -> Kernelレベルジェスチャーマネージャー作成中...");
        gestureManager = new GestureManager();
        
        // 組み込みアプリケーションを登録
        System.out.println("  -> LauncherAppを登録中...");
        LauncherApp launcherApp = new LauncherApp();
        appLoader.registerApplication(launcherApp);
        launcherApp.onInitialize(this);
        
        System.out.println("  -> SettingsAppを登録中...");
        SettingsApp settingsApp = new SettingsApp();
        appLoader.registerApplication(settingsApp);
        settingsApp.onInitialize(this);
        
        System.out.println("Kernel: " + appLoader.getLoadedApps().size() + " 個のアプリケーションを登録");
        
        // スクリーンマネージャーを初期化してランチャーを初期画面に設定
        System.out.println("  -> スクリーンマネージャー作成中...");
        screenManager = new ScreenManager();
        System.out.println("✅ ScreenManager作成済み: " + (screenManager != null));
        
        System.out.println("▶️ LauncherAppを初期画面として開始中...");
        Screen launcherScreen = launcherApp.getEntryScreen(this);
        System.out.println("✅ LauncherApp画面取得済み: " + (launcherScreen != null));
        if (launcherScreen != null) {
            System.out.println("   画面タイトル: " + launcherScreen.getScreenTitle());
        }
        
        screenManager.pushScreen(launcherScreen);
        System.out.println("✅ 画面をScreenManagerにプッシュ済み");
        
        System.out.println("✅ Kernel: OS初期化完了！");
        System.out.println("    • LauncherAppが実行中");
        System.out.println("    • " + appLoader.getLoadedApps().size() + " 個のアプリケーションが利用可能");
        System.out.println("    • システムはユーザー操作に対応可能");
        System.out.println("=======================================");
    }
    
    /**
     * Processingによって継続的に呼ばれるメイン描画ループ。
     * スクリーンマネージャーを通じて現在の画面に描画を委譲する。
     */
    @Override
    public void draw() {
        // 何かが見えるように明るい背景を強制表示
        background(100, 200, 100); // 視認性確保のための明るい緑色
        
        // 詳細なデバッグログ出力
        /*
        if (frameCount <= 10 || frameCount % 60 == 0) {
            System.out.println("🎨 Kernel Frame " + frameCount + ": ScreenManager=" + (screenManager != null));
            if (screenManager != null) {
                System.out.println("   ScreenManager has current screen: " + (screenManager.getCurrentScreen() != null));
                if (screenManager.getCurrentScreen() != null) {
                    System.out.println("   Current screen: " + screenManager.getCurrentScreen().getScreenTitle());
                }
            }
        }

         */
        
        // Kernel draw()が呼ばれていることを確認するため常にデバッグ情報を描画
        fill(255, 255, 255);
        textAlign(LEFT, TOP);
        textSize(14);
        text("Kernel Frame: " + frameCount, 10, 10);
        text("ScreenManager: " + (screenManager != null), 10, 30);
        
        if (screenManager != null) {
            text("Has Screen: " + (screenManager.getCurrentScreen() != null), 10, 50);
            try {
                screenManager.draw(this);
            } catch (Exception e) {
                System.err.println("❌ ScreenManager draw error: " + e.getMessage());
                e.printStackTrace();
                // 大きなエラー表示
                fill(255, 0, 0);
                rect(50, height/2 - 50, width - 100, 100);
                fill(255, 255, 255);
                textAlign(CENTER, CENTER);
                textSize(18);
                text("画面エラー!", width/2, height/2 - 20);
                textSize(12);
                text("エラー: " + e.getMessage(), width/2, height/2);
                text("詳細はコンソールを確認", width/2, height/2 + 20);
            }
        } else {
            // 大きな読み込み中インジケーター
            fill(255, 255, 0);
            rect(50, height/2 - 30, width - 100, 60);
            fill(0);
            textAlign(CENTER, CENTER);
            textSize(18);
            text("スクリーンマネージャーなし!", width/2, height/2);
        }
        
        // ジェスチャーマネージャーの更新（長押し検出など）
        if (gestureManager != null) {
            gestureManager.update();
        }
        
        // ポップアップを最上位に描画（すべての描画の最後）
        if (popupManager != null) {
            popupManager.draw(this);
        }
    }
    
    /**
     * マウスプレスイベントを処理する。
     * スクリーンマネージャーを通じて現在の画面にイベント処理を委譲する。
     */
    @Override
    public void mousePressed() {
        System.out.println("Kernel: mousePressed at (" + mouseX + ", " + mouseY + ")");
        
        // 1. ポップアップマネージャーが先にイベントを処理
        if (popupManager != null && popupManager.handleMouseClick(mouseX, mouseY)) {
            System.out.println("Kernel: Popup handled mousePressed, stopping propagation");
            return;
        }
        
        // 2. ジェスチャーマネージャーでジェスチャー検出開始
        if (gestureManager != null) {
            gestureManager.handleMousePressed(mouseX, mouseY);
        }
        
        // 3. 従来のイベント処理（後方互換のため残す）
        if (screenManager != null) {
            screenManager.mousePressed(mouseX, mouseY);
        }
    }
    
    /**
     * マウスドラッグイベントを処理する。
     * スクリーンマネージャーを通じて現在の画面にイベント処理を委譲する。
     */
    @Override
    public void mouseDragged() {
        System.out.println("Kernel: mouseDragged at (" + mouseX + ", " + mouseY + ")");
        
        // 1. ポップアップ表示中はドラッグイベントをブロック
        if (popupManager != null && popupManager.hasActivePopup()) {
            System.out.println("Kernel: Popup active, ignoring mouseDragged");
            return;
        }
        
        // 2. ジェスチャーマネージャーでドラッグ処理
        if (gestureManager != null) {
            gestureManager.handleMouseDragged(mouseX, mouseY);
        }
        
        // 3. 従来のドラッグ処理（後方互換のため残す）
        if (screenManager != null) {
            screenManager.mouseDragged(mouseX, mouseY);
        }
    }
    
    /**
     * マウスリリースイベントを処理する。
     * スクリーンマネージャーを通じて現在の画面にイベント処理を委譲する。
     */
    @Override
    public void mouseReleased() {
        System.out.println("Kernel: mouseReleased at (" + mouseX + ", " + mouseY + ")");
        
        // 1. ジェスチャーマネージャーでリリース処理
        if (gestureManager != null) {
            gestureManager.handleMouseReleased(mouseX, mouseY);
        }
        
        // 2. 従来のリリース処理（後方互換のため残す）
        if (screenManager != null) {
            screenManager.mouseReleased(mouseX, mouseY);
        }
    }
    
    /**
     * 仮想ファイルシステムサービスを取得する。
     * @return VFSインスタンス
     */
    public VFS getVFS() {
        return vfs;
    }
    
    /**
     * 設定管理サービスを取得する。
     * @return SettingsManagerインスタンス
     */
    public SettingsManager getSettingsManager() {
        return settingsManager;
    }
    
    /**
     * システムクロックサービスを取得する。
     * @return SystemClockインスタンス
     */
    public SystemClock getSystemClock() {
        return systemClock;
    }
    
    /**
     * スクリーンマネージャーを取得する。
     * @return ScreenManagerインスタンス
     */
    public ScreenManager getScreenManager() {
        return screenManager;
    }
    
    /**
     * アプリケーションローダーサービスを取得する。
     * @return AppLoaderインスタンス
     */
    public AppLoader getAppLoader() {
        return appLoader;
    }
    
    /**
     * レイアウト管理サービスを取得する。
     * @return LayoutManagerインスタンス
     */
    public LayoutManager getLayoutManager() {
        return layoutManager;
    }
    
    /**
     * グローバルポップアップマネージャーを取得する。
     * @return PopupManagerインスタンス
     */
    public PopupManager getPopupManager() {
        return popupManager;
    }
    
    /**
     * Kernelレベルジェスチャーマネージャーを取得する。
     * @return GestureManagerインスタンス
     */
    public GestureManager getGestureManager() {
        return gestureManager;
    }
}