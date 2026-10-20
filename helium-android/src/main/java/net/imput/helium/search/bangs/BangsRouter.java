package net.imput.helium.search.bangs;

import android.net.Uri;
import androidx.annotation.NonNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * BangsRouter: Resolves DuckDuckGo / Helium desktop '!bang' syntax directly in the browser
 * without round-tripping through external redirection servers.
 */
public final class BangsRouter {

    private static final Map<String, String> BANGS_MAP;

    static {
        Map<String, String> map = new HashMap<>();
        // Search Engines
        map.put("!g", "https://www.google.com/search?q=");
        map.put("!ddg", "https://duckduckgo.com/?q=");
        map.put("!b", "https://search.brave.com/search?q=");
        map.put("!sp", "https://www.startpage.com/sp/search?query=");
        map.put("!sx", "https://searx.be/search?q=");
        map.put("!eco", "https://www.ecosia.org/search?q=");
        map.put("!kagi", "https://kagi.com/search?q=");
        map.put("!q", "https://www.qwant.com/?q=");

        // Reference & Knowledge
        map.put("!w", "https://en.wikipedia.org/wiki/Special:Search?search=");
        map.put("!wde", "https://de.wikipedia.org/wiki/Spezial:Suche?search=");
        map.put("!wfr", "https://fr.wikipedia.org/wiki/Sp%C3%A9cial:Recherche?search=");
        map.put("!yt", "https://www.youtube.com/results?search_query=");
        map.put("!osm", "https://www.openstreetmap.org/search?query=");
        map.put("!maps", "https://www.google.com/maps/search/");

        // Developer & Engineering
        map.put("!gh", "https://github.com/search?q=");
        map.put("!gl", "https://gitlab.com/search?search=");
        map.put("!so", "https://stackoverflow.com/search?q=");
        map.put("!mdn", "https://developer.mozilla.org/en-US/search?q=");
        map.put("!npm", "https://www.npmjs.com/search?q=");
        map.put("!pypi", "https://pypi.org/search/?q=");
        map.put("!crates", "https://crates.io/search?q=");
        map.put("!aur", "https://aur.archlinux.org/packages?K=");
        map.put("!arch", "https://wiki.archlinux.org/index.php?search=");
        map.put("!mvn", "https://mvnrepository.com/search?q=");

        // News & Community
        map.put("!r", "https://www.reddit.com/search/?q=");
        map.put("!x", "https://x.com/search?q=");
        map.put("!t", "https://x.com/search?q=");
        map.put("!hn", "https://hn.algolia.com/?q=");

        // Privacy & Tools
        map.put("!vt", "https://www.virustotal.com/gui/search/");
        map.put("!virustotal", "https://www.virustotal.com/gui/search/");
        map.put("!archive", "https://web.archive.org/web/*/");
        map.put("!wayback", "https://web.archive.org/web/*/");

        BANGS_MAP = Collections.unmodifiableMap(map);
    }

    public static final class Result {
        private final boolean mIsBang;
        private final String mResolvedUrl;
        private final String mBangPrefix;

        public Result(boolean isBang, String resolvedUrl, String bangPrefix) {
            mIsBang = isBang;
            mResolvedUrl = resolvedUrl;
            mBangPrefix = bangPrefix;
        }

        public boolean isBang() {
            return mIsBang;
        }

        public String getResolvedUrl() {
            return mResolvedUrl;
        }

        public String getBangPrefix() {
            return mBangPrefix;
        }
    }

    private BangsRouter() {}

    /**
     * Resolves a user query against known !bang shortcuts.
     * Returns a Result containing the direct destination URL if matched,
     * or the default search engine dispatch URL if not.
     */
    @NonNull
    public static Result resolve(@NonNull String query, @NonNull String defaultEngineUrl) {
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return new Result(false, defaultEngineUrl, null);
        }

        String[] parts = trimmed.split("\\s+", 2);
        String firstWord = parts[0].toLowerCase();

        if (firstWord.startsWith("!") && BANGS_MAP.containsKey(firstWord)) {
            String baseUrl = BANGS_MAP.get(firstWord);
            String searchTerm = parts.length > 1 ? parts[1].trim() : "";
            String destinationUrl = baseUrl + Uri.encode(searchTerm);
            return new Result(true, destinationUrl, firstWord);
        }

        // Also handle suffix bangs like "arch linux !w"
        if (parts.length > 1) {
            int lastSpace = trimmed.lastIndexOf(' ');
            String lastWord = trimmed.substring(lastSpace + 1).toLowerCase();
            if (lastWord.startsWith("!") && BANGS_MAP.containsKey(lastWord)) {
                String baseUrl = BANGS_MAP.get(lastWord);
                String searchTerm = trimmed.substring(0, lastSpace).trim();
                String destinationUrl = baseUrl + Uri.encode(searchTerm);
                return new Result(true, destinationUrl, lastWord);
            }
        }

        // Default query dispatch
        return new Result(false, defaultEngineUrl + Uri.encode(trimmed), null);
    }

    public static boolean isBangQuery(@NonNull String query) {
        String trimmed = query.trim();
        if (!trimmed.contains("!")) return false;
        String[] parts = trimmed.split("\\s+");
        for (String p : parts) {
            if (BANGS_MAP.containsKey(p.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public static Map<String, String> getBangsMap() {
        return BANGS_MAP;
    }
}
