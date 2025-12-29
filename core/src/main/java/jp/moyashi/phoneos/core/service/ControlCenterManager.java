package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.controls.ControlCenterCardRegistry;
import jp.moyashi.phoneos.core.controls.IControlCenterItem;
import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureListener;
import jp.moyashi.phoneos.core.coordinate.CoordinateTransform;
import jp.moyashi.phoneos.core.navigation.LayerController;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * コントロールセンターの状態（表示/非表示、アイテム一覧）を一元管理するサービス。
 * 画面下からスライドイン/アウトするアニメーション付きでコントロールセンターを表示し、
 * 登録されたアイテムの描画とイベント処理を担当する。
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class ControlCenterManager implements GestureListener {
    
    private static final boolean DEBUG_GESTURE_LOG = Boolean.getBoolean("mochi.debugGesture");
    
    /** コントロールセンターアイテムのリスト（スレッドセーフ） */
    private final List<IControlCenterItem> items;
    
    /** 現在の表示状態 */
    private boolean isVisible;
    
    /** アニメーション進行度（0.0 = 非表示, 1.0 = 完全表示） */
    private float animationProgress;
    
    /** アニメーションの目標進行度 */
    private float targetAnimationProgress;
    
    /** アニメーション速度 */
    private static final float ANIMATION_SPEED = 0.12f;
    
    /** コントロールセンターの高さ（画面の何割を占めるか） */
    private static final float CONTROL_CENTER_HEIGHT_RATIO = 1.0f;
    
    /** アイテムの高さ */
    private static final float ITEM_HEIGHT = 60;
    
    /** アイテム間のマージン */
    private static final float ITEM_MARGIN = 8;
    
    /** 画面の幅（描画時に取得） */
    private float screenWidth = 400;
    
    /** 画面の高さ（描画時に取得） */
    private float screenHeight = 600;

    /** パネルの幅 */
    private int panelWidth;

    /** パネルの高さ */
    private int panelHeight;

    /** 背景のアルファ値 */
    private static final int BACKGROUND_ALPHA = 220;
    
    /** スクロールオフセット（縦方向） */
    private float scrollOffset = 0.0f;
    
    /** スクロール可能な最大オフセット */
    private float maxScrollOffset = 0.0f;
    
    /** スクロールの慣性 */
    private float scrollVelocity = 0.0f;
    
    /** 摩擦係数（スクロールの減速） */
    private static final float FRICTION = 0.85f;
    
    /** スクロール感度 */
    private static final float SCROLL_SENSITIVITY = 1.5f;
    
    /** 最小スクロール速度（これより小さければ停止） */
    private static final float MIN_SCROLL_VELOCITY = 0.1f;
    
    /** 前回のドラッグY座標（スクロール計算用） */
    private float lastDragY = 0;
    
    /** ドラッグが開始されているかどうか */
    private boolean isDragScrolling = false;

    /** 現在ドラッグ操作中のアイテム */
    private IControlCenterItem dragTargetItem = null;
    
    /** 動的優先度（表示状態に応じて変更される） */
    private int dynamicPriority = 0;
    
    /** ジェスチャーマネージャーへの参照（優先度変更時の再ソート用） */
    private jp.moyashi.phoneos.core.input.GestureManager gestureManager;

    /** 統一座標変換システム */
    private CoordinateTransform coordinateTransform;

    /** カードレジストリ */
    private ControlCenterCardRegistry cardRegistry;

    /** Kernelへの参照（LayerController連携用） */
    private Kernel kernel;

    /**
     * ControlCenterManagerを作成する。
     */
    public ControlCenterManager() {
        this.items = new CopyOnWriteArrayList<>();
        this.isVisible = false;
        this.animationProgress = 0.0f;
        this.targetAnimationProgress = 0.0f;
        
        System.out.println("ControlCenterManager: Control center service initialized");
    }
    
    /**
     * Kernelへの参照を設定する。
     * LayerControllerとの連携に必要。
     *
     * @param kernel Kernelインスタンス
     */
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * コントロールセンターを表示する。
     */
    public void show() {
        if (!isVisible) {
            isVisible = true;
            targetAnimationProgress = 1.0f;

            // コントロールセンターが表示される時は最高優先度に設定
            setDynamicPriority(15000); // ロック画面(8000)より高い優先度

            // LayerControllerにCONTROL_CENTERレイヤーを追加
            if (kernel != null && kernel.getLayerController() != null) {
                kernel.getLayerController().activateControlCenterLayer();
            }

            System.out.println("ControlCenterManager: Showing control center with " + items.size() + " items");
            System.out.println("ControlCenterManager: Set priority to 15000 (highest)");
        }
    }
    
    /**
     * コントロールセンターを非表示にする。
     */
    public void hide() {
        if (isVisible) {
            isVisible = false;
            targetAnimationProgress = 0.0f;

            // コントロールセンターが非表示になる時は低い優先度に設定
            setDynamicPriority(0); // 低い優先度に戻す

            // LayerControllerからCONTROL_CENTERレイヤーを削除
            if (kernel != null && kernel.getLayerController() != null) {
                kernel.getLayerController().removeLayer(Kernel.LayerType.CONTROL_CENTER);
            }

            // スクロール状態をリセット（次回表示時に先頭から表示される）
            scrollOffset = 0.0f;
            scrollVelocity = 0.0f;
            isDragScrolling = false;
            lastDragY = 0;

            System.out.println("ControlCenterManager: Hiding control center");
            System.out.println("ControlCenterManager: Set priority to 0 (low)");
        }
    }
    
    /**
     * 表示状態を切り替える。
     */
    public void toggle() {
        if (isVisible) {
            hide();
        } else {
            show();
        }
    }
    
    /**
     * コントロールセンターアイテムを追加する。
     * 
     * @param item 追加するアイテム
     * @return 追加に成功した場合true
     */
    public boolean addItem(IControlCenterItem item) {
        if (item == null) {
            System.err.println("ControlCenterManager: Cannot add null item");
            return false;
        }
        
        // 重複IDチェック
        for (IControlCenterItem existingItem : items) {
            if (existingItem.getId().equals(item.getId())) {
                System.err.println("ControlCenterManager: Item with ID '" + item.getId() + "' already exists");
                return false;
            }
        }
        
        items.add(item);
        System.out.println("ControlCenterManager: Added item '" + item.getDisplayName() + "' (ID: " + item.getId() + ")");
        return true;
    }
    
    /**
     * コントロールセンターアイテムを削除する。
     * 
     * @param itemId 削除するアイテムのID
     * @return 削除に成功した場合true
     */
    public boolean removeItem(String itemId) {
        return items.removeIf(item -> {
            if (item.getId().equals(itemId)) {
                System.out.println("ControlCenterManager: Removed item '" + item.getDisplayName() + "' (ID: " + itemId + ")");
                return true;
            }
            return false;
        });
    }
    
    /**
     * 指定されたIDのアイテムを取得する。
     * 
     * @param itemId アイテムID
     * @return 見つかった場合はアイテム、見つからない場合はnull
     */
    public IControlCenterItem getItem(String itemId) {
        return items.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * すべてのアイテムを削除する。
     */
    public void clearItems() {
        int count = items.size();
        items.clear();
        System.out.println("ControlCenterManager: Cleared " + count + " items");
    }
    
    /**
     * 登録されているアイテムの数を取得する。
     * 
     * @return アイテム数
     */
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * コントロールセンターが表示中かどうかを確認する。
     * 
     * @return 表示中の場合true
     */
    public boolean isVisible() {
        return this.isVisible;
    }
    
    /**
     * コントロールセンターを描画する（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     *
     * @param g Processing描画コンテキスト
     */
    public void draw(PGraphics g) {
        // 画面サイズを更新（coordinateTransformから取得、なければフォールバック）
        if (coordinateTransform != null) {
            screenWidth = coordinateTransform.getScreenWidth();
            screenHeight = coordinateTransform.getScreenHeight();
        } else {
            screenWidth = 400;
            screenHeight = 600;
        }

        // アニメーション進行度を更新
        updateAnimation();

        // 完全に非表示の場合は描画をスキップ
        if (animationProgress <= 0.01f) {
            return;
        }

        // バックアップ設定
        int originalTextAlign = g.textAlign;
        float originalTextSize = g.textSize;

        // 統一座標変換システムを使用してパネル座標を計算
        CoordinateTransform.PanelCoordinates panelCoords = null;
        if (coordinateTransform != null) {
            panelCoords = coordinateTransform.calculateAnimatedPanel(CONTROL_CENTER_HEIGHT_RATIO, animationProgress);
            System.out.println("🔧 ControlCenter: Using unified coordinate system - " + panelCoords.toString());
        }

        // パネルの寸法と位置を設定（統一座標系がない場合は従来の計算）
        panelWidth = (int)screenWidth;
        if (panelCoords != null) {
            panelHeight = (int)panelCoords.panelHeight;
        } else {
            panelHeight = (int)(screenHeight * 0.6f);
        }

        // Y座標を計算（統一座標系を優先、なければ従来の計算）
        int panelY;
        if (panelCoords != null) {
            panelY = (int)panelCoords.panelY;
        } else {
            float animatedY = screenHeight - (panelHeight * animationProgress);
            panelY = (int)animatedY;
        }

        // テーマに基づくパネル背景描画 (ビジュアル改善)
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        float tl = theme != null ? theme.radiusLg() : 20; // radiusXlがないためLgを使用、値は大きめに

        // 背景スクラム（暗幕）
        g.fill(0, 0, 0, 100);
        g.noStroke();
        g.rect(0, 0, (int)screenWidth, (int)screenHeight);

        // パネル本体（フロストガラス風演出）
        // 1. 影（エレベーション）
        jp.moyashi.phoneos.core.ui.effects.Elevation.drawRectShadow(g, 0, panelY, panelWidth, panelHeight, tl, 16);
        
        // 2. ベースレイヤー（濃い背景）
        int surface = 0xFF1C1C1E; // モダンなダークグレー
        int r = (surface>>16)&0xFF, gr = (surface>>8)&0xFF, b = surface&0xFF;
        g.fill(r, gr, b, 240); // 高い不透明度
        g.noStroke();
        g.rect(0, panelY, panelWidth, panelHeight, tl, tl, 0, 0);

        // 3. 質感向上用オーバーレイ（ノイズやグラデーションがあれば尚良いが、シンプルに）
        g.fill(255, 255, 255, 5); // わずかなハイライト
        g.rect(0, panelY, panelWidth, panelHeight, tl, tl, 0, 0);

        // 上端ボーダーで分離
        int borderCol = theme != null ? theme.colorBorder() : 0xFF444444;
        g.stroke((borderCol>>16)&0xFF, (borderCol>>8)&0xFF, borderCol&0xFF);
        g.strokeWeight(1);
        g.line(0, panelY, panelWidth, panelY);

        // --- レイアウト変更：タイトルを一番上に配置 ---

        // タイトル領域とレイアウト定数
        final int PADDING = 20; // 余白を広げる
        final int GAP = 14;     // ギャップを広げる
        int titleY = panelY + PADDING;
        int textCol = theme != null ? theme.colorOnSurface() : 0xFFFFFFFF;
        g.fill((textCol>>16)&0xFF, (textCol>>8)&0xFF, textCol&0xFF);
        g.textAlign(PApplet.CENTER, PApplet.TOP);
        g.textSize(18);
        g.text("コントロールセンター", screenWidth / 2, titleY);

        // ハンドル描画 (タイトルの下に配置)
        int handleY = titleY + 30; // 位置調整
        int handleWidth = 60;
        int handleHeight = 5;
        int handleX = (int)(screenWidth - handleWidth) / 2;
        int handle = theme != null ? theme.colorBorder() : 0xFFC8CDD7;
        g.fill((handle>>16)&0xFF, (handle>>8)&0xFF, handle&0xFF);
        g.rect(handleX, handleY, handleWidth, handleHeight, 2.5f);

        // アイテムグリッド描画 (ハンドルの下に配置)
        int startY = handleY + 25; // 位置を調整
        int cols = 4; // 4カラムに変更して密度を上げる
        int cellWidth = (panelWidth - (PADDING * 2) - (GAP * (cols - 1))) / cols;
        int cellHeight = cellWidth; // 正方形グリッドにする（美しさの向上）

        // グリッド占有状況を追跡（行数は動的に拡張）
        boolean[][] gridOccupied = new boolean[20][cols];

        // 右寄せアイテムを先に配置するためソート
        java.util.List<IControlCenterItem> sortedItems = new java.util.ArrayList<>(items);
        sortedItems.sort((item1, item2) -> {
            int align1 = item1.getGridAlignment() == IControlCenterItem.GridAlignment.RIGHT ? 0 : 1;
            int align2 = item2.getGridAlignment() == IControlCenterItem.GridAlignment.RIGHT ? 0 : 1;
            return align1 - align2;
        });

        for (IControlCenterItem item : sortedItems) {
            if (!item.isVisible()) continue;

            int colSpan = Math.min(item.getColumnSpan(), cols);
            int rowSpan = item.getRowSpan();
            IControlCenterItem.GridAlignment alignment = item.getGridAlignment();

            // 空いているセルを探す（RIGHT指定は右から探す）
            int placeCol = -1, placeRow = -1;
            outer:
            for (int row = 0; row < gridOccupied.length; row++) {
                if (alignment == IControlCenterItem.GridAlignment.RIGHT) {
                    // 右から探す
                    for (int col = cols - colSpan; col >= 0; col--) {
                        boolean canPlace = true;
                        for (int dr = 0; dr < rowSpan && canPlace; dr++) {
                            for (int dc = 0; dc < colSpan && canPlace; dc++) {
                                if (row + dr >= gridOccupied.length || gridOccupied[row + dr][col + dc]) {
                                    canPlace = false;
                                }
                            }
                        }
                        if (canPlace) {
                            placeCol = col;
                            placeRow = row;
                            break outer;
                        }
                    }
                } else {
                    // 左から探す
                    for (int col = 0; col <= cols - colSpan; col++) {
                        boolean canPlace = true;
                        for (int dr = 0; dr < rowSpan && canPlace; dr++) {
                            for (int dc = 0; dc < colSpan && canPlace; dc++) {
                                if (row + dr >= gridOccupied.length || gridOccupied[row + dr][col + dc]) {
                                    canPlace = false;
                                }
                            }
                        }
                        if (canPlace) {
                            placeCol = col;
                            placeRow = row;
                            break outer;
                        }
                    }
                }
            }

            if (placeCol < 0) continue; // 配置できない

            // グリッドを占有
            for (int dr = 0; dr < rowSpan; dr++) {
                for (int dc = 0; dc < colSpan; dc++) {
                    if (placeRow + dr < gridOccupied.length) {
                        gridOccupied[placeRow + dr][placeCol + dc] = true;
                    }
                }
            }

            // 座標計算
            int itemX = PADDING + placeCol * (cellWidth + GAP);
            int itemY = startY + placeRow * (cellHeight + GAP);
            int itemW = cellWidth * colSpan + GAP * (colSpan - 1);
            int itemH = cellHeight * rowSpan + GAP * (rowSpan - 1);

            if (itemY + itemH > panelY + panelHeight - 20) continue;

            // 背景スロット（カード）
            jp.moyashi.phoneos.core.ui.effects.Elevation.drawRectShadow(g, itemX, itemY, itemW, itemH, 10, 1);
            g.fill((surface>>16)&0xFF, (surface>>8)&0xFF, surface&0xFF, 245);
            g.stroke((borderCol>>16)&0xFF, (borderCol>>8)&0xFF, borderCol&0xFF);
            g.strokeWeight(1);
            g.rect(itemX, itemY, itemW, itemH, 10);

            // アイテム描画
            item.draw(g, itemX, itemY, itemW, itemH);
        }

        // 設定復元
        g.textAlign(originalTextAlign, PApplet.BASELINE);
        g.textSize(originalTextSize);
    }

    /**
     * コントロールセンターを描画する（PApplet版）。
     * 互換性のために残存。段階的にPGraphics版に移行予定。
     *
     * @param p Processing描画コンテキスト
     */
    public void draw(PApplet p) {
        // 画面サイズを更新
        screenWidth = p.width;
        screenHeight = p.height;
        
        // アニメーション進行度を更新
        updateAnimation();
        
        // スクロール慣性を更新
        updateScrollPhysics();
        
        // デバッグ情報出力（最初の数フレームのみ）
        if (isVisible && p.frameCount % 60 == 0) {
            System.out.println("ControlCenter: Drawing - visible=" + isVisible + ", progress=" + animationProgress + ", items=" + items.size());
        }
        
        // 完全に非表示の場合は描画をスキップ
        if (animationProgress <= 0.01f) {
            return;
        }
        
        p.pushMatrix();
        p.pushStyle();
        
        try {
            // 背景オーバーレイ描画
            drawBackgroundOverlay(p);
            
            // コントロールセンターパネル描画
            drawControlPanel(p);
            
        } finally {
            p.popStyle();
            p.popMatrix();
        }
    }
    
    /**
     * 背景オーバーレイを描画する（画面全体を暗くする効果）。
     */
    private void drawBackgroundOverlay(PApplet p) {
        int alpha = (int) (100 * animationProgress);
        p.fill(0, 0, 0, alpha);
        p.noStroke();
        p.rect(0, 0, screenWidth, screenHeight);
    }
    
    /**
     * コントロールセンターパネルを描画する。
     */
    private void drawControlPanel(PApplet p) {
        float panelHeight = screenHeight * CONTROL_CENTER_HEIGHT_RATIO;
        float panelY = screenHeight - panelHeight * animationProgress;

        p.pushMatrix();
        p.pushStyle();

        try {
            // パネル背景 (ビジュアル改善)
            int backgroundAlpha = (int) (240 * animationProgress);
            p.fill(40, 45, 55, backgroundAlpha); // モダンなダークブルーグレーに戻す
            p.noStroke();
            p.rect(0, panelY, screenWidth, panelHeight, 20, 20, 0, 0);

            // --- レイアウト変更：タイトルを一番上に配置 ---
            drawHeader(p, panelY); // 1. ヘッダー（タイトル）
            drawHandle(p, panelY); // 2. ハンドル
            drawItemsWithClipping(p, panelY + 80, panelHeight - 90); // 3. アイテム

        } finally {
            p.popStyle();
            p.popMatrix();
        }
    }
    
    /**
     * パネル上部の取っ手を描画する。
     */
    private void drawHandle(PApplet p, float panelY) {
        // ビジュアル改善 (タイトルの下に配置)
        float handleWidth = 60;
        float handleHeight = 5;
        float handleX = (screenWidth - handleWidth) / 2;
        float handleY = panelY + 50; // 位置調整

        int handleAlpha = (int) (200 * animationProgress);
        p.fill(200, 205, 215, handleAlpha);
        p.noStroke();
        p.rect(handleX, handleY, handleWidth, handleHeight, 2.5f);
    }
    
    /**
     * パネルヘッダーテキストを描画する。
     */
    private void drawHeader(PApplet p, float panelY) {
        int textAlpha = (int) (255 * animationProgress);
        p.fill(255, 255, 255, textAlpha);
        p.textAlign(p.CENTER, p.TOP);
        p.textSize(16);
        p.text("コントロールセンター", screenWidth / 2, panelY + 20); // 位置を調整
        
        // 使い方のヒントは削除し、シンプルにする
    }
    
    /**
     * コントロールセンターアイテムをクリッピング付きで描画する。
     */
    private void drawItemsWithClipping(PApplet p, float startY, float availableHeight) {
        if (items.isEmpty()) {
            // アイテムがない場合のメッセージ
            drawEmptyMessage(p, startY, availableHeight);
            return;
        }
        
        // スクロール可能な全体の高さを計算
        float totalContentHeight = 0;
        int visibleItemCount = 0;
        for (IControlCenterItem item : items) {
            if (item.isVisible()) {
                visibleItemCount++;
            }
        }
        
        if (visibleItemCount > 0) {
            totalContentHeight = visibleItemCount * (ITEM_HEIGHT + ITEM_MARGIN) + ITEM_MARGIN;
        }
        
        // 最大スクロールオフセットを更新
        maxScrollOffset = Math.max(0, totalContentHeight - availableHeight);
        
        // スクロールオフセットを制限
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
        
        // クリッピング領域を設定（下のレイヤーに影響しないように）
        // Processing の clip() 機能を使用して描画領域を制限
        p.pushMatrix();
        p.pushStyle();
        
        try {
            // アイテム描画領域をクリップ（範囲外への描画を防止）
            // ProcessingではclipRect()は使えないため、座標チェックで代替
            
            // 描画開始位置をスクロールオフセットに応じて調整
            float currentY = startY + ITEM_MARGIN - scrollOffset;
            int itemIndex = 0;
            
            for (IControlCenterItem item : items) {
                if (!item.isVisible()) {
                    continue;
                }
                
                // 表示領域内かどうかを厳密にチェック（下のレイヤーに影響しないように）
                if (currentY + ITEM_HEIGHT >= startY && currentY < startY + availableHeight) {
                    // アイテム描画
                    float itemX = ITEM_MARGIN;
                    float itemWidth = screenWidth - 2 * ITEM_MARGIN;
                    
                    // アイテムが表示領域内にある場合のみ描画
                    if (currentY >= startY - ITEM_HEIGHT && currentY <= startY + availableHeight + ITEM_HEIGHT) {
                        try {
                            // 下のレイヤーに影響しないよう、描画領域をさらに制限
                            p.pushStyle();
                            
                            // アイテムの描画位置が適切な範囲内にある場合のみ描画
                            if (currentY >= startY && currentY + ITEM_HEIGHT <= startY + availableHeight) {
                                item.draw(p, itemX, currentY, itemWidth, ITEM_HEIGHT);
                            } else if (currentY < startY && currentY + ITEM_HEIGHT > startY) {
                                // 上端で部分的に切れている場合
                                float visibleHeight = currentY + ITEM_HEIGHT - startY;
                                if (visibleHeight > 0) {
                                    item.draw(p, itemX, currentY, itemWidth, ITEM_HEIGHT);
                                }
                            } else if (currentY < startY + availableHeight && currentY + ITEM_HEIGHT > startY + availableHeight) {
                                // 下端で部分的に切れている場合
                                float visibleHeight = startY + availableHeight - currentY;
                                if (visibleHeight > 0) {
                                    item.draw(p, itemX, currentY, itemWidth, ITEM_HEIGHT);
                                }
                            }
                            
                            p.popStyle();
                        } catch (Exception e) {
                            System.err.println("ControlCenterManager: Error drawing item '" + item.getId() + "': " + e.getMessage());
                            
                            // エラー時のフォールバック描画（表示領域内のみ）
                            if (currentY >= startY && currentY + ITEM_HEIGHT <= startY + availableHeight) {
                                drawErrorItem(p, itemX, currentY, itemWidth, ITEM_HEIGHT, item.getDisplayName());
                            }
                        }
                    }
                }
                
                currentY += ITEM_HEIGHT + ITEM_MARGIN;
                itemIndex++;
            }
            
            // スクロールバーを描画（スクロール可能な場合のみ）
            if (maxScrollOffset > 0) {
                drawScrollbar(p, startY, availableHeight);
            }
            
        } finally {
            p.popStyle();
            p.popMatrix();
        }
    }
    
    /**
     * アイテムがない場合のメッセージを描画する。
     */
    private void drawEmptyMessage(PApplet p, float startY, float availableHeight) {
        p.fill(150, 150, 150, (int) (255 * animationProgress));
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(16);
        p.text("コントロールセンターにアイテムがありません", screenWidth / 2, startY + availableHeight / 2);
    }
    
    /**
     * エラー時のフォールバック描画を行う。
     */
    private void drawErrorItem(PApplet p, float x, float y, float w, float h, String itemName) {
        p.fill(100, 50, 50, 150);
        p.noStroke();
        p.rect(x, y, w, h, 8);
        
        p.fill(255, 100, 100);
        p.textAlign(PApplet.LEFT, PApplet.CENTER);
        p.textSize(12);
        p.text("Error: " + itemName, x + 10, y + h / 2);
    }
    
    /**
     * スクロールバーを描画する。
     */
    private void drawScrollbar(PApplet p, float startY, float availableHeight) {
        if (maxScrollOffset <= 0) {
            return;
        }
        
        // スクロールバーの位置とサイズを計算
        float scrollbarWidth = 4;
        float scrollbarX = screenWidth - scrollbarWidth - 2;
        
        float scrollbarHeight = (availableHeight / (maxScrollOffset + availableHeight)) * availableHeight;
        scrollbarHeight = Math.max(20, scrollbarHeight); // 最小サイズを設定
        
        float scrollbarY = startY + (scrollOffset / maxScrollOffset) * (availableHeight - scrollbarHeight);
        
        // スクロールバーを描画
        int scrollbarAlpha = (int) (100 * animationProgress);
        p.fill(255, 255, 255, scrollbarAlpha);
        p.noStroke();
        p.rect(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, scrollbarWidth / 2);
    }
    
    /**
     * スクロールの慣性物理を更新する。
     */
    private void updateScrollPhysics() {
        // 慣性によるスクロール
        if (Math.abs(scrollVelocity) > MIN_SCROLL_VELOCITY) {
            scrollOffset += scrollVelocity;
            scrollVelocity *= FRICTION;
        } else {
            scrollVelocity = 0;
        }
        
        // スクロール範囲を制限
        if (scrollOffset < 0) {
            scrollOffset = 0;
            scrollVelocity = 0;
        } else if (scrollOffset > maxScrollOffset) {
            scrollOffset = maxScrollOffset;
            scrollVelocity = 0;
        }
    }
    
    /**
     * アニメーション進行度を更新する。
     */
    private void updateAnimation() {
        if (Math.abs(animationProgress - targetAnimationProgress) > 0.01f) {
            float speed = ANIMATION_SPEED;
            // Reduce Motion対応: 進行を早める
            speed = (coordinateTransform != null) ? speed : speed; // keep
            // SettingsManagerから読み取り
            // Kernel参照は持っていないため省略。NotificationManager側と同様に将来拡張。
            animationProgress += (targetAnimationProgress - animationProgress) * speed;
        } else {
            animationProgress = targetAnimationProgress;
        }
    }
    
    
    /**
     * 現在のアニメーション進行度を取得する（デバッグ用）。
     * 
     * @return アニメーション進行度（0.0-1.0）
     */
    public float getAnimationProgress() {
        return animationProgress;
    }

    private void debugGesture(String message) {
        if (DEBUG_GESTURE_LOG) {
            System.out.println("ControlCenterManager: " + message);
        }
    }
    
    /**
     * アイテム一覧のコピーを取得する。
     * 
     * @return アイテム一覧（読み取り専用）
     */
    public List<IControlCenterItem> getItems() {
        return new ArrayList<>(items);
    }
    
    // GestureListener interface implementation
    
    /**
     * ジェスチャーの優先度を返す。
     * 動的に設定された優先度を返す。
     * 
     * @return 動的優先度
     */
    @Override
    public int getPriority() {
        return dynamicPriority;
    }
    
    /**
     * 動的優先度を設定する。
     *
     * @param priority 設定する優先度
     */
    public void setDynamicPriority(int priority) {
        int oldPriority = this.dynamicPriority;
        this.dynamicPriority = priority;

        // 優先度が変更された場合、ジェスチャーマネージャーにリスナーの再ソートを要求
        if (oldPriority != priority && gestureManager != null) {
            gestureManager.resortListeners();
            System.out.println("ControlCenterManager: Priority changed from " + oldPriority + " to " + priority + ", triggered re-sort");
        }
    }
    
    /**
     * ジェスチャーマネージャーの参照を設定する。
     * 
     * @param gestureManager ジェスチャーマネージャー
     */
    public void setGestureManager(jp.moyashi.phoneos.core.input.GestureManager gestureManager) {
        this.gestureManager = gestureManager;
    }

    /**
     * 統一座標変換システムを設定する。
     *
     * @param coordinateTransform 統一座標変換システム
     */
    public void setCoordinateTransform(CoordinateTransform coordinateTransform) {
        this.coordinateTransform = coordinateTransform;
    }

    /**
     * カードレジストリを設定する。
     * レジストリが設定されると、カードの追加/削除がレジストリ経由で管理される。
     *
     * @param registry カードレジストリ
     */
    public void setCardRegistry(ControlCenterCardRegistry registry) {
        this.cardRegistry = registry;

        if (registry != null) {
            // レジストリからカードを同期
            registry.addListener(new ControlCenterCardRegistry.CardRegistryListener() {
                @Override
                public void onCardAdded(IControlCenterItem card) {
                    // 重複チェックして追加
                    if (items.stream().noneMatch(i -> i.getId().equals(card.getId()))) {
                        items.add(card);
                        System.out.println("ControlCenterManager: Card synced from registry: " + card.getId());
                    }
                }

                @Override
                public void onCardRemoved(IControlCenterItem card) {
                    items.removeIf(i -> i.getId().equals(card.getId()));
                    System.out.println("ControlCenterManager: Card removed via registry: " + card.getId());
                }

                @Override
                public void onPlacementChanged(String cardId) {
                    // 配置変更時は再描画が必要（自動的に次フレームで反映される）
                }
            });

            System.out.println("ControlCenterManager: CardRegistry connected");
        }
    }

    /**
     * カードレジストリを取得する。
     *
     * @return カードレジストリ、設定されていない場合はnull
     */
    public ControlCenterCardRegistry getCardRegistry() {
        return cardRegistry;
    }

    /**
     * ジェスチャーイベントを処理する。
     * コントロールセンターが表示中の場合、すべてのジェスチャーを受け取り、
     * 適切なアクション（項目選択、スクロール、非表示）を実行する。
     * 
     * @param event ジェスチャーイベント
     * @return イベントを処理した場合true
     */
    @Override
    public boolean onGesture(GestureEvent event) {
        // コントロールセンターが表示されていない場合は処理しない
        if (!isVisible || animationProgress <= 0.1f) {
            return false;
        }
        
        debugGesture("Processing gesture - " + event.getType() + " at (" +
                event.getCurrentX() + ", " + event.getCurrentY() + ")");
        
        switch (event.getType()) {
            case SWIPE_DOWN:
                hide();
                return true;
                
            case TAP:
                handleControlCenterClick(event.getCurrentX(), event.getCurrentY());
                return true;
                
            case DRAG_START:
                // アイテム上でのドラッグ開始か判定
                IControlCenterItem item = findItemAt(event.getCurrentX(), event.getCurrentY());
                if (item != null && item.isDraggable()) {
                    dragTargetItem = item;
                    item.onGesture(event);
                    debugGesture("Started dragging item: " + item.getId());
                } else {
                    // スクロール開始
                    isDragScrolling = true;
                    lastDragY = event.getCurrentY();
                    scrollVelocity = 0;
                    debugGesture("Started scrolling panel");
                }
                return true;
                
            case DRAG_MOVE:
                if (dragTargetItem != null) {
                    dragTargetItem.onGesture(event);
                } else if (isDragScrolling) {
                    handleControlCenterScroll(event);
                }
                return true;
                
            case DRAG_END:
                if (dragTargetItem != null) {
                    dragTargetItem.onGesture(event);
                    dragTargetItem = null;
                    debugGesture("Ended dragging item");
                }
                isDragScrolling = false;
                debugGesture("Drag ended, resetting state");
                return true;
                
            case SWIPE_UP:
            case SWIPE_LEFT:
            case SWIPE_RIGHT:
                return true;
                
            default:
                return true;
        }
    }

    /**
     * 指定座標にあるアイテムを特定する。
     */
    private IControlCenterItem findItemAt(int x, int y) {
        // 統一座標変換システムを使用してパネル座標を計算
        CoordinateTransform.PanelCoordinates panelCoords = null;
        float panelHeight, panelY;

        if (coordinateTransform != null) {
            panelCoords = coordinateTransform.calculateAnimatedPanel(CONTROL_CENTER_HEIGHT_RATIO, animationProgress);
            panelHeight = panelCoords.panelHeight;
            panelY = panelCoords.panelY;
        } else {
            panelHeight = screenHeight * CONTROL_CENTER_HEIGHT_RATIO;
            panelY = screenHeight - panelHeight * animationProgress;
        }

        int panelWidthInt = (int) screenWidth;
        int PADDING = 16;
        int GAP = 12;
        int titleY = (int) (panelY + PADDING);
        int handleY = titleY + 30;
        int startY = handleY + 25;
        int cols = 4; // 4カラム
        int cellWidth = (panelWidthInt - (PADDING * 2) - (GAP * (cols - 1))) / cols;
        int cellHeight = cellWidth; // 正方形

        // グリッド占有状況を追跡
        boolean[][] gridOccupied = new boolean[20][cols];

        // 右寄せアイテムを先に配置するためソート
        java.util.List<IControlCenterItem> sortedItems = new java.util.ArrayList<>(items);
        sortedItems.sort((item1, item2) -> {
            int align1 = item1.getGridAlignment() == IControlCenterItem.GridAlignment.RIGHT ? 0 : 1;
            int align2 = item2.getGridAlignment() == IControlCenterItem.GridAlignment.RIGHT ? 0 : 1;
            return align1 - align2;
        });

        for (IControlCenterItem item : sortedItems) {
            if (!item.isVisible()) continue;

            int colSpan = Math.min(item.getColumnSpan(), cols);
            int rowSpan = item.getRowSpan();
            IControlCenterItem.GridAlignment alignment = item.getGridAlignment();

            int placeCol = -1, placeRow = -1;
            outer:
            for (int row = 0; row < gridOccupied.length; row++) {
                if (alignment == IControlCenterItem.GridAlignment.RIGHT) {
                    for (int col = cols - colSpan; col >= 0; col--) {
                        boolean canPlace = true;
                        for (int dr = 0; dr < rowSpan && canPlace; dr++) {
                            for (int dc = 0; dc < colSpan && canPlace; dc++) {
                                if (row + dr >= gridOccupied.length || gridOccupied[row + dr][col + dc]) {
                                    canPlace = false;
                                }
                            }
                        }
                        if (canPlace) {
                            placeCol = col;
                            placeRow = row;
                            break outer;
                        }
                    }
                } else {
                    for (int col = 0; col <= cols - colSpan; col++) {
                        boolean canPlace = true;
                        for (int dr = 0; dr < rowSpan && canPlace; dr++) {
                            for (int dc = 0; dc < colSpan && canPlace; dc++) {
                                if (row + dr >= gridOccupied.length || gridOccupied[row + dr][col + dc]) {
                                    canPlace = false;
                                }
                            }
                        }
                        if (canPlace) {
                            placeCol = col;
                            placeRow = row;
                            break outer;
                        }
                    }
                }
            }

            if (placeCol < 0) continue;

            for (int dr = 0; dr < rowSpan; dr++) {
                for (int dc = 0; dc < colSpan; dc++) {
                    if (placeRow + dr < gridOccupied.length) {
                        gridOccupied[placeRow + dr][placeCol + dc] = true;
                    }
                }
            }

            int itemX = PADDING + placeCol * (cellWidth + GAP);
            int itemY = startY + placeRow * (cellHeight + GAP);
            int itemW = cellWidth * colSpan + GAP * (colSpan - 1);
            int itemH = cellHeight * rowSpan + GAP * (rowSpan - 1);

            // スクロールオフセットを考慮（描画時はスクロールされていないが、
            // onGestureのイベント座標はスクリーン絶対座標。
            // しかし、drawItemsWithClippingの実装を見ると、アイテム自体はスクロールオフセット分ずれて描画されるはずだが、
            // handleControlCenterClickやdraw(PGraphics)の実装にはスクロール計算が含まれていない！
            // これはバグの可能性があるが、draw(PGraphics)がスクロールに対応していないように見える。
            // いったん既存のPGraphics描画ロジックに合わせる。）
            
            // FIXME: PGraphics版の描画ロジックにスクロールが含まれていないため、
            // ここでもスクロールは考慮しない。将来的にPGraphics版にスクロールを実装する際に修正が必要。

            if (x >= itemX && x <= itemX + itemW && y >= itemY && y <= itemY + itemH) {
                return item;
            }
        }
        return null;
    }
    
    /**
     * コントロールセンター内のクリックを処理する。
     *
     * @param x クリック座標X
     * @param y クリック座標Y
     */
    private void handleControlCenterClick(int x, int y) {
        // 統一座標変換システムを使用してパネル座標を計算
        CoordinateTransform.PanelCoordinates panelCoords = null;
        float panelHeight, panelY;

        if (coordinateTransform != null) {
            panelCoords = coordinateTransform.calculateAnimatedPanel(CONTROL_CENTER_HEIGHT_RATIO, animationProgress);
            panelHeight = panelCoords.panelHeight;
            panelY = panelCoords.panelY;
            System.out.println("🔧 Click: Using unified coordinate system - " + panelCoords.toString());
        } else {
            // フォールバック：従来の計算
            panelHeight = screenHeight * CONTROL_CENTER_HEIGHT_RATIO;
            panelY = screenHeight - panelHeight * animationProgress;
            System.out.println("⚠️ Click: Using fallback coordinate calculation");
        }

        System.out.println("🖱️ ControlCenterManager: Click at (" + x + ", " + y + ") in panel area (panelY=" + panelY + ")");

        // 【重要】PGraphics版の描画ロジックに合わせた3列グリッドレイアウトでのクリック判定
        // draw(PGraphics g)の座標計算と完全に一致させる
        int panelWidthInt = (int) screenWidth;
        int PADDING = 16;
        int GAP = 12;
        int titleY = (int) (panelY + PADDING);
        int handleY = titleY + 30;
        int startY = handleY + 25;
        int cols = 4; // 4カラム
        int cellWidth = (panelWidthInt - (PADDING * 2) - (GAP * (cols - 1))) / cols;
        int cellHeight = cellWidth; // 正方形

        System.out.println("🔧 Grid layout: panelWidth=" + panelWidthInt + ", cols=" + cols + ", cellWidth=" + cellWidth + ", cellHeight=" + cellHeight);

        // グリッド占有状況を追跡（描画と同じロジック）
        boolean[][] gridOccupied = new boolean[20][cols];

        // 右寄せアイテムを先に配置するためソート（描画と同じ）
        java.util.List<IControlCenterItem> sortedItems = new java.util.ArrayList<>(items);
        sortedItems.sort((item1, item2) -> {
            int align1 = item1.getGridAlignment() == IControlCenterItem.GridAlignment.RIGHT ? 0 : 1;
            int align2 = item2.getGridAlignment() == IControlCenterItem.GridAlignment.RIGHT ? 0 : 1;
            return align1 - align2;
        });

        for (IControlCenterItem item : sortedItems) {
            if (!item.isVisible()) {
                continue;
            }

            int colSpan = Math.min(item.getColumnSpan(), cols);
            int rowSpan = item.getRowSpan();
            IControlCenterItem.GridAlignment alignment = item.getGridAlignment();

            // 空いているセルを探す（RIGHT指定は右から探す）
            int placeCol = -1, placeRow = -1;
            outer:
            for (int row = 0; row < gridOccupied.length; row++) {
                if (alignment == IControlCenterItem.GridAlignment.RIGHT) {
                    for (int col = cols - colSpan; col >= 0; col--) {
                        boolean canPlace = true;
                        for (int dr = 0; dr < rowSpan && canPlace; dr++) {
                            for (int dc = 0; dc < colSpan && canPlace; dc++) {
                                if (row + dr >= gridOccupied.length || gridOccupied[row + dr][col + dc]) {
                                    canPlace = false;
                                }
                            }
                        }
                        if (canPlace) {
                            placeCol = col;
                            placeRow = row;
                            break outer;
                        }
                    }
                } else {
                    for (int col = 0; col <= cols - colSpan; col++) {
                        boolean canPlace = true;
                        for (int dr = 0; dr < rowSpan && canPlace; dr++) {
                            for (int dc = 0; dc < colSpan && canPlace; dc++) {
                                if (row + dr >= gridOccupied.length || gridOccupied[row + dr][col + dc]) {
                                    canPlace = false;
                                }
                            }
                        }
                        if (canPlace) {
                            placeCol = col;
                            placeRow = row;
                            break outer;
                        }
                    }
                }
            }

            if (placeCol < 0) continue;

            // グリッドを占有
            for (int dr = 0; dr < rowSpan; dr++) {
                for (int dc = 0; dc < colSpan; dc++) {
                    if (placeRow + dr < gridOccupied.length) {
                        gridOccupied[placeRow + dr][placeCol + dc] = true;
                    }
                }
            }

            // 座標計算
            int itemX = PADDING + placeCol * (cellWidth + GAP);
            int itemY = startY + placeRow * (cellHeight + GAP);
            int itemW = cellWidth * colSpan + GAP * (colSpan - 1);
            int itemH = cellHeight * rowSpan + GAP * (rowSpan - 1);

            // パネル境界チェック
            if (itemY + itemH > panelY + panelHeight - 20) {
                continue;
            }

            // クリック判定
            if (x >= itemX && x <= itemX + itemW && y >= itemY && y <= itemY + itemH) {
                System.out.println("ControlCenterManager: Grid item clicked - " + item.getDisplayName());
                GestureEvent tapEvent = new GestureEvent(jp.moyashi.phoneos.core.input.GestureType.TAP, x, y, x, y, System.currentTimeMillis(), System.currentTimeMillis());
                item.onGesture(tapEvent);
                return;
            }
        }

        System.out.println("ControlCenterManager: No grid item clicked at (" + x + "," + y + ")");
    }
    
    /**
     * コントロールセンターのスクロールを処理する。
     * 
     * @param event ドラッグイベント
     */
    private void handleControlCenterScroll(GestureEvent event) {
        if (!isDragScrolling) {
            isDragScrolling = true;
            lastDragY = event.getCurrentY();
            scrollVelocity = 0;
        } else {
            float deltaY = event.getCurrentY() - lastDragY;
            scrollOffset -= deltaY * SCROLL_SENSITIVITY;
            
            // スクロール範囲の制限
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > maxScrollOffset) scrollOffset = maxScrollOffset;
            
            scrollVelocity = -deltaY * SCROLL_SENSITIVITY;
            lastDragY = event.getCurrentY();
        }
    }
    
    /**
     * 指定された座標がコントロールセンターの範囲内かどうかを確認する。
     * 
     * @param x X座標
     * @param y Y座標
     * @return コントロールセンターが表示中で範囲内の場合true
     */
    @Override
    public boolean isInBounds(int x, int y) {
        // コントロールセンターが表示中の場合、画面全体をカバー
        boolean inBounds = this.isVisible && animationProgress > 0.1f;
        debugGesture("isInBounds(" + x + ", " + y + ") = " + inBounds + " (visible=" + isVisible + ", animProgress=" + animationProgress + ", priority=" + getPriority() + ")");
        return inBounds;
    }
}
