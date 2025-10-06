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

### 2. 現在の仕様

**有効なサービス**:
VFS, SettingsManager, SystemClock, AppLoader, LayoutManager, PopupManager, GestureManager, ControlCenterManager, NotificationManager, LockManager, LayerManager, LoggerService

**組み込みアプリ**:
LauncherApp, SettingsApp, CalculatorApp, AppStoreApp (UIのみ), NetworkApp (メッセージ&ネットワークテスト), HardwareTestApp (ハードウェアAPIデバッグ), VoiceMemoApp (音声録音・再生)

**仮想インターネット通信システム**:
*   **IPvMアドレス**: TCP/IPベースの仮想アドレスシステム。形式: `[種類]-UUID`
    - `0-{Player_UUID}`: プレイヤー
    - `1-{Entity_UUID}`: デバイス(エンティティ) ※現在未使用
    - `2-{登録式識別ID}`: サーバー(外部Mod)
    - `3-{登録式識別ID}`: システム(本Mod)
*   **仮想ルーター**: 本プログラムがルーター機能を提供し、送信先に応じた適切なルーティングを実行
*   **通信方式**:
    - プレイヤー向け: IPからPlayerUUIDを抽出し、Minecraftパケットとして送信
    - サーバー(外部Mod)向け: レジストリに登録されたMod APIを叩いて通信
    - システム向け: 本Mod内部で直接処理
*   **ユースケース**:
    1. アプリケーションインストール要求 (クライアント→システム)
    2. 電子マネー処理 (クライアント→外部Modサーバー→クライアント)
    3. メッセージ送信 (クライアント→クライアント)

**Forge連携** (✅ 完了):
*   MODとしての基本構造とスマートフォンアイテムが実装済み。
*   他のMODがアプリを登録するためのAPI (`PhoneAppRegistryEvent`) が機能している。
*   **PGraphics→Minecraft GUI変換システム**: KernelのPGraphicsバッファからMinecraftのNativeImageへの高速ピクセル変換を実装
*   **Kernel統合**: `Kernel.initializeForMinecraft()`による最適化された初期化処理
*   **入力システム**: Minecraftのマウス・キーボードイベントをKernel APIにバイパス（ProcessingScreen経由）
*   **Kernelインスタンス管理**:
    - ワールドロード時にKernelを起動 (`LevelEvent.Load`)
    - ワールドアンロード時にKernelをシャットダウン (`LevelEvent.Unload`)
    - ワールドごとにKernelインスタンスとデータを分離管理
*   **画面表示システム**:
    - 動的スケーリング：画面サイズに応じてスマートフォン表示を自動拡大縮小（縦横比維持）
    - 中央配置：常に画面中央に表示、20pxマージン確保
    - 画面サイズ：スタンドアロンと同じ400x600ピクセルに統一
    - PoseStack変換を使用した高品質なスケーリング
*   **ワールド別データ分離** (✅ 実装完了、テスト待ち):
    - ワールドロード時に`LevelEvent.Load`を監視
    - シングルプレイヤー：`./mochi_os_data/{world_name}/mochi_os_data/`
    - マルチプレイヤー：`./mochi_os_data/{server_address}/mochi_os_data/`
    - ワールド切り替え時に自動的に新しいKernelインスタンスを作成
    - VFS、Kernel、SmartphoneBackgroundServiceで完全サポート
    - **注意**: core自体の動作は変更なし（`VFS()`は`mochi_os_data/`を使用）、forgeでのみ分離

### 3. 既知の現在の問題

