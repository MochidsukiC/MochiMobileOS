package jp.moyashi.phoneos.core.service.hardware;

import java.util.ArrayList;
import java.util.List;

/**
 * AudioDeviceSocketのデフォルト実装（Standalone用）。
 * 常にシステムデフォルトを使用する。
 */
public class DefaultAudioDeviceSocket implements AudioDeviceSocket {

    @Override
    public List<AudioDevice> getAvailableMicrophones() {
        List<AudioDevice> devices = new ArrayList<>();
        devices.add(new AudioDevice(DEVICE_ID_SYSTEM_DEFAULT, "System Default", true));
        return devices;
    }

    @Override
    public List<AudioDevice> getAvailableSpeakers() {
        List<AudioDevice> devices = new ArrayList<>();
        devices.add(new AudioDevice(DEVICE_ID_SYSTEM_DEFAULT, "System Default", true));
        return devices;
    }

    @Override
    public String getSelectedMicrophoneId() {
        return DEVICE_ID_SYSTEM_DEFAULT;
    }

    @Override
    public boolean setSelectedMicrophone(String deviceId) {
        // デフォルト実装ではデバイス選択をサポートしない
        return DEVICE_ID_SYSTEM_DEFAULT.equals(deviceId);
    }

    @Override
    public String getSelectedSpeakerId() {
        return DEVICE_ID_SYSTEM_DEFAULT;
    }

    @Override
    public boolean setSelectedSpeaker(String deviceId) {
        // デフォルト実装ではデバイス選択をサポートしない
        return DEVICE_ID_SYSTEM_DEFAULT.equals(deviceId);
    }

    @Override
    public void refreshDevices() {
        // デフォルト実装では何もしない
    }
}
