# GUIコンポーネントライブラリ

再利用可能なUIコンポーネントシステムを実装し、アプリ開発の生産性を大幅に向上させました。

**実装場所**: `core/src/main/java/jp/moyashi/phoneos/core/ui/components/`

**基盤インターフェース** (5種類):
- `UIComponent`: 全コンポーネントの基底（描画、更新、座標・サイズ管理、表示/有効状態）
- `Clickable`: クリック可能なコンポーネント（ホバー、プレス、クリックイベント）
- `Focusable`: フォーカス可能なコンポーネント（フォーカス管理、キーボード入力）
- `Scrollable`: スクロール可能なコンポーネント（スクロールオフセット、スクロールバー）
- `Container`: 子要素を持つコンポーネント（子要素管理、レイアウト）

**基本コンポーネント** (5種類):
- `BaseComponent`: UIComponentの基本実装（共通状態管理）
- `Button`: ボタン（テキスト、アイコン、ホバー・プレスアニメーション対応）
- `Label`: テキストラベル（アライメント、フォント、カラー設定）
- `TextField`: 単一行テキスト入力（✅ マウスクリックでカーソル位置設定、ドラッグで選択、キーボード入力、選択範囲ハイライト）
- `TextArea`: 複数行テキスト入力（✅ マウスクリックでカーソル位置設定、ドラッグで複数行選択、スクロール、矢印キー対応）

**選択系コンポーネント** (4種類):
- `Checkbox`: チェックボックス（アニメーション付き）
- `RadioButton`: ラジオボタン（アニメーション付き）
- `RadioGroup`: ラジオボタングループ管理（排他的選択）
- `Switch`: スイッチ/トグル（スムーズアニメーション）

**入力・表示系コンポーネント** (4種類):
- `Slider`: スライダー（水平/垂直、範囲指定、ドラッグ対応）
- `ProgressBar`: プログレスバー/レベルメーター（水平/垂直、パーセンテージ表示）
- `ImageView`: 画像表示（スケーリングモード: FIT/FILL/STRETCH/CENTER）
- `Divider`: 区切り線（水平/垂直、カラー・太さ設定）

**コンテナ系コンポーネント** (5種類):
- `Panel`: コンテナパネル（背景、枠線、子要素グループ化）
- `ScrollView`: スクロール可能なビュー（スクロールバー表示）
- `ListView`: リストビュー（項目選択、スクロール対応）
- `Dialog`: モーダルダイアログ（タイトル、メッセージ、OK/Cancelボタン）
- `Dropdown`: ドロップダウンメニュー（選択肢一覧表示）

**レイアウト・テーマ**:
- `LinearLayout`: 線形レイアウトマネージャー（垂直/水平配置、スペーシング）
- `Theme`: テーマシステム（カラー、フォント、サイズの統一管理、ダーク/ライトテーマ切り替え）

**特徴**:
- **PGraphics統一アーキテクチャ**: すべてのコンポーネントがPGraphicsベースで動作
- **アニメーション対応**: ホバー、プレス、選択などのスムーズなアニメーション
- **イベント駆動型**: クリック、フォーカス、変更イベントのコールバック対応
- **Android風API**: Android開発者にとって学習コストが低い設計
- **日本語フォント対応**: Kernelの日本語フォントシステムと統合可能
- **再利用性**: すべてのコンポーネントが独立して動作、組み合わせ自由

**使用例**:
```java
// ボタンの作成
Button button = new Button(50, 100, 120, 40, "クリック");
button.setOnClickListener(() -> System.out.println("ボタンがクリックされました"));

// テキストフィールドの作成
TextField textField = new TextField(50, 150, 200, 35, "入力してください");
testField.setFont(kernel.getJapaneseFont());

// スライダーの作成
Slider slider = new Slider(50, 200, 200, 0, 100, 50);
slider.setOnValueChangeListener(value -> System.out.println("値: " + value));

// パネルにコンポーネントを追加
LinearLayout panel = new LinearLayout(0, 0, 400, 600, LinearLayout.Orientation.VERTICAL);
panel.addChild(button);
panel.addChild(textField);
panel.addChild(slider);
panel.layout();
```

**今後の改善点**:
- GridLayout、FlexLayoutの実装
- コンポーネントのテーマ統合（Themeから自動的にカラー取得）
- より高度なアニメーションシステム
- アクセシビリティ対応

