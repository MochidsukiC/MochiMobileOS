# Webアプリケーション実装 引継ぎ資料 (Handover Document)

## 現状のステータス (Status)

### 1. WebScreen 1x1 レンダリング問題 (Resolved)
- **状態**: 解決済み。
- **対処**: `ChromiumBrowser.java` でリフレクションを用いて `browser_rect_` を直接書き換え、`wasResized` を呼ぶことで解決。ログ上で 400x600 へのリサイズを確認。

### 2. 画面真っ白問題 (Pending / Critical)
- **症状**: 画面サイズは正しいが、内容が真っ白のまま。
- **調査結果**: 
    - `ChromiumBrowser` に追加したデバッグログにより、**`onPaint` イベントが一度も発火していない** ことが判明。
    - `CefBrowserOsr.java` (jcefmaven 135.0.20) のソースを確認したところ、`onPaint` メソッドの冒頭で `canvas_.getContext() == null` の場合、リスナーを呼ばずに早期リターンしている。
    - **結論**: `GLCanvas` (AWTコンポーネント) が正しく初期化（Realized）されておらず、OpenGLコンテキストが生成されていないため、描画が行われない。
- **ログ証拠**:
    - リサイズ、可視化設定、リスナー登録はすべて成功している。
    - しかし `onPaint` のログが一切出ない。
    - リトライ（リロード）を20回（2秒間）繰り返しても改善しない。

### 3. タブ共有・履歴汚染問題 (Pending)
- **症状**: `WebScreen` のタブが標準ブラウザからも見え、履歴に残る。
- **要望**: ユーザーより「プロセス毎にインスタンスを生成する堅牢なシステムアーキテクチャ」への変更指示あり。

## 実装指示 (Implementation Instructions)

### Task 1: 画面真っ白問題の解決 (GLCanvas初期化)
`core/src/main/java/jp/moyashi/phoneos/core/service/chromium/ChromiumBrowser.java`

`GLCanvas` の初期化を強制するための修正を行う。
現在の `Hidden JFrame` のセットアップロジック（`SwingUtilities.invokeLater` 内）を見直し、確実に `addNotify` が呼ばれ、コンテキストが作成されるようにする。

**試すべき修正案**:
1.  `hiddenFrame.setVisible(true)` のタイミング調整（`add` の後に行うなど）。
2.  `canvas.display()` を手動で呼び出して初期化を促す。
3.  `GLCanvas` が `null` でないことを確認し、`isReadyToRender()` が `true` になるまで待機するロジックの強化。

### Task 2: ChromiumService アーキテクチャの刷新 (AppID分離)
`core/src/main/java/jp/moyashi/phoneos/core/service/chromium/ChromiumService.java`
`core/src/main/java/jp/moyashi/phoneos/core/service/chromium/DefaultChromiumService.java`

アプリID（プロセスID相当）ごとにタブ管理を分離する。

1.  **インターフェース拡張**:
    ```java
    default ChromiumSurface createTab(String appId, int width, int height, String initialUrl) { ... }
    default Collection<ChromiumSurface> getSurfaces(String appId) { ... }
    ```
2.  **DefaultChromiumService 実装**:
    - `Map<String, List<ChromiumSurface>> appSurfaces` のような構造で、AppIDごとにリストを管理。
    - `createTab(appId, ...)` でそのリストに追加。
    - **履歴制御**: `appId` が `"system.browser"` の場合のみ `BrowserDataManager` に記録。
3.  **互換性維持**:
    - 引数なしの `getSurfaces()` は `"system.browser"` のリストを返す（標準ブラウザからはWebアプリのタブが見えないようにする）。

### Task 3: アプリ側の修正
1.  **`WebScreen.java`**:
    - `createTab` 呼び出し時に、自身の `modId` を `appId` として渡す。
    - リトライロジック（`isReadyToRender` チェック）の維持。
2.  **`ChromiumBrowserScreen.java`**:
    - `createTab`, `getSurfaces` 呼び出し時に `"system.browser"` を使用。
    - 起動時のクリーンアップ処理（前回追加したもの）は削除。

## ゴール
1.  `WebScreen` が正常に描画されること（GLContext問題の解決）。
2.  `WebScreen` と `ChromiumBrowserScreen` が互いに干渉せず、履歴も混ざらないこと。
