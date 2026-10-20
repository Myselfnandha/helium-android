package net.imput.helium.browser.tab;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.UUID;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * TabGroup: Represents a persistent, color-coded organizational group of tabs (PRD M06).
 */
public final class TabGroup {

    public static final String COLOR_CYAN = "#06B6D4";
    public static final String COLOR_EMERALD = "#10B981";
    public static final String COLOR_AMBER = "#F59E0B";
    public static final String COLOR_PURPLE = "#8B5CF6";
    public static final String COLOR_ROSE = "#F43F5E";

    private final String mId;
    private String mName;
    private String mColorHex;
    private final long mCreatedAt;

    public TabGroup(@NonNull String name, @NonNull String colorHex) {
        this(UUID.randomUUID().toString(), name, colorHex, System.currentTimeMillis());
    }

    public TabGroup(@NonNull String id, @NonNull String name, @NonNull String colorHex, long createdAt) {
        mId = id;
        mName = name;
        mColorHex = colorHex;
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
    public String getColorHex() {
        return (mColorHex != null && !mColorHex.isEmpty()) ? mColorHex : COLOR_CYAN;
    }

    public void setColorHex(@NonNull String colorHex) {
        mColorHex = colorHex;
    }

    public long getCreatedAt() {
        return mCreatedAt;
    }

    @NonNull
    public JSONObject toJsonObject() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("id", mId);
        obj.put("name", mName);
        obj.put("color", mColorHex);
        obj.put("createdAt", mCreatedAt);
        return obj;
    }

    @NonNull
    public static TabGroup fromJsonObject(@NonNull JSONObject obj) throws JSONException {
        String id = obj.getString("id");
        String name = obj.getString("name");
        String color = obj.optString("color", COLOR_CYAN);
        long createdAt = obj.optLong("createdAt", System.currentTimeMillis());
        return new TabGroup(id, name, color, createdAt);
    }
}
