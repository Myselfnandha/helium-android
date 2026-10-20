package net.imput.helium.sync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.security.GeneralSecurityException;
import java.util.List;
import net.imput.helium.database.BookmarkRecord;
import net.imput.helium.database.HeliumDbHelper;
import net.imput.helium.database.HistoryRecord;
import org.json.JSONException;

/**
 * VaultSyncEngine: Deep module orchestrating end-to-end encrypted backup,
 * export, import, and QR pairing for bookmarks and history.
 */
public final class VaultSyncEngine {

    public interface ExportCallback {
        void onSuccess(@NonNull String encryptedVaultPayload, int bookmarkCount, int historyCount);
        void onError(@NonNull Exception exception);
    }

    public interface ImportCallback {
        void onSuccess(int bookmarksImported, int historyImported);
        void onError(@NonNull Exception exception);
    }

    private static final Handler sMainHandler = new Handler(Looper.getMainLooper());

    private VaultSyncEngine() {}

    /**
     * Checks if a string is a recognizable Helium encrypted vault payload.
     */
    public static boolean isValidVaultPayload(@Nullable String payload) {
        if (payload == null) return false;
        String trimmed = payload.trim();
        return trimmed.startsWith(VaultBundle.VAULT_PREFIX)
                || (trimmed.startsWith("{") && trimmed.contains("\"salt\"") && trimmed.contains("\"data\""));
    }

    /**
     * Asynchronously gathers bookmarks and history, packages them into a VaultBundle,
     * encrypts with AES-256-GCM + PBKDF2 using the supplied password, and delivers the payload.
     */
    public static void exportVault(@NonNull Context context, @NonNull String password, @NonNull ExportCallback callback) {
        if (password.trim().isEmpty()) {
            sMainHandler.post(() -> callback.onError(new IllegalArgumentException("Passphrase cannot be empty")));
            return;
        }

        HeliumDbHelper db = HeliumDbHelper.getInstance(context);
        db.getAllBookmarks(bookmarks -> {
            db.getRecentHistory(200, history -> {
                new Thread(() -> {
                    try {
                        VaultBundle bundle = new VaultBundle(bookmarks, history);
                        String encrypted = bundle.encryptBundle(password);
                        sMainHandler.post(() -> callback.onSuccess(encrypted, bookmarks.size(), history.size()));
                    } catch (GeneralSecurityException | JSONException e) {
                        sMainHandler.post(() -> callback.onError(e));
                    }
                }).start();
            });
        });
    }

    /**
     * Asynchronously decrypts an encrypted vault payload using the supplied password,
     * validates the bundle, and merges the bookmarks and history into Helium's local database.
     */
    public static void importVault(@NonNull Context context, @NonNull String payload,
                                   @NonNull String password, @NonNull ImportCallback callback) {
        if (password.trim().isEmpty()) {
            sMainHandler.post(() -> callback.onError(new IllegalArgumentException("Passphrase cannot be empty")));
            return;
        }

        if (!isValidVaultPayload(payload)) {
            sMainHandler.post(() -> callback.onError(new IllegalArgumentException("Invalid Helium Vault payload")));
            return;
        }

        new Thread(() -> {
            try {
                VaultBundle bundle = VaultBundle.decryptBundle(payload, password);
                HeliumDbHelper db = HeliumDbHelper.getInstance(context);

                List<BookmarkRecord> bookmarks = bundle.getBookmarks();
                List<HistoryRecord> history = bundle.getHistory();

                for (BookmarkRecord b : bookmarks) {
                    db.addBookmark(b.getUrl(), b.getTitle(), null);
                }

                for (HistoryRecord h : history) {
                    db.recordVisit(h.getUrl(), h.getTitle());
                }

                sMainHandler.post(() -> callback.onSuccess(bookmarks.size(), history.size()));
            } catch (GeneralSecurityException | JSONException e) {
                sMainHandler.post(() -> callback.onError(e));
            } catch (Exception e) {
                sMainHandler.post(() -> callback.onError(e));
            }
        }).start();
    }
}
