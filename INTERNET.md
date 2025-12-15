# MochiMobileOS 仮想インターネット通信システム 仕様書

## 概要

MochiMobileOSは、Minecraft内で動作する独自の仮想インターネット通信システムを提供します。このシステムにより、プレイヤー間のメッセージ送信、外部MODとの連携、システム内部通信を統一されたアーキテクチャで実現します。

## アーキテクチャ

### 1. IPvM (IP virtual Mobile) アドレスシステム

TCP/IPベースの仮想アドレスシステムで、各エンティティに一意のアドレスを割り当てます。

#### アドレス形式

```
[種類]-[UUID]
```

- **種類**: 0〜3の数値でアドレスタイプを識別
- **UUID**: 16進数形式のUUID（ハイフン区切り可）

#### アドレスタイプ

| コード | タイプ | 説明 | 例 |
|--------|--------|------|-----|
| 0 | Player | プレイヤーアドレス | `0-380df991-f603-344c-a090-369bad2a924a` |
| 1 | Device | デバイス/エンティティ（現在未使用） | `1-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` |
| 2 | Server | 外部MODサーバー | `2-economy-service` |
| 3 | System | システム（本MOD内部） | `3-appdata-server` |

#### IPvMAddressクラス

**場所**: `core/src/main/java/jp/moyashi/phoneos/core/service/network/IPvMAddress.java`

**主要メソッド**:
```java
// アドレス生成
IPvMAddress.forPlayer(String playerUUID)
IPvMAddress.forServer(String serverId)
IPvMAddress.forSystem(String systemId)
IPvMAddress.forDevice(String deviceUUID)

// 文字列からの変換
IPvMAddress.fromString(String address)

// タイプ判定
boolean isPlayer()
boolean isServer()
boolean isSystem()
boolean isDevice()

// アドレス情報取得
AddressType getType()
String getUUID()
String toString() // "[種類]-[UUID]"形式で返す
```

### 2. VirtualPacket（仮想パケット）

通信データを表現するクラスで、送信元、送信先、パケットタイプ、データを含みます。

**場所**: `core/src/main/java/jp/moyashi/phoneos/core/service/network/VirtualPacket.java`

#### パケットタイプ

```java
public enum PacketType {
    MESSAGE,              // プレイヤー間メッセージ
    APP_INSTALL_REQUEST,  // アプリインストール要求
    APP_INSTALL_RESPONSE, // アプリインストール応答
    DATA_TRANSFER,        // データ転送
    SYSTEM_NOTIFICATION,  // システム通知
    CUSTOM                // カスタム（外部MOD用）
}
```

#### パケット構造

```java
VirtualPacket packet = VirtualPacket.builder()
    .source(sourceAddress)      // 送信元IPvMAddress
    .destination(destAddress)   // 送信先IPvMAddress
    .type(PacketType.MESSAGE)   // パケットタイプ
    .put("key1", "value1")      // データ（キー・バリュー形式）
    .put("key2", 12345)
    .build();
```

**データ取得**:
```java
String value = packet.getString("key1");
int number = packet.getInt("key2", defaultValue);
boolean flag = packet.getBoolean("key3", defaultValue);
```

### 3. VirtualRouter（仮想ルーター）

パケットのルーティングを担当するサービスで、送信先に応じて適切な配送経路を選択します。

**場所**: `core/src/main/java/jp/moyashi/phoneos/core/service/network/VirtualRouter.java`

#### ルーティングロジック

```
送信パケット
    ↓
VirtualRouter.sendPacket(packet)
    ↓
送信先アドレスの種類を判定
    ↓
┌─────────────┬─────────────────┬─────────────────┐
│ Player宛て  │ Server宛て      │ System宛て      │
│ (isPlayer) │ (isServer)      │ (isSystem)      │
├─────────────┼─────────────────┼─────────────────┤
│ 外部送信    │ 外部送信        │ 内部処理        │
│ ハンドラー  │ ハンドラー      │ (直接配送)      │
│ 経由        │ 経由            │                 │
└─────────────┴─────────────────┴─────────────────┘
    ↓              ↓                   ↓
Minecraftパケット  Mod API呼び出し   receivePacket()
として送信
```

#### 主要メソッド

```java
// パケット送信（ルーティング）
void sendPacket(VirtualPacket packet)

// パケット受信処理
void receivePacket(VirtualPacket packet)

// パケットハンドラー登録
void registerPacketHandler(PacketType type, Consumer<VirtualPacket> handler)

// 外部送信ハンドラー設定（Forge側で設定）
void setExternalSendHandler(Consumer<VirtualPacket> handler)

// システムアドレス取得
static IPvMAddress getAppDataServerAddress() // "3-appdata-server"
```

