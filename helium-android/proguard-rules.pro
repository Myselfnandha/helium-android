# Helium Android ProGuard & R8 Optimization Rules

# Keep native JNI bindings for Helium Private Vault
-keepclasseswithmembernames class * {
    native <methods>;
}

-keep class net.imput.helium.sync.VaultClient {
    *;
}

# Keep AndroidX Preference controllers and reflection constructors
-keep public class * extends androidx.preference.Preference {
    public <init>(android.content.Context, android.util.AttributeSet, int, int);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context);
}

-keep class androidx.preference.PreferenceFragmentCompat {
    *;
}

-keep class net.imput.helium.settings.HeliumSettingsFragment {
    *;
}

# Keep Onboarding and Sync entrypoints
-keep class net.imput.helium.onboarding.HeliumFirstRunActivity {
    *;
}

-keep class net.imput.helium.sync.VaultSyncService {
    *;
}
