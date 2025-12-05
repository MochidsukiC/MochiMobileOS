package jp.moyashi.phoneos.core.input;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.popup.PopupManager;
import jp.moyashi.phoneos.core.ui.ScreenManager;
import jp.moyashi.phoneos.core.service.LoggerService;

/**
 * 入力イベント処理を一元管理するクラス。
 * マウスイベント、キーボードイベント、修飾キーの状態管理を担当。
 * Kernelクラスから入力処理責任を分離し、単一責任原則に準拠。
 *
 * @since 2025-12-02
 * @version 1.0
 */
public class InputManager {

    /** 処理を委譲するKernelインスタンス */
    private final Kernel kernel;

    /** 修飾キーの状態管理 */
    private ModifierKeyState modifierState;

    /** ESCキー長押し検出用 */
    private boolean escKeyPressed = false;
    private long escKeyPressTime = 0;
    private static final long ESC_HOLD_DURATION = 2000; // 2秒

    /** ショートカットキー処理 */
    private ShortcutKeyProcessor shortcutProcessor;

    /** ロガーサービス */
    private LoggerService logger;

    /**
     * InputManagerを初期化する。
     *
     * @param kernel Kernelインスタンス
     */
    public InputManager(Kernel kernel) {
        this.kernel = kernel;
        this.modifierState = new ModifierKeyState();
        this.shortcutProcessor = new ShortcutKeyProcessor(kernel);
        this.logger = kernel.getLogger();
    }

    /**
     * マウス押下イベントを処理する。
     * PopupManager → GestureManager → ScreenManagerの順に委譲。
     *
     * @param x X座標
     * @param y Y座標
     * @param button マウスボタン（1=左, 2=中, 3=右）
     */
    public void handleMousePressed(int x, int y, int button) {
        // スリープ中は処理をスキップ
        if (kernel.isSleeping()) {
            return;
        }

        PopupManager popupManager = kernel.getPopupManager();
        ScreenManager screenManager = kernel.getScreenManager();
        GestureManager gestureManager = kernel.getGestureManager();

        // ポップアップが処理した場合は終了
        if (popupManager != null && popupManager.handleMouseClick(x, y)) {
            return;
        }

        // ジェスチャーマネージャーに通知
        if (gestureManager != null) {
            gestureManager.handleMousePressed(x, y);
        }

        // スクリーンマネージャーに委譲
        if (screenManager != null) {
            screenManager.mousePressed(x, y);
        }
    }

    /**
     * マウスリリースイベントを処理する。
     *
     * @param x X座標
     * @param y Y座標
     * @param button マウスボタン
     */
    public void handleMouseReleased(int x, int y, int button) {
        // スリープ中は処理をスキップ
        if (kernel.isSleeping()) {
            return;
        }

        PopupManager popupManager = kernel.getPopupManager();
        ScreenManager screenManager = kernel.getScreenManager();
        GestureManager gestureManager = kernel.getGestureManager();

        // ポップアップが処理した場合は終了
        // TODO: PopupManagerにはhandleMouseReleaseメソッドがない
        // if (popupManager != null && popupManager.handleMouseRelease(x, y)) {
        //     return;
        // }

        // ジェスチャーマネージャーに通知
        if (gestureManager != null) {
            gestureManager.handleMouseReleased(x, y);
        }

        // スクリーンマネージャーに委譲
        if (screenManager != null) {
            screenManager.mouseReleased(x, y);
        }
    }

    /**
     * マウスドラッグイベントを処理する。
     *
     * @param x X座標
     * @param y Y座標
     * @param button マウスボタン
     */
    public void handleMouseDragged(int x, int y, int button) {
        // スリープ中は処理をスキップ
        if (kernel.isSleeping()) {
            return;
        }

        PopupManager popupManager = kernel.getPopupManager();
        ScreenManager screenManager = kernel.getScreenManager();
        GestureManager gestureManager = kernel.getGestureManager();

        // ポップアップが処理した場合は終了
        // TODO: PopupManagerにはhandleMouseDragメソッドがない
        // if (popupManager != null && popupManager.handleMouseDrag(x, y)) {
        //     return;
        // }

        // ジェスチャーマネージャーに通知
        if (gestureManager != null) {
            gestureManager.handleMouseDragged(x, y);
        }

        // スクリーンマネージャーに委譲
        if (screenManager != null) {
            screenManager.mouseDragged(x, y);
        }
    }

