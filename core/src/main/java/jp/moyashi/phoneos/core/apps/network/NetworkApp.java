package jp.moyashi.phoneos.core.apps.network;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

/**
 * ネットワークアプリ
 * メッセージ送受信とネットワークテストを行います
 */
public class NetworkApp implements IApplication {

    @Override
    public void onInitialize(Kernel kernel) {
        System.out.println("[NetworkApp] Network app initialized");
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new NetworkScreen(kernel);
    }

    // getIcon()はデフォルト実装（null返却）を使用し、システムが白いアイコンを生成

    @Override
    public String getApplicationId() {
        return "jp.moyashi.phoneos.core.apps.network";
    }

    @Override
    public String getName() {
        return "Network";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Message and network testing application";
    }
}