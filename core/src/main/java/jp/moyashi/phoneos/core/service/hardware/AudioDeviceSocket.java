package jp.moyashi.phoneos.core.service.hardware;

import java.util.List;

/**
 * オーディオデバイス設定API。
 * Ch1マイク/スピーカーのデバイス選択を管理する。
 *
 * Ch2（VC音声）やCh3（環境音）はシステム管理のため、ここでは扱わない。
 */
public interface AudioDeviceSocket {

    /**
     * オーディオデバイス情報。
     */
    class AudioDevice {
        private final String id;
        private final String name;
        private final boolean isDefault;

        public AudioDevice(String id, String name, boolean isDefault) {
            this.id = id;
            this.name = name;
            this.isDefault = isDefault;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isDefault() {
            return isDefault;
        }

        @Override
        public String toString() {
            return name + (isDefault ? " (Default)" : "");
        }
    }

    /**
     * システムデフォルトを表す特別なデバイスID。
     */
    String DEVICE_ID_SYSTEM_DEFAULT = "system_default";

    /**
     * 利用可能なマイクデバイス一覧を取得する。
     *
     * @return マイクデバイスのリスト
     */
    List<AudioDevice> getAvailableMicrophones();

    /**
     * 利用可能なスピーカーデバイス一覧を取得する。
     *
     * @return スピーカーデバイスのリスト
     */
    List<AudioDevice> getAvailableSpeakers();

    /**
     * 現在選択されているマイクデバイスIDを取得する。
     *
     * @return デバイスID、システムデフォルトの場合は {@link #DEVICE_ID_SYSTEM_DEFAULT}
     */
    String getSelectedMicrophoneId();

    /**
     * マイクデバイスを選択する。
     *
     * @param deviceId デバイスID、またはシステムデフォルトを使う場合は {@link #DEVICE_ID_SYSTEM_DEFAULT}
     * @return 設定が成功した場合true
     */
    boolean setSelectedMicrophone(String deviceId);

    /**
     * 現在選択されているスピーカーデバイスIDを取得する。
     *
     * @return デバイスID、システムデフォルトの場合は {@link #DEVICE_ID_SYSTEM_DEFAULT}
     */
    String getSelectedSpeakerId();

    /**
     * スピーカーデバイスを選択する。
     *
     * @param deviceId デバイスID、またはシステムデフォルトを使う場合は {@link #DEVICE_ID_SYSTEM_DEFAULT}
     * @return 設定が成功した場合true
     */
    boolean setSelectedSpeaker(String deviceId);

    /**
     * デバイス一覧を更新（再スキャン）する。
     */
    void refreshDevices();
}
