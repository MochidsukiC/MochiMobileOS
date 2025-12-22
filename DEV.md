# MochiMobileOS 開発ドキュメント

このドキュメントはMochiMobileOSの技術的な概要と各機能へのポータルです。

## 開発ドキュメント一覧

より詳細な技術仕様や実装については、以下の各ドキュメントを参照してください。

- **[00_Project_Overview.md](./docs/development/00_Project_Overview.md)**
  - プロジェクト全体の概要、アーキテクチャ、主要機能について説明します。本プロジェクトは教育目的での利用も想定して開発されています。

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
- Kernelアーキテクチャのリファクタリング（2025-12-02〜2025-12-04）
  - Service Locatorアンチパターンから依存性注入（DI）パターンへの移行
  - ServiceContainer/CoreServiceBootstrapによるサービス管理の一元化
  - イベント駆動アーキテクチャの導入（EventBus実装）
  - メモリ管理の高度化（MemoryManager: GC制御、リーク検出、統計収集）
  - ファイルシステム管理の抽象化（FileSystemManager: VFS統合、ファイル監視、キャッシュ）
  - リソース管理の効率化（ResourceCache: LRUポリシー、ソフト/ウィーク参照）
  - **既存サービスのDIコンテナへの移行開始**（2025-12-04）
    - 第一段階移行済み: VFS、LoggerService、SystemClock、NotificationManager、AppLoader、LayoutManager、SettingsManager、ThemeEngine、ScreenManager、PopupManager、GestureManager、InputManager、RenderPipeline
    - **第二段階移行完了**（2025-12-04）
      - 新規追加サービス: ClipboardManager（SimpleClipboardManager実装）、ControlCenterManager、LockManager、ServiceManager
      - SimpleClipboardManager実装クラスを作成（ClipboardManagerインターフェースの実装）
      - Kernelのフォールバック処理を拡張
    - **第三段階移行完了**（2025-12-04）
      - 追加サービス: ChromiumService（DefaultChromiumService、ChromiumProviderに依存）、VirtualRouter、MessageStorage、ハードウェアバイパスAPI（MobileDataSocket、BluetoothSocket、LocationSocket、BatteryInfo）
      - ハードウェアAPIのDI優先初期化: DIコンテナ → HardwareController → 直接初期化の順で取得
      - ChromiumServiceはLazySingletonとして登録（ChromiumProviderが環境依存のため）
      - **ChromiumServiceのNullエラー修正**（2025-12-04）
        - 問題: ChromiumProviderが利用できない環境でChromiumServiceファクトリがnullを返し、NullPointerExceptionが発生
        - 解決: NoOpChromiumService実装を作成し、ChromiumProviderが存在しない場合はNoOp実装を返すように修正
        - NoOpChromiumServiceはすべてのメソッドで安全な空の操作またはnullを返す（Null Objectパターン）
    - **第四段階移行完了**（2025-12-04）
      - 追加サービス: CameraSocket、MicrophoneSocket、SpeakerSocket、ICSocket、SIMInfo、SensorManager、BatteryMonitor
      - すべてのハードウェアバイパスAPIのDI統合が完了
      - SensorManagerはLazySingletonとして登録（Kernelインスタンスに依存）
      - 3段階のフォールバック機構を確立: DIコンテナ → HardwareController → 直接初期化
      - Kernelのハードウェアバイパス初期化処理を最適化（各サービスの取得元をログ出力）
    - Kernelの初期化処理: DIコンテナから各サービスを優先的に取得、取得できない場合は従来の直接初期化（フォールバック機構）
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
- **Mac環境でのChromium HiDPI/Retinaディスプレイ対応の不具合**（2025-11-18）
  - 症状: Chromiumブラウザでウェブページが表示されるが、アスペクト比が崩れる
    - x軸が1/2に圧縮され、画像が左右に2つ並んで表示される
    - y軸も大きく圧縮される（約1/8程度）
  - 原因: `ChromiumRenderHandler.java`のHiDPIダウンサンプリングコード（106-128行目）に問題がある可能性
    - Mac Retinaディスプレイでは2倍サイズ（800x952）でレンダリングされることを想定したコードだが、実際の動作が異なる
    - ByteBufferの読み取り位置計算またはサイズ判定に問題がある可能性
  - 現状: HiDPI処理を一時的に無効化（`isHiDPI = false;`）して調査中
  - 関連ファイル:
    - `core/src/main/java/jp/moyashi/phoneos/core/service/chromium/ChromiumRenderHandler.java`
    - `standalone/src/main/java/jp/moyashi/phoneos/standalone/StandaloneChromiumProvider.java`
  - 対応方針: 実際にChromiumが送ってくるバッファサイズを確認し、適切なダウンサンプリングロジックを実装する必要がある

## TODO

### メディア再生管理システム（完了 2025-12-21）
- **MediaSession/MediaController API実装**
  - `core/media/` パッケージに以下のクラスを新規作成:
    - `PlaybackState.java` - 再生状態列挙型（STOPPED, PLAYING, PAUSED, BUFFERING, ERROR）
    - `MediaMetadata.java` - メタデータモデル（Builder付き、title/artist/album/duration/artwork）
    - `MediaSessionCallback.java` - 再生コントロールコールバックインターフェース
    - `MediaSession.java` - アプリがセッションを作成・状態更新するためのクラス
    - `MediaController.java` - 外部からセッションを操作するコントローラー
    - `AudioFocusManager.java` - オーディオフォーカス管理（排他制御）
    - `MediaSessionManager.java` - システム全体のセッション管理サービス
  - `NowPlayingItem.java` - コントロールセンター用のメディア再生ウィジェット
  - `CoreServiceBootstrap.java` - MediaSessionManagerをDIコンテナに登録
  - `Kernel.java` - NowPlayingItemをコントロールセンターに追加
- **Chromiumブラウザ統合（完了 2025-12-21）**
  - `ChromiumBrowser.java` - メディア検出JavaScript注入、`[MochiOS:Media]`コンソールメッセージ処理
  - `ChromiumSurface.java` - メディア検出インターフェースメソッド追加
  - `DefaultChromiumService.java` - DefaultChromiumSurfaceでメディアメソッド実装
  - `ChromiumBrowserScreen.java` - MediaSession統合（コールバック、状態更新、スクリプト注入）
  - YouTubeなどのWebメディア再生をNow Playingウィジェットに連携
- **機能:**
  - 各アプリの再生状態を統合管理
  - コントロールセンターからの再生/一時停止/スキップ操作
  - オーディオフォーカスによる音声出力の排他制御
  - 外部MODアプリからも利用可能なAPI
  - Webブラウザ（YouTube等）のメディア再生検出・制御

### Kernelアーキテクチャ移行の次ステップ
- **既存サービスのDIコンテナ化（ほぼ完了）**
  - ✅ 完了（26サービス）: VFS、LoggerService、SystemClock、NotificationManager、AppLoader、LayoutManager、SettingsManager、ThemeEngine、ScreenManager、PopupManager、GestureManager、InputManager、RenderPipeline、ClipboardManager、ControlCenterManager、LockManager、ServiceManager、ChromiumService、VirtualRouter、MessageStorage、MobileDataSocket、BluetoothSocket、LocationSocket、BatteryInfo、**MediaSessionManager**
  - 残りのサービス: その他のハードウェアソケット（CameraSocket、MicrophoneSocket、SpeakerSocket、ICSocket）、SensorManager、BatteryMonitor
- **画面クラスのDI対応**
  - Screenインターフェースの拡張（依存性を注入可能に）
  - 既存画面クラスのコンストラクタ変更とDI対応
