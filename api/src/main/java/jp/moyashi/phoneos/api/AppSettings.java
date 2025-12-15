package jp.moyashi.phoneos.api;

import java.util.Set;

/**
 * アプリケーション固有設定API。
 * 各アプリは自分専用の設定領域のみにアクセス可能。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public interface AppSettings {

    /**
     * 文字列設定を取得する。
     *
     * @param key 設定キー
     * @param defaultValue デフォルト値
     * @return 設定値、存在しない場合はデフォルト値
     */
    String getString(String key, String defaultValue);

    /**
     * 文字列設定を保存する。
     *
     * @param key 設定キー
     * @param value 設定値
     */
    void setString(String key, String value);

    /**
     * 整数設定を取得する。
     *
     * @param key 設定キー
     * @param defaultValue デフォルト値
     * @return 設定値、存在しない場合はデフォルト値
     */
    int getInt(String key, int defaultValue);

    /**
     * 整数設定を保存する。
     *
     * @param key 設定キー
     * @param value 設定値
     */
    void setInt(String key, int value);

    /**
     * 浮動小数点設定を取得する。
     *
     * @param key 設定キー
     * @param defaultValue デフォルト値
     * @return 設定値、存在しない場合はデフォルト値
     */
    float getFloat(String key, float defaultValue);

    /**
     * 浮動小数点設定を保存する。
     *
     * @param key 設定キー
     * @param value 設定値
     */
    void setFloat(String key, float value);

    /**
     * 真偽値設定を取得する。
     *
     * @param key 設定キー
     * @param defaultValue デフォルト値
     * @return 設定値、存在しない場合はデフォルト値
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * 真偽値設定を保存する。
     *
     * @param key 設定キー
     * @param value 設定値
     */
    void setBoolean(String key, boolean value);

    /**
     * 設定が存在するか確認する。
     *
     * @param key 設定キー
     * @return 存在する場合true
     */
    boolean contains(String key);

    /**
     * 設定を削除する。
     *
     * @param key 設定キー
     */
    void remove(String key);

    /**
     * 全ての設定キーを取得する。
     *
     * @return 設定キーのセット
     */
    Set<String> keys();

    /**
     * 設定を永続化する。
     * 通常は自動保存されるが、即座に保存が必要な場合に使用。
     */
    void save();
}
