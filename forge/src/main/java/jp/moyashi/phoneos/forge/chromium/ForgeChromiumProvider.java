package jp.moyashi.phoneos.forge.chromium;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.chromium.ChromiumAppHandler;
import jp.moyashi.phoneos.core.service.chromium.JCEFChromiumProvider;
import jp.moyashi.phoneos.forge.installer.MMOSInstallListener;
import jp.moyashi.phoneos.forge.installer.MMOSInstallerSettings;
import jp.moyashi.phoneos.forge.installer.MMOSPlatform;
import org.cef.CefApp;
import org.cef.CefSettings;
import org.cef.SystemBootstrap;
import org.cef.browser.CefBrowserFactory;
import org.cef.browser.CefBrowserOsrNoCanvas;
import org.cef.callback.CefSchemeRegistrar;
import org.cef.handler.CefAppHandler;
import org.cef.handler.CefAppHandlerAdapter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Forge環境専用のChromiumProvider。
 * jcefmavenのbuild()を完全にバイパスし、直接CefAppを初期化する。
 *
 * 問題:
 * - jcefmavenのbuild()内部でApache Commons Compressが使用される
 * - ForgeのクラスローダーにはMinecraft付属のCommons Compressがあり、バージョン不一致が発生
 * - TarArchiveInputStream.getNextEntry()がNoSuchMethodErrorになる
 *
 * 解決策:
 * - MMOSInstallerが独自にネイティブをダウンロード・展開済み
 * - このプロバイダーでCefApp.getInstance()を直接呼び出し、jcefmavenをバイパス
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class ForgeChromiumProvider extends JCEFChromiumProvider {

    private static final String TAG = "ForgeChromiumProvider";
    private static final Logger LOGGER = LogManager.getLogger(TAG);

    /** コンソールメッセージリスナー */
    private ConsoleMessageListener consoleListener;

    /** SystemBootstrapの静的初期化フラグ */
    private static volatile boolean systemBootstrapConfigured = false;

    /**
     * 静的初期化: CefAppが使用される前にSystemBootstrapのカスタムローダーを設定する。
     * これはクラスがロードされた時点で実行される。
     */
    static {
        try {
            configureSystemBootstrapEarly();
        } catch (Exception e) {
            System.err.println("[" + TAG + "] Failed to configure SystemBootstrap in static initializer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * SystemBootstrapを早期に設定する（静的メソッド）。
     */
    private static void configureSystemBootstrapEarly() {
        if (systemBootstrapConfigured) {
            return;
        }

        // jcef.pathからJCEFパスを取得
        String jcefPathStr = System.getProperty("jcef.path");
        if (jcefPathStr == null || jcefPathStr.isEmpty()) {
            System.out.println("[" + TAG + "] jcef.path not set yet, skipping early SystemBootstrap configuration");
            return;
        }

        Path jcefPath = Path.of(jcefPathStr);
        if (!Files.exists(jcefPath)) {
            System.out.println("[" + TAG + "] JCEF path does not exist: " + jcefPath);
            return;
        }

        // プラットフォーム検出
        String osName = System.getProperty("os.name").toLowerCase();
        final boolean isWindows = osName.contains("win");
        final boolean isLinux = osName.contains("linux");
        final boolean isMacOS = osName.contains("mac");

        // カスタムローダーを設定
        SystemBootstrap.setLoader(new SystemBootstrap.Loader() {
            @Override
            public void loadLibrary(String libName) {
                String fileName;
                if (isWindows) {
                    fileName = libName + ".dll";
                } else if (isLinux) {
                    fileName = "lib" + libName + ".so";
                } else if (isMacOS) {
                    fileName = "lib" + libName + ".dylib";
                } else {
                    fileName = libName;
                }

                File libFile = jcefPath.resolve(fileName).toFile();
                if (libFile.exists()) {
                    System.out.println("[" + TAG + "] SystemBootstrap loading: " + libFile.getAbsolutePath());
                    System.load(libFile.getAbsolutePath());
                } else {
                    System.out.println("[" + TAG + "] Library not found, falling back: " + libName + " (tried: " + libFile.getAbsolutePath() + ")");
                    System.loadLibrary(libName);
                }
            }
        });

        systemBootstrapConfigured = true;
        System.out.println("[" + TAG + "] SystemBootstrap configured early with JCEF path: " + jcefPath);
    }

    @Override
    public CefApp createCefApp(Kernel kernel) {
        try {
            log("Initializing JCEF for Forge environment (bypassing jcefmaven)...");

            // MMOSInstallerの完了を待機
            if (!waitForInstaller()) {
                throw new RuntimeException("MMOS Installer did not complete successfully");
            }

            // プラットフォーム検出
            MMOSPlatform platform = MMOSPlatform.detect();
            if (platform == MMOSPlatform.UNKNOWN) {
                throw new RuntimeException("Unsupported platform");
            }

            // JCEFパスを取得
            MMOSInstallerSettings settings = MMOSInstallerSettings.getInstance();
            Path jcefPath = settings.getJcefPath(platform);

            // ネイティブライブラリの存在確認
            if (!verifyNatives(jcefPath, platform)) {
                throw new RuntimeException("JCEF native libraries not found at: " + jcefPath);
            }

            log("JCEF path: " + jcefPath.toAbsolutePath());

            // jcef.pathシステムプロパティを設定
            System.setProperty("jcef.path", jcefPath.toAbsolutePath().toString());

            // java.library.pathにJCEFパスを追加（CefAppがSystem.loadLibrary()を使用するため必要）
            addToJavaLibraryPath(jcefPath.toAbsolutePath().toString());

            // SystemBootstrapのカスタムローダーを設定
            // これによりCefApp内部のSystem.loadLibrary()呼び出しを
            // System.load()（絶対パス指定）に置き換える
            setupSystemBootstrapLoader(jcefPath, platform);

            // ネイティブライブラリをロード
            loadNativeLibraries(jcefPath, platform);

            // CefSettingsを設定
            CefSettings cefSettings = new CefSettings();

            // キャッシュパス（VFS内）
            String cachePath = kernel.getVFS().getFullPath("system/browser_chromium/cache");
            cefSettings.cache_path = cachePath;
            log("Cache path: " + cachePath);

            // 重要: サブプロセス（jcef_helper.exe）のパスを設定
            // これがないとCEFの初期化時にクラッシュする
            Path helperPath;
            if (platform.isWindows()) {
                helperPath = jcefPath.resolve("jcef_helper.exe");
            } else if (platform.isMacOS()) {
                helperPath = jcefPath.resolve("jcef_app.app/Contents/Frameworks/jcef Helper.app/Contents/MacOS/jcef Helper");
            } else {
                helperPath = jcefPath.resolve("jcef_helper");
            }
            if (Files.exists(helperPath)) {
                cefSettings.browser_subprocess_path = helperPath.toAbsolutePath().toString();
                log("Subprocess path: " + cefSettings.browser_subprocess_path);
            } else {
                log("WARNING: jcef_helper not found at: " + helperPath);
            }

            // リソースディレクトリ（CEFリソースファイルがある場所）
            cefSettings.resources_dir_path = jcefPath.toAbsolutePath().toString();
            log("Resources path: " + cefSettings.resources_dir_path);

            // localesディレクトリ（言語ファイルはルートディレクトリにある場合もある）
            Path localesPath = jcefPath.resolve("locales");
            boolean localesHasContent = false;
            try {
                localesHasContent = Files.exists(localesPath) && Files.list(localesPath).findAny().isPresent();
            } catch (java.io.IOException e) {
                log("Could not check locales directory: " + e.getMessage());
            }
            if (localesHasContent) {
                // localesディレクトリに内容がある場合
                cefSettings.locales_dir_path = localesPath.toAbsolutePath().toString();
                log("Locales path (from locales dir): " + cefSettings.locales_dir_path);
            } else {
                // 言語ファイルがルートディレクトリにある場合
                cefSettings.locales_dir_path = jcefPath.toAbsolutePath().toString();
                log("Locales path (from root): " + cefSettings.locales_dir_path);
            }

            // User-Agent（モバイル最適化）
            cefSettings.user_agent = "Mozilla/5.0 (Linux; Android 12; MochiMobileOS) " +
                                     "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                     "Chrome/122.0.0.0 Mobile Safari/537.36";

            // ロケール
            cefSettings.locale = "ja";

            // ログレベル
            cefSettings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_WARNING;

            // デバッグ用ログファイル
            cefSettings.log_file = cachePath + "/cef_debug.log";

            // オフスクリーンレンダリング有効化
            cefSettings.windowless_rendering_enabled = true;

            // GPU無効化（Minecraftとの競合を避ける）
            cefSettings.command_line_args_disabled = false;

            // ChromiumAppHandlerを作成（coreモジュール）
            ChromiumAppHandler coreAppHandler = new ChromiumAppHandler(kernel);

            // CefAppHandlerAdapterでラップ
            CefAppHandlerAdapter appHandler = new CefAppHandlerAdapter(null) {
                @Override
                public void onRegisterCustomSchemes(CefSchemeRegistrar registrar) {
                    coreAppHandler.onRegisterCustomSchemes(registrar);
                }

                @Override
                public void onContextInitialized() {
                    coreAppHandler.onContextInitialized();
                }
            };

            // コマンドライン引数を構築
            String[] args = buildCefArgs(platform);

            // NoCanvasモードを有効化（AWTを使わない）
            log("Enabling NoCanvas mode (AWT-free rendering)...");
            CefBrowserFactory.setNoCanvasMode(true);

            // CefApp.startup()を最初に呼び出す（JCEFの初期化に必要）
            log("Calling CefApp.startup()...");
            if (!CefApp.startup(args)) {
                throw new RuntimeException("CefApp.startup() failed");
            }
            log("CefApp.startup() succeeded");

            // AppHandlerを事前に登録
            CefApp.addAppHandler(appHandler);

            // CefAppを直接初期化（jcefmavenのbuild()をバイパス）
            log("Creating CefApp directly (bypassing jcefmaven.build())...");
            CefApp cefApp = CefApp.getInstance(args, cefSettings);

            log("JCEF initialized successfully");
            log("Chromium version: " + cefApp.getVersion());

            return cefApp;

        } catch (Exception e) {
            logError("Failed to initialize JCEF: " + e.getMessage(), e);
            throw new RuntimeException("Failed to initialize JCEF for Forge", e);
        }
    }

    /**
     * MMOSInstallerの完了を待機する。
     */
    private boolean waitForInstaller() {
        MMOSInstallListener listener = MMOSInstallListener.INSTANCE;

        // 最大60秒待機
        int maxWaitSeconds = 60;
        int waitedSeconds = 0;

        while (!listener.isDone() && !listener.isFailed() && waitedSeconds < maxWaitSeconds) {
            try {
                log("Waiting for MMOS Installer... (" + listener.getTask() + ")");
                Thread.sleep(1000);
                waitedSeconds++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }

        if (listener.isFailed()) {
            logError("MMOS Installer failed: " + listener.getErrorMessage(), null);
            return false;
        }

        if (!listener.isDone()) {
            logError("MMOS Installer timed out after " + maxWaitSeconds + " seconds", null);
            return false;
        }

        log("MMOS Installer completed successfully");
        return true;
    }

    /**
     * ネイティブライブラリの存在を確認する。
     */
    private boolean verifyNatives(Path jcefPath, MMOSPlatform platform) {
        if (platform.isWindows()) {
            // Windowsでは jcef.dll と libcef.dll をチェック
            boolean hasJcef = Files.exists(jcefPath.resolve("jcef.dll"));
            boolean hasLibcef = Files.exists(jcefPath.resolve("libcef.dll"));
            log("Native check: jcef.dll=" + hasJcef + ", libcef.dll=" + hasLibcef);
            return hasJcef || hasLibcef;
        } else if (platform.isLinux()) {
            return Files.exists(jcefPath.resolve("libjcef.so")) ||
                   Files.exists(jcefPath.resolve("libcef.so"));
        } else if (platform.isMacOS()) {
            return Files.exists(jcefPath.resolve("jcef_app.app"));
        }
        return false;
    }

    /**
     * SystemBootstrapのカスタムローダーを設定する。
     * CefApp内部でSystem.loadLibrary()が呼ばれた時に、
     * 代わりにSystem.load()で絶対パスからロードする。
     */
    private void setupSystemBootstrapLoader(Path jcefPath, MMOSPlatform platform) {
        final Path finalJcefPath = jcefPath;

        SystemBootstrap.setLoader(new SystemBootstrap.Loader() {
            @Override
            public void loadLibrary(String libName) {
                // ライブラリ名から拡張子付きファイル名を構築
                String fileName;
                if (platform.isWindows()) {
                    fileName = libName + ".dll";
                } else if (platform.isLinux()) {
                    fileName = "lib" + libName + ".so";
                } else if (platform.isMacOS()) {
                    fileName = "lib" + libName + ".dylib";
                } else {
                    fileName = libName;
                }

                File libFile = finalJcefPath.resolve(fileName).toFile();
                if (libFile.exists()) {
                    log("SystemBootstrap loading: " + libFile.getAbsolutePath());
                    System.load(libFile.getAbsolutePath());
                } else {
                    // ファイルが見つからない場合は標準のloadLibraryを試す
                    log("Library not found in JCEF path, falling back: " + libName);
                    System.loadLibrary(libName);
                }
            }
        });

        log("SystemBootstrap custom loader configured");
    }

    /**
     * java.library.pathにパスを動的に追加する。
     * CefAppがSystem.loadLibrary()を使用するため、これが必要。
     */
    private void addToJavaLibraryPath(String path) {
        try {
            String libraryPath = System.getProperty("java.library.path");
            if (libraryPath != null && libraryPath.contains(path)) {
                log("JCEF path already in java.library.path");
                return;
            }

            // java.library.pathを更新
            String newPath = path + java.io.File.pathSeparator + (libraryPath != null ? libraryPath : "");
            System.setProperty("java.library.path", newPath);
            log("Updated java.library.path: " + path);

            // ClassLoaderのsys_pathsキャッシュをクリアして再読み込みを強制
            // これはJava内部のハックだが、動的にlibrary pathを追加する唯一の方法
            try {
                java.lang.reflect.Field sysPathsField = ClassLoader.class.getDeclaredField("sys_paths");
                sysPathsField.setAccessible(true);
                sysPathsField.set(null, null);
                log("ClassLoader sys_paths cache cleared");
            } catch (NoSuchFieldException e) {
                // Java 11+ではこのフィールドがない可能性がある
                log("sys_paths field not found (Java 11+), trying alternative approach");
            }

        } catch (Exception e) {
            logError("Failed to add to java.library.path: " + e.getMessage(), e);
        }
    }

    /**
     * ネイティブライブラリをロードする。
     * CefAppがSystem.loadLibrary()を使う前に、System.load()で明示的にロードしておく。
     * 正しい依存関係の順序でロードする必要がある。
     */
    private void loadNativeLibraries(Path jcefPath, MMOSPlatform platform) {
        if (platform.isWindows()) {
            // Windowsでは依存関係の順序でDLLをロード
            // 1. chrome_elf.dll (最初に必要)
            // 2. libcef.dll (CEFコア)
            // 3. jcef.dll (Javaバインディング)
            String[] dlls = {
                "chrome_elf.dll",
                "libcef.dll",
                "jcef.dll"
            };

            for (String dll : dlls) {
                File dllFile = jcefPath.resolve(dll).toFile();
                if (dllFile.exists()) {
                    try {
                        log("Loading native library: " + dllFile.getAbsolutePath());
                        System.load(dllFile.getAbsolutePath());
                        log("Successfully loaded: " + dll);
                    } catch (UnsatisfiedLinkError e) {
                        // 既にロード済みの場合はエラーを無視
                        log("Library " + dll + " already loaded or error: " + e.getMessage());
                    }
                } else {
                    log("Warning: DLL not found: " + dllFile.getAbsolutePath());
                }
            }
        } else if (platform.isLinux()) {
            // Linuxでは依存関係の順序で.soをロード
            String[] libs = {
                "libcef.so",
                "libjcef.so"
            };

            for (String lib : libs) {
                File libFile = jcefPath.resolve(lib).toFile();
                if (libFile.exists()) {
                    try {
                        log("Loading native library: " + libFile.getAbsolutePath());
                        System.load(libFile.getAbsolutePath());
                        log("Successfully loaded: " + lib);
                    } catch (UnsatisfiedLinkError e) {
                        log("Library " + lib + " already loaded or error: " + e.getMessage());
                    }
                }
            }
        }
        // macOSではフレームワークバンドルを使用するため、明示的なロードは不要
    }

    /**
     * CEFコマンドライン引数を構築する。
     */
    private String[] buildCefArgs(MMOSPlatform platform) {
        java.util.List<String> args = new java.util.ArrayList<>();

        // 共通設定
        args.add("--enable-gpu");
        args.add("--enable-accelerated-video-decode");
        args.add("--enable-accelerated-2d-canvas");
        args.add("--disable-gpu-vsync");
        args.add("--max-fps=60");
        args.add("--disable-frame-rate-limit");

        // Mac固有の設定
        if (platform.isMacOS()) {
            args.add("--no-sandbox");
            args.add("--disable-gpu-sandbox");
            args.add("--ignore-certificate-errors");
            log("Mac detected - applying sandbox workarounds");
        }

        return args.toArray(new String[0]);
    }

    @Override
    public boolean isAvailable() {
        // MMOSInstallerがインストール済みかどうかで判断
        MMOSInstallListener listener = MMOSInstallListener.INSTANCE;
        return listener.isDone() && !listener.isFailed();
    }

    @Override
    public String getName() {
        return "ForgeChromiumProvider (JCEF direct)";
    }

    /**
     * UIコンポーネントをサポートしないことを示す。
     * CefBrowserOsrNoCanvasはAWTを使用しないため、getUIComponent()はnullを返す。
     * これによりChromiumBrowserがhidden JFrameを作成しない。
     */
    @Override
    public boolean supportsUIComponent() {
        return false;
    }

    @Override
    public void addConsoleMessageListener(org.cef.browser.CefBrowser browser, ConsoleMessageListener listener) {
        this.consoleListener = listener;
        log("Console message listener registered");
    }

    /**
     * ブラウザを作成する。
     * ForgeChromiumProviderでは CefBrowserOsrNoCanvas を使用し、AWTを完全にバイパスする。
     *
     * CefBrowserOsrNoCanvas は GLCanvas を使用せず、onPaint() で受け取った
     * ピクセルデータを int[] 配列に保存する。このピクセルデータは
     * ProcessingScreen と同様に Minecraft の NativeImage/DynamicTexture に変換される。
     */
    @Override
    public org.cef.browser.CefBrowser createBrowser(org.cef.CefClient client, String url, boolean osrEnabled, boolean transparent) {
        log("Creating browser with ForgeChromiumProvider (NoCanvas mode)...");
        log("- URL: " + url);
        log("- OSR: " + osrEnabled + ", Transparent: " + transparent);
        log("- CefClient: " + (client != null ? "available" : "NULL"));

        if (client == null) {
            logError("CefClient is null, cannot create browser", null);
            throw new RuntimeException("CefClient is null");
        }

        try {
            // CefBrowserOsrNoCanvas を直接作成（AWTを完全にバイパス）
            log("Creating CefBrowserOsrNoCanvas (AWT-free)...");
            CefBrowserOsrNoCanvas browser = CefBrowserFactory.createNoCanvas(
                    client, url, transparent, null, null);

            // ブラウザを即時作成
            browser.createImmediately();

            log("Browser created successfully: " + browser);
            log("Browser type: CefBrowserOsrNoCanvas (AWT-free)");
            return browser;
        } catch (Exception e) {
            logError("Failed to create browser: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create browser with ForgeChromiumProvider", e);
        }
    }

    /**
     * ログ出力（INFO）。
     */
    private void log(String message) {
        LOGGER.info("[" + TAG + "] " + message);
    }

    /**
     * エラーログ出力。
     */
    private void logError(String message, Throwable e) {
        if (e != null) {
            LOGGER.error("[" + TAG + "] " + message, e);
        } else {
            LOGGER.error("[" + TAG + "] " + message);
        }
    }
}
