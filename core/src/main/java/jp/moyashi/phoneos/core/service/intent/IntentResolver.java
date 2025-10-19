package jp.moyashi.phoneos.core.service.intent;

import java.util.*;
import java.util.stream.Collectors;

/**
 * IntentResolverクラス。
 * Intentを受け取って、それを処理できるアプリケーションを検索する。
 */
public class IntentResolver {

    /** 登録されているActivityInfoのリスト */
    private final List<ActivityInfo> activities;

    /**
     * IntentResolverを作成する。
     */
    public IntentResolver() {
        this.activities = new ArrayList<>();
    }

    /**
     * アクティビティを登録する。
     *
     * @param activityInfo 登録するアクティビティ情報
     */
    public void registerActivity(ActivityInfo activityInfo) {
        // 同じアプリIDのアクティビティが既に登録されている場合は削除
        activities.removeIf(info -> info.getAppId().equals(activityInfo.getAppId()));

        // 新しいアクティビティを追加
        activities.add(activityInfo);

        System.out.println("IntentResolver: Registered activity: " + activityInfo);
    }

    /**
     * アクティビティを登録解除する。
     *
     * @param appId アプリケーションID
     */
    public void unregisterActivity(String appId) {
        boolean removed = activities.removeIf(info -> info.getAppId().equals(appId));
        if (removed) {
            System.out.println("IntentResolver: Unregistered activity: " + appId);
        }
    }

    /**
     * 指定されたIntentを処理できるアプリケーションを検索する。
     *
     * @param intent 検索するIntent
     * @return マッチするActivityInfoのリスト（優先度順にソート済み）
     */
    public List<ActivityInfo> findMatchingActivities(Intent intent) {
        // 明示的Intentの場合は、コンポーネントIDで直接検索
        if (intent.isExplicit()) {
            String componentId = intent.getComponent();
            return activities.stream()
                    .filter(info -> info.getAppId().equals(componentId))
                    .collect(Collectors.toList());
        }

        // 暗黙的Intentの場合は、IntentFilterでマッチング
        List<ActivityInfo> matchingActivities = new ArrayList<>();

        for (ActivityInfo activityInfo : activities) {
            if (activityInfo.canHandle(intent)) {
                matchingActivities.add(activityInfo);
            }
        }

        // 優先度順にソート（高い順）
        matchingActivities.sort((a, b) -> {
            int priorityA = a.getMaxPriority(intent);
            int priorityB = b.getMaxPriority(intent);
            return Integer.compare(priorityB, priorityA);
        });

        return matchingActivities;
    }

    /**
     * 指定されたIntentを処理できる最適なアプリケーションを取得する。
     *
     * @param intent 検索するIntent
     * @return 最適なActivityInfo、見つからない場合はnull
     */
    public ActivityInfo resolveIntent(Intent intent) {
        List<ActivityInfo> matchingActivities = findMatchingActivities(intent);

        if (matchingActivities.isEmpty()) {
            System.out.println("IntentResolver: No matching activity found for " + intent);
            return null;
        }

        // 最も優先度の高いアクティビティを返す
        ActivityInfo bestMatch = matchingActivities.get(0);
        System.out.println("IntentResolver: Resolved intent " + intent + " to " + bestMatch.getAppId());

        return bestMatch;
    }

    /**
     * 登録されているすべてのアクティビティを取得する。
     *
     * @return アクティビティのリスト
     */
    public List<ActivityInfo> getAllActivities() {
        return new ArrayList<>(activities);
    }

    /**
     * 指定されたアプリIDのアクティビティ情報を取得する。
     *
     * @param appId アプリケーションID
     * @return ActivityInfo、見つからない場合はnull
     */
    public ActivityInfo getActivityInfo(String appId) {
        return activities.stream()
                .filter(info -> info.getAppId().equals(appId))
                .findFirst()
                .orElse(null);
    }
}
