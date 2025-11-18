package jp.moyashi.phoneos.core.apps.note;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.clipboard.ClipData;
import jp.moyashi.phoneos.core.service.clipboard.ClipboardManager;
import jp.moyashi.phoneos.core.ui.Screen;
import jp.moyashi.phoneos.core.ui.components.Button;
import jp.moyashi.phoneos.core.ui.components.Label;
import jp.moyashi.phoneos.core.ui.components.TextField;
import jp.moyashi.phoneos.core.ui.components.TextArea;
import processing.core.PApplet;
import processing.core.PGraphics;

/**
 * メモ編集画面。
 * クリップボード機能（コピー・貼り付け）をテスト可能。
 */
public class NoteEditScreen implements Screen {

    private Kernel kernel;
    private NoteScreen.Note note;
    private NoteScreen parentScreen;

    // UIコンポーネント
    private Label headerLabel;
    private Label titleLabel;
    private Label contentLabel;
    private Label clipboardLabel;

    private TextField titleField;
    private TextArea contentArea;

    private Button copyButton;
    private Button pasteButton;
    private Button deleteButton;
    private Button saveButton;
    private Button backButton;

    // 修飾キー状態
    private boolean shiftPressed = false;
    private boolean ctrlPressed = false;

    // UI定数
    private static final int HEADER_HEIGHT = 60;
    private static final int BUTTON_HEIGHT = 40;
    private static final int MARGIN = 10;
    private static final int TITLE_FIELD_Y = HEADER_HEIGHT + 40;
    private static final int TITLE_FIELD_HEIGHT = 35;
    private static final int CONTENT_FIELD_Y = HEADER_HEIGHT + 130;
    private static final int CONTENT_FIELD_HEIGHT = 200;

    public NoteEditScreen(Kernel kernel, NoteScreen.Note note, NoteScreen parentScreen) {
        this.kernel = kernel;
        this.note = note;
        this.parentScreen = parentScreen;

        if (kernel.getLogger() != null) {
            kernel.getLogger().debug("NoteEditScreen", "メモ編集画面を初期化");
        }

        initializeComponents();
    }

