package jp.moyashi.phoneos.standalone;

import jp.moyashi.phoneos.core.Kernel;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * StandaloneのPAppletイベントをcore独立APIに変換するラッパークラス。
 * PGraphics統一アーキテクチャにおいて、PAppletイベントシステムを
 * coreモジュールの独立API呼び出しに変換する責務を持つ。
 *
 * アーキテクチャ:
 * PApplet draw() → Kernel.update() + Kernel.render() → PGraphics描画 → PApplet画面出力
 * PApplet events → Kernel独立APIイベント
 *
 * @author jp.moyashi
 * @version 1.0 (PGraphics統一アーキテクチャ対応)
 */
public class StandaloneWrapper extends PApplet {

    /** MochiMobileOSのカーネルインスタンス */
    private Kernel kernel;

    /** 画面幅 */
    private static final int SCREEN_WIDTH = 400;

    /** 画面高さ */
    private static final int SCREEN_HEIGHT = 600;

    /**
     * Processing設定メソッド。
     * 画面サイズとその他の初期設定を行う。
     */
    @Override
    public void settings() {
        size(SCREEN_WIDTH, SCREEN_HEIGHT);
        System.out.println("StandaloneWrapper: Processing窓口設定完了 (" + SCREEN_WIDTH + "x" + SCREEN_HEIGHT + ")");
    }

    /**
     * Processing初期化メソッド。
     * Kernelの初期化を行う。
     */
    @Override
    public void setup() {
        System.out.println("StandaloneWrapper: Kernel初期化開始...");

        // Kernelを作成し、PGraphics統一アーキテクチャで初期化
        kernel = new Kernel();
        kernel.initialize(this, SCREEN_WIDTH, SCREEN_HEIGHT);

        System.out.println("StandaloneWrapper: Kernel初期化完了");
    }

    /**
     * Processing描画ループ。
     * Kernelの独立APIを呼び出してPGraphicsバッファを取得し、画面に描画する。
     */
    @Override
    public void draw() {
        if (kernel == null) {
            background(255, 0, 0);
            fill(255);
            textAlign(CENTER, CENTER);
            text("Kernel初期化中...", width/2, height/2);
            return;
        }

        try {
            // Kernelの更新処理を実行
            kernel.update();

            // Kernelの描画処理を実行（PGraphicsバッファに描画）
            kernel.render();

            // KernelのPGraphicsバッファを取得してPApplet画面に描画
            PGraphics kernelGraphics = kernel.getGraphics();
            if (kernelGraphics != null) {
                image(kernelGraphics, 0, 0);
            } else {
                // フォールバック: Kernelグラフィックスが利用できない場合
                background(50, 50, 50);
                fill(255);
                textAlign(CENTER, CENTER);
                text("Kernel Graphics Not Available", width/2, height/2);
            }

        } catch (Exception e) {
            System.err.println("StandaloneWrapper: 描画エラー: " + e.getMessage());
            // エラー表示
            background(255, 0, 0);
            fill(255);
            textAlign(CENTER, CENTER);
            text("描画エラー!", width/2, height/2 - 20);
            text(e.getMessage(), width/2, height/2 + 20);
        }
    }

    /**
     * PAppletマウス押下イベント → Kernel独立API変換。
     */
    @Override
    public void mousePressed() {
        if (kernel != null) {
            kernel.mousePressed(mouseX, mouseY);
        }
    }

    /**
     * PAppletマウス離しイベント → Kernel独立API変換。
     */
    @Override
    public void mouseReleased() {
        if (kernel != null) {
            kernel.mouseReleased(mouseX, mouseY);
        }
    }

    /**
     * PAppletマウスドラッグイベント → Kernel独立API変換。
     * ジェスチャー認識に重要な機能。
     */
    @Override
    public void mouseDragged() {
        if (kernel != null) {
            // Kernelの独立mouseDragged APIを呼び出し
            kernel.mouseDragged(mouseX, mouseY);
        }
    }

    /**
     * PAppletキー押下イベント → Kernel独立API変換。
     */
    @Override
    public void keyPressed() {
        System.out.println("StandaloneWrapper: keyPressed - key: '" + key + "', keyCode: " + keyCode);
        if (kernel != null) {
            kernel.keyPressed(key, keyCode);
        } else {
            System.out.println("StandaloneWrapper: kernel is null, cannot forward key event");
        }
    }

    /**
     * PAppletキー離しイベント → Kernel独立API変換。
     */
    @Override
    public void keyReleased() {
        if (kernel != null) {
            kernel.keyReleased(key, keyCode);
        }
    }

    /**
     * アプリ終了時の清片処理。
     */
    @Override
    public void exit() {
        System.out.println("StandaloneWrapper: 終了処理開始...");

        if (kernel != null) {
            try {
                kernel.shutdown();
            } catch (Exception e) {
                System.err.println("StandaloneWrapper: Kernel終了処理エラー: " + e.getMessage());
            }
        }

        System.out.println("StandaloneWrapper: 終了処理完了");
        super.exit();
    }

    /**
     * Kernelインスタンスを取得する。
     *
     * @return Kernelインスタンス
     */
    public Kernel getKernel() {
        return kernel;
    }
}