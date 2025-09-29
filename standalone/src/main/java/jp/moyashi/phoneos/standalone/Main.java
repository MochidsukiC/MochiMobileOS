package jp.moyashi.phoneos.standalone;

import processing.core.PApplet;

/**
 * Main launcher class for the MochiMobileOS standalone application.
 * This class serves as the entry point for running the OS on PC.
 * It initializes and launches the StandaloneWrapper as a Processing application window.
 *
 * PGraphics統一アーキテクチャ対応:
 * Main → StandaloneWrapper(PApplet) → Kernel(独立API) → PGraphics
 *
 * @author YourName
 * @version 2.0 (PGraphics統一アーキテクチャ対応)
 */
public class Main {
    
    /**
     * The main entry point of the application.
     * Creates and launches the OS kernel as a Processing sketch window.
     * 
     * @param args Command line arguments (currently unused)
     */
    public static void main(String[] args) {
        // Handle command line arguments
        if (args.length > 0) {
            switch (args[0]) {
                case "--help":
                case "-h":
                    displayHelp();
                    return;
                case "--version":
                case "-v":
                    displayVersion();
                    return;
            }
        }
        
        System.out.println("MochiMobileOS Standalone Launcher");
        System.out.println("==================================");
        System.out.println("Initializing phone OS...");
        System.out.println("Target Resolution: 400x600 (smartphone-like)");
        System.out.println("Processing Version: 4.4.4");
        System.out.println();
        
        try {
            // Create the standalone wrapper (PGraphics統一アーキテクチャ)
            System.out.println("[1/4] Creating StandaloneWrapper...");
            StandaloneWrapper wrapper = new StandaloneWrapper();

            // Wait a moment for initialization
            Thread.sleep(500);

            System.out.println("[2/4] Preparing PApplet→Kernel API conversion...");
            // Kernel初期化はStandaloneWrapper.setup()で実行される

            System.out.println("[3/4] Configuring Processing window...");
            String[] sketchArgs = new String[]{
                StandaloneWrapper.class.getName()
            };

            System.out.println("[4/4] Launching OS window (PGraphics統一アーキテクチャ)...");
            System.out.println("-> StandaloneWrapper(PApplet) → Kernel(独立API) → PGraphics");
            System.out.println("-> LauncherApp will start automatically");
            System.out.println("-> Use mouse/touch to interact");
            System.out.println("-> Long press for edit mode");
            System.out.println("-> Swipe up for App Library");
            System.out.println();

            PApplet.runSketch(sketchArgs, wrapper);
            
            System.out.println("✅ MochiMobileOS launched successfully!");
            System.out.println("   Window should be visible now.");
            
            // Add shutdown hook for cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("MochiMobileOS: Shutting down...");
                // Perform any necessary cleanup here
                System.out.println("MochiMobileOS: Shutdown complete.");
            }));
            
        } catch (InterruptedException e) {
            System.err.println("Initialization interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("❌ Failed to launch MochiMobileOS: " + e.getMessage());
            System.err.println("\nTroubleshooting:");
            System.err.println("1. Check if Processing 4.x is properly installed");
            System.err.println("2. Verify Java version (requires Java 17+)");
            System.err.println("3. Ensure JOGL libraries are available");
            System.err.println("4. Try running with --help for usage information");
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Displays application information and usage.
     * This method can be called with command line arguments for help.
     */
    private static void displayHelp() {
        System.out.println("MochiMobileOS Standalone Application");
        System.out.println("Usage: java -jar MochiMobileOS.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --help     Display this help message");
        System.out.println("  --version  Display version information");
        System.out.println();
        System.out.println("This application launches a smartphone-like OS interface");
        System.out.println("built with Processing for educational and demonstration purposes.");
    }
    
    /**
     * Displays version information.
     */
    private static void displayVersion() {
        System.out.println("MochiMobileOS Version 1.0.0");
        System.out.println("Built with Processing 4.4.4");
        System.out.println("Java Target: 17+ (Forge 1.20.1 compatible)");
        System.out.println("Architecture: Multi-module (core + standalone)");
        System.out.println("Features: Multi-page launcher, drag & drop, app library");
        System.out.println("Copyright (c) 2024 jp.moyashi");
    }
}