# MochiMobileOS 開発ドキュメント

### 1. プロジェクト仕様書

**概要**: Java/ProcessingベースのスマートフォンOSシミュレータ。最終的にMinecraft Forge MODとしてゲーム内での利用を目指す。

**アーキテクチャ**:
*   **`core`**: OSの純粋なロジック (環境非依存)
*   **`standalone`**: PCでの実行用ランチャー
*   **`forge`**: Minecraftとの連携用ブリッジ (実装中)
*   **カーネル**: サービス指向 (`VFS`, `AppLoader`, `GestureManager`等)
*   **統合MODモデル**: 外部MODがMochiMobileOSアプリとして自己登録できるAPIを提供。

**主要機能**:
*   **システム**: ロック画面、コントロールセンター、通知センター
*   **ランチャー**: マルチページホーム、編集モード、Appライブラリ、Dock

### 2. 現在の仕様

**有効なサービス**:
VFS, SettingsManager, SystemClock, AppLoader, LayoutManager, PopupManager, GestureManager, ControlCenterManager, NotificationManager, LockManager, LayerManager

**組み込みアプリ**:
LauncherApp, SettingsApp, CalculatorApp, AppStoreApp (UIのみ)

**Forge連携**:
*   MODとしての基本構造とスマートフォンアイテムが実装済み。
*   他のMODがアプリを登録するためのAPI (`PhoneAppRegistryEvent`) が機能している。

### 3. 既知の現在の問題

1.  **Forge連携が不完全**: `core`モジュールと`forge`モジュール間のブリッジ(`KernelAdapter`等)が未実装で、両者が独立して動作している。
2.  **設定アプリが機能しない**: `SettingsApp`のUIは存在するが、設定を実際に変更するバックエンドロジックが実装されていない。
3.  **ランチャーの不具合**: ホーム画面のアイコンをDockに移動する機能が正しく動作しない。
4.  **PGraphicsアーキテクチャ移行 (Phase 8 - ✅ 100%完了)**: `core`モジュールのPGraphics統一化が完了。
    *   **完了済み**:
        - **インターフェース**: IApplication, Screen (完全移行、@Deprecated ブリッジ付き)
        - **画面クラス**: HomeScreen, CalculatorScreen, SettingsScreen, LockScreen, SimpleHomeScreen, AppLibraryScreen, BasicHomeScreen, SafeHomeScreen, AppLibraryScreen_Complete
        - **アプリケーションクラス**: LauncherApp, SettingsApp, CalculatorApp
        - **サービスクラス**: ControlCenterManager, NotificationManager (他は元々PApplet非依存)
        - **統一座標システム**: CoordinateTransform クラス実装済み
    *   **移行内容**: すべてのPAppletメソッドは@Deprecatedとしてマークされ、PGraphicsメソッドが新しいプライマリAPIとなっている

### 5. ロック画面でのスペースキー無反応問題 ⚠️ → ✅ **解決済み** (2025年9月29日)

**問題**: ロック画面でスペースキーが押されても何も反応しない現象が発生していました。

**根本原因**: Kernelのキー処理ロジックで、ロック画面が表示されている場合でもスペースキーを「閉じられないレイヤー」として扱い、ScreenManagerに転送していませんでした。

**解決方法**:
- `Kernel.java`のkeyPressed()メソッドを修正
- ロック画面が表示されている場合は、レイヤー管理を通さずに直接ScreenManagerにキー入力を転送
- ロック画面のkeyPressed()メソッドが既に実装されており、スペースキーでパターン入力画面の展開/閉じる機能が動作

**修正されたコード**:
```java
// スペースキー（ホームボタン）の階層管理処理
if (key == ' ' || keyCode == 32) {
    System.out.println("Kernel: Space key pressed - checking lock screen status");

    // ロック画面が表示されている場合は、ロック画面に処理を委譲
    if (layerStack.contains(LayerType.LOCK_SCREEN)) {
        System.out.println("Kernel: Lock screen is active - forwarding space key to screen manager");
        if (screenManager != null) {
            screenManager.keyPressed(key, keyCode);
        }
        return;
    }

    // ロック画面が表示されていない場合は、通常のホームボタン処理
    System.out.println("Kernel: Space key pressed - handling home button");
    handleHomeButton();
    return;
}
```

**結果**: ロック画面でスペースキーが正常に反応し、パターン入力画面の展開/閉じるトグル機能が動作するようになりました。

### 6. アプリケーション実行時のスペースキー無反応問題 ⚠️ → ✅ **解決済み** (2025年9月29日)

