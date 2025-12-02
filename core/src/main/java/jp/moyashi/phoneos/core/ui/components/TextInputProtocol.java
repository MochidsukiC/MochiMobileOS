package jp.moyashi.phoneos.core.ui.components;

/**
 * テキスト入力コンポーネント用プロトコル（iOS UITextInput相当）
 * すべてのテキスト入力コンポーネントはこのインターフェースを実装する
 */
public interface TextInputProtocol {

    // テキストアクセス
    String getText();
    void setText(String text);

    // 選択操作
    String getSelectedText();
    boolean hasSelection();
    void selectAll();
    void clearSelection();

    // 選択範囲
    int getSelectionStart();
    int getSelectionEnd();
    void setSelection(int start, int end);

    // 編集操作
    void deleteSelection();
    void deleteBackward();  // バックスペース操作（選択があれば削除、なければカーソル前の1文字を削除）
    void insertTextAtCursor(String text);
    void replaceSelection(String text);

    // カーソル
    int getCursorPosition();
    void setCursorPosition(int position);
}
