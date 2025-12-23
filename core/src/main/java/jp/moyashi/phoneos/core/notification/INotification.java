package jp.moyashi.phoneos.core.notification;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

/**
 * 通知アイテムのインターフェース。
 * 通知センターに表示される個々の通知を表す。
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public interface INotification {
    
    /**
     * 通知の一意ID を取得する。
     * 
     * @return 通知ID
     */
    String getId();
    
    /**
     * 通知のタイトルを取得する。
     * 
     * @return タイトル
     */
    String getTitle();
    
    /**
     * 通知の内容を取得する。
     * 
     * @return 内容
     */
    String getContent();
    
    /**
     * 通知の送信者/アプリ名を取得する。
     * 
     * @return 送信者名
     */
    String getSender();
    
    /**
     * 通知の作成時刻を取得する（ミリ秒）。
     * 
     * @return 作成時刻
     */
    long getTimestamp();
    
    /**
     * 通知が読まれたかどうかを取得する。
     * 
     * @return 読まれた場合true
     */
    boolean isRead();
    
    /**
     * 通知を読み状態に設定する。
     */
    void markAsRead();
    
    /**
     * 通知の優先度を取得する。
     * 
     * @return 優先度（0=低、1=通常、2=高）
     */
    int getPriority();
    
    /**
     * 通知を描画する（PApplet版）。
     *
     * @param p Processing描画コンテキスト
     * @param x 描画開始X座標
     * @param y 描画開始Y座標
     * @param width 描画幅
     * @param height 描画高さ
     * @deprecated PGraphics版のdrawメソッドを使用してください
     */
    @Deprecated
    void draw(PApplet p, float x, float y, float width, float height);

    /**
     * 通知を描画する（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     *
     * @param g Processing描画コンテキスト
     * @param x 描画開始X座標
     * @param y 描画開始Y座標
     * @param width 描画幅
     * @param height 描画高さ
     */
    void draw(PGraphics g, float x, float y, float width, float height);
    
    /**
     * 通知がクリックされた時の処理。
     * 
     * @param x クリック位置X座標
     * @param y クリック位置Y座標
     * @return 処理した場合true
     */
    boolean onClick(float x, float y);
    
    /**
     * 通知を削除可能かどうかを取得する。
     * 
     * @return 削除可能な場合true
     */
    boolean isDismissible();
    
    /**
     * 通知を削除する。
     */
    void dismiss();

    /**
     * 通知のアプリアイコンを取得する。
     *
     * @return アプリアイコン（nullの場合はデフォルトアイコンを使用）
     */
    default PImage getIcon() {
        return null;
    }

    /**
     * 通知クリック時のアクションを取得する。
     *
     * @return クリック時に実行するアクション（nullの場合は何もしない）
     */
    default Runnable getClickAction() {
        return null;
    }
}