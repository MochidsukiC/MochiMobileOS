// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import org.cef.CefBrowserSettings;
import org.cef.CefClient;

/**
 * Creates a new instance of CefBrowser according the passed values.
 *
 * Forge環境用に修正されたバージョン：
 * - CefBrowserOsrNoCanvasをサポート（AWTを使用しないOSR）
 * - NoCanvasモードでAWT HeadlessExceptionを回避
 */
public class CefBrowserFactory {

    /** AWTを使わないモード（Forge環境用） */
    private static volatile boolean noCanvasMode = false;

    /**
     * NoCanvasモードを設定する。
     * trueに設定すると、OSRブラウザ作成時にCefBrowserOsrNoCanvasを使用する。
     * これによりAWT HeadlessExceptionを回避できる。
     *
     * @param enabled NoCanvasモードを有効にするかどうか
     */
    public static void setNoCanvasMode(boolean enabled) {
        noCanvasMode = enabled;
        System.out.println("[CefBrowserFactory] NoCanvas mode: " + enabled);
    }

    /**
     * NoCanvasモードが有効かどうかを取得する。
     */
    public static boolean isNoCanvasMode() {
        return noCanvasMode;
    }

    /**
     * ブラウザを作成する。
     *
     * Forge環境ではNoCanvasモードが有効な場合、CefBrowserOsrNoCanvasを使用する。
     * NoCanvasモードが無効な場合はUnsupportedOperationExceptionをスローする
     * （Forge環境ではAWT使用は禁止されているため）。
     */
    public static CefBrowser create(CefClient client, String url, boolean isOffscreenRendered,
            boolean isTransparent, CefRequestContext context, CefBrowserSettings settings) {
        if (isOffscreenRendered) {
            if (noCanvasMode) {
                // AWTを使わないブラウザを作成（Forge環境用）
                System.out.println("[CefBrowserFactory] Creating CefBrowserOsrNoCanvas (NoCanvas mode)");
                return new CefBrowserOsrNoCanvas(client, url, isTransparent, context, settings);
            } else {
                // Forge環境ではCefBrowserOsr（AWT使用）は禁止
                throw new UnsupportedOperationException(
                    "CefBrowserOsr is not available in Forge environment. " +
                    "Call CefBrowserFactory.setNoCanvasMode(true) before creating browsers.");
            }
        }
        // ウィンドウレンダリングはForge環境ではサポートしない
        throw new UnsupportedOperationException(
            "Window-rendered browser is not available in Forge environment. " +
            "Use OSR mode (isOffscreenRendered=true).");
    }

    /**
     * NoCanvasブラウザを直接作成する（NoCanvasモード設定に関係なく）。
     * ForgeChromiumProviderなど、明示的にNoCanvasブラウザが必要な場合に使用。
     */
    public static CefBrowserOsrNoCanvas createNoCanvas(CefClient client, String url,
            boolean isTransparent, CefRequestContext context, CefBrowserSettings settings) {
        System.out.println("[CefBrowserFactory] Creating CefBrowserOsrNoCanvas directly");
        return new CefBrowserOsrNoCanvas(client, url, isTransparent, context, settings);
    }
}
