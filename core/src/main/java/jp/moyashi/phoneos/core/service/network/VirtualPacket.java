package jp.moyashi.phoneos.core.service.network;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 仮想ネットワーク通信のパケットを表すクラス
 */
public class VirtualPacket {

    /**
     * パケットタイプを表す列挙型
     */
    public enum PacketType {
        /** アプリケーションインストール要求 */
        APP_INSTALL_REQUEST,
        /** アプリケーションインストール応答 */
        APP_INSTALL_RESPONSE,
        /** データ転送 */
        DATA_TRANSFER,
        /** メッセージ送信 */
        MESSAGE,
        /** 電子マネー処理要求 */
        PAYMENT_REQUEST,
        /** 電子マネー処理応答 */
        PAYMENT_RESPONSE,
        /** 汎用リクエスト */
        GENERIC_REQUEST,
        /** 汎用レスポンス */
        GENERIC_RESPONSE,
        /** カスタム (外部Modが独自に定義) */
        CUSTOM
    }

    private final IPvMAddress source;
    private final IPvMAddress destination;
    private final PacketType type;
    private final Map<String, Object> data;
    private final long timestamp;

    /**
     * VirtualPacketを構築します
     * @param source 送信元アドレス
     * @param destination 送信先アドレス
     * @param type パケットタイプ
     * @param data パケットデータ
     */
    public VirtualPacket(IPvMAddress source, IPvMAddress destination, PacketType type, Map<String, Object> data) {
        if (source == null) {
            throw new IllegalArgumentException("Source address cannot be null");
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination address cannot be null");
        }
        if (type == null) {
            throw new IllegalArgumentException("Packet type cannot be null");
        }

        this.source = source;
        this.destination = destination;
        this.type = type;
        this.data = data != null ? new HashMap<>(data) : new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 送信元アドレスを取得します
     * @return 送信元IPvMAddress
     */
    public IPvMAddress getSource() {
        return source;
    }

    /**
     * 送信先アドレスを取得します
     * @return 送信先IPvMAddress
     */
    public IPvMAddress getDestination() {
        return destination;
    }

    /**
     * パケットタイプを取得します
     * @return PacketType
     */
    public PacketType getType() {
        return type;
    }

    /**
     * パケットデータを取得します
     * @return データマップ（読み取り専用ではない）
     */
    public Map<String, Object> getData() {
        return data;
    }

    /**
     * パケットのタイムスタンプを取得します
     * @return UNIXタイムスタンプ（ミリ秒）
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 特定のキーのデータを取得します
     * @param key データキー
     * @return データ値（存在しない場合はnull）
     */
    public Object get(String key) {
        return data.get(key);
    }

    /**
     * 特定のキーのデータを文字列として取得します
     * @param key データキー
     * @return データ値（存在しない場合はnull）
     */
    public String getString(String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 特定のキーのデータを整数として取得します
     * @param key データキー
     * @return データ値（存在しない、または変換できない場合は0）
     */
    public int getInt(String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    /**
     * 特定のキーのデータを真偽値として取得します
     * @param key データキー
     * @return データ値（存在しない場合はfalse）
     */
    public boolean getBoolean(String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    /**
     * パケットビルダーを作成します
     * @return VirtualPacket.Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * VirtualPacketビルダークラス
     */
    public static class Builder {
        private IPvMAddress source;
        private IPvMAddress destination;
        private PacketType type;
        private Map<String, Object> data = new HashMap<>();

        public Builder source(IPvMAddress source) {
            this.source = source;
            return this;
        }

        public Builder destination(IPvMAddress destination) {
            this.destination = destination;
            return this;
        }

        public Builder type(PacketType type) {
            this.type = type;
            return this;
        }

        public Builder data(Map<String, Object> data) {
            this.data = new HashMap<>(data);
            return this;
        }

        public Builder put(String key, Object value) {
            this.data.put(key, value);
            return this;
        }

        public VirtualPacket build() {
            return new VirtualPacket(source, destination, type, data);
        }
    }

    @Override
    public String toString() {
        return "VirtualPacket{" +
                "source=" + source +
                ", destination=" + destination +
                ", type=" + type +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        VirtualPacket that = (VirtualPacket) obj;
        return timestamp == that.timestamp &&
                Objects.equals(source, that.source) &&
                Objects.equals(destination, that.destination) &&
                type == that.type &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination, type, data, timestamp);
    }
}