package jp.moyashi.phoneos.core.ui.lock;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.input.GestureListener;
import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import jp.moyashi.phoneos.core.service.LockManager;
import jp.moyashi.phoneos.core.service.SystemClock;
import jp.moyashi.phoneos.core.service.NotificationManager;
import jp.moyashi.phoneos.core.notification.INotification;
import jp.moyashi.phoneos.core.ui.LayerManager;
import jp.moyashi.phoneos.core.ui.UILayer;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * OSのロック画面を表示するUIクラス。
 * パターン認証、時刻表示、通知プレビューなどの機能を提供する。
 * 
 * このクラスの主要機能:
 * - 現在時刻の大きな表示
 * - 最新通知のプレビュー表示
 * - 3x3パターン入力グリッドの描画とインタラクション
 * - パターン認証処理
 * - 認証成功時のホーム画面遷移
 * - 認証失敗時のフィードバック表示
 * 
 * @author YourName  
 * @version 1.0
 */
public class LockScreen implements Screen, GestureListener {
    
    /** OSカーネルへの参照 */
    private final Kernel kernel;
    
    /** ロック管理サービスへの参照 */
    private final LockManager lockManager;
    
    /** システムクロックサービスへの参照 */
    private final SystemClock systemClock;
    
    /** 通知管理サービスへの参照 */
    private final NotificationManager notificationManager;
    
    /** レイヤー管理システムへの参照 */
    private final LayerManager layerManager;
    
    /** このロック画面のレイヤーID */
    private static final String LAYER_ID = "lock_screen";
    
    /** レイヤー登録が完了しているかどうか */
    private boolean layerRegistered;
    
    /** パターングリッドの大きさ（3x3） */
    private static final int GRID_SIZE = 3;
    
    /** パターングリッドの中心座標 */
    private int gridCenterX, gridCenterY;
    
    /** パターンドットの半径 */
    private static final int DOT_RADIUS = 20;
    
    /** パターンドット間の間隔 */
    private static final int DOT_SPACING = 90;
    
    /** ドット検出の拡張範囲（ドットの周りの検出エリア） */
    private static final int DOT_DETECTION_RADIUS = 35;
    
    /** 現在入力中のパターン */
    private List<Integer> currentPattern;
    
    /** ドラッグ中の軌跡点リスト */
    private List<int[]> dragPath;
    
    /** 現在ドラッグ中かどうか */
    private boolean isDragging;
    
    /** 認証結果のフィードバック状態 */
    private AuthFeedback authFeedback;
    
    /** フィードバック表示タイマー */
    private long feedbackStartTime;
    
    /** フィードバック表示時間（ミリ秒） */
    private static final long FEEDBACK_DURATION = 1500;
    
    /** パターンエリアハイライト状態 */
    private boolean patternHighlighted;
    
    /** ハイライト開始時間 */
    private long highlightStartTime;
    
    /** ハイライト表示時間（ミリ秒） */
    private static final long HIGHLIGHT_DURATION = 2000;
    
    /** パターン入力画面が表示中かどうか */
    private boolean patternInputVisible;
    
    /** パターン入力スライドのアニメーション進行度 (0.0 = 完全に隠れている, 1.0 = 完全に表示) */
    private float patternSlideProgress;
    
    /** パターン入力アニメーション中かどうか */
    private boolean patternAnimating;
    
    /** パターン入力アニメーション開始時間 */
    private long patternAnimationStartTime;
    
    /** パターン入力スライドアニメーション時間（ミリ秒） */
    private static final long PATTERN_SLIDE_DURATION = 300;
    
    /** 通知リストのスクロールオフセット */
    private float notificationScrollOffset = 0.0f;
    
    /** 通知スクロール中かどうか */
    private boolean isScrollingNotifications = false;
    
    /** 最後のスクロールY座標 */
    private int lastScrollY = 0;
    
    /** 通知の最大表示数（スクロールなし）*/
    private static final int MAX_VISIBLE_NOTIFICATIONS = 3;
    
    // スムーズスクロール用変数（コントロールセンターと同じ実装）
    /** スクロールの慣性速度 */
    private float notificationScrollVelocity = 0.0f;
    
    /** 摩擦係数（スクロールの減速） */
    private static final float NOTIFICATION_SCROLL_FRICTION = 0.85f;
    
    /** スクロール感度 */
    private static final float NOTIFICATION_SCROLL_SENSITIVITY = 1.0f;
    
    /** 最小スクロール速度（これより小さければ停止） */
    private static final float MIN_NOTIFICATION_SCROLL_VELOCITY = 0.1f;
    
    /** 前回のドラッグY座標（スクロール計算用） */
    private float lastNotificationDragY = 0;
    
    /** 通知エリアでのドラッグが開始されているかどうか */
    private boolean isNotificationDragScrolling = false;


    /**
     * 認証結果のフィードバック状態を表す列挙型
     */
    private enum AuthFeedback {
        NONE,      // フィードバックなし
        SUCCESS,   // 認証成功
        FAILED     // 認証失敗
    }
    
    /**
     * LockScreenの新しいインスタンスを作成する。
     * 
     * @param kernel OSカーネルのインスタンス
     */
    public LockScreen(Kernel kernel) {
        this.kernel = kernel;
        this.lockManager = kernel.getLockManager();
        this.systemClock = kernel.getSystemClock();
        this.notificationManager = kernel.getNotificationManager();
        this.layerManager = kernel.getLayerManager();
        
        this.currentPattern = new ArrayList<>();
        this.dragPath = new ArrayList<>();
        this.isDragging = false;
        this.authFeedback = AuthFeedback.NONE;
        this.layerRegistered = false;
        this.patternHighlighted = false;
        this.patternInputVisible = false;
        this.patternSlideProgress = 0.0f;
        this.patternAnimating = false;
        
        System.out.println("LockScreen: ロック画面を初期化しました");
    }
    
    /**
     * ロック画面のセットアップを行う (PGraphics version)。
     * パターングリッドの位置を計算し、レイヤーシステムに登録する。
     */
    @Override
    public void setup(PGraphics g) {
        System.out.println("LockScreen: ロック画面のセットアップ中...");

        // パターングリッドの中心位置を計算（画面中央下部）
        gridCenterX = 400 / 2; // 画面幅の中央
        gridCenterY = 600 - 200; // 画面下部から200px上

        // レイヤー管理システムに登録
        registerWithLayerManager();

        // ジェスチャーマネージャーにこのスクリーンを登録
        kernel.getGestureManager().addGestureListener(this);

        System.out.println("LockScreen: セットアップ完了");
    }

    /**
     * @deprecated Use {@link #setup(PGraphics)} instead
     */
    @Deprecated
    @Override
    public void setup(processing.core.PApplet p) {
        PGraphics g = p.g;
        setup(g);
    }
    
    /**
     * ロック画面のコンテンツを描画する。
     * 時刻、通知、パターングリッド、ドラッグ軌跡、フィードバックを描画する。
     * 
     * @param p 描画用のPAppletインスタンス
     */
    @Override
    public void draw(PApplet p) {
        // アニメーション状態を更新
        updatePatternSlideAnimation();
        
        // 通知スクロールの慣性更新
        updateNotificationScrollInertia();
        
        // 背景を暗いグラデーション色に設定
        p.background(20, 25, 35);
        
        // 現在時刻を大きく表示
        drawCurrentTime(p);
        
        // 通知プレビューを表示（パターン入力画面と通知センターが表示されていない時のみ）
        if (!patternInputVisible && patternSlideProgress < 0.1f && 
            (notificationManager == null || !notificationManager.isVisible())) {
            drawNotificationPreviews(p);
        }
        
        // パターン入力画面を描画（アニメーション中または表示中）
        if (patternInputVisible || patternSlideProgress > 0.0f) {
            drawPatternInputScreen(p);
        }
        
        // ホームボタンヒントを描画（パターン入力が非表示の時のみ）
        if (!patternInputVisible && patternSlideProgress < 0.1f) {
            drawHomeButtonHint(p);
        }


        // デバッグ情報（開発時のみ）
        drawDebugInfo(p);
    }

