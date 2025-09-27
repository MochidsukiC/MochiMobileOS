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
     * スクリーンが最初に作成された時またはアクティブになった時に呼び出される。
     * このメソッドを使用してスクリーン固有のリソースと状態を初期化する。
     * 
     * @param p 初期化処理用のPAppletインスタンス
     */
    void setup(PApplet p);
    
    /**
     * スクリーンのコンテンツを描画するために継続的に呼び出される。
     * このメソッドはスクリーンのすべての視覚的レンダリングを処理する必要がある。
     *
     * @param p 描画操作用のPAppletインスタンス
     */
    void draw(PApplet p);

    /**
     * PGraphics対応のスクリーン描画メソッド。
     * 新しいアーキテクチャでの描画処理に使用される。
     *
     * @param g 描画操作用のPGraphicsインスタンス
     */
    default void draw(PGraphics g) {
        // デフォルト実装：サブクラスでの実装が必要であることを示す
        System.out.println("Warning: " + this.getClass().getSimpleName() +
                         " does not implement draw(PGraphics). Skipping draw.");
    }
    
    /**
     * このスクリーンがアクティブな間にマウスプレスイベントが発生した時に呼び出される。
     * このメソッドでユーザー入力とナビゲーションロジックを処理する。
     * 
     * @param p 描画・計算処理用のPAppletインスタンス
     * @param mouseX マウスプレスのx座標
     * @param mouseY マウスプレスのy座標
     */
    void mousePressed(PApplet p, int mouseX, int mouseY);
    
    /**
     * キーボード入力イベントが発生した時に呼び出される。
     * このメソッドでキーボード入力を処理する。
     * デフォルト実装は何もしない。
     * 
     * @param p 描画・計算処理用のPAppletインスタンス
     * @param key 押されたキー
     * @param keyCode キーコード
     */
    default void keyPressed(PApplet p, char key, int keyCode) {
        // Default empty implementation
    }
    
    /**
     * マウスドラッグイベントが発生した時に呼び出される。
     * このメソッドでドラッグ操作を処理する。
     * デフォルト実装は何もしない。
     * 
     * @param p 描画・計算処理用のPAppletインスタンス
     * @param mouseX ドラッグ位置のx座標
     * @param mouseY ドラッグ位置のy座標
     */
    default void mouseDragged(PApplet p, int mouseX, int mouseY) {
        // Default empty implementation
    }
    
    /**
     * マウスリリースイベントが発生した時に呼び出される。
     * このメソッドでマウスリリース操作を処理する。
     * デフォルト実装は何もしない。
     * 
     * @param p 描画・計算処理用のPAppletインスタンス
     * @param mouseX リリース位置のx座標
     * @param mouseY リリース位置のy座標
     */
    default void mouseReleased(PApplet p, int mouseX, int mouseY) {
        // Default empty implementation
    }
    
    /**
     * スクリーンが非表示になるまたは破棄される直前に呼び出される。
     * このメソッドを使用してリソースをクリーンアップしたり状態を保存したりする。
     * デフォルト実装は何もしない。
     * 
     * @param p クリーンアップ処理用のPAppletインスタンス
     */
    default void cleanup(PApplet p) {
        // Default empty implementation
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