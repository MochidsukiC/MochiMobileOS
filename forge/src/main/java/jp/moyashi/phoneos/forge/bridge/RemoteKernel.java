package jp.moyashi.phoneos.forge.bridge;

import jp.moyashi.phoneos.core.Kernel.LayerType;
import jp.moyashi.phoneos.core.ipc.IPCConstants;
import jp.moyashi.phoneos.core.ipc.InputEvent;
import jp.moyashi.phoneos.core.ipc.KernelProxy;
import jp.moyashi.phoneos.core.ipc.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import processing.core.PFont;
import processing.core.PGraphics;

/**
 * リモートカーネルプロキシ。
 * ProcessingScreenがKernelの代わりに使用するプロキシクラス。
 * 共有メモリを通じてMMOSサーバーと通信する。
 * KernelProxyインターフェースを実装し、ProcessingScreenから透過的に使用可能。
 *
 * @author jp.moyashi
 * @version 1.0
 */
public class RemoteKernel implements KernelProxy {

    private static final Logger LOGGER = LogManager.getLogger(RemoteKernel.class);

    /** 共有メモリクライアント */
    private final SharedMemoryClient shm;

    /** ローカルロガー（Log4jベース） */
    private static final Logger LOCAL_LOGGER = LogManager.getLogger("RemoteKernel");

    // === ローカル状態（修飾キー） ===
    private boolean localShiftState = false;
    private boolean localCtrlState = false;
    private boolean localAltState = false;
    private boolean localMetaState = false;

    // === publicフィールド（ProcessingScreenが直接アクセス） ===
    /** フレームカウンター（サーバーから同期） */
    public int frameCount = 0;

    /** 画面幅（固定値） */
    public int width = IPCConstants.SCREEN_WIDTH;

    /** 画面高さ（固定値） */
    public int height = IPCConstants.SCREEN_HEIGHT;

    /** サーバー状態キャッシュ */
    private ServerState cachedState;

    // === KernelProxyインターフェース実装（フィールドGetter） ===

