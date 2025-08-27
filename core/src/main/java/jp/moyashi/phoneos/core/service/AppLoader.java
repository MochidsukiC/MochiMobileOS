package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.app.IApplication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Application loader service responsible for discovering, loading, and managing
 * applications within the MochiMobileOS environment.
 * 
 * This service scans the virtual file system for application packages (JAR files)
 * in the /apps/ directory, dynamically loads them, and maintains a registry of
 * available applications that can be launched by the user.
 * 
 * The AppLoader works in conjunction with the VFS to provide a plugin-style
 * architecture where applications can be installed and removed at runtime.
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class AppLoader {
    
    /** Virtual file system instance for accessing application files */
    private final VFS vfs;
    
    /** List of successfully loaded applications */
    private final List<IApplication> loadedApps;
    
    /** Flag indicating whether apps have been scanned */
    private boolean hasScannedApps;
    
    /**
     * Constructs a new AppLoader service instance.
     * 
     * @param vfs The virtual file system service for accessing application files
     */
    public AppLoader(VFS vfs) {
        this.vfs = vfs;
        this.loadedApps = new ArrayList<>();
        this.hasScannedApps = false;
        
        System.out.println("AppLoader: Application loader service initialized");
    }
    
    /**
     * Scans the VFS /apps/ directory for application packages and attempts to load them.
     * This method looks for .jar files that contain classes implementing the IApplication
     * interface and dynamically loads them into the application registry.
     * 
     * The scanning process involves:
     * 1. Querying the VFS for files in /apps/ directory
     * 2. Filtering for .jar files
     * 3. Using reflection to load application classes
     * 4. Instantiating applications that implement IApplication
     * 5. Adding valid applications to the loaded apps list
     * 
     * This is a placeholder implementation that will be expanded when the VFS
     * supports actual file system operations.
     */
    public void scanForApps() {
        System.out.println("AppLoader: Scanning /apps/ directory for applications...");
        
        if (hasScannedApps) {
            System.out.println("AppLoader: Apps already scanned, skipping");
            return;
        }
        
        try {
            // TODO: Implement actual file system scanning when VFS is fully implemented
            // For now, this is a placeholder that logs the scanning operation
            
            // Future implementation will include:
            // 1. List<String> jarFiles = vfs.listFiles("/apps/", "*.jar");
            // 2. For each JAR file, create a ClassLoader
            // 3. Search for classes implementing IApplication
            // 4. Instantiate and add to loadedApps list
            
            System.out.println("AppLoader: Scanning complete. Found " + loadedApps.size() + " applications");
            hasScannedApps = true;
            
        } catch (Exception e) {
            System.err.println("AppLoader: Error during application scanning: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Returns an immutable list of all successfully loaded applications.
     * Applications are returned in the order they were loaded during the scan process.
     * 
     * @return An immutable list of loaded IApplication instances
     */
    public List<IApplication> getLoadedApps() {
        return Collections.unmodifiableList(loadedApps);
    }
    
    /**
     * Gets the number of currently loaded applications.
     * 
     * @return The count of loaded applications
     */
    public int getLoadedAppCount() {
        return loadedApps.size();
    }
    
    /**
     * Finds an application by its unique application ID.
     * 
     * @param applicationId The unique identifier of the application to find
     * @return The IApplication instance with the matching ID, or null if not found
     */
    public IApplication findApplicationById(String applicationId) {
        return loadedApps.stream()
                .filter(app -> app.getApplicationId().equals(applicationId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Finds an application by its display name.
     * Note that display names are not guaranteed to be unique, so this method
     * returns the first matching application found.
     * 
     * @param name The display name of the application to find
     * @return The first IApplication instance with the matching name, or null if not found
     */
    public IApplication findApplicationByName(String name) {
        return loadedApps.stream()
                .filter(app -> app.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Manually registers an application with the loader.
     * This method is useful for built-in applications that don't need to be
     * loaded from JAR files, such as the system launcher.
     * 
     * @param application The application instance to register
     * @return true if the application was successfully registered, false if it was already registered
     */
    public boolean registerApplication(IApplication application) {
        if (application == null) {
            System.err.println("AppLoader: Cannot register null application");
            return false;
        }
        
        // Check if already registered
        if (findApplicationById(application.getApplicationId()) != null) {
            System.out.println("AppLoader: Application " + application.getName() + " already registered");
            return false;
        }
        
        loadedApps.add(application);
        System.out.println("AppLoader: Registered application: " + application.getName() + 
                          " (ID: " + application.getApplicationId() + ")");
        return true;
    }
    
    /**
     * Unregisters an application from the loader.
     * 
     * @param applicationId The unique identifier of the application to unregister
     * @return true if the application was successfully unregistered, false if not found
     */
    public boolean unregisterApplication(String applicationId) {
        IApplication app = findApplicationById(applicationId);
        if (app != null) {
            loadedApps.remove(app);
            System.out.println("AppLoader: Unregistered application: " + app.getName());
            return true;
        }
        return false;
    }
    
    /**
     * Refreshes the application list by re-scanning the /apps/ directory.
     * This method clears the current loaded applications and performs a fresh scan.
     */
    public void refreshApps() {
        System.out.println("AppLoader: Refreshing application list...");
        
        // Don't clear built-in apps, only those loaded from files
        // In a full implementation, we would differentiate between
        // file-loaded and manually-registered apps
        
        hasScannedApps = false;
        scanForApps();
    }
    
    /**
     * Checks if the application scanning has been performed.
     * 
     * @return true if apps have been scanned, false otherwise
     */
    public boolean hasScannedForApps() {
        return hasScannedApps;
    }
    
    /**
     * Gets the VFS instance used by this AppLoader.
     * 
     * @return The VFS instance
     */
    public VFS getVFS() {
        return vfs;
    }
}