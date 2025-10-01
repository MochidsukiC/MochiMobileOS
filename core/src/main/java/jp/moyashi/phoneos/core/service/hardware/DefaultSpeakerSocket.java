package jp.moyashi.phoneos.core.service.hardware;

/**
 * スピーカーソケットのデフォルト実装（standalone用）。
 * アプリケーションとして音声を流す（Java標準のAudio APIを使用）。
 */
public class DefaultSpeakerSocket implements SpeakerSocket {
    private VolumeLevel volumeLevel = VolumeLevel.MEDIUM;

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void setVolumeLevel(VolumeLevel level) {
        this.volumeLevel = level;
    }

    @Override
    public VolumeLevel getVolumeLevel() {
        return volumeLevel;
    }

    @Override
    public void playAudio(byte[] audioData) {
        // TODO: Java標準のAudio APIを使用して音声を再生
        System.out.println("DefaultSpeakerSocket: Playing audio with volume level " + volumeLevel);
    }

    @Override
    public void stopAudio() {
        // TODO: 音声再生を停止
        System.out.println("DefaultSpeakerSocket: Stopping audio");
    }
}