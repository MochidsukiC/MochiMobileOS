package jp.moyashi.phoneos.core.ui;

import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * スマートフォンOSのすべてのスクリーンのベースインターフェース。
 * このインターフェースはすべてのスクリーン実装が従うべき契約を定義する。
 * スクリーンは描画、ユーザー入力、ライフサイクル管理を扱う。
 * 
 * @author YourName
 * @version 1.0
 */
public interface Screen {
    
    /**
     * スクリーンが最初に作成された時またはアクティブになった時に呼び出される（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     * このメソッドを使用してスクリーン固有のリソースと状態を初期化する。
     *
     * @param g 初期化処理用のPGraphicsインスタンス
     */
    default void setup(PGraphics g) {
        // デフォルト実装：何もしない
        // 各実装クラスでオーバーライドしてください
    }

    /**
     * スクリーンが最初に作成された時またはアクティブになった時に呼び出される（PApplet版）。
     * 互換性のために残存。段階的にPGraphics版に移行予定。
     *
     * @deprecated Use {@link #setup(PGraphics)} instead. This method will be removed in a future version.
     * @param p 初期化処理用のPAppletインスタンス
     */
    @Deprecated
    default void setup(PApplet p) {
        // デフォルト実装：PGraphics版を呼び出すブリッジ
        setup(p.g);
    }
    
    /**
     * スクリーンのコンテンツを描画するために継続的に呼び出される（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     *
     * @param g 描画操作用のPGraphicsインスタンス
     */
    default void draw(PGraphics g) {
        // デフォルト実装：何もしない
        // 各実装クラスでオーバーライドしてください
    }

    /**
     * スクリーンのコンテンツを描画するために継続的に呼び出される（PApplet版）。
     * 互換性のために残存。段階的にPGraphics版に移行予定。
     *
     * @deprecated Use {@link #draw(PGraphics)} instead. This method will be removed in a future version.
     * @param p 描画操作用のPAppletインスタンス
     */
    @Deprecated
    default void draw(PApplet p) {
        // デフォルト実装：PGraphics版を呼び出すブリッジ
        draw(p.g);
    }
    
    /**
     * このスクリーンがアクティブな間にマウスプレスイベントが発生した時に呼び出される（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     * このメソッドでユーザー入力とナビゲーションロジックを処理する。
     *
     * @param g 描画・計算処理用のPGraphicsインスタンス
     * @param mouseX マウスプレスのx座標
     * @param mouseY マウスプレスのy座標
     */
    default void mousePressed(PGraphics g, int mouseX, int mouseY) {
        // デフォルト実装：何もしない
        // 各実装クラスでオーバーライドしてください
    }

    /**
     * このスクリーンがアクティブな間にマウスプレスイベントが発生した時に呼び出される（PApplet版）。
     * 互換性のために残存。段階的にPGraphics版に移行予定。
     *
     * @deprecated Use {@link #mousePressed(PGraphics, int, int)} instead. This method will be removed in a future version.
     * @param p 描画・計算処理用のPAppletインスタンス
     * @param mouseX マウスプレスのx座標
     * @param mouseY マウスプレスのy座標
     */
    @Deprecated
    default void mousePressed(PApplet p, int mouseX, int mouseY) {
        // デフォルト実装：PGraphics版を呼び出すブリッジ
        mousePressed(p.g, mouseX, mouseY);
    }
    
    /**
     * キーボード入力イベントが発生した時に呼び出される（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     * このメソッドでキーボード入力を処理する。
     * デフォルト実装は何もしない。
     *
     * @param g 描画・計算処理用のPGraphicsインスタンス
     * @param key 押されたキー
     * @param keyCode キーコード
     */
    default void keyPressed(PGraphics g, char key, int keyCode) {
        // デフォルト実装：何もしない
    }

