package jp.moyashi.phoneos.core.service.clipboard;

import processing.core.PImage;

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
     * 画像をクリップボードにコピーする。
     *
     * @param image コピーする画像
     */
    void copyImage(PImage image);

    /**
     * ラベル付き画像をクリップボードにコピーする。
     *
     * @param label ラベル（データの説明）
     * @param image コピーする画像
     */
    void copyImage(String label, PImage image);

    /**
     * クリップボードから画像を取得する。
     *
     * @return 画像データ、存在しない場合はnull
     */
    PImage pasteImage();

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
