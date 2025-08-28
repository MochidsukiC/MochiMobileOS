package jp.moyashi.phoneos.core.ui;

import jp.moyashi.phoneos.core.apps.launcher.ui.SafeHomeScreen;
import jp.moyashi.phoneos.core.apps.launcher.ui.AppLibraryScreen;
import jp.moyashi.phoneos.core.apps.launcher.ui.HomeScreen;
import processing.core.PApplet;
import java.util.Stack;

/**
 * スマートフォンOSでのスクリーン遷移とナビゲーションを扱うスクリーンマネージャー。
 * このクラスはスクリーンのスタックを管理し、モバイルアプリナビゲーションと同様の
 * push/pop操作で階層的なナビゲーションを可能にする。
 * 
 * @author YourName
 * @version 1.0
 */
public class ScreenManager {
    
    /** ナビゲーション管理用のスクリーンスタック */
    private Stack<Screen> screenStack;
    
    /**
     * 新しいScreenManagerインスタンスを構築する。
     * ナビゲーション管理用のスクリーンスタックを初期化する。
     */
    public ScreenManager() {
        screenStack = new Stack<>();
        System.out.println("ScreenManager: Screen manager initialized");
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
            screen.setup();
            System.out.println("ScreenManager: Pushed screen - " + screen.getScreenTitle());
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
            poppedScreen.cleanup();
            System.out.println("ScreenManager: Popped screen - " + poppedScreen.getScreenTitle());
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
     * 現在アクティブなスクリーンを描画する。
     * 描画を現在のスクリーンのdraw()メソッドに委託する。
     * 
     * @param p 描画操作用のPAppletインスタンス
     */
    public void draw(PApplet p) {
        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null) {
            currentScreen.draw(p);
        } else {
            // Draw a default screen if no screens are active
            drawEmptyScreen(p);
        }
    }
    
    /**
     * 現在のスクリーンに委託してマウスプレスイベントを処理する。
     * 
     * @param mouseX マウスプレスのx座標
     * @param mouseY マウスプレスのy座標
     */
    public void mousePressed(int mouseX, int mouseY) {
        Screen currentScreen = getCurrentScreen();
        if (currentScreen != null) {
            currentScreen.mousePressed(mouseX, mouseY);
        }
    }
    
    /**
     * 現在のスクリーンに委託してマウスドラッグイベントを処理する。
     * 
     * @param mouseX マウス位置のx座標
     * @param mouseY マウス位置のy座標
     */
    public void mouseDragged(int mouseX, int mouseY) {
        Screen currentScreen = getCurrentScreen();
        System.out.println("ScreenManager: mouseDragged called at (" + mouseX + ", " + mouseY + ")");
        System.out.println("ScreenManager: Current screen: " + (currentScreen != null ? currentScreen.getScreenTitle() : "null"));
        
        if (currentScreen != null) {
            // SafeHomeScreenのサポート
            if (currentScreen instanceof SafeHomeScreen) {
                System.out.println("ScreenManager: Routing mouseDragged to SafeHomeScreen");
                ((SafeHomeScreen) currentScreen).mouseDragged(mouseX, mouseY);
            }
            // AppLibraryScreenのサポート
            else if (currentScreen instanceof AppLibraryScreen) {
                System.out.println("ScreenManager: Routing mouseDragged to AppLibraryScreen");
                ((AppLibraryScreen) currentScreen).mouseDragged(mouseX, mouseY);
            }
            // HomeScreenのサポート
            else if (currentScreen instanceof HomeScreen) {
                System.out.println("ScreenManager: Routing mouseDragged to HomeScreen");
                ((HomeScreen) currentScreen).mouseDragged(mouseX, mouseY);
            }
            else {
                System.out.println("ScreenManager: ⚠️ Unknown screen type for mouseDragged: " + currentScreen.getClass().getSimpleName());
            }
        } else {
            System.out.println("ScreenManager: ⚠️ No current screen to route mouseDragged to");
        }
    }
    
    /**
     * 現在のスクリーンに委託してマウスリリースイベントを処理する。
     * 
     * @param mouseX マウス位置のx座標
     * @param mouseY マウス位置のy座標
     */
    public void mouseReleased(int mouseX, int mouseY) {
        Screen currentScreen = getCurrentScreen();
        System.out.println("ScreenManager: mouseReleased called at (" + mouseX + ", " + mouseY + ")");
        System.out.println("ScreenManager: Current screen: " + (currentScreen != null ? currentScreen.getScreenTitle() : "null"));
        
        if (currentScreen != null) {
            // SafeHomeScreenのサポート
            if (currentScreen instanceof SafeHomeScreen) {
                System.out.println("ScreenManager: Routing to SafeHomeScreen");
                ((SafeHomeScreen) currentScreen).mouseReleased(mouseX, mouseY);
            }
            // AppLibraryScreenのサポート
            else if (currentScreen instanceof AppLibraryScreen) {
                System.out.println("ScreenManager: Routing to AppLibraryScreen");
                ((AppLibraryScreen) currentScreen).mouseReleased(mouseX, mouseY);
            }
            // HomeScreenのサポート
            else if (currentScreen instanceof HomeScreen) {
                System.out.println("ScreenManager: Routing to HomeScreen");
                ((HomeScreen) currentScreen).mouseReleased(mouseX, mouseY);
            }
            else {
                System.out.println("ScreenManager: ⚠️ Unknown screen type: " + currentScreen.getClass().getSimpleName());
            }
        } else {
            System.out.println("ScreenManager: ⚠️ No current screen to route mouseReleased to");
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
}