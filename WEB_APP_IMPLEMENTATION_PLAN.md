# Webアプリケーション開発支援キット 実装計画 (Web App Support Toolkit Plan)

## 概要 (Overview)
本計画書は、MochiMobileOS向けの外部アプリケーション開発において、HTML/CSS/JSを使用したUI構築を強力に支援するための **「高機能開発支援キット（Framework）」** の実装仕様を定義するものです。

**設計思想:**
*   **Developer Experience (DX) First**: 開発者は最低限の記述でHTML UIを利用できる。面倒なURL生成やストリーム管理はツールキットが自動で行う。
*   **Flexible Resource Loading**: `file://`, `http://` に加え、Jar内リソースやクラスパスからのスマートな読み込みをサポート。
*   **Automatic Scheme Management**: アプリ独自のURLスキーム定義を可能にしつつ、未定義や競合時にはシステムが安全なスキームを自動解決・発行する。

実装はClaude Code 4.5 Opusによって行われます。

## 提供する機能 (Features provided)

### 1. 高機能画面クラス: `WebScreen`
`jp.moyashi.phoneos.core.gui.WebScreen` (仮パッケージ)

HTML表示に必要な全機能をカプセル化した、`Screen` の拡張クラス。

**主な機能:**
*   **スマートファクトリ**:
    ```java
    // 例: 現在のクラスと同じパッケージにある "index.html" をロード
    return WebScreen.create(this, "index.html");
    
    // 例: 特定のModIDのアセットからロード
    return WebScreen.create("my_mod_id", "assets/ui/main.html");
    ```
*   **レンダリング統合**: Chromiumの描画バッファを `PGraphics` に効率的に転送。
*   **入力イベント転送**: マウス、キーボードイベントの自動ブリッジ。

### 2. インテリジェント・リソース・マネージャー
リソースの場所を特定し、Chromiumが読み込めるURLに変換するサブシステム。

*   **独自スキーム登録**:
    *   アプリは `SchemeRegistry.register("myscheme", myResourceProvider)` で独自スキームを定義可能。
    *   Chromium内では `myscheme://path/to/resource` でアクセス可能になる。
*   **自動解決 (Auto-Resolution)**:
    *   スキーム未指定の場合、システムは自動的に一意なスキーム（例: `app-1234://`）を発行し、リソースプロバイダと紐付ける。
    *   開発者はURLの重複や競合を気にする必要がない。

### 3. JavaScript Bridge (`JSBridge`)
Java-JS間の双方向通信を簡素化するアノテーションベースのシステム。

```java
// Java側
webScreen.registerApi("game", new Object() {
    @JavascriptInterface
    public void explode() { ... }
});
```

## 実装ステップ (Implementation Steps)

### Phase 1: コアコンポーネント実装
- [ ] **`WebScreen` クラス**: 基本的な描画・入力転送機能の実装。
- [ ] **`ResourceHandler` 基盤**: Chromiumの `CefResourceHandler` をラップし、Javaの `InputStream` からレスポンスを生成する共通クラスの実装。

### Phase 2: スキーム管理システム
- [ ] **`AppSchemeManager`**: スキームとリソースプロバイダのマッピング管理クラス。
- [ ] **自動解決ロジック**: スキーム重複時のフォールバック処理と、ランダム/連番によるユニークスキーム発行処理。

### Phase 3: スマートAPIの実装
- [ ] **ファクトリメソッド**: `WebScreen.create()` 等の便利メソッドの実装。
- [ ] **JSブリッジ**: アノテーション処理とメッセージルーターの統合。

## 開発者への影響
*   **マニフェスト**: 変更不要。
*   **利便性**: わずか1行でHTMLベースの画面を生成可能。
*   **柔軟性**: こだわりたい場合は独自スキームや詳細なロード設定も可能。