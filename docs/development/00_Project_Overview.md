# MochiMobileOS 開発ドキュメント

### 1. プロジェクト仕様書

**概要**: Java/ProcessingベースのスマートフォンOSシミュレータ。最終的にMinecraft Forge MODとしてゲーム内での利用を目指す。

**アーキテクチャ**:
*   **`core`**: OSの純粋なロジック (環境非依存)
*   **`standalone`**: PCでの実行用ランチャー
*   **`forge`**: Minecraftとの連携用ブリッジ (実装中)
*   **カーネル**: サービス指向 (`VFS`, `AppLoader`, `GestureManager`等)
*   **統合MODモデル**: 外部MODがMochiMobileOSアプリとして自己登録できるAPIを提供。

**主要機能**:
*   **システム**: ロック画面、コントロールセンター、通知センター
*   **ランチャー**: マルチページホーム、編集モード、Appライブラリ、Dock

**有効なサービス**:
VFS, SettingsManager, SystemClock, AppLoader, LayoutManager, PopupManager, GestureManager, ControlCenterManager, NotificationManager, LockManager, LayerManager, LoggerService, WebViewManager, ChromiumManager, PermissionManager, ActivityManager, ClipboardManager, SensorManager

**組み込みアプリ**:
LauncherApp, SettingsApp, CalculatorApp, AppStoreApp (UIのみ), NetworkApp (メッセージ&ネットワークテスト), HardwareTestApp (ハードウェアAPIデバッグ), VoiceMemoApp (音声録音・再生), CalculatorHTMLApp (HTML/CSS/JS電卓、WebView統合デモ), NoteApp (メモアプリ、ClipboardManager統合デモ), BrowserApp (JavaFX WebViewベースブラウザ、http/https/httpm対応、モバイル最適化済み), ChromiumBrowserApp (✅ Phase 5完了: addOnPaintListener()を使用したRenderHandler登録、ビルド成功)
