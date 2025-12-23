package jp.moyashi.phoneos.core.service.hardware;

/**
 * チャットソケットのデフォルト実装（standalone用）。
 * スタンドアロン環境ではチャット機能は利用できないため、
 * メッセージはコンソールに出力するのみ。
 */
public class DefaultChatSocket implements ChatSocket {

    @Override
    public boolean isAvailable() {
        // スタンドアロンではチャット機能は利用不可
        return false;
    }

    @Override
    public void sendMessage(String message) {
        // スタンドアロンでは何もしない（デバッグ用にログ出力）
        System.out.println("DefaultChatSocket: (not sent) " + message);
    }
}
