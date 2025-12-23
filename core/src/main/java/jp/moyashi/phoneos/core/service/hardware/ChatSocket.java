package jp.moyashi.phoneos.core.service.hardware;

/**
 * チャット通知API。
 * 通知をハードウェア固有の方法で外部に送信する。
 *
 * 環境別動作:
 * - standalone: 何もしない（チャット機能なし）
 * - forge-mod: Minecraftのプレイヤーチャットに通知を送信
 */
public interface ChatSocket {

    /**
     * チャット機能が利用可能かどうかを取得する。
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();

    /**
     * チャットにメッセージを送信する。
     *
     * @param message 送信するメッセージ
     */
    void sendMessage(String message);
}
