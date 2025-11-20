# Forge連携

**Forge連携** (✅ 完了):
*   MODとしての基本構造とスマートフォンアイテムが実装済み。
*   他のMODがアプリを登録するためのAPI (`PhoneAppRegistryEvent`) が機能している。
*   **PGraphics→Minecraft GUI変換システム**: KernelのPGraphicsバッファからMinecraftのNativeImageへの高速ピクセル変換を実装
*   **Kernel統合**: `Kernel.initializeForMinecraft()`による最適化された初期化処理
*   **入力システム**: Minecraftのマウス・キーボードイベントをKernel APIにバイパス（ProcessingScreen経由）
*   **Kernelインスタンス管理**:
    - ワールドロード時にKernelを起動 (`LevelEvent.Load`)
    - ワールドアンロード時にKernelをシャットダウン (`LevelEvent.Unload`)
    - ワールドごとにKernelインスタンスとデータを分離管理
*   **画面表示システム**:
    - 動的スケーリング：画面サイズに応じてスマートフォン表示を自動拡大縮小（縦横比維持）
    - 中央配置：常に画面中央に表示、20pxマージン確保
    - 画面サイズ：スタンドアロンと同じ400x600ピクセルに統一
    - PoseStack変換を使用した高品質なスケーリング
*   **ワールド別データ分離** (✅ 実装完了、テスト待ち):
    - ワールドロード時に`LevelEvent.Load`を監視
    - シングルプレイヤー：`./mochi_os_data/{world_name}/mochi_os_data/`
    - マルチプレイヤー：`./mochi_os_data/{server_address}/mochi_os_data/`
    - ワールド切り替え時に自動的に新しいKernelインスタンスを作成
    - VFS、Kernel、SmartphoneBackgroundServiceで完全サポート
    - **注意**: core自体の動作は変更なし（`VFS()`は`mochi_os_data/`を使用）、forgeでのみ分離

#### Minecraft Forge連携システム (✅ 完了)

以下のタスクが完了しました：

1. **Gradle構成の修正** (✅ 完了):
   - `forge/build.gradle`でProcessing coreを`implementation`に変更
   - Java 17を使用するための`run_with_java17.bat`スクリプトを作成
   - runClientが正常にビルドできることを確認

2. **PGraphics→Minecraft GUI変換システムの実装** (✅ 完了):
   - `SmartphoneBackgroundService.updateTextureFromKernel()`で`kernel.update()`と`kernel.render()`を呼び出し
   - PGraphicsのARGB形式からMinecraftのABGR形式への正確なピクセル変換を実装
   - `ProcessingScreen`でブロック単位の効率的な描画を実装

3. **入力バイパスシステムの実装** (✅ 完了):
   - `ProcessingScreen`でマウスイベント（clicked, released）をKernelに転送
   - キーボードイベント（keyPressed）をKernelに転送
   - Minecraft座標からMochiMobileOS座標への変換を実装
