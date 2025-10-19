package jp.moyashi.phoneos.core.apps.htmlcalculator;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;

/**
 * HTML/CSS/JavaScript で実装された電卓アプリケーション。
 * HTMLScreen機能のデモンストレーションとして機能する。
 */
public class CalculatorHTMLApp implements IApplication {

    @Override
    public String getApplicationId() {
        return "jp.moyashi.phoneos.core.apps.htmlcalculator";
    }

    @Override
    public String getName() {
        return "HTML電卓";
    }

    @Override
    public String getDescription() {
        return "HTML/CSS/JSで作られたシンプルな電卓アプリ";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    public String getAuthor() {
        return "MochiOS Team";
    }

    @Override
    public void onInitialize(Kernel kernel) {
        System.out.println("CalculatorHTMLApp: Initialized");
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new CalculatorHTMLScreen(kernel);
    }
}
