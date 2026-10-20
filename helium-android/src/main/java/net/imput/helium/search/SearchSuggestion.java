package net.imput.helium.search;

import androidx.annotation.NonNull;

/**
 * SearchSuggestion: Represents an autocomplete or omnibox suggestion item.
 */
public final class SearchSuggestion {

    public enum Type {
        BANG,
        BOOKMARK,
        HISTORY,
        SEARCH
    }

    private final Type mType;
    private final String mTitle;
    private final String mUrl;
    private final String mExtra;

    public SearchSuggestion(@NonNull Type type, @NonNull String title, @NonNull String url) {
        this(type, title, url, "");
    }

    public SearchSuggestion(@NonNull Type type, @NonNull String title, @NonNull String url, @NonNull String extra) {
        mType = type;
        mTitle = title;
        mUrl = url;
        mExtra = extra;
    }

    @NonNull
    public Type getType() {
        return mType;
    }

    @NonNull
    public String getTitle() {
        return mTitle;
    }

    @NonNull
    public String getUrl() {
        return mUrl;
    }

    @NonNull
    public String getExtra() {
        return mExtra;
    }

    @NonNull
    public String getSubtitle() {
        return mExtra;
    }
}
