// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;
import org.cef.callback.CefDragData;
import org.cef.handler.CefRenderHandler;
import org.cef.handler.CefScreenInfo;

import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * AWT/GLCanvasを使用しないオフスクリーンレンダリングブラウザ。
 * Minecraft Forge環境でのAWT HeadlessException問題を回避するために作成。
 *
 * このクラスはCefBrowserOsrと同様の機能を提供するが、
 * GLCanvasの代わりにint[]配列にピクセルデータを保存する。
 *
 * @author MochiOS Team
 */
public class CefBrowserOsrNoCanvas extends CefBrowser_N implements CefRenderHandler {

    /** ブラウザの矩形（ビューポート） */
    private Rectangle browserRect = new Rectangle(0, 0, 1, 1);

    /** 画面上の位置 */
    private Point screenPoint = new Point(0, 0);

    /** スケールファクター */
    private double scaleFactor = 1.0;

    /** 色深度 */
    private int depth = 32;
    private int depthPerComponent = 8;

    /** 透過フラグ */
    private boolean isTransparent;

    /** ピクセルバッファ（ARGB形式） */
    private volatile int[] pixels;

    /** ピクセルバッファの幅 */
    private volatile int pixelsWidth;

    /** ピクセルバッファの高さ */
    private volatile int pixelsHeight;

    /** ピクセル更新フラグ */
    private volatile boolean pixelsUpdated = false;

    /** onPaintリスナー */
    private CopyOnWriteArrayList<Consumer<CefPaintEvent>> onPaintListeners =
            new CopyOnWriteArrayList<>();

    /** ブラウザが作成されたかどうか */
    private volatile boolean justCreated = false;

    /**
     * コンストラクタ。
     */
    public CefBrowserOsrNoCanvas(CefClient client, String url, boolean transparent,
                                  CefRequestContext context, CefBrowserSettings settings) {
        super(client, url, context, null, null, settings);
        this.isTransparent = transparent;

        System.out.println("[CefBrowserOsrNoCanvas] Created - URL: " + url + ", Transparent: " + transparent);
    }

    /**
     * ブラウザを即時作成する。
     */
    @Override
    public void createImmediately() {
        System.out.println("[CefBrowserOsrNoCanvas] createImmediately() called");
        System.out.println("[CefBrowserOsrNoCanvas] - browserRect: " + browserRect.width + "x" + browserRect.height);
        System.out.println("[CefBrowserOsrNoCanvas] - URL: " + getUrl());
        justCreated = true;
        createBrowserIfRequired(false);
    }

    /**
     * 必要に応じてブラウザを作成する。
     * CefBrowserOsrと同様のパターンで実装。
     *
     * @param hasParent 親ウィンドウがあるかどうか（常にfalse）
     */
    private void createBrowserIfRequired(boolean hasParent) {
        // windowHandle=0（ヘッドレスモード）
        long windowHandle = 0;

        if (getNativeRef("CefBrowser") == 0) {
            if (getParentBrowser() != null) {
                // DevToolsブラウザの場合
                createDevTools(getParentBrowser(), getClient(), windowHandle, true, isTransparent,
                        null, getInspectAt());
            } else {
                // 通常のブラウザ
                createBrowser(getClient(), windowHandle, getUrl(), true, isTransparent, null,
                        getRequestContext());
            }
            System.out.println("[CefBrowserOsrNoCanvas] Browser creation initiated (windowHandle=0, osr=true)");
        } else if (justCreated) {
            // ブラウザが既に存在する場合、親変更通知を送信
            notifyAfterParentChanged();
            setFocus(true);
            justCreated = false;
        }
    }

    /**
     * 親変更後の通知を送信する。
     */
    private void notifyAfterParentChanged() {
        // OSRモードではネイティブウィンドウの再親化はないが、通知は必要
        getClient().onAfterParentChanged(this);
    }

    /**
     * UIコンポーネントを返す。
     * このブラウザはAWTコンポーネントを持たないのでnullを返す。
     */
    @Override
    public Component getUIComponent() {
        // AWTコンポーネントは使用しない
        return null;
    }

    /**
     * CefRenderHandlerを返す。
     */
    @Override
    public CefRenderHandler getRenderHandler() {
        return this;
    }

    /**
     * DevToolsブラウザを作成する。
     */
    @Override
    protected CefBrowser_N createDevToolsBrowser(CefClient client, String url,
                                                  CefRequestContext context, CefBrowser_N parent, Point inspectAt) {
        // DevToolsも同じ実装を使用
        return new CefBrowserOsrNoCanvas(client, url, isTransparent, context, null);
    }

    // ========== CefRenderHandler 実装 ==========

    /**
     * ビューの矩形を取得する。
     */
    @Override
    public Rectangle getViewRect(CefBrowser browser) {
        return browserRect;
    }

