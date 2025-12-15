package jp.moyashi.phoneos.core.service.network;

import jp.moyashi.phoneos.core.Kernel;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 仮想ネットワークアダプター。
 * IPvM（IP virtual Mobile）アドレスへの通信を管理する。
 *
 * <p>ソケットが設定されていない場合は「圏外」状態となり、
 * すべての通信リクエストは {@link NetworkException} をスローする。</p>
 *
 * <p>使用例:</p>
 * <pre>{@code
 * VirtualAdapter adapter = kernel.getNetworkAdapter().getVirtualAdapter();
 *
 * // ソケットが設定されているか確認
 * if (adapter.isAvailable()) {
 *     // IPvMアドレスへHTTPリクエスト
 *     VirtualSocket.VirtualHttpResponse response =
 *         adapter.httpRequest("3-sys-appdata", "/index.html", "GET").get();
 * } else {
 *     // 圏外
 *     System.out.println("Status: " + adapter.getStatus().getDisplayName());
 * }
 * }</pre>
 *
 * @author MochiOS Team
 * @version 2.0
 */
public class VirtualAdapter {

    private final Kernel kernel;
    private VirtualSocket socket;
    private Consumer<VirtualPacket> internalPacketListener;

    /**
     * VirtualAdapterを構築する。
     *
     * @param kernel Kernelインスタンス
     */
    public VirtualAdapter(Kernel kernel) {
        this.kernel = kernel;
        this.socket = null;
    }

    /**
     * 仮想ソケットを設定する。
     * 外部モジュール（Forge/Standalone）から呼び出される。
     *
     * @param socket 仮想ソケット実装
     */
    public void setSocket(VirtualSocket socket) {
        if (this.socket != null) {
            log("Replacing existing VirtualSocket");
            this.socket.close();
        }

        this.socket = socket;

        if (socket != null) {
            // パケット受信リスナーを設定
            socket.setPacketListener(this::onPacketReceived);
            log("VirtualSocket set: " + socket.getClass().getSimpleName() +
                " (status: " + socket.getStatus().getDisplayName() + ")");
        } else {
            log("VirtualSocket cleared - now in NO_SERVICE state");
        }
    }

    /**
     * 仮想ソケットを取得する。
     *
     * @return 仮想ソケット（未設定の場合null）
     */
    public VirtualSocket getSocket() {
        return socket;
    }

    /**
     * ソケットが利用可能か（圏外でないか）を判定する。
     *
     * @return 利用可能な場合true
     */
    public boolean isAvailable() {
        return socket != null && socket.isAvailable();
    }

    /**
     * 現在のネットワーク状態を取得する。
     *
     * @return ネットワーク状態
     */
    public NetworkStatus getStatus() {
        if (socket == null) {
            return NetworkStatus.NO_SERVICE;
        }
        return socket.getStatus();
    }

    /**
     * 電波強度を取得する（0-5）。
     *
     * @return 電波強度
     */
    public int getSignalStrength() {
        if (socket == null) {
            return 0;
        }
        return socket.getSignalStrength();
    }

    /**
     * キャリア名を取得する。
     *
     * @return キャリア名
     */
    public String getCarrierName() {
        if (socket == null) {
            return "圏外";
        }
        return socket.getCarrierName();
    }

    /**
     * IPvMアドレスへHTTPリクエストを送信する。
     *
     * @param ipvmAddress IPvMアドレス文字列（例: "3-sys-appdata"）
     * @param path リクエストパス（例: "/index.html"）
     * @param method HTTPメソッド（GET, POST等）
     * @return HTTPレスポンスのFuture
     * @throws NetworkException 圏外またはネットワークエラー時
     */
    public CompletableFuture<VirtualSocket.VirtualHttpResponse> httpRequest(
            String ipvmAddress, String path, String method) throws NetworkException {

        if (socket == null) {
            throw NetworkException.noService();
        }

        if (!socket.isAvailable()) {
            throw new NetworkException("Network not available: " + socket.getStatus().getDisplayName(),
                    NetworkException.ErrorType.NO_SERVICE);
        }

        IPvMAddress destination;
        try {
            destination = IPvMAddress.fromString(ipvmAddress);
        } catch (IllegalArgumentException e) {
            throw NetworkException.unknownHost(ipvmAddress);
        }

        return socket.httpRequest(destination, path, method);
    }

    /**
     * IPvMアドレスへHTTPリクエストを送信する。
     *
     * @param destination 宛先IPvMAddress
     * @param path リクエストパス
     * @param method HTTPメソッド
     * @return HTTPレスポンスのFuture
     * @throws NetworkException 圏外またはネットワークエラー時
     */
    public CompletableFuture<VirtualSocket.VirtualHttpResponse> httpRequest(
            IPvMAddress destination, String path, String method) throws NetworkException {

        if (socket == null) {
            throw NetworkException.noService();
        }

        if (!socket.isAvailable()) {
            throw new NetworkException("Network not available: " + socket.getStatus().getDisplayName(),
                    NetworkException.ErrorType.NO_SERVICE);
        }

        return socket.httpRequest(destination, path, method);
    }

    /**
     * パケットを送信する。
     *
     * @param packet 送信パケット
     * @return 送信結果のFuture
     * @throws NetworkException 圏外時
     */
    public CompletableFuture<Boolean> sendPacket(VirtualPacket packet) throws NetworkException {
        if (socket == null) {
            throw NetworkException.noService();
        }

        if (!socket.isAvailable()) {
            throw new NetworkException("Network not available", NetworkException.ErrorType.NO_SERVICE);
        }

        return socket.sendPacket(packet);
    }

    /**
     * パケット受信リスナーを設定する。
     * 内部的なパケット処理用（VirtualRouter統合）。
     *
     * @param listener パケット受信リスナー
     */
    public void setPacketListener(Consumer<VirtualPacket> listener) {
        this.internalPacketListener = listener;
    }

    /**
     * パケット受信時の処理。
     */
    private void onPacketReceived(VirtualPacket packet) {
        if (internalPacketListener != null) {
            internalPacketListener.accept(packet);
        }

        // VirtualRouter経由でも処理（後方互換性）
        if (kernel.getVirtualRouter() != null) {
            kernel.getVirtualRouter().receivePacket(packet);
        }
    }

    /**
     * アダプターを閉じる。
     */
    public void close() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
    }

    /**
     * ログ出力。
     */
    private void log(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().info("VirtualAdapter", message);
        } else {
            System.out.println("[VirtualAdapter] " + message);
        }
    }
}
