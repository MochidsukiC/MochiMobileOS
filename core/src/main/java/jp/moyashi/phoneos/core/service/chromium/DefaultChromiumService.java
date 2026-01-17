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
import java.util.concurrent.atomic.AtomicLong;
import java.util.ArrayList;
import java.util.List;

/**
 * クロミウム統合の標準実装。既存の {@link ChromiumManager} と連携し、
 * サービスとしてのライフサイクル管理や入力ディスパッチ、バックグラウンド
 * ポーリング（doMessageLoopWork）を提供する。
 */
public class DefaultChromiumService implements ChromiumService {

    /** サーフェスID生成用のカウンター（衝突防止） */
    private static final AtomicLong surfaceIdCounter = new AtomicLong(0);

    private final ChromiumProvider provider;
    private final Map<String, DefaultChromiumSurface> surfaces = new ConcurrentHashMap<>();
    private String activeSurfaceId;

    private Kernel kernel;
    private ChromiumManager manager;
    private ScheduledExecutorService pumpExecutor;
    private BrowserDataManager browserDataManager;

    public DefaultChromiumService(ChromiumProvider provider) {
        this.provider = provider;
    }

    @Override
    public synchronized void initialize(Kernel kernel) throws Exception {
        this.kernel = kernel;
        this.manager = new ChromiumManager(kernel);
        manager.setProvider(provider);
        manager.initialize();
        
        this.browserDataManager = new BrowserDataManager(kernel);

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
        }, 0, 8, TimeUnit.MILLISECONDS); // 8ms間隔（約120Hz）でCPU負荷を軽減
    }

    @Override
    public ChromiumSurface createTab(int width, int height, String initialUrl, String appId) {
        log("createTab called: " + width + "x" + height + ", URL: " + initialUrl + ", AppID: " + appId);

        if (manager == null) {
            log("ERROR: manager is null!");
            throw new IllegalStateException("ChromiumService is not initialized");
        }

        if (!manager.isInitialized()) {
            log("ERROR: ChromiumManager is not initialized!");
            throw new IllegalStateException("ChromiumManager is not initialized");
        }

        String surfaceId = "browser_tab_" + surfaceIdCounter.incrementAndGet();
        log("Creating surface with ID: " + surfaceId);

        try {
            DefaultChromiumSurface surface = surfaces.computeIfAbsent(surfaceId, id -> {
                log("computeIfAbsent: creating browser for " + id);
                ChromiumBrowser browser = manager.createBrowser(initialUrl, width, height, appId);
                log("computeIfAbsent: browser created");

                browser.addLoadListener(new ChromiumBrowser.LoadListener() {
                    @Override
                    public void onLoadStart(String url) {
                        // Do nothing
                    }

                    @Override
                    public void onLoadEnd(String url, String title, int httpStatusCode) {
                        if (browserDataManager != null) {
                            browserDataManager.addToHistory(title, url);
                        }
                    }
                });

                log("computeIfAbsent: returning new surface (appId=" + appId + ")");
                return new DefaultChromiumSurface(id, browser, appId);
            });

            log("Surface created, total surfaces: " + surfaces.size());
            activeSurfaceId = surfaceId;
            return surface;
        } catch (Exception e) {
            log("ERROR in createTab: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    private void log(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().info("DefaultChromiumService", message);
        }
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

    @Override
    public BrowserDataManager getBrowserDataManager() {
        return browserDataManager;
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
        private final String appId;

        private DefaultChromiumSurface(String id, ChromiumBrowser browser, String appId) {
            this.id = id;
            this.browser = browser;
            this.appId = appId;
        }

        ChromiumBrowser getBrowser() {
            return browser;
        }

        @Override
        public String getSurfaceId() {
            return id;
        }

        @Override
        public String getAppId() {
            return appId;
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
        public void loadContent(String html, String baseUrl) {
            browser.loadContent(html, baseUrl);
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
        public void sendMouseDragged(int x, int y, int button) {
            browser.sendMouseDragged(x, y, button);
        }

        @Override
        public void sendMouseWheel(int x, int y, float delta) {
            browser.sendMouseWheel(x, y, delta);
        }

        @Override
        public void sendKeyPressed(int keyCode, char keyChar, boolean shiftPressed, boolean ctrlPressed, boolean altPressed, boolean metaPressed) {
            browser.sendKeyPressed(keyCode, keyChar, shiftPressed, ctrlPressed, altPressed, metaPressed);
        }

        @Override
        public void sendKeyReleased(int keyCode, char keyChar, boolean shiftPressed, boolean ctrlPressed, boolean altPressed, boolean metaPressed) {
            browser.sendKeyReleased(keyCode, keyChar, shiftPressed, ctrlPressed, altPressed, metaPressed);
        }

        @Override
        public PImage acquireFrame() {
            return browser.getUpdatedImage();
        }

        @Override
        public void dispose() {
            browser.dispose();
        }

        @Override
        public boolean hasTextInputFocus() {
            return browser.hasTextInputFocus();
        }

        @Override
        public String getCachedSelectedText() {
            return browser.getCachedSelectedText();
        }

        @Override
        public void executeScript(String script) {
            browser.executeScript(script);
        }

        @Override
        public boolean isReadyToRender() {
            return browser.isReadyToRender();
        }

        @Override
        public boolean isLoading() {
            return browser.isLoading();
        }

        @Override
        public boolean isMCEF() {
            return browser.isMCEF();
        }

        @Override
        public void injectMediaDetectionScript() {
            browser.injectMediaDetectionScript();
        }

        @Override
        public boolean isMediaPlaying() {
            return browser.isMediaPlaying();
        }

        @Override
        public String getMediaTitle() {
            return browser.getMediaTitle();
        }

        @Override
        public String getMediaArtist() {
            return browser.getMediaArtist();
        }

        @Override
        public double getMediaDuration() {
            return browser.getMediaDuration();
        }

        @Override
        public double getMediaCurrentTime() {
            return browser.getMediaCurrentTime();
        }

        @Override
        public void resetMediaDetection() {
            browser.resetMediaDetection();
        }
    }

    private static class PumpThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "chromium-pump");
            thread.setDaemon(true);
            // 通常優先度に設定（CPU starvation防止）
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        }
    }
}
