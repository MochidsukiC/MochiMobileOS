package jp.moyashi.phoneos.core.controls;

import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import jp.moyashi.phoneos.core.ui.theme.ThemeContext;
import jp.moyashi.phoneos.core.ui.theme.ThemeEngine;
import processing.core.PApplet;
import processing.core.PGraphics;

import java.util.function.Consumer;

/**
 * コントロールセンター用の縦型スライダーアイテム。
 * 音量や輝度などの連続値を調整するために使用する。
 * 1x2のグリッドサイズを占有する。
 */
public class SliderItem implements IControlCenterItem {

    private final String id;
    private final String displayName;
    private final String iconSymbol; // 簡易アイコンとしての文字（例: "☀", "vol"）
    private float value; // 0.0 to 1.0
    private final Consumer<Float> onValueChanged;
    private final GridAlignment gridAlignment;
    private boolean isDragging = false;

    /**
     * SliderItemを作成する。
     * 
     * @param id 一意識別子
     * @param displayName 表示名
     * @param iconSymbol アイコンとして表示する文字列
     * @param initialValue 初期値 (0.0 - 1.0)
     * @param onValueChanged 値変更時のコールバック
     */
    public SliderItem(String id, String displayName, String iconSymbol, float initialValue, Consumer<Float> onValueChanged) {
        this(id, displayName, iconSymbol, initialValue, GridAlignment.LEFT, onValueChanged);
    }

    /**
     * SliderItemを作成する（配置指定あり）。
     *
     * @param id 一意識別子
     * @param displayName 表示名
     * @param iconSymbol アイコンとして表示する文字列
     * @param initialValue 初期値 (0.0 - 1.0)
     * @param alignment グリッド配置（LEFT/RIGHT）
     * @param onValueChanged 値変更時のコールバック
     */
    public SliderItem(String id, String displayName, String iconSymbol, float initialValue, GridAlignment alignment, Consumer<Float> onValueChanged) {
        this.id = id;
        this.displayName = displayName;
        this.iconSymbol = iconSymbol;
        this.value = Math.max(0.0f, Math.min(1.0f, initialValue));
        this.gridAlignment = alignment;
        this.onValueChanged = onValueChanged;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return displayName + ": " + (int)(value * 100) + "%";
    }

    @Override
    public int getColumnSpan() {
        return 1; // 1列幅
    }

    @Override
    public int getRowSpan() {
        return 2; // 2行高さ（縦長）
    }

    @Override
    public GridAlignment getGridAlignment() {
        return gridAlignment;
    }

    @Override
    public boolean isDraggable() {
        return true;
    }

    /**
     * 値を更新する（外部からの変更用）
     * @param newValue 新しい値 (0.0 - 1.0)
     */
    public void setValue(float newValue) {
        this.value = Math.max(0.0f, Math.min(1.0f, newValue));
    }

    @Override
    public void draw(PGraphics g, float x, float y, float w, float h) {
        captureLayout(y, h);
        ThemeEngine theme = ThemeContext.getTheme();
        
        // 背景（トラック）
        // ControlCenterManagerですでに背景が描画されているが、
        // スライダーとしての視覚的区別のために少し暗くする
        g.noStroke();
        g.fill(0, 0, 0, 50);
        g.rect(x, y, w, h, theme.radiusLg());

        // フィル（値の部分）
        float fillHeight = h * value;
        float fillY = y + h - fillHeight;
        
        // 値に応じて色を変えることも可能だが、基本は白またはアクセント
        // 輝度や音量は白が一般的
        g.fill(255, 255, 255, 255);
        
        // 下部の角丸と上部の角丸を適切に処理
        // 値が小さいときは下部のみ、大きいときは全体
        // Processingのrect(x,y,w,h, tl, tr, br, bl)を使用
        // 完全に満たされている場合は上下とも角丸
        // 途中までの場合は上部は直角...だと見た目が悪いので、クリッピング的に描画するか、
        // 単純に下から描画する
        
        // クリップ機能を使って角丸の中に収めるのがベストだが、
        // ここではシンプルに下揃えの矩形を描画
        // 角丸の処理: 下部は常に角丸。上部は100%に近い時のみ角丸...
        // というのは複雑なので、全体を角丸矩形でクリップして描画するアプローチをとる
        
        // PGraphicsのステート保存
        // g.pushStyle(); // 親で管理されているため不要な場合もあるが念のため
        
        // 簡易実装: 下から生えるバー
        // 背景の上に描画。
        // 下部の角丸を維持しつつ、高さが低い場合の見た目を考慮
        
        int radius = theme.radiusLg();
        
        if (value > 0.01f) {
            // 値のバー
            g.fill(255, 255, 255, 240);
            
            // 下部の角丸は親コンテナに合わせる。上部はフラット（ただし100%付近は角丸）
            // Processingのrectで各角の半径を指定
            // tl, tr, br, bl
            float topRadius = (value > 0.95f) ? radius : 0;
            g.rect(x, fillY, w, fillHeight, topRadius, topRadius, radius, radius);
        }

        // アイコン（下部中央）
        g.fill(isDragging || value > 0.5f ? 50 : 200); // 背景が白くなったらアイコンを暗くする
        g.textAlign(PApplet.CENTER, PApplet.BOTTOM);
        g.textSize(24);
        
        // アイコンフォントがないので文字で代用しているが、
        // 実際には画像やシェイプ描画が望ましい
        g.text(iconSymbol, x + w / 2, y + h - 15);
    }

