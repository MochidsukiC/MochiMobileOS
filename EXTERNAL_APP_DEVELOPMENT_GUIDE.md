# MochiMobileOS 外部アプリケーション開発手順書

## 概要

MochiMobileOSは疑似仮想OSであり、外部プロジェクトから依存関係として利用することで、独自のアプリケーションを開発できます。本手順書では、外部アプリケーション開発に必要な手順を詳しく説明します。

**開発方法**:
- **標準アプリケーション開発**: `/apps`または`/mods`ディレクトリからの動的読み込み
- **Forge MODアプリケーション開発**: `PhoneAppRegistryEvent`を通じたMinecraft MODとしてのアプリケーション登録

**最新の仕様**:
- `IApplication`インターフェースの改良（ファイルベースアイコンシステムへの移行）
- `getIcon()`メソッドは`PImage`を返すようになり、nullの場合はシステムが白いデフォルトアイコンを生成
- アプリケーションはJAR内のリソースから独自にアイコンを読み込む責任を持つ
- `ModAppRegistry`によるスレッドセーフなMODアプリケーション管理
- Forge MOD統合システムの本格実装

## 開発環境構築

### 1. 依存関係の設定

外部プロジェクトの`build.gradle.kts`または`build.gradle`に以下の依存関係を追加してください。

```kotlin
repositories {
    mavenCentral()
    maven {
        url = uri("https://jogamp.org/deployment/maven/")
    }
    maven {
        url = uri("https://maven.pkg.github.com/MochidsukiC/MochiMobileOS")
        credentials {
            username = "your-github-username"
            password = "your-github-token"
        }
    }
}

dependencies {
    implementation("jp.moyashi.phoneos:core:1.0.1")
    implementation("org.processing:core:4.4.4")
    implementation("com.google.code.gson:gson:2.10.1")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
```

### 2. 必須インポート

**標準アプリケーション開発**:
```java
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.Kernel;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
```

**Forge MODアプリケーション開発** (追加):
```java
import jp.moyashi.phoneos.forge.event.PhoneAppRegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
```

## 必須クラスの実装

### 1. IApplicationインターフェースの実装

すべてのアプリケーションは`IApplication`インターフェースを実装する必要があります。

```java
package com.yourcompany.yourapp;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.Kernel;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PGraphics;

public class YourApp implements IApplication {

    @Override
    public String getApplicationId() {
        return "com.yourcompany.yourapp";  // 逆ドメイン記法で一意のID
    }

    @Override
    public String getApplicationName() {
        return "あなたのアプリ名";  // ユーザーに表示される名前
    }

    @Override
    public String getApplicationVersion() {
        return "1.0.0";  // セマンティックバージョニング
    }

    @Override
    public PImage getIcon() {
        // アイコン画像をアプリケーションのリソースから読み込む
        // JARファイル内のリソースにアクセスする場合は以下のようにする
        try {
            java.io.InputStream stream = getClass().getResourceAsStream("/icons/yourapp.png");
            if (stream != null) {
                // PImageに変換（Processing環境が必要）
                // 注意: この例は簡略化されています。実際には適切な画像読み込み処理が必要です
                byte[] bytes = stream.readAllBytes();
                // ... PImageへの変換処理 ...
                stream.close();
            }
        } catch (Exception e) {
            System.err.println("Failed to load icon: " + e.getMessage());
        }

        // アイコンが読み込めない場合はnullを返す
        // システムが自動的に白いデフォルトアイコンを生成します
        return null;
    }

    @Override
    public Screen createMainScreen() {
        return new YourAppMainScreen();  // メインスクリーンを返す
    }

    @Override
    public void initialize() {
        System.out.println(getApplicationName() + " が初期化されました");
        // 初期化処理をここに記述
    }

    @Override
    public void onInstall() {
        System.out.println(getApplicationName() + " がインストールされました");
        // インストール時の処理をここに記述
    }

    @Override
    public void terminate() {
        System.out.println(getApplicationName() + " が終了されました");
        // 終了処理をここに記述
    }

    // 互換性のための旧インターフェースエイリアス
    public String getName() {
        return getApplicationName();
    }

    public String getVersion() {
        return getApplicationVersion();
    }

}
```

### 2. Screenインターフェースの実装

