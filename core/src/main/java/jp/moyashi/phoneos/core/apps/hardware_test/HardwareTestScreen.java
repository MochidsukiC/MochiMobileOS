package jp.moyashi.phoneos.core.apps.hardware_test;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.service.hardware.*;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.List;

/**
 * ハードウェアバイパスAPIのテスト画面。
 * 各APIの状態を表示し、動作確認を行う。
 */
public class HardwareTestScreen implements Screen {
    private Kernel kernel;
    private int scrollOffset = 0;
    private int contentHeight = 0;
    private int lastMouseY = 0;
    private boolean isDragging = false;
    private static final int SCROLL_SPEED = 30;
    private static final int HEADER_HEIGHT = 70;
    private static final int FOOTER_HEIGHT = 30;

    public HardwareTestScreen(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void setup(PGraphics g) {
    }

    @Override
    public void draw(PGraphics g) {
        // 背景
        g.background(20, 25, 35);

        // タイトル（固定ヘッダー）
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFF111111;
        { int c=onSurface; g.fill((c>>16)&0xFF, (c>>8)&0xFF, c&0xFF); }
        g.textAlign(PApplet.CENTER, PApplet.TOP);
        g.textSize(20);
        g.text("🔧 Hardware Test", 200, 20);

        // セパレーター
        g.stroke(100, 100, 100);
        g.line(20, 60, 380, 60);

        // スクロール可能な領域の設定
        int viewportY = HEADER_HEIGHT;
        int viewportHeight = 600 - HEADER_HEIGHT - FOOTER_HEIGHT;

        // クリッピング領域を設定（スクロール可能領域のみ）
        g.pushMatrix();
        g.translate(0, viewportY);

        // テスト内容を描画（スクロールオフセット適用）
        g.translate(0, -scrollOffset);
        g.textAlign(PApplet.LEFT, PApplet.TOP);
        g.textSize(12);

        int y = 10;

        // 1. モバイルデータ通信テスト
        y = drawTestSection(g, y, "1. Mobile Data Socket", () -> {
            MobileDataSocket socket = kernel.getMobileDataSocket();
            return String.format(
                "Available: %b\nSignal: %d/5\nService: %s\nConnected: %b",
                socket.isAvailable(),
                socket.getSignalStrength(),
                socket.getServiceName(),
                socket.isConnected()
            );
        });

        // 2. Bluetoothテスト
        y = drawTestSection(g, y, "2. Bluetooth Socket", () -> {
            BluetoothSocket socket = kernel.getBluetoothSocket();
            List<BluetoothSocket.BluetoothDevice> devices = socket.scanNearbyDevices();
            return String.format(
                "Available: %b\nNearby devices: %d\nConnected: %d",
                socket.isAvailable(),
                devices.size(),
                socket.getConnectedDevices().size()
            );
        });

        // 3. 位置情報テスト
        y = drawTestSection(g, y, "3. Location Socket", () -> {
            LocationSocket socket = kernel.getLocationSocket();
            LocationSocket.LocationData loc = socket.getLocation();
            return String.format(
                "Available: %b\nEnabled: %b\nPosition: (%.2f, %.2f, %.2f)\nAccuracy: %.2f",
                socket.isAvailable(),
                socket.isEnabled(),
                loc.x, loc.y, loc.z,
                loc.accuracy
            );
        });

        // 4. バッテリー情報テスト
        y = drawTestSection(g, y, "4. Battery Info", () -> {
            BatteryInfo info = kernel.getBatteryInfo();
            return String.format(
                "Level: %d%%\nHealth: %d%%\nCharging: %b",
                info.getBatteryLevel(),
                info.getBatteryHealth(),
                info.isCharging()
            );
        });

        // 5. カメラテスト
        y = drawTestSection(g, y, "5. Camera Socket", () -> {
            CameraSocket socket = kernel.getCameraSocket();
            return String.format(
                "Available: %b\nEnabled: %b\nFrame: %s",
                socket.isAvailable(),
                socket.isEnabled(),
                socket.getCurrentFrame() != null ? "Available" : "null"
            );
        });

        // 6. マイクテスト
        y = drawTestSection(g, y, "6. Microphone Socket", () -> {
            MicrophoneSocket socket = kernel.getMicrophoneSocket();
            return String.format(
                "Available: %b\nEnabled: %b\nData: %s",
                socket.isAvailable(),
                socket.isEnabled(),
                socket.getAudioData() != null ? "Available" : "null"
            );
        });

        // 7. スピーカーテスト
        y = drawTestSection(g, y, "7. Speaker Socket", () -> {
            SpeakerSocket socket = kernel.getSpeakerSocket();
            return String.format(
                "Available: %b\nVolume: %s (%d)",
                socket.isAvailable(),
                socket.getVolumeLevel().name(),
                socket.getVolumeLevel().getLevel()
            );
        });

        // 8. IC通信テスト
        y = drawTestSection(g, y, "8. IC Socket", () -> {
            ICSocket socket = kernel.getICSocket();
            ICSocket.ICData data = socket.pollICData();
            return String.format(
                "Available: %b\nEnabled: %b\nLast Data: %s",
                socket.isAvailable(),
                socket.isEnabled(),
                data.type.name()
            );
        });

        // 9. SIM情報テスト
        y = drawTestSection(g, y, "9. SIM Info", () -> {
            SIMInfo info = kernel.getSIMInfo();
            return String.format(
                "Available: %b\nInserted: %b\nOwner: %s\nUUID: %s",
                info.isAvailable(),
                info.isInserted(),
                info.getOwnerName(),
                info.getOwnerUUID()
            );
        });

        // コンテンツの高さを記録
        contentHeight = y;

        // クリッピング解除
        g.popMatrix();

        // スクロールバーを描画
        drawScrollbar(g, viewportY, viewportHeight);

        // 操作ガイド（固定フッター）
        g.fill(150, 150, 150);
        g.textAlign(PApplet.CENTER, PApplet.BOTTOM);
        g.textSize(10);
        g.text("Drag/Arrow keys to scroll | 'r' to refresh | 'q' to quit", 200, 590);
    }

    /**
     * テストセクションを描画するヘルパーメソッド。
     */
    private int drawTestSection(PGraphics g, int y, String title, TestDataProvider provider) {
        // タイトル
        g.fill(100, 200, 255);
        g.textSize(14);
        g.text(title, 30, y);
        y += 20;

        // テストデータ
        try {
            String data = provider.getData();
            g.fill(200, 200, 200);
            g.textSize(11);

            String[] lines = data.split("\n");
            for (String line : lines) {
                g.text("  " + line, 30, y);
                y += 15;
            }
        } catch (Exception e) {
            g.fill(255, 100, 100);
            g.textSize(11);
            g.text("  Error: " + e.getMessage(), 30, y);
            y += 15;
        }

        y += 10; // セクション間の余白
        return y;
    }

    /**
     * スクロールバーを描画する。
     */
    private void drawScrollbar(PGraphics g, int viewportY, int viewportHeight) {
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (maxScroll <= 0) {
            return; // スクロール不要
        }

        // スクロールバーの位置とサイズを計算
        float scrollbarHeight = (float) viewportHeight / contentHeight * viewportHeight;
        scrollbarHeight = Math.max(20, scrollbarHeight); // 最小高さ

        float scrollbarY = viewportY + ((float) scrollOffset / maxScroll) * (viewportHeight - scrollbarHeight);

        // スクロールバーを描画
        g.fill(100, 100, 100, 150);
        g.noStroke();
        g.rect(390, scrollbarY, 8, scrollbarHeight, 4);
    }

    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        isDragging = true;
        lastMouseY = mouseY;
    }

