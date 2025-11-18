package jp.moyashi.phoneos.core.ui.effects;

import processing.core.PGraphics;

/**
 * 簡易エレベーション（影）レンダラ。
 * Processingの簡易描画のみで、複数レイヤーの半透明矩形を重ねて影を表現する。
 */
public final class Elevation {
    private Elevation() {}

    /**
     * レベル1〜5の簡易影を描画。
     */
    public static void drawRectShadow(PGraphics g, float x, float y, float w, float h, float radius, int level) {
        if (level <= 0) return;
        int layers = Math.min(5, Math.max(1, level));
        float maxOffset = 6 + (level - 1) * 2;
        float maxAlpha = 50 + (level - 1) * 10; // 50..90

        g.pushStyle();
        g.noStroke();

        for (int i = 0; i < layers; i++) {
            float t = (i + 1) / (float) layers; // 0..1
            float off = t * maxOffset;
            float alpha = (1 - t) * maxAlpha;
            g.fill(0, 0, 0, (int) alpha);
            g.rect(x, y + off, w, h, radius);
        }

        g.popStyle();
    }
}

