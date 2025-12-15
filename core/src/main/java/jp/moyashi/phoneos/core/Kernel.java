package jp.moyashi.phoneos.core;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.service.*;
import jp.moyashi.phoneos.core.service.chromium.ChromiumManager;
import jp.moyashi.phoneos.core.service.chromium.ChromiumService;
import jp.moyashi.phoneos.core.service.chromium.DefaultChromiumService;
import jp.moyashi.phoneos.core.service.CoreServiceBootstrap;
import jp.moyashi.phoneos.core.service.ServiceContainer;
import jp.moyashi.phoneos.core.power.PowerManager;
import jp.moyashi.phoneos.core.lifecycle.SystemLifecycleManager;
import jp.moyashi.phoneos.core.navigation.NavigationController;
import jp.moyashi.phoneos.core.navigation.LayerController;
import jp.moyashi.phoneos.core.resource.ResourceManager;
import jp.moyashi.phoneos.core.hardware.HardwareController;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.ScreenManager;
import jp.moyashi.phoneos.core.ui.popup.PopupManager;
import jp.moyashi.phoneos.core.input.GestureManager;
import jp.moyashi.phoneos.core.input.GestureListener;
import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import jp.moyashi.phoneos.core.input.InputManager;
import jp.moyashi.phoneos.core.render.RenderPipeline;
import jp.moyashi.phoneos.core.apps.launcher.LauncherApp;
import jp.moyashi.phoneos.core.apps.settings.SettingsApp;
import jp.moyashi.phoneos.core.apps.calculator.CalculatorApp;
import jp.moyashi.phoneos.core.ui.LayerManager;
import jp.moyashi.phoneos.core.coordinate.CoordinateTransform;
import jp.moyashi.phoneos.core.event.EventBus;
import jp.moyashi.phoneos.core.event.EventListener;
import jp.moyashi.phoneos.core.event.system.SystemEvent;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PFont;
import java.util.ArrayList;
import java.util.List;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.InputStream;

/**
 * スマートフォンOSの中核となるメインカーネル。
 * PGraphics統一アーキテクチャに基づき、PApplet継承を廃止してPGraphicsバッファのみで動作する。
 * すべてのシステムサービスとScreenManagerを通じたGUIを管理する。
 * コントロールセンター用のジェスチャー処理も担当する。
 *
 * PGraphics統一アーキテクチャ:
 * - coreモジュールではPAppletを使用せず、PGraphicsバッファのみで描画
 * - 各サブモジュール（standalone/forge）でPGraphicsを環境別に変換
 *
 * @author YourName
 * @version 2.0 (PGraphics統一アーキテクチャ対応)
 */
public class Kernel implements GestureListener {
    
    /** UIと画面遷移を管理するスクリーンマネージャー */
    private ScreenManager screenManager;
    
    /** 仮想ファイルシステムサービス */
    private VFS vfs;
    
    /** 設定管理サービス */
    private SettingsManager settingsManager;

    /** テーマエンジン（デザイントークン生成） */
    private jp.moyashi.phoneos.core.ui.theme.ThemeEngine themeEngine;
    
    /** システムクロックサービス */
    private SystemClock systemClock;
    
    /** アプリケーション読み込みサービス */
    private AppLoader appLoader;
    
    /** レイアウト管理サービス */
    private LayoutManager layoutManager;
    
    /** グローバルポップアップマネージャー */
    private PopupManager popupManager;
    
    /** Kernelレベルジェスチャーマネージャー */
    private GestureManager gestureManager;
    
    /** コントロールセンター管理サービス */
    private ControlCenterManager controlCenterManager;
    
    /** 通知センター管理サービス */
    private NotificationManager notificationManager;
    
    /** ロック状態管理サービス */
    private LockManager lockManager;
    
    /** 動的レイヤー管理システム */
    private LayerManager layerManager;

    /** 統一座標変換システム */
    private CoordinateTransform coordinateTransform;

    /** 入力イベント管理システム（Phase 1リファクタリング） */
    private InputManager inputManager;

    /** 描画パイプライン管理システム（Phase 1リファクタリング） */
    private RenderPipeline renderPipeline;

    /** サービスコンテナブートストラップ（Phase 2リファクタリング） */
    private CoreServiceBootstrap serviceBootstrap;

    /** 電源管理システム（Phase 2リファクタリング） */
    private PowerManager powerManager;

    /** ライフサイクル管理システム（Phase 2リファクタリング） */
    private SystemLifecycleManager lifecycleManager;

    /** 画面遷移管理システム（Phase 3リファクタリング） */
    private NavigationController navigationController;

    /** レイヤー管理システム（Phase 3リファクタリング） */
    private LayerController layerController;

    /** リソース管理システム（Phase 3リファクタリング） */
    private ResourceManager resourceManager;

    /** ハードウェア管理システム（Phase 3リファクタリング） */
    private HardwareController hardwareController;

    /** 仮想ネットワークルーターサービス */
    private jp.moyashi.phoneos.core.service.network.VirtualRouter virtualRouter;

    /** ネットワークアダプター（統一ネットワークAPI） */
    private jp.moyashi.phoneos.core.service.network.NetworkAdapter networkAdapter;

    /** メッセージストレージサービス */
    private MessageStorage messageStorage;

    /** OSロガーサービス */
    private LoggerService logger;

    /** サービスマネージャー（アプリプロセス管理） */
    private ServiceManager serviceManager;

    /** ハードウェアバイパスAPI - モバイルデータ通信ソケット */
    private jp.moyashi.phoneos.core.service.hardware.MobileDataSocket mobileDataSocket;

    /** ハードウェアバイパスAPI - Bluetooth通信ソケット */
    private jp.moyashi.phoneos.core.service.hardware.BluetoothSocket bluetoothSocket;

    /** ハードウェアバイパスAPI - 位置情報ソケット */
    private jp.moyashi.phoneos.core.service.hardware.LocationSocket locationSocket;

    /** ハードウェアバイパスAPI - バッテリー情報 */
    private jp.moyashi.phoneos.core.service.hardware.BatteryInfo batteryInfo;

    /** バッテリー監視サービス */
    private BatteryMonitor batteryMonitor;

    /** 前回のバッテリーチェック時刻（ミリ秒） */
    private long lastBatteryCheckTime = 0;

    /** バッテリーチェック間隔（ミリ秒） */
    private static final long BATTERY_CHECK_INTERVAL = 1000; // 1秒

    /** ハードウェアバイパスAPI - カメラソケット */
    private jp.moyashi.phoneos.core.service.hardware.CameraSocket cameraSocket;

    /** ハードウェアバイパスAPI - マイクソケット */
    private jp.moyashi.phoneos.core.service.hardware.MicrophoneSocket microphoneSocket;

    /** ハードウェアバイパスAPI - スピーカーソケット */
    private jp.moyashi.phoneos.core.service.hardware.SpeakerSocket speakerSocket;

    /** ハードウェアバイパスAPI - IC通信ソケット */
    private jp.moyashi.phoneos.core.service.hardware.ICSocket icSocket;

    /** ハードウェアバイパスAPI - SIM情報 */
    private jp.moyashi.phoneos.core.service.hardware.SIMInfo simInfo;

    /** パーミッション管理サービス */
    private jp.moyashi.phoneos.core.service.permission.PermissionManager permissionManager;

    /** アクティビティ管理サービス（Intent/Activityシステム） */
    private jp.moyashi.phoneos.core.service.intent.ActivityManager activityManager;

    /** クリップボード管理サービス */
    private jp.moyashi.phoneos.core.service.clipboard.ClipboardManager clipboardManager;

    /** クリップボードサービス（TextInputProtocol用OS統一管理） */
    private jp.moyashi.phoneos.core.service.ClipboardService clipboardService;

    /** センサー管理サービス */
    private jp.moyashi.phoneos.core.service.sensor.SensorManager sensorManager;

    /** Chromium統合サービス */
    private ChromiumService chromiumService;
    /** 旧アーキテクチャ互換用のChromiumManager */
    private ChromiumManager chromiumManager;

    /** PGraphics描画バッファ（PGraphics統一アーキテクチャ） */
    private PGraphics graphics;

    /** PAppletインスタンス（PGraphics作成用、描画には使用しない） */
    private PApplet parentApplet;

    /** 画面幅 */
    public int width = 400;

    /** 画面高さ */
    public int height = 600;

    /** フレームカウント */
    public int frameCount = 0;

    /** ターゲットフレームレート */
    private int targetFrameRate = 60;

    /** レンダリング同期用ロック */
    private final Object renderLock = new Object();

    /** ピクセルキャッシュ（パフォーマンス改善） */
    private int[] pixelsCache = null;
    private volatile boolean pixelsCacheDirty = true;

    /** ワールドID（データ分離用） */
    private String worldId = null;

    /** 日本語フォント */
    private PFont japaneseFont;

    // ESCキー長押し検出用変数
    /** ESCキーが押されている時間 */
    private long escKeyPressTime = 0;

    /** ESCキーが現在押されているかどうか */
    private boolean escKeyPressed = false;

    // スリープ機能用変数
    /** スリープ状態かどうか */
    private boolean isSleeping = false;

    // 修飾キー状態管理
    /** Shiftキーが押されているかどうか */
    private boolean shiftPressed = false;

    /** Ctrlキーが押されているかどうか */
    private boolean ctrlPressed = false;

    /** Altキーが押されているかどうか */
    private boolean altPressed = false;

    /** Metaキー（Command/Windowsキー）が押されているかどうか */
    private boolean metaPressed = false;

    // ホームボタン動的優先順位システム
    /** レイヤー種別定義 */
    public enum LayerType {
        HOME_SCREEN,    // ホーム画面（最下層）
        APPLICATION,    // アプリケーション
        NOTIFICATION,   // 通知センター
        CONTROL_CENTER, // コントロールセンター
        POPUP,          // ポップアップ（最上層）
        LOCK_SCREEN     // ロック画面（例外、閉じられない）
    }

    /** 現在開いているレイヤーのスタック（後から開いたものが末尾、つまり高い優先度） */
    private List<LayerType> layerStack;
    private static final long INPUT_STAGE_DEBUG_THRESHOLD_NS = 1_000_000L;
    private static final long INPUT_STAGE_WARN_THRESHOLD_NS = 5_000_000L;

    
    /** 長押し判定時間（ミリ秒） */
    private static final long LONG_PRESS_DURATION = 2000; // 2秒

    // =========================================================================
    // PGraphics統一アーキテクチャ：独立イベントAPI
    // サブモジュールがこれらのメソッドを呼び出してKernelを操作
    // =========================================================================

    /**
     * フレーム更新処理を実行（独立API）。
     * 各サブモジュールが適切なタイミングでこのメソッドを呼び出す。
     */
    public void update() {
        frameCount++;
        long startNs = System.nanoTime();

        // Phase 1リファクタリング: InputManagerの更新処理
        if (inputManager != null) {
            inputManager.update(); // ESCキー長押し検出などを処理
        } else {
            // InputManagerが初期化されていない場合の従来処理（後方互換性）
            // ESCキー長押し検出の更新
            if (escKeyPressed) {
                long elapsedTime = System.currentTimeMillis() - escKeyPressTime;
                if (elapsedTime >= LONG_PRESS_DURATION) {
                    System.out.println("Kernel: ESCキー長押し検出 - スリープモード起動");
                    sleep(); // InputManagerではsleep()を呼び出すので統一
                    escKeyPressed = false;
                }
            }
        }

        // ServiceManagerのバックグラウンドサービス処理を呼び出し
        if (serviceManager != null) {
            serviceManager.tickBackground();
        }

        // SensorManagerの更新処理
        if (sensorManager != null) {
            ((jp.moyashi.phoneos.core.service.sensor.SensorManagerImpl) sensorManager).update();
        }

        // BatteryMonitorの定期チェック（1秒ごと）
        long currentTime = System.currentTimeMillis();
        if (batteryMonitor != null && currentTime - lastBatteryCheckTime >= BATTERY_CHECK_INTERVAL) {
            batteryMonitor.checkBatteryLevel();
            lastBatteryCheckTime = currentTime;
        }

        long chromiumStartNs = System.nanoTime();
        if (chromiumService != null) {
            chromiumService.update();
        }
        long chromiumDurationNs = System.nanoTime() - chromiumStartNs;
        if (chromiumDurationNs > 5_000_000L && logger != null) {
            logger.debug("Kernel", String.format("ChromiumService.update() slow: %.2fms", chromiumDurationNs / 1_000_000.0));
        }

        if (screenManager != null) {
            screenManager.tick();
        }

        long totalDurationNs = System.nanoTime() - startNs;
        if (totalDurationNs > 12_000_000L && logger != null) {
            logger.debug("Kernel", String.format("update() slow: %.2fms (Chromium %.2fms)",
                    totalDurationNs / 1_000_000.0,
                    chromiumDurationNs / 1_000_000.0));
        }
    }

