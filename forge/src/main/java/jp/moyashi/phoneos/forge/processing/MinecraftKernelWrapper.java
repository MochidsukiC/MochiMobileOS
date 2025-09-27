package jp.moyashi.phoneos.forge.processing;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.forge.gui.ProcessingScreen;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.core.PApplet;

/**
 * Minecraft環境でMochiMobileOSのKernelを動作させるためのラッパークラス。
 * KernelがPAppletを継承しているため、Minecraft環境での動作をサポートする。
 *
 * @author jp.moyashi
 * @version 1.0
 * @since 1.0
 */
public class MinecraftKernelWrapper {

    private static final Logger LOGGER = LogManager.getLogger();

    /** MochiMobileOSのカーネル */
    private Kernel kernel;

    /** Minecraft画面参照 */
    private ProcessingScreen parentScreen;

    /** 初期化フラグ */
    private boolean initialized = false;

    /** カーネルのサイズ */
    private final int width;
    private final int height;

    /**
     * MinecraftKernelWrapperのコンストラクタ。
     *
     * @param width  画面幅
     * @param height 画面高さ
     */
    public MinecraftKernelWrapper(int width, int height) {
        this.width = width;
        this.height = height;
        System.out.println("[MinecraftKernelWrapper] Creating kernel wrapper: " + width + "x" + height);
    }

    /**
     * カーネルを初期化する。
     */
    public void initialize() {
        try {
            System.out.println("[MinecraftKernelWrapper] Initializing MochiMobileOS kernel...");

            // PAppletインスタンスを作成してKernelを初期化
            PApplet parentApplet = new PApplet();
            kernel = new Kernel();
            kernel.initialize(parentApplet, width, height);
            System.out.println("[MinecraftKernelWrapper] Kernel instance created");

            // Kernelのサイズ設定は初期化時に完了しているためスキップ
            System.out.println("[MinecraftKernelWrapper] Kernel size set: " + width + "x" + height);

            // Kernelの初期化は既にinitializeメソッドで完了
            // setup()は内部で呼び出されるため不要
            System.out.println("[MinecraftKernelWrapper] Kernel setup completed");

            initialized = true;
            System.out.println("[MinecraftKernelWrapper] Kernel initialized successfully");

        } catch (Exception e) {
            System.err.println("[MinecraftKernelWrapper] Failed to initialize kernel: " + e.getMessage());
            e.printStackTrace();
            // フォールバック: 初期化を失敗としてマークするが、エラーでクラッシュはしない
            initialized = false;
        }
    }

    /**
     * カーネルの描画処理を実行。
     */
    public void draw() {
        if (!initialized || kernel == null) {
            return;
        }

        try {
            // Kernelの描画ループを実行
            kernel.draw();

        } catch (Exception e) {
            System.err.println("[MinecraftKernelWrapper] Draw error: " + e.getMessage());
        }
    }

    /**
     * マウスプレスイベントを処理。
     *
     * @param x X座標
     * @param y Y座標
     */
    public void mousePressed(int x, int y) {
        if (!initialized || kernel == null) {
            return;
        }

        try {
            // マウスプレスイベントを実行（新しいAPI）
            kernel.mousePressed(x, y);

        } catch (Exception e) {
            System.err.println("[MinecraftKernelWrapper] Mouse press error: " + e.getMessage());
        }
    }

    /**
     * マウスリリースイベントを処理。
     *
     * @param x X座標
     * @param y Y座標
     */
    public void mouseReleased(int x, int y) {
        if (!initialized || kernel == null) {
            return;
        }

        try {
            // マウスリリースイベントを実行（新しいAPI）
            kernel.mouseReleased(x, y);

        } catch (Exception e) {
            System.err.println("[MinecraftKernelWrapper] Mouse release error: " + e.getMessage());
        }
    }

    /**
     * キーボードイベントを処理。
     *
     * @param key     押されたキー
     * @param keyCode キーコード
     */
    public void keyPressed(char key, int keyCode) {
        if (!initialized || kernel == null) {
            return;
        }

        try {
            // キープレスイベントを実行（新しいAPI）
            kernel.keyPressed(key, keyCode, 0, 0);

        } catch (Exception e) {
            System.err.println("[MinecraftKernelWrapper] Key press error: " + e.getMessage());
        }
    }

    /**
     * 親画面を設定。
     *
     * @param screen ProcessingScreen参照
     */
    public void setParentScreen(ProcessingScreen screen) {
        this.parentScreen = screen;
    }

    /**
     * カーネルインスタンスを取得。
     *
     * @return Kernelインスタンス
     */
    public Kernel getKernel() {
        return kernel;
    }

    /**
     * 初期化状態を取得。
     *
     * @return 初期化済みの場合true
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * カーネルのピクセル配列を取得。
     * MinecraftのGUIに描画するために使用する。
     *
     * @return ピクセル配列
     */
    public int[] getPixels() {
        if (!initialized || kernel == null) {
            return new int[width * height]; // 空の配列を返す
        }

        try {
            // Kernelのピクセル配列を取得（新しいAPI）
            return kernel.getPixels();

        } catch (Exception e) {
            System.err.println("[MinecraftKernelWrapper] Failed to get pixels: " + e.getMessage());
            return new int[width * height];
        }
    }

    /**
     * クリーンアップ処理。
     */
    public void cleanup() {
        try {
            System.out.println("[MinecraftKernelWrapper] Cleaning up kernel wrapper...");

            if (kernel != null) {
                // Kernelのクリーンアップ処理
                kernel.cleanup();
                kernel = null;
            }

            initialized = false;
            System.out.println("[MinecraftKernelWrapper] Kernel wrapper cleanup completed");

        } catch (Exception e) {
            System.err.println("[MinecraftKernelWrapper] Cleanup error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}