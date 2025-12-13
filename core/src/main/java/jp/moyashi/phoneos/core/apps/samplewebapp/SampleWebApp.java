package jp.moyashi.phoneos.core.apps.samplewebapp;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.WebScreen;
import processing.core.PImage;

/**
 * サンプルWebアプリケーション。
 * HTML/CSS/JSを使用したUIのデモンストレーション。
 *
 * <p>このアプリは以下の機能をデモします:</p>
 * <ul>
 *   <li>WebScreenによるHTML UIの表示</li>
 *   <li>mochios-app://スキームによるJAR内リソースの読み込み</li>
 *   <li>CSSによるスタイリング</li>
 *   <li>JavaScriptによるインタラクティブな動作</li>
 * </ul>
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class SampleWebApp implements IApplication {

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        // WebScreen.create()を使用してHTML UIをロード
        // URL: mochios-app://mochios/webapp/sample/index.html
        return WebScreen.create(kernel, "mochios", "webapp/sample/index.html");
    }

    @Override
    public String getName() {
        return "Sample WebApp";
    }

    @Override
    public String getApplicationId() {
        return "jp.moyashi.phoneos.core.apps.samplewebapp";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "HTML/CSS/JS WebApp demonstration for MochiMobileOS";
    }

    @Override
    public PImage getIcon() {
        // デフォルトアイコンを使用
        return null;
    }

    @Override
    public void onInitialize(Kernel kernel) {
        System.out.println("[SampleWebApp] Initialized");
    }

    @Override
    public void onDestroy() {
        System.out.println("[SampleWebApp] Destroyed");
    }
}
