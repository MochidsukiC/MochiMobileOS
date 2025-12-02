package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;

import java.util.Collection;
import java.util.Optional;

/**
 * クロミウム統合をプラットフォーム非依存に提供するサービスインタフェース。
 */
public interface ChromiumService {

    /**
     * サービスを初期化する。
     *
     * @param kernel Kernel インスタンス
     * @throws Exception 初期化に失敗した場合
     */
    void initialize(Kernel kernel) throws Exception;

    /**
     * 新しいタブ（サーフェス）を作成します。
     *
     * @param width       初期幅
     * @param height      初期高さ
     * @param initialUrl  初期表示する URL（任意）
     * @return 作成されたサーフス
     */
    ChromiumSurface createTab(int width, int height, String initialUrl);

    /**
     * タブ（サーフェス）を破棄します。
     */
    void closeTab(String surfaceId);

    /**
     * サーフスを検索します。
     */
    Optional<ChromiumSurface> findSurface(String surfaceId);

    /**
     * アクティブなサーフェスを取得します。
     */
    Optional<ChromiumSurface> getActiveSurface();

    /**
     * アクティブなサーフェスを設定します。
     */
    void setActiveSurface(String surfaceId);

    /**
     * 定期更新処理。バックグラウンドポンプを使用する実装では no-op の場合があります。
     */
    void update();

    /**
     * サービスをシャットダウンし、関連するリソースを解放します。
     */
    void shutdown();

    /**
     * 管理下のサーフス一覧を取得します。
     */
    Collection<ChromiumSurface> getSurfaces();

    /**
     * ブラウザデータマネージャー（ブックマーク、履歴）を取得します。
     *
     * @return BrowserDataManager
     */
    BrowserDataManager getBrowserDataManager();
}
