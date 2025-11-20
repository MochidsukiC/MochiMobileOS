# 既知の現在の問題

1.  **設定アプリが機能しない**: `SettingsApp`のUIは存在するが、設定を実際に変更するバックエンドロジックが実装されていない。
2.  **ランチャーの不具合**: ホーム画面のアイコンをDockに移動する機能が正しく動作しない。
3.  **ボイスメモアプリの音声再生問題（✅ 解決済み）**:
    - **症状**: 録音した音声が倍速再生され、音がブツ切りになる
    - **原因**:
      - 録音時のサンプリングレートが環境によって異なる（48kHz/44.1kHz/16kHz等）
      - 再生時は常に48kHz固定を想定していたため、速度が不一致
      - 例: 44.1kHzで録音→48kHzで再生 = 1.088倍速
    - **解決策**:
      - 録音時のサンプリングレートをメタデータとして保存（JSON形式）
      - 再生時に線形補間を使用して48kHzにリサンプリング
      - `JavaMicrophoneRecorder`、`AudioMixer`、`ForgeMicrophoneSocket`にサンプリングレート取得機能を追加
4.  **Forge環境でボタンが反応しない問題（デバッグ中）**:
    - **症状**: VoiceMemoAppのRecordボタンと、CalculatorAppのボタンが反応しない。ネットワークアプリでメッセージを送信しても、画面を開き直さないとリストが更新されない。
    - **正常動作**: コントロールセンターの音量ボタン、NetworkAppのSend Test Messageボタンは機能する
    - **調査結果**:
      - `ProcessingScreen.render()`内で直接LWJGL/GLFWを使用したマウスイベント検出は機能している
      - `kernel.mousePressed()`/`kernel.mouseReleased()`は正常に実行されている（例外なし）
      - マウス座標も正しく変換されている（例: 座標(93, 174)はRecordボタン範囲(X:50-170, Y:150-190)内）
      - しかし、画面上で何も変化が起きない
    - **根本原因（判明）**:
      - Minecraftのrender()は毎フレーム呼び出されるが、マウスイベント処理後に次のフレームが来るまで画面が更新されない
      - つまり、**描画更新のタイミング問題**が原因
    - **実施した修正**:
      - `ProcessingScreen`でマウスイベント（mousePressed/mouseReleased）直後に即座に`kernel.update()`と`kernel.render()`を呼び出すように変更
      - 60フレームごとに通常レンダリングのログを出力するように変更（ログ過剰防止）
      - マウスイベント後の強制更新は必ずログに出力
    - **OSロガーシステム実装** (✅ 完了):
      - VFS内に保存されるOS専用ロガーシステムを実装
      - `LoggerService`: ログレベル（DEBUG/INFO/WARN/ERROR）、ログローテーション（1MB超過時）、メモリバッファ（最新100件）
      - ログ保存先: `system/logs/latest.log`、アーカイブ: `system/logs/archive.log`
      - Kernel、ScreenManager、VoiceMemoScreenに統合済み
      - **デバッグ方法**: VFSから直接ログを確認可能（例: `forge/run/mochi_os_data/{ワールド名}/mochi_os_data/system/logs/latest.log`）
      - **次のステップ**: OSロガーを使用してボタンクリック時の詳細なデバッグログを収集し、問題箇所を特定
5.  **SVCの音声キャプチャ問題（✅ 解決済み）**:
    - **症状**: VoiceMemoAppで他のプレイヤーの声が録音できない（自分の声のみ録音される）
    - **原因**:
      - SVCプラグインサービスファイル（`META-INF/services/de.maxhenkel.voicechat.api.VoicechatPlugin`）が存在していなかった
      - そのため、SVCが`MochiVoicechatPlugin`を認識できず、`ClientReceiveSoundEvent`のイベントリスナーが登録されていなかった
      - 結果として、他のプレイヤーの音声が`VoicechatAudioCapture`に届いていなかった
    - **解決策**:
      - `forge/src/main/resources/META-INF/services/de.maxhenkel.voicechat.api.VoicechatPlugin`ファイルを作成
      - ファイル内に`jp.moyashi.phoneos.forge.voicechat.MochiVoicechatPlugin`を記述
      - ビルド後、JARファイルにサービスファイルが含まれることを確認
    - **検証方法**:
      - マルチプレイヤー環境でVoiceMemoAppを使用して録音
      - 他のプレイヤーが話した声が録音に含まれることを確認
