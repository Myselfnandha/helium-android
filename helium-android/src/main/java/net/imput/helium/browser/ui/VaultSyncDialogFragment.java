package net.imput.helium.browser.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import net.imput.helium.R;
import net.imput.helium.sync.VaultSyncEngine;

/**
 * VaultSyncDialogFragment: User interface for Helium Private Vault
 * Zero-Knowledge AES-256-GCM desktop-to-mobile synchronization and export/import.
 */
public class VaultSyncDialogFragment extends BottomSheetDialogFragment {

    private Button mBtnModeExport;
    private Button mBtnModeImport;
    private LinearLayout mLlExportContainer;
    private LinearLayout mLlImportContainer;

    // Export views
    private EditText mEtExportPassphrase;
    private Button mBtnGenerateVault;
    private LinearLayout mLlExportResult;
    private TextView mTvExportSummary;
    private EditText mTvVaultPayload;
    private Button mBtnCopyVault;
    private Button mBtnShareVault;

    // Import views
    private EditText mEtImportPayload;
    private EditText mEtImportPassphrase;
    private Button mBtnImportVault;
    private TextView mTvImportStatus;

    @NonNull
    public static VaultSyncDialogFragment newInstance() {
        return new VaultSyncDialogFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_vault_sync, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBtnModeExport = view.findViewById(R.id.btn_mode_export);
        mBtnModeImport = view.findViewById(R.id.btn_mode_import);
        mLlExportContainer = view.findViewById(R.id.ll_export_container);
        mLlImportContainer = view.findViewById(R.id.ll_import_container);

        mEtExportPassphrase = view.findViewById(R.id.et_export_passphrase);
        mBtnGenerateVault = view.findViewById(R.id.btn_generate_vault);
        mLlExportResult = view.findViewById(R.id.ll_export_result);
        mTvExportSummary = view.findViewById(R.id.tv_export_summary);
        mTvVaultPayload = view.findViewById(R.id.tv_vault_payload);
        mBtnCopyVault = view.findViewById(R.id.btn_copy_vault);
        mBtnShareVault = view.findViewById(R.id.btn_share_vault);

        mEtImportPayload = view.findViewById(R.id.et_import_payload);
        mEtImportPassphrase = view.findViewById(R.id.et_import_passphrase);
        mBtnImportVault = view.findViewById(R.id.btn_import_vault);
        mTvImportStatus = view.findViewById(R.id.tv_import_status);

        setupModeSwitch();
        setupExportActions();
        setupImportActions();
    }

    private void setupModeSwitch() {
        mBtnModeExport.setOnClickListener(v -> {
            mLlExportContainer.setVisibility(View.VISIBLE);
            mLlImportContainer.setVisibility(View.GONE);
            mBtnModeExport.setTextColor(ContextCompat.getColor(requireContext(), R.color.helium_accent_cyan));
            mBtnModeImport.setTextColor(ContextCompat.getColor(requireContext(), R.color.helium_text_secondary));
        });

        mBtnModeImport.setOnClickListener(v -> {
            mLlExportContainer.setVisibility(View.GONE);
            mLlImportContainer.setVisibility(View.VISIBLE);
            mBtnModeImport.setTextColor(ContextCompat.getColor(requireContext(), R.color.helium_accent_cyan));
            mBtnModeExport.setTextColor(ContextCompat.getColor(requireContext(), R.color.helium_text_secondary));
        });
    }

    private void setupExportActions() {
        mBtnGenerateVault.setOnClickListener(v -> {
            String passphrase = mEtExportPassphrase.getText().toString().trim();
            if (passphrase.isEmpty()) {
                mEtExportPassphrase.setError("Passphrase cannot be empty");
                return;
            }

            mBtnGenerateVault.setEnabled(false);
            mBtnGenerateVault.setText("Encrypting with AES-256-GCM...");

            VaultSyncEngine.exportVault(requireContext(), passphrase, new VaultSyncEngine.ExportCallback() {
                @Override
                public void onSuccess(@NonNull String encryptedVaultPayload, int bookmarkCount, int historyCount) {
                    if (!isAdded()) return;
                    mBtnGenerateVault.setEnabled(true);
                    mBtnGenerateVault.setText("Encrypt & Generate Vault Bundle");

                    mLlExportResult.setVisibility(View.VISIBLE);
                    mTvExportSummary.setText(bookmarkCount + " Bookmarks, " + historyCount + " History items encrypted");
                    mTvVaultPayload.setText(encryptedVaultPayload);
                }

                @Override
                public void onError(@NonNull Exception exception) {
                    if (!isAdded()) return;
                    mBtnGenerateVault.setEnabled(true);
                    mBtnGenerateVault.setText("Encrypt & Generate Vault Bundle");
                    Toast.makeText(requireContext(), "Encryption failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        mBtnCopyVault.setOnClickListener(v -> {
            String payload = mTvVaultPayload.getText().toString();
            if (!payload.isEmpty()) {
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Helium Vault", payload);
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), "Encrypted payload copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBtnShareVault.setOnClickListener(v -> {
            String payload = mTvVaultPayload.getText().toString();
            if (!payload.isEmpty()) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Helium Encrypted Vault Bundle");
                shareIntent.putExtra(Intent.EXTRA_TEXT, payload);
                startActivity(Intent.createChooser(shareIntent, "Share Encrypted Vault"));
            }
        });
    }

    private void setupImportActions() {
        mBtnImportVault.setOnClickListener(v -> {
            String payload = mEtImportPayload.getText().toString().trim();
            String passphrase = mEtImportPassphrase.getText().toString().trim();

            if (payload.isEmpty()) {
                mEtImportPayload.setError("Please paste the vault payload");
                return;
            }
            if (passphrase.isEmpty()) {
                mEtImportPassphrase.setError("Enter decryption passphrase");
                return;
            }

            mBtnImportVault.setEnabled(false);
            mBtnImportVault.setText("Decrypting & Merging...");
            mTvImportStatus.setVisibility(View.GONE);

            VaultSyncEngine.importVault(requireContext(), payload, passphrase, new VaultSyncEngine.ImportCallback() {
                @Override
                public void onSuccess(int bookmarksImported, int historyImported) {
                    if (!isAdded()) return;
                    mBtnImportVault.setEnabled(true);
                    mBtnImportVault.setText("Decrypt & Merge into Browser");

                    mTvImportStatus.setVisibility(View.VISIBLE);
                    mTvImportStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.helium_shield_active));
                    mTvImportStatus.setText("Successfully imported " + bookmarksImported + " bookmarks & " + historyImported + " history items!");
                    Toast.makeText(requireContext(), "Vault merge complete!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(@NonNull Exception exception) {
                    if (!isAdded()) return;
                    mBtnImportVault.setEnabled(true);
                    mBtnImportVault.setText("Decrypt & Merge into Browser");

                    mTvImportStatus.setVisibility(View.VISIBLE);
                    mTvImportStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.helium_warning_amber));
                    mTvImportStatus.setText("Import failed: " + exception.getMessage());
                }
            });
        });
    }
}
