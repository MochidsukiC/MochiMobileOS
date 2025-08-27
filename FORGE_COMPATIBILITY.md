# MochiMobileOS - Forge 1.20.1 互換性ドキュメント

## 技術仕様

### 対応環境
- **Minecraft Forge**: 1.20.1
- **Java**: 17 (Forge 1.20.1 要件)
- **Processing**: 4.4.4 (最新安定版)

### 設計方針

#### 1. モジュール分離
```
core/       - Processingベースの純粋なOSロジック (MOD統合時も変更なし)
standalone/ - PC単体実行用 (MOD化時は削除対象)
forge/      - 将来のForge統合モジュール (今後追加予定)
```

#### 2. 依存関係管理
- **Processing 4.4.4**: Maven Central経由で取得
- **Java 17**: Forge 1.20.1と完全互換
- **LGPL-2.1**: Processing 4のライセンス (Forge MODで使用可能)

### 将来のForge統合準備

#### A. レンダリング統合
```java
// Forge環境での描画統合案
// MinecraftのGuiGraphicsまたは独自のOpenGL描画システムと統合
public void renderToMinecraft(GuiGraphics graphics, int width, int height) {
    // Processingの描画をMinecraft環境に転写
}
```

#### B. イベントシステム
```java
// Forgeイベントシステムとの統合
@EventBusSubscriber(modid = "mochimobileos")
public class MinecraftIntegration {
    @SubscribeEvent
    public static void onKeyPressed(InputEvent.Key event) {
        // Processing OSへのキー入力転送
    }
}
```

#### C. データ永続化
```java
// Minecraft世界でのデータ保存
public class ForgeVFS extends VFS {
    // MinecraftのNBT形式でファイルシステム実装
    // または専用.datファイルでの管理
}
```

### 互換性チェックリスト

- ✅ **Java 17対応**: sourceCompatibility/targetCompatibility設定済み
- ✅ **Processing 4**: 最新版4.4.4使用
- ✅ **モジュール設計**: core/standaloneの明確分離
- ✅ **ライセンス互換性**: LGPL-2.1はForge MODで使用可能
- 🔄 **レンダリング**: Processing→Minecraft描画の変換が必要
- 🔄 **入力システム**: Minecraft入力イベント→Processing変換が必要
- 🔄 **リソース管理**: Minecraftリソースシステムとの統合が必要

### 推奨開発フロー

1. **フェーズ1**: PC単体版での機能完成
2. **フェーズ2**: `forge/`モジュール追加とブリッジ実装
3. **フェーズ3**: Minecraft内でのテスト・調整
4. **フェーズ4**: 配布用MODパッケージ作成

### 注意点

- Processing 4はJava 17+必須のため、古いForge版では使用不可
- 描画パフォーマンスの最適化が重要 (60FPS維持)
- Minecraft内でのメモリ使用量に注意