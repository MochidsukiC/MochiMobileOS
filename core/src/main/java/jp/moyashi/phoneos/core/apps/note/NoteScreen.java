package jp.moyashi.phoneos.core.apps.note;

import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.ui.Screen;
import processing.core.PGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * メモ一覧画面。
 */
public class NoteScreen implements Screen {

    private Kernel kernel;
    private List<Note> notes;
    private int scrollOffset = 0;
    private static final int ITEM_HEIGHT = 80;
    private static final int HEADER_HEIGHT = 60;

    /**
     * メモデータクラス。
     */
    public static class Note {
        public String id;
        public String title;
        public String content;
        public long lastModified;

        public Note(String id, String title, String content) {
            this.id = id;
            this.title = title;
            this.content = content;
            this.lastModified = System.currentTimeMillis();
        }
    }

    public NoteScreen(Kernel kernel) {
        this.kernel = kernel;
        loadNotes();
    }

    /**
     * メモをVFSから読み込む。
     */
    private void loadNotes() {
        notes = new ArrayList<>();

        // VFSからメモデータを読み込む
        String notesData = kernel.getVFS().readFile("apps/note/notes.json");
        if (notesData != null && !notesData.isEmpty()) {
            // JSONパースは簡易的に実装
            String[] noteEntries = notesData.split("###NOTE###");
            for (String entry : noteEntries) {
                if (entry.trim().isEmpty()) continue;

                String[] lines = entry.split("\n", 4);
                if (lines.length >= 3) {
                    String id = lines[0].replace("ID:", "").trim();
                    String title = lines[1].replace("TITLE:", "").trim();
                    String content = lines.length > 2 ? lines[2].replace("CONTENT:", "").trim() : "";
                    notes.add(new Note(id, title, content));
                }
            }
        }

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("NoteScreen", "メモを読み込み: " + notes.size() + "件");
        }
    }

    /**
     * メモをVFSに保存する。
     */
    public void saveNotes() {
        StringBuilder sb = new StringBuilder();
        for (Note note : notes) {
            sb.append("ID:").append(note.id).append("\n");
            sb.append("TITLE:").append(note.title).append("\n");
            sb.append("CONTENT:").append(note.content).append("\n");
            sb.append("###NOTE###\n");
        }

        kernel.getVFS().writeFile("apps/note/notes.json", sb.toString());

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("NoteScreen", "メモを保存: " + notes.size() + "件");
        }
    }

    @Override
    public void draw(PGraphics g) {
        g.background(240);

        // ヘッダー
        g.fill(70, 130, 180);
        g.rect(0, 0, g.width, HEADER_HEIGHT);

        g.fill(255);
        g.textAlign(g.LEFT, g.CENTER);
        if (kernel.getJapaneseFont() != null) {
            g.textFont(kernel.getJapaneseFont());
        }
        g.textSize(20);
        g.text("メモ", 20, HEADER_HEIGHT / 2);

        // 新規作成ボタン
        g.fill(100, 180, 100);
        g.rect(g.width - 70, 10, 60, 40, 5);
        g.fill(255);
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(24);
        g.text("+", g.width - 40, 30);

        // メモ一覧
        int y = HEADER_HEIGHT + 10;
        g.textAlign(g.LEFT, g.TOP);

        if (notes.isEmpty()) {
            g.fill(150);
            g.textSize(16);
            g.textAlign(g.CENTER, g.CENTER);
            g.text("メモがありません\n\n右上の + ボタンで\n新規作成できます",
                   g.width / 2, g.height / 2);
        } else {
            for (int i = 0; i < notes.size(); i++) {
                Note note = notes.get(i);
                int itemY = y + (i * ITEM_HEIGHT) - scrollOffset;

                if (itemY + ITEM_HEIGHT < HEADER_HEIGHT || itemY > g.height) {
                    continue; // 表示範囲外
                }

                // メモアイテム背景
                g.fill(255);
                g.stroke(200);
                g.strokeWeight(1);
                g.rect(10, itemY, g.width - 20, ITEM_HEIGHT - 10, 8);
                g.noStroke();

                // タイトル
                g.fill(0);
                g.textSize(16);
                g.text(note.title.isEmpty() ? "(無題)" : note.title, 20, itemY + 15);

                // プレビュー
                g.fill(100);
                g.textSize(12);
                String preview = note.content.length() > 40 ?
                    note.content.substring(0, 40) + "..." : note.content;
                g.text(preview, 20, itemY + 40);
            }
        }

        // 戻るボタン
        g.fill(200);
        g.rect(10, g.height - 50, 80, 40, 5);
        g.fill(0);
        g.textAlign(g.CENTER, g.CENTER);
        g.textSize(16);
        g.text("戻る", 50, g.height - 30);
    }

    @Override
    public void mousePressed(PGraphics g, int mouseX, int mouseY) {
        // 新規作成ボタン
        if (mouseY < HEADER_HEIGHT && mouseX > g.width - 70) {
            createNewNote();
            return;
        }

        // 戻るボタン
        if (mouseY > g.height - 50 && mouseX < 90) {
            kernel.getScreenManager().popScreen();
            return;
        }

        // メモアイテムのクリック
        if (mouseY > HEADER_HEIGHT) {
            int index = (mouseY - HEADER_HEIGHT - 10 + scrollOffset) / ITEM_HEIGHT;
            if (index >= 0 && index < notes.size()) {
                openNote(notes.get(index));
            }
        }
    }

    /**
     * 新規メモを作成。
     */
    private void createNewNote() {
        String id = "note_" + System.currentTimeMillis();
        Note newNote = new Note(id, "", "");
        notes.add(0, newNote);
        saveNotes();

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("NoteScreen", "新規メモ作成: " + id);
        }

        kernel.getScreenManager().pushScreen(new NoteEditScreen(kernel, newNote, this));
    }

    /**
     * メモを開く。
     */
    private void openNote(Note note) {
        if (kernel.getLogger() != null) {
            kernel.getLogger().info("NoteScreen", "メモを開く: " + note.id);
        }
        kernel.getScreenManager().pushScreen(new NoteEditScreen(kernel, note, this));
    }

    @Override
    public void keyPressed(PGraphics g, char key, int keyCode) {
        // スクロール機能
        if (keyCode == 38) { // UP
            scrollOffset = Math.max(0, scrollOffset - 20);
        } else if (keyCode == 40) { // DOWN
            int maxScroll = Math.max(0, notes.size() * ITEM_HEIGHT - (600 - HEADER_HEIGHT - 60));
            scrollOffset = Math.min(maxScroll, scrollOffset + 20);
        }
    }

    /**
     * メモを更新（編集画面から呼ばれる）。
     */
    public void updateNote(Note updatedNote) {
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).id.equals(updatedNote.id)) {
                notes.set(i, updatedNote);
                break;
            }
        }
        updatedNote.lastModified = System.currentTimeMillis();
        saveNotes();
    }

    /**
     * メモを削除。
     */
    public void deleteNote(Note note) {
        notes.removeIf(n -> n.id.equals(note.id));
        saveNotes();

        if (kernel.getLogger() != null) {
            kernel.getLogger().info("NoteScreen", "メモを削除: " + note.id);
        }
    }
}