**既存アプリのリファクタリング** (✅ 進行中 - 2025-10-16):

GUIコンポーネントライブラリを活用して、既存アプリを新しいコンポーネントベースのアーキテクチャにリファクタリング：

1. **CalculatorScreen** (✅ 完了):
   - 手動ボタン描画 → Buttonコンポーネント使用（20個の計算機ボタン）
   - ディスプレイ表示 → Labelコンポーネント使用
   - イベントハンドラをコンポーネントのコールバックで実装
   - コード削減: 404行 → 359行（約11%削減）
   - 自動ホバー効果、クリーンなコード構造

2. **VoiceMemoScreen** (✅ 完了):
   - 手動ボタン描画 → Buttonコンポーネント使用（録音、再生、音量調整、メモの再生/削除ボタン）
   - 手動チェックボックス描画 → Checkboxコンポーネント使用（チャンネル選択：マイク、VC音声、環境音）
   - 手動レベルメーター描画 → ProgressBarコンポーネント使用（入力/出力レベルメーター）
   - 手動ラベル描画 → Labelコンポーネント使用（ヘッダー、ステータステキスト）
   - ProgressBarの値範囲を0-1に設定（音声レベル表示に最適化）
   - レベルに応じた色変更（入力レベル：緑→黄→赤、出力レベル：青）
   - イベントハンドラをコンポーネントのコールバックで実装
   - 動的ボタン生成（メモリストの各アイテムに再生/削除ボタン）

