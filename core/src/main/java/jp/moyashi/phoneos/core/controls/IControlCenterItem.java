package jp.moyashi.phoneos.core.controls;

import jp.moyashi.phoneos.core.input.GestureEvent;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * コントロールセンターに配置される全てのアイテム（トグル、スライダー等）が実装する基底インターフェース。
 * アプリケーションがコントロールセンターに機能を追加するためのAPI規格を定義する。
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public interface IControlCenterItem {
    
    /**
     * コントロールセンターアイテムを描画する（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     *
     * @param g Processing描画コンテキスト
     * @param x アイテム描画領域のX座標
     * @param y アイテム描画領域のY座標
     * @param w アイテム描画領域の幅
     * @param h アイテム描画領域の高さ
     */
    default void draw(PGraphics g, float x, float y, float w, float h) {
        // デフォルト実装：何もしない
        // 各実装クラスでオーバーライドしてください
    }

    /**
     * コントロールセンターアイテムを描画する（PApplet版）。
     * 互換性のために残存。段階的にPGraphics版に移行予定。
     *
     * @param p Processing描画コンテキスト
     * @param x アイテム描画領域のX座標
     * @param y アイテム描画領域のY座標
     * @param w アイテム描画領域の幅
     * @param h アイテム描画領域の高さ
     */
    void draw(PApplet p, float x, float y, float w, float h);
    
    /**
     * ジェスチャーイベントを処理する。
     * タップ、長押し、スワイプなどのユーザーインタラクションに対応する。
     * 
     * @param event ジェスチャーイベント
     * @return イベントを処理した場合true、処理しなかった場合false
     */
    boolean onGesture(GestureEvent event);
    
    /**
     * このアイテムの識別子を取得する。
     * コントロールセンター内での一意性を保証するために使用される。
     * 
     * @return アイテムの一意識別子
     */
    String getId();
    
    /**
     * このアイテムの表示名を取得する。
     * ユーザーに表示されるアイテムの名称。
     * 
     * @return アイテムの表示名
     */
    String getDisplayName();
    
    /**
     * このアイテムの説明を取得する。
     * アイテムの機能や状態に関する詳細情報。
     * 
     * @return アイテムの説明
     */
    String getDescription();
    
    /**
     * このアイテムが有効かどうかを確認する。
     * 無効なアイテムはグレーアウト表示され、操作できない。
     * 
     * @return 有効な場合true、無効な場合false
     */
    default boolean isEnabled() {
        return true;
    }
    
    /**
     * このアイテムが表示されるべきかどうかを確認する。
     * 非表示のアイテムはコントロールセンターに表示されない。
     * 
     * @return 表示する場合true、非表示の場合false
     */
    default boolean isVisible() {
        return true;
    }
    
    /**
     * 指定された座標がこのアイテムの操作可能領域内にあるかを確認する。
     * 
     * @param x X座標（アイテム描画領域内での相対座標）
     * @param y Y座標（アイテム描画領域内での相対座標）
     * @param itemX アイテム描画領域のX座標
     * @param itemY アイテム描画領域のY座標
     * @param itemW アイテム描画領域の幅
     * @param itemH アイテム描画領域の高さ
     * @return 操作可能領域内の場合true
     */
    default boolean isInBounds(float x, float y, float itemX, float itemY, float itemW, float itemH) {
        return x >= itemX && x <= itemX + itemW && y >= itemY && y <= itemY + itemH;
    }
}