package net.imput.helium;

import java.util.List;
import net.imput.helium.browser.tab.Tab;
import net.imput.helium.browser.tab.TabGroup;
import net.imput.helium.browser.tab.TabManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for PRD M06: Tab Groups, Color Badges, and Pinned Tabs.
 */
public class TabGroupTest {

    private TabManager mTabManager;

    @Before
    public void setUp() {
        mTabManager = TabManager.getInstance();
        mTabManager.closeAllTabs(true);
        mTabManager.clearGroups();
    }

    @Test
    public void testCreateGroupAndAssignTab() {
        TabGroup group = mTabManager.createGroup("Work & Research", TabGroup.COLOR_CYAN);
        assertNotNull(group.getId());
        assertEquals("Work & Research", group.getName());
        assertEquals(TabGroup.COLOR_CYAN, group.getColorHex());

        Tab tab1 = mTabManager.createTab("https://github.com", false);
        Tab tab2 = mTabManager.createTab("https://docs.oracle.com", false);

        mTabManager.assignTabToGroup(tab1.getId(), group.getId());

        assertEquals(group.getId(), tab1.getGroupId());
        assertNull(tab2.getGroupId());

        List<Tab> groupTabs = mTabManager.getTabsInGroup(group.getId());
        assertEquals(1, groupTabs.size());
        assertEquals(tab1.getId(), groupTabs.get(0).getId());
    }

    @Test
    public void testDeleteGroupDissociatesTabsWithoutClosingThem() {
        TabGroup group = mTabManager.createGroup("Shopping", TabGroup.COLOR_AMBER);
        Tab tab = mTabManager.createTab("https://amazon.com", false);
        mTabManager.assignTabToGroup(tab.getId(), group.getId());

        assertEquals(group.getId(), tab.getGroupId());
        assertEquals(1, mTabManager.getGroups().size());

        boolean deleted = mTabManager.deleteGroup(group.getId());
        assertTrue(deleted);
        assertEquals(0, mTabManager.getGroups().size());
        assertNull(tab.getGroupId());
        assertEquals(1, mTabManager.getTabCount()); // Tab remains open
    }

    @Test
    public void testPinnedTabProtectionFromCloseAll() {
        Tab normalTab = mTabManager.createTab("https://news.ycombinator.com", false);
        Tab pinnedTab = mTabManager.createTab("https://mail.proton.me", false);

        mTabManager.setTabPinned(pinnedTab.getId(), true);
        assertTrue(pinnedTab.isPinned());
        assertFalse(normalTab.isPinned());

        // closeAllTabs() default preserves pinned tabs
        mTabManager.closeAllTabs();

        assertEquals(1, mTabManager.getTabCount());
        assertEquals(pinnedTab.getId(), mTabManager.getTabs().get(0).getId());
        assertTrue(mTabManager.getTabs().get(0).isPinned());

        // closeAllTabs(true) forces close of all tabs including pinned
        mTabManager.closeAllTabs(true);
        assertEquals(0, mTabManager.getTabCount());
    }

    @Test
    public void testSessionSerializationWithGroupsAndPinnedTabs() {
        TabGroup group = mTabManager.createGroup("Security Audits", TabGroup.COLOR_EMERALD);
        Tab tab1 = mTabManager.createTab("https://cve.mitre.org", false);
        tab1.setTitle("CVE Mitre");
        mTabManager.assignTabToGroup(tab1.getId(), group.getId());

        Tab tab2 = mTabManager.createTab("https://virustotal.com", false);
        tab2.setTitle("VirusTotal");
        mTabManager.setTabPinned(tab2.getId(), true);

        mTabManager.selectTab(tab1.getId());

        String sessionJson = mTabManager.serializeSession();
        assertNotNull(sessionJson);
        assertTrue(sessionJson.contains("Security Audits"));
        assertTrue(sessionJson.contains(TabGroup.COLOR_EMERALD));
        assertTrue(sessionJson.contains("cve.mitre.org"));
        assertTrue(sessionJson.contains("virustotal.com"));
        assertTrue(sessionJson.contains("\"pinned\":true"));

        // Clear manager completely
        mTabManager.closeAllTabs(true);
        assertEquals(0, mTabManager.getTabCount());

        // Restore
        boolean restored = mTabManager.restoreSession(sessionJson);
        assertTrue(restored);

        assertEquals(1, mTabManager.getGroups().size());
        TabGroup restoredGroup = mTabManager.getGroups().get(0);
        assertEquals("Security Audits", restoredGroup.getName());
        assertEquals(TabGroup.COLOR_EMERALD, restoredGroup.getColorHex());

        assertEquals(2, mTabManager.getTabCount());
        Tab restoredTab1 = mTabManager.findTabById(tab1.getId());
        assertNotNull(restoredTab1);
        assertEquals(restoredGroup.getId(), restoredTab1.getGroupId());
        assertFalse(restoredTab1.isPinned());

        Tab restoredTab2 = mTabManager.findTabById(tab2.getId());
        assertNotNull(restoredTab2);
        assertNull(restoredTab2.getGroupId());
        assertTrue(restoredTab2.isPinned());
    }
}