    /**
     * 画面情報を取得する。
     */
    @Override
    public boolean getScreenInfo(CefBrowser browser, CefScreenInfo screenInfo) {
        screenInfo.Set(scaleFactor, depth, depthPerComponent, false,
                browserRect.getBounds(), browserRect.getBounds());
        return true;
    }

    /**
     * 画面上の座標を取得する。
     */
    @Override
    public Point getScreenPoint(CefBrowser browser, Point viewPoint) {
        Point point = new Point(screenPoint);
        point.translate(viewPoint.x, viewPoint.y);
        return point;
    }

    /**
     * ポップアップの表示/非表示。
     */
    @Override
    public void onPopupShow(CefBrowser browser, boolean show) {
        if (!show) {
            invalidate();
        }
    }

    /**
     * ポップアップのサイズ変更。
     */
    @Override
    public void onPopupSize(CefBrowser browser, Rectangle size) {
        // ポップアップは現在サポートしない
    }

    /**
     * onPaintリスナーを追加する。
     */
    @Override
    public void addOnPaintListener(Consumer<CefPaintEvent> listener) {
        onPaintListeners.add(listener);
    }

    /**
     * onPaintリスナーを設定する（既存を置き換え）。
     */
    @Override
    public void setOnPaintListener(Consumer<CefPaintEvent> listener) {
        onPaintListeners.clear();
        onPaintListeners.add(listener);
    }

    /**
     * onPaintリスナーを削除する。
     */
    @Override
    public void removeOnPaintListener(Consumer<CefPaintEvent> listener) {
        onPaintListeners.remove(listener);
    }

    /**
     * ブラウザの描画コールバック。
     * ByteBufferからピクセルデータをint[]配列にコピーする。
     *
     * 重要: このメソッドはCEFのUIスレッドから呼ばれる。
     * ピクセルデータはBGRA形式で渡される。
     */
    @Override
    public void onPaint(CefBrowser browser, boolean popup, Rectangle[] dirtyRects,
                        ByteBuffer buffer, int width, int height) {
        if (popup) {
            // ポップアップは現在サポートしない
            return;
        }

        // ピクセルバッファを更新
        synchronized (this) {
            // バッファサイズが変わった場合は再作成
            if (pixels == null || pixelsWidth != width || pixelsHeight != height) {
                pixels = new int[width * height];
                pixelsWidth = width;
                pixelsHeight = height;
                System.out.println("[CefBrowserOsrNoCanvas] Pixel buffer created: " + width + "x" + height);
            }

            // ByteBuffer (BGRA) → int[] (ARGB) 変換
            buffer.rewind();
            for (int i = 0; i < width * height; i++) {
                // BGRA形式で読み取り
                int b = buffer.get() & 0xFF;
                int g = buffer.get() & 0xFF;
                int r = buffer.get() & 0xFF;
                int a = buffer.get() & 0xFF;

                // ARGB形式で保存
                pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
            }

            pixelsUpdated = true;
        }

        // リスナーに通知
        // 重要: bufferのpositionを0に戻す（上記ループで最後まで読んでいるため）
        if (!onPaintListeners.isEmpty()) {
            buffer.rewind();  // bufferを先頭に戻す
            CefPaintEvent paintEvent = new CefPaintEvent(browser, popup, dirtyRects, buffer, width, height);
            for (Consumer<CefPaintEvent> listener : onPaintListeners) {
                listener.accept(paintEvent);
            }
        }
    }

    /**
     * カーソル変更。
     */
    @Override
    public boolean onCursorChange(CefBrowser browser, int cursorType) {
        // カーソル変更は無視（AWTを使用しないため）
        return true;
    }

    /**
     * ドラッグ開始。
     */
    @Override
    public boolean startDragging(CefBrowser browser, CefDragData dragData, int mask, int x, int y) {
        // ドラッグは現在サポートしない
        return false;
    }

    /**
     * ドラッグカーソル更新。
     */
    @Override
    public void updateDragCursor(CefBrowser browser, int operation) {
        // ドラッグは現在サポートしない
    }

    // ========== CefBrowser抽象メソッド実装 ==========

    /**
     * スクリーンショットを作成する。
     * ピクセルバッファからBufferedImageを生成して返す。
     *
     * @param nativeResolution trueの場合はネイティブ解像度、falseの場合はスケーリング
     * @return スクリーンショット画像のFuture
     */
    @Override
    public CompletableFuture<BufferedImage> createScreenshot(boolean nativeResolution) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                if (pixels == null || pixelsWidth <= 0 || pixelsHeight <= 0) {
                    // ピクセルデータがない場合は1x1の透明画像を返す
                    BufferedImage empty = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                    return empty;
                }

                // ピクセルデータからBufferedImageを作成
                BufferedImage image = new BufferedImage(pixelsWidth, pixelsHeight, BufferedImage.TYPE_INT_ARGB);
                image.setRGB(0, 0, pixelsWidth, pixelsHeight, pixels, 0, pixelsWidth);

