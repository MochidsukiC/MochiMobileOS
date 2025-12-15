package jp.moyashi.phoneos.forge.bridge;

import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * MMOSサーバープロセスランチャー。
 * 独立したJVMでMMOSサーバーを起動する。
 *
 * @author jp.moyashi
 * @version 1.0
 */
public class ProcessLauncher {

    private static final Logger LOGGER = LogManager.getLogger(ProcessLauncher.class);

    /** サーバーJARファイル名 */
    private static final String SERVER_JAR_NAME = "MochiMobileOS-Server.jar";

    /** MOD内蔵サーバーJARのリソースパス */
    private static final String EMBEDDED_SERVER_RESOURCE = "/mmos-server/MochiMobileOS-Server.jar";

    /** サーバープロセス */
    private Process serverProcess;

    /** ワールドID */
    private final String worldId;

    /** サーバーJARパス */
    private File serverJarPath;

    /** 出力読み取りスレッド */
    private Thread outputThread;

    /** エラー読み取りスレッド */
    private Thread errorThread;

    /**
     * プロセスランチャーを作成する。
     *
     * @param worldId ワールドID
     */
    public ProcessLauncher(String worldId) {
        this.worldId = worldId;
    }

    /**
     * サーバーJARを探す。
     * 外部ファイルが見つからない場合、MODに内蔵されたJARを抽出する。
     *
     * @return サーバーJARが見つかった場合true
     */
    public boolean findServerJar() {
        // 1. 既に抽出済みのJARをチェック（一時ディレクトリ）
        File tempDir = new File(System.getProperty("java.io.tmpdir"), "mmos");
        File extractedJar = new File(tempDir, SERVER_JAR_NAME);
        if (extractedJar.exists()) {
            serverJarPath = extractedJar;
            LOGGER.info("[ProcessLauncher] Found extracted server JAR: {}", serverJarPath.getAbsolutePath());
            return true;
        }

        // 2. MODフォルダ内を探す
        File modsDir = new File("mods");
        if (modsDir.exists()) {
            File jarInMods = new File(modsDir, SERVER_JAR_NAME);
            if (jarInMods.exists()) {
                serverJarPath = jarInMods;
                LOGGER.info("[ProcessLauncher] Found server JAR in mods: {}", serverJarPath.getAbsolutePath());
                return true;
            }
        }

        // 3. カレントディレクトリを探す
        File jarInCurrent = new File(SERVER_JAR_NAME);
        if (jarInCurrent.exists()) {
            serverJarPath = jarInCurrent;
            LOGGER.info("[ProcessLauncher] Found server JAR in current directory: {}", serverJarPath.getAbsolutePath());
            return true;
        }

        // 4. config/mmosディレクトリを探す
        File configDir = new File("config/mmos");
        if (configDir.exists()) {
            File jarInConfig = new File(configDir, SERVER_JAR_NAME);
            if (jarInConfig.exists()) {
                serverJarPath = jarInConfig;
                LOGGER.info("[ProcessLauncher] Found server JAR in config/mmos: {}", serverJarPath.getAbsolutePath());
                return true;
            }
        }

        // 5. Minecraftディレクトリを探す
        String minecraftDir = System.getProperty("user.home") + "/.minecraft";
        File mcDir = new File(minecraftDir);
        if (mcDir.exists()) {
            File jarInMc = new File(mcDir, "mmos/" + SERVER_JAR_NAME);
            if (jarInMc.exists()) {
                serverJarPath = jarInMc;
                LOGGER.info("[ProcessLauncher] Found server JAR in .minecraft/mmos: {}", serverJarPath.getAbsolutePath());
                return true;
            }
        }

        // 6. MODに内蔵されたJARを抽出
        LOGGER.info("[ProcessLauncher] Attempting to extract embedded server JAR...");
        if (extractEmbeddedServerJar(extractedJar)) {
            serverJarPath = extractedJar;
            LOGGER.info("[ProcessLauncher] Extracted embedded server JAR to: {}", serverJarPath.getAbsolutePath());
            return true;
        }

        LOGGER.error("[ProcessLauncher] Server JAR not found and extraction failed: {}", SERVER_JAR_NAME);
        return false;
    }

    /**
     * MODに内蔵されたサーバーJARを抽出する。
     *
     * @param targetFile 抽出先ファイル
     * @return 抽出成功した場合true
     */
    private boolean extractEmbeddedServerJar(File targetFile) {
        try (InputStream is = getClass().getResourceAsStream(EMBEDDED_SERVER_RESOURCE)) {
            if (is == null) {
                LOGGER.warn("[ProcessLauncher] Embedded server JAR resource not found: {}", EMBEDDED_SERVER_RESOURCE);
                return false;
            }

            // 親ディレクトリを作成
            File parentDir = targetFile.getParentFile();
            if (!parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    LOGGER.error("[ProcessLauncher] Failed to create directory: {}", parentDir.getAbsolutePath());
                    return false;
                }
            }

            // JARを抽出
            Files.copy(is, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            LOGGER.info("[ProcessLauncher] Successfully extracted {} bytes", targetFile.length());
            return true;

        } catch (IOException e) {
            LOGGER.error("[ProcessLauncher] Failed to extract embedded server JAR", e);
            return false;
        }
    }

