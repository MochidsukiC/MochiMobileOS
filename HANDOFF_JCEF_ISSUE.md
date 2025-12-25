# JCEF CefResourceHandler レスポンスボディ問題 - 引継ぎ資料

## 問題概要

MochiMobileOS (MMOS) の仮想ネットワーク機能で、`CefResourceHandler`を使用してカスタムHTTPレスポンスを返す際、**レスポンスボディがブラウザに到達しない**問題が発生しています。

### 症状
- サーバー側（Java）: 79バイトのJSONが正しく設定・返却されている
- クライアント側（JavaScript）: `fetch().then(res => res.json())` で「Unexpected end of JSON input」エラー
- つまり、レスポンスボディが空として認識されている

## 環境

- **プラットフォーム**: Minecraft Forge 1.20.1
- **JCEF**: java-cef（MCEFではない）
- **問題のクラス**: `VirtualNetworkResourceHandler` (CefResourceHandlerAdapter継承)

## 調査結果

### サーバー側ログ（正常に動作している）
```
[VirtualNetworkResourceHandler] processRequest() ENTER - url: http://2-piggleshop/api/status
[VirtualNetworkResourceHandler] sendHttpRequestSync: response received, isSuccess=true
[VirtualNetworkResourceHandler] Set success response (length: 79 bytes, mimeType: application/json)
[VirtualNetworkResourceHandler] sendHttpRequestSync: calling callback.Continue()
[VirtualNetworkResourceHandler] sendHttpRequestSync: callback.Continue() returned
[VirtualNetworkResourceHandler] getResponseHeaders called - status: 200, mimeType: application/json, responseData: 79 bytes
[VirtualNetworkResourceHandler] Headers set via setHeaderByName: CORS + Content-Length=79
[VirtualNetworkResourceHandler] Set responseLength to 79
[VirtualNetworkResourceHandler] readResponse called - bytesToRead: 65536, readPosition: 0, responseData: 79 bytes
[VirtualNetworkResourceHandler] readResponse: copied 79 bytes, remaining: 0
```

### クライアント側ログ（エラー）
```
[ChromiumBrowser] Console message received: [MochiOS:Selection]Error: Unexpected end of JSON input
```

## 試した解決策（すべて失敗）

1. **非同期→同期処理に変更**: `CompletableFuture.thenAccept()` → `future.get()` で同期待機
2. **CORSヘッダー追加**: `Access-Control-Allow-Origin: *` 等
3. **Content-Lengthヘッダー追加**
4. **setHeaderMap → setHeaderByName に変更**

## 関連ファイル

### 1. VirtualNetworkResourceHandler.java
パス: `core/src/main/java/jp/moyashi/phoneos/core/service/chromium/interceptor/VirtualNetworkResourceHandler.java`

主要メソッド:
- `processRequest()`: リクエスト処理、`callback.Continue()`を呼ぶ
- `getResponseHeaders()`: ステータス、MIMEタイプ、ヘッダー、responseLengthを設定
- `readResponse()`: レスポンスボディをバッファにコピー

```java
@Override
public boolean readResponse(byte[] dataOut, int bytesToRead, IntRef bytesRead, CefCallback callback) {
    if (responseData == null || readPosition >= responseData.length) {
        bytesRead.set(0);
        return false;
    }

    int remainingBytes = responseData.length - readPosition;
    int bytesToCopy = Math.min(bytesToRead, remainingBytes);

    System.arraycopy(responseData, readPosition, dataOut, 0, bytesToCopy);
    readPosition += bytesToCopy;
    bytesRead.set(bytesToCopy);

    return readPosition < responseData.length;
}
```

### 2. IPvMSchemeHandlerFactory.java
パス: `core/src/main/java/jp/moyashi/phoneos/core/service/chromium/interceptor/IPvMSchemeHandlerFactory.java`

`CefSchemeHandlerFactory`を実装し、`http://X-serverId/path`形式のURLに対して`VirtualNetworkResourceHandler`を返す。

### 3. ChromiumAppHandler.java
パス: `forge/src/main/java/jp/moyashi/phoneos/forge/chromium/ChromiumAppHandler.java`

`CefApp.addCustomScheme()`や`registerSchemeHandlerFactory()`でスキームハンドラを登録。

## アーキテクチャ

```
[JavaScript fetch()]
       ↓
[JCEF Browser]
       ↓
[IPvMSchemeHandlerFactory.create()]  ← http://2-piggleshop/api/status を検出
       ↓
[VirtualNetworkResourceHandler]
   ├── processRequest() → 仮想ネットワークからデータ取得 → callback.Continue()
   ├── getResponseHeaders() → status=200, mimeType, responseLength=79
   └── readResponse() → 79バイトをdataOutにコピー → return false
       ↓
[JCEF Browser] ← ここでボディが消失している？
       ↓
[JavaScript] → "Unexpected end of JSON input"
```

## 疑わしい点

1. **readResponse()の戻り値**:
   - `return false` で「これ以上データなし」を示している
   - JCEFでは別の方法が必要？

2. **スレッドの問題**:
   - `processRequest()`はCEFのIOスレッドで呼ばれる
   - `callback.Continue()`後に`getResponseHeaders()`と`readResponse()`が呼ばれるはずだが、別スレッドの可能性

3. **ハンドラのライフサイクル**:
   - ハンドラインスタンスがGCで回収されている可能性
   - `responseData`フィールドが読み取り時にnullになっている？

4. **setHeaderByName()の問題**:
   - ヘッダー設定が正しく適用されていない可能性

## 確認すべき項目

1. JCEFの`CefResourceHandler`の正しい実装方法
2. `readResponse()`の戻り値の意味（true/false）
3. CEFのスレッドモデルとコールバックの関係
4. 類似の実装例（JCEFでカスタムリソースハンドラを使用している例）

## 参考: 正常に動作している他のハンドラ

以下のハンドラは正常に動作している。比較分析が有効かもしれない：

1. **AppAssetSchemeHandler.java** (`mochiapp://`スキーム用)
   - パス: `core/src/main/java/jp/moyashi/phoneos/core/service/chromium/webapp/AppAssetSchemeHandler.java`
   - HTMLやCSSなどの静的アセットを返す

2. **HttpmSchemeHandler.java** (`httpm://`スキーム用)
   - パス: `core/src/main/java/jp/moyashi/phoneos/core/service/chromium/httpm/HttpmSchemeHandler.java`

これらと`VirtualNetworkResourceHandler`の実装を比較することで、何が違うか特定できる可能性がある。

## 再現手順

1. PiggleShopアプリを起動
2. 「Refresh Status」ボタンをクリック
3. JavaScript consoleに「Error: Unexpected end of JSON input」が表示される
4. サーバー側ログには79バイトが正常にコピーされたと表示される
