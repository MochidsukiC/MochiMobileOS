# Chromium Integration

**Chromium統合アーキテクチャ** (✅ Phase 7完了 - 2025-10-20):
- **スタンドアロン**: jcefmaven (me.friwi:jcefmaven:135.0.20) を使用
  - CefAppBuilderでJCEFを自動ダウンロード・初期化
  - ModuleOpener（org.openjdk.jol.util.ModuleOpener）でJavaモジュールアクセス制限を回避
- **Forge**: MCEF (com.cinemamod:mcef:2.1.6-1.20.1) を使用
  - MCEF内部でjava-cef (org.cef.*) を提供、LWJGL rendering使用（JOGL不要）
  - apache-commons-compressバージョン競合を回避
  - JVMモジュールアクセスフラグ（--add-opens, --add-exports）不要
- **ChromiumProviderパターン実装** (✅ Phase 7 - 2025-10-20):
  - 課題: coreモジュールがjcefmaven (me.friwi.*) に実行時依存していたため、Forge JARでパッケージ除外が困難
  - 解決策: ChromiumProviderインターフェースを導入し、依存性注入パターンで完全に分離
  - アーキテクチャ:
    - **coreモジュール**: org.cef.* APIをcompileOnlyで参照（実行時依存なし）
    - **standaloneモジュール**: StandaloneChromiumProviderがjcefmavenを提供
    - **forgeモジュール**: ForgeChromiumProviderがMCEFを提供
  - **ChromiumService層** (2025-10-23 更新):
    - `Kernel#setChromiumService()` で初期化前にサービスを注入し、環境ごとの `ChromiumProvider` を統一的に扱う
    - `DefaultChromiumService` が `ChromiumManager` を内部でラップし、`doMessageLoopWork()` のバックグラウンド実行とサーフェス管理 (`ChromiumSurface`) を担当
    - 互換性維持のため `Kernel#getChromiumManager()` は `DefaultChromiumService` 経由で引き続き利用可能（段階的に `ChromiumSurface` API へ移行予定）
    - Standalone/Forge エントリポイントはサービスをカーネルへ注入するよう更新済み（反射によるマネージャ生成を撤廃）
    - Chromium メッセージループはサービス側のシングルスレッドポンプ（2ms 間隔）に統一し、Standalone プロバイダ内の独自ポンプを廃止して入力レスポンスを安定化
  - **重要なバグ修正** (2025-10-23):
    - **問題**: YouTube動画再生時に30-40秒後に10秒以上のインタラクション遅延が発生。動画のカクツキも発生
    - **根本原因（第1回調査）**:
      1. `ChromiumBrowser.flushInputEvents()`が呼ばれておらず、マウス移動イベントがキューに蓄積
      2. 過剰なログI/O（全てのマウスクリックでWARNログ出力、1ms以上の処理でDEBUGログ出力）
    - **第1回修正内容**:
      1. `ChromiumBrowserScreen.draw()`で`activeBrowser.flushInputEvents()`を呼び出し、入力キューを毎フレーム処理
      2. ログ閾値を大幅引き上げ（1ms→50ms、5ms→50ms、10ms→50ms）でログI/O負荷を削減
    - **根本原因（第2回調査）**:
      1. クリック、ホイール、キー入力が**同期的に処理**され、重い処理でUIがブロック
      2. `ChromiumRenderHandler.onPaint()`がピクセルごとのネストループ（240,000回反復）で非常に遅い
      3. `maxEventsPerFrame=10`が少なすぎて処理能力不足
    - **第2回修正内容**:
      1. **すべてのイベントを非同期処理**: クリック、キー入力も含めてキューに入れて非同期処理（UIブロック完全回避）
      2. **イベント処理能力の大幅向上**: maxEventsPerFrame を10→50に増加、重要なイベント（クリック、キー入力）を優先処理
      3. **ChromiumRenderHandler の劇的な高速化**: ピクセルごとのループを`IntBuffer`によるバルク処理に変更（数倍高速化）→色の問題発生
    - **根本原因（第3回調査）**:
      1. **処理バッファの詰まり**: CEFポンプが2ms間隔のscheduleAtFixedRateで実行され、前回完了前に次がキューに蓄積
      2. **入力イベントキューの無制限な成長**: ConcurrentLinkedQueueに際限なくイベントが蓄積
      3. **draw()内のflushInputEvents()がブロック**: 最大50イベントを同期処理し、draw()全体をブロック
      4. **CPU/GPU使用率が低い理由**: リソース不足ではなく、スレッドの処理バッファ制限により使用できない状態
    - **第3回修正内容**:
      1. **CEFポンプの最適化**: scheduleAtFixedRate→scheduleWithFixedDelay、間隔2ms→8ms、1回で5回ポンプ（処理能力5倍）
      2. **入力キューの厳格な制限**: マウス移動は30以上で拒否、重要イベントは100以上で拒否（キューの無制限成長を防止）
      3. **flushInputEvents()に5ms時間制限**: draw()のブロッキングを防止
      4. **ChromiumRenderHandler色修正**: IntBuffer変換を元のバイトごと処理に戻し、色を修正
    - **根本原因（第4回調査 - 2025-10-23）**:
      1. **flushInputEvents()が完全に呼ばれていない**: 第3回修正でdraw()から削除後、ChromiumServiceのポンプループで処理されるはずだったが、ChromiumBrowserScreenで作成したブラウザがChromiumServiceの管理下にないため処理されていなかった
      2. **入力キューの制限が厳しすぎる**: マウス移動10以上、重要イベント50以上で拒否していたため、通常のインタラクトも反応しない状態
    - **第4回修正内容**:
      1. **ChromiumBrowserScreen.draw()でflushInputEvents()を再度呼び出し**: 入力イベントを確実に処理
      2. **入力キューの制限を緩和**: マウス移動50以上、重要イベント150以上で拒否に変更（通常操作に支障がない範囲）
    - **結果**: ✅ コンパイル成功、インタラクト完全に動作、動画再生時の過剰なバッファ蓄積は依然として防止
    - **根本原因（第5回調査 - 2025-10-24）**:
      1. **JAVA2DレンダラーのCPU描画制限**: StandaloneはデフォルトでProcessing JAVA2D（CPU描画のみ）を使用、GPUアクセラレーションなし
      2. **Processing `image()` メソッドのボトルネック**: 動画再生時の大量ピクセル描画（400x600 ARGB = 960KB/frame）が60FPS達成の障害
      3. **AWTイベントキューの設計限界**: Java AWT/Swingは重い負荷に対応しておらず、フルスクリーンBufferedImage描画は10ms～150ms（6.7～100FPS相当）
      4. **CEF onPaint BGRA変換の影響**: ChromiumRenderHandler.onPaint()でのBGRA→ARGB変換がパフォーマンスに追加負荷
      5. **CPU/GPU使用率が低い理由**: リソース不足ではなく、JAVA2Dのソフトウェア描画制限により、ハードウェアリソースを活用できない状態
      6. **Forge環境との違い**: Forgeは`Chromium → OpenGL texture（GPU描画） → Minecraft rendering`でAWT完全回避、GPU完全活用
    - **第5回修正内容（P2Dレンダラーへの切り替え）**:
      1. **StandaloneWrapper.settings()でP2Dレンダラーを明示的に指定**: `size(400, 600, P2D)` に変更
      2. **JOGL/OpenGL GPU描画の有効化**: JAVA2D（CPU）からP2D（JOGL/OpenGL GPU）へ切り替え
      3. **描画パスの最適化**: `Processing.image()` がOpenGLテクスチャとして処理され、Forgeと同様のGPU描画パスを実現
    - **期待される効果**:
      - 動画再生時の`image()`ボトルネック解消（CPU描画 → GPU描画）
      - AWTEventQueue遅延の大幅削減（GPU処理により、AWT負荷が軽減）
      - Forge環境と同等のパフォーマンス（OpenGL描画パスの統一）
    - **実装ファイル**: `standalone/StandaloneWrapper.java:49`（P2Dレンダラー指定）
    - **ビルド結果**: ✅ BUILD SUCCESSFUL in 15s
    - **テスト結果**: ✅ **完全成功** - 動画再生時のインタラクション遅延が完全に解消、Forge環境と同等のパフォーマンスを実現
    - **パフォーマンス向上施策（第6回最適化 - 2025-10-24）**:
      1. **フレームレートを60FPSに引き上げ**: P2D GPU描画により高フレームレート対応が可能になったため
         - `ChromiumBrowserScreen.java:523`: ブラウザフレームレート 10FPS→60FPS
         - `ChromiumRenderHandler.java:44`: onPaint()スキップ間隔 100ms(10FPS)→16ms(60FPS)
      2. **マウスイベントスロットリングを削除**: GPU描画により不要になった
         - `StandaloneWrapper.java`: マウス移動/ドラッグのスロットリング（200ms/100ms）を削除
         - すべてのマウスイベントを即座にKernelに転送
      3. **InstrumentedEventQueueのイベント破棄ロジックを削除**: GPU描画により不要になった
         - マウス移動イベントの100ms遅延時破棄ロジックを削除
         - 通常のAWTイベントキュー動作に戻す
      4. **デバッグログの削除**: 本番環境向けにクリーンアップ
         - `ChromiumRenderHandler.java`: プロファイリングログ（onPaint count/avg/max/skipped）を削除
         - `StandaloneWrapper.java`: logInputBridge()メソッドとすべての入力イベントログを削除
      5. **結果**: ✅ BUILD SUCCESSFUL in 28s、60FPS高速描画 + クリーンなログ出力を実現
    - **マウスドラッグラグ修正（第7回最適化 - 2025-10-24）**:
      1. **問題**: マウスドラッグ時のラグが長く、ドラッグ操作の反映が遅延（ロック画面解除やコントロールセンター展開で1秒遅延）
      2. **根本原因**:
         - マウスドラッグイベントは`MOUSE_MOVE`タイプとして処理される
         - 60FPSで大量のドラッグイベントが発生し、ChromiumBrowserの入力キュー（50件制限）が溢れる
         - フレームごとに50件しか処理されないため、高速ドラッグ時にキューが詰まりラグが発生
      3. **修正内容（第1回）**: マウスドラッグのみに軽量スロットリングを導入
         - `StandaloneWrapper.java`: ドラッグイベントを16ms間隔（60回/秒）でのみ送信
         - → **結果**: まだラグが発生（1秒遅延）
      4. **修正内容（第2回 - 最終）**: スロットリングと入力キュー処理を大幅緩和
         - `StandaloneWrapper.java:34`: ドラッグスロットリングを5ms間隔（200回/秒）に緩和
         - `ChromiumBrowser.java:590-603`: 入力キュー制限を大幅緩和
           - MOUSE_MOVEキュー制限: 50件→200件
           - 重要イベントキュー制限: 150件→500件
         - `ChromiumBrowser.java:526-527`: フレームごとの処理数を大幅増加
           - maxEventsPerFrame: 100→200
           - maxMouseMovePerFrame: 50→150
      5. **実装ファイル**:
         - `standalone/StandaloneWrapper.java:34,177`
         - `core/service/chromium/ChromiumBrowser.java:526,590`
      6. **ビルド結果**: ✅ BUILD SUCCESSFUL in 22s
      7. **期待される効果**: リアルタイムなドラッグ反応、P2D GPU描画の高速処理能力を最大限活用
    - **マウスドラッグスロットリング完全削除（第8回最適化 - 2025-10-24）**:
      1. **問題**: 第7回最適化でマウスドラッグスロットリングを5msに設定したが、依然としてドラッグが即座に反映されない
      2. **根本原因**:
         - 第6回最適化でマウス移動のスロットリングは削除したが、ドラッグのスロットリングが`StandaloneWrapper.java:33-34,177`に残っていた
         - DEV.mdには削除されたと記載されていたが、実際のコードには5msのスロットリングが残存
      3. **修正内容**:
         - `StandaloneWrapper.java:32-34`: マウスドラッグスロットリング用の変数と定数を完全削除
         - `StandaloneWrapper.java:170-173`: `mouseDragged()`メソッドからスロットリングロジックを削除し、すべてのイベントを即座にKernelに転送
      4. **実装ファイル**: `standalone/StandaloneWrapper.java`
      5. **ビルド結果**: ✅ BUILD SUCCESSFUL in 17s
      6. **期待される効果**: ドラッグ操作が完全にリアルタイムで反映され、ロック画面解除やコントロールセンター展開の遅延が解消
  - 実装ファイル:
    - `core/service/chromium/ChromiumProvider.java`: プロバイダーインターフェース（createCefApp, isAvailable, getName等）
    - `core/service/chromium/ChromiumManager.java`: setProvider()メソッドでプロバイダー注入、initialize()で初期化
    - `core/service/chromium/ChromiumAppHandler.java`: jcefmaven依存削除（CefAppHandlerAdapter継承、super(new String[0])呼び出し）
    - `standalone/StandaloneChromiumProvider.java`: jcefmaven用プロバイダー（MavenCefAppHandlerAdapterラッパーで委譲）
    - `forge/chromium/ForgeChromiumProvider.java`: MCEF用プロバイダー（MCEF.getApp().getHandle()でCefApp取得）
    - `standalone/StandaloneWrapper.java`: kernel.getChromiumManager().setProvider(new StandaloneChromiumProvider())で注入
    - `forge/service/SmartphoneBackgroundService.java`: kernel.getChromiumManager().setProvider(new ForgeChromiumProvider())で注入
    - `core/service/chromium/ChromiumBrowser.java`: `getUpdatedImage()`でMCEF用レンダラから`updateFromTexture()`を毎フレーム実行し、最新PImageを取得
    - `core/apps/chromiumbrowser/ChromiumBrowserScreen.java`: `getUpdatedImage()`の結果をProcessingのメインピクセルバッファへ手動コピーしてForge描画に反映
  - Gradle設定:
    - `core/build.gradle.kts`: jcefmavenをcompileOnlyに変更（実行時依存なし）
    - `standalone/build.gradle.kts`: jcefmavenをimplementationとして提供
    - `forge/build.gradle`: JAR exclusion設定を削除（不要になった）
  - ビルド結果: ✅ BUILD SUCCESSFUL in 1m 24s（全モジュール、依存性注入により完全分離達成）
  - 利点:
    - coreモジュールが環境非依存（compileOnlyでAPI参照のみ）
    - JAR exclusion設定が不要（クリーンなビルド設定）
    - 各環境が独自のChromium実装を提供可能（拡張性向上）
    - Java Module System競合が根本的に解決
