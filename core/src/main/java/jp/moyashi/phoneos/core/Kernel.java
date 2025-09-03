package jp.moyashi.phoneos.core;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.service.*;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.ScreenManager;
import jp.moyashi.phoneos.core.ui.popup.PopupManager;
import jp.moyashi.phoneos.core.input.GestureManager;
import jp.moyashi.phoneos.core.input.GestureListener;
import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import jp.moyashi.phoneos.core.apps.launcher.LauncherApp;
import jp.moyashi.phoneos.core.apps.settings.SettingsApp;
import jp.moyashi.phoneos.core.ui.LayerManager;
import processing.core.PApplet;
import processing.core.PFont;
import processing.event.MouseEvent;

/**
 * スマートフォンOSの中核となるメインカーネル。
 * ProcessingのグラフィックスAPIを利用するためPAppletを継承している。
 * すべてのシステムサービスとScreenManagerを通じたGUIを管理する。
 * コントロールセンター用のジェスチャー処理も担当する。
 * 
 * @author YourName
 * @version 1.0
 */
public class Kernel extends PApplet implements GestureListener {
    
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
    
    /** コントロールセンター管理サービス */
    private ControlCenterManager controlCenterManager;
    
    /** 通知センター管理サービス */
    private NotificationManager notificationManager;
    
    /** ロック状態管理サービス */
    private LockManager lockManager;
    
    /** 動的レイヤー管理システム */
    private LayerManager layerManager;
    
    /** 日本語フォント */
    private PFont japaneseFont;
    
    // ESCキー長押し検出用変数
    /** ESCキーが押されている時間 */
    private long escKeyPressTime = 0;
    
    /** ESCキーが現在押されているかどうか */
    private boolean escKeyPressed = false;
    
    /** 長押し判定時間（ミリ秒） */
    private static final long LONG_PRESS_DURATION = 2000; // 2秒
    
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
        
        // 日本語フォントの初期化
        System.out.println("=== MochiMobileOS カーネル初期化 ===");
        System.out.println("Kernel: 日本語フォントを設定中...");
        try {
            japaneseFont = createFont("Meiryo", 16, true);
            textFont(japaneseFont);
            System.out.println("Kernel: Meiryoフォントを正常に読み込みました");
        } catch (Exception e) {
            System.err.println("Kernel: Meiryoフォントの読み込みに失敗: " + e.getMessage());
            System.err.println("Kernel: デフォルトフォントを使用します");
        }
        
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
        
        System.out.println("  -> コントロールセンター管理サービス作成中...");
        controlCenterManager = new ControlCenterManager();
        controlCenterManager.setGestureManager(gestureManager);
        setupControlCenter();
        
        System.out.println("  -> 通知センター管理サービス作成中...");
        notificationManager = new NotificationManager();
        notificationManager.setKernel(this); // Kernelの参照を設定
        
        System.out.println("  -> ロック状態管理サービス作成中...");
        lockManager = new LockManager(settingsManager);
        
        System.out.println("  -> 動的レイヤー管理システム作成中...");
        layerManager = new LayerManager(gestureManager);
        
        // コントロールセンターを最高優先度のジェスチャーリスナーとして登録
        gestureManager.addGestureListener(controlCenterManager);
        
        // 通知センターを高優先度のジェスチャーリスナーとして登録
        gestureManager.addGestureListener(notificationManager);
        
        // Kernelを最低優先度のジェスチャーリスナーとして登録
        gestureManager.addGestureListener(this);
        
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
        
