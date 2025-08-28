package jp.moyashi.phoneos.core.ui;

import jp.moyashi.phoneos.core.apps.launcher.ui.SafeHomeScreen;
import processing.core.PApplet;
import java.util.Stack;

/**
 * Screen manager for handling screen transitions and navigation in the phone OS.
 * This class manages a stack of screens, allowing for hierarchical navigation
 * with push/pop operations similar to mobile app navigation.
 * 
 * @author YourName
 * @version 1.0
 */
public class ScreenManager {
    
    /** Stack of screens for navigation management */
    private Stack<Screen> screenStack;
    
    /**
     * Constructs a new ScreenManager instance.
     * Initializes the screen stack for navigation management.
     */
    public ScreenManager() {
        screenStack = new Stack<>();
        System.out.println("ScreenManager: Screen manager initialized");
    }
    
    /**
     * Pushes a new screen onto the navigation stack.
     * The new screen becomes the active screen and its setup() method is called.
     * 
     * @param screen The screen to push onto the stack
     */
    public void pushScreen(Screen screen) {
        if (screen != null) {
            screenStack.push(screen);
            screen.setup();
            System.out.println("ScreenManager: Pushed screen - " + screen.getScreenTitle());
        }
    }
    
    /**
     * Pops the current screen from the navigation stack.
     * The previous screen becomes active again.
     * Calls cleanup() on the popped screen.
     * 
     * @return The popped screen, or null if stack is empty
     */
    public Screen popScreen() {
        if (!screenStack.isEmpty()) {
            Screen poppedScreen = screenStack.pop();
            poppedScreen.cleanup();
            System.out.println("ScreenManager: Popped screen - " + poppedScreen.getScreenTitle());
            return poppedScreen;
        }
        return null;
    }
    
    /**
     * Gets the currently active screen without removing it from the stack.
     * 
     * @return The current screen, or null if stack is empty
     */
    public Screen getCurrentScreen() {
        if (!screenStack.isEmpty()) {
            return screenStack.peek();
        }
        return null;
    }
    
    /**
     * Draws the currently active screen.
     * Delegates drawing to the current screen's draw() method.
     * 
     * @param p The PApplet instance for drawing operations
     */
    public void draw(PApplet p) {
        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null) {
            currentScreen.draw(p);
        } else {
            // Draw a default screen if no screens are active
            drawEmptyScreen(p);
        }
    }
    
    /**
     * Handles mouse press events by delegating to the current screen.
     * 
     * @param mouseX The x-coordinate of the mouse press
     * @param mouseY The y-coordinate of the mouse press
     */
    public void mousePressed(int mouseX, int mouseY) {
        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null) {
            currentScreen.mousePressed(mouseX, mouseY);
        }
    }
    
    /**
     * Handles mouse drag events by delegating to the current screen.
     * 
     * @param mouseX The x-coordinate of the mouse position
     * @param mouseY The y-coordinate of the mouse position
     */
    public void mouseDragged(int mouseX, int mouseY) {
        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null && currentScreen instanceof SafeHomeScreen) {
            ((SafeHomeScreen) currentScreen).mouseDragged(mouseX, mouseY);
        }
        // Add support for other screens that implement drag handling
    }
    
    /**
     * Handles mouse release events by delegating to the current screen.
     * 
     * @param mouseX The x-coordinate of the mouse position
     * @param mouseY The y-coordinate of the mouse position
     */
    public void mouseReleased(int mouseX, int mouseY) {
        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null && currentScreen instanceof SafeHomeScreen) {
            ((SafeHomeScreen) currentScreen).mouseReleased(mouseX, mouseY);
        }
        // Add support for other screens that implement release handling
    }
    
    /**
     * Gets the number of screens in the navigation stack.
     * 
     * @return The stack size
     */
    public int getStackSize() {
        return screenStack.size();
    }
    
    /**
     * Checks if there are any screens in the navigation stack.
     * 
     * @return true if stack is empty, false otherwise
     */
    public boolean isEmpty() {
        return screenStack.isEmpty();
    }
    
    /**
     * Clears all screens from the navigation stack.
     * Calls cleanup() on each screen before removing it.
     */
    public void clearAllScreens() {
        while (!screenStack.isEmpty()) {
            popScreen();
        }
        System.out.println("ScreenManager: All screens cleared");
    }
    
    /**
     * Draws a default empty screen when no screens are active.
     * 
     * @param p The PApplet instance for drawing operations
     */
    private void drawEmptyScreen(PApplet p) {
        p.background(50); // Dark gray background
        p.fill(255);      // White text
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(16);
        p.text("No active screens", p.width / 2, p.height / 2);
    }
}