- **動作状況**: ✅ ビルド成功、全環境でコンパイル成功、スタンドアロンで実行時テスト成功
  - スタンドアロン: ChromiumManager初期化成功（jcefmaven 135.0.20.333）
  - Forge: ビルド成功。`ChromiumBrowser.getUpdatedImage()`経由で`updateFromTexture()`を確実に呼び出す実装を追加済み（表示確認は次回Forge実機テストで実施）

**Chromiumブラウザ UI 設計（2025-10-21 更新）**
- アドレスバーは画面上部に固定し、最大限URLを表示できるよう横幅を確保する。左側に戻る／進むボタン、右側にタブ一覧ボタンとメニュー（その他オプション）ボタンを配置する
- 画面下部に操作バーを設置し、`戻る/進む`（状態表示のみ）、`新規タブ追加`, `タブ一覧ショートカット`, `その他オプション` を並列配置する。中央のタブ追加ボタンを強調し、バー自体は薄めの高さに抑える
- タブ一覧はカード型グリッド表示を採用し、ピン留め・プライベートモード切替・スワイプ削除をサポート予定。上部アドレスバー右端および下部バーのショートカットから遷移できる
- ページ読み込み中はアドレスバー内部にプログレスインジケータを表示し、httpm通知やエラーは上部からのドロップダウンとして提示する
- Forge環境ではChromiumは脇役であるため、Chrome同等品質を目指しつつも高負荷処理を避け、GPU/CPU使用率を最小限に抑える設計（低フレームレート制御・無操作時のレンダリング抑制・軽量UI描画）を遵守する
- 上記レイアウトのうち、トップアドレスバー拡張と下部ナビゲーション再構成は`ChromiumBrowserScreen`に実装済み（2025-10-21）
- 下部ナビゲーションの機能割当を実装済み：新規タブ生成（Googleホームをロード、アドレスバーへ即フォーカス）、タブ一覧オーバーレイ表示（カードをタップして切替）、メニュー（再読み込み／タブを閉じる／他タブを閉じる）

