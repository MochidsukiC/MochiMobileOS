package jp.moyashi.phoneos.core.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * スマートフォンOS用の仮想ファイルシステムサービス。
 * このクラスはファイル操作を管理し、実際のファイルシステムへの永続化機能を提供する。
 * 実行ディレクトリに「mochi_os_data」フォルダを作成し、そこにOSデータを保存する。
 * 
 * @author YourName
 * @version 2.0
 */
public class VFS {
    
    /** OSデータを格納するルートディレクトリパス */
    private final Path rootPath;
    
    /** システムファイル用のサブディレクトリパス */
    private final Path systemPath;
    
    /**
     * 新しいVFSインスタンスを構築する。
     * 実行ディレクトリにmochi_os_dataフォルダを作成し、必要なサブディレクトリを初期化する。
     */
    public VFS() {
        try {
            // ルートディレクトリパスを設定
            this.rootPath = Paths.get("mochi_os_data");
            this.systemPath = rootPath.resolve("system");
            
            // 必要なディレクトリを作成
            Files.createDirectories(rootPath);
            Files.createDirectories(systemPath);
            
            System.out.println("VFS: 仮想ファイルシステムを初期化完了");
            System.out.println("VFS: ルートパス: " + rootPath.toAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("VFS: ディレクトリ初期化エラー: " + e.getMessage());
            throw new RuntimeException("VFSの初期化に失敗しました", e);
        }
    }
    
    /**
     * 指定されたパスに仮想ファイルを作成する。
     * 
     * @param path 作成するファイルのパス（VFS内の相対パス）
     * @return ファイルの作成に成功した場合true、失敗した場合false
     */
    public boolean createFile(String path) {
        try {
            Path filePath = resolveVFSPath(path);
            
            // 親ディレクトリが存在しない場合は作成
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // ファイルを作成（存在しない場合のみ）
            if (!Files.exists(filePath)) {
                Files.createFile(filePath);
                System.out.println("VFS: ファイル作成成功: " + path);
                return true;
            } else {
                System.out.println("VFS: ファイルは既に存在: " + path);
                return false;
            }
            
        } catch (IOException e) {
            System.err.println("VFS: ファイル作成エラー [" + path + "]: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 仮想ファイルからデータを読み取る。
     * 
     * @param path 読み取るファイルのパス（VFS内の相対パス）
     * @return ファイルの内容を文字列として返す、ファイルが見つからない場合はnull
     */
    public String readFile(String path) {
        try {
            Path filePath = resolveVFSPath(path);
            
            if (!Files.exists(filePath)) {
                System.out.println("VFS: ファイルが見つかりません: " + path);
                return null;
            }
            
            String content = Files.readString(filePath);
            System.out.println("VFS: ファイル読み込み成功: " + path + " (" + content.length() + "文字)");
            return content;
            
        } catch (IOException e) {
            System.err.println("VFS: ファイル読み込みエラー [" + path + "]: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 仮想ファイルにデータを書き込む。
     * 
     * @param path 書き込み先ファイルのパス（VFS内の相対パス）
     * @param data ファイルに書き込むデータ
     * @return 書き込みが成功した場合true、失敗した場合false
     */
    public boolean writeFile(String path, String data) {
        try {
            Path filePath = resolveVFSPath(path);
            
            // 親ディレクトリが存在しない場合は作成
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }
            
            // ファイルに書き込み（既存ファイルを上書き）
            Files.writeString(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            System.out.println("VFS: ファイル書き込み成功: " + path + " (" + data.length() + "文字)");
            return true;
            
        } catch (IOException e) {
            System.err.println("VFS: ファイル書き込みエラー [" + path + "]: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 仮想ファイルを削除する。
     * 
     * @param path 削除するファイルのパス（VFS内の相対パス）
     * @return ファイルの削除に成功した場合true、失敗した場合false
     */
    public boolean deleteFile(String path) {
        try {
            Path filePath = resolveVFSPath(path);
            
            if (!Files.exists(filePath)) {
                System.out.println("VFS: 削除対象のファイルが見つかりません: " + path);
                return false;
            }
            
            Files.delete(filePath);
            System.out.println("VFS: ファイル削除成功: " + path);
            return true;
            
        } catch (IOException e) {
            System.err.println("VFS: ファイル削除エラー [" + path + "]: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * VFS内の相対パスを実際のファイルシステムパスに変換する。
     * 
     * @param vfsPath VFS内の相対パス
     * @return 実際のファイルシステムパス
     */
    private Path resolveVFSPath(String vfsPath) {
        // パスの正規化（先頭の/を除去など）
        String normalizedPath = vfsPath.startsWith("/") ? vfsPath.substring(1) : vfsPath;
        return rootPath.resolve(normalizedPath);
    }
    
    /**
     * VFSのルートパスを取得する。
     * 
     * @return ルートパス
     */
    public Path getRootPath() {
        return rootPath;
    }
    
    /**
     * ファイルが存在するかどうかを確認する。
     * 
     * @param path 確認するファイルのパス（VFS内の相対パス）
     * @return ファイルが存在する場合true、しない場合false
     */
    public boolean fileExists(String path) {
        Path filePath = resolveVFSPath(path);
        return Files.exists(filePath);
    }
}