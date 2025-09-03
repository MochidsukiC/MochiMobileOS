package jp.moyashi.phoneos.core.ui;

import processing.core.PApplet;

/**
 * 動的UIレイヤーを表現するクラス。
 * アプリケーションがレイヤーの描画許可をカーネルに要求し、
 * カーネルが自動的にレイヤーを管理する。
 * 
 * @author YourName
 * @version 1.0
 */
public class UILayer {
    
    /** レイヤーの一意識別子 */
    private final String layerId;
    
    /** レイヤーの名前（デバッグ用） */
    private final String layerName;
    
    /** レイヤーの描画処理を実行するインターフェース */
    private final LayerRenderer renderer;
    
    /** レイヤーの基本優先度 */
    private final int basePriority;
    
    /** レイヤーが現在表示されているかどうか */
    private boolean isVisible;
    
    /** レイヤーが有効かどうか（削除マーク用） */
    private boolean isActive;
    
    /** 最後に更新された時刻 */
    private long lastUpdateTime;
    
    /**
     * レイヤー描画処理を定義するインターフェース
     */
    public interface LayerRenderer {
        /**
         * レイヤーの描画処理を実行する
         * 
         * @param p 描画用PAppletインスタンス
         */
        void render(PApplet p);
        
        /**
         * レイヤーが表示されているかどうかを確認する
         * 
         * @return 表示中の場合true
         */
        boolean isVisible();
    }
    
    /**
     * UILayerを構築する。
     * 
     * @param layerId レイヤーの一意識別子
     * @param layerName レイヤーの名前
     * @param basePriority レイヤーの基本優先度（高いほど上位）
     * @param renderer レイヤー描画処理
     */
    public UILayer(String layerId, String layerName, int basePriority, LayerRenderer renderer) {
        this.layerId = layerId;
        this.layerName = layerName;
        this.basePriority = basePriority;
        this.renderer = renderer;
        this.isVisible = false;
        this.isActive = true;
        this.lastUpdateTime = System.currentTimeMillis();
    }
    
    /**
     * レイヤーを更新し、描画する。
     * 
     * @param p 描画用PAppletインスタンス
     */
    public void update(PApplet p) {
        if (!isActive) return;
        
        // 表示状態を更新
        boolean newVisibility = renderer.isVisible();
        if (newVisibility != isVisible) {
            isVisible = newVisibility;
            lastUpdateTime = System.currentTimeMillis();
            System.out.println("UILayer '" + layerName + "' visibility changed to: " + isVisible);
        }
        
        // 表示中の場合のみ描画
        if (isVisible) {
            try {
                renderer.render(p);
            } catch (Exception e) {
                System.err.println("UILayer '" + layerName + "' render error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 現在の動的優先度を取得する。
     * 表示中のレイヤーは基本優先度、非表示のレイヤーは大幅に低い優先度を返す。
     * 
     * @return 動的優先度
     */
    public int getCurrentPriority() {
        if (!isActive) return -1000; // 削除マークされたレイヤーは最低優先度
        if (isVisible) {
            return basePriority;
        } else {
            return Math.max(1, basePriority / 100); // 非表示時は大幅に優先度を下げる
        }
    }
    
    // Getters
    
    public String getLayerId() { return layerId; }
    public String getLayerName() { return layerName; }
    public int getBasePriority() { return basePriority; }
    public boolean isVisible() { return isVisible; }
    public boolean isActive() { return isActive; }
    public long getLastUpdateTime() { return lastUpdateTime; }
    
    /**
     * レイヤーを削除マークする。
     * 次回のクリーンアップ時に実際に削除される。
     */
    public void markForDeletion() {
        this.isActive = false;
        System.out.println("UILayer '" + layerName + "' marked for deletion");
    }
    
    @Override
    public String toString() {
        return String.format("UILayer{id='%s', name='%s', priority=%d, visible=%s, active=%s}", 
                           layerId, layerName, getCurrentPriority(), isVisible, isActive);
    }
}