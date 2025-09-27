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
import jp.moyashi.phoneos.core.apps.calculator.CalculatorApp;
import jp.moyashi.phoneos.core.apps.appstore.AppStoreApp;
import jp.moyashi.phoneos.core.ui.LayerManager;
import jp.moyashi.phoneos.core.ui.UILayer;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PFont;
import processing.event.MouseEvent;

/**
 * スマートフォンOSの中核となるメインカーネル。
 * PGraphicsバッファに描画し、スタンドアロンとForgeの両方に対応。
 * すべてのシステムサービスとScreenManagerを通じたGUIを管理する。
 * コントロールセンター用のジェスチャー処理も担当する。
 *
 * @author YourName
 * @version 2.0
 */
public class Kernel implements GestureListener {

    /** 描画用のPGraphicsバッファ */
    private PGraphics graphics;

    /** PAppletインスタンス（フォントやリソース作成用） */
    private PApplet parentApplet;

    /** 画面の幅 */
    private int screenWidth = 400;

    /** 画面の高さ */
    private int screenHeight = 600;

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
     * Kernelを初期化する。PAppletインスタンスを受け取り、PGraphicsバッファを作成する。
     *
     * @param applet PAppletインスタンス（フォント作成やリソース管理用）
     */
    public void initialize(PApplet applet) {
        this.parentApplet = applet;
        this.graphics = applet.createGraphics(screenWidth, screenHeight);
        System.out.println("📱 Kernel: PGraphics buffer created (" + screenWidth + "x" + screenHeight + ")");

        // setup()の内容を呼び出し
        setup();
    }

    /**
     * Kernelを初期化する（Forge用：サイズ指定版）。
     *
     * @param applet PAppletインスタンス
     * @param width 画面幅
     * @param height 画面高さ
     */
    public void initialize(PApplet applet, int width, int height) {
        this.parentApplet = applet;
        this.screenWidth = width;
        this.screenHeight = height;
        this.graphics = applet.createGraphics(width, height);
        System.out.println("📱 Kernel: PGraphics buffer created (" + width + "x" + height + ")");

        // setup()の内容を呼び出し
        setup();
    }
    
