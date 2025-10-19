package jp.moyashi.phoneos.core.service.intent;

import java.util.*;

/**
 * IntentFilterクラス。
 * アプリケーションが処理できるIntentのパターンを定義する。
 *
 * <p>IntentFilterは以下の条件でマッチングを行います：</p>
 * <ul>
 *   <li>アクション - 少なくとも1つのアクションがマッチする必要がある</li>
 *   <li>カテゴリ - Intentのすべてのカテゴリがフィルタに含まれる必要がある</li>
 *   <li>データスキーム - データURIのスキームがマッチする必要がある</li>
 *   <li>MIMEタイプ - MIMEタイプがマッチする必要がある（オプション）</li>
 * </ul>
 */
public class IntentFilter {

    /** 処理できるアクションのセット */
    private final Set<String> actions;

    /** 処理できるカテゴリのセット */
    private final Set<String> categories;

    /** 処理できるデータスキームのセット（例: "http", "https", "mailto"） */
    private final Set<String> dataSchemes;

    /** 処理できるMIMEタイプのセット（例: "text/plain", "image/*"） */
    private final Set<String> mimeTypes;

    /** マッチング優先度（高いほど優先） */
    private int priority;

    /**
     * 空のIntentFilterを作成する。
     */
    public IntentFilter() {
        this.actions = new HashSet<>();
        this.categories = new HashSet<>();
        this.dataSchemes = new HashSet<>();
        this.mimeTypes = new HashSet<>();
        this.priority = 0;
    }

    /**
     * アクションを指定してIntentFilterを作成する。
     *
     * @param action アクション
     */
    public IntentFilter(String action) {
        this();
        addAction(action);
    }

    /**
     * アクションを追加する。
     *
     * @param action アクション
     * @return このIntentFilterインスタンス（メソッドチェーン用）
     */
    public IntentFilter addAction(String action) {
        this.actions.add(action);
        return this;
    }

    /**
     * カテゴリを追加する。
     *
     * @param category カテゴリ
     * @return このIntentFilterインスタンス（メソッドチェーン用）
     */
    public IntentFilter addCategory(String category) {
        this.categories.add(category);
        return this;
    }

    /**
     * データスキームを追加する。
     *
     * @param scheme スキーム（例: "http", "https", "mailto"）
     * @return このIntentFilterインスタンス（メソッドチェーン用）
     */
    public IntentFilter addDataScheme(String scheme) {
        this.dataSchemes.add(scheme);
        return this;
    }

    /**
     * MIMEタイプを追加する。
     *
     * @param mimeType MIMEタイプ（例: "text/plain", "image/*"）
     * @return このIntentFilterインスタンス（メソッドチェーン用）
     */
    public IntentFilter addMimeType(String mimeType) {
        this.mimeTypes.add(mimeType);
        return this;
    }

    /**
     * 優先度を設定する。
     *
     * @param priority 優先度（高いほど優先）
     * @return このIntentFilterインスタンス（メソッドチェーン用）
     */
    public IntentFilter setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * 優先度を取得する。
     *
     * @return 優先度
     */
    public int getPriority() {
        return priority;
    }

    /**
     * アクションのセットを取得する。
     *
     * @return アクションのセット
     */
    public Set<String> getActions() {
        return new HashSet<>(actions);
    }

    /**
     * カテゴリのセットを取得する。
     *
     * @return カテゴリのセット
     */
    public Set<String> getCategories() {
        return new HashSet<>(categories);
    }

    /**
     * データスキームのセットを取得する。
     *
     * @return データスキームのセット
     */
    public Set<String> getDataSchemes() {
        return new HashSet<>(dataSchemes);
    }

    /**
     * MIMEタイプのセットを取得する。
     *
     * @return MIMEタイプのセット
     */
    public Set<String> getMimeTypes() {
        return new HashSet<>(mimeTypes);
    }

