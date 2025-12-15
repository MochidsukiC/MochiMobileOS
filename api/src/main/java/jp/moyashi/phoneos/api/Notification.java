package jp.moyashi.phoneos.api;

/**
 * 通知データクラス。
 * アプリケーションからの通知を表現する。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class Notification {

    /** 通知タイトル */
    private final String title;

    /** 通知メッセージ */
    private final String message;

    /** 通知アイコン（オプション） */
    private final String iconPath;

    /** 通知の重要度 */
    private final Priority priority;

    /** タップ時のアクション（オプション） */
    private final Runnable onTap;

    /**
     * 通知の重要度。
     */
    public enum Priority {
        /** 低優先度 - サイレント通知 */
        LOW,
        /** 通常優先度 */
        NORMAL,
        /** 高優先度 - 音と振動 */
        HIGH
    }

    private Notification(Builder builder) {
        this.title = builder.title;
        this.message = builder.message;
        this.iconPath = builder.iconPath;
        this.priority = builder.priority;
        this.onTap = builder.onTap;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getIconPath() {
        return iconPath;
    }

    public Priority getPriority() {
        return priority;
    }

    public Runnable getOnTap() {
        return onTap;
    }

    /**
     * 通知ビルダー。
     */
    public static class Builder {
        private final String title;
        private final String message;
        private String iconPath;
        private Priority priority = Priority.NORMAL;
        private Runnable onTap;

        public Builder(String title, String message) {
            this.title = title;
            this.message = message;
        }

        public Builder icon(String iconPath) {
            this.iconPath = iconPath;
            return this;
        }

        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public Builder onTap(Runnable action) {
            this.onTap = action;
            return this;
        }

        public Notification build() {
            return new Notification(this);
        }
    }
}