    @Override
    public void draw(PApplet p, float x, float y, float w, float h) {
        // PApplet版（PGraphics版と同じロジック）
        // 簡略化のため、PGraphics版への委譲はできない（型が違う）のでコピー
        // ただしThemeContextは共通
        captureLayout(y, h);
        
        ThemeEngine theme = ThemeContext.getTheme();
        int radius = theme.radiusLg();

        // 背景
        p.noStroke();
        p.fill(0, 0, 0, 50);
        p.rect(x, y, w, h, radius);

        // フィル
        float fillHeight = h * value;
        float fillY = y + h - fillHeight;

        if (value > 0.01f) {
            p.fill(255, 255, 255, 240);
            float topRadius = (value > 0.95f) ? radius : 0;
            p.rect(x, fillY, w, fillHeight, topRadius, topRadius, radius, radius);
        }

        // アイコン
        p.fill(isDragging || value > 0.5f ? 50 : 200);
        p.textAlign(PApplet.CENTER, PApplet.BOTTOM);
        p.textSize(24);
        p.text(iconSymbol, x + w / 2, y + h - 15);
    }

    @Override
    public boolean onGesture(GestureEvent event) {
        float x = event.getCurrentX();
        float y = event.getCurrentY();
        
        // アイテムの相対座標等はControlCenterManager側で判定済みで
        // ここにはイベントだけが来るが、座標はスクリーン絶対座標。
        // ControlCenterManagerはアイテムの境界判定をしてから呼び出すわけではない（DRAGの場合）。
        // ただし、TAPの場合は判定済み。
        // DRAG_MOVEの場合はManager側で「現在ドラッグ中のアイテム」にイベントを送る必要がある。
        
        // ここでは座標計算のために、描画時の領域を知る必要があるが、
        // IControlCenterItemの設計上、draw時に座標が渡されるだけで、アイテム自体は位置を知らない。
        // 正確なインタラクションのためには、ジェスチャーイベントに「アイテム内相対座標」が含まれるか、
        // アイテムが自分の位置を記憶する必要がある。
        
        // 簡易的な解決策: 
        // ControlCenterManagerを修正し、onGesture呼び出し時に
        // アイテムの境界情報（または相対座標に変換したイベント）を渡すのがベストだが、
        // インターフェース変更の影響が大きい。
        
        // 現状のControlCenterManagerの実装を見ると、
        // handleControlCenterClickではアイテムの境界判定をしてからonGestureを呼んでいる。
        // しかし、DRAG_MOVEはまだ実装されていない（Manager側でスクロールしてしまう）。
        
        // したがって、Manager側で「ドラッグ開始時のアイテム」を特定し、
        // そのアイテムの領域情報を使って値を計算する必要がある。
        
        // ここでは、「イベントのY座標」と「アイテムのY座標・高さ」が必要。
        // しかし、このメソッドにはイベントしか渡されない。
        
        // 解決策: 
        // SliderItemはステートレス（位置に関して）であるべきだが、
        // インタラクションには位置が必要。
        // draw()が毎フレーム呼ばれることを利用し、最新の描画位置を一時的にキャッシュする、
        // あるいはManagerが計算して正規化された値(0.0-1.0)をセットするメソッドを用意する、などがある。
        
        // 最も堅実なのは、ControlCenterManagerがドラッグ処理を行う際に、
        // アイテムの領域 (itemY, itemH) を使ってタッチ位置の割合を計算し、
        // アイテムに対して `onDrag(float progress)` のようなメソッドを呼ぶことだが、
        // インターフェース変更を避けるため、既存の `onGesture` を使う。
        
        // ControlCenterManagerを修正して、onGestureを呼ぶ前に
        // Eventオブジェクトを相対座標に書き換えるか、
        // SliderItem側で自分の位置を知る方法を用意するか。
        
        // 今回は「draw()で位置を記憶する」方式を採用する。
        // シングルスレッド描画・処理モデルなので、draw() -> onGesture() の順序であれば概ね動作する。
        // ただし、ControlCenterManagerの実装変更が必要。
        
        switch (event.getType()) {
            case TAP:
            case DRAG_START:
            case DRAG_MOVE:
                updateValueFromGesture(event.getCurrentY());
                isDragging = true;
                return true;
            case DRAG_END:
                isDragging = false;
                return true;
            default:
                return false;
        }
    }
    
    // 最後に描画された位置を記憶（インタラクション用）
    private float lastY, lastH;
    
    // drawメソッドで位置を更新
    private void updateLayout(float y, float h) {
        this.lastY = y;
        this.lastH = h;
    }
    
    // drawメソッドをオーバーライドして位置をフック
    // (Processingのオーバーロード問題があるため、両方のdrawで呼ぶ)
    
    private void updateValueFromGesture(float touchY) {
        if (lastH <= 0) return;
        
        // Y座標は下に行くほど大きい。
        // スライダーは下が0、上が1。
        // touchYが (lastY + lastH) に近いほど0。
        // touchYが lastY に近いほど1。
        
        float relativeY = touchY - lastY;
        float ratio = 1.0f - (relativeY / lastH); // 反転
        
        // クランプ
        float newValue = Math.max(0.0f, Math.min(1.0f, ratio));
        
        if (Math.abs(this.value - newValue) > 0.001f) {
            this.value = newValue;
            if (onValueChanged != null) {
                onValueChanged.accept(this.value);
            }
        }
    }
    
    // 位置情報を取得するためのラップメソッド
    private void captureLayout(float y, float h) {
        this.lastY = y;
        this.lastH = h;
    }
    
    // drawメソッドの先頭でcaptureLayoutを呼ぶように、既存のdrawメソッドに追加コードは不要。
    // 上記のdrawメソッド内に `captureLayout(y, h);` を追加する必要がある。
    // コード生成時に追加する。
}