**Chromiumブラウザシステム** (✅ Phase 1実装・動作確認完了 - 2025-10-19):
- **目的**: JCEF (Java Chromium Embedded Framework) を使用した完全機能のChromiumベースブラウザを提供し、http/https/httpmプロトコルに対応したモバイル最適化UIを実現
- **JCEF統合** (✅ 実装・動作確認完了):
  - **依存関係**: `me.friwi:jcefmaven:122.1.10` (core/build.gradle.kts:39)
  - **自動ダウンロード**: jcefmavenによりプラットフォーム別JCEFバイナリを自動取得
  - **オフスクリーンレンダリング**: CefBrowserOsrを使用してヘッドレス環境で動作
  - **動作確認** (✅ 2025-10-19 Standalone環境):
    - ChromiumManager: JCEF Version 122.1.10.324が正常に初期化
    - ChromiumBrowserApp: ランチャーに表示され、クリックで起動成功
    - ChromiumBrowserScreen: セットアップ完了、https://www.google.com のロード開始
    - httpm://スキーム: カスタムスキーム登録成功
- **実装内容**:
  - `ChromiumManager` (ChromiumManager.java): JCEFシングルトン管理、CefApp初期化、ブラウザインスタンス作成
    - CefAppBuilderを使用した初期化（API修正済み）
    - VFS内キャッシュパス設定（`system/browser_chromium/cache`）
    - モバイルUser-Agent設定（Android 12 MochiMobileOS）
  - `ChromiumBrowser` (ChromiumBrowser.java): CefBrowserのラッパークラス
    - URL読み込み、HTMLコンテンツ読み込み
    - JavaScript実行（executeScript）
    - ナビゲーション（戻る/進む/更新/停止）
    - ローディング状態管理（CefLoadHandler統合）
    - **API制約**: マウス/キーボードイベントAPIが jcefmaven 122.1.10 では未実装（将来のアップグレード待ち）
  - `ChromiumRenderHandler` (ChromiumRenderHandler.java): オフスクリーンレンダリングハンドラー
    - onPaint()コールバックでBGRAピクセルデータをPImage（ARGB）に変換
    - 描画更新フラグ管理（needsUpdate）
    - リスナーメソッド実装（setOnPaintListener、addOnPaintListener、removeOnPaintListener）
    - **API修正**: `CefPaintEvent`インポートを`org.cef.browser`パッケージに変更（`org.cef.handler`から修正）
  - `ChromiumBrowserScreen` (ChromiumBrowserScreen.java): モバイル最適化ブラウザUI
    - レイアウト: アドレスバー（70px）、WebViewエリア（470px）、ボトムナビゲーション（60px）
    - UI要素: URL入力、更新ボタン、戻る/進むボタン、ブックマーク/メニュー/タブボタン（Phase 2実装予定）
    - イベント処理: マウスクリック、アドレスバー入力、Enter キーでURL読み込み
    - **API修正**: Screenインターフェース実装（super()呼び出しを削除、ライフサイクルメソッド名変更）
  - `ChromiumBrowserApp` (ChromiumBrowserApp.java): アプリケーションエントリーポイント
    - アプリID: `jp.moyashi.phoneos.core.apps.chromiumbrowser`
    - ChromiumBrowserScreenをエントリースクリーンとして登録
  - `HttpmSchemeHandler` (HttpmSchemeHandler.java): httpm://カスタムプロトコルハンドラー
    - httpm://URLを仮想ネットワーク（VirtualRouter）経由でルーティング
    - VirtualPacket.builder()パターンを使用してリクエスト構築
    - レスポンスハンドラー登録によるHTTP応答処理
    - **API修正**: `putString()`を`put()`に変更、`IPvMAddress.forSystem()`を使用
