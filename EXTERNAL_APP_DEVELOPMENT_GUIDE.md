# MochiMobileOS 外部アプリケーション開発ガイド

**最終更新日: 2026年1月21日**

MochiMobileOS向けのアプリケーションを開発するための公式ガイドです。
MochiMobileOSアプリはJavaで記述され、**Native UI (Processing)** または **Web技術 (HTML/CSS/JS)** を使用してUIを構築できます。

**推奨される開発方法**:
**Native UI (Java/Processing)** による開発を強く推奨します。OSの機能を最大限に活用でき、動作も最も安定しています。Web技術による開発も可能ですが、パフォーマンスや一部APIの制約を受ける場合があります。

## 目次

1. [開発の前提条件](#1-開発の前提条件)
2. [クイックスタート (Nativeアプリ)](#2-クイックスタート-nativeアプリ)
3. [アプリケーションの基本構造](#3-アプリケーションの基本構造)
4. [Nativeアプリ開発 (推奨)](#4-nativeアプリ開発-推奨)
5. [Webアプリ開発](#5-webアプリ開発)
6. [システムAPIリファレンス](#6-システムapiリファレンス)
7. [コントロールセンターカード開発](#7-コントロールセンターカード開発)
8. [ダッシュボードウィジェット開発](#8-ダッシュボードウィジェット開発)
9. [Minecraft Forge MODとしての開発](#9-minecraft-forge-modとしての開発)
10. [サーバーサイドアプリケーション開発](#10-サーバーサイドアプリケーション開発)
11. [バックグラウンドサービス開発](#11-バックグラウンドサービス開発)

---

## 1. 開発の前提条件

*   **JDK 17以上**: プロジェクトはJava 17以降を使用します。
*   **Gradle**: ビルドツールとしてGradle (Kotlin DSL推奨) を使用します。
*   **IDE**: IntelliJ IDEA または Eclipse推奨。

---

## 2. クイックスタート (Nativeアプリ)

最も推奨される、JavaとProcessingを使用したネイティブアプリケーションの作成手順です。

### ステップ 1: プロジェクトのセットアップ

`build.gradle.kts` に以下の依存関係を追加します。

```kotlin
plugins {
    id("java")
}

group = "com.example"
version = "1.0.0"

repositories {
    mavenCentral()
    // MochiMobileOSリポジトリ (GitHub Packages等、環境に合わせて設定)
    maven { url = uri("https://jitpack.io") } 
}

dependencies {
    // MochiMobileOS Coreライブラリ
    implementation("jp.moyashi.phoneos:core:1.0.0") 
    
    // Processing Core (描画用)
    implementation("org.processing:core:4.3")
}
```

### ステップ 2: アプリケーションクラスの作成

`src/main/java/com/example/myapp/MyApp.java` を作成します。
`IApplication` インターフェースを実装し、`getEntryScreen` でメイン画面のインスタンスを返します。

```java
package com.example.myapp;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PImage;

public class MyApp implements IApplication {

    @Override
    public String getApplicationId() {
        return "com.example.myapp";
    }

    @Override
    public String getApplicationName() {
        return "My First App";
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        return new MyScreen();
    }

    @Override
    public PImage getIcon() {
        // アイコン画像をロード (任意)
        // return new PImage(getClass().getResourceAsStream("/assets/myapp/icon.png"));
        return null; // nullの場合、デフォルトアイコンが生成されます
    }

    @Override
    public void onInstall() {
        System.out.println("MyApp installed!");
    }
}
```

### ステップ 3: 画面クラスの作成

`src/main/java/com/example/myapp/MyScreen.java` を作成します。
標準コンポーネントライブラリを使用すると、簡単に美しいUIを作成できます。

```java
package com.example.myapp;

import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.components.Button;
import jp.moyashi.phoneos.core.ui.components.Label;
import jp.moyashi.phoneos.core.ui.components.LinearLayout;
import processing.core.PGraphics;

public class MyScreen implements Screen {

    private LinearLayout layout;
    private Label label;
    private Button button;
    private int count = 0;

    @Override
    public void setup(PGraphics pg) {
        // レイアウトコンテナの作成
        layout = new LinearLayout(0, 0, pg.width, pg.height, LinearLayout.Orientation.VERTICAL);
        layout.setPadding(20);
        layout.setSpacing(15);
        layout.setAlignment(LinearLayout.Alignment.CENTER); // 中央揃え

        // ラベルの作成
        label = new Label("Hello, MochiOS!");
        layout.addChild(label);

        // ボタンの作成
        button = new Button("Click Me");
        button.setSize(200, 50);
        button.setOnClickListener(() -> {
            count++;
            label.setText("Clicked: " + count);
            System.out.println("Button clicked!");
        });
        layout.addChild(button);
        
        // レイアウト計算
        layout.layout();
    }

    @Override
    public void draw(PGraphics pg) {
        pg.background(240); // 背景色
        layout.draw(pg);    // UI描画
    }

    @Override
    public void mousePressed(PGraphics pg, int x, int y) {
        layout.mousePressed(pg, x, y); // イベント転送
    }
    
    @Override
    public void mouseReleased(PGraphics pg, int x, int y) {
        layout.mouseReleased(pg, x, y);
    }
    
    @Override
    public void mouseMoved(PGraphics pg, int x, int y) {
        layout.mouseMoved(pg, x, y);
    }

    @Override
    public String getScreenTitle() {
        return "My App";
    }
}
```

### ステップ 4: ビルドと配置

1.  `./gradlew jar` でJARファイルをビルドします。
2.  生成された JAR ファイルを、MochiMobileOS の `mochi_os_data/apps/` ディレクトリに配置します。
3.  OSを起動（または再起動）すると、アプリ一覧に "My First App" が表示されます。

---

## 3. アプリケーションの基本構造

### IApplication インターフェース

すべてのアプリは `jp.moyashi.phoneos.core.app.IApplication` を実装する必要があります。

| メソッド | 説明 |
| :--- | :--- |
| `getApplicationId()` | アプリの一意なID（逆ドメイン推奨）。 |
| `getApplicationName()` | ランチャーに表示される名前。 |
| `getEntryScreen(Kernel)` | アプリ起動時に表示される最初の `Screen` を返す。 |
| `getIcon()` | アプリアイコン (`PImage`)。null可。 |
| `onInstall()` | アプリインストール時（初回ロード時）に呼ばれる。 |
| `onDispose()` | アプリ終了時に呼ばれる（リソース解放用）。 |
| `hasBackgroundService()` | バックグラウンドサービスを持つ場合trueを返す（デフォルト: false）。 |
| `getBackgroundService(Kernel)` | バックグラウンドサービス用の `Screen` を返す（デフォルト: null）。 |

### Screen インターフェース

UI画面は `jp.moyashi.phoneos.core.ui.Screen` を実装します。
`setup` で初期化し、`draw` で描画、各種入力イベントメソッドでインタラクションを処理します。

---

## 4. Nativeアプリ開発 (推奨)

Processing (PGraphics) を使用して、JavaコードでUIを描画します。
MochiMobileOSの標準コンポーネントライブラリを使用することで、OSのテーマに統合された高パフォーマンスなUIを構築できます。

### GUIコンポーネントライブラリ

`jp.moyashi.phoneos.core.ui.components` パッケージに標準UI部品が含まれています。

*   **入力**: `Button`, `TextField`, `TextArea`, `Checkbox`, `Switch`, `Slider`
*   **表示**: `Label`, `ImageView`, `ProgressBar`
*   **レイアウト**: `LinearLayout`, `Panel`, `ScrollView`, `ListView`, `Dialog`

これらのコンポーネントはすべて `UIComponent` を継承しており、イベント処理や描画の共通インターフェースを持っています。

### テーマへの対応

`ThemeEngine` を使用することで、ライトモード/ダークモードやアクセントカラーに自動対応できます。

```java
import jp.moyashi.phoneos.core.ui.theme.ThemeEngine;
import jp.moyashi.phoneos.core.ui.theme.ThemeContext;

// 描画内で
ThemeEngine theme = ThemeContext.getTheme();
pg.fill(theme.colorPrimary()); // アクセントカラー
pg.rect(0, 0, 100, 100);
```

標準コンポーネントを使用していれば、自動的にテーマが適用されます。

**テーマカラーのオーバーライド**:
バージョンアップにより、標準コンポーネントのデフォルトテーマ色を個別に上書きできるようになりました。例えば、特定のボタンだけ赤色にしたい場合は、以下のように設定します。

```java
Button deleteButton = new Button("削除");
deleteButton.setBackgroundColor(0xFFFF0000); // 赤色に設定
// このボタンはテーマ変更の影響を受けず、赤色のまま維持されます
```

**拡張されたコンポーネント機能**:
- **Dropdown**: `setEditable(true)` を呼び出すことで、テキスト入力による選択肢のフィルタリング（絞り込み）が可能になりました。

---

## 5. Webアプリ開発

Web技術 (HTML/CSS/JS) を使用してアプリを開発することも可能です。既存のWeb資産を活用したい場合に適しています。
Chromiumベースのエンジンを使用しているため、最新のWeb標準が利用可能です。

### WebScreen クラス

`WebScreen` は、JAR内のリソースファイルを `mochiapp://` スキームとしてロードし、レンダリングします。

```java
public class MyWebApp implements IApplication {
    // ...
    @Override
    public Screen getEntryScreen(Kernel kernel) {
        // assets/myapp/ui/index.html をロード
        // "myapp" はパッケージ名から推測されるModID、または明示的に指定可能
        return WebScreen.create(this, "ui/index.html");
    }
}
```

*   **リソースパス**: `src/main/resources/assets/[modid]/` 以下に配置されたファイルを指定します。
*   **Mod IDの自動推測**: `WebScreen.create(this, ...)` を呼ぶと、呼び出し元クラスのパッケージ名からMod IDを推測します（例: `com.example.myapp.Main` -> `myapp`）。

### 機能と制約

*   **JavaScript**: フルサポート。
*   **LocalStorage**: サポート（アプリごとに分離）。
*   **通信**: `XMLHttpRequest` / `fetch` は、CORSの制限を受ける場合があります。
*   **モバイル最適化**: User-AgentはAndroidとして設定され、Viewportの設定が自動的に適用されます。
*   **注意**: Nativeアプリに比べ、メモリ使用量が多くなる傾向があります。また、Minecraft Forge環境下では描画の仕組み上、わずかな遅延が発生する可能性があります。

---

## 6. システムAPIリファレンス

アプリからOSの機能を利用するには、`Kernel` インスタンスを経由して各マネージャーにアクセスします。
Nativeアプリ、Webアプリ（将来拡張予定）共に利用可能です。

### VFS (仮想ファイルシステム)

アプリ固有のデータ保存や、システム設定へのアクセスを行います。

```java
// 書き込み
kernel.getVFS().writeString("apps/myapp/data.json", "{...}");

// 読み込み
String data = kernel.getVFS().readString("apps/myapp/data.json");
```

### ネットワーク (NetworkAdapter)

実インターネットおよびゲーム内仮想ネットワーク(IPvM)へのアクセス。

```java
kernel.getNetworkAdapter().request("https://api.example.com/data", "GET")
    .thenAccept(response -> {
        System.out.println(response.getBody());
    });
```

### Intent / Activity

他のアプリの機能を呼び出したり、画面遷移を行います。

```java
// URLをブラウザで開く
Intent intent = new Intent(Intent.ACTION_VIEW, "https://google.com");
kernel.getActivityManager().startActivity(intent);
```

### センサー (SensorManager)

GPS、バッテリー、加速度などの情報取得。

```java
Sensor battery = kernel.getSensorManager().getDefaultSensor(Sensor.TYPE_BATTERY);
kernel.getSensorManager().registerListener(event -> {
    System.out.println("Battery Level: " + event.values[0] + "%");
}, battery, SensorManager.SENSOR_DELAY_NORMAL);
```

### クリップボード (ClipboardManager)

```java
// コピー
kernel.getClipboardManager().setText("Hello");

// ペースト
String text = kernel.getClipboardManager().getText();
```

---

## 7. コントロールセンターカード開発

外部アプリケーションは、コントロールセンター（ダッシュボード）にカードを追加できます。
WiFiやBluetoothのトグル、メディアプレーヤーウィジェットのように、クイックアクセス機能を提供できます。

### カードの種類

MochiMobileOSは以下の2種類のカードサイズをサポートしています。

| サイズ | 寸法 | 用途 |
| :--- | :--- | :--- |
| **1×1 (スクエア)** | columnSpan=1, rowSpan=1 | トグルボタン、簡易表示 |
| **2×2 (ラージ)** | columnSpan=2, rowSpan=2 | メディアプレーヤー、詳細ウィジェット |

### セクション

カードは以下のセクションに配置できます。

| セクション | 説明 |
| :--- | :--- |
| `QUICK_SETTINGS` | WiFi、Bluetooth、機内モードなどの基本設定 |
| `MEDIA` | NowPlaying、音量スライダー |
| `DISPLAY` | 輝度、ダークモード、回転ロック |
| `APP_WIDGETS` | 外部アプリ提供のカード（デフォルト） |

### カードの実装

`IAppControlCenterItem` インターフェースを実装します。

```java
package com.example.myapp;

import jp.moyashi.phoneos.core.controls.IAppControlCenterItem;
import jp.moyashi.phoneos.core.controls.ControlCenterSection;
import processing.core.PGraphics;

/**
 * 1×1 トグルカード例
 */
public class MyToggleCard implements IAppControlCenterItem {

    private boolean enabled = false;

    @Override
    public String getId() {
        return "com.example.myapp.toggle";
    }

    @Override
    public String getOwnerApplicationId() {
        return "com.example.myapp";
    }

    @Override
    public ControlCenterSection getSection() {
        return ControlCenterSection.APP_WIDGETS; // または QUICK_SETTINGS など
    }

    @Override
    public int getDefaultOrder() {
        return 10; // 小さい値ほど先頭に表示
    }

    @Override
    public int getColumnSpan() {
        return 1; // 1×1サイズ
    }

    @Override
    public int getRowSpan() {
        return 1;
    }

    @Override
    public void draw(PGraphics pg, float x, float y, float w, float h, boolean pressed) {
        // 背景
        pg.fill(enabled ? 0xFF4CAF50 : 0xFF424242);
        pg.noStroke();
        pg.rect(x, y, w, h, 12);

        // アイコン・テキスト
        pg.fill(255);
        pg.textAlign(pg.CENTER, pg.CENTER);
        pg.textSize(14);
        pg.text(enabled ? "ON" : "OFF", x + w/2, y + h/2);
    }

    @Override
    public void onClick() {
        enabled = !enabled;
        System.out.println("Toggle: " + enabled);
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
```

### 2×2 ラージカード例

```java
/**
 * 2×2 ウィジェットカード例
 */
public class MyLargeWidget implements IAppControlCenterItem {

    @Override
    public String getId() {
        return "com.example.myapp.widget";
    }

    @Override
    public String getOwnerApplicationId() {
        return "com.example.myapp";
    }

    @Override
    public int getColumnSpan() {
        return 2; // 2×2サイズ
    }

    @Override
    public int getRowSpan() {
        return 2;
    }

    @Override
    public void draw(PGraphics pg, float x, float y, float w, float h, boolean pressed) {
        // 背景
        pg.fill(0xFF1E1E1E);
        pg.noStroke();
        pg.rect(x, y, w, h, 16);

        // コンテンツ描画
        pg.fill(255);
        pg.textAlign(pg.CENTER, pg.CENTER);
        pg.textSize(18);
        pg.text("My Widget", x + w/2, y + h/2);
    }

    @Override
    public void onClick() {
        // ウィジェットタップ時の処理
    }

    @Override
    public boolean isVisible() {
        return true;
    }
}
```

### カードの登録

アプリの初期化時に `ControlCenterCardRegistry` を通じてカードを登録します。

```java
public class MyApp implements IApplication {

    @Override
    public void onInitialize(Kernel kernel) {
        // コントロールセンターカードを登録
        ControlCenterCardRegistry registry = kernel.getControlCenterCardRegistry();

        if (registry != null) {
            registry.registerCard(new MyToggleCard());
            registry.registerCard(new MyLargeWidget());
        }
    }

    @Override
    public void onDestroy() {
        // アプリ終了時、カードは自動的に削除されます
        // （ownerApplicationIdによる自動管理）
    }
}
```

### ユーザーによるカスタマイズ

登録されたカードは、ユーザーが設定アプリの「Control Center」から管理できます。
- カードの表示/非表示の切り替え
- 表示順序の変更（上下移動）
- セクションごとのカード配置確認

カードの配置設定はシステム設定に永続化され、アプリの再起動後も維持されます。

---

## 8. ダッシュボードウィジェット開発

ホーム画面の1ページ目にあるダッシュボードに表示されるウィジェットを開発できます。
iPhoneのウィジェットに似た機能で、アプリからウィジェットを提供し、ユーザーがスロットにどのウィジェットを配置するか選択できます。

### ウィジェットの種類

#### サイズ（2種類）

| サイズ | 寸法 | 対応スロット |
|--------|------|-------------|
| `FULL_WIDTH` | 360×50〜80px | 検索、下部 |
| `HALF_WIDTH` | 175×150px | 左、右 |

#### タイプ（2種類）

| タイプ | 説明 |
|--------|------|
| `DISPLAY` | 情報表示型。タップするとアプリが開く |
| `INTERACTIVE` | インタラクティブ型。ウィジェット内でボタンやテキスト入力などが可能 |

### 基本実装

`IDashboardWidget` インターフェースを実装してウィジェットを作成します。

```java
package com.example.myapp;

import jp.moyashi.phoneos.core.dashboard.*;
import processing.core.PGraphics;

public class MyInfoWidget implements IDashboardWidget {

    @Override
    public String getId() {
        return "com.example.myapp.info";  // 一意なウィジェットID
    }

    @Override
    public String getDisplayName() {
        return "マイウィジェット";
    }

    @Override
    public String getDescription() {
        return "アプリの情報を表示します";
    }

    @Override
    public DashboardWidgetSize getSize() {
        return DashboardWidgetSize.HALF_WIDTH;  // または FULL_WIDTH
    }

    @Override
    public DashboardWidgetType getType() {
        return DashboardWidgetType.DISPLAY;  // タップでアプリを開く
    }

    @Override
    public String getOwnerApplicationId() {
        return "com.example.myapp";  // 提供元アプリID
    }

    @Override
    public String getTargetApplicationId() {
        return "com.example.myapp";  // タップ時に開くアプリID
    }

    @Override
    public void draw(PGraphics g, float x, float y, float w, float h) {
        // ウィジェットの描画
        g.fill(0xFF1E88E5);
        g.rect(x, y, w, h, 12);
        g.fill(255);
        g.textAlign(g.CENTER, g.CENTER);
        g.text("My Widget", x + w/2, y + h/2);
    }
}
```

### インタラクティブウィジェットの実装

ウィジェット内でタッチイベントを処理し、引数付きでアプリを起動できます。

```java
public class MyInteractiveWidget implements IDashboardWidget {
    private Kernel kernel;

    @Override
    public DashboardWidgetSize getSize() {
        return DashboardWidgetSize.FULL_WIDTH;
    }

    @Override
    public DashboardWidgetType getType() {
        return DashboardWidgetType.INTERACTIVE;
    }

    @Override
    public void onAttach(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void draw(PGraphics g, float x, float y, float w, float h) {
        // ウィジェット本体とボタンを描画
        g.fill(0xFF333333);
        g.rect(x, y, w, h, 8);

        // ボタン領域
        g.fill(0xFF4CAF50);
        g.rect(x + w - 60, y + 10, 50, 30, 4);
        g.fill(255);
        g.text("開く", x + w - 35, y + 25);
    }

    @Override
    public boolean onTouch(float localX, float localY, int action) {
        if (action == ACTION_TAP) {
            // ボタン領域のタップ判定
            float buttonX = 360 - 60;  // ウィジェット幅に応じて調整
            if (localX >= buttonX && localX < buttonX + 50 &&
                localY >= 10 && localY < 40) {
                // 引数付きでアプリを開く
                if (kernel != null) {
                    var app = kernel.getAppLoader().findApplicationById("com.example.myapp");
                    if (app != null) {
                        // アプリ起動処理
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
```

### ウィジェットの登録

アプリの初期化時にウィジェットをレジストリに登録します。

```java
public class MyApp implements IApplication {

    @Override
    public void onInitialize(Kernel kernel) {
        DashboardWidgetRegistry registry = kernel.getDashboardWidgetRegistry();
        if (registry != null) {
            // ウィジェットを登録
            registry.registerWidget(new MyInfoWidget());
            registry.registerWidget(new MyInteractiveWidget());
        }
    }
}
```

### ユーザーによるカスタマイズ

登録されたウィジェットは、ユーザーが設定アプリの「Dashboard」から管理できます。
- 各スロットに割り当てるウィジェットの選択
- サイズが一致するウィジェットのみ選択可能
- 時計スロット（Slot 0）は変更不可

ウィジェットの配置設定はシステム設定に永続化され、アプリの再起動後も維持されます。

---

## 9. Minecraft Forge MODとしての開発

MochiMobileOSアプリをForge MODの一部として配布することも可能です。
InterModComms (IMC) を使用して、他のMODからアプリケーションを登録できます。

### セットアップ

`build.gradle.kts` にForge関連の設定を追加し、MochiMobileOSのForgeモジュールに依存させます。

```kotlin
dependencies {
    // MochiMobileOS Core（コンパイル時のみ）
    compileOnly("jp.moyashi.phoneos:core:1.0.0")

    // Forge環境で実行時に利用可能
    runtimeOnly(fg.deobf("jp.moyashi.phoneos:forge:1.0.0"))
}
```

### アプリケーションの登録 (IMC方式)

`InterModComms.sendTo()` を使用してアプリケーションを登録します。
この方式はForge標準のMod間通信メカニズムを使用するため、安定して動作します。

```java
package com.example.mymod;

import jp.moyashi.phoneos.core.app.IApplication;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod("mymod")
public class MyMod {

    public MyMod() {
        // MODイベントバスにリスナーを登録
        var modBus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onCommonSetup);
    }

    /**
     * FMLCommonSetupEvent でアプリケーションを登録
     */
    private void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // MochiMobileOSにアプリケーションを登録
            InterModComms.sendTo(
                "mochimobileos",           // 送信先MOD ID
                "register_app",            // メソッド名
                () -> new MyApp()          // IApplication インスタンスを返す Supplier
            );

            System.out.println("[MyMod] Registered MyApp with MochiMobileOS");
        });
    }
}
```

### 複数アプリの登録

1つのMODから複数のアプリケーションを登録できます。

```java
private void onCommonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
        // アプリ1を登録
        InterModComms.sendTo("mochimobileos", "register_app", () -> new MyApp1());

        // アプリ2を登録
        InterModComms.sendTo("mochimobileos", "register_app", () -> new MyApp2());

        // アプリ3を登録
        InterModComms.sendTo("mochimobileos", "register_app", () -> new MyApp3());
    });
}
```

### IMC登録の仕組み

```
FMLCommonSetupEvent (あなたのMOD)
    ↓
InterModComms.sendTo("mochimobileos", "register_app", ...)
    ↓
InterModProcessEvent (MochiMobileOS側)
    ↓
ModAppRegistry に登録
    ↓
AppStore / ホーム画面に表示
```

### 注意事項

- `InterModComms.sendTo()` は必ず `event.enqueueWork()` 内で呼び出してください
- Supplierを渡すため、アプリケーションのインスタンス化はMochiMobileOS側で行われます
- アプリケーションIDが重複する場合、自動的にナンバリングされます（例: `myapp_2`）

### 旧方式からの移行

以前の `PhoneAppRegistryEvent` 方式は非推奨です。以下のように移行してください：

```java
// 旧方式（非推奨・動作しません）
@SubscribeEvent
public static void onAppRegistry(PhoneAppRegistryEvent event) {
    event.registerApp(new MyApp());
}

// 新方式（IMC）
private void onCommonSetup(FMLCommonSetupEvent event) {
    event.enqueueWork(() -> {
        InterModComms.sendTo("mochimobileos", "register_app", () -> new MyApp());
    });
}
```

---

## 10. サーバーサイドアプリケーション開発

MochiMobileOSはクライアント-サーバー分離アーキテクチャを採用しており、Forge環境ではサーバーサイドでHTTPライクなリクエストを処理するアプリケーションを開発できます。
これにより、ゲーム内ブラウザ（Chromium）からアクセス可能な仮想Webサービスを提供できます。

### 概要

サーバーサイドアプリは `VirtualHttpServer` インターフェースを実装し、IPvMネットワーク経由でHTTPリクエストを処理します。

```
クライアント（ブラウザ）→ IPvMアドレス（例: http://3-sys-myapp/）
    → NetworkHandler → MMOSServer → VirtualHttpServer.handleRequest()
    → レスポンス返却
```

### VirtualHttpServer インターフェース

```java
package jp.moyashi.phoneos.server.network;

public interface VirtualHttpServer {

    /**
     * サーバーの識別子を取得する。
     * この値がIPvMアドレスの識別子部分になる。
     * 例: "myapp" → 3-sys-myapp でアクセス可能
     */
    String getServerId();

    /**
     * HTTPリクエストを処理する。
     */
    VirtualHttpResponse handleRequest(VirtualHttpRequest request);

    /**
     * サーバーの説明を取得する（デバッグ用、オプション）。
     */
    default String getDescription() {
        return "VirtualHttpServer: " + getServerId();
    }
}
```

### 基本実装例

```java
package com.example.myapp.server;

import jp.moyashi.phoneos.server.network.VirtualHttpServer;
import jp.moyashi.phoneos.server.network.VirtualHttpRequest;
import jp.moyashi.phoneos.server.network.VirtualHttpResponse;

public class MyAppServer implements VirtualHttpServer {

    @Override
    public String getServerId() {
        return "myapp";  // http://3-sys-myapp/ でアクセス可能
    }

    @Override
    public VirtualHttpResponse handleRequest(VirtualHttpRequest request) {
        String path = request.getPath();
        String method = request.getMethod();

        // ルーティング
        if (path.equals("/") || path.equals("/index.html")) {
            return handleIndex();
        } else if (path.equals("/api/data") && method.equals("GET")) {
            return handleApiData(request);
        } else if (path.equals("/api/submit") && method.equals("POST")) {
            return handleApiSubmit(request);
        }

        return VirtualHttpResponse.notFound();
    }

    private VirtualHttpResponse handleIndex() {
        String html = """
            <!DOCTYPE html>
            <html>
            <head>
                <title>My App Server</title>
            </head>
            <body>
                <h1>Welcome to My App!</h1>
                <p>This is a virtual HTTP server running in Minecraft.</p>
            </body>
            </html>
            """;
        return VirtualHttpResponse.html(html);
    }

    private VirtualHttpResponse handleApiData(VirtualHttpRequest request) {
        String json = "{\"status\": \"ok\", \"message\": \"Hello from server!\"}";
        return VirtualHttpResponse.json(json);
    }

    private VirtualHttpResponse handleApiSubmit(VirtualHttpRequest request) {
        String body = request.getBody();
        // POSTデータを処理...
        return VirtualHttpResponse.json("{\"received\": true}");
    }

    @Override
    public String getDescription() {
        return "My App Virtual HTTP Server";
    }
}
```

### VirtualHttpRequest API

| メソッド | 説明 |
|---------|------|
| `getMethod()` | HTTPメソッド（GET, POST, PUT, DELETE等） |
| `getPath()` | リクエストパス（例: `/api/users`） |
| `getHeaders()` | 全ヘッダーのMap |
| `getHeader(name)` | 特定のヘッダー値を取得 |
| `getBody()` | リクエストボディ（POST/PUT時） |
| `getSource()` | 送信元IPvMアドレス |
| `getDestination()` | 送信先IPvMアドレス |

### VirtualHttpResponse API

| メソッド | 説明 |
|---------|------|
| `VirtualHttpResponse.ok(body)` | 200 OK レスポンス |
| `VirtualHttpResponse.html(html)` | HTML レスポンス（Content-Type: text/html） |
| `VirtualHttpResponse.json(json)` | JSON レスポンス（Content-Type: application/json） |
| `VirtualHttpResponse.notFound()` | 404 Not Found レスポンス |
| `VirtualHttpResponse.error(message)` | 500 Internal Server Error レスポンス |
| `VirtualHttpResponse.builder()` | カスタムレスポンス用ビルダー |

### デプロイ方法

サーバーアプリをデプロイするには、以下の2つの方法があります。

#### 方法1: JARファイルによる動的ロード（推奨）

ServiceLoader（SPI）を使用して、JARファイルからサーバーアプリを動的にロードします。

**1. META-INF/services ファイルの作成**

`src/main/resources/META-INF/services/jp.moyashi.phoneos.server.network.VirtualHttpServer` ファイルを作成し、実装クラスのFQCNを記載します。

```
com.example.myapp.server.MyAppServer
```

**2. build.gradle.kts の設定**

```kotlin
plugins {
    id("java")
}

dependencies {
    // MochiMobileOS Server モジュール
    compileOnly("jp.moyashi.phoneos:server:1.0.0")
}

tasks.jar {
    // META-INF/services が含まれることを確認
    from(sourceSets.main.get().resources)
}
```

**3. JARの配置**

ビルドしたJARを以下のいずれかのディレクトリに配置します。

| ディレクトリ | 用途 |
|-------------|------|
| `mmos_server_data/server_apps/` | サーバー専用アプリ |
| `mmos_server_data/apps/` | クライアント+サーバー統合アプリ |

#### 統合JARの作成（IApplication + VirtualHttpServer）

1つのJARにクライアントアプリ（`IApplication`）とサーバーアプリ（`VirtualHttpServer`）の両方を含めることができます。

**プロジェクト構成例:**

```
my-app/
├── src/main/java/
│   └── com/example/myapp/
│       ├── MyApp.java              # IApplication 実装
│       ├── MyScreen.java           # クライアントUI
│       └── server/
│           └── MyAppServer.java    # VirtualHttpServer 実装
└── src/main/resources/
    └── META-INF/services/
        ├── jp.moyashi.phoneos.core.app.IApplication
        └── jp.moyashi.phoneos.server.network.VirtualHttpServer
```

**META-INF/services/jp.moyashi.phoneos.core.app.IApplication:**
```
com.example.myapp.MyApp
```

**META-INF/services/jp.moyashi.phoneos.server.network.VirtualHttpServer:**
```
com.example.myapp.server.MyAppServer
```

統合JARを `mmos_server_data/apps/` に配置すると、クライアント側では `IApplication` が、サーバー側では `VirtualHttpServer` が自動的に検出・登録されます。

#### 方法2: Forge MOD からの直接登録

Forge MODとして開発する場合、コードから直接サーバーを登録できます。

```java
import jp.moyashi.phoneos.server.MMOSServer;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.common.Mod;

@Mod("mymod")
public class MyMod {

    public MyMod() {
        // サーバーサイド初期化
        FMLLoader.getDist().ifServer(() -> {
            // FMLCommonSetupEvent でサーバーを登録
        });
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // システムサーバーとして登録（Type 3: 3-sys-xxx）
            MMOSServer.registerSystemServer("myapp", new MyAppServer());

            // または外部サーバーとして登録（Type 2: 2-xxx）
            // MMOSServer.registerExternalServer("myapp", new MyAppServer());
        });
    }
}
```

### IPvMアドレス体系

| タイプ | 形式 | 用途 |
|-------|------|------|
| Type 3 | `3-sys-{serverId}` | システム/組み込みサーバー |
| Type 2 | `2-{serverId}` | 外部MOD提供サーバー |

例:
- `http://3-sys-test/` - 組み込みテストサーバー
- `http://3-sys-myapp/` - `registerSystemServer("myapp", ...)` で登録
- `http://2-economy/` - `registerExternalServer("economy", ...)` で登録

### 注意事項

- サーバーアプリはForge環境の**サーバーサイド**でのみ実行されます
- スタンドアロン環境ではサーバーアプリは動作しません
- `mmos_server_data/` ディレクトリはゲームディレクトリ直下に作成されます
- サーバーアプリのクラスローダーはアプリのライフサイクル中保持されます

---

## 11. バックグラウンドサービス開発

アプリがフォアグラウンドにない時でも、定期的なデータ同期、通知の受信、ネットワーク監視などのバックグラウンド処理を行うためのサービスを開発できます。
メッセージングアプリ、音楽プレーヤー、同期サービスなど、継続的なバックグラウンド処理が必要なアプリに最適です。

### 概要

バックグラウンドサービスは通常のUIスクリーン（`getEntryScreen()`で返すもの）とは**別のScreenインスタンス**として実装します。
これにより、UIとバックグラウンド処理を明確に分離し、それぞれ独立したライフサイクルを持たせることができます。

```
┌─────────────────────────────────────────────────────────────┐
│ IApplication                                                │
├─────────────────────────────────────────────────────────────┤
│ getEntryScreen()        → LoginScreen (UI画面)              │
│ getBackgroundService()  → MyBackgroundService (BG処理)      │
└─────────────────────────────────────────────────────────────┘
```

### バックグラウンドサービスの実装

#### ステップ 1: バックグラウンドサービスScreenの作成

`Screen` インターフェースを実装し、`background()` と `backgroundInit()` メソッドを重点的に実装します。

```java
package com.example.myapp;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PGraphics;

/**
 * バックグラウンドで定期的な同期処理を行うサービス
 */
public class MyBackgroundService implements Screen {

    private final Kernel kernel;
    private long lastSyncTime = 0;
    private static final long SYNC_INTERVAL_MS = 10000; // 10秒ごとに同期

    public MyBackgroundService(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void setup(PGraphics pg) {
        // バックグラウンドサービスでは通常使用しない
    }

    @Override
    public void backgroundInit() {
        // バックグラウンドサービス初期化時に呼ばれる
        // ServiceManagerによる自動起動時に実行される
        System.out.println("[MyService] Background service initialized");

        // 初期同期を実行
        performSync();
    }

    @Override
    public void background() {
        // 定期的に呼び出されるバックグラウンド処理
        // フレームレートに基づいて呼び出される（約10フレームに1回）

        long now = System.currentTimeMillis();
        if (now - lastSyncTime >= SYNC_INTERVAL_MS) {
            lastSyncTime = now;
            performSync();
        }
    }

    @Override
    public void tick() {
        // フォアグラウンド時のtick処理
        // バックグラウンドサービスでは通常使用しない
    }

    /**
     * 同期処理を実行
     */
    private void performSync() {
        System.out.println("[MyService] Performing background sync...");

        // ネットワーク状態をチェック
        var socket = kernel.getMobileDataSocket();
        if (socket == null || !socket.isAvailable()) {
            System.out.println("[MyService] Network unavailable, skipping sync");
            return;
        }

        // 同期処理を実行（非同期）
        // 例: サーバーから新着データを取得
        fetchNewData()
            .thenAccept(data -> {
                if (data != null && !data.isEmpty()) {
                    // 通知を表示
                    showNotification(data);
                }
            })
            .exceptionally(e -> {
                System.err.println("[MyService] Sync failed: " + e.getMessage());
                return null;
            });
    }

    /**
     * 新着データを通知として表示
     */
    private void showNotification(String data) {
        var notificationManager = kernel.getNotificationManager();
        if (notificationManager != null) {
            notificationManager.addNotification(
                "com.example.myapp",        // アプリID
                "新着データ",                 // タイトル
                data,                        // 内容
                2                            // 優先度（1: 通常, 2: 高）
            );
        }
    }

    // その他のScreenメソッド（バックグラウンドサービスでは空実装）
    @Override
    public void draw(PGraphics pg) {
        // バックグラウンドサービスでは使用しない
    }

    @Override
    public void mousePressed(PGraphics pg, int x, int y) {}

    @Override
    public void mouseReleased(PGraphics pg, int x, int y) {}

    @Override
    public void cleanup(PGraphics pg) {
        System.out.println("[MyService] Background service cleanup");
    }

    @Override
    public String getScreenTitle() {
        return "My Background Service";
    }
}
```

#### ステップ 2: IApplicationでバックグラウンドサービスを提供

`hasBackgroundService()` と `getBackgroundService()` をオーバーライドします。

```java
package com.example.myapp;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PImage;

public class MyApp implements IApplication {

    private static final String APP_ID = "com.example.myapp";

    private Kernel kernel;
    private MyBackgroundService backgroundService;

    @Override
    public String getApplicationId() {
        return APP_ID;
    }

    @Override
    public String getName() {
        return "My App";
    }

    @Override
    public void onInitialize(Kernel kernel) {
        this.kernel = kernel;

        // バックグラウンドサービスとして登録（自動起動を有効化）
        kernel.getServiceManager().registerBackgroundService(APP_ID);
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        // UI画面を返す（通常のアプリ起動時）
        return new MyMainScreen(kernel);
    }

    @Override
    public boolean hasBackgroundService() {
        // バックグラウンドサービスを持つことを宣言
        return true;
    }

    @Override
    public Screen getBackgroundService(Kernel kernel) {
        // バックグラウンドサービス用のScreenを返す
        // シングルトンとして管理（複数インスタンスを防止）
        if (backgroundService == null) {
            backgroundService = new MyBackgroundService(kernel);
        }
        return backgroundService;
    }

    @Override
    public PImage getIcon() {
        return null;
    }
}
```

### Screen インターフェースのライフサイクルメソッド

バックグラウンドサービスに関連する主要なメソッド:

| メソッド | 呼び出しタイミング | 用途 |
|---------|-------------------|------|
| `backgroundInit()` | サービス初期化時（OS起動時に自動起動） | 初期設定、リソース準備 |
| `background()` | 定期的（約10フレームに1回） | 定期同期、ネットワーク監視 |
| `tick()` | フォアグラウンド時のみ | UI更新用（BGサービスでは不使用） |
| `onForeground()` | フォアグラウンドに遷移時 | UI表示準備 |
| `onBackground()` | バックグラウンドに遷移時 | リソース解放、状態保存 |
| `cleanup()` | サービス終了時 | リソースクリーンアップ |

### 自動起動の仕組み

1. アプリが `registerBackgroundService(appId)` を呼び出すと、自動起動リストに登録される
2. OS起動時に `ServiceManager.initialize()` が呼ばれる
3. 自動起動リストのアプリに対して `getBackgroundService()` が呼ばれる
4. 返されたScreenの `backgroundInit()` が呼ばれる
5. 以降、定期的に `background()` が呼び出される

### 優先度設定

バックグラウンドサービスの実行優先度を設定できます。

```java
// アプリ初期化時
kernel.getServiceManager().setPriority(APP_ID, ProcessInfo.Priority.BACKGROUND);
```

| 優先度 | 実行頻度 | 用途 |
|--------|---------|------|
| `HIGH` | 毎フレーム | リアルタイム処理（通常使用しない） |
| `NORMAL` | 毎フレーム | 通常のアプリ |
| `LOW` | 5フレームに1回 | バッテリーセーバー時 |
| `BACKGROUND` | 10フレームに1回 | バックグラウンドサービス（デフォルト） |

### 実装のベストプラクティス

#### 1. UIスクリーンとの分離

バックグラウンドサービスはUI処理を行わないため、`draw()` や入力イベントメソッドは空実装にします。
UIとデータの共有が必要な場合は、共有データクラスやキャッシュマネージャーを介して行います。

```java
// 共有データの例
public class MyDataCache {
    private static final MyDataCache INSTANCE = new MyDataCache();
    private List<Message> messages = new ArrayList<>();

    public static MyDataCache getInstance() { return INSTANCE; }

    public synchronized void addMessage(Message msg) {
        messages.add(msg);
    }

    public synchronized List<Message> getMessages() {
        return new ArrayList<>(messages);
    }
}
```

#### 2. ネットワーク状態のチェック

バックグラウンドでネットワーク処理を行う前に、必ず接続状態を確認します。

```java
var socket = kernel.getMobileDataSocket();
if (socket != null && socket.isAvailable()) {
    // ネットワーク処理を実行
}
```

#### 3. 非同期処理の使用

バックグラウンド処理は他のアプリに影響を与えないよう、非同期で実行します。

```java
CompletableFuture.supplyAsync(() -> {
    // 重い処理
    return fetchData();
}).thenAccept(result -> {
    // 結果処理
});
```

#### 4. 通知の適切な使用

新着情報がある場合のみ通知を表示し、ユーザーに過度な通知を送らないようにします。

```java
// 新規メッセージがある場合のみ通知
if (newMessages.size() > 0) {
    notificationManager.addNotification(...);
}
```

### 注意事項

- バックグラウンドサービスはOS起動時に自動的に初期化されます
- `getBackgroundService()` は `getEntryScreen()` とは別のScreenインスタンスを返す必要があります
- バックグラウンドサービスのScreenで `draw()` を実装しても、描画は行われません
- メモリ使用量に注意し、不要なリソースは適切に解放してください
- バッテリー消費を抑えるため、処理間隔は適切に設定してください（推奨: 5秒以上）