    private void initializeComponents() {
        // ヘッダーラベル
        headerLabel = new Label(20, HEADER_HEIGHT / 2 - 10, 360, 20, "メモ編集");
        headerLabel.setTextSize(18);
        headerLabel.setHorizontalAlign(PApplet.LEFT);
        { var t=jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme(); if (t!=null) headerLabel.setTextColor(t.colorOnSurface()); }
        if (kernel.getJapaneseFont() != null) {
            headerLabel.setFont(kernel.getJapaneseFont());
        }

        // タイトルラベル
        titleLabel = new Label(MARGIN, HEADER_HEIGHT + 20, 100, 12, "タイトル:");
        titleLabel.setTextSize(12);
        titleLabel.setHorizontalAlign(PApplet.LEFT);
        titleLabel.setTextColor(0xFF000000);
        if (kernel.getJapaneseFont() != null) {
            titleLabel.setFont(kernel.getJapaneseFont());
        }

        // タイトル入力フィールド
        titleField = new TextField(MARGIN, TITLE_FIELD_Y, 380, TITLE_FIELD_HEIGHT, "(タイトルを入力)");
        titleField.setText(note.title);
        if (kernel.getJapaneseFont() != null) {
            titleField.setFont(kernel.getJapaneseFont());
        }
        if (kernel.getLogger() != null) {
            titleField.setLogger(kernel.getLogger());
        }

        // コンテンツラベル
        contentLabel = new Label(MARGIN, HEADER_HEIGHT + 90, 100, 12, "内容:");
        contentLabel.setTextSize(12);
        contentLabel.setHorizontalAlign(PApplet.LEFT);
        contentLabel.setTextColor(0xFF000000);
        if (kernel.getJapaneseFont() != null) {
            contentLabel.setFont(kernel.getJapaneseFont());
        }

        // コンテンツ入力エリア
        contentArea = new TextArea(MARGIN, CONTENT_FIELD_Y, 380, CONTENT_FIELD_HEIGHT, "(内容を入力)");
        contentArea.setText(note.content);
        if (kernel.getJapaneseFont() != null) {
            contentArea.setFont(kernel.getJapaneseFont());
        }
        if (kernel.getLogger() != null) {
            contentArea.setLogger(kernel.getLogger());
        }

        // コピーボタン
        int buttonWidth = (400 - 4 * MARGIN) / 2;
        int clipboardY = CONTENT_FIELD_Y + CONTENT_FIELD_HEIGHT + 20;

        copyButton = new Button(MARGIN, clipboardY, buttonWidth, BUTTON_HEIGHT, "📋 コピー");
        copyButton.setBackgroundColor(0xFF64B464);
        copyButton.setHoverColor(0xFF74C474);
        copyButton.setPressColor(0xFF54A454);
        { var t=jp.moyashi.phoneos.core.ui.theme.ThemeContext.getTheme(); if (t!=null) copyButton.setTextColor(t.colorOnSurface()); }
        copyButton.setCornerRadius(5);
        copyButton.setOnClickListener(this::copyToClipboard);

        // 貼り付けボタン
        pasteButton = new Button(MARGIN * 2 + buttonWidth, clipboardY, buttonWidth, BUTTON_HEIGHT, "📄 貼り付け");
        pasteButton.setBackgroundColor(0xFF6496DC);
        pasteButton.setHoverColor(0xFF74A6EC);
        pasteButton.setPressColor(0xFF5486CC);
        pasteButton.setTextColor(0xFFFFFFFF);
        pasteButton.setCornerRadius(5);
        pasteButton.setOnClickListener(this::pasteFromClipboard);

        // クリップボード状態ラベル
        clipboardLabel = new Label(MARGIN, clipboardY + BUTTON_HEIGHT + 5, 380, 15, "");
        clipboardLabel.setTextSize(10);
        clipboardLabel.setHorizontalAlign(PApplet.LEFT);
        clipboardLabel.setTextColor(0xFF969696);
        if (kernel.getJapaneseFont() != null) {
            clipboardLabel.setFont(kernel.getJapaneseFont());
        }

        // 下部ボタン
        int bottomY = 600 - 60;

        deleteButton = new Button(MARGIN, bottomY, 80, BUTTON_HEIGHT, "削除");
        deleteButton.setBackgroundColor(0xFFDC6464);
        deleteButton.setHoverColor(0xFFEC7474);
        deleteButton.setPressColor(0xFFCC5454);
        deleteButton.setTextColor(0xFFFFFFFF);
        deleteButton.setCornerRadius(5);
        deleteButton.setOnClickListener(this::deleteNote);

        saveButton = new Button(400 / 2 - 40, bottomY, 80, BUTTON_HEIGHT, "保存");
        saveButton.setBackgroundColor(0xFF64B464);
        saveButton.setHoverColor(0xFF74C474);
        saveButton.setPressColor(0xFF54A454);
        saveButton.setTextColor(0xFFFFFFFF);
        saveButton.setCornerRadius(5);
        saveButton.setOnClickListener(this::saveNote);

        backButton = new Button(400 - 90, bottomY, 80, BUTTON_HEIGHT, "戻る");
        backButton.setBackgroundColor(0xFF969696);
        backButton.setHoverColor(0xFFA6A6A6);
        backButton.setPressColor(0xFF868686);
        backButton.setTextColor(0xFFFFFFFF);
        backButton.setCornerRadius(5);
        backButton.setOnClickListener(() -> kernel.getScreenManager().popScreen());
    }

    @Override
    public void draw(PGraphics g) {
        g.background(250);

        // ヘッダー背景
        g.fill(70, 130, 180);
        g.rect(0, 0, g.width, HEADER_HEIGHT);

        // ヘッダーラベル
        headerLabel.draw(g);

        // タイトルラベルとフィールド
        titleLabel.draw(g);
        titleField.draw(g);

        // コンテンツラベルとエリア
        contentLabel.draw(g);
        contentArea.draw(g);

        // クリップボードボタン
        copyButton.draw(g);
        pasteButton.draw(g);

        // クリップボード状態表示を更新
        updateClipboardLabel();
        clipboardLabel.draw(g);

        // 下部ボタン
        deleteButton.draw(g);
        saveButton.draw(g);
        backButton.draw(g);
    }

