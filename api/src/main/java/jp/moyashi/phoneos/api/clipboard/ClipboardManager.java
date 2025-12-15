package jp.moyashi.phoneos.api.clipboard;

/**
 * ClipboardManagerインターフェース。
 * アプリケーション間でテキストや画像などのデータをコピー・ペーストする機能を提供する。
 */
public interface ClipboardManager {

    /**
     * テキストをクリップボードにコピーする。
     *
     * @param text コピーするテキスト
     */
    void copyText(String text);

    /**
     * ラベル付きテキストをクリップボードにコピーする。
     *
     * @param label ラベル（データの説明）
     * @param text コピーするテキスト
     */
    void copyText(String label, String text);

    /**
     * クリップボードからテキストを取得する。
     *
     * @return テキストデータ、存在しない場合はnull
     */
    String pasteText();

    /**
     * 画像をクリップボードにコピーする（ピクセル配列として）。
     *
     * @param pixels ピクセル配列（ARGB形式）
     * @param width 幅
     * @param height 高さ
     */
    void copyImage(int[] pixels, int width, int height);

    /**
     * ラベル付き画像をクリップボードにコピーする。
     *
     * @param label ラベル（データの説明）
     * @param pixels ピクセル配列（ARGB形式）
     * @param width 幅
     * @param height 高さ
     */
    void copyImage(String label, int[] pixels, int width, int height);

    /**
     * クリップボードから画像のピクセル配列を取得する。
     *
     * @return ピクセル配列（ARGB形式）、存在しない場合はnull
     */
    int[] pasteImagePixels();

    /**
     * クリップボードの画像の幅を取得する。
     *
     * @return 幅、画像がない場合は0
     */
    int getImageWidth();

    /**
     * クリップボードの画像の高さを取得する。
     *
     * @return 高さ、画像がない場合は0
     */
    int getImageHeight();

    /**
     * HTMLテキストをクリップボードにコピーする。
     *
     * @param label ラベル（データの説明）
     * @param html HTMLテキスト
     */
    void copyHtml(String label, String html);

    /**
     * クリップボードにテキストデータが存在するか確認する。
     *
     * @return テキストデータが存在する場合true
     */
    boolean hasText();

    /**
     * クリップボードに画像データが存在するか確認する。
     *
     * @return 画像データが存在する場合true
     */
    boolean hasImage();

    /**
     * クリップボードにHTMLデータが存在するか確認する。
     *
     * @return HTMLデータが存在する場合true
     */
    boolean hasHtml();

    /**
     * クリップボードの内容をクリアする。
     */
    void clear();

    /**
     * 現在のクリップボードデータを取得する。
     *
     * @return ClipDataインスタンス、データが存在しない場合はnull
     */
    ClipData getPrimaryClip();

    /**
     * クリップボードにデータが存在するか確認する。
     *
     * @return データが存在する場合true
     */
    boolean hasPrimaryClip();
}
