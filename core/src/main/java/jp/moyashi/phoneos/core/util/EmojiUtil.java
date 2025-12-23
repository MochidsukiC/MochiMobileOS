package jp.moyashi.phoneos.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 絵文字の検出とテキストセグメンテーションのユーティリティクラス。
 * Unicode絵文字仕様に対応したモノクロ絵文字レンダリングをサポート。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class EmojiUtil {

    /**
     * 指定されたUnicodeコードポイントが絵文字かどうかを判定する。
     * 以下の主要な絵文字ブロックをカバー:
     * - Emoticons (U+1F600-U+1F64F)
     * - Miscellaneous Symbols and Pictographs (U+1F300-U+1F5FF)
     * - Transport and Map Symbols (U+1F680-U+1F6FF)
     * - Supplemental Symbols and Pictographs (U+1F900-U+1F9FF)
     * - Symbols and Pictographs Extended-A (U+1FA00-U+1FAFF)
     * - Dingbats (U+2700-U+27BF)
     * - Miscellaneous Symbols (U+2600-U+26FF)
     * - Regional Indicator Symbols (U+1F1E0-U+1F1FF)
     *
     * @param codePoint 判定するUnicodeコードポイント
     * @return 絵文字の場合true
     */
    public static boolean isEmoji(int codePoint) {
        // Emoticons
        if (codePoint >= 0x1F600 && codePoint <= 0x1F64F) return true;

        // Miscellaneous Symbols and Pictographs
        if (codePoint >= 0x1F300 && codePoint <= 0x1F5FF) return true;

        // Transport and Map Symbols
        if (codePoint >= 0x1F680 && codePoint <= 0x1F6FF) return true;

        // Alchemical Symbols
        if (codePoint >= 0x1F700 && codePoint <= 0x1F77F) return true;

        // Geometric Shapes Extended
        if (codePoint >= 0x1F780 && codePoint <= 0x1F7FF) return true;

        // Supplemental Arrows-C
        if (codePoint >= 0x1F800 && codePoint <= 0x1F8FF) return true;

        // Supplemental Symbols and Pictographs
        if (codePoint >= 0x1F900 && codePoint <= 0x1F9FF) return true;

        // Chess Symbols
        if (codePoint >= 0x1FA00 && codePoint <= 0x1FA6F) return true;

        // Symbols and Pictographs Extended-A
        if (codePoint >= 0x1FA70 && codePoint <= 0x1FAFF) return true;

        // Dingbats
        if (codePoint >= 0x2700 && codePoint <= 0x27BF) return true;

        // Miscellaneous Symbols
        if (codePoint >= 0x2600 && codePoint <= 0x26FF) return true;

        // Miscellaneous Technical (一部の絵文字を含む)
        if (codePoint >= 0x2300 && codePoint <= 0x23FF) return true;

        // Enclosed Alphanumerics (数字の絵文字など)
        if (codePoint >= 0x2460 && codePoint <= 0x24FF) return true;

        // Box Drawing and Block Elements は除外（通常文字として扱う）

        // Stars and other symbols
        if (codePoint >= 0x2B50 && codePoint <= 0x2B55) return true;

        // Regional Indicator Symbols (国旗)
        if (codePoint >= 0x1F1E0 && codePoint <= 0x1F1FF) return true;

        // Skin tone modifiers (Fitzpatrick modifiers)
        if (codePoint >= 0x1F3FB && codePoint <= 0x1F3FF) return true;

        // Zero-width joiner (絵文字シーケンスで使用)
        if (codePoint == 0x200D) return true;

        // Variation Selector-16 (絵文字表示)
        if (codePoint == 0xFE0F) return true;

        // Keycap base characters (#, *, 0-9) - これらは単独では絵文字ではないが、
        // Variation Selector と組み合わせて絵文字になる
        // 単独では通常文字として扱う

        // Mahjong Tiles
        if (codePoint >= 0x1F000 && codePoint <= 0x1F02F) return true;

        // Domino Tiles
        if (codePoint >= 0x1F030 && codePoint <= 0x1F09F) return true;

        // Playing Cards
        if (codePoint >= 0x1F0A0 && codePoint <= 0x1F0FF) return true;

        return false;
    }

    /**
     * 文字列に絵文字が含まれているかどうかを判定する。
     *
     * @param text 判定する文字列
     * @return 絵文字が含まれている場合true
     */
    public static boolean containsEmoji(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.codePoints().anyMatch(EmojiUtil::isEmoji);
    }

    /**
     * テキストを絵文字と非絵文字のセグメントに分割する。
     *
     * @param text 分割するテキスト
     * @return TextSegmentのリスト
     */
    public static List<TextSegment> segmentText(String text) {
        List<TextSegment> segments = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return segments;
        }

        StringBuilder currentSegment = new StringBuilder();
        boolean currentIsEmoji = false;
        boolean first = true;

        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            boolean thisIsEmoji = isEmoji(codePoint);

            if (first) {
                currentIsEmoji = thisIsEmoji;
                first = false;
            }

            if (thisIsEmoji != currentIsEmoji) {
                // セグメント境界
                if (currentSegment.length() > 0) {
                    segments.add(new TextSegment(currentSegment.toString(), currentIsEmoji));
                    currentSegment = new StringBuilder();
                }
                currentIsEmoji = thisIsEmoji;
            }

            currentSegment.appendCodePoint(codePoint);
            i += Character.charCount(codePoint);
        }

        // 最後のセグメントを追加
        if (currentSegment.length() > 0) {
            segments.add(new TextSegment(currentSegment.toString(), currentIsEmoji));
        }

        return segments;
    }

    /**
     * テキストセグメントを表すクラス。
     * 絵文字/非絵文字の分類を持つ。
     */
    public static class TextSegment {
        /** セグメントのテキスト */
        public final String text;
        /** 絵文字セグメントかどうか */
        public final boolean isEmoji;

        /**
         * TextSegmentを作成する。
         *
         * @param text セグメントのテキスト
         * @param isEmoji 絵文字セグメントかどうか
         */
        public TextSegment(String text, boolean isEmoji) {
            this.text = text;
            this.isEmoji = isEmoji;
        }

        @Override
        public String toString() {
            return "TextSegment{text='" + text + "', isEmoji=" + isEmoji + "}";
        }
    }
}
