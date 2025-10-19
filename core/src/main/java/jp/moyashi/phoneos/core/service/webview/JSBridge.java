package jp.moyashi.phoneos.core.service.webview;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.service.hardware.*;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * JavaScript ↔ Java 通信ブリッジ。
 * WebView内のJavaScriptからKernel APIを呼び出せるようにする。
 *
 * グローバルAPI（JavaScript側）:
 * <pre>
 * // 通知表示
 * MochiOS.showNotification("タイトル", "メッセージ");
 *
 * // VFSからファイル読み込み
 * var content = MochiOS.vfs.readString("data.json");
 *
 * // アプリを開く
 * MochiOS.openApp("jp.moyashi.phoneos.core.apps.calculator");
 *
 * // ハードウェアAPI
 * var battery = MochiOS.hardware.getBatteryLevel();
 * var location = MochiOS.hardware.getLocation();
 *
 * // ログ出力
 * MochiOS.log("Hello from JavaScript!");
 * </pre>
 */
public class JSBridge {
    private final Kernel kernel;
    private final Map<String, Function<Object[], Object>> customHandlers = new HashMap<>();

    /**
     * JSBridgeを作成する。
     *
     * @param kernel Kernelインスタンス
     */
    public JSBridge(Kernel kernel) {
        this.kernel = kernel;
        System.out.println("JSBridge: Initialized with Kernel reference");
    }

    /**
     * カスタムJavaScriptハンドラを追加する。
     * 外部アプリ開発者が独自のJSAPIを追加する場合に使用。
     *
     * @param name ハンドラ名（JavaScript側の関数名）
     * @param handler ハンドラ関数
     */
    public void addHandler(String name, Function<Object[], Object> handler) {
        customHandlers.put(name, handler);
        System.out.println("JSBridge: Custom handler added: " + name);
    }

    /**
     * WebViewにMochiOS APIを注入する。
     *
     * @param wrapper WebViewWrapper インスタンス
     */
    public void injectIntoWebView(WebViewWrapper wrapper) {
        if (wrapper == null || wrapper.isDisposed()) {
            System.err.println("JSBridge: Cannot inject into disposed WebView");
            return;
        }

        System.out.println("JSBridge: Injecting MochiOS API into WebView...");

        // MochiOS グローバルオブジェクトを作成
        String script = buildMochiOSAPI();

        wrapper.executeScript(script);

        System.out.println("JSBridge: MochiOS API injection complete");
    }

    /**
     * MochiOS API のJavaScriptコードを構築する。
     *
     * @return JavaScriptコード
     */
    private String buildMochiOSAPI() {
        StringBuilder sb = new StringBuilder();

        sb.append("(function() {\n");
        sb.append("  if (window.MochiOS) return; // 既に注入済み\n\n");

        sb.append("  window.MochiOS = {\n");

        // ログAPI
        sb.append("    log: function(message) {\n");
        sb.append("      console.log('[MochiOS] ' + message);\n");
        sb.append("    },\n\n");

        // 通知API
        sb.append("    showNotification: function(title, message) {\n");
        sb.append("      console.log('[MochiOS] showNotification:', title, message);\n");
        sb.append("      // Java側で実装（将来）\n");
        sb.append("    },\n\n");

        // VFS API（簡易版）
        sb.append("    vfs: {\n");
        sb.append("      readString: function(path) {\n");
        sb.append("        console.log('[MochiOS] VFS readString:', path);\n");
        sb.append("        return ''; // Java側で実装（将来）\n");
        sb.append("      },\n");
        sb.append("      writeString: function(path, content) {\n");
        sb.append("        console.log('[MochiOS] VFS writeString:', path);\n");
        sb.append("        // Java側で実装（将来）\n");
        sb.append("      }\n");
        sb.append("    },\n\n");

        // アプリ起動API
        sb.append("    openApp: function(appId) {\n");
        sb.append("      console.log('[MochiOS] openApp:', appId);\n");
        sb.append("      // Java側で実装（将来）\n");
        sb.append("    },\n\n");

        // ハードウェアAPI
        sb.append("    hardware: {\n");
        sb.append("      getBatteryLevel: function() {\n");
        sb.append("        console.log('[MochiOS] getBatteryLevel');\n");
        sb.append("        return 100; // Java側で実装（将来）\n");
        sb.append("      },\n");
        sb.append("      getLocation: function() {\n");
        sb.append("        console.log('[MochiOS] getLocation');\n");
        sb.append("        return { x: 0, y: 0, z: 0 }; // Java側で実装（将来）\n");
        sb.append("      }\n");
        sb.append("    },\n\n");

        // バージョン情報
        sb.append("    version: '1.0.0',\n");
        sb.append("    platform: 'MochiMobileOS'\n");

        sb.append("  };\n\n");

        sb.append("  console.log('[MochiOS] API initialized');\n");
        sb.append("})();\n");

        return sb.toString();
    }

