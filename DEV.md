### MochiMobileOS プロジェクト概要

**MochiMobileOS**は、JavaとProcessingグラフィックスライブラリを基盤として開発された、独自のスマートフォンOSシミュレータです。当初はPC上で単体動作するアプリケーションとして開発が進められ、最終的には**Minecraft Forge MOD**としてゲーム内に実装し、プレイヤーがゲーム内で使える多機能なスマートフォンを提供することを目標としています。

---
### 🏗️ アーキテクチャ設計

本OSは、**柔軟性・拡張性・移植性**を最重要視した、現代的なソフトウェア設計に基づいています。

#### **1. モジュール分離アーキテクチャ**
プロジェクトは明確に分離された3つのモジュールで構成され、各環境への対応を容易にしています。

* **`core`モジュール**: OSの心臓部。ProcessingとJavaのみに依存し、特定の環境（PC/Minecraft）を知らない純粋なOSロジックを実装します。これにより、OS本体の独立性が保たれます。
* **`standalone`モジュール**: `core`モジュールをPC上で実行するための起動プログラム（ランチャー）です。
* **`forge`モジュール (実装中)**: `core`モジュールとMinecraft Forge APIの「橋渡し」役。OSの描画をMinecraft GUIに変換したり、OSからの要求をForge APIに伝えたりする責務を負います。

#### **2. サービス指向カーネル**
OSの中心には`Kernel`クラスが存在し、特定の機能は独立した「サービス」として実装・管理されています。

* **`VFS` (仮想ファイルシステム)**: `mochi_os_data`フォルダをルートとし、アプリのデータや設定を安全に管理します。
* **`AppLoader`**: `/apps`ディレクトリや`/mods`フォルダをスキャンし、アプリケーションを動的に認識・読み込みます。
* **`LayoutManager`**: ホーム画面のアイコン配置情報をJSON形式で永続化します。
* **`GestureManager`**: マウス操作を`TAP`, `LONG_PRESS`, `SWIPE`などの高度なジェスチャーに変換し、優先度に基づいて各UIコンポーネントに伝達します。

#### **3. 統合MODモデル**
Minecraftとの深い連携（アイテム追加など）が必要なアプリは、それ自体が**一個のForge MOD**として開発されます。このMODは、Forgeのシステムに自身を登録すると同時に、MochiMobileOSにも「私はアプリです」と自己紹介する仕組みを持ちます。これにより、開発の手間を最小限に抑えつつ、Forgeの機能を100%活用できます。

#### **4. IApplicationインターフェース仕様**
外部アプリケーション開発のための統一インターフェース：

* **必須メソッド**：
    * `String getApplicationId()`: アプリの一意識別子
    * `String getApplicationName()`: 表示名
    * `String getApplicationVersion()`: バージョン情報
    * `PImage getIcon(PApplet p)`: PApplet環境でのアイコン取得
    * `PImage getIcon(PGraphics g)`: PGraphics環境でのアイコン取得
    * `Screen createMainScreen()`: メイン画面インスタンス作成
    * `void initialize()`: アプリ初期化処理
    * `void onInstall()`: アプリインストール時の処理
    * `void terminate()`: アプリ終了処理

* **互換性エイリアス**（旧バージョン対応）：
    * `String getApplicationName()`: `getName()`のエイリアス
    * `String getApplicationVersion()`: `getVersion()`のエイリアス

#### **5. 外部アプリケーション統合システム**
MochiMobileOSは、2つの形態での外部アプリケーション対応をサポートします：

* **standalone環境**: `/apps`ディレクトリと`/mods`ディレクトリからの動的読み込み
* **Forge環境**: `PhoneAppRegistryEvent`を通じたMODアプリケーション登録システム

##### **5.1 MODアプリケーション登録フロー**
1. **MochiMobileOSMod**が初期化時に`PhoneAppRegistryEvent`を発行
2. 他のMODが`@SubscribeEvent`でイベントをリスン
3. `event.registerApp(application)`でアプリケーションを登録
4. **ModAppRegistry**がアプリケーションを一元管理
5. 登録されたアプリは「利用可能なアプリ候補」としてAppStoreに表示

##### **5.2 スレッドセーフなアプリケーション管理**
`ModAppRegistry`は`CopyOnWriteArrayList`を使用し、複数のMODからの同時登録に対応しています。

---
### 🚀 主要機能

