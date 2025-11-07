package jp.moyashi.phoneos.standalone;

import jp.moyashi.phoneos.core.Kernel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * P2DレンダラーのNEWTウィンドウ上でIME入力を可能にする可視JTextFieldオーバーレイ。
 *
 * NEWTウィンドウは標準でIME（Input Method Editor）をサポートしていないため、
 * Swing JTextFieldをオーバーレイしてIME入力を受け付ける。
 *
 * 動作原理:
 * 1. ProcessingウィンドウのAWTコンポーネントに可視JTextFieldを追加
 * 2. テキスト入力が必要な時（hasFocusedComponent() == true）にJTextFieldを表示
 * 3. ユーザーがJTextFieldで入力（IME変換も可能）
 * 4. Enterキーで確定された文字列をKernelに転送
 * 5. Kernelが現在のScreenに文字入力イベントを配信
 *
 * @author MochiOS Team
 * @version 2.0 (修正版: 可視化 + ActionListener)
 */
public class IMEInputLayer {

    private final Kernel kernel;
    private final JTextField imeTextField;
    private boolean isVisible = false;

    /**
     * IME入力レイヤーを作成する。
     *
     * @param kernel Kernelインスタンス
     * @param parentComponent 親コンポーネント（ProcessingのGLWindow）
     */
    public IMEInputLayer(Kernel kernel, Component parentComponent) {
        this.kernel = kernel;

        // 可視のJTextFieldを作成
        imeTextField = new JTextField();
        imeTextField.setBounds(0, 0, 0, 0); // 初期状態では非表示（サイズ0）

        // 可視化設定（白背景、黒文字）
        imeTextField.setOpaque(true);
        imeTextField.setBackground(Color.WHITE);
        imeTextField.setForeground(Color.BLACK);
        imeTextField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        imeTextField.setFont(new Font("Yu Gothic", Font.PLAIN, 16));

        // IME入力を有効化
        imeTextField.enableInputMethods(true);

        // ActionListener: Enterキーで確定された文字列を処理
        imeTextField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleTextSubmit();
            }
        });

        // KeyListener: Escapeキーでキャンセル
        imeTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    // Escapeでキャンセル
                    imeTextField.setText("");
                    hide();
                }
            }
        });

        // フォーカスリスナー: フォーカスを失ったら非表示
        imeTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                // フォーカスを失ったら非表示
                hide();
            }
        });

        // 親コンポーネントに追加
        if (parentComponent instanceof Container) {
            ((Container) parentComponent).add(imeTextField);
            System.out.println("IMEInputLayer: JTextField added to parent component");
        } else {
            System.err.println("IMEInputLayer: Parent component is not a Container, cannot add JTextField");
        }

        System.out.println("IMEInputLayer: Initialized (Visible TextField + ActionListener)");
    }

    /**
     * IME入力レイヤーを表示し、指定位置にフォーカスを設定する。
     *
     * @param x X座標
     * @param y Y座標
     * @param width 幅
     * @param height 高さ
     */
    public void show(int x, int y, int width, int height) {
        if (isVisible) {
            return;
        }

        // JTextFieldの位置とサイズを設定
        imeTextField.setBounds(x, y, width, height);
        imeTextField.setVisible(true);
        imeTextField.setText(""); // テキストをクリア

        // フォーカスを取得（IMEを有効化）
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                imeTextField.requestFocusInWindow();
            }
        });

        isVisible = true;
        System.out.println("IMEInputLayer: Shown at (" + x + ", " + y + ") size " + width + "x" + height);
    }

    /**
     * IME入力レイヤーを非表示にする。
     */
    public void hide() {
        if (!isVisible) {
            return;
        }

        imeTextField.setBounds(0, 0, 0, 0);
        imeTextField.setVisible(false);
        imeTextField.setText("");

        isVisible = false;
        System.out.println("IMEInputLayer: Hidden");
    }

    /**
     * IME入力レイヤーが表示中かを確認する。
     *
     * @return 表示中の場合true
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Enterキーで確定された文字列を処理する。
     */
    private void handleTextSubmit() {
        String text = imeTextField.getText();

        if (text == null || text.isEmpty()) {
            // 空文字の場合は非表示のみ
            hide();
            return;
        }

        System.out.println("IMEInputLayer: Text submitted: '" + text + "'");

        // 確定文字列を1文字ずつKernelに転送
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (kernel != null) {
                kernel.keyPressed(ch, 0);
            }
        }

        // 最後にEnterキーをKernelに転送
        if (kernel != null) {
            kernel.keyPressed('\n', 10); // '\n' = Enter
        }

        // テキストをクリアして非表示
        imeTextField.setText("");
        hide();
    }

    /**
     * IME入力レイヤーを破棄する。
     */
    public void dispose() {
        hide();
        if (imeTextField.getParent() != null) {
            imeTextField.getParent().remove(imeTextField);
        }
        System.out.println("IMEInputLayer: Disposed");
    }
}
