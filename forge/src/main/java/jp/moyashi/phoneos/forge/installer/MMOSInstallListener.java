package jp.moyashi.phoneos.forge.installer;

/**
 * MMOSインストーラーの状態管理シングルトン。
 * インストール進捗、完了状態、エラー情報を保持し、
 * GUIとバックグラウンドスレッド間で状態を共有する。
 *
 * @author MochiOS Team
 * @version 1.0
 */
public class MMOSInstallListener {

    /** シングルトンインスタンス */
    public static final MMOSInstallListener INSTANCE = new MMOSInstallListener();

    /** 現在のタスク名（"Downloading...", "Extracting..."等） */
    private volatile String task = "";

    /** インストール進捗（0.0 ~ 1.0） */
    private volatile float progress = 0.0f;

    /** インストール完了フラグ */
    private volatile boolean done = false;

    /** インストール失敗フラグ */
    private volatile boolean failed = false;

    /** エラーメッセージ（失敗時のみ） */
    private volatile String errorMessage = null;

    /** JCEF初期化完了フラグ */
    private volatile boolean jcefInitialized = false;

    /** プライベートコンストラクタ（シングルトン） */
    private MMOSInstallListener() {
    }

    // ========================================
    // Getter
    // ========================================

    /**
     * 現在のタスク名を取得する。
     *
     * @return タスク名
     */
    public String getTask() {
        return task;
    }

    /**
     * インストール進捗を取得する。
     *
     * @return 進捗（0.0 ~ 1.0）
     */
    public float getProgress() {
        return progress;
    }

    /**
     * インストールが完了したかどうかを取得する。
     *
     * @return 完了していればtrue
     */
    public boolean isDone() {
        return done;
    }

    /**
     * インストールが失敗したかどうかを取得する。
     *
     * @return 失敗していればtrue
     */
    public boolean isFailed() {
        return failed;
    }

    /**
     * エラーメッセージを取得する。
     *
     * @return エラーメッセージ（失敗していない場合はnull）
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * JCEFが初期化されているかどうかを取得する。
     *
     * @return 初期化されていればtrue
     */
    public boolean isJcefInitialized() {
        return jcefInitialized;
    }

    // ========================================
    // Setter
    // ========================================

    /**
     * 現在のタスク名を設定する。
     *
     * @param task タスク名
     */
    public void setTask(String task) {
        this.task = task;
    }

    /**
     * インストール進捗を設定する。
     *
     * @param progress 進捗（0.0 ~ 1.0）
     */
    public void setProgress(float progress) {
        this.progress = Math.max(0.0f, Math.min(1.0f, progress));
    }

    /**
     * インストール完了状態を設定する。
     *
     * @param done 完了していればtrue
     */
    public void setDone(boolean done) {
        this.done = done;
        if (done) {
            this.progress = 1.0f;
        }
    }

    /**
     * インストール失敗状態を設定する。
     *
     * @param failed 失敗していればtrue
     */
    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    /**
     * エラーメッセージを設定する。
     *
     * @param errorMessage エラーメッセージ
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * JCEF初期化状態を設定する。
     *
     * @param initialized 初期化されていればtrue
     */
    public void setJcefInitialized(boolean initialized) {
        this.jcefInitialized = initialized;
    }

    // ========================================
    // ユーティリティ
    // ========================================

    /**
     * 外部プロセスモード用：インストール完了状態に設定する。
     * JCEFはサーバー側でcoreモジュールが管理するため、Forge側でのインストールは不要。
     * この静的メソッドは待機画面をスキップするために使用される。
     */
    public static void setInstalled() {
        INSTANCE.setDone(true);
        INSTANCE.setJcefInitialized(true);
        INSTANCE.setTask("External process mode - JCEF managed by server");
    }

    /**
     * 状態をリセットする（再インストール用）。
     */
    public void reset() {
        this.task = "";
        this.progress = 0.0f;
        this.done = false;
        this.failed = false;
        this.errorMessage = null;
        // jcefInitializedはリセットしない（一度初期化されたら維持）
    }

    /**
     * 進捗パーセンテージを取得する。
     *
     * @return パーセンテージ（0 ~ 100）
     */
    public int getProgressPercent() {
        return Math.round(progress * 100);
    }

    /**
     * 表示用のステータステキストを取得する。
     *
     * @return ステータステキスト
     */
    public String getStatusText() {
        if (failed) {
            return "Installation failed: " + (errorMessage != null ? errorMessage : "Unknown error");
        }
        if (done) {
            return "Installation complete";
        }
        if (task.isEmpty()) {
            return "Preparing...";
        }
        return task + " " + getProgressPercent() + "%";
    }
}
