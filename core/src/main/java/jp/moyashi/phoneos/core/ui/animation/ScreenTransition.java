package jp.moyashi.phoneos.core.ui.animation;

import processing.core.PApplet;
import processing.core.PImage;

/**
 * iOS-style screen transition animation system.
 * Handles zoom-in/zoom-out transitions between screens with icon-based animations.
 * 
 * @author MochiMobileOS
 * @version 1.0
 */
public class ScreenTransition {
    
    /** Animation states */
    public enum AnimationState {
        NONE,           // No animation
        ZOOM_IN,        // Opening app (icon -> screen)
        ZOOM_OUT,       // Closing app (screen -> icon)
        FADE_IN,        // Fade in transition
        FADE_OUT        // Fade out transition
    }
    
    /** Animation duration in milliseconds */
    private long animationDurationMs = 300; // default 300ms like iOS
    
    /** Current animation state */
    private AnimationState currentState;
    
    /** Animation start time */
    private long animationStartTime;
    
    /** Animation progress (0.0 to 1.0) */
    private float progress;
    
    /** Source icon position and size */
    private float sourceX, sourceY, sourceWidth, sourceHeight;
    
    /** Target screen dimensions */
    private float targetX, targetY, targetWidth, targetHeight;
    
    /** App icon image */
    private PImage iconImage;
    
    /** Screen capture for animation */
    private PImage screenCapture;
    
    /** Pending screen to push after animation completes */
    private Object pendingScreen;
    
    /** Callback interface for animation completion */
    private AnimationCallback animationCallback;
    
    /** Animation easing function parameters */
    private static final float EASE_OUT_QUART = 0.25f;
    
    /**
     * Callback interface for animation completion events.
     */
    public interface AnimationCallback {
        void onAnimationComplete(AnimationState completedState, Object pendingScreen);
    }
    
    /**
     * Creates a new ScreenTransition instance.
     */
    public ScreenTransition() {
        this.currentState = AnimationState.NONE;
        this.progress = 0.0f;
        
        // Default screen dimensions (will be updated)
        this.targetX = 0;
        this.targetY = 0;
        this.targetWidth = 400;
        this.targetHeight = 600;
        
        System.out.println("ScreenTransition: Animation system initialized");
    }
    
    /**
     * Starts a zoom-in animation from icon to screen.
     * 
     * @param iconX Icon center X position
     * @param iconY Icon center Y position
     * @param iconSize Icon size
     * @param icon Icon image
     */
    public void startZoomIn(float iconX, float iconY, float iconSize, PImage icon) {
        System.out.println("ScreenTransition: startZoomIn called");
        System.out.println("ScreenTransition: Current state before: " + currentState);
        
        this.currentState = AnimationState.ZOOM_IN;
        this.animationStartTime = System.currentTimeMillis();
        this.progress = 0.0f;
        
        // Source (icon) position
        this.sourceX = iconX;
        this.sourceY = iconY;
        this.sourceWidth = iconSize;
        this.sourceHeight = iconSize;
        
        // Store icon image
        this.iconImage = icon;
        
        System.out.println("ScreenTransition: Animation state set to " + currentState);
        System.out.println("ScreenTransition: Animation start time: " + animationStartTime);
        System.out.println("ScreenTransition: Source position: (" + sourceX + ", " + sourceY + ") size " + sourceWidth + "x" + sourceHeight);
        System.out.println("ScreenTransition: Target dimensions: (" + targetX + ", " + targetY + ") " + targetWidth + "x" + targetHeight);
        System.out.println("ScreenTransition: Icon image: " + (iconImage != null ? iconImage.width + "x" + iconImage.height : "null"));
        System.out.println("ScreenTransition: Starting zoom-in animation from icon at (" + 
                          iconX + ", " + iconY + ") size " + iconSize);
    }
    
