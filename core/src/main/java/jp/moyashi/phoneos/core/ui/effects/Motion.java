package jp.moyashi.phoneos.core.ui.effects;

import jp.moyashi.phoneos.core.service.SettingsManager;

/**
 * モーション（アニメーション）ユーティリティ。
 * - 共通の継続時間とイージング
 * - Reduce Motion（動作を減らす）設定への対応
 */
public final class Motion {
    private Motion() {}

    public static final int DURATION_SHORT = 120;  // ms
    public static final int DURATION_MEDIUM = 200; // ms
    public static final int DURATION_LONG = 300;   // ms

    public static boolean reduce(SettingsManager settings) {
        return settings != null && settings.getBooleanSetting("ui.motion.reduce", false);
    }

    public static boolean lowPower(SettingsManager settings) {
        // TODO: 低電力モード時のモーション簡略化の適用を検討
        return settings != null && settings.getBooleanSetting("ui.performance.low_power", false);
    }

    public static float easeOutCubic(float t) {
        float p = t - 1f;
        return (p * p * p + 1f);
    }

    public static float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float)Math.pow(-2f * t + 2f, 3f) / 2f;
    }

    public static int durationAdjusted(int baseMs, SettingsManager settings) {
        int ms = baseMs;
        if (reduce(settings)) ms = Math.max(60, ms / 2);
        // TODO: 低電力モード（ui.performance.low_power）による継続時間短縮の適用を検討
        return ms;
    }
}
