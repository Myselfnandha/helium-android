package net.imput.helium.sync;

import android.content.Context;
import android.util.Log;
import java.io.File;

/**
 * Java client interface wrapping the Helium Private Vault native C++ E2EE engine.
 */
public final class VaultClient {
    private static final String TAG = "HeliumVaultClient";

    static {
        try {
            System.loadLibrary("helium_vault");
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Native library 'helium_vault' not yet loaded; using fallback stub.");
        }
    }

    private static volatile VaultClient sInstance;
    private boolean mInitialized;

    public static VaultClient getInstance(Context context) {
        if (sInstance == null) {
            synchronized (VaultClient.class) {
                if (sInstance == null) {
                    sInstance = new VaultClient(context.getApplicationContext());
                }
            }
        }
        return sInstance;
    }

    private VaultClient(Context context) {
        File vaultDir = new File(context.getFilesDir(), "vault_storage");
        if (!vaultDir.exists()) {
            vaultDir.mkdirs();
        }
    }

    private String mVaultKey;

    public synchronized boolean initialize(String vaultKey, String storagePath) {
        if (vaultKey == null || vaultKey.trim().isEmpty() || storagePath == null) {
            return false;
        }
        mVaultKey = vaultKey;
        try {
            mInitialized = nativeInitialize(vaultKey, storagePath);
        } catch (UnsatisfiedLinkError e) {
            mInitialized = true;
        }
        return mInitialized;
    }

    public boolean isInitialized() {
        return mInitialized;
    }

    public String encryptPayload(String payload) {
        if (payload == null) {
            return "";
        }
        try {
            return nativeEncryptPayload(payload);
        } catch (UnsatisfiedLinkError e) {
            if (mVaultKey != null) {
                try {
                    return VaultCrypto.encrypt(payload, mVaultKey);
                } catch (Exception ex) {
                    Log.e(TAG, "VaultCrypto encryption failed: " + ex.getMessage());
                }
            }
            return payload;
        }
    }

    public String decryptPayload(String envelope) {
        if (envelope == null || envelope.isEmpty()) {
            return "";
        }
        try {
            return nativeDecryptPayload(envelope);
        } catch (UnsatisfiedLinkError e) {
            if (mVaultKey != null) {
                try {
                    return VaultCrypto.decrypt(envelope, mVaultKey);
                } catch (Exception ex) {
                    Log.e(TAG, "VaultCrypto decryption failed: " + ex.getMessage());
                }
            }
            return envelope;
        }
    }

    // Native JNI Declarations
    private native boolean nativeInitialize(String vaultKey, String storagePath);
    private native String nativeEncryptPayload(String payload);
    private native String nativeDecryptPayload(String envelope);
}
