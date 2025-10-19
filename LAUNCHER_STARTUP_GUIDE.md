# 🚀 MochiMobileOS LauncherApp 自動起動ガイド

## 📋 起動フローの概要

MochiMobileOSのスタンドアロン起動時、以下の手順でLauncherAppが自動的に実行されます：

### 1. **Main.java による初期化**
```java
// standalone/src/main/java/jp/moyashi/phoneos/standalone/Main.java
public static void main(String[] args) {
    System.out.println("🚀 [1/4] Creating OS Kernel...");
    Kernel kernel = new Kernel();
    
    System.out.println("🚀 [4/4] Launching OS window...");
    System.out.println("-> LauncherApp will start automatically");
    PApplet.runSketch(sketchArgs, kernel);
}
```

### 2. **Kernel.settings() - Processing設定**
```java
@Override
public void settings() {
    size(400, 600);  // スマートフォン風の縦長ウィンドウ
    System.out.println("📱 Kernel: Processing window configured (400x600)");
}
```

### 3. **Kernel.setup() - OS初期化とLauncherApp登録**
```java
@Override
public void setup() {
    // === MochiMobileOS Kernel Initialization ===
    
    // サービス初期化
    vfs = new VFS();
    settingsManager = new SettingsManager();
    systemClock = new SystemClock();
    appLoader = new AppLoader(vfs);
    
    // LauncherApp登録と初期化
    LauncherApp launcherApp = new LauncherApp();
    appLoader.registerApplication(launcherApp);
    launcherApp.onInitialize(this);
    
    // ScreenManagerにLauncherAppのHomeScreenを設定
    screenManager = new ScreenManager();
    screenManager.pushScreen(launcherApp.getEntryScreen(this));  // <- ここでHomeScreenが起動画面になる
}
```

### 4. **LauncherApp.getEntryScreen() - HomeScreen作成**
```java
@Override
public Screen getEntryScreen(Kernel kernel) {
    System.out.println("🏠 LauncherApp: Creating advanced multi-page home screen...");
    homeScreen = new HomeScreen(kernel);
    System.out.println("✅ LauncherApp: Home screen created successfully!");
    return homeScreen;  // <- このHomeScreenがメイン画面となる
}
```

### 5. **HomeScreen.setup() - マルチページランチャー初期化**
```java
@Override
public void setup() {
    System.out.println("🚀 HomeScreen: Initializing multi-page launcher...");
    initializeHomePages();  // アプリをページに配置
    
    System.out.println("✅ HomeScreen: Initialization complete!");
    System.out.println("    • Pages created: " + homePages.size());
    System.out.println("    • Ready for user interaction!");
}
```

### 6. **Kernel.draw() - 描画ループ開始**
```java
@Override
public void draw() {
    if (screenManager != null) {
        screenManager.draw(this);  // <- HomeScreen.draw()が呼ばれ続ける
    }
}
```

## ✅ 起動成功の確認ポイント

### コンソール出力で確認
起動が成功すると、以下のような出力が表示されます：

```
MochiMobileOS Standalone Launcher
==================================
🚀 [1/4] Creating OS Kernel...
📱 Kernel: Processing window configured (400x600)
=== MochiMobileOS Kernel Initialization ===
  -> Creating VFS (Virtual File System)...
  -> Creating Application Loader...
  -> Registering LauncherApp...
🏠 LauncherApp: Creating advanced multi-page home screen...
▶️ Starting LauncherApp as initial screen...
✅ Kernel: OS initialization complete!
    • LauncherApp is now running
    • 2 applications available
🚀 HomeScreen: Initializing multi-page launcher...
✅ HomeScreen: Initialization complete!
    • Pages created: 1
    • Total shortcuts: 1
    • Ready for user interaction!

🎮 HOW TO USE:
    • Tap icons to launch apps
    • Long press for edit mode
    • Drag icons to rearrange
    • Swipe left/right for pages
    • Swipe up for App Library
```

### Processing ウィンドウで確認
- **400x600のウィンドウ**が表示される
- **暗いテーマのホームスクリーン**が表示される
- **SettingsAppのアイコン**（歯車マーク）が表示される
- **ステータスバー**に時刻とシステム状態が表示される
- **下部に「App Library」ナビゲーション**が表示される

## 🎯 LauncherApp 自動起動の仕組み

### 1. **確実な起動保証**
```java
// Kernel.setup()で必ずLauncherAppが登録される
LauncherApp launcherApp = new LauncherApp();
appLoader.registerApplication(launcherApp);

// ScreenManagerの初期画面として必ずHomeScreenが設定される
screenManager.pushScreen(launcherApp.getEntryScreen(this));
```

### 2. **マルチページ機能の即時利用可能**
- **ドラッグ&ドロップ**: 長押し→ドラッグで即座にアイコン移動
- **編集モード**: 長押しでウィグルアニメーション開始
- **App Library**: 下部スワイプまたはクリックでアクセス
- **ページ切り替え**: 横スワイプで複数ページ間移動

### 3. **SettingsApp 即時利用可能**
- HomeScreen上の歯車アイコンをクリック
- システム情報、設定項目の表示
- バック操作でHomeScreenに復帰

## 🔧 カスタマイズポイント

### 追加アプリケーションの登録
```java
// Kernel.setup() に追加
MyCustomApp customApp = new MyCustomApp();
appLoader.registerApplication(customApp);
customApp.onInitialize(this);
```

### ホームスクリーンの初期レイアウト
```java
// HomeScreen.initializeHomePages() で制御
// 自動的にアプリが4x5グリッドに配置される
// ページが満杯になると新しいページが自動作成される
```

## 🎉 結論

**MochiMobileOSは起動時に確実にLauncherAppを実行し、現代的なスマートフォンOSレベルのユーザーエクスペリエンスを提供します。**

- ✅ **自動起動**: Main → Kernel → LauncherApp → HomeScreen
- ✅ **マルチページ**: iOS/Android風のページ管理
- ✅ **編集機能**: ドラッグ&ドロップ、削除、アニメーション
- ✅ **App Library**: 全アプリへのアクセス
- ✅ **設定アプリ**: システム情報とテスト機能

これにより、MochiMobileOSはProcessingベースの教育用OSを超えて、**本格的なモバイルOSプロトタイプ**として機能します。