アプリケーションの画面は`Screen`インターフェースを実装する必要があります。

```java
package com.yourcompany.yourapp.ui;

import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.Kernel;
import processing.core.PApplet;

public class YourAppMainScreen implements Screen {

    private final Kernel kernel;
    private boolean initialized = false;

    public YourAppMainScreen(Kernel kernel) {
        this.kernel = kernel;
    }

    @Override
    public void setup(PApplet p) {
        System.out.println("画面をセットアップ中...");
        initialized = true;
        // 初期化処理をここに記述
    }

    @Override
    public void draw(PApplet p) {
        if (!initialized) return;

        // 背景
        p.background(240);

        // タイトル
        p.fill(0);
        p.textAlign(p.CENTER, p.TOP);
        p.textSize(24);
        p.text("あなたのアプリ", p.width / 2, 50);

        // 戻るボタン
        p.fill(150);
        p.textAlign(p.LEFT, p.TOP);
        p.textSize(14);
        p.text("< 戻る", 10, 10);

        // アプリのメインコンテンツ
        drawMainContent(p);
    }

    private void drawMainContent(PApplet p) {
        // メインコンテンツの描画処理
        p.fill(100);
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(18);
        p.text("ここにアプリのコンテンツを表示", p.width / 2, p.height / 2);
    }

    @Override
    public void mousePressed(PApplet p, int mouseX, int mouseY) {
        // 戻るボタンの処理
        if (mouseX < 60 && mouseY < 30) {
            goBack();
            return;
        }

        // その他のクリック処理
        handleMainContentClick(mouseX, mouseY);
    }

    private void handleMainContentClick(int mouseX, int mouseY) {
        // メインコンテンツのクリック処理
        System.out.println("クリック座標: (" + mouseX + ", " + mouseY + ")");
    }

    @Override
    public void keyPressed(PApplet p, char key, int keyCode) {
        // キーボード入力の処理
        if (key == 27) { // ESCキー
            goBack();
        }
    }

    @Override
    public void mouseDragged(PApplet p, int mouseX, int mouseY) {
        // ドラッグ処理（必要に応じて実装）
    }

    @Override
    public void mouseReleased(PApplet p, int mouseX, int mouseY) {
        // マウスリリース処理（必要に応じて実装）
    }

    private void goBack() {
        if (kernel != null && kernel.getScreenManager() != null) {
            kernel.getScreenManager().popScreen();
        }
    }

    @Override
    public void cleanup(PApplet p) {
        System.out.println("画面をクリーンアップ中...");
        // リソースの解放処理
    }

    @Override
    public String getScreenTitle() {
        return "あなたのアプリ メイン画面";
    }
}
```

## MinecraftのMODとして開発する場合

MochiMobileOSは、Minecraft Forge MODとしてアプリケーションを開発することをサポートしています。MODアプリケーションは、`PhoneAppRegistryEvent`を通じて登録され、`ModAppRegistry`によって管理されます。

### 1. ForgeのMODイベントをリスンする

```java
package com.yourcompany.yourmod;

import jp.moyashi.phoneos.forge.event.PhoneAppRegistryEvent;
import jp.moyashi.phoneos.core.app.IApplication;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("yourmodid")
public class YourMod {

    public YourMod() {
        System.out.println("[YourMod] Initializing MOD...");

        // MODイベントバスに登録
        FMLJavaModLoadingContext.get()
            .getModEventBus().addListener(this::onPhoneAppRegistry);

        System.out.println("[YourMod] Event listener registered for PhoneAppRegistryEvent");
    }

    /**
     * MochiMobileOSからのアプリケーション登録イベントをリスン
     * このイベントは、MochiMobileOSMODが初期化時に発行されます。
     */
    @SubscribeEvent
    public void onPhoneAppRegistry(PhoneAppRegistryEvent event) {
        System.out.println("[YourMod] Received PhoneAppRegistryEvent");

        try {
            // あなたのアプリケーションを登録
            IApplication yourApp = new YourApp();
            event.registerApp(yourApp);

            System.out.println("[YourMod] Successfully registered app: " +
                yourApp.getApplicationName() + " (" + yourApp.getApplicationId() + ")");
        } catch (Exception e) {
            System.err.println("[YourMod] Failed to register app: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

### 2. MOD用build.gradle.kts設定

```kotlin
plugins {
    id("net.minecraftforge.gradle") version "5.1.+"
}