    /**
     * PGraphicsバッファに描画を実行（独立API）。
     * すべての描画処理をPGraphicsバッファに対して実行し、サブモジュールが結果を取得可能にする。
     */
    public void render() {
        synchronized (renderLock) {
            if (graphics == null) {
                System.err.println("Kernel: PGraphicsバッファが初期化されていません");
                return;
            }

            // Phase 1リファクタリング: RenderPipelineに描画処理を委譲
            if (renderPipeline != null) {
                // RenderPipelineはbeginDraw/endDrawを内部で管理
                // スリープ処理も内部で管理
                renderPipeline.render(graphics, screenManager, themeEngine, isSleeping);

                // ピクセルキャッシュの同期
                pixelsCache = renderPipeline.getPixelsCache();
                pixelsCacheDirty = false;
                frameCount = renderPipeline.getFrameCount();

                // RenderPipelineが描画処理を完了したので、追加の描画は必要に応じてbeginDraw/endDrawで囲む
                // 以下のコードは後のPhaseで段階的にRenderPipelineに移行予定
                graphics.beginDraw();
            } else {
                // RenderPipelineが初期化されていない場合の緊急処理
                graphics.beginDraw();
                graphics.background(0);
                graphics.fill(255, 0, 0);
                graphics.textAlign(PApplet.CENTER, PApplet.CENTER);
                graphics.text("RenderPipeline not initialized!", width/2, height/2);
                // endDraw()は最後に統一して実行
            }

            // 日本語フォントを適用（全角文字表示のため）
            if (japaneseFont != null) {
                graphics.textFont(japaneseFont);
            }

            // 通知センターの描画（将来的にRenderPipelineに移行）
            if (notificationManager != null) {
                try {
                    notificationManager.draw(graphics);
                } catch (Exception e) {
                    System.err.println("Kernel: NotificationManager描画エラー: " + e.getMessage());
                }
            }

            // コントロールセンターの描画（将来的にRenderPipelineに移行）
            if (controlCenterManager != null) {
                try {
                    controlCenterManager.draw(graphics);
                } catch (Exception e) {
                    System.err.println("Kernel: ControlCenterManager描画エラー: " + e.getMessage());
                }
            }

            // ポップアップの描画（将来的にRenderPipelineに移行）
            if (popupManager != null) {
                try {
                    popupManager.draw(graphics);
                } catch (Exception e) {
                    System.err.println("Kernel: PopupManager描画エラー: " + e.getMessage());
                }
            }

            graphics.endDraw();

            // 重要: 描画完了後にピクセルキャッシュを無効化
            // これにより次のgetPixels()呼び出しで最新のピクセルデータが取得される
            pixelsCacheDirty = true;
        }
    }

    /**
     * マウスクリック処理（独立API）。
     *
     * @param x マウスX座標
     * @param y マウスY座標
     */
    public void mousePressed(int x, int y) {
        // Phase 1リファクタリング: InputManagerに処理を委譲
        if (inputManager != null) {
            inputManager.handleMousePressed(x, y, 1); // デフォルトで左ボタン
        } else {
            // InputManagerが初期化されていない場合の従来処理（後方互換性）
            long startNs = System.nanoTime();
            long stageStartNs = startNs;

            if (isSleeping) {
                if (logger != null) {
                    logger.debug("Kernel", "mousePressed ignored - device is sleeping");
                }
                return;
            }

            if (logger != null) {
                logger.debug("Kernel", "mousePressed at (" + x + ", " + y + ")");
            }

            try {
                if (popupManager != null && popupManager.hasActivePopup()) {
                    boolean popupHandled = popupManager.handleMouseClick(x, y);
                    long stageEndNs = System.nanoTime();
                    logInputStage("mousePressed", "popup", stageStartNs, stageEndNs, x, y);
                    if (popupHandled) {
                        logInputStage("mousePressed", "total", startNs, stageEndNs, x, y);
                        return;
                    }
                    stageStartNs = stageEndNs;
                }

                if (gestureManager != null) {
                    boolean gestureHandled = gestureManager.handleMousePressed(x, y);
                    long stageEndNs = System.nanoTime();
                    logInputStage("mousePressed", "gesture", stageStartNs, stageEndNs, x, y);
                    if (gestureHandled) {
                        logInputStage("mousePressed", "total", startNs, stageEndNs, x, y);
                        return;
                    }
                    stageStartNs = stageEndNs;
                }

                if (screenManager != null) {
                    if (logger != null) {
                        logger.debug("Kernel", "Forwarding mousePressed to ScreenManager");
                    }
                    screenManager.setModifierKeys(shiftPressed, ctrlPressed);
                    screenManager.mousePressed(x, y);
                    long stageEndNs = System.nanoTime();
                    logInputStage("mousePressed", "screen", stageStartNs, stageEndNs, x, y);
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Kernel", "mousePressed処理エラー", e);
                }
                System.err.println("Kernel: mousePressed処理エラー: " + e.getMessage());
                e.printStackTrace();
            } finally {
                long endNs = System.nanoTime();
                logInputStage("mousePressed", "total", startNs, endNs, x, y);
            }
        }
    }

    /**
     * マウス離し処理（独立API）。
     *
     * @param x マウスX座標
     * @param y マウスY座標
     */
    public void mouseReleased(int x, int y) {
        long startNs = System.nanoTime();
        long stageStartNs = startNs;

        if (isSleeping) {
            if (logger != null) {
                logger.debug("Kernel", "mouseReleased ignored - device is sleeping");
            }
            return;
        }

        if (logger != null) {
            logger.debug("Kernel", "mouseReleased at (" + x + ", " + y + ")");
        }

        try {
            if (gestureManager != null) {
                gestureManager.handleMouseReleased(x, y);
                long stageEndNs = System.nanoTime();
                logInputStage("mouseReleased", "gesture", stageStartNs, stageEndNs, x, y);
                stageStartNs = stageEndNs;
            }

            if (screenManager != null) {
                if (logger != null) {
                    logger.debug("Kernel", "Forwarding mouseReleased to ScreenManager");
                }
                screenManager.setModifierKeys(shiftPressed, ctrlPressed);
                screenManager.mouseReleased(x, y);
                long stageEndNs = System.nanoTime();
                logInputStage("mouseReleased", "screen", stageStartNs, stageEndNs, x, y);
            }
        } catch (Exception e) {
            if (logger != null) {
                logger.error("Kernel", "mouseReleased処理エラー", e);
            }
            System.err.println("Kernel: mouseReleased処理エラー: " + e.getMessage());
            e.printStackTrace();
        } finally {
            long endNs = System.nanoTime();
            logInputStage("mouseReleased", "total", startNs, endNs, x, y);
        }
    }

    /**
     * マウスドラッグ処理（独立API）。
     * ジェスチャー認識にとって重要な機能です。
     *
     * @param x マウスX座標
     * @param y マウスY座標
     */
    public void mouseDragged(int x, int y) {
        long startNs = System.nanoTime();
        long stageStartNs = startNs;

        if (isSleeping) {
            if (logger != null) {
                logger.debug("Kernel", "mouseDragged ignored - device is sleeping");
            }
            return;
        }

        try {
            if (gestureManager != null) {
                gestureManager.handleMouseDragged(x, y);
                long stageEndNs = System.nanoTime();
                logInputStage("mouseDragged", "gesture", stageStartNs, stageEndNs, x, y);
                stageStartNs = stageEndNs;
            }

            if (screenManager != null) {
                screenManager.setModifierKeys(shiftPressed, ctrlPressed);
                screenManager.mouseDragged(x, y);
                long stageEndNs = System.nanoTime();
                logInputStage("mouseDragged", "screen", stageStartNs, stageEndNs, x, y);
            }
        } catch (Exception e) {
            System.err.println("Kernel: mouseDragged処理エラー: " + e.getMessage());
            e.printStackTrace();
        } finally {
            long endNs = System.nanoTime();
            logInputStage("mouseDragged", "total", startNs, endNs, x, y);
        }
    }

    /**
     * マウス移動処理（独立API）。
     * ホバーエフェクトやカーソル位置の更新に使用。
     *
     * @param x マウスX座標
     * @param y マウスY座標
     */
    public void mouseMoved(int x, int y) {
        long startNs = System.nanoTime();

        if (isSleeping) {
            return;
        }

        try {
            // ScreenManagerに転送
            if (screenManager != null) {
                screenManager.mouseMoved(x, y);
            }
        } catch (Exception e) {
            System.err.println("Kernel: mouseMoved処理エラー: " + e.getMessage());
            e.printStackTrace();
        } finally {
            long endNs = System.nanoTime();
            // mouseMoved()は頻繁に呼ばれるためログは出力しない
        }
    }

    /**
     * マウスホイール処理（独立API）。
     *
     * @param x マウスX座標
     * @param y マウスY座標
     * @param delta スクロール量（正の値：下スクロール、負の値：上スクロール）
     */
    public void mouseWheel(int x, int y, float delta) {
        long startNs = System.nanoTime();
        long stageStartNs = startNs;

        if (isSleeping) {
            if (logger != null) {
                logger.debug("Kernel", "mouseWheel ignored - device is sleeping");
            }
            return;
        }

        if (logger != null) {
            logger.debug("Kernel", "mouseWheel at (" + x + ", " + y + ") delta=" + delta);
        }

        try {
            if (screenManager != null) {
                screenManager.mouseWheel(x, y, delta);
                long stageEndNs = System.nanoTime();
                logInputStage("mouseWheel", "screen", stageStartNs, stageEndNs, x, y);
            }
        } catch (Exception e) {
            System.err.println("Kernel: mouseWheel処理エラー: " + e.getMessage());
            if (logger != null) {
                logger.error("Kernel", "mouseWheel処理エラー", e);
            }
            e.printStackTrace();
        } finally {
            long endNs = System.nanoTime();
            logInputStage("mouseWheel", "total", startNs, endNs, x, y);
        }
    }

    private void logInputStage(String event, String stage, long startNs, long endNs, int x, int y) {
        if (logger == null) {
            return;
        }
        long durationNs = endNs - startNs;
        if (durationNs <= INPUT_STAGE_DEBUG_THRESHOLD_NS) {
            return;
        }
        double ms = durationNs / 1_000_000.0;
        String message = String.format("%s %s latency=%.3fms coord=(%d,%d)",
                event, stage, ms, x, y);
        if (durationNs >= INPUT_STAGE_WARN_THRESHOLD_NS) {
            logger.warn("KernelInput", message);
        } else {
            logger.debug("KernelInput", message);
        }
    }

