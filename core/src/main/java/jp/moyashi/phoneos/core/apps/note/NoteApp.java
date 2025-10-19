package jp.moyashi.phoneos.core.apps.note;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.app.IApplication;
import jp.moyashi.phoneos.core.ui.Screen;

/**
 * メモアプリケーション。
 * クリップボード機能のテストに使用。
 */
public class NoteApp implements IApplication {

    @Override
    public String getName() {
        return "メモ";
    }

    @Override
    public String getDescription() {
        return "テキストメモとクリップボードのテスト";
    }

    @Override
    public Screen getEntryScreen(Kernel kernel) {
        if (kernel.getLogger() != null) {
            kernel.getLogger().info("NoteApp", "メモアプリを起動");
        }
        return new NoteScreen(kernel);
    }
}
