package jp.moyashi.phoneos.core.apps.calculator.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.components.Button;
import jp.moyashi.phoneos.core.ui.components.Label;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Main screen for the Calculator application.
 * Refactored to use GUI component library for better maintainability.
 */
public class CalculatorScreen implements Screen {

    private final Kernel kernel;
    private String display;
    private String currentInput;
    private String operator;
    private double result;
    private boolean isNewNumber;

    // UI Components
    private List<Button> buttons;
    private Label displayLabel;
    private Button backButton;

    public CalculatorScreen(Kernel kernel) {
        this.kernel = kernel;
        this.display = "0";
        this.currentInput = "";
        this.operator = "";
        this.result = 0;
        this.isNewNumber = true;
        this.buttons = new ArrayList<>();
    }

    @Override
    @Deprecated
    public void setup(processing.core.PApplet p) {
        if (p != null && p.g != null) {
            setup(p.g);
        }
    }

    public void setup(PGraphics g) {
        System.out.println("CalculatorScreen: Calculator initialized with GUI components");

        // ディスプレイラベル
        displayLabel = new Label(20, 20, 360, 80, display);
        displayLabel.setTextSize(32);
        displayLabel.setHorizontalAlign(PApplet.RIGHT);
        displayLabel.setVerticalAlign(PApplet.CENTER);
        displayLabel.setTextColor(0xFFFFFFFF);
        if (kernel != null && kernel.getJapaneseFont() != null) {
            displayLabel.setFont(kernel.getJapaneseFont());
        }

        // 戻るボタン
        backButton = new Button(10, 10, 60, 25, "< Back");
        backButton.setBackgroundColor(0xFF505050);
        backButton.setTextColor(0xFFFFFFFF);
        backButton.setCornerRadius(3);
        backButton.setOnClickListener(this::goBack);

        // 電卓ボタン
        String[][] buttonLabels = {
            {"C", "±", "%", "÷"},
            {"7", "8", "9", "×"},
            {"4", "5", "6", "-"},
            {"1", "2", "3", "+"},
            {"0", ".", "=", ""}
        };

        int buttonWidth = 80;
        int buttonHeight = 60;
        int padding = 10;
        int startX = 20;
        int startY = 120;

        for (int row = 0; row < buttonLabels.length; row++) {
            for (int col = 0; col < buttonLabels[row].length; col++) {
                String label = buttonLabels[row][col];
                if (label.isEmpty()) continue;

                int x = startX + col * (buttonWidth + padding);
                int y = startY + row * (buttonHeight + padding);

                Button btn = new Button(x, y, buttonWidth, buttonHeight, label);

                // ボタンの色設定
                if (isOperator(label)) {
                    btn.setBackgroundColor(0xFFFF9F0A); // オレンジ
                    btn.setHoverColor(0xFFFFAF2A);
                    btn.setPressColor(0xFFFF8F00);
                } else if (label.equals("C")) {
                    btn.setBackgroundColor(0xFFA5A5A5); // グレー
                    btn.setHoverColor(0xFFB5B5B5);
                    btn.setPressColor(0xFF959595);
                } else {
                    btn.setBackgroundColor(0xFF4D4D4D); // ダークグレー
                    btn.setHoverColor(0xFF5D5D5D);
                    btn.setPressColor(0xFF3D3D3D);
                }

                btn.setBorderColor(0xFF000000);
                btn.setTextColor(0xFFFFFFFF);
                btn.setCornerRadius(8);

                // フォント設定
                if (kernel != null && kernel.getJapaneseFont() != null) {
                    // Buttonコンポーネントはフォント設定機能がないため、
                    // テキスト描画時にKernelのフォントを使用
                }

                // クリックリスナー
                final String buttonLabel = label;
                btn.setOnClickListener(() -> handleButtonPress(buttonLabel));

                buttons.add(btn);
            }
        }
    }

    @Override
    @Deprecated
    public void draw(PApplet p) {
        if (p != null && p.g != null) {
            draw(p.g);
        }
    }

    public void draw(PGraphics g) {
        // 背景
        g.background(30);

        // ディスプレイ背景
        g.pushStyle();
        g.fill(50);
        g.noStroke();
        g.rect(20, 20, 360, 80, 8);
        g.popStyle();

        // ディスプレイテキスト更新
        displayLabel.setText(display);
        displayLabel.draw(g);

        // 戻るボタン
        backButton.draw(g);

        // 電卓ボタン
        for (Button btn : buttons) {
            btn.draw(g);
        }
    }

