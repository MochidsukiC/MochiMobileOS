package jp.moyashi.phoneos.core.apps.network;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.MessageStorage;
import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;
import jp.moyashi.phoneos.core.service.network.VirtualRouter;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.List;

/**
 * ネットワークアプリのメイン画面
 */
public class NetworkScreen implements Screen {

    private final Kernel kernel;
    private MessageStorage messageStorage;
    private VirtualRouter virtualRouter;
    private List<MessageStorage.Message> messages;
    private int scrollOffset = 0;
    private static final int MESSAGE_HEIGHT = 80;
    private static final int BUTTON_HEIGHT = 40;

    public NetworkScreen(Kernel kernel) {
        this.kernel = kernel;
        this.messageStorage = kernel.getMessageStorage();
        this.virtualRouter = kernel.getVirtualRouter();
    }

    @Override
    public void setup(PGraphics g) {
        System.out.println("[NetworkScreen] Network screen setup");
        loadMessages();
    }

    @Override
    public void draw(PGraphics g) {
        // 背景
        g.background(245, 245, 250);

        // ヘッダー
        drawHeader(g);

        // メッセージ一覧
        drawMessages(g);

        // フッター（ボタン）
        drawFooter(g);
    }

    /**
     * ヘッダーを描画
     */
    private void drawHeader(PGraphics g) {
        g.fill(100, 150, 255);
        g.noStroke();
        g.rect(0, 0, kernel.width, 60);

        g.fill(255);
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(20);
        g.text("Network & Messages", kernel.width / 2, 30);
    }

    /**
     * メッセージ一覧を描画
     */
    private void drawMessages(PGraphics g) {
        int y = 70;
        int contentHeight = kernel.height - 70 - 100; // ヘッダーとフッターを除く

        if (messages == null || messages.isEmpty()) {
            g.fill(100);
            g.textAlign(PApplet.CENTER, PApplet.CENTER);
            g.textSize(16);
            g.text("No messages", kernel.width / 2, y + contentHeight / 2);
            return;
        }

        // メッセージを描画
        for (int i = 0; i < messages.size(); i++) {
            int messageY = y + i * MESSAGE_HEIGHT - scrollOffset;

            // 画面外のメッセージはスキップ
            if (messageY + MESSAGE_HEIGHT < y || messageY > y + contentHeight) {
                continue;
            }

            MessageStorage.Message message = messages.get(i);
            drawMessage(g, message, messageY);
        }
    }

    /**
     * 個別のメッセージを描画
     */
    private void drawMessage(PGraphics g, MessageStorage.Message message, int y) {
        // 背景
        if (message.isRead()) {
            g.fill(255);
        } else {
            g.fill(240, 250, 255);
        }
        g.stroke(200);
        g.strokeWeight(1);
        g.rect(10, y, kernel.width - 20, MESSAGE_HEIGHT - 5);

        // 送信者名
        g.fill(0);
        g.textAlign(PApplet.LEFT, PApplet.TOP);
        g.textSize(14);
        g.text(message.getSenderName(), 20, y + 10);

        // 未読マーク
        if (!message.isRead()) {
            g.fill(255, 100, 100);
            g.noStroke();
            g.ellipse(kernel.width - 30, y + 20, 10, 10);
        }

        // メッセージ内容
        g.fill(80);
        g.textSize(12);
        String content = message.getContent();
        if (content.length() > 50) {
            content = content.substring(0, 50) + "...";
        }
        g.text(content, 20, y + 35);

        // タイムスタンプ
        g.fill(150);
        g.textSize(10);
        g.text(formatTimestamp(message.getTimestamp()), 20, y + 55);
    }

    /**
     * フッター（ボタン）を描画
     */
    private void drawFooter(PGraphics g) {
        int footerY = kernel.height - 100;

        // テストメッセージ送信ボタン
        drawButton(g, "Send Test Message", 10, footerY, kernel.width - 20, BUTTON_HEIGHT, 100, 200, 100);

        // システムパケット送信ボタン
        drawButton(g, "Send to System", 10, footerY + 50, kernel.width - 20, BUTTON_HEIGHT, 100, 150, 255);
    }

