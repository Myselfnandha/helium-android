package net.imput.helium.privacy.permissions;

import androidx.annotation.NonNull;

/**
 * SitePermissionRecord: Represents a persisted per-origin permission grant or denial (PRD M18).
 */
public final class SitePermissionRecord {

    public static final int STATE_PROMPT = 0;
    public static final int STATE_ALLOW = 1;
    public static final int STATE_DENY = 2;

    public static final String PERMISSION_CAMERA = "camera";
    public static final String PERMISSION_MICROPHONE = "microphone";
    public static final String PERMISSION_GEOLOCATION = "geolocation";
    public static final String PERMISSION_NOTIFICATIONS = "notifications";
    public static final String PERMISSION_CLIPBOARD = "clipboard";

    private final long mId;
    private final String mOrigin;
    private final String mPermission;
    private final int mState;
    private final long mUpdatedAt;

    public SitePermissionRecord(long id, @NonNull String origin, @NonNull String permission, int state, long updatedAt) {
        mId = id;
        mOrigin = origin;
        mPermission = permission;
        mState = state;
        mUpdatedAt = updatedAt;
    }

    public long getId() {
        return mId;
    }

    @NonNull
    public String getOrigin() {
        return mOrigin;
    }

    @NonNull
    public String getPermission() {
        return mPermission;
    }

    public int getState() {
        return mState;
    }

    public boolean isAllowed() {
        return mState == STATE_ALLOW;
    }

    public boolean isDenied() {
        return mState == STATE_DENY;
    }

    public long getUpdatedAt() {
        return mUpdatedAt;
    }
}
