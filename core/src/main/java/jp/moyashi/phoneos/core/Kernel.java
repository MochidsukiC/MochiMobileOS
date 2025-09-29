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
import jp.moyashi.phoneos.core.ui.LayerManager;
import jp.moyashi.phoneos.core.coordinate.CoordinateTransform;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PFont;
import java.util.ArrayList;
import java.util.List;

/**
 * スマートフォンOSの中核となるメインカーネル。
 * PGraphics統一アーキテクチャに基づき、PApplet継承を廃止してPGraphicsバッファのみで動作する。
 * すべてのシステムサービスとScreenManagerを通じたGUIを管理する。
 * コントロールセンター用のジェスチャー処理も担当する。
 *
 * PGraphics統一アーキテクチャ:
 * - coreモジュールではPAppletを使用せず、PGraphicsバッファのみで描画
 * - 各サブモジュール（standalone/forge）でPGraphicsを環境別に変換
 *
 * @author YourName
 * @version 2.0 (PGraphics統一アーキテクチャ対応)
 */
public class Kernel implements GestureListener {
    
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

    /** 統一座標変換システム */
    private CoordinateTransform coordinateTransform;

    /** PGraphics描画バッファ（PGraphics統一アーキテクチャ） */
    private PGraphics graphics;

    /** PAppletインスタンス（PGraphics作成用、描画には使用しない） */
    private PApplet parentApplet;

    /** 画面幅 */
    public int width = 400;

    /** 画面高さ */
    public int height = 600;

    /** フレームカウント */
    public int frameCount = 0;

    /** 日本語フォント */
    private PFont japaneseFont;
    
    // ESCキー長押し検出用変数
    /** ESCキーが押されている時間 */
    private long escKeyPressTime = 0;
    
    /** ESCキーが現在押されているかどうか */
    private boolean escKeyPressed = false;

    // ホームボタン動的優先順位システム
    /** レイヤー種別定義 */
    public enum LayerType {
        HOME_SCREEN,    // ホーム画面（最下層）
        APPLICATION,    // アプリケーション
        NOTIFICATION,   // 通知センター
        CONTROL_CENTER, // コントロールセンター
        POPUP,          // ポップアップ（最上層）
        LOCK_SCREEN     // ロック画面（例外、閉じられない）
    }

    /** 現在開いているレイヤーのスタック（後から開いたものが末尾、つまり高い優先度） */
    private List<LayerType> layerStack;
    
    /** 長押し判定時間（ミリ秒） */
    private static final long LONG_PRESS_DURATION = 2000; // 2秒

    // =========================================================================
    // PGraphics統一アーキテクチャ：独立イベントAPI
    // サブモジュールがこれらのメソッドを呼び出してKernelを操作
    // =========================================================================

    /**
     * フレーム更新処理を実行（独立API）。
     * 各サブモジュールが適切なタイミングでこのメソッドを呼び出す。
     */
    public void update() {
        frameCount++;

        // ESCキー長押し検出の更新
        if (escKeyPressed) {
            long elapsedTime = System.currentTimeMillis() - escKeyPressTime;
            if (elapsedTime >= LONG_PRESS_DURATION) {
                System.out.println("Kernel: ESCキー長押し検出 - シャットダウン開始");
                shutdown();
                escKeyPressed = false;
            }
        }
    }

