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

    /** 画面幅 */
    private static final int SCREEN_WIDTH = 400;

    /** 画面高さ */
    private static final int SCREEN_HEIGHT = 600;

    /** マウス移動スロットリング用 */
    private long lastMouseMoveTimeMs = 0L;
    private long lastMouseDragTimeMs = 0L;
    private static final long MOUSE_MOVE_THROTTLE_MS = 200; // 5回/秒（大幅削減）
    private static final long MOUSE_DRAG_THROTTLE_MS = 100; // 10回/秒

    /**
     * Processing設定メソッド。
     * 画面サイズとその他の初期設定を行う。
     */
    @Override
    public void settings() {
        size(SCREEN_WIDTH, SCREEN_HEIGHT);
        System.out.println("StandaloneWrapper: Processing窓口設定完了 (" + SCREEN_WIDTH + "x" + SCREEN_HEIGHT + ")");
    }

    /**
     * Processing初期化メソッド。
     * Kernelの初期化を行う。
     */
    @Override
    public void setup() {
        System.out.println("StandaloneWrapper: Kernel初期化開始...");

        // IMEを有効化（日本語入力のインライン編集対応）
        try {
            if (surface != null) {
                System.out.println("StandaloneWrapper: Enabling input methods for IME support");
                surface.setResizable(false); // ウィンドウサイズ固定

                // Java AWTコンポーネントでIMEを有効化し、MouseWheelListenerを登録
                java.awt.Component component = (java.awt.Component) surface.getNative();
                if (component != null) {
                    component.enableInputMethods(true);
                    System.out.println("StandaloneWrapper: Input methods enabled on AWT component");

                    // AWTのMouseWheelListenerを直接登録（Processingのイベントが機能しないため）
                    component.addMouseWheelListener(e -> {
                        if (kernel != null) {
                            long captureNs = System.nanoTime();
                            long captureMs = System.currentTimeMillis();
                            kernel.mouseWheel(mouseX, mouseY, e.getWheelRotation());
                            long completeNs = System.nanoTime();
                            logInputBridge("mouseWheel", captureNs, completeNs, captureMs, mouseX, mouseY);
                        }
                    });
                    System.out.println("StandaloneWrapper: MouseWheelListener registered on AWT component");
                }
            }
        } catch (Exception e) {
            System.err.println("StandaloneWrapper: Failed to enable input methods: " + e.getMessage());
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

        System.out.println("StandaloneWrapper: Kernel初期化完了");
    }

    /**
     * Processing描画ループ。
     * Kernelの独立APIを呼び出してPGraphicsバッファを取得し、画面に描画する。
     */
    @Override
    public void draw() {
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
            long captureNs = System.nanoTime();
            long captureMs = System.currentTimeMillis();
            kernel.mousePressed(mouseX, mouseY);
            long completeNs = System.nanoTime();
            logInputBridge("mousePressed", captureNs, completeNs, captureMs, mouseX, mouseY);
        }
    }

    /**
     * PAppletマウス離しイベント → Kernel独立API変換。
     */
    @Override
    public void mouseReleased() {
        if (kernel != null) {
            long captureNs = System.nanoTime();
            long captureMs = System.currentTimeMillis();
            kernel.mouseReleased(mouseX, mouseY);
            long completeNs = System.nanoTime();
            logInputBridge("mouseReleased", captureNs, completeNs, captureMs, mouseX, mouseY);
        }
    }

    /**
     * PAppletマウスドラッグイベント → Kernel独立API変換。
     * ジェスチャー認識に重要な機能。
     * スロットリング: 50ms間隔（20回/秒）でのみ送信し、
     * AWTイベントキューの蓄積を防ぐ。
     */
    @Override
    public void mouseDragged() {
        if (kernel != null) {
            long now = System.currentTimeMillis();
            if (now - lastMouseDragTimeMs >= MOUSE_DRAG_THROTTLE_MS) {
                kernel.mouseDragged(mouseX, mouseY);
                lastMouseDragTimeMs = now;
            }
        }
    }

    /**
     * PAppletマウス移動イベント → Kernel独立API変換。
     * スロットリング: 50ms間隔（20回/秒）でのみ送信し、
     * AWTイベントキューの蓄積を防ぐ。
     */
    @Override
    public void mouseMoved() {
        if (kernel != null) {
            long now = System.currentTimeMillis();
            if (now - lastMouseMoveTimeMs >= MOUSE_MOVE_THROTTLE_MS) {
                kernel.mouseMoved(mouseX, mouseY);
                lastMouseMoveTimeMs = now;
            }
        }
    }

    /**
     * PAppletマウスホイールイベント → Kernel独立API変換。
     * スクロール操作の処理に使用。
     */
    @Override
    public void mouseWheel(processing.event.MouseEvent event) {
        System.out.println("StandaloneWrapper: mouseWheel event received - count: " + event.getCount() + ", mouseX: " + mouseX + ", mouseY: " + mouseY);
        if (kernel != null) {
            // Kernelの独立mouseWheel APIを呼び出し
            // getCount()はスクロール量を返す（正の値：下スクロール、負の値：上スクロール）
            kernel.mouseWheel(mouseX, mouseY, event.getCount());
            System.out.println("StandaloneWrapper: mouseWheel forwarded to Kernel");
        } else {
            System.out.println("StandaloneWrapper: kernel is null, cannot forward mouseWheel event");
        }
    }

    /**
     * PAppletキー押下イベント → Kernel独立API変換。
     * 特殊キー（矢印、Backspace等）の処理に使用。
     */
    @Override
    public void keyPressed() {
        System.out.println("StandaloneWrapper: keyPressed - key: '" + key + "', keyCode: " + keyCode);

        // ESCキー（keyCode == 27）の場合、Processingのデフォルト動作（アプリケーション終了）を無効化
        if (keyCode == 27) {
            System.out.println("StandaloneWrapper: ESC key detected - disabling default exit behavior");
            if (kernel != null) {
                kernel.keyPressed(key, keyCode);
            }
            key = 0; // Processingのデフォルト処理を無効化
            return;
        }

        // 特殊キー（矢印、Backspace、Delete、Shift、Ctrl等）のみここで処理
        // 通常の文字入力はkeyTyped()で処理される
        // 制御文字（< 32）、CODEDキー、特殊キーコード（35-40: Home, End, 矢印）、修飾キー（16: Shift, 17: Ctrl）を処理
        // 修飾キーはkeyCodeで判定（keyの値が不定のため）
        boolean isSpecialKey = (key == CODED || key < 32 || (keyCode >= 35 && keyCode <= 40) ||
                                keyCode == 8 || keyCode == 127 || keyCode == 16 || keyCode == 17);

        // Ctrlが押されている場合、通常文字キーもkeyPressed()に転送（Ctrl+C/V/A等のショートカット用）
        boolean isCtrlPressed = (kernel != null && kernel.isCtrlPressed());
        boolean shouldForwardKey = isSpecialKey || isCtrlPressed;

        if (shouldForwardKey) {
            if (kernel != null) {
                System.out.println("StandaloneWrapper: Forwarding key to Kernel (key: '" + key + "', keyCode: " + keyCode + ", Ctrl: " + isCtrlPressed + ")");
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

    private void logInputBridge(String eventName, long captureNs, long completeNs, long captureMs, int x, int y) {
        if (kernel == null || kernel.getLogger() == null) {
            return;
        }
        double latencyMs = (completeNs - captureNs) / 1_000_000.0;
        String message = String.format(
                "%s latency=%.3fms captured=%tT.%03d coord=(%d,%d)",
                eventName,
                latencyMs,
                captureMs, (int) (captureMs % 1000),
                x, y);
        kernel.getLogger().debug("StandaloneInputBridge", message);
    }
}
