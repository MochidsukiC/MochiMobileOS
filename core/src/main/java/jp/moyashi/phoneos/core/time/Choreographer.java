package jp.moyashi.phoneos.core.time;

import jp.moyashi.phoneos.core.service.LoggerService;

/**
 * Android-style Choreographer。
 * 仮想V-Sync (60Hz) に基づいたフレーム処理を管理する。
 *
 * フェーズ順序:
 * 1. Input - 入力イベント配送
 * 2. Animation - アニメーション更新
 * 3. Traversal (Logic) - Screen.tick() 等
 * 4. Draw - 描画 (Dirty Flagがtrueの場合のみ)
 */
public class Choreographer {

    /** 時間管理 */
    private final Time time;

    /** ロガー */
    private LoggerService logger;

    /** 描画が必要かどうか (Dirty Flag) */
    private boolean renderDirty = true;

    /** フレームコールバック */
    private FrameCallback frameCallback;

    /** デバッグモード */
    private boolean debugMode = false;

    /** 初回フレームかどうか */
    private boolean firstFrame = true;

    /**
     * フレームコールバックインターフェース。
     * 各フェーズの処理をKernelから注入する。
     */
    public interface FrameCallback {
        /** 入力フェーズ */
        void doInputPhase();

        /** アニメーションフェーズ */
        void doAnimationPhase(long vsyncNanos);

        /** ロジックフェーズ (Screen.tick()等) */
        void doTraversalPhase();

        /** 描画フェーズ */
        void doDrawPhase();
    }

    /**
     * Choreographerを構築する。
     */
    public Choreographer() {
        this.time = new Time();
    }

    /**
     * ロガーを設定する。
     * @param logger ロガーサービス
     */
    public void setLogger(LoggerService logger) {
        this.logger = logger;
    }

    /**
     * フレームコールバックを設定する。
     * @param callback フレームコールバック
     */
    public void setFrameCallback(FrameCallback callback) {
        this.frameCallback = callback;
    }

    /**
     * ホストから呼び出されるメインエントリーポイント。
     * 必要な数だけ仮想フレームを処理する (Catch-up)。
     */
    public void doFrame() {
        if (time.isPaused()) {
            return;
        }

        // 初回フレームは1回だけ実行
        if (firstFrame) {
            doSingleFrame();
            firstFrame = false;
            return;
        }

        int pendingFrames = time.calculatePendingFrames();

        // Catch-up: 遅れたフレーム分を処理 (最低1フレームは実行)
        int framesToProcess = Math.max(1, pendingFrames);
        for (int i = 0; i < framesToProcess; i++) {
            doSingleFrame();
        }
    }

    /**
     * 1フレーム分の処理を実行する。
     * フェーズ順序を厳密に守る。
     */
    private void doSingleFrame() {
        if (frameCallback == null) {
            if (logger != null) {
                logger.warn("Choreographer", "No frame callback set");
            }
            return;
        }

        long frameStartNanos = System.nanoTime();

        // Phase 1: Input
        try {
            frameCallback.doInputPhase();
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Choreographer", "Input phase error: " + e.getMessage(), e);
            }
        }

        // Phase 2: Animation
        try {
            frameCallback.doAnimationPhase(time.getVsyncTargetNanos());
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Choreographer", "Animation phase error: " + e.getMessage(), e);
            }
        }

        // Phase 3: Traversal (Logic)
        try {
            frameCallback.doTraversalPhase();
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Choreographer", "Traversal phase error: " + e.getMessage(), e);
            }
        }

        // Phase 4: Draw (条件付き)
        if (renderDirty) {
            try {
                frameCallback.doDrawPhase();
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Choreographer", "Draw phase error: " + e.getMessage(), e);
                }
            }
            renderDirty = false;
        }

        // フレームを進める
        time.advanceFrame();

        // パフォーマンス計測
        long frameDurationNanos = System.nanoTime() - frameStartNanos;
        if (debugMode && frameDurationNanos > Time.VSYNC_INTERVAL_NS && logger != null) {
            logger.warn("Choreographer", String.format(
                    "Frame %d took %.2fms (target: %.2fms)",
                    time.getTotalFrameCount(),
                    frameDurationNanos / 1_000_000.0,
                    Time.VSYNC_INTERVAL_NS / 1_000_000.0
            ));
        }
    }

    /**
     * 描画をリクエストする (Dirty Flag設定)。
     * 画面状態が変化した時に呼び出す。
     */
    public void requestRender() {
        this.renderDirty = true;
    }

    /**
     * 強制的に描画を実行する。
     * ホストのrender()呼び出し時に使用。
     * Dirty Flagに関係なく描画フェーズを実行する。
     */
    public void forceRender() {
        if (frameCallback != null) {
            try {
                frameCallback.doDrawPhase();
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Choreographer", "Force render error: " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Timeインスタンスを取得する。
     * @return Timeインスタンス
     */
    public Time getTime() {
        return time;
    }

    /**
     * 現在の総フレーム数を取得。
     * @return 総フレーム数
     */
    public long getFrameCount() {
        return time.getTotalFrameCount();
    }

    /**
     * 時間スケールを設定する。
     * @param scale 時間スケール (0.1〜5.0)
     */
    public void setTimeScale(float scale) {
        time.setTimeScale(scale);
    }

    /**
     * 時間スケールを取得する。
     * @return 時間スケール
     */
    public float getTimeScale() {
        return time.getTimeScale();
    }

    /**
     * 一時停止する。
     */
    public void pause() {
        time.setPaused(true);
    }

    /**
     * 再開する。
     */
    public void resume() {
        time.setPaused(false);
    }

    /**
     * 一時停止中かどうかを取得する。
     * @return 一時停止中の場合true
     */
    public boolean isPaused() {
        return time.isPaused();
    }

    /**
     * デバッグモードを設定する。
     * @param debug デバッグモードを有効にする場合true
     */
    public void setDebugMode(boolean debug) {
        this.debugMode = debug;
    }

    /**
     * デバッグモードかどうかを取得する。
     * @return デバッグモードの場合true
     */
    public boolean isDebugMode() {
        return debugMode;
    }

    /**
     * Dirty Flagの状態を取得する。
     * @return 描画が必要な場合true
     */
    public boolean isRenderDirty() {
        return renderDirty;
    }
}