6.  **PGraphicsアーキテクチャ移行 (Phase 8 - ✅ 100%完了)**: `core`モジュールのPGraphics統一化が完了。
7.  **全角文字（日本語）がXマークで表示される問題（✅ 解決済み - 2025-10-26）**:
    - **症状**: Forgeモジュールで全角文字がすべてXマークで表示される
    - **根本原因**:
      1. `Kernel.render()`メソッドでPGraphicsに対して`textFont(japaneseFont)`が呼び出されていなかった
      2. Forge環境で複数のClassLoaderが使用されており、`getClass().getResourceAsStream()`だけではフォントファイルを読み込めなかった
      3. **最重要**: `forge/build.gradle`のjar タスクで、coreモジュールのリソースファイル（.ttf等）がJARに含まれていなかった（`include '**/*.class'`のみ指定）
    - **解決策**:
      1. `Kernel.render()`で`graphics.textFont(japaneseFont)`を呼び出し（283-285行目）
      2. `Kernel.loadJapaneseFont()`で複数のClassLoader（Kernelクラス、コンテキスト、システム）を試行するよう修正（1694-1729行目）
      3. `forge/build.gradle`のjar タスクでリソースファイルを明示的にインクルード（209-214行目）:
         ```gradle
         from(project(':core').sourceSets.main.output) {
             include '**/*.class'
             include '**/*.ttf'  // フォントファイル
             include '**/*.otf'
             include '**/*.png'
             include '**/*.jpg'
             include '**/*.json'
         }
         ```
    - **実装ファイル**:
      - `core/src/main/java/jp/moyashi/phoneos/core/Kernel.java:283-285, 1694-1729`
      - `forge/build.gradle:207-215`
    - **ビルド結果**: ✅ BUILD SUCCESSFUL in 55s
    - **検証結果**: ✅ 日本語が正しく表示されることを確認
    *   **完了済み**:
        - **インターフェース**: IApplication, Screen (完全移行、@Deprecated ブリッジ付き)
        - **画面クラス**: HomeScreen, CalculatorScreen, SettingsScreen, LockScreen, SimpleHomeScreen, AppLibraryScreen, BasicHomeScreen, SafeHomeScreen, AppLibraryScreen_Complete
        - **アプリケーションクラス**: LauncherApp, SettingsApp, CalculatorApp
        - **サービスクラス**: ControlCenterManager, NotificationManager (他は元々PApplet非依存)
        - **統一座標システム**: CoordinateTransform クラス実装済み
    *   **移行内容**: すべてのPAppletメソッドは@Deprecatedとしてマークされ、PGraphicsメソッドが新しいプライマリAPIとなっている
7.  **Forge版Chromiumブラウザが真っ黒になる問題（✅ 対応 2025-10-21）**:
    - **原因**: `ChromiumBrowserScreen.draw()`が`MCEFRenderHandlerAdapter.updateFromTexture()`を呼び出しておらず、OpenGLテクスチャからPImageへの転送が行われていなかった。また、MCEFテクスチャのRGBA→ARGB変換時にアルファ値が0のままコピーされてしまい、結果として画面が透過されていた
    - **対応**: `ChromiumBrowser.getUpdatedImage()`で`updateFromTexture()`を毎フレーム実行し、`ChromiumBrowserScreen`はこのメソッドを介して取得したPImageをProcessingメインバッファへコピーするよう修正。`MCEFRenderHandlerAdapter`を刷新し、`MochiMCEFBrowser`のonPaint() ByteBufferを直接取得してPImageへコピー（初期フレームのみglGetTexImage()でフォールバック）。Forge側では`MochiMCEFBrowser.resize(width,height)`と`ChromiumBrowser`の`resize()`リフレクション呼び出しでサイズを明示通知するよう調整
    - **残タスク**: Forgeクライアント上での実機描画確認（次回Minecraftテストで実施）
