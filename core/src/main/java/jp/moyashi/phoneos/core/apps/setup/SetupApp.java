package jp.moyashi.phoneos.core.apps.setup;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;

public class SetupApp implements IApplication {
    @Override
    public String getName() {
        return "Setup";
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new InstallationScreen(kernel);
    }

    @Override
    public String getApplicationId() {
        return "jp.moyashi.phoneos.core.apps.setup";
    }
}
