package jp.moyashi.phoneos.core.apps.hardware_test;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;

/**
 * ハードウェアバイパスAPIのデバッグ用テストアプリケーション。
 * 各ハードウェアAPIの動作を確認するためのテスト画面を提供する。
 */
public class HardwareTestApp implements IApplication {
    private Kernel kernel;
    private HardwareTestScreen testScreen;

    @Override
    public String getApplicationId() {
        return "jp.moyashi.phoneos.core.apps.hardware_test";
    }

    @Override
    public String getName() {
        return "Hardware Test";
    }

    @Override
    public void onInitialize(Kernel kernel) {
        this.kernel = kernel;
        System.out.println("HardwareTestApp: Initialized");
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        if (testScreen == null) {
            testScreen = new HardwareTestScreen(kernel);
        }
        return testScreen;
    }

    @Override
    public void onDestroy() {
        System.out.println("HardwareTestApp: Terminated");
    }
}