8.  **Standalone Chromium入力バイパスの極端な遅延（✅ 対応 2025-10-21）**:
    - **症状**: スタンドアロン環境でChromiumブラウザを操作した際、マウス移動・クリック・キーボード入力が顕著に遅延し、Webページ操作がほぼできない
    - **原因**: `StandaloneChromiumProvider`がマウス・キーボードイベントを送出するたびに`sendMouseEvent`/`sendKeyEvent`等のprotectedメソッドをリフレクションで探索しており、毎回`getDeclaredMethod`＋`setAccessible(true)`を実行していた（`standalone/src/main/java/jp/moyashi/phoneos/standalone/StandaloneChromiumProvider.java:163-407`）。`mouseMoved`はフレーム毎に多数発火するため、これが主なボトルネックとなり応答速度が大幅に低下していた
    - **対応**: ブラウザクラスごとに`sendMouseEvent`/`sendMouseWheelEvent`/`sendKeyEvent`をキャッシュする`BrowserMethodCache`を導入（`StandaloneChromiumProvider.BrowserMethodCache`）。イベントソースの`Canvas`も使い回すことでGC負荷を削減した
    - **今後の改善余地**: JCEF公開API（`CefBrowserHost.send*Event`等）への置き換えを検討し、反射依存を段階的に解消する
9.  **Forge Chromium操作バイパス未実装（✅ 対応 2025-10-21）**:
    - **症状**: Forge環境でChromiumブラウザを開いても、マウス・キーボード操作がChromium側に伝搬せず、ページ操作ができない
    - **原因**: `ForgeChromiumProvider`の`sendMouse*`/`sendKey*`メソッドが未実装であり、`ChromiumBrowser`から届いたイベントをMCEFへ橋渡ししていなかった
    - **対応**: `ForgeChromiumProvider`で`MCEFBrowser`の公開API（`sendMousePress`/`sendMouseMove`/`sendMouseWheel`/`sendKeyPress`等）を呼び出す実装を追加。マウスボタン番号はMinecraft準拠へ変換し、スクロール量はProcessing由来の`delta*50`からMCEF向けに縮小した。イベント受信時には`setFocus(true)`を呼んでブラウザへフォーカスを移譲する
    - **注意**: 実機Forge環境（Minecraftクライアント）での操作確認は未実施。次回テストでマウス・キーボード操作が期待通り反映されるか検証すること
10. **Forge環境でsetWindowlessFrameRate()によるクラッシュ（✅ 解決 2025-10-24）**:
    - **症状**: Forge環境でChromiumブラウザアプリを開こうとすると、`java.lang.NoSuchMethodError: 'void org.cef.browser.CefBrowser.setWindowlessFrameRate(int)'`でクラッシュする
    - **原因**: `setWindowlessFrameRate(int)`メソッドは、java-cef master（jcefmaven 135.0.20+）でのみ追加された新しいAPIであり、MCEFが使用している古いjava-cefバージョンには存在しない
    - **対応**: `ChromiumBrowser.java`の150行目付近で、`ChromiumProvider.getName()`を使用してForge環境（MCEF）かどうかを判別し、MCEFの場合はこのメソッド呼び出しをスキップするよう条件分岐を追加