dependencies {
    minecraft("net.minecraftforge:forge:1.20.1-47.2.0")
    implementation("jp.moyashi.phoneos:core:1.0.1")
    implementation("jp.moyashi.phoneos:forge:1.0.1")
}
```

## システムサービスの利用

### 1. VFS（仮想ファイルシステム）の利用

```java
// ファイルの読み書き
String content = kernel.getVFS().readFile("appdata/yourapp/config.txt");
kernel.getVFS().writeFile("appdata/yourapp/data.json", jsonData);

// ディレクトリ操作
kernel.getVFS().createDirectory("appdata/yourapp/saves");
boolean exists = kernel.getVFS().directoryExists("appdata/yourapp");
```

### 2. 設定管理

```java
// 設定の読み書き
kernel.getSettingsManager().setSetting("yourapp.theme", "dark");
String theme = kernel.getSettingsManager().getSetting("yourapp.theme", "light");
```

### 3. 通知システム

```java
import jp.moyashi.phoneos.core.notification.SimpleNotification;

// 通知の送信
SimpleNotification notification = new SimpleNotification(
    "あなたのアプリ",
    "処理が完了しました",
    System.currentTimeMillis()
);
kernel.getNotificationManager().addNotification(notification);
```

### 4. システム時計

```java
// 現在時刻の取得
long currentTime = kernel.getSystemClock().getCurrentTime();
String timeString = kernel.getSystemClock().getFormattedTime("HH:mm:ss");
```

### 5. ハードウェアバイパスAPI

MochiMobileOSは、スマートフォンのハードウェア機能をエミュレートする**ハードウェアバイパスAPI**を提供しています。これらのAPIは、standalone環境では基本的な動作を提供し、Minecraft Forge環境ではゲーム内の実データに基づいた高度な機能を提供します。

#### 利用可能なハードウェアAPI

##### 5.1 モバイルデータ通信ソケット (MobileDataSocket)

仮想インターネット通信の状態を取得します。

```java
import jp.moyashi.phoneos.core.service.hardware.MobileDataSocket;

// モバイルデータ通信の状態を取得
MobileDataSocket socket = kernel.getMobileDataSocket();

// 利用可能かどうか
boolean available = socket.isAvailable();

// 電波強度（0-5）
int signalStrength = socket.getSignalStrength();

// サービス名（例: "MochiNet", "No Service"）
String serviceName = socket.getServiceName();

// 接続状態
boolean connected = socket.isConnected();
```

**環境別の動作**:
- **standalone**: 常に圏外（利用不可）
- **forge**: Y座標に基づく電波強度、ディメンション別ネットワーク名

##### 5.2 Bluetooth通信ソケット (BluetoothSocket)

周囲のBluetoothデバイスを検出・接続します。

```java
import jp.moyashi.phoneos.core.service.hardware.BluetoothSocket;
import java.util.List;

// Bluetoothソケットを取得
BluetoothSocket socket = kernel.getBluetoothSocket();

// 周囲のデバイスをスキャン
List<BluetoothSocket.BluetoothDevice> devices = socket.scanNearbyDevices();

// デバイス情報を取得
for (BluetoothSocket.BluetoothDevice device : devices) {
    String name = device.name;        // デバイス名
    String address = device.address;  // MACアドレス
    double distance = device.distance; // 距離（メートル）
}

// デバイスに接続
boolean connected = socket.connect(devices.get(0).address);

// 接続済みデバイスのリスト
List<BluetoothSocket.BluetoothDevice> connectedDevices = socket.getConnectedDevices();
```

**環境別の動作**:
- **standalone**: 常にデバイスNOTFOUND
- **forge**: 半径10m以内のプレイヤー/エンティティを検出

##### 5.3 位置情報ソケット (LocationSocket)

現在の位置情報を取得します。

```java
import jp.moyashi.phoneos.core.service.hardware.LocationSocket;

// 位置情報ソケットを取得
LocationSocket socket = kernel.getLocationSocket();

// GPS有効化
socket.setEnabled(true);

