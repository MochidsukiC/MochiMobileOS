package jp.moyashi.phoneos.core.apps.launcher.model;

import jp.moyashi.phoneos.core.app.IApplication;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single page of the home screen in the launcher.
 * Each page contains a grid of shortcut positions and manages
 * the placement, organization, and manipulation of app shortcuts.
 * 
 * The page uses a grid-based layout system where each position
 * can contain at most one shortcut, and empty positions are available
 * for new shortcut placement.
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class HomePage {
    
    /** Grid dimensions for the home page */
    public static final int GRID_COLS = 4;
    public static final int GRID_ROWS = 5;
    public static final int MAX_SHORTCUTS = GRID_COLS * GRID_ROWS;
    
    /** The shortcuts on this page, stored as a 2D array */
    private final Shortcut[][] grid;
    
    /** List of all shortcuts on this page for easy iteration */
    private final List<Shortcut> shortcuts;
    
    /** Custom name for this page (optional) */
    private String pageName;
    
    /** Unique identifier for this page */
    private final String pageId;
    
    /** Static counter for generating unique page IDs */
    private static int nextPageId = 1;
    
    /**
     * Creates a new empty home page.
     */
    public HomePage() {
        this.grid = new Shortcut[GRID_COLS][GRID_ROWS];
        this.shortcuts = new ArrayList<>();
        this.pageName = null;
        this.pageId = "page_" + (nextPageId++);
        
        System.out.println("HomePage: Created new home page with ID " + pageId);
    }
    
    /**
     * Creates a new home page with a custom name.
     * 
     * @param pageName The name for this page
     */
    public HomePage(String pageName) {
        this();
        this.pageName = pageName;
    }
    
    /**
     * Gets the unique identifier for this page.
     * 
     * @return The page ID
     */
    public String getPageId() {
        return pageId;
    }
    
    /**
     * Gets the custom name for this page.
     * 
     * @return The page name, or null if no custom name is set
     */
    public String getPageName() {
        return pageName;
    }
    
    /**
     * Sets the custom name for this page.
     * 
     * @param pageName The new page name, or null to clear
     */
    public void setPageName(String pageName) {
        this.pageName = pageName;
    }
    
    /**
     * Gets the display name for this page.
     * If a custom name is set, returns that; otherwise returns a default name.
     * 
     * @return The name to display for this page
     */
    public String getDisplayName() {
        return pageName != null ? pageName : "Page " + pageId.substring(5);
    }
    
    /**
     * Gets a list of all shortcuts on this page.
     * 
     * @return An unmodifiable view of the shortcuts list
     */
    public List<Shortcut> getShortcuts() {
        return new ArrayList<>(shortcuts);
    }
    
    /**
     * Gets the number of shortcuts currently on this page.
     * 
     * @return The shortcut count
     */
    public int getShortcutCount() {
        return shortcuts.size();
    }
    
    /**
     * Checks if this page is full (no empty positions).
     * 
     * @return true if the page is at maximum capacity, false otherwise
     */
    public boolean isFull() {
        return shortcuts.size() >= MAX_SHORTCUTS;
    }
    
    /**
     * Checks if this page is empty (no shortcuts).
     * 
     * @return true if the page has no shortcuts, false otherwise
     */
    public boolean isEmpty() {
        return shortcuts.isEmpty();
    }
    
    /**
     * Gets the shortcut at the specified grid position.
     * 
     * @param gridX The grid column (0-based)
     * @param gridY The grid row (0-based)
     * @return The shortcut at that position, or null if empty or invalid position
     */
    public Shortcut getShortcutAt(int gridX, int gridY) {
        if (!isValidPosition(gridX, gridY)) {
            return null;
        }
        return grid[gridX][gridY];
    }
    
    /**
     * Checks if the specified grid position is valid.
     * 
     * @param gridX The grid column
     * @param gridY The grid row
     * @return true if the position is within bounds
     */
    public boolean isValidPosition(int gridX, int gridY) {
        return gridX >= 0 && gridX < GRID_COLS && gridY >= 0 && gridY < GRID_ROWS;
    }
    
    /**
     * Checks if the specified grid position is empty.
     * 
     * @param gridX The grid column
     * @param gridY The grid row
     * @return true if the position is valid and empty
     */
    public boolean isPositionEmpty(int gridX, int gridY) {
        return isValidPosition(gridX, gridY) && grid[gridX][gridY] == null;
    }
    
    /**
     * Finds the next available empty position on this page.
     * Searches from left to right, top to bottom.
     * 
     * @return An array [x, y] of the next empty position, or null if page is full
     */
    public int[] findNextEmptyPosition() {
        for (int y = 0; y < GRID_ROWS; y++) {
            for (int x = 0; x < GRID_COLS; x++) {
                if (grid[x][y] == null) {
                    return new int[]{x, y};
                }
            }
        }
        return null; // Page is full
    }
    
    /**
     * Adds a shortcut to this page at the specified position.
     * 
     * @param shortcut The shortcut to add
     * @param gridX The target grid column
     * @param gridY The target grid row
     * @return true if the shortcut was added successfully, false if position is occupied or invalid
     */
    public boolean addShortcut(Shortcut shortcut, int gridX, int gridY) {
        if (shortcut == null || !isPositionEmpty(gridX, gridY)) {
            return false;
        }
        
        // Remove from old position if it was already on this page
        removeShortcut(shortcut);
        
        // Add to new position
        grid[gridX][gridY] = shortcut;
        shortcut.setGridPosition(gridX, gridY);
        shortcuts.add(shortcut);
        
        System.out.println("HomePage: Added shortcut " + shortcut.getDisplayName() + 
                          " to position (" + gridX + ", " + gridY + ") on page " + pageId);
        return true;
    }
    
    /**
     * Adds a shortcut to this page at the next available position.
     * 
     * @param shortcut The shortcut to add
     * @return true if the shortcut was added successfully, false if page is full
     */
    public boolean addShortcut(Shortcut shortcut) {
        int[] position = findNextEmptyPosition();
        if (position == null) {
            return false; // Page is full
        }
        return addShortcut(shortcut, position[0], position[1]);
    }
    
    /**
     * Creates and adds a new shortcut for the specified application.
     * 
     * @param application The application to create a shortcut for
     * @return true if the shortcut was created and added successfully
     */
    public boolean addShortcut(IApplication application) {
        if (application == null) {
            return false;
        }
        Shortcut shortcut = new Shortcut(application);
        return addShortcut(shortcut);
    }
    
    /**
     * Creates and adds a new shortcut for the specified application at the specified position.
     * 
     * @param application The application to create a shortcut for
     * @param gridX The target grid column
     * @param gridY The target grid row
     * @return true if the shortcut was created and added successfully
     */
    public boolean addShortcut(IApplication application, int gridX, int gridY) {
        if (application == null) {
            return false;
        }
        Shortcut shortcut = new Shortcut(application, gridX, gridY);
        return addShortcut(shortcut, gridX, gridY);
    }
    
    /**
     * Removes a shortcut from this page.
     * 
     * @param shortcut The shortcut to remove
     * @return true if the shortcut was removed, false if it wasn't on this page
     */
    public boolean removeShortcut(Shortcut shortcut) {
        if (shortcut == null || !shortcuts.contains(shortcut)) {
            return false;
        }
        
        // Clear from grid
        int gridX = shortcut.getGridX();
        int gridY = shortcut.getGridY();
        if (isValidPosition(gridX, gridY) && grid[gridX][gridY] == shortcut) {
            grid[gridX][gridY] = null;
        }
        
        // Remove from list
        shortcuts.remove(shortcut);
        
        System.out.println("HomePage: Removed shortcut " + shortcut.getDisplayName() + 
                          " from page " + pageId);
        return true;
    }
    
    /**
     * Removes the shortcut at the specified position.
     * 
     * @param gridX The grid column
     * @param gridY The grid row
     * @return The removed shortcut, or null if position was empty or invalid
     */
    public Shortcut removeShortcutAt(int gridX, int gridY) {
        Shortcut shortcut = getShortcutAt(gridX, gridY);
        if (shortcut != null) {
            removeShortcut(shortcut);
        }
        return shortcut;
    }
    
    /**
     * Moves a shortcut from one position to another on this page.
     * 
     * @param shortcut The shortcut to move
     * @param newGridX The target grid column
     * @param newGridY The target grid row
     * @return true if the shortcut was moved successfully
     */
    public boolean moveShortcut(Shortcut shortcut, int newGridX, int newGridY) {
        if (shortcut == null || !shortcuts.contains(shortcut) || !isValidPosition(newGridX, newGridY)) {
            return false;
        }
        
        // Check if target position is empty or contains the same shortcut
        Shortcut existingShortcut = grid[newGridX][newGridY];
        if (existingShortcut != null && existingShortcut != shortcut) {
            return false; // Position occupied by different shortcut
        }
        
        // Clear old position
        int oldX = shortcut.getGridX();
        int oldY = shortcut.getGridY();
        if (isValidPosition(oldX, oldY)) {
            grid[oldX][oldY] = null;
        }
        
        // Set new position
        grid[newGridX][newGridY] = shortcut;
        shortcut.setGridPosition(newGridX, newGridY);
        
        System.out.println("HomePage: Moved shortcut " + shortcut.getDisplayName() + 
                          " from (" + oldX + ", " + oldY + ") to (" + newGridX + ", " + newGridY + ")");
        return true;
    }
    
    /**
     * Swaps the positions of two shortcuts on this page.
     * 
     * @param shortcut1 The first shortcut
     * @param shortcut2 The second shortcut
     * @return true if the shortcuts were swapped successfully
     */
    public boolean swapShortcuts(Shortcut shortcut1, Shortcut shortcut2) {
        if (shortcut1 == null || shortcut2 == null || 
            !shortcuts.contains(shortcut1) || !shortcuts.contains(shortcut2)) {
            return false;
        }
        
        int x1 = shortcut1.getGridX();
        int y1 = shortcut1.getGridY();
        int x2 = shortcut2.getGridX();
        int y2 = shortcut2.getGridY();
        
        // Swap positions in grid
        grid[x1][y1] = shortcut2;
        grid[x2][y2] = shortcut1;
        
        // Update shortcut positions
        shortcut1.setGridPosition(x2, y2);
        shortcut2.setGridPosition(x1, y1);
        
        System.out.println("HomePage: Swapped shortcuts " + shortcut1.getDisplayName() + 
                          " and " + shortcut2.getDisplayName());
        return true;
    }
    
    /**
     * Finds a shortcut that references the specified application.
     * 
     * @param application The application to search for
     * @return The first shortcut found that references the application, or null if none found
     */
    public Shortcut findShortcutForApplication(IApplication application) {
        if (application == null) {
            return null;
        }
        
        return shortcuts.stream()
                .filter(shortcut -> shortcut.referencesApplication(application))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Checks if this page contains a shortcut for the specified application.
     * 
     * @param application The application to check for
     * @return true if a shortcut exists for this application on this page
     */
    public boolean hasShortcutForApplication(IApplication application) {
        return findShortcutForApplication(application) != null;
    }
    
    /**
     * Compacts the page by moving all shortcuts to fill empty spaces.
     * Shortcuts are moved to the earliest available positions, maintaining their relative order.
     */
    public void compact() {
        List<Shortcut> allShortcuts = new ArrayList<>(shortcuts);
        
        // Clear the page
        clear();
        
        // Re-add shortcuts to fill from the top-left
        for (Shortcut shortcut : allShortcuts) {
            addShortcut(shortcut);
        }
        
        System.out.println("HomePage: Compacted page " + pageId + ", now has " + shortcuts.size() + " shortcuts");
    }
    
    /**
     * Clears all shortcuts from this page.
     */
    public void clear() {
        // Clear grid
        for (int x = 0; x < GRID_COLS; x++) {
            for (int y = 0; y < GRID_ROWS; y++) {
                grid[x][y] = null;
            }
        }
        
        // Clear shortcuts list
        shortcuts.clear();
        
        System.out.println("HomePage: Cleared all shortcuts from page " + pageId);
    }
    
    /**
     * Returns a string representation of this home page.
     * 
     * @return A string describing this page
     */
    @Override
    public String toString() {
        return "HomePage{" +
                "id=" + pageId +
                ", name=" + pageName +
                ", shortcuts=" + shortcuts.size() + "/" + MAX_SHORTCUTS +
                '}';
    }
}