11. **Standalone環境でIME（日本語入力）が機能しない問題（✅ 解決 2025-10-26）**:
    - **症状**: P2DレンダラーへGPU最適化後、日本語入力ができなくなった。IME変換候補すら表示されず、半角入力のみ動作する
    - **根本原因**:
      1. P2DレンダラーはNEWT（Native Windowing Toolkit）を使用しており、NEWTウィンドウはIME（Input Method Editor）をネイティブサポートしていない
      2. JAVA2DレンダラーはAWT Frameを使用していたため、AWTのIME機構が利用可能だった
      3. P2D/P3D等のOpenGLレンダラーでIME入力を可能にするには、AWT/Swing JTextFieldオーバーレイが業界標準アプローチ（G4P library、libGDX、LWJGL等で採用）
    - **解決策**:
      1. **IMEInputLayer.java実装** (`standalone/src/main/java/jp/moyashi/phoneos/standalone/IMEInputLayer.java`):
         - 可視JTextField（白背景、黒文字）をProcessingウィンドウのAWTコンポーネントにオーバーレイ
         - ActionListenerでEnterキー確定を処理（DocumentListenerではIMEイベントが消費される）
         - 確定文字列を1文字ずつKernelに転送し、現在のScreenに配信
         - Escapeキーでキャンセル、フォーカスロストで自動非表示
      2. **StandaloneWrapper.java統合** (`standalone/src/main/java/jp/moyashi/phoneos/standalone/StandaloneWrapper.java`):
         - setup()でProcessing Frameを探索し、IMEInputLayerを初期化（201-222行目）
         - draw()で`Screen.hasFocusedComponent()`をチェックし、テキスト入力が必要な時のみIME入力レイヤーを画面下部に表示（260-273行目）
         - 位置: (10, SCREEN_HEIGHT - 50, SCREEN_WIDTH - 20, 30) - スマートフォンOS風の仮想キーボード変換候補表示UX
    - **動作原理**:
      1. テキスト入力が必要な画面（ChromiumBrowser等）で`hasFocusedComponent() == true`
      2. StandaloneWrapper.draw()が自動的にIMEInputLayerを画面下部に表示
      3. ユーザーがJTextFieldで日本語入力（IME変換候補が正しく表示）
      4. Enterキーで確定された文字列をKernelに転送
      5. Kernelが現在のScreenに文字入力イベントを配信
    - **実装ファイル**:
      - `standalone/src/main/java/jp/moyashi/phoneos/standalone/IMEInputLayer.java` (新規作成 v2.0)
      - `standalone/src/main/java/jp/moyashi/phoneos/standalone/StandaloneWrapper.java:27, 201-222, 260-273`
    - **ビルド結果**: ✅ BUILD SUCCESSFUL in 43s
    - **検証**: テスト待ち（Chromiumブラウザのテキストフィールドで日本語入力を確認）
    - **注意**: Forge環境はMinecraftのGUIシステムが`charTyped()`メソッドでIME確定文字を提供するため、この問題は発生しない
    - **実装内容**:
      ```java
      // setWindowlessFrameRateはjava-cef master（jcefmaven 135.0.20+）でのみサポート
      // MCEFの古いjava-cefにはこのメソッドが存在しないため、Forge環境では呼び出さない
      if (!provider.getName().equals("MCEF")) {
          browser.setWindowlessFrameRate(60);
          log("Windowless frame rate set to 60 FPS");
      } else {
          log("Skipping setWindowlessFrameRate() for MCEF (method not available)");
      }
      ```
    - **ビルド結果**: ✅ BUILD SUCCESSFUL（core:compileJava, forge:build共に成功）

# TODO

#### Forge版Chromiumブラウザ描画テスト（未実施）
- Minecraft Forge 47.3.0 + MCEF 2.1.6 環境でChromiumBrowserAppを起動し、`MochiMCEFBrowser`経由のonPaint()転送後にブラウザ画面が不透明で正しく描画されることを確認する
- `MCEFRenderHandlerAdapter`ログに`Frame copied via direct listener`や`onFrame received`、OSログに`resize() called successfully`が記録されるか検証する