// 位置情報を取得
LocationSocket.LocationData location = socket.getLocation();
double x = location.x;
double y = location.y;
double z = location.z;
double accuracy = location.accuracy; // GPS精度（メートル）
```

**環境別の動作**:
- **standalone**: (0, 0, 0) 固定
- **forge**: プレイヤーの実際の座標

##### 5.4 バッテリー情報 (BatteryInfo)

バッテリー残量と状態を取得します。

```java
import jp.moyashi.phoneos.core.service.hardware.BatteryInfo;

// バッテリー情報を取得
BatteryInfo battery = kernel.getBatteryInfo();

// バッテリー残量（0-100%）
int level = battery.getBatteryLevel();

// バッテリー寿命（0-100%）
int health = battery.getBatteryHealth();

// 充電中かどうか
boolean charging = battery.isCharging();
```

**環境別の動作**:
- **standalone**: 常に100%
- **forge**: アイテムNBTから取得（消費・充電機能あり）

##### 5.5 カメラソケット (CameraSocket)

カメラ映像をキャプチャします。

```java
import jp.moyashi.phoneos.core.service.hardware.CameraSocket;
import processing.core.PImage;

// カメラソケットを取得
CameraSocket socket = kernel.getCameraSocket();

// カメラを有効化
socket.setEnabled(true);

// 現在のフレームを取得
PImage frame = socket.getCurrentFrame();
if (frame != null) {
    // フレームを描画
    g.image(frame, 0, 0, width, height);
}
```

**環境別の動作**:
- **standalone**: 常にnull（利用不可）
- **forge**: 保留（実装未完了）

##### 5.6 マイクソケット (MicrophoneSocket)

音声入力を取得します。

```java
import jp.moyashi.phoneos.core.service.hardware.MicrophoneSocket;

// マイクソケットを取得
MicrophoneSocket socket = kernel.getMicrophoneSocket();

// 利用可能かどうか
if (socket.isAvailable()) {
    // マイクを有効化
    socket.setEnabled(true);

    // 音声データを取得
    byte[] audioData = socket.getAudioData();
    if (audioData != null && audioData.length > 0) {
        // 音声データを処理
        processAudio(audioData);
    }
}
```

**環境別の動作**:
- **standalone**: 常にnull（利用不可）
- **forge**: Simple Voice Chat MOD連携（SVC導入時のみ有効）

##### 5.7 スピーカーソケット (SpeakerSocket)

音声を再生します。

```java
import jp.moyashi.phoneos.core.service.hardware.SpeakerSocket;

// スピーカーソケットを取得
SpeakerSocket socket = kernel.getSpeakerSocket();

// 音量レベルを設定（OFF, LOW, MEDIUM, HIGH）
socket.setVolumeLevel(SpeakerSocket.VolumeLevel.MEDIUM);

// 音声データを再生
byte[] audioData = loadAudioFile("sound.wav");
socket.playAudio(audioData);
```

**環境別の動作**:
- **standalone**: Java標準Audio APIで再生
- **forge**: Simple Voice Chat MOD連携（音量に応じた範囲で放送）

##### 5.8 IC通信ソケット (ICSocket)

NFCのようなIC通信機能を提供します。

```java
import jp.moyashi.phoneos.core.service.hardware.ICSocket;

// IC通信ソケットを取得
ICSocket socket = kernel.getICSocket();

// IC通信を有効化
socket.setEnabled(true);

// IC読み取りデータをポーリング
ICSocket.ICData data = socket.pollICData();

if (data.type == ICSocket.ICDataType.BLOCK) {
    // ブロック情報
    System.out.println("Block: (" + data.blockX + ", " + data.blockY + ", " + data.blockZ + ")");
} else if (data.type == ICSocket.ICDataType.ENTITY) {
    // エンティティ情報
    System.out.println("Entity UUID: " + data.entityUUID);
}
```

**環境別の動作**:
- **standalone**: 常にNONE（利用不可）
- **forge**: しゃがみ右クリックでブロック座標/エンティティUUID取得

##### 5.9 SIM情報 (SIMInfo)

SIMカード情報を取得します。

```java
import jp.moyashi.phoneos.core.service.hardware.SIMInfo;

// SIM情報を取得
SIMInfo sim = kernel.getSIMInfo();

