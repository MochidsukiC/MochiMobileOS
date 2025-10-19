package jp.moyashi.phoneos.core.service.clipboard;

import processing.core.PImage;

/**
 * クリップボードデータを表すクラス。
 * テキスト、画像、URI、Intentなど、様々な種類のデータを保持できる。
 */
public class ClipData {

    /** データタイプ */
    public enum Type {
        TEXT,           // プレーンテキスト
        IMAGE,          // 画像データ
        URI,            // URI（ファイルパス、URL等）
        HTML,           // HTMLテキスト
        INTENT          // Intentデータ
    }

    /** データタイプ */
    private final Type type;

    /** テキストデータ */
    private String textData;

    /** 画像データ */
    private PImage imageData;

    /** MIMEタイプ */
    private String mimeType;

    /** ラベル（任意、データの説明） */
    private String label;

    /** データ作成時刻 */
    private final long timestamp;

    /**
     * テキストデータを持つClipDataを作成する。
     *
     * @param label ラベル
     * @param text テキストデータ
     */
    public ClipData(String label, String text) {
        this.type = Type.TEXT;
        this.label = label;
        this.textData = text;
        this.mimeType = "text/plain";
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 画像データを持つClipDataを作成する。
     *
     * @param label ラベル
     * @param image 画像データ
     */
    public ClipData(String label, PImage image) {
        this.type = Type.IMAGE;
        this.label = label;
        this.imageData = image;
        this.mimeType = "image/png";
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * HTMLデータを持つClipDataを作成する。
     *
     * @param label ラベル
     * @param html HTMLデータ
     * @param mimeType MIMEタイプ
     */
    public ClipData(String label, String html, String mimeType) {
        if ("text/html".equals(mimeType)) {
            this.type = Type.HTML;
        } else {
            this.type = Type.TEXT;
        }
        this.label = label;
        this.textData = html;
        this.mimeType = mimeType;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * データタイプを取得する。
     *
     * @return データタイプ
     */
    public Type getType() {
        return type;
    }

    /**
     * テキストデータを取得する。
     *
     * @return テキストデータ、存在しない場合はnull
     */
    public String getText() {
        return textData;
    }

    /**
     * 画像データを取得する。
     *
     * @return 画像データ、存在しない場合はnull
     */
    public PImage getImage() {
        return imageData;
    }

    /**
     * MIMEタイプを取得する。
     *
     * @return MIMEタイプ
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * ラベルを取得する。
     *
     * @return ラベル
     */
    public String getLabel() {
        return label;
    }

    /**
     * データ作成時刻を取得する。
     *
     * @return タイムスタンプ（ミリ秒）
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * データが空かどうかを判定する。
     *
     * @return 空の場合true
     */
    public boolean isEmpty() {
        switch (type) {
            case TEXT:
            case HTML:
                return textData == null || textData.isEmpty();
            case IMAGE:
                return imageData == null;
            default:
                return true;
        }
    }

    @Override
    public String toString() {
        return "ClipData{" +
                "type=" + type +
                ", label='" + label + '\'' +
                ", mimeType='" + mimeType + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
