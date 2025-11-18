package jp.moyashi.phoneos.core.ui.theme;

import jp.moyashi.phoneos.core.service.SettingsManager;

/**
 * 繝・じ繧､繝ｳ繝医・繧ｯ繝ｳ繧堤函謌舌・謠蝉ｾ帙☆繧九ユ繝ｼ繝槭お繝ｳ繧ｸ繝ｳ縲・
 * SettingsManager縺ｮ繝ｬ繧ｸ繧ｹ繝医Μ繧定ｳｼ隱ｭ縺励√Λ繧､繝医・繝繝ｼ繧ｯ繝ｻ繧｢繧ｯ繧ｻ繝ｳ繝郁牡螟画峩繧貞叉譎ょ渚譏縺吶ｋ縲・
 */
public class ThemeEngine implements SettingsManager.SettingsListener {

    public enum Mode { LIGHT, DARK, AUTO, WHITE, ORANGE, YELLOW, PINK, GREEN, BLACK, AQUA }
    public enum Tone { LIGHT, DARK, AUTO }

    private final SettingsManager settings;

    // 繧ｻ繝槭Φ繝・ぅ繝・け繧ｫ繝ｩ繝ｼ (ARGB 0xAARRGGBB蠖｢蠑・
    private int colorPrimary;
    private int colorOnPrimary;
    private int colorBackground;
    private int colorSurface;
    private int colorOnSurface;
    private int colorOnSurfaceSecondary;
    private int colorBorder;
    private int colorError;
    private int colorWarning;
    private int colorSuccess;
    private int colorInfo;
    private int colorHover;
    private int colorPressed;

    // 蝗ｳ蠖｢繝舌Μ繧ｨ繝ｼ繧ｷ繝ｧ繝ｳ繝ｻ繧ｿ繧､繝昴げ繝ｩ繝輔ぅ・井ｸ驛ｨ縺ｮ縺ｿ蜈ｬ髢具ｼ・
    private int radiusSm = 8;
    private int radiusMd = 12;
    private int radiusLg = 16;

    public ThemeEngine(SettingsManager settings) {
        this.settings = settings;
        this.settings.addListener(this);
        recomputePalette();
    }

    public Mode getMode() {
        String m = settings.getStringSetting("ui.theme.mode", "light");
        switch (m) {
            case "dark": return Mode.DARK;
            case "auto": return Mode.AUTO;
            case "white": return Mode.WHITE;
            case "orange": return Mode.ORANGE;
            case "yellow": return Mode.YELLOW;
            case "pink": return Mode.PINK;
            case "green": return Mode.GREEN;
            case "black": return Mode.BLACK;
            case "aqua": return Mode.AQUA;
            default: return Mode.LIGHT;
        }
    }

    public Tone getTone() {
        String t = settings.getStringSetting("ui.theme.tone", null);
        if (t == null || t.isEmpty()) {
            String legacy = settings.getStringSetting("ui.theme.mode", "light");
            if ("dark".equals(legacy)) return Tone.DARK;
            if ("auto".equals(legacy)) return Tone.AUTO;
            return Tone.LIGHT;
        }
        switch (t) {
            case "dark": return Tone.DARK;
            case "auto": return Tone.AUTO;
            default: return Tone.LIGHT;
        }
    }

