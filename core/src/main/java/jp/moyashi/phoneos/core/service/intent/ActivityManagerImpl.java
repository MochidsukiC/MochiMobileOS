package jp.moyashi.phoneos.core.service.intent;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;

import java.util.List;

/**
 * ActivityManagerの実装クラス。
 * Intentベースのアクティビティ起動と管理を提供する。
 */
public class ActivityManagerImpl implements ActivityManager {

    private final Kernel kernel;
    private final IntentResolver intentResolver;

    /** 現在の結果受け取りコールバック */
    private ActivityResultCallback currentCallback;

    /**
     * ActivityManagerを初期化する。
     *
     * @param kernel Kernelインスタンス
     */
    public ActivityManagerImpl(Kernel kernel) {
        this.kernel = kernel;
        this.intentResolver = new IntentResolver();
    }

    @Override
    public boolean startActivity(Intent intent) {
        System.out.println("ActivityManager: Starting activity with intent: " + intent);

        // Intentを解決
        ActivityInfo activityInfo = intentResolver.resolveIntent(intent);

        if (activityInfo == null) {
            System.err.println("ActivityManager: No activity found to handle intent: " + intent);
            return false;
        }

        // アプリケーションを起動
        return launchApplication(activityInfo.getAppId(), intent);
    }

    @Override
    public boolean startActivityForResult(Intent intent, ActivityResultCallback callback) {
        System.out.println("ActivityManager: Starting activity for result with intent: " + intent);

        // コールバックを保存
        this.currentCallback = callback;

        // アクティビティを起動
        return startActivity(intent);
    }

    @Override
    public boolean startActivityWithChooser(Intent intent) {
        System.out.println("ActivityManager: Starting activity with chooser for intent: " + intent);

        // マッチするアクティビティを検索
        List<ActivityInfo> matchingActivities = intentResolver.findMatchingActivities(intent);

        if (matchingActivities.isEmpty()) {
            System.err.println("ActivityManager: No activities found to handle intent: " + intent);
            return false;
        }

        // 1つしかない場合は直接起動
        if (matchingActivities.size() == 1) {
            return launchApplication(matchingActivities.get(0).getAppId(), intent);
        }

        // 複数ある場合は選択ダイアログを表示
        showAppChooserDialog(intent, matchingActivities);

        return true;
    }

    @Override
    public void setActivityResult(int resultCode, Intent data) {
        System.out.println("ActivityManager: Activity result set - resultCode: " + resultCode);

        // コールバックが設定されている場合は呼び出す
        if (currentCallback != null) {
            currentCallback.onActivityResult(resultCode, data);
            currentCallback = null; // コールバックをクリア
        }
    }

    @Override
    public void registerActivity(ActivityInfo activityInfo) {
        intentResolver.registerActivity(activityInfo);
    }

    @Override
    public void unregisterActivity(String appId) {
        intentResolver.unregisterActivity(appId);
    }

    @Override
    public List<ActivityInfo> findMatchingActivities(Intent intent) {
        return intentResolver.findMatchingActivities(intent);
    }

    @Override
    public ActivityInfo resolveIntent(Intent intent) {
        return intentResolver.resolveIntent(intent);
    }

    /**
     * アプリケーションを起動する。
     *
     * @param appId アプリケーションID
     * @param intent Intent（追加情報の引き渡し用）
     * @return 起動に成功した場合true
     */
    private boolean launchApplication(String appId, Intent intent) {
        System.out.println("ActivityManager: Launching application: " + appId);

        try {
            // AppLoaderからアプリケーションを検索
            IApplication app = kernel.getAppLoader().getLoadedApps().stream()
                    .filter(a -> a.getApplicationId().equals(appId))
                    .findFirst()
                    .orElse(null);

            if (app == null) {
                System.err.println("ActivityManager: Application not found: " + appId);
                return false;
            }

            // アプリケーションのエントリスクリーンを取得
            Screen entryScreen = app.getEntryScreen(kernel);

            if (entryScreen == null) {
                System.err.println("ActivityManager: Failed to get entry screen for app: " + appId);
                return false;
            }

            // Intentの追加情報をスクリーンに渡す（拡張機能として）
            if (entryScreen instanceof IntentAwareScreen) {
                ((IntentAwareScreen) entryScreen).onNewIntent(intent);
            }

            // スクリーンマネージャーで画面を開く
            kernel.getScreenManager().pushScreen(entryScreen);

            System.out.println("ActivityManager: Successfully launched app: " + appId);
            return true;

        } catch (Exception e) {
            System.err.println("ActivityManager: Error launching app: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * アプリ選択ダイアログを表示する。
     *
     * @param intent Intent
     * @param matchingActivities マッチするアクティビティのリスト
     */
    private void showAppChooserDialog(Intent intent, List<ActivityInfo> matchingActivities) {
        System.out.println("ActivityManager: Showing app chooser dialog for " + matchingActivities.size() + " apps");

        // ポップアップマネージャーでリスト選択ダイアログを表示
        StringBuilder message = new StringBuilder();
        message.append("次のアプリから選択してください:\n\n");

        for (int i = 0; i < matchingActivities.size(); i++) {
            ActivityInfo info = matchingActivities.get(i);
            message.append((i + 1)).append(". ").append(info.getAppName()).append("\n");
        }

        // TODO: PopupManagerにリスト選択ダイアログを追加する必要がある
        // 現時点では最初のアプリを起動
        launchApplication(matchingActivities.get(0).getAppId(), intent);
    }

    /**
     * IntentResolver を取得する（テスト用）。
     *
     * @return IntentResolver
     */
    public IntentResolver getIntentResolver() {
        return intentResolver;
    }
}
