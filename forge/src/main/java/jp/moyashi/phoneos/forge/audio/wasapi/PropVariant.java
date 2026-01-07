package jp.moyashi.phoneos.forge.audio.wasapi;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WTypes;

/**
 * PROPVARIANT構造体のJNA表現（簡略版）。
 * ActivateAudioInterfaceAsyncにパラメータを渡すために使用。
 */
@Structure.FieldOrder({"vt", "wReserved1", "wReserved2", "wReserved3", "blobVal"})
public class PropVariant extends Structure {
    // VARTYPE values
    public static final short VT_EMPTY = 0;
    public static final short VT_BLOB = 65;

    public short vt;
    public short wReserved1;
    public short wReserved2;
    public short wReserved3;
    public BlobData blobVal;

    public PropVariant() {
        super();
        vt = VT_EMPTY;
    }

    /**
     * AudioClientActivationParamsを含むPROPVARIANTを作成する。
     */
    public static PropVariant createBlob(AudioClientActivationParams params) {
        PropVariant pv = new PropVariant();
        pv.vt = VT_BLOB;
        pv.blobVal = new BlobData();

        // 構造体をメモリに書き込む
        params.write();
        int size = params.size();

        // BLOBデータを設定
        pv.blobVal.cbSize = size;
        pv.blobVal.pBlobData = params.getPointer();

        return pv;
    }

    /**
     * BLOB構造体。
     */
    @Structure.FieldOrder({"cbSize", "pBlobData"})
    public static class BlobData extends Structure {
        public int cbSize;
        public Pointer pBlobData;

        public BlobData() {
            super();
        }
    }

    public static class ByReference extends PropVariant implements Structure.ByReference {
    }
}
