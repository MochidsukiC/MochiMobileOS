package jp.moyashi.phoneos.api.intent;

import java.util.*;

/**
 * アプリケーション間の連携を実現するためのIntentクラス。
 * Androidのインテントシステムに倣った設計。
 */
public class Intent {

    // 標準アクション定義
    public static final String ACTION_VIEW = "android.intent.action.VIEW";
    public static final String ACTION_EDIT = "android.intent.action.EDIT";
    public static final String ACTION_PICK = "android.intent.action.PICK";
    public static final String ACTION_SEND = "android.intent.action.SEND";
    public static final String ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE";
    public static final String ACTION_MAIN = "android.intent.action.MAIN";
    public static final String ACTION_SEARCH = "android.intent.action.SEARCH";
    public static final String ACTION_WEB_SEARCH = "android.intent.action.WEB_SEARCH";
    public static final String ACTION_DIAL = "android.intent.action.DIAL";
    public static final String ACTION_CALL = "android.intent.action.CALL";
    public static final String ACTION_SENDTO = "android.intent.action.SENDTO";
    public static final String ACTION_SETTINGS = "android.intent.action.SETTINGS";

    // 標準カテゴリ定義
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";
    public static final String CATEGORY_DEFAULT = "android.intent.category.DEFAULT";
    public static final String CATEGORY_BROWSABLE = "android.intent.category.BROWSABLE";
    public static final String CATEGORY_ALTERNATIVE = "android.intent.category.ALTERNATIVE";

    private String action;
    private String data;
    private String type;
    private Set<String> categories;
    private Map<String, Object> extras;
    private String component;

    public Intent() {
        this.categories = new HashSet<>();
        this.extras = new HashMap<>();
    }

    public Intent(String action) {
        this();
        this.action = action;
    }

    public Intent(String action, String data) {
        this(action);
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public Intent setAction(String action) {
        this.action = action;
        return this;
    }

    public String getData() {
        return data;
    }

    public Intent setData(String data) {
        this.data = data;
        return this;
    }

    public String getType() {
        return type;
    }

    public Intent setType(String type) {
        this.type = type;
        return this;
    }

    public Set<String> getCategories() {
        return new HashSet<>(categories);
    }

    public Intent addCategory(String category) {
        this.categories.add(category);
        return this;
    }

    public boolean hasCategory(String category) {
        return categories.contains(category);
    }

    public Object getExtra(String key) {
        return extras.get(key);
    }

    public String getStringExtra(String key, String defaultValue) {
        Object value = extras.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    public int getIntExtra(String key, int defaultValue) {
        Object value = extras.get(key);
        return value instanceof Integer ? (Integer) value : defaultValue;
    }

    public boolean getBooleanExtra(String key, boolean defaultValue) {
        Object value = extras.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    public Intent putExtra(String key, Object value) {
        this.extras.put(key, value);
        return this;
    }

    public Map<String, Object> getExtras() {
        return new HashMap<>(extras);
    }

    public String getComponent() {
        return component;
    }

    public Intent setComponent(String component) {
        this.component = component;
        return this;
    }

    public boolean isImplicit() {
        return component == null || component.isEmpty();
    }

    public boolean isExplicit() {
        return !isImplicit();
    }

    public String getScheme() {
        if (data == null || data.isEmpty()) {
            return null;
        }
        int colonIndex = data.indexOf(':');
        if (colonIndex > 0) {
            return data.substring(0, colonIndex);
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Intent{");
        if (action != null) {
            sb.append("action=").append(action);
        }
        if (data != null) {
            if (sb.length() > 7) sb.append(", ");
            sb.append("data=").append(data);
        }
        if (type != null) {
            if (sb.length() > 7) sb.append(", ");
            sb.append("type=").append(type);
        }
        if (!categories.isEmpty()) {
            if (sb.length() > 7) sb.append(", ");
            sb.append("categories=").append(categories);
        }
        if (component != null) {
            if (sb.length() > 7) sb.append(", ");
            sb.append("component=").append(component);
        }
        sb.append("}");
        return sb.toString();
    }
}
