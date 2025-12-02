package jp.moyashi.phoneos.core.apps.chromiumbrowser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.components.Button;
import jp.moyashi.phoneos.core.ui.components.ListView;
import processing.core.PGraphics;

public class BrowserMenuScreen implements Screen {
    private final Kernel kernel;
    private ListView listView;
    private Button backButton;

    public BrowserMenuScreen(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void setup(PGraphics p) {
        backButton = new Button(10, 10, 60, 40, "Close");
        backButton.setOnClickListener(() -> kernel.getScreenManager().popScreen());

        listView = new ListView(10, 60, p.width - 20, p.height - 70);
        listView.addItem("Bookmarks");
        listView.addItem("History");
        listView.addItem("Share");
        
        listView.setOnItemClickListener(index -> {
            String item = listView.getSelectedItem().text;
            
            // ポップアップメニューとして機能するため、選択後は画面を閉じる
            // ただし、遷移先がある場合は閉じた直後に次の画面を開く
            // Shareの場合はここで処理して閉じる
            
            switch (item) {
                case "Bookmarks":
                    kernel.getScreenManager().popScreen();
                    kernel.getScreenManager().pushScreen(new BookmarksScreen(kernel));
                    break;
                case "History":
                    kernel.getScreenManager().popScreen();
                    kernel.getScreenManager().pushScreen(new HistoryScreen(kernel));
                    break;
                case "Share":
                    kernel.getScreenManager().popScreen();
                    kernel.getChromiumService().getActiveSurface().ifPresent(s -> {
                        String url = s.getCurrentUrl();
                        if (url != null && kernel.getClipboardManager() != null) {
                            kernel.getClipboardManager().copyText(url);
                            if (kernel.getLogger() != null) {
                                kernel.getLogger().info("ChromiumBrowser", "URL copied to clipboard: " + url);
                            }
                        }
                    });
                    break;
            }
        });
    }

    @Override
    public void draw(PGraphics g) {
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int bg = theme != null ? theme.colorBackground() : 0xFFFFFFFF;
        g.background((bg >> 16) & 0xFF, (bg >> 8) & 0xFF, bg & 0xFF);
        
        backButton.draw(g);
        listView.draw(g);
    }

    @Override
    public void mousePressed(PGraphics g, int x, int y) {
        if (backButton.onMousePressed(x, y)) return;
        listView.onMousePressed(x, y);
    }

    @Override
    public void mouseReleased(PGraphics g, int x, int y) {
        backButton.onMouseReleased(x, y);
    }

    @Override
    public void mouseMoved(PGraphics g, int x, int y) {
        backButton.onMouseMoved(x, y);
    }

    @Override
    public void mouseWheel(PGraphics g, int x, int y, float delta) {
        listView.scroll((int)(delta * 20));
    }

    @Override public void mouseDragged(PGraphics g, int x, int y) {}
    @Override public void keyPressed(PGraphics g, char key, int keyCode) {}
    public void keyReleased(PGraphics g, char key, int keyCode) {}
    @Override public void onForeground() {}
    @Override public void onBackground() {}
    @Override public String getScreenTitle() { return "Browser Menu"; }
    @Override public void cleanup(PGraphics p) {}
}
