package jp.moyashi.phoneos.forge.hardware;

import com.mojang.logging.LogUtils;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.audiochannel.ClientLocationalAudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.ClientStaticAudioChannel;
import jp.moyashi.phoneos.core.service.hardware.SpeakerSocket;
import jp.moyashi.phoneos.forge.voicechat.MochiVoicechatPlugin;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * Forge環境用のスピーカーソケット実装。
 * Simple Voice Chat (SVC) MODがインストールされている場合のみ有効になる。
 * SVCのAudioChannelを使用して音声を再生する。
 *
 * 音量レベルに応じた動作:
 * - OFF: 音声再生なし
 * - LOW: 自分だけに聞こえる（StaticAudioChannel）
 * - MEDIUM: 自分 + 周囲に聞こえる（StaticAudioChannel + LocationalAudioChannel 16ブロック）
 * - HIGH: 自分 + 遠くまで聞こえる（StaticAudioChannel + LocationalAudioChannel 48ブロック）
 */
public class ForgeSpeakerSocket implements SpeakerSocket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private VolumeLevel volumeLevel;
    private final boolean svcAvailable;
    private ClientStaticAudioChannel selfChannel;      // 自分用チャンネル（常に使用）
    private ClientLocationalAudioChannel publicChannel; // 他プレイヤー用チャンネル（MEDIUM以上）
    private Thread playbackThread;
    private volatile boolean isPlaying = false;

    public ForgeSpeakerSocket() {
        this.volumeLevel = VolumeLevel.MEDIUM;
        this.svcAvailable = SVCDetector.isSVCAvailable();

        if (svcAvailable) {
            LOGGER.info("[ForgeSpeakerSocket] Initialized with SVC support");
        } else {
            LOGGER.info("[ForgeSpeakerSocket] Initialized without SVC (speaker unavailable)");
        }
    }

    @Override
    public boolean isAvailable() {
        // SVCが導入されている場合のみ利用可能
        return svcAvailable;
    }

    @Override
    public void setVolumeLevel(VolumeLevel level) {
        if (!svcAvailable) {
            LOGGER.warn("[ForgeSpeakerSocket] Cannot set volume - SVC not installed");
            return;
        }

        this.volumeLevel = level;
        LOGGER.info("[ForgeSpeakerSocket] Volume level set to " + level.name());

        // TODO: SVCの音量設定を変更するAPIを呼び出す
    }

    @Override
    public VolumeLevel getVolumeLevel() {
        return volumeLevel;
    }

    @Override
    public void playAudio(byte[] audioData) {
        LOGGER.info("[ForgeSpeakerSocket] playAudio() called - svcAvailable: " + svcAvailable + ", volumeLevel: " + volumeLevel + ", dataLength: " + (audioData != null ? audioData.length : 0));

        if (!svcAvailable) {
            LOGGER.warn("[ForgeSpeakerSocket] Cannot play audio - SVC not installed");
            return;
        }

        if (volumeLevel == VolumeLevel.OFF) {
            LOGGER.info("[ForgeSpeakerSocket] Volume is OFF, skipping playback");
            return;
        }

        if (audioData == null || audioData.length == 0) {
            LOGGER.warn("[ForgeSpeakerSocket] Cannot play audio - no data provided");
            return;
        }

        // 既に再生中の場合は停止
        if (isPlaying) {
            LOGGER.info("[ForgeSpeakerSocket] Stopping previous playback...");
            stopAudio();
        }

        LOGGER.info("[ForgeSpeakerSocket] Starting audio playback - volume level: " + volumeLevel + ", data: " + audioData.length + " bytes");

        // byte[]をshort[]に変換（16-bit PCM）
        short[] samples = convertBytesToShorts(audioData);
        LOGGER.info("[ForgeSpeakerSocket] Converted to " + samples.length + " samples");

        // 再生スレッドを開始
        isPlaying = true;
        playbackThread = new Thread(() -> playAudioStream(samples), "MochiOS-Speaker");
        playbackThread.setDaemon(true);
        playbackThread.start();
        LOGGER.info("[ForgeSpeakerSocket] Playback thread started");
    }

    @Override
    public void stopAudio() {
        if (!svcAvailable) {
            return;
        }

        isPlaying = false;

        // 再生スレッドの終了を待つ
        if (playbackThread != null && playbackThread.isAlive()) {
            try {
                playbackThread.join(1000);
            } catch (InterruptedException e) {
                LOGGER.warn("[ForgeSpeakerSocket] Interrupted while stopping playback", e);
            }
        }

        // チャンネルをクリーンアップ
        cleanupChannels();

        LOGGER.info("[ForgeSpeakerSocket] Stopped audio");
    }

    /**
     * 音声ストリームを再生する（別スレッドで実行）。
     */
    private void playAudioStream(short[] samples) {
        LOGGER.info("[ForgeSpeakerSocket] playAudioStream() started - samples: " + samples.length);

        try {
            VoicechatClientApi clientApi = MochiVoicechatPlugin.getClientApi();
            LOGGER.info("[ForgeSpeakerSocket] VoicechatClientApi: " + (clientApi != null ? "available" : "null"));

            if (clientApi == null) {
                LOGGER.error("[ForgeSpeakerSocket] VoicechatClientApi not available");
                return;
            }

            // チャンネルを作成
            LOGGER.info("[ForgeSpeakerSocket] Creating audio channels...");
            createChannels(clientApi);

            if (selfChannel == null) {
                LOGGER.error("[ForgeSpeakerSocket] Failed to create self audio channel");
                return;
            }

            LOGGER.info("[ForgeSpeakerSocket] Audio channels created successfully - selfChannel: " + (selfChannel != null) + ", publicChannel: " + (publicChannel != null));

            // Simple Voice Chatの推奨チャンクサイズ（20ms @ 48kHz）
            // SVCはリアルタイムストリーミング用に設計されているため、小さいチャンクが必要
            int chunkSize = 960; // 48000 * 0.02 = 960 samples (20ms)
            int offset = 0;
            int chunkCount = 0;

            LOGGER.info("[ForgeSpeakerSocket] Starting playback loop - total samples: " + samples.length + ", chunk size: " + chunkSize);

            while (isPlaying && offset < samples.length) {
                long chunkStartTime = System.nanoTime();

                int remaining = samples.length - offset;
                int currentChunkSize = Math.min(chunkSize, remaining);

                // チャンクを切り出す
                short[] chunk = new short[currentChunkSize];
                System.arraycopy(samples, offset, chunk, 0, currentChunkSize);

                // 自分用チャンネルで再生
                selfChannel.play(chunk);
                chunkCount++;

                // MEDIUM以上の場合は他プレイヤーにも送信
                if (publicChannel != null && (volumeLevel == VolumeLevel.MEDIUM || volumeLevel == VolumeLevel.HIGH)) {
                    // 位置更新は50チャンクに1回（1秒ごと）
                    if (chunkCount % 50 == 1) {
                        updatePublicChannelLocation();
                    }
                    publicChannel.play(chunk);
                }

                offset += currentChunkSize;

                // 最初のチャンクだけログ出力
                if (chunkCount == 1) {
                    LOGGER.info("[ForgeSpeakerSocket] First chunk played successfully - chunk size: " + currentChunkSize);
                }

                // 正確な20msウェイト（ナノ秒精度）
                // チャンクの処理時間を差し引いて、正確に20ms間隔を維持
                long chunkEndTime = System.nanoTime();
                long elapsedNanos = chunkEndTime - chunkStartTime;
                long targetNanos = 20_000_000L; // 20ms in nanoseconds
                long sleepNanos = targetNanos - elapsedNanos;

                if (sleepNanos > 0) {
                    long sleepMillis = sleepNanos / 1_000_000;
                    int sleepNanosRemainder = (int) (sleepNanos % 1_000_000);

                    if (sleepMillis > 0) {
                        Thread.sleep(sleepMillis, sleepNanosRemainder);
                    } else {
                        // 1ms未満の場合はビジーウェイト
                        long targetTime = System.nanoTime() + sleepNanos;
                        while (System.nanoTime() < targetTime) {
                            // ビジーウェイト
                        }
                    }
                }
            }

            LOGGER.info("[ForgeSpeakerSocket] Playback loop finished - total chunks played: " + chunkCount);

        } catch (Exception e) {
            LOGGER.error("[ForgeSpeakerSocket] Error during audio playback", e);
            e.printStackTrace();
        } finally {
            LOGGER.info("[ForgeSpeakerSocket] Cleaning up channels...");
            cleanupChannels();
            isPlaying = false;
        }
    }

    /**
     * オーディオチャンネルを作成する。
     */
    private void createChannels(VoicechatClientApi clientApi) {
        UUID channelId = UUID.randomUUID();

        // 自分用チャンネル（静的）
        selfChannel = clientApi.createStaticAudioChannel(channelId);
        LOGGER.info("[ForgeSpeakerSocket] Created self audio channel");

        // MEDIUM以上の場合は他プレイヤー用チャンネルも作成
        if (volumeLevel == VolumeLevel.MEDIUM || volumeLevel == VolumeLevel.HIGH) {
            Player player = Minecraft.getInstance().player;
            if (player != null) {
                Vec3 pos = player.position();
                Position position = new SimplePosition(pos.x, pos.y, pos.z);

                publicChannel = clientApi.createLocationalAudioChannel(UUID.randomUUID(), position);

                // 音量レベルに応じて範囲を設定
                float distance = volumeLevel == VolumeLevel.HIGH ? 48.0f : 16.0f;
                publicChannel.setDistance(distance);

                LOGGER.info("[ForgeSpeakerSocket] Created public audio channel (distance: " + distance + ")");
            }
        }
    }

    /**
     * 他プレイヤー用チャンネルの位置を更新する。
     */
    private void updatePublicChannelLocation() {
        if (publicChannel == null) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player != null) {
            Vec3 pos = player.position();
            publicChannel.setLocation(new SimplePosition(pos.x, pos.y, pos.z));
        }
    }

    /**
     * チャンネルをクリーンアップする。
     */
    private void cleanupChannels() {
        selfChannel = null;
        publicChannel = null;
    }

    /**
     * byte配列をshort配列に変換する（16-bit PCM、リトルエンディアン）。
     */
    private short[] convertBytesToShorts(byte[] bytes) {
        short[] shorts = new short[bytes.length / 2];
        for (int i = 0; i < shorts.length; i++) {
            int offset = i * 2;
            shorts[i] = (short) ((bytes[offset + 1] << 8) | (bytes[offset] & 0xFF));
        }
        return shorts;
    }

    /**
     * 簡易Position実装。
     */
    private static class SimplePosition implements Position {
        private final double x, y, z;

        public SimplePosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public double getX() {
            return x;
        }

        @Override
        public double getY() {
            return y;
        }

        @Override
        public double getZ() {
            return z;
        }
    }
}