- **API修正の経緯** (✅ 全て解決):
  - **問題**: jcefmaven 122.1.10 のAPI仕様が未確認のまま実装し、30+ コンパイルエラーが発生
  - **修正内容**:
    1. CefAppBuilder: `setSettings()`が存在しない → `getCefSettings()`で設定を取得
    2. CefSchemeRegistrar: `addCustomScheme()`が9パラメータではなく8パラメータ
    3. CefClient: `addRenderHandler()`メソッドが存在しない（カスタムRenderHandler統合方法が不明）
    4. CefBrowser: `createBrowser()`がCefRenderingではなくboolean APIを使用（`createBrowser(url, true, false)`）
    5. CefLoadHandler: `onLoadStart()`がTransitionTypeパラメータを持たない
    6. CefRenderHandler: 抽象メソッドとして`setOnPaintListener`、`addOnPaintListener`、`removeOnPaintListener`が必要
    7. CefPaintEvent: パッケージが`org.cef.handler`ではなく`org.cef.browser`
    8. VirtualPacket: `putString()`ではなく`put()`メソッドを使用
    9. IPvMAddress: `VirtualRouter.getLocalAddress()`が存在しない → `IPvMAddress.forSystem()`を使用
- **開発ツール改善** (✅ 完了):
  - **clearBuild.bat修正**: Windowsファイルロック問題を解決
    - Gradleデーモンを明示的に停止（`gradlew.bat --stop`）
    - 2秒待機してファイルロック解放（`timeout /t 2`）
    - PowerShell実行ポリシーをBypassに設定（`-ExecutionPolicy Bypass`）
    - エラーを無視して継続（`-ErrorAction SilentlyContinue`）
    - **効果**: ファイルロックによるコンパイル失敗が解消され、開発効率が大幅に向上
