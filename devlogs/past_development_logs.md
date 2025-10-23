# 過去の開発ログ

## フォントシステム (✅ 実装完了、テスト済み - 2025-10-14)
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

---

## WebViewシステム (✅ モバイル最適化・スクロール機能完了、✅ Forge環境対応 - 2025-10-19)
- **目的**: JavaFX WebViewを使用したHTML/CSS/JavaScript実行環境を提供し、BrowserAppおよびHTML系アプリケーションでWebコンテンツを表示
- **Forge環境での動作確認** (✅ 2025-10-19):
  - JavaFXはオフスクリーンレンダリングのため、Forge MOD環境でも正常に動作する
  - WebViewManagerはForge環境で正常に初期化される（検証済み）
  - **修正した問題** (Kernel.java:740): `parentApplet.g = this.graphics`を設定
    - 問題: Forge環境でBrowserScreenの初期化時に`pg is null`エラーが発生
    - 原因: `initializeForMinecraft()`で作成したヘッドレスPAppletの`g`フィールドが未初期化
    - 解決: `parentApplet.g`に`graphics`を設定することで、ScreenManagerが`setup(currentPApplet.g)`を正しく呼べるようになった
  - Kernel.setup()でWebViewManagerの初期化をtry-catchで囲み、万が一のエラーに備える (Kernel.java:852-869)
- **実装内容**:
  - `WebViewWrapper`: WebViewのラッパークラス（`core/src/main/java/jp/moyashi/phoneos/core/service/webview/`）
    - PGraphicsへのスナップショット描画（非同期、キャッシング対応）
    - マウス/キーボード/スクロールイベントのJavaScript注入
    - DOM操作、URL読み込み、コンテンツ読み込み
  - `WebViewManager`: WebViewインスタンスの管理サービス（Kernelサービス）
  - `JSBridge`: JavaScript ↔ Java連携ブリッジ
- **モバイル最適化** (✅ 2025-10-19):
  - **モバイルUser-Agent**: Android 12 MochiMobileOSとして認識（WebViewWrapper.java:89-91）
    - User-Agent文字列: `Mozilla/5.0 (Linux; Android 12; MochiMobileOS) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36`
  - **Viewport自動注入** (WebViewWrapper.java:786-830):
    - ページ読み込み完了時に`injectMobileViewport()`を自動実行
    - viewportメタタグが存在しない場合は動的に追加: `width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes`
    - モバイル最適化CSS注入:
      - `-webkit-text-size-adjust: 100%`: テキストサイズの自動調整
      - `-webkit-tap-highlight-color`: タップハイライト色
      - `font-size: 16px !important` for input/textarea/select: iOSの自動ズーム防止
  - **パフォーマンス最適化** (WebViewWrapper.java:94-95):
    - JavaScriptを明示的に有効化（`setJavaScriptEnabled(true)`）
    - コンテキストメニューを無効化（`setContextMenuEnabled(false)`）してパフォーマンス向上
  - **フレーム更新の改善** (BrowserScreen.java:209-211):
    - 毎フレーム`requestUpdate()`を呼び出してHTML側の動的な変更を即座に反映
    - JavaScriptアニメーション、フォーム入力、ボタンクリックなどが正常に表示される
- **スクロール機能** (✅ 2025-10-19):
  - **WebViewWrapper.simulateScroll()** (WebViewWrapper.java:508-562):
    - JavaScriptで`window.scrollBy()`を実行してページ全体をスクロール
    - スクロール可能な要素（`overflow: auto/scroll`）を自動検出して個別にスクロール
    - WheelEventをディスパッチして互換性を確保
    - **JavaScript生成の修正**: String.format()のロケール問題を回避するため、文字列連結に変更（浮動小数点数のフォーマット問題を解消）
  - **BrowserScreen.mouseWheel()** (BrowserScreen.java:654-679):
    - マウスホイールイベントをWebViewエリアで検出
    - スクロール量を調整（delta * 50）してWebViewに転送
    - **スクロール方向の修正**: AWTのwheelRotationとJavaScript scrollBy()の符号を統一（正=下スクロール、負=上スクロール）
  - **Screenインターフェース** (Screen.java:238-267):
    - `mouseWheel(PGraphics, int, int, float)`メソッドを追加
    - PApplet版との互換性ブリッジも実装
  - **ScreenManager.mouseWheel()** (ScreenManager.java:506-523):
    - 現在のスクリーンにマウスホイールイベントを転送
    - アニメーション中はイベントをブロック
  - **Kernel.mouseWheel()** (Kernel.java:464-489):
    - マウスホイールイベントの独立API
    - スリープ中はイベントを拒否
  - **StandaloneWrapper.mouseWheel()** (StandaloneWrapper.java:61-72, 167-177):
    - ProcessingのMouseEventをKernelに転送
    - **AWT MouseWheelListenerの追加**: Processing mouseWheel()が機能しないため、AWTコンポーネントに直接リスナーを登録
    - 両方のイベントハンドラーを実装してクロスプラットフォーム対応
  - **ProcessingScreen.mouseScrolled()** (ProcessingScreen.java:595-625):
    - Minecraft GUIのスクロールイベントをKernelに転送
    - 座標変換とスケーリングに対応
    - **スクロール方向の修正**: delta値の符号を統一（負の符号を削除）
