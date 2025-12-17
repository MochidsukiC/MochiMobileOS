package jp.moyashi.phoneos.forge.installer;

import java.util.Locale;

/**
 * プラットフォーム検出と関連情報を提供するユーティリティクラス。
 * JCEFネイティブライブラリのダウンロードに使用される。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public enum MMOSPlatform {

    /** Windows x86_64 */
    WINDOWS_AMD64("windows", "amd64"),

    /** Windows ARM64 */
    WINDOWS_ARM64("windows", "arm64"),

    /** Linux x86_64 */
    LINUX_AMD64("linux", "amd64"),

    /** Linux ARM64 */
    LINUX_ARM64("linux", "arm64"),

    /** macOS Intel */
    MACOS_AMD64("macos", "amd64"),

    /** macOS Apple Silicon */
    MACOS_ARM64("macos", "arm64"),

    /** 不明なプラットフォーム */
    UNKNOWN("unknown", "unknown");

    private final String os;
    private final String arch;

    MMOSPlatform(String os, String arch) {
        this.os = os;
        this.arch = arch;
    }

    /**
     * OS名を取得する。
     *
     * @return OS名（"windows", "linux", "macos"）
     */
    public String getOs() {
        return os;
    }

    /**
     * アーキテクチャを取得する。
     *
     * @return アーキテクチャ（"amd64", "arm64"）
     */
    public String getArch() {
        return arch;
    }

    /**
     * 正規化されたプラットフォーム名を取得する。
     * ダウンロードURLやディレクトリ名に使用される。
     *
     * @return 正規化された名前（例: "windows_amd64"）
     */
    public String getNormalizedName() {
        return os + "_" + arch;
    }

    /**
     * Windowsかどうかを判定する。
     *
     * @return Windowsの場合true
     */
    public boolean isWindows() {
        return os.equals("windows");
    }

    /**
     * Linuxかどうかを判定する。
     *
     * @return Linuxの場合true
     */
    public boolean isLinux() {
        return os.equals("linux");
    }

    /**
     * macOSかどうかを判定する。
     *
     * @return macOSの場合true
     */
    public boolean isMacOS() {
        return os.equals("macos");
    }

    /**
     * 64ビットアーキテクチャかどうかを判定する。
     *
     * @return 64ビットの場合true
     */
    public boolean is64Bit() {
        return arch.equals("amd64") || arch.equals("arm64");
    }

    /**
     * ARMアーキテクチャかどうかを判定する。
     *
     * @return ARMの場合true
     */
    public boolean isArm() {
        return arch.equals("arm64");
    }

    /**
     * 現在のプラットフォームを検出する。
     *
     * @return 検出されたプラットフォーム
     */
    public static MMOSPlatform detect() {
        String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);

        // OS検出
        boolean isWindows = osName.contains("win");
        boolean isLinux = osName.contains("linux");
        boolean isMac = osName.contains("mac") || osName.contains("darwin");

        // アーキテクチャ検出
        boolean isAmd64 = osArch.contains("amd64") || osArch.contains("x86_64");
        boolean isArm64 = osArch.contains("aarch64") || osArch.contains("arm64");

        // プラットフォーム決定
        if (isWindows) {
            if (isArm64) return WINDOWS_ARM64;
            if (isAmd64) return WINDOWS_AMD64;
        } else if (isLinux) {
            if (isArm64) return LINUX_ARM64;
            if (isAmd64) return LINUX_AMD64;
        } else if (isMac) {
            if (isArm64) return MACOS_ARM64;
            if (isAmd64) return MACOS_AMD64;
        }

        System.err.println("[MMOSPlatform] Unknown platform: os=" + osName + ", arch=" + osArch);
        return UNKNOWN;
    }

    /**
     * 現在のプラットフォームがサポートされているかどうかを確認する。
     *
     * @return サポートされている場合true
     */
    public static boolean isSupported() {
        return detect() != UNKNOWN;
    }

    /**
     * デバッグ情報を出力する。
     */
    public static void printDebugInfo() {
        System.out.println("[MMOSPlatform] System Information:");
        System.out.println("  os.name: " + System.getProperty("os.name"));
        System.out.println("  os.arch: " + System.getProperty("os.arch"));
        System.out.println("  os.version: " + System.getProperty("os.version"));
        System.out.println("  java.version: " + System.getProperty("java.version"));
        System.out.println("  Detected platform: " + detect().getNormalizedName());
    }
}