    /**
     * PGraphics統一アーキテクチャ用のdraw()メソッド。
     * PApplet版のdraw()と同じ内容をPGraphicsで描画する。
     *
     * @param g 描画用のPGraphicsインスタンス
     */
    @Override
    public void draw(processing.core.PGraphics g) {
        // アニメーション状態を更新
        updatePatternSlideAnimation();

        // 通知スクロールの慣性更新
        updateNotificationScrollInertia();

        // 背景を暗いグラデーション色に設定
        g.background(20, 25, 35);

        // 現在時刻を大きく表示
        drawCurrentTime(g);

        // 通知プレビューを表示（パターン入力画面と通知センターが表示されていない時のみ）
        if (!patternInputVisible && patternSlideProgress < 0.1f &&
            (notificationManager == null || !notificationManager.isVisible())) {
            drawNotificationPreviews(g);
        }

        // パターン入力画面を描画（アニメーション中または表示中）
        if (patternInputVisible || patternSlideProgress > 0.0f) {
            drawPatternInputScreen(g);
        }

        // ホームボタンヒントを描画（パターン入力が非表示の時のみ）
        if (!patternInputVisible && patternSlideProgress < 0.1f) {
            drawHomeButtonHint(g);
        }


        // デバッグ情報（開発時のみ）
        drawDebugInfo(g);
    }

    /**
     * 現在時刻を画面上部に大きく表示する。
     * 
     * @param p 描画用のPAppletインスタンス
     */
    private void drawCurrentTime(PApplet p) {
        if (systemClock == null) return;
        
        LocalDateTime now = systemClock.getCurrentTime();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M月d日 (E)");
        
        String timeStr = now.format(timeFormatter);
        String dateStr = now.format(dateFormatter);
        
        // 時刻を大きく表示
        p.fill(255, 255, 255);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(72);
        p.text(timeStr, p.width / 2, 120);
        
        // 日付を中程度のサイズで表示
        p.textSize(24);
        p.fill(200, 200, 200);
        p.text(dateStr, p.width / 2, 170);
    }

    /**
     * 現在時刻を画面上部に大きく表示する（PGraphics版）。
     *
     * @param g 描画用のPGraphicsインスタンス
     */
    private void drawCurrentTime(processing.core.PGraphics g) {
        if (systemClock == null) return;

        LocalDateTime now = systemClock.getCurrentTime();
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("M月d日 (E)");

        String timeStr = now.format(timeFormatter);
        String dateStr = now.format(dateFormatter);

        // 日本語フォントを設定
        if (kernel != null && kernel.getJapaneseFont() != null) {
            g.textFont(kernel.getJapaneseFont());
        }

        // 時刻を大きく表示
        g.fill(255, 255, 255);
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(72);
        g.text(timeStr, g.width / 2, 120);

        // 日付を中程度のサイズで表示
        g.textSize(24);
        g.fill(200, 200, 200);
        g.text(dateStr, g.width / 2, 170);
    }

    /**
     * 最新の通知をiOSライクにスクロール可能で表示する。
     * 時刻の下、画面中央に配置し、スクロールで追加の通知を表示。
     * 
     * @param p 描画用のPAppletインスタンス
     */
    private void drawNotificationPreviews(PApplet p) {
        if (notificationManager == null) return;
        
        List<INotification> allNotifications = notificationManager.getRecentNotifications(10); // より多くの通知を取得
        
        if (allNotifications.isEmpty()) {
            drawNoNotificationsMessage(p);
            return;
        }
        
        // 通知表示エリアの設定 - iOSライクに時計の下、画面中央部
        int notificationAreaTop = 200; // 時計の下
        int notificationAreaBottom = p.height - 120; // ホームボタンヒントの上
        int notificationAreaHeight = notificationAreaBottom - notificationAreaTop;
        
        int notificationHeight = 60; // より大きなiOSライクな高さ
        int spacing = 10;
        int totalItemHeight = notificationHeight + spacing;
        
        // スクロール範囲の計算
        int totalContentHeight = allNotifications.size() * totalItemHeight;
        int maxScrollOffset = Math.max(0, totalContentHeight - notificationAreaHeight);
        
        // スクロールオフセットの制限
        notificationScrollOffset = Math.max(0, Math.min(notificationScrollOffset, maxScrollOffset));
        
        // クリッピング領域を設定して通知エリア外の描画を防ぐ
        p.pushMatrix();
        p.pushStyle();
        
        // 通知エリアでのクリッピングを設定
        p.clip(0, notificationAreaTop, p.width, notificationAreaHeight);
        
        for (int i = 0; i < allNotifications.size(); i++) {
            INotification notification = allNotifications.get(i);
            
            // スクロールオフセットを適用した Y 座標
            float y = notificationAreaTop + (i * totalItemHeight) - notificationScrollOffset;
            
            // パフォーマンス最適化：完全に範囲外の通知はスキップ
            if (y + notificationHeight < notificationAreaTop - 100 || y > notificationAreaBottom + 100) {
                continue; // 大幅に範囲外の通知はスキップ
            }
            
            // クリッピングが設定されているので、はみ出し部分は自動的に切り取られる
            drawNotificationCard(p, notification, 20, (int)y, p.width - 40, notificationHeight);
        }
        
        // クリッピングを解除
        p.noClip();
        p.popStyle();
        p.popMatrix();
        
        // スクロール可能であることを示すインジケーター
        if (maxScrollOffset > 0) {
            drawScrollIndicator(p, notificationAreaTop, notificationAreaHeight, notificationScrollOffset, maxScrollOffset);
        }
        
        // 上下端でのフェード効果
        drawNotificationAreaFade(p, notificationAreaTop, notificationAreaBottom);
    }

    /**
     * 最新の通知をiOSライクにスクロール可能で表示する（PGraphics版）。
     * 時刻の下、画面中央に配置し、スクロールで追加の通知を表示。
     *
     * @param g 描画用のPGraphicsインスタンス
     */
    private void drawNotificationPreviews(processing.core.PGraphics g) {
        if (notificationManager == null) return;

        List<INotification> allNotifications = notificationManager.getRecentNotifications(10); // より多くの通知を取得

        if (allNotifications.isEmpty()) {
            drawNoNotificationsMessage(g);
            return;
        }

        // 通知表示エリアの設定 - iOSライクに時計の下、画面中央部
        int notificationAreaTop = 200; // 時計の下
        int notificationAreaBottom = g.height - 120; // ホームボタンヒントの上
        int notificationAreaHeight = notificationAreaBottom - notificationAreaTop;

        int notificationHeight = 60; // より大きなiOSライクな高さ
        int spacing = 10;
        int totalItemHeight = notificationHeight + spacing;

        // スクロール範囲の計算
        int totalContentHeight = allNotifications.size() * totalItemHeight;
        int maxScrollOffset = Math.max(0, totalContentHeight - notificationAreaHeight);

        // スクロールオフセットの制限
        notificationScrollOffset = Math.max(0, Math.min(notificationScrollOffset, maxScrollOffset));

        // クリッピング領域を設定して通知エリア外の描画を防ぐ
        g.pushMatrix();
        g.pushStyle();

        // 通知エリアでのクリッピングを設定
        g.clip(0, notificationAreaTop, g.width, notificationAreaHeight);

        for (int i = 0; i < allNotifications.size(); i++) {
            INotification notification = allNotifications.get(i);

            // スクロールオフセットを適用した Y 座標
            float y = notificationAreaTop + (i * totalItemHeight) - notificationScrollOffset;

            // パフォーマンス最適化：完全に範囲外の通知はスキップ
            if (y + notificationHeight < notificationAreaTop - 100 || y > notificationAreaBottom + 100) {
                continue; // 大幅に範囲外の通知はスキップ
            }

            // クリッピングが設定されているので、はみ出し部分は自動的に切り取られる
            drawNotificationCard(g, notification, 20, (int)y, g.width - 40, notificationHeight);
        }

        // クリッピングを解除
        g.noClip();
        g.popStyle();
        g.popMatrix();

        // スクロール可能であることを示すインジケーター
        if (maxScrollOffset > 0) {
            drawScrollIndicator(g, notificationAreaTop, notificationAreaHeight, notificationScrollOffset, maxScrollOffset);
        }

        // 上下端でのフェード効果
        drawNotificationAreaFade(g, notificationAreaTop, notificationAreaBottom);
    }
    
    /**
     * 通知がない場合のメッセージを表示する。
     */
    private void drawNoNotificationsMessage(PApplet p) {
        p.fill(120, 120, 120);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(16);
        p.text("通知はありません", p.width / 2, 280);
    }

    /**
     * 通知がない場合のメッセージを表示する（PGraphics版）。
     */
    private void drawNoNotificationsMessage(processing.core.PGraphics g) {
        // 日本語フォントを設定
        if (kernel != null && kernel.getJapaneseFont() != null) {
            g.textFont(kernel.getJapaneseFont());
        }

        g.fill(120, 120, 120);
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(16);
        g.text("通知はありません", g.width / 2, 280);
    }
    
