package jp.moyashi.phoneos.core.service.chromium.texture;

import jp.moyashi.phoneos.core.Kernel;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.callback.CefSchemeHandlerFactory;
import org.cef.handler.CefResourceHandler;
import org.cef.network.CefRequest;

/**
 * mochitexture://スキーム用のハンドラーファクトリ。
 * リクエストごとに新しいTextureSchemeHandlerを生成する。
 */
public class TextureSchemeHandlerFactory implements CefSchemeHandlerFactory {

    private final Kernel kernel;

    public TextureSchemeHandlerFactory(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public CefResourceHandler create(CefBrowser browser, CefFrame frame, String schemeName, CefRequest request) {
        return new TextureSchemeHandler(kernel);
    }
}
