# ハードウェアバイパスAPI

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