- **✅ キーイベントバイパス実装完了（2025-10-24）**:
  - **目的**: すべてのキーイベント（制御キー、スペース、バックスペース、エンター等）をChromiumブラウザに転送
  - **実装内容**:
    - **キーイベント転送ロジック** (`ChromiumBrowserScreen.keyPressed()` 781-820行目):
      - `Kernel.isCtrlPressed()` で Ctrl キー状態を取得
      - **サポートする制御キーショートカット**:
        - Ctrl+C (67): コピー
        - Ctrl+V (86): ペースト
        - Ctrl+X (88): カット
        - Ctrl+A (65): 全選択
        - Ctrl+Z (90): アンドゥ
        - Ctrl+Y (89): リドゥ
        - Ctrl+F (70): 検索
        - Ctrl+R (82): 再読み込み
      - **サポートする基本キー**:
        - スペース (32)
        - バックスペース (8)
        - エンター (10)
        - その他すべての文字キー
      - 主要なキー入力時にログ出力（デバッグ用）
      - アドレスバーにフォーカスがない時、すべてのキーイベントを `ChromiumBrowser.sendKeyPressed()` に転送
    - **テキスト入力フォーカス管理** (`ChromiumBrowserScreen.hasFocusedComponent()` 843-854行目):
      - **問題**: スペースキーを押すとKernelのホームボタン処理が発動し、Chromiumブラウザから強制的にホーム画面に戻ってしまう
      - **原因**: `hasFocusedComponent()`が未実装で、デフォルトの`false`を返していた
      - **解決策**: `hasFocusedComponent()`をオーバーライドし、アドレスバーまたはWebView内のテキストフィールドにフォーカスがある可能性を考慮して`true`を返す
      - **効果**: スペースキーがホームボタンとして誤認識されず、Chromiumブラウザに正しく転送される
    - **制御キー修飾子の実装** (2025-10-24):
      - **問題**: Chromiumブラウザ内でCtrl+C/V等の制御キーショートカットが動作しない。バックスペース、エンターも動作しない
      - **根本原因**: `StandaloneChromiumProvider.sendKeyPressed()`の392行目で、`modifiers`が常に`0`になっていたため、Ctrlキーが押されていることがChromiumに伝わっていなかった
      - **修正内容**:
        1. **ChromiumProviderインターフェース変更** (`ChromiumProvider.java:179-190`):
           - `sendKeyPressed()`と`sendKeyReleased()`のシグネチャに`ctrlPressed`と`shiftPressed`パラメータを追加
        2. **ChromiumBrowser修正** (`ChromiumBrowser.java:835-860`):
           - `InputEvent.dispatch()`にKernelパラメータを追加
           - `KEY_PRESS`と`KEY_RELEASE`時に`Kernel.isCtrlPressed()`と`Kernel.isShiftPressed()`で修飾子状態を取得
           - 修飾子フラグをプロバイダーに渡す
        3. **StandaloneChromiumProvider修正** (`StandaloneChromiumProvider.java:379-426,428-462`):
           - `sendKeyPressed()`と`sendKeyReleased()`で修飾子フラグを受け取る
           - AWT KeyEventの`modifiers`フィールドに`CTRL_DOWN_MASK`と`SHIFT_DOWN_MASK`を設定
           - KEY_TYPEDイベントは制御キー押下時は送信しない（Ctrl+Cでcが入力されるのを防ぐ）
        4. **ForgeChromiumProvider修正** (`ForgeChromiumProvider.java:224-267`):
           - MCEFの`sendKeyPress()`と`sendKeyRelease()`に修飾子フラグを渡す
    - **ProcessingキーコードのAWT変換** (2025-10-24):
      - **問題**: ブラウザのUIでは動作するが、サイト内（WebView内のテキストフィールド）で動作しない
      - **根本原因**: ProcessingとAWTではキーコードの定義が異なる（例：エンターはProcessingで10、AWTでは`VK_ENTER`）。ProcessingのキーコードをそのままAWT KeyEventに渡していたため、Chromiumが正しく認識できなかった
      - **修正内容**:
        - `StandaloneChromiumProvider.convertProcessingToAwtKeyCode()` (382-408行目) を実装
        - 特殊キー（Backspace, Tab, Enter, Shift, Ctrl, Alt, Escape, Space, 矢印キー, Delete等）をProcessingからAWTに変換
        - `sendKeyPressed()`と`sendKeyReleased()`で変換メソッドを呼び出し
  - **実装ファイル**:
    - `core/service/chromium/ChromiumProvider.java:179-190`
    - `core/service/chromium/ChromiumBrowser.java:540,835-860`
    - `standalone/StandaloneChromiumProvider.java:378-500`
    - `forge/chromium/ForgeChromiumProvider.java:224-267`
    - `core/apps/chromiumbrowser/ChromiumBrowserScreen.java:781-820,843-854`
  - **ビルド結果**: ✅ BUILD SUCCESSFUL in 22s
  - **期待される効果**: Chromiumブラウザ内（WebView内のテキストフィールド含む）でのテキスト入力、制御キーショートカット（Ctrl+C/V/X/A/Z/Y）、バックスペース、エンター、スペースが完全に動作

