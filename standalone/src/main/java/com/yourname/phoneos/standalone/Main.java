package com.yourname.phoneos.standalone;

import com.yourname.phoneos.core.Kernel;
import processing.core.PApplet;

/**
 * Main launcher class for the MochiMobileOS standalone application.
 * This class serves as the entry point for running the OS on PC.
 * It initializes and launches the Kernel as a Processing application window.
 * 
 * @author YourName
 * @version 1.0
 */
public class Main {
    
    /**
     * The main entry point of the application.
     * Creates and launches the OS kernel as a Processing sketch window.
     * 
     * @param args Command line arguments (currently unused)
     */
    public static void main(String[] args) {
        System.out.println("MochiMobileOS Standalone Launcher");
        System.out.println("==================================");
        System.out.println("Initializing phone OS...");
        
        try {
            // Create the kernel instance
            Kernel kernel = new Kernel();
            
            // Configure the application window - simplified for compatibility
            String[] sketchArgs = new String[]{
                Kernel.class.getName()
            };
            
            // Launch the Processing sketch
            System.out.println("Launching OS window...");
            PApplet.runSketch(sketchArgs, kernel);
            
            // Add shutdown hook for cleanup
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("MochiMobileOS: Shutting down...");
                // Perform any necessary cleanup here
                System.out.println("MochiMobileOS: Shutdown complete.");
            }));
            
        } catch (Exception e) {
            System.err.println("Failed to launch MochiMobileOS: " + e.getMessage());
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
        System.out.println("MochiMobileOS Version 1.0");
        System.out.println("Built with Processing 3.5.4");
        System.out.println("Copyright (c) 2024 YourName");
    }
}