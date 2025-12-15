package jp.moyashi.phoneos.api.intent;

import java.util.*;

/**
 * アクティビティ情報クラス。
 * アプリケーションIDと、そのアプリケーションが処理できるIntentFilterのリストを保持する。
 */
public class ActivityInfo {

    private final String appId;
    private final String appName;
    private final List<IntentFilter> intentFilters;

    public ActivityInfo(String appId, String appName) {
        this.appId = appId;
        this.appName = appName;
        this.intentFilters = new ArrayList<>();
    }

    public String getAppId() {
        return appId;
    }

    public String getAppName() {
        return appName;
    }

    public List<IntentFilter> getIntentFilters() {
        return new ArrayList<>(intentFilters);
    }

    public ActivityInfo addIntentFilter(IntentFilter filter) {
        this.intentFilters.add(filter);
        return this;
    }

    public boolean canHandle(Intent intent) {
        for (IntentFilter filter : intentFilters) {
            if (filter.matches(intent)) {
                return true;
            }
        }
        return false;
    }

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