### 4. MessageStorage（メッセージストレージ）

受信メッセージの永続化を担当するサービスです。

**場所**: `core/src/main/java/jp/moyashi/phoneos/core/service/MessageStorage.java`

#### Messageクラス

```java
public static class Message {
    String getId()           // メッセージID（UUID）
    String getSenderName()   // 送信者名
    String getContent()      // メッセージ内容
    long getTimestamp()      // タイムスタンプ
    boolean isRead()         // 既読フラグ
}
```

#### 主要メソッド

```java
// メッセージ保存
void saveMessage(String senderName, String content)

// 全メッセージ取得（新しい順）
List<Message> getAllMessages()

// 未読メッセージ取得
List<Message> getUnreadMessages()

// 既読マーク
void markAsRead(String messageId)

// メッセージ削除
void deleteMessage(String messageId)
```

## Forge連携（forgeモジュール）

### 1. NetworkHandler

MinecraftのForgeネットワークシステムとの連携を担当します。

**場所**: `forge/src/main/java/jp/moyashi/phoneos/forge/network/NetworkHandler.java`

#### パケット送信

```java
// サーバーに送信（クライアント→サーバー）
NetworkHandler.sendToServer(VirtualPacket packet)

// 全クライアントに送信（サーバー→全クライアント）
NetworkHandler.sendToAll(VirtualPacket packet)

// 特定プレイヤーに送信（サーバー→クライアント）
NetworkHandler.sendToPlayer(VirtualPacket packet, UUID playerUUID)
```

#### パケット処理フロー

**クライアント側**:
```
sendPacket()
    ↓
VirtualRouter.sendPacket()
    ↓
外部送信ハンドラー
    ↓
NetworkHandler.sendToServer()
    ↓
Minecraftパケットとして送信
```

**サーバー側**:
```
Minecraftパケット受信
    ↓
VirtualNetworkPacket.handle()
    ↓
NetworkHandler.handleReceivedPacket()
    ↓
handleServerSide()
    ↓
┌─────────────────┬──────────────────┬─────────────────┐
│ Player宛て      │ Server宛て       │ System宛て      │
├─────────────────┼──────────────────┼─────────────────┤
│ sendToAll()     │ Registry処理     │ Router処理      │
│ ブロードキャスト│ + sendToAll()    │ + sendToAll()   │
└─────────────────┴──────────────────┴─────────────────┘
```

**クライアント側受信**:
```
Minecraftパケット受信
    ↓
VirtualNetworkPacket.handle()
    ↓
NetworkHandler.handleReceivedPacket()
    ↓
handleClientSide()
    ↓
VirtualRouter.receivePacket()
    ↓
登録されたハンドラーで処理
```

### 2. VirtualNetworkPacket

MinecraftネットワークでVirtualPacketを送受信するためのラッパークラスです。

**場所**: `forge/src/main/java/jp/moyashi/phoneos/forge/network/VirtualNetworkPacket.java`

Minecraftの`FriendlyByteBuf`を使用してシリアライズ/デシリアライズを行います。

### 3. SystemPacketHandler

システム宛てパケット（`3-xxx`）の処理を担当します。

**場所**: `forge/src/main/java/jp/moyashi/phoneos/forge/network/SystemPacketHandler.java`

#### 処理されるパケットタイプ

| パケットタイプ | 処理内容 |
|---------------|----------|
| MESSAGE | メッセージをMessageStorageに保存し、通知を作成 |
| APP_INSTALL_REQUEST | アプリインストール要求を処理（未実装） |
| DATA_TRANSFER | データ転送を処理（未実装） |

**MESSAGEパケット処理**:
```java
// パケットデータ
packet.put("message", "メッセージ内容")
packet.put("sender_name", "送信者名")

// 処理
1. MessageStorageにメッセージを保存
2. NotificationManagerに通知を作成
```

### 4. VirtualNetworkRegistry

外部MODがサーバーアドレス（`2-xxx`）としてパケットハンドラーを登録するためのレジストリです。

**場所**: `forge/src/main/java/jp/moyashi/phoneos/forge/network/VirtualNetworkRegistry.java`

```java
// 外部MODからの登録
VirtualNetworkRegistry.register("economy-service", packet -> {
    // パケット処理
    String command = packet.getString("command");
    // ...処理...
});

// パケット処理
VirtualNetworkRegistry.handlePacket(packet); // 宛先のハンドラーを自動検索
```

## 通信パターン

### 1. プレイヤー間メッセージ送信

