package net.imput.helium;

import net.imput.helium.search.SearchSuggestion;
import net.imput.helium.search.SearchSuggestionEngine;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for SearchSuggestion model and SearchSuggestionEngine formatting.
 */
public class SearchSuggestionTest {

    @Test
    public void testFriendlyBangNames() {
        assertEquals("Wikipedia", SearchSuggestionEngine.getFriendlyBangName("!w"));
        assertEquals("GitHub", SearchSuggestionEngine.getFriendlyBangName("!gh"));
        assertEquals("YouTube", SearchSuggestionEngine.getFriendlyBangName("!yt"));
        assertEquals("DuckDuckGo", SearchSuggestionEngine.getFriendlyBangName("!ddg"));
        assertEquals("Arch User Repository", SearchSuggestionEngine.getFriendlyBangName("!aur"));
        assertEquals("Search Shortcut", SearchSuggestionEngine.getFriendlyBangName("!unknown_shortcut"));
    }

    @Test
    public void testSearchSuggestionProperties() {
        SearchSuggestion s = new SearchSuggestion(
            SearchSuggestion.Type.BANG,
            "!w — Wikipedia",
            "https://en.wikipedia.org/wiki/Special:Search?search=",
            "!bang shortcut"
        );

        assertEquals(SearchSuggestion.Type.BANG, s.getType());
        assertEquals("!w — Wikipedia", s.getTitle());
        assertEquals("https://en.wikipedia.org/wiki/Special:Search?search=", s.getUrl());
        assertEquals("!bang shortcut", s.getExtra());
    }
}
