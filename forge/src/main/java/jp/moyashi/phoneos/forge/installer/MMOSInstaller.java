package jp.moyashi.phoneos.forge.installer;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * MMOSインストーラー本体。
 * MCEFと同様のアプローチで、jcefmavenを使用せずに
 * JCEFネイティブライブラリをダウンロード・インストールする。
 *
 * jcefmavenのbuild()/install()はForge環境でデッドロックするため、
 * 独自のダウンロード・展開処理を実装している。
 *
 * このクラスはバックグラウンドスレッドで実行され、
 * MMOSInstallListenerを通じて進捗を報告する。
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class MMOSInstaller implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger();

    /** インストール完了を示すマーカーファイル名 */
    private static final String INSTALL_MARKER = "install_complete";

    /** バージョン情報ファイル名 */
    private static final String VERSION_FILE = "version.txt";

    /** JCEFバージョン情報 */
    private static final String JCEF_VERSION = "jcef-ca49ada+cef-135.0.20+ge7de5c3+chromium-135.0.7049.85";

    /** Maven Centralのベース URL */
    private static final String MAVEN_BASE_URL = "https://repo1.maven.org/maven2/me/friwi/";

    /** JOGL Maven URL (JOGAMP is hosted on their own repository, not Maven Central) */
    private static final String JOGL_MAVEN_BASE_URL = "https://jogamp.org/deployment/maven/org/jogamp/";

    /** JOGLバージョン */
    private static final String JOGL_VERSION = "2.5.0";

    /** 設定 */
    private final MMOSInstallerSettings settings;

    /** プラットフォーム */
    private final MMOSPlatform platform;

    /**
     * インストーラーを構築する。
     */
    public MMOSInstaller() {
        this.settings = MMOSInstallerSettings.getInstance();
        this.platform = MMOSPlatform.detect();
    }

    @Override
    public void run() {
        LOGGER.info("[MMOSInstaller] Starting MMOS installation...");
        MMOSPlatform.printDebugInfo();

        try {
            // プラットフォーム確認
            if (platform == MMOSPlatform.UNKNOWN) {
                throw new RuntimeException("Unsupported platform");
            }

            // ダウンロードスキップ設定の確認
            if (settings.isSkipDownload()) {
                LOGGER.info("[MMOSInstaller] Download skipped (skip-download=true)");
                MMOSInstallListener.INSTANCE.setDone(true);
                return;
            }

            // 既にインストール済みか確認
            if (isInstalled()) {
                LOGGER.info("[MMOSInstaller] JCEF already installed, skipping download");
                MMOSInstallListener.INSTANCE.setDone(true);
                return;
            }

            // インストールディレクトリを準備
            setupInstallDirectory();

            // JCEFをダウンロード・インストール
            installJcef();

            // JOGLネイティブをダウンロード・インストール
            installJoglNatives();

            // インストール完了マーカーを作成
            createInstallMarker();

            LOGGER.info("[MMOSInstaller] MMOS installation completed successfully");
            MMOSInstallListener.INSTANCE.setTask("Installation complete");
            MMOSInstallListener.INSTANCE.setDone(true);

        } catch (Exception e) {
            LOGGER.error("[MMOSInstaller] Installation failed: " + e.getMessage(), e);
            MMOSInstallListener.INSTANCE.setFailed(true);
            MMOSInstallListener.INSTANCE.setErrorMessage(e.getMessage());
        }
    }

    /**
     * JCEFが既にインストールされているかどうかを確認する。
     *
     * @return インストール済みの場合true
     */
    private boolean isInstalled() {
        Path jcefPath = settings.getJcefPath(platform);
        Path markerFile = jcefPath.resolve(INSTALL_MARKER);
        Path versionFile = jcefPath.resolve(VERSION_FILE);

        // マーカーファイルとバージョンファイルが存在するか確認
        if (!Files.exists(markerFile) || !Files.exists(versionFile)) {
            return false;
        }

        // バージョンが一致するか確認
        try {
            String installedVersion = Files.readString(versionFile).trim();

            if (!installedVersion.equals(JCEF_VERSION)) {
                LOGGER.info("[MMOSInstaller] JCEF version mismatch: installed=" + installedVersion + ", current=" + JCEF_VERSION);
                return false;
            }

            // ネイティブライブラリが存在するか確認
            return checkNativeLibraries(jcefPath);

        } catch (IOException e) {
            LOGGER.warn("[MMOSInstaller] Failed to read version file: " + e.getMessage());
            return false;
        }
    }

    /**
     * ネイティブライブラリが存在するかどうかを確認する。
     * JCEFとJOGLの両方のネイティブをチェック。
     *
     * @param jcefPath JCEFパス
     * @return 存在する場合true
     */
    private boolean checkNativeLibraries(Path jcefPath) {
        boolean jcefExists = false;
        boolean joglExists = false;

        if (platform.isWindows()) {
            jcefExists = Files.exists(jcefPath.resolve("jcef.dll")) ||
                         Files.exists(jcefPath.resolve("libcef.dll"));
            // JOGLネイティブの存在確認
            joglExists = Files.exists(jcefPath.resolve("gluegen_rt.dll"));
        } else if (platform.isLinux()) {
            jcefExists = Files.exists(jcefPath.resolve("libjcef.so")) ||
                         Files.exists(jcefPath.resolve("libcef.so"));
            joglExists = Files.exists(jcefPath.resolve("libgluegen_rt.so"));
        } else if (platform.isMacOS()) {
            jcefExists = Files.exists(jcefPath.resolve("jcef_app.app"));
            joglExists = Files.exists(jcefPath.resolve("libgluegen_rt.dylib"));
        }

        if (!jcefExists) {
            LOGGER.info("[MMOSInstaller] JCEF natives not found");
        }
        if (!joglExists) {
            LOGGER.info("[MMOSInstaller] JOGL natives not found");
        }

        return jcefExists && joglExists;
    }

    /**
     * インストールディレクトリを準備する。
     */
    private void setupInstallDirectory() throws IOException {
        Path jcefPath = settings.getJcefPath(platform);
        Files.createDirectories(jcefPath);
        LOGGER.info("[MMOSInstaller] Install directory: " + jcefPath.toAbsolutePath());
    }

    /**
     * JCEFをダウンロード・インストールする。
     * MCEFと同様のアプローチで、独自のダウンロード・展開処理を使用。
     */
    private void installJcef() throws Exception {
        Path jcefPath = settings.getJcefPath(platform);
        Path librariesPath = Path.of(settings.getLibrariesPath());

        // jcef.pathシステムプロパティを設定
        System.setProperty("jcef.path", jcefPath.toAbsolutePath().toString());
        LOGGER.info("[MMOSInstaller] Setting jcef.path to: " + jcefPath.toAbsolutePath());

        // プラットフォームに応じたアーティファクト名を取得
        String artifactName = getNativesArtifactName();
        String jarFileName = artifactName + "-" + JCEF_VERSION + ".jar";
        String downloadUrl = MAVEN_BASE_URL + artifactName + "/" + JCEF_VERSION + "/" + jarFileName;

        LOGGER.info("[MMOSInstaller] Download URL: " + downloadUrl);

        // JARファイルをダウンロード
        File jarFile = librariesPath.resolve(jarFileName).toFile();
        librariesPath.toFile().mkdirs();

        MMOSInstallListener.INSTANCE.setTask("Downloading JCEF...");
        MMOSInstallListener.INSTANCE.setProgress(0.0f);

        downloadFile(downloadUrl, jarFile);

        // JARからtar.gzを抽出
        MMOSInstallListener.INSTANCE.setTask("Extracting archive...");
        MMOSInstallListener.INSTANCE.setProgress(0.5f);

        File tarGzFile = librariesPath.resolve(artifactName + ".tar.gz").toFile();
        extractTarGzFromJar(jarFile, tarGzFile);

        // tar.gzを展開
        MMOSInstallListener.INSTANCE.setTask("Installing JCEF...");
        MMOSInstallListener.INSTANCE.setProgress(0.6f);

        extractTarGz(tarGzFile, jcefPath.toFile());

        // 一時ファイルを削除
        jarFile.delete();
        tarGzFile.delete();

        MMOSInstallListener.INSTANCE.setTask("JCEF installed successfully");
        MMOSInstallListener.INSTANCE.setProgress(1.0f);

        LOGGER.info("[MMOSInstaller] JCEF native libraries installed successfully");
    }

    /**
     * JOGLネイティブライブラリをダウンロード・インストールする。
     * JCEFのCefBrowserOsrはGLCanvas（JOGL）を使用するため、JOGLネイティブが必要。
     */
    private void installJoglNatives() throws Exception {
        Path jcefPath = settings.getJcefPath(platform);
        Path librariesPath = Path.of(settings.getLibrariesPath());

        // プラットフォームに応じたネイティブ分類子を取得
        String nativesClassifier = getJoglNativesClassifier();
        LOGGER.info("[MMOSInstaller] Installing JOGL natives for: " + nativesClassifier);

        MMOSInstallListener.INSTANCE.setTask("Installing JOGL natives...");

        // gluegen-rt natives
        String gluegenJarName = "gluegen-rt-" + JOGL_VERSION + "-" + nativesClassifier + ".jar";
        String gluegenUrl = JOGL_MAVEN_BASE_URL + "gluegen/gluegen-rt/" + JOGL_VERSION + "/" + gluegenJarName;
        File gluegenJar = librariesPath.resolve(gluegenJarName).toFile();

        LOGGER.info("[MMOSInstaller] Downloading gluegen-rt natives: " + gluegenUrl);
        downloadFile(gluegenUrl, gluegenJar);
        extractNativesFromJar(gluegenJar, jcefPath.toFile());
        gluegenJar.delete();

        // jogl-all natives
        String joglJarName = "jogl-all-" + JOGL_VERSION + "-" + nativesClassifier + ".jar";
        String joglUrl = JOGL_MAVEN_BASE_URL + "jogl/jogl-all/" + JOGL_VERSION + "/" + joglJarName;
        File joglJar = librariesPath.resolve(joglJarName).toFile();

        LOGGER.info("[MMOSInstaller] Downloading jogl-all natives: " + joglUrl);
        downloadFile(joglUrl, joglJar);
        extractNativesFromJar(joglJar, jcefPath.toFile());
        joglJar.delete();

        LOGGER.info("[MMOSInstaller] JOGL native libraries installed successfully");
    }

    /**
     * プラットフォームに応じたJOGLネイティブ分類子を取得する。
     */
    private String getJoglNativesClassifier() {
        if (platform.isWindows()) {
            return "natives-windows-amd64";
        } else if (platform.isLinux()) {
            return "natives-linux-amd64";
        } else if (platform.isMacOS()) {
            return "natives-macosx-universal";
        }
        throw new RuntimeException("Unsupported platform for JOGL: " + platform);
    }

    /**
     * JARファイルからネイティブライブラリを抽出する。
     * .dll, .so, .dylib, .jnilib ファイルを対象ディレクトリに展開する。
     */
    private void extractNativesFromJar(File jarFile, File outputDirectory) throws IOException {
        LOGGER.info("[MMOSInstaller] Extracting natives from: " + jarFile.getName());

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(jarFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                String name = entry.getName();

                // ネイティブライブラリのみ抽出
                if (name.endsWith(".dll") || name.endsWith(".so") ||
                    name.endsWith(".dylib") || name.endsWith(".jnilib")) {

                    // パスからファイル名のみを取得
                    String fileName = name;
                    int lastSlash = name.lastIndexOf('/');
                    if (lastSlash >= 0) {
                        fileName = name.substring(lastSlash + 1);
                    }

                    File outputFile = new File(outputDirectory, fileName);
                    LOGGER.info("[MMOSInstaller] Extracting: " + fileName);

                    try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                }
            }
        }
    }

    /**
     * プラットフォームに応じたネイティブアーティファクト名を取得する。
     */
    private String getNativesArtifactName() {
        if (platform.isWindows()) {
            return "jcef-natives-windows-amd64";
        } else if (platform.isLinux()) {
            return "jcef-natives-linux-amd64";
        } else if (platform.isMacOS()) {
            if (platform.isArm()) {
                return "jcef-natives-macosx-arm64";
            } else {
                return "jcef-natives-macosx-amd64";
            }
        }
        throw new RuntimeException("Unsupported platform: " + platform);
    }

    /**
     * ファイルをダウンロードする。
     * MCEFのdownloadFile()を参考に実装。
     */
    private void downloadFile(String urlString, File outputFile) throws IOException {
        LOGGER.info("[MMOSInstaller] Downloading: " + urlString);
        LOGGER.info("[MMOSInstaller] -> " + outputFile.getCanonicalPath());

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            throw new IOException("Failed to download: HTTP " + responseCode);
        }

        int fileSize = connection.getContentLength();
        LOGGER.info("[MMOSInstaller] File size: " + (fileSize / 1024 / 1024) + " MB");

        try (BufferedInputStream inputStream = new BufferedInputStream(connection.getInputStream());
             FileOutputStream outputStream = new FileOutputStream(outputFile)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            int totalBytesRead = 0;
            int lastLoggedPercent = -1;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;

                if (fileSize > 0) {
                    float progress = (float) totalBytesRead / fileSize;
                    int percent = (int) (progress * 100);

                    // 進捗を更新（ダウンロードは0-50%の範囲）
                    MMOSInstallListener.INSTANCE.setProgress(progress * 0.5f);

                    // 1%ごとにログ出力
                    if (percent != lastLoggedPercent) {
                        lastLoggedPercent = percent;
                        LOGGER.info("[MMOSInstaller] Downloading... " + percent + "%");
                    }
                }
            }
        }

        LOGGER.info("[MMOSInstaller] Download completed: " + outputFile.length() + " bytes");
    }

    /**
     * JARファイルからtar.gzを抽出する。
     * JARはZIP形式なので、ZipInputStreamで読み込む。
     */
    private void extractTarGzFromJar(File jarFile, File outputTarGz) throws IOException {
        LOGGER.info("[MMOSInstaller] Extracting tar.gz from JAR...");

        try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(jarFile))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                if (entry.getName().endsWith(".tar.gz")) {
                    LOGGER.info("[MMOSInstaller] Found: " + entry.getName());

                    try (FileOutputStream outputStream = new FileOutputStream(outputTarGz)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = zipInputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }

                    LOGGER.info("[MMOSInstaller] Extracted: " + outputTarGz.length() + " bytes");
                    return;
                }
            }
        }

        throw new IOException("tar.gz not found in JAR file");
    }

    /**
     * tar.gzファイルを展開する。
     * MCEFのextractTarGz()を参考に実装。
     */
    private void extractTarGz(File tarGzFile, File outputDirectory) throws IOException {
        LOGGER.info("[MMOSInstaller] Extracting tar.gz to: " + outputDirectory.getCanonicalPath());

        outputDirectory.mkdirs();

        long fileSize = tarGzFile.length();
        long totalBytesRead = 0;
        int lastLoggedPercent = -1;

        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(
                new GzipCompressorInputStream(new FileInputStream(tarGzFile)))) {

            TarArchiveEntry entry;
            while ((entry = tarInput.getNextTarEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                // エントリ名から最初のディレクトリを除去（例: "jcef-xxx/libcef.dll" -> "libcef.dll"）
                String entryName = entry.getName();
                int slashIndex = entryName.indexOf('/');
                if (slashIndex > 0) {
                    entryName = entryName.substring(slashIndex + 1);
                }

                if (entryName.isEmpty()) {
                    continue;
                }

                File outputFile = new File(outputDirectory, entryName);
                outputFile.getParentFile().mkdirs();

                try (FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = tarInput.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                        totalBytesRead += bytesRead;

                        // 進捗を更新（展開は50-100%の範囲）
                        // 圧縮率は約2.6倍と仮定
                        float progress = ((float) totalBytesRead / fileSize) / 2.6f;
                        if (progress > 1.0f) progress = 1.0f;
                        MMOSInstallListener.INSTANCE.setProgress(0.5f + progress * 0.5f);

                        int percent = (int) (progress * 100);
                        if (percent != lastLoggedPercent && percent % 10 == 0) {
                            lastLoggedPercent = percent;
                            LOGGER.info("[MMOSInstaller] Extracting... " + percent + "%");
                        }
                    }
                }
            }
        }

        LOGGER.info("[MMOSInstaller] Extraction completed");
    }

    /**
     * インストール完了マーカーを作成する。
     */
    private void createInstallMarker() throws IOException {
        Path jcefPath = settings.getJcefPath(platform);
        Path markerFile = jcefPath.resolve(INSTALL_MARKER);
        Path versionFile = jcefPath.resolve(VERSION_FILE);

        // マーカーファイルを作成
        Files.writeString(markerFile, "MMOS JCEF Installation Complete\n");

        // バージョン情報を書き込み
        Files.writeString(versionFile, JCEF_VERSION);

        LOGGER.info("[MMOSInstaller] Install marker created: " + markerFile);
    }

    /**
     * ライブラリパスをセットアップする。
     * Mixin注入前に呼び出される。
     */
    public static void setupLibraryPath() {
        MMOSInstallerSettings settings = MMOSInstallerSettings.getInstance();
        MMOSPlatform platform = MMOSPlatform.detect();

        Path jcefPath = settings.getJcefPath(platform);

        // jcef.pathシステムプロパティを設定
        System.setProperty("jcef.path", jcefPath.toAbsolutePath().toString());

        // mmos.libraries.pathシステムプロパティを設定
        System.setProperty("mmos.libraries.path", settings.getLibrariesPath());

        LOGGER.info("[MMOSInstaller] Library path setup complete");
        LOGGER.info("[MMOSInstaller] jcef.path = " + System.getProperty("jcef.path"));
        LOGGER.info("[MMOSInstaller] mmos.libraries.path = " + System.getProperty("mmos.libraries.path"));
    }
}
