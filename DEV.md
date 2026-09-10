# MochiMobileOS 開発ドキュメンチE
こ�Eドキュメント�EMochiMobileOSの技術的な概要と吁E���Eへのポ�Eタルです、E
## 開発ドキュメント一覧

より詳細な技術仕様や実裁E��つぁE��は、以下�E吁E��キュメントを参�Eしてください、E
- **[00_Project_Overview.md](./docs/development/00_Project_Overview.md)**
  - プロジェクト�E体�E概要、アーキチE��チャ、主要機�EにつぁE��説明します。本プロジェクト�E教育目皁E��の利用も想定して開発されてぁE��す、E
- **[01_Chromium_Integration.md](./docs/development/01_Chromium_Integration.md)**
  - Chromium (JCEF/MCEF) の統合アーキチE��チャ、パフォーマンス最適化�E経緯、UI設計につぁE��詳述します、E
- **[02_WebView_System.md](./docs/development/02_WebView_System.md)**
  - JavaFX WebViewベ�EスのWebレンダリングシスチE��に関する惁E��です、E
- **[03_Virtual_Internet.md](./docs/development/03_Virtual_Internet.md)**
  - MOD冁E��想ネットワーク�E�EPvM�E��E仕様と実裁E��つぁE��説明します、E
- **[04_Forge_Integration.md](./docs/development/04_Forge_Integration.md)**
  - Minecraft Forge MODとしての連携部刁E�EアーキチE��チャと実裁E��つぁE��説明します、E
- **[05_Hardware_API.md](./docs/development/05_Hardware_API.md)**
  - 仮想ハ�Eドウェア�E�センサー、E��信、バチE��リー等）�E抽象化APIに関する仕様です、E
- **[06_External_App_API.md](./docs/development/06_External_App_API.md)**
  - 外部MOD開発老E��け�Eアプリケーション開発API�E�パーミッション、インチE��ト等）につぁE��説明します、E
- **[07_GUI_Component_Library.md](./docs/development/07_GUI_Component_Library.md)**
  - 再利用可能なUIコンポ�Eネントライブラリの設計と使用方法につぁE��説明します、E
- **[08_Font_System.md](./docs/development/08_Font_System.md)**
  - 多言語（特に日本語）フォント�E表示を実現するためのフォントシスチE��につぁE��説明します、E  - **絵斁E��サポ�EチE* (2025-12-23追加): Noto Emojiフォントによるモノクロ絵斁E��表示機�E

- **[09_Known_Issues_And_TODO.md](./docs/development/09_Known_Issues_And_TODO.md)**
  - 現在把握してぁE��既知の問題点と、今後�E開発タスク�E�EODO�E�を管琁E��ます、E

