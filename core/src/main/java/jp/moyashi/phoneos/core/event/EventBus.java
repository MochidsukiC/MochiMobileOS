package jp.moyashi.phoneos.core.event;

import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * イベントバスシステムの中核実装。
 * Publish-Subscribeパターンを実装し、システムコンポーネント間の
 * 疎結合な通信を実現する。
 *
 * 特徴:
 * - スレッドセーフ
 * - 優先度付きリスナー
 * - イベントフィルタリング
 * - 非同期/同期イベント配信
 * - イベント履歴管理
 *
 * @since 2025-12-04
 * @version 1.0
 */
public class EventBus {

    /** ロガー */
    private static final Logger logger = Logger.getLogger(EventBus.class.getName());

    /** シングルトンインスタンス */
    private static EventBus instance;

    /** イベントタイプごとのリスナーマップ */
    private final Map<Class<?>, List<ListenerWrapper<?>>> listeners;

    /** グローバルリスナー（すべてのイベントを受信） */
    private final List<ListenerWrapper<Event>> globalListeners;

    /** イベント実行用のExecutor */
    private final ExecutorService executor;

    /** 同期イベント実行用のExecutor */
    private final ExecutorService syncExecutor;

    /** イベント履歴 */
    private final Queue<Event> eventHistory;

    /** 履歴の最大サイズ */
    private static final int MAX_HISTORY_SIZE = 100;

    /** イベントバスが有効か */
    private volatile boolean enabled = true;

    /** デバッグモード */
    private boolean debugMode = false;

