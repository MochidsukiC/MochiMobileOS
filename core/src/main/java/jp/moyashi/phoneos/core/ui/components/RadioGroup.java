package jp.moyashi.phoneos.core.ui.components;

import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ラジオボタングループコンポーネント。
 * 複数のRadioButtonを管理し、排他的選択を実現する。
 *
 * @author MochiMobileOS Team
 * @version 1.0
 * @since 1.0
 */
public class RadioGroup extends BaseComponent implements Container {

    private List<RadioButton> radioButtons;
    private int selectedIndex = -1;
    private Consumer<Integer> onSelectionChangedListener;
    private float spacing = 5;

    /**
     * コンストラクタ。
     *
     * @param x X座標
     * @param y Y座標
     */
    public RadioGroup(float x, float y) {
        super(x, y, 200, 100);
        this.radioButtons = new ArrayList<>();
    }

    /**
     * ラジオボタンを追加する。
     *
     * @param label ラジオボタンのラベル
     * @return 追加されたRadioButton
     */
    public RadioButton addRadioButton(String label) {
        float buttonY = y + radioButtons.size() * (25 + spacing);
        RadioButton button = new RadioButton(x, buttonY, label);

        int index = radioButtons.size();
        button.setOnClickListener(() -> selectButton(index));

        radioButtons.add(button);
        updateSize();

        return button;
    }

    /**
     * 指定されたインデックスのラジオボタンを選択する。
     *
     * @param index ラジオボタンのインデックス
     */
    public void selectButton(int index) {
        if (index < 0 || index >= radioButtons.size()) {
            return;
        }

        // 他のボタンの選択を解除
        for (int i = 0; i < radioButtons.size(); i++) {
            radioButtons.get(i).setSelected(i == index);
        }

        int previousIndex = selectedIndex;
        selectedIndex = index;

        // リスナーを呼び出す
        if (previousIndex != selectedIndex && onSelectionChangedListener != null) {
            onSelectionChangedListener.accept(selectedIndex);
        }
    }

    @Override
    public void draw(PGraphics g) {
        if (!visible) return;

        for (RadioButton button : radioButtons) {
            button.draw(g);
        }
    }

    @Override
    public void update() {
        for (RadioButton button : radioButtons) {
            button.update();
        }
    }

    /**
     * マウスプレスイベントを処理する。
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @return イベントを処理した場合true
     */
    public boolean onMousePressed(int mouseX, int mouseY) {
        if (!enabled || !visible) return false;

        for (RadioButton button : radioButtons) {
            if (button.onMousePressed(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * マウスリリースイベントを処理する。
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     * @return イベントを処理した場合true
     */
    public boolean onMouseReleased(int mouseX, int mouseY) {
        if (!enabled || !visible) return false;

        for (RadioButton button : radioButtons) {
            if (button.onMouseReleased(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    /**
     * マウス移動イベントを処理する。
     *
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     */
    public void onMouseMoved(int mouseX, int mouseY) {
        for (RadioButton button : radioButtons) {
            button.onMouseMoved(mouseX, mouseY);
        }
    }

    private void updateSize() {
        if (radioButtons.isEmpty()) {
            height = 0;
        } else {
            height = radioButtons.size() * 25 + (radioButtons.size() - 1) * spacing;
        }
    }

    @Override
    public void addChild(UIComponent child) {
        if (child instanceof RadioButton) {
            radioButtons.add((RadioButton) child);
            updateSize();
        }
    }

    @Override
    public void removeChild(UIComponent child) {
        radioButtons.remove(child);
        updateSize();
    }

    @Override
    public void removeAllChildren() {
        radioButtons.clear();
        selectedIndex = -1;
        updateSize();
    }

    @Override
    public List<UIComponent> getChildren() {
        return new ArrayList<>(radioButtons);
    }

    @Override
    public void layout() {
        float currentY = y;
        for (RadioButton button : radioButtons) {
            button.setPosition(x, currentY);
            currentY += 25 + spacing;
        }
    }

    /**
     * 選択変更リスナーを設定する。
     *
     * @param listener 選択変更時のコールバック（選択されたインデックスを受け取る）
     */
    public void setOnSelectionChangedListener(Consumer<Integer> listener) {
        this.onSelectionChangedListener = listener;
    }

    // Getter/Setter

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public RadioButton getSelectedButton() {
        if (selectedIndex >= 0 && selectedIndex < radioButtons.size()) {
            return radioButtons.get(selectedIndex);
        }
        return null;
    }

    public void setSpacing(float spacing) {
        this.spacing = spacing;
        updateSize();
        layout();
    }
}
