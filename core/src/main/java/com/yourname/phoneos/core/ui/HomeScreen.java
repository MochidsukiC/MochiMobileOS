package com.yourname.phoneos.core.ui;

import processing.core.PApplet;

/**
 * The main home screen of the phone OS.
 * This screen serves as the primary interface and starting point for users.
 * Displays basic OS information and provides access to system functions.
 * 
 * @author YourName
 * @version 1.0
 */
public class HomeScreen implements Screen {
    
    /** Background color for the home screen */
    private int backgroundColor;
    
    /** Text color for the home screen */
    private int textColor;
    
    /** Flag to track if the screen has been initialized */
    private boolean isInitialized;
    
    /**
     * Constructs a new HomeScreen instance.
     * Sets up the basic properties for the home screen.
     */
    public HomeScreen() {
        backgroundColor = 0x4A90E2; // Blue background
        textColor = 255;            // White text
        isInitialized = false;
    }
    
    /**
     * Initializes the home screen when it becomes active.
     * Sets up any required resources or state for the screen.
     */
    @Override
    public void setup() {
        isInitialized = true;
        System.out.println("HomeScreen: Home screen initialized");
    }
    
    /**
     * Draws the home screen interface.
     * Renders the background, title, and basic system information.
     * 
     * @param p The PApplet instance for drawing operations
     */
    @Override
    public void draw(PApplet p) {
        // Draw background
        p.background(backgroundColor);
        
        // Set text properties
        p.fill(textColor);
        p.textAlign(p.CENTER, p.CENTER);
        
        // Draw main title
        p.textSize(24);
        p.text("MochiMobileOS", p.width / 2, p.height / 4);
        
        // Draw subtitle
        p.textSize(16);
        p.text("HomeScreen", p.width / 2, p.height / 4 + 40);
        
        // Draw system info
        p.textSize(12);
        p.text("Touch to interact", p.width / 2, p.height / 2);
        
        // Draw status information
        p.textAlign(p.LEFT, p.TOP);
        p.textSize(10);
        p.text("Status: " + (isInitialized ? "Ready" : "Loading..."), 10, 10);
        p.text("Version: 1.0", 10, 25);
        
        // Draw simple decorative elements
        drawDecorations(p);
    }
    
    /**
     * Handles mouse press events on the home screen.
     * Provides basic interaction feedback for development purposes.
     * 
     * @param mouseX The x-coordinate of the mouse press
     * @param mouseY The y-coordinate of the mouse press
     */
    @Override
    public void mousePressed(int mouseX, int mouseY) {
        System.out.println("HomeScreen: Mouse clicked at (" + mouseX + ", " + mouseY + ")");
        
        // Change background color on click for visual feedback
        if (backgroundColor == 0x4A90E2) {
            backgroundColor = 0x50C878; // Green
        } else {
            backgroundColor = 0x4A90E2; // Blue
        }
    }
    
    /**
     * Cleans up resources when the screen is deactivated.
     * Currently performs basic cleanup logging.
     */
    @Override
    public void cleanup() {
        isInitialized = false;
        System.out.println("HomeScreen: Home screen cleaned up");
    }
    
    /**
     * Gets the title of this screen.
     * 
     * @return The screen title
     */
    @Override
    public String getScreenTitle() {
        return "Home Screen";
    }
    
    /**
     * Draws simple decorative elements on the home screen.
     * Creates a basic geometric pattern for visual appeal.
     * 
     * @param p The PApplet instance for drawing operations
     */
    private void drawDecorations(PApplet p) {
        // Draw simple corner decorations
        p.stroke(255, 150); // Semi-transparent white
        p.strokeWeight(2);
        p.noFill();
        
        // Top-left corner
        p.arc(0, 0, 60, 60, 0, p.HALF_PI);
        
        // Top-right corner
        p.arc(p.width, 0, 60, 60, p.HALF_PI, p.PI);
        
        // Bottom-left corner
        p.arc(0, p.height, 60, 60, p.PI + p.HALF_PI, p.TWO_PI);
        
        // Bottom-right corner
        p.arc(p.width, p.height, 60, 60, p.PI, p.PI + p.HALF_PI);
        
        // Reset stroke settings
        p.noStroke();
    }
}