3. **TextField/TextAreaのリファクタリングと機能拡張** (✅ 完了 - 2025-10-18):
   - マウスクリックでカーソル位置設定機能を追加
   - マウスドラッグでテキスト選択機能を追加
   - 選択範囲のハイライト表示（TextFieldは単一行、TextAreaは複数行対応）
   - `getCharPositionFromClick()`メソッドで正確な文字位置計算
   - `getSelectedText()`メソッドで選択テキスト取得
   - Clickableインターフェース実装（onMousePressed/Released/Moved/Dragged）
   - 選択範囲に対するキーボード操作（バックスペース、Delete、文字入力で選択削除）
   - **Unicode対応** (✅ 2025-10-16): 全角文字（日本語、中国語など）の入力に対応
     - TextFieldとTextAreaでUnicode文字を受け入れるように修正（key >= 32 && key != 127 && key != 65535）
     - StandaloneWrapperに`keyTyped()`メソッドを追加（Processing対応）
     - ProcessingScreenに`charTyped()`メソッドを追加（Minecraft対応）
     - **二重入力問題の修正** (✅ 2025-10-19完全修正): `keyPressed()`で特殊キーのみを処理、通常文字は`keyTyped()`/`charTyped()`で処理
       - StandaloneWrapper: 制御文字・CODEDキー・特殊キー（矢印、Backspace等）のみ`keyPressed()`で処理
       - ProcessingScreen: Minecraftキーコードを変換し、特殊キーのみ`keyPressed()`で処理
       - **イベント消費の追加** (ProcessingScreen.java:659): 特殊キーを`keyPressed()`で処理した後、`return true`でイベントを消費し、`charTyped()`での二重処理を防ぐ
       - **制御文字とスペースのフィルタリング** (ProcessingScreen.java:703-706): `charTyped()`で制御文字とスペース（ASCII <= 32 または 127）をスキップし、`keyPressed()`で既に処理された文字を除外
       - **スペースキー修正** (ProcessingScreen.java:703): スペース（ASCII 32）も制御文字と同様に`charTyped()`でスキップすることで、二重入力を完全に防止
       - **矢印キー制御文字問題の修正** (✅ 2025-10-19):
         - **問題**: 矢印キーを押すと`@'%(`が入力される（キーコード35-40が文字として解釈される）
         - **原因**: `charTyped()`/`keyTyped()`で矢印キーのキーコード（35-40）が制御文字として除外されていなかった
         - **解決策**:
           - ProcessingScreen.java:710-713: `charTyped()`で35-40（End, Home, 矢印キー）を除外
           - StandaloneWrapper.java:206-209: `keyTyped()`で35-40（End, Home, 矢印キー）を除外
         - **結果**: 矢印キー、Home、Endキーが正しく特殊キーとして処理され、制御文字が入力されなくなった
       - **Ctrl+C/V/Aショートカット問題の修正** (✅ 2025-10-19):
         - **問題**: Ctrl+Vが機能しない（ペーストできない）
         - **原因**: Ctrl+Vの場合、Vキー（keyCode=86）が`keyPressed()`に転送されず、`charTyped()`には制御文字（ASCII 22）のみが渡されていた
         - **解決策**:
           - Kernel.java:1209-1220: `isShiftPressed()`と`isCtrlPressed()`のgetterを追加
           - ProcessingScreen.java:651-663: Ctrlが押されている場合（modifiers & 2）、通常文字キーも`keyPressed()`に転送
           - StandaloneWrapper.java:186-188: `kernel.isCtrlPressed()`を確認し、Ctrlが押されている場合は通常文字キーも`keyPressed()`に転送
         - **結果**: Ctrl+C/V/Aショートカットが正常に動作し、環境OSのクリップボードとの連携が機能するようになった
     - IMEを通じた日本語入力が正常に動作
   - **シフトクリック範囲選択** (✅ 2025-10-16):
     - 最初のクリックでカーソル位置を設定、シフトクリックで範囲選択
     - TextField/TextAreaに`setShiftPressed()`メソッド追加
   - **Ctrl+C/V/Aショートカット** (✅ 2025-10-16):
     - Ctrl+C: コピー（選択範囲がある場合）
     - Ctrl+V: ペースト
     - Ctrl+A: 全選択
     - TextField/TextAreaに`setCtrlPressed()`メソッド追加
     - **注意**: Ctrl+C/Vの実際のコピー・ペースト処理はNoteEditScreenで実装（ClipboardManager連携）
   - **修飾キー状態管理システム** (✅ 2025-10-17):
     - **Kernelでの状態追跡**:
       - 修飾キー（Shift/Ctrl）の状態をフィールド（`shiftPressed`, `ctrlPressed`）で管理
       - `keyPressed()`でkeyCode 16（Shift）、17（Ctrl）を検出して状態を更新
       - `keyReleased()`で状態をクリア
       - **修飾キー即時伝播** (✅ 2025-10-17): `keyPressed()`/`keyReleased()`で修飾キーの状態が変化した時点で、即座に`screenManager.setModifierKeys(shiftPressed, ctrlPressed)`を呼び出して伝播
         - これにより、Shift+クリックによるテキスト範囲選択やCtrl+C/V/Aのクリップボード操作がリアルタイムで動作
         - 以前はマウスイベント時のみ伝播していたため、修飾キーを押した状態でクリックしても機能しなかった
       - **マウスイベント前の状態伝播**: `mousePressed()`, `mouseReleased()`, `mouseDragged()`の各メソッドで、`screenManager.setModifierKeys(shiftPressed, ctrlPressed)`を呼び出してからイベント転送
     - **ScreenManager**: `setModifierKeys()`で現在のScreenに状態を伝播
     - **Screen**: `setModifierKeys()`インターフェースメソッド（デフォルト実装）
     - **NoteEditScreen**: `setModifierKeys()`を実装し、titleFieldとcontentAreaに伝播
     - **StandaloneWrapper対応**:
       - `keyPressed()`の特殊キー判定にkeyCode 16（Shift）、17（Ctrl）を追加
       - これにより、standalone環境でShift/Ctrlキーが正しくKernelに転送される
     - **ProcessingScreen対応**（Minecraft/Forge環境）:
       - Minecraftの修飾キーコード（340=Shift Left, 344=Shift Right, 341=Ctrl Left, 345=Ctrl Right）を特殊キー判定に追加
       - `convertMinecraftKeyCode()`でMinecraftキーコードをProcessingキーコードに変換（340/344→16, 341/345→17）
       - **keyReleased()メソッドの実装** (✅ 2025-10-17): ProcessingScreenに`keyReleased()`メソッドが欠けており、修飾キーのリリースイベントがKernelに届かず、Shift/Ctrlキーを離してもフラグがtrueのままになる問題を修正
       - これにより、Forge環境でShift/Ctrlキーの押下と解放が正しくKernelに転送される
     - **キーコード**: Shift=16, Ctrl=17（Processing/Java AWT標準）、Minecraft: Shift Left=340, Shift Right=344, Ctrl Left=341, Ctrl Right=345
   - **スペースキー処理の改善** (✅ 2025-10-16):
     - テキスト入力フォーカス中はスペースキーをホームボタンとして処理しない
     - `ScreenManager.hasFocusedComponent()`でフォーカス状態をチェック
     - システム優先の原則を保ちつつ、コンテキスト認識を追加
     - ESCキーなどのシステムキーは引き続き最優先
   - **パフォーマンス問題の修正** (✅ 2025-10-17):
     - **問題**: ドラッグ操作に非常に大きなラグが発生（軌跡表示が遅延）
     - **原因**: 修飾キーのデバッグログが`setModifierKeys()`メソッド内に追加されており、このメソッドが各マウスイベント（mousePressed、mouseReleased、mouseDragged）で呼び出されるため、LoggerServiceが毎回VFSに書き込みを行っていた。頻繁なディスクI/Oが原因でUIがフリーズ状態に
     - **解決策**: `ScreenManager.setModifierKeys()` (ScreenManager.java:745-754) と `NoteEditScreen.setModifierKeys()` (NoteEditScreen.java:283-294) からすべてのデバッグログを削除。実際の修飾キー伝播ロジックは保持
     - **結果**: ドラッグ操作が正常に動作し、ラグが完全に解消
   - **BaseTextInputクラスへのリファクタリング** (✅ 2025-10-18):
     - **目的**: コードの保守性向上と外部アプリ開発者による独自テキスト入力コンポーネント作成の簡易化
     - **実装内容**:
       - `BaseTextInput.java` (461行) を新規作成
         - TextField/TextAreaの共通機能を抽出（テキスト編集、選択、クリップボード、フォーカス管理、修飾キー処理）
         - 抽象メソッド: `getCharPositionFromClick()`, `drawText()`（サブクラスで実装）
         - `anchorPosition`による範囲選択、`lastGraphics`によるテキスト幅計算
       - `TextField.java`をリファクタリング: 560行 → 160行 (71%削減)
         - BaseTextInputを継承、単一行固有の機能のみ実装
       - `TextArea.java`をリファクタリング: 720行 → 365行 (49%削減)
         - BaseTextInputを継承、複数行・スクロール・折り返し機能のみ実装
     - **テキスト幅計算の改善**: `getTextWidth()`メソッドで固定近似値ではなくPGraphics.textWidth()を使用し、正確なカーソル位置決定を実現
   - **TextAreaワードラップ機能** (✅ 2025-10-18):
     - **実装内容**:
       - `wordWrap`フィールド（デフォルトtrue）と`wrapWidth`自動計算
       - `wrapLine()`メソッドで実際のテキスト幅に基づいて行を折り返し
       - `drawText()`を折り返し行のレンダリングに対応
       - `getContentHeight()`を折り返し行の高さ計算に対応
     - **カーソル描画の修正**: 折り返し行を考慮したカーソルY位置計算
     - **クリック位置計算の修正**: `getCharPositionFromClick()`を完全書き直し、表示行から文字位置へのマッピング
     - **選択ハイライトの修正**: `drawSelectionHighlight()`を完全書き直し、折り返し行ごとの選択範囲の交差部分を描画
   - **デバッグログのクリーンアップ** (✅ 2025-10-18):
     - BaseTextInput.javaから大量のデバッグログを削除
     - 必要最小限のログのみ残し、パフォーマンスへの影響を最小化
   - **シフトクリックアンカー位置の更新バグ修正** (✅ 2025-10-18):
     - **問題**: テキスト入力やペーストでカーソルが移動しても、シフトクリックのアンカー位置が更新されず、意図しない位置から範囲選択が開始される
     - **解決策**: カーソル位置が変更されるすべての操作で`anchorPosition = cursorPosition`を実行
       - BaseTextInput.java: 文字入力、バックスペース、Delete、Ctrl+A、`deleteSelection()`、`insertText()`
       - TextField.java: 左右矢印、Home、End
       - TextArea.java: Enter、左右矢印、上下矢印
     - **結果**: カーソル移動後のシフトクリック範囲選択が正しく動作

4. **NoteEditScreen** (✅ 完了):
   - 手動描画 → TextField使用（タイトル入力）
- 手動描画 → TextArea使用（コンテンツ入力）
  - 手動ボタン描画 → Buttonコンポーネント使用（コピー、貼り付け、保存、削除、戻るボタン）
  - Label使用（ヘッダー、各セクションラベル）
  - クリップボード統合：選択範囲のコピー・貼り付け機能
  - イベントハンドラをコンポーネントのコールバックで実装
  - コード可読性向上、保守性向上