```java
// 送信側
String targetPlayerUUID = "380df991-f603-344c-a090-369bad2a924a";
IPvMAddress myAddress = IPvMAddress.forPlayer(myUUID);
IPvMAddress targetAddress = IPvMAddress.forPlayer(targetPlayerUUID);

VirtualPacket packet = VirtualPacket.builder()
    .source(myAddress)
    .destination(targetAddress)
    .type(VirtualPacket.PacketType.MESSAGE)
    .put("message", "Hello!")
    .put("sender_name", "Alice")
    .build();

virtualRouter.sendPacket(packet);
```

**フロー**:
1. クライアント: `VirtualRouter.sendPacket()`
2. クライアント→サーバー: Minecraftパケット送信
3. サーバー: `NetworkHandler.handleServerSide()` → `sendToAll()`
4. サーバー→全クライアント: ブロードキャスト
5. 全クライアント: `NetworkHandler.handleClientSide()` → `VirtualRouter.receivePacket()`
6. 対象クライアント: `SystemPacketHandler.handleMessage()` → メッセージ保存 & 通知

### 2. システム通信（アプリインストール要求）

```java
IPvMAddress myAddress = IPvMAddress.forPlayer(myUUID);
IPvMAddress systemAddress = VirtualRouter.getAppDataServerAddress();

VirtualPacket packet = VirtualPacket.builder()
    .source(myAddress)
    .destination(systemAddress)
    .type(VirtualPacket.PacketType.APP_INSTALL_REQUEST)
    .put("app_id", "com.example.app")
    .put("app_name", "Example App")
    .build();

virtualRouter.sendPacket(packet);
```

**フロー**:
1. クライアント: `VirtualRouter.sendPacket()`
2. クライアント→サーバー: Minecraftパケット送信
3. サーバー: `SystemPacketHandler` で処理 → `sendToAll()`
4. サーバー→全クライアント: ブロードキャスト
5. 全クライアント: システムで処理

### 3. 外部MOD連携（電子マネー処理）

```java
// 外部MOD側: サービス登録
VirtualNetworkRegistry.register("economy-service", packet -> {
    String command = packet.getString("command");
    if ("pay".equals(command)) {
        int amount = packet.getInt("amount", 0);
        String recipient = packet.getString("recipient");

        // 送金処理...

        // 応答パケット作成
        VirtualPacket response = VirtualPacket.builder()
            .source(packet.getDestination())
            .destination(packet.getSource())
            .type(VirtualPacket.PacketType.CUSTOM)
            .put("status", "success")
            .put("new_balance", newBalance)
            .build();

        NetworkHandler.sendToServer(response);
    }
});

// MochiMobileOS側: 送金リクエスト
IPvMAddress myAddress = IPvMAddress.forPlayer(myUUID);
IPvMAddress economyServer = IPvMAddress.forServer("economy-service");

VirtualPacket request = VirtualPacket.builder()
    .source(myAddress)
    .destination(economyServer)
    .type(VirtualPacket.PacketType.CUSTOM)
    .put("command", "pay")
    .put("amount", 1000)
    .put("recipient", targetPlayerUUID)
    .build();

virtualRouter.sendPacket(request);

// 応答受信ハンドラー登録
virtualRouter.registerPacketHandler(VirtualPacket.PacketType.CUSTOM, responsePacket -> {
    String status = responsePacket.getString("status");
    int newBalance = responsePacket.getInt("new_balance", 0);
    // UI更新...
});
```

**フロー**:
1. クライアント: リクエスト送信
2. サーバー: `VirtualNetworkRegistry` でMOD APIを呼び出し
3. 外部MOD: 処理実行 & 応答パケット作成
4. サーバー→全クライアント: 応答ブロードキャスト
5. クライアント: 応答受信 & 処理

## NetworkApp（メッセージアプリ）

MochiMobileOSに組み込まれているメッセージアプリです。

**場所**: `core/src/main/java/jp/moyashi/phoneos/core/apps/network/NetworkApp.java`

### NetworkScreen（メイン画面）

**場所**: `core/src/main/java/jp/moyashi/phoneos/core/apps/network/NetworkScreen.java`

#### 機能

1. **メッセージ一覧表示**:
   - `MessageStorage`から全メッセージを取得
   - 未読メッセージには赤い丸マークを表示
   - タップで既読にする

2. **テストメッセージ送信**:
   - "Send Test Message"ボタン
   - 自分自身宛てにテストメッセージを送信
   - 統合サーバー環境では全プレイヤーにブロードキャスト

3. **システムパケット送信**:
   - "Send to System"ボタン
   - システムアドレスにAPP_INSTALL_REQUESTパケットを送信

