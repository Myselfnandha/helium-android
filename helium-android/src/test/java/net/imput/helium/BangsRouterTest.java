package net.imput.helium;

import net.imput.helium.search.bangs.BangsRouter;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for BangsRouter search prefix resolution.
 */
public class BangsRouterTest {

    private static final String DEFAULT_SEARCH = "https://duckduckgo.com/?q=";

    @Test
    public void testPrefixBangResolution() {
        BangsRouter.Result res = BangsRouter.resolve("!w Helium browser", DEFAULT_SEARCH);
        assertTrue(res.isBang());
        assertEquals("!w", res.getBangPrefix());
        assertTrue(res.getResolvedUrl().startsWith("https://en.wikipedia.org/wiki/Special:Search?search="));
        assertTrue(res.getResolvedUrl().contains("Helium"));
    }

    @Test
    public void testDeveloperBangs() {
        BangsRouter.Result gh = BangsRouter.resolve("!gh imputnet/helium", DEFAULT_SEARCH);
        assertTrue(gh.isBang());
        assertTrue(gh.getResolvedUrl().startsWith("https://github.com/search?q="));

        BangsRouter.Result yt = BangsRouter.resolve("!yt lofi hip hop", DEFAULT_SEARCH);
        assertTrue(yt.isBang());
        assertTrue(yt.getResolvedUrl().startsWith("https://www.youtube.com/results?search_query="));
    }

    @Test
    public void testSuffixBang() {
        BangsRouter.Result res = BangsRouter.resolve("arch linux packages !aur", DEFAULT_SEARCH);
        assertTrue(res.isBang());
        assertEquals("!aur", res.getBangPrefix());
        assertTrue(res.getResolvedUrl().startsWith("https://aur.archlinux.org/packages?K="));
    }

    @Test
    public void testNonBangDefaultFallback() {
        BangsRouter.Result res = BangsRouter.resolve("plain query without bangs", DEFAULT_SEARCH);
        assertFalse(res.isBang());
        assertNull(res.getBangPrefix());
        assertTrue(res.getResolvedUrl().startsWith(DEFAULT_SEARCH));
    }

    @Test
    public void testIsBangQuery() {
        assertTrue(BangsRouter.isBangQuery("!w search"));
        assertTrue(BangsRouter.isBangQuery("query !gh"));
        assertFalse(BangsRouter.isBangQuery("plain search query"));
        assertFalse(BangsRouter.isBangQuery(""));
    }
}
