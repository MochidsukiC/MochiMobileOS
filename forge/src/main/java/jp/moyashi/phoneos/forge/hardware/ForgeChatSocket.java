package jp.moyashi.phoneos.forge.hardware;

import com.mojang.logging.LogUtils;
import jp.moyashi.phoneos.core.service.hardware.ChatSocket;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * Forge環境用のチャットソケット実装。
 * MochiMobileOSの通知をMinecraftのプレイヤーチャットに表示する。
 */
public class ForgeChatSocket implements ChatSocket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String CHAT_PREFIX = "[MochiOS] ";

    public ForgeChatSocket() {
        LOGGER.info("[ForgeChatSocket] Initialized");
    }

    @Override
    public boolean isAvailable() {
        // Minecraftクライアントが存在し、プレイヤーがワールドにいる場合に利用可能
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.player != null;
    }

    @Override
    public void sendMessage(String message) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                // クライアント側のみに表示（サーバーには送信しない）
                Component chatMessage = Component.literal(CHAT_PREFIX + message);
                mc.player.displayClientMessage(chatMessage, false);
                LOGGER.debug("[ForgeChatSocket] Sent message: {}", message);
            } else {
                LOGGER.warn("[ForgeChatSocket] Cannot send message - player not available");
            }
        } catch (Exception e) {
            LOGGER.error("[ForgeChatSocket] Error sending message: {}", e.getMessage());
        }
    }
}