    public Mode getFamily() {
        String f = settings.getStringSetting("ui.theme.family", null);
        if (f == null || f.isEmpty()) {
            return getMode();
        }
        switch (f) {
            case "white": return Mode.WHITE;
            case "orange": return Mode.ORANGE;
            case "yellow": return Mode.YELLOW;
            case "pink": return Mode.PINK;
            case "green": return Mode.GREEN;
            case "black": return Mode.BLACK;
            case "aqua": return Mode.AQUA;
            default: return Mode.WHITE;
        }
    }
    public void recomputePalette() {
        // 險ｭ螳壹°繧峨Δ繝ｼ繝峨・繧ｫ繝ｩ繝ｼ繧貞叙蠕・
        Mode mode = getFamily();
        Tone tone = getTone();
        boolean dark = (tone == Tone.DARK);
        int primary = 0xFF4A90E2; // default
        

        // 繝吶・繧ｹ縺ｮ閭梧勹/繧ｵ繝ｼ繝輔ぉ繧ｹ・医Λ繧､繝医・繝繝ｼ繧ｯ縺ｧ邊励￥蝪励ｊ蛻・￠竊偵ヨ繝ｼ繝ｳ蛻･縺ｫ隱ｿ謨ｴ・・
        if (dark) {
            colorBackground = argb(0xFF, 0x12, 0x12, 0x12);
            colorSurface    = argb(0xFF, 0x20, 0x20, 0x20);
            colorOnSurface  = argb(0xFF, 0xFF, 0xFF, 0xFF);
            colorOnSurfaceSecondary = argb(0xFF, 0xAA, 0xAA, 0xAA);
            colorBorder     = argb(0xFF, 0x55, 0x55, 0x55);

            // 繝繝ｼ繧ｯ繝医・繝ｳ譎ゅ・繝輔ぃ繝溘Μ繝ｼ蛻･縺ｮ閭梧勹濶ｲ隱ｿ謨ｴ縲・QUA縺ｯ豺ｱ縺・ユ繧｣繝ｼ繝ｫ邉ｻ縺ｫ蟇ｾ蠢懊・
            if (mode == Mode.AQUA) {
                colorBackground = argb(0xFF, 0x0E, 0x1F, 0x24); // #0E1F24
                colorSurface    = argb(0xFF, 0x15, 0x2A, 0x31); // #152A31
                // onSurface縺ｯ逋ｽ繧堤ｶｭ謖√＠縲√そ繧ｫ繝ｳ繝繝ｪ繧偵ユ繧｣繝ｼ繝ｫ蟇・ｊ縺ｫ
                colorOnSurfaceSecondary = argb(0xFF, 0x9B, 0xB8, 0xBF);
                colorBorder     = argb(0xFF, 0x28, 0x40, 0x48);
            } else if (mode == Mode.ORANGE) {
                colorBackground = argb(0xFF, 0x20, 0x17, 0x11);
                colorSurface    = argb(0xFF, 0x2A, 0x1E, 0x16);
                colorOnSurfaceSecondary = argb(0xFF, 0xC9, 0xB2, 0xA6);
                colorBorder     = argb(0xFF, 0x4A, 0x35, 0x28);
            } else if (mode == Mode.PINK) {
                colorBackground = argb(0xFF, 0x21, 0x16, 0x1C);
                colorSurface    = argb(0xFF, 0x2B, 0x1D, 0x25);
                colorOnSurfaceSecondary = argb(0xFF, 0xD5, 0xB8, 0xC8);
                colorBorder     = argb(0xFF, 0x4A, 0x34, 0x42);
            } else if (mode == Mode.YELLOW) {
                colorBackground = argb(0xFF, 0x21, 0x1E, 0x12);
                colorSurface    = argb(0xFF, 0x2B, 0x26, 0x17);
                colorOnSurfaceSecondary = argb(0xFF, 0xD9, 0xCF, 0xA6);
                colorBorder     = argb(0xFF, 0x4A, 0x40, 0x28);
            } else if (mode == Mode.GREEN) {
                colorBackground = argb(0xFF, 0x12, 0x1A, 0x14);
                colorSurface    = argb(0xFF, 0x1A, 0x24, 0x1D);
                colorOnSurfaceSecondary = argb(0xFF, 0xB2, 0xC9, 0xBA);
                colorBorder     = argb(0xFF, 0x2D, 0x42, 0x33);
            } else if (mode == Mode.WHITE) {
                colorBackground = argb(0xFF, 0x12, 0x12, 0x13);
                colorSurface    = argb(0xFF, 0x1C, 0x1D, 0x1F);
                colorOnSurfaceSecondary = argb(0xFF, 0xA8, 0xAC, 0xB2);
                colorBorder     = argb(0xFF, 0x2F, 0x31, 0x33);
            } else if (mode == Mode.BLACK) {
                colorBackground = argb(0xFF, 0x0A, 0x0A, 0x0A);
                colorSurface    = argb(0xFF, 0x14, 0x14, 0x14);
                colorOnSurfaceSecondary = argb(0xFF, 0x9A, 0x9A, 0x9A);
                colorBorder     = argb(0xFF, 0x2A, 0x2A, 0x2A);
            }
        } else {
            colorBackground = argb(0xFF, 0xF3, 0xF4, 0xF6);
            colorSurface    = argb(0xFF, 0xF7, 0xF7, 0xF8);
            colorOnSurface  = argb(0xFF, 0x11, 0x11, 0x11);
            colorOnSurfaceSecondary = argb(0xFF, 0x66, 0x66, 0x66);
            colorBorder     = argb(0xFF, 0xD5, 0xD7, 0xDB);
        }

        // 繧ｫ繝ｩ繝ｼ繝｢繝ｼ繝会ｼ・RANGE/PINK/AQUA遲会ｼ峨・縺ｨ縺阪√Λ繧､繝域凾縺ｮ縺ｿ閭梧勹繧剃ｸ頑嶌縺・
        if (!dark) {
            if (mode == Mode.ORANGE) {
                // Light: 螟門・(閭梧勹)繧偵ｄ繧・ｿ・＞縲∝・蛛ｴ(髱｢)繧定埋縺・                colorBackground = argb(0xFF, 0xFF, 0xF1, 0xE6); // #FFF1E6 (豼・＞)
                colorSurface    = argb(0xFF, 0xFF, 0xF6, 0xEE); // #FFF6EE (阮・＞)
                colorOnSurface  = argb(0xFF, 0x12, 0x12, 0x12);
                colorOnSurfaceSecondary = argb(0xFF, 0x66, 0x58, 0x4F);
                colorBorder     = argb(0xFF, 0xE6, 0xD5, 0xC8);
                primary         = 0xFFF2994A;
            } else if (mode == Mode.PINK) {
                colorBackground = argb(0xFF, 0xFF, 0xE9, 0xF1); // #FFE9F1 (豼・＞)
                colorSurface    = argb(0xFF, 0xFF, 0xF0, 0xF5); // #FFF0F5 (阮・＞)
                colorOnSurface  = argb(0xFF, 0x12, 0x12, 0x12);
                colorOnSurfaceSecondary = argb(0xFF, 0x65, 0x55, 0x60);
                colorBorder     = argb(0xFF, 0xE1, 0xC8, 0xD2);
                primary         = 0xFFEC4899;
            } else if (mode == Mode.YELLOW) {
                // Yellow縺瑚埋縺剰ｦ九∴繧句撫鬘娯・螟門・繧呈ｿ・￥縲∝・蛛ｴ繧定埋縺上＠譏主ｺｦ蟾ｮ繧呈僑螟ｧ
                colorBackground = argb(0xFF, 0xFF, 0xF5, 0xCC); // #FFF5CC (豼・＞)
                colorSurface    = argb(0xFF, 0xFF, 0xF9, 0xE6); // #FFF9E6 (阮・＞)
                colorOnSurface  = argb(0xFF, 0x14, 0x12, 0x0A);
                colorOnSurfaceSecondary = argb(0xFF, 0x72, 0x68, 0x30);
                colorBorder     = argb(0xFF, 0xE8, 0xDD, 0xAA);
                primary         = 0xFFF2C94C;
            } else if (mode == Mode.GREEN) {
                // Green: 螟・豼・竊貞・(阮・縺ｧ驕募柱諢溯ｧ｣豸・                colorBackground = argb(0xFF, 0xE6, 0xF4, 0xEA); // #E6F4EA (豼・＞)
                colorSurface    = argb(0xFF, 0xED, 0xF9, 0xF0); // #EDF9F0 (阮・＞)
                colorOnSurface  = argb(0xFF, 0x10, 0x22, 0x14);
                colorOnSurfaceSecondary = argb(0xFF, 0x46, 0x66, 0x50);
                colorBorder     = argb(0xFF, 0xC8, 0xE2, 0xD0);
                primary         = 0xFF27AE60;
            } else if (mode == Mode.WHITE) {
                // WHITE (neutral light)
                primary         = 0xFF4A90E2;
            } else if (mode == Mode.AQUA) {
                // Aqua: 螟・豼・竊貞・(阮・ 縺ｫ菫ｮ豁｣
                colorBackground = argb(0xFF, 0xE5, 0xF3, 0xF8); // #E5F3F8 (豼・＞)
                colorSurface    = argb(0xFF, 0xEA, 0xF7, 0xFB); // #EAF7FB (阮・＞)
                colorOnSurface  = argb(0xFF, 0x11, 0x21, 0x26);
                colorOnSurfaceSecondary = argb(0xFF, 0x56, 0x66, 0x6C);
                colorBorder     = argb(0xFF, 0xC8, 0xDE, 0xE7);
                primary         = 0xFF0EA5E9;
            }
        } else {
            // 繝繝ｼ繧ｯ繝医・繝ｳ譎ゅ・繝励Λ繧､繝槭Μ濶ｲ・医ヵ繧｡繝溘Μ繝ｼ縺ｫ蝓ｺ縺･縺肴ｷｱ繧√・濶ｲ縺ｫ・・
            if (mode == Mode.ORANGE) {
                primary = 0xFFE57C1F;
            } else if (mode == Mode.PINK) {
                primary = 0xFFD9468D;
            } else if (mode == Mode.YELLOW) {
                primary = 0xFFD4AD28;
            } else if (mode == Mode.GREEN) {
                primary = 0xFF1E874C;
            } else if (mode == Mode.BLACK) {
                // 繝｢繝弱け繝ｭ邉ｻ縺ｧ繧よ桃菴懆牡縺ｯ隕冶ｪ肴ｧ縺ｮ鬮倥＞髱堤ｳｻ繧堤ｶｭ謖・
                primary = 0xFF4A90E2;
            } else { // WHITE / 縺昴・莉・
                primary = 0xFF4A90E2;
            }
        }

        // 繝励Λ繧､繝槭Μ繝ｼ濶ｲ縺ｨ蟇ｾ豈碑牡
        // ・医Λ繧､繝医〒縺ｯ闍･蟷ｲ繝繝ｼ繧ｯ縺ｫ・峨・繝ｩ繧ｰ蜀・〒闍･蟷ｲ濶ｲ繧呈囓繧√↓蝗ｺ螳・
        colorPrimary   = withAlpha(dark ? primary : darken(primary, 0.06f), 0xFF);
        colorOnPrimary = getContrastTextColor(colorPrimary);

        // ダークトーンでは常にonSurfaceを白に固定（コントラスト担保）
        if (dark) { colorOnSurface = argb(0xFF,0xFF,0xFF,0xFF); }

        // 繧ｹ繝・・繧ｿ繧ｹ邉ｻ
        colorError   = argb(0xFF, 0xE7, 0x4C, 0x3C);
        colorWarning = argb(0xFF, 0xF5, 0xA6, 0x0D);
        colorSuccess = argb(0xFF, 0x27, 0xAE, 0x60);
        colorInfo    = argb(0xFF, 0x2D, 0x9C, 0xDB);

        // 繧ｹ繝・・繝育ｳｻ・医・繝舌・/繝励Ξ繧ｹ縺ｯ繝｢繝ｼ繝峨↓蠢懊§縺ｦ譏主ｺｦ螟画鋤・・
        colorHover   = dark ? lighten(colorPrimary, 0.08f) : darken(colorPrimary, 0.08f);
        colorPressed = dark ? lighten(colorPrimary, 0.16f) : darken(colorPrimary, 0.16f);

        // 隗剃ｸｸ縺ｮ繧ｹ繧ｱ繝ｼ繝ｫ蛻･縺ｫ蟆主・
        String corner = settings.getStringSetting("ui.shape.corner_scale", "standard");
        if ("compact".equals(corner)) { radiusSm = 6; radiusMd = 8; radiusLg = 12; }
        else if ("rounded".equals(corner)) { radiusSm = 10; radiusMd = 14; radiusLg = 20; }
        else { radiusSm = 8; radiusMd = 12; radiusLg = 16; }
    }