    /**
     * PGraphicsバッファに描画を実行（独立API）。
     * すべての描画処理をPGraphicsバッファに対して実行し、サブモジュールが結果を取得可能にする。
     */
    public void render() {
        if (graphics == null) {
            System.err.println("Kernel: PGraphicsバッファが初期化されていません");
            return;
        }

        // PGraphicsバッファへの描画開始
        graphics.beginDraw();

        try {
            // まず背景を描画（重要：Screenが背景を描画しない場合のために）
            graphics.background(0, 0, 0); // 黒背景

            // スクリーンマネージャーによる通常描画
            if (screenManager != null) {
                try {
                    screenManager.draw(graphics);
                } catch (Exception e) {
                    System.err.println("Kernel: ScreenManager描画エラー: " + e.getMessage());

                    // エラー時のフォールバック表示
                    graphics.background(50, 50, 50); // ダークグレー背景
                    graphics.fill(255, 0, 0);
                    graphics.rect(50, height/2 - 50, width - 100, 100);
                    graphics.fill(255, 255, 255);
                    graphics.textAlign(PApplet.CENTER, PApplet.CENTER);
                    graphics.textSize(18);
                    graphics.text("画面エラー!", width/2, height/2 - 20);
                    graphics.textSize(12);
                    graphics.text("エラー: " + e.getMessage(), width/2, height/2);
                }
            } else {
                // ScreenManagerが未初期化の場合の表示
                graphics.background(30, 30, 30); // ダークグレー背景
                graphics.fill(255, 255, 255);
                graphics.textAlign(PApplet.CENTER, PApplet.CENTER);
                graphics.textSize(16);
                graphics.text("システム初期化中...", width/2, height/2);
            }

            // 通知センターの描画
            if (notificationManager != null) {
                try {
                    notificationManager.draw(graphics);
                } catch (Exception e) {
                    System.err.println("Kernel: NotificationManager描画エラー: " + e.getMessage());
                }
            }

            // コントロールセンターの描画
            if (controlCenterManager != null) {
                try {
                    controlCenterManager.draw(graphics);
                } catch (Exception e) {
                    System.err.println("Kernel: ControlCenterManager描画エラー: " + e.getMessage());
                }
            }

            // ポップアップの描画
            if (popupManager != null) {
                try {
                    popupManager.draw(graphics);
                } catch (Exception e) {
                    System.err.println("Kernel: PopupManager描画エラー: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("Kernel: 描画処理中にエラーが発生: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // PGraphicsバッファへの描画終了
            graphics.endDraw();
        }
    }

    /**
     * マウスクリック処理（独立API）。
     *
     * @param x マウスX座標
     * @param y マウスY座標
     */
    public void mousePressed(int x, int y) {
        System.out.println("Kernel: mousePressed at (" + x + ", " + y + ")");

        try {
            // ポップアップの処理を優先
            if (popupManager != null && popupManager.hasActivePopup()) {
                boolean popupHandled = popupManager.handleMouseClick(x, y);
                if (popupHandled) {
                    System.out.println("Kernel: Popup handled mousePressed, stopping propagation");
                    return;
                }
            }

            // ジェスチャーマネージャーでの処理
            if (gestureManager != null) {
                boolean gestureHandled = gestureManager.handleMousePressed(x, y);
                if (gestureHandled) {
                    System.out.println("Kernel: Gesture handled mousePressed, stopping propagation");
                    return;
                }
            }

            // スクリーンマネージャーでの処理
            if (screenManager != null) {
                screenManager.mousePressed(x, y);
            }
        } catch (Exception e) {
            System.err.println("Kernel: mousePressed処理エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * マウス離し処理（独立API）。
     *
     * @param x マウスX座標
     * @param y マウスY座標
     */
    public void mouseReleased(int x, int y) {
        System.out.println("Kernel: mouseReleased at (" + x + ", " + y + ")");

        try {
            // ジェスチャーマネージャーでの処理
            if (gestureManager != null) {
                gestureManager.handleMouseReleased(x, y);
            }

            // スクリーンマネージャーでの処理
            if (screenManager != null) {
                screenManager.mouseReleased(x, y);
            }
        } catch (Exception e) {
            System.err.println("Kernel: mouseReleased処理エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * マウスドラッグ処理（独立API）。
     * ジェスチャー認識にとって重要な機能です。
     *
     * @param x マウスX座標
     * @param y マウスY座標
     */
    public void mouseDragged(int x, int y) {
        System.out.println("Kernel: mouseDragged at (" + x + ", " + y + ")");

        try {
            // ポップアップの処理は現在mouseDraggedをサポートしていないため、スキップ

            // ジェスチャーマネージャーでの処理（最重要）
            if (gestureManager != null) {
                gestureManager.handleMouseDragged(x, y);
                System.out.println("Kernel: Gesture processed mouseDragged");
            }

            // スクリーンマネージャーでの処理
            if (screenManager != null) {
                screenManager.mouseDragged(x, y);
            }
        } catch (Exception e) {
            System.err.println("Kernel: mouseDragged処理エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * キー押下処理（独立API）。
     *
     * @param key 押されたキー文字
     * @param keyCode キーコード
     */
    public void keyPressed(char key, int keyCode) {
        System.out.println("Kernel: keyPressed - key: '" + key + "', keyCode: " + keyCode);

        try {
            // ESCキーの特別処理
            if (keyCode == 27) { // ESC key code
                escKeyPressed = true;
                escKeyPressTime = System.currentTimeMillis();
                return;
            }

            // 'q'または'Q'でアプリ終了
            if (key == 'q' || key == 'Q') {
                System.out.println("Kernel: Q key pressed - initiating shutdown");
                shutdown();
                return;
            }

            // 'e'または'E'でテストエラー
            if (key == 'e' || key == 'E') {
                System.out.println("Kernel: E key pressed - testing error handling");
                throw new RuntimeException("Test error triggered by user");
            }

            // スペースキー（ホームボタン）の階層管理処理
            if (key == ' ' || keyCode == 32) {
                System.out.println("Kernel: Space key pressed - checking lock screen status");

                // ロック画面が表示されている場合は、ロック画面に処理を委譲
                if (layerStack.contains(LayerType.LOCK_SCREEN)) {
                    System.out.println("Kernel: Lock screen is active - forwarding space key to screen manager");
                    if (screenManager != null) {
                        screenManager.keyPressed(key, keyCode);
                    }
                    return;
                }

                // ロック画面が表示されていない場合は、通常のホームボタン処理
                System.out.println("Kernel: Space key pressed - handling home button");
                handleHomeButton();
                return;
            }

            // 通常のキー処理をスクリーンマネージャーに転送
            if (screenManager != null) {
                screenManager.keyPressed(key, keyCode);
            }
        } catch (Exception e) {
            System.err.println("Kernel: keyPressed処理エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * キー離し処理（独立API）。
     *
     * @param key 離されたキー文字
     * @param keyCode キーコード
     */
    public void keyReleased(char key, int keyCode) {
        System.out.println("Kernel: keyReleased - key: '" + key + "', keyCode: " + keyCode);

        // ESCキーの処理
        if (keyCode == 27) { // ESC key code
            escKeyPressed = false;
            return;
        }
    }

    /**
     * PGraphicsバッファを取得（独立API）。
     * サブモジュールがこのバッファの内容を各環境で描画する。
     *
     * @return PGraphicsバッファインスタンス
     */
    public PGraphics getGraphics() {
        return graphics;
    }

    /**
     * PGraphicsバッファのピクセル配列を取得（独立API）。
     * forge等でピクセルレベルでの処理が必要な場合に使用。
     *
     * @return ピクセル配列
     */
    public int[] getPixels() {
        if (graphics == null) {
            return new int[width * height];
        }
        graphics.loadPixels();
        return graphics.pixels.clone();
    }

    // =========================================================================
    // 以下、旧PAppletベースのメソッド（段階的に削除予定）
    // =========================================================================

    /**
     * Kernelを初期化する（PGraphics統一アーキテクチャ）。
     * PAppletインスタンスを受け取り、PGraphicsバッファを作成して初期化を行う。
     *
     * @param applet PGraphics作成用のPAppletインスタンス
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     */
    public void initialize(PApplet applet, int screenWidth, int screenHeight) {
        this.parentApplet = applet;
        this.width = screenWidth;
        this.height = screenHeight;

        System.out.println("=== MochiMobileOS カーネル初期化 ===");
        System.out.println("📱 Kernel: PGraphics buffer created (" + width + "x" + height + ")");

        // PGraphicsバッファを作成
        this.graphics = applet.createGraphics(width, height);

        // 内部初期化を実行
        setup();
    }

    /**
     * Minecraft環境用の初期化（forge用）。
     * 最小限のPAppletインスタンスでPGraphicsバッファを作成する。
     *
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     */
    public void initializeForMinecraft(int screenWidth, int screenHeight) {
        // 最小限のPAppletインスタンスを作成
        this.parentApplet = new PApplet();
        this.width = screenWidth;
        this.height = screenHeight;

        System.out.println("=== MochiMobileOS カーネル初期化 (Minecraft環境) ===");
        System.out.println("📱 Kernel: PGraphics buffer created (" + width + "x" + height + ")");

        // PGraphicsバッファを作成
        this.graphics = parentApplet.createGraphics(width, height);

        // 内部初期化を実行
        setup();
    }

    /**
     * OSカーネルとすべてのサービスを初期化する（内部メソッド）。
     * PGraphics統一アーキテクチャ対応版。
     */
    private void setup() {
        // 日本語フォントの初期化
        System.out.println("Kernel: 日本語フォントを設定中...");
        try {
            japaneseFont = parentApplet.createFont("Meiryo", 16, true);
            System.out.println("Kernel: Meiryoフォントを正常に読み込みました");
        } catch (Exception e) {
            System.err.println("Kernel: Meiryoフォントの読み込みに失敗: " + e.getMessage());
            System.err.println("Kernel: デフォルトフォントを使用します");
        }
        
        System.out.println("Kernel: OSサービスを初期化中...");
        System.out.println("Kernel: フレームレートを60FPSに設定");

        // 動的レイヤー管理システムを初期化
        System.out.println("  -> 動的レイヤー管理システム作成中...");
        layerStack = new ArrayList<>();
        layerStack.add(LayerType.HOME_SCREEN); // 最初は常にホーム画面

        // 統一座標変換システムを初期化
        System.out.println("  -> 統一座標変換システム作成中...");
        coordinateTransform = new CoordinateTransform(width, height);

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
        controlCenterManager.setCoordinateTransform(coordinateTransform);
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
        
        System.out.println("  -> CalculatorAppを登録中...");
        CalculatorApp calculatorApp = new CalculatorApp();
        appLoader.registerApplication(calculatorApp);
        
        System.out.println("Kernel: " + appLoader.getLoadedApps().size() + " 個のアプリケーションを登録");
        
        // スクリーンマネージャーを初期化してランチャーを初期画面に設定
        System.out.println("  -> スクリーンマネージャー作成中...");
        screenManager = new ScreenManager();
        System.out.println("✅ ScreenManager作成済み: " + (screenManager != null));

        // ScreenManagerにKernelインスタンスを設定（レイヤー管理統合のため）
        screenManager.setKernel(this);

        // ScreenManagerにPAppletを設定（画面のsetup()に必要）
        System.out.println("  -> ScreenManagerにPAppletを設定中...");
        screenManager.setCurrentPApplet(parentApplet);
        System.out.println("✅ ScreenManagerのPApplet設定完了");
        
        // ロック状態に基づいて初期画面を決定
        if (lockManager.isLocked()) {
            System.out.println("▶️ OSがロック状態 - ロック画面を初期画面として開始中...");
            jp.moyashi.phoneos.core.ui.lock.LockScreen lockScreen =
                new jp.moyashi.phoneos.core.ui.lock.LockScreen(this);
            screenManager.pushScreen(lockScreen);
            addLayer(LayerType.LOCK_SCREEN); // レイヤースタックに追加
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
    
    // 旧draw()メソッドは削除済み - render()メソッドを使用してください
    
    // 旧mousePressed()メソッドは削除済み - mousePressed(int x, int y)を使用してください
    
    // 旧mouseDragged()メソッドは削除済み - 必要に応じて独立APIを実装してください
    
    // 旧mouseReleased()メソッドは削除済み - mouseReleased(int x, int y)を使用してください
    
    // 旧mouseWheel()メソッドは削除済み - 必要に応じて独立APIを実装してください
    
    // 旧mouseWheel(MouseEvent event)メソッドは削除済み - 必要に応じて独立APIを実装してください
    
    /**
     * マウスホイールイベント処理。
     * ホイールスクロールをドラッグジェスチャーに変換してスクロール機能を提供する。
     * 注意: PAppletグローバル変数(mouseX, mouseY)への依存を除去する必要があります。
     */
    private void handleMouseWheel(int wheelRotation) {
        System.out.println("==========================================");
        System.out.println("Kernel: handleMouseWheel - rotation: " + wheelRotation);
        System.out.println("GestureManager: " + (gestureManager != null ? "exists" : "null"));
        System.out.println("==========================================");

        if (gestureManager != null && wheelRotation != 0) {
            // ホイールをドラッグジェスチャーとしてシミュレート
            int scrollAmount = wheelRotation * 30; // スクロール量を調整

            // 画面中央の座標を使用（mouseX, mouseYの代替）
            int centerX = width / 2;
            int centerY = height / 2;

            // ドラッグ開始をシミュレート
            gestureManager.handleMousePressed(centerX, centerY);

            // ドラッグ移動をシミュレート（Y軸方向のみ）
            gestureManager.handleMouseDragged(centerX, centerY + scrollAmount);

            // ドラッグ終了をシミュレート
            gestureManager.handleMouseReleased(centerX, centerY + scrollAmount);

            System.out.println("Kernel: Converted wheel scroll to drag gesture (scrollAmount: " + scrollAmount + ")");
        }
    }
    
    // 旧keyPressed()メソッドは削除済み - keyPressed(char key, int keyCode)を使用してください
    
    // 旧keyReleased()メソッドは削除済み - keyReleased(char key, int keyCode)を使用してください
    
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
                addLayer(LayerType.LOCK_SCREEN); // レイヤースタックに追加

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
        shutdown();
    }

    /**
     * システムシャットダウン処理（独立API）。
     */
    public void shutdown() {
        System.out.println("Kernel: System shutdown requested");

        // シャットダウンメッセージをPGraphicsバッファに描画
        if (graphics != null) {
            graphics.beginDraw();
            graphics.background(20, 25, 35);
            graphics.fill(255, 255, 255);
            graphics.textAlign(PApplet.CENTER, PApplet.CENTER);
            graphics.textSize(24);
            graphics.text("システムをシャットダウンしています...", width / 2, height / 2);
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
                System.exit(0);
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
     * 統一座標変換システムのインスタンスを取得する。
     *
     * @return 統一座標変換システム
     */
    public CoordinateTransform getCoordinateTransform() {
        return coordinateTransform;
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
     * ホームボタン（スペースキー）の動的階層管理処理。
     * 現在開いているレイヤーの順序を動的に判定し、最後に開いたレイヤーから閉じる。
     * アプリケーションが閉じられる場合はホームスクリーンに移行する。
     *
     * 例外: ロック画面は閉じられない（デバッグスクリーンが出るため）
     */
    private void handleHomeButton() {
        System.out.println("Kernel: Home button pressed - dynamic layer management");
        System.out.println("Kernel: Current layer stack: " + layerStack);

        try {
            // 1. 動的に最上位の閉じられるレイヤーを取得
            LayerType topLayer = getTopMostClosableLayer();

            if (topLayer == null) {
                System.out.println("Kernel: No closable layers found - already at lowest layer");
                return;
            }

            System.out.println("Kernel: Closing top layer: " + topLayer);

            // 2. レイヤータイプに応じて適切な閉じる処理を実行
            switch (topLayer) {
                case POPUP:
                    if (popupManager != null && popupManager.hasActivePopup()) {
                        popupManager.closeCurrentPopup();
                        removeLayer(LayerType.POPUP);
                        System.out.println("Kernel: Popup closed");
                    }
                    break;

                case CONTROL_CENTER:
                    if (controlCenterManager != null && controlCenterManager.isVisible()) {
                        controlCenterManager.hide();
                        removeLayer(LayerType.CONTROL_CENTER);
                        System.out.println("Kernel: Control center closed");
                    }
                    break;

                case NOTIFICATION:
                    if (notificationManager != null && notificationManager.isVisible()) {
                        notificationManager.hide();
                        removeLayer(LayerType.NOTIFICATION);
                        System.out.println("Kernel: Notification center closed");
                    }
                    break;

                case APPLICATION:
                    // アプリケーションを閉じてホーム画面に移行
                    System.out.println("Kernel: Closing application and returning to home screen");
                    navigateToHome();
                    removeLayer(LayerType.APPLICATION);
                    break;

                default:
                    System.out.println("Kernel: Unknown layer type: " + topLayer);
                    break;
            }

        } catch (Exception e) {
            System.err.println("Kernel: handleHomeButton処理エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * レイヤーがスタックに追加される際に呼び出される。
     * レイヤーの開いた順序を記録し、動的優先順位システムに反映する。
     *
     * @param layerType 追加されるレイヤー種別
     */
    public void addLayer(LayerType layerType) {
        // 既に存在する場合は移除して最上位に移動
        layerStack.remove(layerType);
        layerStack.add(layerType);

        System.out.println("Kernel: Layer '" + layerType + "' added to stack. Current stack: " + layerStack);
    }

    /**
     * レイヤーがスタックから削除される際に呼び出される。
     *
     * @param layerType 削除されるレイヤー種別
     */
    public void removeLayer(LayerType layerType) {
        boolean removed = layerStack.remove(layerType);
        if (removed) {
            System.out.println("Kernel: Layer '" + layerType + "' removed from stack. Current stack: " + layerStack);
        }
    }

    /**
     * 現在最上位の閉じられるレイヤーを取得する。
     * ロック画面とホーム画面は閉じられない（ロック画面はデバッグスクリーンが出るため、ホーム画面は最下層のため）。
     *
     * @return 最上位の閉じられるレイヤー種別、閉じられるレイヤーがない場合はnull
     */
    public LayerType getTopMostClosableLayer() {
        // スタックを逆順で検索（最後に追加されたものから）
        for (int i = layerStack.size() - 1; i >= 0; i--) {
            LayerType layer = layerStack.get(i);

            // ロック画面は閉じられない（デバッグスクリーン防止）
            if (layer == LayerType.LOCK_SCREEN) {
                continue;
            }

            // ホーム画面は最下層なので、これに到達した場合は閉じられるレイヤーがない
            if (layer == LayerType.HOME_SCREEN) {
                break;
            }

            return layer;
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