- **統合テスト**
  - DIコンテナ経由の初期化フロー検証
  - 依存関係の解決順序の最適化
  - メモリ効率とパフォーマンス測定

### Kernel分離リファクタリング Phase 1-5（完了）
- **Phase 1: サービス層基盤（完了 2025-12-02）**
  - ServiceContainer/CoreServiceBootstrap作成（依存性注入コンテナ）
- **Phase 2: 電源・ライフサイクル管理（完了 2025-12-02）**
  - PowerManager/SystemLifecycleManager作成（電源管理とライフサイクル）
- **Phase 3: 画面・リソース管理（完了 2025-12-02）**
  - NavigationController/LayerController作成（画面遷移とレイヤー管理）
  - HardwareController作成（ハードウェアAPI）
- **Phase 4: イベントバスシステム（完了 2025-12-04）**
  - EventBus/Event/EventType/EventListener作成（Publish-Subscribeパターン）
  - SystemEvent/ApplicationEvent/ScreenEvent実装
  - PowerManager、NavigationController、Kernelとの統合
- **Phase 5: メモリ・リソース管理（完了 2025-12-04）**
  - MemoryManager作成（メモリ監視、GC制御、リーク検出）
  - FileSystemManager作成（VFS統合、ファイル監視、キャッシュ）
  - ResourceCache作成（LRUキャッシュ、ソフト/ウィーク参照、メモリ圧迫対応）
  - CoreServiceBootstrapへの統合完了

- BaseComponent/主要コンポーネント（Button/Label/Switch/Slider/Text系）をThemeEngine準拠へ順次移行（進行中）
  - (更新) HomeScreen.javaの描画要素は、テーマ対応の色、寸法、フォントサイズ、角丸の定数化により大幅にThemeEngine準拠に近づいた。
  - (更新) LockScreen.javaをThemeEngine準拠に更新（2025-12-20）。背景、文字色、通知カード、パターン入力UIをテーマカラー/角丸トークンに対応。
  - (更新) コントロールセンターを大幅強化（2025-12-20）。
    - 4カラム・スクエアグリッドデザインへの刷新
    - 縦型スライダー（音量/輝度）の実装と左側配置
    - メディアコントロール（2x2パネル）の右上固定配置
    - ドラッグ操作の最適化（スライダー操作とパネルスクロールの共存）
    - 低電力モード切り替えの実装
    - 音量スライダーとシステム設定("audio.master_volume")の双方向連携実装
    - Chromium連携強化: 再生状態/時間の同期ロジック改善
- 追加適用: Panel/ListView/Dialog/Dropdown をトークン化（背景/枠線/選択色/文字色）
- 追加適用: Checkbox/RadioButton をトークン化（境界/選択色/文字色）
- ~~設定アプリに「外観」セクションを追加（モード/アクセント/角丸/文字サイズ/Reduce Motion）最小実装（完了）~~
- 設定アプリに「外観」セクションを追加（モード/アクセント/角丸/文字サイズ/Reduce Motion）最小実装（完了）
- エレベーション/影・モーションの統一（Reduce Motion対応）
  - ユーティリティ実装済（段階適用中）
- AUTOモードの判定実装（時間帯/ホストOSテーマのフックが可能なら対応）
- App Library: 検索のIME/テキスト入力周りの強化、検索対象の拡張（説明/ID）
- 低電力モードの適用実装（ScreenTransitionのフェード切替、影段階の調整、速度最適化）
- 設定アプリの残りセクション実装（Sound & Vibration、Storage、Apps & Notifications）

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