本OSは、現代的なスマートフォンが持つ主要機能を網羅した、高度なシステムです。

* **セキュリティシステム**:
    * **ロック画面**: パターン認証によるロック解除機能を搭載。時刻や通知プレビューも表示します。
    * **`LockManager`**: OSのロック状態を管理し、パターンの照合・保存を行います。

* **ランチャーアプリ**:
    * **マルチページホーム画面**: 左右のスワイプでページを切り替え可能。
    * **編集モード**: アイコンの長押しで、アイコンが揺れる編集モードに移行。ドラッグ＆ドロップによる配置変更や、ショートカットの削除が可能です。
    * **Appライブラリ**: インストール済みの全アプリを一覧表示。ここからホーム画面にアプリを追加できます。
    * **Dock (常時表示領域)**: 画面下部に主要なアプリを固定配置できます。

* **システムUI**:
    * **コントロールセンター**: 画面下からのスワイプアップで起動。Wi-FiやBluetoothなどのシステム設定を素早く切り替えられます。
    * **通知センター**: 画面上からのスワイプダウンで起動。OSやアプリからのお知らせを時系列で確認できます。
    * **ポップアップシステム**: アプリからの確認メッセージなどをモーダル表示します。

---
### 🔧 技術仕様と互換性

* **Minecraft Forge**: `1.20.1`
* **Java**: `17`
* **Processing**: `4.4.4`
* **主要ライブラリ**:
    * `Gson`: レイアウト情報のJSON永続化に使用。
    * `Minim`: 音声処理ライブラリ（将来的な機能のため導入済み）。
* **国際化**: UTF-8エンコーディングと日本語フォント（Meiryo）のロードに対応し、日本語表示をサポートしています。

---
### 🔬 現状の実装

`Kernel.java`の初期化処理 (`setup`メソッド) および`forge`モジュールのコードを基に、現在の実装状況を以下に示します。

#### **有効化されているコアサービス**

*   **VFS**: 仮想ファイルシステム
*   **SettingsManager**: 設定管理
*   **SystemClock**: システム時刻管理
*   **AppLoader**: アプリケーションローダー
*   **LayoutManager**: ホーム画面のレイアウト管理
*   **PopupManager**: グローバルポップアップ表示
*   **GestureManager**: タッチジェスチャー認識
*   **ControlCenterManager**: コントロールセンター管理
*   **NotificationManager**: 通知センター管理
*   **LockManager**: 画面ロック管理
*   **LayerManager**: UIレイヤー管理

#### **組み込みアプリケーション**

*   **LauncherApp**: ホーム画面およびアプリランチャー
*   **SettingsApp**: OSの基本設定アプリ
*   **CalculatorApp**: 電卓アプリ
*   **AppStoreApp**: アプリストア（UIのみ）

#### **Forge連携 (MochiMobileOSMod)**

`forge`モジュールは本格実装段階にあり、以下の機能が実装されています。

*   **MODの基本構造**: `@Mod`アノテーションの付いた`MochiMobileOSMod.java`が存在し、MODとして認識される基本的な構造ができています。
*   **アイテム登録**: スマートフォンを表現するアイテム (`ModItems`) がMinecraftに登録されます。
*   **アプリケーション登録API**: 他のMODがMochiMobileOS用のアプリケーションとして自身を登録するためのイベントシステム (`PhoneAppRegistryEvent`) が実装されており、「統合MODモデル」の基盤が構築されています。
*   **ModAppRegistry**: シングルトンパターンによるスレッドセーフなMODアプリケーション管理システム。`CopyOnWriteArrayList`を使用して複数MODからの同時登録に対応。
*   **PhoneAppRegistryEvent**: `FMLCommonSetupEvent`時に発行される、MODアプリケーション登録のためのカスタムイベント。イベントのキャンセル不可で、結果を持たない安全なイベント設計。
*   **初期化フェーズ管理**: 5段階の初期化プロセス（Registry準備→Kernel確認→アイテム初期化→イベント発行→アプリ処理）で確実な起動を保証。

---
### 🐛 現在の問題とTODO

プロジェクトのソースコードに残された`TODO`コメントに基づき、現在把握している問題点と今後のタスクを以下に示します。

#### **1. Forge連携の実装**
`forge`モジュールと`core`モジュールの連携が部分的に実装済みです。

