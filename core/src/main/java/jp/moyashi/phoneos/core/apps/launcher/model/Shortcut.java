package jp.moyashi.phoneos.core.apps.launcher.model;

import jp.moyashi.phoneos.core.app.IApplication;

/**
 * Represents a shortcut to an application on the home screen.
 * A shortcut contains a reference to the application it represents,
 * along with positioning and customization information.
 * 
 * Shortcuts can be placed at specific grid positions on home screen pages
 * and may have custom display properties different from the original application.
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class Shortcut {
    
    /** The application this shortcut references */
    private final IApplication application;
    
    /** Grid column position (0-based) */
    private int gridX;
    
    /** Grid row position (0-based) */
    private int gridY;
    
    /** Custom display name for the shortcut (null to use app name) */
    private String customName;
    
    /** Whether this shortcut is currently being dragged */
    private boolean isDragging;
    
    /** Temporary position while dragging (screen coordinates) */
    private float dragX;
    private float dragY;
    
    /** Animation offset for edit mode wiggle effect */
    private float wiggleOffset;
    
    /** Unique identifier for this shortcut */
    private final String shortcutId;
    
    /** Static counter for generating unique shortcut IDs */
    private static int nextId = 1;
    
    /**
     * Creates a new shortcut for the specified application.
     * 
     * @param application The application this shortcut will reference
     * @param gridX The initial grid column position
     * @param gridY The initial grid row position
     */
    public Shortcut(IApplication application, int gridX, int gridY) {
        if (application == null) {
            throw new IllegalArgumentException("Application cannot be null");
        }
        
        this.application = application;
        this.gridX = gridX;
        this.gridY = gridY;
        this.customName = null;
        this.isDragging = false;
        this.dragX = 0;
        this.dragY = 0;
        this.wiggleOffset = 0;
        this.shortcutId = "shortcut_" + (nextId++);
        
        System.out.println("Shortcut: Created shortcut for " + application.getName() + 
                          " at position (" + gridX + ", " + gridY + ")");
    }
    
    /**
     * Creates a new shortcut for the specified application at position (0,0).
     * 
     * @param application The application this shortcut will reference
     */
    public Shortcut(IApplication application) {
        this(application, 0, 0);
    }
    
    /**
     * Gets the application this shortcut references.
     * 
     * @return The IApplication instance
     */
    public IApplication getApplication() {
        return application;
    }
    
    /**
     * Gets the grid column position of this shortcut.
     * 
     * @return The grid X coordinate (0-based)
     */
    public int getGridX() {
        return gridX;
    }
    
    /**
     * Sets the grid column position of this shortcut.
     * 
     * @param gridX The new grid X coordinate (0-based)
     */
    public void setGridX(int gridX) {
        this.gridX = gridX;
    }
    
    /**
     * Gets the grid row position of this shortcut.
     * 
     * @return The grid Y coordinate (0-based)
     */
    public int getGridY() {
        return gridY;
    }
    
    /**
     * Sets the grid row position of this shortcut.
     * 
     * @param gridY The new grid Y coordinate (0-based)
     */
    public void setGridY(int gridY) {
        this.gridY = gridY;
    }
    
    /**
     * Sets the grid position of this shortcut.
     * 
     * @param gridX The new grid X coordinate (0-based)
     * @param gridY The new grid Y coordinate (0-based)
     */
    public void setGridPosition(int gridX, int gridY) {
        this.gridX = gridX;
        this.gridY = gridY;
    }
    
    /**
     * Gets the display name for this shortcut.
     * If a custom name is set, returns that; otherwise returns the application name.
     * 
     * @return The name to display for this shortcut
     */
    public String getDisplayName() {
        return customName != null ? customName : application.getName();
    }
    
    /**
     * Gets the custom display name set for this shortcut.
     * 
     * @return The custom name, or null if using the application's default name
     */
    public String getCustomName() {
        return customName;
    }
    
    /**
     * Sets a custom display name for this shortcut.
     * 
     * @param customName The custom name, or null to use the application's name
     */
    public void setCustomName(String customName) {
        this.customName = customName;
    }
    
    /**
     * Checks if this shortcut is currently being dragged.
     * 
     * @return true if the shortcut is being dragged, false otherwise
     */
    public boolean isDragging() {
        return isDragging;
    }
    
    /**
     * Sets the dragging state of this shortcut.
     * 
     * @param dragging true to mark as being dragged, false otherwise
     */
    public void setDragging(boolean dragging) {
        this.isDragging = dragging;
    }
    
    /**
     * Gets the current drag X position (screen coordinates).
     * 
     * @return The drag X position
     */
    public float getDragX() {
        return dragX;
    }
    
    /**
     * Sets the current drag X position (screen coordinates).
     * 
     * @param dragX The new drag X position
     */
    public void setDragX(float dragX) {
        this.dragX = dragX;
    }
    
    /**
     * Gets the current drag Y position (screen coordinates).
     * 
     * @return The drag Y position
     */
    public float getDragY() {
        return dragY;
    }
    
    /**
     * Sets the current drag Y position (screen coordinates).
     * 
     * @param dragY The new drag Y position
     */
    public void setDragY(float dragY) {
        this.dragY = dragY;
    }
    
    /**
     * Sets the drag position (screen coordinates).
     * 
     * @param dragX The new drag X position
     * @param dragY The new drag Y position
     */
    public void setDragPosition(float dragX, float dragY) {
        this.dragX = dragX;
        this.dragY = dragY;
    }
    
    /**
     * Gets the current wiggle animation offset for edit mode.
     * 
     * @return The wiggle offset value
     */
    public float getWiggleOffset() {
        return wiggleOffset;
    }
    
    /**
     * Sets the wiggle animation offset for edit mode.
     * 
     * @param wiggleOffset The new wiggle offset value
     */
    public void setWiggleOffset(float wiggleOffset) {
        this.wiggleOffset = wiggleOffset;
    }
    
    /**
     * Gets the unique identifier for this shortcut.
     * 
     * @return The shortcut ID
     */
    public String getShortcutId() {
        return shortcutId;
    }
    
    /**
     * Checks if this shortcut references the same application as another shortcut.
     * 
     * @param other The other shortcut to compare
     * @return true if both shortcuts reference the same application
     */
    public boolean referencesApplication(Shortcut other) {
        if (other == null) return false;
        return application.getApplicationId().equals(other.application.getApplicationId());
    }
    
    /**
     * Checks if this shortcut references the specified application.
     * 
     * @param app The application to check
     * @return true if this shortcut references the specified application
     */
    public boolean referencesApplication(IApplication app) {
        if (app == null) return false;
        return application.getApplicationId().equals(app.getApplicationId());
    }
    
    /**
     * Creates a copy of this shortcut with the same application reference
     * but potentially different position and properties.
     * 
     * @param newGridX The grid X position for the copy
     * @param newGridY The grid Y position for the copy
     * @return A new Shortcut instance
     */
    public Shortcut createCopy(int newGridX, int newGridY) {
        Shortcut copy = new Shortcut(application, newGridX, newGridY);
        copy.setCustomName(customName);
        return copy;
    }
    
    /**
     * Returns a string representation of this shortcut.
     * 
     * @return A string describing this shortcut
     */
    @Override
    public String toString() {
        return "Shortcut{" +
                "app=" + application.getName() +
                ", pos=(" + gridX + "," + gridY + ")" +
                ", dragging=" + isDragging +
                ", id=" + shortcutId +
                '}';
    }
    
    /**
     * Checks if this shortcut is equal to another object.
     * Two shortcuts are considered equal if they have the same shortcut ID.
     * 
     * @param obj The object to compare
     * @return true if the objects are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Shortcut shortcut = (Shortcut) obj;
        return shortcutId.equals(shortcut.shortcutId);
    }
    
    /**
     * Returns the hash code for this shortcut.
     * 
     * @return The hash code based on the shortcut ID
     */
    @Override
    public int hashCode() {
        return shortcutId.hashCode();
    }
}