# MochiMobileOS 仮想ネットワークAPI ドキュメント

## 概要

MochiMobileOSは、Minecraft内で動作する仮想ネットワーク通信システムを提供します。外部Modは`VirtualNetworkRegistry`を使用してサーバーとして登録し、MochiMobileOSデバイスと通信できます。

## IPvMアドレス

仮想ネットワークでは、IPvM (IP virtual Mobile) アドレスを使用します。

### アドレス形式
```
[種類]-UUID
```

### アドレスの種類
- `0-{Player_UUID}`: プレイヤー
- `1-{Entity_UUID}`: デバイス/エンティティ ※現在未使用
- `2-{登録式識別ID}`: サーバー（外部Mod）
- `3-{登録式識別ID}`: システム（MochiMobileOS本体）

### 例
```
0-550e8400-e29b-41d4-a716-446655440000  // プレイヤー
2-0000-0000-0000-0002                   // 外部Modサーバー
3-0000-0000-0000-0001                   // システム（アプリデータサーバー）
```

## 外部Modの登録

外部ModをMochiMobileOSの仮想ネットワークサーバーとして登録する方法を説明します。

### 1. 依存関係の追加

`build.gradle`にMochiMobileOSへの依存を追加します：

```gradle
dependencies {
    implementation fg.deobf("jp.moyashi:mochimobileos:1.0.0")
}
```

### 2. サーバーの登録

Modの初期化時に`VirtualNetworkRegistry.registerServer()`を呼び出します：

```java
import jp.moyashi.phoneos.forge.network.VirtualNetworkRegistry;
import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;

@Mod("example_payment_mod")
public class ExamplePaymentMod {

    private static IPvMAddress serverAddress;

    public ExamplePaymentMod() {
        FMLJavaModLoadingContext.get().getModEventBus()
            .addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // 仮想ネットワークサーバーとして登録
        serverAddress = VirtualNetworkRegistry.registerServer(
            "example_payment_mod",
            this::handlePacket
        );

        System.out.println("[ExamplePaymentMod] Registered as virtual network server");
        System.out.println("[ExamplePaymentMod] Server address: " + serverAddress);
    }

    /**
     * パケット受信ハンドラー
     */
    private void handlePacket(VirtualPacket packet) {
        System.out.println("[ExamplePaymentMod] Received packet from: " + packet.getSource());

        // パケットタイプに応じて処理を分岐
        switch (packet.getType()) {
            case PAYMENT_REQUEST:
                handlePaymentRequest(packet);
                break;
            case GENERIC_REQUEST:
                handleGenericRequest(packet);
                break;
            default:
                System.out.println("[ExamplePaymentMod] Unknown packet type: " + packet.getType());
        }
    }
}
```

### 3. パケットの受信と処理

```java
private void handlePaymentRequest(VirtualPacket packet) {
    // パケットデータを取得
    String fromPlayer = packet.getString("from_player");
    String toPlayer = packet.getString("to_player");
    int amount = packet.getInt("amount");

    System.out.println("[PaymentMod] Payment request: " + fromPlayer + " -> " + toPlayer + ": " + amount);

    // 処理結果を送信元に返信
    sendResponse(packet.getSource(), true, "Payment processed successfully");
}

private void sendResponse(IPvMAddress destination, boolean success, String message) {
    // 応答パケットを作成
    VirtualPacket response = VirtualPacket.builder()
        .source(serverAddress)
        .destination(destination)
        .type(VirtualPacket.PacketType.PAYMENT_RESPONSE)
        .put("success", success)
        .put("message", message)
        .build();

    // パケットを送信（VirtualRouterを通じて）
    jp.moyashi.phoneos.forge.network.NetworkHandler.sendToServer(response);
}
```

## パケットタイプ

MochiMobileOSは以下のパケットタイプを提供します：

| タイプ | 用途 |
|--------|------|
| `APP_INSTALL_REQUEST` | アプリケーションインストール要求 |
| `APP_INSTALL_RESPONSE` | アプリケーションインストール応答 |
| `DATA_TRANSFER` | データ転送 |
| `MESSAGE` | メッセージ送信 |
| `PAYMENT_REQUEST` | 電子マネー処理要求 |
| `PAYMENT_RESPONSE` | 電子マネー処理応答 |
| `GENERIC_REQUEST` | 汎用リクエスト |
| `GENERIC_RESPONSE` | 汎用レスポンス |
| `CUSTOM` | カスタム（独自定義） |

## 完全なサンプルコード

### 電子マネーサービスMod