    /**
     * Starts a zoom-out animation from screen to icon.
     * 
     * @param iconX Target icon center X position
     * @param iconY Target icon center Y position
     * @param iconSize Target icon size
     * @param icon Icon image
     * @param screenCapture Current screen capture
     */
    public void startZoomOut(float iconX, float iconY, float iconSize, PImage icon, PImage screenCapture) {
        this.currentState = AnimationState.ZOOM_OUT;
        this.animationStartTime = System.currentTimeMillis();
        this.progress = 0.0f;
        
        // Target (icon) position
        this.sourceX = iconX;
        this.sourceY = iconY;
        this.sourceWidth = iconSize;
        this.sourceHeight = iconSize;
        
        // Store images
        this.iconImage = icon;
        this.screenCapture = screenCapture;
        
        System.out.println("ScreenTransition: Starting zoom-out animation to icon at (" + 
                          iconX + ", " + iconY + ") size " + iconSize);
    }
    
    /**
     * Updates the animation progress.
     */
    public void update() {
        if (currentState == AnimationState.NONE) {
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - animationStartTime;
        
        // Calculate linear progress
        float linearProgress = Math.min((float) elapsed / Math.max(1, animationDurationMs), 1.0f);
        
        // Apply easing function (ease-out-quart for iOS-like feel)
        // Use cubic ease-out for consistency
        this.progress = 1.0f - (float) Math.pow(1.0f - linearProgress, 3);
        
        // Debug logging
        if (elapsed % 50 == 0 || linearProgress >= 1.0f) { // Log every ~50ms or at completion
            System.out.println("ScreenTransition: update() - elapsed=" + elapsed + "ms, linearProgress=" + linearProgress + ", easedProgress=" + progress);
        }
        
        // Check if animation is complete
        if (linearProgress >= 1.0f) {
            System.out.println("ScreenTransition: Animation completing - elapsed=" + elapsed + "ms, duration=" + animationDurationMs + "ms");
            completeAnimation();
        }
    }
    
    /**
     * Renders the current animation frame.
     * 
     * @param p PApplet for drawing
     */
    public void draw(PApplet p) {
        if (currentState == AnimationState.NONE) {
            return;
        }
        
        System.out.println("ScreenTransition: Drawing animation " + currentState + " with progress " + progress);
        
        switch (currentState) {
            case ZOOM_IN:
                drawZoomIn(p);
                break;
                
            case ZOOM_OUT:
                drawZoomOut(p);
                break;
                
            case FADE_IN:
                drawFadeIn(p);
                break;
                
            case FADE_OUT:
                drawFadeOut(p);
                break;
        }
    }
    
    /**
     * Draws zoom-in animation (icon expanding to screen).
     */
    private void drawZoomIn(PApplet p) {
        if (iconImage == null) {
            System.out.println("ScreenTransition: drawZoomIn - iconImage is null!");
            return;
        }
        
        System.out.println("ScreenTransition: drawZoomIn - progress=" + progress + ", iconImage size=" + iconImage.width + "x" + iconImage.height);
        
        // DON'T clear background - let ScreenManager draw the background screen first
        
        // Calculate current size and position
        float currentWidth = PApplet.lerp(sourceWidth, targetWidth, progress);
        float currentHeight = PApplet.lerp(sourceHeight, targetHeight, progress);
        float currentX = PApplet.lerp(sourceX - sourceWidth/2, targetX, progress);
        float currentY = PApplet.lerp(sourceY - sourceHeight/2, targetY, progress);
        
        System.out.println("ScreenTransition: Drawing at (" + currentX + ", " + currentY + ") size " + currentWidth + "x" + currentHeight);
        
        // Calculate opacity for fade effect
        float iconOpacity = 255 * (1.0f - progress);
        
        // Draw gradual black overlay that starts light and gets darker
        float overlayAlpha = progress * 180; // Max 180 alpha for smoother transition
        p.fill(0, 0, 0, overlayAlpha);
        p.rect(0, 0, p.width, p.height);
        
        // Draw expanding icon on top of the overlay
        p.tint(255, iconOpacity);
        p.image(iconImage, currentX, currentY, currentWidth, currentHeight);
        p.noTint();
    }
    
    /**
     * Draws zoom-out animation (screen shrinking to icon).
     */
    private void drawZoomOut(PApplet p) {
        if (screenCapture == null) return;
        
        // Clear background
        p.background(0);
        
        // Calculate current size and position (reverse of zoom-in)
        float currentWidth = PApplet.lerp(targetWidth, sourceWidth, progress);
        float currentHeight = PApplet.lerp(targetHeight, sourceHeight, progress);
        float currentX = PApplet.lerp(targetX, sourceX - sourceWidth/2, progress);
        float currentY = PApplet.lerp(targetY, sourceY - sourceHeight/2, progress);
        
        // Calculate opacity
        float screenOpacity = 255 * (1.0f - progress);
        
        // Draw shrinking screen
        p.tint(255, screenOpacity);
        p.image(screenCapture, currentX, currentY, currentWidth, currentHeight);
        p.noTint();
        
        // Draw target icon when animation is nearly complete
        if (progress > 0.8f && iconImage != null) {
            float iconOpacity = (progress - 0.8f) / 0.2f * 255;
            p.tint(255, iconOpacity);
            p.image(iconImage, sourceX - sourceWidth/2, sourceY - sourceHeight/2, sourceWidth, sourceHeight);
            p.noTint();
        }
    }
    
    /**
     * Draws fade-in animation.
     */
    private void drawFadeIn(PApplet p) {
        if (screenCapture != null) {
            p.tint(255, 255 * progress);
            p.image(screenCapture, 0, 0);
            p.noTint();
        }
    }
    
    /**
     * Draws fade-out animation.
     */
    private void drawFadeOut(PApplet p) {
        if (screenCapture != null) {
            p.tint(255, 255 * (1.0f - progress));
            p.image(screenCapture, 0, 0);
            p.noTint();
        }
    }
    
    /**
     * Completes the current animation.
     */
    private void completeAnimation() {
        System.out.println("ScreenTransition: Animation " + currentState + " completed");
        
        AnimationState completedState = currentState;
        
        this.currentState = AnimationState.NONE;
        this.progress = 0.0f;
        this.iconImage = null;
        this.screenCapture = null;
        
        // Call callback if set
        if (animationCallback != null) {
            animationCallback.onAnimationComplete(completedState, null);
        }
    }
    
    /**
     * Applies ease-out-quart easing function.
     * Creates a smooth deceleration effect like iOS.
     * 
     * @param t Linear progress (0.0 to 1.0)
     * @return Eased progress (0.0 to 1.0)
     */
    // kept for potential future use
    private float easeOutQuart(float t) { return 1.0f - (float) Math.pow(1.0f - t, 4); }
    
    /**
     * Checks if an animation is currently playing.
     * 
     * @return true if animation is active, false otherwise
     */
    public boolean isAnimating() {
        return currentState != AnimationState.NONE;
    }
    
    /**
     * Gets the current animation state.
     * 
     * @return Current animation state
     */
    public AnimationState getCurrentState() {
        return currentState;
    }
    
    /**
     * Gets the current animation progress.
     * 
     * @return Progress from 0.0 to 1.0
     */
    public float getProgress() {
        return progress;
    }
    
    /**
     * Forces the animation to stop.
     */
    public void stopAnimation() {
        completeAnimation();
    }
    
    /**
     * Sets the target screen dimensions.
     * 
     * @param x Screen X position
     * @param y Screen Y position
     * @param width Screen width
     * @param height Screen height
     */
    public void setTargetDimensions(float x, float y, float width, float height) {
        this.targetX = x;
        this.targetY = y;
        this.targetWidth = width;
        this.targetHeight = height;
    }
    
    /**
     * Sets the pending screen to be processed after animation completion.
     * 
     * @param screen The screen to set as pending
     */
    public void setPendingScreen(Object screen) {
        this.pendingScreen = screen;
    }
    
    /**
     * Sets the callback to be called when animation completes.
     * 
     * @param callback The callback to set
     */
    public void setAnimationCallback(AnimationCallback callback) {
        this.animationCallback = callback;
    }

    /**
     * Sets animation duration in milliseconds.
     */
    public void setAnimationDurationMs(long durationMs) {
        this.animationDurationMs = Math.max(1, durationMs);
    }
}
