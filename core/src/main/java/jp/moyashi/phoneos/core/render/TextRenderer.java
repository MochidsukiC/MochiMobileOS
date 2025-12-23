package jp.moyashi.phoneos.core.render;

import jp.moyashi.phoneos.core.util.EmojiUtil;
import jp.moyashi.phoneos.core.util.EmojiUtil.TextSegment;
import processing.core.PFont;
import processing.core.PGraphics;

import java.util.List;

/**
 * 絵文字フォントフォールバック対応のテキストレンダラー。
 * テキストを適切なフォント（日本語フォントまたは絵文字フォント）で描画する。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class TextRenderer {

    /** プライマリフォント（日本語） */
    private final PFont primaryFont;

    /** 絵文字フォント */
    private final PFont emojiFont;

    /** テキストの垂直オフセット（絵文字に合わせるためのベースライン調整用） */
    private static final float TEXT_VERTICAL_OFFSET = 2.0f;

    /**
     * TextRendererを作成する。
     *
     * @param primaryFont プライマリフォント（日本語）
     * @param emojiFont 絵文字フォント
     */
    public TextRenderer(PFont primaryFont, PFont emojiFont) {
        this.primaryFont = primaryFont;
        this.emojiFont = emojiFont;
    }

    /**
     * 絵文字フォールバック対応でテキストを描画する。
     * 効率のためセグメントベースのレンダリングを使用。
     * 描画後はプライマリフォントに復元する。
     *
     * @param g PGraphicsコンテキスト
     * @param text 描画するテキスト
     * @param x X座標
     * @param y Y座標
     * @param textSize フォントサイズ
     */
    public void drawText(PGraphics g, String text, float x, float y, float textSize) {
        if (text == null || text.isEmpty()) {
            return;
        }

        // Fast path: 絵文字なし
        if (!EmojiUtil.containsEmoji(text)) {
            if (primaryFont != null) {
                g.textFont(primaryFont);
            }
            g.textSize(textSize);
            g.text(text, x, y);
            return;
        }

        // Slow path: セグメント分割してレンダリング
        List<TextSegment> segments = EmojiUtil.segmentText(text);
        float currentX = x;

        for (TextSegment segment : segments) {
            PFont font = segment.isEmoji ? emojiFont : primaryFont;
            if (font != null) {
                g.textFont(font);
            }
            g.textSize(textSize);
            // 通常テキストを少し下にオフセット（絵文字に合わせるベースライン調整）
            float drawY = segment.isEmoji ? y : y + TEXT_VERTICAL_OFFSET;
            g.text(segment.text, currentX, drawY);
            currentX += g.textWidth(segment.text);
        }

        // プライマリフォントに復元（他のテキスト描画への影響を防ぐ）
        if (primaryFont != null) {
            g.textFont(primaryFont);
        }
    }

    /**
     * 絵文字対応でテキストの幅を計算する。
     * 計算後はプライマリフォントに復元する。
     *
     * @param g PGraphicsコンテキスト
     * @param text 計測するテキスト
     * @param textSize フォントサイズ
     * @return テキストの幅
     */
    public float getTextWidth(PGraphics g, String text, float textSize) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Fast path: 絵文字なし
        if (!EmojiUtil.containsEmoji(text)) {
            if (primaryFont != null) {
                g.textFont(primaryFont);
            }
            g.textSize(textSize);
            return g.textWidth(text);
        }

        // Slow path: セグメントごとに計算
        List<TextSegment> segments = EmojiUtil.segmentText(text);
        float totalWidth = 0;

        for (TextSegment segment : segments) {
            PFont font = segment.isEmoji ? emojiFont : primaryFont;
            if (font != null) {
                g.textFont(font);
            }
            g.textSize(textSize);
            totalWidth += g.textWidth(segment.text);
        }

        // プライマリフォントに復元
        if (primaryFont != null) {
            g.textFont(primaryFont);
        }

        return totalWidth;
    }

    /**
     * 各文字位置での累積幅を取得する（カーソル/選択計算用）。
     * 計算後はプライマリフォントに復元する。
     *
     * @param g PGraphicsコンテキスト
     * @param text 計測するテキスト
     * @param textSize フォントサイズ
     * @return 各文字位置での累積幅の配列
     */
    public float[] getCharacterWidths(PGraphics g, String text, float textSize) {
        if (text == null || text.isEmpty()) {
            return new float[0];
        }

        float[] widths = new float[text.length() + 1];
        widths[0] = 0;
        float cumulative = 0;

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            int charCount = Character.charCount(codePoint);
            String ch = text.substring(i, i + charCount);

            PFont font = EmojiUtil.isEmoji(codePoint) ? emojiFont : primaryFont;
            if (font != null) {
                g.textFont(font);
            }
            g.textSize(textSize);

            cumulative += g.textWidth(ch);

            // このコードポイントの全charユニットに幅を設定
            for (int j = 0; j < charCount && (i + j) < text.length(); j++) {
                widths[i + j + 1] = cumulative;
            }

            i += charCount;
        }

        // プライマリフォントに復元
        if (primaryFont != null) {
            g.textFont(primaryFont);
        }

        return widths;
    }

    /**
     * 指定位置の文字を描画するためのフォントを取得する。
     *
     * @param text テキスト
     * @param position 文字位置
     * @return 使用するフォント
     */
    public PFont getFontForPosition(String text, int position) {
        if (text == null || position < 0 || position >= text.length()) {
            return primaryFont;
        }

        int codePoint = text.codePointAt(position);
        return EmojiUtil.isEmoji(codePoint) ? emojiFont : primaryFont;
    }

    /**
     * プライマリフォントを取得する。
     *
     * @return プライマリフォント
     */
    public PFont getPrimaryFont() {
        return primaryFont;
    }

    /**
     * 絵文字フォントを取得する。
     *
     * @return 絵文字フォント
     */
    public PFont getEmojiFont() {
        return emojiFont;
    }

    /**
     * 絵文字フォントが利用可能かどうかを確認する。
     *
     * @return 絵文字フォントが利用可能な場合true
     */
    public boolean hasEmojiFont() {
        return emojiFont != null;
    }
}
