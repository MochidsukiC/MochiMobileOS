package jp.moyashi.phoneos.core.controls;

import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import jp.moyashi.phoneos.core.media.*;
import jp.moyashi.phoneos.core.ui.theme.ThemeContext;
import jp.moyashi.phoneos.core.ui.theme.ThemeEngine;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;

/**
 * コントロールセンター用のメディアプレーヤーウィジェット（刷新版）。
 * アルバムアートワークを背景に使用した、没入感のあるモダンなデザイン。
 */
public class NowPlayingItem implements IControlCenterItem {

    private static final String ID = "now_playing";
    private static final String DISPLAY_NAME = "Media";

    private final MediaSessionManager sessionManager;

    // タッチ判定領域（動的に更新）
    private float itemX, itemY, itemW, itemH;
    private float playBtnX, playBtnY, playBtnR;
    private float prevBtnX, prevBtnY, prevBtnW, prevBtnH;
    private float nextBtnX, nextBtnY, nextBtnW, nextBtnH;

    public NowPlayingItem(MediaSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public void draw(PGraphics g, float x, float y, float w, float h) {
        this.itemX = x;
        this.itemY = y;
        this.itemW = w;
        this.itemH = h;

        MediaController controller = sessionManager.getActiveController();
        MediaMetadata metadata = (controller != null) ? controller.getMetadata() : null;
        PlaybackState state = (controller != null) ? controller.getPlaybackState() : PlaybackState.STOPPED;

        // --- 1. 背景 & アートワーク ---
        drawBackground(g, x, y, w, h, metadata);

        // --- 2. 情報テキスト (曲名/アーティスト) ---
        drawMetadata(g, x, y, w, h, metadata);

        // --- 3. コントロールボタン ---
        drawControls(g, x, y, w, h, state);

        // --- 4. プログレスバー ---
        drawProgressBar(g, x, y, w, h, controller);
    }

    @Override
    public void draw(PApplet p, float x, float y, float w, float h) {
        draw(p.g, x, y, w, h);
    }

    private void drawBackground(PGraphics g, float x, float y, float w, float h, MediaMetadata metadata) {
        ThemeEngine theme = ThemeContext.getTheme();
        float radius = theme != null ? theme.radiusMd() : 16;
        int surfaceCol = theme != null ? theme.colorSurface() : 0xFF202020;

        g.pushStyle();
        
        // クリップ用マスク設定 (角丸)
        // Note: Processingのclip()は複雑なので、ここでは背景を描画し、
        // アートワークがある場合はその上に描画して、四隅をマスクする擬似的な手法をとるか、
        // 単純に角丸矩形の中に画像を描画する。
        // ここではシンプルに「背景色で塗りつぶし」 -> 「画像をアスペクト比維持で中央トリミング描画」 -> 「グラデーション」とする。

        // ベース背景
        g.noStroke();
        g.fill((surfaceCol >> 16) & 0xFF, (surfaceCol >> 8) & 0xFF, surfaceCol & 0xFF);
        g.rect(x, y, w, h, radius);

        // アートワーク（存在する場合）
        if (metadata != null && metadata.getArtwork() != null) {
            PImage art = metadata.getArtwork();
            // アートワークを暗くして背景として使用
            // 描画モード設定ができないため、画像の上に半透明の黒を重ねる方式を採用
            
            // 画像の描画（角丸の内側に収めるのは難しいので、パディングを少し入れるか、
            // あるいは四角く描画してから枠を描く。今回は「カード全体がアートワーク」を目指すため、
            // 簡易的に矩形描画する。角のはみ出しは許容するか、OS側のステンシルバッファに依存する。
            // ユーザー体験向上のため、少し内側に描画して角丸枠でクリップされているように見せる。
            
            float imgSize = Math.max(w, h);
            g.imageMode(PConstants.CENTER);
            // 画像を少し拡大して配置（隙間をなくす）
            // 角丸の処理ができないので、ここでは「インナーパディングなし」で描画し、
            // 四隅に背景色のコーナーパーツを被せることで角丸を擬似再現するのが高品質だが、
            // 重たいので、シンプルに rect の texture として描画する。
            
            g.fill(255);
            g.noStroke();
            g.textureMode(PConstants.NORMAL);
            g.beginShape();
            g.texture(art);
            // 左上
            g.vertex(x, y, 0, 0);
            // 右上
            g.vertex(x + w, y, 1, 0);
            // 右下
            g.vertex(x + w, y + h, 1, 1);
            // 左下
            g.vertex(x, y + h, 0, 1);
            g.endShape(PConstants.CLOSE);
            
            // ダークオーバーレイ (グラデーション)
            // 上部は少し明るく、下部は暗くしてテキストを読みやすく
            g.beginShape();
            g.fill(0, 0, 0, 100); // Top: Semi-transparent black
            g.vertex(x, y);
            g.vertex(x + w, y);
            g.fill(0, 0, 0, 220); // Bottom: Darker black
            g.vertex(x + w, y + h);
            g.vertex(x, y + h);
            g.endShape(PConstants.CLOSE);
            
        } else {
            // アートワークなし：幾何学模様または単色グラデーション
            g.beginShape();
            g.fill(40, 40, 50); // Top
            g.vertex(x, y);
            g.vertex(x + w, y);
            g.fill(20, 20, 25); // Bottom
            g.vertex(x + w, y + h);
            g.vertex(x, y + h);
            g.endShape(PConstants.CLOSE);
            
            // 音符アイコン
            g.textAlign(PConstants.CENTER, PConstants.CENTER);
            g.fill(255, 30);
            g.textSize(h * 0.4f);
            g.text("♫", x + w/2, y + h/2);
        }

        // 枠線（ボーダー）
        g.noFill();
        g.stroke(255, 30);
        g.strokeWeight(1);
        g.rect(x, y, w, h, radius);
        
        g.imageMode(PConstants.CORNER); // Reset
        g.popStyle();
    }

    private void drawMetadata(PGraphics g, float x, float y, float w, float h, MediaMetadata metadata) {
        g.pushStyle();
        
        String title = (metadata != null && metadata.getTitle() != null) ? metadata.getTitle() : "Not Playing";
        String artist = (metadata != null && metadata.getArtist() != null) ? metadata.getArtist() : "Select media";

        // レイアウト: 左下（コントロールの上）
        float padding = 15;
        float controlsHeight = 50; // 下部のコントロールエリアの高さ
        float textAreaBottom = y + h - controlsHeight;
        
        // 曲名 (Bold, Large)
        g.fill(255);
        g.textAlign(PConstants.LEFT, PConstants.BOTTOM);
        g.textSize(16);
        // はみ出し処理付き描画
        String truncTitle = truncateText(g, title, w - padding * 2);
        g.text(truncTitle, x + padding, textAreaBottom - 20);

        // アーティスト名 (Regular, Small, Grayish)
        g.fill(200);
        g.textSize(12);
        String truncArtist = truncateText(g, artist, w - padding * 2);
        g.text(truncArtist, x + padding, textAreaBottom - 5);

        g.popStyle();
    }

    private void drawControls(PGraphics g, float x, float y, float w, float h, PlaybackState state) {
        g.pushStyle();
        
        float padding = 15;
        float bottomY = y + h - padding;
        
        // メインの再生ボタン（右下）
        float playSize = 44;
        this.playBtnX = x + w - padding - playSize;
        this.playBtnY = bottomY - playSize;
        this.playBtnR = playSize / 2;

        // 再生ボタン背景 (Circle)
        // テーマカラーを使用せず、白背景＋黒アイコンでモダンに
        g.noStroke();
        g.fill(255);
        g.ellipse(playBtnX + playBtnR, playBtnY + playBtnR, playSize, playSize);

        // 再生アイコン
        g.fill(0);
        float iconSize = playSize * 0.35f;
        float cx = playBtnX + playBtnR;
        float cy = playBtnY + playBtnR;
        
        if (state == PlaybackState.PLAYING) {
            // Pause (||)
            float barW = iconSize * 0.25f;
            float barH = iconSize;
            g.rect(cx - barW * 1.6f, cy - barH / 2, barW, barH);
            g.rect(cx + barW * 0.6f, cy - barH / 2, barW, barH);
        } else {
            // Play (▶)
            // 少し右にずらして視覚的中心を合わせる
            float ox = iconSize * 0.1f; 
            g.triangle(
                cx - iconSize * 0.4f + ox, cy - iconSize * 0.5f,
                cx - iconSize * 0.4f + ox, cy + iconSize * 0.5f,
                cx + iconSize * 0.6f + ox, cy
            );
        }

        // 前へ/次へボタン（再生ボタンの左側に配置）
        float subBtnSize = 30;
        float gap = 15;
        
        // 次へ (Skip Next)
        this.nextBtnX = playBtnX - gap - subBtnSize;
        this.nextBtnY = playBtnY + (playSize - subBtnSize) / 2;
        this.nextBtnW = subBtnSize;
        this.nextBtnH = subBtnSize;
        
        drawSkipIcon(g, nextBtnX + subBtnSize/2, nextBtnY + subBtnSize/2, subBtnSize, true);

        // 前へ (Skip Prev)
        this.prevBtnX = nextBtnX - gap - subBtnSize;
        this.prevBtnY = nextBtnY;
        this.prevBtnW = subBtnSize;
        this.prevBtnH = subBtnSize;
        
        drawSkipIcon(g, prevBtnX + subBtnSize/2, prevBtnY + subBtnSize/2, subBtnSize, false);

        g.popStyle();
    }

    private void drawSkipIcon(PGraphics g, float cx, float cy, float size, boolean isNext) {
        g.fill(255);
        g.noStroke();
        float iconSize = size * 0.5f;
        
        if (isNext) {
            // |>|
            g.triangle(
                cx - iconSize * 0.3f, cy - iconSize * 0.5f,
                cx - iconSize * 0.3f, cy + iconSize * 0.5f,
                cx + iconSize * 0.3f, cy
            );
            g.rect(cx + iconSize * 0.3f, cy - iconSize * 0.5f, iconSize * 0.15f, iconSize);
        } else {
            // |<|
            g.rect(cx - iconSize * 0.45f, cy - iconSize * 0.5f, iconSize * 0.15f, iconSize);
            g.triangle(
                cx + iconSize * 0.3f, cy - iconSize * 0.5f,
                cx + iconSize * 0.3f, cy + iconSize * 0.5f,
                cx - iconSize * 0.3f, cy
            );
        }
    }

    private void drawProgressBar(PGraphics g, float x, float y, float w, float h, MediaController controller) {
        if (controller == null) return;
        
        // メタデータ取得（nullの場合はデフォルト値で処理）
        MediaMetadata metadata = controller.getMetadata();
        long duration = (metadata != null) ? metadata.getDurationMs() : 0;
        long current = controller.getCurrentPosition();
        
        // 進捗率計算（durationが0以下の場合は0とする）
        float progress = (duration > 0) ? Math.min(1.0f, Math.max(0.0f, (float)current / duration)) : 0.0f;

        g.pushStyle();
        
        // カード下端に配置
        float barH = 4;
        float barY = y + h - barH;
        
        // 背景トラック（常に描画）
        g.noStroke();
        g.fill(255, 50);
        g.rect(x, barY, w, barH);
        
        // 進捗バー（進捗がある場合のみ描画）
        if (progress > 0) {
            ThemeEngine theme = ThemeContext.getTheme();
            int primary = theme != null ? theme.colorPrimary() : 0xFF4A90E2;
            g.fill((primary >> 16) & 0xFF, (primary >> 8) & 0xFF, primary & 0xFF);
            g.rect(x, barY, w * progress, barH);
        }
        
        g.popStyle();
    }

    private String truncateText(PGraphics g, String text, float maxWidth) {
        if (text == null || text.isEmpty()) return "";
        if (g.textWidth(text) <= maxWidth) return text;
        
        String ellipsis = "...";
        float ellW = g.textWidth(ellipsis);
        
        for (int i = text.length() - 1; i > 0; i--) {
            String sub = text.substring(0, i);
            if (g.textWidth(sub) + ellW <= maxWidth) {
                return sub + ellipsis;
            }
        }
        return text.substring(0, 1) + ellipsis;
    }

    @Override
    public boolean onGesture(GestureEvent event) {
        if (event.getType() != GestureType.TAP) return false;
        
        MediaController controller = sessionManager.getActiveController();
        if (controller == null) return false; // コントローラーがない場合は反応しない

        float tx = event.getCurrentX();
        float ty = event.getCurrentY();
        
        // ヒットテスト
        
        // 再生ボタン (円形判定)
        float dx = tx - (playBtnX + playBtnR);
        float dy = ty - (playBtnY + playBtnR);
        if (dx*dx + dy*dy <= playBtnR * playBtnR * 1.5) { // 少し判定を甘く(1.5倍)
            System.out.println("NowPlaying: Toggle Play/Pause");
            controller.togglePlayPause();
            return true;
        }
        
        // Next
        if (tx >= nextBtnX - 10 && tx <= nextBtnX + nextBtnW + 10 &&
            ty >= nextBtnY - 10 && ty <= nextBtnY + nextBtnH + 10) {
            System.out.println("NowPlaying: Skip Next");
            controller.skipToNext();
            return true;
        }
        
        // Prev
        if (tx >= prevBtnX - 10 && tx <= prevBtnX + prevBtnW + 10 &&
            ty >= prevBtnY - 10 && ty <= prevBtnY + prevBtnH + 10) {
            System.out.println("NowPlaying: Skip Prev");
            controller.skipToPrevious();
            return true;
        }
        
        // 全体タップ（アプリを開くなどの動作に割り当て可能だが、今は消費のみ）
        if (tx >= itemX && tx <= itemX + itemW && ty >= itemY && ty <= itemY + itemH) {
            return true;
        }

        return false;
    }

    @Override
    public String getId() { return ID; }

    @Override
    public String getDisplayName() { return DISPLAY_NAME; }

    @Override
    public String getDescription() { return "Media Player Widget"; }

    @Override
    public boolean isVisible() { return sessionManager.hasActiveSession(); }

    @Override
    public boolean isEnabled() { return sessionManager.hasActiveSession(); }

    @Override
    public int getColumnSpan() { return 2; }

    @Override
    public int getRowSpan() { return 2; }

    @Override
    public GridAlignment getGridAlignment() { return GridAlignment.RIGHT; }
}