package net.imput.helium.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import net.imput.helium.preferences.HeliumPreferences;

/**
 * Background sync service managing periodic delta exchanges with the Helium Private Vault.
 */
public class VaultSyncService extends Service {
    private static final String TAG = "HeliumVaultSync";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        HeliumPreferences prefs = HeliumPreferences.getInstance(this);

        if (!prefs.isVaultSyncEnabled()) {
            Log.d(TAG, "Private Vault Sync is disabled by user preference. Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        performSyncCycle();
        return START_STICKY;
    }

    private void performSyncCycle() {
        VaultClient client = VaultClient.getInstance(this);
        Log.i(TAG, "Executing Helium Private Vault delta synchronization cycle...");
        // Exchanges commits, reconciles bookmarks & tabs without Google Play Services
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
