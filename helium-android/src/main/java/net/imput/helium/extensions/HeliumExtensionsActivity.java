package net.imput.helium.extensions;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import java.util.List;
import net.imput.helium.R;

/**
 * Native Android Activity for viewing, toggling, and sideloading Helium extensions.
 */
public class HeliumExtensionsActivity extends AppCompatActivity implements ExtensionManager.ExtensionStateListener {

    private static final int REQUEST_PICK_CRX = 1001;

    private ExtensionManager mExtensionManager;
    private LinearLayout mContainer;
    private TextView mEmptyState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_helium_extensions);

        mExtensionManager = ExtensionManager.getInstance(this);
        mContainer = findViewById(R.id.container_extensions);
        mEmptyState = findViewById(R.id.tv_empty_state);

        View btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }

        View btnSideload = findViewById(R.id.btn_sideload_crx);
        if (btnSideload != null) {
            btnSideload.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    launchCrxFilePicker();
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mExtensionManager.registerListener(this);
        renderExtensions(mExtensionManager.getInstalledExtensions());
    }

    @Override
    protected void onStop() {
        super.onStop();
        mExtensionManager.unregisterListener(this);
    }

    @Override
    public void onExtensionsUpdated(List<ExtensionItem> extensions) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                renderExtensions(extensions);
            }
        });
    }

    private void renderExtensions(List<ExtensionItem> extensions) {
        if (mContainer == null) return;

        // Clear existing cards but retain the empty state view
        mContainer.removeAllViews();
        if (mEmptyState != null) {
            mContainer.addView(mEmptyState);
        }

        if (extensions.isEmpty()) {
            if (mEmptyState != null) mEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        if (mEmptyState != null) {
            mEmptyState.setVisibility(View.GONE);
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        for (ExtensionItem item : extensions) {
            View card = inflater.inflate(R.layout.item_extension_card, mContainer, false);
            bindExtensionCard(card, item);
            mContainer.addView(card);
        }
    }

    private void bindExtensionCard(View card, final ExtensionItem item) {
        TextView tvName = card.findViewById(R.id.tv_ext_name);
        TextView tvVersion = card.findViewById(R.id.tv_ext_version);
        TextView tvDesc = card.findViewById(R.id.tv_ext_description);
        SwitchCompat switchEnabled = card.findViewById(R.id.switch_ext_enabled);
        Button btnOptions = card.findViewById(R.id.btn_ext_options);
        Button btnRemove = card.findViewById(R.id.btn_ext_remove);

        if (tvName != null) tvName.setText(item.getName());
        if (tvVersion != null) {
            tvVersion.setText(getString(R.string.extensions_version_format, item.getVersion()));
        }
        if (tvDesc != null) tvDesc.setText(item.getDescription());

        if (switchEnabled != null) {
            switchEnabled.setChecked(item.isEnabled());
            switchEnabled.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    mExtensionManager.setExtensionEnabled(item.getId(), isChecked);
                }
            });
        }

        if (btnOptions != null) {
            if (item.getOptionsUrl() != null && !item.getOptionsUrl().isEmpty()) {
                btnOptions.setVisibility(View.VISIBLE);
                btnOptions.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openExtensionOptions(item.getOptionsUrl());
                    }
                });
            } else {
                btnOptions.setVisibility(View.GONE);
            }
        }

        if (btnRemove != null) {
            if (!item.isBundled()) {
                btnRemove.setVisibility(View.VISIBLE);
                btnRemove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mExtensionManager.removeExtension(item.getId());
                    }
                });
            } else {
                btnRemove.setVisibility(View.GONE);
            }
        }
    }

    private void openExtensionOptions(String optionsUrl) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(optionsUrl));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Extension options: " + optionsUrl, Toast.LENGTH_SHORT).show();
        }
    }

    private void launchCrxFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "Select .crx extension package"), REQUEST_PICK_CRX);
        } catch (Exception e) {
            Toast.makeText(this, "File manager not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PICK_CRX && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                handleSideloadUri(uri);
            }
        }
    }

    private void handleSideloadUri(Uri uri) {
        String lastSegment = uri.getLastPathSegment();
        String name = (lastSegment != null && !lastSegment.isEmpty()) ? lastSegment : "Sideloaded Extension";
        String id = "sideload_" + Math.abs(uri.toString().hashCode());
        boolean installed = mExtensionManager.installFromCrx(id, name, "1.0.0", "Locally sideloaded extension package.");
        if (installed) {
            Toast.makeText(this, "Extension installed: " + name, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Extension already installed", Toast.LENGTH_SHORT).show();
        }
    }
}