```java
package com.example.payment;

import jp.moyashi.phoneos.forge.network.VirtualNetworkRegistry;
import jp.moyashi.phoneos.core.service.network.IPvMAddress;
import jp.moyashi.phoneos.core.service.network.VirtualPacket;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.HashMap;
import java.util.Map;

@Mod("payment_service")
public class PaymentServiceMod {

    private static IPvMAddress serverAddress;
    private static final Map<String, Integer> balances = new HashMap<>();

    public PaymentServiceMod() {
        FMLJavaModLoadingContext.get().getModEventBus()
            .addListener(this::commonSetup);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // サーバーとして登録
        serverAddress = VirtualNetworkRegistry.registerServer(
            "payment_service",
            this::handlePacket
        );

        System.out.println("[PaymentService] Registered at: " + serverAddress);

        // テストデータ
        balances.put("test-player-1", 1000);
        balances.put("test-player-2", 500);
    }

    private void handlePacket(VirtualPacket packet) {
        if (packet.getType() == VirtualPacket.PacketType.PAYMENT_REQUEST) {
            handlePayment(packet);
        }
    }

    private void handlePayment(VirtualPacket packet) {
        String fromPlayer = packet.getString("from_player");
        String toPlayer = packet.getString("to_player");
        int amount = packet.getInt("amount");

        // 残高チェック
        int fromBalance = balances.getOrDefault(fromPlayer, 0);
        if (fromBalance < amount) {
            sendPaymentResponse(packet.getSource(), false, "Insufficient balance");
            return;
        }

        // 送金処理
        balances.put(fromPlayer, fromBalance - amount);
        balances.put(toPlayer, balances.getOrDefault(toPlayer, 0) + amount);

        // 成功応答を送信
        sendPaymentResponse(packet.getSource(), true, "Payment successful");

        // 受取人に通知
        IPvMAddress recipientAddress = IPvMAddress.forPlayer(toPlayer);
        VirtualPacket notification = VirtualPacket.builder()
            .source(serverAddress)
            .destination(recipientAddress)
            .type(VirtualPacket.PacketType.MESSAGE)
            .put("message", "You received " + amount + " coins from " + fromPlayer)
            .put("sender_name", "Payment Service")
            .build();

        jp.moyashi.phoneos.forge.network.NetworkHandler.sendToAll(notification);
    }

    private void sendPaymentResponse(IPvMAddress destination, boolean success, String message) {
        VirtualPacket response = VirtualPacket.builder()
            .source(serverAddress)
            .destination(destination)
            .type(VirtualPacket.PacketType.PAYMENT_RESPONSE)
            .put("success", success)
            .put("message", message)
            .build();

        jp.moyashi.phoneos.forge.network.NetworkHandler.sendToAll(response);
    }
}
```

## 通信フロー

### クライアント→サーバー→クライアント

```
1. クライアント (0-{UUID}) が電子マネーサーバー (2-xxxx-xxxx-xxxx-xxxx) に送金要求
   ↓
2. MinecraftパケットとしてサーバーForgeに送信
   ↓
3. サーバーForgeがVirtualNetworkRegistryから該当Modを検索
   ↓
4. 該当ModのPacketHandlerが呼ばれる
   ↓
5. Modが内部で処理を実行
   ↓
6. Modが応答パケットを生成してNetworkHandler.sendToAll()
   ↓
7. クライアントForgeがパケットを受信
   ↓
8. VirtualRouterが適切なハンドラーに転送
```

## デバッグ

### ログ出力

すべての通信は自動的にログに記録されます：

```
[NetworkHandler] Received packet: PAYMENT_REQUEST from 0-xxx to 2-0000-0000-0000-0002
[VirtualNetworkRegistry] Packet handled by server: 0000-0000-0000-0002
[PaymentService] Payment processed successfully
```

### トラブルシューティング

**Q: パケットが届かない**
- サーバーが正しく登録されているか確認（`VirtualNetworkRegistry.getRegisteredModIds()`）
- 宛先アドレスが正しいか確認
- パケットタイプが適切か確認

**Q: 登録に失敗する**
- ModIDが重複していないか確認
- 初期化のタイミングを確認（`FMLCommonSetupEvent`で登録する）

## APIリファレンス

### VirtualNetworkRegistry

```java
// サーバー登録
public static IPvMAddress registerServer(String modId, PacketHandler handler)

// 登録解除
public static void unregisterServer(String modId)

// アドレス取得
public static IPvMAddress getServerAddress(String modId)
```

### NetworkHandler

```java
// 全クライアントに送信
public static void sendToAll(VirtualPacket packet)

// サーバーに送信
public static void sendToServer(VirtualPacket packet)
```

### VirtualPacket

```java
// パケット作成
VirtualPacket packet = VirtualPacket.builder()
    .source(sourceAddress)
    .destination(destAddress)
    .type(PacketType.CUSTOM)
    .put("key", "value")
    .build();

// データ取得
String value = packet.getString("key");
int number = packet.getInt("key");
boolean flag = packet.getBoolean("key");
```

## まとめ

MochiMobileOSの仮想ネットワークAPIを使用することで、Minecraft内で動作するスマートフォンOSと外部Modが密接に連携できます。電子マネー、アプリケーション配信、メッセージングなど、様々なサービスを実装できます。