**問題**: アプリケーションを開いている時にスペースキーを押してもホームスクリーンに戻らない現象が発生していました。

**根本原因**: ScreenManagerでアプリケーション画面をプッシュした際に、KernelのレイヤースタックにAPPLICATIONレイヤーが追加されていませんでした。そのため、Kernelのスペースキー処理で`getTopMostClosableLayer()`がAPPLICATIONレイヤーを見つけられず、ホームボタン処理が機能していませんでした。

**解決方法**:
1. **ScreenManagerにKernel参照を追加**: レイヤー管理との統合を可能にするため
2. **pushScreen()メソッドの修正**: アプリケーション画面（ランチャー関連以外）をプッシュ時に`kernel.addLayer(LayerType.APPLICATION)`を呼び出す
3. **popScreen()メソッドの修正**: アプリケーション画面をポップ時に`kernel.removeLayer(LayerType.APPLICATION)`を呼び出す
4. **isLauncherScreen()ヘルパー追加**: HomeScreen、AppLibraryScreen等のランチャー関連画面を判定し、これらにはAPPLICATIONレイヤーを追加しない

**修正されたコード**:
- **ScreenManager.java**: Kernel参照の追加、pushScreen/popScreenでのレイヤー管理統合
- **Kernel.java**: ScreenManager初期化時にKernel参照を設定

**結果**: アプリケーション実行時にスペースキーを押すとAPPLICATIONレイヤーが検出され、正常にホーム画面に戻るようになりました。

### 7. スペースキー（ホームボタン）によるホーム画面復帰機能の実装 ✅ **解決済み** (2025年9月29日)

**問題**: アプリケーションを開いている時にスペースキーを押してもホームスクリーンに戻らない現象が発生していました。

**根本原因分析**:
- Kernelがスペースキー入力を受け取り、APPLICATIONレイヤーを検知してScreenManagerに転送することまでは正常に動作
- しかし、ScreenManagerのkeyPressed()メソッドは単純に現在のスクリーンにキーイベントを転送するだけで、**ホームボタンとしての処理が実装されていなかった**

**新しいアプローチ**:
アプリケーション自体がホームボタンを検知する必要はなく、**ScreenManager**がスペースキー（ホームボタン）を受け取って直接ホーム画面に戻る処理を実行するアプローチを採用。

**解決方法**:
1. **ScreenManager.keyPressed()メソッドの修正**: スペースキー検知時に`navigateToHome()`メソッドを呼び出すロジックを追加
2. **navigateToHome()メソッドの実装**:
   - 現在の画面がホーム画面（ランチャー関連）の場合は何もしない
   - アプリケーション画面の場合はスタックからポップしてホーム画面に戻る
   - APPLICATIONレイヤーの適切な削除処理

**修正されたコード**:
- **ScreenManager.java**:
  ```java
  // keyPressed()メソッドにスペースキー処理を追加
  if (key == ' ' || keyCode == 32) {
      System.out.println("ScreenManager: Space key detected - returning to home screen");
      navigateToHome();
      return;
  }

  // navigateToHome()メソッドの実装
  private void navigateToHome() {
      // 現在の画面がホーム画面の場合は何もしない
      if (currentScreen != null && isLauncherScreen(currentScreen)) {
          return;
      }

      // アプリケーション画面からホーム画面に戻る処理
      while (!screenStack.isEmpty() && !isLauncherScreen(getCurrentScreen())) {
          Screen poppedScreen = screenStack.pop();
          if (kernel != null) {
              kernel.removeLayer(LayerType.APPLICATION);
          }
      }
  }
  ```

**結果**: アプリケーション実行時にスペースキーを押すとScreenManagerが直接ホーム画面復帰処理を実行し、正常にホーム画面に戻るようになりました。

**追加考慮事項**: ロック画面では解除フィールド表示のためにスペースキーが必要なため、ロック画面の場合のみ例外的にスペースキーをスクリーンに転送する処理を追加しました。

### 8. ホームスクリーン完全体化 ✅ **完了** (2025年9月29日)

**実装された機能**:

**1. ページ編集機能 ✅**
- **編集モード開始時の空ページ自動追加**: 編集モードに入ると、最後に空のページが自動的に追加される
- **編集モード終了時の空ページ自動削除**: 編集モードを終了すると、末尾の空ページが自動的に削除される
- **ページ間ドラッグ機能**: 編集モードでショートカットを右端（x > 350）にドラッグすると、自動的に次のページに移動
  - 次のページが存在しない場合は新しいページを作成
  - 次のページが満杯の場合はさらに新しいページを作成
  - ドラッグ後は自動的に目標ページにスライド

