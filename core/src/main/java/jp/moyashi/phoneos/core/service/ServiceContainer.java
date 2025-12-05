package jp.moyashi.phoneos.core.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * アプリケーションサービスのDIコンテナ。
 * Service Locatorパターンから依存性注入パターンへの移行を実現し、
 * カーネルパニックのリスクを軽減する。
 *
 * 機能:
 * - サービスの登録と解決
 * - シングルトンとトランジエントなライフサイクル管理
 * - 遅延初期化のサポート
 * - 型安全なサービス取得
 * - サービス間の依存関係管理
 *
 * @since 2025-12-02
 * @version 1.0
 */
public class ServiceContainer {

    /** サービスファクトリーのレジストリ */
    private final Map<Class<?>, ServiceRegistration<?>> registrations = new ConcurrentHashMap<>();

    /** 初期化済みシングルトンサービスのキャッシュ */
    private final Map<Class<?>, Object> singletonCache = new ConcurrentHashMap<>();

    /** コンテナがロックされているか（登録フェーズ終了後） */
    private volatile boolean isLocked = false;

    /**
     * サービス登録情報を保持する内部クラス。
     */
    private static class ServiceRegistration<T> {
        private final Class<T> serviceClass;
        private final Supplier<T> factory;
        private final ServiceLifetime lifetime;
        private final boolean isLazy;

        ServiceRegistration(Class<T> serviceClass, Supplier<T> factory,
                          ServiceLifetime lifetime, boolean isLazy) {
            this.serviceClass = serviceClass;
            this.factory = factory;
            this.lifetime = lifetime;
            this.isLazy = isLazy;
        }
    }

    /**
     * サービスのライフタイム定義。
     */
    public enum ServiceLifetime {
        /** アプリケーション全体で単一インスタンス */
        SINGLETON,
        /** 要求ごとに新しいインスタンス */
        TRANSIENT,
        /** リクエストスコープ（将来の拡張用） */
        SCOPED
    }

    /**
     * シングルトンサービスを登録する。
     *
     * @param <T> サービスの型
     * @param serviceClass サービスクラス
     * @param factory サービスのファクトリ関数
     * @return このコンテナインスタンス（メソッドチェーン用）
     */
    public <T> ServiceContainer registerSingleton(Class<T> serviceClass, Supplier<T> factory) {
        return register(serviceClass, factory, ServiceLifetime.SINGLETON, false);
    }

    /**
     * 遅延初期化シングルトンサービスを登録する。
     *
     * @param <T> サービスの型
     * @param serviceClass サービスクラス
     * @param factory サービスのファクトリ関数
     * @return このコンテナインスタンス
     */
    public <T> ServiceContainer registerLazySingleton(Class<T> serviceClass, Supplier<T> factory) {
        return register(serviceClass, factory, ServiceLifetime.SINGLETON, true);
    }

    /**
     * トランジエントサービスを登録する。
     *
     * @param <T> サービスの型
     * @param serviceClass サービスクラス
     * @param factory サービスのファクトリ関数
     * @return このコンテナインスタンス
     */
    public <T> ServiceContainer registerTransient(Class<T> serviceClass, Supplier<T> factory) {
        return register(serviceClass, factory, ServiceLifetime.TRANSIENT, false);
    }

    /**
     * 既存のインスタンスをシングルトンとして登録する。
     *
     * @param <T> サービスの型
     * @param serviceClass サービスクラス
     * @param instance サービスインスタンス
     * @return このコンテナインスタンス
     */
    public <T> ServiceContainer registerInstance(Class<T> serviceClass, T instance) {
        checkNotLocked();
        if (instance == null) {
            throw new IllegalArgumentException("Instance cannot be null");
        }

        registrations.put(serviceClass,
            new ServiceRegistration<>(serviceClass, () -> instance, ServiceLifetime.SINGLETON, false));
        singletonCache.put(serviceClass, instance);
        return this;
    }

