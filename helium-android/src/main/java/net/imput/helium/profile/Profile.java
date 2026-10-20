package net.imput.helium.profile;

import androidx.annotation.NonNull;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Profile: Represents an isolated persistent browsing environment (PRD M21).
 */
public final class Profile {

    public static final String DEFAULT_PROFILE_ID = "default";
    public static final String DEFAULT_PROFILE_NAME = "Personal";

    private final String mId;
    private String mName;
    private String mIconName;
    private final long mCreatedAt;

    public static Profile createDefault() {
        return new Profile(DEFAULT_PROFILE_ID, DEFAULT_PROFILE_NAME, "user", System.currentTimeMillis());
    }

    public Profile(@NonNull String name, @NonNull String iconName) {
        this(UUID.randomUUID().toString(), name, iconName, System.currentTimeMillis());
    }

    public Profile(@NonNull String id, @NonNull String name, @NonNull String iconName, long createdAt) {
        mId = id;
        mName = name;
        mIconName = iconName;
        mCreatedAt = createdAt;
    }

    @NonNull
    public String getId() {
        return mId;
    }

    @NonNull
    public String getName() {
        return mName;
    }

    public void setName(@NonNull String name) {
        mName = name;
    }

    @NonNull
    public String getIconName() {
        return (mIconName != null) ? mIconName : "user";
    }

    public void setIconName(@NonNull String iconName) {
        mIconName = iconName;
    }

    public long getCreatedAt() {
        return mCreatedAt;
    }

    public boolean isDefault() {
        return DEFAULT_PROFILE_ID.equals(mId);
    }

    @NonNull
    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", mId);
        obj.put("name", mName);
        obj.put("icon", mIconName);
        obj.put("createdAt", mCreatedAt);
        return obj;
    }

    @NonNull
    public static Profile fromJsonObject(@NonNull JSONObject obj) throws JSONException {
        String id = obj.getString("id");
        String name = obj.getString("name");
        String icon = obj.optString("icon", "user");
        long createdAt = obj.optLong("createdAt", System.currentTimeMillis());
        return new Profile(id, name, icon, createdAt);
    }
}
