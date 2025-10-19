# ハードウェアバイパスAPI デバッグガイド

## 概要

このガイドでは、MochiMobileOSのハードウェアバイパスAPIの各サービスをデバッグする方法を説明します。

## デバッグ用アプリケーション

### Hardware Test App

`HardwareTestApp`は、すべてのハードウェアバイパスAPIの動作を確認するためのテストアプリケーションです。

**起動方法**:
1. standalone環境またはforge環境でMochiMobileOSを起動
2. ランチャーから「Hardware Test」アプリ（🔧アイコン）を起動
3. 各APIの状態がリアルタイムで表示されます

**操作方法**:
- `r`: テストデータをリフレッシュ
- `q`: アプリを終了

## 各サービスのデバッグ方法

### 1. モバイルデータ通信ソケット (MobileDataSocket)

**standalone環境でのテスト**:
```java
MobileDataSocket socket = kernel.getMobileDataSocket();
System.out.println("Available: " + socket.isAvailable()); // false
System.out.println("Signal: " + socket.getSignalStrength()); // 0
System.out.println("Service: " + socket.getServiceName()); // "No Service"
System.out.println("Connected: " + socket.isConnected()); // false
```

**forge環境でのテスト**:
```java
// forge-modで独自実装を提供する場合
MobileDataSocket forgeSocket = new ForgeMobileDataSocket();
kernel.setMobileDataSocket(forgeSocket);

// 仮想インターネット通信の状態を確認
System.out.println("Signal: " + forgeSocket.getSignalStrength()); // 実装による
```

**期待される動作**:
- standalone: 常に圏外
- forge-mod: 仮想インターネット通信の状態に応じた値

---

### 2. Bluetooth通信ソケット (BluetoothSocket)

**standalone環境でのテスト**:
```java
BluetoothSocket socket = kernel.getBluetoothSocket();
System.out.println("Available: " + socket.isAvailable()); // false

List<BluetoothSocket.BluetoothDevice> devices = socket.scanNearbyDevices();
System.out.println("Nearby devices: " + devices.size()); // 0
```

**forge環境でのテスト**:
```java
// 半径10m以内のBluetoothデバイスを検索
List<BluetoothSocket.BluetoothDevice> devices = socket.scanNearbyDevices();
for (BluetoothSocket.BluetoothDevice device : devices) {
    System.out.println("Device: " + device.name + " at " + device.distance + "m");
}

// デバイスに接続
boolean connected = socket.connect(devices.get(0).address);
System.out.println("Connected: " + connected);
```

**期待される動作**:
- standalone: 常にデバイスNOTFOUND
- forge-mod: 半径10m以内のプレイヤー/エンティティをBluetoothデバイスとして検出

---

### 3. 位置情報ソケット (LocationSocket)

**standalone環境でのテスト**:
```java
LocationSocket socket = kernel.getLocationSocket();
LocationSocket.LocationData loc = socket.getLocation();
System.out.println("Position: (" + loc.x + ", " + loc.y + ", " + loc.z + ")"); // (0, 0, 0)
System.out.println("Accuracy: " + loc.accuracy); // 0.0
```

**forge環境でのテスト**:
```java
// プレイヤーの位置情報を取得
LocationSocket.LocationData loc = socket.getLocation();
System.out.println("Player position: (" + loc.x + ", " + loc.y + ", " + loc.z + ")");
System.out.println("GPS accuracy: " + loc.accuracy + "m");
```

**期待される動作**:
- standalone: 常に(0, 0, 0)
- forge-mod: プレイヤーの実際の座標

---

### 4. バッテリー情報 (BatteryInfo)

**standalone環境でのテスト**:
```java
BatteryInfo info = kernel.getBatteryInfo();
System.out.println("Battery level: " + info.getBatteryLevel() + "%"); // 100
System.out.println("Battery health: " + info.getBatteryHealth() + "%"); // 100
System.out.println("Charging: " + info.isCharging()); // false
```

