package jp.moyashi.phoneos.forge.chromium;

import com.cinemamod.mcef.MCEFClient;
import com.cinemamod.mcef.MCEFBrowser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.cef.browser.CefBrowser;
import org.cef.misc.CefCursorType;

import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MCEFのオンスクリーン描画バッファを取得するための拡張ブラウザ。
 * onPaint()コールバックで受け取ったByteBufferをリスナーへ転送し、
 * OpenGLのテクスチャを読み返さずにピクセルデータへアクセスできるようにする。
 */
public class MochiMCEFBrowser extends MCEFBrowser {

    private static final Logger LOGGER = LogManager.getLogger();
    private final AtomicBoolean firstFrameLogged = new AtomicBoolean(false);

    /**
     * フレーム更新リスナー。
     */
    public interface FrameListener {
        void onFrame(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height);
    }

    private final List<FrameListener> frameListeners = new CopyOnWriteArrayList<>();

    public MochiMCEFBrowser(MCEFClient client, String url, boolean transparent) {
        super(client, url, transparent);
        LOGGER.info("[MochiMCEFBrowser] Instantiated for URL={} transparent={}", url, transparent);
    }

    /**
     * フレーム更新リスナーを登録する。
     */
    public void addFrameListener(FrameListener listener) {
        if (listener != null) {
            frameListeners.add(listener);
            LOGGER.info("[MochiMCEFBrowser] FrameListener registered: {}", listener.getClass().getSimpleName());
        }
    }

    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height) {
        if (firstFrameLogged.compareAndSet(false, true)) {
            LOGGER.info("[MochiMCEFBrowser] onPaint first frame received: {}x{}, popup={}, dirtyCount={}", width, height, popup, dirtyRects != null ? dirtyRects.length : 0);
        }

        super.onPaint(browser, popup, dirtyRects, buffer, width, height);

        if (frameListeners.isEmpty() || buffer == null) {
            return;
        }

        // ByteBufferは呼び出し元と共有されるため、リスナー側でduplicate()等を使用して扱う
        for (FrameListener listener : frameListeners) {
            listener.onFrame(browser, popup, dirtyRects, buffer, width, height);
        }
    }

    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        CefCursorType type = CefCursorType.fromId(cursorType);
        if (type == CefCursorType.NONE) {
            return super.onCursorChange(browser, 0);
        }
        return super.onCursorChange(browser, cursorType);
    }
}
