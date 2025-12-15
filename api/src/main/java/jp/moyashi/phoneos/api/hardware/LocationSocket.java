package jp.moyashi.phoneos.api.hardware;

/**
 * 位置情報ソケットAPI。
 * スマートフォンの位置情報をx, y, zの3つの座標系と電波精度で提供する。
 */
public interface LocationSocket {
    /**
     * 位置情報データクラス。
     */
    class LocationData {
        public final double x;
        public final double y;
        public final double z;
        public final float accuracy;

        public LocationData(double x, double y, double z, float accuracy) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.accuracy = accuracy;
        }
    }

    /**
     * 位置情報サービスが利用可能かどうかを取得する。
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();

    /**
     * 現在の位置情報を取得する。
     *
     * @return 位置情報データ
     */
    LocationData getLocation();

    /**
     * 位置情報サービスを有効化する。
     *
     * @param enabled 有効にする場合true
     */
    void setEnabled(boolean enabled);

    /**
     * 位置情報サービスが有効かどうかを取得する。
     *
     * @return 有効な場合true
     */
    boolean isEnabled();
}
