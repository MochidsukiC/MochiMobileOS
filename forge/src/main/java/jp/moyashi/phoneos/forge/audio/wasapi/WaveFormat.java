package jp.moyashi.phoneos.forge.audio.wasapi;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

import java.util.Arrays;
import java.util.List;

/**
 * WAVEFORMATEX構造体のJNA表現。
 */
@Structure.FieldOrder({"wFormatTag", "nChannels", "nSamplesPerSec", "nAvgBytesPerSec", "nBlockAlign", "wBitsPerSample", "cbSize"})
public class WaveFormat extends Structure {
    public short wFormatTag;
    public short nChannels;
    public int nSamplesPerSec;
    public int nAvgBytesPerSec;
    public short nBlockAlign;
    public short wBitsPerSample;
    public short cbSize;

    public WaveFormat() {
        super();
    }

    public WaveFormat(Pointer p) {
        super(p);
        read();
    }

    /**
     * 48kHz, 16-bit, ステレオのPCMフォーマットを作成する。
     */
    public static WaveFormat createPCM48kHz16bitStereo() {
        WaveFormat fmt = new WaveFormat();
        fmt.wFormatTag = WasapiConstants.WAVE_FORMAT_PCM;
        fmt.nChannels = 2;
        fmt.nSamplesPerSec = 48000;
        fmt.wBitsPerSample = 16;
        fmt.nBlockAlign = (short) (fmt.nChannels * fmt.wBitsPerSample / 8);
        fmt.nAvgBytesPerSec = fmt.nSamplesPerSec * fmt.nBlockAlign;
        fmt.cbSize = 0;
        return fmt;
    }

    /**
     * 48kHz, 32-bit float, ステレオのフォーマットを作成する。
     * WASAPI loopbackは通常floatフォーマットでデータを返す。
     */
    public static WaveFormat createFloat48kHzStereo() {
        WaveFormat fmt = new WaveFormat();
        fmt.wFormatTag = WasapiConstants.WAVE_FORMAT_IEEE_FLOAT;
        fmt.nChannels = 2;
        fmt.nSamplesPerSec = 48000;
        fmt.wBitsPerSample = 32;
        fmt.nBlockAlign = (short) (fmt.nChannels * fmt.wBitsPerSample / 8);
        fmt.nAvgBytesPerSec = fmt.nSamplesPerSec * fmt.nBlockAlign;
        fmt.cbSize = 0;
        return fmt;
    }

    @Override
    public String toString() {
        return String.format("WaveFormat[tag=%d, channels=%d, rate=%d, bits=%d, blockAlign=%d]",
            wFormatTag, nChannels, nSamplesPerSec, wBitsPerSample, nBlockAlign);
    }

    public static class ByReference extends WaveFormat implements Structure.ByReference {
        public ByReference() {
            super();
        }

        public ByReference(Pointer p) {
            super(p);
        }
    }
}
