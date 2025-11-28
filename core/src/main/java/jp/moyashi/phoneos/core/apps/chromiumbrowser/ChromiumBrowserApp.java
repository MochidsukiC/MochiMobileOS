package jp.moyashi.phoneos.core.apps.chromiumbrowser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PImage;

public class ChromiumBrowserApp implements IApplication {

    private Kernel kernel;
    private PImage icon;

    @Override
    public String getApplicationId() {
        return "jp.moyashi.phoneos.core.apps.chromiumbrowser";
    }

    @Override
    public String getName() {
        return "Browser";
    }

    @Override
    public String getDescription() {
        return "A web browser based on Chromium.";
    }

    @Override
    public PImage getIcon() {
        return icon;
    }

    @Override
    public void onInitialize(Kernel kernel) {
        this.kernel = kernel;
        // In a real scenario, we would load an icon from resources
        // icon = kernel.getVFS().loadImage("icons/browser.png");
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new ChromiumBrowserScreen(kernel);
    }
}