package jp.moyashi.phoneos.forge.hardware;

import com.mojang.logging.LogUtils;
import jp.moyashi.phoneos.core.service.hardware.AudioDeviceSocket;
import org.slf4j.Logger;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Forge環境用のオーディオデバイス設定ソケット実装。
 * Java Sound APIを使用してオーディオデバイスを列挙・選択する。
 */
public class ForgeAudioDeviceSocket implements AudioDeviceSocket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String PREF_MICROPHONE_ID = "mmos_microphone_id";
    private static final String PREF_SPEAKER_ID = "mmos_speaker_id";

    private final Preferences prefs;
    private List<AudioDevice> cachedMicrophones = new ArrayList<>();
    private List<AudioDevice> cachedSpeakers = new ArrayList<>();
    private String selectedMicrophoneId;
    private String selectedSpeakerId;

    // デバイス変更リスナー
    private Runnable onMicrophoneChangeListener;
    private Runnable onSpeakerChangeListener;

    public ForgeAudioDeviceSocket() {
        this.prefs = Preferences.userNodeForPackage(ForgeAudioDeviceSocket.class);
        this.selectedMicrophoneId = prefs.get(PREF_MICROPHONE_ID, DEVICE_ID_SYSTEM_DEFAULT);
        this.selectedSpeakerId = prefs.get(PREF_SPEAKER_ID, DEVICE_ID_SYSTEM_DEFAULT);

        refreshDevices();

        LOGGER.info("[ForgeAudioDeviceSocket] Initialized - Microphone: {}, Speaker: {}",
            selectedMicrophoneId, selectedSpeakerId);
    }

    @Override
    public List<AudioDevice> getAvailableMicrophones() {
        return new ArrayList<>(cachedMicrophones);
    }

    @Override
    public List<AudioDevice> getAvailableSpeakers() {
        return new ArrayList<>(cachedSpeakers);
    }

    @Override
    public String getSelectedMicrophoneId() {
        return selectedMicrophoneId;
    }

    @Override
    public boolean setSelectedMicrophone(String deviceId) {
        // デバイスIDが有効か確認
        if (!DEVICE_ID_SYSTEM_DEFAULT.equals(deviceId)) {
            boolean found = cachedMicrophones.stream()
                .anyMatch(d -> d.getId().equals(deviceId));
            if (!found) {
                LOGGER.warn("[ForgeAudioDeviceSocket] Microphone not found: {}", deviceId);
                return false;
            }
        }

        selectedMicrophoneId = deviceId;
        prefs.put(PREF_MICROPHONE_ID, deviceId);
        LOGGER.info("[ForgeAudioDeviceSocket] Selected microphone: {}", deviceId);

        // リスナーに通知
        if (onMicrophoneChangeListener != null) {
            onMicrophoneChangeListener.run();
        }

        return true;
    }

    @Override
    public String getSelectedSpeakerId() {
        return selectedSpeakerId;
    }

    @Override
    public boolean setSelectedSpeaker(String deviceId) {
        // デバイスIDが有効か確認
        if (!DEVICE_ID_SYSTEM_DEFAULT.equals(deviceId)) {
            boolean found = cachedSpeakers.stream()
                .anyMatch(d -> d.getId().equals(deviceId));
            if (!found) {
                LOGGER.warn("[ForgeAudioDeviceSocket] Speaker not found: {}", deviceId);
                return false;
            }
        }

        selectedSpeakerId = deviceId;
        prefs.put(PREF_SPEAKER_ID, deviceId);
        LOGGER.info("[ForgeAudioDeviceSocket] Selected speaker: {}", deviceId);

        // リスナーに通知
        if (onSpeakerChangeListener != null) {
            onSpeakerChangeListener.run();
        }

        return true;
    }

    @Override
    public void refreshDevices() {
        cachedMicrophones.clear();
        cachedSpeakers.clear();

        // システムデフォルトを追加
        cachedMicrophones.add(new AudioDevice(DEVICE_ID_SYSTEM_DEFAULT, "System Default", true));
        cachedSpeakers.add(new AudioDevice(DEVICE_ID_SYSTEM_DEFAULT, "System Default", true));

        // 全ミキサーを列挙
        Mixer.Info[] mixerInfos = AudioSystem.getMixerInfo();

        for (Mixer.Info mixerInfo : mixerInfos) {
            String name = mixerInfo.getName();
            String id = mixerInfo.getName(); // 名前をIDとして使用

            try {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);

                // マイク（入力デバイス）を確認
                Line.Info[] targetLines = mixer.getTargetLineInfo();
                for (Line.Info lineInfo : targetLines) {
                    if (lineInfo instanceof DataLine.Info) {
                        DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                        if (TargetDataLine.class.isAssignableFrom(dataLineInfo.getLineClass())) {
                            // 重複チェック
                            boolean exists = cachedMicrophones.stream()
                                .anyMatch(d -> d.getId().equals(id));
                            if (!exists && !name.isEmpty()) {
                                cachedMicrophones.add(new AudioDevice(id, name, false));
                                LOGGER.debug("[ForgeAudioDeviceSocket] Found microphone: {}", name);
                            }
                            break;
                        }
                    }
                }

                // スピーカー（出力デバイス）を確認
                Line.Info[] sourceLines = mixer.getSourceLineInfo();
                for (Line.Info lineInfo : sourceLines) {
                    if (lineInfo instanceof DataLine.Info) {
                        DataLine.Info dataLineInfo = (DataLine.Info) lineInfo;
                        if (SourceDataLine.class.isAssignableFrom(dataLineInfo.getLineClass())) {
                            // 重複チェック
                            boolean exists = cachedSpeakers.stream()
                                .anyMatch(d -> d.getId().equals(id));
                            if (!exists && !name.isEmpty()) {
                                cachedSpeakers.add(new AudioDevice(id, name, false));
                                LOGGER.debug("[ForgeAudioDeviceSocket] Found speaker: {}", name);
                            }
                            break;
                        }
                    }
                }

            } catch (Exception e) {
                LOGGER.debug("[ForgeAudioDeviceSocket] Error checking mixer {}: {}", name, e.getMessage());
            }
        }

        LOGGER.info("[ForgeAudioDeviceSocket] Refreshed devices - {} microphones, {} speakers",
            cachedMicrophones.size(), cachedSpeakers.size());
    }

    /**
     * 選択されたマイクのMixer.Infoを取得する。
     *
     * @return Mixer.Info、システムデフォルトの場合はnull
     */
    public Mixer.Info getSelectedMicrophoneMixerInfo() {
        if (DEVICE_ID_SYSTEM_DEFAULT.equals(selectedMicrophoneId)) {
            return null;
        }

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(selectedMicrophoneId)) {
                return info;
            }
        }

        return null;
    }

    /**
     * 選択されたスピーカーのMixer.Infoを取得する。
     *
     * @return Mixer.Info、システムデフォルトの場合はnull
     */
    public Mixer.Info getSelectedSpeakerMixerInfo() {
        if (DEVICE_ID_SYSTEM_DEFAULT.equals(selectedSpeakerId)) {
            return null;
        }

        for (Mixer.Info info : AudioSystem.getMixerInfo()) {
            if (info.getName().equals(selectedSpeakerId)) {
                return info;
            }
        }

        return null;
    }

    /**
     * マイク変更リスナーを設定する。
     */
    public void setOnMicrophoneChangeListener(Runnable listener) {
        this.onMicrophoneChangeListener = listener;
    }

    /**
     * スピーカー変更リスナーを設定する。
     */
    public void setOnSpeakerChangeListener(Runnable listener) {
        this.onSpeakerChangeListener = listener;
    }
}
