package jp.moyashi.phoneos.core.service.chromium;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jp.moyashi.phoneos.core.Kernel;
import jp.moyashi.phoneos.core.service.VFS;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class BrowserDataManager {
    private static final String BOOKMARKS_FILE = "system/browser_chromium/bookmarks.json";
    private static final String HISTORY_FILE = "system/browser_chromium/history.json";

    private final VFS vfs;
    private final Gson gson;

    private List<BookmarkEntry> bookmarks = new ArrayList<>();
    private List<HistoryEntry> history = new ArrayList<>();

    public BrowserDataManager(Kernel kernel) {
        this.vfs = kernel.getVFS();
        this.gson = new Gson();
        loadBookmarks();
        loadHistory();
    }

    public void loadBookmarks() {
        String json = vfs.readFile(BOOKMARKS_FILE);
        if (json != null) {
            try {
                Type listType = new TypeToken<ArrayList<BookmarkEntry>>(){}.getType();
                bookmarks = gson.fromJson(json, listType);
            } catch (Exception e) {
                System.err.println("Failed to parse bookmarks.json: " + e.getMessage());
                bookmarks = new ArrayList<>();
            }
        }
        if (bookmarks == null) bookmarks = new ArrayList<>();
    }

    public void saveBookmarks() {
        String json = gson.toJson(bookmarks);
        vfs.writeFile(BOOKMARKS_FILE, json);
    }

    public void loadHistory() {
        String json = vfs.readFile(HISTORY_FILE);
        if (json != null) {
            try {
                Type listType = new TypeToken<ArrayList<HistoryEntry>>(){}.getType();
                history = gson.fromJson(json, listType);
            } catch (Exception e) {
                System.err.println("Failed to parse history.json: " + e.getMessage());
                history = new ArrayList<>();
            }
        }
        if (history == null) history = new ArrayList<>();
    }

    public void saveHistory() {
        String json = gson.toJson(history);
        vfs.writeFile(HISTORY_FILE, json);
    }

    public void addBookmark(String title, String url) {
        if (url == null || url.isEmpty()) return;
        if (isBookmarked(url)) return;
        bookmarks.add(new BookmarkEntry(title == null || title.isEmpty() ? url : title, url, System.currentTimeMillis()));
        saveBookmarks();
    }

    public void removeBookmark(String url) {
        if (url == null) return;
        bookmarks.removeIf(b -> b.url.equals(url));
        saveBookmarks();
    }

    public boolean isBookmarked(String url) {
        if (url == null) return false;
        return bookmarks.stream().anyMatch(b -> b.url.equals(url));
    }
    
    public List<BookmarkEntry> getBookmarks() {
        return new ArrayList<>(bookmarks);
    }

    public void addToHistory(String title, String url) {
        if (url == null || url.isEmpty() || url.equals("about:blank")) return;
        
        // 重複排除: 最新の履歴と同じURLなら追加しない
        if (!history.isEmpty() && history.get(0).url.equals(url)) {
            return; 
        }
        
        history.add(0, new HistoryEntry(title == null || title.isEmpty() ? url : title, url, System.currentTimeMillis()));
        
        // 最大100件に制限
        if (history.size() > 100) {
            history = new ArrayList<>(history.subList(0, 100));
        }
        saveHistory();
    }
    
    public List<HistoryEntry> getHistory() {
        return new ArrayList<>(history);
    }
    
    public void clearHistory() {
        history.clear();
        saveHistory();
    }

    public static class BookmarkEntry {
        public String title;
        public String url;
        public long timestamp;

        public BookmarkEntry(String title, String url, long timestamp) {
            this.title = title;
            this.url = url;
            this.timestamp = timestamp;
        }
    }

    public static class HistoryEntry {
        public String title;
        public String url;
        public long timestamp;

        public HistoryEntry(String title, String url, long timestamp) {
            this.title = title;
            this.url = url;
            this.timestamp = timestamp;
        }
    }
}
