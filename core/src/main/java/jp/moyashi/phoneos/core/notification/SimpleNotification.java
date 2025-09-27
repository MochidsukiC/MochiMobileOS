package jp.moyashi.phoneos.core.notification;

import processing.core.PApplet;
import processing.core.PFont;
import jp.moyashi.phoneos.core.Kernel;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * シンプルな通知の実装クラス。
 * 基本的な通知機能を提供する。
 * 
 * @author YourName
 * @version 1.0
 * @since 1.0
 */
public class SimpleNotification implements INotification {
    
    private final String id;
    private final String title;
    private final String content;
    private final String sender;
    private final long timestamp;
    private final int priority;
    private boolean isRead;
    private boolean isDismissed;
    
    /** 時刻フォーマッター */
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm");
    
    /** 削除ボタンのサイズ */
    private static final float DISMISS_BUTTON_SIZE = 24;
    
    /**
     * 新しい通知を作成する。
     * 
     * @param id 通知ID
     * @param title タイトル
     * @param content 内容
     * @param sender 送信者
     * @param priority 優先度（0=低、1=通常、2=高）
     */
    public SimpleNotification(String id, String title, String content, String sender, int priority) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.sender = sender;
        this.priority = Math.max(0, Math.min(2, priority));
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.isDismissed = false;
    }
    
    /**
     * 通常優先度で新しい通知を作成する。
     */
    public SimpleNotification(String id, String title, String content, String sender) {
        this(id, title, content, sender, 1);
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public String getTitle() {
        return title;
    }
    
    @Override
    public String getContent() {
        return content;
    }
    
    @Override
    public String getSender() {
        return sender;
    }
    
    @Override
    public long getTimestamp() {
        return timestamp;
    }
    
    @Override
    public boolean isRead() {
        return isRead;
    }
    
    @Override
    public void markAsRead() {
        this.isRead = true;
    }
    
    @Override
    public int getPriority() {
        return priority;
    }
    
    @Override
    public void draw(PApplet p, float x, float y, float width, float height) {
        if (isDismissed) return;
        
        p.pushStyle();
        
        // 日本語フォントを設定
        // フォント設定は省略（基本フォントを使用）
        
        // 背景
        int bgAlpha = isRead ? 100 : 150;
        p.fill(60, 60, 70, bgAlpha);
        p.noStroke();
        p.rect(x, y, width, height, 8);
        
        // 未読インジケーター
        if (!isRead) {
            p.fill(70, 130, 200);
            p.rect(x + 4, y + 4, 3, height - 8, 1);
        }
        
        // 優先度インジケーター
        if (priority == 2) {
            p.fill(220, 80, 80); // 高優先度：赤
            p.rect(x + width - 4, y + 4, 3, height - 8, 1);
        } else if (priority == 0) {
            p.fill(120, 120, 120); // 低優先度：灰色
            p.rect(x + width - 4, y + 4, 3, height - 8, 1);
        }
        
        // テキスト描画エリア
        float textX = x + (isRead ? 12 : 16);
        float textWidth = width - (textX - x) - DISMISS_BUTTON_SIZE - 8;
        
        // 送信者とタイムスタンプ
        p.fill(180, 180, 180);
        p.textAlign(PApplet.LEFT, PApplet.TOP);
        p.textSize(10);
        String timeStr = TIME_FORMAT.format(new Date(timestamp));
        p.text(sender + " • " + timeStr, textX, y + 6);
        
        // タイトル
        p.fill(255, 255, 255);
        p.textSize(12);
        p.text(truncateText(p, title, textWidth), textX, y + 20);
        
        // 内容
        p.fill(200, 200, 200);
        p.textSize(10);
        String wrappedContent = wrapText(p, content, textWidth, 2);
        p.text(wrappedContent, textX, y + 36);
        
        // 削除ボタン
        drawDismissButton(p, x + width - DISMISS_BUTTON_SIZE - 4, y + 4);
        
        p.popStyle();
    }
    
    /**
     * 削除ボタンを描画する。
     */
    private void drawDismissButton(PApplet p, float x, float y) {
        p.fill(150, 150, 150, 100);
        p.rect(x, y, DISMISS_BUTTON_SIZE, DISMISS_BUTTON_SIZE, 4);
        
        // X マーク
        p.stroke(200, 200, 200);
        p.strokeWeight(1.5f);
        float margin = 6;
        p.line(x + margin, y + margin, 
               x + DISMISS_BUTTON_SIZE - margin, y + DISMISS_BUTTON_SIZE - margin);
        p.line(x + DISMISS_BUTTON_SIZE - margin, y + margin, 
               x + margin, y + DISMISS_BUTTON_SIZE - margin);
        p.noStroke();
    }
    
    /**
     * テキストを指定幅に収まるように切り詰める。
     */
    private String truncateText(PApplet p, String text, float maxWidth) {
        if (text == null || text.isEmpty()) return "";
        
        String suffix = "...";
        if (p.textWidth(text) <= maxWidth) {
            return text;
        }
        
        for (int i = text.length() - 1; i > 0; i--) {
            String truncated = text.substring(0, i) + suffix;
            if (p.textWidth(truncated) <= maxWidth) {
                return truncated;
            }
        }
        return suffix;
    }
    
    /**
     * テキストを指定幅と行数に収まるように改行する。
     */
    private String wrapText(PApplet p, String text, float maxWidth, int maxLines) {
        if (text == null || text.isEmpty()) return "";
        
        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        int lineCount = 0;
        
        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;
            
            if (p.textWidth(testLine) > maxWidth) {
                if (currentLine.length() > 0) {
                    if (lineCount > 0) result.append("\n");
                    result.append(currentLine);
                    lineCount++;
                    
                    if (lineCount >= maxLines) {
                        result.append("...");
                        break;
                    }
                    
                    currentLine = new StringBuilder(word);
                } else {
                    // 単語が長すぎる場合
                    if (lineCount > 0) result.append("\n");
                    result.append(truncateText(p, word, maxWidth));
                    lineCount++;
                    break;
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        
        if (currentLine.length() > 0 && lineCount < maxLines) {
            if (lineCount > 0) result.append("\n");
            result.append(currentLine);
        }
        
        return result.toString();
    }
    
    @Override
    public boolean onClick(float x, float y) {
        if (isDismissed) return false;
        
        markAsRead();
        return true;
    }
    
    @Override
    public boolean isDismissible() {
        return !isDismissed;
    }
    
    @Override
    public void dismiss() {
        this.isDismissed = true;
    }
    
    /**
     * 削除ボタンがクリックされたかチェックする。
     * 
     * @param clickX クリック位置X
     * @param clickY クリック位置Y
     * @param notificationX 通知のX座標
     * @param notificationY 通知のY座標
     * @param notificationWidth 通知の幅
     * @return 削除ボタンがクリックされた場合true
     */
    public boolean isDismissButtonClicked(float clickX, float clickY, float notificationX, float notificationY, float notificationWidth) {
        float buttonX = notificationX + notificationWidth - DISMISS_BUTTON_SIZE - 4;
        float buttonY = notificationY + 4;
        
        return clickX >= buttonX && clickX <= buttonX + DISMISS_BUTTON_SIZE &&
               clickY >= buttonY && clickY <= buttonY + DISMISS_BUTTON_SIZE;
    }
}