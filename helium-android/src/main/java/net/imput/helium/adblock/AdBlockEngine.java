package net.imput.helium.adblock;

import android.net.Uri;
import android.webkit.WebResourceResponse;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * AdBlockEngine: High-performance subresource network filter for Helium Android.
 * Intercepts and blocks network requests for known ad networks, trackers, and telemetry
 * before network sockets open, cutting page load time and data consumption.
 */
public final class AdBlockEngine {

    private static volatile AdBlockEngine sInstance;

    private final Set<String> mBlockedHosts;
    private final Set<String> mCustomBlockedHosts;
    private final AtomicLong mBlockedCount = new AtomicLong(0);

    // Common ad & tracker domain patterns matching desktop Helium filter baseline
    private static final String[] BASE_BLOCKED_HOSTS = new String[] {
        // Major Advertising Exchanges & Networks
        "doubleclick.net",
        "googlesyndication.com",
        "google-analytics.com",
        "googleadservices.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "adnxs.com",
        "criteo.com",
        "criteo.net",
        "adroll.com",
        "rubiconproject.com",
        "openx.net",
        "pubmatic.com",
        "outbrain.com",
        "taboola.com",
        "smartadserver.com",
        "casalemedia.com",
        "yieldmo.com",
        "bidswitch.net",
        "advertising.com",
        "contextweb.com",
        "sovrn.com",
        "sharethrough.com",
        "triplelift.com",
        "33across.com",
        "gumgum.com",
        "infolinks.com",
        "media.net",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "mgid.com",
        "revcontent.com",
        "zergnet.com",

        // Amazon Ad Systems
        "amazon-adsystem.com",
        "aax.amazon-adsystem.com",

        // Social Trackers & Pixels
        "pixel.facebook.com",
        "an.facebook.com",
        "tr.snapchat.com",
        "analytics.twitter.com",
        "ads-twitter.com",
        "ads.tiktok.com",
        "analytics.tiktok.com",
        "ads.pinterest.com",
        "ct.pinterest.com",
        "px.ads.linkedin.com",
        "snap.licdn.com",

        // Behavioral & Telemetry Tracking
        "scorecardresearch.com",
        "quantserve.com",
        "hotjar.com",
        "mouseflow.com",
        "crazyegg.com",
        "fullstory.com",
        "chartbeat.com",
        "mixpanel.com",
        "amplitude.com",
        "segment.io",
        "segment.com",
        "heapanalytics.com",
        "optimizely.com",
        "newrelic.com",
        "branch.io",
        "appsflyer.com",
        "adjust.com",
        "braze.com",
        "leanplum.com",
        "onesignal.com",
        "statcounter.com",
        "yandex.ru/metrika",
        "mc.yandex.ru",

        // Mobile Ad Networks
        "mopub.com",
        "unityads.unity3d.com",
        "vungle.com",
        "applovin.com",
        "ironsrc.com",
        "inmobi.com",
        "chartboost.com",
        "adcolony.com",
        "fyber.com",
        "tapjoy.com",
        "startapp.com",
        "hyprmx.com",
        "smaato.net",
        "flurry.com",

        // Telemetry
        "telemetry.microsoft.com",
        "vortex.data.microsoft.com",
        "browser.pipe.aria.microsoft.com"
    };

    public static AdBlockEngine getInstance() {
        if (sInstance == null) {
            synchronized (AdBlockEngine.class) {
                if (sInstance == null) {
                    sInstance = new AdBlockEngine();
                }
            }
        }
        return sInstance;
    }

    private AdBlockEngine() {
        mBlockedHosts = new HashSet<>(Arrays.asList(BASE_BLOCKED_HOSTS));
        mCustomBlockedHosts = Collections.synchronizedSet(new HashSet<>());
    }

    /**
     * Determines whether the given URL should be blocked based on known ad/tracker hosts.
     * High performance O(1) matching with sub-domain traversal.
     */
    public boolean shouldBlock(@Nullable String urlString) {
        if (urlString == null || urlString.isEmpty()) {
            return false;
        }

        // Whitelist internal schemes
        if (urlString.startsWith("file://") || urlString.startsWith("about:") || urlString.startsWith("chrome://")) {
            return false;
        }

        String host = extractHost(urlString);
        if (host == null || host.isEmpty()) {
            return false;
        }

        if (isHostBlocked(host)) {
            mBlockedCount.incrementAndGet();
            return true;
        }

        return false;
    }

    /**
     * Extracts host from URL string without full URI parsing overhead when possible.
     */
    @Nullable
    public static String extractHost(@NonNull String url) {
        try {
            int schemeEnd = url.indexOf("://");
            if (schemeEnd == -1) {
                return null;
            }
            int hostStart = schemeEnd + 3;
            int hostEnd = url.indexOf('/', hostStart);
            if (hostEnd == -1) {
                hostEnd = url.indexOf('?', hostStart);
            }
            if (hostEnd == -1) {
                hostEnd = url.indexOf('#', hostStart);
            }
            if (hostEnd == -1) {
                hostEnd = url.length();
            }

            String hostWithPort = url.substring(hostStart, hostEnd);
            int colonIdx = hostWithPort.indexOf(':');
            String host = (colonIdx != -1) ? hostWithPort.substring(0, colonIdx) : hostWithPort;
            return host.toLowerCase().trim();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Checks if host or any parent domain (e.g. sub.adservice.google.com -> googleadservices.com)
     * matches the blocked hosts database.
     */
    public boolean isHostBlocked(@NonNull String host) {
        String cleanHost = host.toLowerCase().trim();

        if (mCustomBlockedHosts.contains(cleanHost) || mBlockedHosts.contains(cleanHost)) {
            return true;
        }

        // Check subdomain matching (e.g., "pagead2.googlesyndication.com" matches "googlesyndication.com")
        int dotIndex = cleanHost.indexOf('.');
        while (dotIndex != -1 && dotIndex < cleanHost.length() - 1) {
            String parentDomain = cleanHost.substring(dotIndex + 1);
            if (mCustomBlockedHosts.contains(parentDomain) || mBlockedHosts.contains(parentDomain)) {
                return true;
            }
            dotIndex = cleanHost.indexOf('.', dotIndex + 1);
        }

        return false;
    }

    /**
     * Returns an empty WebResourceResponse to immediately terminate the blocked subresource request.
     */
    @NonNull
    public static WebResourceResponse createEmptyBlockResponse() {
        return new WebResourceResponse(
            "text/plain",
            "UTF-8",
            200,
            "OK",
            Collections.singletonMap("X-Helium-Shield", "Blocked"),
            new ByteArrayInputStream(new byte[0])
        );
    }

    public long getBlockedCount() {
        return mBlockedCount.get();
    }

    public void resetBlockedCount() {
        mBlockedCount.set(0);
    }

    public void addCustomBlockedHost(@NonNull String host) {
        mCustomBlockedHosts.add(host.toLowerCase().trim());
    }

    public void removeCustomBlockedHost(@NonNull String host) {
        mCustomBlockedHosts.remove(host.toLowerCase().trim());
    }

    public int getRuleCount() {
        return mBlockedHosts.size() + mCustomBlockedHosts.size();
    }
}
