package jp.moyashi.phoneos.core.app;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PApplet;
import processing.core.PImage;

/**
 * Interface defining the contract that all applications must implement in the MochiMobileOS.
 * This interface provides a standardized way for the OS to interact with applications,
 * including retrieving their metadata, icons, and entry points.
 * 
 * Applications implementing this interface can be dynamically loaded and managed
 * by the OS's application loader system.
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public interface IApplication {
    
    /**
     * Gets the display name of this application.
     * This name will be shown to users in the launcher, app library,
     * and other system interfaces.
     * 
     * @return The human-readable name of the application
     */
    String getName();
    
    /**
     * Gets the icon image for this application.
     * The icon is used throughout the OS interface to represent the application
     * visually in launchers, task switchers, and other system UI elements.
     * 
     * The returned PImage should be square and appropriately sized for display.
     * If no custom icon is available, implementations should return a default
     * or generated icon.
     * 
     * @param p The PApplet instance used for creating the icon image
     * @return A PImage representing the application's icon
     */
    PImage getIcon(PApplet p);
    
    /**
     * Gets the main entry screen for this application.
     * This screen will be displayed when the user launches the application.
     * The screen serves as the primary interface for the application.
     * 
     * The provided Kernel instance allows the application to access OS services
     * such as the VFS, settings manager, system clock, and screen manager.
     * Applications should store this reference to interact with system services.
     * 
     * @param kernel The OS kernel instance providing access to system services
     * @return The main Screen instance for this application
     */
    Screen getEntryScreen(Kernel kernel);
    
    /**
     * Gets the unique identifier for this application.
     * This ID is used internally by the OS for application management,
     * storage organization, and system operations.
     * 
     * The ID should be unique across all applications in the system
     * and should follow reverse domain name notation (e.g., "com.example.myapp").
     * 
     * @return The unique application identifier
     */
    default String getApplicationId() {
        return this.getClass().getPackage().getName() + "." + this.getClass().getSimpleName().toLowerCase();
    }
    
    /**
     * Gets the version string of this application.
     * This version information can be used for update management,
     * compatibility checks, and user information display.
     * 
     * @return The version string of the application (e.g., "1.0.0")
     */
    default String getVersion() {
        return "1.0.0";
    }
    
    /**
     * Gets a brief description of what this application does.
     * This description may be shown in app stores, system information
     * dialogs, or help interfaces.
     * 
     * @return A short description of the application's purpose
     */
    default String getDescription() {
        return "A MochiMobileOS application";
    }
    
    /**
     * Called when the application is being initialized by the OS.
     * Applications can use this method to perform any necessary
     * setup operations that don't require UI interaction.
     * 
     * This method is called before any screens are created or displayed.
     * 
     * @param kernel The OS kernel instance
     */
    default void onInitialize(Kernel kernel) {
        System.out.println("Application " + getName() + " initialized");
    }
    
    /**
     * Called when the application is being shut down by the OS.
     * Applications should use this method to clean up resources,
     * save state, and perform any necessary cleanup operations.
     */
    default void onDestroy() {
        System.out.println("Application " + getName() + " destroyed");
    }
}