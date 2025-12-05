package jp.moyashi.phoneos.core.event;

/**
 * イベントリスナーインターフェース。
 * イベントを受信して処理するコンポーネントが実装する。
 *
 * @param <T> 処理するイベントの型
 * @since 2025-12-04
 * @version 1.0
 */
@FunctionalInterface
public interface EventListener<T extends Event> {

    /**
     * イベントを処理する。
     *
     * @param event 処理するイベント
     */
    void onEvent(T event);

    /**
     * このリスナーの優先度を取得する。
     * 数値が小さいほど優先度が高い（先に実行される）。
     * デフォルトは100。
     *
     * @return 優先度
     */
    default int getPriority() {
        return 100;
    }

    /**
     * このリスナーが有効かを判定する。
     * 無効なリスナーはイベントを受信しない。
     *
     * @return 有効な場合true
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * イベントフィルターを取得する。
     * nullを返す場合、すべてのイベントを受信する。
     *
     * @return イベントフィルター、またはnull
     */
    default EventFilter<T> getFilter() {
        return null;
    }
}