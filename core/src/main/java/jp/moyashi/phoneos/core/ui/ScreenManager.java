package jp.moyashi.phoneos.core.ui;

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

    /** 現在描画中のPGraphicsインスタンス */
    private PGraphics currentGraphics;
    
    /** アニメーション中に新しくプッシュされたスクリーンの描画を抑制するフラグ */
    private Screen animatingScreen;
    
    /** setup()が呼ばれていないスクリーンを追跡するためのリスト */
    private java.util.Set<Screen> unsetupScreens;
    
    /**
     * 新しいScreenManagerインスタンスを構築する。
     * ナビゲーション管理用のスクリーンスタックを初期化する。
     */
    public ScreenManager() {
        screenStack = new Stack<>();
        screenTransition = new ScreenTransition();
        screenTransition.setAnimationCallback(this); // Set callback to handle animation completion
        unsetupScreens = new java.util.HashSet<>();
        System.out.println("ScreenManager: Screen manager initialized with animation support");
    }
    
    /**
     * ナビゲーションスタックに新しいスクリーンをプッシュする。
     * 新しいスクリーンがアクティブスクリーンになり、そのsetup()メソッドが呼び出される。
     * 
     * @param screen スタックにプッシュするスクリーン
     */
    public void pushScreen(Screen screen) {
        if (screen != null) {
            screenStack.push(screen);
            // setup()にPAppletを渡すため、currentPAppletが利用可能な場合のみ呼び出し
            if (currentPApplet != null) {
                screen.setup(currentPApplet);
            } else {
                // PAppletが利用できない場合、後で初期化するためにリストに追加
                unsetupScreens.add(screen);
                System.out.println("ScreenManager: Screen " + screen.getScreenTitle() + " queued for setup (PApplet not available)");
            }
            System.out.println("ScreenManager: Pushed screen - " + screen.getScreenTitle());
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
        System.out.println("ScreenManager: pushScreenWithAnimation called");
        System.out.println("ScreenManager: screen=" + (screen != null ? screen.getScreenTitle() : "null"));
        System.out.println("ScreenManager: currentPApplet=" + (currentPApplet != null ? "available" : "null"));
        System.out.println("ScreenManager: iconImage=" + (iconImage != null ? iconImage.width + "x" + iconImage.height : "null"));
        System.out.println("ScreenManager: iconPosition=(" + iconX + ", " + iconY + "), iconSize=" + iconSize);
        
        if (screen != null && currentPApplet != null) {
            // スクリーンを即座にプッシュするが、アニメーション中は描画をブロック
            screenStack.push(screen);
            screen.setup(currentPApplet);
            unsetupScreens.remove(screen); // セットアップ完了なのでリストから削除
            animatingScreen = screen; // このスクリーンはアニメーション中
            
            // アニメーションを開始
            System.out.println("ScreenManager: Setting target dimensions: " + currentPApplet.width + "x" + currentPApplet.height);
            screenTransition.setTargetDimensions(0, 0, currentPApplet.width, currentPApplet.height);
            
            System.out.println("ScreenManager: Starting zoom-in animation...");
            screenTransition.startZoomIn(iconX, iconY, iconSize, iconImage);
            
            System.out.println("ScreenManager: Animation setup complete for screen - " + screen.getScreenTitle());
        } else {
            System.out.println("ScreenManager: Cannot start animation - missing screen or PApplet");
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
            System.out.println("ScreenManager: Popped screen - " + poppedScreen.getScreenTitle());
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
            
            // アニメーションを開始
            screenTransition.startZoomOut(iconX, iconY, iconSize, iconImage, screenCapture);
            
            System.out.println("ScreenManager: Popped screen with zoom-out animation - " + poppedScreen.getScreenTitle());
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
     * PGraphics対応の描画メソッド。現在アクティブなスクリーンを描画する。
     * アニメーション中の場合は、適切なトランジション効果を適用する。
     *
     * @param g 描画操作用のPGraphicsインスタンス
     */
    public void draw(PGraphics g) {
        draw(g, null);
    }

    /**
     * PGraphics対応の描画メソッド（PApplet参照付き）。
     *
     * @param g 描画操作用のPGraphicsインスタンス
     * @param applet PAppletインスタンス（アニメーション等で必要）
     */
    public void draw(PGraphics g, PApplet applet) {
        // PGraphicsインスタンスを保存
        this.currentGraphics = g;
        // PAppletインスタンスを保存
        this.currentPApplet = applet;
        
        // 初回draw()呼び出し時に、未初期化のスクリーンのsetup()を呼び出す
        ensureCurrentScreenSetup();
        
        // アニメーション更新
        screenTransition.update();
        
        boolean isAnimating = screenTransition.isAnimating();
        if (isAnimating) {
            System.out.println("ScreenManager: draw() - Animation is active: " + screenTransition.getCurrentState() + ", progress: " + screenTransition.getProgress());
        }
        
        // アニメーション中でない場合、通常のスクリーン描画
        if (!isAnimating) {
            Screen currentScreen = getCurrentScreen();
            if (currentScreen != null) {
                currentScreen.draw(g);
            } else {
                // Draw a default screen if no screens are active
                drawEmptyScreen(g);
            }
        } else {
            System.out.println("ScreenManager: Rendering animation - " + screenTransition.getCurrentState());
            // アニメーション中の場合
            switch (screenTransition.getCurrentState()) {
                case ZOOM_IN:
                    // ズームインアニメーション中は背景として前のスクリーンを描画（アニメーション対象スクリーンは除く）
                    if (screenStack.size() > 1 && animatingScreen != null) {
                        // アニメーション対象スクリーンを除いた前のスクリーン
                        Screen backgroundScreen = screenStack.get(screenStack.size() - 2);
                        System.out.println("ScreenManager: Drawing background screen during zoom-in: " + backgroundScreen.getScreenTitle());
                        backgroundScreen.draw(g);
                    } else {
                        System.out.println("ScreenManager: Drawing empty screen during zoom-in");
                        drawEmptyScreen(g);
                    }
                    System.out.println("ScreenManager: Drawing zoom-in animation overlay");
                    if (currentPApplet != null) {
                        screenTransition.draw(currentPApplet);
                    }
                    break;
                    
                case ZOOM_OUT:
                    // ズームアウトアニメーション中は次のスクリーンを背景として描画
                    Screen currentScreen = getCurrentScreen();
                    if (currentScreen != null) {
                        currentScreen.draw(g);
                    } else {
                        drawEmptyScreen(g);
                    }
                    if (currentPApplet != null) {
                        screenTransition.draw(currentPApplet);
                    }
                    break;
                    
                default:
                    // その他のアニメーション
                    if (currentPApplet != null) {
                        screenTransition.draw(currentPApplet);
                    }
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
        System.out.println("ScreenManager: All screens cleared");
    }

    
    /**
     * 現在のスクリーンがまだsetup()されていない場合、遅延setup()を実行する。
     * PAppletが利用可能になった際の初回draw()で呼び出される。
     */
    private void ensureCurrentScreenSetup() {
        if (!unsetupScreens.isEmpty()) {
            // 未初期化スクリーンのセットアップを実行
            java.util.Iterator<Screen> iterator = unsetupScreens.iterator();
            while (iterator.hasNext()) {
                Screen screen = iterator.next();
                try {
                    System.out.println("ScreenManager: Delayed setup for screen - " + screen.getScreenTitle());

                    // PAppletとPGraphicsの両方をサポート
                    boolean setupCalled = false;

                    // PGraphicsが利用可能な場合は、PGraphics版setup()を試す
                    if (currentGraphics != null) {
                        try {
                            // リフレクションでsetup(PGraphics)メソッドを探す
                            java.lang.reflect.Method setupMethod = screen.getClass().getMethod("setup", processing.core.PGraphics.class);
                            setupMethod.invoke(screen, currentGraphics);
                            System.out.println("ScreenManager: Called setup(PGraphics) for " + screen.getScreenTitle());
                            setupCalled = true;
                        } catch (NoSuchMethodException e) {
                            // setup(PGraphics)メソッドが存在しない場合は何もしない
                        } catch (Exception e) {
                            System.err.println("ScreenManager: Error calling setup(PGraphics) for " + screen.getScreenTitle() + ": " + e.getMessage());
                        }
                    }

                    // PAppletが利用可能で、まだsetupが呼ばれていない場合は、PApplet版setup()を試す
                    if (!setupCalled && currentPApplet != null) {
                        screen.setup(currentPApplet);
                        System.out.println("ScreenManager: Called setup(PApplet) for " + screen.getScreenTitle());
                        setupCalled = true;
                    }

                    if (setupCalled) {
                        iterator.remove(); // セットアップ完了後にリストから削除
                    } else {
                        System.out.println("ScreenManager: No setup method available for " + screen.getScreenTitle() + " - keeping in queue");
                    }
                } catch (Exception e) {
                    System.err.println("ScreenManager: Error in delayed setup for " + screen.getScreenTitle() + ": " + e.getMessage());
                    e.printStackTrace();
                    iterator.remove(); // エラーでもリストから削除
                }
            }
        }
    }
    
    /**
     * アクティブなスクリーンがない時にデフォルトの空のスクリーンを描画する。
     *
     * @param g 描画操作用のPGraphicsインスタンス
     */
    private void drawEmptyScreen(PGraphics g) {
        g.background(50); // Dark gray background
        g.fill(255);      // White text
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(16);
        g.text("No active screens", g.width / 2, g.height / 2);
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
        System.out.println("ScreenManager: Animation completed - " + completedState);
        
        switch (completedState) {
            case ZOOM_IN:
                // アニメーション完了でアニメーション対象スクリーンの描画を許可
                if (animatingScreen != null) {
                    System.out.println("ScreenManager: Animation completed - allowing " + animatingScreen.getScreenTitle() + " to be drawn");
                    animatingScreen = null; // アニメーションフラグをクリア
                }
                break;
                
            case ZOOM_OUT:
                System.out.println("ScreenManager: Zoom-out animation completed");
                animatingScreen = null;
                break;
                
            default:
                System.out.println("ScreenManager: Unhandled animation completion - " + completedState);
                animatingScreen = null;
                break;
        }
    }
}