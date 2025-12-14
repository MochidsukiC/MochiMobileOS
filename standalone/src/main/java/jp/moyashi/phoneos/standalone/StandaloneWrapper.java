package jp.moyashi.phoneos.standalone;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumService;
import jp.moyashi.phoneos.core.service.chromium.DefaultChromiumService;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * StandaloneのPAppletイベントをcore独立APIに変換するラッパークラス。
 * PGraphics統一アーキテクチャにおいて、PAppletイベントシステムを
 * coreモジュールの独立API呼び出しに変換する責務を持つ。
 *
 * アーキテクチャ:
 * PApplet draw() → Kernel.update() + Kernel.render() → PGraphics描画 → PApplet画面出力
 * PApplet events → Kernel独立APIイベント
 *
 * @author jp.moyashi
 * @version 1.0 (PGraphics統一アーキテクチャ対応)
 */
public class StandaloneWrapper extends PApplet {

    /** MochiMobileOSのカーネルインスタンス */
    private Kernel kernel;

    /** IME入力レイヤー（P2DレンダラーでIME入力を可能にする） */
    private IMEInputLayer imeInputLayer;

    /** ハードウェアボタンウィンドウ（メインウィンドウの右側に表示、ホーム/音量ボタン） */
    private HardwareWindow hardwareWindow;

    /** 画面幅 */
    private static final int SCREEN_WIDTH = 400;

    /** 画面高さ */
    private static final int SCREEN_HEIGHT = 600;


    /**
     * Processing設定メソッド。
     * 画面サイズとその他の初期設定を行う。
     *
     * P2Dレンダラー使用理由:
     * - デフォルトのJAVA2D（CPU描画）では動画再生時に`image()`がボトルネックとなる
     * - P2D（JOGL/OpenGL GPU描画）により、Forgeと同様のGPU描画パスを実現
     * - 動画再生時のAWTEventQueue遅延を回避
     *
     * IME対応:
     * - P2DはNEWTウィンドウを使用し、標準ではIMEをサポートしない
     * - AWTの隠しウィンドウを作成してIME入力を受け取り、CEFに転送する方式で対応
     */
    @Override
    public void settings() {
        size(SCREEN_WIDTH, SCREEN_HEIGHT, P2D);
        System.out.println("StandaloneWrapper: Processing窓口設定完了 (" + SCREEN_WIDTH + "x" + SCREEN_HEIGHT + ", P2D renderer)");
    }

    /**
     * ProcessingのhandleKeyEvent()をオーバーライド。
     * ESCキーイベントを完全に無効化する。
     */
    @Override
    public void handleKeyEvent(processing.event.KeyEvent event) {
        // ESCキーの場合、Processingのデフォルト処理をスキップ
        if (event.getKeyCode() == 27) {
            // ESCキーイベントを消費（親クラスのhandleKeyEventを呼ばない）
            keyPressed = (event.getAction() == processing.event.KeyEvent.PRESS);
            key = event.getKey();
            keyCode = event.getKeyCode();

            // 自分のkeyPressed/keyReleased/keyTypedを直接呼ぶ
            if (event.getAction() == processing.event.KeyEvent.PRESS) {
                keyPressed();
            } else if (event.getAction() == processing.event.KeyEvent.RELEASE) {
                keyReleased();
            }
            return; // 親クラスのhandleKeyEventを呼ばない
        }
        // ESC以外は通常通り処理
        super.handleKeyEvent(event);
    }

