package jp.moyashi.phoneos.core.controls;

/**
 * 外部アプリケーションが提供するコントロールセンターカードのインターフェース。
 * アプリIDとの紐付けを必須とし、アプリアンインストール時の自動削除を可能にする。
 *
 * <p>使用例:</p>
 * <pre>{@code
 * public class MyToggleCard implements IAppControlCenterItem {
 *     @Override
 *     public String getId() { return "com.example.myapp.toggle"; }
 *
 *     @Override
 *     public String getOwnerApplicationId() { return "com.example.myapp"; }
 *
 *     @Override
 *     public int getColumnSpan() { return 1; }
 *
 *     @Override
 *     public int getRowSpan() { return 1; }
 *
 *     // ... その他のメソッド実装
 * }
 * }</pre>
 */
public interface IAppControlCenterItem extends IControlCenterItem {

    /**
     * このカードを提供するアプリケーションのIDを取得する。
     * アプリがアンインストールされた場合、このIDに紐づくカードは自動的に削除される。
     *
     * @return アプリケーションID (例: "com.example.myapp")
     */
    String getOwnerApplicationId();

    /**
     * カードが属するセクションを取得する。
     * セクションはコントロールセンター内でのグループ分けに使用される。
     *
     * @return セクション (デフォルト: APP_WIDGETS)
     */
    default ControlCenterSection getSection() {
        return ControlCenterSection.APP_WIDGETS;
    }

    /**
     * このカードのデフォルト表示順序を取得する。
     * 同一セクション内での初期配置順序を決定する（低い値が先頭）。
     * ユーザーが設定で順序を変更した場合、その値が優先される。
     *
     * @return 表示順序 (0-100、デフォルト50)
     */
    default int getDefaultOrder() {
        return 50;
    }
}
