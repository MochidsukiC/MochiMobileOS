package jp.moyashi.phoneos.forge.bridge;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ipc.KernelProxy;
import processing.core.PGraphics;

/**
 * ローカルKernelラッパー。
 * 実際のKernelインスタンスをKernelProxyインターフェースでラップする。
 * ProcessingScreenがローカルモードとリモートモードを透過的に使用可能にする。
 *
 * @author jp.moyashi
 * @version 1.0
 */
public class KernelLocalWrapper implements KernelProxy {

    /** 実際のKernelインスタンス */
    private final Kernel kernel;

    /**
     * KernelLocalWrapperを作成する。
     *
     * @param kernel ラップするKernelインスタンス
     */
    public KernelLocalWrapper(Kernel kernel) {
        this.kernel = kernel;
    }

    /**
     * ラップしているKernelインスタンスを取得する。
     *
     * @return Kernelインスタンス
     */
    public Kernel getKernel() {
        return kernel;
    }

    // === KernelProxyインターフェース実装 ===

    @Override
    public int getFrameCount() {
        return kernel.frameCount;
    }

    @Override
    public int getWidth() {
        return kernel.width;
    }

    @Override
    public int getHeight() {
        return kernel.height;
    }

    @Override
    public void update() {
        kernel.update();
    }

    @Override
    public void render() {
        kernel.render();
    }

    @Override
    public int[] getPixels() {
        return kernel.getPixels();
    }

    @Override
    public PGraphics getGraphics() {
        return kernel.getGraphics();
    }

    @Override
    public void sleep() {
        kernel.sleep();
    }

    @Override
    public void wake() {
        kernel.wake();
    }

    @Override
    public boolean isSleeping() {
        return kernel.isSleeping();
    }

    @Override
    public void mousePressed(int x, int y) {
        kernel.mousePressed(x, y);
    }

    @Override
    public void mouseReleased(int x, int y) {
        kernel.mouseReleased(x, y);
    }

    @Override
    public void mouseDragged(int x, int y) {
        kernel.mouseDragged(x, y);
    }

    @Override
    public void mouseWheel(int x, int y, float delta) {
        kernel.mouseWheel(x, y, delta);
    }

    @Override
    public void keyPressed(char key, int keyCode) {
        kernel.keyPressed(key, keyCode);
    }

    @Override
    public void keyReleased(char key, int keyCode) {
        kernel.keyReleased(key, keyCode);
    }

    @Override
    public void requestGoHome() {
        kernel.requestGoHome();
    }

    @Override
    public Object getScreenManager() {
        return kernel.getScreenManager();
    }
}