                if (!nativeResolution && scaleFactor != 1.0) {
                    // スケーリングが必要な場合（現在は未実装、ネイティブ解像度で返す）
                    // 将来的にはscaleFactorに基づいてリサイズ
                }

                return image;
            }
        });
    }

    // ========== カスタムメソッド ==========

    /**
     * ブラウザのサイズを設定する。
     */
    public void setSize(int width, int height) {
        if (width > 0 && height > 0) {
            System.out.println("[CefBrowserOsrNoCanvas] setSize() called: " + width + "x" + height);
            browserRect.setSize(width, height);

            // wasResized()を呼び出してCEFにサイズ変更を通知
            // これによりonPaint()が発火する
            try {
                wasResized(width, height);
                System.out.println("[CefBrowserOsrNoCanvas] wasResized() called successfully");
            } catch (Exception e) {
                System.err.println("[CefBrowserOsrNoCanvas] wasResized() failed: " + e.getMessage());
                e.printStackTrace();
            }

            // setWindowVisibility(true)を呼び出してレンダリングを有効化
            try {
                setWindowVisibility(true);
                System.out.println("[CefBrowserOsrNoCanvas] setWindowVisibility(true) called");
            } catch (Exception e) {
                System.err.println("[CefBrowserOsrNoCanvas] setWindowVisibility() failed: " + e.getMessage());
            }

            System.out.println("[CefBrowserOsrNoCanvas] Size set to: " + width + "x" + height);
        }
    }

    /**
     * ピクセルバッファを取得する。
     * 返されるピクセルはARGB形式。
     *
     * @return ピクセル配列のコピー（スレッドセーフ）
     */
    public int[] getPixels() {
        synchronized (this) {
            if (pixels != null) {
                return pixels.clone();
            }
        }
        return null;
    }

    /**
     * ピクセルバッファの幅を取得する。
     */
    public int getPixelsWidth() {
        return pixelsWidth;
    }

    /**
     * ピクセルバッファの高さを取得する。
     */
    public int getPixelsHeight() {
        return pixelsHeight;
    }

    /**
     * ピクセルが更新されたかどうかを取得し、フラグをリセットする。
     */
    public boolean consumePixelsUpdated() {
        synchronized (this) {
            boolean updated = pixelsUpdated;
            pixelsUpdated = false;
            return updated;
        }
    }

    /**
     * マウスイベントを送信する（カスタム座標版）。
     * Minecraft環境ではAWTのMouseEventを作成する必要がある。
     */
    public void sendMouseEventCustom(int x, int y, int button, int clickCount, int modifiers, int eventType) {
        // ダミーのComponentを使用してMouseEventを作成
        // AWTのMouseEventはコンポーネントが必須だが、nullでも一部動作する
        try {
            // 軽量なダミーコンポーネント
            Component dummyComponent = new java.awt.Canvas();

            MouseEvent event = new MouseEvent(
                    dummyComponent,
                    eventType,
                    System.currentTimeMillis(),
                    modifiers,
                    x, y,
                    clickCount,
                    false,  // popup trigger
                    button
            );

            sendMouseEvent(event);
        } catch (Exception e) {
            System.err.println("[CefBrowserOsrNoCanvas] Failed to send mouse event: " + e.getMessage());
        }
    }

    /**
     * マウスホイールイベントを送信する（カスタム版）。
     */
    public void sendMouseWheelEventCustom(int x, int y, double delta) {
        try {
            Component dummyComponent = new java.awt.Canvas();

            MouseWheelEvent event = new MouseWheelEvent(
                    dummyComponent,
                    MouseEvent.MOUSE_WHEEL,
                    System.currentTimeMillis(),
                    0,  // modifiers
                    x, y,
                    0,  // click count
                    false,  // popup trigger
                    MouseWheelEvent.WHEEL_UNIT_SCROLL,
                    3,  // scroll amount
                    (int) (-delta * 120)  // wheel rotation (negative = scroll up)
            );

            sendMouseWheelEvent(event);
        } catch (Exception e) {
            System.err.println("[CefBrowserOsrNoCanvas] Failed to send mouse wheel event: " + e.getMessage());
        }
    }

    /**
     * キーイベントを送信する（カスタム版）。
     */
    public void sendKeyEventCustom(char keyChar, int keyCode, int eventType) {
        try {
            Component dummyComponent = new java.awt.Canvas();

            KeyEvent event = new KeyEvent(
                    dummyComponent,
                    eventType,
                    System.currentTimeMillis(),
                    0,  // modifiers
                    keyCode,
                    keyChar
            );

            sendKeyEvent(event);
        } catch (Exception e) {
            System.err.println("[CefBrowserOsrNoCanvas] Failed to send key event: " + e.getMessage());
        }
    }
}
