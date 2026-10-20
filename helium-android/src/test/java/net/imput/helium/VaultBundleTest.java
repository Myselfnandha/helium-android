package net.imput.helium;

import static org.junit.Assert.*;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import net.imput.helium.database.BookmarkRecord;
import net.imput.helium.database.HistoryRecord;
import net.imput.helium.sync.VaultBundle;
import net.imput.helium.sync.VaultSyncEngine;
import org.junit.Test;

public class VaultBundleTest {

    @Test
    public void testBundleJsonRoundtrip() throws Exception {
        List<BookmarkRecord> bookmarks = new ArrayList<>();
        bookmarks.add(new BookmarkRecord(1, "https://imput.net", "Imput Systems", 1000L));
        bookmarks.add(new BookmarkRecord(2, "https://github.com/imputnet/helium", "Helium GitHub", 2000L));

        List<HistoryRecord> history = new ArrayList<>();
        history.add(new HistoryRecord(1, "https://duckduckgo.com", "DuckDuckGo", 5, 3000L));

        VaultBundle original = new VaultBundle(123456L, "Helium-Test", bookmarks, history);
        String json = original.toJson();

        assertNotNull(json);
        assertTrue(json.contains("imput.net"));
        assertTrue(json.contains("duckduckgo.com"));

        VaultBundle restored = VaultBundle.fromJson(json);
        assertEquals(2, restored.getBookmarks().size());
        assertEquals("https://imput.net", restored.getBookmarks().get(0).getUrl());
        assertEquals("Imput Systems", restored.getBookmarks().get(0).getTitle());
        assertEquals(1, restored.getHistory().size());
        assertEquals("https://duckduckgo.com", restored.getHistory().get(0).getUrl());
        assertEquals(5, restored.getHistory().get(0).getVisitCount());
    }

    @Test
    public void testEncryptedBundleRoundtrip() throws Exception {
        List<BookmarkRecord> bookmarks = new ArrayList<>();
        bookmarks.add(new BookmarkRecord(1, "https://helium.imput.net", "Helium Browser", 5000L));

        List<HistoryRecord> history = new ArrayList<>();
        history.add(new HistoryRecord(1, "https://news.ycombinator.com", "Hacker News", 2, 6000L));

        VaultBundle bundle = new VaultBundle(bookmarks, history);
        String password = "CorrectHorseBatteryStaple!";

        String encryptedPayload = bundle.encryptBundle(password);
        assertNotNull(encryptedPayload);
        assertTrue(encryptedPayload.startsWith(VaultBundle.VAULT_PREFIX));
        assertFalse(encryptedPayload.contains("helium.imput.net")); // Zero leakage in ciphertext

        assertTrue(VaultSyncEngine.isValidVaultPayload(encryptedPayload));

        VaultBundle decrypted = VaultBundle.decryptBundle(encryptedPayload, password);
        assertEquals(1, decrypted.getBookmarks().size());
        assertEquals("https://helium.imput.net", decrypted.getBookmarks().get(0).getUrl());
        assertEquals("Helium Browser", decrypted.getBookmarks().get(0).getTitle());
        assertEquals(1, decrypted.getHistory().size());
        assertEquals("https://news.ycombinator.com", decrypted.getHistory().get(0).getUrl());
    }

    @Test(expected = GeneralSecurityException.class)
    public void testDecryptionFailsWithWrongPassword() throws Exception {
        List<BookmarkRecord> bookmarks = new ArrayList<>();
        bookmarks.add(new BookmarkRecord(1, "https://secret.local", "Private Bookmark", 100L));
        VaultBundle bundle = new VaultBundle(bookmarks, new ArrayList<>());

        String encryptedPayload = bundle.encryptBundle("SuperSecretKey123");
        VaultBundle.decryptBundle(encryptedPayload, "WrongPassword");
    }

    @Test(expected = GeneralSecurityException.class)
    public void testTamperedCiphertextRejected() throws Exception {
        List<BookmarkRecord> bookmarks = new ArrayList<>();
        bookmarks.add(new BookmarkRecord(1, "https://tamper.test", "Tamper Test", 100L));
        VaultBundle bundle = new VaultBundle(bookmarks, new ArrayList<>());

        String payload = bundle.encryptBundle("KeyPassphrase");
        String rawJson = payload.substring(VaultBundle.VAULT_PREFIX.length());

        // Alter single byte in data
        String tamperedJson = rawJson.replace("\"data\":\"", "\"data\":\"X");
        VaultBundle.decryptBundle(VaultBundle.VAULT_PREFIX + tamperedJson, "KeyPassphrase");
    }

    @Test
    public void testPayloadValidation() {
        assertFalse(VaultSyncEngine.isValidVaultPayload(null));
        assertFalse(VaultSyncEngine.isValidVaultPayload(""));
        assertFalse(VaultSyncEngine.isValidVaultPayload("random garbage"));
        assertTrue(VaultSyncEngine.isValidVaultPayload("helium-vault:v1:{\"v\":1}"));
        assertTrue(VaultSyncEngine.isValidVaultPayload("{\"v\":1,\"salt\":\"abc\",\"data\":\"xyz\"}"));
    }
}
