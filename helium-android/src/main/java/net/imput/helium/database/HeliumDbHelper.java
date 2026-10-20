package net.imput.helium.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.imput.helium.search.SearchSuggestion;

/**
 * HeliumDbHelper: High-performance SQLite backing store for History and Bookmarks.
 * All write and read operations are offloaded to an asynchronous background worker
 * thread, with results delivered back to the Android main thread via Handler.
 */
public final class HeliumDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "helium_browser.db";
    private static final int DATABASE_VERSION = 1;

    // Bookmarks Table
    public static final String TABLE_BOOKMARKS = "bookmarks";
    public static final String COL_BOOKMARK_ID = "id";
    public static final String COL_BOOKMARK_URL = "url";
    public static final String COL_BOOKMARK_TITLE = "title";
    public static final String COL_BOOKMARK_CREATED = "created_at";

    // History Table
    public static final String TABLE_HISTORY = "history";
    public static final String COL_HISTORY_ID = "id";
    public static final String COL_HISTORY_URL = "url";
    public static final String COL_HISTORY_TITLE = "title";
    public static final String COL_HISTORY_VISIT_COUNT = "visit_count";
    public static final String COL_HISTORY_LAST_VISITED = "last_visited_at";

    public interface Callback<T> {
        void onComplete(T result);
    }

    private static volatile HeliumDbHelper sInstance;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    public static HeliumDbHelper getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (HeliumDbHelper.class) {
                if (sInstance == null) {
                    sInstance = new HeliumDbHelper(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private HeliumDbHelper(@NonNull Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Bookmarks Schema
        db.execSQL("CREATE TABLE " + TABLE_BOOKMARKS + " (" +
                COL_BOOKMARK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_BOOKMARK_URL + " TEXT UNIQUE NOT NULL, " +
                COL_BOOKMARK_TITLE + " TEXT, " +
                COL_BOOKMARK_CREATED + " INTEGER NOT NULL);");
        db.execSQL("CREATE INDEX idx_bookmarks_url ON " + TABLE_BOOKMARKS + "(" + COL_BOOKMARK_URL + ");");

        // History Schema
        db.execSQL("CREATE TABLE " + TABLE_HISTORY + " (" +
                COL_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_HISTORY_URL + " TEXT UNIQUE NOT NULL, " +
                COL_HISTORY_TITLE + " TEXT, " +
                COL_HISTORY_VISIT_COUNT + " INTEGER DEFAULT 1, " +
                COL_HISTORY_LAST_VISITED + " INTEGER NOT NULL);");
        db.execSQL("CREATE INDEX idx_history_url ON " + TABLE_HISTORY + "(" + COL_HISTORY_URL + ");");
        db.execSQL("CREATE INDEX idx_history_visited ON " + TABLE_HISTORY + "(" + COL_HISTORY_LAST_VISITED + " DESC);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKMARKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    // --- Bookmarks Operations ---

    public void addBookmark(@NonNull String url, @NonNull String title, @Nullable Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            boolean success = false;
            try {
                SQLiteDatabase db = getWritableDatabase();
                ContentValues cv = new ContentValues();
                cv.put(COL_BOOKMARK_URL, url);
                cv.put(COL_BOOKMARK_TITLE, title.isEmpty() ? url : title);
                cv.put(COL_BOOKMARK_CREATED, System.currentTimeMillis());
                long id = db.insertWithOnConflict(TABLE_BOOKMARKS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                success = id != -1;
            } catch (Exception ignored) {}

            boolean finalSuccess = success;
            if (callback != null) {
                mMainHandler.post(() -> callback.onComplete(finalSuccess));
            }
        });
    }

    public void removeBookmark(@NonNull String url, @Nullable Callback<Boolean> callback) {
        mExecutor.execute(() -> {
            int rows = 0;
            try {
                SQLiteDatabase db = getWritableDatabase();
                rows = db.delete(TABLE_BOOKMARKS, COL_BOOKMARK_URL + " = ?", new String[]{url});
            } catch (Exception ignored) {}

            boolean deleted = rows > 0;
            if (callback != null) {
                mMainHandler.post(() -> callback.onComplete(deleted));
            }
        });
    }

    public void getAllBookmarks(@NonNull Callback<List<BookmarkRecord>> callback) {
        mExecutor.execute(() -> {
            List<BookmarkRecord> list = new ArrayList<>();
            try {
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = db.query(TABLE_BOOKMARKS, null, null, null, null, null, COL_BOOKMARK_CREATED + " DESC");
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_BOOKMARK_ID));
                        String url = cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKMARK_URL));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_BOOKMARK_TITLE));
                        long createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_BOOKMARK_CREATED));
                        list.add(new BookmarkRecord(id, url, title, createdAt));
                    }
                    cursor.close();
                }
            } catch (Exception ignored) {}

            mMainHandler.post(() -> callback.onComplete(list));
        });
    }

    // --- History Operations ---

    public void recordVisit(@NonNull String url, @NonNull String title) {
        // Skip internal/local URLs from history
        if (url.startsWith("file://") || url.startsWith("about:") || url.startsWith("chrome://")) {
            return;
        }

        mExecutor.execute(() -> {
            try {
                SQLiteDatabase db = getWritableDatabase();
                long now = System.currentTimeMillis();

                // Check if already in history
                Cursor c = db.query(TABLE_HISTORY, new String[]{COL_HISTORY_VISIT_COUNT},
                        COL_HISTORY_URL + " = ?", new String[]{url}, null, null, null);
                if (c != null && c.moveToFirst()) {
                    int visits = c.getInt(0) + 1;
                    c.close();
                    ContentValues cv = new ContentValues();
                    cv.put(COL_HISTORY_TITLE, title.isEmpty() ? url : title);
                    cv.put(COL_HISTORY_VISIT_COUNT, visits);
                    cv.put(COL_HISTORY_LAST_VISITED, now);
                    db.update(TABLE_HISTORY, cv, COL_HISTORY_URL + " = ?", new String[]{url});
                } else {
                    if (c != null) c.close();
                    ContentValues cv = new ContentValues();
                    cv.put(COL_HISTORY_URL, url);
                    cv.put(COL_HISTORY_TITLE, title.isEmpty() ? url : title);
                    cv.put(COL_HISTORY_VISIT_COUNT, 1);
                    cv.put(COL_HISTORY_LAST_VISITED, now);
                    db.insert(TABLE_HISTORY, null, cv);
                }
            } catch (Exception ignored) {}
        });
    }

    public void getRecentHistory(int limit, @NonNull Callback<List<HistoryRecord>> callback) {
        mExecutor.execute(() -> {
            List<HistoryRecord> list = new ArrayList<>();
            try {
                SQLiteDatabase db = getReadableDatabase();
                Cursor cursor = db.query(TABLE_HISTORY, null, null, null, null, null,
                        COL_HISTORY_LAST_VISITED + " DESC", String.valueOf(limit));
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_HISTORY_ID));
                        String url = cursor.getString(cursor.getColumnIndexOrThrow(COL_HISTORY_URL));
                        String title = cursor.getString(cursor.getColumnIndexOrThrow(COL_HISTORY_TITLE));
                        int count = cursor.getInt(cursor.getColumnIndexOrThrow(COL_HISTORY_VISIT_COUNT));
                        long visitedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_HISTORY_LAST_VISITED));
                        list.add(new HistoryRecord(id, url, title, count, visitedAt));
                    }
                    cursor.close();
                }
            } catch (Exception ignored) {}

            mMainHandler.post(() -> callback.onComplete(list));
        });
    }

    // --- Search & Autocomplete Operations ---

    public void searchSuggestions(@NonNull String query, int limit, @NonNull Callback<List<SearchSuggestion>> callback) {
        mExecutor.execute(() -> {
            List<SearchSuggestion> suggestions = new ArrayList<>();
            if (query.trim().isEmpty()) {
                mMainHandler.post(() -> callback.onComplete(suggestions));
                return;
            }

            try {
                SQLiteDatabase db = getReadableDatabase();
                String wildcard = "%" + query.trim() + "%";

                // 1. Search Bookmarks
                Cursor bCursor = db.query(TABLE_BOOKMARKS,
                        new String[]{COL_BOOKMARK_TITLE, COL_BOOKMARK_URL},
                        COL_BOOKMARK_TITLE + " LIKE ? OR " + COL_BOOKMARK_URL + " LIKE ?",
                        new String[]{wildcard, wildcard}, null, null, COL_BOOKMARK_CREATED + " DESC", "3");
                if (bCursor != null) {
                    while (bCursor.moveToNext()) {
                        String title = bCursor.getString(0);
                        String url = bCursor.getString(1);
                        suggestions.add(new SearchSuggestion(SearchSuggestion.Type.BOOKMARK, title, url, "Bookmark"));
                    }
                    bCursor.close();
                }

                // 2. Search History
                int remaining = Math.max(0, limit - suggestions.size());
                if (remaining > 0) {
                    Cursor hCursor = db.query(TABLE_HISTORY,
                            new String[]{COL_HISTORY_TITLE, COL_HISTORY_URL},
                            COL_HISTORY_TITLE + " LIKE ? OR " + COL_HISTORY_URL + " LIKE ?",
                            new String[]{wildcard, wildcard}, null, null,
                            COL_HISTORY_LAST_VISITED + " DESC", String.valueOf(remaining));
                    if (hCursor != null) {
                        while (hCursor.moveToNext()) {
                            String title = hCursor.getString(0);
                            String url = hCursor.getString(1);
                            suggestions.add(new SearchSuggestion(SearchSuggestion.Type.HISTORY, title, url, "History"));
                        }
                        hCursor.close();
                    }
                }
            } catch (Exception ignored) {}

            mMainHandler.post(() -> callback.onComplete(suggestions));
        });
    }

    public void clearAllBrowsingData(@Nullable Runnable onFinished) {
        mExecutor.execute(() -> {
            try {
                SQLiteDatabase db = getWritableDatabase();
                db.delete(TABLE_HISTORY, null, null);
                db.delete(TABLE_BOOKMARKS, null, null);
            } catch (Exception ignored) {}

            if (onFinished != null) {
                mMainHandler.post(onFinished);
            }
        });
    }
}
