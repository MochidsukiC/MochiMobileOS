package jp.moyashi.phoneos.core.ui;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.Kernel.LayerType;
import jp.moyashi.phoneos.core.apps.launcher.ui.SafeHomeScreen;
import jp.moyashi.phoneos.core.apps.launcher.ui.AppLibraryScreen;
import jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen;
import jp.moyashi.phoneos.core.ui.animation.ScreenTransition;
import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PGraphics;
import java.util.Stack;

/**
 * スマートフォンOSでのスクリーン遷移とナビゲーションを扱うスクリーンマネージャー。
 * このクラスはスクリーンのスタックを管理し、モバイルアプリナビゲーションと同様の
 * push/pop操作で階層的なナビゲーションを可能にする。
 * 
 * @author YourName
 * @version 1.0
 */
public class ScreenManager implements ScreenTransition.AnimationCallback {
    
    /** ナビゲーション管理用のスクリーンスタック */
    private Stack<Screen> screenStack;
    
    /** アニメーション管理システム */
    private ScreenTransition screenTransition;
    
    /** 現在描画中のPAppletインスタンス */
    private PApplet currentPApplet;
    
    /** アニメーション中に新しくプッシュされたスクリーンの描画を抑制するフラグ */
    private Screen animatingScreen;
    
    /** setup()が呼ばれていないスクリーンを追跡するためのリスト */
    private java.util.Set<Screen> unsetupScreens;

    /** Kernelインスタンスへの参照（レイヤー管理のため） */
    private Kernel kernel;

    /** 修飾キー状態 - Shiftキー */
    private boolean shiftPressed = false;

    /** 修飾キー状態 - Ctrlキー */
    private boolean ctrlPressed = false;