    /**
     * マウス移動イベントを処理する。
     *
     * @param x X座標
     * @param y Y座標
     */
    public void handleMouseMoved(int x, int y) {
        // スリープ中は処理をスキップ
        if (kernel.isSleeping()) {
            return;
        }

        ScreenManager screenManager = kernel.getScreenManager();
        GestureManager gestureManager = kernel.getGestureManager();

        // ジェスチャーマネージャーに通知
        // TODO: GestureManagerにhandleMouseMovedメソッドが存在しない可能性
        // if (gestureManager != null) {
        //     gestureManager.handleMouseMoved(x, y);
        // }

        // スクリーンマネージャーに委譲
        if (screenManager != null) {
            screenManager.mouseMoved(x, y);
        }
    }

    /**
     * マウスホイールイベントを処理する。
     * 現在のマウス座標はKernelが管理すると仮定。
     *
     * @param delta ホイール回転量（正=上、負=下）
     */
    public void handleMouseWheel(float delta) {
        // スリープ中は処理をスキップ
        if (kernel.isSleeping()) {
            return;
        }

        ScreenManager screenManager = kernel.getScreenManager();

        // スクリーンマネージャーに委譲
        // TODO: 現在のマウス座標を取得する必要がある。現在は0, 0で仮実装
        if (screenManager != null) {
            screenManager.mouseWheel(0, 0, delta);
        }
    }

    /**
     * キー押下イベントを処理する。
     * 修飾キー検出、ショートカット処理、画面への委譲を行う。
     *
     * @param key 押されたキー文字
     * @param keyCode キーコード
     */
    public void handleKeyPressed(char key, int keyCode) {
        if (logger != null) {
            logger.debug("InputManager", "handleKeyPressed - key='" + key + "', keyCode=" + keyCode);
        }

        // 修飾キーの状態を更新
        modifierState.updateOnKeyPressed(keyCode);

        // ESCキー処理はKernelで直接処理（元の動作を復元）
        // ESCキー単押し：スリープトグル、長押し：シャットダウン
        // if (keyCode == 27) { // ESC
        //     return; // Kernelで処理
        // }

        // スリープ中の場合、任意のキーで復帰
        if (kernel.isSleeping()) {
            kernel.wake();
            return;
        }

        // スペースキーのホームボタン処理はKernelで直接処理（元の動作を復元）
        // Phase 1リファクタリングで移行したが、二重処理になるため無効化
        // if (key == ' ' || keyCode == 32) {
        //     handleHomeButton();
        //     return;
        // }

        // ショートカットキー処理（Ctrl+C/V/X/A、バックスペース等）
        if (shortcutProcessor.processKeyPressed(key, keyCode, modifierState)) {
            return; // ショートカットが処理された
        }

        ScreenManager screenManager = kernel.getScreenManager();

        // スクリーンマネージャーに委譲
        if (screenManager != null) {
            // 修飾キーの状態を通知（ShiftとCtrlのみサポート）
            screenManager.setModifierKeys(
                modifierState.isShiftPressed(),
                modifierState.isCtrlPressed()
            );
            screenManager.keyPressed(key, keyCode);
        }
    }

