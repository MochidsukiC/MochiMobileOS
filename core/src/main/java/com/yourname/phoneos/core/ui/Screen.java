package com.yourname.phoneos.core.ui;

import processing.core.PApplet;

/**
 * Base interface for all screens in the phone OS.
 * This interface defines the contract that all screen implementations must follow.
 * Screens handle drawing, user input, and lifecycle management.
 * 
 * @author YourName
 * @version 1.0
 */
public interface Screen {
    
    /**
     * Called when the screen is first created or when it becomes active.
     * Use this method to initialize screen-specific resources and state.
     */
    void setup();
    
    /**
     * Called continuously to draw the screen contents.
     * This method should handle all visual rendering for the screen.
     * 
     * @param p The PApplet instance for drawing operations
     */
    void draw(PApplet p);
    
    /**
     * Called when a mouse press event occurs while this screen is active.
     * Handle user input and navigation logic in this method.
     * 
     * @param mouseX The x-coordinate of the mouse press
     * @param mouseY The y-coordinate of the mouse press
     */
    void mousePressed(int mouseX, int mouseY);
    
    /**
     * Called when the screen is about to be hidden or destroyed.
     * Use this method to clean up resources or save state.
     * Default implementation does nothing.
     */
    default void cleanup() {
        // Default empty implementation
    }
    
    /**
     * Gets the title or name of this screen.
     * This can be used for debugging or navigation history.
     * 
     * @return The screen title
     */
    default String getScreenTitle() {
        return this.getClass().getSimpleName();
    }
}