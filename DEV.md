# MochiMobileOS 開発ドキュメント

このドキュメントはMochiMobileOSの技術的な概要と各機能へのポータルです。

## 開発ドキュメント一覧

より詳細な技術仕様や実装については、以下の各ドキュメントを参照してください。

- **[00_Project_Overview.md](./docs/development/00_Project_Overview.md)**
  - プロジェクト全体の概要、アーキテクチャ、主要機能について説明します。

- **[01_Chromium_Integration.md](./docs/development/01_Chromium_Integration.md)**
  - Chromium (JCEF/MCEF) の統合アーキテクチャ、パフォーマンス最適化の経緯、UI設計について詳述します。

- **[02_WebView_System.md](./docs/development/02_WebView_System.md)**
  - JavaFX WebViewベースのWebレンダリングシステムに関する情報です。

- **[03_Virtual_Internet.md](./docs/development/03_Virtual_Internet.md)**
  - MOD内仮想ネットワーク（IPvM）の仕様と実装について説明します。

- **[04_Forge_Integration.md](./docs/development/04_Forge_Integration.md)**
  - Minecraft Forge MODとしての連携部分のアーキテクチャと実装について説明します。

- **[05_Hardware_API.md](./docs/development/05_Hardware_API.md)**
  - 仮想ハードウェア（センサー、通信、バッテリー等）の抽象化APIに関する仕様です。

- **[06_External_App_API.md](./docs/development/06_External_App_API.md)**
  - 外部MOD開発者向けのアプリケーション開発API（パーミッション、インテント等）について説明します。

- **[07_GUI_Component_Library.md](./docs/development/07_GUI_Component_Library.md)**
  - 再利用可能なUIコンポーネントライブラリの設計と使用方法について説明します。

- **[08_Font_System.md](./docs/development/08_Font_System.md)**
  - 多言語（特に日本語）フォントの表示を実現するためのフォントシステムについて説明します。

- **[09_Known_Issues_And_TODO.md](./docs/development/09_Known_Issues_And_TODO.md)**
  - 現在把握している既知の問題点と、今後の開発タスク（TODO）を管理します。

## 開発ログ

日々の詳細な開発ログは `devlogs` ディレクトリに格納されています。

- [devlogs/2025-10-14.md](./devlogs/2025-10-14.md)
- [devlogs/2025-10-13.md](./devlogs/2025-10-13.md)

## プロジェクト仕様（UI/UX・テーマ・レジストリ）

- デザインシステム（デザイントークン）を導入し、色・角丸・スペーシング等を変数化
  - セマンティックカラー: `color.primary`, `color.background`, `color.surface`, `color.on_surface`, `color.border` など
  - モード: ライト/ダーク/オート、コントラスト: normal/high、角丸スケール: compact/standard/rounded
- レジストリ（設定システム）
  - `SettingsManager` を拡張（VFS JSON永続化、リスナー通知）
  - 保存先: `system/settings/registry.json`
  - 主キー: `ui.theme.mode`, `ui.theme.seed_color`, `ui.theme.accent_color`, `ui.motion.reduce` 等
- テーマエンジン
  - `ThemeEngine` を新設（設定を購読しデザイントークンを生成）
  - 代表API: `colorPrimary()`, `colorBackground()`, `colorSurface()`, `colorOnSurface()`, `radiusSm()` など
  - 付随ユーティリティ: `ui/effects/Motion`（Reduce Motion対応の継続時間/イージング）, `ui/effects/Elevation`（簡易影）
- パフォーマンスセーバー: `ui.performance.low_power`（低電力/低負荷モード）設定を追加（UI/設定のみ）。挙動は今後実装。

## 現在の仕様（抜粋）

- Kernel 初期化順に `SettingsManager(vfs)` と `ThemeEngine(settings)` を作成
- SettingsScreen は ThemeEngine のトークンを使用して配色（ダーク/ライト即時反映）
- AppLibraryScreen はテーマ同期し、検索ボックス/ホバー/押下ハイライト/ホイールスクロールに対応
- Appearanceにテーマカラープリセット（12色のseed選択）を追加。アクセントはseedから自動生成
- PGraphics統一アーキテクチャへの移行
  - `INotification`インターフェースに`draw(PGraphics g, ...)`メソッドを追加（2025-11-07）
  - PApplet版のdrawメソッドは非推奨化（@Deprecated）
  - 文字化けコメントを修正
    - AppLibraryScreen.java: 20箇所以上の文字化けを修正（2025-11-07）
    - SettingsManager.java: 全14箇所の文字化けを修正（2025-11-08）
    - ThemeEngine.java: 全16箇所の文字化けを修正（2025-11-08）
