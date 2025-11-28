package jp.moyashi.phoneos.core.service.chromium;

import jp.moyashi.phoneos.core.Kernel;
import processing.core.PImage;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * クロミウム統合の標準実装。既存の {@link ChromiumManager} と連携し、
 * サービスとしてのライフサイクル管理や入力ディスパッチ、バックグラウンド
 * ポーリング（doMessageLoopWork）を提供する。
 */
public class DefaultChromiumService implements ChromiumService {

    private final ChromiumProvider provider;
    private final Map<String, DefaultChromiumSurface> surfaces = new ConcurrentHashMap<>();
    private String activeSurfaceId;

    private Kernel kernel;
    private ChromiumManager manager;
    private ScheduledExecutorService pumpExecutor;

    public DefaultChromiumService(ChromiumProvider provider) {
        this.provider = provider;
    }

    @Override
    public synchronized void initialize(Kernel kernel) throws Exception {
        this.kernel = kernel;
        this.manager = new ChromiumManager(kernel);
        manager.setProvider(provider);
        manager.initialize();

        // バックグラウンドでCEFメッセージループと入力イベント処理を実行
        // 高優先度スレッドで実行し、draw()のブロッキングを完全に回避
        pumpExecutor = Executors.newSingleThreadScheduledExecutor(new PumpThreadFactory());
        pumpExecutor.scheduleWithFixedDelay(() -> {
            try {
                // CEFメッセージループを実行
                manager.doMessageLoopWork();

                // 全てのサーフェスの入力イベントを処理
                // これによりdraw()がブロックされなくなる
                for (DefaultChromiumSurface surface : surfaces.values()) {
                    surface.getBrowser().flushInputEvents();
                }
            } catch (Throwable t) {
                if (kernel != null && kernel.getLogger() != null) {
                    kernel.getLogger().error("ChromiumService", "Failed to pump CEF loop", t);
                }
            }
        }, 0, 1, TimeUnit.MILLISECONDS);
    }

    @Override
    public ChromiumSurface createTab(int width, int height, String initialUrl) {
        if (manager == null) {
            throw new IllegalStateException("ChromiumService is not initialized");
        }

        String surfaceId = "browser_tab_" + System.currentTimeMillis();

        DefaultChromiumSurface surface = surfaces.computeIfAbsent(surfaceId, id -> {
            ChromiumBrowser browser = manager.createBrowser(initialUrl, width, height);
            return new DefaultChromiumSurface(id, browser);
        });
        
        activeSurfaceId = surfaceId;
        return surface;
    }

    @Override
    public void closeTab(String surfaceId) {
        DefaultChromiumSurface surface = surfaces.remove(surfaceId);
        if (surface != null) {
            surface.dispose();

            // If the closed tab was the active one, switch to another tab if available
            if (surfaceId.equals(activeSurfaceId)) {
                if (!surfaces.isEmpty()) {
                    activeSurfaceId = surfaces.keySet().iterator().next();
                } else {
                    activeSurfaceId = null;
                }
            }
        }
    }

    @Override
    public Optional<ChromiumSurface> findSurface(String surfaceId) {
        return Optional.ofNullable(surfaces.get(surfaceId));
    }

    @Override
    public Optional<ChromiumSurface> getActiveSurface() {
        return Optional.ofNullable(surfaces.get(activeSurfaceId));
    }

    @Override
    public void setActiveSurface(String surfaceId) {
        if (surfaces.containsKey(surfaceId)) {
            this.activeSurfaceId = surfaceId;
        }
    }

    @Override
    public void update() {
        if ((pumpExecutor == null || pumpExecutor.isShutdown()) && manager != null) {
            try {
                manager.doMessageLoopWork();
            } catch (Throwable t) {
                if (kernel != null && kernel.getLogger() != null) {
                    kernel.getLogger().error("ChromiumService", "Fallback pump failed", t);
                }
            }
        }
    }

    @Override
    public synchronized void shutdown() {
        surfaces.values().forEach(DefaultChromiumSurface::dispose);
        surfaces.clear();

        if (pumpExecutor != null) {
            pumpExecutor.shutdownNow();
            pumpExecutor = null;
        }

        if (manager != null) {
            manager.shutdown();
            manager = null;
        }
    }

    @Override
    public Collection<ChromiumSurface> getSurfaces() {
        List<ChromiumSurface> list = new ArrayList<>(surfaces.size());
        for (DefaultChromiumSurface surface : surfaces.values()) {
            list.add(surface);
        }
        return Collections.unmodifiableList(list);
    }

    /**
     * 既存のChromiumManagerインスタンスを返す。
     * 旧アーキテクチャとの互換性維持目的。
     */
    public ChromiumManager getChromiumManager() {
        return manager;
    }

    private class DefaultChromiumSurface implements ChromiumSurface {

        private final String id;
        private final ChromiumBrowser browser;

        private DefaultChromiumSurface(String id, ChromiumBrowser browser) {
            this.id = id;
            this.browser = browser;
        }

        ChromiumBrowser getBrowser() {
            return browser;
        }

        @Override
        public String getSurfaceId() {
            return id;
        }

        @Override
        public int getWidth() {
            return browser.getWidth();
        }

        @Override
        public int getHeight() {
            return browser.getHeight();
        }

        @Override
        public void resize(int width, int height) {
            browser.resize(width, height);
        }

        @Override
        public void loadUrl(String url) {
            browser.loadURL(url);
        }

        @Override
        public String getCurrentUrl() {
            return browser.getCurrentURL();
        }

        @Override
        public String getTitle() {
            return browser.getTitle();
        }

        @Override
        public void reload() {
            browser.reload();
        }

        @Override
        public void stopLoading() {
            browser.stopLoad();
        }

        @Override
        public boolean canGoBack() {
            return browser.canGoBack();
        }

        @Override
        public void goBack() {
            browser.goBack();
        }

        @Override
        public boolean canGoForward() {
            return browser.canGoForward();
        }

        @Override
        public void goForward() {
            browser.goForward();
        }

        @Override
        public void setFrameRate(int fps) {
            browser.setFrameRate(fps);
        }

        @Override
        public void sendMousePressed(int x, int y, int button) {
            browser.sendMousePressed(x, y, button);
        }

        @Override
        public void sendMouseReleased(int x, int y, int button) {
            browser.sendMouseReleased(x, y, button);
        }

        @Override
        public void sendMouseMoved(int x, int y) {
            browser.sendMouseMoved(x, y);
        }

        @Override
        public void sendMouseWheel(int x, int y, float delta) {
            browser.sendMouseWheel(x, y, delta);
        }

        @Override
        public void sendKeyPressed(int keyCode, char keyChar) {
            browser.sendKeyPressed(keyCode, keyChar);
        }

        @Override
        public void sendKeyReleased(int keyCode, char keyChar) {
            browser.sendKeyReleased(keyCode, keyChar);
        }

        @Override
        public PImage acquireFrame() {
            return browser.getUpdatedImage();
        }

        @Override
        public void dispose() {
            browser.dispose();
        }
    }

    private static class PumpThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "chromium-pump");
            thread.setDaemon(true);
            // スレッド優先度を最高に設定（CPU/GPU使用率を最大化）
            thread.setPriority(Thread.MAX_PRIORITY);
            return thread;
        }
    }
}
