package jp.moyashi.phoneos.core.ui;

import processing.core.PApplet;

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
     */
    void setup();
    
    /**
     * スクリーンのコンテンツを描画するために継続的に呼び出される。
     * このメソッドはスクリーンのすべての視覚的レンダリングを処理する必要がある。
     * 
     * @param p 描画操作用のPAppletインスタンス
     */
    void draw(PApplet p);
    
    /**
     * このスクリーンがアクティブな間にマウスプレスイベントが発生した時に呼び出される。
     * このメソッドでユーザー入力とナビゲーションロジックを処理する。
     * 
     * @param mouseX マウスプレスのx座標
     * @param mouseY マウスプレスのy座標
     */
    void mousePressed(int mouseX, int mouseY);
    
    /**
     * スクリーンが非表示になるまたは破棄される直前に呼び出される。
     * このメソッドを使用してリソースをクリーンアップしたり状態を保存したりする。
     * デフォルト実装は何もしない。
     */
    default void cleanup() {
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