    /**
     * キー押下処理（独立API）。
     *
     * @param key 押されたキー文字
     * @param keyCode キーコード
     */
    public void keyPressed(char key, int keyCode) {
        // Phase 1リファクタリング: InputManagerに処理を委譲
        // ただし、ESCとスペースキーは元のKernelで処理（動作しないため）
        if (inputManager != null) {
            // ESCキー（27）とスペースキー（32）以外はInputManagerで処理
            if (keyCode != 27 && keyCode != 32 && key != ' ') {
                inputManager.handleKeyPressed(key, keyCode);

                // 修飾キーの状態を同期（互換性のため）
                InputManager.ModifierKeyState modifierState = inputManager.getModifierState();
                shiftPressed = modifierState.isShiftPressed();
                ctrlPressed = modifierState.isCtrlPressed();
                altPressed = modifierState.isAltPressed();
                metaPressed = modifierState.isMetaPressed();
                return;
            }

            // ESCとスペースは下の従来処理で実行
            // 修飾キーの状態だけ同期
            InputManager.ModifierKeyState modifierState = inputManager.getModifierState();
            shiftPressed = modifierState.isShiftPressed();
            ctrlPressed = modifierState.isCtrlPressed();
            altPressed = modifierState.isAltPressed();
            metaPressed = modifierState.isMetaPressed();
        }

        // InputManagerが初期化されていない場合の従来処理（後方互換性）
        System.out.println("Kernel: keyPressed - key: '" + key + "', keyCode: " + keyCode);
        System.out.println("Kernel: [MODIFIER STATE] shift=" + shiftPressed + ", ctrl=" + ctrlPressed + ", alt=" + altPressed + ", meta=" + metaPressed);

        // LoggerServiceでデバッグログを記録（VFS保存用）
        if (logger != null) {
            logger.debug("Kernel", "keyPressed - key='" + key + "' (charCode=" + (int)key + "), keyCode=" + keyCode);
        }

        try {
            // 修飾キーの状態を追跡
            if (keyCode == 16) { // Shift key code
                shiftPressed = true;
                System.out.println("Kernel: *** Shift key pressed - shiftPressed=true ***");
                if (logger != null) {
                    logger.debug("Kernel", "*** SHIFT キー検出 (keyCode=16) - shiftPressed=true ***");
                }
                // 修飾キーの状態をすぐにScreenManagerに伝播
                if (screenManager != null) {
                    screenManager.setModifierKeys(shiftPressed, ctrlPressed);
                }
            }
            if (keyCode == 17) { // Ctrl key code
                ctrlPressed = true;
                System.out.println("Kernel: *** Ctrl key pressed - ctrlPressed=true ***");
                if (logger != null) {
                    logger.debug("Kernel", "*** CTRL キー検出 (keyCode=17) - ctrlPressed=true ***");
                }
                // 修飾キーの状態をすぐにScreenManagerに伝播
                if (screenManager != null) {
                    screenManager.setModifierKeys(shiftPressed, ctrlPressed);
                }
            }
            if (keyCode == 18) { // Alt key code
                altPressed = true;
                System.out.println("Kernel: *** Alt key pressed - altPressed=true ***");
                if (logger != null) {
                    logger.debug("Kernel", "*** ALT キー検出 (keyCode=18) - altPressed=true ***");
                }
                // 修飾キーの状態をすぐにScreenManagerに伝播
                if (screenManager != null) {
                    screenManager.setModifierKeys(shiftPressed, ctrlPressed);
                }
            }
            if (keyCode == 91 || keyCode == 157) { // Meta key code (Command on Mac, Windows key on Windows)
                metaPressed = true;
                System.out.println("Kernel: *** Meta key pressed - metaPressed=true ***");
                if (logger != null) {
                    logger.debug("Kernel", "*** META キー検出 (keyCode=" + keyCode + ") - metaPressed=true ***");
                }
                // 修飾キーの状態をすぐにScreenManagerに伝播
                if (screenManager != null) {
                    screenManager.setModifierKeys(shiftPressed, ctrlPressed);
                }
            }

            // ESCキーの特別処理（スリープ中でも許可）
            if (keyCode == 27) { // ESC key code
                escKeyPressed = true;
                escKeyPressTime = System.currentTimeMillis();
                return;
            }

            // スリープ中はESC以外のすべてのキー入力を拒否
            if (isSleeping) {
                System.out.println("Kernel: keyPressed ignored - device is sleeping (only ESC is allowed)");
                return;
            }

            // 'q'または'Q'でアプリ終了
            if (key == 'q' || key == 'Q') {
                System.out.println("Kernel: Q key pressed - initiating shutdown");
                shutdown();
                return;
            }

            // スペースキーは通常のキー入力として扱う（ホームボタン機能はプラットフォーム層で実装）
            // Standalone: Ctrl+Space または HomeButtonWindow
            // Forge: Ctrl+Space または 画面上ホームボタン

            // バックスペースキー処理（テキスト入力にフォーカスがある場合）
            if (keyCode == 8) { // Backspace key
                jp.moyashi.phoneos.core.ui.components.TextInputProtocol textInput = null;
                if (screenManager != null) {
                    textInput = screenManager.getFocusedTextInput();
                }
                if (textInput != null) {
                    textInput.deleteBackward();
                    System.out.println("Kernel: Backspace - deleted backward");
                    return; // イベント消費
                }
            }

            // Ctrl+C/V/X/A検出（OS統一クリップボード管理）
            if (ctrlPressed && !shiftPressed && !altPressed && !metaPressed) {
                jp.moyashi.phoneos.core.ui.components.TextInputProtocol textInput = null;
                if (screenManager != null) {
                    textInput = screenManager.getFocusedTextInput();
                }

                if (textInput != null) {
                    if (keyCode == 67) { // Ctrl+C
                        if (textInput.hasSelection()) {
                            String selectedText = textInput.getSelectedText();
                            if (selectedText != null && !selectedText.isEmpty()) {
                                clipboardService.copy(selectedText);
                                System.out.println("Kernel: Ctrl+C - copied: " + selectedText);
                            }
                        }
                        return; // イベント消費
                    } else if (keyCode == 86) { // Ctrl+V
                        String pasteText = clipboardService.paste();
                        if (pasteText != null && !pasteText.isEmpty()) {
                            textInput.replaceSelection(pasteText);
                            System.out.println("Kernel: Ctrl+V - pasted: " + pasteText);
                        }
                        return; // イベント消費
                    } else if (keyCode == 88) { // Ctrl+X
                        if (textInput.hasSelection()) {
                            String selectedText = textInput.getSelectedText();
                            if (selectedText != null && !selectedText.isEmpty()) {
                                clipboardService.copy(selectedText);
                                textInput.deleteSelection();
                                System.out.println("Kernel: Ctrl+X - cut: " + selectedText);
                            }
                        }
                        return; // イベント消費
                    } else if (keyCode == 65) { // Ctrl+A
                        textInput.selectAll();
                        System.out.println("Kernel: Ctrl+A - selected all");
                        return; // イベント消費
                    }
                }
            }

            // 通常のキー処理をスクリーンマネージャーに転送
            // 修飾キーの状態も一緒に送る
            if (screenManager != null) {
                screenManager.setModifierKeys(shiftPressed, ctrlPressed);
                screenManager.keyPressed(key, keyCode);
            }
        } catch (Exception e) {
            System.err.println("Kernel: keyPressed処理エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * キー離し処理（独立API）。
     *
     * @param key 離されたキー文字
     * @param keyCode キーコード
     */
    public void keyReleased(char key, int keyCode) {
        // Phase 1リファクタリング: InputManagerに処理を委譲
        // ただし、ESCキーは元のKernelで処理（動作しないため）
        if (inputManager != null) {
            // ESCキー（27）以外はInputManagerで処理
            if (keyCode != 27) {
                inputManager.handleKeyReleased(key, keyCode);

                // 修飾キーの状態を同期（互換性のため）
                InputManager.ModifierKeyState modifierState = inputManager.getModifierState();
                shiftPressed = modifierState.isShiftPressed();
                ctrlPressed = modifierState.isCtrlPressed();
                altPressed = modifierState.isAltPressed();
                metaPressed = modifierState.isMetaPressed();
                return;
            }

            // ESCは下の従来処理で実行
            // 修飾キーの状態だけ同期
            InputManager.ModifierKeyState modifierState = inputManager.getModifierState();
            shiftPressed = modifierState.isShiftPressed();
            ctrlPressed = modifierState.isCtrlPressed();
            altPressed = modifierState.isAltPressed();
            metaPressed = modifierState.isMetaPressed();
        }

        // InputManagerが初期化されていない場合の従来処理（後方互換性）
        System.out.println("Kernel: keyReleased - key: '" + key + "', keyCode: " + keyCode);

        // 修飾キーのリリースを追跡
        if (keyCode == 16) { // Shift key code
            shiftPressed = false;
            System.out.println("Kernel: *** Shift key released - shiftPressed=false ***");
            // 修飾キーの状態をすぐにScreenManagerに伝播
            if (screenManager != null) {
                screenManager.setModifierKeys(shiftPressed, ctrlPressed);
            }
        }
        if (keyCode == 17) { // Ctrl key code
            ctrlPressed = false;
            System.out.println("Kernel: *** Ctrl key released - ctrlPressed=false ***");
            // 修飾キーの状態をすぐにScreenManagerに伝播
            if (screenManager != null) {
                screenManager.setModifierKeys(shiftPressed, ctrlPressed);
            }
        }
        if (keyCode == 18) { // Alt key code
            altPressed = false;
            System.out.println("Kernel: *** Alt key released - altPressed=false ***");
            // 修飾キーの状態をすぐにScreenManagerに伝播
            if (screenManager != null) {
                screenManager.setModifierKeys(shiftPressed, ctrlPressed);
            }
        }
        if (keyCode == 91 || keyCode == 157) { // Meta key code
            metaPressed = false;
            System.out.println("Kernel: *** Meta key released - metaPressed=false ***");
            // 修飾キーの状態をすぐにScreenManagerに伝播
            if (screenManager != null) {
                screenManager.setModifierKeys(shiftPressed, ctrlPressed);
            }
        }

        // ESCキーの処理（スリープ中でも許可）
        if (keyCode == 27) { // ESC key code
            if (escKeyPressed) {
                long pressDuration = System.currentTimeMillis() - escKeyPressTime;
                escKeyPressed = false;

                System.out.println("Kernel: ESC key released after " + pressDuration + "ms");

                // 長押し判定時間未満の場合はスリープ/解除の切り替え
                if (pressDuration < LONG_PRESS_DURATION) {
                    if (isSleeping) {
                        // スリープ解除
                        wake();
                    } else {
                        // スリープ
                        sleep();
                    }
                }
                // 長押しの場合はupdate()でシャットダウンが実行される
            }
            return;
        }

        // スリープ中はESC以外のすべてのキー入力を拒否
        if (isSleeping) {
            System.out.println("Kernel: keyReleased ignored - device is sleeping (only ESC is allowed)");
            return;
        }

        // 通常のキー処理をスクリーンマネージャーに転送
        if (screenManager != null) {
            screenManager.keyReleased(key, keyCode);
        }
    }

    /**
     * PGraphicsバッファを取得（独立API）。
     * サブモジュールがこのバッファの内容を各環境で描画する。
     *
     * @return PGraphicsバッファインスタンス
     */
    public PGraphics getGraphics() {
        return graphics;
    }

    /**
     * PGraphicsバッファのピクセル配列を取得（独立API・キャッシュ付き）。
     * forge等でピクセルレベルでの処理が必要な場合に使用。
     * パフォーマンス改善: キャッシュを使用してロック競合とコピーコストを削減。
     *
     * @return ピクセル配列
     */
    public int[] getPixels() {
        // キャッシュが有効な場合は即座に返す（ロック不要）
        if (!pixelsCacheDirty && pixelsCache != null) {
            return pixelsCache;
        }

        // キャッシュが無効な場合のみロックを取得
        synchronized (renderLock) {
            if (graphics == null) {
                return new int[width * height];
            }

            // ダブルチェック: 他のスレッドが既に更新した可能性
            if (!pixelsCacheDirty && pixelsCache != null) {
                return pixelsCache;
            }

            graphics.loadPixels();

            // キャッシュ配列を初期化または再利用
            if (pixelsCache == null || pixelsCache.length != graphics.pixels.length) {
                pixelsCache = new int[graphics.pixels.length];
            }

            // 配列をコピー（clone()より高速なSystem.arraycopy()を使用）
            System.arraycopy(graphics.pixels, 0, pixelsCache, 0, graphics.pixels.length);
            pixelsCacheDirty = false;

            return pixelsCache;
        }
    }

    // =========================================================================
    // 以下、旧PAppletベースのメソッド（段階的に削除予定）
    // =========================================================================

    /**
     * Kernelを初期化する（PGraphics統一アーキテクチャ）。
     * PAppletインスタンスを受け取り、PGraphicsバッファを作成して初期化を行う。
     *
     * @param applet PGraphics作成用のPAppletインスタンス
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     */
    public void initialize(PApplet applet, int screenWidth, int screenHeight) {
        this.parentApplet = applet;
        this.width = screenWidth;
        this.height = screenHeight;

        System.out.println("=== MochiMobileOS カーネル初期化 ===");
        System.out.println("📱 Kernel: PGraphics buffer created (" + width + "x" + height + ")");

        // PGraphicsバッファを作成
        this.graphics = applet.createGraphics(width, height);

        // 内部初期化を実行
        setup();
    }

    /**
     * Minecraft環境用の初期化（forge用）。
     * PAppletのヘッドレスインスタンスを作成してPGraphicsバッファを作成する。
     *
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     */
    public void initializeForMinecraft(int screenWidth, int screenHeight) {
        initializeForMinecraft(screenWidth, screenHeight, null);
    }

    /**
     * Minecraft環境用の初期化（forge用）。ワールドID指定版。
     * PAppletのヘッドレスインスタンスを作成してPGraphicsバッファを作成する。
     *
     * @param screenWidth 画面幅
     * @param screenHeight 画面高さ
     * @param worldId ワールドID（データ分離用）
     */
    public void initializeForMinecraft(int screenWidth, int screenHeight, String worldId) {
        this.width = screenWidth;
        this.height = screenHeight;
        this.worldId = worldId;

        System.out.println("=== MochiMobileOS カーネル初期化 (Minecraft環境) ===");
        System.out.println("📱 Kernel: Creating PGraphics buffer directly (" + width + "x" + height + ")");

        try {
            // PAppletを使わず、PGraphicsを直接作成（リフレクション使用）
            // Processing内部では "processing.awt.PGraphicsJava2D" が使用される
            Class<?> pgClass = Class.forName("processing.awt.PGraphicsJava2D");
            this.graphics = (PGraphics) pgClass.getDeclaredConstructor().newInstance();

            // PGraphicsのサイズを設定
            this.graphics.setSize(width, height);

            // 親PAppletを設定（一部の描画メソッドで必要）
            this.parentApplet = new PApplet();
            this.graphics.setParent(parentApplet);

            // 重要: ScreenManagerがscreen.setup(currentPApplet.g)を呼ぶために、
            // parentApplet.gにgraphicsを設定する必要がある
            this.parentApplet.g = this.graphics;

        } catch (Exception e) {
            System.err.println("Failed to create PGraphics directly: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize PGraphics", e);
        }

        // 内部初期化を実行
        setup();
    }

    /**
     * OSカーネルとすべてのサービスを初期化する（内部メソッド）。
     * PGraphics統一アーキテクチャ対応版。
     */
    private void setup() {
        System.out.println("Kernel: OSサービスを初期化中...");
        System.out.println("Kernel: フレームレートを60FPSに設定");

        // Phase 4リファクタリング: イベントバスの初期化
        System.out.println("=== Phase 4: イベントバスシステム初期化開始 ===");
        EventBus eventBus = EventBus.getInstance();
        eventBus.setDebugMode(false); // デバッグモードは必要に応じて有効化

        // システムイベントリスナーを登録（例）
        eventBus.register(SystemEvent.class, new EventListener<SystemEvent>() {
            @Override
            public void onEvent(SystemEvent event) {
                System.out.println("System Event: " + event.getType() + " - " + event.getMessage());
            }
        });

        // システム起動イベントを発行
        eventBus.post(SystemEvent.startup(this));
        System.out.println("✅ イベントバスシステム初期化完了");

        // Phase 2リファクタリング: ServiceContainerの初期化
        System.out.println("=== Phase 2: サービスコンテナ初期化開始 ===");
        serviceBootstrap = new CoreServiceBootstrap(this);
        boolean servicesInitialized = serviceBootstrap.initialize(graphics);

        if (servicesInitialized) {
            System.out.println("✅ サービスコンテナ初期化完了: " +
                             serviceBootstrap.getServiceCount() + "個のサービスが登録されました");

            // ServiceContainerから主要サービスを取得
            powerManager = serviceBootstrap.tryGetService(PowerManager.class);
            lifecycleManager = serviceBootstrap.tryGetService(SystemLifecycleManager.class);

            if (powerManager != null) {
                System.out.println("  -> PowerManager: 初期化成功");
            }
            if (lifecycleManager != null) {
                System.out.println("  -> SystemLifecycleManager: 初期化成功");
                lifecycleManager.start(); // システム開始
            }
        } else {
            System.err.println("⚠️ サービスコンテナの初期化に失敗しました。従来の初期化方法を使用します。");
        }

        // Phase 3リファクタリング: 画面遷移とリソース管理の初期化
        System.out.println("=== Phase 3: 画面遷移・リソース管理システム初期化開始 ===");

        // NavigationController初期化
        System.out.println("  -> NavigationController作成中...");
        navigationController = new NavigationController(this);

        // LayerController初期化（従来のlayerStack処理を移行）
        System.out.println("  -> LayerController作成中...");
        layerController = new LayerController(this);

        // ResourceManager初期化（日本語フォント処理を移行）
        System.out.println("  -> ResourceManager作成中...");
        resourceManager = new ResourceManager(logger);
        if (parentApplet != null) {
            resourceManager.setApplet(parentApplet);
        }

        // HardwareController初期化（ハードウェアバイパスAPIを統合）
        System.out.println("  -> HardwareController作成中...");
        hardwareController = new HardwareController();

        System.out.println("✅ Phase 3システム初期化完了");

        // 動的レイヤー管理システムを初期化（後方互換性のため残す）
        System.out.println("  -> 動的レイヤー管理システム作成中...");
        layerStack = new ArrayList<>();
        layerStack.add(LayerType.HOME_SCREEN); // 最初は常にホーム画面

        // 統一座標変換システムを初期化
        System.out.println("  -> 統一座標変換システム作成中...");
        coordinateTransform = new CoordinateTransform(width, height);

        // 基本的なサービスの早期初期化（DIコンテナの前提条件）
        System.out.println("  -> VFS（仮想ファイルシステム）作成中...");
        if (worldId != null && !worldId.isEmpty()) {
            System.out.println("     World ID: " + worldId);
        }
        vfs = new VFS(worldId);

        // DIコンテナから各サービスを取得
        if (serviceBootstrap != null && serviceBootstrap.isInitialized()) {
            System.out.println("=== DIコンテナからサービスを取得 ===");

            // LoggerService取得
            logger = serviceBootstrap.tryGetService(LoggerService.class);
            if (logger != null) {
                System.out.println("  -> LoggerService: DIコンテナから取得成功");
                logger.setLogLevel(jp.moyashi.phoneos.core.service.LoggerService.LogLevel.DEBUG);
                logger.info("Kernel", "=== MochiMobileOS カーネル初期化開始 ===");
                logger.info("Kernel", "画面サイズ: " + width + "x" + height);
                if (worldId != null && !worldId.isEmpty()) {
                    logger.info("Kernel", "World ID: " + worldId);
                }
            } else {
                System.out.println("  -> LoggerService: DIコンテナから取得失敗、直接作成");
                logger = new LoggerService(vfs);
                logger.setLogLevel(jp.moyashi.phoneos.core.service.LoggerService.LogLevel.DEBUG);
            }

            // SystemClock取得
            systemClock = serviceBootstrap.tryGetService(SystemClock.class);
            if (systemClock != null) {
                System.out.println("  -> SystemClock: DIコンテナから取得成功");
            }

            // NotificationManager取得
            notificationManager = serviceBootstrap.tryGetService(NotificationManager.class);
            if (notificationManager != null) {
                System.out.println("  -> NotificationManager: DIコンテナから取得成功");
            }

            // AppLoader取得
            appLoader = serviceBootstrap.tryGetService(AppLoader.class);
            if (appLoader != null) {
                System.out.println("  -> AppLoader: DIコンテナから取得成功");
            }

            // LayoutManager取得
            layoutManager = serviceBootstrap.tryGetService(LayoutManager.class);
            if (layoutManager != null) {
                System.out.println("  -> LayoutManager: DIコンテナから取得成功");
            }

            // SettingsManager取得
            settingsManager = serviceBootstrap.tryGetService(SettingsManager.class);
            if (settingsManager != null) {
                System.out.println("  -> SettingsManager: DIコンテナから取得成功");
            }

            // ThemeEngine取得
            themeEngine = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.ui.theme.ThemeEngine.class);
            if (themeEngine != null) {
                System.out.println("  -> ThemeEngine: DIコンテナから取得成功");
            }

            // ScreenManager取得
            screenManager = serviceBootstrap.tryGetService(ScreenManager.class);
            if (screenManager != null) {
                System.out.println("  -> ScreenManager: DIコンテナから取得成功");
            }

            // PopupManager取得
            popupManager = serviceBootstrap.tryGetService(PopupManager.class);
            if (popupManager != null) {
                System.out.println("  -> PopupManager: DIコンテナから取得成功");
            }

            // GestureManager取得
            gestureManager = serviceBootstrap.tryGetService(GestureManager.class);
            if (gestureManager != null) {
                System.out.println("  -> GestureManager: DIコンテナから取得成功");
            }

            // InputManager取得
            inputManager = serviceBootstrap.tryGetService(InputManager.class);
            if (inputManager != null) {
                System.out.println("  -> InputManager: DIコンテナから取得成功");
            }

            // RenderPipeline取得
            renderPipeline = serviceBootstrap.tryGetService(RenderPipeline.class);
            if (renderPipeline != null) {
                System.out.println("  -> RenderPipeline: DIコンテナから取得成功");
            }

            // ControlCenterManager取得
            controlCenterManager = serviceBootstrap.tryGetService(ControlCenterManager.class);
            if (controlCenterManager != null) {
                System.out.println("  -> ControlCenterManager: DIコンテナから取得成功");
            }

            // LockManager取得
            lockManager = serviceBootstrap.tryGetService(LockManager.class);
            if (lockManager != null) {
                System.out.println("  -> LockManager: DIコンテナから取得成功");
            }

            // VirtualRouter取得
            virtualRouter = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.service.network.VirtualRouter.class);
            if (virtualRouter != null) {
                System.out.println("  -> VirtualRouter: DIコンテナから取得成功");
            }

            // MessageStorage取得
            messageStorage = serviceBootstrap.tryGetService(MessageStorage.class);
            if (messageStorage != null) {
                System.out.println("  -> MessageStorage: DIコンテナから取得成功");
            }

            // ChromiumService取得（setChromiumService()で事前に設定されていない場合のみDIコンテナから取得）
            if (chromiumService == null) {
                chromiumService = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.service.chromium.ChromiumService.class);
                if (chromiumService != null) {
                    System.out.println("  -> ChromiumService: DIコンテナから取得成功");
                }
            } else {
                System.out.println("  -> ChromiumService: setChromiumService()で事前設定済み（DIコンテナをスキップ）");
            }

            // ハードウェアバイパスAPI取得
            mobileDataSocket = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.service.hardware.MobileDataSocket.class);
            if (mobileDataSocket != null) {
                System.out.println("  -> MobileDataSocket: DIコンテナから取得成功");
            }

            bluetoothSocket = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.service.hardware.BluetoothSocket.class);
            if (bluetoothSocket != null) {
                System.out.println("  -> BluetoothSocket: DIコンテナから取得成功");
            }

