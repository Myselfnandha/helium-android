package net.imput.helium.sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import net.imput.helium.database.BookmarkRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * VaultBundle: State delta payload container for Helium bookmarks, settings, and sync objects.
 */
public final class VaultBundle {

    private final long mTimestamp;
    private final List<BookmarkRecord> mBookmarks;
    private final String mSearchEngine;
    private final boolean mAdblockEnabled;
    private final boolean mHttpsOnlyEnabled;

    public VaultBundle(long timestamp,
                       @NonNull List<BookmarkRecord> bookmarks,
                       @NonNull String searchEngine,
                       boolean adblockEnabled,
                       boolean httpsOnlyEnabled) {
        mTimestamp = timestamp;
        mBookmarks = new ArrayList<>(bookmarks);
        mSearchEngine = searchEngine;
        mAdblockEnabled = adblockEnabled;
        mHttpsOnlyEnabled = httpsOnlyEnabled;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    @NonNull
    public List<BookmarkRecord> getBookmarks() {
        return mBookmarks;
    }

    @NonNull
    public String getSearchEngine() {
        return mSearchEngine;
    }

    public boolean isAdblockEnabled() {
        return mAdblockEnabled;
    }

    public boolean isHttpsOnlyEnabled() {
        return mHttpsOnlyEnabled;
    }

    @NonNull
    public String toPlainJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put("timestamp", mTimestamp);
        root.put("search_engine", mSearchEngine);
        root.put("adblock_enabled", mAdblockEnabled);
        root.put("https_only_enabled", mHttpsOnlyEnabled);

        JSONArray bArray = new JSONArray();
        for (BookmarkRecord b : mBookmarks) {
            JSONObject bObj = new JSONObject();
            bObj.put("id", b.getId());
            bObj.put("url", b.getUrl());
            bObj.put("title", b.getTitle());
            bObj.put("created_at", b.getCreatedAt());
            bArray.put(bObj);
        }
        root.put("bookmarks", bArray);

        return root.toString();
    }

    @NonNull
    public static VaultBundle fromPlainJson(@NonNull String jsonString) throws JSONException {
        JSONObject root = new JSONObject(jsonString);
        long ts = root.optLong("timestamp", System.currentTimeMillis());
        String engine = root.optString("search_engine", "duckduckgo");
        boolean adblock = root.optBoolean("adblock_enabled", true);
        boolean httpsOnly = root.optBoolean("https_only_enabled", true);

        List<BookmarkRecord> bookmarks = new ArrayList<>();
        JSONArray bArray = root.optJSONArray("bookmarks");
        if (bArray != null) {
            for (int i = 0; i < bArray.length(); i++) {
                JSONObject bObj = bArray.getJSONObject(i);
                bookmarks.add(new BookmarkRecord(
                    bObj.optLong("id", i + 1),
                    bObj.getString("url"),
                    bObj.optString("title", ""),
                    bObj.optLong("created_at", System.currentTimeMillis())
                ));
            }
        }
        return new VaultBundle(ts, bookmarks, engine, adblock, httpsOnly);
    }

    @NonNull
    public String encryptBundle(@NonNull String password) throws GeneralSecurityException, JSONException {
        return VaultCrypto.encrypt(toPlainJson(), password);
    }

    @NonNull
    public static VaultBundle decryptBundle(@NonNull String encryptedEnvelope, @NonNull String password)
            throws GeneralSecurityException, JSONException {
        String plain = VaultCrypto.decrypt(encryptedEnvelope, password);
        return fromPlainJson(plain);
    }
}