- **効果**:
  - ウェブサイトがスマートフォンOSとして認識され、モバイル最適化されたページが表示される
  - レスポンシブデザイン対応のウェブサイトで自動的にモバイルレイアウトに切り替わる
  - WebView描画パフォーマンスが向上
  - マウスホイールによるスムーズなスクロールが可能
  - ページ全体のスクロールとスクロール可能な要素の両方に対応

---

# 完了済みTODOタスク

### ハードウェアバイパスAPIの実装 (✅ 完了)

Kernelに以下のハードウェアバイパスAPIを実装完了:

1. **モバイルデータ通信ソケット**:
   - プロパティ: 通信強度、サービス名
   - standalone: 圏外を返す
   - forge-mod: 仮想インターネット通信をバイパス

2. **Bluetooth通信ソケット**:
   - プロパティ: 周囲のデバイスリスト、接続済みデバイス
   - standalone: NOTFOUND
   - forge-mod: 半径10m以内のBluetoothデバイス検出

3. **位置情報ソケット**:
   - プロパティ: x, y, z座標、電波精度
   - standalone: 0,0,0固定
   - forge-mod: プレイヤー位置

4. **バッテリー情報**:
   - プロパティ: バッテリー残量、バッテリー寿命
   - standalone: 100%固定
   - forge-mod: アイテムNBTから取得

5. **カメラ**:
   - プロパティ: オンオフ
   - standalone: null
   - forge-mod: ❌ 保留（フレームバッファキャプチャが複雑なため）

6. **マイク**:
   - プロパティ: オンオフ、ストリーム形式音声
   - standalone: null
   - forge-mod: ✅ SVCソフト依存（SVC導入時のみ有効）

7. **スピーカー**:
   - プロパティ: 音量4段階（OFF/LOW/MEDIUM/HIGH）
   - standalone: アプリケーション音声再生
   - forge-mod: ✅ SVCソフト依存（SVC導入時のみ有効）

8. **IC通信**:
   - プロパティ: オンオフ
   - standalone: null
   - forge-mod: ✅ 右クリックでブロック座標/エンティティUUID取得（IC有効時はGUIを開かない）

9. **SIM情報**:
   - プロパティ: 名前、UUID
   - standalone: Dev/0000-0000-0000-0000
   - forge-mod: 所持者の表示名/UUID

**実装内容**:
- 各ハードウェアAPIのインターフェース定義（`core/src/main/java/jp/moyashi/phoneos/core/service/hardware/`）
- デフォルト実装（standalone用、`Default*`クラス）
- Kernelへの統合（フィールド、getter、setter）
- forge-modでの実装差し替えが可能な設計
- デバッグ用テストアプリ（`HardwareTestApp`）

**Forge実装** (✅ 完了):
- `ForgeSIMInfo`: プレイヤーの表示名とUUIDを提供 ✅ テスト済み
- `ForgeBatteryInfo`: アイテムNBTからバッテリー情報を管理 ✅ テスト済み
- `ForgeLocationSocket`: プレイヤー座標とGPS精度を提供 ✅ テスト済み
- `ForgeMobileDataSocket`: Y座標ベースの通信強度、ディメンション別ネットワーク名 ✅ テスト済み
- `ForgeBluetoothSocket`: 半径10m以内のプレイヤー/エンティティを検出 ✅ テスト済み
- `ForgeICSocket`: 右クリックでブロック/エンティティをスキャン ✅ 実装完了
  - `SmartphoneItem.useOn()`: ブロック右クリック時のICスキャン
  - `SmartphoneItem.interactLivingEntity()`: エンティティ右クリック時のICスキャン
  - IC有効時はGUIを開かず、無効時は通常動作
