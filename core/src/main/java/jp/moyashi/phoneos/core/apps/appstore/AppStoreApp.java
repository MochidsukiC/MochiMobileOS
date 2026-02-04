package jp.moyashi.phoneos.core.apps.appstore;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.apps.appstore.ui.AppStoreScreen;
import processing.core.PImage;

/**
 * MochiMobileOS用のAppStoreアプリケーション。
 * 外部MODから登録されたアプリケーションのインストール・管理機能を提供する。
 *
 * 主な機能：
 * - 利用可能なMODアプリ一覧の表示
 * - アプリのインストール/アンインストール
 * - インストール済みアプリの管理
 *
 * @author jp.moyashi
 * @version 1.0
 * @since 1.0
 */
public class AppStoreApp implements IApplication {

    /** アプリケーションメタデータ */
    private static final String APP_ID = "jp.moyashi.phoneos.core.apps.appstore";
    private static final String APP_NAME = "App Store";
    private static final String APP_VERSION = "1.1.0";
    private static final String APP_DESCRIPTION = "Download and manage MochiMobileOS applications";

    /** 初期化状態 */
    private boolean isInitialized = false;

    /**
     * 新しいAppStoreアプリケーションインスタンスを作成する。
     */
    public AppStoreApp() {
        // Instance created
    }

    /**
     * このアプリケーションの一意識別子を取得する。
     *
     * @return アプリケーションID
     */
    @Override
    public String getApplicationId() {
        return APP_ID;
    }

    /**
     * このアプリケーションの表示名を取得する。
     *
     * @return アプリケーション名
     */
    @Override
    public String getName() {
        return APP_NAME;
    }

    /**
     * このアプリケーションのバージョンを取得する。
     *
     * @return アプリケーションバージョン
     */
    @Override
    public String getVersion() {
        return APP_VERSION;
    }

    /**
     * このアプリケーションの説明を取得する。
     *
     * @return アプリケーション説明
     */
    @Override
    public String getDescription() {
        return APP_DESCRIPTION;
    }

    /**
     * アプリケーションのアイコンを取得する。
     * 動的に生成されたショッピングバッグアイコンを返す。
     * 
     * @param kernel OSカーネルインスタンス
     * @return アプリケーションアイコン
     */
    @Override
    public PImage getIcon(Kernel kernel) {
        int size = 64;
        java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = image.createGraphics();
        
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(java.awt.RenderingHints.KEY_RENDERING, java.awt.RenderingHints.VALUE_RENDER_QUALITY);

        // テーマカラー取得
        java.awt.Color themeColor = new java.awt.Color(30, 136, 229); // Blue 600
        if (kernel != null && kernel.getThemeEngine() != null) {
            int primary = kernel.getThemeEngine().colorPrimary();
            themeColor = new java.awt.Color((primary >> 16) & 0xFF, (primary >> 8) & 0xFF, primary & 0xFF);
        }

        // 背景: グラデーション
        java.awt.GradientPaint bgGradient = new java.awt.GradientPaint(
            0, 0, themeColor.brighter(),
            size, size, themeColor.darker()
        );
        g.setPaint(bgGradient);
        g.fillRoundRect(0, 0, size, size, 16, 16);

        // ショッピングバッグの描画
        g.setColor(java.awt.Color.WHITE);
        int bagWidth = 36;
        int bagHeight = 32;
        int bagX = (size - bagWidth) / 2;
        int bagY = (size - bagHeight) / 2 + 4;

        // 持ち手
        g.setStroke(new java.awt.BasicStroke(3));
        g.drawArc(bagX + 8, bagY - 12, 20, 20, 0, 180);

        // バッグ本体
        java.awt.geom.RoundRectangle2D bag = new java.awt.geom.RoundRectangle2D.Float(bagX, bagY, bagWidth, bagHeight, 8, 8);
        g.fill(bag);
        
        // バッグの中のシンボル（クローバー）
        g.setColor(themeColor);
        int symCenterX = size / 2;
        int symCenterY = bagY + 16;
        int leafSize = 10;
        int leafOffset = 4;
        
        // 四つ葉のクローバー
        // 上
        g.fillOval(symCenterX - leafSize/2, symCenterY - leafOffset - leafSize, leafSize, leafSize);
        // 下
        g.fillOval(symCenterX - leafSize/2, symCenterY + leafOffset, leafSize, leafSize);
        // 左
        g.fillOval(symCenterX - leafOffset - leafSize, symCenterY - leafSize/2, leafSize, leafSize);
        // 右
        g.fillOval(symCenterX + leafOffset, symCenterY - leafSize/2, leafSize, leafSize);
        
        // 中央の結合部
        g.fillOval(symCenterX - 3, symCenterY - 3, 6, 6);
        
        // 茎
        g.setStroke(new java.awt.BasicStroke(2.5f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
        g.drawArc(symCenterX - 4, symCenterY + 4, 8, 12, 0, 90);

        g.dispose();
        
        return new PImage(image);
    }
    
    /**
     * このアプリケーションのエントリースクリーンを取得する。
     *
     * @param kernel OSカーネルインスタンス
     * @return AppStoreのメインスクリーン
     */
    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new AppStoreScreen(kernel);
    }

    /**
     * AppStoreアプリケーションを初期化する。
     * アプリケーションが最初に読み込まれるときに呼び出される。
     *
     * @param kernel OSカーネルインスタンス
     */
    @Override
    public void onInitialize(Kernel kernel) {
        if (!isInitialized) {
            isInitialized = true;
        }
    }

    /**
     * AppStoreアプリケーションをクリーンアップする。
     * アプリケーションがアンロードされるときに呼び出される。
     */
    @Override
    public void onDestroy() {
        if (isInitialized) {
            isInitialized = false;
        }
    }

    /**
     * アプリケーションが初期化されているかどうかを確認する。
     *
     * @return 初期化されている場合true、そうでなければfalse
     */
    public boolean isInitialized() {
        return isInitialized;
    }
}
