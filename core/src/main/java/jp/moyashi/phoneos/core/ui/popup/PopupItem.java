package jp.moyashi.phoneos.core.ui.popup;

/**
 * ポップアップメニューの個別アイテムを表すクラス。
 * 各アイテムはテキスト、アクション、有効/無効状態を持つ。
 */
public class PopupItem {
    
    /** アイテムのテキスト */
    private final String text;
    
    /** クリック時に実行されるアクション */
    private final Runnable action;
    
    /** アイテムが有効かどうか */
    private boolean enabled;
    
    /**
     * PopupItemを作成する。
     * 
     * @param text 表示テキスト
     * @param action クリック時のアクション
     */
    public PopupItem(String text, Runnable action) {
        this.text = text;
        this.action = action;
        this.enabled = true;
    }
    
    /**
     * PopupItemを作成する（有効/無効指定可能）。
     * 
     * @param text 表示テキスト
     * @param action クリック時のアクション
     * @param enabled 有効かどうか
     */
    public PopupItem(String text, Runnable action, boolean enabled) {
        this.text = text;
        this.action = action;
        this.enabled = enabled;
    }
    
    /**
     * アイテムのテキストを取得する。
     * 
     * @return テキスト
     */
    public String getText() {
        return text;
    }
    
    /**
     * アイテムが有効かどうかを確認する。
     * 
     * @return 有効な場合true
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * アイテムの有効/無効を設定する。
     * 
     * @param enabled 有効にする場合true
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    /**
     * アイテムがクリックされた時のアクションを実行する。
     */
    public void executeAction() {
        if (enabled && action != null) {
            action.run();
        }
    }
    
    @Override
    public String toString() {
        return "PopupItem{text='" + text + "', enabled=" + enabled + "}";
    }
}