        // ロック状態に基づいて初期画面を決定
        if (lockManager.isLocked()) {
            System.out.println("▶️ OSがロック状態 - ロック画面を初期画面として開始中...");
            jp.moyashi.phoneos.core.ui.lock.LockScreen lockScreen = 
                new jp.moyashi.phoneos.core.ui.lock.LockScreen(this);
            screenManager.pushScreen(lockScreen);
            System.out.println("✅ ロック画面をScreenManagerにプッシュ済み");
        } else {
            System.out.println("▶️ OSがアンロック状態 - LauncherAppを初期画面として開始中...");
            Screen launcherScreen = launcherApp.getEntryScreen(this);
            System.out.println("✅ LauncherApp画面取得済み: " + (launcherScreen != null));
            if (launcherScreen != null) {
                System.out.println("   画面タイトル: " + launcherScreen.getScreenTitle());
            }
            
            screenManager.pushScreen(launcherScreen);
            System.out.println("✅ 画面をScreenManagerにプッシュ済み");
        }
        
        System.out.println("✅ Kernel: OS初期化完了！");
        if (lockManager.isLocked()) {
            System.out.println("    • ロック画面が表示されています");
            System.out.println("    • パターン入力でアンロックできます (デフォルト: L字型パターン)");
        } else {
            System.out.println("    • LauncherAppが実行中");
        }
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
        
        // レイヤー管理システムによる描画とジェスチャー優先度管理
        if (layerManager != null) {
            layerManager.updateAndRender(this);
        }
        
        // 従来のシステム描画（レイヤー管理に移行するまでの互換性維持）
        // TODO: すべてのコンポーネントをレイヤー管理システムに移行後、以下のコードを削除
        
        // 動的優先度を更新（描画順序に基づく）
        // DISABLED: ControlCenterManagerとNotificationManagerが独自に優先度を管理するため、
        // ここでの上書きを無効化。コントロールセンターが15000の高優先度を維持できるようになる。
        // updateDynamicPriorities();
        
        // 通知センターを描画（最初に、背景の一部として）
        if (notificationManager != null && !isComponentManagedByLayer("notification_center")) {
            notificationManager.draw(this);
        }
        
        // コントロールセンターを描画（通知センターの上に）
        if (controlCenterManager != null && !isComponentManagedByLayer("control_center")) {
            controlCenterManager.draw(this);
        }
        