    /**
     * OSカーネルとすべてのサービスを初期化する。
     * このメソッドはプログラム開始時に一度だけ呼ばれる。
     * すべてのコアサービスとスクリーンマネージャーのインスタンスを作成する。
     */
    private void setup() {
        // 日本語フォントの初期化
        System.out.println("=== MochiMobileOS カーネル初期化 ===");
        System.out.println("Kernel: 日本語フォントを設定中...");
        try {
            if (parentApplet != null) {
                japaneseFont = parentApplet.createFont("Meiryo", 16, true);
                System.out.println("Kernel: Meiryoフォントを正常に読み込みました");
            }
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

        // コントロールセンターをレイヤーマネージャーに登録
        registerControlCenterAsLayer();

        // 通知センターをレイヤーマネージャーに登録
        registerNotificationCenterAsLayer();

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
        
        System.out.println("  -> CalculatorAppを登録中...");
        CalculatorApp calculatorApp = new CalculatorApp();
        appLoader.registerApplication(calculatorApp);

        System.out.println("  -> AppStoreAppを登録中...");
        AppStoreApp appStoreApp = new AppStoreApp();
        appLoader.registerApplication(appStoreApp);
        appStoreApp.onInitialize(this);

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
     * メイン描画ループ。PGraphicsバッファに描画する。
     * スクリーンマネージャーを通じて現在の画面に描画を委譲する。
     */
    public void draw() {
        if (graphics == null) return;

        // フレームカウントを更新
        updateFrameCount();

        // PGraphicsバッファでの描画開始
        graphics.beginDraw();

        // 何かが見えるように明るい背景を強制表示
        graphics.background(100, 200, 100); // 視認性確保のための明るい緑色
        
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
        graphics.fill(255, 255, 255);
        graphics.textAlign(graphics.LEFT, graphics.TOP);
        graphics.textSize(14);
        if (japaneseFont != null) graphics.textFont(japaneseFont);
        graphics.text("Kernel Frame: " + getFrameCount(), 10, 10);
        graphics.text("ScreenManager: " + (screenManager != null), 10, 30);

        if (screenManager != null) {
            graphics.text("Has Screen: " + (screenManager.getCurrentScreen() != null), 10, 50);
            try {
                screenManager.draw(graphics);
            } catch (Exception e) {
                System.err.println("❌ ScreenManager draw error: " + e.getMessage());
                e.printStackTrace();
                // 大きなエラー表示
                graphics.fill(255, 0, 0);
                graphics.rect(50, screenHeight/2 - 50, screenWidth - 100, 100);
                graphics.fill(255, 255, 255);
                graphics.textAlign(graphics.CENTER, graphics.CENTER);
                graphics.textSize(18);
                graphics.text("画面エラー!", screenWidth/2, screenHeight/2 - 20);
                graphics.textSize(12);
                graphics.text("エラー: " + e.getMessage(), screenWidth/2, screenHeight/2);
                graphics.text("詳細はコンソールを確認", screenWidth/2, screenHeight/2 + 20);
            }
        } else {
            // 大きな読み込み中インジケーター
            graphics.fill(255, 255, 0);
            graphics.rect(50, screenHeight/2 - 30, screenWidth - 100, 60);
            graphics.fill(0);
            graphics.textAlign(graphics.CENTER, graphics.CENTER);
            graphics.textSize(18);
            graphics.text("スクリーンマネージャーなし!", screenWidth/2, screenHeight/2);
        }
        
        // ジェスチャーマネージャーの更新（長押し検出など）
        if (gestureManager != null) {
            gestureManager.update();
        }
        
        // レイヤー管理システムによる描画とジェスチャー優先度管理
        if (layerManager != null) {
            layerManager.updateAndRender(graphics);
        }

        // ポップアップを描画（通常の上位レイヤーとして）
        if (popupManager != null && !isComponentManagedByLayer("popup")) {
            if (parentApplet != null) {
                popupManager.draw(parentApplet);
            } else {
                popupManager.draw(graphics);
            }
        }

        // 従来のシステム描画（レイヤー管理に移行するまでの互換性維持）
        // TODO: すべてのコンポーネントをレイヤー管理システムに移行後、以下のコードを削除

        // 動的優先度を更新（描画順序に基づく）
        // DISABLED: ControlCenterManagerとNotificationManagerが独自に優先度を管理するため、
        // ここでの上書きを無効化。コントロールセンターが15000の高優先度を維持できるようになる。
        // updateDynamicPriorities();

        // 通知センターとコントロールセンターを最上位に描画（すべてのUI要素の上に表示）
        // これらは画面上のすべてのコンテンツの上に表示される必要がある

        // 通知センターを描画（最上位オーバーレイとして）
        boolean notificationManagedByLayer = isComponentManagedByLayer("notification_center");
        System.out.println("Kernel: Notification center overlay check - manager=" + (notificationManager != null) +
                         ", managedByLayer=" + notificationManagedByLayer +
                         ", parentApplet=" + (parentApplet != null) +
                         ", graphics=" + (graphics != null));
        if (notificationManager != null && !notificationManagedByLayer) {
            System.out.println("Kernel: Drawing notification center as top overlay (managedByLayer=" + notificationManagedByLayer + ")");
            try {
                if (parentApplet != null) {
                    System.out.println("Kernel: Calling notificationManager.draw(PApplet)");
                    notificationManager.draw(parentApplet);
                } else {
                    System.out.println("Kernel: Calling notificationManager.draw(PGraphics)");
                    notificationManager.draw(graphics);
                }
                System.out.println("Kernel: Notification center draw completed");
            } catch (Exception e) {
                System.err.println("Kernel: Error drawing notification center: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // コントロールセンターを描画（最上位オーバーレイとして、通知センターの上に）
        boolean controlCenterManagedByLayer = isComponentManagedByLayer("control_center");
        System.out.println("Kernel: Control center overlay check - manager=" + (controlCenterManager != null) +
                         ", managedByLayer=" + controlCenterManagedByLayer +
                         ", parentApplet=" + (parentApplet != null) +
                         ", graphics=" + (graphics != null));
        if (controlCenterManager != null && !controlCenterManagedByLayer) {
            System.out.println("Kernel: Drawing control center as top overlay (managedByLayer=" + controlCenterManagedByLayer + ")");
            try {
                if (parentApplet != null) {
                    System.out.println("Kernel: Calling controlCenterManager.draw(PApplet)");
                    controlCenterManager.draw(parentApplet);
                } else {
                    System.out.println("Kernel: Calling controlCenterManager.draw(PGraphics)");
                    controlCenterManager.draw(graphics);
                }
                System.out.println("Kernel: Control center draw completed");
            } catch (Exception e) {
                System.err.println("Kernel: Error drawing control center: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("Kernel: Skipping control center overlay - manager=" + (controlCenterManager != null) +
                             ", managedByLayer=" + controlCenterManagedByLayer);
        }

        // PGraphicsバッファでの描画終了
        graphics.endDraw();
    }

    /**
     * PGraphicsバッファを取得する。
     *
     * @return 描画されたPGraphicsバッファ
     */
    public PGraphics getGraphics() {
        return graphics;
    }

    /**
     * PGraphicsバッファを取得する（別名メソッド）。
     * Forgeモジュールとの互換性のため。
     *
     * @return PGraphicsバッファ
     */
    public PGraphics getGraphicsBuffer() {
        return graphics;
    }

    /**
     * 現在のピクセルデータを配列として取得する。
     * Forgeモジュールでテクスチャ変換に使用される。
     *
     * @return ピクセルデータ配列
     */
    public int[] getPixels() {
        if (graphics == null) {
            return new int[screenWidth * screenHeight];
        }

        graphics.loadPixels();
        return graphics.pixels.clone();
    }

    /**
     * Kernelのクリーンアップ処理。
     * リソースの解放を行う。
     */
    public void cleanup() {
        System.out.println("📱 Kernel: クリーンアップ処理開始...");

        if (layerManager != null) {
            System.out.println("  -> LayerManager cleanup...");
        }

        if (screenManager != null) {
            System.out.println("  -> ScreenManager cleanup...");
        }

        if (graphics != null) {
            System.out.println("  -> PGraphics バッファ解放...");
            graphics = null;
        }

        System.out.println("✅ Kernel: クリーンアップ完了");
    }

    /**
     * フレームカウンターを取得する（ダミー実装）。
     *
     * @return フレーム数（parentAppletがある場合はそのframeCount、ない場合は0）
     */
    public int getFrameCount() {
        return parentApplet != null ? parentApplet.frameCount : 0;
    }

    /**
     * 親PAppletインスタンスを取得する。
     * 新しいアーキテクチャでPAppletの機能が必要な場合に使用される。
     *
     * @return 親PAppletインスタンス（設定されていない場合はnull）
     */
    public PApplet getParentApplet() {
        return parentApplet;
    }

    /**
     * 画面サイズを取得する。
     *
     * @return 幅と高さの配列 [width, height]
     */
    public int[] getScreenSize() {
        return new int[]{screenWidth, screenHeight};
    }

    /**
     * 互換性のため、PAppletのような描画メソッドをKernelに追加。
     * これらのメソッドはPGraphicsに描画を委譲する。
     */
    public void background(int rgb) {
        if (graphics != null) {
            graphics.background(rgb);
        }
    }

    public void background(int r, int g, int b) {
        if (graphics != null) {
            graphics.background(r, g, b);
        }
    }

    public void fill(int rgb) {
        if (graphics != null) {
            graphics.fill(rgb);
        }
    }

    public void fill(int r, int g, int b) {
        if (graphics != null) {
            graphics.fill(r, g, b);
        }
    }

    public void fill(int r, int g, int b, int a) {
        if (graphics != null) {
            graphics.fill(r, g, b, a);
        }
    }

    public void stroke(int rgb) {
        if (graphics != null) {
            graphics.stroke(rgb);
        }
    }

    public void stroke(int r, int g, int b) {
        if (graphics != null) {
            graphics.stroke(r, g, b);
        }
    }

    public void strokeWeight(float weight) {
        if (graphics != null) {
            graphics.strokeWeight(weight);
        }
    }

    public void noStroke() {
        if (graphics != null) {
            graphics.noStroke();
        }
    }

    public void rect(float x, float y, float w, float h) {
        if (graphics != null) {
            graphics.rect(x, y, w, h);
        }
    }

    public void ellipse(float x, float y, float w, float h) {
        if (graphics != null) {
            graphics.ellipse(x, y, w, h);
        }
    }

    public void line(float x1, float y1, float x2, float y2) {
        if (graphics != null) {
            graphics.line(x1, y1, x2, y2);
        }
    }

    public void textAlign(int alignX) {
        if (graphics != null) {
            graphics.textAlign(alignX);
        }
    }

    public void textAlign(int alignX, int alignY) {
        if (graphics != null) {
            graphics.textAlign(alignX, alignY);
        }
    }

    public void textSize(float size) {
        if (graphics != null) {
            graphics.textSize(size);
        }
    }

    public void textFont(processing.core.PFont font) {
        if (graphics != null) {
            graphics.textFont(font);
        }
    }

    public void text(String str, float x, float y) {
        if (graphics != null) {
            graphics.text(str, x, y);
        }
    }

    public float textWidth(String str) {
        if (graphics != null) {
            return graphics.textWidth(str);
        }
        return 0;
    }

    public void pushMatrix() {
        if (graphics != null) {
            graphics.pushMatrix();
        }
    }

    public void popMatrix() {
        if (graphics != null) {
            graphics.popMatrix();
        }
    }

    public void translate(float x, float y) {
        if (graphics != null) {
            graphics.translate(x, y);
        }
    }

    public void scale(float s) {
        if (graphics != null) {
            graphics.scale(s);
        }
    }

    public void scale(float x, float y) {
        if (graphics != null) {
            graphics.scale(x, y);
        }
    }

    // 画面サイズプロパティ（互換性のため）
    public int width = screenWidth;
    public int height = screenHeight;

    // フレームカウント（互換性のため）
    public int frameCount = 0;

    /**
     * 描画時にフレームカウントを更新
     */
    private void updateFrameCount() {
        frameCount++;
        width = screenWidth;
        height = screenHeight;
    }

    /**
     * マウスプレスイベントを処理する。
     * スクリーンマネージャーを通じて現在の画面にイベント処理を委譲する。
     */
    public void mousePressed(int mouseX, int mouseY) {
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
    public void mouseDragged(int mouseX, int mouseY) {
        System.out.println("Kernel: mouseDragged at (" + mouseX + ", " + mouseY + ")");
        
        // 1. ポップアップ表示中はドラッグイベントをブロック
        if (popupManager != null && popupManager.isPopupVisible()) {
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
    public void mouseReleased(int mouseX, int mouseY) {
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
     * マウスホイールイベントを処理する。
     */
    public void mouseWheel(int wheelRotation, int mouseX, int mouseY) {
        System.out.println("Kernel: mouseWheel called with rotation: " + wheelRotation);
        handleMouseWheel(wheelRotation, mouseX, mouseY);
    }
    
    /**
     * マウスホイールイベント処理。
     * ホイールスクロールをドラッグジェスチャーに変換してスクロール機能を提供する。
     */
    private void handleMouseWheel(int wheelRotation, int mouseX, int mouseY) {
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
    public void keyPressed(char key, int keyCode, int mouseX, int mouseY) {
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
            handleMouseWheel(-1, mouseX, mouseY); // 上向きスクロール
            return;
        }

        if (keyCode == 34 || keyCode == 367) { // Page Down キー
            System.out.println("Kernel: Page Down pressed - simulating wheel down");
            handleMouseWheel(1, mouseX, mouseY); // 下向きスクロール
            return;
        }

        // より簡単なテスト用キーを追加
        if (key == 'q' || key == 'Q') {
            System.out.println("Kernel: Q pressed - simulating wheel up");
            handleMouseWheel(-1, mouseX, mouseY);
            return;
        }

        if (key == 'e' || key == 'E') {
            System.out.println("Kernel: E pressed - simulating wheel down");
            handleMouseWheel(1, mouseX, mouseY);
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
    public void keyReleased(char key, int keyCode) {
        System.out.println("Kernel: keyReleased - key: " + key + ", keyCode: " + keyCode);
        
        // ESCキーのリリース処理
        if (keyCode == 27) { // ESC key code
            handleEscKeyRelease();
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
        if (graphics != null) {
            graphics.beginDraw();
            graphics.background(20, 25, 35);
            graphics.fill(255, 255, 255);
            graphics.textAlign(graphics.CENTER, graphics.CENTER);
            graphics.textSize(24);
            if (japaneseFont != null) graphics.textFont(japaneseFont);
            graphics.text("システムをシャットダウンしています...", screenWidth / 2, screenHeight / 2);
            graphics.endDraw();
        }
        
        // 少し遅延してから終了
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                System.out.println("Kernel: Shutdown complete");
                if (parentApplet != null) {
                    parentApplet.exit();
                }
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
            if (event.getStartY() <= screenHeight * 0.1f) {
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
            if (event.getStartY() >= screenHeight * 0.9f) {
                System.out.println("Kernel: Detected swipe up from bottom at y=" + event.getStartY() +
                                 ", showing control center");
                if (controlCenterManager != null) {
                    System.out.println("Kernel: ControlCenterManager is not null, calling show()");
                    controlCenterManager.show();
                    System.out.println("Kernel: ControlCenterManager.show() completed");
                    return true;
                } else {
                    System.out.println("Kernel: ERROR - ControlCenterManager is null!");
                }
            }
        }

        // 現在のスクリーンにジェスチャーを委譲
        if (screenManager != null && screenManager.getCurrentScreen() != null) {
            Screen currentScreen = screenManager.getCurrentScreen();
            System.out.println("Kernel: Current screen is " + currentScreen.getClass().getSimpleName());
            if (currentScreen instanceof GestureListener) {
                System.out.println("Kernel: Delegating gesture " + event.getType() + " to " + currentScreen.getClass().getSimpleName());
                return ((GestureListener) currentScreen).onGesture(event);
            } else {
                System.out.println("Kernel: Current screen does not implement GestureListener");
            }
        } else {
            System.out.println("Kernel: No current screen available for gesture delegation");
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
     * PGraphics環境では通知センターとコントロールセンターはLayerManagerの更新が無効化されているため、
     * 直接描画を行う必要がある。
     *
     * @param componentId コンポーネントID
     * @return レイヤー管理されている場合true
     */
    /**
     * 通知センターをレイヤーマネージャーに登録する。
     */
    private void registerNotificationCenterAsLayer() {
        if (layerManager == null || notificationManager == null) {
            System.err.println("Kernel: Cannot register notification center - layerManager or notificationManager is null");
            return;
        }

        // 通知センター用のLayerRendererを作成
        UILayer.LayerRenderer notificationCenterRenderer = new UILayer.LayerRenderer() {
            @Override
            public void render(PApplet p) {
                if (notificationManager != null && notificationManager.isVisible()) {
                    notificationManager.draw(p);
                }
            }

            @Override
            public void render(PGraphics g) {
                if (notificationManager != null && notificationManager.isVisible()) {
                    notificationManager.draw(g);
                }
            }

            @Override
            public boolean isVisible() {
                return notificationManager != null && notificationManager.isVisible();
            }
        };

        // 通知センターレイヤーを登録（コントロールセンターより低い優先度）
        boolean registered = layerManager.requestLayerPermission(
            "notification_center",
            "Notification Center",
            8500, // 通知センターはコントロールセンター(9000)より低い優先度
            notificationCenterRenderer
        );

        if (registered) {
            System.out.println("Kernel: Notification center successfully registered as layer with priority 8500");
        } else {
            System.err.println("Kernel: Failed to register notification center as layer");
        }
    }

    /**
     * コントロールセンターをレイヤーマネージャーに登録する。
     */
    private void registerControlCenterAsLayer() {
        if (layerManager == null || controlCenterManager == null) {
            System.err.println("Kernel: Cannot register control center - layerManager or controlCenterManager is null");
            return;
        }

        // コントロールセンター用のLayerRendererを作成
        UILayer.LayerRenderer controlCenterRenderer = new UILayer.LayerRenderer() {
            @Override
            public void render(PApplet p) {
                if (controlCenterManager != null && controlCenterManager.isVisible()) {
                    controlCenterManager.draw(p);
                }
            }

            @Override
            public void render(PGraphics g) {
                if (controlCenterManager != null && controlCenterManager.isVisible()) {
                    controlCenterManager.draw(g);
                }
            }

            @Override
            public boolean isVisible() {
                return controlCenterManager != null && controlCenterManager.isVisible();
            }
        };

        // コントロールセンターレイヤーを登録（最高優先度）
        boolean registered = layerManager.requestLayerPermission(
            "control_center",
            "Control Center",
            9000, // コントロールセンターは最高優先度
            controlCenterRenderer
        );

        if (registered) {
            System.out.println("Kernel: Control center successfully registered as layer with priority 9000");
        } else {
            System.err.println("Kernel: Failed to register control center as layer");
        }
    }

    private boolean isComponentManagedByLayer(String componentId) {
        if (layerManager == null) return false;

        // コントロールセンターがレイヤーとして登録されているかどうかを確認
        if ("control_center".equals(componentId)) {
            return layerManager.isLayerVisible("control_center") ||
                   layerManager.getLayerCount() > 0; // レイヤーが存在すればレイヤー管理されている
        }

        // 通知センターがレイヤーとして登録されているかどうかを確認
        if ("notification_center".equals(componentId)) {
            return layerManager.isLayerVisible("notification_center") ||
                   layerManager.getLayerCount() > 0; // レイヤーが存在すればレイヤー管理されている
        }

        return layerManager.isLayerVisible(componentId);
    }
}