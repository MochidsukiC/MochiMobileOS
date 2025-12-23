package jp.moyashi.phoneos.core.media;

import processing.core.PImage;

/**
 * メディアのメタデータを保持する不変クラス。
 * タイトル、アーティスト、アルバム、再生時間、アートワーク等の情報を管理する。
 * Builderパターンで構築する。
 *
 * @since 2025-12-21
 * @version 1.0
 */
public class MediaMetadata {

    private final String title;
    private final String artist;
    private final String album;
    private final long durationMs;
    private final PImage artwork;
    private final String mediaId;
    private final String mediaType;

    /**
     * プライベートコンストラクタ。Builderを通じてのみ構築可能。
     */
    private MediaMetadata(Builder builder) {
        this.title = builder.title;
        this.artist = builder.artist;
        this.album = builder.album;
        this.durationMs = builder.durationMs;
        this.artwork = builder.artwork;
        this.mediaId = builder.mediaId;
        this.mediaType = builder.mediaType;
    }

    /**
     * メディアのタイトルを取得する。
     *
     * @return タイトル（曲名、動画タイトル等）
     */
    public String getTitle() {
        return title;
    }

    /**
     * アーティスト名を取得する。
     *
     * @return アーティスト名
     */
    public String getArtist() {
        return artist;
    }

    /**
     * アルバム名を取得する。
     *
     * @return アルバム名
     */
    public String getAlbum() {
        return album;
    }

    /**
     * 再生時間をミリ秒で取得する。
     *
     * @return 再生時間（ミリ秒）
     */
    public long getDurationMs() {
        return durationMs;
    }

    /**
     * アートワーク画像を取得する。
     *
     * @return アートワーク（ジャケット画像）、設定されていない場合はnull
     */
    public PImage getArtwork() {
        return artwork;
    }

    /**
     * メディアの一意識別子を取得する。
     *
     * @return メディアID
     */
    public String getMediaId() {
        return mediaId;
    }

    /**
     * メディアの種類を取得する。
     *
     * @return メディアタイプ（"audio", "video"等）
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * 再生時間を「MM:SS」形式の文字列で取得する。
     *
     * @return フォーマットされた再生時間
     */
    public String getFormattedDuration() {
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * 指定された位置を「MM:SS」形式の文字列で取得する。
     *
     * @param positionMs 再生位置（ミリ秒）
     * @return フォーマットされた再生位置
     */
    public static String formatPosition(long positionMs) {
        long totalSeconds = positionMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    /**
     * MediaMetadataを構築するためのBuilderクラス。
     */
    public static class Builder {
        private String title;
        private String artist;
        private String album;
        private long durationMs;
        private PImage artwork;
        private String mediaId;
        private String mediaType = "audio";

        /**
         * 新しいBuilderを作成する。
         */
        public Builder() {
        }

        /**
         * タイトルを設定する。
         *
         * @param title タイトル
         * @return このBuilderインスタンス
         */
        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        /**
         * アーティスト名を設定する。
         *
         * @param artist アーティスト名
         * @return このBuilderインスタンス
         */
        public Builder setArtist(String artist) {
            this.artist = artist;
            return this;
        }

        /**
         * アルバム名を設定する。
         *
         * @param album アルバム名
         * @return このBuilderインスタンス
         */
        public Builder setAlbum(String album) {
            this.album = album;
            return this;
        }

        /**
         * 再生時間を設定する。
         *
         * @param durationMs 再生時間（ミリ秒）
         * @return このBuilderインスタンス
         */
        public Builder setDuration(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        /**
         * アートワークを設定する。
         *
         * @param artwork アートワーク画像
         * @return このBuilderインスタンス
         */
        public Builder setArtwork(PImage artwork) {
            this.artwork = artwork;
            return this;
        }

        /**
         * メディアIDを設定する。
         *
         * @param mediaId メディアの一意識別子
         * @return このBuilderインスタンス
         */
        public Builder setMediaId(String mediaId) {
            this.mediaId = mediaId;
            return this;
        }

        /**
         * メディアタイプを設定する。
         *
         * @param mediaType メディアの種類（"audio", "video"等）
         * @return このBuilderインスタンス
         */
        public Builder setMediaType(String mediaType) {
            this.mediaType = mediaType;
            return this;
        }

        /**
         * MediaMetadataを構築する。
         *
         * @return 構築されたMediaMetadataインスタンス
         */
        public MediaMetadata build() {
            return new MediaMetadata(this);
        }
    }

    @Override
    public String toString() {
        return "MediaMetadata{" +
                "title='" + title + '\'' +
                ", artist='" + artist + '\'' +
                ", album='" + album + '\'' +
                ", durationMs=" + durationMs +
                ", mediaType='" + mediaType + '\'' +
                '}';
    }
}
