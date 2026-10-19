package net.imput.helium;

import net.imput.helium.extensions.ExtensionItem;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Helium mobile extension data models and state invariants.
 */
public class ExtensionManagerTest {

    @Test
    public void testExtensionItemImmutabilityAndGetters() {
        ExtensionItem item = new ExtensionItem(
                "test_id",
                "Test Extension",
                "1.0.0",
                "A test extension description",
                true,
                "chrome-extension://test_id/options.html",
                false
        );

        assertEquals("test_id", item.getId());
        assertEquals("Test Extension", item.getName());
        assertEquals("1.0.0", item.getVersion());
        assertEquals("A test extension description", item.getDescription());
        assertTrue(item.isEnabled());
        assertEquals("chrome-extension://test_id/options.html", item.getOptionsUrl());
        assertFalse(item.isBundled());

        // withEnabled returns a new instance with updated boolean
        ExtensionItem disabled = item.withEnabled(false);
        assertFalse(disabled.isEnabled());
        assertEquals("test_id", disabled.getId());
        assertNotEquals(item, disabled);

        // Content script verification
        ExtensionItem itemWithScript = new ExtensionItem(
                "script_ext", "Script Ext", "2.0.0", "Desc", true, null, false, "console.log('helium');"
        );
        assertEquals("console.log('helium');", itemWithScript.getContentScript());
        assertEquals("console.log('helium');", itemWithScript.withEnabled(false).getContentScript());
    }

    @Test
    public void testExtensionItemEqualityContract() {
        ExtensionItem item1 = new ExtensionItem(
                "ext_1", "Name", "1.0.0", "Desc", true, null, true
        );
        ExtensionItem item2 = new ExtensionItem(
                "ext_1", "Name", "1.0.0", "Desc", true, null, true
        );
        ExtensionItem item3 = new ExtensionItem(
                "ext_2", "Name", "1.0.0", "Desc", true, null, true
        );

        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
        assertNotEquals(item1, item3);
    }

    @Test(expected = NullPointerException.class)
    public void testExtensionItemNullIdGuard() {
        new ExtensionItem(null, "Name", "1.0.0", "Desc", true, null, false);
    }

    @Test(expected = NullPointerException.class)
    public void testExtensionItemNullNameGuard() {
        new ExtensionItem("id", null, "1.0.0", "Desc", true, null, false);
    }
}
