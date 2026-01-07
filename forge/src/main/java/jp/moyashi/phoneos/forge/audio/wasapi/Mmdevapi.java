package jp.moyashi.phoneos.forge.audio.wasapi;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

/**
 * mmdevapi.dll のJNAバインディング。
 * WASAPI Process-Specific Audio Captureに使用する。
 */
public interface Mmdevapi extends StdCallLibrary {

    Mmdevapi INSTANCE = Native.load("mmdevapi", Mmdevapi.class, W32APIOptions.DEFAULT_OPTIONS);

    /**
     * ActivateAudioInterfaceAsync - 非同期でオーディオインターフェースをアクティブ化する。
     * Windows 10 build 20348以降でProcess Loopback Captureをサポート。
     *
     * @param deviceInterfacePath デバイスインターフェースパス
     *        Process Loopbackの場合は VIRTUAL_AUDIO_DEVICE_PROCESS_LOOPBACK
     * @param riid 要求するインターフェースのIID (通常 IID_IAudioClient)
     * @param activationParams アクティベーションパラメータ (PROPVARIANT)
     * @param completionHandler 完了ハンドラー (IActivateAudioInterfaceCompletionHandler)
     * @param activationOperation 出力: アクティベーション操作オブジェクト
     * @return HRESULT
     */
    HRESULT ActivateAudioInterfaceAsync(
        String deviceInterfacePath,
        GUID.ByReference riid,
        PropVariant activationParams,
        Pointer completionHandler,
        PointerByReference activationOperation
    );
}
