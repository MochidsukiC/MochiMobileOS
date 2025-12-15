package jp.moyashi.phoneos.api.hardware;

/**
 * IC通信API。
 * オンオフのプロパティを持つ。ICチップから突発的に送られる情報をバイパスする。
 */
public interface ICSocket {
    /**
     * IC通信データ種別。
     */
    enum ICDataType {
        NONE,
        BLOCK,
        ENTITY
    }

    /**
     * IC通信データクラス。
     */
    class ICData {
        public final ICDataType type;
        public final int blockX;
        public final int blockY;
        public final int blockZ;
        public final String entityUUID;

        public ICData() {
            this.type = ICDataType.NONE;
            this.blockX = 0;
            this.blockY = 0;
            this.blockZ = 0;
            this.entityUUID = null;
        }

        public ICData(int x, int y, int z) {
            this.type = ICDataType.BLOCK;
            this.blockX = x;
            this.blockY = y;
            this.blockZ = z;
            this.entityUUID = null;
        }

        public ICData(String entityUUID) {
            this.type = ICDataType.ENTITY;
            this.blockX = 0;
            this.blockY = 0;
            this.blockZ = 0;
            this.entityUUID = entityUUID;
        }
    }

    /**
     * IC通信が利用可能かどうかを取得する。
     *
     * @return 利用可能な場合true
     */
    boolean isAvailable();

    /**
     * IC通信を有効化する。
     *
     * @param enabled 有効にする場合true
     */
    void setEnabled(boolean enabled);

    /**
     * IC通信が有効かどうかを取得する。
     *
     * @return 有効な場合true
     */
    boolean isEnabled();

    /**
     * IC通信データを取得する（1回のみ、取得後はクリアされる）。
     *
     * @return IC通信データ、データがない場合はtype=NONEのデータ
     */
    ICData pollICData();
}
