package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.service.hardware.SpeakerSocket;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * 通知音再生サービス。
 * デフォルトの通知音またはユーザー設定の通知音を再生する。
 */
public class NotificationSoundService {

    /** デフォルトの通知音パス（リソースから） */
    private static final String DEFAULT_SOUND_RESOURCE = "/sounds/notification.wav";

    /** 設定キー：通知音ファイルパス */
    private static final String SETTING_SOUND_PATH = "notification.sound_path";

    private final SpeakerSocket speakerSocket;
    private final SettingsManager settingsManager;
    private final VFS vfs;

    /** デフォルト通知音のキャッシュ */
    private byte[] defaultSoundData;

    /** 再生中フラグ */
    private volatile boolean isPlaying = false;

    /**
     * NotificationSoundServiceを作成する。
     *
     * @param speakerSocket スピーカーソケット
     * @param settingsManager 設定マネージャー
     * @param vfs 仮想ファイルシステム
     */
    public NotificationSoundService(SpeakerSocket speakerSocket, SettingsManager settingsManager, VFS vfs) {
        this.speakerSocket = speakerSocket;
        this.settingsManager = settingsManager;
        this.vfs = vfs;

        // デフォルト通知音を事前読み込み
        loadDefaultSound();

        System.out.println("NotificationSoundService: Initialized");
    }

    /**
     * デフォルトの通知音をリソースから読み込む。
     */
    private void loadDefaultSound() {
        try (InputStream is = getClass().getResourceAsStream(DEFAULT_SOUND_RESOURCE)) {
            if (is != null) {
                defaultSoundData = is.readAllBytes();
                System.out.println("NotificationSoundService: Default sound loaded (" + defaultSoundData.length + " bytes)");
            } else {
                System.out.println("NotificationSoundService: Default sound resource not found, will use fallback beep");
                defaultSoundData = null;
            }
        } catch (Exception e) {
            System.err.println("NotificationSoundService: Error loading default sound: " + e.getMessage());
            defaultSoundData = null;
        }
    }

    /**
     * 通知音を再生する。
     * 設定でカスタム通知音が指定されていればそれを、なければデフォルト音を再生する。
     */
    public void playNotificationSound() {
        if (isPlaying) {
            return; // 既に再生中の場合はスキップ
        }

        // 別スレッドで再生（UIブロック防止）
        new Thread(() -> {
            isPlaying = true;
            try {
                byte[] soundData = getSoundData();
                if (soundData != null && soundData.length > 0) {
                    playWavData(soundData);
                } else {
                    playFallbackBeep();
                }
            } catch (Exception e) {
                System.err.println("NotificationSoundService: Error playing sound: " + e.getMessage());
                playFallbackBeep();
            } finally {
                isPlaying = false;
            }
        }, "NotificationSound").start();
    }

    /**
     * 通知音データを取得する。
     * カスタム設定があればVFSから読み込み、なければデフォルト音を返す。
     */
    private byte[] getSoundData() {
        // カスタム通知音の設定を確認
        if (settingsManager != null) {
            String customPath = settingsManager.getStringSetting(SETTING_SOUND_PATH, null);
            if (customPath != null && !customPath.isEmpty()) {
                try {
                    byte[] customData = vfs.readBinaryFile(customPath);
                    if (customData != null && customData.length > 0) {
                        return customData;
                    }
                } catch (Exception e) {
                    System.err.println("NotificationSoundService: Error loading custom sound: " + e.getMessage());
                }
            }
        }

        // デフォルト音を返す
        return defaultSoundData;
    }

    /**
     * WAVデータを再生する。
     */
    private void playWavData(byte[] wavData) {
        try (AudioInputStream audioStream = AudioSystem.getAudioInputStream(
                new ByteArrayInputStream(wavData))) {

            AudioFormat format = audioStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                System.err.println("NotificationSoundService: Audio line not supported");
                playFallbackBeep();
                return;
            }

            try (Clip clip = (Clip) AudioSystem.getLine(info)) {
                clip.open(audioStream);
                clip.start();

                // 再生完了まで待機（最大3秒）
                long startTime = System.currentTimeMillis();
                while (clip.isRunning() && (System.currentTimeMillis() - startTime) < 3000) {
                    Thread.sleep(50);
                }

                clip.stop();
            }

        } catch (Exception e) {
            System.err.println("NotificationSoundService: Error playing WAV: " + e.getMessage());
            playFallbackBeep();
        }
    }

    /**
     * フォールバックのビープ音を再生する。
     * WAVファイルが利用できない場合に使用。
     */
    private void playFallbackBeep() {
        try {
            // 簡易ビープ音（Toolkit.beep()はGUI環境が必要なため、代替手段）
            System.out.println("NotificationSoundService: Playing fallback beep");
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Exception e) {
            // ビープも失敗した場合は無視
            System.err.println("NotificationSoundService: Fallback beep also failed");
        }
    }

    /**
     * 通知音設定のパスを設定する。
     *
     * @param path VFS内の音声ファイルパス（nullでデフォルトに戻す）
     */
    public void setCustomSoundPath(String path) {
        if (settingsManager != null) {
            if (path == null || path.isEmpty()) {
                // デフォルトに戻す
                settingsManager.setSetting(SETTING_SOUND_PATH, "");
            } else {
                settingsManager.setSetting(SETTING_SOUND_PATH, path);
            }
            settingsManager.saveSettings();
        }
    }

    /**
     * 現在の通知音設定パスを取得する。
     *
     * @return 通知音ファイルパス（デフォルトの場合はnull）
     */
    public String getCustomSoundPath() {
        if (settingsManager != null) {
            String path = settingsManager.getStringSetting(SETTING_SOUND_PATH, null);
            return (path != null && !path.isEmpty()) ? path : null;
        }
        return null;
    }
}