        // ポップアップを最上位に描画（すべての描画の最後）
        if (popupManager != null && !isComponentManagedByLayer("popup")) {
            popupManager.draw(this);
        }
    }
    
    /**
     * マウスプレスイベントを処理する。
     * スクリーンマネージャーを通じて現在の画面にイベント処理を委譲する。
     */
    @Override
    public void mousePressed() {
        System.out.println("========================================");
        System.out.println("Kernel: mousePressed at (" + mouseX + ", " + mouseY + ")");
        System.out.println("ControlCenter visible: " + (controlCenterManager != null ? controlCenterManager.isVisible() : "null"));
        System.out.println("NotificationManager visible: " + (notificationManager != null ? notificationManager.isVisible() : "null"));
        System.out.println("========================================");
        
        // 1. ポップアップマネージャーが先にイベントを処理
        if (popupManager != null && popupManager.handleMouseClick(mouseX, mouseY)) {
            System.out.println("Kernel: Popup handled mousePressed, stopping propagation");
            return;
        }
        
        // 2. 通知センターが表示中の場合、そのイベント処理を優先
        if (notificationManager != null && notificationManager.isVisible()) {
            // ジェスチャーマネージャーを通じてイベントを処理する
            if (gestureManager != null) {
                gestureManager.handleMousePressed(mouseX, mouseY);
            }
            return;
        }
        
        // 3. コントロールセンターが表示中の場合、そのイベント処理を優先
        if (controlCenterManager != null && controlCenterManager.isVisible()) {
            // ジェスチャーマネージャーを通じてイベントを処理する
            if (gestureManager != null) {
                gestureManager.handleMousePressed(mouseX, mouseY);
            }
            return;
        }
        
        // 4. ジェスチャーマネージャーでジェスチャー検出開始（常に実行）
        if (gestureManager != null) {
            gestureManager.handleMousePressed(mouseX, mouseY);
        }
        
        // 4. 従来のイベント処理（後方互換のため残す）
        // ただし、ロック中、コントロールセンターや通知センターが表示中の場合はブロック
        if (screenManager != null && 
            (lockManager == null || !lockManager.isLocked()) &&
            (controlCenterManager == null || !controlCenterManager.isVisible()) &&
            (notificationManager == null || !notificationManager.isVisible())) {
            screenManager.mousePressed(mouseX, mouseY);
        } else if (lockManager != null && lockManager.isLocked()) {
            System.out.println("Kernel: Device is locked - mouse input handled by lock screen only");
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
        
        // 2. ジェスチャーマネージャーでドラッグ処理（常に実行）
        if (gestureManager != null) {
            gestureManager.handleMouseDragged(mouseX, mouseY);
        }
        
        // 3. 従来のドラッグ処理（後方互換のため残す）
        // ただし、ロック中、コントロールセンターや通知センターが表示中の場合はブロック
        if (screenManager != null && 
            (lockManager == null || !lockManager.isLocked()) &&
            (controlCenterManager == null || !controlCenterManager.isVisible()) &&
            (notificationManager == null || !notificationManager.isVisible())) {
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
        
        // 1. ジェスチャーマネージャーでリリース処理（常に実行）
        if (gestureManager != null) {
            gestureManager.handleMouseReleased(mouseX, mouseY);
        }
        
        // 2. 従来のリリース処理（後方互換のため残す）
        // ただし、ロック中、コントロールセンターや通知センターが表示中の場合はブロック
        if (screenManager != null && 
            (lockManager == null || !lockManager.isLocked()) &&
            (controlCenterManager == null || !controlCenterManager.isVisible()) &&
            (notificationManager == null || !notificationManager.isVisible())) {
            screenManager.mouseReleased(mouseX, mouseY);
        }
    }
    
    /**
     * Processingのmousewheel()メソッド（複数の実装を試す）
     */
    public void mouseWheel() {
        System.out.println("Kernel: mouseWheel() called (no args)!");
        if (mouseEvent != null) {
            float wheelRotation = mouseEvent.getCount();
            System.out.println("Kernel: wheelRotation = " + wheelRotation);
            handleMouseWheel((int)wheelRotation);
        } else {
            System.out.println("Kernel: mouseEvent is null!");
        }
    }
    
    /**
     * Processing 4.x用のmouseWheelメソッド
     */
    @Override
    public void mouseWheel(MouseEvent event) {
        System.out.println("Kernel: mouseWheel(MouseEvent) called!");
        float wheelRotation = event.getCount();
        System.out.println("Kernel: wheelRotation = " + wheelRotation);
        handleMouseWheel((int)wheelRotation);
    }
    
    /**
     * マウスホイールイベント処理。
     * ホイールスクロールをドラッグジェスチャーに変換してスクロール機能を提供する。
     */
    private void handleMouseWheel(int wheelRotation) {
        System.out.println("==========================================");
        System.out.println("Kernel: handleMouseWheel - rotation: " + wheelRotation + " at (" + mouseX + ", " + mouseY + ")");
        System.out.println("GestureManager: " + (gestureManager != null ? "exists" : "null"));
        System.out.println("==========================================");
        
        if (gestureManager != null && wheelRotation != 0) {
            // ホイールをドラッグジェスチャーとしてシミュレート
            int scrollAmount = wheelRotation * 30; // スクロール量を調整
            
            // ドラッグ開始をシミュレート
            gestureManager.handleMousePressed(mouseX, mouseY);
            
            // ドラッグ移動をシミュレート（Y軸方向のみ）
            gestureManager.handleMouseDragged(mouseX, mouseY + scrollAmount);
            
            // ドラッグ終了をシミュレート
            gestureManager.handleMouseReleased(mouseX, mouseY + scrollAmount);
            
            System.out.println("Kernel: Converted wheel scroll to drag gesture (scrollAmount: " + scrollAmount + ")");
        }
    }
    
    /**
     * キーボード入力イベントを処理する。
     * スペースキーでホーム画面に戻る機能を提供する。
     * ただし、ロック中はスペースキーを無効化する。
     */
    @Override
    public void keyPressed() {
        System.out.println("========================================");
        System.out.println("Kernel: keyPressed - key: '" + key + "', keyCode: " + keyCode);
        System.out.println("========================================");
        
        // ESCキーの処理
        if (keyCode == 27) { // ESC key code
            handleEscKeyPress();
            key = 0; // ProcessingのデフォルトESC動作を無効化
            return;
        }
        
        // スペースキー（ホームボタン）の処理
        if (key == ' ' || keyCode == 32) {
            if (lockManager != null && lockManager.isLocked()) {
                // ロック中：パターン入力エリアをハイライト表示
                System.out.println("Kernel: Home button pressed while locked - highlighting pattern input");
                highlightPatternInput();
                return;
            } else {
                // アンロック時：ホーム画面に戻る
                navigateToHome();
                return;
            }
        }
        
        // テスト用：すべてのキーコードをログ出力
        System.out.println("Kernel: Checking keyCode " + keyCode + " for special keys");
        
        // Page Up/Down キーでマウスホイールをシミュレート (複数のキーコードを試す)
        if (keyCode == 33 || keyCode == 366) { // Page Up キー (WindowsとJavaで異なる場合)
            System.out.println("Kernel: Page Up pressed - simulating wheel up");
            handleMouseWheel(-1); // 上向きスクロール
            return;
        }
        
        if (keyCode == 34 || keyCode == 367) { // Page Down キー
            System.out.println("Kernel: Page Down pressed - simulating wheel down");
            handleMouseWheel(1); // 下向きスクロール
            return;
        }
        
        // より簡単なテスト用キーを追加
        if (key == 'q' || key == 'Q') {
            System.out.println("Kernel: Q pressed - simulating wheel up");
            handleMouseWheel(-1);
            return;
        }
        
        if (key == 'e' || key == 'E') {
            System.out.println("Kernel: E pressed - simulating wheel down");
            handleMouseWheel(1);
            return;
        }
        
        // その他のキーイベントを現在の画面に委譲
        if (screenManager != null) {
            screenManager.keyPressed(key, keyCode);
        }
    }
    
    /**
     * キーリリースイベント処理。
     * ESCキーの長押し検出に使用される。
     */
    @Override
    public void keyReleased() {
        System.out.println("Kernel: keyReleased - key: " + key + ", keyCode: " + keyCode);
        
        // ESCキーのリリース処理
        if (keyCode == 27) { // ESC key code
            handleEscKeyRelease();
            key = 0; // ProcessingのデフォルトESC動作を無効化
        }
        
        // keyReleasedはScreenManagerでサポートされていないため、コメントアウト
        // if (screenManager != null) {
        //     screenManager.keyReleased(key, keyCode);
        // }
    }
    
    /**
     * ESCキープレス処理。
     * 長押し検出を開始する。
     */
    private void handleEscKeyPress() {
        if (!escKeyPressed) {
            escKeyPressed = true;
            escKeyPressTime = System.currentTimeMillis();
            System.out.println("Kernel: ESC key pressed - starting long press detection");
        }
    }
    
    /**
     * ESCキーリリース処理。
     * 短押し（ロック）か長押し（シャットダウン）かを判定する。
     */
    private void handleEscKeyRelease() {
        if (escKeyPressed) {
            escKeyPressed = false;
            long pressDuration = System.currentTimeMillis() - escKeyPressTime;
            
            System.out.println("Kernel: ESC key released after " + pressDuration + "ms");
            
            if (pressDuration >= LONG_PRESS_DURATION) {
                // 長押し：シャットダウン
                System.out.println("Kernel: ESC long press detected - initiating shutdown");
                handleShutdown();
            } else {
                // 短押し：ロック
                System.out.println("Kernel: ESC short press detected - locking device");
                handleDeviceLock();
            }
        }
    }
    
    /**
     * デバイスロック処理。
     * 現在のロック状態に関わらずロック画面を表示する。
     */
    private void handleDeviceLock() {
        System.out.println("Kernel: Locking device - switching to lock screen");
        
        if (lockManager != null) {
            lockManager.lock(); // デバイスをロック状態にする
            
            // ロック画面に切り替え
            try {
                jp.moyashi.phoneos.core.ui.lock.LockScreen lockScreen = 
                    new jp.moyashi.phoneos.core.ui.lock.LockScreen(this);
                
                // 現在の画面をクリアしてロック画面をプッシュ
                screenManager.clearAllScreens();
                screenManager.pushScreen(lockScreen);
                
                System.out.println("Kernel: Device locked successfully");
            } catch (Exception e) {
                System.err.println("Kernel: Error switching to lock screen: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * システムシャットダウン処理。
     */
    private void handleShutdown() {
        System.out.println("Kernel: Initiating system shutdown...");
        
        // シャットダウンメッセージを表示
        background(20, 25, 35);
        fill(255, 255, 255);
        textAlign(CENTER, CENTER);
        textSize(24);
        text("システムをシャットダウンしています...", width / 2, height / 2);
        
        // 少し遅延してから終了
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                System.out.println("Kernel: Shutdown complete");
                exit();
            } catch (InterruptedException e) {
                System.err.println("Kernel: Shutdown interrupted: " + e.getMessage());
            }
        }).start();
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
    
    /**
     * コントロールセンター管理サービスを取得する。
     * @return ControlCenterManagerインスタンス
     */
    public ControlCenterManager getControlCenterManager() {
        return controlCenterManager;
    }
    
    /**
     * 通知センター管理サービスのインスタンスを取得する。
     * 
     * @return 通知センターマネージャー
     */
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }
    
    /**
     * ロック状態管理サービスのインスタンスを取得する。
     * 
     * @return ロック管理サービス
     */
    public LockManager getLockManager() {
        return lockManager;
    }
    
    /**
     * 動的レイヤー管理システムのインスタンスを取得する。
     * 
     * @return レイヤーマネージャー
     */
    public LayerManager getLayerManager() {
        return layerManager;
    }
    
    /**
     * 日本語対応フォントを取得する。
     * 
     * @return 日本語フォント、初期化されていない場合はnull
     */
    public PFont getJapaneseFont() {
        return japaneseFont;
    }
    
    /**
     * Kernelレベルでのジェスチャーイベント処理。
     * 主に画面上からのスワイプダウンで通知センター、画面下からのスワイプアップでコントロールセンターを表示する処理を行う。
     * 
     * @param event ジェスチャーイベント
     * @return イベントを処理した場合true
     */
    @Override
    public boolean onGesture(GestureEvent event) {
        // 通知センターとコントロールセンターの処理はGestureManagerが自動的に優先度に基づいて処理するため、
        // ここでは手動チェックは不要
        
        // 画面上からのスワイプダウンで通知センターを表示
        if (event.getType() == GestureType.SWIPE_DOWN) {
            // 画面上部（高さの10%以下）からのスワイプダウンを検出
            if (event.getStartY() <= height * 0.1f) {
                System.out.println("Kernel: Detected swipe down from top at y=" + event.getStartY() + 
                                 ", showing notification center");
                if (notificationManager != null) {
                    notificationManager.show();
                    return true;
                }
            }
        }
        
        // 画面下からのスワイプアップでコントロールセンターを表示
        if (event.getType() == GestureType.SWIPE_UP) {
            // 画面下部（高さの90%以上）からのスワイプアップを検出
            if (event.getStartY() >= height * 0.9f) {
                System.out.println("Kernel: Detected swipe up from bottom at y=" + event.getStartY() + 
                                 ", showing control center");
                if (controlCenterManager != null) {
                    controlCenterManager.show();
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Kernelは画面全体を処理対象とする。
     * 
     * @param x X座標
     * @param y Y座標
     * @return 常にtrue
     */
    @Override
    public boolean isInBounds(int x, int y) {
        return true;
    }
    
    /**
     * Kernelの優先度は最低に設定する。
     * 他のリスナーがイベントを処理しなかった場合のみ処理される。
     * 
     * @return 最低優先度（-1000）
     */
    @Override
    public int getPriority() {
        return -1000;
    }
    
    /**
     * ホーム画面に戻る処理を実行する。
     * コントロールセンターの非表示、ホーム画面への遷移、ホーム画面内での最初のページへの移動を行う。
     */
    private void navigateToHome() {
        System.out.println("Kernel: Navigating to home screen");
        
        // 1. コントロールセンターが表示されている場合は閉じる
        if (controlCenterManager != null && controlCenterManager.isVisible()) {
            System.out.println("Kernel: Closing control center");
            controlCenterManager.hide();
            return;
        }
        
        // 2. 現在の画面を確認
        if (screenManager != null) {
            Screen currentScreen = screenManager.getCurrentScreen();
            
            if (currentScreen != null) {
                String currentScreenTitle = currentScreen.getScreenTitle();
                System.out.println("Kernel: Current screen: " + currentScreenTitle);
                
                // ホーム画面でない場合はホーム画面に戻る
                if (!"Home Screen".equals(currentScreenTitle)) {
                    // ホーム画面に戻る（LauncherAppを検索）
                    if (appLoader != null) {
                        IApplication launcherApp = findLauncherApp();
                        if (launcherApp != null) {
                            System.out.println("Kernel: Returning to home screen");
                            screenManager.clearAllScreens();
                            screenManager.pushScreen(launcherApp.getEntryScreen(this));
                        }
                    }
                } else {
                    // 既にホーム画面にいる場合は最初のページに戻る
                    if (currentScreen instanceof jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen) {
                        System.out.println("Kernel: Already on home screen, navigating to first page");
                        jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen homeScreen = 
                            (jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen) currentScreen;
                        homeScreen.navigateToFirstPage();
                    }
                }
            }
        }
    }
    
    /**
     * LauncherAppを検索して取得する。
     * 
     * @return LauncherAppのインスタンス、見つからない場合はnull
     */
    private IApplication findLauncherApp() {
        if (appLoader == null) return null;
        
        for (IApplication app : appLoader.getLoadedApps()) {
            if ("jp.moyashi.phoneos.core.apps.launcher".equals(app.getApplicationId())) {
                return app;
            }
        }
        return null;
    }
    
    /**
     * コントロールセンターに様々なアイテムを追加してセットアップする。
     */
    private void setupControlCenter() {
        if (controlCenterManager == null) {
            return;
        }
        
        System.out.println("  -> コントロールセンターアイテムを追加中...");
        
        // ToggleItemをimportするため
        jp.moyashi.phoneos.core.controls.ToggleItem toggleItem;
        
        // WiFi切り替え
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "wifi", "WiFi", "ワイヤレス接続のオン/オフ", 
            false, (isOn) -> System.out.println("WiFi toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // Bluetooth切り替え
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "bluetooth", "Bluetooth", "Bluetooth接続のオン/オフ", 
            false, (isOn) -> System.out.println("Bluetooth toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // 機内モード
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "airplane_mode", "機内モード", "すべての通信をオフにする", 
            false, (isOn) -> System.out.println("Airplane mode toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // モバイルデータ
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "mobile_data", "モバイルデータ", "携帯電話ネットワーク経由のデータ通信", 
            true, (isOn) -> System.out.println("Mobile data toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // 位置情報サービス
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "location", "位置情報", "GPS位置情報サービス", 
            true, (isOn) -> System.out.println("Location services toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // 自動回転
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "auto_rotate", "画面回転", "デバイスの向きに応じて画面を回転", 
            true, (isOn) -> System.out.println("Auto rotate toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // バッテリーセーバー
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "battery_saver", "バッテリーセーバー", "電力消費を抑制する省電力モード", 
            false, (isOn) -> System.out.println("Battery saver toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // ホットスポット
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "hotspot", "ホットスポット", "他のデバイスとの接続を共有", 
            false, (isOn) -> System.out.println("Hotspot toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // サイレントモード
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "silent_mode", "サイレント", "着信音と通知音をオフにする", 
            false, (isOn) -> System.out.println("Silent mode toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // ダークモード
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "dark_mode", "ダークモード", "暗い色調のテーマを使用", 
            false, (isOn) -> System.out.println("Dark mode toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        System.out.println("  -> " + controlCenterManager.getItemCount() + "個のコントロールアイテムを追加完了");
    }
    
    /**
     * 表示状態に応じて動的優先度を更新する。
     * レイヤーの表示順序に基づいて、最上位のレイヤーが最高優先度を持つ。
     */
    private void updateDynamicPriorities() {
        // ベース優先度
        int basePriority = 100;
        
        // 両方とも表示されていない場合のデフォルト優先度
        if ((notificationManager == null || !notificationManager.isVisible()) &&
            (controlCenterManager == null || !controlCenterManager.isVisible())) {
            // 通知センターとコントロールセンター両方が非表示の場合、デフォルト優先度を設定
            if (notificationManager != null) {
                notificationManager.setDynamicPriority(900);  // デフォルト高優先度
            }
            if (controlCenterManager != null) {
                controlCenterManager.setDynamicPriority(1000); // デフォルト最高優先度
            }
            return;
        }
        
        // 現在表示中のレイヤーに基づいて優先度を設定
        // 描画順序: 通知センター（先に描画/下層） -> コントロールセンター（後に描画/上層）
        
        if (notificationManager != null && notificationManager.isVisible()) {
            if (controlCenterManager != null && controlCenterManager.isVisible()) {
                // 両方表示中: コントロールセンターが上層なので高優先度
                notificationManager.setDynamicPriority(basePriority + 100); // 通知センター: 200
                controlCenterManager.setDynamicPriority(basePriority + 200); // コントロールセンター: 300
            } else {
                // 通知センターのみ表示中
                notificationManager.setDynamicPriority(basePriority + 200); // 通知センター: 300
            }
        }
        
        if (controlCenterManager != null && controlCenterManager.isVisible()) {
            if (notificationManager == null || !notificationManager.isVisible()) {
                // コントロールセンターのみ表示中
                controlCenterManager.setDynamicPriority(basePriority + 200); // コントロールセンター: 300
            }
            // 両方表示中の場合は上記で既に設定済み
        }
    }
    
    /**
     * パターン入力エリアをハイライト表示する。
     * ロック中にホームボタンが押された際に呼び出される。
     */
    private void highlightPatternInput() {
        // 現在の画面がロック画面の場合、パターンハイライト機能を呼び出す
        if (screenManager != null) {
            Screen currentScreen = screenManager.getCurrentScreen();
            if (currentScreen instanceof jp.moyashi.phoneos.core.ui.lock.LockScreen) {
                jp.moyashi.phoneos.core.ui.lock.LockScreen lockScreen = 
                    (jp.moyashi.phoneos.core.ui.lock.LockScreen) currentScreen;
                lockScreen.highlightPatternArea();
            }
        }
    }
    
    /**
     * 指定されたコンポーネントがレイヤー管理システムで管理されているかチェックする。
     * 
     * @param componentId コンポーネントID
     * @return レイヤー管理されている場合true
     */
    private boolean isComponentManagedByLayer(String componentId) {
        if (layerManager == null) return false;
        return layerManager.isLayerVisible(componentId);
    }
}