**forge環境でのテスト**:
```java
// アイテムNBTからバッテリー情報を取得
BatteryInfo info = kernel.getBatteryInfo();
System.out.println("Battery level: " + info.getBatteryLevel() + "%");

// バッテリー消費をシミュレート
info.setBatteryLevel(info.getBatteryLevel() - 1);
```

**期待される動作**:
- standalone: 常に100%
- forge-mod: アイテムNBTに記録されたバッテリー残量

---

### 5. カメラ (CameraSocket)

**standalone環境でのテスト**:
```java
CameraSocket socket = kernel.getCameraSocket();
System.out.println("Available: " + socket.isAvailable()); // false

socket.setEnabled(true);
PImage frame = socket.getCurrentFrame();
System.out.println("Frame: " + (frame != null ? "Available" : "null")); // null
```

**forge環境でのテスト**:
```java
// カメラを有効化
socket.setEnabled(true);

// Minecraftの画面映像を取得
PImage frame = socket.getCurrentFrame();
if (frame != null) {
    // フレームをアプリで表示
    g.image(frame, 0, 0, width, height);
}
```

**期待される動作**:
- standalone: 常にnull
- forge-mod: Minecraftの画面映像をPImageとして取得

---

### 6. マイク (MicrophoneSocket)

**standalone環境でのテスト**:
```java
MicrophoneSocket socket = kernel.getMicrophoneSocket();
System.out.println("Available: " + socket.isAvailable()); // false

socket.setEnabled(true);
byte[] audioData = socket.getAudioData();
System.out.println("Audio data: " + (audioData != null ? "Available" : "null")); // null
```

**forge環境でのテスト**:
```java
// マイクを有効化
socket.setEnabled(true);

// SVCのマイク入力を取得
byte[] audioData = socket.getAudioData();
if (audioData != null && audioData.length > 0) {
    System.out.println("Received " + audioData.length + " bytes of audio");
}
```

**期待される動作**:
- standalone: 常にnull
- forge-mod: SVCマイク入力をバイトストリームとして取得

---

### 7. スピーカー (SpeakerSocket)

**standalone環境でのテスト**:
```java
SpeakerSocket socket = kernel.getSpeakerSocket();
System.out.println("Available: " + socket.isAvailable()); // true

// 音量を設定
socket.setVolumeLevel(SpeakerSocket.VolumeLevel.MEDIUM);

// 音声を再生
byte[] audioData = loadAudioFile("test.wav");
socket.playAudio(audioData);
```

**forge環境でのテスト**:
```java
// 音量レベルに応じた再生
socket.setVolumeLevel(SpeakerSocket.VolumeLevel.LOW); // 自分だけに聞こえる
socket.playAudio(audioData);

socket.setVolumeLevel(SpeakerSocket.VolumeLevel.MEDIUM); // 周囲に聞こえる（中音量）
socket.playAudio(audioData);

socket.setVolumeLevel(SpeakerSocket.VolumeLevel.HIGH); // 遠くまで聞こえる（大音量）
socket.playAudio(audioData);
```

**期待される動作**:
- standalone: Java標準のAudio APIで再生
- forge-mod:
  - LOW: 自分だけに再生
  - MEDIUM/HIGH: SVCシステムを使用して周囲に放送

---

### 8. IC通信 (ICSocket)

**standalone環境でのテスト**:
```java
ICSocket socket = kernel.getICSocket();
System.out.println("Available: " + socket.isAvailable()); // false

socket.setEnabled(true);
ICSocket.ICData data = socket.pollICData();
System.out.println("Data type: " + data.type.name()); // NONE
```

**forge環境でのテスト**:
```java
// IC通信を有効化
socket.setEnabled(true);

// プレイヤーがブロックをしゃがみ右クリックしたときの処理
ICSocket.ICData data = socket.pollICData();
if (data.type == ICSocket.ICDataType.BLOCK) {
    System.out.println("Block scanned: (" + data.blockX + ", " + data.blockY + ", " + data.blockZ + ")");
} else if (data.type == ICSocket.ICDataType.ENTITY) {
    System.out.println("Entity scanned: " + data.entityUUID);
}
```