// 利用可能かどうか
boolean available = sim.isAvailable();

// SIMが挿入されているかどうか
boolean inserted = sim.isInserted();

// 所有者名
String ownerName = sim.getOwnerName();

// 所有者UUID
String ownerUUID = sim.getOwnerUUID();
```

**環境別の動作**:
- **standalone**: "Dev" / "00000000-0000-0000-0000-000000000000"
- **forge**: プレイヤーの表示名/UUID

#### ハードウェアAPIの使用例

完全な使用例として、`HardwareTestApp`を参照してください:
- `core/src/main/java/jp/moyashi/phoneos/core/apps/hardware_test/HardwareTestApp.java`
- `core/src/main/java/jp/moyashi/phoneos/core/apps/hardware_test/HardwareTestScreen.java`

#### デバッグ方法

詳細なデバッグ方法については、`HARDWARE_DEBUG_GUIDE.md`を参照してください。

## 画面遷移とライフサイクル

### 1. 画面の遷移

```java
// 新しい画面にプッシュ
Screen newScreen = new AnotherScreen(kernel);
kernel.getScreenManager().pushScreen(newScreen);

// 現在の画面をポップ
kernel.getScreenManager().popScreen();

// アニメーション付きポップ
kernel.getScreenManager().popScreenWithAnimation(iconX, iconY, iconSize, appIcon);
```

### 2. アプリケーションライフサイクル

```java
public class YourApp implements IApplication {

    @Override
    public void onInitialize(Kernel kernel) {
        // アプリ起動時の処理
    }

    @Override
    public void onInstall(Kernel kernel) {
        // 初回インストール時の処理
        // データディレクトリ作成、初期設定など
    }

    @Override
    public void onDestroy() {
        // アプリ終了時の処理
        // リソース解放、設定保存など
    }
}
```

## ベストプラクティス

### 1. パフォーマンス
- `draw()`メソッドは毎フレーム呼び出されるため、重い処理は避ける
- 必要に応じて`setup()`で初期化処理を完了させる
- 不要なオブジェクト作成を避ける

### 2. ユーザーエクスペリエンス
- 戻るボタンを必ず実装する
- タッチ領域は十分な大きさを確保する
- 適切なフィードバックを提供する

### 3. システム統合
- アプリケーションIDは一意にする（逆ドメイン記法推奨）
- VFSを使用してデータを永続化する
- システムの設定管理を活用する

### 4. エラーハンドリング
- `try-catch`でシステムサービスの呼び出しを保護する
- ユーザーに分かりやすいエラーメッセージを表示する
- クリティカルエラーは適切にログ出力する

## サンプルプロジェクト構造

```
your-app-project/
├── build.gradle.kts
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── yourcompany/
│                   └── yourapp/
│                       ├── YourApp.java          # IApplication実装
│                       ├── ui/
│                       │   ├── MainScreen.java   # メイン画面
│                       │   └── SettingsScreen.java # 設定画面
│                       ├── model/
│                       │   └── AppData.java      # データモデル
│                       └── service/
│                           └── DataService.java  # データ処理サービス
└── README.md
```

## トラブルシューティング

### よくある問題と解決方法

1. **依存関係エラー**
   - Processing 4.4.4が正しくインポートされているか確認
   - Java 17以上を使用しているか確認

2. **画面が表示されない**
   - `setup()`メソッドが正しく呼び出されているか確認
   - `draw()`メソッドで適切に描画処理を実装しているか確認

3. **システムサービスが利用できない**
   - `Kernel`インスタンスが正しく渡されているか確認
   - サービスの初期化が完了しているか確認

4. **MODで登録されない**
   - `PhoneAppRegistryEvent`を正しくリスンしているか確認
   - MODのクラスパスが正しく設定されているか確認

## まとめ

MochiMobileOSでの外部アプリケーション開発は、`IApplication`と`Screen`インターフェースの実装が中心となります。システムサービスを活用し、適切なライフサイクル管理を行うことで、高品質なアプリケーションを開発できます。

詳細な実装例については、`core/src/main/java/jp/moyashi/phoneos/core/apps/calculator/`ディレクトリの`CalculatorApp.java`と`CalculatorScreen.java`を参考にしてください。