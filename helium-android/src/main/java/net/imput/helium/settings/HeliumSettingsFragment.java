package net.imput.helium.settings;

import android.content.Intent;
import android.os.Bundle;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;
import net.imput.helium.R;
import net.imput.helium.extensions.HeliumExtensionsActivity;
import net.imput.helium.preferences.HeliumPreferences;

/**
 * Dedicated Helium Browser Settings screen.
 * Hosts controls for native adblock, desktop extensions, JIT hardening, and Private Vault Sync.
 */
public class HeliumSettingsFragment extends PreferenceFragmentCompat {

    private HeliumPreferences mHeliumPrefs;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.helium_preferences, rootKey);

        if (getContext() != null) {
            mHeliumPrefs = HeliumPreferences.getInstance(getContext());
            bindPreferenceHandlers();
        }
    }

    private interface OnBooleanPreferenceChangeListener {
        void onChange(boolean newValue);
    }

    private void bindSwitch(String key, boolean currentValue, OnBooleanPreferenceChangeListener listener) {
        SwitchPreferenceCompat pref = findPreference(key);
        if (pref != null) {
            pref.setChecked(currentValue);
            pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if (newValue instanceof Boolean) {
                        listener.onChange((Boolean) newValue);
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private void bindPreferenceHandlers() {
        bindSwitch(HeliumPreferences.KEY_NATIVE_ADBLOCK_ENABLED, mHeliumPrefs.isNativeAdblockEnabled(),
                new OnBooleanPreferenceChangeListener() {
                    @Override
                    public void onChange(boolean val) { mHeliumPrefs.setNativeAdblockEnabled(val); }
                });

        bindSwitch(HeliumPreferences.KEY_URL_TRACKER_STRIPPER_ENABLED, mHeliumPrefs.isUrlTrackerStripperEnabled(),
                new OnBooleanPreferenceChangeListener() {
                    @Override
                    public void onChange(boolean val) { mHeliumPrefs.setUrlTrackerStripperEnabled(val); }
                });

        bindSwitch(HeliumPreferences.KEY_ANTI_FINGERPRINT_ENABLED, mHeliumPrefs.isAntiFingerprintEnabled(),
                new OnBooleanPreferenceChangeListener() {
                    @Override
                    public void onChange(boolean val) { mHeliumPrefs.setAntiFingerprintEnabled(val); }
                });

        bindSwitch(HeliumPreferences.KEY_HTTPS_ONLY_MODE_ENABLED, mHeliumPrefs.isHttpsOnlyModeEnabled(),
                new OnBooleanPreferenceChangeListener() {
                    @Override
                    public void onChange(boolean val) { mHeliumPrefs.setHttpsOnlyModeEnabled(val); }
                });

        bindSwitch(HeliumPreferences.KEY_SECURE_DNS_ENABLED, mHeliumPrefs.isSecureDnsEnabled(),
                new OnBooleanPreferenceChangeListener() {
                    @Override
                    public void onChange(boolean val) { mHeliumPrefs.setSecureDnsEnabled(val); }
                });

        bindSwitch(HeliumPreferences.KEY_BACKGROUND_MEDIA_ENABLED, mHeliumPrefs.isBackgroundMediaEnabled(),
                new OnBooleanPreferenceChangeListener() {
                    @Override
                    public void onChange(boolean val) { mHeliumPrefs.setBackgroundMediaEnabled(val); }
                });

        bindSwitch(HeliumPreferences.KEY_EXTENSIONS_ENABLED, mHeliumPrefs.isExtensionsEnabled(),
                new OnBooleanPreferenceChangeListener() {
                    @Override
                    public void onChange(boolean val) { mHeliumPrefs.setExtensionsEnabled(val); }
                });

        bindSwitch(HeliumPreferences.KEY_DARK_READER_ENABLED, mHeliumPrefs.isDarkReaderEnabled(),
                new OnBooleanPreferenceChangeListener() {
                    @Override
                    public void onChange(boolean val) { mHeliumPrefs.setDarkReaderEnabled(val); }
                });

        bindSwitch(HeliumPreferences.KEY_JIT_PROTECTION_ENABLED, mHeliumPrefs.isJitProtectionEnabled(),
                new OnBooleanPreferenceChangeListener() {
                    @Override
                    public void onChange(boolean val) { mHeliumPrefs.setJitProtectionEnabled(val); }
                });

        bindSwitch(HeliumPreferences.KEY_VAULT_SYNC_ENABLED, mHeliumPrefs.isVaultSyncEnabled(),
                new OnBooleanPreferenceChangeListener() {
                    @Override
                    public void onChange(boolean val) { mHeliumPrefs.setVaultSyncEnabled(val); }
                });

        Preference manageExtPref = findPreference("helium_manage_extensions");
        if (manageExtPref != null) {
            manageExtPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    if (getContext() != null) {
                        Intent intent = new Intent(getContext(), HeliumExtensionsActivity.class);
                        startActivity(intent);
                        return true;
                    }
                    return false;
                }
            });
        }
    }
}
