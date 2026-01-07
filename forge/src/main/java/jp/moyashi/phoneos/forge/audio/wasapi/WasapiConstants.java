package jp.moyashi.phoneos.forge.audio.wasapi;

import com.sun.jna.platform.win32.Guid.GUID;

/**
 * WASAPI関連の定数定義。
 */
public class WasapiConstants {

    // HRESULT values
    public static final int S_OK = 0;
    public static final int S_FALSE = 1;
    public static final int E_FAIL = 0x80004005;
    public static final int E_INVALIDARG = 0x80070057;
    public static final int E_NOINTERFACE = 0x80004002;

    // AUDCLNT_SHAREMODE
    public static final int AUDCLNT_SHAREMODE_SHARED = 0;
    public static final int AUDCLNT_SHAREMODE_EXCLUSIVE = 1;

    // AUDCLNT_STREAMFLAGS
    public static final int AUDCLNT_STREAMFLAGS_LOOPBACK = 0x00020000;
    public static final int AUDCLNT_STREAMFLAGS_EVENTCALLBACK = 0x00040000;
    public static final int AUDCLNT_STREAMFLAGS_NOPERSIST = 0x00080000;

    // AUDIOCLIENT_ACTIVATION_TYPE
    public static final int AUDIOCLIENT_ACTIVATION_TYPE_DEFAULT = 0;
    public static final int AUDIOCLIENT_ACTIVATION_TYPE_PROCESS_LOOPBACK = 1;

    // PROCESS_LOOPBACK_MODE
    public static final int PROCESS_LOOPBACK_MODE_INCLUDE_TARGET_PROCESS_TREE = 0;
    public static final int PROCESS_LOOPBACK_MODE_EXCLUDE_TARGET_PROCESS_TREE = 1;

    // Device ID for process loopback
    public static final String VIRTUAL_AUDIO_DEVICE_PROCESS_LOOPBACK =
        "VAD\\Process_Loopback";

    // GUIDs
    public static final GUID IID_IAudioClient = new GUID("1CB9AD4C-DBFA-4c32-B178-C2F568A703B2");
    public static final GUID IID_IAudioCaptureClient = new GUID("C8ADBD64-E71E-48a0-A4DE-185C395CD317");
    public static final GUID IID_IActivateAudioInterfaceAsyncOperation = new GUID("72A22D78-CDE4-431D-B8CC-843A71199B6D");
    public static final GUID IID_IAgileObject = new GUID("94EA2B94-E9CC-49E0-C0FF-EE64CA8F5B90");

    // CLSID for MMDeviceEnumerator
    public static final GUID CLSID_MMDeviceEnumerator = new GUID("BCDE0395-E52F-467C-8E3D-C4579291692E");
    public static final GUID IID_IMMDeviceEnumerator = new GUID("A95664D2-9614-4F35-A746-DE8DB63617E6");

    // WAVE format tags
    public static final short WAVE_FORMAT_PCM = 1;
    public static final short WAVE_FORMAT_IEEE_FLOAT = 3;
    public static final short WAVE_FORMAT_EXTENSIBLE = (short) 0xFFFE;

    // KSDATAFORMAT_SUBTYPE
    public static final GUID KSDATAFORMAT_SUBTYPE_PCM = new GUID("00000001-0000-0010-8000-00aa00389b71");
    public static final GUID KSDATAFORMAT_SUBTYPE_IEEE_FLOAT = new GUID("00000003-0000-0010-8000-00aa00389b71");

    // Reference time units (100-nanosecond intervals)
    public static final long REFTIMES_PER_SEC = 10000000L;
    public static final long REFTIMES_PER_MILLISEC = 10000L;

    // Buffer duration (100ms)
    public static final long DEFAULT_BUFFER_DURATION = REFTIMES_PER_MILLISEC * 100;
}
