package jp.moyashi.phoneos.core.resource;

import jp.moyashi.phoneos.core.service.LoggerService;
import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PImage;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * リソース管理を担当するクラス。
 * フォント、画像、音声などのリソースの読み込み、キャッシュ、解放を管理する。
 * Kernelからリソース管理の責任を分離。
 *
 * 機能:
 * - フォントの読み込みとキャッシュ
 * - 画像の読み込みとキャッシュ
 * - リソースの遅延読み込み
 * - メモリ管理とリソース解放
 * - 日本語フォントの特別処理
 *
 * @since 2025-12-02
 * @version 1.0
 */
public class ResourceManager {

    private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());

    /** LoggerService */
    private final LoggerService loggerService;

    /** PAppletインスタンス（Processing用） */
    private PApplet applet;

    /** フォントキャッシュ */
    private final Map<String, PFont> fontCache = new HashMap<>();

    /** 画像キャッシュ */
    private final Map<String, PImage> imageCache = new HashMap<>();

    /** デフォルトフォント */
    private PFont defaultFont;

    /** 日本語フォント */
    private PFont japaneseFont;

    /** 日本語フォントのパス */
    private static final String JAPANESE_FONT_PATH = "/fonts/NotoSansJP-Regular.ttf";

    /** デフォルトフォントサイズ */
    private static final int DEFAULT_FONT_SIZE = 16;

    /** 日本語フォントサイズ */
    private static final int JAPANESE_FONT_SIZE = 16;

    /**
     * ResourceManagerを初期化する。
     *
     * @param loggerService LoggerServiceインスタンス
     */
    public ResourceManager(LoggerService loggerService) {
        this.loggerService = loggerService;
        logger.info("ResourceManager initialized");
    }

    /**
     * PAppletを設定する。
     *
     * @param applet PAppletインスタンス
     */
    public void setApplet(PApplet applet) {
        this.applet = applet;
        initializeDefaultFonts();
    }

    /**
     * デフォルトフォントを初期化する。
     */
    private void initializeDefaultFonts() {
        if (applet == null) {
            logger.warning("Cannot initialize fonts without PApplet");
            return;
        }

        // デフォルトフォントを作成
        defaultFont = applet.createFont("SansSerif", DEFAULT_FONT_SIZE);
        fontCache.put("default", defaultFont);

        // 日本語フォントを読み込む
        japaneseFont = loadJapaneseFont();
        if (japaneseFont != null) {
            fontCache.put("japanese", japaneseFont);
        }
    }

    /**
     * 日本語フォントを読み込む。
     * 複数のClassLoaderを試してリソースを読み込む（Forge環境対応）。
     *
     * @return 読み込まれたPFont、失敗時はnull
     */
    public PFont loadJapaneseFont() {
        try {
            if (loggerService != null) {
                loggerService.debug("ResourceManager", "リソースからNoto Sans JP TTFファイルを読み込み中...");
            }

            InputStream fontStream = null;

            // 1. このクラスのClassLoaderから試す
            fontStream = getClass().getResourceAsStream(JAPANESE_FONT_PATH);
            if (fontStream != null && loggerService != null) {
                loggerService.debug("ResourceManager", "ResourceManagerクラスのクラスローダーからフォントを読み込みました");
            }

            // 2. コンテキストClassLoaderから試す
            if (fontStream == null) {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                if (contextClassLoader != null) {
                    fontStream = contextClassLoader.getResourceAsStream(JAPANESE_FONT_PATH.substring(1));
                    if (fontStream != null && loggerService != null) {
                        loggerService.debug("ResourceManager", "コンテキストクラスローダーからフォントを読み込みました");
                    }
                }
            }

            // 3. システムClassLoaderから試す
            if (fontStream == null) {
                fontStream = ClassLoader.getSystemResourceAsStream(JAPANESE_FONT_PATH.substring(1));
                if (fontStream != null && loggerService != null) {
                    loggerService.debug("ResourceManager", "システムクラスローダーからフォントを読み込みました");
                }
            }

            if (fontStream == null) {
                if (loggerService != null) {
                    loggerService.error("ResourceManager", "フォントファイルが見つかりません: " + JAPANESE_FONT_PATH);
                }
                return createSystemJapaneseFont();
            }

            // InputStreamからフォントを作成
            Font awtFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            fontStream.close();

            // システムにフォントを登録
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(awtFont);

            // PFontに変換（PAppletが必要）
            if (applet != null) {
                PFont pFont = new PFont(awtFont.deriveFont((float) JAPANESE_FONT_SIZE), true);
                if (loggerService != null) {
                    loggerService.info("ResourceManager", "Noto Sans JP フォントを正常に読み込みました (サイズ: " + JAPANESE_FONT_SIZE + ")");
                }
                return pFont;
            } else {
                logger.warning("PApplet not set, cannot create PFont");
                return null;
            }

        } catch (Exception e) {
            if (loggerService != null) {
                loggerService.error("ResourceManager", "日本語フォントの読み込みエラー: " + e.getMessage());
            }
            e.printStackTrace();
            return createSystemJapaneseFont();
        }
    }

    /**
     * システムの日本語フォントを作成する（フォールバック）。
     *
     * @return システム日本語フォント
     */
    private PFont createSystemJapaneseFont() {
        if (applet == null) {
            return null;
        }

        // システムに存在する日本語フォントを探す
        String[] japaneseFonts = {
            "Yu Gothic",
            "Hiragino Sans",
            "Hiragino Kaku Gothic Pro",
            "Meiryo",
            "MS Gothic",
            "IPAGothic"
        };

        for (String fontName : japaneseFonts) {
            try {
                PFont font = applet.createFont(fontName, JAPANESE_FONT_SIZE);
                if (font != null) {
                    if (loggerService != null) {
                        loggerService.info("ResourceManager", "システム日本語フォントを使用: " + fontName);
                    }
                    return font;
                }
            } catch (Exception e) {
                // このフォントは利用できない、次を試す
            }
        }

        logger.warning("No Japanese font found, using default font");
        return defaultFont;
    }

    /**
     * フォントを読み込む。
     * キャッシュに存在する場合はキャッシュから返す。
     *
     * @param fontName フォント名
     * @param fontSize フォントサイズ
     * @return 読み込まれたPFont
     */
    public PFont loadFont(String fontName, int fontSize) {
        String cacheKey = fontName + "_" + fontSize;

        // キャッシュを確認
        if (fontCache.containsKey(cacheKey)) {
            return fontCache.get(cacheKey);
        }

        if (applet == null) {
            logger.warning("Cannot load font without PApplet");
            return null;
        }

        try {
            PFont font = applet.createFont(fontName, fontSize);
            fontCache.put(cacheKey, font);
            logger.info("Loaded font: " + fontName + " (size: " + fontSize + ")");
            return font;
        } catch (Exception e) {
            logger.warning("Failed to load font: " + fontName + " - " + e.getMessage());
            return defaultFont;
        }
    }

    /**
     * 画像を読み込む。
     * キャッシュに存在する場合はキャッシュから返す。
     *
     * @param imagePath 画像パス
     * @return 読み込まれたPImage
     */
    public PImage loadImage(String imagePath) {
        // キャッシュを確認
        if (imageCache.containsKey(imagePath)) {
            return imageCache.get(imagePath);
        }

        if (applet == null) {
            logger.warning("Cannot load image without PApplet");
            return null;
        }

        try {
            PImage image = applet.loadImage(imagePath);
            imageCache.put(imagePath, image);
            logger.info("Loaded image: " + imagePath);
            return image;
        } catch (Exception e) {
            logger.warning("Failed to load image: " + imagePath + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * デフォルトフォントを取得する。
     *
     * @return デフォルトフォント
     */
    public PFont getDefaultFont() {
        return defaultFont;
    }

    /**
     * 日本語フォントを取得する。
     *
     * @return 日本語フォント
     */
    public PFont getJapaneseFont() {
        return japaneseFont;
    }

    /**
     * 指定した名前のフォントを取得する。
     *
     * @param fontName フォント名
     * @return フォント、存在しない場合はnull
     */
    public PFont getFont(String fontName) {
        return fontCache.get(fontName);
    }

    /**
     * 指定したパスの画像を取得する。
     *
     * @param imagePath 画像パス
     * @return 画像、存在しない場合はnull
     */
    public PImage getImage(String imagePath) {
        return imageCache.get(imagePath);
    }

    /**
     * フォントキャッシュをクリアする。
     */
    public void clearFontCache() {
        fontCache.clear();
        // デフォルトフォントは保持
        if (defaultFont != null) {
            fontCache.put("default", defaultFont);
        }
        if (japaneseFont != null) {
            fontCache.put("japanese", japaneseFont);
        }
        logger.info("Font cache cleared");
    }

    /**
     * 画像キャッシュをクリアする。
     */
    public void clearImageCache() {
        imageCache.clear();
        logger.info("Image cache cleared");
    }

    /**
     * すべてのリソースを解放する。
     */
    public void releaseAllResources() {
        clearFontCache();
        clearImageCache();
        logger.info("All resources released");
    }

    /**
     * キャッシュサイズを取得する。
     *
     * @return フォントキャッシュと画像キャッシュの合計サイズ
     */
    public int getCacheSize() {
        return fontCache.size() + imageCache.size();
    }

    /**
     * フォントキャッシュサイズを取得する。
     *
     * @return フォントキャッシュサイズ
     */
    public int getFontCacheSize() {
        return fontCache.size();
    }

    /**
     * 画像キャッシュサイズを取得する。
     *
     * @return 画像キャッシュサイズ
     */
    public int getImageCacheSize() {
        return imageCache.size();
    }
}