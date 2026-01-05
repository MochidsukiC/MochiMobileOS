package jp.moyashi.phoneos.core.time;

import jp.moyashi.phoneos.core.ui.Screen;

import java.util.HashMap;
import java.util.Map;

/**
 * Screen別のティックレート管理。
 * 各Screenが要求するTPSに基づいて、間引き実行を行う。
 */
public class ScreenTickScheduler {

    /** Screen別の最終実行フレーム番号 */
    private final Map<Screen, Long> lastTickFrames = new HashMap<>();

    /** 現在のフレーム番号への参照 */
    private final Time time;

    /**
     * ScreenTickSchedulerを構築する。
     * @param time Timeインスタンス
     */
    public ScreenTickScheduler(Time time) {
        this.time = time;
    }

    /**
     * このスクリーンがこのフレームでtick()を実行すべきかを判定する。
     *
     * @param screen 判定対象のScreen
     * @return tick()を実行すべき場合true
     */
    public boolean shouldTick(Screen screen) {
        int targetTPS = screen.getTargetTPS();
        long currentFrame = time.getTotalFrameCount();

        // デフォルト(60Hz)以上の場合は常にtrue
        if (targetTPS >= Time.TARGET_FPS) {
            return true;
        }

        // 0以下の場合はtickしない
        if (targetTPS <= 0) {
            return false;
        }

        // 間引き間隔を計算 (例: 1Hz = 60フレームに1回)
        int interval = Math.max(1, Time.TARGET_FPS / targetTPS);

        // 最終実行からの経過フレーム数をチェック
        Long lastFrame = lastTickFrames.get(screen);
        if (lastFrame == null || (currentFrame - lastFrame) >= interval) {
            lastTickFrames.put(screen, currentFrame);
            return true;
        }

        return false;
    }

    /**
     * Screenが削除された時にクリーンアップする。
     * @param screen 削除するScreen
     */
    public void unregisterScreen(Screen screen) {
        lastTickFrames.remove(screen);
    }

    /**
     * 全てのスケジュール情報をクリアする。
     */
    public void clear() {
        lastTickFrames.clear();
    }

    /**
     * 登録されているScreen数を取得する。
     * @return Screen数
     */
    public int getRegisteredScreenCount() {
        return lastTickFrames.size();
    }
}
