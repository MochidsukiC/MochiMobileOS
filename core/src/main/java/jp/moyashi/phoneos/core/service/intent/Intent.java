package jp.moyashi.phoneos.core.service.intent;

import java.util.*;

/**
 * アプリケーション間の連携を実現するためのIntentクラス。
 * Androidのインテントシステムに倣った設計。
 *
 * <p>Intentは以下の情報を保持します：</p>
 * <ul>
 *   <li>Action - 実行する操作（例: VIEW, EDIT, SHARE）</li>
 *   <li>Data - 操作対象のデータURI（例: http://example.com）</li>
 *   <li>Extras - 追加パラメータ（Key-Valueペア）</li>
 *   <li>Categories - Intentのカテゴリ（例: LAUNCHER, BROWSABLE）</li>
 *   <li>Component - 明示的に起動するアプリID（オプション）</li>
 * </ul>
 */
public class Intent {

    // =========================================================================
    // 標準アクション定義（Android互換）
    // =========================================================================

    /** データを表示するアクション */
    public static final String ACTION_VIEW = "android.intent.action.VIEW";

    /** データを編集するアクション */
    public static final String ACTION_EDIT = "android.intent.action.EDIT";

    /** データを選択するアクション */
    public static final String ACTION_PICK = "android.intent.action.PICK";

    /** データを共有するアクション */
    public static final String ACTION_SEND = "android.intent.action.SEND";

    /** 複数のデータを共有するアクション */
    public static final String ACTION_SEND_MULTIPLE = "android.intent.action.SEND_MULTIPLE";

    /** アプリのメインアクティビティを起動するアクション */
    public static final String ACTION_MAIN = "android.intent.action.MAIN";

    /** 検索を実行するアクション */
    public static final String ACTION_SEARCH = "android.intent.action.SEARCH";

    /** Webブラウザを開くアクション */
    public static final String ACTION_WEB_SEARCH = "android.intent.action.WEB_SEARCH";

    /** 電話をかけるアクション */
    public static final String ACTION_DIAL = "android.intent.action.DIAL";

    /** 電話を直接かけるアクション */
    public static final String ACTION_CALL = "android.intent.action.CALL";

    /** SMSを送信するアクション */
    public static final String ACTION_SENDTO = "android.intent.action.SENDTO";

    /** 設定を開くアクション */
    public static final String ACTION_SETTINGS = "android.intent.action.SETTINGS";

    // =========================================================================
    // 標準カテゴリ定義（Android互換）
    // =========================================================================

    /** ランチャーから起動可能なアクティビティ */
    public static final String CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER";

    /** デフォルトアクション */
    public static final String CATEGORY_DEFAULT = "android.intent.category.DEFAULT";

    /** ブラウザから起動可能なアクティビティ */
    public static final String CATEGORY_BROWSABLE = "android.intent.category.BROWSABLE";

    /** 代替アクション（ユーザーが選択可能） */
    public static final String CATEGORY_ALTERNATIVE = "android.intent.category.ALTERNATIVE";

    // =========================================================================
    // フィールド
    // =========================================================================

    /** 実行するアクション */
    private String action;

    /** データURI */
    private String data;

    /** MIMEタイプ */
    private String type;

    /** カテゴリのセット */
    private Set<String> categories;

    /** 追加パラメータ */
    private Map<String, Object> extras;

    /** 明示的に起動するコンポーネント（アプリID） */
    private String component;

    // =========================================================================
    // コンストラクタ
    // =========================================================================

    /**
     * 空のIntentを作成する。
     */
    public Intent() {
        this.categories = new HashSet<>();
        this.extras = new HashMap<>();
    }

    /**
     * アクションを指定してIntentを作成する。
     *
     * @param action アクション
     */
    public Intent(String action) {
        this();
        this.action = action;
    }

    /**
     * アクションとデータURIを指定してIntentを作成する。
     *
     * @param action アクション
     * @param data データURI
     */
    public Intent(String action, String data) {
        this(action);
        this.data = data;
    }

    // =========================================================================
    // Getter/Setter
    // =========================================================================

    /**
     * アクションを取得する。
     *
     * @return アクション
     */
    public String getAction() {
        return action;
    }

