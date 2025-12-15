package jp.moyashi.phoneos.forge.installer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

/**
 * MMOSインストーラーの設定を管理するクラス。
 * 設定ファイルの読み書きを行う。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class MMOSInstallerSettings {

    private static final Logger LOGGER = LogManager.getLogger("MMOSInstallerSettings");

    /** 設定ファイルのパス */
    private static final Path SETTINGS_PATH = Paths.get("config", "mmos", "installer.properties");

    /** シングルトンインスタンス */
    private static MMOSInstallerSettings instance;

    /** ダウンロードをスキップするかどうか */
    private boolean skipDownload = false;

    /** カスタムダウンロードミラー（nullの場合はデフォルト） */
    private String downloadMirror = null;

    /** ライブラリの保存先パス */
    private String librariesPath = "mods/mmos-system";

    /** デバッグモード */
    private boolean debug = false;

    /** プライベートコンストラクタ */
    private MMOSInstallerSettings() {
    }

    /**
     * シングルトンインスタンスを取得する。
     *
     * @return インスタンス
     */
    public static synchronized MMOSInstallerSettings getInstance() {
        if (instance == null) {
            instance = new MMOSInstallerSettings();
            instance.load();
        }
        return instance;
    }

    // ========================================
    // Getter
    // ========================================

    /**
     * ダウンロードをスキップするかどうかを取得する。
     *
     * @return スキップする場合true
     */
    public boolean isSkipDownload() {
        return skipDownload;
    }

    /**
     * ダウンロードミラーを取得する。
     *
     * @return カスタムミラー（設定されていない場合はnull）
     */
    public String getDownloadMirror() {
        return downloadMirror;
    }

    /**
     * ライブラリの保存先パスを取得する。
     *
     * @return ライブラリパス
     */
    public String getLibrariesPath() {
        return librariesPath;
    }

    /**
     * デバッグモードかどうかを取得する。
     *
     * @return デバッグモードの場合true
     */
    public boolean isDebug() {
        return debug;
    }

    // ========================================
    // Setter
    // ========================================

    /**
     * ダウンロードスキップを設定する。
     *
     * @param skipDownload スキップする場合true
     */
    public void setSkipDownload(boolean skipDownload) {
        this.skipDownload = skipDownload;
    }

    /**
     * ダウンロードミラーを設定する。
     *
     * @param downloadMirror カスタムミラーURL
     */
    public void setDownloadMirror(String downloadMirror) {
        this.downloadMirror = downloadMirror;
    }

    /**
     * ライブラリの保存先パスを設定する。
     *
     * @param librariesPath ライブラリパス
     */
    public void setLibrariesPath(String librariesPath) {
        this.librariesPath = librariesPath;
    }

    /**
     * デバッグモードを設定する。
     *
     * @param debug デバッグモードの場合true
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    // ========================================
    // 永続化
    // ========================================

    /**
     * 設定をファイルから読み込む。
     */
    public void load() {
        try {
            File file = SETTINGS_PATH.toFile();

            if (!file.exists()) {
                // デフォルト設定で保存
                save();
                return;
            }

            Properties properties = new Properties();
            try (FileInputStream input = new FileInputStream(file)) {
                properties.load(input);
            }

            // プロパティを読み込み
            skipDownload = Boolean.parseBoolean(properties.getProperty("skip-download", "false"));
            downloadMirror = properties.getProperty("download-mirror");
            librariesPath = properties.getProperty("libraries-path", "mods/mmos-system");
            debug = Boolean.parseBoolean(properties.getProperty("debug", "false"));

            if (downloadMirror != null && downloadMirror.isEmpty()) {
                downloadMirror = null;
            }

            LOGGER.info("[MMOSInstallerSettings] Settings loaded from " + SETTINGS_PATH);

        } catch (IOException e) {
            LOGGER.error("[MMOSInstallerSettings] Failed to load settings: " + e.getMessage());
            // デフォルト値を使用
        }
    }

    /**
     * 設定をファイルに保存する。
     */
    public void save() {
        try {
            // ディレクトリを作成
            Files.createDirectories(SETTINGS_PATH.getParent());

            Properties properties = new Properties();
            properties.setProperty("skip-download", String.valueOf(skipDownload));
            if (downloadMirror != null && !downloadMirror.isEmpty()) {
                properties.setProperty("download-mirror", downloadMirror);
            }
            properties.setProperty("libraries-path", librariesPath);
            properties.setProperty("debug", String.valueOf(debug));

            try (FileOutputStream output = new FileOutputStream(SETTINGS_PATH.toFile())) {
                properties.store(output, "MMOS Installer Settings");
            }

            LOGGER.info("[MMOSInstallerSettings] Settings saved to " + SETTINGS_PATH);

        } catch (IOException e) {
            LOGGER.error("[MMOSInstallerSettings] Failed to save settings: " + e.getMessage());
        }
    }

    /**
     * 設定を非同期で保存する。
     */
    public void saveAsync() {
        CompletableFuture.runAsync(this::save);
    }

    /**
     * 設定をリセットする（デフォルト値に戻す）。
     */
    public void reset() {
        skipDownload = false;
        downloadMirror = null;
        librariesPath = "mods/mmos-system";
        debug = false;
    }

    /**
     * ライブラリパスのPathオブジェクトを取得する。
     *
     * @return ライブラリパス
     */
    public Path getLibrariesPathAsPath() {
        return Paths.get(librariesPath);
    }

    /**
     * JCEFライブラリのパスを取得する。
     *
     * @param platform プラットフォーム
     * @return JCEFライブラリパス
     */
    public Path getJcefPath(MMOSPlatform platform) {
        return getLibrariesPathAsPath().resolve("lib").resolve("jcef").resolve(platform.getNormalizedName());
    }
}