- コンパイルエラー修正（2025-11-08）
    - SettingsScreen.java: 構文エラー修正（コメントアウトされたif/switch文の復元）
    - SettingsScreen.java: 未定義変数追加（showAppearancePanel, y2, y3）
    - ThemeEngine.java: 95行目の埋め込み改行文字を修正
    - SettingsManager.java: 文字化けコメントを全て修正（UTF-8エンコーディングエラー解消）
    - ThemeEngine.java: 文字化けコメントを全て修正（UTF-8エンコーディングエラー解消）
    - ThemeEngine.java: ダークトーン時のファミリー別プライマリ色の設定を追加
    - SettingsManager.java: ui.theme.toneとui.theme.familyの設定を追加（99行目の埋め込み改行文字を修正）
    - ThemeEngine.java: Mode列挙型にAQUAを追加、52行目と77行目の埋め込み改行文字を修正
    - SettingsScreen.java: 文字化けコメント修正（79, 134, 149, 152, 186, 196, 202, 368, 436, 438行目）
    - ThemeEngine.java: 全コメントの文字化けを修正（2025-11-08）
      - 全ての日本語コメントが文字化けしていた問題を解消
      - 6, 7, 16, 31, 90, 96, 104, 107, 149, 184, 194, 196, 201, 202, 206, 212, 216, 223, 231, 250, 273行目の文字化けを修正
    - SettingsScreen.java: 文字化けと構文エラーを修正（2025-11-08）
      - 36, 104, 159, 174, 177, 211, 221, 227, 402, 619, 625, 886, 956, 964, 969行目の文字化けを修正
      - 660行目: メソッド宣言前の閉じ括弧欠落を修正
      - 850-865行目: 不正な位置にあったレガシーコードを削除
- 通知センターのイベント処理の改善（2025-11-07）
  - **第1版**: パネル内のすべてのジェスチャーイベントを確実に消費し、下のレイヤーへの貫通を防止
  - **第2版**: 当たり判定とイベント処理順序を完全に再設計
    - パネル内外の判定を最初に行い、処理フローを明確化
    - すべてのジェスチャータイプ（TAP、LONG_PRESS、DRAG_START、DRAG_MOVE、DRAG_END、SWIPE_*）を適切に処理
    - パネル外のイベントも確実に消費し、閉じる動作を実装
    - ヘッダーエリア、通知エリア、スクロール処理の優先順位を整理
    - `isInBounds()`メソッドで画面全体をカバー（背景の暗幕を含む）
    - `onGesture()`メソッドで、パネル内であれば常に`true`を返すように改善

## 初期マイルストーン：ダッシュボード構成と主要アプリ

### ダッシュボード（ホーム画面）カード (5種)
-   **メッセージカード**:
    -   機能: 新着メッセージの通知を表示（表示専用）。
    -   連携: 内部メッセージングシステム。
-   **電子マネーカード**:
    -   機能: 「MoyMoy」アプリの残高を表示。タップで本体アプリを開く。
    -   連携: MoyMoyアプリ向けAPI。
-   **クロック＆ウェザーカード**:
    -   機能: ゲーム内の現在時刻と天候を表示（表示専用）。
    -   連携: ゲーム内時間・天候API。
-   **Chromium検索カード**:
    -   機能: キーワード入力欄。入力を受けてChromiumブラウザアプリを起動する。
    -   連携: Chromiumブラウザアプリ。
-   **AIアシスタントカード**:
    -   機能: 質問の入力欄。入力を受けてAIアシスタントアプリを起動する。
    -   連携: AIアシスタントアプリ。

### コア・アプリケーション (3種)
-   **Chromiumブラウザアプリ**:
    -   `java-cef` を利用した基本的なWebブラウザウィンドウ。
-   **AIアシスタントアプリ**:
    -   チャット形式のUIと、固定の応答を返す**モックAI**を実装。
-   **各種APIおよび内部システム**:
    -   MoyMoy等の外部アプリがカード情報を提供するためのAPI。
    -   メッセージカードに情報を渡すための内部メッセージングシステム。

### デザイン刷新 (HomeScreen.java)
-   **ホログラフィック・レイヤーUIの適用**: ステータスバー、ナビゲーションエリア、アプリアイコンカードの背景に半透明効果を適用。
-   **テーマ対応の強化**: 削除ボタンのハードコードされた色をテーマ対応のエラーカラーに置き換え。寸法、フォントサイズ、角丸の値を定数化し、コードの保守性とテーマ統合の準備を強化。