- `ForgeCameraSocket`: 基本構造のみ（保留: フレームバッファキャプチャが複雑なため）
- `ForgeMicrophoneSocket`: SVCソフト依存実装 ✅ 完了
  - `SVCDetector`: Simple Voice Chat MODの自動検知
  - SVC導入時のみ`isAvailable()`が`true`を返す
  - **注意**: SVCは開発環境（runClient）では動作しない。本番環境でのみテスト可能
- `ForgeSpeakerSocket`: SVCソフト依存実装 ✅ 完了
  - SVC導入時のみ`isAvailable()`が`true`を返す
  - 音量レベル管理（OFF/LOW/MEDIUM/HIGH）
  - **注意**: SVCは開発環境（runClient）では動作しない。本番環境でのみテスト可能
- `SmartphoneBackgroundService`で初期化時とフレームごとに自動更新 ✅ テスト済み

**デバッグ方法**:
- `HARDWARE_DEBUG_GUIDE.md`に詳細なデバッグ方法を記載
- Hardware Test App（🔧アイコン）で全APIの動作確認が可能
- standalone環境とforge環境での動作を比較可能
- スクロール機能：マウスドラッグ、矢印キー、Page Up/Down、Home/End

---

### Minecraft Forge連携システム (✅ 完了)

以下のタスクが完了しました：

1. **Gradle構成の修正** (✅ 完了):
   - `forge/build.gradle`でProcessing coreを`implementation`に変更
   - Java 17を使用するための`run_with_java17.bat`スクリプトを作成
   - runClientが正常にビルドできることを確認

2. **PGraphics→Minecraft GUI変換システムの実装** (✅ 完了):
   - `SmartphoneBackgroundService.updateTextureFromKernel()`で`kernel.update()`と`kernel.render()`を呼び出し
   - PGraphicsのARGB形式からMinecraftのABGR形式への正確なピクセル変換を実装
   - `ProcessingScreen`でブロック単位の効率的な描画を実装

3. **入力バイパスシステムの実装** (✅ 完了):
   - `ProcessingScreen`でマウスイベント（clicked, released）をKernelに転送
   - キーボードイベント（keyPressed）をKernelに転送
   - Minecraft座標からMochiMobileOS座標への変換を実装

---

### 次のステップ
- **仮想インターネット通信システムの実装** (✅ 100%完了):
  1. **coreモジュール**:
     - `IPvMAddress`: アドレス表現クラス (✅)
     - `VirtualPacket`: パケットデータクラス (✅)
     - `VirtualRouter`: ルーティングサービス（Kernelサービスとして登録） (✅)
     - `MessageStorage`: メッセージ永続化サービス (✅)
  2. **forgeモジュール**:
     - Minecraftパケット通信（プレイヤー間通信） (✅)
     - 外部Mod登録API (`VirtualNetworkRegistry`) (✅)
     - システム通信ハンドラー（`SystemPacketHandler`） (✅)
     - メッセージ処理の完全実装 (✅)
  3. **統合**:
     - `NetworkHandler`: ForgeのSimpleChannelを使ったパケット送受信 (✅)
     - `MochiMobileOSMod`: ネットワーク初期化 (✅)
     - `SmartphoneBackgroundService`: VirtualRouter初期化 (✅)
  4. **アプリケーション**:
     - `NetworkApp`: メッセージ送受信とネットワークテスト機能 (✅)
     - メッセージ一覧表示、テスト送信、システムパケット送信 (✅)
  5. **ドキュメント**:
     - `VIRTUAL_NETWORK_API.md`: 外部Mod連携APIドキュメント (✅)
     - 完全なサンプルコード（電子マネーサービス）を含む (✅)

- **仮想インターネット通信システムのテスト** (✅ 完了):
  - NetworkAppの「Send Test Message」ボタンでメッセージ送信テストを実施
  - マルチプレイヤー環境（LAN公開）でテスト完了
  - 全プレイヤーがメッセージを受信できることを確認
  - メッセージ通知機能が正常に動作することを確認
  - **注意**: 現在の実装では、`NetworkScreen.sendTestMessage()`は自分自身（UUID: `00000000-0000-0000-0000-000000000000`）宛てにメッセージを送信している。統合サーバー環境では全クライアントにブロードキャストされるため問題なく動作するが、将来的には適切なプレイヤーUUIDを使用する必要がある