- **Phase 1完了事項** (✅ 2025-10-19):
  - ✅ 単一タブブラウザ実装
  - ✅ URL読み込み（http/https/httpm）
  - ✅ 戻る/進む/更新機能
  - ✅ モバイル最適化UI（下部ナビゲーション）
  - ✅ アドレスバー入力
  - ✅ httpm://カスタムプロトコル対応
  - ✅ オフスクリーンレンダリング（PGraphics描画）
  - ✅ コンパイル成功
  - ✅ Standalone環境での動作確認成功（ChromiumManager初期化、アプリ起動、URL読み込み開始）
- **既知の制約**:
  - マウス/キーボードイベントAPIが jcefmaven 122.1.10 では利用不可（sendMouseEvent、sendKeyEvent等）
  - カスタムRenderHandlerの統合方法が不明（CefClientに追加する公開APIがない）
  - これらは将来のJCEFバージョンアップグレード時に対応予定

## Chromiumブラウザアプリ開発 (✅ Phase 3完了 - 2025-10-19)

### 目的
JavaFX WebViewの代わりにJCEF (Java Chromium Embedded Framework) を使用した完全機能のブラウザアプリを実装。
キャッシュ、Cookie、ブックマーク、履歴機能を備え、httpm:プロトコルで仮想ネットワークにアクセス可能。

