# MMOS Kernel Loop Architecture (V2)

## 1. 概要
MMOS (`MochiMobileOS`) のKernel更新ループを再設計し、ホスト環境（PC/Minecraft）のフレームレートに依存しない、時間的に正確で堅牢なアプリケーション実行環境を提供します。
本アーキテクチャは Android の `Choreographer` システムを採用し、仮想的なV-Sync信号に基づいた厳密なフレーム処理を実現します。

## 2. コアコンセプト

### 2.1. Android-style Choreographer (Virtual V-Sync)
Kernelの内部論理クロックを **60Hz (約16.66ms周期)** の仮想V-Syncとして定義します。
ホスト環境（Minecraftなど）のFPSが変動しても、内部的には「何回分のV-Syncが経過したか」を計算し、遅れた分を取り戻す（Catch-up）処理を行うことで、常に実時間と同期した動作を保証します。

### 2.2. VSYNC Phase Flow
仮想V-Syncごとに、以下のフェーズを厳密に順序立てて実行します。
1. **Input**: 入力イベントの配送。
2. **Animation**: V-Sync予定時刻に基づいたアニメーション更新。
3. **Traversal (Logic)**: `Screen.tick()` 等のアプリロジック更新。
4. **Draw**: 描画バッファの更新（実際に画面更新が必要な場合のみ）。

### 2.3. Catch-up (追いつき処理)
Minecraft (20fps = 50ms) のようにホストの更新頻度が低い場合、1回のホストフレーム内で Choreographer のループを **3回 (16.6ms x 3 = 49.8ms)** 回すことで、60fps相当の滑らかさと判定精度を維持します。

## 3. システムアーキテクチャ図

```mermaid
graph TD
    Host[Host Environment<br>(Standalone / Forge)] -->|draw/render| Kernel[Kernel]
    
    subgraph Choreographer [Choreographer Loop @ Virtual 60Hz]
        Sync[Virtual V-Sync Manager]
        Input[Input Phase]
        Logic[Logic Phase]
        Render[Render Phase]
        
        Sync -->|Step 16.6ms| Input
        Input -->|Dispatch| Logic
        Logic -->|Tick| Apps[Apps / Screens]
        Logic -->|If dirty| Render
    end

    subgraph Threading [Process Management]
        MainThread[UI Main Thread]
        AudioThread[High-Priority Audio Thread]
        WorkerPool[Background Worker Pool]
    end

    Kernel --> Choreographer
    Apps -.->|Request| Threading
```

## 4. 主要コンポーネントの仕様

### 4.1. Time クラス
ナノ秒精度の整数演算を用いて時間を管理し、累積誤差を排除します。
- `VSYNC_INTERVAL_NS`: 16,666,666 ns (60Hz)
- `timeScale`: 時間の進行速度を変更可能（スローモーション等）。

### 4.2. アプリケーション別ティックレート (Variable Sub-tick)
基本は60Hzで動作しますが、アプリは `Screen.getTargetTPS()` を通じて「間引き」を要求できます。
- **Game App**: 60Hz (デフォルト) → 毎回呼び出し。
- **Clock Widget**: 1Hz要求 → 60回に1回呼び出し。

### 4.3. 描画最適化 (Dirty Flag)
ロジック更新によって画面状態が変化した場合のみ再描画（`draw`）を行い、静止時のCPU/GPU負荷を最小限に抑えます。

## 5. 並行処理モデル (Future)
音声ストリーミングやネットワーク通信などの重い処理は、MMOSが管理する **「Managed Thread System」** を通じて実行されます。
- UIメインスレッド（Choreographer）をブロックしない。
- アプリの終了と連動してバックグラウンド処理も破棄する。

## 6. 実装計画
1. **`Time` クラスの実装**: 60Hzベースのナノ秒時間管理。
2. **`Choreographer` の実装**: `Kernel` 内に仮想V-Syncループ制御を導入。
3. **`Screen` インターフェース拡張**: `getTargetTPS()` による可変レート対応。
4. **ホスト側の統合**: 新しいメインループへの移行。
