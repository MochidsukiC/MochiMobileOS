package jp.moyashi.phoneos.core.apps.calculator.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * Main screen for the Calculator application.
 * Provides a simple interface for basic arithmetic operations.
 */
public class
CalculatorScreen implements Screen {
    
    private final Kernel kernel;
    private String display;
    private String currentInput;
    private String operator;
    private double result;
    private boolean isNewNumber;
    
    public CalculatorScreen(Kernel kernel) {
        this.kernel = kernel;
        this.display = "0";
        this.currentInput = "";
        this.operator = "";
        this.result = 0;
        this.isNewNumber = true;
    }
    
    @Override
    @Deprecated
    public void setup(processing.core.PApplet p) {
        // Bridge pattern: delegate to PGraphics version
        if (p != null && p.g != null) {
            setup(p.g);
        }
    }

    /**
     * Initializes the calculator screen using PGraphics (unified architecture).
     *
     * @param g The PGraphics instance for initialization
     */
    public void setup(PGraphics g) {
        System.out.println("CalculatorScreen: Calculator initialized (PGraphics)");
    }
    
    @Override
    @Deprecated
    public void draw(PApplet p) {
        // Bridge pattern: delegate to PGraphics version
        if (p != null && p.g != null) {
            draw(p.g);
        }
    }

    /**
     * Draws the calculator screen using PGraphics (unified architecture).
     *
     * @param g The PGraphics instance to draw to
     */
    public void draw(PGraphics g) {
        // Background
        g.background(30);

        // Display
        g.fill(50);
        g.rect(20, 20, 360, 80, 8);

        g.fill(255);
        g.textAlign(g.RIGHT, g.CENTER);
        g.textSize(32);

        // 日本語フォントを設定
        if (kernel != null && kernel.getJapaneseFont() != null) {
            g.textFont(kernel.getJapaneseFont());
        }

        g.text(display, 370, 60);

        // Buttons
        drawButtons(g);

        // Back button
        g.fill(150);
        g.textAlign(g.LEFT, g.TOP);
        g.textSize(14);
        g.text("< Back", 10, 10);
    }

    
    private boolean isOperator(String button) {
        return button.equals("+") || button.equals("-") || button.equals("×") || 
               button.equals("÷") || button.equals("=");
    }
    
    @Override
    @Deprecated
    public void mousePressed(processing.core.PApplet p, int mouseX, int mouseY) {
        // Bridge pattern: delegate to PGraphics version
        if (p != null && p.g != null) {
            mousePressed(p.g, mouseX, mouseY);
        }
    }

    /**
     * Handles mouse press events using PGraphics (unified architecture).
     *
     * @param g The PGraphics instance
     * @param mouseX The X coordinate of the mouse
     * @param mouseY The Y coordinate of the mouse
     */
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        // Back button
        if (mouseX < 60 && mouseY < 30) {
            goBack();
            return;
        }

        // Calculator buttons
        String button = getButtonAt(mouseX, mouseY);
        if (button != null) {
            handleButtonPress(button);
        }
    }
    
    private String getButtonAt(int mouseX, int mouseY) {
        String[][] buttons = {
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
        
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[row].length; col++) {
                if (buttons[row][col].isEmpty()) continue;
                
                int x = startX + col * (buttonWidth + padding);
                int y = startY + row * (buttonHeight + padding);
                
                if (mouseX >= x && mouseX <= x + buttonWidth && 
                    mouseY >= y && mouseY <= y + buttonHeight) {
                    return buttons[row][col];
                }
            }
        }
        return null;
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
        System.out.println("CalculatorScreen: Going back with animation");
        if (kernel != null && kernel.getScreenManager() != null) {
            // Try to get Calculator app icon for animation
            try {
                processing.core.PImage calculatorIcon = null;
                // TODO: PGraphics統一アーキテクチャに対応したアイコン作成機能を実装
                // 現在はcalculatorIconをnullのままにして、アイコンなしで実行
                System.out.println("CalculatorScreen: Icon creation disabled in PGraphics architecture");
                calculatorIcon = null;
                
                // Pop with animation - assume icon is at center-left of app library
                if (calculatorIcon != null) {
                    float iconX = 48; // Approximate icon position in app library
                    float iconY = 200; // Approximate icon position
                    kernel.getScreenManager().popScreenWithAnimation(iconX, iconY, 48, calculatorIcon);
                } else {
                    kernel.getScreenManager().popScreen();
                }
            } catch (Exception e) {
                // Fallback to normal pop
                kernel.getScreenManager().popScreen();
            }
        }
    }
    
    @Override
    @Deprecated
    public void cleanup(PApplet pApplet) {
        // Bridge pattern: delegate to PGraphics version
        if (pApplet != null && pApplet.g != null) {
            cleanup(pApplet.g);
        }
    }

    /**
     * Cleanup method using PGraphics (unified architecture).
     *
     * @param g The PGraphics instance
     */
    public void cleanup(PGraphics g) {
        System.out.println("CalculatorScreen: Calculator screen cleaned up (PGraphics)");
    }

    /**
     * Handles mouse drag events using PGraphics (unified architecture).
     *
     * @param g The PGraphics instance
     * @param mouseX The X coordinate of the mouse
     * @param mouseY The Y coordinate of the mouse
     */
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        // Calculator doesn't need drag handling, but method provided for completeness
    }

    /**
     * Handles mouse release events using PGraphics (unified architecture).
     *
     * @param g The PGraphics instance
     * @param mouseX The X coordinate of the mouse
     * @param mouseY The Y coordinate of the mouse
     */
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        // Calculator doesn't need mouse release handling, but method provided for completeness
    }

    /**
     * Handles key press events using PGraphics (unified architecture).
     *
     * @param g The PGraphics instance
     * @param key The key that was pressed
     * @param keyCode The key code
     */
    public void keyPressed(PGraphics g, char key, int keyCode) {
        // Calculator could potentially handle number key input, but currently uses button clicks
        // This method is provided for future enhancement
    }
    
    @Override
    public String getScreenTitle() {
        return "Calculator";
    }

    /**
     * ボタンを描画する（PGraphics版 - 統一アーキテクチャ）
     *
     * @param g 描画対象のPGraphics
     */
    private void drawButtons(PGraphics g) {
        String[][] buttons = {
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

        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[row].length; col++) {
                String buttonText = buttons[row][col];
                if (buttonText.isEmpty()) continue;

                int x = startX + col * (buttonWidth + padding);
                int y = startY + row * (buttonHeight + padding);

                // Button background color
                if (isOperator(buttonText)) {
                    g.fill(255, 159, 10); // Orange for operators
                } else if (buttonText.equals("C")) {
                    g.fill(165, 165, 165); // Gray for clear
                } else {
                    g.fill(77, 77, 77); // Dark gray for numbers
                }

                g.noStroke();
                g.rect(x, y, buttonWidth, buttonHeight, 8);

                // Button text
                g.fill(255);
                g.textAlign(g.CENTER, g.CENTER);
                g.textSize(24);

                // 日本語フォントを設定
                if (kernel != null && kernel.getJapaneseFont() != null) {
                    g.textFont(kernel.getJapaneseFont());
                }

                g.text(buttonText, x + buttonWidth / 2, y + buttonHeight / 2);
            }
        }
    }
}