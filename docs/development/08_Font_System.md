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
