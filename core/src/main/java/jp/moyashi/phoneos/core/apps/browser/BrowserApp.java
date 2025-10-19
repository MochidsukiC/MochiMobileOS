package jp.moyashi.phoneos.core.apps.browser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;

/**
 * Webブラウザアプリケーション。
 * http/https/httpm プロトコルをサポートし、実際のウェブサイトと仮想ネットワークの両方にアクセスできる。
 *
 * 機能:
 * - アドレスバーでURLを入力
 * - http://, https://: 実際のウェブサイトを読み込み
 * - httpm://[IPvMAddress]/path: 仮想ネットワークからHTMLを取得
 * - 戻る/進む/更新機能
 * - 履歴管理
 */
public class BrowserApp implements IApplication {

    @Override
    public String getApplicationId() {
        return "jp.moyashi.phoneos.core.apps.browser";
    }

    @Override
    public String getName() {
        return "ブラウザ";
    }

    @Override
    public String getDescription() {
        return "Web閲覧アプリ。http/httpsと仮想ネットワーク(httpm)に対応";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    public String getAuthor() {
        return "MochiOS Team";
    }

    @Override
    public void onInitialize(Kernel kernel) {
        if (kernel.getLogger() != null) {
            kernel.getLogger().info("BrowserApp", "ブラウザアプリを初期化しました");
        }
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new BrowserScreen(kernel);
    }
}
