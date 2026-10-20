package net.imput.helium.database;

import androidx.annotation.NonNull;

/**
 * HistoryRecord: Data model for browser navigation history.
 */
public final class HistoryRecord {

    private final long mId;
    private final String mUrl;
    private final String mTitle;
    private final int mVisitCount;
    private final long mLastVisitedAt;

    public HistoryRecord(long id, @NonNull String url, @NonNull String title, int visitCount, long lastVisitedAt) {
        mId = id;
        mUrl = url;
        mTitle = title;
        mVisitCount = visitCount;
        mLastVisitedAt = lastVisitedAt;
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

    public int getVisitCount() {
        return mVisitCount;
    }

    public long getLastVisitedAt() {
        return mLastVisitedAt;
    }

    @NonNull
    @Override
    public String toString() {
        return mTitle + " (" + mUrl + ") - visited " + mVisitCount + " times";
    }
}
