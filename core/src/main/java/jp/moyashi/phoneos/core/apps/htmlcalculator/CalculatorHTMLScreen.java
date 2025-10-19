package jp.moyashi.phoneos.core.apps.htmlcalculator;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.HTMLScreen;
import processing.core.PGraphics;

/**
 * HTML/CSS/JavaScript で実装された電卓画面。
 * HTMLScreenを継承してWebViewベースのUIを提供する。
 *
 * CSSアニメーション（スピナー、パルス）とJavaScript FPSカウンターがあるため、
 * 継続的なフレーム更新が必要。
 */
public class CalculatorHTMLScreen extends HTMLScreen {
    private int frameCounter = 0;

    public CalculatorHTMLScreen(Kernel kernel) {
        super(kernel);
    }

    @Override
    public void tick() {
        // スリープ中またはバックグラウンド状態の場合は、WebViewの更新を停止してCPU使用率を削減
        if (kernel.isSleeping() || isInBackground) {
            return;
        }

        // CSSアニメーションとJavaScript FPSカウンターのため、
        // 2フレームごとにWebViewの更新を要求
        frameCounter++;
        if (frameCounter % 2 == 0 && webViewWrapper != null && !webViewWrapper.isDisposed()) {
            webViewWrapper.requestUpdate();
        }
    }

    @Override
    public void draw(PGraphics pg) {
        // 親クラスの描画処理を実行
        super.draw(pg);
    }

