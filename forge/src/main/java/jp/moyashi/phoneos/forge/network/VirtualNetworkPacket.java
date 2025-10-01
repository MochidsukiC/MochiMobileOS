package jp.moyashi.phoneos.forge.network;

import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 仮想ネットワークパケットのMinecraft送信用ラッパー
 */
public class VirtualNetworkPacket {

    private String sourceAddress;
    private String destinationAddress;
    private String packetType;
    private Map<String, String> data;

    /**
     * デフォルトコンストラクタ（デシリアライズ用）
     */
    public VirtualNetworkPacket() {
        this.data = new HashMap<>();
    }

    /**
     * VirtualNetworkPacketを構築します
     * @param packet coreモジュールのVirtualPacket
     */
    public VirtualNetworkPacket(VirtualPacket packet) {
        this.sourceAddress = packet.getSource().toString();
        this.destinationAddress = packet.getDestination().toString();
        this.packetType = packet.getType().name();
        this.data = new HashMap<>();

        // データを文字列形式に変換
        for (Map.Entry<String, Object> entry : packet.getData().entrySet()) {
            this.data.put(entry.getKey(), entry.getValue() != null ? entry.getValue().toString() : "");
        }
    }

    /**
     * ByteBufからパケットをデコードします
     * @param buf ByteBuf
     */
    public static VirtualNetworkPacket decode(FriendlyByteBuf buf) {
        VirtualNetworkPacket packet = new VirtualNetworkPacket();
        packet.sourceAddress = buf.readUtf();
        packet.destinationAddress = buf.readUtf();
        packet.packetType = buf.readUtf();

        int dataSize = buf.readInt();
        packet.data = new HashMap<>();
        for (int i = 0; i < dataSize; i++) {
            String key = buf.readUtf();
            String value = buf.readUtf();
            packet.data.put(key, value);
        }

        return packet;
    }

    /**
     * パケットをByteBufにエンコードします
     * @param buf ByteBuf
     */
    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(sourceAddress);
        buf.writeUtf(destinationAddress);
        buf.writeUtf(packetType);

        buf.writeInt(data.size());
        for (Map.Entry<String, String> entry : data.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    /**
     * パケット受信時の処理
     * @param ctx ネットワークコンテキスト
     */
    public void handle(Supplier<NetworkEvent.Context> ctx) {
        System.out.println("[VirtualNetworkPacket] Packet received: " + packetType + " from " + sourceAddress + " to " + destinationAddress);
        System.out.println("[VirtualNetworkPacket] About to enqueue work");
        ctx.get().enqueueWork(() -> {
            System.out.println("[VirtualNetworkPacket] Work executing on thread: " + Thread.currentThread().getName());
            try {
                // VirtualPacketに変換
                VirtualPacket virtualPacket = toVirtualPacket();
                System.out.println("[VirtualNetworkPacket] VirtualPacket created");

                // VirtualRouterに転送
                System.out.println("[VirtualNetworkPacket] Calling NetworkHandler.handleReceivedPacket");
                NetworkHandler.handleReceivedPacket(virtualPacket, ctx.get());
                System.out.println("[VirtualNetworkPacket] NetworkHandler.handleReceivedPacket completed");
            } catch (Exception e) {
                System.err.println("[VirtualNetworkPacket] Error in packet handling: " + e.getMessage());
                e.printStackTrace();
            }
        });
        System.out.println("[VirtualNetworkPacket] setPacketHandled called");
        ctx.get().setPacketHandled(true);
    }

    /**
     * coreモジュールのVirtualPacketに変換します
     * @return VirtualPacket
     */
    public VirtualPacket toVirtualPacket() {
        IPvMAddress source = IPvMAddress.fromString(sourceAddress);
        IPvMAddress destination = IPvMAddress.fromString(destinationAddress);
        VirtualPacket.PacketType type = VirtualPacket.PacketType.valueOf(packetType);

        Map<String, Object> packetData = new HashMap<>();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            packetData.put(entry.getKey(), entry.getValue());
        }

        return new VirtualPacket(source, destination, type, packetData);
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public String getPacketType() {
        return packetType;
    }

    public Map<String, String> getData() {
        return data;
    }
}