**2. ホームボタンオートスクロール ✅**
- **既存実装の確認**: Kernel.javaで既に実装済み
- スペースキー（ホームボタン）押下で自動的に最初のページにスライドする機能が動作している

**3. デザイン改善 ✅**
- **アイコンサイズ縮小**: ICON_SIZE を 64px から 48px に変更
- **グリッド間隔調整**: ICON_SPACING を 20px から 15px に変更
- より密度の高い、モダンな外観を実現

**修正されたファイル**:
- `HomeScreen.java` (core/src/main/java/jp/moyashi/phoneos/core/apps/launcher/ui/HomeScreen.java):
  - `toggleEditMode()`: 編集モード開始時の空ページ追加、終了時の空ページ削除機能
  - `addEmptyPageIfNeeded()`: 編集モード用空ページ追加ヘルパー
  - `removeEmptyPagesAtEnd()`: 編集モード終了時の空ページ削除ヘルパー
  - `handleShortcutDrop()`: 右端ドラッグ時のページ間移動機能
  - `handleShortcutMoveToNextPage()`: ページ間ショートカット移動の実装
  - `findFirstEmptySlot()`: ページ内の最初の空きスロット検索
  - アイコンサイズとスペーシングの調整

**結果**: ホームスクリーンがより使いやすく、直感的な操作が可能になった。編集モードでの柔軟なページ管理とドラッグ操作によるページ間移動が実現され、ホームボタンでの迅速なナビゲーションも確保されている。

### 9. 空ページ追加機能の修正 ✅ **完了** (2025年9月29日)

**問題**: 編集モード開始時に空のページが追加されない問題があった。

**根本原因**: AppLibraryページが最後のページにある場合、元の実装では適切に空ページが追加されていなかった。また、ページの削除ロジックもAppLibraryページを考慮していなかった。

**修正内容**:

**1. `addEmptyPageIfNeeded()` メソッドの改善**:
- AppLibraryページがある場合、その前に空ページを挿入するロジックに変更
- AppLibraryページの前のページが空でない場合のみ空ページを追加
- デバッグメッセージを追加して動作を追跡可能に

**2. `removeEmptyPagesAtEnd()` メソッドの改善**:
- AppLibraryページをスキップしつつ、空の通常ページを削除
- ページインデックスの調整を適切に処理
- デバッグメッセージを追加して削除プロセスを可視化

**修正後の動作**:
- 編集モード開始時: AppLibraryページがある場合はその前に、ない場合は最後に空ページを追加
- 編集モード終了時: AppLibraryページを除いた空の通常ページを末尾から削除
- 現在のページインデックスが削除の影響を受ける場合は適切に調整

**テスト方法**:
1. ホームスクリーン上で長押しして編集モードに入る
2. コンソールに「Added empty page」メッセージが表示されることを確認
3. 編集モードを終了する
4. コンソールに空ページ削除メッセージが表示されることを確認

### 10. 編集モードでのページスライド機能復活 ✅ **完了** (2025年9月29日)

**問題**: 編集モードでページスライドジェスチャーが無効になっており、ページを切り替えられない。

**根本原因**: 編集モードで全てのページスワイプが一律に無効化されていた。これにより、ユーザーは編集モード中にページを移動できず、特に空ページを追加した後にそのページにアクセスできない問題があった。

**修正内容**:

**1. 条件付きページスワイプ有効化**:
- **従来**: 編集モード中は全てのページスワイプが無効
- **修正後**: ショートカットドラッグ中のみページスワイプを無効、それ以外では有効

**2. 修正したメソッド**:
- `handleSwipeLeft()`: `isEditing && isDragging` 時のみ無効化
- `handleSwipeRight()`: `isEditing && isDragging` 時のみ無効化
- `handleDragMove()`: ショートカットドラッグ中のみページドラッグを無効化
- `handleDragEnd()`: ショートカットドラッグ中のみページドラッグ終了を無効化

**修正後の動作**:
- **編集モードでページスライド可能**: 左右スワイプでページ間を移動できる
- **ショートカットドラッグ中は無効**: アイコンをドラッグしている間はページスライドが無効になり、誤動作を防ぐ
- **両機能の共存**: ページスライドとショートカットドラッグが適切に区別される

**ユーザビリティの向上**:
- 編集モード中に新しく追加された空ページにアクセス可能
- ページ間でのショートカット移動がより柔軟に
- 直感的な操作が可能

### 11. TODO