    /**
     * 個別の通知カードを描画する（iOSライク）。
     */
    private void drawNotificationCard(PApplet p, INotification notification, int x, int y, int width, int height) {
        // 通知カードの背景（iOSライクな丸角とぼかし効果）
        p.fill(45, 50, 60, 200);
        p.stroke(70, 75, 85, 150);
        p.strokeWeight(1);
        p.rect(x, y, width, height, 12); // 丸角
        
        // アプリアイコンエリア（模擬）
        p.fill(80, 140, 200);
        p.rect(x + 10, y + 10, 30, 30, 6);
        
        // 通知タイトル
        p.fill(255, 255, 255);
        p.textAlign(p.LEFT, p.TOP);
        p.textSize(14);
        p.text(notification.getTitle(), x + 50, y + 10);
        
        // 通知内容
        p.fill(200, 200, 200);
        p.textSize(12);
        p.text(notification.getContent(), x + 50, y + 30);
        
        // 時刻表示（右上）
        p.fill(150, 150, 150);
        p.textAlign(p.RIGHT, p.TOP);
        p.textSize(10);
        p.text("今", x + width - 10, y + 10);
    }

    /**
     * 個別の通知カードを描画する（iOSライク）（PGraphics版）。
     */
    private void drawNotificationCard(processing.core.PGraphics g, INotification notification, int x, int y, int width, int height) {
        // 通知カードの背景（iOSライクな丸角とぼかし効果）
        g.fill(45, 50, 60, 200);
        g.stroke(70, 75, 85, 150);
        g.strokeWeight(1);
        g.rect(x, y, width, height, 12); // 丸角

        // アプリアイコンエリア（模擬）
        g.fill(80, 140, 200);
        g.rect(x + 10, y + 10, 30, 30, 6);

        // 日本語フォントを設定
        if (kernel != null && kernel.getJapaneseFont() != null) {
            g.textFont(kernel.getJapaneseFont());
        }

        // 通知タイトル
        g.fill(255, 255, 255);
        g.textAlign(PApplet.LEFT, PApplet.TOP);
        g.textSize(14);
        g.text(notification.getTitle(), x + 50, y + 10);

        // 通知内容
        g.fill(200, 200, 200);
        g.textSize(12);
        g.text(notification.getContent(), x + 50, y + 30);

        // 時刻表示（右上）
        g.fill(150, 150, 150);
        g.textAlign(PApplet.RIGHT, PApplet.TOP);
        g.textSize(10);
        g.text("今", x + width - 10, y + 10);
    }
    
    /**
     * スクロールインジケーターを描画する。
     */
    private void drawScrollIndicator(PApplet p, int areaTop, int areaHeight, float scrollOffset, int maxScroll) {
        if (maxScroll <= 0) return;
        
        // スクロールバーの設定
        int barX = p.width - 8;
        int barWidth = 3;
        int barTop = areaTop + 10;
        int barBottom = areaTop + areaHeight - 10;
        int barHeight = barBottom - barTop;
        
        // スクロール位置の割合
        float scrollRatio = scrollOffset / maxScroll;
        float indicatorHeight = Math.max(10, barHeight * 0.3f);
        float indicatorY = barTop + (barHeight - indicatorHeight) * scrollRatio;
        
        // スクロールバー背景
        p.fill(50, 50, 50, 100);
        p.rect(barX, barTop, barWidth, barHeight, barWidth/2);
        
        // スクロールインジケーター
        p.fill(150, 150, 150, 150);
        p.rect(barX, indicatorY, barWidth, indicatorHeight, barWidth/2);
    }

    /**
     * スクロールインジケーターを描画する（PGraphics版）。
     */
    private void drawScrollIndicator(processing.core.PGraphics g, int areaTop, int areaHeight, float scrollOffset, int maxScroll) {
        if (maxScroll <= 0) return;

        // スクロールバーの設定
        int barX = g.width - 8;
        int barWidth = 3;
        int barTop = areaTop + 10;
        int barBottom = areaTop + areaHeight - 10;
        int barHeight = barBottom - barTop;

        // スクロール位置の割合
        float scrollRatio = scrollOffset / maxScroll;
        float indicatorHeight = Math.max(10, barHeight * 0.3f);
        float indicatorY = barTop + (barHeight - indicatorHeight) * scrollRatio;

        // スクロールバー背景
        g.fill(50, 50, 50, 100);
        g.rect(barX, barTop, barWidth, barHeight, barWidth/2);

        // スクロールインジケーター
        g.fill(150, 150, 150, 150);
        g.rect(barX, indicatorY, barWidth, indicatorHeight, barWidth/2);
    }
    
    /**
     * 通知エリアの上下端にフェード効果を適用する。
     */
    private void drawNotificationAreaFade(PApplet p, int areaTop, int areaBottom) {
        int fadeHeight = 15;
        
        // 上端のフェード
        for (int i = 0; i < fadeHeight; i++) {
            float alpha = (float)i / fadeHeight;
            p.stroke(20, 25, 35, (int)(255 * (1 - alpha)));
            p.line(0, areaTop + i, p.width, areaTop + i);
        }
        
        // 下端のフェード
        for (int i = 0; i < fadeHeight; i++) {
            float alpha = (float)i / fadeHeight;
            p.stroke(20, 25, 35, (int)(255 * (1 - alpha)));
            p.line(0, areaBottom - i, p.width, areaBottom - i);
        }
    }

    /**
     * 通知エリアの上下端にフェード効果を適用する（PGraphics版）。
     */
    private void drawNotificationAreaFade(processing.core.PGraphics g, int areaTop, int areaBottom) {
        int fadeHeight = 15;

        // 上端のフェード
        for (int i = 0; i < fadeHeight; i++) {
            float alpha = (float)i / fadeHeight;
            g.stroke(20, 25, 35, (int)(255 * (1 - alpha)));
            g.line(0, areaTop + i, g.width, areaTop + i);
        }

        // 下端のフェード
        for (int i = 0; i < fadeHeight; i++) {
            float alpha = (float)i / fadeHeight;
            g.stroke(20, 25, 35, (int)(255 * (1 - alpha)));
            g.line(0, areaBottom - i, g.width, areaBottom - i);
        }
    }
    