            locationSocket = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.service.hardware.LocationSocket.class);
            if (locationSocket != null) {
                System.out.println("  -> LocationSocket: DIコンテナから取得成功");
            }

            batteryInfo = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.service.hardware.BatteryInfo.class);
            if (batteryInfo != null) {
                System.out.println("  -> BatteryInfo: DIコンテナから取得成功");
            }

            // 追加のハードウェアバイパスAPI取得
            cameraSocket = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.service.hardware.CameraSocket.class);
            if (cameraSocket != null) {
                System.out.println("  -> CameraSocket: DIコンテナから取得成功");
            }

            microphoneSocket = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.service.hardware.MicrophoneSocket.class);
            if (microphoneSocket != null) {
                System.out.println("  -> MicrophoneSocket: DIコンテナから取得成功");
            }

            speakerSocket = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.service.hardware.SpeakerSocket.class);
            if (speakerSocket != null) {
                System.out.println("  -> SpeakerSocket: DIコンテナから取得成功");
            }

            icSocket = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.service.hardware.ICSocket.class);
            if (icSocket != null) {
                System.out.println("  -> ICSocket: DIコンテナから取得成功");
            }

            simInfo = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.service.hardware.SIMInfo.class);
            if (simInfo != null) {
                System.out.println("  -> SIMInfo: DIコンテナから取得成功");
            }

            // SensorManager取得
            sensorManager = serviceBootstrap.tryGetService(jp.moyashi.phoneos.core.service.sensor.SensorManager.class);
            if (sensorManager != null) {
                System.out.println("  -> SensorManager: DIコンテナから取得成功");
            }

            // BatteryMonitor取得
            batteryMonitor = serviceBootstrap.tryGetService(BatteryMonitor.class);
            if (batteryMonitor != null) {
                System.out.println("  -> BatteryMonitor: DIコンテナから取得成功");
            }
        } else {
            // フォールバック: 従来の初期化
            System.out.println("⚠️ DIコンテナが利用できません。従来の初期化を実行します。");
            logger = new LoggerService(vfs);
            logger.setLogLevel(jp.moyashi.phoneos.core.service.LoggerService.LogLevel.DEBUG);
            logger.info("Kernel", "=== MochiMobileOS カーネル初期化開始（フォールバック） ===");
        }

        // サービスマネージャーは直接作成（将来DI化予定）
        System.out.println("  -> サービスマネージャー作成中...");
        serviceManager = new ServiceManager(this);
        serviceManager.initialize();
        if (logger != null) {
            logger.info("Kernel", "サービスマネージャー初期化完了");
        }

        // 日本語フォントの初期化（Phase 3: ResourceManager経由）
        logger.info("Kernel", "日本語フォントを初期化中...");
        if (resourceManager != null) {
            japaneseFont = resourceManager.getJapaneseFont();
            if (japaneseFont != null) {
                logger.info("Kernel", "日本語フォント (Noto Sans JP) を正常に読み込みました");
            } else {
                logger.warn("Kernel", "日本語フォントの読み込みに失敗しました。デフォルトフォントを使用します");
            }
        } else {
            // ResourceManagerが利用できない場合は従来の方法
            japaneseFont = loadJapaneseFont();
        }

        // SettingsManagerの初期化（DIで取得できなかった場合）
        if (settingsManager == null) {
            System.out.println("  -> 設定マネージャー作成中（フォールバック）...");
            settingsManager = new SettingsManager(vfs);
        }

        // ThemeEngineの初期化（DIで取得できなかった場合）
        if (themeEngine == null) {
            System.out.println("  -> テーマエンジン作成中（フォールバック）...");
            themeEngine = new jp.moyashi.phoneos.core.ui.theme.ThemeEngine(settingsManager);
        }
        jp.moyashi.phoneos.core.ui.theme.ThemeContext.setTheme(themeEngine);
        
        // SystemClockの初期化（DIで取得できなかった場合）
        if (systemClock == null) {
            System.out.println("  -> システムクロック作成中（フォールバック）...");
            systemClock = new SystemClock();
        }

        // AppLoaderの初期化（DIで取得できなかった場合）
        if (appLoader == null) {
            System.out.println("  -> アプリケーションローダー作成中（フォールバック）...");
            appLoader = new AppLoader(vfs);
        }

        // アプリケーションをスキャンして読み込む
        System.out.println("  -> 外部アプリケーションをスキャン中...");
        appLoader.scanForApps();

        // LayoutManagerの初期化（DIで取得できなかった場合）
        if (layoutManager == null) {
            System.out.println("  -> レイアウト管理サービス作成中（フォールバック）...");
            layoutManager = new LayoutManager(vfs, appLoader);
        }
        
        // PopupManagerの初期化（DIで取得できなかった場合）
        if (popupManager == null) {
            System.out.println("  -> グローバルポップアップマネージャー作成中（フォールバック）...");
            popupManager = new PopupManager();
        }

        // Phase 1リファクタリング: 入力管理と描画パイプラインの初期化
        // InputManagerの初期化（DIで取得できなかった場合）
        if (inputManager == null) {
            System.out.println("  -> 入力管理システム作成中（フォールバック）...");
            inputManager = new InputManager(this);
        }
        logger.info("Kernel", "InputManager初期化完了");

        // RenderPipelineの初期化（DIで取得できなかった場合）
        if (renderPipeline == null) {
            System.out.println("  -> 描画パイプライン作成中（フォールバック）...");
            renderPipeline = new RenderPipeline(this, width, height);
        }
        logger.info("Kernel", "RenderPipeline初期化完了");

        // GestureManagerの初期化（DIで取得できなかった場合）
        if (gestureManager == null) {
            System.out.println("  -> Kernelレベルジェスチャーマネージャー作成中（フォールバック）...");
            gestureManager = new GestureManager(logger);
        }

        // ControlCenterManagerの初期化（DIで取得できなかった場合）
        if (controlCenterManager == null) {
            System.out.println("  -> コントロールセンター管理サービス作成中（フォールバック）...");
            controlCenterManager = new ControlCenterManager();
        }
        controlCenterManager.setGestureManager(gestureManager);
        controlCenterManager.setCoordinateTransform(coordinateTransform);
        setupControlCenter();

        // NotificationManagerの初期化（DIで取得できなかった場合）
        if (notificationManager == null) {
            System.out.println("  -> 通知センター管理サービス作成中（フォールバック）...");
            notificationManager = new NotificationManager();
        }
        notificationManager.setKernel(this); // Kernelの参照を設定
        
        // LockManagerの初期化（DIで取得できなかった場合）
        if (lockManager == null) {
            System.out.println("  -> ロック状態管理サービス作成中（フォールバック）...");
            lockManager = new LockManager(settingsManager);
        }
        
        System.out.println("  -> 動的レイヤー管理システム作成中...");
        layerManager = new LayerManager(gestureManager);

        // VirtualRouterの初期化（DIで取得できなかった場合）
        if (virtualRouter == null) {
            System.out.println("  -> 仮想ネットワークルーター作成中（フォールバック）...");
            virtualRouter = new jp.moyashi.phoneos.core.service.network.VirtualRouter();
        }

        // NetworkAdapterの初期化
        System.out.println("  -> ネットワークアダプター作成中...");
        networkAdapter = new jp.moyashi.phoneos.core.service.network.NetworkAdapter(this);

        // MessageStorageの初期化（DIで取得できなかった場合）
        if (messageStorage == null) {
            System.out.println("  -> メッセージストレージサービス作成中（フォールバック）...");
            messageStorage = new MessageStorage(vfs);
        }

        // ハードウェアバイパスAPIの初期化
        System.out.println("  -> ハードウェアバイパスAPI作成中...");

        // DIコンテナから取得したサービスを優先
        boolean usedDI = false;
        if (mobileDataSocket != null || bluetoothSocket != null ||
            locationSocket != null || batteryInfo != null ||
            cameraSocket != null || microphoneSocket != null ||
            speakerSocket != null || icSocket != null ||
            simInfo != null) {
            System.out.println("     ハードウェアAPIの一部またはすべてをDIコンテナから取得済み");
            usedDI = true;
        }

        // DIで取得できなかったサービスをHardwareControllerまたは直接初期化で補完
        if (hardwareController != null) {
            // DIで取得できなかったサービスのみHardwareControllerから取得
            if (mobileDataSocket == null) {
                mobileDataSocket = hardwareController.getMobileDataSocket();
                if (mobileDataSocket != null) System.out.println("     -> MobileDataSocket: HardwareController経由で取得");
            }
            if (bluetoothSocket == null) {
                bluetoothSocket = hardwareController.getBluetoothSocket();
                if (bluetoothSocket != null) System.out.println("     -> BluetoothSocket: HardwareController経由で取得");
            }
            if (locationSocket == null) {
                locationSocket = hardwareController.getLocationSocket();
                if (locationSocket != null) System.out.println("     -> LocationSocket: HardwareController経由で取得");
            }
            if (batteryInfo == null) {
                batteryInfo = hardwareController.getBatteryInfo();
                if (batteryInfo != null) System.out.println("     -> BatteryInfo: HardwareController経由で取得");
            }
            if (cameraSocket == null) {
                cameraSocket = hardwareController.getCameraSocket();
                if (cameraSocket != null) System.out.println("     -> CameraSocket: HardwareController経由で取得");
            }
            if (microphoneSocket == null) {
                microphoneSocket = hardwareController.getMicrophoneSocket();
                if (microphoneSocket != null) System.out.println("     -> MicrophoneSocket: HardwareController経由で取得");
            }
            if (speakerSocket == null) {
                speakerSocket = hardwareController.getSpeakerSocket();
                if (speakerSocket != null) System.out.println("     -> SpeakerSocket: HardwareController経由で取得");
            }
            if (icSocket == null) {
                icSocket = hardwareController.getICSocket();
                if (icSocket != null) System.out.println("     -> ICSocket: HardwareController経由で取得");
            }
            if (simInfo == null) {
                simInfo = hardwareController.getSIMInfo();
                if (simInfo != null) System.out.println("     -> SIMInfo: HardwareController経由で取得");
            }

            // バッテリー監視サービスの初期化（DIで取得できなかった場合）
            if (batteryMonitor == null) {
                System.out.println("  -> BatteryMonitor初期化中...");
                hardwareController.initializeBatteryMonitor(settingsManager);
                batteryMonitor = hardwareController.getBatteryMonitor();
                if (batteryMonitor != null) System.out.println("     -> BatteryMonitor: HardwareController経由で取得");
            }
        }

        // まだ取得できていないサービスは直接初期化（フォールバック）
        if (mobileDataSocket == null) {
            mobileDataSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultMobileDataSocket();
            System.out.println("     -> MobileDataSocket: 直接初期化（フォールバック）");
        }
        if (bluetoothSocket == null) {
            bluetoothSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultBluetoothSocket();
            System.out.println("     -> BluetoothSocket: 直接初期化（フォールバック）");
        }
        if (locationSocket == null) {
            locationSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultLocationSocket();
            System.out.println("     -> LocationSocket: 直接初期化（フォールバック）");
        }
        if (batteryInfo == null) {
            batteryInfo = new jp.moyashi.phoneos.core.service.hardware.DefaultBatteryInfo();
            System.out.println("     -> BatteryInfo: 直接初期化（フォールバック）");
        }
        if (cameraSocket == null) {
            cameraSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultCameraSocket();
            System.out.println("     -> CameraSocket: 直接初期化（フォールバック）");
        }
        if (microphoneSocket == null) {
            microphoneSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultMicrophoneSocket();
            System.out.println("     -> MicrophoneSocket: 直接初期化（フォールバック）");
        }
        if (speakerSocket == null) {
            speakerSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultSpeakerSocket();
            System.out.println("     -> SpeakerSocket: 直接初期化（フォールバック）");
        }
        if (icSocket == null) {
            icSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultICSocket();
            System.out.println("     -> ICSocket: 直接初期化（フォールバック）");
        }
        if (simInfo == null) {
            simInfo = new jp.moyashi.phoneos.core.service.hardware.DefaultSIMInfo();
            System.out.println("     -> SIMInfo: 直接初期化（フォールバック）");
        }
        if (batteryMonitor == null) {
            batteryMonitor = new BatteryMonitor(batteryInfo, settingsManager);
            System.out.println("     -> BatteryMonitor: 直接初期化（フォールバック）");
        }

        System.out.println("  -> ChromiumService初期化中...");
        chromiumManager = null;
        if (chromiumService != null) {
            try {
                chromiumService.initialize(this);
                if (chromiumService instanceof DefaultChromiumService) {
                    ChromiumManager manager = ((DefaultChromiumService) chromiumService).getChromiumManager();
                    if (manager != null) {
                        chromiumManager = manager;
                    }
                }
                if (logger != null) {
                    logger.info("Kernel", "ChromiumService初期化完了");
                }
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Kernel", "ChromiumServiceの初期化に失敗しました", e);
                }
                e.printStackTrace();
            }
        } else {
            System.out.println("  -> ChromiumServiceが設定されていないため、初期化をスキップします");
            if (logger != null) {
                logger.warn("Kernel", "ChromiumServiceが未設定のため、Chromium機能は無効です");
            }
        }

        // パーミッション管理サービスの初期化
        System.out.println("  -> パーミッション管理サービス作成中...");
        permissionManager = new jp.moyashi.phoneos.core.service.permission.PermissionManagerImpl(this);
        logger.info("Kernel", "パーミッション管理サービス初期化完了");

        // アクティビティ管理サービスの初期化
        System.out.println("  -> アクティビティ管理サービス作成中...");
        activityManager = new jp.moyashi.phoneos.core.service.intent.ActivityManagerImpl(this);
        logger.info("Kernel", "アクティビティ管理サービス初期化完了");

        // クリップボード管理サービスの初期化
        System.out.println("  -> クリップボード管理サービス作成中...");
        clipboardManager = new jp.moyashi.phoneos.core.service.clipboard.ClipboardManagerImpl(this);
        logger.info("Kernel", "クリップボード管理サービス初期化完了");

        // クリップボードサービスの初期化（TextInputProtocol用OS統一管理）
        System.out.println("  -> クリップボードサービス（OS統一管理）作成中...");
        clipboardService = new jp.moyashi.phoneos.core.service.ClipboardService();
        logger.info("Kernel", "クリップボードサービス（OS統一管理）初期化完了");

        // SensorManagerの初期化（DIで取得できなかった場合）
        if (sensorManager == null) {
            System.out.println("  -> センサー管理サービス作成中（フォールバック）...");
            sensorManager = new jp.moyashi.phoneos.core.service.sensor.SensorManagerImpl(this);
        }
        logger.info("Kernel", "センサー管理サービス初期化完了");

        // コントロールセンターを最高優先度のジェスチャーリスナーとして登録
        gestureManager.addGestureListener(controlCenterManager);
        
        // 通知センターを高優先度のジェスチャーリスナーとして登録
        gestureManager.addGestureListener(notificationManager);
        
        // Kernelを最低優先度のジェスチャーリスナーとして登録
        gestureManager.addGestureListener(this);
        
        // 組み込みアプリケーションを登録（まず全て登録してから初期化）
        System.out.println("  -> 組み込みアプリケーションを登録中...");
        LauncherApp launcherApp = new LauncherApp();
        appLoader.registerApplication(launcherApp);

        SettingsApp settingsApp = new SettingsApp();
        appLoader.registerApplication(settingsApp);

        CalculatorApp calculatorApp = new CalculatorApp();
        appLoader.registerApplication(calculatorApp);

        jp.moyashi.phoneos.core.apps.network.NetworkApp networkApp = new jp.moyashi.phoneos.core.apps.network.NetworkApp();
        appLoader.registerApplication(networkApp);

        jp.moyashi.phoneos.core.apps.hardware_test.HardwareTestApp hardwareTestApp = new jp.moyashi.phoneos.core.apps.hardware_test.HardwareTestApp();
        appLoader.registerApplication(hardwareTestApp);

        jp.moyashi.phoneos.core.apps.voicememo.VoiceMemoApp voiceMemoApp = new jp.moyashi.phoneos.core.apps.voicememo.VoiceMemoApp();
        appLoader.registerApplication(voiceMemoApp);

        jp.moyashi.phoneos.core.apps.note.NoteApp noteApp = new jp.moyashi.phoneos.core.apps.note.NoteApp();
        appLoader.registerApplication(noteApp);

        jp.moyashi.phoneos.core.apps.chromiumbrowser.ChromiumBrowserApp chromiumBrowserApp = new jp.moyashi.phoneos.core.apps.chromiumbrowser.ChromiumBrowserApp();
        appLoader.registerApplication(chromiumBrowserApp);

        // Sample WebApp（HTML/CSS/JSデモ）
        jp.moyashi.phoneos.core.apps.samplewebapp.SampleWebApp sampleWebApp = new jp.moyashi.phoneos.core.apps.samplewebapp.SampleWebApp();
        appLoader.registerApplication(sampleWebApp);

        System.out.println("Kernel: " + appLoader.getLoadedApps().size() + " 個のアプリケーションを登録");

        // MODアプリケーションを同期して自動登録
        System.out.println("  -> MODアプリケーションを同期中...");
        appLoader.syncWithModRegistry();
        // availableModAppsから直接loadedAppsに追加（自動インストール）
        for (IApplication modApp : appLoader.getAvailableModApps()) {
            if (appLoader.registerApplication(modApp)) {
                System.out.println("Kernel: MODアプリを登録: " + modApp.getName());
                modApp.onInitialize(this);
            }
        }
        System.out.println("Kernel: MODアプリ同期完了 - 合計 " + appLoader.getLoadedApps().size() + " 個のアプリ");

        // すべてのアプリ登録後に初期化を実行
        System.out.println("  -> アプリケーションを初期化中...");
        launcherApp.onInitialize(this);
        settingsApp.onInitialize(this);
        calculatorApp.onInitialize(this);
        networkApp.onInitialize(this);
        hardwareTestApp.onInitialize(this);
        noteApp.onInitialize(this);
        chromiumBrowserApp.onInitialize(this);
        sampleWebApp.onInitialize(this);

        // ScreenManagerの初期化（DIで取得できなかった場合）
        if (screenManager == null) {
            System.out.println("  -> スクリーンマネージャー作成中（フォールバック）...");
            screenManager = new ScreenManager();
        }
        System.out.println("✅ ScreenManager作成済み: " + (screenManager != null));

        // ScreenManagerにKernelインスタンスを設定（レイヤー管理統合のため）
        screenManager.setKernel(this);

        // Phase 3: NavigationControllerとLayerControllerの設定
        if (navigationController != null) {
            navigationController.setScreenManager(screenManager);
        }
        if (layerController != null) {
            layerController.setManagers(navigationController, screenManager,
                popupManager, controlCenterManager, notificationManager);
        }

        // ScreenManagerにPAppletを設定（画面のsetup()に必要）
        System.out.println("  -> ScreenManagerにPAppletを設定中...");
        screenManager.setCurrentPApplet(parentApplet);
        System.out.println("✅ ScreenManagerのPApplet設定完了");
        
        // ロック状態に基づいて初期画面を決定
        if (lockManager.isLocked()) {
            System.out.println("▶️ OSがロック状態 - ロック画面を初期画面として開始中...");
            jp.moyashi.phoneos.core.ui.lock.LockScreen lockScreen =
                new jp.moyashi.phoneos.core.ui.lock.LockScreen(this);
            screenManager.pushScreen(lockScreen);
            addLayer(LayerType.LOCK_SCREEN); // レイヤースタックに追加
            System.out.println("✅ ロック画面をScreenManagerにプッシュ済み");
        } else {
            System.out.println("▶️ OSがアンロック状態 - LauncherAppを初期画面として開始中...");
            Screen launcherScreen = launcherApp.getEntryScreen(this);
            System.out.println("✅ LauncherApp画面取得済み: " + (launcherScreen != null));
            if (launcherScreen != null) {
                System.out.println("   画面タイトル: " + launcherScreen.getScreenTitle());
            }
            
            screenManager.pushScreen(launcherScreen);
            System.out.println("✅ 画面をScreenManagerにプッシュ済み");
        }
        
        System.out.println("✅ Kernel: OS初期化完了！");
        if (lockManager.isLocked()) {
            System.out.println("    • ロック画面が表示されています");
            System.out.println("    • パターン入力でアンロックできます (デフォルト: L字型パターン)");
        } else {
            System.out.println("    • LauncherAppが実行中");
        }
        System.out.println("    • " + appLoader.getLoadedApps().size() + " 個のアプリケーションが利用可能");
        System.out.println("    • システムはユーザー操作に対応可能");
        System.out.println("=======================================");
    }
    
    // 旧draw()メソッドは削除済み - render()メソッドを使用してください
    
    // 旧mousePressed()メソッドは削除済み - mousePressed(int x, int y)を使用してください
    
    // 旧mouseDragged()メソッドは削除済み - 必要に応じて独立APIを実装してください
    
    // 旧mouseReleased()メソッドは削除済み - mouseReleased(int x, int y)を使用してください
    
    // 旧mouseWheel()メソッドは削除済み - 必要に応じて独立APIを実装してください
    
    // 旧mouseWheel(MouseEvent event)メソッドは削除済み - 必要に応じて独立APIを実装してください
    
    /**
     * マウスホイールイベント処理。
     * ホイールスクロールをドラッグジェスチャーに変換してスクロール機能を提供する。
     * 注意: PAppletグローバル変数(mouseX, mouseY)への依存を除去する必要があります。
     */
    private void handleMouseWheel(int wheelRotation) {
        System.out.println("==========================================");
        System.out.println("Kernel: handleMouseWheel - rotation: " + wheelRotation);
        System.out.println("GestureManager: " + (gestureManager != null ? "exists" : "null"));
        System.out.println("==========================================");

        if (gestureManager != null && wheelRotation != 0) {
            // ホイールをドラッグジェスチャーとしてシミュレート
            int scrollAmount = wheelRotation * 30; // スクロール量を調整

            // 画面中央の座標を使用（mouseX, mouseYの代替）
            int centerX = width / 2;
            int centerY = height / 2;

            // ドラッグ開始をシミュレート
            gestureManager.handleMousePressed(centerX, centerY);

            // ドラッグ移動をシミュレート（Y軸方向のみ）
            gestureManager.handleMouseDragged(centerX, centerY + scrollAmount);

            // ドラッグ終了をシミュレート
            gestureManager.handleMouseReleased(centerX, centerY + scrollAmount);

            System.out.println("Kernel: Converted wheel scroll to drag gesture (scrollAmount: " + scrollAmount + ")");
        }
    }
    
    // 旧keyPressed()メソッドは削除済み - keyPressed(char key, int keyCode)を使用してください
    
    // 旧keyReleased()メソッドは削除済み - keyReleased(char key, int keyCode)を使用してください
    
    /**
     * ESCキープレス処理。
     * 長押し検出を開始する。
     */
    private void handleEscKeyPress() {
        if (!escKeyPressed) {
            escKeyPressed = true;
            escKeyPressTime = System.currentTimeMillis();
            System.out.println("Kernel: ESC key pressed - starting long press detection");
        }
    }
    
    /**
     * ESCキーリリース処理。
     * 短押し（ロック）か長押し（シャットダウン）かを判定する。
     */
    private void handleEscKeyRelease() {
        if (escKeyPressed) {
            escKeyPressed = false;
            long pressDuration = System.currentTimeMillis() - escKeyPressTime;
            
            System.out.println("Kernel: ESC key released after " + pressDuration + "ms");
            
            if (pressDuration >= LONG_PRESS_DURATION) {
                // 長押し：シャットダウン
                System.out.println("Kernel: ESC long press detected - initiating shutdown");
                handleShutdown();
            } else {
                // 短押し：ロック
                System.out.println("Kernel: ESC short press detected - locking device");
                handleDeviceLock();
            }
        }
    }
    
    /**
     * デバイスロック処理。
     * 現在のロック状態に関わらずロック画面を表示する。
     */
    private void handleDeviceLock() {
        System.out.println("Kernel: Locking device - switching to lock screen");
        
        if (lockManager != null) {
            lockManager.lock(); // デバイスをロック状態にする
            
            // ロック画面に切り替え
            try {
                jp.moyashi.phoneos.core.ui.lock.LockScreen lockScreen =
                    new jp.moyashi.phoneos.core.ui.lock.LockScreen(this);

                // 現在の画面をクリアしてロック画面をプッシュ
                screenManager.clearAllScreens();
                screenManager.pushScreen(lockScreen);
                addLayer(LayerType.LOCK_SCREEN); // レイヤースタックに追加

                System.out.println("Kernel: Device locked successfully");
            } catch (Exception e) {
                System.err.println("Kernel: Error switching to lock screen: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * システムシャットダウン処理。
     */
    private void handleShutdown() {
        System.out.println("Kernel: Initiating system shutdown...");
        shutdown();
    }

    /**
     * システムシャットダウン処理（独立API）。
     */
    public void shutdown() {
        System.out.println("Kernel: System shutdown requested");

        // システムシャットダウンイベントを発行
        EventBus.getInstance().post(SystemEvent.shutdown(this));

        // ServiceManager のシャットダウン
        if (serviceManager != null) {
            System.out.println("Kernel: Shutting down ServiceManager...");
            serviceManager.shutdown();
        }

        if (chromiumService != null) {
            System.out.println("Kernel: Shutting down ChromiumService...");
            chromiumService.shutdown();
        }
        chromiumManager = null;

        // シャットダウンメッセージをPGraphicsバッファに描画
        if (graphics != null) {
            graphics.beginDraw();
            graphics.background(20, 25, 35);
            graphics.fill(255, 255, 255);
            graphics.textAlign(PApplet.CENTER, PApplet.CENTER);
            graphics.textSize(24);
            graphics.text("システムをシャットダウンしています...", width / 2, height / 2);
            graphics.endDraw();
        }

        // 少し遅延してから終了
        new Thread(() -> {
            try {
                Thread.sleep(1500);

                // EventBusのシャットダウン
                EventBus.getInstance().shutdown();

                System.out.println("Kernel: Shutdown complete");
                if (parentApplet != null) {
                    parentApplet.exit();
                }
                System.exit(0);
            } catch (InterruptedException e) {
                System.err.println("Kernel: Shutdown interrupted: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * 仮想ファイルシステムサービスを取得する。
     * @return VFSインスタンス
     */
    public VFS getVFS() {
        return vfs;
    }

    /**
     * VFSサービスを設定する。
     * @param vfs VFSインスタンス
     */
    public void setVFS(VFS vfs) {
        this.vfs = vfs;
    }

    /**
     * 設定管理サービスを取得する。
     * @return SettingsManagerインスタンス
     */
    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    /**
     * テーマエンジンを取得する。
     * @return ThemeEngine
     */
    public jp.moyashi.phoneos.core.ui.theme.ThemeEngine getThemeEngine() {
        return themeEngine;
    }
    
    /**
     * システムクロックサービスを取得する。
     * @return SystemClockインスタンス
     */
    public SystemClock getSystemClock() {
        return systemClock;
    }
    
    /**
     * スクリーンマネージャーを取得する。
     * @return ScreenManagerインスタンス
     */
    public ScreenManager getScreenManager() {
        return screenManager;
    }

    /**
     * OSロガーサービスを取得する。
     *
     * @return LoggerService
     */
    public LoggerService getLogger() {
        return logger;
    }

    /**
     * サービスマネージャーを取得する。
     *
     * @return ServiceManager
     */
    public ServiceManager getServiceManager() {
        return serviceManager;
    }

    /**
     * Shiftキーが押されているかどうかを取得する。
     *
     * @return Shiftキーが押されている場合true
     */
    public boolean isShiftPressed() {
        // Phase 1リファクタリング: InputManagerから状態を取得
        if (inputManager != null) {
            return inputManager.isShiftPressed();
        }
        return shiftPressed;
    }

    /**
     * Ctrlキーが押されているかどうかを取得する。
     *
     * @return Ctrlキーが押されている場合true
     */
    public boolean isCtrlPressed() {
        // Phase 1リファクタリング: InputManagerから状態を取得
        if (inputManager != null) {
            return inputManager.isCtrlPressed();
        }
        return ctrlPressed;
    }

    /**
     * Altキーが押されているかどうかを取得する。
     *
     * @return Altキーが押されている場合true
     */
    public boolean isAltPressed() {
        // Phase 1リファクタリング: InputManagerから状態を取得
        if (inputManager != null) {
            return inputManager.isAltPressed();
        }
        return altPressed;
    }

    /**
     * Metaキー（Command/Windowsキー）が押されているかどうかを取得する。
     *
     * @return Metaキーが押されている場合true
     */
    public boolean isMetaPressed() {
        // Phase 1リファクタリング: InputManagerから状態を取得
        if (inputManager != null) {
            return inputManager.isMetaPressed();
        }
        return metaPressed;
    }

    /**
     * アプリケーションローダーサービスを取得する。
     * @return AppLoaderインスタンス
     */
    public AppLoader getAppLoader() {
        return appLoader;
    }
    
    /**
     * レイアウト管理サービスを取得する。
     * @return LayoutManagerインスタンス
     */
    public LayoutManager getLayoutManager() {
        return layoutManager;
    }
    
    /**
     * グローバルポップアップマネージャーを取得する。
     * @return PopupManagerインスタンス
     */
    public PopupManager getPopupManager() {
        return popupManager;
    }
    
    /**
     * Kernelレベルジェスチャーマネージャーを取得する。
     * @return GestureManagerインスタンス
     */
    public GestureManager getGestureManager() {
        return gestureManager;
    }
    
    /**
     * コントロールセンター管理サービスを取得する。
     * @return ControlCenterManagerインスタンス
     */
    public ControlCenterManager getControlCenterManager() {
        return controlCenterManager;
    }
    
    /**
     * 通知センター管理サービスのインスタンスを取得する。
     *
     * @return 通知センターマネージャー
     */
    public NotificationManager getNotificationManager() {
        return notificationManager;
    }

    /**
     * 通知を送信する。
     *
     * @param title 通知タイトル
     * @param message 通知メッセージ
     */
    public void sendNotification(String title, String message) {
        if (notificationManager != null) {
            notificationManager.addNotification("System", title, message, 1);
        }
    }

    /**
     * 画面幅を取得する。
     *
     * @return 画面幅
     */
    public int getWidth() {
        return width;
    }

    /**
     * 画面高さを取得する。
     *
     * @return 画面高さ
     */
    public int getHeight() {
        return height;
    }

    /**
     * ワールドIDを取得する。
     *
     * @return ワールドID
     */
    public String getWorldId() {
        return worldId;
    }

    /**
     * 統一座標変換システムのインスタンスを取得する。
     *
     * @return 統一座標変換システム
     */
    public CoordinateTransform getCoordinateTransform() {
        return coordinateTransform;
    }
    
    /**
     * ロック状態管理サービスのインスタンスを取得する。
     * 
     * @return ロック管理サービス
     */
    public LockManager getLockManager() {
        return lockManager;
    }
    
    /**
     * 動的レイヤー管理システムのインスタンスを取得する。
     *
     * @return レイヤーマネージャー
     */
    public LayerManager getLayerManager() {
        return layerManager;
    }

    /**
     * 仮想ネットワークルーターサービスのインスタンスを取得する。
     *
     * @return 仮想ネットワークルーター
     */
    public jp.moyashi.phoneos.core.service.network.VirtualRouter getVirtualRouter() {
        return virtualRouter;
    }

    /**
     * ネットワークアダプター（統一ネットワークAPI）のインスタンスを取得する。
     * すべてのネットワーク通信はこのアダプターを経由する。
     *
     * @return ネットワークアダプター
     */
    public jp.moyashi.phoneos.core.service.network.NetworkAdapter getNetworkAdapter() {
        return networkAdapter;
    }

    /**
     * メッセージストレージサービスのインスタンスを取得する。
     *
     * @return メッセージストレージ
     */
    public MessageStorage getMessageStorage() {
        return messageStorage;
    }

    /**
     * モバイルデータ通信ソケットのインスタンスを取得する。
     *
     * @return モバイルデータ通信ソケット
     */
    public jp.moyashi.phoneos.core.service.hardware.MobileDataSocket getMobileDataSocket() {
        return mobileDataSocket;
    }

    /**
     * Bluetooth通信ソケットのインスタンスを取得する。
     *
     * @return Bluetooth通信ソケット
     */
    public jp.moyashi.phoneos.core.service.hardware.BluetoothSocket getBluetoothSocket() {
        return bluetoothSocket;
    }

    /**
     * 位置情報ソケットのインスタンスを取得する。
     *
     * @return 位置情報ソケット
     */
    public jp.moyashi.phoneos.core.service.hardware.LocationSocket getLocationSocket() {
        return locationSocket;
    }

    /**
     * バッテリー情報のインスタンスを取得する。
     *
     * @return バッテリー情報
     */
    public jp.moyashi.phoneos.core.service.hardware.BatteryInfo getBatteryInfo() {
        return batteryInfo;
    }

    /**
     * バッテリー監視サービスのインスタンスを取得する。
     *
     * @return バッテリー監視サービス
     */
    public BatteryMonitor getBatteryMonitor() {
        return batteryMonitor;
    }

    /**
     * カメラソケットのインスタンスを取得する。
     *
     * @return カメラソケット
     */
    public jp.moyashi.phoneos.core.service.hardware.CameraSocket getCameraSocket() {
        return cameraSocket;
    }

    /**
     * マイクソケットのインスタンスを取得する。
     *
     * @return マイクソケット
     */
    public jp.moyashi.phoneos.core.service.hardware.MicrophoneSocket getMicrophoneSocket() {
        return microphoneSocket;
    }

    /**
     * スピーカーソケットのインスタンスを取得する。
     *
     * @return スピーカーソケット
     */
    public jp.moyashi.phoneos.core.service.hardware.SpeakerSocket getSpeakerSocket() {
        return speakerSocket;
    }

    /**
     * IC通信ソケットのインスタンスを取得する。
     *
     * @return IC通信ソケット
     */
    public jp.moyashi.phoneos.core.service.hardware.ICSocket getICSocket() {
        return icSocket;
    }

    /**
     * SIM情報のインスタンスを取得する。
     *
     * @return SIM情報
     */
    public jp.moyashi.phoneos.core.service.hardware.SIMInfo getSIMInfo() {
        return simInfo;
    }

    /**
     * パーミッション管理サービスを取得する。
     *
     * @return PermissionManagerインスタンス
     */
    public jp.moyashi.phoneos.core.service.permission.PermissionManager getPermissionManager() {
        return permissionManager;
    }

    /**
     * アクティビティ管理サービスを取得する。
     *
     * @return ActivityManagerインスタンス
     */
    public jp.moyashi.phoneos.core.service.intent.ActivityManager getActivityManager() {
        return activityManager;
    }

    /**
     * クリップボード管理サービスを取得する。
     *
     * @return ClipboardManagerインスタンス
     */
    public jp.moyashi.phoneos.core.service.clipboard.ClipboardManager getClipboardManager() {
        return clipboardManager;
    }

    public void setChromiumService(ChromiumService chromiumService) {
        this.chromiumService = chromiumService;
    }

    public ChromiumService getChromiumService() {
        return chromiumService;
    }

    /**
     * 旧ChromiumManager APIを取得する（互換用）。
     */
    public ChromiumManager getChromiumManager() {
        return chromiumManager;
    }

    /**
     * センサー管理サービスを取得する。
     *
     * @return SensorManagerインスタンス
     */
    public jp.moyashi.phoneos.core.service.sensor.SensorManager getSensorManager() {
        return sensorManager;
    }

    /**
     * 電源管理システムを取得する（Phase 2リファクタリング）。
     *
     * @return PowerManagerインスタンス
     */
    public PowerManager getPowerManager() {
        return powerManager;
    }

    /**
     * ライフサイクル管理システムを取得する（Phase 2リファクタリング）。
     *
     * @return SystemLifecycleManagerインスタンス
     */
    public SystemLifecycleManager getLifecycleManager() {
        return lifecycleManager;
    }

    /**
     * サービスコンテナから任意のサービスを取得する（Phase 2リファクタリング）。
     * 高度な使用向け。
     *
     * @param <T> サービスの型
     * @param serviceClass サービスクラス
     * @return サービスインスタンス、存在しない場合はnull
     */
    public <T> T getService(Class<T> serviceClass) {
        if (serviceBootstrap != null) {
            return serviceBootstrap.tryGetService(serviceClass);
        }
        return null;
    }

    /**
     * NavigationControllerを取得する（Phase 3リファクタリング）。
     *
     * @return NavigationControllerインスタンス
     */
    public NavigationController getNavigationController() {
        return navigationController;
    }

    /**
     * LayerControllerを取得する（Phase 3リファクタリング）。
     *
     * @return LayerControllerインスタンス
     */
    public LayerController getLayerController() {
        return layerController;
    }

    /**
     * ResourceManagerを取得する（Phase 3リファクタリング）。
     *
     * @return ResourceManagerインスタンス
     */
    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    /**
     * HardwareControllerを取得する（Phase 3リファクタリング）。
     *
     * @return HardwareControllerインスタンス
     */
    public HardwareController getHardwareController() {
        return hardwareController;
    }

    /**
     * モバイルデータ通信ソケットを設定する（forge-mod用）。
     *
     * @param socket モバイルデータ通信ソケット
     */
    public void setMobileDataSocket(jp.moyashi.phoneos.core.service.hardware.MobileDataSocket socket) {
        if (hardwareController != null) {
            hardwareController.setMobileDataSocket(socket);
        }
        this.mobileDataSocket = socket; // 後方互換性のため
    }

    /**
     * Bluetooth通信ソケットを設定する（forge-mod用）。
     *
     * @param socket Bluetooth通信ソケット
     */
    public void setBluetoothSocket(jp.moyashi.phoneos.core.service.hardware.BluetoothSocket socket) {
        this.bluetoothSocket = socket;
    }

    /**
     * 位置情報ソケットを設定する（forge-mod用）。
     *
     * @param socket 位置情報ソケット
     */
    public void setLocationSocket(jp.moyashi.phoneos.core.service.hardware.LocationSocket socket) {
        this.locationSocket = socket;
    }

    /**
     * バッテリー情報を設定する（forge-mod用）。
     *
     * @param info バッテリー情報
     */
    public void setBatteryInfo(jp.moyashi.phoneos.core.service.hardware.BatteryInfo info) {
        this.batteryInfo = info;
    }

    /**
     * カメラソケットを設定する（forge-mod用）。
     *
     * @param socket カメラソケット
     */
    public void setCameraSocket(jp.moyashi.phoneos.core.service.hardware.CameraSocket socket) {
        this.cameraSocket = socket;
    }

    /**
     * マイクソケットを設定する（forge-mod用）。
     *
     * @param socket マイクソケット
     */
    public void setMicrophoneSocket(jp.moyashi.phoneos.core.service.hardware.MicrophoneSocket socket) {
        this.microphoneSocket = socket;
    }

    /**
     * スピーカーソケットを設定する（forge-mod用）。
     *
     * @param socket スピーカーソケット
     */
    public void setSpeakerSocket(jp.moyashi.phoneos.core.service.hardware.SpeakerSocket socket) {
        this.speakerSocket = socket;
    }

    /**
     * IC通信ソケットを設定する（forge-mod用）。
     *
     * @param socket IC通信ソケット
     */
    public void setICSocket(jp.moyashi.phoneos.core.service.hardware.ICSocket socket) {
        this.icSocket = socket;
    }

    /**
     * SIM情報を設定する（forge-mod用）。
     *
     * @param info SIM情報
     */
    public void setSIMInfo(jp.moyashi.phoneos.core.service.hardware.SIMInfo info) {
        this.simInfo = info;
    }

    /**
     * リソースから日本語フォントを読み込む。
     * Noto Sans JP TTFファイルをリソースから読み込み、Processing PFontとして返す。
     * クロスプラットフォーム対応（Windows, Mac, Linux）およびForge環境でも動作する。
     *
     * @return 読み込まれたPFont、失敗した場合はnull
     */
    private PFont loadJapaneseFont() {
        try {
            // リソースからTTFファイルを読み込む
            if (logger != null) {
                logger.debug("Kernel", "リソースからNoto Sans JP TTFファイルを読み込み中...");
            }

            // 複数のClassLoaderを試してリソースを読み込む（Forge環境対応）
            InputStream fontStream = null;
            String fontPath = "/fonts/NotoSansJP-Regular.ttf";

            // 1. KernelクラスのClassLoaderから試す
            fontStream = getClass().getResourceAsStream(fontPath);
            if (fontStream != null && logger != null) {
                logger.debug("Kernel", "Kernelクラスのクラスローダーからフォントを読み込みました");
            }

            // 2. コンテキストClassLoaderから試す
            if (fontStream == null) {
                ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
                if (contextClassLoader != null) {
                    fontStream = contextClassLoader.getResourceAsStream(fontPath.substring(1)); // 先頭の"/"を除去
                    if (fontStream != null && logger != null) {
                        logger.debug("Kernel", "コンテキストクラスローダーからフォントを読み込みました");
                    }
                }
            }

            // 3. システムClassLoaderから試す
            if (fontStream == null) {
                fontStream = ClassLoader.getSystemResourceAsStream(fontPath.substring(1)); // 先頭の"/"を除去
                if (fontStream != null && logger != null) {
                    logger.debug("Kernel", "システムクラスローダーからフォントを読み込みました");
                }
            }

            if (fontStream == null) {
                if (logger != null) {
                    logger.error("Kernel", "フォントファイルが見つかりません: " + fontPath);
                    logger.error("Kernel", "試行したクラスローダー: Kernelクラス、コンテキスト、システム");
                }
                return null;
            }

            // Java AWTフォントを作成
            Font awtFont = Font.createFont(Font.TRUETYPE_FONT, fontStream);
            fontStream.close();

            if (logger != null) {
                logger.debug("Kernel", "AWTフォントを作成しました: " + awtFont.getFontName());
            }

            // GraphicsEnvironmentに登録（システムフォントとして利用可能にする）
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            boolean registered = ge.registerFont(awtFont);

            if (registered) {
                if (logger != null) {
                    logger.info("Kernel", "フォントをシステムに登録しました: " + awtFont.getFontName());
                }
            } else {
                if (logger != null) {
                    logger.debug("Kernel", "フォント登録をスキップ（既に登録済みまたは登録不要）");
                }
            }

            // Processing PFontとして作成
            // PApplet.createFont()を使わず、AWT FontからPFontを直接構築する
            // これにより、setup()が呼ばれていない状態でも動作する
            if (logger != null) {
                logger.debug("Kernel", "AWT FontからPFontを直接構築中... (フォント名: " + awtFont.getFontName() + ")");
            }
            try {
                // サイズ16でフォントを派生
                java.awt.Font derivedFont = awtFont.deriveFont(16f);

                // PFontコンストラクタを使用して直接作成（PApplet不要）
                PFont pFont = new PFont(derivedFont, true);  // smooth=true

                if (logger != null) {
                    logger.info("Kernel", "PFontを作成しました (サイズ: 16、フォント名: " + derivedFont.getFontName() + ")");
                }
                return pFont;
            } catch (Exception e) {
                if (logger != null) {
                    logger.error("Kernel", "PFont構築に失敗: " + e.getMessage(), e);
                }
                // フォールバック: nullを返してデフォルトフォントを使用
                return null;
            }

        } catch (Exception e) {
            if (logger != null) {
                logger.error("Kernel", "フォント読み込み中にエラーが発生: " + e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * 日本語対応フォントを取得する。
     *
     * @return 日本語フォント、初期化されていない場合はnull
     */
    public PFont getJapaneseFont() {
        return japaneseFont;
    }
    
    /**
     * Kernelレベルでのジェスチャーイベント処理。
     * 主に画面上からのスワイプダウンで通知センター、画面下からのスワイプアップでコントロールセンターを表示する処理を行う。
     * 
     * @param event ジェスチャーイベント
     * @return イベントを処理した場合true
     */
    @Override
    public boolean onGesture(GestureEvent event) {
        // 通知センターとコントロールセンターの処理はGestureManagerが自動的に優先度に基づいて処理するため、
        // ここでは手動チェックは不要
        
        // 画面上からのスワイプダウンで通知センターを表示
        if (event.getType() == GestureType.SWIPE_DOWN) {
            // 画面上部（高さの10%以下）からのスワイプダウンを検出
            if (event.getStartY() <= height * 0.1f) {
                System.out.println("Kernel: Detected swipe down from top at y=" + event.getStartY() + 
                                 ", showing notification center");
                if (notificationManager != null) {
                    notificationManager.show();
                    return true;
                }
            }
        }
        
        // 画面下からのスワイプアップでコントロールセンターを表示
        if (event.getType() == GestureType.SWIPE_UP) {
            // 画面下部（高さの90%以上）からのスワイプアップを検出
            if (event.getStartY() >= height * 0.9f) {
                System.out.println("Kernel: Detected swipe up from bottom at y=" + event.getStartY() + 
                                 ", showing control center");
                if (controlCenterManager != null) {
                    controlCenterManager.show();
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Kernelは画面全体を処理対象とする。
     * 
     * @param x X座標
     * @param y Y座標
     * @return 常にtrue
     */
    @Override
    public boolean isInBounds(int x, int y) {
        return true;
    }
    
    /**
     * Kernelの優先度は最低に設定する。
     * 他のリスナーがイベントを処理しなかった場合のみ処理される。
     * 
     * @return 最低優先度（-1000）
     */
    @Override
    public int getPriority() {
        return -1000;
    }
    
    /**
     * ホーム画面に戻る処理を実行する。
     * コントロールセンターの非表示、ホーム画面への遷移、ホーム画面内での最初のページへの移動を行う。
     */
    private void navigateToHome() {
        System.out.println("Kernel: Navigating to home screen");
        
        // 1. コントロールセンターが表示されている場合は閉じる
        if (controlCenterManager != null && controlCenterManager.isVisible()) {
            System.out.println("Kernel: Closing control center");
            controlCenterManager.hide();
            return;
        }
        
        // 2. 現在の画面を確認
        if (screenManager != null) {
            Screen currentScreen = screenManager.getCurrentScreen();
            
            if (currentScreen != null) {
                String currentScreenTitle = currentScreen.getScreenTitle();
                System.out.println("Kernel: Current screen: " + currentScreenTitle);
                
                // ホーム画面でない場合はホーム画面に戻る
                if (!"Home Screen".equals(currentScreenTitle)) {
                    // ホーム画面に戻る（LauncherAppを検索）
                    if (appLoader != null) {
                        IApplication launcherApp = findLauncherApp();
                        if (launcherApp != null) {
                            System.out.println("Kernel: Returning to home screen");
                            screenManager.clearAllScreens();
                            screenManager.pushScreen(launcherApp.getEntryScreen(this));
                        }
                    }
                } else {
                    // 既にホーム画面にいる場合は最初のページに戻る
                    if (currentScreen instanceof jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen) {
                        System.out.println("Kernel: Already on home screen, navigating to first page");
                        jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen homeScreen = 
                            (jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen) currentScreen;
                        homeScreen.navigateToFirstPage();
                    }
                }
            }
        }
    }
    
    /**
     * LauncherAppを検索して取得する。
     * 
     * @return LauncherAppのインスタンス、見つからない場合はnull
     */
    private IApplication findLauncherApp() {
        if (appLoader == null) return null;
        
        for (IApplication app : appLoader.getLoadedApps()) {
            if ("jp.moyashi.phoneos.core.apps.launcher".equals(app.getApplicationId())) {
                return app;
            }
        }
        return null;
    }

    /**
     * ホームボタン（スペースキー）の動的階層管理処理。
     * 現在開いているレイヤーの順序を動的に判定し、最後に開いたレイヤーから閉じる。
     * アプリケーションが閉じられる場合はホームスクリーンに移行する。
     *
     * 例外: ロック画面は閉じられない（デバッグスクリーンが出るため）
     */
    /**
     * ホームボタンの処理を実行する。
     * Phase 1リファクタリングでInputManagerから呼び出されるため、publicに変更。
     * 将来的にはLayerControllerに移行予定。
     */
    public void handleHomeButton() {
        // Phase 3: LayerControllerに処理を委譲
        if (layerController != null) {
            layerController.handleHomeButton();
            return;
        }

        // LayerControllerが利用できない場合の従来処理（後方互換性）
        System.out.println("Kernel: Home button pressed - dynamic layer management");
        System.out.println("Kernel: Current layer stack: " + layerStack);

        try {
            // 1. 動的に最上位の閉じられるレイヤーを取得
            LayerType topLayer = getTopMostClosableLayer();

            if (topLayer == null) {
                System.out.println("Kernel: No closable layers found - already at lowest layer");
                return;
            }

            System.out.println("Kernel: Closing top layer: " + topLayer);

            // 2. レイヤータイプに応じて適切な閉じる処理を実行
            switch (topLayer) {
                case POPUP:
                    if (popupManager != null && popupManager.hasActivePopup()) {
                        popupManager.closeCurrentPopup();
                        removeLayer(LayerType.POPUP);
                        System.out.println("Kernel: Popup closed");
                    }
                    break;

                case CONTROL_CENTER:
                    if (controlCenterManager != null && controlCenterManager.isVisible()) {
                        controlCenterManager.hide();
                        removeLayer(LayerType.CONTROL_CENTER);
                        System.out.println("Kernel: Control center closed");
                    }
                    break;

                case NOTIFICATION:
                    if (notificationManager != null && notificationManager.isVisible()) {
                        notificationManager.hide();
                        removeLayer(LayerType.NOTIFICATION);
                        System.out.println("Kernel: Notification center closed");
                    }
                    break;

                case APPLICATION:
                    // アプリケーションを閉じてホーム画面に移行
                    System.out.println("Kernel: Closing application and returning to home screen");
                    navigateToHome();
                    removeLayer(LayerType.APPLICATION);
                    break;

                default:
                    System.out.println("Kernel: Unknown layer type: " + topLayer);
                    break;
            }

        } catch (Exception e) {
            System.err.println("Kernel: handleHomeButton処理エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ホームに戻るリクエストを処理する公開API。
     * プラットフォーム層（Standalone/Forge）から呼び出される。
     * 内部的にhandleHomeButton()を呼び出す。
     */
    public void requestGoHome() {
        System.out.println("Kernel: requestGoHome() called");
        handleHomeButton();
    }

    /**
     * テキスト入力にフォーカスがあるかを返す。
     * プラットフォーム層でホームボタンショートカットの判定に使用される。
     *
     * @return テキスト入力フィールドにフォーカスがある場合true
     */
    public boolean hasTextInputFocus() {
        if (screenManager != null) {
            return screenManager.hasFocusedComponent();
        }
        return false;
    }

    /**
     * レイヤーがスタックに追加される際に呼び出される。
     * レイヤーの開いた順序を記録し、動的優先順位システムに反映する。
     *
     * @param layerType 追加されるレイヤー種別
     */
    public void addLayer(LayerType layerType) {
        // 既に存在する場合は移除して最上位に移動
        layerStack.remove(layerType);
        layerStack.add(layerType);

        System.out.println("Kernel: Layer '" + layerType + "' added to stack. Current stack: " + layerStack);

        // Phase 3: LayerControllerにも同期
        if (layerController != null) {
            layerController.addLayer(layerType);
        }
    }

    /**
     * レイヤーがスタックから削除される際に呼び出される。
     *
     * @param layerType 削除されるレイヤー種別
     */
    public void removeLayer(LayerType layerType) {
        boolean removed = layerStack.remove(layerType);
        if (removed) {
            System.out.println("Kernel: Layer '" + layerType + "' removed from stack. Current stack: " + layerStack);
        }

        // Phase 3: LayerControllerにも同期
        if (layerController != null) {
            layerController.removeLayer(layerType);
        }
    }

    /**
     * 現在最上位の閉じられるレイヤーを取得する。
     * ロック画面とホーム画面は閉じられない（ロック画面はデバッグスクリーンが出るため、ホーム画面は最下層のため）。
     *
     * @return 最上位の閉じられるレイヤー種別、閉じられるレイヤーがない場合はnull
     */
    public LayerType getTopMostClosableLayer() {
        // スタックを逆順で検索（最後に追加されたものから）
        for (int i = layerStack.size() - 1; i >= 0; i--) {
            LayerType layer = layerStack.get(i);

            // ロック画面は閉じられない（デバッグスクリーン防止）
            if (layer == LayerType.LOCK_SCREEN) {
                continue;
            }

            // ホーム画面は最下層なので、これに到達した場合は閉じられるレイヤーがない
            if (layer == LayerType.HOME_SCREEN) {
                break;
            }

            return layer;
        }

        return null;
    }

    /**
     * コントロールセンターに様々なアイテムを追加してセットアップする。
     */
    private void setupControlCenter() {
        if (controlCenterManager == null) {
            return;
        }
        
        System.out.println("  -> コントロールセンターアイテムを追加中...");
        
        // ToggleItemをimportするため
        jp.moyashi.phoneos.core.controls.ToggleItem toggleItem;
        
        // WiFi切り替え
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "wifi", "WiFi", "ワイヤレス接続のオン/オフ", 
            false, (isOn) -> System.out.println("WiFi toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // Bluetooth切り替え
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "bluetooth", "Bluetooth", "Bluetooth接続のオン/オフ", 
            false, (isOn) -> System.out.println("Bluetooth toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // 機内モード
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "airplane_mode", "機内モード", "すべての通信をオフにする", 
            false, (isOn) -> System.out.println("Airplane mode toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // モバイルデータ
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "mobile_data", "モバイルデータ", "携帯電話ネットワーク経由のデータ通信", 
            true, (isOn) -> System.out.println("Mobile data toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // 位置情報サービス
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "location", "位置情報", "GPS位置情報サービス", 
            true, (isOn) -> System.out.println("Location services toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // 自動回転
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "auto_rotate", "画面回転", "デバイスの向きに応じて画面を回転", 
            true, (isOn) -> System.out.println("Auto rotate toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // バッテリーセーバー
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "battery_saver", "バッテリーセーバー", "電力消費を抑制する省電力モード", 
            false, (isOn) -> System.out.println("Battery saver toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // ホットスポット
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "hotspot", "ホットスポット", "他のデバイスとの接続を共有", 
            false, (isOn) -> System.out.println("Hotspot toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // サイレントモード
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "silent_mode", "サイレント", "着信音と通知音をオフにする", 
            false, (isOn) -> System.out.println("Silent mode toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        // ダークモード
        toggleItem = new jp.moyashi.phoneos.core.controls.ToggleItem(
            "dark_mode", "ダークモード", "暗い色調のテーマを使用", 
            false, (isOn) -> System.out.println("Dark mode toggled: " + isOn)
        );
        controlCenterManager.addItem(toggleItem);
        
        System.out.println("  -> " + controlCenterManager.getItemCount() + "個のコントロールアイテムを追加完了");
    }
    
    /**
     * 表示状態に応じて動的優先度を更新する。
     * レイヤーの表示順序に基づいて、最上位のレイヤーが最高優先度を持つ。
     */
    private void updateDynamicPriorities() {
        // ベース優先度
        int basePriority = 100;
        
        // 両方とも表示されていない場合のデフォルト優先度
        if ((notificationManager == null || !notificationManager.isVisible()) &&
            (controlCenterManager == null || !controlCenterManager.isVisible())) {
            // 通知センターとコントロールセンター両方が非表示の場合、デフォルト優先度を設定
            if (notificationManager != null) {
                notificationManager.setDynamicPriority(900);  // デフォルト高優先度
            }
            if (controlCenterManager != null) {
                controlCenterManager.setDynamicPriority(1000); // デフォルト最高優先度
            }
            return;
        }
        
        // 現在表示中のレイヤーに基づいて優先度を設定
        // 描画順序: 通知センター（先に描画/下層） -> コントロールセンター（後に描画/上層）
        
        if (notificationManager != null && notificationManager.isVisible()) {
            if (controlCenterManager != null && controlCenterManager.isVisible()) {
                // 両方表示中: コントロールセンターが上層なので高優先度
                notificationManager.setDynamicPriority(basePriority + 100); // 通知センター: 200
                controlCenterManager.setDynamicPriority(basePriority + 200); // コントロールセンター: 300
            } else {
                // 通知センターのみ表示中
                notificationManager.setDynamicPriority(basePriority + 200); // 通知センター: 300
            }
        }
        
        if (controlCenterManager != null && controlCenterManager.isVisible()) {
            if (notificationManager == null || !notificationManager.isVisible()) {
                // コントロールセンターのみ表示中
                controlCenterManager.setDynamicPriority(basePriority + 200); // コントロールセンター: 300
            }
            // 両方表示中の場合は上記で既に設定済み
        }
    }
    
    /**
     * パターン入力エリアをハイライト表示する。
     * ロック中にホームボタンが押された際に呼び出される。
     */
    private void highlightPatternInput() {
        // 現在の画面がロック画面の場合、パターンハイライト機能を呼び出す
        if (screenManager != null) {
            Screen currentScreen = screenManager.getCurrentScreen();
            if (currentScreen instanceof jp.moyashi.phoneos.core.ui.lock.LockScreen) {
                jp.moyashi.phoneos.core.ui.lock.LockScreen lockScreen = 
                    (jp.moyashi.phoneos.core.ui.lock.LockScreen) currentScreen;
                lockScreen.highlightPatternArea();
            }
        }
    }
    
    /**
     * 指定されたコンポーネントがレイヤー管理システムで管理されているかチェックする。
     *
     * @param componentId コンポーネントID
     * @return レイヤー管理されている場合true
     */
    private boolean isComponentManagedByLayer(String componentId) {
        if (layerManager == null) return false;
        return layerManager.isLayerVisible(componentId);
    }

    // =========================================================================
    // スリープ機能
    // =========================================================================

    /**
     * スリープ状態に入る。
     * 画面がブラックアウトし、すべてのdraw()が停止する。
     * background()とtick()はそのまま動作する。
     */
    public void sleep() {
        // Phase 2リファクタリング: PowerManagerを使用
        if (powerManager != null) {
            if (powerManager.sleep()) {
                isSleeping = true; // 互換性のために保持
                System.out.println("Kernel: Device entering sleep mode (via PowerManager)");
                if (logger != null) {
                    logger.info("Kernel", "スリープモードに入りました (PowerManager経由)");
                }
            } else {
                System.out.println("Kernel: Sleep blocked by PowerManager");
                return;
            }
        } else if (!isSleeping) {
            // PowerManagerが利用できない場合の従来処理
            isSleeping = true;
            System.out.println("Kernel: Device entering sleep mode (legacy)");
            if (logger != null) {
                logger.info("Kernel", "スリープモードに入りました");
            }

            // スリープに入る際、現在のスクリーンをバックグラウンドに送る
            // これにより、WebViewのレンダリングが停止し、GPU使用率が削減される
            if (screenManager != null) {
                Screen currentScreen = screenManager.getCurrentScreen();
                if (currentScreen != null) {
                    currentScreen.onBackground();
                    System.out.println("Kernel: Current screen moved to background for sleep: " + currentScreen.getScreenTitle());
                    if (logger != null) {
                        logger.info("Kernel", "スクリーンをバックグラウンドに移行: " + currentScreen.getScreenTitle());
                    }
                }
            }

            // スリープに入る際に一度だけ黒背景を描画
            // 以降はrender()がスキップされるため、この黒い画面が維持される
            synchronized (renderLock) {
                if (graphics != null) {
                    graphics.beginDraw();
                    graphics.background(0, 0, 0); // 完全な黒背景
                    graphics.endDraw();
                    System.out.println("Kernel: Black screen drawn for sleep mode");
                }
            }
        }
    }

    /**
     * スリープ状態から復帰する。
     * ロック画面が表示される。
     * 注意: 既存のスクリーンスタックは保持され、ロック画面がその上にプッシュされる。
     * これにより、ロック解除後に前回のセッションを復帰できる。
     */
    public void wake() {
        // Phase 2リファクタリング: PowerManagerを使用
        if (powerManager != null) {
            if (powerManager.isSleeping()) {
                powerManager.wake();
                isSleeping = false; // 互換性のために保持
                System.out.println("Kernel: Device waking up from sleep mode (via PowerManager)");
                if (logger != null) {
                    logger.info("Kernel", "スリープモードから復帰しました (PowerManager経由)");
                }

                // ロック画面を表示（PowerManager経由でも必要）
                showLockScreenAfterWake();
            } else {
                System.out.println("Kernel: Not sleeping, cannot wake (PowerManager)");
                return;
            }
        } else if (isSleeping) {
            // PowerManagerが利用できない場合の従来処理
            isSleeping = false;
            System.out.println("Kernel: Device waking up from sleep mode (legacy)");
            if (logger != null) {
                logger.info("Kernel", "スリープモードから復帰しました");
            }

            // ロック画面を表示
            showLockScreenAfterWake();
        }
    }

    /**
     * スリープから復帰後にロック画面を表示する。
     * PowerManager経由と従来処理の両方から呼び出される共通処理。
     */
    private void showLockScreenAfterWake() {
        if (lockManager != null) {
            lockManager.lock(); // デバイスをロック状態にする

            // ロック画面に切り替え
            try {
                jp.moyashi.phoneos.core.ui.lock.LockScreen lockScreen =
                    new jp.moyashi.phoneos.core.ui.lock.LockScreen(this);

                // 既存のスクリーンスタックを保持したまま、ロック画面をプッシュ
                // 注意: clearAllScreens()は呼ばない（WebViewの破棄を防ぐため）
                if (screenManager != null) {
                    screenManager.pushScreen(lockScreen);
                    addLayer(LayerType.LOCK_SCREEN); // レイヤースタックに追加
                }

                System.out.println("Kernel: Wake up - lock screen pushed (screen stack preserved)");
                if (logger != null) {
                    logger.info("Kernel", "ロック画面を表示（スクリーンスタック保持）");
                }
            } catch (Exception e) {
                System.err.println("Kernel: Error displaying lock screen after wake: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * スリープ状態かどうかを取得する。
     *
     * @return スリープ状態の場合true
     */
    public boolean isSleeping() {
        return isSleeping;
    }

    /**
     * デバッグモードかどうかを取得する。
     * RenderPipeline等のコンポーネントから利用される。
     *
     * @return デバッグモードの場合true
     */
    public boolean isDebugMode() {
        // 設定からデバッグモードを取得（将来的実装）
        // 現在は環境変数またはシステムプロパティから判定
        String debug = System.getProperty("debug.mode", "false");
        if ("true".equalsIgnoreCase(debug)) {
            return true;
        }
        // または環境変数から
        String envDebug = System.getenv("MOCHI_DEBUG");
        return "true".equalsIgnoreCase(envDebug);
    }

    /**
     * フレームレートを設定する（PowerManager用）。
     *
     * @param fps ターゲットフレームレート
     */
    public void frameRate(int fps) {
        this.targetFrameRate = fps;
        if (logger != null) {
            logger.debug("Kernel", "Frame rate changed to: " + fps + " FPS");
        }
    }

    /**
     * 現在のフレームレートを取得する。
     *
     * @return ターゲットフレームレート
     */
    public int getFrameRate() {
        return targetFrameRate;
    }
}