- **ワールド別データ分離のテスト**: `./run_with_java17.bat forge:runClient`でMinecraftを起動し、以下をテスト:
  1. ワールドを作成/ロードして、`mochi_os_data/{ワールド名}/mochi_os_data/`ディレクトリが作成されることを確認
  2. そのワールドでスマートフォンを使用して変更を加える（アプリ配置など）
  3. 別のワールドをロードして、`mochi_os_data/{別のワールド名}/mochi_os_data/`が作成され、データが分離されていることを確認
  4. ログで`[SmartphoneBackgroundService] World loaded: {ワールド名}`が出力されることを確認

- **ボイスメモアプリの実装** (✅ 完了):
  - `VoiceMemoApp`: マイク・スピーカーAPIを使用した音声録音・再生アプリ
  - 機能:
    - 録音: マイクから音声を録音（マイクAPI使用）
    - 再生: 録音した音声をスピーカーで再生（スピーカーAPI使用）
    - 保存: VFSに音声データをBase64エンコードして保存
    - 一覧: 保存されたメモの一覧表示
    - 削除: メモの削除
    - 音量調整: スピーカー音量の4段階調整（OFF/LOW/MEDIUM/HIGH）
  - ハードウェア状態表示: マイク・スピーカーの利用可否をリアルタイム表示
  - **注意**: SVCが導入されていない環境では、マイク・スピーカーが利用不可と表示される

- **外部アプリ開発ドキュメントの更新** (✅ 完了):
  - `EXTERNAL_APP_DEVELOPMENT_GUIDE.md`にハードウェアバイパスAPIのセクションを追加
  - 全9種類のハードウェアAPI（モバイルデータ、Bluetooth、位置情報、バッテリー、カメラ、マイク、スピーカー、IC通信、SIM情報）の使用方法を記載
  - 各APIの詳細なコード例とstandalone/forge環境での動作の違いを説明
  - `HardwareTestApp`への参照とデバッグガイドへのリンクを追加
  - 外部開発者がハードウェアAPIを利用可能であることを明確化

---

### 外部アプリ開発API拡張 (✅ 完了)

Android互換のアプリケーション開発APIを実装し、外部開発者が高度なアプリを作成できるようになりました。

1. **パーミッション管理システム** (✅ 完了):
   - `PermissionManager`: Android風のパーミッション管理
   - 機能: パーミッション要求、チェック、リスナーコールバック
   - パーミッション種別: DANGEROUS（実行時要求）、NORMAL（自動許可）
   - 利用可能なパーミッション: CAMERA, MICROPHONE, LOCATION, CONTACTS, SMS, PHONE, STORAGE, CALENDAR, SENSORS, NOTIFICATION
   - VFSへの永続化: `system/permissions.json`
   - `Kernel.getPermissionManager()`でアクセス可能

2. **Intent/Activity管理システム** (✅ 完了):
   - `Intent`: アプリ間通信のためのメッセージクラス
     - 明示的インテント: コンポーネント名指定（直接起動）
     - 暗黙的インテント: アクション/カテゴリ/データ型指定（パターンマッチング）
     - 標準アクション: ACTION_VIEW, ACTION_SEND, ACTION_EDIT, ACTION_PICK等
     - Extrasサポート: 文字列、整数、真偽値、Serializable
   - `IntentFilter`: インテントパターンマッチング
     - アクション、カテゴリ、データスキーム、MIMEタイプのマッチング
     - ワイルドカード対応（例: "image/*", "text/*"）
     - 優先度ベースの解決（priority値）
   - `ActivityInfo`: アプリのアクティビティメタデータ
   - `IntentResolver`: インテント解決エンジン（優先度順ソート）
   - `ActivityManager`: アクティビティ起動サービス
     - アプリ起動、結果コールバック、アプリチューザー
   - `IntentAwareScreen`: インテントを受け取る画面インターフェース
   - `Kernel.getActivityManager()`でアクセス可能

