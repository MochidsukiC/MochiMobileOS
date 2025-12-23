package jp.moyashi.phoneos.core.notification;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PFont;
import processing.core.PImage;
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

    /** アプリアイコン */
    private PImage icon;

    /** クリック時のアクション */
    private Runnable clickAction;

    /** Kernelへの参照 */
    private jp.moyashi.phoneos.core.Kernel kernel;
    
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
    
    /**
     * 通知を描画する（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     */
    public void draw(PGraphics g, float x, float y, float width, float height) {
        if (isDismissed) return;

        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int surface = theme != null ? theme.colorSurface() : 0xFF3C3C46;
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFFFFFFFF;
        int onSurfaceSec = theme != null ? theme.colorOnSurfaceSecondary() : 0xFFB4B4B4;
        int primary = theme != null ? theme.colorPrimary() : 0xFF4682B4;

        // 背景（未読は少し濃く）
        int a = isRead ? 120 : 180;
        g.fill((surface>>16)&0xFF, (surface>>8)&0xFF, surface&0xFF, a);
        g.noStroke();
        g.rect(x, y, width, height, theme != null ? theme.radiusSm() : 8);

        // 未読インジケーター
        if (!isRead) {
            g.fill((primary>>16)&0xFF, (primary>>8)&0xFF, primary&0xFF);
            g.rect(x + 4, y + 4, 3, height - 8, 1);
        }

        // 優先度インジケーター
        if (priority == 2) {
            g.fill(220, 80, 80); // 高優先度：赤
            g.rect(x + width - 4, y + 4, 3, height - 8, 1);
        } else if (priority == 0) {
            g.fill(120, 120, 120); // 低優先度：灰色
            g.rect(x + width - 4, y + 4, 3, height - 8, 1);
        }

        // アイコン描画
        float iconSize = 32;
        float iconMargin = 8;
        float iconX = x + (isRead ? 12 : 16);
        float iconY = y + (height - iconSize) / 2;

        if (icon != null) {
            g.image(icon, iconX, iconY, iconSize, iconSize);
        } else {
            // デフォルトアイコン（ベルマーク）
            g.fill((onSurfaceSec >> 16) & 0xFF, (onSurfaceSec >> 8) & 0xFF, onSurfaceSec & 0xFF, 150);
            g.ellipse(iconX + iconSize / 2, iconY + iconSize / 2, iconSize - 4, iconSize - 4);
            g.fill((onSurface >> 16) & 0xFF, (onSurface >> 8) & 0xFF, onSurface & 0xFF);
            g.textAlign(PApplet.CENTER, PApplet.CENTER);
            g.textSize(16);
            // ベルの形を描画
            float cx = iconX + iconSize / 2;
            float cy = iconY + iconSize / 2;
            g.noStroke();
            g.fill((onSurface >> 16) & 0xFF, (onSurface >> 8) & 0xFF, onSurface & 0xFF);
            g.arc(cx, cy - 2, 14, 14, (float)Math.PI, (float)Math.PI * 2);
            g.rect(cx - 7, cy - 2, 14, 8);
            g.ellipse(cx, cy + 8, 6, 4);
        }

        // テキスト描画エリア（アイコン分右にずらす）
        float textX = iconX + iconSize + iconMargin;
        float textWidth = width - (textX - x) - DISMISS_BUTTON_SIZE - 8;

        // 送信者とタイムスタンプ
        g.fill((onSurfaceSec>>16)&0xFF, (onSurfaceSec>>8)&0xFF, onSurfaceSec&0xFF);
        g.textAlign(PApplet.LEFT, PApplet.TOP);
        g.textSize(10);
        String timeStr = TIME_FORMAT.format(new Date(timestamp));
        g.text(sender + " • " + timeStr, textX, y + 6);

        // タイトル
        g.fill((onSurface>>16)&0xFF, (onSurface>>8)&0xFF, onSurface&0xFF);
        g.textSize(12);
        g.text(truncateText(g, title, textWidth), textX, y + 20);

        // 内容
        int body = onSurfaceSec;
        g.fill((body>>16)&0xFF, (body>>8)&0xFF, body&0xFF);
        g.textSize(10);
        String wrappedContent = wrapText(g, content, textWidth, 2);
        g.text(wrappedContent, textX, y + 36);

        // 削除ボタン
        drawDismissButton(g, x + width - DISMISS_BUTTON_SIZE - 4, y + 4);
    }

    @Override
    public void draw(PApplet p, float x, float y, float width, float height) {
        if (isDismissed) return;
        
        p.pushStyle();
        
        // 日本語フォントを設定
        if (kernel != null) {
            PFont japaneseFont = kernel.getJapaneseFont();
            if (japaneseFont != null) {
                p.textFont(japaneseFont);
            }
        }
        
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

        // アイコン描画
        float iconSize = 32;
        float iconMargin = 8;
        float iconX = x + (isRead ? 12 : 16);
        float iconY = y + (height - iconSize) / 2;

        if (icon != null) {
            p.image(icon, iconX, iconY, iconSize, iconSize);
        } else {
            // デフォルトアイコン（ベルマーク）
            p.fill(150, 150, 150, 150);
            p.ellipse(iconX + iconSize / 2, iconY + iconSize / 2, iconSize - 4, iconSize - 4);
            // ベルの形を描画
            float cx = iconX + iconSize / 2;
            float cy = iconY + iconSize / 2;
            p.noStroke();
            p.fill(220, 220, 220);
            p.arc(cx, cy - 2, 14, 14, (float)Math.PI, (float)Math.PI * 2);
            p.rect(cx - 7, cy - 2, 14, 8);
            p.ellipse(cx, cy + 8, 6, 4);
        }

        // テキスト描画エリア（アイコン分右にずらす）
        float textX = iconX + iconSize + iconMargin;
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
     * 削除ボタンを描画する（PGraphics版）。
     */
    private void drawDismissButton(PGraphics g, float x, float y) {
        var theme = jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme();
        int border = theme != null ? theme.colorBorder() : 0xFF999999;
        int onSurface = theme != null ? theme.colorOnSurface() : 0xFFE0E0E0;
        g.fill((border>>16)&0xFF, (border>>8)&0xFF, border&0xFF, 80);
        g.rect(x, y, DISMISS_BUTTON_SIZE, DISMISS_BUTTON_SIZE, theme != null ? theme.radiusSm() : 4);

        // X マーク
        g.stroke((onSurface>>16)&0xFF, (onSurface>>8)&0xFF, onSurface&0xFF);
        g.strokeWeight(1.5f);
        float margin = 6;
        g.line(x + margin, y + margin,
               x + DISMISS_BUTTON_SIZE - margin, y + DISMISS_BUTTON_SIZE - margin);
        g.line(x + DISMISS_BUTTON_SIZE - margin, y + margin,
               x + margin, y + DISMISS_BUTTON_SIZE - margin);
        g.noStroke();
    }

    /**
     * 削除ボタンを描画する（PApplet版）。
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
     * テキストを指定幅に収まるように切り詰める（PGraphics版）。
     */
    private String truncateText(PGraphics g, String text, float maxWidth) {
        if (text == null || text.isEmpty()) return "";

        String suffix = "...";
        if (g.textWidth(text) <= maxWidth) {
            return text;
        }

        for (int i = text.length() - 1; i > 0; i--) {
            String truncated = text.substring(0, i) + suffix;
            if (g.textWidth(truncated) <= maxWidth) {
                return truncated;
            }
        }
        return suffix;
    }

    /**
     * テキストを指定幅に収まるように切り詰める（PApplet版）。
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
     * テキストを指定幅と行数に収まるように改行する（PGraphics版）。
     */
    private String wrapText(PGraphics g, String text, float maxWidth, int maxLines) {
        if (text == null || text.isEmpty()) return "";

        String[] words = text.split(" ");
        StringBuilder result = new StringBuilder();
        StringBuilder currentLine = new StringBuilder();
        int lineCount = 0;

        for (String word : words) {
            String testLine = currentLine.length() > 0 ? currentLine + " " + word : word;

            if (g.textWidth(testLine) > maxWidth) {
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
                    result.append(truncateText(g, word, maxWidth));
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

    /**
     * テキストを指定幅と行数に収まるように改行する（PApplet版）。
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

        // クリックアクションがあれば実行
        if (clickAction != null) {
            try {
                clickAction.run();
            } catch (Exception e) {
                System.err.println("SimpleNotification: Error executing click action: " + e.getMessage());
            }
        }

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

    /**
     * Kernelへの参照を設定する。
     *
     * @param kernel Kernelインスタンス
     */
    public void setKernel(jp.moyashi.phoneos.core.Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * アプリアイコンを設定する。
     *
     * @param icon アプリアイコン
     */
    public void setIcon(PImage icon) {
        this.icon = icon;
    }

    /**
     * アプリアイコンを取得する。
     *
     * @return アプリアイコン
     */
    @Override
    public PImage getIcon() {
        return icon;
    }

    /**
     * クリック時のアクションを設定する。
     *
     * @param clickAction クリック時に実行するアクション
     */
    public void setClickAction(Runnable clickAction) {
        this.clickAction = clickAction;
    }

    /**
     * クリック時のアクションを取得する。
     *
     * @return クリック時に実行するアクション
     */
    @Override
    public Runnable getClickAction() {
        return clickAction;
    }
}
