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
        
        try {
            // Create the standalone wrapper (PGraphics統一アーキテクチャ)
            StandaloneWrapper wrapper = new StandaloneWrapper();

            // Wait a moment for initialization
            Thread.sleep(500);

            // Kernel初期化はStandaloneWrapper.setup()で実行される

            String[] sketchArgs = new String[]{
                StandaloneWrapper.class.getName()
            };

            PApplet.runSketch(sketchArgs, wrapper);

            // Add shutdown hook for cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                // Perform any necessary cleanup here
            }));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.exit(1);
        } catch (Exception e) {
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