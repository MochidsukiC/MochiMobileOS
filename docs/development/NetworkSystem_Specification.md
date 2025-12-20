# MochiMobileOS ネットワークシステム仕様書

**バージョン:** 2.0
**最終更新:** 2024年
**対象:** アプリケーション開発者、外部MOD開発者

---

## 目次

1. [概要](#1-概要)
2. [IPvMアドレス体系](#2-ipvmアドレス体系)
3. [NetworkAdapter API](#3-networkadapter-api)
4. [VirtualSocketインターフェース](#4-virtualsocketインターフェース)
5. [ゲーム内仮想インターネット回線](#5-ゲーム内仮想インターネット回線)
6. [アプリ開発者向けガイド](#6-アプリ開発者向けガイド)
7. [外部MOD連携](#7-外部mod連携)

---

## 1. 概要

### 1.1 MochiMobileOSネットワークアーキテクチャ

MochiMobileOSは、2種類のネットワーク通信をサポートします：

1. **実インターネット (IPv4/IPv6)** - Java HttpClientを使用した実際のWeb通信
2. **仮想インターネット (IPvM)** - ゲーム内独自のネットワークプロトコル

```
┌─────────────────────────────────────────────────────────────┐
│                    MochiMobileOS アプリ                     │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│   ┌─────────────────────────────────────────────────────┐   │
│   │                 NetworkAdapter                       │   │
│   │                                                     │   │
│   │   request(url) → URLを解析 → 適切なAdapterへ        │   │
│   │                                                     │   │
│   │   ┌───────────────────┐   ┌───────────────────┐     │   │
│   │   │  VirtualAdapter   │   │   RealAdapter     │     │   │
│   │   │  (IPvMアドレス)    │   │   (IPv4/IPv6)     │     │   │
│   │   │                   │   │                   │     │   │
│   │   │  setSocket()で    │   │  Java HttpClient  │     │   │
│   │   │  外部実装をバインド│   │                   │     │   │
│   │   └─────────┬─────────┘   └───────────────────┘     │   │
│   └─────────────┼───────────────────────────────────────┘   │
│                 │                                           │
│                 ▼                                           │
│   ┌─────────────────────────────────────────────────────┐   │
│   │              VirtualSocket (インターフェース)        │   │
│   │                                                     │   │
│   │   Forge環境: ForgeVirtualSocket                     │   │
│   │   Standalone環境: StandaloneVirtualSocket           │   │
│   │   将来: GTAVirtualSocket, RustVirtualSocket等       │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 設計原則

**依存性逆転の原則 (DIP)**

- Core層はインターフェース（`VirtualSocket`）のみを定義
- 実行モジュール（Forge/Standalone等）が実装を提供
- 新モジュール追加時にCore変更不要

**圏外状態の管理**

- `VirtualSocket`が未設定の場合は「圏外」状態
- SIMカードのない携帯電話と同様の動作

---

## 2. IPvMアドレス体系

### 2.1 アドレス形式

IPvM (IP virtual Mobile) アドレスは以下の形式を取ります：

```
[種類]-[識別子]
```

| 種類コード | 名称 | 識別子形式 | 説明 |
|-----------|------|-----------|------|
| 0 | PLAYER | UUID | プレイヤーのMinecraft UUID |
| 1 | DEVICE | UUID | デバイス/エンティティ（将来用） |
| 2 | SERVER | 文字列ID | 外部MODが提供するサーバー |
| 3 | SYSTEM | 文字列ID | 本MODが提供するシステムサービス |

### 2.2 識別子形式

**UUID形式（Player/Device用）:**
```
xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
例: 0-550e8400-e29b-41d4-a716-446655440000
```

**文字列ID形式（Server/System用）:**
```
[a-zA-Z0-9][a-zA-Z0-9_-]*
例: 3-sys-google
例: 2-mod-myservice
```

### 2.3 IPvMアドレスの例

| アドレス | 説明 |
|---------|------|
| `0-550e8400-e29b-41d4-a716-446655440000` | プレイヤーUUIDへの直接通信 |
| `3-sys-google` | システム提供のGoogle検索サービス |
| `3-sys-appstore` | システム提供のアプリストア |
| `2-mymod-chat` | 外部MOD「mymod」のチャットサーバー |

### 2.4 URL形式

IPvMアドレスを使用したURLは以下の形式です：

```
http://[IPvMアドレス]/[パス]
```

例：
- `http://3-sys-google/search?q=minecraft`
- `http://0-550e8400-e29b-41d4-a716-446655440000/profile`
- `http://2-mymod-chat/messages`

---

## 3. NetworkAdapter API

### 3.1 概要

`NetworkAdapter`はすべてのネットワーク通信の統一エントリーポイントです。

```java
// Kernelからの取得方法
NetworkAdapter networkAdapter = kernel.getNetworkAdapter();
```

### 3.2 主要メソッド

#### request() - HTTP リクエスト送信

```java
/**
 * URLに対してHTTPリクエストを送信する。
 * URLを解析し、適切なアダプターにルーティングする。
 *
 * @param url リクエストURL
 * @param method HTTPメソッド（GET, POST等）
 * @return HTTPレスポンスのFuture
 * @throws NetworkException ネットワークエラー時
 */
CompletableFuture<NetworkResponse> request(String url, String method) throws NetworkException;
```

使用例：
```java
// IPvMアドレスへのリクエスト（VirtualAdapter経由）
networkAdapter.request("http://3-sys-google/search?q=test", "GET")
    .thenAccept(response -> {
        if (response.isSuccess()) {
            String html = response.getBody();
            // HTMLを処理
        }
    });

// 通常URLへのリクエスト（RealAdapter経由）
networkAdapter.request("https://example.com/api/data", "GET")
    .thenAccept(response -> {
        // レスポンスを処理
    });
```

#### getVirtualAdapter() / getRealAdapter()

```java
// 仮想ネットワークアダプターを取得
VirtualAdapter virtualAdapter = networkAdapter.getVirtualAdapter();

// 実インターネットアダプターを取得
RealAdapter realAdapter = networkAdapter.getRealAdapter();
```

#### ネットワーク状態の確認

```java
// 仮想ネットワークが利用可能か
boolean available = networkAdapter.isVirtualNetworkAvailable();

// 電波強度（0-5）
int signal = networkAdapter.getSignalStrength();

// キャリア名
String carrier = networkAdapter.getCarrierName();

// ネットワーク状態
NetworkStatus status = networkAdapter.getVirtualNetworkStatus();
```

### 3.3 NetworkResponse

```java
public static class NetworkResponse {
    public enum Source {
        VIRTUAL,  // IPvM経由
        REAL      // 実インターネット経由
    }

    // ステータスコード（200, 404, 500等）
    int getStatusCode();

    // Content-Type
    String getContentType();

    // レスポンスボディ
    String getBody();

    // リクエスト元
    Source getSource();

    // 成功判定（200-299）
    boolean isSuccess();

    // 仮想ネットワーク経由かどうか
    boolean isFromVirtualNetwork();
}
```

### 3.4 NetworkStatus

```java
public enum NetworkStatus {
    NO_SERVICE("圏外"),      // VirtualSocket未設定または電波なし
    CONNECTED("接続中"),      // 正常に接続
    CONNECTING("接続中..."),  // 接続処理中
    OFFLINE("オフライン"),    // 意図的な切断
    ERROR("エラー");          // 接続エラー

    // データ送信可能か
    boolean canSendData();

    // 表示名を取得
    String getDisplayName();
}
```

---

## 4. VirtualSocketインターフェース

### 4.1 概要

`VirtualSocket`は、外部モジュールがMochiMobileOSの仮想ネットワークに参加するためのインターフェースです。

**このインターフェースを実装することで、任意の通信基盤（Minecraft Forge、Rust、GTA V等）をMochiMobileOSの仮想ネットワークに接続できます。**

### 4.2 インターフェース定義

```java
package jp.moyashi.phoneos.core.service.network;

public interface VirtualSocket {

    /**
     * ソケットが利用可能かを判定する。
     */
    boolean isAvailable();

    /**
     * 現在の接続状態を取得する。
     */
    NetworkStatus getStatus();

    /**
     * パケットを送信する。
     */
    CompletableFuture<Boolean> sendPacket(VirtualPacket packet);

    /**
     * パケット受信リスナーを登録する。
     */
    void setPacketListener(Consumer<VirtualPacket> listener);

    /**
     * HTTPリクエストを送信する。
     */
    CompletableFuture<VirtualHttpResponse> httpRequest(
        IPvMAddress destination,
        String path,
        String method
    ) throws NetworkException;

    /**
     * 接続を閉じる。
     */
    void close();

    /**
     * 電波強度を取得する（0-5）。
     */
    default int getSignalStrength() {
        return isAvailable() ? 5 : 0;
    }

    /**
     * キャリア名を取得する。
     */
    default String getCarrierName() {
        return "Virtual Network";
    }
}
```

### 4.3 VirtualHttpResponse

```java
public class VirtualHttpResponse {
    // コンストラクタ
    public VirtualHttpResponse(
        int statusCode,
        String statusText,
        String contentType,
        String body
    );

    // ゲッター
    int getStatusCode();
    String getStatusText();
    String getContentType();
    String getBody();
    boolean isSuccess();

    // ファクトリメソッド
    static VirtualHttpResponse ok(String body);
    static VirtualHttpResponse notFound(String message);
    static VirtualHttpResponse internalError(String message);
    static VirtualHttpResponse noService();
}
```

### 4.4 Kernelへの登録方法

```java
// 1. VirtualSocketを実装
public class MyVirtualSocket implements VirtualSocket {
    // 実装...
}

// 2. Kernel初期化後にバインド
Kernel kernel = new Kernel();
kernel.setup();

// 3. VirtualAdapterにソケットを設定
VirtualSocket socket = new MyVirtualSocket();
kernel.getNetworkAdapter().getVirtualAdapter().setSocket(socket);
```

### 4.5 サンプル実装（Standalone環境）

```java
public class StandaloneVirtualSocket implements VirtualSocket {

    private final Kernel kernel;
    private Consumer<VirtualPacket> packetListener;
    private boolean connected = true;

    public StandaloneVirtualSocket(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public boolean isAvailable() {
        return connected;
    }

    @Override
    public NetworkStatus getStatus() {
        return connected ? NetworkStatus.CONNECTED : NetworkStatus.OFFLINE;
    }

    @Override
    public CompletableFuture<Boolean> sendPacket(VirtualPacket packet) {
        return CompletableFuture.supplyAsync(() -> {
            // VirtualRouterに直接ルーティング
            VirtualRouter router = kernel.getVirtualRouter();
            if (router != null) {
                router.receivePacket(packet);
                return true;
            }
            return false;
        });
    }

    @Override
    public CompletableFuture<VirtualHttpResponse> httpRequest(
            IPvMAddress destination, String path, String method)
            throws NetworkException {

        if (!isAvailable()) {
            throw NetworkException.noService();
        }

        // HTTPリクエストパケットを作成
        VirtualPacket packet = VirtualPacket.builder()
                .source(IPvMAddress.forSystem("standalone-browser"))
                .destination(destination)
                .type(VirtualPacket.PacketType.GENERIC_REQUEST)
                .put("path", path)
                .put("method", method)
                .build();

        // 非同期でレスポンスを待機
        CompletableFuture<VirtualHttpResponse> future = new CompletableFuture<>();

        // パケット送信とレスポンス待機の実装...

        return future;
    }

    @Override
    public void setPacketListener(Consumer<VirtualPacket> listener) {
        this.packetListener = listener;
    }

    @Override
    public void close() {
        connected = false;
    }

    @Override
    public int getSignalStrength() {
        return connected ? 5 : 0;
    }

    @Override
    public String getCarrierName() {
        return "Standalone Network";
    }
}
```

---

## 5. ゲーム内仮想インターネット回線

### 5.1 Minecraft Forge環境

Forge環境では、Minecraft ForgeのSimpleChannelを使用してパケット通信を行います。

#### 通信フロー

```
┌────────────────┐                    ┌────────────────┐
│   クライアント   │                    │    サーバー     │
│  (MochiMobileOS) │                    │  (VirtualRouter) │
└───────┬────────┘                    └────────┬───────┘
        │                                      │
        │  1. HTTPリクエストパケット送信        │
        │ ─────────────────────────────────→   │
        │                                      │
        │                                      │ 2. VirtualRouterで
        │                                      │    パケット処理
        │                                      │
        │  3. HTTPレスポンスパケット返信        │
        │ ←─────────────────────────────────   │
        │                                      │
        ▼                                      ▼
```

#### 電波強度の計算

Minecraft環境では、プレイヤーのY座標に基づいて電波強度が決定されます：

| Y座標範囲 | 電波強度 | 説明 |
|----------|---------|------|
| 60 - 100 | 5 (最強) | 地上付近（最適） |
| 100 - 200 | 4 (強い) | 高所 |
| 30 - 60 | 3 (中程度) | 地下浅部 |
| 0 - 30 | 2 (弱い) | 地下深部 |
| 0未満 | 1 (非常に弱い) | 深層 |

#### キャリア名

ディメンションに基づいてキャリア名が決定されます：

| ディメンション | キャリア名 |
|--------------|-----------|
| overworld | Overworld Network |
| the_nether | Nether Network |
| the_end | End Network |
| その他 | [ディメンション名] Network |

### 5.2 Standalone環境

Standalone環境（デスクトップアプリ）では、すべての通信がローカルで処理されます。

- 電波強度: 常に5（最強）
- キャリア名: "Standalone Network"
- パケット: VirtualRouterに直接ルーティング

---

## 6. アプリ開発者向けガイド

### 6.1 基本的な使用方法

```java
public class MyApp {
    private Kernel kernel;

    public void fetchData() {
        NetworkAdapter network = kernel.getNetworkAdapter();

        // 接続状態を確認
        if (!network.isVirtualNetworkAvailable()) {
            showError("圏外です");
            return;
        }

        // IPvMアドレスへリクエスト
        try {
            network.request("http://3-sys-myservice/api/data", "GET")
                .thenAccept(response -> {
                    if (response.isSuccess()) {
                        processData(response.getBody());
                    } else {
                        showError("エラー: " + response.getStatusCode());
                    }
                })
                .exceptionally(e -> {
                    showError("通信エラー: " + e.getMessage());
                    return null;
                });
        } catch (NetworkException e) {
            if (e.getErrorType() == NetworkException.ErrorType.NO_SERVICE) {
                showError("圏外です");
            } else {
                showError("ネットワークエラー");
            }
        }
    }
}
```

### 6.2 ネットワーク状態の監視

```java
public class StatusBarComponent {

    public void updateNetworkIndicator() {
        NetworkAdapter network = kernel.getNetworkAdapter();

        int signal = network.getSignalStrength();
        String carrier = network.getCarrierName();
        NetworkStatus status = network.getVirtualNetworkStatus();

        // UIを更新
        signalIcon.setLevel(signal);
        carrierLabel.setText(carrier);

        if (status == NetworkStatus.NO_SERVICE) {
            signalIcon.showNoService();
        }
    }
}
```

### 6.3 エラーハンドリング

```java
try {
    networkAdapter.request(url, "GET").get();
} catch (NetworkException e) {
    switch (e.getErrorType()) {
        case NO_SERVICE:
            // 圏外
            showNoServicePage();
            break;
        case TIMEOUT:
            // タイムアウト
            showRetryDialog();
            break;
        case UNKNOWN_HOST:
            // 不明なホスト
            showNotFoundPage();
            break;
        case PROTOCOL_ERROR:
            // プロトコルエラー
            showErrorPage(e.getMessage());
            break;
        default:
            showGenericError();
    }
}
```

### 6.4 VirtualAdapterの直接使用

IPvM専用の機能を使用する場合：

```java
VirtualAdapter virtualAdapter = kernel.getNetworkAdapter().getVirtualAdapter();

// IPvMアドレスへ直接リクエスト
virtualAdapter.httpRequest("3-sys-google", "/search", "GET")
    .thenAccept(response -> {
        // VirtualSocket.VirtualHttpResponse として受け取る
        String html = response.getBody();
    });

// パケット送信
VirtualPacket packet = VirtualPacket.builder()
    .source(IPvMAddress.forPlayer(playerUUID))
    .destination(IPvMAddress.forSystem("chat"))
    .type(VirtualPacket.PacketType.GENERIC_REQUEST)
    .put("message", "Hello!")
    .build();

virtualAdapter.sendPacket(packet);
```

---

## 7. 外部MOD連携

### 7.1 概要

外部MODは以下の方法でMochiMobileOSネットワークに参加できます：

1. **VirtualSocketの実装** - 新しい通信基盤を追加
2. **VirtualRouterへのサーバー登録** - 仮想サーバーを提供
3. **パケットハンドラーの登録** - カスタムパケット処理

### 7.2 仮想サーバーの実装

```java
// VirtualRouterにサーバーを登録
VirtualRouter router = kernel.getVirtualRouter();

// IPvMアドレス: 2-mymod-chat
router.registerServer("2-mymod-chat", new VirtualServerHandler() {
    @Override
    public void handleRequest(VirtualPacket request, ResponseCallback callback) {
        String path = request.getString("path");

        if ("/messages".equals(path)) {
            // メッセージ一覧を返す
            String html = generateMessagesHtml();
            callback.respond(VirtualSocket.VirtualHttpResponse.ok(html));
        } else {
            callback.respond(VirtualSocket.VirtualHttpResponse.notFound("Page not found"));
        }
    }
});
```

### 7.3 新しい通信基盤の追加

GTA V MODの例：

```java
public class GTAVirtualSocket implements VirtualSocket {

    private final ScriptHookInterface scriptHook;

    @Override
    public boolean isAvailable() {
        // GTA V内のネットワーク接続状態を確認
        return scriptHook.isOnline();
    }

    @Override
    public int getSignalStrength() {
        // GTA V内の位置に基づいて電波強度を計算
        Vector3 pos = scriptHook.getPlayerPosition();
        // 山岳部では弱い、都市部では強い等
        return calculateSignalFromPosition(pos);
    }

    @Override
    public String getCarrierName() {
        return "LS Mobile";  // Los Santos Mobile
    }

    // その他のメソッド実装...
}
```

### 7.4 パケットタイプの拡張

```java
// カスタムパケットタイプのハンドラーを登録
VirtualRouter router = kernel.getVirtualRouter();

router.registerTypeHandler(VirtualPacket.PacketType.CUSTOM, packet -> {
    String customType = packet.getString("customType");

    if ("MY_MOD_DATA".equals(customType)) {
        // カスタムパケットを処理
        processMyModData(packet);
    }
});
```

### 7.5 ベストプラクティス

1. **一意なアドレス空間を使用**
   - サーバーアドレスには`2-[modid]-[service]`形式を使用
   - 例: `2-mymod-chat`, `2-mymod-shop`

2. **エラーハンドリング**
   - `NetworkException`を適切にスロー
   - タイムアウトを設定（推奨: 10秒）

3. **非同期処理**
   - すべてのネットワーク操作は`CompletableFuture`で非同期に
   - UIスレッドをブロックしない

4. **リソース管理**
   - `close()`メソッドでリソースを適切に解放
   - 未完了のリクエストをキャンセル

---

## 付録

### A. パッケージ構成

```
jp.moyashi.phoneos.core.service.network/
├── NetworkAdapter.java          # 統一API
├── VirtualAdapter.java          # IPvM用アダプター
├── RealAdapter.java             # IPv4/6用アダプター
├── VirtualSocket.java           # 外部実装用インターフェース
├── NetworkStatus.java           # 接続状態enum
├── NetworkException.java        # ネットワーク例外
├── IPvMAddress.java             # IPvMアドレス
├── VirtualPacket.java           # 仮想パケット
└── VirtualRouter.java           # パケットルーター

jp.moyashi.phoneos.forge.network/
├── ForgeVirtualSocket.java      # Forge用実装
├── ForgeNetworkInitializer.java # 初期化処理
└── NetworkHandler.java          # パケット送受信

jp.moyashi.phoneos.standalone.network/
├── StandaloneVirtualSocket.java # Standalone用実装
└── StandaloneNetworkInitializer.java
```

### B. エラーコード一覧

| HTTPステータス | 説明 | 対処法 |
|--------------|------|-------|
| 200 | OK | 正常 |
| 400 | Bad Request | リクエスト形式を確認 |
| 404 | Not Found | URLを確認 |
| 500 | Internal Server Error | サーバーログを確認 |
| 503 | Service Unavailable | ネットワーク接続を確認（圏外） |
| 504 | Gateway Timeout | 再試行 |

### C. 変更履歴

| バージョン | 日付 | 変更内容 |
|-----------|------|---------|
| 2.0 | 2024年 | 依存性逆転パターン導入、VirtualSocketインターフェース追加 |
| 1.0 | - | 初版 |

---

*本ドキュメントはMochiMobileOS開発チームにより作成されました。*
