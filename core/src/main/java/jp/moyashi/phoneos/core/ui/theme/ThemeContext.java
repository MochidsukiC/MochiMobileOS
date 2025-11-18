package jp.moyashi.phoneos.core.ui.theme;

/**
 * UI全体からThemeEngineへアクセスするための簡易コンテキスト。
 * Kernel初期化時にセットし、各コンポーネントが参照できるようにする。
 */
public final class ThemeContext {
    private static ThemeEngine theme;

    private ThemeContext() {}

    public static void setTheme(ThemeEngine engine) { theme = engine; }
    public static ThemeEngine getTheme() { return theme; }
}

