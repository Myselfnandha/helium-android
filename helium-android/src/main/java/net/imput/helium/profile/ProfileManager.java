package net.imput.helium.profile;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * ProfileManager: Deep-module controller for persistent browsing environments (PRD M21).
 * Manages profile lifecycle, switching, and persistent serialized storage.
 */
public final class ProfileManager {

    private static final String PREF_NAME = "helium_profiles";
    private static final String KEY_PROFILES_JSON = "profiles_list";
    private static final String KEY_ACTIVE_PROFILE_ID = "active_profile_id";

    public interface OnProfileChangeListener {
        void onProfileSwitched(@NonNull Profile newProfile);
        void onProfilesChanged(@NonNull List<Profile> profiles);
    }

    private static volatile ProfileManager sInstance;

    private final SharedPreferences mPrefs;
    private final List<Profile> mProfiles = new ArrayList<>();
    private final List<OnProfileChangeListener> mListeners = new CopyOnWriteArrayList<>();
    private String mActiveProfileId;

    public static ProfileManager getInstance(@NonNull Context context) {
        if (sInstance == null) {
            synchronized (ProfileManager.class) {
                if (sInstance == null) {
                    SharedPreferences prefs = context.getApplicationContext()
                            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
                    sInstance = new ProfileManager(prefs);
                }
            }
        }
        return sInstance;
    }

    public ProfileManager(@NonNull SharedPreferences prefs) {
        mPrefs = prefs;
        loadProfiles();
    }

    public synchronized void addListener(@NonNull OnProfileChangeListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public synchronized void removeListener(@NonNull OnProfileChangeListener listener) {
        mListeners.remove(listener);
    }

    private void loadProfiles() {
        mProfiles.clear();
        String json = mPrefs.getString(KEY_PROFILES_JSON, null);
        if (json != null && !json.trim().isEmpty()) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    mProfiles.add(Profile.fromJsonObject(array.getJSONObject(i)));
                }
            } catch (JSONException ignored) {}
        }

        // Ensure default profile exists
        if (mProfiles.isEmpty()) {
            mProfiles.add(Profile.createDefault());
        }

        String savedActive = mPrefs.getString(KEY_ACTIVE_PROFILE_ID, null);
        if (savedActive != null && findProfileById(savedActive) != null) {
            mActiveProfileId = savedActive;
        } else {
            mActiveProfileId = mProfiles.get(0).getId();
        }
    }

    private void saveProfiles() {
        try {
            JSONArray array = new JSONArray();
            for (Profile p : mProfiles) {
                array.put(p.toJsonObject());
            }
            mPrefs.edit()
                    .putString(KEY_PROFILES_JSON, array.toString())
                    .putString(KEY_ACTIVE_PROFILE_ID, mActiveProfileId)
                    .apply();
        } catch (JSONException ignored) {}
    }

    @NonNull
    public synchronized Profile getActiveProfile() {
        Profile active = findProfileById(mActiveProfileId);
        if (active != null) {
            return active;
        }
        return mProfiles.get(0);
    }

    public synchronized boolean switchProfile(@NonNull String profileId) {
        Profile target = findProfileById(profileId);
        if (target == null || profileId.equals(mActiveProfileId)) {
            return false;
        }

        mActiveProfileId = profileId;
        saveProfiles();

        for (OnProfileChangeListener l : mListeners) {
            l.onProfileSwitched(target);
        }
        return true;
    }

    @NonNull
    public synchronized Profile createProfile(@NonNull String name, @NonNull String iconName) {
        Profile profile = new Profile(name, iconName);
        mProfiles.add(profile);
        saveProfiles();

        for (OnProfileChangeListener l : mListeners) {
            l.onProfilesChanged(getProfiles());
        }
        return profile;
    }

    public synchronized boolean deleteProfile(@NonNull String profileId) {
        // Invariant: Default profile cannot be deleted
        if (Profile.DEFAULT_PROFILE_ID.equals(profileId)) {
            return false;
        }

        Profile target = findProfileById(profileId);
        if (target == null) {
            return false;
        }

        mProfiles.remove(target);

        // If active profile was deleted, switch back to default
        if (profileId.equals(mActiveProfileId)) {
            mActiveProfileId = Profile.DEFAULT_PROFILE_ID;
            for (OnProfileChangeListener l : mListeners) {
                l.onProfileSwitched(getActiveProfile());
            }
        }

        saveProfiles();

        for (OnProfileChangeListener l : mListeners) {
            l.onProfilesChanged(getProfiles());
        }
        return true;
    }

    public synchronized boolean renameProfile(@NonNull String profileId, @NonNull String newName) {
        Profile target = findProfileById(profileId);
        if (target == null || newName.trim().isEmpty()) {
            return false;
        }

        target.setName(newName.trim());
        saveProfiles();

        for (OnProfileChangeListener l : mListeners) {
            l.onProfilesChanged(getProfiles());
        }
        return true;
    }

    @NonNull
    public synchronized List<Profile> getProfiles() {
        return Collections.unmodifiableList(new ArrayList<>(mProfiles));
    }

    @Nullable
    public synchronized Profile findProfileById(@NonNull String profileId) {
        for (Profile p : mProfiles) {
            if (p.getId().equals(profileId)) {
                return p;
            }
        }
        return null;
    }
}