    /**
     * ロガーヘルパーメソッド。
     *
     * @param message ログメッセージ
     */
    private void log(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().debug("ScreenManager", message);
        }
        System.out.println("ScreenManager: " + message);
    }

    /**
     * エラーロガーヘルパーメソッド。
     *
     * @param message ログメッセージ
     */
    private void logError(String message) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("ScreenManager", message);
        }
        System.err.println("ScreenManager: " + message);
    }

    /**
     * エラーロガーヘルパーメソッド（例外付き）。
     *
     * @param message ログメッセージ
     * @param throwable 例外
     */
    private void logError(String message, Throwable throwable) {
        if (kernel != null && kernel.getLogger() != null) {
            kernel.getLogger().error("ScreenManager", message, throwable);
        }
        System.err.println("ScreenManager: " + message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    /**
     * 新しいScreenManagerインスタンスを構築する。
     * ナビゲーション管理用のスクリーンスタックを初期化する。
     */
    public ScreenManager() {
        screenStack = new Stack<>();
        screenTransition = new ScreenTransition();
        screenTransition.setAnimationCallback(this); // Set callback to handle animation completion
        unsetupScreens = new java.util.HashSet<>();
        log("Screen manager initialized with animation support");
    }

    /**
     * Kernelインスタンスを設定する。レイヤー管理との統合に必要。
     *
     * @param kernel Kernelインスタンス
     */
    public void setKernel(Kernel kernel) {
        this.kernel = kernel;
        log("Kernel reference set for layer management integration");
    }
    
    /**
     * ナビゲーションスタックに新しいスクリーンをプッシュする。
     * 新しいスクリーンがアクティブスクリーンになり、そのsetup()メソッドが呼び出される。
     *
     * @param screen スタックにプッシュするスクリーン
     */
    public void pushScreen(Screen screen) {
        if (screen != null) {
            // 古いトップスクリーンをバックグラウンドに移行（OS側で強制的に制御）
            Screen previousScreen = getCurrentScreen();
            if (previousScreen != null) {
                previousScreen.onBackground();
                log("Previous screen moved to background: " + previousScreen.getScreenTitle());
            }

            screenStack.push(screen);
            // setup()にPGraphicsを渡すため、currentPAppletが利用可能な場合のみ呼び出し
            if (currentPApplet != null) {
                screen.setup(currentPApplet.g);
            } else {
                // PAppletが利用できない場合、後で初期化するためにリストに追加
                unsetupScreens.add(screen);
                log("Screen " + screen.getScreenTitle() + " queued for setup (PApplet not available)");
            }

            // 新しくプッシュされたスクリーンをフォアグラウンドに設定（OS側で強制的に制御）
            // スクリーンが再利用される場合（ServiceManager経由）でも、確実にフォアグラウンド状態にする
            screen.onForeground();
            log("New screen moved to foreground: " + screen.getScreenTitle());

            // アプリケーション画面の場合はKernelレイヤースタックにAPPLICATIONレイヤーを追加
            boolean isLauncher = isLauncherScreen(screen);
            log("Screen analysis - Title: '" + screen.getScreenTitle() + "', ClassName: '" + screen.getClass().getSimpleName() + "', isLauncher: " + isLauncher);
            if (kernel != null && !isLauncher) {
                kernel.addLayer(LayerType.APPLICATION);
                log("Added APPLICATION layer to Kernel stack for screen: " + screen.getScreenTitle());
            } else {
                log("Skipped APPLICATION layer addition - kernel=" + (kernel != null) + ", isLauncher=" + isLauncher);
            }

            log("Pushed screen - " + screen.getScreenTitle());
        }
    }
    
    /**
     * アニメーション付きでスクリーンをプッシュする（アイコンからの起動）。
     *
     * @param screen スタックにプッシュするスクリーン
     * @param iconX アイコンの中心X座標
     * @param iconY アイコンの中心Y座標
     * @param iconSize アイコンのサイズ
     * @param iconImage アイコンの画像
     */
    public void pushScreenWithAnimation(Screen screen, float iconX, float iconY, float iconSize, PImage iconImage) {
        log("pushScreenWithAnimation called");
        log("screen=" + (screen != null ? screen.getScreenTitle() : "null"));
        log("currentPApplet=" + (currentPApplet != null ? "available" : "null"));
        log("iconImage=" + (iconImage != null ? iconImage.width + "x" + iconImage.height : "null"));
        log("iconPosition=(" + iconX + ", " + iconY + "), iconSize=" + iconSize);

        if (screen != null && currentPApplet != null) {
            // 古いトップスクリーンをバックグラウンドに移行（OS側で強制的に制御）
            Screen previousScreen = getCurrentScreen();
            if (previousScreen != null) {
                previousScreen.onBackground();
                log("Previous screen moved to background (animation): " + previousScreen.getScreenTitle());
            }

            // スクリーンを即座にプッシュするが、アニメーション中は描画をブロック
            screenStack.push(screen);

            // デバッグ: setup()呼び出し前
            log("Calling setup() on screen: " + screen.getScreenTitle() + ", PGraphics available: " + (currentPApplet.g != null));
            try {
                screen.setup(currentPApplet.g);
                log("setup() completed successfully for: " + screen.getScreenTitle());
            } catch (Exception e) {
                logError("Error calling setup() on " + screen.getScreenTitle() + ": " + e.getMessage(), e);
            }

            unsetupScreens.remove(screen); // セットアップ完了なのでリストから削除
            animatingScreen = screen; // このスクリーンはアニメーション中

            // 新しくプッシュされたスクリーンをフォアグラウンドに設定（OS側で強制的に制御）
            // スクリーンが再利用される場合（ServiceManager経由）でも、確実にフォアグラウンド状態にする
            screen.onForeground();
            log("New screen moved to foreground (animation): " + screen.getScreenTitle());

            // アプリケーション画面の場合はKernelレイヤースタックにAPPLICATIONレイヤーを追加
            boolean isLauncher = isLauncherScreen(screen);
            log("Animated screen analysis - Title: '" + screen.getScreenTitle() + "', ClassName: '" + screen.getClass().getSimpleName() + "', isLauncher: " + isLauncher);
            if (kernel != null && !isLauncher) {
                kernel.addLayer(LayerType.APPLICATION);
                log("Added APPLICATION layer to Kernel stack for animated screen: " + screen.getScreenTitle());
            } else {
                log("Skipped APPLICATION layer addition for animation - kernel=" + (kernel != null) + ", isLauncher=" + isLauncher);
            }

            // アニメーションを開始
            log("Setting target dimensions: " + currentPApplet.width + "x" + currentPApplet.height);
            screenTransition.setTargetDimensions(0, 0, currentPApplet.width, currentPApplet.height);

            log("Starting zoom-in animation...");
            screenTransition.startZoomIn(iconX, iconY, iconSize, iconImage);

            log("Animation setup complete for screen - " + screen.getScreenTitle());
        } else {
            log("Cannot start animation - missing screen or PApplet");
        }
    }
    
    /**
     * ナビゲーションスタックから現在のスクリーンをポップする。
     * 前のスクリーンが再びアクティブになる。
     * ポップされたスクリーンのcleanup()を呼び出す。
     *
     * @return ポップされたスクリーン、またはスタックが空の場合null
     */
    public Screen popScreen() {
        if (!screenStack.isEmpty()) {
            Screen poppedScreen = screenStack.pop();
            if (currentPApplet != null) {
                poppedScreen.cleanup(currentPApplet);
            }
            // 未セットアップリストからも削除
            unsetupScreens.remove(poppedScreen);

            // アプリケーション画面の場合はKernelレイヤースタックからAPPLICATIONレイヤーを削除
            if (kernel != null && !isLauncherScreen(poppedScreen)) {
                kernel.removeLayer(LayerType.APPLICATION);
                log("Removed APPLICATION layer from Kernel stack for screen: " + poppedScreen.getScreenTitle());
            }

            // 新しいトップスクリーンをフォアグラウンドに復帰（OS側で強制的に制御）
            Screen newTopScreen = getCurrentScreen();
            if (newTopScreen != null) {
                newTopScreen.onForeground();
                log("New top screen moved to foreground: " + newTopScreen.getScreenTitle());
            }

            log("Popped screen - " + poppedScreen.getScreenTitle());
            return poppedScreen;
        }
        return null;
    }
    
    /**
     * アニメーション付きでスクリーンをポップする（アイコンへの終了）。
     *
     * @param iconX 戻り先アイコンの中心X座標
     * @param iconY 戻り先アイコンの中心Y座標
     * @param iconSize アイコンのサイズ
     * @param iconImage アイコンの画像
     * @return ポップされたスクリーン、またはスタックが空の場合null
     */
    public Screen popScreenWithAnimation(float iconX, float iconY, float iconSize, PImage iconImage) {
        if (!screenStack.isEmpty() && currentPApplet != null) {
            // 現在のスクリーンをキャプチャ（現在の描画を使用）
            PGraphics screenCapture = currentPApplet.createGraphics(currentPApplet.width, currentPApplet.height);
            screenCapture.beginDraw();

            // 現在のフレームバッファからコピー
            screenCapture.image(currentPApplet.get(), 0, 0);

            screenCapture.endDraw();

            // スクリーンをポップ
            Screen poppedScreen = screenStack.pop();
            if (currentPApplet != null) {
                poppedScreen.cleanup(currentPApplet);
            }
            // 未セットアップリストからも削除
            unsetupScreens.remove(poppedScreen);

            // 新しいトップスクリーンをフォアグラウンドに復帰（OS側で強制的に制御）
            Screen newTopScreen = getCurrentScreen();
            if (newTopScreen != null) {
                newTopScreen.onForeground();
                log("New top screen moved to foreground (animation): " + newTopScreen.getScreenTitle());
            }

            // アニメーションを開始
            screenTransition.startZoomOut(iconX, iconY, iconSize, iconImage, screenCapture);

            log("Popped screen with zoom-out animation - " + poppedScreen.getScreenTitle());
            return poppedScreen;
        }
        return null;
    }
    
    /**
     * スタックから除去することなく現在アクティブなスクリーンを取得する。
     * 
     * @return 現在のスクリーン、またはスタックが空の場合null
     */
    public Screen getCurrentScreen() {
        if (!screenStack.isEmpty()) {
            return screenStack.peek();
        }
        return null;
    }
    
    /**
     * 全てのスクリーン（バックグラウンドも含む）のtick()を呼び出す。
     * このメソッドは毎フレーム実行され、バックグラウンドタスク（通知フェッチなど）の処理継続を保証する。
     * tick()はスリープ中でも動作し、重要なバックグラウンド処理を継続する。
     */
    public void tick() {
        // スタック内の全スクリーンのtick()を呼び出し（バックグラウンドも含む）
        for (Screen screen : screenStack) {
            try {
                screen.tick();
            } catch (Exception e) {
                logError("Error in tick() for screen " + screen.getScreenTitle() + ": " + e.getMessage(), e);
            }
        }
    }

    /**
     * 現在アクティブなスクリーンを描画する（PGraphics版）。
     * PGraphics統一アーキテクチャで使用する。
     *
     * @param g 描画操作用のPGraphicsインスタンス
     */
    public void draw(PGraphics g) {
        // スリープ中の場合は黒背景のみ描画
        if (kernel != null && kernel.isSleeping()) {
            g.background(0);
            return;
        }

        // 初回draw()呼び出し時に、未初期化のスクリーンのsetup()を呼び出す
        ensureCurrentScreenSetup();

        // アニメーション更新（一時的にスキップ - ScreenTransitionのPGraphics対応が必要）
        if (screenTransition != null && screenTransition.isAnimating()) {
            screenTransition.update();

            // アニメーション完了チェック
            if (!screenTransition.isAnimating()) {
                onAnimationComplete(ScreenTransition.AnimationState.NONE, null);
            }
        }

        // 通常の画面描画（アニメーション中でも実行）
        {
            // 通常の画面描画
            Screen currentScreen = getCurrentScreen();
            if (currentScreen != null) {
                try {
                    // デバッグ：現在描画しているスクリーンを表示
                    if (currentScreen.getClass().getSimpleName().contains("HTML")) {
                        if (kernel != null && kernel.getLogger() != null) {
                            kernel.getLogger().debug("ScreenManager", "Drawing current screen: " + currentScreen.getScreenTitle());
                        }
                    }
                    currentScreen.draw(g);
                } catch (Exception e) {
                    logError("Error drawing current screen: " + e.getMessage(), e);

                    // エラーメッセージを描画
                    g.fill(255, 0, 0);
                    g.rect(50, 200, 300, 100);
                    g.fill(255, 255, 255);
                    g.textAlign(PApplet.CENTER, PApplet.CENTER);
                    g.textSize(18);
                    g.text("画面描画エラー", 200, 230);
                    g.textSize(12);
                    g.text("Error: " + e.getMessage(), 200, 250);
                }
            }
        }
    }

    /**
     * 現在アクティブなスクリーンを描画する（PApplet版）。
     * 互換性のために残存。段階的にPGraphics版に移行予定。
     *
     * @param p 描画操作用のPAppletインスタンス
     */
    public void draw(PApplet p) {
        // PAppletインスタンスを保存
        this.currentPApplet = p;
        
        // 初回draw()呼び出し時に、未初期化のスクリーンのsetup()を呼び出す
        ensureCurrentScreenSetup();
        
        // アニメーション更新
        screenTransition.update();
        
        boolean isAnimating = screenTransition.isAnimating();
        if (isAnimating) {
            log("draw() - Animation is active: " + screenTransition.getCurrentState() + ", progress: " + screenTransition.getProgress());
        }
        
        // アニメーション中でない場合、通常のスクリーン描画
        if (!isAnimating) {
            Screen currentScreen = getCurrentScreen();
            if (currentScreen != null) {
                currentScreen.draw(p);
            } else {
                // Draw a default screen if no screens are active
                drawEmptyScreen(p);
            }
        } else {
            log("Rendering animation - " + screenTransition.getCurrentState());
            // アニメーション中の場合
            switch (screenTransition.getCurrentState()) {
                case ZOOM_IN:
                    // ズームインアニメーション中は背景として前のスクリーンを描画（アニメーション対象スクリーンは除く）
                    if (screenStack.size() > 1 && animatingScreen != null) {
                        // アニメーション対象スクリーンを除いた前のスクリーン
                        Screen backgroundScreen = screenStack.get(screenStack.size() - 2);
                        log("Drawing background screen during zoom-in: " + backgroundScreen.getScreenTitle());
                        backgroundScreen.draw(p);
                    } else {
                        log("Drawing empty screen during zoom-in");
                        drawEmptyScreen(p);
                    }
                    log("Drawing zoom-in animation overlay");
                    screenTransition.draw(p);
                    break;
                    
                case ZOOM_OUT:
                    // ズームアウトアニメーション中は次のスクリーンを背景として描画
                    Screen currentScreen = getCurrentScreen();
                    if (currentScreen != null) {
                        currentScreen.draw(p);
                    } else {
                        drawEmptyScreen(p);
                    }
                    screenTransition.draw(p);
                    break;
                    
                default:
                    // その他のアニメーション
                    screenTransition.draw(p);
                    break;
            }
        }
    }
    
    /**
     * 現在のスクリーンに委託してマウスプレスイベントを処理する。
     * 
     * @param mouseX マウスプレスのx座標
     * @param mouseY マウスプレスのy座標
     */
    public void mousePressed(int mouseX, int mouseY) {
        // Block mouse events during animations
        if (screenTransition.isAnimating()) {
            return;
        }

        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null && currentPApplet != null) {
            currentScreen.mousePressed(currentPApplet, mouseX, mouseY);
        }
    }
    
    /**
     * 現在のスクリーンに委託してマウスドラッグイベントを処理する。
     * 
     * @param mouseX マウス位置のx座標
     * @param mouseY マウス位置のy座標
     */
    public void mouseDragged(int mouseX, int mouseY) {
        // Block mouse events during animations
        if (screenTransition.isAnimating()) {
            return;
        }

        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null && currentPApplet != null) {
            currentScreen.mouseDragged(currentPApplet, mouseX, mouseY);
        }
    }

    /**
     * 現在のスクリーンに委託してマウスリリースイベントを処理する。
     *
     * @param mouseX マウス位置のx座標
     * @param mouseY マウス位置のy座標
     */
    public void mouseReleased(int mouseX, int mouseY) {
        // Block mouse events during animations
        if (screenTransition.isAnimating()) {
            return;
        }

        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null && currentPApplet != null) {
            currentScreen.mouseReleased(currentPApplet, mouseX, mouseY);
        }
    }

    /**
     * 現在のスクリーンに委託してマウスホイールイベントを処理する。
     *
     * @param mouseX マウス位置のx座標
     * @param mouseY マウス位置のy座標
     * @param delta スクロール量（正の値：下スクロール、負の値：上スクロール）
     */
    public void mouseWheel(int mouseX, int mouseY, float delta) {
        // Block mouse events during animations
        if (screenTransition.isAnimating()) {
            return;
        }

        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null && currentPApplet != null) {
            currentScreen.mouseWheel(currentPApplet, mouseX, mouseY, delta);
        }
    }

    /**
     * 現在のスクリーンに委託してキーボード入力イベントを処理する。
     *
     * @param key 押されたキー
     * @param keyCode キーコード
     */
    public void keyPressed(char key, int keyCode) {
        // アニメーション中はイベントを無視
        if (screenTransition.isAnimating()) {
            return;
        }

        // スペースキー（ホームボタン）の処理
        if (key == ' ' || keyCode == 32) {
            Screen currentScreen = getCurrentScreen();

            // ロック画面の場合は例外的にスペースキーを転送（解除フィールド表示のため）
            if (currentScreen != null && currentScreen.getClass().getSimpleName().equals("LockScreen")) {
                log("Space key on lock screen - forwarding to screen");
                currentScreen.keyPressed(currentPApplet, key, keyCode);
                return;
            }

            // 通常のアプリケーション画面の場合はホーム画面に戻る
            log("Space key detected - returning to home screen");
            navigateToHome();
            return;
        }

        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null && currentPApplet != null) {
            currentScreen.keyPressed(currentPApplet, key, keyCode);
        }
    }

    /**
     * ナビゲーションスタック内のスクリーン数を取得する。
     * 
     * @return スタックのサイズ
     */
    public int getStackSize() {
        return screenStack.size();
    }
    
    /**
     * ナビゲーションスタックにスクリーンがあるかどうかを確認する。
     * 
     * @return スタックが空の場合true、そうでなければfalse
     */
    public boolean isEmpty() {
        return screenStack.isEmpty();
    }
    
    /**
     * ナビゲーションスタックからすべてのスクリーンをクリアする。
     * スクリーンを除去する前にそれぞれのcleanup()を呼び出す。
     */
    public void clearAllScreens() {
        while (!screenStack.isEmpty()) {
            popScreen();
        }
        log("All screens cleared");
    }

    
    /**
     * 現在のスクリーンがまだsetup()されていない場合、遅延setup()を実行する。
     * PAppletが利用可能になった際の初回draw()で呼び出される。
     */
    private void ensureCurrentScreenSetup() {
        if (currentPApplet != null && !unsetupScreens.isEmpty()) {
            // 未初期化スクリーンのセットアップを実行
            java.util.Iterator<Screen> iterator = unsetupScreens.iterator();
            while (iterator.hasNext()) {
                Screen screen = iterator.next();
                try {
                    log("Delayed setup for screen - " + screen.getScreenTitle());
                    screen.setup(currentPApplet.g);
                    iterator.remove(); // セットアップ完了後にリストから削除
                } catch (Exception e) {
                    logError("Error in delayed setup for " + screen.getScreenTitle() + ": " + e.getMessage(), e);
                    iterator.remove(); // エラーでもリストから削除
                }
            }
        }
    }
    
    /**
     * アクティブなスクリーンがない時にデフォルトの空のスクリーンを描画する。
     * 
     * @param p 描画操作用のPAppletインスタンス
     */
    private void drawEmptyScreen(PApplet p) {
        p.background(50); // Dark gray background
        p.fill(255);      // White text
        p.textAlign(p.CENTER, p.CENTER);
        p.textSize(16);
        p.text("No active screens", p.width / 2, p.height / 2);
    }
    
    /**
     * Animation completion callback implementation.
     * Called when an animation completes and handles pending screen operations.
     * 
     * @param completedState The animation state that just completed
     * @param pendingScreen The screen object to handle after animation completion
     */
    @Override
    public void onAnimationComplete(ScreenTransition.AnimationState completedState, Object pendingScreen) {
        log("Animation completed - " + completedState);
        
        switch (completedState) {
            case ZOOM_IN:
                // アニメーション完了でアニメーション対象スクリーンの描画を許可
                if (animatingScreen != null) {
                    log("Animation completed - allowing " + animatingScreen.getScreenTitle() + " to be drawn");
                    animatingScreen = null; // アニメーションフラグをクリア
                }
                break;
                
            case ZOOM_OUT:
                log("Zoom-out animation completed");
                animatingScreen = null;
                break;
                
            default:
                log("Unhandled animation completion - " + completedState);
                animatingScreen = null;
                break;
        }
    }

    /**
     * PAppletインスタンスを設定し、未初期化スクリーンのsetup()を実行する。
     * PGraphics統一アーキテクチャ対応のため追加されたメソッド。
     *
     * @param pApplet PAppletインスタンス
     */
    public void setCurrentPApplet(PApplet pApplet) {
        this.currentPApplet = pApplet;
        log("PAppletを設定 - " + (pApplet != null ? "成功" : "null"));

        // 未初期化のスクリーンがある場合、setup()を実行
        if (pApplet != null && !unsetupScreens.isEmpty()) {
            log(unsetupScreens.size() + "個の未初期化スクリーンのsetup()を実行中...");
            for (Screen screen : unsetupScreens.toArray(new Screen[0])) {
                try {
                    screen.setup(pApplet.g);
                    unsetupScreens.remove(screen);
                    log(screen.getScreenTitle() + "のsetup()完了");
                } catch (Exception e) {
                    logError(screen.getScreenTitle() + "のsetup()でエラー: " + e.getMessage(), e);
                }
            }
            log("全ての未初期化スクリーンのsetup()完了");
        }
    }

    /**
     * スクリーンがランチャー関連の画面かどうかを判定する。
     * ランチャー関連の画面（HomeScreen、AppLibraryScreen、SafeHomeScreen等）には
     * APPLICATIONレイヤーを追加しない。
     *
     * @param screen 判定するスクリーン
     * @return ランチャー関連の画面の場合true
     */
    private boolean isLauncherScreen(Screen screen) {
        if (screen == null) return false;

        String className = screen.getClass().getSimpleName();
        return className.equals("HomeScreen") ||
               className.equals("AppLibraryScreen") ||
               className.equals("SafeHomeScreen") ||
               className.equals("SimpleHomeScreen") ||
               className.equals("BasicHomeScreen") ||
               className.equals("LockScreen");
    }

    /**
     * ホーム画面に戻る処理を実行する。
     * スペースキー（ホームボタン）が押された時に呼び出される。
     * 現在の画面がホーム画面でない場合、スタックをクリアしてホーム画面に戻る。
     * アプリケーション画面はバックグラウンドに送られ、次回起動時に即座に復帰できる。
     */
    private void navigateToHome() {
        Screen currentScreen = getCurrentScreen();

        // 現在の画面がホーム画面（ランチャー関連）の場合は何もしない
        if (currentScreen != null && isLauncherScreen(currentScreen)) {
            log("Already on launcher screen (" + currentScreen.getClass().getSimpleName() + ") - no action needed");
            return;
        }

        log("Navigating to home screen from " +
                         (currentScreen != null ? currentScreen.getClass().getSimpleName() : "null"));

        // アプリケーション画面からホーム画面に戻る
        // スタックをクリアしてホーム画面のみにする
        while (!screenStack.isEmpty() && !isLauncherScreen(getCurrentScreen())) {
            Screen poppedScreen = screenStack.pop();
            log("Popped screen during home navigation - " + poppedScreen.getClass().getSimpleName());

            // ★重要★ スクリーンをバックグラウンドに送る（OS側の強制制御）
            // これにより、WebViewのレンダリングパイプラインが停止し、GPU使用率が削減される
            poppedScreen.onBackground();
            log("Screen moved to background during home navigation: " + poppedScreen.getScreenTitle());

            // APPLICATIONレイヤーを削除
            if (kernel != null) {
                kernel.removeLayer(LayerType.APPLICATION);
                log("Removed APPLICATION layer during home navigation");
            }
        }

        // ホーム画面が見つからない場合やスタックが空の場合は、新しいホーム画面を作成
        if (screenStack.isEmpty() || !isLauncherScreen(getCurrentScreen())) {
            log("No home screen found - this should not happen in normal operation");
            // 通常の動作では、初期化時にホーム画面がプッシュされているはずなので、
            // このケースは異常状態として扱う
        }

        // ホーム画面をフォアグラウンドに復帰
        Screen homeScreen = getCurrentScreen();
        if (homeScreen != null) {
            homeScreen.onForeground();
            log("Home screen moved to foreground: " + homeScreen.getScreenTitle());
        }

        log("Home navigation completed - current screen: " +
                         (getCurrentScreen() != null ? getCurrentScreen().getClass().getSimpleName() : "null"));
    }

    /**
     * 修飾キー（Shift/Ctrl）の状態を設定する。
     * Kernelから呼び出され、テキスト入力コンポーネントに伝播される。
     *
     * @param shift Shiftキーが押されているかどうか
     * @param ctrl Ctrlキーが押されているかどうか
     */
    public void setModifierKeys(boolean shift, boolean ctrl) {
        this.shiftPressed = shift;
        this.ctrlPressed = ctrl;

        // 現在のスクリーンに修飾キーの状態を伝播
        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null) {
            currentScreen.setModifierKeys(shift, ctrl);
        }
    }

    /**
     * 現在のスクリーンにフォーカスされたテキスト入力コンポーネントがあるかチェック。
     * スペースキー処理の前にKernelから呼び出される。
     *
     * @return フォーカスされたコンポーネントがある場合true
     */
    public boolean hasFocusedComponent() {
        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null) {
            return currentScreen.hasFocusedComponent();
        }
        return false;
    }
}