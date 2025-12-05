package jp.moyashi.phoneos.core.event;

/**
 * イベントフィルターインターフェース。
 * 特定の条件に基づいてイベントをフィルタリングする。
 *
 * @param <T> フィルタリング対象のイベント型
 * @since 2025-12-04
 * @version 1.0
 */
@FunctionalInterface
public interface EventFilter<T extends Event> {

    /**
     * イベントを通過させるかを判定する。
     *
     * @param event 判定対象のイベント
     * @return 通過させる場合true、ブロックする場合false
     */
    boolean accept(T event);

    /**
     * 複数のフィルターをAND条件で結合する。
     *
     * @param other 結合するフィルター
     * @return 結合されたフィルター
     */
    default EventFilter<T> and(EventFilter<T> other) {
        return event -> this.accept(event) && other.accept(event);
    }

    /**
     * 複数のフィルターをOR条件で結合する。
     *
     * @param other 結合するフィルター
     * @return 結合されたフィルター
     */
    default EventFilter<T> or(EventFilter<T> other) {
        return event -> this.accept(event) || other.accept(event);
    }

    /**
     * フィルター条件を反転する。
     *
     * @return 反転されたフィルター
     */
    default EventFilter<T> negate() {
        return event -> !this.accept(event);
    }

    /**
     * イベントタイプでフィルタリングするフィルターを作成する。
     *
     * @param types 許可するイベントタイプ
     * @param <T> イベント型
     * @return イベントタイプフィルター
     */
    @SafeVarargs
    static <T extends Event> EventFilter<T> byType(EventType... types) {
        return event -> {
            for (EventType type : types) {
                if (event.getType() == type) {
                    return true;
                }
            }
            return false;
        };
    }

    /**
     * イベント発生元でフィルタリングするフィルターを作成する。
     *
     * @param source 許可する発生元
     * @param <T> イベント型
     * @return 発生元フィルター
     */
    static <T extends Event> EventFilter<T> bySource(Object source) {
        return event -> source.equals(event.getSource());
    }

    /**
     * すべてのイベントを通過させるフィルターを作成する。
     *
     * @param <T> イベント型
     * @return 全通過フィルター
     */
    static <T extends Event> EventFilter<T> acceptAll() {
        return event -> true;
    }

    /**
     * すべてのイベントをブロックするフィルターを作成する。
     *
     * @param <T> イベント型
     * @return 全ブロックフィルター
     */
    static <T extends Event> EventFilter<T> rejectAll() {
        return event -> false;
    }
}