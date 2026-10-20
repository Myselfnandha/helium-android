package net.imput.helium.widget;

import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.SeekBar;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import net.imput.helium.R;

/**
 * SearchWidgetConfigureActivity: Configuration screen shown when adding a search widget
 * to customize corner radius, visual style, and enabled shortcuts.
 */
public class SearchWidgetConfigureActivity extends AppCompatActivity {

    private int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private SeekBar mSeekBarRadius;
    private CheckBox mCbVoice;
    private CheckBox mCbBookmarks;
    private CheckBox mCbQr;
    private Button mBtnSave;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setResult(RESULT_CANCELED);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        setContentView(R.layout.activity_widget_configure);

        mSeekBarRadius = findViewById(R.id.seekbar_corner_radius);
        mCbVoice = findViewById(R.id.cb_shortcut_voice);
        mCbBookmarks = findViewById(R.id.cb_shortcut_bookmarks);
        mCbQr = findViewById(R.id.cb_shortcut_qr);
        mBtnSave = findViewById(R.id.btn_save_widget_config);

        // Load existing or defaults
        int radius = WidgetPreferencesStore.getCornerRadius(this, mAppWidgetId);
        mSeekBarRadius.setProgress(radius);
        mCbVoice.setChecked(WidgetPreferencesStore.isVoiceEnabled(this, mAppWidgetId));
        mCbBookmarks.setChecked(WidgetPreferencesStore.isBookmarksEnabled(this, mAppWidgetId));
        mCbQr.setChecked(WidgetPreferencesStore.isQrEnabled(this, mAppWidgetId));

        mBtnSave.setOnClickListener(v -> saveAndFinish());
    }

    private void saveAndFinish() {
        int radius = mSeekBarRadius.getProgress();
        boolean voice = mCbVoice.isChecked();
        boolean bookmarks = mCbBookmarks.isChecked();
        boolean qr = mCbQr.isChecked();

        WidgetPreferencesStore.setCornerRadius(this, mAppWidgetId, radius);
        WidgetPreferencesStore.setVoiceEnabled(this, mAppWidgetId, voice);
        WidgetPreferencesStore.setBookmarksEnabled(this, mAppWidgetId, bookmarks);
        WidgetPreferencesStore.setQrEnabled(this, mAppWidgetId, qr);

        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        SearchWidgetProvider.updateAppWidget(this, appWidgetManager, mAppWidgetId);

        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        finish();
    }
}