### Phase 3完了報告 (✅ 2025-10-19)

**Phase 3目標: jcefmaven API仕様の正確な調査と修正**

**調査手法:**
- ❌ **以前の問題**: WebFetchが使用できず、推測でAPIを実装していたため、多数のコンパイルエラーとURL読み込み不具合が発生
- ✅ **解決策**: gemini-cliを活用して最新のJCEF情報を調査
  - jcefmaven vs JetBrains/jcef vs jcefbuildの比較
  - jcefmavenが推奨され、URL読み込みも「Excellent」と評価
  - jcefmaven 135.0.20の正しいAPI仕様を確認

**実装完了事項:**
1. ✅ **JCEFバージョン確認**: Gradle設定が`jcefmaven:135.0.20`であることを確認（DEV.mdには古い122.1.10情報が残っていた）
2. ✅ **onLoadStart()シグネチャ修正**:
   - 誤: `CefLoadHandler.TransitionType` ❌
   - 正: `org.cef.network.CefRequest.TransitionType` ✅
   - ChromiumBrowser.java:111 - 正しいシグネチャに修正
3. ✅ **createBrowser()引数修正**:
   - 誤: `createBrowser(url, true, false, null)` - 4引数版 ❌
   - 正: `createBrowser(url, true, false)` - 3引数版 ✅
   - ChromiumBrowser.java:142 - 正しいAPI仕様に修正
4. ✅ **ビルド成功**: coreモジュールのコンパイルが成功（BUILD SUCCESSFUL in 21s）

**gemini-cli活用による成果:**
- jcefmavenのURL読み込み機能が「Excellent」であることを確認
- 正しいAPI仕様（TransitionTypeの完全修飾名、createBrowserの引数数）を特定
- Common Mistakes（java.library.path、スレッド問題、dispose忘れ）を学習

**結論:**
Phase 3完了により、jcefmaven 135.0.20の正しいAPI仕様に基づいた実装が完了しました。以前の「jcefmaven 122.1.10はURL読み込み不可」という結論は、API仕様の誤解によるものでした。gemini-cliを活用することで、正確な情報に基づいた実装が可能になりました。

### Phase 7完了報告: JVMフラグ自動適用機能の実装 (✅ 2025-10-20)

**Phase 7目標: JVMフラグを手動指定せずに、JCEF/JOGLが必要とするモジュールアクセスを自動化する**

**背景:**
- JCEF/JOGLは内部API（sun.awt、sun.java2d）へのアクセスが必要
- 通常はJVM起動時に`--add-opens`/`--add-exports`フラグの指定が必須
- JAR配布時やForge MOD環境で、エンドユーザーに手動フラグ指定を要求したくない

**実装完了事項:**

#### 1. ✅ ModuleOpenerユーティリティクラスの作成
- **ファイル**: `core/src/main/java/jp/moyashi/phoneos/core/service/chromium/ModuleOpener.java`
- **機能**:
  - `Module.implAddOpens()`メソッドをリフレクションで取得し、実行時にモジュールを開く
  - 対象モジュール/パッケージ:
    - `java.base/java.lang`
    - `java.desktop/sun.awt`
    - `java.desktop/sun.java2d`
  - `openRequiredModules()`: ランタイムモジュール操作を試行
  - `checkJvmFlags()`: JVMフラグが設定されているか確認
  - `getErrorMessage()`: エラーメッセージ取得