## ���
- REIN ��JAR�� mochi_os_data/unknown/apps �ɑ��݂��邪 AppLoader �œǂݍ��܂ꂸ�z�[���ɏo�Ȃ��i2026-02-07 �������j
- App Store �ŃC���X�g�[�������A�v�����z�[����ʂɔ��f����Ȃ����Ƃ�����iAppStoreScreen ���̍X�V���K�v�A2026-02-07�j
- REIN �̃C���X�g�[���{�^�������Łu����/���s�v�\���ɂȂ邪�Adebug.log �ł̓C���X�g�[���������O���o��i2026-02-07 �񍐁A�v�����j
- �p�^�[�����b�N���������E�ǂ��炩�̃N���b�N�ł����������Ȃ����Ƃ�����iForge���A2026-02-08�񍐁j
- ~~AppStore Updateボタンが更新後も「UPDATE」のまま「Installed」に変わらない（2026-02-11報告）~~ **修正済み（2026-02-11）**
  - 原因: `IApplication.getVersion()` がデフォルト `"1.0.0"` を返すため、リポジトリJSONのバージョンと常に不一致
  - 修正: `AppStoreScreen` に `installedVersions` マップを追加し、VFS永続化（`system/appstore_installed_versions.json`）でバージョンを独自追跡
- ~~ディメンション移動（ネザー・エンド等）時にMMOSが完全停止するバグ（2026-02-12報告、2026-02-14再修正）~~ **修正済み（2026-02-14）**
  - 根本原因1: `onWorldUnload()` の `mc.level != event.getLevel()` チェックがイベント発火順序に依存し、`/tp`コマンド等で順序が逆転するとディメンション移動を「ワールド切断」と誤判定
  - 根本原因2: `shutdown(false)` の非同期スレッド（1.5秒後にEventBus.shutdown()）が新Kernel作成後に実行され、EventBusシングルトンが無効化されるレースコンディション
  - 修正1: イベントベースのシャットダウン判定を完全に廃止。`onWorldLoad()` は `sharedKernel != null` なら何もしない（ディメンション移動）。切断検出は `onClientTick()` で `mc.level == null` を検査（ディメンション移動中は old→new 遷移で null にならない）
  - 修正2: シャットダウンは常に `shutdownKernelSafe()` で `shutdown(true)` を使用し、非同期スレッドを起動しない
  - 修正3: `Kernel.java` に `activeKernelId` ガードを追加し、新Kernelが作成済みの場合は旧Kernelの非同期スレッドがEventBusシャットダウンをスキップ
- ~~ロチE��画面が稀に操作不�Eになる！E026-02-06報告）~~ **修正済み�E�E026-02-08�E�E*
  - 修正1: LockScreenをKernelフィールドでシングルトン化！E+ �E�。古ぁE��ンスタンスのGestureListenerリーク問題を解涁E
  - 修正2: でlock/wake時にControlCenter/NotificationManagerを強制hide。がfalseになる問題を解涁E

- **ホットリスタート機能（2026-02-08実装）**
  - Forge環境でワールドからリログせずにMMOS（Kernel）を再起動する機能
  - Settings > About System > "Restart OS" ボタンから実行可能
  - 実装: `Kernel.requestRestart()` → `SmartphoneBackgroundService.restartKernel()` → `createKernel(worldId)` で同じワールドIDで再作成
  - `Kernel.shutdown(boolean forRestart)`: forRestart=true でシャットダウン画面描画・非同期終了をスキップ
  - ProcessingScreenは閉じて再度開くことで新しいKernelを自動取得
  - VFSデータ（インストール済みアプリ、設定）はワールドIDが同じため引き継がれる
  - CefApp idempotent化（2026-02-05実装済み）によりChromiumの再利用基盤を活用

## 開発ログ

日、E�E詳細な開発ログは `devlogs` チE��レクトリに格納されてぁE��す、E
- [devlogs/2025-10-14.md](./devlogs/2025-10-14.md)
- [devlogs/2025-10-13.md](./devlogs/2025-10-13.md)

## プロジェクト仕様！EI/UX・チE�Eマ�Eレジストリ�E�E
- チE��インシスチE���E�デザイント�Eクン�E�を導�Eし、色・角丸・スペ�Eシング等を変数匁E  - セマンチE��チE��カラー: `color.primary`, `color.background`, `color.surface`, `color.on_surface`, `color.border` など
  - モーチE ライチEダーク/オート、コントラスチE normal/high、角丸スケール: compact/standard/rounded
- レジストリ�E�設定シスチE���E�E  - `SettingsManager` を拡張�E�EFS JSON永続化、リスナ�E通知�E�E  - 保存�E: `system/settings/registry.json`
  - 主キー: `ui.theme.mode`, `ui.theme.seed_color`, `ui.theme.accent_color`, `ui.motion.reduce` 筁E- チE�Eマエンジン
  - `ThemeEngine` を新設�E�設定を購読しデザイント�Eクンを生成！E  - 代表API: `colorPrimary()`, `colorBackground()`, `colorSurface()`, `colorOnSurface()`, `radiusSm()` など
  - 付随ユーチE��リチE��: `ui/effects/Motion`�E�Eeduce Motion対応�E継続時閁Eイージング�E�E `ui/effects/Elevation`�E�簡易影�E�E- パフォーマンスセーバ�E: `ui.performance.low_power`�E�低電劁E低負荷モード）設定を追加�E�EI/設定�Eみ�E�。挙動�E今後実裁E��E- Chromiumレンダリングパフォーマンス最適化！E025-12-23�E�E  - 問顁E Windows環墁E��FPSが不安定！E4 MacBookでは安定）�E CPU-GPU間転送オーバ�Eヘッドが原因
  - **ChromiumRenderHandler最適匁E*:
    - チE��チE��ログ削除�E�毎フレームのピクセルダンプを完�E削除�E�E    - IntBuffer + ビット�Eスク演算によるBGRA→ARGB高速変換�E�Eピクセルずつ→バルク処琁E��E    - GPU直接チE��スチャアチE�EロードAPI追加�E�EuploadToGPUTexture()`: GL_BGRA形式で直接OpenGLにアチE�Eロード！E  - **StandaloneWrapper最適匁E*:
    - ESCキー処琁E�Eリフレクションをsetup()でキャチE��ュ化（毎フレームのリフレクション削除�E�E  - **StandaloneChromiumProvider最適匁E*:
    - Windows固有GPU設定追加�E�E--enable-zero-copy`, `--enable-native-gpu-memory-buffers`, `--disable-software-rasterizer`�E�E- 絵斁E��サポ�Eト！E025-12-23�E�E  - Noto Emoji�E�モノクロ版）フォントを追加し、Unicode絵斁E���E斁E��化けを解涁E  - `EmojiUtil`で絵斁E��判定、`TextRenderer`でフォントフォールバック実裁E  - Label、TextField、Button等�EUIコンポ�Eネントで絵斁E��描画に対忁E  - Fast Path最適匁E 絵斁E��なしテキスト�E従来どおり高速描画
- コントロールセンターカード外部開放�E�E025-12-23�E�E  - 外部アプリがコントロールセンターにカードを登録可能
  - `ControlCenterCardRegistry` - カード登録・配置管琁E��ジストリ
  - `IAppControlCenterItem` - 外部アプリ用カードインターフェース
  - `ControlCenterSection` - セクション列挙型！EUICK_SETTINGS, MEDIA, DISPLAY, APP_WIDGETS�E�E  - `CardPlacement` - カード�E置惁E��モチE���E�ESONシリアライズ対応！E  - 設定画面にグリチE��プレビュー形式�Eカード管琁EI追加�E�表示/非表示、E��E��変更�E�E  - カード�E置設定�E永続化�E�Econtrol_center.card_placements`�E�E  - 2種類�Eカードサイズをサポ�Eト！EÁEスクエア、EÁEラージ�E�E  - 開発ガイチE `EXTERNAL_APP_DEVELOPMENT_GUIDE.md` セクション7
- ダチE��ュボ�EドウィジェチE��外部開放�E�E025-12-23�E�E  - ホ�Eム画面1ペ�Eジ目のダチE��ュボ�EドにウィジェチE��を�E置可能
  - iPhoneのウィジェチE��に類似した機�E
  - 新規パチE��ージ: `jp.moyashi.phoneos.core.dashboard`
  - `IDashboardWidget` - ウィジェチE��インターフェース�E�描画、タチE��処琁E��ライフサイクル�E�E  - `DashboardWidgetRegistry` - ウィジェチE��登録・配置管琁E��ジストリ
  - `DashboardSlot` - 5つの固定スロチE���E�ELOCK, SEARCH, LEFT, RIGHT, BOTTOM�E�E  - `DashboardWidgetSize` - 2種類�Eサイズ�E�EULL_WIDTH: 360px, HALF_WIDTH: 175px�E�E  - `DashboardWidgetType` - 2種類�Eタイプ！EISPLAY: タチE�Eでアプリ起勁E INTERACTIVE: ウィジェチE��冁E��ンタラクション�E�E  - `DashboardSlotAssignment` - スロチE��割り当て惁E���E�ESONシリアライズ対応！E  - シスチE��ウィジェチE��5種: ClockWidget, SearchWidget, MessagesWidget, EMoneyWidget, AIAssistantWidget
  - 設定画面「Dashboard」パネルでスロチE��毎�EウィジェチE��選択UI
  - ウィジェチE��配置設定�E永続化�E�Edashboard.slot_assignments`�E�E  - 開発ガイチE `EXTERNAL_APP_DEVELOPMENT_GUIDE.md` セクション8
- **Choreographer シスチE���E�E026-01-06�E�E*
  - Android Choreographerスタイルの仮想V-Sync (60Hz) ベ�Eスフレーム制御を実裁E  - 新規パチE��ージ: `jp.moyashi.phoneos.core.time`
  - `Time` - ナノ秒精度の時間管琁E��timeScale対応（スローモーション等！E  - `Choreographer` - 4フェーズのフレームループ制御�E�Enput ↁEAnimation ↁETraversal ↁEDraw�E�E  - `ScreenTickScheduler` - Screen別の可変ティチE��レート管琁E  - Catch-up処琁E ホスチEPS�E�Einecraft 20fps等）に関係なく、E��れたフレームを追ぁE��き�E琁E  - `Screen.getTargetTPS()` - アプリが要求するTPS�E�E、E0Hz�E�を持E��可能
  - `Screen.invalidate()` - 再描画リクエスト用メソチE��
  - `Kernel.requestRender()` - Dirty Flagベ�Eスの描画最適匁E  - 後方互換性: 既存�EScreen.tick()はそ�Eまま動作（デフォルチE0Hz�E�E  - 設計ドキュメンチE `KERNEL_LOOP_ARCHITECTURE.md`
- **サーバ�Eアプリ動的ロード機�E�E�E026-01-09�E�E*
  - 外部アプリ�E�EirtualHttpServer実裁E��を専用チE��レクトリから動的にロード可能に
  - 新規クラス: `jp.moyashi.phoneos.server.ServerAppLoader`
    - スキャン対象チE��レクトリ�E�優先頁E��E
      1. `mmos_server_data/server_apps/` - サーバ�E専用アプリ
      2. `mmos_server_data/apps/` - クライアンチEサーバ�E統合アプリ�E�EApplication + VirtualHttpServer�E�E    - ServiceLoader�E�EPI�E�を使用してVirtualHttpServer実裁E��検�E
    - 検�Eしたサーバ�EをSystemServerRegistryに自動登録
  - MMOSServer拡張:
    - `setServerDataPath(Path)` - サーバ�EチE�Eタのベ�Eスパス設宁E    - `getServerDataPath()` - サーバ�EチE�Eタパス取征E    - 初期化時にServerAppLoaderを呼び出ぁE  - Forge統吁E `MochiMobileOSMod.commonSetup()`でサーバ�EチE�Eタパスを設宁E  - サーバ�Eアプリの作�E方況E
    - VirtualHttpServerインターフェースを実裁E    - `META-INF/services/jp.moyashi.phoneos.server.network.VirtualHttpServer`にクラス名を記輁E    - JARを`mmos_server_data/server_apps/`また�E`mmos_server_data/apps/`に配置
  - **統吁EAR対忁E*: 1つのJARにIApplication�E�クライアント）とVirtualHttpServer�E�サーバ�E�E��E両方を含めることが可能
- **System.out/err キャプチャ機�E�E�E026-01-19�E�E*
  - MMOS冁E���E�Ep.moyashi.phoneos パッケージ�E�から�ESystem.out/err出力をLoggerServiceにキャプチャ
  - 新規クラス: `CapturingPrintStream` - PrintStreamを拡張したキャプチャ用ストリーム
    - スタチE��トレースを検査してパッケージを判定！EMOS冁E��のみキャプチャ�E�E    - ThreadLocalフラグで無限ループを防止
    - 允E�Eコンソール出力�E維持E��常に允E�Eストリームにも�E力！E  - LoggerService拡張:
    - `enableSystemStreamCapture()` - キャプチャを有効匁E    - `disableSystemStreamCapture()` - キャプチャを無効化（�Eのストリームに復允E��E    - `isStreamCaptureEnabled()` - キャプチャ状態�E取征E  - ログレベルマッピング: System.out ↁEINFO [STDOUT]、System.err ↁEERROR [STDERR]
  - Kernel初期化時に自動有効匁E- **バックグラウンドサービスAPI拡張�E�E026-01-19、E026-01-20更新�E�E*
  - 問顁E `IApplication.getEntryScreen()` がUIスクリーンを返すが、バチE��グラウンドサービスには別のScreenインスタンスが忁E��E  - 解決: IApplicationインターフェースに2つの新規メソチE��を追加
    - `hasBackgroundService()` - バックグラウンドサービスの有無を返す�E�デフォルチE false�E�E    - `getBackgroundService(Kernel kernel)` - バックグラウンドサービス用Screenを返す�E�デフォルチE null�E�E  - **アーキチE��チャ改喁E��E026-01-20�E�E*:
    - 設計原剁E IApplicationインスタンスはシングルトン、フォアグラウンドスクリーンとバックグラウンドサービススクリーンは完�E刁E��
    - **ProcessInfo拡張**:
      - `IApplication application` フィールドを追加�E�シングルトンへの参�Eを保持�E�E      - `Screen foregroundScreen` - フォアグラウンド用スクリーン�E�EI表示用�E�E      - `Screen backgroundServiceScreen` - バックグラウンドサービス用スクリーン�E�バチE��グラウンド�E琁E���E�E      - `getApplication()`, `getForegroundScreen()`, `getBackgroundServiceScreen()` メソチE��追加
      - 後方互換性: `getScreen()`, `setScreen()` は @Deprecated として維持E    - **ServiceManager改喁E*:
      - `launchApp()`: ProcessInfoからIApplicationインスタンスを取得し、`getEntryScreen()`でフォアグラウンドスクリーンを取征E      - `initializeBackgroundService()`: IApplicationインスタンスを保持、`getBackgroundService()`でバックグラウンドサービススクリーンを取征E      - `tick()`: foregroundScreenに対してtick()を呼び出ぁE      - `tickBackground()`: backgroundServiceScreenに対してbackground()を呼び出す（フォアグラウンド状態に関係なく常に実行！E      - `shutdown()`: 両方のスクリーンをクリーンアチE�E
      - `setForeground()`: foregroundScreenに対してライフサイクルイベントを通知
  - 外部アプリの対応方況E
    - `hasBackgroundService()` をオーバ�Eライドしてtrueを返す
    - `getBackgroundService()` をオーバ�Eライドして専用のバックグラウンドServiceScreenを返す
  - 利点:
    - UIスクリーン�E�EoginScreen等）とバックグラウンドサービス�E�EEINBackgroundService等）を明確に刁E��
    - バックグラウンドサービスはフォアグラウンチEバックグラウンド状態に関係なく常に動佁E    - IApplicationインスタンスからリソース�E�Elient、cacheManager等）を共有可能

- **JCEF ライフサイクル Idempotent 化！E026-02-05�E�E*
  - 問顁E ワールド�E接続時に `CefApp.addAppHandler()` ぁE`IllegalStateException` を投げる
    - CefApp は JVM レベルシングルトンで、INITIALIZED 状態�Eまま残孁E    - 新しい Kernel ぁE`createCefApp()` を�E実行すると、state > NEW で `addAppHandler()` が失敁E  - 解決筁E CefApp 初期化を idempotent 匁E    - `ForgeChromiumProvider.createCefApp()`: CefApp の状態をチェチE��し、INITIALIZED なら既存インスタンスを�E利用
    - `ForgeChromiumProvider.shutdown()`: `CefApp.dispose()` を呼ばなぁE��Eorge では CefApp は JVM ライフタイムで存続！E    - `ChromiumAppHandler.reRegisterFactories()`: 再利用時にスキームハンドラファクトリを新 Kernel で再登録
    - `SmartphoneBackgroundService.shutdownKernel()`: `sharedKernel.shutdown()` を呼んでリソースを正しく解放
  - 修正ファイル:
    - `forge/.../ForgeChromiumProvider.java` - idempotent 初期匁E+ shutdown オーバ�EライチE    - `core/.../ChromiumAppHandler.java` - `reRegisterFactories()` static メソチE��追加
    - `forge/.../SmartphoneBackgroundService.java` - `shutdownKernel()` で `kernel.shutdown()` を呼ぶ

## 現在の仕様（抜粋！E
- Kernel 初期化頁E�� `SettingsManager(vfs)` と `ThemeEngine(settings)` を作�E
- SettingsScreen は ThemeEngine のト�Eクンを使用して配色�E�ダーク/ライト即時反映�E�E- AppLibraryScreen はチE�Eマ同期し、検索ボックス/ホバー/押下ハイライチEホイールスクロールに対忁E- AppearanceにチE�EマカラープリセチE���E�E2色のseed選択）を追加。アクセント�Eseedから自動生戁E- KernelアーキチE��チャのリファクタリング�E�E025-12-02、E025-12-04�E�E  - Service Locatorアンチパターンから依存性注入�E�EI�E�パターンへの移衁E  - ServiceContainer/CoreServiceBootstrapによるサービス管琁E�E一允E��
  - イベント駁E��アーキチE��チャの導�E�E�EventBus実裁E��E  - メモリ管琁E�E高度化！EemoryManager: GC制御、リーク検�E、統計収雁E��E  - ファイルシスチE��管琁E�E抽象化！EileSystemManager: VFS統合、ファイル監視、キャチE��ュ�E�E  - リソース管琁E�E効玁E���E�EesourceCache: LRUポリシー、ソフト/ウィーク参�E�E�E  - **既存サービスのDIコンチE��への移行開姁E*�E�E025-12-04�E�E    - 第一段階移行済み: VFS、LoggerService、SystemClock、NotificationManager、AppLoader、LayoutManager、SettingsManager、ThemeEngine、ScreenManager、PopupManager、GestureManager、InputManager、RenderPipeline
    - **第二段階移行完亁E*�E�E025-12-04�E�E      - 新規追加サービス: ClipboardManager�E�EimpleClipboardManager実裁E��、ControlCenterManager、LockManager、ServiceManager
      - SimpleClipboardManager実裁E��ラスを作�E�E�ElipboardManagerインターフェースの実裁E��E      - Kernelのフォールバック処琁E��拡張
    - **第三段階移行完亁E*�E�E025-12-04�E�E      - 追加サービス: ChromiumService�E�EefaultChromiumService、ChromiumProviderに依存）、VirtualRouter、MessageStorage、ハードウェアバイパスAPI�E�EobileDataSocket、BluetoothSocket、LocationSocket、BatteryInfo�E�E      - ハ�EドウェアAPIのDI優先�E期化: DIコンチE�� ↁEHardwareController ↁE直接初期化�E頁E��取征E      - ChromiumServiceはLazySingletonとして登録�E�EhromiumProviderが環墁E��存�Eため�E�E      - **ChromiumServiceのNullエラー修正**�E�E025-12-04�E�E        - 問顁E ChromiumProviderが利用できなぁE��墁E��ChromiumServiceファクトリがnullを返し、NullPointerExceptionが発甁E        - 解決: NoOpChromiumService実裁E��作�Eし、ChromiumProviderが存在しなぁE��合�ENoOp実裁E��返すように修正
        - NoOpChromiumServiceはすべてのメソチE��で安�Eな空の操作また�Enullを返す�E�Eull Objectパターン�E�E    - **第四段階移行完亁E*�E�E025-12-04�E�E      - 追加サービス: CameraSocket、MicrophoneSocket、SpeakerSocket、ICSocket、SIMInfo、SensorManager、BatteryMonitor
      - すべてのハ�EドウェアバイパスAPIのDI統合が完亁E      - SensorManagerはLazySingletonとして登録�E�Eernelインスタンスに依存！E      - 3段階�Eフォールバック機構を確竁E DIコンチE�� ↁEHardwareController ↁE直接初期匁E      - Kernelのハ�Eドウェアバイパス初期化�E琁E��最適化（各サービスの取得�Eをログ出力！E    - Kernelの初期化�E琁E DIコンチE��から吁E��ービスを優先的に取得、取得できなぁE��合�E従来の直接初期化（フォールバック機構！E- PGraphics統一アーキチE��チャへの移衁E  - `INotification`インターフェースに`draw(PGraphics g, ...)`メソチE��を追加�E�E025-11-07�E�E  - PApplet版�EdrawメソチE��は非推奨化！EDeprecated�E�E  - 斁E��化けコメントを修正
    - AppLibraryScreen.java: 20箁E��以上�E斁E��化けを修正�E�E025-11-07�E�E    - SettingsManager.java: 全14箁E��の斁E��化けを修正�E�E025-11-08�E�E    - ThemeEngine.java: 全16箁E��の斁E��化けを修正�E�E025-11-08�E�E- コンパイルエラー修正�E�E025-11-08�E�E    - SettingsScreen.java: 構文エラー修正�E�コメントアウトされたif/switch斁E�E復允E��E    - SettingsScreen.java: 未定義変数追加�E�EhowAppearancePanel, y2, y3�E�E    - ThemeEngine.java: 95行目の埋め込み改行文字を修正
    - SettingsManager.java: 斁E��化けコメントを全て修正�E�ETF-8エンコーチE��ングエラー解消！E    - ThemeEngine.java: 斁E��化けコメントを全て修正�E�ETF-8エンコーチE��ングエラー解消！E    - ThemeEngine.java: ダークト�Eン時�Eファミリー別プライマリ色の設定を追加
    - SettingsManager.java: ui.theme.toneとui.theme.familyの設定を追加�E�E9行目の埋め込み改行文字を修正�E�E    - ThemeEngine.java: Mode列挙型にAQUAを追加、E2行目と77行目の埋め込み改行文字を修正
    - SettingsScreen.java: 斁E��化けコメント修正�E�E9, 134, 149, 152, 186, 196, 202, 368, 436, 438行目�E�E    - ThemeEngine.java: 全コメント�E斁E��化けを修正�E�E025-11-08�E�E      - 全ての日本語コメントが斁E��化けしてぁE��問題を解涁E      - 6, 7, 16, 31, 90, 96, 104, 107, 149, 184, 194, 196, 201, 202, 206, 212, 216, 223, 231, 250, 273行目の斁E��化けを修正
    - SettingsScreen.java: 斁E��化けと構文エラーを修正�E�E025-11-08�E�E      - 36, 104, 159, 174, 177, 211, 221, 227, 402, 619, 625, 886, 956, 964, 969行目の斁E��化けを修正
      - 660行目: メソチE��宣言前�E閉じ括弧欠落を修正
      - 850-865行目: 不正な位置にあったレガシーコードを削除
- 通知センターのイベント�E琁E�E改喁E��E025-11-07�E�E  - **第1牁E*: パネル冁E�Eすべてのジェスチャーイベントを確実に消費し、下�Eレイヤーへの貫通を防止
  - **第2牁E*: 当たり判定とイベント�E琁E��E��を完�Eに再設訁E    - パネル冁E���E判定を最初に行い、�E琁E��ローを�E確匁E    - すべてのジェスチャータイプ！EAP、LONG_PRESS、DRAG_START、DRAG_MOVE、DRAG_END、SWIPE_*�E�を適刁E��処琁E    - パネル外�Eイベントも確実に消費し、E��じる動作を実裁E    - ヘッダーエリア、E��知エリア、スクロール処琁E�E優先頁E��を整琁E    - `isInBounds()`メソチE��で画面全体をカバ�E�E�背景の暗幕を含む�E�E    - `onGesture()`メソチE��で、パネル冁E��あれば常に`true`を返すように改喁E- 通知シスチE��の実裁E��E025-12-23�E�E  - **通知センターの実機�E匁E*
    - モチE��通知チE�Eタを削除し、アプリからの実通知を受信・表示
    - 通知にアプリアイコン表示対応！EINotification.getIcon()`�E�E    - 通知クリチE��時にアプリ起動可能�E�EINotification.getClickAction()`�E�E  - **通知音シスチE��**
    - `NotificationSoundService`: チE��ォルト通知音�E�リソース冁E���E�とカスタム通知音�E�EFS�E�対忁E    - VFSの`readBinaryFile()`/`writeBinaryFile()`メソチE��追加
    - 設定キー: `notification.sound_path`�E�カスタム音声ファイルパス�E�E  - **チャチE��通知�E�ハードウェアAPI�E�E*
    - `ChatSocket`インターフェース追加�E�Ehardware/`パッケージ�E�E    - `DefaultChatSocket`: スタンドアロン用�E�ログ出力�Eみ�E�E    - `ForgeChatSocket`: Forge用�E�EinecraftプレイヤーチャチE��に送信�E�E    - 設定キー: `notification.chat_enabled`�E�チャチE��通知の有効/無効�E�E  - **消音モーチE*
    - 設定キー: `audio.silent_mode`�E�消音時�E通知音・チャチE��通知をオフ！E    - コントロールセンターのトグルと設定アプリの同期を実裁E    - `ToggleItem.setOnSilent()`: コールバックなしで状態変更�E�外部同期用�E�E  - **設定アプリの通知設定パネル**
    - Settings > Apps & Notifications に通知設定画面を追加
    - サイレントモードスイチE��、チャチE��通知スイチE��
    - 通知音選択機�E�E�Esystem/sounds/`冁E�Ewav/mp3ファイルを検索・選択！E
## 初期マイルスト�Eン�E�ダチE��ュボ�Eド構�Eと主要アプリ

### ダチE��ュボ�Eド（�Eーム画面�E�カーチE(5種)
-   **メチE��ージカーチE*:
    -   機�E: 新着メチE��ージの通知を表示�E�表示専用�E�、E    -   連携: 冁E��メチE��ージングシスチE��、E-   **電子�Eネ�EカーチE*:
    -   機�E: 「MoyMoy」アプリの残高を表示。タチE�Eで本体アプリを開く、E    -   連携: MoyMoyアプリ向けAPI、E-   **クロチE���E�E��ェザーカーチE*:
    -   機�E: ゲーム冁E�E現在時刻と天候を表示�E�表示専用�E�、E    -   連携: ゲーム冁E��間�E天候API、E-   **Chromium検索カーチE*:
    -   機�E: キーワード�E力欁E���E力を受けてChromiumブラウザアプリを起動する、E    -   連携: Chromiumブラウザアプリ、E-   **AIアシスタントカーチE*:
    -   機�E: 質問�E入力欁E���E力を受けてAIアシスタントアプリを起動する、E    -   連携: AIアシスタントアプリ、E
### コア・アプリケーション (3種)
-   **Chromiumブラウザアプリ**:
    -   `java-cef` を利用した基本皁E��Webブラウザウィンドウ、E-   **AIアシスタントアプリ**:
    -   チャチE��形式�EUIと、固定�E応答を返す**モチE��AI**を実裁E��E-   **吁E��APIおよび冁E��シスチE��**:
    -   MoyMoy等�E外部アプリがカード情報を提供するため�EAPI、E    -   メチE��ージカードに惁E��を渡すため�E冁E��メチE��ージングシスチE��、E
### チE��イン刷新 (HomeScreen.java)
-   **ホログラフィチE��・レイヤーUIの適用**: スチE�Eタスバ�E、ナビゲーションエリア、アプリアイコンカード�E背景に半透�E効果を適用、E-   **チE�Eマ対応�E強匁E*: 削除ボタンのハ�Eドコードされた色をテーマ対応�Eエラーカラーに置き換え。寸法、フォントサイズ、角丸の値を定数化し、コード�E保守性とチE�Eマ統合�E準備を強化、E
### チE��スチャ配信シスチE���E�E026-02-04追加、E026-02-05修正�E�E- **目皁E*: マルチ�Eレイ環墁E��PiggleShopのアイチE��チE��スチャが表示されなぁE��題を解決
- **原因**: PiggleShopApiServerがサーバ�EスレチE��で`Minecraft.getInstance()`を呼び出してぁE���E�専用サーバ�EにはMinecraftクラスが存在しなぁE��E- **解決**: チE��スチャ取得をサーバ�E側からクライアント�Eに移動、ETTP経由�E�Ehttp://3-texture/namespace/path`�E�でIPvMSchemeHandlerFactoryからTextureSchemeHandlerに委譲し、クライアント�EのResourceManagerから直接チE��スチャPNGを返す
- **�d�l**: PiggleShop�̉��i��AutoEconomicManagementMod (AEM) ����擾�i2026-02-06 �ǋL�j
- **経緯**: 当�E `mochitexture://` カスタムスキームで実裁E��たが、CEF OSRモードでは `<img src>` からのカスタムスキームサブリソースリクエストがSchemeHandlerFactoryに到達しなぁE��とが判明。既に動作実績のあるHTTP IPvMスキームハンドラを活用する方式に変更
- **構�E**:
  - `TextureProvider` インターフェース�E�Eore�E�E チE��スチャ取得�E抽象匁E  - `TextureSchemeHandler`�E�Eore�E�E チE��スチャリソースハンドラー�E�Emochitexture://` と `http://3-texture/` の両パターン対応！E  - `IPvMSchemeHandlerFactory`�E�Eore�E�E `3-texture` ホストを検�EしてTextureSchemeHandlerに委譲
  - `ForgeTextureProvider`�E�Eorge�E�E Minecraft ResourceManagerを使用した実裁E  - `ChromiumAppHandler`: スキーム登録�E�Eis_fetch_enabled=true`�E�E- **PiggleShop側**: `PiggleShopApiServer.handleSearchHtml()` でチE��スチャURLめE`http://3-texture/namespace/path` 形式で出劁E- **利点**: SSRレスポンスサイズ削減！Ease64埋め込みが不要E��、他�EMMOSアプリからも�E利用可能

### JCEF ライフサイクル Idempotent 化！E026-02-05�E�E- **目皁E*: ワールド�E接続時にCefApp再�E期化でクラチE��ュする問題を解決
- **原因**: CefApp はJVMレベルシングルトンで、INITIALIZED状態で `addAppHandler()` を呼ぶと `IllegalStateException` が発甁E- **解決**:
  - `ForgeChromiumProvider.createCefApp()`: CefApp.getState() をチェチE��し、INITIALIZED なら既存インスタンスを�E利用
  - `ForgeChromiumProvider.shutdown()`: `CefApp.dispose()` を呼ばなぁE��EVMライフタイムで存続！E  - `ChromiumAppHandler.reRegisterFactories()`: 再接続時にスキームハンドラファクトリを新Kernelで再登録
  - `SmartphoneBackgroundService.shutdownKernel()`: `kernel.shutdown()` を呼んでリソースを正しく解放

### チャットURLインターセプト（2026-02-09）
- **目的**: Minecraftチャット内のURLクリックをMMOS内蔵Chromiumブラウザで開く
- **実装**: `MMOSChatUrlInterceptMixin` - `Screen.handleComponentClicked(Style)` をMixinでインターセプト
  - `ClickEvent.Action.OPEN_URL` を検出し、Kernelが利用可能な場合はMMOSブラウザにリダイレクト
  - Kernelが利用不可（ワールド外等）の場合は従来のシステムブラウザにフォールバック
  - エラー発生時も従来動作にフォールバック（堅牢性確保）
  - **ロック状態対応**: OSがロック中の場合、ペンディングURLを`Kernel.setPendingUrlAfterUnlock()`に設定し、スリープ中ならwake。ロック解除後に`LockScreen.unlockAndNavigateToHome()`がペンディングURLを検出してブラウザを直接起動
- **変更ファイル**: `MMOSChatUrlInterceptMixin.java`（新規）、`mmos.mixins.json`（Mixin追加）、`Kernel.java`（pendingUrl API追加）、`LockScreen.java`（unlock後のブラウザ遷移）

## 問題！Essues�E�E
- MMOSの画面を開きっぱなしにするとメモリ使用量が継続的に増加する�E�リーク疑い、E026-02-01報告！E- ~~MODアプリのインスト�Eル状況が再起勁Eワールド�E読み込みで失われる！EnstalledModAppsの永続化が未実裁E��E026-02-01報告）~~ ↁE**解決済み�E�E026-02-01�E�E*: `system/installed_mod_apps.json`への永続化機�Eを実裁E- 多数のUIコンポ�EネンチE画面が色をハードコードしており、トークン適用が未宁E- Theme�E�旧�E�クラスは暫定互換のまま。段階的に ThemeEngine へ統合予宁E- AUTOモード�E刁E��ロジチE���E�時閁E環墁E��存）�E未対応（封E��拡張�E�E- Gradleビルド時にファイルロチE��の問題が発生する場合がある�E�EntelliJ IDEA等�EIDEを閉じてから実行する忁E��がある�E�E- ~~通知センターのインタラクトが下�Eレイヤーに貫通するバグ�E�修正済み: 2025-11-07�E�~~
- ~~長押し後にドラチE��ジェスチャーが一刁E��作しなぁE��E-2の排他制御が原因、E025-11-10修正済み�E�~~
- ~~ホ�Eム画面のペ�Eジアニメーション完亁E�E琁E��呼ばれず、E回目以降�EドラチE��が無効になる~~
- **Mac環墁E��のChromium HiDPI/RetinaチE��スプレイ対応�E不�E吁E*�E�E025-11-18�E�E  - 痁E��: Chromiumブラウザでウェブ�Eージが表示されるが、アスペクト比が崩れる
    - x軸ぁE/2に圧縮され、画像が左右に2つ並んで表示されめE    - y軸も大きく圧縮される（紁E/8程度�E�E  - 原因: `ChromiumRenderHandler.java`のHiDPIダウンサンプリングコード！E06-128行目�E�に問題がある可能性
    - Mac RetinaチE��スプレイでは2倍サイズ�E�E00x952�E�でレンダリングされることを想定したコードだが、実際の動作が異なめE    - ByteBufferの読み取り位置計算また�Eサイズ判定に問題がある可能性
  - 現状: HiDPI処琁E��一時的に無効化！EisHiDPI = false;`�E�して調査中
  - 関連ファイル:
    - `core/src/main/java/jp/moyashi/phoneos/core/service/chromium/ChromiumRenderHandler.java`
    - `standalone/src/main/java/jp/moyashi/phoneos/standalone/StandaloneChromiumProvider.java`
  - 対応方釁E 実際にChromiumが送ってくるバッファサイズを確認し、E��刁E��ダウンサンプリングロジチE��を実裁E��る忁E��がある
- **JCEF CefResourceHandler レスポンスボディ消失問顁E*�E�E025-12-25刁E���E�E  - 痁E��: `VirtualNetworkResourceHandler` (http://X-serverId) で、サーバ�E側は79バイトを送信したログがあるが、クライアンチESの `fetch` ぁE"Unexpected end of JSON input" で失敗する、E  - 原因: `readResponse` メソチE��の実裁E��備。最後�EチE�Eタチャンクを書き込んだ際、`bytesRead > 0` であってめE`readPosition < length` ぁE`false` となり、`false` を返却してぁE��、E  - JCEF仕槁E `readResponse` はチE�Eタが利用可能な場合！EbytesRead > 0`�E�、忁E�� `true` を返すべきである。`false` はエラーまた�EEOF�E�EbytesRead == 0`�E�を示す、E  - 解決筁E `readResponse` でチE�Eタを書き込んだ場合�E常に `true` を返すように修正する、E- ~~**シスチE��ジェスチャー領域が画面下部のUI操作を阻害**�E�E026-01-08修正済み�E�~~
  - 痁E��: 画面下部�E�E > gestureZoneY�E�に配置されたUIボタンが反応しなぁE  - 原因: `InputManager`がシスチE��ジェスチャー領域からのタチE��を即座にブロチE��し、アプリにイベントを転送しなかっぁE  - 解決筁E 「上方向スワイプ」�EみをシスチE��ジェスチャーとして消費し、単なるタチE�Eはアプリに転送するよぁE��修正
  - 修正ファイル: `core/src/main/java/jp/moyashi/phoneos/core/input/InputManager.java`
- **AppLoader.resolveAppId の競合状慁E*�E�E026-01-18発見！E  - 痁E��: 褁E��のスレチE��が同時に `resolveAppId` を呼び出した場合、アプリIDの重褁E��録めE��突が発生する可能性がある！Eheck-then-act操作がアトミチE��でなぁE��めE��、E  - 対応方釁E メソチE��の同期化！Eynchronized�E�また�E `ConcurrentHashMap.computeIfAbsent` を使用してアトミチE��性を保証する忁E��がある、E- **AppLoader.loadApplicationFromJar のリソースリークと非効玁E��**�E�E026-01-18発見！E  - 痁E��: JARスキャン時にクラスごとに新しい `URLClassLoader` を作�Eしており、これらが閉じられてぁE��ぁE��ファイルハンドルリーク�E�EindowsでのファイルロチE��問題）とメモリ浪費を引き起こす、E  - 対応方釁E JARファイルごとに単一の `URLClassLoader` を作�Eして再利用し、E��刁E��ライフサイクル管琁E��クローズ�E�を行う忁E��がある、E- **Kernel.getPixels() のカプセル化違叁E*�E�E026-01-18発見！E  - 痁E��: 描画バッファの冁E��配�E�E�EpixelsCache`�E�を直接返してぁE��ため、呼び出し�Eが意図せずバッファを破壊したり、レンダリングスレチE��と競合するリスクがある、E  - 対応方釁E 防御皁E��ピ�E�E�EArrays.copyOf`�E�を返すか、アクセスを厳寁E��制御する忁E��がある、E- **Kernel.initializeForMinecraft の不安定なリフレクション**�E�E026-01-18発見！E  - 痁E��: `processing.awt.PGraphicsJava2D` をクラス名文字�Eでロードしており、E��AWT環墁E��難読化環墁E��クラチE��ュする�E�EuntimeException�E�可能性がある、E  - 対応方釁E 依存性注入めE��ァクトリパターンを使用して、環墁E��応じたPGraphics実裁E��安�Eに提供する設計に変更すべき、E- ~~**PhoneAppRegistryEvent による外部MODアプリ登録が動作しなぁE*�E�E026-01-21修正済み�E�~~
  - 痁E��: 外部MODぁE`@SubscribeEvent` で `PhoneAppRegistryEvent` を購読しても、イベントが届かずアプリが登録されなぁE  - 原因: `FMLJavaModLoadingContext.get().getModEventBus()` はMochiMobileOS自身のMODバスのみを返すため、他�EMODはそ�Eバスに登録できなぁE  - 解決筁E InterModComms (IMC) 方式に移行。`InterModComms.sendTo("mochimobileos", "register_app", () -> app)` で登録可能に
  - 修正ファイル: `forge/src/main/java/jp/moyashi/phoneos/forge/MochiMobileOSMod.java`
  - 関連ドキュメンチE `EXTERNAL_APP_DEVELOPMENT_GUIDE.md` セクション9も更新済み

## CodeXレビューログ

### CodeXレビュー (2026-01-17) - Iteration 1
- **レビューチE�Eル**: CodeX CLI (`codex exec --full-auto`)
- **持E��件数**: 4件対応（重要な問題に絞って対応！E- **対応�E容**:
  - `Event.java`: HashMap ↁEConcurrentHashMap�E�非同期イベント�E信時�EスレチE��セーフ性改喁E��E  - `EventBus.java`: 未使用のsyncExecutor削除�E�デチE��コード除去、リソース節紁E��E  - `ResourceManager.java`: try-with-resources使用でInputStream確実クローズ�E�リソースリーク修正�E�E  - `LoggerService.java`: ログレベル判定復活、System.out出力をWARN/ERRORのみに�E�パフォーマンス改喁E��E- **ビルド結果**: 成功
- **コミッチE*: ce6272d

### CodeXレビュー (2026-01-17) - Iteration 2
- **レビューチE�Eル**: CodeX CLI (`codex exec --full-auto`)
- **持E��件数**: 3件対応（高優先度問題に絞って対応！E- **対応�E容**:
  - `EventBus.java`: postDelayed用の共有ScheduledExecutorService導�E�E�リソースリーク修正�E�E  - `ResourceManager.java`: fontCache/imageCache ↁEConcurrentHashMap�E�スレチE��セーフ化�E�E  - `VFS.java`: resolveVFSPathにパストラバ�Eサル対策追加�E�セキュリチE��修正�E�E- **ビルド結果**: 成功
- **コミッチE*: 2e7d77d
### CodeXレビュー (2026-01-17) - Iteration 3
- **レビューチE�Eル**: 手動�E�Eteration 2で検�Eされた中優先度持E��を対応！E- **持E��件数**: 4件対忁E- **対応�E容**:
  - `DefaultChromiumService.java`: ポンプ間隁EmsↁEms�E�EPU負荷軽減）、スレチE��優先度NORM_PRIORITY�E�Etarvation防止�E�、surfaceId生�EをAtomicLong化（衝突E��止�E�E  - `VirtualRouter.java`: HashMap/ArrayList ↁEConcurrentHashMap/CopyOnWriteArrayList�E�スレチE��セーフ化�E�E  - `ResourceCache.java`: removeInvalidEntriesでaccessOrderからも削除�E�ERU頁E���EチE�E肥大化防止�E�E  - `ChromiumRenderHandler.java`: dispose()メソチE��追加でGPUチE��スチャ解放対忁E- **ビルド結果**: 成功
- **コミッチE*: 4feac4e

### CodeXレビュー (2026-01-17) - Iteration 4
- **レビューチE�Eル**: CodeX CLI (`codex exec --full-auto`)
- **持E��件数**: 6件対応（セキュリチE��・リソースリーク�E�E- **対応�E容**:
  - `HttpmSchemeHandler.java`: XSS脁E��性修正�E�EenerateErrorPageにHTMLエスケープ追加�E�E  - `VirtualNetworkResourceHandler.java`: XSS脁E��性修正�E�EenerateErrorPage/generateNoServicePageにHTMLエスケープ追加�E�E  - `AppAssetResourceHandler.java`: XSS脁E��性修正 + リソースリーク修正�E�Einally blockでInputStream確実クローズ�E�E  - `WebScreen.java`: リソースリーク修正�E�EoadHtmlFromResourceにfinally block追加�E�E  - `Kernel.java`: リソースリーク修正�E�EoadJapaneseFontにtry-finally追加でfontStream確実クローズ�E�E- **ビルド結果**: 成功
- **コミッチE*: 29416f0

### CodeXレビュー (2026-01-17) - Iteration 5
- **レビューチE�Eル**: CodeX CLI (`codex exec --full-auto`)
- **持E��件数**: 3件対応（高優先度のセキュリチE��・スレチE��セーフ�Eバグ�E�E- **対応�E容**:
  - `ChromiumBrowser.java`: Intent URLスキームをhttp/httpsに限定！Eile:/javascript:等ブロチE���E�E  - `DefaultChromiumService.java`: activeSurfaceIdをvolatile化、�EンプループでスナップショチE��取得による競合回避
  - `Kernel.java`: SensorManagerImpl直接キャストをinstanceofガード付きに変更�E�ElassCastException防止�E�E- **ビルド結果**: 成功
- **コミッチE*: f691301

### CodeXレビュー (2026-01-17) - Iteration 6
- **レビューチE�Eル**: CodeX CLI (`codex exec --full-auto`)
- **持E��件数**: 3件対応（スレチE��セーフ�EセキュリチE��・バグ�E�E- **対応�E容**:
  - `LoggerService.java`: SimpleDateFormatをDateTimeFormatterに置換（スレチE��セーフ化�E�E  - `ChromiumBrowserScreen.java`: intent:// URLのスキームホワイトリスト追加�E�Ettps/http限定）、センシチE��ブなパラメータのログマスク追加
  - `InputManager.java`: マウスホイールイベントで正しい座標を使用するよう修正�E�座標追跡フィールド追加�E�E- **ビルド結果**: 成功
- **コミッチE*: 8ab496a

### CodeXレビュー (2026-01-17) - Iteration 7
- **レビューチE�Eル**: CodeX CLI (`codex exec --full-auto`)
- **持E��件数**: 2件対応（バグ/エンコーチE��ング・安�E性�E�E- **対応�E容**:
  - `MessageStorage.java`: FileReader/FileWriterをUTF-8明示のFiles.newBufferedReader/Writerに変更�E��EラチE��フォーム依存�EエンコーチE��ング問題修正�E�E  - `Kernel.java`: System.exit(0)を削除�E�Eorge環墁E��Minecraftプロセス全体を終亁E��せるリスク回避、parentApplet.exit()に委譲�E�E- **未対応（設計変更が忁E��E��E*:
  - LoggerService: 追記モーチE非同期化�E�EFS設計変更が忁E��E��E  - FileSystemManager: 仮想パス判定�E厳寁E���E�大規模リファクタリングが忁E��E��E  - DefaultChromiumService: ポ�Eリング間隔の動的制御�E�機�E影響あり�E�E  - System.out.println統一�E�影響篁E��が庁E���E�E- **ビルド結果**: 成功
- **コミッチE*: ad2a2e0

### CodeXレビュー (2026-01-18) - Iteration 8
- **レビューチE�Eル**: CodeX CLI (`codex exec --full-auto`)
- **持E��件数**: 2件対応（スレチE��セーフ�EクロスプラチE��フォーム�E�E- **対応�E容**:
  - `ThemeContext.java`: staticフィールド`theme`にvolatile追加�E�褁E��スレチE��間での可視性を保証�E�E  - `FileSystemManager.java`: getStorageInfo()をクロスプラチE��フォーム対応！Eaths.get("/")をVFSルートとFileSystems.getDefault().getRootDirectories()に変更�E�E- **未対応（設計変更が忁E��E��E*:
  - AppStore: JAR署吁Eハッシュ検証、https強制�E�サプライチェーンセキュリチE���E�E  - AppAssetResourceHandler/AppSchemeManager: パストラバ�Eサル対策（セキュリチE��設計！E  - AppLoader: URLClassLoaderの使ぁE��ぁEclose�E�リファクタリング忁E��E��E  - FileSystemManager: LRUキャチE��ュ実裁E��新�E�Eaffeine等導�E検討！E  - printStackTraceのLogger化（影響篁E��が庁E���E�E- **ビルド結果**: 成功
- **コミッチE*: 065a40f

### CodeXレビュー (2026-01-18) - Iteration 9
- **レビューチE�Eル**: CodeX CLI (`codex exec --full-auto`)
- **持E��件数**: 4件対応（文字コード統一・NPE防止・スレチE��セーフ�EスレチE��プ�Eル最適化！E- **対応�E容**:
  - `VFS.java`: Files.readString/writeStringにUTF-8エンコーチE��ングを�E示皁E��持E��（クロスプラチE��フォーム互換性�E�E  - `FileSystemManager.java`: readFile戻り値がnullの場合�EキャチE��ュ追加をスキチE�E�E�EPE防止�E�E  - `AppStoreScreen.java`: downloadingAppIdsをCopyOnWriteArrayListに変更、Thread.sleepをScheduledExecutorServiceに置換（スレチE��セーフ化・非ブロチE��ング化！E  - `EventBus.java`: CachedThreadPoolを上限付きThreadPoolExecutorに変更�E�コア2/最大8スレチE��、キュー100、CallerRunsPolicy�E�E- **未対応（設計変更が忁E��E��E*:
  - FileSystemManager: 仮想パス判定ロジチE��刷新�E�EFSパス規紁E�E明示化！E  - InputManager.update: チE��ドコード整琁E��長押し検�E設計見直し！E  - 既存�E見送り頁E��継続！EAR署名検証、パストラバ�Eサル、LRUキャチE��ュ等！E- **ビルド結果**: 成功
- **コミッチE*: 732c816

### CodeXレビュー (2026-01-18) - Iteration 10
- **レビューチE�Eル**: CodeX CLI (`codex exec --full-auto`)
- **持E��件数**: 2件対応（スレチE��プ�Eル最適化！E- **対応�E容**:
  - `FileSystemManager.java`: newCachedThreadPoolを上限付きThreadPoolExecutorに変更�E�コア2/最大8/キュー50/CallerRunsPolicy�E�E  - `ResourceCache.java`: loaderExecutorをnewCachedThreadPoolから上限付きThreadPoolExecutorに変更�E�コア2/最大6/キュー30/CallerRunsPolicy�E�E- **未対応（設計変更が忁E��E��E*:
  - 既存�E見送り頁E��継続！EAR署名検証、パストラバ�Eサル、LRUキャチE��ュ等！E- **ビルド結果**: 成功
- **コミッチE*: c7d962a

### CodeXレビュー (2026-01-18) - Iteration 11
- **レビューチE�Eル**: CodeX CLI (`codex exec --full-auto`)
- **持E��件数**: 3件対応（スレチE��セーフ�EXSS対策！E- **対応�E容**:
  - `VirtualAdapter.java`: socketフィールドにvolatile追加�E�褁E��スレチE��間�E可視性保証�E�E  - `VirtualHttpResponse.java`: error()メソチE��にHTMLエスケープ追加�E�ESS対策！E  - `AppLoader.java`: hasScannedAppsをvolatile化、コレクションをCopyOnWriteArrayList/ConcurrentHashMapに変更�E�スレチE��セーフ化�E�E- **未対応（設計変更が忁E��E��E*:
  - URLClassLoaderリーク�E�EAR単位�Eライフサイクル管琁E��忁E��E��E  - JAR署名検証�E�セキュリチE��設計が忁E��E��E  - JSONパ�Eス刷新�E�Esonへの移行！E  - AppStoreScreenのScheduler停止�E�Ecreenライフサイクル設計が忁E��E��E  - NetworkAdapterホスト抽出厳格化！ERIパ�Eス導�E�E�E- **ビルド結果**: 成功
- **コミッチE*: 109d6ab

### CodeXレビュー (2026-01-18) - Iteration 12
- **レビューチE�Eル**: CodeX CLI (`codex exec --full-auto`)
- **持E��件数**: 1件対応！EPEクラチE��ュ防止�E�E- **対応�E容**:
  - `AppStoreScreen.java`: pkg.nameのnullチェチE��追加、フォールバック値として"Unknown"を使用�E�悪意あるリポジトリJSONによるクラチE��ュ防止�E�E- **未対応（設計変更が忁E��E��E*:
  - 危険権限�E自動許可�E�EermissionManagerImpl - ダイアログ実裁E��忁E��E��E  - AppStore JARインスト�Eルに署吁Eハッシュ検証・サイズ上限�E�セキュリチE��設計が忁E��E��E  - AppAssetResourceHandler/AppSchemeManagerのパストラバ�Eサル対策（、E.」禁止等！E  - RealAdapterバイナリ取得�E応答サイズ上限
  - 既存�E見送り頁E��継綁E- **ビルド結果**: 成功
- **コミッチE*: 98a412c

### CodeXレビュー (2026-01-18) - Iteration 13
- **レビューチE�Eル**: Gemini-CLI�E�EodeX使用制限�Eため代替�E�E- **持E��件数**: 2件対応（スレチE��セーフ�EセキュリチE���E�E- **対応�E容**:
  - `Choreographer.java`: renderDirtyフィールドにvolatile追加�E�褁E��スレチE��からrequestRender()が呼ばれた際�E可視性保証�E�E  - `RealAdapter.java`: SSRF対策追加�E��Eライベ�EチEローカルIPアドレスへのアクセスをブロチE���E�E    - ループバチE�� (127.0.0.0/8, ::1)
    - プライベ�Eトアドレス (10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16)
    - リンクローカル (169.254.0.0/16, fe80::/10)
    - マルチキャスト�Eワイルドカードアドレス
    - IPv4マップIPv6アドレス対忁E- **未対応（設計変更が忁E��E��E*:
  - 既存�E見送り頁E��継綁E- **ビルド結果**: 成功
- **コミッチE*: 8e2a86b

### CodeXレビュー (2026-01-18) - Iteration 14
- **レビューチE�Eル**: Gemini-CLI
- **持E��件数**: 4件対応（スレチE��セーフ！E- **対応�E容**:
  - `MediaSessionManager.java`: activeSessionにvolatile追加�E�スレチE��間可視性保証�E�E  - `LockManager.java`: isLockedにvolatile追加�E�スレチE��間可視性保証�E�E  - `PowerManager.java`: 褁E��フィールドにvolatile追加
    - currentState, sleepStartTime, lastActivityTime, autoSleepTimeout, autoSleepEnabled
  - `ServiceManager.java`: launchAppをcomputeIfAbsentでスレチE��セーフ化�E�Eheck-then-act競合防止�E�E- **確認済み�E�修正不要E��E*:
  - ControlCenterManager: 主要フィールドには既にvolatile付与済み
  - ScreenManager: unsetupScreens/setupCompletedScreensは既にConcurrentHashMap.newKeySet()使用
- **未対応（設計変更が忁E��E��E*:
  - PowerManager状態�E移の原子性�E�EynchronizedブロチE��、AtomicReference導�E�E�E  - ProcessInfo統計更新の原子性�E�EtomicLong導�E�E�E  - RenderPipelineキャチE��ュ設計（ピクセルキャチE��ュのスレチE��セーフ化�E�E  - MediaSessionManager.createSessionの重褁E��ェチE���E�EomputeIfAbsent化！E  - 既存�E見送り頁E��継綁E- **ビルド結果**: 成功
- **コミッチE*: e4d01d5

### CodeXレビュー (2026-01-18) - Iteration 15
- **レビューチE�Eル**: Gemini-CLI
- **持E��件数**: 5件対応（バグ1件、スレチE��セーチE件�E�E- **対応�E容**:
  - `HardwareController.java`: getBatteryLevel()のロジチE��バグ修正�E�E-100めE.0-1.0に正規化�E�E  - `Time.java`: 褁E��フィールドにvolatile追加�E�EimeScale, lastVsyncNanos, totalFrameCount, currentVsyncTargetNanos, paused�E�E  - `ControlCenterCardRegistry.java`: 読み取りメソチE��にsynchronized追加�E�EetCard, getAllCards, getCardsForSection, getAllVisibleCards, getAllPlacements, getPlacement�E�E  - `DashboardWidgetRegistry.java`: 読み取りメソチE��にsynchronized追加�E�EetWidget, getAllWidgets, getWidgetsBySize, getAvailableWidgetsForSlot, getWidgetForSlot, getAllAssignments�E�E- **未対応（設計変更が忁E��E��E*:
  - TabListScreen: Buttonインスタンス毎フレーム再生成（パフォーマンス�E�E  - HomePage: shortcutsのスレチE��セーフ化、nextPageIdのAtomicInteger匁E  - ClockWidget/SearchWidget: volatileフィールド追加
  - 既存�E見送り頁E��継綁E- **ビルド結果**: 成功
- **コミッチE*: e34d4f9

### CodeXレビュー (2026-01-18) - Iteration 16
- **レビューチE�Eル**: Gemini-CLI
- **持E��件数**: 4件対応（セキュリチE��1件、DoS1件、バグ1件、スレチE��セーチE件�E�E- **対応�E容**:
  - `LockManager.java`: checkPattern()のログからパターン惁E��を削除�E�EWE-532対策、平斁E��スワードログ出力防止�E�E  - `VirtualRouter.java`: handleAppInstallRequest()で送信允E��シスチE��アドレスの場合応答しなぁE��EWE-400対策、無限�E帰ループ防止�E�E  - `FileSystemManager.java`: キャチE��ュ削除の頁E��修正�E�削除成功後にサイズ減算！E  - `ProcessInfo.java`: priority, isForeground, isBackgroundServiceにvolatile追加
- **未対応（設計変更が忁E��E��E*:
  - 既存�E見送り頁E��継綁E- **ビルド結果**: 成功
- **コミッチE*: f903b4a

### CodeXレビュー (2026-01-18) - Iteration 17
- **レビューチE�Eル**: Gemini-CLI (Manual)
- **持E��件数**: 4件対応（スレチE��セーフ�E斁E��化け修正・ログ出力適正化！E- **対応�E容**:
  - `ThemeEngine.java`: 斁E��化けしてぁE��コメントを英語に修正、�Eての色・寸法フィールドに`volatile`を追加�E�スレチE��セーフ化�E�E  - `NotificationManager.java`: 状態管琁E��ィールド！EisVisible`等）に`volatile`を追加、`System.out`を`LoggerService`に置揁E  - `ClipboardManagerImpl.java`: `provider`フィールドに`volatile`を追加
  - `SensorManagerImpl.java`: `System.out`を`LoggerService`に置揁E- **未対応（設計変更が忁E��E��E*:
  - `SettingsManager.java`: ブロチE��ングI/O�E�EsaveSettings`�E��E非同期化�E�現状は許容篁E���E�E  - 既存�E見送り頁E��継綁E- **ビルド結果**: 成功
- **コミッチE*: 372df3f

### CodeXレビュー (2026-01-18) - Iteration 17-24�E��E動�E帰レビュー�E�E- **レビューチE�Eル**: Gemini-CLI (自勁E
- **持E��件数合訁E*: 紁E5件対忁E
#### Iteration 17
- `SimpleNotification.java`: SimpleDateFormat ↁEDateTimeFormatter�E�スレチE��セーフ化�E�E- `VirtualNetworkRegistry.java`: HashMap ↁEConcurrentHashMap�E�スレチE��セーフ化�E�E- **コミッチE*: 9b2d791

#### Iteration 18
- `ScreenManager.java`: screenStackへのアクセスをsynchronizedで保護�E�EoncurrentModificationException防止�E�E- **コミッチE*: 9fee6f3

#### Iteration 19
- `VFS.java`: worldIdにパストラバ�Eサル対策（英数字�Eハイフン・アンダースコアのみ許可�E�E- `Kernel.java`: layerStackをCopyOnWriteArrayListに変更、isShuttingDownフラグ追加
- **コミッチE*: 830ffdc

#### Iteration 20
- `VirtualRouter.java`: externalSendHandlerにvolatile追加
- **コミッチE*: f3de492

#### Iteration 21
- `FileSystemManager.java`: handleWatchEventでチE��レクトリ削除通知を追加
- `NotificationManager.java`: ドラチE��スクロールのロジチE��修正�E�累積バグ解消！E- **コミッチE*: c773da5

#### Iteration 22
- `NotificationManager.java`: draw()とgetRecentNotifications()でスナップショチE��使用�E�EndexOutOfBoundsException防止�E�、E��知表示頁E��修正
- **コミッチE*: 394daea

#### Iteration 23
- `SettingsManager.java`: リスナ�Eエラーをログ出劁E- `NotificationManager.java`: グラフィチE��クリチE�Eエラーをログ出劁E- **コミッチE*: a4072b3

#### Iteration 24
- `AppStoreScreen.java`: cleanup()メソチE��追加でScheduledExecutorServiceのリソースリーク修正
- `AppStoreScreen.java`: isValidAppId()メソチE��追加でアプリIDのパストラバ�Eサル対筁E- **コミッチE*: 301e830

- **最終結果**: 新規�E重要指摘なし（レビュー完亁E��E- **ビルド結果**: すべて成功

## TODO

### メチE��ア再生管琁E��スチE���E�完亁E2025-12-21�E�E- **MediaSession/MediaController API実裁E*
  - `core/media/` パッケージに以下�Eクラスを新規作�E:
    - `PlaybackState.java` - 再生状態�E挙型�E�ETOPPED, PLAYING, PAUSED, BUFFERING, ERROR�E�E    - `MediaMetadata.java` - メタチE�EタモチE���E�Euilder付き、title/artist/album/duration/artwork�E�E    - `MediaSessionCallback.java` - 再生コントロールコールバックインターフェース
    - `MediaSession.java` - アプリがセチE��ョンを作�E・状態更新するためのクラス
    - `MediaController.java` - 外部からセチE��ョンを操作するコントローラー
    - `AudioFocusManager.java` - オーチE��オフォーカス管琁E��排他制御�E�E    - `MediaSessionManager.java` - シスチE��全体�EセチE��ョン管琁E��ービス
  - `NowPlayingItem.java` - コントロールセンター用のメチE��ア再生ウィジェチE��
  - `CoreServiceBootstrap.java` - MediaSessionManagerをDIコンチE��に登録
  - `Kernel.java` - NowPlayingItemをコントロールセンターに追加
- **Chromiumブラウザ統合（完亁E2025-12-21�E�E*
  - `ChromiumBrowser.java` - メチE��ア検�EJavaScript注入、`[MochiOS:Media]`コンソールメチE��ージ処琁E  - `ChromiumSurface.java` - メチE��ア検�EインターフェースメソチE��追加
  - `DefaultChromiumService.java` - DefaultChromiumSurfaceでメチE��アメソチE��実裁E  - `ChromiumBrowserScreen.java` - MediaSession統合（コールバック、状態更新、スクリプト注入�E�E  - YouTubeなどのWebメチE��ア再生をNow PlayingウィジェチE��に連携
  - ChromiumBrowser.dispose()でvideo/audioを、�gーソチE��らにメチE��アを大幁Eし、褧��Eログリアウトカード（ナラのみとえ���E
- **機�E:**
  - 吁E��プリの再生状態を統合管琁E  - コントロールセンターからの再生/一時停止/スキチE�E操佁E  - オーチE��オフォーカスによる音声出力�E排他制御
  - 外部MODアプリからも利用可能なAPI
  - Webブラウザ�E�EouTube等）�EメチE��ア再生検�E・制御

### KernelアーキチE��チャ移行�E次スチE��チE- **既存サービスのDIコンチE��化（ほぼ完亁E��E*
  - ✁E完亁E��E6サービス�E�E VFS、LoggerService、SystemClock、NotificationManager、AppLoader、LayoutManager、SettingsManager、ThemeEngine、ScreenManager、PopupManager、GestureManager、InputManager、RenderPipeline、ClipboardManager、ControlCenterManager、LockManager、ServiceManager、ChromiumService、VirtualRouter、MessageStorage、MobileDataSocket、BluetoothSocket、LocationSocket、BatteryInfo、E*MediaSessionManager**
  - 残りのサービス: そ�E他�Eハ�EドウェアソケチE���E�EameraSocket、MicrophoneSocket、SpeakerSocket、ICSocket�E�、SensorManager、BatteryMonitor
- **画面クラスのDI対忁E*
  - Screenインターフェースの拡張�E�依存性を注入可能に�E�E  - 既存画面クラスのコンストラクタ変更とDI対忁E- **統合テスチE*
  - DIコンチE��経由の初期化フロー検証
  - 依存関係�E解決頁E���E最適匁E  - メモリ効玁E��パフォーマンス測宁E
### Kernel刁E��リファクタリング Phase 1-5�E�完亁E��E- **Phase 1: サービス層基盤�E�完亁E2025-12-02�E�E*
  - ServiceContainer/CoreServiceBootstrap作�E�E�依存性注入コンチE���E�E- **Phase 2: 電源�Eライフサイクル管琁E��完亁E2025-12-02�E�E*
  - PowerManager/SystemLifecycleManager作�E�E�電源管琁E��ライフサイクル�E�E- **Phase 3: 画面・リソース管琁E��完亁E2025-12-02�E�E*
  - NavigationController/LayerController作�E�E�画面遷移とレイヤー管琁E��E  - HardwareController作�E�E�ハードウェアAPI�E�E- **Phase 4: イベントバスシスチE���E�完亁E2025-12-04�E�E*
  - EventBus/Event/EventType/EventListener作�E�E�Eublish-Subscribeパターン�E�E  - SystemEvent/ApplicationEvent/ScreenEvent実裁E  - PowerManager、NavigationController、Kernelとの統吁E- **Phase 5: メモリ・リソース管琁E��完亁E2025-12-04�E�E*
  - MemoryManager作�E�E�メモリ監視、GC制御、リーク検�E�E�E  - FileSystemManager作�E�E�EFS統合、ファイル監視、キャチE��ュ�E�E  - ResourceCache作�E�E�ERUキャチE��ュ、ソフト/ウィーク参�E、メモリ圧迫対応！E  - CoreServiceBootstrapへの統合完亁E
- BaseComponent/主要コンポ�Eネント！Eutton/Label/Switch/Slider/Text系�E�をThemeEngine準拠へ頁E��移行（進行中�E�E  - (更新) HomeScreen.javaの描画要素は、テーマ対応�E色、寸法、フォントサイズ、角丸の定数化により大幁E��ThemeEngine準拠に近づぁE��、E  - (更新) LockScreen.javaをThemeEngine準拠に更新�E�E025-12-20�E�。背景、文字色、E��知カード、パターン入力UIをテーマカラー/角丸ト�Eクンに対応、E  - (更新) コントロールセンターを大幁E��化！E025-12-20�E�、E    - 4カラム・スクエアグリチE��チE��インへの刷新
    - 縦型スライダー�E�音釁E輝度�E��E実裁E��左側配置
    - メチE��アコントロール�E�Ex2パネル�E��E右上固定�E置
    - ドラチE��操作�E最適化（スライダー操作とパネルスクロールの共存！E    - 低電力モード�Eり替え�E実裁E    - 音量スライダーとシスチE��設宁E"audio.master_volume")の双方向連携実裁E    - Chromium連携強匁E 再生状慁E時間の同期ロジチE��改喁E    - ToggleItemのチE��イン刷新: スイチE��廁E��、iOS風のアイコン・背景色変化スタイルに変更
    - SliderItemのチE��イン刷新: iOS風の太ぁE��ーとアイコンによる視覚的フィードバチE���E�絵斁E��位置補正済み�E�E    - ControlCenterパネルの質感向丁E 背景色、角丸、影の調整
- 追加適用: Panel/ListView/Dialog/Dropdown をトークン化（背景/枠緁E選択色/斁E��色�E�E- 追加適用: Checkbox/RadioButton をトークン化（墁E��/選択色/斁E��色�E�E- ~~設定アプリに「外観」セクションを追加�E�モーチEアクセンチE角丸/斁E��サイズ/Reduce Motion�E�最小実裁E��完亁E��~~
- 設定アプリに「外観」セクションを追加�E�モーチEアクセンチE角丸/斁E��サイズ/Reduce Motion�E�最小実裁E��完亁E��E- エレベ�Eション/影・モーションの統一�E�Eeduce Motion対応！E  - ユーチE��リチE��実裁E��（段階適用中�E�E- AUTOモード�E判定実裁E��時間帯/ホスチESチE�Eマ�Eフックが可能なら対応！E- App Library: 検索のIME/チE��スト�E力周り�E強化、検索対象の拡張�E�説昁EID�E�E- 低電力モード�E適用実裁E��EcreenTransitionのフェード�E替、影段階�E調整、E��度最適化！E- 設定アプリの残りセクション実裁E��Eound & Vibration、Storage、Apps & Notifications�E�E
### 初期マイルスト�Eン実裁E��スク
- **ダチE��ュボ�Eドカード�E実裁E**
  - メチE��ージカード（通知連携�E�E  - 電子�Eネ�Eカード！EoyMoy連携API�E�E  - クロチE���E�E��ェザーカーチE  - Chromium検索カード（ブラウザ起動！E  - AIアシスタントカード（アプリ起動！E- **コア・アプリケーションの基礎実裁E**
  - Chromiumブラウザアプリ
  - AIアシスタントアプリ�E�EIとモチE��AIのみ�E�E  - MoyMoyアプリ向けAPI
  - 冁E��メチE��ージングシスチE��

## dliǉEXVj

- CAEgg[Nibj
  - spacing: PADDING=16, GAP=12, ITEM_HEIGHT=88iControl Center  App Library ŋʉ^pj
  - grid: Control Center  3 JAEpfBOƃMbvlz
  - App Library: LIST_START_Y=112, ITEM_PADDING=16, s=88AE[ 400-ITEM_PADDING ���ɁE
- gOiControl Center / ToggleItemj
  - wi=surface{border̃J[hAONaccent̒At@d
  - x=onSurfaceA#999999AACRaccent^C{onPrimaryeLXg
  - XCb`=ON:accent / OFF:#B0B0B0Amu=iAt@enabledɉĒj

- iXVj

- ꕔʂŕE 400x600 Õn[hR[hciIɉσTCY֓ꂪKvj
- Reduce Motion/Low Power ̋Kp͖i`掞́E//TODO ƂĖj

## TODOiXVj

- Control Center ̑SACe IControlCenterItem Ńe[}Ă邩I
- App Library  hover/pressed ̃At@le[} Reduce Motion ɘAi//TODOj
- ωʃTCYΉ̂߁A 400 ̃e���iΉj

### ǁE ���npl̎FPiCg[hj
- XNiÖj: ControlCenter=110, NotificationCenter(PApplet)=110, (PGraphics)=100
- plʂ킸ɈÁE ThemeEngine.light.colorSurface=#FAFAFAAɍ̔I[o[Ci~16?18j
- ړI: wƂ̍𖾊mAсEῂ̒ጸ

### ǁE e[}[h̊g
- V[h: orange / pink / qua ǉiLIGHTñoAgj
- ThemeEngine: Mode ɗ񋓂ǉAe[h ackground/surface/onSurface/border ₩ȐFŏ㏑
- Settings > Appearance: Theme Mode  2iڃ{^iOrange/Pink/AquajǉAui.theme.mode ɕہE

## ύX(2025-11-08)
- _[N+YellowŕFɂȁE: ThemeEngine.recomputePalette()Ń_[Ng[colorOnSurfaceɁE0xFFFFFFFF)ɌŒBt@~[ɈˑFSہB


- SettingsScreen.java: UTF-8エンコーチE��ングエラー修正�E�E025-11-08�E�E  - BOM (Byte Order Mark) を削除
  - 全ての斁E��化けした日本語コメントを修正�E�E72, 175, 178, 228, 473行目など�E�E  - 471行目: 破損した文字�EリチE��ル を修正
  - 683行目: 埋め込まれた改行文孁E を実際の改行に置揁E  - コンパイルエラー解涁E BUILD SUCCESSFUL

## 変更(2025-11-09)
- **JavaFX WebViewの完�E削除**�E�ETMLアプリケーション軽量化プロジェクチEPhase 1�E�E  - 目皁E Chromium統合により冗長となったJavaFX WebViewを削除し、JARサイズ削減！E0-80MB�E�、起動時間短縮�E�E-2秒）、メモリ使用量削減！E0-50MB�E�を実現
  - 削除されたコンポ�EネンチE
    - `core/src/main/java/jp/moyashi/phoneos/core/apps/browser/` (BrowserApp, BrowserScreen)
    - `core/src/main/java/jp/moyashi/phoneos/core/apps/htmlcalculator/` (CalculatorHTMLApp)
    - `core/src/main/java/jp/moyashi/phoneos/core/ui/HTMLScreen.java` (HTML画面の基底クラス)
    - `core/src/main/java/jp/moyashi/phoneos/core/service/webview/` (WebViewManager, WebViewWrapper)
  - Kernel.javaの更新:
    - BrowserAppのimport削除�E�E8行目�E�E    - CalculatorHTMLAppの登録コード削除�E�E076-1077行目�E�E    - BrowserAppの登録コード削除�E�E082-1083行目�E�E    - calculatorHTMLAppとbrowserAppの初期化コード削除�E�E090, 1092行目�E�E  - ビルド設定�E更新:
    - `core/build.gradle.kts`: JavaFX依存！Eavafx-base, javafx-graphics, javafx-controls, javafx-web, javafx-swing, javafx-media�E�を削除
    - `forge/build.gradle`: JavaFX依存とcopyJavaFxJars/extractJavaFxCoreタスクを削除
  - 結果: BUILD SUCCESSFUL�E�警告�Eみ、エラーなし！E  - 今後�E展開: Chromium APIを使用したHTML表示機�Eの実裁E��検訁E
- **UTF-8エンコーチE��ングエラーの全面修正**�E�E025-11-09�E�E  - 目皁E JavaFX削除後�Eコンパイルエラーを修正し、ビルドを成功させめE  - 修正されたファイル:
    - `core/src/main/java/jp/moyashi/phoneos/core/apps/launcher/ui/HomeScreen.java`
      - 20箁E��以上�E斁E��化け！EチE`, `琁E`, `宁E`, `E`等）を修正
      - 改行が失われてぁE��箁E��を修正�E�E239, 1645行目等！E      - 斁E���EリチE��ルの斁E��化けを修正�E�E276, 2280, 2918, 2926行目�E�E      - 未定義メソチE��`syncLivePageDragFromGesture()`をコメントアウト！E51行目�E�E    - `core/src/main/java/jp/moyashi/phoneos/core/input/GestureManager.java`
      - javadocコメント�E斁E��化けを修正�E�E19-120, 170, 174, 178-179, 231, 234行目�E�E      - 改行が失われてぁE��箁E��を修正�E�E16, 170, 178, 231, 233行目�E�E      - クラス閉じ括弧の位置エラーを修正�E�E28ↁE35行目に移動！E  - 結果: **全モジュール�E�Eore, standalone, forge�E��Eコンパイルが�E劁E* - BUILD SUCCESSFUL

- **ホ�Eムスクリーンのフォントサイズ調整**�E�E025-11-09�E�E  - 目皁E ホ�Eムスクリーンの斁E��が大きすぎる問題を修正
  - 修正冁E���E�EomeScreen.java�E�E
    - アプリアイコン冁E�Eイニシャル表示: 20 ↁE12 (1569行目)
    - アプリ名ラベル: 11 ↁE8 (1406行目、E498行目)
  - 結果: BUILD SUCCESSFUL

- **ジェスチャー機�Eの大幁E��改喁E*�E�E025-11-09/10�E�E  - 目皁E GestureManagerの問顁E1件を体系皁E��修正し、堁E��性とパフォーマンスを向丁E  - Phase 1�E�Eigh優先度�E�E
    - H-1: 長押し検�Eをupdate()メソチE��に一本化！EandleMouseReleased()からの重褁E��除�E�E    - H-2: ドラチE��終亁E��のスワイプ検�Eを削除�E�ドラチE��とスワイプ�E競合を防止�E�E    - H-3: isInBounds()実裁E�E調査完亁E��各マネージャーの実裁E�E適刁E��判断、変更なし！E  - Phase 2�E�Eedium優先度�E�E
    - M-1: ドラチE��座標�E常時更新を確認（既に実裁E��み�E�E    - M-2: 長押し後�EドラチE��排他制御を追加�E�意図しなぁE��動を防止�E�E    - M-3: リスナ�E登録の一允E��琁E�Eリスク高�EためスキチE�E
    - M-4: スワイプ方向判定に角度判定を追加�E�±30度の篁E��冁E��水平/垂直を判定、対角線�E検�EしなぁE��E    - M-5: 例外発生時も次のリスナ�Eへ継続する仕絁E��を実裁E��エラー回復性向上！E  - Phase 3�E�Eow優先度�E�E
    - L-1: パフォーマンスログの閾値めE0ms/20msから20ms/50msに引き上げ�E�E/O負荷軽減！E    - L-2: スワイプ角度判定�E定数化！EWIPE_HORIZONTAL_ANGLE_MAX=30.0, SWIPE_VERTICAL_ANGLE_MIN=60.0�E�E    - L-3: チE��チE��ログの統一確認！Eystem.out.printlnは存在せず、�Eてlogger経由�E�E    - L-4: 封E��皁E��ジェスチャー拡張は今回対応せぁE    - L-5: コード重褁E�E削除�E�距離計算を共通メソチE��calculateDistance()に統一、debugVerbose()メソチE��を削除�E�E  - 結果: **全モジュールコンパイル成功** - BUILD SUCCESSFUL
  - 修正ファイル: `core/src/main/java/jp/moyashi/phoneos/core/input/GestureManager.java`
  - **バグ修正**�E�E025-11-10�E�E
    - 問顁E: ドラチE��が一度しか動作しなぁEↁEM-2の不要なコーチE`longPressDetected = false;`)を削除して修正
    - 問顁E: 長押しが動作しなぁEↁEupdate()が呼ばれてぁE��ぁE��め、handleMouseDragged()冁E��も長押しをチェチE��するように補宁E    - 問顁E: ドラチE��のログは出るが実際に反映されなぁE���Eージスワイプ、AppLibraryスクロール�E��E 長押し検�EロジチE��が距離チェチE��より先に実行されてぁE��ため、E00ms以上かけてドラチE��すると長押しとして誤検�Eされ、ドラチE��処琁E��ブロチE��されてぁE��。距離計算を長押しチェチE��の前に移動し、`dragDistance < DRAG_THRESHOLD`条件を追加して修正�E�EestureManager.java 149-165行目�E�E    - H-1の補宁E Kernel.javaでgestureManager.update()が呼ばれてぁE��ぁE��墁E��対忁E  - **追加修正**�E�E025-11-10�E�E    - 長押し後�EドラチE��排他制御を撤回し、DRAG_THRESHOLDを趁E��た移動では常にドラチE��を開始するよぁE��更�E�EestureManager.java 173行目付近！E    - 長押し�EドラチE��でアイコン移動やペ�Eジスワイプが再�E機�Eする�E��Eーム画面/AppLibraryで確認済み�E�E  - **ホ�Eムスクリーン ペ�Eジアニメーション修正**�E�E025-11-10�E�E    - `updatePageAnimation()` でアニメーション完亁E��に `completePageTransition()` が呼ばれておらず、`isAnimating` が解除されなぁE��題を修正
    - これによりホ�Eム画面のペ�EジスワイチEドラチE��が�E回�Eみ有効になる不�E合が解消！EomeScreen.java 1039行目�E�E  - **ドラチE��処琁E�Eパフォーマンス改喁E*�E�E025-11-10�E�E    - GestureManagerのドラチE��イベント発火間隔めEms ↁE8msへ変更し、不要な高頻度チE��スパッチによるUIスレチE��飽和を緩咁E    - DRAG_MOVEイベント�EチE��チE��ログ出力を抑制し、ミニ�Eム移動量�E�Epx�E�また�E経過時間8msのどちらかでイベントをまとめるフレーム同期征E��に変更
    - ControlCenterManager/LockScreenの高頻度`System.out.println`をデバッグフラグ経由にし、標準実行時のI/O負荷を削渁E  - **グローバルスワイプ検�Eの復活**�E�E025-11-10�E�E    - `checkForSwipe()` を�E度呼び出し、エチE��スワイプで通知センター/コントロールセンター/ロチE��画面が展開できるように修正
    - 長押し中はスワイプを発火させず、ドラチE��完亁E��に距離・速度条件を満たした場合�EみSWIPEイベントを生�E

## 変更(2025-11-28)
- **Mac版とWindows版�Eコード�Eースマ�Eジ完亁E*
  - 背景: Mac環墁E��`git push --force`が実行され、Windows環墁E��Git履歴が完�Eに刁E��E    - ChromiumBrowserScreen.java: Mac版では完�Eにリライト！EhromiumSurface/TextField/Buttonベ�Eスの新アーキチE��チャ、E57行）、Windows版では従来の手動レンダリング�E�E075行！E    - SettingsScreen.java: Windows版ではPhase 1-3実裁E��亁E��Eセクション、E573行）、Mac版�E旧バ�Eジョン�E�E75行！E  - 対応手頁E
    1. `.gitignore`を更新し、未追跡のランタイムファイル/開発ファイル�E�Eidea/*, standalone/jcef-bundle/*, forge/run/*, *.log, *.bat等）を除夁E    2. `git pull origin master --allow-unrelated-histories`で強制マ�Eジ実衁E    3. 20ファイル以上�Eマ�Eジ競合をユーザーがIDE上で手動解決
    4. HomeScreen.javaのimport斁E��足�E�EextField, Sensor, SensorEvent, SensorEventListener�E�をユーザーが手動修正
  - 結果: **core/forgeモジュールのビルドに成功** - BUILD SUCCESSFUL
    - Mac版�EChromiumブラウザUI改喁E��新アーキチE��チャ�E�を取り込み
    - Windows版�ESettings Phase 1-3実裁E��Eセクション完�E実裁E��を保持
    - 両環墁E�E変更を統合した完�Eなコード�Eースを確竁E  - 今後�E課顁E
    - Mac環墁E��のHiDPI/RetinaチE��スプレイ対応�E検証
    - 統合されたChromiumBrowserScreenの動作テスチE
- **ホ�Eムスクリーンのペ�Eジ構造修正**
  - 問顁E マ�Eジの過程で1ペ�Eジ目�E�ダチE��ュボ�Eド）にアプリショートカチE��が�E置されてぁE��
  - 修正冁E���E�EomeScreen.java `createDefaultLayout()`メソチE���E�E
    - 1ペ�Eジ目: ダチE��ュボ�Eド専用ペ�Eジ�E�アプリショートカチE��は配置しなぁE��E    - 2ペ�Eジ目以陁E アプリグリチE��ペ�Eジ�E�従来通りアプリショートカチE��を�E置�E�E    - 最終�Eージ: AppLibraryペ�Eジ�E��Eアプリリスト表示�E�E  - 結果: 正しいペ�Eジ構造を復允E��、ダチE��ュボ�EドとアプリグリチE��が�E離されぁE
- **Chromiumブラウザのマウスイベント�E琁E��正**
  - 問顁E クロミウムブラウザでスクロール、クリチE��、ドラチE��が正しくChromiumに転送されてぁE��かっぁE  - 修正冁E���E�EhromiumBrowserScreen.java�E�E
    1. `mousePressed`メソチE���E�E36行目�E�E `sendMouseDragged`を誤って呼んでぁE�� ↁE`sendMousePressed`に修正
    2. `mouseDragged`メソチE��を新規追加�E�E82-294行目�E�E ドラチE��イベントをChromiumに転送E    3. `mouseMoved`メソチE���E�E67-271行目�E�E UIコンポ�Eネント�E琁E��、コンチE��チE��リア冁E��あればChromiumに`sendMouseMoved`を転送E    4. `mouseWheel`メソチE���E�E77-279行目�E�E コンチE��チE��リア冁E��どぁE��をチェチE��してから転送E    5. `mouseReleased`メソチE���E�E51-254行目�E�E コンチE��チE��リア冁E��どぁE��をチェチE��してから転送E    6. `isInContentArea`ヘルパ�EメソチE��を追加�E�E27-329行目�E�E コンチE��チE��リア�E�E0, 60, width-10, height-60�E��E当たり判宁E  - **追加修正1**: ボタン番号とスクロール量を修正
    - ボタン番号: ChromiumProviderはProcessing規紁E��E=左, 2=中, 3=右�E�でボタン番号を受け取めE      - `mousePressed`, `mouseReleased`, `mouseDragged`でボタン番号めE�E�左ボタン�E�に修正�E�Eutton 0 ↁEbutton 1�E�E    - スクロール釁E ChromiumProviderでdelta値を`(int)`にキャストするため、小さな値ぁEになる問題を修正
      - `mouseWheel`でdelta値めE0倍に増幁E��Edelta * 10`�E�してから転送E��当�E3倍�E10倍に調整�E�E  - **追加修正2**: スクロールイベント�E重褁E��修正�E�EtandaloneWrapper.java�E�E    - 問顁E マウスホイールイベントが3箁E��で登録されており、E回�Eスクロールで褁E��回イベントが発火してぁE��
      - NEWTウィンドウのmouseWheelMovedリスナ�E�E�E8-105行目�E�E      - AWTのMouseWheelListener�E�E75-186行目�E�E      - Processing標準�EmouseWheel()メソチE���E�E53-361行目�E�E    - 修正: NEWT/AWTのリスナ�Eをコメントアウトし、Processing標準�EmouseWheel()のみを使用
    - 結果: スクロールイベント�E重褁E��解消され、正しいスクロール動作を実現
  - **最終結果**: Chromiumブラウザ冁E��のクリチE��、ドラチE��、スクロール、�Eウス移動が正常に動作するよぁE��なっぁE
- **Android Intent URL対忁E*
  - 問顁E `intent://`スキームのURLにアクセスすると「ERR_UNKNOWN_URL_SCHEME」エラーが発甁E  - Intent URLとは: Androidアプリを起動するため�EURL形式で、E��常のブラウザでは処琁E��きなぁE    - 形弁E `intent://HOST/PATH#Intent;scheme=SCHEME;S.browser_fallback_url=FALLBACK_URL;end;`
  - 修正冁E��:
    - **ChromiumBrowserScreen.java**:
      - `convertIntentUrl()`メソチE��を新規追加�E�E47-395行目�E�E        - Intent URLを検�Eし、`S.browser_fallback_url`パラメータからフォールバックURLを抽出
        - フォールバックURLが存在しなぁE��合�E、`scheme`パラメータとホスチEパスから通常のURLを構篁E        - URLチE��ード�E琁E��含む
      - `keyPressed()`メソチE��を修正�E�E03-306行目�E�E アドレスバ�EからのURL入力時にIntent URLを変換してから読み込む
    - **ChromiumBrowser.java�E�追加修正�E�E*:
      - `CefRequestHandlerAdapter`を追加�E�E07-125行目�E�E ペ�Eジ冁E�EIntent URLリンククリチE��時にもインターセプト
        - `onBeforeBrowse()`メソチE��でナビゲーション前にURLをチェチE��
        - Intent URLを検�Eしたら、変換後�EURLで`browser.loadURL()`を呼び出ぁE        - 允E�Eリクエストをキャンセル�E�Ereturn true`�E�E      - `convertIntentUrl()`メソチE��を追加�E�E024-1071行目�E�E ChromiumBrowserScreen.javaと同じロジチE��
      - import斁E��`CefRequestHandlerAdapter`と`CefRequest`を追加�E�E2-14行目�E�E  - 結果: アドレスバ�E入力とペ�Eジ冁E��ンククリチE��の両方でIntent URLが�E動変換され、エラーなく�Eージが表示されめE
- **ChromiumブラウザのショートカチE��キー対忁E*�E�E025-11-28�E�E  - 問顁E Chromiumブラウザ冁E��Ctrl+C、Ctrl+V、Alt+F4、Cmd+C�E�Eac�E�等�EショートカチE��キーが動作しなぁE  - 原因: 修飾キー�E�Elt/Meta�E��E状態がChromiumに渡されてぁE��かっぁE    - Kernel.javaにはShift/Ctrlのみ実裁E��れており、Alt/Metaキーの追跡がなかっぁE    - ChromiumSurface/ChromiumProviderのインターフェースに修飾キーパラメータがなかっぁE    - StandaloneWrapper.javaでAlt/Metaキーが特殊キーとして検�EされてぁE��かっぁE  - 修正冁E��:
    - **Kernel.java**:
      - `altPressed`/`metaPressed`フィールドを追加�E�E97-201行目�E�E      - `isAltPressed()`/`isMetaPressed()`メソチE��を追加�E�E443-1459行目�E�E      - `keyPressed()`でAlt�E�EeyCode==18�E�とMeta�E�EeyCode==91/157�E�を検�E�E�E68-689行目�E�E      - `keyReleased()`でAlt/Metaキーのリリースを検�E�E�E75-790行目�E�E    - **ChromiumSurface.java�E�インターフェース変更�E�E*:
      - `sendKeyPressed()`/`sendKeyReleased()`にshiftPressed/ctrlPressed/altPressed/metaPressedパラメータを追加�E�E21行目、E33行目�E�E    - **ChromiumProvider.java�E�インターフェース変更�E�E*:
      - `sendKeyPressed()`/`sendKeyReleased()`にshiftPressed/ctrlPressed/altPressed/metaPressedパラメータを追加�E�E91行目、E04行目�E�E    - **ChromiumBrowser.java**:
      - `sendKeyPressed()`/`sendKeyReleased()`のシグネチャを更新�E�E25行目、E39行目�E�E      - `flushInputEvents()`でKernelから全修飾キー�E�Ehift/Ctrl/Alt/Meta�E�を取得してProviderに渡す！E93-997行目、E000-1004行目�E�E    - **DefaultChromiumService.java**:
      - `sendKeyPressed()`/`sendKeyReleased()`のシグネチャを更新�E�E95行目、E00行目�E�E    - **StandaloneChromiumProvider.java**:
      - `sendKeyPressed()`/`sendKeyReleased()`のシグネチャを更新�E�E69行目、E33行目�E�E      - Alt/MetaのmodifierをAWTイベントに追加�E�EALT_DOWN_MASK`、`META_DOWN_MASK`�E�！E85-490行目、E49-554行目�E�E    - **ForgeChromiumProvider.java**:
      - `sendKeyPressed()`/`sendKeyReleased()`のシグネチャを更新�E�E36行目、E67行目�E�E      - Alt/MetaのmodifierをMCEFイベントに追加�E�Eodifiers |= 4/8�E�！E49-254行目、E80-285行目�E�E    - **StandaloneWrapper.java**:
      - `keyPressed()`でAlt�E�E8�E�とMeta�E�E1/157�E�を特殊キーとして検�E�E�E85-386行目�E�E      - Ctrl/Alt/MetaのぁE��れかが押されてぁE��場合も通常斁E��キーをKernelに転送E��E89-392行目�E�E    - **ChromiumBrowserScreen.java**:
      - `keyPressed()`/`keyReleased()`でKernelから全修飾キーを取得してsendKeyPressed/sendKeyReleasedに渡す！E15-320行目、E27-331行目�E�E  - 結果: **Ctrl+C/V/X/A、Alt+F4、Cmd+C�E�Eac�E�等�EショートカチE��キーがChromiumブラウザ冁E��正常に動作するよぁE��なっぁE*
  - **追加修正1**�E�制御斁E���E問題�E改訂版�E�E
    - 問顁E Ctrl+Aのみ動作し、Ctrl+C/V/X等�EショートカチE��が動作しなぁE    - 原因: Ctrl押下時にkeyCharが制御斁E��！Etrl+C=0x03、Ctrl+V=0x16等）になっており、Chromium側で正しく解釈されなかっぁE    - 修正冁E���E�EtandaloneChromiumProvider.java�E�E
      - **sendKeyPressed()でCtrl/Alt/Meta押下時に制御斁E��が来た場合、keyCharをCHAR_UNDEFINEDに変換**�E�E01-509行目�E�E        - ChromiumはmodifiersとkeyCodeの絁E��合わせでショートカチE��を判定するため、keyCharは不要E        - すべての制御斁E��！Ex00-0x1F�E�をCHAR_UNDEFINED�E�ExFFFF�E�に変換
      - sendKeyReleased()でも同様�E処琁E��追加�E�E78-582行目�E�E      - **チE��チE��ログを追加**して、E��信されるイベント情報を確認できるようにした�E�E95-512行目�E�E    - チE��ト手頁E
      1. `./gradlew standalone:build`でビルチE      2. standaloneアプリを起動してChromiumブラウザでCtrl+Cを押ぁE      3. コンソールに以下�Eようなログが�E力される�E�E         ```
         [StandaloneChromiumProvider] sendKeyPressed: keyCode=67, keyChar=3 ('?'), shift=false, ctrl=true, alt=false, meta=false
         [StandaloneChromiumProvider] Adjusted control char to UNDEFINED: keyCode=67, original keyChar=3
         [StandaloneChromiumProvider] Sending KEY_PRESSED: awtKeyCode=67, adjustedKeyChar=65535, modifiers=128
         ```
    - 期征E��れる結果: **Ctrl+C/V/X等�EショートカチE��キーが正常に動作すめE*
  - **追加修正2**�E�フォーカス管琁E��スチE��への統合）！E025-11-28�E�E
    - 問顁E アドレスバ�EをクリチE��した後、Ctrl+C等�EショートカチE��キーがアドレスバ�Eに送られてしまぁE��Chromiumで動作しなぁE    - 根本原因: ChromiumBrowserScreenが独自の`isEditingUrl`フラグでフォーカス管琁E��行っており、既存�EOSフォーカス管琁E��スチE��と統合されてぁE��かっぁE    - 修正冁E���E�EhromiumBrowserScreen.java�E�E
      - `isEditingUrl`フラグを完�Eに削除�E�E7行目�E�E      - `hasFocusedComponent()`メソチE��を実裁E��E35-437行目�E�：`addressBar.isFocused()`を返す
      - `setModifierKeys()`メソチE��を実裁E��E47-454行目�E�：封E��皁E��拡張のための準備
      - `keyPressed()`メソチE��を簡略化！E02-337行目�E�：`addressBar.isFocused()`で判定し、フォーカスされてぁE��場合�Eアドレスバ�Eに、されてぁE��ぁE��合�EChromiumに送信
      - `keyReleased()`メソチE��も同様に修正�E�E39-352行目�E�E      - `mousePressed()`メソチE��の`isEditingUrl`参�Eを`addressBar.isFocused()`に置換！E19-234行目�E�E      - そ�E他�Eての`isEditingUrl`参�Eを`addressBar.isFocused()`に置換！E13, 139, 252, 269, 290行目�E�E    - 結果: **OSの既存フォーカス管琁E��スチE��に統合され、E��常のチE��スト�E力と同じ動作になっぁE*
      - アドレスバ�EをクリチE��した後でも、ChromiumコンチE��チE��クリチE��するとフォーカスが�E動的に移勁E      - ショートカチE��キーは常にフォーカスされてぁE��コンポ�Eネント（アドレスバ�Eまた�EChromium�E�に正しく送信されめE  - **追加修正3**�E�ECEFブラウザへのフォーカス設定�E問題を修正�E�！E025-11-28�E�E
    - 問顁E `browser.setFocus(true)` の呼び出しでAWTイベントキューで例外が発甁E      - "Exception in thread "AWT-EventQueue-0"" が褁E��回�E力される
      - ChromiumBrowser.javaの初期化時に `uiComponent.setFocusable(false)` を設定してぁE���E�E28行目�E�ため、setFocus()が失敁E    - 修正冁E���E�EtandaloneChromiumProvider.java�E�E
      - `browser.setFocus(true)` の呼び出しを完�Eに削除�E�E33-536行目、E89行目�E�E      - JCEFはフォーカスなしでもキーイベントを受け取ることができる
      - UIコンポ�Eネントが `setFocusable(false)` に設定されてぁE��琁E��: ProcessingウィンドウでIMEを使用するため
    - エラーハンドリングの改喁E
      - InvocationTargetExceptionの詳細を表示するように改喁E��E65-571行目、E24-630行目�E�E    - 結果: **AWTスレチE��での例外が解消され、キーイベントが正しく送信されるよぁE��なっぁE*
  - **追加修正4**�E�EeyEvent/MouseEventのsourceComponentを実際のUIコンポ�Eネントに変更�E�！E025-11-28�E�E
    - 問顁E キーイベント�E送信されてぁE��が、ショートカチE��キーが動作しなぁE    - 根本原因: KeyEvent/MouseEventのsourceとして`new Canvas()`とぁE��空のコンポ�Eネントを使用してぁE��
      - こ�Eコンポ�Eネント�E実際のブラウザのUIコンポ�Eネントとは無関係で、フォーカスも持ってぁE��ぁE    - 修正冁E���E�EtandaloneChromiumProvider.java�E�E
      - `eventComponent`を`fallbackComponent`に改名！E5行目�E�E      - すべてのイベント送信メソチE��で`browser.getUIComponent()`を呼び出して実際のUIコンポ�Eネントを取征E        - sendKeyPressed()�E�E18-522行目�E�E        - sendKeyReleased()�E�E07-611行目�E�E        - sendMousePressed()�E�E77-280行目�E�E        - sendMouseReleased()�E�E29-332行目�E�E        - sendMouseMoved()�E�E62-365行目�E�E        - sendMouseDragged()�E�E06-409行目�E�E        - sendMouseWheel()�E�E37-440行目�E�E      - getUIComponent()がnullの場合�EfallbackComponentを使用
    - 結果: **KeyEvent/MouseEventが実際のブラウザUIコンポ�Eネントから発火されるよぁE��なり、ChromiumがショートカチE��キーを正しく認識できるようになっぁE*

## 変更(2025-12-02)
- **Kernel刁E��リファクタリング Phase 1 完亁E*
  - 目皁E Kernelクラス�E�E456行）�E責任刁E��と単一責任原則への準拠
  - 作�Eされた新規クラス:
    - `InputManager`: 入力イベント�E琁E���Eウス/キーボ�Eド）を一允E��琁E    - `ShortcutKeyProcessor`: Ctrl+C/V/X/A等�EショートカチE��処琁E��専門匁E    - `RenderPipeline`: 描画パイプライン管琁E��背景、スクリーン、スリープ�E琁E��E  - Kernelの変更:
    - 入力�E琁E��ソチE���E�EousePressed/Released/Dragged/Moved、keyPressed/Released�E�をInputManagerへ委譲
    - render()メソチE��をRenderPipelineへ委譲
    - 修飾キー状態管琁E��InputManagerへ移管
    - handleHomeButton()をpublicに変更�E�封E��皁E��LayerControllerへ移行予定！E  - 互換性維持E
    - 既存APIは維持し、�E部実裁E�Eみ委譲に変更
    - InputManager/RenderPipelineが�E期化されてぁE��ぁE��合�E従来処琁E��実衁E  - 技術的修正:
    - PopupManagerのインポ�Eトパスを修正�E�Eervice→ui.popup�E�E    - ClipboardManagerのメソチE��名を修正�E�EetText/getText→copyText/pasteText�E�E    - ScreenManager/GestureManagerのメソチE��シグネチャに合わせて修正
  - 結果: **BUILD SUCCESSFUL** - コンパイルエラー解涁E
- **Kernel刁E��リファクタリング Phase 2 完亁E*
  - 目皁E Service Locatorアンチパターンの解消と依存性注入パターンへの移衁E  - 作�Eされた新規クラス:
    - `ServiceContainer`: 依存性注入コンチE���E�シングルトン/トランジエンチE遁E��初期化をサポ�Eト！E    - `CoreServiceBootstrap`: サービスの初期化頁E��と依存関係を管琁E    - `PowerManager`: スリーチEウェイク、省電力モード、�E動スリープ機�Eを管琁E    - `SystemLifecycleManager`: シスチE��の起勁E停止/一時停止/再開ライフサイクルを管琁E    - `FileSystemManager`、`MemoryManager`、`ResourceBundle`: プレースホルダー実裁E��Ehase 3で完�E実裁E��定！E  - Kernelの変更:
    - CoreServiceBootstrapをsetup()メソチE��の最初に初期匁E    - PowerManager経由でsleep()/wake()を�E琁E��従来処琁E��維持E��E    - frameRate()メソチE��を追加�E�EowerManagerのFPS制御用�E�E    - getService()メソチE��を追加�E�EerviceContainerからの任意サービス取得！E  - 技術的解決:
    - Service Locatorパターンの問題「カーネルパニチE��の危険性」を解涁E    - 依存関係�E明示化とチE��ト可能性の向丁E    - サービス初期化頁E���E自動管琁E  - 結果: **BUILD SUCCESSFUL** - 全コンパイルエラー解消、サービスコンチE��統合完亁E
- **Kernel刁E��リファクタリング Phase 3 完亁E*
  - 目皁E 上位層の責任刁E��とハ�Eドウェア/リソース管琁E�E独竁E  - 作�Eされた新規クラス:
    - `NavigationController`: 画面遷移管琁E���EチE��ュ/ポッチE刁E��替え、履歴管琁E��E    - `LayerController`: レイヤー管琁E��ホ�Eムボタン処琁E��動皁E��先頁E��シスチE���E�E    - `ResourceManager`: リソース管琁E��フォント、画像�EキャチE��ュと遁E��読み込み�E�E    - `HardwareController`: ハ�EドウェアAPI統合（モバイルチE�Eタ、Bluetooth、位置惁E��、カメラ等！E  - Kernelの変更:
    - Phase 3コンポ�Eネント�E初期化をsetup()メソチE��に追加
    - handleHomeButton()をLayerControllerへ完�E委譲�E�互換性のため従来処琁E��維持E��E    - 吁E��ントローラーへの参�Eを保持し、忁E��に応じて連携
  - LayerController機�E:
    - LayerType列挙垁E HOME_SCREEN、APPLICATION、NOTIFICATION、CONTROL_CENTER、POPUP、LOCK_SCREEN
    - 動的レイヤースタチE��管琁E��優先頁E��制御
    - ホ�Eムボタン処琁E�E一允E���E�最上位�E閉じられるレイヤーから頁E��処琁E��E  - ResourceManager機�E:
    - 日本語フォント！Eoto Sans JP�E��E褁E��ClassLoader対応読み込み
    - フォンチE画像キャチE��ュと遁E��読み込み
    - メモリ管琁E��リソース解放機�E
  - HardwareController機�E:
    - 吁E��ードウェアソケチE���E�EobileData、Bluetooth、Location、Camera等）�E統一管琁E    - プラチE��フォーム固有実裁E��Eorge/Standalone�E�への対忁E    - BatteryMonitor統合とハ�Eドウェア状態確認API
  - 動作確誁E
    - standalone:runでの起動確認完亁E    - LayerControllerによるホ�Eムボタン処琁E��正常動佁E    - Phase 3コンポ�Eネント�E初期化ログを確誁E  - 残タスク:
    - Phase 4: SystemGestureHandlerの作�E�E�シスチE��ジェスチャー処琁E��E    - ControlCenterManager/NotificationManagerのclose関連メソチE��実裁E    - MobileDataSocket/BluetoothSocketのisEnabled()メソチE��実裁E  - 結果: **BUILD SUCCESSFUL** - Phase 3統合完亁E��実行時動作確認済み

## 変更(2025-12-04)
- **Kernel構造健全化後�E入力�E琁E��題�E修正�E�第4版！E*
  - 問顁E Phase 1-3のリファクタリング後、スペ�EスキーとESCキーが動作しなくなっぁE    - InputManagerに処琁E��移行したが、KernelがInputManagerに完�Eに委譲してreturnするため処琁E��れなぁE    - LayerControllerのaddLayer/removeLayerがKernelと同期してぁE��ぁE  - 修正冁E��:
    - **Kernel.keyPressed()/keyReleased()の修正**:
      - ESCキー�E�EeyCode=27�E�とスペ�Eスキー�E�EeyCode=32�E��EInputManagerをスキチE�E
      - これら�Eキーは允E�EKernelの処琁E��直接実衁E      - そ�E他�EキーはInputManagerで処琁E    - **InputManagerの修正**:
      - ESCキーとスペ�Eスキーの処琁E��コメントアウト！Eernelで処琁E��るためE��E      - update()メソチE��のESCキー長押し検�Eを削除
    - **Kernel.addLayer()/removeLayer()の修正**:
      - LayerControllerのaddLayer/removeLayerも同時に呼び出して同期
    - **LayerControllerのロガー修正**:
      - java.util.logging.LoggerをLoggerServiceに変更
      - すべてのログ出力を`logger.info("LayerController", message)`形式に統一
  - 結果: **允E�E動作を復允E��スペ�Eスキーによるホ�Eムボタン動作、ESCキー単押しでスリープトグル、ESCキー長押しでシャチE��ダウンが正常に動作すめE*

- **スリープから復帰時�EロチE��画面表示修正**
  - 問顁E PowerManager経由でwake()した場合、ロチE��画面が表示されなぁE  - 修正冁E��:
    - **showLockScreenAfterWake()メソチE��作�E**: ロチE��画面表示処琁E��共通メソチE��として抽出
    - **PowerManager経由のwake()でもロチE��画面表示**: wake()メソチE��でPowerManager経由でもshowLockScreenAfterWake()を呼び出ぁE  - 結果: **スリープから復帰時に忁E��ロチE��画面が表示されるよぁE��修正**

## 変更(2025-12-04)
- **Standalone入力�E琁E�Eバグ修正�E�第3版！E*
  - 問顁E スペ�EスキーとESCキーが正しく動作しなぁE    - スペ�Eスキー: InputManagerからKernel.handleHomeButton()が呼ばれるが、LayerControllerのログが�E力されなぁE    - ESCキー: ProcessingのチE��ォルト終亁E��作でシスチE��がシャチE��ダウンされてぁE��
  - 修正冁E��:
    - **LayerControllerのロガー修正**:
      - java.util.logging.LoggerをLoggerServiceに変更
      - Kernelから`getLogger()`でLoggerServiceを取征E      - すべてのログ出力を`logger.info("LayerController", message)`形式に変更
    - **StandaloneWrapper.java**:
      - スペ�Eスキー処琁E�E改喁E
        - keyPressed()でkey <= 32の条件に変更�E�E11行目�E�E        - スペ�Eスキー専用のチE��チE��ログ追加�E�E15-418行目�E�E        - keyTyped()でもkey <= 32に変更し、スペ�Eスキーを確実に除外！E50行目�E�E      - ESCキー処琁E�E完�Eな修正:
        - handleKeyEvent()をオーバ�Eライドし、ESCキーイベントを完�Eに消費�E�E0-78行目�E�E        - draw()メソチE��でexitCalledフラグを毎フレームリセチE���E�E56-265行目�E�E        - exitActual()のオーバ�Eライド継続！E59-467行目�E�E  - 結果: **LayerControllerのログが正しく出力されるようになり、デバッグが可能に。スペ�Eスキーによるホ�Eムボタン動作、ESCキー2秒長押しによるスリープモードが正常に動作するよぁE��なっぁE*

## 変更(2025-12-01)
- **iOS/Android方式�EチE��スト�E力統一アーキチE��チャ完�E**
  - 目皁E OS全体でCtrl+C/V/X/A等�EクリチE�Eボ�Eド操作を統一皁E��処琁E��めE  - iOSの設計パターンを採用:
    - `TextInputProtocol`: iOS UITextInput相当�Eインターフェース
    - `Screen.getFocusedTextInput()`: iOS UIResponder Chain相彁E    - `ClipboardService`: iOS UIPasteboard相彁E    - `Kernel`: 中央でCtrlショートカチE��を検�Eし、フォーカスされた�E力に操作を委譲
  - 修正冁E��:
    - **ChromiumSurface.java**: TextInputProtocol用メソチE��を追加�E�EasTextInputFocus, getCachedSelectedText, executeScript�E�E    - **DefaultChromiumService.java**: 新しいChromiumSurfaceメソチE��を実裁E    - **ChromiumTextInput.java**: ChromiumBrowserからChromiumSurfaceを使用するように変更
    - **NoteEditScreen.java**: getFocusedTextInput()を実裁E��、旧Ctrl+C/V処琁E��削除
    - **ChromiumBrowserScreen.java**: iOS/Android方式を採用 - フォーカス検�Eに頼らず、ChromiumサーフェスがアクチE��ブな場合�E常にChromiumTextInputを返す
  - 解決した問顁E
    - メモアプリでCtrl+Aが動作しなぁEↁEgetFocusedTextInput()実裁E��解決
    - YouTube等�E褁E��なWebアプリでdocument.activeElementがBODYを返す ↁEiOS/Android方式（常にJS実行）で解決
  - 技術的発要E
    - iOS/AndroidはWebViewでフォーカス検�Eに頼らなぁE    - 常にJavaScript操作を実行し、実際に入力フィールドがあれば動作する方式を採用
  - 結果: **Google検索、YouTube検索、メモアプリ等、すべてのチE��スト�E力でCtrl+C/V/X/Aが正常に動佁E*

- **バックスペ�EスキーのTextInputProtocol統吁E*
  - 問顁E バックスペ�EスがHTMLフィールドに正しく転送されてぁE��かっぁE  - 修正冁E��:
    - **TextInputProtocol.java**: `deleteBackward()`メソチE��を追加
    - **BaseTextInput.java**: `deleteBackward()`を実裁E��選択があれば削除、なければカーソル前�E1斁E��を削除�E�E    - **ChromiumTextInput.java**: JavaScript経由で`deleteBackward()`を実裁E��ENPUT/TEXTAREAはvalue操作、contentEditableはexecCommand�E�E    - **Kernel.java**: バックスペ�Eスキー�E�EeyCode=8�E�をTextInputProtocol経由で処琁E  - 結果: **メモアプリ、Chromiumアドレスバ�E、HTMLフィールド！Eoogle/YouTube等）でバックスペ�Eスが正常に動佁E*

## PDE実行エンジン仕槁E
MochiMobileOS上でProcessingスケチE���E�Epde�E�をアプリケーションとして実行するため�E仕様、E
### 1. アプリケーション形式と自動ローチE
-   **フォルダ構�E**: PDEアプリは、褁E��の`.pde`ファイル、アイコン、およ�Eマニフェストファイルを含む単一のフォルダとして提供される、E-   **配置場所**: ユーザーは、このアプリフォルダを仮想ファイルシスチE��上�E `/apps/` チE��レクトリに配置する、E-   **マニフェスチE(`app.json`)**: アプリフォルダには、以下�E惁E��を含む`app.json`を忁E��とする、E    -   `name` (String): アプリケーション名、E    -   `version` (String): バ�Eジョン番号、E    -   `entry_point` (String): メインとなる`.pde`ファイル名、E    -   `type` (String): `"pde"` とぁE��固定値、E-   **自動ローチE*: OS起動時に`AppLoader`サービスが`/apps/`をスキャンし、`"type": "pde"`のマニフェストを持つフォルダを�E動的にPDEアプリとして認識し、ランチャーに登録する、E
### 2. 実行アーキチE��チャ

-   **動的コンパイル**: PDEアプリは、起動時に`PdeInterpreterService`によってJavaソースコードに変換され、動皁E��コンパイル・ロードされる、E-   **PGraphicsベ�Eスのレンダリング**: コンパイルされたスケチE��は、OSのオフスクリーンレンダリング�E�ESR�E�アーキチE��チャに統合される。`PApplet`インスタンスはUIに直接描画せず、OSから提供される`PGraphics`オブジェクトへの描画エンジンとして機�Eする、E
### 3. OS API連携

-   **安�EなAPIアクセス**: スケチE��からOSの機�Eを安�Eに利用するため、�E開用のAPIラチE��ーを提供する、E-   **カスタム基底クラス (`MmosPdeApplet`)**: スケチE��は、コンパイル時に`MmosPdeApplet`とぁE��カスタム基底クラスを継承する。この基底クラスが、�E開APIへのアクセスポイント（侁E `api`変数�E�を提供する、E-   **利用侁E*: スケチE��開発老E�E、`api.showNotification("タイトル", "メチE��ージ");` のような形式で、準備なしにOSの機�Eを呼び出すことができる、E
### PDE実行エンジン実裁E��スク (TODO)
- **`PdeInterpreterService`の作�E:** .pdeファイルの動的コンパイルとクラスロード機�Eの実裁E��E- **`AppLoader`の拡張:** `app.json`の`type: "pde"`を解釈し、PDEアプリを登録する機�Eの追加、E- **API連携基盤の実裁E**
    - `MmosPdeApplet`カスタム基底クラスの作�E、E    - 公開用`MmosApi`ラチE��ークラスの作�E�E�第一弾として通知機�Eなど�E�、E- **`PdeScreen`の作�E:** コンパイルされたPDEスケチE��をPGraphicsベ�Eスで描画・実行する画面の実裁E��E- **サンプルPDEアプリの作�E:** 動作テストとユースケースを示すため�E簡単なサンプルアプリ、E
## キーボ�Eド�E力�E琁E�E修正�E�E025-12-04�E�E
### 問題と修正冁E��

**問顁E*: Kernel刁E��リファクタリング�E�Ehase 1-3�E�実施後、ESCキーとスペ�Eスキーが動作しなくなった、E
**根本原因**: KernelがInputManagerに処琁E��完�Eに委譲し、�Eの処琁E��スキチE�EしてぁE��ため、E
**修正冁E��**:
1. **Kernel.java**:
   - `keyPressed()`メソチE��で、ESC�E�EeyCode=27�E�とスペ�Eス�E�EeyCode=32�E��EInputManagerをスキチE�Eし、従来処琁E��実衁E   - `keyReleased()`メソチE��も同様に修正
   ```java
   if (inputManager != null) {
       // ESCキー�E�E7�E�とスペ�Eスキー�E�E2�E�以外�EInputManagerで処琁E       if (keyCode != 27 && keyCode != 32 && key != ' ') {
           inputManager.handleKeyPressed(key, keyCode);
           return;
       }
       // ESCとスペ�Eスは下�E従来処琁E��実衁E   }
   ```

2. **ロチE��画面表示の修正**:
   - スリープから復帰時にロチE��画面が表示されなぁE��題を修正
   - `showLockScreenAfterWake()`メソチE��を追加し、PowerManager経由とレガシー経路両方で呼び出ぁE
3. **Chromiumブラウザでのスペ�Eスキー入力修正**:
   - `ChromiumBrowserScreen.hasFocusedComponent()`を修正
   - ChromiumコンチE��チE��アクチE��ブな場合も`true`を返すように変更
   - これによりWebペ�Eジ冁E��のスペ�Eスキー入力が正常に動佁E
**動佁E*:
- ESCキー: 単押しでスリープトグル、E��押し！E秒）でシャチE��ダウン
- スペ�Eスキー: チE��スト�E力フォーカスがなぁE��はホ�Eムボタン、フォーカスがある時は通常の斁E���E劁E- スリープ解除時にロチE��画面が正常に表示されめE
### Chromiumブラウザ冁E��ペ�Eスキー処琁E�E改喁E
**問顁E*: Chromiumブラウザ冁E��チE��スト�EチE��スからフォーカスが外れた状態でもスペ�Eスキーが�Eージスクロールに使われてしまぁE���Eームボタンとして機�EしなぁE��E
**解決方法（最終版�E�E*: 既存�EJavaScriptフォーカス検�EシスチE��を活用:

1. **既存シスチE��の発要E*:
   - ChromiumBrowserに`injectFocusDetectionScript()`メソチE��が既に実裁E��み
   - ペ�Eジロード時にJavaScriptを注入してフォーカス状態を監要E   - input、textarea、contenteditable、role="textbox"等を検�E
   - `[MochiOS:TextFocus]`メチE��ージで状態変更を通知
   - `hasTextInputFocus()`メソチE��で状態を取得可能

2. **JavaScript検�EロジチE��**:
   - Shadow DOM冁E�E要素も検�E�E�EgetDeepActiveElement()`�E�E   - focusin/focusout、click、inputイベントをリチE��ン
   - YouTube等�Eカスタム要素�E�Eole属性�E�にも対忁E   - 選択テキストも監視！EextInputProtocol用�E�E
3. **ChromiumBrowserScreenの修正**:
   - ヒューリスチE��チE��処琁E��削除
   - `hasFocusedComponent()`でChromiumSurface.hasTextInputFocus()を使用
   - アドレスバ�Eまた�EWeb冁E��キスト�E力フォーカスでtrue返却

**動佁E*:
- チE��スト�EチE��スにフォーカス晁E スペ�Eスキーは斁E���E劁E- フォーカス夁E スペ�Eスキーはホ�Eムボタンとして機�E
- 正確なフォーカス検�E�E�モバイルOSと同等！E
## OS構造最適匁EPhase 4: イベントバスシスチE���E�E025-12-04�E�E
### 実裁E�E容

**目皁E*: イベント駁E��アーキチE��チャによるコンポ�Eネント間の疎結合化を実現

**作�Eされたクラス**:

1. **イベントバスコア** (`core/src/main/java/jp/moyashi/phoneos/core/event/`):
   - `Event`: 基底イベントクラス�E�キャンセル/消費機�E付き�E�E   - `EventType`: シスチE��全体�Eイベントタイプ定義
   - `EventListener<T>`: イベントリスナ�Eインターフェース
   - `EventFilter<T>`: イベントフィルタリングインターフェース
   - `EventBus`: Publish-Subscribe パターンの中核実裁E     - スレチE��セーフ設訁E     - 優先度付きリスナ�E
     - 非同朁E同期イベント�E信
     - イベント履歴管琁E
2. **イベント実裁E��ラス**:
   - `system/SystemEvent`: シスチE��ライフサイクルイベント（起勁E終亁EスリーチEウェイク�E�E   - `app/ApplicationEvent`: アプリケーションイベント（起勁E終亁Eフォーカス�E�E   - `ui/ScreenEvent`: 画面遷移イベント（�EチE��ュ/ポッチE遷移タイプ！E
3. **既存コンポ�Eネント�E統吁E*:
   - `PowerManager`: スリーチEウェイク時にSystemEventを発衁E   - `NavigationController`: 画面遷移時にScreenEventを発行（キャンセル可能�E�E   - `Kernel`: シスチE��起動時にEventBus初期化、シャチE��ダウン時にイベント発衁E
**技術的特徴**:
- **疎結合匁E*: コンポ�Eネント間の直接皁E��依存関係を削渁E- **拡張性**: 新しいイベントタイプとリスナ�Eの追加が容昁E- **チE��チE��性**: イベント履歴とチE��チE��モードによる詳細ログ
- **パフォーマンス**: 非同期イベント�E信とキャチE��ュ済みスレチE��プ�Eル

**結果**:
- コンポ�Eネント間通信の標準化と疎結合化により、シスチE��の保守性と拡張性が向丁E- BUILD SUCCESSFUL - 全コンパイルエラー解涁E
## 変更(2025-12-13)
- **WebScreen画面真っ白問題�E修正**�E�Ehase 1�E�E  - 問顁E WebScreenでChromiumの描画が真っ白のまま
  - 原因: GLCanvasのOpenGLコンチE��ストが初期化される前にonPaintが呼ばれ、早期リターンしてぁE��
  - 修正冁E��:
    - **ChromiumBrowser.java**:
      - `CountDownLatch` と `glContextReady` フラグを追加してGLContext初期化完亁E��同期匁E      - `SwingUtilities.invokeLater()` 冁E��GLCanvas追加後に200ms征E��し、`glInitLatch.countDown()` を呼び出ぁE      - `loadURL()` で `waitForGLContext(3000)` を呼び出してGLContext準備完亁E��征E��E      - `isReadyToRender()` を改喁E��、`glContextReady` フラグを優先チェチE��
    - **ChromiumSurface.java**:
      - `isReadyToRender()` メソチE��をインターフェースに追加
    - **DefaultChromiumService.java**:
      - `DefaultChromiumSurface` 冁E��クラスに `isReadyToRender()` を実裁E    - **WebScreen.java**:
      - 時間ベ�EスのリトライロジチE��めE`surface.isReadyToRender()` チェチE��に改喁E      - GLContext準備完亁E��確認してからreload()を実衁E  - 結果: **BUILD SUCCESSFUL** - core/forge/standaloneの全モジュールでコンパイル成功

## 変更(2025-12-14)
- **ホ�EムボタンシスチE�� アーキチE��チャ再設訁E*
  - 目皁E スペ�Eスキーを�Eームボタンとして扱ぁE��計を廁E��し、�EラチE��フォーム固有�E実裁E��刁E��
  - 設計変更:
    - **Core**: スペ�Eスキー特別処琁E��削除。`requestGoHome()` API のみを提侁E    - **Standalone**: 別ウィンドウにホ�Eムボタン表示 + Ctrl+SpaceショートカチE��
    - **Forge**: 画面上にホ�Eムボタンを常時表示 + Ctrl+SpaceショートカチE��
  - 修正冁E��:
    - **Kernel.java**:
      - `requestGoHome()` メソチE��追加 - プラチE��フォーム層からの公開API
      - `hasTextInputFocus()` メソチE��追加 - チE��スト�E力フォーカス判宁E      - スペ�Eスキー特別処琁E��削除�E�通常のキー入力として扱ぁE��E    - **ScreenManager.java**:
      - スペ�Eスキー特別処琁E��削除�E�通常のキーとしてスクリーンに転送E��E    - **StandaloneWrapper.java**:
      - Ctrl+Space検�Eを追加 ↁE`kernel.requestGoHome()` 呼び出ぁE      - スペ�Eスキーを通常の斁E��として扱ぁE��ぁE��修正�E�EkeyTyped()` で処琁E��E    - **HardwareWindow.java**: 新規作�E�E�ハードウェアボタンエミュレーション�E�E      - メインウィンドウの右側に配置される独立Swingウィンドウ
      - 音量アチE�Eボタン�E�上！E TODO: 音量�E琁E��裁E      - ホ�Eムボタン�E�中央�E�E `kernel.requestGoHome()` 呼び出ぁE      - 音量ダウンボタン�E�下！E TODO: 音量�E琁E��裁E      - ボタン外領域をドラチE��してウィンドウ移動可能
    - **ProcessingScreen.java (Forge)**:
      - Ctrl+Space検�Eを追加 ↁE`kernel.requestGoHome()` 呼び出ぁE      - ホ�Eムボタンを常時表示�E�EhromiumBrowserScreen限定を解除�E�E      - `handleHomeButtonClick()` めE`kernel.requestGoHome()` に変更
      - スペ�Eスキーを通常の斁E��として扱ぁE��ぁE��修正�E�EcharTyped()` で処琁E��E  - 動作フロー:
    ```
    [Standalone環境]
    Ctrl+Space押丁Eまた�E HomeButtonWindow クリチE�� ↁEkernel.requestGoHome() ↁEhandleHomeButton()

    [Forge環境]
    Ctrl+Space押丁Eまた�E 画面上�EームボタンクリチE�� ↁEkernel.requestGoHome() ↁEhandleHomeButton()

    [スペ�Eスキー]
    通常のキー入力として処琁E��テキスト�E力フィールドに ' ' 入力！E    ```
  - 結果: **BUILD SUCCESSFUL** - スペ�Eスキーがテキスト�E力として正常動作、�Eームへの移動�ECtrl+Spaceまた�E専用ボタンで実現

## 変更(2025-12-17)
- **Forge環墁E��のAWT/LWJGL競合問題�E解決�E�EefBrowserOsrNoCanvas�E�E*
  - 問顁E Forge環墁E��JCEFのCefBrowserOsr�E�EWTのGLCanvas使用�E�を使用すると、MinecraftのLWJGLと競合してAWT HeadlessExceptionが発甁E  - 原因: CefBrowserOsrはAWTのGLCanvasを使用してOpenGLレンダリングを行うが、MinecraftはLWJGLを使用しており、両老E��競合すめE  - 解決筁E Processing P2D vs LWJGL競合�E解決と同様に、AWTを使用しなぁE��フスクリーンブラウザを作�E
  - 作�Eされたクラス:
    - **`forge/src/main/java/org/cef/browser/CefBrowserOsrNoCanvas.java`**:
      - CefBrowser_Nを継承し、CefRenderHandlerを実裁E      - AWTのGLCanvasを使用せず、onPaint()でByteBuffer→int[]配�EにピクセルチE�Eタを保孁E      - onPaintListenerに通知してChromiumRenderHandlerに転送E      - setSize()でwasResized()とsetWindowVisibility(true)を呼び出してレンダリングをトリガー
    - **`forge/src/main/java/org/cef/browser/CefBrowserFactory.java`**:
      - NoCanvasモード�Eブラウザを作�Eするファクトリ
      - `setNoCanvasMode(true)`でCefBrowserOsrNoCanvasを使用
      - `createNoCanvas()`で直接NoCanvasブラウザを作�E
  - 修正されたクラス:
    - **`ForgeChromiumProvider.java`**:
      - `supportsUIComponent()`をオーバ�Eライドしてfalseを返す
      - `createBrowser()`でCefBrowserOsrNoCanvasを作�E
      - NoCanvasモードを有効匁E    - **`ChromiumBrowser.java`**:
      - `provider.supportsUIComponent()`がfalseの場合�Eelse刁E��を追加
      - NoCanvasモードではhidden JFrameを作�Eせず、`tryTriggerRendering()`を呼び出ぁE      - `tryTriggerRendering()`でsetSize()メソチE��を優先的に呼び出ぁE  - 動作フロー:
    ```
    [Forge環境]
    ForgeChromiumProvider.createBrowser()
    ↁECefBrowserFactory.createNoCanvas()
    ↁECefBrowserOsrNoCanvas作�E
    ↁEcreateImmediately()
    ↁEChromiumBrowser: supportsUIComponent()=false
    ↁEtryTriggerRendering() ↁEsetSize() ↁEwasResized() + setWindowVisibility(true)
    ↁECEFがonPaint()を呼び出ぁE    ↁECefBrowserOsrNoCanvasがonPaintListenerに通知
    ↁEChromiumRenderHandlerがPImageに変換
    ↁEProcessingScreen経由でMinecraftチE��スチャに描画
    ```
  - 結果: **AWT HeadlessException問題が解決し、Forge環墁E��Chromiumブラウザが正常にレンダリングされるよぁE��なっぁE*

## 変更(2025-12-18)
- **サーバ�Eモジュールの作�E�E�EPvMシスチE��サーバ�Eインフラ�E�E*
  - 目皁E サーバ�Eサイド�E仮想ネットワークシスチE��を構築し、クライアント！Ehromium�E�から�EIPvMリクエストをサーバ�Eで処琁E��めE  - 背景: 従来はクライアント�EでチE��トサーバ�Eを登録してぁE��が、正しい設計としてサーバ�E側で管琁E��べぁE  - 作�Eされたモジュール: `server`
    - `server/build.gradle.kts`: coreモジュールに依存、Gson追加
    - `settings.gradle.kts`に`server`を追加
  - 作�Eされたクラス:
    - **`server/src/main/java/jp/moyashi/phoneos/server/MMOSServer.java`**:
      - サーバ�EモジュールのエントリーポインチE      - `initialize()`: 絁E��込みサーバ�E�E�Eys-test等）を登録
      - `handleHttpRequest(VirtualPacket)`: クライアントから�EHTTPリクエストを処琁E      - `registerSystemServer()`/`registerExternalServer()`: サーバ�E登録API
    - **`server/src/main/java/jp/moyashi/phoneos/server/network/VirtualHttpServer.java`**:
      - 仮想HTTPサーバ�Eのインターフェース
      - `getServerId()`: サーバ�E識別子（侁E "sys-test"�E�E      - `handleRequest(VirtualHttpRequest)`: リクエスト�E琁E    - **`server/src/main/java/jp/moyashi/phoneos/server/network/VirtualHttpRequest.java`**:
      - HTTPリクエストデータクラス�E�Eource、destination、method、path、headers、body�E�E      - Builderパターンで構篁E    - **`server/src/main/java/jp/moyashi/phoneos/server/network/VirtualHttpResponse.java`**:
      - HTTPレスポンスチE�Eタクラス�E�EtatusCode、statusText、headers、body、mimeType�E�E      - ファクトリメソチE��: `ok()`, `html()`, `json()`, `notFound()`, `error()`
    - **`server/src/main/java/jp/moyashi/phoneos/server/network/SystemServerRegistry.java`**:
      - シスチE��サーバ�E�E�Eype 3�E�と外部サーバ�E�E�Eype 2�E��E登録レジストリ
      - シングルトンパターン
      - IPvMアドレスからサーバ�Eを検索
    - **`server/src/main/java/jp/moyashi/phoneos/server/network/ServerVirtualRouter.java`**:
      - サーバ�Eサイド�EパケチE��ルーチE��ング
      - VirtualPacketをVirtualHttpRequestに変換
      - SystemServerRegistryからサーバ�Eを検索してリクエスト�E琁E    - **`server/src/main/java/jp/moyashi/phoneos/server/network/builtin/TestSystemServer.java`**:
      - チE��ト用シスチE��サーバ�E�E�E-sys-test�E�E      - `/`: チE��ト�Eージ�E�接続�E功表示�E�E      - `/api/echo`: Echoエンド�EインチE      - `/api/info`: サーバ�E惁E��エンド�EインチE  - Forgeモジュールの修正:
    - **`forge/build.gradle`**:
      - `implementation project(':server')` 追加
      - JARビルドにサーバ�Eモジュールクラスを含める
      - runClient設定にサーバ�Eモジュールソースを追加
    - **`MochiMobileOSMod.java`**:
      - `commonSetup()`で`MMOSServer.initialize()`を呼び出ぁE    - **`NetworkHandler.java`**:
      - `handleServerSide()`で`MMOSServer.handleHttpRequest()`を使用
      - `sendToPlayer(VirtualPacket, ServerPlayer)`メソチE��追加
    - **`ForgeNetworkInitializer.java`**:
      - クライアント�EのチE��トサーバ�E登録コードを削除
      - レスポンスハンドラーのみを登録
    - **`ForgeVirtualSocket.java`**:
      - `onPacketReceived()`をサーバ�Eモジュールのレスポンスフォーマットに対忁E      - statusCode、statusText、mimeType、bodyを正しく処琁E  - パケチE��フロー:
    ```
    [クライアンチE
    Chromium ↁEMochiResourceRequestHandler ↁEVirtualNetworkResourceHandler
    ↁEForgeVirtualSocket.httpRequest() ↁENetworkHandler.sendToServer()

    [サーバ�E]
    NetworkHandler.handleReceivedPacket() ↁEhandleServerSide()
    ↁEMMOSServer.handleHttpRequest() ↁEServerVirtualRouter.routeRequest()
    ↁESystemServerRegistry.getServer() ↁETestSystemServer.handleRequest()
    ↁEVirtualHttpResponse ↁEVirtualPacket ↁENetworkHandler.sendToPlayer()

    [クライアンチE
    NetworkHandler.handleClientSide() ↁEForgeNetworkInitializer.onPacketReceived()
    ↁEForgeVirtualSocket.onPacketReceived() ↁECompletableFuture完亁E    ↁEVirtualNetworkResourceHandler ↁEChromiumに表示
    ```
  - 結果: **BUILD SUCCESSFUL** - サーバ�EモジュールとForgeモジュールの統合完亁E
- **JCEF getResourceRequestHandler() 問題�E修正�E�EnBeforeBrowseアプローチE��E*
  - 問顁E Forge環墁E��EefBrowserOsrNoCanvas�E�で`CefRequestHandler.getResourceRequestHandler()`がネイチE��ブレベルで呼び出されず、IPvMアドレス�E�Ehttp://3-sys-test/`等）へのリクエストがインターセプトできなぁE  - 原因刁E��:
    - `CefBrowserOsrNoCanvas`はAWTを使用しなぁE��スタムOSRブラウザ実裁E    - `CefRequestHandler`のコールバック�E�EgetResourceRequestHandler`�E�がネイチE��ブCEFから呼び出されなぁE    - `client.removeRequestHandler()`を追加しても根本皁E��解決にならなぁE    - `CefSchemeHandlerFactory`によるHTTPスキームのインターセプトも動作しなぁE��ファクトリが呼び出されなぁE��E  - 解決筁E `CefRequestHandler.onBeforeBrowse()`を使用したナビゲーションインターセプト
    - `onBeforeBrowse()`はCefBrowserOsrNoCanvasでも確実に呼び出されめE    - IPvMアドレスパターン�E�E0-*`, `1-*`, `2-*`, `3-*`�E�にマッチするURLを検�E
    - 允E�Eナビゲーションをキャンセル�E�Eeturn true�E�し、VirtualAdapterで非同期リクエストを実衁E    - レスポンスHTMLをdata: URL経由でブラウザに読み込み
  - 実裁E��細:
    - **`ChromiumBrowser.java`**:
      - `isIPvMUrl(String url)`: URLがIPvMパターンにマッチするか判宁E      - `handleIPvMRequest(CefBrowser browser, String url)`: VirtualAdapterで非同期リクエストを実行し、レスポンスをdata: URLで読み込み
      - `loadHtmlContent()`, `loadErrorPage()`, `loadNoServicePage()`: レスポンス表示用ヘルパ�EメソチE��
      - `onBeforeBrowse()`冁E��IPvM URLを検�Eして処琁E  - 結果: **動作確認済み** - onBeforeBrowse()経由でIPvMアドレスが正しくインターセプトされ、VirtualAdapter経由でサーバ�Eからのレスポンスが表示されめE
- **IPvM URL表示シスチE��の改喁E��Ettpm://スキーム表示�E�E*
  - 問顁E data: URL方式ではURLバ�Eに「data:」が表示され、ユーザーにとって刁E��りにくい
  - 要件: URLバ�Eに`httpm://3-sys-test/`のような意味のあるURLを表示したぁE  - 試行した解決筁E
    1. **CefResourceHandler**: OSRモードでHTMLがレンダリングされなぁE��白画面�E�E    2. **httpm://カスタムスキーム**: CefSchemeHandlerFactory経由でもOSRレンダリング問題�E解決せず
    3. **CefFrame.loadString()**: こ�EJCEF実裁E��は存在しなぁE    4. **history.replaceState()**: data: URLでは同一オリジンポリシーにより動作しなぁE  - 最終解決筁E **displayUrlフィールドによるUI層での仮想URL表示**
    - `ChromiumBrowser`に`displayUrl`フィールドを追加
    - IPvM URLロード時に`displayUrl`を設定（侁E `httpm://3-sys-test/`�E�E    - `getCurrentURL()`メソチE��を修正: 実際のURLがdata:の場合�E`displayUrl`を返す
    - 通常URL�E�非data:�E�をロードする場合�E`displayUrl`をクリア
  - 実裁E��細:
    - **`ChromiumBrowser.java`**:
      - `displayUrl`フィールチE IPvM用の表示URL保持
      - `loadIPvMContent()`: HTML取得後、`loadHtmlWithUrl()`を呼び出ぁE      - `loadHtmlWithUrl()`: `displayUrl`設定後、data: URLでHTML読み込み
      - `getCurrentURL()`: `displayUrl`が設定済み かつ `currentUrl`がdata:で始まる場合�E`displayUrl`を返す
      - `loadURL()`: 非data: URLの場合�E`displayUrl`をnullにリセチE��
    - `onBeforeBrowse()`で`http://`と`httpm://`両方のIPvM URLを�E琁E      - `http://3-sys-test/` ↁEdisplayUrl=`httpm://3-sys-test/`、data: URLでローチE      - `httpm://3-sys-test/` ↁEdisplayUrl=`httpm://3-sys-test/`、data: URLでローチE  - 結果: **動作確認済み** - URLバ�Eに`httpm://3-sys-test/`が正しく表示されめE
## 変更(2026-01-05)
- **オーチE��オシスチE��アーキチE��チャの刷新**
  - 目皁E MMOS�E�ストリーミング再生、Push型）とAdvancedVC 2.0�E�フレームポ�Eリング、Poll型）�Eインピ�Eダンスミスマッチを解消し、ブチE�Eれ�EなぁE��定した音声送信を実現する、E  - **ByteRingBufferの導�E**:
    - スレチE��セーフなバイトリングバッファを実裁E(`jp.moyashi.phoneos.forge.audio.ByteRingBuffer`)、E    - Push/Poll間�E速度差・サイズ差を吸収、E  - **AVCAudioBridgeの改修**:
    - `ConcurrentLinkedQueue` を廁E��し、リングバッファ方式に変更、E    - `pollSpeakerFrame` で常に5760バイト！E0ms�E��E完�Eなフレームを返すように変更�E�データ不足時�Enullを返し、AVC側で無音処琁E��、E    - MMOSからのチE�Eタ供給タイミングとAVCのポ�Eリングタイミングを�E離、E  - **ForgeSpeakerSocketの改修**:
    - `playAudio` をノンブロチE��ング化（リングバッファへの書き込みのみ�E�、E    - バックグラウンドワーカー (`workerThread`) を導�Eし、SourceDataLineへの書き込み�E��E刁E��再生�E�とAVC送信を並行して管琁E��E    - 連続的なストリーミング再生時でもスレチE��の再作�Eを行わず、スムーズな再生を実現、E  - 結果: **BUILD SUCCESSFUL** - 警告�Eみでエラーなし。ストリーミング再生の安定性が大幁E��向上する見込み、E
## 変更(2026-01-06)
- **初回セチE��アチE�Eフローの実裁E*
  - 目皁E OS初回起動時にインスト�Eル画面と初期設定ウィザードを表示する、E  - **仕槁E*:
    - SettingsManagerに `system.setup_completed` フラグを追加�E�デフォルチE false�E�、E    - Kernel初期化時にフラグを確認し、未完亁E�E場合�E SetupApp を起動、E    - セチE��アチE�E完亁E��にフラグめEtrue に更新し、�Eーム画面へ遷移、E  - **実裁E��ラス**:
    - `jp.moyashi.phoneos.core.apps.setup.SetupApp`: セチE��アチE�E用アプリ�E�ランチャー非表示�E�、E    - `jp.moyashi.phoneos.core.apps.setup.InstallationScreen`: インスト�Eル進行状況E���Eログレスバ�E�E�を表示する画面、E    - `jp.moyashi.phoneos.core.apps.setup.SetupWizardScreen`: 言語設定等�Eウィザード画面�E�現在はモチE��実裁E��、E  - **リソースと機�E拡張**:
    - `ResourceManager.loadImage()` を拡張し、クラスパス�E�EAR冁E��ソース�E�から�E読み込みフォールバックを実裁E��E    - `InstallationScreen` にカスタムロゴ�E�Esetup_logo.png`�E��E表示機�Eを追加、E  - **Kernel変更**:
    - `setup()` メソチE��冁E�� `lockManager.isLocked()` チェチE��の前に `system.setup_completed` を確認するよぁE��変更、E- **初回セチE��アチE�Eフローのバグ修正**
  - **NullPointerException修正**: `InstallationScreen` 及�E `SetupWizardScreen` において、`ScreenManager` の取得方法を `kernel.getService(ScreenManager.class)` から `kernel.getScreenManager()` に変更�E�EIコンチE��未登録時�Eフォールバック�E�、E  - **コンパイルエラー修正**: `SetupWizardScreen` において、存在しなぁE`setScreen()` メソチE��の使用めE`clearAllScreens()` と `pushScreen()` の絁E��合わせに修正、E- **初回セチE��アチE�Eフローのバグ修正 (InstallationScreen)**
  - **画面遷移ループ�E防止**: `InstallationScreen` に `isFinished` フラグを追加。進行度ぁE00%に達して画面遷移した後、�E度 `pushScreen` が呼ばれなぁE��ぁE��修正、E- **初回セチE��アチE�EフローのUI刷新 (InstallationScreen / SetupWizardScreen)**
  - **"Nebula & Glass" チE�Eマ�E導�E**: 深ぁE��E���EようなグラチE�Eション背景と浮遊するパーチE��クルによる没入感�EあるチE��イン、E  - **アニメーション強匁E*: ロゴの呼吸効果、�Eログレスバ�Eの発光表現、コンチE��チE�Eフェードイン/スライドイン、�Eタンのブリージングアニメーションを実裁E��E  - **パ�EチE��クルシスチE��**: 視覚的な豊かさを与える軽量なパ�EチE��クルエフェクトを追加、E  - **ガラスモーフィズム**: セチE��アチE�Eウィザード�Eカードに半透�Eのガラス風チE��インを採用、E- **InstallationScreenのUI調整**
  - **ロゴ描画修正**: 正方形に強制されてぁE��ロゴ描画を、�E画像�Eアスペクト比を維持するよぁE��修正�E�長辺基準でリサイズ�E�、E  - **アニメーション調整**: ロゴとボタンのブリージング�E�呼吸�E�アニメーションを削除し、E��皁E��重厚感�Eあるスタイルに変更、E- **セチE��アチE�E画面の描画座標修正 (InstallationScreen / SetupWizardScreen)**
  - **PGraphics依存�Eサイズ管琁E*: `kernel.width/height` の代わりに `PGraphics.width/height` を使用するように修正。これにより、Forge環墁E���Eオフスクリーンレンダリング時でも正しいサイズとアスペクト比で描画されるよぁE��なった、E  - **状態同朁E*: `draw` メソチE��冁E��画面サイズを更新し、パーチE��クルやUIレイアウトが常に最新の描画領域に適合するよぁE��調整、E
## [2026-01-21] AI Review Loop 1
- Status: Build Passed & Committed
- Review Summary: PowerManagerから未使用定数SLEEP_FPSを削除�E�スリープ中FPS制限機�Eの削除に伴ぁE��要となった）。対象ファイル�E�EoggerService, VFS, NotificationManager, PowerManager, SmartphoneBackgroundService�E�をレビューし、E��大な品質問題�E検�Eされず、E\
## 変更(2026-01-27)
- **SettingsScreen UIアーキチE��チャの刷新**
  - 問顁E パネル�E�Eppearance, Battery等）が半透�Eで描画され、背面のメインリストと重なって表示されるバグがあった。また、各パネルでヘッダー描画ロジチE��が重褁E�E散逸してぁE��、E  - 修正冁E��:
    - **描画ロジチE��の刁E��**: draw()メソチE��をリファクタリングし、サブパネル表示中はメインリスト�E描画をスキチE�Eするように変更。これにより透過バグを解消、E    - **ヘッダーの統一**: すべてのサブパネルで drawPanelHeader() を使用するように統一。手動でのチE��スト描画を削除し、UIの一貫性を向上、E    - **重褁E��画の削除**: About System, Sound & Vibration, Storage パネルにおいて、Panel クラスと手動描画で二重に描画されてぁE��コンポ�Eネント�E描画処琁E��削除。パフォーマンスを改喁E��E
\
## 変更(2026-01-27) 追訁E- **SettingsScreen UI微修正**
  - Appearanceパネルで消えてぁE��Accent ColorプリセチE��の手動描画処琁E��復允E��E  - Notificationsパネルで説明テキストがスイチE��と重なる問題を修正、Eabelコンポ�Eネントを使用して自動レイアウトに対応させた、E
\
## 変更(2026-01-27) 追訁E
- **SettingsScreen レイアウト�Eコンパクト化**
  - 設定一覧の吁E��イチE��の高さめE70 -> 52 に縮小。これにより、�EアイチE���E�Ebout System 含む�E�が画面冁E��収まるよぁE��なった、E  - 吁E��ブパネルの高さ�E�Eh�E�を 480 -> 440 に縮小。画面下部のシスチE��ジェスチャー領域との干渉を防ぁE��、E  - About System パネル冁E�E頁E��間隔を調整し、縮小されたパネル冁E��すべての惁E��が収まるよぁE��修正、E  - シスチE��惁E���E�ES名、バージョン等）�E表示位置を、設定リスト�E直後に動的に配置されるよぁE��喁E��E

## 変更(2026-02-05)
- **AVC �A�g�̖�����**
  - MMOS ���� AVC �A�g�͊���Ŗ������i`mmos.avc.disable` �� true �̏ꍇ�� AVC ���g�p���Ȃ��j
  - �L��������ꍇ�� JVM �N�������� `-Dmmos.avc.disable=false`

