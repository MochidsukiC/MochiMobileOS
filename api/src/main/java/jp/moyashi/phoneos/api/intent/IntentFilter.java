package jp.moyashi.phoneos.api.intent;

import java.util.*;

/**
 * IntentFilterクラス。
 * アプリケーションが処理できるIntentのパターンを定義する。
 */
public class IntentFilter {

    private final Set<String> actions;
    private final Set<String> categories;
    private final Set<String> dataSchemes;
    private final Set<String> mimeTypes;
    private int priority;

    public IntentFilter() {
        this.actions = new HashSet<>();
        this.categories = new HashSet<>();
        this.dataSchemes = new HashSet<>();
        this.mimeTypes = new HashSet<>();
        this.priority = 0;
    }

    public IntentFilter(String action) {
        this();
        addAction(action);
    }

    public IntentFilter addAction(String action) {
        this.actions.add(action);
        return this;
    }

    public IntentFilter addCategory(String category) {
        this.categories.add(category);
        return this;
    }

    public IntentFilter addDataScheme(String scheme) {
        this.dataSchemes.add(scheme);
        return this;
    }

    public IntentFilter addMimeType(String mimeType) {
        this.mimeTypes.add(mimeType);
        return this;
    }

    public IntentFilter setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public int getPriority() {
        return priority;
    }

    public Set<String> getActions() {
        return new HashSet<>(actions);
    }

    public Set<String> getCategories() {
        return new HashSet<>(categories);
    }

    public Set<String> getDataSchemes() {
        return new HashSet<>(dataSchemes);
    }

    public Set<String> getMimeTypes() {
        return new HashSet<>(mimeTypes);
    }

    public boolean matches(Intent intent) {
        if (!matchAction(intent.getAction())) {
            return false;
        }
        if (!matchCategories(intent.getCategories())) {
            return false;
        }
        if (!matchDataScheme(intent.getScheme())) {
            return false;
        }
        if (!matchMimeType(intent.getType())) {
            return false;
        }
        return true;
    }

    private boolean matchAction(String action) {
        if (action == null || action.isEmpty()) {
            return actions.isEmpty();
        }
        return actions.contains(action);
    }

    private boolean matchCategories(Set<String> intentCategories) {
        for (String category : intentCategories) {
            if (!categories.contains(category) && !Intent.CATEGORY_DEFAULT.equals(category)) {
                return false;
            }
        }
        return true;
    }

    private boolean matchDataScheme(String scheme) {
        if (scheme == null || scheme.isEmpty()) {
            return dataSchemes.isEmpty();
        }
        return dataSchemes.isEmpty() || dataSchemes.contains(scheme);
    }

    private boolean matchMimeType(String mimeType) {
        if (mimeType == null || mimeType.isEmpty()) {
            return mimeTypes.isEmpty();
        }
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

    private boolean matchMimeTypePattern(String mimeType, String pattern) {
        if (pattern.equals(mimeType)) {
            return true;
        }
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
        sb.append("}");
        return sb.toString();
    }
}