    // 繝ｪ繧ｹ繝翫・: 險ｭ螳壼､画峩譎ゅ↓繝医・繧ｯ繝ｳ繧貞・險育ｮ・
    @Override
    public void onSettingChanged(String key, Object newValue) {
        if (key != null && (key.startsWith("ui.theme.") || key.startsWith("ui.shape."))) {
            recomputePalette();
        }
    }

    // 繝ｭ繝ｼ繧ｫ繝ｫ繧ｲ繝・ち繝ｼ・亥・髢具ｼ・
    public int colorPrimary() { return colorPrimary; }
    public int colorOnPrimary() { return colorOnPrimary; }
    public int colorBackground() { return colorBackground; }
    public int colorSurface() { return colorSurface; }
    public int colorOnSurface() { return colorOnSurface; }
    public int colorOnSurfaceSecondary() { return colorOnSurfaceSecondary; }
    public int colorBorder() { return colorBorder; }
    public int colorHover() { return colorHover; }
    public int colorPressed() { return colorPressed; }
    public int colorSuccess() { return colorSuccess; }
    public int colorWarning() { return colorWarning; }
    public int colorError() { return colorError; }
    public int colorInfo() { return colorInfo; }

    public int radiusSm() { return radiusSm; }
    public int radiusMd() { return radiusMd; }
    public int radiusLg() { return radiusLg; }