    private void updateClipboardLabel() {
        ClipboardManager clipboard = kernel.getClipboardManager();
        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clip = clipboard.getPrimaryClip();
            String clipPreview = "クリップボード: " +
                (clip.getText().length() > 30 ? clip.getText().substring(0, 30) + "..." : clip.getText());
            clipboardLabel.setText(clipPreview);
        } else {
            clipboardLabel.setText("");
        }
    }

    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        // コンポーネントのイベント処理
        if (titleField.onMousePressed(mouseX, mouseY)) return;
        if (contentArea.onMousePressed(mouseX, mouseY)) return;

        if (copyButton.onMousePressed(mouseX, mouseY)) return;
        if (pasteButton.onMousePressed(mouseX, mouseY)) return;

        if (deleteButton.onMousePressed(mouseX, mouseY)) return;
        if (saveButton.onMousePressed(mouseX, mouseY)) return;
        if (backButton.onMousePressed(mouseX, mouseY)) return;
    }

    @Override
    public void mouseReleased(PGraphics g, int mouseX, int mouseY) {
        // コンポーネントのイベント処理
        titleField.onMouseReleased(mouseX, mouseY);
        contentArea.onMouseReleased(mouseX, mouseY);

        copyButton.onMouseReleased(mouseX, mouseY);
        pasteButton.onMouseReleased(mouseX, mouseY);

        deleteButton.onMouseReleased(mouseX, mouseY);
        saveButton.onMouseReleased(mouseX, mouseY);
        backButton.onMouseReleased(mouseX, mouseY);
    }

    public void mouseMoved(PGraphics g, int mouseX, int mouseY) {
        // コンポーネントのhoverイベント
        titleField.onMouseMoved(mouseX, mouseY);
        contentArea.onMouseMoved(mouseX, mouseY);

        copyButton.onMouseMoved(mouseX, mouseY);
        pasteButton.onMouseMoved(mouseX, mouseY);

        deleteButton.onMouseMoved(mouseX, mouseY);
        saveButton.onMouseMoved(mouseX, mouseY);
        backButton.onMouseMoved(mouseX, mouseY);
    }

    @Override
    public void mouseDragged(PGraphics g, int mouseX, int mouseY) {
        // テキスト選択のためのドラッグ処理
        titleField.onMouseDragged(mouseX, mouseY);
        contentArea.onMouseDragged(mouseX, mouseY);
    }

    @Override
    public void keyPressed(PGraphics g, char key, int keyCode) {
        // Ctrl+C: コピー（keyCode=67）
        if (ctrlPressed && keyCode == 67) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().debug("NoteEditScreen", "Ctrl+C detected - calling copyToClipboard()");
            }
            copyToClipboard();
            return;
        }

        // Ctrl+V: ペースト（keyCode=86）
        if (ctrlPressed && keyCode == 86) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().debug("NoteEditScreen", "Ctrl+V detected - calling pasteFromClipboard()");
            }
            pasteFromClipboard();
            return;
        }

        // コンポーネントにキーイベントを送信
        if (titleField.isFocused()) {
            titleField.onKeyPressed(key, keyCode);
        } else if (contentArea.isFocused()) {
            contentArea.onKeyPressed(key, keyCode);
        }

        // Qキーで終了
        if (key == 'q' || key == 'Q') {
            kernel.getScreenManager().popScreen();
        }
    }

    @Override
    public void setModifierKeys(boolean shift, boolean ctrl) {
        // 修飾キーの状態を保持
        this.shiftPressed = shift;
        this.ctrlPressed = ctrl;

        // 修飾キーの状態を両コンポーネントに伝播
        if (titleField != null) {
            titleField.setShiftPressed(shift);
            titleField.setCtrlPressed(ctrl);
        }
        if (contentArea != null) {
            contentArea.setShiftPressed(shift);
            contentArea.setCtrlPressed(ctrl);
        }
    }

    @Override
    public boolean hasFocusedComponent() {
        // titleFieldまたはcontentAreaがフォーカスされているかチェック
        return (titleField != null && titleField.isFocused()) ||
               (contentArea != null && contentArea.isFocused());
    }

    /**
     * クリップボードにコピー。
     */
    private void copyToClipboard() {
        ClipboardManager clipboard = kernel.getClipboardManager();
        if (clipboard == null) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().error("NoteEditScreen", "ClipboardManagerが利用できません");
            }
            return;
        }

        String textToCopy;

        // フォーカスされているフィールドから選択テキストまたは全体をコピー
        if (titleField.isFocused()) {
            String selectedText = titleField.getSelectedText();
            textToCopy = selectedText.isEmpty() ? titleField.getText() : selectedText;

            if (kernel.getLogger() != null) {
                kernel.getLogger().info("NoteEditScreen",
                    (selectedText.isEmpty() ? "タイトル全体をコピー: " : "タイトルの選択範囲をコピー: ") + textToCopy.length() + "文字");
            }
        } else if (contentArea.isFocused()) {
            String selectedText = contentArea.getSelectedText();
            textToCopy = selectedText.isEmpty() ? contentArea.getText() : selectedText;

            if (kernel.getLogger() != null) {
                kernel.getLogger().info("NoteEditScreen",
                    (selectedText.isEmpty() ? "コンテンツ全体をコピー: " : "コンテンツの選択範囲をコピー: ") + textToCopy.length() + "文字");
            }
        } else {
            // フォーカスがない場合はコンテンツ全体
            textToCopy = contentArea.getText();
            if (kernel.getLogger() != null) {
                kernel.getLogger().info("NoteEditScreen", "コンテンツ全体をコピー: " + textToCopy.length() + "文字");
            }
        }

        clipboard.copyText("Note", textToCopy);

        System.out.println("✓ クリップボードにコピーしました: " +
            (textToCopy.length() > 20 ? textToCopy.substring(0, 20) + "..." : textToCopy));
    }

    /**
     * クリップボードから貼り付け。
     */
    private void pasteFromClipboard() {
        ClipboardManager clipboard = kernel.getClipboardManager();
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            if (kernel.getLogger() != null) {
                kernel.getLogger().warn("NoteEditScreen", "クリップボードが空です");
            }
            System.out.println("✗ クリップボードが空です");
            return;
        }

        ClipData clipData = clipboard.getPrimaryClip();
        String pastedText = clipData != null ? clipData.getText() : null;
        if (pastedText == null || pastedText.isEmpty()) {
            System.out.println("✗ クリップボードにテキストがありません");
            return;
        }

        // フォーカスされているフィールドのカーソル位置に貼り付け
        if (titleField.isFocused()) {
            // カーソル位置に挿入
            titleField.insertText(pastedText);

            if (kernel.getLogger() != null) {
                kernel.getLogger().info("NoteEditScreen", "タイトルのカーソル位置に貼り付け: " + pastedText.length() + "文字");
            }
        } else if (contentArea.isFocused()) {
            // カーソル位置に挿入
            contentArea.insertText(pastedText);

            if (kernel.getLogger() != null) {
                kernel.getLogger().info("NoteEditScreen", "コンテンツのカーソル位置に貼り付け: " + pastedText.length() + "文字");
            }
        } else {
            // フォーカスがない場合はコンテンツエリアにフォーカスして貼り付け
            contentArea.setFocused(true);
            contentArea.insertText(pastedText);

            if (kernel.getLogger() != null) {
                kernel.getLogger().info("NoteEditScreen", "コンテンツに貼り付け（自動フォーカス）: " + pastedText.length() + "文字");
            }
        }

        System.out.println("✓ クリップボードから貼り付けました: " +
            (pastedText.length() > 20 ? pastedText.substring(0, 20) + "..." : pastedText));
    }

    /**
     * メモを保存。
     */
    private void saveNote() {
        note.title = titleField.getText();
        note.content = contentArea.getText();
        parentScreen.updateNote(note);

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("NoteEditScreen", "メモを保存: " + note.id);
        }

        System.out.println("✓ メモを保存しました");
        kernel.getScreenManager().popScreen();
    }

    /**
     * メモを削除。
     */
    private void deleteNote() {
        parentScreen.deleteNote(note);

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("NoteEditScreen", "メモを削除: " + note.id);
        }

        System.out.println("✓ メモを削除しました");
        kernel.getScreenManager().popScreen();
    }
}