## 問題（Issues）

- 多数のUIコンポーネント/画面が色をハードコードしており、トークン適用が未完
- Theme（旧）クラスは暫定互換のまま。段階的に ThemeEngine へ統合予定
- AUTOモードの切替ロジック（時間/環境依存）は未対応（将来拡張）
- Gradleビルド時にファイルロックの問題が発生する場合がある（IntelliJ IDEA等のIDEを閉じてから実行する必要がある）
- ~~通知センターのインタラクトが下のレイヤーに貫通するバグ（修正済み: 2025-11-07）~~
- ~~長押し後にドラッグジェスチャーが一切動作しない（M-2の排他制御が原因、2025-11-10修正済み）~~
- ~~ホーム画面のページアニメーション完了処理が呼ばれず、2回目以降のドラッグが無効になる~~

## TODO

- BaseComponent/主要コンポーネント（Button/Label/Switch/Slider/Text系）をThemeEngine準拠へ順次移行（進行中）
  - (更新) HomeScreen.javaの描画要素は、テーマ対応の色、寸法、フォントサイズ、角丸の定数化により大幅にThemeEngine準拠に近づいた。
- 追加適用: Panel/ListView/Dialog/Dropdown をトークン化（背景/枠線/選択色/文字色）
- 追加適用: Checkbox/RadioButton をトークン化（境界/選択色/文字色）
- 設定アプリに「外観」セクションを追加（モード/アクセント/角丸/文字サイズ/Reduce Motion）最小実装（完了）
- エレベーション/影・モーションの統一（Reduce Motion対応）
  - ユーティリティ実装済（段階適用中）
- AUTOモードの判定実装（時間帯/ホストOSテーマのフックが可能なら対応）
- App Library: 検索のIME/テキスト入力周りの強化、検索対象の拡張（説明/ID）
- 低電力モードの適用実装（ScreenTransitionのフェード切替、影段階の調整、速度最適化）

### 初期マイルストーン実装タスク
- **ダッシュボードカードの実装:**
  - メッセージカード（通知連携）
  - 電子マネーカード（MoyMoy連携API）
  - クロック＆ウェザーカード
  - Chromium検索カード（ブラウザ起動）
  - AIアシスタントカード（アプリ起動）
- **コア・アプリケーションの基礎実装:**
  - Chromiumブラウザアプリ
  - AIアシスタントアプリ（UIとモックAIのみ）
  - MoyMoyアプリ向けAPI
  - 内部メッセージングシステム

## �d�l�i�ǉ��E�X�V�j

