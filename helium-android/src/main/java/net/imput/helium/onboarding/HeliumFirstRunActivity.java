package net.imput.helium.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import net.imput.helium.R;
import net.imput.helium.preferences.HeliumPreferences;

/**
 * Helium Browser First-Run Setup Wizard.
 * Allows users to configure Extensions, Adblock, JIT protection, and Vault Sync on startup.
 */
public class HeliumFirstRunActivity extends AppCompatActivity {

    private SwitchCompat mSwitchExtensions;
    private SwitchCompat mSwitchAdblock;
    private SwitchCompat mSwitchJit;
    private SwitchCompat mSwitchSync;
    private Button mBtnStartBrowsing;
    private HeliumPreferences mPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = HeliumPreferences.getInstance(this);

        // If onboarding was already completed, finish and proceed directly
        if (mPreferences.isFirstRunCompleted()) {
            launchBrowserAndFinish();
            return;
        }

        setContentView(R.layout.activity_helium_first_run);

        mSwitchExtensions = findViewById(R.id.switch_extensions);
        mSwitchAdblock = findViewById(R.id.switch_adblock);
        mSwitchJit = findViewById(R.id.switch_jit);
        mSwitchSync = findViewById(R.id.switch_sync);
        mBtnStartBrowsing = findViewById(R.id.btn_start_browsing);

        if (savedInstanceState == null) {
            // Initialize default switch states on initial creation
            mSwitchExtensions.setChecked(mPreferences.isExtensionsEnabled());
            mSwitchAdblock.setChecked(mPreferences.isNativeAdblockEnabled());
            mSwitchJit.setChecked(mPreferences.isJitProtectionEnabled());
            mSwitchSync.setChecked(mPreferences.isVaultSyncEnabled());
        }

        mBtnStartBrowsing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePreferencesAndProceed();
            }
        });
    }

    private void savePreferencesAndProceed() {
        mPreferences.setExtensionsEnabled(mSwitchExtensions.isChecked());
        mPreferences.setNativeAdblockEnabled(mSwitchAdblock.isChecked());
        mPreferences.setJitProtectionEnabled(mSwitchJit.isChecked());
        mPreferences.setVaultSyncEnabled(mSwitchSync.isChecked());
        mPreferences.setFirstRunCompleted(true);

        launchBrowserAndFinish();
    }

    private void launchBrowserAndFinish() {
        // Return OK to Chromium's FirstRunFlowSequencer and finish
        setResult(Activity.RESULT_OK);
        finish();
    }
}