    /**
     * 指定されたIntentがこのフィルタにマッチするかチェックする。
     *
     * @param intent チェックするIntent
     * @return マッチする場合true
     */
    public boolean matches(Intent intent) {
        // 1. アクションのチェック
        if (!matchAction(intent.getAction())) {
            return false;
        }

        // 2. カテゴリのチェック
        if (!matchCategories(intent.getCategories())) {
            return false;
        }

        // 3. データスキームのチェック
        if (!matchDataScheme(intent.getScheme())) {
            return false;
        }

        // 4. MIMEタイプのチェック
        if (!matchMimeType(intent.getType())) {
            return false;
        }

        return true;
    }

    /**
     * アクションがマッチするかチェックする。
     *
     * @param action チェックするアクション
     * @return マッチする場合true
     */
    private boolean matchAction(String action) {
        // アクションが指定されていない場合は、フィルタにアクションがなければマッチ
        if (action == null || action.isEmpty()) {
            return actions.isEmpty();
        }

        // フィルタにアクションが含まれているかチェック
        return actions.contains(action);
    }

    /**
     * カテゴリがマッチするかチェックする。
     *
     * @param intentCategories チェックするカテゴリのセット
     * @return マッチする場合true
     */
    private boolean matchCategories(Set<String> intentCategories) {
        // Intentのすべてのカテゴリがフィルタに含まれている必要がある
        for (String category : intentCategories) {
            if (!categories.contains(category) && !Intent.CATEGORY_DEFAULT.equals(category)) {
                return false;
            }
        }
        return true;
    }

    /**
     * データスキームがマッチするかチェックする。
     *
     * @param scheme チェックするスキーム
     * @return マッチする場合true
     */
    private boolean matchDataScheme(String scheme) {
        // スキームが指定されていない場合は、フィルタにスキームがなければマッチ
        if (scheme == null || scheme.isEmpty()) {
            return dataSchemes.isEmpty();
        }

        // フィルタにスキームが含まれているかチェック
        return dataSchemes.isEmpty() || dataSchemes.contains(scheme);
    }

    /**
     * MIMEタイプがマッチするかチェックする。
     *
     * @param mimeType チェックするMIMEタイプ
     * @return マッチする場合true
     */
    private boolean matchMimeType(String mimeType) {
        // MIMEタイプが指定されていない場合は、フィルタにMIMEタイプがなければマッチ
        if (mimeType == null || mimeType.isEmpty()) {
            return mimeTypes.isEmpty();
        }

        // フィルタにMIMEタイプが含まれているかチェック
        if (mimeTypes.isEmpty()) {
            return true;
        }

        for (String filterType : mimeTypes) {
            if (matchMimeTypePattern(mimeType, filterType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * MIMEタイプのパターンマッチングを行う。
     * ワイルドカード（*）をサポートする。
     *
     * @param mimeType チェックするMIMEタイプ
     * @param pattern パターン（例: "text/*", "image/*", "text/plain"）
     * @return マッチする場合true
     */
    private boolean matchMimeTypePattern(String mimeType, String pattern) {
        // 完全一致
        if (pattern.equals(mimeType)) {
            return true;
        }

        // ワイルドカードチェック
        if (pattern.endsWith("/*")) {
            String patternPrefix = pattern.substring(0, pattern.length() - 2);
            String mimeTypePrefix = mimeType.contains("/") ? mimeType.substring(0, mimeType.indexOf('/')) : mimeType;
            return patternPrefix.equals(mimeTypePrefix);
        }

        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("IntentFilter{");

        if (!actions.isEmpty()) {
            sb.append("actions=").append(actions);
        }

        if (!categories.isEmpty()) {
            if (sb.length() > 13) sb.append(", ");
            sb.append("categories=").append(categories);
        }

        if (!dataSchemes.isEmpty()) {
            if (sb.length() > 13) sb.append(", ");
            sb.append("dataSchemes=").append(dataSchemes);
        }

        if (!mimeTypes.isEmpty()) {
            if (sb.length() > 13) sb.append(", ");
            sb.append("mimeTypes=").append(mimeTypes);
        }

        if (priority != 0) {
            if (sb.length() > 13) sb.append(", ");
            sb.append("priority=").append(priority);
        }

        sb.append("}");
        return sb.toString();
    }
}
