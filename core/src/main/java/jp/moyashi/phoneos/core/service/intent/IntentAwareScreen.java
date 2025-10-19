package jp.moyashi.phoneos.core.service.intent;

/**
 * Intentを受け取ることができるスクリーンのためのインターフェース。
 * このインターフェースを実装したスクリーンは、ActivityManagerから起動された際に
 * Intentを受け取ることができる。
 */
public interface IntentAwareScreen {

    /**
     * 新しいIntentを受け取った時に呼び出される。
     * スクリーンはこのメソッドで、Intentから必要な情報を取得して処理を行う。
     *
     * @param intent 受け取ったIntent
     */
    void onNewIntent(Intent intent);
}
