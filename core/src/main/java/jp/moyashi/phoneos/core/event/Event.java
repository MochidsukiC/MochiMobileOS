package jp.moyashi.phoneos.core.event;

import java.util.HashMap;
import java.util.Map;

/**
 * イベントバスシステムの基底イベントクラス。
 * すべてのシステムイベントはこのクラスを継承する。
 *
 * イベント駆動アーキテクチャの中核となるクラスで、
 * コンポーネント間の疎結合な通信を実現する。
 *
 * @since 2025-12-04
 * @version 1.0
 */
public abstract class Event {

    /** イベントタイプを識別するための型 */
    private final EventType type;

    /** イベント発生時刻 */
    private final long timestamp;

    /** イベント発生元 */
    private final Object source;

    /** イベントがキャンセルされたか */
    private boolean cancelled = false;

    /** イベントが消費されたか（他のリスナーに伝播しない） */
    private boolean consumed = false;

    /** イベントの追加データ */
    private final Map<String, Object> data;

    /**
     * イベントを作成する。
     *
     * @param type イベントタイプ
     * @param source イベント発生元
     */
    protected Event(EventType type, Object source) {
        this.type = type;
        this.source = source;
        this.timestamp = System.currentTimeMillis();
        this.data = new HashMap<>();
    }

    /**
     * イベントタイプを取得する。
     *
     * @return イベントタイプ
     */
    public EventType getType() {
        return type;
    }

    /**
     * イベント発生時刻を取得する。
     *
     * @return タイムスタンプ（ミリ秒）
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * イベント発生元を取得する。
     *
     * @return 発生元オブジェクト
     */
    public Object getSource() {
        return source;
    }

    /**
     * イベントをキャンセルする。
     * キャンセル可能なイベントのみ有効。
     */
    public void cancel() {
        if (isCancellable()) {
            this.cancelled = true;
        }
    }

    /**
     * イベントがキャンセルされているかを確認する。
     *
     * @return キャンセルされている場合true
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * イベントを消費する。
     * 消費されたイベントは他のリスナーに伝播しない。
     */
    public void consume() {
        this.consumed = true;
    }

    /**
     * イベントが消費されているかを確認する。
     *
     * @return 消費されている場合true
     */
    public boolean isConsumed() {
        return consumed;
    }

    /**
     * イベントがキャンセル可能かを判定する。
     * サブクラスでオーバーライドして制御する。
     *
     * @return キャンセル可能な場合true
     */
    public boolean isCancellable() {
        return false;
    }

    /**
     * 追加データを設定する。
     *
     * @param key データキー
     * @param value データ値
     */
    public void setData(String key, Object value) {
        data.put(key, value);
    }

    /**
     * 追加データを取得する。
     *
     * @param key データキー
     * @return データ値、存在しない場合null
     */
    public Object getData(String key) {
        return data.get(key);
    }

    /**
     * 追加データを型付きで取得する。
     *
     * @param key データキー
     * @param type 期待する型
     * @param <T> 型パラメータ
     * @return データ値、存在しないか型が一致しない場合null
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        Object value = data.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("%s[type=%s, source=%s, timestamp=%d, cancelled=%s, consumed=%s]",
                getClass().getSimpleName(), type, source, timestamp, cancelled, consumed);
    }
}