**期待される動作**:
- standalone: 常にNONE
- forge-mod: しゃがみ右クリックでブロック座標またはエンティティUUIDを取得

---

### 9. SIM情報 (SIMInfo)

**standalone環境でのテスト**:
```java
SIMInfo info = kernel.getSIMInfo();
System.out.println("Available: " + info.isAvailable()); // true
System.out.println("Inserted: " + info.isInserted()); // true
System.out.println("Owner: " + info.getOwnerName()); // "Dev"
System.out.println("UUID: " + info.getOwnerUUID()); // "00000000-0000-0000-0000-000000000000"
```

**forge環境でのテスト**:
```java
// プレイヤー情報を取得
SIMInfo info = kernel.getSIMInfo();
System.out.println("Owner: " + info.getOwnerName()); // プレイヤーの表示名
System.out.println("UUID: " + info.getOwnerUUID()); // プレイヤーのUUID

// 他のプレイヤーとの通信に使用
String targetAddress = "0-" + info.getOwnerUUID();
```

**期待される動作**:
- standalone: 固定値（Dev, 00000000-0000-0000-0000-000000000000）
- forge-mod: スマートフォン所持者の表示名とUUID

---

## デバッグのベストプラクティス

### 1. ログ出力の活用

各APIの呼び出し時にログを出力して動作を追跡:

```java
System.out.println("[DEBUG] Calling getMobileDataSocket()");
MobileDataSocket socket = kernel.getMobileDataSocket();
System.out.println("[DEBUG] Signal strength: " + socket.getSignalStrength());
```

### 2. Hardware Test Appでの確認

実装後、必ずHardware Test Appで全APIの動作を確認:

1. standalone環境で起動
2. すべてのAPIがデフォルト値を返すことを確認
3. forge環境で起動
4. 各APIが適切な値を返すことを確認

### 3. 段階的なテスト

1. **Phase 1**: インターフェースとデフォルト実装のテスト（standalone）
2. **Phase 2**: forge-mod実装の作成
3. **Phase 3**: forge-mod実装のテスト
4. **Phase 4**: 実アプリでの動作確認

### 4. エラーハンドリング

各APIの呼び出しはtry-catchで囲み、エラーを適切に処理:

```java
try {
    LocationSocket.LocationData loc = kernel.getLocationSocket().getLocation();
    System.out.println("Position: " + loc.x + ", " + loc.y + ", " + loc.z);
} catch (Exception e) {
    System.err.println("Error getting location: " + e.getMessage());
    e.printStackTrace();
}
```

---

## トラブルシューティング

### 問題: APIがnullを返す

**原因**: Kernelが初期化されていない、またはAPIが設定されていない

**解決策**:
```java
if (kernel.getMobileDataSocket() == null) {
    System.err.println("MobileDataSocket is not initialized!");
    // デフォルト実装を設定
    kernel.setMobileDataSocket(new DefaultMobileDataSocket());
}
```

### 問題: forge-mod実装が反映されない

**原因**: setterが呼び出されていない

**解決策**:
```java
// SmartphoneBackgroundServiceのKernel初期化後に実装を差し替え
kernel.setMobileDataSocket(new ForgeMobileDataSocket());
kernel.setLocationSocket(new ForgeLocationSocket(player));
// ... 他のAPIも同様
```

### 問題: Hardware Test Appが起動しない

**原因**: アプリが登録されていない

**解決策**:
1. `Kernel.java`の`setup()`メソッドで`HardwareTestApp`が登録されているか確認
2. コンパイルエラーがないか確認
3. ランチャーのアプリ一覧を確認

---

## 次のステップ

1. ✅ core モジュールでのインターフェース実装（完了）
2. ⏳ forge モジュールでの実装作成
3. ⏳ 実装のテストとデバッグ
4. ⏳ 実アプリでの動作確認

詳細は`DEV.md`を参照してください。
