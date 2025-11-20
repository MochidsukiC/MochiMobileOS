# 外部アプリ開発API拡張

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