    /**
     * Processing初期化メソッド。
     * Kernelの初期化を行う。
     */
    @Override
    public void setup() {
        System.out.println("StandaloneWrapper: Kernel初期化開始...");

        // ProcessingのESCキーによる終了を無効化
        // これによりESCキーを通常のキー入力として使用可能にする
        try {
            // PAppletの内部変数 exitCalled を無効化
            java.lang.reflect.Field exitCalledField = PApplet.class.getDeclaredField("exitCalled");
            exitCalledField.setAccessible(true);
            // ESCキー処理中は常にfalseになるように設定される
        } catch (Exception e) {
            System.out.println("StandaloneWrapper: Note - exitCalled field access: " + e);
        }

        // IMEを有効化（日本語入力のインライン編集対応）
        try {
            if (surface != null) {
                System.out.println("StandaloneWrapper: Setting up input methods for IME support");
                surface.setResizable(false); // ウィンドウサイズ固定

                Object nativeWindow = surface.getNative();
                System.out.println("StandaloneWrapper: Native window class: " + nativeWindow.getClass().getName());

                // P2Dレンダラーの場合、NEWTのGLWindowを使用
                // com.jogamp.newt.opengl.GLWindow はAWT Componentではないため、
                // リフレクションでマウスリスナーを登録
                if (nativeWindow.getClass().getName().equals("com.jogamp.newt.opengl.GLWindow")) {
                    System.out.println("StandaloneWrapper: Detected NEWT GLWindow (P2D renderer)");

                    // NEWTウィンドウでマウスホイールリスナーを登録
                    try {
                        Class<?> glWindowClass = nativeWindow.getClass();
                        Class<?> mouseListenerClass = Class.forName("com.jogamp.newt.event.MouseListener");

                        // MouseListenerインターフェースを実装する匿名クラスを作成
                        Object mouseListener = java.lang.reflect.Proxy.newProxyInstance(
                            glWindowClass.getClassLoader(),
                            new Class<?>[] { mouseListenerClass },
                            (proxy, method, args) -> {
                                // DISABLED: Duplicate mouseWheel event handling
                                // Processing's mouseWheel() method (line 343) already handles this
                                // Multiple listeners were causing duplicate/conflicting scroll events
                                /*
                                if (method.getName().equals("mouseWheelMoved") && args != null && args.length > 0) {
                                    // MouseEventからwheel rotationを取得
                                    Object mouseEvent = args[0];
                                    try {
                                        java.lang.reflect.Method getRotationMethod = mouseEvent.getClass().getMethod("getRotation");
                                        float[] rotation = (float[]) getRotationMethod.invoke(mouseEvent);
                                        if (rotation != null && rotation.length > 0 && kernel != null) {
                                            kernel.mouseWheel(mouseX, mouseY, (int) rotation[1]);
                                        }
                                    } catch (Exception ex) {
                                        // Silent failure
                                    }
                                }
                                */
                                return null;
                            }
                        );

                        // addMouseListener()を呼び出し
                        java.lang.reflect.Method addMouseListenerMethod = glWindowClass.getMethod("addMouseListener", mouseListenerClass);
                        addMouseListenerMethod.invoke(nativeWindow, mouseListener);
                        System.out.println("StandaloneWrapper: NEWT MouseListener registered");

                    } catch (Exception e) {
                        System.err.println("StandaloneWrapper: Failed to register NEWT mouse listener: " + e.getMessage());
                    }

                    // CRITICAL: NEWTウィンドウでIMEを有効化
                    // ProcessingのkeyTyped()はIME確定後の文字のみを受け取るため、
                    // NEWTの低レベルAPIを使用してIMEサポートを有効化する
                    try {
                        // WindowImplを取得（NEWTの内部実装）
                        Class<?> windowImplClass = Class.forName("jogamp.newt.WindowImpl");

                        if (windowImplClass.isInstance(nativeWindow)) {
                            // setInputMethodEnabled() メソッドを探す（存在する場合）
                            try {
                                java.lang.reflect.Method setInputMethodEnabledMethod =
                                    nativeWindow.getClass().getMethod("setInputMethodEnabled", boolean.class);
                                setInputMethodEnabledMethod.invoke(nativeWindow, true);
                                System.out.println("StandaloneWrapper: IME enabled on NEWT window via setInputMethodEnabled()");
                            } catch (NoSuchMethodException e) {
                                // メソッドが存在しない場合、WindowImplのフィールドを直接操作
                                System.out.println("StandaloneWrapper: setInputMethodEnabled() not found, trying field access...");

                                // inputMethodEnabledフィールドを探す
                                try {
                                    java.lang.reflect.Field inputMethodEnabledField =
                                        windowImplClass.getDeclaredField("inputMethodEnabled");
                                    inputMethodEnabledField.setAccessible(true);
                                    inputMethodEnabledField.set(nativeWindow, true);
                                    System.out.println("StandaloneWrapper: IME enabled on NEWT window via field access");
                                } catch (Exception fieldEx) {
                                    System.err.println("StandaloneWrapper: Failed to enable IME via field: " + fieldEx.getMessage());
                                }
                            }
                        }

                        System.out.println("StandaloneWrapper: IME configuration completed for NEWT window");
                        System.out.println("StandaloneWrapper: Note - IME input will work through Processing's keyTyped() events");

                    } catch (Exception e) {
                        System.err.println("StandaloneWrapper: Failed to enable IME on NEWT window: " + e.getMessage());
                        System.out.println("StandaloneWrapper: Falling back to Processing's default keyTyped() handling");
                    }

                } else if (nativeWindow instanceof java.awt.Component) {
                    // AWT Componentの場合（JAVA2Dレンダラー）
                    System.out.println("StandaloneWrapper: Detected AWT Component");
                    java.awt.Component component = (java.awt.Component) nativeWindow;

                    // IMEを明示的に有効化
                    component.enableInputMethods(true);

                    // InputContextを取得してIMEが有効か確認
                    java.awt.im.InputContext inputContext = component.getInputContext();
                    if (inputContext != null) {
                        System.out.println("StandaloneWrapper: InputContext available - IME should be working");
                        System.out.println("StandaloneWrapper: InputContext locale: " + inputContext.getLocale());
                    } else {
                        System.err.println("StandaloneWrapper: WARNING - InputContext is null");
                    }

                    // DISABLED: Duplicate mouseWheel event handling
                    // Processing's mouseWheel() method (line 343) already handles this
                    // Multiple listeners were causing duplicate/conflicting scroll events
                    /*
                    // AWTのMouseWheelListenerを登録
                    component.addMouseWheelListener(e -> {
                        if (kernel != null) {
                            kernel.mouseWheel(mouseX, mouseY, e.getWheelRotation());
                        }
                    });
                    System.out.println("StandaloneWrapper: AWT MouseWheelListener registered");
                    */
                } else {
                    System.err.println("StandaloneWrapper: Unknown native window type: " + nativeWindow.getClass().getName());
                }
            }
        } catch (Exception e) {
            System.err.println("StandaloneWrapper: Failed to setup input methods: " + e.getMessage());
            e.printStackTrace();
        }

        // Kernelを作成し、ChromiumServiceを注入してから初期化する
        kernel = new Kernel();
        ChromiumService chromiumService = new DefaultChromiumService(new StandaloneChromiumProvider());
        kernel.setChromiumService(chromiumService);
        kernel.initialize(this, SCREEN_WIDTH, SCREEN_HEIGHT);
        // AWT EventQueue を計測するラッパーを登録
        try {
            java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().push(new InstrumentedEventQueue(kernel));
            if (kernel.getLogger() != null) {
                kernel.getLogger().info("StandaloneWrapper", "Instrumented AWT EventQueue installed");
            }
        } catch (Exception e) {
            System.err.println("StandaloneWrapper: Failed to install InstrumentedEventQueue: " + e.getMessage());
        }

        // IME入力レイヤーを初期化（P2DレンダラーでIME入力を可能にする）
        java.awt.Frame processingFrame = null;
        try {
            // ProcessingウィンドウのAWTコンポーネントを取得
            java.awt.Frame[] frames = java.awt.Frame.getFrames();
            System.out.println("StandaloneWrapper: Searching for Processing Frame among " + frames.length + " frames");
            for (java.awt.Frame frame : frames) {
                System.out.println("StandaloneWrapper: Frame: " + frame.getTitle() +
                    ", visible=" + frame.isVisible() +
                    ", size=" + frame.getWidth() + "x" + frame.getHeight());
                // ウィンドウ装飾のためサイズが完全一致しない場合があるので、近似値で判定
                // または可視フレームで最初のものを使用
                if (frame.isVisible() && frame.getWidth() > 0 && frame.getHeight() > 0) {
                    // Processingフレームはクラス名にPSurfaceを含むことが多い
                    String className = frame.getClass().getName();
                    if (className.contains("processing") || className.contains("PSurface") ||
                        processingFrame == null) {
                        processingFrame = frame;
                        System.out.println("StandaloneWrapper: Selected frame: " + frame.getTitle());
                    }
                }
            }

            if (processingFrame != null) {
                imeInputLayer = new IMEInputLayer(kernel, processingFrame);
                System.out.println("StandaloneWrapper: IMEInputLayer initialized");
            } else {
                System.err.println("StandaloneWrapper: Could not find Processing Frame for IMEInputLayer");
            }
        } catch (Exception e) {
            System.err.println("StandaloneWrapper: Failed to initialize IMEInputLayer: " + e.getMessage());
            e.printStackTrace();
        }

        // ハードウェアボタンウィンドウを初期化（ホーム/音量ボタン）
        javax.swing.SwingUtilities.invokeLater(() -> {
            try {
                hardwareWindow = new HardwareWindow(kernel, SCREEN_WIDTH, SCREEN_HEIGHT);
                hardwareWindow.showWindow();
                System.out.println("StandaloneWrapper: HardwareWindow initialized and shown");
            } catch (Exception ex) {
                System.err.println("StandaloneWrapper: Failed to create HardwareWindow: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        System.out.println("StandaloneWrapper: Kernel初期化完了");
    }

    /**
     * Processing描画ループ。
     * Kernelの独立APIを呼び出してPGraphicsバッファを取得し、画面に描画する。
     */
    @Override
    public void draw() {
        // ProcessingのESCキーによる終了を防ぐ
        // 毎フレーム、exitCalledフラグをリセット
        try {
            java.lang.reflect.Field exitCalledField = PApplet.class.getDeclaredField("exitCalled");
            exitCalledField.setAccessible(true);
            if ((Boolean) exitCalledField.get(this)) {
                System.out.println("StandaloneWrapper: ESC exit detected and cancelled");
                exitCalledField.set(this, false);
            }
        } catch (Exception e) {
            // Silent failure - continue normal operation
        }

        if (kernel == null) {
            background(255, 0, 0);
            fill(255);
            textAlign(CENTER, CENTER);
            text("Kernel初期化中...", width/2, height/2);
            return;
        }

        try {
            // Kernelの更新処理を実行
            kernel.update();

            // Kernelの描画処理を実行（PGraphicsバッファに描画）
            kernel.render();

            // KernelのPGraphicsバッファを取得してPApplet画面に描画
            PGraphics kernelGraphics = kernel.getGraphics();
            if (kernelGraphics != null) {
                image(kernelGraphics, 0, 0);
            } else {
                // フォールバック: Kernelグラフィックスが利用できない場合
                background(50, 50, 50);
                fill(255);
                textAlign(CENTER, CENTER);
                text("Kernel Graphics Not Available", width/2, height/2);
            }

            // IME入力レイヤーの表示/非表示を制御
            if (imeInputLayer != null && kernel.getScreenManager() != null) {
                // 現在のScreenがテキスト入力フォーカスを持っている場合、IME入力レイヤーを表示
                jp.moyashi.phoneos.core.ui.Screen currentScreen = kernel.getScreenManager().getCurrentScreen();
                boolean needsIME = currentScreen != null && currentScreen.hasFocusedComponent();

                if (needsIME && !imeInputLayer.isVisible()) {
                    // IME入力レイヤーを画面下部に表示
                    imeInputLayer.show(10, SCREEN_HEIGHT - 50, SCREEN_WIDTH - 20, 30);
                } else if (!needsIME && imeInputLayer.isVisible()) {
                    // フォーカスがなくなったら非表示
                    imeInputLayer.hide();
                }
            }

        } catch (Exception e) {
            System.err.println("StandaloneWrapper: 描画エラー: " + e.getMessage());
            // エラー表示
            background(255, 0, 0);
            fill(255);
            textAlign(CENTER, CENTER);
            text("描画エラー!", width/2, height/2 - 20);
            text(e.getMessage(), width/2, height/2 + 20);
        }
    }

    /**
     * PAppletマウス押下イベント → Kernel独立API変換。
     */
    @Override
    public void mousePressed() {
        if (kernel != null) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().debug("StandaloneWrapper", "mousePressed event received at (" + mouseX + ", " + mouseY + ")");
            }
            kernel.mousePressed(mouseX, mouseY);
        }
    }

    /**
     * PAppletマウス離しイベント → Kernel独立API変換。
     */
    @Override
    public void mouseReleased() {
        if (kernel != null) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().debug("StandaloneWrapper", "mouseReleased event received at (" + mouseX + ", " + mouseY + ")");
            }
            kernel.mouseReleased(mouseX, mouseY);
        }
    }

