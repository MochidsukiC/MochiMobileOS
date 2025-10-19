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
     * 毎フレーム実行される処理。
     * このメソッドはスクリーンがバックグラウンドでもフォアグラウンドでも呼び出される。
     * 描画に依存しないロジック（タイマー、ネットワーク処理、音声再生など）を実装する。
     * バックグラウンドアプリの継続的な処理に使用される。
     * デフォルト実装は何もしない。
     */
    default void tick() {
        // デフォルト実装：何もしない
        // 各実装クラスでオーバーライドしてください
    }

    /**
     * バックグラウンドサービス初期化時に呼び出される。
     * OS起動時に自動起動リストに登録されているアプリはこのメソッドで初期化される。
     * アプリ本体が起動していなくても、バックグラウンドサービスとして動作するための準備を行う。
     * デフォルト実装は何もしない。
     */
    default void backgroundInit() {
        // デフォルト実装：何もしない
        // バックグラウンドサービスとして動作する場合にオーバーライドしてください
    }

    /**
     * バックグラウンドサービスとして毎フレーム実行される処理。
     * OS起動時から終了まで、アプリ本体が起動していなくても呼び出され続ける。
     * 自動起動リストに登録されたアプリのみが対象。
     * 例：音楽プレイヤーのバックグラウンド再生、メッセージ通知監視など。
     * デフォルト実装は何もしない。
     */
    default void background() {
        // デフォルト実装：何もしない
        // バックグラウンドサービスとして動作する場合にオーバーライドしてください
    }

    /**
     * スクリーンがフォアグラウンド（画面に表示）に移行した時に呼び出される。
     * アプリがユーザーに表示され、操作可能になったタイミング。
     * UIの更新、一時停止していた処理の再開などを行う。
     * デフォルト実装は何もしない。
     */
    default void onForeground() {
        // デフォルト実装：何もしない
        // 必要に応じてオーバーライドしてください
    }

    /**
     * スクリーンがバックグラウンドに移行した時に呼び出される。
     * 別のアプリが起動されて、画面から非表示になったタイミング。
     * リソースの解放、状態の保存などを行う。
     * デフォルト実装は何もしない。
     */
    default void onBackground() {
        // デフォルト実装：何もしない
        // 必要に応じてオーバーライドしてください
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
     * マウスホイールイベントが発生した時に呼び出される（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     * このメソッドでスクロール操作を処理する。
     * デフォルト実装は何もしない。
     *
     * @param g 描画・計算処理用のPGraphicsインスタンス
     * @param mouseX マウス位置のx座標
     * @param mouseY マウス位置のy座標
     * @param delta スクロール量（正の値：下スクロール、負の値：上スクロール）
     */
    default void mouseWheel(PGraphics g, int mouseX, int mouseY, float delta) {
        // デフォルト実装：何もしない
    }

    /**
     * マウスホイールイベントが発生した時に呼び出される（PApplet版）。
     * 互換性のために残存。段階的にPGraphics版に移行予定。
     *
     * @deprecated Use {@link #mouseWheel(PGraphics, int, int, float)} instead. This method will be removed in a future version.
     * @param p 描画・計算処理用のPAppletインスタンス
     * @param mouseX マウス位置のx座標
     * @param mouseY マウス位置のy座標
     * @param delta スクロール量（正の値：下スクロール、負の値：上スクロール）
     */
    @Deprecated
    default void mouseWheel(PApplet p, int mouseX, int mouseY, float delta) {
        // デフォルト実装：PGraphics版を呼び出すブリッジ
        mouseWheel(p.g, mouseX, mouseY, delta);
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

    /**
     * 修飾キー（Shift/Ctrl）の状態を設定する。
     * ScreenManagerから呼び出され、フォーカスされたテキスト入力コンポーネントに伝播される。
     * デフォルト実装は何もしない。
     *
     * @param shift Shiftキーが押されているかどうか
     * @param ctrl Ctrlキーが押されているかどうか
     */
    default void setModifierKeys(boolean shift, boolean ctrl) {
        // デフォルト実装：何もしない
        // テキスト入力を持つスクリーンでオーバーライドしてください
    }

    /**
     * このスクリーンにフォーカスされたテキスト入力コンポーネントがあるかチェック。
     * スペースキー処理の前にKernelから呼び出される。
     * デフォルト実装はfalseを返す。
     *
     * @return フォーカスされたコンポーネントがある場合true
     */
    default boolean hasFocusedComponent() {
        // デフォルト実装：フォーカスされたコンポーネントなし
        return false;
    }
}