    /**
     * JavaScript側からのAPI呼び出しを処理する。
     * （将来の実装用プレースホルダー）
     *
     * @param apiName API名
     * @param args 引数
     * @return 戻り値
     */
    public Object handleAPICall(String apiName, Object[] args) {
        System.out.println("JSBridge: API call received: " + apiName);

        // カスタムハンドラをチェック
        if (customHandlers.containsKey(apiName)) {
            return customHandlers.get(apiName).apply(args);
        }

        // 組み込みAPIの処理
        switch (apiName) {
            case "showNotification":
                return handleShowNotification(args);

            case "vfs.readString":
                return handleVFSReadString(args);

            case "vfs.writeString":
                return handleVFSWriteString(args);

            case "openApp":
                return handleOpenApp(args);

            case "hardware.getBatteryLevel":
                return handleGetBatteryLevel();

            case "hardware.getLocation":
                return handleGetLocation();

            default:
                System.err.println("JSBridge: Unknown API call: " + apiName);
                return null;
        }
    }

    /**
     * 通知表示API
     */
    private Object handleShowNotification(Object[] args) {
        if (args.length < 2) {
            System.err.println("JSBridge: showNotification requires 2 arguments");
            return false;
        }

        String title = String.valueOf(args[0]);
        String message = String.valueOf(args[1]);

        if (kernel.getNotificationManager() != null) {
            // sender="WebApp", title, content=message, priority=1(normal)
            kernel.getNotificationManager().addNotification("WebApp", title, message, 1);
            System.out.println("JSBridge: Notification shown: " + title);
            return true;
        }

        return false;
    }

    /**
     * VFS読み込みAPI
     */
    private Object handleVFSReadString(Object[] args) {
        if (args.length < 1) {
            System.err.println("JSBridge: vfs.readString requires 1 argument");
            return "";
        }

        String path = String.valueOf(args[0]);

        if (kernel.getVFS() != null) {
            String content = kernel.getVFS().readFile(path);
            System.out.println("JSBridge: VFS read: " + path);
            return content != null ? content : "";
        }

        return "";
    }

    /**
     * VFS書き込みAPI
     */
    private Object handleVFSWriteString(Object[] args) {
        if (args.length < 2) {
            System.err.println("JSBridge: vfs.writeString requires 2 arguments");
            return false;
        }

        String path = String.valueOf(args[0]);
        String content = String.valueOf(args[1]);

        if (kernel.getVFS() != null) {
            boolean success = kernel.getVFS().writeFile(path, content);
            System.out.println("JSBridge: VFS written: " + path);
            return success;
        }

        return false;
    }

    /**
     * アプリ起動API
     */
    private Object handleOpenApp(Object[] args) {
        if (args.length < 1) {
            System.err.println("JSBridge: openApp requires 1 argument");
            return false;
        }

        String appId = String.valueOf(args[0]);

        if (kernel.getAppLoader() != null && kernel.getScreenManager() != null) {
            IApplication app = kernel.getAppLoader().findApplicationById(appId);
            if (app != null) {
                kernel.getScreenManager().pushScreen(app.getEntryScreen(kernel));
                System.out.println("JSBridge: App opened: " + appId);
                return true;
            }
        }

        return false;
    }

    /**
     * バッテリーレベル取得API
     */
    private Object handleGetBatteryLevel() {
        if (kernel.getBatteryInfo() != null) {
            int level = kernel.getBatteryInfo().getBatteryLevel();
            System.out.println("JSBridge: Battery level: " + level);
            return level;
        }
        return 100;
    }

    /**
     * 位置情報取得API
     */
    private Object handleGetLocation() {
        if (kernel.getLocationSocket() != null) {
            LocationSocket locationSocket = kernel.getLocationSocket();
            LocationSocket.LocationData data = locationSocket.getLocation();
            Map<String, Object> result = new HashMap<>();
            result.put("x", data.x);
            result.put("y", data.y);
            result.put("z", data.z);
            result.put("accuracy", data.accuracy);

            System.out.println("JSBridge: Location: " + result);
            return result;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("x", 0);
        result.put("y", 0);
        result.put("z", 0);
        result.put("accuracy", 0);
        return result;
    }
}
