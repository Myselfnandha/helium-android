package net.imput.helium.privacy.noise;

import android.content.Context;
import androidx.annotation.NonNull;
import java.security.SecureRandom;
import net.imput.helium.preferences.HeliumPreferences;

/**
 * HeliumNoiseController: Manages canvas, audio, and WebGL anti-fingerprinting
 * noise injection, mirroring Helium desktop's helium-noise component.
 */
public final class HeliumNoiseController {

    private final HeliumPreferences mPreferences;
    private final int mSessionSeed;
    private String mCachedScript;

    public HeliumNoiseController(@NonNull Context context) {
        mPreferences = HeliumPreferences.getInstance(context);
        mSessionSeed = new SecureRandom().nextInt(100000) + 1;
    }

    public boolean isEnabled() {
        return mPreferences.isCanvasFingerprintProtectionEnabled();
    }

    /**
     * Returns a lightweight, high-performance JS snippet that introduces sub-perceptual
     * deterministic entropy to HTML5 Canvas, Web Audio, and WebGL query APIs.
     */
    @NonNull
    public synchronized String getInjectionScript() {
        if (!isEnabled()) {
            return "";
        }
        if (mCachedScript != null) {
            return mCachedScript;
        }

        // Lightweight self-executing sandbox overriding fingerprint surfaces
        mCachedScript = "(function() {" +
            "  if (window.__helium_noise_active) return;" +
            "  window.__helium_noise_active = true;" +
            "  var seed = " + mSessionSeed + ";" +
            "  function pseudoRand(s) { var x = Math.sin(s++) * 10000; return x - Math.floor(x); }" +
            "  try {" +
            "    var origGetImageData = CanvasRenderingContext2D.prototype.getImageData;" +
            "    CanvasRenderingContext2D.prototype.getImageData = function(x, y, w, h) {" +
            "      var imgData = origGetImageData.apply(this, arguments);" +
            "      for (var i = 0; i < imgData.data.length; i += 64) {" +
            "        imgData.data[i] = (imgData.data[i] ^ (seed & 1));" +
            "      }" +
            "      return imgData;" +
            "    };" +
            "  } catch(e) {}" +
            "  try {" +
            "    var origAudio = AudioBuffer.prototype.getChannelData;" +
            "    AudioBuffer.prototype.getChannelData = function(channel) {" +
            "      var data = origAudio.apply(this, arguments);" +
            "      for (var j = 0; j < data.length; j += 100) {" +
            "        data[j] += (pseudoRand(seed + j) * 0.0000001);" +
            "      }" +
            "      return data;" +
            "    };" +
            "  } catch(e) {}" +
            "  try {" +
            "    var origGetParam = WebGLRenderingContext.prototype.getParameter;" +
            "    WebGLRenderingContext.prototype.getParameter = function(param) {" +
            "      if (param === 0x9245) return 'Helium Secure Core';" +
            "      if (param === 0x9246) return 'Helium Hardened Mobile GPU';" +
            "      return origGetParam.apply(this, arguments);" +
            "    };" +
            "  } catch(e) {}" +
            "})();";

        return mCachedScript;
    }
}
