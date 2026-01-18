package jp.moyashi.phoneos.core.ui.theme;

import jp.moyashi.phoneos.core.service.SettingsManager;

/**
 * ThemeEngine provides design tokens and color palettes.
 * It subscribes to SettingsManager to update colors based on light/dark mode and accents.
 */
public class ThemeEngine implements SettingsManager.SettingsListener {

    public enum Mode { LIGHT, DARK, AUTO, WHITE, ORANGE, YELLOW, PINK, GREEN, BLACK, AQUA }
    public enum Tone { LIGHT, DARK, AUTO }

    private final SettingsManager settings;

    // Semantic colors (ARGB 0xAARRGGBB)
    private volatile int colorPrimary;
    private volatile int colorOnPrimary;
    private volatile int colorBackground;
    private volatile int colorSurface;
    private volatile int colorOnSurface;
    private volatile int colorOnSurfaceSecondary;
    private volatile int colorBorder;
    private volatile int colorError;
    private volatile int colorWarning;
    private volatile int colorSuccess;
    private volatile int colorInfo;
    private volatile int colorHover;
    private volatile int colorPressed;

    // Shape and Typography
    private volatile int radiusSm = 8;
    private volatile int radiusMd = 12;
    private volatile int radiusLg = 16;

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
        // Get mode and color from settings
        Mode mode = getFamily();
        Tone tone = getTone();
        boolean dark = (tone == Tone.DARK);
        int primary = 0xFF4A90E2; // default
        