    /**
     * ボタンを描画
     */
    private void drawButton(PGraphics g, String label, int x, int y, int w, int h, int r, int gr, int b) {
        g.fill(r, gr, b);
        g.noStroke();
        g.rect(x, y, w, h, 5);

        g.fill(255);
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(14);
        g.text(label, x + w / 2, y + h / 2);
    }

    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        int footerY = kernel.height - 100;

        // テストメッセージ送信ボタン
        if (mouseY >= footerY && mouseY <= footerY + BUTTON_HEIGHT) {
            sendTestMessage();
        }

        // システムパケット送信ボタン
        if (mouseY >= footerY + 50 && mouseY <= footerY + 50 + BUTTON_HEIGHT) {
            sendSystemPacket();
        }

        // メッセージをタップして既読にする
        int messageY = 70;
        int contentHeight = kernel.height - 70 - 100;
        if (mouseY >= messageY && mouseY <= messageY + contentHeight) {
            int index = (mouseY - messageY + scrollOffset) / MESSAGE_HEIGHT;
            if (index >= 0 && index < messages.size()) {
                MessageStorage.Message message = messages.get(index);
                if (!message.isRead()) {
                    messageStorage.markAsRead(message.getId());
                    loadMessages();
                }
            }
        }
    }

    /**
     * テストメッセージを送信
     */
    private void sendTestMessage() {
        System.out.println("[NetworkScreen] Sending test message");

        // フォールバックUUIDを使用（統合サーバーではプレイヤーUUIDを取得できないため）
        String myUUID = "00000000-0000-0000-0000-000000000000";
        IPvMAddress myAddress = IPvMAddress.forPlayer(myUUID);

        // 自分自身宛てにメッセージを送信（テスト用）
        VirtualPacket packet = VirtualPacket.builder()
                .source(myAddress)
                .destination(myAddress)
                .type(VirtualPacket.PacketType.MESSAGE)
                .put("message", "Test message sent at " + System.currentTimeMillis())
                .put("sender_name", "Test User")
                .build();

        if (virtualRouter != null) {
            // sendPacket()を使用してネットワーク経由で送信
            virtualRouter.sendPacket(packet);
            System.out.println("[NetworkScreen] Test message sent to self");
        }

        // 画面を更新
        loadMessages();
    }

    /**
     * システムアドレスにパケットを送信
     */
    private void sendSystemPacket() {
        System.out.println("[NetworkScreen] Sending system packet");

        // フォールバックUUIDを使用
        String myUUID = "00000000-0000-0000-0000-000000000000";
        IPvMAddress myAddress = IPvMAddress.forPlayer(myUUID);
        IPvMAddress systemAddress = VirtualRouter.getAppDataServerAddress();

        VirtualPacket packet = VirtualPacket.builder()
                .source(myAddress)
                .destination(systemAddress)
                .type(VirtualPacket.PacketType.APP_INSTALL_REQUEST)
                .put("app_id", "test.app.example")
                .put("app_name", "Test Application")
                .build();

        if (virtualRouter != null) {
            virtualRouter.sendPacket(packet);
            System.out.println("[NetworkScreen] System packet sent");
        }
    }

    /**
     * メッセージを読み込み
     */
    private void loadMessages() {
        if (messageStorage != null) {
            messages = messageStorage.getAllMessages();
            System.out.println("[NetworkScreen] Loaded " + messages.size() + " messages");
        }
    }

    /**
     * タイムスタンプをフォーマット
     */
    private String formatTimestamp(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) {
            return "Just now";
        } else if (diff < 3600000) {
            return (diff / 60000) + " minutes ago";
        } else if (diff < 86400000) {
            return (diff / 3600000) + " hours ago";
        } else {
            return (diff / 86400000) + " days ago";
        }
    }

    @Override
    public String getScreenTitle() {
        return "Network";
    }
}