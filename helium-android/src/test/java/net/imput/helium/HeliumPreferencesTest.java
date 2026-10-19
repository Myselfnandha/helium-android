package net.imput.helium;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests validating Helium preference key constants and invariant contracts.
 */
public class HeliumPreferencesTest {

    @Test
    public void testPreferenceKeyConstants() {
        assertEquals("helium_first_run_completed", 
                net.imput.helium.preferences.HeliumPreferences.KEY_FIRST_RUN_COMPLETED);
        assertEquals("helium_extensions_enabled", 
                net.imput.helium.preferences.HeliumPreferences.KEY_EXTENSIONS_ENABLED);
        assertEquals("helium_native_adblock_enabled", 
                net.imput.helium.preferences.HeliumPreferences.KEY_NATIVE_ADBLOCK_ENABLED);
        assertEquals("helium_jit_protection_enabled", 
                net.imput.helium.preferences.HeliumPreferences.KEY_JIT_PROTECTION_ENABLED);
        assertEquals("helium_vault_sync_enabled", 
                net.imput.helium.preferences.HeliumPreferences.KEY_VAULT_SYNC_ENABLED);
        assertEquals("helium_background_media_enabled", 
                net.imput.helium.preferences.HeliumPreferences.KEY_BACKGROUND_MEDIA_ENABLED);
        assertEquals("helium_anti_fingerprint_enabled", 
                net.imput.helium.preferences.HeliumPreferences.KEY_ANTI_FINGERPRINT_ENABLED);
        assertEquals("helium_url_tracker_stripper_enabled", 
                net.imput.helium.preferences.HeliumPreferences.KEY_URL_TRACKER_STRIPPER_ENABLED);
        assertEquals("helium_https_only_mode_enabled", 
                net.imput.helium.preferences.HeliumPreferences.KEY_HTTPS_ONLY_MODE_ENABLED);
        assertEquals("helium_secure_dns_enabled", 
                net.imput.helium.preferences.HeliumPreferences.KEY_SECURE_DNS_ENABLED);
        assertEquals("helium_default_search_engine", 
                net.imput.helium.preferences.HeliumPreferences.KEY_DEFAULT_SEARCH_ENGINE);
        assertEquals("https://duckduckgo.com/?q=", 
                net.imput.helium.preferences.HeliumPreferences.SEARCH_ENGINE_DUCKDUCKGO);
        assertEquals("https://search.brave.com/search?q=", 
                net.imput.helium.preferences.HeliumPreferences.SEARCH_ENGINE_BRAVE);
        assertEquals("https://searx.be/search?q=", 
                net.imput.helium.preferences.HeliumPreferences.SEARCH_ENGINE_SEARXNG);
        assertEquals("https://www.startpage.com/sp/search?query=", 
                net.imput.helium.preferences.HeliumPreferences.SEARCH_ENGINE_STARTPAGE);
    }
}