    @Override
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        if (isDragging) {
            int deltaY = mouseY - lastMouseY;
            lastMouseY = mouseY;

            // スクロールオフセットを更新
            scrollOffset -= deltaY;

            // スクロール範囲を制限
            int viewportHeight = 600 - HEADER_HEIGHT - FOOTER_HEIGHT;
            int maxScroll = Math.max(0, contentHeight - viewportHeight);
            scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
        }
    }

    @Override
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        isDragging = false;
    }

    @Override
    public void keyPressed(PGraphics g, char key, int keyCode) {
        if (key == 'r' || key == 'R') {
            // リフレッシュ（再描画）
        } else if (key == 'q' || key == 'Q') {
            // アプリを閉じる
            kernel.getScreenManager().popScreen();
        }

        // 矢印キーでスクロール
        int viewportHeight = 600 - HEADER_HEIGHT - FOOTER_HEIGHT;
        int maxScroll = Math.max(0, contentHeight - viewportHeight);

        if (keyCode == 38) { // UP arrow
            scrollOffset = Math.max(0, scrollOffset - SCROLL_SPEED);
        } else if (keyCode == 40) { // DOWN arrow
            scrollOffset = Math.min(maxScroll, scrollOffset + SCROLL_SPEED);
        } else if (keyCode == 33) { // PAGE UP
            scrollOffset = Math.max(0, scrollOffset - viewportHeight);
        } else if (keyCode == 34) { // PAGE DOWN
            scrollOffset = Math.min(maxScroll, scrollOffset + viewportHeight);
        } else if (keyCode == 36) { // HOME
            scrollOffset = 0;
        } else if (keyCode == 35) { // END
            scrollOffset = maxScroll;
        }
    }

    @Override
    public String getScreenTitle() {
        return "Hardware Test";
    }

    /**
     * テストデータプロバイダーインターフェース。
     */
    @FunctionalInterface
    private interface TestDataProvider {
        String getData();
    }
}
