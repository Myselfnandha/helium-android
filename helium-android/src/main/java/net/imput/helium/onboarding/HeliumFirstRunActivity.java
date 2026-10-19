package net.imput.helium.onboarding;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import net.imput.helium.R;
import net.imput.helium.preferences.HeliumPreferences;

/**
 * Helium Browser First-Run Setup Wizard.
 * Allows users to configure Extensions, Adblock, JIT protection, Vault Sync, and Search Engine on startup.
 */
public class HeliumFirstRunActivity extends AppCompatActivity {

    private SwitchCompat mSwitchExtensions;
    private SwitchCompat mSwitchAdblock;
    private SwitchCompat mSwitchJit;
    private SwitchCompat mSwitchSync;
    private RadioGroup mRgSearchEngines;
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
        mRgSearchEngines = findViewById(R.id.rg_search_engines);
        mBtnStartBrowsing = findViewById(R.id.btn_start_browsing);

        if (savedInstanceState == null) {
            // Initialize default switch states on initial creation
            mSwitchExtensions.setChecked(mPreferences.isExtensionsEnabled());
            mSwitchAdblock.setChecked(mPreferences.isNativeAdblockEnabled());
            mSwitchJit.setChecked(mPreferences.isJitProtectionEnabled());
            mSwitchSync.setChecked(mPreferences.isVaultSyncEnabled());

            String currentEngine = mPreferences.getDefaultSearchEngine();
            if (HeliumPreferences.SEARCH_ENGINE_BRAVE.equals(currentEngine)) {
                mRgSearchEngines.check(R.id.rb_brave);
            } else if (HeliumPreferences.SEARCH_ENGINE_SEARXNG.equals(currentEngine)) {
                mRgSearchEngines.check(R.id.rb_searxng);
            } else if (HeliumPreferences.SEARCH_ENGINE_STARTPAGE.equals(currentEngine)) {
                mRgSearchEngines.check(R.id.rb_startpage);
            } else {
                mRgSearchEngines.check(R.id.rb_duckduckgo);
            }
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

        int selectedId = mRgSearchEngines.getCheckedRadioButtonId();
        if (selectedId == R.id.rb_brave) {
            mPreferences.setDefaultSearchEngine(HeliumPreferences.SEARCH_ENGINE_BRAVE);
        } else if (selectedId == R.id.rb_searxng) {
            mPreferences.setDefaultSearchEngine(HeliumPreferences.SEARCH_ENGINE_SEARXNG);
        } else if (selectedId == R.id.rb_startpage) {
            mPreferences.setDefaultSearchEngine(HeliumPreferences.SEARCH_ENGINE_STARTPAGE);
        } else {
            mPreferences.setDefaultSearchEngine(HeliumPreferences.SEARCH_ENGINE_DUCKDUCKGO);
        }

        mPreferences.setFirstRunCompleted(true);
        launchBrowserAndFinish();
    }

    private void launchBrowserAndFinish() {
        if (isTaskRoot()) {
            Intent intent = new Intent(this, net.imput.helium.browser.HeliumMainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        // Return OK to parent and finish
        setResult(Activity.RESULT_OK);
        finish();
    }
}
