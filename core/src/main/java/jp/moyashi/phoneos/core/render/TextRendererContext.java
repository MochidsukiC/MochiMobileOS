package jp.moyashi.phoneos.core.render;

/**
 * TextRendererのグローバルコンテキスト。
 * UIコンポーネントがTextRendererにアクセスするための静的参照を提供。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class TextRendererContext {

    /** グローバルTextRendererインスタンス */
    private static TextRenderer textRenderer;

    /**
     * TextRendererを設定する。
     * Kernel初期化時に呼び出される。
     *
     * @param renderer TextRendererインスタンス
     */
    public static void setTextRenderer(TextRenderer renderer) {
        textRenderer = renderer;
    }

    /**
     * TextRendererを取得する。
     *
     * @return TextRenderer、設定されていない場合はnull
     */
    public static TextRenderer getTextRenderer() {
        return textRenderer;
    }

    /**
     * TextRendererが設定されているかどうかを確認する。
     *
     * @return 設定されている場合true
     */
    public static boolean hasTextRenderer() {
        return textRenderer != null;
    }
}
