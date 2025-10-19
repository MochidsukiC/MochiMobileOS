package jp.moyashi.phoneos.core.service.intent;

import java.util.List;

/**
 * ActivityManagerインターフェース。
 * Intentベースのアクティビティ起動と管理を提供する。
 */
public interface ActivityManager {

    /**
     * 結果を受け取るためのコールバックインターフェース。
     */
    @FunctionalInterface
    interface ActivityResultCallback {
        /**
         * アクティビティの結果が返された時に呼び出される。
         *
         * @param resultCode 結果コード
         * @param data 結果データ
         */
        void onActivityResult(int resultCode, Intent data);
    }

    /** 結果コード: 成功 */
    int RESULT_OK = -1;

    /** 結果コード: キャンセル */
    int RESULT_CANCELED = 0;

    /** 結果コード: 失敗 */
    int RESULT_ERROR = 1;

    /**
     * Intentからアクティビティを起動する。
     *
     * @param intent 起動するIntent
     * @return 起動に成功した場合true
     */
    boolean startActivity(Intent intent);

    /**
     * Intentからアクティビティを起動し、結果を受け取る。
     *
     * @param intent 起動するIntent
     * @param callback 結果を受け取るコールバック
     * @return 起動に成功した場合true
     */
    boolean startActivityForResult(Intent intent, ActivityResultCallback callback);

    /**
     * アプリケーション選択ダイアログを表示してアクティビティを起動する。
     *
     * @param intent 起動するIntent
     * @return 起動に成功した場合true
     */
    boolean startActivityWithChooser(Intent intent);

    /**
     * アクティビティの結果を設定する（アクティビティ側から呼び出される）。
     *
     * @param resultCode 結果コード
     * @param data 結果データ
     */
    void setActivityResult(int resultCode, Intent data);

    /**
     * アクティビティを登録する。
     *
     * @param activityInfo 登録するアクティビティ情報
     */
    void registerActivity(ActivityInfo activityInfo);

    /**
     * アクティビティを登録解除する。
     *
     * @param appId アプリケーションID
     */
    void unregisterActivity(String appId);

    /**
     * 指定されたIntentを処理できるアプリケーションを検索する。
     *
     * @param intent 検索するIntent
     * @return マッチするActivityInfoのリスト（優先度順）
     */
    List<ActivityInfo> findMatchingActivities(Intent intent);

    /**
     * 指定されたIntentを処理できる最適なアプリケーションを取得する。
     *
     * @param intent 検索するIntent
     * @return 最適なActivityInfo、見つからない場合はnull
     */
    ActivityInfo resolveIntent(Intent intent);
}
