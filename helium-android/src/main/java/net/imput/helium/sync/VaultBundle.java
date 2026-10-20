package net.imput.helium.sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import net.imput.helium.database.BookmarkRecord;
import net.imput.helium.database.HistoryRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * VaultBundle: Serializable data bundle containing encrypted bookmarks and history
 * for Helium Private Vault Zero-Knowledge synchronization and QR pairing.
 */
public final class VaultBundle {

    public static final String VAULT_PREFIX = "helium-vault:v1:";
    private static final String KEY_VERSION = "version";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_SOURCE = "source";
    private static final String KEY_BOOKMARKS = "bookmarks";
    private static final String KEY_HISTORY = "history";

    private final long mTimestamp;
    private final String mSource;
    private final List<BookmarkRecord> mBookmarks;
    private final List<HistoryRecord> mHistory;

    public VaultBundle(@NonNull List<BookmarkRecord> bookmarks, @NonNull List<HistoryRecord> history) {
        this(System.currentTimeMillis(), "Helium-Android", bookmarks, history);
    }

    public VaultBundle(long timestamp, @NonNull String source,
                       @NonNull List<BookmarkRecord> bookmarks,
                       @NonNull List<HistoryRecord> history) {
        mTimestamp = timestamp;
        mSource = source;
        mBookmarks = new ArrayList<>(bookmarks);
        mHistory = new ArrayList<>(history);
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    @NonNull
    public String getSource() {
        return mSource;
    }

    @NonNull
    public List<BookmarkRecord> getBookmarks() {
        return mBookmarks;
    }

    @NonNull
    public List<HistoryRecord> getHistory() {
        return mHistory;
    }

    @NonNull
    public String toJson() throws JSONException {
        JSONObject root = new JSONObject();
        root.put(KEY_VERSION, 1);
        root.put(KEY_TIMESTAMP, mTimestamp);
        root.put(KEY_SOURCE, mSource);

        JSONArray bArray = new JSONArray();
        for (BookmarkRecord b : mBookmarks) {
            JSONObject bObj = new JSONObject();
            bObj.put("url", b.getUrl());
            bObj.put("title", b.getTitle());
            bObj.put("createdAt", b.getCreatedAt());
            bArray.put(bObj);
        }
        root.put(KEY_BOOKMARKS, bArray);

        JSONArray hArray = new JSONArray();
        for (HistoryRecord h : mHistory) {
            JSONObject hObj = new JSONObject();
            hObj.put("url", h.getUrl());
            hObj.put("title", h.getTitle());
            hObj.put("visitCount", h.getVisitCount());
            hObj.put("lastVisitedAt", h.getLastVisitedAt());
            hArray.put(hObj);
        }
        root.put(KEY_HISTORY, hArray);

        return root.toString();
    }

    @NonNull
    public static VaultBundle fromJson(@NonNull String jsonString) throws JSONException {
        JSONObject root = new JSONObject(jsonString);
        long timestamp = root.optLong(KEY_TIMESTAMP, System.currentTimeMillis());
        String source = root.optString(KEY_SOURCE, "Unknown");

        List<BookmarkRecord> bookmarks = new ArrayList<>();
        JSONArray bArray = root.optJSONArray(KEY_BOOKMARKS);
        if (bArray != null) {
            for (int i = 0; i < bArray.length(); i++) {
                JSONObject bObj = bArray.getJSONObject(i);
                String url = bObj.optString("url");
                String title = bObj.optString("title", url);
                long createdAt = bObj.optLong("createdAt", System.currentTimeMillis());
                if (!url.isEmpty()) {
                    bookmarks.add(new BookmarkRecord(i + 1, url, title, createdAt));
                }
            }
        }

        List<HistoryRecord> history = new ArrayList<>();
        JSONArray hArray = root.optJSONArray(KEY_HISTORY);
        if (hArray != null) {
            for (int i = 0; i < hArray.length(); i++) {
                JSONObject hObj = hArray.getJSONObject(i);
                String url = hObj.optString("url");
                String title = hObj.optString("title", url);
                int count = hObj.optInt("visitCount", 1);
                long visitedAt = hObj.optLong("lastVisitedAt", System.currentTimeMillis());
                if (!url.isEmpty()) {
                    history.add(new HistoryRecord(i + 1, url, title, count, visitedAt));
                }
            }
        }

        return new VaultBundle(timestamp, source, bookmarks, history);
    }

    /**
     * Encrypts the entire bundle using AES-256-GCM + PBKDF2 with user passphrase.
     * Returns a prefixed vault payload string.
     */
    @NonNull
    public String encryptBundle(@NonNull String password) throws GeneralSecurityException, JSONException {
        String json = toJson();
        String encryptedEnvelope = VaultCrypto.encrypt(json, password);
        return VAULT_PREFIX + encryptedEnvelope;
    }

    /**
     * Decrypts a prefixed or raw encrypted vault envelope and reconstructs the VaultBundle.
     */
    @NonNull
    public static VaultBundle decryptBundle(@NonNull String payload, @NonNull String password)
            throws GeneralSecurityException, JSONException {
        String cleaned = payload.trim();
        if (cleaned.startsWith(VAULT_PREFIX)) {
            cleaned = cleaned.substring(VAULT_PREFIX.length()).trim();
        }

        String decryptedJson = VaultCrypto.decrypt(cleaned, password);
        return fromJson(decryptedJson);
    }
}
