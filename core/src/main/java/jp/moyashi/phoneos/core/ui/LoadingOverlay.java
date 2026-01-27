package jp.moyashi.phoneos.core.ui;

import jp.moyashi.phoneos.core.ui.theme.ThemeContext;
import jp.moyashi.phoneos.core.ui.theme.ThemeEngine;
import processing.core.PGraphics;
import processing.core.PImage;

/**
 * HTMLアプリケーション用ローディング画面を描画するユーティリティクラス。
 * アプリアイコンを中央に表示し、右下に小さな回転スピナーを表示する。
 */
public class LoadingOverlay {

    /** スピナーのサイズ */
    private static final int SPINNER_SIZE = 24;
    /** スピナーの線の太さ */
    private static final int SPINNER_STROKE = 3;
    /** スピナーの余白（右下からの距離） */
    private static final int SPINNER_MARGIN = 16;
    /** アイコンのサイズ */
    private static final int ICON_SIZE = 64;
    /** アニメーション速度（度/フレーム） */
    private static final float ANIMATION_SPEED = 8f;

    /** 現在のスピナー角度 */
    private static float spinnerAngle = 0;

    /**
     * ローディング画面を描画する。
     *
     * @param g 描画対象のPGraphics
     * @param appIcon アプリアイコン（nullの場合はアイコンなし）
     * @param x 描画領域の左上X座標
     * @param y 描画領域の左上Y座標
     * @param width 描画領域の幅
     * @param height 描画領域の高さ
     */
    public static void draw(PGraphics g, PImage appIcon, int x, int y, int width, int height) {
        ThemeEngine theme = ThemeContext.getTheme();

        // 背景を描画
        g.fill(theme.colorSurface());
        g.noStroke();
        g.rect(x, y, width, height, 8);

        // アプリアイコンを中央に描画
        if (appIcon != null) {
            int iconX = x + (width - ICON_SIZE) / 2;
            int iconY = y + (height - ICON_SIZE) / 2;
            g.image(appIcon, iconX, iconY, ICON_SIZE, ICON_SIZE);
        }

        // 右下にスピナーを描画
        int spinnerX = x + width - SPINNER_SIZE - SPINNER_MARGIN;
        int spinnerY = y + height - SPINNER_SIZE - SPINNER_MARGIN;
        drawSpinner(g, spinnerX, spinnerY, theme);

        // アニメーションを更新
        updateAnimation();
    }

    /**
     * 回転スピナーを描画する。
     *
     * @param g 描画対象のPGraphics
     * @param x スピナーの左上X座標
     * @param y スピナーの左上Y座標
     * @param theme テーマエンジン
     */
    private static void drawSpinner(PGraphics g, int x, int y, ThemeEngine theme) {
        int centerX = x + SPINNER_SIZE / 2;
        int centerY = y + SPINNER_SIZE / 2;
        int radius = (SPINNER_SIZE - SPINNER_STROKE) / 2;

        // スピナーの背景円（薄いグレー）
        g.noFill();
        g.stroke(theme.colorOnSurface() & 0x33FFFFFF); // 20%透明度
        g.strokeWeight(SPINNER_STROKE);
        g.ellipse(centerX, centerY, radius * 2, radius * 2);

        // 回転する円弧（アクセントカラー）
        g.stroke(theme.colorPrimary());
        g.strokeWeight(SPINNER_STROKE);
        g.strokeCap(PGraphics.ROUND);

        // 円弧を描画（90度分）
        float startAngle = (float) Math.toRadians(spinnerAngle);
        float endAngle = (float) Math.toRadians(spinnerAngle + 90);
        g.arc(centerX, centerY, radius * 2, radius * 2, startAngle, endAngle);

        // ストローク設定をリセット
        g.strokeWeight(1);
        g.strokeCap(PGraphics.SQUARE);
    }

    /**
     * アニメーションを更新する。
     * 毎フレーム呼び出してスピナーを回転させる。
     */
    private static void updateAnimation() {
        spinnerAngle += ANIMATION_SPEED;
        if (spinnerAngle >= 360) {
            spinnerAngle -= 360;
        }
    }

    /**
     * アニメーションをリセットする。
     */
    public static void resetAnimation() {
        spinnerAngle = 0;
    }
}
