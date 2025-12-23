# フォントシステム

**フォントシステム** (✅ 実装完了、テスト済み - 2025-10-14):
- **目的**: 日本語フォントの文字化けを修正し、全環境（standalone、Forge、Windows、Mac、Linux）で統一されたフォント表示を実現
- **実装内容**:
  - Noto Sans JP TTFファイル（Variable font、9.2MB）をリソースとして埋め込み（`core/src/main/resources/fonts/NotoSansJP-Regular.ttf`）
  - `Kernel.loadJapaneseFont()`メソッドを実装（Kernel.java:714-782）：
    - リソースからTTFファイルを読み込み
    - Java AWT Fontを作成してGraphicsEnvironmentに登録
    - **Processing PFontとして作成（サイズ16、スムージング有効）**
    - **重要**: `PApplet.createFont()`ではなく、`new PFont(Font, boolean)`コンストラクタを直接使用
    - これにより、PApplet.setup()が呼ばれていない状態でも動作可能（Forge環境対応）
  - `Kernel.setup()`での初期化（Kernel.java:85-92）
  - `Kernel.getJapaneseFont()`でアプリケーションから取得可能
- **使用箇所**: CalculatorScreen、SettingsScreen、LockScreen、HomeScreen、NotificationManager、SimpleNotification等、16箇所
- **クロスプラットフォーム対応**: JARリソースからの読み込みにより、OS依存なし
- **Forge環境での問題と解決**:
  - **問題**: Forge環境で日本語フォントが文字化けしていた
  - **根本原因**: `PApplet.createFont()`は内部でPAppletの"ready"フラグをチェックし、`setup()`が呼ばれていない場合にエラーを返す。Forge環境では`initializeForMinecraft()`でヘッドレスPAppletを作成するが、`setup()`は呼ばれない。
  - **試行した失敗策**: リフレクションを使ってPAppletの"ready"フラグを設定しようとしたが、ビルドキャッシュの問題で古いコードが実行され続けた。
  - **最終解決策**: `PApplet.createFont()`を使わず、`new PFont(derivedFont, true)`コンストラクタで直接PFontを構築することで、PAppletのライフサイクルに依存しない実装に変更。
  - **修正箇所**: Kernel.java:757-767（PFont直接構築）、Kernel.java:557-559（不要なリフレクションコードを削除）
- **検証結果** (✅ 成功 - 2025-10-14):
  - ✅ Standalone環境: 正常動作
  - ✅ Forge環境: 正常動作（文字化け解消）
  - ✅ LoggerServiceによるログ出力: VFSログで初期化プロセスを確認可能

## 絵文字サポート (✅ 実装完了 - 2025-12-23)

**目的**: Unicode絵文字の文字化け（□表示）を修正し、モノクロ絵文字を正しく表示する

### 実装内容

#### 新規ファイル
1. **`core/src/main/resources/fonts/NotoEmoji-Regular.ttf`** (約2MB)
   - Google Noto Emoji（モノクロ版）を埋め込み
   - 最新Unicode絵文字規格に対応

2. **`core/src/main/java/jp/moyashi/phoneos/core/util/EmojiUtil.java`**
   - 絵文字判定ユーティリティ
   - `isEmoji(int codePoint)` - Unicodeコードポイントで絵文字判定
   - `containsEmoji(String text)` - テキストに絵文字が含まれるか判定
   - `segmentText(String text)` - テキストを絵文字/非絵文字セグメントに分割
   - 対応Unicode範囲: Emoticons, Misc Symbols/Pictographs, Transport/Map, Supplemental Symbols, Dingbats等

3. **`core/src/main/java/jp/moyashi/phoneos/core/render/TextRenderer.java`**
   - フォントフォールバック対応テキストレンダラー
   - `drawText(g, text, x, y, textSize)` - 絵文字/非絵文字を適切なフォントで描画
   - `getTextWidth(g, text, textSize)` - 混合テキストの幅を正確に計算
   - `getCharacterWidths(...)` - カーソル位置計算用の累積幅配列取得

4. **`core/src/main/java/jp/moyashi/phoneos/core/render/TextRendererContext.java`**
   - UIコンポーネントからTextRendererにアクセスするための静的コンテキスト

#### 変更ファイル
- **ResourceManager.java**: `loadEmojiFont()`, `getEmojiFont()` 追加
- **Kernel.java**: `emojiFont`, `textRenderer` フィールド追加、`getTextRenderer()` 追加
- **Label.java**: 絵文字含有時にTextRendererで描画
- **TextField.java**: 絵文字対応（テキスト描画、選択、カーソル位置）
- **BaseTextInput.java**: `getTextWidth()` で絵文字対応
- **Button.java**: ボタンテキストの絵文字対応
- **Checkbox.java**: ラベルの絵文字対応
- **ToggleItem.java**: コントロールセンターのアイコン絵文字対応
- **SliderItem.java**: コントロールセンターのスライダーアイコン絵文字対応

### アーキテクチャ
```
テキスト描画リクエスト
    ↓
EmojiUtil.containsEmoji(text)
    ↓
[絵文字なし] → 従来の g.text() で高速描画
[絵文字あり] → TextRenderer.drawText()
                 ↓
              EmojiUtil.segmentText(text)
                 ↓
              セグメントごとにフォント切替
              (日本語フォント / 絵文字フォント)
```

### 最適化
- **Fast Path**: 絵文字を含まないテキストは `containsEmoji()` チェック後すぐに従来の描画パスを使用
- **セグメントベース**: 文字単位ではなくセグメント単位でフォント切替（パフォーマンス向上）
