package jp.moyashi.phoneos.core.service;

import jp.moyashi.phoneos.core.notification.INotification;
import jp.moyashi.phoneos.core.notification.SimpleNotification;
import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import jp.moyashi.phoneos.core.input.GestureListener;
import jp.moyashi.phoneos.core.Kernel;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PFont;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * 通知センターの状態と通知の管理を行うサービス。
 * 画面上からスライドイン/アウトするアニメーション付きで通知センターを表示し、
 * 通知の追加、削除、表示を担当する。
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class NotificationManager implements GestureListener {
    
    /** 通知のリスト（スレッドセーフ） */
    private final List<INotification> notifications;
    
    /** 現在の表示状態 */
    private boolean isVisible;
    
    /** アニメーション進行度（0.0 = 非表示, 1.0 = 完全表示） */
    private float animationProgress;
    
    /** アニメーションの目標進行度 */
    private float targetAnimationProgress;
    
    /** アニメーション速度 */
    private static final float ANIMATION_SPEED = 0.12f;
    
    /** 通知センターの高さ（画面の何割を占めるか） */
    private static final float NOTIFICATION_CENTER_HEIGHT_RATIO = 0.6f;
    
    /** 通知アイテムの高さ */
    private static final float NOTIFICATION_HEIGHT = 80;
    
    /** 通知間のマージン */
    private static final float NOTIFICATION_MARGIN = 8;
    
    /** 画面の幅（描画時に取得） */
    private float screenWidth = 400;
    
    /** 画面の高さ（描画時に取得） */
    private float screenHeight = 600;
    
    /** 背景のアルファ値 */
    private static final int BACKGROUND_ALPHA = 230;
    
    /** スクロールオフセット */
    private float scrollOffset = 0;
    
    /** 最大スクロール量 */
    private float maxScrollOffset = 0;
    
    /** Kernelの参照（動的優先度変更時の再ソート用） */
    private jp.moyashi.phoneos.core.Kernel kernel;

    /** パネルの幅 */
    private int panelWidth;

    /** パネルの高さ */
    private int panelHeight;

    /** 最大表示可能通知数 */
    private static final int maxVisibleNotifications = 5;
    
    /**
     * NotificationManagerを作成する。
     */
    public NotificationManager() {
        this.notifications = new CopyOnWriteArrayList<>();
        this.isVisible = false;
        this.animationProgress = 0.0f;
        this.targetAnimationProgress = 0.0f;
        
        System.out.println("NotificationManager: Notification center service initialized");
        
        // テスト用通知を追加
        addTestNotifications();
    }
    
    /**
     * テスト用の通知を追加する。
     */
    private void addTestNotifications() {
        addNotification("\u30b7\u30b9\u30c6\u30e0", "\u30b7\u30b9\u30c6\u30e0\u66f4\u65b0", "MochiMobileOS\u306e\u65b0\u3057\u3044\u30d0\u30fc\u30b8\u30e7\u30f3\u304c\u5229\u7528\u53ef\u80fd\u3067\u3059", 1);
        addNotification("\u8a2d\u5b9a", "Wi-Fi\u63a5\u7d9a", "\u30db\u30fc\u30e0\u30cd\u30c3\u30c8\u30ef\u30fc\u30af\u306b\u63a5\u7d9a\u3057\u307e\u3057\u305f", 0);
        addNotification("\u30e1\u30c3\u30bb\u30fc\u30b8", "\u65b0\u7740\u30e1\u30c3\u30bb\u30fc\u30b8", "\u53cb\u9054\u304b\u3089\u30e1\u30c3\u30bb\u30fc\u30b8\u304c\u5c4a\u304d\u307e\u3057\u305f", 2);
        addNotification("\u30a2\u30d7\u30ea\u30b9\u30c8\u30a2", "\u30a2\u30d7\u30ea\u66f4\u65b0", "3\u3064\u306e\u30a2\u30d7\u30ea\u304c\u66f4\u65b0\u3055\u308c\u307e\u3057\u305f", 0);
        // スクロールテスト用の追加通知
        addNotification("\u30e1\u30fc\u30eb", "\u65b0\u7740\u30e1\u30fc\u30eb", "\u4ed5\u4e8b\u306e\u30e1\u30fc\u30eb\u304c\u5c4a\u3044\u3066\u3044\u307e\u3059", 1);
        addNotification("\u30ab\u30ec\u30f3\u30c0\u30fc", "\u4f1a\u8b70\u306e\u30ea\u30de\u30a4\u30f3\u30c0\u30fc", "15\u5206\u5f8c\u306b\u4f1a\u8b70\u304c\u59cb\u307e\u308a\u307e\u3059", 2);
        addNotification("\u5929\u6c17", "\u96e8\u306e\u4e88\u5831", "\u4eca\u65e5\u306e\u5348\u5f8c\u304b\u3089\u96e8\u304c\u964d\u308a\u307e\u3059", 0);
        addNotification("\u30bb\u30ad\u30e5\u30ea\u30c6\u30a3", "\u30bb\u30ad\u30e5\u30ea\u30c6\u30a3\u66f4\u65b0", "\u30bb\u30ad\u30e5\u30ea\u30c6\u30a3\u30a2\u30c3\u30d7\u30c7\u30fc\u30c8\u304c\u5fc5\u8981\u3067\u3059", 2);
        addNotification("\u30d0\u30c3\u30c6\u30ea\u30fc", "\u30d0\u30c3\u30c6\u30ea\u30fc\u4f4e\u4e0b", "\u30d0\u30c3\u30c6\u30ea\u30fc\u304c20%\u4ee5\u4e0b\u3067\u3059", 1);
        addNotification("\u30b9\u30c8\u30ec\u30fc\u30b8", "\u30b9\u30c8\u30ec\u30fc\u30b8\u4e0d\u8db3", "\u30b9\u30c8\u30ec\u30fc\u30b8\u304c\u6e80\u5bb9\u306b\u8fd1\u3065\u3044\u3066\u3044\u307e\u3059", 1);
    }
    
    /**
     * 通知センターを表示する。
     */
    public void show() {
        if (!isVisible) {
            isVisible = true;
            targetAnimationProgress = 1.0f;
            setDynamicPriority(10000); // ロック画面(8000)より高い優先度でControlCenter(15000)より低い
            System.out.println("NotificationManager: Showing notification center with " + notifications.size() + " notifications");
        }
    }
    
    /**
     * 通知センターを非表示にする。
     */
    public void hide() {
        if (isVisible) {
            isVisible = false;
            targetAnimationProgress = 0.0f;
            scrollOffset = 0; // スクロール位置をリセット
            setDynamicPriority(900); // 元の優先度に戻す
            System.out.println("NotificationManager: Hiding notification center");
        }
    }
    
    /**
     * 表示状態を切り替える。
     */
    public void toggle() {
        if (isVisible) {
            hide();
        } else {
            show();
        }
    }
    
    /**
     * 新しい通知を追加する。
     * 
     * @param sender 送信者
     * @param title タイトル
     * @param content 内容
     * @param priority 優先度（0=低、1=通常、2=高）
     * @return 追加された通知のID
     */
    public String addNotification(String sender, String title, String content, int priority) {
        String id = "notification_" + System.currentTimeMillis() + "_" + notifications.size();
        SimpleNotification notification = new SimpleNotification(id, title, content, sender, priority);
        notification.setKernel(this.kernel); // Kernelの参照を設定

        // 優先度順でソート（高優先度が上に）
        notifications.add(notification);
        notifications.sort((n1, n2) -> Integer.compare(n2.getPriority(), n1.getPriority()));
        
        System.out.println("NotificationManager: Added notification '" + title + "' from " + sender);
        updateScrollLimits();
        return id;
    }
    
    /**
     * 通知を削除する。
     * 
     * @param notificationId 削除する通知のID
     * @return 削除に成功した場合true
     */
    public boolean removeNotification(String notificationId) {
        boolean removed = notifications.removeIf(notification -> {
            if (notification.getId().equals(notificationId)) {
                notification.dismiss();
                System.out.println("NotificationManager: Removed notification '" + notification.getTitle() + "'");
                return true;
            }
            return false;
        });
        
        if (removed) {
            updateScrollLimits();
        }
        
        return removed;
    }
    
    /**
     * 指定されたIDの通知を取得する。
     * 
     * @param notificationId 通知ID
     * @return 見つかった場合は通知、見つからない場合はnull
     */
    public INotification getNotification(String notificationId) {
        return notifications.stream()
                .filter(notification -> notification.getId().equals(notificationId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * すべての通知を削除する。
     */
    public void clearAllNotifications() {
        int count = notifications.size();
        notifications.forEach(INotification::dismiss);
        notifications.clear();
        scrollOffset = 0;
        updateScrollLimits();
        System.out.println("NotificationManager: Cleared " + count + " notifications");
    }
    
    /**
     * 読み済みの通知を削除する。
     */
    public void clearReadNotifications() {
        List<INotification> toRemove = notifications.stream()
                .filter(INotification::isRead)
                .collect(Collectors.toList());
        
        toRemove.forEach(INotification::dismiss);
        notifications.removeAll(toRemove);
        updateScrollLimits();
        
        System.out.println("NotificationManager: Cleared " + toRemove.size() + " read notifications");
    }
    
    /**
     * 未読通知の数を取得する。
     * 
     * @return 未読通知数
     */
    public int getUnreadCount() {
        return (int) notifications.stream().filter(n -> !n.isRead()).count();
    }
    
    /**
     * 総通知数を取得する。
     * 
     * @return 通知数
     */
    public int getNotificationCount() {
        return notifications.size();
    }
    
    /**
     * 最新の通知を指定した数だけ取得する。
     * ロック画面でのプレビュー表示に使用される。
     * 
     * @param count 取得する通知の最大数
     * @return 最新通知のリスト
     */
    public List<INotification> getRecentNotifications(int count) {
        if (notifications.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 最新の通知から順に取得
        int size = Math.min(count, notifications.size());
        List<INotification> recent = new ArrayList<>();
        
        for (int i = notifications.size() - 1; i >= notifications.size() - size; i--) {
            recent.add(notifications.get(i));
        }
        
        return recent;
    }
    
    /**
     * 通知センターが表示中かどうかを確認する。
     * 
     * @return 表示中の場合true
     */
    public boolean isVisible() {
        return isVisible;
    }
    
    /**
     * スクロール制限を更新する。
     */
    private void updateScrollLimits() {
        float contentHeight = notifications.size() * (NOTIFICATION_HEIGHT + NOTIFICATION_MARGIN) + NOTIFICATION_MARGIN;
        float availableHeight = screenHeight * NOTIFICATION_CENTER_HEIGHT_RATIO - 80; // ヘッダー分を引く
        maxScrollOffset = Math.max(0, contentHeight - availableHeight);
        
        // スクロール位置を制限内に調整
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
    }
    
    /**
     * 通知センターを描画する（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     *
     * @param g Processing描画コンテキスト
     */
    public void draw(PGraphics g) {
        // 画面サイズを更新（仮定値を使用）
        screenWidth = 400;
        screenHeight = 600;

        // アニメーション進行度を更新
        updateAnimation();

        // 完全に非表示の場合は描画をスキップ
        if (animationProgress <= 0.01f) {
            return;
        }

        // バックアップ設定
        int originalTextAlign = g.textAlign;
        float originalTextSize = g.textSize;

        // 通知センターパネルの寸法と位置を計算
        panelWidth = (int)screenWidth;
        panelHeight = (int)(screenHeight * NOTIFICATION_CENTER_HEIGHT_RATIO);

        // アニメーション進行度に基づいて位置を計算
        float animatedY = -panelHeight + (panelHeight * animationProgress);
        int panelY = (int)animatedY;

        // 背景スクラム（暗幕）で背面を落としてパネルを浮かせる（やや強め）
        g.fill(0, 0, 0, 130);
        g.noStroke();
        g.rect(0, 0, (int)screenWidth, (int)screenHeight);

        // 影（エレベーション）
        // TODO: 低電力モード(ui.performance.low_power)時は影を省略するなどの最適化を検討
        jp.moyashi.phoneos.core.ui.effects.Elevation.drawRectShadow(g, 0, panelY, panelWidth, panelHeight, 16, 3);

        // テーマに基づくパネル背景描画
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int panelBg = theme != null ? theme.colorSurface() : 0xFF282C34;
        int a = 230; // 透過でオーバーレイ感
        int r = (panelBg >> 16) & 0xFF;
        int gr = (panelBg >> 8) & 0xFF;
        int b = panelBg & 0xFF;
        g.fill(r, gr, b, a);
        g.noStroke();
        g.rect(0, panelY, panelWidth, panelHeight, 16);

        // ライトモード時のみ、面をややグレー寄りに見せる黒の薄いオーバーレイ
        if (theme == null || theme.getMode() == jp.moyashi.phoneos.core.ui.theme.ThemeEngine.Mode.LIGHT) {
            g.noStroke();
            g.fill(0, 0, 0, 26);
            g.rect(0, panelY, panelWidth, panelHeight, 16);
        }

        // タイトル領域
        int titleY = panelY + 15;
        int textCol = theme != null ? theme.colorOnSurface() : 0xFFFFFFFF;
        g.fill((textCol>>16)&0xFF, (textCol>>8)&0xFF, textCol&0xFF);
        g.textAlign(PApplet.CENTER, PApplet.TOP);
        g.textSize(18);
        g.text("通知", screenWidth / 2, titleY);

        // 通知リスト描画
        int startY = titleY + 40;
        int notificationHeight = 60;
        int margin = 10;
        int available = panelHeight - (startY - panelY) - 30; // 下部のハンドル等の余白を考慮
        if (available < 0) available = 0;

        if (notifications.isEmpty()) {
            int sec = theme != null ? theme.colorOnSurfaceSecondary() : 0xFFB4BAC3;
            g.fill((sec>>16)&0xFF, (sec>>8)&0xFF, sec&0xFF);
            g.textAlign(PApplet.CENTER, PApplet.CENTER);
            g.textSize(14);
            g.text("通知はありません", screenWidth / 2, startY + notificationHeight / 2);
        } else {
            // 収容可能数を計算（はみ出し防止）
            int capacity = notificationHeight > 0 ? Math.max(0, available / notificationHeight) : 0;
            int size = Math.min(Math.min(maxVisibleNotifications, capacity), notifications.size());

            // クリップ領域を設定
            try { g.clip(0, startY, panelWidth, Math.max(0, available)); } catch (Exception ignore) {}

            int currentY = startY;
            for (int i = notifications.size() - size; i < notifications.size(); i++) {
                if (i < 0) continue;
                INotification notification = notifications.get(i);
                try {
                    if (notification instanceof SimpleNotification) {
                        (notification).draw(g, margin, currentY, panelWidth - (margin * 2), notificationHeight - margin);
                    } else {
                        notification.draw(g, margin, currentY, panelWidth - (margin * 2), notificationHeight - margin);
                    }
                } catch (Exception e) {
                    System.err.println("NotificationManager: Error drawing notification: " + e.getMessage());
                }
                currentY += notificationHeight;
            }

            // クリップ解除
            try { g.noClip(); } catch (Exception ignore) {}
        }

        // ハンドル描画
        int handleWidth = 40;
        int handleHeight = 4;
        int handleX = (int)(screenWidth - handleWidth) / 2;
        int handleY = panelY + panelHeight - 15;

        int handle = theme != null ? theme.colorBorder() : 0xFFA0A5AF;
        g.fill((handle>>16)&0xFF, (handle>>8)&0xFF, handle&0xFF);
        g.rect(handleX, handleY, handleWidth, handleHeight, 2);

        // 設定復元
        g.textAlign(originalTextAlign, PApplet.BASELINE);
        g.textSize(originalTextSize);
    }

    /**
     * 通知センターを描画する（PApplet版）。
     * 互換性のために残存。段階的にPGraphics版に移行予定。
     *
     * @param p Processing描画コンテキスト
     */
    public void draw(PApplet p) {
        // 画面サイズを更新
        screenWidth = p.width;
        screenHeight = p.height;
        
        // アニメーション進行度を更新
        updateAnimation();
        
        // 完全に非表示の場合は描画をスキップ
        if (animationProgress <= 0.01f) {
            return;
        }
        
        p.pushMatrix();
        p.pushStyle();
        
        try {
            // 背景オーバーレイ描画
            drawBackgroundOverlay(p);
            
            // 通知センターパネル描画
            drawNotificationPanel(p);
            
        } finally {
            p.popStyle();
            p.popMatrix();
        }
        
        updateScrollLimits();
    }
    
    /**
     * 背景オーバーレイを描画する（画面全体を暗くする効果）。
     */
    private void drawBackgroundOverlay(PApplet p) {
        // 画面全体の暗幕をやや強めに（ライト時の白飛びを抑制）
        int alpha = (int) (130 * animationProgress);
        p.fill(0, 0, 0, alpha);
        p.noStroke();
        p.rect(0, 0, screenWidth, screenHeight);
    }
    
    /**
     * 通知センターパネルを描画する。
     */
    private void drawNotificationPanel(PApplet p) {
        float panelHeight = screenHeight * NOTIFICATION_CENTER_HEIGHT_RATIO;
        float panelY = -panelHeight + panelHeight * animationProgress;
        
        // パネル背景
        int backgroundAlpha = (int) (BACKGROUND_ALPHA * animationProgress);
        p.fill(35, 35, 40, backgroundAlpha);
        p.noStroke();
        p.rect(0, panelY, screenWidth, panelHeight, 0, 0, 20, 20);
        
        // パネル下部の取っ手
        drawHandle(p, panelY + panelHeight - 20);
        
        // ヘッダー描画
        drawHeader(p, panelY);
        
        // 通知描画
        drawNotifications(p, panelY + 60, panelHeight - 80);
    }
    
    /**
     * パネル下部の取っ手を描画する。
     */
    private void drawHandle(PApplet p, float y) {
        float handleWidth = 40;
        float handleHeight = 4;
        float handleX = (screenWidth - handleWidth) / 2;
        
        int handleAlpha = (int) (150 * animationProgress);
        p.fill(255, 255, 255, handleAlpha);
        p.noStroke();
        p.rect(handleX, y, handleWidth, handleHeight, handleHeight / 2);
    }
    
    /**
     * パネルヘッダーを描画する。
     */
    private void drawHeader(PApplet p, float panelY) {
        int textAlpha = (int) (255 * animationProgress);
        
        // 日本語フォントを設定
        if (kernel != null) {
            PFont japaneseFont = kernel.getJapaneseFont();
            if (japaneseFont != null) {
                p.textFont(japaneseFont);
            }
        }
        
        // タイトル
        p.fill(255, 255, 255, textAlpha);
        p.textAlign(PApplet.CENTER, PApplet.TOP);
        p.textSize(18);
        p.text("\u901a\u77e5\u30bb\u30f3\u30bf\u30fc", screenWidth / 2, panelY + 15);
        
        // 通知数表示
        p.fill(180, 180, 180, textAlpha);
        p.textSize(12);
        int unreadCount = getUnreadCount();
        String countText = notifications.size() + " \u4ef6\u306e\u901a\u77e5";
        if (unreadCount > 0) {
            countText += " (" + unreadCount + " \u4ef6\u672a\u8aad)";
        }
        p.text(countText, screenWidth / 2, panelY + 38);
        
        // クリアボタン（右上）
        if (notifications.size() > 0) {
            p.fill(100, 150, 200, textAlpha);
            p.textAlign(PApplet.RIGHT, PApplet.TOP);
            p.textSize(10);
            p.text("\u3059\u3079\u3066\u30af\u30ea\u30a2", screenWidth - 12, panelY + 15);
        }
    }
    
    /**
     * 通知を描画する。
     */
    private void drawNotifications(PApplet p, float startY, float availableHeight) {
        if (notifications.isEmpty()) {
            drawEmptyMessage(p, startY, availableHeight);
            return;
        }
        
        // クリッピング（スクロール領域に限定）
        try { p.clip((int)NOTIFICATION_MARGIN, (int)startY, (int)(screenWidth - 2*NOTIFICATION_MARGIN), (int)availableHeight); } catch (Exception ignore) {}
        
        float currentY = startY - scrollOffset + NOTIFICATION_MARGIN;
        
        for (INotification notification : notifications) {
            if (!notification.isDismissible()) {
                continue; // 削除された通知はスキップ
            }
            
            // 表示範囲外の通知はスキップ（パフォーマンス向上）
            if (currentY + NOTIFICATION_HEIGHT < startY || currentY > startY + availableHeight) {
                currentY += NOTIFICATION_HEIGHT + NOTIFICATION_MARGIN;
                continue;
            }
            
            float notificationWidth = screenWidth - 2 * NOTIFICATION_MARGIN;
            
            // 通知を描画
            try {
                notification.draw(p, NOTIFICATION_MARGIN, currentY, notificationWidth, NOTIFICATION_HEIGHT);
            } catch (Exception e) {
                System.err.println("NotificationManager: Error drawing notification '" + notification.getId() + "': " + e.getMessage());
            }
            
            currentY += NOTIFICATION_HEIGHT + NOTIFICATION_MARGIN;
        }
        
        try { p.noClip(); } catch (Exception ignore) {}
    }
    
    /**
     * 通知がない場合のメッセージを描画する。
     */
    private void drawEmptyMessage(PApplet p, float startY, float availableHeight) {
        // 日本語フォントを設定
        if (kernel != null) {
            PFont japaneseFont = kernel.getJapaneseFont();
            if (japaneseFont != null) {
                p.textFont(japaneseFont);
            }
        }
        
        p.fill(150, 150, 150, (int) (255 * animationProgress));
        p.textAlign(PApplet.CENTER, PApplet.CENTER);
        p.textSize(16);
        p.text("\u901a\u77e5\u306f\u3042\u308a\u307e\u305b\u3093", screenWidth / 2, startY + availableHeight / 2);
    }
    
    /**
     * アニメーション進行度を更新する。
     */
    private void updateAnimation() {
        // Reduce Motion対応: 進行を早める
        boolean reduce = false;
        if (kernel != null && kernel.getSettingsManager() != null) {
            reduce = kernel.getSettingsManager().getBooleanSetting("ui.motion.reduce", false);
        }
        float speed = reduce ? (ANIMATION_SPEED * 2.5f) : ANIMATION_SPEED;
        // TODO: 低電力モード(ui.performance.low_power)時にさらなる簡略化（速度上げ/アニメ省略）を検討
        if (Math.abs(animationProgress - targetAnimationProgress) > 0.01f) {
            animationProgress += (targetAnimationProgress - animationProgress) * speed;
        } else {
            animationProgress = targetAnimationProgress;
        }
    }
    
    /**
     * ジェスチャーイベントを処理する。
     * 
     * @param event ジェスチャーイベント
     * @return イベントを処理した場合true
     */
    public boolean onGesture(GestureEvent event) {
        // 非表示の場合は処理しない
        if (animationProgress <= 0.1f) {
            return false;
        }

        float panelHeight = screenHeight * NOTIFICATION_CENTER_HEIGHT_RATIO;
        float panelY = -panelHeight + panelHeight * animationProgress;

        // パネル内かどうかを判定
        boolean isInsidePanel = event.getCurrentY() >= panelY &&
                                event.getCurrentY() <= panelY + panelHeight;

        // パネル外をタップした場合は閉じる（TAP、LONG_PRESSのみ）
        if (!isInsidePanel) {
            if (event.getType() == GestureType.TAP ||
                event.getType() == GestureType.LONG_PRESS ||
                event.getType() == GestureType.DRAG_START) {
                System.out.println("NotificationManager: Tapped outside panel, hiding notification center");
                hide();
                return true;
            }
            // その他のジェスチャー（SWIPE、DRAG_MOVEなど）もパネル外であれば消費
            return true;
        }

        // 以下、パネル内の処理

        // ヘッダーエリアのクリック処理（パネル上端から60pxまで）
        if (event.getCurrentY() >= panelY && event.getCurrentY() <= panelY + 60) {
            // TAPまたはLONG_PRESSの場合のみボタン処理を実行
            if (event.getType() == GestureType.TAP || event.getType() == GestureType.LONG_PRESS) {
                handleHeaderClick(event, panelY);
            }
            // ヘッダーエリア内のすべてのジェスチャーを消費
            return true;
        }

        // 通知エリアの範囲を計算
        float notificationAreaStart = panelY + 60;
        float notificationAreaHeight = panelHeight - 80;
        boolean inNotificationArea = event.getCurrentY() >= notificationAreaStart &&
                                     event.getCurrentY() <= notificationAreaStart + notificationAreaHeight;

        // 通知エリアでのスクロール処理
        if (inNotificationArea && maxScrollOffset > 0) {
            if (event.getType() == GestureType.SWIPE_UP) {
                // 上向きスワイプで下にスクロール（通知を上に移動）
                float scrollAmount = 80.0f;
                scrollOffset = Math.min(maxScrollOffset, scrollOffset + scrollAmount);
                System.out.println("NotificationManager: Swipe up scroll, offset: " + scrollOffset);
                return true;
            } else if (event.getType() == GestureType.SWIPE_DOWN) {
                // 下向きスワイプで上にスクロール（通知を下に移動）
                float scrollAmount = 80.0f;
                scrollOffset = Math.max(0, scrollOffset - scrollAmount);
                System.out.println("NotificationManager: Swipe down scroll, offset: " + scrollOffset);
                return true;
            } else if (event.getType() == GestureType.DRAG_MOVE) {
                // ドラッグ中のスムーズなスクロール
                float deltaY = event.getCurrentY() - event.getStartY();
                scrollOffset = Math.max(0, Math.min(scrollOffset - deltaY * 1.2f, maxScrollOffset));
                System.out.println("NotificationManager: Drag scroll, offset: " + scrollOffset);
                return true;
            }
        }

        // 上向きスワイプで閉じる（通知エリア外、またはスクロール不要な場合）
        if (event.getType() == GestureType.SWIPE_UP &&
            (!inNotificationArea || maxScrollOffset <= 0)) {
            System.out.println("NotificationManager: Swipe up detected, hiding notification center");
            hide();
            return true;
        }

        // 通知のクリック処理（TAPまたはLONG_PRESSの場合）
        if (event.getType() == GestureType.TAP || event.getType() == GestureType.LONG_PRESS) {
            handleNotificationClick(event, panelY);
        }

        // パネル内のすべてのイベントを消費して、下のレイヤーへの貫通を防ぐ
        return true;
    }

    /**
     * ヘッダーエリアのクリックを処理する。
     */
    private boolean handleHeaderClick(GestureEvent event, float panelY) {
        // "すべてクリア" ボタンのクリック
        if (event.getCurrentX() >= screenWidth - 80 && event.getCurrentX() <= screenWidth - 12 &&
            event.getCurrentY() >= panelY + 10 && event.getCurrentY() <= panelY + 30) {
            clearAllNotifications();
            return true;
        }
        
        return false;
    }
    
    /**
     * 通知のクリックを処理する。
     */
    private boolean handleNotificationClick(GestureEvent event, float panelY) {
        float startY = panelY + 60;
        float currentY = startY - scrollOffset + NOTIFICATION_MARGIN;
        
        for (INotification notification : notifications) {
            if (!notification.isDismissible()) {
                continue;
            }
            
            float notificationWidth = screenWidth - 2 * NOTIFICATION_MARGIN;
            
            // 通知の範囲内かチェック
            if (event.getCurrentX() >= NOTIFICATION_MARGIN && 
                event.getCurrentX() <= NOTIFICATION_MARGIN + notificationWidth &&
                event.getCurrentY() >= currentY && 
                event.getCurrentY() <= currentY + NOTIFICATION_HEIGHT) {
                
                // 削除ボタンのクリックチェック
                if (notification instanceof SimpleNotification) {
                    SimpleNotification simpleNotif = (SimpleNotification) notification;
                    if (simpleNotif.isDismissButtonClicked(event.getCurrentX(), event.getCurrentY(), 
                            NOTIFICATION_MARGIN, currentY, notificationWidth)) {
                        removeNotification(notification.getId());
                        return true;
                    }
                }
                
                // 通知のクリック処理
                if (notification.onClick(event.getCurrentX(), event.getCurrentY())) {
                    System.out.println("NotificationManager: Clicked notification: " + notification.getTitle());
                    return true;
                }
            }
            
            currentY += NOTIFICATION_HEIGHT + NOTIFICATION_MARGIN;
        }
        
        return false;
    }
    
    /**
     * 現在のアニメーション進行度を取得する（デバッグ用）。
     * 
     * @return アニメーション進行度（0.0-1.0）
     */
    public float getAnimationProgress() {
        return animationProgress;
    }
    
    /** 動的優先度（表示状態に応じて変更される） */
    private int dynamicPriority = 900;
    
    // GestureListener interface implementation
    
    /**
     * ジェスチャーの優先度を返す。
     * 通知センターは動的優先度システムを使用する。
     * 
     * @return 動的優先度
     */
    @Override
    public int getPriority() {
        return dynamicPriority;
    }
    
    /**
     * 動的優先度を設定する（表示レイヤー順に応じて更新）。
     * 
     * @param priority 新しい優先度
     */
    public void setDynamicPriority(int priority) {
        if (this.dynamicPriority != priority) {
            this.dynamicPriority = priority;
            System.out.println("NotificationManager: Dynamic priority updated to " + priority);
            
            // GestureManagerに再ソートを要求
            if (kernel != null && kernel.getGestureManager() != null) {
                kernel.getGestureManager().resortListeners();
            }
        }
    }
    
    /**
     * Kernelの参照を設定する。
     * 
     * @param kernel Kernelインスタンス
     */
    public void setKernel(jp.moyashi.phoneos.core.Kernel kernel) {
        this.kernel = kernel;
    }
    
    /**
     * 指定された座標が通知センターの範囲内かどうかを確認する。
     *
     * @param x X座標
     * @param y Y座標
     * @return 通知センターが表示中で範囲内の場合true
     */
    @Override
    public boolean isInBounds(int x, int y) {
        // 非表示の場合は範囲外
        if (!this.isVisible || animationProgress <= 0.1f) {
            return false;
        }

        // パネルの位置と高さを計算
        float panelHeight = screenHeight * NOTIFICATION_CENTER_HEIGHT_RATIO;
        float panelY = -panelHeight + panelHeight * animationProgress;

        // 通知センターが表示中の場合、画面全体をカバー（背景の暗幕を含む）
        // パネル外をタップすると閉じるため、画面全体でイベントをキャプチャする必要がある
        return true;
    }
}
