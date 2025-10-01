package jp.moyashi.phoneos.core.service.hardware;

/**
 * モバイルデータ通信ソケットのデフォルト実装（standalone用）。
 * 常に圏外を返す。
 */
public class DefaultMobileDataSocket implements MobileDataSocket {
    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public int getSignalStrength() {
        return 0; // 圏外
    }

    @Override
    public String getServiceName() {
        return "No Service";
    }

    @Override
    public boolean isConnected() {
        return false;
    }
}