3. **クリップボード管理API** (✅ 完了、2025-10-19更新):
   - `ClipboardManager`: システムクリップボード管理
   - **環境OSクリップボード統合** (✅ 2025-10-19):
     - **プロバイダーパターン実装**:
       - `ClipboardProvider`インターフェース: 環境に応じた実装を切り替え可能
       - `AWTClipboardProvider`: Standalone環境用（`java.awt.Toolkit.getSystemClipboard()`使用）
       - `GLFWClipboardProvider`: Forge環境用（`Minecraft.keyboardHandler.setClipboard()`/`getClipboard()`使用）
     - **Standalone環境** (AWTClipboardProvider):
       - テキスト: `StringSelection`と`DataFlavor.stringFlavor`でコピー/ペースト
       - 画像: `ImageSelection`（カスタムTransferable）と`DataFlavor.imageFlavor`でコピー/ペースト
       - PImage ⇔ BufferedImageの自動変換
       - Windowsのメモ帳やブラウザなど、他のアプリケーションとのクリップボード共有が可能
     - **Forge環境** (GLFWClipboardProvider):
       - GLFW（LWJGL）のクリップボードAPIを使用
       - `Minecraft.keyboardHandler`を経由してOSのクリップボードにアクセス
       - テキストのコピー/ペーストが正常に動作
       - 画像は非サポート（GLFWの制限）
       - `SmartphoneBackgroundService.initializeClipboardProvider()`で自動的に設定
   - `ClipData`: クリップボードデータの抽象化
     - サポート型: TEXT, IMAGE (PImage), URI, HTML, INTENT
     - メタデータ: MIMEタイプ、ラベル、タイムスタンプ
   - 機能: テキスト、画像、HTMLのコピー/ペースト、クリップボードクリア
   - `Kernel.getClipboardManager()`でアクセス可能
   - ログ出力: すべてLoggerServiceに移行済み
   - **テストアプリ: NoteApp** (✅ 完了):
     - メモの作成、編集、削除機能
     - VFS永続化（`apps/note/notes.json`）
     - **テキスト選択機能** (✅ 実装完了):
       - マウスドラッグによる選択範囲設定（`mouseDragged`, `mouseReleased`）
       - 選択範囲のハイライト表示（青色半透明、タイトル・コンテンツ両対応）
       - 選択テキストのみをコピー（選択範囲がない場合はフィールド全体）
       - mousePressed時に選択範囲を自動クリア
     - クリップボード統合: コピー・貼り付け機能（環境OSのクリップボードと連携、Standalone/Forge両対応）
     - クリップボード状態表示: 現在のクリップボード内容のプレビュー

4. **センサー管理API** (✅ 完了):
   - `SensorManager`: Android風センサー管理
   - `Sensor`: センサー表現（11種類）
     - TYPE_ACCELEROMETER: 加速度センサー（m/s², 3軸）
     - TYPE_GYROSCOPE: ジャイロスコープ（rad/s, 3軸）
     - TYPE_LIGHT: 光センサー（lux）
     - TYPE_PROXIMITY: 近接センサー（cm）
     - TYPE_AMBIENT_TEMPERATURE: 温度センサー（℃）
     - TYPE_PRESSURE: 気圧センサー（hPa）
     - TYPE_RELATIVE_HUMIDITY: 湿度センサー（%）
     - TYPE_MAGNETIC_FIELD: 磁気センサー（μT, 3軸）
     - TYPE_GPS: GPS（緯度、経度、高度）
     - TYPE_BATTERY: バッテリーモニター（%）
     - TYPE_NETWORK: ネットワークモニター（強度）
   - `SensorEventListener`: センサーイベントリスナー
   - サンプリングレート: FASTEST, GAME (50Hz), UI (15Hz), NORMAL (5Hz)
   - シミュレーション機能: デバッグ用の値設定
   - デフォルトセンサー: 9種類を初期化時に登録
   - `Kernel.update()`で自動更新
   - `Kernel.getSensorManager()`でアクセス可能

**実装内容**:
- 各APIのインターフェースと実装クラス（`core/src/main/java/jp/moyashi/phoneos/core/service/`）
- Kernelへの統合（フィールド、初期化、getter）
- `EXTERNAL_APP_DEVELOPMENT_GUIDE.md`への包括的なドキュメント追加
  - Section 5: PermissionManager
  - Section 6: Intent/ActivityManager
  - Section 7: ClipboardManager
  - Section 8: SensorManager
  - 各セクションに詳細なコード例と実装サンプルを含む

**設計方針**:
- Android APIとの互換性を重視（外部開発者の学習コストを削減）
- VFSへの永続化対応（パーミッション設定等）
- スレッドセーフな実装（ConcurrentHashMap、CopyOnWriteArrayList使用）
- Kernelのライフサイクルと連動（初期化、更新、シャットダウン）

**今後の拡張**:
- forge環境でのセンサー実装（プレイヤー移動→加速度、時刻→光センサー等）
- パーミッション要求のUIダイアログ実装
- インテントベースのアプリランチャー統合

---

### GUIコンポーネントライブラリ (✅ 完了 - 2025-10-16)

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
textField.setFont(kernel.getJapaneseFont());

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