        // Base Background/Surface (Split by Light/Dark -> Adjust by Tone)
        if (dark) {
            colorBackground = argb(0xFF, 0x12, 0x12, 0x12);
            colorSurface    = argb(0xFF, 0x20, 0x20, 0x20);
            colorOnSurface  = argb(0xFF, 0xFF, 0xFF, 0xFF);
            colorOnSurfaceSecondary = argb(0xFF, 0xAA, 0xAA, 0xAA);
            colorBorder     = argb(0xFF, 0x55, 0x55, 0x55);

            // Background color adjustment by family in Dark Tone. AQUA corresponds to deep Teal.
            if (mode == Mode.AQUA) {
                colorBackground = argb(0xFF, 0x0E, 0x1F, 0x24); // #0E1F24
                colorSurface    = argb(0xFF, 0x15, 0x2A, 0x31); // #152A31
                // Maintain white for onSurface, Teal-ish for Secondary
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

        // Overwrite background in Light Mode for Color Modes (ORANGE/PINK/AQUA etc.)
        if (!dark) {
            if (mode == Mode.ORANGE) {
                // Light: Outer (Background) slightly light, Inner (Surface) slightly dark
                colorBackground = argb(0xFF, 0xFF, 0xF1, 0xE6); // #FFF1E6
                colorSurface    = argb(0xFF, 0xFF, 0xF6, 0xEE); // #FFF6EE
                colorOnSurface  = argb(0xFF, 0x12, 0x12, 0x12);
                colorOnSurfaceSecondary = argb(0xFF, 0x66, 0x58, 0x4F);
                colorBorder     = argb(0xFF, 0xE6, 0xD5, 0xC8);
                primary         = 0xFFF2994A;
            } else if (mode == Mode.PINK) {
                colorBackground = argb(0xFF, 0xFF, 0xE9, 0xF1); // #FFE9F1
                colorSurface    = argb(0xFF, 0xFF, 0xF0, 0xF5); // #FFF0F5
                colorOnSurface  = argb(0xFF, 0x12, 0x12, 0x12);
                colorOnSurfaceSecondary = argb(0xFF, 0x65, 0x55, 0x60);
                colorBorder     = argb(0xFF, 0xE1, 0xC8, 0xD2);
                primary         = 0xFFEC4899;
            } else if (mode == Mode.YELLOW) {
                // Yellow
                colorBackground = argb(0xFF, 0xFF, 0xF5, 0xCC); // #FFF5CC
                colorSurface    = argb(0xFF, 0xFF, 0xF9, 0xE6); // #FFF9E6
                colorOnSurface  = argb(0xFF, 0x14, 0x12, 0x0A);
                colorOnSurfaceSecondary = argb(0xFF, 0x72, 0x68, 0x30);
                colorBorder     = argb(0xFF, 0xE8, 0xDD, 0xAA);
                primary         = 0xFFF2C94C;
            } else if (mode == Mode.GREEN) {
                // Green: Dark -> Light -> Dark (resolved with surface)
                colorBackground = argb(0xFF, 0xE6, 0xF4, 0xEA); // #E6F4EA
                colorSurface    = argb(0xFF, 0xED, 0xF9, 0xF0); // #EDF9F0
                colorOnSurface  = argb(0xFF, 0x10, 0x22, 0x14);
                colorOnSurfaceSecondary = argb(0xFF, 0x46, 0x66, 0x50);
                colorBorder     = argb(0xFF, 0xC8, 0xE2, 0xD0);
                primary         = 0xFF27AE60;
            } else if (mode == Mode.WHITE) {
                // WHITE (neutral light)
                primary         = 0xFF4A90E2;
            } else if (mode == Mode.AQUA) {
                // Aqua: Dark -> Light -> Dark
                colorBackground = argb(0xFF, 0xE5, 0xF3, 0xF8); // #E5F3F8
                colorSurface    = argb(0xFF, 0xEA, 0xF7, 0xFB); // #EAF7FB
                colorOnSurface  = argb(0xFF, 0x11, 0x21, 0x26);
                colorOnSurfaceSecondary = argb(0xFF, 0x56, 0x66, 0x6C);
                colorBorder     = argb(0xFF, 0xC8, 0xDE, 0xE7);
                primary         = 0xFF0EA5E9;
            }
        } else {
            // Primary color in Dark Tone (Deep color based on family)
            if (mode == Mode.ORANGE) {
                primary = 0xFFE57C1F;
            } else if (mode == Mode.PINK) {
                primary = 0xFFD9468D;
            } else if (mode == Mode.YELLOW) {
                primary = 0xFFD4AD28;
            } else if (mode == Mode.GREEN) {
                primary = 0xFF1E874C;
            } else if (mode == Mode.BLACK) {
                // Maintain Blue system for high visibility in Monochrome
                primary = 0xFF4A90E2;
            } else { // WHITE / Others
                primary = 0xFF4A90E2;
            }
        }

        // Contrast between Primary and Color
        // (Slightly dark in Light, fixed slightly lighter in Dark)
        colorPrimary   = withAlpha(dark ? primary : darken(primary, 0.06f), 0xFF);
        colorOnPrimary = getContrastTextColor(colorPrimary);

        // Always fix onSurface to white in Dark Tone (ensure contrast)
        if (dark) { colorOnSurface = argb(0xFF,0xFF,0xFF,0xFF); }

        // Status Colors
        colorError   = argb(0xFF, 0xE7, 0x4C, 0x3C);
        colorWarning = argb(0xFF, 0xF5, 0xA6, 0x0D);
        colorSuccess = argb(0xFF, 0x27, 0xAE, 0x60);
        colorInfo    = argb(0xFF, 0x2D, 0x9C, 0xDB);

        // State Colors (Hover/Pressed change brightness according to mode)
        colorHover   = dark ? lighten(colorPrimary, 0.08f) : darken(colorPrimary, 0.08f);
        colorPressed = dark ? lighten(colorPrimary, 0.16f) : darken(colorPrimary, 0.16f);

        // Separate by Corner Scale
        String corner = settings.getStringSetting("ui.shape.corner_scale", "standard");
        if ("compact".equals(corner)) { radiusSm = 6; radiusMd = 8; radiusLg = 12; }
        else if ("rounded".equals(corner)) { radiusSm = 10; radiusMd = 14; radiusLg = 20; }
        else { radiusSm = 8; radiusMd = 12; radiusLg = 16; }
    }

    // Listener: Recalculate tokens when settings change
    @Override
    public void onSettingChanged(String key, Object newValue) {
        if (key != null && (key.startsWith("ui.theme.") || key.startsWith("ui.shape."))) {
            recomputePalette();
        }
    }

    // Local Getters (Public)
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

    // Helpers
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
