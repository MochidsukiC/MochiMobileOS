package jp.moyashi.phoneos.core.navigation;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.Kernel.LayerType;  // KernelのLayerTypeを使用
import jp.moyashi.phoneos.core.ui.ScreenManager;
import jp.moyashi.phoneos.core.service.ControlCenterManager;
import jp.moyashi.phoneos.core.service.NotificationManager;
import jp.moyashi.phoneos.core.service.LoggerService;
import jp.moyashi.phoneos.core.ui.popup.PopupManager;

import java.util.ArrayList;
import java.util.List;

/**
 * レイヤー管理を担当するコントローラー。
 * ホーム画面、アプリケーション、通知、コントロールセンター、ポップアップなどの
 * レイヤー階層を管理し、ホームボタン処理を制御する。
 * Kernelからレイヤー管理の責任を分離。
 *
 * 機能:
 * - レイヤースタックの管理
 * - ホームボタン処理の実装
 * - レイヤー優先順位の制御
 * - 各レイヤーマネージャーとの連携
 *
 * @since 2025-12-02
 * @version 1.0
 */
public class LayerController {

    /** LoggerService */
    private LoggerService logger;

    /** Kernelインスタンス */
    private final Kernel kernel;

    /** 現在開いているレイヤーのスタック */
    private final List<LayerType> layerStack;

    /** NavigationController（画面遷移制御） */
    private NavigationController navigationController;

    /** ScreenManager */
    private ScreenManager screenManager;

    /** PopupManager */
    private PopupManager popupManager;

    /** ControlCenterManager */
    private ControlCenterManager controlCenterManager;

    /** NotificationManager */
    private NotificationManager notificationManager;

    /** ホームボタン処理が有効か */
    private boolean homeButtonEnabled = true;

    /**
     * LayerControllerを初期化する。
     *
     * @param kernel Kernelインスタンス
     */
    public LayerController(Kernel kernel) {
        this.kernel = kernel;
        this.layerStack = new ArrayList<>();
        this.logger = kernel.getLogger();  // KernelからLoggerServiceを取得

        // 最初は常にホーム画面
        layerStack.add(LayerType.HOME_SCREEN);

        if (logger != null) {
            logger.info("LayerController", "LayerController initialized with HOME_SCREEN");
        }
    }

    /**
     * 各マネージャーを設定する。
     */
    public void setManagers(NavigationController navigationController,
                           ScreenManager screenManager,
                           PopupManager popupManager,
                           ControlCenterManager controlCenterManager,
                           NotificationManager notificationManager) {
        this.navigationController = navigationController;
        this.screenManager = screenManager;
        this.popupManager = popupManager;
        this.controlCenterManager = controlCenterManager;
        this.notificationManager = notificationManager;
    }

    /**
     * レイヤーを追加する。
     *
     * @param layer 追加するレイヤー
     */
    public void addLayer(LayerType layer) {
        if (!layerStack.contains(layer)) {
            layerStack.add(layer);
            if (logger != null) {
                logger.info("LayerController", "Added layer: " + layer);
            }
        }
    }

    /**
     * レイヤーを削除する。
     *
     * @param layer 削除するレイヤー
     */
    public void removeLayer(LayerType layer) {
        layerStack.remove(layer);
        if (logger != null) {
            logger.info("LayerController", "Removed layer: " + layer);
        }
    }

    /**
     * 特定のレイヤーが存在するかを確認する。
     *
     * @param layer 確認するレイヤー
     * @return 存在する場合 true
     */
    public boolean hasLayer(LayerType layer) {
        return layerStack.contains(layer);
    }

    /**
     * 最上位の閉じることができるレイヤーを取得する。
     * ロック画面は閉じることができないため除外される。
     *
     * @return 最上位の閉じることができるレイヤー、ない場合は null
     */
    public LayerType getTopMostClosableLayer() {
        // スタックを逆順に探索（最上位から）
        for (int i = layerStack.size() - 1; i >= 0; i--) {
            LayerType layer = layerStack.get(i);
            if (layer != LayerType.LOCK_SCREEN && layer != LayerType.HOME_SCREEN) {
                return layer;
            }
        }
        return null;
    }