    /**
     * アクションを設定する。
     *
     * @param action アクション
     * @return このIntentインスタンス（メソッドチェーン用）
     */
    public Intent setAction(String action) {
        this.action = action;
        return this;
    }

    /**
     * データURIを取得する。
     *
     * @return データURI
     */
    public String getData() {
        return data;
    }

    /**
     * データURIを設定する。
     *
     * @param data データURI
     * @return このIntentインスタンス（メソッドチェーン用）
     */
    public Intent setData(String data) {
        this.data = data;
        return this;
    }

    /**
     * MIMEタイプを取得する。
     *
     * @return MIMEタイプ
     */
    public String getType() {
        return type;
    }

    /**
     * MIMEタイプを設定する。
     *
     * @param type MIMEタイプ
     * @return このIntentインスタンス（メソッドチェーン用）
     */
    public Intent setType(String type) {
        this.type = type;
        return this;
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
     * カテゴリを追加する。
     *
     * @param category カテゴリ
     * @return このIntentインスタンス（メソッドチェーン用）
     */
    public Intent addCategory(String category) {
        this.categories.add(category);
        return this;
    }

    /**
     * 指定されたカテゴリが含まれているかチェックする。
     *
     * @param category カテゴリ
     * @return 含まれている場合true
     */
    public boolean hasCategory(String category) {
        return categories.contains(category);
    }

    /**
     * 追加パラメータを取得する。
     *
     * @param key キー
     * @return 値、存在しない場合はnull
     */
    public Object getExtra(String key) {
        return extras.get(key);
    }

    /**
     * 文字列型の追加パラメータを取得する。
     *
     * @param key キー
     * @param defaultValue デフォルト値
     * @return 値、存在しない場合はdefaultValue
     */
    public String getStringExtra(String key, String defaultValue) {
        Object value = extras.get(key);
        return value instanceof String ? (String) value : defaultValue;
    }

    /**
     * 整数型の追加パラメータを取得する。
     *
     * @param key キー
     * @param defaultValue デフォルト値
     * @return 値、存在しない場合はdefaultValue
     */
    public int getIntExtra(String key, int defaultValue) {
        Object value = extras.get(key);
        return value instanceof Integer ? (Integer) value : defaultValue;
    }

    /**
     * ブール型の追加パラメータを取得する。
     *
     * @param key キー
     * @param defaultValue デフォルト値
     * @return 値、存在しない場合はdefaultValue
     */
    public boolean getBooleanExtra(String key, boolean defaultValue) {
        Object value = extras.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    /**
     * 追加パラメータを設定する。
     *
     * @param key キー
     * @param value 値
     * @return このIntentインスタンス（メソッドチェーン用）
     */
    public Intent putExtra(String key, Object value) {
        this.extras.put(key, value);
        return this;
    }

    /**
     * すべての追加パラメータを取得する。
     *
     * @return 追加パラメータのマップ
     */
    public Map<String, Object> getExtras() {
        return new HashMap<>(extras);
    }

    /**
     * コンポーネント（明示的起動するアプリID）を取得する。
     *
     * @return コンポーネント
     */
    public String getComponent() {
        return component;
    }

    /**
     * コンポーネント（明示的起動するアプリID）を設定する。
     *
     * @param component コンポーネント
     * @return このIntentインスタンス（メソッドチェーン用）
     */
    public Intent setComponent(String component) {
        this.component = component;
        return this;
    }

    // =========================================================================
    // ヘルパーメソッド
    // =========================================================================

    /**
     * このIntentが暗黙的Intentか（コンポーネントが指定されていないか）を判定する。
     *
     * @return 暗黙的Intentの場合true
     */
    public boolean isImplicit() {
        return component == null || component.isEmpty();
    }

    /**
     * このIntentが明示的Intentか（コンポーネントが指定されているか）を判定する。
     *
     * @return 明示的Intentの場合true
     */
    public boolean isExplicit() {
        return !isImplicit();
    }

    /**
     * データURIからスキームを抽出する。
     *
     * @return スキーム（例: "http", "mailto"）、存在しない場合はnull
     */
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

        if (!extras.isEmpty()) {
            if (sb.length() > 7) sb.append(", ");
            sb.append("extras=").append(extras.keySet());
        }

        sb.append("}");
        return sb.toString();
    }
}
