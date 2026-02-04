package jp.moyashi.phoneos.core.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.ArrayList;

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
        this(null);
    }

    /**
     * ワールドIDを指定して新しいVFSインスタンスを構築する。
     * ワールド/サーバー毎にデータをサンドボックス化する。
     * Forge環境で使用され、./mochi_os_data/{worldId}/ にデータを保存する。
     *
     * @param worldId ワールドID（nullの場合は共通データ）
     */
    public VFS(String worldId) {
        try {
            // ルートディレクトリパスを設定（ワールドID毎にサンドボックス化）
            if (worldId != null && !worldId.isEmpty()) {
                // セキュリティ: worldIdのパストラバーサル対策
                // 英数字、ハイフン、アンダースコアのみ許可
                if (!worldId.matches("^[a-zA-Z0-9_-]+$")) {
                    throw new IllegalArgumentException("Invalid worldId: contains unsafe characters. Only alphanumeric, hyphen and underscore allowed.");
                }
                // Forge環境: mochi_os_data/{worldId}/
                this.rootPath = Paths.get("mochi_os_data", worldId);
            } else {
                // スタンドアロン環境: mochi_os_data/
                this.rootPath = Paths.get("mochi_os_data");
            }
            this.systemPath = rootPath.resolve("system");

            // 必要なディレクトリを作成
            Files.createDirectories(rootPath);
            Files.createDirectories(systemPath);


        } catch (IOException e) {
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
                return true;
            } else {
                return false;
            }

        } catch (IOException e) {
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
                return null;
            }

            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            return content;

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 仮想ファイルからバイナリデータを読み取る。
     *
     * @param path 読み取るファイルのパス（VFS内の相対パス）
     * @return ファイルの内容をバイト配列として返す、ファイルが見つからない場合はnull
     */
    public byte[] readBinaryFile(String path) {
        try {
            Path filePath = resolveVFSPath(path);

            if (!Files.exists(filePath)) {
                return null;
            }

            byte[] data = Files.readAllBytes(filePath);
            return data;

        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 仮想ファイルにバイナリデータを書き込む。
     *
     * @param path 書き込み先ファイルのパス（VFS内の相対パス）
     * @param data ファイルに書き込むバイナリデータ
     * @return 書き込みが成功した場合true、失敗した場合false
     */
    public boolean writeBinaryFile(String path, byte[] data) {
        try {
            Path filePath = resolveVFSPath(path);

            // 親ディレクトリが存在しない場合は作成
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // ファイルに書き込み（既存ファイルを上書き）
            Files.write(filePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;

        } catch (IOException e) {
            return false;
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
            Files.writeString(filePath, data, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return true;

        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 仮想ファイルにデータを追記する。
     * ファイルが存在しない場合は新規作成する。
     *
     * @param path 追記先ファイルのパス（VFS内の相対パス）
     * @param data 追記するデータ
     * @return 追記が成功した場合true、失敗した場合false
     */
    public boolean appendFile(String path, String data) {
        try {
            Path filePath = resolveVFSPath(path);

            // 親ディレクトリが存在しない場合は作成
            Path parentDir = filePath.getParent();
            if (parentDir != null && !Files.exists(parentDir)) {
                Files.createDirectories(parentDir);
            }

            // ファイルに追記（存在しない場合は作成）
            Files.writeString(filePath, data, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            return true;

        } catch (IOException e) {
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
                return false;
            }

            Files.delete(filePath);
            return true;

        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * VFS内の相対パスを実際のファイルシステムパスに変換する。
     * パストラバーサル攻撃を防止するため、解決後のパスがVFSルート内にあることを検証する。
     *
     * @param vfsPath VFS内の相対パス
     * @return 実際のファイルシステムパス
     * @throws SecurityException パスがVFSルート外を指す場合
     */
    private Path resolveVFSPath(String vfsPath) {
        // null/空パスのチェック
        if (vfsPath == null || vfsPath.isEmpty()) {
            return rootPath;
        }

        // パスの正規化（先頭の/を除去）
        String normalizedPath = vfsPath.startsWith("/") ? vfsPath.substring(1) : vfsPath;

        // Windows絶対パス/UNCパスの検出
        if (normalizedPath.matches("^[A-Za-z]:.*") || normalizedPath.startsWith("\\\\")) {
            throw new SecurityException("VFS: 絶対パス/UNCパスは許可されていません: " + vfsPath);
        }

        // パスを解決して正規化
        Path resolved = rootPath.resolve(normalizedPath).normalize();
        Path absoluteRoot = rootPath.toAbsolutePath().normalize();
        Path absoluteResolved = resolved.toAbsolutePath().normalize();

        // 解決後のパスがVFSルート内にあることを検証（パストラバーサル防止）
        if (!absoluteResolved.startsWith(absoluteRoot)) {
            throw new SecurityException("VFS: パストラバーサルは許可されていません: " + vfsPath);
        }

        return resolved;
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
    
    /**
     * ディレクトリが存在するかどうかを確認する。
     * 
     * @param path 確認するディレクトリのパス（VFS内の相対パス）
     * @return ディレクトリが存在する場合true、しない場合false
     */
    public boolean directoryExists(String path) {
        Path dirPath = resolveVFSPath(path);
        return Files.exists(dirPath) && Files.isDirectory(dirPath);
    }
    
    /**
     * ディレクトリを作成する。
     * 
     * @param path 作成するディレクトリのパス（VFS内の相対パス）
     * @return ディレクトリの作成に成功した場合true、失敗した場合false
     */
    public boolean createDirectory(String path) {
        try {
            Path dirPath = resolveVFSPath(path);
            Files.createDirectories(dirPath);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 指定されたディレクトリ内のファイル一覧を取得する。
     * 
     * @param directoryPath ディレクトリのパス（VFS内の相対パス）
     * @return ファイル名のリスト、ディレクトリが存在しない場合は空のリスト
     */
    public List<String> listFiles(String directoryPath) {
        try {
            Path dirPath = resolveVFSPath(directoryPath);
            
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return new ArrayList<>();
            }

            try (Stream<Path> stream = Files.list(dirPath)) {
                List<String> files = stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());

                return files;
            }

        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * 指定されたディレクトリ内の特定の拡張子を持つファイル一覧を取得する。
     * 
     * @param directoryPath ディレクトリのパス（VFS内の相対パス）
     * @param extension 拡張子（例: ".jar", ".class"）
     * @return 指定拡張子のファイル名のリスト、ディレクトリが存在しない場合は空のリスト
     */
    public List<String> listFilesByExtension(String directoryPath, String extension) {
        try {
            Path dirPath = resolveVFSPath(directoryPath);
            
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return new ArrayList<>();
            }

            try (Stream<Path> stream = Files.list(dirPath)) {
                List<String> files = stream
                    .filter(Files::isRegularFile)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .filter(name -> name.toLowerCase().endsWith(extension.toLowerCase()))
                    .collect(Collectors.toList());

                return files;
            }

        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * 指定されたディレクトリ内のサブディレクトリ一覧を取得する。
     * 
     * @param directoryPath ディレクトリのパス（VFS内の相対パス）
     * @return サブディレクトリ名のリスト、ディレクトリが存在しない場合は空のリスト
     */
    public List<String> listDirectories(String directoryPath) {
        try {
            Path dirPath = resolveVFSPath(directoryPath);
            
            if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
                return new ArrayList<>();
            }

            try (Stream<Path> stream = Files.list(dirPath)) {
                List<String> directories = stream
                    .filter(Files::isDirectory)
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toList());

                return directories;
            }

        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * ファイルの完全パスを取得する。
     * 
     * @param path VFS内の相対パス
     * @return 実際のファイルシステムでの完全パス
     */
    public String getFullPath(String path) {
        return resolveVFSPath(path).toAbsolutePath().toString();
    }
}