    /**
     * キーボード入力イベントが発生した時に呼び出される（PApplet版）。
     * 互換性のために残存。段階的にPGraphics版に移行予定。
     *
     * @deprecated Use {@link #keyPressed(PGraphics, char, int)} instead. This method will be removed in a future version.
     * @param p 描画・計算処理用のPAppletインスタンス
     * @param key 押されたキー
     * @param keyCode キーコード
     */
    @Deprecated
    default void keyPressed(PApplet p, char key, int keyCode) {
        // デフォルト実装：PGraphics版を呼び出すブリッジ
        keyPressed(p.g, key, keyCode);
    }
    
    /**
     * マウスドラッグイベントが発生した時に呼び出される（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     * このメソッドでドラッグ操作を処理する。
     * デフォルト実装は何もしない。
     *
     * @param g 描画・計算処理用のPGraphicsインスタンス
     * @param mouseX ドラッグ位置のx座標
     * @param mouseY ドラッグ位置のy座標
     */
    default void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        // デフォルト実装：何もしない
    }

    /**
     * マウスドラッグイベントが発生した時に呼び出される（PApplet版）。
     * 互換性のために残存。段階的にPGraphics版に移行予定。
     *
     * @deprecated Use {@link #mouseDragged(PGraphics, int, int)} instead. This method will be removed in a future version.
     * @param p 描画・計算処理用のPAppletインスタンス
     * @param mouseX ドラッグ位置のx座標
     * @param mouseY ドラッグ位置のy座標
     */
    @Deprecated
    default void mouseDragged(PApplet p, int mouseX, int mouseY) {
        // デフォルト実装：PGraphics版を呼び出すブリッジ
        mouseDragged(p.g, mouseX, mouseY);
    }
    
    /**
     * マウスリリースイベントが発生した時に呼び出される（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     * このメソッドでマウスリリース操作を処理する。
     * デフォルト実装は何もしない。
     *
     * @param g 描画・計算処理用のPGraphicsインスタンス
     * @param mouseX リリース位置のx座標
     * @param mouseY リリース位置のy座標
     */
    default void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        // デフォルト実装：何もしない
    }

    /**
     * マウスリリースイベントが発生した時に呼び出される（PApplet版）。
     * 互換性のために残存。段階的にPGraphics版に移行予定。
     *
     * @deprecated Use {@link #mouseReleased(PGraphics, int, int)} instead. This method will be removed in a future version.
     * @param p 描画・計算処理用のPAppletインスタンス
     * @param mouseX リリース位置のx座標
     * @param mouseY リリース位置のy座標
     */
    @Deprecated
    default void mouseReleased(PApplet p, int mouseX, int mouseY) {
        // デフォルト実装：PGraphics版を呼び出すブリッジ
        mouseReleased(p.g, mouseX, mouseY);
    }
    
    /**
     * スクリーンが非表示になるまたは破棄される直前に呼び出される（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     * このメソッドを使用してリソースをクリーンアップしたり状態を保存したりする。
     * デフォルト実装は何もしない。
     *
     * @param g クリーンアップ処理用のPGraphicsインスタンス
     */
    default void cleanup(PGraphics g) {
        // デフォルト実装：何もしない
    }

    /**
     * スクリーンが非表示になるまたは破棄される直前に呼び出される（PApplet版）。
     * 互換性のために残存。段階的にPGraphics版に移行予定。
     *
     * @deprecated Use {@link #cleanup(PGraphics)} instead. This method will be removed in a future version.
     * @param p クリーンアップ処理用のPAppletインスタンス
     */
    @Deprecated
    default void cleanup(PApplet p) {
        // デフォルト実装：PGraphics版を呼び出すブリッジ
        cleanup(p.g);
    }
    
    /**
     * このスクリーンのタイトルまたは名前を取得する。
     * これはデバッグやナビゲーション履歴に使用できる。
     * 
     * @return スクリーンのタイトル
     */
    default String getScreenTitle() {
        return this.getClass().getSimpleName();
    }
}