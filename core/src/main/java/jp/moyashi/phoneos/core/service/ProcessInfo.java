package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.ui.Screen;

/**
 * アプリケーションプロセスの実行状態を保持するデータクラス。
 * ServiceManagerで管理されるアプリのメタデータと統計情報を提供する。
 *
 * @author MochiMobileOS
 * @version 1.0
 */
public class ProcessInfo {

    /**
     * プロセス優先度の列挙型。
     * 優先度に応じてtick実行頻度やリソース配分が変わる。
     */
    public enum Priority {
        /** 最高優先度: 毎フレーム実行、システムアプリ用 */
        HIGH,
        /** 通常優先度: 毎フレーム実行、一般アプリ用 */
        NORMAL,
        /** 低優先度: 5フレームに1回実行、バッテリーセーバー時用 */
        LOW,
        /** バックグラウンド: 10フレームに1回実行、バックグラウンドサービス用 */
        BACKGROUND
    }

    private final String appId;
    private final Screen screen;
    private Priority priority;
    private boolean isForeground;
    private boolean isBackgroundService;

    // 統計情報
    private long totalTickTime;      // 累積tick実行時間（ナノ秒）
    private long totalDrawTime;      // 累積draw実行時間（ナノ秒）
    private int launchCount;         // 起動回数
    private long lastLaunchTime;     // 最終起動時刻（ミリ秒）
    private int crashCount;          // クラッシュ回数

    /**
     * ProcessInfoを作成する。
     *
     * @param appId アプリケーションID
     * @param screen Screenインスタンス
     */
    public ProcessInfo(String appId, Screen screen) {
        this.appId = appId;
        this.screen = screen;
        this.priority = Priority.NORMAL;
        this.isForeground = false;
        this.isBackgroundService = false;

        this.totalTickTime = 0;
        this.totalDrawTime = 0;
        this.launchCount = 0;
        this.lastLaunchTime = System.currentTimeMillis();
        this.crashCount = 0;
    }

    // ==================== Getters ====================

    public String getAppId() {
        return appId;
    }

    public Screen getScreen() {
        return screen;
    }

    public Priority getPriority() {
        return priority;
    }

    public boolean isForeground() {
        return isForeground;
    }

    public boolean isBackgroundService() {
        return isBackgroundService;
    }

    public long getTotalTickTime() {
        return totalTickTime;
    }

    public long getTotalDrawTime() {
        return totalDrawTime;
    }

    public int getLaunchCount() {
        return launchCount;
    }

    public long getLastLaunchTime() {
        return lastLaunchTime;
    }

    public int getCrashCount() {
        return crashCount;
    }

    // ==================== Setters ====================

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public void setForeground(boolean foreground) {
        this.isForeground = foreground;
    }

    public void setBackgroundService(boolean backgroundService) {
        this.isBackgroundService = backgroundService;
    }

    // ==================== 統計情報の更新 ====================

    /**
     * tick実行時間を加算する。
     *
     * @param nanos 実行時間（ナノ秒）
     */
    public void addTickTime(long nanos) {
        this.totalTickTime += nanos;
    }

    /**
     * draw実行時間を加算する。
     *
     * @param nanos 実行時間（ナノ秒）
     */
    public void addDrawTime(long nanos) {
        this.totalDrawTime += nanos;
    }

    /**
     * 起動回数をインクリメントする。
     */
    public void incrementLaunchCount() {
        this.launchCount++;
        this.lastLaunchTime = System.currentTimeMillis();
    }

    /**
     * クラッシュ回数をインクリメントする。
     */
    public void incrementCrashCount() {
        this.crashCount++;
    }

    // ==================== ユーティリティ ====================

    /**
     * tick実行が必要かどうかを判定する（優先度に基づく）。
     *
     * @param frameCount 現在のフレーム数
     * @return tick実行が必要な場合true
     */
    public boolean shouldTick(long frameCount) {
        switch (priority) {
            case HIGH:
            case NORMAL:
                return true; // 毎フレーム実行
            case LOW:
                return frameCount % 5 == 0; // 5フレームに1回
            case BACKGROUND:
                return frameCount % 10 == 0; // 10フレームに1回
            default:
                return true;
        }
    }

    /**
     * 平均tick時間を取得する（ミリ秒）。
     *
     * @return 平均tick時間
     */
    public double getAverageTickTimeMs() {
        if (launchCount == 0) return 0.0;
        return (totalTickTime / 1_000_000.0) / launchCount;
    }

    /**
     * 平均draw時間を取得する（ミリ秒）。
     *
     * @return 平均draw時間
     */
    public double getAverageDrawTimeMs() {
        if (launchCount == 0) return 0.0;
        return (totalDrawTime / 1_000_000.0) / launchCount;
    }

    @Override
    public String toString() {
        return String.format("ProcessInfo{appId='%s', priority=%s, foreground=%s, backgroundService=%s, " +
                        "launchCount=%d, avgTickTime=%.2fms, avgDrawTime=%.2fms, crashCount=%d}",
                appId, priority, isForeground, isBackgroundService,
                launchCount, getAverageTickTimeMs(), getAverageDrawTimeMs(), crashCount);
    }
}
