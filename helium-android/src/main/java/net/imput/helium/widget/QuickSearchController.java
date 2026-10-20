package net.imput.helium.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import net.imput.helium.preferences.HeliumPreferences;
import net.imput.helium.search.SearchSuggestion;
import net.imput.helium.search.SearchSuggestionEngine;

/**
 * QuickSearchController: Coordinates 250ms debounced omnibox autocomplete,
 * local recent searches grouping, 6-row limiting, and smart URL interpretation.
 */
public final class QuickSearchController {

    public static final int MAX_VISIBLE_SUGGESTIONS = 6;
    public static final long DEBOUNCE_DELAY_MS = 250L;

    public interface SuggestionCallback {
        void onSuggestionsUpdated(
                @NonNull List<String> recentSearches,
                @NonNull List<SearchSuggestion> liveSuggestions,
                long generation
        );
    }

    private final Context mContext;
    private final Handler mHandler;
    private long mCurrentGeneration = 0;
    private Runnable mPendingDebounceRunnable;

    public QuickSearchController(@NonNull Context context) {
        mContext = context.getApplicationContext();
        mHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Determines whether user input should be treated as a direct URL or a search query.
     */
    public static boolean isDirectUrl(@NonNull String input) {
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return false;
        String lower = trimmed.toLowerCase();

        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("about:")) {
            return true;
        }

        // Domain pattern without spaces e.g. "reddit.com", "news.ycombinator.com/item?id=1"
        if (!trimmed.contains(" ") && trimmed.contains(".")) {
            int dot = trimmed.indexOf('.');
            if (dot > 0 && dot < trimmed.length() - 1) {
                String tld = trimmed.substring(dot + 1);
                int slash = tld.indexOf('/');
                if (slash != -1) {
                    tld = tld.substring(0, slash);
                }
                // Valid TLD usually 2+ characters
                return tld.length() >= 2;
            }
        }
        return false;
    }

    /**
     * Resolves submission text to a fully qualified URL.
     */
    @NonNull
    public static String resolveSubmissionUrl(@NonNull Context context, @NonNull String input) {
        String trimmed = input.trim();
        if (isDirectUrl(trimmed)) {
            if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://") && !trimmed.startsWith("about:")) {
                return "https://" + trimmed;
            }
            return trimmed;
        }

        // Search engine fallback
        HeliumPreferences prefs = HeliumPreferences.getInstance(context);
        String engineUrl = prefs.getDefaultSearchEngine();
        String encoded;
        try {
            encoded = java.net.URLEncoder.encode(trimmed, "UTF-8");
        } catch (Exception e) {
            encoded = trimmed.replace(" ", "+");
        }

        if (engineUrl.contains("%s")) {
            return engineUrl.replace("%s", encoded);
        } else {
            return engineUrl + encoded;
        }
    }

    /**
     * Dispatches typing update with 250ms debounce and stale generation discard.
     */
    public void onQueryChanged(@NonNull String query, @NonNull SuggestionCallback callback) {
        if (mPendingDebounceRunnable != null) {
            mHandler.removeCallbacks(mPendingDebounceRunnable);
        }

        mCurrentGeneration++;
        final long generation = mCurrentGeneration;

        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            // Show recent history immediately when query is blank
            List<String> recents = SearchHistoryStore.getRecentQueries(mContext, MAX_VISIBLE_SUGGESTIONS);
            callback.onSuggestionsUpdated(recents, new ArrayList<>(), generation);
            return;
        }

        mPendingDebounceRunnable = () -> {
            if (generation != mCurrentGeneration) {
                return; // Stale query generation discarded
            }

            // 1. Fetch matching local recent searches
            List<String> allRecents = SearchHistoryStore.getRecentQueries(mContext, 10);
            List<String> matchingRecents = new ArrayList<>();
            String lower = trimmed.toLowerCase();
            for (String r : allRecents) {
                if (r.toLowerCase().contains(lower) && !r.equalsIgnoreCase(trimmed)) {
                    matchingRecents.add(r);
                    if (matchingRecents.size() >= 3) break;
                }
            }

            // 2. Fetch live suggestions from engine
            int remainingSlots = MAX_VISIBLE_SUGGESTIONS - matchingRecents.size();
            SearchSuggestionEngine.query(mContext, trimmed, remainingSlots, liveList -> {
                if (generation != mCurrentGeneration) {
                    return; // Stale async response discarded
                }

                // Strictly enforce total 6-row cap
                List<SearchSuggestion> cappedLive = new ArrayList<>(liveList);
                if (matchingRecents.size() + cappedLive.size() > MAX_VISIBLE_SUGGESTIONS) {
                    int allowed = Math.max(0, MAX_VISIBLE_SUGGESTIONS - matchingRecents.size());
                    cappedLive = cappedLive.subList(0, allowed);
                }

                callback.onSuggestionsUpdated(matchingRecents, cappedLive, generation);
            });
        };

        mHandler.postDelayed(mPendingDebounceRunnable, DEBOUNCE_DELAY_MS);
    }

    public void cancel() {
        if (mPendingDebounceRunnable != null) {
            mHandler.removeCallbacks(mPendingDebounceRunnable);
            mPendingDebounceRunnable = null;
        }
        mCurrentGeneration++;
    }
}