    @Override
    protected String getHTMLContent() {
        // 電卓のHTML/CSS/JSを文字列として返す
        return "<!DOCTYPE html>\n" +
                "<html lang=\"ja\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>HTML電卓</title>\n" +
                "    <style>\n" +
                "        * {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            box-sizing: border-box;\n" +
                "        }\n" +
                "\n" +
                "        body {\n" +
                "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
                "            display: flex;\n" +
                "            justify-content: center;\n" +
                "            align-items: center;\n" +
                "            min-height: 100vh;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "\n" +
                "        .calculator {\n" +
                "            background: white;\n" +
                "            border-radius: 20px;\n" +
                "            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);\n" +
                "            padding: 20px;\n" +
                "            width: 100%;\n" +
                "            max-width: 360px;\n" +
                "        }\n" +
                "\n" +
                "        .display {\n" +
                "            background: #2d3561;\n" +
                "            color: white;\n" +
                "            font-size: 2.5em;\n" +
                "            padding: 20px;\n" +
                "            border-radius: 10px;\n" +
                "            text-align: right;\n" +
                "            margin-bottom: 20px;\n" +
                "            min-height: 80px;\n" +
                "            word-wrap: break-word;\n" +
                "            display: flex;\n" +
                "            align-items: center;\n" +
                "            justify-content: flex-end;\n" +
                "        }\n" +
                "\n" +
                "        .buttons {\n" +
                "            display: grid;\n" +
                "            grid-template-columns: repeat(4, 1fr);\n" +
                "            gap: 10px;\n" +
                "        }\n" +
                "\n" +
                "        button {\n" +
                "            background: #f0f0f0;\n" +
                "            border: none;\n" +
                "            border-radius: 10px;\n" +
                "            font-size: 1.5em;\n" +
                "            padding: 20px;\n" +
                "            cursor: pointer;\n" +
                "            transition: all 0.2s;\n" +
                "            font-weight: 500;\n" +
                "        }\n" +
                "\n" +
                "        button:hover {\n" +
                "            background: #e0e0e0;\n" +
                "            transform: scale(1.05);\n" +
                "        }\n" +
                "\n" +
                "        button:active {\n" +
                "            transform: scale(0.95);\n" +
                "        }\n" +
                "\n" +
                "        .operator {\n" +
                "            background: #667eea;\n" +
                "            color: white;\n" +
                "        }\n" +
                "\n" +
                "        .operator:hover {\n" +
                "            background: #5568d3;\n" +
                "        }\n" +
                "\n" +
                "        .equals {\n" +
                "            background: #764ba2;\n" +
                "            color: white;\n" +
                "            grid-column: span 2;\n" +
                "        }\n" +
                "\n" +
                "        .equals:hover {\n" +
                "            background: #643a8c;\n" +
                "        }\n" +
                "\n" +
                "        .clear {\n" +
                "            background: #ff6b6b;\n" +
                "            color: white;\n" +
                "        }\n" +
                "\n" +
                "        .clear:hover {\n" +
                "            background: #ee5a52;\n" +
                "        }\n" +
                "\n" +
                "        .zero {\n" +
                "            grid-column: span 2;\n" +
                "        }\n" +
                "\n" +
                "        h1 {\n" +
                "            text-align: center;\n" +
                "            margin-bottom: 20px;\n" +
                "            color: #2d3561;\n" +
                "            font-size: 1.5em;\n" +
                "        }\n" +
                "\n" +
                "        .fps-counter {\n" +
                "            position: fixed;\n" +
                "            top: 10px;\n" +
                "            right: 10px;\n" +
                "            background: rgba(0, 0, 0, 0.8);\n" +
                "            color: #00ff00;\n" +
                "            padding: 10px 15px;\n" +
                "            border-radius: 8px;\n" +
                "            font-family: 'Courier New', monospace;\n" +
                "            font-size: 0.9em;\n" +
                "            z-index: 1000;\n" +
                "            min-width: 120px;\n" +
                "        }\n" +
                "\n" +
                "        .spinner {\n" +
                "            width: 30px;\n" +
                "            height: 30px;\n" +
                "            border: 3px solid rgba(102, 126, 234, 0.3);\n" +
                "            border-top: 3px solid #667eea;\n" +
                "            border-radius: 50%;\n" +
                "            animation: spin 1s linear infinite;\n" +
                "            margin: 0 auto 10px;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes spin {\n" +
                "            0% { transform: rotate(0deg); }\n" +
                "            100% { transform: rotate(360deg); }\n" +
                "        }\n" +
                "\n" +
                "        .pulse {\n" +
                "            animation: pulse 2s ease-in-out infinite;\n" +
                "        }\n" +
                "\n" +
                "        @keyframes pulse {\n" +
                "            0%, 100% { transform: scale(1); opacity: 1; }\n" +
                "            50% { transform: scale(1.05); opacity: 0.8; }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"fps-counter\" id=\"fps-counter\">\n" +
                "        FPS: <span id=\"fps\">--</span><br>\n" +
                "        Frame: <span id=\"frame-time\">--</span>ms\n" +
                "    </div>\n" +
                "    <div class=\"calculator\">\n" +
                "        <div class=\"spinner\"></div>\n" +
                "        <h1 class=\"pulse\">📱 HTML電卓</h1>\n" +
                "        <div class=\"display\" id=\"display\">0</div>\n" +
                "        <div class=\"buttons\">\n" +
                "            <button class=\"clear\" onclick=\"clearDisplay()\">C</button>\n" +
                "            <button class=\"operator\" onclick=\"appendOperator('/')\">÷</button>\n" +
                "            <button class=\"operator\" onclick=\"appendOperator('*')\">×</button>\n" +
                "            \n" +
                "            <button onclick=\"appendNumber('7')\">7</button>\n" +
                "            <button onclick=\"appendNumber('8')\">8</button>\n" +
                "            <button onclick=\"appendNumber('9')\">9</button>\n" +
                "            <button class=\"operator\" onclick=\"appendOperator('-')\">-</button>\n" +
                "            \n" +
                "            <button onclick=\"appendNumber('4')\">4</button>\n" +
                "            <button onclick=\"appendNumber('5')\">5</button>\n" +
                "            <button onclick=\"appendNumber('6')\">6</button>\n" +
                "            <button class=\"operator\" onclick=\"appendOperator('+')\">+</button>\n" +
                "            \n" +
                "            <button onclick=\"appendNumber('1')\">1</button>\n" +
                "            <button onclick=\"appendNumber('2')\">2</button>\n" +
                "            <button onclick=\"appendNumber('3')\">3</button>\n" +
                "            <button class=\"operator\" onclick=\"appendDecimal()\">.</button>\n" +
                "            \n" +
                "            <button class=\"zero\" onclick=\"appendNumber('0')\">0</button>\n" +
                "            <button class=\"equals\" onclick=\"calculate()\">=</button>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "\n" +
                "    <script>\n" +
                "        let currentInput = '0';\n" +
                "        let previousInput = '';\n" +
                "        let operator = '';\n" +
                "        let shouldResetDisplay = false;\n" +
                "\n" +
                "        const display = document.getElementById('display');\n" +
                "\n" +
                "        function updateDisplay() {\n" +
                "            display.textContent = currentInput;\n" +
                "            console.log('[Calculator] Display updated:', currentInput);\n" +
                "        }\n" +
                "\n" +
                "        function appendNumber(num) {\n" +
                "            if (shouldResetDisplay || currentInput === '0') {\n" +
                "                currentInput = num;\n" +
                "                shouldResetDisplay = false;\n" +
                "            } else {\n" +
                "                currentInput += num;\n" +
                "            }\n" +
                "            updateDisplay();\n" +
                "        }\n" +
                "\n" +
                "        function appendDecimal() {\n" +
                "            if (shouldResetDisplay) {\n" +
                "                currentInput = '0.';\n" +
                "                shouldResetDisplay = false;\n" +
                "            } else if (!currentInput.includes('.')) {\n" +
                "                currentInput += '.';\n" +
                "            }\n" +
                "            updateDisplay();\n" +
                "        }\n" +
                "\n" +
                "        function appendOperator(op) {\n" +
                "            if (operator !== '' && !shouldResetDisplay) {\n" +
                "                calculate();\n" +
                "            }\n" +
                "            previousInput = currentInput;\n" +
                "            operator = op;\n" +
                "            shouldResetDisplay = true;\n" +
                "            console.log('[Calculator] Operator set:', op);\n" +
                "        }\n" +
                "\n" +
                "        function calculate() {\n" +
                "            if (operator === '' || shouldResetDisplay) {\n" +
                "                return;\n" +
                "            }\n" +
                "\n" +
                "            const prev = parseFloat(previousInput);\n" +
                "            const current = parseFloat(currentInput);\n" +
                "            let result = 0;\n" +
                "\n" +
                "            console.log('[Calculator] Calculating:', prev, operator, current);\n" +
                "\n" +
                "            switch (operator) {\n" +
                "                case '+':\n" +
                "                    result = prev + current;\n" +
                "                    break;\n" +
                "                case '-':\n" +
                "                    result = prev - current;\n" +
                "                    break;\n" +
                "                case '*':\n" +
                "                    result = prev * current;\n" +
                "                    break;\n" +
                "                case '/':\n" +
                "                    if (current === 0) {\n" +
                "                        currentInput = 'エラー';\n" +
                "                        updateDisplay();\n" +
                "                        setTimeout(() => {\n" +
                "                            clearDisplay();\n" +
                "                        }, 1500);\n" +
                "                        return;\n" +
                "                    }\n" +
                "                    result = prev / current;\n" +
                "                    break;\n" +
                "            }\n" +
                "\n" +
                "            currentInput = String(Math.round(result * 100000000) / 100000000);\n" +
                "            operator = '';\n" +
                "            shouldResetDisplay = true;\n" +
                "            updateDisplay();\n" +
                "            console.log('[Calculator] Result:', result);\n" +
                "        }\n" +
                "\n" +
                "        function clearDisplay() {\n" +
                "            currentInput = '0';\n" +
                "            previousInput = '';\n" +
                "            operator = '';\n" +
                "            shouldResetDisplay = false;\n" +
                "            updateDisplay();\n" +
                "            console.log('[Calculator] Display cleared');\n" +
                "        }\n" +
                "\n" +
                "        // 初期化ログ\n" +
                "        console.log('[Calculator] HTML Calculator initialized');\n" +
                "        console.log('[Calculator] MochiOS API available:', typeof MochiOS !== 'undefined');\n" +
                "\n" +
                "        // FPSカウンター\n" +
                "        let frameCount = 0;\n" +
                "        let lastTime = performance.now();\n" +
                "        let lastFrameTime = performance.now();\n" +
                "        const fpsDisplay = document.getElementById('fps');\n" +
                "        const frameTimeDisplay = document.getElementById('frame-time');\n" +
                "\n" +
                "        function updateFPS() {\n" +
                "            const now = performance.now();\n" +
                "            const deltaTime = now - lastFrameTime;\n" +
                "            lastFrameTime = now;\n" +
                "\n" +
                "            frameCount++;\n" +
                "\n" +
                "            // 1秒ごとにFPSを更新\n" +
                "            if (now - lastTime >= 1000) {\n" +
                "                const fps = Math.round((frameCount * 1000) / (now - lastTime));\n" +
                "                fpsDisplay.textContent = fps;\n" +
                "                \n" +
                "                // FPSに応じて色を変更\n" +
                "                if (fps >= 55) {\n" +
                "                    fpsDisplay.style.color = '#00ff00'; // 緑（良好）\n" +
                "                } else if (fps >= 30) {\n" +
                "                    fpsDisplay.style.color = '#ffff00'; // 黄（普通）\n" +
                "                } else {\n" +
                "                    fpsDisplay.style.color = '#ff0000'; // 赤（低い）\n" +
                "                }\n" +
                "\n" +
                "                frameCount = 0;\n" +
                "                lastTime = now;\n" +
                "            }\n" +
                "\n" +
                "            // フレーム時間を更新\n" +
                "            frameTimeDisplay.textContent = deltaTime.toFixed(1);\n" +
                "\n" +
                "            requestAnimationFrame(updateFPS);\n" +
                "        }\n" +
                "\n" +
                "        // FPSカウンター開始\n" +
                "        requestAnimationFrame(updateFPS);\n" +
                "        console.log('[Calculator] FPS counter started');\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }

    @Override
    public String getScreenTitle() {
        return "HTML電卓";
    }
}
