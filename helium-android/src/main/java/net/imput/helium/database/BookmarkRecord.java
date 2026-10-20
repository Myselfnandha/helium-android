package net.imput.helium.database;

import androidx.annotation.NonNull;

/**
 * BookmarkRecord: Data model for a bookmarked URL.
 */
public final class BookmarkRecord {

    private final long mId;
    private final String mUrl;
    private final String mTitle;
    private final long mCreatedAt;

    public BookmarkRecord(long id, @NonNull String url, @NonNull String title, long createdAt) {
        mId = id;
        mUrl = url;
        mTitle = title;
        mCreatedAt = createdAt;
    }

    public long getId() {
        return mId;
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    public long getCreatedAt() {
        return mCreatedAt;
    }

    @NonNull
    @Override
    public String toString() {
        return mTitle + " (" + mUrl + ")";
    }
}