1.  **設定アプリが機能しない**: `SettingsApp`のUIは存在するが、設定を実際に変更するバックエンドロジックが実装されていない。
2.  **ランチャーの不具合**: ホーム画面のアイコンをDockに移動する機能が正しく動作しない。
3.  **ボイスメモアプリの音声再生問題（✅ 解決済み）**:
    - **症状**: 録音した音声が倍速再生され、音がブツ切りになる
    - **原因**:
      - 録音時のサンプリングレートが環境によって異なる（48kHz/44.1kHz/16kHz等）
      - 再生時は常に48kHz固定を想定していたため、速度が不一致
      - 例: 44.1kHzで録音→48kHzで再生 = 1.088倍速
    - **解決策**:
      - 録音時のサンプリングレートをメタデータとして保存（JSON形式）
      - 再生時に線形補間を使用して48kHzにリサンプリング
      - `JavaMicrophoneRecorder`、`AudioMixer`、`ForgeMicrophoneSocket`にサンプリングレート取得機能を追加
4.  **Forge環境でボタンが反応しない問題（デバッグ中）**:
    - **症状**: VoiceMemoAppのRecordボタンと、CalculatorAppのボタンが反応しない。ネットワークアプリでメッセージを送信しても、画面を開き直さないとリストが更新されない。
    - **正常動作**: コントロールセンターの音量ボタン、NetworkAppのSend Test Messageボタンは機能する
    - **調査結果**:
      - `ProcessingScreen.render()`内で直接LWJGL/GLFWを使用したマウスイベント検出は機能している
      - `kernel.mousePressed()`/`kernel.mouseReleased()`は正常に実行されている（例外なし）
      - マウス座標も正しく変換されている（例: 座標(93, 174)はRecordボタン範囲(X:50-170, Y:150-190)内）
      - しかし、画面上で何も変化が起きない
    - **根本原因（判明）**:
      - Minecraftのrender()は毎フレーム呼び出されるが、マウスイベント処理後に次のフレームが来るまで画面が更新されない
      - つまり、**描画更新のタイミング問題**が原因
    - **実施した修正**:
      - `ProcessingScreen`でマウスイベント（mousePressed/mouseReleased）直後に即座に`kernel.update()`と`kernel.render()`を呼び出すように変更
      - 60フレームごとに通常レンダリングのログを出力するように変更（ログ過剰防止）
      - マウスイベント後の強制更新は必ずログに出力
    - **OSロガーシステム実装** (✅ 完了):
      - VFS内に保存されるOS専用ロガーシステムを実装
      - `LoggerService`: ログレベル（DEBUG/INFO/WARN/ERROR）、ログローテーション（1MB超過時）、メモリバッファ（最新100件）
      - ログ保存先: `system/logs/latest.log`、アーカイブ: `system/logs/archive.log`
      - Kernel、ScreenManager、VoiceMemoScreenに統合済み
      - **デバッグ方法**: VFSから直接ログを確認可能（例: `forge/run/mochi_os_data/{ワールド名}/mochi_os_data/system/logs/latest.log`）
      - **次のステップ**: OSロガーを使用してボタンクリック時の詳細なデバッグログを収集し、問題箇所を特定
5.  **PGraphicsアーキテクチャ移行 (Phase 8 - ✅ 100%完了)**: `core`モジュールのPGraphics統一化が完了。
    *   **完了済み**:
        - **インターフェース**: IApplication, Screen (完全移行、@Deprecated ブリッジ付き)
        - **画面クラス**: HomeScreen, CalculatorScreen, SettingsScreen, LockScreen, SimpleHomeScreen, AppLibraryScreen, BasicHomeScreen, SafeHomeScreen, AppLibraryScreen_Complete
        - **アプリケーションクラス**: LauncherApp, SettingsApp, CalculatorApp
        - **サービスクラス**: ControlCenterManager, NotificationManager (他は元々PApplet非依存)
        - **統一座標システム**: CoordinateTransform クラス実装済み
    *   **移行内容**: すべてのPAppletメソッドは@Deprecatedとしてマークされ、PGraphicsメソッドが新しいプライマリAPIとなっている

### 4. TODO

#### ハードウェアバイパスAPIの実装 (✅ 完了)

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

#### Minecraft Forge連携システム (✅ 完了)

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

#### 次のステップ

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