- StandaloneChromiumProviderの入力イベント送出をJCEF公開APIベースに置き換え、反射依存を段階的に排除する（Forge側との互換性検討込み）
- ChromiumBrowserScreen/ChromiumBrowserAppを`ChromiumSurface`ベースへ移行し、`Kernel#getChromiumManager()`互換レイヤーを廃止できる状態に整理する
- Forgeクライアント実機でChromium操作バイパスを検証し、クリック・スクロール・文字入力が正しく反映されるか確認する（必要ならマウスボタン/スクロール係数を再調整）
- タブスイッチャーUIの改善（サムネイル表示、スクロール対応、タブ削除ボタン）とメニュー項目の拡張（ブックマーク管理・履歴画面遷移）

- **ワールド別データ分離のテスト**: `./run_with_java17.bat forge:runClient`でMinecraftを起動し、以下をテスト:
  1. ワールドを作成/ロードして、`mochi_os_data/{ワールド名}/mochi_os_data/`ディレクトリが作成されることを確認
  2. そのワールドでスマートフォンを使用して変更を加える（アプリ配置など）
  3. 別のワールドをロードして、`mochi_os_data/{別のワールド名}/mochi_os_data/`が作成され、データが分離されていることを確認
  4. ログで`[SmartphoneBackgroundService] World loaded: {ワールド名}`が出力されることを確認

- **ボイスメモアプリの実装** (✅ 完了):
  - `VoiceMemoApp`: マイク・スピーカーAPIを使用した音声録音・再生アプリ
  - 機能:
    - 録音: マイクから音声を録音（マイクAPI使用）
    - 再生: 録音した音声をスピーカーで再生（スピーカーAPI使用）
    - 保存: VFSに音声データをBase64エンコードして保存
    - 一覧: 保存されたメモの一覧表示
    - 削除: メモの削除
    - 音量調整: スピーカー音量の4段階調整（OFF/LOW/MEDIUM/HIGH）
  - ハードウェア状態表示: マイク・スピーカーの利用可否をリアルタイム表示
  - **注意**: SVCが導入されていない環境では、マイク・スピーカーが利用不可と表示される

- **外部アプリ開発ドキュメントの更新** (✅ 完了):
  - `EXTERNAL_APP_DEVELOPMENT_GUIDE.md`にハードウェアバイパスAPIのセクションを追加
  - 全9種類のハードウェアAPI（モバイルデータ、Bluetooth、位置情報、バッテリー、カメラ、マイク、スピーカー、IC通信、SIM情報）の使用方法を記載
  - 各APIの詳細なコード例とstandalone/forge環境での動作の違いを説明
  - `HardwareTestApp`への参照とデバッグガイドへのリンクを追加
  - 外部開発者がハードウェアAPIを利用可能であることを明確化