    /**
     * PAppletマウスドラッグイベント → Kernel独立API変換。
     * ジェスチャー認識に重要な機能。
     *
     * P2D GPU描画により、すべてのマウスドラッグイベントを即座にKernelに転送。
     */
    @Override
    public void mouseDragged() {
        if (kernel != null) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().debug("StandaloneWrapper", "mouseDragged event received at (" + mouseX + ", " + mouseY + ")");
            }
            kernel.mouseDragged(mouseX, mouseY);
        }
    }

    /**
     * PAppletマウス移動イベント → Kernel独立API変換。
     */
    @Override
    public void mouseMoved() {
        if (kernel != null) {
            kernel.mouseMoved(mouseX, mouseY);
        }
    }

    /**
     * PAppletマウスホイールイベント → Kernel独立API変換。
     * スクロール操作の処理に使用。
     */
    @Override
    public void mouseWheel(processing.event.MouseEvent event) {
        // Only Processing's mouseWheel event is used to avoid duplicate event handling
        // NEWT and AWT listeners have been disabled (lines 88-105, 175-186)
        if (kernel != null) {
            // Kernelの独立mouseWheel APIを呼び出し
            // getCount()はスクロール量を返す（正の値：下スクロール、負の値：上スクロール）
            kernel.mouseWheel(mouseX, mouseY, event.getCount());
        }
    }

    /**
     * PAppletキー押下イベント → Kernel独立API変換。
     * 特殊キー（矢印、Backspace等）の処理に使用。
     */
    @Override
    public void keyPressed() {
        System.out.println("StandaloneWrapper: keyPressed - key: '" + key + "', keyCode: " + keyCode);

        // Ctrl/Alt/Metaの状態を取得（複数箇所で使用）
        boolean isCtrlPressed = (kernel != null && kernel.isCtrlPressed());
        boolean isAltPressed = (kernel != null && kernel.isAltPressed());
        boolean isMetaPressed = (kernel != null && kernel.isMetaPressed());

        // Ctrl+Space でホームに戻る（プラットフォーム固有のホームボタンショートカット）
        if (isCtrlPressed && !isAltPressed && !isMetaPressed && (key == ' ' || keyCode == 32)) {
            System.out.println("StandaloneWrapper: Ctrl+Space detected - requesting go home");
            if (kernel != null) {
                kernel.requestGoHome();
            }
            return; // イベント消費
        }

        // ESCキー（keyCode == 27）の場合、Processingのデフォルト動作（アプリケーション終了）を無効化
        if (keyCode == 27) {
            System.out.println("StandaloneWrapper: ESC key detected - disabling default exit behavior");
            if (kernel != null) {
                kernel.keyPressed(key, keyCode);
            }
            // key = 0を削除 - keyReleased()に影響を与えないようにする
            // Processingのデフォルト処理を無効化する別の方法を使用
            return;
        }

        // 特殊キー（矢印、Backspace、Delete、Shift、Ctrl、Alt、Meta等）のみここで処理
        // 通常の文字入力はkeyTyped()で処理される
        // 制御文字（< 32）、CODEDキー、特殊キーコード（35-40: Home, End, 矢印）、修飾キー（16: Shift, 17: Ctrl, 18: Alt, 91/157: Meta）を処理
        // 修飾キーはkeyCodeで判定（keyの値が不定のため）
        // スペースキーは通常のキーとして扱う（keyTyped()で処理）
        boolean isSpecialKey = (key == CODED || key < 32 || (keyCode >= 35 && keyCode <= 40) ||
                                keyCode == 8 || keyCode == 127 || keyCode == 16 || keyCode == 17 || keyCode == 18 || keyCode == 91 || keyCode == 157);

        // Ctrl/Alt/Metaが押されている場合、通常文字キーもkeyPressed()に転送（Ctrl+C/V/A、Alt+F4、Cmd+C等のショートカット用）
        boolean shouldForwardKey = isSpecialKey || isCtrlPressed || isAltPressed || isMetaPressed;

        if (shouldForwardKey) {
            if (kernel != null) {
                System.out.println("StandaloneWrapper: Forwarding key to Kernel (key: '" + key + "', keyCode: " + keyCode + ", Ctrl: " + isCtrlPressed + ", Alt: " + isAltPressed + ", Meta: " + isMetaPressed + ")");
                kernel.keyPressed(key, keyCode);
            } else {
                System.out.println("StandaloneWrapper: kernel is null, cannot forward key event");
            }
        } else {
            System.out.println("StandaloneWrapper: Skipping normal character in keyPressed() - will be handled by keyTyped()");
        }
    }

    /**
     * PAppletキータイプイベント → Kernel独立API変換。
     * Unicode文字（日本語等）の入力処理に使用。
     * keyPressed()と異なり、IMEを通じて確定した文字がここに渡される。
     */
    @Override
    public void keyTyped() {
        System.out.println("StandaloneWrapper: keyTyped - key: '" + key + "' (Unicode: " + (int)key + ")");

        // 制御文字、CODEDキー、特殊キーコード（35-40: Home, End, 矢印）を除外
        // これらはkeyPressed()で既に処理されている
        // スペースキー（32）は通常の文字として扱う
        if (key == CODED || key < 32 || (key >= 35 && key <= 40) || key == 127) {
            System.out.println("StandaloneWrapper: Skipping control character or special key in keyTyped()");
            return;
        }

        if (kernel != null) {
            // keyTypedでは通常のUnicode文字が渡される
            // keyCodeは常に0なので、keyのみを使用
            kernel.keyPressed(key, 0);
        } else {
            System.out.println("StandaloneWrapper: kernel is null, cannot forward key event");
        }
    }

    /**
     * PAppletキー離しイベント → Kernel独立API変換。
     */
    @Override
    public void keyReleased() {
        if (kernel != null) {
            kernel.keyReleased(key, keyCode);
        }
    }

    /**
     * ProcessingのexitActual()をオーバーライド。
     * ESCキーで誤って終了しないようにする。
     */
    @Override
    public void exitActual() {
        // ESCキーが押された場合は終了をキャンセル
        if (keyCode == 27) {
            System.out.println("StandaloneWrapper: ESC key exit cancelled");
            return; // 終了をキャンセル
        }
        // 通常の終了処理
        super.exitActual();
    }

    /**
     * アプリ終了時の清片処理。
     */
    @Override
    public void exit() {
        System.out.println("StandaloneWrapper: 終了処理開始...");

        if (kernel != null) {
            try {
                kernel.shutdown();
            } catch (Exception e) {
                System.err.println("StandaloneWrapper: Kernel終了処理エラー: " + e.getMessage());
            }
        }

        System.out.println("StandaloneWrapper: 終了処理完了");
        super.exit();
    }

    /**
     * Kernelインスタンスを取得する。
     *
     * @return Kernelインスタンス
     */
    public Kernel getKernel() {
        return kernel;
    }
}