**実装済み部分**:
*   **MODアプリケーション登録システム**: `PhoneAppRegistryEvent` + `ModAppRegistry`による完全なMODアプリ管理機能
*   **イベント駆動型アーキテクチャ**: FMLイベントバスを活用したMOD間連携システム
*   **スレッドセーフなアプリ管理**: 複数MODからの同時登録対応

**残存課題**:
*   **`Kernel`のブリッジ**: `core`の`Kernel`をMinecraft環境で利用するためのラッパーまたはブリッジクラス (`KernelAdapter`など) の実装が必要です。
*   **`AppLoader`との連携**: Forge経由で登録されたMODアプリを、`core`の`AppLoader`に認識させる仕組み（`syncWithModRegistry()`メソッドの実装）が必要です。
*   **実行環境統合**: 現在はスタンドアロン版とForge版が独立動作しているため、統一された動作環境の構築が必要です。

#### **2. UIシステムの移行 ✅ 解決済み (2025-09-28)**
通知センターとコントロールセンターの描画問題を修正し、LayerManagerベースのシステムに完全移行しました。

**修正済み項目**:
*   **コントロールセンターのレイヤー統合**: `registerControlCenterAsLayer()`メソッドを実装し、優先度9000でレイヤーマネージャーに登録
*   **通知センターのレイヤー統合**: `registerNotificationCenterAsLayer()`メソッドを実装し、優先度8500でレイヤーマネージャーに登録
*   **UILayerのPGraphics対応**: `LayerRenderer`インターフェースに`render(PGraphics)`メソッドを追加し、PGraphics環境での描画を対応
*   **LayerManagerのPGraphics対応**: `updateAndRender(PGraphics)`メソッドでレイヤー更新処理を有効化
*   **重複描画の解決**: `isComponentManagedByLayer()`メソッドでレイヤー管理状態を正しく検出し、直接描画を無効化

**技術詳細**:
*   レイヤー優先度階層: コントロールセンター(9000) > 通知センター(8500) > ロック画面(8000)
*   PAppletとPGraphicsの両環境で動作する統一レンダリングシステム
*   ジェスチャー処理とレイヤー描画の適切な統合

#### **2-2. アニメーション省略問題の修正 ✅ 解決済み (2025-09-28)**
コントロールセンターと通知センターで「初回スライドインは正常だが、スライドアウトとその後のアニメーションが省略される」問題を完全解決しました。

**問題の原因と解決策**:

**第1層問題 - show()時のアニメーション状態管理**:
* **原因**: アニメーション完了状態からの再表示時に、`animationProgress`と`targetAnimationProgress`が同値になりアニメーション更新がスキップされる
* **解決**: show()メソッドで`animationProgress >= 0.99f`の場合に`animationProgress = 0.0f`に強制リセット

**第2層問題 - hide()時のアニメーション状態設定**:
* **原因**: hide()メソッドで不適切なアニメーション状態リセットロジック
* **解決**: hide()時は`animationProgress`を現在値(1.0)に保持し、`targetAnimationProgress = 0.0f`でスライドアウト開始

**第3層問題 - LayerManagerの描画停止**:
* **原因**: hide()後に`isVisible = false`となり、LayerManagerが描画をスキップしてアニメーション更新が停止
* **解決**: `isVisible()`メソッドを`return isVisible || animationProgress > 0.01f`に修正し、アニメーション中は描画継続

**修正されたファイル**:
*   `ControlCenterManager.java`: show()/hide()/isVisible()メソッドの修正
*   `NotificationManager.java`: show()/hide()/isVisible()メソッドの修正

**技術詳細**:
*   アニメーション状態の3段階管理: 表示開始(0.0→1.0) / 表示完了(1.0) / 非表示開始(1.0→0.0)
*   LayerManagerとアニメーションシステムの統合による継続的な描画更新
*   フレームベースの重複更新防止システムと状態管理の両立

#### **3. 設定アプリの機能実装**
`SettingsApp`は現在UIのみで、実際の機能が伴っていません。
*   **各種設定項目**: 「Wi-Fi」「Bluetooth」「機内モード」などのトグルスイッチが、実際のOSの状態を反映・変更するように実装する必要があります。

#### **4. ランチャーの機能改善**
ホーム画面 (`HomeScreen`) の一部機能が未実装または不完全です。
*   **Dockへのショートカット移動**: ホーム画面のアイコンを長押ししてDockに移動させる機能 (`moveShortcutToDock`) が正しく動作していません。
