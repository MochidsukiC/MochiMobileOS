package jp.moyashi.phoneos.core;

import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.service.*;
import jp.moyashi.phoneos.core.service.chromium.ChromiumManager;
import jp.moyashi.phoneos.core.service.chromium.ChromiumService;
import jp.moyashi.phoneos.core.service.chromium.DefaultChromiumService;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.ScreenManager;
import jp.moyashi.phoneos.core.ui.popup.PopupManager;
import jp.moyashi.phoneos.core.input.GestureManager;
import jp.moyashi.phoneos.core.input.GestureListener;
import jp.moyashi.phoneos.core.input.GestureEvent;
import jp.moyashi.phoneos.core.input.GestureType;
import jp.moyashi.phoneos.core.apps.launcher.LauncherApp;
import jp.moyashi.phoneos.core.apps.settings.SettingsApp;
import jp.moyashi.phoneos.core.apps.calculator.CalculatorApp;
import jp.moyashi.phoneos.core.ui.LayerManager;
import jp.moyashi.phoneos.core.coordinate.CoordinateTransform;
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

    /** 仮想ネットワークルーターサービス */
    private jp.moyashi.phoneos.core.service.network.VirtualRouter virtualRouter;

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

        // ESCキー長押し検出の更新
        if (escKeyPressed) {
            long elapsedTime = System.currentTimeMillis() - escKeyPressTime;
            if (elapsedTime >= LONG_PRESS_DURATION) {
                System.out.println("Kernel: ESCキー長押し検出 - シャットダウン開始");
                shutdown();
                escKeyPressed = false;
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

            // スリープ中の場合は描画処理を完全にスキップしてGPU使用率を削減
            if (isSleeping) {
                // 描画をスキップ（前フレームのバッファ内容を維持）
                // これにより、GPU処理が大幅に削減される
                return;
            }

            // ピクセルキャッシュを無効化（新しい描画が行われる）
            pixelsCacheDirty = true;

            // PGraphicsバッファへの描画開始
            graphics.beginDraw();

            try {

            // 日本語フォントを適用（全角文字表示のため）
            if (japaneseFont != null) {
                graphics.textFont(japaneseFont);
            }

            // まず背景を描画（重要：Screenが背景を描画しない場合のために）
            int bg = themeEngine != null ? themeEngine.colorBackground() : 0xFF000000;
            int br = (bg >> 16) & 0xFF;
            int bgc = (bg >> 8) & 0xFF;
            int bb = bg & 0xFF;
            graphics.background(br, bgc, bb);

            // スクリーンマネージャーによる通常描画
            if (screenManager != null) {
                try {
                    screenManager.draw(graphics);
                } catch (Exception e) {
                    System.err.println("Kernel: ScreenManager描画エラー: " + e.getMessage());

                    // エラー時のフォールバック表示
                    graphics.background(50, 50, 50); // ダークグレー背景
                    graphics.fill(255, 0, 0);
                    graphics.rect(50, height/2 - 50, width - 100, 100);
                    graphics.fill(255, 255, 255);
                    graphics.textAlign(PApplet.CENTER, PApplet.CENTER);
                    graphics.textSize(18);
                    graphics.text("画面エラー!", width/2, height/2 - 20);
                    graphics.textSize(12);
                    graphics.text("エラー: " + e.getMessage(), width/2, height/2);
                }
            } else {
                // ScreenManagerが未初期化の場合の表示
                // テーマ背景
                int tbg = themeEngine != null ? themeEngine.colorBackground() : 0xFF1E1E1E;
                graphics.background((tbg>>16)&0xFF, (tbg>>8)&0xFF, (tbg)&0xFF);
                graphics.fill(255, 255, 255);
                graphics.textAlign(PApplet.CENTER, PApplet.CENTER);
                graphics.textSize(16);
                graphics.text("システム初期化中...", width/2, height/2);
            }

            // 通知センターの描画
            if (notificationManager != null) {
                try {
                    notificationManager.draw(graphics);
                } catch (Exception e) {
                    System.err.println("Kernel: NotificationManager描画エラー: " + e.getMessage());
                }
            }

            // コントロールセンターの描画
            if (controlCenterManager != null) {
                try {
                    controlCenterManager.draw(graphics);
                } catch (Exception e) {
                    System.err.println("Kernel: ControlCenterManager描画エラー: " + e.getMessage());
                }
            }

            // ポップアップの描画
            if (popupManager != null) {
                try {
                    popupManager.draw(graphics);
                } catch (Exception e) {
                    System.err.println("Kernel: PopupManager描画エラー: " + e.getMessage());
                }
            }

            } catch (Exception e) {
                System.err.println("Kernel: 描画処理中にエラーが発生: " + e.getMessage());
                e.printStackTrace();
            } finally {
                // PGraphicsバッファへの描画終了
                graphics.endDraw();
            }
        }
    }

    /**
     * マウスクリック処理（独立API）。
     *
     * @param x マウスX座標
     * @param y マウスY座標
     */
    public void mousePressed(int x, int y) {
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
        System.out.println("Kernel: keyPressed - key: '" + key + "', keyCode: " + keyCode);

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

            // スペースキー（ホームボタン）の階層管理処理
            if (key == ' ' || keyCode == 32) {
                System.out.println("Kernel: Space key pressed - checking lock screen and focus status");

                // ロック画面が表示されている場合は、ロック画面に処理を委譲
                if (layerStack.contains(LayerType.LOCK_SCREEN)) {
                    System.out.println("Kernel: Lock screen is active - forwarding space key to screen manager");
                    if (screenManager != null) {
                        screenManager.keyPressed(key, keyCode);
                    }
                    return;
                }

                // テキスト入力フォーカスがある場合は、スペースキーをスクリーンに転送
                if (screenManager != null && screenManager.hasFocusedComponent()) {
                    System.out.println("Kernel: Text input focused - forwarding space key to screen manager");
                    screenManager.keyPressed(key, keyCode);
                    return;
                }

                // フォーカスがない場合は、通常のホームボタン処理
                System.out.println("Kernel: No focus - handling home button");
                handleHomeButton();
                return;
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

        // 動的レイヤー管理システムを初期化
        System.out.println("  -> 動的レイヤー管理システム作成中...");
        layerStack = new ArrayList<>();
        layerStack.add(LayerType.HOME_SCREEN); // 最初は常にホーム画面

        // 統一座標変換システムを初期化
        System.out.println("  -> 統一座標変換システム作成中...");
        coordinateTransform = new CoordinateTransform(width, height);

        // コアサービスの初期化（LoggerService用にVFSを先に初期化）
        System.out.println("  -> VFS（仮想ファイルシステム）作成中...");
        if (worldId != null && !worldId.isEmpty()) {
            System.out.println("     World ID: " + worldId);
        }
        vfs = new VFS(worldId);

        System.out.println("  -> OSロガーサービス作成中...");
        logger = new LoggerService(vfs);
        // Enable DEBUG logging for troubleshooting
        logger.setLogLevel(jp.moyashi.phoneos.core.service.LoggerService.LogLevel.DEBUG);
        logger.info("Kernel", "=== MochiMobileOS カーネル初期化開始 ===");
        logger.info("Kernel", "画面サイズ: " + width + "x" + height);
        if (worldId != null && !worldId.isEmpty()) {
            logger.info("Kernel", "World ID: " + worldId);
        }

        System.out.println("  -> サービスマネージャー作成中...");
        serviceManager = new ServiceManager(this);
        serviceManager.initialize();
        logger.info("Kernel", "サービスマネージャー初期化完了");

        // 日本語フォントの初期化（LoggerService使用）
        logger.info("Kernel", "日本語フォントを初期化中...");
        japaneseFont = loadJapaneseFont();
        if (japaneseFont != null) {
            logger.info("Kernel", "日本語フォント (Noto Sans JP) を正常に読み込みました");
        } else {
            logger.warn("Kernel", "日本語フォントの読み込みに失敗しました。デフォルトフォントを使用します");
        }

        System.out.println("  -> 設定マネージャー作成中...");
        settingsManager = new SettingsManager(vfs);

        // テーマエンジン初期化（設定を購読し即時反映）
        System.out.println("  -> テーマエンジン作成中...");
        themeEngine = new jp.moyashi.phoneos.core.ui.theme.ThemeEngine(settingsManager);
        jp.moyashi.phoneos.core.ui.theme.ThemeContext.setTheme(themeEngine);
        
        System.out.println("  -> システムクロック作成中...");
        systemClock = new SystemClock();
        
        System.out.println("  -> アプリケーションローダー作成中...");
        appLoader = new AppLoader(vfs);
        
        // アプリケーションをスキャンして読み込む
        System.out.println("  -> 外部アプリケーションをスキャン中...");
        appLoader.scanForApps();
        
        System.out.println("  -> レイアウト管理サービス作成中...");
        layoutManager = new LayoutManager(vfs, appLoader);
        
        System.out.println("  -> グローバルポップアップマネージャー作成中...");
        popupManager = new PopupManager();
        
        System.out.println("  -> Kernelレベルジェスチャーマネージャー作成中...");
        gestureManager = new GestureManager(logger);
        
        System.out.println("  -> コントロールセンター管理サービス作成中...");
        controlCenterManager = new ControlCenterManager();
        controlCenterManager.setGestureManager(gestureManager);
        controlCenterManager.setCoordinateTransform(coordinateTransform);
        setupControlCenter();
        
        System.out.println("  -> 通知センター管理サービス作成中...");
        notificationManager = new NotificationManager();
        notificationManager.setKernel(this); // Kernelの参照を設定
        
        System.out.println("  -> ロック状態管理サービス作成中...");
        lockManager = new LockManager(settingsManager);
        
        System.out.println("  -> 動的レイヤー管理システム作成中...");
        layerManager = new LayerManager(gestureManager);

        System.out.println("  -> 仮想ネットワークルーター作成中...");
        virtualRouter = new jp.moyashi.phoneos.core.service.network.VirtualRouter();

        System.out.println("  -> メッセージストレージサービス作成中...");
        messageStorage = new MessageStorage(vfs);

        // ハードウェアバイパスAPIの初期化（デフォルト実装）
        System.out.println("  -> ハードウェアバイパスAPI作成中...");
        mobileDataSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultMobileDataSocket();
        bluetoothSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultBluetoothSocket();
        locationSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultLocationSocket();
        batteryInfo = new jp.moyashi.phoneos.core.service.hardware.DefaultBatteryInfo();
        cameraSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultCameraSocket();
        microphoneSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultMicrophoneSocket();
        speakerSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultSpeakerSocket();
        icSocket = new jp.moyashi.phoneos.core.service.hardware.DefaultICSocket();
        simInfo = new jp.moyashi.phoneos.core.service.hardware.DefaultSIMInfo();

        // バッテリー監視サービスの初期化
        System.out.println("  -> BatteryMonitor初期化中...");
        batteryMonitor = new BatteryMonitor(batteryInfo, settingsManager);

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

        // センサー管理サービスの初期化
        System.out.println("  -> センサー管理サービス作成中...");
        sensorManager = new jp.moyashi.phoneos.core.service.sensor.SensorManagerImpl(this);
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

        System.out.println("Kernel: " + appLoader.getLoadedApps().size() + " 個のアプリケーションを登録");

        // すべてのアプリ登録後に初期化を実行
        System.out.println("  -> アプリケーションを初期化中...");
        launcherApp.onInitialize(this);
        settingsApp.onInitialize(this);
        calculatorApp.onInitialize(this);
        networkApp.onInitialize(this);
        hardwareTestApp.onInitialize(this);
        noteApp.onInitialize(this);
        chromiumBrowserApp.onInitialize(this);

        // スクリーンマネージャーを初期化してランチャーを初期画面に設定
        System.out.println("  -> スクリーンマネージャー作成中...");
        screenManager = new ScreenManager();
        System.out.println("✅ ScreenManager作成済み: " + (screenManager != null));

        // ScreenManagerにKernelインスタンスを設定（レイヤー管理統合のため）
        screenManager.setKernel(this);

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
        return shiftPressed;
    }

    /**
     * Ctrlキーが押されているかどうかを取得する。
     *
     * @return Ctrlキーが押されている場合true
     */
    public boolean isCtrlPressed() {
        return ctrlPressed;
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
     * モバイルデータ通信ソケットを設定する（forge-mod用）。
     *
     * @param socket モバイルデータ通信ソケット
     */
    public void setMobileDataSocket(jp.moyashi.phoneos.core.service.hardware.MobileDataSocket socket) {
        this.mobileDataSocket = socket;
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
    private void handleHomeButton() {
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
        if (!isSleeping) {
            isSleeping = true;
            System.out.println("Kernel: Device entering sleep mode");
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
        if (isSleeping) {
            isSleeping = false;
            System.out.println("Kernel: Device waking up from sleep mode");
            if (logger != null) {
                logger.info("Kernel", "スリープモードから復帰しました");
            }

            // ロック画面を表示
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
    }

    /**
     * スリープ状態かどうかを取得する。
     *
     * @return スリープ状態の場合true
     */
    public boolean isSleeping() {
        return isSleeping;
    }
}