    /**
     * キーリリースイベントを処理する。
     *
     * @param key リリースされたキー文字
     * @param keyCode キーコード
     */
    public void handleKeyReleased(char key, int keyCode) {
        // 修飾キーの状態を更新
        modifierState.updateOnKeyReleased(keyCode);

        // ESCキー処理はKernelで直接処理（元の動作を復元）
        // if (keyCode == 27) { // ESC
        //     return; // Kernelで処理
        // }

        // スリープ中は処理をスキップ
        if (kernel.isSleeping()) {
            return;
        }

        ScreenManager screenManager = kernel.getScreenManager();

        // スクリーンマネージャーに委譲
        if (screenManager != null) {
            screenManager.keyReleased(key, keyCode);
        }
    }

    /**
     * 定期的な更新処理。
     * ESCキー長押し検出などを行う。
     */
    public void update() {
        // ESCキー長押し検出は元のKernelの処理を使用
        // Phase 1リファクタリングで移行したが、動作しないため元に戻す
    }

    /**
     * ホームボタン処理を実行する。
     * LayerControllerに移行予定だが、現在はKernelのメソッドを呼び出す。
     */
    private void handleHomeButton() {
        // TODO: Phase 3でLayerControllerに移行
        if (logger != null) {
            logger.info("InputManager", "Calling kernel.handleHomeButton()");
        }
        kernel.handleHomeButton();
    }

    /**
     * 修飾キーの状態を取得する。
     *
     * @return 修飾キーの状態
     */
    public ModifierKeyState getModifierState() {
        return modifierState;
    }

    /**
     * Shiftキーが押されているかを取得する。
     *
     * @return Shiftキーが押されている場合true
     */
    public boolean isShiftPressed() {
        return modifierState.isShiftPressed();
    }

    /**
     * Ctrlキーが押されているかを取得する。
     *
     * @return Ctrlキーが押されている場合true
     */
    public boolean isCtrlPressed() {
        return modifierState.isCtrlPressed();
    }

    /**
     * Altキーが押されているかを取得する。
     *
     * @return Altキーが押されている場合true
     */
    public boolean isAltPressed() {
        return modifierState.isAltPressed();
    }

    /**
     * Metaキー（Cmd/Windows）が押されているかを取得する。
     *
     * @return Metaキーが押されている場合true
     */
    public boolean isMetaPressed() {
        return modifierState.isMetaPressed();
    }

    /**
     * 修飾キーの状態を管理する内部クラス。
     */
    public static class ModifierKeyState {
        private boolean shiftPressed = false;
        private boolean ctrlPressed = false;
        private boolean altPressed = false;
        private boolean metaPressed = false;

        /**
         * キー押下時に修飾キーの状態を更新する。
         *
         * @param keyCode キーコード
         */
        public void updateOnKeyPressed(int keyCode) {
            switch (keyCode) {
                case 16: // Shift
                    shiftPressed = true;
                    break;
                case 17: // Ctrl
                    ctrlPressed = true;
                    break;
                case 18: // Alt
                    altPressed = true;
                    break;
                case 91: // Meta (Left)
                case 157: // Meta (Right/Windows)
                    metaPressed = true;
                    break;
            }
        }

        /**
         * キーリリース時に修飾キーの状態を更新する。
         *
         * @param keyCode キーコード
         */
        public void updateOnKeyReleased(int keyCode) {
            switch (keyCode) {
                case 16: // Shift
                    shiftPressed = false;
                    break;
                case 17: // Ctrl
                    ctrlPressed = false;
                    break;
                case 18: // Alt
                    altPressed = false;
                    break;
                case 91: // Meta (Left)
                case 157: // Meta (Right/Windows)
                    metaPressed = false;
                    break;
            }
        }

        /**
         * いずれかの修飾キーが押されているかを判定する。
         *
         * @return いずれかの修飾キーが押されている場合true
         */
        public boolean isAnyModifierPressed() {
            return shiftPressed || ctrlPressed || altPressed || metaPressed;
        }

        // Getter methods
        public boolean isShiftPressed() { return shiftPressed; }
        public boolean isCtrlPressed() { return ctrlPressed; }
        public boolean isAltPressed() { return altPressed; }
        public boolean isMetaPressed() { return metaPressed; }
    }
}