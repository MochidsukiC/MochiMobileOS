package jp.moyashi.phoneos.core.ui.components;

/**
 * 線形レイアウトマネージャー。
 * 子要素を垂直/水平方向に配置。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class LinearLayout extends Panel {

    public enum Orientation {
        VERTICAL,
        HORIZONTAL
    }

    private Orientation orientation;
    private float spacing;

    public LinearLayout(float x, float y, float width, float height, Orientation orientation) {
        super(x, y, width, height);
        this.orientation = orientation;
        this.spacing = 5;
        setBorderWidth(0);
    }

    @Override
    public void layout() {
        float currentPos = getPadding();

        for (UIComponent child : getChildren()) {
            if (!child.isVisible()) continue;

            if (orientation == Orientation.VERTICAL) {
                child.setPosition(x + getPadding(), y + currentPos);
                currentPos += child.getHeight() + spacing;
            } else {
                child.setPosition(x + currentPos, y + getPadding());
                currentPos += child.getWidth() + spacing;
            }
        }
    }

    @Override
    public void addChild(UIComponent child) {
        super.addChild(child);
        layout();
    }

    public void setSpacing(float spacing) {
        this.spacing = spacing;
        layout();
    }
}
