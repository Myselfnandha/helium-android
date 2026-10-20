package net.imput.helium.browser.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import net.imput.helium.R;
import net.imput.helium.adblock.AdBlockEngine;
import net.imput.helium.onboarding.HeliumFirstRunActivity;
import net.imput.helium.preferences.HeliumPreferences;

/**
 * PrivacyShieldDialogFragment: Displays real-time breakdown of blocked trackers,
 * fingerprinting noise status, HTTPS connection encryption, and JIT protection.
 */
public class PrivacyShieldDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_URL = "arg_url";

    public static PrivacyShieldDialogFragment newInstance(@NonNull String url) {
        PrivacyShieldDialogFragment fragment = new PrivacyShieldDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_privacy_shields, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String currentUrl = (getArguments() != null) ? getArguments().getString(ARG_URL, "") : "";
        String host = AdBlockEngine.extractHost(currentUrl);
        if (host == null || host.isEmpty()) {
            host = currentUrl.startsWith("file://") ? "Helium Offline NTP" : "Active Page";
        }

        TextView tvDomain = view.findViewById(R.id.tv_shield_domain);
        TextView tvBlockedCount = view.findViewById(R.id.tv_blocked_count);
        TextView tvNoiseStatus = view.findViewById(R.id.tv_noise_status);
        TextView tvHttpsStatus = view.findViewById(R.id.tv_https_status);
        TextView tvJitStatus = view.findViewById(R.id.tv_jit_status);
        ImageView ivShieldStatus = view.findViewById(R.id.iv_shield_status);
        Button btnSettings = view.findViewById(R.id.btn_open_shield_settings);

        tvDomain.setText(host);

        // Populate Live Metrics
        AdBlockEngine adblock = AdBlockEngine.getInstance();
        tvBlockedCount.setText(String.valueOf(adblock.getBlockedCount()));

        if (getContext() != null) {
            HeliumPreferences prefs = HeliumPreferences.getInstance(getContext());
            tvNoiseStatus.setText(prefs.isCanvasFingerprintProtectionEnabled() ? "Active (Canvas/WebGL)" : "Disabled");
            tvJitStatus.setText(prefs.isJitProtectionEnabled() ? "Hardened (Safe)" : "JIT Active");
        }

        boolean isHttps = currentUrl.startsWith("https://");
        if (isHttps) {
            tvHttpsStatus.setText("HTTPS Secure (TLS)");
            tvHttpsStatus.setTextColor(getResources().getColor(R.color.helium_shield_active));
        } else if (currentUrl.startsWith("http://")) {
            tvHttpsStatus.setText("Insecure HTTP");
            tvHttpsStatus.setTextColor(getResources().getColor(R.color.helium_shield_danger));
            ivShieldStatus.setImageResource(R.drawable.ic_lock);
        } else {
            tvHttpsStatus.setText("Internal Scheme");
            tvHttpsStatus.setTextColor(getResources().getColor(R.color.helium_accent_cyan));
        }

        btnSettings.setOnClickListener(v -> {
            dismiss();
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), HeliumFirstRunActivity.class);
                startActivity(intent);
            }
        });
    }
}