    /**
     * サービスを登録する内部メソッド。
     */
    private <T> ServiceContainer register(Class<T> serviceClass, Supplier<T> factory,
                                         ServiceLifetime lifetime, boolean isLazy) {
        checkNotLocked();
        if (serviceClass == null || factory == null) {
            throw new IllegalArgumentException("Service class and factory cannot be null");
        }

        registrations.put(serviceClass,
            new ServiceRegistration<>(serviceClass, factory, lifetime, isLazy));

        // 非遅延シングルトンは即座に初期化
        if (lifetime == ServiceLifetime.SINGLETON && !isLazy) {
            try {
                T instance = factory.get();
                singletonCache.put(serviceClass, instance);
            } catch (Exception e) {
                throw new ServiceInitializationException(
                    "Failed to initialize service: " + serviceClass.getName(), e);
            }
        }

        return this;
    }

    /**
     * サービスを解決（取得）する。
     *
     * @param <T> サービスの型
     * @param serviceClass サービスクラス
     * @return サービスインスタンス
     * @throws ServiceNotFoundException サービスが登録されていない場合
     */
    @SuppressWarnings("unchecked")
    public <T> T resolve(Class<T> serviceClass) {
        ServiceRegistration<?> registration = registrations.get(serviceClass);

        if (registration == null) {
            throw new ServiceNotFoundException(
                "Service not registered: " + serviceClass.getName());
        }

        // シングルトンの場合はキャッシュを確認
        if (registration.lifetime == ServiceLifetime.SINGLETON) {
            Object cached = singletonCache.get(serviceClass);
            if (cached != null) {
                return (T) cached;
            }

            // 遅延初期化シングルトンの初期化
            synchronized (singletonCache) {
                cached = singletonCache.get(serviceClass);
                if (cached != null) {
                    return (T) cached;
                }

                try {
                    T instance = (T) registration.factory.get();
                    singletonCache.put(serviceClass, instance);
                    return instance;
                } catch (Exception e) {
                    throw new ServiceInitializationException(
                        "Failed to create service: " + serviceClass.getName(), e);
                }
            }
        }

        // トランジエントの場合は毎回新しいインスタンスを作成
        try {
            return (T) registration.factory.get();
        } catch (Exception e) {
            throw new ServiceInitializationException(
                "Failed to create transient service: " + serviceClass.getName(), e);
        }
    }

    /**
     * サービスを安全に解決する（存在しない場合はnullを返す）。
     *
     * @param <T> サービスの型
     * @param serviceClass サービスクラス
     * @return サービスインスタンス、または null
     */
    public <T> T tryResolve(Class<T> serviceClass) {
        try {
            return resolve(serviceClass);
        } catch (ServiceNotFoundException e) {
            return null;
        }
    }

    /**
     * サービスが登録されているかを確認する。
     *
     * @param serviceClass サービスクラス
     * @return 登録されている場合 true
     */
    public boolean isRegistered(Class<?> serviceClass) {
        return registrations.containsKey(serviceClass);
    }

    /**
     * コンテナをロックして新規登録を防ぐ。
     * アプリケーション起動完了後に呼び出す。
     */
    public void lock() {
        isLocked = true;
    }

    /**
     * コンテナがロックされていないことを確認する。
     */
    private void checkNotLocked() {
        if (isLocked) {
            throw new IllegalStateException(
                "ServiceContainer is locked. No new registrations allowed.");
        }
    }

    /**
     * すべての登録済みサービスをクリアする（テスト用）。
     */
    protected void clear() {
        checkNotLocked();
        registrations.clear();
        singletonCache.clear();
    }

    /**
     * 登録済みサービスの数を取得する。
     *
     * @return サービス数
     */
    public int getServiceCount() {
        return registrations.size();
    }

    /**
     * サービスが見つからない場合の例外。
     */
    public static class ServiceNotFoundException extends RuntimeException {
        public ServiceNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * サービス初期化失敗時の例外。
     */
    public static class ServiceInitializationException extends RuntimeException {
        public ServiceInitializationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}