- ���C�A�E�g�g�[�N���i�b��j
  - spacing: PADDING=16, GAP=12, ITEM_HEIGHT=88�iControl Center �� App Library �ŋ��ʉ^�p�j
  - grid: Control Center �� 3 �J�����A���E�p�f�B���O�ƃM���b�v���l���������z��
  - App Library: LIST_START_Y=112, ITEM_PADDING=16, �s��=88�A�E�[�� 400-ITEM_PADDING ����ɐ���

- �g�O���iControl Center / ToggleItem�j
  - �w�i=surface�{border�̃J�[�h�AON����accent�̒�A���t�@�d��
  - ���x��=onSurface�A��������#999999�A�A�C�R����accent�^�C���{onPrimary�e�L�X�g
  - �X�C�b�`=ON:accent / OFF:#B0B0B0�A�m�u=���i�A���t�@��enabled�ɉ����Ē����j

## ���i�X�V�j

- �ꕔ��ʂŕ��E������ 400x600 �O��̃n�[�h�R�[�h���c���i�����I�ɉσT�C�Y�֓��ꂪ�K�v�j
- Reduce Motion/Low Power �̋����K�p�͖������i�`�掞�̕���� //TODO �Ƃ��Ė����j

## TODO�i�X�V�j

- Control Center �̑S�A�C�e���� IControlCenterItem �����Ńe�[�}������Ă��邩�I����
- App Library �� hover/pressed �̃A���t�@�l���e�[�}�� Reduce Motion �ɘA���i//TODO�j
- �ω�ʃT�C�Y�Ή��̂��߁A���� 400 �̃��e�����팸�i�����Ή��j

### �ǉ�: �킹�n�p�l���̎��F�����P�i���C�g���[�h�j
- �X�N�����i�Ö��j������: ControlCenter=110, NotificationCenter(PApplet)=110, (PGraphics)=100
- �p�l���ʂ��킸���ɈÂ�: ThemeEngine.light.colorSurface=#FAFAFA�A��ɍ��̔����I�[�o�[���C�i~16?18�j
- �ړI: ���w�Ƃ̍��𖾊m�����A����сEῂ����̒ጸ

### �ǉ�: �e�[�}���[�h�̊g��
- �V���[�h: orange / pink / qua ��ǉ��iLIGHT�n�̃o���A���g�j
- ThemeEngine: Mode �ɗ񋓂�ǉ����A�e���[�h�� ackground/surface/onSurface/border �����₩�ȐF���ŏ㏑��
- Settings > Appearance: Theme Mode �� 2�i�ڃ{�^���iOrange/Pink/Aqua�j��ǉ����Aui.theme.mode �ɕۑ�


## ύX(2025-11-08)
- _[N+YellowŕFɂȂC: ThemeEngine.recomputePalette()Ń_[Ng[colorOnSurfaceɔ(0xFFFFFFFF)ɌŒBt@~[ɈˑFSہB


- SettingsScreen.java: UTF-8エンコーディングエラー修正（2025-11-08）
  - BOM (Byte Order Mark) を削除
  - 全ての文字化けした日本語コメントを修正（172, 175, 178, 228, 473行目など）
  - 471行目: 破損した文字列リテラル を修正
  - 683行目: 埋め込まれた改行文字
 を実際の改行に置換
  - コンパイルエラー解消: BUILD SUCCESSFUL

## 変更(2025-11-09)
- **JavaFX WebViewの完全削除**（HTMLアプリケーション軽量化プロジェクト Phase 1）
  - 目的: Chromium統合により冗長となったJavaFX WebViewを削除し、JARサイズ削減（50-80MB）、起動時間短縮（1-2秒）、メモリ使用量削減（30-50MB）を実現
  - 削除されたコンポーネント:
    - `core/src/main/java/jp/moyashi/phoneos/core/apps/browser/` (BrowserApp, BrowserScreen)
    - `core/src/main/java/jp/moyashi/phoneos/core/apps/htmlcalculator/` (CalculatorHTMLApp)
    - `core/src/main/java/jp/moyashi/phoneos/core/ui/HTMLScreen.java` (HTML画面の基底クラス)
    - `core/src/main/java/jp/moyashi/phoneos/core/service/webview/` (WebViewManager, WebViewWrapper)
  - Kernel.javaの更新:
    - BrowserAppのimport削除（18行目）
    - CalculatorHTMLAppの登録コード削除（1076-1077行目）
    - BrowserAppの登録コード削除（1082-1083行目）
    - calculatorHTMLAppとbrowserAppの初期化コード削除（1090, 1092行目）
  - ビルド設定の更新:
    - `core/build.gradle.kts`: JavaFX依存（javafx-base, javafx-graphics, javafx-controls, javafx-web, javafx-swing, javafx-media）を削除
    - `forge/build.gradle`: JavaFX依存とcopyJavaFxJars/extractJavaFxCoreタスクを削除
  - 結果: BUILD SUCCESSFUL（警告のみ、エラーなし）
  - 今後の展開: Chromium APIを使用したHTML表示機能の実装を検討

- **UTF-8エンコーディングエラーの全面修正**（2025-11-09）
  - 目的: JavaFX削除後のコンパイルエラーを修正し、ビルドを成功させる
  - 修正されたファイル:
    - `core/src/main/java/jp/moyashi/phoneos/core/apps/launcher/ui/HomeScreen.java`
      - 20箇所以上の文字化け（`チE��`, `琁E`, `宁E`, `E��`等）を修正
      - 改行が失われていた箇所を修正（1239, 1645行目等）
      - 文字列リテラルの文字化けを修正（2276, 2280, 2918, 2926行目）
      - 未定義メソッド`syncLivePageDragFromGesture()`をコメントアウト（251行目）
    - `core/src/main/java/jp/moyashi/phoneos/core/input/GestureManager.java`
      - javadocコメントの文字化けを修正（119-120, 170, 174, 178-179, 231, 234行目）
      - 改行が失われていた箇所を修正（116, 170, 178, 231, 233行目）
      - クラス閉じ括弧の位置エラーを修正（328→335行目に移動）
  - 結果: **全モジュール（core, standalone, forge）のコンパイルが成功** - BUILD SUCCESSFUL

- **ホームスクリーンのフォントサイズ調整**（2025-11-09）
  - 目的: ホームスクリーンの文字が大きすぎる問題を修正
  - 修正内容（HomeScreen.java）:
    - アプリアイコン内のイニシャル表示: 20 → 12 (1569行目)
    - アプリ名ラベル: 11 → 8 (1406行目、1498行目)
  - 結果: BUILD SUCCESSFUL

- **ジェスチャー機能の大幅な改善**（2025-11-09/10）
  - 目的: GestureManagerの問題21件を体系的に修正し、堅牢性とパフォーマンスを向上
  - Phase 1（High優先度）:
    - H-1: 長押し検出をupdate()メソッドに一本化（handleMouseReleased()からの重複削除）
    - H-2: ドラッグ終了時のスワイプ検出を削除（ドラッグとスワイプの競合を防止）
    - H-3: isInBounds()実装の調査完了（各マネージャーの実装は適切と判断、変更なし）
  - Phase 2（Medium優先度）:
    - M-1: ドラッグ座標の常時更新を確認（既に実装済み）
    - M-2: 長押し後のドラッグ排他制御を追加（意図しない移動を防止）
    - M-3: リスナー登録の一元管理はリスク高のためスキップ
    - M-4: スワイプ方向判定に角度判定を追加（±30度の範囲内で水平/垂直を判定、対角線は検出しない）
    - M-5: 例外発生時も次のリスナーへ継続する仕組みを実装（エラー回復性向上）
  - Phase 3（Low優先度）:
    - L-1: パフォーマンスログの閾値を10ms/20msから20ms/50msに引き上げ（I/O負荷軽減）
    - L-2: スワイプ角度判定の定数化（SWIPE_HORIZONTAL_ANGLE_MAX=30.0, SWIPE_VERTICAL_ANGLE_MIN=60.0）
    - L-3: デバッグログの統一確認（System.out.printlnは存在せず、全てlogger経由）
    - L-4: 将来的なジェスチャー拡張は今回対応せず
    - L-5: コード重複の削除（距離計算を共通メソッドcalculateDistance()に統一、debugVerbose()メソッドを削除）
  - 結果: **全モジュールコンパイル成功** - BUILD SUCCESSFUL
  - 修正ファイル: `core/src/main/java/jp/moyashi/phoneos/core/input/GestureManager.java`
  - **バグ修正**（2025-11-10）:
    - 問題1: ドラッグが一度しか動作しない → M-2の不要なコード(`longPressDetected = false;`)を削除して修正
    - 問題2: 長押しが動作しない → update()が呼ばれていないため、handleMouseDragged()内でも長押しをチェックするように補完
    - 問題3: ドラッグのログは出るが実際に反映されない（ページスワイプ、AppLibraryスクロール）→ 長押し検出ロジックが距離チェックより先に実行されていたため、500ms以上かけてドラッグすると長押しとして誤検出され、ドラッグ処理がブロックされていた。距離計算を長押しチェックの前に移動し、`dragDistance < DRAG_THRESHOLD`条件を追加して修正（GestureManager.java 149-165行目）
    - H-1の補完: Kernel.javaでgestureManager.update()が呼ばれていない環境に対応
  - **追加修正**（2025-11-10）
    - 長押し後のドラッグ排他制御を撤回し、DRAG_THRESHOLDを超えた移動では常にドラッグを開始するよう変更（GestureManager.java 173行目付近）
    - 長押し→ドラッグでアイコン移動やページスワイプが再び機能する（ホーム画面/AppLibraryで確認済み）
  - **ホームスクリーン ページアニメーション修正**（2025-11-10）
    - `updatePageAnimation()` でアニメーション完了時に `completePageTransition()` が呼ばれておらず、`isAnimating` が解除されない問題を修正
    - これによりホーム画面のページスワイプ/ドラッグが初回のみ有効になる不具合が解消（HomeScreen.java 1039行目）
  - **ドラッグ処理のパフォーマンス改善**（2025-11-10）
    - GestureManagerのドラッグイベント発火間隔を1ms → 8msへ変更し、不要な高頻度ディスパッチによるUIスレッド飽和を緩和
    - DRAG_MOVEイベントのデバッグログ出力を抑制し、ミニマム移動量（2px）または経過時間8msのどちらかでイベントをまとめるフレーム同期待ちに変更
    - ControlCenterManager/LockScreenの高頻度`System.out.println`をデバッグフラグ経由にし、標準実行時のI/O負荷を削減
  - **グローバルスワイプ検出の復活**（2025-11-10）
    - `checkForSwipe()` を再度呼び出し、エッジスワイプで通知センター/コントロールセンター/ロック画面が展開できるように修正
    - 長押し中はスワイプを発火させず、ドラッグ完了後に距離・速度条件を満たした場合のみSWIPEイベントを生成
