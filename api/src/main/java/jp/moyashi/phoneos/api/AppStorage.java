package jp.moyashi.phoneos.api;

import java.util.List;

/**
 * アプリケーション専用ストレージAPI。
 * 各アプリはサンドボックス化された自分専用の領域のみにアクセス可能。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public interface AppStorage {

    /**
     * テキストファイルを読み込む。
     *
     * @param path ファイルパス（アプリルート相対）
     * @return ファイル内容、存在しない場合はnull
     */
    String readFile(String path);

    /**
     * テキストファイルを書き込む。
     *
     * @param path ファイルパス（アプリルート相対）
     * @param content 書き込む内容
     * @return 成功した場合true
     */
    boolean writeFile(String path, String content);

    /**
     * バイナリファイルを読み込む。
     *
     * @param path ファイルパス（アプリルート相対）
     * @return ファイル内容、存在しない場合はnull
     */
    byte[] readBytes(String path);

    /**
     * バイナリファイルを書き込む。
     *
     * @param path ファイルパス（アプリルート相対）
     * @param data 書き込むデータ
     * @return 成功した場合true
     */
    boolean writeBytes(String path, byte[] data);

    /**
     * ファイルまたはディレクトリが存在するか確認する。
     *
     * @param path パス（アプリルート相対）
     * @return 存在する場合true
     */
    boolean exists(String path);

    /**
     * ファイルまたはディレクトリを削除する。
     *
     * @param path パス（アプリルート相対）
     * @return 成功した場合true
     */
    boolean delete(String path);

    /**
     * ディレクトリの内容を一覧取得する。
     *
     * @param directory ディレクトリパス（アプリルート相対）
     * @return ファイル・ディレクトリ名のリスト
     */
    List<String> list(String directory);

    /**
     * ディレクトリを作成する。
     *
     * @param path ディレクトリパス（アプリルート相対）
     * @return 成功した場合true
     */
    boolean mkdir(String path);

    /**
     * ファイルかどうか確認する。
     *
     * @param path パス（アプリルート相対）
     * @return ファイルの場合true
     */
    boolean isFile(String path);

    /**
     * ディレクトリかどうか確認する。
     *
     * @param path パス（アプリルート相対）
     * @return ディレクトリの場合true
     */
    boolean isDirectory(String path);
}
