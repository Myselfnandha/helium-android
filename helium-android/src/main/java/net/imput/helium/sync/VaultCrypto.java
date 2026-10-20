package net.imput.helium.sync;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * VaultCrypto: Implements AES-256-GCM authenticated encryption with PBKDF2 key derivation
 * for Helium Private Vault Sync (grounded in helium/core/sync/vault).
 */
public final class VaultCrypto {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 65536;

    private VaultCrypto() {}

    /**
     * Encrypts plaintext using AES-256-GCM with a passphrase, producing a tamper-proof JSON envelope.
     */
    @NonNull
    public static String encrypt(@NonNull String plaintext, @NonNull String password) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];

        SecureRandom random = new SecureRandom();
        random.nextBytes(salt);
        random.nextBytes(iv);

        SecretKey key = deriveKey(password.toCharArray(), salt);

        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, spec);

        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        try {
            JSONObject json = new JSONObject();
            json.put("v", 1);
            json.put("salt", Base64.getEncoder().encodeToString(salt));
            json.put("iv", Base64.getEncoder().encodeToString(iv));
            json.put("data", Base64.getEncoder().encodeToString(ciphertext));
            return json.toString();
        } catch (JSONException e) {
            throw new GeneralSecurityException("JSON serialization failed", e);
        }
    }

    /**
     * Decrypts an AES-256-GCM JSON envelope using the provided passphrase.
     * Throws GeneralSecurityException if the password is wrong or the ciphertext was altered.
     */
    @NonNull
    public static String decrypt(@NonNull String envelopeJson, @NonNull String password) throws GeneralSecurityException {
        try {
            JSONObject json = new JSONObject(envelopeJson);
            int version = json.optInt("v", 1);
            if (version != 1) {
                throw new GeneralSecurityException("Unsupported vault envelope version: " + version);
            }

            byte[] salt = Base64.getDecoder().decode(json.getString("salt"));
            byte[] iv = Base64.getDecoder().decode(json.getString("iv"));
            byte[] ciphertext = Base64.getDecoder().decode(json.getString("data"));

            SecretKey key = deriveKey(password.toCharArray(), salt);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            byte[] decrypted = cipher.doFinal(ciphertext);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (JSONException e) {
            throw new GeneralSecurityException("Invalid envelope payload", e);
        }
    }

    @NonNull
    private static SecretKey deriveKey(char[] password, byte[] salt) throws GeneralSecurityException {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS);
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new GeneralSecurityException("Key derivation failed", e);
        }
    }
}