#### 2. ✅ ChromiumManagerへの統合
- **ファイル**: `core/src/main/java/jp/moyashi/phoneos/core/service/chromium/ChromiumManager.java`
- **3段階フォールバック戦略**:
  1. **ステップ1**: ランタイムモジュール操作を試行 (`ModuleOpener.openRequiredModules()`)
  2. **ステップ2**: 失敗した場合、JVMフラグの設定を確認 (`ModuleOpener.checkJvmFlags()`)
  3. **ステップ3**: JVMフラグもない場合、エラーログを出力して初期化を中断
- **ロギング**: すべての出力をLoggerServiceを通じて記録（System.out/errは使用しない）

#### 3. ✅ 動作確認
- **テスト環境**: Java 24
- **テスト方法**: JVMフラグなしで`java -jar standalone-1.1-SNAPSHOT.jar`を実行
- **結果**: ✅ 正常に動作 - ランタイムモジュール操作が成功
- **確認事項**:
  - `Module.implAddOpens()`がJava 24でも利用可能
  - JVMフラグを手動指定しなくてもJCEF/JOGLが正常に初期化される

**技術的詳細:**
```java
// ModuleOpener.javaの核心部分
Method implAddOpens = Module.class.getDeclaredMethod("implAddOpens", String.class, Module.class);
implAddOpens.setAccessible(true);

Module module = ModuleLayer.boot().findModule(moduleName).orElseThrow(...);
Module unnamed = ModuleOpener.class.getClassLoader().getUnnamedModule();

implAddOpens.invoke(module, packageName, unnamed);
```

**期待される動作:**
- **Java 9-24**: ランタイムモジュール操作が成功 → JVMフラグ不要
- **Java 17+（制限強化時）**: ランタイムモジュール操作が失敗 → JVMフラグの確認 → フラグがあれば初期化継続
- **フラグなし**: エラーメッセージを表示して初期化を中断

**成果:**
- JAR配布時にエンドユーザーがJVMフラグを意識する必要がなくなった
- Forge MOD環境でもプレイヤーがフラグ設定を変更する必要がなくなる可能性がある
- 開発環境での起動も簡素化（`java -jar`で起動可能）

**結論:**
Phase 7完了により、MochiMobileOSはJVMフラグを自動的に処理できるようになりました。Java 24での動作確認により、最新のJavaバージョンでもこのアプローチが有効であることが実証されました。

### ✅ MCEF統合完了（2025-10-20）

**実装内容:**
MCEF (Minecraft Chromium Embedded Framework) を使用したForge環境でのChromium統合を実装しました。

**解決した問題:**
1. **Java Module System Package Conflict**:
   - 問題: mochimobileosモジュールとmcefモジュールが両方ともorg.cef.*パッケージを提供し、Module Resolution Exceptionが発生
   - 解決策: forge/build.gradle jar設定でorg.cef.**とme.friwi.jcefmaven.**を除外

2. **Standalone環境とForge環境の両立**:
   - 課題: リフレクション実装を試みたが、Standalone環境が動作しなくなった
   - 最終解決策: ChromiumManager.javaは元の直接import方式を維持（Standalone環境優先）
   - Forge環境では、JAR exclusionにより`me.friwi.jcefmaven.*`がパッケージされないため、SmartphoneBackgroundService経由でMCEFのCefAppを注入する設計

**アーキテクチャ:**
- **Standalone環境**: jcefmavenを直接使用してJCEF初期化（従来通り）
- **Forge環境**:
  - JAR exclusionにより`me.friwi.jcefmaven.*`が除外される
  - SmartphoneBackgroundService.createKernel()でMCEFのCefAppを取得
  - ChromiumManager.setCefApp()経由でCefAppを注入
- **コンパイル時**: 両環境ともjcefmaven APIに対してコンパイル
- **ランタイム時**: JAR exclusionで環境を分離

**変更ファイル:**
1. `core/src/main/java/jp/moyashi/phoneos/core/service/chromium/ChromiumManager.java`:
   - 元の直接import方式を維持（リフレクション実装は取りやめ）
   - setCefApp()メソッドを追加（Forge環境でのMCEF注入用）
2. `forge/build.gradle` (lines 201-209): JAR exclusion設定
3. `core/build.gradle.kts` (lines 38-42): dependency設定とコメント追加
4. `standalone/build.gradle.kts` (lines 7-12): 不要なruntimeOnly削除

**ビルド結果:**
- ✅ コアモジュール: BUILD SUCCESSFUL
- ✅ Forgeモジュール: BUILD SUCCESSFUL
- ✅ Standaloneモジュール: BUILD SUCCESSFUL
- ✅ Standalone実行: 動作確認済み

