package jp.moyashi.phoneos.core.service.intent;

import java.util.*;

/**
 * アクティビティ情報クラス。
 * アプリケーションIDと、そのアプリケーションが処理できるIntentFilterのリストを保持する。
 */
public class ActivityInfo {

    /** アプリケーションID */
    private final String appId;

    /** アプリケーション名（表示用） */
    private final String appName;

    /** IntentFilterのリスト */
    private final List<IntentFilter> intentFilters;

    /**
     * ActivityInfoを作成する。
     *
     * @param appId アプリケーションID
     * @param appName アプリケーション名
     */
    public ActivityInfo(String appId, String appName) {
        this.appId = appId;
        this.appName = appName;
        this.intentFilters = new ArrayList<>();
    }

    /**
     * アプリケーションIDを取得する。
     *
     * @return アプリケーションID
     */
    public String getAppId() {
        return appId;
    }

    /**
     * アプリケーション名を取得する。
     *
     * @return アプリケーション名
     */
    public String getAppName() {
        return appName;
    }

    /**
     * IntentFilterのリストを取得する。
     *
     * @return IntentFilterのリスト
     */
    public List<IntentFilter> getIntentFilters() {
        return new ArrayList<>(intentFilters);
    }

    /**
     * IntentFilterを追加する。
     *
     * @param filter 追加するIntentFilter
     * @return このActivityInfoインスタンス（メソッドチェーン用）
     */
    public ActivityInfo addIntentFilter(IntentFilter filter) {
        this.intentFilters.add(filter);
        return this;
    }

    /**
     * 指定されたIntentがこのアクティビティで処理できるかチェックする。
     *
     * @param intent チェックするIntent
     * @return 処理できる場合true
     */
    public boolean canHandle(Intent intent) {
        for (IntentFilter filter : intentFilters) {
            if (filter.matches(intent)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定されたIntentにマッチするIntentFilterの最高優先度を取得する。
     *
     * @param intent チェックするIntent
     * @return 最高優先度、マッチするフィルタがない場合は-1
     */
    public int getMaxPriority(Intent intent) {
        int maxPriority = -1;

        for (IntentFilter filter : intentFilters) {
            if (filter.matches(intent)) {
                maxPriority = Math.max(maxPriority, filter.getPriority());
            }
        }

        return maxPriority;
    }

    @Override
    public String toString() {
        return "ActivityInfo{" +
                "appId='" + appId + '\'' +
                ", appName='" + appName + '\'' +
                ", intentFilters=" + intentFilters.size() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ActivityInfo that = (ActivityInfo) o;
        return Objects.equals(appId, that.appId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId);
    }
}