    /**
     * プライベートコンストラクタ（シングルトン）。
     */
    private EventBus() {
        this.listeners = new ConcurrentHashMap<>();
        this.globalListeners = new CopyOnWriteArrayList<>();
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "EventBus-Async");
            t.setDaemon(true);
            return t;
        });
        this.syncExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "EventBus-Sync");
            t.setDaemon(true);
            return t;
        });
        this.eventHistory = new ConcurrentLinkedQueue<>();
    }

    /**
     * EventBusのシングルトンインスタンスを取得する。
     *
     * @return EventBusインスタンス
     */
    public static synchronized EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    /**
     * イベントリスナーを登録する。
     *
     * @param eventClass リスニングするイベントクラス
     * @param listener リスナー
     * @param <T> イベント型
     */
    public <T extends Event> void register(Class<T> eventClass, EventListener<T> listener) {
        register(eventClass, listener, listener.getPriority());
    }

    /**
     * イベントリスナーを優先度付きで登録する。
     *
     * @param eventClass リスニングするイベントクラス
     * @param listener リスナー
     * @param priority 優先度（小さいほど高優先）
     * @param <T> イベント型
     */
    public <T extends Event> void register(Class<T> eventClass, EventListener<T> listener, int priority) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        ListenerWrapper<T> wrapper = new ListenerWrapper<>(listener, priority);

        listeners.compute(eventClass, (key, list) -> {
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
            }
            list.add(wrapper);
            // 優先度順にソート
            list.sort(Comparator.comparingInt(ListenerWrapper::getPriority));
            return list;
        });

        if (debugMode) {
            logger.info("Registered listener for " + eventClass.getSimpleName() +
                       " with priority " + priority);
        }
    }

    /**
     * グローバルリスナーを登録する。
     *
     * @param listener すべてのイベントを受信するリスナー
     */
    public void registerGlobal(EventListener<Event> listener) {
        registerGlobal(listener, listener.getPriority());
    }

    /**
     * グローバルリスナーを優先度付きで登録する。
     *
     * @param listener すべてのイベントを受信するリスナー
     * @param priority 優先度
     */
    public void registerGlobal(EventListener<Event> listener, int priority) {
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }

        ListenerWrapper<Event> wrapper = new ListenerWrapper<>(listener, priority);
        globalListeners.add(wrapper);
        globalListeners.sort(Comparator.comparingInt(ListenerWrapper::getPriority));

        if (debugMode) {
            logger.info("Registered global listener with priority " + priority);
        }
    }

    /**
     * イベントリスナーを登録解除する。
     *
     * @param eventClass イベントクラス
     * @param listener 登録解除するリスナー
     * @param <T> イベント型
     */
    public <T extends Event> void unregister(Class<T> eventClass, EventListener<T> listener) {
        List<ListenerWrapper<?>> list = listeners.get(eventClass);
        if (list != null) {
            list.removeIf(wrapper -> wrapper.getListener().equals(listener));
            if (list.isEmpty()) {
                listeners.remove(eventClass);
            }
        }
    }

    /**
     * グローバルリスナーを登録解除する。
     *
     * @param listener 登録解除するリスナー
     */
    public void unregisterGlobal(EventListener<Event> listener) {
        globalListeners.removeIf(wrapper -> wrapper.getListener().equals(listener));
    }

    /**
     * すべてのリスナーを登録解除する。
     */
    public void unregisterAll() {
        listeners.clear();
        globalListeners.clear();
    }

    /**
     * イベントを同期的に発行する。
     * 呼び出し元のスレッドでリスナーが実行される。
     *
     * @param event 発行するイベント
     */
    public void post(Event event) {
        if (!enabled || event == null) {
            return;
        }

        // 履歴に追加
        addToHistory(event);

        if (debugMode) {
            logger.info("Posting event: " + event);
        }

        // グローバルリスナーに通知
        notifyListeners(globalListeners, event);

        // 型固有のリスナーに通知
        List<ListenerWrapper<?>> specificListeners = listeners.get(event.getClass());
        if (specificListeners != null) {
            notifyListeners(specificListeners, event);
        }

        // 親クラスのリスナーにも通知
        Class<?> superClass = event.getClass().getSuperclass();
        while (superClass != null && Event.class.isAssignableFrom(superClass)) {
            List<ListenerWrapper<?>> superListeners = listeners.get(superClass);
            if (superListeners != null) {
                notifyListeners(superListeners, event);
            }
            superClass = superClass.getSuperclass();
        }
    }

    /**
     * イベントを非同期的に発行する。
     * 別スレッドでリスナーが実行される。
     *
     * @param event 発行するイベント
     * @return 非同期実行のFuture
     */
    public CompletableFuture<Void> postAsync(Event event) {
        return CompletableFuture.runAsync(() -> post(event), executor);
    }

    /**
     * イベントを遅延して発行する。
     *
     * @param event 発行するイベント
     * @param delay 遅延時間
     * @param unit 時間単位
     * @return 遅延実行のFuture
     */
    public ScheduledFuture<?> postDelayed(Event event, long delay, TimeUnit unit) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "EventBus-Delayed");
            t.setDaemon(true);
            return t;
        });

        return scheduler.schedule(() -> {
            post(event);
            scheduler.shutdown();
        }, delay, unit);
    }

    /**
     * リスナーに通知する。
     *
     * @param wrappers リスナーラッパーのリスト
     * @param event 通知するイベント
     */
    @SuppressWarnings("unchecked")
    private void notifyListeners(List<? extends ListenerWrapper<?>> wrappers, Event event) {
        for (ListenerWrapper<?> wrapper : wrappers) {
            if (event.isConsumed()) {
                break;
            }

            EventListener listener = wrapper.getListener();

            // リスナーが無効な場合はスキップ
            if (!listener.isEnabled()) {
                continue;
            }

            // フィルターチェック
            EventFilter filter = listener.getFilter();
            if (filter != null && !filter.accept(event)) {
                continue;
            }

            try {
                ((EventListener<Event>) listener).onEvent(event);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error in event listener", e);
            }
        }
    }

    /**
     * イベント履歴に追加する。
     *
     * @param event 追加するイベント
     */
    private void addToHistory(Event event) {
        eventHistory.offer(event);
        while (eventHistory.size() > MAX_HISTORY_SIZE) {
            eventHistory.poll();
        }
    }

    /**
     * イベント履歴を取得する。
     *
     * @return イベント履歴のリスト
     */
    public List<Event> getHistory() {
        return new ArrayList<>(eventHistory);
    }

    /**
     * イベント履歴をクリアする。
     */
    public void clearHistory() {
        eventHistory.clear();
    }

    /**
     * イベントバスを有効/無効にする。
     *
     * @param enabled 有効にする場合true
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * デバッグモードを設定する。
     *
     * @param debugMode デバッグモードを有効にする場合true
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * EventBusをシャットダウンする。
     */
    public void shutdown() {
        enabled = false;
        executor.shutdown();
        syncExecutor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            if (!syncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                syncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            syncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * リスナーをラップするクラス。
     * 優先度情報を保持する。
     *
     * @param <T> イベント型
     */
    private static class ListenerWrapper<T extends Event> {
        private final EventListener<T> listener;
        private final int priority;

        public ListenerWrapper(EventListener<T> listener, int priority) {
            this.listener = listener;
            this.priority = priority;
        }

        public EventListener<T> getListener() {
            return listener;
        }

        public int getPriority() {
            return priority;
        }
    }
}