    @Override
    public int getFrameCount() {
        return frameCount;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    /**
     * RemoteKernelを作成する。
     *
     * @param worldId ワールドID
     */
    public RemoteKernel(String worldId) {
        this.shm = new SharedMemoryClient(worldId);
        LOGGER.info("[RemoteKernel] Created for world: {}", worldId);
    }

    /**
     * サーバーに接続する。
     *
     * @return 接続成功した場合true
     */
    public boolean connect() {
        boolean result = shm.connect();
        if (result) {
            LOGGER.info("[RemoteKernel] Connected to MMOS server");
        }
        return result;
    }

    /**
     * 接続済みかどうかを返す。
     */
    public boolean isConnected() {
        return shm.isConnected();
    }

    /**
     * サーバーが準備完了しているかを確認する。
     */
    public boolean isServerReady() {
        return shm.isServerReady();
    }

    /**
     * サーバー状態を更新する。
     * 毎フレーム呼び出して状態を同期する。
     */
    private void refreshState() {
        if (!shm.isConnected()) return;

        cachedState = shm.readState();
        if (cachedState != null) {
            frameCount = cachedState.frameCount;
        }
    }

    // === グループA: 毎フレーム（IPC経由 - 画像転送） ===

    /**
     * 更新処理（サーバー側で自動実行、クライアントは状態更新のみ）。
     */
    @Override
    public void update() {
        refreshState();
    }

    /**
     * 描画処理（サーバー側で自動実行、クライアントはno-op）。
     */
    @Override
    public void render() {
        // サーバー側で自動実行されるため、何もしない
    }

    /**
     * ピクセル配列を取得する。
     *
     * @return ピクセル配列（ARGB形式）
     */
    @Override
    public int[] getPixels() {
        return shm.readPixels();
    }

    /**
     * グラフィックスバッファを取得する（サーバー側で管理）。
     *
     * @return null（サーバー側で管理）
     */
    @Override
    public PGraphics getGraphics() {
        return null;
    }

    // === グループB: ライフサイクル（IPC経由 - コマンド） ===

    /**
     * Minecraft用に初期化する。
     */
    public void initializeForMinecraft(int w, int h) {
        initializeForMinecraft(w, h, "default");
    }

    /**
     * Minecraft用に初期化する（ワールドID指定）。
     */
    public void initializeForMinecraft(int w, int h, String worldId) {
        shm.sendCommand(IPCConstants.CMD_INIT, w, h, worldId);
    }

    /**
     * シャットダウンする。
     */
    public void shutdown() {
        shm.sendCommand(IPCConstants.CMD_SHUTDOWN);
        try {
            shm.close();
        } catch (Exception e) {
            LOGGER.error("[RemoteKernel] Error closing shared memory", e);
        }
    }

    /**
     * スリープ状態にする。
     */
    @Override
    public void sleep() {
        shm.sendCommand(IPCConstants.CMD_SLEEP);
    }

    /**
     * ウェイクアップする。
     */
    @Override
    public void wake() {
        shm.sendCommand(IPCConstants.CMD_WAKE);
    }

    /**
     * スリープ状態かどうかを返す。
     */
    @Override
    public boolean isSleeping() {
        return cachedState != null && cachedState.sleeping;
    }

    /**
     * フレームレートを設定する。
     */
    public void frameRate(int fps) {
        shm.sendCommand(IPCConstants.CMD_FRAMERATE, fps);
    }

    /**
     * フレームレートを取得する。
     */
    public int getFrameRate() {
        return cachedState != null ? cachedState.frameRate : 60;
    }

    // === グループC: 入力（共有メモリキュー経由） ===

    /**
     * マウスプレスイベントを送信する。
     */
    @Override
    public void mousePressed(int x, int y) {
        shm.writeInputEvent(InputEvent.mousePressed(x, y, getModifiers()));
    }

    /**
     * マウスリリースイベントを送信する。
     */
    @Override
    public void mouseReleased(int x, int y) {
        shm.writeInputEvent(InputEvent.mouseReleased(x, y, getModifiers()));
    }

    /**
     * マウスドラッグイベントを送信する。
     */
    @Override
    public void mouseDragged(int x, int y) {
        shm.writeInputEvent(InputEvent.mouseDragged(x, y, getModifiers()));
    }

    /**
     * マウスムーブイベントを送信する。
     */
    public void mouseMoved(int x, int y) {
        shm.writeInputEvent(InputEvent.mouseMoved(x, y, getModifiers()));
    }

    /**
     * マウスホイールイベントを送信する。
     */
    @Override
    public void mouseWheel(int x, int y, float delta) {
        shm.writeInputEvent(InputEvent.mouseWheel(x, y, delta, getModifiers()));
    }

    /**
     * キープレスイベントを送信する。
     */
    @Override
    public void keyPressed(char key, int keyCode) {
        updateModifierState(keyCode, true);
        shm.writeInputEvent(InputEvent.keyboard(IPCConstants.INPUT_KEY_PRESSED, key, keyCode, getModifiers()));
    }

    /**
     * キーリリースイベントを送信する。
     */
    @Override
    public void keyReleased(char key, int keyCode) {
        updateModifierState(keyCode, false);
        shm.writeInputEvent(InputEvent.keyboard(IPCConstants.INPUT_KEY_RELEASED, key, keyCode, getModifiers()));
    }

    /**
     * 修飾キー状態を更新する。
     */
    private void updateModifierState(int keyCode, boolean pressed) {
        switch (keyCode) {
            case 16: // Shift
                localShiftState = pressed;
                break;
            case 17: // Ctrl
                localCtrlState = pressed;
                break;
            case 18: // Alt
                localAltState = pressed;
                break;
            case 157: // Meta (Command on Mac)
            case 524: // Windows key
                localMetaState = pressed;
                break;
        }
    }

    /**
     * 修飾キーフラグを取得する。
     */
    private int getModifiers() {
        int mods = 0;
        if (localShiftState) mods |= IPCConstants.MOD_SHIFT;
        if (localCtrlState) mods |= IPCConstants.MOD_CTRL;
        if (localAltState) mods |= IPCConstants.MOD_ALT;
        if (localMetaState) mods |= IPCConstants.MOD_META;
        return mods;
    }

    // === グループD: 修飾キー状態（ローカル管理） ===

    public boolean isShiftPressed() {
        return localShiftState;
    }

    public boolean isCtrlPressed() {
        return localCtrlState;
    }

    public boolean isAltPressed() {
        return localAltState;
    }

    public boolean isMetaPressed() {
        return localMetaState;
    }

    // === グループE: ナビゲーション/UI（IPC経由） ===

    /**
     * ホームボタン処理。
     */
    public void handleHomeButton() {
        shm.sendCommand(IPCConstants.CMD_HOME_BUTTON);
    }

    /**
     * ホーム画面に戻る。
     */
    @Override
    public void requestGoHome() {
        shm.sendCommand(IPCConstants.CMD_GO_HOME);
    }

    /**
     * テキスト入力フォーカスがあるかどうか。
     */
    public boolean hasTextInputFocus() {
        return cachedState != null && cachedState.hasTextInputFocus;
    }

    /**
     * レイヤーを追加する。
     */
    public void addLayer(LayerType layerType) {
        shm.sendCommand(IPCConstants.CMD_ADD_LAYER, layerType.ordinal());
    }

    /**
     * レイヤーを削除する。
     */
    public void removeLayer(LayerType layerType) {
        shm.sendCommand(IPCConstants.CMD_REMOVE_LAYER, layerType.ordinal());
    }

    /**
     * 最上位クローズ可能レイヤーを取得する。
     */
    public LayerType getTopMostClosableLayer() {
        if (cachedState == null || cachedState.topClosableLayer < 0) {
            return null;
        }
        LayerType[] values = LayerType.values();
        if (cachedState.topClosableLayer < values.length) {
            return values[cachedState.topClosableLayer];
        }
        return null;
    }

    /**
     * 座標が画面内かどうか。
     */
    public boolean isInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    /**
     * 優先度を取得する。
     */
    public int getPriority() {
        return 0;
    }

    /**
     * デバッグモードかどうか。
     */
    public boolean isDebugMode() {
        return cachedState != null && cachedState.debugMode;
    }

    // === グループF: ハードウェアSetter（no-op - サーバー側で独自実装） ===
    // ProcessingScreenはこれらを使用しないため、すべてno-op

    // === グループG: マネージャー/サービスGetter（null - サーバー側で管理） ===

    /**
     * スクリーンマネージャーを取得する（ProcessingScreenでnullチェックのみ使用）。
     */
    @Override
    public Object getScreenManager() {
        // ProcessingScreenはnullチェックのみに使用するため、
        // 接続済みでサーバー準備完了なら非nullを返す
        return (shm.isConnected() && isServerReady()) ? this : null;
    }

    /**
     * ロガーを取得する。
     * Note: ProcessingScreenはロガーを使用しないため、nullを返しても問題ない。
     *
     * @return null（サーバー側で管理）
     */
    public Object getLogger() {
        return null;
    }

    /**
     * 日本語フォントを取得する。
     */
    public PFont getJapaneseFont() {
        return null;
    }
}
