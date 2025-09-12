package jp.moyashi.phoneos.core.apps.calculator.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PApplet;

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
    public void setup(processing.core.PApplet p) {
        System.out.println("CalculatorScreen: Calculator initialized");
    }
    
    @Override
    public void draw(PApplet p) {
        // Background
        p.background(30);
        
        // Display
        p.fill(50);
        p.rect(20, 20, 360, 80, 8);
        
        p.fill(255);
        p.textAlign(p.RIGHT, p.CENTER);
        p.textSize(32);
        p.text(display, 370, 60);
        
        // Buttons
        drawButtons(p);
        
        // Back button
        p.fill(150);
        p.textAlign(p.LEFT, p.TOP);
        p.textSize(14);
        p.text("< Back", 10, 10);
    }
    
    private void drawButtons(PApplet p) {
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
                
                // Button color
                if (isOperator(buttons[row][col])) {
                    p.fill(255, 159, 10); // Orange for operators
                } else if (buttons[row][col].equals("C")) {
                    p.fill(165, 165, 165); // Gray for clear
                } else {
                    p.fill(77, 77, 77); // Dark gray for numbers
                }
                
                p.rect(x, y, buttonWidth, buttonHeight, 8);
                
                // Button text
                p.fill(255);
                p.textAlign(p.CENTER, p.CENTER);
                p.textSize(24);
                p.text(buttons[row][col], x + buttonWidth/2, y + buttonHeight/2);
            }
        }
    }
    
    private boolean isOperator(String button) {
        return button.equals("+") || button.equals("-") || button.equals("×") || 
               button.equals("÷") || button.equals("=");
    }
    
    @Override
    public void mousePressed(processing.core.PApplet p, int mouseX, int mouseY) {
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
                if (kernel.getClass().getSuperclass().getSimpleName().equals("PApplet")) {
                    processing.core.PApplet pApplet = (processing.core.PApplet) kernel;
                    
                    // Create a simple icon for animation
                    calculatorIcon = pApplet.createGraphics(64, 64);
                    processing.core.PGraphics icon = (processing.core.PGraphics) calculatorIcon;
                    icon.beginDraw();
                    icon.background(255, 159, 10); // Orange background
                    icon.fill(0);
                    icon.rect(8, 8, 48, 16, 2);
                    icon.fill(255);
                    for (int row = 0; row < 3; row++) {
                        for (int col = 0; col < 3; col++) {
                            int x = 8 + col * 16;
                            int y = 32 + row * 10;
                            icon.rect(x, y, 12, 8, 1);
                        }
                    }
                    icon.endDraw();
                }
                
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
    public void cleanup(PApplet pApplet) {
        System.out.println("CalculatorScreen: Calculator screen cleaned up");
    }
    
    @Override
    public String getScreenTitle() {
        return "Calculator";
    }
}