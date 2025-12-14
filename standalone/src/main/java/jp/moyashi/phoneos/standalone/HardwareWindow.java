package jp.moyashi.phoneos.standalone;

import jp.moyashi.phoneos.core.Kernel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * スタンドアロン環境用ハードウェアボタンウィンドウ。
 * ホームボタン、音量ボタン等の物理ボタンをエミュレートする。
 * メインウィンドウの右側に配置される独立したSwingウィンドウ。
 */
public class HardwareWindow extends JFrame {

    private static final int BUTTON_SIZE = 50;
    private static final int BUTTON_MARGIN = 8;
    private static final int WINDOW_WIDTH = BUTTON_SIZE + BUTTON_MARGIN * 2;

    private final Kernel kernel;
    private final int mainWindowWidth;
    private final int mainWindowHeight;

    // ウィンドウドラッグ用
    private Point dragOffset;

    /**
     * HardwareWindowを構築する。
     *
     * @param kernel Kernelインスタンス
     * @param mainWindowWidth メインウィンドウの幅（位置計算用）
     * @param mainWindowHeight メインウィンドウの高さ（位置計算用）
     */
    public HardwareWindow(Kernel kernel, int mainWindowWidth, int mainWindowHeight) {
        this.kernel = kernel;
        this.mainWindowWidth = mainWindowWidth;
        this.mainWindowHeight = mainWindowHeight;

        initializeWindow();
        setupButtons();
        setupWindowDrag();
    }

    private void initializeWindow() {
        setTitle("Hardware");
        setUndecorated(true);
        setResizable(false);
        setAlwaysOnTop(true);
        setType(Type.UTILITY);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // 3ボタン分の高さ（ホーム + 音量上 + 音量下）
        int windowHeight = BUTTON_SIZE * 3 + BUTTON_MARGIN * 4;
        setSize(WINDOW_WIDTH, windowHeight);

        // 背景色
        getContentPane().setBackground(new Color(40, 40, 40));
    }

    private void setupButtons() {
        setLayout(null);

        // 音量アップボタン
        JPanel volumeUpButton = createButton("＋", () -> onVolumeUp());
        volumeUpButton.setBounds(BUTTON_MARGIN, BUTTON_MARGIN, BUTTON_SIZE, BUTTON_SIZE);
        add(volumeUpButton);

        // ホームボタン（中央）
        JPanel homeButton = createHomeButton();
        homeButton.setBounds(BUTTON_MARGIN, BUTTON_MARGIN + BUTTON_SIZE + BUTTON_MARGIN, BUTTON_SIZE, BUTTON_SIZE);
        add(homeButton);

        // 音量ダウンボタン
        JPanel volumeDownButton = createButton("－", () -> onVolumeDown());
        volumeDownButton.setBounds(BUTTON_MARGIN, BUTTON_MARGIN + (BUTTON_SIZE + BUTTON_MARGIN) * 2, BUTTON_SIZE, BUTTON_SIZE);
        add(volumeDownButton);
    }

    private JPanel createButton(String label, Runnable onClick) {
        JPanel button = new JPanel() {
            private boolean pressed = false;
            private boolean hovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        pressed = true;
                        repaint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (pressed && contains(e.getPoint())) {
                            onClick.run();
                        }
                        pressed = false;
                        repaint();
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        pressed = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // ボタン背景
                if (pressed) {
                    g2d.setColor(new Color(80, 80, 80));
                } else if (hovered) {
                    g2d.setColor(new Color(60, 60, 60));
                } else {
                    g2d.setColor(new Color(50, 50, 50));
                }
                g2d.fillRoundRect(2, 2, w - 4, h - 4, 12, 12);

                // 枠線
                g2d.setColor(new Color(100, 100, 100));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(2, 2, w - 4, h - 4, 12, 12);

                // ラベル
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 20));
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (w - fm.stringWidth(label)) / 2;
                int textY = (h + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(label, textX, textY);
            }
        };
        button.setOpaque(false);
        return button;
    }

    private JPanel createHomeButton() {
        JPanel button = new JPanel() {
            private boolean pressed = false;
            private boolean hovered = false;

            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        pressed = true;
                        repaint();
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        if (pressed && contains(e.getPoint())) {
                            onHomeButtonClicked();
                        }
                        pressed = false;
                        repaint();
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        pressed = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int w = getWidth();
                int h = getHeight();

                // ボタン背景
                if (pressed) {
                    g2d.setColor(new Color(80, 80, 80));
                } else if (hovered) {
                    g2d.setColor(new Color(60, 60, 60));
                } else {
                    g2d.setColor(new Color(50, 50, 50));
                }
                g2d.fillRoundRect(2, 2, w - 4, h - 4, 12, 12);

                // 枠線
                g2d.setColor(new Color(100, 100, 100));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(2, 2, w - 4, h - 4, 12, 12);

                // ホームアイコン（家の形）
                g2d.setColor(Color.WHITE);
                int cx = w / 2;
                int cy = h / 2;
                int iconSize = 20;

                // 屋根（三角形）
                int[] xPoints = {cx, cx - iconSize / 2, cx + iconSize / 2};
                int[] yPoints = {cy - iconSize / 2, cy, cy};
                g2d.fillPolygon(xPoints, yPoints, 3);

                // 家本体
                int bodyW = iconSize * 2 / 3;
                int bodyH = iconSize / 2;
                g2d.fillRect(cx - bodyW / 2, cy, bodyW, bodyH);
            }
        };
        button.setOpaque(false);
        return button;
    }

    private void setupWindowDrag() {
        // コンテンツペインにドラッグリスナーを追加
        Container contentPane = getContentPane();
        contentPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }
        });
        contentPane.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset != null) {
                    Point currentLocation = getLocation();
                    setLocation(
                        currentLocation.x + e.getX() - dragOffset.x,
                        currentLocation.y + e.getY() - dragOffset.y
                    );
                }
            }
        });
    }

    private void onHomeButtonClicked() {
        System.out.println("HardwareWindow: Home button clicked");
        if (kernel != null) {
            kernel.requestGoHome();
        }
    }

    private void onVolumeUp() {
        System.out.println("HardwareWindow: Volume up clicked");
        // TODO: 音量アップ処理を実装
        if (kernel != null) {
            // kernel.volumeUp();
        }
    }

    private void onVolumeDown() {
        System.out.println("HardwareWindow: Volume down clicked");
        // TODO: 音量ダウン処理を実装
        if (kernel != null) {
            // kernel.volumeDown();
        }
    }

    /**
     * ウィンドウを表示し、画面中央右側に配置する。
     */
    public void showWindow() {
        // 画面の中央付近に配置（メインウィンドウの右側を想定）
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - mainWindowWidth) / 2 + mainWindowWidth + 10;
        int y = (screenSize.height - getHeight()) / 2;
        setLocation(x, y);
        setVisible(true);
    }
}