    // 繝倥Ν繝代・
    private static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static int withAlpha(int c, int a) { return (c & 0x00FFFFFF) | ((a & 0xFF) << 24); }

    private static int parseColor(String hex) {
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.length() == 6) {
            int rgb = (int) Long.parseLong(s, 16);
            return 0xFF000000 | rgb;
        } else if (s.length() == 8) {
            return (int) Long.parseLong(s, 16);
        }
        return 0xFF4A90E2;
    }

    private static int getContrastTextColor(int bg) {
        double r = ((bg >> 16) & 0xFF) / 255.0;
        double g = ((bg >> 8) & 0xFF) / 255.0;
        double b = (bg & 0xFF) / 255.0;
        // 逶ｸ蟇ｾ霈晏ｺｦ
        double L = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        return (L > 0.58) ? 0xFF000000 : 0xFFFFFFFF;
    }

    private static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    private static int darken(int c, float amt) {
        int a = (c >> 24) & 0xFF;
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        r = clamp((int) (r * (1 - amt)));
        g = clamp((int) (g * (1 - amt)));
        b = clamp((int) (b * (1 - amt)));
        return argb(a, r, g, b);
    }

    private static int lighten(int c, float amt) {
        int a = (c >> 24) & 0xFF;
        int r = (c >> 16) & 0xFF;
        int g = (c >> 8) & 0xFF;
        int b = c & 0xFF;
        r = clamp((int) (r + (255 - r) * amt));
        g = clamp((int) (g + (255 - g) * amt));
        b = clamp((int) (b + (255 - b) * amt));
        return argb(a, r, g, b);
    }
}
