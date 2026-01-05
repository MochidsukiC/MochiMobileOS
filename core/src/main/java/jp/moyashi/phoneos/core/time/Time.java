package jp.moyashi.phoneos.core.time;

/**
 * ナノ秒精度の時間管理クラス。
 * 仮想V-Sync (60Hz) ベースの時間進行を管理する。
 * 累積誤差を排除するため整数演算のみ使用。
 */
public class Time {

    /** 60Hz V-Syncの1フレーム間隔 (ナノ秒) */
    public static final long VSYNC_INTERVAL_NS = 16_666_666L; // 約16.66ms

    /** 目標FPS */
    public static final int TARGET_FPS = 60;

    /** 時間スケール係数 (1.0 = 通常速度, 0.5 = スローモーション) */
    private float timeScale = 1.0f;

    /** システム起動時のナノ秒タイムスタンプ */
    private final long systemStartNanos;

    /** 前回のV-Sync時刻 (ナノ秒) */
    private long lastVsyncNanos;

    /** 経過した仮想フレーム数 */
    private long totalFrameCount;

    /** 現在のV-Sync予定時刻 (アニメーション用) */
    private long currentVsyncTargetNanos;

    /** 一時停止中かどうか */
    private boolean paused = false;

    /** 一時停止開始時のナノ秒タイムスタンプ */
    private long pauseStartNanos;

    /** 累積一時停止時間 (ナノ秒) */
    private long totalPausedNanos = 0;

    public Time() {
        this.systemStartNanos = System.nanoTime();
        this.lastVsyncNanos = systemStartNanos;
        this.currentVsyncTargetNanos = systemStartNanos;
        this.totalFrameCount = 0;
    }

    /**
     * 現在時刻から経過したV-Syncフレーム数を計算する。
     * Catch-up処理で使用。
     *
     * @return 前回更新から経過したフレーム数 (0以上)
     */
    public int calculatePendingFrames() {
        if (paused) return 0;

        long currentNanos = System.nanoTime();
        long elapsedNanos = currentNanos - lastVsyncNanos;

        // timeScaleを考慮した経過時間
        long scaledElapsedNanos = (long)(elapsedNanos * timeScale);

        // 経過フレーム数を計算 (整数除算で累積誤差を防ぐ)
        int pendingFrames = (int)(scaledElapsedNanos / VSYNC_INTERVAL_NS);

        // 最大catch-upフレーム数を制限 (暴走防止)
        return Math.min(pendingFrames, 10);
    }

    /**
     * 1フレーム進める。
     * Choreographerのループ内で呼び出される。
     */
    public void advanceFrame() {
        long interval = (long)(VSYNC_INTERVAL_NS / timeScale);
        lastVsyncNanos += interval;
        currentVsyncTargetNanos = lastVsyncNanos + interval;
        totalFrameCount++;
    }

    /**
     * 現在のV-Sync予定時刻を取得 (アニメーション補間用)。
     * @return ナノ秒タイムスタンプ
     */
    public long getVsyncTargetNanos() {
        return currentVsyncTargetNanos;
    }

    /**
     * システム起動からの経過時間をミリ秒で取得。
     * @return 経過ミリ秒
     */
    public long getUptimeMillis() {
        long currentNanos = System.nanoTime();
        long elapsed = currentNanos - systemStartNanos - totalPausedNanos;
        if (paused) {
            elapsed -= (currentNanos - pauseStartNanos);
        }
        return elapsed / 1_000_000L;
    }

    /**
     * 現在のフレーム時間 (デルタタイム) を秒で取得。
     * @return デルタタイム (秒)
     */
    public float getDeltaTime() {
        return (VSYNC_INTERVAL_NS / timeScale) / 1_000_000_000.0f;
    }

    /**
     * フレーム番号ベースのアニメーション進捗を計算。
     * @param durationFrames アニメーション全体のフレーム数
     * @param startFrame 開始フレーム番号
     * @return 0.0〜1.0の進捗値
     */
    public float getAnimationProgress(int durationFrames, long startFrame) {
        if (durationFrames <= 0) return 1.0f;
        long elapsed = totalFrameCount - startFrame;
        return Math.min(1.0f, (float)elapsed / durationFrames);
    }

    /**
     * 時間スケールを取得する。
     * @return 時間スケール (1.0 = 通常)
     */
    public float getTimeScale() {
        return timeScale;
    }

    /**
     * 時間スケールを設定する。
     * @param scale 時間スケール (0.1〜5.0)
     */
    public void setTimeScale(float scale) {
        this.timeScale = Math.max(0.1f, Math.min(5.0f, scale));
    }

    /**
     * 総フレーム数を取得する。
     * @return 総フレーム数
     */
    public long getTotalFrameCount() {
        return totalFrameCount;
    }

    /**
     * 一時停止状態を設定する。
     * @param paused 一時停止する場合true
     */
    public void setPaused(boolean paused) {
        if (this.paused == paused) return;

        long currentNanos = System.nanoTime();
        if (paused) {
            // 一時停止開始
            this.pauseStartNanos = currentNanos;
        } else {
            // 一時停止解除
            long pausedDuration = currentNanos - pauseStartNanos;
            this.totalPausedNanos += pausedDuration;
            // lastVsyncNanosも調整して、再開時にcatch-upが暴走しないようにする
            this.lastVsyncNanos += pausedDuration;
        }
        this.paused = paused;
    }

    /**
     * 一時停止中かどうかを取得する。
     * @return 一時停止中の場合true
     */
    public boolean isPaused() {
        return paused;
    }

    /**
     * 現在の実時間ナノ秒を取得する。
     * @return System.nanoTime()の値
     */
    public long getCurrentNanos() {
        return System.nanoTime();
    }

    /**
     * 現在の実時間ミリ秒を取得する。
     * @return System.currentTimeMillis()の値
     */
    public long getCurrentMillis() {
        return System.currentTimeMillis();
    }
}
