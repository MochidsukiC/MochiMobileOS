package jp.moyashi.phoneos.core.service.chromium;

import java.lang.reflect.Method;

/**
 * JVMモジュールシステムを実行時に操作するユーティリティ
 *
 * このクラスは、JVMフラグ（--add-opens, --add-exports）を指定せずに、
 * 実行時にJavaモジュールを強制的に開くことを試みます。
 *
 * 技術的背景:
 * - JCEF/JOGLは内部API（sun.awt, sun.java2d）へのアクセスが必要
 * - 通常はJVM起動時フラグで許可が必要
 * - Java 9-16では実行時にModule.implAddOpens()でモジュールを開ける
 * - Java 17+では制限が強化され、失敗する可能性あり
 *
 * フォールバック戦略:
 * 1. この方法を試行
 * 2. 失敗した場合、JVMフラグの有無を確認
 * 3. フラグがない場合、エラーメッセージを表示
 */
public class ModuleOpener {

    private static boolean attempted = false;
    private static boolean succeeded = false;
    private static String errorMessage = null;

    /**
     * 必要なモジュールを実行時に開く
     *
     * @return 成功した場合true、失敗した場合false
     */
    public static boolean openRequiredModules() {
        if (attempted) {
            return succeeded;
        }

        attempted = true;

        try {
            // 必要なモジュールとパッケージの定義
            String[][] modulesToOpen = {
                {"java.base", "java.lang"},
                {"java.desktop", "sun.awt"},
                {"java.desktop", "sun.java2d"}
            };

            // 対象モジュールを決定（名前付きモジュールか無名モジュールか）
            // Forge環境では名前付きモジュール "mochimobileos" として動作
            // スタンドアロンJARでは無名モジュールとして動作
            Module targetModule = ModuleOpener.class.getModule();
            boolean isNamedModule = targetModule.isNamed();
            Module openToModule = isNamedModule ? targetModule : ModuleOpener.class.getClassLoader().getUnnamedModule();

            // Module.implAddOpens()メソッドを取得
            // これは内部APIだが、これ自体にアクセスするためにリフレクションを使う
            Method implAddOpens;
            try {
                implAddOpens = Module.class.getDeclaredMethod("implAddOpens", String.class, Module.class);
                implAddOpens.setAccessible(true);
            } catch (NoSuchMethodException e) {
                // Java 9より前、またはメソッドが削除された場合
                errorMessage = "Module.implAddOpens()メソッドが見つかりません（Java " +
                              System.getProperty("java.version") + "）";
                System.err.println("[ModuleOpener] " + errorMessage);
                return false;
            } catch (Exception e) {
                errorMessage = "Module.implAddOpens()へのアクセスに失敗: " + e.getMessage();
                System.err.println("[ModuleOpener] " + errorMessage);
                return false;
            }

            // 各モジュール/パッケージを開く
            for (String[] moduleAndPackage : modulesToOpen) {
                String moduleName = moduleAndPackage[0];
                String packageName = moduleAndPackage[1];

                try {
                    // モジュールを取得
                    Module module = ModuleLayer.boot()
                        .findModule(moduleName)
                        .orElseThrow(() -> new IllegalStateException("モジュールが見つかりません: " + moduleName));

                    // パッケージを開く
                    implAddOpens.invoke(module, packageName, openToModule);

                } catch (Exception e) {
                    errorMessage = moduleName + "/" + packageName + " を開けませんでした: " + e.getMessage();
                    return false;
                }
            }

            succeeded = true;
            return true;

        } catch (Exception e) {
            errorMessage = "予期しないエラー: " + e.getMessage();
            return false;
        }
    }

    /**
     * モジュール操作が成功したかどうか
     * @return 成功した場合true
     */
    public static boolean isSucceeded() {
        return succeeded;
    }

    /**
     * エラーメッセージを取得
     * @return エラーメッセージ、またはnull
     */
    public static String getErrorMessage() {
        return errorMessage;
    }

    /**
     * JVMフラグが正しく設定されているかチェック
     * （モジュール操作が失敗した場合のフォールバック確認）
     *
     * @return フラグが設定されている場合true
     */
    public static boolean checkJvmFlags() {
        try {
            // 実際にJOGLがアクセスするクラス（sun.awt.* / sun.java2d.*）にアクセスできるかテスト
            // これが成功すれば、JVMフラグが正しく設定されている

            // sun.awt.SunToolkitへのアクセスを試みる（JOGLが使用するクラス）
            Class<?> sunToolkitClass = Class.forName("sun.awt.SunToolkit");

            // sun.java2d.SunGraphicsへのアクセスを試みる（JOGLが使用するクラス）
            Class<?> sunGraphicsClass = Class.forName("sun.java2d.SunGraphics2D");

            // どちらも読み込めれば、JVMフラグは正しく設定されている
            return sunToolkitClass != null && sunGraphicsClass != null;

        } catch (ClassNotFoundException e) {
            // クラスが見つからない = JVMフラグが設定されていない
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
