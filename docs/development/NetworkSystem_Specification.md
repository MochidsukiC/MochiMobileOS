# MochiMobileOS ネットワークシステム仕様書

**バージョン:** 2.1
**最終更新:** 2025年12月17日
**対象:** アプリケーション開発者、外部MOD開発者

---

## 目次

1. [概要](#1-概要)
2. [IPvMアドレス体系](#2-ipvmアドレス体系)
3. [NetworkAdapter API](#3-networkadapter-api)
4. [VirtualSocketインターフェース](#4-virtualsocketインターフェース)
5. [サーバーサイドアーキテクチャ](#5-サーバーサイドアーキテクチャ)
6. [ゲーム内仮想インターネット回線](#6-ゲーム内仮想インターネット回線)
7. [アプリ開発者向けガイド](#7-アプリ開発者向けガイド)
8. [外部MOD連携](#8-外部mod連携)

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
│   └─────────────┬───────────────────────────────────────┘   │
│                 │ (パケット通信)                             │
│                 ▼                                           │
│   ┌─────────────────────────────────────────────────────┐   │
│   │               MochiMobileOS Server                  │   │
│   │               (serverモジュール)                     │   │
│   │                                                     │   │
│   │   SystemServerRegistry: サーバーの登録・検索          │   │
│   │   VirtualHttpServer: リクエスト処理                  │   │
│   └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 設計原則

**依存性逆転の原則 (DIP)**

- Core層はインターフェース（`VirtualSocket`）のみを定義
- 実行モジュール（Forge/Standalone等）が実装を提供
- 新モジュール追加時にCore変更不要

**クライアント・サーバー分離**

- クライアント側（Core/Forge）は通信と表示に専念
- サーバー側（Serverモジュール）がリクエスト処理とレスポンス生成を担当

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

### 2.3 URL形式と表示スキーム

IPvMアドレスを使用したURLは以下の形式です。
ブラウザのアドレスバーには、ユーザーが認識しやすいよう `httpm://` スキームが表示されます。

```
httpm://[IPvMアドレス]/[パス]
```

例：
- `httpm://3-sys-google/search?q=minecraft`
- `httpm://2-mymod-chat/messages`

※ 内部的には `http://` または `data:` URLとして処理される場合がありますが、ユーザーインターフェース上は `httpm://` で統一されます。

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
Coreモジュールは具体的な通信手段を持たず、このインターフェースを通じてパケットを送受信します。

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

---

## 5. サーバーサイドアーキテクチャ

バージョン2.1より、IPvMリクエストを処理するための専用サーバーモジュール（`:server`）が導入されました。

### 5.1 MMOSServer

サーバーサイドのエントリーポイントです。

```java
package jp.moyashi.phoneos.server;

public class MMOSServer {
    // 初期化（サーバー起動時）
    public static void initialize();
    
    // パケット処理（クライアントからのリクエスト受信時）
    public static void handleHttpRequest(VirtualPacket packet, ResponseCallback callback);
    
    // システムサーバー登録
    public static void registerSystemServer(VirtualHttpServer server);
}
```

### 5.2 SystemServerRegistry

稼働中の仮想サーバーを管理するレジストリです。

- **Type 3 (SYSTEM)**: システム組み込みサーバー（例: `3-sys-test`）
- **Type 2 (SERVER)**: 外部MOD/アドオン提供サーバー

### 5.3 仮想サーバーの実装

`VirtualHttpServer` インターフェースを実装することで、独自のサーバーロジックを定義できます。

```java
public class MyServer implements VirtualHttpServer {
    @Override
    public String getServerId() {
        return "sys-myserver";
    }

    @Override
    public VirtualHttpResponse handleRequest(VirtualHttpRequest request) {
        if (request.getPath().equals("/hello")) {
            return VirtualHttpResponse.ok("Hello IPvM!");
        }
        return VirtualHttpResponse.notFound("Page not found");
    }
}
```

---

## 6. ゲーム内仮想インターネット回線

### 6.1 Minecraft Forge環境

Forge環境では、Minecraft ForgeのSimpleChannelを使用してパケット通信を行います。

#### 通信フロー

```
[クライアント (Phone)]
   ↓ HTTPリクエスト (Chrome/App)
   ↓
VirtualSocket (ForgeVirtualSocket)
   ↓ パケット送信 (NetworkHandler)
   ↓
[サーバー (Dedicated/Integrated)]
   ↓ パケット受信
   ↓
MMOSServer.handleHttpRequest()
   ↓ ルーティング
VirtualHttpServer (例: sys-google)
   ↓ レスポンス生成
   ↓
[クライアント (Phone)]
   ↓ パケット受信
   ↓
VirtualSocket (コールバック完了)
   ↓
ブラウザに表示
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

### 6.2 Standalone環境

Standalone環境（デスクトップアプリ）では、サーバーモジュールが同じプロセス内で動作し、すべての通信がローカルで処理されます。

- 電波強度: 常に5（最強）
- キャリア名: "Standalone Network"

---

## 7. アプリ開発者向けガイド

### 7.1 基本的な使用方法

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
            network.request("httpm://3-sys-myservice/api/data", "GET")
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
            // エラー処理
        }
    }
}
```

### 7.2 ネットワーク状態の監視

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

---

## 8. 外部MOD連携

### 8.1 概要

外部MODは以下の方法でMochiMobileOSネットワークに参加できます：

1. **VirtualHttpServerの実装** - サーバーサイドでコンテンツを提供
2. **VirtualSocketの実装** - 新しい通信基盤（GTA V, Rust連携等）を追加

### 8.2 仮想サーバーの実装（推奨）

`jp.moyashi.phoneos.server` モジュールを利用して、サーバーサイドでコンテンツを提供します。

```java
// サーバー初期化時に登録
MMOSServer.registerExternalServer(new MyModServer());

public class MyModServer implements VirtualHttpServer {
    @Override
    public String getServerId() {
        return "mod-mymod"; // アドレス: 2-mod-mymod
    }
    
    @Override
    public VirtualHttpResponse handleRequest(VirtualHttpRequest req) {
        // ...
    }
}
```

### 8.3 新しい通信基盤の追加

GTA V MODの例：

```java
public class GTAVirtualSocket implements VirtualSocket {

    private final ScriptHookInterface scriptHook;

    @Override
    public boolean isAvailable() {
        // GTA V内のネットワーク接続状態を確認
        return scriptHook.isOnline();
    }
    
    // ...
}
```

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
└── VirtualPacket.java           # 仮想パケット

jp.moyashi.phoneos.server/       # サーバーモジュール
├── MMOSServer.java              # サーバーエントリーポイント
├── SystemServerRegistry.java    # サーバーレジストリ
├── VirtualHttpServer.java       # 仮想サーバーIF
├── VirtualHttpRequest.java      # リクエストデータ
└── VirtualHttpResponse.java     # レスポンスデータ

jp.moyashi.phoneos.forge.network/
├── ForgeVirtualSocket.java      # Forge用実装
└── NetworkHandler.java          # パケット送受信

jp.moyashi.phoneos.standalone.network/
├── StandaloneVirtualSocket.java # Standalone用実装
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
| 2.1 | 2025/12/17 | サーバーモジュール仕様、httpm://スキームの追加 |
| 2.0 | 2024年 | 依存性逆転パターン導入、VirtualSocketインターフェース追加 |
| 1.0 | - | 初版 |

---

*本ドキュメントはMochiMobileOS開発チームにより作成されました。*