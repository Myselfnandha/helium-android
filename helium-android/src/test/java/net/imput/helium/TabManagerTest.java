package net.imput.helium;

import net.imput.helium.browser.tab.Tab;
import net.imput.helium.browser.tab.TabManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests verifying TabManager multi-tab invariants.
 */
public class TabManagerTest {

    private TabManager mTabManager;

    @Before
    public void setUp() {
        mTabManager = TabManager.getInstance();
        mTabManager.closeAllTabs();
    }

    @Test
    public void testTabCreationAndActiveState() {
        assertEquals(0, mTabManager.getTabCount());
        assertNull(mTabManager.getActiveTab());

        Tab tab1 = mTabManager.createTab("https://duckduckgo.com", false);
        assertEquals(1, mTabManager.getTabCount());
        assertNotNull(mTabManager.getActiveTab());
        assertEquals(tab1.getId(), mTabManager.getActiveTab().getId());
        assertFalse(tab1.isIncognito());

        Tab tab2 = mTabManager.createTab("https://startpage.com", true);
        assertEquals(2, mTabManager.getTabCount());
        assertEquals(tab2.getId(), mTabManager.getActiveTab().getId());
        assertTrue(tab2.isIncognito());
    }

    @Test
    public void testTabSwitching() {
        Tab tab1 = mTabManager.createTab("https://site1.com", false);
        Tab tab2 = mTabManager.createTab("https://site2.com", false);

        assertEquals(tab2.getId(), mTabManager.getActiveTab().getId());

        boolean switched = mTabManager.selectTab(tab1.getId());
        assertTrue(switched);
        assertEquals(tab1.getId(), mTabManager.getActiveTab().getId());
    }

    @Test
    public void testTabClosing() {
        Tab tab1 = mTabManager.createTab("https://site1.com", false);
        Tab tab2 = mTabManager.createTab("https://site2.com", false);
        Tab tab3 = mTabManager.createTab("https://site3.com", false);

        assertEquals(3, mTabManager.getTabCount());
        assertEquals(tab3.getId(), mTabManager.getActiveTab().getId());

        // Close active tab -> should switch to previous
        mTabManager.closeTab(tab3.getId());
        assertEquals(2, mTabManager.getTabCount());
        assertNotNull(mTabManager.getActiveTab());
        assertEquals(tab2.getId(), mTabManager.getActiveTab().getId());

        mTabManager.closeAllTabs();
        assertEquals(0, mTabManager.getTabCount());
        assertNull(mTabManager.getActiveTab());
    }
}