    /**
     * ホームボタンを処理する。
     * 動的優先順位システムに基づいて、適切なレイヤーを閉じる。
     */
    public void handleHomeButton() {
        if (!homeButtonEnabled) {
            if (logger != null) {
                logger.info("LayerController", "Home button is disabled");
            }
            return;
        }

        // 現在のレイヤースタックをログ出力
        if (logger != null) {
            logger.info("LayerController", "Current layer stack: " + layerStack);
        }

        // 最上位の閉じることができるレイヤーを取得
        LayerType topLayer = getTopMostClosableLayer();

        if (topLayer == null) {
            if (logger != null) {
                logger.info("LayerController", "Already at home screen or locked");
            }
            return;
        }

        // レイヤーに応じた処理を実行
        switch (topLayer) {
            case POPUP:
        if (logger != null) {
                    logger.info("LayerController", "Closing popup layer");
                }
                if (popupManager != null) {
                    popupManager.closeCurrentPopup();
                }
                removeLayer(LayerType.POPUP);
                break;

            case CONTROL_CENTER:
                if (logger != null) {
                    logger.info("LayerController", "Closing control center");
                }
                if (controlCenterManager != null) {
                    // TODO: ControlCenterManagerにcloseControlCenter()メソッドを実装
                    // controlCenterManager.closeControlCenter();
                }
                removeLayer(LayerType.CONTROL_CENTER);
                break;

            case NOTIFICATION:
                if (logger != null) {
                    logger.info("LayerController", "Closing notification center");
                }
                if (notificationManager != null) {
                    // TODO: NotificationManagerにcloseNotificationCenter()メソッドを実装
                    // notificationManager.closeNotificationCenter();
                }
                removeLayer(LayerType.NOTIFICATION);
                break;

            case APPLICATION:
                if (logger != null) {
                    logger.info("LayerController", "Closing application, returning to home");
                }
                if (navigationController != null) {
                    navigationController.goToHome();
                }
                // アプリケーションレイヤーを削除してホーム画面のみに
                layerStack.clear();
                layerStack.add(LayerType.HOME_SCREEN);
                break;

            default:
                if (logger != null) {
                    logger.warn("LayerController", "Unknown layer type: " + topLayer);
                }
                break;
        }
    }

    /**
     * ロック画面を表示する。
     * ロック画面は特殊なレイヤーで、ホームボタンで閉じることができない。
     */
    public void showLockScreen() {
        if (!hasLayer(LayerType.LOCK_SCREEN)) {
            addLayer(LayerType.LOCK_SCREEN);

            if (navigationController != null) {
                navigationController.goToLockScreen();
            }

            if (logger != null) {
                logger.info("LayerController", "Lock screen shown");
            }
        }
    }

    /**
     * ロック画面を解除する。
     */
    public void dismissLockScreen() {
        if (hasLayer(LayerType.LOCK_SCREEN)) {
            removeLayer(LayerType.LOCK_SCREEN);

            // ホーム画面に戻る
            if (navigationController != null) {
                navigationController.goToHome();
            }

            if (logger != null) {
                logger.info("LayerController", "Lock screen dismissed");
            }
        }
    }

    /**
     * アプリケーションレイヤーをアクティブにする。
     */
    public void activateApplicationLayer() {
        if (!hasLayer(LayerType.APPLICATION)) {
            addLayer(LayerType.APPLICATION);
        }
    }

    /**
     * 通知センターレイヤーをアクティブにする。
     */
    public void activateNotificationLayer() {
        if (!hasLayer(LayerType.NOTIFICATION)) {
            addLayer(LayerType.NOTIFICATION);
        }
    }

    /**
     * コントロールセンターレイヤーをアクティブにする。
     */
    public void activateControlCenterLayer() {
        if (!hasLayer(LayerType.CONTROL_CENTER)) {
            addLayer(LayerType.CONTROL_CENTER);
        }
    }

    /**
     * ポップアップレイヤーをアクティブにする。
     */
    public void activatePopupLayer() {
        if (!hasLayer(LayerType.POPUP)) {
            addLayer(LayerType.POPUP);
        }
    }

    /**
     * 現在のレイヤースタックを取得する。
     *
     * @return レイヤースタックのコピー
     */
    public List<LayerType> getLayerStack() {
        return new ArrayList<>(layerStack);
    }

    /**
     * 最上位のレイヤーを取得する。
     *
     * @return 最上位のレイヤー
     */
    public LayerType getTopLayer() {
        if (layerStack.isEmpty()) {
            return null;
        }
        return layerStack.get(layerStack.size() - 1);
    }

    /**
     * ホームボタンを有効/無効にする。
     *
     * @param enabled 有効にする場合 true
     */
    public void setHomeButtonEnabled(boolean enabled) {
        this.homeButtonEnabled = enabled;
        if (logger != null) {
            logger.info("LayerController", "Home button " + (enabled ? "enabled" : "disabled"));
        }
    }

    /**
     * ホームボタンが有効かを判定する。
     *
     * @return 有効な場合 true
     */
    public boolean isHomeButtonEnabled() {
        return homeButtonEnabled;
    }

    /**
     * システムをリセットしてホーム画面に戻る。
     */
    public void resetToHome() {
        layerStack.clear();
        layerStack.add(LayerType.HOME_SCREEN);

        if (navigationController != null) {
            navigationController.goToHome();
        }

        if (logger != null) {
            logger.info("LayerController", "Reset to home screen");
        }
    }
}