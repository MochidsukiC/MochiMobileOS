package jp.moyashi.phoneos.core.ui;

import jp.moyashi.phoneos.core.input.GestureListener;
import jp.moyashi.phoneos.core.input.GestureManager;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 動的UIレイヤー管理システム。
 * アプリケーションからのレイヤー描画許可要求を受け付け、
 * レイヤーの登録・削除・描画・優先度管理を自動的に行う。
 * 
 * @author YourName
 * @version 1.0
 */
public class LayerManager {
    
    /** 登録されたレイヤーのマップ（ID -> Layer） */
    private final Map<String, UILayer> layers;
    
    /** 描画順序でソートされたレイヤーリスト（優先度降順） */
    private final List<UILayer> sortedLayers;
    
    /** ジェスチャーマネージャーへの参照 */
    private final GestureManager gestureManager;
    
    /** 最後にクリーンアップが実行された時刻 */
    private long lastCleanupTime;
    
    /** クリーンアップの実行間隔（ミリ秒） */
    private static final long CLEANUP_INTERVAL = 5000; // 5秒
    
    /**
     * LayerManagerを構築する。
     * 
     * @param gestureManager ジェスチャー優先度管理用のマネージャー
     */
    public LayerManager(GestureManager gestureManager) {
        this.layers = new ConcurrentHashMap<>();
        this.sortedLayers = new CopyOnWriteArrayList<>();
        this.gestureManager = gestureManager;
        this.lastCleanupTime = System.currentTimeMillis();
        
        System.out.println("LayerManager: Dynamic layer management system initialized");
    }
    
    /**
     * アプリケーションがレイヤー描画許可を要求する。
     * 許可された場合、レイヤーが自動的に登録される。
     * 
     * @param layerId レイヤーの一意識別子
     * @param layerName レイヤーの表示名
     * @param basePriority レイヤーの基本優先度（高いほど上位）
     * @param renderer レイヤー描画処理
     * @return 許可された場合true、拒否または既存の場合false
     */
    public synchronized boolean requestLayerPermission(String layerId, String layerName, 
                                                     int basePriority, UILayer.LayerRenderer renderer) {
        // 既存レイヤーの確認
        if (layers.containsKey(layerId)) {
            System.out.println("LayerManager: Layer '" + layerId + "' already exists - request denied");
            return false;
        }
        
        // レイヤー作成
        UILayer newLayer = new UILayer(layerId, layerName, basePriority, renderer);
        
        // レイヤー登録
        layers.put(layerId, newLayer);
        updateLayerOrder();
        
        System.out.println("LayerManager: Layer permission granted - " + newLayer);
        return true;
    }
    
    /**
     * レイヤーを削除する。
     * 
     * @param layerId 削除するレイヤーID
     * @return 削除された場合true
     */
    public synchronized boolean removeLayer(String layerId) {
        UILayer layer = layers.get(layerId);
        if (layer != null) {
            layer.markForDeletion();
            System.out.println("LayerManager: Layer '" + layerId + "' marked for removal");
            return true;
        }
        return false;
    }
    
    /**
     * すべてのレイヤーを更新し、描画する。
     *
     * @param g 描画用PGraphicsインスタンス
     */
    public void updateAndRender(PGraphics g) {
        // 定期的なクリーンアップ
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > CLEANUP_INTERVAL) {
            performCleanup();
            lastCleanupTime = currentTime;
        }
        
        // レイヤー順序更新（表示状態が変更された可能性があるため）
        updateLayerOrder();
        
        // レイヤー描画（優先度順：低い優先度から高い優先度へ）
        List<UILayer> renderList = new ArrayList<>(sortedLayers);
        renderList.sort(Comparator.comparingInt(UILayer::getCurrentPriority));
        
        for (UILayer layer : renderList) {
            if (layer.isActive()) {
                try {
                    // PGraphics対応のレイヤー更新を実行
                    layer.update(g);
                } catch (Exception e) {
                    System.err.println("LayerManager: Error updating layer '" + layer.getLayerId() + "': " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * ジェスチャーリスナーの優先度を現在のレイヤー状態に基づいて更新する。
     * 注意: このメソッドはレイヤー描画ループ外で呼び出すこと（循環参照回避のため）
     * 
     * @param listener 優先度を更新するジェスチャーリスナー
     * @param layerId 関連するレイヤーID
     * @return 更新された優先度
     */
    public int updateGesturePriority(GestureListener listener, String layerId) {
        UILayer layer = layers.get(layerId);
        if (layer != null) {
            int newPriority = layer.getCurrentPriority();
            
            // 循環参照を避けるため、ジェスチャーマネージャーの更新は後で行う
            // gestureManager.removeGestureListener(listener);
            // gestureManager.addGestureListener(listener);
            
            return newPriority;
        }
        return 0; // デフォルト優先度
    }
    
    /**
     * 指定されたレイヤーが表示中かどうかを確認する。
     * 
     * @param layerId レイヤーID
     * @return 表示中の場合true
     */
    public boolean isLayerVisible(String layerId) {
        UILayer layer = layers.get(layerId);
        return layer != null && layer.isVisible();
    }
    
    /**
     * レイヤー数を取得する。
     * 
     * @return 現在登録されているレイヤー数
     */
    public int getLayerCount() {
        return layers.size();
    }
    
    /**
     * レイヤー情報をデバッグ出力する。
     */
    public void printLayerStatus() {
        System.out.println("=== Layer Status ===");
        List<UILayer> debugList = new ArrayList<>(sortedLayers);
        debugList.sort(Comparator.comparingInt(UILayer::getCurrentPriority).reversed());
        
        for (UILayer layer : debugList) {
            System.out.println("  " + layer);
        }
        System.out.println("==================");
    }
    
    /**
     * レイヤーの描画順序を現在の優先度に基づいて更新する。
     */
    private synchronized void updateLayerOrder() {
        // アクティブなレイヤーのみをソート
        sortedLayers.clear();
        for (UILayer layer : layers.values()) {
            if (layer.isActive()) {
                sortedLayers.add(layer);
            }
        }
        
        // 優先度でソート（降順：高い優先度が先頭）
        sortedLayers.sort(Comparator.comparingInt(UILayer::getCurrentPriority).reversed());
    }
    
    /**
     * 削除マークされたレイヤーを実際に削除する。
     */
    private synchronized void performCleanup() {
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, UILayer> entry : layers.entrySet()) {
            if (!entry.getValue().isActive()) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (String layerId : toRemove) {
            UILayer removed = layers.remove(layerId);
            if (removed != null) {
                System.out.println("LayerManager: Layer '" + layerId + "' cleaned up and removed");
            }
        }
        
        if (!toRemove.isEmpty()) {
            updateLayerOrder();
        }
    }
    
    /**
     * 現在最上位のレイヤーを取得する。
     * 
     * @return 最上位レイヤー、レイヤーが存在しない場合null
     */
    public UILayer getTopLayer() {
        if (sortedLayers.isEmpty()) return null;
        
        return sortedLayers.stream()
                .filter(layer -> layer.isActive() && layer.isVisible())
                .max(Comparator.comparingInt(UILayer::getCurrentPriority))
                .orElse(null);
    }
}