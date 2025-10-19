package jp.moyashi.phoneos.core.ui.components;

import processing.core.PGraphics;
import processing.core.PImage;

/**
 * 画像表示コンポーネント。
 * スケーリング、アスペクト比維持、配置設定に対応。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class ImageView extends BaseComponent {

    public enum ScaleMode {
        FIT,        // アスペクト比を維持してフィット
        FILL,       // アスペクト比を維持してフィル（はみ出す）
        STRETCH,    // アスペクト比を無視して引き伸ばし
        CENTER      // 中央配置、スケーリングなし
    }

    private PImage image;
    private ScaleMode scaleMode;
    private int backgroundColor;
    private float cornerRadius;

    /**
     * コンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     */
    public ImageView(float x, float y, float width, float height) {
        super(x, y, width, height);
        this.image = null;
        this.scaleMode = ScaleMode.FIT;
        this.backgroundColor = 0xFFF0F0F0;
        this.cornerRadius = 0;
    }

    /**
     * 画像付きコンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     * @param image 表示する画像
     */
    public ImageView(float x, float y, float width, float height, PImage image) {
        this(x, y, width, height);
        this.image = image;
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        g.pushStyle();

        // 背景
        g.fill(backgroundColor);
        g.noStroke();
        if (cornerRadius > 0) {
            g.rect(x, y, width, height, cornerRadius);
        } else {
            g.rect(x, y, width, height);
        }

        // 画像描画
        if (image != null) {
            drawImage(g);
        }

        g.popStyle();
    }

    private void drawImage(PGraphics g) {
        switch (scaleMode) {
            case FIT:
                drawFit(g);
                break;
            case FILL:
                drawFill(g);
                break;
            case STRETCH:
                drawStretch(g);
                break;
            case CENTER:
                drawCenter(g);
                break;
        }
    }

    private void drawFit(PGraphics g) {
        float imgAspect = (float) image.width / image.height;
        float viewAspect = width / height;

        float drawWidth, drawHeight;
        float drawX, drawY;

        if (imgAspect > viewAspect) {
            // 画像が横長
            drawWidth = width;
            drawHeight = width / imgAspect;
            drawX = x;
            drawY = y + (height - drawHeight) / 2;
        } else {
            // 画像が縦長
            drawHeight = height;
            drawWidth = height * imgAspect;
            drawX = x + (width - drawWidth) / 2;
            drawY = y;
        }

        g.image(image, drawX, drawY, drawWidth, drawHeight);
    }

    private void drawFill(PGraphics g) {
        float imgAspect = (float) image.width / image.height;
        float viewAspect = width / height;

        float drawWidth, drawHeight;
        float drawX, drawY;

        if (imgAspect < viewAspect) {
            // 画像が縦長
            drawWidth = width;
            drawHeight = width / imgAspect;
            drawX = x;
            drawY = y + (height - drawHeight) / 2;
        } else {
            // 画像が横長
            drawHeight = height;
            drawWidth = height * imgAspect;
            drawX = x + (width - drawWidth) / 2;
            drawY = y;
        }

        g.image(image, drawX, drawY, drawWidth, drawHeight);
    }

    private void drawStretch(PGraphics g) {
        g.image(image, x, y, width, height);
    }

    private void drawCenter(PGraphics g) {
        float drawX = x + (width - image.width) / 2;
        float drawY = y + (height - image.height) / 2;
        g.image(image, drawX, drawY);
    }

    // Getter/Setter

    public PImage getImage() {
        return image;
    }

    public void setImage(PImage image) {
        this.image = image;
    }

    public ScaleMode getScaleMode() {
        return scaleMode;
    }

    public void setScaleMode(ScaleMode scaleMode) {
        this.scaleMode = scaleMode;
    }

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    public void setCornerRadius(float radius) {
        this.cornerRadius = radius;
    }
}
