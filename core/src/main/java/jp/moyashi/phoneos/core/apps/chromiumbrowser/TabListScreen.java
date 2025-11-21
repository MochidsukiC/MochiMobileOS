package jp.moyashi.phoneos.core.apps.chromiumbrowser;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumSurface;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.components.Button;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PApplet;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class TabListScreen implements Screen {

    private final Kernel kernel;

    private Button backButton; // Button to close TabListScreen

    // For rendering tab cards
    private static final int CARD_WIDTH = 170;
    private static final int CARD_HEIGHT = 100;
    private static final int CARD_SPACING = 10;
    private static final int COLUMNS = 2;

    public TabListScreen(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void setup(PGraphics p) {
        initializeUI(p);
    }

    private void initializeUI(PGraphics p) {
        // Back button (to close TabListScreen)
        backButton = new Button(p.width / 2f - 40, p.height - 60, 80, 40, "Close");
        backButton.setOnClickListener(() -> {
            if (kernel != null && kernel.getScreenManager() != null) {
                kernel.getScreenManager().popScreen();
            }
        });
    }

    @Override
    public void draw(PGraphics g) {
        // Draw a semi-transparent background overlay
        g.background(0, 150);

        // Draw a title
        g.fill(255);
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(24);
        g.text("Tab List", g.width / 2, 50);

        // Draw the back button
        if (backButton != null) {
            backButton.draw(g);
        }

        // Draw tab cards
        drawTabCards(g);
    }

    private void drawTabCards(PGraphics g) {
        if (kernel == null || kernel.getChromiumService() == null) return;

        Collection<ChromiumSurface> surfaces = kernel.getChromiumService().getSurfaces();
        if (surfaces.isEmpty()) {
            g.fill(255);
            g.textAlign(g.CENTER, g.CENTER);
            g.textSize(16);
            g.text("No tabs open", g.width / 2, g.height / 2);
            return;
        }

        List<ChromiumSurface> sortedSurfaces = surfaces.stream()
                .sorted((s1, s2) -> s1.getSurfaceId().compareTo(s2.getSurfaceId())) // Sort by ID for consistent order
                .collect(Collectors.toList());

        int startX = (g.width - (COLUMNS * CARD_WIDTH + (COLUMNS - 1) * CARD_SPACING)) / 2;
        int startY = 80;

        int row = 0;
        int col = 0;

        for (ChromiumSurface surface : sortedSurfaces) {
            int cardX = startX + col * (CARD_WIDTH + CARD_SPACING);
            int cardY = startY + row * (CARD_HEIGHT + CARD_SPACING);

            drawTabCard(g, surface, cardX, cardY);

            col++;
            if (col >= COLUMNS) {
                col = 0;
                row++;
            }
        }
    }

    private void drawTabCard(PGraphics g, ChromiumSurface surface, int x, int y) {
        var theme = kernel.getThemeEngine();
        int surfaceColor = theme != null ? theme.colorSurface() : 0xFFFFFFFF;
        int onSurfaceColor = theme != null ? theme.colorOnSurface() : 0xFF111111;

        // Card background
        g.fill((surfaceColor >> 16) & 0xFF, (surfaceColor >> 8) & 0xFF, surfaceColor & 0xFF, 220); // Slightly less transparent
        g.stroke(theme != null ? theme.colorBorder() : 0xFFCCCCCC);
        g.rect(x, y, CARD_WIDTH, CARD_HEIGHT, 8); // Corner radius 8

        // Thumbnail
        PImage thumbnail = surface.acquireFrame();
        if (thumbnail != null) {
            // Scale thumbnail to fit card
            float scaleX = (float) CARD_WIDTH / thumbnail.width;
            float scaleY = (float) (CARD_HEIGHT - 30) / thumbnail.height; // Leave space for title
            float scale = Math.min(scaleX, scaleY);
            
            g.pushMatrix();
            g.translate(x + CARD_WIDTH / 2f, y + (CARD_HEIGHT - 30) / 2f); // Center thumbnail
            g.imageMode(PGraphics.CENTER);
            g.image(thumbnail, 0, 0, thumbnail.width * scale, thumbnail.height * scale);
            g.imageMode(PGraphics.CORNER);
            g.popMatrix();
        } else {
            // Placeholder for thumbnail
            g.fill(theme != null ? theme.colorBackground() : 0xFFEEEEEE);
            g.rect(x + 5, y + 5, CARD_WIDTH - 10, CARD_HEIGHT - 30, 4);
        }

        // Tab title
        g.fill(onSurfaceColor);
        g.textAlign(PApplet.LEFT, PApplet.TOP);
        g.textSize(10);
        String title = surface.getTitle();
        if (title.length() > 20) {
            title = title.substring(0, 17) + "...";
        }
        g.text(title, x + 5, y + CARD_HEIGHT - 20);

        // Close button
        // Positioned at top-right corner of the card
        Button closeButton = new Button(x + CARD_WIDTH - 25, y + 5, 20, 20, "X");
        closeButton.setOnClickListener(() -> {
            if (kernel != null && kernel.getChromiumService() != null) {
                kernel.getChromiumService().closeTab(surface.getSurfaceId());
                // After closing, redraw tab list
            }
        });
        closeButton.draw(g);
    }

    @Override
    public void cleanup(PGraphics p) {}

    @Override
    public String getScreenTitle() {
        return "Tab List";
    }

    @Override
    public void onForeground() {}

    @Override
    public void onBackground() {}

    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        if (backButton != null && backButton.onMousePressed(mouseX, mouseY)) return;

        // Handle clicks on tab cards and close buttons
        if (kernel == null || kernel.getChromiumService() == null) return;
        Collection<ChromiumSurface> surfaces = kernel.getChromiumService().getSurfaces();
        List<ChromiumSurface> sortedSurfaces = surfaces.stream().sorted((s1, s2) -> s1.getSurfaceId().compareTo(s2.getSurfaceId())).collect(Collectors.toList());

        int startX = (g.width - (COLUMNS * CARD_WIDTH + (COLUMNS - 1) * CARD_SPACING)) / 2;
        int startY = 80;

        int row = 0;
        int col = 0;

        for (ChromiumSurface surface : sortedSurfaces) {
            int cardX = startX + col * (CARD_WIDTH + CARD_SPACING);
            int cardY = startY + row * (CARD_HEIGHT + CARD_SPACING);

            // Check if card was clicked (excluding close button area)
            if (mouseX >= cardX && mouseX < cardX + CARD_WIDTH && mouseY >= cardY && mouseY < cardY + CARD_HEIGHT) {
                // Check if close button was clicked
                int closeButtonX = cardX + CARD_WIDTH - 25;
                int closeButtonY = cardY + 5;
                if (mouseX >= closeButtonX && mouseX < closeButtonX + 20 && mouseY >= closeButtonY && mouseY < closeButtonY + 20) {
                    // Close button clicked
                    if (kernel.getChromiumService().getActiveSurface().map(ChromiumSurface::getSurfaceId).orElse("").equals(surface.getSurfaceId())) {
                         // If closing active tab, make sure to set new active tab or close screen if no tabs left
                         // This logic is handled by ChromiumService.closeTab
                    }
                    kernel.getChromiumService().closeTab(surface.getSurfaceId());
                    return; // Event handled
                } else {
                    // Card body clicked - switch to this tab
                    kernel.getChromiumService().setActiveSurface(surface.getSurfaceId());
                    kernel.getScreenManager().popScreen(); // Close TabListScreen
                    return; // Event handled
                }
            }

            col++;
            if (col >= COLUMNS) {
                col = 0;
                row++;
            }
        }
    }

    @Override
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        if (backButton != null) backButton.onMouseReleased(mouseX, mouseY);
    }

    @Override
    public void mouseMoved(PGraphics g, int mouseX, int mouseY) {
        if (backButton != null) backButton.onMouseMoved(mouseX, mouseY);
    }
}
