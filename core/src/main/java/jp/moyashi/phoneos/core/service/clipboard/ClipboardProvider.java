package jp.moyashi.phoneos.core.service.clipboard;

import processing.core.PImage;

/**
 * クリップボードへのアクセスを提供するインターフェース。
 * 環境（standalone、Forge等）ごとに異なる実装を提供できる。
 */
public interface ClipboardProvider {

    /**
     * テキストをクリップボードにコピーする。
     *
     * @param text コピーするテキスト
     * @return 成功した場合true
     */
    boolean setText(String text);

    /**
     * クリップボードからテキストを取得する。
     *
     * @return テキストデータ、存在しない場合はnull
     */
    String getText();

    /**
     * クリップボードにテキストデータが存在するか確認する。
     *
     * @return テキストデータが存在する場合true
     */
    boolean hasText();

    /**
     * 画像をクリップボードにコピーする。
     *
     * @param image コピーする画像
     * @return 成功した場合true
     */
    boolean setImage(PImage image);

    /**
     * クリップボードから画像を取得する。
     *
     * @return 画像データ、存在しない場合はnull
     */
    PImage getImage();

    /**
     * クリップボードに画像データが存在するか確認する。
     *
     * @return 画像データが存在する場合true
     */
    boolean hasImage();

    /**
     * クリップボードの内容をクリアする。
     */
    void clear();

    /**
     * このプロバイダーが利用可能かどうかを確認する。
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();
}
