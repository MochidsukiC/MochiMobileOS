package jp.moyashi.phoneos.core.apps.chromiumbrowser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;

/**
 * Chromiumブラウザアプリケーション。
 * JCEF (Java Chromium Embedded Framework) を使用した完全機能のWebブラウザ。
 *
 * 機能:
 * - http/https/httpm プロトコルをサポート
 * - モバイル最適化UI（下部ナビゲーション、タブ管理）
 * - Cookie、キャッシュ、ブックマーク、履歴管理
 * - httpm:プロトコルで仮想ネットワークにアクセス
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class ChromiumBrowserApp implements IApplication {

    @Override
    public String getApplicationId() {
        return "jp.moyashi.phoneos.core.apps.chromiumbrowser";
    }

    @Override
    public String getName() {
        return "Chromiumブラウザ";
    }

    @Override
    public String getDescription() {
        return "Chromiumベースの高機能ブラウザ。http/https/httpm対応、タブ、ブックマーク、履歴機能";
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
            kernel.getLogger().info("ChromiumBrowserApp", "Chromiumブラウザアプリを初期化しました");
        }
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new ChromiumBrowserScreen(kernel);
    }
}
