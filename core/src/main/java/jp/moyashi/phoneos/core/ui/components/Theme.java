package jp.moyashi.phoneos.core.ui.components;

import processing.core.PFont;

/**
 * UIテーマシステム。
 * カラー、フォント、サイズの統一管理。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class Theme {

    // カラー
    public int primaryColor = 0xFF4A90E2;
    public int secondaryColor = 0xFF50E3C2;
    public int backgroundColor = 0xFFFFFFFF;
    public int surfaceColor = 0xFFF5F5F5;
    public int errorColor = 0xFFE74C3C;
    public int textColor = 0xFF000000;
    public int textSecondaryColor = 0xFF666666;
    public int borderColor = 0xFFCCCCCC;

    // フォント
    public PFont defaultFont = null;
    public PFont titleFont = null;

    // サイズ
    public float textSizeSmall = 12;
    public float textSizeMedium = 14;
    public float textSizeLarge = 18;
    public float buttonHeight = 40;
    public float inputHeight = 35;
    public float spacing = 10;
    public float cornerRadius = 5;

    // シングルトン
    private static Theme instance = new Theme();

    public static Theme getInstance() {
        return instance;
    }

    private Theme() {
        // デフォルトテーマ
    }

    /**
     * ダークテーマに切り替え。
     */
    public void applyDarkTheme() {
        primaryColor = 0xFF3A7BC8;
        backgroundColor = 0xFF1E1E1E;
        surfaceColor = 0xFF2D2D2D;
        textColor = 0xFFFFFFFF;
        textSecondaryColor = 0xFFAAAAAA;
        borderColor = 0xFF555555;
    }

    /**
     * ライトテーマに切り替え。
     */
    public void applyLightTheme() {
        primaryColor = 0xFF4A90E2;
        backgroundColor = 0xFFFFFFFF;
        surfaceColor = 0xFFF5F5F5;
        textColor = 0xFF000000;
        textSecondaryColor = 0xFF666666;
        borderColor = 0xFFCCCCCC;
    }
}
