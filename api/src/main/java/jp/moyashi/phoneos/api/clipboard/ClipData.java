package jp.moyashi.phoneos.api.clipboard;

/**
 * クリップボードデータを表すクラス。
 * テキスト、画像、HTML、URIなど、様々な種類のデータを保持できる。
 */
public class ClipData {

    /** データタイプ */
    public enum Type {
        TEXT,
        IMAGE,
        URI,
        HTML,
        INTENT
    }

    private final Type type;
    private String textData;
    private int[] imagePixels;
    private int imageWidth;
    private int imageHeight;
    private String mimeType;
    private String label;
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
     * 画像データ（ピクセル配列）を持つClipDataを作成する。
     *
     * @param label ラベル
     * @param pixels ピクセル配列（ARGB形式）
     * @param width 幅
     * @param height 高さ
     */
    public ClipData(String label, int[] pixels, int width, int height) {
        this.type = Type.IMAGE;
        this.label = label;
        this.imagePixels = pixels != null ? pixels.clone() : null;
        this.imageWidth = width;
        this.imageHeight = height;
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

    public Type getType() {
        return type;
    }

    public String getText() {
        return textData;
    }

    public int[] getImagePixels() {
        return imagePixels != null ? imagePixels.clone() : null;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getLabel() {
        return label;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isEmpty() {
        switch (type) {
            case TEXT:
            case HTML:
                return textData == null || textData.isEmpty();
            case IMAGE:
                return imagePixels == null || imagePixels.length == 0;
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
