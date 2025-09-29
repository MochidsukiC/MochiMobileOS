package jp.moyashi.phoneos.core.coordinate;

/**
 * MochiMobileOS統一座標変換システム。
 *
 * PGraphics版とPApplet版、描画座標と当たり判定座標の不整合を解決し、
 * 統一的な座標系でUIコンポーネントの座標計算を簡素化する。
 *
 * このクラスは以下の問題を解決します：
 * - PGraphics/PApplet間での描画ロジックの差異
 * - アニメーション進行度による複雑な座標計算
 * - 描画座標と当たり判定座標の不一致
 * - UIコンポーネント間での座標計算の重複
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class CoordinateTransform {

    /** 画面幅 */
    private final float screenWidth;

    /** 画面高さ */
    private final float screenHeight;

    /**
     * 座標変換システムを初期化する。
     *
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     */
    public CoordinateTransform(float screenWidth, float screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        System.out.println("CoordinateTransform: 統一座標変換システムを初期化 (" + screenWidth + "x" + screenHeight + ")");
    }

    /**
     * 画面幅を取得する。
     *
     * @return 画面幅
     */
    public float getScreenWidth() {
        return screenWidth;
    }

    /**
     * 画面高さを取得する。
     *
     * @return 画面高さ
     */
    public float getScreenHeight() {
        return screenHeight;
    }

    /**
     * アニメーション可能なパネルの座標を計算する。
     *
     * 画面下部からスライドイン/アウトするパネル（コントロールセンター、通知センターなど）の
     * 統一的な座標計算を提供する。
     *
     * @param panelHeightRatio パネルの高さ比率（0.0～1.0）
     * @param animationProgress アニメーション進行度（0.0=非表示, 1.0=完全表示）
     * @return パネル座標情報
     */
    public PanelCoordinates calculateAnimatedPanel(float panelHeightRatio, float animationProgress) {
        // パネルの絶対高さを計算
        float panelHeight = screenHeight * panelHeightRatio;

        // アニメーション進行度に基づくY座標を計算
        // animationProgress=0.0: パネルは画面外（下部）
        // animationProgress=1.0: パネルは完全表示
        float panelY = screenHeight - (panelHeight * animationProgress);

        return new PanelCoordinates(panelY, panelHeight, animationProgress);
    }

    /**
     * リストアイテムの座標を計算する。
     *
     * 縦方向にスクロール可能なリストアイテムの座標計算を統一的に処理する。
     *
     * @param startY リスト開始Y座標
     * @param itemHeight アイテムの高さ
     * @param itemMargin アイテム間のマージン
     * @param itemIndex アイテムのインデックス（0から開始）
     * @param scrollOffset スクロールオフセット
     * @return アイテム座標情報
     */
    public ItemCoordinates calculateListItem(float startY, float itemHeight, float itemMargin,
                                           int itemIndex, float scrollOffset) {
        // アイテムのY座標を計算
        float itemY = startY + itemMargin + (itemIndex * (itemHeight + itemMargin)) - scrollOffset;

        return new ItemCoordinates(itemY, itemHeight, itemIndex, scrollOffset);
    }

    /**
     * 表示領域内でのアイテム可視性を判定する。
     *
     * @param itemY アイテムのY座標
     * @param itemHeight アイテムの高さ
     * @param visibleAreaY 表示領域の開始Y座標
     * @param visibleAreaHeight 表示領域の高さ
     * @return アイテムが表示領域内にある場合true
     */
    public boolean isItemVisible(float itemY, float itemHeight, float visibleAreaY, float visibleAreaHeight) {
        float itemBottom = itemY + itemHeight;
        float visibleAreaBottom = visibleAreaY + visibleAreaHeight;

        // アイテムの一部でも表示領域内にあれば可視と判定
        return (itemY < visibleAreaBottom) && (itemBottom > visibleAreaY);
    }

    /**
     * 点（クリック座標）がUI要素の範囲内にあるかを判定する。
     *
     * @param pointX 点のX座標
     * @param pointY 点のY座標
     * @param elementX 要素のX座標
     * @param elementY 要素のY座標
     * @param elementWidth 要素の幅
     * @param elementHeight 要素の高さ
     * @return 点が要素範囲内にある場合true
     */
    public boolean isPointInBounds(float pointX, float pointY, float elementX, float elementY,
                                  float elementWidth, float elementHeight) {
        return pointX >= elementX &&
               pointX <= elementX + elementWidth &&
               pointY >= elementY &&
               pointY <= elementY + elementHeight;
    }

    /**
     * スクロール可能な最大オフセットを計算する。
     *
     * @param totalContentHeight コンテンツの総高さ
     * @param visibleAreaHeight 表示領域の高さ
     * @return 最大スクロールオフセット
     */
    public float calculateMaxScrollOffset(float totalContentHeight, float visibleAreaHeight) {
        return Math.max(0, totalContentHeight - visibleAreaHeight);
    }

    /**
     * スクロールオフセットを制限内に収める。
     *
     * @param scrollOffset 現在のスクロールオフセット
     * @param maxScrollOffset 最大スクロールオフセット
     * @return 制限内に調整されたスクロールオフセット
     */
    public float constrainScrollOffset(float scrollOffset, float maxScrollOffset) {
        return Math.max(0, Math.min(scrollOffset, maxScrollOffset));
    }

    /**
     * パネル座標情報を格納するデータクラス。
     */
    public static class PanelCoordinates {
        public final float panelY;
        public final float panelHeight;
        public final float animationProgress;

        public PanelCoordinates(float panelY, float panelHeight, float animationProgress) {
            this.panelY = panelY;
            this.panelHeight = panelHeight;
            this.animationProgress = animationProgress;
        }

        /**
         * パネル内の相対座標（ヘッダー分のオフセット付き）を計算する。
         *
         * @param headerOffset ヘッダー分のオフセット
         * @return パネル内のコンテンツ開始Y座標
         */
        public float getContentStartY(float headerOffset) {
            return panelY + headerOffset;
        }

        /**
         * パネル内で利用可能な高さを計算する。
         *
         * @param headerOffset ヘッダー分のオフセット
         * @param footerOffset フッター分のオフセット
         * @return 利用可能な高さ
         */
        public float getAvailableHeight(float headerOffset, float footerOffset) {
            return panelHeight - headerOffset - footerOffset;
        }

        @Override
        public String toString() {
            return String.format("PanelCoordinates{panelY=%.1f, panelHeight=%.1f, animationProgress=%.2f}",
                               panelY, panelHeight, animationProgress);
        }
    }

    /**
     * アイテム座標情報を格納するデータクラス。
     */
    public static class ItemCoordinates {
        public final float itemY;
        public final float itemHeight;
        public final int itemIndex;
        public final float scrollOffset;

        public ItemCoordinates(float itemY, float itemHeight, int itemIndex, float scrollOffset) {
            this.itemY = itemY;
            this.itemHeight = itemHeight;
            this.itemIndex = itemIndex;
            this.scrollOffset = scrollOffset;
        }

        /**
         * アイテムの下端Y座標を取得する。
         *
         * @return 下端Y座標
         */
        public float getBottom() {
            return itemY + itemHeight;
        }

        @Override
        public String toString() {
            return String.format("ItemCoordinates{itemY=%.1f, itemHeight=%.1f, itemIndex=%d, scrollOffset=%.1f}",
                               itemY, itemHeight, itemIndex, scrollOffset);
        }
    }

    /**
     * デバッグ用に座標情報を文字列で出力する。
     *
     * @param componentName コンポーネント名
     * @param coordinates 座標情報
     * @return デバッグ文字列
     */
    public String debugString(String componentName, Object coordinates) {
        return String.format("CoordinateTransform[%s]: %s", componentName, coordinates.toString());
    }
}