package jp.moyashi.phoneos.core.service.permission;

/**
 * システムで定義されている権限の種類。
 * Androidのパーミッションシステムに準拠しつつ、MochiMobileOS独自の権限も定義する。
 */
public enum Permission {
    // ハードウェアアクセス権限
    RECORD_AUDIO("android.permission.RECORD_AUDIO", "マイク", "音声を録音する", true),
    CAMERA("android.permission.CAMERA", "カメラ", "写真や動画を撮影する", true),
    ACCESS_FINE_LOCATION("android.permission.ACCESS_FINE_LOCATION", "位置情報", "正確な位置情報にアクセスする", true),
    BLUETOOTH("android.permission.BLUETOOTH", "Bluetooth", "Bluetoothデバイスに接続する", false),
    BLUETOOTH_CONNECT("android.permission.BLUETOOTH_CONNECT", "Bluetooth接続", "ペアリング済みデバイスに接続する", true),

    // ストレージアクセス権限
    READ_EXTERNAL_STORAGE("android.permission.READ_EXTERNAL_STORAGE", "ストレージ読み取り", "ファイルやメディアを読み取る", true),
    WRITE_EXTERNAL_STORAGE("android.permission.WRITE_EXTERNAL_STORAGE", "ストレージ書き込み", "ファイルやメディアを保存する", true),

    // 通信権限
    INTERNET("android.permission.INTERNET", "インターネット", "ネットワーク通信を行う", false),
    ACCESS_NETWORK_STATE("android.permission.ACCESS_NETWORK_STATE", "ネットワーク状態", "ネットワーク接続状態を確認する", false),

    // システム権限
    VIBRATE("android.permission.VIBRATE", "バイブレーション", "デバイスを振動させる", false),
    WAKE_LOCK("android.permission.WAKE_LOCK", "スリープ解除", "デバイスのスリープを防ぐ", false),

    // 通知権限
    POST_NOTIFICATIONS("android.permission.POST_NOTIFICATIONS", "通知", "通知を表示する", true),

    // MochiMobileOS独自権限
    IC_COMMUNICATION("jp.moyashi.phoneos.permission.IC_COMMUNICATION", "IC通信", "IC通信（NFC）を使用する", true),
    VIRTUAL_NETWORK("jp.moyashi.phoneos.permission.VIRTUAL_NETWORK", "仮想ネットワーク", "仮想ネットワークにアクセスする", false),
    SYSTEM_SETTINGS("jp.moyashi.phoneos.permission.SYSTEM_SETTINGS", "システム設定", "システム設定を変更する", true);

    private final String permissionString;
    private final String displayName;
    private final String description;
    private final boolean dangerous;  // 危険な権限（ユーザーの明示的な許可が必要）

    Permission(String permissionString, String displayName, String description, boolean dangerous) {
        this.permissionString = permissionString;
        this.displayName = displayName;
        this.description = description;
        this.dangerous = dangerous;
    }

    /**
     * 権限文字列を取得する。
     * @return 権限文字列（例: "android.permission.CAMERA"）
     */
    public String getPermissionString() {
        return permissionString;
    }

    /**
     * ユーザー向けの表示名を取得する。
     * @return 表示名（例: "カメラ"）
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * 権限の説明を取得する。
     * @return 説明文
     */
    public String getDescription() {
        return description;
    }

    /**
     * 危険な権限かどうかを判定する。
     * 危険な権限はユーザーの明示的な許可が必要。
     * @return 危険な権限の場合true
     */
    public boolean isDangerous() {
        return dangerous;
    }

    /**
     * 権限文字列からPermissionを取得する。
     * @param permissionString 権限文字列
     * @return 対応するPermission、見つからない場合はnull
     */
    public static Permission fromString(String permissionString) {
        for (Permission permission : values()) {
            if (permission.permissionString.equals(permissionString)) {
                return permission;
            }
        }
        return null;
    }
}