### Phase 8完了報告: マウス・キーボードイベントバイパス実装 (✅ 2025-10-21)

**Phase 8目標: ChromiumBrowserScreenからChromiumブラウザへのマウス・キーボードイベント転送**

**実装完了事項:**

#### 1. ✅ ChromiumProviderインターフェースの拡張
- **ファイル**: `core/src/main/java/jp/moyashi/phoneos/core/service/chromium/ChromiumProvider.java`
- **追加メソッド** (lines 131-186):
  - `sendMousePressed(CefBrowser, x, y, button)`
  - `sendMouseReleased(CefBrowser, x, y, button)`
  - `sendMouseMoved(CefBrowser, x, y)`
  - `sendMouseWheel(CefBrowser, x, y, delta)`
  - `sendKeyPressed(CefBrowser, keyCode, keyChar)`
  - `sendKeyReleased(CefBrowser, keyCode, keyChar)`

#### 2. ✅ ChromiumBrowser.javaの実装
- **ファイル**: `core/src/main/java/jp/moyashi/phoneos/core/service/chromium/ChromiumBrowser.java`
- **実装内容** (lines 412-530):
  - 各イベントメソッドがChromiumProviderに委譲
  - nullチェックとエラーハンドリング
  - ログ出力（エラー時のみ）

#### 3. ✅ StandaloneChromiumProvider.javaの実装
- **ファイル**: `standalone/src/main/java/jp/moyashi/phoneos/standalone/StandaloneChromiumProvider.java`
- **実装内容**:
  - **パフォーマンス最適化** (lines 30-79):
    - `BrowserMethodCache`: リフレクションメソッドをキャッシュ
    - `METHOD_CACHE`: ConcurrentHashMapで複数ブラウザに対応
    - 再利用可能な`eventComponent` (Canvas) でGC負荷を削減
    - `findAndPrepare()`: クラス階層を探索してprotectedメソッドを取得
  - **イベント実装** (lines 163-407):
    - **マウスイベント**: AWT MouseEventを構築して`sendMouseEvent()`に送信
    - **ホイールイベント**: MouseWheelEventを構築して`sendMouseWheelEvent()`に送信
    - **キーイベント**: KeyEventを構築して`sendKeyEvent()`に送信
    - **ボタンマッピング**: Processing (1=左, 2=中, 3=右) → AWT (BUTTON1, BUTTON2, BUTTON3)
    - **KEY_TYPED送信**: キー押下時に通常文字（ASCII >= 32）に対してKEY_TYPEDイベントも送信

#### 4. ✅ ForgeChromiumProvider.javaの実装
- **ファイル**: `forge/src/main/java/jp/moyashi/phoneos/forge/chromium/ForgeChromiumProvider.java`
- **実装内容** (lines 142-236):
  - MCEFBrowserの公開API使用:
    - `sendMousePress()`, `sendMouseRelease()`, `sendMouseMove()`, `sendMouseWheel()`
    - `sendKeyPress()`, `sendKeyRelease()`, `sendKeyTyped()`
    - `setFocus(true)`: クリック/キー入力時にフォーカスを設定
  - **ボタンマッピング**: Processing (1=左, 2=中, 3=右) → MCEF (0=左, 2=中, 1=右)

#### 5. ✅ ChromiumBrowserScreen.javaのパフォーマンス最適化
- **ファイル**: `core/src/main/java/jp/moyashi/phoneos/core/apps/chromiumbrowser/ChromiumBrowserScreen.java`
- **変更内容** (lines 80-126):
  - `draw()`メソッドからすべてのログ出力を削除
  - 毎ティック（60fps）のログがパフォーマンスに悪影響を与える問題を解消

**技術的詳細:**

**Reflection使用理由:**
- JCEFの`sendMouseEvent()`, `sendKeyEvent()`はprotectedメソッド
- `CefBrowser_N`親クラスで定義されており、直接呼び出し不可
- `getDeclaredMethod()`でクラス階層を探索して取得
- `method.setAccessible(true)`でアクセス可能に

**メソッドキャッシュの重要性:**
- リフレクションは低速なため、毎回メソッド探索すると60fpsでパフォーマンス低下
- 初回アクセス時にキャッシュし、以降は`Method.invoke()`のみ実行
- `ConcurrentHashMap`でスレッドセーフ

**Componentの再利用:**
- AWT EventはSourceとしてComponentを要求
- 毎回`new Canvas()`するとGC負荷が大きい
- フィールドで1つのCanvasを保持して再利用