    /**
     * 3x3のパターン入力グリッドを描画する。
     * 
     * @param p 描画用のPAppletインスタンス
     */
    private void drawPatternGrid(PApplet p) {
        // ハイライトの状態をチェック
        boolean showHighlight = checkHighlightState();
        
        // ハイライト背景を描画
        if (showHighlight) {
            drawHighlightBackground(p);
        }
        
        // グリッドの各ドットを描画
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int dotIndex = row * GRID_SIZE + col;
                int[] dotPos = getDotPosition(dotIndex);
                
                // ドットが現在のパターンに含まれているかチェック
                boolean isSelected = currentPattern.contains(dotIndex);
                
                // 選択状態に応じて色を変更
                if (isSelected) {
                    p.fill(100, 180, 255); // 青色（選択済み）
                    p.stroke(150, 200, 255);
                    p.strokeWeight(4);
                } else {
                    p.fill(60, 65, 75); // 濃いグレー（未選択）
                    p.stroke(100, 110, 120);
                    p.strokeWeight(2);
                }
                
                // ドットを描画
                p.ellipse(dotPos[0], dotPos[1], DOT_RADIUS * 2, DOT_RADIUS * 2);
                
                // 中央の小さなドット
                p.fill(200, 200, 200);
                p.noStroke();
                p.ellipse(dotPos[0], dotPos[1], 6, 6);
            }
        }
        
        // 選択されたドット間に線を描画
        if (currentPattern.size() > 1) {
            p.stroke(100, 180, 255);
            p.strokeWeight(4);
            
            for (int i = 0; i < currentPattern.size() - 1; i++) {
                int[] start = getDotPosition(currentPattern.get(i));
                int[] end = getDotPosition(currentPattern.get(i + 1));
                p.line(start[0], start[1], end[0], end[1]);
            }
        }
    }
    
    /**
     * ドラッグ中の軌跡を描画する。
     * 
     * @param p 描画用のPAppletインスタンス
     */
    private void drawDragPath(PApplet p) {
        if (dragPath.size() < 2) return;
        
        p.stroke(100, 180, 255, 100);
        p.strokeWeight(3);
        p.noFill();
        
        for (int i = 0; i < dragPath.size() - 1; i++) {
            int[] start = dragPath.get(i);
            int[] end = dragPath.get(i + 1);
            p.line(start[0], start[1], end[0], end[1]);
        }
    }

    /**
     * ドラッグ中の軌跡を描画する（PGraphics版）。
     *
     * @param g 描画用のPGraphicsインスタンス
     */
    private void drawDragPath(processing.core.PGraphics g) {
        if (dragPath.size() < 2) return;

        g.stroke(100, 180, 255, 100);
        g.strokeWeight(3);
        g.noFill();

        for (int i = 0; i < dragPath.size() - 1; i++) {
            int[] start = dragPath.get(i);
            int[] end = dragPath.get(i + 1);
            g.line(start[0], start[1], end[0], end[1]);
        }
    }

    /**
     * 認証成功/失敗のフィードバックを描画する。
     *
     * @param p 描画用のPAppletインスタンス
     */
    private void drawAuthFeedback(PApplet p) {
        if (authFeedback == AuthFeedback.NONE) return;
        
        long elapsed = System.currentTimeMillis() - feedbackStartTime;
        if (elapsed > FEEDBACK_DURATION) {
            authFeedback = AuthFeedback.NONE;
            return;
        }
        
        // フェードアウト効果
        float alpha = 1.0f - (float) elapsed / FEEDBACK_DURATION;
        
        String message;
        int color;
        
        if (authFeedback == AuthFeedback.SUCCESS) {
            message = "認証成功！";
            color = p.color(50, 255, 50, (int)(alpha * 255));
        } else {
            message = "パターンが正しくありません";
            color = p.color(255, 50, 50, (int)(alpha * 255));
        }
        
        // 背景ボックス
        p.fill(color);
        p.rect(50, 500, p.width - 100, 50, 10);
        
        // テキスト
        p.fill(255, 255, 255, (int)(alpha * 255));
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(18);
        p.text(message, p.width / 2, 525);
    }

    /**
     * 認証成功/失敗のフィードバックを描画する（PGraphics版）。
     *
     * @param g 描画用のPGraphicsインスタンス
     */
    private void drawAuthFeedback(processing.core.PGraphics g) {
        if (authFeedback == AuthFeedback.NONE) return;

        long elapsed = System.currentTimeMillis() - feedbackStartTime;
        if (elapsed > FEEDBACK_DURATION) {
            authFeedback = AuthFeedback.NONE;
            return;
        }

        // フェードアウト効果
        float alpha = 1.0f - (float) elapsed / FEEDBACK_DURATION;

        String message;
        int color;

        if (authFeedback == AuthFeedback.SUCCESS) {
            message = "認証成功！";
            color = (50 << 16) | (255 << 8) | 50 | ((int)(alpha * 255) << 24); // RGB(50,255,50) with alpha
        } else {
            message = "パターンが正しくありません";
            color = (255 << 16) | (50 << 8) | 50 | ((int)(alpha * 255) << 24); // RGB(255,50,50) with alpha
        }

        // 背景ボックス
        g.fill(color);
        g.rect(50, 500, g.width - 100, 50, 10);

        // 日本語フォントを設定
        if (kernel != null && kernel.getJapaneseFont() != null) {
            g.textFont(kernel.getJapaneseFont());
        }

        // テキスト
        g.fill(255, 255, 255, (int)(alpha * 255));
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(18);
        g.text(message, g.width / 2, 525);
    }

    /**
     * パターンヒントを表示する。
     *
     * @param p 描画用のPAppletインスタンス
     */
    private void drawPatternHint(PApplet p) {
        // チュートリアル表示を削除 - よりクリーンなデザインのため
    }

    /**
     * パターンヒントを表示する（PGraphics版）。
     *
     * @param g 描画用のPGraphicsインスタンス
     */
    private void drawPatternHint(processing.core.PGraphics g) {
        // チュートリアル表示を削除 - よりクリーンなデザインのため
    }
    
    /**
     * 開発時のデバッグ情報を表示する。
     * 
     * @param p 描画用のPAppletインスタンス
     */
    private void drawDebugInfo(PApplet p) {
        p.fill(255, 255, 255, 100);
        p.textAlign(p.LEFT, p.TOP);
        p.textSize(10);
        p.text("パターン: " + currentPattern + " | ドラッグ: " + isDragging, 10, p.height - 20);
        
        // パターングリッド中心位置の表示
        p.text("グリッド中心: (" + gridCenterX + "," + gridCenterY + ")", 10, p.height - 35);
    }

    /**
     * 開発時のデバッグ情報を表示する（PGraphics版）。
     *
     * @param g 描画用のPGraphicsインスタンス
     */
    private void drawDebugInfo(processing.core.PGraphics g) {
        // 日本語フォントを設定
        if (kernel != null && kernel.getJapaneseFont() != null) {
            g.textFont(kernel.getJapaneseFont());
        }

        g.fill(255, 255, 255, 100);
        g.textAlign(PApplet.LEFT, PApplet.TOP);
        g.textSize(10);
        g.text("パターン: " + currentPattern + " | ドラッグ: " + isDragging, 10, g.height - 20);

        // パターングリッド中心位置の表示
        g.text("グリッド中心: (" + gridCenterX + "," + gridCenterY + ")", 10, g.height - 35);
    }

    /**
     * マウスプレスイベント処理 (PGraphics version)。
     * 従来のマウスイベント用（後方互換性）。
     *
     * @param g PGraphicsインスタンス
     * @param mouseX マウスX座標
     * @param mouseY マウスY座標
     */
    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        // ジェスチャー処理に委譲
        System.out.println("LockScreen: mousePressed - delegating to gesture system");
    }

    /**
     * @deprecated Use {@link #mousePressed(PGraphics, int, int)} instead
     */
    @Deprecated
    @Override
    public void mousePressed(processing.core.PApplet p, int mouseX, int mouseY) {
        PGraphics g = p.g;
        mousePressed(g, mouseX, mouseY);
    }
    
    /**
     * ジェスチャーイベント処理。
     * ドラッグによるパターン入力を処理する。
     * ロック画面では特定のジェスチャー以外をブロックする。
     * 
     * @param event ジェスチャーイベント
     * @return イベントを処理した場合true、他のリスナーに渡す場合false
     */
    @Override
    public boolean onGesture(GestureEvent event) {
        System.out.println("LockScreen: Gesture received - " + event.getType() + 
                         " at (" + event.getStartX() + "," + event.getStartY() + ")");
        
        // パターン入力が表示されていない場合、システムジェスチャーと通知スクロールのみ許可
        if (!patternInputVisible && patternSlideProgress < 0.1f) {
            if (event.getType() == GestureType.SWIPE_UP && event.getStartY() >= 600 * 0.9f) {
                return false; // コントロールセンター用SWIPE_UPは許可
            }
            if (event.getType() == GestureType.SWIPE_DOWN && event.getStartY() <= 600 * 0.1f) {
                return false; // 通知センター用SWIPE_DOWNは許可
            }
            // 通知エリア内でのスワイプとドラッグは通知スクロール用に許可
            boolean inNotificationArea = false;
            if (event.getType() == GestureType.DRAG_START) {
                inNotificationArea = isInNotificationArea(event.getStartX(), event.getStartY());
            } else if (event.getType() == GestureType.DRAG_MOVE || event.getType() == GestureType.DRAG_END) {
                // ドラッグ中は現在座標と開始座標の両方をチェック
                inNotificationArea = isInNotificationArea(event.getCurrentX(), event.getCurrentY()) ||
                                   isInNotificationArea(event.getStartX(), event.getStartY());
            } else {
                inNotificationArea = isInNotificationArea(event.getStartX(), event.getStartY());
            }
            
            if ((event.getType() == GestureType.SWIPE_UP || 
                 event.getType() == GestureType.SWIPE_DOWN ||
                 event.getType() == GestureType.DRAG_START ||
                 event.getType() == GestureType.DRAG_MOVE ||
                 event.getType() == GestureType.DRAG_END) 
                && inNotificationArea) {
                System.out.println("LockScreen: Allowing notification area gesture - " + event.getType() + 
                                 " at (" + event.getCurrentX() + "," + event.getCurrentY() + ")");
                // スクロール処理は後続のswitch文で処理されるため、ここではスキップ
            } else {
                // その他のジェスチャーはブロック（パターン入力なし）
                System.out.println("LockScreen: Blocking gesture in pattern-hidden mode - " + event.getType());
                return true;
            }
        }
        
        // 認証フィードバック中は最低限のジェスチャーのみ処理
        if (authFeedback != AuthFeedback.NONE) {
            if (event.getType() == GestureType.SWIPE_UP && event.getStartY() >= 600 * 0.9f) {
                return false; // コントロールセンター用SWIPE_UPは許可
            }
            if (event.getType() == GestureType.SWIPE_DOWN && event.getStartY() <= 600 * 0.1f) {
                return false; // 通知センター用SWIPE_DOWNは許可
            }
            return true; // その他はブロック
        }
        
        switch (event.getType()) {
            case DRAG_START:
                handleDragStart(event);
                return true;
                
            case DRAG_MOVE:
                handleDragMove(event);
                return true;
                
            case DRAG_END:
                handleDragEnd(event);
                return true;
                
            case SWIPE_UP:
                // 画面下部からのスワイプアップはコントロールセンター用に許可
                if (event.getStartY() >= 600 * 0.9f) {
                    System.out.println("LockScreen: SWIPE_UP from bottom (y=" + event.getStartY() + 
                                     ") - delegating to control center");
                    return false; // イベントを他のリスナー（Kernel→コントロールセンター）に渡す
                } else if (isInNotificationArea(event.getStartX(), event.getStartY()) && !patternInputVisible) {
                    // 通知エリア内でのスワイプアップは通知スクロール（下向きスクロール）
                    System.out.println("LockScreen: SWIPE_UP in notification area - executing scroll");
                    handleNotificationScroll(event, false);
                    return true;
                } else {
                    boolean inNotificationArea = isInNotificationArea(event.getStartX(), event.getStartY());
                    System.out.println("LockScreen: SWIPE_UP not from bottom (y=" + event.getStartY() + 
                                     ") - inNotificationArea=" + inNotificationArea + 
                                     ", patternInputVisible=" + patternInputVisible + " - blocked");
                    return true;
                }
                
            case SWIPE_DOWN:
                // 画面上部からのスワイプダウンは通知センター用に許可
                if (event.getStartY() <= 600 * 0.1f) {
                    System.out.println("LockScreen: SWIPE_DOWN from top (y=" + event.getStartY() + 
                                     ") - delegating to notification center");
                    return false; // イベントを他のリスナー（Kernel→通知センター）に渡す
                } else if (isInNotificationArea(event.getStartX(), event.getStartY()) && !patternInputVisible) {
                    // 通知エリア内でのスワイプダウンは通知スクロール（上向きスクロール）
                    System.out.println("LockScreen: SWIPE_DOWN in notification area - executing scroll");
                    handleNotificationScroll(event, true);
                    return true;
                } else {
                    boolean inNotificationArea = isInNotificationArea(event.getStartX(), event.getStartY());
                    System.out.println("LockScreen: SWIPE_DOWN not from top (y=" + event.getStartY() + 
                                     ") - inNotificationArea=" + inNotificationArea + 
                                     ", patternInputVisible=" + patternInputVisible + " - blocked");
                    return true;
                }
                
            case SWIPE_LEFT:
            case SWIPE_RIGHT:
            case TAP:
            case LONG_PRESS:
                // ロック中はこれらのジェスチャーを無効化
                System.out.println("LockScreen: " + event.getType() + " blocked during lock");
                return true;
                
            default:
                System.out.println("LockScreen: Unknown gesture " + event.getType() + " blocked");
                return true;
        }
    }
    
    /**
     * ドラッグ開始処理。
     * パターン入力の開始を処理する。
     * 
     * @param event ジェスチャーイベント
     * @return イベントを処理した場合true
     */
    private boolean handleDragStart(GestureEvent event) {
        int startX = event.getStartX();
        int startY = event.getStartY();
        
        System.out.println("LockScreen: ドラッグ開始試行 at (" + startX + ", " + startY + ")");
        
        // 通知エリア内でのドラッグの場合は通知スクロール処理
        if (isInNotificationArea(startX, startY) && !patternInputVisible) {
            System.out.println("LockScreen: 通知エリアドラッグ開始");
            isNotificationDragScrolling = true;
            lastNotificationDragY = startY;
            notificationScrollVelocity = 0;
            return true;
        }
        
        // パターン入力エリアでのドラッグの場合はパターン処理
        isDragging = true;
        currentPattern.clear();
        dragPath.clear();
        dragPath.add(new int[]{startX, startY});
        
        // 最初に触れたドットを追加
        int dotIndex = getDotIndexAt(startX, startY);
        if (dotIndex >= 0) {
            currentPattern.add(dotIndex);
            System.out.println("LockScreen: 初期ドット選択 - " + dotIndex);
        } else {
            System.out.println("LockScreen: 初期ドット未検出 at (" + startX + ", " + startY + ")");
        }
        
        System.out.println("LockScreen: ドラッグ開始 - パターン: " + currentPattern);
        return true;
    }
    
    /**
     * ドラッグ移動処理。
     * パターンの軌跡を記録し、新しいドットとの接触を検出する。
     * 
     * @param event ジェスチャーイベント
     * @return イベントを処理した場合true
     */
    private boolean handleDragMove(GestureEvent event) {
        int currentX = event.getCurrentX();
        int currentY = event.getCurrentY();
        
        // 通知エリアでのドラッグスクロール処理
        if (isNotificationDragScrolling) {
            handleNotificationDragScroll(event);
            return true;
        }
        
        if (!isDragging) return false;
        
        // ドラッグ軌跡を記録
        dragPath.add(new int[]{currentX, currentY});
        
        // 新しいドットに触れているかチェック
        int dotIndex = getDotIndexAt(currentX, currentY);
        if (dotIndex >= 0 && !currentPattern.contains(dotIndex)) {
            currentPattern.add(dotIndex);
            System.out.println("LockScreen: ドット追加 - 現在のパターン: " + currentPattern);
        }
        
        return true;
    }
    
    /**
     * ドラッグ終了処理。
     * 完成したパターンの認証を実行する。
     * 
     * @param event ジェスチャーイベント
     * @return イベントを処理した場合true
     */
    private boolean handleDragEnd(GestureEvent event) {
        // 通知エリアでのドラッグ終了処理
        if (isNotificationDragScrolling) {
            System.out.println("LockScreen: 通知エリアドラッグ終了 - 慣性スクロール開始");
            isNotificationDragScrolling = false;
            // 慣性スクロールは draw() メソッドで処理される
            return true;
        }
        
        if (!isDragging) return false;
        
        isDragging = false;
        
        // パターンが入力されている場合のみ認証
        if (!currentPattern.isEmpty()) {
            boolean isCorrect = lockManager.checkPattern(currentPattern);
            
            if (isCorrect) {
                showAuthFeedback(AuthFeedback.SUCCESS);
                // 少し遅らせてアンロック処理を実行
                new Thread(() -> {
                    try {
                        Thread.sleep(500); // フィードバックを少し表示してから
                        unlockAndNavigateToHome();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
                
            } else {
                showAuthFeedback(AuthFeedback.FAILED);
                // パターンをクリア
                currentPattern.clear();
                dragPath.clear();
            }
        }
        
        return true;
    }
    
    /**
     * 指定座標がパターングリッド領域内かどうかを判定する。
     * 
     * @param x X座標
     * @param y Y座標
     * @return グリッド領域内の場合true
     */
    private boolean isInPatternGridArea(int x, int y) {
        int gridLeft = gridCenterX - DOT_SPACING;
        int gridRight = gridCenterX + DOT_SPACING;
        int gridTop = gridCenterY - DOT_SPACING;
        int gridBottom = gridCenterY + DOT_SPACING;
        
        return x >= gridLeft && x <= gridRight && y >= gridTop && y <= gridBottom;
    }
    
    /**
     * 指定座標にあるドットのインデックスを取得する。
     * 
     * @param x X座標
     * @param y Y座標
     * @return ドットインデックス（0-8）、該当なしの場合-1
     */
    private int getDotIndexAt(int x, int y) {
        for (int i = 0; i < GRID_SIZE * GRID_SIZE; i++) {
            int[] dotPos = getDotPosition(i);
            int distance = (int) Math.sqrt(Math.pow(x - dotPos[0], 2) + Math.pow(y - dotPos[1], 2));
            
            // デバッグ情報（最初の数回のみ）
            if (System.currentTimeMillis() % 1000 < 100) {
                System.out.println("LockScreen: Checking dot " + i + " at (" + dotPos[0] + "," + dotPos[1] + 
                                 ") distance=" + distance + " from (" + x + "," + y + ")");
            }
            
            if (distance <= DOT_DETECTION_RADIUS) {
                System.out.println("LockScreen: Dot " + i + " detected at distance " + distance);
                return i;
            }
        }
        return -1;
    }
    
    /**
     * ドットインデックスから座標を取得する。
     * 
     * @param dotIndex ドットインデックス（0-8）
     * @return [x, y]座標の配列
     */
    private int[] getDotPosition(int dotIndex) {
        int row = dotIndex / GRID_SIZE;
        int col = dotIndex % GRID_SIZE;
        
        int x = gridCenterX + (col - 1) * DOT_SPACING;
        int y = gridCenterY + (row - 1) * DOT_SPACING;
        
        return new int[]{x, y};
    }
    
    /**
     * 認証フィードバックを表示する。
     * 
     * @param feedback フィードバックタイプ
     */
    private void showAuthFeedback(AuthFeedback feedback) {
        this.authFeedback = feedback;
        this.feedbackStartTime = System.currentTimeMillis();
    }
    
    /**
     * アンロック処理とホーム画面への遷移。
     */
    private void unlockAndNavigateToHome() {
        System.out.println("LockScreen: 認証成功 - アンロック処理実行中...");
        
        // OSをアンロック状態にする
        lockManager.unlock();
        
        // LauncherAppのホーム画面に遷移
        try {
            jp.moyashi.phoneos.core.apps.launcher.LauncherApp launcherApp = 
                (jp.moyashi.phoneos.core.apps.launcher.LauncherApp) findLauncherApp();
            
            if (launcherApp != null) {
                jp.moyashi.phoneos.core.ui.Screen homeScreen = launcherApp.getEntryScreen(kernel);
                
                // 現在のロック画面をポップし、ホーム画面をプッシュ
                kernel.getScreenManager().popScreen();
                kernel.getScreenManager().pushScreen(homeScreen);
                
                System.out.println("LockScreen: ホーム画面への遷移完了");
            } else {
                System.err.println("LockScreen: LauncherAppが見つかりません");
            }
        } catch (Exception e) {
            System.err.println("LockScreen: ホーム画面遷移エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * LauncherAppを検索して取得する。
     * 
     * @return LauncherAppのインスタンス、見つからない場合はnull
     */
    private jp.moyashi.phoneos.core.app.IApplication findLauncherApp() {
        if (kernel.getAppLoader() == null) return null;
        
        for (jp.moyashi.phoneos.core.app.IApplication app : kernel.getAppLoader().getLoadedApps()) {
            if ("jp.moyashi.phoneos.core.apps.launcher".equals(app.getApplicationId())) {
                return app;
            }
        }
        return null;
    }
    
    
    /**
     * ロック画面のクリーンアップ処理 (PGraphics version)。
     * レイヤー管理システムとジェスチャーリスナーの登録を解除する。
     */
    @Override
    public void cleanup(PGraphics g) {
        System.out.println("LockScreen: クリーンアップ実行中...");

        // レイヤー管理システムから登録解除
        unregisterFromLayerManager();

        // ジェスチャーマネージャーから登録解除
        if (kernel.getGestureManager() != null) {
            kernel.getGestureManager().removeGestureListener(this);
        }

        // 状態をクリア
        currentPattern.clear();
        dragPath.clear();
        isDragging = false;
        authFeedback = AuthFeedback.NONE;

        System.out.println("LockScreen: クリーンアップ完了");
    }

    /**
     * @deprecated Use {@link #cleanup(PGraphics)} instead
     */
    @Deprecated
    @Override
    public void cleanup(PApplet p) {
        PGraphics g = p.g;
        cleanup(g);
    }
    
    /**
     * ロック画面のタイトルを取得する。
     * 
     * @return "Lock Screen"
     */
    @Override
    public String getScreenTitle() {
        return "Lock Screen";
    }
    
    /**
     * レイヤー管理システムにロック画面を登録する。
     */
    private void registerWithLayerManager() {
        if (layerManager != null && !layerRegistered) {
            // ロック画面用のレンダラーを作成
            UILayer.LayerRenderer renderer = new UILayer.LayerRenderer() {
                @Override
                public void render(PApplet p) {
                    // LockScreenのdraw()メソッドを直接呼び出す
                    draw(p);
                }
                
                @Override
                public boolean isVisible() {
                    // ロック画面は常にロック状態と連動
                    return lockManager != null && lockManager.isLocked();
                }
            };
            
            // レイヤー登録要求
            boolean granted = layerManager.requestLayerPermission(
                LAYER_ID, 
                "Lock Screen", 
                8000, // コントロールセンターより低い、通知センターより高い優先度
                renderer
            );
            
            if (granted) {
                layerRegistered = true;
                System.out.println("LockScreen: レイヤー管理システムに登録完了");
            } else {
                System.err.println("LockScreen: レイヤー管理システムへの登録に失敗");
            }
        }
    }
    
    /**
     * レイヤー管理システムからロック画面を削除する。
     */
    private void unregisterFromLayerManager() {
        if (layerManager != null && layerRegistered) {
            boolean removed = layerManager.removeLayer(LAYER_ID);
            if (removed) {
                layerRegistered = false;
                System.out.println("LockScreen: レイヤー管理システムから削除完了");
            } else {
                System.err.println("LockScreen: レイヤー管理システムからの削除に失敗");
            }
        }
    }
    
    /**
     * レイヤーベースの動的優先度を取得する。
     * レイヤー管理システムが有効な場合、動的優先度を使用する。
     * 
     * @return 現在の優先度
     */
    @Override
    public int getPriority() {
        // 固定優先度を返す（循環参照を避けるため）
        return 8000; // コントロールセンターより低い、通知センターより高い優先度
    }
    
    /**
     * ロック画面がジェスチャーを受け取るべき座標範囲内かどうかを確認する。
     * コントロールセンターが表示中の場合は、ロック画面はジェスチャーを受け取らない。
     * 
     * @param x X座標
     * @param y Y座標
     * @return ロック画面がジェスチャーを処理すべき場合true
     */
    @Override
    public boolean isInBounds(int x, int y) {
        // コントロールセンターが表示中かどうかをチェック
        boolean controlCenterVisible = kernel.getControlCenterManager().isVisible() && 
                                      kernel.getControlCenterManager().getAnimationProgress() > 0.1f;
        
        // ロック画面がアクティブで、コントロールセンターが表示されていない場合のみtrue
        boolean inBounds = lockManager.isLocked() && !controlCenterVisible;
        
        System.out.println("LockScreen.isInBounds(" + x + ", " + y + ") = " + inBounds + 
                          " (locked=" + lockManager.isLocked() + ", controlCenterVisible=" + controlCenterVisible + 
                          ", priority=" + getPriority() + ")");
        
        return inBounds;
    }
    
    /**
     * ホームボタンが押された際にパターン入力画面を表示する。
     */
    public void highlightPatternArea() {
        if (!patternInputVisible && !patternAnimating) {
            showPatternInput();
        } else if (patternInputVisible && !patternAnimating) {
            hidePatternInput();
        }
    }
    
    /**
     * パターン入力画面を表示する。
     */
    private void showPatternInput() {
        patternInputVisible = true;
        patternAnimating = true;
        patternAnimationStartTime = System.currentTimeMillis();
        System.out.println("LockScreen: Showing pattern input screen");
    }
    
    /**
     * パターン入力画面を隠す。
     */
    private void hidePatternInput() {
        patternInputVisible = false;
        patternAnimating = true;
        patternAnimationStartTime = System.currentTimeMillis();
        System.out.println("LockScreen: Hiding pattern input screen");
    }
    
    /**
     * ハイライト状態をチェックし、期限切れの場合は無効化する。
     * 
     * @return ハイライト表示中の場合true
     */
    private boolean checkHighlightState() {
        if (!patternHighlighted) return false;
        
        long elapsed = System.currentTimeMillis() - highlightStartTime;
        if (elapsed > HIGHLIGHT_DURATION) {
            patternHighlighted = false;
            return false;
        }
        
        return true;
    }
    
    /**
     * パターンエリアのハイライト背景を描画する。
     * 
     * @param p 描画用のPAppletインスタンス
     */
    private void drawHighlightBackground(PApplet p) {
        long elapsed = System.currentTimeMillis() - highlightStartTime;
        float progress = (float) elapsed / HIGHLIGHT_DURATION;
        
        // フェードアウトエフェクト
        float alpha = 1.0f - progress;
        
        // パルスエフェクト
        float pulse = 0.7f + 0.3f * (float) Math.sin(elapsed * 0.01);
        
        // ハイライト領域の描画
        int gridLeft = gridCenterX - DOT_SPACING - 30;
        int gridRight = gridCenterX + DOT_SPACING + 30;
        int gridTop = gridCenterY - DOT_SPACING - 30;
        int gridBottom = gridCenterY + DOT_SPACING + 30;
        
        // 背景色（薄い黄色のハイライト）
        p.fill(255, 255, 100, (int)(alpha * pulse * 50));
        p.stroke(255, 255, 150, (int)(alpha * pulse * 150));
        p.strokeWeight(2);
        p.rect(gridLeft, gridTop, gridRight - gridLeft, gridBottom - gridTop, 15);
        
        // ハイライトメッセージ
        p.fill(255, 255, 255, (int)(alpha * 255));
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(16);
        p.text("パターンを入力してください", p.width / 2, gridTop - 20);
    }
    
    /**
     * パターンスライドアニメーションを更新する。
     */
    private void updatePatternSlideAnimation() {
        if (!patternAnimating) return;
        
        long elapsed = System.currentTimeMillis() - patternAnimationStartTime;
        float progress = (float) elapsed / PATTERN_SLIDE_DURATION;
        
        if (progress >= 1.0f) {
            // アニメーション完了
            progress = 1.0f;
            patternAnimating = false;
        }
        
        // イージング関数を適用
        progress = easeInOutCubic(progress);
        
        if (patternInputVisible) {
            // 表示アニメーション
            patternSlideProgress = progress;
        } else {
            // 非表示アニメーション
            patternSlideProgress = 1.0f - progress;
        }
    }
    
    /**
     * イージング関数（ease-in-out cubic）
     */
    private float easeInOutCubic(float t) {
        if (t < 0.5f) {
            return 4 * t * t * t;
        } else {
            return 1 - (float) Math.pow(-2 * t + 2, 3) / 2;
        }
    }
    
    /**
     * パターン入力画面を描画する（スライドアニメーション付き）。
     */
    private void drawPatternInputScreen(PApplet p) {
        // スライド位置を計算（画面下から上にスライド）
        float screenHeight = p.height;
        float inputScreenHeight = screenHeight * 0.6f; // 画面の60%を占める
        float slideOffset = inputScreenHeight * (1.0f - patternSlideProgress);
        
        // パターン入力画面の背景
        p.fill(30, 35, 45, (int)(255 * patternSlideProgress * 0.9f));
        p.rect(0, screenHeight - inputScreenHeight + slideOffset, p.width, inputScreenHeight);
        
        // 上部のハンドル（ドラッグ用）
        float handleY = screenHeight - inputScreenHeight + slideOffset + 10;
        p.fill(150, 150, 150, (int)(255 * patternSlideProgress));
        p.rect(p.width/2 - 30, handleY, 60, 4, 2);
        
        // パターン入力のタイトル
        p.fill(255, 255, 255, (int)(255 * patternSlideProgress));
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(18);
        p.text("パターンでロックを解除", p.width / 2, handleY + 30);
        
        // アニメーション進行度に応じてパターングリッドを描画
        if (patternSlideProgress > 0.3f) {
            p.pushMatrix();
            p.translate(0, slideOffset);
            
            // パターングリッドの透明度を調整
            drawPatternGridWithAlpha(p, patternSlideProgress);
            drawDragPath(p);
            drawAuthFeedback(p);
            drawPatternHint(p);
            
            p.popMatrix();
        }
    }

    /**
     * パターン入力画面を描画する（スライドアニメーション付き）（PGraphics版）。
     */
    private void drawPatternInputScreen(processing.core.PGraphics g) {
        // スライド位置を計算（画面下から上にスライド）
        float screenHeight = g.height;
        float inputScreenHeight = screenHeight * 0.6f; // 画面の60%を占める
        float slideOffset = inputScreenHeight * (1.0f - patternSlideProgress);

        // パターン入力画面の背景
        g.fill(30, 35, 45, (int)(255 * patternSlideProgress * 0.9f));
        g.rect(0, screenHeight - inputScreenHeight + slideOffset, g.width, inputScreenHeight);

        // 上部のハンドル（ドラッグ用）
        float handleY = screenHeight - inputScreenHeight + slideOffset + 10;
        g.fill(150, 150, 150, (int)(255 * patternSlideProgress));
        g.rect(g.width/2 - 30, handleY, 60, 4, 2);

        // 日本語フォントを設定
        if (kernel != null && kernel.getJapaneseFont() != null) {
            g.textFont(kernel.getJapaneseFont());
        }

        // パターン入力のタイトル
        g.fill(255, 255, 255, (int)(255 * patternSlideProgress));
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(18);
        g.text("パターンでロックを解除", g.width / 2, handleY + 30);

        // アニメーション進行度に応じてパターングリッドを描画
        if (patternSlideProgress > 0.3f) {
            g.pushMatrix();
            g.translate(0, slideOffset);

            // パターングリッドの透明度を調整
            drawPatternGridWithAlpha(g, patternSlideProgress);
            drawDragPath(g);
            drawAuthFeedback(g);
            drawPatternHint(g);

            g.popMatrix();
        }
    }
    
    /**
     * 透明度を指定してパターングリッドを描画する。
     */
    private void drawPatternGridWithAlpha(PApplet p, float alpha) {
        // グリッドの各ドットを描画
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int dotIndex = row * GRID_SIZE + col;
                int[] dotPos = getDotPosition(dotIndex);
                
                // ドットが現在のパターンに含まれているかチェック
                boolean isSelected = currentPattern.contains(dotIndex);
                
                // 選択状態に応じて色を変更（透明度適用）
                if (isSelected) {
                    p.fill(100, 180, 255, (int)(255 * alpha)); // 青色（選択済み）
                    p.stroke(150, 200, 255, (int)(255 * alpha));
                    p.strokeWeight(4);
                } else {
                    p.fill(60, 65, 75, (int)(255 * alpha)); // 濃いグレー（未選択）
                    p.stroke(100, 110, 120, (int)(255 * alpha));
                    p.strokeWeight(2);
                }
                
                // ドットを描画
                p.ellipse(dotPos[0], dotPos[1], DOT_RADIUS * 2, DOT_RADIUS * 2);
                
                // 中央の小さなドット
                p.fill(200, 200, 200, (int)(255 * alpha));
                p.noStroke();
                p.ellipse(dotPos[0], dotPos[1], 6, 6);
            }
        }
        
        // 選択されたドット間に線を描画
        if (currentPattern.size() > 1) {
            p.stroke(100, 180, 255, (int)(255 * alpha));
            p.strokeWeight(4);
            
            for (int i = 0; i < currentPattern.size() - 1; i++) {
                int[] start = getDotPosition(currentPattern.get(i));
                int[] end = getDotPosition(currentPattern.get(i + 1));
                p.line(start[0], start[1], end[0], end[1]);
            }
        }
    }

    /**
     * 透明度を指定してパターングリッドを描画する（PGraphics版）。
     */
    private void drawPatternGridWithAlpha(processing.core.PGraphics g, float alpha) {
        // グリッドの各ドットを描画
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int dotIndex = row * GRID_SIZE + col;
                int[] dotPos = getDotPosition(dotIndex);

                // ドットが現在のパターンに含まれているかチェック
                boolean isSelected = currentPattern.contains(dotIndex);

                // 選択状態に応じて色を変更（透明度適用）
                if (isSelected) {
                    g.fill(100, 180, 255, (int)(255 * alpha)); // 青色（選択済み）
                    g.stroke(150, 200, 255, (int)(255 * alpha));
                    g.strokeWeight(4);
                } else {
                    g.fill(60, 65, 75, (int)(255 * alpha)); // 濃いグレー（未選択）
                    g.stroke(100, 110, 120, (int)(255 * alpha));
                    g.strokeWeight(2);
                }

                // ドットを描画
                g.ellipse(dotPos[0], dotPos[1], DOT_RADIUS * 2, DOT_RADIUS * 2);

                // 中央の小さなドット
                g.fill(200, 200, 200, (int)(255 * alpha));
                g.noStroke();
                g.ellipse(dotPos[0], dotPos[1], 6, 6);
            }
        }

        // 選択されたドット間に線を描画
        if (currentPattern.size() > 1) {
            g.stroke(100, 180, 255, (int)(255 * alpha));
            g.strokeWeight(4);

            for (int i = 0; i < currentPattern.size() - 1; i++) {
                int[] start = getDotPosition(currentPattern.get(i));
                int[] end = getDotPosition(currentPattern.get(i + 1));
                g.line(start[0], start[1], end[0], end[1]);
            }
        }
    }

    /**
     * ホームボタンのヒントを表示する。
     */
    private void drawHomeButtonHint(PApplet p) {
        // ヒント背景
        p.fill(0, 0, 0, 100);
        p.rect(50, p.height - 100, p.width - 100, 60, 8);
        
        // ヒントテキスト
        p.fill(200, 200, 200);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(16);
        p.text("ホームボタン（スペース）を押してロックを解除", p.width / 2, p.height - 80);

    }

    /**
     * ホームボタンのヒントを表示する（PGraphics版）。
     */
    private void drawHomeButtonHint(processing.core.PGraphics g) {
        // ヒント背景
        g.fill(0, 0, 0, 100);
        g.rect(50, g.height - 100, g.width - 100, 60, 8);

        // 日本語フォントを設定
        if (kernel != null && kernel.getJapaneseFont() != null) {
            g.textFont(kernel.getJapaneseFont());
        }

        // ヒントテキスト
        g.fill(200, 200, 200);
        g.textAlign(PApplet.CENTER, PApplet.CENTER);
        g.textSize(16);
        g.text("ホームボタン（スペース）を押してロックを解除", g.width / 2, g.height - 80);
    }

    /**
     * 通知エリア内かどうかを判定する。
     *
     * @param x X座標
     * @param y Y座標
     * @return 通知エリア内の場合true
     */
    private boolean isInNotificationArea(float x, float y) {
        int notificationAreaTop = 200;
        int notificationAreaBottom = 600 - 120; // ホームボタンヒントの上
        return y >= notificationAreaTop && y <= notificationAreaBottom;
    }
    
    /**
     * 通知スクロールを処理する。
     * 
     * @param event ジェスチャーイベント
     * @param isScrollingUp trueの場合上向きスクロール（通知を下へ移動）、falseの場合下向きスクロール（通知を上へ移動）
     */
    private void handleNotificationScroll(GestureEvent event, boolean isScrollingUp) {
        if (notificationManager == null) return;
        
        List<INotification> allNotifications = notificationManager.getRecentNotifications(10);
        if (allNotifications.isEmpty()) return;
        
        // スクロール感度とスピード調整
        float scrollSpeed = 40.0f; // iOSライクなスムーズなスクロール
        
        if (isScrollingUp) {
            // スワイプダウン → 通知を下に移動（スクロールオフセットを減少）
            notificationScrollOffset = Math.max(0, notificationScrollOffset - scrollSpeed);
            System.out.println("LockScreen: Notification scroll up, offset: " + notificationScrollOffset);
        } else {
            // スワイプアップ → 通知を上に移動（スクロールオフセットを増加）
            int notificationAreaHeight = 480 - 200; // 表示エリアの高さ
            int notificationHeight = 60;
            int spacing = 10;
            int totalItemHeight = notificationHeight + spacing;
            int totalContentHeight = allNotifications.size() * totalItemHeight;
            int maxScrollOffset = Math.max(0, totalContentHeight - notificationAreaHeight);
            
            notificationScrollOffset = Math.min(maxScrollOffset, notificationScrollOffset + scrollSpeed);
            System.out.println("LockScreen: Notification scroll down, offset: " + notificationScrollOffset + ", max: " + maxScrollOffset);
        }
        
        isScrollingNotifications = true;
        
        // スクロールアニメーション用の後処理（オプション）
        // 将来的にはスムーズなアニメーションを追加できる
    }
    
    /**
     * 通知エリアでのドラッグスクロールを処理する（コントロールセンターと同じ実装）。
     * 
     * @param event ドラッグイベント
     */
    private void handleNotificationDragScroll(GestureEvent event) {
        if (notificationManager == null) return;
        
        List<INotification> allNotifications = notificationManager.getRecentNotifications(10);
        if (allNotifications.isEmpty()) return;
        
        float deltaY = (float)event.getCurrentY() - lastNotificationDragY;
        notificationScrollOffset -= deltaY * NOTIFICATION_SCROLL_SENSITIVITY;
        
        // スクロール範囲の制限
        float notificationAreaHeight = 480f - 200f; // 表示エリアの高さ
        float notificationHeight = 60f;
        float spacing = 10f;
        float totalItemHeight = notificationHeight + spacing;
        float totalContentHeight = allNotifications.size() * totalItemHeight;
        float maxScrollOffset = Math.max(0, totalContentHeight - notificationAreaHeight);
        
        if (notificationScrollOffset < 0) notificationScrollOffset = 0;
        if (notificationScrollOffset > maxScrollOffset) notificationScrollOffset = maxScrollOffset;
        
        notificationScrollVelocity = -deltaY * NOTIFICATION_SCROLL_SENSITIVITY;
        lastNotificationDragY = (float)event.getCurrentY();
        
        System.out.println("LockScreen: Smooth scroll - deltaY: " + deltaY + 
                          ", offset: " + String.format("%.2f", notificationScrollOffset) + 
                          ", velocity: " + String.format("%.2f", notificationScrollVelocity));
    }
    
    /**
     * 通知スクロールの慣性更新を処理する。
     * draw() メソッドから呼ばれる。
     */
    private void updateNotificationScrollInertia() {
        if (Math.abs(notificationScrollVelocity) > MIN_NOTIFICATION_SCROLL_VELOCITY) {
            notificationScrollOffset += notificationScrollVelocity;
            
            // スクロール範囲の制限
            if (notificationManager != null) {
                List<INotification> allNotifications = notificationManager.getRecentNotifications(10);
                if (!allNotifications.isEmpty()) {
                    float notificationAreaHeight = 480f - 200f;
                    float notificationHeight = 60f;
                    float spacing = 10f;
                    float totalItemHeight = notificationHeight + spacing;
                    float totalContentHeight = allNotifications.size() * totalItemHeight;
                    float maxScrollOffset = Math.max(0, totalContentHeight - notificationAreaHeight);
                    
                    if (notificationScrollOffset < 0) {
                        notificationScrollOffset = 0;
                        notificationScrollVelocity = 0;
                    } else if (notificationScrollOffset > maxScrollOffset) {
                        notificationScrollOffset = maxScrollOffset;
                        notificationScrollVelocity = 0;
                    } else {
                        notificationScrollVelocity *= NOTIFICATION_SCROLL_FRICTION;
                    }
                }
            }
        } else {
            notificationScrollVelocity = 0;
        }
    }

    /**
     * キー押下イベントを処理する (PGraphics version)。
     * スペースキーでロック解除フィールドを展開/ロックを解除する。
     *
     * @param g PGraphicsインスタンス
     * @param key 押されたキー文字
     * @param keyCode キーコード
     */
    @Override
    public void keyPressed(PGraphics g, char key, int keyCode) {
        System.out.println("LockScreen: keyPressed - key: '" + key + "', keyCode: " + keyCode);

        // スペースキーでパターン入力画面を展開/閉じる
        if (key == ' ' || keyCode == 32) {
            if (!patternInputVisible) {
                System.out.println("LockScreen: スペースキーが押されました - パターン入力画面を展開");
                showPatternInput();
            } else {
                System.out.println("LockScreen: スペースキーが押されました - パターン入力画面を閉じる");
                hidePatternInput();
            }
            return;
        }

        // ESCキーでパターン入力画面を閉じる
        if (keyCode == 27 && patternInputVisible) { // ESC key
            System.out.println("LockScreen: ESCキーが押されました - パターン入力画面を閉じる");
            hidePatternInput();
            return;
        }

        // パターン入力画面が表示されていない場合、他のキー入力は無視
        if (!patternInputVisible) {
            System.out.println("LockScreen: パターン入力画面が非表示のため、キー入力を無視");
        }
    }

    /**
     * @deprecated Use {@link #keyPressed(PGraphics, char, int)} instead
     */
    @Deprecated
    @Override
    public void keyPressed(processing.core.PApplet p, char key, int keyCode) {
        PGraphics g = p.g;
        keyPressed(g, key, keyCode);
    }






    /**
     * Adds mouseDragged support for PGraphics (empty implementation, can be overridden)
     */
    @Override
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        // Default implementation - subclasses can override
    }

    /**
     * Adds mouseReleased support for PGraphics (empty implementation, can be overridden)
     */
    @Override
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        // Default implementation - subclasses can override
    }
}