## 初期化フロー

### Forge MOD起動時

```
MochiMobileOSMod.commonSetup()
    ↓
NetworkHandler.register()
    ↓
SystemPacketHandler.register()
```

### ワールドロード時

```
SmartphoneBackgroundService.onWorldLoad()
    ↓
Kernel作成
    ↓
VirtualRouter作成（Kernelサービスとして登録）
    ↓
MessageStorage作成（Kernelサービスとして登録）
    ↓
NetworkHandler.setupVirtualRouter(kernel)
    ↓
外部送信ハンドラー設定
    ↓
SystemPacketHandler.initialize(kernel)
    ↓
MESSAGEハンドラー登録
```

## データ永続化

### メッセージデータ

**保存場所**: `{ワールドディレクトリ}/mochi_os_data/messages.json`

**形式**:
```json
[
  {
    "id": "uuid-string",
    "senderName": "Alice",
    "content": "Hello!",
    "timestamp": 1234567890,
    "read": false
  }
]
```

## セキュリティと制限

### 現在の制限

1. **認証なし**: 送信元アドレスの検証は行われていません（送信者が自由に設定可能）
2. **暗号化なし**: パケットデータは平文で送信されます
3. **レート制限なし**: パケット送信の頻度制限はありません

### 将来の改善案

1. サーバー側でプレイヤーUUIDを検証し、送信元アドレスを強制設定
2. 重要なデータに対する暗号化機構の追加
3. DoS攻撃対策としてのレート制限実装

## 外部MOD開発者向けAPI

詳細は`VIRTUAL_NETWORK_API.md`を参照してください。

### クイックスタート

1. **依存関係追加**（build.gradle）:
```gradle
repositories {
    maven {
        url "https://your-maven-repo/releases"
    }
}

dependencies {
    implementation "jp.moyashi.phoneos:mochimobileos:1.0.0"
}
```

2. **サービス登録**:
```java
@Mod.EventBusSubscriber(modid = "yourmod", bus = Mod.EventBusSubscriber.Bus.MOD)
public class YourMod {
    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        VirtualNetworkRegistry.register("your-service", packet -> {
            // パケット処理
        });
    }
}
```

3. **パケット送受信**:
```java
// 送信
IPvMAddress serviceAddress = IPvMAddress.forServer("your-service");
VirtualPacket packet = VirtualPacket.builder()
    .source(myAddress)
    .destination(serviceAddress)
    .type(VirtualPacket.PacketType.CUSTOM)
    .put("command", "action")
    .build();
NetworkHandler.sendToServer(packet);

// 受信（登録したハンドラーで処理）
```

## トラブルシューティング

### パケットが届かない

1. **ログ確認**:
   - `[NetworkHandler]`
   - `[VirtualRouter]`
   - `[VirtualNetworkPacket]`
   - `[SystemPacketHandler]`

2. **チェックポイント**:
   - アドレス形式が正しいか（Player/Device: `^[0-3]-[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$`, Server/System: `^[2-3]-[a-zA-Z0-9][a-zA-Z0-9_-]*$`）
   - パケットタイプが正しいか
   - ハンドラーが登録されているか

### メッセージが表示されない

1. **MessageStorageの確認**:
   ```java
   List<Message> messages = kernel.getMessageStorage().getAllMessages();
   System.out.println("Messages: " + messages.size());
   ```

2. **ファイル確認**:
   - `mochi_os_data/messages.json`が存在するか
   - JSON形式が正しいか

### 外部MODのハンドラーが呼ばれない

1. **登録確認**:
   ```java
   VirtualNetworkRegistry.register("service-id", handler);
   ```

2. **アドレス確認**:
   - 送信先が`2-service-id`形式になっているか
   - サービスIDが一致しているか

## パフォーマンス考慮事項

1. **パケットサイズ**: 大きなデータは避け、必要に応じて分割送信を検討
2. **送信頻度**: 高頻度送信は避け、バッチ処理を推奨
3. **ハンドラー処理**: 重い処理は非同期で実行することを推奨

## バージョン情報

- **実装バージョン**: 1.0.0
- **Minecraft**: 1.20.1
- **Forge**: 47.3.12
- **対応Java**: 17

## 関連ドキュメント

- [VIRTUAL_NETWORK_API.md](./VIRTUAL_NETWORK_API.md) - 外部MOD開発者向けAPI詳細
- [DEV.md](./DEV.md) - プロジェクト全体の開発ドキュメント
- [EXTERNAL_APP_DEVELOPMENT_GUIDE.md](./EXTERNAL_APP_DEVELOPMENT_GUIDE.md) - 外部アプリ開発ガイド