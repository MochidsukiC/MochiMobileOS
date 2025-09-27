package jp.moyashi.phoneos.standalone;

import jp.moyashi.phoneos.core.Kernel;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * Main launcher class for the MochiMobileOS standalone application.
 * This class serves as the entry point for running the OS on PC.
 * It initializes the Kernel and displays its PGraphics buffer.
 *
 * @author YourName
 * @version 2.0
 */
public class Main extends PApplet {

    /** MochiMobileOSのカーネルインスタンス */
    private Kernel kernel;

    /** コマンドライン引数で指定されたデバッグモード */
    private static boolean debugMode = false;

    /** コマンドライン引数で指定されたプレイヤーID */
    private static String playerIdString = null;
    
    /**
     * Processing window setup.
     */
    @Override
    public void settings() {
        size(400, 600);  // スマートフォンに似たアスペクト比
        System.out.println("📱 Standalone: Processing window configured (400x600)");
    }

    /**
     * Initialize the kernel and set up the standalone environment.
     */
    @Override
    public void setup() {
        frameRate(60);

        System.out.println("=== MochiMobileOS Standalone Initialization ===");
        if (debugMode) {
            System.out.println("🐛 DEBUG MODE ENABLED");
            if (playerIdString != null) {
                System.out.println("Player ID: " + playerIdString);
            }
        }

        // Initialize kernel
        kernel = new Kernel();
        kernel.initialize(this);

        System.out.println("✅ MochiMobileOS standalone launched successfully!");
    }

    /**
     * Main drawing loop - display the kernel's graphics buffer.
     */
    @Override
    public void draw() {
        background(50);  // デフォルト背景

        if (kernel != null) {
            // カーネルの描画処理を実行
            kernel.draw();

            // カーネルのPGraphicsバッファを取得して表示
            PGraphics kernelGraphics = kernel.getGraphics();
            if (kernelGraphics != null) {
                image(kernelGraphics, 0, 0);
            }
        }
    }

    /**
     * Mouse event forwarding to kernel.
     */
    @Override
    public void mousePressed() {
        if (kernel != null) {
            kernel.mousePressed(mouseX, mouseY);
        }
    }

    @Override
    public void mouseDragged() {
        if (kernel != null) {
            kernel.mouseDragged(mouseX, mouseY);
        }
    }

    @Override
    public void mouseReleased() {
        if (kernel != null) {
            kernel.mouseReleased(mouseX, mouseY);
        }
    }

    @Override
    public void mouseWheel(processing.event.MouseEvent event) {
        if (kernel != null) {
            kernel.mouseWheel((int)event.getCount(), mouseX, mouseY);
        }
    }

    /**
     * Key event forwarding to kernel.
     */
    @Override
    public void keyPressed() {
        if (kernel != null) {
            kernel.keyPressed(key, keyCode, mouseX, mouseY);
        }
    }

    @Override
    public void keyReleased() {
        if (kernel != null) {
            kernel.keyReleased(key, keyCode);
        }
    }

    /**
     * The main entry point of the application.
     * Parses command line arguments and launches the Processing sketch.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args) {
        // Handle command line arguments
        for (String arg : args) {
            switch (arg) {
                case "--help":
                case "-h":
                    displayHelp();
                    return;
                case "--version":
                case "-v":
                    displayVersion();
                    return;
                case "--debug":
                    debugMode = true;
                    break;
                default:
                    if (arg.startsWith("--player=")) {
                        playerIdString = arg.substring("--player=".length());
                    }
                    break;
            }
        }
        
        System.out.println("MochiMobileOS Standalone Launcher");
        System.out.println("==================================");
        System.out.println("Target Resolution: 400x600 (smartphone-like)");
        System.out.println("Processing Version: 4.4.4");
        System.out.println("Architecture: PGraphics Buffer Based");

        if (debugMode) {
            System.out.println("🐛 DEBUG MODE ENABLED");
            if (playerIdString != null) {
                System.out.println("Player ID: " + playerIdString);
            }
        }

        System.out.println();
        System.out.println("Launching MochiMobileOS...");

        // Launch the Processing sketch
        String windowTitle = "MochiMobileOS";
        if (debugMode && playerIdString != null) {
            windowTitle += " - Player: " + playerIdString.substring(0, Math.min(8, playerIdString.length()));
        }

        String[] sketchArgs = new String[]{windowTitle};
        PApplet.runSketch(sketchArgs, new Main());

        // Add shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("MochiMobileOS: Shutting down...");
            System.out.println("MochiMobileOS: Shutdown complete.");
        }));
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