    private boolean isOperator(String button) {
        return button.equals("+") || button.equals("-") || button.equals("×") ||
               button.equals("÷") || button.equals("=");
    }

    @Override
    @Deprecated
    public void mousePressed(processing.core.PApplet p, int mouseX, int mouseY) {
        if (p != null && p.g != null) {
            mousePressed(p.g, mouseX, mouseY);
        }
    }

    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        // 戻るボタン
        if (backButton.onMousePressed(mouseX, mouseY)) {
            return;
        }

        // 電卓ボタン
        for (Button btn : buttons) {
            if (btn.onMousePressed(mouseX, mouseY)) {
                return;
            }
        }
    }

    @Override
    @Deprecated
    public void mouseReleased(processing.core.PApplet p, int mouseX, int mouseY) {
        if (p != null && p.g != null) {
            mouseReleased(p.g, mouseX, mouseY);
        }
    }

    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        // 戻るボタン
        if (backButton.onMouseReleased(mouseX, mouseY)) {
            return;
        }

        // 電卓ボタン
        for (Button btn : buttons) {
            if (btn.onMouseReleased(mouseX, mouseY)) {
                return;
            }
        }
    }

    /**
     * マウス移動イベント（ホバー効果用）
     */
    public void mouseMoved(PGraphics g, int mouseX, int mouseY) {
        backButton.onMouseMoved(mouseX, mouseY);

        for (Button btn : buttons) {
            btn.onMouseMoved(mouseX, mouseY);
        }
    }

    private void handleButtonPress(String button) {
        System.out.println("Calculator: Button pressed: " + button);

        switch (button) {
            case "C":
                clear();
                break;
            case "=":
                calculate();
                break;
            case "+":
            case "-":
            case "×":
            case "÷":
                setOperator(button);
                break;
            case ".":
                addDecimal();
                break;
            default:
                if (isDigit(button)) {
                    addDigit(button);
                }
                break;
        }
    }

    private void clear() {
        display = "0";
        currentInput = "";
        operator = "";
        result = 0;
        isNewNumber = true;
    }

    private void addDigit(String digit) {
        if (isNewNumber) {
            display = digit;
            isNewNumber = false;
        } else {
            if (display.equals("0")) {
                display = digit;
            } else {
                display += digit;
            }
        }
    }

    private void addDecimal() {
        if (isNewNumber) {
            display = "0.";
            isNewNumber = false;
        } else if (!display.contains(".")) {
            display += ".";
        }
    }

    private void setOperator(String op) {
        if (!operator.isEmpty()) {
            calculate();
        }
        result = Double.parseDouble(display);
        operator = op;
        isNewNumber = true;
    }

    private void calculate() {
        if (operator.isEmpty()) return;

        double current = Double.parseDouble(display);

        switch (operator) {
            case "+":
                result += current;
                break;
            case "-":
                result -= current;
                break;
            case "×":
                result *= current;
                break;
            case "÷":
                if (current != 0) {
                    result /= current;
                } else {
                    display = "Error";
                    return;
                }
                break;
        }

        display = formatResult(result);
        operator = "";
        isNewNumber = true;
    }

    private String formatResult(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        } else {
            return String.valueOf(value);
        }
    }

    private boolean isDigit(String str) {
        return str.matches("\\d");
    }

    private void goBack() {
        System.out.println("CalculatorScreen: Going back");
        if (kernel != null && kernel.getScreenManager() != null) {
            kernel.getScreenManager().popScreen();
        }
    }

    @Override
    @Deprecated
    public void cleanup(PApplet pApplet) {
        if (pApplet != null && pApplet.g != null) {
            cleanup(pApplet.g);
        }
    }

    public void cleanup(PGraphics g) {
        System.out.println("CalculatorScreen: Calculator screen cleaned up");
    }

    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        // Calculator doesn't need drag handling
    }

    public void keyPressed(PGraphics g, char key, int keyCode) {
        // Calculator could potentially handle number key input
    }

    @Override
    public String getScreenTitle() {
        return "Calculator";
    }
}
