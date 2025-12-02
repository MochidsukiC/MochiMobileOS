package jp.moyashi.phoneos.core.apps.chromiumbrowser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.BrowserDataManager;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.components.Button;
import jp.moyashi.phoneos.core.ui.components.ListView;
import processing.core.PGraphics;

import java.util.List;

public class HistoryScreen implements Screen {
    private final Kernel kernel;
    private ListView listView;
    private Button backButton;
    private Button clearButton;

    public HistoryScreen(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void setup(PGraphics p) {
        backButton = new Button(10, 10, 60, 40, "Back");
        backButton.setOnClickListener(() -> kernel.getScreenManager().popScreen());

        clearButton = new Button(p.width - 70, 10, 60, 40, "Clear");
        clearButton.setOnClickListener(() -> {
            kernel.getChromiumService().getBrowserDataManager().clearHistory();
            listView.clear();
        });

        listView = new ListView(10, 60, p.width - 20, p.height - 70);
        
        BrowserDataManager dataManager = kernel.getChromiumService().getBrowserDataManager();
        List<BrowserDataManager.HistoryEntry> history = dataManager.getHistory();
        
        for (BrowserDataManager.HistoryEntry h : history) {
            String label = h.title;
            if (label.length() > 30) label = label.substring(0, 30) + "...";
            listView.addItem(label, h.url);
        }
        
        listView.setOnItemClickListener(index -> {
            String url = (String) listView.getSelectedItem().data;
            if (url != null) {
                kernel.getChromiumService().getActiveSurface().ifPresent(s -> s.loadUrl(url));
                kernel.getScreenManager().popScreen();
            }
        });
    }

    @Override
    public void draw(PGraphics g) {
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int bg = theme != null ? theme.colorBackground() : 0xFFFFFFFF;
        g.background((bg >> 16) & 0xFF, (bg >> 8) & 0xFF, bg & 0xFF);
        
        backButton.draw(g);
        clearButton.draw(g);
        listView.draw(g);
    }

    @Override
    public void mousePressed(PGraphics g, int x, int y) {
        if (backButton.onMousePressed(x, y)) return;
        if (clearButton.onMousePressed(x, y)) return;
        listView.onMousePressed(x, y);
    }

    @Override
    public void mouseReleased(PGraphics g, int x, int y) {
        backButton.onMouseReleased(x, y);
        clearButton.onMouseReleased(x, y);
    }

    @Override
    public void mouseMoved(PGraphics g, int x, int y) {
        backButton.onMouseMoved(x, y);
        clearButton.onMouseMoved(x, y);
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
    @Override public String getScreenTitle() { return "History"; }
    @Override public void cleanup(PGraphics p) {}
}
