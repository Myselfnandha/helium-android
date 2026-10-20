package net.imput.helium.search;

import android.content.Context;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.imput.helium.database.HeliumDbHelper;
import net.imput.helium.preferences.HeliumPreferences;
import net.imput.helium.search.bangs.BangsRouter;

/**
 * SearchSuggestionEngine: Synthesizes real-time omnibox suggestions across
 * Bangs shortcuts, bookmarks, navigation history, and default search engine.
 */
public final class SearchSuggestionEngine {

    public interface Callback {
        void onSuggestionsReady(@NonNull List<SearchSuggestion> suggestions);
    }

    private SearchSuggestionEngine() {}

    /**
     * Resolves matching suggestions asynchronously.
     */
    public static void query(@NonNull Context context,
                             @NonNull String query,
                             int maxResults,
                             @NonNull Callback callback) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            callback.onSuggestionsReady(Collections.emptyList());
            return;
        }

        List<SearchSuggestion> immediateSuggestions = new ArrayList<>();

        // 1. Bang Shortcut Matches
        if (trimmed.startsWith("!")) {
            String lower = trimmed.toLowerCase();
            for (Map.Entry<String, String> entry : BangsRouter.getBangsMap().entrySet()) {
                String bang = entry.getKey();
                if (bang.startsWith(lower) || lower.startsWith(bang)) {
                    String cleanName = getFriendlyBangName(bang);
                    immediateSuggestions.add(new SearchSuggestion(
                        SearchSuggestion.Type.BANG,
                        bang + " — " + cleanName,
                        entry.getValue(),
                        "!bang shortcut"
                    ));
                    if (immediateSuggestions.size() >= 3) break;
                }
            }
        }

        // 2. Direct Web Address Suggestion
        if (trimmed.contains(".") && !trimmed.contains(" ") && !trimmed.startsWith("http")) {
            immediateSuggestions.add(new SearchSuggestion(
                SearchSuggestion.Type.SEARCH,
                "Go to https://" + trimmed,
                "https://" + trimmed,
                "Web address"
            ));
        }

        // 3. Search Engine Query Fallback
        HeliumPreferences prefs = HeliumPreferences.getInstance(context);
        String engineUrl = prefs.getDefaultSearchEngine();
        immediateSuggestions.add(new SearchSuggestion(
            SearchSuggestion.Type.SEARCH,
            "Search for \"" + trimmed + "\"",
            engineUrl,
            "Web search"
        ));

        // 4. Query Stored Bookmarks & History via Database
        HeliumDbHelper db = HeliumDbHelper.getInstance(context);
        db.searchSuggestions(trimmed, maxResults, dbSuggestions -> {
            List<SearchSuggestion> combined = new ArrayList<>(immediateSuggestions);
            combined.addAll(dbSuggestions);

            if (combined.size() > maxResults) {
                combined = combined.subList(0, maxResults);
            }
            callback.onSuggestionsReady(combined);
        });
    }

    @NonNull
    public static String getFriendlyBangName(@NonNull String bang) {
        switch (bang) {
            case "!w": return "Wikipedia";
            case "!gh": return "GitHub";
            case "!yt": return "YouTube";
            case "!ddg": return "DuckDuckGo";
            case "!b": return "Brave Search";
            case "!sp": return "Startpage";
            case "!sx": return "SearXNG";
            case "!r": return "Reddit";
            case "!so": return "StackOverflow";
            case "!aur": return "Arch User Repository";
            case "!arch": return "Arch Linux Wiki";
            case "!mdn": return "Mozilla Developer Network";
            case "!npm": return "npm Package Registry";
            case "!vt": return "VirusTotal";
            case "!archive": return "Internet Archive Wayback Machine";
            default: return "Search Shortcut";
        }
    }
}