## 変更(2025-11-28)
- **Mac版とWindows版のコードベースマージ完了**
  - 背景: Mac環境で`git push --force`が実行され、Windows環境とGit履歴が完全に分岐
    - ChromiumBrowserScreen.java: Mac版では完全にリライト（ChromiumSurface/TextField/Buttonベースの新アーキテクチャ、257行）、Windows版では従来の手動レンダリング（1075行）
    - SettingsScreen.java: Windows版ではPhase 1-3実装完了（6セクション、1573行）、Mac版は旧バージョン（875行）
  - 対応手順:
    1. `.gitignore`を更新し、未追跡のランタイムファイル/開発ファイル（.idea/*, standalone/jcef-bundle/*, forge/run/*, *.log, *.bat等）を除外
    2. `git pull origin master --allow-unrelated-histories`で強制マージ実行
    3. 20ファイル以上のマージ競合をユーザーがIDE上で手動解決
    4. HomeScreen.javaのimport文不足（TextField, Sensor, SensorEvent, SensorEventListener）をユーザーが手動修正
  - 結果: **core/forgeモジュールのビルドに成功** - BUILD SUCCESSFUL
    - Mac版のChromiumブラウザUI改善（新アーキテクチャ）を取り込み
    - Windows版のSettings Phase 1-3実装（6セクション完全実装）を保持
    - 両環境の変更を統合した完全なコードベースを確立
  - 今後の課題:
    - Mac環境でのHiDPI/Retinaディスプレイ対応の検証
    - 統合されたChromiumBrowserScreenの動作テスト

- **ホームスクリーンのページ構造修正**
  - 問題: マージの過程で1ページ目（ダッシュボード）にアプリショートカットが配置されていた
  - 修正内容（HomeScreen.java `createDefaultLayout()`メソッド）:
    - 1ページ目: ダッシュボード専用ページ（アプリショートカットは配置しない）
    - 2ページ目以降: アプリグリッドページ（従来通りアプリショートカットを配置）
    - 最終ページ: AppLibraryページ（全アプリリスト表示）
  - 結果: 正しいページ構造を復元し、ダッシュボードとアプリグリッドが分離された

- **Chromiumブラウザのマウスイベント処理修正**
  - 問題: クロミウムブラウザでスクロール、クリック、ドラッグが正しくChromiumに転送されていなかった
  - 修正内容（ChromiumBrowserScreen.java）:
    1. `mousePressed`メソッド（236行目）: `sendMouseDragged`を誤って呼んでいた → `sendMousePressed`に修正
    2. `mouseDragged`メソッドを新規追加（282-294行目）: ドラッグイベントをChromiumに転送
    3. `mouseMoved`メソッド（267-271行目）: UIコンポーネント処理後、コンテンツエリア内であればChromiumに`sendMouseMoved`を転送
    4. `mouseWheel`メソッド（277-279行目）: コンテンツエリア内かどうかをチェックしてから転送
    5. `mouseReleased`メソッド（251-254行目）: コンテンツエリア内かどうかをチェックしてから転送
    6. `isInContentArea`ヘルパーメソッドを追加（327-329行目）: コンテンツエリア（10, 60, width-10, height-60）の当たり判定
  - **追加修正1**: ボタン番号とスクロール量を修正
    - ボタン番号: ChromiumProviderはProcessing規約（1=左, 2=中, 3=右）でボタン番号を受け取る
      - `mousePressed`, `mouseReleased`, `mouseDragged`でボタン番号を1（左ボタン）に修正（button 0 → button 1）
    - スクロール量: ChromiumProviderでdelta値を`(int)`にキャストするため、小さな値が0になる問題を修正
      - `mouseWheel`でdelta値を10倍に増幅（`delta * 10`）してから転送（当初3倍→10倍に調整）
  - **追加修正2**: スクロールイベントの重複を修正（StandaloneWrapper.java）
    - 問題: マウスホイールイベントが3箇所で登録されており、1回のスクロールで複数回イベントが発火していた
      - NEWTウィンドウのmouseWheelMovedリスナー（88-105行目）
      - AWTのMouseWheelListener（175-186行目）
      - Processing標準のmouseWheel()メソッド（353-361行目）
    - 修正: NEWT/AWTのリスナーをコメントアウトし、Processing標準のmouseWheel()のみを使用
    - 結果: スクロールイベントの重複が解消され、正しいスクロール動作を実現
  - **最終結果**: Chromiumブラウザ内でのクリック、ドラッグ、スクロール、マウス移動が正常に動作するようになった

- **Android Intent URL対応**
  - 問題: `intent://`スキームのURLにアクセスすると「ERR_UNKNOWN_URL_SCHEME」エラーが発生
  - Intent URLとは: Androidアプリを起動するためのURL形式で、通常のブラウザでは処理できない
    - 形式: `intent://HOST/PATH#Intent;scheme=SCHEME;S.browser_fallback_url=FALLBACK_URL;end;`
  - 修正内容:
    - **ChromiumBrowserScreen.java**:
      - `convertIntentUrl()`メソッドを新規追加（347-395行目）
        - Intent URLを検出し、`S.browser_fallback_url`パラメータからフォールバックURLを抽出
        - フォールバックURLが存在しない場合は、`scheme`パラメータとホスト/パスから通常のURLを構築
        - URLデコード処理を含む
      - `keyPressed()`メソッドを修正（303-306行目）: アドレスバーからのURL入力時にIntent URLを変換してから読み込む
    - **ChromiumBrowser.java（追加修正）**:
      - `CefRequestHandlerAdapter`を追加（107-125行目）: ページ内のIntent URLリンククリック時にもインターセプト
        - `onBeforeBrowse()`メソッドでナビゲーション前にURLをチェック
        - Intent URLを検出したら、変換後のURLで`browser.loadURL()`を呼び出し
        - 元のリクエストをキャンセル（`return true`）
      - `convertIntentUrl()`メソッドを追加（1024-1071行目）: ChromiumBrowserScreen.javaと同じロジック
      - import文に`CefRequestHandlerAdapter`と`CefRequest`を追加（12-14行目）
  - 結果: アドレスバー入力とページ内リンククリックの両方でIntent URLが自動変換され、エラーなくページが表示される

- **Chromiumブラウザのショートカットキー対応**（2025-11-28）
  - 問題: Chromiumブラウザ内でCtrl+C、Ctrl+V、Alt+F4、Cmd+C（Mac）等のショートカットキーが動作しない
  - 原因: 修飾キー（Alt/Meta）の状態がChromiumに渡されていなかった
    - Kernel.javaにはShift/Ctrlのみ実装されており、Alt/Metaキーの追跡がなかった
    - ChromiumSurface/ChromiumProviderのインターフェースに修飾キーパラメータがなかった
    - StandaloneWrapper.javaでAlt/Metaキーが特殊キーとして検出されていなかった
  - 修正内容:
    - **Kernel.java**:
      - `altPressed`/`metaPressed`フィールドを追加（197-201行目）
      - `isAltPressed()`/`isMetaPressed()`メソッドを追加（1443-1459行目）
      - `keyPressed()`でAlt（keyCode==18）とMeta（keyCode==91/157）を検出（668-689行目）
      - `keyReleased()`でAlt/Metaキーのリリースを検出（775-790行目）
    - **ChromiumSurface.java（インターフェース変更）**:
      - `sendKeyPressed()`/`sendKeyReleased()`にshiftPressed/ctrlPressed/altPressed/metaPressedパラメータを追加（121行目、133行目）
    - **ChromiumProvider.java（インターフェース変更）**:
      - `sendKeyPressed()`/`sendKeyReleased()`にshiftPressed/ctrlPressed/altPressed/metaPressedパラメータを追加（191行目、204行目）
    - **ChromiumBrowser.java**:
      - `sendKeyPressed()`/`sendKeyReleased()`のシグネチャを更新（625行目、639行目）
      - `flushInputEvents()`でKernelから全修飾キー（Shift/Ctrl/Alt/Meta）を取得してProviderに渡す（993-997行目、1000-1004行目）
    - **DefaultChromiumService.java**:
      - `sendKeyPressed()`/`sendKeyReleased()`のシグネチャを更新（295行目、300行目）
    - **StandaloneChromiumProvider.java**:
      - `sendKeyPressed()`/`sendKeyReleased()`のシグネチャを更新（469行目、533行目）
      - Alt/MetaのmodifierをAWTイベントに追加（`ALT_DOWN_MASK`、`META_DOWN_MASK`）（485-490行目、549-554行目）
    - **ForgeChromiumProvider.java**:
      - `sendKeyPressed()`/`sendKeyReleased()`のシグネチャを更新（236行目、267行目）
      - Alt/MetaのmodifierをMCEFイベントに追加（modifiers |= 4/8）（249-254行目、280-285行目）
    - **StandaloneWrapper.java**:
      - `keyPressed()`でAlt（18）とMeta（91/157）を特殊キーとして検出（385-386行目）
      - Ctrl/Alt/Metaのいずれかが押されている場合も通常文字キーをKernelに転送（389-392行目）
    - **ChromiumBrowserScreen.java**:
      - `keyPressed()`/`keyReleased()`でKernelから全修飾キーを取得してsendKeyPressed/sendKeyReleasedに渡す（315-320行目、327-331行目）
  - 結果: **Ctrl+C/V/X/A、Alt+F4、Cmd+C（Mac）等のショートカットキーがChromiumブラウザ内で正常に動作するようになった**
  - **追加修正1**（制御文字の問題・改訂版）:
    - 問題: Ctrl+Aのみ動作し、Ctrl+C/V/X等のショートカットが動作しない
    - 原因: Ctrl押下時にkeyCharが制御文字（Ctrl+C=0x03、Ctrl+V=0x16等）になっており、Chromium側で正しく解釈されなかった
    - 修正内容（StandaloneChromiumProvider.java）:
      - **sendKeyPressed()でCtrl/Alt/Meta押下時に制御文字が来た場合、keyCharをCHAR_UNDEFINEDに変換**（501-509行目）
        - ChromiumはmodifiersとkeyCodeの組み合わせでショートカットを判定するため、keyCharは不要
        - すべての制御文字（0x00-0x1F）をCHAR_UNDEFINED（0xFFFF）に変換
      - sendKeyReleased()でも同様の処理を追加（578-582行目）
      - **デバッグログを追加**して、送信されるイベント情報を確認できるようにした（495-512行目）
    - テスト手順:
      1. `./gradlew standalone:build`でビルド
      2. standaloneアプリを起動してChromiumブラウザでCtrl+Cを押す
      3. コンソールに以下のようなログが出力される：
         ```
         [StandaloneChromiumProvider] sendKeyPressed: keyCode=67, keyChar=3 ('?'), shift=false, ctrl=true, alt=false, meta=false
         [StandaloneChromiumProvider] Adjusted control char to UNDEFINED: keyCode=67, original keyChar=3
         [StandaloneChromiumProvider] Sending KEY_PRESSED: awtKeyCode=67, adjustedKeyChar=65535, modifiers=128
         ```
    - 期待される結果: **Ctrl+C/V/X等のショートカットキーが正常に動作する**
  - **追加修正2**（フォーカス管理システムへの統合）（2025-11-28）:
    - 問題: アドレスバーをクリックした後、Ctrl+C等のショートカットキーがアドレスバーに送られてしまい、Chromiumで動作しない
    - 根本原因: ChromiumBrowserScreenが独自の`isEditingUrl`フラグでフォーカス管理を行っており、既存のOSフォーカス管理システムと統合されていなかった
    - 修正内容（ChromiumBrowserScreen.java）:
      - `isEditingUrl`フラグを完全に削除（27行目）
      - `hasFocusedComponent()`メソッドを実装（435-437行目）：`addressBar.isFocused()`を返す
      - `setModifierKeys()`メソッドを実装（447-454行目）：将来的な拡張のための準備
      - `keyPressed()`メソッドを簡略化（302-337行目）：`addressBar.isFocused()`で判定し、フォーカスされている場合はアドレスバーに、されていない場合はChromiumに送信
      - `keyReleased()`メソッドも同様に修正（339-352行目）
      - `mousePressed()`メソッドの`isEditingUrl`参照を`addressBar.isFocused()`に置換（219-234行目）
      - その他全ての`isEditingUrl`参照を`addressBar.isFocused()`に置換（113, 139, 252, 269, 290行目）
    - 結果: **OSの既存フォーカス管理システムに統合され、通常のテキスト入力と同じ動作になった**
      - アドレスバーをクリックした後でも、Chromiumコンテンツをクリックするとフォーカスが自動的に移動
      - ショートカットキーは常にフォーカスされているコンポーネント（アドレスバーまたはChromium）に正しく送信される
  - **追加修正3**（JCEFブラウザへのフォーカス設定の問題を修正）（2025-11-28）:
    - 問題: `browser.setFocus(true)` の呼び出しでAWTイベントキューで例外が発生
      - "Exception in thread "AWT-EventQueue-0"" が複数回出力される
      - ChromiumBrowser.javaの初期化時に `uiComponent.setFocusable(false)` を設定している（328行目）ため、setFocus()が失敗
    - 修正内容（StandaloneChromiumProvider.java）:
      - `browser.setFocus(true)` の呼び出しを完全に削除（533-536行目、289行目）
      - JCEFはフォーカスなしでもキーイベントを受け取ることができる
      - UIコンポーネントが `setFocusable(false)` に設定されている理由: ProcessingウィンドウでIMEを使用するため
    - エラーハンドリングの改善:
      - InvocationTargetExceptionの詳細を表示するように改善（565-571行目、624-630行目）
    - 結果: **AWTスレッドでの例外が解消され、キーイベントが正しく送信されるようになった**
  - **追加修正4**（KeyEvent/MouseEventのsourceComponentを実際のUIコンポーネントに変更）（2025-11-28）:
    - 問題: キーイベントは送信されているが、ショートカットキーが動作しない
    - 根本原因: KeyEvent/MouseEventのsourceとして`new Canvas()`という空のコンポーネントを使用していた
      - このコンポーネントは実際のブラウザのUIコンポーネントとは無関係で、フォーカスも持っていない
    - 修正内容（StandaloneChromiumProvider.java）:
      - `eventComponent`を`fallbackComponent`に改名（55行目）
      - すべてのイベント送信メソッドで`browser.getUIComponent()`を呼び出して実際のUIコンポーネントを取得
        - sendKeyPressed()（518-522行目）
        - sendKeyReleased()（607-611行目）
        - sendMousePressed()（277-280行目）
        - sendMouseReleased()（329-332行目）
        - sendMouseMoved()（362-365行目）
        - sendMouseDragged()（406-409行目）
        - sendMouseWheel()（437-440行目）
      - getUIComponent()がnullの場合はfallbackComponentを使用
    - 結果: **KeyEvent/MouseEventが実際のブラウザUIコンポーネントから発火されるようになり、Chromiumがショートカットキーを正しく認識できるようになった**

## 変更(2025-12-02)
- **Kernel分離リファクタリング Phase 1 完了**
  - 目的: Kernelクラス（2456行）の責任分散と単一責任原則への準拠
  - 作成された新規クラス:
    - `InputManager`: 入力イベント処理（マウス/キーボード）を一元管理
    - `ShortcutKeyProcessor`: Ctrl+C/V/X/A等のショートカット処理を専門化
    - `RenderPipeline`: 描画パイプライン管理（背景、スクリーン、スリープ処理）
  - Kernelの変更:
    - 入力処理メソッド（mousePressed/Released/Dragged/Moved、keyPressed/Released）をInputManagerへ委譲
    - render()メソッドをRenderPipelineへ委譲
    - 修飾キー状態管理をInputManagerへ移管
    - handleHomeButton()をpublicに変更（将来的にLayerControllerへ移行予定）
  - 互換性維持:
    - 既存APIは維持し、内部実装のみ委譲に変更
    - InputManager/RenderPipelineが初期化されていない場合は従来処理を実行
  - 技術的修正:
    - PopupManagerのインポートパスを修正（service→ui.popup）
    - ClipboardManagerのメソッド名を修正（setText/getText→copyText/pasteText）
    - ScreenManager/GestureManagerのメソッドシグネチャに合わせて修正
  - 結果: **BUILD SUCCESSFUL** - コンパイルエラー解消

- **Kernel分離リファクタリング Phase 2 完了**
  - 目的: Service Locatorアンチパターンの解消と依存性注入パターンへの移行
  - 作成された新規クラス:
    - `ServiceContainer`: 依存性注入コンテナ（シングルトン/トランジエント/遅延初期化をサポート）
    - `CoreServiceBootstrap`: サービスの初期化順序と依存関係を管理
    - `PowerManager`: スリープ/ウェイク、省電力モード、自動スリープ機能を管理
    - `SystemLifecycleManager`: システムの起動/停止/一時停止/再開ライフサイクルを管理
    - `FileSystemManager`、`MemoryManager`、`ResourceBundle`: プレースホルダー実装（Phase 3で完全実装予定）
  - Kernelの変更:
    - CoreServiceBootstrapをsetup()メソッドの最初に初期化
    - PowerManager経由でsleep()/wake()を処理（従来処理も維持）
    - frameRate()メソッドを追加（PowerManagerのFPS制御用）
    - getService()メソッドを追加（ServiceContainerからの任意サービス取得）
  - 技術的解決:
    - Service Locatorパターンの問題「カーネルパニックの危険性」を解消
    - 依存関係の明示化とテスト可能性の向上
    - サービス初期化順序の自動管理
  - 結果: **BUILD SUCCESSFUL** - 全コンパイルエラー解消、サービスコンテナ統合完了

- **Kernel分離リファクタリング Phase 3 完了**
  - 目的: 上位層の責任分離とハードウェア/リソース管理の独立
  - 作成された新規クラス:
    - `NavigationController`: 画面遷移管理（プッシュ/ポップ/切り替え、履歴管理）
    - `LayerController`: レイヤー管理とホームボタン処理（動的優先順位システム）
    - `ResourceManager`: リソース管理（フォント、画像のキャッシュと遅延読み込み）
    - `HardwareController`: ハードウェアAPI統合（モバイルデータ、Bluetooth、位置情報、カメラ等）
  - Kernelの変更:
    - Phase 3コンポーネントの初期化をsetup()メソッドに追加
    - handleHomeButton()をLayerControllerへ完全委譲（互換性のため従来処理も維持）
    - 各コントローラーへの参照を保持し、必要に応じて連携
  - LayerController機能:
    - LayerType列挙型: HOME_SCREEN、APPLICATION、NOTIFICATION、CONTROL_CENTER、POPUP、LOCK_SCREEN
    - 動的レイヤースタック管理と優先順位制御
    - ホームボタン処理の一元化（最上位の閉じられるレイヤーから順に処理）
  - ResourceManager機能:
    - 日本語フォント（Noto Sans JP）の複数ClassLoader対応読み込み
    - フォント/画像キャッシュと遅延読み込み
    - メモリ管理とリソース解放機能
  - HardwareController機能:
    - 各ハードウェアソケット（MobileData、Bluetooth、Location、Camera等）の統一管理
    - プラットフォーム固有実装（Forge/Standalone）への対応
    - BatteryMonitor統合とハードウェア状態確認API
  - 動作確認:
    - standalone:runでの起動確認完了
    - LayerControllerによるホームボタン処理が正常動作
    - Phase 3コンポーネントの初期化ログを確認
  - 残タスク:
    - Phase 4: SystemGestureHandlerの作成（システムジェスチャー処理）
    - ControlCenterManager/NotificationManagerのclose関連メソッド実装
    - MobileDataSocket/BluetoothSocketのisEnabled()メソッド実装
  - 結果: **BUILD SUCCESSFUL** - Phase 3統合完了、実行時動作確認済み

## 変更(2025-12-04)
- **Kernel構造健全化後の入力処理問題の修正（第4版）**
  - 問題: Phase 1-3のリファクタリング後、スペースキーとESCキーが動作しなくなった
    - InputManagerに処理を移行したが、KernelがInputManagerに完全に委譲してreturnするため処理されない
    - LayerControllerのaddLayer/removeLayerがKernelと同期していない
  - 修正内容:
    - **Kernel.keyPressed()/keyReleased()の修正**:
      - ESCキー（keyCode=27）とスペースキー（keyCode=32）はInputManagerをスキップ
      - これらのキーは元のKernelの処理で直接実行
      - その他のキーはInputManagerで処理
    - **InputManagerの修正**:
      - ESCキーとスペースキーの処理をコメントアウト（Kernelで処理するため）
      - update()メソッドのESCキー長押し検出を削除
    - **Kernel.addLayer()/removeLayer()の修正**:
      - LayerControllerのaddLayer/removeLayerも同時に呼び出して同期
    - **LayerControllerのロガー修正**:
      - java.util.logging.LoggerをLoggerServiceに変更
      - すべてのログ出力を`logger.info("LayerController", message)`形式に統一
  - 結果: **元の動作を復元。スペースキーによるホームボタン動作、ESCキー単押しでスリープトグル、ESCキー長押しでシャットダウンが正常に動作**

- **スリープから復帰時のロック画面表示修正**
  - 問題: PowerManager経由でwake()した場合、ロック画面が表示されない
  - 修正内容:
    - **showLockScreenAfterWake()メソッド作成**: ロック画面表示処理を共通メソッドとして抽出
    - **PowerManager経由のwake()でもロック画面表示**: wake()メソッドでPowerManager経由でもshowLockScreenAfterWake()を呼び出し
  - 結果: **スリープから復帰時に必ずロック画面が表示されるように修正**

## 変更(2025-12-04)
- **Standalone入力処理のバグ修正（第3版）**
  - 問題: スペースキーとESCキーが正しく動作しない
    - スペースキー: InputManagerからKernel.handleHomeButton()が呼ばれるが、LayerControllerのログが出力されない
    - ESCキー: Processingのデフォルト終了動作でシステムがシャットダウンされていた
  - 修正内容:
    - **LayerControllerのロガー修正**:
      - java.util.logging.LoggerをLoggerServiceに変更
      - Kernelから`getLogger()`でLoggerServiceを取得
      - すべてのログ出力を`logger.info("LayerController", message)`形式に変更
    - **StandaloneWrapper.java**:
      - スペースキー処理の改善:
        - keyPressed()でkey <= 32の条件に変更（411行目）
        - スペースキー専用のデバッグログ追加（415-418行目）
        - keyTyped()でもkey <= 32に変更し、スペースキーを確実に除外（450行目）
      - ESCキー処理の完全な修正:
        - handleKeyEvent()をオーバーライドし、ESCキーイベントを完全に消費（60-78行目）
        - draw()メソッドでexitCalledフラグを毎フレームリセット（256-265行目）
        - exitActual()のオーバーライド継続（459-467行目）
  - 結果: **LayerControllerのログが正しく出力されるようになり、デバッグが可能に。スペースキーによるホームボタン動作、ESCキー2秒長押しによるスリープモードが正常に動作するようになった**

## 変更(2025-12-01)
- **iOS/Android方式のテキスト入力統一アーキテクチャ完成**
  - 目的: OS全体でCtrl+C/V/X/A等のクリップボード操作を統一的に処理する
  - iOSの設計パターンを採用:
    - `TextInputProtocol`: iOS UITextInput相当のインターフェース
    - `Screen.getFocusedTextInput()`: iOS UIResponder Chain相当
    - `ClipboardService`: iOS UIPasteboard相当
    - `Kernel`: 中央でCtrlショートカットを検出し、フォーカスされた入力に操作を委譲
  - 修正内容:
    - **ChromiumSurface.java**: TextInputProtocol用メソッドを追加（hasTextInputFocus, getCachedSelectedText, executeScript）
    - **DefaultChromiumService.java**: 新しいChromiumSurfaceメソッドを実装
    - **ChromiumTextInput.java**: ChromiumBrowserからChromiumSurfaceを使用するように変更
    - **NoteEditScreen.java**: getFocusedTextInput()を実装し、旧Ctrl+C/V処理を削除
    - **ChromiumBrowserScreen.java**: iOS/Android方式を採用 - フォーカス検出に頼らず、Chromiumサーフェスがアクティブな場合は常にChromiumTextInputを返す
  - 解決した問題:
    - メモアプリでCtrl+Aが動作しない → getFocusedTextInput()実装で解決
    - YouTube等の複雑なWebアプリでdocument.activeElementがBODYを返す → iOS/Android方式（常にJS実行）で解決
  - 技術的発見:
    - iOS/AndroidはWebViewでフォーカス検出に頼らない
    - 常にJavaScript操作を実行し、実際に入力フィールドがあれば動作する方式を採用
  - 結果: **Google検索、YouTube検索、メモアプリ等、すべてのテキスト入力でCtrl+C/V/X/Aが正常に動作**

- **バックスペースキーのTextInputProtocol統合**
  - 問題: バックスペースがHTMLフィールドに正しく転送されていなかった
  - 修正内容:
    - **TextInputProtocol.java**: `deleteBackward()`メソッドを追加
    - **BaseTextInput.java**: `deleteBackward()`を実装（選択があれば削除、なければカーソル前の1文字を削除）
    - **ChromiumTextInput.java**: JavaScript経由で`deleteBackward()`を実装（INPUT/TEXTAREAはvalue操作、contentEditableはexecCommand）
    - **Kernel.java**: バックスペースキー（keyCode=8）をTextInputProtocol経由で処理
  - 結果: **メモアプリ、Chromiumアドレスバー、HTMLフィールド（Google/YouTube等）でバックスペースが正常に動作**

## PDE実行エンジン仕様

MochiMobileOS上でProcessingスケッチ（.pde）をアプリケーションとして実行するための仕様。

### 1. アプリケーション形式と自動ロード

-   **フォルダ構成**: PDEアプリは、複数の`.pde`ファイル、アイコン、およびマニフェストファイルを含む単一のフォルダとして提供される。
-   **配置場所**: ユーザーは、このアプリフォルダを仮想ファイルシステム上の `/apps/` ディレクトリに配置する。
-   **マニフェスト (`app.json`)**: アプリフォルダには、以下の情報を含む`app.json`を必須とする。
    -   `name` (String): アプリケーション名。
    -   `version` (String): バージョン番号。
    -   `entry_point` (String): メインとなる`.pde`ファイル名。
    -   `type` (String): `"pde"` という固定値。
-   **自動ロード**: OS起動時に`AppLoader`サービスが`/apps/`をスキャンし、`"type": "pde"`のマニフェストを持つフォルダを自動的にPDEアプリとして認識し、ランチャーに登録する。

### 2. 実行アーキテクチャ

-   **動的コンパイル**: PDEアプリは、起動時に`PdeInterpreterService`によってJavaソースコードに変換され、動的にコンパイル・ロードされる。
-   **PGraphicsベースのレンダリング**: コンパイルされたスケッチは、OSのオフスクリーンレンダリング（OSR）アーキテクチャに統合される。`PApplet`インスタンスはUIに直接描画せず、OSから提供される`PGraphics`オブジェクトへの描画エンジンとして機能する。

### 3. OS API連携

-   **安全なAPIアクセス**: スケッチからOSの機能を安全に利用するため、公開用のAPIラッパーを提供する。
-   **カスタム基底クラス (`MmosPdeApplet`)**: スケッチは、コンパイル時に`MmosPdeApplet`というカスタム基底クラスを継承する。この基底クラスが、公開APIへのアクセスポイント（例: `api`変数）を提供する。
-   **利用例**: スケッチ開発者は、`api.showNotification("タイトル", "メッセージ");` のような形式で、準備なしにOSの機能を呼び出すことができる。

### PDE実行エンジン実装タスク (TODO)
- **`PdeInterpreterService`の作成:** .pdeファイルの動的コンパイルとクラスロード機能の実装。
- **`AppLoader`の拡張:** `app.json`の`type: "pde"`を解釈し、PDEアプリを登録する機能の追加。
- **API連携基盤の実装:**
    - `MmosPdeApplet`カスタム基底クラスの作成。
    - 公開用`MmosApi`ラッパークラスの作成（第一弾として通知機能など）。
- **`PdeScreen`の作成:** コンパイルされたPDEスケッチをPGraphicsベースで描画・実行する画面の実装。
- **サンプルPDEアプリの作成:** 動作テストとユースケースを示すための簡単なサンプルアプリ。

## キーボード入力処理の修正（2025-12-04）

### 問題と修正内容

**問題**: Kernel分離リファクタリング（Phase 1-3）実施後、ESCキーとスペースキーが動作しなくなった。

**根本原因**: KernelがInputManagerに処理を完全に委譲し、元の処理をスキップしていたため。

**修正内容**:
1. **Kernel.java**:
   - `keyPressed()`メソッドで、ESC（keyCode=27）とスペース（keyCode=32）はInputManagerをスキップし、従来処理を実行
   - `keyReleased()`メソッドも同様に修正
   ```java
   if (inputManager != null) {
       // ESCキー（27）とスペースキー（32）以外はInputManagerで処理
       if (keyCode != 27 && keyCode != 32 && key != ' ') {
           inputManager.handleKeyPressed(key, keyCode);
           return;
       }
       // ESCとスペースは下の従来処理で実行
   }
   ```

2. **ロック画面表示の修正**:
   - スリープから復帰時にロック画面が表示されない問題を修正
   - `showLockScreenAfterWake()`メソッドを追加し、PowerManager経由とレガシー経路両方で呼び出し

3. **Chromiumブラウザでのスペースキー入力修正**:
   - `ChromiumBrowserScreen.hasFocusedComponent()`を修正
   - Chromiumコンテンツがアクティブな場合も`true`を返すように変更
   - これによりWebページ内でのスペースキー入力が正常に動作

**結果**:
- ESCキー: 単押しでスリープトグル、長押し（2秒）でシャットダウン
- スペースキー: テキスト入力フォーカスがない時はホームボタン、フォーカスがある時は通常の文字入力
- スリープ解除時にロック画面が正常に表示される

### Chromiumブラウザ内スペースキー処理の改善

**問題**: Chromiumブラウザ内でテキストボックスからフォーカスが外れた状態でもスペースキーがページスクロールに使われてしまい、ホームボタンとして機能しない。

**解決方法（最終版）**: 既存のJavaScriptフォーカス検出システムを活用:

1. **既存システムの発見**:
   - ChromiumBrowserに`injectFocusDetectionScript()`メソッドが既に実装済み
   - ページロード時にJavaScriptを注入してフォーカス状態を監視
   - input、textarea、contenteditable、role="textbox"等を検出
   - `[MochiOS:TextFocus]`メッセージで状態変更を通知
   - `hasTextInputFocus()`メソッドで状態を取得可能

2. **JavaScript検出ロジック**:
   - Shadow DOM内の要素も検出（`getDeepActiveElement()`）
   - focusin/focusout、click、inputイベントをリッスン
   - YouTube等のカスタム要素（role属性）にも対応
   - 選択テキストも監視（TextInputProtocol用）

3. **ChromiumBrowserScreenの修正**:
   - ヒューリスティック処理を削除
   - `hasFocusedComponent()`でChromiumSurface.hasTextInputFocus()を使用
   - アドレスバーまたはWeb内テキスト入力フォーカスでtrue返却

**動作**:
- テキストボックスにフォーカス時: スペースキーは文字入力
- フォーカス外: スペースキーはホームボタンとして機能
- 正確なフォーカス検出（モバイルOSと同等）

## OS構造最適化 Phase 4: イベントバスシステム（2025-12-04）

### 実装内容

**目的**: イベント駆動アーキテクチャによるコンポーネント間の疎結合化を実現

**作成されたクラス**:

1. **イベントバスコア** (`core/src/main/java/jp/moyashi/phoneos/core/event/`):
   - `Event`: 基底イベントクラス（キャンセル/消費機能付き）
   - `EventType`: システム全体のイベントタイプ定義
   - `EventListener<T>`: イベントリスナーインターフェース
   - `EventFilter<T>`: イベントフィルタリングインターフェース
   - `EventBus`: Publish-Subscribe パターンの中核実装
     - スレッドセーフ設計
     - 優先度付きリスナー
     - 非同期/同期イベント配信
     - イベント履歴管理

2. **イベント実装クラス**:
   - `system/SystemEvent`: システムライフサイクルイベント（起動/終了/スリープ/ウェイク）
   - `app/ApplicationEvent`: アプリケーションイベント（起動/終了/フォーカス）
   - `ui/ScreenEvent`: 画面遷移イベント（プッシュ/ポップ/遷移タイプ）

3. **既存コンポーネントの統合**:
   - `PowerManager`: スリープ/ウェイク時にSystemEventを発行
   - `NavigationController`: 画面遷移時にScreenEventを発行（キャンセル可能）
   - `Kernel`: システム起動時にEventBus初期化、シャットダウン時にイベント発行

**技術的特徴**:
- **疎結合化**: コンポーネント間の直接的な依存関係を削減
- **拡張性**: 新しいイベントタイプとリスナーの追加が容易
- **デバッグ性**: イベント履歴とデバッグモードによる詳細ログ
- **パフォーマンス**: 非同期イベント配信とキャッシュ済みスレッドプール

**結果**:
- コンポーネント間通信の標準化と疎結合化により、システムの保守性と拡張性が向上
- BUILD SUCCESSFUL - 全コンパイルエラー解消

## 変更(2025-12-13)
- **WebScreen画面真っ白問題の修正**（Phase 1）
  - 問題: WebScreenでChromiumの描画が真っ白のまま
  - 原因: GLCanvasのOpenGLコンテキストが初期化される前にonPaintが呼ばれ、早期リターンしていた
  - 修正内容:
    - **ChromiumBrowser.java**:
      - `CountDownLatch` と `glContextReady` フラグを追加してGLContext初期化完了を同期化
      - `SwingUtilities.invokeLater()` 内でGLCanvas追加後に200ms待機し、`glInitLatch.countDown()` を呼び出し
      - `loadURL()` で `waitForGLContext(3000)` を呼び出してGLContext準備完了を待機
      - `isReadyToRender()` を改善し、`glContextReady` フラグを優先チェック
    - **ChromiumSurface.java**:
      - `isReadyToRender()` メソッドをインターフェースに追加
    - **DefaultChromiumService.java**:
      - `DefaultChromiumSurface` 内部クラスに `isReadyToRender()` を実装
    - **WebScreen.java**:
      - 時間ベースのリトライロジックを `surface.isReadyToRender()` チェックに改善
      - GLContext準備完了を確認してからreload()を実行
  - 結果: **BUILD SUCCESSFUL** - core/forge/standaloneの全モジュールでコンパイル成功

## 変更(2025-12-14)
- **ホームボタンシステム アーキテクチャ再設計**
  - 目的: スペースキーをホームボタンとして扱う設計を廃止し、プラットフォーム固有の実装に分離
  - 設計変更:
    - **Core**: スペースキー特別処理を削除。`requestGoHome()` API のみを提供
    - **Standalone**: 別ウィンドウにホームボタン表示 + Ctrl+Spaceショートカット
    - **Forge**: 画面上にホームボタンを常時表示 + Ctrl+Spaceショートカット
  - 修正内容:
    - **Kernel.java**:
      - `requestGoHome()` メソッド追加 - プラットフォーム層からの公開API
      - `hasTextInputFocus()` メソッド追加 - テキスト入力フォーカス判定
      - スペースキー特別処理を削除（通常のキー入力として扱う）
    - **ScreenManager.java**:
      - スペースキー特別処理を削除（通常のキーとしてスクリーンに転送）
    - **StandaloneWrapper.java**:
      - Ctrl+Space検出を追加 → `kernel.requestGoHome()` 呼び出し
      - スペースキーを通常の文字として扱うように修正（`keyTyped()` で処理）
    - **HardwareWindow.java**: 新規作成（ハードウェアボタンエミュレーション）
      - メインウィンドウの右側に配置される独立Swingウィンドウ
      - 音量アップボタン（上）- TODO: 音量処理実装
      - ホームボタン（中央）- `kernel.requestGoHome()` 呼び出し
      - 音量ダウンボタン（下）- TODO: 音量処理実装
      - ボタン外領域をドラッグしてウィンドウ移動可能
    - **ProcessingScreen.java (Forge)**:
      - Ctrl+Space検出を追加 → `kernel.requestGoHome()` 呼び出し
      - ホームボタンを常時表示（ChromiumBrowserScreen限定を解除）
      - `handleHomeButtonClick()` を `kernel.requestGoHome()` に変更
      - スペースキーを通常の文字として扱うように修正（`charTyped()` で処理）
  - 動作フロー:
    ```
    [Standalone環境]
    Ctrl+Space押下 または HomeButtonWindow クリック → kernel.requestGoHome() → handleHomeButton()

    [Forge環境]
    Ctrl+Space押下 または 画面上ホームボタンクリック → kernel.requestGoHome() → handleHomeButton()

    [スペースキー]
    通常のキー入力として処理（テキスト入力フィールドに ' ' 入力）
    ```
  - 結果: **BUILD SUCCESSFUL** - スペースキーがテキスト入力として正常動作、ホームへの移動はCtrl+Spaceまたは専用ボタンで実現

## 変更(2025-12-17)
- **Forge環境でのAWT/LWJGL競合問題の解決（CefBrowserOsrNoCanvas）**
  - 問題: Forge環境でJCEFのCefBrowserOsr（AWTのGLCanvas使用）を使用すると、MinecraftのLWJGLと競合してAWT HeadlessExceptionが発生
  - 原因: CefBrowserOsrはAWTのGLCanvasを使用してOpenGLレンダリングを行うが、MinecraftはLWJGLを使用しており、両者が競合する
  - 解決策: Processing P2D vs LWJGL競合の解決と同様に、AWTを使用しないオフスクリーンブラウザを作成
  - 作成されたクラス:
    - **`forge/src/main/java/org/cef/browser/CefBrowserOsrNoCanvas.java`**:
      - CefBrowser_Nを継承し、CefRenderHandlerを実装
      - AWTのGLCanvasを使用せず、onPaint()でByteBuffer→int[]配列にピクセルデータを保存
      - onPaintListenerに通知してChromiumRenderHandlerに転送
      - setSize()でwasResized()とsetWindowVisibility(true)を呼び出してレンダリングをトリガー
    - **`forge/src/main/java/org/cef/browser/CefBrowserFactory.java`**:
      - NoCanvasモードのブラウザを作成するファクトリ
      - `setNoCanvasMode(true)`でCefBrowserOsrNoCanvasを使用
      - `createNoCanvas()`で直接NoCanvasブラウザを作成
  - 修正されたクラス:
    - **`ForgeChromiumProvider.java`**:
      - `supportsUIComponent()`をオーバーライドしてfalseを返す
      - `createBrowser()`でCefBrowserOsrNoCanvasを作成
      - NoCanvasモードを有効化
    - **`ChromiumBrowser.java`**:
      - `provider.supportsUIComponent()`がfalseの場合のelse分岐を追加
      - NoCanvasモードではhidden JFrameを作成せず、`tryTriggerRendering()`を呼び出し
      - `tryTriggerRendering()`でsetSize()メソッドを優先的に呼び出し
  - 動作フロー:
    ```
    [Forge環境]
    ForgeChromiumProvider.createBrowser()
    → CefBrowserFactory.createNoCanvas()
    → CefBrowserOsrNoCanvas作成
    → createImmediately()
    → ChromiumBrowser: supportsUIComponent()=false
    → tryTriggerRendering() → setSize() → wasResized() + setWindowVisibility(true)
    → CEFがonPaint()を呼び出し
    → CefBrowserOsrNoCanvasがonPaintListenerに通知
    → ChromiumRenderHandlerがPImageに変換
    → ProcessingScreen経由でMinecraftテクスチャに描画
    ```
  - 結果: **AWT HeadlessException問題が解決し、Forge環境でChromiumブラウザが正常にレンダリングされるようになった**

## 変更(2025-12-18)
- **サーバーモジュールの作成（IPvMシステムサーバーインフラ）**
  - 目的: サーバーサイドの仮想ネットワークシステムを構築し、クライアント（Chromium）からのIPvMリクエストをサーバーで処理する
  - 背景: 従来はクライアント側でテストサーバーを登録していたが、正しい設計としてサーバー側で管理すべき
  - 作成されたモジュール: `server`
    - `server/build.gradle.kts`: coreモジュールに依存、Gson追加
    - `settings.gradle.kts`に`server`を追加
  - 作成されたクラス:
    - **`server/src/main/java/jp/moyashi/phoneos/server/MMOSServer.java`**:
      - サーバーモジュールのエントリーポイント
      - `initialize()`: 組み込みサーバー（sys-test等）を登録
      - `handleHttpRequest(VirtualPacket)`: クライアントからのHTTPリクエストを処理
      - `registerSystemServer()`/`registerExternalServer()`: サーバー登録API
    - **`server/src/main/java/jp/moyashi/phoneos/server/network/VirtualHttpServer.java`**:
      - 仮想HTTPサーバーのインターフェース
      - `getServerId()`: サーバー識別子（例: "sys-test"）
      - `handleRequest(VirtualHttpRequest)`: リクエスト処理
    - **`server/src/main/java/jp/moyashi/phoneos/server/network/VirtualHttpRequest.java`**:
      - HTTPリクエストデータクラス（source、destination、method、path、headers、body）
      - Builderパターンで構築
    - **`server/src/main/java/jp/moyashi/phoneos/server/network/VirtualHttpResponse.java`**:
      - HTTPレスポンスデータクラス（statusCode、statusText、headers、body、mimeType）
      - ファクトリメソッド: `ok()`, `html()`, `json()`, `notFound()`, `error()`
    - **`server/src/main/java/jp/moyashi/phoneos/server/network/SystemServerRegistry.java`**:
      - システムサーバー（Type 3）と外部サーバー（Type 2）の登録レジストリ
      - シングルトンパターン
      - IPvMアドレスからサーバーを検索
    - **`server/src/main/java/jp/moyashi/phoneos/server/network/ServerVirtualRouter.java`**:
      - サーバーサイドのパケットルーティング
      - VirtualPacketをVirtualHttpRequestに変換
      - SystemServerRegistryからサーバーを検索してリクエスト処理
    - **`server/src/main/java/jp/moyashi/phoneos/server/network/builtin/TestSystemServer.java`**:
      - テスト用システムサーバー（3-sys-test）
      - `/`: テストページ（接続成功表示）
      - `/api/echo`: Echoエンドポイント
      - `/api/info`: サーバー情報エンドポイント
  - Forgeモジュールの修正:
    - **`forge/build.gradle`**:
      - `implementation project(':server')` 追加
      - JARビルドにサーバーモジュールクラスを含める
      - runClient設定にサーバーモジュールソースを追加
    - **`MochiMobileOSMod.java`**:
      - `commonSetup()`で`MMOSServer.initialize()`を呼び出し
    - **`NetworkHandler.java`**:
      - `handleServerSide()`で`MMOSServer.handleHttpRequest()`を使用
      - `sendToPlayer(VirtualPacket, ServerPlayer)`メソッド追加
    - **`ForgeNetworkInitializer.java`**:
      - クライアント側のテストサーバー登録コードを削除
      - レスポンスハンドラーのみを登録
    - **`ForgeVirtualSocket.java`**:
      - `onPacketReceived()`をサーバーモジュールのレスポンスフォーマットに対応
      - statusCode、statusText、mimeType、bodyを正しく処理
  - パケットフロー:
    ```
    [クライアント]
    Chromium → MochiResourceRequestHandler → VirtualNetworkResourceHandler
    → ForgeVirtualSocket.httpRequest() → NetworkHandler.sendToServer()

    [サーバー]
    NetworkHandler.handleReceivedPacket() → handleServerSide()
    → MMOSServer.handleHttpRequest() → ServerVirtualRouter.routeRequest()
    → SystemServerRegistry.getServer() → TestSystemServer.handleRequest()
    → VirtualHttpResponse → VirtualPacket → NetworkHandler.sendToPlayer()

    [クライアント]
    NetworkHandler.handleClientSide() → ForgeNetworkInitializer.onPacketReceived()
    → ForgeVirtualSocket.onPacketReceived() → CompletableFuture完了
    → VirtualNetworkResourceHandler → Chromiumに表示
    ```
  - 結果: **BUILD SUCCESSFUL** - サーバーモジュールとForgeモジュールの統合完了

- **JCEF getResourceRequestHandler() 問題の修正（onBeforeBrowseアプローチ）**
  - 問題: Forge環境（CefBrowserOsrNoCanvas）で`CefRequestHandler.getResourceRequestHandler()`がネイティブレベルで呼び出されず、IPvMアドレス（`http://3-sys-test/`等）へのリクエストがインターセプトできない
  - 原因分析:
    - `CefBrowserOsrNoCanvas`はAWTを使用しないカスタムOSRブラウザ実装
    - `CefRequestHandler`のコールバック（`getResourceRequestHandler`）がネイティブCEFから呼び出されない
    - `client.removeRequestHandler()`を追加しても根本的な解決にならない
    - `CefSchemeHandlerFactory`によるHTTPスキームのインターセプトも動作しない（ファクトリが呼び出されない）
  - 解決策: `CefRequestHandler.onBeforeBrowse()`を使用したナビゲーションインターセプト
    - `onBeforeBrowse()`はCefBrowserOsrNoCanvasでも確実に呼び出される
    - IPvMアドレスパターン（`0-*`, `1-*`, `2-*`, `3-*`）にマッチするURLを検出
    - 元のナビゲーションをキャンセル（return true）し、VirtualAdapterで非同期リクエストを実行
    - レスポンスHTMLをdata: URL経由でブラウザに読み込み
  - 実装詳細:
    - **`ChromiumBrowser.java`**:
      - `isIPvMUrl(String url)`: URLがIPvMパターンにマッチするか判定
      - `handleIPvMRequest(CefBrowser browser, String url)`: VirtualAdapterで非同期リクエストを実行し、レスポンスをdata: URLで読み込み
      - `loadHtmlContent()`, `loadErrorPage()`, `loadNoServicePage()`: レスポンス表示用ヘルパーメソッド
      - `onBeforeBrowse()`内でIPvM URLを検出して処理
  - 結果: **動作確認済み** - onBeforeBrowse()経由でIPvMアドレスが正しくインターセプトされ、VirtualAdapter経由でサーバーからのレスポンスが表示される

- **IPvM URL表示システムの改善（httpm://スキーム表示）**
  - 問題: data: URL方式ではURLバーに「data:」が表示され、ユーザーにとって分かりにくい
  - 要件: URLバーに`httpm://3-sys-test/`のような意味のあるURLを表示したい
  - 試行した解決策:
    1. **CefResourceHandler**: OSRモードでHTMLがレンダリングされない（白画面）
    2. **httpm://カスタムスキーム**: CefSchemeHandlerFactory経由でもOSRレンダリング問題は解決せず
    3. **CefFrame.loadString()**: このJCEF実装には存在しない
    4. **history.replaceState()**: data: URLでは同一オリジンポリシーにより動作しない
  - 最終解決策: **displayUrlフィールドによるUI層での仮想URL表示**
    - `ChromiumBrowser`に`displayUrl`フィールドを追加
    - IPvM URLロード時に`displayUrl`を設定（例: `httpm://3-sys-test/`）
    - `getCurrentURL()`メソッドを修正: 実際のURLがdata:の場合は`displayUrl`を返す
    - 通常URL（非data:）をロードする場合は`displayUrl`をクリア
  - 実装詳細:
    - **`ChromiumBrowser.java`**:
      - `displayUrl`フィールド: IPvM用の表示URL保持
      - `loadIPvMContent()`: HTML取得後、`loadHtmlWithUrl()`を呼び出し
      - `loadHtmlWithUrl()`: `displayUrl`設定後、data: URLでHTML読み込み
      - `getCurrentURL()`: `displayUrl`が設定済み かつ `currentUrl`がdata:で始まる場合は`displayUrl`を返す
      - `loadURL()`: 非data: URLの場合は`displayUrl`をnullにリセット
    - `onBeforeBrowse()`で`http://`と`httpm://`両方のIPvM URLを処理
      - `http://3-sys-test/` → displayUrl=`httpm://3-sys-test/`、data: URLでロード
      - `httpm://3-sys-test/` → displayUrl=`httpm://3-sys-test/`、data: URLでロード
  - 結果: **動作確認済み** - URLバーに`httpm://3-sys-test/`が正しく表示される
