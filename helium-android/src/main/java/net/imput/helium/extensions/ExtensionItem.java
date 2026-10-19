package net.imput.helium.extensions;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;

/**
 * Immutable model representing an installed browser extension.
 */
public final class ExtensionItem {
    @NonNull private final String mId;
    @NonNull private final String mName;
    @NonNull private final String mVersion;
    @NonNull private final String mDescription;
    private final boolean mIsEnabled;
    @Nullable private final String mOptionsUrl;
    private final boolean mIsBundled;
    @Nullable private final String mContentScript;

    public ExtensionItem(
            @NonNull String id,
            @NonNull String name,
            @NonNull String version,
            @NonNull String description,
            boolean isEnabled,
            @Nullable String optionsUrl,
            boolean isBundled) {
        this(id, name, version, description, isEnabled, optionsUrl, isBundled, null);
    }

    public ExtensionItem(
            @NonNull String id,
            @NonNull String name,
            @NonNull String version,
            @NonNull String description,
            boolean isEnabled,
            @Nullable String optionsUrl,
            boolean isBundled,
            @Nullable String contentScript) {
        mId = Objects.requireNonNull(id, "id must not be null");
        mName = Objects.requireNonNull(name, "name must not be null");
        mVersion = Objects.requireNonNull(version, "version must not be null");
        mDescription = Objects.requireNonNull(description, "description must not be null");
        mIsEnabled = isEnabled;
        mOptionsUrl = optionsUrl;
        mIsBundled = isBundled;
        mContentScript = contentScript;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    @NonNull
    public String getVersion() {
        return mVersion;
    }

    @NonNull
    public String getDescription() {
        return mDescription;
    }

    public boolean isEnabled() {
        return mIsEnabled;
    }

    @Nullable
    public String getOptionsUrl() {
        return mOptionsUrl;
    }

    public boolean isBundled() {
        return mIsBundled;
    }

    @Nullable
    public String getContentScript() {
        return mContentScript;
    }

    @NonNull
    public ExtensionItem withEnabled(boolean enabled) {
        return new ExtensionItem(mId, mName, mVersion, mDescription, enabled, mOptionsUrl, mIsBundled, mContentScript);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExtensionItem that = (ExtensionItem) o;
        return mIsEnabled == that.mIsEnabled &&
                mIsBundled == that.mIsBundled &&
                mId.equals(that.mId) &&
                mName.equals(that.mName) &&
                mVersion.equals(that.mVersion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mName, mVersion, mIsEnabled, mIsBundled);
    }
}