    /**
     * サーバープロセスを起動する。
     *
     * @return 起動成功した場合true
     */
    public boolean launch() {
        if (serverJarPath == null) {
            if (!findServerJar()) {
                LOGGER.error("[ProcessLauncher] Cannot launch: Server JAR not found");
                return false;
            }
        }

        if (serverProcess != null && serverProcess.isAlive()) {
            LOGGER.warn("[ProcessLauncher] Server process already running");
            return true;
        }

        try {
            // Javaコマンドを構築
            List<String> command = new ArrayList<>();

            // Java実行ファイル
            String javaHome = System.getProperty("java.home");
            String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                javaBin += ".exe";
            }
            command.add(javaBin);

            // JVM引数
            command.add("-Xmx4G");
            command.add("-Xms512M");
            command.add("-XX:+UseG1GC");
            command.add("-XX:MaxGCPauseMillis=50");
            command.add("--add-opens=java.base/java.lang=ALL-UNNAMED");
            command.add("--add-exports=java.base/java.lang=ALL-UNNAMED");
            command.add("--add-exports=java.desktop/sun.awt=ALL-UNNAMED");
            command.add("--add-exports=java.desktop/sun.java2d=ALL-UNNAMED");

            // JCEFはサーバー側（coreモジュール）でjcefmavenにより自動セットアップされる
            // Forge側からのパス指定は不要（責任分離原則）

            // JAR指定
            command.add("-jar");
            command.add(serverJarPath.getAbsolutePath());

            // ワールドID引数
            command.add(worldId);

            LOGGER.info("[ProcessLauncher] Launching server: {}", String.join(" ", command));

            // プロセスビルダー
            // ゲーム実行ディレクトリ（Minecraftの実行ディレクトリ）で実行
            // これによりVFSデータディレクトリがMinecraftフォルダ内に配置される
            ProcessBuilder builder = new ProcessBuilder(command);
            File gameDir = FMLPaths.GAMEDIR.get().toFile();
            builder.directory(gameDir);
            LOGGER.info("[ProcessLauncher] Working directory: {}", gameDir.getAbsolutePath());
            builder.redirectErrorStream(false);

            // プロセスを起動
            serverProcess = builder.start();

            // 出力読み取りスレッドを開始
            startOutputReaders();

            LOGGER.info("[ProcessLauncher] Server process started, PID: {}", serverProcess.pid());
            return true;

        } catch (IOException e) {
            LOGGER.error("[ProcessLauncher] Failed to launch server process", e);
            return false;
        }
    }

    /**
     * 出力読み取りスレッドを開始する。
     */
    private void startOutputReaders() {
        // 標準出力
        outputThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(serverProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.info("[MMOS-Server] {}", line);
                }
            } catch (IOException e) {
                if (serverProcess.isAlive()) {
                    LOGGER.error("[ProcessLauncher] Error reading server output", e);
                }
            }
        }, "MMOS-Server-Output");
        outputThread.setDaemon(true);
        outputThread.start();

        // 標準エラー
        errorThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(serverProcess.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    LOGGER.error("[MMOS-Server-ERR] {}", line);
                }
            } catch (IOException e) {
                if (serverProcess.isAlive()) {
                    LOGGER.error("[ProcessLauncher] Error reading server error output", e);
                }
            }
        }, "MMOS-Server-Error");
        errorThread.setDaemon(true);
        errorThread.start();
    }

    /**
     * サーバープロセスが実行中かどうか。
     */
    public boolean isRunning() {
        return serverProcess != null && serverProcess.isAlive();
    }

    /**
     * サーバープロセスを停止する。
     */
    public void stop() {
        if (serverProcess == null) {
            return;
        }

        LOGGER.info("[ProcessLauncher] Stopping server process...");

        try {
            // 正常終了を試みる
            serverProcess.destroy();

            // 5秒待機
            if (!serverProcess.waitFor(5, TimeUnit.SECONDS)) {
                // 強制終了
                LOGGER.warn("[ProcessLauncher] Server did not stop gracefully, forcing...");
                serverProcess.destroyForcibly();
                serverProcess.waitFor(2, TimeUnit.SECONDS);
            }

            LOGGER.info("[ProcessLauncher] Server process stopped");

        } catch (InterruptedException e) {
            LOGGER.error("[ProcessLauncher] Interrupted while stopping server", e);
            serverProcess.destroyForcibly();
        }

        serverProcess = null;
    }

    /**
     * サーバープロセスのPIDを取得する。
     */
    public long getPid() {
        return serverProcess != null ? serverProcess.pid() : -1;
    }
}
